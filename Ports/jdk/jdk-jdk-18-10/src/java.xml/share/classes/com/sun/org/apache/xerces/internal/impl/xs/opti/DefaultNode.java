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

import org.w3c.dom.UserDataHandler;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import org.w3c.dom.DOMException;


/**
 * @xerces.internal
 *
 * @author Rahul Srivastava, Sun Microsystems Inc.
 *
 */
public class DefaultNode implements Node {

    // default constructor
    public DefaultNode() {
    }

    //
    // org.w3c.dom.Node methods
    //

    // getter methods
    public String getNodeName() {
        return null;
    }


    public String getNodeValue() throws DOMException {
        return null;
    }


    public short getNodeType() {
        return -1;
    }


    public Node getParentNode() {
        return null;
    }


    public NodeList getChildNodes() {
        return null;
    }


    public Node getFirstChild() {
        return null;
    }


    public Node getLastChild() {
        return null;
    }


    public Node getPreviousSibling() {
        return null;
    }


    public Node getNextSibling() {
        return null;
    }


    public NamedNodeMap getAttributes() {
        return null;
    }


    public Document getOwnerDocument() {
        return null;
    }


    public boolean hasChildNodes() {
        return false;
    }


    public Node cloneNode(boolean deep) {
        return null;
    }


    public void normalize() {
    }


    public boolean isSupported(String feature, String version) {
        return false;
    }


    public String getNamespaceURI() {
        return null;
    }


    public String getPrefix() {
        return null;
    }


    public String getLocalName() {
        return null;
    }
    /** DOM Level 3*/
    public String getBaseURI(){
        return null;
    }



    public boolean hasAttributes() {
        return false;
    }

    // setter methods
    public void setNodeValue(String nodeValue) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public Node removeChild(Node oldChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public Node appendChild(Node newChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }


    public void setPrefix(String prefix) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    public short compareDocumentPosition(Node other){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    public String getTextContent() throws DOMException{
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }
    public void setTextContent(String textContent)throws DOMException{
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }
    public boolean isSameNode(Node other){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");

    }
    public String lookupPrefix(String namespaceURI){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
                                        }
    public boolean isDefaultNamespace(String namespaceURI){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    public String lookupNamespaceURI(String prefix){
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    public boolean isEqualNode(Node arg){
       throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");

    }

    public Object getFeature(String feature, String version){
        return null;
    }
    public Object setUserData(String key,  Object data, UserDataHandler handler){
       throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }
    public Object getUserData(String key){
        return null;
    }


}
