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

package org.w3c.dom;

/**
 *  The <code>DOMConfiguration</code> interface represents the configuration
 * of a document and maintains a table of recognized parameters. Using the
 * configuration, it is possible to change
 * <code>Document.normalizeDocument()</code> behavior, such as replacing the
 * <code>CDATASection</code> nodes with <code>Text</code> nodes or
 * specifying the type of the schema that must be used when the validation
 * of the <code>Document</code> is requested. <code>DOMConfiguration</code>
 * objects are also used in [<a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-LS-20040407'>DOM Level 3 Load and Save</a>]
 *  in the <code>DOMParser</code> and <code>DOMSerializer</code> interfaces.
 * <p> The parameter names used by the <code>DOMConfiguration</code> object
 * are defined throughout the DOM Level 3 specifications. Names are
 * case-insensitive. To avoid possible conflicts, as a convention, names
 * referring to parameters defined outside the DOM specification should be
 * made unique. Because parameters are exposed as properties in names
 * are recommended to follow the section 5.16 Identifiers of [Unicode] with the addition of the character '-' (HYPHEN-MINUS) but it is not
 * enforced by the DOM implementation. DOM Level 3 Core Implementations are
 * required to recognize all parameters defined in this specification. Some
 * parameter values may also be required to be supported by the
 * implementation. Refer to the definition of the parameter to know if a
 * value must be supported or not.
 * <p ><b>Note:</b>  Parameters are similar to features and properties used in
 * SAX2 [<a href='http://www.saxproject.org/'>SAX</a>].
 * <p> The following list of parameters defined in the DOM:
 * <dl>
 * <dt>
 * <code>"canonical-form"</code></dt>
 * <dd>
 * <dl>
 * <dt><code>true</code></dt>
 * <dd>[<em>optional</em>] Canonicalize the document according to the rules specified in [<a href='http://www.w3.org/TR/2001/REC-xml-c14n-20010315'>Canonical XML</a>],
 * such as removing the <code>DocumentType</code> node (if any) from the
 * tree, or removing superfluous namespace declarations from each element.
 * Note that this is limited to what can be represented in the DOM; in
 * particular, there is no way to specify the order of the attributes in the
 * DOM. In addition,  Setting this parameter to <code>true</code> will also
 * set the state of the parameters listed below. Later changes to the state
 * of one of those parameters will revert "canonical-form" back to
 * <code>false</code>. Parameters set to <code>false</code>: "entities", "
 * normalize-characters", "cdata-sections". Parameters set to
 * <code>true</code>: "namespaces", "namespace-declarations", "well-formed",
 * "element-content-whitespace". Other parameters are not changed unless
 * explicitly specified in the description of the parameters.</dd>
 * <dt>
 * <code>false</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>)Do not canonicalize the document.</dd>
 * </dl></dd>
 * <dt><code>"cdata-sections"</code></dt>
 * <dd>
 * <dl>
 * <dt>
 * <code>true</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>)Keep <code>CDATASection</code> nodes in the document.</dd>
 * <dt><code>false</code></dt>
 * <dd>[<em>required</em>]Transform <code>CDATASection</code> nodes in the document into
 * <code>Text</code> nodes. The new <code>Text</code> node is then combined
 * with any adjacent <code>Text</code> node.</dd>
 * </dl></dd>
 * <dt>
 * <code>"check-character-normalization"</code></dt>
 * <dd>
 * <dl>
 * <dt><code>true</code></dt>
 * <dd>[<em>optional</em>] Check if the characters in the document are <a href='http://www.w3.org/TR/2004/REC-xml11-20040204/#dt-fullnorm'>fully
 * normalized</a>, as defined in appendix B of [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>]. When a
 * sequence of characters is encountered that fails normalization checking,
 * an error with the <code>DOMError.type</code> equals to
 * "check-character-normalization-failure" is issued. </dd>
 * <dt><code>false</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>)Do not check if characters are normalized.</dd>
 * </dl></dd>
 * <dt><code>"comments"</code></dt>
 * <dd>
 * <dl>
 * <dt>
 * <code>true</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>)Keep <code>Comment</code> nodes in the document.</dd>
 * <dt><code>false</code></dt>
 * <dd>[<em>required</em>]Discard <code>Comment</code> nodes in the document.</dd>
 * </dl></dd>
 * <dt>
 * <code>"datatype-normalization"</code></dt>
 * <dd>
 * <dl>
 * <dt><code>true</code></dt>
 * <dd>[<em>optional</em>] Expose schema normalized values in the tree, such as <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#key-nv'>XML
 * Schema normalized values</a> in the case of XML Schema. Since this parameter requires to have schema
 * information, the "validate" parameter will also be set to
 * <code>true</code>. Having this parameter activated when "validate" is
 * <code>false</code> has no effect and no schema-normalization will happen.
 * <p ><b>Note:</b>  Since the document contains the result of the XML 1.0
 * processing, this parameter does not apply to attribute value
 * normalization as defined in section 3.3.3 of [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>] and is only
 * meant for schema languages other than Document Type Definition (DTD). </dd>
 * <dt>
 * <code>false</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>) Do not perform schema normalization on the tree. </dd>
 * </dl></dd>
 * <dt>
 * <code>"element-content-whitespace"</code></dt>
 * <dd>
 * <dl>
 * <dt><code>true</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>)Keep all whitespaces in the document.</dd>
 * <dt><code>false</code></dt>
 * <dd>[<em>optional</em>] Discard all <code>Text</code> nodes that contain whitespaces in element
 * content, as described in <a href='http://www.w3.org/TR/2004/REC-xml-infoset-20040204#infoitem.character'>
 * [element content whitespace]</a>. The implementation is expected to use the attribute
 * <code>Text.isElementContentWhitespace</code> to determine if a
 * <code>Text</code> node should be discarded or not.</dd>
 * </dl></dd>
 * <dt><code>"entities"</code></dt>
 * <dd>
 * <dl>
 * <dt>
 * <code>true</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>)Keep <code>EntityReference</code> nodes in the document.</dd>
 * <dt>
 * <code>false</code></dt>
 * <dd>[<em>required</em>] Remove all <code>EntityReference</code> nodes from the document,
 * putting the entity expansions directly in their place. <code>Text</code>
 * nodes are normalized, as defined in <code>Node.normalize</code>. Only <a href='http://www.w3.org/TR/2004/REC-xml-infoset-20040204/#infoitem.rse'>
 * unexpanded entity references</a> are kept in the document. </dd>
 * </dl>
 * <p ><b>Note:</b>  This parameter does not affect <code>Entity</code> nodes. </dd>
 * <dt>
 * <code>"error-handler"</code></dt>
 * <dd>[<em>required</em>] Contains a <code>DOMErrorHandler</code> object. If an error is
 * encountered in the document, the implementation will call back the
 * <code>DOMErrorHandler</code> registered using this parameter. The
 * implementation may provide a default <code>DOMErrorHandler</code> object.
 *  When called, <code>DOMError.relatedData</code> will contain the closest
 * node to where the error occurred. If the implementation is unable to
 * determine the node where the error occurs,
 * <code>DOMError.relatedData</code> will contain the <code>Document</code>
 * node. Mutations to the document from within an error handler will result
 * in implementation dependent behavior. </dd>
 * <dt><code>"infoset"</code></dt>
 * <dd>
 * <dl>
 * <dt>
 * <code>true</code></dt>
 * <dd>[<em>required</em>]Keep in the document the information defined in the XML Information Set [<a href='http://www.w3.org/TR/2004/REC-xml-infoset-20040204/'>XML Information Set</a>]
 * .This forces the following parameters to <code>false</code>: "
 * validate-if-schema", "entities", "datatype-normalization", "cdata-sections
 * ".This forces the following parameters to <code>true</code>: "
 * namespace-declarations", "well-formed", "element-content-whitespace", "
 * comments", "namespaces".Other parameters are not changed unless
 * explicitly specified in the description of the parameters. Note that
 * querying this parameter with <code>getParameter</code> returns
 * <code>true</code> only if the individual parameters specified above are
 * appropriately set.</dd>
 * <dt><code>false</code></dt>
 * <dd>Setting <code>infoset</code> to
 * <code>false</code> has no effect.</dd>
 * </dl></dd>
 * <dt><code>"namespaces"</code></dt>
 * <dd>
 * <dl>
 * <dt>
 * <code>true</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>) Perform the namespace processing as defined in . </dd>
 * <dt><code>false</code></dt>
 * <dd>[<em>optional</em>] Do not perform the namespace processing. </dd>
 * </dl></dd>
 * <dt>
 * <code>"namespace-declarations"</code></dt>
 * <dd> This parameter has no effect if the
 * parameter "namespaces" is set to <code>false</code>.
 * <dl>
 * <dt><code>true</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>) Include namespace declaration attributes, specified or defaulted from
 * the schema, in the document. See also the sections "Declaring Namespaces"
 * in [<a href='http://www.w3.org/TR/1999/REC-xml-names-19990114/'>XML Namespaces</a>]
 *  and [<a href='http://www.w3.org/TR/2004/REC-xml-names11-20040204/'>XML Namespaces 1.1</a>]
 * .</dd>
 * <dt><code>false</code></dt>
 * <dd>[<em>required</em>]Discard all namespace declaration attributes. The namespace prefixes (
 * <code>Node.prefix</code>) are retained even if this parameter is set to
 * <code>false</code>.</dd>
 * </dl></dd>
 * <dt><code>"normalize-characters"</code></dt>
 * <dd>
 * <dl>
 * <dt><code>true</code></dt>
 * <dd>[<em>optional</em>] <a href='http://www.w3.org/TR/2004/REC-xml11-20040204/#dt-fullnorm'>Fully
 * normalized</a> the characters in the document as defined in appendix B of [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>]. </dd>
 * <dt>
 * <code>false</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>)Do not perform character normalization.</dd>
 * </dl></dd>
 * <dt><code>"schema-location"</code></dt>
 * <dd>[<em>optional</em>] Represent a <code>DOMString</code> object containing a list of URIs,
 * separated by whitespaces (characters matching the <a href='http://www.w3.org/TR/2004/REC-xml-20040204#NT-S'>nonterminal
 * production S</a> defined in section 2.3 [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>]), that
 * represents the schemas against which validation should occur, i.e. the
 * current schema. The types of schemas referenced in this list must match
 * the type specified with <code>schema-type</code>, otherwise the behavior
 * of an implementation is undefined.  The schemas specified using this
 * property take precedence to the schema information specified in the
 * document itself. For namespace aware schema, if a schema specified using
 * this property and a schema specified in the document instance (i.e. using
 * the <code>schemaLocation</code> attribute) in a schema document (i.e.
 * using schema <code>import</code> mechanisms) share the same
 * <code>targetNamespace</code>, the schema specified by the user using this
 * property will be used. If two schemas specified using this property share
 * the same <code>targetNamespace</code> or have no namespace, the behavior
 * is implementation dependent.  If no location has been provided, this
 * parameter is <code>null</code>.
 * <p ><b>Note:</b>  The <code>"schema-location"</code> parameter is ignored
 * unless the "schema-type" parameter value is set. It is strongly
 * recommended that <code>Document.documentURI</code> will be set so that an
 * implementation can successfully resolve any external entities referenced. </dd>
 * <dt>
 * <code>"schema-type"</code></dt>
 * <dd>[<em>optional</em>] Represent a <code>DOMString</code> object containing an absolute URI
 * and representing the type of the schema language used to validate a
 * document against. Note that no lexical checking is done on the absolute
 * URI.  If this parameter is not set, a default value may be provided by
 * the implementation, based on the schema languages supported and on the
 * schema language used at load time. If no value is provided, this
 * parameter is <code>null</code>.
 * <p ><b>Note:</b>  For XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
 * , applications must use the value
 * <code>"http://www.w3.org/2001/XMLSchema"</code>. For XML DTD [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>],
 * applications must use the value
 * <code>"http://www.w3.org/TR/REC-xml"</code>. Other schema languages are
 * outside the scope of the W3C and therefore should recommend an absolute
 * URI in order to use this method. </dd>
 * <dt><code>"split-cdata-sections"</code></dt>
 * <dd>
 * <dl>
 * <dt>
 * <code>true</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>)Split CDATA sections containing the CDATA section termination marker
 * ']]&gt;'. When a CDATA section is split a warning is issued with a
 * <code>DOMError.type</code> equals to
 * <code>"cdata-sections-splitted"</code> and
 * <code>DOMError.relatedData</code> equals to the first
 * <code>CDATASection</code> node in document order resulting from the split.</dd>
 * <dt>
 * <code>false</code></dt>
 * <dd>[<em>required</em>]Signal an error if a <code>CDATASection</code> contains an
 * unrepresentable character.</dd>
 * </dl></dd>
 * <dt><code>"validate"</code></dt>
 * <dd>
 * <dl>
 * <dt><code>true</code></dt>
 * <dd>[<em>optional</em>] Require the validation against a schema (i.e. XML schema, DTD, any
 * other type or representation of schema) of the document as it is being
 * normalized as defined by [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>]. If
 * validation errors are found, or no schema was found, the error handler is
 * notified. Schema-normalized values will not be exposed according to the
 * schema in used unless the parameter "datatype-normalization" is
 * <code>true</code>.  This parameter will reevaluate:
 * <ul>
 * <li> Attribute nodes with
 * <code>Attr.specified</code> equals to <code>false</code>, as specified in
 * the description of the <code>Attr</code> interface;
 * </li>
 * <li> The value of the
 * attribute <code>Text.isElementContentWhitespace</code> for all
 * <code>Text</code> nodes;
 * </li>
 * <li> The value of the attribute
 * <code>Attr.isId</code> for all <code>Attr</code> nodes;
 * </li>
 * <li> The attributes
 * <code>Element.schemaTypeInfo</code> and <code>Attr.schemaTypeInfo</code>.
 * </li>
 * </ul>
 * <p ><b>Note:</b>  "validate-if-schema" and "validate" are mutually
 * exclusive, setting one of them to <code>true</code> will set the other
 * one to <code>false</code>. Applications should also consider setting the
 * parameter "well-formed" to <code>true</code>, which is the default for
 * that option, when validating the document. </dd>
 * <dt><code>false</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>) Do not accomplish schema processing, including the internal subset
 * processing. Default attribute values information are kept. Note that
 * validation might still happen if "validate-if-schema" is <code>true</code>
 * . </dd>
 * </dl></dd>
 * <dt><code>"validate-if-schema"</code></dt>
 * <dd>
 * <dl>
 * <dt><code>true</code></dt>
 * <dd>[<em>optional</em>]Enable validation only if a declaration for the document element can be
 * found in a schema (independently of where it is found, i.e. XML schema,
 * DTD, or any other type or representation of schema). If validation is
 * enabled, this parameter has the same behavior as the parameter "validate"
 * set to <code>true</code>.
 * <p ><b>Note:</b>  "validate-if-schema" and "validate" are mutually
 * exclusive, setting one of them to <code>true</code> will set the other
 * one to <code>false</code>. </dd>
 * <dt><code>false</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>) No schema processing should be performed if the document has a schema,
 * including internal subset processing. Default attribute values
 * information are kept. Note that validation must still happen if "validate
 * " is <code>true</code>. </dd>
 * </dl></dd>
 * <dt><code>"well-formed"</code></dt>
 * <dd>
 * <dl>
 * <dt><code>true</code></dt>
 * <dd>[<em>required</em>] (<em>default</em>) Check if all nodes are XML well formed according to the XML version in
 * use in <code>Document.xmlVersion</code>:
 * <ul>
 * <li> check if the attribute
 * <code>Node.nodeName</code> contains invalid characters according to its
 * node type and generate a <code>DOMError</code> of type
 * <code>"wf-invalid-character-in-node-name"</code>, with a
 * <code>DOMError.SEVERITY_ERROR</code> severity, if necessary;
 * </li>
 * <li> check if
 * the text content inside <code>Attr</code>, <code>Element</code>,
 * <code>Comment</code>, <code>Text</code>, <code>CDATASection</code> nodes
 * for invalid characters and generate a <code>DOMError</code> of type
 * <code>"wf-invalid-character"</code>, with a
 * <code>DOMError.SEVERITY_ERROR</code> severity, if necessary;
 * </li>
 * <li> check if
 * the data inside <code>ProcessingInstruction</code> nodes for invalid
 * characters and generate a <code>DOMError</code> of type
 * <code>"wf-invalid-character"</code>, with a
 * <code>DOMError.SEVERITY_ERROR</code> severity, if necessary;
 * </li>
 * </ul></dd>
 * <dt>
 * <code>false</code></dt>
 * <dd>[<em>optional</em>] Do not check for XML well-formedness. </dd>
 * </dl></dd>
 * </dl>
 * <p> The resolution of the system identifiers associated with entities is
 * done using <code>Document.documentURI</code>. However, when the feature
 * "LS" defined in [<a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-LS-20040407'>DOM Level 3 Load and Save</a>]
 *  is supported by the DOM implementation, the parameter
 * "resource-resolver" can also be used on <code>DOMConfiguration</code>
 * objects attached to <code>Document</code> nodes. If this parameter is
 * set, <code>Document.normalizeDocument()</code> will invoke the resource
 * resolver instead of using <code>Document.documentURI</code>.
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>Document Object Model (DOM) Level 3 Core Specification</a>.
 * @since 1.5, DOM Level 3
 */
