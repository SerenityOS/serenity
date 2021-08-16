/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262503
 * @summary Dynalink supports property getters for record components
 */

import static jdk.dynalink.StandardNamespace.PROPERTY;
import static jdk.dynalink.StandardOperation.GET;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Set;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.DynamicLinker;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.NoSuchDynamicMethodException;
import jdk.dynalink.beans.BeansLinker;
import jdk.dynalink.support.SimpleRelinkableCallSite;

public class BeanLinkerRecordsTest {
    public static record A(int num) {}

    public interface I {
        int num();
    }

    static record B(int num, int inaccessible) implements I {}

    public static record C(int num) {
        public int num() { return 42; }
        public int getNum() { return 43; }
    }

    public static void main(String[] args) throws Throwable {
        DynamicLinker linker = new DynamicLinkerFactory().createLinker();

        MethodHandle get_num = linker.link(makePropertyGetter("num")).dynamicInvoker();

        // Can access public record's component through its accessor
        assert(get_num.invoke(new A(100)).equals(100));

        // Can access non-public record's component through accessor declared in public interface
        assert(get_num.invoke(new B(100, 200)).equals(100));

        // Correctly selects overridden accessor; also ignores getXxx style getters
        assert(get_num.invoke(new C(100)).equals(42));

        // Can not access non-public record's component without accessor declared in public interface
        MethodHandle get_inaccessible = linker.link(makePropertyGetter("inaccessible")).dynamicInvoker();
        try {
            get_inaccessible.invoke(new B(100, 200));
            throw new AssertionError(); // should've failed
        } catch (NoSuchDynamicMethodException e) {
            // This is expected
        }

        // Record components show up in the list of readable instance property names.
        List.of(A.class, B.class, C.class).forEach(clazz -> {
            var propNames = BeansLinker.getReadableInstancePropertyNames(clazz);
            assert propNames.equals(Set.of("num", "class")): String.valueOf(propNames);
        });
    }

    private static SimpleRelinkableCallSite makePropertyGetter(String name) {
        return new SimpleRelinkableCallSite(new CallSiteDescriptor(
            MethodHandles.publicLookup(),
            GET.withNamespace(PROPERTY).named(name),
            MethodType.methodType(Object.class, Object.class)));
    }
}
