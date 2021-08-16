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

package com.sun.org.apache.xerces.internal.xni;

/**
 * A structure that holds the components of an XML Namespaces qualified
 * name.
 * <p>
 * To be used correctly, the strings must be identical references for
 * equal strings. Within the parser, these values are considered symbols
 * and should always be retrieved from the <code>SymbolTable</code>.
 *
 * @see <a href="../../../../../xerces2/com/sun/org/apache/xerces/internal/util/SymbolTable.html">com.sun.org.apache.xerces.internal.util.SymbolTable</a>
 *
 * @author Andy Clark, IBM
 *
 * Better performance patch for the equals method by Daniel Petersson: refer to jaxp issue 61;
 * == were used to compare strings
 * @author Joe Wang, Oracle
 *
 */
public class QName
implements Cloneable {


    /**
     * The qname prefix. For example, the prefix for the qname "a:foo"
     * is "a".
     */
    public String prefix;

    /**
     * The qname localpart. For example, the localpart for the qname "a:foo"
     * is "foo".
     */
    public String localpart;

    /**
     * The qname rawname. For example, the rawname for the qname "a:foo"
     * is "a:foo".
     */
    public String rawname;

    /**
     * The URI to which the qname prefix is bound. This binding must be
     * performed by a XML Namespaces aware processor.
     */
    public String uri;

    //
    // Constructors
    //

    /** Default constructor. */
    public QName() {
        clear();
    } // <init>()

    /** Constructs a QName with the specified values. */
    public QName(String prefix, String localpart, String rawname, String uri) {
        setValues(prefix, localpart, rawname, uri);
    } // <init>(String,String,String,String)

    /** Constructs a copy of the specified QName. */
    public QName(QName qname) {
        setValues(qname);
    } // <init>(QName)

    //
    // Public methods
    //

    /**
     * Convenience method to set the values of the qname components.
     *
     * @param QName The qualified name to be copied.
     */
    public void setValues(QName qname) {
        prefix = qname.prefix;
        localpart = qname.localpart;
        rawname = qname.rawname;
        uri = qname.uri;
    } // setValues(QName)

    /**
     * Convenience method to set the values of the qname components.
     *
     * @param prefix    The qname prefix. (e.g. "a")
     * @param localpart The qname localpart. (e.g. "foo")
     * @param rawname   The qname rawname. (e.g. "a:foo")
     * @param uri       The URI binding. (e.g. "http://foo.com/mybinding")
     */
    public void setValues(String prefix, String localpart, String rawname,
    String uri) {
        this.prefix = prefix;
        this.localpart = localpart;
        this.rawname = rawname;
        this.uri = uri;
    } // setValues(String,String,String,String)

    /** Clears the values of the qname components. */
    public void clear() {
        prefix = null;
        localpart = null;
        rawname = null;
        uri = null;
    } // clear()

    //
    // Cloneable methods
    //

    /** Returns a clone of this object. */
    public Object clone() {
        return new QName(this);
    } // clone():Object

    //
    // Object methods
    //

    /** Returns the hashcode for this object. */
    public int hashCode() {
        if (uri != null) {
            return uri.hashCode() +
                ((localpart != null) ? localpart.hashCode() : 0);
        }
        return (rawname != null) ? rawname.hashCode() : 0;
    } // hashCode():int

    /** Returns true if the two objects are equal. */
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (object != null && object instanceof QName) {
            QName qname = (QName)object;
            if (qname.uri != null) {
                    return qname.localpart.equals(localpart) && qname.uri.equals(uri);
            }
            else if (uri == null) {
                return rawname.equals(qname.rawname);
            }
            // fall through and return not equal
        }
        return false;
    } // equals(Object):boolean

    /** Returns a string representation of this object. */
    public String toString() {

        StringBuffer str = new StringBuffer();
        boolean comma = false;
        if (prefix != null) {
            str.append("prefix=\""+prefix+'"');
            comma = true;
        }
        if (localpart != null) {
            if (comma) {
                str.append(',');
            }
            str.append("localpart=\""+localpart+'"');
            comma = true;
        }
        if (rawname != null) {
            if (comma) {
                str.append(',');
            }
            str.append("rawname=\""+rawname+'"');
            comma = true;
        }
        if (uri != null) {
            if (comma) {
                str.append(',');
            }
            str.append("uri=\""+uri+'"');
        }
        return str.toString();

    } // toString():String

} // class QName
