/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8025113
 * @author  sogoel
 * @summary Test shadowing of resource variable
 */

/*
 * "...a variable declared in a resource specification
 * may be shadowed (6.3.1) anywhere inside a class declaration nested
 * within the Block of the try."
 */
public class ResourceShadow {

    static final String str = "asdf";  //this is okay

    /**
     * Resource variable shadows switch and case variables
     */
    String test1() {
        String ret = null;
        switch (str) {
            case str: //this is okay
                try (SilentCloseable str = new SilentCloseable()) {
                    SilentCloseable tr = new SilentCloseable(str);
                    ret = str.getClass().getSimpleName();
                }
                break;
            default:
                ret = "";
        }
        return ret;
    }

    /**
     * Resource variable may be shadowed (6.3.1) anywhere inside a class
     * declaration nested within the Block of the try
     */
    String test2() {
        String ret = null;
        try (SilentCloseable str = new SilentCloseable()) {
            class temp {

                String str = "I am not a SilentCloseable";

                public void printSTR() {
                    System.out.println(str);
                }

                public String getSTR() {
                    return str;
                }
            }
            temp tmp = new temp();
            SilentCloseable tr = new SilentCloseable(tmp.getSTR());
            ret = tr.getMsg();
        }
        return ret;
    }

    public static void main(String... args) {
        ResourceShadow t = new ResourceShadow();
        if (t.test1().compareTo("SilentCloseable") != 0) {
            throw new RuntimeException("FAIL-test1");
        }
        if (t.test2().compareTo("I am not a SilentCloseable") != 0) {
            throw new RuntimeException("FAIL-test2");
        }
    }
}

class SilentCloseable implements AutoCloseable {

    SilentCloseable testres = null;
    String msg = "default";

    @Override
    public void close() {
    }

    public SilentCloseable() {
    }

    public SilentCloseable(String s) {
        msg = s;
    }

    public SilentCloseable(SilentCloseable tr) {
        testres = tr;
    }

    public String getMsg() {
        return msg;
    }
}

