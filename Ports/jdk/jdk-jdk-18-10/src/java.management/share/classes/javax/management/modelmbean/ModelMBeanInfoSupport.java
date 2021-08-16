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
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.RuntimeOperationsException;

/**
 * This class represents the meta data for ModelMBeans.  Descriptors have been
 * added on the meta data objects.
 * <P>
 * Java resources wishing to be manageable instantiate the ModelMBean using the
 * MBeanServer's createMBean method.  The resource then sets the ModelMBeanInfo
 * and Descriptors for the ModelMBean instance. The attributes and operations
 * exposed via the ModelMBeanInfo for the ModelMBean are accessible
 * from MBeans, connectors/adaptors like other MBeans. Through the Descriptors,
 * values and methods in the managed application can be defined and mapped to
 * attributes and operations of the ModelMBean.
 * This mapping can be defined during development in a file or dynamically and
 * programmatically at runtime.
 * <P>
 * Every ModelMBean which is instantiated in the MBeanServer becomes manageable:
 * its attributes and operations
 * become remotely accessible through the connectors/adaptors connected to that
 * MBeanServer.
 * A Java object cannot be registered in the MBeanServer unless it is a JMX
 * compliant MBean.
 * By instantiating a ModelMBean, resources are guaranteed that the MBean is
 * valid.
 *
 * MBeanException and RuntimeOperationsException must be thrown on every public
 * method.  This allows for wrapping exceptions from distributed
 * communications (RMI, EJB, etc.)
 *
 * <p>The <b>serialVersionUID</b> of this class is
 * <code>-1935722590756516193L</code>.
 *
 * @since 1.5
 */
@SuppressWarnings("serial")
public class ModelMBeanInfoSupport extends MBeanInfo implements ModelMBeanInfo {

    // Serialization compatibility stuff:
    // Two serial forms are supported in this class. The selected form depends
    // on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form
    private static final long oldSerialVersionUID = -3944083498453227709L;
    //
    // Serial version for new serial form
    private static final long newSerialVersionUID = -1935722590756516193L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields =
    {
        new ObjectStreamField("modelMBeanDescriptor", Descriptor.class),
                new ObjectStreamField("mmbAttributes", MBeanAttributeInfo[].class),
                new ObjectStreamField("mmbConstructors", MBeanConstructorInfo[].class),
                new ObjectStreamField("mmbNotifications", MBeanNotificationInfo[].class),
                new ObjectStreamField("mmbOperations", MBeanOperationInfo[].class),
                new ObjectStreamField("currClass", String.class)
    };
    //
    // Serializable fields in new serial form
    private static final ObjectStreamField[] newSerialPersistentFields =
    {
        new ObjectStreamField("modelMBeanDescriptor", Descriptor.class),
                new ObjectStreamField("modelMBeanAttributes", MBeanAttributeInfo[].class),
                new ObjectStreamField("modelMBeanConstructors", MBeanConstructorInfo[].class),
                new ObjectStreamField("modelMBeanNotifications", MBeanNotificationInfo[].class),
                new ObjectStreamField("modelMBeanOperations", MBeanOperationInfo[].class)
    };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;
    /**
     * @serialField modelMBeanDescriptor Descriptor The descriptor containing
     *              MBean wide policy
     * @serialField modelMBeanAttributes ModelMBeanAttributeInfo[] The array of
     *              {@link ModelMBeanAttributeInfo} objects which
     *              have descriptors
     * @serialField modelMBeanConstructors MBeanConstructorInfo[] The array of
     *              {@link ModelMBeanConstructorInfo} objects which
     *              have descriptors
     * @serialField modelMBeanNotifications MBeanNotificationInfo[] The array of
     *              {@link ModelMBeanNotificationInfo} objects which
     *              have descriptors
     * @serialField modelMBeanOperations MBeanOperationInfo[] The array of
     *              {@link ModelMBeanOperationInfo} objects which
     *              have descriptors
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
     * @serial The descriptor containing MBean wide policy
     */
    private Descriptor modelMBeanDescriptor = null;

