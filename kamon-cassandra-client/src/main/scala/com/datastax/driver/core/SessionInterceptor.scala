package com.datastax.driver.core

import java.util.concurrent.Callable

import kamon.instrumentation.cassandra.client.InstrumentedSession
import kanela.agent.libs.net.bytebuddy.asm.Advice
import kanela.agent.libs.net.bytebuddy.implementation.bind.annotation.SuperCall

object SessionInterceptor {

  @Advice.OnMethodExit
  def wrapSession(@SuperCall session: Callable[Session]): Session = {
    new InstrumentedSession(session.call())
  }
}
