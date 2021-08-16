/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.instrument;

import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

/*
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 * This class provides services needed to instrument Java
 * programming language code.
 * Instrumentation is the addition of byte-codes to methods for the
 * purpose of gathering data to be utilized by tools.
 * Since the changes are purely additive, these tools do not modify
 * application state or behavior.
 * Examples of such benign tools include monitoring agents, profilers,
 * coverage analyzers, and event loggers.
 *
 * <P>
 * There are two ways to obtain an instance of the
 * <code>Instrumentation</code> interface:
 *
 * <ol>
 *   <li><p> When a JVM is launched in a way that indicates an agent
 *     class. In that case an <code>Instrumentation</code> instance
 *     is passed to the <code>premain</code> method of the agent class.
 *     </p></li>
 *   <li><p> When a JVM provides a mechanism to start agents sometime
 *     after the JVM is launched. In that case an <code>Instrumentation</code>
 *     instance is passed to the <code>agentmain</code> method of the
 *     agent code. </p> </li>
 * </ol>
 * <p>
 * These mechanisms are described in the
 * {@linkplain java.lang.instrument package specification}.
 * <p>
 * Once an agent acquires an <code>Instrumentation</code> instance,
 * the agent may call methods on the instance at any time.
 *
 * @apiNote This interface is not intended to be implemented outside of
 * the java.instrument module.
 *
 * @since   1.5
 */
public interface Instrumentation {
    /**
     * Registers the supplied transformer. All future class definitions
     * will be seen by the transformer, except definitions of classes upon which any
     * registered transformer is dependent.
     * The transformer is called when classes are loaded, when they are
     * {@linkplain #redefineClasses redefined}. and if <code>canRetransform</code> is true,
     * when they are {@linkplain #retransformClasses retransformed}.
     * {@link ClassFileTransformer} defines the order of transform calls.
     *
     * If a transformer throws
     * an exception during execution, the JVM will still call the other registered
     * transformers in order. The same transformer may be added more than once,
     * but it is strongly discouraged -- avoid this by creating a new instance of
     * transformer class.
     * <P>
     * This method is intended for use in instrumentation, as described in the
     * {@linkplain Instrumentation class specification}.
     *
     * @param transformer          the transformer to register
     * @param canRetransform       can this transformer's transformations be retransformed
     * @throws java.lang.NullPointerException if passed a <code>null</code> transformer
     * @throws java.lang.UnsupportedOperationException if <code>canRetransform</code>
     * is true and the current configuration of the JVM does not allow
     * retransformation ({@link #isRetransformClassesSupported} is false)
     * @since 1.6
     */
    void
    addTransformer(ClassFileTransformer transformer, boolean canRetransform);

    /**
     * Registers the supplied transformer.
     * <P>
     * Same as <code>addTransformer(transformer, false)</code>.
     *
     * @param transformer          the transformer to register
     * @throws java.lang.NullPointerException if passed a <code>null</code> transformer
     * @see    #addTransformer(ClassFileTransformer,boolean)
     */
    void
    addTransformer(ClassFileTransformer transformer);

    /**
     * Unregisters the supplied transformer. Future class definitions will
     * not be shown to the transformer. Removes the most-recently-added matching
     * instance of the transformer. Due to the multi-threaded nature of
     * class loading, it is possible for a transformer to receive calls
     * after it has been removed. Transformers should be written defensively
     * to expect this situation.
     *
     * @param transformer          the transformer to unregister
     * @return  true if the transformer was found and removed, false if the
     *           transformer was not found
     * @throws java.lang.NullPointerException if passed a <code>null</code> transformer
     */
    boolean
    removeTransformer(ClassFileTransformer transformer);

    /**
     * Returns whether or not the current JVM configuration supports retransformation
     * of classes.
     * The ability to retransform an already loaded class is an optional capability
     * of a JVM.
     * Retransformation will only be supported if the
     * <code>Can-Retransform-Classes</code> manifest attribute is set to
     * <code>true</code> in the agent JAR file (as described in the
     * {@linkplain java.lang.instrument package specification}) and the JVM supports
     * this capability.
     * During a single instantiation of a single JVM, multiple calls to this
     * method will always return the same answer.
     * @return  true if the current JVM configuration supports retransformation of
     *          classes, false if not.
     * @see #retransformClasses
     * @since 1.6
     */
    boolean
    isRetransformClassesSupported();

