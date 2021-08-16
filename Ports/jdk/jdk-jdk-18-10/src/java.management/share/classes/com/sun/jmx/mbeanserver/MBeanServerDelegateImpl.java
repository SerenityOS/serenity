/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.jmx.mbeanserver;

import java.lang.System.Logger.Level;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.JMRuntimeException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

import static com.sun.jmx.defaults.JmxProperties.MBEANSERVER_LOGGER;

/**
 * This class is the MBean implementation of the MBeanServerDelegate.
 *
 * @since 1.5
 */
final class MBeanServerDelegateImpl
    extends MBeanServerDelegate
    implements DynamicMBean, MBeanRegistration {

    final private static String[] attributeNames = new String[] {
        "MBeanServerId",
        "SpecificationName",
        "SpecificationVersion",
        "SpecificationVendor",
        "ImplementationName",
        "ImplementationVersion",
        "ImplementationVendor"
    };

    private static final MBeanAttributeInfo[] attributeInfos =
        new MBeanAttributeInfo[] {
            new MBeanAttributeInfo("MBeanServerId","java.lang.String",
                                   "The MBean server agent identification",
                                   true,false,false),
            new MBeanAttributeInfo("SpecificationName","java.lang.String",
                                   "The full name of the JMX specification "+
                                   "implemented by this product.",
                                   true,false,false),
            new MBeanAttributeInfo("SpecificationVersion","java.lang.String",
                                   "The version of the JMX specification "+
                                   "implemented by this product.",
                                   true,false,false),
            new MBeanAttributeInfo("SpecificationVendor","java.lang.String",
                                   "The vendor of the JMX specification "+
                                   "implemented by this product.",
                                   true,false,false),
            new MBeanAttributeInfo("ImplementationName","java.lang.String",
                                   "The JMX implementation name "+
                                   "(the name of this product)",
                                   true,false,false),
            new MBeanAttributeInfo("ImplementationVersion","java.lang.String",
                                   "The JMX implementation version "+
                                   "(the version of this product).",
                                   true,false,false),
            new MBeanAttributeInfo("ImplementationVendor","java.lang.String",
                                   "the JMX implementation vendor "+
                                   "(the vendor of this product).",
                                   true,false,false)
                };

    private final MBeanInfo delegateInfo;

    public MBeanServerDelegateImpl () {
        super();
        delegateInfo =
            new MBeanInfo("javax.management.MBeanServerDelegate",
                          "Represents  the MBean server from the management "+
                          "point of view.",
                          MBeanServerDelegateImpl.attributeInfos, null,
                          null,getNotificationInfo());
    }

    final public ObjectName preRegister (MBeanServer server, ObjectName name)
        throws java.lang.Exception {
        if (name == null) return DELEGATE_NAME;
        else return name;
    }

    final public void postRegister (Boolean registrationDone) {
    }

    final public void preDeregister()
        throws java.lang.Exception {
        throw new IllegalArgumentException(
                 "The MBeanServerDelegate MBean cannot be unregistered");
    }

    final public void postDeregister() {
    }

    /**
     * Obtains the value of a specific attribute of the MBeanServerDelegate.
     *
     * @param attribute The name of the attribute to be retrieved
     *
     * @return  The value of the attribute retrieved.
     *
     * @exception AttributeNotFoundException
     * @exception MBeanException
     *            Wraps a <CODE>java.lang.Exception</CODE> thrown by the
     *            MBean's getter.
     */
    public Object getAttribute(String attribute)
        throws AttributeNotFoundException,
               MBeanException, ReflectionException {
        try {
            // attribute must not be null
            //
            if (attribute == null)
                throw new AttributeNotFoundException("null");

            // Extract the requested attribute from file
            //
            if (attribute.equals("MBeanServerId"))
                return getMBeanServerId();
            else if (attribute.equals("SpecificationName"))
                return getSpecificationName();
            else if (attribute.equals("SpecificationVersion"))
                return getSpecificationVersion();
            else if (attribute.equals("SpecificationVendor"))
                return getSpecificationVendor();
            else if (attribute.equals("ImplementationName"))
                return getImplementationName();
            else if (attribute.equals("ImplementationVersion"))
                return getImplementationVersion();
            else if (attribute.equals("ImplementationVendor"))
                return getImplementationVendor();

            // Unknown attribute
            //
            else
                throw new AttributeNotFoundException("null");

        } catch (AttributeNotFoundException x) {
            throw x;
        } catch (JMRuntimeException j) {
            throw j;
        } catch (SecurityException s) {
            throw s;
        } catch (Exception x) {
            throw new MBeanException(x,"Failed to get " + attribute);
        }
    }

    /**
     * This method always fail since all MBeanServerDelegateMBean attributes
     * are read-only.
     *
     * @param attribute The identification of the attribute to
     * be set and  the value it is to be set to.
     *
     * @exception AttributeNotFoundException
     */
    public void setAttribute(Attribute attribute)
        throws AttributeNotFoundException, InvalidAttributeValueException,
               MBeanException, ReflectionException {

        // Now we will always fail:
        // Either because the attribute is null or because it is not
        // accessible (or does not exist).
        //
        final String attname = (attribute==null?null:attribute.getName());
        if (attname == null) {
            final RuntimeException r =
                new IllegalArgumentException("Attribute name cannot be null");
            throw new RuntimeOperationsException(r,
                "Exception occurred trying to invoke the setter on the MBean");
        }

        // This is a hack: we call getAttribute in order to generate an
        // AttributeNotFoundException if the attribute does not exist.
        //
        Object val = getAttribute(attname);

        // If we reach this point, we know that the requested attribute
        // exists. However, since all attributes are read-only, we throw
        // an AttributeNotFoundException.
        //
        throw new AttributeNotFoundException(attname + " not accessible");
    }

    /**
     * Makes it possible to get the values of several attributes of
     * the MBeanServerDelegate.
     *
     * @param attributes A list of the attributes to be retrieved.
     *
     * @return  The list of attributes retrieved.
     *
     */
    public AttributeList getAttributes(String[] attributes) {
        // If attributes is null, the get all attributes.
        //
        final String[] attn = (attributes==null?attributeNames:attributes);

        // Prepare the result list.
        //
        final int len = attn.length;
        final AttributeList list = new AttributeList(len);

        // Get each requested attribute.
        //
        for (int i=0;i<len;i++) {
            try {
                final Attribute a =
                    new Attribute(attn[i],getAttribute(attn[i]));
                list.add(a);
            } catch (Exception x) {
                // Skip the attribute that couldn't be obtained.
                //
                if (MBEANSERVER_LOGGER.isLoggable(Level.TRACE)) {
                    MBEANSERVER_LOGGER.log(Level.TRACE,
                            "Attribute " + attn[i] + " not found");
                }
            }
        }

        // Finally return the result.
        //
        return list;
    }

    /**
     * This method always return an empty list since all
     * MBeanServerDelegateMBean attributes are read-only.
     *
     * @param attributes A list of attributes: The identification of the
     * attributes to be set and  the values they are to be set to.
     *
     * @return  The list of attributes that were set, with their new values.
     *          In fact, this method always return an empty list since all
     *          MBeanServerDelegateMBean attributes are read-only.
     */
    public AttributeList setAttributes(AttributeList attributes) {
        return new AttributeList(0);
    }

    /**
     * Always fails since the MBeanServerDelegate MBean has no operation.
     *
     * @param actionName The name of the action to be invoked.
     * @param params An array containing the parameters to be set when the
     *        action is invoked.
     * @param signature An array containing the signature of the action.
     *
     * @return  The object returned by the action, which represents
     *          the result of invoking the action on the MBean specified.
     *
     * @exception MBeanException  Wraps a <CODE>java.lang.Exception</CODE>
     *         thrown by the MBean's invoked method.
     * @exception ReflectionException  Wraps a
     *      <CODE>java.lang.Exception</CODE> thrown while trying to invoke
     *      the method.
     */
    public Object invoke(String actionName, Object params[],
                         String signature[])
        throws MBeanException, ReflectionException {
        // Check that operation name is not null.
        //
        if (actionName == null) {
            final RuntimeException r =
              new IllegalArgumentException("Operation name  cannot be null");
            throw new RuntimeOperationsException(r,
            "Exception occurred trying to invoke the operation on the MBean");
        }

        throw new ReflectionException(
                          new NoSuchMethodException(actionName),
                          "The operation with name " + actionName +
                          " could not be found");
    }

    /**
     * Provides the MBeanInfo describing the MBeanServerDelegate.
     *
     * @return  The MBeanInfo describing the MBeanServerDelegate.
     *
     */
    public MBeanInfo getMBeanInfo() {
        return delegateInfo;
    }

}
