/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.jvm.hotspot.ci.ciEnv;
import sun.jvm.hotspot.code.CodeBlob;
import sun.jvm.hotspot.code.CodeCacheVisitor;
import sun.jvm.hotspot.code.NMethod;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.debugger.OopHandle;
import sun.jvm.hotspot.classfile.ClassLoaderDataGraph;
import sun.jvm.hotspot.memory.FileMapInfo;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.memory.Universe;
import sun.jvm.hotspot.gc.shared.CollectedHeap;
import sun.jvm.hotspot.gc.g1.G1CollectedHeap;
import sun.jvm.hotspot.oops.DefaultHeapVisitor;
import sun.jvm.hotspot.oops.HeapVisitor;
import sun.jvm.hotspot.oops.InstanceKlass;
import sun.jvm.hotspot.oops.Klass;
import sun.jvm.hotspot.oops.Metadata;
import sun.jvm.hotspot.oops.Method;
import sun.jvm.hotspot.oops.MethodData;
import sun.jvm.hotspot.oops.Oop;
import sun.jvm.hotspot.oops.RawHeapVisitor;
import sun.jvm.hotspot.oops.Symbol;
import sun.jvm.hotspot.oops.UnknownOopException;
import sun.jvm.hotspot.opto.Compile;
import sun.jvm.hotspot.opto.InlineTree;
import sun.jvm.hotspot.runtime.CompiledVFrame;
import sun.jvm.hotspot.runtime.CompilerThread;
import sun.jvm.hotspot.runtime.JavaThread;
import sun.jvm.hotspot.runtime.JavaVFrame;
import sun.jvm.hotspot.runtime.Threads;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.tools.ObjectHistogram;
import sun.jvm.hotspot.tools.JMap;
import sun.jvm.hotspot.tools.PMap;
import sun.jvm.hotspot.tools.PStack;
import sun.jvm.hotspot.tools.StackTrace;
import sun.jvm.hotspot.tools.SysPropsDumper;
import sun.jvm.hotspot.tools.jcore.ClassDump;
import sun.jvm.hotspot.tools.jcore.ClassFilter;
import sun.jvm.hotspot.tools.jcore.ClassWriter;
import sun.jvm.hotspot.types.CIntegerType;
import sun.jvm.hotspot.types.Field;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.basic.BasicType;
import sun.jvm.hotspot.ui.classbrowser.HTMLGenerator;
import sun.jvm.hotspot.ui.tree.CTypeTreeNodeAdapter;
import sun.jvm.hotspot.ui.tree.OopTreeNodeAdapter;
import sun.jvm.hotspot.ui.tree.SimpleTreeNode;
import sun.jvm.hotspot.utilities.AddressOps;
import sun.jvm.hotspot.utilities.Assert;
import sun.jvm.hotspot.utilities.CompactHashTable;
import sun.jvm.hotspot.utilities.HeapProgressThunk;
import sun.jvm.hotspot.utilities.LivenessPathElement;
import sun.jvm.hotspot.utilities.MethodArray;
import sun.jvm.hotspot.utilities.ObjectReader;
import sun.jvm.hotspot.utilities.PointerFinder;
import sun.jvm.hotspot.utilities.PointerLocation;
import sun.jvm.hotspot.utilities.ReversePtrs;
import sun.jvm.hotspot.utilities.ReversePtrsAnalysis;
import sun.jvm.hotspot.utilities.RobustOopDeterminator;
import sun.jvm.hotspot.utilities.SystemDictionaryHelper;

public class CommandProcessor {

    volatile boolean quit;

    public abstract static class DebuggerInterface {
        public abstract HotSpotAgent getAgent();
        public abstract boolean isAttached();
        public abstract void attach(int pid);
        public abstract void attach(String java, String core);
        public abstract void attach(String debugServerName);
        public abstract void detach();
        public abstract void reattach();
    }

    public static class BootFilter implements ClassFilter {
        public boolean canInclude(InstanceKlass kls) {
            return kls.getClassLoader() == null;
        }
    }

    public static class NonBootFilter implements ClassFilter {
        private HashMap<Symbol, InstanceKlass> emitted = new HashMap<>();
        public boolean canInclude(InstanceKlass kls) {
            if (kls.getClassLoader() == null) return false;
            if (emitted.get(kls.getName()) != null) {
                // Since multiple class loaders are being shoved
                // together duplicate classes are a possibilty.  For
                // now just ignore them.
                return false;
            }
            emitted.put(kls.getName(), kls);
            return true;
        }
    }

    static class Tokens {
        final String input;
        int i;
        String[] tokens;
        int length;

        String[] splitWhitespace(String cmd) {
            String[] t = cmd.split("\\s");
            if (t.length == 1 && t[0].length() == 0) {
                return new String[0];
            }
            return t;
        }

        void add(String s, ArrayList<String> t) {
            if (s.length() > 0) {
                t.add(s);
            }
        }

        Tokens(String cmd) {
            input = cmd;
            // check for quoting
            int quote = cmd.indexOf('"');
            ArrayList<String> t = new ArrayList<>();
            if (quote != -1) {
                while (cmd.length() > 0) {
                    if (quote != -1) {
                        int endquote = cmd.indexOf('"', quote + 1);
                        if (endquote == -1) {
                            throw new RuntimeException("mismatched quotes: " + input);
                        }

                        String before = cmd.substring(0, quote).trim();
                        String quoted = cmd.substring(quote + 1, endquote);
                        cmd = cmd.substring(endquote + 1).trim();
                        if (before.length() > 0) {
                            String[] w = splitWhitespace(before);
                            for (int i = 0; i < w.length; i++) {
                                add(w[i], t);
                            }
                        }
                        add(quoted, t);
                        quote = cmd.indexOf('"');
                    } else {
                        String[] w = splitWhitespace(cmd);
                        for (int i = 0; i < w.length; i++) {
                            add(w[i], t);
                        }
                        cmd = "";

                    }
                }
            } else {
                String[] w = splitWhitespace(cmd);
                for (int i = 0; i < w.length; i++) {
                    add(w[i], t);
                }
            }
            tokens = (String[])t.toArray(new String[0]);
            i = 0;
            length = tokens.length;

            //for (int i = 0; i < tokens.length; i++) {
            //    System.out.println("\"" + tokens[i] + "\"");
            //}
        }

        String nextToken() {
            return tokens[i++];
        }
        boolean hasMoreTokens() {
            return i < length;
        }
        int countTokens() {
            return length - i;
        }
        void trim(int n) {
            if (length >= n) {
                length -= n;
            } else {
                throw new IndexOutOfBoundsException(String.valueOf(n));
            }
        }
        String join(String sep) {
            StringBuilder result = new StringBuilder();
            for (int w = i; w < length; w++) {
                result.append(tokens[w]);
                if (w + 1 < length) {
                    result.append(sep);
                }
            }
            return result.toString();
        }

        String at(int i) {
            Objects.checkIndex(i, length);
            return tokens[i];
        }
    }


    abstract class Command {
        Command(String n, String u, boolean ok) {
            name = n;
            usage = u;
            okIfDisconnected = ok;
        }

        Command(String n, boolean ok) {
            name = n;
            usage = n;
            okIfDisconnected = ok;
        }

        final String name;
        final String usage;
        final boolean okIfDisconnected;
        abstract void doit(Tokens t);
        void usage() {
            out.println("Usage: " + usage);
        }

        void printNode(SimpleTreeNode node) {
            int count = node.getChildCount();
            for (int i = 0; i < count; i++) {
                try {
                    SimpleTreeNode field = node.getChild(i);
                    out.println(field);
                } catch (Exception e) {
                    out.println();
                    out.println("Error: " + e);
                    if (verboseExceptions) {
                        e.printStackTrace(out);
                    }
                }
            }
        }
    }

