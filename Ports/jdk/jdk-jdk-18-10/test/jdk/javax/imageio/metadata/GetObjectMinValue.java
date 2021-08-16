/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4429875 7186799
 * @compile GetObjectMinValue.java
 * @run main GetObjectMinValue
 * @summary Tests the getObject{Min,Max}Value method of
 * IIOMetadataFormatImpl for an inclusive range
 */

import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.ImageTypeSpecifier;

public class GetObjectMinValue {

    public static void main(String argv[]) {
        test(true, true);
        test(true, false);
        test(false, true);
        test(false, false);
    }

    private static void test(boolean minInclusive, boolean maxInclusive) {
        Integer defValue = new Integer(1);
        Integer minValue = new Integer(0);
        Integer maxValue = new Integer(10);

        MyFormatImpl fmt = new MyFormatImpl("root", 1, 10);

        fmt.addObjectValue("root", defValue.getClass(), defValue,
                           minValue, maxValue, minInclusive, maxInclusive);

        try {
            Integer act_min = (Integer)fmt.getObjectMinValue("root");
            if (! act_min.equals(minValue))
                throw new RuntimeException("invalid min value: " + act_min);
        } catch (Throwable e) {
            throw new RuntimeException
                ("getObjectMinValue: unexpected exception: " + e);
        }
        try {
            Integer act_max = (Integer)fmt.getObjectMaxValue("root");
            if (! act_max.equals(maxValue))
                throw new RuntimeException("invalid max value: " + act_max);
        } catch (Throwable e) {
            throw new RuntimeException
                ("getObjectMaxValue: unexpected exception: " + e);
        }
    }

    static class MyFormatImpl extends IIOMetadataFormatImpl {

        MyFormatImpl(String root, int minChildren, int maxChildren) {
            super(root, minChildren, maxChildren);
        }

        public void addObjectValue(String elementName,
                                   Class<?> classType, Integer defaultValue,
                                   Comparable minValue, Comparable maxValue,
                                   boolean minInclusive, boolean maxInclusive) {
            super.<Integer>addObjectValue(elementName,
                                          (Class<Integer>)classType, defaultValue,
                                          (Comparable<? super Integer>) minValue, (Comparable<? super Integer>) maxValue,
                                          minInclusive, maxInclusive);
        }

        public boolean canNodeAppear(String elementName,
                                     ImageTypeSpecifier imageType) {
            return true;
        }
    }

}
