/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8146167
 * @summary Anonymous type declarations drop supertype type parameter annotations
 * @run main AnonymousExtendsTest
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({ "javadoc", "serial" })
public class AnonymousExtendsTest {

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TA {
        int value();
    }

    public class TestClass extends @TA(1) ArrayList<@TA(2) List<Number>> {
    }

    public void testIt() {
        checkAnnotations(TestClass.class.getAnnotatedSuperclass(),
              "[@AnonymousExtendsTest$TA(1)],[@AnonymousExtendsTest$TA(2)]");
        checkAnnotations(new @TA(3) ArrayList<@TA(4) List<Number>>() {
                         }.getClass().getAnnotatedSuperclass(),
              "[@AnonymousExtendsTest$TA(3)],[@AnonymousExtendsTest$TA(4)]");
    }

    public void checkAnnotations(AnnotatedType type, String expected) {
        String actual = Arrays.asList(((AnnotatedParameterizedType) type)
                                      .getAnnotations())
                                      .toString()
                                       + "," +
                        Arrays.asList(((AnnotatedParameterizedType) type)
                                       .getAnnotatedActualTypeArguments()[0].getAnnotations())
                                       .toString();

        if (!actual.equals(expected))
            throw new AssertionError("Unexpected annotations" + actual);
    }

    public static void main(String[] args) {
        new AnonymousExtendsTest().testIt();
    }
}
