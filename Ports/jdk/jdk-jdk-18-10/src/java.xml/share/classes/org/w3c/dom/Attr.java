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
 * The <code>Attr</code> interface represents an attribute in an
 * <code>Element</code> object. Typically the allowable values for the
 * attribute are defined in a schema associated with the document.
 * <p><code>Attr</code> objects inherit the <code>Node</code> interface, but
 * since they are not actually child nodes of the element they describe, the
 * DOM does not consider them part of the document tree. Thus, the
 * <code>Node</code> attributes <code>parentNode</code>,
 * <code>previousSibling</code>, and <code>nextSibling</code> have a
 * <code>null</code> value for <code>Attr</code> objects. The DOM takes the
 * view that attributes are properties of elements rather than having a
 * separate identity from the elements they are associated with; this should
 * make it more efficient to implement such features as default attributes
 * associated with all elements of a given type. Furthermore,
 * <code>Attr</code> nodes may not be immediate children of a
 * <code>DocumentFragment</code>. However, they can be associated with
 * <code>Element</code> nodes contained within a
 * <code>DocumentFragment</code>. In short, users and implementors of the
 * DOM need to be aware that <code>Attr</code> nodes have some things in
 * common with other objects inheriting the <code>Node</code> interface, but
 * they also are quite distinct.
 * <p>The attribute's effective value is determined as follows: if this
 * attribute has been explicitly assigned any value, that value is the
 * attribute's effective value; otherwise, if there is a declaration for
 * this attribute, and that declaration includes a default value, then that
 * default value is the attribute's effective value; otherwise, the
 * attribute does not exist on this element in the structure model until it
 * has been explicitly added. Note that the <code>Node.nodeValue</code>
 * attribute on the <code>Attr</code> instance can also be used to retrieve
 * the string version of the attribute's value(s).
 * <p> If the attribute was not explicitly given a value in the instance
 * document but has a default value provided by the schema associated with
 * the document, an attribute node will be created with
 * <code>specified</code> set to <code>false</code>. Removing attribute
 * nodes for which a default value is defined in the schema generates a new
 * attribute node with the default value and <code>specified</code> set to
 * <code>false</code>. If validation occurred while invoking
 * <code>Document.normalizeDocument()</code>, attribute nodes with
 * <code>specified</code> equals to <code>false</code> are recomputed
 * according to the default attribute values provided by the schema. If no
 * default value is associate with this attribute in the schema, the
 * attribute node is discarded.
 * <p>In XML, where the value of an attribute can contain entity references,
 * the child nodes of the <code>Attr</code> node may be either
 * <code>Text</code> or <code>EntityReference</code> nodes (when these are
 * in use; see the description of <code>EntityReference</code> for
 * discussion).
 * <p>The DOM Core represents all attribute values as simple strings, even if
 * the DTD or schema associated with the document declares them of some
 * specific type such as tokenized.
 * <p>The way attribute value normalization is performed by the DOM
 * implementation depends on how much the implementation knows about the
 * schema in use. Typically, the <code>value</code> and
 * <code>nodeValue</code> attributes of an <code>Attr</code> node initially
 * returns the normalized value given by the parser. It is also the case
 * after <code>Document.normalizeDocument()</code> is called (assuming the
 * right options have been set). But this may not be the case after
 * mutation, independently of whether the mutation is performed by setting
 * the string value directly or by changing the <code>Attr</code> child
 * nodes. In particular, this is true when <a href='http://www.w3.org/TR/2004/REC-xml-20040204#dt-charref'>character
 * references</a> are involved, given that they are not represented in the DOM and they
 * impact attribute value normalization. On the other hand, if the
 * implementation knows about the schema in use when the attribute value is
 * changed, and it is of a different type than CDATA, it may normalize it
 * again at that time. This is especially true of specialized DOM
 * implementations, such as SVG DOM implementations, which store attribute
 * values in an internal form different from a string.
 * <p>The following table gives some examples of the relations between the
 * attribute value in the original document (parsed attribute), the value as
 * exposed in the DOM, and the serialization of the value:
 * <table class="striped">
 * <caption>Examples of the Original, Normalized and Serialized Values </caption>
 * <thead>
 * <tr>
 * <th scope="col">Examples</th>
 * <th scope="col">Parsed
 * attribute value</th>
 * <th scope="col">Initial <code>Attr.value</code></th>
 * <th scope="col">Serialized attribute value</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <th scope="row" valign='top' rowspan='1' colspan='1'>
 * Character reference</th>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"x&amp;#178;=5"</pre>
 * </td>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"x&#178;=5"</pre>
 * </td>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"x&amp;#178;=5"</pre>
 * </td>
 * </tr>
 * <tr>
 * <th scope="row" valign='top' rowspan='1' colspan='1'>Built-in
 * character entity</th>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"y&amp;lt;6"</pre>
 * </td>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"y&lt;6"</pre>
 * </td>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"y&amp;lt;6"</pre>
 * </td>
 * </tr>
 * <tr>
 * <th scope="row" valign='top' rowspan='1' colspan='1'>Literal newline between</th>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>
 * "x=5&amp;#10;y=6"</pre>
 * </td>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"x=5 y=6"</pre>
 * </td>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"x=5&amp;#10;y=6"</pre>
 * </td>
 * </tr>
 * <tr>
 * <th scope="row" valign='top' rowspan='1' colspan='1'>Normalized newline between</th>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"x=5
 * y=6"</pre>
 * </td>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"x=5 y=6"</pre>
 * </td>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>"x=5 y=6"</pre>
 * </td>
 * </tr>
 * <tr>
 * <th scope="row" valign='top' rowspan='1' colspan='1'>Entity <code>e</code> with literal newline</th>
 * <td valign='top' rowspan='1' colspan='1'>
 * <pre>
 * &lt;!ENTITY e '...&amp;#10;...'&gt; [...]&gt; "x=5&amp;e;y=6"</pre>
 * </td>
 * <td valign='top' rowspan='1' colspan='1'><em>Dependent on Implementation and Load Options</em></td>
 * <td valign='top' rowspan='1' colspan='1'><em>Dependent on Implementation and Load/Save Options</em></td>
 * </tr>
 * </tbody>
 * </table>
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>Document Object Model (DOM) Level 3 Core Specification</a>.
 */
