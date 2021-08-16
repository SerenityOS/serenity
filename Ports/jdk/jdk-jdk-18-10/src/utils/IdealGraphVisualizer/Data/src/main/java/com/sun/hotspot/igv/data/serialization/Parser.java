/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.hotspot.igv.data.serialization.XMLParser.ElementHandler;
import com.sun.hotspot.igv.data.serialization.XMLParser.HandoverElementHandler;
import com.sun.hotspot.igv.data.serialization.XMLParser.TopElementHandler;
import com.sun.hotspot.igv.data.services.GroupCallback;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 *
 * @author Thomas Wuerthinger
 */
public class Parser implements GraphParser {

    public static final String INDENT = "  ";
    public static final String TOP_ELEMENT = "graphDocument";
    public static final String GROUP_ELEMENT = "group";
    public static final String GRAPH_ELEMENT = "graph";
    public static final String ROOT_ELEMENT = "graphDocument";
    public static final String PROPERTIES_ELEMENT = "properties";
    public static final String EDGES_ELEMENT = "edges";
    public static final String PROPERTY_ELEMENT = "p";
    public static final String EDGE_ELEMENT = "edge";
    public static final String NODE_ELEMENT = "node";
    public static final String NODES_ELEMENT = "nodes";
    public static final String REMOVE_EDGE_ELEMENT = "removeEdge";
    public static final String REMOVE_NODE_ELEMENT = "removeNode";
    public static final String METHOD_NAME_PROPERTY = "name";
    public static final String GROUP_NAME_PROPERTY = "name";
    public static final String METHOD_IS_PUBLIC_PROPERTY = "public";
    public static final String METHOD_IS_STATIC_PROPERTY = "static";
    public static final String TRUE_VALUE = "true";
    public static final String NODE_NAME_PROPERTY = "name";
    public static final String EDGE_NAME_PROPERTY = "name";
    public static final String NODE_ID_PROPERTY = "id";
    public static final String FROM_PROPERTY = "from";
    public static final String TO_PROPERTY = "to";
    public static final String TYPE_PROPERTY = "type";
    public static final String PROPERTY_NAME_PROPERTY = "name";
    public static final String GRAPH_NAME_PROPERTY = "name";
    public static final String FROM_INDEX_PROPERTY = "fromIndex";
    public static final String TO_INDEX_PROPERTY = "toIndex";
    public static final String TO_INDEX_ALT_PROPERTY = "index";
    public static final String LABEL_PROPERTY = "label";
    public static final String METHOD_ELEMENT = "method";
    public static final String INLINE_ELEMENT = "inline";
    public static final String BYTECODES_ELEMENT = "bytecodes";
    public static final String METHOD_BCI_PROPERTY = "bci";
    public static final String METHOD_SHORT_NAME_PROPERTY = "shortName";
    public static final String CONTROL_FLOW_ELEMENT = "controlFlow";
    public static final String BLOCK_NAME_PROPERTY = "name";
    public static final String BLOCK_ELEMENT = "block";
    public static final String SUCCESSORS_ELEMENT = "successors";
    public static final String SUCCESSOR_ELEMENT = "successor";
    public static final String ASSEMBLY_ELEMENT = "assembly";
    public static final String DIFFERENCE_PROPERTY = "difference";
    private TopElementHandler<GraphDocument> xmlDocument = new TopElementHandler<>();
    private Map<Group, Boolean> differenceEncoding = new HashMap<>();
    private Map<Group, InputGraph> lastParsedGraph = new HashMap<>();
    private GroupCallback groupCallback;
    private HashMap<String, Integer> idCache = new HashMap<>();
    private ArrayList<Pair<String, String>> blockConnections = new ArrayList<>();
    private int maxId = 0;
    private GraphDocument graphDocument;
    private final ParseMonitor monitor;
    private final ReadableByteChannel channel;
    private boolean invokeLater = true;

