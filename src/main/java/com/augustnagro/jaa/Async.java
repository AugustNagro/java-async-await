package com.augustnagro.jaa;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class Async {

  public static <A> CompletableFuture<A> async(Function<AsyncContext, A> fn) {
    CompletableFuture<A> promise = new CompletableFuture<>();
    Thread.ofVirtual().start(() -> {
      try {
        promise.complete(fn.apply(new Coroutine()));
      } catch (Exception e) {
        promise.completeExceptionally(e);
      }
    });
    return promise;
  }
}
