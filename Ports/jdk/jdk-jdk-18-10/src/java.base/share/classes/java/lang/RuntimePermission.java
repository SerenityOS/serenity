/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.lang.module.ModuleFinder;

/**
 * This class is for runtime permissions. A {@code RuntimePermission}
 * contains a name (also referred to as a "target name") but no actions
 * list; you either have the named permission or you don't.
 * <p>
 * The target name is the name of the runtime permission (see below). The
 * naming convention follows the  hierarchical property naming convention.
 * Also, an asterisk may appear at the end of the name, following a ".",
 * or by itself, to signify a wildcard match. For example: "loadLibrary.*"
 * and "*" signify a wildcard match, while "*loadLibrary" and "a*b" do not.
 * <p>
 * The following table lists the standard {@code RuntimePermission}
 * target names, and for each provides a description of what the permission
 * allows and a discussion of the risks of granting code the permission.
 *
 * <table class="striped">
 * <caption style="display:none">permission target name,
 *  what the target allows, and associated risks</caption>
 * <thead>
 * <tr>
 * <th scope="col">Permission Target Name</th>
 * <th scope="col">What the Permission Allows</th>
 * <th scope="col">Risks of Allowing this Permission</th>
 * </tr>
 * </thead>
 * <tbody>
 *
 * <tr>
 *   <th scope="row">createClassLoader</th>
 *   <td>Creation of a class loader</td>
 *   <td>This is an extremely dangerous permission to grant.
 * Malicious applications that can instantiate their own class
 * loaders could then load their own rogue classes into the system.
 * These newly loaded classes could be placed into any protection
 * domain by the class loader, thereby automatically granting the
 * classes the permissions for that domain.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getClassLoader</th>
 *   <td>Retrieval of a class loader (e.g., the class loader for the calling
 * class)</td>
 *   <td>This would grant an attacker permission to get the
 * class loader for a particular class. This is dangerous because
 * having access to a class's class loader allows the attacker to
 * load other classes available to that class loader. The attacker
 * would typically otherwise not have access to those classes.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setContextClassLoader</th>
 *   <td>Setting of the context class loader used by a thread</td>
 *   <td>The context class loader is used by system code and extensions
 * when they need to lookup resources that might not exist in the system
 * class loader. Granting setContextClassLoader permission would allow
 * code to change which context class loader is used
 * for a particular thread, including system threads.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">enableContextClassLoaderOverride</th>
 *   <td>Subclass implementation of the thread context class loader methods</td>
 *   <td>The context class loader is used by system code and extensions
 * when they need to lookup resources that might not exist in the system
 * class loader. Granting enableContextClassLoaderOverride permission would allow
 * a subclass of Thread to override the methods that are used
 * to get or set the context class loader for a particular thread.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">closeClassLoader</th>
 *   <td>Closing of a ClassLoader</td>
 *   <td>Granting this permission allows code to close any URLClassLoader
 * that it has a reference to.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setSecurityManager</th>
 *   <td>Setting of the security manager (possibly replacing an existing one)
 * </td>
 *   <td>The security manager is a class that allows
 * applications to implement a security policy. Granting the setSecurityManager
 * permission would allow code to change which security manager is used by
 * installing a different, possibly less restrictive security manager,
 * thereby bypassing checks that would have been enforced by the original
 * security manager.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">createSecurityManager</th>
 *   <td>Creation of a new security manager</td>
 *   <td>This gives code access to protected, sensitive methods that may
 * disclose information about other classes or the execution stack.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getenv.{variable name}</th>
 *   <td>Reading of the value of the specified environment variable</td>
 *   <td>This would allow code to read the value, or determine the
 *       existence, of a particular environment variable.  This is
 *       dangerous if the variable contains confidential data.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">exitVM.{exit status}</th>
 *   <td>Halting of the Java Virtual Machine with the specified exit status</td>
 *   <td>This allows an attacker to mount a denial-of-service attack
 * by automatically forcing the virtual machine to halt.
 * Note: The "exitVM.*" permission is automatically granted to all code
 * loaded from the application class path, thus enabling applications
 * to terminate themselves. Also, the "exitVM" permission is equivalent to
 * "exitVM.*".</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">shutdownHooks</th>
 *   <td>Registration and cancellation of virtual-machine shutdown hooks</td>
 *   <td>This allows an attacker to register a malicious shutdown
 * hook that interferes with the clean shutdown of the virtual machine.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setFactory</th>
 *   <td>Setting of the socket factory used by ServerSocket or Socket,
 * or of the stream handler factory used by URL</td>
 *   <td>This allows code to set the actual implementation
 * for the socket, server socket, stream handler, or RMI socket factory.
 * An attacker may set a faulty implementation which mangles the data
 * stream.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setIO</th>
 *   <td>Setting of System.out, System.in, and System.err</td>
 *   <td>This allows changing the value of the standard system streams.
 * An attacker may change System.in to monitor and
 * steal user input, or may set System.err to a "null" OutputStream,
 * which would hide any error messages sent to System.err. </td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">modifyThread</th>
 *   <td>Modification of threads, e.g., via calls to Thread
 * {@code interrupt, stop, suspend, resume, setDaemon, setPriority,
 * setName} and {@code setUncaughtExceptionHandler}
 * methods</td>
 * <td>This allows an attacker to modify the behaviour of
 * any thread in the system.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">stopThread</th>
 *   <td>Stopping of threads via calls to the Thread {@code stop}
 * method</td>
 *   <td>This allows code to stop any thread in the system provided that it is
 * already granted permission to access that thread.
 * This poses as a threat, because that code may corrupt the system by
 * killing existing threads.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">modifyThreadGroup</th>
 *   <td>modification of thread groups, e.g., via calls to ThreadGroup
 * {@code destroy}, {@code getParent}, {@code resume},
 * {@code setDaemon}, {@code setMaxPriority}, {@code stop},
 * and {@code suspend} methods</td>
 *   <td>This allows an attacker to create thread groups and
 * set their run priority.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getProtectionDomain</th>
 *   <td>Retrieval of the ProtectionDomain for a class</td>
 *   <td>This allows code to obtain policy information
 * for a particular code source. While obtaining policy information
 * does not compromise the security of the system, it does give
 * attackers additional information, such as local file names for
 * example, to better aim an attack.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getFileSystemAttributes</th>
 *   <td>Retrieval of file system attributes</td>
 *   <td>This allows code to obtain file system information such as disk usage
 *       or disk space available to the caller.  This is potentially dangerous
 *       because it discloses information about the system hardware
 *       configuration and some information about the caller's privilege to
 *       write files.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">readFileDescriptor</th>
 *   <td>Reading of file descriptors</td>
 *   <td>This would allow code to read the particular file associated
 *       with the file descriptor read. This is dangerous if the file
 *       contains confidential data.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">writeFileDescriptor</th>
 *   <td>Writing to file descriptors</td>
 *   <td>This allows code to write to a particular file associated
 *       with the descriptor. This is dangerous because it may allow
 *       malicious code to plant viruses or at the very least, fill up
 *       your entire disk.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">loadLibrary.{library name}</th>
 *   <td>Dynamic linking of the specified library</td>
 *   <td>It is dangerous to allow an applet permission to load native code
 * libraries, because the Java security architecture is not designed to and
 * does not prevent malicious behavior at the level of native code.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">accessClassInPackage.{package name}</th>
 *   <td>Access to the specified package via a class loader's
 * {@code loadClass} method when that class loader calls
 * the SecurityManager {@code checkPackageAccess} method</td>
 *   <td>This gives code access to classes in packages
 * to which it normally does not have access. Malicious code
 * may use these classes to help in its attempt to compromise
 * security in the system.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">defineClassInPackage.{package name}</th>
 *   <td>Definition of classes in the specified package, via a class
 * loader's {@code defineClass} method when that class loader calls
 * the SecurityManager {@code checkPackageDefinition} method.</td>
 *   <td>This grants code permission to define a class
 * in a particular package. This is dangerous because malicious
 * code with this permission may define rogue classes in
 * trusted packages like {@code java.security} or {@code java.lang},
 * for example.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">defineClass</th>
 *   <td>Define a class with
 * {@link java.lang.invoke.MethodHandles.Lookup#defineClass(byte[])
 * Lookup.defineClass}.</td>
 *   <td>This grants code with a suitably privileged {@code Lookup} object
 * permission to define classes in the same package as the {@code Lookup}'s
 * lookup class. </td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">accessDeclaredMembers</th>
 *   <td>Access to the declared members of a class</td>
 *   <td>This grants code permission to query a class for its public,
 * protected, default (package) access, and private fields and/or
 * methods. Although the code would have
 * access to the private and protected field and method names, it would not
 * have access to the private/protected field data and would not be able
 * to invoke any private methods. Nevertheless, malicious code
 * may use this information to better aim an attack.
 * Additionally, it may invoke any public methods and/or access public fields
 * in the class.  This could be dangerous if
 * the code would normally not be able to invoke those methods and/or
 * access the fields  because
 * it can't cast the object to the class/interface with those methods
 * and fields.</td>
 * </tr>
 * <tr>
 *   <th scope="row">queuePrintJob</th>
 *   <td>Initiation of a print job request</td>
 *   <td>This could print sensitive information to a printer,
 * or simply waste paper.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getStackTrace</th>
 *   <td>Retrieval of the stack trace information of another thread.</td>
 *   <td>This allows retrieval of the stack trace information of
 * another thread.  This might allow malicious code to monitor the
 * execution of threads and discover vulnerabilities in applications.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getStackWalkerWithClassReference</th>
 *   <td>Get a stack walker that can retrieve stack frames with class reference.</td>
 *   <td>This allows retrieval of Class objects from stack walking.
 *   This might allow malicious code to access Class objects on the stack
 *   outside its own context.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setDefaultUncaughtExceptionHandler</th>
 *   <td>Setting the default handler to be used when a thread
 *   terminates abruptly due to an uncaught exception</td>
 *   <td>This allows an attacker to register a malicious
 *   uncaught exception handler that could interfere with termination
 *   of a thread</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">preferences</th>
 *   <td>Represents the permission required to get access to the
 *   java.util.prefs.Preferences implementations user or system root
 *   which in turn allows retrieval or update operations within the
 *   Preferences persistent backing store.) </td>
 *   <td>This permission allows the user to read from or write to the
 *   preferences backing store if the user running the code has
 *   sufficient OS privileges to read/write to that backing store.
 *   The actual backing store may reside within a traditional filesystem
 *   directory or within a registry depending on the platform OS</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">manageProcess</th>
 *   <td>Native process termination and information about processes
 *       {@link ProcessHandle}.</td>
 *   <td>Allows code to identify and terminate processes that it did not create.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">localeServiceProvider</th>
 *   <td>This {@code RuntimePermission} is required to be granted to
 *   classes which subclass and implement
 *   {@code java.util.spi.LocaleServiceProvider}. The permission is
 *   checked during invocation of the abstract base class constructor.
 *   This permission ensures trust in classes which implement this
 *   security-sensitive provider mechanism. </td>
 *   <td>See <a href= "../util/spi/LocaleServiceProvider.html">
 *   {@code java.util.spi.LocaleServiceProvider}</a> for more
 *   information.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">loggerFinder</th>
 *   <td>This {@code RuntimePermission} is required to be granted to
 *   classes which subclass or call methods on
 *   {@code java.lang.System.LoggerFinder}. The permission is
 *   checked during invocation of the abstract base class constructor, as
 *   well as on the invocation of its public methods.
 *   This permission ensures trust in classes which provide loggers
 *   to system classes.</td>
 *   <td>See {@link java.lang.System.LoggerFinder java.lang.System.LoggerFinder}
 *   for more information.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">accessSystemModules</th>
 *   <td>Access system modules in the runtime image.</td>
 *   <td>This grants the permission to access resources in the
 *   {@linkplain ModuleFinder#ofSystem system modules} in the runtime image.</td>
 * </tr>
 *
 * </tbody>
 * </table>
 *
 * @implNote
 * Implementations may define additional target names, but should use naming
 * conventions such as reverse domain name notation to avoid name clashes.
 *
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 *
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 * @since 1.2
 */

public final class RuntimePermission extends BasicPermission {

    @java.io.Serial
    private static final long serialVersionUID = 7399184964622342223L;

    /**
     * Creates a new RuntimePermission with the specified name.
     * The name is the symbolic name of the RuntimePermission, such as
     * "exit", "setFactory", etc. An asterisk
     * may appear at the end of the name, following a ".", or by itself, to
     * signify a wildcard match.
     *
     * @param name the name of the RuntimePermission.
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */

    public RuntimePermission(String name)
    {
        super(name);
    }

    /**
     * Creates a new RuntimePermission object with the specified name.
     * The name is the symbolic name of the RuntimePermission, and the
     * actions String is currently unused and should be null.
     *
     * @param name the name of the RuntimePermission.
     * @param actions should be null.
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */

    public RuntimePermission(String name, String actions)
    {
        super(name, actions);
    }
}