    /**
     * Retransform the supplied set of classes.
     *
     * <P>
     * This function facilitates the instrumentation
     * of already loaded classes.
     * When classes are initially loaded or when they are
     * {@linkplain #redefineClasses redefined},
     * the initial class file bytes can be transformed with the
     * {@link java.lang.instrument.ClassFileTransformer ClassFileTransformer}.
     * This function reruns the transformation process
     * (whether or not a transformation has previously occurred).
     * This retransformation follows these steps:
     *  <ul>
     *    <li>starting from the initial class file bytes
     *    </li>
     *    <li>for each transformer that was added with <code>canRetransform</code>
     *      false, the bytes returned by
     *      {@link ClassFileTransformer#transform(Module,ClassLoader,String,Class,ProtectionDomain,byte[])
     *      transform} during the last class load or redefine are
     *      reused as the output of the transformation; note that this is
     *      equivalent to reapplying the previous transformation, unaltered;
     *      except that {@code transform} method is not called.
     *    </li>
     *    <li>for each transformer that was added with <code>canRetransform</code>
     *      true, the
     *      {@link ClassFileTransformer#transform(Module,ClassLoader,String,Class,ProtectionDomain,byte[])
     *      transform} method is called in these transformers
     *    </li>
     *    <li>the transformed class file bytes are installed as the new
     *      definition of the class
     *    </li>
     *  </ul>
     * <P>
     *
     * The order of transformation is described in {@link ClassFileTransformer}.
     * This same order is used in the automatic reapplication of
     * retransformation incapable transforms.
     * <P>
     *
     * The initial class file bytes represent the bytes passed to
     * {@link java.lang.ClassLoader#defineClass ClassLoader.defineClass} or
     * {@link #redefineClasses redefineClasses}
     * (before any transformations
     *  were applied), however they might not exactly match them.
     *  The constant pool might not have the same layout or contents.
     *  The constant pool may have more or fewer entries.
     *  Constant pool entries may be in a different order; however,
     *  constant pool indices in the bytecodes of methods will correspond.
     *  Some attributes may not be present.
     *  Where order is not meaningful, for example the order of methods,
     *  order might not be preserved.
     *
     * <P>
     * This method operates on
     * a set in order to allow interdependent changes to more than one class at the same time
     * (a retransformation of class A can require a retransformation of class B).
     *
     * <P>
     * If a retransformed method has active stack frames, those active frames continue to
     * run the bytecodes of the original method.
     * The retransformed method will be used on new invokes.
     *
     * <P>
     * This method does not cause any initialization except that which would occur
     * under the customary JVM semantics. In other words, redefining a class
     * does not cause its initializers to be run. The values of static variables
     * will remain as they were prior to the call.
     *
     * <P>
     * Instances of the retransformed class are not affected.
     *
     * <P>
     * The supported class file changes are described in
     * <a href="{@docRoot}/../specs/jvmti.html#RetransformClasses">JVM TI RetransformClasses</a>.
     * The class file bytes are not checked, verified and installed
     * until after the transformations have been applied, if the resultant bytes are in
     * error this method will throw an exception.
     *
     * <P>
     * If this method throws an exception, no classes have been retransformed.
     * <P>
     * This method is intended for use in instrumentation, as described in the
     * {@linkplain Instrumentation class specification}.
     *
     * @param classes array of classes to retransform;
     *                a zero-length array is allowed, in this case, this method does nothing
     * @throws java.lang.instrument.UnmodifiableClassException if a specified class cannot be modified
     * ({@link #isModifiableClass} would return <code>false</code>)
     * @throws java.lang.UnsupportedOperationException if the current configuration of the JVM does not allow
     * retransformation ({@link #isRetransformClassesSupported} is false) or the retransformation attempted
     * to make unsupported changes
     * @throws java.lang.ClassFormatError if the data did not contain a valid class
     * @throws java.lang.NoClassDefFoundError if the name in the class file is not equal to the name of the class
     * @throws java.lang.UnsupportedClassVersionError if the class file version numbers are not supported
     * @throws java.lang.ClassCircularityError if the new classes contain a circularity
     * @throws java.lang.LinkageError if a linkage error occurs
     * @throws java.lang.NullPointerException if the supplied classes  array or any of its components
     *                                        is <code>null</code>.
     *
     * @see #isRetransformClassesSupported
     * @see #addTransformer
     * @see java.lang.instrument.ClassFileTransformer
     * @since 1.6
     */
    void
    retransformClasses(Class<?>... classes) throws UnmodifiableClassException;

