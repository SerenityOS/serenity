/*
 * Copyright (c) 2015 SAP SE. All rights reserved.
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
 * @bug 8068909
 * @summary test that string optimizations produce code, that doesn't lead to a crash.
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.stringopts.TestOptimizeStringConcat
 * @author axel.siebenborn@sap.com
 */

package compiler.stringopts;

public class TestOptimizeStringConcat {

    static boolean checkArgumentSyntax(String value, String allowedchars, String notallowedchars, String logmsg) {
        String rc = null;

        int maxchar = 99999;
        int minchar = 1;
        if ((allowedchars != null && notallowedchars != null) || minchar > maxchar) {
            rc = "internal error";
        } else {
            if (value == null) {
                rc = "the value null is not allowed, it is missing";
            } else if (value != null && minchar > 0 && value.trim().equals("")) {
                rc = "the value must not be empty";
            } else if (value != null) {
                if (value.length() < minchar || value.length() > maxchar) {
                    if (rc == null) {
                        rc = "the value length must be between +minchar+ and +maxchar";
                    }
                }
                char[] _value = value.toCharArray();
                boolean dotfound = false;
                int i = 1;
                if (_value[i] == '.' && !dotfound) {
                    dotfound = true;
                } else if (allowedchars != null && allowedchars.indexOf(_value[i]) == -1) {
                    if (rc == null) {
                        rc = "the value contains an illegal character: '" + _value[i] + "', only following characters are allowed: '+allowedchars+'";
                    } else {
                        rc += " / the value contains an illegal character: '" + _value[i] + "', only following characters are allowed: '+allowedchars+'";
                    }
                } else if (notallowedchars != null && notallowedchars.indexOf(_value[i]) != -1) {
                    if (rc == null) {
                        rc = "the value contains an illegal character: '" + _value[i] + "', following characters are not allowed '+notallowedchars+'";
                    } else {
                        rc += " / the value contains an illegal character: '" + _value[i] + "', following characters are not allowed '+notallowedchars+'";
                    }
                }
            }
        }

        if (rc != null) {
            System.out.println(logmsg + " ==> " + rc);
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        boolean failed = false;
        for (int i = 0; i < 10000; i++) {
            failed |= !checkArgumentSyntax("theName", null, "\"<&", "Error consistencyCheck: name in component definition");
            failed |= !checkArgumentSyntax(null, null, "\"<&", "Error consistencyCheck: name in component definition");
            failed |= !checkArgumentSyntax("42", "0123456789.", null, "Error consistencyCheck: counter in component definition");
        }
        System.out.println(failed);
    }
}
