/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * LambdaTranslationCompoundSamTest
 *
 * @author Brian Goetz
 */
@Test
public class LambdaTranslationCompoundSamTest {
    interface Accepts<T> {
        void accept(T t);
    }

    interface AcceptsInt {
        void accept(int i);
    }

    interface A<T> extends Accepts<T> {

        default void accept(int value) {
            throw new IllegalStateException();
        }

        interface OfInt extends A<Integer>, AcceptsInt {
            @Override
            void accept(int value);

            @Override
            default void accept(Integer i) {
                accept(i.intValue());
            }
        }
    }

    protected interface Target<T> extends A<T> {
        public interface OfInt extends Target<Integer>, A.OfInt { }
    }

    public void testConversion() {
        int[] result = new int[4];

        Target<Integer> tb = (Integer i) -> { result[0] = i+1; };
        tb.accept((Integer) 3);
        assertEquals(4, result[0]);

        Target.OfInt ti = (int i) -> { result[1] = i+1; };
        ti.accept(7);
        assertEquals(8, result[1]);
    }
}
