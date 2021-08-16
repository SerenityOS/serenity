/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4108453 4778440 6304780 6396378 8263202
 * @summary Basic tests for java.awt.ComponentOrientation
 * @build TestBundle TestBundle_es TestBundle_iw
 * @build TestBundle1 TestBundle1_ar
 *
 * @run main BasicTest
 */
/*
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation is copyrighted
 * and owned by IBM, Inc. These materials are provided under terms of a
 * License Agreement between IBM and Sun. This technology is protected by
 * multiple US and International patents. This notice and attribution to IBM
 * may not be removed.
 */

import java.awt.ComponentOrientation;
import java.util.Locale;
import java.util.ResourceBundle;

public class BasicTest {
    public static void main(String args[]) {
        System.out.println("BasicTest {");
        TestInvariants();
        TestLocale();
        TestBundle();

        System.out.println("} Pass");
    }

    // TestInvariants
    //
    // Various no-brainer tests to make sure the constants behave properly
    // and so on.
    //
    static void TestInvariants() {
        System.out.println("  TestInvariants {");

        Assert(ComponentOrientation.LEFT_TO_RIGHT.isLeftToRight(),
               "LEFT_TO_RIGHT.isLeftToRight()");

        Assert(ComponentOrientation.UNKNOWN.isLeftToRight(),
               "UNKNOWN.isLeftToRight()");

        Assert(!ComponentOrientation.RIGHT_TO_LEFT.isLeftToRight(),
               "!RIGHT_TO_LEFT.isLeftToRight()");

        Assert(ComponentOrientation.LEFT_TO_RIGHT.isHorizontal(),
               "LEFT_TO_RIGHT.isHorizontal()");

        Assert(ComponentOrientation.UNKNOWN.isHorizontal(),
               "UNKNOWN.isHorizontal()");

        Assert(ComponentOrientation.RIGHT_TO_LEFT.isHorizontal(),
               "RIGHT_TO_LEFT.isHorizontal()");

        System.out.println("  } Pass");
    }

    // TestLocale
    //
    // Make sure that getOrientation(Locale) works, and that the appropriate
    // system locales are RIGHT_TO_LEFT
    //
    static void TestLocale() {
        System.out.println("  TestLocale {");

        ComponentOrientation orient = ComponentOrientation.getOrientation(Locale.US);
        Assert(orient == ComponentOrientation.LEFT_TO_RIGHT, "US == LEFT_TO_RIGHT");

        orient = ComponentOrientation.getOrientation(new Locale("iw", ""));
        Assert(orient == ComponentOrientation.RIGHT_TO_LEFT, "iw == RIGHT_TO_LEFT");

        orient = ComponentOrientation.getOrientation(new Locale("ar", ""));
        Assert(orient == ComponentOrientation.RIGHT_TO_LEFT, "ar == RIGHT_TO_LEFT");

        orient = ComponentOrientation.getOrientation(new Locale("he", ""));
        Assert(orient == ComponentOrientation.RIGHT_TO_LEFT, "he == RIGHT_TO_LEFT");

        orient = ComponentOrientation.getOrientation(new Locale("yi", ""));
        Assert(orient == ComponentOrientation.RIGHT_TO_LEFT, "yi == RIGHT_TO_LEFT");

        System.out.println("  } Pass");
    }

    // TestBundle
    //
    // Make sure that getOrientation(ResourceBundle) works right, especially
    // the fallback mechasm
    //
    static void TestBundle() {
        System.out.println("  TestBundle {");

        // This will fall back to the default locale's bundle or root bundle
        ResourceBundle rb = ResourceBundle.getBundle("TestBundle",
                                                        new Locale("et", ""));
        if (rb.getLocale().getLanguage().equals(new Locale("iw").getLanguage())) {
            assertEquals(rb, ComponentOrientation.RIGHT_TO_LEFT, "et == RIGHT_TO_LEFT" );
        } else if (rb.getLocale().getLanguage() == "es") {
            assertEquals(rb, ComponentOrientation.LEFT_TO_RIGHT, "et == LEFT_TO_RIGHT" );
        } else {
            assertEquals(rb, ComponentOrientation.UNKNOWN, "et == UNKNOWN" );
        }

        // We have actual bundles for "es" and "iw", so it should just fetch
        // the orientation object out of them
        rb = ResourceBundle.getBundle("TestBundle",new Locale("es", ""));
        assertEquals(rb, ComponentOrientation.LEFT_TO_RIGHT, "es == LEFT_TO_RIGHT" );

        rb = ResourceBundle.getBundle("TestBundle", new Locale("iw", "IL"));
        assertEquals(rb, ComponentOrientation.RIGHT_TO_LEFT, "iw == RIGHT_TO_LEFT" );

        // Test with "he" locale. This should load TestBundle_iw and fetch the orientation from there
        rb = ResourceBundle.getBundle("TestBundle", new Locale("he", "IL"));
        assertEquals(rb, ComponentOrientation.RIGHT_TO_LEFT, "he == RIGHT_TO_LEFT" );

        // This bundle has no orientation setting at all, so we should get
        // the system's default orientation for Arabic
        rb = ResourceBundle.getBundle("TestBundle1", new Locale("ar", ""));
        assertEquals(rb, ComponentOrientation.RIGHT_TO_LEFT, "ar == RIGHT_TO_LEFT" );

        System.out.println("  } Pass");
    }

    static void assertEquals(ResourceBundle rb, ComponentOrientation o, String str) {
        Assert(ComponentOrientation.getOrientation(rb) == o, str);
    }

    static void Assert(boolean condition, String str) {
        if (!condition) {
            System.err.println("    ASSERT FAILED: " + str);
            throw new RuntimeException("Assert Failed: " + str);
        }
    }
}
