/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6219304
 * @summary Subject.doAs* should be generified
 */

import java.security.*;
import javax.security.auth.Subject;

public class Generic {
    public static void main(String[] args) throws Exception {
        String foo = Subject.doAs(new Subject(),
                                new A1<String>("foo"));

        Integer one = Subject.doAs(new Subject(),
                                new A2<Integer>(new Integer("1")));

        Boolean troo = Subject.doAsPrivileged(new Subject(),
                                new A1<Boolean>(new Boolean("true")),
                                AccessController.getContext());

        Generic gen = Subject.doAsPrivileged(new Subject(),
                                new A2<Generic>(new Generic()),
                                AccessController.getContext());
    }

    private static class A1<T> implements PrivilegedAction<T> {
        T t;

        public A1(T t) {
            this.t = t;
        }

        public T run() {
            return t;
        }
    }

    private static class A2<T> implements PrivilegedExceptionAction<T> {
        T t;

        public A2(T t) {
            this.t = t;
        }

        public T run() throws PrivilegedActionException {
            return t;
        }
    }
}
