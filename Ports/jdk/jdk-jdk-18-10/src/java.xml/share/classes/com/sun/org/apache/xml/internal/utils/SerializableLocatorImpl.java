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

package com.sun.org.apache.xml.internal.utils;


/**
 * The standard SAX implementation of LocatorImpl is not serializable,
 * limiting its utility as "a persistent snapshot of a locator".
 * This is a quick hack to make it so. Note that it makes more sense
 * in many cases to set up fields to hold this data rather than pointing
 * at another object... but that decision should be made on architectural
 * grounds rather than serializability.
 *<p>
 * It isn't clear whether subclassing LocatorImpl and adding serialization
 * methods makes more sense than copying it and just adding Serializable
 * to its interface. Since it's so simple, I've taken the latter approach
 * for now.
 *
 * @see org.xml.sax.helpers.LocatorImpl
 * @see org.xml.sax.Locator Locator
 * @since XalanJ2
 * @author Joe Kesselman
 */
public class SerializableLocatorImpl
implements org.xml.sax.Locator, java.io.Serializable

{
    static final long serialVersionUID = -2660312888446371460L;
    /**
     * Zero-argument constructor.
     *
     * <p>SAX says "This will not normally be useful, since the main purpose
     * of this class is to make a snapshot of an existing Locator." In fact,
     * it _is_ sometimes useful when you want to construct a new Locator
     * pointing to a specific location... which, after all, is why the
     * setter methods are provided.
     * </p>
     */
    public SerializableLocatorImpl ()
    {
    }


    /**
     * Copy constructor.
     *
     * <p>Create a persistent copy of the current state of a locator.
     * When the original locator changes, this copy will still keep
     * the original values (and it can be used outside the scope of
     * DocumentHandler methods).</p>
     *
     * @param locator The locator to copy.
     */
    public SerializableLocatorImpl (org.xml.sax.Locator locator)
    {
        setPublicId(locator.getPublicId());
        setSystemId(locator.getSystemId());
        setLineNumber(locator.getLineNumber());
        setColumnNumber(locator.getColumnNumber());
    }


    ////////////////////////////////////////////////////////////////////
    // Implementation of org.xml.sax.Locator
    ////////////////////////////////////////////////////////////////////


    /**
     * Return the saved public identifier.
     *
     * @return The public identifier as a string, or null if none
     *         is available.
     * @see org.xml.sax.Locator#getPublicId
     * @see #setPublicId
     */
    public String getPublicId ()
    {
        return publicId;
    }


    /**
     * Return the saved system identifier.
     *
     * @return The system identifier as a string, or null if none
     *         is available.
     * @see org.xml.sax.Locator#getSystemId
     * @see #setSystemId
     */
    public String getSystemId ()
    {
        return systemId;
    }


    /**
     * Return the saved line number (1-based).
     *
     * @return The line number as an integer, or -1 if none is available.
     * @see org.xml.sax.Locator#getLineNumber
     * @see #setLineNumber
     */
    public int getLineNumber ()
    {
        return lineNumber;
    }


    /**
     * Return the saved column number (1-based).
     *
     * @return The column number as an integer, or -1 if none is available.
     * @see org.xml.sax.Locator#getColumnNumber
     * @see #setColumnNumber
     */
    public int getColumnNumber ()
    {
        return columnNumber;
    }


    ////////////////////////////////////////////////////////////////////
    // Setters for the properties (not in org.xml.sax.Locator)
    ////////////////////////////////////////////////////////////////////


    /**
     * Set the public identifier for this locator.
     *
     * @param publicId The new public identifier, or null
     *        if none is available.
     * @see #getPublicId
     */
    public void setPublicId (String publicId)
    {
        this.publicId = publicId;
    }


    /**
     * Set the system identifier for this locator.
     *
     * @param systemId The new system identifier, or null
     *        if none is available.
     * @see #getSystemId
     */
    public void setSystemId (String systemId)
    {
        this.systemId = systemId;
    }


    /**
     * Set the line number for this locator (1-based).
     *
     * @param lineNumber The line number, or -1 if none is available.
     * @see #getLineNumber
     */
    public void setLineNumber (int lineNumber)
    {
        this.lineNumber = lineNumber;
    }


    /**
     * Set the column number for this locator (1-based).
     *
     * @param columnNumber The column number, or -1 if none is available.
     * @see #getColumnNumber
     */
    public void setColumnNumber (int columnNumber)
    {
        this.columnNumber = columnNumber;
    }


    ////////////////////////////////////////////////////////////////////
    // Internal state.
    ////////////////////////////////////////////////////////////////////

    /**
     * The public ID.
     * @serial
     */
    private String publicId;

    /**
     * The system ID.
     * @serial
     */
    private String systemId;

    /**
     * The line number.
     * @serial
     */
    private int lineNumber;

    /**
     * The column number.
     * @serial
     */
    private int columnNumber;

}

// end of LocatorImpl.java