    private int lookupID(String i) {
        try {
            return Integer.parseInt(i);
        } catch (NumberFormatException nfe) {
            // ignore
        }
        Integer id = idCache.get(i);
        if (id == null) {
            id = maxId++;
            idCache.put(i, id);
        }
        return id.intValue();
    }

    // <graphDocument>
    private ElementHandler<GraphDocument, Object> topHandler = new ElementHandler<GraphDocument, Object>(TOP_ELEMENT) {

        @Override
        protected GraphDocument start() throws SAXException {
            graphDocument = new GraphDocument();
            return graphDocument;
        }
    };
    // <group>
    private ElementHandler<Group, Folder> groupHandler = new XMLParser.ElementHandler<Group, Folder>(GROUP_ELEMENT) {

        @Override
        protected Group start() throws SAXException {
            final Group group = new Group(this.getParentObject());

            String differenceProperty = this.readAttribute(DIFFERENCE_PROPERTY);
            Parser.this.differenceEncoding.put(group, (differenceProperty != null && (differenceProperty.equals("1") || differenceProperty.equals("true"))));

            ParseMonitor monitor = getMonitor();
            if (monitor != null) {
                monitor.setState(group.getName());
            }

            final Folder parent = getParentObject();
            if (groupCallback == null || parent instanceof Group) {
                Runnable addToParent = () -> parent.addElement(group);
                if (invokeLater) {
                    SwingUtilities.invokeLater(addToParent);
                } else {
                    addToParent.run();
                }
            }

            return group;
        }

        @Override
        protected void end(String text) throws SAXException {
        }
    };
    // <method>
    private ElementHandler<InputMethod, Group> methodHandler = new XMLParser.ElementHandler<InputMethod, Group>(METHOD_ELEMENT) {

        @Override
        protected InputMethod start() throws SAXException {

            InputMethod method = parseMethod(this, getParentObject());
            getParentObject().setMethod(method);
            return method;
        }
    };

