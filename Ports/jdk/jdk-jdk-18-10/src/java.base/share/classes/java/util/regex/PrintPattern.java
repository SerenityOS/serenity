/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.util.regex;

import java.util.HashMap;
import java.util.regex.Pattern.CharPredicate;
import static java.util.regex.ASCII.*;

/**
 * A utility class to print out the pattern node tree.
 */

class PrintPattern {

    private static HashMap<Pattern.Node, Integer> ids = new HashMap<>();

    private static void print(Pattern.Node node, String text, int depth) {
        if (!ids.containsKey(node))
            ids.put(node, ids.size());
        System.out.printf("%6d:%" + (depth==0? "": depth<<1) + "s<%s>",
                          ids.get(node), "", text);
        if (ids.containsKey(node.next))
            System.out.printf(" (=>%d)", ids.get(node.next));
        System.out.printf("%n");
    }

    private static void print(String s, int depth) {
        System.out.printf("       %" + (depth==0?"":depth<<1) + "s<%s>%n",
                          "", s);
    }

    private static String toStringCPS(int[] cps) {
        StringBuilder sb = new StringBuilder(cps.length);
        for (int cp : cps)
            sb.append(toStringCP(cp));
        return sb.toString();
    }

    private static String toStringCP(int cp) {
        return (isPrint(cp) ? "" + (char)cp
                            : "\\u" + Integer.toString(cp, 16));
    }

    private static String toStringRange(int min, int max) {
       if (max == Pattern.MAX_REPS) {
           if (min == 0)
               return " * ";
           else if (min == 1)
               return " + ";
           return "{" + min + ", max}";
       }
       return "{" + min + ", " +  max + "}";
    }

    private static String toStringCtype(int type) {
        return switch (type) {
            case UPPER  -> "ASCII.UPPER";
            case LOWER  -> "ASCII.LOWER";
            case DIGIT  -> "ASCII.DIGIT";
            case SPACE  -> "ASCII.SPACE";
            case PUNCT  -> "ASCII.PUNCT";
            case CNTRL  -> "ASCII.CNTRL";
            case BLANK  -> "ASCII.BLANK";
            case UNDER  -> "ASCII.UNDER";
            case ASCII  -> "ASCII.ASCII";
            case ALPHA  -> "ASCII.ALPHA";
            case ALNUM  -> "ASCII.ALNUM";
            case GRAPH  -> "ASCII.GRAPH";
            case WORD   -> "ASCII.WORD";
            case XDIGIT -> "ASCII.XDIGIT";
            default     -> "ASCII ?";
        };
    }

    private static String toString(Pattern.Node node) {
        String name = node.getClass().getName();
        return name.substring(name.lastIndexOf('$') + 1);
    }

    static HashMap<CharPredicate, String> pmap;
    static {
        pmap = new HashMap<>();
        pmap.put(Pattern.ALL(), "All");
        pmap.put(Pattern.DOT(), "Dot");
        pmap.put(Pattern.UNIXDOT(), "UnixDot");
        pmap.put(Pattern.VertWS(), "VertWS");
        pmap.put(Pattern.HorizWS(), "HorizWS");

        pmap.put(CharPredicates.ASCII_DIGIT(), "ASCII.DIGIT");
        pmap.put(CharPredicates.ASCII_WORD(),  "ASCII.WORD");
        pmap.put(CharPredicates.ASCII_SPACE(), "ASCII.SPACE");
    }

    static void walk(Pattern.Node node, int depth) {
        depth++;
        while(node != null) {
            String name = toString(node);
            String str;
            if (node instanceof Pattern.Prolog) {
                print(node, name, depth);
                // print the loop here
                Pattern.Loop loop = ((Pattern.Prolog)node).loop;
                name = toString(loop);
                str = name + " " + toStringRange(loop.cmin, loop.cmax);
                print(loop, str, depth);
                walk(loop.body, depth);
                print("/" + name, depth);
                node = loop;
            } else if (node instanceof Pattern.Loop) {
                return;  // stop here, body.next -> loop
            } else if (node instanceof Pattern.Curly c) {
                str = "Curly " + c.type + " " + toStringRange(c.cmin, c.cmax);
                print(node, str, depth);
                walk(c.atom, depth);
                print("/Curly", depth);
            } else if (node instanceof Pattern.GroupCurly gc) {
                str = "GroupCurly " + gc.groupIndex / 2 +
                      ", " + gc.type + " " + toStringRange(gc.cmin, gc.cmax);
                print(node, str, depth);
                walk(gc.atom, depth);
                print("/GroupCurly", depth);
            } else if (node instanceof Pattern.GroupHead head) {
                Pattern.GroupTail tail = head.tail;
                print(head, "Group.head " + (tail.groupIndex / 2), depth);
                walk(head.next, depth);
                print(tail, "/Group.tail " + (tail.groupIndex / 2), depth);
                node = tail;
            } else if (node instanceof Pattern.GroupTail) {
                return;  // stopper
            } else if (node instanceof Pattern.Ques) {
                print(node, "Ques " + ((Pattern.Ques)node).type, depth);
                walk(((Pattern.Ques)node).atom, depth);
                print("/Ques", depth);
            } else if (node instanceof Pattern.Branch b) {
                print(b, name, depth);
                int i = 0;
                while (true) {
                    if (b.atoms[i] != null) {
                        walk(b.atoms[i], depth);
                    } else {
                        print("  (accepted)", depth);
                    }
                    if (++i == b.size)
                        break;
                    print("-branch.separator-", depth);
                }
                node = b.conn;
                print(node, "/Branch", depth);
            } else if (node instanceof Pattern.BranchConn) {
                return;
            } else if (node instanceof Pattern.CharProperty) {
                str = pmap.get(((Pattern.CharProperty)node).predicate);
                if (str == null)
                    str = toString(node);
                else
                    str = "Single \"" + str + "\"";
                print(node, str, depth);
            } else if (node instanceof Pattern.SliceNode) {
                str = name + "  \"" +
                      toStringCPS(((Pattern.SliceNode)node).buffer) + "\"";
                print(node, str, depth);
            } else if (node instanceof Pattern.CharPropertyGreedy gcp) {
                String pstr = pmap.get(gcp.predicate);
                if (pstr == null)
                    pstr = gcp.predicate.toString();
                else
                    pstr = "Single \"" + pstr + "\"";
                str = name + " " + pstr;
                if (gcp.cmin == 0)
                    str += "*";
                else if (gcp.cmin == 1)
                    str += "+";
                else
                    str += "{" + gcp.cmin + ",}";
                print(node, str, depth);
            } else if (node instanceof Pattern.BackRef) {
                str = "GroupBackRef " + ((Pattern.BackRef)node).groupIndex / 2;
                print(node, str, depth);
            } else if (node instanceof Pattern.LastNode) {
                print(node, "END", depth);
            } else if (node == Pattern.accept) {
                return;
            } else {
                print(node, name, depth);
            }
            node = node.next;
        }
    }

    public static void main(String[] args) {
        Pattern p = Pattern.compile(args[0]);
        System.out.println("   Pattern: " + p);
        walk(p.root, 0);
    }
}
