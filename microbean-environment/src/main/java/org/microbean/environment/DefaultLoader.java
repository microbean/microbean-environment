/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2021 microBean™.
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
package org.microbean.environment;

import java.lang.reflect.Type;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

import org.microbean.development.annotation.Experimental;

import org.microbean.environment.api.Loader;
import org.microbean.environment.api.Path;
import org.microbean.environment.api.Path.Element;
import org.microbean.environment.api.Qualifiers;

import org.microbean.environment.provider.AmbiguityHandler;
import org.microbean.environment.provider.AssignableType;
import org.microbean.environment.provider.Provider;
import org.microbean.environment.provider.Value;

/**
 * A subclassable default {@link Loader} implementation that delegates
 * its work to {@link Provider}s and an {@link #ambiguityHandler()
 * AmbiguityHandler}.
 *
 * @param <T> the type of configured objects this {@link
 * DefaultLoader} supplies
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Loader
 *
 * @see Provider
 */
public class DefaultLoader<T> implements AutoCloseable, Loader<T> {


  private static final ThreadLocal<Map<Path<?>, Deque<Provider>>> currentProviderStacks = ThreadLocal.withInitial(() -> new HashMap<>(7));


  /*
   * Instance fields.
   */


  // Package-private for testing only.
  final ConcurrentMap<Qualified<Path<?>>, DefaultLoader<?>> loaderCache;

  private final boolean deterministic;
  
  private final Path<T> absolutePath;

  private final DefaultLoader<?> parent;

  private final Supplier<T> supplier;

  private final Collection<Provider> providers;

  private final Qualifiers qualifiers;

