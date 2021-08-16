/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4130097
   @summary toUpperCase and toLowerCase always return a new String,
            whereas if there is no conversion that needs to be done,
            they should be returning the same instance.
   @author James Bond/007
*/
public class CaseConvertSameInstance {
    public static void main(String[] args) throws Exception {
        /* these two are the real tests for this bug. */
        if ("foobar".toLowerCase() != "foobar")
            throw new Exception("toLowerCase returned different object");
        if ("FOOBAR".toUpperCase() != "FOOBAR")
            throw new Exception("toUpperCase returned different object");

        /* sanity test toLowerCase with some border conditions. */
        if (!("FooBar".toLowerCase().equals("foobar")))
            throw new Exception("toLowerCase broken");
        if (!("fooBar".toLowerCase().equals("foobar")))
            throw new Exception("toLowerCase broken");
        if (!("foobaR".toLowerCase().equals("foobar")))
            throw new Exception("toLowerCase broken");
        if (!("FOOBAR".toLowerCase().equals("foobar")))
            throw new Exception("toLowerCase broken");

        /* sanity test toUpperCase with some border conditions. */
        if (!("FooBar".toUpperCase().equals("FOOBAR")))
            throw new Exception("toUpperCase broken");
        if (!("fooBar".toUpperCase().equals("FOOBAR")))
            throw new Exception("toUpperCase broken");
        if (!("foobaR".toUpperCase().equals("FOOBAR")))
            throw new Exception("toUpperCase broken");
        if (!("foobar".toUpperCase().equals("FOOBAR")))
            throw new Exception("toUpperCase broken");
    }
}
