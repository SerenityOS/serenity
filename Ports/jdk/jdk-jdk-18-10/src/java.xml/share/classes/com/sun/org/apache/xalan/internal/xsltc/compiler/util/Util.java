/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler.util;

import com.sun.org.apache.bcel.internal.generic.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Constants;
import com.sun.org.apache.xml.internal.utils.XML11Char;
import java.util.StringTokenizer;
import jdk.xml.internal.SecuritySupport;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @LastModified: Sep 2017
 */
public final class Util {
    private static char filesep;

    static {
        String temp = SecuritySupport.getSystemProperty("file.separator", "/");
        filesep = temp.charAt(0);
    }

    public static String noExtName(String name) {
        final int index = name.lastIndexOf('.');
        return name.substring(0, index >= 0 ? index : name.length());
    }

    /**
     * Search for both slashes in order to support URLs and
     * files.
     */
    public static String baseName(String name) {
        int index = name.lastIndexOf('\\');
        if (index < 0) {
            index = name.lastIndexOf('/');
        }

        if (index >= 0)
            return name.substring(index + 1);
        else {
            int lastColonIndex = name.lastIndexOf(':');
            if (lastColonIndex > 0)
                return name.substring(lastColonIndex + 1);
            else
                return name;
        }
    }

    /**
     * Search for both slashes in order to support URLs and
     * files.
     */
    public static String pathName(String name) {
        int index = name.lastIndexOf('/');
        if (index < 0) {
            index = name.lastIndexOf('\\');
        }
        return name.substring(0, index + 1);
    }

    /**
     * Replace all illegal Java chars by '_'.
     */
    public static String toJavaName(String name) {
        if (name.length() > 0) {
            final StringBuffer result = new StringBuffer();

            char ch = name.charAt(0);
            result.append(Character.isJavaIdentifierStart(ch) ? ch : '_');

            final int n = name.length();
            for (int i = 1; i < n; i++) {
                ch = name.charAt(i);
                result.append(Character.isJavaIdentifierPart(ch)  ? ch : '_');
            }
            return result.toString();
        }
        return name;
    }

    public static Type getJCRefType(String signature) {
        return Type.getType(signature);
    }

    public static String internalName(String cname) {
        return cname.replace('.', filesep);
    }

    public static void println(String s) {
        System.out.println(s);
    }

    public static void println(char ch) {
        System.out.println(ch);
    }

    public static void TRACE1() {
        System.out.println("TRACE1");
    }

    public static void TRACE2() {
        System.out.println("TRACE2");
    }

    public static void TRACE3() {
        System.out.println("TRACE3");
    }

    /**
     * Replace a certain character in a string with a new substring.
     */
    public static String replace(String base, char ch, String str) {
        return (base.indexOf(ch) < 0) ? base :
            replace(base, String.valueOf(ch), new String[] { str });
    }

    public static String replace(String base, String delim, String[] str) {
        final int len = base.length();
        final StringBuffer result = new StringBuffer();

        for (int i = 0; i < len; i++) {
            final char ch = base.charAt(i);
            final int k = delim.indexOf(ch);

            if (k >= 0) {
                result.append(str[k]);
            }
            else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * Replace occurances of '.', '-', '/' and ':'
     */
    public static String escape(String input) {
        return replace(input, ".-/:",
            new String[] { "$dot$", "$dash$", "$slash$", "$colon$" });
    }

    public static String getLocalName(String qname) {
        final int index = qname.lastIndexOf(":");
        return (index > 0) ? qname.substring(index + 1) : qname;
    }

    public static String getPrefix(String qname) {
        final int index = qname.lastIndexOf(":");
        return (index > 0) ? qname.substring(0, index) :
            Constants.EMPTYSTRING;
    }

    /**
     * Checks if the string is a literal (i.e. not an AVT) or not.
     */
    public static boolean isLiteral(String str) {
        final int length = str.length();
        for (int i = 0; i < length - 1; i++) {
            if (str.charAt(i) == '{' && str.charAt(i + 1) != '{') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the string is valid list of qnames
     */
    public static boolean isValidQNames(String str) {
        if ((str != null) && (!str.equals(Constants.EMPTYSTRING))) {
            final StringTokenizer tokens = new StringTokenizer(str);
            while (tokens.hasMoreTokens()) {
                if (!XML11Char.isXML11ValidQName(tokens.nextToken())) {
                    return false;
                }
            }
        }
        return true;
    }

}
