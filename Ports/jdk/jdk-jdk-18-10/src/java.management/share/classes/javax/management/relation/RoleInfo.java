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


import com.sun.jmx.mbeanserver.GetPropertyAction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.security.AccessController;

import javax.management.MBeanServer;

import javax.management.NotCompliantMBeanException;

/**
 * A RoleInfo object summarises a role in a relation type.
 *
 * <p>The <b>serialVersionUID</b> of this class is {@code 2504952983494636987L}.
 *
 * @since 1.5
 */
@SuppressWarnings("serial")  // serialVersionUID not constant
public class RoleInfo implements Serializable {

    // Serialization compatibility stuff:
    // Two serial forms are supported in this class. The selected form depends
    // on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form
    private static final long oldSerialVersionUID = 7227256952085334351L;
    //
    // Serial version for new serial form
    private static final long newSerialVersionUID = 2504952983494636987L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields =
    {
      new ObjectStreamField("myName", String.class),
      new ObjectStreamField("myIsReadableFlg", boolean.class),
      new ObjectStreamField("myIsWritableFlg", boolean.class),
      new ObjectStreamField("myDescription", String.class),
      new ObjectStreamField("myMinDegree", int.class),
      new ObjectStreamField("myMaxDegree", int.class),
      new ObjectStreamField("myRefMBeanClassName", String.class)
    };
    //
    // Serializable fields in new serial form
    private static final ObjectStreamField[] newSerialPersistentFields =
    {
      new ObjectStreamField("name", String.class),
      new ObjectStreamField("isReadable", boolean.class),
      new ObjectStreamField("isWritable", boolean.class),
      new ObjectStreamField("description", String.class),
      new ObjectStreamField("minDegree", int.class),
      new ObjectStreamField("maxDegree", int.class),
      new ObjectStreamField("referencedMBeanClassName", String.class)
    };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;
    /**
     * @serialField name String Role name
     * @serialField isReadable boolean Read access mode: {@code true} if role is readable
     * @serialField isWritable boolean Write access mode: {@code true} if role is writable
     * @serialField description String Role description
     * @serialField minDegree int Minimum degree (i.e. minimum number of referenced MBeans in corresponding role)
     * @serialField maxDegree int Maximum degree (i.e. maximum number of referenced MBeans in corresponding role)
     * @serialField referencedMBeanClassName String Name of class of MBean(s) expected to be referenced in corresponding role
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
    // Public constants
    //

    /**
     * To specify an unlimited cardinality.
     */
    public static final int ROLE_CARDINALITY_INFINITY = -1;

    //
    // Private members
    //

    /**
     * @serial Role name
     */
    private String name = null;

    /**
     * @serial Read access mode: {@code true} if role is readable
     */
    private boolean isReadable;

    /**
     * @serial Write access mode: {@code true} if role is writable
     */
    private boolean isWritable;

    /**
     * @serial Role description
     */
    private String description = null;

    /**
     * @serial Minimum degree (i.e. minimum number of referenced MBeans in corresponding role)
     */
    private int minDegree;

    /**
     * @serial Maximum degree (i.e. maximum number of referenced MBeans in corresponding role)
     */
    private int maxDegree;

    /**
     * @serial Name of class of MBean(s) expected to be referenced in corresponding role
     */
    private String referencedMBeanClassName = null;

    //
    // Constructors
    //

    /**
     * Constructor.
     *
     * @param roleName  name of the role.
     * @param mbeanClassName  name of the class of MBean(s) expected to
     * be referenced in corresponding role.  If an MBean <em>M</em> is in
     * this role, then the MBean server must return true for
     * {@link MBeanServer#isInstanceOf isInstanceOf(M, mbeanClassName)}.
     * @param read  flag to indicate if the corresponding role
     * can be read
     * @param write  flag to indicate if the corresponding role
     * can be set
     * @param min  minimum degree for role, i.e. minimum number of
     * MBeans to provide in corresponding role
     * Must be less than or equal to {@code max}.
     * (ROLE_CARDINALITY_INFINITY for unlimited)
     * @param max  maximum degree for role, i.e. maximum number of
     * MBeans to provide in corresponding role
     * Must be greater than or equal to {@code min}
     * (ROLE_CARDINALITY_INFINITY for unlimited)
     * @param descr  description of the role (can be null)
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception InvalidRoleInfoException  if the minimum degree is
     * greater than the maximum degree.
     * @exception ClassNotFoundException As of JMX 1.2, this exception
     * can no longer be thrown.  It is retained in the declaration of
     * this class for compatibility with existing code.
     * @exception NotCompliantMBeanException  if the class mbeanClassName
     * is not a MBean class.
     */
    public RoleInfo(String roleName,
                    String mbeanClassName,
                    boolean read,
                    boolean write,
                    int min,
                    int max,
                    String descr)
    throws IllegalArgumentException,
           InvalidRoleInfoException,
           ClassNotFoundException,
           NotCompliantMBeanException {

        init(roleName,
             mbeanClassName,
             read,
             write,
             min,
             max,
             descr);
        return;
    }