    /**
     * Returns whether or not the current JVM configuration supports redefinition
     * of classes.
     * The ability to redefine an already loaded class is an optional capability
     * of a JVM.
     * Redefinition will only be supported if the
     * <code>Can-Redefine-Classes</code> manifest attribute is set to
     * <code>true</code> in the agent JAR file (as described in the
     * {@linkplain java.lang.instrument package specification}) and the JVM supports
     * this capability.
     * During a single instantiation of a single JVM, multiple calls to this
     * method will always return the same answer.
     * @return  true if the current JVM configuration supports redefinition of classes,
     * false if not.
     * @see #redefineClasses
     */
    boolean
    isRedefineClassesSupported();

    /**
     * Redefine the supplied set of classes using the supplied class files.
     *
     * <P>
     * This method is used to replace the definition of a class without reference
     * to the existing class file bytes, as one might do when recompiling from source
     * for fix-and-continue debugging.
     * Where the existing class file bytes are to be transformed (for
     * example in bytecode instrumentation)
     * {@link #retransformClasses retransformClasses}
     * should be used.
     *
     * <P>
     * This method operates on
     * a set in order to allow interdependent changes to more than one class at the same time
     * (a redefinition of class A can require a redefinition of class B).
     *
     * <P>
     * If a redefined method has active stack frames, those active frames continue to
     * run the bytecodes of the original method.
     * The redefined method will be used on new invokes.
     *
     * <P>
     * This method does not cause any initialization except that which would occur
     * under the customary JVM semantics. In other words, redefining a class
     * does not cause its initializers to be run. The values of static variables
     * will remain as they were prior to the call.
     *
     * <P>
     * Instances of the redefined class are not affected.
     *
     * <P>
     * The supported class file changes are described in
     * <a href="{@docRoot}/../specs/jvmti.html#RedefineClasses">JVM TI RedefineClasses</a>.
     * The class file bytes are not checked, verified and installed
     * until after the transformations have been applied, if the resultant bytes are in
     * error this method will throw an exception.
     *
     * <P>
     * If this method throws an exception, no classes have been redefined.
     * <P>
     * This method is intended for use in instrumentation, as described in the
     * {@linkplain Instrumentation class specification}.
     *
     * @param definitions array of classes to redefine with corresponding definitions;
     *                    a zero-length array is allowed, in this case, this method does nothing
     * @throws java.lang.instrument.UnmodifiableClassException if a specified class cannot be modified
     * ({@link #isModifiableClass} would return <code>false</code>)
     * @throws java.lang.UnsupportedOperationException if the current configuration of the JVM does not allow
     * redefinition ({@link #isRedefineClassesSupported} is false) or the redefinition attempted
     * to make unsupported changes
     * @throws java.lang.ClassFormatError if the data did not contain a valid class
     * @throws java.lang.NoClassDefFoundError if the name in the class file is not equal to the name of the class
     * @throws java.lang.UnsupportedClassVersionError if the class file version numbers are not supported
     * @throws java.lang.ClassCircularityError if the new classes contain a circularity
     * @throws java.lang.LinkageError if a linkage error occurs
     * @throws java.lang.NullPointerException if the supplied definitions array or any of its components
     * is <code>null</code>
     * @throws java.lang.ClassNotFoundException Can never be thrown (present for compatibility reasons only)
     *
     * @see #isRedefineClassesSupported
     * @see #addTransformer
     * @see java.lang.instrument.ClassFileTransformer
     */
    void
    redefineClasses(ClassDefinition... definitions)
        throws  ClassNotFoundException, UnmodifiableClassException;


