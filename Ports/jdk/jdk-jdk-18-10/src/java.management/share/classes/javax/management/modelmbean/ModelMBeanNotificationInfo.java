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

import static com.sun.jmx.defaults.JmxProperties.MODELMBEAN_LOGGER;
import com.sun.jmx.mbeanserver.GetPropertyAction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.security.AccessController;
import java.lang.System.Logger.Level;

import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.MBeanNotificationInfo;
import javax.management.RuntimeOperationsException;

/**
 * <p>The ModelMBeanNotificationInfo object describes a notification emitted
 * by a ModelMBean.
 * It is a subclass of MBeanNotificationInfo with the addition of an
 * associated Descriptor and an implementation of the Descriptor interface.</p>
 *
 * <P id="descriptor">
 * The fields in the descriptor are defined, but not limited to, the following.
 * Note that when the Type in this table is Number, a String that is the decimal
 * representation of a Long can also be used.</P>
 *
 * <table class="striped">
 * <caption style="display:none">ModelMBeanNotificationInfo Fields</caption>
 * <thead>
 * <tr><th scope="col">Name</th><th scope="col">Type</th><th scope="col">Meaning</th></tr>
 * </thead>
 * <tbody style="text-align:left">
 * <tr><th scope="row">name</th><td>String</td>
 *     <td>Notification name.</td></tr>
 * <tr><th scope="row">descriptorType</th><td>String</td>
 *     <td>Must be "notification".</td></tr>
 * <tr><th scope="row">severity</th><td>Number</td>
 *     <td>0-6 where 0: unknown; 1: non-recoverable;
 *         2: critical, failure; 3: major, severe;
 *         4: minor, marginal, error; 5: warning;
 *         6: normal, cleared, informative</td></tr>
 * <tr><th scope="row">messageID</th><td>String</td>
 *     <td>Unique key for message text (to allow translation, analysis).</td></tr>
 * <tr><th scope="row">messageText</th><td>String</td>
 *     <td>Text of notification.</td></tr>
 * <tr><th scope="row">log</th><td>String</td>
 *     <td>T - log message, F - do not log message.</td></tr>
 * <tr><th scope="row">logfile</th><td>String</td>
 *     <td>fully qualified file name appropriate for operating system.</td></tr>
 * <tr><th scope="row">visibility</th><td>Number</td>
 *     <td>1-4 where 1: always visible 4: rarely visible.</td></tr>
 * <tr><th scope="row">presentationString</th><td>String</td>
 *     <td>XML formatted string to allow presentation of data.</td></tr>
 * </tbody>
 * </table>
 *
 * <p>The default descriptor contains the name, descriptorType,
 * displayName and severity(=6) fields.  The default value of the name
 * and displayName fields is the name of the Notification class (as
 * specified by the <code>name</code> parameter of the
 * ModelMBeanNotificationInfo constructor).</p>
 *
 * <p>The <b>serialVersionUID</b> of this class is <code>-7445681389570207141L</code>.
 *
 * @since 1.5
 */

