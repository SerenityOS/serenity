/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.security.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

/**
 * A utility class to expand properties embedded in a string.
 * Strings of the form ${some.property.name} are expanded to
 * be the value of the property. Also, the special ${/} property
 * is expanded to be the same as file.separator. If a property
 * is not set, a GeneralSecurityException will be thrown.
 *
 * @author Roland Schemers
 */
public class PropertyExpander {


    public static class ExpandException extends GeneralSecurityException {

        @java.io.Serial
        private static final long serialVersionUID = -7941948581406161702L;

        public ExpandException(String msg) {
            super(msg);
        }
    }

    public static String expand(String value)
        throws ExpandException
    {
        return expand(value, false);
    }

     public static String expand(String value, boolean encodeURL)
         throws ExpandException
     {
        if (value == null)
            return null;

        int p = value.indexOf("${", 0);

        // no special characters
        if (p == -1) return value;

        StringBuilder sb = new StringBuilder(value.length());
        int max = value.length();
        int i = 0;  // index of last character we copied

    scanner:
        while (p < max) {
            if (p > i) {
                // copy in anything before the special stuff
                sb.append(value.substring(i, p));
                i = p;
            }
            int pe = p+2;

            // do not expand ${{ ... }}
            if (pe < max && value.charAt(pe) == '{') {
                pe = value.indexOf("}}", pe);
                if (pe == -1 || pe+2 == max) {
                    // append remaining chars
                    sb.append(value.substring(p));
                    break scanner;
                } else {
                    // append as normal text
                    pe++;
                    sb.append(value.substring(p, pe+1));
                }
            } else {
                while ((pe < max) && (value.charAt(pe) != '}')) {
                    pe++;
                }
                if (pe == max) {
                    // no matching '}' found, just add in as normal text
                    sb.append(value.substring(p, pe));
                    break scanner;
                }
                String prop = value.substring(p+2, pe);
                if (prop.equals("/")) {
                    sb.append(java.io.File.separatorChar);
                } else {
                    String val = System.getProperty(prop);
                    if (val != null) {
                        if (encodeURL) {
                            // encode 'val' unless it's an absolute URI
                            // at the beginning of the string buffer
                            try {
                                if (sb.length() > 0 ||
                                    !(new URI(val)).isAbsolute()) {
                                    val = sun.net.www.ParseUtil.encodePath(val);
                                }
                            } catch (URISyntaxException use) {
                                val = sun.net.www.ParseUtil.encodePath(val);
                            }
                        }
                        sb.append(val);
                    } else {
                        throw new ExpandException(
                                             "unable to expand property " +
                                             prop);
                    }
                }
            }
            i = pe+1;
            p = value.indexOf("${", i);
            if (p == -1) {
                // no more to expand. copy in any extra
                if (i < max) {
                    sb.append(value.substring(i, max));
                }
                // break out of loop
                break scanner;
            }
        }
        return sb.toString();
    }
}
