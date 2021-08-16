/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file and, per its terms, should not be removed:
 *
 * Copyright (c) 2004 World Wide Web Consortium,
 *
 * (Massachusetts Institute of Technology, European Research Consortium for
 * Informatics and Mathematics, Keio University). All Rights Reserved. This
 * work is distributed under the W3C(r) Software License [1] in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
 */

package org.w3c.dom.ls;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Node;
import org.w3c.dom.DOMException;

/**
 *  A <code>LSSerializer</code> provides an API for serializing (writing) a
 * DOM document out into XML. The XML data is written to a string or an
 * output stream. Any changes or fixups made during the serialization affect
 * only the serialized data. The <code>Document</code> object and its
 * children are never altered by the serialization operation.
 * <p> During serialization of XML data, namespace fixup is done as defined in
 * [<a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>DOM Level 3 Core</a>]
 * , Appendix B. [<a href='http://www.w3.org/TR/2000/REC-DOM-Level-2-Core-20001113'>DOM Level 2 Core</a>]
 *  allows empty strings as a real namespace URI. If the
 * <code>namespaceURI</code> of a <code>Node</code> is empty string, the
 * serialization will treat them as <code>null</code>, ignoring the prefix
 * if any.
 * <p> <code>LSSerializer</code> accepts any node type for serialization. For
 * nodes of type <code>Document</code> or <code>Entity</code>, well-formed
 * XML will be created when possible (well-formedness is guaranteed if the
 * document or entity comes from a parse operation and is unchanged since it
 * was created). The serialized output for these node types is either as a
 * XML document or an External XML Entity, respectively, and is acceptable
 * input for an XML parser. For all other types of nodes the serialized form
 * is implementation dependent.
 * <p>Within a <code>Document</code>, <code>DocumentFragment</code>, or
 * <code>Entity</code> being serialized, <code>Nodes</code> are processed as
 * follows
 * <ul>
 * <li> <code>Document</code> nodes are written, including the XML
 * declaration (unless the parameter "xml-declaration" is set to
 * <code>false</code>) and a DTD subset, if one exists in the DOM. Writing a
 * <code>Document</code> node serializes the entire document.
 * </li>
 * <li>
 * <code>Entity</code> nodes, when written directly by
 * <code>LSSerializer.write</code>, outputs the entity expansion but no
 * namespace fixup is done. The resulting output will be valid as an
 * external entity.
 * </li>
 * <li> If the parameter
 * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-entities'>entities</a>"
 * is set to <code>true</code>, <code>EntityReference</code> nodes are
 * serialized as an entity reference of the form "
 * <code>&amp;entityName;</code>" in the output. Child nodes (the expansion)
 * of the entity reference are ignored. If the parameter
 * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-entities'>entities</a>"
 * is set to <code>false</code>, only the children of the entity reference
 * are serialized. <code>EntityReference</code> nodes with no children (no
 * corresponding <code>Entity</code> node or the corresponding
 * <code>Entity</code> nodes have no children) are always serialized.
 * </li>
 * <li>
 * <code>CDATAsections</code> containing content characters that cannot be
 * represented in the specified output encoding are handled according to the
 * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-split-cdata-sections'>split-cdata-sections</a>"
 * parameter.  If the parameter is set to <code>true</code>,
 * <code>CDATAsections</code> are split, and the unrepresentable characters
 * are serialized as numeric character references in ordinary content. The
 * exact position and number of splits is not specified.  If the parameter
 * is set to <code>false</code>, unrepresentable characters in a
 * <code>CDATAsection</code> are reported as
 * <code>"wf-invalid-character"</code> errors if the parameter
 * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-well-formed'>well-formed</a>"
 * is set to <code>true</code>. The error is not recoverable - there is no
 * mechanism for supplying alternative characters and continuing with the
 * serialization.
 * </li>
 * <li> <code>DocumentFragment</code> nodes are serialized by
 * serializing the children of the document fragment in the order they
 * appear in the document fragment.
 * </li>
 * <li> All other node types (Element, Text,
 * etc.) are serialized to their corresponding XML source form.
 * </li>
 * </ul>
 * <p ><b>Note:</b>  The serialization of a <code>Node</code> does not always
 * generate a well-formed XML document, i.e. a <code>LSParser</code> might
 * throw fatal errors when parsing the resulting serialization.
 * <p> Within the character data of a document (outside of markup), any
 * characters that cannot be represented directly are replaced with
 * character references. Occurrences of '&lt;' and '&amp;' are replaced by
 * the predefined entities &amp;lt; and &amp;amp;. The other predefined
 * entities (&amp;gt;, &amp;apos;, and &amp;quot;) might not be used, except
 * where needed (e.g. using &amp;gt; in cases such as ']]&gt;'). Any
 * characters that cannot be represented directly in the output character
 * encoding are serialized as numeric character references (and since
 * character encoding standards commonly use hexadecimal representations of
 * characters, using the hexadecimal representation when serializing
 * character references is encouraged).
 * <p> To allow attribute values to contain both single and double quotes, the
 * apostrophe or single-quote character (') may be represented as
 * "&amp;apos;", and the double-quote character (")  as "&amp;quot;". New
 * line characters and other characters that cannot be represented directly
 * in attribute values in the output character encoding are serialized as a
 * numeric character reference.
 * <p> Within markup, but outside of attributes, any occurrence of a character
 * that cannot be represented in the output character encoding is reported
 * as a <code>DOMError</code> fatal error. An example would be serializing
 * the element &lt;LaCa&ntilde;ada/&gt; with <code>encoding="us-ascii"</code>.
 * This will result with a generation of a <code>DOMError</code>
 * "wf-invalid-character-in-node-name" (as proposed in
 * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-well-formed'>well-formed</a>").
 * <p> When requested by setting the parameter
 * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-normalize-characters'>normalize-characters</a>"
 * on <code>LSSerializer</code> to true, character normalization is
 * performed according to the definition of
 * <a href='http://www.w3.org/TR/2004/REC-xml11-20040204/#dt-fullnorm'>fully
 * normalized</a> characters included in appendix E of
 * [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>] on all
 * data to be serialized, both markup and character data. The character
 * normalization process affects only the data as it is being written; it
 * does not alter the DOM's view of the document after serialization has
 * completed.
 * <p> Implementations are required to support the encodings "UTF-8",
 * "UTF-16", "UTF-16BE", and "UTF-16LE" to guarantee that data is
 * serializable in all encodings that are required to be supported by all
 * XML parsers. When the encoding is UTF-8, whether or not a byte order mark
 * is serialized, or if the output is big-endian or little-endian, is
 * implementation dependent. When the encoding is UTF-16, whether or not the
 * output is big-endian or little-endian is implementation dependent, but a
 * Byte Order Mark must be generated for non-character outputs, such as
 * <code>LSOutput.byteStream</code> or <code>LSOutput.systemId</code>. If
 * the Byte Order Mark is not generated, a "byte-order-mark-needed" warning
 * is reported. When the encoding is UTF-16LE or UTF-16BE, the output is
 * big-endian (UTF-16BE) or little-endian (UTF-16LE) and the Byte Order Mark
 * is not be generated. In all cases, the encoding declaration, if
 * generated, will correspond to the encoding used during the serialization
 * (e.g. <code>encoding="UTF-16"</code> will appear if UTF-16 was
 * requested).
 * <p> Namespaces are fixed up during serialization, the serialization process
 * will verify that namespace declarations, namespace prefixes and the
 * namespace URI associated with elements and attributes are consistent. If
 * inconsistencies are found, the serialized form of the document will be
 * altered to remove them. The method used for doing the namespace fixup
 * while serializing a document is the algorithm defined in Appendix B.1,
 * "Namespace normalization", of
 * [<a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>DOM Level 3 Core</a>]
 * .
 * <p> While serializing a document, the parameter "discard-default-content"
 * controls whether or not non-specified data is serialized.
 * <p> While serializing, errors and warnings are reported to the application
 * through the error handler (<code>LSSerializer.domConfig</code>'s
 * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-error-handler'>error-handler</a>"
 * parameter). This specification does in no way try to define all possible
 * errors and warnings that can occur while serializing a DOM node, but some
 * common error and warning cases are defined. The types (
 * <code>DOMError.type</code>) of errors and warnings defined by this
 * specification are:
 * <dl>
 * <dt><code>"no-output-specified" [fatal]</code></dt>
 * <dd> Raised when
 * writing to a <code>LSOutput</code> if no output is specified in the
 * <code>LSOutput</code>. </dd>
 * <dt>
 * <code>"unbound-prefix-in-entity-reference" [fatal]</code> </dt>
 * <dd> Raised if the
 * configuration parameter
 * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-namespaces'>namespaces</a>"
 * is set to <code>true</code> and an entity whose replacement text
 * contains unbound namespace prefixes is referenced in a location where
 * there are no bindings for the namespace prefixes. </dd>
 * <dt>
 * <code>"unsupported-encoding" [fatal]</code></dt>
 * <dd> Raised if an unsupported
 * encoding is encountered. </dd>
 * </dl>
 * <p> In addition to raising the defined errors and warnings, implementations
 * are expected to raise implementation specific errors and warnings for any
 * other error and warning cases such as IO errors (file not found,
 * permission denied,...) and so on.
 * <p>See also the
 * <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-LS-20040407'>
Document Object Model (DOM) Level 3 Load and Save Specification</a>.
 *
 * @since 1.5
 */
