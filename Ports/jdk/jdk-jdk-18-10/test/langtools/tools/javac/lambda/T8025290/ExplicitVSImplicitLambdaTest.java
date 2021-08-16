/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8025290
 * @summary javac implicit versus explicit lambda compilation error
 * @compile ExplicitVSImplicitLambdaTest.java
 */

import java.util.function.*;

public class ExplicitVSImplicitLambdaTest {
    private void test()
    {
        /* in the explicit case "e" is inferred to String so we can use a String
         * only method.
         */
        MyComparator.mycomparing1((String e) -> e.concat(""));
        MyComparator.mycomparing2((String e) -> e.concat(""));
        MyComparator.mycomparing3((String e) -> e.concat(""));
        MyComparator.mycomparing4((String e) -> e.concat(""));

        /* in the implicit case "e" is inferred to Object so toString() is OK.
         */
        MyComparator.mycomparing1((e) -> e.toString());
        MyComparator.mycomparing2((e) -> e.toString());
        MyComparator.mycomparing3((e) -> e.toString());
        MyComparator.mycomparing4((e) -> e.toString());
    }
}

interface MyComparator<T> {
    public static <T, U extends Comparable<? super U>> MyComparator<T> mycomparing1(
            Function<? super T, ? extends U> keyExtractor) {
        return null;
    }

    public static <T, U extends Comparable<? super U>> MyComparator<T> mycomparing2(
            Function<? super T, ? super U> keyExtractor) {
        return null;
    }

    public static <T, U extends Comparable<? super U>> MyComparator<T> mycomparing3(
            Function<? extends T, ? extends U> keyExtractor) {
        return null;
    }

    public static <T, U extends Comparable<? super U>> MyComparator<T> mycomparing4(
            Function<? extends T, ? super U> keyExtractor) {
        return null;
    }
}
