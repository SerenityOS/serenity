/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/**
 * A SAX based parser of LogCompilation output from HotSpot.  It takes a complete
 */

package com.sun.hotspot.tools.compiler;

import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX parser for HotSpot compilation logs. The bulk of the parsing and event
 * maintenance work is done in the {@link #startElement(String,String,String,Attributes)}
 * and {@link #endElement(String,String,String)} methods.
 */
public class LogParser extends DefaultHandler implements ErrorHandler {

    static final Pattern spacePattern = Pattern.compile(" ");

    /**
     * Map internal array type descriptors to Java names.
     */
    static final HashMap<String, String> type2printableMap;

    /**
     * Map Java primitive type names to internal type descriptors.
     */
    static final HashMap<String, String> type2vmtypeMap;

    static {
        type2printableMap = new HashMap<>();
        type2printableMap.put("[I", "int[]");
        type2printableMap.put("[C", "char[]");
        type2printableMap.put("[Z", "boolean[]");
        type2printableMap.put("[L", "Object[]");
        type2printableMap.put("[B", "byte[]");

        type2vmtypeMap = new HashMap<>();
        type2vmtypeMap.put("void", "V");
        type2vmtypeMap.put("boolean", "Z");
        type2vmtypeMap.put("byte", "B");
        type2vmtypeMap.put("char", "C");
        type2vmtypeMap.put("short", "S");
        type2vmtypeMap.put("int", "I");
        type2vmtypeMap.put("long", "J");
        type2vmtypeMap.put("float", "F");
        type2vmtypeMap.put("double", "D");
    }

    static String[] bytecodes = new String[] {
        "nop",
        "aconst_null",
        "iconst_m1",
        "iconst_0",
        "iconst_1",
        "iconst_2",
        "iconst_3",
        "iconst_4",
        "iconst_5",
        "lconst_0",
        "lconst_1",
        "fconst_0",
        "fconst_1",
        "fconst_2",
        "dconst_0",
        "dconst_1",
        "bipush",
        "sipush",
        "ldc",
        "ldc_w",
        "ldc2_w",
        "iload",
        "lload",
        "fload",
        "dload",
        "aload",
        "iload_0",
        "iload_1",
        "iload_2",
        "iload_3",
        "lload_0",
        "lload_1",
        "lload_2",
        "lload_3",
        "fload_0",
        "fload_1",
        "fload_2",
        "fload_3",
        "dload_0",
        "dload_1",
        "dload_2",
        "dload_3",
        "aload_0",
        "aload_1",
        "aload_2",
        "aload_3",
        "iaload",
        "laload",
        "faload",
        "daload",
        "aaload",
        "baload",
        "caload",
        "saload",
        "istore",
        "lstore",
        "fstore",
        "dstore",
        "astore",
        "istore_0",
        "istore_1",
        "istore_2",
        "istore_3",
        "lstore_0",
        "lstore_1",
        "lstore_2",
        "lstore_3",
        "fstore_0",
        "fstore_1",
        "fstore_2",
        "fstore_3",
        "dstore_0",
        "dstore_1",
        "dstore_2",
        "dstore_3",
        "astore_0",
        "astore_1",
        "astore_2",
        "astore_3",
        "iastore",
        "lastore",
        "fastore",
        "dastore",
        "aastore",
        "bastore",
        "castore",
        "sastore",
        "pop",
        "pop2",
        "dup",
        "dup_x1",
        "dup_x2",
        "dup2",
        "dup2_x1",
        "dup2_x2",
        "swap",
        "iadd",
        "ladd",
        "fadd",
        "dadd",
        "isub",
        "lsub",
        "fsub",
        "dsub",
        "imul",
        "lmul",
        "fmul",
        "dmul",
        "idiv",
        "ldiv",
        "fdiv",
        "ddiv",
        "irem",
        "lrem",
        "frem",
        "drem",
        "ineg",
        "lneg",
        "fneg",
        "dneg",
        "ishl",
        "lshl",
        "ishr",
        "lshr",
        "iushr",
        "lushr",
        "iand",
        "land",
        "ior",
        "lor",
        "ixor",
        "lxor",
        "iinc",
        "i2l",
        "i2f",
        "i2d",
        "l2i",
        "l2f",
        "l2d",
        "f2i",
        "f2l",
        "f2d",
        "d2i",
        "d2l",
        "d2f",
        "i2b",
        "i2c",
        "i2s",
        "lcmp",
        "fcmpl",
        "fcmpg",
        "dcmpl",
        "dcmpg",
        "ifeq",
        "ifne",
        "iflt",
        "ifge",
        "ifgt",
        "ifle",
        "if_icmpeq",
        "if_icmpne",
        "if_icmplt",
        "if_icmpge",
        "if_icmpgt",
        "if_icmple",
        "if_acmpeq",
        "if_acmpne",
        "goto",
        "jsr",
        "ret",
        "tableswitch",
        "lookupswitch",
        "ireturn",
        "lreturn",
        "freturn",
        "dreturn",
        "areturn",
        "return",
        "getstatic",
        "putstatic",
        "getfield",
        "putfield",
        "invokevirtual",
        "invokespecial",
        "invokestatic",
        "invokeinterface",
        "invokedynamic",
        "new",
        "newarray",
        "anewarray",
        "arraylength",
        "athrow",
        "checkcast",
        "instanceof",
        "monitorenter",
        "monitorexit",
        "wide",
        "multianewarray",
        "ifnull",
        "ifnonnull",
        "goto_w",
        "jsr_w",
        "breakpoint"
    };

    /**
     * Sort log events by start time.
     */
    static Comparator<LogEvent> sortByStart = new Comparator<LogEvent>() {

        public int compare(LogEvent a, LogEvent b) {
            double difference = (a.getStart() - b.getStart());
            if (difference < 0) {
                return -1;
            }
            if (difference > 0) {
                return 1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object other) {
            return false;
        }

        @Override
        public int hashCode() {
            return 7;
        }
    };

    /**
     * Sort log events first by the name of the compiled method, then by start
     * time. In case one of the events has no associated compilation (or the
     * associated compilation has no method name), the event with a compilation
     * and/or name is considered the larger one.
     */
    static Comparator<LogEvent> sortByNameAndStart = new Comparator<LogEvent>() {

        public int compare(LogEvent a, LogEvent b) {
            Compilation c1 = a.getCompilation();
            Compilation c2 = b.getCompilation();
            if (c1 != null && c1.getMethod() != null && c2 != null && c2.getMethod() != null) {
                int result = c1.getMethod().toString().compareTo(c2.getMethod().toString());
                if (result != 0) {
                    return result;
                }
            } else if ((c1 == null || c1.getMethod() == null) && c2 != null && c2.getMethod() != null) {
                return -1;
            } else if ((c2 == null || c2.getMethod() == null) && c1 != null && c1.getMethod() != null) {
                return 1;
            }
            return Double.compare(a.getStart(), b.getStart());
        }

        public boolean equals(Object other) {
            return false;
        }

        @Override
        public int hashCode() {
            return 7;
        }
    };

    /**
     * Sort log events by duration.
     */
    static Comparator<LogEvent> sortByElapsed = new Comparator<LogEvent>() {

        public int compare(LogEvent a, LogEvent b) {
            double difference = (a.getElapsedTime() - b.getElapsedTime());
            if (difference < 0) {
                return -1;
            }
            if (difference > 0) {
                return 1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object other) {
            return false;
        }

        @Override
        public int hashCode() {
            return 7;
        }
    };

    static Comparator<LogEvent> sortByNMethodSize = new Comparator<LogEvent>() {

        public int compare(LogEvent a, LogEvent b) {
            Compilation c1 = a.getCompilation();
            Compilation c2 = b.getCompilation();
            if ((c1 != null && c2 == null)) {
                return -1;
            } else if (c1 == null && c2 != null) {
                return 1;
            } else if (c1 == null && c2 == null) {
                return 0;
            }

            if (c1.getNMethod() != null && c2.getNMethod() == null) {
                return -1;
            } else if (c1.getNMethod() == null && c2.getNMethod() != null) {
                return 1;
            } else if (c1.getNMethod() == null && c2.getNMethod() == null) {
                return 0;
            }

            assert c1.getNMethod() != null && c2.getNMethod() != null : "Neither should be null here";

            long c1Size = c1.getNMethod().getInstSize();
            long c2Size = c2.getNMethod().getInstSize();

            if (c1Size == 0 && c2Size == 0) {
                return 0;
            }

            if (c1Size > c2Size) {
                return -1;
            } else if (c1Size < c2Size) {
                return 1;
            }

            return 0;
        }

        public boolean equals(Object other) {
            return false;
        }

        @Override
        public int hashCode() {
            return 7;
        }
  };

    /**
     * Shrink-wrapped representation of a JVMState (tailored to meet this
     * tool's needs). It only records a method and bytecode instruction index.
     */
    class Jvms {
        Jvms(Method method, int bci) {
            this.method = method;
            this.bci = bci;
        }
        final public Method method;
        final public int bci;
        final public String toString() {
            return "@" + bci + " " + method;
        }
    }

    /**
     * Representation of a lock elimination. Locks, corresponding to
     * synchronized blocks and method calls, may be eliminated if the object in
     * question is guaranteed to be used thread-locally.
     */
    class LockElimination extends BasicLogEvent {

        /**
         * Track all locations from which this lock was eliminated.
         */
        ArrayList<Jvms> jvms = new ArrayList<>(1);

        /**
         * The kind of lock (coarsened, nested, non-escaping, unknown).
         */
        final String kind;

        /**
         * The lock class (unlock, lock, unknown).
         */
        final String classId;

        /**
         * The precise type of lock.
         */
        final String tagName;

        LockElimination(String tagName, double start, String id, String kind, String classId) {
            super(start, id);
            this.kind = kind;
            this.classId = classId;
            this.tagName = tagName;
        }

        @Override
        public void print(PrintStream stream, boolean printID) {
            if (printID) {
                stream.printf("%s ", getId());
            }
            stream.printf("%s %s %s  %.3f ", tagName, kind, classId, getStart());
            stream.print(jvms.toString());
            stream.print("\n");
        }

        void addJVMS(Method method, int bci) {
            jvms.add(new Jvms(method, bci));
        }

    }

    /**
     * A list of log events. This is populated with the events found in the
     * compilation log file during parsing.
     */
    private ArrayList<LogEvent> events = new ArrayList<>();

    /**
     * Map compilation log IDs to type names.
     */
    private HashMap<String, String> types = new HashMap<>();

    /**
     * Map compilation log IDs to methods.
     */
    private HashMap<String, Method> methods = new HashMap<>();

    /**
     * Map compilation IDs ({@see #makeId()}) to newly created nmethods.
     */
    private LinkedHashMap<String, NMethod> nmethods = new LinkedHashMap<>();

    /**
     * Map compilation task IDs {@see #makeId()}) to {@link Compilation}
     * objects.
     */
    private HashMap<String, Compilation> compiles = new HashMap<>();

    /**
     * Track compilation failure reasons.
     */
    private String failureReason;

    /**
     * The current bytecode instruction index.
     */
    private int current_bci;

    /**
     * The current bytecode instruction.
     */
    private int current_bytecode;

    /**
     * A sequence of {@link CallSite}s representing a call stack. A scope
     * typically holds several {@link CallSite}s that represent calls
     * originating from that scope.
     *
     * New scopes are typically pushed when parse log events are encountered
     * ({@see #startElement()}) and popped when parsing of a given Java method
     * is done ({@see #endElement()}). Parsing events can be nested. Several
     * other events add information to scopes ({@see #startElement()}).
     */
    private Deque<CallSite> scopes = new ArrayDeque<>();

    /**
     * The current compilation.
     */
    private Compilation compile;

    /**
     * The {@linkplain CallSite compilation scope} currently in focus.
     */
    private CallSite site;

    /**
     * The {@linkplain CallSite method handle call site} currently under
     * observation.
     */
    private CallSite methodHandleSite;

    /**
     * Keep track of potentially nested compiler {@linkplain Phase phases}.
     */
    private Deque<Phase> phaseStack = new ArrayDeque<>();

    /**
     * The {@linkplain LockElimination lock elimination event} currently being
     * processed.
     */
    private LockElimination currentLockElimination;

    /**
     * The {@linkplain UncommonTrapEvent uncommon trap event} currently being
     * processed.
     */
    private UncommonTrapEvent currentTrap;

    /**
     * During the processing of a late inline event, this stack holds the
     * {@link CallSite}s that represent the inlining event's call stack.
     */
    private Deque<CallSite> lateInlineScope;

    /**
     * Denote whether a late inlining event is currently being processed.
     */
    private boolean lateInlining;

    /**
     * A document locator to provide better error messages: this allows the
     * tool to display in which line of the log file the problem occurred.
     */
    private Locator locator;

    /**
     * Record the location in a replace_string_concat.
     */
    private boolean expectStringConcatTrap = false;

    /**
     * Callback for the SAX framework to set the document locator.
     */
    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    /**
     * Report an internal error explicitly raised, i.e., not derived from an
     * exception.
     *
     * @param msg The error message to report.
     */
    private void reportInternalError(String msg) {
        reportInternalError(msg, null);
    }

    /**
     * Report an internal error derived from an exception.
     *
     * @param msg The beginning of the error message to report. The message
     * from the exception will be appended to this.
     * @param e The exception that led to the internal error.
     */
    private void reportInternalError(String msg, Exception e) {
        if (locator != null) {
            msg += " at " + locator.getLineNumber() + ":" + locator.getColumnNumber();
            if (e != null) {
                msg += " - " + e.getMessage();
            }
        }
        if (e != null) {
            throw new InternalError(msg, e);
        } else {
            throw new InternalError(msg);
        }
    }

    /**
     * Parse a long hexadecimal address into a {@code long} value. As Java only
     * supports positive {@code long} values, extra error handling and parsing
     * logic is provided.
     */
    long parseLong(String l) {
        try {
            return Long.decode(l).longValue();
        } catch (NumberFormatException nfe) {
            int split = l.length() - 8;
            String s1 = "0x" + l.substring(split);
            String s2 = l.substring(0, split);
            long v1 = Long.decode(s1).longValue() & 0xffffffffL;
            long v2 = (Long.decode(s2).longValue() & 0xffffffffL) << 32;
            if (!l.equals("0x" + Long.toHexString(v1 + v2))) {
                System.out.println(l);
                System.out.println(s1);
                System.out.println(s2);
                System.out.println(v1);
                System.out.println(v2);
                System.out.println(Long.toHexString(v1 + v2));
                reportInternalError("bad conversion");
            }
            return v1 + v2;
        }
    }

    /**
     * Entry point for log file parsing with a file name.
     *
     * @param file The name of the log file to parse.
     * @param cleanup Whether to perform bad XML cleanup during parsing (this
     * is relevant for some log files generated by the 1.5 JVM).
     * @return a list of {@link LogEvent} instances describing the events found
     * in the log file.
     */
    public static ArrayList<LogEvent> parse(String file, boolean cleanup) throws Exception {
        return parse(new FileReader(file), cleanup);
    }

    /**
     * Entry point for log file parsing with a file reader.
     * {@link #parse(String,boolean)}
     */
    public static ArrayList<LogEvent> parse(Reader reader, boolean cleanup) throws Exception {
        // Create the XML input factory
        SAXParserFactory factory = SAXParserFactory.newInstance();

        // Create the XML LogEvent reader
        SAXParser p = factory.newSAXParser();

        if (cleanup) {
            // some versions of the log have slightly malformed XML, so clean it
            // up before passing it to SAX
            reader = new LogCleanupReader(reader);
        }

        LogParser log = new LogParser();
        try {
            p.parse(new InputSource(reader), log);
        } catch (Throwable th) {
            th.printStackTrace();
            // Carry on with what we've got...
        }

        // Associate compilations with their NMethods and other kinds of events
        for (LogEvent e : log.events) {
            if (e instanceof BasicLogEvent) {
                BasicLogEvent ble = (BasicLogEvent) e;
                Compilation c = log.compiles.get(ble.getId());
                if (c == null) {
                    if (!(ble instanceof NMethod)) {
                        if (ble instanceof MakeNotEntrantEvent && ((MakeNotEntrantEvent) ble).getCompileKind().equals("c2n")) {
                            // this is ok for c2n
                            assert ((MakeNotEntrantEvent) ble).getLevel().equals("0") : "Should be level 0";
                        } else {
                            throw new InternalError("only nmethods should have a null compilation, here's a " + ble.getClass());
                        }
                    }
                    continue;
                }
                ble.setCompilation(c);
                if (ble instanceof NMethod) {
                    c.setNMethod((NMethod) ble);
                }
            }
        }

        return log.events;
    }

    /**
     * Retrieve a given attribute's value from a collection of XML tag
     * attributes. Report an error if the requested attribute is not found.
     *
     * @param attr A collection of XML tag attributes.
     * @param name The name of the attribute the value of which is to be found.
     * @return The value of the requested attribute, or {@code null} if it was
     * not found.
     */
    String search(Attributes attr, String name) {
        String result = attr.getValue(name);
        if (result != null) {
            return result;
        } else {
            reportInternalError("can't find " + name);
            return null;
        }
    }

    /**
     * Retrieve a given attribute's value from a collection of XML tag
     * attributes. Return a default value if the requested attribute is not
     * found.
     *
     * @param attr A collection of XML tag attributes.
     * @param name The name of the attribute the value of which is to be found.
     * @param defaultValue The default value to return if the attribute is not
     * found.
     * @return The value of the requested attribute, or the default value if it
     * was not found.
     */
    String search(Attributes attr, String name, String defaultValue) {
        String result = attr.getValue(name);
        if (result != null) {
            return result;
        }
        return defaultValue;
    }

    /**
     * Map a type ID from the compilation log to an actual type name. In case
     * the type represents an internal array type descriptor, return a
     * Java-level name. If the type ID cannot be mapped to a name, raise an
     * error.
     */
    String type(String id) {
        String result = types.get(id);
        String remapped = type2printableMap.get(result);
        if (remapped != null) {
            return remapped;
        }
        return result;
    }

    /**
     * Register a mapping from log file type ID to type name.
     */
    void type(String id, String name) {
        assert type(id) == null;
        types.put(id, name);
    }

    /**
     * Map a log file type ID to an internal type declarator.
     */
    String sigtype(String id) {
        String result = types.get(id);
        String remapped = type2vmtypeMap.get(result);
        if (remapped != null) {
            return remapped;
        }
        if (result == null) {
            reportInternalError(id);
        }
        if (result.charAt(0) == '[') {
            return result;
        }
        return "L" + result + ";";
    }

    /**
     * Retrieve a method based on the log file ID it was registered under.
     * Raise an error if the ID does not map to a method.
     */
    Method method(String id) {
        Method result = methods.get(id);
        if (result == null) {
            reportInternalError(id);
        }
        return result;
    }

    /**
     * From a compilation ID and kind, assemble a compilation ID for inclusion
     * in the output.
     *
     * @param atts A collection of XML attributes from which the required
     * attributes are retrieved.
     */
    public String makeId(Attributes atts) {
        String id = atts.getValue("compile_id");
        String kind = atts.getValue("kind");
        if (kind != null && kind.equals("osr")) {
            id += "%";
        }
        return id;
    }

    /**
     * Process the start of a compilation log XML element.<ul>
     * <li><b>phase:</b> record the beginning of a compilation phase, pushing
     * it on the {@linkplain #phaseStack phase stack} and collecting
     * information about the compiler graph.</li>
     * <li><b>phase_done:</b> record the end of a compilation phase, popping it
     * off the {@linkplain #phaseStack phase stack} and collecting information
     * about the compiler graph (number of nodes and live nodes).</li>
     * <li><b>task:</b> register the start of a new compilation.</li>
     * <li><b>type:</b> register a type.</li>
     * <li><b>bc:</b> note the current bytecode index and instruction name,
     * updating {@link #current_bci} and {@link #current_bytecode}.</li>
     * <li><b>klass:</b> register a type (class).</li>
     * <li><b>method:</b> register a Java method.</li>
     * <li><b>call:</b> process a call, populating {@link #site} with the
     * appropriate data.</li>
     * <li><b>regalloc:</b> record the register allocator's trip count in the
     * {@linkplain #compile current compilation}.</li>
     * <li><b>inline_fail:</b> record the reason for a failed inline
     * operation.</li>
     * <li><b>inline_success:</b> record a successful inlining operation,
     * noting the success reason in the {@linkplain #site call site}.</li>
     * <li><b>failure:</b> note a compilation failure, storing the reason
     * description in {@link #failureReason}.</li>
     * <li><b>task_done:</b> register the end of a compilation, recording time
     * stamp and success information.</li>
     * <li><b>make_not_entrant:</b> deal with making a native method
     * non-callable (e.g., during an OSR compilation, if there are still
     * activations) or a zombie (when the method can be deleted).</li>
     * <li><b>uncommon_trap:</b> process an uncommon trap, setting the
     * {@link #currentTrap} field.</li>
     * <li><b>eliminate_lock:</b> record the start of a lock elimination,
     * setting the {@link #currentLockElimination} event.</li>
     * <li><b>late_inline:</b> start processing a late inline decision:
     * initialize the {@linkplain #lateInlineScope inline scope stack}, create
     * an {@linkplain #site initial scope} with a bogus bytecode index and the
     * right inline ID, and push the scope with the inline ID attached. Note
     * that most of late inlining processing happens in
     * {@link #endElement(String,String,String)}.</li>
     * <li><b>jvms:</b> record a {@linkplain Jvms JVMState}. Depending on the
     * context in which this event is encountered, this can mean adding
     * information to the currently being processed trap, lock elimination, or
     * inlining operation.</li>
     * <li><b>inline_id:</b> set the inline ID in the
     * {@linkplain #site current call site}.</li>
     * <li><b>nmethod:</b> record the creation of a new {@link NMethod} and
     * store it in the {@link #nmethods} map.</li>
     * <li><b>parse:</b> begin parsing a Java method's bytecode and
     * transforming it into an initial compiler IR graph.</li>
     * <li><b>parse_done:</b> finish parsing a Java method's bytecode.</li>
     * </ul>
     */
    @Override
    public void startElement(String uri, String localName, String qname, Attributes atts) {
        if (qname.equals("phase")) {
            Phase p = new Phase(search(atts, "name"),
                    Double.parseDouble(search(atts, "stamp")),
                    Integer.parseInt(search(atts, "nodes", "0")),
                    Integer.parseInt(search(atts, "live", "0")));
            phaseStack.push(p);
        } else if (qname.equals("phase_done")) {
            Phase p = phaseStack.pop();
            String phaseName = search(atts, "name", null);
            if (phaseName != null && !p.getId().equals(phaseName)) {
                System.out.println("phase: " + p.getId());
                reportInternalError("phase name mismatch");
            }
            p.setEnd(Double.parseDouble(search(atts, "stamp")));
            p.setEndNodes(Integer.parseInt(search(atts, "nodes", "0")));
            p.setEndLiveNodes(Integer.parseInt(search(atts, "live", "0")));
            compile.getPhases().add(p);
        } else if (qname.equals("task")) {
            String id = makeId(atts);

            // Create the new Compilation instance and populate it with readily
            // available data.
            compile = new Compilation(Integer.parseInt(search(atts, "compile_id", "-1")));
            compile.setStart(Double.parseDouble(search(atts, "stamp")));
            compile.setICount(search(atts, "count", "0"));
            compile.setBCount(search(atts, "backedge_count", "0"));
            compile.setBCI(Integer.parseInt(search(atts, "osr_bci", "-1")));
            String compiler = atts.getValue("compiler");
            assert compiler == null : "Compiler is not specified in task";
            long level = parseLong(search(atts, "level", "0"));
            if (level != 0) {
                compile.setLevel(level);
            }
            // Extract the name of the compiled method.
            String[] parts = spacePattern.split(atts.getValue("method"));
            String methodName = parts[0] + "::" + parts[1];

            // Continue collecting compilation meta-data.
            String kind = atts.getValue("compile_kind");
            if (kind == null) {
                kind = "normal";
            }
            if (kind.equals("osr")) {
                compile.setOsr(true);
            } else if (kind.equals("c2i")) {
                compile.setSpecial("--- adapter " + methodName);
            } else {
                compile.setSpecial(compile.getId() + " " + methodName + " (0 bytes)");
            }

            // Build a dummy method to stuff in the Compilation at the
            // beginning.
            Method m = new Method();
            m.setHolder(parts[0]);
            m.setName(parts[1]);
            m.setSignature(parts[2]);
            m.setFlags("0");
            m.setBytes(search(atts, "bytes", "unknown"));
            m.setLevel(compile.getLevel());
            compile.setMethod(m);
            events.add(compile);
            compiles.put(id, compile);
            site = compile.getCall();
        } else if (qname.equals("type")) {
            type(search(atts, "id"), search(atts, "name"));
        } else if (qname.equals("bc")) {
            current_bci = Integer.parseInt(search(atts, "bci"));
            current_bytecode = Integer.parseInt(search(atts, "code"));
        } else if (qname.equals("klass")) {
            type(search(atts, "id"), search(atts, "name"));
        } else if (qname.equals("method")) {
            String id = search(atts, "id");
            Method m = new Method();
            m.setHolder(type(search(atts, "holder")));
            m.setName(search(atts, "name"));
            m.setReturnType(type(search(atts, "return")));
            String arguments = atts.getValue("arguments");;
            if (arguments == null) {
                m.setSignature("()" + sigtype(atts.getValue("return")));
            } else {
                String[] args = spacePattern.split(arguments);
                StringBuilder sb = new StringBuilder("(");
                for (int i = 0; i < args.length; i++) {
                    sb.append(sigtype(args[i]));
                }
                sb.append(")");
                sb.append(sigtype(atts.getValue("return")));
                m.setSignature(sb.toString());
            }

            if (search(atts, "unloaded", "0").equals("0")) {
               m.setBytes(search(atts, "bytes"));
               m.setIICount(search(atts, "iicount"));
               m.setFlags(search(atts, "flags"));
            }
            String compiler = search(atts, "compiler", "");
            m.setCompiler(compiler);
            long level = parseLong(search(atts, "level", "0"));
            if (level != 0) {
                m.setLevel(level);
            }
            methods.put(id, m);
        } else if (qname.equals("call")) {
            Phase p = phaseStack.peek();
            if (scopes.peek() == null && p != null) {
                // Do not process other phases when scopes is null
                if (p.getName().equals("parse")) {
                  reportInternalError( "should not be in parse here:" + p.getName());
                }
            } else {
              if (methodHandleSite != null) {
                  methodHandleSite = null;
              }
              Method m = method(search(atts, "method"));
              if (lateInlining && scopes.size() == 0) {
                  // re-attempting already seen call site (late inlining for MH invokes)
                  if (m != site.getMethod()) {
                      if (current_bci != site.getBci()) {
                          System.err.println(m + " bci: " + current_bci);
                          System.err.println(site.getMethod() +  " bci: " + site.getBci());
                          reportInternalError("bci mismatch after late inlining");
                      }
                      site.setMethod(m);
                  }
              } else {
                  // We're dealing with a new call site; the called method is
                  // likely to be parsed next.
                  site = new CallSite(current_bci, m);
              }
              site.setCount(Integer.parseInt(search(atts, "count", "0")));
              String receiver = atts.getValue("receiver");
              if (receiver != null) {
                  site.setReceiver(type(receiver));
                  site.setReceiver_count(Integer.parseInt(search(atts, "receiver_count")));
              }
              int methodHandle = Integer.parseInt(search(atts, "method_handle_intrinsic", "0"));
              if (lateInlining && scopes.size() == 0) {
                  // The call was already added before this round of late
                  // inlining. Ignore.
              } else if (methodHandle == 0) {
                  scopes.peek().add(site);
              } else {
                  // method handle call site can be followed by another
                  // call (in case it is inlined). If that happens we
                  // discard the method handle call site. So we keep
                  // track of it but don't add it to the list yet.
                  methodHandleSite = site;
              }
          }
        } else if (qname.equals("intrinsic")) {
            String id = atts.getValue("id");
            assert id != null : "intrinsic id is null";
            CallSite cs = (site != null) ? site : scopes.peek();
            assert cs != null : "no CallSite?";
            cs.setIntrinsicName(id);
        } else if (qname.equals("regalloc")) {
            compile.setAttempts(Integer.parseInt(search(atts, "attempts")));
        } else if (qname.equals("replace_string_concat")) {
            expectStringConcatTrap = true;
        } else if (qname.equals("inline_fail")) {
            Phase p = phaseStack.peek();
            if (scopes.peek() == null && p != null) {
                // Do not process other phases when scopes is null
                if (p.getName().equals("parse")) {
                  reportInternalError( "should not be in parse here:" + p.getName());
                }
            } else {
                if (methodHandleSite != null) {
                    scopes.peek().add(methodHandleSite);
                    methodHandleSite = null;
                }
                if (lateInlining && scopes.size() == 0) {
                    site.setReason("fail: " + search(atts, "reason"));
                    lateInlining = false;
                } else {
                    scopes.peek().last().setReason("fail: " + search(atts, "reason"));
                }
            }
        } else if (qname.equals("inline_success")) {
            if (methodHandleSite != null) {
                reportInternalError("method handle site should have been replaced");
            }
            site.setReason("succeed: " + search(atts, "reason"));
        } else if (qname.equals("failure")) {
            failureReason = search(atts, "reason");
        } else if (qname.equals("task_done")) {
            compile.setEnd(Double.parseDouble(search(atts, "stamp")));
            if (Integer.parseInt(search(atts, "success")) == 0) {
                compile.setFailureReason(failureReason);
                failureReason = null;
            }
        } else if (qname.equals("make_not_entrant")) {
            String id = makeId(atts);
            NMethod nm = nmethods.get(id);
            if (nm == null) reportInternalError("nm == null");
            MakeNotEntrantEvent e = new MakeNotEntrantEvent(Double.parseDouble(search(atts, "stamp")), id,
                                                 atts.getValue("zombie") != null, nm);
            String compileKind = atts.getValue("compile_kind");
            e.setCompileKind(compileKind);
            String level = atts.getValue("level");
            e.setLevel(level);
            events.add(e);
        } else if (qname.equals("uncommon_trap")) {
            String id = atts.getValue("compile_id");
            if (id != null) {
                id = makeId(atts);
                currentTrap = new UncommonTrapEvent(Double.parseDouble(search(atts, "stamp")),
                        id,
                        atts.getValue("reason"),
                        atts.getValue("action"),
                        Integer.parseInt(search(atts, "count", "0")));
                events.add(currentTrap);
            } else {
                if (atts.getValue("method") != null) {
                    // These are messages from ciTypeFlow that don't
                    // actually correspond to generated code.
                    return;
                }
                try {
                    String currBytecode = current_bytecode >= 0 ? bytecodes[current_bytecode] : "<unknown>";
                    UncommonTrap unc = new UncommonTrap(Integer.parseInt(search(atts, "bci")),
                            search(atts, "reason"),
                            search(atts, "action"),
                            currBytecode);
                    if (scopes.size() == 0) {
                        // There may be a dangling site not yet in scopes after a late_inline
                        if (site != null) {
                            site.add(unc);
                        } else {
                            reportInternalError("scope underflow");
                        }
                    } else {
                        scopes.peek().add(unc);
                    }
                } catch (Error e) {
                    e.printStackTrace();
                }
            }
        } else if (qname.startsWith("eliminate_lock")) {
            String id = atts.getValue("compile_id");
            if (id != null) {
                id = makeId(atts);
                String kind = atts.getValue("kind");
                String classId = atts.getValue("class_id");
                currentLockElimination = new LockElimination(qname, Double.parseDouble(search(atts, "stamp")), id, kind, classId);
                events.add(currentLockElimination);
            }
        } else if (qname.equals("late_inline")) {
            long inlineId = 0;
            try {
                inlineId = Long.parseLong(search(atts, "inline_id"));
            } catch (InternalError ex) {
                // Log files from older hotspots may lack inline_id,
                // and zero is an acceptable substitute that allows processing to continue.
            }
            lateInlineScope = new ArrayDeque<>();
            Method m = method(search(atts, "method"));
            site = new CallSite(-999, m);
            site.setInlineId(inlineId);
            lateInlineScope.push(site);
        } else if (qname.equals("jvms")) {
            // <jvms bci='4' method='java/io/DataInputStream readChar ()C' bytes='40' count='5815' iicount='20815'/>
            if (currentTrap != null) {
                String[] parts = spacePattern.split(atts.getValue("method"));
                currentTrap.addMethodAndBCI(parts[0].replace('/', '.') + '.' + parts[1] + parts[2], Integer.parseInt(atts.getValue("bci")));
            } else if (currentLockElimination != null) {
                  currentLockElimination.addJVMS(method(atts.getValue("method")), Integer.parseInt(atts.getValue("bci")));
            } else if (lateInlineScope != null) {
                current_bci = Integer.parseInt(search(atts, "bci"));
                Method m = method(search(atts, "method"));
                site = new CallSite(current_bci, m);
                lateInlineScope.push(site);
            } else if (expectStringConcatTrap == true) {
                // Record the location of the replace_string_concat for the
                // uncommon_trap 'intrinsic_or_type_checked_inlining' that should follow it
                current_bci = Integer.parseInt(search(atts, "bci"));
                Method m = method(search(atts, "method"));
                site = new CallSite(current_bci, m);
            } else {
                // Ignore <eliminate_allocation type='667'>,
            }
        } else if (qname.equals("inline_id")) {
            if (methodHandleSite != null) {
                reportInternalError("method handle site should have been replaced");
            }
            long id = Long.parseLong(search(atts, "id"));
            site.setInlineId(id);
        } else if (qname.equals("nmethod")) {
            String id = makeId(atts);
            NMethod nm = new NMethod(Double.parseDouble(search(atts, "stamp")),
                    id,
                    parseLong(atts.getValue("address")),
                    parseLong(atts.getValue("size")));
            String level = atts.getValue("level");
            if (level != null) {
                nm.setLevel(parseLong(level));
            }
            String iOffset = atts.getValue("insts_offset");
            String sOffset = atts.getValue("stub_offset");
            if (iOffset != null && sOffset != null) {
                long insts_offset = parseLong(iOffset);
                long stub_offset = parseLong(sOffset);
                nm.setInstSize(stub_offset - insts_offset);
            }
            String compiler = search(atts, "compiler", "");
            nm.setCompiler(compiler);
            nmethods.put(id, nm);
            events.add(nm);
        } else if (qname.equals("parse")) {
            if (failureReason != null && scopes.size() == 0 && !lateInlining) {
                // A compilation just failed, and we're back at a top
                // compilation scope.
                failureReason = null;
                compile.reset();
                site = compile.getCall();
            }

            // Error checking.
            if (methodHandleSite != null) {
                reportInternalError("method handle site should have been replaced");
            }
            Method m = method(search(atts, "method")); // this is the method being parsed
            if (lateInlining && scopes.size() == 0) {
                if (site.getMethod() != m) {
                    reportInternalError("Unexpected method mismatch during late inlining (method at call site: " +
                        site.getMethod() + ", method being parsed: " + m + ")");
                }
            }

            if (scopes.size() == 0 && !lateInlining) {
                // The method being parsed is actually the method being
                // compiled; i.e., we're dealing with a compilation top scope,
                // which we must consequently push to the scopes stack.
                compile.setMethod(m);
                scopes.push(site);
            } else {
                // The method being parsed is *not* the current compilation's
                // top scope; i.e., we're dealing with an actual call site
                // in the top scope or somewhere further down a call stack.
                if (site != null && site.getMethod() == m) {
                    // We're dealing with monomorphic inlining that didn't have
                    // to be narrowed down, because the receiver was known
                    // beforehand.
                    scopes.push(site);
                } else if (scopes.peek().getCalls().size() > 2 && m == scopes.peek().lastButOne().getMethod()) {
                    // We're dealing with an at least bimorphic call site, and
                    // the compiler has now decided to parse the last-but-one
                    // method. The last one may already have been parsed for
                    // inlining.
                    scopes.push(scopes.peek().lastButOne());
                } else {
                    // The method has been narrowed down to the one we're now
                    // going to parse, which is inlined here. It's monomorphic
                    // inlining, but was not immediately clear as such.
                    //
                    // C1 prints multiple method tags during inlining when it
                    // narrows the method being inlined. Example:
                    //   ...
                    //   <method id="813" holder="694" name="toString" return="695" flags="1" bytes="36" iicount="1"/>
                    //   <call method="813" instr="invokevirtual"/>
                    //   <inline_success reason="receiver is statically known"/>
                    //   <method id="814" holder="792" name="toString" return="695" flags="1" bytes="5" iicount="3"/>
                    //   <parse method="814">
                    //   ...
                    site.setMethod(m);
                    scopes.push(site);
                }
            }
        } else if (qname.equals("parse_done")) {
            // Attach collected information about IR nodes to the current
            // parsing scope before it's popped off the stack in endElement()
            // (see where the parse tag is handled).
            CallSite call = scopes.peek();
            call.setEndNodes(Integer.parseInt(search(atts, "nodes", "0")));
            call.setEndLiveNodes(Integer.parseInt(search(atts, "live", "0")));
            call.setTimeStamp(Double.parseDouble(search(atts, "stamp")));
        }
    }

    /**
     * Process the end of a compilation log XML element.<ul>
     * <li><b>parse:</b> finish transforming a Java method's bytecode
     * instructions to an initial compiler IR graph.</li>
     * <li><b>uncommon_trap:</b> record the end of processing an uncommon trap,
     * resetting {@link #currentTrap}.</li>
     * <li><b>eliminate_lock:</b> record the end of a lock elimination,
     * resetting {@link #currentLockElimination}.</li>
     * <li><b>late_inline:</b> the closing tag for late_inline does not denote
     * the end of a late inlining operation, but the end of the descriptive log
     * data given at its beginning. That is, we're now in the position to
     * assemble details about the inlining chain (bytecode instruction index in
     * caller, called method). The {@link #lateInlining} flag is set to
     * {@code true} here. (It will be reset when parsing the inlined methods is
     * done; this happens for the successful case in this method as well, when
     * {@code parse} elements are processed; and for inlining failures, in
     * {@link #startElement(String,String,String,Attributes)}, when {@code inline_fail} elements are
     * processed.)</li>
     * <li><b>task:</b> perform cleanup at the end of a compilation. Note that
     * the explicit {@code task_done} event is handled in
     * {@link #startElement(String,String,String,Attributes)}.</li>
     * </ul>
     */
    @Override
    public void endElement(String uri, String localName, String qname) {
        try {
            if (qname.equals("parse")) {
                // Finish dealing with the current call scope. If no more are
                // left, no late inlining can be going on.
                scopes.pop();
                if (scopes.size() == 0) {
                    lateInlining = false;
                }
                // Clear the bytecode and site from the last parse
                site = null;
                current_bytecode = -1;
            } else if (qname.equals("uncommon_trap")) {
                currentTrap = null;
            } else if (qname.startsWith("eliminate_lock")) {
                currentLockElimination = null;
            } else if (qname.equals("late_inline")) {
                // Populate late inlining info.
                if (scopes.size() != 0) {
                    reportInternalError("scopes should be empty for late inline");
                }
                // late inline scopes are specified in reverse order:
                // compiled method should be on top of stack.
                CallSite caller = lateInlineScope.pop();
                Method m = compile.getMethod();
                if (!m.equals(caller.getMethod())) {
                    reportInternalError(String.format("call site and late_inline info don't match:\n  method %s\n  caller method %s, bci %d", m, caller.getMethod(), current_bci));
                }

                // Walk down the inlining chain and assemble bci+callee info.
                // This needs to be converted from caller+bci info contained in
                // the late_inline data.
                CallSite lateInlineSite = compile.getLateInlineCall();
                ArrayDeque<CallSite> thisCallScopes = new ArrayDeque<>();
                do {
                    current_bci = caller.getBci();
                    // Next inlined call.
                    caller = lateInlineScope.pop();
                    CallSite callee = new CallSite(current_bci, caller.getMethod());
                    callee.setInlineId(caller.getInlineId());
                    thisCallScopes.addLast(callee);
                    lateInlineSite.add(callee);
                    lateInlineSite = callee;
                } while (!lateInlineScope.isEmpty());

                site = compile.getCall().findCallSite(thisCallScopes);
                if (site == null) {
                    // Call site could not be found - report the problem in detail.
                    System.err.println("call scopes:");
                    for (CallSite c : thisCallScopes) {
                        System.err.println(c.getMethod() + " " + c.getBci() + " " + c.getInlineId());
                    }
                    CallSite c = thisCallScopes.getLast();
                    if (c.getInlineId() != 0) {
                        System.err.println("Looking for call site in entire tree:");
                        ArrayDeque<CallSite> stack = compile.getCall().findCallSite2(c);
                        for (CallSite c2 : stack) {
                            System.err.println(c2.getMethod() + " " + c2.getBci() + " " + c2.getInlineId());
                        }
                    }
                    System.err.println(caller.getMethod() + " bci: " + current_bci);
                    reportInternalError("couldn't find call site");
                }
                lateInlining = true;

                if (caller.getBci() != -999) {
                    System.out.println(caller.getMethod());
                    reportInternalError("broken late_inline info");
                }
                if (site.getMethod() != caller.getMethod()) {
                    if (site.getInlineId() == caller.getInlineId()) {
                        site.setMethod(caller.getMethod());
                    } else {
                        System.out.println(site.getMethod());
                        System.out.println(caller.getMethod());
                        reportInternalError("call site and late_inline info don't match");
                    }
                }
                // late_inline is followed by parse with scopes.size() == 0,
                // 'site' will be pushed to scopes.
                lateInlineScope = null;
            } else if (qname.equals("task")) {
                types.clear();
                methods.clear();
                site = null;
                lateInlining = false;
            } else if (qname.equals("replace_string_concat")) {
                expectStringConcatTrap = false;
            }
        } catch (Exception e) {
            reportInternalError("exception while processing end element", e);
        }
    }

    //
    // Handlers for problems that occur in XML parsing itself.
    //

    @Override
    public void warning(org.xml.sax.SAXParseException e) {
        System.err.println(e.getMessage() + " at line " + e.getLineNumber() + ", column " + e.getColumnNumber());
        e.printStackTrace();
    }

    @Override
    public void error(org.xml.sax.SAXParseException e) {
        System.err.println(e.getMessage() + " at line " + e.getLineNumber() + ", column " + e.getColumnNumber());
        e.printStackTrace();
    }

    @Override
    public void fatalError(org.xml.sax.SAXParseException e) {
        System.err.println(e.getMessage() + " at line " + e.getLineNumber() + ", column " + e.getColumnNumber());
        e.printStackTrace();
    }
}
