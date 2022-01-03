/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2021–2022 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.environment.api;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import java.util.stream.Stream;

import org.microbean.development.annotation.Convenience;
import org.microbean.development.annotation.OverridingDiscouraged;

/**
 * A {@link Supplier} that is {@link Optional}-like, and a convenient
 * {@linkplain #optional() bridge to <code>Optional</code> objects}.
 *
 * <p>Unlike {@link Optional}, any implementation of this interface is
 * not a <a
 * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/doc-files/ValueBased.html">value-based
 * class</a>.  This is also why there are no {@code isPresent()} or
 * {@code isEmpty()} methods.</p>
 *
 * @param <T> the type of value implementations of this interface
 * {@linkplain #get() supply}
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #get()
 *
 * @see #optional()
 */
@FunctionalInterface
public interface OptionalSupplier<T> extends Supplier<T> {

  /**
   * Returns either the result of invoking the {@link #get()} method,
   * if a {@link RuntimeException} does not occur, or the result of
   * invoking the {@link Function#apply(Object)} method on the
   * supplied {@code handler} if a {@link RuntimeException} does
   * occur.
   *
   * @param handler the exception handler; must not be {@code null}
   *
   * @return either the result of invoking the {@link #get()} method,
   * if a {@link RuntimeException} does not occur, or the result of
   * invoking the {@link Function#apply(Object)} method on the
   * supplied {@code handler} if a {@link RuntimeException} does occur
   *
   * @exception NullPointerException if {@code handler} is {@code
   * null}
   *
   * @nullability This method may return {@code null}
   *
   * @idempotency This method is not guaranteed to be idempotent or
   * deterministic.
   *
   * @threadsafety This method itself is safe for concurrent use by
   * multiple threads, but the {@link #get()} method may not be, and
   * the {@link Function#apply(Object)} method of the supplied {@code
   * handler} may not be.
   */
  @OverridingDiscouraged
  public default T exceptionally(final Function<? super RuntimeException, ? extends T> handler) {
    try {
      return this.get();
    } catch (final RuntimeException e) {
      return handler.apply(e);
    }
  }

  /**
   * Invokes the {@link Optional#filter(Predicate)} method on the
   * return value of the {@link #optional()} method, supplying it with
   * the supplied {@code predicate}, and returns the result.
   *
   * @param predicate the {@link Predicate} in question; must not be
   * {@code null}
   *
   * @return the result of invoking the {@link
   * Optional#filter(Predicate)} method on the return value of the
   * {@link #optional()} method with the supplied {@code predicate};
   * never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method itself is safe for concurrent use by
   * multiple threads, but the {@link Predicate#test(Object)} method
   * of the supplied {@code predicate} may not be
   *
   * @see #optional()
   *
   * @see Optional#filter(Predicate)
   */
  @Convenience
  @OverridingDiscouraged
  public default Optional<T> filter(final Predicate<? super T> predicate) {
    return this.optional().filter(predicate);
  }

  /**
   * Invokes the {@link Optional#flatMap(Function)} method on the
   * return value of the {@link #optional()} method, supplying it with
   * the supplied {@code mapper}, and returns the result.
   *
   * @param <U> the mapped type
   *
   * @param mapper the {@link Function} performing the flat mapping
   * operation; must not be {@code null}
   *
   * @return the result of invoking the {@link
   * Optional#flatMap(Function)} method on the return value of the
   * {@link #optional()} method, supplying it with the supplied {@code
   * mapper}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method itself is safe for concurrent use by
   * multiple threads, but the {@link Predicate#test(Object)} method
   * of the supplied {@code predicate} may not be
   *
   * @see #optional()
   *
   * @see Optional#flatMap(Function)
   */
  @Convenience
  @OverridingDiscouraged
  public default <U> Optional<U> flatMap(final Function<? super T, ? extends Optional<? extends U>> mapper) {
    return this.optional().flatMap(mapper);
  }

