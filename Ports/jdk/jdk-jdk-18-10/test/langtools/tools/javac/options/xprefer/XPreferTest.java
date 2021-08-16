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

/*
 * @test
 * @summary Tests which path is used to represent an implicit type given
 * various xprefer arguments and multiple .class / .java files involved.
 * @bug 8028196
 * @modules jdk.compiler
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;


public class XPreferTest {

    enum Dir {
        SOURCE_PATH("src"),
        CLASS_PATH("cp"),
        BOOT_PATH("boot");

        File file;
        Dir(String dir) {
            this.file = new File(dir);
        }
    }

    enum ImplicitOption {
        XPREFER_SOURCE("-Xprefer:source"),
        XPREFER_NEWER("-Xprefer:newer"),
        XXUSERPATHSFIRST("-XXuserPathsFirst");

        final String optionString;
        private ImplicitOption(String optionString) {
            this.optionString = optionString;
        }
    }

    final static JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
    final static StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null);
    final static File OUTPUT_DIR = new File("out");

    public static void main(String... args) throws Exception {
        try {
            // Initialize test-directories
            OUTPUT_DIR.mkdir();
            for (Dir dir : Dir.values())
                dir.file.mkdir();

            int testCaseCounter = 0;

            for (List<Dir> dirSubset : SubseqIter.subseqsOf(Dir.values())) {

                if (dirSubset.isEmpty())
                    continue;

                for (ImplicitOption policy : ImplicitOption.values()) {
                    for (List<Dir> dirOrder : PermutationIterator.permutationsOf(dirSubset)) {
                        new TestCase(dirOrder, policy, testCaseCounter++).run();
                    }
                }
            }
        } finally {
            fm.close();
        }
    }

    static class TestCase {

        String classId;
        List<Dir> dirs;
        ImplicitOption option;

        public TestCase(List<Dir> dirs, ImplicitOption option, int testCaseNum) {
            this.dirs = dirs;
            this.option = option;
            this.classId = "XPreferTestImplicit" + testCaseNum;
        }

        void run() throws Exception {

            System.out.println("Test:");
            System.out.println("    Class id: " + classId);
            System.out.println("    Dirs:     " + dirs);
            System.out.println("    Option:   " + option);

            createTestFiles();
            String compileOutput = compile();
            Dir actual = getChosenOrigin(compileOutput);
            Dir expected = getExpectedOrigin();

            System.out.println("    Expected: " + expected);
            System.out.println("    Actual:   " + actual);

            if (actual != expected) {
                throw new RuntimeException(String.format(
                        "Expected javac to choose %s but %s was chosen",
                        expected == null ? "<none>" : expected.name(),
                        actual   == null ? "<none>" : actual.name()));
            }
        }

        Dir getExpectedOrigin() {

            Dir newest = dirs.get(0);

            switch (option) {

            case XPREFER_NEWER:

                Dir cls = dirs.contains(Dir.BOOT_PATH) ? Dir.BOOT_PATH
                        : dirs.contains(Dir.CLASS_PATH) ? Dir.CLASS_PATH
                        : null;

                Dir src = dirs.contains(Dir.SOURCE_PATH) ? Dir.SOURCE_PATH
                        : null;

                for (Dir dir : dirs)
                    if (dir == cls || dir == src)
                        return dir;

                return null;

            case XPREFER_SOURCE:
                return dirs.contains(Dir.SOURCE_PATH) ? Dir.SOURCE_PATH
                     : dirs.contains(Dir.BOOT_PATH) ? Dir.BOOT_PATH
                     : dirs.contains(Dir.CLASS_PATH) ? Dir.CLASS_PATH
                     : null;

            case XXUSERPATHSFIRST:

                for (Dir dir : dirs)
                    if (dir == Dir.SOURCE_PATH || dir == Dir.CLASS_PATH)
                        return dir;

                // Neither SOURCE_PATH nor CLASS_PATH among dirs. Safty check:
                if (newest != Dir.BOOT_PATH)
                    throw new AssertionError("Expected to find BOOT_PATH");

                return Dir.BOOT_PATH;

            default:
                throw new RuntimeException("Unhandled policy case.");
            }
        }

        Dir getChosenOrigin(String compilerOutput) {
            Scanner s = new Scanner(compilerOutput);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.matches("\\[loading .*\\]")) {
                    for (Dir dir : Dir.values()) {
                        // On Windows all paths are printed with '/' except
                        // paths inside zip-files, which are printed with '\'.
                        // For this reason we accept both '/' and '\' below.
                        String regex = dir.file.getName() + "[\\\\/]" + classId;
                        if (Pattern.compile(regex).matcher(line).find())
                            return dir;
                    }
                }
            }
            return null;
        }

        String compile() throws IOException {

            // Create a class that references classId
            File explicit = new File("ExplicitClass.java");
            FileWriter filewriter = new FileWriter(explicit);
            filewriter.append("class ExplicitClass { " + classId + " implicit; }");
            filewriter.close();

            StringWriter sw = new StringWriter();

            com.sun.tools.javac.Main.compile(new String[] {
                    "-verbose",
                    option.optionString,
                    "-source", "8", "-target", "8",
                    "-sourcepath", Dir.SOURCE_PATH.file.getPath(),
                    "-classpath", Dir.CLASS_PATH.file.getPath(),
                    "-Xbootclasspath/p:" + Dir.BOOT_PATH.file.getPath(),
                    "-d", XPreferTest.OUTPUT_DIR.getPath(),
                    explicit.getPath()
            }, new PrintWriter(sw));

            return sw.toString();
        }

        void createTestFiles() throws IOException {
            long t = 1390927988755L;  // Tue Jan 28 17:53:08 CET 2014
            for (Dir dir : dirs) {
                createFile(dir).setLastModified(t);
                t -= 10000;
            }
        }

        File createFile(Dir dir) throws IOException {
            File src = new File(dir.file, classId + ".java");
            try (FileWriter w = new FileWriter(src)) {
                w.append("public class " + classId + " {}");
            }
            // If we're after the ".java" representation, we're done...
            if(dir == Dir.SOURCE_PATH)
                return src;
            // ...otherwise compile into a ".class".
            CompilationTask task = comp.getTask(null, fm, null, null, null,
                    fm.getJavaFileObjects(src));
            File dest = new File(dir.file, classId + ".class");
            if(!task.call() || !dest.exists())
                throw new RuntimeException("Compilation failure.");
            src.delete();
            return dest;
        }
    }
}

// Iterator for iteration over all subsequences of a given list.
class SubseqIter<T> implements Iterator<List<T>> {

    List<T> elements;
    boolean[] states;

    public SubseqIter(Collection<T> c) {
        states = new boolean[c.size()];
        elements = new ArrayList<T>(c);
    }

    public static <T> Iterable<List<T>> subseqsOf(final T[] t) {
        return new Iterable<List<T>>() {
            @Override
            public Iterator<List<T>> iterator() {
                return new SubseqIter<T>(Arrays.asList(t));
            }
        };
    }

    // Roll values in 'states' from index i and forward.
    // Return true if we wrapped back to zero.
    private boolean roll(int i) {
        if (i == states.length)
            return true;
        if (!roll(i + 1))
            return false;
        states[i] = !states[i];
        return !states[i];
    }

    @Override
    public List<T> next() {
        if (!hasNext())
            throw new NoSuchElementException();
        // Include element i if states[i] is true
        List<T> next = new ArrayList<T>();
        for (int i = 0; i < states.length; i++)
            if (states[i])
                next.add(elements.get(i));
        if (roll(0))
            states = null; // hasNext() == false from now on.
        return next;
    }

    @Override
    public boolean hasNext() {
        return states != null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

class PermutationIterator<T> implements Iterator<List<T>> {

    DirInt head;
    boolean hasNext = true;

    public PermutationIterator(List<T> toPermute) {
        ListIterator<T> iter = toPermute.listIterator();
        if (iter.hasNext())
            head = new DirInt(iter.nextIndex(), iter.next());
        DirInt prev = head;
        while (iter.hasNext()) {
            DirInt di = new DirInt(iter.nextIndex(), iter.next());
            di.left = prev;
            prev.right = di;
            prev = di;
        }
    }

    public static <T> Iterable<List<T>> permutationsOf(final List<T> list) {
        return new Iterable<List<T>>() {
            public Iterator<List<T>> iterator() {
                return new PermutationIterator<>(list);
            }
        };
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public List<T> next() {
        // Prep return value based on current state
        List<T> result = new ArrayList<>();
        for (DirInt di = head; di != null; di = di.right)
            result.add(di.object);

        // Step state forward
        DirInt maxMob = null;
        for (DirInt di = head; di != null; di = di.right)
            if (di.isMobile() && (maxMob == null || di.val > maxMob.val))
                maxMob = di;

        if (maxMob == null) {
            hasNext = false;
        } else {
            maxMob.swapWithAdjacent();
            for (DirInt di = head; di != null; di = di.right)
                if (di.val > maxMob.val)
                    di.facingLeft = !di.facingLeft;
        }
        return result;
    }

    private final class DirInt {
        int val;
        T object;
        DirInt left, right;
        boolean facingLeft = true;

        public DirInt(int val, T object) {
            this.val = val;
            this.object = object;
        }

        boolean isMobile() {
            DirInt adjacent = facingLeft ? left : right;
            return adjacent != null && val > adjacent.val;
        }

        public void swapWithAdjacent() {
            DirInt l = facingLeft ? left : this;
            DirInt r = facingLeft ? this : right;
            if (head == l) head = r;
            if (l.left  != null) l.left.right = r;
            if (r.right != null) r.right.left = l;
            l.right = r.right;
            r.left = l.left;
            r.right = l;
            l.left = r;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