    /**
     * Tests whether a class is modifiable by
     * {@linkplain #retransformClasses retransformation}
     * or {@linkplain #redefineClasses redefinition}.
     * If a class is modifiable then this method returns <code>true</code>.
     * If a class is not modifiable then this method returns <code>false</code>.
     * <P>
     * For a class to be retransformed, {@link #isRetransformClassesSupported} must also be true.
     * But the value of <code>isRetransformClassesSupported()</code> does not influence the value
     * returned by this function.
     * For a class to be redefined, {@link #isRedefineClassesSupported} must also be true.
     * But the value of <code>isRedefineClassesSupported()</code> does not influence the value
     * returned by this function.
     * <P>
     * Primitive classes (for example, <code>java.lang.Integer.TYPE</code>)
     * and array classes are never modifiable.
     *
     * @param theClass the class to check for being modifiable
     * @return whether or not the argument class is modifiable
     * @throws java.lang.NullPointerException if the specified class is <code>null</code>.
     *
     * @see #retransformClasses
     * @see #isRetransformClassesSupported
     * @see #redefineClasses
     * @see #isRedefineClassesSupported
     * @since 1.6
     */
    boolean
    isModifiableClass(Class<?> theClass);

    /**
     * Returns an array of all classes currently loaded by the JVM.
     * The returned array includes all classes and interfaces, including
     * {@linkplain Class#isHidden hidden classes or interfaces}, and array classes
     * of all types.
     *
     * @return an array containing all the classes loaded by the JVM, zero-length if there are none
     */
    @SuppressWarnings("rawtypes")
    Class[]
    getAllLoadedClasses();

    /**
     * Returns an array of all classes which {@code loader} can find by name
     * via {@link ClassLoader#loadClass(String, boolean) ClassLoader::loadClass},
     * {@link Class#forName(String) Class::forName} and bytecode linkage.
     * That is, all classes for which {@code loader} has been recorded as
     * an initiating loader. If the supplied {@code loader} is {@code null},
     * classes that the bootstrap class loader can find by name are returned.
     * <p>
     * The returned array does not include {@linkplain Class#isHidden()
     * hidden classes or interfaces} or array classes whose
     * {@linkplain Class#componentType() element type} is a
     * {@linkplain Class#isHidden() hidden class or interface}.
     * as they cannot be discovered by any class loader.
     *
     * @param loader          the loader whose initiated class list will be returned
     * @return an array containing all classes which {@code loader} can find by name;
     *          zero-length if there are none
     */
    @SuppressWarnings("rawtypes")
    Class[]
    getInitiatedClasses(ClassLoader loader);

    /**
     * Returns an implementation-specific approximation of the amount of storage consumed by
     * the specified object. The result may include some or all of the object's overhead,
     * and thus is useful for comparison within an implementation but not between implementations.
     *
     * The estimate may change during a single invocation of the JVM.
     *
     * @param objectToSize     the object to size
     * @return an implementation-specific approximation of the amount of storage consumed by the specified object
     * @throws java.lang.NullPointerException if the supplied Object is <code>null</code>.
     */
    long
    getObjectSize(Object objectToSize);


    /**
     * Specifies a JAR file with instrumentation classes to be defined by the
     * bootstrap class loader.
     *
     * <p> When the virtual machine's built-in class loader, known as the "bootstrap
     * class loader", unsuccessfully searches for a class, the entries in the {@link
     * java.util.jar.JarFile JAR file} will be searched as well.
     *
     * <p> This method may be used multiple times to add multiple JAR files to be
     * searched in the order that this method was invoked.
     *
     * <p> The agent should take care to ensure that the JAR does not contain any
     * classes or resources other than those to be defined by the bootstrap
     * class loader for the purpose of instrumentation.
     * Failure to observe this warning could result in unexpected
     * behavior that is difficult to diagnose. For example, suppose there is a
     * loader L, and L's parent for delegation is the bootstrap class loader.
     * Furthermore, a method in class C, a class defined by L, makes reference to
     * a non-public accessor class C$1. If the JAR file contains a class C$1 then
     * the delegation to the bootstrap class loader will cause C$1 to be defined
     * by the bootstrap class loader. In this example an <code>IllegalAccessError</code>
     * will be thrown that may cause the application to fail. One approach to
     * avoiding these types of issues, is to use a unique package name for the
     * instrumentation classes.
     *
     * <p>
     * <cite>The Java Virtual Machine Specification</cite>
     * specifies that a subsequent attempt to resolve a symbolic
     * reference that the Java virtual machine has previously unsuccessfully attempted
     * to resolve always fails with the same error that was thrown as a result of the
     * initial resolution attempt. Consequently, if the JAR file contains an entry
     * that corresponds to a class for which the Java virtual machine has
     * unsuccessfully attempted to resolve a reference, then subsequent attempts to
     * resolve that reference will fail with the same error as the initial attempt.
     *
     * @param   jarfile
     *          The JAR file to be searched when the bootstrap class loader
     *          unsuccessfully searches for a class.
     *
     * @throws  NullPointerException
     *          If <code>jarfile</code> is <code>null</code>.
     *
     * @see     #appendToSystemClassLoaderSearch
     * @see     java.lang.ClassLoader
     * @see     java.util.jar.JarFile
     *
     * @since 1.6
     */
    void
    appendToBootstrapClassLoaderSearch(JarFile jarfile);

