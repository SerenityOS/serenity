/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.xpath.regex;

import java.text.CharacterIterator;

/**
 * @xerces.internal
 *
 */
public final class REUtil {
    private REUtil() {
    }

    static final int composeFromSurrogates(int high, int low) {
        return 0x10000 + ((high-0xd800)<<10) + low-0xdc00;
    }

    static final boolean isLowSurrogate(int ch) {
        return (ch & 0xfc00) == 0xdc00;
    }

    static final boolean isHighSurrogate(int ch) {
        return (ch & 0xfc00) == 0xd800;
    }

    static final String decomposeToSurrogates(int ch) {
        char[] chs = new char[2];
        ch -= 0x10000;
        chs[0] = (char)((ch>>10)+0xd800);
        chs[1] = (char)((ch&0x3ff)+0xdc00);
        return new String(chs);
    }

    static final String substring(CharacterIterator iterator, int begin, int end) {
        char[] src = new char[end-begin];
        for (int i = 0;  i < src.length;  i ++)
            src[i] = iterator.setIndex(i+begin);
        return new String(src);
    }

    // ================================================================

    static final int getOptionValue(int ch) {
        int ret = 0;
        switch (ch) {
          case 'i':
            ret = RegularExpression.IGNORE_CASE;
            break;
          case 'm':
            ret = RegularExpression.MULTIPLE_LINES;
            break;
          case 's':
            ret = RegularExpression.SINGLE_LINE;
            break;
          case 'x':
            ret = RegularExpression.EXTENDED_COMMENT;
            break;
          case 'u':
            ret = RegularExpression.USE_UNICODE_CATEGORY;
            break;
          case 'w':
            ret = RegularExpression.UNICODE_WORD_BOUNDARY;
            break;
          case 'F':
            ret = RegularExpression.PROHIBIT_FIXED_STRING_OPTIMIZATION;
            break;
          case 'H':
            ret = RegularExpression.PROHIBIT_HEAD_CHARACTER_OPTIMIZATION;
            break;
          case 'X':
            ret = RegularExpression.XMLSCHEMA_MODE;
            break;
          case ',':
            ret = RegularExpression.SPECIAL_COMMA;
            break;
          default:
        }
        return ret;
    }

    static final int parseOptions(String opts) throws ParseException {
        if (opts == null)  return 0;
        int options = 0;
        for (int i = 0;  i < opts.length();  i ++) {
            int v = getOptionValue(opts.charAt(i));
            if (v == 0)
                throw new ParseException("Unknown Option: "+opts.substring(i), -1);
            options |= v;
        }
        return options;
    }

    static final String createOptionString(int options) {
        StringBuilder sb = new StringBuilder(9);
        if ((options & RegularExpression.PROHIBIT_FIXED_STRING_OPTIMIZATION) != 0)
            sb.append('F');
        if ((options & RegularExpression.PROHIBIT_HEAD_CHARACTER_OPTIMIZATION) != 0)
            sb.append('H');
        if ((options & RegularExpression.XMLSCHEMA_MODE) != 0)
            sb.append('X');
        if ((options & RegularExpression.IGNORE_CASE) != 0)
            sb.append('i');
        if ((options & RegularExpression.MULTIPLE_LINES) != 0)
            sb.append('m');
        if ((options & RegularExpression.SINGLE_LINE) != 0)
            sb.append('s');
        if ((options & RegularExpression.USE_UNICODE_CATEGORY) != 0)
            sb.append('u');
        if ((options & RegularExpression.UNICODE_WORD_BOUNDARY) != 0)
            sb.append('w');
        if ((options & RegularExpression.EXTENDED_COMMENT) != 0)
            sb.append('x');
        if ((options & RegularExpression.SPECIAL_COMMA) != 0)
            sb.append(',');
        return sb.toString().intern();
    }

    // ================================================================

    static String stripExtendedComment(String regex) {
        int len = regex.length();
        StringBuilder buffer = new StringBuilder(len);
        int offset = 0;
        int charClass = 0;
        while (offset < len) {
            int ch = regex.charAt(offset++);
                                                // Skips a white space.
            if (ch == '\t' || ch == '\n' || ch == '\f' || ch == '\r' || ch == ' ') {
                // if we are inside a character class, we keep the white space
                if (charClass > 0) {
                    buffer.append((char)ch);
                }
                continue;
            }

            if (ch == '#') {                    // Skips chracters between '#' and a line end.
                while (offset < len) {
                    ch = regex.charAt(offset++);
                    if (ch == '\r' || ch == '\n')
                        break;
                }
                continue;
            }

            int next;                           // Strips an escaped white space.
            if (ch == '\\' && offset < len) {
                if ((next = regex.charAt(offset)) == '#'
                    || next == '\t' || next == '\n' || next == '\f'
                    || next == '\r' || next == ' ') {
                    buffer.append((char)next);
                    offset ++;
                } else {                        // Other escaped character.
                    buffer.append('\\');
                    buffer.append((char)next);
                    offset ++;
                }
            }
            else if (ch == '[') {
                charClass++;
                buffer.append((char)ch);
                if (offset < len) {
                    next = regex.charAt(offset);
                    if (next == '[' || next ==']') {
                        buffer.append((char)next);
                        offset ++;
                    }
                    else if (next == '^' && offset + 1 < len) {
                        next = regex.charAt(offset + 1);
                        if (next == '[' || next ==']') {
                            buffer.append('^');
                            buffer.append((char)next);
                            offset += 2;
                        }
                    }
                }
            }
            else {
                if (charClass > 0 && ch == ']') {
                    --charClass;
                }
                buffer.append((char)ch);
            }
        }
        return buffer.toString();
    }

