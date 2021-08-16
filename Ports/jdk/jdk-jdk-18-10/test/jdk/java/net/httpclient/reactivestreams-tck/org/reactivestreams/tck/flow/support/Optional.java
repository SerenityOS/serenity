/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.reactivestreams.tck.flow.support;

import java.util.NoSuchElementException;

// simplest possible version of Scala's Option type
public abstract class Optional<T> {

  private static final Optional<Object> NONE = new Optional<Object>() {
    @Override
    public Object get() {
      throw new NoSuchElementException(".get call on None!");
    }

    @Override
    public boolean isEmpty() {
      return true;
    }
  };

  private Optional() {
  }

  @SuppressWarnings("unchecked")
  public static <T> Optional<T> empty() {
    return (Optional<T>) NONE;
  }

  @SuppressWarnings("unchecked")
  public static <T> Optional<T> of(T it) {
    if (it == null) return (Optional<T>) Optional.NONE;
    else return new Some(it);
  }

  public abstract T get();

  public abstract boolean isEmpty();

  public boolean isDefined() {
    return !isEmpty();
  }

  public static class Some<T> extends Optional<T> {
    private final T value;

    Some(T value) {
      this.value = value;
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public String toString() {
      return String.format("Some(%s)", value);
    }
  }

  @Override
  public String toString() {
    return "None";
  }
}
