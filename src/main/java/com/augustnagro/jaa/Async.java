package com.augustnagro.jaa;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Async {

  // todo refactor to use ScopeLocal when it's no longer a draft JEP.
  // https://openjdk.java.net/jeps/8263012
  private static final ThreadLocal<Coroutine> AWAIT_CONTEXT = new ThreadLocal<>();

  public static <A> CompletableFuture<A> async(Callable<A> fn) {
    CompletableFuture<A> promise = new CompletableFuture<>();
    Thread.startVirtualThread(() -> {
      try {
        AWAIT_CONTEXT.set(new Coroutine());
        promise.complete(fn.call());
      } catch (Throwable t) {
        promise.completeExceptionally(t);
      } finally {
        AWAIT_CONTEXT.remove();
      }
    });
    return promise;
  }

  public static <A> A await(CompletionStage<A> stage) {
    Coroutine coroutine = Objects.requireNonNull(
      AWAIT_CONTEXT.get(),
      "await must be called within an async block"
    );

    return coroutine.await(stage);
  }
}