    private InputMethod parseMethod(XMLParser.ElementHandler<?,?> handler, Group group) throws SAXException {
        String s = handler.readRequiredAttribute(METHOD_BCI_PROPERTY);
        int bci = 0;
        try {
            bci = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new SAXException(e);
        }
        InputMethod method = new InputMethod(group, handler.readRequiredAttribute(METHOD_NAME_PROPERTY), handler.readRequiredAttribute(METHOD_SHORT_NAME_PROPERTY), bci);
        return method;
    }
    // <bytecodes>
    private HandoverElementHandler<InputMethod> bytecodesHandler = new XMLParser.HandoverElementHandler<InputMethod>(BYTECODES_ELEMENT, true) {

        @Override
        protected void end(String text) throws SAXException {
            getParentObject().setBytecodes(text);
        }
    };
    // <inlined>
    private HandoverElementHandler<InputMethod> inlinedHandler = new XMLParser.HandoverElementHandler<>(INLINE_ELEMENT);
    // <inlined><method>
    private ElementHandler<InputMethod, InputMethod> inlinedMethodHandler = new XMLParser.ElementHandler<InputMethod, InputMethod>(METHOD_ELEMENT) {

        @Override
        protected InputMethod start() throws SAXException {
            InputMethod method = parseMethod(this, getParentObject().getGroup());
            getParentObject().addInlined(method);
            return method;
        }
    };
    // <graph>
    private ElementHandler<InputGraph, Group> graphHandler = new XMLParser.ElementHandler<InputGraph, Group>(GRAPH_ELEMENT) {

        @Override
        protected InputGraph start() throws SAXException {
            String name = readAttribute(GRAPH_NAME_PROPERTY);
            InputGraph curGraph = new InputGraph(name);
            if (differenceEncoding.get(getParentObject())) {
                InputGraph previous = lastParsedGraph.get(getParentObject());
                lastParsedGraph.put(getParentObject(), curGraph);
                if (previous != null) {
                    for (InputNode n : previous.getNodes()) {
                        curGraph.addNode(n);
                    }
                    for (InputEdge e : previous.getEdges()) {
                        curGraph.addEdge(e);
                    }
                }
            }
            ParseMonitor monitor = getMonitor();
            if (monitor != null) {
                monitor.updateProgress();
            }
            return curGraph;
        }

        @Override
        protected void end(String text) throws SAXException {
            // NOTE: Some graphs intentionally don't provide blocks. Instead
            //       they later generate the blocks from other information such
            //       as node properties (example: ServerCompilerScheduler).
            //       Thus, we shouldn't assign nodes that don't belong to any
            //       block to some artificial block below unless blocks are
            //       defined and nodes are assigned to them.

            final InputGraph graph = getObject();
            final Group parent = getParentObject();
            if (graph.getBlocks().size() > 0) {
                boolean blocksContainNodes = false;
                for (InputBlock b : graph.getBlocks()) {
                    if (b.getNodes().size() > 0) {
                        blocksContainNodes = true;
                        break;
                    }
                }

                if (!blocksContainNodes) {
                    graph.clearBlocks();
                    blockConnections.clear();
                } else {
                    // Blocks and their nodes defined: add other nodes to an
                    //  artificial "no block" block
                    InputBlock noBlock = null;
                    for (InputNode n : graph.getNodes()) {
                        if (graph.getBlock(n) == null) {
                            if (noBlock == null) {
                                noBlock = graph.addBlock("(no block)");
                            }

                            noBlock.addNode(n.getId());
                        }

                        assert graph.getBlock(n) != null;
                    }
                }
            }

            // Resolve block successors
            for (Pair<String, String> p : blockConnections) {
                final InputBlock left = graph.getBlock(p.getLeft());
                assert left != null;
                final InputBlock right = graph.getBlock(p.getRight());
                assert right != null;
                graph.addBlockEdge(left, right);
            }
            blockConnections.clear();

            Runnable addToParent = () -> parent.addElement(graph);
            if (invokeLater) {
                SwingUtilities.invokeLater(addToParent);
            } else {
                addToParent.run();
            }
        }
    };
    // <nodes>
    private HandoverElementHandler<InputGraph> nodesHandler = new HandoverElementHandler<>(NODES_ELEMENT);
    // <controlFlow>
    private HandoverElementHandler<InputGraph> controlFlowHandler = new HandoverElementHandler<>(CONTROL_FLOW_ELEMENT);
    // <block>
    private ElementHandler<InputBlock, InputGraph> blockHandler = new ElementHandler<InputBlock, InputGraph>(BLOCK_ELEMENT) {

        @Override
        protected InputBlock start() throws SAXException {
            InputGraph graph = getParentObject();
            String name = readRequiredAttribute(BLOCK_NAME_PROPERTY);
            InputBlock b = graph.addBlock(name);
            for (InputNode n : b.getNodes()) {
                assert graph.getBlock(n).equals(b);
            }
            return b;
        }
    };
    // <nodes>
    private HandoverElementHandler<InputBlock> blockNodesHandler = new HandoverElementHandler<>(NODES_ELEMENT);
    // <node>
    private ElementHandler<InputBlock, InputBlock> blockNodeHandler = new ElementHandler<InputBlock, InputBlock>(NODE_ELEMENT) {

        @Override
        protected InputBlock start() throws SAXException {
            String s = readRequiredAttribute(NODE_ID_PROPERTY);

            int id = 0;
            try {
                id = lookupID(s);
            } catch (NumberFormatException e) {
                throw new SAXException(e);
            }
            getParentObject().addNode(id);
            return getParentObject();
        }
    };
    // <successors>
    private HandoverElementHandler<InputBlock> successorsHandler = new HandoverElementHandler<>(SUCCESSORS_ELEMENT);
    // <successor>
    private ElementHandler<InputBlock, InputBlock> successorHandler = new ElementHandler<InputBlock, InputBlock>(SUCCESSOR_ELEMENT) {

        @Override
        protected InputBlock start() throws SAXException {
            String name = readRequiredAttribute(BLOCK_NAME_PROPERTY);
            blockConnections.add(new Pair<>(getParentObject().getName(), name));
            return getParentObject();
        }
    };
    // <node>
    private ElementHandler<InputNode, InputGraph> nodeHandler = new ElementHandler<InputNode, InputGraph>(NODE_ELEMENT) {

        @Override
        protected InputNode start() throws SAXException {
            String s = readRequiredAttribute(NODE_ID_PROPERTY);
            int id = 0;
            try {
                id = lookupID(s);
            } catch (NumberFormatException e) {
                throw new SAXException(e);
            }
            InputNode node = new InputNode(id);
            getParentObject().addNode(node);
            return node;
        }
    };
    // <removeNode>
    private ElementHandler<InputNode, InputGraph> removeNodeHandler = new ElementHandler<InputNode, InputGraph>(REMOVE_NODE_ELEMENT) {

        @Override
        protected InputNode start() throws SAXException {
            String s = readRequiredAttribute(NODE_ID_PROPERTY);
            int id = 0;
            try {
                id = lookupID(s);
            } catch (NumberFormatException e) {
                throw new SAXException(e);
            }
            return getParentObject().removeNode(id);
        }
    };
    // <graph>
    private HandoverElementHandler<InputGraph> edgesHandler = new HandoverElementHandler<>(EDGES_ELEMENT);