public interface DOMConfiguration {
    /**
     * Set the value of a parameter.
     * @param name The name of the parameter to set.
     * @param value  The new value or <code>null</code> if the user wishes to
     *   unset the parameter. While the type of the value parameter is
     *   defined as <code>DOMUserData</code>, the object type must match the
     *   type defined by the definition of the parameter. For example, if
     *   the parameter is "error-handler", the value must be of type
     *   <code>DOMErrorHandler</code>.
     * @exception DOMException
     *    NOT_FOUND_ERR: Raised when the parameter name is not recognized.
     *   <br> NOT_SUPPORTED_ERR: Raised when the parameter name is recognized
     *   but the requested value cannot be set.
     *   <br> TYPE_MISMATCH_ERR: Raised if the value type for this parameter
     *   name is incompatible with the expected value type.
     */
    public void setParameter(String name,
                             Object value)
                             throws DOMException;

    /**
     *  Return the value of a parameter if known.
     * @param name  The name of the parameter.
     * @return  The current object associated with the specified parameter or
     *   <code>null</code> if no object has been associated or if the
     *   parameter is not supported.
     * @exception DOMException
     *    NOT_FOUND_ERR: Raised when the parameter name is not recognized.
     */
    public Object getParameter(String name)
                               throws DOMException;

    /**
     * Check if setting a parameter to a specific value is supported.
     * @param name The name of the parameter to check.
     * @param value  An object. if <code>null</code>, the returned value is
     *   <code>true</code>.
     * @return  <code>true</code> if the parameter could be successfully set
     *   to the specified value, or <code>false</code> if the parameter is
     *   not recognized or the requested value is not supported. This does
     *   not change the current value of the parameter itself.
     */
    public boolean canSetParameter(String name,
                                   Object value);

    /**
     *  The list of the parameters supported by this
     * <code>DOMConfiguration</code> object and for which at least one value
     * can be set by the application. Note that this list can also contain
     * parameter names defined outside this specification.
     */
    public DOMStringList getParameterNames();

}