  /**
   * Returns a value, which may be {@code null}, indicating (possibly
   * transitory) <em>emptiness</em>, or non-{@code null}, indicating
   * (possibly transitory) <em>presence</em>.
   *
   * <p>This method's contract extends {@link Supplier#get()}'s
   * contract with the following additional requirements:</p>
   *
   * <ul>
   *
   * <li>An implementation of this method need not be deterministic.</li>
   *
   * <li>An implementation of this method may indicate (possibly
   * transitory) emptiness by any of the following means:
   *
   * <ul><li>Returning {@code null}.  The emptiness is
   * <em>transitory</em>, i.e. a subsequent invocation of this method
   * may return a non-{@code null} result.</li>
   *
   * <li>Throwing a {@link NoSuchElementException}.  The emptiness is
   * <em>permanent</em>, i.e. all subsequent invocations of this
   * method will (must) also throw an {@link
   * NoSuchElementException}.</li>
   *
   * <li>Throwing an {@link UnsupportedOperationException}.  The
   * emptiness is <em>permanent</em>, i.e. all subsequent invocations
   * of this method will (must) also throw an {@link
   * UnsupportedOperationException}.</li>
   *
   * </ul></li>
   *
   * <li>The returning of a non-{@code null} value indicates (only)
   * <em>transitory presence</em>, i.e. a subsequent invocation of
   * this method may return {@code null} or throw either a {@link
   * NoSuchElementException} or an {@link
   * UnsupportedOperationException}.</li>
   *
   * </ul>
   *
   * @return a value, or {@code null} to indicate transitory emptiness
   *
   * @exception NoSuchElementException to indicate permanent emptiness
   *
   * @exception UnsupportedOperationException to indicate permanent
   * emptiness
   *
   * @nullability Implementations of this method may and often will
   * return {@code null}, indicating transitory emptiness.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent
   * but need not be deterministic.  However, once an implementation
   * of this method throws either a {@link NoSuchElementException} or
   * an {@link UnsupportedOperationException}, it must also do so for
   * every subsequent invocation.
   */
  @Override
  public T get();

  /**
   * Invokes the {@link #get()} method and handles either the result
   * or any {@link RuntimeException}s that are thrown using the
   * supplied {@link BiFunction}.
   *
   * <p>The first argument to the handler will be the return value of
   * the {@link #get()} method, which may be {@code null}.  The second
   * argument to the handler will be any {@link RuntimeException} that
   * was thrown during the invocation of the {@link #get()}
   * method.</p>
   *
   * @param <U> the type of the value returned
   *
   * @param handler the handler; must not be {@code null}
   *
   * @return the return value of the supplied {@code handler}'s {@link
   * BiFunction#apply(Object, Object)} method, which may be {@code
   * null}
   *
   * @nullability The default implementation of this method and its
   * overrides may return {@code null}.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees about either idempotency or
   * determinism are made.
   */
  @OverridingDiscouraged
  public default <U> U handle(final BiFunction<? super T, ? super RuntimeException, ? extends U> handler) {
    T value = null;
    try {
      value = this.get();
    } catch (final RuntimeException e) {
      return handler.apply(null, e);
    }
    return handler.apply(value, null);
  }

  /**
   * Invokes the {@link Optional#ifPresent(Consumer)} method on the return
   * value of the {@link #optional()} method, supplying it with the
   * supplied {@code action}, and returns the result.
   *
   * @param action the {@link Consumer} representing the action to
   * take; must not be {@code null}
   *
   * @exception NullPointerException if {@code action} is {@code null}
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic, but the supplied {@link Consumer}
   * may not be.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads, but the supplied {@link
   * Consumer}'s {@link Consumer#accept(Object)} method may not be
   *
   * @see #optional()
   *
   * @see Optional#ifPresent(Consumer)
   */
  @Convenience
  @OverridingDiscouraged
  public default void ifPresent(final Consumer<? super T> action) {
    this.optional().ifPresent(action);
  }

