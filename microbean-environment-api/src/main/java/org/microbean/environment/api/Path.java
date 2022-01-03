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

import java.lang.StackWalker.StackFrame;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import java.util.stream.Stream;

import org.microbean.development.annotation.Convenience;
import org.microbean.development.annotation.EntryPoint;
import org.microbean.development.annotation.Experimental;

import org.microbean.environment.api.Path.Element;

import org.microbean.type.Types;

/**
 * A sequence of {@link Path.Element}s representing part of a request
 * for a {@link Loader} to supply an environmental object.
 *
 * @param <T> the type of the last {@link Path.Element} in this {@link
 * Path}
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #of(Path.Element)
 */
public final class Path<T> implements Iterable<Element<?>> {


  /*
   * Static fields.
   */


  private static final StackWalker stackWalker = StackWalker.getInstance();

  private static final Path<Void> ROOT = new Path<>(List.of(Element.root()), true);


  /*
   * Instance fields.
   */


  private final List<Element<?>> elements;

  private final boolean transliterated;


  /*
   * Constructors.
   */


  private Path(final Element<T> element) {
    this(List.of(element), false);
  }

  private Path(final List<? extends Element<?>> elements, final boolean transliterated) {
    super();
    final int size = elements.size();
    switch (size) {
    case 0:
      throw new IllegalArgumentException("elements.isEmpty()");
    default:
      final List<Element<?>> newList = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        final Element<?> e = Objects.requireNonNull(elements.get(i));
        if (i != 0 && (e.isRoot() || i + 1 >= size && e.type().isEmpty())) {
          // No element other than the first one can be root.
          // The last element must have a present Type.
          throw new IllegalArgumentException("elements: " + elements);
        }
        newList.add(e);
      }
      this.elements = Collections.unmodifiableList(newList);
      this.transliterated = transliterated;
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns an {@link Iterator} over the {@link Element}s in this
   * {@link Path}.
   *
   * @return an {@link Iterator} over the {@link Path.Element}s in
   * this {@link Path}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  @Override // Iterable<Path.Element>
  public final Iterator<Element<?>> iterator() {
    return this.elements.iterator();
  }

  /**
   * Returns {@code true} if this {@link Path} is the result of
   * {@linkplain #transliterate(BiFunction) transliteration}.
   *
   * @return {@code true} if this {@link Path} is the result of
   * {@linkplain #transliterate(BiFunction) transliteration}; {@code
   * false} otherwise
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #transliterate(BiFunction)
   */
  public final boolean isTransliterated() {
    return this.transliterated;
  }

  /**
   * <em>Transliterates</em> this {@link Path} into another,
   * semantically equivalent {@link Path} by applying the supplied
   * {@link BiFunction}, and returns the transliterated {@link Path}.
   *
   * <p>The supplied {@link BiFunction} accepts a Java package name as
   * its first argument, which will be the first package name
   * {@linkplain StackWalker encountered in the current thread's
   * stack} that identifies a caller whose package name is not equal
   * to {@link Class#getPackageName() Path.class.getPackageName()}.
   * Its second argument is an {@link Element Element} from this
   * {@link Path}.  It must return an {@link Element} representing the
   * transliteration of its second argument (which may be the second
   * argument itself).</p>
   *
   * <p>Transliteration can be needed when a {@link Path} is defined
   * by a Java class and used by an application containing that Java
   * class&mdash;because another Java class may have used the same
   * element names to refer to different things.</p>
   *
   * <p>If this {@link Path} {@linkplain #isTransliterated() is
   * already transliterated} then it is returned.</p>
   *
   * @param f a {@link BiFunction} responsible for the
   * transliteration, element by element; may be {@code null}
   *
   * @return the transliterated {@link Path}, which may be this {@link
   * Path}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic, but the
   * supplied {@link BiFunction} may not be.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads, but the supplied {@link BiFunction} may not be.
   *
   * @see #isTransliterated()
   */
  @Experimental
  public final Path<T> transliterate(final BiFunction<? super String, ? super Element<?>, ? extends Element<?>> f) {
    if (f == null) {
      return new Path<>(this.elements, true);
    } else if (this.transliterated) {
      return this;
    } else {
      final String userPackageName = stackWalker.walk(Path::findUserPackageName);
      final int size = this.size();
      final List<Element<?>> newElements = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        newElements.add(f.apply(userPackageName, this.get(i)));
      }
      return new Path<>(newElements, true);
    }
  }

  /**
   * Returns {@code true} if this {@link Path} {@linkplain
   * #isAbsolute() is absolute} and {@linkplain #size() has a size} of
   * {@code 1}.
   *
   * @return {@code true} if this {@link Path} {@linkplain
   * #isAbsolute() is absolute} and {@linkplain #size() has a size} of
   * {@code 1}; {@code false} otherwise
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final boolean isRoot() {
    return this.isAbsolute() && this.size() == 1;
  }

  /**
   * Returns {@code true} if this {@link Path}'s {@linkplain #first()
   * first element} returns {@code true} from its {@link
   * Path.Element#isRoot() isRoot()} method, indicating that this
   * {@link Path} is rooted and therefore absolute.
   *
   * @return {@code true} if this {@link Path}'s {@linkplain #first()
   * first element} returns {@code true} from its {@link
   * Path.Element#isRoot() isRoot()} method; {@code false} otherwise
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final boolean isAbsolute() {
    return this.first().isRoot();
  }

  /**
   * Returns {@code true} if this {@link Path} {@linkplain
   * #isAbsolute() is not absolute}.
   *
   * @return {@code true} if this {@link Path} {@linkplain
   * #isAbsolute() is not absolute}; {@code false} otherwise
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see #isAbsolute()
   */
  @Convenience
  public final boolean isRelative() {
    return !this.isAbsolute();
  }

  /**
   * Returns the size of this {@link Path} (the count of its
   * {@linkplain Path.Element elements}).
   *
   * <p>This method always returns an {@code int} that is {@code 1} or
   * greater.</p>
   *
   * @return the size of this {@link Path}; always {@code 1} or
   * greater
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final int size() {
    return this.elements.size();
  }

  /**
   * Returns a {@link Path} whose {@linkplain #last() last element}
   * {@linkplain Path.Element#type() has a <code>Type</code>} equal to
   * the supplied {@code type} but is in all other ways equal to this
   * {@link Path}.
   *
   * <p>If the supplied {@code type} is already that of the existing
   * {@linkplain #last() last element} of this {@link Path}, then this
   * {@link Path} is returned unchanged.</p>
   *
   * @param <U> the type for the new {@link Path}
   *
   * @param type the new {@link Class}; must not be {@code null}
   *
   * @return a {@link Path} whose {@linkplain #last() last element}
   * {@linkplain Path.Element#type() has a <code>Type</code>} equal to
   * the supplied {@code type} but is in all other ways equal to this
   * {@link Path}; never {@code null}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final <U> Path<U> with(final Class<U> type) {
    if (type == this.type()) {
      @SuppressWarnings("unchecked")
      final Path<U> returnValue = (Path<U>)this;
      return returnValue;
    } else {
      final List<Element<?>> newElements = new ArrayList<>(this.size());
      newElements.addAll(this.elements.subList(0, this.size() - 1));
      newElements.add(this.last().with(type));
      return new Path<>(newElements, this.isTransliterated());
    }
  }

  /**
   * Returns a {@link Path} whose {@linkplain #last() last element}
   * {@linkplain Path.Element#type() has a <code>Type</code>} equal to
   * the supplied {@code type} but is in all other ways equal to this
   * {@link Path}.
   *
   * <p>If the supplied {@code type} is already that of the existing
   * {@linkplain #last() last element} of this {@link Path}, then this
   * {@link Path} is returned unchanged.</p>
   *
   * @param <U> the type of the new {@link Path}
   *
   * @param type the new {@link TypeToken}; must not be {@code null}
   *
   * @return a {@link Path} whose {@linkplain #last() last element}
   * {@linkplain Path.Element#type() has a <code>Type</code>} equal to
   * the supplied {@code type} but is in all other ways equal to this
   * {@link Path}; never {@code null}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final <U> Path<U> with(final TypeToken<U> type) {
    if (type.type() == this.type()) {
      @SuppressWarnings("unchecked")
      final Path<U> returnValue = (Path<U>)this;
      return returnValue;
    } else {
      final List<Element<?>> newElements = new ArrayList<>(this.size());
      newElements.addAll(this.elements.subList(0, this.size() - 1));
      newElements.add(this.last().with(type));
      return new Path<>(newElements, this.isTransliterated());
    }
  }

  /**
   * Returns a {@link Path} whose {@linkplain #last() last element}
   * {@linkplain Path.Element#type() has a <code>Type</code>} equal to
   * the supplied {@code type} but is in all other ways equal to this
   * {@link Path}.
   *
   * <p>If the supplied {@code type} is already that of the existing
   * {@linkplain #last() last element} of this {@link Path}, then this
   * {@link Path} is returned unchanged.</p>
   *
   * @param type the new {@link Type}; must not be {@code null}
   *
   * @return a {@link Path} whose {@linkplain #last() last element}
   * {@linkplain Path.Element#type() has a <code>Type</code>} equal to
   * the supplied {@code type} but is in all other ways equal to this
   * {@link Path}; never {@code null}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final Path<?> with(final Type type) {
    if (type == this.type()) {
      return this;
    } else {
      final List<Element<?>> newElements = new ArrayList<>(this.size());
      newElements.addAll(this.elements.subList(0, this.size() - 1));
      newElements.add(this.last().with(type));
      return new Path<>(newElements, this.isTransliterated());
    }
  }

  /**
   * Returns a new {@link Path} consisting of all the {@linkplain
   * Path.Element elements} of this {@link Path} in order plus an
   * {@linkplain Path.Element element} {@linkplain
   * Path.Element#of(String, Type) formed} from the supplied
   * arguments.
   *
   * @param name the {@linkplain Path.Element#name() name} for the new
   * trailing {@link Path.Element}; must not be {@code null}
   *
   * @param <U> the type of the new {@link Path}
   *
   * @param type the {@linkplain Path.Element#type() type} for the new
   * trailing {@link Path.Element}; must not be {@code null}
   *
   * @return the new {@link Path}; never {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see #plus(Path.Element)
   */
  @Convenience
  public final <U> Path<U> plus(final String name, final Class<U> type) {
    return this.plus(Element.of(name, type));
  }

