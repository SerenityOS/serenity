/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 7013272 7127924
 * @summary Automatically generate info about how compiler resource keys are used
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.util
 * @build Example ArgTypeCompilerFactory MessageFile MessageInfo
 * @run main/othervm MessageInfo
 */
/*
 *      See CR 7127924 for info on why othervm is used.
 */

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility to manipulate compiler.properties, and suggest info comments based
 * on information derived from running examples.
 *
 * Options:
 *   -examples dir   location of examples directory
 *   -o file         output file
 *   -check          just check message file
 *   -ensureNewlines ensure newline after each entry
 *   -fixIndent      fix indentation of continuation lines
 *   -sort           sort messages
 *   -verbose        verbose output
 *   -replace        replace comments instead of merging comments
 *   file            javac compiler.properties file
 *
 */
public class MessageInfo {
    public static void main(String... args) throws Exception {
        jtreg = (System.getProperty("test.src") != null);
        File tmpDir;
        if (jtreg) {
            // use standard jtreg scratch directory: the current directory
            tmpDir = new File(System.getProperty("user.dir"));
        } else {
            tmpDir = new File(System.getProperty("java.io.tmpdir"),
                    MessageInfo.class.getName()
                    + (new SimpleDateFormat("yyMMddHHmmss")).format(new Date()));
        }
        Example.setTempDir(tmpDir);
        Example.Compiler.factory = new ArgTypeCompilerFactory();

        MessageInfo mi = new MessageInfo();

        try {
            if (mi.run(args))
                return;
        } finally {
            /* VERY IMPORTANT NOTE. In jtreg mode, tmpDir is set to the
             * jtreg scratch directory, which is the current directory.
             * In case someone is faking jtreg mode, make sure to only
             * clean tmpDir when it is reasonable to do so.
             */
            if (tmpDir.isDirectory() &&
                    tmpDir.getName().startsWith(MessageInfo.class.getName())) {
                if (clean(tmpDir))
                    tmpDir.delete();
            }
        }

        if (jtreg)
            throw new Exception(mi.errors + " errors occurred");
        else
            System.exit(1);
    }

    void usage() {
        System.out.println("Usage:");
        System.out.println("    java MessageInfo [options] [file]");
        System.out.println("where options include");
        System.out.println("    -examples dir   location of examples directory");
        System.out.println("    -o file         output file");
        System.out.println("    -check          just check message file");
        System.out.println("    -ensureNewlines ensure newline after each entry");
        System.out.println("    -fixIndent      fix indentation of continuation lines");
        System.out.println("    -sort           sort messages");
        System.out.println("    -verbose        verbose output");
        System.out.println("    -replace        replace comments instead of merging comments");
        System.out.println("    file            javac compiler.properties file");
    }

    boolean run(String... args) {
        File testSrc = new File(System.getProperty("test.src", "."));
        File examplesDir = new File(testSrc, "examples");
        File notYetFile = null;
        File msgFile = null;
        File outFile = null;
        boolean verbose = false;
        boolean ensureNewlines = false;
        boolean fixIndent = false;
        boolean sort = false;
        boolean replace = false;
        boolean check = jtreg; // default true in jtreg mode

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-examples") && (i + 1) < args.length)
                examplesDir = new File(args[++i]);
            else if(arg.equals("-notyet") && (i + 1) < args.length)
                notYetFile = new File(args[++i]);
            else if (arg.equals("-ensureNewlines"))
                ensureNewlines = true;
            else if (arg.equals("-fixIndent"))
                fixIndent = true;
            else if (arg.equals("-sort"))
                sort = true;
            else if (arg.equals("-verbose"))
                verbose = true;
            else if (arg.equals("-replace"))
                replace = true;
            else if (arg.equals("-check"))
                check = true;
            else if (arg.equals("-o") && (i + 1) < args.length)
                outFile = new File(args[++i]);
            else if (arg.startsWith("-")) {
                error("unknown option: " + arg);
                return false;
            } else if (i == args.length - 1) {
                msgFile = new File(arg);
            } else {
                error("unknown arg: " + arg);
                return false;
            }
        }

        if (!check && outFile == null) {
            usage();
            return true;
        }

        if ((ensureNewlines || fixIndent || sort) && outFile == null) {
            error("must set output file for these options");
            return false;
        }

        if (notYetFile == null) {
            notYetFile = new File(examplesDir.getParentFile(), "examples.not-yet.txt");
        }

        if (msgFile == null) {
            for (File d = testSrc; d != null; d = d.getParentFile()) {
                if (new File(d, "TEST.ROOT").exists()) {
                    d = d.getParentFile();
                    File f = new File(d, "src/jdk.compiler/share/classes/com/sun/tools/javac/resources/compiler.properties");
                    if (f.exists()) {
                        msgFile = f;
                        break;
                    }
                }
            }
            if (msgFile == null) {
                if (jtreg) {
                    System.err.println("Warning: no message file available, test skipped");
                    return true;
                }
                error("no message file available");
                return false;
            }
        }

        MessageFile mf;
        try {
            mf = new MessageFile(msgFile);
        } catch (IOException e) {
            error("problem reading message file: " + e);
            return false;
        }

        Map<String, Set<String>> msgInfo = runExamples(examplesDir, verbose);

        if (ensureNewlines)
            ensureNewlines(mf);

        if (fixIndent)
            fixIndent(mf);

