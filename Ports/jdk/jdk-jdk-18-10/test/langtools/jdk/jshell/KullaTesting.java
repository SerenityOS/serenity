/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.Diagnostic;

import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.JShell.Subscription;
import jdk.jshell.Snippet;
import jdk.jshell.DeclarationSnippet;
import jdk.jshell.ExpressionSnippet;
import jdk.jshell.ImportSnippet;
import jdk.jshell.Snippet.Kind;
import jdk.jshell.MethodSnippet;
import jdk.jshell.Snippet.Status;
import jdk.jshell.Snippet.SubKind;
import jdk.jshell.TypeDeclSnippet;
import jdk.jshell.VarSnippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.SourceCodeAnalysis.CompletionInfo;
import jdk.jshell.SourceCodeAnalysis.Completeness;
import jdk.jshell.SourceCodeAnalysis.QualifiedNames;
import jdk.jshell.SourceCodeAnalysis.Suggestion;
import jdk.jshell.UnresolvedReferenceException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import jdk.jshell.Diag;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static jdk.jshell.Snippet.Status.*;
import static org.testng.Assert.*;
import static jdk.jshell.Snippet.SubKind.METHOD_SUBKIND;
import jdk.jshell.SourceCodeAnalysis.Documentation;

public class KullaTesting {

    public static final String IGNORE_VALUE = "<ignore-value>";
    public static final Class<? extends Throwable> IGNORE_EXCEPTION = (new Throwable() {}).getClass();
    public static final Snippet MAIN_SNIPPET;

    private SourceCodeAnalysis analysis = null;
    private JShell state = null;
    private InputStream inStream = null;
    private ByteArrayOutputStream outStream = null;
    private ByteArrayOutputStream errStream = null;

    private Map<String, Snippet> idToSnippet = new LinkedHashMap<>();
    private Set<Snippet> allSnippets = new LinkedHashSet<>();

    static {
        JShell js = JShell.create();
        MAIN_SNIPPET = js.eval("MAIN_SNIPPET").get(0).snippet();
        js.close();
        assertTrue(MAIN_SNIPPET != null, "Bad MAIN_SNIPPET set-up -- must not be null");
    }

    public enum DiagCheck {
        DIAG_OK,
        DIAG_WARNING,
        DIAG_ERROR,
        DIAG_IGNORE
    }

    public void setInput(String s) {
        setInput(new ByteArrayInputStream(s.getBytes()));
    }

    public void setInput(InputStream in) {
        inStream = in;
    }

    public String getOutput() {
        String s = outStream.toString();
        outStream.reset();
        return s;
    }

    public String getErrorOutput() {
        String s = errStream.toString();
        errStream.reset();
        return s;
    }

    /**
     * @return the analysis
     */
    public SourceCodeAnalysis getAnalysis() {
        if (analysis == null) {
            analysis = state.sourceCodeAnalysis();
        }
        return analysis;
    }

    /**
     * @return the state
     */
    public JShell getState() {
        return state;
    }

    public List<Snippet> getActiveKeys() {
        return allSnippets.stream()
                .filter(k -> getState().status(k).isActive())
                .collect(Collectors.toList());
    }

    public void addToClasspath(String path) {
        getState().addToClasspath(path);
    }

    public void addToClasspath(Path path) {
        addToClasspath(path.toString());
    }

    @BeforeMethod
    public void setUp() {
        setUp(b -> {});
    }

