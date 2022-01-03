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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.function.Predicate;

import java.util.stream.Stream;

import org.microbean.development.annotation.Convenience;

import static java.util.Collections.emptySortedMap;
import static java.util.Collections.emptySortedSet;
import static java.util.Collections.unmodifiableSortedMap;
import static java.util.Collections.unmodifiableSortedSet;

/**
 * An immutable set of {@link String}-typed key-value pair coordinates
 * that locate an environment in environment space.
 *
 * <p>This is a <a
 * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/doc-files/ValueBased.html">value-based
 * class</a>.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #of(CharSequence, CharSequence)
 */
public final class Qualifiers {


  /*
   * Static fields.
   */


  private static final Qualifiers EMPTY_QUALIFIERS = new Qualifiers();


  /*
   * Instance fields.
   */


  private final SortedMap<String, String> qualifiers;


  /*
   * Constructors.
   */


  private Qualifiers() {
    this(emptySortedMap());
  }

  private Qualifiers(final Map<? extends String, ? extends String> qualifiers) {
    super();
    this.qualifiers = qualifiers == null || qualifiers.isEmpty() ? emptySortedMap() : unmodifiableSortedMap(new TreeMap<>(qualifiers));
  }

  private Qualifiers(final SortedMap<String, ? extends String> qualifiers) {
    super();
    this.qualifiers = qualifiers == null || qualifiers.isEmpty() ? emptySortedMap() : unmodifiableSortedMap(new TreeMap<>(qualifiers));
  }


  /*
   * Instance methods.
   */


  /**
   * Returns an immutable {@link SortedMap} of the qualifiers in this
   * {@link Qualifiers}.
   *
   * @return an immutable {@link SortedMap} of the qualifiers in this
   * {@link Qualifiers}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final SortedMap<String, String> toMap() {
    return this.qualifiers;
  }

  /**
   * Returns {@code true} if this {@link Qualifiers} has no entries.
   *
   * @return {@code true} if this {@link Qualifiers} has no entries
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #toMap()
   *
   * @see Map#isEmpty()
   */
  public final boolean isEmpty() {
    return this.toMap().isEmpty();
  }

  /**
   * Returns the number of entries in this {@link Qualifiers}.
   *
   * <p>The number returned is always {@code 0} or greater.</p>
   *
   * @return the number of entries in this {@link Qualifiers}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #toMap()
   *
   * @see Map#size()
   */
  public final int size() {
    return this.toMap().size();
  }

  /**
   * Returns a non-{@code null}, immutable {@link Set} of {@link
   * Entry} instances representing individual qualifier entries.
   *
   * @return a non-{@code null} {@link Set} of {@link Entry} instances
   * representing individual qualifier entries
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #toMap()
   *
   * @see SortedMap#entrySet()
   */
  @Convenience
  public final Set<Entry<String, String>> entrySet() {
    return this.toMap().entrySet();
  }

  /**
   * Returns an immutable {@link Set} containing all the qualifier
   * names stored by this {@link Qualifiers}.
   *
   * @return an immutable {@link Set} containing all the qualifier
   * names stored by this {@link Qualifiers}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final Set<String> keySet() {
    return this.toMap().keySet();
  }

  /**
   * Returns a qualifier value indexed under the supplied {@code
   * name}, or {@code null}.
   *
   * @param name the qualifier name; must not be {@code null}
   *
   * @return a qualifier value indexed under the supplied {@code
   * name}, or {@code null}
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final String get(final String name) {
    return this.qualifiers.get(name);
  }

  /**
   * Returns {@code true} if this {@link Qualifiers} logically
   * contains the supplied {@link Qualifiers}.
   *
   * <p>A {@link Qualifiers} is said to contain another {@link Qualifiers} if either:</p>
   *
   * <ul>
   *
   * <li>the two {@link Qualifiers} instances are identical, or</li>
   *
   * <li>the {@linkplain #size() size} of the first {@link Qualifiers}
   * is greater than the {@linkplain #size() size} of the second
   * {@link Qualifiers}, and the {@linkplain #entrySet() entry set} of
   * this {@link Qualifiers} {@linkplain
   * Set#containsAll(java.util.Collection) contains all} of the
   * entries in the second {@link Qualifiers}' {@linkplain #entrySet()
   * entry set}</li>
   *
   * </ul>
   *
   * @param other the {@link Qualifiers} to test; must not be {@code
   * null}
   *
   * @return {@code true} if this {@link Qualifiers} logically
   * contains the supplied {@link Qualifiers}
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #size()
   *
   * @see #entrySet()
   *
   * @see Set#containsAll(java.util.Collection)
   */
  public final boolean contains(final Qualifiers other) {
    return this == other || this.size() >= other.size() && this.entrySet().containsAll(other.entrySet());
  }

