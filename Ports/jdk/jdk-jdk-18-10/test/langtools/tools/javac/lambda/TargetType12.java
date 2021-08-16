/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *  Flow should not analyze lambda body that contains errors due to partially specified parameter types
 * @author  Maurizio Cimadamore
 * @compile TargetType12.java
 */

import java.util.*;

class TargetType12 {

    interface Extractor<X,Y> {
       Y get(X x);
    }

    static <T, U extends Comparable<? super U>> void sortBy2(T[] array, Extractor<T, U> extractor) {
        Comparator<T> comparator = (left,right) -> extractor.get(left).compareTo(extractor.get(right));
        Arrays.sort(array, comparator);
    }
}