        if (sort)
            sort(mf, true);

        for (Map.Entry<String, Set<String>> e: msgInfo.entrySet()) {
            String k = e.getKey();
            Set<String> suggestions = e.getValue();
            MessageFile.Message m = mf.messages.get(k);
            if (m == null) {
                error("Can't find message for " + k + " in message file");
                continue;
            }

            MessageFile.Info info = m.getInfo();
            Set<Integer> placeholders = m.getPlaceholders();
            MessageFile.Info suggestedInfo = new MessageFile.Info(suggestions);
            suggestedInfo.markUnused(placeholders);

            if (!info.isEmpty()) {
                if (info.contains(suggestedInfo))
                    continue;
                if (!replace) {
                    if (info.fields.size() != suggestedInfo.fields.size())
                        error("Cannot merge info for " + k);
                    else
                        suggestedInfo.merge(info);
                }
            }

            if (outFile == null) {
                System.err.println("suggest for " + k);
                System.err.println(suggestedInfo.toComment());
            }  else
                m.setInfo(suggestedInfo);
        }

        if (check)
            check(mf, notYetFile);

        try {
            if (outFile != null)
                mf.write(outFile);
        } catch (IOException e) {
            error("problem writing file: " + e);
            return false;
        }

        return (errors == 0);
    }

    void check(MessageFile mf, File notYetFile) {
        Set<String> notYetList = null;
        for (Map.Entry<String, MessageFile.Message> e: mf.messages.entrySet()) {
            String key = e.getKey();
            MessageFile.Message m = e.getValue();
            if (m.needInfo() && m.getInfo().isEmpty()) {
                if (notYetList == null)
                    notYetList = getNotYetList(notYetFile);
                if (notYetList.contains(key))
                    System.err.println("Warning: no info for " + key);
                else
                    error("no info for " + key);
            }
        }

    }

    void ensureNewlines(MessageFile mf) {
        for (MessageFile.Message m: mf.messages.values()) {
            MessageFile.Line l = m.firstLine;
            while (l.text.endsWith("\\"))
                l = l.next;
            if (l.next != null && !l.next.text.isEmpty())
                l.insertAfter("");
        }
    }

    void fixIndent(MessageFile mf) {
        for (MessageFile.Message m: mf.messages.values()) {
            MessageFile.Line l = m.firstLine;
            while (l.text.endsWith("\\") && l.next != null) {
                if (!l.next.text.matches("^    \\S.*"))
                    l.next.text = "    " + l.next.text.trim();
                l = l.next;
            }
        }
    }

    void sort(MessageFile mf, boolean includePrecedingNewlines) {
        for (MessageFile.Message m: mf.messages.values()) {
            for (MessageFile.Line l: m.getLines(includePrecedingNewlines)) {
                l.remove();
                mf.lastLine.insertAfter(l);
            }
        }
    }

    Map<String, Set<String>> runExamples(File examplesDir, boolean verbose) {
        Map<String, Set<String>> map = new TreeMap<String, Set<String>>();
        for (Example e: getExamples(examplesDir)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.run(pw, true, verbose);
            pw.close();
            String[] lines = sw.toString().split("\n");
            for (String line: lines) {
                if (!line.startsWith("compiler."))
                    continue;
                int colon = line.indexOf(":");
                if (colon == -1)
                    continue;
                String key = line.substring(0, colon);
                StringBuilder sb = new StringBuilder();
                sb.append("# ");
                int i = 0;
                String[] descs = line.substring(colon + 1).split(", *");
                for (String desc: descs) {
                    if (i > 0) sb.append(", ");
                    sb.append(i++);
                    sb.append(": ");
                    sb.append(desc.trim());
                }
                Set<String> set = map.get(key);
                if (set == null)
                    map.put(key, set = new TreeSet<String>());
                set.add(sb.toString());
            }
        }

        return map;
    }

    /**
     * Get the complete set of examples to be checked.
     */
    Set<Example> getExamples(File examplesDir) {
        Set<Example> results = new TreeSet<Example>();
        for (File f: examplesDir.listFiles()) {
            if (isValidExample(f))
                results.add(new Example(f));
        }
        return results;
    }

    boolean isValidExample(File f) {
        return (f.isDirectory() && (!jtreg || f.list().length > 0)) ||
                (f.isFile() && f.getName().endsWith(".java"));
    }

    /**
     * Get the contents of the "not-yet" list.
     */
    Set<String> getNotYetList(File file) {
        Set<String> results = new TreeSet<String>();
        try {
            String[] lines = read(file).split("[\r\n]");
            for (String line: lines) {
                int hash = line.indexOf("#");
                if (hash != -1)
                    line = line.substring(0, hash).trim();
                if (line.matches("[A-Za-z0-9-_.]+"))
                    results.add(line);
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        return results;
    }

    /**
     * Read the contents of a file.
     */
    String read(File f) throws IOException {
        byte[] bytes = new byte[(int) f.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        try {
            in.readFully(bytes);
        } finally {
            in.close();
        }
        return new String(bytes);
    }

    /**
     * Report an error.
     */
    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    static boolean jtreg;

    int errors;

    /**
     * Clean the contents of a directory.
     */
    static boolean clean(File dir) {
        boolean ok = true;
        for (File f: dir.listFiles()) {
            if (f.isDirectory())
                ok &= clean(f);
            ok &= f.delete();
        }
        return ok;
    }

}
