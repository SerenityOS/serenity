/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: AttributesImplSerializer.java,v 1.2.4.1 2005/09/15 08:15:14 suresh_emailid Exp $
 */

package com.sun.org.apache.xml.internal.serializer;

import java.util.HashMap;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class extends org.xml.sax.helpers.AttributesImpl which implements org.
 * xml.sax.Attributes. But for optimization this class adds a Map for
 * faster lookup of an index by qName, which is commonly done in the stream
 * serializer.
 *
 * @see org.xml.sax.Attributes
 *
 * @xsl.usage internal
 */
public final class AttributesImplSerializer extends AttributesImpl
{
    /**
     * Hash table of qName/index values to quickly lookup the index
     * of an attributes qName.  qNames are in uppercase in the hash table
     * to make the search case insensitive.
     *
     * The keys to the hashtable to find the index are either
     * "prefix:localName"  or "{uri}localName".
     */
    private final Map<String, Integer> m_indexFromQName = new HashMap<>();

    private final StringBuffer m_buff = new StringBuffer();

    /**
     * This is the number of attributes before switching to the hash table,
     * and can be tuned, but 12 seems good for now - Brian M.
     */
    private static final int MAX = 12;

    /**
     * One less than the number of attributes before switching to
     * the Map.
     */
    private static final int MAXMinus1 = MAX - 1;

    /**
     * This method gets the index of an attribute given its qName.
     * @param qname the qualified name of the attribute, e.g. "prefix1:locName1"
     * @return the integer index of the attribute.
     * @see org.xml.sax.Attributes#getIndex(String)
     */
    public final int getIndex(String qname)
    {
        int index;

        if (super.getLength() < MAX)
        {
            // if we haven't got too many attributes let the
            // super class look it up
            index = super.getIndex(qname);
            return index;
        }
        // we have too many attributes and the super class is slow
        // so find it quickly using our Map.
        Integer i = m_indexFromQName.get(qname);
        if (i == null)
            index = -1;
        else
            index = i.intValue();
        return index;
    }
    /**
     * This method adds the attribute, but also records its qName/index pair in
     * the hashtable for fast lookup by getIndex(qName).
     * @param uri the URI of the attribute
     * @param local the local name of the attribute
     * @param qname the qualified name of the attribute
     * @param type the type of the attribute
     * @param val the value of the attribute
     *
     * @see org.xml.sax.helpers.AttributesImpl#addAttribute(String, String, String, String, String)
     * @see #getIndex(String)
     */
    public final void addAttribute(
        String uri,
        String local,
        String qname,
        String type,
        String val)
    {
        int index = super.getLength();
        super.addAttribute(uri, local, qname, type, val);
        // (index + 1) is now the number of attributes
        // so either compare (index+1) to MAX, or compare index to (MAX-1)

        if (index < MAXMinus1)
        {
            return;
        }
        else if (index == MAXMinus1)
        {
            switchOverToHash(MAX);
        }
        else
        {
            /* add the key with the format of "prefix:localName" */
            /* we have just added the attibute, its index is the old length */
            Integer i = index;
            m_indexFromQName.put(qname, i);

            /* now add with key of the format "{uri}localName" */
            m_buff.setLength(0);
            m_buff.append('{').append(uri).append('}').append(local);
            String key = m_buff.toString();
            m_indexFromQName.put(key, i);
        }
    }

    /**
     * We are switching over to having a hash table for quick look
     * up of attributes, but up until now we haven't kept any
     * information in the Map, so we now update the Map.
     * Future additional attributes will update the Map as
     * they are added.
     * @param numAtts
     */
    private void switchOverToHash(int numAtts)
    {
        for (int index = 0; index < numAtts; index++)
        {
            String qName = super.getQName(index);
            Integer i = index;
            m_indexFromQName.put(qName, i);

            // Add quick look-up to find with uri/local name pair
            String uri = super.getURI(index);
            String local = super.getLocalName(index);
            m_buff.setLength(0);
            m_buff.append('{').append(uri).append('}').append(local);
            String key = m_buff.toString();
            m_indexFromQName.put(key, i);
        }
    }

    /**
     * This method clears the accumulated attributes.
     *
     * @see org.xml.sax.helpers.AttributesImpl#clear()
     */
    public final void clear()
    {

        int len = super.getLength();
        super.clear();
        if (MAX <= len)
        {
            // if we have had enough attributes and are
            // using the Map, then clear the Map too.
            m_indexFromQName.clear();
        }

    }

    /**
     * This method sets the attributes, previous attributes are cleared,
     * it also keeps the hashtable up to date for quick lookup via
     * getIndex(qName).
     * @param atts the attributes to copy into these attributes.
     * @see org.xml.sax.helpers.AttributesImpl#setAttributes(Attributes)
     * @see #getIndex(String)
     */
    public final void setAttributes(Attributes atts)
    {

        super.setAttributes(atts);

        // we've let the super class add the attributes, but
        // we need to keep the hash table up to date ourselves for the
        // potentially new qName/index pairs for quick lookup.
        int numAtts = atts.getLength();
        if (MAX <= numAtts)
            switchOverToHash(numAtts);

    }

    /**
     * This method gets the index of an attribute given its uri and locanName.
     * @param uri the URI of the attribute name.
     * @param localName the local namer (after the ':' ) of the attribute name.
     * @return the integer index of the attribute.
     * @see org.xml.sax.Attributes#getIndex(String)
     */
    public final int getIndex(String uri, String localName)
    {
        int index;

        if (super.getLength() < MAX)
        {
            // if we haven't got too many attributes let the
            // super class look it up
            index = super.getIndex(uri,localName);
            return index;
        }
        // we have too many attributes and the super class is slow
        // so find it quickly using our Map.
        // Form the key of format "{uri}localName"
        m_buff.setLength(0);
        m_buff.append('{').append(uri).append('}').append(localName);
        String key = m_buff.toString();
        Integer i = m_indexFromQName.get(key);
        if (i == null)
            index = -1;
        else
            index = i;
        return index;
    }
}