    /**
     * Specifies a JAR file with instrumentation classes to be defined by the
     * system class loader.
     *
     * When the system class loader for delegation (see
     * {@link java.lang.ClassLoader#getSystemClassLoader getSystemClassLoader()})
     * unsuccessfully searches for a class, the entries in the {@link
     * java.util.jar.JarFile JarFile} will be searched as well.
     *
     * <p> This method may be used multiple times to add multiple JAR files to be
     * searched in the order that this method was invoked.
     *
     * <p> The agent should take care to ensure that the JAR does not contain any
     * classes or resources other than those to be defined by the system class
     * loader for the purpose of instrumentation.
     * Failure to observe this warning could result in unexpected
     * behavior that is difficult to diagnose (see
     * {@link #appendToBootstrapClassLoaderSearch
     * appendToBootstrapClassLoaderSearch}).
     *
     * <p> The system class loader supports adding a JAR file to be searched if
     * it implements a method named <code>appendToClassPathForInstrumentation</code>
     * which takes a single parameter of type <code>java.lang.String</code>. The
     * method is not required to have <code>public</code> access. The name of
     * the JAR file is obtained by invoking the {@link java.util.zip.ZipFile#getName
     * getName()} method on the <code>jarfile</code> and this is provided as the
     * parameter to the <code>appendToClassPathForInstrumentation</code> method.
     *
     * <p>
     * <cite>The Java Virtual Machine Specification</cite>
     * specifies that a subsequent attempt to resolve a symbolic
     * reference that the Java virtual machine has previously unsuccessfully attempted
     * to resolve always fails with the same error that was thrown as a result of the
     * initial resolution attempt. Consequently, if the JAR file contains an entry
     * that corresponds to a class for which the Java virtual machine has
     * unsuccessfully attempted to resolve a reference, then subsequent attempts to
     * resolve that reference will fail with the same error as the initial attempt.
     *
     * <p> This method does not change the value of <code>java.class.path</code>
     * {@link java.lang.System#getProperties system property}.
     *
     * @param   jarfile
     *          The JAR file to be searched when the system class loader
     *          unsuccessfully searches for a class.
     *
     * @throws  UnsupportedOperationException
     *          If the system class loader does not support appending a
     *          a JAR file to be searched.
     *
     * @throws  NullPointerException
     *          If <code>jarfile</code> is <code>null</code>.
     *
     * @see     #appendToBootstrapClassLoaderSearch
     * @see     java.lang.ClassLoader#getSystemClassLoader
     * @see     java.util.jar.JarFile
     * @since 1.6
     */
    void
    appendToSystemClassLoaderSearch(JarFile jarfile);

    /**
     * Returns whether the current JVM configuration supports
     * {@linkplain #setNativeMethodPrefix(ClassFileTransformer,String)
     * setting a native method prefix}.
     * The ability to set a native method prefix is an optional
     * capability of a JVM.
     * Setting a native method prefix will only be supported if the
     * <code>Can-Set-Native-Method-Prefix</code> manifest attribute is set to
     * <code>true</code> in the agent JAR file (as described in the
     * {@linkplain java.lang.instrument package specification}) and the JVM supports
     * this capability.
     * During a single instantiation of a single JVM, multiple
     * calls to this method will always return the same answer.
     * @return  true if the current JVM configuration supports
     * setting a native method prefix, false if not.
     * @see #setNativeMethodPrefix
     * @since 1.6
     */
    boolean
    isNativeMethodPrefixSupported();

