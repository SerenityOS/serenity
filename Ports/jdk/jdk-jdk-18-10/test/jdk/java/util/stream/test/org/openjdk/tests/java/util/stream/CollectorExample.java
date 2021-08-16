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

package org.openjdk.tests.java.util.stream;

/*
 * THE CONTENTS OF THIS FILE HAVE TO BE IN SYNC WITH THE EXAMPLES USED
 * IN THE JAVADOC.
 *
 * @test
 * @bug 8231161
 * @compile CollectorExample.java
 * @summary Compilation test only. Compile code snippets from
 * java.util.stream.Collector class-level API documentation
 */

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CollectorExample {

    // Empty helper classes

    class Widget {
    }

    class Employee {
        public int getSalary() {
            return 0;    // money isn't everything
        }

        public Department getDepartment() {
            return new Department();
        }
    }

    class Department {
    }

    <T, A, R> void testSnippet1(Collector<T, A, R> collector, T t1, T t2) {

        Supplier<A> supplier = collector.supplier();
        BiConsumer<A, T> accumulator = collector.accumulator();
        BinaryOperator<A> combiner = collector.combiner();
        Function<A, R> finisher = collector.finisher();

        // Example start
        A a1 = supplier.get();
        accumulator.accept(a1, t1);
        accumulator.accept(a1, t2);
        R r1 = finisher.apply(a1);

        A a2 = supplier.get();
        accumulator.accept(a2, t1);
        A a3 = supplier.get();
        accumulator.accept(a3, t2);
        R r2 = finisher.apply(combiner.apply(a2, a3));
    }

    void testSnippet2() {
        Collector<Widget, ?, TreeSet<Widget>> intoSet =
                Collector.of(TreeSet::new, TreeSet::add,
                        (left, right) -> { left.addAll(right); return left; });
    }

    <T, A, R> void testSnippet3(Collector<T, A, R> collector, Collection<T> data) {
        A container = collector.supplier().get();
        for (T t : data)
            collector.accumulator().accept(container, t);
        collector.finisher().apply(container);
    }

    void testSnippet4and5() {
        Collector<Employee, ?, Integer> summingSalaries
                = Collectors.summingInt(Employee::getSalary);

        Collector<Employee, ?, Map<Department, Integer>> summingSalariesByDept
                = Collectors.groupingBy(Employee::getDepartment, summingSalaries);
    }
}
