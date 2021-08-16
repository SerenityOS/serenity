/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.data.serialization;

import com.sun.hotspot.igv.data.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Thomas Wuerthinger
 */
public class Printer {

    private InputStream in;

    public Printer() {
        this(null);
    }

    public Printer(InputStream inputStream) {
        this.in = inputStream;
    }

    public void export(Writer writer, GraphDocument document) {

        XMLWriter xmlWriter = new XMLWriter(writer);

        try {
            export(xmlWriter, document);
        } catch (IOException ex) {
        }
    }

    private void export(XMLWriter xmlWriter, GraphDocument document) throws IOException {
        xmlWriter.startTag(Parser.ROOT_ELEMENT);
        xmlWriter.writeProperties(document.getProperties());
        for (FolderElement e : document.getElements()) {
            if (e instanceof Group) {
                export(xmlWriter, (Group) e);
            } else if (e instanceof InputGraph) {
                export(xmlWriter, (InputGraph)e, null, false);
            }
        }

        xmlWriter.endTag();
        xmlWriter.flush();
    }

    private void export(XMLWriter writer, Group g) throws IOException {
        Properties attributes = new Properties();
        attributes.setProperty("difference", Boolean.toString(true));
        writer.startTag(Parser.GROUP_ELEMENT, attributes);
        writer.writeProperties(g.getProperties());

        boolean shouldExport = true;
        if (in != null) {
            char c = (char) in.read();
            if (c != 'y') {
                shouldExport = false;
            }
        }

        if (shouldExport) {
            if (g.getMethod() != null) {
                export(writer, g.getMethod());
            }

            InputGraph previous = null;
            for (FolderElement e : g.getElements()) {
                if (e instanceof InputGraph) {
                    InputGraph graph = (InputGraph) e;
                    export(writer, graph, previous, true);
                    previous = graph;
                } else if (e instanceof Group) {
                    export(writer, (Group) e);
                }
            }
        }

        writer.endTag();
    }

    public void export(XMLWriter writer, InputGraph graph, InputGraph previous, boolean difference) throws IOException {

        writer.startTag(Parser.GRAPH_ELEMENT);
        writer.writeProperties(graph.getProperties());
        writer.startTag(Parser.NODES_ELEMENT);

        Set<InputNode> removed = new HashSet<>();
        Set<InputNode> equal = new HashSet<>();

        if (previous != null) {
            for (InputNode n : previous.getNodes()) {
                int id = n.getId();
                InputNode n2 = graph.getNode(id);
                if (n2 == null) {
                    removed.add(n);
                } else if (n.equals(n2)) {
                    equal.add(n);
                }
            }
        }

        if (difference) {
            for (InputNode n : removed) {
                writer.simpleTag(Parser.REMOVE_NODE_ELEMENT, new Properties(Parser.NODE_ID_PROPERTY, Integer.toString(n.getId())));
            }
        }

        for (InputNode n : graph.getNodes()) {
            if (!difference || !equal.contains(n)) {
                writer.startTag(Parser.NODE_ELEMENT, new Properties(Parser.NODE_ID_PROPERTY, Integer.toString(n.getId())));
                writer.writeProperties(n.getProperties());
                writer.endTag();
            }
        }

        writer.endTag();

        writer.startTag(Parser.EDGES_ELEMENT);
        Set<InputEdge> removedEdges = new HashSet<>();
        Set<InputEdge> equalEdges = new HashSet<>();

        if (previous != null) {
            for (InputEdge e : previous.getEdges()) {
                if (graph.getEdges().contains(e)) {
                    equalEdges.add(e);
                } else {
                    removedEdges.add(e);
                }
            }
        }

        if (difference) {
            for (InputEdge e : removedEdges) {
                writer.simpleTag(Parser.REMOVE_EDGE_ELEMENT, createProperties(e));
            }
        }

        for (InputEdge e : graph.getEdges()) {
            if (!difference || !equalEdges.contains(e)) {
                if (!equalEdges.contains(e)) {
                    writer.simpleTag(Parser.EDGE_ELEMENT, createProperties(e));
                }
            }
        }

        writer.endTag();

        writer.startTag(Parser.CONTROL_FLOW_ELEMENT);
        for (InputBlock b : graph.getBlocks()) {
            writer.startTag(Parser.BLOCK_ELEMENT, new Properties(Parser.BLOCK_NAME_PROPERTY, b.getName()));

            if (b.getSuccessors().size() > 0) {
                writer.startTag(Parser.SUCCESSORS_ELEMENT);
                for (InputBlock s : b.getSuccessors()) {
                    writer.simpleTag(Parser.SUCCESSOR_ELEMENT, new Properties(Parser.BLOCK_NAME_PROPERTY, s.getName()));
                }
                writer.endTag();
            }

            if (b.getNodes().size() > 0) {
            writer.startTag(Parser.NODES_ELEMENT);
                for (InputNode n : b.getNodes()) {
                    writer.simpleTag(Parser.NODE_ELEMENT, new Properties(Parser.NODE_ID_PROPERTY, n.getId() + ""));
                }
                writer.endTag();
            }

            writer.endTag();
        }

        writer.endTag();
        writer.endTag();
    }

    private void export(XMLWriter w, InputMethod method) throws IOException {

        w.startTag(Parser.METHOD_ELEMENT, new Properties(Parser.METHOD_BCI_PROPERTY, method.getBci() + "", Parser.METHOD_NAME_PROPERTY, method.getName(), Parser.METHOD_SHORT_NAME_PROPERTY, method.getShortName()));

        w.writeProperties(method.getProperties());

        if (method.getInlined().size() > 0) {
            w.startTag(Parser.INLINE_ELEMENT);
            for (InputMethod m : method.getInlined()) {
                export(w, m);
            }
            w.endTag();
        }

        w.startTag(Parser.BYTECODES_ELEMENT);

        StringBuilder b = new StringBuilder();
        b.append("<![CDATA[\n");
        for (InputBytecode code : method.getBytecodes()) {
            b.append(code.getBci());
            b.append(" ");
            b.append(code.getName());
            b.append("\n");
        }

        b.append("]]>");
        w.write(b.toString());
        w.endTag();
        w.endTag();
    }

    private Properties createProperties(InputEdge edge) {
        Properties p = new Properties();
        if (edge.getToIndex() != 0) {
            p.setProperty(Parser.TO_INDEX_PROPERTY, Integer.toString(edge.getToIndex()));
        }
        if (edge.getFromIndex() != 0) {
            p.setProperty(Parser.FROM_INDEX_PROPERTY, Integer.toString(edge.getFromIndex()));
        }
        p.setProperty(Parser.TO_PROPERTY, Integer.toString(edge.getTo()));
        p.setProperty(Parser.FROM_PROPERTY, Integer.toString(edge.getFrom()));
        p.setProperty(Parser.TYPE_PROPERTY, edge.getType());
        return p;
    }
}
