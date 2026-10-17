package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.*;
import com.microinvestment.simulator.entity.Transaction;
import com.microinvestment.simulator.exception.*;
import com.microinvestment.simulator.repository.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionService {
    private final UserRepository users;
    private final TransactionRepository transactions;

    public TransactionService(UserRepository users, TransactionRepository transactions) {
        this.users = users;
        this.transactions = transactions;
    }

    public PageResponse<TransactionResponse> page(Long userId, int page, int size) {
        requireUser(userId);
        var result = transactions.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::response).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    // INTERVIEW: Checked I/O errors require catch/declare; domain rule exceptions are unchecked.
    public String exportCsv(Long userId) throws SimulationExportException {
        requireUser(userId);
        StringWriter output = new StringWriter();
        BufferedWriter writer = new BufferedWriter(output);
        try {
            writer.write("id,type,symbol,quantity,price,amount,created_at\n");
            for (Transaction tx : transactions.findByUserIdOrderByCreatedAtAsc(userId)) {
                writer.write(tx.getId() + "," + tx.getType() + "," +
                        (tx.getStock() == null ? "" : tx.getStock().getSymbol()) + "," +
                        tx.getQuantity() + "," + tx.getPrice() + "," + tx.getAmount() + "," + tx.getCreatedAt() + "\n");
            }
            writer.flush();
            return output.toString();
        } catch (IOException exception) {
            throw new SimulationExportException("Transaction export could not be generated.", exception);
        } finally {
            // INTERVIEW: finally performs cleanup whether try returns or throws.
            try {
                writer.close();
            } catch (IOException exception) {
                throw new SimulationExportException("Transaction export could not be closed.", exception);
            }
        }
    }

    private void requireUser(Long id) {
        if (!users.existsById(id)) throw new ResourceNotFoundException("User not found.");
    }

    private TransactionResponse response(Transaction tx) {
        return new TransactionResponse(tx.getId(), tx.getStock() == null ? null : tx.getStock().getId(),
                tx.getStock() == null ? null : tx.getStock().getSymbol(), tx.getType().name(),
                tx.getQuantity(), tx.getPrice(), tx.getAmount(), tx.getCreatedAt());
    }
}
