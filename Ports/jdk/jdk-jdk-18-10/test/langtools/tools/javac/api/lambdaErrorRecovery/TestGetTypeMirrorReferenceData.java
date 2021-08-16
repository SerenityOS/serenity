/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test/*getTypeMirror:PACKAGE:test*/;

public class TestGetTypeMirrorReferenceData {

    private static void test() {
        Test.of(1).convert(c1 -> {Object o = c1/*getTypeMirror:DECLARED:java.lang.Integer*/;});
        Test.of(1).consume(c2 -> {Object o = c2/*getTypeMirror:DECLARED:java.lang.Integer*/; return null;});
        Test.of(1).consumeWithParam(c3 -> {Object o = c3/*getTypeMirror:DECLARED:java.lang.Integer*/;});
        convert(0, c4 -> {Object o = c4/*getTypeMirror:DECLARED:java.lang.Integer*/;});
        consume(0, c5 -> {Object o = c5/*getTypeMirror:DECLARED:java.lang.Integer*/;});
        convertVarArgs(0, c6 -> {Object o = c6/*getTypeMirror:DECLARED:java.lang.Integer*/;}, 1, 2, 3, 4);
        consumeVarArgs(0, c7 -> {Object o = c7/*getTypeMirror:DECLARED:java.lang.Integer*/;}, 1, 2, 3, 4);
        convertVarArgs2(0, c8 -> {Object o = c8/*getTypeMirror:DECLARED:java.lang.Integer*/;}, c8 -> {Object o = c8/*getTypeMirror:DECLARED:java.lang.Integer*/;});
        consumeVarArgs2(0, c9 -> {Object o = c9/*getTypeMirror:DECLARED:java.lang.Integer*/;}, c9 -> {Object o = c9/*getTypeMirror:DECLARED:java.lang.Integer*/;});
    }
    public <T, R> R convert(T t, Function<T, R> f, int i) {
        return null;
    }
    public <T> void consume(T t, Consumer<T> c, int i) {
    }
    public <T, R> R convertVarArgs(T t, Function<T, R> c, int... i) {
        return null;
    }
    public <T> void consumeVarArgs(T t, Consumer<T> c, int... i) {
    }
    public <T, R> R convertVarArgs2(T t, Function<T, R>... c) {
        return null;
    }
    public <T> void consumeVarArgs2(T t, Consumer<T>... c) {
    }
    public static class Test<T> {
        public static <T> Test<T> of(T t) {
            return new Test<>();
        }
        public <R> Test<R> convert(Function<T, R> c) {
            return null;
        }
        public void consume(Consumer<T> c) {}
        public void consumeWithParam(Consumer<T> c, int i) {}
    }
    public interface Function<T, R> {
        public R map(T t);
    }
    public interface Consumer<T> {
        public void run(T t);
    }
}