  /**
   * Invokes the {@link Optional#ifPresentOrElse(Consumer, Runnable)}
   * method on the return value of the {@link #optional()} method,
   * supplying it with the supplied {@code action} and {@code
   * emptyAction}, and returns the result.
   *
   * @param action the {@link Consumer} representing the action to
   * take if a value is present; must not be {@code null}
   *
   * @param emptyAction the {@link Runnable} representing the action to
   * take if a value is absent; must not be {@code null}
   *
   * @exception NullPointerException if {@code action} or {@code
   * emptyAction} is {@code null}
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic, but the supplied {@link Consumer}
   * and {@link Runnable} may not be.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads, but the supplied {@link
   * Consumer}'s {@link Consumer#accept(Object)} method may not be and
   * the supplied {@link Runnable}'s {@link Runnable#run()} method may
   * not be
   *
   * @see #optional()
   *
   * @see Optional#ifPresentOrElse(Consumer, Runnable)
   */
  @Convenience
  @OverridingDiscouraged
  public default void ifPresentOrElse(final Consumer<? super T> action, final Runnable emptyAction) {
    this.optional().ifPresentOrElse(action, emptyAction);
  }

  /**
   * Invokes the {@link Optional#map(Function)} method on the return
   * value of the {@link #optional()} method, supplying it with the
   * supplied {@code mapper}, and returns an {@link Optional}
   * representing the result.
   *
   * @param <U> the mapped type
   *
   * @param mapper the {@link Function} performing the mapping
   * operation; must not be {@code null}
   *
   * @return an {@link Optional} representing the result of invoking
   * the {@link Optional#map(Function)} method on the return value of
   * the {@link #optional()} method, supplying it with the supplied
   * {@code mapper}; never {@code null}
   *
   * @nullability This method never returns, and its overrides must
   * never return, {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method itself is, and its overrides must be,
   * safe for concurrent use by multiple threads, but the {@link
   * Function#apply(Object)} method of the supplied {@code mapper} may
   * not be
   *
   * @see #optional()
   *
   * @see Optional#map(Function)
   */
  @Convenience
  @OverridingDiscouraged
  public default <U> Optional<U> map(final Function <? super T, ? extends U> mapper) {
    return this.optional().map(mapper);
  }

  /**
   * Returns a non-{@code null} but possibly {@linkplain
   * Optional#isEmpty() empty} {@link Optional} representing this
   * {@link OptionalSupplier}'s {@linkplain #get() value}.
   *
   * <p>The default implementation of this method does not and its
   * overrides must not return {@code null}.</p>
   *
   * <p>The default implementation of this method catches all {@link
   * NoSuchElementException}s and {@link
   * UnsupportedOperationException}s and returns an empty {@link
   * Optional} in these cases.  To detect permanent versus transitory
   * emptiness, potential callers should use the {@link #get()} method
   * directly or either the {@link #optional(Function)} or {@link
   * #optional(BiFunction)} methods.</p>
   *
   * @return a non-{@code null} but possibly {@linkplain
   * Optional#isEmpty() empty} {@link Optional} representing this
   * {@link OptionalSupplier}'s {@linkplain #get() value}
   *
   * @nullability The default implementation of this method does not
   * and overrides must not return {@code null}.
   *
   * @threadsafety The default implementation of this method is and
   * overrides must be safe for concurrent use by multiple threads.
   *
   * @idempotency The default implementation and overrides of this
   * method may not be idempotent or deterministic.
   *
   * @see #optional(BiFunction)
   *
   * @see #get()
   */
  @Convenience
  @OverridingDiscouraged
  public default Optional<T> optional() {
    return this.optional(OptionalSupplier::returnNull);
  }

