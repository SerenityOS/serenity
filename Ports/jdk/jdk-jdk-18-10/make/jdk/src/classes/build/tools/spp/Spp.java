/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.spp;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.regex.*;

/*
 * Spp: A simple regex-based stream preprocessor based on Mark Reinhold's
 *      sed-based spp.sh
 *
 * Usage:
 * java build.tools.spp.Spp [-be] [-nel] [-Kkey] -Dvar=value ... -iin -oout
 *
 * If -nel is declared then empty lines will not be substituted for lines of
 * text in the template that do not appear in the output.
 *
 *   Meaningful only at beginning of line, works with any number of keys:
 *
 *    #if[key]              Includes text between #if/#end if -Kkey specified,
 *    #else[key]            otherwise changes text to blank lines; key test
 *    #end[key]             may be negated by prefixing !, e.g., #if[!key]
 *
 *    #begin                If -be is specified then lines up to and including
 *    #end                  #begin, and from #end to EOF, are deleted
 *
 *    #warn                 Changed into warning that file is generated
 *
 *    // ##                 Changed into blank line
 *
 *  Meaningful anywhere in line
 *
 *    {#if[key]?yes}        Expands to yes if -Kkey specified
 *    {#if[key]?yes:no}     Expands to yes if -Kkey, otherwise no
 *    {#if[!key]?yes}       Expands to yes if -Kother
 *    {#if[!key]?yes:no}    Expands to yes if -Kother, otherwise no
 *    $var$                 Expands to value if -Dvar=value given
 *
 *    yes, no must not contain whitespace
 *
 * @author Xueming Shen
 */

public class Spp {
    public static void main(String args[]) throws Exception {
        Map<String, String> vars = new HashMap<>();
        Set<String> keys = new HashSet<>();
        boolean be = false;
        boolean el = true;
        String inputFile = null;
        String outputFile = null;

        for (String arg:args) {
            if (arg.startsWith("-D")) {
                int i = arg.indexOf('=');
                vars.put(arg.substring(2, i),arg.substring(i+1));
            } else if (arg.startsWith("-K")) {
                keys.add(arg.substring(2));
            } else if (arg.startsWith("-i")) {
                inputFile = arg.substring(2);
            } else if (arg.startsWith("-o")) {
                outputFile = arg.substring(2);
            } else if ("-be".equals(arg)) {
                be = true;
            } else if ("-nel".equals(arg)) {
                el = false;
            } else {
                System.err.println("Usage: java build.tools.spp.Spp [-be] [-nel] [-Kkey] -Dvar=value ... <in >out");
                System.exit(-1);
            }
        }

        StringBuffer out = new StringBuffer();
        new Spp().spp(new Scanner(new FileInputStream(inputFile)),
                      out, "",
                      keys, vars, be, el,
                      false);
        new FileOutputStream(outputFile, true).write(out.toString().getBytes());
    }

    static final String LNSEP = System.getProperty("line.separator");
    static final String KEY = "([a-zA-Z0-9]+)";
    static final String VAR = "([a-zA-Z0-9_\\-]+)";
    static final String TEXT = "([\\p{Print}&&[^{#:}]]+)";

    static final int GN_NOT = 1;
    static final int GN_KEY = 2;
    static final int GN_YES = 3;
    static final int GN_NO  = 5;
    static final int GN_VAR = 6;

    final Matcher   ifkey = Pattern.compile("^#if\\[(!)?" + KEY + "\\]").matcher("");
    final Matcher elsekey = Pattern.compile("^#else\\[(!)?" + KEY + "\\]").matcher("");
    final Matcher  endkey = Pattern.compile("^#end\\[(!)?" + KEY + "\\]").matcher("");
    final Matcher  vardef = Pattern.compile("\\{#if\\[(!)?" + KEY + "\\]\\?" + TEXT + "(:"+ TEXT + ")?\\}|\\$" + VAR + "\\$").matcher("");
    final Matcher vardef2 = Pattern.compile("\\$" + VAR + "\\$").matcher("");

    void append(StringBuffer buf, String ln,
                Set<String> keys, Map<String, String> vars) {
        vardef.reset(ln);
        while (vardef.find()) {
            String repl = "";
            if (vardef.group(GN_VAR) != null)
                repl = vars.get(vardef.group(GN_VAR));
            else {
                boolean test = keys.contains(vardef.group(GN_KEY));
                if (vardef.group(GN_NOT) != null)
                    test = !test;
                repl = test?vardef.group(GN_YES):vardef.group(GN_NO);
                if (repl == null)
                    repl = "";
                else {  // embedded $var$
                    while (vardef2.reset(repl).find()) {
                        repl = vardef2.replaceFirst(vars.get(vardef2.group(1)));
                    }
                }
            }
            if (repl == null) {
                System.err.println("Error: undefined variable in line " + ln);
                System.exit(-1);
            }
            vardef.appendReplacement(buf, repl);
        }
        vardef.appendTail(buf);
    }

    // return true if #end[key], #end or EOF reached
    boolean spp(Scanner in, StringBuffer buf, String key,
                Set<String> keys, Map<String, String> vars,
                boolean be, boolean el, boolean skip) {
        while (in.hasNextLine()) {
            String ln = in.nextLine();
            if (be) {
                if (ln.startsWith("#begin")) {
                    buf.setLength(0);      //clean up to this line
                    continue;
                }
                if (ln.equals("#end")) {
                    while (in.hasNextLine())
                        in.nextLine();
                    return true;           //discard the rest to EOF
                }
            }
            if (ifkey.reset(ln).find()) {
                String k = ifkey.group(GN_KEY);
                boolean test = keys.contains(k);
                if (ifkey.group(GN_NOT) != null)
                    test = !test;
                if (el) buf.append(LNSEP);
                if (!spp(in, buf, k, keys, vars, be, el, skip || !test)) {
                    spp(in, buf, k, keys, vars, be, el, skip || test);
                }
                continue;
            }
            if (elsekey.reset(ln).find()) {
                if (!key.equals(elsekey.group(GN_KEY))) {
                    throw new Error("Mis-matched #if-else-end at line <" + ln + ">");
                }
                if (el) buf.append(LNSEP);
                return false;
            }
            if (endkey.reset(ln).find()) {
                if (!key.equals(endkey.group(GN_KEY))) {
                    throw new Error("Mis-matched #if-else-end at line <" + ln + ">");
                }
                if (el) buf.append(LNSEP);
                return true;
            }
            if (ln.startsWith("#warn")) {
                ln = "// -- This file was mechanically generated: Do not edit! -- //";
            } else if (ln.trim().startsWith("// ##")) {
                ln = "";
            }
            if (!skip) {
                append(buf, ln, keys, vars);
                if (!el) buf.append(LNSEP);
            }
            if (el) buf.append(LNSEP);
        }
        return true;
    }
}
