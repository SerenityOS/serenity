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
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.lang.System.Logger.Level;

import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.DescriptorKey;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanParameterInfo;
import javax.management.RuntimeOperationsException;

/**
 * <p>The ModelMBeanConstructorInfo object describes a constructor of the ModelMBean.
 * It is a subclass of MBeanConstructorInfo with the addition of an associated Descriptor
 * and an implementation of the DescriptorAccess interface.</p>
 *
 * <P id="descriptor">
 * The fields in the descriptor are defined, but not limited to, the following.
 * Note that when the Type in this table is Number, a String that is the decimal
 * representation of a Long can also be used.</P>
 *
 * <table class="striped">
 * <caption style="display:none">ModelMBeanConstructorInfo Fields</caption>
 * <thead>
 * <tr><th scope="col">Name</th><th scope="col">Type</th><th scope="col">Meaning</th></tr>
 * </thead>
 * <tbody style="text-align:left">
 * <tr><th scope="row">name</th><td>String</td>
 *     <td>Constructor name.</td></tr>
 * <tr><th scope="row">descriptorType</th><td>String</td>
 *     <td>Must be "operation".</td></tr>
 * <tr><th scope="row">role</th><td>String</td>
 *     <td>Must be "constructor".</td></tr>
 * <tr><th scope="row">displayName</th><td>String</td>
 *     <td>Human readable name of constructor.</td></tr>
 * <tr><th scope="row">visibility</th><td>Number</td>
 *     <td>1-4 where 1: always visible 4: rarely visible.</td></tr>
 * <tr><th scope="row">presentationString</th><td>String</td>
 *     <td>XML formatted string to describe how to present operation</td></tr>
 * </tbody>
 * </table>
 *
 * <p>The {@code persistPolicy} and {@code currencyTimeLimit} fields
 * are meaningless for constructors, but are not considered invalid.</p>
 *
 * <p>The default descriptor will have the {@code name}, {@code
 * descriptorType}, {@code displayName} and {@code role} fields.
 *
 * <p>The <b>serialVersionUID</b> of this class is <code>3862947819818064362L</code>.
 *
 * @since 1.5
 */