    /**
     * This method modifies the failure handling of
     * native method resolution by allowing retry
     * with a prefix applied to the name.
     * When used with the
     * {@link java.lang.instrument.ClassFileTransformer ClassFileTransformer},
     * it enables native methods to be
     * instrumented.
     * <p>
     * Since native methods cannot be directly instrumented
     * (they have no bytecodes), they must be wrapped with
     * a non-native method which can be instrumented.
     * For example, if we had:
     * <pre>
     *   native boolean foo(int x);</pre>
     * <p>
     * We could transform the class file (with the
     * ClassFileTransformer during the initial definition
     * of the class) so that this becomes:
     * <pre>
     *   boolean foo(int x) {
     *     <i>... record entry to foo ...</i>
     *     return wrapped_foo(x);
     *   }
     *
     *   native boolean wrapped_foo(int x);</pre>
     * <p>
     * Where <code>foo</code> becomes a wrapper for the actual native
     * method with the appended prefix "wrapped_".  Note that
     * "wrapped_" would be a poor choice of prefix since it
     * might conceivably form the name of an existing method
     * thus something like "$$$MyAgentWrapped$$$_" would be
     * better but would make these examples less readable.
     * <p>
     * The wrapper will allow data to be collected on the native
     * method call, but now the problem becomes linking up the
     * wrapped method with the native implementation.
     * That is, the method <code>wrapped_foo</code> needs to be
     * resolved to the native implementation of <code>foo</code>,
     * which might be:
     * <pre>
     *   Java_somePackage_someClass_foo(JNIEnv* env, jint x)</pre>
     * <p>
     * This function allows the prefix to be specified and the
     * proper resolution to occur.
     * Specifically, when the standard resolution fails, the
     * resolution is retried taking the prefix into consideration.
     * There are two ways that resolution occurs, explicit
     * resolution with the JNI function <code>RegisterNatives</code>
     * and the normal automatic resolution.  For
     * <code>RegisterNatives</code>, the JVM will attempt this
     * association:
     * <pre>{@code
     *   method(foo) -> nativeImplementation(foo)
     * }</pre>
     * <p>
     * When this fails, the resolution will be retried with
     * the specified prefix prepended to the method name,
     * yielding the correct resolution:
     * <pre>{@code
     *   method(wrapped_foo) -> nativeImplementation(foo)
     * }</pre>
     * <p>
     * For automatic resolution, the JVM will attempt:
     * <pre>{@code
     *   method(wrapped_foo) -> nativeImplementation(wrapped_foo)
     * }</pre>
     * <p>
     * When this fails, the resolution will be retried with
     * the specified prefix deleted from the implementation name,
     * yielding the correct resolution:
     * <pre>{@code
     *   method(wrapped_foo) -> nativeImplementation(foo)
     * }</pre>
     * <p>
     * Note that since the prefix is only used when standard
     * resolution fails, native methods can be wrapped selectively.
     * <p>
     * Since each <code>ClassFileTransformer</code>
     * can do its own transformation of the bytecodes, more
     * than one layer of wrappers may be applied. Thus each
     * transformer needs its own prefix.  Since transformations
     * are applied in order, the prefixes, if applied, will
     * be applied in the same order
     * (see {@link #addTransformer(ClassFileTransformer,boolean) addTransformer}).
     * Thus if three transformers applied
     * wrappers, <code>foo</code> might become
     * <code>$trans3_$trans2_$trans1_foo</code>.  But if, say,
     * the second transformer did not apply a wrapper to
     * <code>foo</code> it would be just
     * <code>$trans3_$trans1_foo</code>.  To be able to
     * efficiently determine the sequence of prefixes,
     * an intermediate prefix is only applied if its non-native
     * wrapper exists.  Thus, in the last example, even though
     * <code>$trans1_foo</code> is not a native method, the
     * <code>$trans1_</code> prefix is applied since
     * <code>$trans1_foo</code> exists.
     *
     * @param   transformer
     *          The ClassFileTransformer which wraps using this prefix.
     * @param   prefix
     *          The prefix to apply to wrapped native methods when
     *          retrying a failed native method resolution. If prefix
     *          is either <code>null</code> or the empty string, then
     *          failed native method resolutions are not retried for
     *          this transformer.
     * @throws java.lang.NullPointerException if passed a <code>null</code> transformer.
     * @throws java.lang.UnsupportedOperationException if the current configuration of
     *           the JVM does not allow setting a native method prefix
     *           ({@link #isNativeMethodPrefixSupported} is false).
     * @throws java.lang.IllegalArgumentException if the transformer is not registered
     *           (see {@link #addTransformer(ClassFileTransformer,boolean) addTransformer}).
     *
     * @since 1.6
     */
    void
    setNativeMethodPrefix(ClassFileTransformer transformer, String prefix);

