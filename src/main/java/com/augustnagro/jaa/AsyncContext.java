package com.augustnagro.jaa;

import java.util.concurrent.CompletionStage;

/**
 * Context of an async block, enabling await() on CompletionStages.
 */
public interface AsyncContext {

  /** Await this CompletionStage. */
  <A> A await(CompletionStage<A> stage);
}
