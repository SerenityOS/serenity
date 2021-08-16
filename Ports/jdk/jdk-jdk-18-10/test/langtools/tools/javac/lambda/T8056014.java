/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8056014
 * @summary Verify that full type inference is used when calling a method on a type variable.
 * @compile T8056014.java
 * @run main T8056014
 */

import java.util.*;

public class T8056014 {
    public static void main(String[] args) {
        new T8056014().run();
    }

    void run() {
        List<S> l = Arrays.asList(new S());
        C<S> c = new C<>(new S());
        foo(l.get(0).copy(1));
        foo(c.get(0).copy(1));
    }

    void foo(S d) {
    }
}

class B {
    public B copy(long j) {
        throw new AssertionError("Should not get here.");
    }
}

class S extends B {
    public <T> T copy(int i) {
        return null;
    }
}

class C<T extends B> {
    final T t;
    public C(T t) {
        this.t = t;
    }
    public T get(int i) {
        return t;
    }
}
