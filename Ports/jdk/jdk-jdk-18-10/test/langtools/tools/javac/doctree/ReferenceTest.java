/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7021614
 * @summary extend com.sun.source API to support parsing javadoc comments
 * @summary check references in at-see and {at-link} tags
 * @modules jdk.compiler
 * @build ReferenceTest
 * @compile -processor ReferenceTest -proc:only ReferenceTest.java
 */

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;

import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

/**
 * {@link java.lang        Package}
 * {@link java.lang.ERROR  Bad}
 * {@link java.lang#ERROR  Bad}
 *
 * {@link java.lang.String Class}
 * {@link           String Class}
 * {@link java.lang.String#CASE_INSENSITIVE_ORDER Field}
 * {@link java.lang.String#String Constructor}
 * {@link java.lang.String#String(byte[]) Constructor}
 * {@link java.lang.String#String(byte[] bytes) Constructor}
 * {@link java.lang.String#String(byte[], String) Constructor}
 * {@link java.lang.String#String(byte[] bytes, String charSetName) Constructor}
 * {@link java.lang.String#isEmpty Method}
 * {@link java.lang.String#isEmpty() Method}
 * {@link java.lang.String#ERROR Bad}
 * {@link java.lang.String#equals(Object) Method}
 *
 * {@link AbstractProcessor Class}
 *
 * {@link List#add(Object) Method}
 *
 * {@link #trees Field}
 * {@link #getSupportedSourceVersion Method}
 * {@link #init(ProcessingEnvironment Method}
 *
 * @see java.lang        Package
 * @see java.lang.ERROR  Bad
 * @see java.lang#ERROR  Bad
 *
 * @see java.lang.String Class
 * @see           String Class
 * @see java.lang.String#CASE_INSENSITIVE_ORDER Field
 * @see java.lang.String#String Constructor
 * @see java.lang.String#String(byte[]) Constructor
 * @see java.lang.String#String(byte[] bytes) Constructor
 * @see java.lang.String#String(byte[],String) Constructor
 * @see java.lang.String#String(byte[] bytes, String charsetName) Constructor
 * @see java.lang.String#isEmpty Method
 * @see java.lang.String#isEmpty() Method
 * @see java.lang.String#ERROR Bad
 * @see java.lang.String#equals(Object) Method
 *
 * @see AbstractProcessor Class
 *
 * @see List#add(Object) Method
 *
 * @see #trees Field
 * @see #getSupportedSourceVersion Method
 * @see #init(ProcessingEnvironment) Method
 *
 * @see java.io.BufferedInputStream#BufferedInputStream(InputStream) Constructor
 */
@SupportedAnnotationTypes("*")
public class ReferenceTest extends AbstractProcessor {
    DocTrees trees;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public void init(ProcessingEnvironment pEnv) {
        super.init(pEnv);
        trees = DocTrees.instance(pEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e: roundEnv.getRootElements()) {
            new DocCommentScanner(trees.getPath(e)).scan();
        }
        return true;
    }

    class DocCommentScanner extends DocTreePathScanner<Void, Void> {
        TreePath path;
        DocCommentTree dc;

        DocCommentScanner(TreePath path) {
            this.path = path;
        }

        void scan() {
            dc = trees.getDocCommentTree(path);
            scan(new DocTreePath(path, dc), null);
        }

        @Override
        public Void visitLink(LinkTree tree, Void ignore) {
            checkReference(tree.getReference(), tree.getLabel());
            return null;
        }

        @Override
        public Void visitSee(SeeTree tree, Void ignore) {
            List<? extends DocTree> refLabel = tree.getReference();
            if (refLabel.size() > 1 && (refLabel.get(0) instanceof ReferenceTree)) {
                ReferenceTree ref = (ReferenceTree) refLabel.get(0);
                List<? extends DocTree> label = refLabel.subList(1, refLabel.size());
                checkReference(ref, label);
            }
            return null;
        }

        void checkReference(ReferenceTree tree, List<? extends DocTree> label) {
            String sig = tree.getSignature();

            Element found = trees.getElement(new DocTreePath(getCurrentPath(), tree));
            if (found == null) {
                System.err.println(sig + " NOT FOUND");
            } else {
                System.err.println(sig + " found " + found.getKind() + " " + found);
            }

            String expect = "UNKNOWN";
            if (label.size() > 0 && label.get(0) instanceof TextTree)
                expect = ((TextTree) label.get(0)).getBody();

            if (!expect.equalsIgnoreCase(found == null ? "bad" : found.getKind().name())) {
                error(tree, "Unexpected value found: " + found +", expected: " + expect);
            }
        }

        void error(DocTree tree, String msg) {
            trees.printMessage(Kind.ERROR, msg, tree, dc, path.getCompilationUnit());
        }
    }
}

/**
 * @see ReferenceTestExtras    Class
 * @see #ReferenceTestExtras   Field
 * @see #ReferenceTestExtras() Constructor
 *
 * @see #X    Field
 * @see #X()  Method
 *
 * @see #m    Method
 *
 * @see #varargs(int...)        Method
 * @see #varargs(int... args)   Method
 * @see #varargs(int[])         Method
 * @see #varargs(int[] args)    Method
 */
class ReferenceTestExtras {
    int ReferenceTestExtras;            // field
    ReferenceTestExtras() { }           // constructor
    void ReferenceTestExtras() { }      // method

    int X;
    void X() { }
    static class X { }

    void m() { }
    void m(int i) { }
    void m(int i, int j) { }

    void varargs(int... args) { }
}