  /**
   * Invokes the {@link #handle(BiFunction)} method, supplying it with
   * the supplied {@code handler}, supplies its return value to the
   * {@link Optional#ofNullable(Object)} method, and returns the
   * result.
   *
   * @param <U> the type of the returned {@link Optional}
   *
   * @param handler the handler; must not be {@code null}
   *
   * @return the result of invoking the {@link
   * Optional#ofNullable(Object)} method supplied with the return
   * value of invoking the {@link #handle(BiFunction)} method with the
   * supplied {@code handler}
   *
   * @exception NullPointerException if {@code handler} is {@code
   * null}
   *
   * @nullability This method does not, and its overrides must not,
   * return {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @see #handle(BiFunction)
   *
   * @see Optional#ofNullable(Object)
   */
  @Convenience
  @OverridingDiscouraged
  public default <U> Optional<U> optional(final BiFunction<? super T, ? super RuntimeException, ? extends U> handler) {
    return Optional.ofNullable(this.handle(handler));
  }

  /**
   * Invokes the {@link #exceptionally(Function)} method, supplying it
   * with the supplied {@code handler}, supplies its return value to
   * the {@link Optional#ofNullable(Object)} method, and returns the
   * result.
   *
   * @param handler the handler; must not be {@code null}
   *
   * @return the result of invoking the {@link
   * Optional#ofNullable(Object)} method supplied with the return
   * value of invoking the {@link #exceptionally(Function)} method
   * with the supplied {@code handler}
   *
   * @exception NullPointerException if {@code handler} is {@code
   * null}
   *
   * @nullability This method does not, and its overrides must not,
   * return {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @see #exceptionally(Function)
   *
   * @see Optional#ofNullable(Object)
   */
  @Convenience
  @OverridingDiscouraged
  public default Optional<T> optional(final Function<? super RuntimeException, ? extends T> handler) {
    return Optional.ofNullable(this.exceptionally(handler));
  }

  /**
   * Invokes the {@link Optional#or(Supplier)} method on the return
   * value of the {@link #optional()} method, supplying it with the
   * supplied {@code supplier}, and returns the result.
   *
   * @param supplier the {@link Supplier} providing the alternate
   * {@link Optional}; must not be {@code null}
   *
   * @return the result of invoking the {@link Optional#or(Supplier)}
   * method on the return value of the {@link #optional()} method,
   * supplying it with the supplied {@code supplier}; never {@code
   * null}
   *
   * @exception NullPointerException if {@code supplier} is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method itself is safe for concurrent use by
   * multiple threads, but the {@link Supplier#get()} method of the
   * supplied {@code supplier} may not be
   *
   * @see #optional()
   *
   * @see Optional#or(Supplier)
   */
  @Convenience
  @OverridingDiscouraged
  public default Optional<T> or(final Supplier<? extends Optional<? extends T>> supplier) {
    return this.optional().or(supplier);
  }

  /**
   * Invokes the {@link Optional#orElse(Object)} method on the return
   * value of the {@link #optional()} method, supplying it with the
   * supplied {@code other}, and returns the result.
   *
   * @param other the alternate value; may be {@code null}
   *
   * @return the result of invoking the {@link
   * Optional#orElse(Object)} method on the return value of the {@link
   * #optional()} method, supplying it with the supplied {@code
   * other}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method itself is safe for concurrent use by
   * multiple threads, but the {@link Supplier#get()} method of the
   * supplied {@code supplier} may not be
   *
   * @see #optional()
   *
   * @see Optional#orElse(Object)
   */
  @Convenience
  @OverridingDiscouraged
  public default T orElse(final T other) {
    return this.optional().orElse(other);
  }