public interface Attr extends Node {
    /**
     * Returns the name of this attribute. If <code>Node.localName</code> is
     * different from <code>null</code>, this attribute is a qualified name.
     */
    public String getName();

    /**
     *  <code>True</code> if this attribute was explicitly given a value in
     * the instance document, <code>false</code> otherwise. If the
     * application changed the value of this attribute node (even if it ends
     * up having the same value as the default value) then it is set to
     * <code>true</code>. The implementation may handle attributes with
     * default values from other schemas similarly but applications should
     * use <code>Document.normalizeDocument()</code> to guarantee this
     * information is up-to-date.
     */
    public boolean getSpecified();

    /**
     * On retrieval, the value of the attribute is returned as a string.
     * Character and general entity references are replaced with their
     * values. See also the method <code>getAttribute</code> on the
     * <code>Element</code> interface.
     * <br>On setting, this creates a <code>Text</code> node with the unparsed
     * contents of the string, i.e. any characters that an XML processor
     * would recognize as markup are instead treated as literal text. See
     * also the method <code>Element.setAttribute()</code>.
     * <br> Some specialized implementations, such as some [<a href='http://www.w3.org/TR/2003/REC-SVG11-20030114/'>SVG 1.1</a>]
     * implementations, may do normalization automatically, even after
     * mutation; in such case, the value on retrieval may differ from the
     * value on setting.
     */
    public String getValue();
    /**
     * On retrieval, the value of the attribute is returned as a string.
     * Character and general entity references are replaced with their
     * values. See also the method <code>getAttribute</code> on the
     * <code>Element</code> interface.
     * <br>On setting, this creates a <code>Text</code> node with the unparsed
     * contents of the string, i.e. any characters that an XML processor
     * would recognize as markup are instead treated as literal text. See
     * also the method <code>Element.setAttribute()</code>.
     * <br> Some specialized implementations, such as some [<a href='http://www.w3.org/TR/2003/REC-SVG11-20030114/'>SVG 1.1</a>]
     * implementations, may do normalization automatically, even after
     * mutation; in such case, the value on retrieval may differ from the
     * value on setting.
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
     */
    public void setValue(String value)
                            throws DOMException;

