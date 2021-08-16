/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.management.internal;

import com.sun.management.DiagnosticCommandMBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.util.*;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import sun.management.ManagementFactoryHelper;
import sun.management.NotificationEmitterSupport;
import sun.management.VMManagement;

/**
 * Implementation class for the diagnostic commands subsystem.
 *
 * @since 1.8
 */
public class DiagnosticCommandImpl extends NotificationEmitterSupport
    implements DiagnosticCommandMBean {

    private final VMManagement jvm;
    private volatile Map<String, Wrapper> wrappers = null;
    private static final String strClassName = "".getClass().getName();
    private static final String strArrayClassName = String[].class.getName();
    private final boolean isSupported;
    private static DiagnosticCommandImpl diagCommandMBean = null;

    static synchronized DiagnosticCommandMBean getDiagnosticCommandMBean() {
        VMManagement jvm = ManagementFactoryHelper.getVMManagement();

        // Remote Diagnostic Commands may not be supported
        if (diagCommandMBean == null && jvm.isRemoteDiagnosticCommandsSupported()) {
            diagCommandMBean = new DiagnosticCommandImpl(jvm);
        }
        return diagCommandMBean;
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException,
        MBeanException, ReflectionException {
        throw new AttributeNotFoundException(attribute);
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
        InvalidAttributeValueException, MBeanException, ReflectionException {
        throw new AttributeNotFoundException(attribute.getName());
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        return new AttributeList();
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return new AttributeList();
    }

    private class Wrapper {

        String name;
        String cmd;
        DiagnosticCommandInfo info;
        Permission permission;

        Wrapper(String name, String cmd, DiagnosticCommandInfo info)
                throws InstantiationException {
            this.name = name;
            this.cmd = cmd;
            this.info = info;
            this.permission = null;
            Exception cause = null;
            if (info.getPermissionClass() != null) {
                try {
                    Class<?> c = Class.forName(info.getPermissionClass());
                    if (info.getPermissionAction() == null) {
                        try {
                            Constructor<?> constructor = c.getConstructor(String.class);
                            permission = (Permission) constructor.newInstance(info.getPermissionName());

                        } catch (InstantiationException | IllegalAccessException
                                | IllegalArgumentException | InvocationTargetException
                                | NoSuchMethodException | SecurityException ex) {
                            cause = ex;
                        }
                    }
                    if (permission == null) {
                        try {
                            Constructor<?> constructor = c.getConstructor(String.class, String.class);
                            permission = (Permission) constructor.newInstance(
                                    info.getPermissionName(),
                                    info.getPermissionAction());
                        } catch (InstantiationException | IllegalAccessException
                                | IllegalArgumentException | InvocationTargetException
                                | NoSuchMethodException | SecurityException ex) {
                            cause = ex;
                        }
                    }
                } catch (ClassNotFoundException ex) { }
                if (permission == null) {
                    InstantiationException iex =
                            new InstantiationException("Unable to instantiate required permission");
                    iex.initCause(cause);
                }
            }
        }

        public String execute(String[] args) {
            if (permission != null) {
                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    sm.checkPermission(permission);
                }
            }
            if(args == null) {
                return executeDiagnosticCommand(cmd);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(cmd);
                for(int i=0; i<args.length; i++) {
                    if(args[i] == null) {
                        throw new IllegalArgumentException("Invalid null argument");
                    }
                    sb.append(" ");
                    sb.append(args[i]);
                }
                return executeDiagnosticCommand(sb.toString());
            }
        }
    }

    DiagnosticCommandImpl(VMManagement jvm) {
        this.jvm = jvm;
        isSupported = jvm.isRemoteDiagnosticCommandsSupported();
    }

    private static class OperationInfoComparator implements Comparator<MBeanOperationInfo> {
        @Override
        public int compare(MBeanOperationInfo o1, MBeanOperationInfo o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        SortedSet<MBeanOperationInfo> operations = new TreeSet<>(new OperationInfoComparator());
        Map<String, Wrapper> wrappersmap;
        if (!isSupported) {
            wrappersmap = Collections.emptyMap();
        } else {
            try {
                String[] command = getDiagnosticCommands();
                DiagnosticCommandInfo[] info = getDiagnosticCommandInfo(command);
                MBeanParameterInfo stringArgInfo[] = new MBeanParameterInfo[]{
                    new MBeanParameterInfo("arguments", strArrayClassName,
                    "Array of Diagnostic Commands Arguments and Options")
                };
                wrappersmap = new HashMap<>();
                for (int i = 0; i < command.length; i++) {
                    String name = transform(command[i]);
                    try {
                        Wrapper w = new Wrapper(name, command[i], info[i]);
                        wrappersmap.put(name, w);
                        operations.add(new MBeanOperationInfo(
                                w.name,
                                w.info.getDescription(),
                                (w.info.getArgumentsInfo() == null
                                    || w.info.getArgumentsInfo().isEmpty())
                                    ? null : stringArgInfo,
                                strClassName,
                                MBeanOperationInfo.ACTION_INFO,
                                commandDescriptor(w)));
                    } catch (InstantiationException ex) {
                        // If for some reasons the creation of a diagnostic command
                        // wrappers fails, the diagnostic command is just ignored
                        // and won't appear in the DynamicMBean
                    }
                }
            } catch (IllegalArgumentException | UnsupportedOperationException e) {
                wrappersmap = Collections.emptyMap();
            }
        }
        wrappers =  Collections.unmodifiableMap(wrappersmap);
        HashMap<String, Object> map = new HashMap<>();
        map.put("immutableInfo", "false");
        map.put("interfaceClassName","com.sun.management.DiagnosticCommandMBean");
        map.put("mxbean", "false");
        Descriptor desc = new ImmutableDescriptor(map);
        return new MBeanInfo(
                this.getClass().getName(),
                "Diagnostic Commands",
                null, // attributes
                null, // constructors
                operations.toArray(new MBeanOperationInfo[operations.size()]), // operations
                getNotificationInfo(), // notifications
                desc);
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        if (!isSupported) {
            throw new UnsupportedOperationException();
        }
        if (wrappers == null) {
            getMBeanInfo();
        }
        Wrapper w = wrappers.get(actionName);
        if (w != null) {
            if (w.info.getArgumentsInfo().isEmpty()
                    && (params == null || params.length == 0)
                    && (signature == null || signature.length == 0)) {
                return w.execute(null);
            } else if((params != null && params.length == 1)
                    && (signature != null && signature.length == 1
                    && signature[0] != null
                    && signature[0].compareTo(strArrayClassName) == 0)) {
                return w.execute((String[]) params[0]);
            } else {
                throw new ReflectionException(
                    new NoSuchMethodException(actionName
                    + ": mismatched signature "
                    + (signature != null ? Arrays.toString(signature) : "[]")
                    + " or parameters"));
            }
        } else {
            throw new ReflectionException(
                new NoSuchMethodException("Method " + actionName
                + " with signature "
                + (signature != null ? Arrays.toString(signature) : "[]")
                + " not found"));
        }
    }

    private static String transform(String name) {
        StringBuilder sb = new StringBuilder();
        boolean toLower = true;
        boolean toUpper = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.' || c == '_') {
                toLower = false;
                toUpper = true;
            } else {
                if (toUpper) {
                    toUpper = false;
                    sb.append(Character.toUpperCase(c));
                } else if(toLower) {
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private Descriptor commandDescriptor(Wrapper w) throws IllegalArgumentException {
        HashMap<String, Object> map = new HashMap<>();
        map.put("dcmd.name", w.info.getName());
        map.put("dcmd.description", w.info.getDescription());
        map.put("dcmd.vmImpact", w.info.getImpact());
        map.put("dcmd.permissionClass", w.info.getPermissionClass());
        map.put("dcmd.permissionName", w.info.getPermissionName());
        map.put("dcmd.permissionAction", w.info.getPermissionAction());
        map.put("dcmd.enabled", w.info.isEnabled());
        StringBuilder sb = new StringBuilder();
        sb.append("help ");
        sb.append(w.info.getName());
        map.put("dcmd.help", executeDiagnosticCommand(sb.toString()));
        if (w.info.getArgumentsInfo() != null && !w.info.getArgumentsInfo().isEmpty()) {
            HashMap<String, Object> allargmap = new HashMap<>();
            for (DiagnosticCommandArgumentInfo arginfo : w.info.getArgumentsInfo()) {
                HashMap<String, Object> argmap = new HashMap<>();
                argmap.put("dcmd.arg.name", arginfo.getName());
                argmap.put("dcmd.arg.type", arginfo.getType());
                argmap.put("dcmd.arg.description", arginfo.getDescription());
                argmap.put("dcmd.arg.isMandatory", arginfo.isMandatory());
                argmap.put("dcmd.arg.isMultiple", arginfo.isMultiple());
                boolean isOption = arginfo.isOption();
                argmap.put("dcmd.arg.isOption", isOption);
                if(!isOption) {
                    argmap.put("dcmd.arg.position", arginfo.getPosition());
                } else {
                    argmap.put("dcmd.arg.position", -1);
                }
                allargmap.put(arginfo.getName(), new ImmutableDescriptor(argmap));
            }
            map.put("dcmd.arguments", new ImmutableDescriptor(allargmap));
        }
        return new ImmutableDescriptor(map);
    }

    private final static String notifName =
        "javax.management.Notification";

    private final static String[] diagFramNotifTypes = {
        "jmx.mbean.info.changed"
    };

    private MBeanNotificationInfo[] notifInfo = null;

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        synchronized (this) {
            if (notifInfo == null) {
                 notifInfo = new MBeanNotificationInfo[1];
                 notifInfo[0] =
                         new MBeanNotificationInfo(diagFramNotifTypes,
                                                   notifName,
                                                   "Diagnostic Framework Notification");
            }
        }
        return notifInfo.clone();
    }

    private static long seqNumber = 0;
    private static long getNextSeqNumber() {
        return ++seqNumber;
    }

    private void createDiagnosticFrameworkNotification() {

        if (!hasListeners()) {
            return;
        }
        ObjectName on = null;
        try {
            on = ObjectName.getInstance(PlatformMBeanProviderImpl.DIAGNOSTIC_COMMAND_MBEAN_NAME);
        } catch (MalformedObjectNameException e) { }
        Notification notif = new Notification("jmx.mbean.info.changed",
                                              on,
                                              getNextSeqNumber());
        notif.setUserData(getMBeanInfo());
        sendNotification(notif);
    }

    @Override
    public synchronized void addNotificationListener(NotificationListener listener,
            NotificationFilter filter,
            Object handback) {
        boolean before = hasListeners();
        super.addNotificationListener(listener, filter, handback);
        boolean after = hasListeners();
        if (!before && after) {
            setNotificationEnabled(true);
        }
    }

    @Override
    public synchronized void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        boolean before = hasListeners();
        super.removeNotificationListener(listener);
        boolean after = hasListeners();
        if (before && !after) {
            setNotificationEnabled(false);
        }
    }

    @Override
    public synchronized void removeNotificationListener(NotificationListener listener,
            NotificationFilter filter,
            Object handback)
            throws ListenerNotFoundException {
        boolean before = hasListeners();
        super.removeNotificationListener(listener, filter, handback);
        boolean after = hasListeners();
        if (before && !after) {
            setNotificationEnabled(false);
        }
    }

    private native void setNotificationEnabled(boolean enabled);
    private native String[] getDiagnosticCommands();
    private native DiagnosticCommandInfo[] getDiagnosticCommandInfo(String[] commands);
    private native String executeDiagnosticCommand(String command);

}
