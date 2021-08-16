/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.TerminatingThreadLocal;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/*
 * @test
 * @bug 8202788
 * @summary TerminatingThreadLocal unit test
 * @modules java.base/jdk.internal.misc
 * @run main TestTerminatingThreadLocal
 */
public class TestTerminatingThreadLocal {

    public static void main(String[] args) {
        ttlTestSet(42,   112);
        ttlTestSet(null, 112);
        ttlTestSet(42,  null);
    }

    static <T> void ttlTestSet(T v0, T v1) {
        ttlTest(v0, ttl -> {                                         }    );
        ttlTest(v0, ttl -> { ttl.get();                              }, v0);
        ttlTest(v0, ttl -> { ttl.get();   ttl.remove();              }    );
        ttlTest(v0, ttl -> { ttl.get();   ttl.set(v1);               }, v1);
        ttlTest(v0, ttl -> { ttl.set(v1);                            }, v1);
        ttlTest(v0, ttl -> { ttl.set(v1); ttl.remove();              }    );
        ttlTest(v0, ttl -> { ttl.set(v1); ttl.remove(); ttl.get();   }, v0);
        ttlTest(v0, ttl -> { ttl.get();   ttl.remove(); ttl.set(v1); }, v1);
    }

    @SafeVarargs
    static <T> void ttlTest(T initialValue,
                            Consumer<? super TerminatingThreadLocal<T>> ttlOps,
                            T... expectedTerminatedValues)
    {
        List<T> terminatedValues = new CopyOnWriteArrayList<>();

        TerminatingThreadLocal<T> ttl = new TerminatingThreadLocal<>() {
            @Override
            protected void threadTerminated(T value) {
                terminatedValues.add(value);
            }

            @Override
            protected T initialValue() {
                return initialValue;
            }
        };

        Thread thread = new Thread(() -> ttlOps.accept(ttl));
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!terminatedValues.equals(Arrays.asList(expectedTerminatedValues))) {
            throw new AssertionError("Expected terminated values: " +
                                     Arrays.toString(expectedTerminatedValues) +
                                     " but got: " + terminatedValues);
        }
    }
}