  /**
   * Returns a new {@link Path} consisting of all the {@linkplain
   * Path.Element elements} of this {@link Path} in order plus an
   * {@linkplain Path.Element element} {@linkplain
   * Path.Element#of(String, Type) formed} from the supplied
   * arguments.
   *
   * @param name the {@linkplain Path.Element#name() name} for the new
   * trailing {@link Path.Element}; must not be {@code null}
   *
   * @param <U> the type of the new {@link Path}
   *
   * @param type the {@linkplain Path.Element#type() type} for the new
   * trailing {@link Path.Element}; must not be {@code null}
   *
   * @return the new {@link Path}; never {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see #plus(Path.Element)
   */
  @Convenience
  public final <U> Path<U> plus(final String name, final TypeToken<U> type) {
    return this.plus(Element.of(name, type));
  }

  /**
   * Returns a new {@link Path} consisting of all the {@linkplain
   * Path.Element elements} of this {@link Path} in order plus an
   * {@linkplain Path.Element element} {@linkplain
   * Path.Element#of(String, Type) formed} from the supplied
   * arguments.
   *
   * @param name the {@linkplain Path.Element#name() name} for the new
   * trailing {@link Path.Element}; must not be {@code null}
   *
   * @param type the {@linkplain Path.Element#type() type} for the new
   * trailing {@link Path.Element}; must not be {@code null}
   *
   * @return the new {@link Path}; never {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see #plus(Path.Element)
   */
  @Convenience
  public final Path<?> plus(final String name, final Type type) {
    return this.plus(Element.of(name, type));
  }

  /**
   * Returns a new {@link Path} consisting of all the {@linkplain
   * Path.Element elements} of this {@link Path} in order plus the
   * supplied {@link Path.Element}.
   *
   * @param <U> the type of the new {@link Path}
   *
   * @param element the last {@link Path.Element} of the new {@link
   * Path}; must not be {@code null}
   *
   * @return the new {@link Path}; never {@code null}
   *
   * @exception NullPointerException if {@code element} is {@code
   * null}
   *
   * @exception IllegalArgumentException if {@code element}'s
   * {@linkplain Path.Element#type() type is empty}, or if its {@link
   * Path.Element#isRoot() isRoot()} method returns {@code true}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see #plus(List)
   */
  @SuppressWarnings("unchecked")
  public final <U> Path<U> plus(final Element<U> element) {
    return (Path<U>)this.plus(List.of(element));
  }

  /**
   * Returns a new {@link Path} consisting of all the {@linkplain
   * Path.Element elements} of this {@link Path} in order plus the
   * {@linkplain Path.Element elements} of the supplied {@code path}
   * in order.
   *
   * @param <U> the type of the new {@link Path}
   *
   * @param path the {@link Path} to append to this one; must not be
   * {@code null} and must return {@code false} from its {@link
   * #isAbsolute()} method
   *
   * @return the new {@link Path}; never {@code null}
   *
   * @exception NullPointerException if {@code element} is {@code
   * null}
   *
   * @exception IllegalArgumentException if {@code path}'s {@link
   * #isAbsolute()} method returns {@code true}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see #plus(List)
   */
  @SuppressWarnings("unchecked")
  public final <U> Path<U> plus(final Path<U> path) {
    return (Path<U>)this.plus(path.elements);
  }

  /**
   * Returns a (normally) new {@link Path} consisting of all the
   * {@linkplain Path.Element elements} of this {@link Path} in order
   * plus the supplied {@linkplain Path.Element elements} in order.
   *
   * @param elements the {@linkplain Path.Element elements} to append;
   * must not be {@code null}; may be {@linkplain List#isEmpty()
   * empty} in which case this {@link Path} will be returned
   *
   * @return a {@link Path} representing this {@link Path} plus all of
   * the supplied {@linkplain Path.Element elements} in order; never
   * {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final Path<?> plus(final List<? extends Element<?>> elements) {
    if (elements.isEmpty()) {
      return this;
    } else {
      final List<Element<?>> newElements = new ArrayList<>(this.size() + elements.size());
      newElements.addAll(this.elements);
      newElements.addAll(elements);
      return new Path<>(newElements, false);
    }
  }

  /**
   * Returns the {@link Path.Element} found at the supplied zero-based
   * index, or {@code null} if no such {@linkplain Path.Element
   * element} exists.
   *
   * @param index the index of the {@link Path.Element} to return;
   * must be {@code 0} or greater and less than this {@link Path}'s
   * {@linkplain #size() size}
   *
   * @return the {@link Path.Element} found at the supplied zero-based
   * index, or {@code null} if no such {@linkplain Path.Element}
   * exists
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final Element<?> get(final int index) {
    return this.elements.get(index);
  }

  /**
   * Returns the first {@link Path.Element} in this {@link Path}.
   *
   * @return the first {@link Path.Element} in this {@link Path};
   * never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final Element<?> first() {
    return this.get(0);
  }

  /**
   * Returns the last {@link Path.Element} in this {@link Path}.
   *
   * @return the last {@link Path.Element} in this {@link Path};
   * never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  @SuppressWarnings("unchecked")
  public final Element<T> last() {
    return (Element<T>)this.get(this.size() - 1);
  }

  /**
   * Returns the {@link Type} of this {@link Path}, which is exactly
   * the {@link Path.Element#type() Type} of this {@link Path}'s
   * {@linkplain #last() last element}.
   *
   * @return the {@link Type} of this {@link Path}, which is exactly
   * the {@link Path.Element#type() Type} of this {@link Path}'s
   * {@linkplain #last() last element}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final Type type() {
    return this.last().type().orElseThrow(AssertionError::new);
  }

  /**
   * Returns the type erasure of this {@link Path}.
   *
   * @return the type erasure of this {@link Path}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #type()
   */
  @SuppressWarnings("unchecked")
  public final Class<T> typeErasure() {    
    return (Class<T>)Types.erase(this.type());
  }

