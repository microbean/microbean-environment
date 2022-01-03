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
package org.microbean.environment;

import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.function.Supplier;

import org.microbean.environment.api.Loader;
import org.microbean.environment.api.Path;
import org.microbean.environment.api.Qualifiers;

import org.microbean.environment.provider.AbstractProvider;
import org.microbean.environment.provider.Value;

/**
 * An {@link AbstractProvider} that provides {@link Qualifiers}
 * instances from {@linkplain System#getProperties() System
 * properties}.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public class SystemPropertiesQualifiersProvider extends AbstractProvider<Qualifiers> {


  /*
   * Constructors.
   */

  
  /**
   * Creates a new {@link SystemPropertiesQualifiersProvider}.
   */
  public SystemPropertiesQualifiersProvider() {
    super();
  }


  /*
   * Instance methods.
   */
  

  /**
   * Returns a {@link Value} suitable for the supplied {@link
   * Loader} and {@link Path}.
   *
   * <p>This implementation does the following:</p>
   *
   * <ul>
   *
   * <li>Calls the {@link Loader#load(String, Class)} method of the
   * supplied {@link Loader} to find a {@link String} qualified with
   * "{@code qualifierPrefix}".  In the very common case where such a
   * {@link String} is not found, "{@code qualifier.}" is used
   * instead.</li>
   *
   * <li>Calls {@link System#getProperties()}, and, for every {@link
   * Properties#stringPropertyNames() property name} found,
   * {@linkplain System#getProperty(String) harvests} every property
   * starting with the preifx.</li>
   *
   * <li>For each such harvested property, a new {@link Qualifiers}
   * entry is created whose name is the property name with the prefix
   * removed, and whose value is the property value.</li>
   *
   * <li>Creates a {@link Value} whose {@linkplain Value#qualifiers()
   * qualifiers} are represented by the {@link Qualifiers} created out
   * of these individual {@link Qualifiers} entries, and whose {@link
   * Value#path() Path} is created from an invocation of the {@link
   * Path#of(Type)} method supplied with the return value of an
   * invocation of {@code absolutePath.type()}.</li>
   *
   * </ul>
   *
   * <p>In many if not most scenarios, the relevant {@link Qualifiers}
   * will be {@linkplain Qualifiers#isEmpty() empty}.</p>
   *
   * @param requestor the {@link Loader} issuing the current request;
   * must not be {@code null}
   *
   * @param absolutePath the {@linkplain Path#isAbsolute() absolute}
   * {@link Path} representing the current request; must not be {@code null}
   *
   * @return a {@link Value} suitable for the supplied {@link
   * Loader} and {@link Path}.
   *
   * @exception NullPointerException if {@code requestor} or {@code
   * absolutePath} is {@code null}
   *
   * @nullability This method does not, though its overrides may,
   * return {@code null}.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @idempotency This method is, and its overrides must be
   * idempotent, but are not assumed to be deterministic.
   */
  @Override // AbstractProvider<Qualifiers>
  @SuppressWarnings("unchecked")
  public <T> Value<T> get(final Loader<?> requestor, final Path<T> absolutePath) {
    assert absolutePath.isAbsolute();
    assert absolutePath.startsWith(requestor.absolutePath());
    assert !absolutePath.equals(requestor.absolutePath());
    
    return
      new Value<>(null, // no defaults
                  Qualifiers.of(),
                  (Path<T>)Path.of(absolutePath.type()),
                  (Supplier<T>)() -> (T)qualifiers(requestor),
                  false, // nulls are not legal values
                  false); // not deterministic
  }

  private static final Qualifiers qualifiers(final Loader<?> requestor) {
    // Use the configuration system to find a String under the path
    // :void/qualifierPrefix:java.lang.String.
    final String prefix = requestor.load("qualifierPrefix", String.class).orElse("qualifier.");
    final int prefixLength = prefix.length();
    final SortedMap<String, String> map = new TreeMap<>();
    final Properties systemProperties = System.getProperties();
    for (final String propertyName : systemProperties.stringPropertyNames()) {
      if (propertyName.startsWith(prefix) && propertyName.length() > prefixLength) {
        final String qualifierValue = systemProperties.getProperty(propertyName);
        if (qualifierValue != null) {
          map.put(propertyName.substring(prefixLength), qualifierValue);
        }
      }
    }
    return Qualifiers.of(map);
  }

}
