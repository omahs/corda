package net.corda.finance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import java.util.*

/**
 * Initiates a flow that self-issues cash and then send this to a recipient.
 *
 * We issue cash only to ourselves so that all KYC/AML checks on payments are enforced consistently, rather than risk
 * checks for issuance and payments differing. Outside of test scenarios it would be extremely unusual to issue cash
 * and immediately transfer it, so impact of this limitation is considered minimal.
 *
 * @param amount the amount of currency to issue.
 * @param issueRef a reference to put on the issued currency.
 * @param recipient the recipient of the currency
 * @param anonymous if true, the recipient of the cash will be anonymous. Should be true for normal usage
 * @param notary the notary to set on the output states.
 */
@StartableByRPC
class MassCashPaymentFlow(val numberOfPayments: Int,
                          val amount: Amount<Currency>,
                          val recipient: Party,
                          val anonymous: Boolean,
                          val notary: Party,
                          progressTracker: ProgressTracker) : AbstractCashFlow<AbstractCashFlow.Result>(progressTracker) {
    constructor(numberOfPayments: Int,
                amount: Amount<Currency>,
                recipient: Party,
                anonymous: Boolean,
                notary: Party) : this(numberOfPayments, amount, recipient, anonymous, notary, tracker())

    constructor(request: MassIssueAndPaymentRequest) : this(request.numberOfPayments, request.amount, request.recipient, request.anonymous, request.notary, tracker())

    companion object {
        val PAYING_RECIPIENT = ProgressTracker.Step("Paying recipient")

        fun tracker() = ProgressTracker(PAYING_RECIPIENT)
    }

    @Suspendable
    override fun call(): Result {
        progressTracker.currentStep = PAYING_RECIPIENT
        for (i in 1 until numberOfPayments) {
            subFlow(CashPaymentFlow(amount, recipient, anonymous, notary))
        }
        return subFlow(CashPaymentFlow(amount, recipient, anonymous, notary))
    }

    @CordaSerializable
    class MassIssueAndPaymentRequest(val numberOfPayments: Int,
                                     amount: Amount<Currency>,
                                     val recipient: Party,
                                     val notary: Party,
                                     val anonymous: Boolean) : AbstractRequest(amount)
}
