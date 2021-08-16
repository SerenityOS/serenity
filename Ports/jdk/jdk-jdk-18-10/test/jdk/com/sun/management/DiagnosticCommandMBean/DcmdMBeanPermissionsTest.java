/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @test
 * @bug     7150256
 * @summary Permissions Tests for the DiagnosticCommandMBean
 * @author  Frederic Parain
 *
 * @modules java.logging
 *          java.management
 *
 * @run main/othervm -Djava.security.manager=allow DcmdMBeanPermissionsTest
 */

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.HashSet;
import java.util.Iterator;
import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanPermission;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;

/**
 *
 * @author fparain
 */
public class DcmdMBeanPermissionsTest {

    private static String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME =
        "com.sun.management:type=DiagnosticCommand";

    static public class CustomSecurityManager extends SecurityManager {

        private HashSet<Permission> grantedPermissions;

        public CustomSecurityManager() {
            grantedPermissions = new HashSet<Permission>();
        }

        public final void grantPermission(final Permission perm) {
            grantedPermissions.add(perm);
        }

        public final void denyPermission(final Permission perm) {
            Iterator<Permission> it = grantedPermissions.iterator();
            while (it.hasNext()) {
                Permission p = it.next();
                if (p.equals(perm)) {
                    it.remove();
                }
            }
        }

        public final void checkPermission(final Permission perm) {
            for (Permission p : grantedPermissions) {
                if (p.implies(perm)) {
                    return;
                }
            }
            throw new SecurityException(perm.toString());
        }
    };

    static Permission createPermission(String classname, String name,
            String action) {
        Permission permission = null;
        try {
            Class c = Class.forName(classname);
            if (action == null) {
                try {
                    Constructor constructor = c.getConstructor(String.class);
                    permission = (Permission) constructor.newInstance(name);

                } catch (InstantiationException | IllegalAccessException
                        | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException ex) {
                    ex.printStackTrace();
                    throw new RuntimeException("TEST FAILED");
                }
            }
            if (permission == null) {
                try {
                    Constructor constructor = c.getConstructor(String.class,
                            String.class);
                    permission = (Permission) constructor.newInstance(
                            name,
                            action);
                } catch (InstantiationException | IllegalAccessException
                        | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException ex) {
                    ex.printStackTrace();
                    throw new RuntimeException("TEST FAILED");
                }
            }
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
                    throw new RuntimeException("TEST FAILED");
        }
        if (permission == null) {
            throw new RuntimeException("TEST FAILED");
        }
        return permission;
    }

    // return true if invokation triggered a SecurityException
    static boolean invokeOperation(MBeanServer mbs, ObjectName on,
            MBeanOperationInfo opInfo) {
        try {
            if (opInfo.getSignature().length == 0) {
                mbs.invoke(on, opInfo.getName(),
                        new Object[0], new String[0]);
            } else {
                mbs.invoke(on, opInfo.getName(),
                        new Object[1], new String[]{ String[].class.getName()});
            }
        } catch (SecurityException ex) {
            ex.printStackTrace();
            return true;
        } catch (RuntimeMBeanException ex) {
            if (ex.getCause() instanceof SecurityException) {
                //ex.printStackTrace();
                return true;
            }
        } catch (MBeanException | InstanceNotFoundException
                | ReflectionException ex) {
            throw new RuntimeException("TEST FAILED");
        }
        return false;
    }

    static void testOperation(MBeanServer mbs, CustomSecurityManager sm,
            ObjectName on, MBeanOperationInfo opInfo) {
        System.out.println("Testing " + opInfo.getName());
        Descriptor desc = opInfo.getDescriptor();
        if (desc.getFieldValue("dcmd.permissionClass") == null) {
        // No special permission required, execution should not trigger
        // any security exception
            if (invokeOperation(mbs, on, opInfo)) {
                throw new RuntimeException("TEST FAILED");
            }
        } else {
            // Building the required permission
            Permission reqPerm = createPermission(
                    (String)desc.getFieldValue("dcmd.permissionClass"),
                    (String)desc.getFieldValue("dcmd.permissionName"),
                    (String)desc.getFieldValue("dcmd.permissionAction"));
            // Paranoid mode: check that the SecurityManager has not already
            // been granted the permission
            sm.denyPermission(reqPerm);
            // A special permission is required for this operation,
            // invoking it without the permission granted must trigger
            // a security exception
            if(!invokeOperation(mbs, on, opInfo)) {
                throw new RuntimeException("TEST FAILED");
            }
            // grant the permission and re-try invoking the operation
            sm.grantPermission(reqPerm);
            if(invokeOperation(mbs, on, opInfo)) {
                throw new RuntimeException("TEST FAILED");
            }
            // Clean up
            sm.denyPermission(reqPerm);
        }
    }

    public static void main(final String[] args) {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName on = null;
        try {
            on = new ObjectName(HOTSPOT_DIAGNOSTIC_MXBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
            throw new RuntimeException("TEST FAILED");
        }
        MBeanInfo info = null;
        try {
            info = mbs.getMBeanInfo(on);
        } catch (InstanceNotFoundException | IntrospectionException
                | ReflectionException ex) {
            ex.printStackTrace();
            throw new RuntimeException("TEST FAILED");
        }
        CustomSecurityManager sm = new CustomSecurityManager();
        System.setSecurityManager(sm);
        // Set of permission required to run the test cleanly
        // Some permissions are required by the MBeanServer and other
        // platform services (RuntimePermission("createClassLoader"),
        // ReflectPermission("suppressAccessChecks"),
        // java.util.logging.LoggingPermission("control"),
        // RuntimePermission("exitVM.97")).
        // Other permissions are required by commands being invoked
        // in the test (for instance, RuntimePermission("modifyThreadGroup")
        // and RuntimePermission("modifyThread") are checked when
        // runFinalization() is invoked by the gcRunFinalization command.
        sm.grantPermission(new RuntimePermission("createClassLoader"));
        sm.grantPermission(new ReflectPermission("suppressAccessChecks"));
        sm.grantPermission(new java.util.logging.LoggingPermission("control", ""));
        sm.grantPermission(new java.lang.RuntimePermission("exitVM.*"));
        sm.grantPermission(new java.lang.RuntimePermission("modifyThreadGroup"));
        sm.grantPermission(new java.lang.RuntimePermission("modifyThread"));
        sm.grantPermission(new java.security.SecurityPermission("getProperty.jdk.jar.disabledAlgorithms"));
        for(MBeanOperationInfo opInfo : info.getOperations()) {
            Permission opPermission = new MBeanPermission(info.getClassName(),
                    opInfo.getName(),
                    on,
                    "invoke");
            sm.grantPermission(opPermission);
            testOperation(mbs, sm, on, opInfo);
            sm.denyPermission(opPermission);
        }
        System.out.println("TEST PASSED");
    }
}
