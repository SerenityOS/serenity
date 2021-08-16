/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

import pkg1.*;

/**
 * Another test class. Testing empty {@index }.
 */
public class AnotherClass {

    /**
     * A test field. Testing only white-spaces in index tag text {@index       }.
     */
    public RegClass field;

    /**
     * Constant field. Testing no text in index tag {@index}.
     */
    public static final String CONSTANT_FIELD_3 = "constant";

    /**
     * @deprecated don't use this field anymore.
     */
    public RegClass dep_field;

    /**
     * A sample enum.
     */
    public static enum ModalExclusionType {
        /**
         * Test comment. Testing inline tag inside index tag {@index "nested {@index nested_tag_test}"}
         */
        NO_EXCLUDE,
        /**
         * Another comment. Testing HTML inside index tag {@index "html <span> see </span>"}
         */
        APPLICATION_EXCLUDE
    };

    /**
     * A string constant. Testing {@index "quoted"no-space}.
     */
    public static final String CONSTANT1 = "C2";

    /**
     * A sample method. Testing search tag for {@index "unclosed quote}.
     *
     * @param param some parameter.
     * @return a test object.
     */
    public pkg1.RegClass method(pkg1.RegClass param) {
        return param;
    }

    /**
     * Method to test member search index URL.
     *
     * @param testArray some test array.
     * @param testInt some test int.
     * @param testString some test string.
     */
    public void method(byte[] testArray, int testInt, String testString) {
    }
}
