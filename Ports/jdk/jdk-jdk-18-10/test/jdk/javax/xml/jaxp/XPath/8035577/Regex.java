/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035577
 * @summary Tests for xpath regular expression methods.
 * @modules java.xml/com.sun.org.apache.xerces.internal.impl.xpath.regex
 * @run main Regex
 * @author david.x.li@oracle.com
 */

import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

public class Regex {

    public static void main(String[] args) {
        testIntersect();
    }

    static void testIntersect() {
        // The use of intersection operator & is not allowed in
        // XML schema.  Consequently, the intersection operator
        // can never be called except for internal API usage.
        // Following test illustrates this.
        try{
            new RegularExpression("(?[b-d]&[a-r])", "X");
            throw new RuntimeException ("Xerces XPath Regex: " +
                "intersection not allowed in XML schema mode, " +
                "exception expected above.");
        }
        catch (ParseException e) {
            // Empty, expecting an exception
        }

        // Bug 8035577: verifying a typo fix in RangeToken.intersectRanges.
        // Note: Each test case has a diagram showing the ranges being tested.
        // Following test case will trigger the typo.
        // o-----o
        //    o-----o
        RegularExpression ce = new RegularExpression("(?[a-e]&[c-r])");
        if (!(ce.matches("c") && ce.matches("d") && ce.matches("e"))) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[c-e] expected to match c,d,e.");
        }

        if (ce.matches("b") || ce.matches("f")) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[c-e] not expected to match b or f.");
        }

        // Test the expected behavior after fixing the typo.
        //    o------o
        // o-------------o
        RegularExpression bd = new RegularExpression("(?[b-d]&[a-r])");
        if (!(bd.matches("b") && bd.matches("c") && bd.matches("d"))) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[b-d] expected to match b,c,d.");
        }

        if (bd.matches("e") || bd.matches("a")) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[b-d] not expected to match a or e.");
        }

        // Bug fix for first range ends later than second range.
        // o--------o
        //    o--o
        RegularExpression bd2 = new RegularExpression("(?[a-r]&[b-d])");
        if (!(bd.matches("b") && bd.matches("c") && bd.matches("d"))) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[b-d] expected to match b,c,d, test 2.");
        }

        if (bd2.matches("e") || bd2.matches("a")) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[b-d] not expected to match a or e, test 2.");
        }

        //    o-----o
        // o----o
        RegularExpression dh = new RegularExpression("(?[d-z]&[a-h])");
        if (!(dh.matches("d") && dh.matches("e") && dh.matches("h"))) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[d-h] expected to match d,e,h.");
        }

        if (dh.matches("c") || bd2.matches("i")) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[d-h] not expected to match c or i.");
        }

        // Test code improvement, addition of src2+=2 to one of the
        // conditions.  In this case, src1 leftover from matching
        // first portion of src2 is re-used to match against the next
        // portion of src2.
        // o--------------o
        //   o--o  o--o
        RegularExpression dfhk = new RegularExpression("(?[b-r]&[d-fh-k])");
        if (!(dfhk.matches("d") && dfhk.matches("f") && dfhk.matches("h") && dfhk.matches("k"))) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[d-fh-k] expected to match d,f,h,k.");
        }

        if (dfhk.matches("c") || dfhk.matches("g") || dfhk.matches("l")) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[d-fh-k] not expected to match c,g,l.");
        }

        // random tests
        //    o------------o
        // o-----o  o--o
        RegularExpression cfhk = new RegularExpression("(?[c-r]&[b-fh-k])");
        if (!(cfhk.matches("c") && cfhk.matches("f") && cfhk.matches("h") && cfhk.matches("k"))) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[c-fh-k] expected to match c,f,h,k.");
        }

        if (cfhk.matches("b") || cfhk.matches("g") || cfhk.matches("l")) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[c-fh-k] not expected to match b,g,l.");
        }

        // o----------o
        //    o-----------o
        //  o----o  o---o
        RegularExpression ekor = new RegularExpression("(?[a-r]&[e-z]&[c-ko-s])");
        if (!(ekor.matches("e") && ekor.matches("k") && ekor.matches("o") && ekor.matches("r"))) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[e-ko-r] expected to match e,k,o,r.");
        }

        if (ekor.matches("d") || ekor.matches("l") || ekor.matches("s")) {
            throw new RuntimeException("Xerces XPath Regex Error: " +
                "[e-ko-r] not expected to match d,l,s.");
        }

    }

}
