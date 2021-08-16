/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.loading;

import static com.sun.jmx.defaults.JmxProperties.MLET_LOGGER;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.lang.System.Logger.Level;

/**
 * This class is used for parsing URLs.
 *
 * @since 1.5
 */
class MLetParser {

/*
  * ------------------------------------------
  *   PRIVATE VARIABLES
  * ------------------------------------------
  */

    /**
     * The current character
     */
    private int c;

    /**
     * Tag to parse.
     */
    private static String tag = "mlet";


  /*
  * ------------------------------------------
  *   CONSTRUCTORS
  * ------------------------------------------
  */

    /**
     * Create an MLet parser object
     */
    public MLetParser() {
    }

    /*
     * ------------------------------------------
     *   PUBLIC METHODS
     * ------------------------------------------
     */

    /**
     * Scan spaces.
     */
    public void skipSpace(Reader in) throws IOException {
        while ((c >= 0) && ((c == ' ') || (c == '\t') || (c == '\n') || (c == '\r'))) {
            c = in.read();
        }
    }

    /**
     * Scan identifier
     */
    public String scanIdentifier(Reader in) throws IOException {
        StringBuilder buf = new StringBuilder();
        while (true) {
            if (((c >= 'a') && (c <= 'z')) ||
                ((c >= 'A') && (c <= 'Z')) ||
                ((c >= '0') && (c <= '9')) || (c == '_')) {
                buf.append((char)c);
                c = in.read();
            } else {
                return buf.toString();
            }
        }
    }

    /**
     * Scan tag
     */
    public Map<String,String> scanTag(Reader in) throws IOException {
        Map<String,String> atts = new HashMap<String,String>();
        skipSpace(in);
        while (c >= 0 && c != '>') {
            if (c == '<')
                throw new IOException("Missing '>' in tag");
            String att = scanIdentifier(in);
            String val = "";
            skipSpace(in);
            if (c == '=') {
                int quote = -1;
                c = in.read();
                skipSpace(in);
                if ((c == '\'') || (c == '\"')) {
                    quote = c;
                    c = in.read();
                }
                StringBuilder buf = new StringBuilder();
                while ((c > 0) &&
                       (((quote < 0) && (c != ' ') && (c != '\t') &&
                         (c != '\n') && (c != '\r') && (c != '>'))
                        || ((quote >= 0) && (c != quote)))) {
                    buf.append((char)c);
                    c = in.read();
                }
                if (c == quote) {
                    c = in.read();
                }
                skipSpace(in);
                val = buf.toString();
            }
            atts.put(att.toLowerCase(Locale.ENGLISH), val);
            skipSpace(in);
        }
        return atts;
    }

    /**
     * Scan an html file for {@literal <mlet>} tags.
     */
    public List<MLetContent> parse(URL url) throws IOException {
        // Warning Messages
        String requiresTypeWarning = "<arg type=... value=...> tag requires type parameter.";
        String requiresValueWarning = "<arg type=... value=...> tag requires value parameter.";
        String paramOutsideWarning = "<arg> tag outside <mlet> ... </mlet>.";
        String requiresCodeWarning = "<mlet> tag requires either code or object parameter.";
        String requiresJarsWarning = "<mlet> tag requires archive parameter.";

        URLConnection conn;

        conn = url.openConnection();
        Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                                                             "UTF-8"));

        // The original URL may have been redirected - this
        // sets it to whatever URL/codebase we ended up getting
        //
        url = conn.getURL();

        List<MLetContent> mlets = new ArrayList<MLetContent>();
        Map<String,String> atts = null;

        List<String> types = new ArrayList<String>();
        List<String> values = new ArrayList<String>();

        // debug("parse","*** Parsing " + url );
        while(true) {
            c = in.read();
            if (c == -1)
                break;
            if (c == '<') {
                c = in.read();
                if (c == '/') {
                    c = in.read();
                    String nm = scanIdentifier(in);
                    if (c != '>')
                        throw new IOException("Missing '>' in tag");
                    if (nm.equalsIgnoreCase(tag)) {
                        if (atts != null) {
                            mlets.add(new MLetContent(url, atts, types, values));
                        }
                        atts = null;
                        types = new ArrayList<String>();
                        values = new ArrayList<String>();
                    }
                } else {
                    String nm = scanIdentifier(in);
                    if (nm.equalsIgnoreCase("arg")) {
                        Map<String,String> t = scanTag(in);
                        String att = t.get("type");
                        if (att == null) {
                            MLET_LOGGER.log(Level.TRACE, requiresTypeWarning);
                            throw new IOException(requiresTypeWarning);
                        } else {
                            if (atts != null) {
                                types.add(att);
                            } else {
                                MLET_LOGGER.log(Level.TRACE, paramOutsideWarning);
                                throw new IOException(paramOutsideWarning);
                            }
                        }
                        String val = t.get("value");
                        if (val == null) {
                            MLET_LOGGER.log(Level.TRACE, requiresValueWarning);
                            throw new IOException(requiresValueWarning);
                        } else {
                            if (atts != null) {
                                values.add(val);
                            } else {
                                MLET_LOGGER.log(Level.TRACE, paramOutsideWarning);
                                throw new IOException(paramOutsideWarning);
                            }
                        }
                    } else {
                        if (nm.equalsIgnoreCase(tag)) {
                            atts = scanTag(in);
                            if (atts.get("code") == null && atts.get("object") == null) {
                                MLET_LOGGER.log(Level.TRACE, requiresCodeWarning);
                                throw new IOException(requiresCodeWarning);
                            }
                            if (atts.get("archive") == null) {
                                MLET_LOGGER.log(Level.TRACE, requiresJarsWarning);
                                throw new IOException(requiresJarsWarning);
                            }
                        }
                    }
                }
            }
        }
        in.close();
        return mlets;
    }

    /**
     * Parse the document pointed by the URL urlname
     */
    public List<MLetContent> parseURL(String urlname) throws IOException {
        // Parse the document
        //
        URL url;
        if (urlname.indexOf(':') <= 1) {
            String userDir = System.getProperty("user.dir");
            String prot;
            if (userDir.charAt(0) == '/' ||
                userDir.charAt(0) == File.separatorChar) {
                prot = "file:";
            } else {
                prot = "file:/";
            }
            url =
                new URL(prot + userDir.replace(File.separatorChar, '/') + "/");
            url = new URL(url, urlname);
        } else {
            url = new URL(urlname);
        }
        // Return list of parsed MLets
        //
        return parse(url);
    }

}
