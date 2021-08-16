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
import java.lang.reflect.Method;
import java.security.AccessController;
import java.lang.System.Logger.Level;

import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.DescriptorKey;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.RuntimeOperationsException;

/**
 * <p>The ModelMBeanOperationInfo object describes a management operation of
 * the ModelMBean.  It is a subclass of MBeanOperationInfo with the addition
 * of an associated Descriptor and an implementation of the DescriptorAccess
 * interface.</p>
 *
 * <P id="descriptor">
 * The fields in the descriptor are defined, but not limited to, the following.
 * Note that when the Type in this table is Number, a String that is the decimal
 * representation of a Long can also be used.</P>
 *
 * <table class="striped">
 * <caption style="display:none">ModelMBeanOperationInfo Fields</caption>
 * <thead>
 * <tr><th scope="col">Name</th><th scope="col">Type</th><th scope="col">Meaning</th></tr>
 * </thead>
 * <tbody style="text-align:left">
 * <tr><th scope="row">name</th><td>String</td>
 *     <td>Operation name.</td></tr>
 * <tr><th scope="row">descriptorType</th><td>String</td>
 *     <td>Must be "operation".</td></tr>
 * <tr><th scope="row">class</th><td>String</td>
 *     <td>Class where method is defined (fully qualified).</td></tr>
 * <tr><th scope="row">role</th><td>String</td>
 *     <td>Must be "operation", "getter", or "setter".</td></tr>
 * <tr><th scope="row">targetObject</th><td>Object</td>
 *     <td>Object on which to execute this method.</td></tr>
 * <tr><th scope="row">targetType</th><td>String</td>
 *     <td>type of object reference for targetObject. Can be:
 *         ObjectReference | Handle | EJBHandle | IOR | RMIReference.</td></tr>
 * <tr><th scope="row">value</th><td>Object</td>
 *     <td>Cached value for operation.</td></tr>
 * <tr><th scope="row">displayName</th><td>String</td>
 *     <td>Human readable display name of the operation.</td>
 * <tr><th scope="row">currencyTimeLimit</th><td>Number</td>
 *     <td>How long cached value is valid.</td></tr>
 * <tr><th scope="row">lastUpdatedTimeStamp</th><td>Number</td>
 *     <td>When cached value was set.</td></tr>
 * <tr><th scope="row">visibility</th><td>Number</td>
 *     <td>1-4 where 1: always visible 4: rarely visible.</td></tr>
 * <tr><th scope="row">presentationString</th><td>String</td>
 *     <td>XML formatted string to describe how to present operation</td></tr>
 * </tbody>
 * </table>
 *
 * <p>The default descriptor will have name, descriptorType, displayName and
 * role fields set.  The default value of the name and displayName fields is
 * the operation name.</p>
 *
 * <p><b>Note:</b> because of inconsistencies in previous versions of
 * this specification, it is recommended not to use negative or zero
 * values for <code>currencyTimeLimit</code>.  To indicate that a
 * cached value is never valid, omit the
 * <code>currencyTimeLimit</code> field.  To indicate that it is
 * always valid, use a very large number for this field.</p>
 *
 * <p>The <b>serialVersionUID</b> of this class is <code>6532732096650090465L</code>.
 *
 * @since 1.5
 */

