package org.rakam.plugin;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.rakam.collection.Event;
import org.rakam.util.RakamException;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

public interface SyncEventStore
        extends EventStore
{
    ThreadPoolExecutor workerGroup = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors() * 6,
            60L, TimeUnit.SECONDS,
            // SynchronousQueue
            new ArrayBlockingQueue<>(Runtime.getRuntime().availableProcessors() * 6 * 20));

    default CompletableFuture<Void> storeAsync(Event event)
    {
        try {
            return CompletableFuture.supplyAsync(() -> {
                store(event);
                return null;
            }, workerGroup);
        }
        catch (RejectedExecutionException e) {
            throw new RakamException("The server is busy, please try again later", INTERNAL_SERVER_ERROR);
        }
    }

    default CompletableFuture<int[]> storeBatchAsync(List<Event> events)
    {
        try {
            return CompletableFuture.supplyAsync(() -> storeBatch(events), workerGroup);
        }
        catch (RejectedExecutionException e) {
            throw new RakamException("The server is busy, please try again later", INTERNAL_SERVER_ERROR);
        }
    }

    void store(Event event);

    int[] storeBatch(List<Event> events);
}
