/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi;

import java.util.List;
import java.util.Map;

import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.spi.Connection;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MonitorContendedEnterRequest;
import com.sun.jdi.request.MonitorContendedEnteredRequest;
import com.sun.jdi.request.MonitorWaitRequest;
import com.sun.jdi.request.MonitorWaitedRequest;
import com.sun.jdi.request.VMDeathRequest;

/**
 * A virtual machine targeted for debugging.
 * More precisely, a {@link Mirror mirror} representing the
 * composite state of the target VM.
 * All other mirrors are associated with an instance of this
 * interface.  Access to all other mirrors is achieved
 * directly or indirectly through an instance of this
 * interface.
 * Access to global VM properties and control of VM execution
 * are supported directly by this interface.
 * <P>
 * Instances of this interface are created by instances of
 * {@link Connector}. For example,
 * an {@link AttachingConnector AttachingConnector}
 * attaches to a target VM and returns its virtual machine mirror.
 * A Connector will typically create a VirtualMachine by invoking
 * the VirtualMachineManager's {@link
 * VirtualMachineManager#createVirtualMachine(Connection)}
 * createVirtualMachine(Connection) method.
 * <p>
 * Note that a target VM launched by a launching connector is not
 * guaranteed to be stable until after the {@link VMStartEvent} has been
 * received.
 * <p>
 * Any method on <code>VirtualMachine</code> which
 * takes <code>VirtualMachine</code> as an parameter may throw
 * {@link VMDisconnectedException} if the target VM is
 * disconnected and the {@link VMDisconnectEvent} has been or is
 * available to be read from the {@link EventQueue}.
 * <p>
 * Any method on <code>VirtualMachine</code> which
 * takes <code>VirtualMachine</code> as an parameter may throw
 * {@link VMOutOfMemoryException} if the target VM has run out of memory.
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface VirtualMachine extends Mirror {

    /**
     * Returns all modules. For each module in the target
     * VM a {@link ModuleReference} will be placed in the returned list.
     * <P>
     *
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canGetModuleInfo()}
     * to determine if the operation is supported.
     *
     * @implSpec
     * The default implementation throws {@code UnsupportedOperationException}.
     *
     * @return a list of {@link ModuleReference} objects, each mirroring
     * a module in the target VM.
     *
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation.
     *
     * @since 9
     */
    default List<ModuleReference> allModules() {
        throw new java.lang.UnsupportedOperationException(
            "The method allModules() must be implemented");
    }

    /**
     * Returns the loaded reference types that
     * match a given name. The name must be fully qualified
     * (for example, java.lang.String). The returned list
     * will contain a {@link ReferenceType} for each class
     * or interface found with the given name. The search
     * is confined to loaded classes only; no attempt is made
     * to load a class of the given name.
     * <P>
     * The returned list will include reference types
     * loaded at least to the point of preparation and
     * types (like array) for which preparation is
     * not defined.
     *
     * @param className the class/interface name to search for
     * @return a list of {@link ReferenceType} objects, each
     * mirroring a type in the target VM with the given name.
     */
    List<ReferenceType> classesByName(String className);

    /**
     * Returns all {@linkplain ReferenceType loaded types} in the target VM.
     * <p>
     * The returned list includes all reference types, including
     * {@link Class#isHidden hidden classes or interfaces}, loaded
     * at least to the point of preparation and types (like array)
     * for which preparation is not defined.
     *
     * @return a list of {@link ReferenceType} objects, each mirroring
     * a loaded type in the target VM.
     * @see <a href="{@docRoot}/../specs/jvmti/jvmti.html#GetLoadedClasses">
     * JVM TI GetLoadedClasses</a> regarding how class and interface creation can be triggered
     */
    List<ReferenceType> allClasses();

    /**
     * All classes given are redefined according to the
     * definitions supplied.  A method in a redefined class
     * is called 'equivalent' (to the old version of the
     * method) if
     * <UL>
     * <LI>their bytecodes are the same except for indicies into
     *   the constant pool, and
     * <LI>the referenced constants are equal.
     * </UL>
     * Otherwise, the new method is called 'non-equivalent'.
     * If a redefined method has active stack frames, those active
     * frames continue to run the bytecodes of the previous version of the
     * method.  If the new version of such a method is non-equivalent,
     * then a method from one of these active frames is called 'obsolete' and
     * {@link Method#isObsolete Method.isObsolete()}
     * will return true when called on one of these methods.
     * If resetting such a frame is desired, use
     * {@link ThreadReference#popFrames ThreadReference.popFrames(StackFrame)}
     * to pop the old obsolete method execution from the stack.
     * New invocations of redefined methods will always invoke the new versions.
     * <p>
     * This function does not cause any initialization except
     * that which would occur under the customary JVM semantics.
     * In other words, redefining a class does not cause
     * its initializers to be run. The values of preexisting
     * static variables will remain as they were prior to the
     * call. However, completely uninitialized (new) static
     * variables will be assigned their default value.
     * <p>
     * If a redefined class has instances then all those
     * instances will have the fields defined by the redefined
     * class at the completion of the call. Preexisting fields
     * will retain their previous values. Any new fields will
     * have their default values; no instance initializers or
     * constructors are run.
     * <p>
     * Threads need not be suspended.
     * <p>
     * No events are generated by this function.
     * <p>
     * All breakpoints in the redefined classes are deleted.
     * <p>
     * Not all target virtual machines support this operation.
     * Use {@link #canRedefineClasses() canRedefineClasses()}
     * to determine if the operation is supported.
     * Use {@link #canAddMethod() canAddMethod()}
     * to determine if the redefinition can add methods.
     * Use {@link #canUnrestrictedlyRedefineClasses() canUnrestrictedlyRedefineClasses()}
     * to determine if the redefinition can change the schema,
     * delete methods, change the class hierarchy, etc.
     *
     * @param classToBytes A map from {@link ReferenceType}
     * to array of byte.
     * The bytes represent the new class definition and
     * are in Java Virtual Machine class file format.
     *
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation.
     * <UL>
     * <LI>If {@link #canRedefineClasses() canRedefineClasses()}
     * is false any call of this method will throw this exception.
     * <LI>If {@link #canAddMethod() canAddMethod()} is false
     * attempting to add a method will throw this exception.
     * <LI>If {@link #canUnrestrictedlyRedefineClasses()
     *            canUnrestrictedlyRedefineClasses()}
     * is false attempting any of the unsupported class file changes described
     * in <a href="{@docRoot}/../specs/jvmti.html#RedefineClasses">
     * JVM TI RedefineClasses</a> will throw this exception.
     * </UL>
     *
     * @throws java.lang.NoClassDefFoundError if the bytes
     * don't correspond to the reference type (the names
     * don't match).
     *
     * @throws java.lang.VerifyError if a "verifier" detects
     * that a class, though well formed, contains an internal
     * inconsistency or security problem.
     *
     * @throws java.lang.ClassFormatError if the bytes
     * do not represent a valid class.
     *
     * @throws java.lang.ClassCircularityError if a
     * circularity has been detected while initializing a class.
     *
     * @throws java.lang.UnsupportedClassVersionError if the
     * major and minor version numbers in bytes
     * are not supported by the VM.
     *
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     *
     * @see Method#isObsolete
     * @see ThreadReference#popFrames
     * @see #canRedefineClasses
     * @see #canAddMethod
     * @see #canUnrestrictedlyRedefineClasses
     *
     * @since 1.4
     */
    void redefineClasses(Map<? extends ReferenceType,byte[]> classToBytes);

    /**
     * Returns a list of the currently running threads. For each
     * running thread in the target VM, a {@link ThreadReference}
     * that mirrors it is placed in the list.
     * The returned list contains threads created through
     * java.lang.Thread, all native threads attached to
     * the target VM through JNI, and system threads created
     * by the target VM. Thread objects that have
     * not yet been started
     * (see {@link java.lang.Thread#start Thread.start()})
     * and thread objects that have
     * completed their execution are not included in the returned list.
     *
     * @return a list of {@link ThreadReference} objects, one for each
     * running thread in the mirrored VM.
     */
    List<ThreadReference> allThreads();

    /**
     * Suspends the execution of the application running in this
     * virtual machine. All threads currently running will be suspended.
     * <p>
     * Unlike {@link java.lang.Thread#suspend Thread.suspend()},
     * suspends of both the virtual machine and individual threads are
     * counted. Before a thread will run again, it must be resumed
     * (through {@link #resume} or {@link ThreadReference#resume})
     * the same number of times it has been suspended.
     *
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     */
    void suspend();

    /**
     * Continues the execution of the application running in this
     * virtual machine. All threads are resumed as documented in
     * {@link ThreadReference#resume}.
     *
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     *
     * @see #suspend
     */
    void resume();

    /**
     * Returns each thread group which does not have a parent. For each
     * top level thread group a {@link ThreadGroupReference} is placed in the
     * returned list.
     * <p>
     * This command may be used as the first step in building a tree
     * (or trees) of the existing thread groups.
     *
     * @return a list of {@link ThreadGroupReference} objects, one for each
     * top level thread group.
     */
    List<ThreadGroupReference> topLevelThreadGroups();

    /**
     * Returns the event queue for this virtual machine.
     * A virtual machine has only one {@link EventQueue} object, this
     * method will return the same instance each time it
     * is invoked.
     *
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     *
     * @return the {@link EventQueue} for this virtual machine.
     */
    EventQueue eventQueue();

    /**
     * Returns the event request manager for this virtual machine.
     * The {@link EventRequestManager} controls user settable events
     * such as breakpoints.
     * A virtual machine has only one {@link EventRequestManager} object,
     * this method will return the same instance each time it
     * is invoked.
     *
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     *
     * @return the {@link EventRequestManager} for this virtual machine.
     */
    EventRequestManager eventRequestManager();

    /**
     * Creates a {@link BooleanValue} for the given value. This value
     * can be used for setting and comparing against a value retrieved
     * from a variable or field in this virtual machine.
     *
     * @param value a boolean for which to create the value
     * @return the {@link BooleanValue} for the given boolean.
     */
    BooleanValue mirrorOf(boolean value);

    /**
     * Creates a {@link ByteValue} for the given value. This value
     * can be used for setting and comparing against a value retrieved
     * from a variable or field in this virtual machine.
     *
     * @param value a byte for which to create the value
     * @return the {@link ByteValue} for the given byte.
     */
    ByteValue mirrorOf(byte value);

    /**
     * Creates a {@link CharValue} for the given value. This value
     * can be used for setting and comparing against a value retrieved
     * from a variable or field in this virtual machine.
     *
     * @param value a char for which to create the value
     * @return the {@link CharValue} for the given char.
     */
    CharValue mirrorOf(char value);

    /**
     * Creates a {@link ShortValue} for the given value. This value
     * can be used for setting and comparing against a value retrieved
     * from a variable or field in this virtual machine.
     *
     * @param value a short for which to create the value
     * @return the {@link ShortValue} for the given short.
     */
    ShortValue mirrorOf(short value);

    /**
     * Creates an {@link IntegerValue} for the given value. This value
     * can be used for setting and comparing against a value retrieved
     * from a variable or field in this virtual machine.
     *
     * @param value an int for which to create the value
     * @return the {@link IntegerValue} for the given int.
     */
    IntegerValue mirrorOf(int value);

    /**
     * Creates a {@link LongValue} for the given value. This value
     * can be used for setting and comparing against a value retrieved
     * from a variable or field in this virtual machine.
     *
     * @param value a long for which to create the value
     * @return the {@link LongValue} for the given long.
     */
    LongValue mirrorOf(long value);

    /**
     * Creates a {@link FloatValue} for the given value. This value
     * can be used for setting and comparing against a value retrieved
     * from a variable or field in this virtual machine.
     *
     * @param value a float for which to create the value
     * @return the {@link FloatValue} for the given float.
     */
    FloatValue mirrorOf(float value);

    /**
     * Creates a {@link DoubleValue} for the given value. This value
     * can be used for setting and comparing against a value retrieved
     * from a variable or field in this virtual machine.
     *
     * @param value a double for which to create the value
     * @return the {@link DoubleValue} for the given double.
     */
    DoubleValue mirrorOf(double value);

    /**
     * Creates a string in this virtual machine.
     * The created string can be used for setting and comparing against
     * a string value retrieved from a variable or field in this
     * virtual machine.
     *
     * @param value the string to be created
     * @return a {@link StringReference} that mirrors the newly created
     * string in the target VM.
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only
     * -see {@link VirtualMachine#canBeModified()}.
     */
    StringReference mirrorOf(String value);


    /**
     * Creates a {@link VoidValue}.  This value
     * can be passed to {@link ThreadReference#forceEarlyReturn}
     * when a void method is to be exited.
     *
     * @return the {@link VoidValue}.
     */
    VoidValue mirrorOfVoid();

    /**
     * Returns the {@link java.lang.Process} object for this
     * virtual machine if launched by a {@link LaunchingConnector}
     *
     * @return the {@link java.lang.Process} object for this virtual
     * machine, or null if it was not launched by a {@link LaunchingConnector}.
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only
     * -see {@link VirtualMachine#canBeModified()}.
     */
    Process process();

    /**
     * Invalidates this virtual machine mirror.
     * The communication channel to the target VM is closed, and
     * the target VM prepares to accept another subsequent connection
     * from this debugger or another debugger, including the
     * following tasks:
     * <ul>
     * <li>All event requests are cancelled.
     * <li>All threads suspended by {@link #suspend} or by
     * {@link ThreadReference#suspend} are resumed as many
     * times as necessary for them to run.
     * <li>Garbage collection is re-enabled in all cases where it was
     * disabled through {@link ObjectReference#disableCollection}.
     * </ul>
     * Any current method invocations executing in the target VM
     * are continued after the disconnection. Upon completion of any such
     * method invocation, the invoking thread continues from the
     * location where it was originally stopped.
     * <p>
     * Resources originating in
     * this VirtualMachine (ObjectReferences, ReferenceTypes, etc.)
     * will become invalid.
     */
    void dispose();

    /**
     * Causes the mirrored VM to terminate with the given error code.
     * All resources associated with this VirtualMachine are freed.
     * If the mirrored VM is remote, the communication channel
     * to it will be closed. Resources originating in
     * this VirtualMachine (ObjectReferences, ReferenceTypes, etc.)
     * will become invalid.
     * <p>
     * Threads running in the mirrored VM are abruptly terminated.
     * A thread death exception is not thrown and
     * finally blocks are not run.
     *
     * @param exitCode the exit code for the target VM.  On some platforms,
     * the exit code might be truncated, for example, to the lower order 8 bits.
     *
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     */
    void exit(int exitCode);

    /**
     * Determines if the target VM supports watchpoints
     * for field modification.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     */
    boolean canWatchFieldModification();

    /**
     * Determines if the target VM supports watchpoints
     * for field access.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     */
    boolean canWatchFieldAccess();

    /**
     * Determines if the target VM supports the retrieval
     * of a method's bytecodes.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     */
    boolean canGetBytecodes();

    /**
     * Determines if the target VM supports the query
     * of the synthetic attribute of a method or field.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     */
    boolean canGetSyntheticAttribute();

    /**
     * Determines if the target VM supports the retrieval
     * of the monitors owned by a thread.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     */
    boolean canGetOwnedMonitorInfo();

    /**
     * Determines if the target VM supports the retrieval
     * of the monitor for which a thread is currently waiting.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     */
    boolean canGetCurrentContendedMonitor();

    /**
     * Determines if the target VM supports the retrieval
     * of the monitor information for an object.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     */
    boolean canGetMonitorInfo();

    /**
     * Determines if the target VM supports filtering
     * events by specific instance object.  For example,
     * see {@link BreakpointRequest#addInstanceFilter}.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     */
    boolean canUseInstanceFilters();

    /**
     * Determines if the target VM supports any level
     * of class redefinition.
     * @see #redefineClasses
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.4
     */
    boolean canRedefineClasses();

    /**
     * Determines if the target VM supports the addition
     * of methods when performing class redefinition.
     * @see #redefineClasses
     * @deprecated A JVM TI based JDWP back-end will never set this capability to true.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.4
     */
    @Deprecated(since="15")
    boolean canAddMethod();

    /**
     * Determines if the target VM supports
     * changes when performing class redefinition that are
     * otherwise restricted by {@link #redefineClasses}.
     * @see #redefineClasses
     * @deprecated A JVM TI based JDWP back-end will never set this capability to true.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.4
     */
    @Deprecated(since="15")
    boolean canUnrestrictedlyRedefineClasses();

    /**
     * Determines if the target VM supports popping
     * frames of a threads stack.
     * @see ThreadReference#popFrames
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.4
     */
    boolean canPopFrames();

    /**
     * Determines if the target VM supports getting
     * the source debug extension.
     * @see ReferenceType#sourceDebugExtension
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.4
     */
    boolean canGetSourceDebugExtension();

    /**
     * Determines if the target VM supports the creation of
     * {@link VMDeathRequest}s.
     * @see EventRequestManager#createVMDeathRequest
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.4
     */
    boolean canRequestVMDeathEvent();

    /**
     * Determines if the target VM supports the inclusion of return values
     * in
     * {@link MethodExitEvent}s.
     * @see EventRequestManager#createMethodExitRequest
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.6
     */
    boolean canGetMethodReturnValues();

    /**
     * Determines if the target VM supports the accessing of class instances,
     * instance counts, and referring objects.
     *
     * @see #instanceCounts
     * @see ReferenceType#instances(long)
     * @see ObjectReference#referringObjects(long)
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.6
     */
    boolean canGetInstanceInfo();

    /**
     * Determines if the target VM supports the filtering of
     * class prepare events by source name.
     *
     * see {@link ClassPrepareRequest#addSourceNameFilter}.
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.6
     */
    boolean canUseSourceNameFilters();

    /**
     * Determines if the target VM supports the forcing of a method to
     * return early.
     *
     * @see ThreadReference#forceEarlyReturn(Value)
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.6
     */
    boolean canForceEarlyReturn();

    /**
     * Determines if the target VM is a read-only VM.  If a method which
     * would modify the state of the VM is called on a read-only VM,
     * then {@link VMCannotBeModifiedException} is thrown.
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.5
     */

    boolean canBeModified();

    /**
     * Determines if the target VM supports the creation of
     * {@link MonitorContendedEnterRequest}s.
     * {@link MonitorContendedEnteredRequest}s.
     * {@link MonitorWaitRequest}s.
     * {@link MonitorWaitedRequest}s.
     * @see EventRequestManager#createMonitorContendedEnterRequest
     * @see EventRequestManager#createMonitorContendedEnteredRequest
     * @see EventRequestManager#createMonitorWaitRequest
     * @see EventRequestManager#createMonitorWaitedRequest
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.6
     */

    boolean canRequestMonitorEvents();

    /**
     * Determines if the target VM supports getting which
     * frame has acquired a monitor.
     * @see ThreadReference#ownedMonitorsAndFrames
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.6
     */

     boolean canGetMonitorFrameInfo();


    /**
     * Determines if the target VM supports reading class file
     * major and minor versions.
     *
     * @see ReferenceType#majorVersion()
     * @see ReferenceType#minorVersion()
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.6
     */
    boolean canGetClassFileVersion();

    /**
     * Determines if the target VM supports getting constant pool
     * information of a class.
     *
     * @see ReferenceType#constantPoolCount()
     * @see ReferenceType#constantPool()
     *
     * @return <code>true</code> if the feature is supported,
     * <code>false</code> otherwise.
     *
     * @since 1.6
     */
    boolean canGetConstantPool();

    /**
     * Determines if the target VM supports getting information about modules.
     *
     * @return {@code true} if the feature is supported, {@code false} otherwise
     *
     * @implSpec
     * The default implementation returns {@code false}.
     *
     * @see VirtualMachine#allModules()
     * @see ReferenceType#module()
     * @see ModuleReference
     *
     * @since 9
     */
    default boolean canGetModuleInfo() {
        return false;
    }

    /**
     * Set this VM's default stratum (see {@link Location} for a
     * discussion of strata).  Overrides the per-class default set
     * in the class file.
     * <P>
     * Affects location queries (such as,
     * {@link Location#sourceName()})
     * and the line boundaries used in
     * single stepping.
     *
     * @param stratum the stratum to set as VM default,
     * or null to use per-class defaults.
     *
     * @throws java.lang.UnsupportedOperationException if the
     * target virtual machine does not support this operation.
     *
     * @since 1.4
     */
    void setDefaultStratum(String stratum);

    /**
     * Return this VM's default stratum.
     *
     * @see #setDefaultStratum(String)
     * @see ReferenceType#defaultStratum()
     * @return <code>null</code> (meaning that the per-class
     * default - {@link ReferenceType#defaultStratum()} -
     * should be used) unless the default stratum has been
     * set with
     * {@link #setDefaultStratum(String)}.
     *
     * @since 1.4
     */
    String getDefaultStratum();

    /**
     * Returns the number of instances of each ReferenceType in the 'refTypes'
     * list.
     * Only instances that are reachable for the purposes of garbage collection
     * are counted.
     * <p>
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canGetInstanceInfo()}
     * to determine if the operation is supported.
     *
     * @see ReferenceType#instances(long)
     * @see ObjectReference#referringObjects(long)
     * @param refTypes the list of {@link ReferenceType} objects for which counts
     *        are to be obtained.
     *
     * @return an array of <code>long</code> containing one element for each
     *         element in the 'refTypes' list.  Element i of the array contains
     *         the number of instances in the target VM of the ReferenceType at
     *         position i in the 'refTypes' list.
     *         If the 'refTypes' list is empty, a zero-length array is returned.
     *         If a ReferenceType in refTypes has been garbage collected, zero
     *         is returned for its instance count.
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation - see
     * {@link VirtualMachine#canGetInstanceInfo() canGetInstanceInfo()}
     * @throws NullPointerException if the 'refTypes' list is null.
     * @since 1.6
     */
    long[] instanceCounts(List<? extends ReferenceType> refTypes);

    /**
     * Returns text information on the target VM and the
     * debugger support that mirrors it. No specific format
     * for this information is guaranteed.
     * Typically, this string contains version information for the
     * target VM and debugger interfaces.
     * More precise information
     * on VM and JDI versions is available through
     * {@link #version}, {@link VirtualMachineManager#majorInterfaceVersion},
     * and {@link VirtualMachineManager#minorInterfaceVersion}
     *
     * @return the description.
     */
    String description();

    /**
     * Returns the version of the Java Runtime Environment in the target
     * VM as reported by the property <code>java.version</code>.
     * For obtaining the JDI interface version, use
     * {@link VirtualMachineManager#majorInterfaceVersion}
     * and {@link VirtualMachineManager#minorInterfaceVersion}
     *
     * @return the target VM version.
     */
    String version();

    /**
     * Returns the name of the target VM as reported by the
     * property <code>java.vm.name</code>.
     *
     * @return the target VM name.
     */
    String name();

    /** All tracing is disabled. */
    int TRACE_NONE        = 0x00000000;
    /** Tracing enabled for JDWP packets sent to target VM. */
    int TRACE_SENDS       = 0x00000001;
    /** Tracing enabled for JDWP packets received from target VM. */
    int TRACE_RECEIVES    = 0x00000002;
    /** Tracing enabled for internal event handling. */
    int TRACE_EVENTS      = 0x00000004;
    /** Tracing enabled for internal managment of reference types. */
    int TRACE_REFTYPES    = 0x00000008;
    /** Tracing enabled for internal management of object references. */
    int TRACE_OBJREFS      = 0x00000010;
    /** All tracing is enabled. */
    int TRACE_ALL         = 0x00ffffff;

    /**
     * Traces the activities performed by the com.sun.jdi implementation.
     * All trace information is output to System.err. The given trace
     * flags are used to limit the output to only the information
     * desired. The given flags are in effect and the corresponding
     * trace will continue until the next call to
     * this method.
     * <p>
     * Output is implementation dependent and trace mode may be ignored.
     *
     * @param traceFlags identifies which kinds of tracing to enable.
     */
    void setDebugTraceMode(int traceFlags);
}
