package com.picpaydesafio.services;

import com.picpaydesafio.domain.transaction.Transaction;
import com.picpaydesafio.domain.user.User;
import com.picpaydesafio.dtos.TransactionDTO;
import com.picpaydesafio.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service

public class TransactionService {
    @Autowired
    private UserService userService;
    @Autowired
    private TransactionRepository repository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private NotificationService notificationService;

    public Transaction createTransaction(TransactionDTO transaction) throws Exception {
        User sender = this.userService.findUserById(transaction.senderId());
        User receiver = this.userService.findUserById(transaction.receiverId());

        userService.validateTransaction(sender, transaction.value());

        // Verificação de autorização
        if (!this.authorizeTransaction(sender, transaction.value())) {
            throw new Exception("Transação não autorizada");
        }

        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.value());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());

        // Atualiza saldos
        sender.setBalance(sender.getBalance().subtract(transaction.value()));
        receiver.setBalance(receiver.getBalance().add(transaction.value()));

        this.repository.save(newTransaction);
        this.userService.saveUser(sender);
        this.userService.saveUser(receiver);

        // Envia notificações
        this.notificationService.sendNotification(sender, "Transação enviada com sucesso");
        this.notificationService.sendNotification(receiver, "Transação recebida com sucesso");

        return newTransaction;
    }

    public boolean authorizeTransaction(User sender, BigDecimal value) {
        try {
            String mockUrl = "https://run.mocky.io/v3/9430dff0-8f3a-4fcb-8039-35538b1dec43";

            // Faz a requisição e captura a resposta completa
            ResponseEntity<String> response = restTemplate.exchange(
                    mockUrl,
                    HttpMethod.GET,
                    null,
                    String.class // Esperamos uma String pois o body está vazio
            );

            // Debug: mostra todos os headers recebidos
            System.out.println("HEADERS RECEBIDOS: " + response.getHeaders());

            // Extrai os headers específicos
            HttpHeaders headers = response.getHeaders();
            String message = headers.getFirst("message");
            String authorization = headers.getFirst("authorization");

            // Verifica a autorização
            boolean isAuthorized = response.getStatusCode() == HttpStatus.OK
                    && "Autorizado".equalsIgnoreCase(message)
                    && "true".equalsIgnoreCase(authorization);

            System.out.println("RESULTADO AUTORIZAÇÃO: " + isAuthorized);
            return isAuthorized;

        } catch (Exception e) {
            System.err.println("ERRO NA AUTORIZAÇÃO: " + e.getMessage());
            return false;
        }
    }
}