  /**
   * Returns the {@link ClassLoader} {@linkplain
   * Class#getClassLoader() associated with} this {@link Path}'s
   * {@linkplain #typeErasure() type erasure}.
   *
   * @return the {@link ClassLoader} {@linkplain
   * Class#getClassLoader() associated with} this {@link Path}'s
   * {@linkplain #typeErasure() type erasure}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Convenience
  public final ClassLoader classLoader() {
    return this.typeErasure().getClassLoader();
  }

  /**
   * Returns the zero-based index identifying the position of the
   * first occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} the supplied {@link Path} within this {@link Path}, or
   * a negative value if the supplied {@link Path} does not occur
   * within this {@link Path}.
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @return the zero-based index identifying the position of the
   * first occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int indexOf(final Path<?> other) {
    return other == this ? 0 : Collections.indexOfSubList(this.elements, other.elements);
  }

  /**
   * Returns the zero-based index identifying the position of the
   * first occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}.
   *
   * <p>The supplied {@link BiPredicate} is used to test each {@link
   * Element} of the supplied {@link Path} against {@link Element}s
   * from this {@link Path}.  The first argument is an {@link Element}
   * drawn from this {@link Path}.  The second argument is an {@link
   * Element} drawn from the supplied {@link Path}.  The {@link
   * BiPredicate} returns {@code true} if its arguments are deemed to
   * match.  The supplied {@link BiPredicate}'s {@link
   * BiPredicate#test(Object, Object)} method must be idempotent and
   * deterministic.</p>
   *
   * @param path the other {@link Path}; must not be {@code null}
   *
   * @param p the {@link BiPredicate} used to {@linkplain
   * BiPredicate#test(Object, Object) test} {@link Element}s; must not
   * be {@code null}
   *
   * @return the zero-based index identifying the position of the
   * first occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}
   *
   * @exception NullPointerException if either {@code path} or {@code
   * p} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int indexOf(final Path<?> path, final BiPredicate<? super Element<?>, ? super Element<?>> p) {
    final int pathSize = path.size();
    final int sizeDiff = this.size() - pathSize;
    OUTER_LOOP:
    for (int i = 0; i <= sizeDiff; i++) {
      for (int j = 0, k = i; j < pathSize; j++, k++) {
        if (!p.test(this.elements.get(k), path.elements.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

  /**
   * Returns the zero-based index identifying the position of the
   * last occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} the supplied {@link Path} within this {@link Path}, or
   * a negative value if the supplied {@link Path} does not occur
   * within this {@link Path}.
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @return the zero-based index identifying the position of the
   * last occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int lastIndexOf(final Path<?> other) {
    return other == this ? 0 : Collections.lastIndexOfSubList(this.elements, other.elements);
  }

  /**
   * Returns the zero-based index identifying the position of the
   * last occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}.
   *
   * <p>The supplied {@link BiPredicate} is used to test each {@link
   * Element} of the supplied {@link Path} against {@link Element}s
   * from this {@link Path}.  The first argument is an {@link Element}
   * drawn from this {@link Path}.  The second argument is an {@link
   * Element} drawn from the supplied {@link Path}.  The {@link
   * BiPredicate} returns {@code true} if its arguments are deemed to
   * match.  The supplied {@link BiPredicate}'s {@link
   * BiPredicate#test(Object, Object)} method must be idempotent and
   * deterministic.</p>
   *
   * @param path the other {@link Path}; must not be {@code null}
   *
   * @param p the {@link BiPredicate} used to {@linkplain
   * BiPredicate#test(Object, Object) test} {@link Element}s; must not
   * be {@code null}
   *
   * @return the zero-based index identifying the position of the
   * last occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}
   *
   * @exception NullPointerException if either {@code path} or {@code
   * p} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int lastIndexOf(final Path<?> path, final BiPredicate<? super Element<?>, ? super Element<?>> p) {
    final int pathSize = path.size();
    final int sizeDiff = this.size() - pathSize;
    OUTER_LOOP:
    for (int i = sizeDiff; i >= 0; i--) {
      for (int j = 0, k = i; j < pathSize; j++, k++) {
        if (!p.test(this.elements.get(k), path.elements.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

  /**
   * Returns {@code true} if this {@link Path} starts with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}.
   *
   * <p>This method returns {@code true} if and only if:</p>
   *
   * <ul>
   *
   * <li>{@code other} is identical to this {@link Path}, or</li>
   *
   * <li>An invocation of the {@link #indexOf(Path)} method with
   * {@code other} as its sole argument returns {@code 0}</li>
   *
   * </ul>
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @return {@code true} if this {@link Path} starts with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}; {@code false} otherwise
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #indexOf(Path)
   */
  public final boolean startsWith(final Path<?> other) {
    return other == this || this.indexOf(other) == 0;
  }

  /**
   * Returns {@code true} if this {@link Path} starts with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}.
   *
   * <p>The supplied {@link BiPredicate} is used to test each {@link
   * Element} of the supplied {@link Path} against {@link Element}s
   * from this {@link Path}.  The first argument is an {@link Element}
   * drawn from this {@link Path}.  The second argument is an {@link
   * Element} drawn from the supplied {@link Path}.  The {@link
   * BiPredicate} returns {@code true} if its arguments are deemed to
   * match.  The supplied {@link BiPredicate}'s {@link
   * BiPredicate#test(Object, Object)} method must be idempotent and
   * deterministic.</p>
   *
   * <p>This method returns {@code true} if and only if:</p>
   *
   * <ul>
   *
   * <li>{@code other} is identical to this {@link Path}, or</li>
   *
   * <li>An invocation of the {@link #indexOf(Path, BiPredicate)}
   * method with {@code other} and {@code p} as its arguments returns
   * {@code 0}</li>
   *
   * </ul>
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @param p the {@link BiPredicate} used to {@linkplain
   * BiPredicate#test(Object, Object) test} {@link Element}s; must not
   * be {@code null}
   *
   * @return {@code true} if this {@link Path} starts with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}; {@code false} otherwise
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #indexOf(Path, BiPredicate)
   */
  public final boolean startsWith(final Path<?> other, final BiPredicate<? super Element<?>, ? super Element<?>> p) {
    return other == this || this.indexOf(other, p) == 0;
  }

  /**
   * Returns {@code true} if this {@link Path} ends with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}.
   *
   * <p>This method returns {@code true} if and only if:</p>
   *
   * <ul>
   *
   * <li>{@code other} is identical to this {@link Path}, or</li>
   *
   * <li>An invocation of the {@link #lastIndexOf(Path)} method with
   * {@code other} as its sole argument returns {@code 0} or a
   * positive {@code int}, and that number plus the supplied {@link
   * Path}'s {@linkplain #size() size} is equal to this {@link Path}'s
   * {@linkplain #size() size}</li>
   *
   * </ul>
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @return {@code true} if this {@link Path} ends with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}; {@code false} otherwise
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #lastIndexOf(Path)
   */
  public final boolean endsWith(final Path<?> other) {
    if (other == this) {
      return true;
    } else {
      final int lastIndex = this.lastIndexOf(other);
      return lastIndex >= 0 && lastIndex + other.size() == this.size();
    }
  }

  /**
   * Returns {@code true} if this {@link Path} ends with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}.
   *
   * <p>The supplied {@link BiPredicate} is used to test each {@link
   * Element} of the supplied {@link Path} against {@link Element}s
   * from this {@link Path}.  The first argument is an {@link Element}
   * drawn from this {@link Path}.  The second argument is an {@link
   * Element} drawn from the supplied {@link Path}.  The {@link
   * BiPredicate} returns {@code true} if its arguments are deemed to
   * match.  The supplied {@link BiPredicate}'s {@link
   * BiPredicate#test(Object, Object)} method must be idempotent and
   * deterministic.</p>
   *
   * <p>This method returns {@code true} if and only if:</p>
   *
   * <ul>
   *
   * <li>{@code other} is identical to this {@link Path}, or</li>
   *
   * <li>An invocation of the {@link #indexOf(Path, BiPredicate)}
   * method with {@code other} and {@code p} as its arguments returns
   * {@code 0} or a positive {@code int}, and if that number plus the
   * supplied {@link Path}'s {@linkplain #size() size} is equal to
   * this {@link Path}'s {@linkplain #size() size}</li>
   *
   * </ul>
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @param p the {@link BiPredicate} used to {@linkplain
   * BiPredicate#test(Object, Object) test} {@link Element}s; must not
   * be {@code null}
   *
   * @return {@code true} if this {@link Path} ends with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}; {@code false} otherwise
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #lastIndexOf(Path, BiPredicate)
   */
  public final boolean endsWith(final Path<?> other, final BiPredicate<? super Element<?>, ? super Element<?>> p) {
    if (other == this) {
      return true;
    } else {
      final int lastIndex = this.lastIndexOf(other, p);
      return lastIndex >= 0 && lastIndex + other.size() == this.size();
    }
  }

