/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8038455 8047347
 * @summary Verify that in-method ClassSymbols from one round do not affect ClassSymbols in
 *          following rounds.
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build JavacTestingAbstractProcessor OnDemandAttribution
 * @clean Gen
 * @compile/process -processor OnDemandAttribution OnDemandAttributionData.java
 */

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import com.sun.source.tree.*;
import com.sun.source.util.*;

public class OnDemandAttribution extends JavacTestingAbstractProcessor {

    public OnDemandAttribution() {
    }

    int round;
    Set<String> roots = new HashSet<>();

    public boolean process(Set<? extends TypeElement> annos,RoundEnvironment rEnv) {
        for (Element root : rEnv.getRootElements()) {
            while (root.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
                root = root.getEnclosingElement();
            }
            roots.add(((TypeElement) root).getQualifiedName().toString());
        }
        for (String root : roots) {
            TypeElement currentClass = elements.getTypeElement(root);
            ExecutableElement constr = constructorsIn(currentClass.getEnclosedElements()).get(0);
            Trees trees = Trees.instance(processingEnv);
            TreePath path = trees.getPath(constr);

            new TreePathScanner<Void, Void>() {
                @Override public Void visitClass(ClassTree node, Void p) {
                    if (node.getSimpleName().contentEquals("Local")) {
                         //will also attribute the body on demand:
                        Element el = trees.getElement(getCurrentPath());
                        Name binaryName = elements.getBinaryName((TypeElement) el);
                        if (!binaryName.contentEquals("OnDemandAttributionData$1Local")) {
                            throw new IllegalStateException("Incorrect binary name=" + binaryName);
                        }
                    }
                    return super.visitClass(node, p);
                }
                @Override public Void visitNewClass(NewClassTree node, Void p) {
                    Element el = trees.getElement(getCurrentPath());
                    Name binaryName = elements.getBinaryName((TypeElement) el.getEnclosingElement());
                    if (!binaryName.contentEquals("OnDemandAttributionData$1")) {
                        throw new IllegalStateException("Incorrect binary name=" + binaryName);
                    }
                    return super.visitNewClass(node, p);
                }
                @Override
                public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
                    if (trees.getElement(getCurrentPath()) == null)
                        throw new IllegalStateException("No element for: " + node);
                    return super.visitMethodInvocation(node, p);
                }
                @Override
                public Void visitIdentifier(IdentifierTree node, Void p) {
                    if (trees.getElement(getCurrentPath()) == null)
                        throw new IllegalStateException("No element for: " + node);
                    return super.visitIdentifier(node, p);
                }
            }.scan(path, null);
        }

        if (round++ == 0) {
            try (Writer out = filer.createSourceFile("Gen").openWriter()) {
                out.write("class Gen { public static long C = 1; }");
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        return true;
    }
}
