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

package org.reactivestreams.example.unicast;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Executor;
import org.reactivestreams.Subscription;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Publisher;

public class NumberIterablePublisher extends AsyncIterablePublisher<Integer> {
    public NumberIterablePublisher(final int from, final int to, final Executor executor) {
        super(new Iterable<Integer>() {
          { if(from > to) throw new IllegalArgumentException("from must be equal or greater than to!"); }
          @Override public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
              private int at = from;
              @Override public boolean hasNext() { return at < to; }
              @Override public Integer next() {
                if (!hasNext()) return Collections.<Integer>emptyList().iterator().next();
                else return at++;
              }
              @Override public void remove() { throw new UnsupportedOperationException(); }
            };
          }
        }, executor);
    }
}
