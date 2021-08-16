/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.reflect.Member;
import java.io.FileDescriptor;
import java.io.File;
import java.io.FilePermission;
import java.net.InetAddress;
import java.net.SocketPermission;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.Security;
import java.security.SecurityPermission;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jdk.internal.module.ModuleLoaderMap;
import jdk.internal.reflect.CallerSensitive;
import sun.security.util.SecurityConstants;

/**
 * The security manager is a class that allows
 * applications to implement a security policy. It allows an
 * application to determine, before performing a possibly unsafe or
 * sensitive operation, what the operation is and whether
 * it is being attempted in a security context that allows the
 * operation to be performed. The
 * application can allow or disallow the operation.
 * <p>
 * The {@code SecurityManager} class contains many methods with
 * names that begin with the word {@code check}. These methods
 * are called by various methods in the Java libraries before those
 * methods perform certain potentially sensitive operations. The
 * invocation of such a {@code check} method typically looks like this:
 * <blockquote><pre>
 *     SecurityManager security = System.getSecurityManager();
 *     if (security != null) {
 *         security.check<i>XXX</i>(argument, &nbsp;.&nbsp;.&nbsp;.&nbsp;);
 *     }
 * </pre></blockquote>
 * <p>
 * The security manager is thereby given an opportunity to prevent
 * completion of the operation by throwing an exception. A security
 * manager routine simply returns if the operation is permitted, but
 * throws a {@code SecurityException} if the operation is not
 * permitted.
 * <p>
 * Environments using a security manager will typically set the security
 * manager at startup. In the JDK implementation, this is done by setting the
 * system property {@systemProperty java.security.manager} on the command line
 * to the class name of the security manager. It can also be set to the empty
 * String ("") or the special token "{@code default}" to use the
 * default {@code java.lang.SecurityManager}. If a class name is specified,
 * it must be {@code java.lang.SecurityManager} or a public subclass and have
 * a public no-arg constructor. The class is loaded by the
 * {@linkplain ClassLoader#getSystemClassLoader() built-in system class loader}
 * if it is not {@code java.lang.SecurityManager}. If the
 * {@code java.security.manager} system property is not set, the default value
 * is {@code null}, which means a security manager will not be set at startup.
 * <p>
 * The Java run-time may also allow, but is not required to allow, the security
 * manager to be set dynamically by invoking the
 * {@link System#setSecurityManager(SecurityManager) setSecurityManager} method.
 * In the JDK implementation, if the Java virtual machine is started with
 * the {@code java.security.manager} system property set to the special token
 * "{@code disallow}" then a security manager will not be set at startup and
 * cannot be set dynamically (the
 * {@link System#setSecurityManager(SecurityManager) setSecurityManager}
 * method will throw an {@code UnsupportedOperationException}). If the
 * {@code java.security.manager} system property is not set or is set to the
 * special token "{@code allow}", then a security manager will not be set at
 * startup but can be set dynamically. Finally, if the
 * {@code java.security.manager} system property is set to the class name of
 * the security manager, or to the empty String ("") or the special token
 * "{@code default}", then a security manager is set at startup (as described
 * previously) and can also be subsequently replaced (or disabled) dynamically
 * (subject to the policy of the currently installed security manager). The
 * following table illustrates the behavior of the JDK implementation for the
 * different settings of the {@code java.security.manager} system property:
 * <table class="striped">
 * <caption style="display:none">property value,
 *  the SecurityManager set at startup,
 *  can dynamically set a SecurityManager
 * </caption>
 * <thead>
 * <tr>
 * <th scope="col">Property Value</th>
 * <th scope="col">The SecurityManager set at startup</th>
 * <th scope="col">System.setSecurityManager run-time behavior</th>
 * </tr>
 * </thead>
 * <tbody>
 *
 * <tr>
 *   <th scope="row">null</th>
 *   <td>None</td>
 *   <td>Success or throws {@code SecurityException} if not permitted by
 * the currently installed security manager</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">empty String ("")</th>
 *   <td>{@code java.lang.SecurityManager}</td>
 *   <td>Success or throws {@code SecurityException} if not permitted by
 * the currently installed security manager</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">"default"</th>
 *   <td>{@code java.lang.SecurityManager}</td>
 *   <td>Success or throws {@code SecurityException} if not permitted by
 * the currently installed security manager</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">"disallow"</th>
 *   <td>None</td>
 *   <td>Always throws {@code UnsupportedOperationException}</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">"allow"</th>
 *   <td>None</td>
 *   <td>Success or throws {@code SecurityException} if not permitted by
 * the currently installed security manager</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">a class name</th>
 *   <td>the named class</td>
 *   <td>Success or throws {@code SecurityException} if not permitted by
 * the currently installed security manager</td>
 * </tr>
 *
 * </tbody>
 * </table>
 * <p> A future release of the JDK may change the default value of the
 * {@code java.security.manager} system property to "{@code disallow}".
 * <p>
 * The current security manager is returned by the
 * {@link System#getSecurityManager() getSecurityManager} method.
 * <p>
 * The special method
 * {@link SecurityManager#checkPermission(java.security.Permission)}
 * determines whether an access request indicated by a specified
 * permission should be granted or denied. The
 * default implementation calls
 *
 * <pre>
 *   AccessController.checkPermission(perm);
 * </pre>
 *
 * <p>
 * If a requested access is allowed,
 * {@code checkPermission} returns quietly. If denied, a
 * {@code SecurityException} is thrown.
 * <p>
 * The default implementation of each of the other
 * {@code check} methods in {@code SecurityManager} is to
 * call the {@code SecurityManager checkPermission} method
 * to determine if the calling thread has permission to perform the requested
 * operation.
 * <p>
 * Note that the {@code checkPermission} method with
 * just a single permission argument always performs security checks
 * within the context of the currently executing thread.
 * Sometimes a security check that should be made within a given context
 * will actually need to be done from within a
 * <i>different</i> context (for example, from within a worker thread).
 * The {@link SecurityManager#getSecurityContext getSecurityContext} method
 * and the {@link SecurityManager#checkPermission(java.security.Permission,
 * java.lang.Object) checkPermission}
 * method that includes a context argument are provided
 * for this situation. The
 * {@code getSecurityContext} method returns a "snapshot"
 * of the current calling context. (The default implementation
 * returns an AccessControlContext object.) A sample call is
 * the following:
 *
 * <pre>
 *   Object context = null;
 *   SecurityManager sm = System.getSecurityManager();
 *   if (sm != null) context = sm.getSecurityContext();
 * </pre>
 *
 * <p>
 * The {@code checkPermission} method
 * that takes a context object in addition to a permission
 * makes access decisions based on that context,
 * rather than on that of the current execution thread.
 * Code within a different context can thus call that method,
 * passing the permission and the
 * previously-saved context object. A sample call, using the
 * SecurityManager {@code sm} obtained as in the previous example,
 * is the following:
 *
 * <pre>
 *   if (sm != null) sm.checkPermission(permission, context);
 * </pre>
 *
 * <p>Permissions fall into these categories: File, Socket, Net,
 * Security, Runtime, Property, AWT, Reflect, and Serializable.
 * The classes managing these various
 * permission categories are {@code java.io.FilePermission},
 * {@code java.net.SocketPermission},
 * {@code java.net.NetPermission},
 * {@code java.security.SecurityPermission},
 * {@code java.lang.RuntimePermission},
 * {@code java.util.PropertyPermission},
 * {@code java.awt.AWTPermission},
 * {@code java.lang.reflect.ReflectPermission}, and
 * {@code java.io.SerializablePermission}.
 *
 * <p>All but the first two (FilePermission and SocketPermission) are
 * subclasses of {@code java.security.BasicPermission}, which itself
 * is an abstract subclass of the
 * top-level class for permissions, which is
 * {@code java.security.Permission}. BasicPermission defines the
 * functionality needed for all permissions that contain a name
 * that follows the hierarchical property naming convention
 * (for example, "exitVM", "setFactory", "queuePrintJob", etc).
 * An asterisk
 * may appear at the end of the name, following a ".", or by itself, to
 * signify a wildcard match. For example: "a.*" or "*" is valid,
 * "*a" or "a*b" is not valid.
 *
 * <p>FilePermission and SocketPermission are subclasses of the
 * top-level class for permissions
 * ({@code java.security.Permission}). Classes like these
 * that have a more complicated name syntax than that used by
 * BasicPermission subclass directly from Permission rather than from
 * BasicPermission. For example,
 * for a {@code java.io.FilePermission} object, the permission name is
 * the path name of a file (or directory).
 *
 * <p>Some of the permission classes have an "actions" list that tells
 * the actions that are permitted for the object.  For example,
 * for a {@code java.io.FilePermission} object, the actions list
 * (such as "read, write") specifies which actions are granted for the
 * specified file (or for files in the specified directory).
 *
 * <p>Other permission classes are for "named" permissions -
 * ones that contain a name but no actions list; you either have the
 * named permission or you don't.
 *
 * <p>Note: There is also a {@code java.security.AllPermission}
 * permission that implies all permissions. It exists to simplify the work
 * of system administrators who might need to perform multiple
 * tasks that require all (or numerous) permissions.
 * <p>
 * See {@extLink security_guide_permissions
 * Permissions in the Java Development Kit (JDK)}
 * for permission-related information.
 * This document includes a table listing the various SecurityManager
 * {@code check} methods and the permission(s) the default
 * implementation of each such method requires.
 * It also contains a table of the methods
 * that require permissions, and for each such method tells
 * which permission it requires.
 *
 * @author  Arthur van Hoff
 * @author  Roland Schemers
 *
 * @see     java.lang.ClassLoader
 * @see     java.lang.SecurityException
 * @see     java.lang.System#getSecurityManager() getSecurityManager
 * @see     java.lang.System#setSecurityManager(java.lang.SecurityManager)
 *  setSecurityManager
 * @see     java.security.AccessController AccessController
 * @see     java.security.AccessControlContext AccessControlContext
 * @see     java.security.AccessControlException AccessControlException
 * @see     java.security.Permission
 * @see     java.security.BasicPermission
 * @see     java.io.FilePermission
 * @see     java.net.SocketPermission
 * @see     java.util.PropertyPermission
 * @see     java.lang.RuntimePermission
 * @see     java.security.Policy Policy
 * @see     java.security.SecurityPermission SecurityPermission
 * @see     java.security.ProtectionDomain
 *
 * @since   1.0
 * @deprecated The Security Manager is deprecated and subject to removal in a
 *       future release. There is no replacement for the Security Manager.
 *       See <a href="https://openjdk.java.net/jeps/411">JEP 411</a> for
 *       discussion and alternatives.
 */