public interface LSSerializer {
    /**
     *  The <code>DOMConfiguration</code> object used by the
     * <code>LSSerializer</code> when serializing a DOM node.
     * <br> In addition to the parameters recognized by the
     * <a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#DOMConfiguration'>DOMConfiguration</a>
     * interface defined in
     * [<a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>DOM Level 3 Core</a>]
     * , the <code>DOMConfiguration</code> objects for
     * <code>LSSerializer</code> adds, or modifies, the following
     * parameters:
     * <dl>
     * <dt><code>"canonical-form"</code></dt>
     * <dd>
     * <dl>
     * <dt><code>true</code></dt>
     * <dd>[<em>optional</em>] Writes the document according to the rules specified in
     * [<a href='http://www.w3.org/TR/2001/REC-xml-c14n-20010315'>Canonical XML</a>].
     * In addition to the behavior described in
     * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-canonical-form'>canonical-form</a>"
     * [<a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>DOM Level 3 Core</a>]
     * , setting this parameter to <code>true</code> will set the parameters
     * "format-pretty-print", "discard-default-content", and "xml-declaration
     * ", to <code>false</code>. Setting one of those parameters to
     * <code>true</code> will set this parameter to <code>false</code>.
     * Serializing an XML 1.1 document when "canonical-form" is
     * <code>true</code> will generate a fatal error. </dd>
     * <dt><code>false</code></dt>
     * <dd>[<em>required</em>] (<em>default</em>) Do not canonicalize the output. </dd>
     * </dl></dd>
     * <dt><code>"discard-default-content"</code></dt>
     * <dd>
     * <dl>
     * <dt>
     * <code>true</code></dt>
     * <dd>[<em>required</em>] (<em>default</em>) Use the <code>Attr.specified</code> attribute to decide what attributes
     * should be discarded. Note that some implementations might use
     * whatever information available to the implementation (i.e. XML
     * schema, DTD, the <code>Attr.specified</code> attribute, and so on) to
     * determine what attributes and content to discard if this parameter is
     * set to <code>true</code>. </dd>
     * <dt><code>false</code></dt>
     * <dd>[<em>required</em>]Keep all attributes and all content.</dd>
     * </dl></dd>
     * <dt><code>"format-pretty-print"</code></dt>
     * <dd>
     * <dl>
     * <dt>
     * <code>true</code></dt>
     * <dd>[<em>optional</em>] Formatting the output by adding whitespace to produce a pretty-printed,
     * indented, human-readable form. The exact form of the transformations
     * is not specified by this specification. Pretty-printing changes the
     * content of the document and may affect the validity of the document,
     * validating implementations should preserve validity. </dd>
     * <dt>
     * <code>false</code></dt>
     * <dd>[<em>required</em>] (<em>default</em>) Don't pretty-print the result. </dd>
     * </dl></dd>
     * <dt>
     * <code>"ignore-unknown-character-denormalizations"</code> </dt>
     * <dd>
     * <dl>
     * <dt>
     * <code>true</code></dt>
     * <dd>[<em>required</em>] (<em>default</em>) If, while verifying full normalization when
     * [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>] is
     * supported, a character is encountered for which the normalization
     * properties cannot be determined, then raise a
     * <code>"unknown-character-denormalization"</code> warning (instead of
     * raising an error, if this parameter is not set) and ignore any
     * possible denormalizations caused by these characters. </dd>
     * <dt>
     * <code>false</code></dt>
     * <dd>[<em>optional</em>] Report a fatal error if a character is encountered for which the
     * processor cannot determine the normalization properties. </dd>
     * </dl></dd>
     * <dt>
     * <code>"normalize-characters"</code></dt>
     * <dd> This parameter is equivalent to
     * the one defined by <code>DOMConfiguration</code> in
     * [<a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>DOM Level 3 Core</a>]
     * . Unlike in the Core, the default value for this parameter is
     * <code>true</code>. While DOM implementations are not required to
     * support <a href='http://www.w3.org/TR/2004/REC-xml11-20040204/#dt-fullnorm'>fully
     * normalizing</a> the characters in the document according to appendix E of
     * [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>], this
     * parameter must be activated by default if supported. </dd>
     * <dt>
     * <code>"xml-declaration"</code></dt>
     * <dd>
     * <dl>
     * <dt><code>true</code></dt>
     * <dd>[<em>required</em>] (<em>default</em>) If a <code>Document</code>,
     * <code>Element</code>, or <code>Entity</code>
     *  node is serialized, the XML declaration, or text declaration, should
     * be included. The version (<code>Document.xmlVersion</code> if the
     * document is a Level 3 document and the version is non-null, otherwise
     * use the value "1.0"), and the output encoding (see
     * <code>LSSerializer.write</code> for details on how to find the output
     * encoding) are specified in the serialized XML declaration. </dd>
     * <dt>
     * <code>false</code></dt>
     * <dd>[<em>required</em>] Do not serialize the XML and text declarations. Report a
     * <code>"xml-declaration-needed"</code> warning if this will cause
     * problems (i.e. the serialized data is of an XML version other than
     * [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>], or an
     * encoding would be needed to be able to re-parse the serialized data). </dd>
     * </dl></dd>
     * </dl>
     */
    public DOMConfiguration getDomConfig();