  /**
   * Invokes the {@link Optional#orElseGet(Supplier)} method on the
   * return value of the {@link #optional()} method, supplying it with
   * the supplied {@code supplier}, and returns the result.
   *
   * @param supplier the alternate value {@link Supplier}; must not be
   * {@code null}
   *
   * @return the result of invoking the {@link
   * Optional#orElseGet(Supplier)} method on the return value of the
   * {@link #optional()} method, supplying it with the supplied {@code
   * supplier}
   *
   * @exception NullPointerException if {@code supplier} is {@code
   * null}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method itself is safe for concurrent use by
   * multiple threads, but the {@link Supplier#get()} method of the
   * supplied {@code supplier} may not be
   *
   * @see #optional()
   *
   * @see Optional#orElseGet(Supplier)
   */
  @Convenience
  @OverridingDiscouraged
  public default T orElseGet(final Supplier<? extends T> supplier) {
    return this.optional().orElseGet(supplier);
  }

  /**
   * Invokes the {@link #orElseThrow(Supplier)} method with {@link
   * NoSuchElementException#NoSuchElementException()
   * NoSuchElementException::new} and returns the result.
   *
   * @return the result of invoking the {@link #orElseThrow(Supplier)}
   * method with {@link
   * NoSuchElementException#NoSuchElementException()
   * NoSuchElementException::new}
   *
   * @exception NoSuchElementException if the {@link #optional()}
   * method returns an {@linkplain Optional#isEmpty() empty} {@link
   * Optional}
   *
   * @nullability This method never returns, and its overrides must
   * never return, {@code null}.
   *
   * @idempotency This method is, and its overrides must be, as
   * idempotent and deterministic as the {@link #optional()} method.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads
   *
   * @see #orElseThrow(Supplier)
   */
  @Convenience
  @OverridingDiscouraged
  public default T orElseThrow() {
    return this.orElseThrow(NoSuchElementException::new);
  }

  /**
   * Invokes the {@link Optional#orElseThrow(Supplier)} method on the
   * return value of the {@link #optional()} method, supplying it with
   * the supplied {@code throwableSupplier}, and returns the result.
   *
   * @param <X> the type of {@link Throwable} the supplied {@code
   * throwableSupplier} {@linkplain Supplier#get() supplies}
   *
   * @param throwableSupplier the {@link Throwable} {@link Supplier};
   * must not be {@code null}
   *
   * @return the result of invoking the {@link
   * Optional#orElseThrow(Supplier)} method on the return value of the
   * {@link #optional()} method, supplying it with the supplied {@code
   * throwableSupplier}
   *
   * @exception NullPointerException if {@code supplier} is {@code
   * null}
   *
   * @exception X (actually {@code <X>}) if the {@link
   * #optional()} method returns an {@linkplain Optional#isEmpty()
   * empty} {@link Optional} and the {@link
   * Optional#orElseThrow(Supplier)} method throws an exception
   *
   * @nullability This method never returns, and its overrides must
   * never return, {@code null}.
   *
   * @idempotency This method is and its overrides must be idempotent
   * and deterministic.
   *
   * @threadsafety This method itself is, and its overrides must be,
   * safe for concurrent use by multiple threads.
   *
   * @see #optional()
   *
   * @see Optional#orElseThrow(Supplier)
   */
  @Convenience
  @OverridingDiscouraged
  public default <X extends Throwable> T orElseThrow(final Supplier<? extends X> throwableSupplier) throws X {
    return this.optional().orElseThrow(throwableSupplier);
  }

  /**
   * Invokes the {@link Optional#stream()} method on the return value
   * of the {@link #optional()} method, and returns the result.
   *
   * @return the result of invoking the {@link
   * Optional#stream()} method on the return value of the
   * {@link #optional()} method
   *
   * @nullability This method does not, and its overrides must not,
   * return {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @see #optional()
   *
   * @see Optional#stream()
   */
  @Convenience
  @OverridingDiscouraged
  public default Stream<T> stream() {
    return this.optional().stream();
  }

  private static <T> T returnNull(final RuntimeException e) {
    if (e instanceof NoSuchElementException || e instanceof UnsupportedOperationException) {
      return null;
    } else {
      throw e;
    }
  }

}