    /**
     * The <code>Element</code> node this attribute is attached to or
     * <code>null</code> if this attribute is not in use.
     * @since 1.4, DOM Level 2
     */
    public Element getOwnerElement();

    /**
     *  The type information associated with this attribute. While the type
     * information contained in this attribute is guarantee to be correct
     * after loading the document or invoking
     * <code>Document.normalizeDocument()</code>, <code>schemaTypeInfo</code>
     *  may not be reliable if the node was moved.
     * @since 1.5, DOM Level 3
     */
    public TypeInfo getSchemaTypeInfo();

    /**
     *  Returns whether this attribute is known to be of type ID (i.e. to
     * contain an identifier for its owner element) or not. When it is and
     * its value is unique, the <code>ownerElement</code> of this attribute
     * can be retrieved using the method <code>Document.getElementById</code>
     * . The implementation could use several ways to determine if an
     * attribute node is known to contain an identifier:
     * <ul>
     * <li> If validation
     * occurred using an XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     *  while loading the document or while invoking
     * <code>Document.normalizeDocument()</code>, the post-schema-validation
     * infoset contributions (PSVI contributions) values are used to
     * determine if this attribute is a schema-determined ID attribute using
     * the <a href='http://www.w3.org/TR/2003/REC-xptr-framework-20030325/#term-sdi'>
     * schema-determined ID</a> definition in [<a href='http://www.w3.org/TR/2003/REC-xptr-framework-20030325/'>XPointer</a>]
     * .
     * </li>
     * <li> If validation occurred using a DTD while loading the document or
     * while invoking <code>Document.normalizeDocument()</code>, the infoset <b>[type definition]</b> value is used to determine if this attribute is a DTD-determined ID
     * attribute using the <a href='http://www.w3.org/TR/2003/REC-xptr-framework-20030325/#term-ddi'>
     * DTD-determined ID</a> definition in [<a href='http://www.w3.org/TR/2003/REC-xptr-framework-20030325/'>XPointer</a>]
     * .
     * </li>
     * <li> from the use of the methods <code>Element.setIdAttribute()</code>,
     * <code>Element.setIdAttributeNS()</code>, or
     * <code>Element.setIdAttributeNode()</code>, i.e. it is an
     * user-determined ID attribute;
     * <p ><b>Note:</b>  XPointer framework (see section 3.2 in [<a href='http://www.w3.org/TR/2003/REC-xptr-framework-20030325/'>XPointer</a>]
     * ) consider the DOM user-determined ID attribute as being part of the
     * XPointer externally-determined ID definition.
     * </li>
     * <li> using mechanisms that
     * are outside the scope of this specification, it is then an
     * externally-determined ID attribute. This includes using schema
     * languages different from XML schema and DTD.
     * </li>
     * </ul>
     * <br> If validation occurred while invoking
     * <code>Document.normalizeDocument()</code>, all user-determined ID
     * attributes are reset and all attribute nodes ID information are then
     * reevaluated in accordance to the schema used. As a consequence, if
     * the <code>Attr.schemaTypeInfo</code> attribute contains an ID type,
     * <code>isId</code> will always return true.
     * @since 1.5, DOM Level 3
     */
    public boolean isId();

}