@SuppressWarnings("serial")  // serialVersionUID is not constant
public class ModelMBeanOperationInfo extends MBeanOperationInfo
         implements DescriptorAccess
{

    // Serialization compatibility stuff:
    // Two serial forms are supported in this class. The selected form depends
    // on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form
    private static final long oldSerialVersionUID = 9087646304346171239L;
    //
    // Serial version for new serial form
    private static final long newSerialVersionUID = 6532732096650090465L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields =
    {
      new ObjectStreamField("operationDescriptor", Descriptor.class),
      new ObjectStreamField("currClass", String.class)
    };
    //
    // Serializable fields in new serial form
    private static final ObjectStreamField[] newSerialPersistentFields =
    {
      new ObjectStreamField("operationDescriptor", Descriptor.class)
    };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;
    /**
     * @serialField operationDescriptor Descriptor The descriptor
     * containing the appropriate metadata for this instance
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
         * @serial The descriptor containing the appropriate metadata for this instance
         */
        private Descriptor operationDescriptor = validDescriptor(null);

        private static final String currClass = "ModelMBeanOperationInfo";

        /**
         * Constructs a ModelMBeanOperationInfo object with a default
         * descriptor. The {@link Descriptor} of the constructed
         * object will include fields contributed by any annotations
         * on the {@code Method} object that contain the {@link
         * DescriptorKey} meta-annotation.
         *
         * @param operationMethod The java.lang.reflect.Method object
         * describing the MBean operation.
         * @param description A human readable description of the operation.
         */

        public ModelMBeanOperationInfo(String description,
                                       Method operationMethod)
        {
                super(description, operationMethod);
                // create default descriptor
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "ModelMBeanOperationInfo(String,Method) " +
                            "Entry");
                }
                operationDescriptor = validDescriptor(null);
        }

        /**
         * Constructs a ModelMBeanOperationInfo object. The {@link
         * Descriptor} of the constructed object will include fields
         * contributed by any annotations on the {@code Method} object
         * that contain the {@link DescriptorKey} meta-annotation.
         *
         * @param operationMethod The java.lang.reflect.Method object
         * describing the MBean operation.
         * @param description A human readable description of the
         * operation.
         * @param descriptor An instance of Descriptor containing the
         * appropriate metadata for this instance of the
         * ModelMBeanOperationInfo.  If it is null a default
         * descriptor will be created. If the descriptor does not
         * contain the fields
         * "displayName" or "role", the missing ones are added with
         * their default values.
         *
         * @exception RuntimeOperationsException Wraps an
         * IllegalArgumentException. The descriptor is invalid; or
         * descriptor field "name" is not equal to
         * operation name; or descriptor field "DescriptorType" is
         * not equal to "operation"; or descriptor
         * optional field "role" is present but not equal to "operation",
         * "getter", or "setter".
         *
         */

        public ModelMBeanOperationInfo(String description,
                                       Method operationMethod,
                                       Descriptor descriptor)
        {

                super(description, operationMethod);
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "ModelMBeanOperationInfo(String,Method,Descriptor) "
                            + "Entry");
                }
                operationDescriptor = validDescriptor(descriptor);
        }

        /**
        * Constructs a ModelMBeanOperationInfo object with a default descriptor.
        *
        * @param name The name of the method.
        * @param description A human readable description of the operation.
        * @param signature MBeanParameterInfo objects describing the
        * parameters(arguments) of the method.
        * @param type The type of the method's return value.
        * @param impact The impact of the method, one of INFO, ACTION,
        * ACTION_INFO, UNKNOWN.
        */

        public ModelMBeanOperationInfo(String name,
                                       String description,
                                       MBeanParameterInfo[] signature,
                                       String type,
                                       int impact)
        {

                super(name, description, signature, type, impact);
                // create default descriptor
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "ModelMBeanOperationInfo(" +
                            "String,String,MBeanParameterInfo[],String,int) " +
                            "Entry");
                }
                operationDescriptor = validDescriptor(null);
        }

        /**
        * Constructs a ModelMBeanOperationInfo object.
        *
        * @param name The name of the method.
        * @param description A human readable description of the operation.
        * @param signature MBeanParameterInfo objects describing the
        * parameters(arguments) of the method.
        * @param type The type of the method's return value.
        * @param impact The impact of the method, one of INFO, ACTION,
        * ACTION_INFO, UNKNOWN.
        * @param descriptor An instance of Descriptor containing the
        * appropriate metadata for this instance of the
        * MBeanOperationInfo. If it is null then a default descriptor
        * will be created.  If the descriptor does not contain
        * fields "displayName" or "role",
        * the missing ones are added with their default values.
        *
        * @exception RuntimeOperationsException Wraps an
        * IllegalArgumentException. The descriptor is invalid; or
        * descriptor field "name" is not equal to
        * operation name; or descriptor field "DescriptorType" is
        * not equal to "operation"; or descriptor optional
        * field "role" is present but not equal to "operation", "getter", or
        * "setter".
        */

        public ModelMBeanOperationInfo(String name,
                                       String description,
                                       MBeanParameterInfo[] signature,
                                       String type,
                                       int impact,
                                       Descriptor descriptor)
        {
                super(name, description, signature, type, impact);
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "ModelMBeanOperationInfo(String,String," +
                            "MBeanParameterInfo[],String,int,Descriptor) " +
                            "Entry");
                }
                operationDescriptor = validDescriptor(descriptor);
        }

        /**
         * Constructs a new ModelMBeanOperationInfo object from this
         * ModelMBeanOperation Object.
         *
         * @param inInfo the ModelMBeanOperationInfo to be duplicated
         *
         */

        public ModelMBeanOperationInfo(ModelMBeanOperationInfo inInfo)
        {
                super(inInfo.getName(),
                          inInfo.getDescription(),
                          inInfo.getSignature(),
                          inInfo.getReturnType(),
                          inInfo.getImpact());
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "ModelMBeanOperationInfo(ModelMBeanOperationInfo)" +
                            "Entry");
                }
                Descriptor newDesc = inInfo.getDescriptor();
                operationDescriptor = validDescriptor(newDesc);
        }

        /**
        * Creates and returns a new ModelMBeanOperationInfo which is a
        * duplicate of this ModelMBeanOperationInfo.
        *
        */

        public Object clone ()
        {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
            }
                return(new ModelMBeanOperationInfo(this)) ;
        }

        /**
         * Returns a copy of the associated Descriptor of the
         * ModelMBeanOperationInfo.
         *
         * @return Descriptor associated with the
         * ModelMBeanOperationInfo object.
         *
         * @see #setDescriptor
         */

        public Descriptor getDescriptor()
        {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
            }
            if (operationDescriptor == null) {
                operationDescriptor = validDescriptor(null);
            }

            return((Descriptor) operationDescriptor.clone());
        }

        /**
         * Sets associated Descriptor (full replace) for the
         * ModelMBeanOperationInfo If the new Descriptor is null, then
         * the associated Descriptor reverts to a default descriptor.
         * The Descriptor is validated before it is assigned.  If the
         * new Descriptor is invalid, then a
         * RuntimeOperationsException wrapping an
         * IllegalArgumentException is thrown.
         *
         * @param inDescriptor replaces the Descriptor associated with the
         * ModelMBeanOperation.
         *
         * @exception RuntimeOperationsException Wraps an
         * IllegalArgumentException for invalid Descriptor.
         *
         * @see #getDescriptor
         */
        public void setDescriptor(Descriptor inDescriptor)
        {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
            }
            operationDescriptor = validDescriptor(inDescriptor);
        }

        /**
        * Returns a string containing the entire contents of the
        * ModelMBeanOperationInfo in human readable form.
        */
        public String toString()
        {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
            }
                String retStr =
                    "ModelMBeanOperationInfo: " + this.getName() +
                    " ; Description: " + this.getDescription() +
                    " ; Descriptor: " + this.getDescriptor() +
                    " ; ReturnType: " + this.getReturnType() +
                    " ; Signature: ";
                MBeanParameterInfo[] pTypes = this.getSignature();
                for (int i=0; i < pTypes.length; i++)
                {
                        retStr = retStr.concat((pTypes[i]).getType() + ", ");
                }
                return retStr;
        }

        /**
         * Clones the passed in Descriptor, sets default values, and checks for validity.
         * If the Descriptor is invalid (for instance by having the wrong "name"),
         * this indicates programming error and a RuntimeOperationsException will be thrown.
         *
         * The following fields will be defaulted if they are not already set:
         * displayName=this.getName(),name=this.getName(),
         * descriptorType="operation", role="operation"
         *
         *
         * @param in Descriptor to be checked, or null which is equivalent to
         * an empty Descriptor.
         * @exception RuntimeOperationsException if Descriptor is invalid
         */
        private Descriptor validDescriptor(final Descriptor in)
        throws RuntimeOperationsException {
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
                clone.setField("descriptorType", "operation");
                MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting descriptorType to \"operation\"");
            }
            if (clone.getFieldValue("displayName") == null) {
                clone.setField("displayName",this.getName());
                MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor displayName to " + this.getName());
            }
            if (clone.getFieldValue("role") == null) {
                clone.setField("role","operation");
                MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor role field to \"operation\"");
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
            if (!"operation".equalsIgnoreCase((String) clone.getFieldValue("descriptorType"))) {
                     throw new RuntimeOperationsException(new IllegalArgumentException("Invalid Descriptor argument"),
                    "The Descriptor \"descriptorType\" field does not match the object described. " +
                     " Expected: \"operation\" ," + " was: " + clone.getFieldValue("descriptorType"));
            }
            final String role = (String)clone.getFieldValue("role");
            if (!(role.equalsIgnoreCase("operation") ||
                  role.equalsIgnoreCase("setter") ||
                  role.equalsIgnoreCase("getter"))) {
                     throw new RuntimeOperationsException(new IllegalArgumentException("Invalid Descriptor argument"),
                    "The Descriptor \"role\" field does not match the object described. " +
                     " Expected: \"operation\", \"setter\", or \"getter\" ," + " was: " + clone.getFieldValue("role"));
            }
            final Object targetValue = clone.getFieldValue("targetType");
            if (targetValue != null) {
                if (!(targetValue instanceof java.lang.String)) {
                    throw new RuntimeOperationsException(new IllegalArgumentException("Invalid Descriptor argument"),
                    "The Descriptor field \"targetValue\" is invalid class. " +
                     " Expected: java.lang.String, " + " was: " + targetValue.getClass().getName());
                }
            }
            return clone;
        }

    /**
     * Deserializes a {@link ModelMBeanOperationInfo} from an {@link ObjectInputStream}.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
      // New serial form ignores extra field "currClass"
      in.defaultReadObject();
    }


    /**
     * Serializes a {@link ModelMBeanOperationInfo} to an {@link ObjectOutputStream}.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
      if (compat)
      {
        // Serializes this instance in the old serial form
        //
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("operationDescriptor", operationDescriptor);
        fields.put("currClass", currClass);
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