@Deprecated(since="17", forRemoval=true)
public class SecurityManager {

    /*
     * Have we been initialized. Effective against finalizer attacks.
     */
    private boolean initialized = false;

    /**
     * Constructs a new {@code SecurityManager}.
     *
     * <p> If there is a security manager already installed, this method first
     * calls the security manager's {@code checkPermission} method
     * with the {@code RuntimePermission("createSecurityManager")}
     * permission to ensure the calling thread has permission to create a new
     * security manager.
     * This may result in throwing a {@code SecurityException}.
     *
     * @throws     java.lang.SecurityException if a security manager already
     *             exists and its {@code checkPermission} method
     *             doesn't allow creation of a new security manager.
     * @see        java.lang.System#getSecurityManager()
     * @see        #checkPermission(java.security.Permission) checkPermission
     * @see java.lang.RuntimePermission
     */
    public SecurityManager() {
        synchronized(SecurityManager.class) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                // ask the currently installed security manager if we
                // can create a new one.
                sm.checkPermission(new RuntimePermission
                                   ("createSecurityManager"));
            }
            initialized = true;
        }
    }

    /**
     * Returns the current execution stack as an array of classes.
     * <p>
     * The length of the array is the number of methods on the execution
     * stack. The element at index {@code 0} is the class of the
     * currently executing method, the element at index {@code 1} is
     * the class of that method's caller, and so on.
     *
     * @return  the execution stack.
     */
    protected native Class<?>[] getClassContext();

    /**
     * Creates an object that encapsulates the current execution
     * environment. The result of this method is used, for example, by the
     * three-argument {@code checkConnect} method and by the
     * two-argument {@code checkRead} method.
     * These methods are needed because a trusted method may be called
     * on to read a file or open a socket on behalf of another method.
     * The trusted method needs to determine if the other (possibly
     * untrusted) method would be allowed to perform the operation on its
     * own.
     * <p> The default implementation of this method is to return
     * an {@code AccessControlContext} object.
     *
     * @return  an implementation-dependent object that encapsulates
     *          sufficient information about the current execution environment
     *          to perform some security checks later.
     * @see     java.lang.SecurityManager#checkConnect(java.lang.String, int,
     *   java.lang.Object) checkConnect
     * @see     java.lang.SecurityManager#checkRead(java.lang.String,
     *   java.lang.Object) checkRead
     * @see     java.security.AccessControlContext AccessControlContext
     */
    @SuppressWarnings("removal")
    public Object getSecurityContext() {
        return AccessController.getContext();
    }

    /**
     * Throws a {@code SecurityException} if the requested
     * access, specified by the given permission, is not permitted based
     * on the security policy currently in effect.
     * <p>
     * This method calls {@code AccessController.checkPermission}
     * with the given permission.
     *
     * @param     perm   the requested permission.
     * @throws    SecurityException if access is not permitted based on
     *            the current security policy.
     * @throws    NullPointerException if the permission argument is
     *            {@code null}.
     * @since     1.2
     */
    @SuppressWarnings("removal")
    public void checkPermission(Permission perm) {
        java.security.AccessController.checkPermission(perm);
    }

    /**
     * Throws a {@code SecurityException} if the
     * specified security context is denied access to the resource
     * specified by the given permission.
     * The context must be a security
     * context returned by a previous call to
     * {@code getSecurityContext} and the access control
     * decision is based upon the configured security policy for
     * that security context.
     * <p>
     * If {@code context} is an instance of
     * {@code AccessControlContext} then the
     * {@code AccessControlContext.checkPermission} method is
     * invoked with the specified permission.
     * <p>
     * If {@code context} is not an instance of
     * {@code AccessControlContext} then a
     * {@code SecurityException} is thrown.
     *
     * @param      perm      the specified permission
     * @param      context   a system-dependent security context.
     * @throws     SecurityException  if the specified security context
     *             is not an instance of {@code AccessControlContext}
     *             (e.g., is {@code null}), or is denied access to the
     *             resource specified by the given permission.
     * @throws     NullPointerException if the permission argument is
     *             {@code null}.
     * @see        java.lang.SecurityManager#getSecurityContext()
     * @see java.security.AccessControlContext#checkPermission(java.security.Permission)
     * @since      1.2
     */
    @SuppressWarnings("removal")
    public void checkPermission(Permission perm, Object context) {
        if (context instanceof AccessControlContext) {
            ((AccessControlContext)context).checkPermission(perm);
        } else {
            throw new SecurityException();
        }
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to create a new class loader.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code RuntimePermission("createClassLoader")}
     * permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkCreateClassLoader}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @throws    SecurityException if the calling thread does not
     *             have permission
     *             to create a new class loader.
     * @see        java.lang.ClassLoader#ClassLoader()
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkCreateClassLoader() {
        checkPermission(SecurityConstants.CREATE_CLASSLOADER_PERMISSION);
    }

    /**
     * reference to the root thread group, used for the checkAccess
     * methods.
     */

    private static ThreadGroup rootGroup = getRootGroup();

    private static ThreadGroup getRootGroup() {
        ThreadGroup root =  Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to modify the thread argument.
     * <p>
     * This method is invoked for the current security manager by the
     * {@code stop}, {@code suspend}, {@code resume},
     * {@code setPriority}, {@code setName}, and
     * {@code setDaemon} methods of class {@code Thread}.
     * <p>
     * If the thread argument is a system thread (belongs to
     * the thread group with a {@code null} parent) then
     * this method calls {@code checkPermission} with the
     * {@code RuntimePermission("modifyThread")} permission.
     * If the thread argument is <i>not</i> a system thread,
     * this method just returns silently.
     * <p>
     * Applications that want a stricter policy should override this
     * method. If this method is overridden, the method that overrides
     * it should additionally check to see if the calling thread has the
     * {@code RuntimePermission("modifyThread")} permission, and
     * if so, return silently. This is to ensure that code granted
     * that permission (such as the JDK itself) is allowed to
     * manipulate any thread.
     * <p>
     * If this method is overridden, then
     * {@code super.checkAccess} should
     * be called by the first statement in the overridden method, or the
     * equivalent security check should be placed in the overridden method.
     *
     * @param      t   the thread to be checked.
     * @throws     SecurityException  if the calling thread does not have
     *             permission to modify the thread.
     * @throws     NullPointerException if the thread argument is
     *             {@code null}.
     * @see        java.lang.Thread#resume() resume
     * @see        java.lang.Thread#setDaemon(boolean) setDaemon
     * @see        java.lang.Thread#setName(java.lang.String) setName
     * @see        java.lang.Thread#setPriority(int) setPriority
     * @see        java.lang.Thread#stop() stop
     * @see        java.lang.Thread#suspend() suspend
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkAccess(Thread t) {
        if (t == null) {
            throw new NullPointerException("thread can't be null");
        }
        if (t.getThreadGroup() == rootGroup) {
            checkPermission(SecurityConstants.MODIFY_THREAD_PERMISSION);
        } else {
            // just return
        }
    }
    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to modify the thread group argument.
     * <p>
     * This method is invoked for the current security manager when a
     * new child thread or child thread group is created, and by the
     * {@code setDaemon}, {@code setMaxPriority},
     * {@code stop}, {@code suspend}, {@code resume}, and
     * {@code destroy} methods of class {@code ThreadGroup}.
     * <p>
     * If the thread group argument is the system thread group (
     * has a {@code null} parent) then
     * this method calls {@code checkPermission} with the
     * {@code RuntimePermission("modifyThreadGroup")} permission.
     * If the thread group argument is <i>not</i> the system thread group,
     * this method just returns silently.
     * <p>
     * Applications that want a stricter policy should override this
     * method. If this method is overridden, the method that overrides
     * it should additionally check to see if the calling thread has the
     * {@code RuntimePermission("modifyThreadGroup")} permission, and
     * if so, return silently. This is to ensure that code granted
     * that permission (such as the JDK itself) is allowed to
     * manipulate any thread.
     * <p>
     * If this method is overridden, then
     * {@code super.checkAccess} should
     * be called by the first statement in the overridden method, or the
     * equivalent security check should be placed in the overridden method.
     *
     * @param      g   the thread group to be checked.
     * @throws     SecurityException  if the calling thread does not have
     *             permission to modify the thread group.
     * @throws     NullPointerException if the thread group argument is
     *             {@code null}.
     * @see        java.lang.ThreadGroup#destroy() destroy
     * @see        java.lang.ThreadGroup#resume() resume
     * @see        java.lang.ThreadGroup#setDaemon(boolean) setDaemon
     * @see        java.lang.ThreadGroup#setMaxPriority(int) setMaxPriority
     * @see        java.lang.ThreadGroup#stop() stop
     * @see        java.lang.ThreadGroup#suspend() suspend
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkAccess(ThreadGroup g) {
        if (g == null) {
            throw new NullPointerException("thread group can't be null");
        }
        if (g == rootGroup) {
            checkPermission(SecurityConstants.MODIFY_THREADGROUP_PERMISSION);
        } else {
            // just return
        }
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to cause the Java Virtual Machine to
     * halt with the specified status code.
     * <p>
     * This method is invoked for the current security manager by the
     * {@code exit} method of class {@code Runtime}. A status
     * of {@code 0} indicates success; other values indicate various
     * errors.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code RuntimePermission("exitVM."+status)} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkExit}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      status   the exit status.
     * @throws    SecurityException if the calling thread does not have
     *              permission to halt the Java Virtual Machine with
     *              the specified status.
     * @see        java.lang.Runtime#exit(int) exit
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkExit(int status) {
        checkPermission(new RuntimePermission("exitVM."+status));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to create a subprocess.
     * <p>
     * This method is invoked for the current security manager by the
     * {@code exec} methods of class {@code Runtime}.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code FilePermission(cmd,"execute")} permission
     * if cmd is an absolute path, otherwise it calls
     * {@code checkPermission} with
     * <code>FilePermission("&lt;&lt;ALL FILES&gt;&gt;","execute")</code>.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkExec}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      cmd   the specified system command.
     * @throws     SecurityException if the calling thread does not have
     *             permission to create a subprocess.
     * @throws     NullPointerException if the {@code cmd} argument is
     *             {@code null}.
     * @see     java.lang.Runtime#exec(java.lang.String)
     * @see     java.lang.Runtime#exec(java.lang.String, java.lang.String[])
     * @see     java.lang.Runtime#exec(java.lang.String[])
     * @see     java.lang.Runtime#exec(java.lang.String[], java.lang.String[])
     * @see     #checkPermission(java.security.Permission) checkPermission
     */
    public void checkExec(String cmd) {
        File f = new File(cmd);
        if (f.isAbsolute()) {
            checkPermission(new FilePermission(cmd,
                SecurityConstants.FILE_EXECUTE_ACTION));
        } else {
            checkPermission(new FilePermission("<<ALL FILES>>",
                SecurityConstants.FILE_EXECUTE_ACTION));
        }
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to dynamic link the library code
     * specified by the string argument file. The argument is either a
     * simple library name or a complete filename.
     * <p>
     * This method is invoked for the current security manager by
     * methods {@code load} and {@code loadLibrary} of class
     * {@code Runtime}.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code RuntimePermission("loadLibrary."+lib)} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkLink}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      lib   the name of the library.
     * @throws     SecurityException if the calling thread does not have
     *             permission to dynamically link the library.
     * @throws     NullPointerException if the {@code lib} argument is
     *             {@code null}.
     * @see        java.lang.Runtime#load(java.lang.String)
     * @see        java.lang.Runtime#loadLibrary(java.lang.String)
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkLink(String lib) {
        if (lib == null) {
            throw new NullPointerException("library can't be null");
        }
        checkPermission(new RuntimePermission("loadLibrary."+lib));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to read from the specified file
     * descriptor.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code RuntimePermission("readFileDescriptor")}
     * permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkRead}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      fd   the system-dependent file descriptor.
     * @throws     SecurityException  if the calling thread does not have
     *             permission to access the specified file descriptor.
     * @throws     NullPointerException if the file descriptor argument is
     *             {@code null}.
     * @see        java.io.FileDescriptor
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkRead(FileDescriptor fd) {
        if (fd == null) {
            throw new NullPointerException("file descriptor can't be null");
        }
        checkPermission(new RuntimePermission("readFileDescriptor"));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to read the file specified by the
     * string argument.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code FilePermission(file,"read")} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkRead}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      file   the system-dependent file name.
     * @throws     SecurityException if the calling thread does not have
     *             permission to access the specified file.
     * @throws     NullPointerException if the {@code file} argument is
     *             {@code null}.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkRead(String file) {
        checkPermission(new FilePermission(file,
            SecurityConstants.FILE_READ_ACTION));
    }

    /**
     * Throws a {@code SecurityException} if the
     * specified security context is not allowed to read the file
     * specified by the string argument. The context must be a security
     * context returned by a previous call to
     * {@code getSecurityContext}.
     * <p> If {@code context} is an instance of
     * {@code AccessControlContext} then the
     * {@code AccessControlContext.checkPermission} method will
     * be invoked with the {@code FilePermission(file,"read")} permission.
     * <p> If {@code context} is not an instance of
     * {@code AccessControlContext} then a
     * {@code SecurityException} is thrown.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkRead}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      file      the system-dependent filename.
     * @param      context   a system-dependent security context.
     * @throws     SecurityException  if the specified security context
     *             is not an instance of {@code AccessControlContext}
     *             (e.g., is {@code null}), or does not have permission
     *             to read the specified file.
     * @throws     NullPointerException if the {@code file} argument is
     *             {@code null}.
     * @see        java.lang.SecurityManager#getSecurityContext()
     * @see        java.security.AccessControlContext#checkPermission(java.security.Permission)
     */
    public void checkRead(String file, Object context) {
        checkPermission(
            new FilePermission(file, SecurityConstants.FILE_READ_ACTION),
            context);
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to write to the specified file
     * descriptor.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code RuntimePermission("writeFileDescriptor")}
     * permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkWrite}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      fd   the system-dependent file descriptor.
     * @throws    SecurityException  if the calling thread does not have
     *             permission to access the specified file descriptor.
     * @throws     NullPointerException if the file descriptor argument is
     *             {@code null}.
     * @see        java.io.FileDescriptor
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkWrite(FileDescriptor fd) {
        if (fd == null) {
            throw new NullPointerException("file descriptor can't be null");
        }
        checkPermission(new RuntimePermission("writeFileDescriptor"));

    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to write to the file specified by
     * the string argument.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code FilePermission(file,"write")} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkWrite}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      file   the system-dependent filename.
     * @throws     SecurityException  if the calling thread does not
     *             have permission to access the specified file.
     * @throws     NullPointerException if the {@code file} argument is
     *             {@code null}.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkWrite(String file) {
        checkPermission(new FilePermission(file,
            SecurityConstants.FILE_WRITE_ACTION));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to delete the specified file.
     * <p>
     * This method is invoked for the current security manager by the
     * {@code delete} method of class {@code File}.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code FilePermission(file,"delete")} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkDelete}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      file   the system-dependent filename.
     * @throws     SecurityException if the calling thread does not
     *             have permission to delete the file.
     * @throws     NullPointerException if the {@code file} argument is
     *             {@code null}.
     * @see        java.io.File#delete()
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkDelete(String file) {
        checkPermission(new FilePermission(file,
            SecurityConstants.FILE_DELETE_ACTION));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to open a socket connection to the
     * specified host and port number.
     * <p>
     * A port number of {@code -1} indicates that the calling
     * method is attempting to determine the IP address of the specified
     * host name.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code SocketPermission(host+":"+port,"connect")} permission if
     * the port is not equal to -1. If the port is equal to -1, then
     * it calls {@code checkPermission} with the
     * {@code SocketPermission(host,"resolve")} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkConnect}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      host   the host name port to connect to.
     * @param      port   the protocol port to connect to.
     * @throws     SecurityException  if the calling thread does not have
     *             permission to open a socket connection to the specified
     *               {@code host} and {@code port}.
     * @throws     NullPointerException if the {@code host} argument is
     *             {@code null}.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkConnect(String host, int port) {
        if (host == null) {
            throw new NullPointerException("host can't be null");
        }
        if (!host.startsWith("[") && host.indexOf(':') != -1) {
            host = "[" + host + "]";
        }
        if (port == -1) {
            checkPermission(new SocketPermission(host,
                SecurityConstants.SOCKET_RESOLVE_ACTION));
        } else {
            checkPermission(new SocketPermission(host+":"+port,
                SecurityConstants.SOCKET_CONNECT_ACTION));
        }
    }

    /**
     * Throws a {@code SecurityException} if the
     * specified security context is not allowed to open a socket
     * connection to the specified host and port number.
     * <p>
     * A port number of {@code -1} indicates that the calling
     * method is attempting to determine the IP address of the specified
     * host name.
     * <p> If {@code context} is not an instance of
     * {@code AccessControlContext} then a
     * {@code SecurityException} is thrown.
     * <p>
     * Otherwise, the port number is checked. If it is not equal
     * to -1, the {@code context}'s {@code checkPermission}
     * method is called with a
     * {@code SocketPermission(host+":"+port,"connect")} permission.
     * If the port is equal to -1, then
     * the {@code context}'s {@code checkPermission} method
     * is called with a
     * {@code SocketPermission(host,"resolve")} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkConnect}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      host      the host name port to connect to.
     * @param      port      the protocol port to connect to.
     * @param      context   a system-dependent security context.
     * @throws     SecurityException if the specified security context
     *             is not an instance of {@code AccessControlContext}
     *             (e.g., is {@code null}), or does not have permission
     *             to open a socket connection to the specified
     *             {@code host} and {@code port}.
     * @throws     NullPointerException if the {@code host} argument is
     *             {@code null}.
     * @see        java.lang.SecurityManager#getSecurityContext()
     * @see        java.security.AccessControlContext#checkPermission(java.security.Permission)
     */
    public void checkConnect(String host, int port, Object context) {
        if (host == null) {
            throw new NullPointerException("host can't be null");
        }
        if (!host.startsWith("[") && host.indexOf(':') != -1) {
            host = "[" + host + "]";
        }
        if (port == -1)
            checkPermission(new SocketPermission(host,
                SecurityConstants.SOCKET_RESOLVE_ACTION),
                context);
        else
            checkPermission(new SocketPermission(host+":"+port,
                SecurityConstants.SOCKET_CONNECT_ACTION),
                context);
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to wait for a connection request on
     * the specified local port number.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code SocketPermission("localhost:"+port,"listen")}.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkListen}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      port   the local port.
     * @throws     SecurityException  if the calling thread does not have
     *             permission to listen on the specified port.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkListen(int port) {
        checkPermission(new SocketPermission("localhost:"+port,
            SecurityConstants.SOCKET_LISTEN_ACTION));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not permitted to accept a socket connection from
     * the specified host and port number.
     * <p>
     * This method is invoked for the current security manager by the
     * {@code accept} method of class {@code ServerSocket}.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code SocketPermission(host+":"+port,"accept")} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkAccept}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      host   the host name of the socket connection.
     * @param      port   the port number of the socket connection.
     * @throws     SecurityException  if the calling thread does not have
     *             permission to accept the connection.
     * @throws     NullPointerException if the {@code host} argument is
     *             {@code null}.
     * @see        java.net.ServerSocket#accept()
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkAccept(String host, int port) {
        if (host == null) {
            throw new NullPointerException("host can't be null");
        }
        if (!host.startsWith("[") && host.indexOf(':') != -1) {
            host = "[" + host + "]";
        }
        checkPermission(new SocketPermission(host+":"+port,
            SecurityConstants.SOCKET_ACCEPT_ACTION));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to use
     * (join/leave/send/receive) IP multicast.
     * <p>
     * This method calls {@code checkPermission} with the
     * <code>java.net.SocketPermission(maddr.getHostAddress(),
     * "accept,connect")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkMulticast}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      maddr  Internet group address to be used.
     * @throws     SecurityException  if the calling thread is not allowed to
     *  use (join/leave/send/receive) IP multicast.
     * @throws     NullPointerException if the address argument is
     *             {@code null}.
     * @since      1.1
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkMulticast(InetAddress maddr) {
        String host = maddr.getHostAddress();
        if (!host.startsWith("[") && host.indexOf(':') != -1) {
            host = "[" + host + "]";
        }
        checkPermission(new SocketPermission(host,
            SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to use
     * (join/leave/send/receive) IP multicast.
     * <p>
     * This method calls {@code checkPermission} with the
     * <code>java.net.SocketPermission(maddr.getHostAddress(),
     * "accept,connect")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkMulticast}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      maddr  Internet group address to be used.
     * @param      ttl        value in use, if it is multicast send.
     * Note: this particular implementation does not use the ttl
     * parameter.
     * @throws     SecurityException  if the calling thread is not allowed to
     *  use (join/leave/send/receive) IP multicast.
     * @throws     NullPointerException if the address argument is
     *             {@code null}.
     * @since      1.1
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    @Deprecated(since="1.4", forRemoval=true)
    public void checkMulticast(InetAddress maddr, byte ttl) {
        String host = maddr.getHostAddress();
        if (!host.startsWith("[") && host.indexOf(':') != -1) {
            host = "[" + host + "]";
        }
        checkPermission(new SocketPermission(host,
            SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to access or modify the system
     * properties.
     * <p>
     * This method is used by the {@code getProperties} and
     * {@code setProperties} methods of class {@code System}.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code PropertyPermission("*", "read,write")} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkPropertiesAccess}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @throws     SecurityException  if the calling thread does not have
     *             permission to access or modify the system properties.
     * @see        java.lang.System#getProperties()
     * @see        java.lang.System#setProperties(java.util.Properties)
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkPropertiesAccess() {
        checkPermission(new PropertyPermission("*",
            SecurityConstants.PROPERTY_RW_ACTION));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to access the system property with
     * the specified {@code key} name.
     * <p>
     * This method is used by the {@code getProperty} method of
     * class {@code System}.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code PropertyPermission(key, "read")} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkPropertyAccess}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      key   a system property key.
     *
     * @throws     SecurityException  if the calling thread does not have
     *             permission to access the specified system property.
     * @throws     NullPointerException if the {@code key} argument is
     *             {@code null}.
     * @throws     IllegalArgumentException if {@code key} is empty.
     *
     * @see        java.lang.System#getProperty(java.lang.String)
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkPropertyAccess(String key) {
        checkPermission(new PropertyPermission(key,
            SecurityConstants.PROPERTY_READ_ACTION));
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to initiate a print job request.
     * <p>
     * This method calls
     * {@code checkPermission} with the
     * {@code RuntimePermission("queuePrintJob")} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkPrintJobAccess}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @throws     SecurityException  if the calling thread does not have
     *             permission to initiate a print job request.
     * @since   1.1
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkPrintJobAccess() {
        checkPermission(new RuntimePermission("queuePrintJob"));
    }

    /*
     * We have an initial invalid bit (initially false) for the class
     * variables which tell if the cache is valid.  If the underlying
     * java.security.Security property changes via setProperty(), the
     * Security class uses reflection to change the variable and thus
     * invalidate the cache.
     *
     * Locking is handled by synchronization to the
     * packageAccessLock/packageDefinitionLock objects.  They are only
     * used in this class.
     *
     * Note that cache invalidation as a result of the property change
     * happens without using these locks, so there may be a delay between
     * when a thread updates the property and when other threads updates
     * the cache.
     */
    private static boolean packageAccessValid = false;
    private static String[] packageAccess;
    private static final Object packageAccessLock = new Object();

    private static boolean packageDefinitionValid = false;
    private static String[] packageDefinition;
    private static final Object packageDefinitionLock = new Object();

    private static String[] getPackages(String p) {
        String packages[] = null;
        if (p != null && !p.isEmpty()) {
            java.util.StringTokenizer tok =
                new java.util.StringTokenizer(p, ",");
            int n = tok.countTokens();
            if (n > 0) {
                packages = new String[n];
                int i = 0;
                while (tok.hasMoreElements()) {
                    String s = tok.nextToken().trim();
                    packages[i++] = s;
                }
            }
        }

        if (packages == null) {
            packages = new String[0];
        }
        return packages;
    }

    // The non-exported packages in modules defined to the boot or platform
    // class loaders. A non-exported package is a package that is not exported
    // or is only exported to specific modules.
    private static final Map<String, Boolean> nonExportedPkgs = new ConcurrentHashMap<>();
    static {
        addNonExportedPackages(ModuleLayer.boot());
    }

    /**
     * Record the non-exported packages of the modules in the given layer
     */
    static void addNonExportedPackages(ModuleLayer layer) {
        Set<String> bootModules = ModuleLoaderMap.bootModules();
        Set<String> platformModules = ModuleLoaderMap.platformModules();
        layer.modules().stream()
                .map(Module::getDescriptor)
                .filter(md -> bootModules.contains(md.name())
                        || platformModules.contains(md.name()))
                .map(SecurityManager::nonExportedPkgs)
                .flatMap(Set::stream)
                .forEach(pn -> nonExportedPkgs.put(pn, Boolean.TRUE));
    }


    /**
     * Called by java.security.Security
     */
    static void invalidatePackageAccessCache() {
        synchronized (packageAccessLock) {
            packageAccessValid = false;
        }
        synchronized (packageDefinitionLock) {
            packageDefinitionValid = false;
        }
    }

    /**
     * Returns the non-exported packages of the specified module.
     */
    private static Set<String> nonExportedPkgs(ModuleDescriptor md) {
        // start with all packages in the module
        Set<String> pkgs = new HashSet<>(md.packages());

        // remove the non-qualified exported packages
        md.exports().stream()
                    .filter(p -> !p.isQualified())
                    .map(Exports::source)
                    .forEach(pkgs::remove);

        // remove the non-qualified open packages
        md.opens().stream()
                  .filter(p -> !p.isQualified())
                  .map(Opens::source)
                  .forEach(pkgs::remove);

        return pkgs;
    }

    /**
     * Throws a {@code SecurityException} if the calling thread is not allowed
     * to access the specified package.
     * <p>
     * During class loading, this method may be called by the {@code loadClass}
     * method of class loaders and by the Java Virtual Machine to ensure that
     * the caller is allowed to access the package of the class that is
     * being loaded.
     * <p>
     * This method checks if the specified package starts with or equals
     * any of the packages in the {@code package.access} Security Property.
     * An implementation may also check the package against an additional
     * list of restricted packages as noted below. If the package is restricted,
     * {@link #checkPermission(Permission)} is called with a
     * {@code RuntimePermission("accessClassInPackage."+pkg)} permission.
     * <p>
     * If this method is overridden, then {@code super.checkPackageAccess}
     * should be called as the first line in the overridden method.
     *
     * @implNote
     * This implementation also restricts all non-exported packages of modules
     * loaded by {@linkplain ClassLoader#getPlatformClassLoader
     * the platform class loader} or its ancestors. A "non-exported package"
     * refers to a package that is not exported to all modules. Specifically,
     * it refers to a package that either is not exported at all by its
     * containing module or is exported in a qualified fashion by its
     * containing module.
     *
     * @param      pkg   the package name.
     * @throws     SecurityException  if the calling thread does not have
     *             permission to access the specified package.
     * @throws     NullPointerException if the package name argument is
     *             {@code null}.
     * @see        java.lang.ClassLoader#loadClass(String, boolean) loadClass
     * @see        java.security.Security#getProperty getProperty
     * @see        #checkPermission(Permission) checkPermission
     */
    public void checkPackageAccess(String pkg) {
        Objects.requireNonNull(pkg, "package name can't be null");

        // check if pkg is not exported to all modules
        if (nonExportedPkgs.containsKey(pkg)) {
            checkPermission(
                new RuntimePermission("accessClassInPackage." + pkg));
            return;
        }

        String[] restrictedPkgs;
        synchronized (packageAccessLock) {
            /*
             * Do we need to update our property array?
             */
            if (!packageAccessValid) {
                @SuppressWarnings("removal")
                String tmpPropertyStr =
                    AccessController.doPrivileged(
                        new PrivilegedAction<>() {
                            public String run() {
                                return Security.getProperty("package.access");
                            }
                        }
                    );
                packageAccess = getPackages(tmpPropertyStr);
                packageAccessValid = true;
            }

            // Using a snapshot of packageAccess -- don't care if static field
            // changes afterwards; array contents won't change.
            restrictedPkgs = packageAccess;
        }

        /*
         * Traverse the list of packages, check for any matches.
         */
        final int plen = pkg.length();
        for (String restrictedPkg : restrictedPkgs) {
            final int rlast = restrictedPkg.length() - 1;

            // Optimizations:
            //
            // If rlast >= plen then restrictedPkg is longer than pkg by at
            // least one char. This means pkg cannot start with restrictedPkg,
            // since restrictedPkg will be longer than pkg.
            //
            // Similarly if rlast != plen, then pkg + "." cannot be the same
            // as restrictedPkg, since pkg + "." will have a different length
            // than restrictedPkg.
            //
            if (rlast < plen && pkg.startsWith(restrictedPkg) ||
                // The following test is equivalent to
                // restrictedPkg.equals(pkg + ".") but is noticeably more
                // efficient:
                rlast == plen && restrictedPkg.startsWith(pkg) &&
                restrictedPkg.charAt(rlast) == '.')
            {
                checkPermission(
                    new RuntimePermission("accessClassInPackage." + pkg));
                break;  // No need to continue; only need to check this once
            }
        }
    }

    /**
     * Throws a {@code SecurityException} if the calling thread is not
     * allowed to define classes in the specified package.
     * <p>
     * This method is called by the {@code loadClass} method of some
     * class loaders.
     * <p>
     * This method checks if the specified package starts with or equals
     * any of the packages in the {@code package.definition} Security
     * Property. An implementation may also check the package against an
     * additional list of restricted packages as noted below. If the package
     * is restricted, {@link #checkPermission(Permission)} is called with a
     * {@code RuntimePermission("defineClassInPackage."+pkg)} permission.
     * <p>
     * If this method is overridden, then {@code super.checkPackageDefinition}
     * should be called as the first line in the overridden method.
     *
     * @implNote
     * This implementation also restricts all non-exported packages of modules
     * loaded by {@linkplain ClassLoader#getPlatformClassLoader
     * the platform class loader} or its ancestors. A "non-exported package"
     * refers to a package that is not exported to all modules. Specifically,
     * it refers to a package that either is not exported at all by its
     * containing module or is exported in a qualified fashion by its
     * containing module.
     *
     * @param      pkg   the package name.
     * @throws     SecurityException  if the calling thread does not have
     *             permission to define classes in the specified package.
     * @throws     NullPointerException if the package name argument is
     *             {@code null}.
     * @see        java.lang.ClassLoader#loadClass(String, boolean)
     * @see        java.security.Security#getProperty getProperty
     * @see        #checkPermission(Permission) checkPermission
     */
    public void checkPackageDefinition(String pkg) {
        Objects.requireNonNull(pkg, "package name can't be null");

        // check if pkg is not exported to all modules
        if (nonExportedPkgs.containsKey(pkg)) {
            checkPermission(
                new RuntimePermission("defineClassInPackage." + pkg));
            return;
        }

        String[] pkgs;
        synchronized (packageDefinitionLock) {
            /*
             * Do we need to update our property array?
             */
            if (!packageDefinitionValid) {
                @SuppressWarnings("removal")
                String tmpPropertyStr =
                    AccessController.doPrivileged(
                        new PrivilegedAction<>() {
                            public String run() {
                                return java.security.Security.getProperty(
                                    "package.definition");
                            }
                        }
                    );
                packageDefinition = getPackages(tmpPropertyStr);
                packageDefinitionValid = true;
            }
            // Using a snapshot of packageDefinition -- don't care if static
            // field changes afterwards; array contents won't change.
            pkgs = packageDefinition;
        }

        /*
         * Traverse the list of packages, check for any matches.
         */
        for (String restrictedPkg : pkgs) {
            if (pkg.startsWith(restrictedPkg) || restrictedPkg.equals(pkg + ".")) {
                checkPermission(
                    new RuntimePermission("defineClassInPackage." + pkg));
                break; // No need to continue; only need to check this once
            }
        }
    }

    /**
     * Throws a {@code SecurityException} if the
     * calling thread is not allowed to set the socket factory used by
     * {@code ServerSocket} or {@code Socket}, or the stream
     * handler factory used by {@code URL}.
     * <p>
     * This method calls {@code checkPermission} with the
     * {@code RuntimePermission("setFactory")} permission.
     * <p>
     * If you override this method, then you should make a call to
     * {@code super.checkSetFactory}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @throws     SecurityException  if the calling thread does not have
     *             permission to specify a socket factory or a stream
     *             handler factory.
     *
     * @see        java.net.ServerSocket#setSocketFactory(java.net.SocketImplFactory) setSocketFactory
     * @see        java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory) setSocketImplFactory
     * @see        java.net.URL#setURLStreamHandlerFactory(java.net.URLStreamHandlerFactory) setURLStreamHandlerFactory
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkSetFactory() {
        checkPermission(new RuntimePermission("setFactory"));
    }

    /**
     * Determines whether the permission with the specified permission target
     * name should be granted or denied.
     *
     * <p> If the requested permission is allowed, this method returns
     * quietly. If denied, a SecurityException is raised.
     *
     * <p> This method creates a {@code SecurityPermission} object for
     * the given permission target name and calls {@code checkPermission}
     * with it.
     *
     * <p> See the documentation for
     * <code>{@link java.security.SecurityPermission}</code> for
     * a list of possible permission target names.
     *
     * <p> If you override this method, then you should make a call to
     * {@code super.checkSecurityAccess}
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param target the target name of the {@code SecurityPermission}.
     *
     * @throws    SecurityException if the calling thread does not have
     * permission for the requested access.
     * @throws    NullPointerException if {@code target} is null.
     * @throws    IllegalArgumentException if {@code target} is empty.
     *
     * @since   1.1
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkSecurityAccess(String target) {
        checkPermission(new SecurityPermission(target));
    }

    /**
     * Returns the thread group into which to instantiate any new
     * thread being created at the time this is being called.
     * By default, it returns the thread group of the current
     * thread. This should be overridden by a specific security
     * manager to return the appropriate thread group.
     *
     * @return  ThreadGroup that new threads are instantiated into
     * @since   1.1
     * @see     java.lang.ThreadGroup
     */
    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }

}
