/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8000612 8254627 8247994
 * @summary need test program to validate javadoc resource bundles
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 *          jdk.javadoc/jdk.javadoc.internal.doclets.formats.html.resources:open
 *          jdk.javadoc/jdk.javadoc.internal.doclets.toolkit.resources:open
 *          jdk.javadoc/jdk.javadoc.internal.tool.resources:open
 *          jdk.jdeps/com.sun.tools.classfile
 */

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.*;
import com.sun.tools.classfile.*;

/**
 * Compare string constants in javadoc classes against keys in javadoc resource bundles.
 */
public class CheckResourceKeys {
    /**
     * Main program.
     * Options:
     * -finddeadkeys
     *      look for keys in resource bundles that are no longer required
     * -findmissingkeys
     *      look for keys in resource bundles that are missing
     *
     * @throws Exception if invoked by jtreg and errors occur
     */
    public static void main(String... args) throws Exception {
        CheckResourceKeys c = new CheckResourceKeys();
        if (c.run(args))
            return;

        if (is_jtreg())
            throw new Exception(c.errors + " errors occurred");
        else
            System.exit(1);
    }

    static boolean is_jtreg() {
        return (System.getProperty("test.src") != null);
    }

    /**
     * Main entry point.
     */
    boolean run(String... args) throws Exception {
        boolean findDeadKeys = false;
        boolean findMissingKeys = false;

        if (args.length == 0) {
            if (is_jtreg()) {
                findDeadKeys = true;
                findMissingKeys = true;
            } else {
                System.err.println("Usage: java CheckResourceKeys <options>");
                System.err.println("where options include");
                System.err.println("  -finddeadkeys      find keys in resource bundles which are no longer required");
                System.err.println("  -findmissingkeys   find keys in resource bundles that are required but missing");
                return true;
            }
        } else {
            for (String arg: args) {
                if (arg.equalsIgnoreCase("-finddeadkeys"))
                    findDeadKeys = true;
                else if (arg.equalsIgnoreCase("-findmissingkeys"))
                    findMissingKeys = true;
                else
                    error("bad option: " + arg);
            }
        }

        if (errors > 0)
            return false;

        Set<String> codeKeys = getCodeKeys();
        Set<String> resourceKeys = getResourceKeys();

        System.err.println("found " + codeKeys.size() + " keys in code");
        System.err.println("found " + resourceKeys.size() + " keys in resource bundles");

        if (findDeadKeys)
            findDeadKeys(codeKeys, resourceKeys);

        if (findMissingKeys)
            findMissingKeys(codeKeys, resourceKeys);

        usageTests(false);
        usageTests(true);

        return (errors == 0);
    }

    void usageTests(boolean xflag) {
        String[] argarray = { xflag ? "-X" : "--help" };
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (jdk.javadoc.internal.tool.Main.execute(argarray, pw) == 0) {
            pw.flush();
            String s = sw.toString();
            if (s.isEmpty()) {
                error("no javadoc output ?");
                return;
            }
            if (sw.toString().contains("<MISSING KEY>")) {
                System.out.println(s);
                error("missing resources in output ?");
            }
        } else {
            error("failed to execute javadoc");
        }
    }

    /**
     * Find keys in resource bundles which are probably no longer required.
     * A key is required if there is a string in the code that is a resource key,
     * or if the key is well-known according to various pragmatic rules.
     */
    void findDeadKeys(Set<String> codeKeys, Set<String> resourceKeys) {
        for (String rk: resourceKeys) {
            // ignore these synthesized keys, tested by usageTests
            if (rk.startsWith("doclet.usage.") || rk.startsWith("doclet.xusage"))
                continue;
            // ignore these synthesized keys, tested by usageTests
            if (rk.matches("main\\.opt\\..*\\.(arg|desc)"))
                continue;
            // ignore this partial key
            if (rk.startsWith("doclet.Declared_Using_Preview."))
                continue;
            if (codeKeys.contains(rk))
                continue;

            error("Resource key not found in code: '" + rk + '"');
        }
    }

    /**
     * For all strings in the code that look like they might be
     * a resource key, verify that a key exists.
     */
    void findMissingKeys(Set<String> codeKeys, Set<String> resourceKeys) {
        for (String ck: codeKeys) {
            // ignore these synthesized keys, tested by usageTests
            if (ck.startsWith("doclet.usage.") || ck.startsWith("doclet.xusage."))
                continue;
            // ignore this partial key, tested by usageTests
            if (ck.equals("main.opt."))
                continue;
            // ignore these system property names
            if (ck.equals("javadoc.internal.show.taglets") || ck.equals("javadoc.legal-notices"))
                continue;
            if (resourceKeys.contains(ck))
                continue;
            error("No resource for \"" + ck + "\"");
        }
    }

