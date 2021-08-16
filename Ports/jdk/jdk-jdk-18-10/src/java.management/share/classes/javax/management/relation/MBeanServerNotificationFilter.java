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
import static com.sun.jmx.defaults.JmxProperties.RELATION_LOGGER;
import com.sun.jmx.mbeanserver.GetPropertyAction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.security.AccessController;

import java.util.List;
import java.util.Vector;

import javax.management.MBeanServerNotification;

import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;

import java.util.List;
import java.lang.System.Logger.Level;
import java.util.Vector;

/**
 * Filter for {@link MBeanServerNotification}.
 * This filter filters MBeanServerNotification notifications by
 * selecting the ObjectNames of interest and the operations (registration,
 * unregistration, both) of interest (corresponding to notification
 * types).
 *
 * <p>The <b>serialVersionUID</b> of this class is <code>2605900539589789736L</code>.
 *
 * @since 1.5
 */
@SuppressWarnings("serial")  // serialVersionUID must be constant
public class MBeanServerNotificationFilter extends NotificationFilterSupport {

    // Serialization compatibility stuff:
    // Two serial forms are supported in this class. The selected form depends
    // on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form
    private static final long oldSerialVersionUID = 6001782699077323605L;
    //
    // Serial version for new serial form
    private static final long newSerialVersionUID = 2605900539589789736L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields =
    {
      new ObjectStreamField("mySelectObjNameList", Vector.class),
      new ObjectStreamField("myDeselectObjNameList", Vector.class)
    };
    //
    // Serializable fields in new serial form
    private static final ObjectStreamField[] newSerialPersistentFields =
    {
      new ObjectStreamField("selectedNames", List.class),
      new ObjectStreamField("deselectedNames", List.class)
    };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;
    /**
     * @serialField selectedNames List List of {@link ObjectName}s of interest
     *         <ul>
     *         <li><code>null</code> means that all {@link ObjectName}s are implicitly selected
     *         (check for explicit deselections)</li>
     *         <li>Empty vector means that no {@link ObjectName} is explicitly selected</li>
     *         </ul>
     * @serialField deselectedNames List List of {@link ObjectName}s with no interest
     *         <ul>
     *         <li><code>null</code> means that all {@link ObjectName}s are implicitly deselected
     *         (check for explicit selections))</li>
     *         <li>Empty vector means that no {@link ObjectName} is explicitly deselected</li>
     *         </ul>
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
     * @serial List of {@link ObjectName}s of interest
     *         <ul>
     *         <li><code>null</code> means that all {@link ObjectName}s are implicitly selected
     *         (check for explicit deselections)</li>
     *         <li>Empty vector means that no {@link ObjectName} is explicitly selected</li>
     *         </ul>
     */
    private List<ObjectName> selectedNames = new Vector<ObjectName>();

    /**
     * @serial List of {@link ObjectName}s with no interest
     *         <ul>
     *         <li><code>null</code> means that all {@link ObjectName}s are implicitly deselected
     *         (check for explicit selections))</li>
     *         <li>Empty vector means that no {@link ObjectName} is explicitly deselected</li>
     *         </ul>
     */
    private List<ObjectName> deselectedNames = null;

    //
    // Constructor
    //

