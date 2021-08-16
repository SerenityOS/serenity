/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006263
 * @summary Supplementary test cases needed for doclint
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 */

import java.util.Objects;

import jdk.javadoc.internal.doclint.Checker;
import jdk.javadoc.internal.doclint.HtmlTag;
import jdk.javadoc.internal.doclint.Messages;

public class CoverageExtras {
    public static void main(String... args) {
        new CoverageExtras().run();
    }

    void run() {
        check(HtmlTag.A, HtmlTag.valueOf("A"), HtmlTag.values());
        check(HtmlTag.Attr.ABBR, HtmlTag.Attr.valueOf("ABBR"), HtmlTag.Attr.values());
        check(HtmlTag.AttrKind.INVALID, HtmlTag.AttrKind.valueOf("INVALID"), HtmlTag.AttrKind.values());
        check(HtmlTag.BlockType.BLOCK, HtmlTag.BlockType.valueOf("BLOCK"), HtmlTag.BlockType.values());
        check(HtmlTag.EndKind.NONE, HtmlTag.EndKind.valueOf("NONE"), HtmlTag.EndKind.values());
        check(HtmlTag.Flag.EXPECT_CONTENT, HtmlTag.Flag.valueOf("EXPECT_CONTENT"), HtmlTag.Flag.values());

        check(Checker.Flag.TABLE_HAS_CAPTION, Checker.Flag.valueOf("TABLE_HAS_CAPTION"), Checker.Flag.values());

        check(Messages.Group.ACCESSIBILITY, Messages.Group.valueOf("ACCESSIBILITY"), Messages.Group.values());
    }

    <T extends Enum<T>> void check(T expect, T value, T[] values) {
        if (!Objects.equals(expect, value)) {
            error("Mismatch: '" + expect + "', '" + value + "'");
        }
        if (!Objects.equals(expect, values[0])) {
            error("Mismatch: '" + expect + "', '" + values[0] + "'");
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
