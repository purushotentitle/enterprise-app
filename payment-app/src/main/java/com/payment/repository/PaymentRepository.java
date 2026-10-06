package com.payment.repository;

import com.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByFromAccount(String fromAccount);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    List<Payment> findByStandard(Payment.MessageStandard standard);
}
