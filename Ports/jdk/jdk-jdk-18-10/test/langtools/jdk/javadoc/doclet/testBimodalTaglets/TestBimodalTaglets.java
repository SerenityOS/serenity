/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8202947
 * @summary  Test bimodal (inline and block) taglets
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main TestBimodalTaglets
 */

import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import javadoc.tester.JavadocTester;
import jdk.javadoc.doclet.Taglet;
import toolbox.ToolBox;

public class TestBimodalTaglets extends JavadocTester implements Taglet {
    public static void main(String... args) throws Exception {
        new TestBimodalTaglets().runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    ToolBox tb = new ToolBox();

    @Test
    public void testBimodalTaglet(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p;\n"
                + comment("This is a comment.",
                        "Here is an inline {@test abc} test tag {@test def}.",
                        "@see Object",
                        "@test 123",
                        "@see String",
                        "@test 456")
                + "public class C { }\n");

        javadoc("-d", base.resolve("out").toString(),
                "--source-path", src.toString(),
                "-tagletpath", System.getProperty("test.class.path"),
                "-taglet", "TestBimodalTaglets",
                "p");

        checkOutput("p/C.html", true,
                "Here is an inline INLINE[abc] test tag INLINE[def].",
                "<dt>BLOCK:<dd>123<dd>456");
    }

    String comment(String... lines) {
        return Arrays.stream(lines)
                .collect(Collectors.joining("\n * ", "/**\n * ", "\n */"));
    }

    // the taglet ....

    @Override
    public Set<Location> getAllowedLocations() {
        return EnumSet.allOf(Location.class);
    }

    @Override
    public boolean isInlineTag() {
        return true;
    }

    @Override
    public boolean isBlockTag() {
        return true;
    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        if (tags.size() == 1 && tags.get(0) instanceof UnknownInlineTagTree) {
            return inlineTagToString((UnknownInlineTagTree) tags.get(0));
        } else {
            return blockTagsToString(tags);
        }
    }

    /**
     * Converts an inline tag to a string composed of its contents wrapped in "INLINE[" ... "]"
     *
     * @param tag the tag
     * @return the string
     */
    private String inlineTagToString(UnknownInlineTagTree tag) {
        return "INLINE[" +
                toString(tag.getContent())
                + "]";
    }

    /**
     * Converts a series of block tags to a string composed of a {@code> <dt>} header,
     * followed by the contents of each tag preceded by {@code <dd>}.
     * Note that the doclet provides the enclosing {@code <dl>...</dl>} around all the
     * block tags.
     *
     * @param tags the tags
     * @return the string
     */
    private String blockTagsToString(List<? extends DocTree> tags) {
        return "<dt>BLOCK:"
                + tags.stream()
                    .map (t -> (UnknownBlockTagTree) t)
                    .map(t -> "<dd>" + toString(t.getContent()))
                    .collect(Collectors.joining());
    }

    private String toString(List<? extends DocTree> trees) {
        return trees.stream()
                .map(Object::toString)
                .collect(Collectors.joining());
    }
}

