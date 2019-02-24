/*
    Implements the data access layer (DAL).
    This file implements the database queries used to fetch and insert rows in our database tables.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

//TODO(firat.eren) refactor this class
class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                    .select { InvoiceTable.id.eq(id) }
                    .firstOrNull()
                    ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .selectAll()
                    .map { it.toInvoice() }
        }
    }

    fun fetchInvoicesByStatusAndStartDate(status: InvoiceStatus, startDate: DateTime): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .select { InvoiceTable.status.eq(status.name).and(InvoiceTable.startDate.less(startDate)) }
                    .map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                    .insert {
                        it[this.value] = amount.value
                        it[this.currency] = amount.currency.toString()
                        it[this.status] = status.toString()
                        it[this.customerId] = customer.id
                        it[this.startDate] = DateTime.now()
                    } get InvoiceTable.id
        }

        return fetchInvoice(id!!)
    }

    fun updateInvoiceStatus(id: Int, status: InvoiceStatus): Int {
        return transaction(db) {
            InvoiceTable
                    .update({ InvoiceTable.id.eq(id) }) {
                        it[this.status] = status.toString()
                    }
        }
    }

    fun updateStartDate(id: Int, startDate: DateTime): Int {
        return transaction(db) {
            InvoiceTable
                    .update({ InvoiceTable.id.eq(id) }) {
                        it[this.startDate] = startDate
                    }
        }
    }

    fun fetchInvoicePayment(id: Int): InvoicePayment? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoicePaymentTable
                    .select { InvoicePaymentTable.id.eq(id) }
                    .firstOrNull()
                    ?.toInvoicePayment()
        }
    }

    fun fetchInvoicePaymentsByInvoiceAndStatus(invoiceId: Int, status: InvoicePaymentStatus): List<InvoicePayment> {
        return transaction(db) {
            InvoicePaymentTable
                    .select { InvoicePaymentTable.invoiceId.eq(invoiceId).and(InvoicePaymentTable.status.eq(status.name)) }
                    .map { it.toInvoicePayment() }
        }
    }

    fun createInvoicePayment(invoice: Invoice, status: InvoicePaymentStatus): InvoicePayment? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoicePaymentTable
                    .insert {
                        it[this.invoiceId] = invoice.id
                        it[this.status] = status.toString()
                        it[this.createDate] = DateTime.now()
                    } get InvoicePaymentTable.id
        }
        return fetchInvoicePayment(id!!)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                    .select { CustomerTable.id.eq(id) }
                    .firstOrNull()
                    ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                    .selectAll()
                    .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id!!)
    }
}
