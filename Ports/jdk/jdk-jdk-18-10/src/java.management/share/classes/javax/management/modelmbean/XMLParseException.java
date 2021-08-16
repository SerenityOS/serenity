/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @author    IBM Corp.
 *
 * Copyright IBM Corp. 1999-2000.  All rights reserved.
 */


package javax.management.modelmbean;

import com.sun.jmx.mbeanserver.GetPropertyAction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.security.AccessController;

/**
* This exception is thrown when an XML formatted string is being parsed into ModelMBean objects
* or when XML formatted strings are being created from ModelMBean objects.
*
* It is also used to wrapper exceptions from XML parsers that may be used.
*
* <p>The <b>serialVersionUID</b> of this class is <code>3176664577895105181L</code>.
*
* @since 1.5
*/
@SuppressWarnings("serial")  // serialVersionUID not constant
public class XMLParseException
extends Exception
{
    // Serialization compatibility stuff:
    // Two serial forms are supported in this class. The selected form depends
    // on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form
    private static final long oldSerialVersionUID = -7780049316655891976L;
    //
    // Serial version for new serial form
    private static final long newSerialVersionUID = 3176664577895105181L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields =
    {
      new ObjectStreamField("msgStr", String.class)
    };
    //
    // Serializable fields in new serial form
  private static final ObjectStreamField[] newSerialPersistentFields = { };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;
    private static final ObjectStreamField[] serialPersistentFields;
    private static boolean compat = false;
    static {
        try {
            GetPropertyAction act = new GetPropertyAction("jmx.serial.form");
            @SuppressWarnings("removal")
            String form = AccessController.doPrivileged(act);
            compat = (form != null && form.equals("1.0"));
        } catch (Exception e) {
            // OK: No compat with 1.0
        }
        if (compat) {
            serialPersistentFields = oldSerialPersistentFields;
            serialVersionUID = oldSerialVersionUID;
        } else {
            serialPersistentFields = newSerialPersistentFields;
            serialVersionUID = newSerialVersionUID;
        }
    }
    //
    // END Serialization compatibility stuff

    /**
     * Default constructor .
     */
    public  XMLParseException ()
    {
      super("XML Parse Exception.");
    }

    /**
     * Constructor taking a string.
     *
     * @param s the detail message.
     */
    public  XMLParseException (String s)
    {
      super("XML Parse Exception: " + s);
    }
    /**
     * Constructor taking a string and an exception.
     *
     * @param e the nested exception.
     * @param s the detail message.
     */
    public  XMLParseException (Exception e, String s)
    {
      super("XML Parse Exception: " + s + ":" + e.toString());
    }

    /**
     * Deserializes an {@link XMLParseException} from an {@link ObjectInputStream}.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
      // New serial form ignores extra field "msgStr"
      in.defaultReadObject();
    }


    /**
     * Serializes an {@link XMLParseException} to an {@link ObjectOutputStream}.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
      if (compat)
      {
        // Serializes this instance in the old serial form
        //
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("msgStr", getMessage());
        out.writeFields();
      }
      else
      {
        // Serializes this instance in the new serial form
        //
        out.defaultWriteObject();
      }
    }
}