    /**
     *  The end-of-line sequence of characters to be used in the XML being
     * written out. Any string is supported, but XML treats only a certain
     * set of characters sequence as end-of-line (See section 2.11,
     * "End-of-Line Handling" in [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>],
     * if the serialized content is XML 1.0 or section 2.11, "End-of-Line Handling"
     * in [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>], if the
     * serialized content is XML 1.1). Using other character sequences than
     * the recommended ones can result in a document that is either not
     * serializable or not well-formed).
     * <br> On retrieval, the default value of this attribute is the
     * implementation specific default end-of-line sequence. DOM
     * implementations should choose the default to match the usual
     * convention for text files in the environment being used.
     * Implementations must choose a default sequence that matches one of
     * those allowed by XML 1.0 or XML 1.1, depending on the serialized
     * content. Setting this attribute to <code>null</code> will reset its
     * value to the default value.
     * <br>
     */
    public String getNewLine();
    /**
     *  The end-of-line sequence of characters to be used in the XML being
     * written out. Any string is supported, but XML treats only a certain
     * set of characters sequence as end-of-line (See section 2.11,
     * "End-of-Line Handling" in [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>],
     * if the serialized content is XML 1.0 or section 2.11, "End-of-Line Handling"
     * in [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>], if the
     * serialized content is XML 1.1). Using other character sequences than
     * the recommended ones can result in a document that is either not
     * serializable or not well-formed).
     * <br> On retrieval, the default value of this attribute is the
     * implementation specific default end-of-line sequence. DOM
     * implementations should choose the default to match the usual
     * convention for text files in the environment being used.
     * Implementations must choose a default sequence that matches one of
     * those allowed by XML 1.0 or XML 1.1, depending on the serialized
     * content. Setting this attribute to <code>null</code> will reset its
     * value to the default value.
     * <br>
     */
    public void setNewLine(String newLine);

