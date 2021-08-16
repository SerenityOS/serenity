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

package javax.management.relation;

import static com.sun.jmx.mbeanserver.Util.cast;
import com.sun.jmx.mbeanserver.GetPropertyAction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;

import java.security.AccessController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.ObjectName;

/**
 * Represents a role: includes a role name and referenced MBeans (via their
 * ObjectNames). The role value is always represented as an ArrayList
 * collection (of ObjectNames) to homogenize the access.
 *
 * <p>The <b>serialVersionUID</b> of this class is <code>-279985518429862552L</code>.
 *
 * @since 1.5
 */
@SuppressWarnings("serial")  // serialVersionUID not constant
public class Role implements Serializable {

    // Serialization compatibility stuff:
    // Two serial forms are supported in this class. The selected form depends
    // on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form
    private static final long oldSerialVersionUID = -1959486389343113026L;
    //
    // Serial version for new serial form
    private static final long newSerialVersionUID = -279985518429862552L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields =
    {
      new ObjectStreamField("myName", String.class),
      new ObjectStreamField("myObjNameList", ArrayList.class)
    };
    //
    // Serializable fields in new serial form
    private static final ObjectStreamField[] newSerialPersistentFields =
    {
      new ObjectStreamField("name", String.class),
      new ObjectStreamField("objectNameList", List.class)
    };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;
    /**
     * @serialField name String Role name
     * @serialField objectNameList List {@link List} of {@link ObjectName}s of referenced MBeans
     */
    private static final ObjectStreamField[] serialPersistentFields;
    private static boolean compat = false;
    static {
        try {
            GetPropertyAction act = new GetPropertyAction("jmx.serial.form");
            @SuppressWarnings("removal")
            String form = AccessController.doPrivileged(act);
            compat = (form != null && form.equals("1.0"));
        } catch (Exception e) {
            // OK : Too bad, no compat with 1.0
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

    //
    // Private members
    //

    /**
     * @serial Role name
     */
    private String name = null;

    /**
     * @serial {@link List} of {@link ObjectName}s of referenced MBeans
     */
    private List<ObjectName> objectNameList = new ArrayList<ObjectName>();

    //
    // Constructors
    //

    /**
     * <p>Make a new Role object.
     * No check is made that the ObjectNames in the role value exist in
     * an MBean server.  That check will be made when the role is set
     * in a relation.
     *
     * @param roleName  role name
     * @param roleValue  role value (List of ObjectName objects)
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public Role(String roleName,
                List<ObjectName> roleValue)
        throws IllegalArgumentException {

        if (roleName == null || roleValue == null) {
            String excMsg = "Invalid parameter";
            throw new IllegalArgumentException(excMsg);
        }

        setRoleName(roleName);
        setRoleValue(roleValue);

        return;
    }

    //
    // Accessors
    //

    /**
     * Retrieves role name.
     *
     * @return the role name.
     *
     * @see #setRoleName
     */
    public String getRoleName() {
        return name;
    }

    /**
     * Retrieves role value.
     *
     * @return ArrayList of ObjectName objects for referenced MBeans.
     *
     * @see #setRoleValue
     */
    public List<ObjectName> getRoleValue() {
        return objectNameList;
    }

    /**
     * Sets role name.
     *
     * @param roleName  role name
     *
     * @exception IllegalArgumentException  if null parameter
     *
     * @see #getRoleName
     */
    public void setRoleName(String roleName)
        throws IllegalArgumentException {

        if (roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        name = roleName;
        return;
    }

    /**
     * Sets role value.
     *
     * @param roleValue  List of ObjectName objects for referenced
     * MBeans.
     *
     * @exception IllegalArgumentException  if null parameter
     *
     * @see #getRoleValue
     */
    public void setRoleValue(List<ObjectName> roleValue)
        throws IllegalArgumentException {

        if (roleValue == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        objectNameList = new ArrayList<ObjectName>(roleValue);
        return;
    }

    /**
     * Returns a string describing the role.
     *
     * @return the description of the role.
     */
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("role name: " + name + "; role value: ");
        for (Iterator<ObjectName> objNameIter = objectNameList.iterator();
             objNameIter.hasNext();) {
            ObjectName currObjName = objNameIter.next();
            result.append(currObjName.toString());
            if (objNameIter.hasNext()) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    //
    // Misc
    //

    /**
     * Clone the role object.
     *
     * @return a Role that is an independent copy of the current Role object.
     */
    public Object clone() {

        try {
            return new Role(name, objectNameList);
        } catch (IllegalArgumentException exc) {
            return null; // can't happen
        }
    }

    /**
     * Returns a string for the given role value.
     *
     * @param roleValue  List of ObjectName objects
     *
     * @return A String consisting of the ObjectNames separated by
     * newlines (\n).
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public static String roleValueToString(List<ObjectName> roleValue)
        throws IllegalArgumentException {

        if (roleValue == null) {
            String excMsg = "Invalid parameter";
            throw new IllegalArgumentException(excMsg);
        }

        StringBuilder result = new StringBuilder();
        for (ObjectName currObjName : roleValue) {
            if (result.length() > 0)
                result.append("\n");
            result.append(currObjName.toString());
        }
        return result.toString();
    }

    /**
     * Deserializes a {@link Role} from an {@link ObjectInputStream}.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
      if (compat)
      {
        // Read an object serialized in the old serial form
        //
        ObjectInputStream.GetField fields = in.readFields();
        name = (String) fields.get("myName", null);
        if (fields.defaulted("myName"))
        {
          throw new NullPointerException("myName");
        }
        objectNameList = cast(fields.get("myObjNameList", null));
        if (fields.defaulted("myObjNameList"))
        {
          throw new NullPointerException("myObjNameList");
        }
      }
      else
      {
        // Read an object serialized in the new serial form
        //
        in.defaultReadObject();
      }
    }


    /**
     * Serializes a {@link Role} to an {@link ObjectOutputStream}.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
      if (compat)
      {
        // Serializes this instance in the old serial form
        //
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("myName", name);
        fields.put("myObjNameList", objectNameList);
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