    // Local class for edge elements
    private class EdgeElementHandler extends ElementHandler<InputEdge, InputGraph> {

        public EdgeElementHandler(String name) {
            super(name);
        }

        @Override
        protected InputEdge start() throws SAXException {
            int fromIndex = 0;
            int toIndex = 0;
            int from = -1;
            int to = -1;
            String label = null;
            String type = null;

            try {
                String fromIndexString = readAttribute(FROM_INDEX_PROPERTY);
                if (fromIndexString != null) {
                    fromIndex = Integer.parseInt(fromIndexString);
                }

                String toIndexString = readAttribute(TO_INDEX_PROPERTY);
                if (toIndexString == null) {
                    toIndexString = readAttribute(TO_INDEX_ALT_PROPERTY);
                }
                if (toIndexString != null) {
                    toIndex = Integer.parseInt(toIndexString);
                }

                label = readAttribute(LABEL_PROPERTY);
                type = readAttribute(TYPE_PROPERTY);

                from = lookupID(readRequiredAttribute(FROM_PROPERTY));
                to = lookupID(readRequiredAttribute(TO_PROPERTY));
            } catch (NumberFormatException e) {
                throw new SAXException(e);
            }

            InputEdge conn = new InputEdge((char) fromIndex, (char) toIndex, from, to, label, type == null ? "" : type);
            return start(conn);
        }

        protected InputEdge start(InputEdge conn) throws SAXException {
            return conn;
        }
    }
    // <edge>
    private EdgeElementHandler edgeHandler = new EdgeElementHandler(EDGE_ELEMENT) {

        @Override
        protected InputEdge start(InputEdge conn) throws SAXException {
            getParentObject().addEdge(conn);
            return conn;
        }
    };
    // <removeEdge>
    private EdgeElementHandler removeEdgeHandler = new EdgeElementHandler(REMOVE_EDGE_ELEMENT) {

        @Override
        protected InputEdge start(InputEdge conn) throws SAXException {
            getParentObject().removeEdge(conn);
            return conn;
        }
    };
    // <properties>
    private HandoverElementHandler<Properties.Provider> propertiesHandler = new HandoverElementHandler<>(PROPERTIES_ELEMENT);
    // <properties>
    private HandoverElementHandler<Group> groupPropertiesHandler = new HandoverElementHandler<Group>(PROPERTIES_ELEMENT) {

        @Override
        public void end(String text) throws SAXException {
            if (groupCallback != null && getParentObject().getParent() instanceof GraphDocument) {
                final Group group = getParentObject();
                Runnable addStarted = () -> groupCallback.started(group);
                if (invokeLater) {
                    SwingUtilities.invokeLater(addStarted);
                } else {
                    addStarted.run();
                }
            }
        }
    };
    // <property>
    private ElementHandler<String, Properties.Provider> propertyHandler = new XMLParser.ElementHandler<String, Properties.Provider>(PROPERTY_ELEMENT, true) {

        @Override
        public String start() throws SAXException {
            return readRequiredAttribute(PROPERTY_NAME_PROPERTY);
         }

        @Override
        public void end(String text) {
            getParentObject().getProperties().setProperty(getObject(), text.trim());
        }
    };

