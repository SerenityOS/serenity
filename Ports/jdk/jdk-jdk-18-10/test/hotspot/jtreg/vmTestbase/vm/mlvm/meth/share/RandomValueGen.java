/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.meth.share;

public class RandomValueGen {

    private static final int MAX_DISTINCT_TRIES = 11;

    /*
     * Primitive values are returned boxed. void is returned as null String
     * contains 0..100 random characters
     */
    public static Object next(Class<?> type) throws InstantiationException, IllegalAccessException {
        return TestTypes.nextRandomValueForType(type);
    }

    public static Object nextDistinct(Class<?> type, Object notEqualTo) throws InstantiationException, IllegalAccessException {
        Object nonEqualValue;
        for ( int i = MAX_DISTINCT_TRIES; i > 0; i -- ){
            nonEqualValue = next(type);
            if ( ! nonEqualValue.equals(notEqualTo) )
                return nonEqualValue;
        }

        // A workaround for booleans. Sometimes RNG produces long series of trues or falses
        if ( type.equals(Boolean.class) || type.equals(boolean.class) )
            return Boolean.valueOf(! (Boolean) notEqualTo);

        throw new InstantiationException("Can't create distinct value for type=[" + type.getName() + "]; value=[" + notEqualTo + "]");
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            Class<?> type = RandomTypeGen.next();
            Object value;
            try {
                value = next(type);
                System.out.println("type=[" + type + "], value=[" + value + "]");
            } catch (Exception e) {
                System.err.println("type=[" + type + "]");
                e.printStackTrace();
            }
        }
    }


}
