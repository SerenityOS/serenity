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
 * @bug 8259224
 * @summary A receiver type's owner type is of the correct type for nested classes.
 */

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;

public class TestReceiverTypeOwnerType<T> {

    public static void main(String[] args) throws Exception {
        AnnotatedType nested = Class.forName(TestReceiverTypeOwnerType.class.getTypeName() + "$Nested").getMethod("method").getAnnotatedReceiverType();
        if (!(nested instanceof AnnotatedParameterizedType)) {
            throw new AssertionError();
        } else if (!(nested.getAnnotatedOwnerType() instanceof AnnotatedParameterizedType)) {
            throw new AssertionError();
        }
        AnnotatedType inner = Inner.class.getMethod("method").getAnnotatedReceiverType();
        if (inner instanceof AnnotatedParameterizedType) {
            throw new AssertionError();
        } else if (inner.getAnnotatedOwnerType() instanceof AnnotatedParameterizedType) {
            throw new AssertionError();
        }
        AnnotatedType nestedInner = GenericInner.class.getMethod("method").getAnnotatedReceiverType();
        if (!(nestedInner instanceof AnnotatedParameterizedType)) {
            throw new AssertionError();
        } else if (nestedInner.getAnnotatedOwnerType() instanceof AnnotatedParameterizedType) {
            throw new AssertionError();
        }
    }

    public class Nested {
        public void method(TestReceiverTypeOwnerType<T>.Nested this) { }
    }

    public static class Inner {
        public void method(TestReceiverTypeOwnerType.Inner this) { }
    }

    public static class GenericInner<S> {
        public void method(TestReceiverTypeOwnerType.GenericInner<S> this) { }
    }
}
