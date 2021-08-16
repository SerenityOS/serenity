/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
package test.gaptest;

import static jaxp.library.JAXPTestUtilities.filenameToURL;
import static org.testng.Assert.assertEquals;
import static test.gaptest.GapTestConst.GOLDEN_DIR;
import static test.gaptest.GapTestConst.XML_DIR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
 * @test
 * @bug 4858685 4894410
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.gaptest.Bug4858685
 * @run testng/othervm test.gaptest.Bug4858685
 * @summary test transforming text node
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4858685 {
    @Test
    public void test() throws TransformerException, IOException {
        String uri = XML_DIR + "certificate.xml";
        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        Transformer transformer = transformerFactory.newTransformer();

        // use URI as a StreamSource
        StreamSource streamSource = new StreamSource(filenameToURL(uri));

        DOMResult domResult = new DOMResult();

        // StreamSource -> DOMResult
        transformer.transform(streamSource, domResult);

        // dump DOM in a human readable form
        String gotString = DOMDump.dumpDom(domResult.getNode());

        String goldenString = new String(Files.readAllBytes(Paths.get(GOLDEN_DIR + "Bug4858685.txt")));

        assertEquals(gotString, goldenString);

    }

    /**
     * DOMDump: dump a DOM to a String in human readable form. method dumpDOM()
     * is static for easy calling:
     */
    private static class DOMDump {

        /**
         * the maximum level to indent with blanks
         */
        private static final int BLANKS_LEN = 64;

        /**
         * each level of the tree will be indented with blanks for readability
         */
        private static final String BLANKS = "                                                              ";

        /**
         * dumpDOM will dump the DOM into a String for human readability
         *
         * @param domNode
         *            the DOM Node to dump
         * @return human readabile DOM as a String
         */
        public static String dumpDom(Node domNode) {
            return dumpInternal(domNode, 0);
        }

        /**
         * dumpInternal is used internaly to recursively dump DOM Nodes
         *
         * @param domNode
         *            to dump
         * @param indent
         *            level
         * @return domNode as human readable String
         */
        private static String dumpInternal(Node domNode, int indent) {

            String result = "";

            // indent for readability
            result += indentBlanks(indent);
            indent += 2;

            // protect against null
            if (domNode == null) {
                result = result + "[null]" + "\n";
                return result;
            }

            // what to output depends on NodeType
            short type = domNode.getNodeType();
            switch (type) {
                case Node.ATTRIBUTE_NODE: {
                    result += "[attribute] " + domNode.getNodeName() + "=\"" + domNode.getNodeValue() + "\"";
                    break;
                }
                case Node.CDATA_SECTION_NODE: {
                    result += "[cdata] " + domNode.getNodeValue();
                    break;
                }
                case Node.COMMENT_NODE: {
                    result += "[comment] " + domNode.getNodeValue();
                    break;
                }
                case Node.DOCUMENT_FRAGMENT_NODE: {
                    result += "[document fragment]";
                    break;
                }
                case Node.DOCUMENT_NODE: {
                    result += "[document]";
                    break;
                }
                case Node.DOCUMENT_TYPE_NODE: {
                    result += "[document type] " + domNode.getNodeName();
                    break;
                }
                case Node.ELEMENT_NODE: {
                    result += "[element] " + domNode.getNodeName();
                    // output all attributes for Element
                    if (domNode.hasAttributes()) {
                        NamedNodeMap attributes = domNode.getAttributes();
                        for (int onAttribute = 0; onAttribute < attributes.getLength(); onAttribute++) {

                            // seprate each attribute with a space
                            result += " ";

                            Node attribute = attributes.item(onAttribute);
                            String namespaceURI = attribute.getNamespaceURI();
                            String prefix = attribute.getPrefix();
                            String localName = attribute.getLocalName();
                            String name = attribute.getNodeName();
                            String value = attribute.getNodeValue();

                            // using Namespaces?
                            if (namespaceURI != null) {
                                result += "{" + namespaceURI + "}";
                            }
                            if (prefix != null) {
                                result += prefix + ":";
                            }

                            // name="value"
                            result += attribute.getNodeName() + "=\"" + attribute.getNodeValue() + "\"";
                        }
                    }

                    break;
                }
                case Node.ENTITY_NODE: {
                    result += "[entity] " + domNode.getNodeName();
                    break;
                }
                case Node.ENTITY_REFERENCE_NODE: {
                    result += "[entity reference] " + domNode.getNodeName();
                    break;
                }
                case Node.NOTATION_NODE: {
                    result += "[notation] " + domNode.getNodeName();
                    break;
                }
                case Node.PROCESSING_INSTRUCTION_NODE: {
                    result += "[pi] target=\"" + domNode.getNodeName() + "\" content=\"" + domNode.getNodeValue() + "\"";
                    break;
                }
                case Node.TEXT_NODE: {
                    result += "[text] " + domNode.getNodeValue();
                    break;
                }
                default: {
                    result += "[unknown]";
                    break;
                }
            }

            // humans read in lines
            result += "\n";

            // process children
            NodeList children = domNode.getChildNodes();
            for (int onChild = 0; onChild < children.getLength(); onChild++) {
                Node child = children.item(onChild);
                result += dumpInternal(child, indent);
            }

            // return human readable DOM as String
            return result;
        }

        /**
         * indentBlanks will return a String of indent blanks
         *
         * @param indent
         *            level
         * @return String of blanks
         */
        private static String indentBlanks(int indent) {
            if (indent == 0) {
                return "";
            }

            if (indent > BLANKS_LEN) {
                return BLANKS;
            }

            return BLANKS.substring(0, indent + 1);
        }

    }
}