@SuppressWarnings("serial")  // serialVersionUID is not constant
public class ModelMBeanNotificationInfo
    extends MBeanNotificationInfo
    implements DescriptorAccess {

    // Serialization compatibility stuff:
    // Two serial forms are supported in this class. The selected form
    // depends on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form
    private static final long oldSerialVersionUID = -5211564525059047097L;
    //
    // Serial version for new serial form
    private static final long newSerialVersionUID = -7445681389570207141L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields =
    {
      new ObjectStreamField("notificationDescriptor", Descriptor.class),
      new ObjectStreamField("currClass", String.class)
    };
    //
    // Serializable fields in new serial form
    private static final ObjectStreamField[] newSerialPersistentFields =
    {
      new ObjectStreamField("notificationDescriptor", Descriptor.class)
    };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;
    /**
     * @serialField notificationDescriptor Descriptor The descriptor
     *   containing the appropriate metadata for this instance
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
     * @serial The descriptor containing the appropriate metadata for
     *         this instance
     */
    private Descriptor notificationDescriptor;

    private static final String currClass = "ModelMBeanNotificationInfo";

    /**
     * Constructs a ModelMBeanNotificationInfo object with a default
     * descriptor.
     *
     * @param notifTypes The array of strings (in dot notation) containing
     *     the notification types that may be emitted.
     * @param name The name of the Notification class.
     * @param description A human readable description of the
     *     Notification. Optional.
     **/
    public ModelMBeanNotificationInfo(String[] notifTypes,
                                      String name,
                                      String description) {
        this(notifTypes,name,description,null);
    }

    /**
     * Constructs a ModelMBeanNotificationInfo object.
     *
     * @param notifTypes The array of strings (in dot notation)
     *        containing the notification types that may be emitted.
     * @param name The name of the Notification class.
     * @param description A human readable description of the Notification.
     *        Optional.
     * @param descriptor An instance of Descriptor containing the
     *        appropriate metadata for this instance of the
     *        MBeanNotificationInfo. If it is null a default descriptor
     *        will be created. If the descriptor does not contain the
     *        fields "displayName" or "severity",
     *        the missing ones are added with their default values.
     *
     * @exception RuntimeOperationsException Wraps an
     *    {@link IllegalArgumentException}. The descriptor is invalid, or
     *    descriptor field "name" is not equal to parameter name, or
     *    descriptor field "descriptorType" is not equal to "notification".
     *
     **/
    public ModelMBeanNotificationInfo(String[] notifTypes,
                                      String name,
                                      String description,
                                      Descriptor descriptor) {
        super(notifTypes, name, description);
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        notificationDescriptor = validDescriptor(descriptor);
    }

    /**
     * Constructs a new ModelMBeanNotificationInfo object from this
     * ModelMBeanNotfication Object.
     *
     * @param inInfo the ModelMBeanNotificationInfo to be duplicated
     *
     **/
    public ModelMBeanNotificationInfo(ModelMBeanNotificationInfo inInfo) {
        this(inInfo.getNotifTypes(),
             inInfo.getName(),
             inInfo.getDescription(),inInfo.getDescriptor());
    }

    /**
     * Creates and returns a new ModelMBeanNotificationInfo which is a
     * duplicate of this ModelMBeanNotificationInfo.
     **/
    public Object clone () {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        return(new ModelMBeanNotificationInfo(this));
    }

    /**
     * Returns a copy of the associated Descriptor for the
     * ModelMBeanNotificationInfo.
     *
     * @return Descriptor associated with the
     * ModelMBeanNotificationInfo object.
     *
     * @see #setDescriptor
     **/
    public Descriptor getDescriptor() {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (notificationDescriptor == null) {
            // Dead code. Should never happen.
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "Descriptor value is null, " +
                        "setting descriptor to default values");
            }
            notificationDescriptor = validDescriptor(null);
        }

        return((Descriptor)notificationDescriptor.clone());
    }

    /**
     * Sets associated Descriptor (full replace) for the
     * ModelMBeanNotificationInfo If the new Descriptor is null,
     * then the associated Descriptor reverts to a default
     * descriptor.  The Descriptor is validated before it is
     * assigned.  If the new Descriptor is invalid, then a
     * RuntimeOperationsException wrapping an
     * IllegalArgumentException is thrown.
     *
     * @param inDescriptor replaces the Descriptor associated with the
     * ModelMBeanNotification interface
     *
     * @exception RuntimeOperationsException Wraps an
     * {@link IllegalArgumentException} for invalid Descriptor.
     *
     * @see #getDescriptor
     **/
    public void setDescriptor(Descriptor inDescriptor) {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        notificationDescriptor = validDescriptor(inDescriptor);
    }

    /**
     * Returns a human readable string containing
     * ModelMBeanNotificationInfo.
     *
     * @return a string describing this object.
     **/
    public String toString() {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        final StringBuilder retStr = new StringBuilder();

        retStr.append("ModelMBeanNotificationInfo: ")
            .append(this.getName());

        retStr.append(" ; Description: ")
            .append(this.getDescription());

        retStr.append(" ; Descriptor: ")
            .append(this.getDescriptor());

        retStr.append(" ; Types: ");
        String[] nTypes = this.getNotifTypes();
        for (int i=0; i < nTypes.length; i++) {
            if (i > 0) retStr.append(", ");
            retStr.append(nTypes[i]);
        }
        return retStr.toString();
    }


    /**
     * Clones the passed in Descriptor, sets default values, and checks for validity.
     * If the Descriptor is invalid (for instance by having the wrong "name"),
     * this indicates programming error and a RuntimeOperationsException will be thrown.
     *
     * The following fields will be defaulted if they are not already set:
     * descriptorType="notification",displayName=this.getName(),
     * name=this.getName(),severity="6"
     *
     *
     * @param in Descriptor to be checked, or null which is equivalent to an
     * empty Descriptor.
     * @exception RuntimeOperationsException if Descriptor is invalid
     */
    private Descriptor validDescriptor(final Descriptor in) throws RuntimeOperationsException {
        Descriptor clone;
        boolean defaulted = (in == null);
        if (defaulted) {
            clone = new DescriptorSupport();
            MODELMBEAN_LOGGER.log(Level.TRACE, "Null Descriptor, creating new.");
        } else {
            clone = (Descriptor) in.clone();
        }

        //Setting defaults.
        if (defaulted && clone.getFieldValue("name")==null) {
            clone.setField("name", this.getName());
            MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor name to " + this.getName());
        }
        if (defaulted && clone.getFieldValue("descriptorType")==null) {
            clone.setField("descriptorType", "notification");
            MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting descriptorType to \"notification\"");
        }
        if (clone.getFieldValue("displayName") == null) {
            clone.setField("displayName",this.getName());
            MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor displayName to " + this.getName());
        }
        if (clone.getFieldValue("severity") == null) {
            clone.setField("severity", "6");
            MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor severity field to 6");
        }

        //Checking validity
        if (!clone.isValid()) {
             throw new RuntimeOperationsException(new IllegalArgumentException("Invalid Descriptor argument"),
                "The isValid() method of the Descriptor object itself returned false,"+
                "one or more required fields are invalid. Descriptor:" + clone.toString());
        }
        if (!getName().equalsIgnoreCase((String) clone.getFieldValue("name"))) {
                throw new RuntimeOperationsException(new IllegalArgumentException("Invalid Descriptor argument"),
                "The Descriptor \"name\" field does not match the object described. " +
                 " Expected: "+ this.getName() + " , was: " + clone.getFieldValue("name"));
        }
        if (!"notification".equalsIgnoreCase((String) clone.getFieldValue("descriptorType"))) {
                 throw new RuntimeOperationsException(new IllegalArgumentException("Invalid Descriptor argument"),
                "The Descriptor \"descriptorType\" field does not match the object described. " +
                 " Expected: \"notification\" ," + " was: " + clone.getFieldValue("descriptorType"));
        }

        return clone;
    }


    /**
     * Deserializes a {@link ModelMBeanNotificationInfo} from an
     * {@link ObjectInputStream}.
     **/
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // New serial form ignores extra field "currClass"
        in.defaultReadObject();
    }


    /**
     * Serializes a {@link ModelMBeanNotificationInfo} to an
     * {@link ObjectOutputStream}.
     **/
    private void writeObject(ObjectOutputStream out)
        throws IOException {
        if (compat) {
            // Serializes this instance in the old serial form
            //
            ObjectOutputStream.PutField fields = out.putFields();
            fields.put("notificationDescriptor", notificationDescriptor);
            fields.put("currClass", currClass);
            out.writeFields();
        } else {
            // Serializes this instance in the new serial form
            //
            out.defaultWriteObject();
        }
    }

}