    void quote(String s) {
        if (s.indexOf(" ") == -1) {
            out.print(s);
        } else {
            out.print("\"");
            out.print(s);
            out.print("\"");
        }
    }

    void dumpType(Type type) {
        out.print("type ");
        quote(type.getName());
        out.print(" ");
        if (type.getSuperclass() != null) {
            quote(type.getSuperclass().getName());
            out.print(" ");
        } else {
            out.print("null ");
        }
        out.print(type.isOopType());
        out.print(" ");
        if (type.isCIntegerType()) {
            out.print("true ");
            out.print(((CIntegerType)type).isUnsigned());
            out.print(" ");
        } else {
            out.print("false false ");
        }
        out.print(type.getSize());
        out.println();
    }

    void dumpFields(Type type) {
        dumpFields(type, true);
    }

    void dumpFields(Type type, boolean allowStatic) {
        Iterator i = type.getFields();
        while (i.hasNext()) {
            Field f = (Field) i.next();
            if (!allowStatic && f.isStatic()) continue;
            out.print("field ");
            quote(type.getName());
            out.print(" ");
            out.print(f.getName());
            out.print(" ");
            quote(f.getType().getName());
            out.print(" ");
            out.print(f.isStatic());
            out.print(" ");
            if (f.isStatic()) {
                out.print("0 ");
                out.print(f.getStaticFieldAddress());
            } else {
                out.print(f.getOffset());
                out.print(" 0x0");
            }
            out.println();
        }
    }


    Address lookup(String symbol) {
        if (symbol.indexOf("::") != -1) {
            String[] parts = symbol.split("::");
            StringBuilder mangled = new StringBuilder("__1c");
            for (int i = 0; i < parts.length; i++) {
                int len = parts[i].length();
                if (len >= 26) {
                    mangled.append((char)('a' + (len / 26)));
                    len = len % 26;
                }
                mangled.append((char)('A' + len));
                mangled.append(parts[i]);
            }
            mangled.append("_");
            symbol = mangled.toString();
        }
        return VM.getVM().getDebugger().lookup(null, symbol);
    }

    Address parseAddress(String addr) {
        return VM.getVM().getDebugger().parseAddress(addr);
    }

