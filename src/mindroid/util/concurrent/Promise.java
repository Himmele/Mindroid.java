/*
 * Copyright (C) 2013 Daniel Himmelein
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
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/CompletableFuture.java?revision=1.211
 * which contained the following notice:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package mindroid.util.concurrent;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import mindroid.os.Handler;
import mindroid.os.Looper;
import mindroid.os.SystemClock;
import sun.misc.Unsafe;

/**
 * A Promise of a possibly asynchronous computation, that performs an
 * action or computes a value when another Promise completes.
 * A Promise completes upon termination of its computation, but this may
 * in turn trigger other dependent Promises.
 *
 * <ul>
 *
 * <li>The computation performed by a Promise may be expressed as a
 * Function, Consumer, or Runnable (using methods with names including
 * <em>apply</em> and <em>compose</em>, <em>accept</em>, or <em>run</em>,
 * respectively) depending on whether it requires arguments and/or
 * produces results.
 * For example:
 * <pre> {@code
 * promise.thenApply(x -> square(x))
 *      .thenAccept(x -> System.out.print(x))
 *      .thenRun(() -> System.out.println());}</pre>
 *
 * <p>Any argument to a Promise's computation is the outcome of a
 * triggering Promise's computation.
 *
 * <li>Dependencies among Promises control the triggering of
 * computations, but do not otherwise guarantee any particular
 * ordering. Additionally, execution of a new Promise's computations may
 * be arranged in any of two ways: default asynchronous execution,
 * or custom (via a supplied {@link Handler} or {@link Executor}).
 *
 * <li>Two method forms ({@link #thenApply(BiFunction) thenApply(BiFunction)}
 * and {@link #thenAccept(BiConsumer) thenAccept(BiConsumer)}) support
 * unconditional computation whether the triggering Promise completed normally
 * or exceptionally.
 * Method {@link #catchException catchException} supports computation
 * only when the triggering Promise completes exceptionally, computing a
 * replacement result, similarly to the Java {@code catch} keyword.
 * In all other cases, if a Promise's computation terminates abruptly
 * with an (unchecked) exception or error, then all dependent Promises
 * requiring its completion complete exceptionally as well, with a
 * {@link CompletionException} holding the exception as its cause.
 * In the case of method {@code thenApply(BiFunction)} or
 * {@code thenAccept(BiFunction)}, when the supplied action itself
 * encounters an exception, then the Promise completes exceptionally
 * with this exception even when the source Promise also completed exceptionally.
 *
 * </ul>
 *
 * <p>All methods adhere to the above triggering, execution, and
 * exceptional completion specifications (which are not repeated in
 * individual method specifications). Additionally, while arguments
 * used to pass a completion result (that is, for parameters of type
 * {@code T}) for methods accepting them may be null, passing a null
 * value for any other parameter will result in a {@link
 * NullPointerException} being thrown.
 *
 * <p>Method form {@link #thenApply(BiFunction) thenApply(BiFunction)}
 * is the most general way of creating a continuation Promise,
 * unconditionally performing a computation that is given both
 * the result and exception (if any) of the triggering Promise,
 * and computing an arbitrary result.
 * Method {@link #thenAccept(BiConsumer) thenAccept(BiConsumer)} is similar,
 * but preserves the result of the triggering Promise instead of computing
 * a new one.
 * Because a Promise's normal result may be {@code null}, both methods
 * should have a computation structured thus:
 *
 * <pre>{@code (result, exception) -> {
 *   if (exception == null) {
 *     // triggering Promise completed normally
 *   } else {
 *     // triggering Promise completed exceptionally
 *   }
 * }}</pre>
 */
public class Promise<T> implements Future<T> {
    private static final sun.misc.Unsafe UNSAFE;
    private static final long RESULT;
    private static final Object NULL = new Object();

    private final Lock mLock = new ReentrantLock();
    private final Condition mCondition = mLock.newCondition();

