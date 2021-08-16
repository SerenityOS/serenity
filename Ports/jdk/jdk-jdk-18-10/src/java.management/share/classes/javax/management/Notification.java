/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.EventObject;

import java.security.AccessController;

import com.sun.jmx.mbeanserver.GetPropertyAction;

/**
 * <p>The Notification class represents a notification emitted by an
 * MBean.  It contains a reference to the source MBean: if the
 * notification has been forwarded through the MBean server, and the
 * original source of the notification was a reference to the emitting
 * MBean object, then the MBean server replaces it by the MBean's
 * ObjectName.  If the listener has registered directly with the
 * MBean, this is either the object name or a direct reference to the
 * MBean.</p>
 *
 * <p>It is strongly recommended that notification senders use the
 * object name rather than a reference to the MBean object as the
 * source.</p>
 *
 * <p>The <b>serialVersionUID</b> of this class is <code>-7516092053498031989L</code>.
 *
 * @since 1.5
 */
@SuppressWarnings("serial")  // serialVersionUID is not constant
public class Notification extends EventObject {

    // Serialization compatibility stuff:
    // Two serial forms are supported in this class. The selected form depends
    // on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form
    private static final long oldSerialVersionUID = 1716977971058914352L;
    //
    // Serial version for new serial form
    private static final long newSerialVersionUID = -7516092053498031989L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields =
    {
        new ObjectStreamField("message", String.class),
        new ObjectStreamField("sequenceNumber", Long.TYPE),
        new ObjectStreamField("source", Object.class),
        new ObjectStreamField("sourceObjectName", ObjectName.class),
        new ObjectStreamField("timeStamp", Long.TYPE),
        new ObjectStreamField("type", String.class),
        new ObjectStreamField("userData", Object.class)
    };
    //
    // Serializable fields in new serial form
    private static final ObjectStreamField[] newSerialPersistentFields =
    {
        new ObjectStreamField("message", String.class),
        new ObjectStreamField("sequenceNumber", Long.TYPE),
        new ObjectStreamField("source", Object.class),
        new ObjectStreamField("timeStamp", Long.TYPE),
        new ObjectStreamField("type", String.class),
        new ObjectStreamField("userData", Object.class)
    };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;
    /**
     * @serialField type String The notification type.
     *              A string expressed in a dot notation similar to Java properties.
     *              An example of a notification type is network.alarm.router
     * @serialField sequenceNumber long The notification sequence number.
     *              A serial number which identify particular instance
     *              of notification in the context of the notification source.
     * @serialField timeStamp long The notification timestamp.
     *              Indicating when the notification was generated
     * @serialField userData Object The notification user data.
     *              Used for whatever other data the notification
     *              source wishes to communicate to its consumers
     * @serialField message String The notification message.
     * @serialField source Object The object on which the notification initially occurred.
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
            // OK: exception means no compat with 1.0, too bad
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
     * @serial The notification type.
     *         A string expressed in a dot notation similar to Java properties.
     *         An example of a notification type is network.alarm.router
     */
    private String type;

    /**
     * @serial The notification sequence number.
     *         A serial number which identify particular instance
     *         of notification in the context of the notification source.
     */
    private long sequenceNumber;

    /**
     * @serial The notification timestamp.
     *         Indicating when the notification was generated
     */
    private long timeStamp;

    /**
     * @serial The notification user data.
     *         Used for whatever other data the notification
     *         source wishes to communicate to its consumers
     */
    private Object userData = null;

    /**
     * @serial The notification message.
     */
    private String message  = "";

    /**
     * <p>This field hides the {@link EventObject#source} field in the
     * parent class to make it non-transient and therefore part of the
     * serialized form.</p>
     *
     * @serial The object on which the notification initially occurred.
     */
    protected Object source = null;


    /**
     * Creates a Notification object.
     * The notification timeStamp is set to the current date.
     *
     * @param type The notification type.
     * @param source The notification source.
     * @param sequenceNumber The notification sequence number within the source object.
     *
     */
    public Notification(String type, Object source, long sequenceNumber) {
        super (source) ;
        this.source = source;
        this.type = type;
        this.sequenceNumber = sequenceNumber ;
        this.timeStamp = (new java.util.Date()).getTime() ;
    }

