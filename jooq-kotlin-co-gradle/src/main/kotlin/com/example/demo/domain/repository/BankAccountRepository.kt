package com.example.demo.domain.repository

import com.example.demo.domain.model.BankAccount
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import java.util.*

interface BankAccountRepository : CoroutineSortingRepository<BankAccount, UUID>,
    CoroutineCrudRepository<BankAccount, UUID> {
}