    public void setUp(Consumer<JShell.Builder> bc) {
        InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                assertNotNull(inStream);
                return inStream.read();
            }
            @Override
            public int read(byte[] b) throws IOException {
                assertNotNull(inStream);
                return inStream.read(b);
            }
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                assertNotNull(inStream);
                return inStream.read(b, off, len);
            }
        };
        outStream = new ByteArrayOutputStream();
        errStream = new ByteArrayOutputStream();
        JShell.Builder builder = JShell.builder()
                .in(in)
                .out(new PrintStream(outStream))
                .err(new PrintStream(errStream));
        bc.accept(builder);
        state = builder.build();
        allSnippets = new LinkedHashSet<>();
        idToSnippet = new LinkedHashMap<>();
    }

    @AfterMethod
    public void tearDown() {
        if (state != null) state.close();
        state = null;
        analysis = null;
        allSnippets = null;
        idToSnippet = null;
    }

    public ClassLoader createAndRunFromModule(String moduleName, Path modPath) {
        ModuleFinder finder = ModuleFinder.of(modPath);
        ModuleLayer parent = ModuleLayer.boot();
        Configuration cf = parent.configuration()
                .resolve(finder, ModuleFinder.of(), Set.of(moduleName));
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer = parent.defineModulesWithOneLoader(cf, scl);
        ClassLoader loader = layer.findLoader(moduleName);
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        return ccl;
    }

    public List<String> assertUnresolvedDependencies(DeclarationSnippet key, int unresolvedSize) {
        List<String> unresolved = getState().unresolvedDependencies(key).collect(toList());
        assertEquals(unresolved.size(), unresolvedSize, "Input: " + key.source() + ", checking unresolved: ");
        return unresolved;
    }

    public DeclarationSnippet assertUnresolvedDependencies1(DeclarationSnippet key, Status status, String name) {
        List<String> unresolved = assertUnresolvedDependencies(key, 1);
        String input = key.source();
        assertEquals(unresolved.size(), 1, "Given input: " + input + ", checking unresolved");
        assertEquals(unresolved.get(0), name, "Given input: " + input + ", checking unresolved: ");
        assertEquals(getState().status(key), status, "Given input: " + input + ", checking status: ");
        return key;
    }

    public DeclarationSnippet assertEvalUnresolvedException(String input, String name, int unresolvedSize, int diagnosticsSize) {
        List<SnippetEvent> events = assertEval(input, null, UnresolvedReferenceException.class, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, null);
        SnippetEvent ste = events.get(0);
        DeclarationSnippet sn = ((UnresolvedReferenceException) ste.exception()).getSnippet();
        assertEquals(sn.name(), name, "Given input: " + input + ", checking name");
        assertEquals(getState().unresolvedDependencies(sn).count(), unresolvedSize, "Given input: " + input + ", checking unresolved");
        assertEquals(getState().diagnostics(sn).count(), (long) diagnosticsSize, "Given input: " + input + ", checking diagnostics");
        return sn;
    }

    public Snippet assertKeyMatch(String input, boolean isExecutable, SubKind expectedSubKind, STEInfo mainInfo, STEInfo... updates) {
        Snippet key = key(assertEval(input, IGNORE_VALUE, mainInfo, updates));
        String source = key.source();
        assertEquals(source, input, "Key \"" + input + "\" source mismatch, got: " + source + ", expected: " + input);
        SubKind subkind = key.subKind();
        assertEquals(subkind, expectedSubKind, "Key \"" + input + "\" subkind mismatch, got: "
                + subkind + ", expected: " + expectedSubKind);
        assertEquals(subkind.isExecutable(), isExecutable, "Key \"" + input + "\", expected isExecutable: "
                + isExecutable + ", got: " + subkind.isExecutable());
        Snippet.Kind expectedKind = getKind(key);
        assertEquals(key.kind(), expectedKind, "Checking kind: ");
        assertEquals(expectedSubKind.kind(), expectedKind, "Checking kind: ");
        return key;
    }

    private Kind getKind(Snippet key) {
        SubKind expectedSubKind = key.subKind();
        Kind expectedKind;
        switch (expectedSubKind) {
            case SINGLE_TYPE_IMPORT_SUBKIND:
            case SINGLE_STATIC_IMPORT_SUBKIND:
            case TYPE_IMPORT_ON_DEMAND_SUBKIND:
            case STATIC_IMPORT_ON_DEMAND_SUBKIND:
                expectedKind = Kind.IMPORT;
                break;
            case CLASS_SUBKIND:
            case INTERFACE_SUBKIND:
            case ENUM_SUBKIND:
            case ANNOTATION_TYPE_SUBKIND:
                expectedKind = Kind.TYPE_DECL;
                break;
            case METHOD_SUBKIND:
                expectedKind = Kind.METHOD;
                break;
            case VAR_DECLARATION_SUBKIND:
            case TEMP_VAR_EXPRESSION_SUBKIND:
            case VAR_DECLARATION_WITH_INITIALIZER_SUBKIND:
                expectedKind = Kind.VAR;
                break;
            case VAR_VALUE_SUBKIND:
            case ASSIGNMENT_SUBKIND:
                expectedKind = Kind.EXPRESSION;
                break;
            case STATEMENT_SUBKIND:
                expectedKind = Kind.STATEMENT;
                break;
            case UNKNOWN_SUBKIND:
                expectedKind = Kind.ERRONEOUS;
                break;
            default:
                throw new AssertionError("Unsupported key: " + key.getClass().getCanonicalName());
        }
        return expectedKind;
    }

    public ImportSnippet assertImportKeyMatch(String input, String name, SubKind subkind, STEInfo mainInfo, STEInfo... updates) {
        Snippet key = assertKeyMatch(input, false, subkind, mainInfo, updates);

        assertTrue(key instanceof ImportSnippet, "Expected an ImportKey, got: " + key.getClass().getName());
        ImportSnippet importKey = (ImportSnippet) key;
        assertEquals(importKey.name(), name, "Input \"" + input +
                "\" name mismatch, got: " + importKey.name() + ", expected: " + name);
        assertEquals(importKey.kind(), Kind.IMPORT, "Checking kind: ");
        return importKey;
    }

    public DeclarationSnippet assertDeclarationKeyMatch(String input, boolean isExecutable, String name, SubKind subkind, STEInfo mainInfo, STEInfo... updates) {
        Snippet key = assertKeyMatch(input, isExecutable, subkind, mainInfo, updates);

        assertTrue(key instanceof DeclarationSnippet, "Expected a DeclarationKey, got: " + key.getClass().getName());
        DeclarationSnippet declKey = (DeclarationSnippet) key;
        assertEquals(declKey.name(), name, "Input \"" + input +
                "\" name mismatch, got: " + declKey.name() + ", expected: " + name);
        return declKey;
    }

    public VarSnippet assertVarKeyMatch(String input, boolean isExecutable, String name, SubKind kind, String typeName, STEInfo mainInfo, STEInfo... updates) {
        Snippet sn = assertDeclarationKeyMatch(input, isExecutable, name, kind, mainInfo, updates);
        assertTrue(sn instanceof VarSnippet, "Expected a VarKey, got: " + sn.getClass().getName());
        VarSnippet variableKey = (VarSnippet) sn;
        String signature = variableKey.typeName();
        assertEquals(signature, typeName, "Key \"" + input +
                "\" typeName mismatch, got: " + signature + ", expected: " + typeName);
        assertEquals(variableKey.kind(), Kind.VAR, "Checking kind: ");
        return variableKey;
    }

    public void assertExpressionKeyMatch(String input, String name, SubKind kind, String typeName) {
        Snippet key = assertKeyMatch(input, true, kind, added(VALID));
        assertTrue(key instanceof ExpressionSnippet, "Expected a ExpressionKey, got: " + key.getClass().getName());
        ExpressionSnippet exprKey = (ExpressionSnippet) key;
        assertEquals(exprKey.name(), name, "Input \"" + input +
                "\" name mismatch, got: " + exprKey.name() + ", expected: " + name);
        assertEquals(exprKey.typeName(), typeName, "Key \"" + input +
                "\" typeName mismatch, got: " + exprKey.typeName() + ", expected: " + typeName);
        assertEquals(exprKey.kind(), Kind.EXPRESSION, "Checking kind: ");
    }

    // For expressions throwing an EvalException
    public SnippetEvent assertEvalException(String input) {
        List<SnippetEvent> events = assertEval(input, null, EvalException.class,
                DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, null);
        return events.get(0);
    }


    public List<SnippetEvent> assertEvalFail(String input) {
        return assertEval(input, null, null,
                DiagCheck.DIAG_ERROR, DiagCheck.DIAG_IGNORE, added(REJECTED));
    }

    public List<SnippetEvent> assertEval(String input) {
        return assertEval(input, IGNORE_VALUE, null, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, added(VALID));
    }

    public List<SnippetEvent> assertEval(String input, String value) {
        return assertEval(input, value, null, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, added(VALID));
    }

    public List<SnippetEvent> assertEval(String input, STEInfo mainInfo, STEInfo... updates) {
        return assertEval(input, IGNORE_VALUE, null, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, mainInfo, updates);
    }

    public List<SnippetEvent> assertEval(String input, String value,
            STEInfo mainInfo, STEInfo... updates) {
        return assertEval(input, value, null, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, mainInfo, updates);
    }

    public List<SnippetEvent> assertEval(String input, DiagCheck diagMain, DiagCheck diagUpdates) {
        return assertEval(input, IGNORE_VALUE, null, diagMain, diagUpdates, added(VALID));
    }

    public List<SnippetEvent> assertEval(String input, DiagCheck diagMain, DiagCheck diagUpdates,
            STEInfo mainInfo, STEInfo... updates) {
        return assertEval(input, IGNORE_VALUE, null, diagMain, diagUpdates, mainInfo, updates);
    }

    public List<SnippetEvent> assertEval(String input,
            String value, Class<? extends Throwable> exceptionClass,
            DiagCheck diagMain, DiagCheck diagUpdates,
            STEInfo mainInfo, STEInfo... updates) {
        return assertEval(input, diagMain, diagUpdates, new EventChain(mainInfo, value, exceptionClass, updates));
    }

    // Use this directly or usually indirectly for all non-empty calls to eval()
    public List<SnippetEvent> assertEval(String input,
           DiagCheck diagMain, DiagCheck diagUpdates, EventChain... eventChains) {
        return checkEvents(() -> getState().eval(input), "eval(" + input + ")", diagMain, diagUpdates, eventChains);
    }

    <T> void assertStreamMatch(Stream<T> result, T... expected) {
        Set<T> sns = result.collect(toSet());
        Set<T> exp = Stream.of(expected).collect(toSet());
        assertEquals(sns, exp);
    }

    private Map<Snippet, Snippet> closure(List<SnippetEvent> events) {
        Map<Snippet, Snippet> transitions = new HashMap<>();
        for (SnippetEvent event : events) {
            transitions.put(event.snippet(), event.causeSnippet());
        }
        Map<Snippet, Snippet> causeSnippets = new HashMap<>();
        for (Map.Entry<Snippet, Snippet> entry : transitions.entrySet()) {
            Snippet snippet = entry.getKey();
            Snippet cause = getInitialCause(transitions, entry.getValue());
            causeSnippets.put(snippet, cause);
        }
        return causeSnippets;
    }

    private Snippet getInitialCause(Map<Snippet, Snippet> transitions, Snippet snippet) {
        Snippet result;
        while ((result = transitions.get(snippet)) != null) {
            snippet = result;
        }
        return snippet;
    }

    private Map<Snippet, List<SnippetEvent>> groupByCauseSnippet(List<SnippetEvent> events) {
        Map<Snippet, List<SnippetEvent>> map = new TreeMap<>((a, b) -> a.id().compareTo(b.id()));
        for (SnippetEvent event : events) {
            if (event == null) {
                throw new InternalError("null event found in " + events);
            }
            if (event.snippet() == null) {
                throw new InternalError("null event Snippet found in " + events);
            }
            if (event.snippet().id() == null) {
                throw new InternalError("null event Snippet id() found in " + events);
            }
        }
        for (SnippetEvent event : events) {
            if (event.causeSnippet() == null) {
                map.computeIfAbsent(event.snippet(), ($) -> new ArrayList<>()).add(event);
            }
        }
        Map<Snippet, Snippet> causeSnippets = closure(events);
        for (SnippetEvent event : events) {
            Snippet causeSnippet = causeSnippets.get(event.snippet());
            if (causeSnippet != null) {
                map.get(causeSnippet).add(event);
            }
        }
        for (Map.Entry<Snippet, List<SnippetEvent>> entry : map.entrySet()) {
            Collections.sort(entry.getValue(),
                    (a, b) -> a.causeSnippet() == null
                            ? -1 : b.causeSnippet() == null
                            ? 1 : a.snippet().id().compareTo(b.snippet().id()));
        }
        return map;
    }

    private List<STEInfo> getInfos(EventChain... eventChains) {
        List<STEInfo> list = new ArrayList<>();
        for (EventChain i : eventChains) {
            list.add(i.mainInfo);
            Collections.addAll(list, i.updates);
        }
        return list;
    }

    private List<SnippetEvent> checkEvents(Supplier<List<SnippetEvent>> toTest,
             String descriptor,
             DiagCheck diagMain, DiagCheck diagUpdates,
             EventChain... eventChains) {
        List<SnippetEvent> dispatched = new ArrayList<>();
        Subscription token = getState().onSnippetEvent(kse -> {
            if (dispatched.size() > 0 && dispatched.get(dispatched.size() - 1) == null) {
                throw new RuntimeException("dispatch event after done");
            }
            dispatched.add(kse);
        });
        List<SnippetEvent> events = toTest.get();
        getState().unsubscribe(token);
        assertEquals(dispatched.size(), events.size(), "dispatched event size not the same as event size");
        for (int i = events.size() - 1; i >= 0; --i) {
            assertEquals(dispatched.get(i), events.get(i), "Event element " + i + " does not match");
        }
        dispatched.add(null); // mark end of dispatchs

        for (SnippetEvent evt : events) {
            assertTrue(evt.snippet() != null, "key must never be null, but it was for: " + descriptor);
            assertTrue(evt.previousStatus() != null, "previousStatus must never be null, but it was for: " + descriptor);
            assertTrue(evt.status() != null, "status must never be null, but it was for: " + descriptor);
            assertTrue(evt.status() != NONEXISTENT, "status must not be NONEXISTENT: " + descriptor);
            if (evt.previousStatus() != NONEXISTENT) {
                Snippet old = idToSnippet.get(evt.snippet().id());
                if (old != null) {
                    switch (evt.status()) {
                        case DROPPED:
                            assertEquals(old, evt.snippet(),
                                    "Drop: Old snippet must be what is dropped -- input: " + descriptor);
                            break;
                        case OVERWRITTEN:
                            assertEquals(old, evt.snippet(),
                                    "Overwrite: Old snippet (" + old
                                    + ") must be what is overwritten -- input: "
                                    + descriptor + " -- " + evt);
                            break;
                        default:
                            if (evt.causeSnippet() == null) {
                                // New source
                                assertNotEquals(old, evt.snippet(),
                                        "New source: Old snippet must be different from the replacing -- input: "
                                        + descriptor);
                            } else {
                                // An update (key Overwrite??)
                                assertEquals(old, evt.snippet(),
                                        "Update: Old snippet must be equal to the replacing -- input: "
                                        + descriptor);
                            }
                            break;
                    }
                }
            }
        }
        for (SnippetEvent evt : events) {
            if (evt.causeSnippet() == null && evt.status() != DROPPED) {
                allSnippets.add(evt.snippet());
                idToSnippet.put(evt.snippet().id(), evt.snippet());
            }
        }
        assertTrue(events.size() >= 1, "Expected at least one event, got none.");
        List<STEInfo> all = getInfos(eventChains);
        if (events.size() != all.size()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Got events --\n");
            for (SnippetEvent evt : events) {
                sb.append("  key: ").append(evt.snippet());
                sb.append(" before: ").append(evt.previousStatus());
                sb.append(" status: ").append(evt.status());
                sb.append(" isSignatureChange: ").append(evt.isSignatureChange());
                sb.append(" cause: ");
                if (evt.causeSnippet() == null) {
                    sb.append("direct");
                } else {
                    sb.append(evt.causeSnippet());
                }
                sb.append("\n");
            }
            sb.append("Expected ").append(all.size());
            sb.append(" events, got: ").append(events.size());
            fail(sb.toString());
        }

        int impactId = 0;
        Map<Snippet, List<SnippetEvent>> groupedEvents = groupByCauseSnippet(events);
        assertEquals(groupedEvents.size(), eventChains.length, "Number of main events");
        for (Map.Entry<Snippet, List<SnippetEvent>> entry : groupedEvents.entrySet()) {
            EventChain eventChain = eventChains[impactId++];
            SnippetEvent main = entry.getValue().get(0);
            Snippet mainKey = main.snippet();
            if (eventChain.mainInfo != null) {
                eventChain.mainInfo.assertMatch(entry.getValue().get(0), mainKey);
                if (eventChain.updates.length > 0) {
                    if (eventChain.updates.length == 1) {
                        eventChain.updates[0].assertMatch(entry.getValue().get(1), mainKey);
                    } else {
                        Arrays.sort(eventChain.updates, (a, b) -> ((a.snippet() == MAIN_SNIPPET)
                                ? mainKey
                                : a.snippet()).id().compareTo(b.snippet().id()));
                        List<SnippetEvent> updateEvents = new ArrayList<>(entry.getValue().subList(1, entry.getValue().size()));
                        int idx = 0;
                        for (SnippetEvent ste : updateEvents) {
                            eventChain.updates[idx++].assertMatch(ste, mainKey);
                        }
                    }
                }
            }
            if (((Object) eventChain.value) != IGNORE_VALUE) {
                assertEquals(main.value(), eventChain.value, "Expected execution value of: " + eventChain.value +
                        ", but got: " + main.value());
            }
            if (eventChain.exceptionClass != IGNORE_EXCEPTION) {
                if (main.exception() == null) {
                    assertEquals(eventChain.exceptionClass, null, "Expected an exception of class "
                            + eventChain.exceptionClass + " got no exception");
                } else if (eventChain.exceptionClass == null) {
                    fail("Expected no exception but got " + main.exception().toString());
                } else {
                    assertTrue(eventChain.exceptionClass.isInstance(main.exception()),
                            "Expected an exception of class " + eventChain.exceptionClass +
                                    " got: " + main.exception().toString());
                }
            }
            List<Diag> diagnostics = getState().diagnostics(mainKey).collect(toList());
            switch (diagMain) {
                case DIAG_OK:
                    assertEquals(diagnostics.size(), 0, "Expected no diagnostics, got: " + diagnosticsToString(diagnostics));
                    break;
                case DIAG_WARNING:
                    assertFalse(hasFatalError(diagnostics), "Expected no errors, got: " + diagnosticsToString(diagnostics));
                    break;
                case DIAG_ERROR:
                    assertTrue(hasFatalError(diagnostics), "Expected errors, got: " + diagnosticsToString(diagnostics));
                    break;
            }
            if (eventChain.mainInfo != null) {
                for (STEInfo ste : eventChain.updates) {
                    diagnostics = getState().diagnostics(ste.snippet()).collect(toList());
                    switch (diagUpdates) {
                        case DIAG_OK:
                            assertEquals(diagnostics.size(), 0, "Expected no diagnostics, got: " + diagnosticsToString(diagnostics));
                            break;
                        case DIAG_WARNING:
                            assertFalse(hasFatalError(diagnostics), "Expected no errors, got: " + diagnosticsToString(diagnostics));
                            break;
                    }
                }
            }
        }
        return events;
    }

    // Use this for all EMPTY calls to eval()
    public void assertEvalEmpty(String input) {
        List<SnippetEvent> events = getState().eval(input);
        assertEquals(events.size(), 0, "Expected no events, got: " + events.size());
    }

    public VarSnippet varKey(List<SnippetEvent> events) {
        Snippet key = key(events);
        assertTrue(key instanceof VarSnippet, "Expected a VariableKey, got: " + key);
        return (VarSnippet) key;
    }

    public MethodSnippet methodKey(List<SnippetEvent> events) {
        Snippet key = key(events);
        assertTrue(key instanceof MethodSnippet, "Expected a MethodKey, got: " + key);
        return (MethodSnippet) key;
    }

    public TypeDeclSnippet classKey(List<SnippetEvent> events) {
        Snippet key = key(events);
        assertTrue(key instanceof TypeDeclSnippet, "Expected a ClassKey, got: " + key);
        return (TypeDeclSnippet) key;
    }

    public ImportSnippet importKey(List<SnippetEvent> events) {
        Snippet key = key(events);
        assertTrue(key instanceof ImportSnippet, "Expected a ImportKey, got: " + key);
        return (ImportSnippet) key;
    }

    public Snippet key(List<SnippetEvent> events) {
        assertTrue(events.size() >= 1, "Expected at least one event, got none.");
        return events.get(0).snippet();
    }

    public void assertVarValue(Snippet key, String expected) {
        String value = state.varValue((VarSnippet) key);
        assertEquals(value, expected, "Expected var value of: " + expected + ", but got: " + value);
    }

    public Snippet assertDeclareFail(String input, String expectedErrorCode) {
        return assertDeclareFail(input, expectedErrorCode, added(REJECTED));
    }

    public Snippet assertDeclareFail(String input, String expectedErrorCode,
            STEInfo mainInfo, STEInfo... updates) {
        return assertDeclareFail(input,
                new ExpectedDiagnostic(expectedErrorCode, -1, -1, -1, -1, -1, Diagnostic.Kind.ERROR),
                mainInfo, updates);
    }

    public Snippet assertDeclareFail(String input, ExpectedDiagnostic expectedDiagnostic) {
        return assertDeclareFail(input, expectedDiagnostic, added(REJECTED));
    }

    public Snippet assertDeclareFail(String input, ExpectedDiagnostic expectedDiagnostic,
            STEInfo mainInfo, STEInfo... updates) {
        List<SnippetEvent> events = assertEval(input, null, null,
                DiagCheck.DIAG_ERROR, DiagCheck.DIAG_IGNORE, mainInfo, updates);
        SnippetEvent e = events.get(0);
        Snippet key = e.snippet();
        assertEquals(getState().status(key), REJECTED);
        List<Diag> diagnostics = getState().diagnostics(e.snippet()).collect(toList());
        assertTrue(diagnostics.size() > 0, "Expected diagnostics, got none");
        assertDiagnostic(input, diagnostics.get(0), expectedDiagnostic);
        assertTrue(key != null, "key must never be null, but it was for: " + input);
        return key;
    }

    public Snippet assertDeclareWarn1(String input, String expectedErrorCode) {
        return assertDeclareWarn1(input, new ExpectedDiagnostic(expectedErrorCode, -1, -1, -1, -1, -1, Diagnostic.Kind.WARNING));
    }

    public Snippet assertDeclareWarn1(String input, ExpectedDiagnostic expectedDiagnostic) {
        return assertDeclareWarn1(input, expectedDiagnostic, added(VALID));
    }

    public Snippet assertDeclareWarn1(String input, ExpectedDiagnostic expectedDiagnostic, STEInfo mainInfo, STEInfo... updates) {
        List<SnippetEvent> events = assertEval(input, IGNORE_VALUE, null,
                DiagCheck.DIAG_WARNING, DiagCheck.DIAG_IGNORE, mainInfo, updates);
        SnippetEvent e = events.get(0);
        List<Diag> diagnostics = getState().diagnostics(e.snippet()).collect(toList());
        if (expectedDiagnostic != null) assertDiagnostic(input, diagnostics.get(0), expectedDiagnostic);
        return e.snippet();
    }

    private void assertDiagnostic(String input, Diag diagnostic, ExpectedDiagnostic expectedDiagnostic) {
        if (expectedDiagnostic != null) expectedDiagnostic.assertDiagnostic(diagnostic);
        // assertEquals(diagnostic.getSource(), input, "Diagnostic source");
    }

    public void assertTypeDeclSnippet(TypeDeclSnippet type, String expectedName,
            Status expectedStatus, SubKind expectedSubKind,
            int unressz, int othersz) {
        assertDeclarationSnippet(type, expectedName, expectedStatus,
                expectedSubKind, unressz, othersz);
    }

    public void assertMethodDeclSnippet(MethodSnippet method,
            String expectedName, String expectedSignature,
            Status expectedStatus, int unressz, int othersz) {
        assertDeclarationSnippet(method, expectedName, expectedStatus,
                METHOD_SUBKIND, unressz, othersz);
        String signature = method.signature();
        assertEquals(signature, expectedSignature,
                "Expected " + method.source() + " to have the name: " +
                        expectedSignature + ", got: " + signature);
    }

    public void assertVariableDeclSnippet(VarSnippet var,
            String expectedName, String expectedTypeName,
            Status expectedStatus, SubKind expectedSubKind,
            int unressz, int othersz) {
        assertDeclarationSnippet(var, expectedName, expectedStatus,
                expectedSubKind, unressz, othersz);
        String signature = var.typeName();
        assertEquals(signature, expectedTypeName,
                "Expected " + var.source() + " to have the type name: " +
                        expectedTypeName + ", got: " + signature);
    }

    public void assertDeclarationSnippet(DeclarationSnippet declarationKey,
            String expectedName,
            Status expectedStatus, SubKind expectedSubKind,
            int unressz, int othersz) {
        assertKey(declarationKey, expectedStatus, expectedSubKind);
        String source = declarationKey.source();
        assertEquals(declarationKey.name(), expectedName,
                "Expected " + source + " to have the name: " + expectedName + ", got: " + declarationKey.name());
        long unresolved = getState().unresolvedDependencies(declarationKey).count();
        assertEquals(unresolved, unressz, "Expected " + source + " to have " + unressz
                + " unresolved symbols, got: " + unresolved);
        long otherCorralledErrorsCount = getState().diagnostics(declarationKey).count();
        assertEquals(otherCorralledErrorsCount, othersz, "Expected " + source + " to have " + othersz
                + " other errors, got: " + otherCorralledErrorsCount);
    }

    public void assertKey(Snippet key, Status expectedStatus, SubKind expectedSubKind) {
        String source = key.source();
        SubKind actualSubKind = key.subKind();
        assertEquals(actualSubKind, expectedSubKind,
                "Expected " + source + " to have the subkind: " + expectedSubKind + ", got: " + actualSubKind);
        Status status = getState().status(key);
        assertEquals(status, expectedStatus, "Expected " + source + " to be "
                + expectedStatus + ", but it is " + status);
        Snippet.Kind expectedKind = getKind(key);
        assertEquals(key.kind(), expectedKind, "Checking kind: ");
        assertEquals(expectedSubKind.kind(), expectedKind, "Checking kind: ");
    }

    public void assertDrop(Snippet key, STEInfo mainInfo, STEInfo... updates) {
        assertDrop(key, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, mainInfo, updates);
    }

    public void assertDrop(Snippet key, DiagCheck diagMain, DiagCheck diagUpdates, STEInfo mainInfo, STEInfo... updates) {
        assertDrop(key, diagMain, diagUpdates, new EventChain(mainInfo, null, null, updates));
    }

    public void assertDrop(Snippet key, DiagCheck diagMain, DiagCheck diagUpdates, EventChain... eventChains) {
        checkEvents(() -> getState().drop(key), "drop(" + key + ")", diagMain, diagUpdates, eventChains);
    }

    public void assertAnalyze(String input, String source, String remaining, boolean isComplete) {
        assertAnalyze(input, null, source, remaining, isComplete);
    }

     public void assertAnalyze(String input, Completeness status, String source) {
        assertAnalyze(input, status, source, null, null);
    }

    public void assertAnalyze(String input, Completeness status, String source, String remaining, Boolean isComplete) {
        CompletionInfo ci = getAnalysis().analyzeCompletion(input);
        if (status != null) assertEquals(ci.completeness(), status, "Input : " + input + ", status: ");
        assertEquals(ci.source(), source, "Input : " + input + ", source: ");
        if (remaining != null) assertEquals(ci.remaining(), remaining, "Input : " + input + ", remaining: ");
        if (isComplete != null) {
            boolean isExpectedComplete = isComplete;
            assertEquals(ci.completeness().isComplete(), isExpectedComplete, "Input : " + input + ", isComplete: ");
        }
    }

    public void assertNumberOfActiveVariables(int cnt) {
        assertEquals(getState().variables().count(), cnt, "Variables : " + getState().variables().collect(toList()));
    }

    public void assertNumberOfActiveMethods(int cnt) {
        assertEquals(getState().methods().count(), cnt, "Methods : " + getState().methods().collect(toList()));
    }

    public void assertNumberOfActiveClasses(int cnt) {
        assertEquals(getState().types().count(), cnt, "Types : " + getState().types().collect(toList()));
    }

    public void assertKeys(MemberInfo... expected) {
        int index = 0;
        List<Snippet> snippets = getState().snippets().collect(toList());
        assertEquals(allSnippets.size(), snippets.size());
        for (Snippet sn : snippets) {
            if (sn.kind().isPersistent() && getState().status(sn).isActive()) {
                MemberInfo actual = getMemberInfo(sn);
                MemberInfo exp = expected[index];
                assertEquals(actual, exp, String.format("Difference in #%d. Expected: %s, actual: %s",
                        index, exp, actual));
                ++index;
            }
        }
    }

    public void assertActiveKeys() {
        Collection<Snippet> expected = getActiveKeys();
        assertActiveKeys(expected.toArray(new Snippet[expected.size()]));
    }

    public void assertActiveKeys(Snippet... expected) {
        int index = 0;
        for (Snippet key : getState().snippets().collect(toList())) {
            if (state.status(key).isActive()) {
                assertEquals(expected[index], key, String.format("Difference in #%d. Expected: %s, actual: %s", index, key, expected[index]));
                ++index;
            }
        }
    }

    private void assertActiveSnippets(Stream<? extends Snippet> snippets, Predicate<Snippet> p, String label) {
        Set<Snippet> active = getActiveKeys().stream()
                .filter(p)
                .collect(Collectors.toSet());
        Set<Snippet> got = snippets
                .collect(Collectors.toSet());
        assertEquals(active, got, label);
    }

    public void assertVariables() {
        assertActiveSnippets(getState().variables(), (key) -> key instanceof VarSnippet, "Variables");
    }

    public void assertMethods() {
        assertActiveSnippets(getState().methods(), (key) -> key instanceof MethodSnippet, "Methods");
    }

    public void assertClasses() {
        assertActiveSnippets(getState().types(), (key) -> key instanceof TypeDeclSnippet, "Classes");
    }

    public void assertMembers(Stream<? extends Snippet> members, MemberInfo...expectedInfos) {
        Set<MemberInfo> expected = Stream.of(expectedInfos).collect(Collectors.toSet());
        Set<MemberInfo> got = members
                        .map(this::getMemberInfo)
                        .collect(Collectors.toSet());
        assertEquals(got.size(), expected.size(), "Expected : " + expected + ", actual : " + members);
        assertEquals(got, expected);
    }

    public void assertVariables(MemberInfo...expected) {
        assertMembers(getState().variables(), expected);
    }

    public void assertMethods(MemberInfo...expected) {
        assertMembers(getState().methods(), expected);
        getState().methods().forEach(methodKey -> {
            MemberInfo expectedInfo = null;
            for (MemberInfo info : expected) {
                if (info.name.equals(methodKey.name()) && info.type.equals(methodKey.signature())) {
                    expectedInfo = getMemberInfo(methodKey);
                }
            }
            assertNotNull(expectedInfo, "Not found method: " + methodKey.name());
            int lastIndexOf = expectedInfo.type.lastIndexOf(')');
            assertEquals(methodKey.parameterTypes(), expectedInfo.type.substring(1, lastIndexOf), "Parameter types");
        });
    }

    public void assertClasses(MemberInfo...expected) {
        assertMembers(getState().types(), expected);
    }

    public void assertCompletion(String code, String... expected) {
        assertCompletion(code, null, expected);
    }

    public void assertCompletion(String code, Boolean isSmart, String... expected) {
        List<String> completions = computeCompletions(code, isSmart);
        assertEquals(completions, Arrays.asList(expected), "Input: " + code + ", " + completions.toString());
    }

    public void assertCompletionIncludesExcludes(String code, Set<String> expected, Set<String> notExpected) {
        assertCompletionIncludesExcludes(code, null, expected, notExpected);
    }

    public void assertCompletionIncludesExcludes(String code, Boolean isSmart, Set<String> expected, Set<String> notExpected) {
        List<String> completions = computeCompletions(code, isSmart);
        assertTrue(completions.containsAll(expected), "Expected completions: "
                + String.valueOf(expected)
                + ", got: "
                + String.valueOf(completions));
        assertTrue(Collections.disjoint(completions, notExpected), String.valueOf(completions));
    }

    private List<String> computeCompletions(String code, Boolean isSmart) {
        waitIndexingFinished();

        int cursor =  code.indexOf('|');
        code = code.replace("|", "");
        assertTrue(cursor > -1, "'|' expected, but not found in: " + code);
        List<Suggestion> completions =
                getAnalysis().completionSuggestions(code, cursor, new int[1]); //XXX: ignoring anchor for now
        return completions.stream()
                          .filter(s -> isSmart == null || isSmart == s.matchesType())
                          .map(s -> s.continuation())
                          .distinct()
                          .collect(Collectors.toList());
    }

    public void assertInferredType(String code, String expectedType) {
        String inferredType = getAnalysis().analyzeType(code, code.length());

        assertEquals(inferredType, expectedType, "Input: " + code + ", " + inferredType);
    }

    public void assertInferredFQNs(String code, String... fqns) {
        assertInferredFQNs(code, code.length(), false, fqns);
    }

    public void assertInferredFQNs(String code, int simpleNameLen, boolean resolvable, String... fqns) {
        waitIndexingFinished();

        QualifiedNames candidates = getAnalysis().listQualifiedNames(code, code.length());

        assertEquals(candidates.getNames(), Arrays.asList(fqns), "Input: " + code + ", candidates=" + candidates.getNames());
        assertEquals(candidates.getSimpleNameLength(), simpleNameLen, "Input: " + code + ", simpleNameLen=" + candidates.getSimpleNameLength());
        assertEquals(candidates.isResolvable(), resolvable, "Input: " + code + ", resolvable=" + candidates.isResolvable());
    }

    protected void waitIndexingFinished() {
        try {
            Method waitBackgroundTaskFinished = getAnalysis().getClass().getDeclaredMethod("waitBackgroundTaskFinished");

            waitBackgroundTaskFinished.setAccessible(true);
            waitBackgroundTaskFinished.invoke(getAnalysis());
        } catch (Exception ex) {
            throw new AssertionError("Cannot wait for indexing end.", ex);
        }
    }

    public void assertSignature(String code, String... expected) {
        int cursor =  code.indexOf('|');
        code = code.replace("|", "");
        assertTrue(cursor > -1, "'|' expected, but not found in: " + code);
        List<Documentation> documentation = getAnalysis().documentation(code, cursor, false);
        Set<String> docSet = documentation.stream().map(doc -> doc.signature()).collect(Collectors.toSet());
        Set<String> expectedSet = Stream.of(expected).collect(Collectors.toSet());
        assertEquals(docSet, expectedSet, "Input: " + code);
    }

    public void assertJavadoc(String code, String... expected) {
        int cursor =  code.indexOf('|');
        code = code.replace("|", "");
        assertTrue(cursor > -1, "'|' expected, but not found in: " + code);
        List<Documentation> documentation = getAnalysis().documentation(code, cursor, true);
        Set<String> docSet = documentation.stream()
                                          .map(doc -> doc.signature() + "\n" + doc.javadoc())
                                          .collect(Collectors.toSet());
        Set<String> expectedSet = Stream.of(expected).collect(Collectors.toSet());
        assertEquals(docSet, expectedSet, "Input: " + code);
    }

    public enum ClassType {
        CLASS("CLASS_SUBKIND", "class", "class"),
        ENUM("ENUM_SUBKIND", "enum", "enum"),
        INTERFACE("INTERFACE_SUBKIND", "interface", "interface"),
        ANNOTATION("ANNOTATION_TYPE_SUBKIND", "@interface", "annotation interface");

        private final String classType;
        private final String name;
        private final String displayed;

        ClassType(String classType, String name, String displayed) {
            this.classType = classType;
            this.name = name;
            this.displayed = displayed;
        }

        public String getClassType() {
            return classType;
        }

        public String getDisplayed() {
            return displayed;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static MemberInfo variable(String type, String name) {
        return new MemberInfo(type, name);
    }

    public static MemberInfo method(String signature, String name) {
        return new MemberInfo(signature, name);
    }

    public static MemberInfo clazz(ClassType classType, String className) {
        return new MemberInfo(classType.getClassType(), className);
    }

    public static class MemberInfo {
        public final String type;
        public final String name;

        public MemberInfo(String type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return type.hashCode() + 3 * name.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MemberInfo) {
                MemberInfo other = (MemberInfo) o;
                return type.equals(other.type) && name.equals(other.name);
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("%s %s", type, name);
        }
    }

    public MemberInfo getMemberInfo(Snippet key) {
        SubKind subkind = key.subKind();
        switch (subkind) {
            case CLASS_SUBKIND:
            case INTERFACE_SUBKIND:
            case ENUM_SUBKIND:
            case ANNOTATION_TYPE_SUBKIND:
                return new MemberInfo(subkind.name(), ((DeclarationSnippet) key).name());
            case METHOD_SUBKIND:
                MethodSnippet method = (MethodSnippet) key;
                return new MemberInfo(method.signature(), method.name());
            case VAR_DECLARATION_SUBKIND:
            case VAR_DECLARATION_WITH_INITIALIZER_SUBKIND:
            case TEMP_VAR_EXPRESSION_SUBKIND:
                VarSnippet var = (VarSnippet) key;
                return new MemberInfo(var.typeName(), var.name());
            default:
                throw new AssertionError("Unknown snippet : " + key.kind() + " in expression " + key.toString());
        }
    }

    public String diagnosticsToString(List<Diag> diagnostics) {
        StringWriter writer = new StringWriter();
        for (Diag diag : diagnostics) {
            writer.write("Error --\n");
            for (String line : diag.getMessage(null).split("\\r?\\n")) {
                writer.write(String.format("%s\n", line));
            }
        }
        return writer.toString().replace("\n", System.lineSeparator());
    }

    public boolean hasFatalError(List<Diag> diagnostics) {
        for (Diag diag : diagnostics) {
            if (diag.isError()) {
                return true;
            }
        }
        return false;
    }

    public static EventChain chain(STEInfo mainInfo, STEInfo... updates) {
        return chain(mainInfo, IGNORE_VALUE, null, updates);
    }

    public static EventChain chain(STEInfo mainInfo, String value, Class<? extends Throwable> exceptionClass, STEInfo... updates) {
        return new EventChain(mainInfo, value, exceptionClass, updates);
    }

    public static STEInfo ste(Snippet key, Status previousStatus, Status status,
                Boolean isSignatureChange, Snippet causeKey) {
        return new STEInfo(key, previousStatus, status, isSignatureChange, causeKey);
    }

    public static STEInfo added(Status status) {
        return new STEInfo(MAIN_SNIPPET, NONEXISTENT, status, status.isDefined(), null);
    }

    public static class EventChain {
        public final STEInfo mainInfo;
        public final STEInfo[] updates;
        public final String value;
        public final Class<? extends Throwable> exceptionClass;

        public EventChain(STEInfo mainInfo, String value, Class<? extends Throwable> exceptionClass, STEInfo... updates) {
            this.mainInfo = mainInfo;
            this.updates = updates;
            this.value = value;
            this.exceptionClass = exceptionClass;
        }
    }

    public static class STEInfo {

        STEInfo(Snippet snippet, Status previousStatus, Status status,
                Boolean isSignatureChange, Snippet causeSnippet) {
            this.snippet = snippet;
            this.previousStatus = previousStatus;
            this.status = status;
            this.checkIsSignatureChange = isSignatureChange != null;
            this.isSignatureChange = checkIsSignatureChange ? isSignatureChange : false;
            this.causeSnippet = causeSnippet;
            assertTrue(snippet != null, "Bad test set-up. The match snippet must not be null");
        }

        final Snippet snippet;
        final Status previousStatus;
        final Status status;
        final boolean isSignatureChange;
        final Snippet causeSnippet;

         final boolean checkIsSignatureChange;
        public Snippet snippet() {
            return snippet;
        }
        public Status previousStatus() {
            return previousStatus;
        }
        public Status status() {
            return status;
        }
        public boolean isSignatureChange() {
            if (!checkIsSignatureChange) {
                throw new IllegalStateException("isSignatureChange value is undefined");
            }
            return isSignatureChange;
        }
        public Snippet causeSnippet() {
            return causeSnippet;
        }
        public String value() {
            return null;
        }
        public Exception exception() {
            return null;
        }

        public void assertMatch(SnippetEvent ste, Snippet mainSnippet) {
            assertKeyMatch(ste, ste.snippet(), snippet(), mainSnippet);
            assertStatusMatch(ste, ste.previousStatus(), previousStatus());
            assertStatusMatch(ste, ste.status(), status());
            if (checkIsSignatureChange) {
                assertEquals(ste.isSignatureChange(), isSignatureChange(),
                        "Expected " +
                                (isSignatureChange()? "" : "no ") +
                                "signature-change, got: " +
                                (ste.isSignatureChange()? "" : "no ") +
                                "signature-change" +
                        "\n   expected-event: " + this + "\n   got-event: " + toString(ste));
            }
            assertKeyMatch(ste, ste.causeSnippet(), causeSnippet(), mainSnippet);
        }

        private void assertKeyMatch(SnippetEvent ste, Snippet sn, Snippet expected, Snippet mainSnippet) {
            Snippet testKey = expected;
            if (testKey != null) {
                if (expected == MAIN_SNIPPET) {
                    assertNotNull(mainSnippet, "MAIN_SNIPPET used, test must pass value to assertMatch");
                    testKey = mainSnippet;
                }
                if (ste.causeSnippet() == null && ste.status() != DROPPED && expected != MAIN_SNIPPET) {
                    // Source change, always new snippet -- only match id()
                    assertTrue(sn != testKey,
                            "Main-event: Expected new snippet to be != : " + testKey
                            + "\n   got-event: " + toString(ste));
                    assertEquals(sn.id(), testKey.id(), "Expected IDs to match: " + testKey + ", got: " + sn
                            + "\n   expected-event: " + this + "\n   got-event: " + toString(ste));
                } else {
                    assertEquals(sn, testKey, "Expected key to be: " + testKey + ", got: " + sn
                            + "\n   expected-event: " + this + "\n   got-event: " + toString(ste));
                }
            }
        }

        private void assertStatusMatch(SnippetEvent ste, Status status, Status expected) {
            if (expected != null) {
                assertEquals(status, expected, "Expected status to be: " + expected + ", got: " + status +
                        "\n   expected-event: " + this + "\n   got-event: " + toString(ste));
            }
        }

        @Override
        public String toString() {
            return "STEInfo key: " +
                    (snippet()==MAIN_SNIPPET? "MAIN_SNIPPET" : (snippet()==null? "ignore" : snippet().id())) +
                    " before: " + previousStatus() +
                    " status: " + status() + " sig: " + isSignatureChange() +
                    " cause: " + (causeSnippet()==null? "null" : causeSnippet().id());
        }

        private String toString(SnippetEvent ste) {
            return "key: " + (ste.snippet()==MAIN_SNIPPET? "MAIN_SNIPPET" : ste.snippet().id()) + " before: " + ste.previousStatus()
                    + " status: " + ste.status() + " sig: " + ste.isSignatureChange()
                    + " cause: " + ste.causeSnippet();
        }
    }
}
