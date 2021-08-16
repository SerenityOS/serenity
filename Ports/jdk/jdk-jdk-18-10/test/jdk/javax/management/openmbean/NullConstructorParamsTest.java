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
 * @bug 6434298
 * @summary Test IAE is thrown when typeName or description parameter is null for TabularType and CompositeType constructors
 * @author Joel FERAUD
 */

import javax.management.openmbean.*;

public class NullConstructorParamsTest {

    /**
     * Print message
     */
    private static void echo(String message) {
        System.out.println(message);
    }

    /**
     * Main
     */
    public static void main(String[] args) throws Exception {

        echo("SUMMARY: Test IAE is thrown when typeName or description parameter is null " +
             "for TabularType and CompositeType constructors");

        echo(">>> Create CompositeType with non null params: should be ok");
        CompositeType ctok =
            new CompositeType(
                              "typeNameOK",
                              "for test",
                              new String[] {"a", "b"},
                              new String[] {"a_desc", "b_desc"},
                              new OpenType[] {SimpleType.BOOLEAN,SimpleType.STRING});
        echo("+++ CompositeType created ok");
        echo("");

        echo(">>> Create TabularType with non null params: should be ok");
        TabularType tabok =
            new TabularType(
                            "typeNameOK",
                            "for test",
                            ctok,
                            new String[] {"a"});
        echo("+++ TabularType created ok");
        echo("");


        int IAENotThrown = 0;

        try {
            echo(">>> Create CompositeType with null typeName: expect IAE");
            CompositeType ctnull1 =
                new CompositeType(
                                  null,
                                  "for test",
                                  new String[] {"a", "b"},
                                  new String[] {"a_desc", "b_desc"},
                                  new OpenType[] {SimpleType.BOOLEAN, SimpleType.STRING});
            IAENotThrown++;
            echo("*** IAE not thrown as expected ***");
            echo("*** Test will FAIL ***");
            echo("");
        } catch (IllegalArgumentException iae) {
            echo("+++ IAE thrown as expected");
            echo("");
        } catch (Exception e) {
            IAENotThrown++;
            echo("*** Did not get IAE as expected, but instead: ");
            e.printStackTrace();
            echo("*** Test will FAIL ***");
            echo("");
        }

        try {
            echo(">>> Create TabularType with null typeName: expect IAE");
            TabularType tabnull1 =
                new TabularType(
                                null,
                                "for test",
                                ctok,
                                new String[] {"a"});
            IAENotThrown++;
            echo("*** IAE not thrown as expected ***");
            echo("*** Test will FAIL ***");
            echo("");
        } catch (IllegalArgumentException iae) {
            echo("+++ IAE thrown as expected");
            echo("");
        } catch (Exception e) {
            IAENotThrown++;
            echo("*** Did not get IAE as expected, but instead: ");
            e.printStackTrace();
            echo("*** Test will FAIL ***");
            echo("");
        }

        try {
            echo(">>> Create CompositeType with null description: expect IAE");
            CompositeType ctnull2 =
                new CompositeType(
                                  "test",
                                  null,
                                  new String[] {"a", "b"},
                                  new String[] {"a_desc", "b_desc"},
                                  new OpenType[] {SimpleType.BOOLEAN, SimpleType.STRING});
            IAENotThrown++;
            echo("*** IAE not thrown as expected ***");
            echo("*** Test will FAIL ***");
            echo("");
        } catch (IllegalArgumentException iae) {
            echo("+++ IAE thrown as expected");
            echo("");
        } catch (Exception e) {
            IAENotThrown++;
            echo("*** Did not get IAE as expected, but instead: ");
            e.printStackTrace();
            echo("*** Test will FAIL ***");
            echo("");
        }

        try {
            echo(">>> Create TabularType with null description: expect IAE");
            TabularType tabnull2 =
                new TabularType(
                                "test",
                                null,
                                ctok,
                                new String[] {"a"});
            IAENotThrown++;
            echo("*** IAE not thrown as expected ***");
            echo("*** Test will FAIL ***");
            echo("");
        } catch (IllegalArgumentException iae) {
            echo("+++ IAE thrown as expected");
            echo("");
        } catch (Exception e) {
            IAENotThrown++;
            echo("*** Did not get IAE as expected, but instead: ");
            e.printStackTrace();
            echo("*** Test will FAIL ***");
            echo("");
        }

        if (IAENotThrown != 0 ) {
            echo("*** Test FAILED: IAE not thrown as expected ***");
            echo("");
            throw new RuntimeException("IAE not thrown as expected");
        }
        echo("+++ Test PASSED");
        echo("");

    }
}
