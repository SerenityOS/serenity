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

/* java imports */

import static com.sun.jmx.defaults.JmxProperties.MODELMBEAN_LOGGER;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Set;

import java.util.Vector;
import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeChangeNotificationFilter;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeOperationsException;
import javax.management.ServiceNotFoundException;
import javax.management.loading.ClassLoaderRepository;
import jdk.internal.access.JavaSecurityAccess;
import jdk.internal.access.SharedSecrets;

import sun.reflect.misc.MethodUtil;
import sun.reflect.misc.ReflectUtil;

/**
 * This class is the implementation of a ModelMBean. An appropriate
 * implementation of a ModelMBean must be shipped with every JMX Agent
 * and the class must be named RequiredModelMBean.
 * <P>
 * Java resources wishing to be manageable instantiate the
 * RequiredModelMBean using the MBeanServer's createMBean method.
 * The resource then sets the MBeanInfo and Descriptors for the
 * RequiredModelMBean instance. The attributes and operations exposed
 * via the ModelMBeanInfo for the ModelMBean are accessible
 * from MBeans, connectors/adaptors like other MBeans. Through the
 * Descriptors, values and methods in the managed application can be
 * defined and mapped to attributes and operations of the ModelMBean.
 * This mapping can be defined in an XML formatted file or dynamically and
 * programmatically at runtime.
 * <P>
 * Every RequiredModelMBean which is instantiated in the MBeanServer
 * becomes manageable:<br>
 * its attributes and operations become remotely accessible through the
 * connectors/adaptors connected to that MBeanServer.
 * <P>
 * A Java object cannot be registered in the MBeanServer unless it is a
 * JMX compliant MBean. By instantiating a RequiredModelMBean, resources
 * are guaranteed that the MBean is valid.
 *
 * MBeanException and RuntimeOperationsException must be thrown on every
 * public method.  This allows for wrapping exceptions from distributed
 * communications (RMI, EJB, etc.)
 *
 * @since 1.5
 */

