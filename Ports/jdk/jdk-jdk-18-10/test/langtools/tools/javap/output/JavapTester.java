/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;

/**
 * {@code JavapTester} is an abstract test-driver that provides the logic
 * to execute test-cases, grouped by test classes.
 * A test class is a main class extending this class, that instantiate
 * itself, and calls the {@link run} method, passing any command line
 * arguments.
 * <p>
 * The {@code run} method, expects arguments to identify test-case classes.
 * A test-case class is a class extending the test class, and annotated
 * with {@code TestCase}.
 * <p>
 * If no test-cases are specified, the test class directory is searched for
 * co-located test-case classes (i.e. any class extending the test class,
 * annotated with  {@code TestCase}).
 * <p>
 * Besides serving to group test-cases, extending the driver allow
 * setting up a test-case template, and possibly overwrite default
 * test-driver behaviour.
 */
public abstract class JavapTester {

    private static boolean debug = false;
    private static final PrintStream out = System.err;
    private static final PrintStream err = System.err;


    protected void run(String... args) throws Exception {

        final File classesdir = new File(System.getProperty("test.classes", "."));

        String[] classNames = args;

        // If no test-cases are specified, we regard all co-located classes
        // as potential test-cases.
        if (args.length == 0) {
            final String pattern =  ".*\\.class";
            final File classFiles[] = classesdir.listFiles(new FileFilter() {
                    public boolean accept(File f) {
                        return f.getName().matches(pattern);
                    }
                });
            ArrayList<String> names = new ArrayList<String>(classFiles.length);
            for (File f : classFiles) {
                String fname = f.getName();
                names.add(fname.substring(0, fname.length() -6));
            }
            classNames = names.toArray(new String[names.size()]);
        } else {
            debug = true;
        }
        // Test-cases must extend the driver type, and be marked
        // @TestCase. Other arguments (classes) are ignored.
        // Test-cases are instantiated, and thereby executed.
        for (String clname : classNames) {
            try {
                final Class tclass = Class.forName(clname);
                if  (!getClass().isAssignableFrom(tclass)) continue;
                TestCase anno = (TestCase) tclass.getAnnotation(TestCase.class);
                if (anno == null) continue;
                if (!debug) {
                    ignore i = (ignore) tclass.getAnnotation(ignore.class);
                    if (i != null) {
                        out.println("Ignore: " + clname);
                        ignored++;
                        continue;
                    }
                }
                out.println("TestCase: " + clname);
                cases++;
                JavapTester tc = (JavapTester) tclass.getConstructor().newInstance();
                if (tc.errors > 0) {
                    error("" + tc.errors + " test points failed in " + clname);
                    errors += tc.errors - 1;
                    fcases++;
                }
            } catch(ReflectiveOperationException roe) {
                error("Warning: " + clname + " - ReflectiveOperationException");
                roe.printStackTrace(err);
            } catch(Exception unknown) {
                error("Warning: " + clname + " - uncaught exception");
                unknown.printStackTrace(err);
            }
        }

        String imsg = ignored > 0 ? " (" +  ignored + " ignored)" : "";
        if (errors > 0)
            throw new Error(errors + " error, in " + fcases + " of " + cases + " test-cases" + imsg);
        else
            err.println("" + cases + " test-cases executed" + imsg + ", no errors");
    }


    /**
     * Test-cases must be marked with the {@code TestCase} annotation,
     * as well as extend {@code JavapTester} (or an driver extension
     * specified as the first argument to the {@code main()} method.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface TestCase { }

    /**
     * Individual test-cases failing due to product bugs, may temporarily
     * be excluded by marking them like this, (where "at-" is replaced by "@")
     * at-ignore // 1234567: bug synopsis
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface ignore { }

    /**
     * Test-cases are classes extending {@code JavapTester}, and
     * calling {@link setSrc}, followed by one or more invocations
     * of {@link verify} in the body of the constructor.
     * <p>
     * Sets a default test-case template, which is empty except
     * for a key of {@code "TESTCASE"}.
     * Subclasses will typically call {@code setSrc(TestSource)}
     * to setup a useful test-case template.
     */
    public JavapTester() {
        this.testCase = this.getClass().getName();
        src = new TestSource("TESTCASE");
    }

    /**
     * Set the top-level source template.
     */
    protected JavapTester setSrc(TestSource src) {
        this.src = src;
        return this;
    }

    /**
     * Convenience method for calling {@code innerSrc("TESTCASE", ...)}.
     */
    protected JavapTester setSrc(String... lines) {
        return innerSrc("TESTCASE", lines);
    }

    /**
     * Convenience method for calling {@code innerSrc(key, new TestSource(...))}.
     */
    protected JavapTester innerSrc(String key, String... lines) {
        return innerSrc(key, new TestSource(lines));
    }

    /**
     * Specialize the testcase template, setting replacement content
     * for the specified key.
     */
    protected JavapTester innerSrc(String key, TestSource content) {
        if (src == null) {
            src = new TestSource(key);
        }
        src.setInner(key, content);
        return this;
    }

