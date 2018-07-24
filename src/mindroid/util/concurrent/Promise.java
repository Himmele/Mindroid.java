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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
        final Throwable mThrowable;

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
    public Promise(final Throwable throwable) {
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
        mExecutor = executor;
    }

    public Promise(final Promise<T> supplier) {
        this();
        completeWith(supplier);
    }

    public Promise(final CompletionStage<T> supplier) {
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

    @Override
    public boolean isDone() {
        return mResult != null;
    }

    @Override
    public T get() throws CancellationException, ExecutionException, InterruptedException {
        synchronized (this) {
            while (!isDone()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw e;
                }
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

    @Override
    public T get(long timeout) throws CancellationException, ExecutionException, TimeoutException, InterruptedException {
        long duration = timeout;
        long start = SystemClock.uptimeMillis();
        synchronized (this) {
            while (!isDone() && (duration > 0)) {
                try {
                    wait(duration);
                } catch (InterruptedException e) {
                    throw e;
                }
                duration = start + timeout - SystemClock.uptimeMillis();
            }
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
        synchronized (this) {
            notifyAll();
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

    public boolean completeWith(CompletionStage<T> supplier) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        supplier.whenComplete((value, exception) -> {
            if (exception != null) {
                Throwable throwable = exception;
                Throwable cause;
                if ((throwable instanceof java.util.concurrent.CompletionException) &&
                        (cause = throwable.getCause()) != null) {
                    throwable = cause;
                }
                completeWith(new CompletionException(throwable));
            } else {
                complete(value);
            }
        });
        return true;
    }

    @Override
    public boolean cancel() {
        boolean cancelled = completeWith(new CancellationException());
        return cancelled || isCancelled();
    }

    @Override
    public boolean isCancelled() {
        if (mResult instanceof Promise.Error) {
            Throwable throwable = ((Promise.Error) mResult).mThrowable;
            if (throwable instanceof CancellationException) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCompletedExceptionally() {
        return (mResult instanceof Promise.Error);
    }

    /**
     * Returns a new Promise that is completed when all of
     * the given Promises complete and is executed using
     * this Promise's default asynchronous execution facility.
     * If any of the given Promises complete exceptionally,
     * then the returned Promise also does so, with a
     * CompletionException holding this exception as its cause.
     * Otherwise, the results, if any, of the given Promises
     * are not reflected in the returned Promise, but may be
     * obtained by inspecting them individually. If no
     * Promises are provided, returns a Promise completed
     * with the value {@code null}.
     *
     * <p>Among the applications of this method is to await completion
     * of a set of independent Promise before continuing a
     * program, as in: {@code Promise.allOf(p1, p2, p3).get();}.
     *
     * @param promises the Promises
     * @return a new Promise that is completed when all of the
     * given Promise complete
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static Promise<Void> allOf(Promise<?>... promises) {
        return allOf((Looper.myLooper() != null) ? new Handler().asExecutor() : null, promises);
    }

    /**
     * Returns a new Promise that is completed when all of
     * the given Promises complete and is executed using
     * the supplied Handler.
     * If any of the given Promises complete exceptionally,
     * then the returned Promise also does so, with a
     * CompletionException holding this exception as its cause.
     * Otherwise, the results, if any, of the given Promises
     * are not reflected in the returned Promise, but may be
     * obtained by inspecting them individually. If no
     * Promises are provided, returns a Promise completed
     * with the value {@code null}.
     *
     * <p>Among the applications of this method is to await completion
     * of a set of independent Promise before continuing a
     * program, as in: {@code Promise.allOf(p1, p2, p3).get();}.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param promises the Promises
     * @return a new Promise that is completed when all of the
     * given Promise complete
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static Promise<Void> allOf(Handler handler, Promise<?>... promises) {
        return allOf(handler.asExecutor(), promises);
    }

    /**
     * Returns a new Promise that is completed when all of
     * the given Promises complete and is executed using
     * the supplied Executor.
     * If any of the given Promises complete exceptionally,
     * then the returned Promise also does so, with a
     * CompletionException holding this exception as its cause.
     * Otherwise, the results, if any, of the given Promises
     * are not reflected in the returned Promise, but may be
     * obtained by inspecting them individually. If no
     * Promises are provided, returns a Promise completed
     * with the value {@code null}.
     *
     * <p>Among the applications of this method is to await completion
     * of a set of independent Promise before continuing a
     * program, as in: {@code Promise.allOf(p1, p2, p3).get();}.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param promises the Promises
     * @return a new Promise that is completed when all of the
     * given Promise complete
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static Promise<Void> allOf(Executor executor, Promise<?>... promises) {
        if (promises.length == 0) {
            Promise<Void> p = new Promise<>(executor);
            p.complete(null);
            return p;
        }
        return andTree(executor, promises, 0, promises.length - 1);
    }

    private static Promise<Void> andTree(Executor executor, Promise<?>[] promises, int start, int end) {
        Promise<?> supplier1;
        Promise<?> supplier2;
        Promise<Void> consumer = new Promise<>(executor);
        Object error;
        int mid = (start + end) / 2;
        if ((supplier1 = (start == mid ? promises[start] :
                andTree(executor, promises, start, mid))) == null ||
            (supplier2 = (start == end ? supplier1 : (end == mid + 1) ? promises[end] :
                andTree(executor, promises, mid + 1, end))) == null) {
            throw new NullPointerException();
        }
        if (supplier1.mResult == null || supplier2.mResult == null) {
            Action<?, ?> action = new BiRelayAction<>(supplier1, supplier2, consumer);
            if (supplier1.mResult != null) {
                action.tryRun();
            } else {
                supplier1.addAction(action);
            }
            if (supplier2.mResult != null) {
                action.tryRun();
            } else {
                supplier2.addAction(action);
            }
        } else if (((error = supplier1.mResult) instanceof Error) || ((error = supplier2.mResult) instanceof Error)) {
            consumer.setResult(toCompletionException((Promise.Error) error));
        } else {
            consumer.setResult(NULL);
        }
        return consumer;
    }

    /**
     * Returns a new Promise that is completed when any of
     * the given Promises complete, with the same result
     * and is executed using this Promise's default asynchronous
     * execution facility.
     * Otherwise, if it completed exceptionally, the returned
     * Promise also does so, with a CompletionException
     * holding this exception as its cause.  If no Promises
     * are provided, returns an incomplete Promise.
     *
     * @param promises the Promises
     * @return a new Promise that is completed with the
     * result or exception of any of the given Promises when
     * one completes
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static Promise<Object> anyOf(Promise<?>... promises) {
        return anyOf((Looper.myLooper() != null) ? new Handler().asExecutor() : null, promises);
    }

    /**
     * Returns a new Promise that is completed when any of
     * the given Promises complete, with the same result
     * and is executed using the supplied Handler.
     * Otherwise, if it completed exceptionally, the returned
     * Promise also does so, with a CompletionException
     * holding this exception as its cause.  If no Promises
     * are provided, returns an incomplete Promise.
     *
     * @param handler the Handler to use for asynchronous execution
     * @param promises the Promises
     * @return a new Promise that is completed with the
     * result or exception of any of the given Promises when
     * one completes
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static Promise<Object> anyOf(Handler handler, Promise<?>... promises) {
        return anyOf(handler.asExecutor(), promises);
    }

    /**
     * Returns a new Promise that is completed when any of
     * the given Promises complete, with the same result
     * and is executed using the supplied Executor.
     * Otherwise, if it completed exceptionally, the returned
     * Promise also does so, with a CompletionException
     * holding this exception as its cause.  If no Promises
     * are provided, returns an incomplete Promise.
     *
     * @param executor the Executor to use for asynchronous execution
     * @param promises the Promises
     * @return a new Promise that is completed with the
     * result or exception of any of the given Promises when
     * one completes
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static Promise<Object> anyOf(Executor executor, Promise<?>... promises) {
        if (promises.length == 0) {
            return new Promise<>(executor);
        } else {
            Promise<Object> consumer = new Promise<>(executor);
            for (Promise<?> p : promises) {
                if (p.mResult != null) {
                    if (!(p.mResult instanceof Error)) {
                        consumer.setResult(p.mResult);
                    } else {
                        consumer.setResult(toCompletionException((Promise.Error) p.mResult));
                    }
                    return consumer;
                }
            }

            final Action<?, ?>[] actions = new Action<?, ?>[promises.length];
            for (int i = 0; i < promises.length; i++) {
                actions[i] =  new AnyOfAction<>(promises[i], consumer, actions);
            }
            for (int i = 0; i < promises.length; i++) {
                promises[i].addAction(actions[i]);
            }

            // Clean up all promises if the consumer completed while adding the AnyOfAction actions.
            if (consumer.mResult != null) {
                for (int i = 0; i < promises.length; i++) {
                    promises[i].removeAction(actions[i]);
                }
            }

            return consumer;
        }
    }

    /**
     * Returns this Promise.
     *
     * @return this Promise
     */
    @Override
    public Promise<T> toPromise() {
        return this;
    }

    @Override
    public <U> Promise<U> thenApply(Function<? super T, ? extends U> function) {
        return thenApply(mExecutor, function);
    }

    @Override
    public <U> Promise<U> then(Function<? super T, ? extends U> function) {
        return thenApply(mExecutor, function);
    }

    @Override
    public <U> Promise<U> thenApply(Handler handler, Function<? super T, ? extends U> function) {
        return thenApply(handler.asExecutor(), function);
    }

    @Override
    public <U> Promise<U> then(Handler handler, Function<? super T, ? extends U> function) {
        return thenApply(handler.asExecutor(), function);
    }

    @Override
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

    @Override
    public <U> Promise<U> then(Executor executor, Function<? super T, ? extends U> function) {
        return thenApply(executor, function);
    }

    @Override
    public <U> Promise<U> thenApply(BiFunction<? super T, Throwable, ? extends U> function) {
        return thenApply(mExecutor, function);
    }

    @Override
    public <U> Promise<U> then(BiFunction<? super T, Throwable, ? extends U> function) {
        return thenApply(mExecutor, function);
    }

    @Override
    public <U> Promise<U> thenApply(Handler handler, BiFunction<? super T, Throwable, ? extends U> function) {
        return thenApply(handler.asExecutor(), function);
    }

    @Override
    public <U> Promise<U> then(Handler handler, BiFunction<? super T, Throwable, ? extends U> function) {
        return thenApply(handler.asExecutor(), function);
    }

    @Override
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

    @Override
    public <U> Promise<U> then(Executor executor, BiFunction<? super T, Throwable, ? extends U> function) {
        return thenApply(executor, function);
    }

    @Override
    public <U> Promise<U> thenCompose(Function<? super T, ? extends Future<U>> function) {
        return thenCompose(mExecutor, function);
    }

    @Override
    public <U> Promise<U> thenCompose(Handler handler, Function<? super T, ? extends Future<U>> function) {
        return thenCompose(handler.asExecutor(), function);
    }

    @Override
    public <U> Promise<U> thenCompose(Executor executor, Function<? super T, ? extends Future<U>> function) {
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

    @Override
    public <U> Promise<U> thenCompose(BiFunction<? super T, Throwable, ? extends Future<U>> function) {
        return thenCompose(mExecutor, function);
    }

    @Override
    public <U> Promise<U> thenCompose(Handler handler, BiFunction<? super T, Throwable, ? extends Future<U>> function) {
        return thenCompose(handler.asExecutor(), function);
    }

    public <U> Promise<U> thenCompose(Executor executor, BiFunction<? super T, Throwable, ? extends Future<U>> function) {
        if (executor == null) {
            throw new NullPointerException();
        }
        if (function == null) {
            throw new NullPointerException();
        }
        Promise<U> p = new Promise<>(mExecutor);
        Action<?, ?> a = new BiCompositionFunctionAction<>(executor, this, p, function);
        if (mResult != null) {
            a.tryRun();
        } else {
            addAction(a);
        }
        return p;
    }

    @Override
    public Promise<T> thenAccept(Consumer<? super T> action) {
        return thenAccept(mExecutor, action);
    }

    @Override
    public Promise<T> then(Consumer<? super T> action) {
        return thenAccept(mExecutor, action);
    }

    @Override
    public Promise<T> thenAccept(Handler handler, Consumer<? super T> action) {
        return thenAccept(handler.asExecutor(), action);
    }

    @Override
    public Promise<T> then(Handler handler, Consumer<? super T> action) {
        return thenAccept(handler.asExecutor(), action);
    }

    @Override
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

    @Override
    public Promise<T> then(Executor executor, Consumer<? super T> action) {
        return thenAccept(executor, action);
    }

    @Override
    public Promise<T> thenAccept(BiConsumer<? super T, ? super Throwable> action) {
        return thenAccept(mExecutor, action);
    }

    @Override
    public Promise<T> then(BiConsumer<? super T, ? super Throwable> action) {
        return thenAccept(mExecutor, action);
    }

    @Override
    public Promise<T> thenAccept(Handler handler, BiConsumer<? super T, ? super Throwable> action) {
        return thenAccept(handler.asExecutor(), action);
    }

    @Override
    public Promise<T> then(Handler handler, BiConsumer<? super T, ? super Throwable> action) {
        return thenAccept(handler.asExecutor(), action);
    }

    @Override
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

    @Override
    public Promise<T> then(Executor executor, BiConsumer<? super T, ? super Throwable> action) {
        return thenAccept(executor, action);
    }

    @Override
    public Promise<T> thenRun(Runnable action) {
        return thenRun(mExecutor, action);
    }

    @Override
    public Promise<T> then(Runnable action) {
        return thenRun(mExecutor, action);
    }

    @Override
    public Promise<T> thenRun(Handler handler, Runnable action) {
        return thenRun(handler.asExecutor(), action);
    }

    @Override
    public Promise<T> then(Handler handler, Runnable action) {
        return thenRun(handler.asExecutor(), action);
    }

    @Override
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

    @Override
    public Promise<T> then(Executor executor, Runnable action) {
        return thenRun(executor, action);
    }

    @Override
    public Promise<T> catchException(Function<Throwable, ? extends T> function) {
        return catchException(mExecutor, function);
    }

    @Override
    public Promise<T> catchException(Handler handler, Function<Throwable, ? extends T> function) {
        return catchException(handler.asExecutor(), function);
    }

    @Override
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

    @Override
    public Promise<T> catchException(Consumer<Throwable> action) {
        return catchException(mExecutor, action);
    }

    @Override
    public Promise<T> catchException(Handler handler, Consumer<Throwable> action) {
        return catchException(handler.asExecutor(), action);
    }

    @Override
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

    @Override
    public Promise<T> orTimeout(long timeout) {
        return orTimeout(timeout, null, null);
    }

    public Promise<T> orTimeout(long timeout, String message) {
        return orTimeout(timeout, message, null);
    }

    public Promise<T> orTimeout(long timeout, String message, Throwable cause) {
        if (mResult == null) {
            then(Timeout.add(new Timeout.Exception(this, message, cause), timeout));
        }
        return this;
    }

    @Override
    public Promise<T> completeOnTimeout(T value, long timeout) {
        if (mResult == null) {
            then(Timeout.add(new Timeout.Completion<T>(this, value), timeout));
        }
        return this;
    }

    @Override
    public Promise<T> delay(long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Delay < 0");
        }
        Promise<T> p = new Promise<>(mExecutor);
        Action<?, ?> a = new DelayAction<>(this, p, delay);
        if (mResult != null) {
            a.tryRun();
        } else {
            addAction(a);
        }
        return p;
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

    private static abstract class BiAction<T, U, V> extends Action<T, V> {
        protected Promise<U> mSupplier2;

        BiAction(Executor executor, Promise<T> supplier1, Promise<U> supplier2, Promise<V> consumer) {
            super(executor, supplier1, consumer);
            mSupplier2 = supplier2;
        }

        @Override
        void tryRun() {
            if (mSupplier.mResult != null && mSupplier2.mResult != null) {
                if (claim()) {
                    mExecutor.execute(this);
                }
            }
        }

        @Override
        public abstract void run();
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

    private static final class BiRelayAction<T, U> extends BiAction<T, U, Void> {
        BiRelayAction(Promise<T> supplier1, Promise<U> supplier2, Promise<Void> consumer) {
            super(null, supplier1, supplier2, consumer);
        }

        @Override
        final void tryRun() {
            if (mSupplier.mResult != null && mSupplier2.mResult != null) {
                if (claim()) {
                    run();
                }
            }
        }

        @Override
        public final void run() {
            if (mSupplier.mResult instanceof Promise.Error) {
                mConsumer.setResult(toCompletionException((Promise.Error) mSupplier.mResult));
            } else if (mSupplier2.mResult instanceof Promise.Error) {
                mConsumer.setResult(toCompletionException((Promise.Error) mSupplier2.mResult));
            } else {
                mConsumer.setResult(NULL);
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
        private Function<? super T, ? extends Future<U>> mFunction;

        CompositionFunctionAction(Executor executor, Promise<T> supplier, Promise<U> consumer,
                 Function<? super T, ? extends Future<U>> function) {
            super(executor, supplier, consumer);
            mFunction = function;
        }

        @Override
        public final void run() {
            if (!(mSupplier.mResult instanceof Promise.Error)) {
                try {
                    @SuppressWarnings("unchecked") T result = (T) mSupplier.mResult;
                    Promise<U> u = mFunction.apply(result != NULL ? result : null).toPromise();
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

    private static final class BiCompositionFunctionAction<T, U> extends Action<T, U> {
        private BiFunction<? super T, Throwable, ? extends Future<U>> mFunction;

        BiCompositionFunctionAction(Executor executor, Promise<T> supplier, Promise<U> consumer,
                BiFunction<? super T, Throwable, ? extends Future<U>> function) {
            super(executor, supplier, consumer);
            mFunction = function;
        }

        @Override
        public final void run() {
            try {
                Promise<U> u;
                if (!(mSupplier.mResult instanceof Promise.Error)) {
                    @SuppressWarnings("unchecked") T result = (T) mSupplier.mResult;
                    u = mFunction.apply(result != NULL ? result : null, null).toPromise();
                } else {
                    Throwable throwable = ((Promise.Error) mSupplier.mResult).mThrowable;
                    u = mFunction.apply(null, throwable).toPromise();
                }
                mConsumer.completeWith(u);
                return;
            } catch (Throwable e) {
                mConsumer.setResult(toCompletionException(e));
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
                    mAction.accept(result != NULL ? result : null);
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

    private static final class AnyOfAction<T> extends Action<T, Object> {
        final Action<?, ?>[] mActions;

        AnyOfAction(Promise<T> supplier, Promise<Object> consumer, Action<?, ?>[] actions) {
            super(null, supplier, consumer);
            mActions = actions;
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

            for (Action<?, ?> a : mActions) {
                if (a != this) {
                    a.mSupplier.removeAction(a);
                }
            }
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

    private static final class DelayAction<T> extends Action<T, T> {
        private long mDelay;

        DelayAction(Promise<T> supplier, Promise<T> consumer, long delay) {
            super(null, supplier, consumer);
            mDelay = delay;
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
                try {
                    @SuppressWarnings("unchecked") T result = (T) mSupplier.mResult;
                    mConsumer.completeOnTimeout(result != NULL ? result : null, mDelay);
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

    private void removeAction(Action<?, ?> action) {
        if (mActions.get() != null) {
            mActions.get().remove(action);
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
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("TimeoutExecutorDaemon");
                return t;
            }
        }

        static final class Completion<U> implements Runnable {
            final Promise<U> mConsumer;
            final U mValue;

            Completion(Promise<U> consumer, U value) {
                mConsumer = consumer;
                mValue = value;
            }

            @Override
            public void run() {
                if (mConsumer != null && !mConsumer.isDone()) {
                    mConsumer.complete(mValue);
                }
            }
        }

        static final class Exception implements Runnable {
            final Promise<?> mConsumer;
            final String mMessage;
            final Throwable mCause;

            Exception(Promise<?> consumer, String message, Throwable cause) {
                mConsumer = consumer;
                mMessage = message;
                mCause = cause;
            }

            @Override
            public void run() {
                if (mConsumer != null && !mConsumer.isDone()) {
                    mConsumer.completeWith(new TimeoutException(mMessage, mCause));
                }
            }
        }
    }
}
