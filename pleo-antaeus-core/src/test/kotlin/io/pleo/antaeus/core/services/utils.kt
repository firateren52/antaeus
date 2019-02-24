import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoicePaymentTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.lang.RuntimeException
import java.math.BigDecimal
import java.sql.Connection
import kotlin.random.Random

internal fun getAntaeusDal(): AntaeusDal {
// The tables to create in the database.
    val tables = arrayOf(InvoiceTable, InvoicePaymentTable, CustomerTable)

    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
            .connect("jdbc:sqlite:/tmp/data.db", "org.sqlite.JDBC")
            .also {
                TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                transaction(it) {
                    addLogger(StdOutSqlLogger)
                    // Drop all existing tables to ensure a clean slate on each run
                    SchemaUtils.drop(*tables)
                    // Create all tables
                    SchemaUtils.create(*tables)
                }
            }

    // Set up data access layer.
    return AntaeusDal(db = db)
}

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal, customerCount: Int = 100, invoicePerCustomerCount: Int = 10) {
    val customers = (1..customerCount).mapNotNull {
        dal.createCustomer(
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..invoicePerCustomerCount).forEach {
            dal.createInvoice(
                    amount = Money(
                            value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                            currency = customer.currency
                    ),
                    customer = customer,
                    status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider with exceptions
internal fun getPaymentProviderWithExceptions(percentage: Int =  Random.nextInt(100) % 100): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            if (percentage < 5) {
                throw CurrencyMismatchException(invoice.id, invoice.customerId)
            } else if (percentage < 10) {
                throw CustomerNotFoundException(invoice.customerId)
            } else if (percentage < 15) {
                throw NetworkException()
            } else if (percentage < 20) {
                throw RuntimeException()
            } else if (percentage < 40) {
                return false
            }
            return true
        }
    }
}

internal fun assertInvoicePayment(invoicePayment: InvoicePayment?, invoiceId: Int, status: InvoicePaymentStatus) {
    assertTrue(invoicePayment != null && invoicePayment.id > 0)
    assertEquals(invoiceId, invoicePayment?.invoiceId)
    assertEquals(status, invoicePayment?.status)
}