  /**
   * Returns a hashcode for this {@link Path} based solely off of its
   * {@link Element}s.
   *
   * @return a hashcode for this {@link Path}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Object
  public final int hashCode() {
    return this.elements.hashCode();
  }

  /**
   * Returns {@code true} if this {@link Path} is equal to the
   * supplied {@link Object}.
   *
   * @param other the {@link Object} to test; may be {@code null} in
   * which case {@code false} will be returned
   *
   * @return {@code true} if this {@link Path} is equal to the
   * supplied {@link Object}; {@code false} otherwise
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      return this.elements.equals(((Path)other).elements);
    } else {
      return false;
    }
  }

  /**
   * Returns a non-{@code null} {@link String} representation of this
   * {@link Path}.
   *
   * @return a non-{@code null} {@link String} representation of this
   * {@link Path}
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
    final StringJoiner sj = new StringJoiner("/");
    for (final Object element : this.elements) {
      sj.add(element.toString());
    }
    return sj.toString();
  }


  /*
   * Static methods.
   */


  /**
   * Returns the root {@link Path} (the {@link Path} that {@linkplain
   * #isRelative() is relative to} no other {@link Path}).
   *
   * @return the root {@link Path} (the {@link Path} that {@linkplain
   * #isRelative() is relative to} no other {@link Path}); never
   * {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public static final Path<?> root() {
    return ROOT;
  }

  /**
   * Invokes the {@link Element#of(String, Class, List, List)} method
   * with the supplied {@code type}, passes the result to the {@link
   * #of(Element)} method, and returns the result.
   *
   * @param <U> the {@linkplain Element#type() type} of the returned
   * {@link Path}'s last {@link Element}, and hence the {@linkplain
   * #type() type} of the {@link Path} itself
   *
   * @param type the {@linkplain Element#type() type} of the sole
   * {@link Element}; must not be {@code null}
   *
   * @return a {@link Path} with a single {@link Element} {@linkplain
   * Element#type() bearing} the supplied {@code type}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(Element)
   *
   * @see Element#of(String, Class, List, List)
   */
  public static final <U> Path<U> of(final Class<U> type) {
    return of(Element.of("", type, (List<? extends Class<?>>)null, (List<? extends String>)null));
  }

  /**
   * Invokes the {@link Element#of(String, TypeToken, List, List)}
   * method with the supplied {@code type}, passes the result to the
   * {@link #of(Element)} method, and returns the result.
   *
   * @param <U> the {@linkplain Element#type() type} of the returned
   * {@link Path}'s last {@link Element}, and hence the {@linkplain
   * #type() type} of the {@link Path} itself
   *
   * @param type the {@linkplain Element#type() type} of the sole
   * {@link Element}; must not be {@code null}
   *
   * @return a {@link Path} with a single {@link Element} {@linkplain
   * Element#type() bearing} the supplied {@code type}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(Element)
   *
   * @see Element#of(String, TypeToken, List, List)
   */
  public static final <U> Path<U> of(final TypeToken<U> type) {
    return of(Element.of("", type, (List<? extends Class<?>>)null, (List<? extends String>)null));
  }

  /**
   * Invokes the {@link Element#of(String, Type, List, List)} method
   * with the supplied {@code type}, passes the result to the {@link
   * #of(Element)} method, and returns the result.
   *
   * @param type the {@linkplain Element#type() type} of the sole
   * {@link Element}; must not be {@code null}
   *
   * @return a {@link Path} with a single {@link Element} {@linkplain
   * Element#type() bearing} the supplied {@code type}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(Element)
   *
   * @see Element#of(String, Type, List, List)
   */
  public static final Path<?> of(final Type type) {
    return of(Element.of("", type, (List<? extends Class<?>>)null, (List<? extends String>)null));
  }

  /**
   * Supplies a {@link Path} equal to a {@link Path} consisting of a
   * single element that is equal to the supplied {@link Element}.
   *
   * <p>In most cases, this method creates a new {@link Path} with the
   * supplied {@link Element} as its only {@link Element}.</p>
   *
   * <p>If the supplied {@link Element} {@linkplain Element#isRoot()
   * is the root <code>Element</code>}, then the return value of the
   * {@link #root()} method is returned instead.</p>
   *
   * @param <U> the {@linkplain Element#type() type} of the supplied
   * {@link Element}, and hence the {@linkplain #type() type} of the
   * {@link Path} itself
   *
   * @param element the sole {@link Element} of the returned {@link
   * Path}; must not be {@code null}
   *
   * @return a {@link Path} equal to a {@link Path} consisting of a
   * single element that is equal to the supplied {@link Element};
   * never {@code null}
   *
   * @exception NullPointerException if {@code element} is {@code null}
   *
   * @exception IllegalArgumentException if the supplied {@link
   * Element} returns an {@linkplain Optional#isEmpty() empty} {@link
   * Optional} from an invocation of its {@link Element#type()} method
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @EntryPoint
  @SuppressWarnings("unchecked")
  public static final <U> Path<U> of(final Element<U> element) {
    return element.isRoot() ? (Path<U>)root() : new Path<>(element);
  }

  /**
   * Supplies a {@link Path} {@linkplain #equals(Object) equal to} a
   * {@link Path} comprising the supplied {@link List} of {@link
   * Element}s.
   *
   * <p>In most cases, this method creates a new {@link Path} with the
   * supplied {@link List} of {@link Element}s as its {@link
   * Element}s.</p>
   *
   * <p>If {@code elements} contains only one {@link Element} and that
   * {@link Element} {@linkplain Element#isRoot() is the root
   * <code>Element</code>}, then the return value of the {@link
   * #root()} method is returned instead.</p>
   *
   * @param elements a {@link List} of {@link Element}s that will be
   * equal to the returned {@link Path}'s {@linkplain #iterator()
   * elements}; must not be {@code null}; must not be {@linkplain
   * List#isEmpty() empty}
   *
   * @return a {@link Path} equal to a {@link Path} comprising {@link
   * #iterator() Element}s equal to the supplied {@code elements};
   * never {@code null}
   *
   * @exception NullPointerException if {@code elements} is {@code null}
   *
   * @exception IllegalArgumentException if {@code elements} is
   * {@linkplain List#isEmpty() empty} or if the last {@link Element}
   * in the list returns an {@linkplain Optional#isEmpty() empty}
   * {@link Optional} from an invocation of its {@link Element#type()}
   * method
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public static final Path<?> of(final List<? extends Element<?>> elements) {
    if (elements.isEmpty()) {
      throw new IllegalArgumentException("elements.isEmpty()");
    } else if (elements.size() == 1 && elements.get(0).isRoot()) {
      return root();
    } else {
      return new Path<>(elements, false);
    }
  }

  /**
   * Creates a {@link List} of {@link Element}s by invoking the {@link
   * Element#of(String)} and {@link Element#of(TypeToken)} methods,
   * supplies that {@link List} to the {@link #of(List)} method, and
   * returns the resulting {@link Path}.
   *
   * @param <U> the {@linkplain Element#type() type} of the returned
   * {@link Path}'s last {@link Element}, and hence the {@linkplain
   * #type() type} of the {@link Path} itself
   *
   * @param names a list of {@link String}s that will become
   * {@linkplain Element#name() element names}; must not be {@code
   * null}
   *
   * @param type the {@linkplain Element#type() type} of the returned
   * {@link Path}'s last {@link Element}, and hence the {@linkplain
   * #type() type} of the {@link Path} itself
   *
   * @return a suitable {@link Path}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(List)
   *
   * @see Element#of(String)
   *
   * @see Element#of(TypeToken)
   */
  @SuppressWarnings("unchecked")
  public static final <U> Path<U> of(final List<? extends String> names, final TypeToken<U> type) {
    final List<Element<?>> elements = new ArrayList<>(names.size() + 1);
    for (final String name : names) {
      elements.add(Element.of(name));
    }
    elements.add(Element.of(type));
    return (Path<U>)Path.of(elements);
  }

