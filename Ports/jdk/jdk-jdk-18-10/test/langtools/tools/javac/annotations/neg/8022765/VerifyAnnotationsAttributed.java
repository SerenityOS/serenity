/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public class VerifyAnnotationsAttributed {
    public static void main(String... args) throws IOException, URISyntaxException {
        if (args.length != 1) throw new IllegalStateException("Must provide class name!");
        File testSrc = new File(System.getProperty("test.src"));
        File testFile = new File(testSrc, args[0]);
        if (!testFile.canRead()) throw new IllegalStateException("Cannot read the test source");
        try (JavacFileManager fm = JavacTool.create().getStandardFileManager(null, null, null)) {
            JavacTask task = JavacTool.create().getTask(null,
                                                        fm,
                                                        null,
                                                        Collections.<String>emptyList(),
                                                        null,
                                                        fm.getJavaFileObjects(testFile));
            final Trees trees = Trees.instance(task);
            final CompilationUnitTree cut = task.parse().iterator().next();
            task.analyze();

            //ensure all the annotation attributes are annotated meaningfully
            //all the attributes in the test file should contain either an identifier
            //or a select, so only checking those for a reasonable Element/Symbol.
            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitIdentifier(IdentifierTree node, Void p) {
                    verifyAttributedMeaningfully();
                    return super.visitIdentifier(node, p);
                }
                @Override
                public Void visitMemberSelect(MemberSelectTree node, Void p) {
                    verifyAttributedMeaningfully();
                    return super.visitMemberSelect(node, p);
                }
                private void verifyAttributedMeaningfully() {
                    Element el = trees.getElement(getCurrentPath());

                    if (el == null || el.getKind() == ElementKind.OTHER) {
                        throw new IllegalStateException("Not attributed properly: " + getCurrentPath().getParentPath().getLeaf());
                    }
                }
            }.scan(cut, null);
        }
    }
}
