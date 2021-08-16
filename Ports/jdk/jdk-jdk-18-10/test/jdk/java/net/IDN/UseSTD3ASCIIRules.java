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
 * @bug 8023881
 * @summary IDN.USE_STD3_ASCII_RULES option is too strict to use Unicode
 *          in IDN.toASCII
 */

import java.net.*;

public class UseSTD3ASCIIRules {

    public static void main(String[] args) throws Exception {
        // Per Section 4.1, RFC 3490, if the UseSTD3ASCIIRules flag is set,
        // then perform these checks:
        //
        // (a) Verify the absence of non-LDH ASCII code points; that is, the
        //     absence of 0..2C, 2E..2F, 3A..40, 5B..60, and 7B..7F.
        //
        // (b) Verify the absence of leading and trailing hyphen-minus; that
        //     is, the absence of U+002D at the beginning and end of the
        //     sequence.
        String[] illegalNames = {
                "www.example.com-",
                "-www.example.com",
                "-www.example.com-",
                "www.ex\u002Cmple.com",
                "www.ex\u007Bmple.com",
                "www.ex\u007Fmple.com"
            };

        String[] legalNames = {
                "www.ex-ample.com",
                "www.ex\u002Dmple.com",         // www.ex-mple.com
                "www.ex\u007Ample.com",         // www.exzmple.com
                "www.ex\u3042mple.com",         // www.xn--exmple-j43e.com
                "www.\u3042\u3044\u3046.com",   // www.xn--l8jeg.com
                "www.\u793A\u4F8B.com"          // www.xn--fsq092h.com
            };

        for (String name : illegalNames) {
            try {
                System.out.println("Convering illegal IDN: " + name);
                IDN.toASCII(name, IDN.USE_STD3_ASCII_RULES);
                throw new Exception(
                    "Expected to get IllegalArgumentException for " + name);
            } catch (IllegalArgumentException iae) {
                // That's the right behavior.
            }
        }

        for (String name : legalNames) {
            System.out.println("Convering legal IDN: " + name);
            System.out.println("\tThe ACE form is: " +
                        IDN.toASCII(name, IDN.USE_STD3_ASCII_RULES));
        }
    }
}
