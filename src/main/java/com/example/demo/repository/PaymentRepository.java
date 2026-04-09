package com.example.demo.repository;

import com.example.demo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByGroupId(Long groupId);

    @Query("SELECT p FROM Payment p WHERE p.group.id = :groupId AND " +
           "((p.debtor.id = :debtorId AND p.creditor.id = :creditorId) OR " +
           "(p.debtor.id = :creditorId AND p.creditor.id = :debtorId))")
    List<Payment> findPaymentsBetweenMembers(@Param("groupId") Long groupId,
                                             @Param("debtorId") Long debtorId,
                                             @Param("creditorId") Long creditorId);
}