    /**
     * Redefine a module to expand the set of modules that it reads, the set of
     * packages that it exports or opens, or the services that it uses or
     * provides. This method facilitates the instrumentation of code in named
     * modules where that instrumentation requires changes to the set of modules
     * that are read, the packages that are exported or open, or the services
     * that are used or provided.
     *
     * <p> This method cannot reduce the set of modules that a module reads, nor
     * reduce the set of packages that it exports or opens, nor reduce the set
     * of services that it uses or provides. This method is a no-op when invoked
     * to redefine an unnamed module. </p>
     *
     * <p> When expanding the services that a module uses or provides then the
     * onus is on the agent to ensure that the service type will be accessible at
     * each instrumentation site where the service type is used. This method
     * does not check if the service type is a member of the module or in a
     * package exported to the module by another module that it reads. </p>
     *
     * <p> The {@code extraExports} parameter is the map of additional packages
     * to export. The {@code extraOpens} parameter is the map of additional
     * packages to open. In both cases, the map key is the fully-qualified name
     * of the package as defined in section 6.5.3 of
     * <cite>The Java Language Specification </cite>, for example, {@code
     * "java.lang"}. The map value is the non-empty set of modules that the
     * package should be exported or opened to. </p>
     *
     * <p> The {@code extraProvides} parameter is the additional service providers
     * for the module to provide. The map key is the service type. The map value
     * is the non-empty list of implementation types, each of which is a member
     * of the module and an implementation of the service. </p>
     *
     * <p> This method is safe for concurrent use and so allows multiple agents
     * to instrument and update the same module at around the same time. </p>
     *
     * @param module the module to redefine
     * @param extraReads the possibly-empty set of additional modules to read
     * @param extraExports the possibly-empty map of additional packages to export
     * @param extraOpens the possibly-empty map of additional packages to open
     * @param extraUses the possibly-empty set of additional services to use
     * @param extraProvides the possibly-empty map of additional services to provide
     *
     * @throws IllegalArgumentException
     *         If {@code extraExports} or {@code extraOpens} contains a key
     *         that is not a package in the module; if {@code extraExports} or
     *         {@code extraOpens} maps a key to an empty set; if a value in the
     *         {@code extraProvides} map contains a service provider type that
     *         is not a member of the module or an implementation of the service;
     *         or {@code extraProvides} maps a key to an empty list
     * @throws UnmodifiableModuleException if the module cannot be modified
     * @throws NullPointerException if any of the arguments are {@code null} or
     *         any of the Sets or Maps contains a {@code null} key or value
     *
     * @see #isModifiableModule(Module)
     * @since 9
     */
    void redefineModule(Module module,
                        Set<Module> extraReads,
                        Map<String, Set<Module>> extraExports,
                        Map<String, Set<Module>> extraOpens,
                        Set<Class<?>> extraUses,
                        Map<Class<?>, List<Class<?>>> extraProvides);

    /**
     * Tests whether a module can be modified with {@link #redefineModule
     * redefineModule}. If a module is modifiable then this method returns
     * {@code true}. If a module is not modifiable then this method returns
     * {@code false}. This method always returns {@code true} when the module
     * is an unnamed module (as redefining an unnamed module is a no-op).
     *
     * @param module the module to test if it can be modified
     * @return {@code true} if the module is modifiable, otherwise {@code false}
     * @throws NullPointerException if the module is {@code null}
     *
     * @since 9
     */
    boolean isModifiableModule(Module module);
}
