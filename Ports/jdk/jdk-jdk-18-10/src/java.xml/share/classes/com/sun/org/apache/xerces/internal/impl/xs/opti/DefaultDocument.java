/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl.xs.opti;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.Element;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.DocumentType;
import org.w3c.dom.CDATASection;
import org.w3c.dom.EntityReference;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.ProcessingInstruction;

import org.w3c.dom.DOMException;


/**
 * @xerces.internal
 *
 * @author Rahul Srivastava, Sun Microsystems Inc.
 *
 */
public class DefaultDocument extends NodeImpl
                             implements Document {

    private String fDocumentURI = null;

    // default constructor
    public DefaultDocument() {
    }

    //
    // org.w3c.dom.Document methods
    //

    public DocumentType getDoctype() {
        return null;
    }


    public DOMImplementation getImplementation() {
        return null;
    }


    public Element getDocumentElement() {
        return null;
    }


    public NodeList getElementsByTagName(String tagname) {
        return null;
    }


    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        return null;
    }


    public Element getElementById(String elementId) {
        return null;
    }


    public Node importNode(Node importedNode, boolean deep) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public Element createElement(String tagName) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public DocumentFragment createDocumentFragment() {
        return null;
    }


    public Text createTextNode(String data) {
        return null;
    }

    public Comment createComment(String data) {
        return null;
    }


    public CDATASection createCDATASection(String data) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public Attr createAttribute(String name) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public EntityReference createEntityReference(String name) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    // DOM Level 3 methods.

    public String getInputEncoding(){
        return null;
    }

    /**
    public void setInputEncoding(String actualEncoding){
       throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }
        */

    public String getXmlEncoding(){
        return null;
    }


    /**
     * An attribute specifying, as part of the XML declaration, the encoding
     * of this document. This is <code>null</code> when unspecified.
     * @since DOM Level 3
    public void setXmlEncoding(String encoding){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }
     */

    /**
     * An attribute specifying, as part of the XML declaration, whether this
     * document is standalone.
     * <br> This attribute represents the property [standalone] defined in .
     * @since DOM Level 3
     */
    public boolean getXmlStandalone(){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }
    /**
     * An attribute specifying, as part of the XML declaration, whether this
     * document is standalone.
     * <br> This attribute represents the property [standalone] defined in .
     * @since DOM Level 3
     */
    public void setXmlStandalone(boolean standalone){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /**
     * An attribute specifying, as part of the XML declaration, the version
     * number of this document. This is <code>null</code> when unspecified.
     * <br> This attribute represents the property [version] defined in .
     * @exception DOMException
     *   NOT_SUPPORTED_ERR: Raised if the version is set to a value that is
     *   not supported by this <code>Document</code>.
     * @since DOM Level 3
     */
    public String getXmlVersion(){
        return null;
    }
    /**
     * An attribute specifying, as part of the XML declaration, the version
     * number of this document. This is <code>null</code> when unspecified.
     * <br> This attribute represents the property [version] defined in .
     * @exception DOMException
     *   NOT_SUPPORTED_ERR: Raised if the version is set to a value that is
     *   not supported by this <code>Document</code>.
     * @since DOM Level 3
     */
    public void setXmlVersion(String version) throws DOMException{
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /**
     * An attribute specifying whether errors checking is enforced or not.
     * When set to <code>false</code>, the implementation is free to not
     * test every possible error case normally defined on DOM operations,
     * and not raise any <code>DOMException</code>. In case of error, the
     * behavior is undefined. This attribute is <code>true</code> by
     * defaults.
     * @since DOM Level 3
     */
    public boolean getStrictErrorChecking(){
        return false;
    }
    /**
     * An attribute specifying whether errors checking is enforced or not.
     * When set to <code>false</code>, the implementation is free to not
     * test every possible error case normally defined on DOM operations,
     * and not raise any <code>DOMException</code>. In case of error, the
     * behavior is undefined. This attribute is <code>true</code> by
     * defaults.
     * @since DOM Level 3
     */
    public void setStrictErrorChecking(boolean strictErrorChecking){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /**
     * The location of the document or <code>null</code> if undefined.
     * <br>Beware that when the <code>Document</code> supports the feature
     * "HTML" , the href attribute of the HTML BASE element takes precedence
     * over this attribute.
     * @since DOM Level 3
     */
    public String getDocumentURI() {
        return fDocumentURI;
    }

    /**
     * The location of the document or <code>null</code> if undefined.
     * <br>Beware that when the <code>Document</code> supports the feature
     * "HTML" , the href attribute of the HTML BASE element takes precedence
     * over this attribute.
     * @since DOM Level 3
     */
    public void setDocumentURI(String documentURI) {
        fDocumentURI = documentURI;
    }

    /** DOM Level 3*/
    public Node adoptNode(Node source) throws DOMException{
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** DOM Level 3*/
    public void normalizeDocument(){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /**
     *  The configuration used when <code>Document.normalizeDocument</code> is
     * invoked.
     * @since DOM Level 3
     */
    public DOMConfiguration getDomConfig(){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** DOM Level 3*/
    public Node renameNode(Node n,String namespaceURI, String name) throws DOMException{
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }








}
