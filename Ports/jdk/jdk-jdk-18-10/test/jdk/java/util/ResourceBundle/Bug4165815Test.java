/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
    The fix for 4165815 has been backed out because of compatibility issues.
    Disabled this test temporarily until a better fix is found by removing
    the at-signs.
    test
    summary test Bug 4165815
    run main Bug4165815Test
    bug 4165815
*/
/*
 *
 *
 * (C) Copyright IBM Corp. 1999 - All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by IBM. These materials are provided
 * under terms of a License Agreement between IBM and Sun.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 *
 */

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/**
 *  This is a regression test for the following bug:
 *  "If the path specified by the baseName argument to
 *  ResourceBundle.getBundle() begins with a leading slash, then the bundle
 *  is not found relative to the classpath.
 *
 *  Clearly, the leading slash was inappropriate, however this did work
 *  previously (pre 1.2) and should continue to work in the same fashion."
 *
 *  A Bundle base name should never contain a "/" and thus an
 *  IllegalArgumentException should be thrown.
 */
public class Bug4165815Test extends RBTestFmwk {
    public static void main(String[] args) throws Exception {
        new Bug4165815Test().run(args);
    }

    private static final String bundleName = "/Bug4165815Bundle";
    public void testIt() throws Exception {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName, new Locale("en", "US"));
            errln("ResourceBundle returned a bundle when it should not have.");
        } catch (IllegalArgumentException e) {
            //This is what we should get when the base name contains a "/" character.
        } catch (MissingResourceException e) {
            errln("ResourceBundle threw a MissingResourceException when it should have thrown an IllegalArgumentException.");
        }
    }
}