    /**
     * Creates a filter selecting all MBeanServerNotification notifications for
     * all ObjectNames.
     */
    public MBeanServerNotificationFilter() {

        super();
        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        enableType(MBeanServerNotification.REGISTRATION_NOTIFICATION);
        enableType(MBeanServerNotification.UNREGISTRATION_NOTIFICATION);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    //
    // Accessors
    //

    /**
     * Disables any MBeanServerNotification (all ObjectNames are
     * deselected).
     */
    public synchronized void disableAllObjectNames() {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        selectedNames = new Vector<ObjectName>();
        deselectedNames = null;

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Disables MBeanServerNotifications concerning given ObjectName.
     *
     * @param objectName  ObjectName no longer of interest
     *
     * @exception IllegalArgumentException  if the given ObjectName is null
     */
    public synchronized void disableObjectName(ObjectName objectName)
        throws IllegalArgumentException {

        if (objectName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}" + objectName);

        // Removes from selected ObjectNames, if present
        if (selectedNames != null) {
            if (selectedNames.size() != 0) {
                selectedNames.remove(objectName);
            }
        }

        // Adds it in deselected ObjectNames
        if (deselectedNames != null) {
            // If all are deselected, no need to do anything :)
            if (!(deselectedNames.contains(objectName))) {
                // ObjectName was not already deselected
                deselectedNames.add(objectName);
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Enables all MBeanServerNotifications (all ObjectNames are selected).
     */
    public synchronized void enableAllObjectNames() {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        selectedNames = null;
        deselectedNames = new Vector<ObjectName>();

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Enables MBeanServerNotifications concerning given ObjectName.
     *
     * @param objectName  ObjectName of interest
     *
     * @exception IllegalArgumentException  if the given ObjectName is null
     */
    public synchronized void enableObjectName(ObjectName objectName)
        throws IllegalArgumentException {

        if (objectName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", objectName);

        // Removes from deselected ObjectNames, if present
        if (deselectedNames != null) {
            if (deselectedNames.size() != 0) {
                deselectedNames.remove(objectName);
            }
        }

        // Adds it in selected ObjectNames
        if (selectedNames != null) {
            // If all are selected, no need to do anything :)
            if (!(selectedNames.contains(objectName))) {
                // ObjectName was not already selected
                selectedNames.add(objectName);
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Gets all the ObjectNames enabled.
     *
     * @return Vector of ObjectNames:
     * <P>- null means all ObjectNames are implicitly selected, except the
     * ObjectNames explicitly deselected
     * <P>- empty means all ObjectNames are deselected, i.e. no ObjectName
     * selected.
     */
    public synchronized Vector<ObjectName> getEnabledObjectNames() {
        if (selectedNames != null) {
            return new Vector<ObjectName>(selectedNames);
        } else {
            return null;
        }
    }

    /**
     * Gets all the ObjectNames disabled.
     *
     * @return Vector of ObjectNames:
     * <P>- null means all ObjectNames are implicitly deselected, except the
     * ObjectNames explicitly selected
     * <P>- empty means all ObjectNames are selected, i.e. no ObjectName
     * deselected.
     */
    public synchronized Vector<ObjectName> getDisabledObjectNames() {
        if (deselectedNames != null) {
            return new Vector<ObjectName>(deselectedNames);
        } else {
            return null;
        }
    }

    //
    // NotificationFilter interface
    //

    /**
     * Invoked before sending the specified notification to the listener.
     * <P>If:
     * <P>- the ObjectName of the concerned MBean is selected (explicitly OR
     * (implicitly and not explicitly deselected))
     * <P>AND
     * <P>- the type of the operation (registration or unregistration) is
     * selected
     * <P>then the notification is sent to the listener.
     *
     * @param notif  The notification to be sent.
     *
     * @return true if the notification has to be sent to the listener, false
     * otherwise.
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public synchronized boolean isNotificationEnabled(Notification notif)
        throws IllegalArgumentException {

        if (notif == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", notif);

        // Checks the type first
        String ntfType = notif.getType();
        Vector<String> enabledTypes = getEnabledTypes();
        if (!(enabledTypes.contains(ntfType))) {
            RELATION_LOGGER.log(Level.TRACE,
                    "Type not selected, exiting");
            return false;
        }

        // We have a MBeanServerNotification: downcasts it
        MBeanServerNotification mbsNtf = (MBeanServerNotification)notif;

        // Checks the ObjectName
        ObjectName objName = mbsNtf.getMBeanName();
        // Is it selected?
        boolean isSelectedFlg = false;
        if (selectedNames != null) {
            // Not all are implicitly selected:
            // checks for explicit selection
            if (selectedNames.size() == 0) {
                // All are explicitly not selected
                RELATION_LOGGER.log(Level.TRACE,
                        "No ObjectNames selected, exiting");
                return false;
            }

            isSelectedFlg = selectedNames.contains(objName);
            if (!isSelectedFlg) {
                // Not in the explicit selected list
                RELATION_LOGGER.log(Level.TRACE,
                        "ObjectName not in selected list, exiting");
                return false;
            }
        }

        if (!isSelectedFlg) {
            // Not explicitly selected: is it deselected?

            if (deselectedNames == null) {
                // All are implicitly deselected and it is not explicitly
                // selected
                RELATION_LOGGER.log(Level.TRACE,
                        "ObjectName not selected, and all " +
                        "names deselected, exiting");
                return false;

            } else if (deselectedNames.contains(objName)) {
                // Explicitly deselected
                RELATION_LOGGER.log(Level.TRACE,
                        "ObjectName explicitly not selected, exiting");
                return false;
            }
        }

        RELATION_LOGGER.log(Level.TRACE,
                "ObjectName selected, exiting");
        return true;
    }


    /**
     * Deserializes an {@link MBeanServerNotificationFilter} from an {@link ObjectInputStream}.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
      if (compat)
      {
        // Read an object serialized in the old serial form
        //
        ObjectInputStream.GetField fields = in.readFields();
        selectedNames = cast(fields.get("mySelectObjNameList", null));
        if (fields.defaulted("mySelectObjNameList"))
        {
          throw new NullPointerException("mySelectObjNameList");
        }
        deselectedNames = cast(fields.get("myDeselectObjNameList", null));
        if (fields.defaulted("myDeselectObjNameList"))
        {
          throw new NullPointerException("myDeselectObjNameList");
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
     * Serializes an {@link MBeanServerNotificationFilter} to an {@link ObjectOutputStream}.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
      if (compat)
      {
        // Serializes this instance in the old serial form
        //
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("mySelectObjNameList", selectedNames);
        fields.put("myDeselectObjNameList", deselectedNames);
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
