/*
 * Copyright (C) 2016 Daniel Himmelein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Portions of this file are modified versions of
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/CompletionStage.java?revision=1.39
 * which contained the following notice:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package mindroid.util.concurrent;

import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import mindroid.os.Handler;

public interface Future<T> {
    /**
     * Returns {@code true} if completed in any fashion: normally,
     * exceptionally, or via cancellation.
     *
     * @return {@code true} if completed
     */
    public boolean isDone();

    /**
     * Waits if necessary for this Future to complete, and then
     * returns its result.
     *
     * @return the result value
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException if this future completed exceptionally
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
    public T get() throws CancellationException, ExecutionException, InterruptedException;

    /**
     * Waits if necessary for at most the given time for this Future
     * to complete, and then returns its result, if available.
     *
     * @param timeout the maximum time to wait in milliseconds
     * @return the result value
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException if this future completed exceptionally
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     * @throws TimeoutException if the wait timed out
     */
    public T get(long timeout) throws CancellationException, ExecutionException, TimeoutException, InterruptedException;

    /**
     * If not already completed, completes this Future with
     * a {@link CancellationException}. Dependent Futures
     * that have not already completed will also complete
     * exceptionally, with a {@link CompletionException} caused by
     * this {@code CancellationException}.
     *
     * @return {@code true} if this task is now cancelled
     */
    public boolean cancel();

    /**
     * Returns {@code true} if this Future was cancelled
     * before it completed normally.
     *
     * @return {@code true} if this Future was cancelled
     * before it completed normally
     */
    public boolean isCancelled();

    /**
     * Returns {@code true} if this Future completed
     * exceptionally, in any way. Possible causes include
     * cancellation, explicit invocation of {@code
     * completeWith(Throwable)}, and abrupt termination of a
     * Future action.
     *
     * @return {@code true} if this Future completed
     * exceptionally
     */
    public boolean isCompletedExceptionally();

    /**
     * Returns a new Future that, when this Future completes
     * normally, is executed using this Future's default asynchronous
     * execution facility, with this Future's result as the argument to
     * the supplied function.
     *
     * See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param function the Function to use to compute the value of the
     * returned Future
     * @param <U> the function's return type
     * @return the new Future
     */
    public <U> Future<U> thenApply(Function<? super T, ? extends U> function);

    /**
     * @see #thenApply(Function)
     */
    public <U> Future<U> then(Function<? super T, ? extends U> function);

    /**
     * Returns a new Future that, when this Future completes
     * normally, is executed using the supplied Handler, with this
     * Future's result as the argument to the supplied function.
     *
     * See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Future
     * @param <U> the function's return type
     * @return the new Future
     */
    public <U> Future<U> thenApply(Handler handler, Function<? super T, ? extends U> function);

    /**
     * @see #thenApply(Handler, Function)
     */
    public <U> Future<U> then(Handler handler, Function<? super T, ? extends U> function);

    /**
     * Returns a new Future that, when this Future completes
     * normally, is executed using the supplied Executor, with this
     * Future's result as the argument to the supplied function.
     *
     * See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Future
     * @param <U> the function's return type
     * @return the new Future
     */
    public <U> Future<U> thenApply(Executor executor, Function<? super T, ? extends U> function);

    /**
     * @see #thenApply(Executor, Function)
     */
    public <U> Future<U> then(Executor executor, Function<? super T, ? extends U> function);

    /**
     * Returns a new Future that, when this Future completes
     * either normally or exceptionally, is executed using this Future's
     * default asynchronous execution facility, with this Future's
     * result and exception as arguments to the supplied function.
     *
     * <p>When this Future is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this Future as arguments, and the
     * function's result is used to complete the returned Future.
     *
     * @param function the Function to use to compute the value of the
     * returned Future
     * @param <U> the function's return type
     * @return the new Future
     */
    public <U> Future<U> thenApply(BiFunction<? super T, Throwable, ? extends U> function);

    /**
     * @see #thenApply(BiFunction)
     */
    public <U> Future<U> then(BiFunction<? super T, Throwable, ? extends U> function);

    /**
     * Returns a new Future that, when this Future completes
     * either normally or exceptionally, is executed using the
     * supplied Handler, with this Future's result and exception as
     * arguments to the supplied function.
     *
     * <p>When this Future is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this Future as arguments, and the
     * function's result is used to complete the returned Future.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Future
     * @param <U> the function's return type
     * @return the new Future
     */
    public <U> Future<U> thenApply(Handler handler, BiFunction<? super T, Throwable, ? extends U> function);

    /**
     * @see #thenApply(Handler, BiFunction)
     */
    public <U> Future<U> then(Handler handler, BiFunction<? super T, Throwable, ? extends U> function);

    /**
     * Returns a new Future that, when this Future completes
     * either normally or exceptionally, is executed using the
     * supplied Executor, with this Future's result and exception as
     * arguments to the supplied function.
     *
     * <p>When this Future is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this Future as arguments, and the
     * function's result is used to complete the returned Future.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Future
     * @param <U> the function's return type
     * @return the new Future
     */
    public <U> Future<U> thenApply(Executor executor, BiFunction<? super T, Throwable, ? extends U> function);

    /**
     * @see #thenApply(Executor, BiFunction)
     */
    public <U> Future<U> then(Executor executor, BiFunction<? super T, Throwable, ? extends U> function);

    /**
     * Returns a new Future that is completed with the same
     * value as the Future returned by the given function,
     * executed using this Future's default asynchronous execution
     * facility.
     *
     * <p>When this Future completes normally, the given function is
     * invoked with this Future's result as the argument, returning
     * another Future.  When that Future completes normally,
     * the Future returned by this method is completed with
     * the same value.
     *
     * <p>To ensure progress, the supplied function must arrange
     * eventual completion of its result.
     *
     * <p>See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param function the Function to use to compute another Future
     * @param <U> the type of the returned Future's result
     * @return the new Future
     */
    public <U> Future<U> thenCompose(Function<? super T, ? extends Future<U>> function);

    /**
     * Returns a new Future that is completed with the same
     * value as the Future returned by the given function,
     * executed using the supplied Handler.
     *
     * <p>When this Future completes normally, the given function is
     * invoked with this Future's result as the argument, returning
     * another Future.  When that Future completes normally,
     * the Future returned by this method is completed with
     * the same value.
     *
     * <p>To ensure progress, the supplied function must arrange
     * eventual completion of its result.
     *
     * <p>See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param function the Function to use to compute another Future
     * @param <U> the type of the returned Future's result
     * @return the new Future
     */
    public <U> Future<U> thenCompose(Handler handler, Function<? super T, ? extends Future<U>> function);

    /**
     * Returns a new Future that is completed with the same
     * value as the Future returned by the given function,
     * executed using the supplied Executor.
     *
     * <p>When this Future completes normally, the given function is
     * invoked with this Future's result as the argument, returning
     * another Future.  When that Future completes normally,
     * the Future returned by this method is completed with
     * the same value.
     *
     * <p>To ensure progress, the supplied function must arrange
     * eventual completion of its result.
     *
     * <p>See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param function the Function to use to compute another Future
     * @param <U> the type of the returned Future's result
     * @return the new Future
     */
    public <U> Future<U> thenCompose(Executor executor, Function<? super T, ? extends Future<U>> function);

    /**
     * Returns a new Future that is completed with the same
     * value as the Future returned by the given function,
     * executed using this Future's default asynchronous execution
     * facility.
     *
     * <p>When this Future is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this Future as arguments, returning
     * another Future.  When that Future completes normally,
     * the Future returned by this method is completed with
     * the same value.
     *
     * <p>To ensure progress, the supplied function must arrange
     * eventual completion of its result.
     *
     * <p>See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param function the Function to use to compute another Future
     * @param <U> the type of the returned Future's result
     * @return the new Future
     */
    public <U> Future<U> thenCompose(BiFunction<? super T, Throwable, ? extends Future<U>> function);

    /**
     * Returns a new Future that is completed with the same
     * value as the Future returned by the given function,
     * executed using the supplied Handler.
     *
     * <p>When this Future is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this Future as arguments, returning
     * another Future.  When that Future completes normally,
     * the Future returned by this method is completed with
     * the same value.
     *
     * <p>To ensure progress, the supplied function must arrange
     * eventual completion of its result.
     *
     * <p>See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param function the Function to use to compute another Future
     * @param <U> the type of the returned Future's result
     * @return the new Future
     */
    public <U> Future<U> thenCompose(Handler handler, BiFunction<? super T, Throwable, ? extends Future<U>> function);

    /**
     * Returns a new Future that is completed with the same
     * value as the Future returned by the given function,
     * executed using the supplied Executor.
     *
     * <p>When this Future is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this Future as arguments, returning
     * another Future.  When that Future completes normally,
     * the Future returned by this method is completed with
     * the same value.
     *
     * <p>To ensure progress, the supplied function must arrange
     * eventual completion of its result.
     *
     * <p>See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param function the Function to use to compute another Future
     * @param <U> the type of the returned Future's result
     * @return the new Future
     */
    public <U> Future<U> thenCompose(Executor executor, BiFunction<? super T, Throwable, ? extends Future<U>> function);

    /**
     * Returns a new Future that, when this Future completes
     * normally, is executed using this Future's default asynchronous
     * execution facility, with this Future's result as the argument to
     * the supplied action.
     *
     * See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param action the action to perform before completing the
     * returned Future
     * @return the new Future
     */
    public Future<T> thenAccept(Consumer<? super T> action);

    /**
     * @see #thenAccept(Consumer)
     */
    public Future<T> then(Consumer<? super T> action);

    /**
     * Returns a new Future that, when this Future completes
     * normally, is executed using the supplied Handler, with this
     * Future's result as the argument to the supplied action.
     *
     * See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param action the action to perform before completing the
     * returned Future
     * @return the new Future
     */
    public Future<T> thenAccept(Handler handler, Consumer<? super T> action);

    /**
     * @see #thenAccept(Handler, Consumer)
     */
    public Future<T> then(Handler handler, Consumer<? super T> action);

    /**
     * Returns a new Future that, when this Future completes
     * normally, is executed using the supplied Executor, with this
     * Future's result as the argument to the supplied action.
     *
     * See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param action the action to perform before completing the
     * returned Future
     * @return the new Future
     */
    public Future<T> thenAccept(Executor executor, Consumer<? super T> action);

    /**
     * @see #thenAccept(Executor, Consumer)
     */
    public Future<T> then(Executor executor, Consumer<? super T> action);

    /**
     * Returns a new Future with the same result or exception as
     * this Future, that executes the given action using this Future's
     * default asynchronous execution facility when this Future completes.
     *
     * <p>When this Future is complete, the given action is invoked with the
     * result (or {@code null} if none) and the exception (or {@code null}
     * if none) of this Future as arguments.  The returned Future is completed
     * when the action returns.
     *
     * @param action the action to perform
     * @return the new Future
     */
    public Future<T> thenAccept(BiConsumer<? super T, ? super Throwable> action);

    /**
     * @see #thenAccept(BiConsumer)
     */
    public Future<T> then(BiConsumer<? super T, ? super Throwable> action);

    /**
     * Returns a new Future with the same result or exception as
     * this Future, that executes the given action using the supplied
     * Handler when this Future completes.
     *
     * <p>When this Future is complete, the given action is invoked with the
     * result (or {@code null} if none) and the exception (or {@code null}
     * if none) of this Future as arguments.  The returned Future is completed
     * when the action returns.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param action the action to perform
     * @return the new Future
     */
    public Future<T> thenAccept(Handler handler, BiConsumer<? super T, ? super Throwable> action);

    /**
     * @see #thenAccept(Handler, BiConsumer)
     */
    public Future<T> then(Handler handler, BiConsumer<? super T, ? super Throwable> action);

    /**
     * Returns a new Future with the same result or exception as
     * this Future, that executes the given action using the supplied
     * Executor when this Future completes.
     *
     * <p>When this Future is complete, the given action is invoked with the
     * result (or {@code null} if none) and the exception (or {@code null}
     * if none) of this Future as arguments.  The returned Future is completed
     * when the action returns.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param action the action to perform
     * @return the new Future
     */
    public Future<T> thenAccept(Executor executor, BiConsumer<? super T, ? super Throwable> action);

    /**
     * @see #thenAccept(Executor, BiConsumer)
     */
    public Future<T> then(Executor executor, BiConsumer<? super T, ? super Throwable> action);

    /**
     * Returns a new Future that, when this Future completes
     * normally, executes the given action using this Future's default
     * asynchronous execution facility.
     *
     * See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param action the action to perform before completing the
     * returned Future
     * @return the new Future
     */
    public Future<T> thenRun(Runnable action);

    /**
     * @see #thenRun(Runnable)
     */
    public Future<T> then(Runnable action);

    /**
     * Returns a new Future that, when this Future completes
     * normally, executes the given action using the supplied Handler.
     *
     * See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param action the action to perform before completing the
     * returned Future
     * @return the new Future
     */
    public Future<T> thenRun(Handler handler, Runnable action);

    /**
     * @see #thenRun(Handler, Runnable)
     */
    public Future<T> then(Handler handler, Runnable action);

    /**
     * Returns a new Future that, when this Future completes
     * normally, executes the given action using the supplied Executor.
     *
     * See the {@link Future} documentation for rules
     * covering exceptional completion.
     *
     * @param executor the executor to use for asynchronous execution
     * @param action the action to perform before completing the
     * returned Future
     * @return the new Future
     */
    public Future<T> thenRun(Executor executor, Runnable action);

    /**
     * @see #thenRun(Executor, Runnable)
     */
    public Future<T> then(Executor executor, Runnable action);

    /**
     * Returns a new Future that, when this Future completes
     * exceptionally, is executed with this Future's exception as the
     * argument to the supplied function using this Future's
     * default asynchronous execution.  Otherwise, if this Future
     * completes normally, then the returned Future also completes
     * normally with the same value.
     *
     * @param function the Function to use to compute the value of the
     * returned Future if this Future completed
     * exceptionally
     * @return the new Future
     */
    public Future<T> catchException(Function<Throwable, ? extends T> function);

    /**
     * Returns a new Future that, when this Future completes
     * exceptionally, is executed with this Future's exception as the
     * argument to the supplied function using the supplied
     * Handler.  Otherwise, if this Future completes normally,
     * then the returned Future also completes normally with the same value.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Future if this Future completed
     * exceptionally
     * @return the new Future
     */
    public Future<T> catchException(Handler handler, Function<Throwable, ? extends T> function);

    /**
     * Returns a new Future that, when this Future completes
     * exceptionally, is executed with this Future's exception as the
     * argument to the supplied function using the supplied
     * Executor.  Otherwise, if this Future completes normally,
     * then the returned Future also completes normally with the same value.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Future if this Future completed
     * exceptionally
     * @return the new Future
     */
    public Future<T> catchException(Executor executor, Function<Throwable, ? extends T> function);

    public Future<T> catchException(Consumer<Throwable> action);

    public Future<T> catchException(Handler handler, Consumer<Throwable> action);

    public Future<T> catchException(Executor executor, Consumer<Throwable> action);

    public Future<T> logUncaughtException();

    /**
     * Exceptionally completes this Future with a {@link TimeoutException}
     * if not otherwise completed before the given timeout.
     *
     * @param timeout how long to wait before completing exceptionally
     *        with a TimeoutException in milliseconds
     * @return this Future
     */
    public Future<T> orTimeout(long timeout);

    /**
     * Completes this Future with the given value if not otherwise
     * completed before the given timeout.
     *
     * @param value the value to use upon timeout
     * @param timeout how long to wait before completing normally
     *        with the given value in milliseconds
     * @return this Future
     */
    public Future<T> completeOnTimeout(T value, long timeout);

    public Future<T> delay(long delay);

    /**
     * Returns a {@link Promise} maintaining the same
     * completion properties as this Future. If this Future is already a
     * Promise, this method may return this Promise itself.
     * Otherwise, invocation of this method may be equivalent in
     * effect to {@code thenApply(x -> x)}, but returning an instance
     * of type {@code Promise}.
     *
     * @return the Promise
     */
    public Promise<T> toPromise();
}
