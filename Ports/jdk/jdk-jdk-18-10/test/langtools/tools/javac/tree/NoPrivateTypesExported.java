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
 * @bug 8026180 8132096
 * @summary Ensuring javax.lang.model.**, javax.tools.**, javax.annotation.processing.**
 *          and com.sun.source.** don't export inappropriate types.
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor NoPrivateTypesExported
 * @compile -processor NoPrivateTypesExported NoPrivateTypesExported.java
 */
import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.tools.Diagnostic.Kind;

public class NoPrivateTypesExported extends JavacTestingAbstractProcessor {

    private static final String[] javaxLangModelPackages = new String[] {
        "javax.lang.model",
        "javax.lang.model.element",
        "javax.lang.model.type",
        "javax.lang.model.util",
    };

    private static final Set<String> javaxLangModelAcceptable;

    private static final String[] javaxToolsProcessingPackages = new String[] {
        "javax.annotation.processing",
        "javax.tools",
    };

    private static final Set<String> javaxToolsProcessingAcceptable;

    private static final String[] comSunSourcePackages = new String[] {
        "com.sun.source.doctree",
        "com.sun.source.tree",
        "com.sun.source.util"
    };

    private static final Set<String> comSunSourceAcceptable;

    static {
        javaxLangModelAcceptable = new HashSet<>(Arrays.asList(
            "java.io.",
            "java.lang.",
            "java.net.",
            "java.nio.",
            "java.text.",
            "java.util.",
            "javax.lang.model.",
            "javax.annotation.processing.SupportedSourceVersion"
        ));
        Set<String> javaxToolsProcessingAcceptableTemp = new HashSet<>();
        javaxToolsProcessingAcceptableTemp.addAll(javaxLangModelAcceptable);
        javaxToolsProcessingAcceptableTemp.addAll(Arrays.asList(
                "javax.annotation.processing.",
                "javax.tools."
        ));
        javaxToolsProcessingAcceptable = javaxToolsProcessingAcceptableTemp;
        Set<String> comSunSourceAcceptableTemp = new HashSet<>();
        comSunSourceAcceptableTemp.addAll(javaxToolsProcessingAcceptable);
        comSunSourceAcceptableTemp.addAll(Arrays.asList(
                "com.sun.source.doctree.",
                "com.sun.source.tree.",
                "com.sun.source.util."
        ));
        comSunSourceAcceptable = comSunSourceAcceptableTemp;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            verifyPackage(javaxLangModelPackages, javaxLangModelAcceptable);
            verifyPackage(javaxToolsProcessingPackages, javaxToolsProcessingAcceptable);
            verifyPackage(comSunSourcePackages, comSunSourceAcceptable);
        }
        return true;
    }

    private void verifyPackage(String[] packagesToTest, Set<String> acceptable) {
        for (String packageToTest : packagesToTest) {
            PackageElement packageElement = processingEnv.getElementUtils()
                    .getPackageElement(packageToTest);

            verifyReferredTypesAcceptable(packageElement, acceptable);
        }
    }

    private void verifyReferredTypesAcceptable(Element rootElement,
                                               final Set<String> acceptable) {
        new ElementScanner<Void, Void>() {
            @Override public Void visitType(TypeElement e, Void p) {
                verifyTypeAcceptable(e.getSuperclass(), acceptable);
                verifyTypesAcceptable(e.getInterfaces(), acceptable);
                scan(e.getTypeParameters(), p);
                scan(e.getEnclosedElements(), p);
                verifyAnnotations(e.getAnnotationMirrors(), acceptable);
                return null;
            }
            @Override public Void visitTypeParameter(TypeParameterElement e, Void p) {
                verifyTypesAcceptable(e.getBounds(), acceptable);
                scan(e.getEnclosedElements(), p);
                verifyAnnotations(e.getAnnotationMirrors(), acceptable);
                return null;
            }
            @Override public Void visitPackage(PackageElement e, Void p) {
                scan(e.getEnclosedElements(), p);
                verifyAnnotations(e.getAnnotationMirrors(), acceptable);
                return null;
            }
            @Override public Void visitVariable(VariableElement e, Void p) {
                verifyTypeAcceptable(e.asType(), acceptable);
                scan(e.getEnclosedElements(), p);
                verifyAnnotations(e.getAnnotationMirrors(), acceptable);
                return null;
            }
            @Override
            public Void visitExecutable(ExecutableElement e, Void p) {
                scan(e.getTypeParameters(), p);
                verifyTypeAcceptable(e.getReturnType(), acceptable);
                scan(e.getParameters(), p);
                verifyTypesAcceptable(e.getThrownTypes(), acceptable);
                scan(e.getEnclosedElements(), p);
                verifyAnnotations(e.getAnnotationMirrors(), acceptable);
                return null;
            }
        }.scan(rootElement, null);
    }

    private void verifyAnnotations(Iterable<? extends AnnotationMirror> annotations,
                                   Set<String> acceptable) {
        for (AnnotationMirror mirror : annotations) {
            Element annotationElement = mirror.getAnnotationType().asElement();

            if (annotationElement.getAnnotation(Documented.class) == null) {
                note("Ignoring undocumented annotation: " + mirror.getAnnotationType());
                continue;
            }

            verifyTypeAcceptable(mirror.getAnnotationType(), acceptable);

            for (AnnotationValue value : mirror.getElementValues().values()) {
                verifyAnnotationValue(value, acceptable);
            }
        }
    }

    private void verifyAnnotationValue(AnnotationValue value,
                                       final Set<String> acceptable) {
        value.accept(new SimpleAnnotationValueVisitor<Void, Void>() {
            @Override public Void visitType(TypeMirror t, Void p) {
                verifyTypeAcceptable(t, acceptable);
                return null;
            }
            @Override
            public Void visitEnumConstant(VariableElement c, Void p) {
                verifyReferredTypesAcceptable(c, acceptable);
                return null;
            }
            @Override public Void visitArray(List<? extends AnnotationValue> vals,
                                             Void p) {
                for (AnnotationValue val : vals) {
                    val.accept(this, p);
                }
                return null;
            }
            @Override public Void visitBoolean(boolean b, Void p) {
                return null;
            }
            @Override public Void visitByte(byte b, Void p) {
                return null;
            }
            @Override public Void visitChar(char c, Void p) {
                return null;
            }
            @Override public Void visitDouble(double d, Void p) {
                return null;
            }
            @Override public Void visitFloat(float f, Void p) {
                return null;
            }
            @Override public Void visitInt(int i, Void p) {
                return null;
            }
            @Override public Void visitLong(long i, Void p) {
                return null;
            }
            @Override public Void visitShort(short s, Void p) {
                return null;
            }
            @Override public Void visitString(String s, Void p) {
                return null;
            }
            @Override protected Void defaultAction(Object o, Void p) {
                error("Unexpected AnnotationValue: " + o.toString());
                return super.defaultAction(o, p);
            }
        }, null);
    }

    private void verifyTypesAcceptable(Iterable<? extends TypeMirror> types,
                                       Set<String> acceptable) {
        if (types == null) return ;

        for (TypeMirror type : types) {
            verifyTypeAcceptable(type, acceptable);
        }
    }

    private void verifyTypeAcceptable(TypeMirror type, Set<String> acceptable) {
        if (type == null) return ;

        verifyAnnotations(type.getAnnotationMirrors(), acceptable);

        switch (type.getKind()) {
            case BOOLEAN: case BYTE: case CHAR: case DOUBLE: case FLOAT:
            case INT: case LONG: case SHORT: case VOID: case NONE: case NULL:
                return ;
            case DECLARED:
                DeclaredType dt = (DeclaredType) type;
                TypeElement outermostTypeElement = outermostTypeElement(dt.asElement());
                String outermostType = outermostTypeElement.getQualifiedName().toString();
                boolean isAcceptable = false;
                for (String acceptablePackage : acceptable) {
                    if (outermostType.startsWith(acceptablePackage)) {
                        isAcceptable = true;
                        break;
                    }
                }
                if (!isAcceptable) {
                    error("Type not acceptable for this API: " + dt.toString());
                }

                for (TypeMirror bound : dt.getTypeArguments()) {
                    verifyTypeAcceptable(bound, acceptable);
                }
                break;
            case ARRAY:
                verifyTypeAcceptable(((ArrayType) type).getComponentType(), acceptable);
                break;
            case INTERSECTION:
                for (TypeMirror element : ((IntersectionType) type).getBounds()) {
                    verifyTypeAcceptable(element, acceptable);
                }
                break;
            case TYPEVAR:
                verifyTypeAcceptable(((TypeVariable) type).getLowerBound(), acceptable);
                verifyTypeAcceptable(((TypeVariable) type).getUpperBound(), acceptable);
                break;
            case WILDCARD:
                verifyTypeAcceptable(((WildcardType) type).getExtendsBound(), acceptable);
                verifyTypeAcceptable(((WildcardType) type).getSuperBound(), acceptable);
                break;
            default:
                error("Type not acceptable for this API: " + type.toString());
                break;

        }
    }

    private TypeElement outermostTypeElement(Element el) {
        while (el.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
            el = el.getEnclosingElement();
        }

        return (TypeElement) el;
    }

    private void error(String text) {
        processingEnv.getMessager().printMessage(Kind.ERROR, text);
    }

    private void note(String text) {
        processingEnv.getMessager().printMessage(Kind.NOTE, text);
    }
}
