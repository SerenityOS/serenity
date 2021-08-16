/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.javac.processing;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import static javax.lang.model.element.ElementKind.*;
import static javax.lang.model.element.NestingKind.*;
import static javax.lang.model.element.ModuleElement.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;


import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.StringUtils;

/**
 * A processor which prints out elements.  Used to implement the
 * -Xprint option; the included visitor class is used to implement
 * Elements.printElements.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_18)
public class PrintingProcessor extends AbstractProcessor {
    PrintWriter writer;

    public PrintingProcessor() {
        super();
        writer = new PrintWriter(System.out);
    }

    public void setWriter(Writer w) {
        writer = new PrintWriter(w);
    }

    @Override @DefinedBy(Api.ANNOTATION_PROCESSING)
    public boolean process(Set<? extends TypeElement> tes,
                           RoundEnvironment renv) {

        for(Element element : renv.getRootElements()) {
            print(element);
        }

        // Just print the elements, nothing more to do.
        return true;
    }

    void print(Element element) {
        new PrintingElementVisitor(writer, processingEnv.getElementUtils()).
            visit(element).flush();
    }

    /**
     * Used for the -Xprint option and called by Elements.printElements
     */
    public static class PrintingElementVisitor
        extends SimpleElementVisitor14<PrintingElementVisitor, Boolean> {
        int indentation; // Indentation level;
        final PrintWriter writer;
        final Elements elementUtils;

        public PrintingElementVisitor(Writer w, Elements elementUtils) {
            super();
            this.writer = new PrintWriter(w);
            this.elementUtils = elementUtils;
            indentation = 0;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        protected PrintingElementVisitor defaultAction(Element e, Boolean newLine) {
            if (newLine != null && newLine)
                writer.println();
            printDocComment(e);
            printModifiers(e);
            return this;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public PrintingElementVisitor visitRecordComponent(RecordComponentElement e, Boolean p) {
                // Do nothing; printing of component information done by
                // printing the record type itself
            return this;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public PrintingElementVisitor visitExecutable(ExecutableElement e, Boolean p) {
            ElementKind kind = e.getKind();

            if (kind != STATIC_INIT &&
                kind != INSTANCE_INIT) {
                Element enclosing = e.getEnclosingElement();

                // Don't print out the constructor of an anonymous class
                if (kind == CONSTRUCTOR &&
                    enclosing != null &&
                    NestingKind.ANONYMOUS ==
                    // Use an anonymous class to determine anonymity!
                    (new SimpleElementVisitor14<NestingKind, Void>() {
                        @Override @DefinedBy(Api.LANGUAGE_MODEL)
                        public NestingKind visitType(TypeElement e, Void p) {
                            return e.getNestingKind();
                        }
                    }).visit(enclosing))
                    return this;

                defaultAction(e, true);
                printFormalTypeParameters(e, true);

                switch(kind) {
                    case CONSTRUCTOR:
                    // Print out simple name of the class
                    writer.print(e.getEnclosingElement().getSimpleName());
                    break;

                    case METHOD:
                    writer.print(e.getReturnType().toString());
                    writer.print(" ");
                    writer.print(e.getSimpleName().toString());
                    break;
                }

                writer.print("(");
                printParameters(e);
                writer.print(")");
                AnnotationValue defaultValue = e.getDefaultValue();
                if (defaultValue != null)
                    writer.print(" default " + defaultValue);

                printThrows(e);
                writer.println(";");
            }
            return this;
        }


        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public PrintingElementVisitor visitType(TypeElement e, Boolean p) {
            ElementKind kind = e.getKind();
            NestingKind nestingKind = e.getNestingKind();

            if (NestingKind.ANONYMOUS == nestingKind) {
                // Print out an anonymous class in the style of a
                // class instance creation expression rather than a
                // class declaration.
                writer.print("new ");

                // If the anonymous class implements an interface
                // print that name, otherwise print the superclass.
                List<? extends TypeMirror> interfaces = e.getInterfaces();
                if (!interfaces.isEmpty())
                    writer.print(interfaces.get(0));
                else
                    writer.print(e.getSuperclass());

                writer.print("(");
                // Anonymous classes that implement an interface can't
                // have any constructor arguments.
                if (interfaces.isEmpty()) {
                    // Print out the parameter list from the sole
                    // constructor.  For now, don't try to elide any
                    // synthetic parameters by determining if the
                    // anonymous class is in a static context, etc.
                    List<? extends ExecutableElement> constructors =
                        ElementFilter.constructorsIn(e.getEnclosedElements());

                    if (!constructors.isEmpty())
                        printParameters(constructors.get(0));
                }
                writer.print(")");
            } else {
                if (nestingKind == TOP_LEVEL) {
                    PackageElement pkg = elementUtils.getPackageOf(e);
                    if (!pkg.isUnnamed())
                        writer.print("package " + pkg.getQualifiedName() + ";\n");
                }

                defaultAction(e, true);

                switch(kind) {
                case ANNOTATION_TYPE:
                    writer.print("@interface");
                    break;
                default:
                    writer.print(StringUtils.toLowerCase(kind.toString()));
                }
                writer.print(" ");
                writer.print(e.getSimpleName());

                printFormalTypeParameters(e, false);

                if (kind == RECORD) {
                    // Print out record components
                    writer.print("(");
                    writer.print(e.getRecordComponents()
                                 .stream()
                                 .map(recordDes -> annotationsToString(recordDes) + recordDes.asType().toString() + " " + recordDes.getSimpleName())
                                 .collect(Collectors.joining(", ")));
                    writer.print(")");
                }

                // Print superclass information if informative
                if (kind == CLASS) {
                    TypeMirror supertype = e.getSuperclass();
                    if (supertype.getKind() != TypeKind.NONE) {
                        TypeElement e2 = (TypeElement)
                            ((DeclaredType) supertype).asElement();
                        if (e2.getSuperclass().getKind() != TypeKind.NONE)
                            writer.print(" extends " + supertype);
                    }
                }

                printInterfaces(e);
                printPermittedSubclasses(e);
            }
            writer.println(" {");
            indentation++;

            if (kind == ENUM) {
                List<Element> enclosedElements = new ArrayList<>(e.getEnclosedElements());
                // Handle any enum constants specially before other entities.
                List<Element> enumConstants = new ArrayList<>();
                for(Element element : enclosedElements) {
                    if (element.getKind() == ENUM_CONSTANT)
                        enumConstants.add(element);
                }
                if (!enumConstants.isEmpty()) {
                    int i;
                    for(i = 0; i < enumConstants.size()-1; i++) {
                        this.visit(enumConstants.get(i), true);
                        writer.print(",");
                    }
                    this.visit(enumConstants.get(i), true);
                    writer.println(";\n");

                    enclosedElements.removeAll(enumConstants);
                }

                for(Element element : enclosedElements)
                    this.visit(element);
            } else {
                for(Element element :
                        (kind != RECORD ?
                         e.getEnclosedElements() :
                         e.getEnclosedElements()
                         .stream()
                         .filter(elt -> elementUtils.getOrigin(elt) == Elements.Origin.EXPLICIT )
                         .toList() ) )
                    this.visit(element);
            }

            indentation--;
            indent();
            writer.println("}");
            return this;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public PrintingElementVisitor visitVariable(VariableElement e, Boolean newLine) {
            ElementKind kind = e.getKind();
            defaultAction(e, newLine);

            if (kind == ENUM_CONSTANT)
                writer.print(e.getSimpleName());
            else {
                writer.print(e.asType().toString() + " " + e.getSimpleName() );
                Object constantValue  = e.getConstantValue();
                if (constantValue != null) {
                    writer.print(" = ");
                    writer.print(elementUtils.getConstantExpression(constantValue));
                }
                writer.println(";");
            }
            return this;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public PrintingElementVisitor visitTypeParameter(TypeParameterElement e, Boolean p) {
            writer.print(e.getSimpleName());
            return this;
        }

        // Should we do more here?
        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public PrintingElementVisitor visitPackage(PackageElement e, Boolean p) {
            defaultAction(e, false);
            if (!e.isUnnamed())
                writer.println("package " + e.getQualifiedName() + ";");
            else
                writer.println("// Unnamed package");
            return this;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public PrintingElementVisitor visitModule(ModuleElement e, Boolean p) {
            defaultAction(e, false);

            if (!e.isUnnamed()) {
                if (e.isOpen()) {
                    writer.print("open ");
                }
                writer.println("module " + e.getQualifiedName() + " {");
                indentation++;
                for (ModuleElement.Directive directive : e.getDirectives()) {
                    printDirective(directive);
                }
                indentation--;
                writer.println("}");
            } else
                writer.println("// Unnamed module"); // Should we do more here?
            return this;
        }

        private void printDirective(ModuleElement.Directive directive) {
            indent();
            (new PrintDirective(writer)).visit(directive);
            writer.println(";");
        }

        private static class PrintDirective implements ModuleElement.DirectiveVisitor<Void, Void> {
            private final PrintWriter writer;

            PrintDirective(PrintWriter writer) {
                this.writer = writer;
            }

            @Override @DefinedBy(Api.LANGUAGE_MODEL)
            public Void visitExports(ExportsDirective d, Void p) {
                // "exports package-name [to module-name-list]"
                writer.print("exports ");
                writer.print(d.getPackage().getQualifiedName());
                printModuleList(d.getTargetModules());
                return null;
            }

            @Override @DefinedBy(Api.LANGUAGE_MODEL)
            public Void visitOpens(OpensDirective d, Void p) {
                // opens package-name [to module-name-list]
                writer.print("opens ");
                writer.print(d.getPackage().getQualifiedName());
                printModuleList(d.getTargetModules());
                return null;
            }

            @Override @DefinedBy(Api.LANGUAGE_MODEL)
            public Void visitProvides(ProvidesDirective d, Void p) {
                // provides service-name with implementation-name
                writer.print("provides ");
                writer.print(d.getService().getQualifiedName());
                writer.print(" with ");
                printNameableList(d.getImplementations());
                return null;
            }

            @Override @DefinedBy(Api.LANGUAGE_MODEL)
            public Void visitRequires(RequiresDirective d, Void p) {
                // requires (static|transitive)* module-name
                writer.print("requires ");
                if (d.isStatic())
                    writer.print("static ");
                if (d.isTransitive())
                    writer.print("transitive ");
                writer.print(d.getDependency().getQualifiedName());
                return null;
            }

            @Override @DefinedBy(Api.LANGUAGE_MODEL)
            public Void visitUses(UsesDirective d, Void p) {
                // uses service-name
                writer.print("uses ");
                writer.print(d.getService().getQualifiedName());
                return null;
            }

            private void printModuleList(List<? extends ModuleElement> modules) {
                if (modules != null) {
                    writer.print(" to ");
                    printNameableList(modules);
                }
            }

            private void printNameableList(List<? extends QualifiedNameable> nameables) {
                writer.print(nameables.stream().
                             map(QualifiedNameable::getQualifiedName).
                             collect(Collectors.joining(", ")));
            }
        }

        public void flush() {
            writer.flush();
        }

        private void printDocComment(Element e) {
            String docComment = elementUtils.getDocComment(e);

            if (docComment != null) {
                // Break comment into lines
                java.util.StringTokenizer st = new StringTokenizer(docComment,
                                                                  "\n\r");
                indent();
                writer.println("/**");

                while(st.hasMoreTokens()) {
                    indent();
                    writer.print(" *");
                    writer.println(st.nextToken());
                }

                indent();
                writer.println(" */");
            }
        }

        private void printModifiers(Element e) {
            ElementKind kind = e.getKind();
            if (kind == PARAMETER || kind == RECORD_COMPONENT) {
                // Print annotation inline
                writer.print(annotationsToString(e));
            } else {
                printAnnotations(e);
                indent();
            }

            if (kind == ENUM_CONSTANT || kind == RECORD_COMPONENT)
                return;

            Set<Modifier> modifiers = new LinkedHashSet<>();
            modifiers.addAll(e.getModifiers());

            switch (kind) {
            case ANNOTATION_TYPE:
            case INTERFACE:
                modifiers.remove(Modifier.ABSTRACT);
                break;

            case ENUM:
                modifiers.remove(Modifier.FINAL);
                modifiers.remove(Modifier.ABSTRACT);
                modifiers.remove(Modifier.SEALED);
                break;

            case RECORD:
                modifiers.remove(Modifier.FINAL);
                break;

            case METHOD:
            case FIELD:
                Element enclosingElement = e.getEnclosingElement();
                if (enclosingElement != null &&
                    enclosingElement.getKind().isInterface()) {
                    modifiers.remove(Modifier.PUBLIC);
                    modifiers.remove(Modifier.ABSTRACT); // only for methods
                    modifiers.remove(Modifier.STATIC);   // only for fields
                    modifiers.remove(Modifier.FINAL);    // only for fields
                }
                break;

            }
            if (!modifiers.isEmpty()) {
                writer.print(modifiers.stream()
                             .map(Modifier::toString)
                             .collect(Collectors.joining(" ", "", " ")));
            }
        }

        private void printFormalTypeParameters(Parameterizable e,
                                               boolean pad) {
            List<? extends TypeParameterElement> typeParams = e.getTypeParameters();
            if (!typeParams.isEmpty()) {
                writer.print(typeParams.stream()
                             .map(tpe -> annotationsToString(tpe) + tpe.toString())
                             .collect(Collectors.joining(", ", "<", ">")));
                if (pad)
                    writer.print(" ");
            }
        }

        private String annotationsToString(Element e) {
            List<? extends AnnotationMirror> annotations = e.getAnnotationMirrors();
            return annotations.isEmpty() ?
                "" :
                annotations.stream()
                .map(AnnotationMirror::toString)
                .collect(Collectors.joining(" ", "", " "));
        }

        private void printAnnotations(Element e) {
            List<? extends AnnotationMirror> annots = e.getAnnotationMirrors();
            for(AnnotationMirror annotationMirror : annots) {
                // Handle compiler-generated container annotations specially
                if (!printedContainerAnnotation(e, annotationMirror)) {
                    indent();
                    writer.println(annotationMirror);
                }
            }
        }

        private boolean printedContainerAnnotation(Element e,
                                                   AnnotationMirror annotationMirror) {
            /*
             * If the annotation mirror is marked as mandated and
             * looks like a container annotation, elide printing the
             * container and just print the wrapped contents.
             */
            if (elementUtils.getOrigin(e, annotationMirror) == Elements.Origin.MANDATED) {
                // From JLS Chapter 9, an annotation interface AC is a
                // containing annotation interface of A if AC declares
                // a value() method whose return type is A[] and any
                // methods declared by AC other than value() have a
                // default value. As an implementation choice, if more
                // than one annotation element is found on the outer
                // annotation, in other words, something besides a
                // "value" method, the annotation will not be treated
                // as a wrapper for the purposes of printing. These
                // checks are intended to preserve correctness in the
                // face of some other kind of annotation being marked
                // as mandated.

                var entries = annotationMirror.getElementValues().entrySet();
                if (entries.size() == 1) {
                    var annotationType = annotationMirror.getAnnotationType();
                    var annotationTypeAsElement = annotationType.asElement();

                    var entry = entries.iterator().next();
                    var annotationElements = entry.getValue();

                    // Check that the annotation type declaration has
                    // a single method named "value" and that it
                    // returns an array. A stricter check would be
                    // that it is an array of an annotation type and
                    // that annotation type in turn was repeatable.
                    if (annotationTypeAsElement.getKind() == ElementKind.ANNOTATION_TYPE) {
                        var annotationMethods =
                            ElementFilter.methodsIn(annotationTypeAsElement.getEnclosedElements());
                        if (annotationMethods.size() == 1) {
                            var valueMethod = annotationMethods.get(0);
                            var returnType = valueMethod.getReturnType();

                            if ("value".equals(valueMethod.getSimpleName().toString()) &&
                                returnType.getKind() == TypeKind.ARRAY) {
                                // Use annotation value visitor that
                                // returns a boolean if it prints out
                                // contained annotations as expected
                                // and false otherwise

                                return (new SimpleAnnotationValueVisitor14<Boolean, Void>(false) {
                                    @Override
                                    public Boolean visitArray(List<? extends AnnotationValue> vals, Void p) {
                                        if (vals.size() < 2) {
                                            return false;
                                        } else {
                                            for (var annotValue: vals) {
                                                indent();
                                                writer.println(annotValue.toString());
                                            }
                                            return true;
                                        }
                                    }
                                }).visit(annotationElements);
                            }
                        }
                    }
                }
            }
            return false;
        }

        // TODO: Refactor
        private void printParameters(ExecutableElement e) {
            List<? extends VariableElement> parameters = e.getParameters();
            int size = parameters.size();

            switch (size) {
            case 0:
                break;

            case 1:
                for(VariableElement parameter: parameters) {
                    printModifiers(parameter);

                    if (e.isVarArgs() ) {
                        TypeMirror tm = parameter.asType();
                        if (tm.getKind() != TypeKind.ARRAY)
                            throw new AssertionError("Var-args parameter is not an array type: " + tm);
                        writer.print((ArrayType.class.cast(tm)).getComponentType() );
                        writer.print("...");
                    } else
                        writer.print(parameter.asType());
                    writer.print(" " + parameter.getSimpleName());
                }
                break;

            default:
                {
                    int i = 1;
                    for(VariableElement parameter: parameters) {
                        if (i == 2)
                            indentation++;

                        if (i > 1)
                            indent();

                        printModifiers(parameter);

                        if (i == size && e.isVarArgs() ) {
                            TypeMirror tm = parameter.asType();
                            if (tm.getKind() != TypeKind.ARRAY)
                                throw new AssertionError("Var-args parameter is not an array type: " + tm);
                                    writer.print((ArrayType.class.cast(tm)).getComponentType() );

                            writer.print("...");
                        } else
                            writer.print(parameter.asType());
                        writer.print(" " + parameter.getSimpleName());

                        if (i < size)
                            writer.println(",");

                        i++;
                    }

                    if (parameters.size() >= 2)
                        indentation--;
                }
                break;
            }
        }

        private void printInterfaces(TypeElement e) {
            ElementKind kind = e.getKind();

            if(kind != ANNOTATION_TYPE) {
                List<? extends TypeMirror> interfaces = e.getInterfaces();
                if (!interfaces.isEmpty()) {
                    writer.print((kind.isClass() ? " implements " : " extends "));
                    writer.print(interfaces.stream()
                                 .map(TypeMirror::toString)
                                 .collect(Collectors.joining(", ")));
                }
            }
        }

        private void printPermittedSubclasses(TypeElement e) {
            List<? extends TypeMirror> subtypes = e.getPermittedSubclasses();
            if (!subtypes.isEmpty()) { // could remove this check with more complicated joining call
                writer.print(" permits ");
                writer.print(subtypes
                             .stream()
                             .map(subtype -> subtype.toString())
                             .collect(Collectors.joining(", ")));
            }
        }

        private void printThrows(ExecutableElement e) {
            List<? extends TypeMirror> thrownTypes = e.getThrownTypes();
            final int size = thrownTypes.size();
            if (size != 0) {
                writer.print(" throws");

                int i = 1;
                for(TypeMirror thrownType: thrownTypes) {
                    if (i == 1)
                        writer.print(" ");

                    if (i == 2)
                        indentation++;

                    if (i >= 2)
                        indent();

                    writer.print(thrownType);

                    if (i != size)
                        writer.println(", ");

                    i++;
                }

                if (size >= 2)
                    indentation--;
            }
        }

        private static final String [] spaces = {
            "",
            "  ",
            "    ",
            "      ",
            "        ",
            "          ",
            "            ",
            "              ",
            "                ",
            "                  ",
            "                    "
        };

        private void indent() {
            int indentation = this.indentation;
            if (indentation < 0)
                return;
            final int maxIndex = spaces.length - 1;

            while (indentation > maxIndex) {
                writer.print(spaces[maxIndex]);
                indentation -= maxIndex;
            }
            writer.print(spaces[indentation]);
        }

    }
}