public class RequiredModelMBean
    implements ModelMBean, MBeanRegistration, NotificationEmitter {

    /*************************************/
    /* attributes                        */
    /*************************************/
    ModelMBeanInfo modelMBeanInfo;

    /* Notification broadcaster for any notification to be sent
     * from the application through the RequiredModelMBean.  */
    private NotificationBroadcasterSupport generalBroadcaster = null;

    /* Notification broadcaster for attribute change notifications */
    private NotificationBroadcasterSupport attributeBroadcaster = null;

    /* handle, name, or reference for instance on which the actual invoke
     * and operations will be executed */
    private Object managedResource = null;


    /* records the registering in MBeanServer */
    private boolean registered = false;
    private transient MBeanServer server = null;

    private static final JavaSecurityAccess javaSecurityAccess = SharedSecrets.getJavaSecurityAccess();
    @SuppressWarnings("removal")
    final private AccessControlContext acc = AccessController.getContext();

    /*************************************/
    /* constructors                      */
    /*************************************/

    /**
     * Constructs an <CODE>RequiredModelMBean</CODE> with an empty
     * ModelMBeanInfo.
     * <P>
     * The RequiredModelMBean's MBeanInfo and Descriptors
     * can be customized using the {@link #setModelMBeanInfo} method.
     * After the RequiredModelMBean's MBeanInfo and Descriptors are
     * customized, the RequiredModelMBean can be registered with
     * the MBeanServer.
     *
     * @exception MBeanException Wraps a distributed communication Exception.
     *
     * @exception RuntimeOperationsException Wraps a {@link
     * RuntimeException} during the construction of the object.
     **/
    public RequiredModelMBean()
        throws MBeanException, RuntimeOperationsException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        modelMBeanInfo = createDefaultModelMBeanInfo();
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }
    }

    /**
     * Constructs a RequiredModelMBean object using ModelMBeanInfo passed in.
     * As long as the RequiredModelMBean is not registered
     * with the MBeanServer yet, the RequiredModelMBean's MBeanInfo and
     * Descriptors can be customized using the {@link #setModelMBeanInfo}
     * method.
     * After the RequiredModelMBean's MBeanInfo and Descriptors are
     * customized, the RequiredModelMBean can be registered with the
     * MBeanServer.
     *
     * @param mbi The ModelMBeanInfo object to be used by the
     *            RequiredModelMBean. The given ModelMBeanInfo is cloned
     *            and modified as specified by {@link #setModelMBeanInfo}
     *
     * @exception MBeanException Wraps a distributed communication Exception.
     * @exception RuntimeOperationsException Wraps an
     *    {link java.lang.IllegalArgumentException}:
     *          The MBeanInfo passed in parameter is null.
     *
     **/
    public RequiredModelMBean(ModelMBeanInfo mbi)
        throws MBeanException, RuntimeOperationsException {

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }
        setModelMBeanInfo(mbi);

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }
    }


    /*************************************/
    /* initializers                      */
    /*************************************/

    /**
     * Initializes a ModelMBean object using ModelMBeanInfo passed in.
     * This method makes it possible to set a customized ModelMBeanInfo on
     * the ModelMBean as long as it is not registered with the MBeanServer.
     * <br>
     * Once the ModelMBean's ModelMBeanInfo (with Descriptors) are
     * customized and set on the ModelMBean, the  ModelMBean be
     * registered with the MBeanServer.
     * <P>
     * If the ModelMBean is currently registered, this method throws
     * a {@link javax.management.RuntimeOperationsException} wrapping an
     * {@link IllegalStateException}
     * <P>
     * If the given <var>inModelMBeanInfo</var> does not contain any
     * {@link ModelMBeanNotificationInfo} for the <code>GENERIC</code>
     * or <code>ATTRIBUTE_CHANGE</code> notifications, then the
     * RequiredModelMBean will supply its own default
     * {@link ModelMBeanNotificationInfo ModelMBeanNotificationInfo}s for
     * those missing notifications.
     *
     * @param mbi The ModelMBeanInfo object to be used
     *        by the ModelMBean.
     *
     * @exception MBeanException Wraps a distributed communication
     *        Exception.
     * @exception RuntimeOperationsException
     * <ul><li>Wraps an {@link IllegalArgumentException} if
     *         the MBeanInfo passed in parameter is null.</li>
     *     <li>Wraps an {@link IllegalStateException} if the ModelMBean
     *         is currently registered in the MBeanServer.</li>
     * </ul>
     *
     **/
    public void setModelMBeanInfo(ModelMBeanInfo mbi)
        throws MBeanException, RuntimeOperationsException {

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (mbi == null) {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                    "ModelMBeanInfo is null: Raising exception.");
            }
            final RuntimeException x = new
                IllegalArgumentException("ModelMBeanInfo must not be null");
            final String exceptionText =
                "Exception occurred trying to initialize the " +
                "ModelMBeanInfo of the RequiredModelMBean";
            throw new RuntimeOperationsException(x,exceptionText);
        }

        if (registered) {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                    "RequiredMBean is registered: Raising exception.");
            }
            final String exceptionText =
                "Exception occurred trying to set the " +
                "ModelMBeanInfo of the RequiredModelMBean";
            final RuntimeException x = new IllegalStateException(
             "cannot call setModelMBeanInfo while ModelMBean is registered");
            throw new RuntimeOperationsException(x,exceptionText);
        }

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                "Setting ModelMBeanInfo to " + printModelMBeanInfo(mbi));
            int noOfNotifications = 0;
            if (mbi.getNotifications() != null) {
                noOfNotifications = mbi.getNotifications().length;
            }
            MODELMBEAN_LOGGER.log(Level.TRACE,
                "ModelMBeanInfo notifications has " +
                noOfNotifications + " elements");
        }

        modelMBeanInfo = (ModelMBeanInfo)mbi.clone();

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "set mbeanInfo to: "+
                 printModelMBeanInfo(modelMBeanInfo));
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }
    }


    /**
     * Sets the instance handle of the object against which to
     * execute all methods in this ModelMBean management interface
     * (MBeanInfo and Descriptors).
     *
     * @param mr Object that is the managed resource
     * @param mr_type The type of reference for the managed resource.
     *     <br>Can be: "ObjectReference", "Handle", "IOR", "EJBHandle",
     *         or "RMIReference".
     *     <br>In this implementation only "ObjectReference" is supported.
     *
     * @exception MBeanException The initializer of the object has
     *            thrown an exception.
     * @exception InstanceNotFoundException The managed resource
     *            object could not be found
     * @exception InvalidTargetObjectTypeException The managed
     *            resource type should be "ObjectReference".
     * @exception RuntimeOperationsException Wraps a {@link
     *            RuntimeException} when setting the resource.
     **/
    public void setManagedResource(Object mr, String mr_type)
        throws MBeanException, RuntimeOperationsException,
               InstanceNotFoundException, InvalidTargetObjectTypeException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        // check that the mr_type is supported by this JMXAgent
        // only "objectReference" is supported
        if ((mr_type == null) ||
            (! mr_type.equalsIgnoreCase("objectReference"))) {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                    "Managed Resource Type is not supported: " + mr_type);
            }
            throw new InvalidTargetObjectTypeException(mr_type);
        }

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                "Managed Resource is valid");
        }
        managedResource = mr;

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }
    }

    /**
     * <p>Instantiates this MBean instance with the data found for
     * the MBean in the persistent store.  The data loaded could include
     * attribute and operation values.</p>
     *
     * <p>This method should be called during construction or
     * initialization of this instance, and before the MBean is
     * registered with the MBeanServer.</p>
     *
     * <p>If the implementation of this class does not support
     * persistence, an {@link MBeanException} wrapping a {@link
     * ServiceNotFoundException} is thrown.</p>
     *
     * @exception MBeanException Wraps another exception, or
     * persistence is not supported
     * @exception RuntimeOperationsException Wraps exceptions from the
     * persistence mechanism
     * @exception InstanceNotFoundException Could not find or load
     * this MBean from persistent storage
     */
    public void load()
        throws MBeanException, RuntimeOperationsException,
               InstanceNotFoundException {
        final ServiceNotFoundException x = new ServiceNotFoundException(
                                "Persistence not supported for this MBean");
        throw new MBeanException(x, x.getMessage());
    }

        /**
     * <p>Captures the current state of this MBean instance and writes
     * it out to the persistent store.  The state stored could include
     * attribute and operation values.</p>
     *
     * <p>If the implementation of this class does not support
     * persistence, an {@link MBeanException} wrapping a {@link
     * ServiceNotFoundException} is thrown.</p>
     *
     * <p>Persistence policy from the MBean and attribute descriptor
     * is used to guide execution of this method. The MBean should be
     * stored if 'persistPolicy' field is:</p>
     *
     * <PRE>{@literal  != "never"
     *   = "always"
     *   = "onTimer" and now > 'lastPersistTime' + 'persistPeriod'
     *   = "NoMoreOftenThan" and now > 'lastPersistTime' + 'persistPeriod'
     *   = "onUnregister"
     * }</PRE>
     *
     * <p>Do not store the MBean if 'persistPolicy' field is:</p>
     * <PRE>{@literal
     *    = "never"
     *    = "onUpdate"
     *    = "onTimer" && now < 'lastPersistTime' + 'persistPeriod'
     * }</PRE>
     *
     * @exception MBeanException Wraps another exception, or
     * persistence is not supported
     * @exception RuntimeOperationsException Wraps exceptions from the
     * persistence mechanism
     * @exception InstanceNotFoundException Could not find/access the
     * persistent store
     */
    public void store()
        throws MBeanException, RuntimeOperationsException,
               InstanceNotFoundException {
        final ServiceNotFoundException x = new ServiceNotFoundException(
                                "Persistence not supported for this MBean");
        throw new MBeanException(x, x.getMessage());
    }

    /*************************************/
    /* DynamicMBean Interface            */
    /*************************************/

    /**
     * The resolveForCacheValue method checks the descriptor passed in to
     * see if there is a valid cached value in the descriptor.
     * The valid value will be in the 'value' field if there is one.
     * If the 'currencyTimeLimit' field in the descriptor is:
     * <ul>
     *   <li><b>&lt;0</b> Then the value is not cached and is never valid.
     *         Null is returned. The 'value' and 'lastUpdatedTimeStamp'
     *         fields are cleared.</li>
     *   <li><b>=0</b> Then the value is always cached and always valid.
     *         The 'value' field is returned.
     *         The 'lastUpdatedTimeStamp' field is not checked.</li>
     *   <li><b>&gt;0</b> Represents the number of seconds that the
     *         'value' field is valid.
     *         The 'value' field is no longer valid when
     *         'lastUpdatedTimeStamp' + 'currencyTimeLimit' &gt; Now.
     *       <ul>
     *       <li>When 'value' is valid, 'valid' is returned.</li>
     *       <li>When 'value' is no longer valid then null is returned and
     *           'value' and 'lastUpdatedTimeStamp' fields are cleared.</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     **/
    private Object resolveForCacheValue(Descriptor descr)
        throws MBeanException, RuntimeOperationsException {

        final boolean tracing = MODELMBEAN_LOGGER.isLoggable(Level.TRACE);
        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        Object response = null;
        boolean resetValue = false, returnCachedValue = true;
        long currencyPeriod = 0;

        if (descr == null) {
            if (tracing) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                    "Input Descriptor is null");
            }
            return response;
        }

        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    "descriptor is " + descr);
        }

        final Descriptor mmbDescr = modelMBeanInfo.getMBeanDescriptor();
        if (mmbDescr == null) {
            if (tracing) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "MBean Descriptor is null");
            }
            //return response;
        }

        Object objExpTime = descr.getFieldValue("currencyTimeLimit");

        String expTime;
        if (objExpTime != null) {
            expTime = objExpTime.toString();
        } else {
            expTime = null;
        }

        if ((expTime == null) && (mmbDescr != null)) {
            objExpTime = mmbDescr.getFieldValue("currencyTimeLimit");
            if (objExpTime != null) {
                expTime = objExpTime.toString();
            } else {
                expTime = null;
            }
        }

        if (expTime != null) {
            if (tracing) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "currencyTimeLimit: " + expTime);
            }

            // convert seconds to milliseconds for time comparison
            currencyPeriod = Long.parseLong(expTime) * 1000;
            if (currencyPeriod < 0) {
                /* if currencyTimeLimit is -1 then value is never cached */
                returnCachedValue = false;
                resetValue = true;
                if (tracing) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                        currencyPeriod + ": never Cached");
                }
            } else if (currencyPeriod == 0) {
                /* if currencyTimeLimit is 0 then value is always cached */
                returnCachedValue = true;
                resetValue = false;
                if (tracing) {
                    MODELMBEAN_LOGGER.log(Level.TRACE, "always valid Cache");
                }
            } else {
                Object objtStamp =
                    descr.getFieldValue("lastUpdatedTimeStamp");

                String tStamp;
                if (objtStamp != null) tStamp = objtStamp.toString();
                else tStamp = null;

                if (tracing) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                        "lastUpdatedTimeStamp: " + tStamp);
                }

                if (tStamp == null)
                    tStamp = "0";

                long lastTime = Long.parseLong(tStamp);

                if (tracing) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                        "currencyPeriod:" + currencyPeriod +
                        " lastUpdatedTimeStamp:" + lastTime);
                }

                long now = (new Date()).getTime();

                if (now < (lastTime + currencyPeriod)) {
                    returnCachedValue = true;
                    resetValue = false;
                    if (tracing) {
                        MODELMBEAN_LOGGER.log(Level.TRACE,
                            " timed valid Cache for " + now + " < " +
                            (lastTime + currencyPeriod));
                    }
                } else { /* value is expired */
                    returnCachedValue = false;
                    resetValue = true;
                    if (tracing) {
                        MODELMBEAN_LOGGER.log(Level.TRACE,
                            "timed expired cache for " + now + " > " +
                            (lastTime + currencyPeriod));
                    }
                }
            }
            if (tracing) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                    "returnCachedValue:" + returnCachedValue +
                    " resetValue: " + resetValue);
            }

            if (returnCachedValue == true) {
                Object currValue = descr.getFieldValue("value");
                if (currValue != null) {
                    /* error/validity check return value here */
                    response = currValue;
                    /* need to cast string cached value to type */
                    if (tracing) {
                        MODELMBEAN_LOGGER.log(Level.TRACE,
                            "valid Cache value: " + currValue);
                    }

                } else {
                    response = null;
                    if (tracing) {
                        MODELMBEAN_LOGGER.log(Level.TRACE,
                                 "no Cached value");
                    }
                }
            }

            if (resetValue == true) {
                /* value is not current, so remove it */
                descr.removeField("lastUpdatedTimeStamp");
                descr.removeField("value");
                response = null;
                modelMBeanInfo.setDescriptor(descr,null);
                if (tracing) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                        "reset cached value to null");
                }
            }
        }

        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

        return response;
    }

    /**
     * Returns the attributes, operations, constructors and notifications
     * that this RequiredModelMBean exposes for management.
     *
     * @return  An instance of ModelMBeanInfo allowing retrieval all
     *          attributes, operations, and Notifications of this MBean.
     *
     **/
    public MBeanInfo getMBeanInfo() {

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (modelMBeanInfo == null) {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "modelMBeanInfo is null");
            }
            modelMBeanInfo = createDefaultModelMBeanInfo();
            //return new ModelMBeanInfo(" ", "", null, null, null, null);
        }

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "ModelMBeanInfo is " +
                  modelMBeanInfo.getClassName() + " for " +
                  modelMBeanInfo.getDescription());
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    printModelMBeanInfo(modelMBeanInfo));
        }

        return((MBeanInfo) modelMBeanInfo.clone());
    }

    private String printModelMBeanInfo(ModelMBeanInfo info) {
        final StringBuilder retStr = new StringBuilder();
        if (info == null) {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "ModelMBeanInfo to print is null, " +
                        "printing local ModelMBeanInfo");
            }
            info = modelMBeanInfo;
        }

        retStr.append("\nMBeanInfo for ModelMBean is:");
        retStr.append("\nCLASSNAME: \t").append(info.getClassName());
        retStr.append("\nDESCRIPTION: \t").append(info.getDescription());


        try {
            retStr.append("\nMBEAN DESCRIPTOR: \t").append(info.getMBeanDescriptor());
        } catch (Exception e) {
            retStr.append("\nMBEAN DESCRIPTOR: \t  is invalid");
        }

        retStr.append("\nATTRIBUTES");

        final MBeanAttributeInfo[] attrInfo = info.getAttributes();
        if ((attrInfo != null) && (attrInfo.length>0)) {
            for (int i=0; i<attrInfo.length; i++) {
                final ModelMBeanAttributeInfo attInfo =
                    (ModelMBeanAttributeInfo)attrInfo[i];
                retStr.append(" ** NAME: \t").append(attInfo.getName());
                retStr.append("    DESCR: \t").append(attInfo.getDescription());
                retStr.append("    TYPE: \t").append(attInfo.getType())
                        .append("    READ: \t").append(attInfo.isReadable())
                        .append("    WRITE: \t").append(attInfo.isWritable());
                retStr.append("    DESCRIPTOR: ").append(attInfo.getDescriptor());
            }
        } else {
            retStr.append(" ** No attributes **");
        }

        retStr.append("\nCONSTRUCTORS");
        final MBeanConstructorInfo[] constrInfo = info.getConstructors();
        if ((constrInfo != null) && (constrInfo.length > 0 )) {
            for (int i=0; i<constrInfo.length; i++) {
                final ModelMBeanConstructorInfo ctorInfo =
                    (ModelMBeanConstructorInfo)constrInfo[i];
                retStr.append(" ** NAME: \t").append(ctorInfo.getName());
                retStr.append("    DESCR: \t").append(ctorInfo.getDescription());
                retStr.append("    PARAM: \t")
                        .append(ctorInfo.getSignature().length)
                        .append(" parameter(s)");
                retStr.append("    DESCRIPTOR: ").append(
                        ctorInfo.getDescriptor());
            }
        } else {
            retStr.append(" ** No Constructors **");
        }

        retStr.append("\nOPERATIONS");
        final MBeanOperationInfo[] opsInfo = info.getOperations();
        if ((opsInfo != null) && (opsInfo.length>0)) {
            for (int i=0; i<opsInfo.length; i++) {
                final ModelMBeanOperationInfo operInfo =
                    (ModelMBeanOperationInfo)opsInfo[i];
                retStr.append(" ** NAME: \t").append(operInfo.getName());
                retStr.append("    DESCR: \t").append(operInfo.getDescription());
                retStr.append("    PARAM: \t")
                        .append(operInfo.getSignature().length)
                        .append(" parameter(s)");
                retStr.append("    DESCRIPTOR: ").append(operInfo.getDescriptor());
            }
        } else {
            retStr.append(" ** No operations ** ");
        }

        retStr.append("\nNOTIFICATIONS");

        MBeanNotificationInfo[] notifInfo = info.getNotifications();
        if ((notifInfo != null) && (notifInfo.length>0)) {
            for (int i=0; i<notifInfo.length; i++) {
                final ModelMBeanNotificationInfo nInfo =
                    (ModelMBeanNotificationInfo)notifInfo[i];
                retStr.append(" ** NAME: \t").append(nInfo.getName());
                retStr.append("    DESCR: \t").append(nInfo.getDescription());
                retStr.append("    DESCRIPTOR: ").append(nInfo.getDescriptor());
            }
        } else {
            retStr.append(" ** No notifications **");
        }

        retStr.append(" ** ModelMBean: End of MBeanInfo ** ");

        return retStr.toString();
    }

    /**
     * Invokes a method on or through a RequiredModelMBean and returns
     * the result of the method execution.
     * <P>
     * If the given method to be invoked, together with the provided
     * signature, matches one of RequiredModelMbean
     * accessible methods, this one will be call. Otherwise the call to
     * the given method will be tried on the managed resource.
     * <P>
     * The last value returned by an operation may be cached in
     * the operation's descriptor which
     * is in the ModelMBeanOperationInfo's descriptor.
     * The valid value will be in the 'value' field if there is one.
     * If the 'currencyTimeLimit' field in the descriptor is:
     * <UL>
     * <LI><b>&lt;0</b> Then the value is not cached and is never valid.
     *      The operation method is invoked.
     *      The 'value' and 'lastUpdatedTimeStamp' fields are cleared.</LI>
     * <LI><b>=0</b> Then the value is always cached and always valid.
     *      The 'value' field is returned. If there is no 'value' field
     *      then the operation method is invoked for the attribute.
     *      The 'lastUpdatedTimeStamp' field and `value' fields are set to
     *      the operation's return value and the current time stamp.</LI>
     * <LI><b>&gt;0</b> Represents the number of seconds that the 'value'
     *      field is valid.
     *      The 'value' field is no longer valid when
     *      'lastUpdatedTimeStamp' + 'currencyTimeLimit' &gt; Now.
     *      <UL>
     *         <LI>When 'value' is valid, 'value' is returned.</LI>
     *         <LI>When 'value' is no longer valid then the operation
     *             method is invoked. The 'lastUpdatedTimeStamp' field
     *             and `value' fields are updated.</lI>
     *      </UL>
     * </LI>
     * </UL>
     *
     * <p><b>Note:</b> because of inconsistencies in previous versions of
     * this specification, it is recommended not to use negative or zero
     * values for <code>currencyTimeLimit</code>.  To indicate that a
     * cached value is never valid, omit the
     * <code>currencyTimeLimit</code> field.  To indicate that it is
     * always valid, use a very large number for this field.</p>
     *
     * @param opName The name of the method to be invoked. The
     *     name can be the fully qualified method name including the
     *     classname, or just the method name if the classname is
     *     defined in the 'class' field of the operation descriptor.
     * @param opArgs An array containing the parameters to be set
     *     when the operation is invoked
     * @param sig An array containing the signature of the
     *     operation. The class objects will be loaded using the same
     *     class loader as the one used for loading the MBean on which
     *     the operation was invoked.
     *
     * @return  The object returned by the method, which represents the
     *     result of invoking the method on the specified managed resource.
     *
     * @exception MBeanException  Wraps one of the following Exceptions:
     * <UL>
     * <LI> An Exception thrown by the managed object's invoked method.</LI>
     * <LI> {@link ServiceNotFoundException}: No ModelMBeanOperationInfo or
     *      no descriptor defined for the specified operation or the managed
     *      resource is null.</LI>
     * <LI> {@link InvalidTargetObjectTypeException}: The 'targetType'
     *      field value is not 'objectReference'.</LI>
     * </UL>
     * @exception ReflectionException  Wraps an {@link java.lang.Exception}
     *      thrown while trying to invoke the method.
     * @exception RuntimeOperationsException Wraps an
     *      {@link IllegalArgumentException} Method name is null.
     *
     **/
    /*
      The requirement to be able to invoke methods on the
      RequiredModelMBean class itself makes this method considerably
      more complicated than it might otherwise be.  Note that, unlike
      earlier versions, we do not allow you to invoke such methods if
      they are not explicitly mentioned in the ModelMBeanInfo.  Doing
      so was potentially a security problem, and certainly very
      surprising.

      We do not look for the method in the RequiredModelMBean class
      itself if:
      (a) there is a "targetObject" field in the Descriptor for the
      operation; or
      (b) there is a "class" field in the Descriptor for the operation
      and the named class is not RequiredModelMBean or one of its
      superinterfaces; or
      (c) the name of the operation is not the name of a method in
      RequiredModelMBean (this is just an optimization).

      In cases (a) and (b), if you have gone to the trouble of adding
      those fields specifically for this operation then presumably you
      do not want RequiredModelMBean's methods to be called.

      We have to pay attention to class loading issues.  If the
      "class" field is present, the named class has to be resolved
      relative to RequiredModelMBean's class loader to test the
      condition (b) above, and relative to the managed resource's
      class loader to ensure that the managed resource is in fact of
      the named class (or a subclass).  The class names in the sig
      array likewise have to be resolved, first against
      RequiredModelMBean's class loader, then against the managed
      resource's class loader.  There is no point in using any other
      loader because when we call Method.invoke we must call it on
      a Method that is implemented by the target object.
     */
    public Object invoke(String opName, Object[] opArgs, String[] sig)
            throws MBeanException, ReflectionException {

        final boolean tracing = MODELMBEAN_LOGGER.isLoggable(Level.TRACE);

        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (opName == null) {
            final RuntimeException x =
                new IllegalArgumentException("Method name must not be null");
            throw new RuntimeOperationsException(x,
                      "An exception occurred while trying to " +
                      "invoke a method on a RequiredModelMBean");
        }

        String opClassName = null;
        String opMethodName;

        // Parse for class name and method
        int opSplitter = opName.lastIndexOf('.');
        if (opSplitter > 0) {
            opClassName = opName.substring(0,opSplitter);
            opMethodName = opName.substring(opSplitter+1);
        } else
            opMethodName = opName;

        /* Ignore anything after a left paren.  We keep this for
           compatibility but it isn't specified.  */
        opSplitter = opMethodName.indexOf('(');
        if (opSplitter > 0)
            opMethodName = opMethodName.substring(0,opSplitter);

        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    "Finding operation " + opName + " as " + opMethodName);
        }

        ModelMBeanOperationInfo opInfo =
            modelMBeanInfo.getOperation(opMethodName);
        if (opInfo == null) {
            final String msg =
                "Operation " + opName + " not in ModelMBeanInfo";
            throw new MBeanException(new ServiceNotFoundException(msg), msg);
        }

        final Descriptor opDescr = opInfo.getDescriptor();
        if (opDescr == null) {
            final String msg = "Operation descriptor null";
            throw new MBeanException(new ServiceNotFoundException(msg), msg);
        }

        final Object cached = resolveForCacheValue(opDescr);
        if (cached != null) {
            if (tracing) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Returning cached value");
            }
            return cached;
        }

        if (opClassName == null)
            opClassName = (String) opDescr.getFieldValue("class");
        // may still be null now

        opMethodName = (String) opDescr.getFieldValue("name");
        if (opMethodName == null) {
            final String msg =
                "Method descriptor must include `name' field";
            throw new MBeanException(new ServiceNotFoundException(msg), msg);
        }

        final String targetTypeField = (String)
            opDescr.getFieldValue("targetType");
        if (targetTypeField != null
            && !targetTypeField.equalsIgnoreCase("objectReference")) {
            final String msg =
                "Target type must be objectReference: " + targetTypeField;
            throw new MBeanException(new InvalidTargetObjectTypeException(msg),
                                     msg);
        }

        final Object targetObjectField = opDescr.getFieldValue("targetObject");
        if (tracing && targetObjectField != null)
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "Found target object in descriptor");

        /* Now look for the method, either in RequiredModelMBean itself
           or in the target object.  Set "method" and "targetObject"
           appropriately.  */
        Method method;
        Object targetObject;

        method = findRMMBMethod(opMethodName, targetObjectField,
                                opClassName, sig);

        if (method != null)
            targetObject = this;
        else {
            if (tracing) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "looking for method in managedResource class");
            }
            if (targetObjectField != null)
                targetObject = targetObjectField;
            else {
                targetObject = managedResource;
                if (targetObject == null) {
                    final String msg =
                        "managedResource for invoke " + opName +
                        " is null";
                    Exception snfe = new ServiceNotFoundException(msg);
                    throw new MBeanException(snfe);
                }
            }

            final Class<?> targetClass;

            if (opClassName != null) {
                try {
                    @SuppressWarnings("removal")
                    AccessControlContext stack = AccessController.getContext();
                    final Object obj = targetObject;
                    final String className = opClassName;
                    final ClassNotFoundException[] caughtException = new ClassNotFoundException[1];

                    targetClass = javaSecurityAccess.doIntersectionPrivilege(new PrivilegedAction<Class<?>>() {

                        @Override
                        public Class<?> run() {
                            try {
                                ReflectUtil.checkPackageAccess(className);
                                final ClassLoader targetClassLoader =
                                    obj.getClass().getClassLoader();
                                return Class.forName(className, false,
                                                            targetClassLoader);
                            } catch (ClassNotFoundException e) {
                                caughtException[0] = e;
                            }
                            return null;
                        }
                    }, stack, acc);

                    if (caughtException[0] != null) {
                        throw caughtException[0];
                    }
                } catch (ClassNotFoundException e) {
                    final String msg =
                        "class for invoke " + opName + " not found";
                    throw new ReflectionException(e, msg);
                }
            } else
                targetClass = targetObject.getClass();

            method = resolveMethod(targetClass, opMethodName, sig);
        }

        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    "found " + opMethodName + ", now invoking");
        }

        final Object result =
            invokeMethod(opName, method, targetObject, opArgs);

        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "successfully invoked method");
        }

        if (result != null)
            cacheResult(opInfo, opDescr, result);

        return result;
    }

    private Method resolveMethod(Class<?> targetClass,
                                        String opMethodName,
                                        final String[] sig)
            throws ReflectionException {
        final boolean tracing = MODELMBEAN_LOGGER.isLoggable(Level.TRACE);

        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    "resolving " + targetClass.getName() + "." + opMethodName);
        }

        final Class<?>[] argClasses;

        if (sig == null)
            argClasses = null;
        else {
            @SuppressWarnings("removal")
            final AccessControlContext stack = AccessController.getContext();
            final ReflectionException[] caughtException = new ReflectionException[1];
            final ClassLoader targetClassLoader = targetClass.getClassLoader();
            argClasses = new Class<?>[sig.length];

            javaSecurityAccess.doIntersectionPrivilege(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    for (int i = 0; i < sig.length; i++) {
                        if (tracing) {
                            MODELMBEAN_LOGGER.log(Level.TRACE,
                                    "resolve type " + sig[i]);
                        }
                        argClasses[i] = (Class<?>) primitiveClassMap.get(sig[i]);
                        if (argClasses[i] == null) {
                            try {
                                ReflectUtil.checkPackageAccess(sig[i]);
                                argClasses[i] =
                                    Class.forName(sig[i], false, targetClassLoader);
                            } catch (ClassNotFoundException e) {
                                if (tracing) {
                                    MODELMBEAN_LOGGER.log(Level.TRACE,
                                            "class not found");
                                }
                                final String msg = "Parameter class not found";
                                caughtException[0] = new ReflectionException(e, msg);
                            }
                        }
                    }
                    return null;
                }
            }, stack, acc);

            if (caughtException[0] != null) {
                throw caughtException[0];
            }
        }

        try {
            return targetClass.getMethod(opMethodName, argClasses);
        } catch (NoSuchMethodException e) {
            final String msg =
                "Target method not found: " + targetClass.getName() + "." +
                opMethodName;
            throw new ReflectionException(e, msg);
        }
    }

    /* Map e.g. "int" to int.class.  Goodness knows how many time this
       particular wheel has been reinvented.  */
    private static final Class<?>[] primitiveClasses = {
        int.class, long.class, boolean.class, double.class,
        float.class, short.class, byte.class, char.class,
    };
    private static final Map<String,Class<?>> primitiveClassMap =
        new HashMap<String,Class<?>>();
    static {
        for (int i = 0; i < primitiveClasses.length; i++) {
            final Class<?> c = primitiveClasses[i];
            primitiveClassMap.put(c.getName(), c);
        }
    }

    /* Find a method in RequiredModelMBean as determined by the given
       parameters.  Return null if there is none, or if the parameters
       exclude using it.  Called from invoke. */
    private Method findRMMBMethod(String opMethodName,
                                         Object targetObjectField,
                                         String opClassName,
                                         String[] sig) {
        final boolean tracing = MODELMBEAN_LOGGER.isLoggable(Level.TRACE);

        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                  "looking for method in RequiredModelMBean class");
        }

        if (!isRMMBMethodName(opMethodName))
            return null;
        if (targetObjectField != null)
            return null;
        final Class<RequiredModelMBean> rmmbClass = RequiredModelMBean.class;
        final Class<?> targetClass;
        if (opClassName == null)
            targetClass = rmmbClass;
        else {
            @SuppressWarnings("removal")
            AccessControlContext stack = AccessController.getContext();
            final String className = opClassName;
            targetClass = javaSecurityAccess.doIntersectionPrivilege(new PrivilegedAction<Class<?>>() {

                @Override
                public Class<?> run() {
                    try {
                        ReflectUtil.checkPackageAccess(className);
                        final ClassLoader targetClassLoader =
                            rmmbClass.getClassLoader();
                        Class<?> clz = Class.forName(className, false,
                                                    targetClassLoader);
                        if (!rmmbClass.isAssignableFrom(clz))
                            return null;
                        return clz;
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
            }, stack, acc);
        }
        try {
            return targetClass != null ? resolveMethod(targetClass, opMethodName, sig) : null;
        } catch (ReflectionException e) {
            return null;
        }
    }

    /*
     * Invoke the given method, and throw the somewhat unpredictable
     * appropriate exception if the method itself gets an exception.
     */
    private Object invokeMethod(String opName, final Method method,
                                final Object targetObject, final Object[] opArgs)
            throws MBeanException, ReflectionException {
        try {
            final Throwable[] caughtException = new Throwable[1];
            @SuppressWarnings("removal")
            AccessControlContext stack = AccessController.getContext();
            Object rslt = javaSecurityAccess.doIntersectionPrivilege(new PrivilegedAction<Object>() {

                @Override
                public Object run() {
                    try {
                        ReflectUtil.checkPackageAccess(method.getDeclaringClass());
                        return MethodUtil.invoke(method, targetObject, opArgs);
                    } catch (InvocationTargetException e) {
                        caughtException[0] = e;
                    } catch (IllegalAccessException e) {
                        caughtException[0] = e;
                    }
                    return null;
                }
            }, stack, acc);
            if (caughtException[0] != null) {
                if (caughtException[0] instanceof Exception) {
                    throw (Exception)caughtException[0];
                } else if(caughtException[0] instanceof Error) {
                    throw (Error)caughtException[0];
                }
            }
            return rslt;
        } catch (RuntimeErrorException ree) {
            throw new RuntimeOperationsException(ree,
                      "RuntimeException occurred in RequiredModelMBean "+
                      "while trying to invoke operation " + opName);
        } catch (RuntimeException re) {
            throw new RuntimeOperationsException(re,
                      "RuntimeException occurred in RequiredModelMBean "+
                      "while trying to invoke operation " + opName);
        } catch (IllegalAccessException iae) {
            throw new ReflectionException(iae,
                      "IllegalAccessException occurred in " +
                      "RequiredModelMBean while trying to " +
                      "invoke operation " + opName);
        } catch (InvocationTargetException ite) {
            Throwable mmbTargEx = ite.getCause();
            if (mmbTargEx instanceof RuntimeException) {
                throw new MBeanException ((RuntimeException)mmbTargEx,
                      "RuntimeException thrown in RequiredModelMBean "+
                      "while trying to invoke operation " + opName);
            } else if (mmbTargEx instanceof Error) {
                throw new RuntimeErrorException((Error)mmbTargEx,
                      "Error occurred in RequiredModelMBean while trying "+
                      "to invoke operation " + opName);
            } else if (mmbTargEx instanceof ReflectionException) {
                throw (ReflectionException) mmbTargEx;
            } else {
                throw new MBeanException ((Exception)mmbTargEx,
                      "Exception thrown in RequiredModelMBean "+
                      "while trying to invoke operation " + opName);
            }
        } catch (Error err) {
            throw new RuntimeErrorException(err,
                  "Error occurred in RequiredModelMBean while trying "+
                  "to invoke operation " + opName);
        } catch (Exception e) {
            throw new ReflectionException(e,
                  "Exception occurred in RequiredModelMBean while " +
                  "trying to invoke operation " + opName);
        }
    }

    /*
     * Cache the result of an operation in the descriptor, if that is
     * called for by the descriptor's configuration.  Note that we
     * don't remember operation parameters when caching the result, so
     * this is unlikely to be useful if there are any.
     */
    private void cacheResult(ModelMBeanOperationInfo opInfo,
                             Descriptor opDescr, Object result)
            throws MBeanException {

        Descriptor mmbDesc =
            modelMBeanInfo.getMBeanDescriptor();

        Object objctl =
            opDescr.getFieldValue("currencyTimeLimit");
        String ctl;
        if (objctl != null) {
            ctl = objctl.toString();
        } else {
            ctl = null;
        }
        if ((ctl == null) && (mmbDesc != null)) {
            objctl =
                mmbDesc.getFieldValue("currencyTimeLimit");
            if (objctl != null) {
                ctl = objctl.toString();
            } else {
                ctl = null;
            }
        }
        if ((ctl != null) && !(ctl.equals("-1"))) {
            opDescr.setField("value", result);
            opDescr.setField("lastUpdatedTimeStamp",
                    String.valueOf((new Date()).getTime()));


            modelMBeanInfo.setDescriptor(opDescr,
                                         "operation");
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "new descriptor is " + opDescr);
            }
        }
    }

    /*
     * Determine whether the given name is the name of a public method
     * in this class.  This is only an optimization: it prevents us
     * from trying to do argument type lookups and reflection on a
     * method that will obviously fail because it has the wrong name.
     *
     * The first time this method is called we do the reflection, and
     * every other time we reuse the remembered values.
     *
     * It's conceivable that the (possibly malicious) first caller
     * doesn't have the required permissions to do reflection, in
     * which case we don't touch anything so as not to interfere
     * with a later permissionful caller.
     */
    private static Set<String> rmmbMethodNames;
    private static synchronized boolean isRMMBMethodName(String name) {
        if (rmmbMethodNames == null) {
            try {
                Set<String> names = new HashSet<String>();
                Method[] methods = RequiredModelMBean.class.getMethods();
                for (int i = 0; i < methods.length; i++)
                    names.add(methods[i].getName());
                rmmbMethodNames = names;
            } catch (Exception e) {
                return true;
                // This is only an optimization so we'll go on to discover
                // whether the name really is an RMMB method.
            }
        }
        return rmmbMethodNames.contains(name);
    }

    /**
     * Returns the value of a specific attribute defined for this
     * ModelMBean.
     * The last value returned by an attribute may be cached in the
     * attribute's descriptor.
     * The valid value will be in the 'value' field if there is one.
     * If the 'currencyTimeLimit' field in the descriptor is:
     * <UL>
     * <LI>  <b>&lt;0</b> Then the value is not cached and is never valid.
     *       The getter method is invoked for the attribute.
     *       The 'value' and 'lastUpdatedTimeStamp' fields are cleared.</LI>
     * <LI>  <b>=0</b> Then the value is always cached and always valid.
     *       The 'value' field is returned. If there is no'value' field
     *       then the getter method is invoked for the attribute.
     *       The 'lastUpdatedTimeStamp' field and `value' fields are set
     *       to the attribute's value and the current time stamp.</LI>
     * <LI>  <b>&gt;0</b> Represents the number of seconds that the 'value'
     *       field is valid.
     *       The 'value' field is no longer valid when
     *       'lastUpdatedTimeStamp' + 'currencyTimeLimit' &gt; Now.
     *   <UL>
     *        <LI>When 'value' is valid, 'value' is returned.</LI>
     *        <LI>When 'value' is no longer valid then the getter
     *            method is invoked for the attribute.
     *            The 'lastUpdatedTimeStamp' field and `value' fields
     *            are updated.</LI>
     *   </UL></LI>
     * </UL>
     *
     * <p><b>Note:</b> because of inconsistencies in previous versions of
     * this specification, it is recommended not to use negative or zero
     * values for <code>currencyTimeLimit</code>.  To indicate that a
     * cached value is never valid, omit the
     * <code>currencyTimeLimit</code> field.  To indicate that it is
     * always valid, use a very large number for this field.</p>
     *
     * <p>If the 'getMethod' field contains the name of a valid
     * operation descriptor, then the method described by the
     * operation descriptor is executed.  The response from the
     * method is returned as the value of the attribute.  If the
     * operation fails or the returned value is not compatible with
     * the declared type of the attribute, an exception will be thrown.</p>
     *
     * <p>If no 'getMethod' field is defined then the default value of the
     * attribute is returned. If the returned value is not compatible with
     * the declared type of the attribute, an exception will be thrown.</p>
     *
     * <p>The declared type of the attribute is the String returned by
     * {@link ModelMBeanAttributeInfo#getType()}.  A value is compatible
     * with this type if one of the following is true:
     * <ul>
     * <li>the value is null;</li>
     * <li>the declared name is a primitive type name (such as "int")
     *     and the value is an instance of the corresponding wrapper
     *     type (such as java.lang.Integer);</li>
     * <li>the name of the value's class is identical to the declared name;</li>
     * <li>the declared name can be loaded by the value's class loader and
     *     produces a class to which the value can be assigned.</li>
     * </ul>
     *
     * <p>In this implementation, in every case where the getMethod needs to
     * be called, because the method is invoked through the standard "invoke"
     * method and thus needs operationInfo, an operation must be specified
     * for that getMethod so that the invocation works correctly.</p>
     *
     * @param attrName A String specifying the name of the
     * attribute to be retrieved. It must match the name of a
     * ModelMBeanAttributeInfo.
     *
     * @return The value of the retrieved attribute from the
     * descriptor 'value' field or from the invocation of the
     * operation in the 'getMethod' field of the descriptor.
     *
     * @exception AttributeNotFoundException The specified attribute is
     *    not accessible in the MBean.
     *    The following cases may result in an AttributeNotFoundException:
     *    <UL>
     *      <LI> No ModelMBeanInfo was found for the Model MBean.</LI>
     *      <LI> No ModelMBeanAttributeInfo was found for the specified
     *           attribute name.</LI>
     *      <LI> The ModelMBeanAttributeInfo isReadable method returns
     *           'false'.</LI>
     *    </UL>
     * @exception MBeanException  Wraps one of the following Exceptions:
     *    <UL>
     *      <LI> {@link InvalidAttributeValueException}: A wrong value type
     *           was received from the attribute's getter method or
     *           no 'getMethod' field defined in the descriptor for
     *           the attribute and no default value exists.</LI>
     *      <LI> {@link ServiceNotFoundException}: No
     *           ModelMBeanOperationInfo defined for the attribute's
     *           getter method or no descriptor associated with the
     *           ModelMBeanOperationInfo or the managed resource is
     *           null.</LI>
     *      <LI> {@link InvalidTargetObjectTypeException} The 'targetType'
     *           field value is not 'objectReference'.</LI>
     *      <LI> An Exception thrown by the managed object's getter.</LI>
     *    </UL>
     * @exception ReflectionException  Wraps an {@link java.lang.Exception}
     *    thrown while trying to invoke the getter.
     * @exception RuntimeOperationsException Wraps an
     *    {@link IllegalArgumentException}: The attribute name in
     *    parameter is null.
     *
     * @see #setAttribute(javax.management.Attribute)
     **/
    public Object getAttribute(String attrName)
        throws AttributeNotFoundException, MBeanException,
               ReflectionException {
        if (attrName == null)
            throw new RuntimeOperationsException(new
                IllegalArgumentException("attributeName must not be null"),
                "Exception occurred trying to get attribute of a " +
                "RequiredModelMBean");
        final boolean tracing = MODELMBEAN_LOGGER.isLoggable(Level.TRACE);
        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry with " + attrName);
        }

        /* Check attributeDescriptor for getMethod */
        Object response;

        try {
            if (modelMBeanInfo == null)
                throw new AttributeNotFoundException(
                      "getAttribute failed: ModelMBeanInfo not found for "+
                      attrName);

            ModelMBeanAttributeInfo attrInfo = modelMBeanInfo.getAttribute(attrName);
            Descriptor mmbDesc = modelMBeanInfo.getMBeanDescriptor();

            if (attrInfo == null)
                throw new AttributeNotFoundException("getAttribute failed:"+
                      " ModelMBeanAttributeInfo not found for " + attrName);

            Descriptor attrDescr = attrInfo.getDescriptor();
            if (attrDescr != null) {
                if (!attrInfo.isReadable())
                    throw new AttributeNotFoundException(
                          "getAttribute failed: " + attrName +
                          " is not readable ");

                response = resolveForCacheValue(attrDescr);

                /* return current cached value */
                if (tracing) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "*** cached value is " + response);
                }

                if (response == null) {
                    /* no cached value, run getMethod */
                    if (tracing) {
                        MODELMBEAN_LOGGER.log(Level.TRACE,
                            "**** cached value is null - getting getMethod");
                    }
                    String attrGetMethod =
                        (String)(attrDescr.getFieldValue("getMethod"));

                    if (attrGetMethod != null) {
                        /* run method from operations descriptor */
                        if (tracing) {
                            MODELMBEAN_LOGGER.log(Level.TRACE,
                                "invoking a getMethod for " +  attrName);
                        }

                        Object getResponse =
                            invoke(attrGetMethod, new Object[] {},
                                   new String[] {});

                        if (getResponse != null) {
                            // error/validity check return value here
                            if (tracing) {
                                MODELMBEAN_LOGGER.log(Level.TRACE,
                                        "got a non-null response " +
                                        "from getMethod\n");
                            }

                            response = getResponse;

                            // change cached value in attribute descriptor
                            Object objctl =
                                attrDescr.getFieldValue("currencyTimeLimit");

                            String ctl;
                            if (objctl != null) ctl = objctl.toString();
                            else ctl = null;

                            if ((ctl == null) && (mmbDesc != null)) {
                                objctl = mmbDesc.
                                    getFieldValue("currencyTimeLimit");
                                if (objctl != null) ctl = objctl.toString();
                                else ctl = null;
                            }

                            if ((ctl != null) && !(ctl.equals("-1"))) {
                                if (tracing) {
                                    MODELMBEAN_LOGGER.log(Level.TRACE,
                                            "setting cached value and " +
                                            "lastUpdatedTime in descriptor");
                                }
                                attrDescr.setField("value", response);
                                final String stamp = String.valueOf(
                                    (new Date()).getTime());
                                attrDescr.setField("lastUpdatedTimeStamp",
                                                   stamp);
                                attrInfo.setDescriptor(attrDescr);
                                modelMBeanInfo.setDescriptor(attrDescr,
                                                             "attribute");
                                if (tracing) {
                                    MODELMBEAN_LOGGER.log(Level.TRACE,
                                            "new descriptor is " +attrDescr);
                                    MODELMBEAN_LOGGER.log(Level.TRACE,
                                            "AttributeInfo descriptor is " +
                                            attrInfo.getDescriptor());
                                    final String attStr = modelMBeanInfo.
                                        getDescriptor(attrName,"attribute").
                                            toString();
                                    MODELMBEAN_LOGGER.log(Level.TRACE,
                                            "modelMBeanInfo: AttributeInfo " +
                                            "descriptor is " + attStr);
                                }
                            }
                        } else {
                            // response was invalid or really returned null
                            if (tracing) {
                                MODELMBEAN_LOGGER.log(Level.TRACE,
                                    "got a null response from getMethod\n");
                            }
                            response = null;
                        }
                    } else {
                        // not getMethod so return descriptor (default) value
                        String qualifier="";
                        response = attrDescr.getFieldValue("value");
                        if (response == null) {
                            qualifier="default ";
                            response = attrDescr.getFieldValue("default");
                        }
                        if (tracing) {
                            MODELMBEAN_LOGGER.log(Level.TRACE,
                                "could not find getMethod for " +attrName +
                                ", returning descriptor " +qualifier + "value");
                        }
                        // !! cast response to right class
                    }
                }

                // make sure response class matches type field
                final String respType = attrInfo.getType();
                if (response != null) {
                    String responseClass = response.getClass().getName();
                    if (!respType.equals(responseClass)) {
                        boolean wrongType = false;
                        boolean primitiveType = false;
                        boolean correspondingTypes = false;
                        for (int i = 0; i < primitiveTypes.length; i++) {
                            if (respType.equals(primitiveTypes[i])) {
                                primitiveType = true;
                                if (responseClass.equals(primitiveWrappers[i]))
                                    correspondingTypes = true;
                                break;
                            }
                        }
                        if (primitiveType) {
                            // inequality may come from primitive/wrapper class
                            if (!correspondingTypes)
                                wrongType = true;
                        } else {
                            // inequality may come from type subclassing
                            boolean subtype;
                            try {
                                final Class<?> respClass = response.getClass();
                                final Exception[] caughException = new Exception[1];

                                @SuppressWarnings("removal")
                                AccessControlContext stack = AccessController.getContext();

                                Class<?> c = javaSecurityAccess.doIntersectionPrivilege(new PrivilegedAction<Class<?>>() {

                                    @Override
                                    public Class<?> run() {
                                        try {
                                            ReflectUtil.checkPackageAccess(respType);
                                            ClassLoader cl =
                                                respClass.getClassLoader();
                                            return Class.forName(respType, true, cl);
                                        } catch (Exception e) {
                                            caughException[0] = e;
                                        }
                                        return null;
                                    }
                                }, stack, acc);

                                if (caughException[0] != null) {
                                    throw caughException[0];
                                }

                                subtype = c.isInstance(response);
                            } catch (Exception e) {
                                subtype = false;

                                if (tracing) {
                                    MODELMBEAN_LOGGER.log(Level.TRACE,
                                            "Exception: ", e);
                                }
                            }
                            if (!subtype)
                                wrongType = true;
                        }
                        if (wrongType) {
                            if (tracing) {
                                MODELMBEAN_LOGGER.log(Level.TRACE,
                                     "Wrong response type '" + respType + "'");
                            }
                            // throw exception, didn't get
                            // back right attribute type
                            throw new MBeanException(
                              new InvalidAttributeValueException(
                                "Wrong value type received for get attribute"),
                              "An exception occurred while trying to get an " +
                              "attribute value through a RequiredModelMBean");
                        }
                    }
                }
            } else {
                if (tracing) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "getMethod failed " + attrName +
                            " not in attributeDescriptor\n");
                }
                throw new MBeanException(new
                    InvalidAttributeValueException(
                    "Unable to resolve attribute value, " +
                    "no getMethod defined in descriptor for attribute"),
                    "An exception occurred while trying to get an "+
                    "attribute value through a RequiredModelMBean");
            }

        } catch (MBeanException mbe) {
            throw mbe;
        } catch (AttributeNotFoundException t) {
            throw t;
        } catch (Exception e) {
            if (tracing) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "getMethod failed with " + e.getMessage() +
                        " exception type " + (e.getClass()).toString());
            }
            throw new MBeanException(e,"An exception occurred while trying "+
                      "to get an attribute value: " + e.getMessage());
        }

        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

        return response;
    }

    /**
     * Returns the values of several attributes in the ModelMBean.
     * Executes a getAttribute for each attribute name in the
     * attrNames array passed in.
     *
     * @param attrNames A String array of names of the attributes
     * to be retrieved.
     *
     * @return The array of the retrieved attributes.
     *
     * @exception RuntimeOperationsException Wraps an
     * {@link IllegalArgumentException}: The object name in parameter is
     * null or attributes in parameter is null.
     *
     * @see #setAttributes(javax.management.AttributeList)
     */
    public AttributeList getAttributes(String[] attrNames)      {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    RequiredModelMBean.class.getName(), "Entry");
        }

        if (attrNames == null)
            throw new RuntimeOperationsException(new
                IllegalArgumentException("attributeNames must not be null"),
                "Exception occurred trying to get attributes of a "+
                "RequiredModelMBean");

        AttributeList responseList = new AttributeList();
        for (int i = 0; i < attrNames.length; i++) {
            try {
                responseList.add(new Attribute(attrNames[i],
                                     getAttribute(attrNames[i])));
            } catch (Exception e) {
                // eat exceptions because interface doesn't have an
                // exception on it
                if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "Failed to get \"" + attrNames[i] + "\": ", e);
                }
            }
        }

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

        return responseList;
    }

    /**
     * Sets the value of a specific attribute of a named ModelMBean.
     *
     * If the 'setMethod' field of the attribute's descriptor
     * contains the name of a valid operation descriptor, then the
     * method described by the operation descriptor is executed.
     * In this implementation, the operation descriptor must be specified
     * correctly and assigned to the modelMBeanInfo so that the 'setMethod'
     * works correctly.
     * The response from the method is set as the value of the attribute
     * in the descriptor.
     *
     * <p>If currencyTimeLimit is &gt; 0, then the new value for the
     * attribute is cached in the attribute descriptor's
     * 'value' field and the 'lastUpdatedTimeStamp' field is set to
     * the current time stamp.
     *
     * <p>If the persist field of the attribute's descriptor is not null
     * then Persistence policy from the attribute descriptor is used to
     * guide storing the attribute in a persistent store.
     * <br>Store the MBean if 'persistPolicy' field is:
     * <UL>
     * <Li> != "never"</Li>
     * <Li> = "always"</Li>
     * <Li> = "onUpdate"</Li>
     * <Li> {@literal = "onTimer" and now > 'lastPersistTime' + 'persistPeriod'}</Li>
     * <Li> {@literal = "NoMoreOftenThan" and now > 'lastPersistTime' +
     *         'persistPeriod'}</Li>
     * </UL>
     * Do not store the MBean if 'persistPolicy' field is:
     * <UL>
     * <Li> = "never"</Li>
     * <Li> = {@literal = "onTimer" && now < 'lastPersistTime' + 'persistPeriod'}</Li>
     * <Li> = "onUnregister"</Li>
     * <Li> = {@literal = "NoMoreOftenThan" and now < 'lastPersistTime' +
     *        'persistPeriod'}</Li>
     * </UL>
     *
     * <p>The ModelMBeanInfo of the Model MBean is stored in a file.
     *
     * @param attribute The Attribute instance containing the name of
     *        the attribute to be set and the value it is to be set to.
     *
     *
     * @exception AttributeNotFoundException The specified attribute is
     *   not accessible in the MBean.
     *   <br>The following cases may result in an AttributeNotFoundException:
     *   <UL>
     *     <LI> No ModelMBeanAttributeInfo is found for the specified
     *          attribute.</LI>
     *     <LI> The ModelMBeanAttributeInfo's isWritable method returns
     *          'false'.</LI>
     *   </UL>
     * @exception InvalidAttributeValueException No descriptor is defined
     *   for the specified attribute.
     * @exception MBeanException Wraps one of the following Exceptions:
     *   <UL>
     *     <LI> An Exception thrown by the managed object's setter.</LI>
     *     <LI> A {@link ServiceNotFoundException} if a setMethod field is
     *          defined in the descriptor for the attribute and the managed
     *          resource is null; or if no setMethod field is defined and
     *          caching is not enabled for the attribute.
     *          Note that if there is no getMethod field either, then caching
     *          is automatically enabled.</LI>
     *     <LI> {@link InvalidTargetObjectTypeException} The 'targetType'
     *          field value is not 'objectReference'.</LI>
     *     <LI> An Exception thrown by the managed object's getter.</LI>
     *   </UL>
     * @exception ReflectionException  Wraps an {@link java.lang.Exception}
     *   thrown while trying to invoke the setter.
     * @exception RuntimeOperationsException Wraps an
     *   {@link IllegalArgumentException}: The attribute in parameter is
     *   null.
     *
     * @see #getAttribute(java.lang.String)
     **/
    public void setAttribute(Attribute attribute)
        throws AttributeNotFoundException, InvalidAttributeValueException,
               MBeanException, ReflectionException {
        final boolean tracing = MODELMBEAN_LOGGER.isLoggable(Level.TRACE);
        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (attribute == null)
            throw new RuntimeOperationsException(new
                IllegalArgumentException("attribute must not be null"),
                "Exception occurred trying to set an attribute of a "+
                "RequiredModelMBean");

        /* run setMethod if there is one */
        /* return cached value if its current */
        /* set cached value in descriptor and set date/time */
        /* send attribute change Notification */
        /* check persistence policy and persist if need be */
        String attrName = attribute.getName();
        Object attrValue = attribute.getValue();
        boolean updateDescriptor = false;

        ModelMBeanAttributeInfo attrInfo =
            modelMBeanInfo.getAttribute(attrName);

        if (attrInfo == null)
            throw new AttributeNotFoundException("setAttribute failed: " +
                                               attrName + " is not found ");

        Descriptor mmbDesc = modelMBeanInfo.getMBeanDescriptor();
        Descriptor attrDescr = attrInfo.getDescriptor();

        if (attrDescr != null) {
            if (!attrInfo.isWritable())
                throw new AttributeNotFoundException("setAttribute failed: "
                                          + attrName + " is not writable ");

            String attrSetMethod = (String)
                (attrDescr.getFieldValue("setMethod"));
            String attrGetMethod = (String)
                (attrDescr.getFieldValue("getMethod"));

            String attrType = attrInfo.getType();
            Object currValue = "Unknown";

            try {
                currValue = this.getAttribute(attrName);
            } catch (Throwable t) {
                // OK: Default "Unknown" value used for unknown attribute
            }

            Attribute oldAttr = new Attribute(attrName, currValue);

            /* run method from operations descriptor */
            if (attrSetMethod == null) {
                if (attrValue != null) {
                    try {
                        final Class<?> clazz = loadClass(attrType);
                        if (! clazz.isInstance(attrValue))  throw new
                            InvalidAttributeValueException(clazz.getName() +
                                                           " expected, "   +
                                            attrValue.getClass().getName() +
                                                           " received.");
                    } catch (ClassNotFoundException x) {
                        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                            MODELMBEAN_LOGGER.log(Level.TRACE,
                                "Class " + attrType + " for attribute "
                                + attrName + " not found: ", x);
                        }
                    }
                }
                updateDescriptor = true;
            } else {
                invoke(attrSetMethod,
                       (new Object[] {attrValue}),
                       (new String[] {attrType}) );
            }

            /* change cached value */
            Object objctl = attrDescr.getFieldValue("currencyTimeLimit");
            String ctl;
            if (objctl != null) ctl = objctl.toString();
            else ctl = null;

            if ((ctl == null) && (mmbDesc != null)) {
                objctl = mmbDesc.getFieldValue("currencyTimeLimit");
                if (objctl != null) ctl = objctl.toString();
                else ctl = null;
            }

            final boolean updateCache = ((ctl != null) && !(ctl.equals("-1")));

             if(attrSetMethod == null  && !updateCache && attrGetMethod != null)
                throw new MBeanException(new ServiceNotFoundException("No " +
                        "setMethod field is defined in the descriptor for " +
                        attrName + " attribute and caching is not enabled " +
                        "for it"));

            if (updateCache || updateDescriptor) {
                if (tracing) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                            "setting cached value of " +
                            attrName + " to " + attrValue);
                }

                attrDescr.setField("value", attrValue);

                if (updateCache) {
                    final String currtime = String.valueOf(
                        (new Date()).getTime());

                    attrDescr.setField("lastUpdatedTimeStamp", currtime);
                }

                attrInfo.setDescriptor(attrDescr);

                modelMBeanInfo.setDescriptor(attrDescr,"attribute");
                if (tracing) {
                    final StringBuilder strb = new StringBuilder()
                    .append("new descriptor is ").append(attrDescr)
                    .append(". AttributeInfo descriptor is ")
                    .append(attrInfo.getDescriptor())
                    .append(". AttributeInfo descriptor is ")
                    .append(modelMBeanInfo.getDescriptor(attrName,"attribute"));
                    MODELMBEAN_LOGGER.log(Level.TRACE, strb::toString);
                }

            }

            if (tracing) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "sending sendAttributeNotification");
            }
            sendAttributeChangeNotification(oldAttr,attribute);

        } else { // if descriptor ... else no descriptor

            if (tracing) {
                    MODELMBEAN_LOGGER.log(Level.TRACE,
                        "setMethod failed " + attrName +
                        " not in attributeDescriptor\n");
            }

            throw new InvalidAttributeValueException(
                      "Unable to resolve attribute value, "+
                      "no defined in descriptor for attribute");
        } // else no descriptor

        if (tracing) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

    }

    /**
     * Sets the values of an array of attributes of this ModelMBean.
     * Executes the setAttribute() method for each attribute in the list.
     *
     * @param attributes A list of attributes: The identification of the
     * attributes to be set and  the values they are to be set to.
     *
     * @return  The array of attributes that were set, with their new
     *    values in Attribute instances.
     *
     * @exception RuntimeOperationsException Wraps an
     *   {@link IllegalArgumentException}: The object name in parameter
     *   is null or attributes in parameter is null.
     *
     * @see #getAttributes
     **/
    public AttributeList setAttributes(AttributeList attributes) {

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (attributes == null)
            throw new RuntimeOperationsException(new
                IllegalArgumentException("attributes must not be null"),
                "Exception occurred trying to set attributes of a "+
                "RequiredModelMBean");

        final AttributeList responseList = new AttributeList();

        // Go through the list of attributes
        for (Attribute attr : attributes.asList()) {
            try {
                setAttribute(attr);
                responseList.add(attr);
            } catch (Exception excep) {
                responseList.remove(attr);
            }
        }

        return responseList;
    }



    private ModelMBeanInfo createDefaultModelMBeanInfo() {
        return(new ModelMBeanInfoSupport((this.getClass().getName()),
                   "Default ModelMBean", null, null, null, null));
    }

    /*************************************/
    /* NotificationBroadcaster Interface */
    /*************************************/


    private synchronized void writeToLog(String logFileName,
                                         String logEntry) throws Exception {

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                "Notification Logging to " + logFileName + ": " + logEntry);
        }
        if ((logFileName == null) || (logEntry == null)) {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                    "Bad input parameters, will not log this entry.");
            }
            return;
        }

        FileOutputStream fos = new FileOutputStream(logFileName, true);
        try {
            PrintStream logOut = new PrintStream(fos);
            logOut.println(logEntry);
            logOut.close();
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                    "Successfully opened log " + logFileName);
            }
        } catch (Exception e) {
            if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                        "Exception " + e.toString() +
                        " trying to write to the Notification log file " +
                        logFileName);
            }
            throw e;
        } finally {
            fos.close();
        }
    }


    /**
     * Registers an object which implements the NotificationListener
     * interface as a listener.  This
     * object's 'handleNotification()' method will be invoked when any
     * notification is issued through or by the ModelMBean.  This does
     * not include attributeChangeNotifications.  They must be registered
     * for independently.
     *
     * @param listener The listener object which will handles
     *        notifications emitted by the registered MBean.
     * @param filter The filter object. If null, no filtering will be
     *        performed before handling notifications.
     * @param handback The context to be sent to the listener with
     *        the notification when a notification is emitted.
     *
     * @exception IllegalArgumentException The listener cannot be null.
     *
     * @see #removeNotificationListener
     */
    public void addNotificationListener(NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback)
        throws java.lang.IllegalArgumentException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (listener == null)
            throw new IllegalArgumentException(
                  "notification listener must not be null");

        if (generalBroadcaster == null)
            generalBroadcaster = new NotificationBroadcasterSupport();

        generalBroadcaster.addNotificationListener(listener, filter,
                                                   handback);
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE,
                    "NotificationListener added");
                MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }
    }

    /**
     * Removes a listener for Notifications from the RequiredModelMBean.
     *
     * @param listener The listener name which was handling notifications
     *    emitted by the registered MBean.
     *    This method will remove all information related to this listener.
     *
     * @exception ListenerNotFoundException The listener is not registered
     *    in the MBean or is null.
     *
     * @see #addNotificationListener
     **/
    public void removeNotificationListener(NotificationListener listener)
        throws ListenerNotFoundException {
        if (listener == null)
            throw new ListenerNotFoundException(
                      "Notification listener is null");

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
                MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (generalBroadcaster == null)
            throw new ListenerNotFoundException(
                  "No notification listeners registered");


        generalBroadcaster.removeNotificationListener(listener);
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

    }

    public void removeNotificationListener(NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback)
        throws ListenerNotFoundException {

        if (listener == null)
            throw new ListenerNotFoundException(
                      "Notification listener is null");

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (generalBroadcaster == null)
            throw new ListenerNotFoundException(
                  "No notification listeners registered");


        generalBroadcaster.removeNotificationListener(listener,filter,
                                                      handback);

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

    }

    public void sendNotification(Notification ntfyObj)
        throws MBeanException, RuntimeOperationsException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (ntfyObj == null)
            throw new RuntimeOperationsException(new
                IllegalArgumentException("notification object must not be "+
                                         "null"),
                "Exception occurred trying to send a notification from a "+
                "RequiredModelMBean");


        // log notification if specified in descriptor
        Descriptor ntfyDesc =
            modelMBeanInfo.getDescriptor(ntfyObj.getType(),"notification");
        Descriptor mmbDesc = modelMBeanInfo.getMBeanDescriptor();

        if (ntfyDesc != null) {
            String logging = (String) ntfyDesc.getFieldValue("log");

            if (logging == null) {
                if (mmbDesc != null)
                    logging = (String) mmbDesc.getFieldValue("log");
            }

            if ((logging != null) &&
                (logging.equalsIgnoreCase("t") ||
                 logging.equalsIgnoreCase("true"))) {

                String logfile = (String) ntfyDesc.getFieldValue("logfile");
                if (logfile == null) {
                    if (mmbDesc != null)
                        logfile = (String)mmbDesc.getFieldValue("logfile");
                }
                if (logfile != null) {
                    try {
                        writeToLog(logfile,"LogMsg: " +
                            ((new Date(ntfyObj.getTimeStamp())).toString())+
                            " " + ntfyObj.getType() + " " +
                            ntfyObj.getMessage() + " Severity = " +
                            (String)ntfyDesc.getFieldValue("severity"));
                    } catch (Exception e) {
                        if (MODELMBEAN_LOGGER.isLoggable(Level.DEBUG)) {
                            MODELMBEAN_LOGGER.log(Level.DEBUG,
                                    "Failed to log " +
                                    ntfyObj.getType() + " notification: ", e);
                        }
                    }
                }
            }
        }
        if (generalBroadcaster != null) {
            generalBroadcaster.sendNotification(ntfyObj);
        }

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    "sendNotification sent provided notification object");
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

    }


    public void sendNotification(String ntfyText)
        throws MBeanException, RuntimeOperationsException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (ntfyText == null)
            throw new RuntimeOperationsException(new
                IllegalArgumentException("notification message must not "+
                                         "be null"),
                "Exception occurred trying to send a text notification "+
                "from a ModelMBean");

        Notification myNtfyObj = new Notification("jmx.modelmbean.generic",
                                                  this, 1, ntfyText);
        sendNotification(myNtfyObj);
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Notification sent");
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }
    }

    /**
     * Returns `true' if the notification `notifName' is found
     * in `info'. (bug 4744667)
     **/
    private static final
        boolean hasNotification(final ModelMBeanInfo info,
                                final String notifName) {
        try {
            if (info == null) return false;
            else return (info.getNotification(notifName)!=null);
        } catch (MBeanException x) {
            return false;
        } catch (RuntimeOperationsException r) {
            return false;
        }
    }

    /**
     * Creates a default ModelMBeanNotificationInfo for GENERIC
     * notification.  (bug 4744667)
     **/
    private static final ModelMBeanNotificationInfo makeGenericInfo() {
        final Descriptor genericDescriptor = new DescriptorSupport( new
            String[] {
                "name=GENERIC",
                "descriptorType=notification",
                "log=T",
                "severity=6",
                "displayName=jmx.modelmbean.generic"} );

        return new ModelMBeanNotificationInfo(new
            String[] {"jmx.modelmbean.generic"},
            "GENERIC",
            "A text notification has been issued by the managed resource",
            genericDescriptor);
    }

    /**
     * Creates a default ModelMBeanNotificationInfo for ATTRIBUTE_CHANGE
     * notification.  (bug 4744667)
     **/
    private static final
        ModelMBeanNotificationInfo makeAttributeChangeInfo() {
        final Descriptor attributeDescriptor = new DescriptorSupport(new
            String[] {
                "name=ATTRIBUTE_CHANGE",
                "descriptorType=notification",
                "log=T",
                "severity=6",
                "displayName=jmx.attribute.change"});

        return new ModelMBeanNotificationInfo(new
            String[] {"jmx.attribute.change"},
            "ATTRIBUTE_CHANGE",
            "Signifies that an observed MBean attribute value has changed",
            attributeDescriptor );
    }

    /**
     * Returns the array of Notifications always generated by the
     * RequiredModelMBean.
     * <P>
     *
     * RequiredModelMBean may always send also two additional notifications:
     * <UL>
     *   <LI> One with descriptor <code>"name=GENERIC,descriptorType=notification,log=T,severity=6,displayName=jmx.modelmbean.generic"</code></LI>
     *   <LI> Second is a standard attribute change notification
     *        with descriptor <code>"name=ATTRIBUTE_CHANGE,descriptorType=notification,log=T,severity=6,displayName=jmx.attribute.change"</code></LI>
     * </UL>
     * Thus these two notifications are always added to those specified
     * by the application.
     *
     * @return MBeanNotificationInfo[]
     *
     **/
    public MBeanNotificationInfo[] getNotificationInfo() {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        // Using hasNotification() is not optimal, but shouldn't really
        // matter in this context...

        // hasGeneric==true if GENERIC notification is present.
        // (bug 4744667)
        final boolean hasGeneric = hasNotification(modelMBeanInfo,"GENERIC");

        // hasAttributeChange==true if ATTRIBUTE_CHANGE notification is
        // present.
        // (bug 4744667)
        final boolean hasAttributeChange =
           hasNotification(modelMBeanInfo,"ATTRIBUTE_CHANGE");

        // User supplied list of notification infos.
        //
        final ModelMBeanNotificationInfo[] currInfo =
           (ModelMBeanNotificationInfo[])modelMBeanInfo.getNotifications();

        // Length of the returned list of notification infos:
        //    length of user suplied list + possibly 1 for GENERIC, +
        //    possibly 1 for ATTRIBUTE_CHANGE
        //    (bug 4744667)
        final int len = ((currInfo==null?0:currInfo.length) +
                         (hasGeneric?0:1) + (hasAttributeChange?0:1));

        // Returned list of notification infos:
        //
        final ModelMBeanNotificationInfo[] respInfo =
           new ModelMBeanNotificationInfo[len];

        // Preserve previous ordering (JMX 1.1)
        //

        // Counter of "standard" notification inserted before user
        // supplied notifications.
        //
        int inserted=0;
        if (!hasGeneric)
            // We need to add description for GENERIC notification
            // (bug 4744667)
            respInfo[inserted++] = makeGenericInfo();


        if (!hasAttributeChange)
            // We need to add description for ATTRIBUTE_CHANGE notification
            // (bug 4744667)
            respInfo[inserted++] = makeAttributeChangeInfo();

        // Now copy user supplied list in returned list.
        //
        final int count  = currInfo.length;
        final int offset = inserted;
        for (int j=0; j < count; j++) {
            respInfo[offset+j] = currInfo[j];
        }

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

        return respInfo;
    }


    public void addAttributeChangeNotificationListener(NotificationListener
                                                       inlistener,
                                                       String
                                                       inAttributeName,
                                                       Object inhandback)
        throws MBeanException, RuntimeOperationsException,
               IllegalArgumentException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (inlistener == null)
            throw new IllegalArgumentException(
                  "Listener to be registered must not be null");


        if (attributeBroadcaster == null)
            attributeBroadcaster = new NotificationBroadcasterSupport();

        AttributeChangeNotificationFilter currFilter =
            new AttributeChangeNotificationFilter();

        MBeanAttributeInfo[] attrInfo = modelMBeanInfo.getAttributes();
        boolean found = false;
        if (inAttributeName == null) {
            if ((attrInfo != null) && (attrInfo.length>0)) {
                for (int i=0; i<attrInfo.length; i++) {
                    currFilter.enableAttribute(attrInfo[i].getName());
                }
            }
        } else {
            if ((attrInfo != null) && (attrInfo.length>0)) {
                for (int i=0; i<attrInfo.length; i++) {
                    if (inAttributeName.equals(attrInfo[i].getName())) {
                        found = true;
                        currFilter.enableAttribute(inAttributeName);
                        break;
                    }
                }
            }
            if (!found) {
                throw new RuntimeOperationsException(new
                    IllegalArgumentException(
                    "The attribute name does not exist"),
                    "Exception occurred trying to add an "+
                    "AttributeChangeNotification listener");
            }
        }

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            Vector<String> enabledAttrs = currFilter.getEnabledAttributes();
            String s = (enabledAttrs.size() > 1) ?
                        "[" + enabledAttrs.firstElement() + ", ...]" :
                        enabledAttrs.toString();
            MODELMBEAN_LOGGER.log(Level.TRACE,
                "Set attribute change filter to " + s);
        }

        attributeBroadcaster.addNotificationListener(inlistener,currFilter,
                                                     inhandback);
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    "Notification listener added for " + inAttributeName);
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }
    }

    public void removeAttributeChangeNotificationListener(
            NotificationListener inlistener, String inAttributeName)
        throws MBeanException, RuntimeOperationsException,
               ListenerNotFoundException {
        if (inlistener == null) throw new
            ListenerNotFoundException("Notification listener is null");

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                    RequiredModelMBean.class.getName(), "Entry");
        }


        if (attributeBroadcaster == null)
            throw new ListenerNotFoundException(
                  "No attribute change notification listeners registered");


        MBeanAttributeInfo[] attrInfo = modelMBeanInfo.getAttributes();
        boolean found = false;
        if ((attrInfo != null) && (attrInfo.length>0)) {
            for (int i=0; i<attrInfo.length; i++) {
                if (attrInfo[i].getName().equals(inAttributeName)) {
                    found = true;
                    break;
                }
            }
        }

        if ((!found) && (inAttributeName != null)) {
            throw new RuntimeOperationsException(new
                IllegalArgumentException("Invalid attribute name"),
                "Exception occurred trying to remove "+
                "attribute change notification listener");
        }

        /* note: */
        /* this may be a problem if the same listener is registered for
           multiple attributes with multiple filters and/or handback
           objects.  It may remove all of them */

        attributeBroadcaster.removeNotificationListener(inlistener);

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }
    }

    public void sendAttributeChangeNotification(AttributeChangeNotification
                                                ntfyObj)
        throws MBeanException, RuntimeOperationsException {

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        if (ntfyObj == null)
            throw new RuntimeOperationsException(new
                IllegalArgumentException(
                "attribute change notification object must not be null"),
                "Exception occurred trying to send "+
                "attribute change notification of a ModelMBean");

        Object oldv = ntfyObj.getOldValue();
        Object newv =  ntfyObj.getNewValue();

        if (oldv == null) oldv = "null";
        if (newv == null) newv = "null";

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE,
                "Sending AttributeChangeNotification with " +
                ntfyObj.getAttributeName() + ntfyObj.getAttributeType() +
                ntfyObj.getNewValue() + ntfyObj.getOldValue());
        }

        // log notification if specified in descriptor
        Descriptor ntfyDesc =
            modelMBeanInfo.getDescriptor(ntfyObj.getType(),"notification");
        Descriptor mmbDesc = modelMBeanInfo.getMBeanDescriptor();

        String logging, logfile;

        if (ntfyDesc != null) {
            logging =(String)  ntfyDesc.getFieldValue("log");
            if (logging == null) {
                if (mmbDesc != null)
                    logging = (String) mmbDesc.getFieldValue("log");
            }
            if ((logging != null) &&
                ( logging.equalsIgnoreCase("t") ||
                  logging.equalsIgnoreCase("true"))) {
                logfile = (String) ntfyDesc.getFieldValue("logfile");
                if (logfile == null) {
                    if (mmbDesc != null)
                        logfile = (String)mmbDesc.getFieldValue("logfile");
                }

                if (logfile != null) {
                    try {
                        writeToLog(logfile,"LogMsg: " +
                           ((new Date(ntfyObj.getTimeStamp())).toString())+
                           " " + ntfyObj.getType() + " " +
                           ntfyObj.getMessage() +
                           " Name = " + ntfyObj.getAttributeName() +
                           " Old value = " + oldv +
                           " New value = " + newv);
                    } catch (Exception e) {
                        if (MODELMBEAN_LOGGER.isLoggable(Level.DEBUG)) {
                            MODELMBEAN_LOGGER.log(Level.DEBUG,
                                "Failed to log " + ntfyObj.getType() +
                                    " notification: ", e);
                        }
                    }
                }
            }
        } else if (mmbDesc != null) {
            logging = (String) mmbDesc.getFieldValue("log");
            if ((logging != null) &&
                ( logging.equalsIgnoreCase("t") ||
                  logging.equalsIgnoreCase("true") )) {
                logfile = (String) mmbDesc.getFieldValue("logfile");

                if (logfile != null) {
                    try {
                        writeToLog(logfile,"LogMsg: " +
                           ((new Date(ntfyObj.getTimeStamp())).toString())+
                           " " + ntfyObj.getType() + " " +
                           ntfyObj.getMessage() +
                           " Name = " + ntfyObj.getAttributeName() +
                           " Old value = " + oldv +
                           " New value = " + newv);
                    } catch (Exception e) {
                        if (MODELMBEAN_LOGGER.isLoggable(Level.DEBUG)) {
                            MODELMBEAN_LOGGER.log(Level.DEBUG,
                                "Failed to log " + ntfyObj.getType() +
                                    " notification: ", e);
                        }
                    }
                }
            }
        }
        if (attributeBroadcaster != null) {
            attributeBroadcaster.sendNotification(ntfyObj);
        }

        // XXX Revisit: This is a quickfix: it would be better to have a
        //     single broadcaster. However, it is not so simple because
        //     removeAttributeChangeNotificationListener() should
        //     remove only listeners whose filter is an instanceof
        //     AttributeChangeNotificationFilter.
        //
        if (generalBroadcaster != null) {
            generalBroadcaster.sendNotification(ntfyObj);
        }

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "sent notification");
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }
    }

    public void sendAttributeChangeNotification(Attribute inOldVal,
                                                Attribute inNewVal)
        throws MBeanException, RuntimeOperationsException {
        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Entry");
        }

        // do we really want to do this?
        if ((inOldVal == null) || (inNewVal == null))
            throw new RuntimeOperationsException(new
               IllegalArgumentException("Attribute object must not be null"),
               "Exception occurred trying to send " +
               "attribute change notification of a ModelMBean");


        if (!(inOldVal.getName().equals(inNewVal.getName())))
            throw new RuntimeOperationsException(new
                IllegalArgumentException("Attribute names are not the same"),
                "Exception occurred trying to send " +
                "attribute change notification of a ModelMBean");


        Object newVal = inNewVal.getValue();
        Object oldVal = inOldVal.getValue();
        String className = "unknown";
        if (newVal != null)
            className = newVal.getClass().getName();
        if (oldVal != null)
            className = oldVal.getClass().getName();

        AttributeChangeNotification myNtfyObj = new
            AttributeChangeNotification(this,
                                        1,
                                        ((new Date()).getTime()),
                                        "AttributeChangeDetected",
                                        inOldVal.getName(),
                                        className,
                                        inOldVal.getValue(),
                                        inNewVal.getValue());

        sendAttributeChangeNotification(myNtfyObj);

        if (MODELMBEAN_LOGGER.isLoggable(Level.TRACE)) {
            MODELMBEAN_LOGGER.log(Level.TRACE, "Exit");
        }

    }

    /**
     * Return the Class Loader Repository used to perform class loading.
     * Subclasses may wish to redefine this method in order to return
     * the appropriate {@link javax.management.loading.ClassLoaderRepository}
     * that should be used in this object.
     *
     * @return the Class Loader Repository.
     *
     */
    protected ClassLoaderRepository getClassLoaderRepository() {
        return MBeanServerFactory.getClassLoaderRepository(server);
    }

    private Class<?> loadClass(final String className)
        throws ClassNotFoundException {
        @SuppressWarnings("removal")
        AccessControlContext stack = AccessController.getContext();
        final ClassNotFoundException[] caughtException = new ClassNotFoundException[1];

        Class<?> c = javaSecurityAccess.doIntersectionPrivilege(new PrivilegedAction<Class<?>>() {

            @Override
            public Class<?> run() {
                try {
                    ReflectUtil.checkPackageAccess(className);
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    final ClassLoaderRepository clr =
                        getClassLoaderRepository();
                    try {
                        if (clr == null) throw new ClassNotFoundException(className);
                        return clr.loadClass(className);
                    } catch (ClassNotFoundException ex) {
                        caughtException[0] = ex;
                    }
                }
                return null;
            }
        }, stack, acc);

        if (caughtException[0] != null) {
            throw caughtException[0];
        }

        return c;
    }


    /*************************************/
    /* MBeanRegistration Interface       */
    /*************************************/

    /**
     * Allows the MBean to perform any operations it needs before
     * being registered in the MBean server.  If the name of the MBean
     * is not specified, the MBean can provide a name for its
     * registration.  If any exception is raised, the MBean will not be
     * registered in the MBean server.
     * <P>
     * In order to ensure proper run-time semantics of RequireModelMBean,
     * Any subclass of RequiredModelMBean overloading or overriding this
     * method should call <code>super.preRegister(server, name)</code>
     * in its own <code>preRegister</code> implementation.
     *
     * @param server The MBean server in which the MBean will be registered.
     *
     * @param name The object name of the MBean.  This name is null if
     * the name parameter to one of the <code>createMBean</code> or
     * <code>registerMBean</code> methods in the {@link MBeanServer}
     * interface is null.  In that case, this method must return a
     * non-null ObjectName for the new MBean.
     *
     * @return The name under which the MBean is to be registered.
     * This value must not be null.  If the <code>name</code>
     * parameter is not null, it will usually but not necessarily be
     * the returned value.
     *
     * @exception java.lang.Exception This exception will be caught by
     * the MBean server and re-thrown as an
     * {@link javax.management.MBeanRegistrationException
     * MBeanRegistrationException}.
     */
    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name)
        throws java.lang.Exception  {
        // Since ModelMbeanInfo cannot be null (otherwise exception
        // thrown at creation)
        // no exception thrown on ModelMBeanInfo not set.
        if (name == null) throw new NullPointerException(
                     "name of RequiredModelMBean to registered is null");
        this.server = server;
        return name;
    }

    /**
     * Allows the MBean to perform any operations needed after having been
     * registered in the MBean server or after the registration has failed.
     * <P>
     * In order to ensure proper run-time semantics of RequireModelMBean,
     * Any subclass of RequiredModelMBean overloading or overriding this
     * method should call <code>super.postRegister(registrationDone)</code>
     * in its own <code>postRegister</code> implementation.
     *
     * @param registrationDone Indicates whether or not the MBean has
     * been successfully registered in the MBean server. The value
     * false means that the registration phase has failed.
     */
    public void postRegister(Boolean registrationDone) {
        registered = registrationDone.booleanValue();
    }

    /**
     * Allows the MBean to perform any operations it needs before
     * being unregistered by the MBean server.
     * <P>
     * In order to ensure proper run-time semantics of RequireModelMBean,
     * Any subclass of RequiredModelMBean overloading or overriding this
     * method should call <code>super.preDeregister()</code> in its own
     * <code>preDeregister</code> implementation.
     *
     * @exception java.lang.Exception This exception will be caught by
     * the MBean server and re-thrown as an
     * {@link javax.management.MBeanRegistrationException
     * MBeanRegistrationException}.
     */
    public void preDeregister() throws java.lang.Exception {
    }

    /**
     * Allows the MBean to perform any operations needed after having been
     * unregistered in the MBean server.
     * <P>
     * In order to ensure proper run-time semantics of RequireModelMBean,
     * Any subclass of RequiredModelMBean overloading or overriding this
     * method should call <code>super.postDeregister()</code> in its own
     * <code>postDeregister</code> implementation.
     */
    public void postDeregister() {
        registered = false;
        this.server=null;
    }

    private static final String[] primitiveTypes;
    private static final String[] primitiveWrappers;
    static {
        primitiveTypes = new String[] {
            Boolean.TYPE.getName(),
            Byte.TYPE.getName(),
            Character.TYPE.getName(),
            Short.TYPE.getName(),
            Integer.TYPE.getName(),
            Long.TYPE.getName(),
            Float.TYPE.getName(),
            Double.TYPE.getName(),
            Void.TYPE.getName()
        };
        primitiveWrappers = new String[] {
            Boolean.class.getName(),
            Byte.class.getName(),
            Character.class.getName(),
            Short.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Float.class.getName(),
            Double.class.getName(),
            Void.class.getName()
        };
    }
}