  /**
   * Returns {@code true} if and only if this {@link Qualifiers} is a
   * subset of the supplied {@link Qualifiers}.
   *
   * @param other the other {@link Qualifiers}; must not be {@code
   * null}
   *
   * @return {@code true} if and only if this {@link Qualifiers} is a
   * subset of the supplied {@link Qualifiers}
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final boolean isSubsetOf(final Qualifiers other) {
    return other == this || other.contains(this);
  }

  /**
   * Returns the number of entries this {@link Qualifiers} has in
   * common with the supplied {@link Qualifiers}.
   *
   * <p>The number returned will be {@code 0} or greater.</p>
   *
   * @param other the other {@link Qualifiers}; may be {@code null} in
   * which case {@code 0} will be returned
   *
   * @return the number of entries this {@link Qualifiers} has in
   * common with the supplied {@link Qualifiers}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int intersectionSize(final Qualifiers other) {
    if (other == this) {
      // Just an identity check to rule this easy case out.
      return this.size();
    } else if (other == null || other.isEmpty()) {
      return 0;
    } else {
      final Set<? extends Entry<String, String>> otherEntrySet = other.entrySet();
      return (int)this.entrySet()
        .stream()
        .filter(otherEntrySet::contains)
        .count();
    }
  }

  /**
   * Returns the size of the <em>symmetric difference</em> between
   * this {@link Qualifiers} and the supplied {@link Qualifiers}.
   *
   * <p>The number returned is always {@code 0} or greater.</p>
   *
   * <p>The size of the symmetric difference between two {@link
   * Qualifiers} instances is the number of qualifier entries that are
   * in one {@link Qualifiers} instance but not in the other.</p>
   *
   * @param other the other {@link Qualifiers} instance; may be {@code
   * null} in which case the return value of an invocation of this
   * {@link Qualifiers}' {@link #size()} method will be returned
   *
   * @return the size of the <em>symmetric difference</em> between
   * this {@link Qualifiers} and the supplied {@link Qualifiers};
   * always {@code 0} or greater
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int symmetricDifferenceSize(final Qualifiers other) {
    if (other == this) {
      // Just an identity check to rule this easy case out.
      return 0;
    } else if (other == null || other.isEmpty()) {
      return this.size();
    } else if (this.equals(other)) {
      return 0;
    } else {
      final Set<Entry<?, ?>> otherSymmetricDifference = new HashSet<>(this.entrySet());
      other.entrySet().stream()
        .filter(Predicate.not(otherSymmetricDifference::add))
        .forEach(otherSymmetricDifference::remove);
      return otherSymmetricDifference.size();
    }
  }

  /**
   * Returns a hashcode for this {@link Qualifiers}.
   *
   * @return a hashcode for this {@link Qualifiers}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #toMap()
   *
   * @see Map#hashCode()
   */
  @Override // Object
  public final int hashCode() {
    return this.toMap().hashCode();
  }