    /**
     *  When the application provides a filter, the serializer will call out
     * to the filter before serializing each Node. The filter implementation
     * can choose to remove the node from the stream or to terminate the
     * serialization early.
     * <br> The filter is invoked after the operations requested by the
     * <code>DOMConfiguration</code> parameters have been applied. For
     * example, CDATA sections won't be passed to the filter if
     * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-cdata-sections'>cdata-sections</a>"
     * is set to <code>false</code>.
     */
    public LSSerializerFilter getFilter();
    /**
     *  When the application provides a filter, the serializer will call out
     * to the filter before serializing each Node. The filter implementation
     * can choose to remove the node from the stream or to terminate the
     * serialization early.
     * <br> The filter is invoked after the operations requested by the
     * <code>DOMConfiguration</code> parameters have been applied. For
     * example, CDATA sections won't be passed to the filter if
     * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-cdata-sections'>cdata-sections</a>"
     * is set to <code>false</code>.
     */
    public void setFilter(LSSerializerFilter filter);

    /**
     *  Serialize the specified node as described above in the general
     * description of the <code>LSSerializer</code> interface. The output is
     * written to the supplied <code>LSOutput</code>.
     * <br> When writing to a <code>LSOutput</code>, the encoding is found by
     * looking at the encoding information that is reachable through the
     * <code>LSOutput</code> and the item to be written (or its owner
     * document) in this order:
     * <ol>
     * <li> <code>LSOutput.encoding</code>,
     * </li>
     * <li>
     * <code>Document.inputEncoding</code>,
     * </li>
     * <li>
     * <code>Document.xmlEncoding</code>.
     * </li>
     * </ol>
     * <br> If no encoding is reachable through the above properties, a
     * default encoding of "UTF-8" will be used. If the specified encoding
     * is not supported an "unsupported-encoding" fatal error is raised.
     * <br> If no output is specified in the <code>LSOutput</code>, a
     * "no-output-specified" fatal error is raised.
     * <br> The implementation is responsible of associating the appropriate
     * media type with the serialized data.
     * <br> When writing to a HTTP URI, a HTTP PUT is performed. When writing
     * to other types of URIs, the mechanism for writing the data to the URI
     * is implementation dependent.
     * @param nodeArg  The node to serialize.
     * @param destination The destination for the serialized DOM.
     * @return  Returns <code>true</code> if <code>node</code> was
     *   successfully serialized. Return <code>false</code> in case the
     *   normal processing stopped but the implementation kept serializing
     *   the document; the result of the serialization being implementation
     *   dependent then.
     * @exception LSException
     *    SERIALIZE_ERR: Raised if the <code>LSSerializer</code> was unable to
     *   serialize the node. DOM applications should attach a
     *   <code>DOMErrorHandler</code> using the parameter
     * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-error-handler'>error-handler</a>"
     * if they wish to get details on the error.
     */
    public boolean write(Node nodeArg,
                         LSOutput destination)
                         throws LSException;