    /**
     * Creates a Notification object.
     * The notification timeStamp is set to the current date.
     *
     * @param type The notification type.
     * @param source The notification source.
     * @param sequenceNumber The notification sequence number within the source object.
     * @param message The detailed message.
     *
     */
    public Notification(String type, Object source, long sequenceNumber, String message) {
        super (source) ;
        this.source = source;
        this.type = type;
        this.sequenceNumber = sequenceNumber ;
        this.timeStamp = (new java.util.Date()).getTime() ;
        this.message = message ;
    }

    /**
     * Creates a Notification object.
     *
     * @param type The notification type.
     * @param source The notification source.
     * @param sequenceNumber The notification sequence number within the source object.
     * @param timeStamp The notification emission date.
     *
     */
    public Notification(String type, Object source, long sequenceNumber, long timeStamp) {
        super (source) ;
        this.source = source;
        this.type = type ;
        this.sequenceNumber = sequenceNumber ;
        this.timeStamp = timeStamp ;
    }

    /**
     * Creates a Notification object.
     *
     * @param type The notification type.
     * @param source The notification source.
     * @param sequenceNumber The notification sequence number within the source object.
     * @param timeStamp The notification emission date.
     * @param message The detailed message.
     *
     */
    public Notification(String type, Object source, long sequenceNumber, long timeStamp, String message) {
        super (source) ;
        this.source = source;
        this.type = type ;
        this.sequenceNumber = sequenceNumber ;
        this.timeStamp = timeStamp ;
        this.message = message ;
    }

    /**
     * Sets the source.
     *
     * @param source the new source for this object.
     *
     * @see EventObject#getSource
     */
    public void setSource(Object source) {
        super.source = source;
        this.source = source;
    }

    /**
     * Get the notification sequence number.
     *
     * @return The notification sequence number within the source object. It's a serial number
     * identifying a particular instance of notification in the context of the notification source.
     * The notification model does not assume that notifications will be received in the same order
     * that they are sent. The sequence number helps listeners to sort received notifications.
     *
     * @see #setSequenceNumber
     */
    public long getSequenceNumber() {
        return sequenceNumber ;
    }

    /**
     * Set the notification sequence number.
     *
     * @param sequenceNumber The notification sequence number within the source object. It is
     * a serial number identifying a particular instance of notification in the
     * context of the notification source.
     *
     * @see #getSequenceNumber
     */
    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Get the notification type.
     *
     * @return The notification type. It's a string expressed in a dot notation
     * similar to Java properties. It is recommended that the notification type
     * should follow the reverse-domain-name convention used by Java package
     * names.  An example of a notification type is com.example.alarm.router.
     */
    public String getType() {
        return type ;
    }

    /**
     * Get the notification timestamp.
     *
     * @return The notification timestamp.
     *
     * @see #setTimeStamp
     */
    public long getTimeStamp() {
        return timeStamp ;
    }

    /**
     * Set the notification timestamp.
     *
     * @param timeStamp The notification timestamp. It indicates when the notification was generated.
     *
     * @see #getTimeStamp
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Get the notification message.
     *
     * @return The message string of this notification object.
     *
     */
    public String getMessage() {
        return message ;
    }

    /**
     * Get the user data.
     *
     * @return The user data object. It is used for whatever data
     * the notification source wishes to communicate to its consumers.
     *
     * @see #setUserData
     */
    public Object getUserData() {
        return userData ;
    }

    /**
     * Set the user data.
     *
     * @param userData The user data object. It is used for whatever data
     * the notification source wishes to communicate to its consumers.
     *
     * @see #getUserData
     */
    public void setUserData(Object userData) {

        this.userData = userData ;
    }

    /**
     * Returns a String representation of this notification.
     *
     * @return A String representation of this notification.
     */
    @Override
    public String toString() {
        return super.toString()+"[type="+type+"][message="+message+"]";
    }

    /**
     * Deserializes a {@link Notification} from an {@link ObjectInputStream}.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
      // New serial form ignores extra field "sourceObjectName"
      in.defaultReadObject();
      super.source = source;
    }


    /**
     * Serializes a {@link Notification} to an {@link ObjectOutputStream}.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
        if (compat) {
            // Serializes this instance in the old serial form
            //
            ObjectOutputStream.PutField fields = out.putFields();
            fields.put("type", type);
            fields.put("sequenceNumber", sequenceNumber);
            fields.put("timeStamp", timeStamp);
            fields.put("userData", userData);
            fields.put("message", message);
            fields.put("source", source);
            out.writeFields();
        } else {
            // Serializes this instance in the new serial form
            //
            out.defaultWriteObject();
        }
    }
}