  /**
   * Returns {@code true} if this {@link Qualifiers} is equal to the
   * supplied {@link Object}.
   *
   * <p>This {@link Qualifiers} is equal to the supplied {@link
   * Object} if and only if all of the following conditions hold:</p>
   *
   * <ul>
   *
   * <li>the supplied {@link Object} is non-{@code null} and returns
   * {@link Qualifiers Qualifiers.class} from its {@link
   * Object#getClass()} method</li>
   *
   * <li>The return value of an invocation of the {@link #toMap()}
   * method on this {@link Qualifiers} instance is {@linkplain
   * Map#equals(Object) equal to} the return value of an invocation of
   * the {@link #toMap()} method on the other {@link Qualifiers}
   * instance</li>
   *
   * </ul>
   *
   * @param other the {@link Object} to test; may be {@code null} in
   * which case {@code false} will be returned
   *
   * @return {@code true} if this {@link Qualifiers} is equal to the
   * supplied {@link Object}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see Map#equals(Object)
   */
  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other == null || this.getClass() != other.getClass()) {
      return false;
    } else {
      return this.toMap().equals(((Qualifiers)other).toMap());
    }
  }

  /**
   * Returns a non-{@code null} {@link String} representatino of this
   * {@link Qualifiers}.
   *
   * <p>The format of the returned {@link String} is deliberately
   * unspecified.</p>
   *
   * @return a non-{@code null} {@link String} representatino of this
   * {@link Qualifiers}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Object
  public final String toString() {
    final StringJoiner sj = new StringJoiner(";");
    for (final Entry<String, String> entry : this.entrySet()) {
      sj.add(entry.getKey() + "=" + entry.getValue());
    }
    return sj.toString();
  }


  /*
   * Static methods.
   */


  /**
   * Returns a {@link Qualifiers} whose {@link #size()} method returns
   * {@code 0} (and whose {@link #isEmpty()} method returns {@code
   * true}).
   *
   * @return a {@link Qualifiers} whose {@link #size()} method returns
   * {@code 0} (and whose {@link #isEmpty()} method returns {@code
   * true})
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public static final Qualifiers of() {
    return EMPTY_QUALIFIERS;
  }

  /**
   * Returns a {@link Qualifiers} equal to one consisting of the
   * entries represented by the supplied {@link Map}.
   *
   * @param map the {@link Map} of qualifier names and values; may be
   * {@code null} in which case the return value of the {@link #of()}
   * method will be returned
   *
   * @return a {@link Qualifiers} equal to one consisting of the
   * entries represented by the supplied {@link Map}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(CharSequence, CharSequence)
   */
  public static final Qualifiers of(final Map<? extends CharSequence, ? extends CharSequence> map) {
    if (map == null || map.isEmpty()) {
      return of();
    } else {
      final SortedMap<String, String> newMap = new TreeMap<>();
      map.entrySet().forEach(e -> newMap.put(e.getKey().toString(), e.getValue().toString()));
      return new Qualifiers(newMap);
    }
  }

  /**
   * Returns a new {@link Qualifiers} with a single qualifier whose
   * name is {@code value} and whose value is the return value of an
   * invocation of {@link CharSequence#toString()} on the supplied
   * {@code value0} argument.
   *
   * @param value0 the new value; must not be {@code null}
   *
   * @return a new {@link Qualifiers} with a single qualifier whose
   * name is {@code value} and whose value is the return value of an
   * invocation of {@link CharSequence#toString()} on the supplied
   * {@code value0} argument
   *
   * @exception NullPointerException if {@code value0} is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(CharSequence, CharSequence)
   */
  public static final Qualifiers of(final CharSequence value0) {
    return of("value", value0);
  }

  /**
   * Returns a new {@link Qualifiers} with a single qualifier whose
   * name is the return value of an invocation of {@link
   * CharSequence#toString()} on the supplied {@code name0} argument,
   * and whose value is the return value of an invocation of {@link
   * CharSequence#toString()} on the supplied {@code value0} argument.
   *
   * @param name0 the qualifier name; must not be {@code null}
   *
   * @param value0 the new value; must not be {@code null}
   *
   * @return a new {@link Qualifiers} with a single qualifier whose
   * name is the return value of an invocation of {@link
   * CharSequence#toString()} on the supplied {@code name0} argument,
   * and whose value is the return value of an invocation of {@link
   * CharSequence#toString()} on the supplied {@code value0} argument
   *
   * @exception NullPointerException if either {@code name0} or {@code
   * value0} is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public static final Qualifiers of(final CharSequence name0, final CharSequence value0) {
    return new Qualifiers(Map.of(name0.toString(), value0.toString()));
  }

  /**
   * Returns a {@link Qualifiers} consisting of the entries
   * represented by the supplied alternating name-value pairs.
   *
   * @param name0 a qualifier name; must not be {@code null}
   *
   * @param value0 the qualifier value corresponding to the
   * immediately preceding qualifier name; must not be {@code null}
   *
   * @param name1 a qualifier name; must not be {@code null}
   *
   * @param value1 the qualifier value corresponding to the
   * immediately preceding qualifier name; must not be {@code null}
   *
   * @return a {@link Qualifiers} consisting of the entries
   * represented by the supplied alternating name-value pairs
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Convenience
  public static final Qualifiers of(final CharSequence name0, final CharSequence value0,
                                    final CharSequence name1, final CharSequence value1) {
    final SortedMap<String, String> map = new TreeMap<>();
    map.put(name0.toString(), value0.toString());
    map.put(name1.toString(), value1.toString());
    return new Qualifiers(map);
  }

  /**
   * Returns a {@link Qualifiers} consisting of the entries
   * represented by the supplied alternating name-value pairs.
   *
   * @param name0 a qualifier name; must not be {@code null}
   *
   * @param value0 the qualifier value corresponding to the
   * immediately preceding qualifier name; must not be {@code null}
   *
   * @param name1 a qualifier name; must not be {@code null}
   *
   * @param value1 the qualifier value corresponding to the
   * immediately preceding qualifier name; must not be {@code null}
   *
   * @param name2 a qualifier name; must not be {@code null}
   *
   * @param value2 the qualifier value corresponding to the
   * immediately preceding qualifier name; must not be {@code null}
   *
   * @return a {@link Qualifiers} consisting of the entries
   * represented by the supplied alternating name-value pairs
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Convenience
  public static final Qualifiers of(final CharSequence name0, final CharSequence value0,
                                    final CharSequence name1, final CharSequence value1,
                                    final CharSequence name2, final CharSequence value2) {
    final SortedMap<String, String> map = new TreeMap<>();
    map.put(name0.toString(), value0.toString());
    map.put(name1.toString(), value1.toString());
    map.put(name2.toString(), value2.toString());
    return new Qualifiers(map);
  }

  /**
   * Returns a {@link Qualifiers} consisting of the entries
   * represented by the supplied alternating name-value pairs.
   *
   * <p>The supplied {@code nameValuePairs} must be an even-numbered
   * array of non-{@code null} {@link CharSequence} instances, where
   * the zero-based even-numbered elements represent qualifier names,
   * and the zero-based odd-numbered elements represent qualifier
   * values.</p>
   *
   * @param nameValuePairs the name-value pairs as described above;
   * may be {@code null} in which case the return value of the {@link
   * #of()} method will be returned
   *
   * @return a {@link Qualifiers} consisting of the entries
   * represented by the supplied alternating name-value pairs
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public static final Qualifiers of(final CharSequence... nameValuePairs) {
    if (nameValuePairs == null || nameValuePairs.length <= 0) {
      return Qualifiers.of();
    } else if (nameValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("nameValuePairs: " + Arrays.toString(nameValuePairs));
    } else {
      final SortedMap<String, String> map = new TreeMap<>();
      for (int i = 0; i < nameValuePairs.length; i++) {
        map.put(nameValuePairs[i++].toString(), nameValuePairs[i].toString());
      }
      return new Qualifiers(map);
    }
  }

}