  /**
   * Creates a {@link List} of {@link Element}s by invoking the {@link
   * Element#of(String)} and {@link Element#of(Class)} methods,
   * supplies that {@link List} to the {@link #of(List)} method, and
   * returns the resulting {@link Path}.
   *
   * @param <U> the {@linkplain Element#type() type} of the returned
   * {@link Path}'s last {@link Element}, and hence the {@linkplain
   * #type() type} of the {@link Path} itself
   *
   * @param names a list of {@link String}s that will become
   * {@linkplain Element#name() element names}; must not be {@code
   * null}
   *
   * @param type the {@linkplain Element#type() type} of the returned
   * {@link Path}'s last {@link Element}, and hence the {@linkplain
   * #type() type} of the {@link Path} itself
   *
   * @return a suitable {@link Path}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(List)
   *
   * @see Element#of(String)
   *
   * @see Element#of(Class)
   */
  @SuppressWarnings("unchecked")
  public static final <U> Path<U> of(final List<? extends String> names, final Class<U> type) {
    final List<Element<?>> elements = new ArrayList<>(names.size() + 1);
    for (final String name : names) {
      elements.add(Element.of(name));
    }
    elements.add(Element.of(type));
    return (Path<U>)Path.of(elements);
  }

  /**
   * Creates a {@link List} of {@link Element}s by invoking the {@link
   * Element#of(String)} and {@link Element#of(Type)} methods,
   * supplies that {@link List} to the {@link #of(List)} method, and
   * returns the resulting {@link Path}.
   *
   * @param names a list of {@link String}s that will become
   * {@linkplain Element#name() element names}; must not be {@code
   * null}
   *
   * @param type the {@linkplain Element#type() type} of the returned
   * {@link Path}'s last {@link Element}, and hence the {@linkplain
   * #type() type} of the {@link Path} itself
   *
   * @return a suitable {@link Path}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(List)
   *
   * @see Element#of(String)
   *
   * @see Element#of(Type)
   */
  public static final Path<?> of(final List<? extends String> names, final Type type) {
    final List<Element<?>> elements = new ArrayList<>(names.size() + 1);
    for (final String name : names) {
      elements.add(Element.of(name));
    }
    elements.add(Element.of(type));
    return Path.of(elements);
  }

  private static final String findUserPackageName(final Stream<StackFrame> stream) {
    final String className = stream.sequential()
      .dropWhile(f -> f.getClassName().startsWith(Path.class.getPackageName()))
      .dropWhile(f -> f.getClassName().contains(".$Proxy")) // skip JDK proxies (and any other kind of proxies)
      .map(StackFrame::getClassName)
      .findFirst()
      .orElse(null);
    if (className == null) {
      return "";
    } else {
      final int lastIndex = className.lastIndexOf('.');
      if (lastIndex < 0) {
        return "";
      } else if (lastIndex == 0) {
        throw new AssertionError("className: " + className);
      } else {
        return className.substring(0, lastIndex);
      }
    }
  }


  /*
   * Inner and nested classes.
   */


  /**
   * A node in a {@link Path}.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see name()
   *
   * @see type()
   */
  public static final class Element<T> {


    /*
     * Static fields.
     */


    private static final Element<Void> ROOT = new Element<>("", void.class, null, null, true);


    /*
     * Instance fields.
     */


    private final String name;

    private final Optional<Type> type;

    private final Optional<List<Class<?>>> parameters;

    private final Optional<List<String>> arguments;


    /*
     * Constructors.
     */


    private Element(final String name,
                    final Type type,
                    final List<? extends Class<?>> parameters,
                    final List<? extends String> arguments,
                    final boolean root) {
      super();
      if (type == null) {
        if (name == null || name.isEmpty()) {
          throw new IllegalArgumentException("An empty name may not be paired with a null type");
        }
        this.name = name;
        this.type = Optional.empty();
      } else if (!root && type == void.class) {
        throw new IllegalArgumentException("type must not be void");
      } else {
        this.type = Optional.of(type);
        this.name = name == null ? "" : name;
      }
      if (parameters == null) {
        this.parameters = Optional.empty();
        if (arguments == null) {
          this.arguments = Optional.empty();
        } else {
          throw new IllegalArgumentException("arguments: " + arguments + "; parameters: null");
        }
      } else if (arguments == null) {
        this.parameters = Optional.of(List.copyOf(parameters));
        this.arguments = Optional.empty();
      } else if (parameters.size() == arguments.size()) {
        this.parameters = Optional.of(List.copyOf(parameters));
        this.arguments = Optional.of(List.copyOf(arguments));
      } else {
        throw new IllegalArgumentException("parameters: " + parameters + "; arguments: " + arguments);
      }
    }


    /*
     * Instance methods.
     */


    /**
     * Returns the non-{@code null} name of this {@link Element}.
     *
     * <p><strong>Note:</strong> if the resulting {@link String}
     * {@linkplain String#isEmpty() is empty}, then during any matching
     * operations the name will be considered to match all possible
     * names.</p>
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @return the non-{@code null} name of this {@link Element},
     * which may {@linkplain String#isEmpty() be empty} indicating the
     * special semantics described above
     *
     * @nullability This method never returns {@code null}.
     *
     * @threadsafety This method is safe for concurrent use by multiple
     * threads.
     *
     * @idempotency This method is idempotent and deterministic.
     */
    public final String name() {
      return this.name;
    }

    /**
     * Returns a non-{@code null} but possibly {@linkplain
     * Optional#isEmpty() empty} {@link Optional} {@linkplain
     * Optional#get() containing} the {@link Type} of this {@link
     * Element}.
     *
     * @return a non-{@code null} but possibly {@linkplain
     * Optional#isEmpty() empty} {@link Optional} {@linkplain
     * Optional#get() containing} the {@link Type} of this {@link
     * Element}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public final Optional<Type> type() {
      return this.type;
    }

    /**
     * Returns a non-{@code null} but but possibly {@linkplain
     * Optional#isEmpty() empty} {@link Optional} {@linkplain
     * Optional#get() containing} the type erasure of this {@link
     * Element}'s {@linkplain #type() type}.
     *
     * @return a non-{@code null} but but possibly {@linkplain
     * Optional#isEmpty() empty} {@link Optional} {@linkplain
     * Optional#get() containing} the type erasure of this {@link
     * Element}'s {@linkplain #type() type}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     *
     * @see #type()
     */
    public final Optional<Class<T>> typeErasure() {
      @SuppressWarnings("unchecked")
      final Class<T> c = (Class<T>)Types.erase(this.type().orElse(null));
      return Optional.ofNullable(c);
    }

    /**
     * Returns an {@link Element} equal to this {@link Element} in all
     * respects but with the supplied {@code type} as its {@linkplain
     * #type() type}.
     *
     * @param <U> the type of the returned {@link Element}
     *
     * @param type the type for the returned {@link Element}; may be
     * {@code null}
     *
     * @return an {@link Element} equal to this {@link Element} in all
     * respects but with the supplied {@code type} as its {@linkplain
     * #type() type}; never {@code null}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     *
     * @see #type()
     */
    @SuppressWarnings("unchecked")
    public final <U> Element<U> with(final Class<U> type) {
      if (type == this.type().orElse(null)) {
        @SuppressWarnings("unchecked")
        final Element<U> returnValue = (Element<U>)this;
        return returnValue;
      } else if (type == void.class && this.name().isEmpty()) {
        return (Element<U>)root();
      } else {
        return new Element<>(this.name(), type, this.parameters().orElse(null), this.arguments().orElse(null), false);
      }
    }

    /**
     * Returns an {@link Element} equal to this {@link Element} in all
     * respects but with the supplied {@code type} as its {@linkplain
     * #type() type}.
     *
     * @param <U> the type of the returned {@link Element}
     *
     * @param type the type for the returned {@link Element}; may be
     * {@code null}
     *
     * @return an {@link Element} equal to this {@link Element} in all
     * respects but with the supplied {@code type} as its {@linkplain
     * #type() type}; never {@code null}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     *
     * @see #type()
     */
    @SuppressWarnings("unchecked")
    public final <U> Element<U> with(final TypeToken<U> type) {
      final Type t = type.type();
      if (t == this.type().orElse(null)) {
        @SuppressWarnings("unchecked")
        final Element<U> returnValue = (Element<U>)this;
        return returnValue;
      } else if (t == void.class && this.name().isEmpty()) {
        return (Element<U>)root();
      } else {
        return new Element<>(this.name(), t, this.parameters().orElse(null), this.arguments().orElse(null), false);
      }
    }

