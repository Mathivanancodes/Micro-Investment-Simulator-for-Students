#!/usr/bin/env bash
# Shared local-development initialization. Source this file; it never prints secrets.
set -euo pipefail
project_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
mkdir -p .local
if ! test -f .local/dev.env; then
  (umask 077
   printf 'MYSQL_PASSWORD=%s\nMYSQL_ROOT_PASSWORD=%s\nJWT_SECRET=%s\nDEMO_PASSWORD=%s\n' \
     "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" "$(openssl rand -hex 32)" "$(openssl rand -hex 12)" > .local/dev.env)
fi
set -a
source .local/dev.env
set +a
export LOCAL_DB_PORT=${LOCAL_DB_PORT:-3307}
export SPRING_DATASOURCE_USERNAME=simulator
export SPRING_DATASOURCE_PASSWORD="$MYSQL_PASSWORD"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Duser.timezone=UTC"
compose() { docker compose -p micro-investment -f "$project_dir/compose.yaml" "$@"; }
prepare_database() {
  compose up -d --wait db
  compose cp db:/var/lib/mysql/ca.pem .local/mysql-ca.pem >/dev/null
  rm -f .local/mysql-trust.p12
  keytool -importcert -noprompt -alias local-mysql -file .local/mysql-ca.pem \
    -keystore .local/mysql-trust.p12 -storetype PKCS12 -storepass changeit >/dev/null 2>&1
  export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:$LOCAL_DB_PORT/micro_investment?sslMode=VERIFY_CA&trustCertificateKeyStoreUrl=file:$project_dir/.local/mysql-trust.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=changeit&connectionTimeZone=UTC"
}
# MAVEN_ARGS can supply a local settings file and cache flags without editing the POM.
run_maven() { mvn ${MAVEN_ARGS:-} -B -ntp "$@"; }
