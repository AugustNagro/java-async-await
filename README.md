## Java Async-Await

Async-Await support for Java [CompletionStage](https://download.java.net/java/early_access/loom/docs/api/java.base/java/util/concurrent/CompletionStage.html).

```java
import com.augustnagro.jaa.AsyncContext;
import static com.augustnagro.jaa.Async.async;

CompletableFuture<byte[]> pdfForUsers = async((AsyncContext ctx) -> {
  List<Long> userIds = ctx.await(userIdsFromDb());

  List<String> userNames = userIds.stream()
  .map(id -> ctx.await(userNamesFromSomeApi(id)))
  .toList();

  byte[] pdf = ctx.await(buildPdf(userNames));
  
  System.out.println("Generated pdf for user ids: " + userIds);
  return pdf;
});
```

vs.

```java
CompletionStage<byte[]> userPdf = userIdsFromDb().thenCompose(userIds -> {

  // note that we could flood the api here by starting all futures at once!
  List<CompletableFuture<String>> userNameFutures = userIds.stream()
    .map(id -> userNamesFromSomeApi(id))
    .map(CompletionStage::toCompletableFuture)
    .toList();

  return CompletableFuture.allOf(userNameFutures.toArray(new CompletableFuture[0]))
    .thenCompose(voidd -> {

      List<String> userNames = userNameFutures.stream()
        .map(CompletableFuture::join)
        .toList();

      return buildPdf(userNames)
        .thenApply(pdf -> {
          System.out.println("Generated pdf for user ids: " + userIds);
          return pdf;
        });
    });
});
```

## Maven Coordinates

```xml
<dependency>
  <groupId>com.augustnagro</groupId>
  <artifactId>java-async-await</artifactId>
  <version>0.1.0</version>
</dependency>
```

This library requires the latest [JDK 18 Loom Preview Build](http://jdk.java.net/loom/) and depends on `vertx-core` v. 4.1.5.

## Docs:

Within an `async` scope, you can `await` CompletionStages (which most Future types extend or convert to) and program in an imperative style. `async` and `await` calls can be nested to any depth.

## Why Async-Await vs the CompletableFuture higher-order-function API?

Abstractions like Future, Rx, ZIO, Uni, etc, are great. They provide convenient functions, like handling timeout and retries.

However, there are serious downsides to implementing your code in a fully 'monadic' style:

* Often it's difficult to express something with Futures, when it is trivial with simple blocking code. 
* `flatMap` and its aliases like `thenCompose` are generally not stack-safe, and will StackOverflow if you recurse far enough (see unit tests for an example).
* It's hard to debug big Future chains in IDEs
* Stack traces are often meaningless.

Project Loom solves all four issues.

## Why Async-Await vs using synchronous APIs on Virtual Threads and dropping async APIs entirely?

* You lose concurrency features like timeouts and retry offered by abstractions like Rx, Uni, etc.
* Maybe you're already using Async libraries; the effort to migrate back to sync is gigantic, whereas introducing Async-Await can be done incrementally.
* Async library authors care more about performance; regardless of concurrency, these libraries are faster and higher quality.

## Alternatives

* Java bytecode manipulation: https://github.com/electronicarts/ea-async
* Scala 3 Async-Await macro: https://github.com/rssh/dotty-cps-async
* Scala 3 Monadic Reflection: https://github.com/lampepfl/monadic-reflection
* Kotlin Coroutines (another form of bytecode manipulation)