  private final AmbiguityHandler ambiguityHandler;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link DefaultLoader}.
   *
   * @see org.microbean.environment.api.Loader#loader()
   *
   * @deprecated This constructor should be invoked by subclasses and
   * {@link ServiceLoader java.util.ServiceLoader} instances only.
   */
  @Deprecated // intended for use by subclasses and java.util.ServiceLoader only
  public DefaultLoader() {
    this(new ConcurrentHashMap<Qualified<Path<?>>, DefaultLoader<?>>(),
         null, // providers
         null, // Qualifiers
         true, // deterministic
         null, // parent,
         null, // absolutePath
         null, // Supplier
         null); // AmbiguityHandler
  }

  private DefaultLoader(final ConcurrentMap<Qualified<Path<?>>, DefaultLoader<?>> loaderCache,
                        final Collection<? extends Provider> providers,
                        final Qualifiers qualifiers,
                        final boolean deterministic,
                        final DefaultLoader<?> parent, // if null, will end up being "this" if absolutePath is null or Path.root()
                        final Path<T> absolutePath,
                        final Supplier<T> supplier, // if null, will end up being () -> this if absolutePath is null or Path.root()
                        final AmbiguityHandler ambiguityHandler) {
    super();
    this.loaderCache = Objects.requireNonNull(loaderCache, "loaderCache");
    if (parent == null) {
      // Bootstrap case, i.e. the zero-argument constructor called us.
      // Pay attention.
      if (absolutePath == null || absolutePath.equals(Path.root())) {
        @SuppressWarnings("unchecked")
        final Path<T> p = (Path<T>)Path.root();
        this.absolutePath = p;
        this.deterministic = true;
        this.parent = this; // NOTE
        this.supplier = supplier == null ? this::returnThis : supplier; // NOTE
        this.providers = List.copyOf(providers == null ? loadedProviders() : providers);
        final Qualified<Path<?>> qp = new Qualified<>(Qualifiers.of(), Path.root());
        this.loaderCache.put(qp, this); // NOTE
        // While the following call is in effect, our
        // final-but-as-yet-uninitialized qualifiers field and our
        // final-but-as-yet-uninitialized ambiguityHandler field will
        // both be null.  Note that the qualifiers() instance method
        // accounts for this and will return Qualifiers.of() instead,
        // and the ambiguityHandler() instance method does as well.
        try {
          this.qualifiers = this.load(Qualifiers.class).orElseGet(Qualifiers::of);
          this.ambiguityHandler = this.load(AmbiguityHandler.class).orElseGet(DefaultLoader::loadedAmbiguityHandler);
        } finally {
          this.loaderCache.remove(qp);
        }
      } else {
        throw new IllegalArgumentException("!absolutePath.equals(Path.root()): " + absolutePath);
      }
    } else if (absolutePath.equals(Path.root())) {
      throw new IllegalArgumentException("absolutePath.equals(Path.root()): " + absolutePath);
    } else if (!absolutePath.isAbsolute()) {
      throw new IllegalArgumentException("!absolutePath.isAbsolute(): " + absolutePath);
    } else if (!parent.absolutePath().isAbsolute()) {
      throw new IllegalArgumentException("!parent.absolutePath().isAbsolute(): " + parent.absolutePath());
    } else {
      this.absolutePath = absolutePath;
      this.deterministic = deterministic;
      this.parent = parent;
      this.supplier = Objects.requireNonNull(supplier, "supplier");
      this.providers = List.copyOf(providers);
      this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers");
      this.ambiguityHandler = Objects.requireNonNull(ambiguityHandler, "ambiguityHandler");
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Clears any caches used by this {@link DefaultLoader}.
   *
   * <p>This {@link DefaultLoader} remains valid to use.</p>
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is deterministic but not idempotent
   * unless the caches are already cleared.
   */
  @Experimental
  @Override // AutoCloseable
  public final void close() {
    this.loaderCache.clear();
  }

  /**
   * Returns an {@linkplain
   * java.util.Collections#unmodifiableCollection(Collection)
   * unmodifiable} {@link Collection} of {@link Provider}s that this
   * {@link DefaultLoader} will use to supply objects.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return an {@linkplain
   * java.util.Collections#unmodifiableCollection(Collection)
   * unmodifiable} {@link Collection} of {@link Provider}s that this
   * {@link DefaultLoader} will use to supply objects; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final Collection<Provider> providers() {
    return this.providers;
  }

  /**
   * Returns the {@link AmbiguityHandler} associated with this {@link
   * DefaultLoader}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the {@link AmbiguityHandler} associated with this {@link
   * DefaultLoader}; never {@code null}
   *
   * @nullability This method never returns {@code null}
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see AmbiguityHandler
   */
  public final AmbiguityHandler ambiguityHandler() {
    // NOTE: This null check is critical.  We check for null here
    // because during bootstrapping the AmbiguityHandler will not have
    // been loaded yet, and yet the bootstrapping mechanism may still
    // end up calling this.ambiguityHandler().  The alternative would
    // be to make the ambiguityHandler field non-final and I don't
    // want to do that.
    final AmbiguityHandler ambiguityHandler = this.ambiguityHandler;
    return ambiguityHandler == null ? NoOpAmbiguityHandler.INSTANCE : ambiguityHandler;
  }

  /**
   * Returns the {@link Qualifiers} with which this {@link DefaultLoader}
   * is associated.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the {@link Qualifiers} with which this {@link DefaultLoader}
   * is associated
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  @Override // Loader<T>
  public final Qualifiers qualifiers() {
    // NOTE: This null check is critical.  We check for null here
    // because during bootstrapping the qualifiers will not have been
    // loaded yet, and yet the bootstrapping mechanism may still end
    // up calling this.qualifiers().  The alternative would be to make
    // the qualifiers field non-final and I don't want to do that.
    final Qualifiers qualifiers = this.qualifiers;
    return qualifiers == null ? Qualifiers.of() : qualifiers;
  }

  /**
   * Returns the {@link DefaultLoader} serving as the parent of this
   * {@link DefaultLoader}.
   *
   * <p>The "root" {@link DefaultLoader} returns itself from its
   * {@link #parent()} implementation.</p>
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the non-{@code null} {@link DefaultLoader} serving as the
   * parent of this {@link DefaultLoader}; may be this {@link
   * DefaultLoader} itself
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  // Note that the root will have itself as its parent.
  @Override // Loader<T>
  public final DefaultLoader<?> parent() {
    return this.parent;
  }

  /**
   * Returns the {@linkplain Path#isAbsolute() absolute} {@link Path}
   * with which this {@link DefaultLoader} is associated.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the non-{@code null} {@linkplain Path#isAbsolute()
   * absolute} {@link Path} with which this {@link DefaultLoader} is
   * associated
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  @Override // Loader<T>
  public final Path<T> absolutePath() {
    return this.absolutePath;
  }

  /**
   * Returns {@code true} if this {@link DefaultLoader}'s {@link
   * #get()} method is deterministic.
   *
   * <p>A method is deterministic if it returns the same object
   * reference for every invocation.</p>
   *
   * @return {@code true} if this {@link DefaultLoader}'s {@link
   * #get()} method is deterministic; {@code false} otherwise
   */
  public final boolean deterministic() {
    return this.deterministic;
  }

  @Override // Loader<T>
  public final T get() {
    return this.supplier.get();
  }

  @Override // Loader<T>
  public final DefaultLoader<?> loaderFor(Path<?> path) {
    return (DefaultLoader<?>)Loader.super.loaderFor(path);
  }

  /**
   * Returns a {@link DefaultLoader} that can {@linkplain #get()
   * supply} environmental objects that are suitable for the supplied
   * {@code path}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>The {@link DefaultLoader} that is returned may return {@code
   * null} from its {@link Loader#get() get()} method.  Additionally,
   * the {@link DefaultLoader} that is returned may throw {@link
   * java.util.NoSuchElementException} or {@link
   * UnsupportedOperationException} from its {@link #get() get()}
   * method.</p>
   *
   * @param <U> the type of the supplied {@link Path} and the type of
   * the returned {@link DefaultLoader}
   *
   * @param path the {@link Path} for which a {@link DefaultLoader}
   * should be returned; must not be {@code null}
   *
   * @return a {@link DefaultLoader} capable of {@linkplain #get()
   * supplying} environmental objects suitable for the supplied {@code
   * path}; never {@code null}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @exception IllegalArgumentException if the {@code path}, after
   * {@linkplain #normalize(Path) normalization}, {@linkplain
   * Path#isRoot() is the root <code>Path</code>}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @threadsafety This method is idempotent and deterministic.
   */
  @Override // Loader<T>
  public final <U> DefaultLoader<U> load(final Path<U> path) {
    final Path<U> absolutePath = this.normalize(path);
    if (!absolutePath.isAbsolute()) {
      throw new IllegalArgumentException("!normalize(path).isAbsolute(): " + absolutePath);
    } else if (absolutePath.isRoot()) {
      throw new IllegalArgumentException("normalize(path).isRoot(): " + absolutePath);
    }
    final DefaultLoader<?> requestor = this.loaderFor(absolutePath);
    // We deliberately do not use computeIfAbsent() because load()
    // operations can kick off other load() operations, and then you'd
    // have a cache mutating operation occuring within a cache
    // mutating operation, which is forbidden.  Sometimes you get an
    // IllegalStateException as you are supposed to; other times you
    // do not, which is a JDK bug.  See
    // https://blog.jooq.org/avoid-recursion-in-concurrenthashmap-computeifabsent/.
    //
    // This obviously can result in unnecessary work, but most
    // configuration use cases will cause this work to happen anyway.
    final Qualified<Path<?>> qp = new Qualified<>(requestor.qualifiers(), absolutePath);
    DefaultLoader<?> environment = this.loaderCache.get(qp);
    if (environment == null) {
      environment = this.loaderCache.putIfAbsent(qp, this.computeLoader(requestor, absolutePath));
      if (environment == null) {
        environment = this.loaderCache.get(qp);
      }
    }
    @SuppressWarnings("unchecked")
    final DefaultLoader<U> returnValue = (DefaultLoader<U>)environment;
    return returnValue;
  }

  private final <U> DefaultLoader<U> computeLoader(final DefaultLoader<?> requestor, final Path<U> absolutePath) {
    final Qualifiers qualifiers = requestor.qualifiers();
    final AmbiguityHandler ambiguityHandler = requestor.ambiguityHandler();
    Value<U> candidate = null;
    final Collection<? extends Provider> providers = this.providers();
    if (!providers.isEmpty()) {
      final Map<Path<?>, Deque<Provider>> map = currentProviderStacks.get();
      Provider candidateProvider = null;
      if (providers.size() == 1) {
        candidateProvider = providers instanceof List<? extends Provider> list ? list.get(0) : providers.iterator().next();
        if (candidateProvider == null) {
          ambiguityHandler.providerRejected(requestor, absolutePath, candidateProvider);
        } else if (candidateProvider == peek(map, absolutePath)) {
          // Behave the same as the null case immediately prior, but
          // there's no need to notify the ambiguityHandler.
        } else if (!isSelectable(candidateProvider, absolutePath)) {
          ambiguityHandler.providerRejected(requestor, absolutePath, candidateProvider);
        } else {
          push(map, absolutePath, candidateProvider);
          try {
            candidate = candidateProvider.get(requestor, absolutePath);
          } finally {
            pop(map, absolutePath);
          }
          if (candidate == null) {
            ambiguityHandler.providerRejected(requestor, absolutePath, candidateProvider);
          } else if (!isSelectable(qualifiers, absolutePath, candidate.qualifiers(), candidate.path())) {
            ambiguityHandler.valueRejected(requestor, absolutePath, candidateProvider, candidate);
          }
        }
      } else {
        int candidateQualifiersScore = Integer.MIN_VALUE;
        int candidatePathScore = Integer.MIN_VALUE;
        PROVIDER_LOOP:
        for (final Provider provider : providers) {

          if (provider == null) {
            ambiguityHandler.providerRejected(requestor, absolutePath, provider);
            continue PROVIDER_LOOP;
          }

          if (provider == peek(map, absolutePath)) {
            // Behave the same as the null case immediately prior, but
            // there's no need to notify the ambiguityHandler.
            continue PROVIDER_LOOP;
          }

          if (!isSelectable(provider, absolutePath)) {
            ambiguityHandler.providerRejected(requestor, absolutePath, provider);
            continue PROVIDER_LOOP;
          }

          Value<U> value;

          push(map, absolutePath, provider);
          try {
            value = provider.get(requestor, absolutePath);
          } finally {
            pop(map, absolutePath);
          }

          if (value == null) {
            ambiguityHandler.providerRejected(requestor, absolutePath, provider);
            continue PROVIDER_LOOP;
          }

          // NOTE: INFINITE LOOP POSSIBILITY; read carefully!
          VALUE_EVALUATION_LOOP:
          while (true) {

            if (!isSelectable(qualifiers, absolutePath, value.qualifiers(), value.path())) {
              ambiguityHandler.valueRejected(requestor, absolutePath, provider, value);
              break VALUE_EVALUATION_LOOP;
            }

            if (candidate == null) {
              candidate = value;
              candidateProvider = provider;
              candidateQualifiersScore = ambiguityHandler.score(qualifiers, candidate.qualifiers());
              candidatePathScore = ambiguityHandler.score(absolutePath, candidate.path());
              break VALUE_EVALUATION_LOOP;
            }

            // Let's score Qualifiers first, not paths.  This is an
            // arbitrary decision.
            final int valueQualifiersScore = ambiguityHandler.score(qualifiers, value.qualifiers());
            if (valueQualifiersScore < candidateQualifiersScore) {
              candidate = new Value<>(value, candidate);
              break VALUE_EVALUATION_LOOP;
            }

            if (valueQualifiersScore > candidateQualifiersScore) {
              candidate = new Value<>(candidate, value);
              candidateProvider = provider;
              candidateQualifiersScore = valueQualifiersScore;
              // (No need to update candidatePathScore.)
              break VALUE_EVALUATION_LOOP;
            }

            // The Qualifiers scores were equal.  Let's do paths.
            final int valuePathScore = ambiguityHandler.score(absolutePath, value.path());

            if (valuePathScore < candidatePathScore) {
              candidate = new Value<>(value, candidate);
              break VALUE_EVALUATION_LOOP;
            }

            if (valuePathScore > candidatePathScore) {
              candidate = new Value<>(candidate, value);
              candidateProvider = provider;
              candidateQualifiersScore = valueQualifiersScore;
              candidatePathScore = valuePathScore;
              break VALUE_EVALUATION_LOOP;
            }

            final Value<U> disambiguatedValue =
              ambiguityHandler.disambiguate(requestor, absolutePath, candidateProvider, candidate, provider, value);

            if (disambiguatedValue == null) {
              // Couldn't disambiguate.  Drop both values.
              break VALUE_EVALUATION_LOOP;
            }

            if (disambiguatedValue.equals(candidate)) {
              candidate = new Value<>(value, candidate);
              break VALUE_EVALUATION_LOOP;
            }

            if (disambiguatedValue.equals(value)) {
              candidate = new Value<>(candidate, disambiguatedValue);
              candidateProvider = provider;
              candidateQualifiersScore = valueQualifiersScore;
              candidatePathScore = valuePathScore;
              break VALUE_EVALUATION_LOOP;
            }

            // Disambiguation came up with an entirely different value, so
            // run it back through the while loop.
            value = disambiguatedValue;
            continue VALUE_EVALUATION_LOOP;

          }
        }
      }
    }
    final Supplier<U> supplier;
    final boolean deterministic;
    if (candidate == null) {
      supplier = DefaultLoader::throwNoSuchElementException;
      deterministic = true;
    } else {
      supplier = candidate;
      deterministic = candidate.deterministic();
    }
    return
      new DefaultLoader<>(this.loaderCache,
                          providers,
                          qualifiers,
                          deterministic,
                          requestor, // parent
                          absolutePath,
                          supplier,
                          ambiguityHandler);
  }

  @SuppressWarnings("unchecked")
  private final <X> X returnThis() {
    return (X)this;
  }


  /*
   * Static methods.
   */


  private static final Provider peek(final Map<?, ? extends Deque<Provider>> map, final Path<?> absolutePath) {
    final Queue<? extends Provider> q = map.get(absolutePath);
    return q == null ? null : q.peek();
  }

  private static final void push(final Map<Path<?>, Deque<Provider>> map, final Path<?> absolutePath, final Provider provider) {
    map.computeIfAbsent(absolutePath, ap -> new ArrayDeque<>(5)).push(provider);
  }

  private static final Provider pop(final Map<?, ? extends Deque<Provider>> map, final Path<?> absolutePath) {
    final Deque<Provider> dq = map.get(absolutePath);
    return dq == null ? null : dq.pop();
  }

  private static final boolean isSelectable(final Provider provider, final Path<?> absolutePath) {
    return AssignableType.of(provider.upperBound()).isAssignable(absolutePath.type());
  }

  static final Collection<Provider> loadedProviders() {
    return Loaded.providers;
  }

  private static final AmbiguityHandler loadedAmbiguityHandler() {
    return Loaded.ambiguityHandler;
  }

  private static final boolean isSelectable(final Qualifiers referenceQualifiers,
                                            final Path<?> absoluteReferencePath,
                                            final Qualifiers valueQualifiers,
                                            final Path<?> valuePath) {
    return isSelectable(referenceQualifiers, valueQualifiers) && isSelectable(absoluteReferencePath, valuePath);
  }

  private static final boolean isSelectable(final Qualifiers referenceQualifiers, final Qualifiers valueQualifiers) {
    return referenceQualifiers.isEmpty() || valueQualifiers.isEmpty() || referenceQualifiers.intersectionSize(valueQualifiers) > 0;
  }

  /**
   * Returns {@code true} if the supplied {@code valuePath} is
   * <em>selectable</em> (for further consideration and scoring) with
   * respect to the supplied {@code absoluteReferencePath}.
   *
   * <p>This method calls {@link Path#endsWith(Path, BiPredicate)} on
   * the supplied {@code absoluteReferencePath} with {@code valuePath}
   * as its {@link Path}-typed first argument, and a {@link
   * BiPredicate} that returns {@code true} if and only if all of the
   * following conditions are true:</p>
   *
   * <ul>
   *
   * <li>Each {@link Element} has a {@linkplain Element#name()
   * name} that is either {@linkplain String#isEmpty() empty} or equal
   * to the other's.</li>
   *
   * <li>Either {@link Element} has a {@link Element#type() Type}
   * that is {@code null}, or the first {@link Element}'s {@link
   * Element#type() Type} {@link AssignableType#of(Type) is
   * assignable from} the second's.</li>
   *
   * <li>Either {@link Element} has {@code null} {@linkplain
   * Element#parameters() parameters} or each of the first {@link
   * Element}'s {@linkplain Element#parameters() parameters}
   * {@linkplain Class#isAssignableFrom(Class) is assignable from} the
   * second's corresponding parameter.</li>
   *
   * </ul>
   *
   * <p>In all other cases this method returns {@code false} or throws
   * an exception.</p>
   *
   * @param absoluteReferencePath the reference path; must not be
   * {@code null}; must be {@linkplain Path#isAbsolute() absolute}
   *
   * @param valuePath the {@link Path} to test; must not be {@code
   * null}
   *
   * @return {@code true} if {@code valuePath} is selectable (for
   * further consideration and scoring) with respect to {@code
   * absoluteReferencePath}; {@code false} in all other cases
   *
   * @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @exception IllegalArgumentException if {@code
   * absoluteReferencePath} {@linkplain Path#isAbsolute() is not
   * absolute}
   */
  private static final boolean isSelectable(final Path<?> absoluteReferencePath, final Path<?> valuePath) {
    if (!absoluteReferencePath.isAbsolute()) {
      throw new IllegalArgumentException("absoluteReferencePath: " + absoluteReferencePath);
    }
    return absoluteReferencePath.endsWith(valuePath, ElementsMatchBiPredicate.INSTANCE);
  }

  private static final <T> T throwNoSuchElementException() {
    throw new NoSuchElementException();
  }


  /*
   * Inner and nested classes.
   */


  private static final class Loaded {

    private static final List<Provider> providers =
      ServiceLoader.load(Provider.class, Provider.class.getClassLoader())
      .stream()
      .map(ServiceLoader.Provider::get)
      .toList();

    private static final AmbiguityHandler ambiguityHandler =
      ServiceLoader.load(AmbiguityHandler.class, AmbiguityHandler.class.getClassLoader())
      .stream()
      .map(ServiceLoader.Provider::get)
      .findFirst()
      .orElse(NoOpAmbiguityHandler.INSTANCE);

  }

  private static final class NoOpAmbiguityHandler implements AmbiguityHandler {

    private static final NoOpAmbiguityHandler INSTANCE = new NoOpAmbiguityHandler();

    private NoOpAmbiguityHandler() {
      super();
    }

  }

  // Matches element names (equality), parameter types
  // (isAssignableFrom) and Types (AssignableType.isAssignable()).
  // Argument values themselves are deliberately ignored.
  private static final class ElementsMatchBiPredicate implements BiPredicate<Element<?>, Element<?>> {

    private static final ElementsMatchBiPredicate INSTANCE = new ElementsMatchBiPredicate();

    private ElementsMatchBiPredicate() {
      super();
    }

    @Override // BiPredicate<Element<?>, Element<?>>
    public final boolean test(final Element<?> e1, final Element<?> e2) {
      final String name1 = e1.name();
      final String name2 = e2.name();
      if (!name1.isEmpty() && !name2.isEmpty() && !name1.equals(name2)) {
        // Empty names have special significance in that they "match"
        // any other name.
        return false;
      }

      final Type t1 = e1.type().orElse(null);
      final Type t2 = e2.type().orElse(null);
      if (t1 != null && t2 != null && !AssignableType.of(t1).isAssignable(t2)) {
        return false;
      }

      final List<Class<?>> p1 = e1.parameters().orElse(null);
      final List<Class<?>> p2 = e2.parameters().orElse(null);
      if (p1 != null && p2 != null) {
        if (p1.size() != p2.size()) {
          return false;
        } else {
          for (int i = 0; i < p1.size(); i++) {
            if (!p1.get(i).isAssignableFrom(p2.get(i))) {
              return false;
            }
          }
        }
      }

      return true;
    }

  }

}
