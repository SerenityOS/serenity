/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5072174
 * @summary Test CompositeDataSupport.equals with ArrayType
 * @author Shanliang JIANG
 *
 * @run clean EqualsTest
 * @run build EqualsTest
 * @run main EqualsTest
 */

import javax.management.ObjectName;
import javax.management.openmbean.*;

public class EqualsTest {

    /*
     * Print message
     */
    private static void echo(String message) {
        System.out.println(message);
    }

    /**
     * Main
     */
    public static void main(String[] args) throws Exception {
        echo("=-=-= Test CompositeDataSupport.equals() with ArrayType =-=-=");

        echo(">>> Two SimpleTypes with different descriptions");
        CompositeType ct1 = new CompositeType(
                    "MyType",
                    "for test",
                    new String[] {"a", "b"},
                    new String[] {"a_desc", "b_desc"},
                    new OpenType[] {SimpleType.BOOLEAN,SimpleType.STRING});

        CompositeType ct2 = new CompositeType(
                    "MyType",
                    "for test",
                    new String[] {"a", "b"},
                    new String[] {"aa_desc", "bb_desc"},
                    new OpenType[] {SimpleType.BOOLEAN, SimpleType.STRING});

        if (!ct1.equals(ct2)) {
            throw new RuntimeException("CompositeType.equals fails!");
        }
        if (ct1.hashCode() != ct2.hashCode()) {
            throw new RuntimeException("CompositeType.hashCode fails!");
        }

        echo(">>> Two SimpleTypes with equal values");
        CompositeData compositeData0 = new CompositeDataSupport(
                    ct1,
                    new String[] {"a", "b"},
                    new Object[] {new Boolean(true), ""});

        CompositeData compositeData1 = new CompositeDataSupport(
                    ct2,
                    new String[] {"a", "b"},
                    new Object[] {new Boolean(true), ""});

        if (!compositeData0.equals(compositeData1)) {
            throw new RuntimeException("CompositeDataSupport.equals fails!");
        }
        if (compositeData0.hashCode() != compositeData1.hashCode()) {
            throw new RuntimeException("CompositeDataSupport.hashCode fails!");
        }

        echo(">>> Two ArrayTypes with different references");
        CompositeType ct3 = new CompositeType(
                    "MyType",
                    "for test",
                    new String[] {"a"},
                    new String[] {"a_desc"},
                    new OpenType[] {new ArrayType(1, SimpleType.STRING)});

        CompositeData compositeData2 = new CompositeDataSupport(
                    ct3,
                    new String[] {"a"},
                    new Object[] {new String[] {"x", "y"}});

        CompositeData compositeData3 = new CompositeDataSupport(
                    ct3,
                    new String[] {"a"},
                    new Object[] {new String[] {"x", "y"}});

        if (!compositeData2.equals(compositeData3)) {
            throw new RuntimeException("CompositeDataSupport.equals fails!");
        }
        if (compositeData2.hashCode() != compositeData3.hashCode()) {
            throw new RuntimeException("CompositeDataSupport.hashCode fails!");
        }

        echo(">>> Two ArrayTypes with different values");
        CompositeData compositeData4 = new CompositeDataSupport(
                    ct3,
                    new String[] {"a"},
                    new Object[] {new String[] {"x", "y", "x"}});

        if (compositeData2.equals(compositeData4)) {
            throw new RuntimeException("CompositeDataSupport.equals fails!");
        }

        echo(">>> Two 2-dimension ArrayTypes with equal values");
        CompositeType ct4 = new CompositeType(
                    "MyType",
                    "for test",
                    new String[] {"a"},
                    new String[] {"a_desc"},
                    new OpenType[] {new ArrayType(2, SimpleType.OBJECTNAME)});

        final String s = "t:t=t";
        CompositeData compositeData5 = new CompositeDataSupport(
                    ct4,
                    new String[] {"a"},
                    new Object[] {
                        new ObjectName[][] {
                            new ObjectName[] {
                                new ObjectName(s), new ObjectName(s)
                            },
                            new ObjectName[] {
                                new ObjectName(s), new ObjectName(s)
                            }
                        }
                    });

        CompositeData compositeData6 = new CompositeDataSupport(
                    ct4,
                    new String[] {"a"},
                    new Object[] {
                        new ObjectName[][] {
                            new ObjectName[] {
                                new ObjectName(s), new ObjectName(s)
                            },
                            new ObjectName[] {
                                new ObjectName(s), new ObjectName(s)
                            }
                        }
                    });

        if (!compositeData5.equals(compositeData6)) {
            throw new RuntimeException("CompositeDataSupport.equals fails!");
        }
        if (compositeData5.hashCode() != compositeData6.hashCode()) {
            throw new RuntimeException("CompositeDataSupport.hashCode fails!");
        }

        echo(">>> Two primitive ArrayTypes with different descriptions");
        CompositeType ct5 = new CompositeType(
                    "MyType",
                    "for test",
                    new String[] {"a", "b"},
                    new String[] {"a_desc", "b_desc"},
                    new OpenType[] {
                        SimpleType.BOOLEAN,
                        ArrayType.getPrimitiveArrayType(short[].class)
                    });

        CompositeType ct6 = new CompositeType(
                    "MyType",
                    "for test",
                    new String[] {"a", "b"},
                    new String[] {"aa_desc", "bb_desc"},
                    new OpenType[] {
                        SimpleType.BOOLEAN,
                        ArrayType.getPrimitiveArrayType(short[].class)
                    });

        if (!ct5.equals(ct6)) {
            throw new RuntimeException("CompositeType.equals fails!");
        }
        if (ct5.hashCode() != ct6.hashCode()) {
            throw new RuntimeException("CompositeType.hashCode fails!");
        }

        echo(">>> Two primitive ArrayTypes with different references");
        CompositeType ct7 = new CompositeType(
                    "MyType",
                    "for test",
                    new String[] {"a"},
                    new String[] {"a_desc"},
                    new OpenType[] {
                        ArrayType.getPrimitiveArrayType(int[].class)
                    });

        CompositeData compositeData7 = new CompositeDataSupport(
                    ct7,
                    new String[] {"a"},
                    new Object[] {new int[] {1, 2}});

        CompositeData compositeData8 = new CompositeDataSupport(
                    ct7,
                    new String[] {"a"},
                    new Object[] {new int[] {1, 2}});

        if (!compositeData7.equals(compositeData8)) {
            throw new RuntimeException("CompositeDataSupport.equals fails!");
        }
        if (compositeData7.hashCode() != compositeData8.hashCode()) {
            throw new RuntimeException("CompositeDataSupport.hashCode fails!");
        }

        echo(">>> Two primitive ArrayTypes with different values");
        CompositeData compositeData9 = new CompositeDataSupport(
                    ct7,
                    new String[] {"a"},
                    new Object[] {new int[] {1, 2, 3}});

        if (compositeData7.equals(compositeData9)) {
            throw new RuntimeException("CompositeDataSupport.equals fails!");
        }

        echo(">>> Two 2-dimension primitive ArrayTypes with equal values");
        CompositeType ct8 = new CompositeType(
                    "MyType",
                    "for test",
                    new String[] {"a"},
                    new String[] {"a_desc"},
                    new OpenType[] {
                        ArrayType.getPrimitiveArrayType(boolean[][].class)
                    });

        CompositeData compositeData10 = new CompositeDataSupport(
                    ct8,
                    new String[] {"a"},
                    new Object[] {new boolean[][]
                          {new boolean[] {true, true},
                           new boolean[] {true, true}}});

        CompositeData compositeData11 = new CompositeDataSupport(
                    ct8,
                    new String[] {"a"},
                    new Object[] {new boolean[][]
                          {new boolean[] {true, true},
                           new boolean[] {true, true}}});

        if (!compositeData10.equals(compositeData11)) {
            throw new RuntimeException("CompositeDataSupport.equals fails!");
        }
        if (compositeData10.hashCode() != compositeData11.hashCode()) {
            throw new RuntimeException("CompositeDataSupport.hashCode fails!");
        }
    }
}
