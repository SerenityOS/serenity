/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8027730
 * @summary Test visitor support for intersection types
 * @modules java.compiler
 *          jdk.compiler
 */

import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

public class TestIntersectionTypeVisitors {
    public static void main(String... args) throws Exception {
        IntersectionType it = new TestIntersectionType();

        boolean result = it.accept(new TypeKindVisitor8Child(), null) &&
            it.accept(new SimpleTypeVisitor8Child(), null) &&
            it.accept(new SimpleTypeVisitor6Child(), null);

        if (!result)
            throw new RuntimeException();
    }

    static class TestIntersectionType implements IntersectionType {
        TestIntersectionType() {}

        @Override
        public List<? extends TypeMirror> getBounds() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R,P> R accept(TypeVisitor<R,P> v,
                       P p) {
            return v.visitIntersection(this, p);
        }

        @Override
        public TypeKind getKind() {
            return TypeKind.INTERSECTION;
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            throw new UnsupportedOperationException();
        }
    }

    static class TypeKindVisitor8Child extends TypeKindVisitor8<Boolean, Void> {
        TypeKindVisitor8Child() {
            super(false);
        }

        @Override
        public Boolean visitIntersection(IntersectionType t, Void p) {
            super.visitIntersection(t, p); // Make sure overridden method doesn't throw an exception
            return true;
        }
    }

    static class SimpleTypeVisitor8Child extends SimpleTypeVisitor8<Boolean, Void> {
        SimpleTypeVisitor8Child() {
            super(false);
        }

        @Override
        public Boolean visitIntersection(IntersectionType t, Void p) {
            super.visitIntersection(t, p);  // Make sure overridden method doesn't throw an exception
            return true;
        }
    }

    static class SimpleTypeVisitor6Child extends SimpleTypeVisitor6<Boolean, Void> {
        SimpleTypeVisitor6Child() {
            super(false);
        }

        @Override
        public Boolean visitIntersection(IntersectionType t, Void p) {
            try {
                super.visitIntersection(t, p);
                return false;
            } catch (UnknownTypeException ute) {
                return true; // Expected
            }
        }
    }
}
