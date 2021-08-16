/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Window;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Hashtable;

import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.QueueTool.QueueAction;
import org.netbeans.jemmy.operators.Operator;

/**
 * Allows to "dump" current GUI state into XML file. Uses operators' getDump
 * methods to gather the information.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class Dumper {

    /**
     * Prints XML DTD information.
     *
     * @param writer a writer to write to.
     */
    public static void printDTD(PrintWriter writer) {
        printDTD(writer, "");
    }

    /**
     * Prints XML DTD information.
     *
     * @param writer a stream to write to.
     */
    public static void printDTD(PrintStream writer) {
        printDTD(new PrintWriter(writer));
    }

    /**
     * Prints XML DTD information into file.
     *
     * @param fileName a file to write to.
     * @throws FileNotFoundException
     */
    public static void printDTD(String fileName)
            throws FileNotFoundException {
        printDTD(new PrintWriter(new FileOutputStream(fileName)));
    }

    /**
     * Prints component hierarchy (GUI dump) starting from {@code comp}
     * component.
     *
     * @param comp a component to get information from.
     * @param writer a writer to write to.
     */
    public static void dumpComponent(Component comp, final PrintWriter writer, final DumpController listener) {
        QueueTool qt = new QueueTool();
        Component[] comps;
        if (comp != null) {
            comps = new Component[1];
            comps[0] = comp;
        } else {
            comps = Frame.getFrames();
        }
        final Component[] comps_final = comps;
        qt.invokeSmoothly(new QueueAction<Void>("dumpComponent") {
            @Override
            public Void launch() throws Exception {
                printHeader(writer);
                dumpSome("dump", comps_final, writer, "", listener);
                writer.flush();
                return null;
            }
        });
    }

    public static void dumpComponent(Component comp, PrintWriter writer) {
        dumpComponent(comp, writer, new DumpController() {
            @Override
            public boolean onComponentDump(Component comp) {
                return true;
            }

            @Override
            public boolean onPropertyDump(Component comp, String name, String value) {
                return true;
            }
        });
    }

    /**
     * Prints component hierarchy (GUI dump). starting from {@code comp}
     * component.
     *
     * @param comp a component to get information from.
     * @param writer a stream to write to.
     */
    public static void dumpComponent(Component comp, PrintStream writer) {
        dumpComponent(comp, new PrintWriter(writer));
    }

    public static void dumpComponent(Component comp, PrintStream writer, DumpController listener) {
        dumpComponent(comp, new PrintWriter(writer), listener);
    }

    /**
     * Prints component hierarchy (GUI dump) into file.
     *
     * @param comp a component to get information from.
     * @param fileName a file to write to.
     * @throws FileNotFoundException
     */
    public static void dumpComponent(Component comp, String fileName)
            throws FileNotFoundException {
        dumpComponent(comp, new PrintWriter(new FileOutputStream(fileName)));
    }

    public static void dumpComponent(Component comp, String fileName, DumpController listener)
            throws FileNotFoundException {
        dumpComponent(comp, new PrintWriter(new FileOutputStream(fileName)), listener);
    }

    /**
     * Prints all component hierarchy (GUI dump).
     *
     * @param writer a writer to write to.
     */
    public static void dumpAll(PrintWriter writer) {
        dumpComponent(null, writer);
    }

    public static void dumpAll(PrintWriter writer, DumpController listener) {
        dumpComponent(null, writer, listener);
    }

    /**
     * Prints all component hierarchy (GUI dump).
     *
     * @param writer a stream to write to.
     */
    public static void dumpAll(PrintStream writer) {
        dumpAll(new PrintWriter(writer));
    }

    public static void dumpAll(PrintStream writer, DumpController listener) {
        dumpAll(new PrintWriter(writer), listener);
    }

    /**
     * Prints component hierarchy (GUI dump) into file.
     *
     * @param fileName a file to write to.
     * @throws FileNotFoundException
     */
    public static void dumpAll(String fileName)
            throws FileNotFoundException {
        dumpAll(new PrintWriter(new FileOutputStream(fileName)));
    }

    public static void dumpAll(String fileName, DumpController listener)
            throws FileNotFoundException {
        dumpAll(new PrintWriter(new FileOutputStream(fileName)), listener);
    }

    private static final String tabIncrease = "  ";

    private static void printTagStart(PrintWriter writer, String tag, String tab) {
        writer.println(tab + "<" + tag + ">");
    }

    private static void printTagOpening(PrintWriter writer, String tag, String tab) {
        writer.print(tab + "<" + tag);
    }

    private static void printTagClosing(PrintWriter writer, String tag) {
        writer.println(">");
    }

    private static void printTagEnd(PrintWriter writer, String tag, String tab) {
        writer.println(tab + "</" + tag + ">");
    }

    private static void printEmptyTagOpening(PrintWriter writer, String tag, String tab) {
        writer.print(tab + "<" + tag);
    }

    private static void printEmptyTagClosing(PrintWriter writer, String tag) {
        writer.println("/>");
    }

    private static void dumpSome(String tag, Component[] comps, PrintWriter writer, String tab, DumpController listener) {
        if (comps.length > 0) {
            printTagStart(writer, tag, tab);
            for (Component comp : comps) {
                dumpOne(comp, writer, tab + tabIncrease, listener);
            }
            printTagEnd(writer, tag, tab);
        }
    }

    private static void dumpOne(Component component, PrintWriter writer, String tab, DumpController listener) {
        //whether to dump at all
        boolean toDump = listener.onComponentDump(component);
        if (toDump) {
            try {
                Operator oper = Operator.createOperator(component);
                Hashtable<String, Object> componentDump = oper.getDump();
                printTagOpening(writer, "component", tab);
                writer.print(" operator=\""
                        + oper.getClass().getName() + "\"");
                printTagClosing(writer, "component");
                Object[] keys = componentDump.keySet().toArray();
                Arrays.sort(keys);
                String name, value;
                for (Object key : keys) {
                    name = (String) key;
                    value = ((String) componentDump.get(key));
                    if (listener.onPropertyDump(component, name, value)) {
                        printEmptyTagOpening(writer, "property", tab + tabIncrease);
                        writer.print(" name=\""
                                + escape(name) + "\" value=\""
                                + escape(value) + "\"");
                        printEmptyTagClosing(writer, "property");
                    }
                }
            } catch (Exception e) {
                JemmyProperties.getCurrentOutput().printStackTrace(e);
                printTagStart(writer, "component", tab);
                printEmptyTagOpening(writer, "exception", tab + tabIncrease);
                writer.print(" toString=\""
                        + escape(e.toString()) + "\"");
                printEmptyTagClosing(writer, "exception");
            }
        }
        if (component instanceof Window) {
            dumpSome("subwindows", ((Window) component).getOwnedWindows(), writer, tab + tabIncrease, listener);
        }
        if (component instanceof Container) {
            dumpSome("subcomponents", ((Container) component).getComponents(), writer, tab + tabIncrease, listener);
        }
        if (toDump) {
            printTagEnd(writer, "component", tab);
        }
    }

    private static void printHeader(PrintWriter writer) {
        writer.println("<?xml version=\"1.0\"?>");
        writer.println("<!DOCTYPE dump [");
        printDTD(writer, tabIncrease);
        writer.println("]>");
    }

    private static void printDTD(PrintWriter writer, String tab) {
        writer.println(tab + "<!ELEMENT dump (component*)>");
        writer.println(tab + "<!ELEMENT component (property+, subcomponents?, subwindows?, exception?)>");
        writer.println(tab + "<!ELEMENT subcomponents (component+)>");
        writer.println(tab + "<!ELEMENT subwindows (component+)>");
        writer.println(tab + "<!ELEMENT property EMPTY>");
        writer.println(tab + "<!ELEMENT exception EMPTY>");
        writer.println(tab + "<!ATTLIST component");
        writer.println(tab + "          operator CDATA #IMPLIED>");
        writer.println(tab + "<!ATTLIST exception");
        writer.println(tab + "          toString CDATA #REQUIRED>");
        writer.println(tab + "<!ATTLIST property");
        writer.println(tab + "          name  CDATA #REQUIRED");
        writer.println(tab + "          value CDATA #REQUIRED>");
    }

    public static String escape(String str) {
        return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
    }
}
