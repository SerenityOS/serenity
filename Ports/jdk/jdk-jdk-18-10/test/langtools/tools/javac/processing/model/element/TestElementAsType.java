/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8224687
 * @summary Test Element.asType and its overrides
 * @library /tools/javac/lib
 * @build   JavacTestingAbstractProcessor TestElementAsType
 * @compile -processor TestElementAsType -proc:only TestElementAsType.java
 */

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;

/*
 * The leaf subinterfaces of javax.lang.model.Element of interest here
 * are:
 *
 * ExecutableElement
 * ModuleElement
 * PackageElement
 * TypeElement
 * TypeParameterElement
 * VariableElement
 *
 * For each of these categories of elements, at least one mapping of
 * the result of its asType() method should be tested for its
 * TypeMirror and TypeKind. Some elements will have more than one
 * possible TypeMirror and/or TypeKind in its image under
 * asType(). For example, a VariableElement representing a
 * ElementKind.FIELD could be a DeclaredType, ArrayType,
 * PrimitiveType, etc. In contrast, a ModuleElement should always map
 * to the pseudo-type of a NoType with a TypeKind of MODULE.
 */
public class TestElementAsType extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            List<Element> elts = new ArrayList<>();
            elts.add(Objects.requireNonNull(eltUtils.getModuleElement("java.base")));
            elts.add(Objects.requireNonNull(eltUtils.getPackageElement("java.lang")));
            elts.add(Objects.requireNonNull(eltUtils.getTypeElement("java.lang.String")));

            // Get elements representing a class, field, method,
            // constructor, and type variable from the nested class.
            TypeElement tmp =
                Objects.requireNonNull(eltUtils.getTypeElement("TestElementAsType.NestedClass"));
            elts.add(tmp);
            elts.add(ElementFilter.fieldsIn(tmp.getEnclosedElements()).get(0));
            elts.add(ElementFilter.methodsIn(tmp.getEnclosedElements()).get(0));
            elts.add(ElementFilter.constructorsIn(tmp.getEnclosedElements()).get(0));
            elts.add(tmp.getTypeParameters().get(0));

            // For a variety of different kinds of elements, check that
            // the TypeKind and TypeMirror subinterface is as expected.
            for(Element elt : elts) {
                ElementKind eltKind = elt.getKind();
                Class<?> expectedTypeClass = elementKindToTypeClass.get(eltKind);
                TypeKind expectedTypeKind = elementKindToTypeKind.get(eltKind);

                TypeMirror typeMirror = elt.asType();
                TypeKind typeKind = typeMirror.getKind();

                System.out.printf("%s\t%s\t%s%n",
                                  typeMirror,
                                  typeMirror.getClass(),
                                  typeKind);

                if (expectedTypeKind != typeKind) {
                    System.out.printf("TypeKind mismatch on ''%s'';%n\texpected %s but got %s%n",
                                      typeMirror, expectedTypeKind, typeKind);
                    throw new RuntimeException();
                }

                Class<?> typeImplClass = typeMirror.getClass();
                if (!expectedTypeClass.isAssignableFrom(typeImplClass)) {
                    System.out.printf("Unexpected assignability failure on ''%s'';%n" +
                                      "expected to be able to assign%n\t''%s'' to%n\t''%s''%n",
                                      typeMirror, typeImplClass, expectedTypeClass);
                    throw new RuntimeException();
                }
            }
        }
        return true;
    }

    /*
     * For both of the maps below, a ElementKind value is mapped to
     * one value related to an element's asType image. In some cases,
     * the ElementKind -> (TypeMirror type, TypeKind) mapping is
     * always the same, such as ElementKind.PACKAGE mapping to
     * (NoType.class, TypeKind.PACKAGE). In other cases, such as for a
     * field, there are many possible mappings and they are not
     * attempted to be examined exhaustively by this test.
     */
    private static final Map<ElementKind, Class<?>> elementKindToTypeClass =
        Map.of(ElementKind.CLASS,          DeclaredType.class,
               ElementKind.CONSTRUCTOR,    ExecutableType.class,
               ElementKind.METHOD,         ExecutableType.class,
               ElementKind.PACKAGE,        NoType.class,
               ElementKind.MODULE,         NoType.class,
               ElementKind.TYPE_PARAMETER, TypeVariable.class,
               // For the field NestedClass.name that is tested, a
               // declared type is used.
               ElementKind.FIELD,          DeclaredType.class);

    private static final Map<ElementKind, TypeKind> elementKindToTypeKind =
        Map.of(ElementKind.CLASS,          TypeKind.DECLARED,
               ElementKind.CONSTRUCTOR,    TypeKind.EXECUTABLE,
               ElementKind.METHOD,         TypeKind.EXECUTABLE,
               ElementKind.PACKAGE,        TypeKind.PACKAGE,
               ElementKind.MODULE,         TypeKind.MODULE,
               ElementKind.TYPE_PARAMETER, TypeKind.TYPEVAR,
               // For the field NestedClass.name that is tested, a
               // declared type is used.
               ElementKind.FIELD,          TypeKind.DECLARED);

    static class NestedClass<N>  {
        public NestedClass() {super();}

        String name() {
            return name;
        }

        private static String name = "NestedClass";
    }
}
