 /*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8224177
 * @summary Test warnings from the annotation processing runtime about malformed supported information from processors.
 * @compile TestRepeatedItemsRuntime.java
 * @compile/ref=gold_sv_none.out  -XDrawDiagnostics -processor TestRepeatedItemsRuntime -proc:only TestRepeatedItemsRuntime.java
 * @compile/ref=auric_current.out -XDrawDiagnostics -processor TestRepeatedItemsRuntime -proc:only -Xlint:processing TestRepeatedItemsRuntime.java
 */

import java.lang.annotation.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * A warning should be issued by the logic in
 * javax.annotation.processing.AbstractProcessor for the repeated
 * information.  The "Foo" option warnings occur regardless of source
 * level. The number of times the Baz annotation type is repeated
 * depends on whether or not the source level supports modules.
 */
@Quux
public class TestRepeatedItemsRuntime extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String>getSupportedOptions() {
        IdentityHashMap<String, Integer> temp = new IdentityHashMap<>();
        // Use String constructor for identity map.
        temp.put(new String("foo"), 1);
        temp.put(new String("foo"), 2);

        var returnValue = temp.keySet();
        assert returnValue.size() == 2;
        return returnValue;
    }

    /**
     * Partial implementation of the Set interface with identity
     * semantics and predictable iteration order.
     *
     * The javax.annotation.processing.Processor protocol relies on
     * the iterator.
     */
    private static class ArrayBackedSet implements Set<String> {
        private static String[] data = {"Quux",
                                        "Quux",
                                        "&&&/foo.Bar",
                                        "foo.Bar",
                                        "foo.Bar",
                                        "quux/Quux",
                                        "*"};
        public ArrayBackedSet() {}

        // Return an iterator of known iteration order so the set warning messages will be predictable.
        @Override
        public Iterator<String> iterator() {
            return Arrays.asList(data).iterator();
        }

        @Override
        public boolean add(String e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object o){
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (String s : data) {
                hash += s.hashCode();
            }
            return hash;
        }

        @Override
        public boolean isEmpty() {
            return data.length > 0;
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public Object[] toArray() {
            return data.clone();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Set<String>getSupportedAnnotationTypes() {
        return new ArrayBackedSet();
    }

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnvironment) {
        return true;
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface Quux {
}
