/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8172910
 * @summary Test behavior of default methods on visitors.
 * @modules java.compiler
 */

import java.util.List;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

/**
 * Verify expected behavior of default methods on visitors.
 */
public class TestVisitorDefaults {
    public static void main(String... args) {
        DirectElementVisitorChild dvc = new DirectElementVisitorChild();
        if (!"visitUnknown".equals(dvc.visitModule(null, null))) {
            throw new RuntimeException("Problem with DirectElementVisitorChild");
        }
        if (!"visit".equals(dvc.visit(null))) {
            throw new RuntimeException("Problem with DirectElementVisitorChild");
        }

        IndirectElementVisitorChild ivc = new IndirectElementVisitorChild();
        if (!"visitUnknown".equals(ivc.visitModule(null, null))) {
            throw new RuntimeException("Problem with IndirectElementVisitorChild");
        }

        DirectTypeVisitorChild dtvc = new DirectTypeVisitorChild();
        if (!"visit".equals(dtvc.visit(null))) {
            throw new RuntimeException("Problem with DirectTypeVisitorChild");
        }

        DirectAnnotationVisitorChild davc = new DirectAnnotationVisitorChild();
        if (!"visit".equals(davc.visit(null))) {
            throw new RuntimeException("Problem with DirectAnnotationVisitorChild");
        }
    }

    private static class DirectElementVisitorChild
        implements ElementVisitor<String, Object> {

        public DirectElementVisitorChild() {
            super();
        }

        @Override
        public String visitModule(ModuleElement e, Object o) {
            return ElementVisitor.super.visitModule(e, null);
        }

        @Override
        public String visitUnknown(Element e, Object o) {
            return "visitUnknown";
        }

        @Override
        public String visit(Element e) {
            return ElementVisitor.super.visit(e);
        }

        @Override
        public String visit(Element e, Object o) {
            return "visit";
        }

        @Override
        public String visitExecutable(ExecutableElement e, Object o)       { return throwUOE(); }
        @Override
        public String visitPackage(PackageElement e, Object o)             { return throwUOE(); }
        @Override
        public String visitType(TypeElement e, Object o)                   { return throwUOE(); }
        @Override
        public String visitTypeParameter(TypeParameterElement e, Object o) { return throwUOE(); }
        @Override
        public String visitVariable(VariableElement e, Object o)           { return throwUOE(); }
    }

    private static class IndirectElementVisitorChild
        extends AbstractElementVisitor6<String, Object> {

        public IndirectElementVisitorChild() {
            super();
        }

        @Override
        public String visitModule(ModuleElement e, Object o) {
            return super.visitModule(e, o);
        }


        @Override
        public String visitUnknown(Element e, Object o) {
            return "visitUnknown";
        }

        @Override
        public String visitExecutable(ExecutableElement e, Object o)       { return throwUOE(); }
        @Override
        public String visitPackage(PackageElement e, Object o)             { return throwUOE(); }
        @Override
        public String visitType(TypeElement e, Object o)                   { return throwUOE(); }
        @Override
        public String visitTypeParameter(TypeParameterElement e, Object o) { return throwUOE(); }
        @Override
        public String visitVariable(VariableElement e, Object o)           { return throwUOE(); }
    }


    private static class DirectTypeVisitorChild
        implements TypeVisitor<String, Object> {

        public DirectTypeVisitorChild() {
            super();
        }

        @Override
        public String visit(TypeMirror t) {
            return TypeVisitor.super.visit(t);
        }

        @Override
        public String visit(TypeMirror t, Object o) {
            return "visit";
        }

        @Override
        public String visitUnknown(TypeMirror t, Object o)            { return throwUOE(); }
        @Override
        public String visitArray(ArrayType t, Object o)               { return throwUOE(); }
        @Override
        public String visitDeclared(DeclaredType t, Object o)         { return throwUOE(); }
        @Override
        public String visitError(ErrorType t, Object o)               { return throwUOE(); }
        @Override
        public String visitExecutable(ExecutableType t, Object o)     { return throwUOE(); }
        @Override
        public String visitIntersection(IntersectionType t, Object o) { return throwUOE(); }
        @Override
        public String visitNoType(NoType t, Object o)                 { return throwUOE(); }
        @Override
        public String visitNull(NullType t, Object o)                 { return throwUOE(); }
        @Override
        public String visitPrimitive(PrimitiveType t, Object o)       { return throwUOE(); }
        @Override
        public String visitTypeVariable(TypeVariable t, Object o)     { return throwUOE(); }
        @Override
        public String visitUnion(UnionType t, Object o)               { return throwUOE(); }
        @Override
        public String visitWildcard(WildcardType t, Object o)         { return throwUOE(); }
    }

    private static class DirectAnnotationVisitorChild
        implements AnnotationValueVisitor<String, Object> {

        @Override
        public String visit(AnnotationValue av) {
            return AnnotationValueVisitor.super.visit(av);
        }

        @Override
        public String visit(AnnotationValue av, Object o) {
            return "visit";
        }

        @Override
        public String visitAnnotation(AnnotationMirror a, Object o)    { return throwUOE(); }
        @Override
        public String visitArray(List<? extends AnnotationValue> vals,
                                   Object o)                           { return throwUOE(); }
        @Override
        public String visitBoolean(boolean b, Object o)                { return throwUOE(); }
        @Override
        public String visitByte(byte b, Object o)                      { return throwUOE(); }
        @Override
        public String visitChar(char c, Object o)                      { return throwUOE(); }
        @Override
        public String visitDouble(double d, Object o)                  { return throwUOE(); }
        @Override
        public String visitEnumConstant(VariableElement c, Object o)   { return throwUOE(); }
        @Override
        public String visitFloat(float f, Object o)                    { return throwUOE(); }
        @Override
        public String visitInt(int i, Object o)                        { return throwUOE(); }
        @Override
        public String visitLong(long i, Object o)                      { return throwUOE(); }
        @Override
        public String visitShort(short s, Object o)                    { return throwUOE(); }
        @Override
        public String visitString(String s, Object o)                  { return throwUOE(); }
        @Override
        public String visitType(TypeMirror t, Object o)                { return throwUOE(); }
        @Override
        public String visitUnknown(AnnotationValue av, Object o)       { return throwUOE(); }
    }

    private static String throwUOE() {
        throw new UnsupportedOperationException();
    }
}