    /**
     * Constructor.
     *
     * @param roleName  name of the role
     * @param mbeanClassName  name of the class of MBean(s) expected to
     * be referenced in corresponding role.  If an MBean <em>M</em> is in
     * this role, then the MBean server must return true for
     * {@link MBeanServer#isInstanceOf isInstanceOf(M, mbeanClassName)}.
     * @param read  flag to indicate if the corresponding role
     * can be read
     * @param write  flag to indicate if the corresponding role
     * can be set
     *
     * <P>Minimum and maximum degrees defaulted to 1.
     * <P>Description of role defaulted to null.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception ClassNotFoundException As of JMX 1.2, this exception
     * can no longer be thrown.  It is retained in the declaration of
     * this class for compatibility with existing code.
     * @exception NotCompliantMBeanException As of JMX 1.2, this
     * exception can no longer be thrown.  It is retained in the
     * declaration of this class for compatibility with existing code.
     */
    public RoleInfo(String roleName,
                    String mbeanClassName,
                    boolean read,
                    boolean write)
    throws IllegalArgumentException,
           ClassNotFoundException,
           NotCompliantMBeanException {

        try {
            init(roleName,
                 mbeanClassName,
                 read,
                 write,
                 1,
                 1,
                 null);
        } catch (InvalidRoleInfoException exc) {
            // OK : Can never happen as the minimum
            //      degree equals the maximum degree.
        }

        return;
    }

    /**
     * Constructor.
     *
     * @param roleName  name of the role
     * @param mbeanClassName  name of the class of MBean(s) expected to
     * be referenced in corresponding role.  If an MBean <em>M</em> is in
     * this role, then the MBean server must return true for
     * {@link MBeanServer#isInstanceOf isInstanceOf(M, mbeanClassName)}.
     *
     * <P>IsReadable and IsWritable defaulted to true.
     * <P>Minimum and maximum degrees defaulted to 1.
     * <P>Description of role defaulted to null.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception ClassNotFoundException As of JMX 1.2, this exception
     * can no longer be thrown.  It is retained in the declaration of
     * this class for compatibility with existing code.
     * @exception NotCompliantMBeanException As of JMX 1.2, this
     * exception can no longer be thrown.  It is retained in the
     * declaration of this class for compatibility with existing code.
      */
    public RoleInfo(String roleName,
                    String mbeanClassName)
    throws IllegalArgumentException,
           ClassNotFoundException,
           NotCompliantMBeanException {

        try {
            init(roleName,
                 mbeanClassName,
                 true,
                 true,
                 1,
                 1,
                 null);
        } catch (InvalidRoleInfoException exc) {
            // OK : Can never happen as the minimum
            //      degree equals the maximum degree.
        }

        return;
    }

    /**
     * Copy constructor.
     *
     * @param roleInfo the {@code RoleInfo} instance to be copied.
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public RoleInfo(RoleInfo roleInfo)
        throws IllegalArgumentException {

        if (roleInfo == null) {
            // Revisit [cebro] Localize message
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        try {
            init(roleInfo.getName(),
                 roleInfo.getRefMBeanClassName(),
                 roleInfo.isReadable(),
                 roleInfo.isWritable(),
                 roleInfo.getMinDegree(),
                 roleInfo.getMaxDegree(),
                 roleInfo.getDescription());
        } catch (InvalidRoleInfoException exc3) {
            // OK : Can never happen as the minimum degree and the maximum
            //      degree were already checked at the time the roleInfo
            //      instance was created.
        }
    }

    //
    // Accessors
    //

    /**
     * Returns the name of the role.
     *
     * @return the name of the role.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns read access mode for the role (true if it is readable).
     *
     * @return true if the role is readable.
     */
    public boolean isReadable() {
        return isReadable;
    }

    /**
     * Returns write access mode for the role (true if it is writable).
     *
     * @return true if the role is writable.
     */
    public boolean isWritable() {
        return isWritable;
    }

    /**
     * Returns description text for the role.
     *
     * @return the description of the role.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns minimum degree for corresponding role reference.
     *
     * @return the minimum degree.
     */
    public int getMinDegree() {
        return minDegree;
    }

    /**
     * Returns maximum degree for corresponding role reference.
     *
     * @return the maximum degree.
     */
    public int getMaxDegree() {
        return maxDegree;
    }

    /**
     * <p>Returns name of type of MBean expected to be referenced in
     * corresponding role.</p>
     *
     * @return the name of the referenced type.
     */
    public String getRefMBeanClassName() {
        return referencedMBeanClassName;
    }

