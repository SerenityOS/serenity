/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.http.logging;

import java.util.logging.LogRecord;
import java.util.regex.*;

/**
 * A Formatter to make the HTTP logs a bit more palatable to the developer
 * looking at them. The idea is to present the HTTP events in such a way that
 * commands and headers are easily spotted (i.e. on separate lines).
 * @author jccollet
 */
public class HttpLogFormatter extends java.util.logging.SimpleFormatter {
    // Pattern for MessageHeader data. Mostly pairs within curly brackets
    private static volatile Pattern pattern = null;
    // Pattern for Cookies
    private static volatile Pattern cpattern = null;

    public HttpLogFormatter() {
        if (pattern == null) {
            pattern = Pattern.compile("\\{[^\\}]*\\}");
            cpattern = Pattern.compile("[^,\\] ]{2,}");
        }
    }

    @Override
    public String format(LogRecord record) {
        String sourceClassName = record.getSourceClassName();
        if (sourceClassName == null ||
            !(sourceClassName.startsWith("sun.net.www.protocol.http") ||
              sourceClassName.startsWith("sun.net.www.http"))) {
            return super.format(record);
        }
        String src = record.getMessage();
        StringBuilder buf = new StringBuilder("HTTP: ");
        if (src.startsWith("sun.net.www.MessageHeader@")) {
            // MessageHeader logs are composed of pairs within curly brackets
            // Let's extract them to make it more readable. That way we get one
            // header pair (name, value) per line. A lot easier to read.
            Matcher match = pattern.matcher(src);
            while (match.find()) {
                int i = match.start();
                int j = match.end();
                String s = src.substring(i + 1, j - 1);
                if (s.startsWith("null: ")) {
                    s = s.substring(6);
                }
                if (s.endsWith(": null")) {
                    s = s.substring(0, s.length() - 6);
                }
                buf.append("\t").append(s).append("\n");
            }
        } else if (src.startsWith("Cookies retrieved: {")) {
            // This comes from the Cookie handler, let's clean up the format a bit
            String s = src.substring(20);
            buf.append("Cookies from handler:\n");
            while (s.length() >= 7) {
                if (s.startsWith("Cookie=[")) {
                    String s2 = s.substring(8);
                    int c = s2.indexOf("Cookie2=[");
                    if (c > 0) {
                        s2 = s2.substring(0, c-1);
                        s = s2.substring(c);
                    } else {
                        s = "";
                    }
                    if (s2.length() < 4) {
                        continue;
                    }
                    Matcher m = cpattern.matcher(s2);
                    while (m.find()) {
                        int i = m.start();
                        int j = m.end();
                        if (i >= 0) {
                            String cookie = s2.substring(i + 1, j > 0 ? j - 1 : s2.length() - 1);
                            buf.append("\t").append(cookie).append("\n");
                        }
                    }
                }
                if (s.startsWith("Cookie2=[")) {
                    String s2 = s.substring(9);
                    int c = s2.indexOf("Cookie=[");
                    if (c > 0) {
                        s2 = s2.substring(0, c-1);
                        s = s2.substring(c);
                    } else {
                        s = "";
                    }
                    Matcher m = cpattern.matcher(s2);
                    while (m.find()) {
                        int i = m.start();
                        int j = m.end();
                        if (i >= 0) {
                            String cookie = s2.substring(i+1, j > 0 ? j-1 : s2.length() - 1);
                            buf.append("\t").append(cookie).append("\n");
                        }
                    }
                }
            }
        } else {
            // Anything else we let as is.
            buf.append(src).append("\n");
        }
        return buf.toString();
    }

}
