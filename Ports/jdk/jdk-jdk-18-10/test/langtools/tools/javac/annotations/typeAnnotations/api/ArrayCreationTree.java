/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Checks that the interaction between annotated and unannotated
  *         array levels in array creation trees
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import java.lang.annotation.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import javax.tools.StandardJavaFileManager;


public class ArrayCreationTree {
    public static void main(String[] args) throws Exception {
        PrintWriter out = new PrintWriter(System.out, true);
        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File testSrc = new File(System.getProperty("test.src"));
            Iterable<? extends JavaFileObject> f =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrc, "ArrayCreationTree.java")));
            JavacTask task = tool.getTask(out, fm, null, null, null, f);
            Iterable<? extends CompilationUnitTree> trees = task.parse();
            out.flush();

            Scanner s = new Scanner();
            for (CompilationUnitTree t: trees)
                s.scan(t, null);
        }
    }

    private static class Scanner extends TreeScanner<Void,Void> {
        int foundAnnotations = 0;
        public Void visitCompilationUnit(CompilationUnitTree node, Void ignore) {
            super.visitCompilationUnit(node, ignore);
            if (foundAnnotations != expectedAnnotations) {
                throw new AssertionError("Expected " + expectedAnnotations +
                        " annotations but found: " + foundAnnotations);
            }
            return null;
        }

        private void testAnnotations(List<? extends AnnotationTree> annos, int found) {
            if (annos.isEmpty()) return;

            String annotation = annos.get(0).toString();
            foundAnnotations++;

            int expected = -1;
            if (annotation.equals("@A"))
                expected = 0;
            else if (annotation.equals("@B"))
                expected = 1;
            else if (annotation.equals("@C"))
                expected = 2;
            else
                throw new AssertionError("found an unexpected annotation: " + annotation);
            if (found != expected) {
                throw new AssertionError("Unexpected found length" +
                    ", found " + found + " but expected " + expected);
            }
        }

        public Void visitAnnotatedType(AnnotatedTypeTree node, Void ignore) {
            testAnnotations(node.getAnnotations(), arrayLength(node));
            return super.visitAnnotatedType(node, ignore);
        }

        public Void visitNewArray(NewArrayTree node, Void ignore) {
            // the Tree API hasn't been updated to expose annotations yet
            JCNewArray newArray = (JCNewArray)node;
            int totalLength = node.getDimensions().size()
                                + arrayLength(node.getType())
                                + ((newArray.getInitializers() != null) ? 1 : 0);
            testAnnotations(newArray.annotations, totalLength);
            int count = 0;
            for (List<? extends AnnotationTree> annos : newArray.dimAnnotations) {
                testAnnotations(annos, totalLength - count);
                count++;
            }
            return super.visitNewArray(node, ignore);
        }

        private int arrayLength(Tree tree) {
            // TODO: the tree is null when called with node.getType(). Why?
            if (tree==null) return -1;
            switch (tree.getKind()) {
            case ARRAY_TYPE:
                return 1 + arrayLength(((ArrayTypeTree)tree).getType());
            case ANNOTATED_TYPE:
                return arrayLength(((AnnotatedTypeTree)tree).getUnderlyingType());
            default:
                return 0;
            }
        }
    }

    static int expectedAnnotations = 21;

    Object a1 = new @A Object @C [2] @B [1];
    Object b1 = new @A Object @C [2] @B [ ];
    Object c1 = new @A Object @C [ ] @B [ ] { };

    Object a2 = new @A Object @C [2]    [1];
    Object b2 = new @A Object @C [2]    [ ];
    Object c2 = new @A Object @C [ ]    [ ] { };

    Object a3 = new @A Object    [2] @B [1];
    Object b3 = new @A Object    [2] @B [ ];
    Object c3 = new @A Object    [ ] @B [ ] { };

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @interface A {}
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @interface B {}
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @interface C {}

}