    private final Command[] commandList = {
        new Command("reattach", true) {
            public void doit(Tokens t) {
                int tokens = t.countTokens();
                if (tokens != 0) {
                    usage();
                    return;
                }
                preAttach();
                debugger.reattach();
                postAttach();
            }
        },
        new Command("attach", "attach pid | exec core | remote_server", true) {
            public void doit(Tokens t) {
                int tokens = t.countTokens();
                if (tokens == 1) {
                    preAttach();
                    String arg = t.nextToken();
                    try {
                        // Attempt to attach as a PID
                        debugger.attach(Integer.parseInt(arg));
                    } catch (NumberFormatException e) {
                        // Attempt to connect to remote debug server
                        debugger.attach(arg);
                    }
                    postAttach();
                } else if (tokens == 2) {
                    preAttach();
                    debugger.attach(t.nextToken(), t.nextToken());
                    postAttach();
                } else {
                    usage();
                }
            }
        },
        new Command("detach", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                } else {
                    debugger.detach();
                }
            }
        },
        new Command("examine", "examine [ address/count ] | [ address,address]", false) {
            Pattern args1 = Pattern.compile("^(0x[0-9a-f]+)(/([0-9]*)([a-z]*))?$");
            Pattern args2 = Pattern.compile("^(0x[0-9a-f]+),(0x[0-9a-f]+)(/[a-z]*)?$");

            String fill(Address a, int width) {
                String s = "0x0";
                if (a != null) {
                    s = a.toString();
                }
                if (s.length() != width) {
                    return s.substring(0, 2) + "000000000000000000000".substring(0, width - s.length()) + s.substring(2);
                }
                return s;
            }

            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    String arg = t.nextToken();
                    Matcher m1 = args1.matcher(arg);
                    Matcher m2 = args2.matcher(arg);
                    Address start = null;
                    Address end   = null;
                    String format = "";
                    int formatSize = (int)VM.getVM().getAddressSize();

                    if (m1.matches()) {
                        start = VM.getVM().getDebugger().parseAddress(m1.group(1));
                        int count = 1;
                        if (m1.group(2) != null) {
                            count = Integer.parseInt(m1.group(3));
                        }
                        end = start.addOffsetTo(count * formatSize);
                    } else if (m2.matches()) {
                        start = VM.getVM().getDebugger().parseAddress(m2.group(1));
                        end   = VM.getVM().getDebugger().parseAddress(m2.group(2));
                    } else {
                        usage();
                        return;
                    }
                    int line = 80;
                    int formatWidth = formatSize * 8 / 4 + 2;

                    out.print(fill(start, formatWidth));
                    out.print(": ");
                    int width = line - formatWidth - 2;

                    boolean needsPrintln = true;
                    while (start != null && start.lessThan(end)) {
                        Address val = start.getAddressAt(0);
                        out.print(fill(val, formatWidth));
                        needsPrintln = true;
                        width -= formatWidth;
                        start = start.addOffsetTo(formatSize);
                        if (width <= formatWidth) {
                            out.println();
                            needsPrintln = false;
                            if (start.lessThan(end)) {
                                out.print(fill(start, formatWidth));
                                out.print(": ");
                                width = line - formatWidth - 2;
                            }
                        } else {
                            out.print(" ");
                            width -= 1;
                        }
                    }
                    if (needsPrintln) {
                        out.println();
                    }
                }
            }
        },
        new Command("dumpreplaydata", "dumpreplaydata { <address > | -a | <thread_id> }", false) {
            // This is used to dump replay data from ciInstanceKlass, ciMethodData etc
            // default file name is replay.txt, also if java crashes in compiler
            // thread, this file will be dumped in error processing.
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                    return;
                }
                String name = t.nextToken();
                Address a = null;
                try {
                    a = VM.getVM().getDebugger().parseAddress(name);
                } catch (NumberFormatException e) { }
                if (a != null) {
                    // only nmethod, Method, MethodData and InstanceKlass needed to
                    // dump replay data

                    CodeBlob cb = VM.getVM().getCodeCache().findBlob(a);
                    if (cb != null && (cb instanceof NMethod)) {
                        ((NMethod)cb).dumpReplayData(out);
                        return;
                    }
                    // assume it is Metadata
                    Metadata meta = Metadata.instantiateWrapperFor(a);
                    if (meta != null) {
                        meta.dumpReplayData(out);
                    } else {
                        usage();
                        return;
                    }
                }
                // Not an address
                boolean all = name.equals("-a");
                Threads threads = VM.getVM().getThreads();
                for (int i = 0; i < threads.getNumberOfThreads(); i++) {
                    JavaThread thread = threads.getJavaThreadAt(i);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    thread.printThreadIDOn(new PrintStream(bos));
                    if (all || bos.toString().equals(name)) {
                        if (thread instanceof CompilerThread) {
                            CompilerThread ct = (CompilerThread)thread;
                            ciEnv env = ct.env();
                            if (env != null) {
                               env.dumpReplayData(out);
                            }
                        }
                    }
                }
            }
        },
        new Command("buildreplayjars", "buildreplayjars [ all | app | boot ]  | [ prefix ]", false) {
            // This is used to dump jar files of all the classes
            // loaded in the core.  Everything with null classloader
            // will go in boot.jar and everything else will go in
            // app.jar. boot.jar usually not needed, unless changed by jvmti.
            public void doit(Tokens t) {
                int tcount = t.countTokens();
                if (tcount > 2) {
                    usage();
                    return;
                }
                try {
                   String prefix = "";
                   String option = "all"; // default
                   switch(tcount) {
                       case 0:
                           break;
                       case 1:
                           option = t.nextToken();
                           if (!option.equalsIgnoreCase("all") && !option.equalsIgnoreCase("app") &&
                               !option.equalsIgnoreCase("root")) {
                              prefix = option;
                              option = "all";
                           }
                           break;
                       case 2:
                           option = t.nextToken();
                           prefix = t.nextToken();
                           break;
                       default:
                           usage();
                           return;
                   }
                   if (!option.equalsIgnoreCase("all") && !option.equalsIgnoreCase("app") &&
                               !option.equalsIgnoreCase("boot")) {
                       usage();
                       return;
                   }
                   ClassDump cd = new ClassDump();
                   if (option.equalsIgnoreCase("all") || option.equalsIgnoreCase("boot")) {
                     cd.setClassFilter(new BootFilter());
                     cd.setJarOutput(prefix + "boot.jar");
                     cd.run();
                   }
                   if (option.equalsIgnoreCase("all") || option.equalsIgnoreCase("app")) {
                     cd.setClassFilter(new NonBootFilter());
                     cd.setJarOutput(prefix + "app.jar");
                     cd.run();
                   }
                } catch (IOException ioe) {
                   ioe.printStackTrace();
                }
            }
        },
        new Command("findsym", "findsym name", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    String result = VM.getVM().getDebugger().findSymbol(t.nextToken());
                    out.println(result == null ? "Symbol not found" : result);
                }
            }
        },
        new Command("findpc", "findpc address", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    Address a = VM.getVM().getDebugger().parseAddress(t.nextToken());
                    PointerLocation loc = PointerFinder.find(a);
                    loc.printOn(out);
                }
            }
        },
        new Command("symbol", "symbol address", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    Address a = VM.getVM().getDebugger().parseAddress(t.nextToken());
                    Symbol.create(a).printValueOn(out);
                    out.println();
                }
            }
        },
        new Command("flags", "flags [ flag | -nd ]", false) {
            public void doit(Tokens t) {
                int tokens = t.countTokens();
                if (tokens != 0 && tokens != 1) {
                    usage();
                } else {
                    String name = tokens > 0 ? t.nextToken() : null;
                    boolean nonDefault = false;
                    if (name != null && name.equals("-nd")) {
                        name = null;
                        nonDefault = true;
                    }

                    VM.Flag[] flags = VM.getVM().getCommandLineFlags();
                    if (flags == null) {
                        out.println("Command Flag info not available (use 1.4.1_03 or later)!");
                    } else {
                        boolean printed = false;
                        for (int f = 0; f < flags.length; f++) {
                            VM.Flag flag = flags[f];
                            if (name == null || flag.getName().equals(name)) {

                                if (nonDefault && (flag.getOrigin() == VM.Flags_DEFAULT)) {
                                    // only print flags which aren't their defaults
                                    continue;
                                }
                                out.println(flag.getName() + " = " + flag.getValue() + " " + flag.getOriginString());
                                printed = true;
                            }
                        }
                        if (name != null && !printed) {
                            out.println("Couldn't find flag: " + name);
                        }
                    }
                }
            }
        },
        new Command("help", "help [ command ]", true) {
            public void doit(Tokens t) {
                int tokens = t.countTokens();
                Command cmd = null;
                if (tokens == 1) {
                    cmd = findCommand(t.nextToken());
                }

                if (cmd != null) {
                    cmd.usage();
                } else if (tokens == 0) {
                    out.println("Available commands:");
                    Object[] keys = commands.keySet().toArray();
                    Arrays.sort(keys, new Comparator<>() {
                             public int compare(Object o1, Object o2) {
                                 return o1.toString().compareTo(o2.toString());
                             }
                          });
                    for (int i = 0; i < keys.length; i++) {
                        out.print("  ");
                        out.println(((Command)commands.get(keys[i])).usage);
                    }
                }
            }
        },
        new Command("history", "history", true) {
            public void doit(Tokens t) {
                int tokens = t.countTokens();
                if (tokens != 0 && (tokens != 1 || !t.nextToken().equals("-h"))) {
                    usage();
                    return;
                }
                boolean printIndex = tokens == 0;
                for (int i = 0; i < history.size(); i++) {
                    if (printIndex) out.print(i + " ");
                    out.println(history.get(i));
                }
            }
        },
        // decode raw address
        new Command("dis", "dis address [length]", false) {
            public void doit(Tokens t) {
                int tokens = t.countTokens();
                if (tokens != 1 && tokens != 2) {
                    usage();
                    return;
                }
                String name = t.nextToken();
                Address addr = null;
                int len = 0x10; // default length
                try {
                    addr = VM.getVM().getDebugger().parseAddress(name);
                } catch (NumberFormatException e) {
                   out.println(e);
                   return;
                }
                if (tokens == 2) {
                    try {
                        len = Integer.parseInt(t.nextToken());
                    } catch (NumberFormatException e) {
                        out.println(e);
                        return;
                    }
                }
                HTMLGenerator generator = new HTMLGenerator(false);
                out.println(generator.genHTMLForRawDisassembly(addr, len));
            }

        },
        // decode codeblob or nmethod
        new Command("disassemble", "disassemble address", false) {
            public void doit(Tokens t) {
                int tokens = t.countTokens();
                if (tokens != 1) {
                    usage();
                    return;
                }
                String name = t.nextToken();
                Address addr = null;
                try {
                    addr = VM.getVM().getDebugger().parseAddress(name);
                } catch (NumberFormatException e) {
                   out.println(e);
                   return;
                }

                HTMLGenerator generator = new HTMLGenerator(false);
                out.println(generator.genHTML(addr));
            }
        },
        // print Java bytecode disassembly
        new Command("jdis", "jdis address", false) {
            public void doit(Tokens t) {
                int tokens = t.countTokens();
                if (tokens != 1) {
                    usage();
                    return;
                }
                Address a = VM.getVM().getDebugger().parseAddress(t.nextToken());
                Method m = (Method)Metadata.instantiateWrapperFor(a);
                HTMLGenerator html = new HTMLGenerator(false);
                out.println(html.genHTML(m));
            }
        },
        new Command("revptrs", "revptrs address", false) {
            public void doit(Tokens t) {
                int tokens = t.countTokens();
                if (tokens != 1 && (tokens != 2 || !t.nextToken().equals("-c"))) {
                    usage();
                    return;
                }
                boolean chase = tokens == 2;
                ReversePtrs revptrs = VM.getVM().getRevPtrs();
                if (revptrs == null) {
                    out.println("Computing reverse pointers...");
                    ReversePtrsAnalysis analysis = new ReversePtrsAnalysis();
                    final boolean[] complete = new boolean[1];
                    HeapProgressThunk thunk = new HeapProgressThunk() {
                            public void heapIterationFractionUpdate(double d) {}
                            public synchronized void heapIterationComplete() {
                                complete[0] = true;
                                notify();
                            }
                        };
                    analysis.setHeapProgressThunk(thunk);
                    analysis.run();
                    while (!complete[0]) {
                        synchronized (thunk) {
                            try {
                                thunk.wait();
                            } catch (Exception e) {
                            }
                        }
                    }
                    revptrs = VM.getVM().getRevPtrs();
                    out.println("Done.");
                }
                Address a = VM.getVM().getDebugger().parseAddress(t.nextToken());
                if (VM.getVM().getUniverse().heap().isInReserved(a)) {
                    OopHandle handle = a.addOffsetToAsOopHandle(0);
                    Oop oop = VM.getVM().getObjectHeap().newOop(handle);
                    ArrayList ptrs = revptrs.get(oop);
                    if (ptrs == null) {
                        out.println("no live references to " + a);
                    } else {
                        if (chase) {
                            while (ptrs.size() == 1) {
                                LivenessPathElement e = (LivenessPathElement)ptrs.get(0);
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                Oop.printOopValueOn(e.getObj(), new PrintStream(bos));
                                out.println(bos.toString());
                                ptrs = revptrs.get(e.getObj());
                            }
                        } else {
                            for (int i = 0; i < ptrs.size(); i++) {
                                LivenessPathElement e = (LivenessPathElement)ptrs.get(i);
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                Oop.printOopValueOn(e.getObj(), new PrintStream(bos));
                                out.println(bos.toString());
                                oop = e.getObj();
                            }
                        }
                    }
                }
            }
        },
        new Command("printmdo", "printmdo [ -a | expression ]", false) {
            // Print every MDO in the heap or the one referenced by expression.
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    String s = t.nextToken();
                    if (s.equals("-a")) {
                        ClassLoaderDataGraph cldg = VM.getVM().getClassLoaderDataGraph();
                        cldg.classesDo(new ClassLoaderDataGraph.ClassVisitor() {
                                public void visit(Klass k) {
                                    if (k instanceof InstanceKlass) {
                                        MethodArray methods = ((InstanceKlass)k).getMethods();
                                        for (int i = 0; i < methods.length(); i++) {
                                            Method m = methods.at(i);
                                            MethodData mdo = m.getMethodData();
                                            if (mdo != null) {
                                                out.println("MethodData " + mdo.getAddress() + " for " +
                                                    "method " + m.getMethodHolder().getName().asString() + "." +
                                                    m.getName().asString() +
                                                            m.getSignature().asString() + "@" + m.getAddress());
                                                mdo.printDataOn(out);
                                    }
                                }
                                    }
                                }
                            }
                            );
                    } else {
                        Address a = VM.getVM().getDebugger().parseAddress(s);
                        MethodData mdo = (MethodData) Metadata.instantiateWrapperFor(a);
                        mdo.printDataOn(out);
                    }
                }
            }
        },
        new Command("printall", "printall", false) {
            // Print every MDO in the heap or the one referenced by expression.
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                } else {
                    ClassLoaderDataGraph cldg = VM.getVM().getClassLoaderDataGraph();
                    cldg.classesDo(new ClassLoaderDataGraph.ClassVisitor() {
                            public void visit(Klass k) {
                                if (k instanceof InstanceKlass && ((InstanceKlass)k).getConstants().getCache() != null) {
                                    MethodArray methods = ((InstanceKlass)k).getMethods();
                                    for (int i = 0; i < methods.length(); i++) {
                                        Method m = methods.at(i);
                                        HTMLGenerator gen = new HTMLGenerator(false);
                                        out.println(gen.genHTML(m));
                                    }
                                }
                            }
                        }
                        );
                }
            }
        },
        new Command("dumpideal", "dumpideal { -a | id }", false) {
            // Do a full dump of the nodes reachable from root in each compiler thread.
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    String name = t.nextToken();
                    boolean all = name.equals("-a");
                    Threads threads = VM.getVM().getThreads();
                    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
                        JavaThread thread = threads.getJavaThreadAt(i);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        thread.printThreadIDOn(new PrintStream(bos));
                        if (all || bos.toString().equals(name)) {
                          if (thread instanceof CompilerThread) {
                            CompilerThread ct = (CompilerThread)thread;
                            out.println(ct);
                            ciEnv env = ct.env();
                            if (env != null) {
                              Compile c = env.compilerData();
                              c.root().dump(9999, out);
                            } else {
                              out.println("  not compiling");
                            }
                          }
                        }
                    }
                }
            }
        },
        new Command("dumpcfg", "dumpcfg { -a | id }", false) {
            // Dump the PhaseCFG for every compiler thread that has one live.
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    String name = t.nextToken();
                    boolean all = name.equals("-a");
                    Threads threads = VM.getVM().getThreads();
                    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
                        JavaThread thread = threads.getJavaThreadAt(i);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        thread.printThreadIDOn(new PrintStream(bos));
                        if (all || bos.toString().equals(name)) {
                          if (thread instanceof CompilerThread) {
                            CompilerThread ct = (CompilerThread)thread;
                            out.println(ct);
                            ciEnv env = ct.env();
                            if (env != null) {
                              Compile c = env.compilerData();
                              c.cfg().dump(out);
                            }
                          }
                        }
                    }
                }
            }
        },
        new Command("dumpilt", "dumpilt { -a | id }", false) {
            // dumps the InlineTree of a C2 compile
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    String name = t.nextToken();
                    boolean all = name.equals("-a");
                    Threads threads = VM.getVM().getThreads();
                    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
                        JavaThread thread = threads.getJavaThreadAt(i);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        thread.printThreadIDOn(new PrintStream(bos));
                        if (all || bos.toString().equals(name)) {
                            if (thread instanceof CompilerThread) {
                                CompilerThread ct = (CompilerThread)thread;
                                ciEnv env = ct.env();
                                if (env != null) {
                                    Compile c = env.compilerData();
                                    InlineTree ilt = c.ilt();
                                    if (ilt != null) {
                                        ilt.print(out);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        new Command("vmstructsdump", "vmstructsdump", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                    return;
                }

                // Dump a copy of the type database in a form that can
                // be read back.
                Iterator i = agent.getTypeDataBase().getTypes();
                // Make sure the types are emitted in an order than can be read back in
                HashSet<String> emitted = new HashSet<>();
                Stack<Type> pending = new Stack<>();
                while (i.hasNext()) {
                    Type n = (Type)i.next();
                    if (emitted.contains(n.getName())) {
                        continue;
                    }

                    while (n != null && !emitted.contains(n.getName())) {
                        pending.push(n);
                        n = n.getSuperclass();
                    }
                    while (!pending.empty()) {
                        n = (Type)pending.pop();
                        dumpType(n);
                        emitted.add(n.getName());
                    }
                }
                i = agent.getTypeDataBase().getTypes();
                while (i.hasNext()) {
                    dumpFields((Type)i.next(), false);
                }
            }
        },

        new Command("inspect", "inspect expression", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    Address a = VM.getVM().getDebugger().parseAddress(t.nextToken());
                    SimpleTreeNode node = null;
                    if (VM.getVM().getUniverse().heap().isInReserved(a)) {
                        OopHandle handle = a.addOffsetToAsOopHandle(0);
                        Oop oop = VM.getVM().getObjectHeap().newOop(handle);
                        node = new OopTreeNodeAdapter(oop, null);

                        out.println("instance of " + node.getValue() +
                                    " (size = " + oop.getObjectSize() + ")");
                    } else if (VM.getVM().getCodeCache().contains(a)) {
                        CodeBlob blob = VM.getVM().getCodeCache().findBlobUnsafe(a);
                        a = blob.headerBegin();
                    }
                    if (node == null) {
                        Type type = VM.getVM().getTypeDataBase().guessTypeForAddress(a);
                        if (type == null && VM.getVM().isSharingEnabled()) {
                            // Check if the value falls in the _md_region
                            Address loc1 = a.getAddressAt(0);
                            FileMapInfo cdsFileMapInfo = VM.getVM().getFileMapInfo();
                            if (cdsFileMapInfo.inCopiedVtableSpace(loc1)) {
                               type = cdsFileMapInfo.getTypeForVptrAddress(loc1);
                            }

                        }
                        if (type != null) {
                            out.println("Type is " + type.getName() + " (size of " + type.getSize() + ")");
                            node = new CTypeTreeNodeAdapter(a, type, null);
                        }
                    }
                    if (node != null) {
                        printNode(node);
                    }
                }
            }
        },
        new Command("jhisto", "jhisto", false) {
            public void doit(Tokens t) {
                 ObjectHistogram histo = new ObjectHistogram();
                 histo.run(out, err);
            }
        },
        new Command("jstack", "jstack [-v]", false) {
            public void doit(Tokens t) {
                boolean verbose = false;
                if (t.countTokens() > 0 && t.nextToken().equals("-v")) {
                    verbose = true;
                }
                StackTrace jstack = new StackTrace(verbose, true);
                jstack.run(out);
            }
        },
        new Command("print", "print expression", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    Address a = VM.getVM().getDebugger().parseAddress(t.nextToken());
                    HTMLGenerator gen = new HTMLGenerator(false);
                    out.println(gen.genHTML(a));
                }
            }
        },
        new Command("printas", "printas type expression", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 2) {
                    usage();
                } else {
                    Type type = agent.getTypeDataBase().lookupType(t.nextToken());
                    Address a = VM.getVM().getDebugger().parseAddress(t.nextToken());
                    CTypeTreeNodeAdapter node = new CTypeTreeNodeAdapter(a, type, null);

                    out.println("pointer to " + type + " @ " + a +
                                " (size = " + type.getSize() + ")");
                    printNode(node);
                }
            }
        },
        new Command("printstatics", "printstatics [ type ]", false) {
            public void doit(Tokens t) {
                if (t.countTokens() > 1) {
                    usage();
                } else {
                    if (t.countTokens() == 0) {
                        out.println("All known static fields");
                        printNode(new CTypeTreeNodeAdapter(agent.getTypeDataBase().getTypes()));
                    } else {
                        Type type = agent.getTypeDataBase().lookupType(t.nextToken());
                        out.println("Static fields of " + type.getName());
                        printNode(new CTypeTreeNodeAdapter(type));
                    }
                }
            }
        },
        new Command("pmap", "pmap", false) {
            public void doit(Tokens t) {
                PMap pmap = new PMap(debugger.getAgent());
                pmap.run(out, debugger.getAgent().getDebugger());
            }
        },
        new Command("pstack", "pstack [-v]", false) {
            public void doit(Tokens t) {
                boolean verbose = false;
                if (t.countTokens() > 0 && t.nextToken().equals("-v")) {
                    verbose = true;
                }
                PStack pstack = new PStack(verbose, true, debugger.getAgent());
                pstack.run(out, debugger.getAgent().getDebugger());
            }
        },
        new Command("quit", true) {
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                } else {
                    debugger.detach();
                    quit = true;
                }
            }
        },
        new Command("echo", "echo [ true | false ]", true) {
            public void doit(Tokens t) {
                if (t.countTokens() == 0) {
                    out.println("echo is " + doEcho);
                } else if (t.countTokens() == 1) {
                    doEcho = Boolean.valueOf(t.nextToken()).booleanValue();
                } else {
                    usage();
                }
            }
        },
        new Command("versioncheck", "versioncheck [ true | false ]", true) {
            public void doit(Tokens t) {
                if (t.countTokens() == 0) {
                    out.println("versioncheck is " +
                                (System.getProperty("sun.jvm.hotspot.runtime.VM.disableVersionCheck") == null));
                } else if (t.countTokens() == 1) {
                    if (Boolean.valueOf(t.nextToken()).booleanValue()) {
                        System.clearProperty("sun.jvm.hotspot.runtime.VM.disableVersionCheck");
                    } else {
                        System.setProperty("sun.jvm.hotspot.runtime.VM.disableVersionCheck", "true");
                    }
                } else {
                    usage();
                }
            }
        },
        new Command("scanoops", "scanoops start end [ type ]", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 2 && t.countTokens() != 3) {
                    usage();
                } else {
                    long stride = VM.getVM().getAddressSize();
                    Address base = VM.getVM().getDebugger().parseAddress(t.nextToken());
                    Address end  = VM.getVM().getDebugger().parseAddress(t.nextToken());
                    Klass klass = null;
                    if (t.countTokens() == 1) {
                        klass = SystemDictionaryHelper.findInstanceKlass(t.nextToken());
                        if (klass == null) {
                            out.println("No such type.");
                            return;
                        }
                    }
                    while (base != null && base.lessThan(end)) {
                        long step = stride;
                        OopHandle handle = base.addOffsetToAsOopHandle(0);
                        if (RobustOopDeterminator.oopLooksValid(handle)) {
                            try {
                                Oop oop = VM.getVM().getObjectHeap().newOop(handle);
                                if (klass == null || oop.getKlass().isSubtypeOf(klass))
                                    out.println(handle.toString() + " " + oop.getKlass().getName().asString());
                                step = oop.getObjectSize();
                            } catch (UnknownOopException ex) {
                                // ok
                            } catch (RuntimeException ex) {
                                ex.printStackTrace();
                            }
                        }
                        base = base.addOffsetTo(step);
                    }
                }
            }
        },
        new Command("intConstant", "intConstant [ name [ value ] ]", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1 && t.countTokens() != 0 && t.countTokens() != 2) {
                    usage();
                    return;
                }
                HotSpotTypeDataBase db = (HotSpotTypeDataBase)agent.getTypeDataBase();
                if (t.countTokens() == 1) {
                    String name = t.nextToken();
                    out.println("intConstant " + name + " " + db.lookupIntConstant(name));
                } else if (t.countTokens() == 0) {
                    Iterator i = db.getIntConstants();
                    while (i.hasNext()) {
                        String name = (String)i.next();
                        out.println("intConstant " + name + " " + db.lookupIntConstant(name));
                    }
                } else if (t.countTokens() == 2) {
                    String name = t.nextToken();
                    Integer value = Integer.valueOf(t.nextToken());
                    db.addIntConstant(name, value);
                }
            }
        },
        new Command("longConstant", "longConstant [ name [ value ] ]", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1 && t.countTokens() != 0 && t.countTokens() != 2) {
                    usage();
                    return;
                }
                HotSpotTypeDataBase db = (HotSpotTypeDataBase)agent.getTypeDataBase();
                if (t.countTokens() == 1) {
                    String name = t.nextToken();
                    out.println("longConstant " + name + " " + db.lookupLongConstant(name));
                } else if (t.countTokens() == 0) {
                    Iterator i = db.getLongConstants();
                    while (i.hasNext()) {
                        String name = (String)i.next();
                        out.println("longConstant " + name + " " + db.lookupLongConstant(name));
                    }
                } else if (t.countTokens() == 2) {
                    String name = t.nextToken();
                    Long value = Long.valueOf(t.nextToken());
                    db.addLongConstant(name, value);
                }
            }
        },
        new Command("field", "field [ type [ name fieldtype isStatic offset address ] ]", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1 && t.countTokens() != 0 && t.countTokens() != 6) {
                    usage();
                    return;
                }
                if (t.countTokens() == 1) {
                    Type type = agent.getTypeDataBase().lookupType(t.nextToken());
                    dumpFields(type);
                } else if (t.countTokens() == 0) {
                    Iterator i = agent.getTypeDataBase().getTypes();
                    while (i.hasNext()) {
                        dumpFields((Type)i.next());
                    }
                } else {
                    BasicType containingType = (BasicType)agent.getTypeDataBase().lookupType(t.nextToken());

                    String fieldName = t.nextToken();

                    // The field's Type must already be in the database -- no exceptions
                    Type fieldType = agent.getTypeDataBase().lookupType(t.nextToken());

                    boolean isStatic = Boolean.valueOf(t.nextToken()).booleanValue();
                    long offset = Long.parseLong(t.nextToken());
                    Address staticAddress = parseAddress(t.nextToken());
                    if (isStatic && staticAddress == null) {
                        staticAddress = lookup(containingType.getName() + "::" + fieldName);
                    }

                    // check to see if the field already exists
                    Iterator i = containingType.getFields();
                    while (i.hasNext()) {
                        Field f = (Field) i.next();
                        if (f.getName().equals(fieldName)) {
                            if (f.isStatic() != isStatic) {
                                throw new RuntimeException("static/nonstatic mismatch: " + t.input);
                            }
                            if (!isStatic) {
                                if (f.getOffset() != offset) {
                                    throw new RuntimeException("bad redefinition of field offset: " + t.input);
                                }
                            } else {
                                if (!f.getStaticFieldAddress().equals(staticAddress)) {
                                    throw new RuntimeException("bad redefinition of field location: " + t.input);
                                }
                            }
                            if (f.getType() != fieldType) {
                                throw new RuntimeException("bad redefinition of field type: " + t.input);
                            }
                            return;
                        }
                    }

                    // Create field by type
                    HotSpotTypeDataBase db = (HotSpotTypeDataBase)agent.getTypeDataBase();
                    db.createField(containingType,
                                   fieldName, fieldType,
                                   isStatic,
                                   offset,
                                   staticAddress);

                }
            }

        },
        new Command("tokenize", "tokenize ...", true) {
            public void doit(Tokens t) {
                while (t.hasMoreTokens()) {
                    out.println("\"" + t.nextToken() + "\"");
                }
            }
        },
        new Command("type", "type [ type [ name super isOop isInteger isUnsigned size ] ]", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1 && t.countTokens() != 0 && t.countTokens() != 6) {
                    usage();
                    return;
                }
                if (t.countTokens() == 6) {
                    String typeName = t.nextToken();
                    String superclassName = t.nextToken();
                    if (superclassName.equals("null")) {
                        superclassName = null;
                    }
                    boolean isOop = Boolean.valueOf(t.nextToken()).booleanValue();
                    boolean isInteger = Boolean.valueOf(t.nextToken()).booleanValue();
                    boolean isUnsigned = Boolean.valueOf(t.nextToken()).booleanValue();
                    long size = Long.parseLong(t.nextToken());

                    BasicType type = null;
                    try {
                        type = (BasicType)agent.getTypeDataBase().lookupType(typeName);
                    } catch (RuntimeException e) {
                    }
                    if (type != null) {
                        if (type.isOopType() != isOop) {
                            throw new RuntimeException("oop mismatch in type definition: " + t.input);
                        }
                        if (type.isCIntegerType() != isInteger) {
                            throw new RuntimeException("integer type mismatch in type definition: " + t.input);
                        }
                        if (type.isCIntegerType() && (((CIntegerType)type).isUnsigned()) != isUnsigned) {
                            throw new RuntimeException("unsigned mismatch in type definition: " + t.input);
                        }
                        if (type.getSuperclass() == null) {
                            if (superclassName != null) {
                                if (type.getSize() == -1) {
                                    type.setSuperclass(agent.getTypeDataBase().lookupType(superclassName));
                                } else {
                                    throw new RuntimeException("unexpected superclass in type definition: " + t.input);
                                }
                            }
                        } else {
                            if (superclassName == null) {
                                throw new RuntimeException("missing superclass in type definition: " + t.input);
                            }
                            if (!type.getSuperclass().getName().equals(superclassName)) {
                                throw new RuntimeException("incorrect superclass in type definition: " + t.input);
                            }
                        }
                        if (type.getSize() != size) {
                            if (type.getSize() == -1) {
                                type.setSize(size);
                            }
                            throw new RuntimeException("size mismatch in type definition: " + t.input);
                        }
                        return;
                    }

                    // Create type
                    HotSpotTypeDataBase db = (HotSpotTypeDataBase)agent.getTypeDataBase();
                    db.createType(typeName, superclassName, isOop, isInteger, isUnsigned, size);
                } else if (t.countTokens() == 1) {
                    Type type = agent.getTypeDataBase().lookupType(t.nextToken());
                    dumpType(type);
                } else {
                    Iterator i = agent.getTypeDataBase().getTypes();
                    // Make sure the types are emitted in an order than can be read back in
                    HashSet<String> emitted = new HashSet<>();
                    Stack<Type> pending = new Stack<>();
                    while (i.hasNext()) {
                        Type n = (Type)i.next();
                        if (emitted.contains(n.getName())) {
                            continue;
                        }

                        while (n != null && !emitted.contains(n.getName())) {
                            pending.push(n);
                            n = n.getSuperclass();
                        }
                        while (!pending.empty()) {
                            n = (Type)pending.pop();
                            dumpType(n);
                            emitted.add(n.getName());
                        }
                    }
                }
            }

        },
        new Command("source", "source filename", true) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                    return;
                }
                String file = t.nextToken();
                BufferedReader savedInput = in;
                try {
                    BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    in = input;
                    run(false);
                } catch (Exception e) {
                    out.println("Error: " + e);
                    if (verboseExceptions) {
                        e.printStackTrace(out);
                    }
                } finally {
                    in = savedInput;
                }

            }
        },
        new Command("search", "search [ heap | perm | rawheap | codecache | threads ] value", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 2) {
                    usage();
                    return;
                }
                String type = t.nextToken();
                final Address value = VM.getVM().getDebugger().parseAddress(t.nextToken());
                final long stride = VM.getVM().getAddressSize();
                if (type.equals("threads")) {
                    Threads threads = VM.getVM().getThreads();
                    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
                        JavaThread thread = threads.getJavaThreadAt(i);
                        Address base = thread.getStackBase();
                        Address end = thread.getLastJavaSP();
                        if (end == null) continue;
                        if (end.lessThan(base)) {
                            Address tmp = base;
                            base = end;
                            end = tmp;
                        }
                        //out.println("Searching " + base + " " + end);
                        while (base != null && base.lessThan(end)) {
                            Address val = base.getAddressAt(0);
                            if (AddressOps.equal(val, value)) {
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                thread.printThreadIDOn(new PrintStream(bos));
                                out.println("found on the stack of thread " + bos.toString() + " at " + base);
                            }
                            base = base.addOffsetTo(stride);
                        }
                    }
                } else if (type.equals("rawheap")) {
                    RawHeapVisitor iterator = new RawHeapVisitor() {
                            public void prologue(long used) {
                            }

                            public void visitAddress(Address addr) {
                                Address val = addr.getAddressAt(0);
                                if (AddressOps.equal(val, value)) {
                                        out.println("found at " + addr);
                                }
                            }
                            public void visitCompOopAddress(Address addr) {
                                Address val = addr.getCompOopAddressAt(0);
                                if (AddressOps.equal(val, value)) {
                                    out.println("found at " + addr);
                                }
                            }
                            public void epilogue() {
                            }
                        };
                    VM.getVM().getObjectHeap().iterateRaw(iterator);
                } else if (type.equals("heap")) {
                    HeapVisitor iterator = new DefaultHeapVisitor() {
                            public boolean doObj(Oop obj) {
                                int index = 0;
                                Address start = obj.getHandle();
                                long end = obj.getObjectSize();
                                while (index < end) {
                                    Address val = start.getAddressAt(index);
                                    if (AddressOps.equal(val, value)) {
                                        out.println("found in " + obj.getHandle());
                                        break;
                                    }
                                    index += 4;
                                }
                                return false;
                            }
                        };
                        VM.getVM().getObjectHeap().iterate(iterator);
                } else if (type.equals("codecache")) {
                    CodeCacheVisitor v = new CodeCacheVisitor() {
                            public void prologue(Address start, Address end) {
                            }
                            public void visit(CodeBlob blob) {
                                boolean printed = false;
                                Address base = blob.getAddress();
                                Address end = base.addOffsetTo(blob.getSize());
                                while (base != null && base.lessThan(end)) {
                                    Address val = base.getAddressAt(0);
                                    if (AddressOps.equal(val, value)) {
                                        if (!printed) {
                                            printed = true;
                                            try {
                                                blob.printOn(out);
                                            } catch (Exception e) {
                                                out.println("Exception printing blob at " + base);
                                                e.printStackTrace();
                                            }
                                        }
                                        out.println("found at " + base + "\n");
                                    }
                                    base = base.addOffsetTo(stride);
                                }
                            }
                            public void epilogue() {
                            }


                        };
                    VM.getVM().getCodeCache().iterate(v);

                }
            }
        },
        new Command("dumpcodecache", "dumpcodecache", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                } else {
                    final PrintStream fout = out;
                    final HTMLGenerator gen = new HTMLGenerator(false);
                    CodeCacheVisitor v = new CodeCacheVisitor() {
                            public void prologue(Address start, Address end) {
                            }
                            public void visit(CodeBlob blob) {
                                fout.println(gen.genHTML(blob.contentBegin()));
                            }
                            public void epilogue() {
                            }


                        };
                    VM.getVM().getCodeCache().iterate(v);
                }
            }
        },
        new Command("where", "where { -a | id }", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    String name = t.nextToken();
                    Threads threads = VM.getVM().getThreads();
                    boolean all = name.equals("-a");
                    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
                        JavaThread thread = threads.getJavaThreadAt(i);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        thread.printThreadIDOn(new PrintStream(bos));
                        if (all || bos.toString().equals(name)) {
                            out.println("Thread " + bos.toString() + " Address: " + thread.getAddress());
                            HTMLGenerator gen = new HTMLGenerator(false);
                            try {
                                out.println(gen.genHTMLForJavaStackTrace(thread));
                            } catch (Exception e) {
                                err.println("Error: " + e);
                                if (verboseExceptions) {
                                    e.printStackTrace(err);
                                }
                            }
                            if (!all) return;
                        }
                    }
                    if (!all) out.println("Couldn't find thread " + name);
                }
            }
        },
        new Command("thread", "thread { -a | id }", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    String name = t.nextToken();
                    Threads threads = VM.getVM().getThreads();
                    boolean all = name.equals("-a");
                    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
                        JavaThread thread = threads.getJavaThreadAt(i);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        thread.printThreadIDOn(new PrintStream(bos));
                        if (all || bos.toString().equals(name)) {
                            out.println("Thread " + bos.toString() + " Address " + thread.getAddress());
                            thread.printInfoOn(out);
                            out.println(" ");
                            if (!all) return;
                        }
                    }
                    if (!all) {
                        out.println("Couldn't find thread " + name);
                    }
                }
            }
        },

        new Command("threads", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                } else {
                    Threads threads = VM.getVM().getThreads();
                    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
                        JavaThread thread = threads.getJavaThreadAt(i);
                        thread.printThreadIDOn(out);
                        out.println(" " + thread.getThreadName());
                        thread.printInfoOn(out);
                        out.println("\n...");
                    }
                }
            }
        },

        new Command("livenmethods", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                } else {
                    ArrayList<NMethod> nmethods = new ArrayList<>();
                    Threads threads = VM.getVM().getThreads();
                    HTMLGenerator gen = new HTMLGenerator(false);
                    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
                        JavaThread thread = threads.getJavaThreadAt(i);
                        try {
                            for (JavaVFrame vf = thread.getLastJavaVFrameDbg(); vf != null; vf = vf.javaSender()) {
                                if (vf instanceof CompiledVFrame) {
                                    NMethod c = ((CompiledVFrame)vf).getCode();
                                    if (!nmethods.contains(c)) {
                                        nmethods.add(c);
                                        out.println(gen.genHTML(c));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        },
        new Command("g1regiondetails", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                } else {
                    CollectedHeap heap = VM.getVM().getUniverse().heap();
                    if (!(heap instanceof G1CollectedHeap)) {
                        out.println("This command is valid only for G1GC.");
                        return;
                    }
                    out.println("Region Details:");
                    ((G1CollectedHeap)heap).printRegionDetails(out);
                }
            }
        },
        new Command("universe", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                } else {
                    Universe u = VM.getVM().getUniverse();
                    out.println("Heap Parameters:");
                    u.heap().printOn(out);
                }
            }
        },
        new Command("verbose", "verbose true | false", true) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    verboseExceptions = Boolean.valueOf(t.nextToken()).booleanValue();
                }
            }
        },
        new Command("assert", "assert true | false", true) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                } else {
                    Assert.ASSERTS_ENABLED = Boolean.valueOf(t.nextToken()).booleanValue();
                }
            }
        },
        new Command("dumpclass", "dumpclass {address | name} [directory]", false) {
            public void doit(Tokens t) {
                int tokenCount = t.countTokens();
                if (tokenCount != 1 && tokenCount != 2) {
                    usage();
                    return;
                }

                /* Find the InstanceKlass for specified class name or class address. */
                InstanceKlass ik = null;
                String classname = t.nextToken();
                if (classname.startsWith("0x")) {
                    // treat it as address
                    VM vm = VM.getVM();
                    Address addr = vm.getDebugger().parseAddress(classname);
                    Metadata metadata = Metadata.instantiateWrapperFor(addr.addOffsetTo(0));
                    if (metadata instanceof InstanceKlass) {
                        ik = (InstanceKlass) metadata;
                    } else {
                        out.println("Specified address is not an InstanceKlass");
                        return;
                    }
                } else {
                    ik = SystemDictionaryHelper.findInstanceKlass(classname);
                    if (ik == null) {
                        out.println("class not found: " + classname);
                        return;
                    }
                }

                /* Compute filename for class. */
                StringBuilder buf = new StringBuilder();
                if (tokenCount > 1) {
                    buf.append(t.nextToken());
                } else {
                    buf.append('.');
                }
                buf.append(File.separatorChar);
                buf.append(ik.getName().asString().replace('/', File.separatorChar));
                buf.append(".class");
                String fileName = buf.toString();
                File file = new File(fileName);

                /* Dump the class file. */
                try {
                    int index = fileName.lastIndexOf(File.separatorChar);
                    File dir = new File(fileName.substring(0, index));
                    dir.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        ClassWriter cw = new ClassWriter(ik, fos);
                        cw.write();
                    }
                } catch (Exception e) {
                    err.println("Error: " + e);
                    if (verboseExceptions) {
                        e.printStackTrace(err);
                    }
                }
            }
        },
        new Command("sysprops", "sysprops", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                    return;
                }
                SysPropsDumper sysProps = new SysPropsDumper();
                sysProps.run();
            }
        },
        new Command("dumpheap", "dumpheap [gz=<1-9>] [filename]", false) {
            public void doit(Tokens t) {
                int cntTokens = t.countTokens();
                if (cntTokens > 2) {
                    err.println("More than 2 options specified: " + cntTokens);
                    usage();
                    return;
                }
                JMap jmap = new JMap();
                String filename = "heap.bin";
                int gzlevel = 0;
                /*
                 * Possible command:
                 *     dumpheap gz=1 file;
                 *     dumpheap gz=1;
                 *     dumpheap file;
                 *     dumpheap
                 *
                 * Use default filename if cntTokens == 0.
                 * Handle cases with cntTokens == 1 or 2.
                 */
                if (cntTokens == 1) { // first argument could be filename or "gz="
                    String option = t.nextToken();
                    if (!option.startsWith("gz=")) {
                        filename = option;
                    } else {
                        gzlevel = parseHeapDumpCompressionLevel(option);
                        if (gzlevel == 0) {
                            usage();
                            return;
                        }
                        filename = "heap.bin.gz";
                    }
                }
                if (cntTokens == 2) { // first argument is "gz=" followed by filename
                    String option = t.nextToken();
                    gzlevel = parseHeapDumpCompressionLevel(option);
                    if (gzlevel == 0) {
                        usage();
                        return;
                    }
                    filename = t.nextToken();
                    if (filename.startsWith("gz=")) {
                        err.println("Filename should not start with \"gz=\": " + filename);
                        usage();
                        return;
                    }
                }
                try {
                    jmap.writeHeapHprofBin(filename, gzlevel);
                } catch (Exception e) {
                    err.println("Error: " + e);
                    if (verboseExceptions) {
                        e.printStackTrace(err);
                    }
                }
            }
        },
        new Command("class", "class name", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 1) {
                    usage();
                    return;
                }
                String classname = t.nextToken();
                InstanceKlass ik = SystemDictionaryHelper.findInstanceKlass(classname);
                if (ik == null) {
                    out.println("class not found: " + classname);
                } else {
                    out.println(ik.getName().asString() + " @" + ik.getAddress());
                }
            }
        },
        new Command("classes", "classes", false) {
            public void doit(Tokens t) {
                if (t.countTokens() != 0) {
                    usage();
                    return;
                }
                ClassLoaderDataGraph cldg = VM.getVM().getClassLoaderDataGraph();
                cldg.classesDo(new ClassLoaderDataGraph.ClassVisitor() {
                        public void visit(Klass k) {
                            out.println(k.getName().asString() + " @" + k.getAddress());
                        }
                    }
                );
            }
        },
    };

    private boolean verboseExceptions = false;
    private ArrayList<String> history = new ArrayList<>();
    private HashMap<String, Command> commands = new HashMap<>();
    private boolean doEcho = false;

    private Command findCommand(String key) {
        return (Command)commands.get(key);
    }

    public void printPrompt() {
        out.print("hsdb> ");
    }

    private DebuggerInterface debugger;
    private HotSpotAgent agent;
    private BufferedReader in;
    private PrintStream out;
    private PrintStream err;

    // called before debuggee attach
    private void preAttach() {
        // nothing for now..
    }

    // called after debuggee attach
    private void postAttach() {
        // nothing for now..
    }

    public void setOutput(PrintStream o) {
        out = o;
    }

    public void setErr(PrintStream e) {
        err = e;
    }

    public CommandProcessor(DebuggerInterface debugger, BufferedReader in, PrintStream out, PrintStream err) {
        this.debugger = debugger;
        this.agent = debugger.getAgent();
        this.in = in;
        this.out = out;
        this.err = err;
        for (int i = 0; i < commandList.length; i++) {
            Command c = commandList[i];
            if (commands.get(c.name) != null) {
                throw new InternalError(c.name + " has multiple definitions");
            }
            commands.put(c.name, c);
        }
        if (debugger.isAttached()) {
            postAttach();
        }
    }


    public void run(boolean prompt) {
        // Process interactive commands.
        while (!quit) {
            if (prompt) printPrompt();
            String ln = null;
            try {
                ln = in.readLine();
            } catch (IOException e) {
            }
            if (ln == null) {
                if (prompt) err.println("Input stream closed.");
                return;
            }

            executeCommand(ln, prompt);
        }
    }

    static Pattern historyPattern = Pattern.compile("([\\\\]?)((!\\*)|(!\\$)|(!!-?)|(!-?[0-9][0-9]*)|(![a-zA-Z][^ ]*))");

    public void executeCommand(String ln, boolean putInHistory) {
        if (ln.indexOf('!') != -1) {
            int size = history.size();
            if (size == 0) {
                ln = "";
                err.println("History is empty");
            } else {
                StringBuilder result = new StringBuilder();
                Matcher m = historyPattern.matcher(ln);
                int start = 0;
                while (m.find()) {
                    // Capture the text preceding the matched text.
                    if (m.start() > start) {
                        result.append(ln.substring(start, m.start()));
                    }
                    start = m.end();

                    if (m.group(1).length() != 0) {
                        // This means we matched a `\` before the '!'. Don't do any history
                        // expansion in this case. Just capture what matched after the `\`.
                        result.append(m.group(2));
                        continue;
                    }

                    String cmd = m.group(2);
                    if (cmd.equals("!!")) {
                        result.append((String)history.get(history.size() - 1));
                    } else if (cmd.equals("!!-")) {
                        Tokens item = new Tokens((String)history.get(history.size() - 1));
                        item.trim(1);
                        result.append(item.join(" "));
                    } else if (cmd.equals("!*")) {
                        Tokens item = new Tokens((String)history.get(history.size() - 1));
                        item.nextToken();
                        result.append(item.join(" "));
                    } else if (cmd.equals("!$")) {
                        Tokens item = new Tokens((String)history.get(history.size() - 1));
                        result.append(item.at(item.countTokens() - 1));
                    } else {
                        String tail = cmd.substring(1);
                        switch (tail.charAt(0)) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                        case '-': {
                            int index = Integer.parseInt(tail);
                            if (index < 0) {
                                index = history.size() + index;
                            }
                            if (index > size) {
                                err.println("No such history item");
                            } else {
                                result.append((String)history.get(index));
                            }
                            break;
                        }
                        default: {
                            for (int i = history.size() - 1; i >= 0; i--) {
                                String s = (String)history.get(i);
                                if (s.startsWith(tail)) {
                                    result.append(s);
                                    break; // only capture the most recent match in the history
                                }
                            }
                        }
                        }
                    }
                }
                if (result.length() == 0) {
                    err.println("malformed history reference");
                    ln = "";
                } else {
                    if (start < ln.length()) {
                        result.append(ln.substring(start));
                    }
                    ln = result.toString();
                    if (!doEcho) {
                        out.println(ln);
                    }
                }
            }
        }

        if (doEcho) {
            out.println("+ " + ln);
        }

        PrintStream redirect = null;
        Tokens t = new Tokens(ln);
        if (t.hasMoreTokens()) {
            boolean error = false;
            if (putInHistory) history.add(ln);
            int len = t.countTokens();
            if (len > 2) {
                String r = t.at(len - 2);
                if (r.equals(">") || r.equals(">>")) {
                    boolean append = r.length() == 2;
                    String file = t.at(len - 1);
                    try {
                        redirect = new PrintStream(new BufferedOutputStream(new FileOutputStream(file, append)));
                        t.trim(2);
                    } catch (Exception e) {
                        out.println("Error: " + e);
                        if (verboseExceptions) {
                            e.printStackTrace(out);
                        }
                        error = true;
                    }
                }
            }
            if (!error) {
                PrintStream savedout = out;
                if (redirect != null) {
                    out = redirect;
                }
                try {
                    executeCommand(t);
                } catch (Exception e) {
                    err.println("Error: " + e);
                    if (verboseExceptions) {
                        e.printStackTrace(err);
                    }
                } finally {
                    if (redirect != null) {
                        out = savedout;
                        redirect.close();
                    }
                }
            }
        }
    }

    void executeCommand(Tokens args) {
        String cmd = args.nextToken();

        Command doit = findCommand(cmd);

        /*
         * Check for an unknown command
         */
        if (doit == null) {
            out.println("Unrecognized command.  Try help...");
        } else if (!debugger.isAttached() && !doit.okIfDisconnected) {
            out.println("Command not valid until attached to a VM");
        } else {
            try {
                doit.doit(args);
            } catch (Exception e) {
                out.println("Error: " + e);
                if (verboseExceptions) {
                    e.printStackTrace(out);
                }
            }
        }
    }

    /* Parse compression level
     * @return   1-9    compression level
     *           0      compression level is illegal
     */
    private int parseHeapDumpCompressionLevel(String option) {

        String[] keyValue = option.split("=");
        if (!keyValue[0].equals("gz")) {
            err.println("Expected option is \"gz=\"");
            return 0;
        }
        if (keyValue.length != 2) {
            err.println("Exactly one argument is expected for option \"gz\"");
            return 0;
        }
        int gzl = 0;
        String level = keyValue[1];
        try {
            gzl = Integer.parseInt(level);
        } catch (NumberFormatException e) {
            err.println("gz option value not an integer ("+level+")");
            return 0;
        }
        if (gzl < 1 || gzl > 9) {
            err.println("Compression level out of range (1-9): " + level);
            return 0;
        }
        return gzl;
    }
}
