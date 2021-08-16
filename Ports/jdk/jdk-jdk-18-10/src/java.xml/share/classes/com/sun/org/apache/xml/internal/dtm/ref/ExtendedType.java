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

package com.sun.org.apache.xml.internal.dtm.ref;

/**
 * The class ExtendedType represents an extended type object used by
 * ExpandedNameTable.
 */
public final class ExtendedType
{
    private int nodetype;
    private String namespace;
    private String localName;
    private int hash;

    /**
     * Create an ExtendedType object from node type, namespace and local name.
     * The hash code is calculated from the node type, namespace and local name.
     *
     * @param nodetype Type of the node
     * @param namespace Namespace of the node
     * @param localName Local name of the node
     */
    public ExtendedType (int nodetype, String namespace, String localName)
    {
      this.nodetype = nodetype;
      this.namespace = namespace;
      this.localName = localName;
      this.hash = nodetype + namespace.hashCode() + localName.hashCode();
    }

    /**
     * Create an ExtendedType object from node type, namespace, local name
     * and a given hash code.
     *
     * @param nodetype Type of the node
     * @param namespace Namespace of the node
     * @param localName Local name of the node
     * @param hash The given hash code
     */
    public ExtendedType (int nodetype, String namespace, String localName, int hash)
    {
      this.nodetype = nodetype;
      this.namespace = namespace;
      this.localName = localName;
      this.hash = hash;
    }

    /**
     * Redefine this ExtendedType object to represent a different extended type.
     * This is intended to be used ONLY on the hashET object. Using it elsewhere
     * will mess up existing hashtable entries!
     */
    protected void redefine(int nodetype, String namespace, String localName)
    {
      this.nodetype = nodetype;
      this.namespace = namespace;
      this.localName = localName;
      this.hash = nodetype + namespace.hashCode() + localName.hashCode();
    }

    /**
     * Redefine this ExtendedType object to represent a different extended type.
     * This is intended to be used ONLY on the hashET object. Using it elsewhere
     * will mess up existing hashtable entries!
     */
    protected void redefine(int nodetype, String namespace, String localName, int hash)
    {
      this.nodetype = nodetype;
      this.namespace = namespace;
      this.localName = localName;
      this.hash = hash;
    }

    /**
     * Override the hashCode() method in the Object class
     */
    public int hashCode()
    {
      return hash;
    }

    /**
     * Test if this ExtendedType object is equal to the given ExtendedType.
     *
     * @param other The other ExtendedType object to test for equality
     * @return true if the two ExtendedType objects are equal.
     */
    public boolean equals(ExtendedType other)
    {
      try
      {
        return other.nodetype == this.nodetype &&
                other.localName.equals(this.localName) &&
                other.namespace.equals(this.namespace);
      }
      catch(NullPointerException e)
      {
        return false;
      }
    }

    /**
     * Return the node type
     */
    public int getNodeType()
    {
      return nodetype;
    }

    /**
     * Return the local name
     */
    public String getLocalName()
    {
      return localName;
    }

    /**
     * Return the namespace
     */
    public String getNamespace()
    {
      return namespace;
    }

}