    /* The following fields always have the same values as the
       fields inherited from MBeanInfo and are retained only for
       compatibility.  By rewriting the serialization code we could
       get rid of them.

       These fields can't be final because they are assigned to by
       readObject().  */

    /**
     * @serial The array of {@link ModelMBeanAttributeInfo} objects which
     *         have descriptors
     */
    private MBeanAttributeInfo[] modelMBeanAttributes;

    /**
     * @serial The array of {@link ModelMBeanConstructorInfo} objects which
     *         have descriptors
     */
    private MBeanConstructorInfo[] modelMBeanConstructors;

    /**
     * @serial The array of {@link ModelMBeanNotificationInfo} objects which
     *         have descriptors
     */
    private MBeanNotificationInfo[] modelMBeanNotifications;

    /**
     * @serial The array of {@link ModelMBeanOperationInfo} objects which
     *         have descriptors
     */
    private MBeanOperationInfo[] modelMBeanOperations;

    private static final String ATTR = "attribute";
    private static final String OPER = "operation";
    private static final String NOTF = "notification";
    private static final String CONS = "constructor";
    private static final String MMB = "mbean";
    private static final String ALL = "all";
    private static final String currClass = "ModelMBeanInfoSupport";

    /**
     * Constructs a ModelMBeanInfoSupport which is a duplicate of the given
     * ModelMBeanInfo.  The returned object is a shallow copy of the given
     * object.  Neither the Descriptor nor the contained arrays
     * ({@code ModelMBeanAttributeInfo[]} etc) are cloned.  This method is
     * chiefly of interest to modify the Descriptor of the returned instance
     * via {@link #setDescriptor setDescriptor} without affecting the
     * Descriptor of the original object.
     *
     * @param mbi the ModelMBeanInfo instance from which the ModelMBeanInfo
     * being created is initialized.
     */
    public ModelMBeanInfoSupport(ModelMBeanInfo  mbi) {
        super(mbi.getClassName(),
                mbi.getDescription(),
                mbi.getAttributes(),
                mbi.getConstructors(),
                mbi.getOperations(),
                mbi.getNotifications());

        modelMBeanAttributes = mbi.getAttributes();
        modelMBeanConstructors = mbi.getConstructors();
        modelMBeanOperations = mbi.getOperations();
        modelMBeanNotifications = mbi.getNotifications();

        try {
            Descriptor mbeandescriptor = mbi.getMBeanDescriptor();
            modelMBeanDescriptor = validDescriptor(mbeandescriptor);
        } catch (MBeanException mbe) {
            modelMBeanDescriptor = validDescriptor(null);
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "ModelMBeanInfo(ModelMBeanInfo) " +
                        "Could not get a valid modelMBeanDescriptor, " +
                        "setting a default Descriptor");
            }
        }

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }
    }

    /**
     * Creates a ModelMBeanInfoSupport with the provided information,
     * but the descriptor is a default.
     * The default descriptor is: name=className, descriptorType="mbean",
     * displayName=className, persistPolicy="never", log="F", visibility="1"
     *
     * @param className classname of the MBean
     * @param description human readable description of the
     * ModelMBean
     * @param attributes array of ModelMBeanAttributeInfo objects
     * which have descriptors
     * @param constructors array of ModelMBeanConstructorInfo
     * objects which have descriptors
     * @param operations array of ModelMBeanOperationInfo objects
     * which have descriptors
     * @param notifications array of ModelMBeanNotificationInfo
     * objects which have descriptors
     */
    public ModelMBeanInfoSupport(String className,
            String description,
            ModelMBeanAttributeInfo[] attributes,
            ModelMBeanConstructorInfo[] constructors,
            ModelMBeanOperationInfo[] operations,
            ModelMBeanNotificationInfo[] notifications) {
        this(className, description, attributes, constructors,
                operations, notifications, null);
    }

    /**
     * Creates a ModelMBeanInfoSupport with the provided information
     * and the descriptor given in parameter.
     *
     * @param className classname of the MBean
     * @param description human readable description of the
     * ModelMBean
     * @param attributes array of ModelMBeanAttributeInfo objects
     * which have descriptors
     * @param constructors array of ModelMBeanConstructorInfo
     * objects which have descriptor
     * @param operations array of ModelMBeanOperationInfo objects
     * which have descriptor
     * @param notifications array of ModelMBeanNotificationInfo
     * objects which have descriptor
     * @param mbeandescriptor descriptor to be used as the
     * MBeanDescriptor containing MBean wide policy. If the
     * descriptor is null, a default descriptor will be constructed.
     * The default descriptor is:
     * name=className, descriptorType="mbean", displayName=className,
     * persistPolicy="never", log="F", visibility="1".  If the descriptor
     * does not contain all of these fields, the missing ones are
     * added with these default values.
     *
     * @exception RuntimeOperationsException Wraps an
     * IllegalArgumentException for invalid descriptor passed in
     * parameter.  (see {@link #getMBeanDescriptor
     * getMBeanDescriptor} for the definition of a valid MBean
     * descriptor.)
     */

    public ModelMBeanInfoSupport(String    className,
            String description,
            ModelMBeanAttributeInfo[] attributes,
            ModelMBeanConstructorInfo[] constructors,
            ModelMBeanOperationInfo[] operations,
            ModelMBeanNotificationInfo[] notifications,
            Descriptor mbeandescriptor) {
        super(className,
                description,
                (attributes != null) ? attributes : NO_ATTRIBUTES,
                (constructors != null) ? constructors : NO_CONSTRUCTORS,
                (operations != null) ? operations : NO_OPERATIONS,
                (notifications != null) ? notifications : NO_NOTIFICATIONS);
        /* The values saved here are possibly null, but we
           check this everywhere they are referenced.  If at
           some stage we replace null with an empty array
           here, as we do in the superclass constructor
           parameters, then we must also do this in
           readObject().  */
        modelMBeanAttributes = attributes;
        modelMBeanConstructors = constructors;
        modelMBeanOperations = operations;
        modelMBeanNotifications = notifications;
        modelMBeanDescriptor = validDescriptor(mbeandescriptor);
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    "ModelMBeanInfoSupport(String,String,ModelMBeanAttributeInfo[]," +
                    "ModelMBeanConstructorInfo[],ModelMBeanOperationInfo[]," +
                    "ModelMBeanNotificationInfo[],Descriptor) " +
                    "Exit");
        }
    }

    private static final ModelMBeanAttributeInfo[] NO_ATTRIBUTES =
            new ModelMBeanAttributeInfo[0];
    private static final ModelMBeanConstructorInfo[] NO_CONSTRUCTORS =
            new ModelMBeanConstructorInfo[0];
    private static final ModelMBeanNotificationInfo[] NO_NOTIFICATIONS =
            new ModelMBeanNotificationInfo[0];
    private static final ModelMBeanOperationInfo[] NO_OPERATIONS =
            new ModelMBeanOperationInfo[0];

    // Java doc inherited from MOdelMBeanInfo interface

    /**
     * Returns a shallow clone of this instance.  Neither the Descriptor nor
     * the contained arrays ({@code ModelMBeanAttributeInfo[]} etc) are
     * cloned.  This method is chiefly of interest to modify the Descriptor
     * of the clone via {@link #setDescriptor setDescriptor} without affecting
     * the Descriptor of the original object.
     *
     * @return a shallow clone of this instance.
     */
    public Object clone() {
        return(new ModelMBeanInfoSupport(this));
    }


    public Descriptor[] getDescriptors(String inDescriptorType)
    throws MBeanException, RuntimeOperationsException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if ((inDescriptorType == null) || (inDescriptorType.isEmpty())) {
            inDescriptorType = "all";
        }

        // if no descriptors of that type, will return empty array
        //
        final Descriptor[] retList;

        if (inDescriptorType.equalsIgnoreCase(MMB)) {
            retList = new Descriptor[] {modelMBeanDescriptor};
        } else if (inDescriptorType.equalsIgnoreCase(ATTR)) {
            final MBeanAttributeInfo[] attrList = modelMBeanAttributes;
            int numAttrs = 0;
            if (attrList != null) numAttrs = attrList.length;

            retList = new Descriptor[numAttrs];
            for (int i=0; i < numAttrs; i++) {
                retList[i] = (((ModelMBeanAttributeInfo)
                    attrList[i]).getDescriptor());
            }
        } else if (inDescriptorType.equalsIgnoreCase(OPER)) {
            final MBeanOperationInfo[] operList = modelMBeanOperations;
            int numOpers = 0;
            if (operList != null) numOpers = operList.length;

            retList = new Descriptor[numOpers];
            for (int i=0; i < numOpers; i++) {
                retList[i] = (((ModelMBeanOperationInfo)
                    operList[i]).getDescriptor());
            }
        } else if (inDescriptorType.equalsIgnoreCase(CONS)) {
            final MBeanConstructorInfo[] consList =  modelMBeanConstructors;
            int numCons = 0;
            if (consList != null) numCons = consList.length;

            retList = new Descriptor[numCons];
            for (int i=0; i < numCons; i++) {
                retList[i] = (((ModelMBeanConstructorInfo)
                    consList[i]).getDescriptor());
            }
        } else if (inDescriptorType.equalsIgnoreCase(NOTF)) {
            final MBeanNotificationInfo[] notifList = modelMBeanNotifications;
            int numNotifs = 0;
            if (notifList != null) numNotifs = notifList.length;

            retList = new Descriptor[numNotifs];
            for (int i=0; i < numNotifs; i++) {
                retList[i] = (((ModelMBeanNotificationInfo)
                    notifList[i]).getDescriptor());
            }
        } else if (inDescriptorType.equalsIgnoreCase(ALL)) {

            final MBeanAttributeInfo[] attrList = modelMBeanAttributes;
            int numAttrs = 0;
            if (attrList != null) numAttrs = attrList.length;

            final MBeanOperationInfo[] operList = modelMBeanOperations;
            int numOpers = 0;
            if (operList != null) numOpers = operList.length;

            final MBeanConstructorInfo[] consList = modelMBeanConstructors;
            int numCons = 0;
            if (consList != null) numCons = consList.length;

            final MBeanNotificationInfo[] notifList = modelMBeanNotifications;
            int numNotifs = 0;
            if (notifList != null) numNotifs = notifList.length;

            int count = numAttrs + numCons + numOpers + numNotifs + 1;
            retList = new Descriptor[count];

            retList[count-1] = modelMBeanDescriptor;

            int j=0;
            for (int i=0; i < numAttrs; i++) {
                retList[j] = (((ModelMBeanAttributeInfo)
                    attrList[i]).getDescriptor());
                j++;
            }
            for (int i=0; i < numCons; i++) {
                retList[j] = (((ModelMBeanConstructorInfo)
                    consList[i]).getDescriptor());
                j++;
            }
            for (int i=0; i < numOpers; i++) {
                retList[j] = (((ModelMBeanOperationInfo)operList[i]).
                        getDescriptor());
                j++;
            }
            for (int i=0; i < numNotifs; i++) {
                retList[j] = (((ModelMBeanNotificationInfo)notifList[i]).
                        getDescriptor());
                j++;
            }
        } else {
            final IllegalArgumentException iae =
                    new IllegalArgumentException("Descriptor Type is invalid");
            final String msg = "Exception occurred trying to find"+
                    " the descriptors of the MBean";
            throw new RuntimeOperationsException(iae,msg);
        }
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

        return retList;
    }


    public void setDescriptors(Descriptor[] inDescriptors)
    throws MBeanException, RuntimeOperationsException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        if (inDescriptors==null) {
            // throw RuntimeOperationsException - invalid descriptor
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Descriptor list is invalid"),
                    "Exception occurred trying to set the descriptors " +
                    "of the MBeanInfo");
        }
        if (inDescriptors.length == 0) { // empty list, no-op
            return;
        }
        for (int j=0; j < inDescriptors.length; j++) {
            setDescriptor(inDescriptors[j],null);
        }
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

    }


    /**
     * Returns a Descriptor requested by name.
     *
     * @param inDescriptorName The name of the descriptor.
     *
     * @return Descriptor containing a descriptor for the ModelMBean with the
     *         same name. If no descriptor is found, null is returned.
     *
     * @exception MBeanException Wraps a distributed communication Exception.
     * @exception RuntimeOperationsException Wraps an IllegalArgumentException
     *            for null name.
     *
     * @see #setDescriptor
     */

    public Descriptor getDescriptor(String inDescriptorName)
    throws MBeanException, RuntimeOperationsException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        return(getDescriptor(inDescriptorName, null));
    }


    public Descriptor getDescriptor(String inDescriptorName,
            String inDescriptorType)
            throws MBeanException, RuntimeOperationsException {
        if (inDescriptorName==null) {
            // throw RuntimeOperationsException - invalid descriptor
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Descriptor is invalid"),
                    "Exception occurred trying to set the descriptors of " +
                    "the MBeanInfo");
        }

        if (MMB.equalsIgnoreCase(inDescriptorType)) {
            return (Descriptor) modelMBeanDescriptor.clone();
        }

            /* The logic here is a bit convoluted, because we are
               dealing with two possible cases, depending on whether
               inDescriptorType is null.  If it's not null, then only
               one of the following ifs will run, and it will either
               return a descriptor or null.  If inDescriptorType is
               null, then all of the following ifs will run until one
               of them finds a descriptor.  */
        if (ATTR.equalsIgnoreCase(inDescriptorType) || inDescriptorType == null) {
            ModelMBeanAttributeInfo attr = getAttribute(inDescriptorName);
            if (attr != null)
                return attr.getDescriptor();
            if (inDescriptorType != null)
                return null;
        }
        if (OPER.equalsIgnoreCase(inDescriptorType) || inDescriptorType == null) {
            ModelMBeanOperationInfo oper = getOperation(inDescriptorName);
            if (oper != null)
                return oper.getDescriptor();
            if (inDescriptorType != null)
                return null;
        }
        if (CONS.equalsIgnoreCase(inDescriptorType) || inDescriptorType == null) {
            ModelMBeanConstructorInfo oper =
                    getConstructor(inDescriptorName);
            if (oper != null)
                return oper.getDescriptor();
            if (inDescriptorType != null)
                return null;
        }
        if (NOTF.equalsIgnoreCase(inDescriptorType) || inDescriptorType == null) {
            ModelMBeanNotificationInfo notif =
                    getNotification(inDescriptorName);
            if (notif != null)
                return notif.getDescriptor();
            if (inDescriptorType != null)
                return null;
        }
        if (inDescriptorType == null)
            return null;
        throw new RuntimeOperationsException(
                new IllegalArgumentException("Descriptor Type is invalid"),
                "Exception occurred trying to find the descriptors of the MBean");

    }



    public void setDescriptor(Descriptor inDescriptor,
            String inDescriptorType)
            throws MBeanException, RuntimeOperationsException {
        final String excMsg =
                "Exception occurred trying to set the descriptors of the MBean";
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (inDescriptor==null) {
            inDescriptor = new DescriptorSupport();
        }

        if ((inDescriptorType == null) || (inDescriptorType.isEmpty())) {
            inDescriptorType =
                    (String) inDescriptor.getFieldValue("descriptorType");

            if (inDescriptorType == null) {
                   MODELMBEAN_LOGGER.log(Level.TRACE,
                                "descriptorType null in both String parameter " +
                                "and Descriptor, defaulting to "+ MMB);
                inDescriptorType = MMB;
            }
        }

        String inDescriptorName =
                (String) inDescriptor.getFieldValue("name");
        if (inDescriptorName == null) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                                "descriptor name null, defaulting to " +
                                this.getClassName());
            inDescriptorName = this.getClassName();
        }
        boolean found = false;
        if (inDescriptorType.equalsIgnoreCase(MMB)) {
            setMBeanDescriptor(inDescriptor);
            found = true;
        } else if (inDescriptorType.equalsIgnoreCase(ATTR)) {
            MBeanAttributeInfo[] attrList =  modelMBeanAttributes;
            int numAttrs = 0;
            if (attrList != null) numAttrs = attrList.length;

            for (int i=0; i < numAttrs; i++) {
                if (inDescriptorName.equals(attrList[i].getName())) {
                    found = true;
                    ModelMBeanAttributeInfo mmbai =
                            (ModelMBeanAttributeInfo) attrList[i];
                    mmbai.setDescriptor(inDescriptor);
                    if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                        StringBuilder strb = new StringBuilder()
                        .append("Setting descriptor to ").append(inDescriptor)
                        .append("\t\n local: AttributeInfo descriptor is ")
                        .append(mmbai.getDescriptor())
                        .append("\t\n modelMBeanInfo: AttributeInfo descriptor is ")
                        .append(this.getDescriptor(inDescriptorName,"attribute"));
                        MODELMBEAN_LOGGER.log(Level.TRACE, strb::toString);
                    }
                }
            }
        } else if (inDescriptorType.equalsIgnoreCase(OPER)) {
            MBeanOperationInfo[] operList =  modelMBeanOperations;
            int numOpers = 0;
            if (operList != null) numOpers = operList.length;

            for (int i=0; i < numOpers; i++) {
                if (inDescriptorName.equals(operList[i].getName())) {
                    found = true;
                    ModelMBeanOperationInfo mmboi =
                            (ModelMBeanOperationInfo) operList[i];
                    mmboi.setDescriptor(inDescriptor);
                }
            }
        } else if (inDescriptorType.equalsIgnoreCase(CONS)) {
            MBeanConstructorInfo[] consList =  modelMBeanConstructors;
            int numCons = 0;
            if (consList != null) numCons = consList.length;

            for (int i=0; i < numCons; i++) {
                if (inDescriptorName.equals(consList[i].getName())) {
                    found = true;
                    ModelMBeanConstructorInfo mmbci =
                            (ModelMBeanConstructorInfo) consList[i];
                    mmbci.setDescriptor(inDescriptor);
                }
            }
        } else if (inDescriptorType.equalsIgnoreCase(NOTF)) {
            MBeanNotificationInfo[] notifList =  modelMBeanNotifications;
            int numNotifs = 0;
            if (notifList != null) numNotifs = notifList.length;

            for (int i=0; i < numNotifs; i++) {
                if (inDescriptorName.equals(notifList[i].getName())) {
                    found = true;
                    ModelMBeanNotificationInfo mmbni =
                            (ModelMBeanNotificationInfo) notifList[i];
                    mmbni.setDescriptor(inDescriptor);
                }
            }
        } else {
            RuntimeException iae =
                    new IllegalArgumentException("Invalid descriptor type: " +
                    inDescriptorType);
            throw new RuntimeOperationsException(iae, excMsg);
        }

        if (!found) {
            RuntimeException iae =
                    new IllegalArgumentException("Descriptor name is invalid: " +
                    "type=" + inDescriptorType +
                    "; name=" + inDescriptorName);
            throw new RuntimeOperationsException(iae, excMsg);
        }
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

    }


    public ModelMBeanAttributeInfo getAttribute(String inName)
    throws MBeanException, RuntimeOperationsException {
        ModelMBeanAttributeInfo retInfo = null;
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        if (inName == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Attribute Name is null"),
                    "Exception occurred trying to get the " +
                    "ModelMBeanAttributeInfo of the MBean");
        }
        MBeanAttributeInfo[] attrList = modelMBeanAttributes;
        int numAttrs = 0;
        if (attrList != null) numAttrs = attrList.length;

        for (int i=0; (i < numAttrs) && (retInfo == null); i++) {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                final StringBuilder strb = new StringBuilder()
                .append("\t\n this.getAttributes() MBeanAttributeInfo Array ")
                .append(i).append(":")
                .append(((ModelMBeanAttributeInfo)attrList[i]).getDescriptor())
                .append("\t\n this.modelMBeanAttributes MBeanAttributeInfo Array ")
                .append(i).append(":")
                .append(((ModelMBeanAttributeInfo)modelMBeanAttributes[i]).getDescriptor());
                MODELMBEAN_LOGGER.log(Level.TRACE, strb::toString);
            }
            if (inName.equals(attrList[i].getName())) {
                retInfo = ((ModelMBeanAttributeInfo)attrList[i].clone());
            }
        }
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

        return retInfo;
    }



    public ModelMBeanOperationInfo getOperation(String inName)
    throws MBeanException, RuntimeOperationsException {
        ModelMBeanOperationInfo retInfo = null;
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        if (inName == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("inName is null"),
                    "Exception occurred trying to get the " +
                    "ModelMBeanOperationInfo of the MBean");
        }
        MBeanOperationInfo[] operList = modelMBeanOperations; //this.getOperations();
        int numOpers = 0;
        if (operList != null) numOpers = operList.length;

        for (int i=0; (i < numOpers) && (retInfo == null); i++) {
            if (inName.equals(operList[i].getName())) {
                retInfo = ((ModelMBeanOperationInfo) operList[i].clone());
            }
        }
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

        return retInfo;
    }

    /**
     * Returns the ModelMBeanConstructorInfo requested by name.
     * If no ModelMBeanConstructorInfo exists for this name null is returned.
     *
     * @param inName the name of the constructor.
     *
     * @return the constructor info for the named constructor, or null
     * if there is none.
     *
     * @exception MBeanException Wraps a distributed communication Exception.
     * @exception RuntimeOperationsException Wraps an IllegalArgumentException
     *            for a null constructor name.
     */

    public ModelMBeanConstructorInfo getConstructor(String inName)
    throws MBeanException, RuntimeOperationsException {
        ModelMBeanConstructorInfo retInfo = null;
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        if (inName == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Constructor name is null"),
                    "Exception occurred trying to get the " +
                    "ModelMBeanConstructorInfo of the MBean");
        }
        MBeanConstructorInfo[] consList = modelMBeanConstructors; //this.getConstructors();
        int numCons = 0;
        if (consList != null) numCons = consList.length;

        for (int i=0; (i < numCons) && (retInfo == null); i++) {
            if (inName.equals(consList[i].getName())) {
                retInfo = ((ModelMBeanConstructorInfo) consList[i].clone());
            }
        }
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

        return retInfo;
    }


    public ModelMBeanNotificationInfo getNotification(String inName)
    throws MBeanException, RuntimeOperationsException {
        ModelMBeanNotificationInfo retInfo = null;
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        if (inName == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Notification name is null"),
                    "Exception occurred trying to get the " +
                    "ModelMBeanNotificationInfo of the MBean");
        }
        MBeanNotificationInfo[] notifList = modelMBeanNotifications; //this.getNotifications();
        int numNotifs = 0;
        if (notifList != null) numNotifs = notifList.length;

        for (int i=0; (i < numNotifs) && (retInfo == null); i++) {
            if (inName.equals(notifList[i].getName())) {
                retInfo = ((ModelMBeanNotificationInfo) notifList[i].clone());
            }
        }
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

        return retInfo;
    }


    /* We override MBeanInfo.getDescriptor() to return our descriptor. */
    /**
     * @since 1.6
     */
    @Override
    public Descriptor getDescriptor() {
        return getMBeanDescriptorNoException();
    }

    public Descriptor getMBeanDescriptor() throws MBeanException {
        return getMBeanDescriptorNoException();
    }

    private Descriptor getMBeanDescriptorNoException() {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (modelMBeanDescriptor == null)
            modelMBeanDescriptor = validDescriptor(null);

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    "Exit, returning: " + modelMBeanDescriptor);
        }
        return (Descriptor) modelMBeanDescriptor.clone();
    }

    public void setMBeanDescriptor(Descriptor inMBeanDescriptor)
    throws MBeanException, RuntimeOperationsException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        modelMBeanDescriptor = validDescriptor(inMBeanDescriptor);
    }


    /**
     * Clones the passed in Descriptor, sets default values, and checks for validity.
     * If the Descriptor is invalid (for instance by having the wrong "name"),
     * this indicates programming error and a RuntimeOperationsException will be thrown.
     *
     * The following fields will be defaulted if they are not already set:
     * displayName=className,name=className,descriptorType="mbean",
     * persistPolicy="never", log="F", visibility="1"
     *
     * @param in Descriptor to be checked, or null which is equivalent to
     * an empty Descriptor.
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
            clone.setField("name", this.getClassName());
            MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor name to " + this.getClassName());
        }
        if (defaulted && clone.getFieldValue("descriptorType")==null) {
            clone.setField("descriptorType", MMB);
            MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting descriptorType to \"" + MMB + "\"");
        }
        if (clone.getFieldValue("displayName") == null) {
            clone.setField("displayName",this.getClassName());
            MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor displayName to " + this.getClassName());
        }
        if (clone.getFieldValue("persistPolicy") == null) {
            clone.setField("persistPolicy","never");
            MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor persistPolicy to \"never\"");
        }
        if (clone.getFieldValue("log") == null) {
            clone.setField("log","F");
            MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor \"log\" field to \"F\"");
        }
        if (clone.getFieldValue("visibility") == null) {
            clone.setField("visibility","1");
            MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor visibility to 1");
        }

        //Checking validity
        if (!clone.isValid()) {
             throw new RuntimeOperationsException(new IllegalArgumentException("Invalid Descriptor argument"),
                "The isValid() method of the Descriptor object itself returned false,"+
                "one or more required fields are invalid. Descriptor:" + clone.toString());
        }

        if (! ((String)clone.getFieldValue("descriptorType")).equalsIgnoreCase(MMB)) {
                 throw new RuntimeOperationsException(new IllegalArgumentException("Invalid Descriptor argument"),
                "The Descriptor \"descriptorType\" field does not match the object described. " +
                 " Expected: "+ MMB + " , was: " + clone.getFieldValue("descriptorType"));
        }

        return clone;
    }




    /**
     * Deserializes a {@link ModelMBeanInfoSupport} from an {@link ObjectInputStream}.
     */
    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        if (compat) {
            // Read an object serialized in the old serial form
            //
            ObjectInputStream.GetField fields = in.readFields();
            modelMBeanDescriptor =
                    (Descriptor) fields.get("modelMBeanDescriptor", null);
            if (fields.defaulted("modelMBeanDescriptor")) {
                throw new NullPointerException("modelMBeanDescriptor");
            }
            modelMBeanAttributes =
                    (MBeanAttributeInfo[]) fields.get("mmbAttributes", null);
            if (fields.defaulted("mmbAttributes")) {
                throw new NullPointerException("mmbAttributes");
            }
            modelMBeanConstructors =
                    (MBeanConstructorInfo[]) fields.get("mmbConstructors", null);
            if (fields.defaulted("mmbConstructors")) {
                throw new NullPointerException("mmbConstructors");
            }
            modelMBeanNotifications =
                    (MBeanNotificationInfo[]) fields.get("mmbNotifications", null);
            if (fields.defaulted("mmbNotifications")) {
                throw new NullPointerException("mmbNotifications");
            }
            modelMBeanOperations =
                    (MBeanOperationInfo[]) fields.get("mmbOperations", null);
            if (fields.defaulted("mmbOperations")) {
                throw new NullPointerException("mmbOperations");
            }
        } else {
            // Read an object serialized in the new serial form
            //
            in.defaultReadObject();
        }
    }


    /**
     * Serializes a {@link ModelMBeanInfoSupport} to an {@link ObjectOutputStream}.
     */
    private void writeObject(ObjectOutputStream out)
    throws IOException {
        if (compat) {
            // Serializes this instance in the old serial form
            //
            ObjectOutputStream.PutField fields = out.putFields();
            fields.put("modelMBeanDescriptor", modelMBeanDescriptor);
            fields.put("mmbAttributes", modelMBeanAttributes);
            fields.put("mmbConstructors", modelMBeanConstructors);
            fields.put("mmbNotifications", modelMBeanNotifications);
            fields.put("mmbOperations", modelMBeanOperations);
            fields.put("currClass", currClass);
            out.writeFields();
        } else {
            // Serializes this instance in the new serial form
            //
            out.defaultWriteObject();
        }
    }


}
