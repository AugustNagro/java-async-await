package com.augustnagro.jaa;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Must be run in a Virtual Thread, so that blocking until the
 * CompletionStage completes does not degrade performance.
 */
class Coroutine implements AsyncContext {

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition cond = lock.newCondition();

  @Override
  public <A> A await(CompletionStage<A> stage) {
    lock.lock();
    try {

      stage.whenCompleteAsync((A res, Throwable ex) -> {
        lock.lock();
        try {
          cond.signal();
        } finally {
          lock.unlock();
        }
      });

      cond.await();

      return stage.toCompletableFuture().join();

    } catch (InterruptedException e) {
      throw new RuntimeException(e);

    } finally {
      lock.unlock();
    }
  }
}
