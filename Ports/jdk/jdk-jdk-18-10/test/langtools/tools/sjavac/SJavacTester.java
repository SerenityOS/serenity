/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

import com.sun.tools.sjavac.Main;

import toolbox.ToolBox;

public class SJavacTester {

    final ToolBox tb = new ToolBox();
    final Path TEST_ROOT = Paths.get(getClass().getSimpleName());

    // Generated sources that will test aspects of sjavac
    final Path GENSRC = TEST_ROOT.resolve("gensrc");
    // Gensrc dirs used to test merging of serveral source roots.
    final Path GENSRC2 = TEST_ROOT.resolve("gensrc2");
    final Path GENSRC3 = TEST_ROOT.resolve("gensrc3");

    // Dir for compiled classes.
    final Path BIN = TEST_ROOT.resolve("bin");
    // Dir for c-header files.
    final Path HEADERS = TEST_ROOT.resolve("headers");

    // Remember the previous bin and headers state here.
    Map<String,Long> previous_bin_state;
    Map<String,Long> previous_headers_state;

    void initialCompile() throws Exception {
        System.out.println("\nInitial compile of gensrc.");
        tb.writeFile(GENSRC.resolve("alfa/omega/AINT.java"),
                     "package alfa.omega; public interface AINT { void aint(); }");
        tb.writeFile(GENSRC.resolve("alfa/omega/A.java"),
                     "package alfa.omega; public class A implements AINT { "+
                     "public final static int DEFINITION = 17; public void aint() { } }");
        tb.writeFile(GENSRC.resolve("alfa/omega/AA.java"),
            "package alfa.omega;"+
            "// A package private class, not contributing to the public api.\n"+
            "class AA {"+
            "   // A properly nested static inner class.\n"+
            "    static class AAA { }\n"+
            "    // A properly nested inner class.\n"+
            "    class AAAA { }\n"+
            "    Runnable foo() {\n"+
            "        // A proper anonymous class.\n"+
            "        return new Runnable() { public void run() { } };\n"+
            "    }\n"+
            "    AAA aaa;\n"+
            "    AAAA aaaa;\n"+
            "    AAAAA aaaaa;\n"+
            "}\n"+
            "class AAAAA {\n"+
            "    // A bad auxiliary class, but no one is referencing it\n"+
            "    // from outside of this source file, therefore it is ok.\n"+
            "}\n");
        tb.writeFile(GENSRC.resolve("beta/BINT.java"),
                     "package beta;public interface BINT { void foo(); }");
        tb.writeFile(GENSRC.resolve("beta/B.java"),
                     "package beta; import alfa.omega.A; public class B {"+
                     "private int b() { return A.DEFINITION; } native void foo(); }");

        compile(GENSRC.toString(),
                "-d", BIN.toString(),
                "--state-dir=" + BIN,
                "-h", HEADERS.toString(),
                "-j", "1",
                "--log=debug");
    }

    void removeFrom(Path dir, String... args) throws IOException {
        for (String filename : args) {
            Path p = dir.resolve(filename);
            Files.delete(p);
        }
    }

    void compile(String... args) throws Exception {
        int rc = Main.go(args);
        if (rc != 0) throw new Exception("Error during compile!");

        // Wait a second, to get around the (temporary) problem with
        // second resolution in the Java file api. But do not do this
        // on windows where the timestamps work.
        long in_a_sec = System.currentTimeMillis()+1000;
        while (in_a_sec > System.currentTimeMillis()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    void compileExpectFailure(String... args) throws Exception {
        int rc = Main.go(args);
        if (rc == 0) throw new Exception("Expected error during compile! Did not fail!");
    }

    Map<String,Long> collectState(Path dir) throws IOException {
        final Map<String,Long> files = new HashMap<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                 @Override
                 public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                   throws IOException
                 {
                     files.put(file.toString(),new Long(Files.getLastModifiedTime(file).toMillis()));
                     return FileVisitResult.CONTINUE;
                 }
            });
        return files;
    }

    void verifyThatFilesHaveBeenRemoved(Map<String,Long> from,
                                        Map<String,Long> to,
                                        String... args) throws Exception {

        Set<String> froms = from.keySet();
        Set<String> tos = to.keySet();

        if (froms.equals(tos)) {
            throw new Exception("Expected new state to have fewer files than previous state!");
        }

        for (String t : tos) {
            if (!froms.contains(t)) {
                throw new Exception("Expected "+t+" to exist in previous state!");
            }
        }

        for (String f : args) {
            f = f.replace("/", File.separator);
            if (!froms.contains(f)) {
                throw new Exception("Expected "+f+" to exist in previous state!");
            }
            if (tos.contains(f)) {
                throw new Exception("Expected "+f+" to have been removed from the new state!");
            }
        }

        if (froms.size() - args.length != tos.size()) {
            throw new Exception("There are more removed files than the expected list!");
        }
    }

    void verifyThatFilesHaveBeenAdded(Map<String,Long> from,
                                      Map<String,Long> to,
                                      String... args) throws Exception {

        Set<String> froms = from.keySet();
        Set<String> tos = to.keySet();

        if (froms.equals(tos)) {
            throw new Exception("Expected new state to have more files than previous state!");
        }

        for (String t : froms) {
            if (!tos.contains(t)) {
                throw new Exception("Expected "+t+" to exist in new state!");
            }
        }

        for (String f : args) {
            f = f.replace("/", File.separator);
            if (!tos.contains(f)) {
                throw new Exception("Expected "+f+" to have been added to new state!");
            }
            if (froms.contains(f)) {
                throw new Exception("Expected "+f+" to not exist in previous state!");
            }
        }

        if (froms.size() + args.length != tos.size()) {
            throw new Exception("There are more added files than the expected list!");
        }
    }

    void verifyNewerFiles(Map<String,Long> from,
                          Map<String,Long> to,
                          String... args) throws Exception {
        if (!from.keySet().equals(to.keySet())) {
            throw new Exception("Expected the set of files to be identical!");
        }
        Set<String> files = new HashSet<String>();
        for (String s : args) {
            files.add(s.replace("/", File.separator));
        }
        for (String fn : from.keySet()) {
            long f = from.get(fn);
            long t = to.get(fn);
            if (files.contains(fn)) {
                if (t <= f) {
                    throw new Exception("Expected "+fn+" to have a more recent timestamp!");
                }
            } else {
                if (t != f) {
                    throw new Exception("Expected "+fn+" to have the same timestamp!");
                }
            }
        }
    }

    String print(Map<String,Long> m) {
        StringBuilder b = new StringBuilder();
        Set<String> keys = m.keySet();
        for (String k : keys) {
            b.append(k+" "+m.get(k)+"\n");
        }
        return b.toString();
    }

    void verifyEqual(Map<String,Long> from, Map<String,Long> to) throws Exception {
        if (!from.equals(to)) {
            System.out.println("FROM---"+print(from));
            System.out.println("TO-----"+print(to));
            throw new Exception("The dir should not differ! But it does!");
        }
    }
}
