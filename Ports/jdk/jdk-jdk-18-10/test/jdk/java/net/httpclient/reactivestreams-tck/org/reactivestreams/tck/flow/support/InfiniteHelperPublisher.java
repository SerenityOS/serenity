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

import org.reactivestreams.example.unicast.AsyncIterablePublisher;

import java.util.Iterator;
import java.util.concurrent.Executor;

public class InfiniteHelperPublisher<T> extends AsyncIterablePublisher<T> {

    public InfiniteHelperPublisher(final Function<Integer, T> create, final Executor executor) {
        super(new Iterable<T>() {
          @Override public Iterator<T> iterator() {
            return new Iterator<T>() {
              private int at = 0;

              @Override public boolean hasNext() { return true; }
              @Override public T next() {
                try {
                  return create.apply(at++); // Wraps around on overflow
                } catch (Throwable t) {
                  throw new IllegalStateException(
                    String.format("Failed to create element in %s for id %s!", getClass().getSimpleName(), at - 1), t);
                }
              }
              @Override public void remove() { throw new UnsupportedOperationException(); }
            };
          }
        }, executor);
    }
}
