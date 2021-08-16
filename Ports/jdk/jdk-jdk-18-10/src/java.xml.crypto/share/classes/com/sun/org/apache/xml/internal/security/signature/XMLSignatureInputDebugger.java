/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.signature;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Set;

import com.sun.org.apache.xml.internal.security.c14n.helper.AttrCompare;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

/**
 * Class XMLSignatureInputDebugger
 */
public class XMLSignatureInputDebugger {

    /** Field _xmlSignatureInput */
    private Set<Node> xpathNodeSet;

    private Set<String> inclusiveNamespaces;

    /** Field writer */
    private Writer writer;

    /** The HTML Prefix* */
    static final String HTMLPrefix =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n"
        + "<html>\n"
        + "<head>\n"
        + "<title>Canonical XML node set</title>\n"
        + "<style type=\"text/css\">\n"
        + "<!-- \n"
        + ".INCLUDED { \n"
        + "   color: #000000; \n"
        + "   background-color: \n"
        + "   #FFFFFF; \n"
        + "   font-weight: bold; } \n"
        + ".EXCLUDED { \n"
        + "   color: #666666; \n"
        + "   background-color: \n"
        + "   #999999; } \n"
        + ".INCLUDEDINCLUSIVENAMESPACE { \n"
        + "   color: #0000FF; \n"
        + "   background-color: #FFFFFF; \n"
        + "   font-weight: bold; \n"
        + "   font-style: italic; } \n"
        + ".EXCLUDEDINCLUSIVENAMESPACE { \n"
        + "   color: #0000FF; \n"
        + "   background-color: #999999; \n"
        + "   font-style: italic; } \n"
        + "--> \n"
        + "</style> \n"
        + "</head>\n"
        + "<body bgcolor=\"#999999\">\n"
        + "<h1>Explanation of the output</h1>\n"
        + "<p>The following text contains the nodeset of the given Reference before it is canonicalized. There exist four different styles to indicate how a given node is treated.</p>\n"
        + "<ul>\n"
        + "<li class=\"INCLUDED\">A node which is in the node set is labeled using the INCLUDED style.</li>\n"
        + "<li class=\"EXCLUDED\">A node which is <em>NOT</em> in the node set is labeled EXCLUDED style.</li>\n"
        + "<li class=\"INCLUDEDINCLUSIVENAMESPACE\">A namespace which is in the node set AND in the InclusiveNamespaces PrefixList is labeled using the INCLUDEDINCLUSIVENAMESPACE style.</li>\n"
        + "<li class=\"EXCLUDEDINCLUSIVENAMESPACE\">A namespace which is in NOT the node set AND in the InclusiveNamespaces PrefixList is labeled using the INCLUDEDINCLUSIVENAMESPACE style.</li>\n"
        + "</ul>\n" + "<h1>Output</h1>\n" + "<pre>\n";

    /** HTML Suffix * */
    static final String HTMLSuffix = "</pre></body></html>";

    static final String HTMLExcludePrefix = "<span class=\"EXCLUDED\">";

    static final String HTMLIncludePrefix = "<span class=\"INCLUDED\">";

    static final String HTMLIncludeOrExcludeSuffix = "</span>";

    static final String HTMLIncludedInclusiveNamespacePrefix = "<span class=\"INCLUDEDINCLUSIVENAMESPACE\">";

    static final String HTMLExcludedInclusiveNamespacePrefix = "<span class=\"EXCLUDEDINCLUSIVENAMESPACE\">";

    private static final int NODE_BEFORE_DOCUMENT_ELEMENT = -1;

    private static final int NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT = 0;

    private static final int NODE_AFTER_DOCUMENT_ELEMENT = 1;

    static final AttrCompare ATTR_COMPARE = new AttrCompare();

    /**
     * Constructor XMLSignatureInputDebugger
     *
     * @param xmlSignatureInput the signature to pretty print
     */
    public XMLSignatureInputDebugger(XMLSignatureInput xmlSignatureInput) {
        if (!xmlSignatureInput.isNodeSet()) {
            this.xpathNodeSet = null;
        } else {
            this.xpathNodeSet = xmlSignatureInput.getInputNodeSet();
        }
    }