    /**
     * On the first invocation, call {@code execute()} to compile
     * the test-case source and process the resulting class(se)
     * into verifiable output.
     * <p>
     * Verify that the output matches each of the regular expressions
     * given as argument.
     * <p>
     * Any failure to match constitutes a test failure, but doesn't
     * abort the test-case.
     * <p>
     * Any exception (e.g. bad regular expression syntax) results in
     * a test failure, and aborts the test-case.
     */
    protected void verify(String... expect) {
        if (!didExecute) {
            try {
                execute();
            } catch(Exception ue) {
                throw new Error(ue);
            } finally {
                didExecute = true;
            }
        }
        if (output == null) {
            error("output is null");
            return;
        }
        for (String e: expect) {
            // Escape regular expressions (to allow input to be literals).
            // Notice, characters to be escaped are themselves identified
            // using regular expressions
            String rc[] = { "(", ")", "[", "]", "{", "}", "$" };
            for (String c : rc) {
                e = e.replace(c, "\\" + c);
            }
            // DEBUG: Uncomment this to test modulo constant pool index.
            // e = e.replaceAll("#[0-9]{2}", "#[0-9]{2}");
            if (!output.matches("(?s).*" + e + ".*")) {
                if (!didPrint) {
                    out.println(output);
                    didPrint = true;
                }
                error("not matched: '" + e + "'");
            } else if(debug) {
                out.println("matched: '" + e + "'");
            }
        }
    }

    /**
     * Calls {@code writeTestFile()} to write out the test-case source
     * content to a file, then call {@code compileTestFile()} to
     * compile it, and finally run the {@link process} method to produce
     * verifiable output. The default {@code process} method runs javap.
     * <p>
     * If an exception occurs, it results in a test failure, and
     * aborts the test-case.
     */
    protected void execute() throws IOException {
        err.println("TestCase: " + testCase);
        writeTestFile();
        compileTestFile();
        process();
    }

    /**
     * Generate java source from test-case.
     * TBD: change to use javaFileObject, possibly make
     * this class extend JavaFileObject.
     */
    protected void writeTestFile() throws IOException {
        javaFile = new File("Test.java");
        FileWriter fw = new FileWriter(javaFile);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter pw = new PrintWriter(bw);
        for (String line : src) {
            pw.println(line);
            if (debug) out.println(line);
        }
        pw.close();
    }

    /**
     * Compile the Java source code.
     */
    protected void compileTestFile() {
        String path = javaFile.getPath();
        String params[] =  {"-g", path };
        int rc = com.sun.tools.javac.Main.compile(params);
        if (rc != 0)
            throw new Error("compilation failed. rc=" + rc);
        classFile = new File(path.substring(0, path.length() - 5) + ".class");
    }


    /**
     * Process class file to generate output for verification.
     * The default implementation simply runs javap. This might be
     * overwritten to generate output in a different manner.
     */
    protected void process() {
        String testClasses = "."; //System.getProperty("test.classes", ".");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] args = { "-v", "-classpath", testClasses, "Test" };
        int rc = com.sun.tools.javap.Main.run(args, pw);
        if (rc != 0)
            throw new Error("javap failed. rc=" + rc);
        pw.close();
        output = sw.toString();
        if (debug) {
            out.println(output);
            didPrint = true;
        }

    }


    private String testCase;
    private TestSource src;
    private File javaFile = null;
    private File classFile = null;
    private String output = null;
    private boolean didExecute = false;
    private boolean didPrint = false;


    protected void error(String msg) {
        err.println("Error: " + msg);
        errors++;
    }

    private int cases;
    private int fcases;
    private int errors;
    private int ignored;

    /**
     * The TestSource class provides a simple container for
     * test cases. It contains an array of source code lines,
     * where zero or more lines may be markers for nested lines.
     * This allows representing templates, with specialization.
     * <P>
     * This may be generalized to support more advance combo
     * tests, but presently it's only used with a static template,
     * and one level of specialization.
     */
    public class TestSource implements Iterable<String> {

        private String[] lines;
        private Hashtable<String, TestSource> innerSrc;

        public TestSource(String... lines) {
            this.lines = lines;
            innerSrc = new Hashtable<String, TestSource>();
        }

        public void setInner(String key, TestSource inner) {
            innerSrc.put(key, inner);
        }

        public void setInner(String key, String... lines) {
            innerSrc.put(key, new TestSource(lines));
        }

        public Iterator<String> iterator() {
            return new LineIterator();
        }

        private class LineIterator implements Iterator<String> {

            int nextLine = 0;
            Iterator<String> innerIt = null;

            public  boolean hasNext() {
                return nextLine < lines.length;
            }

            public String next() {
                if (!hasNext()) throw new NoSuchElementException();
                String str = lines[nextLine];
                TestSource inner = innerSrc.get(str);
                if (inner == null) {
                    nextLine++;
                    return str;
                }
                if (innerIt == null) {
                    innerIt = inner.iterator();
                }
                if (innerIt.hasNext()) {
                    return innerIt.next();
                }
                innerIt = null;
                nextLine++;
                return next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