    /**
     * Returns true if the {@code value} parameter is greater than or equal to
     * the expected minimum degree, false otherwise.
     *
     * @param value  the value to be checked
     *
     * @return true if greater than or equal to minimum degree, false otherwise.
     */
    public boolean checkMinDegree(int value) {
        if (value >= ROLE_CARDINALITY_INFINITY &&
            (minDegree == ROLE_CARDINALITY_INFINITY
             || value >= minDegree)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the {@code value} parameter is lower than or equal to
     * the expected maximum degree, false otherwise.
     *
     * @param value  the value to be checked
     *
     * @return true if lower than or equal to maximum degree, false otherwise.
     */
    public boolean checkMaxDegree(int value) {
        if (value >= ROLE_CARDINALITY_INFINITY &&
            (maxDegree == ROLE_CARDINALITY_INFINITY ||
             (value != ROLE_CARDINALITY_INFINITY &&
              value <= maxDegree))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns a string describing the role info.
     *
     * @return a description of the role info.
     */
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("role info name: " + name);
        result.append("; isReadable: " + isReadable);
        result.append("; isWritable: " + isWritable);
        result.append("; description: " + description);
        result.append("; minimum degree: " + minDegree);
        result.append("; maximum degree: " + maxDegree);
        result.append("; MBean class: " + referencedMBeanClassName);
        return result.toString();
    }

    //
    // Misc
    //

    // Initialization
    private void init(String roleName,
                      String mbeanClassName,
                      boolean read,
                      boolean write,
                      int min,
                      int max,
                      String descr)
            throws IllegalArgumentException,
                   InvalidRoleInfoException {

        if (roleName == null ||
            mbeanClassName == null) {
            // Revisit [cebro] Localize message
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        name = roleName;
        isReadable = read;
        isWritable = write;
        if (descr != null) {
            description = descr;
        }

        boolean invalidRoleInfoFlg = false;
        StringBuilder excMsgStrB = new StringBuilder();
        if (max != ROLE_CARDINALITY_INFINITY &&
            (min == ROLE_CARDINALITY_INFINITY ||
             min > max)) {
            // Revisit [cebro] Localize message
            excMsgStrB.append("Minimum degree ");
            excMsgStrB.append(min);
            excMsgStrB.append(" is greater than maximum degree ");
            excMsgStrB.append(max);
            invalidRoleInfoFlg = true;

        } else if (min < ROLE_CARDINALITY_INFINITY ||
                   max < ROLE_CARDINALITY_INFINITY) {
            // Revisit [cebro] Localize message
            excMsgStrB.append("Minimum or maximum degree has an illegal value, must be [0, ROLE_CARDINALITY_INFINITY].");
            invalidRoleInfoFlg = true;
        }
        if (invalidRoleInfoFlg) {
            throw new InvalidRoleInfoException(excMsgStrB.toString());
        }
        minDegree = min;
        maxDegree = max;

        referencedMBeanClassName = mbeanClassName;

        return;
    }

    /**
     * Deserializes a {@link RoleInfo} from an {@link ObjectInputStream}.
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
        isReadable = fields.get("myIsReadableFlg", false);
        if (fields.defaulted("myIsReadableFlg"))
        {
          throw new NullPointerException("myIsReadableFlg");
        }
        isWritable = fields.get("myIsWritableFlg", false);
        if (fields.defaulted("myIsWritableFlg"))
        {
          throw new NullPointerException("myIsWritableFlg");
        }
        description = (String) fields.get("myDescription", null);
        if (fields.defaulted("myDescription"))
        {
          throw new NullPointerException("myDescription");
        }
        minDegree = fields.get("myMinDegree", 0);
        if (fields.defaulted("myMinDegree"))
        {
          throw new NullPointerException("myMinDegree");
        }
        maxDegree = fields.get("myMaxDegree", 0);
        if (fields.defaulted("myMaxDegree"))
        {
          throw new NullPointerException("myMaxDegree");
        }
        referencedMBeanClassName = (String) fields.get("myRefMBeanClassName", null);
        if (fields.defaulted("myRefMBeanClassName"))
        {
          throw new NullPointerException("myRefMBeanClassName");
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
     * Serializes a {@link RoleInfo} to an {@link ObjectOutputStream}.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
      if (compat)
      {
        // Serializes this instance in the old serial form
        //
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("myName", name);
        fields.put("myIsReadableFlg", isReadable);
        fields.put("myIsWritableFlg", isWritable);
        fields.put("myDescription", description);
        fields.put("myMinDegree", minDegree);
        fields.put("myMaxDegree", maxDegree);
        fields.put("myRefMBeanClassName", referencedMBeanClassName);
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