    /**
     * Constructor XMLSignatureInputDebugger
     *
     * @param xmlSignatureInput the signatur to pretty print
     * @param inclusiveNamespace
     */
    public XMLSignatureInputDebugger(
        XMLSignatureInput xmlSignatureInput,
        Set<String> inclusiveNamespace
    ) {
        this(xmlSignatureInput);
        this.inclusiveNamespaces = inclusiveNamespace;
    }

    /**
     * Method getHTMLRepresentation
     *
     * @return The HTML Representation.
     * @throws XMLSignatureException
     */
    public String getHTMLRepresentation() throws XMLSignatureException {
        if (this.xpathNodeSet == null || this.xpathNodeSet.isEmpty()) {
            return HTMLPrefix + "<blink>no node set, sorry</blink>" + HTMLSuffix;
        }

        // get only a single node as anchor to fetch the owner document
        Node n = this.xpathNodeSet.iterator().next();

        Document doc = XMLUtils.getOwnerDocument(n);

        try {
            this.writer = new StringWriter();

            this.canonicalizeXPathNodeSet(doc);
            this.writer.close();

            return this.writer.toString();
        } catch (IOException ex) {
            throw new XMLSignatureException(ex);
        } finally {
            this.xpathNodeSet = null;
            this.writer = null;
        }
    }

