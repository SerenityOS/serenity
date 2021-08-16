/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.testng.annotations.BeforeMethod;

import jdk.jshell.tool.JavaShellToolBuilder;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ReplToolTesting {

    private final static String DEFAULT_STARTUP_MESSAGE = "|  Welcome to";
    final static List<ImportInfo> START_UP_IMPORTS = Stream.of(
                    "java.io.*",
                    "java.math.*",
                    "java.net.*",
                    "java.nio.file.*",
                    "java.util.*",
                    "java.util.concurrent.*",
                    "java.util.function.*",
                    "java.util.prefs.*",
                    "java.util.regex.*",
                    "java.util.stream.*")
                    .map(s -> new ImportInfo("import " + s + ";", "", s))
                    .collect(toList());
    final static List<MethodInfo> START_UP_METHODS = Stream.<MethodInfo>of()
                    .collect(toList());
    final static List<String> START_UP_CMD_METHOD = Stream.<String>of()
                    .collect(toList());
    final static List<String> PRINTING_CMD_METHOD = Stream.of(
            "|    void print(boolean)",
            "|    void print(char)",
            "|    void print(int)",
            "|    void print(long)",
            "|    void print(float)",
            "|    void print(double)",
            "|    void print(char s[])",
            "|    void print(String)",
            "|    void print(Object)",
            "|    void println()",
            "|    void println(boolean)",
            "|    void println(char)",
            "|    void println(int)",
            "|    void println(long)",
            "|    void println(float)",
            "|    void println(double)",
            "|    void println(char s[])",
            "|    void println(String)",
            "|    void println(Object)",
            "|    void printf(java.util.Locale,String,Object...)",
            "|    void printf(String,Object...)")
            .collect(toList());
    final static List<String> START_UP = Collections.unmodifiableList(
            Stream.concat(START_UP_IMPORTS.stream(), START_UP_METHODS.stream())
            .map(s -> s.getSource())
            .collect(toList()));

    private WaitingTestingInputStream cmdin = null;
    private ByteArrayOutputStream cmdout = null;
    private ByteArrayOutputStream cmderr = null;
    private PromptedCommandOutputStream console = null;
    private TestingInputStream userin = null;
    private ByteArrayOutputStream userout = null;
    private ByteArrayOutputStream usererr = null;

    private List<MemberInfo> keys;
    private Map<String, VariableInfo> variables;
    private Map<String, MethodInfo> methods;
    private Map<String, ClassInfo> classes;
    private Map<String, ImportInfo> imports;
    private boolean isDefaultStartUp = true;
    protected Map<String, String> prefsMap;
    private Map<String, String> envvars;

    public interface ReplTest {
        void run(boolean after);
    }

    public void setCommandInput(String s) {
        cmdin.setInput(s);
    }

    public void closeCommandInput() {
        try {
            cmdin.close();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public final static Pattern idPattern = Pattern.compile("^\\s+(\\d+)");
    public Consumer<String> assertList() {
        return s -> {
            List<String> lines = Stream.of(s.split("\n"))
                    .filter(l -> !l.isEmpty())
                    .collect(Collectors.toList());
            int previousId = Integer.MIN_VALUE;
            assertEquals(lines.size(), keys.size(), "Number of keys");
            for (int i = 0; i < lines.size(); ++i) {
                String line = lines.get(i);
                Matcher matcher = idPattern.matcher(line);
                assertTrue(matcher.find(), "Snippet id not found: " + line);
                String src = keys.get(i).getSource();
                assertTrue(line.endsWith(src), "Line '" + line + "' does not end with: " + src);
                int id = Integer.parseInt(matcher.group(1));
                assertTrue(previousId < id,
                        String.format("The previous id is not less than the next one: previous: %d, next: %d",
                                previousId, id));
                previousId = id;
            }
        };
    }

    private final static Pattern extractPattern = Pattern.compile("^\\| *(.*)$");
    private Consumer<String> assertMembers(String message, Map<String, ? extends MemberInfo> set) {
        return s -> {
            List<String> lines = Stream.of(s.split("\n"))
                    .filter(l -> !l.isEmpty())
                    .filter(l -> !l.startsWith("|     ")) // error/unresolved info
                    .collect(Collectors.toList());
            assertEquals(lines.size(), set.size(), message + " : expected: " + set.keySet() + "\ngot:\n" + lines);
            for (String line : lines) {
                Matcher matcher = extractPattern.matcher(line);
                assertTrue(matcher.find(), line);
                String src = matcher.group(1);
                MemberInfo info = set.get(src);
                assertNotNull(info, "Not found snippet with signature: " + src + ", line: "
                        + line + ", keys: " + set.keySet() + "\n");
            }
        };
    }

    public Consumer<String> assertVariables() {
        return assertMembers("Variables", variables);
    }

    public Consumer<String> assertMethods() {
        return assertMembers("Methods", methods);
    }

    public Consumer<String> assertClasses() {
        return assertMembers("Classes", classes);
    }

    public Consumer<String> assertImports() {
        return assertMembers("Imports", imports);
    }

    public String getCommandOutput() {
        String s = normalizeLineEndings(cmdout.toString());
        cmdout.reset();
        return s;
    }

    public String getCommandErrorOutput() {
        String s = normalizeLineEndings(cmderr.toString());
        cmderr.reset();
        return s;
    }

    public void setUserInput(String s) {
        userin.setInput(s);
    }

    public String getUserOutput() {
        String s = normalizeLineEndings(userout.toString());
        userout.reset();
        return s;
    }

    public String getUserErrorOutput() {
        String s = normalizeLineEndings(usererr.toString());
        usererr.reset();
        return s;
    }

    public void test(ReplTest... tests) {
        test(new String[0], tests);
    }

    public void test(String[] args, ReplTest... tests) {
        test(true, args, tests);
    }

    public void test(boolean isDefaultStartUp, String[] args, ReplTest... tests) {
        test(Locale.ROOT, isDefaultStartUp, args, DEFAULT_STARTUP_MESSAGE, tests);
    }

    public void testNoStartUp(ReplTest... tests) {
        test(Locale.ROOT, false, new String[] {"--no-startup"}, DEFAULT_STARTUP_MESSAGE, tests);
    }

    public void test(Locale locale, boolean isDefaultStartUp, String[] args, String startUpMessage, ReplTest... tests) {
        this.isDefaultStartUp = isDefaultStartUp;
        initSnippets();
        ReplTest[] wtests = new ReplTest[tests.length + 3];
        wtests[0] = a -> assertCommandCheckOutput(a, "<start>",
                s -> assertTrue(s.startsWith(startUpMessage), "Expected start-up message '" + startUpMessage + "' Got: " + s));
        wtests[1] = a -> assertCommand(a, "/debug 0", null);
        System.arraycopy(tests, 0, wtests, 2, tests.length);
        wtests[tests.length + 2] = a -> assertCommand(a, "/exit", null);
        testRaw(locale, args, wtests);
    }

    private void initSnippets() {
        keys = new ArrayList<>();
        variables = new HashMap<>();
        methods = new HashMap<>();
        classes = new HashMap<>();
        imports = new HashMap<>();
        if (isDefaultStartUp) {
            methods.putAll(
                START_UP_METHODS.stream()
                    .collect(Collectors.toMap(Object::toString, Function.identity())));
            imports.putAll(
                START_UP_IMPORTS.stream()
                    .collect(Collectors.toMap(Object::toString, Function.identity())));
        }
    }

    @BeforeMethod
    public void setUp() {
        prefsMap = new HashMap<>();
        prefsMap.put("INDENT", "0");
        envvars = new HashMap<>();
        System.setProperty("jshell.test.allow.incomplete.inputs", "true");
    }

    protected void setEnvVar(String name, String value) {
        envvars.put(name, value);
    }

    protected JavaShellToolBuilder builder(Locale locale) {
        // turn on logging of launch failures
        Logger.getLogger("jdk.jshell.execution").setLevel(Level.ALL);
        return JavaShellToolBuilder
                    .builder()
                    .in(cmdin, userin)
                    .out(new PrintStream(cmdout), new PrintStream(console), new PrintStream(userout))
                    .err(new PrintStream(cmderr), new PrintStream(usererr))
                    .persistence(prefsMap)
                    .env(envvars)
                    .locale(locale)
                    .promptCapture(true);
    }

    private void testRaw(Locale locale, String[] args, ReplTest... tests) {
        testRawInit(tests);
        testRawRun(locale, args);
        testRawCheck(locale);
    }

    private void testRawInit(ReplTest... tests) {
        cmdin = new WaitingTestingInputStream();
        cmdout = new ByteArrayOutputStream();
        cmderr = new ByteArrayOutputStream();
        console = new PromptedCommandOutputStream(tests);
        userin = new TestingInputStream();
        userout = new ByteArrayOutputStream();
        usererr = new ByteArrayOutputStream();
    }

    protected void testRawRun(Locale locale, String[] args) {
        try {
            builder(locale)
                    .run(args);
        } catch (Exception ex) {
            fail("Repl tool died with exception", ex);
        }
    }

    private void testRawCheck(Locale locale) {
        // perform internal consistency checks on state, if desired
        String cos = getCommandOutput();
        String ceos = getCommandErrorOutput();
        String uos = getUserOutput();
        String ueos = getUserErrorOutput();
        assertTrue((cos.isEmpty() || cos.startsWith("|  Goodbye") || !locale.equals(Locale.ROOT)),
                "Expected a goodbye, but got: " + cos);
        assertTrue(ceos.isEmpty(), "Expected empty command error output, got: " + ceos);
        assertTrue(uos.isEmpty(), "Expected empty user output, got: " + uos);
        assertTrue(ueos.isEmpty(), "Expected empty user error output, got: " + ueos);
    }

    public void assertReset(boolean after, String cmd) {
        assertCommand(after, cmd, "|  Resetting state.\n");
        initSnippets();
    }

    public void evaluateExpression(boolean after, String type, String expr, String value) {
        String output = String.format("(\\$\\d+) ==> %s", value);
        Pattern outputPattern = Pattern.compile(output);
        assertCommandCheckOutput(after, expr, s -> {
            Matcher matcher = outputPattern.matcher(s);
            assertTrue(matcher.find(), "Output: '" + s + "' does not fit pattern: '" + output + "'");
            String name = matcher.group(1);
            VariableInfo tempVar = new TempVariableInfo(expr, type, name, value);
            variables.put(tempVar.toString(), tempVar);
            addKey(after, tempVar);
        });
    }

    public void loadVariable(boolean after, String type, String name) {
        loadVariable(after, type, name, null, null);
    }

    public void loadVariable(boolean after, String type, String name, String expr, String value) {
        String src = expr == null
                ? String.format("%s %s", type, name)
                : String.format("%s %s = %s", type, name, expr);
        VariableInfo var = expr == null
                ? new VariableInfo(src, type, name)
                : new VariableInfo(src, type, name, value);
        addKey(after, var, variables);
        addKey(after, var);
    }

    public void assertVariable(boolean after, String type, String name) {
        assertVariable(after, type, name, null, null);
    }

    public void assertVariable(boolean after, String type, String name, String expr, String value) {
        String src = expr == null
                ? String.format("%s %s", type, name)
                : String.format("%s %s = %s", type, name, expr);
        VariableInfo var = expr == null
                ? new VariableInfo(src, type, name)
                : new VariableInfo(src, type, name, value);
        assertCommandCheckOutput(after, src, var.checkOutput());
        addKey(after, var, variables);
        addKey(after, var);
    }

    public void loadMethod(boolean after, String src, String signature, String name) {
        MethodInfo method = new MethodInfo(src, signature, name);
        addKey(after, method, methods);
        addKey(after, method);
    }

    public void assertMethod(boolean after, String src, String signature, String name) {
        MethodInfo method = new MethodInfo(src, signature, name);
        assertCommandCheckOutput(after, src, method.checkOutput());
        addKey(after, method, methods);
        addKey(after, method);
    }

    public void loadClass(boolean after, String src, String type, String name) {
        ClassInfo clazz = new ClassInfo(src, type, name);
        addKey(after, clazz, classes);
        addKey(after, clazz);
    }

    public void assertClass(boolean after, String src, String type, String name) {
        ClassInfo clazz = new ClassInfo(src, type, name);
        assertCommandCheckOutput(after, src, clazz.checkOutput());
        addKey(after, clazz, classes);
        addKey(after, clazz);
    }

    public void loadImport(boolean after, String src, String type, String name) {
        ImportInfo i = new ImportInfo(src, type, name);
        addKey(after, i, imports);
        addKey(after, i);
    }

    public void assertImport(boolean after, String src, String type, String name) {
        ImportInfo i = new ImportInfo(src, type, name);
        assertCommandCheckOutput(after, src, i.checkOutput());
        addKey(after, i, imports);
        addKey(after, i);
    }

    private <T extends MemberInfo> void addKey(boolean after, T memberInfo, Map<String, T> map) {
        if (after) {
            map.entrySet().removeIf(e -> e.getValue().equals(memberInfo));
            map.put(memberInfo.toString(), memberInfo);
        }
    }

    private <T extends MemberInfo> void addKey(boolean after, T memberInfo) {
        if (after) {
            for (int i = 0; i < keys.size(); ++i) {
                MemberInfo m = keys.get(i);
                if (m.equals(memberInfo)) {
                    keys.set(i, memberInfo);
                    return;
                }
            }
            keys.add(memberInfo);
        }
    }

    private void dropKey(boolean after, String cmd, String name, Map<String, ? extends MemberInfo> map, String output) {
        assertCommand(after, cmd, output);
        if (after) {
            map.remove(name);
            for (int i = 0; i < keys.size(); ++i) {
                MemberInfo m = keys.get(i);
                if (m.toString().equals(name)) {
                    keys.remove(i);
                    return;
                }
            }
            throw new AssertionError("Key not found: " + name + ", keys: " + keys);
        }
    }

    public void dropVariable(boolean after, String cmd, String name, String output) {
        dropKey(after, cmd, name, variables, output);
    }

    public void dropMethod(boolean after, String cmd, String name, String output) {
        dropKey(after, cmd, name, methods, output);
    }

    public void dropClass(boolean after, String cmd, String name, String output) {
        dropKey(after, cmd, name, classes, output);
    }

    public void dropImport(boolean after, String cmd, String name, String output) {
        dropKey(after, cmd, name, imports, output);
    }

    public void assertCommand(boolean after, String cmd, String out) {
        assertCommand(after, cmd, out, "", null, "", "");
    }

    public void assertCommandOutputContains(boolean after, String cmd, String... hasThese) {
        assertCommandCheckOutput(after, cmd, (s)
                -> assertTrue(Arrays.stream(hasThese)
                                    .allMatch(has -> s.contains(has)),
                        "Output: \'" + s + "' does not contain: "
                                + Arrays.stream(hasThese)
                                        .filter(has -> !s.contains(has))
                                        .collect(Collectors.joining(", "))));
    }

    public void assertCommandOutputStartsWith(boolean after, String cmd, String starts) {
        assertCommandCheckOutput(after, cmd, assertStartsWith(starts));
    }

    public void assertCommandCheckOutput(boolean after, String cmd, Consumer<String> check) {
        if (!after) {
            assertCommand(false, cmd, null);
        } else {
            String got = getCommandOutput();
            check.accept(got);
            assertCommand(true, cmd, null);
        }
    }

    public void assertCommand(boolean after, String cmd, String out, String err,
            String userinput, String print, String usererr) {
        if (!after) {
            if (userinput != null) {
                setUserInput(userinput);
            }
            if (cmd.endsWith("\u0003")) {
                setCommandInput(cmd);
            } else {
                setCommandInput(cmd + "\n");
            }
        } else {
            assertOutput(getCommandOutput().trim(), out==null? out : out.trim(), "command output: " + cmd);
            assertOutput(getCommandErrorOutput(), err, "command error: " + cmd);
            assertOutput(getUserOutput(), print, "user output: " + cmd);
            assertOutput(getUserErrorOutput(), usererr, "user error: " + cmd);
        }
    }

    public Consumer<String> assertStartsWith(String prefix) {
        return (output) -> {
                            if (!output.trim().startsWith(prefix)) {
                                int i = 0;
        }
            assertTrue(output.trim().startsWith(prefix), "Output: \'" + output + "' does not start with: " + prefix);
        };
    }

    public void assertOutput(String got, String expected, String display) {
        if (expected != null) {
            assertEquals(got, expected, display + ".\n");
        }
    }

    private String normalizeLineEndings(String text) {
        return ANSI_CODE_PATTERN.matcher(text.replace(System.getProperty("line.separator"), "\n")).replaceAll("");
    }
        private static final Pattern ANSI_CODE_PATTERN = Pattern.compile("\033\\[[\060-\077]*[\040-\057]*[\100-\176]");

    public static abstract class MemberInfo {
        public final String source;
        public final String type;
        public final String name;

        public MemberInfo(String source, String type, String name) {
            this.source = source;
            this.type = type;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MemberInfo) {
                MemberInfo mi = (MemberInfo) o;
                return name.equals(mi.name);
            }
            return false;
        }

        public abstract Consumer<String> checkOutput();

        public String getSource() {
            return source;
        }
    }

    public static class VariableInfo extends MemberInfo {

        public final String value;
        public final String initialValue;

        public VariableInfo(String src, String type, String name) {
            super(src, type, name);
            this.initialValue = null;
            switch (type) {
                case "byte":
                case "short":
                case "int":
                case "long":
                    value = "0";
                    break;
                case "boolean":
                    value = "false";
                    break;
                case "char":
                    value = "''";
                    break;
                case "float":
                case "double":
                    value = "0.0";
                    break;
                default:
                    value = "null";
            }
        }

        public VariableInfo(String src, String type, String name, String value) {
            super(src, type, name);
            this.value = value;
            this.initialValue = value;
        }

        @Override
        public Consumer<String> checkOutput() {
            String arrowPattern = String.format("%s ==> %s", name, value);
            Predicate<String> arrowCheckOutput = Pattern.compile(arrowPattern).asPredicate();
            String howeverPattern = String.format("\\| *\\w+ variable %s, however*.", name);
            Predicate<String> howeverCheckOutput = Pattern.compile(howeverPattern).asPredicate();
            return output -> {
                if (output.startsWith("|  ")) {
                    assertTrue(howeverCheckOutput.test(output),
                    "Output: " + output + " does not fit pattern: " + howeverPattern);
                } else {
                    assertTrue(arrowCheckOutput.test(output),
                    "Output: " + output + " does not fit pattern: " + arrowPattern);
                }
            };
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof VariableInfo) {
                VariableInfo v = (VariableInfo) o;
                return name.equals(v.name);
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("%s %s = %s", type, name, value);
        }

        @Override
        public String getSource() {
            String src = super.getSource();
            return src.endsWith(";") ? src : src + ";";
        }
    }

    public static class TempVariableInfo extends VariableInfo {

        public TempVariableInfo(String src, String type, String name, String value) {
            super(src, type, name, value);
        }

        @Override
        public String getSource() {
            return source;
        }
    }

    public static class MethodInfo extends MemberInfo {

        public final String signature;

        public MethodInfo(String source, String signature, String name) {
            super(source, signature.substring(0, signature.lastIndexOf(')') + 1), name);
            this.signature = signature;
        }

        @Override
        public Consumer<String> checkOutput() {
            String expectedOutput = String.format("\\| *\\w+ method %s", name);
            Predicate<String> checkOutput = Pattern.compile(expectedOutput).asPredicate();
            return s -> assertTrue(checkOutput.test(s), "Expected: '" + expectedOutput + "', actual: " + s);
        }

        @Override
        public int hashCode() {
            return (name.hashCode() << 2) ^ type.hashCode() ;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MemberInfo) {
                MemberInfo m = (MemberInfo) o;
                return name.equals(m.name) && type.equals(m.type);
            }
            return false;
        }

        @Override
        public String toString() {
            int i = signature.lastIndexOf(")") + 1;
            if (i <= 0) {
                return String.format("%s", name);
            } else {
                return String.format("%s %s%s", signature.substring(i), name, signature.substring(0, i));
            }
        }
    }

    public static class ClassInfo extends MemberInfo {

        public ClassInfo(String source, String type, String name) {
            super(source, type, name);
        }

        @Override
        public Consumer<String> checkOutput() {
            String fullType = type.equals("@interface")? "annotation interface" : type;
            String expectedOutput = String.format("\\| *\\w+ %s %s", fullType, name);
            Predicate<String> checkOutput = Pattern.compile(expectedOutput).asPredicate();
            return s -> assertTrue(checkOutput.test(s), "Expected: '" + expectedOutput + "', actual: " + s);
        }

        @Override
        public int hashCode() {
            return name.hashCode() ;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ClassInfo) {
                ClassInfo c = (ClassInfo) o;
                return name.equals(c.name);
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("%s %s", type, name);
        }
    }

    public static class ImportInfo extends MemberInfo {
        public ImportInfo(String source, String type, String fullname) {
            super(source, type, fullname);
        }

        @Override
        public Consumer<String> checkOutput() {
            return s -> assertTrue("".equals(s), "Expected: '', actual: " + s);
        }

        @Override
        public int hashCode() {
            return (name.hashCode() << 2) ^ type.hashCode() ;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ImportInfo) {
                ImportInfo i = (ImportInfo) o;
                return name.equals(i.name) && type.equals(i.type);
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("import %s%s", type.equals("static") ? "static " : "", name);
        }
    }

    class WaitingTestingInputStream extends TestingInputStream {

        private boolean closed;

        @Override
        synchronized void setInput(String s) {
            super.setInput(s);
            notify();
        }

        synchronized void waitForInput() {
            boolean interrupted = false;
            try {
                while (available() == 0 && !closed) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                        // fall through and retry
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public int read() {
            waitForInput();
            return super.read();
        }

        @Override
        public int read(byte b[], int off, int len) {
            waitForInput();
            return super.read(b, off, len);
        }

        @Override
        public synchronized void close() throws IOException {
            closed = true;
            notify();
        }
    }

    class PromptedCommandOutputStream extends OutputStream {
        private final ReplTest[] tests;
        private int index = 0;
        PromptedCommandOutputStream(ReplTest[] tests) {
            this.tests = tests;
        }

        @Override
        public synchronized void write(int b) {
            if (b == 5 || b == 6) {
                if (index < (tests.length - 1)) {
                    tests[index].run(true);
                    tests[index + 1].run(false);
                } else {
                    fail("Did not exit Repl tool after test");
                }
                ++index;
            } // For now, anything else is thrown away
        }

        @Override
        public synchronized void write(byte b[], int off, int len) {
            if ((off < 0) || (off > b.length) || (len < 0)
                    || ((off + len) - b.length > 0)) {
                throw new IndexOutOfBoundsException();
            }
            for (int i = 0; i < len; ++i) {
                write(b[off + i]);
            }
        }
    }

    public static final class MemoryPreferences extends AbstractPreferences {

        private final Map<String, String> values = new HashMap<>();
        private final Map<String, MemoryPreferences> nodes = new HashMap<>();

        public MemoryPreferences() {
            this(null, "");
        }

        public MemoryPreferences(MemoryPreferences parent, String name) {
            super(parent, name);
        }

        @Override
        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi() throws BackingStoreException {
            ((MemoryPreferences) parent()).nodes.remove(name());
        }

        @Override
        protected String[] keysSpi() throws BackingStoreException {
            return values.keySet().toArray(new String[0]);
        }

        @Override
        protected String[] childrenNamesSpi() throws BackingStoreException {
            return nodes.keySet().toArray(new String[0]);
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            return nodes.computeIfAbsent(name, n -> new MemoryPreferences(this, name));
        }

        @Override
        protected void syncSpi() throws BackingStoreException {
        }

        @Override
        protected void flushSpi() throws BackingStoreException {
        }

    }
}
