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

package com.sun.org.apache.xerces.internal.xs;

/**
 * The interface represents the namespace schema information information item.
 * Each namespace schema information information item corresponds to an XML
 * Schema with a unique namespace name.
 */
public interface XSNamespaceItem {
    /**
     * [schema namespace]: A namespace name or <code>null</code> if absent.
     */
    public String getSchemaNamespace();

    /**
     * [schema components]: a list of top-level components, i.e. element
     * declarations, attribute declarations, etc. Identity-constraint
     * definitions are also considered top-level.
     *
     * @param objectType The type of the declaration, i.e.
     *   <code>ELEMENT_DECLARATION</code>. Note that
     *   <code>XSTypeDefinition.SIMPLE_TYPE</code> and
     *   <code>XSTypeDefinition.COMPLEX_TYPE</code> can also be used as the
     *   <code>objectType</code> to retrieve only complex types or simple
     *   types, instead of all types.
     * @return  A list of top-level definition of the specified type in
     *   <code>objectType</code> or an empty <code>XSNamedMap</code> if no
     *   such definitions exist.
     */
    public XSNamedMap getComponents(short objectType);

    /**
     *  [annotations]: a set of annotations if it exists, otherwise an empty
     * <code>XSObjectList</code>.
     */
    public XSObjectList getAnnotations();

    /**
     * Convenience method. Returns a top-level element declaration.
     * @param name The name of the declaration.
     * @return A top-level element declaration or <code>null</code> if such a
     *   declaration does not exist.
     */
    public XSElementDeclaration getElementDeclaration(String name);

    /**
     * Convenience method. Returns a top-level attribute declaration.
     * @param name The name of the declaration.
     * @return A top-level attribute declaration or <code>null</code> if such
     *   a declaration does not exist.
     */
    public XSAttributeDeclaration getAttributeDeclaration(String name);

    /**
     * Convenience method. Returns a top-level simple or complex type
     * definition.
     * @param name The name of the definition.
     * @return An <code>XSTypeDefinition</code> or <code>null</code> if such
     *   a definition does not exist.
     */
    public XSTypeDefinition getTypeDefinition(String name);

    /**
     * Convenience method. Returns a top-level attribute group definition.
     * @param name The name of the definition.
     * @return A top-level attribute group definition or <code>null</code> if
     *   such a definition does not exist.
     */
    public XSAttributeGroupDefinition getAttributeGroup(String name);

    /**
     * Convenience method. Returns a top-level model group definition.
     * @param name The name of the definition.
     * @return A top-level model group definition definition or
     *   <code>null</code> if such a definition does not exist.
     */
    public XSModelGroupDefinition getModelGroupDefinition(String name);

    /**
     * Convenience method. Returns a top-level notation declaration.
     * @param name The name of the declaration.
     * @return A top-level notation declaration or <code>null</code> if such
     *   a declaration does not exist.
     */
    public XSNotationDeclaration getNotationDeclaration(String name);

    /**
     * Convenience method. Returns an identity-constraint definition.
     * @param name The name of the definition.
     * @return An identity-constraint definition or <code>null</code> if such
     *   a declaration does not exist.
     */
    public XSIDCDefinition getIDCDefinition(String name);

    /**
     * [document location] - a list of location URIs for the documents that
     * contributed to the <code>XSModel</code>.
     */
    public StringList getDocumentLocations();

}
