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

/*
 * @test
 * @bug 8266856
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 *          jdk.javadoc/jdk.javadoc.internal.doclets.formats.html.markup
 * @run main TestVoidHtmlElements
 */

import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclint.HtmlTag;

public class TestVoidHtmlElements {

    public static void main(String[] args) {
        int checks = 0;

        // For tags that are represented as both an HtmlTag and a TagName,
        // check that the definition of void-ness is the same.
        for (HtmlTag htmlTag : HtmlTag.values()) {
            try {
                TagName tagName = TagName.valueOf(htmlTag.name());
                checks++;
                check(htmlTag, tagName);
            } catch (IllegalArgumentException e) {
                // no matching TagName
            }
        }

        if (checks == 0) { // suspicious
            throw new AssertionError();
        }
        System.out.println(checks + " checks passed");
    }

    private static void check(HtmlTag htmlTag, TagName tagName) {
        boolean elementIsVoid = new HtmlTree(tagName).isVoid();
        boolean elementHasNoEndTag = htmlTag.endKind == HtmlTag.EndKind.NONE;
        if (elementIsVoid != elementHasNoEndTag) {
            throw new AssertionError(htmlTag + ", " + elementIsVoid + ", " + elementHasNoEndTag);
        }
    }
}
