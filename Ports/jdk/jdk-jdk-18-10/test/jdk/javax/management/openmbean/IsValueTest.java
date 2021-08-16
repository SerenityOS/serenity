/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5072004
 * @summary Test new rules for isValue
 * @author Eamonn McManus
 */

import javax.management.openmbean.*;

public class IsValueTest {
    private static String failed;

    public static void main(String[] args) throws Exception {
        CompositeType ctOld =
            new CompositeType("same.type.name", "old",
                new String[] {"int", "string"},
                new String[] {"an int", "a string"},
                new OpenType[] {SimpleType.INTEGER, SimpleType.STRING});
        CompositeType ctNew =
            new CompositeType("same.type.name", "new",
                new String[] {"int", "int2", "string"},
                new String[] {"an int", "another int", "a string"},
                new OpenType[] {SimpleType.INTEGER, SimpleType.INTEGER, SimpleType.STRING});
        CompositeData cdOld =
            new CompositeDataSupport(ctOld,
                new String[] {"string", "int"},
                new Object[] {"bar", 17});
        CompositeData cdNew =
            new CompositeDataSupport(ctNew,
                new String[] {"int2", "int", "string"},
                new Object[] {4, 3, "foo"});

        // Check that adding fields doesn't make isValue return false
        check(ctOld.isValue(cdNew), "isValue: " + ctOld + "[" + cdNew + "]");

        // Check that removing fields does make isValue return false
        check(!ctNew.isValue(cdOld), "isValue: " + ctNew + "[" + cdOld + "]");

        // Check that we can add a contained CompositeData with extra fields
        // inside another CompositeData
        CompositeType ctWrapOld =
            new CompositeType("wrapper", "wrapper",
                new String[] {"wrapped"},
                new String[] {"wrapped"},
                new OpenType[] {ctOld});
        try {
            new CompositeDataSupport(ctWrapOld,
                new String[] {"wrapped"},
                new Object[] {cdNew});
            check(true, "CompositeDataSupport containing CompositeDataSupport");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            check(false, "CompositeDataSupport containing CompositeDataSupport: " + e);
        }

        // ...but not the contrary
        CompositeType ctWrapNew =
            new CompositeType("wrapper", "wrapper",
                new String[] {"wrapped"},
                new String[] {"wrapped"},
                new OpenType[] {ctNew});
        try {
            new CompositeDataSupport(ctWrapNew,
                new String[] {"wrapped"},
                new Object[] {cdOld});
            check(false, "CompositeDataSupport containing old did not get exception");
        } catch (OpenDataException e) {
            check(true, "CompositeDataSupport containing old got expected exception: " + e);
        }

        // Check that a TabularData can get an extended CompositeData row
        TabularType ttOld =
            new TabularType("tabular", "tabular", ctOld, new String[] {"int"});
        TabularData tdOld =
            new TabularDataSupport(ttOld);
        try {
            tdOld.put(cdNew);
            check(true, "TabularDataSupport adding extended CompositeData");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            check(false, "TabularDataSupport adding extended CompositeData: " + e);
        }

        // Check that an extended TabularData can be put into a CompositeData
        TabularType ttNew =
            new TabularType("tabular", "tabular", ctNew, new String[] {"int"});
        TabularData tdNew =
            new TabularDataSupport(ttNew);
        CompositeType cttWrap =
            new CompositeType("wrapTT", "wrapTT",
                new String[] {"wrapped"},
                new String[] {"wrapped"},
                new OpenType[] {ttOld});
        try {
            new CompositeDataSupport(cttWrap,
                new String[] {"wrapped"},
                new Object[] {tdNew});
            check(true, "CompositeDataSupport adding extended TabularData");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            check(false, "CompositeDataSupport adding extended TabularData: " + e);
        }

        if (failed != null)
            throw new Exception("TEST FAILED: " + failed);
    }

    private static void check(boolean value, String what) {
        if (value)
            System.out.println("OK: " + what);
        else {
            failed = what;
            System.out.println("FAILED: " + what);
        }
    }
}