    /**
     * Get the set of strings from (most of) the javadoc classfiles.
     */
    Set<String> getCodeKeys() throws IOException {
        Set<String> results = new TreeSet<String>();
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (JavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            JavaFileManager.Location javadocLoc = findJavadocLocation(fm);
            String[] pkgs = {
                "jdk.javadoc.internal.doclets",
                "jdk.javadoc.internal.tool"
            };
            for (String pkg: pkgs) {
                for (JavaFileObject fo: fm.list(javadocLoc,
                        pkg, EnumSet.of(JavaFileObject.Kind.CLASS), true)) {
                    String name = fo.getName();
                    // ignore resource files
                    if (name.matches(".*resources.[A-Za-z_0-9]+\\.class.*"))
                        continue;
                    scan(fo, results);
                }
            }

            // special handling for strings in search.js.template
            FileObject fo = fm.getFileForInput(javadocLoc,
                    "jdk.javadoc.internal.doclets.formats.html",
                    "resources/search.js.template");
            CharSequence search_js = fo.getCharContent(true);
            Pattern p = Pattern.compile("##REPLACE:(?<key>[A-Za-z0-9._]+)##");
            Matcher m = p.matcher(search_js);
            while (m.find()) {
                results.add(m.group("key"));
            }

            // special handling for code strings synthesized in
            // jdk.javadoc.internal.doclets.toolkit.util.Utils.getTypeName
            String[] extras = {
                "AnnotationType", "Class", "Enum", "EnumClass", "Error", "Exception", "Interface", "RecordClass"
            };
            for (String s: extras) {
                if (results.contains("doclet." + s))
                    results.add("doclet." + s.toLowerCase());
            }

            // special handling for code strings synthesized in
            // jdk.javadoc.internal.tool.JavadocLog
            results.add("javadoc.error.msg");
            results.add("javadoc.note.msg");
            results.add("javadoc.note.pos.msg");
            results.add("javadoc.warning.msg");

            results.add("javadoc.err.message");
            results.add("javadoc.warn.message");
            results.add("javadoc.note.message");

            return results;
        }
    }

    // depending on how the test is run, javadoc may be on bootclasspath or classpath
    JavaFileManager.Location findJavadocLocation(JavaFileManager fm) {
        JavaFileManager.Location[] locns =
            { StandardLocation.PLATFORM_CLASS_PATH, StandardLocation.CLASS_PATH };
        try {
            for (JavaFileManager.Location l: locns) {
                JavaFileObject fo = fm.getJavaFileForInput(l,
                    "jdk.javadoc.internal.tool.Main", JavaFileObject.Kind.CLASS);
                if (fo != null) {
                    System.err.println("found javadoc in " + l);
                    return l;
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        throw new IllegalStateException("Cannot find javadoc");
    }

    /**
     * Get the set of strings from a class file.
     * Only strings that look like they might be a resource key are returned.
     */
    void scan(JavaFileObject fo, Set<String> results) throws IOException {
        //System.err.println("scan " + fo.getName());
        InputStream in = fo.openInputStream();
        try {
            ClassFile cf = ClassFile.read(in);
            for (ConstantPool.CPInfo cpinfo: cf.constant_pool.entries()) {
                if (cpinfo.getTag() == ConstantPool.CONSTANT_Utf8) {
                    String v = ((ConstantPool.CONSTANT_Utf8_info) cpinfo).value;
                    if (v.matches("(doclet|main|javadoc|tag)\\.[A-Za-z0-9-_.]+"))
                        results.add(v);
                }
            }
        } catch (ConstantPoolException ignore) {
        } finally {
            in.close();
        }
    }

    /**
     * Get the set of keys from the javadoc resource bundles.
     */
    Set<String> getResourceKeys() {
        Module jdk_javadoc = ModuleLayer.boot().findModule("jdk.javadoc").get();
        String[] names = {
                "jdk.javadoc.internal.doclets.formats.html.resources.standard",
                "jdk.javadoc.internal.doclets.toolkit.resources.doclets",
                "jdk.javadoc.internal.tool.resources.javadoc",
        };
        Set<String> results = new TreeSet<String>();
        for (String name : names) {
            ResourceBundle b = ResourceBundle.getBundle(name, jdk_javadoc);
            results.addAll(b.keySet());
        }
        return results;
    }

    /**
     * Report an error.
     */
    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
