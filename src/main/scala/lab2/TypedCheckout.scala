package lab2

import lab3.OrderManager
import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import lab3.{OrderManager, TypedPayment}

import scala.concurrent.duration._
import scala.language.postfixOps

object TypedCheckout {

  sealed trait Data

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String, orderManagerRef: ActorRef[OrderManager.Command]) extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command

  sealed trait Event
}

class TypedCheckout {
  import TypedCheckout._

  var deliveryMethod = ""
  var paymentMethod = ""
  val checkoutTimerDuration: FiniteDuration = 10 seconds
  val paymentTimerDuration: FiniteDuration  = 10 seconds

  private def checkoutTimer(context: ActorContext[Command]): Cancellable =
    context.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout)

  private def paymentTimer(context: ActorContext[Command]): Cancellable =
    context.scheduleOnce(paymentTimerDuration, context.self, ExpirePayment)
  def start: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case StartCheckout =>
        selectingDelivery(checkoutTimer(context))
    }
  )

  def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case SelectDeliveryMethod(method: String) =>
        println("damian delivery")
        this.deliveryMethod = method
        timer.cancel()
        println("selectin payment methd")
        selectingPaymentMethod(checkoutTimer(context))

      case ExpireCheckout =>
        timer.cancel()
        println("Checkout expired")
        cancelled

      case CancelCheckout =>
        timer.cancel()
        cancelled
    }
  )

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case SelectPayment(method: String, orderManagerRef) =>
        println("DAMIAN select payment checkout")
        this.paymentMethod = method
        timer.cancel()


        val payment = new TypedPayment(method, orderManagerRef, context.self)

        val paymentRef = context.spawn(payment.start, "payment")
        println("DAMIAN22")
        orderManagerRef ! OrderManager.ConfirmPaymentStarted(paymentRef = paymentRef)
        println("DAMIAN ConfirmPaymentStarted sent")
        processingPayment(paymentTimer(context))

      case ExpireCheckout =>
        timer.cancel()
        println("Checkout expired")
        cancelled

      case CancelCheckout =>
        timer.cancel()
        cancelled
    }
  )

  def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, msg) => msg match {
      case ConfirmPaymentReceived =>
        timer.cancel()
        closed

      case ExpirePayment =>
        timer.cancel()
        println("Payment expired")
        cancelled

      case CancelCheckout =>
        timer.cancel()
        cancelled
    }
  )

  def cancelled: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, _) => {
      Behaviors.same
    }
  )

  def closed: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, _) => {
      println("Checkout closed")
      Behaviors.same
    }
  )

}