    /**
     * Returns an {@link Element} equal to this {@link Element} in all
     * respects but with the supplied {@code type} as its {@linkplain
     * #type() type}.
     *
     * @param type the type for the returned {@link Element}; may be
     * {@code null}
     *
     * @return an {@link Element} equal to this {@link Element} in all
     * respects but with the supplied {@code type} as its {@linkplain
     * #type() type}; never {@code null}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     *
     * @see #type()
     */
    public final Element<?> with(final Type type) {
      if (type == this.type().orElse(null)) {
        return this;
      } else if (type == void.class && this.name().isEmpty()) {
        return root();
      } else {
        return new Element<>(this.name(), type, this.parameters().orElse(null), this.arguments().orElse(null), false);
      }
    }

    /**
     * Returns an {@link Optional} {@linkplain Optional#get()
     * yielding} an immutable {@link List} of the parameters
     * associated with this {@link Element}.
     *
     * @return an {@link Optional} {@linkplain Optional#get()
     * yielding} an immutable {@link List} of the parameters
     * associated with this {@link Element}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public final Optional<List<Class<?>>> parameters() {
      return this.parameters;
    }

    /**
     * Returns a non-{@code null} but possibly {@linkplain
     * Optional#isEmpty() empty} {@link Optional} {@linkplain
     * Optional#get() containing} a {@link List} of {@link String}
     * representations of argument values supplied for their
     * corresponding {@linkplain #parameters() parameters}.
     *
     * <p>The {@link List}, if any, contained by the returned {@link
     * Optional} is <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/List.html#unmodifiable">unmodifiable</a>.</p>
     *
     * <p>The {@link List}, if any, contained by the returned {@link
     * Optional} will have a {@linkplain List#size() size} less than
     * or equal to that of the {@link List} contained by the {@link
     * Optional} returned by the {@link #parameters()} method.</p>
     *
     * <p><strong>Design note:</strong> Arguments are {@link
     * String}-typed rather than typed with their corresponding
     * parameter types because {@link Path.Element} instances must be
     * wholly immutable.  In practice, since arguments and parameters
     * in a {@link Path.Element} are used for informational purposes
     * during environmental object selection, typing all arguments as
     * {@link String}s is adequate.</p>
     *
     * @return a non-{@code null} but possibly {@linkplain
     * Optional#isEmpty() empty} {@link Optional} containing a {@link
     * List} of {@link String} representations of argument values
     * supplied for their corresponding {@linkplain #parameters()
     * parameters}
     *
     * @nullability This method never returns {@code null}.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     *
     * @idempotency This method is idempotent and deterministic.
     */
    public final Optional<List<String>> arguments() {
      return this.arguments;
    }

    /**
     * Returns {@code true} if and only if this {@link Element}'s
     * {@linkplain type() type} is identical to {@code void.class} and
     * this {@link Element}'s {@linkplain #name() name} {@linkplain
     * String#isEmpty() is empty}.
     *
     * @return {@code true} if and only if this {@link Element}'s
     * {@linkplain type() type} is identical to {@code void.class} and
     * this {@link Element}'s {@linkplain #name() name} {@linkplain
     * String#isEmpty() is empty}
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @see #type()
     *
     * @see #name()
     */
    public final boolean isRoot() {
      return this.type().orElse(null) == void.class && this.name().isEmpty();
    }

    /**
     * Computes and returns a hashcode for this {@link Element} based
     * on its {@linkplain #name() name}, {@linkplain #type() type},
     * {@linkplain #parameters() parameters} and {@linkplain
     * #arguments() arguments}.
     *
     * @return a hashcode for this {@link Element}
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @see #equals(Object)
     */
    @Override // Object
    public final int hashCode() {
      return Objects.hash(this.name(), this.type(), this.parameters(), this.arguments());
    }

    /**
     * Returns {@code true} if this {@link Element} is equal to the
     * supplied {@link Object}.
     *
     * <p>This {@link Element} is equal to the supplied {@link Object}
     * if and only if all of the following conditions hold:</p>
     *
     * <ul>
     *
     * <li>The supplied {@link Object} is non-{@code null} and returns
     * {@link Element Element.class} from its {@link
     * Object#getClass()} method</li>
     *
     * <li>{@link Objects#equals(Object, Object)} returns {@code true}
     * when supplied with this {@link Element}'s {@linkplain #name()
     * name} and the other {@link Element}'s {@linkplain #name()
     * name}</li>
     *
     * <li>{@link Objects#equals(Object, Object)} returns {@code true}
     * when supplied with this {@link Element}'s {@linkplain #type()
     * type} and the other {@link Element}'s {@linkplain #type()
     * type}</li>
     *
     * <li>{@link Objects#equals(Object, Object)} returns {@code true}
     * when supplied with this {@link Element}'s {@linkplain
     * #parameters() parameters} and the other {@link Element}'s
     * {@linkplain #parameters() parameters}</li>
     *
     * <li>{@link Objects#equals(Object, Object)} returns {@code true}
     * when supplied with this {@link Element}'s {@linkplain
     * #arguments() arguments} and the other {@link Element}'s
     * {@linkplain #arguments() arguments}</li>
     *
     * </ul>
     *
     * @param other the {@link Object} to test; may be {@code null} in
     * which case {@code false} will be returned
     *
     * @return {@code true} if this {@link Element} is equal to the
     * supplied {@link Object}; {@code false} otherwise
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     *
     * @see #hashCode()
     *
     * @see #name()
     *
     * @see #type()
     *
     * @see #parameters()
     *
     * @see #arguments()
     *
     * @see Objects#equals(Object, Object)
     */
    @Override // Object
    public final boolean equals(final Object other) {
      if (other == this) {
        return true;
      } else if (other != null && this.getClass() == other.getClass()) {
        final Element<?> her = (Element<?>)other;
        return
          Objects.equals(this.name(), her.name()) &&
          Objects.equals(this.type(), her.type()) &&
          Objects.equals(this.parameters(), her.parameters()) &&
          Objects.equals(this.arguments(), her.arguments());
      } else {
        return false;
      }
    }

    /**
     * Returns a non-{@code null} {@link String} representation of
     * this {@link Element}.
     *
     * <p>The format of the returned {@link String} is deliberately
     * not specified.</p>
     *
     * return a non-{@code null} {@link String} representation of this
     * {@link Element}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @Override // Object
    public final String toString() {
      final StringBuilder sb = new StringBuilder(this.name());
      final Optional<List<Class<?>>> parameters = this.parameters();
      if (parameters.isPresent()) {
        sb.append("(");
        final List<Class<?>> ps = parameters.orElseThrow();
        final List<?> as = this.arguments().orElseGet(List::of);
        for (int i = 0; i < ps.size(); i++) {
          sb.append(ps.get(i).getName()).append("=\"").append(as.get(i).toString()).append("\"");
          if (i + 1 < ps.size()) {
            sb.append(",");
          }
        }
        sb.append(")");
      }
      final Optional<Type> type = this.type();
      if (type.isPresent()) {
        sb.append(":").append(type.orElseThrow().getTypeName());
      }
      return sb.toString();
    }


    /*
     * Static methods.
     */


