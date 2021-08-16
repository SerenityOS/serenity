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

/*
 * @test
 * @bug 8222251
 * @summary preflow visitor is not visiting lambda expressions
 * @compile PreflowNotVisitingLambdaExpTest.java
 */

public class PreflowNotVisitingLambdaExpTest {
    interface HandleCallback<T, X extends Exception> {
        T withHandle(Handle handle) throws X;
    }

    interface HandleConsumer<X extends Exception> {
        void useHandle(Handle handle) throws X;
    }

    interface Handle {}

    interface Jdbi {
        <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X;
        <X extends Exception> void useHandle(final HandleConsumer<X> callback) throws X;
    }

    interface ObjectAssert<ACTUAL> {
        void isSameAs(ACTUAL t);
    }

    static <T> ObjectAssert<T> assertThat(T actual) {
        return null;
    }

    private Jdbi jdbi;

    public void nestedUseHandle() {
        jdbi.withHandle(h1 -> {
            jdbi.useHandle(h2 ->
                    assertThat(h1).isSameAs(h2));
            return null;
        });
    }
}
