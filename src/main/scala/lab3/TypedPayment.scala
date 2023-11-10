package lab3

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import lab2.TypedCheckout

object TypedPayment {

  sealed trait Command

  case object DoPayment extends Command
}

class TypedPayment(method: String, orderManager: ActorRef[OrderManager.Command], checkout: ActorRef[TypedCheckout.Command]) {

  import TypedPayment._

  def start: Behavior[TypedPayment.Command] = Behaviors.receive((_, msg) => msg match {
    case DoPayment =>
      println(method)
      orderManager ! OrderManager.ConfirmPaymentReceived
      checkout ! TypedCheckout.ConfirmPaymentReceived
      Behaviors.stopped
  })
}
