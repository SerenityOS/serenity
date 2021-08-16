/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTypeTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.StandardDoclet;
import jdk.javadoc.doclet.Taglet;

/**
 * A taglet to be called in the context of the standard doclet.
 * When invoked, it scans the entire enclosing doc comment, and
 * reports diagnostics at all instances of selected node kinds,
 * so that a test can verify the contents of the diagnostics.
 */
public class MyTaglet implements Taglet {
    private DocletEnvironment docEnv;
    private Reporter reporter;

    @Override
    public void init(DocletEnvironment docEnv, Doclet doclet) {
        this.docEnv = docEnv;
        reporter = ((StandardDoclet) doclet).getReporter();
    }

    @Override
    public Set<Location> getAllowedLocations() {
        return EnumSet.allOf(Location.class);
    }

    @Override
    public boolean isInlineTag() {
        return false;
    }

    @Override
    public String getName() {
        return "scanMe";
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        DocTrees trees = docEnv.getDocTrees();
        Elements elements = docEnv.getElementUtils();
        DocTreePath dtp;
        // Use reflection to access the file object underlying a doc-files/*.html file
        // in order to access a DocCommentTree for the file.
        // Note that using reflective access inside javadoc requires the -XDallowInternalAccess option.
        // Note also that technically the doc comment tree that is found may be a different instance
        // to the current instance, but since we only want to scan it and report diagnostics,
        // that should not matter.
        if (element.getClass().getSimpleName().equals("DocFileElement")) {
            try {
                Method getFileObjectMethod = element.getClass().getMethod("getFileObject");
                FileObject fo = (FileObject) getFileObjectMethod.invoke(element);
                DocCommentTree dct = trees.getDocCommentTree(fo);
                dtp = trees.getDocTreePath(fo, elements.getPackageElement("p"));
            } catch (ReflectiveOperationException e) {
                return "MyTaglet[" + e + "]";
            }
        } else {
            DocCommentTree dct = trees.getDocCommentTree(element);
            TreePath tp = trees.getPath(element);
            dtp = new DocTreePath(tp, dct);
        }

        scan(dtp);

        return "MyTaglet[" + element + "]";
    }

    /**
     * Scans a DocCommentTree, generating diagnostics for selected nodes.
     * Information about the expected position is encoded within the
     * text of the diagnostic, surrounded by {@code >>> <<<}.
     *
     * @param dtp the path to scan
     */
    void scan(DocTreePath dtp) {
        DocTreePathScanner<Void, Void> s = new DocTreePathScanner<Void, Void>() {
            @Override
            public Void visitDocComment(DocCommentTree t, Void p) {
                // By default, DocTreeScanner does not include the preamble and postamble
                scan(t.getPreamble(), p);
                super.visitDocComment(t, p);
                scan(t.getPostamble(), p);
                return null;
            }

            @Override
            public Void visitComment(CommentTree t, Void p) {
                report(t, t.getBody());
                return super.visitComment(t, p);
            }

            @Override
            public Void visitDocType(DocTypeTree t, Void p) {
                report(t, t.getText());
                return super.visitDocType(t, p);
            }

            @Override
            public Void visitReference(ReferenceTree t, Void p) {
                report(t, t.getSignature());
                return super.visitReference(t, p);
            }

            @Override
            public Void visitText(TextTree t, Void p) {
                report(t, t.getBody());
                return super.visitText(t, p);
            }

            void report(DocTree t, String s) {
                int pad = 3;
                assert (s.length() > 2 * pad + 3) : ">>>" + s + "<<<";
                int mid = s.length() / 2;
                String detail = s.substring(mid - pad, mid) + "[" + s.charAt(mid) + "]" + s.substring(mid + 1, mid + pad + 1);
                // The diagnostic is reported at a position in a range of characters
                // in the middle of the string; the characters are encoded within the
                // message of the diagnostic, with {@code [ ]} surrounding the character
                // that should be indicated by the caret.
                reporter.print(Diagnostic.Kind.WARNING, getCurrentPath(),
                        mid - pad, mid, mid + pad + 1,
                        "length: " + s.length() + " mid: " + mid + " >>>" + detail + "<<<");
            }
        };
        s.scan(dtp, null);
    }
}
