package com.example.demo.repository;

import com.example.demo.entity.ExpenseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseDetailRepository extends JpaRepository<ExpenseDetail, Long> {
    List<ExpenseDetail> findByExpenseId(Long expenseId);
    List<ExpenseDetail> findByMemberId(Long memberId);
}