    /**
     * Method canonicalizeXPathNodeSet
     *
     * @param currentNode
     * @throws XMLSignatureException
     * @throws IOException
     */
    private void canonicalizeXPathNodeSet(Node currentNode)
        throws XMLSignatureException, IOException {

        int currentNodeType = currentNode.getNodeType();
        switch (currentNodeType) {


        case Node.ENTITY_NODE:
        case Node.NOTATION_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
        case Node.ATTRIBUTE_NODE:
            throw new XMLSignatureException("empty", new Object[]{"An incorrect node was provided for c14n: " + currentNodeType});
        case Node.DOCUMENT_NODE:
            this.writer.write(HTMLPrefix);

            for (Node currentChild = currentNode.getFirstChild();
                currentChild != null; currentChild = currentChild.getNextSibling()) {
                this.canonicalizeXPathNodeSet(currentChild);
            }

            this.writer.write(HTMLSuffix);
            break;

        case Node.COMMENT_NODE:
            if (this.xpathNodeSet.contains(currentNode)) {
                this.writer.write(HTMLIncludePrefix);
            } else {
                this.writer.write(HTMLExcludePrefix);
            }

            int position = getPositionRelativeToDocumentElement(currentNode);

            if (position == NODE_AFTER_DOCUMENT_ELEMENT) {
                this.writer.write("\n");
            }

            this.outputCommentToWriter((Comment) currentNode);

            if (position == NODE_BEFORE_DOCUMENT_ELEMENT) {
                this.writer.write("\n");
            }

            this.writer.write(HTMLIncludeOrExcludeSuffix);
            break;

        case Node.PROCESSING_INSTRUCTION_NODE:
            if (this.xpathNodeSet.contains(currentNode)) {
                this.writer.write(HTMLIncludePrefix);
            } else {
                this.writer.write(HTMLExcludePrefix);
            }

            position = getPositionRelativeToDocumentElement(currentNode);

            if (position == NODE_AFTER_DOCUMENT_ELEMENT) {
                this.writer.write("\n");
            }

            this.outputPItoWriter((ProcessingInstruction) currentNode);

            if (position == NODE_BEFORE_DOCUMENT_ELEMENT) {
                this.writer.write("\n");
            }

            this.writer.write(HTMLIncludeOrExcludeSuffix);
            break;

        case Node.TEXT_NODE:
        case Node.CDATA_SECTION_NODE:
            if (this.xpathNodeSet.contains(currentNode)) {
                this.writer.write(HTMLIncludePrefix);
            } else {
                this.writer.write(HTMLExcludePrefix);
            }

            outputTextToWriter(currentNode.getNodeValue());

            for (Node nextSibling = currentNode.getNextSibling();
                nextSibling != null
                && (nextSibling.getNodeType() == Node.TEXT_NODE
                    || nextSibling.getNodeType() == Node.CDATA_SECTION_NODE);
                nextSibling = nextSibling.getNextSibling()) {
                /*
                 * The XPath data model allows to select only the first of a
                 * sequence of mixed text and CDATA nodes. But we must output
                 * them all, so we must search:
                 *
                 * @see http://nagoya.apache.org/bugzilla/show_bug.cgi?id=6329
                 */
                this.outputTextToWriter(nextSibling.getNodeValue());
            }

            this.writer.write(HTMLIncludeOrExcludeSuffix);
            break;

        case Node.ELEMENT_NODE:
            Element currentElement = (Element) currentNode;

            if (this.xpathNodeSet.contains(currentNode)) {
                this.writer.write(HTMLIncludePrefix);
            } else {
                this.writer.write(HTMLExcludePrefix);
            }

            this.writer.write("&lt;");
            this.writer.write(currentElement.getTagName());

            this.writer.write(HTMLIncludeOrExcludeSuffix);

            // we output all Attrs which are available
            NamedNodeMap attrs = currentElement.getAttributes();
            int attrsLength = attrs.getLength();
            Attr attrs2[] = new Attr[attrsLength];

            for (int i = 0; i < attrsLength; i++) {
                attrs2[i] = (Attr)attrs.item(i);
            }

            Arrays.sort(attrs2, ATTR_COMPARE);
            Object[] attrs3 = attrs2;

            for (int i = 0; i < attrsLength; i++) {
                Attr a = (Attr) attrs3[i];
                boolean included = this.xpathNodeSet.contains(a);
                boolean inclusive = this.inclusiveNamespaces.contains(a.getName());

                if (included) {
                    if (inclusive) {
                        // included and inclusive
                        this.writer.write(HTMLIncludedInclusiveNamespacePrefix);
                    } else {
                        // included and not inclusive
                        this.writer.write(HTMLIncludePrefix);
                    }
                } else {
                    if (inclusive) {
                        // excluded and inclusive
                        this.writer.write(HTMLExcludedInclusiveNamespacePrefix);
                    } else {
                        // excluded and not inclusive
                        this.writer.write(HTMLExcludePrefix);
                    }
                }

                this.outputAttrToWriter(a.getNodeName(), a.getNodeValue());
                this.writer.write(HTMLIncludeOrExcludeSuffix);
            }

            if (this.xpathNodeSet.contains(currentNode)) {
                this.writer.write(HTMLIncludePrefix);
            } else {
                this.writer.write(HTMLExcludePrefix);
            }

            this.writer.write("&gt;");

            this.writer.write(HTMLIncludeOrExcludeSuffix);

            // traversal
            for (Node currentChild = currentNode.getFirstChild();
                currentChild != null;
                currentChild = currentChild.getNextSibling()) {
                this.canonicalizeXPathNodeSet(currentChild);
            }

            if (this.xpathNodeSet.contains(currentNode)) {
                this.writer.write(HTMLIncludePrefix);
            } else {
                this.writer.write(HTMLExcludePrefix);
            }

            this.writer.write("&lt;/");
            this.writer.write(currentElement.getTagName());
            this.writer.write("&gt;");

            this.writer.write(HTMLIncludeOrExcludeSuffix);
            break;

        case Node.DOCUMENT_TYPE_NODE:
        default:
            break;
        }
    }

    /**
     * Checks whether a Comment or ProcessingInstruction is before or after the
     * document element. This is needed for prepending or appending "\n"s.
     *
     * @param currentNode
     *            comment or pi to check
     * @return NODE_BEFORE_DOCUMENT_ELEMENT,
     *         NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT or
     *         NODE_AFTER_DOCUMENT_ELEMENT
     * @see #NODE_BEFORE_DOCUMENT_ELEMENT
     * @see #NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT
     * @see #NODE_AFTER_DOCUMENT_ELEMENT
     */
    private int getPositionRelativeToDocumentElement(Node currentNode) {
        if (currentNode == null) {
            return NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT;
        }

        Document doc = currentNode.getOwnerDocument();

        if (currentNode.getParentNode() != doc) {
            return NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT;
        }

        Element documentElement = doc.getDocumentElement();

        if (documentElement == null) {
            return NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT;
        }

        if (documentElement == currentNode) {
            return NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT;
        }

        for (Node x = currentNode; x != null; x = x.getNextSibling()) {
            if (x == documentElement) {
                return NODE_BEFORE_DOCUMENT_ELEMENT;
            }
        }

        return NODE_AFTER_DOCUMENT_ELEMENT;
    }