@SuppressWarnings("serial")  // serialVersionUID is not constant
public class ModelMBeanConstructorInfo
    extends MBeanConstructorInfo
    implements DescriptorAccess {

    // Serialization compatibility stuff:
    // Two serial forms are supported in this class. The selected form depends
    // on system property "jmx.serial.form":
    //  - "1.0" for JMX 1.0
    //  - any other value for JMX 1.1 and higher
    //
    // Serial version for old serial form
    private static final long oldSerialVersionUID = -4440125391095574518L;
    //
    // Serial version for new serial form
    private static final long newSerialVersionUID = 3862947819818064362L;
    //
    // Serializable fields in old serial form
    private static final ObjectStreamField[] oldSerialPersistentFields =
    {
      new ObjectStreamField("consDescriptor", Descriptor.class),
      new ObjectStreamField("currClass", String.class)
    };
    //
    // Serializable fields in new serial form
    private static final ObjectStreamField[] newSerialPersistentFields =
    {
      new ObjectStreamField("consDescriptor", Descriptor.class)
    };
    //
    // Actual serial version and serial form
    private static final long serialVersionUID;
    /**
     * @serialField consDescriptor Descriptor The {@link Descriptor} containing the metadata for this instance
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
         * @serial The {@link Descriptor} containing the metadata for this instance
         */
        private Descriptor consDescriptor = validDescriptor(null);

        private static final String currClass = "ModelMBeanConstructorInfo";


        /**
        * Constructs a ModelMBeanConstructorInfo object with a default
        * descriptor.  The {@link Descriptor} of the constructed
        * object will include fields contributed by any annotations on
        * the {@code Constructor} object that contain the {@link
        * DescriptorKey} meta-annotation.
        *
        * @param description A human readable description of the constructor.
        * @param constructorMethod The java.lang.reflect.Constructor object
        * describing the MBean constructor.
        */
        public ModelMBeanConstructorInfo(String description,
                                         Constructor<?> constructorMethod)
    {
                super(description, constructorMethod);
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "ModelMBeanConstructorInfo(String,Constructor) " +
                            "Entry");
                }
                consDescriptor = validDescriptor(null);

                // put getter and setter methods in constructors list
                // create default descriptor

        }

        /**
        * Constructs a ModelMBeanConstructorInfo object.  The {@link
        * Descriptor} of the constructed object will include fields
        * contributed by any annotations on the {@code Constructor}
        * object that contain the {@link DescriptorKey}
        * meta-annotation.
        *
        * @param description A human readable description of the constructor.
        * @param constructorMethod The java.lang.reflect.Constructor object
        * describing the ModelMBean constructor.
        * @param descriptor An instance of Descriptor containing the
        * appropriate metadata for this instance of the
        * ModelMBeanConstructorInfo.  If it is null, then a default
        * descriptor will be created. If the descriptor does not
        * contain the field "displayName" this field is added in the
        * descriptor with its default value.
        *
        * @exception RuntimeOperationsException Wraps an
        * IllegalArgumentException. The descriptor is invalid, or
        * descriptor field "name" is not equal to name
        * parameter, or descriptor field "descriptorType" is
        * not equal to "operation" or descriptor field "role" is
        * present but not equal to "constructor".
        */

        public ModelMBeanConstructorInfo(String description,
                                         Constructor<?> constructorMethod,
                                         Descriptor descriptor)
        {

                super(description, constructorMethod);
                // put getter and setter methods in constructors list
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "ModelMBeanConstructorInfo(" +
                            "String,Constructor,Descriptor) Entry");
                }
                consDescriptor = validDescriptor(descriptor);
        }
        /**
        * Constructs a ModelMBeanConstructorInfo object with a default descriptor.
        *
        * @param name The name of the constructor.
        * @param description A human readable description of the constructor.
        * @param signature MBeanParameterInfo object array describing the parameters(arguments) of the constructor.
        */

        public ModelMBeanConstructorInfo(String name,
                                         String description,
                                         MBeanParameterInfo[] signature)
        {

                super(name, description, signature);
                // create default descriptor
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "ModelMBeanConstructorInfo(" +
                            "String,String,MBeanParameterInfo[]) Entry");
                }
                consDescriptor = validDescriptor(null);
        }
        /**
        * Constructs a ModelMBeanConstructorInfo object.
        *
        * @param name The name of the constructor.
        * @param description A human readable description of the constructor.
        * @param signature MBeanParameterInfo objects describing the parameters(arguments) of the constructor.
        * @param descriptor An instance of Descriptor containing the appropriate metadata
        *                   for this instance of the MBeanConstructorInfo. If it is null then a default descriptor will be created.
        * If the descriptor does not contain the field "displayName" this field
        * is added in the descriptor with its default value.
        *
        * @exception RuntimeOperationsException Wraps an
        * IllegalArgumentException. The descriptor is invalid, or
        * descriptor field "name" is not equal to name
        * parameter, or descriptor field "descriptorType" is
        * not equal to "operation" or descriptor field "role" is
        * present but not equal to "constructor".
        */

        public ModelMBeanConstructorInfo(String name,
                                         String description,
                                         MBeanParameterInfo[] signature,
                                         Descriptor descriptor)
        {
                super(name, description, signature);
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "ModelMBeanConstructorInfo(" +
                            "String,String,MBeanParameterInfo[],Descriptor) " +
                            "Entry");
                }
                consDescriptor = validDescriptor(descriptor);
        }

        /**
         * Constructs a new ModelMBeanConstructorInfo object from this ModelMBeanConstructor Object.
         *
         * @param old the ModelMBeanConstructorInfo to be duplicated
         *
         */
        ModelMBeanConstructorInfo(ModelMBeanConstructorInfo old)
        {
                super(old.getName(), old.getDescription(), old.getSignature());
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "ModelMBeanConstructorInfo(" +
                            "ModelMBeanConstructorInfo) Entry");
                }
                consDescriptor = validDescriptor(consDescriptor);
        }

        /**
        * Creates and returns a new ModelMBeanConstructorInfo which is a duplicate of this ModelMBeanConstructorInfo.
        *
        */
        @Override
        public Object clone ()
        {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
            }
                return(new ModelMBeanConstructorInfo(this)) ;
        }

        /**
         * Returns a copy of the associated Descriptor.
         *
         * @return Descriptor associated with the
         * ModelMBeanConstructorInfo object.
         *
         * @see #setDescriptor
         */


        @Override
        public Descriptor getDescriptor()
        {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
            }
            if (consDescriptor == null){
                consDescriptor = validDescriptor(null);
            }
            return((Descriptor)consDescriptor.clone());
        }
        /**
        * Sets associated Descriptor (full replace) of
        * ModelMBeanConstructorInfo.  If the new Descriptor is null,
        * then the associated Descriptor reverts to a default
        * descriptor.  The Descriptor is validated before it is
        * assigned.  If the new Descriptor is invalid, then a
        * RuntimeOperationsException wrapping an
        * IllegalArgumentException is thrown.
        *
        * @param inDescriptor replaces the Descriptor associated with
        * the ModelMBeanConstructor. If the descriptor does not
        * contain all the following fields, the missing ones are added with
        * their default values: displayName, name, role, descriptorType.
        *
        * @exception RuntimeOperationsException Wraps an
        * IllegalArgumentException.  The descriptor is invalid, or
        * descriptor field "name" is present but not equal to name
        * parameter, or descriptor field "descriptorType" is present
        * but not equal to "operation" or descriptor field "role" is
        * present but not equal to "constructor".
        *
        * @see #getDescriptor
        */
        public void setDescriptor(Descriptor inDescriptor)
        {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
            }
            consDescriptor = validDescriptor(inDescriptor);
        }

        /**
        * Returns a string containing the entire contents of the ModelMBeanConstructorInfo in human readable form.
        */
        @Override
        public String toString()
        {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
            }
                String retStr =
                    "ModelMBeanConstructorInfo: " + this.getName() +
                    " ; Description: " + this.getDescription() +
                    " ; Descriptor: " + this.getDescriptor() +
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
         * displayName=this.getName(), name=this.getName(), descriptorType="operation",
         * role="constructor"
         *
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
                clone.setField("role","constructor");
                MODELMBEAN_LOGGER.log(Level.TRACE, "Defaulting Descriptor role field to \"constructor\"");
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
            if (! ((String)clone.getFieldValue("role")).equalsIgnoreCase("constructor")) {
                     throw new RuntimeOperationsException(new IllegalArgumentException("Invalid Descriptor argument"),
                    "The Descriptor \"role\" field does not match the object described. " +
                     " Expected: \"constructor\" ," + " was: " + clone.getFieldValue("role"));
            }

            return clone;
        }

    /**
     * Deserializes a {@link ModelMBeanConstructorInfo} from an {@link ObjectInputStream}.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
      // New serial form ignores extra field "currClass"
      in.defaultReadObject();
    }


    /**
     * Serializes a {@link ModelMBeanConstructorInfo} to an {@link ObjectOutputStream}.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
      if (compat)
      {
        // Serializes this instance in the old serial form
        //
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("consDescriptor", consDescriptor);
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