    public Parser(ReadableByteChannel channel) {
        this(channel, null, null);
    }

    public Parser(ReadableByteChannel channel, ParseMonitor monitor, GroupCallback groupCallback) {

        this.groupCallback = groupCallback;
        this.monitor = monitor;
        this.channel = channel;

        // Initialize dependencies
        xmlDocument.addChild(topHandler);
        topHandler.addChild(groupHandler);

        groupHandler.addChild(methodHandler);
        groupHandler.addChild(graphHandler);
        groupHandler.addChild(groupHandler);

        methodHandler.addChild(inlinedHandler);
        methodHandler.addChild(bytecodesHandler);

        inlinedHandler.addChild(inlinedMethodHandler);
        inlinedMethodHandler.addChild(bytecodesHandler);
        inlinedMethodHandler.addChild(inlinedHandler);

        graphHandler.addChild(nodesHandler);
        graphHandler.addChild(edgesHandler);
        graphHandler.addChild(controlFlowHandler);

        controlFlowHandler.addChild(blockHandler);

        blockHandler.addChild(successorsHandler);
        successorsHandler.addChild(successorHandler);
        blockHandler.addChild(blockNodesHandler);
        blockNodesHandler.addChild(blockNodeHandler);

        nodesHandler.addChild(nodeHandler);
        nodesHandler.addChild(removeNodeHandler);
        edgesHandler.addChild(edgeHandler);
        edgesHandler.addChild(removeEdgeHandler);

        methodHandler.addChild(propertiesHandler);
        inlinedMethodHandler.addChild(propertiesHandler);
        topHandler.addChild(propertiesHandler);
        groupHandler.addChild(groupPropertiesHandler);
        graphHandler.addChild(propertiesHandler);
        nodeHandler.addChild(propertiesHandler);
        propertiesHandler.addChild(propertyHandler);
        groupPropertiesHandler.addChild(propertyHandler);
    }

    // Returns a new GraphDocument object deserialized from an XML input source.
    @Override
    public GraphDocument parse() throws IOException {
        if (monitor != null) {
            monitor.setState("Starting parsing");
        }
        try {
            XMLReader reader = createReader();
            // To enforce using English for non-English users, we must use Locale.ROOT rather than Locale.ENGLISH
            reader.setProperty("http://apache.org/xml/properties/locale", Locale.ROOT);
            reader.setContentHandler(new XMLParser(xmlDocument, monitor));
            reader.parse(new InputSource(Channels.newInputStream(channel)));
        } catch (SAXException ex) {
            if (!(ex instanceof SAXParseException) || !"XML document structures must start and end within the same entity.".equals(ex.getMessage())) {
                throw new IOException(ex);
            }
        }
        if (monitor != null) {
            monitor.setState("Finished parsing");
        }
        return graphDocument;
    }

    // Whether the parser is allowed to defer connecting the parsed elements.
    // Setting to false is useful for synchronization in unit tests.
    public void setInvokeLater(boolean invokeLater) {
        this.invokeLater = invokeLater;
    }

    private XMLReader createReader() throws SAXException {
        try {
            SAXParserFactory pfactory = SAXParserFactory.newInstance();
            pfactory.setValidating(false);
            pfactory.setNamespaceAware(true);
            return pfactory.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException ex) {
            throw new SAXException(ex);
        }
    }
}
