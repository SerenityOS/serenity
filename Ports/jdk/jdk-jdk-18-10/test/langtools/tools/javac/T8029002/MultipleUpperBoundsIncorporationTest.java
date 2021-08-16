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

/*
 * @test
 * @bug 8029002
 * @summary javac should take multiple upper bounds into account in incorporation
 * @compile MultipleUpperBoundsIncorporationTest.java
 */

import java.util.ArrayList;
import java.util.List;

public class MultipleUpperBoundsIncorporationTest {

    static class TestCase1 {
        interface Task<E extends Exception> {}

        class Comparator<T> {}

        class CustomException extends Exception {}

        class TaskQueue<E extends Exception, T extends Task<E>> {}

        abstract class Test {
            abstract <E extends Exception, T extends Task<E>> TaskQueue<E, T> create(Comparator<? super T> comparator);

            void f(Comparator<Task<CustomException>> comp) {
                TaskQueue<CustomException, Task<CustomException>> queue = create(comp);
                queue.getClass();
            }
        }
    }

    static class TestCase2 {
        public <T, E extends List<T>> E typedNull() {
            return null;
        }

        public void call() {
            ArrayList<String> list = typedNull();
        }
    }

    static class TestCase3 {
        interface I extends Iterable<String> {}

        <T, Exp extends Iterable<T>> Exp typedNull() { return null; }
        I i = typedNull();
    }

}
