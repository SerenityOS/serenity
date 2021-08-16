/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql.rowset.serial;

import java.sql.*;
import java.io.*;
import java.net.URL;


/**
 * A serialized mapping in the Java programming language of an SQL
 * <code>DATALINK</code> value. A <code>DATALINK</code> value
 * references a file outside of the underlying data source that the
 * data source manages.
 * <P>
 * <code>RowSet</code> implementations can use the method <code>RowSet.getURL</code>
 * to retrieve a <code>java.net.URL</code> object, which can be used
 * to manipulate the external data.
 * <pre>
 *      java.net.URL url = rowset.getURL(1);
 * </pre>
 *
 * <h2> Thread safety </h2>
 *
 * A SerialDatalink is not safe for use by multiple concurrent threads.  If a
 * SerialDatalink is to be used by more than one thread then access to the
 * SerialDatalink should be controlled by appropriate synchronization.
 *
 * @since 1.5
 */
public class SerialDatalink implements Serializable, Cloneable {

    /**
     * The extracted URL field retrieved from the DATALINK field.
     * @serial
     */
    private URL url;

    /**
     * The SQL type of the elements in this <code>SerialDatalink</code>
     * object.  The type is expressed as one of the constants from the
     * class <code>java.sql.Types</code>.
     * @serial
     */
    private int baseType;

    /**
     * The type name used by the DBMS for the elements in the SQL
     * <code>DATALINK</code> value that this SerialDatalink object
     * represents.
     * @serial
     */
    private String baseTypeName;

    /**
      * Constructs a new <code>SerialDatalink</code> object from the given
      * <code>java.net.URL</code> object.
      *
      * @param url the {@code URL} to create the {@code SerialDataLink} from
      * @throws SerialException if url parameter is a null
      */
    public SerialDatalink(URL url) throws SerialException {
        if (url == null) {
            throw new SerialException("Cannot serialize empty URL instance");
        }
        this.url = url;
    }

    /**
     * Returns a new URL that is a copy of this <code>SerialDatalink</code>
     * object.
     *
     * @return a copy of this <code>SerialDatalink</code> object as a
     * <code>URL</code> object in the Java programming language.
     * @throws SerialException if the <code>URL</code> object cannot be de-serialized
     */
    public URL getDatalink() throws SerialException {

        URL aURL = null;

        try {
            aURL = new URL((this.url).toString());
        } catch (java.net.MalformedURLException e) {
            throw new SerialException("MalformedURLException: " + e.getMessage());
        }
        return aURL;
    }

    /**
     * Compares this {@code SerialDatalink} to the specified object.
     * The result is {@code true} if and only if the argument is not
     * {@code null} and is a {@code SerialDatalink} object whose URL is
     * identical to this object's URL
     *
     * @param  obj The object to compare this {@code SerialDatalink} against
     *
     * @return  {@code true} if the given object represents a {@code SerialDatalink}
     *          equivalent to this SerialDatalink, {@code false} otherwise
     *
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SerialDatalink) {
            SerialDatalink sdl = (SerialDatalink) obj;
            return url.equals(sdl.url);
        }
        return false;
    }

    /**
     * Returns a hash code for this {@code SerialDatalink}. The hash code for a
     * {@code SerialDatalink} object is taken as the hash code of
     * the {@code URL} it stores
     *
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        return 31 + url.hashCode();
    }

    /**
     * Returns a clone of this {@code SerialDatalink}.
     *
     * @return  a clone of this SerialDatalink
     */
    public Object clone() {
        try {
            SerialDatalink sdl = (SerialDatalink) super.clone();
            return sdl;
        } catch (CloneNotSupportedException ex) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    /**
     * readObject and writeObject are called to restore the state
     * of the {@code SerialDatalink}
     * from a stream. Note: we leverage the default Serialized form
     */

    /**
     * The identifier that assists in the serialization of this
     *  {@code SerialDatalink} object.
     */
    static final long serialVersionUID = 2826907821828733626L;
}
