/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

// A sample nest for use with reflection API tests
public class SampleNest {

    // recursively gather all the named nested types

    static List<Class<?>> _nestedTypes = new LinkedList<>();

    static void gather(Class<?> c) {
        _nestedTypes.add(c);
        for (Class<?> d : c.getDeclaredClasses()) {
            gather(d);
        }
    }

    static {
        gather(SampleNest.class);
        SampleNest s = new SampleNest();
    }

    public static Class<?>[] nestedTypes() {
        return _nestedTypes.toArray(new Class<?>[0]);
    }

    // Define a nested type of each possible kind

    static class StaticClass { }
    static interface StaticIface { }
    class InnerClass { }
    interface InnerIface { }

    // check multi-level nesting

    static class DeepNest1 {
        static class DeepNest2 {
            static class DeepNest3 {
            }
        }
    }

    // local and anonymous classes aren't declared
    // so they have to add themselves
    public SampleNest() {
        class LocalClass { }
        _nestedTypes.add(LocalClass.class);

        Runnable r = new Runnable() {
                public void run() {
                    // anonymous class
                    _nestedTypes.add(getClass());
                }
            };
        r.run();
    }
}

