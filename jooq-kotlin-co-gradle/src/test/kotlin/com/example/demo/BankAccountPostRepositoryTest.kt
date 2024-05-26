package com.example.demo

import com.example.demo.domain.JooqConfig
import com.example.demo.domain.R2dbcConfig
import com.example.demo.domain.model.BankAccount
import com.example.demo.domain.model.BusinessBankAccount
import com.example.demo.domain.model.IndividualBankAccount
import com.example.demo.domain.repository.BankAccountRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.allAndAwait
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.MountableFile


@Testcontainers
@DataR2dbcTest()
@Import(JooqConfig::class, R2dbcConfig::class)
class BankAccountPostRepositoryTest {
    companion object {
        private val log = LoggerFactory.getLogger(BankAccountPostRepositoryTest::class.java)


        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:12")
            .withCopyToContainer(
                MountableFile.forClasspathResource("/init.sql"),
                "/docker-entrypoint-initdb.d/init.sql"
            )

        @JvmStatic
        @DynamicPropertySource
        fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgreSQLContainer.host}:${postgreSQLContainer.firstMappedPort}/${postgreSQLContainer.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgreSQLContainer.username }
            registry.add("spring.r2dbc.password") { postgreSQLContainer.password }
        }

    }

    @Autowired
    lateinit var bankAccountRepository: BankAccountRepository

    @Autowired
    lateinit var r2dbcEntityTemplate: R2dbcEntityTemplate

    @BeforeEach
    fun setup() = runTest {
        log.info(" clear sample data ...")
        val deletedPostsCount = r2dbcEntityTemplate.delete(BankAccount::class.java).allAndAwait()
        // postRepository.deleteAll()
        log.debug(" deletedPostsCount: $deletedPostsCount")

    }

    @Test
    fun `query sample data`() = runTest {
        log.debug(" add new sample data...")
        val companyBankAccount = BusinessBankAccount(
            accountNumber = "test",
            routingNumber = "routetest",
            businessName = "business name"
        )
        bankAccountRepository.save(companyBankAccount)

        val personalBankAccount = IndividualBankAccount(
            accountNumber = "test",
            routingNumber = "routetest",
            firstName = "first name",
            lastName = "last name"
        )

        bankAccountRepository.save(personalBankAccount)

        bankAccountRepository.findAll().toList().forEach {
            log.debug("saved bank accounts: $it")
        }
    }

}