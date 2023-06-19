## Java Async-Await

Async-Await support for Java [CompletionStage](https://download.java.net/java/early_access/loom/docs/api/java.base/java/util/concurrent/CompletionStage.html).

```java
import com.augustnagro.jaa.AsyncContext;
import static com.augustnagro.jaa.Async.async;
import static com.augustnagro.jaa.Async.await;

CompletableFuture<byte[]> pdfForUsers = async(() -> {
  List<Long> userIds = await(userIdsFromDb());

  List<String> userNames = userIds.stream()
    .map(id -> await(userNamesFromSomeApi(id)))
    .toList();

  byte[] pdf = await(buildPdf(userNames));  //buildPdf returns CompletableFuture<byte[]>
  
  System.out.println("Generated pdf for user ids: " + userIds);
  return pdf;
});
```

vs.

```java
CompletionStage<byte[]> userPdf = userIdsFromDb().thenCompose(userIds -> {

  CompletionStage<List<String>> userNamesFuture =
    CompletableFuture.supplyAsync(ArrayList::new);

  for (Long userId : userIds) {
    userNamesFuture = userNamesFuture.thenCompose(list -> {
      return userNamesFromSomeApi(userId)
        .thenApply(userName -> {
          list.add(userName);
          return list;
        });
    });
  }

  return userNamesFuture.thenCompose(userNames -> {
    return buildPdf(userNames).thenApply(pdf -> {
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
  <version>0.3.0</version>
</dependency>
```

This library requires [JDK 20](https://jdk.java.net/20/) with `--enable-preview`, and has no dependencies.

## Docs:

Within an `async` scope, you can `await` CompletionStages and program in an imperative style. Most Future apis implement CompletionStage, or provide conversions. `async` and `await` calls can be nested to any depth.

## Why Async-Await vs the higher-order Future API?

Abstractions like Future, Rx, ZIO, Uni, etc, are great. They provide convenient functions, like handling timeout and retries.

However, there are serious downsides to implementing business logic in a fully 'monadic' style:

* Often it's difficult to express something with Futures, when it is trivial with simple blocking code. 
* `flatMap` and its aliases like `thenCompose` are generally not stack-safe, and will StackOverflow if you recurse far enough (see unit tests for example).
* It's hard to debug big Future chains in IDEs
* Stack traces are often meaningless.
* Future is 'viral', infecting your codebase.

Project Loom solves all five issues, although Async-Await only solves the first 3. Stack traces are significantly better then using higher-order functions, but can still lack detail in certain cases. Since `async` returns `CompletableFuture`, it retains the virility of Future-like apis.

## Why Async-Await vs synchronous APIs on Virtual Threads and dropping async entirely?

* That's not a bad idea.
* You lose the concurrency features like timeout and retry offered by Rx, Uni, ZIO, etc.
* Maybe you're already using Async libraries; the effort to migrate back to sync is gigantic, whereas introducing Async-Await can be done incrementally.

## Alternative Approaches

* Java bytecode manipulation: https://github.com/electronicarts/ea-async
* Scala 3 Async-Await macro: https://github.com/rssh/dotty-cps-async
* Scala 3 Monadic Reflection using Loom: https://github.com/lampepfl/monadic-reflection
* Kotlin Coroutines (another form of bytecode manipulation)