    // ================================================================

    /**
     * Sample entry.
     * <div>Usage: <KBD>com.sun.org.apache.xerces.internal.utils.regex.REUtil &lt;regex&gt; &lt;string&gt;</KBD></div>
     */
    public static void main(String[] argv) {
        String pattern = null;
        try {
            String options = "";
            String target = null;
            if( argv.length == 0 ) {
                System.out.println( "Error:Usage: java REUtil -i|-m|-s|-u|-w|-X regularExpression String" );
                System.exit( 0 );
            }
            for (int i = 0;  i < argv.length;  i ++) {
                if (argv[i].length() == 0 || argv[i].charAt(0) != '-') {
                    if (pattern == null)
                        pattern = argv[i];
                    else if (target == null)
                        target = argv[i];
                    else
                        System.err.println("Unnecessary: "+argv[i]);
                } else if (argv[i].equals("-i")) {
                    options += "i";
                } else if (argv[i].equals("-m")) {
                    options += "m";
                } else if (argv[i].equals("-s")) {
                    options += "s";
                } else if (argv[i].equals("-u")) {
                    options += "u";
                } else if (argv[i].equals("-w")) {
                    options += "w";
                } else if (argv[i].equals("-X")) {
                    options += "X";
                } else {
                    System.err.println("Unknown option: "+argv[i]);
                }
            }
            RegularExpression reg = new RegularExpression(pattern, options);
            System.out.println("RegularExpression: "+reg);
            Match match = new Match();
            reg.matches(target, match);
            for (int i = 0;  i < match.getNumberOfGroups();  i ++) {
                if (i == 0 )  System.out.print("Matched range for the whole pattern: ");
                else System.out.print("["+i+"]: ");
                if (match.getBeginning(i) < 0)
                    System.out.println("-1");
                else {
                    System.out.print(match.getBeginning(i)+", "+match.getEnd(i)+", ");
                    System.out.println("\""+match.getCapturedText(i)+"\"");
                }
            }
        } catch (ParseException pe) {
            if (pattern == null) {
                pe.printStackTrace();
            } else {
                System.err.println("com.sun.org.apache.xerces.internal.utils.regex.ParseException: "+pe.getMessage());
                String indent = "        ";
                System.err.println(indent+pattern);
                int loc = pe.getLocation();
                if (loc >= 0) {
                    System.err.print(indent);
                    for (int i = 0;  i < loc;  i ++)  System.err.print("-");
                    System.err.println("^");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static final int CACHESIZE = 20;
    static final RegularExpression[] regexCache = new RegularExpression[CACHESIZE];
    /**
     * Creates a RegularExpression instance.
     * This method caches created instances.
     *
     * @see RegularExpression#RegularExpression(java.lang.String, java.lang.String)
     */
    public static RegularExpression createRegex(String pattern, String options)
        throws ParseException {
        RegularExpression re = null;
        int intOptions = REUtil.parseOptions(options);
        synchronized (REUtil.regexCache) {
            int i;
            for (i = 0;  i < REUtil.CACHESIZE;  i ++) {
                RegularExpression cached = REUtil.regexCache[i];
                if (cached == null) {
                    i = -1;
                    break;
                }
                if (cached.equals(pattern, intOptions)) {
                    re = cached;
                    break;
                }
            }
            if (re != null) {
                if (i != 0) {
                    System.arraycopy(REUtil.regexCache, 0, REUtil.regexCache, 1, i);
                    REUtil.regexCache[0] = re;
                }
            } else {
                re = new RegularExpression(pattern, options);
                System.arraycopy(REUtil.regexCache, 0, REUtil.regexCache, 1, REUtil.CACHESIZE-1);
                REUtil.regexCache[0] = re;
            }
        }
        return re;
    }

    /**
     *
     * @see RegularExpression#matches(java.lang.String)
     */
    public static boolean matches(String regex, String target) throws ParseException {
        return REUtil.createRegex(regex, null).matches(target);
    }

    /**
     *
     * @see RegularExpression#matches(java.lang.String)
     */
    public static boolean matches(String regex, String options, String target) throws ParseException {
        return REUtil.createRegex(regex, options).matches(target);
    }

    // ================================================================

    /**
     *
     */
    public static String quoteMeta(String literal) {
        int len = literal.length();
        StringBuilder buffer = null;
        for (int i = 0;  i < len;  i ++) {
            int ch = literal.charAt(i);
            if (".*+?{[()|\\^$".indexOf(ch) >= 0) {
                if (buffer == null) {
                    buffer = new StringBuilder(i+(len-i)*2);
                    if (i > 0)  buffer.append(literal.substring(0, i));
                }
                buffer.append('\\');
                buffer.append((char)ch);
            } else if (buffer != null)
                buffer.append((char)ch);
        }
        return buffer != null ? buffer.toString() : literal;
    }

    // ================================================================

    static void dumpString(String v) {
        for (int i = 0;  i < v.length();  i ++) {
            System.out.print(Integer.toHexString(v.charAt(i)));
            System.out.print(" ");
        }
        System.out.println();
    }
}
