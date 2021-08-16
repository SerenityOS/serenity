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
 * @summary Resource creation in nested expressions
 */

/**
 * This test checks for resource creation in nested expressions.
 * test1() - Create 3 resource in nested new expressions, style 1
 * test2() - Create 3 resource in nested new expressions, style 2
 * test3() - Create 4 resources with resources as parameters: new expression; typeid & new expression
 */

public class ResInNestedExpr {

    static final int expected = 5;
    static int closed = 0;

    static void closing(String clazz) {
        closed++;
    }

    static void checkClosedCount() {
        if (expected != closed) {
            throw new RuntimeException("Did not find enough closed resources."
               + "Expected " + expected + ", but found " + closed);
        }
    }
    /**
     * The "expected output" is each class name gotten with getSimpleName() to unclutter things.
     * Each test method returns a classname of the resource and that is compared with
     * values in this array.
     */
    static String[] expectedOutput = {
        "aResource::bResource::cResource", //test1
        "aResource::bResource::cResource&aResource::cResource", //test3
        "aResource::bResource::cResource&aResource::cResource"}; //test2

    static void compare(String s1, String s2) {
        if (s1.compareTo(s2) != 0) {
            throw new RuntimeException(s1 + "!=" + s2);
        }
    }

    String test1() {
        String ret = null;
        try (bResource br = new bResource(new cResource());
                aResource ar = new aResource(br)) {
            ret = ar.getClass().getSimpleName() + "::" +
                  ar.getB().getClass().getSimpleName() + "::" +
                  ar.getB().getC().getClass().getSimpleName();
        }
        return ret;
    }

    String test2() {
        String ret = null;
        try (aResource ar = new aResource(new bResource(new cResource()), new cResource())) {
            String abc = ar.getClass().getSimpleName() + "::" +
                         ar.getB().getClass().getSimpleName() + "::" +
                         ar.getB().getC().getClass().getSimpleName();
            String ac = ar.getClass().getSimpleName() + "::" +
                        ar.getC().getClass().getSimpleName();
            ret = abc + "&" + ac;
        }
        return ret;
    }

    String test3() {
        String ret = null;
        try (bResource br = new bResource(new cResource());
                aResource ar = new aResource(br, new cResource())) {
            String abc = ar.getClass().getSimpleName() + "::" +
                         ar.getB().getClass().getSimpleName() + "::" +
                         ar.getB().getC().getClass().getSimpleName();
            String ac = ar.getClass().getSimpleName() + "::" +
                        ar.getC().getClass().getSimpleName();
            ret = abc + "&" + ac;
        }
        return ret;
    }

    public static void main(String... args) {
        ResInNestedExpr t = new ResInNestedExpr();
        int eo = 0;
        compare(expectedOutput[eo++], t.test1());
        compare(expectedOutput[eo++], t.test3());
        compare(expectedOutput[eo++], t.test2());
        ResInNestedExpr.checkClosedCount();
    }

    /**
     * A resource to implement AutoCloseable
     * Contains two other resources as data items.
     */
    static class aResource implements AutoCloseable {

        bResource bR;
        cResource cR;

        public aResource() {
            bR = null;
            cR = null;
        }

        public aResource(bResource br) {
            bR = br;
        }

        public aResource(cResource cr) {
            cR = cr;
        }

        public aResource(bResource br, cResource cr) {
            bR = br;
            cR = cr;
        }

        public bResource getB() {
            return bR;
        }

        public cResource getC() {
            return cR;
        }

        @Override
        public void close() {
            ResInNestedExpr.closing(this.getClass().getName());
        }
    }

    /**
     * A resource to implement AutoCloseable
     * Contains one other resources as a data item.
     */
    static class bResource implements AutoCloseable {

        cResource cR;

        public bResource() {
            cR = null;
        }

        public bResource(cResource cr) {
            cR = cr;
        }

        public cResource getC() {
            return cR;
        }

        @Override
        public void close() {
            ResInNestedExpr.closing(this.getClass().getName());
        }
    }

    /** A resource to implement AutoCloseable */
    static class cResource implements AutoCloseable {

        public cResource() {
        }

        @Override
        public void close() {
            ResInNestedExpr.closing(this.getClass().getName());
        }
    }
}