    /**
     *  A convenience method that acts as if <code>LSSerializer.write</code>
     * was called with a <code>LSOutput</code> with no encoding specified
     * and <code>LSOutput.systemId</code> set to the <code>uri</code>
     * argument.
     * @param nodeArg  The node to serialize.
     * @param uri The URI to write to.
     * @return  Returns <code>true</code> if <code>node</code> was
     *   successfully serialized. Return <code>false</code> in case the
     *   normal processing stopped but the implementation kept serializing
     *   the document; the result of the serialization being implementation
     *   dependent then.
     * @exception LSException
     *    SERIALIZE_ERR: Raised if the <code>LSSerializer</code> was unable to
     *   serialize the node. DOM applications should attach a
     *   <code>DOMErrorHandler</code> using the parameter
     * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-error-handler'>error-handler</a>"
     * if they wish to get details on the error.
     */
    public boolean writeToURI(Node nodeArg,
                              String uri)
                              throws LSException;

    /**
     *  Serialize the specified node as described above in the general
     * description of the <code>LSSerializer</code> interface. The output is
     * written to a <code>DOMString</code> that is returned to the caller.
     * The encoding used is the encoding of the <code>DOMString</code> type,
     * i.e. UTF-16. Note that no Byte Order Mark is generated in a
     * <code>DOMString</code> object.
     * @param nodeArg  The node to serialize.
     * @return  Returns the serialized data.
     * @exception DOMException
     *    DOMSTRING_SIZE_ERR: Raised if the resulting string is too long to
     *   fit in a <code>DOMString</code>.
     * @exception LSException
     *    SERIALIZE_ERR: Raised if the <code>LSSerializer</code> was unable to
     *   serialize the node. DOM applications should attach a
     *   <code>DOMErrorHandler</code> using the parameter
     * "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-error-handler'>error-handler</a>"
     * if they wish to get details on the error.
     */
    public String writeToString(Node nodeArg)
                                throws DOMException, LSException;

}
