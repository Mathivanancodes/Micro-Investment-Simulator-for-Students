#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"
prepare_database
# Tests may create/drop tables only in this separate disposable schema.
compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root -e "CREATE DATABASE IF NOT EXISTS micro_investment_test; GRANT ALL ON micro_investment_test.* TO \"simulator\"@\"%\";"'
export TEST_DATABASE_URL="${SPRING_DATASOURCE_URL/\/micro_investment?/\/micro_investment_test?}"
export RUN_MYSQL_TESTS=true
(cd backend && run_maven clean verify)
(cd frontend && npm ci --no-audit --no-fund && npm run build && npm run lint)
echo 'Backend unit and MySQL integration tests, frontend build and lint passed.'