    /**
     * Normalizes an {@link Attr}ibute value
     *
     * The string value of the node is modified by replacing
     * <UL>
     * <LI>all ampersands (&) with {@code &amp;amp;}</LI>
     * <LI>all open angle brackets (<) with {@code &amp;lt;}</LI>
     * <LI>all quotation mark characters with {@code &amp;quot;}</LI>
     * <LI>and the whitespace characters {@code #x9}, #xA, and #xD,
     * with character references. The character references are written in
     * uppercase hexadecimal with no leading zeroes (for example, {@code #xD}
     * is represented by the character reference {@code &amp;#xD;})</LI>
     * </UL>
     *
     * @param name
     * @param value
     * @throws IOException
     */
    private void outputAttrToWriter(String name, String value) throws IOException {
        this.writer.write(" ");
        this.writer.write(name);
        this.writer.write("=\"");

        int length = value.length();

        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);

            switch (c) {

            case '&':
                this.writer.write("&amp;amp;");
                break;

            case '<':
                this.writer.write("&amp;lt;");
                break;

            case '"':
                this.writer.write("&amp;quot;");
                break;

            case 0x09: // '\t'
                this.writer.write("&amp;#x9;");
                break;

            case 0x0A: // '\n'
                this.writer.write("&amp;#xA;");
                break;

            case 0x0D: // '\r'
                this.writer.write("&amp;#xD;");
                break;

            default:
                this.writer.write(c);
                break;
            }
        }

        this.writer.write("\"");
    }

    /**
     * Normalizes a {@link org.w3c.dom.Comment} value
     *
     * @param currentPI
     * @throws IOException
     */
    private void outputPItoWriter(ProcessingInstruction currentPI) throws IOException {

        if (currentPI == null) {
            return;
        }

        this.writer.write("&lt;?");

        String target = currentPI.getTarget();
        int length = target.length();

        for (int i = 0; i < length; i++) {
            char c = target.charAt(i);

            switch (c) {

            case 0x0D:
                this.writer.write("&amp;#xD;");
                break;

            case ' ':
                this.writer.write("&middot;");
                break;

            case '\n':
                this.writer.write("&para;\n");
                break;

            default:
                this.writer.write(c);
                break;
            }
        }

        String data = currentPI.getData();

        length = data.length();

        if (length > 0) {
            this.writer.write(" ");

            for (int i = 0; i < length; i++) {
                char c = data.charAt(i);

                if (c == 0x0D) {
                    this.writer.write("&amp;#xD;");
                } else {
                    this.writer.write(c);
                }
            }
        }

        this.writer.write("?&gt;");
    }

    /**
     * Method outputCommentToWriter
     *
     * @param currentComment
     * @throws IOException
     */
    private void outputCommentToWriter(Comment currentComment) throws IOException {

        if (currentComment == null) {
            return;
        }

        this.writer.write("&lt;!--");

        String data = currentComment.getData();
        int length = data.length();

        for (int i = 0; i < length; i++) {
            char c = data.charAt(i);

            switch (c) {

            case 0x0D:
                this.writer.write("&amp;#xD;");
                break;

            case ' ':
                this.writer.write("&middot;");
                break;

            case '\n':
                this.writer.write("&para;\n");
                break;

            default:
                this.writer.write(c);
                break;
            }
        }

        this.writer.write("--&gt;");
    }

    /**
     * Method outputTextToWriter
     *
     * @param text
     * @throws IOException
     */
    private void outputTextToWriter(String text) throws IOException {
        if (text == null) {
            return;
        }

        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            switch (c) {

            case '&':
                this.writer.write("&amp;amp;");
                break;

            case '<':
                this.writer.write("&amp;lt;");
                break;

            case '>':
                this.writer.write("&amp;gt;");
                break;

            case 0xD:
                this.writer.write("&amp;#xD;");
                break;

            case ' ':
                this.writer.write("&middot;");
                break;

            case '\n':
                this.writer.write("&para;\n");
                break;

            default:
                this.writer.write(c);
                break;
            }
        }
    }
}