    private Executor mExecutor;
    private volatile Object mResult = null;
    private AtomicReference<Queue<Action<?, ?>>> mActions = new AtomicReference<>();

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
            RESULT = UNSAFE.objectFieldOffset(Promise.class.getDeclaredField("mResult"));
        } catch (Exception e) {
            throw new java.lang.Error(e);
        }
    }

    private static class Error {
        Throwable mThrowable;

        public Error(Throwable throwable) {
            mThrowable = throwable;
        }
    }

    /**
     * Creates a new incomplete Promise.
     */
    public Promise() {
        if (Looper.myLooper() != null) {
            mExecutor = new Handler().asExecutor();
        } else {
            mExecutor = null;
        }
    }

    /**
     * Creates a new Promise that is already completed with
     * the given value.
     */
    public Promise(T result) {
        this();
        complete(result);
    }

    /**
     * Creates a new Promise that is already completed with
     * the given throwable.
     */
    public Promise(Throwable throwable) {
        this();
        completeWith(throwable);
    }

    public Promise(final Handler handler) {
        if (handler == null) {
            throw new NullPointerException();
        }
        mExecutor = handler.asExecutor();
    }

    public Promise(final Executor executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        mExecutor = executor;
    }

    public Promise(final Promise<T> supplier) {
        this();
        completeWith(supplier);
    }

    public Promise<T> onHandler(final Handler handler) {
        if (handler == null) {
            throw new NullPointerException();
        }
        mExecutor = handler.asExecutor();
        return this;
    }

    public Promise<T> onExecutor(final Executor executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        mExecutor = executor;
        return this;
    }

    /**
     * Returns {@code true} if completed in any fashion: normally,
     * exceptionally, or via cancellation.
     *
     * @return {@code true} if completed
     */
    public boolean isDone() {
        return mResult != null;
    }

    /**
     * Waits if necessary for this promise to complete, and then
     * returns its result.
     *
     * @return the result value
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException if this future completed exceptionally
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
    public T get() throws CancellationException, ExecutionException, InterruptedException {
        while (!isDone()) {
            mLock.lock();
            try {
                mCondition.await();
            } catch (InterruptedException e) {
                throw e;
            } finally {
                mLock.unlock();
            }
        }
        if (mResult == NULL) {
            return null;
        }
        if (mResult instanceof Promise.Error) {
            Throwable throwable = ((Promise.Error) mResult).mThrowable;
            if (throwable instanceof CancellationException) {
                throw (CancellationException) throwable;
            }
            Throwable cause;
            if ((throwable instanceof CompletionException) &&
                    (cause = throwable.getCause()) != null) {
                throwable = cause;
            }
            throw new ExecutionException(throwable);
        }
        @SuppressWarnings("unchecked") T result = (T) mResult;
        return result;
    }

    /**
     * Waits if necessary for at most the given time for this promise
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
    public T get(long timeout) throws CancellationException, ExecutionException, TimeoutException, InterruptedException {
        long duration = timeout;
        long start = SystemClock.uptimeMillis();
        while (!isDone() && (duration > 0)) {
            mLock.lock();
            try {
                mCondition.await(duration, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw e;
            } finally {
                mLock.unlock();
            }
            duration = start + timeout - SystemClock.uptimeMillis();
        }
        if (!isDone()) {
            throw new TimeoutException("Promise timed out");
        }
        if (mResult == NULL) {
            return null;
        }
        if (mResult instanceof Promise.Error) {
            Throwable throwable = ((Promise.Error) mResult).mThrowable;
            if (throwable instanceof CancellationException) {
                throw (CancellationException) throwable;
            }
            Throwable cause;
            if ((throwable instanceof CompletionException) &&
                    (cause = throwable.getCause()) != null) {
                throwable = cause;
            }
            throw new ExecutionException(throwable);
        }
        @SuppressWarnings("unchecked") T result = (T) mResult;
        return result;
    }

    /**
     * If not already completed, sets the value returned by {@link
     * #get()} and related methods to the given value.
     *
     * @param value the result value
     * @return {@code true} if this invocation caused this Promise
     * to transition to a completed state, else {@code false}
     */
    public boolean complete(T value) {
        boolean completed = setResult((value != null) ? value : NULL);
        if (completed) {
            onComplete();
        }
        return completed;
    }

    /**
     * If not already completed, causes invocations of {@link #get()}
     * and related methods to throw the given exception.
     *
     * @param throwable the exception
     * @return {@code true} if this invocation caused this Promise
     * to transition to a completed state, else {@code false}
     */
    public boolean completeWith(Throwable throwable) {
        if (throwable == null) {
            throw new NullPointerException();
        }
        boolean completed = setResult(new Error(throwable));
        if (completed) {
            onComplete();
        }
        return completed;
    }

    private boolean setResult(Object result) {
        return UNSAFE.compareAndSwapObject(this, RESULT, null, result);
    }

    private void onComplete() {
        mLock.lock();
        try {
            mCondition.signalAll();
        } finally {
            mLock.unlock();
        }

        runActions();
    }

    public boolean completeWith(Promise<T> supplier) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        Action<?, ?> a = new RelayAction<>(supplier, this);
        if (supplier.mResult != null) {
            a.tryRun();
        } else {
            supplier.addAction(a);
        }
        return true;
    }

    /**
     * If not already completed, completes this Promise with
     * a {@link CancellationException}. Dependent Promises
     * that have not already completed will also complete
     * exceptionally, with a {@link CompletionException} caused by
     * this {@code CancellationException}.
     *
     * @return {@code true} if this task is now cancelled
     */
    public boolean cancel() {
        boolean cancelled = completeWith(new CancellationException());
        return cancelled || isCancelled();
    }

    /**
     * Returns {@code true} if this Promise was cancelled
     * before it completed normally.
     *
     * @return {@code true} if this Promise was cancelled
     * before it completed normally
     */
    public boolean isCancelled() {
        if (mResult instanceof Promise.Error) {
            Throwable throwable = ((Promise.Error) mResult).mThrowable;
            if (throwable instanceof CancellationException) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this Promise completed
     * exceptionally, in any way. Possible causes include
     * cancellation, explicit invocation of {@code
     * completeWith(Throwable)}, and abrupt termination of a
     * Promise action.
     *
     * @return {@code true} if this Promise completed
     * exceptionally
     */
    public boolean isCompletedExceptionally() {
        return (mResult instanceof Promise.Error);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * normally, is executed using this Promise's default asynchronous
     * execution facility, with this Promise's result as the argument to
     * the supplied function.
     *
     * See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param function the Function to use to compute the value of the
     * returned Promise
     * @param <U> the function's return type
     * @return the new Promise
     */
    public <U> Promise<U> thenApply(Function<? super T, ? extends U> function) {
        return thenApply(mExecutor, function);
    }

    /**
     * @see #thenApply(Function)
     */
    public <U> Promise<U> then(Function<? super T, ? extends U> function) {
        return thenApply(mExecutor, function);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * normally, is executed using the supplied Handler, with this
     * Promise's result as the argument to the supplied function.
     *
     * See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Promise
     * @param <U> the function's return type
     * @return the new Promise
     */
    public <U> Promise<U> thenApply(Handler handler, Function<? super T, ? extends U> function) {
        return thenApply(handler.asExecutor(), function);
    }

    /**
     * @see #thenApply(Handler, Function)
     */
    public <U> Promise<U> then(Handler handler, Function<? super T, ? extends U> function) {
        return thenApply(handler.asExecutor(), function);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * normally, is executed using the supplied Executor, with this
     * Promise's result as the argument to the supplied function.
     *
     * See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Promise
     * @param <U> the function's return type
     * @return the new Promise
     */
    public <U> Promise<U> thenApply(Executor executor, Function<? super T, ? extends U> function) {
        if (executor == null) {
            throw new NullPointerException();
        }
        if (function == null) {
            throw new NullPointerException();
        }
        Promise<U> p = new Promise<>(mExecutor);
        Action<?, ?> a = new FunctionAction<>(executor, this, p, function);
        if (mResult != null) {
            a.tryRun();
        } else {
            addAction(a);
        }
        return p;
    }

    /**
     * @see #thenApply(Executor, Function)
     */
    public <U> Promise<U> then(Executor executor, Function<? super T, ? extends U> function) {
        return thenApply(executor, function);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * either normally or exceptionally, is executed using this Promise's
     * default asynchronous execution facility, with this Promise's
     * result and exception as arguments to the supplied function.
     *
     * <p>When this Promise is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this Promise as arguments, and the
     * function's result is used to complete the returned Promise.
     *
     * @param function the Function to use to compute the value of the
     * returned Promise
     * @param <U> the function's return type
     * @return the new Promise
     */
    public <U> Promise<U> thenApply(BiFunction<? super T, Throwable, ? extends U> function) {
        return thenApply(mExecutor, function);
    }

    /**
     * @see #thenApply(BiFunction)
     */
    public <U> Promise<U> then(BiFunction<? super T, Throwable, ? extends U> function) {
        return thenApply(mExecutor, function);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * either normally or exceptionally, is executed using the
     * supplied Handler, with this Promise's result and exception as
     * arguments to the supplied function.
     *
     * <p>When this Promise is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this Promise as arguments, and the
     * function's result is used to complete the returned Promise.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Promise
     * @param <U> the function's return type
     * @return the new Promise
     */
    public <U> Promise<U> thenApply(Handler handler, BiFunction<? super T, Throwable, ? extends U> function) {
        return thenApply(handler.asExecutor(), function);
    }

    /**
     * @see #thenApply(Handler, BiFunction)
     */
    public <U> Promise<U> then(Handler handler, BiFunction<? super T, Throwable, ? extends U> function) {
        return thenApply(handler.asExecutor(), function);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * either normally or exceptionally, is executed using the
     * supplied Executor, with this Promise's result and exception as
     * arguments to the supplied function.
     *
     * <p>When this Promise is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this Promise as arguments, and the
     * function's result is used to complete the returned Promise.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Promise
     * @param <U> the function's return type
     * @return the new Promise
     */
    public <U> Promise<U> thenApply(Executor executor, BiFunction<? super T, Throwable, ? extends U> function) {
        if (executor == null) {
            throw new NullPointerException();
        }
        if (function == null) {
            throw new NullPointerException();
        }
        Promise<U> p = new Promise<>(mExecutor);
        Action<?, ?> a = new BiFunctionAction<>(executor, this, p, function);
        if (mResult != null) {
            a.tryRun();
        } else {
            addAction(a);
        }
        return p;
    }

    /**
     * @see #thenApply(Executor, BiFunction)
     */
    public <U> Promise<U> then(Executor executor, BiFunction<? super T, Throwable, ? extends U> function) {
        return thenApply(executor, function);
    }

    /**
     * Returns a new Promise that is completed with the same
     * value as the Promise returned by the given function,
     * executed using this Promise's default asynchronous execution
     * facility.
     *
     * <p>When this Promise completes normally, the given function is
     * invoked with this Promise's result as the argument, returning
     * another Promise.  When that Promise completes normally,
     * the Promise returned by this method is completed with
     * the same value.
     *
     * <p>To ensure progress, the supplied function must arrange
     * eventual completion of its result.
     *
     * <p>See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param function the Function to use to compute another Promise
     * @param <U> the type of the returned Promise's result
     * @return the new Promise
     */
    public <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> function) {
        return thenCompose(mExecutor, function);
    }

    /**
     * Returns a new Promise that is completed with the same
     * value as the Promise returned by the given function,
     * executed using the supplied Handler.
     *
     * <p>When this Promise completes normally, the given function is
     * invoked with this Promise's result as the argument, returning
     * another Promise.  When that Promise completes normally,
     * the Promise returned by this method is completed with
     * the same value.
     *
     * <p>To ensure progress, the supplied function must arrange
     * eventual completion of its result.
     *
     * <p>See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param function the Function to use to compute another Promise
     * @param <U> the type of the returned Promise's result
     * @return the new Promise
     */
    public <U> Promise<U> thenCompose(Handler handler, Function<? super T, ? extends Promise<U>> function) {
        return thenCompose(handler.asExecutor(), function);
    }

    /**
     * Returns a new Promise that is completed with the same
     * value as the Promise returned by the given function,
     * executed using the supplied Executor.
     *
     * <p>When this Promise completes normally, the given function is
     * invoked with this Promise's result as the argument, returning
     * another Promise.  When that Promise completes normally,
     * the Promise returned by this method is completed with
     * the same value.
     *
     * <p>To ensure progress, the supplied function must arrange
     * eventual completion of its result.
     *
     * <p>See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param function the Function to use to compute another Promise
     * @param <U> the type of the returned Promise's result
     * @return the new Promise
     */
    public <U> Promise<U> thenCompose(Executor executor, Function<? super T, ? extends Promise<U>> function) {
        if (executor == null) {
            throw new NullPointerException();
        }
        if (function == null) {
            throw new NullPointerException();
        }
        Promise<U> p = new Promise<>(mExecutor);
        Action<?, ?> a = new CompositionFunctionAction<>(executor, this, p, function);
        if (mResult != null) {
            a.tryRun();
        } else {
            addAction(a);
        }
        return p;
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * normally, is executed using this Promise's default asynchronous
     * execution facility, with this Promise's result as the argument to
     * the supplied action.
     *
     * See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param action the action to perform before completing the
     * returned Promise
     * @return the new Promise
     */
    public Promise<T> thenAccept(Consumer<? super T> action) {
        return thenAccept(mExecutor, action);
    }

    /**
     * @see #thenAccept(Consumer)
     */
    public Promise<T> then(Consumer<? super T> action) {
        return thenAccept(mExecutor, action);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * normally, is executed using the supplied Handler, with this
     * Promise's result as the argument to the supplied action.
     *
     * See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param action the action to perform before completing the
     * returned Promise
     * @return the new Promise
     */
    public Promise<T> thenAccept(Handler handler, Consumer<? super T> action) {
        return thenAccept(handler.asExecutor(), action);
    }

    /**
     * @see #thenAccept(Handler, Consumer)
     */
    public Promise<T> then(Handler handler, Consumer<? super T> action) {
        return thenAccept(handler.asExecutor(), action);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * normally, is executed using the supplied Executor, with this
     * Promise's result as the argument to the supplied action.
     *
     * See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param action the action to perform before completing the
     * returned Promise
     * @return the new Promise
     */
    public Promise<T> thenAccept(Executor executor, Consumer<? super T> action) {
        if (executor == null) {
            throw new NullPointerException();
        }
        if (action == null) {
            throw new NullPointerException();
        }
        Promise<T> p = new Promise<>(mExecutor);
        Action<?, ?> a = new ConsumerAction<>(executor, this, p, action);
        if (mResult != null) {
            a.tryRun();
        } else {
            addAction(a);
        }
        return p;
    }

    /**
     * @see #thenAccept(Executor, Consumer)
     */
    public Promise<T> then(Executor executor, Consumer<? super T> action) {
        return thenAccept(executor, action);
    }

    /**
     * Returns a new Promise with the same result or exception as
     * this Promise, that executes the given action using this Promise's
     * default asynchronous execution facility when this Promise completes.
     *
     * <p>When this Promise is complete, the given action is invoked with the
     * result (or {@code null} if none) and the exception (or {@code null}
     * if none) of this Promise as arguments.  The returned Promise is completed
     * when the action returns.
     *
     * @param action the action to perform
     * @return the new Promise
     */
    public Promise<T> thenAccept(BiConsumer<? super T, ? super Throwable> action) {
        return thenAccept(mExecutor, action);
    }

    /**
     * @see #thenAccept(BiConsumer)
     */
    public Promise<T> then(BiConsumer<? super T, ? super Throwable> action) {
        return thenAccept(mExecutor, action);
    }

    /**
     * Returns a new Promise with the same result or exception as
     * this Promise, that executes the given action using the supplied
     * Handler when this Promise completes.
     *
     * <p>When this Promise is complete, the given action is invoked with the
     * result (or {@code null} if none) and the exception (or {@code null}
     * if none) of this Promise as arguments.  The returned Promise is completed
     * when the action returns.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param action the action to perform
     * @return the new Promise
     */
    public Promise<T> thenAccept(Handler handler, BiConsumer<? super T, ? super Throwable> action) {
        return thenAccept(handler.asExecutor(), action);
    }

    /**
     * @see #thenAccept(Handler, BiConsumer)
     */
    public Promise<T> then(Handler handler, BiConsumer<? super T, ? super Throwable> action) {
        return thenAccept(handler.asExecutor(), action);
    }

    /**
     * Returns a new Promise with the same result or exception as
     * this Promise, that executes the given action using the supplied
     * Executor when this Promise completes.
     *
     * <p>When this Promise is complete, the given action is invoked with the
     * result (or {@code null} if none) and the exception (or {@code null}
     * if none) of this Promise as arguments.  The returned Promise is completed
     * when the action returns.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param action the action to perform
     * @return the new Promise
     */
    public Promise<T> thenAccept(Executor executor, BiConsumer<? super T, ? super Throwable> action) {
        if (executor == null) {
            throw new NullPointerException();
        }
        if (action == null) {
            throw new NullPointerException();
        }
        Promise<T> p = new Promise<>(mExecutor);
        Action<?, ?> a = new BiConsumerAction<>(executor, this, p, action);
        if (mResult != null) {
            a.tryRun();
        } else {
            addAction(a);
        }
        return p;
    }

    /**
     * @see #thenAccept(Executor, BiConsumer)
     */
    public Promise<T> then(Executor executor, BiConsumer<? super T, ? super Throwable> action) {
        return thenAccept(executor, action);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * normally, executes the given action using this Promise's default
     * asynchronous execution facility.
     *
     * See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param action the action to perform before completing the
     * returned Promise
     * @return the new Promise
     */
    public Promise<T> thenRun(Runnable action) {
        return thenRun(mExecutor, action);
    }

    /**
     * @see #thenRun(Runnable)
     */
    public Promise<T> then(Runnable action) {
        return thenRun(mExecutor, action);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * normally, executes the given action using the supplied Handler.
     *
     * See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param action the action to perform before completing the
     * returned Promise
     * @return the new Promise
     */
    public Promise<T> thenRun(Handler handler, Runnable action) {
        return thenRun(handler.asExecutor(), action);
    }

    /**
     * @see #thenRun(Handler, Runnable)
     */
    public Promise<T> then(Handler handler, Runnable action) {
        return thenRun(handler.asExecutor(), action);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * normally, executes the given action using the supplied Executor.
     *
     * See the {@link Promise} documentation for rules
     * covering exceptional completion.
     *
     * @param executor the executor to use for asynchronous execution
     * @param action the action to perform before completing the
     * returned Promise
     * @return the new Promise
     */
    public Promise<T> thenRun(Executor executor, Runnable action) {
        if (executor == null) {
            throw new NullPointerException();
        }
        if (action == null) {
            throw new NullPointerException();
        }
        Promise<T> p = new Promise<>(mExecutor);
        Action<?, ?> a = new RunAction<>(executor, this, p, action);
        if (mResult != null) {
            a.tryRun();
        } else {
            addAction(a);
        }
        return p;
    }

    /**
     * @see #thenRun(Executor, Runnable)
     */
    public Promise<T> then(Executor executor, Runnable action) {
        return thenRun(executor, action);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * exceptionally, is executed with this Promise's exception as the
     * argument to the supplied function using this Promise's
     * default asynchronous execution.  Otherwise, if this Promise
     * completes normally, then the returned Promise also completes
     * normally with the same value.
     *
     * @param function the Function to use to compute the value of the
     * returned Promise if this Promise completed
     * exceptionally
     * @return the new Promise
     */
    public Promise<T> catchException(Function<Throwable, ? extends T> function) {
        return catchException(mExecutor, function);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * exceptionally, is executed with this Promise's exception as the
     * argument to the supplied function using the supplied
     * Handler.  Otherwise, if this Promise completes normally,
     * then the returned Promise also completes normally with the same value.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Promise if this Promise completed
     * exceptionally
     * @return the new Promise
     */
    public Promise<T> catchException(Handler handler, Function<Throwable, ? extends T> function) {
        return catchException(handler.asExecutor(), function);
    }

    /**
     * Returns a new Promise that, when this Promise completes
     * exceptionally, is executed with this Promise's exception as the
     * argument to the supplied function using the supplied
     * Executor.  Otherwise, if this Promise completes normally,
     * then the returned Promise also completes normally with the same value.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param function the Function to use to compute the value of the
     * returned Promise if this Promise completed
     * exceptionally
     * @return the new Promise
     */
    public Promise<T> catchException(Executor executor, Function<Throwable, ? extends T> function) {
        if (executor == null) {
            throw new NullPointerException();
        }
        if (function == null) {
            throw new NullPointerException();
        }
        Promise<T> p = new Promise<>(mExecutor);
        Action<?, ?> a = new ErrorFunctionAction<T>(executor, this, p, function);
        if (mResult != null) {
            a.tryRun();
        } else {
            addAction(a);
        }
        return p;
    }

    public Promise<T> catchException(Consumer<Throwable> action) {
        return catchException(mExecutor, action);
    }

    public Promise<T> catchException(Handler handler, Consumer<Throwable> action) {
        return catchException(handler.asExecutor(), action);
    }

    public Promise<T> catchException(Executor executor, Consumer<Throwable> action) {
        if (executor == null) {
            throw new NullPointerException();
        }
        if (action == null) {
            throw new NullPointerException();
        }
        Promise<T> p = new Promise<>(mExecutor);
        Action<?, ?> a = new ErrorConsumerAction<T>(executor, this, p, action);
        if (mResult != null) {
            a.tryRun();
        } else {
            addAction(a);
        }
        return p;
    }

    /**
     * Exceptionally completes this Promise with a {@link TimeoutException}
     * if not otherwise completed before the given timeout.
     *
     * @param timeout how long to wait before completing exceptionally
     *        with a TimeoutException in milliseconds
     * @return this Promise
     */
    public Promise<T> orTimeout(long timeout) {
        if (mResult == null) {
            then(Timeout.add(new Timeout.Exception(this), timeout));
        }
        return this;
    }

    /**
     * Completes this Promise with the given value if not otherwise
     * completed before the given timeout.
     *
     * @param value the value to use upon timeout
     * @param timeout how long to wait before completing normally
     *        with the given value in milliseconds
     * @return this Promise
     */
    public Promise<T> completeOnTimeout(T value, long timeout) {
        if (mResult == null) {
            then(Timeout.add(new Timeout.Completion<T>(this, value), timeout));
        }
        return this;
    }

    private static abstract class Action<T, U> implements Runnable {
        protected Executor mExecutor;
        protected Promise<T> mSupplier;
        protected Promise<U> mConsumer;
        private final AtomicBoolean mClaim = new AtomicBoolean(false);

        Action(Executor executor, Promise<T> supplier, Promise<U> consumer) {
            mExecutor = executor;
            mSupplier = supplier;
            mConsumer = consumer;
        }

        void tryRun() {
            if (claim()) {
                mExecutor.execute(this);
            }
        }

        @Override
        public abstract void run();

        protected final boolean claim() {
            return mClaim.compareAndSet(false, true);
        }
    }

    private static final class RelayAction<T, U extends T> extends Action<T, U> {
        RelayAction(Promise<T> supplier, Promise<U> consumer) {
            super(null, supplier, consumer);
        }

        @Override
        final void tryRun() {
            if (claim()) {
                run();
            }
        }

        @Override
        public final void run() {
            if (!(mSupplier.mResult instanceof Promise.Error)) {
                mConsumer.setResult(mSupplier.mResult);
            } else {
                mConsumer.setResult(toCompletionException((Promise.Error) mSupplier.mResult));
            }

            mConsumer.onComplete();
        }
    }

    private static final class FunctionAction<T, U> extends Action<T, U> {
        private Function<? super T, ? extends U> mFunction;

        FunctionAction(Executor executor, Promise<T> supplier, Promise<U> consumer,
                 Function<? super T, ? extends U> function) {
            super(executor, supplier, consumer);
            mFunction = function;
        }

        @Override
        public final void run() {
            if (!(mSupplier.mResult instanceof Promise.Error)) {
                try {
                    @SuppressWarnings("unchecked") T result = (T) mSupplier.mResult;
                    U u = mFunction.apply(result != NULL ? result : null);
                    mConsumer.setResult((u != null) ? u : NULL);
                } catch (Throwable e) {
                    mConsumer.setResult(toCompletionException(e));
                }
            } else {
                mConsumer.setResult(toCompletionException((Promise.Error) mSupplier.mResult));
            }

            mConsumer.onComplete();
        }
    }

    private static final class BiFunctionAction<T, U> extends Action<T, U> {
        private BiFunction<? super T,Throwable, ? extends U> mFunction;

        BiFunctionAction(Executor executor, Promise<T> supplier, Promise<U> consumer,
                BiFunction<? super T,Throwable, ? extends U> function) {
            super(executor, supplier, consumer);
            mFunction = function;
        }

        @Override
        public final void run() {
            try {
                U u;
                if (!(mSupplier.mResult instanceof Promise.Error)) {
                    @SuppressWarnings("unchecked") T result = (T) mSupplier.mResult;
                    u = mFunction.apply(result != NULL ? result : null, null);
                } else {
                    Throwable throwable = ((Promise.Error) mSupplier.mResult).mThrowable;
                    u = mFunction.apply(null, throwable);
                }
                mConsumer.setResult((u != null) ? u : NULL);
            } catch (Throwable e) {
                mConsumer.setResult(toCompletionException(e));
            }

            mConsumer.onComplete();
        }
    }

    private static final class CompositionFunctionAction<T, U> extends Action<T, U> {
        private Function<? super T, ? extends Promise<U>> mFunction;

        CompositionFunctionAction(Executor executor, Promise<T> supplier, Promise<U> consumer,
                 Function<? super T, ? extends Promise<U>> function) {
            super(executor, supplier, consumer);
            mFunction = function;
        }

        @Override
        public final void run() {
            if (!(mSupplier.mResult instanceof Promise.Error)) {
                try {
                    @SuppressWarnings("unchecked") T result = (T) mSupplier.mResult;
                    Promise<U> u = mFunction.apply(result != NULL ? result : null);
                    mConsumer.completeWith(u);
                    return;
                } catch (Throwable e) {
                    mConsumer.setResult(toCompletionException(e));
                }
            } else {
                mConsumer.setResult(toCompletionException((Promise.Error) mSupplier.mResult));
            }

            mConsumer.onComplete();
        }
    }

    private static final class ConsumerAction<T> extends Action<T, T> {
        private Consumer<? super T> mAction;

        ConsumerAction(Executor executor, Promise<T> supplier, Promise<T> consumer,
                Consumer<? super T> action) {
            super(executor, supplier, consumer);
            mAction = action;
        }

        @Override
        public final void run() {
            if (!(mSupplier.mResult instanceof Promise.Error)) {
                try {
                    @SuppressWarnings("unchecked") T result = (T) mSupplier.mResult;
                    mAction.accept(result);
                    mConsumer.setResult(mSupplier.mResult);
                } catch (Throwable e) {
                    mConsumer.setResult(toCompletionException(e));
                }
            } else {
                mConsumer.setResult(toCompletionException((Promise.Error) mSupplier.mResult));
            }

            mConsumer.onComplete();
        }
    }

    private static final class BiConsumerAction<T> extends Action<T, T> {
        private BiConsumer<? super T, ? super Throwable> mAction;

        BiConsumerAction(Executor executor, Promise<T> supplier, Promise<T> consumer,
                BiConsumer<? super T, ? super Throwable> action) {
            super(executor, supplier, consumer);
            mAction = action;
        }

        @Override
        public final void run() {
            try {
                if (!(mSupplier.mResult instanceof Promise.Error)) {
                    @SuppressWarnings("unchecked") T result = (T) mSupplier.mResult;
                    mAction.accept(result != NULL ? result : null, null);
                } else {
                    Throwable throwable = ((Promise.Error) mSupplier.mResult).mThrowable;
                    mAction.accept(null, throwable);
                }
                mConsumer.setResult(mSupplier.mResult);
            } catch (Throwable e) {
                mConsumer.setResult(toCompletionException(e));
            }

            mConsumer.onComplete();
        }
    }

    private static final class RunAction<T> extends Action<T, T> {
        private Runnable mAction;

        RunAction(Executor executor, Promise<T> supplier, Promise<T> consumer, Runnable action) {
            super(executor, supplier, consumer);
            mAction = action;
        }

        @Override
        public final void run() {
            if (!(mSupplier.mResult instanceof Promise.Error)) {
                try {
                    mAction.run();
                    mConsumer.setResult(mSupplier.mResult);
                } catch (Throwable e) {
                    mConsumer.setResult(toCompletionException(e));
                }
            } else {
                mConsumer.setResult(toCompletionException((Promise.Error) mSupplier.mResult));
            }

            mConsumer.onComplete();
        }
    }

    private static final class ErrorFunctionAction<T> extends Action<T, T> {
        private Function<Throwable, ? extends T> mFunction;

        ErrorFunctionAction(Executor executor, Promise<T> supplier, Promise<T> consumer,
                Function<Throwable, ? extends T> function) {
            super(executor, supplier, consumer);
            mFunction = function;
        }

        @Override
        public final void run() {
            if (mSupplier.mResult instanceof Promise.Error) {
                try {
                    Throwable throwable = ((Promise.Error) mSupplier.mResult).mThrowable;
                    T t = mFunction.apply(throwable);
                    mConsumer.setResult((t != null) ? t : NULL);
                } catch (Throwable e) {
                    mConsumer.setResult(toCompletionException(e));
                }
            } else {
                mConsumer.setResult(mSupplier.mResult);
            }

            mConsumer.onComplete();
        }
    }

    private static final class ErrorConsumerAction<T> extends Action<T, T> {
        private Consumer<Throwable> mAction;

        ErrorConsumerAction(Executor executor, Promise<T> supplier, Promise<T> consumer,
                Consumer<Throwable> action) {
            super(executor, supplier, consumer);
            mAction = action;
        }

        @Override
        public final void run() {
            if (mSupplier.mResult instanceof Promise.Error) {
                try {
                    Throwable throwable = ((Promise.Error) mSupplier.mResult).mThrowable;
                    mAction.accept(throwable);
                    mConsumer.setResult(mSupplier.mResult);
                } catch (Throwable e) {
                    mConsumer.setResult(toCompletionException(e));
                }
            } else {
                mConsumer.setResult(mSupplier.mResult);
            }

            mConsumer.onComplete();
        }
    }

    private void addAction(Action<?, ?> action) {
        if (mActions.get() == null) {
            mActions.compareAndSet(null, new ConcurrentLinkedQueue<>());
        }
        mActions.get().add(action);
        if (mResult != null) {
            mActions.get().remove(action);
            action.tryRun();
        }
    }

    private void runActions() {
        if (mActions.get() != null) {
            Action<?, ?> action;
            while ((action = mActions.get().poll()) != null) {
                action.tryRun();
            }
        }
    }

    private static Error toCompletionException(Throwable throwable) {
        return new Error((throwable instanceof CompletionException) ? (CompletionException) throwable : new CompletionException(throwable));
    }

    private static Error toCompletionException(Error error) {
        return toCompletionException(error.mThrowable);
    }

    private static final class Timeout {
        static final ScheduledThreadPoolExecutor sExecutor;

        static {
            sExecutor = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory());
            sExecutor.setKeepAliveTime(10, TimeUnit.SECONDS);
            sExecutor.allowCoreThreadTimeOut(true);
            sExecutor.setRemoveOnCancelPolicy(true);
        }

        static BiConsumer<Object, Throwable> add(Runnable command, long delay) {
            final ScheduledFuture<?> future = sExecutor.schedule(command, delay, TimeUnit.MILLISECONDS);
            return new BiConsumer<Object, Throwable>() {
                @Override
                public void accept(Object ignore, Throwable exception) {
                    if (exception == null && future != null && !future.isDone()) {
                        future.cancel(false);
                    }
                }
            };
        }

        private static final class DaemonThreadFactory implements ThreadFactory {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("TimeoutExecutorDaemon");
                return t;
            }
        }

        static final class Completion<U> implements Runnable {
            final Promise<U> mConsumer;
            final U u;

            Completion(Promise<U> consumer, U u) {
                mConsumer = consumer; this.u = u;
            }

            public void run() {
                if (mConsumer != null && !mConsumer.isDone()) {
                    mConsumer.complete(u);
                }
            }
        }

        static final class Exception implements Runnable {
            final Promise<?> mConsumer;

            Exception(Promise<?> consumer) {
                mConsumer = consumer;
            }

            public void run() {
                if (mConsumer != null && !mConsumer.isDone()) {
                    mConsumer.completeWith(new TimeoutException());
                }
            }
        }
    }
}
