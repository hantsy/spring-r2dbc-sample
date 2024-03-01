package com.example.demo.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.*


@Table("bank_accounts")
sealed class BankAccount(
    @Id
    open val id: UUID? = null,

    @Column(value = "account_number")
    open  var accountNumber: String? = null,

    @Column(value = "routing_number")
    open var routingNumber: String? = null
)

data class BusinessBankAccount(
    override val id: UUID? = null,
    override var accountNumber: String? = null,
    override var routingNumber: String? = null,
    @Column(value = "business_name")
    var businessName: String? = null

) : BankAccount()

data class IndividualBankAccount(
    override val id: UUID? = null,
    override var accountNumber: String? = null,
    override var routingNumber: String? = null,
    @Column(value = "first_name")
    var firstName: String? = null,

    @Column(value = "last_name")
    var lastName: String? = null
) : BankAccount()