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
package org.microbean.environment.provider;

import java.lang.reflect.Type;

import java.util.NoSuchElementException;
import java.util.Objects;

import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

import org.microbean.environment.api.Path;
import org.microbean.environment.api.Qualifiers;

/**
 * A {@link Supplier} of a value that is additionally qualified by a
 * {@link Qualifiers} and a {@link Path} partially identifying the
 * kinds of {@link Qualifiers} and {@link Path}s for which it might be
 * suitable.
 *
 * <p>{@link Value}s are typically returned by {@link Provider}
 * implementations.</p>
 *
 * <p>A {@link Value} once received retains no reference to whatever
 * produced it and can be regarded as an authoritative source for
 * (possibly ever-changing) values going forward.  Notably, it can be
 * cached.</p>
 *
 * @param <T> the type of value this {@link Value} returns
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Supplier
 *
 * @see Provider
 */
public final class Value<T> implements Supplier<T> {


  /*
   * Instance fields.
   */


  private final Qualifiers qualifiers;

  private final Path<T> path;

  private final Supplier<? extends T> supplier;

  private final boolean nullsPermitted;

  private final boolean deterministic;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Qualifiers, Path, Supplier,
   * boolean, boolean)} constructor with the supplied arguments,
   * {@code null} for the value of the {@code defaults} parameter,
   * {@code true} for the value of the {@code nullsPermitted}
   * parameter and {@code true} for the value of the {@code
   * deterministic} parameter.</p>
   *
   * @param qualifiers the {@link Qualifiers} for which this {@link
   * Value} is suitable; must not be {@code null}
   *
   * @param path the {@link Path}, possibly {@linkplain
   * Path#isRelative() relative}, for which this {@link Value} is
   * suitable; must not be {@code null}
   *
   * @param value the value that will be returned by the {@link
   * #get()} method; may be {@code null}
   *
   * @see #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean)
   */
  public Value(final Qualifiers qualifiers,
               final Path<T> path,
               final T value) {
    // Because defaults are null, it actually doesn't matter what the
    // trailing two booleans are for anything other than informational
    // purposes.
    this(null, qualifiers, path, () -> value, true, true);
  }

  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Qualifiers, Path, Supplier,
   * boolean, boolean)} constructor with the supplied arguments,
   * {@code null} for the value of the {@code defaults} parameter,
   * {@code true} for the value of the {@code nullsPermitted}
   * parameter and {@code false} for the value of the {@code
   * deterministic} parameter.</p>
   *
   * @param qualifiers the {@link Qualifiers} for which this {@link
   * Value} is suitable; must not be {@code null}
   *
   * @param path the {@link Path}, possibly {@linkplain
   * Path#isRelative() relative}, for which this {@link Value} is
   * suitable; must not be {@code null}
   *
   * @param supplier the actual {@link Supplier} that will return
   * values; must not be {@code null}
   *
   * @see #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean)
   */
  public Value(final Qualifiers qualifiers,
               final Path<T> path,
               final Supplier<? extends T> supplier) {
    // Because defaults are null, it actually doesn't matter what the
    // trailing two booleans are for anything other than informational
    // purposes.
    this(null, qualifiers, path, supplier, true, false);
  }

  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Qualifiers, Path, Supplier,
   * boolean, boolean)} constructor with the supplied arguments and
   * {@code null} for the value of the {@code defaults} parameter.</p>
   *
   * @param qualifiers the {@link Qualifiers} for which this {@link
   * Value} is suitable; must not be {@code null}
   *
   * @param path the {@link Path}, possibly {@linkplain
   * Path#isRelative() relative}, for which this {@link Value} is
   * suitable; must not be {@code null}
   *
   * @param supplier the actual {@link Supplier} that will return
   * values; must not be {@code null}
   *
   * @param nullsPermitted whether {@code null} values returned from
   * the {@link #get()} method are legal values or indicate the
   * (possibly transitory) absence of a value
   *
   * @param deterministic a {@code boolean} indicating whether the
   * supplied {@code supplier} returns a singleton from its {@link
   * Supplier#get()} method
   *
   * @see #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean)
   */
  public Value(final Qualifiers qualifiers,
               final Path<T> path,
               final Supplier<? extends T> supplier,
               final boolean nullsPermitted,
               final boolean deterministic) {
    // Because defaults are null, it actually doesn't matter what the
    // trailing two booleans are for anything other than informational
    // purposes.
    this(null, qualifiers, path, supplier, nullsPermitted, deterministic);
  }

  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Qualifiers, Path, Supplier,
   * boolean, boolean)} constructor with the supplied arguments and
   * {@code true} for the value of the {@code nullsPermitted}
   * parameter and {@code false} for the value of the {@code
   * deterministic} parameter.</p>
   *
   * @param defaults a {@link Supplier} to be used in case this {@link
   * Value}'s {@link #get()} method throws either a {@link
   * NoSuchElementException} or an {@link
   * UnsupportedOperationException}; may be {@code null}
   *
   * @param qualifiers the {@link Qualifiers} for which this {@link
   * Value} is suitable; must not be {@code null}
   *
   * @param path the {@link Path}, possibly {@linkplain
   * Path#isRelative() relative}, for which this {@link Value} is
   * suitable; must not be {@code null}
   *
   * @param supplier the actual {@link Supplier} that will return
   * values; must not be {@code null}
   *
   * @see #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean)
   */
  public Value(final Supplier<? extends T> defaults,
               final Qualifiers qualifiers,
               final Path<T> path,
               final Supplier<? extends T> supplier) {
    this(defaults, qualifiers, path, supplier, true, false);
  }

  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Qualifiers, Path, Supplier,
   * boolean, boolean)} constructor with the value of the {@code
   * defaults} parameter and arguments derived from the supplied {@code
   * source}.</p>
   *
   * @param defaults a {@link Supplier} to be used in case this {@link
   * Value}'s {@link #get()} method throws either a {@link
   * NoSuchElementException} or an {@link
   * UnsupportedOperationException}; may be {@code null}
   *
   * @param source the {@link Value} from which other arguments will
   * be derived; must not be {@code null}
   *
   * @exception NullPointerException if {@code source} is {@code null}
   *
   * @see #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean)
   */
  public Value(final Supplier<? extends T> defaults,
               final Value<T> source) {
    this(defaults, source.qualifiers(), source.path(), source, source.nullsPermitted(), source.deterministic());
  }

  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Qualifiers, Path, Supplier,
   * boolean, boolean)} constructor with no defaults and arguments
   * derived from the supplied {@code source}.</p>
   *
   * @param source the {@link Value} from which other arguments will
   * be derived; must not be {@code null}
   *
   * @exception NullPointerException if {@code source} is {@code null}
   *
   * @see #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean)
   */
  public Value(final Value<T> source) {
    // Because defaults are null, it actually doesn't matter what the
    // trailing two booleans are for anything other than informational
    // purposes.
    this(null, source.qualifiers(), source.path(), source, source.nullsPermitted(), source.deterministic());
  }

  /**
   * Creates a new {@link Value}.
   *
   * @param defaults a {@link Supplier} to be used in case this {@link
   * Value}'s {@link #get()} method throws either a {@link
   * NoSuchElementException} or an {@link
   * UnsupportedOperationException}; may be {@code null}
   *
   * @param qualifiers the {@link Qualifiers} for which this {@link
   * Value} is suitable; must not be {@code null}
   *
   * @param path the {@link Path}, possibly {@linkplain
   * Path#isRelative() relative}, for which this {@link Value} is
   * suitable; must not be {@code null}
   *
   * @param supplier the actual {@link Supplier} that will return
   * values; must not be {@code null}
   *
   * @param nullsPermitted whether {@code null} values returned from
   * the {@link #get()} method are legal values or indicate the
   * (possibly transitory) absence of a value
   *
   * @param deterministic a {@code boolean} indicating whether the
   * supplied {@code supplier} returns a singleton from its {@link
   * Supplier#get()} method
   *
   * @see #get()
   */
  public Value(final Supplier<? extends T> defaults,
               final Qualifiers qualifiers,
               final Path<T> path,
               final Supplier<? extends T> supplier,
               final boolean nullsPermitted,
               final boolean deterministic) {
    super();
    this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers");
    this.path = Objects.requireNonNull(path, "path");
    Objects.requireNonNull(supplier, "supplier");
    this.nullsPermitted = nullsPermitted;
    this.deterministic = deterministic;
    if (deterministic) {
      if (defaults == null) {
        this.supplier = supplier;
      } else if (nullsPermitted) {
        this.supplier = new Supplier<>() {
            private volatile Supplier<? extends T> s = supplier;
            @Override
            public final T get() {
              final Supplier<? extends T> s = this.s;
              try {
                return s.get();
              } catch (final NoSuchElementException | UnsupportedOperationException e) {
                if (s == defaults) {
                  throw e;
                }
                this.s = defaults;
                return defaults.get();
              }
            }
          };
      } else {
        this.supplier = new Supplier<>() {
            private volatile Supplier<? extends T> s = supplier;
            @Override
            public final T get() {
              final Supplier<? extends T> s = this.s;
              T value = null;
              try {
                value = s.get();
              } catch (final NoSuchElementException | UnsupportedOperationException e) {
                if (s == defaults) {
                  throw e;
                }
                return defaults.get();
              }
              if (value == null) {
                if (s != defaults) {
                  this.s = defaults;
                  value = defaults.get();
                }
              }
              return value;
            }
          };
      }
    } else if (defaults == null) {
      this.supplier = supplier;
    } else if (nullsPermitted) {
      this.supplier = new Supplier<>() {
          private volatile Supplier<? extends T> s = supplier;
          @Override
          public final T get() {
            final Supplier<? extends T> s = this.s;
            try {
              return s.get();
            } catch (final NoSuchElementException | UnsupportedOperationException e) {
              if (s == defaults) {
                throw e;
              }
              this.s = defaults;
              return defaults.get();
            }
          }
        };
    } else {
      this.supplier = new Supplier<>() {
          private volatile Supplier<? extends T> s = supplier;
          @Override
          public final T get() {
            final Supplier<? extends T> s = this.s;
            T value = null;
            try {
              value = s.get();
            } catch (final NoSuchElementException | UnsupportedOperationException e) {
              if (s == defaults) {
                throw e;
              }
              return defaults.get();
            }
            return value == null ? defaults.get() : value;
          }
        };
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns the {@link Qualifiers} with which this {@link Value} is
   * associated.
   *
   * @return the {@link Qualifiers} with which this {@link Value} is
   * associated
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean)
   */
  public final Qualifiers qualifiers() {
    return this.qualifiers;
  }

  /**
   * Returns the {@link Path} with which this {@link Value} is
   * associated.
   *
   * @return the {@link Path} with which this {@link Value} is
   * associated
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean)
   */
  public final Path<T> path() {
    return this.path;
  }

  /**
   * Invokes the {@link Supplier#get() get()} method of the {@link
   * Supplier} supplied at {@linkplain #Value(Supplier, Qualifiers,
   * Path, Supplier, boolean, boolean) construction time} and returns
   * its value, which may be {@code null}.
   *
   * <p>Note that a return value of {@code null} indicates the absence
   * of a value only if the {@link #nullsPermitted()} method returns
   * {@code false}.</p>
   *
   * @return tbe return value of an invocation of the {@link
   * Supplier#get() get()} method of the {@link Supplier} supplied at
   * {@linkplain #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean) construction time}, which may be {@code null}
   *
   * @exception NoSuchElementException if this method should no longer
   * be invoked because there is no chance it will ever produce a
   * suitable value again
   *
   * @exception UnsupportedOperationException if this method should no longer
   * be invoked because there is no chance it will ever produce a
   * suitable value again
   *
   * @see #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean)
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads, provided that the {@link Supplier} supplied at
   * {@linkplain #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean) construction time} is also safe for concurrent use by
   * multiple threads.
   *
   * @idempotency This method is as idempotent and deterministic as
   * the {@link Supplier} supplied at {@linkplain #Value(Supplier,
   * Qualifiers, Path, Supplier, boolean, boolean) construction time}.
   */
  @Override // Supplier<T>
  public final T get() {
    return this.supplier.get();
  }

  /**
   * Returns {@code true} if and only if {@code null} returned from
   * the {@link #get()} method is a permitted value.
   *
   * <p>If there were no defaults supplied at {@linkplain
   * #Value(Supplier, Qualifiers, Path, Supplier, boolean, boolean)
   * construction time}, then this method is informational only.</p>
   *
   * @return {@code true} if and only if {@code null} returned from
   * the {@link #get()} method is a permitted value
   *
   * @see #Value(Supplier, Qualifiers, Path, Supplier, boolean,
   * boolean)
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final boolean nullsPermitted() {
    return this.nullsPermitted;
  }

  /**
   * Returns {@code true} if and only if it is known that the {@link
   * Supplier} supplied at {@linkplain #Value(Supplier, Qualifiers,
   * Path, Supplier, boolean, boolean) construction time} will return
   * one and only one value from its {@link Supplier#get() get()}
   * method.
   *
   * <p>If there were no defaults supplied at {@linkplain
   * #Value(Supplier, Qualifiers, Path, Supplier, boolean, boolean)
   * construction time}, then this method is informational only.</p>
   *
   * @return {@code true} if and only if it is known that the {@link
   * Supplier} supplied at {@linkplain #Value(Supplier, Qualifiers,
   * Path, Supplier, boolean, boolean) construction time} will return
   * one and only one value from its {@link Supplier#get() get()}
   * method
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final boolean deterministic() {
    return this.deterministic;
  }

  /**
   * Returns the result of invoking {@link Path#type()} on the
   * return value of this {@link Value}'s {@link #path()} method.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the result of invoking {@link Path#type()} on the
   * return value of this {@link Value}'s {@link #path()} method;
   * never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by
   * multiple threads.
   *
   * @see #path()
   *
   * @see Path#type()
   */
  @Convenience
  public final Type type() {
    return this.path().type();
  }

  /**
   * Returns the result of invoking {@link Path#typeErasure()} on the
   * return value of this {@link Value}'s {@link #path()} method.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the result of invoking {@link Path#typeErasure()} on the
   * return value of this {@link Value}'s {@link #path()} method;
   * never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by
   * multiple threads.
   *
   * @see #path()
   *
   * @see Path#typeErasure()
   */
  @Convenience
  public final Class<T> typeErasure() {
    return this.path().typeErasure();
  }

  /**
   * Returns a hashcode for this {@link Value} calculated from its
   * {@link #qualifiers() Qualifiers}, its {@link #path() Path},
   * {@linkplain #nullsPermitted() whether it permits
   * <code>null</code>s} and {@linkplain #deterministic() whether it
   * is deterministic}.
   *
   * @return a hashcode for this {@link Value}
   *
   * @idempotency This method is idempotent and and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by
   * multiple threads.
   *
   * @see #equals(Object)
   *
   * @see #path()
   *
   * @see #qualifiers()
   *
   * @see #nullsPermitted()
   *
   * @see #deterministic()
   */
  @Override // Object
  public final int hashCode() {
    int hashCode = 17;
    Object v = this.qualifiers();
    int c = v == null ? 0 : v.hashCode();
    hashCode = 37 * hashCode + c;

    v = this.path();
    c = v == null ? 0 : v.hashCode();
    hashCode = 37 * hashCode + c;

    c = this.nullsPermitted() ? 1 : 0;
    hashCode = 37 * hashCode + c;

    c = this.deterministic() ? 1 : 0;
    hashCode = 37 * hashCode + c;

    return hashCode;
  }

  /**
   * Returns {@code true} if this {@link Value} is equal to the
   * supplied {@link Object}.
   *
   * <p>This method will return {@code true} if and only if the
   * following conditions hold:</p>
   *
   * <ul>
   *
   * <li>The supplied {@link Object} is not {@code null}</li>
   *
   * <li>The supplied {@link Object}'s {@link Object#getClass()}
   * method returns {@link Value Value.class}</li>
   *
   * <li>{@link Objects#equals(Object, Object)
   * Objects.equals(this.qualifiers(), otherValue.qualifiers())} returns
   * {@code true}</li>
   *
   * <li>{@link Objects#equals(Object, Object)
   * Objects.equals(this.path(), otherValue.path())} returns {@code
   * true}</li>
   *
   * <li>{@code this.nullsPermitted() &amp;&amp;
   * otherValue.nullsPermitted() &amp;&amp; this.deterministic()
   * &amp;&amp; otherValue.deterministic()} is {@code true}</li>
   *
   * </ul>
   *
   * @param other the {@link Object} to test; may be {@code null} in
   * which case {@code false} will be returned
   *
   * @return {@code true} if this {@link Value} is equal to the
   * supplied {@link Object}; {@code false} otherwise
   *
   * @idempotency This method is idempotent and and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by
   * multiple threads.
   *
   * @see #qualifiers()
   *
   * @see #path()
   *
   * @see #nullsPermitted()
   *
   * @see deterministic()
   */
  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      final Value<?> her = (Value<?>)other;
      return
        Objects.equals(this.qualifiers(), her.qualifiers()) &&
        Objects.equals(this.path(), her.path()) &&
        this.nullsPermitted() && her.nullsPermitted() &&
        this.deterministic() && her.deterministic();
    } else {
      return false;
    }
  }

}
