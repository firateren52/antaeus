package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoicePayment
import io.pleo.antaeus.models.InvoicePaymentStatus
import io.pleo.antaeus.models.InvoiceStatus
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

class BillingService private constructor(val paymentProvider: PaymentProvider,
                                         val invoiceService: InvoiceService,
                                         val invoicePaymentService: InvoicePaymentService) {
    companion object {
        private var INSTANCE: BillingService? = null
        fun getInstance(paymentProvider: PaymentProvider, invoiceService: InvoiceService, invoicePaymentService: InvoicePaymentService): BillingService {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = BillingService(paymentProvider, invoiceService, invoicePaymentService)
                }
                return INSTANCE!!
            }
        }
    }

    private val log = LoggerFactory.getLogger("BillingService")
    private val TIMER_INITIAL_DELAY = 1L
    private val TIMER_DELAY = 60L

    init {
        // call chargeInvoices in one hour delay
        val scheduledExecutor = Executors.newScheduledThreadPool(1)
        scheduledExecutor.scheduleWithFixedDelay({ chargeInvoices() }, TIMER_INITIAL_DELAY, TIMER_DELAY, TimeUnit.MINUTES)
    }

    fun chargeInvoices() {
        runBlocking {
            runBlocking {
                try {
                    log.info("chargeInvoices started")
                    val invoices = invoiceService.fetchAllByStatusAndStartDate(InvoiceStatus.PENDING, DateTime.now())
                    val jobs = mutableListOf<Job>()
                    for (invoice in invoices) {
                        val job = GlobalScope.launch {
                            chargeInvoice(invoice)
                        }
                        jobs.add(job)
                    }
                    jobs.forEach { it.join() }
                    log.info("chargeInvoices finished")
                } catch (e: Exception) {
                    log.error("chargeInvoices failed", e)
                }
            }
        }
    }

    fun chargeInvoice(invoice: Invoice): InvoicePayment? {
        log.debug("chargeInvoice started for invoice '${invoice.id}'")
        var invoicePaymentStatus: InvoicePaymentStatus
        try {
            if (paymentProvider.charge(invoice)) {
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
                invoicePaymentStatus = InvoicePaymentStatus.SUCCESS
            } else {
                log.warn("chargeInvoice failed for invoice '$invoice.id'")
                invoicePaymentStatus = InvoicePaymentStatus.FAIL
            }
        } catch (e: Exception) {
            log.error("chargeInvoice failed for invoice '$invoice.id'", e)
            invoicePaymentStatus = invoicePaymentService.getPaymentStatus(e)
        }
        val invoicePayment = invoicePaymentService.create(invoice, invoicePaymentStatus)
        if (invoicePaymentStatus.attemptCount > 0) {
            val invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(invoice.id, invoicePaymentStatus)
            if (invoicePayments.size >= invoicePaymentStatus.attemptCount) {
                //sets invoice status uncollectible if the number of invoice payments for status exceeded the status attemptCount value
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.UNCOLLECTIBLE)
            } else {
                // postpone invoice startDate by plus seconds
                invoiceService.updateStartDate(invoice.id, invoice.startDate.plusSeconds(invoicePaymentStatus.delaySec))
            }
        }
        log.debug("chargeInvoice finished for invoice '${invoice.id}'")
        return invoicePayment
    }

}