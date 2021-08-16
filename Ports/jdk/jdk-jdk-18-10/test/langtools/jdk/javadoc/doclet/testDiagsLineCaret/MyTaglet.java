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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EntityTree;
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
 * A taglet that writes messages to the doclet's reporter.
 */
public class MyTaglet implements Taglet {
    private DocletEnvironment docEnv;
    private Reporter reporter;

    @Override
    public void init(DocletEnvironment env, Doclet doclet) {
        Taglet.super.init(env, doclet);
        docEnv = env;

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
    public boolean isBlockTag() {
        return true;
    }

    /**
     * Refines an existing tag ({@code @since} that provides a {@code List<DocTree>},
     * so that we can better test positions within the tree node. The alternative,
     * defining a new tag, would use {@code UnknownBlockTagTree} which just provides
     * a single {@code Doc Tree}.
     *
     * @return the name of the tag supported by this taglet
     */
    @Override
    public String getName() {
        return "since";
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        List<Diagnostic.Kind> kinds = Arrays.stream(Diagnostic.Kind.values())
                .filter(k -> k != Diagnostic.Kind.OTHER)
                .toList();

        for (Diagnostic.Kind k : kinds) {
            String message = "This is a " + k.toString().toLowerCase(Locale.ROOT);
            reporter.print(k, message);
        }

        for (Diagnostic.Kind k : kinds) {
            String message = "This is a " + k.toString().toLowerCase(Locale.ROOT) + " for " + element;
            reporter.print(k, element, message);
        }

        DocTreePathScanner<Void, Diagnostic.Kind> s = new DocTreePathScanner<>() {
            @Override
            public Void scan(DocTree tree, Diagnostic.Kind k) {
                return super.scan(tree, k);
            }

            @Override
            public Void visitEntity(EntityTree node, Diagnostic.Kind k) {
                if (node.getName().contentEquals("#x1f955")) {
                    String message = "This is a " + k.toString().toLowerCase(Locale.ROOT)
                            + ": this is not a caret";
                    reporter.print(k, getCurrentPath(), message);
                }
                return super.visitEntity(node, k);
            }
        };

        DocTrees trees = docEnv.getDocTrees();
        TreePath tp = trees.getPath(element);
        DocTreePath root = new DocTreePath(tp, trees.getDocCommentTree(element));

        for (Diagnostic.Kind k : kinds) {
            tags.forEach(t -> s.scan(new DocTreePath(root, t), k));
        }
        return "<b>mytaglet output</b>";
    }
}