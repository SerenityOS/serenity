/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6273455
 * @summary Stack overflow in Types.java:347
 * @compile T6273455.java
 */

import java.util.*;

public class T6273455<T extends Comparable<? super T>> {

    abstract class Group<E extends Comparable<? super E>>
        extends ArrayList<E>
        implements Comparable<Group<? extends E>> {}

    abstract class Sequence<E extends Comparable<? super E>>
        extends TreeSet<E>
        implements Comparable<Sequence<? extends E>> {}

    public void containsCombination(SortedSet<Group<T>> groups,
                                    SortedSet<Sequence<T>> sequences) {
        foo(groups, sequences);
    }

    <C extends Collection<T>> void foo(SortedSet<? extends C> setToCheck,
                                       SortedSet<? extends C> validSet) {}

}
