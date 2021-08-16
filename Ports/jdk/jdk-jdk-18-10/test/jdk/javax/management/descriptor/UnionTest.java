/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6273752
 * @summary Test ImmutableDescriptor.union
 * @author Eamonn McManus
 *
 * @run clean UnionTest
 * @run build UnionTest
 * @run main UnionTest
 */

import java.util.Collections;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import static javax.management.ImmutableDescriptor.union;
import static javax.management.ImmutableDescriptor.EMPTY_DESCRIPTOR;
import javax.management.modelmbean.DescriptorSupport;

public class UnionTest {
    public static void main(String[] args) throws Exception {
        ImmutableDescriptor immutableEmpty = new ImmutableDescriptor();
        DescriptorSupport mutableEmpty = new DescriptorSupport();

        checkEmpty(union());
        checkEmpty(union(immutableEmpty));
        checkEmpty(union(mutableEmpty));
        checkEmpty(union(EMPTY_DESCRIPTOR, immutableEmpty, mutableEmpty));
        checkEmpty(union(null, immutableEmpty, null));

        ImmutableDescriptor immutableNumbers =
            new ImmutableDescriptor(new String[] {"one", "two", "three"},
                                    new Object[] {1, 2, 3});
        final String[] noNames = null;
        DescriptorSupport mutableNumbers =
            new DescriptorSupport(immutableNumbers.getFieldNames(),
                                  immutableNumbers.getFieldValues(noNames));
        ImmutableDescriptor immutableOne =
            new ImmutableDescriptor(Collections.singletonMap("one", 1));
        DescriptorSupport mutableOne =
            new DescriptorSupport(new String[] {"one"}, new Object[] {1});
        ImmutableDescriptor immutableTwo =
            new ImmutableDescriptor(Collections.singletonMap("two", 2));
        DescriptorSupport mutableTwo =
            new DescriptorSupport(new String[] {"two"}, new Object[] {2});
        ImmutableDescriptor immutableOneTwo =
            new ImmutableDescriptor(new String[] {"one", "two"},
                                    new Object[] {1, 2});


        checkEqual(union(immutableNumbers), immutableNumbers);
        checkEqual(union(immutableNumbers, mutableNumbers), immutableNumbers);
        checkEqual(union(mutableNumbers, immutableNumbers), immutableNumbers);
        checkEqual(union(mutableEmpty, immutableEmpty, immutableNumbers,
                         mutableNumbers, immutableOne), immutableNumbers);
        checkEqual(union(immutableOne, immutableTwo, immutableNumbers),
                   immutableNumbers);
        checkEquivalent(union(immutableOne, mutableNumbers), immutableNumbers);
        checkEquivalent(union(immutableOne, immutableTwo), immutableOneTwo);
        checkEquivalent(union(mutableOne, mutableTwo), immutableOneTwo);

        if (failure != null)
            throw new Exception("TEST FAILED: " + failure);
        System.out.println("TEST PASSED");
    }

    private static void checkEmpty(ImmutableDescriptor d) {
        if (d != EMPTY_DESCRIPTOR) {
            failure = "Union of empty descriptors should be " +
                "ImmutableDescriptor.EMPTY";
            System.err.println("FAILED: " + failure);
            Thread.dumpStack();
        }
    }

    private static void checkEqual(ImmutableDescriptor d,
                                   ImmutableDescriptor e) {
        if (d != e) {
            failure = "Union should produce one of its arguments but does not";
            System.err.println("FAILED: " + failure);
            Thread.dumpStack();
        }
    }

    private static void checkEquivalent(ImmutableDescriptor d,
                                        ImmutableDescriptor e) {
        if (!d.equals(e)) {
            failure = "Union produced this: " + d + "; but should have " +
                "produced this: " + e;
            System.err.println("FAILED: " + failure);
            Thread.dumpStack();
        }
    }

    private static String failure;
}
