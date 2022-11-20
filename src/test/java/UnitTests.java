import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.augustnagro.jaa.Async.async;
import static com.augustnagro.jaa.Async.await;
import static org.junit.Assert.assertEquals;

public class UnitTests {

  CompletionStage<List<Long>> userIdsFromDb() {
    return CompletableFuture.completedFuture(List.of(1L, 2L, 3L));
  }

  CompletionStage<String> userNamesFromSomeApi(Long userId) {
    return CompletableFuture.supplyAsync(() -> "User " + userId);
  }

  CompletionStage<byte[]> buildPdf(List<String> userNames) {
    return CompletableFuture.completedFuture(new byte[0]);
  }

  @Test
  public void testSimpleAsyncAwait() {
    CompletableFuture<byte[]> future = async(() -> {
      List<Long> userIds = await(userIdsFromDb());

      List<String> userNames = userIds.stream()
        .map(id -> await(userNamesFromSomeApi(id)))
        .toList();

      byte[] pdf = await(buildPdf(userNames));

      System.out.println("Generated pdf for user ids: " + userIds);
      return pdf;
    });

    assertEquals(0, future.join().length);
  }

  @Test
  public void testSimple() {
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

    assertEquals(0, userPdf.toCompletableFuture().join().length);
  }


  @Test
  public void testAwaitNesting() throws ExecutionException, InterruptedException {
    Future<Integer> future = async(() -> {
      List<Long> ids1 = await(userIdsFromDb());
      List<Long> ids2 = await(async(() -> await(userIdsFromDb())));

      return ids1.size() + ids2.size();
    });

    assertEquals(6L, future.get().longValue());
  }

  @Test
  public void testAwaitForLoop() throws ExecutionException, InterruptedException {
    Future<String> future = async(() -> {
      ArrayList<String> userNames = new ArrayList<>();
      for (long userId = 1; userId <= 1000; ++userId) {
        userNames.add(await(userNamesFromSomeApi(userId)));
      }

      return userNames.get(userNames.size() - 1);
    });

    assertEquals("User 1000", future.get());
  }

  private static final long RECURSE_ITERATIONS = 20_000;

  @Test
  public void testRecursiveAwait() {
    CompletableFuture<Long> future = recurseAsync(RECURSE_ITERATIONS, 0);

    assertEquals(RECURSE_ITERATIONS, future.join().longValue());
  }

  private CompletableFuture<Long> recurseAsync(long iterations, long result) {
    return async(() -> {
      if (iterations == 0) {
        return result;
      } else {
        Long newResult = await(calculateNewResult(result));
        return await(recurseAsync(iterations - 1, newResult));
      }
    });
  }

  private CompletionStage<Long> calculateNewResult(long oldResult) {
    return CompletableFuture.completedFuture(oldResult + 1);
  }


  @Test
  @Ignore
  public void testRecursiveFlatMap() {
    CompletionStage<Long> future = recurseFlatMap(RECURSE_ITERATIONS, 0);
    assertEquals(RECURSE_ITERATIONS, future.toCompletableFuture().join().longValue());
  }

  private CompletionStage<Long> recurseFlatMap(long iterations, long result) {
    CompletionStage<Long> newResult = calculateNewResult(result);
    return newResult.thenCompose(nr -> recurseFlatMap(iterations - 1, nr));
  }


}