    /**
     * Returns an {@link Element} whose {@link #isRoot()} method is
     * guaranteed to return {@code true}.
     *
     * @return an {@link Element} whose {@link #isRoot()} method is
     * guaranteed to return {@code true}; never {@code null}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     *
     * @see #isRoot()
     */
    public static final Element<?> root() {
      return ROOT;
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} a copy of the supplied {@code parameters}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} a copy of the supplied {@code arguments}.
     *
     * @param <U> the type of the Element
     *
     * @param name the name for the {@link Element}; may be {@code
     * null}
     *
     * @param type the type for the {@link Element}; may be {@code
     * null}
     *
     * @param parameters the parameters for the {@link Element}; may
     * be {@code null}
     *
     * @param arguments the arguments for the {@link Element}; may be
     * {@code null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} a copy of the supplied {@code parameters}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} a copy of the supplied {@code arguments}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @SuppressWarnings("unchecked")
    public static final <U> Element<U> of(final String name,
                                          final Class<U> type,
                                          final List<? extends Class<?>> parameters,
                                          final List<? extends String> arguments) {
      if ((name == null || name.isEmpty()) && type == void.class && parameters == null && arguments == null) {
        return (Element<U>)root();
      }
      return new Element<>(name, type, parameters, arguments, false);
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} a copy of the supplied {@code parameters}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} a copy of the supplied {@code arguments}.
     *
     * @param <U> the type of the Element
     *
     * @param name the name for the {@link Element}; may be {@code
     * null}
     *
     * @param type the type for the {@link Element}; may be {@code
     * null}
     *
     * @param parameters the parameters for the {@link Element}; may
     * be {@code null}
     *
     * @param arguments the arguments for the {@link Element}; may be
     * {@code null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} a copy of the supplied {@code parameters}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} a copy of the supplied {@code arguments}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @SuppressWarnings("unchecked")
    public static final <U> Element<U> of(final String name,
                                          final TypeToken<U> type,
                                          final List<? extends Class<?>> parameters,
                                          final List<? extends String> arguments) {
      final Type t = type == null ? null : type.type();
      if ((name == null || name.isEmpty()) && t == void.class && parameters == null && arguments == null) {
        return (Element<U>)root();
      }
      return new Element<>(name, t, parameters, arguments, false);
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} a copy of the supplied {@code parameters}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} a copy of the supplied {@code arguments}.
     *
     * @param name the name for the {@link Element}; may be {@code
     * null}
     *
     * @param type the type for the {@link Element}; may be {@code
     * null}
     *
     * @param parameters the parameters for the {@link Element}; may
     * be {@code null}
     *
     * @param arguments the arguments for the {@link Element}; may be
     * {@code null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} a copy of the supplied {@code parameters}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} a copy of the supplied {@code arguments}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final Element<?> of(final String name,
                                      final Type type,
                                      final List<? extends Class<?>> parameters,
                                      final List<? extends String> arguments) {
      if ((name == null || name.isEmpty()) && type == void.class && parameters == null && arguments == null) {
        return root();
      }
      return new Element<>(name, type, parameters, arguments, false);
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} the supplied {@code parameter}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} the supplied {@code argument}.
     *
     * @param <U> the type of the {@link Element}
     * 
     * @param name the name for the {@link Element}; may be {@code
     * null}
     *
     * @param type the type for the {@link Element}; may be {@code
     * null}
     *
     * @param parameter the sole parameter for the {@link Element}; may
     * be {@code null}
     *
     * @param argument the sole argument for the {@link Element}; may
     * be {@code null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} the supplied {@code parameter}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} the supplied {@code argument}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final <U> Element<U> of(final String name,
                                          final Class<U> type,
                                          final Class<?> parameter,
                                          final String argument) {
      return of(name, type, parameter == null ? null : List.of(parameter), argument == null ? null : List.of(argument));
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} the supplied {@code parameter}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} the supplied {@code argument}.
     *
     * @param <U> the type of the {@link Element}
     *
     * @param name the name for the {@link Element}; may be {@code
     * null}
     *
     * @param type the type for the {@link Element}; may be {@code
     * null}
     *
     * @param parameter the sole parameter for the {@link Element}; may
     * be {@code null}
     *
     * @param argument the sole argument for the {@link Element}; may
     * be {@code null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} the supplied {@code parameter}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} the supplied {@code argument}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final <U> Element<U> of(final String name,
                                          final TypeToken<U> type,
                                          final Class<?> parameter,
                                          final String argument) {
      return of(name, type, parameter == null ? null : List.of(parameter), argument == null ? null : List.of(argument));
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} the supplied {@code parameter}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} the supplied {@code argument}.
     *
     * @param name the name for the {@link Element}; may be {@code
     * null}
     *
     * @param type the type for the {@link Element}; may be {@code
     * null}
     *
     * @param parameter the sole parameter for the {@link Element}; may
     * be {@code null}
     *
     * @param argument the sole argument for the {@link Element}; may
     * be {@code null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, whose {@linkplain
     * #parameters() parameters} will {@linkplain Optional#get()
     * yield} the supplied {@code parameter}, and whose
     * {@linkplain #arguments() arguments} will {@linkplain
     * Optional#get() yield} the supplied {@code argument}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final Element<?> of(final String name,
                                      final Type type,
                                      final Class<?> parameter,
                                      final String argument) {
      return of(name, type, parameter == null ? null : List.of(parameter), argument == null ? null : List.of(argument));
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type}, {@linkplain #parameters()
     * parameters} and {@linkplain #arguments() arguments} are
     * {@linkplain Optional#isEmpty() empty}.
     *
     * @param name the name of the {@link Element}; may be {@code null}
     *
     * @return an {@link Element} whose whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type}, {@linkplain #parameters()
     * parameters} and {@linkplain #arguments() arguments} are
     * {@linkplain Optional#isEmpty() empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final Element<?> of(final String name) {
      return new Element<>(name, null, null, null, false);
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, and whose {@linkplain
     * #parameters() parameters} and {@linkplain #arguments()
     * arguments} are {@linkplain Optional#isEmpty() empty}.
     *
     * @param <U> the type of the {@link Element}
     *
     * @param name the name of the {@link Element}; may be {@code
     * null}
     *
     * @param type the type of the {@link Element}; may be {@code
     * null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, and whose {@linkplain
     * #parameters() parameters} and {@linkplain #arguments()
     * arguments} are {@linkplain Optional#isEmpty() empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @SuppressWarnings("unchecked")
    public static final <U> Element<U> of(final String name, final Class<U> type) {
      if (name.isEmpty() && type == void.class) {
        return (Element<U>)root();
      }
      return new Element<>(name, type, null, null, false);
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, and whose {@linkplain
     * #parameters() parameters} and {@linkplain #arguments()
     * arguments} are {@linkplain Optional#isEmpty() empty}.
     *
     * @param <U> the type of the {@link Element}
     *
     * @param name the name of the {@link Element}; may be {@code
     * null}
     *
     * @param type the type of the {@link Element}; may be {@code
     * null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, and whose {@linkplain
     * #parameters() parameters} and {@linkplain #arguments()
     * arguments} are {@linkplain Optional#isEmpty() empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @SuppressWarnings("unchecked")
    public static final <U> Element<U> of(final String name, final TypeToken<U> type) {
      final Type t = type.type();
      if (name.isEmpty() && t == void.class) {
        return (Element<U>)root();
      }
      return new Element<>(name, t, null, null, false);
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, and whose {@linkplain
     * #parameters() parameters} and {@linkplain #arguments()
     * arguments} are {@linkplain Optional#isEmpty() empty}.
     *
     * @param name the name of the {@link Element}; may be {@code
     * null}
     *
     * @param type the type of the {@link Element}; may be {@code
     * null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} will
     * {@linkplain Optional#get() yield} the supplied {@code name},
     * whose {@linkplain #type() type} will {@linkplain Optional#get()
     * yield} the supplied {@code type}, and whose {@linkplain
     * #parameters() parameters} and {@linkplain #arguments()
     * arguments} are {@linkplain Optional#isEmpty() empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final Element<?> of(final String name, final Type type) {
      if (name.isEmpty() && type == void.class) {
        return root();
      }
      return new Element<>(name, type, null, null, false);
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} is
     * {@linkplain String#isEmpty() empty}, whose {@linkplain #type()
     * type} will {@linkplain Optional#get() yield} the supplied
     * {@code type}, and whose {@linkplain #parameters() parameters}
     * and {@linkplain #arguments() arguments} are {@linkplain
     * Optional#isEmpty() empty}.
     *
     * @param <U> the type of the {@link Element}
     *
     * @param type the type of of the {@link Element}; may be {@code
     * null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} is
     * {@linkplain String#isEmpty() empty}, whose {@linkplain #type()
     * type} will {@linkplain Optional#get() yield} the supplied
     * {@code type}, and whose {@linkplain #parameters() parameters}
     * and {@linkplain #arguments() arguments} are {@linkplain
     * Optional#isEmpty() empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @SuppressWarnings("unchecked")
    public static final <U> Element<U> of(final Class<U> type) {
      if (type == void.class) {
        return (Element<U>)root();
      }
      return new Element<>("", type, null, null, false);
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} is
     * {@linkplain String#isEmpty() empty}, whose {@linkplain #type()
     * type} will {@linkplain Optional#get() yield} the supplied
     * {@code type}, and whose {@linkplain #parameters() parameters}
     * and {@linkplain #arguments() arguments} are {@linkplain
     * Optional#isEmpty() empty}.
     *
     * @param <U> the type of the {@link Element}
     *
     * @param type the type of of the {@link Element}; may be {@code
     * null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} is
     * {@linkplain String#isEmpty() empty}, whose {@linkplain #type()
     * type} will {@linkplain Optional#get() yield} the supplied
     * {@code type}, and whose {@linkplain #parameters() parameters}
     * and {@linkplain #arguments() arguments} are {@linkplain
     * Optional#isEmpty() empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @SuppressWarnings("unchecked")
    public static final <U> Element<U> of(final TypeToken<U> type) {
      final Type t = type.type();
      if (t == void.class) {
        return (Element<U>)root();
      } else {
        return new Element<>("", t, null, null, false);
      }
    }

    /**
     * Returns an {@link Element} whose {@linkplain #name() name} is
     * {@linkplain String#isEmpty() empty}, whose {@linkplain #type()
     * type} will {@linkplain Optional#get() yield} the supplied
     * {@code type}, and whose {@linkplain #parameters() parameters}
     * and {@linkplain #arguments() arguments} are {@linkplain
     * Optional#isEmpty() empty}.
     *
     * @param type the type of of the {@link Element}; may be {@code
     * null}
     *
     * @return an {@link Element} whose {@linkplain #name() name} is
     * {@linkplain String#isEmpty() empty}, whose {@linkplain #type()
     * type} will {@linkplain Optional#get() yield} the supplied
     * {@code type}, and whose {@linkplain #parameters() parameters}
     * and {@linkplain #arguments() arguments} are {@linkplain
     * Optional#isEmpty() empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final Element<?> of(final Type type) {
      return type instanceof Class<?> c ? of(c) : new Element<>("", type, null, null, false);
    }


    /*
     * Inner and nested classes.
     */


    static final class Parser {


      /*
       * Static fields.
       */


      private static final int NAME = 1;

      private static final int TYPE = 2;

      private static final int ARGUMENTS = 3;


      /*
       * Instance fields.
       */


      private final ClassLoader cl;


      /*
       * Constructors.
       */


      public Parser(final ClassLoader cl) {
        super();
        this.cl = Objects.requireNonNull(cl, "cl");
      }


      /*
       * Instance methods.
       */


      public final Element<?> parse(final CharSequence s) throws ClassNotFoundException {
        int state = NAME;
        final StringBuilder sb = new StringBuilder();
        String name = null;
        Type type = null;
        List<Class<?>> params = null;
        List<String> args = null;
        final int length = s.length();
        for (int i = 0; i < length; i++) {
          final int c = s.charAt(i);
          final int next = i + 1 < length ? s.charAt(i + 1) : -1;
          switch (c) {

          case '(':
            switch (state) {
            case NAME:
              switch (next) {
              case -1:
                throw new IllegalArgumentException(iae(s, i));
              default:
                name = sb.toString();
                sb.setLength(0);
                params = new ArrayList<>(3);
                state = ARGUMENTS;
                break;
              }
              break;
            case ARGUMENTS:
            case TYPE:
              throw new IllegalArgumentException(iae(s, i));
            default:
              throw new IllegalStateException();
            }
            break;

          case ')':
            switch (state) {
            case ARGUMENTS:
              if (sb.isEmpty()) {
                if (args == null) {
                  args = List.of();
                }
              } else {
                if (args == null) {
                  params.add(loadClass(sb.toString()));
                } else {
                  args.add(sb.toString());
                }
                sb.setLength(0);
              }
              state = TYPE;
              break;
            case NAME:
            case TYPE:
              throw new IllegalArgumentException(iae(s, i));
            default:
              throw new IllegalStateException();
            }
            break;

          case '\\':
            switch (next) {
            case -1:
              throw new IllegalArgumentException(iae(s, i));
            default:
              sb.append((char)next);
              ++i;
              break;
            }
            break;

          case ',':
            switch (state) {
            case NAME:
              sb.append((char)c);
              break;
            case ARGUMENTS:
              assert name != null;
              assert type == null;
              assert params != null;
              if (params.isEmpty()) {
                throw new IllegalArgumentException(iae(s, i));
              }
              args.add(sb.toString());
              sb.setLength(0);
              break;
            case TYPE:
              throw new IllegalArgumentException(iae(s, i));
            default:
              throw new IllegalStateException();
            }
            break;

          case '=':
            switch (state) {
            case NAME:
              sb.append((char)c);
              break;
            case ARGUMENTS:
              if (!sb.isEmpty()) {
                params.add(loadClass(sb.toString()));
                sb.setLength(0);
              }
              if (args == null) {
                args = new ArrayList<>(3);
              }
              break;
            case TYPE:
              throw new IllegalArgumentException(iae(s, i));
            default:
              throw new IllegalStateException();
            }
            break;

          case ':':
            switch (state) {
            case NAME:
              name = sb.toString();
              sb.setLength(0);
              state = TYPE;
              break;
            case ARGUMENTS:
              sb.append((char)c);
              break;
            case TYPE:
              if (!sb.isEmpty()) {
                throw new IllegalArgumentException(iae(s, i));
              }
              break;
            default:
              throw new IllegalStateException();
            }
            break;

          default:
            sb.append((char)c);
            break;
          }
        }

        // Cleanup
        switch (state) {

        case NAME:
          name = sb.toString();
          sb.setLength(0);
          break;

        case ARGUMENTS:
          if (!sb.isEmpty()) {
            if (params == null) {
              params = List.of(loadClass(sb.toString()));
            } else {
              assert !params.isEmpty();
              args.add(sb.toString());
            }
            sb.setLength(0);
          }
          break;

        case TYPE:
          type = loadType(sb.toString());
          sb.setLength(0);
          break;

        default:
          throw new IllegalStateException();
        }

        assert params == null ? args == null : args == null || args.size() <= params.size() : s + "; params: " + params + "; args: " + args;

        if (name.isEmpty() && params == null && args == null && (type == null || type == void.class)) {
          return Element.root();
        } else {
          return new Element<>(name, type, params, args, false);
        }
      }


      /*
       * Static methods.
       */


      private final String iae(final CharSequence s, final int pos) {
        final StringBuilder sb = new StringBuilder(s.toString()).append(System.lineSeparator());
        for (int i = 0; i < pos; i++) {
          sb.append(' ');
        }
        sb.append('^').append(System.lineSeparator());
        return sb.toString();
      }

      private final Class<?> loadClass(final String s) throws ClassNotFoundException {
        return switch (s) {
        case "boolean" -> boolean.class;
        case "char" -> char.class;
        case "double" -> double.class;
        case "float" -> float.class;
        case "int" -> int.class;
        case "long" -> long.class;
        case "short" -> short.class;
        case "void" -> void.class;
        default -> Class.forName(s, false, this.cl);
        };
      }

      private final Type loadType(final String s) throws ClassNotFoundException {
        return loadClass(s);
      }

    }

  }

  static final class Parser {


    /*
     * Instance fields.
     */


    private final ClassLoader cl;

    private final Element.Parser parser;


    /*
     * Constructors.
     */


    public Parser(final ClassLoader cl) {
      super();
      this.cl = Objects.requireNonNull(cl, "cl");
      this.parser = new Element.Parser(cl);
    }


    /*
     * Instance methods.
     */


    public final Path<?> parse(final CharSequence s) throws ClassNotFoundException {
      if (s.isEmpty()) {
        throw new IllegalArgumentException("s.isEmpty()");
      } else {
        final List<Element<?>> elements = new ArrayList<>(11);
        final int length = s.length();
        int start = 0;
        for (int i = 0; i < length; i++) {
          final int c = s.charAt(i);
          switch (c) {
          case '/':
            if (i + 1 < length) {
              elements.add(this.parser.parse(s.subSequence(start, i)));
            } else {
              elements.add(this.parser.parse(""));
            }
            start = i + 1;
            break;
          case '\\':
            if (i + 2 < length && s.charAt(i + 1) == '/') {
              i += 2;
            }
            break;
          default:
            break;
          }
        }
        // Cleanup
        if (start < length) {
          elements.add(this.parser.parse(s.subSequence(start, length)));
        }
        assert !elements.isEmpty();
        if (elements.size() == 1 && elements.get(0).isRoot()) {
          return Path.root();
        } else {
          return new Path<>(elements, false);
        }
      }
    }

  }

}
