/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * NOTE: this class is an almost a replica of the example used in the
 * package-info.java.
 */
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;

import jdk.javadoc.doclet.*;

/**
 * An Example class implementing the Doclet.
 */
public class Example implements Doclet {

    Reporter reporter;

    @Override
    public void init(Locale locale, Reporter reporter) {
        reporter.print(Kind.NOTE, "Doclet using locale: " + locale);
        this.reporter = reporter;
    }

    /**
     * Prints an element.
     *
     * @param trees the utility class
     * @param e the element to be printed
     */
    public void printElement(DocTrees trees, Element e) {
        DocCommentTree docCommentTree = trees.getDocCommentTree(e);
        if (docCommentTree != null) {
            System.out.println("Element (" + e.getKind() + ": "
                    + e + ") has the following comments:");
            System.out.println("Entire body: " + docCommentTree.getFullBody());
            System.out.println("Block tags: " + docCommentTree.getBlockTags());
        }
    }

    @Override
    public boolean run(DocletEnvironment docEnv) {
        reporter.print(Kind.NOTE, "overviewfile: " + overviewfile);
        // get the DocTrees utility class to access DocComments
        DocTrees docTrees = docEnv.getDocTrees();

        // location of an element in the same directory as overview.html
        try {
            Element e = ElementFilter.typesIn(docEnv.getSpecifiedElements()).iterator().next();
            DocCommentTree docCommentTree
                    = docTrees.getDocCommentTree(e, overviewfile);
            if (docCommentTree != null) {
                System.out.println("Overview html: " + docCommentTree.getFullBody());
            }
        } catch (IOException missing) {
            reporter.print(Kind.ERROR, "No overview.html found.");
        }

        for (TypeElement t : ElementFilter.typesIn(docEnv.getIncludedElements())) {
            System.out.println(t.getKind() + ":" + t);
            for (Element e : t.getEnclosedElements()) {
                printElement(docTrees, e);
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "Example";
    }

    private String overviewfile;

    @Override
    public Set<? extends Option> getSupportedOptions() {
        Option[] options = {
            new Option() {
                private final List<String> someOption = Arrays.asList(
                        "-overviewfile",
                        "-overview-file",
                        "--over-view-file"
                );

                @Override
                public int getArgumentCount() {
                    return 1;
                }

                @Override
                public String getDescription() {
                    return "an option with aliases";
                }

                @Override
                public Option.Kind getKind() {
                    return Option.Kind.STANDARD;
                }

                @Override
                public List<String> getNames() {
                    return someOption;
                }

                @Override
                public String getParameters() {
                    return "file";
                }

                @Override
                public boolean process(String opt, List<String> arguments) {
                    overviewfile = arguments.get(0);
                    return true;
                }
            }
        };
        return new HashSet<>(Arrays.asList(options));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // support the latest release
        return SourceVersion.latest();
    }
}
