/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package build.tools.depend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Documented;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.DirectiveVisitor;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import javax.lang.model.element.ElementKind;

public class Depend implements Plugin {

    @Override
    public String getName() {
        return "depend";
    }

    @Override
    public void init(JavacTask jt, String... args) {
        jt.addTaskListener(new TaskListener() {
            private final Map<ModuleElement, Set<PackageElement>> apiPackages = new HashMap<>();
            private final MessageDigest apiHash;
            {
                try {
                    apiHash = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException ex) {
                    throw new IllegalStateException(ex);
                }
            }
            @Override
            public void started(TaskEvent te) {
            }

            @Override
            public void finished(TaskEvent te) {
                if (te.getKind() == Kind.ANALYZE) {
                    if (te.getSourceFile().isNameCompatible("module-info", JavaFileObject.Kind.SOURCE)) {
                        ModuleElement mod = (ModuleElement) Trees.instance(jt).getElement(new TreePath(te.getCompilationUnit()));
                        new APIVisitor(apiHash).visit(mod);
                    } else if (te.getSourceFile().isNameCompatible("package-info", JavaFileObject.Kind.SOURCE)) {
                        //ignore - cannot contain important changes (?)
                    } else {
                        TypeElement clazz = te.getTypeElement();
                        ModuleElement mod = jt.getElements().getModuleOf(clazz);
                        Set<PackageElement> thisModulePackages = apiPackages.computeIfAbsent(mod, m -> {
                            return ElementFilter.exportsIn(mod.getDirectives())
                                                .stream()
                                                .map(ed -> ed.getPackage())
                                                .collect(Collectors.toSet());
                        });
                        if (thisModulePackages.contains(jt.getElements().getPackageOf(clazz))) {
                            new APIVisitor(apiHash).visit(clazz);
                        }
                    }
                }
                if (te.getKind() == Kind.COMPILATION) {
                    String previousSignature = null;
                    File digestFile = new File(args[0]);
                    try (InputStream in = new FileInputStream(digestFile)) {
                        previousSignature = new String(in.readAllBytes(), "UTF-8");
                    } catch (IOException ex) {
                        //ignore
                    }
                    String currentSignature = Depend.this.toString(apiHash.digest());
                    if (!Objects.equals(previousSignature, currentSignature)) {
                        digestFile.getParentFile().mkdirs();
                        try (OutputStream out = new FileOutputStream(digestFile)) {
                            out.write(currentSignature.getBytes("UTF-8"));
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    }
                }
            }
        });
    }

    private String toString(byte[] digest) {
        StringBuilder result = new StringBuilder();

        for (byte b : digest) {
            result.append(String.format("%X", b));
        }

        return result.toString();
    }

    private static final class APIVisitor implements ElementVisitor<Void, Void>,
                                                     TypeVisitor<Void, Void>,
                                                     AnnotationValueVisitor<Void, Void>,
                                                     DirectiveVisitor<Void, Void> {

        private final MessageDigest apiHash;
        private final Charset utf8;

        public APIVisitor(MessageDigest apiHash) {
            this.apiHash = apiHash;
            utf8 = Charset.forName("UTF-8");
        }

        public Void visit(Iterable<? extends Element> list, Void p) {
            list.forEach(e -> visit(e, p));
            return null;
        }

        private void update(CharSequence data) {
            apiHash.update(data.toString().getBytes(utf8));
        }

        private void visit(Iterable<? extends TypeMirror> types) {
            for (TypeMirror type : types) {
                visit(type);
            }
        }

        private void updateAnnotation(AnnotationMirror am) {
            update("@");
            visit(am.getAnnotationType());
            am.getElementValues()
              .keySet()
              .stream()
              .sorted((ee1, ee2) -> ee1.getSimpleName().toString().compareTo(ee2.getSimpleName().toString()))
              .forEach(ee -> {
                  visit(ee);
                  visit(am.getElementValues().get(ee));
              });
        }

        private void updateAnnotations(Iterable<? extends AnnotationMirror> annotations) {
            for (AnnotationMirror am : annotations) {
                if (am.getAnnotationType().asElement().getAnnotation(Documented.class) == null)
                    continue;
                updateAnnotation(am);
            }
        }

        @Override
        public Void visit(Element e, Void p) {
            if (e.getKind() != ElementKind.MODULE &&
                !e.getModifiers().contains(Modifier.PUBLIC) &&
                !e.getModifiers().contains(Modifier.PROTECTED)) {
                return null;
            }
            update(e.getKind().name());
            update(e.getModifiers().stream()
                                   .map(mod -> mod.name())
                                   .collect(Collectors.joining(",", "[", "]")));
            update(e.getSimpleName());
            updateAnnotations(e.getAnnotationMirrors());
            return e.accept(this, p);
        }

        @Override
        public Void visit(Element e) {
            return visit(e, null);
        }

        @Override
        public Void visitModule(ModuleElement e, Void p) {
            update(String.valueOf(e.isOpen()));
            update(e.getQualifiedName());
            e.getDirectives()
             .stream()
             .forEach(d -> d.accept(this, null));
            return null;
        }

        @Override
        public Void visitPackage(PackageElement e, Void p) {
            throw new UnsupportedOperationException("Should not get here.");
        }

        @Override
        public Void visitType(TypeElement e, Void p) {
            visit(e.getTypeParameters(), p);
            visit(e.getSuperclass());
            visit(e.getInterfaces());
            visit(e.getEnclosedElements(), p);
            return null;
        }

        @Override
        public Void visitRecordComponent(@SuppressWarnings("preview")RecordComponentElement e, Void p) {
            update(e.getSimpleName());
            visit(e.asType());
            return null;
        }

        @Override
        public Void visitVariable(VariableElement e, Void p) {
            visit(e.asType());
            update(String.valueOf(e.getConstantValue()));
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableElement e, Void p) {
            update("<");
            visit(e.getTypeParameters(), p);
            update(">");
            visit(e.getReturnType());
            update("(");
            visit(e.getParameters(), p);
            update(")");
            visit(e.getThrownTypes());
            update(String.valueOf(e.getDefaultValue()));
            update(String.valueOf(e.isVarArgs()));
            return null;
        }

        @Override
        public Void visitTypeParameter(TypeParameterElement e, Void p) {
            visit(e.getBounds());
            return null;
        }

        @Override
        public Void visitUnknown(Element e, Void p) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Void visit(TypeMirror t, Void p) {
            if (t == null) {
                update("null");
                return null;
            }
            update(t.getKind().name());
            updateAnnotations(t.getAnnotationMirrors());
            t.accept(this, p);
            return null;
        }

        @Override
        public Void visitPrimitive(PrimitiveType t, Void p) {
            return null; //done
        }

        @Override
        public Void visitNull(NullType t, Void p) {
            return null; //done
        }

        @Override
        public Void visitArray(ArrayType t, Void p) {
            visit(t.getComponentType());
            update("[]");
            return null;
        }

        @Override
        public Void visitDeclared(DeclaredType t, Void p) {
            update(((QualifiedNameable) t.asElement()).getQualifiedName());
            update("<");
            visit(t.getTypeArguments());
            update(">");
            return null;
        }

        @Override
        public Void visitError(ErrorType t, Void p) {
            return visitDeclared(t, p);
        }

        private final Set<Element> seenVariables = new HashSet<>();

        @Override
        public Void visitTypeVariable(TypeVariable t, Void p) {
            Element decl = t.asElement();
            if (!seenVariables.add(decl)) {
                return null;
            }
            visit(decl, null);
            visit(t.getLowerBound(), null);
            visit(t.getUpperBound(), null);
            seenVariables.remove(decl);
            return null;
        }

        @Override
        public Void visitWildcard(WildcardType t, Void p) {
            visit(t.getSuperBound());
            visit(t.getExtendsBound());
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableType t, Void p) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Void visitNoType(NoType t, Void p) {
            return null;//done
        }

        @Override
        public Void visitUnknown(TypeMirror t, Void p) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Void visitUnion(UnionType t, Void p) {
            update("(");
            visit(t.getAlternatives());
            update(")");
            return null;
        }

        @Override
        public Void visitIntersection(IntersectionType t, Void p) {
            update("(");
            visit(t.getBounds());
            update(")");
            return null;
        }

        @Override
        public Void visit(AnnotationValue av, Void p) {
            return av.accept(this, p);
        }

        @Override
        public Void visitBoolean(boolean b, Void p) {
            update(String.valueOf(b));
            return null;
        }

        @Override
        public Void visitByte(byte b, Void p) {
            update(String.valueOf(b));
            return null;
        }

        @Override
        public Void visitChar(char c, Void p) {
            update(String.valueOf(c));
            return null;
        }

        @Override
        public Void visitDouble(double d, Void p) {
            update(String.valueOf(d));
            return null;
        }

        @Override
        public Void visitFloat(float f, Void p) {
            update(String.valueOf(f));
            return null;
        }

        @Override
        public Void visitInt(int i, Void p) {
            update(String.valueOf(i));
            return null;
        }

        @Override
        public Void visitLong(long i, Void p) {
            update(String.valueOf(i));
            return null;
        }

        @Override
        public Void visitShort(short s, Void p) {
            update(String.valueOf(s));
            return null;
        }

        @Override
        public Void visitString(String s, Void p) {
            update(s);
            return null;
        }

        @Override
        public Void visitType(TypeMirror t, Void p) {
            return visit(t);
        }

        @Override
        public Void visitEnumConstant(VariableElement c, Void p) {
            return visit(c);
        }

        @Override
        public Void visitAnnotation(AnnotationMirror a, Void p) {
            updateAnnotation(a);
            return null;
        }

        @Override
        public Void visitArray(List<? extends AnnotationValue> vals, Void p) {
            update("(");
            for (AnnotationValue av : vals) {
                visit(av);
            }
            update(")");
            return null;
        }

        @Override
        public Void visitUnknown(AnnotationValue av, Void p) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Void visitRequires(RequiresDirective d, Void p) {
            update("RequiresDirective");
            update(String.valueOf(d.isStatic()));
            update(String.valueOf(d.isTransitive()));
            update(d.getDependency().getQualifiedName());
            return null;
        }

        @Override
        public Void visitExports(ExportsDirective d, Void p) {
            update("ExportsDirective");
            update(d.getPackage().getQualifiedName());
            if (d.getTargetModules() != null) {
                for (ModuleElement me : d.getTargetModules()) {
                    update(me.getQualifiedName());
                }
            } else {
                update("<none>");
            }
            return null;
        }

        @Override
        public Void visitOpens(OpensDirective d, Void p) {
            update("OpensDirective");
            update(d.getPackage().getQualifiedName());
            if (d.getTargetModules() != null) {
                for (ModuleElement me : d.getTargetModules()) {
                    update(me.getQualifiedName());
                }
            } else {
                update("<none>");
            }
            return null;
        }

        @Override
        public Void visitUses(UsesDirective d, Void p) {
            update("UsesDirective");
            update(d.getService().getQualifiedName());
            return null;
        }

        @Override
        public Void visitProvides(ProvidesDirective d, Void p) {
            update("ProvidesDirective");
            update(d.getService().getQualifiedName());
            update("(");
            for (TypeElement impl : d.getImplementations()) {
                update(impl.getQualifiedName());
            }
            update(")");
            return null;
        }

    }
}
