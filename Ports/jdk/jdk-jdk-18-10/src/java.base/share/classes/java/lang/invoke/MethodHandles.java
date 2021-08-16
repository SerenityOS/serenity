/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import jdk.internal.vm.annotation.ForceInline;
import sun.invoke.util.ValueConversions;
import sun.invoke.util.VerifyAccess;
import sun.invoke.util.Wrapper;
import sun.reflect.misc.ReflectUtil;
import sun.security.util.SecurityConstants;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.LambdaForm.BasicType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ReflectPermission;
import java.nio.ByteOrder;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.lang.invoke.LambdaForm.BasicType.V_TYPE;
import static java.lang.invoke.MethodHandleImpl.Intrinsic;
import static java.lang.invoke.MethodHandleNatives.Constants.*;
import static java.lang.invoke.MethodHandleStatics.newIllegalArgumentException;
import static java.lang.invoke.MethodType.methodType;

/**
 * This class consists exclusively of static methods that operate on or return
 * method handles. They fall into several categories:
 * <ul>
 * <li>Lookup methods which help create method handles for methods and fields.
 * <li>Combinator methods, which combine or transform pre-existing method handles into new ones.
 * <li>Other factory methods to create method handles that emulate other common JVM operations or control flow patterns.
 * </ul>
 * A lookup, combinator, or factory method will fail and throw an
 * {@code IllegalArgumentException} if the created method handle's type
 * would have <a href="MethodHandle.html#maxarity">too many parameters</a>.
 *
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */
public class MethodHandles {

    private MethodHandles() { }  // do not instantiate

    static final MemberName.Factory IMPL_NAMES = MemberName.getFactory();

    // See IMPL_LOOKUP below.

    //// Method handle creation from ordinary methods.

    /**
     * Returns a {@link Lookup lookup object} with
     * full capabilities to emulate all supported bytecode behaviors of the caller.
     * These capabilities include {@linkplain Lookup#hasFullPrivilegeAccess() full privilege access} to the caller.
     * Factory methods on the lookup object can create
     * <a href="MethodHandleInfo.html#directmh">direct method handles</a>
     * for any member that the caller has access to via bytecodes,
     * including protected and private fields and methods.
     * This lookup object is created by the original lookup class
     * and has the {@link Lookup#ORIGINAL ORIGINAL} bit set.
     * This lookup object is a <em>capability</em> which may be delegated to trusted agents.
     * Do not store it in place where untrusted code can access it.
     * <p>
     * This method is caller sensitive, which means that it may return different
     * values to different callers.
     * @return a lookup object for the caller of this method, with
     * {@linkplain Lookup#ORIGINAL original} and
     * {@linkplain Lookup#hasFullPrivilegeAccess() full privilege access}.
     */
    @CallerSensitive
    @ForceInline // to ensure Reflection.getCallerClass optimization
    public static Lookup lookup() {
        return new Lookup(Reflection.getCallerClass());
    }

    /**
     * This reflected$lookup method is the alternate implementation of
     * the lookup method when being invoked by reflection.
     */
    @CallerSensitive
    private static Lookup reflected$lookup() {
        Class<?> caller = Reflection.getCallerClass();
        if (caller.getClassLoader() == null) {
            throw newIllegalArgumentException("illegal lookupClass: "+caller);
        }
        return new Lookup(caller);
    }

    /**
     * Returns a {@link Lookup lookup object} which is trusted minimally.
     * The lookup has the {@code UNCONDITIONAL} mode.
     * It can only be used to create method handles to public members of
     * public classes in packages that are exported unconditionally.
     * <p>
     * As a matter of pure convention, the {@linkplain Lookup#lookupClass() lookup class}
     * of this lookup object will be {@link java.lang.Object}.
     *
     * @apiNote The use of Object is conventional, and because the lookup modes are
     * limited, there is no special access provided to the internals of Object, its package
     * or its module.  This public lookup object or other lookup object with
     * {@code UNCONDITIONAL} mode assumes readability. Consequently, the lookup class
     * is not used to determine the lookup context.
     *
     * <p style="font-size:smaller;">
     * <em>Discussion:</em>
     * The lookup class can be changed to any other class {@code C} using an expression of the form
     * {@link Lookup#in publicLookup().in(C.class)}.
     * A public lookup object is always subject to
     * <a href="MethodHandles.Lookup.html#secmgr">security manager checks</a>.
     * Also, it cannot access
     * <a href="MethodHandles.Lookup.html#callsens">caller sensitive methods</a>.
     * @return a lookup object which is trusted minimally
     *
     * @revised 9
     */
    public static Lookup publicLookup() {
        return Lookup.PUBLIC_LOOKUP;
    }

    /**
     * Returns a {@link Lookup lookup} object on a target class to emulate all supported
     * bytecode behaviors, including <a href="MethodHandles.Lookup.html#privacc">private access</a>.
     * The returned lookup object can provide access to classes in modules and packages,
     * and members of those classes, outside the normal rules of Java access control,
     * instead conforming to the more permissive rules for modular <em>deep reflection</em>.
     * <p>
     * A caller, specified as a {@code Lookup} object, in module {@code M1} is
     * allowed to do deep reflection on module {@code M2} and package of the target class
     * if and only if all of the following conditions are {@code true}:
     * <ul>
     * <li>If there is a security manager, its {@code checkPermission} method is
     * called to check {@code ReflectPermission("suppressAccessChecks")} and
     * that must return normally.
     * <li>The caller lookup object must have {@linkplain Lookup#hasFullPrivilegeAccess()
     * full privilege access}.  Specifically:
     *   <ul>
     *     <li>The caller lookup object must have the {@link Lookup#MODULE MODULE} lookup mode.
     *         (This is because otherwise there would be no way to ensure the original lookup
     *         creator was a member of any particular module, and so any subsequent checks
     *         for readability and qualified exports would become ineffective.)
     *     <li>The caller lookup object must have {@link Lookup#PRIVATE PRIVATE} access.
     *         (This is because an application intending to share intra-module access
     *         using {@link Lookup#MODULE MODULE} alone will inadvertently also share
     *         deep reflection to its own module.)
     *   </ul>
     * <li>The target class must be a proper class, not a primitive or array class.
     * (Thus, {@code M2} is well-defined.)
     * <li>If the caller module {@code M1} differs from
     * the target module {@code M2} then both of the following must be true:
     *   <ul>
     *     <li>{@code M1} {@link Module#canRead reads} {@code M2}.</li>
     *     <li>{@code M2} {@link Module#isOpen(String,Module) opens} the package
     *         containing the target class to at least {@code M1}.</li>
     *   </ul>
     * </ul>
     * <p>
     * If any of the above checks is violated, this method fails with an
     * exception.
     * <p>
     * Otherwise, if {@code M1} and {@code M2} are the same module, this method
     * returns a {@code Lookup} on {@code targetClass} with
     * {@linkplain Lookup#hasFullPrivilegeAccess() full privilege access}
     * with {@code null} previous lookup class.
     * <p>
     * Otherwise, {@code M1} and {@code M2} are two different modules.  This method
     * returns a {@code Lookup} on {@code targetClass} that records
     * the lookup class of the caller as the new previous lookup class with
     * {@code PRIVATE} access but no {@code MODULE} access.
     * <p>
     * The resulting {@code Lookup} object has no {@code ORIGINAL} access.
     *
     * @param targetClass the target class
     * @param caller the caller lookup object
     * @return a lookup object for the target class, with private access
     * @throws IllegalArgumentException if {@code targetClass} is a primitive type or void or array class
     * @throws NullPointerException if {@code targetClass} or {@code caller} is {@code null}
     * @throws SecurityException if denied by the security manager
     * @throws IllegalAccessException if any of the other access checks specified above fails
     * @since 9
     * @see Lookup#dropLookupMode
     * @see <a href="MethodHandles.Lookup.html#cross-module-lookup">Cross-module lookups</a>
     */
    public static Lookup privateLookupIn(Class<?> targetClass, Lookup caller) throws IllegalAccessException {
        if (caller.allowedModes == Lookup.TRUSTED) {
            return new Lookup(targetClass);
        }

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(SecurityConstants.ACCESS_PERMISSION);
        if (targetClass.isPrimitive())
            throw new IllegalArgumentException(targetClass + " is a primitive class");
        if (targetClass.isArray())
            throw new IllegalArgumentException(targetClass + " is an array class");
        // Ensure that we can reason accurately about private and module access.
        int requireAccess = Lookup.PRIVATE|Lookup.MODULE;
        if ((caller.lookupModes() & requireAccess) != requireAccess)
            throw new IllegalAccessException("caller does not have PRIVATE and MODULE lookup mode");

        // previous lookup class is never set if it has MODULE access
        assert caller.previousLookupClass() == null;

        Class<?> callerClass = caller.lookupClass();
        Module callerModule = callerClass.getModule();  // M1
        Module targetModule = targetClass.getModule();  // M2
        Class<?> newPreviousClass = null;
        int newModes = Lookup.FULL_POWER_MODES & ~Lookup.ORIGINAL;

        if (targetModule != callerModule) {
            if (!callerModule.canRead(targetModule))
                throw new IllegalAccessException(callerModule + " does not read " + targetModule);
            if (targetModule.isNamed()) {
                String pn = targetClass.getPackageName();
                assert !pn.isEmpty() : "unnamed package cannot be in named module";
                if (!targetModule.isOpen(pn, callerModule))
                    throw new IllegalAccessException(targetModule + " does not open " + pn + " to " + callerModule);
            }

            // M2 != M1, set previous lookup class to M1 and drop MODULE access
            newPreviousClass = callerClass;
            newModes &= ~Lookup.MODULE;
        }
        return Lookup.newLookup(targetClass, newPreviousClass, newModes);
    }

    /**
     * Returns the <em>class data</em> associated with the lookup class
     * of the given {@code caller} lookup object, or {@code null}.
     *
     * <p> A hidden class with class data can be created by calling
     * {@link Lookup#defineHiddenClassWithClassData(byte[], Object, boolean, Lookup.ClassOption...)
     * Lookup::defineHiddenClassWithClassData}.
     * This method will cause the static class initializer of the lookup
     * class of the given {@code caller} lookup object be executed if
     * it has not been initialized.
     *
     * <p> A hidden class created by {@link Lookup#defineHiddenClass(byte[], boolean, Lookup.ClassOption...)
     * Lookup::defineHiddenClass} and non-hidden classes have no class data.
     * {@code null} is returned if this method is called on the lookup object
     * on these classes.
     *
     * <p> The {@linkplain Lookup#lookupModes() lookup modes} for this lookup
     * must have {@linkplain Lookup#ORIGINAL original access}
     * in order to retrieve the class data.
     *
     * @apiNote
     * This method can be called as a bootstrap method for a dynamically computed
     * constant.  A framework can create a hidden class with class data, for
     * example that can be {@code Class} or {@code MethodHandle} object.
     * The class data is accessible only to the lookup object
     * created by the original caller but inaccessible to other members
     * in the same nest.  If a framework passes security sensitive objects
     * to a hidden class via class data, it is recommended to load the value
     * of class data as a dynamically computed constant instead of storing
     * the class data in private static field(s) which are accessible to
     * other nestmates.
     *
     * @param <T> the type to cast the class data object to
     * @param caller the lookup context describing the class performing the
     * operation (normally stacked by the JVM)
     * @param name must be {@link ConstantDescs#DEFAULT_NAME}
     *             ({@code "_"})
     * @param type the type of the class data
     * @return the value of the class data if present in the lookup class;
     * otherwise {@code null}
     * @throws IllegalArgumentException if name is not {@code "_"}
     * @throws IllegalAccessException if the lookup context does not have
     * {@linkplain Lookup#ORIGINAL original} access
     * @throws ClassCastException if the class data cannot be converted to
     * the given {@code type}
     * @throws NullPointerException if {@code caller} or {@code type} argument
     * is {@code null}
     * @see Lookup#defineHiddenClassWithClassData(byte[], Object, boolean, Lookup.ClassOption...)
     * @see MethodHandles#classDataAt(Lookup, String, Class, int)
     * @since 16
     * @jvms 5.5 Initialization
     */
     public static <T> T classData(Lookup caller, String name, Class<T> type) throws IllegalAccessException {
         Objects.requireNonNull(caller);
         Objects.requireNonNull(type);
         if (!ConstantDescs.DEFAULT_NAME.equals(name)) {
             throw new IllegalArgumentException("name must be \"_\": " + name);
         }

         if ((caller.lookupModes() & Lookup.ORIGINAL) != Lookup.ORIGINAL)  {
             throw new IllegalAccessException(caller + " does not have ORIGINAL access");
         }

         Object classdata = MethodHandleNatives.classData(caller.lookupClass());
         if (classdata == null) return null;

         try {
             return BootstrapMethodInvoker.widenAndCast(classdata, type);
         } catch (RuntimeException|Error e) {
             throw e; // let CCE and other runtime exceptions through
         } catch (Throwable e) {
             throw new InternalError(e);
         }
    }

    /**
     * Returns the element at the specified index in the
     * {@linkplain #classData(Lookup, String, Class) class data},
     * if the class data associated with the lookup class
     * of the given {@code caller} lookup object is a {@code List}.
     * If the class data is not present in this lookup class, this method
     * returns {@code null}.
     *
     * <p> A hidden class with class data can be created by calling
     * {@link Lookup#defineHiddenClassWithClassData(byte[], Object, boolean, Lookup.ClassOption...)
     * Lookup::defineHiddenClassWithClassData}.
     * This method will cause the static class initializer of the lookup
     * class of the given {@code caller} lookup object be executed if
     * it has not been initialized.
     *
     * <p> A hidden class created by {@link Lookup#defineHiddenClass(byte[], boolean, Lookup.ClassOption...)
     * Lookup::defineHiddenClass} and non-hidden classes have no class data.
     * {@code null} is returned if this method is called on the lookup object
     * on these classes.
     *
     * <p> The {@linkplain Lookup#lookupModes() lookup modes} for this lookup
     * must have {@linkplain Lookup#ORIGINAL original access}
     * in order to retrieve the class data.
     *
     * @apiNote
     * This method can be called as a bootstrap method for a dynamically computed
     * constant.  A framework can create a hidden class with class data, for
     * example that can be {@code List.of(o1, o2, o3....)} containing more than
     * one object and use this method to load one element at a specific index.
     * The class data is accessible only to the lookup object
     * created by the original caller but inaccessible to other members
     * in the same nest.  If a framework passes security sensitive objects
     * to a hidden class via class data, it is recommended to load the value
     * of class data as a dynamically computed constant instead of storing
     * the class data in private static field(s) which are accessible to other
     * nestmates.
     *
     * @param <T> the type to cast the result object to
     * @param caller the lookup context describing the class performing the
     * operation (normally stacked by the JVM)
     * @param name must be {@link java.lang.constant.ConstantDescs#DEFAULT_NAME}
     *             ({@code "_"})
     * @param type the type of the element at the given index in the class data
     * @param index index of the element in the class data
     * @return the element at the given index in the class data
     * if the class data is present; otherwise {@code null}
     * @throws IllegalArgumentException if name is not {@code "_"}
     * @throws IllegalAccessException if the lookup context does not have
     * {@linkplain Lookup#ORIGINAL original} access
     * @throws ClassCastException if the class data cannot be converted to {@code List}
     * or the element at the specified index cannot be converted to the given type
     * @throws IndexOutOfBoundsException if the index is out of range
     * @throws NullPointerException if {@code caller} or {@code type} argument is
     * {@code null}; or if unboxing operation fails because
     * the element at the given index is {@code null}
     *
     * @since 16
     * @see #classData(Lookup, String, Class)
     * @see Lookup#defineHiddenClassWithClassData(byte[], Object, boolean, Lookup.ClassOption...)
     */
    public static <T> T classDataAt(Lookup caller, String name, Class<T> type, int index)
            throws IllegalAccessException
    {
        @SuppressWarnings("unchecked")
        List<Object> classdata = (List<Object>)classData(caller, name, List.class);
        if (classdata == null) return null;

        try {
            Object element = classdata.get(index);
            return BootstrapMethodInvoker.widenAndCast(element, type);
        } catch (RuntimeException|Error e) {
            throw e; // let specified exceptions and other runtime exceptions/errors through
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    /**
     * Performs an unchecked "crack" of a
     * <a href="MethodHandleInfo.html#directmh">direct method handle</a>.
     * The result is as if the user had obtained a lookup object capable enough
     * to crack the target method handle, called
     * {@link java.lang.invoke.MethodHandles.Lookup#revealDirect Lookup.revealDirect}
     * on the target to obtain its symbolic reference, and then called
     * {@link java.lang.invoke.MethodHandleInfo#reflectAs MethodHandleInfo.reflectAs}
     * to resolve the symbolic reference to a member.
     * <p>
     * If there is a security manager, its {@code checkPermission} method
     * is called with a {@code ReflectPermission("suppressAccessChecks")} permission.
     * @param <T> the desired type of the result, either {@link Member} or a subtype
     * @param target a direct method handle to crack into symbolic reference components
     * @param expected a class object representing the desired result type {@code T}
     * @return a reference to the method, constructor, or field object
     * @throws    SecurityException if the caller is not privileged to call {@code setAccessible}
     * @throws    NullPointerException if either argument is {@code null}
     * @throws    IllegalArgumentException if the target is not a direct method handle
     * @throws    ClassCastException if the member is not of the expected type
     * @since 1.8
     */
    public static <T extends Member> T reflectAs(Class<T> expected, MethodHandle target) {
        @SuppressWarnings("removal")
        SecurityManager smgr = System.getSecurityManager();
        if (smgr != null)  smgr.checkPermission(SecurityConstants.ACCESS_PERMISSION);
        Lookup lookup = Lookup.IMPL_LOOKUP;  // use maximally privileged lookup
        return lookup.revealDirect(target).reflectAs(expected, lookup);
    }

    /**
     * A <em>lookup object</em> is a factory for creating method handles,
     * when the creation requires access checking.
     * Method handles do not perform
     * access checks when they are called, but rather when they are created.
     * Therefore, method handle access
     * restrictions must be enforced when a method handle is created.
     * The caller class against which those restrictions are enforced
     * is known as the {@linkplain #lookupClass() lookup class}.
     * <p>
     * A lookup class which needs to create method handles will call
     * {@link MethodHandles#lookup() MethodHandles.lookup} to create a factory for itself.
     * When the {@code Lookup} factory object is created, the identity of the lookup class is
     * determined, and securely stored in the {@code Lookup} object.
     * The lookup class (or its delegates) may then use factory methods
     * on the {@code Lookup} object to create method handles for access-checked members.
     * This includes all methods, constructors, and fields which are allowed to the lookup class,
     * even private ones.
     *
     * <h2><a id="lookups"></a>Lookup Factory Methods</h2>
     * The factory methods on a {@code Lookup} object correspond to all major
     * use cases for methods, constructors, and fields.
     * Each method handle created by a factory method is the functional
     * equivalent of a particular <em>bytecode behavior</em>.
     * (Bytecode behaviors are described in section {@jvms 5.4.3.5} of
     * the Java Virtual Machine Specification.)
     * Here is a summary of the correspondence between these factory methods and
     * the behavior of the resulting method handles:
     * <table class="striped">
     * <caption style="display:none">lookup method behaviors</caption>
     * <thead>
     * <tr>
     *     <th scope="col"><a id="equiv"></a>lookup expression</th>
     *     <th scope="col">member</th>
     *     <th scope="col">bytecode behavior</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#findGetter lookup.findGetter(C.class,"f",FT.class)}</th>
     *     <td>{@code FT f;}</td><td>{@code (T) this.f;}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#findStaticGetter lookup.findStaticGetter(C.class,"f",FT.class)}</th>
     *     <td>{@code static}<br>{@code FT f;}</td><td>{@code (FT) C.f;}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#findSetter lookup.findSetter(C.class,"f",FT.class)}</th>
     *     <td>{@code FT f;}</td><td>{@code this.f = x;}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#findStaticSetter lookup.findStaticSetter(C.class,"f",FT.class)}</th>
     *     <td>{@code static}<br>{@code FT f;}</td><td>{@code C.f = arg;}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#findVirtual lookup.findVirtual(C.class,"m",MT)}</th>
     *     <td>{@code T m(A*);}</td><td>{@code (T) this.m(arg*);}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#findStatic lookup.findStatic(C.class,"m",MT)}</th>
     *     <td>{@code static}<br>{@code T m(A*);}</td><td>{@code (T) C.m(arg*);}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#findSpecial lookup.findSpecial(C.class,"m",MT,this.class)}</th>
     *     <td>{@code T m(A*);}</td><td>{@code (T) super.m(arg*);}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#findConstructor lookup.findConstructor(C.class,MT)}</th>
     *     <td>{@code C(A*);}</td><td>{@code new C(arg*);}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#unreflectGetter lookup.unreflectGetter(aField)}</th>
     *     <td>({@code static})?<br>{@code FT f;}</td><td>{@code (FT) aField.get(thisOrNull);}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#unreflectSetter lookup.unreflectSetter(aField)}</th>
     *     <td>({@code static})?<br>{@code FT f;}</td><td>{@code aField.set(thisOrNull, arg);}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#unreflect lookup.unreflect(aMethod)}</th>
     *     <td>({@code static})?<br>{@code T m(A*);}</td><td>{@code (T) aMethod.invoke(thisOrNull, arg*);}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#unreflectConstructor lookup.unreflectConstructor(aConstructor)}</th>
     *     <td>{@code C(A*);}</td><td>{@code (C) aConstructor.newInstance(arg*);}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#unreflectSpecial lookup.unreflectSpecial(aMethod,this.class)}</th>
     *     <td>{@code T m(A*);}</td><td>{@code (T) super.m(arg*);}</td>
     * </tr>
     * <tr>
     *     <th scope="row">{@link java.lang.invoke.MethodHandles.Lookup#findClass lookup.findClass("C")}</th>
     *     <td>{@code class C { ... }}</td><td>{@code C.class;}</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * Here, the type {@code C} is the class or interface being searched for a member,
     * documented as a parameter named {@code refc} in the lookup methods.
     * The method type {@code MT} is composed from the return type {@code T}
     * and the sequence of argument types {@code A*}.
     * The constructor also has a sequence of argument types {@code A*} and
     * is deemed to return the newly-created object of type {@code C}.
     * Both {@code MT} and the field type {@code FT} are documented as a parameter named {@code type}.
     * The formal parameter {@code this} stands for the self-reference of type {@code C};
     * if it is present, it is always the leading argument to the method handle invocation.
     * (In the case of some {@code protected} members, {@code this} may be
     * restricted in type to the lookup class; see below.)
     * The name {@code arg} stands for all the other method handle arguments.
     * In the code examples for the Core Reflection API, the name {@code thisOrNull}
     * stands for a null reference if the accessed method or field is static,
     * and {@code this} otherwise.
     * The names {@code aMethod}, {@code aField}, and {@code aConstructor} stand
     * for reflective objects corresponding to the given members declared in type {@code C}.
     * <p>
     * The bytecode behavior for a {@code findClass} operation is a load of a constant class,
     * as if by {@code ldc CONSTANT_Class}.
     * The behavior is represented, not as a method handle, but directly as a {@code Class} constant.
     * <p>
     * In cases where the given member is of variable arity (i.e., a method or constructor)
     * the returned method handle will also be of {@linkplain MethodHandle#asVarargsCollector variable arity}.
     * In all other cases, the returned method handle will be of fixed arity.
     * <p style="font-size:smaller;">
     * <em>Discussion:</em>
     * The equivalence between looked-up method handles and underlying
     * class members and bytecode behaviors
     * can break down in a few ways:
     * <ul style="font-size:smaller;">
     * <li>If {@code C} is not symbolically accessible from the lookup class's loader,
     * the lookup can still succeed, even when there is no equivalent
     * Java expression or bytecoded constant.
     * <li>Likewise, if {@code T} or {@code MT}
     * is not symbolically accessible from the lookup class's loader,
     * the lookup can still succeed.
     * For example, lookups for {@code MethodHandle.invokeExact} and
     * {@code MethodHandle.invoke} will always succeed, regardless of requested type.
     * <li>If there is a security manager installed, it can forbid the lookup
     * on various grounds (<a href="MethodHandles.Lookup.html#secmgr">see below</a>).
     * By contrast, the {@code ldc} instruction on a {@code CONSTANT_MethodHandle}
     * constant is not subject to security manager checks.
     * <li>If the looked-up method has a
     * <a href="MethodHandle.html#maxarity">very large arity</a>,
     * the method handle creation may fail with an
     * {@code IllegalArgumentException}, due to the method handle type having
     * <a href="MethodHandle.html#maxarity">too many parameters.</a>
     * </ul>
     *
     * <h2><a id="access"></a>Access checking</h2>
     * Access checks are applied in the factory methods of {@code Lookup},
     * when a method handle is created.
     * This is a key difference from the Core Reflection API, since
     * {@link java.lang.reflect.Method#invoke java.lang.reflect.Method.invoke}
     * performs access checking against every caller, on every call.
     * <p>
     * All access checks start from a {@code Lookup} object, which
     * compares its recorded lookup class against all requests to
     * create method handles.
     * A single {@code Lookup} object can be used to create any number
     * of access-checked method handles, all checked against a single
     * lookup class.
     * <p>
     * A {@code Lookup} object can be shared with other trusted code,
     * such as a metaobject protocol.
     * A shared {@code Lookup} object delegates the capability
     * to create method handles on private members of the lookup class.
     * Even if privileged code uses the {@code Lookup} object,
     * the access checking is confined to the privileges of the
     * original lookup class.
     * <p>
     * A lookup can fail, because
     * the containing class is not accessible to the lookup class, or
     * because the desired class member is missing, or because the
     * desired class member is not accessible to the lookup class, or
     * because the lookup object is not trusted enough to access the member.
     * In the case of a field setter function on a {@code final} field,
     * finality enforcement is treated as a kind of access control,
     * and the lookup will fail, except in special cases of
     * {@link Lookup#unreflectSetter Lookup.unreflectSetter}.
     * In any of these cases, a {@code ReflectiveOperationException} will be
     * thrown from the attempted lookup.  The exact class will be one of
     * the following:
     * <ul>
     * <li>NoSuchMethodException &mdash; if a method is requested but does not exist
     * <li>NoSuchFieldException &mdash; if a field is requested but does not exist
     * <li>IllegalAccessException &mdash; if the member exists but an access check fails
     * </ul>
     * <p>
     * In general, the conditions under which a method handle may be
     * looked up for a method {@code M} are no more restrictive than the conditions
     * under which the lookup class could have compiled, verified, and resolved a call to {@code M}.
     * Where the JVM would raise exceptions like {@code NoSuchMethodError},
     * a method handle lookup will generally raise a corresponding
     * checked exception, such as {@code NoSuchMethodException}.
     * And the effect of invoking the method handle resulting from the lookup
     * is <a href="MethodHandles.Lookup.html#equiv">exactly equivalent</a>
     * to executing the compiled, verified, and resolved call to {@code M}.
     * The same point is true of fields and constructors.
     * <p style="font-size:smaller;">
     * <em>Discussion:</em>
     * Access checks only apply to named and reflected methods,
     * constructors, and fields.
     * Other method handle creation methods, such as
     * {@link MethodHandle#asType MethodHandle.asType},
     * do not require any access checks, and are used
     * independently of any {@code Lookup} object.
     * <p>
     * If the desired member is {@code protected}, the usual JVM rules apply,
     * including the requirement that the lookup class must either be in the
     * same package as the desired member, or must inherit that member.
     * (See the Java Virtual Machine Specification, sections {@jvms
     * 4.9.2}, {@jvms 5.4.3.5}, and {@jvms 6.4}.)
     * In addition, if the desired member is a non-static field or method
     * in a different package, the resulting method handle may only be applied
     * to objects of the lookup class or one of its subclasses.
     * This requirement is enforced by narrowing the type of the leading
     * {@code this} parameter from {@code C}
     * (which will necessarily be a superclass of the lookup class)
     * to the lookup class itself.
     * <p>
     * The JVM imposes a similar requirement on {@code invokespecial} instruction,
     * that the receiver argument must match both the resolved method <em>and</em>
     * the current class.  Again, this requirement is enforced by narrowing the
     * type of the leading parameter to the resulting method handle.
     * (See the Java Virtual Machine Specification, section {@jvms 4.10.1.9}.)
     * <p>
     * The JVM represents constructors and static initializer blocks as internal methods
     * with special names ({@code "<init>"} and {@code "<clinit>"}).
     * The internal syntax of invocation instructions allows them to refer to such internal
     * methods as if they were normal methods, but the JVM bytecode verifier rejects them.
     * A lookup of such an internal method will produce a {@code NoSuchMethodException}.
     * <p>
     * If the relationship between nested types is expressed directly through the
     * {@code NestHost} and {@code NestMembers} attributes
     * (see the Java Virtual Machine Specification, sections {@jvms
     * 4.7.28} and {@jvms 4.7.29}),
     * then the associated {@code Lookup} object provides direct access to
     * the lookup class and all of its nestmates
     * (see {@link java.lang.Class#getNestHost Class.getNestHost}).
     * Otherwise, access between nested classes is obtained by the Java compiler creating
     * a wrapper method to access a private method of another class in the same nest.
     * For example, a nested class {@code C.D}
     * can access private members within other related classes such as
     * {@code C}, {@code C.D.E}, or {@code C.B},
     * but the Java compiler may need to generate wrapper methods in
     * those related classes.  In such cases, a {@code Lookup} object on
     * {@code C.E} would be unable to access those private members.
     * A workaround for this limitation is the {@link Lookup#in Lookup.in} method,
     * which can transform a lookup on {@code C.E} into one on any of those other
     * classes, without special elevation of privilege.
     * <p>
     * The accesses permitted to a given lookup object may be limited,
     * according to its set of {@link #lookupModes lookupModes},
     * to a subset of members normally accessible to the lookup class.
     * For example, the {@link MethodHandles#publicLookup publicLookup}
     * method produces a lookup object which is only allowed to access
     * public members in public classes of exported packages.
     * The caller sensitive method {@link MethodHandles#lookup lookup}
     * produces a lookup object with full capabilities relative to
     * its caller class, to emulate all supported bytecode behaviors.
     * Also, the {@link Lookup#in Lookup.in} method may produce a lookup object
     * with fewer access modes than the original lookup object.
     *
     * <p style="font-size:smaller;">
     * <a id="privacc"></a>
     * <em>Discussion of private and module access:</em>
     * We say that a lookup has <em>private access</em>
     * if its {@linkplain #lookupModes lookup modes}
     * include the possibility of accessing {@code private} members
     * (which includes the private members of nestmates).
     * As documented in the relevant methods elsewhere,
     * only lookups with private access possess the following capabilities:
     * <ul style="font-size:smaller;">
     * <li>access private fields, methods, and constructors of the lookup class and its nestmates
     * <li>create method handles which {@link Lookup#findSpecial emulate invokespecial} instructions
     * <li>avoid <a href="MethodHandles.Lookup.html#secmgr">package access checks</a>
     *     for classes accessible to the lookup class
     * <li>create {@link Lookup#in delegated lookup objects} which have private access to other classes
     *     within the same package member
     * </ul>
     * <p style="font-size:smaller;">
     * Similarly, a lookup with module access ensures that the original lookup creator was
     * a member in the same module as the lookup class.
     * <p style="font-size:smaller;">
     * Private and module access are independently determined modes; a lookup may have
     * either or both or neither.  A lookup which possesses both access modes is said to
     * possess {@linkplain #hasFullPrivilegeAccess() full privilege access}.
     * <p style="font-size:smaller;">
     * A lookup with <em>original access</em> ensures that this lookup is created by
     * the original lookup class and the bootstrap method invoked by the VM.
     * Such a lookup with original access also has private and module access
     * which has the following additional capability:
     * <ul style="font-size:smaller;">
     * <li>create method handles which invoke <a href="MethodHandles.Lookup.html#callsens">caller sensitive</a> methods,
     *     such as {@code Class.forName}
     * <li>obtain the {@linkplain MethodHandles#classData(Lookup, String, Class)
     * class data} associated with the lookup class</li>
     * </ul>
     * <p style="font-size:smaller;">
     * Each of these permissions is a consequence of the fact that a lookup object
     * with private access can be securely traced back to an originating class,
     * whose <a href="MethodHandles.Lookup.html#equiv">bytecode behaviors</a> and Java language access permissions
     * can be reliably determined and emulated by method handles.
     *
     * <h2><a id="cross-module-lookup"></a>Cross-module lookups</h2>
     * When a lookup class in one module {@code M1} accesses a class in another module
     * {@code M2}, extra access checking is performed beyond the access mode bits.
     * A {@code Lookup} with {@link #PUBLIC} mode and a lookup class in {@code M1}
     * can access public types in {@code M2} when {@code M2} is readable to {@code M1}
     * and when the type is in a package of {@code M2} that is exported to
     * at least {@code M1}.
     * <p>
     * A {@code Lookup} on {@code C} can also <em>teleport</em> to a target class
     * via {@link #in(Class) Lookup.in} and {@link MethodHandles#privateLookupIn(Class, Lookup)
     * MethodHandles.privateLookupIn} methods.
     * Teleporting across modules will always record the original lookup class as
     * the <em>{@linkplain #previousLookupClass() previous lookup class}</em>
     * and drops {@link Lookup#MODULE MODULE} access.
     * If the target class is in the same module as the lookup class {@code C},
     * then the target class becomes the new lookup class
     * and there is no change to the previous lookup class.
     * If the target class is in a different module from {@code M1} ({@code C}'s module),
     * {@code C} becomes the new previous lookup class
     * and the target class becomes the new lookup class.
     * In that case, if there was already a previous lookup class in {@code M0},
     * and it differs from {@code M1} and {@code M2}, then the resulting lookup
     * drops all privileges.
     * For example,
     * <blockquote><pre>
     * {@code
     * Lookup lookup = MethodHandles.lookup();   // in class C
     * Lookup lookup2 = lookup.in(D.class);
     * MethodHandle mh = lookup2.findStatic(E.class, "m", MT);
     * }</pre></blockquote>
     * <p>
     * The {@link #lookup()} factory method produces a {@code Lookup} object
     * with {@code null} previous lookup class.
     * {@link Lookup#in lookup.in(D.class)} transforms the {@code lookup} on class {@code C}
     * to class {@code D} without elevation of privileges.
     * If {@code C} and {@code D} are in the same module,
     * {@code lookup2} records {@code D} as the new lookup class and keeps the
     * same previous lookup class as the original {@code lookup}, or
     * {@code null} if not present.
     * <p>
     * When a {@code Lookup} teleports from a class
     * in one nest to another nest, {@code PRIVATE} access is dropped.
     * When a {@code Lookup} teleports from a class in one package to
     * another package, {@code PACKAGE} access is dropped.
     * When a {@code Lookup} teleports from a class in one module to another module,
     * {@code MODULE} access is dropped.
     * Teleporting across modules drops the ability to access non-exported classes
     * in both the module of the new lookup class and the module of the old lookup class
     * and the resulting {@code Lookup} remains only {@code PUBLIC} access.
     * A {@code Lookup} can teleport back and forth to a class in the module of
     * the lookup class and the module of the previous class lookup.
     * Teleporting across modules can only decrease access but cannot increase it.
     * Teleporting to some third module drops all accesses.
     * <p>
     * In the above example, if {@code C} and {@code D} are in different modules,
     * {@code lookup2} records {@code D} as its lookup class and
     * {@code C} as its previous lookup class and {@code lookup2} has only
     * {@code PUBLIC} access. {@code lookup2} can teleport to other class in
     * {@code C}'s module and {@code D}'s module.
     * If class {@code E} is in a third module, {@code lookup2.in(E.class)} creates
     * a {@code Lookup} on {@code E} with no access and {@code lookup2}'s lookup
     * class {@code D} is recorded as its previous lookup class.
     * <p>
     * Teleporting across modules restricts access to the public types that
     * both the lookup class and the previous lookup class can equally access
     * (see below).
     * <p>
     * {@link MethodHandles#privateLookupIn(Class, Lookup) MethodHandles.privateLookupIn(T.class, lookup)}
     * can be used to teleport a {@code lookup} from class {@code C} to class {@code T}
     * and create a new {@code Lookup} with <a href="#privacc">private access</a>
     * if the lookup class is allowed to do <em>deep reflection</em> on {@code T}.
     * The {@code lookup} must have {@link #MODULE} and {@link #PRIVATE} access
     * to call {@code privateLookupIn}.
     * A {@code lookup} on {@code C} in module {@code M1} is allowed to do deep reflection
     * on all classes in {@code M1}.  If {@code T} is in {@code M1}, {@code privateLookupIn}
     * produces a new {@code Lookup} on {@code T} with full capabilities.
     * A {@code lookup} on {@code C} is also allowed
     * to do deep reflection on {@code T} in another module {@code M2} if
     * {@code M1} reads {@code M2} and {@code M2} {@link Module#isOpen(String,Module) opens}
     * the package containing {@code T} to at least {@code M1}.
     * {@code T} becomes the new lookup class and {@code C} becomes the new previous
     * lookup class and {@code MODULE} access is dropped from the resulting {@code Lookup}.
     * The resulting {@code Lookup} can be used to do member lookup or teleport
     * to another lookup class by calling {@link #in Lookup::in}.  But
     * it cannot be used to obtain another private {@code Lookup} by calling
     * {@link MethodHandles#privateLookupIn(Class, Lookup) privateLookupIn}
     * because it has no {@code MODULE} access.
     *
     * <h2><a id="module-access-check"></a>Cross-module access checks</h2>
     *
     * A {@code Lookup} with {@link #PUBLIC} or with {@link #UNCONDITIONAL} mode
     * allows cross-module access. The access checking is performed with respect
     * to both the lookup class and the previous lookup class if present.
     * <p>
     * A {@code Lookup} with {@link #UNCONDITIONAL} mode can access public type
     * in all modules when the type is in a package that is {@linkplain Module#isExported(String)
     * exported unconditionally}.
     * <p>
     * If a {@code Lookup} on {@code LC} in {@code M1} has no previous lookup class,
     * the lookup with {@link #PUBLIC} mode can access all public types in modules
     * that are readable to {@code M1} and the type is in a package that is exported
     * at least to {@code M1}.
     * <p>
     * If a {@code Lookup} on {@code LC} in {@code M1} has a previous lookup class
     * {@code PLC} on {@code M0}, the lookup with {@link #PUBLIC} mode can access
     * the intersection of all public types that are accessible to {@code M1}
     * with all public types that are accessible to {@code M0}. {@code M0}
     * reads {@code M1} and hence the set of accessible types includes:
     *
     * <table class="striped">
     * <caption style="display:none">
     * Public types in the following packages are accessible to the
     * lookup class and the previous lookup class.
     * </caption>
     * <thead>
     * <tr>
     * <th scope="col">Equally accessible types to {@code M0} and {@code M1}</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <th scope="row" style="text-align:left">unconditional-exported packages from {@code M1}</th>
     * </tr>
     * <tr>
     * <th scope="row" style="text-align:left">unconditional-exported packages from {@code M0} if {@code M1} reads {@code M0}</th>
     * </tr>
     * <tr>
     * <th scope="row" style="text-align:left">unconditional-exported packages from a third module {@code M2}
     * if both {@code M0} and {@code M1} read {@code M2}</th>
     * </tr>
     * <tr>
     * <th scope="row" style="text-align:left">qualified-exported packages from {@code M1} to {@code M0}</th>
     * </tr>
     * <tr>
     * <th scope="row" style="text-align:left">qualified-exported packages from {@code M0} to {@code M1}
     * if {@code M1} reads {@code M0}</th>
     * </tr>
     * <tr>
     * <th scope="row" style="text-align:left">qualified-exported packages from a third module {@code M2} to
     * both {@code M0} and {@code M1} if both {@code M0} and {@code M1} read {@code M2}</th>
     * </tr>
     * </tbody>
     * </table>
     *
     * <h2><a id="access-modes"></a>Access modes</h2>
     *
     * The table below shows the access modes of a {@code Lookup} produced by
     * any of the following factory or transformation methods:
     * <ul>
     * <li>{@link #lookup() MethodHandles::lookup}</li>
     * <li>{@link #publicLookup() MethodHandles::publicLookup}</li>
     * <li>{@link #privateLookupIn(Class, Lookup) MethodHandles::privateLookupIn}</li>
     * <li>{@link Lookup#in Lookup::in}</li>
     * <li>{@link Lookup#dropLookupMode(int) Lookup::dropLookupMode}</li>
     * </ul>
     *
     * <table class="striped">
     * <caption style="display:none">
     * Access mode summary
     * </caption>
     * <thead>
     * <tr>
     * <th scope="col">Lookup object</th>
     * <th style="text-align:center">original</th>
     * <th style="text-align:center">protected</th>
     * <th style="text-align:center">private</th>
     * <th style="text-align:center">package</th>
     * <th style="text-align:center">module</th>
     * <th style="text-align:center">public</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <th scope="row" style="text-align:left">{@code CL = MethodHandles.lookup()} in {@code C}</th>
     * <td style="text-align:center">ORI</td>
     * <td style="text-align:center">PRO</td>
     * <td style="text-align:center">PRI</td>
     * <td style="text-align:center">PAC</td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <th scope="row" style="text-align:left">{@code CL.in(C1)} same package</th>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">PAC</td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <th scope="row" style="text-align:left">{@code CL.in(C1)} same module</th>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <th scope="row" style="text-align:left">{@code CL.in(D)} different module</th>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code CL.in(D).in(C)} hop back to module</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI1 = privateLookupIn(C1,CL)}</td>
     * <td></td>
     * <td style="text-align:center">PRO</td>
     * <td style="text-align:center">PRI</td>
     * <td style="text-align:center">PAC</td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI1a = privateLookupIn(C,PRI1)}</td>
     * <td></td>
     * <td style="text-align:center">PRO</td>
     * <td style="text-align:center">PRI</td>
     * <td style="text-align:center">PAC</td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI1.in(C1)} same package</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">PAC</td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI1.in(C1)} different package</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI1.in(D)} different module</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI1.dropLookupMode(PROTECTED)}</td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">PRI</td>
     * <td style="text-align:center">PAC</td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI1.dropLookupMode(PRIVATE)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">PAC</td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI1.dropLookupMode(PACKAGE)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI1.dropLookupMode(MODULE)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI1.dropLookupMode(PUBLIC)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">none</td>
     * <tr>
     * <td>{@code PRI2 = privateLookupIn(D,CL)}</td>
     * <td></td>
     * <td style="text-align:center">PRO</td>
     * <td style="text-align:center">PRI</td>
     * <td style="text-align:center">PAC</td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code privateLookupIn(D,PRI1)}</td>
     * <td></td>
     * <td style="text-align:center">PRO</td>
     * <td style="text-align:center">PRI</td>
     * <td style="text-align:center">PAC</td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code privateLookupIn(C,PRI2)} fails</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">IAE</td>
     * </tr>
     * <tr>
     * <td>{@code PRI2.in(D2)} same package</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">PAC</td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI2.in(D2)} different package</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI2.in(C1)} hop back to module</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI2.in(E)} hop to third module</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">none</td>
     * </tr>
     * <tr>
     * <td>{@code PRI2.dropLookupMode(PROTECTED)}</td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">PRI</td>
     * <td style="text-align:center">PAC</td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI2.dropLookupMode(PRIVATE)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">PAC</td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI2.dropLookupMode(PACKAGE)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI2.dropLookupMode(MODULE)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">2R</td>
     * </tr>
     * <tr>
     * <td>{@code PRI2.dropLookupMode(PUBLIC)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">none</td>
     * </tr>
     * <tr>
     * <td>{@code CL.dropLookupMode(PROTECTED)}</td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">PRI</td>
     * <td style="text-align:center">PAC</td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code CL.dropLookupMode(PRIVATE)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">PAC</td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code CL.dropLookupMode(PACKAGE)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">MOD</td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code CL.dropLookupMode(MODULE)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">1R</td>
     * </tr>
     * <tr>
     * <td>{@code CL.dropLookupMode(PUBLIC)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">none</td>
     * </tr>
     * <tr>
     * <td>{@code PUB = publicLookup()}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">U</td>
     * </tr>
     * <tr>
     * <td>{@code PUB.in(D)} different module</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">U</td>
     * </tr>
     * <tr>
     * <td>{@code PUB.in(D).in(E)} third module</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">U</td>
     * </tr>
     * <tr>
     * <td>{@code PUB.dropLookupMode(UNCONDITIONAL)}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">none</td>
     * </tr>
     * <tr>
     * <td>{@code privateLookupIn(C1,PUB)} fails</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">IAE</td>
     * </tr>
     * <tr>
     * <td>{@code ANY.in(X)}, for inaccessible {@code X}</td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td></td>
     * <td style="text-align:center">none</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * <p>
     * Notes:
     * <ul>
     * <li>Class {@code C} and class {@code C1} are in module {@code M1},
     *     but {@code D} and {@code D2} are in module {@code M2}, and {@code E}
     *     is in module {@code M3}. {@code X} stands for class which is inaccessible
     *     to the lookup. {@code ANY} stands for any of the example lookups.</li>
     * <li>{@code ORI} indicates {@link #ORIGINAL} bit set,
     *     {@code PRO} indicates {@link #PROTECTED} bit set,
     *     {@code PRI} indicates {@link #PRIVATE} bit set,
     *     {@code PAC} indicates {@link #PACKAGE} bit set,
     *     {@code MOD} indicates {@link #MODULE} bit set,
     *     {@code 1R} and {@code 2R} indicate {@link #PUBLIC} bit set,
     *     {@code U} indicates {@link #UNCONDITIONAL} bit set,
     *     {@code IAE} indicates {@code IllegalAccessException} thrown.</li>
     * <li>Public access comes in three kinds:
     * <ul>
     * <li>unconditional ({@code U}): the lookup assumes readability.
     *     The lookup has {@code null} previous lookup class.
     * <li>one-module-reads ({@code 1R}): the module access checking is
     *     performed with respect to the lookup class.  The lookup has {@code null}
     *     previous lookup class.
     * <li>two-module-reads ({@code 2R}): the module access checking is
     *     performed with respect to the lookup class and the previous lookup class.
     *     The lookup has a non-null previous lookup class which is in a
     *     different module from the current lookup class.
     * </ul>
     * <li>Any attempt to reach a third module loses all access.</li>
     * <li>If a target class {@code X} is not accessible to {@code Lookup::in}
     * all access modes are dropped.</li>
     * </ul>
     *
     * <h2><a id="secmgr"></a>Security manager interactions</h2>
     * Although bytecode instructions can only refer to classes in
     * a related class loader, this API can search for methods in any
     * class, as long as a reference to its {@code Class} object is
     * available.  Such cross-loader references are also possible with the
     * Core Reflection API, and are impossible to bytecode instructions
     * such as {@code invokestatic} or {@code getfield}.
     * There is a {@linkplain java.lang.SecurityManager security manager API}
     * to allow applications to check such cross-loader references.
     * These checks apply to both the {@code MethodHandles.Lookup} API
     * and the Core Reflection API
     * (as found on {@link java.lang.Class Class}).
     * <p>
     * If a security manager is present, member and class lookups are subject to
     * additional checks.
     * From one to three calls are made to the security manager.
     * Any of these calls can refuse access by throwing a
     * {@link java.lang.SecurityException SecurityException}.
     * Define {@code smgr} as the security manager,
     * {@code lookc} as the lookup class of the current lookup object,
     * {@code refc} as the containing class in which the member
     * is being sought, and {@code defc} as the class in which the
     * member is actually defined.
     * (If a class or other type is being accessed,
     * the {@code refc} and {@code defc} values are the class itself.)
     * The value {@code lookc} is defined as <em>not present</em>
     * if the current lookup object does not have
     * {@linkplain #hasFullPrivilegeAccess() full privilege access}.
     * The calls are made according to the following rules:
     * <ul>
     * <li><b>Step 1:</b>
     *     If {@code lookc} is not present, or if its class loader is not
     *     the same as or an ancestor of the class loader of {@code refc},
     *     then {@link SecurityManager#checkPackageAccess
     *     smgr.checkPackageAccess(refcPkg)} is called,
     *     where {@code refcPkg} is the package of {@code refc}.
     * <li><b>Step 2a:</b>
     *     If the retrieved member is not public and
     *     {@code lookc} is not present, then
     *     {@link SecurityManager#checkPermission smgr.checkPermission}
     *     with {@code RuntimePermission("accessDeclaredMembers")} is called.
     * <li><b>Step 2b:</b>
     *     If the retrieved class has a {@code null} class loader,
     *     and {@code lookc} is not present, then
     *     {@link SecurityManager#checkPermission smgr.checkPermission}
     *     with {@code RuntimePermission("getClassLoader")} is called.
     * <li><b>Step 3:</b>
     *     If the retrieved member is not public,
     *     and if {@code lookc} is not present,
     *     and if {@code defc} and {@code refc} are different,
     *     then {@link SecurityManager#checkPackageAccess
     *     smgr.checkPackageAccess(defcPkg)} is called,
     *     where {@code defcPkg} is the package of {@code defc}.
     * </ul>
     * Security checks are performed after other access checks have passed.
     * Therefore, the above rules presuppose a member or class that is public,
     * or else that is being accessed from a lookup class that has
     * rights to access the member or class.
     * <p>
     * If a security manager is present and the current lookup object does not have
     * {@linkplain #hasFullPrivilegeAccess() full privilege access}, then
     * {@link #defineClass(byte[]) defineClass},
     * {@link #defineHiddenClass(byte[], boolean, ClassOption...) defineHiddenClass},
     * {@link #defineHiddenClassWithClassData(byte[], Object, boolean, ClassOption...)
     * defineHiddenClassWithClassData}
     * calls {@link SecurityManager#checkPermission smgr.checkPermission}
     * with {@code RuntimePermission("defineClass")}.
     *
     * <h2><a id="callsens"></a>Caller sensitive methods</h2>
     * A small number of Java methods have a special property called caller sensitivity.
     * A <em>caller-sensitive</em> method can behave differently depending on the
     * identity of its immediate caller.
     * <p>
     * If a method handle for a caller-sensitive method is requested,
     * the general rules for <a href="MethodHandles.Lookup.html#equiv">bytecode behaviors</a> apply,
     * but they take account of the lookup class in a special way.
     * The resulting method handle behaves as if it were called
     * from an instruction contained in the lookup class,
     * so that the caller-sensitive method detects the lookup class.
     * (By contrast, the invoker of the method handle is disregarded.)
     * Thus, in the case of caller-sensitive methods,
     * different lookup classes may give rise to
     * differently behaving method handles.
     * <p>
     * In cases where the lookup object is
     * {@link MethodHandles#publicLookup() publicLookup()},
     * or some other lookup object without the
     * {@linkplain #ORIGINAL original access},
     * the lookup class is disregarded.
     * In such cases, no caller-sensitive method handle can be created,
     * access is forbidden, and the lookup fails with an
     * {@code IllegalAccessException}.
     * <p style="font-size:smaller;">
     * <em>Discussion:</em>
     * For example, the caller-sensitive method
     * {@link java.lang.Class#forName(String) Class.forName(x)}
     * can return varying classes or throw varying exceptions,
     * depending on the class loader of the class that calls it.
     * A public lookup of {@code Class.forName} will fail, because
     * there is no reasonable way to determine its bytecode behavior.
     * <p style="font-size:smaller;">
     * If an application caches method handles for broad sharing,
     * it should use {@code publicLookup()} to create them.
     * If there is a lookup of {@code Class.forName}, it will fail,
     * and the application must take appropriate action in that case.
     * It may be that a later lookup, perhaps during the invocation of a
     * bootstrap method, can incorporate the specific identity
     * of the caller, making the method accessible.
     * <p style="font-size:smaller;">
     * The function {@code MethodHandles.lookup} is caller sensitive
     * so that there can be a secure foundation for lookups.
     * Nearly all other methods in the JSR 292 API rely on lookup
     * objects to check access requests.
     *
     * @revised 9
     */
    public static final
    class Lookup {
        /** The class on behalf of whom the lookup is being performed. */
        private final Class<?> lookupClass;

        /** previous lookup class */
        private final Class<?> prevLookupClass;

        /** The allowed sorts of members which may be looked up (PUBLIC, etc.). */
        private final int allowedModes;

        static {
            Reflection.registerFieldsToFilter(Lookup.class, Set.of("lookupClass", "allowedModes"));
        }

        /** A single-bit mask representing {@code public} access,
         *  which may contribute to the result of {@link #lookupModes lookupModes}.
         *  The value, {@code 0x01}, happens to be the same as the value of the
         *  {@code public} {@linkplain java.lang.reflect.Modifier#PUBLIC modifier bit}.
         *  <p>
         *  A {@code Lookup} with this lookup mode performs cross-module access check
         *  with respect to the {@linkplain #lookupClass() lookup class} and
         *  {@linkplain #previousLookupClass() previous lookup class} if present.
         */
        public static final int PUBLIC = Modifier.PUBLIC;

        /** A single-bit mask representing {@code private} access,
         *  which may contribute to the result of {@link #lookupModes lookupModes}.
         *  The value, {@code 0x02}, happens to be the same as the value of the
         *  {@code private} {@linkplain java.lang.reflect.Modifier#PRIVATE modifier bit}.
         */
        public static final int PRIVATE = Modifier.PRIVATE;

        /** A single-bit mask representing {@code protected} access,
         *  which may contribute to the result of {@link #lookupModes lookupModes}.
         *  The value, {@code 0x04}, happens to be the same as the value of the
         *  {@code protected} {@linkplain java.lang.reflect.Modifier#PROTECTED modifier bit}.
         */
        public static final int PROTECTED = Modifier.PROTECTED;

        /** A single-bit mask representing {@code package} access (default access),
         *  which may contribute to the result of {@link #lookupModes lookupModes}.
         *  The value is {@code 0x08}, which does not correspond meaningfully to
         *  any particular {@linkplain java.lang.reflect.Modifier modifier bit}.
         */
        public static final int PACKAGE = Modifier.STATIC;

        /** A single-bit mask representing {@code module} access,
         *  which may contribute to the result of {@link #lookupModes lookupModes}.
         *  The value is {@code 0x10}, which does not correspond meaningfully to
         *  any particular {@linkplain java.lang.reflect.Modifier modifier bit}.
         *  In conjunction with the {@code PUBLIC} modifier bit, a {@code Lookup}
         *  with this lookup mode can access all public types in the module of the
         *  lookup class and public types in packages exported by other modules
         *  to the module of the lookup class.
         *  <p>
         *  If this lookup mode is set, the {@linkplain #previousLookupClass()
         *  previous lookup class} is always {@code null}.
         *
         *  @since 9
         */
        public static final int MODULE = PACKAGE << 1;

        /** A single-bit mask representing {@code unconditional} access
         *  which may contribute to the result of {@link #lookupModes lookupModes}.
         *  The value is {@code 0x20}, which does not correspond meaningfully to
         *  any particular {@linkplain java.lang.reflect.Modifier modifier bit}.
         *  A {@code Lookup} with this lookup mode assumes {@linkplain
         *  java.lang.Module#canRead(java.lang.Module) readability}.
         *  This lookup mode can access all public members of public types
         *  of all modules when the type is in a package that is {@link
         *  java.lang.Module#isExported(String) exported unconditionally}.
         *
         *  <p>
         *  If this lookup mode is set, the {@linkplain #previousLookupClass()
         *  previous lookup class} is always {@code null}.
         *
         *  @since 9
         *  @see #publicLookup()
         */
        public static final int UNCONDITIONAL = PACKAGE << 2;

        /** A single-bit mask representing {@code original} access
         *  which may contribute to the result of {@link #lookupModes lookupModes}.
         *  The value is {@code 0x40}, which does not correspond meaningfully to
         *  any particular {@linkplain java.lang.reflect.Modifier modifier bit}.
         *
         *  <p>
         *  If this lookup mode is set, the {@code Lookup} object must be
         *  created by the original lookup class by calling
         *  {@link MethodHandles#lookup()} method or by a bootstrap method
         *  invoked by the VM.  The {@code Lookup} object with this lookup
         *  mode has {@linkplain #hasFullPrivilegeAccess() full privilege access}.
         *
         *  @since 16
         */
        public static final int ORIGINAL = PACKAGE << 3;

        private static final int ALL_MODES = (PUBLIC | PRIVATE | PROTECTED | PACKAGE | MODULE | UNCONDITIONAL | ORIGINAL);
        private static final int FULL_POWER_MODES = (ALL_MODES & ~UNCONDITIONAL);   // with original access
        private static final int TRUSTED   = -1;

        /*
         * Adjust PUBLIC => PUBLIC|MODULE|ORIGINAL|UNCONDITIONAL
         * Adjust 0 => PACKAGE
         */
        private static int fixmods(int mods) {
            mods &= (ALL_MODES - PACKAGE - MODULE - ORIGINAL - UNCONDITIONAL);
            if (Modifier.isPublic(mods))
                mods |= UNCONDITIONAL;
            return (mods != 0) ? mods : PACKAGE;
        }

        /** Tells which class is performing the lookup.  It is this class against
         *  which checks are performed for visibility and access permissions.
         *  <p>
         *  If this lookup object has a {@linkplain #previousLookupClass() previous lookup class},
         *  access checks are performed against both the lookup class and the previous lookup class.
         *  <p>
         *  The class implies a maximum level of access permission,
         *  but the permissions may be additionally limited by the bitmask
         *  {@link #lookupModes lookupModes}, which controls whether non-public members
         *  can be accessed.
         *  @return the lookup class, on behalf of which this lookup object finds members
         *  @see <a href="#cross-module-lookup">Cross-module lookups</a>
         */
        public Class<?> lookupClass() {
            return lookupClass;
        }

        /** Reports a lookup class in another module that this lookup object
         * was previously teleported from, or {@code null}.
         * <p>
         * A {@code Lookup} object produced by the factory methods, such as the
         * {@link #lookup() lookup()} and {@link #publicLookup() publicLookup()} method,
         * has {@code null} previous lookup class.
         * A {@code Lookup} object has a non-null previous lookup class
         * when this lookup was teleported from an old lookup class
         * in one module to a new lookup class in another module.
         *
         * @return the lookup class in another module that this lookup object was
         *         previously teleported from, or {@code null}
         * @since 14
         * @see #in(Class)
         * @see MethodHandles#privateLookupIn(Class, Lookup)
         * @see <a href="#cross-module-lookup">Cross-module lookups</a>
         */
        public Class<?> previousLookupClass() {
            return prevLookupClass;
        }

        // This is just for calling out to MethodHandleImpl.
        private Class<?> lookupClassOrNull() {
            return (allowedModes == TRUSTED) ? null : lookupClass;
        }

        /** Tells which access-protection classes of members this lookup object can produce.
         *  The result is a bit-mask of the bits
         *  {@linkplain #PUBLIC PUBLIC (0x01)},
         *  {@linkplain #PRIVATE PRIVATE (0x02)},
         *  {@linkplain #PROTECTED PROTECTED (0x04)},
         *  {@linkplain #PACKAGE PACKAGE (0x08)},
         *  {@linkplain #MODULE MODULE (0x10)},
         *  {@linkplain #UNCONDITIONAL UNCONDITIONAL (0x20)},
         *  and {@linkplain #ORIGINAL ORIGINAL (0x40)}.
         *  <p>
         *  A freshly-created lookup object
         *  on the {@linkplain java.lang.invoke.MethodHandles#lookup() caller's class} has
         *  all possible bits set, except {@code UNCONDITIONAL}.
         *  A lookup object on a new lookup class
         *  {@linkplain java.lang.invoke.MethodHandles.Lookup#in created from a previous lookup object}
         *  may have some mode bits set to zero.
         *  Mode bits can also be
         *  {@linkplain java.lang.invoke.MethodHandles.Lookup#dropLookupMode directly cleared}.
         *  Once cleared, mode bits cannot be restored from the downgraded lookup object.
         *  The purpose of this is to restrict access via the new lookup object,
         *  so that it can access only names which can be reached by the original
         *  lookup object, and also by the new lookup class.
         *  @return the lookup modes, which limit the kinds of access performed by this lookup object
         *  @see #in
         *  @see #dropLookupMode
         *
         *  @revised 9
         */
        public int lookupModes() {
            return allowedModes & ALL_MODES;
        }

        /** Embody the current class (the lookupClass) as a lookup class
         * for method handle creation.
         * Must be called by from a method in this package,
         * which in turn is called by a method not in this package.
         */
        Lookup(Class<?> lookupClass) {
            this(lookupClass, null, FULL_POWER_MODES);
        }

        private Lookup(Class<?> lookupClass, Class<?> prevLookupClass, int allowedModes) {
            assert prevLookupClass == null || ((allowedModes & MODULE) == 0
                    && prevLookupClass.getModule() != lookupClass.getModule());
            assert !lookupClass.isArray() && !lookupClass.isPrimitive();
            this.lookupClass = lookupClass;
            this.prevLookupClass = prevLookupClass;
            this.allowedModes = allowedModes;
        }

        private static Lookup newLookup(Class<?> lookupClass, Class<?> prevLookupClass, int allowedModes) {
            // make sure we haven't accidentally picked up a privileged class:
            checkUnprivilegedlookupClass(lookupClass);
            return new Lookup(lookupClass, prevLookupClass, allowedModes);
        }

        /**
         * Creates a lookup on the specified new lookup class.
         * The resulting object will report the specified
         * class as its own {@link #lookupClass() lookupClass}.
         *
         * <p>
         * However, the resulting {@code Lookup} object is guaranteed
         * to have no more access capabilities than the original.
         * In particular, access capabilities can be lost as follows:<ul>
         * <li>If the new lookup class is different from the old lookup class,
         * i.e. {@link #ORIGINAL ORIGINAL} access is lost.
         * <li>If the new lookup class is in a different module from the old one,
         * i.e. {@link #MODULE MODULE} access is lost.
         * <li>If the new lookup class is in a different package
         * than the old one, protected and default (package) members will not be accessible,
         * i.e. {@link #PROTECTED PROTECTED} and {@link #PACKAGE PACKAGE} access are lost.
         * <li>If the new lookup class is not within the same package member
         * as the old one, private members will not be accessible, and protected members
         * will not be accessible by virtue of inheritance,
         * i.e. {@link #PRIVATE PRIVATE} access is lost.
         * (Protected members may continue to be accessible because of package sharing.)
         * <li>If the new lookup class is not
         * {@linkplain #accessClass(Class) accessible} to this lookup,
         * then no members, not even public members, will be accessible
         * i.e. all access modes are lost.
         * <li>If the new lookup class, the old lookup class and the previous lookup class
         * are all in different modules i.e. teleporting to a third module,
         * all access modes are lost.
         * </ul>
         * <p>
         * The new previous lookup class is chosen as follows:
         * <ul>
         * <li>If the new lookup object has {@link #UNCONDITIONAL UNCONDITIONAL} bit,
         * the new previous lookup class is {@code null}.
         * <li>If the new lookup class is in the same module as the old lookup class,
         * the new previous lookup class is the old previous lookup class.
         * <li>If the new lookup class is in a different module from the old lookup class,
         * the new previous lookup class is the old lookup class.
         *</ul>
         * <p>
         * The resulting lookup's capabilities for loading classes
         * (used during {@link #findClass} invocations)
         * are determined by the lookup class' loader,
         * which may change due to this operation.
         *
         * @param requestedLookupClass the desired lookup class for the new lookup object
         * @return a lookup object which reports the desired lookup class, or the same object
         * if there is no change
         * @throws IllegalArgumentException if {@code requestedLookupClass} is a primitive type or void or array class
         * @throws NullPointerException if the argument is null
         *
         * @revised 9
         * @see #accessClass(Class)
         * @see <a href="#cross-module-lookup">Cross-module lookups</a>
         */
        public Lookup in(Class<?> requestedLookupClass) {
            Objects.requireNonNull(requestedLookupClass);
            if (requestedLookupClass.isPrimitive())
                throw new IllegalArgumentException(requestedLookupClass + " is a primitive class");
            if (requestedLookupClass.isArray())
                throw new IllegalArgumentException(requestedLookupClass + " is an array class");

            if (allowedModes == TRUSTED)  // IMPL_LOOKUP can make any lookup at all
                return new Lookup(requestedLookupClass, null, FULL_POWER_MODES);
            if (requestedLookupClass == this.lookupClass)
                return this;  // keep same capabilities
            int newModes = (allowedModes & FULL_POWER_MODES) & ~ORIGINAL;
            Module fromModule = this.lookupClass.getModule();
            Module targetModule = requestedLookupClass.getModule();
            Class<?> plc = this.previousLookupClass();
            if ((this.allowedModes & UNCONDITIONAL) != 0) {
                assert plc == null;
                newModes = UNCONDITIONAL;
            } else if (fromModule != targetModule) {
                if (plc != null && !VerifyAccess.isSameModule(plc, requestedLookupClass)) {
                    // allow hopping back and forth between fromModule and plc's module
                    // but not the third module
                    newModes = 0;
                }
                // drop MODULE access
                newModes &= ~(MODULE|PACKAGE|PRIVATE|PROTECTED);
                // teleport from this lookup class
                plc = this.lookupClass;
            }
            if ((newModes & PACKAGE) != 0
                && !VerifyAccess.isSamePackage(this.lookupClass, requestedLookupClass)) {
                newModes &= ~(PACKAGE|PRIVATE|PROTECTED);
            }
            // Allow nestmate lookups to be created without special privilege:
            if ((newModes & PRIVATE) != 0
                    && !VerifyAccess.isSamePackageMember(this.lookupClass, requestedLookupClass)) {
                newModes &= ~(PRIVATE|PROTECTED);
            }
            if ((newModes & (PUBLIC|UNCONDITIONAL)) != 0
                && !VerifyAccess.isClassAccessible(requestedLookupClass, this.lookupClass, this.prevLookupClass, allowedModes)) {
                // The requested class it not accessible from the lookup class.
                // No permissions.
                newModes = 0;
            }
            return newLookup(requestedLookupClass, plc, newModes);
        }

        /**
         * Creates a lookup on the same lookup class which this lookup object
         * finds members, but with a lookup mode that has lost the given lookup mode.
         * The lookup mode to drop is one of {@link #PUBLIC PUBLIC}, {@link #MODULE
         * MODULE}, {@link #PACKAGE PACKAGE}, {@link #PROTECTED PROTECTED},
         * {@link #PRIVATE PRIVATE}, {@link #ORIGINAL ORIGINAL}, or
         * {@link #UNCONDITIONAL UNCONDITIONAL}.
         *
         * <p> If this lookup is a {@linkplain MethodHandles#publicLookup() public lookup},
         * this lookup has {@code UNCONDITIONAL} mode set and it has no other mode set.
         * When dropping {@code UNCONDITIONAL} on a public lookup then the resulting
         * lookup has no access.
         *
         * <p> If this lookup is not a public lookup, then the following applies
         * regardless of its {@linkplain #lookupModes() lookup modes}.
         * {@link #PROTECTED PROTECTED} and {@link #ORIGINAL ORIGINAL} are always
         * dropped and so the resulting lookup mode will never have these access
         * capabilities. When dropping {@code PACKAGE}
         * then the resulting lookup will not have {@code PACKAGE} or {@code PRIVATE}
         * access. When dropping {@code MODULE} then the resulting lookup will not
         * have {@code MODULE}, {@code PACKAGE}, or {@code PRIVATE} access.
         * When dropping {@code PUBLIC} then the resulting lookup has no access.
         *
         * @apiNote
         * A lookup with {@code PACKAGE} but not {@code PRIVATE} mode can safely
         * delegate non-public access within the package of the lookup class without
         * conferring  <a href="MethodHandles.Lookup.html#privacc">private access</a>.
         * A lookup with {@code MODULE} but not
         * {@code PACKAGE} mode can safely delegate {@code PUBLIC} access within
         * the module of the lookup class without conferring package access.
         * A lookup with a {@linkplain #previousLookupClass() previous lookup class}
         * (and {@code PUBLIC} but not {@code MODULE} mode) can safely delegate access
         * to public classes accessible to both the module of the lookup class
         * and the module of the previous lookup class.
         *
         * @param modeToDrop the lookup mode to drop
         * @return a lookup object which lacks the indicated mode, or the same object if there is no change
         * @throws IllegalArgumentException if {@code modeToDrop} is not one of {@code PUBLIC},
         * {@code MODULE}, {@code PACKAGE}, {@code PROTECTED}, {@code PRIVATE}, {@code ORIGINAL}
         * or {@code UNCONDITIONAL}
         * @see MethodHandles#privateLookupIn
         * @since 9
         */
        public Lookup dropLookupMode(int modeToDrop) {
            int oldModes = lookupModes();
            int newModes = oldModes & ~(modeToDrop | PROTECTED | ORIGINAL);
            switch (modeToDrop) {
                case PUBLIC: newModes &= ~(FULL_POWER_MODES); break;
                case MODULE: newModes &= ~(PACKAGE | PRIVATE); break;
                case PACKAGE: newModes &= ~(PRIVATE); break;
                case PROTECTED:
                case PRIVATE:
                case ORIGINAL:
                case UNCONDITIONAL: break;
                default: throw new IllegalArgumentException(modeToDrop + " is not a valid mode to drop");
            }
            if (newModes == oldModes) return this;  // return self if no change
            return newLookup(lookupClass(), previousLookupClass(), newModes);
        }

        /**
         * Creates and links a class or interface from {@code bytes}
         * with the same class loader and in the same runtime package and
         * {@linkplain java.security.ProtectionDomain protection domain} as this lookup's
         * {@linkplain #lookupClass() lookup class} as if calling
         * {@link ClassLoader#defineClass(String,byte[],int,int,ProtectionDomain)
         * ClassLoader::defineClass}.
         *
         * <p> The {@linkplain #lookupModes() lookup modes} for this lookup must include
         * {@link #PACKAGE PACKAGE} access as default (package) members will be
         * accessible to the class. The {@code PACKAGE} lookup mode serves to authenticate
         * that the lookup object was created by a caller in the runtime package (or derived
         * from a lookup originally created by suitably privileged code to a target class in
         * the runtime package). </p>
         *
         * <p> The {@code bytes} parameter is the class bytes of a valid class file (as defined
         * by the <em>The Java Virtual Machine Specification</em>) with a class name in the
         * same package as the lookup class. </p>
         *
         * <p> This method does not run the class initializer. The class initializer may
         * run at a later time, as detailed in section 12.4 of the <em>The Java Language
         * Specification</em>. </p>
         *
         * <p> If there is a security manager and this lookup does not have {@linkplain
         * #hasFullPrivilegeAccess() full privilege access}, its {@code checkPermission} method
         * is first called to check {@code RuntimePermission("defineClass")}. </p>
         *
         * @param bytes the class bytes
         * @return the {@code Class} object for the class
         * @throws IllegalAccessException if this lookup does not have {@code PACKAGE} access
         * @throws ClassFormatError if {@code bytes} is not a {@code ClassFile} structure
         * @throws IllegalArgumentException if {@code bytes} denotes a class in a different package
         * than the lookup class or {@code bytes} is not a class or interface
         * ({@code ACC_MODULE} flag is set in the value of the {@code access_flags} item)
         * @throws VerifyError if the newly created class cannot be verified
         * @throws LinkageError if the newly created class cannot be linked for any other reason
         * @throws SecurityException if a security manager is present and it
         *                           <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if {@code bytes} is {@code null}
         * @since 9
         * @see Lookup#privateLookupIn
         * @see Lookup#dropLookupMode
         * @see ClassLoader#defineClass(String,byte[],int,int,ProtectionDomain)
         */
        public Class<?> defineClass(byte[] bytes) throws IllegalAccessException {
            ensureDefineClassPermission();
            if ((lookupModes() & PACKAGE) == 0)
                throw new IllegalAccessException("Lookup does not have PACKAGE access");
            return makeClassDefiner(bytes.clone()).defineClass(false);
        }

        private void ensureDefineClassPermission() {
            if (allowedModes == TRUSTED)  return;

            if (!hasFullPrivilegeAccess()) {
                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (sm != null)
                    sm.checkPermission(new RuntimePermission("defineClass"));
            }
        }

        /**
         * The set of class options that specify whether a hidden class created by
         * {@link Lookup#defineHiddenClass(byte[], boolean, ClassOption...)
         * Lookup::defineHiddenClass} method is dynamically added as a new member
         * to the nest of a lookup class and/or whether a hidden class has
         * a strong relationship with the class loader marked as its defining loader.
         *
         * @since 15
         */
        public enum ClassOption {
            /**
             * Specifies that a hidden class be added to {@linkplain Class#getNestHost nest}
             * of a lookup class as a nestmate.
             *
             * <p> A hidden nestmate class has access to the private members of all
             * classes and interfaces in the same nest.
             *
             * @see Class#getNestHost()
             */
            NESTMATE(NESTMATE_CLASS),

            /**
             * Specifies that a hidden class has a <em>strong</em>
             * relationship with the class loader marked as its defining loader,
             * as a normal class or interface has with its own defining loader.
             * This means that the hidden class may be unloaded if and only if
             * its defining loader is not reachable and thus may be reclaimed
             * by a garbage collector (JLS 12.7).
             *
             * <p> By default, a hidden class or interface may be unloaded
             * even if the class loader that is marked as its defining loader is
             * <a href="../ref/package-summary.html#reachability">reachable</a>.

             *
             * @jls 12.7 Unloading of Classes and Interfaces
             */
            STRONG(STRONG_LOADER_LINK);

            /* the flag value is used by VM at define class time */
            private final int flag;
            ClassOption(int flag) {
                this.flag = flag;
            }

            static int optionsToFlag(Set<ClassOption> options) {
                int flags = 0;
                for (ClassOption cp : options) {
                    flags |= cp.flag;
                }
                return flags;
            }
        }

        /**
         * Creates a <em>hidden</em> class or interface from {@code bytes},
         * returning a {@code Lookup} on the newly created class or interface.
         *
         * <p> Ordinarily, a class or interface {@code C} is created by a class loader,
         * which either defines {@code C} directly or delegates to another class loader.
         * A class loader defines {@code C} directly by invoking
         * {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)
         * ClassLoader::defineClass}, which causes the Java Virtual Machine
         * to derive {@code C} from a purported representation in {@code class} file format.
         * In situations where use of a class loader is undesirable, a class or interface
         * {@code C} can be created by this method instead. This method is capable of
         * defining {@code C}, and thereby creating it, without invoking
         * {@code ClassLoader::defineClass}.
         * Instead, this method defines {@code C} as if by arranging for
         * the Java Virtual Machine to derive a nonarray class or interface {@code C}
         * from a purported representation in {@code class} file format
         * using the following rules:
         *
         * <ol>
         * <li> The {@linkplain #lookupModes() lookup modes} for this {@code Lookup}
         * must include {@linkplain #hasFullPrivilegeAccess() full privilege} access.
         * This level of access is needed to create {@code C} in the module
         * of the lookup class of this {@code Lookup}.</li>
         *
         * <li> The purported representation in {@code bytes} must be a {@code ClassFile}
         * structure (JVMS {@jvms 4.1}) of a supported major and minor version.
         * The major and minor version may differ from the {@code class} file version
         * of the lookup class of this {@code Lookup}.</li>
         *
         * <li> The value of {@code this_class} must be a valid index in the
         * {@code constant_pool} table, and the entry at that index must be a valid
         * {@code CONSTANT_Class_info} structure. Let {@code N} be the binary name
         * encoded in internal form that is specified by this structure. {@code N} must
         * denote a class or interface in the same package as the lookup class.</li>
         *
         * <li> Let {@code CN} be the string {@code N + "." + <suffix>},
         * where {@code <suffix>} is an unqualified name.
         *
         * <p> Let {@code newBytes} be the {@code ClassFile} structure given by
         * {@code bytes} with an additional entry in the {@code constant_pool} table,
         * indicating a {@code CONSTANT_Utf8_info} structure for {@code CN}, and
         * where the {@code CONSTANT_Class_info} structure indicated by {@code this_class}
         * refers to the new {@code CONSTANT_Utf8_info} structure.
         *
         * <p> Let {@code L} be the defining class loader of the lookup class of this {@code Lookup}.
         *
         * <p> {@code C} is derived with name {@code CN}, class loader {@code L}, and
         * purported representation {@code newBytes} as if by the rules of JVMS {@jvms 5.3.5},
         * with the following adjustments:
         * <ul>
         * <li> The constant indicated by {@code this_class} is permitted to specify a name
         * that includes a single {@code "."} character, even though this is not a valid
         * binary class or interface name in internal form.</li>
         *
         * <li> The Java Virtual Machine marks {@code L} as the defining class loader of {@code C},
         * but no class loader is recorded as an initiating class loader of {@code C}.</li>
         *
         * <li> {@code C} is considered to have the same runtime
         * {@linkplain Class#getPackage() package}, {@linkplain Class#getModule() module}
         * and {@linkplain java.security.ProtectionDomain protection domain}
         * as the lookup class of this {@code Lookup}.
         * <li> Let {@code GN} be the binary name obtained by taking {@code N}
         * (a binary name encoded in internal form) and replacing ASCII forward slashes with
         * ASCII periods. For the instance of {@link java.lang.Class} representing {@code C}:
         * <ul>
         * <li> {@link Class#getName()} returns the string {@code GN + "/" + <suffix>},
         *      even though this is not a valid binary class or interface name.</li>
         * <li> {@link Class#descriptorString()} returns the string
         *      {@code "L" + N + "." + <suffix> + ";"},
         *      even though this is not a valid type descriptor name.</li>
         * <li> {@link Class#describeConstable()} returns an empty optional as {@code C}
         *      cannot be described in {@linkplain java.lang.constant.ClassDesc nominal form}.</li>
         * </ul>
         * </ul>
         * </li>
         * </ol>
         *
         * <p> After {@code C} is derived, it is linked by the Java Virtual Machine.
         * Linkage occurs as specified in JVMS {@jvms 5.4.3}, with the following adjustments:
         * <ul>
         * <li> During verification, whenever it is necessary to load the class named
         * {@code CN}, the attempt succeeds, producing class {@code C}. No request is
         * made of any class loader.</li>
         *
         * <li> On any attempt to resolve the entry in the run-time constant pool indicated
         * by {@code this_class}, the symbolic reference is considered to be resolved to
         * {@code C} and resolution always succeeds immediately.</li>
         * </ul>
         *
         * <p> If the {@code initialize} parameter is {@code true},
         * then {@code C} is initialized by the Java Virtual Machine.
         *
         * <p> The newly created class or interface {@code C} serves as the
         * {@linkplain #lookupClass() lookup class} of the {@code Lookup} object
         * returned by this method. {@code C} is <em>hidden</em> in the sense that
         * no other class or interface can refer to {@code C} via a constant pool entry.
         * That is, a hidden class or interface cannot be named as a supertype, a field type,
         * a method parameter type, or a method return type by any other class.
         * This is because a hidden class or interface does not have a binary name, so
         * there is no internal form available to record in any class's constant pool.
         * A hidden class or interface is not discoverable by {@link Class#forName(String, boolean, ClassLoader)},
         * {@link ClassLoader#loadClass(String, boolean)}, or {@link #findClass(String)}, and
         * is not {@linkplain java.lang.instrument.Instrumentation#isModifiableClass(Class)
         * modifiable} by Java agents or tool agents using the <a href="{@docRoot}/../specs/jvmti.html">
         * JVM Tool Interface</a>.
         *
         * <p> A class or interface created by
         * {@linkplain ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)
         * a class loader} has a strong relationship with that class loader.
         * That is, every {@code Class} object contains a reference to the {@code ClassLoader}
         * that {@linkplain Class#getClassLoader() defined it}.
         * This means that a class created by a class loader may be unloaded if and
         * only if its defining loader is not reachable and thus may be reclaimed
         * by a garbage collector (JLS 12.7).
         *
         * By default, however, a hidden class or interface may be unloaded even if
         * the class loader that is marked as its defining loader is
         * <a href="../ref/package-summary.html#reachability">reachable</a>.
         * This behavior is useful when a hidden class or interface serves multiple
         * classes defined by arbitrary class loaders.  In other cases, a hidden
         * class or interface may be linked to a single class (or a small number of classes)
         * with the same defining loader as the hidden class or interface.
         * In such cases, where the hidden class or interface must be coterminous
         * with a normal class or interface, the {@link ClassOption#STRONG STRONG}
         * option may be passed in {@code options}.
         * This arranges for a hidden class to have the same strong relationship
         * with the class loader marked as its defining loader,
         * as a normal class or interface has with its own defining loader.
         *
         * If {@code STRONG} is not used, then the invoker of {@code defineHiddenClass}
         * may still prevent a hidden class or interface from being
         * unloaded by ensuring that the {@code Class} object is reachable.
         *
         * <p> The unloading characteristics are set for each hidden class when it is
         * defined, and cannot be changed later.  An advantage of allowing hidden classes
         * to be unloaded independently of the class loader marked as their defining loader
         * is that a very large number of hidden classes may be created by an application.
         * In contrast, if {@code STRONG} is used, then the JVM may run out of memory,
         * just as if normal classes were created by class loaders.
         *
         * <p> Classes and interfaces in a nest are allowed to have mutual access to
         * their private members.  The nest relationship is determined by
         * the {@code NestHost} attribute (JVMS {@jvms 4.7.28}) and
         * the {@code NestMembers} attribute (JVMS {@jvms 4.7.29}) in a {@code class} file.
         * By default, a hidden class belongs to a nest consisting only of itself
         * because a hidden class has no binary name.
         * The {@link ClassOption#NESTMATE NESTMATE} option can be passed in {@code options}
         * to create a hidden class or interface {@code C} as a member of a nest.
         * The nest to which {@code C} belongs is not based on any {@code NestHost} attribute
         * in the {@code ClassFile} structure from which {@code C} was derived.
         * Instead, the following rules determine the nest host of {@code C}:
         * <ul>
         * <li>If the nest host of the lookup class of this {@code Lookup} has previously
         *     been determined, then let {@code H} be the nest host of the lookup class.
         *     Otherwise, the nest host of the lookup class is determined using the
         *     algorithm in JVMS {@jvms 5.4.4}, yielding {@code H}.</li>
         * <li>The nest host of {@code C} is determined to be {@code H},
         *     the nest host of the lookup class.</li>
         * </ul>
         *
         * <p> A hidden class or interface may be serializable, but this requires a custom
         * serialization mechanism in order to ensure that instances are properly serialized
         * and deserialized. The default serialization mechanism supports only classes and
         * interfaces that are discoverable by their class name.
         *
         * @param bytes the bytes that make up the class data,
         * in the format of a valid {@code class} file as defined by
         * <cite>The Java Virtual Machine Specification</cite>.
         * @param initialize if {@code true} the class will be initialized.
         * @param options {@linkplain ClassOption class options}
         * @return the {@code Lookup} object on the hidden class,
         * with {@linkplain #ORIGINAL original} and
         * {@linkplain Lookup#hasFullPrivilegeAccess() full privilege} access
         *
         * @throws IllegalAccessException if this {@code Lookup} does not have
         * {@linkplain #hasFullPrivilegeAccess() full privilege} access
         * @throws SecurityException if a security manager is present and it
         * <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws ClassFormatError if {@code bytes} is not a {@code ClassFile} structure
         * @throws UnsupportedClassVersionError if {@code bytes} is not of a supported major or minor version
         * @throws IllegalArgumentException if {@code bytes} denotes a class in a different package
         * than the lookup class or {@code bytes} is not a class or interface
         * ({@code ACC_MODULE} flag is set in the value of the {@code access_flags} item)
         * @throws IncompatibleClassChangeError if the class or interface named as
         * the direct superclass of {@code C} is in fact an interface, or if any of the classes
         * or interfaces named as direct superinterfaces of {@code C} are not in fact interfaces
         * @throws ClassCircularityError if any of the superclasses or superinterfaces of
         * {@code C} is {@code C} itself
         * @throws VerifyError if the newly created class cannot be verified
         * @throws LinkageError if the newly created class cannot be linked for any other reason
         * @throws NullPointerException if any parameter is {@code null}
         *
         * @since 15
         * @see Class#isHidden()
         * @jvms 4.2.1 Binary Class and Interface Names
         * @jvms 4.2.2 Unqualified Names
         * @jvms 4.7.28 The {@code NestHost} Attribute
         * @jvms 4.7.29 The {@code NestMembers} Attribute
         * @jvms 5.4.3.1 Class and Interface Resolution
         * @jvms 5.4.4 Access Control
         * @jvms 5.3.5 Deriving a {@code Class} from a {@code class} File Representation
         * @jvms 5.4 Linking
         * @jvms 5.5 Initialization
         * @jls 12.7 Unloading of Classes and Interfaces
         */
        public Lookup defineHiddenClass(byte[] bytes, boolean initialize, ClassOption... options)
                throws IllegalAccessException
        {
            Objects.requireNonNull(bytes);
            Objects.requireNonNull(options);

            ensureDefineClassPermission();
            if (!hasFullPrivilegeAccess()) {
                throw new IllegalAccessException(this + " does not have full privilege access");
            }

            return makeHiddenClassDefiner(bytes.clone(), Set.of(options), false).defineClassAsLookup(initialize);
        }

        /**
         * Creates a <em>hidden</em> class or interface from {@code bytes} with associated
         * {@linkplain MethodHandles#classData(Lookup, String, Class) class data},
         * returning a {@code Lookup} on the newly created class or interface.
         *
         * <p> This method is equivalent to calling
         * {@link #defineHiddenClass(byte[], boolean, ClassOption...) defineHiddenClass(bytes, initialize, options)}
         * as if the hidden class is injected with a private static final <i>unnamed</i>
         * field which is initialized with the given {@code classData} at
         * the first instruction of the class initializer.
         * The newly created class is linked by the Java Virtual Machine.
         *
         * <p> The {@link MethodHandles#classData(Lookup, String, Class) MethodHandles::classData}
         * and {@link MethodHandles#classDataAt(Lookup, String, Class, int) MethodHandles::classDataAt}
         * methods can be used to retrieve the {@code classData}.
         *
         * @apiNote
         * A framework can create a hidden class with class data with one or more
         * objects and load the class data as dynamically-computed constant(s)
         * via a bootstrap method.  {@link MethodHandles#classData(Lookup, String, Class)
         * Class data} is accessible only to the lookup object created by the newly
         * defined hidden class but inaccessible to other members in the same nest
         * (unlike private static fields that are accessible to nestmates).
         * Care should be taken w.r.t. mutability for example when passing
         * an array or other mutable structure through the class data.
         * Changing any value stored in the class data at runtime may lead to
         * unpredictable behavior.
         * If the class data is a {@code List}, it is good practice to make it
         * unmodifiable for example via {@link List#of List::of}.
         *
         * @param bytes     the class bytes
         * @param classData pre-initialized class data
         * @param initialize if {@code true} the class will be initialized.
         * @param options   {@linkplain ClassOption class options}
         * @return the {@code Lookup} object on the hidden class,
         * with {@linkplain #ORIGINAL original} and
         * {@linkplain Lookup#hasFullPrivilegeAccess() full privilege} access
         *
         * @throws IllegalAccessException if this {@code Lookup} does not have
         * {@linkplain #hasFullPrivilegeAccess() full privilege} access
         * @throws SecurityException if a security manager is present and it
         * <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws ClassFormatError if {@code bytes} is not a {@code ClassFile} structure
         * @throws UnsupportedClassVersionError if {@code bytes} is not of a supported major or minor version
         * @throws IllegalArgumentException if {@code bytes} denotes a class in a different package
         * than the lookup class or {@code bytes} is not a class or interface
         * ({@code ACC_MODULE} flag is set in the value of the {@code access_flags} item)
         * @throws IncompatibleClassChangeError if the class or interface named as
         * the direct superclass of {@code C} is in fact an interface, or if any of the classes
         * or interfaces named as direct superinterfaces of {@code C} are not in fact interfaces
         * @throws ClassCircularityError if any of the superclasses or superinterfaces of
         * {@code C} is {@code C} itself
         * @throws VerifyError if the newly created class cannot be verified
         * @throws LinkageError if the newly created class cannot be linked for any other reason
         * @throws NullPointerException if any parameter is {@code null}
         *
         * @since 16
         * @see Lookup#defineHiddenClass(byte[], boolean, ClassOption...)
         * @see Class#isHidden()
         * @see MethodHandles#classData(Lookup, String, Class)
         * @see MethodHandles#classDataAt(Lookup, String, Class, int)
         * @jvms 4.2.1 Binary Class and Interface Names
         * @jvms 4.2.2 Unqualified Names
         * @jvms 4.7.28 The {@code NestHost} Attribute
         * @jvms 4.7.29 The {@code NestMembers} Attribute
         * @jvms 5.4.3.1 Class and Interface Resolution
         * @jvms 5.4.4 Access Control
         * @jvms 5.3.5 Deriving a {@code Class} from a {@code class} File Representation
         * @jvms 5.4 Linking
         * @jvms 5.5 Initialization
         * @jls 12.7 Unloading of Classes and Interface
         */
        public Lookup defineHiddenClassWithClassData(byte[] bytes, Object classData, boolean initialize, ClassOption... options)
                throws IllegalAccessException
        {
            Objects.requireNonNull(bytes);
            Objects.requireNonNull(classData);
            Objects.requireNonNull(options);

            ensureDefineClassPermission();
            if (!hasFullPrivilegeAccess()) {
                throw new IllegalAccessException(this + " does not have full privilege access");
            }

            return makeHiddenClassDefiner(bytes.clone(), Set.of(options), false)
                       .defineClassAsLookup(initialize, classData);
        }

        static class ClassFile {
            final String name;
            final int accessFlags;
            final byte[] bytes;
            ClassFile(String name, int accessFlags, byte[] bytes) {
                this.name = name;
                this.accessFlags = accessFlags;
                this.bytes = bytes;
            }

            static ClassFile newInstanceNoCheck(String name, byte[] bytes) {
                return new ClassFile(name, 0, bytes);
            }

            /**
             * This method checks the class file version and the structure of `this_class`.
             * and checks if the bytes is a class or interface (ACC_MODULE flag not set)
             * that is in the named package.
             *
             * @throws IllegalArgumentException if ACC_MODULE flag is set in access flags
             * or the class is not in the given package name.
             */
            static ClassFile newInstance(byte[] bytes, String pkgName) {
                int magic = readInt(bytes, 0);
                if (magic != 0xCAFEBABE) {
                    throw new ClassFormatError("Incompatible magic value: " + magic);
                }
                int minor = readUnsignedShort(bytes, 4);
                int major = readUnsignedShort(bytes, 6);
                if (!VM.isSupportedClassFileVersion(major, minor)) {
                    throw new UnsupportedClassVersionError("Unsupported class file version " + major + "." + minor);
                }

                String name;
                int accessFlags;
                try {
                    ClassReader reader = new ClassReader(bytes);
                    // ClassReader::getClassName does not check if `this_class` is CONSTANT_Class_info
                    // workaround to read `this_class` using readConst and validate the value
                    int thisClass = reader.readUnsignedShort(reader.header + 2);
                    Object constant = reader.readConst(thisClass, new char[reader.getMaxStringLength()]);
                    if (!(constant instanceof Type type)) {
                        throw new ClassFormatError("this_class item: #" + thisClass + " not a CONSTANT_Class_info");
                    }
                    if (!type.getDescriptor().startsWith("L")) {
                        throw new ClassFormatError("this_class item: #" + thisClass + " not a CONSTANT_Class_info");
                    }
                    name = type.getClassName();
                    accessFlags = reader.readUnsignedShort(reader.header);
                } catch (RuntimeException e) {
                    // ASM exceptions are poorly specified
                    ClassFormatError cfe = new ClassFormatError();
                    cfe.initCause(e);
                    throw cfe;
                }

                // must be a class or interface
                if ((accessFlags & Opcodes.ACC_MODULE) != 0) {
                    throw newIllegalArgumentException("Not a class or interface: ACC_MODULE flag is set");
                }

                // check if it's in the named package
                int index = name.lastIndexOf('.');
                String pn = (index == -1) ? "" : name.substring(0, index);
                if (!pn.equals(pkgName)) {
                    throw newIllegalArgumentException(name + " not in same package as lookup class");
                }

                return new ClassFile(name, accessFlags, bytes);
            }

            private static int readInt(byte[] bytes, int offset) {
                if ((offset+4) > bytes.length) {
                    throw new ClassFormatError("Invalid ClassFile structure");
                }
                return ((bytes[offset] & 0xFF) << 24)
                        | ((bytes[offset + 1] & 0xFF) << 16)
                        | ((bytes[offset + 2] & 0xFF) << 8)
                        | (bytes[offset + 3] & 0xFF);
            }

            private static int readUnsignedShort(byte[] bytes, int offset) {
                if ((offset+2) > bytes.length) {
                    throw new ClassFormatError("Invalid ClassFile structure");
                }
                return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
            }
        }

        /*
         * Returns a ClassDefiner that creates a {@code Class} object of a normal class
         * from the given bytes.
         *
         * Caller should make a defensive copy of the arguments if needed
         * before calling this factory method.
         *
         * @throws IllegalArgumentException if {@code bytes} is not a class or interface or
         * {@bytes} denotes a class in a different package than the lookup class
         */
        private ClassDefiner makeClassDefiner(byte[] bytes) {
            ClassFile cf = ClassFile.newInstance(bytes, lookupClass().getPackageName());
            return new ClassDefiner(this, cf, STRONG_LOADER_LINK);
        }

        /**
         * Returns a ClassDefiner that creates a {@code Class} object of a hidden class
         * from the given bytes.  The name must be in the same package as the lookup class.
         *
         * Caller should make a defensive copy of the arguments if needed
         * before calling this factory method.
         *
         * @param bytes   class bytes
         * @return ClassDefiner that defines a hidden class of the given bytes.
         *
         * @throws IllegalArgumentException if {@code bytes} is not a class or interface or
         * {@bytes} denotes a class in a different package than the lookup class
         */
        ClassDefiner makeHiddenClassDefiner(byte[] bytes) {
            ClassFile cf = ClassFile.newInstance(bytes, lookupClass().getPackageName());
            return makeHiddenClassDefiner(cf, Set.of(), false);
        }

        /**
         * Returns a ClassDefiner that creates a {@code Class} object of a hidden class
         * from the given bytes and options.
         * The name must be in the same package as the lookup class.
         *
         * Caller should make a defensive copy of the arguments if needed
         * before calling this factory method.
         *
         * @param bytes   class bytes
         * @param options class options
         * @param accessVmAnnotations true to give the hidden class access to VM annotations
         * @return ClassDefiner that defines a hidden class of the given bytes and options
         *
         * @throws IllegalArgumentException if {@code bytes} is not a class or interface or
         * {@bytes} denotes a class in a different package than the lookup class
         */
        ClassDefiner makeHiddenClassDefiner(byte[] bytes,
                                            Set<ClassOption> options,
                                            boolean accessVmAnnotations) {
            ClassFile cf = ClassFile.newInstance(bytes, lookupClass().getPackageName());
            return makeHiddenClassDefiner(cf, options, accessVmAnnotations);
        }

        /**
         * Returns a ClassDefiner that creates a {@code Class} object of a hidden class
         * from the given bytes.  No package name check on the given name.
         *
         * @param name    fully-qualified name that specifies the prefix of the hidden class
         * @param bytes   class bytes
         * @return ClassDefiner that defines a hidden class of the given bytes.
         */
        ClassDefiner makeHiddenClassDefiner(String name, byte[] bytes) {
            // skip name and access flags validation
            return makeHiddenClassDefiner(ClassFile.newInstanceNoCheck(name, bytes), Set.of(), false);
        }

        /**
         * Returns a ClassDefiner that creates a {@code Class} object of a hidden class
         * from the given class file and options.
         *
         * @param cf ClassFile
         * @param options class options
         * @param accessVmAnnotations true to give the hidden class access to VM annotations
         */
        private ClassDefiner makeHiddenClassDefiner(ClassFile cf,
                                                    Set<ClassOption> options,
                                                    boolean accessVmAnnotations) {
            int flags = HIDDEN_CLASS | ClassOption.optionsToFlag(options);
            if (accessVmAnnotations | VM.isSystemDomainLoader(lookupClass.getClassLoader())) {
                // jdk.internal.vm.annotations are permitted for classes
                // defined to boot loader and platform loader
                flags |= ACCESS_VM_ANNOTATIONS;
            }

            return new ClassDefiner(this, cf, flags);
        }

        static class ClassDefiner {
            private final Lookup lookup;
            private final String name;
            private final byte[] bytes;
            private final int classFlags;

            private ClassDefiner(Lookup lookup, ClassFile cf, int flags) {
                assert ((flags & HIDDEN_CLASS) != 0 || (flags & STRONG_LOADER_LINK) == STRONG_LOADER_LINK);
                this.lookup = lookup;
                this.bytes = cf.bytes;
                this.name = cf.name;
                this.classFlags = flags;
            }

            String className() {
                return name;
            }

            Class<?> defineClass(boolean initialize) {
                return defineClass(initialize, null);
            }

            Lookup defineClassAsLookup(boolean initialize) {
                Class<?> c = defineClass(initialize, null);
                return new Lookup(c, null, FULL_POWER_MODES);
            }

            /**
             * Defines the class of the given bytes and the given classData.
             * If {@code initialize} parameter is true, then the class will be initialized.
             *
             * @param initialize true if the class to be initialized
             * @param classData classData or null
             * @return the class
             *
             * @throws LinkageError linkage error
             */
            Class<?> defineClass(boolean initialize, Object classData) {
                Class<?> lookupClass = lookup.lookupClass();
                ClassLoader loader = lookupClass.getClassLoader();
                ProtectionDomain pd = (loader != null) ? lookup.lookupClassProtectionDomain() : null;
                Class<?> c = SharedSecrets.getJavaLangAccess()
                        .defineClass(loader, lookupClass, name, bytes, pd, initialize, classFlags, classData);
                assert !isNestmate() || c.getNestHost() == lookupClass.getNestHost();
                return c;
            }

            Lookup defineClassAsLookup(boolean initialize, Object classData) {
                Class<?> c = defineClass(initialize, classData);
                return new Lookup(c, null, FULL_POWER_MODES);
            }

            private boolean isNestmate() {
                return (classFlags & NESTMATE_CLASS) != 0;
            }
        }

        private ProtectionDomain lookupClassProtectionDomain() {
            ProtectionDomain pd = cachedProtectionDomain;
            if (pd == null) {
                cachedProtectionDomain = pd = SharedSecrets.getJavaLangAccess().protectionDomain(lookupClass);
            }
            return pd;
        }

        // cached protection domain
        private volatile ProtectionDomain cachedProtectionDomain;

        // Make sure outer class is initialized first.
        static { IMPL_NAMES.getClass(); }

        /** Package-private version of lookup which is trusted. */
        static final Lookup IMPL_LOOKUP = new Lookup(Object.class, null, TRUSTED);

        /** Version of lookup which is trusted minimally.
         *  It can only be used to create method handles to publicly accessible
         *  members in packages that are exported unconditionally.
         */
        static final Lookup PUBLIC_LOOKUP = new Lookup(Object.class, null, UNCONDITIONAL);

        private static void checkUnprivilegedlookupClass(Class<?> lookupClass) {
            String name = lookupClass.getName();
            if (name.startsWith("java.lang.invoke."))
                throw newIllegalArgumentException("illegal lookupClass: "+lookupClass);
        }

        /**
         * Displays the name of the class from which lookups are to be made,
         * followed by "/" and the name of the {@linkplain #previousLookupClass()
         * previous lookup class} if present.
         * (The name is the one reported by {@link java.lang.Class#getName() Class.getName}.)
         * If there are restrictions on the access permitted to this lookup,
         * this is indicated by adding a suffix to the class name, consisting
         * of a slash and a keyword.  The keyword represents the strongest
         * allowed access, and is chosen as follows:
         * <ul>
         * <li>If no access is allowed, the suffix is "/noaccess".
         * <li>If only unconditional access is allowed, the suffix is "/publicLookup".
         * <li>If only public access to types in exported packages is allowed, the suffix is "/public".
         * <li>If only public and module access are allowed, the suffix is "/module".
         * <li>If public and package access are allowed, the suffix is "/package".
         * <li>If public, package, and private access are allowed, the suffix is "/private".
         * </ul>
         * If none of the above cases apply, it is the case that
         * {@linkplain #hasFullPrivilegeAccess() full privilege access}
         * (public, module, package, private, and protected) is allowed.
         * In this case, no suffix is added.
         * This is true only of an object obtained originally from
         * {@link java.lang.invoke.MethodHandles#lookup MethodHandles.lookup}.
         * Objects created by {@link java.lang.invoke.MethodHandles.Lookup#in Lookup.in}
         * always have restricted access, and will display a suffix.
         * <p>
         * (It may seem strange that protected access should be
         * stronger than private access.  Viewed independently from
         * package access, protected access is the first to be lost,
         * because it requires a direct subclass relationship between
         * caller and callee.)
         * @see #in
         *
         * @revised 9
         */
        @Override
        public String toString() {
            String cname = lookupClass.getName();
            if (prevLookupClass != null)
                cname += "/" + prevLookupClass.getName();
            switch (allowedModes) {
            case 0:  // no privileges
                return cname + "/noaccess";
            case UNCONDITIONAL:
                return cname + "/publicLookup";
            case PUBLIC:
                return cname + "/public";
            case PUBLIC|MODULE:
                return cname + "/module";
            case PUBLIC|PACKAGE:
            case PUBLIC|MODULE|PACKAGE:
                return cname + "/package";
            case PUBLIC|PACKAGE|PRIVATE:
            case PUBLIC|MODULE|PACKAGE|PRIVATE:
                    return cname + "/private";
            case PUBLIC|PACKAGE|PRIVATE|PROTECTED:
            case PUBLIC|MODULE|PACKAGE|PRIVATE|PROTECTED:
            case FULL_POWER_MODES:
                    return cname;
            case TRUSTED:
                return "/trusted";  // internal only; not exported
            default:  // Should not happen, but it's a bitfield...
                cname = cname + "/" + Integer.toHexString(allowedModes);
                assert(false) : cname;
                return cname;
            }
        }

        /**
         * Produces a method handle for a static method.
         * The type of the method handle will be that of the method.
         * (Since static methods do not take receivers, there is no
         * additional receiver argument inserted into the method handle type,
         * as there would be with {@link #findVirtual findVirtual} or {@link #findSpecial findSpecial}.)
         * The method and all its argument types must be accessible to the lookup object.
         * <p>
         * The returned method handle will have
         * {@linkplain MethodHandle#asVarargsCollector variable arity} if and only if
         * the method's variable arity modifier bit ({@code 0x0080}) is set.
         * <p>
         * If the returned method handle is invoked, the method's class will
         * be initialized, if it has not already been initialized.
         * <p><b>Example:</b>
         * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodHandle MH_asList = publicLookup().findStatic(Arrays.class,
  "asList", methodType(List.class, Object[].class));
assertEquals("[x, y]", MH_asList.invoke("x", "y").toString());
         * }</pre></blockquote>
         * @param refc the class from which the method is accessed
         * @param name the name of the method
         * @param type the type of the method
         * @return the desired method handle
         * @throws NoSuchMethodException if the method does not exist
         * @throws IllegalAccessException if access checking fails,
         *                                or if the method is not {@code static},
         *                                or if the method's variable arity modifier bit
         *                                is set and {@code asVarargsCollector} fails
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         */
        public MethodHandle findStatic(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            MemberName method = resolveOrFail(REF_invokeStatic, refc, name, type);
            return getDirectMethod(REF_invokeStatic, refc, method, findBoundCallerLookup(method));
        }

        /**
         * Produces a method handle for a virtual method.
         * The type of the method handle will be that of the method,
         * with the receiver type (usually {@code refc}) prepended.
         * The method and all its argument types must be accessible to the lookup object.
         * <p>
         * When called, the handle will treat the first argument as a receiver
         * and, for non-private methods, dispatch on the receiver's type to determine which method
         * implementation to enter.
         * For private methods the named method in {@code refc} will be invoked on the receiver.
         * (The dispatching action is identical with that performed by an
         * {@code invokevirtual} or {@code invokeinterface} instruction.)
         * <p>
         * The first argument will be of type {@code refc} if the lookup
         * class has full privileges to access the member.  Otherwise
         * the member must be {@code protected} and the first argument
         * will be restricted in type to the lookup class.
         * <p>
         * The returned method handle will have
         * {@linkplain MethodHandle#asVarargsCollector variable arity} if and only if
         * the method's variable arity modifier bit ({@code 0x0080}) is set.
         * <p>
         * Because of the general <a href="MethodHandles.Lookup.html#equiv">equivalence</a> between {@code invokevirtual}
         * instructions and method handles produced by {@code findVirtual},
         * if the class is {@code MethodHandle} and the name string is
         * {@code invokeExact} or {@code invoke}, the resulting
         * method handle is equivalent to one produced by
         * {@link java.lang.invoke.MethodHandles#exactInvoker MethodHandles.exactInvoker} or
         * {@link java.lang.invoke.MethodHandles#invoker MethodHandles.invoker}
         * with the same {@code type} argument.
         * <p>
         * If the class is {@code VarHandle} and the name string corresponds to
         * the name of a signature-polymorphic access mode method, the resulting
         * method handle is equivalent to one produced by
         * {@link java.lang.invoke.MethodHandles#varHandleInvoker} with
         * the access mode corresponding to the name string and with the same
         * {@code type} arguments.
         * <p>
         * <b>Example:</b>
         * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodHandle MH_concat = publicLookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
MethodHandle MH_hashCode = publicLookup().findVirtual(Object.class,
  "hashCode", methodType(int.class));
MethodHandle MH_hashCode_String = publicLookup().findVirtual(String.class,
  "hashCode", methodType(int.class));
assertEquals("xy", (String) MH_concat.invokeExact("x", "y"));
assertEquals("xy".hashCode(), (int) MH_hashCode.invokeExact((Object)"xy"));
assertEquals("xy".hashCode(), (int) MH_hashCode_String.invokeExact("xy"));
// interface method:
MethodHandle MH_subSequence = publicLookup().findVirtual(CharSequence.class,
  "subSequence", methodType(CharSequence.class, int.class, int.class));
assertEquals("def", MH_subSequence.invoke("abcdefghi", 3, 6).toString());
// constructor "internal method" must be accessed differently:
MethodType MT_newString = methodType(void.class); //()V for new String()
try { assertEquals("impossible", lookup()
        .findVirtual(String.class, "<init>", MT_newString));
 } catch (NoSuchMethodException ex) { } // OK
MethodHandle MH_newString = publicLookup()
  .findConstructor(String.class, MT_newString);
assertEquals("", (String) MH_newString.invokeExact());
         * }</pre></blockquote>
         *
         * @param refc the class or interface from which the method is accessed
         * @param name the name of the method
         * @param type the type of the method, with the receiver argument omitted
         * @return the desired method handle
         * @throws NoSuchMethodException if the method does not exist
         * @throws IllegalAccessException if access checking fails,
         *                                or if the method is {@code static},
         *                                or if the method's variable arity modifier bit
         *                                is set and {@code asVarargsCollector} fails
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         */
        public MethodHandle findVirtual(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            if (refc == MethodHandle.class) {
                MethodHandle mh = findVirtualForMH(name, type);
                if (mh != null)  return mh;
            } else if (refc == VarHandle.class) {
                MethodHandle mh = findVirtualForVH(name, type);
                if (mh != null)  return mh;
            }
            byte refKind = (refc.isInterface() ? REF_invokeInterface : REF_invokeVirtual);
            MemberName method = resolveOrFail(refKind, refc, name, type);
            return getDirectMethod(refKind, refc, method, findBoundCallerLookup(method));
        }
        private MethodHandle findVirtualForMH(String name, MethodType type) {
            // these names require special lookups because of the implicit MethodType argument
            if ("invoke".equals(name))
                return invoker(type);
            if ("invokeExact".equals(name))
                return exactInvoker(type);
            assert(!MemberName.isMethodHandleInvokeName(name));
            return null;
        }
        private MethodHandle findVirtualForVH(String name, MethodType type) {
            try {
                return varHandleInvoker(VarHandle.AccessMode.valueFromMethodName(name), type);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * Produces a method handle which creates an object and initializes it, using
         * the constructor of the specified type.
         * The parameter types of the method handle will be those of the constructor,
         * while the return type will be a reference to the constructor's class.
         * The constructor and all its argument types must be accessible to the lookup object.
         * <p>
         * The requested type must have a return type of {@code void}.
         * (This is consistent with the JVM's treatment of constructor type descriptors.)
         * <p>
         * The returned method handle will have
         * {@linkplain MethodHandle#asVarargsCollector variable arity} if and only if
         * the constructor's variable arity modifier bit ({@code 0x0080}) is set.
         * <p>
         * If the returned method handle is invoked, the constructor's class will
         * be initialized, if it has not already been initialized.
         * <p><b>Example:</b>
         * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodHandle MH_newArrayList = publicLookup().findConstructor(
  ArrayList.class, methodType(void.class, Collection.class));
Collection orig = Arrays.asList("x", "y");
Collection copy = (ArrayList) MH_newArrayList.invokeExact(orig);
assert(orig != copy);
assertEquals(orig, copy);
// a variable-arity constructor:
MethodHandle MH_newProcessBuilder = publicLookup().findConstructor(
  ProcessBuilder.class, methodType(void.class, String[].class));
ProcessBuilder pb = (ProcessBuilder)
  MH_newProcessBuilder.invoke("x", "y", "z");
assertEquals("[x, y, z]", pb.command().toString());
         * }</pre></blockquote>
         * @param refc the class or interface from which the method is accessed
         * @param type the type of the method, with the receiver argument omitted, and a void return type
         * @return the desired method handle
         * @throws NoSuchMethodException if the constructor does not exist
         * @throws IllegalAccessException if access checking fails
         *                                or if the method's variable arity modifier bit
         *                                is set and {@code asVarargsCollector} fails
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         */
        public MethodHandle findConstructor(Class<?> refc, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            if (refc.isArray()) {
                throw new NoSuchMethodException("no constructor for array class: " + refc.getName());
            }
            String name = "<init>";
            MemberName ctor = resolveOrFail(REF_newInvokeSpecial, refc, name, type);
            return getDirectConstructor(refc, ctor);
        }

        /**
         * Looks up a class by name from the lookup context defined by this {@code Lookup} object,
         * <a href="MethodHandles.Lookup.html#equiv">as if resolved</a> by an {@code ldc} instruction.
         * Such a resolution, as specified in JVMS 5.4.3.1 section, attempts to locate and load the class,
         * and then determines whether the class is accessible to this lookup object.
         * <p>
         * The lookup context here is determined by the {@linkplain #lookupClass() lookup class},
         * its class loader, and the {@linkplain #lookupModes() lookup modes}.
         *
         * @param targetName the fully qualified name of the class to be looked up.
         * @return the requested class.
         * @throws SecurityException if a security manager is present and it
         *                           <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws LinkageError if the linkage fails
         * @throws ClassNotFoundException if the class cannot be loaded by the lookup class' loader.
         * @throws IllegalAccessException if the class is not accessible, using the allowed access
         * modes.
         * @throws NullPointerException if {@code targetName} is null
         * @since 9
         * @jvms 5.4.3.1 Class and Interface Resolution
         */
        public Class<?> findClass(String targetName) throws ClassNotFoundException, IllegalAccessException {
            Class<?> targetClass = Class.forName(targetName, false, lookupClass.getClassLoader());
            return accessClass(targetClass);
        }

        /**
         * Ensures that {@code targetClass} has been initialized. The class
         * to be initialized must be {@linkplain #accessClass accessible}
         * to this {@code Lookup} object.  This method causes {@code targetClass}
         * to be initialized if it has not been already initialized,
         * as specified in JVMS {@jvms 5.5}.
         *
         * @param targetClass the class to be initialized
         * @return {@code targetClass} that has been initialized
         *
         * @throws  IllegalArgumentException if {@code targetClass} is a primitive type or {@code void}
         *          or array class
         * @throws  IllegalAccessException if {@code targetClass} is not
         *          {@linkplain #accessClass accessible} to this lookup
         * @throws  ExceptionInInitializerError if the class initialization provoked
         *          by this method fails
         * @throws  SecurityException if a security manager is present and it
         *          <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @since 15
         * @jvms 5.5 Initialization
         */
        public Class<?> ensureInitialized(Class<?> targetClass) throws IllegalAccessException {
            if (targetClass.isPrimitive())
                throw new IllegalArgumentException(targetClass + " is a primitive class");
            if (targetClass.isArray())
                throw new IllegalArgumentException(targetClass + " is an array class");

            if (!VerifyAccess.isClassAccessible(targetClass, lookupClass, prevLookupClass, allowedModes)) {
                throw makeAccessException(targetClass);
            }
            checkSecurityManager(targetClass);

            // ensure class initialization
            Unsafe.getUnsafe().ensureClassInitialized(targetClass);
            return targetClass;
        }

        /*
         * Returns IllegalAccessException due to access violation to the given targetClass.
         *
         * This method is called by {@link Lookup#accessClass} and {@link Lookup#ensureInitialized}
         * which verifies access to a class rather a member.
         */
        private IllegalAccessException makeAccessException(Class<?> targetClass) {
            String message = "access violation: "+ targetClass;
            if (this == MethodHandles.publicLookup()) {
                message += ", from public Lookup";
            } else {
                Module m = lookupClass().getModule();
                message += ", from " + lookupClass() + " (" + m + ")";
                if (prevLookupClass != null) {
                    message += ", previous lookup " +
                            prevLookupClass.getName() + " (" + prevLookupClass.getModule() + ")";
                }
            }
            return new IllegalAccessException(message);
        }

        /**
         * Determines if a class can be accessed from the lookup context defined by
         * this {@code Lookup} object. The static initializer of the class is not run.
         * If {@code targetClass} is an array class, {@code targetClass} is accessible
         * if the element type of the array class is accessible.  Otherwise,
         * {@code targetClass} is determined as accessible as follows.
         *
         * <p>
         * If {@code targetClass} is in the same module as the lookup class,
         * the lookup class is {@code LC} in module {@code M1} and
         * the previous lookup class is in module {@code M0} or
         * {@code null} if not present,
         * {@code targetClass} is accessible if and only if one of the following is true:
         * <ul>
         * <li>If this lookup has {@link #PRIVATE} access, {@code targetClass} is
         *     {@code LC} or other class in the same nest of {@code LC}.</li>
         * <li>If this lookup has {@link #PACKAGE} access, {@code targetClass} is
         *     in the same runtime package of {@code LC}.</li>
         * <li>If this lookup has {@link #MODULE} access, {@code targetClass} is
         *     a public type in {@code M1}.</li>
         * <li>If this lookup has {@link #PUBLIC} access, {@code targetClass} is
         *     a public type in a package exported by {@code M1} to at least  {@code M0}
         *     if the previous lookup class is present; otherwise, {@code targetClass}
         *     is a public type in a package exported by {@code M1} unconditionally.</li>
         * </ul>
         *
         * <p>
         * Otherwise, if this lookup has {@link #UNCONDITIONAL} access, this lookup
         * can access public types in all modules when the type is in a package
         * that is exported unconditionally.
         * <p>
         * Otherwise, {@code targetClass} is in a different module from {@code lookupClass},
         * and if this lookup does not have {@code PUBLIC} access, {@code lookupClass}
         * is inaccessible.
         * <p>
         * Otherwise, if this lookup has no {@linkplain #previousLookupClass() previous lookup class},
         * {@code M1} is the module containing {@code lookupClass} and
         * {@code M2} is the module containing {@code targetClass},
         * then {@code targetClass} is accessible if and only if
         * <ul>
         * <li>{@code M1} reads {@code M2}, and
         * <li>{@code targetClass} is public and in a package exported by
         *     {@code M2} at least to {@code M1}.
         * </ul>
         * <p>
         * Otherwise, if this lookup has a {@linkplain #previousLookupClass() previous lookup class},
         * {@code M1} and {@code M2} are as before, and {@code M0} is the module
         * containing the previous lookup class, then {@code targetClass} is accessible
         * if and only if one of the following is true:
         * <ul>
         * <li>{@code targetClass} is in {@code M0} and {@code M1}
         *     {@linkplain Module#reads reads} {@code M0} and the type is
         *     in a package that is exported to at least {@code M1}.
         * <li>{@code targetClass} is in {@code M1} and {@code M0}
         *     {@linkplain Module#reads reads} {@code M1} and the type is
         *     in a package that is exported to at least {@code M0}.
         * <li>{@code targetClass} is in a third module {@code M2} and both {@code M0}
         *     and {@code M1} reads {@code M2} and the type is in a package
         *     that is exported to at least both {@code M0} and {@code M2}.
         * </ul>
         * <p>
         * Otherwise, {@code targetClass} is not accessible.
         *
         * @param targetClass the class to be access-checked
         * @return the class that has been access-checked
         * @throws IllegalAccessException if the class is not accessible from the lookup class
         * and previous lookup class, if present, using the allowed access modes.
         * @throws SecurityException if a security manager is present and it
         *                           <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if {@code targetClass} is {@code null}
         * @since 9
         * @see <a href="#cross-module-lookup">Cross-module lookups</a>
         */
        public Class<?> accessClass(Class<?> targetClass) throws IllegalAccessException {
            if (!isClassAccessible(targetClass)) {
                throw makeAccessException(targetClass);
            }
            checkSecurityManager(targetClass);
            return targetClass;
        }

        /**
         * Produces an early-bound method handle for a virtual method.
         * It will bypass checks for overriding methods on the receiver,
         * <a href="MethodHandles.Lookup.html#equiv">as if called</a> from an {@code invokespecial}
         * instruction from within the explicitly specified {@code specialCaller}.
         * The type of the method handle will be that of the method,
         * with a suitably restricted receiver type prepended.
         * (The receiver type will be {@code specialCaller} or a subtype.)
         * The method and all its argument types must be accessible
         * to the lookup object.
         * <p>
         * Before method resolution,
         * if the explicitly specified caller class is not identical with the
         * lookup class, or if this lookup object does not have
         * <a href="MethodHandles.Lookup.html#privacc">private access</a>
         * privileges, the access fails.
         * <p>
         * The returned method handle will have
         * {@linkplain MethodHandle#asVarargsCollector variable arity} if and only if
         * the method's variable arity modifier bit ({@code 0x0080}) is set.
         * <p style="font-size:smaller;">
         * <em>(Note:  JVM internal methods named {@code "<init>"} are not visible to this API,
         * even though the {@code invokespecial} instruction can refer to them
         * in special circumstances.  Use {@link #findConstructor findConstructor}
         * to access instance initialization methods in a safe manner.)</em>
         * <p><b>Example:</b>
         * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
static class Listie extends ArrayList {
  public String toString() { return "[wee Listie]"; }
  static Lookup lookup() { return MethodHandles.lookup(); }
}
...
// no access to constructor via invokeSpecial:
MethodHandle MH_newListie = Listie.lookup()
  .findConstructor(Listie.class, methodType(void.class));
Listie l = (Listie) MH_newListie.invokeExact();
try { assertEquals("impossible", Listie.lookup().findSpecial(
        Listie.class, "<init>", methodType(void.class), Listie.class));
 } catch (NoSuchMethodException ex) { } // OK
// access to super and self methods via invokeSpecial:
MethodHandle MH_super = Listie.lookup().findSpecial(
  ArrayList.class, "toString" , methodType(String.class), Listie.class);
MethodHandle MH_this = Listie.lookup().findSpecial(
  Listie.class, "toString" , methodType(String.class), Listie.class);
MethodHandle MH_duper = Listie.lookup().findSpecial(
  Object.class, "toString" , methodType(String.class), Listie.class);
assertEquals("[]", (String) MH_super.invokeExact(l));
assertEquals(""+l, (String) MH_this.invokeExact(l));
assertEquals("[]", (String) MH_duper.invokeExact(l)); // ArrayList method
try { assertEquals("inaccessible", Listie.lookup().findSpecial(
        String.class, "toString", methodType(String.class), Listie.class));
 } catch (IllegalAccessException ex) { } // OK
Listie subl = new Listie() { public String toString() { return "[subclass]"; } };
assertEquals(""+l, (String) MH_this.invokeExact(subl)); // Listie method
         * }</pre></blockquote>
         *
         * @param refc the class or interface from which the method is accessed
         * @param name the name of the method (which must not be "&lt;init&gt;")
         * @param type the type of the method, with the receiver argument omitted
         * @param specialCaller the proposed calling class to perform the {@code invokespecial}
         * @return the desired method handle
         * @throws NoSuchMethodException if the method does not exist
         * @throws IllegalAccessException if access checking fails,
         *                                or if the method is {@code static},
         *                                or if the method's variable arity modifier bit
         *                                is set and {@code asVarargsCollector} fails
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         */
        public MethodHandle findSpecial(Class<?> refc, String name, MethodType type,
                                        Class<?> specialCaller) throws NoSuchMethodException, IllegalAccessException {
            checkSpecialCaller(specialCaller, refc);
            Lookup specialLookup = this.in(specialCaller);
            MemberName method = specialLookup.resolveOrFail(REF_invokeSpecial, refc, name, type);
            return specialLookup.getDirectMethod(REF_invokeSpecial, refc, method, findBoundCallerLookup(method));
        }

        /**
         * Produces a method handle giving read access to a non-static field.
         * The type of the method handle will have a return type of the field's
         * value type.
         * The method handle's single argument will be the instance containing
         * the field.
         * Access checking is performed immediately on behalf of the lookup class.
         * @param refc the class or interface from which the method is accessed
         * @param name the field's name
         * @param type the field's type
         * @return a method handle which can load values from the field
         * @throws NoSuchFieldException if the field does not exist
         * @throws IllegalAccessException if access checking fails, or if the field is {@code static}
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         * @see #findVarHandle(Class, String, Class)
         */
        public MethodHandle findGetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName field = resolveOrFail(REF_getField, refc, name, type);
            return getDirectField(REF_getField, refc, field);
        }

        /**
         * Produces a method handle giving write access to a non-static field.
         * The type of the method handle will have a void return type.
         * The method handle will take two arguments, the instance containing
         * the field, and the value to be stored.
         * The second argument will be of the field's value type.
         * Access checking is performed immediately on behalf of the lookup class.
         * @param refc the class or interface from which the method is accessed
         * @param name the field's name
         * @param type the field's type
         * @return a method handle which can store values into the field
         * @throws NoSuchFieldException if the field does not exist
         * @throws IllegalAccessException if access checking fails, or if the field is {@code static}
         *                                or {@code final}
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         * @see #findVarHandle(Class, String, Class)
         */
        public MethodHandle findSetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName field = resolveOrFail(REF_putField, refc, name, type);
            return getDirectField(REF_putField, refc, field);
        }

        /**
         * Produces a VarHandle giving access to a non-static field {@code name}
         * of type {@code type} declared in a class of type {@code recv}.
         * The VarHandle's variable type is {@code type} and it has one
         * coordinate type, {@code recv}.
         * <p>
         * Access checking is performed immediately on behalf of the lookup
         * class.
         * <p>
         * Certain access modes of the returned VarHandle are unsupported under
         * the following conditions:
         * <ul>
         * <li>if the field is declared {@code final}, then the write, atomic
         *     update, numeric atomic update, and bitwise atomic update access
         *     modes are unsupported.
         * <li>if the field type is anything other than {@code byte},
         *     {@code short}, {@code char}, {@code int}, {@code long},
         *     {@code float}, or {@code double} then numeric atomic update
         *     access modes are unsupported.
         * <li>if the field type is anything other than {@code boolean},
         *     {@code byte}, {@code short}, {@code char}, {@code int} or
         *     {@code long} then bitwise atomic update access modes are
         *     unsupported.
         * </ul>
         * <p>
         * If the field is declared {@code volatile} then the returned VarHandle
         * will override access to the field (effectively ignore the
         * {@code volatile} declaration) in accordance to its specified
         * access modes.
         * <p>
         * If the field type is {@code float} or {@code double} then numeric
         * and atomic update access modes compare values using their bitwise
         * representation (see {@link Float#floatToRawIntBits} and
         * {@link Double#doubleToRawLongBits}, respectively).
         * @apiNote
         * Bitwise comparison of {@code float} values or {@code double} values,
         * as performed by the numeric and atomic update access modes, differ
         * from the primitive {@code ==} operator and the {@link Float#equals}
         * and {@link Double#equals} methods, specifically with respect to
         * comparing NaN values or comparing {@code -0.0} with {@code +0.0}.
         * Care should be taken when performing a compare and set or a compare
         * and exchange operation with such values since the operation may
         * unexpectedly fail.
         * There are many possible NaN values that are considered to be
         * {@code NaN} in Java, although no IEEE 754 floating-point operation
         * provided by Java can distinguish between them.  Operation failure can
         * occur if the expected or witness value is a NaN value and it is
         * transformed (perhaps in a platform specific manner) into another NaN
         * value, and thus has a different bitwise representation (see
         * {@link Float#intBitsToFloat} or {@link Double#longBitsToDouble} for more
         * details).
         * The values {@code -0.0} and {@code +0.0} have different bitwise
         * representations but are considered equal when using the primitive
         * {@code ==} operator.  Operation failure can occur if, for example, a
         * numeric algorithm computes an expected value to be say {@code -0.0}
         * and previously computed the witness value to be say {@code +0.0}.
         * @param recv the receiver class, of type {@code R}, that declares the
         * non-static field
         * @param name the field's name
         * @param type the field's type, of type {@code T}
         * @return a VarHandle giving access to non-static fields.
         * @throws NoSuchFieldException if the field does not exist
         * @throws IllegalAccessException if access checking fails, or if the field is {@code static}
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         * @since 9
         */
        public VarHandle findVarHandle(Class<?> recv, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName getField = resolveOrFail(REF_getField, recv, name, type);
            MemberName putField = resolveOrFail(REF_putField, recv, name, type);
            return getFieldVarHandle(REF_getField, REF_putField, recv, getField, putField);
        }

        /**
         * Produces a method handle giving read access to a static field.
         * The type of the method handle will have a return type of the field's
         * value type.
         * The method handle will take no arguments.
         * Access checking is performed immediately on behalf of the lookup class.
         * <p>
         * If the returned method handle is invoked, the field's class will
         * be initialized, if it has not already been initialized.
         * @param refc the class or interface from which the method is accessed
         * @param name the field's name
         * @param type the field's type
         * @return a method handle which can load values from the field
         * @throws NoSuchFieldException if the field does not exist
         * @throws IllegalAccessException if access checking fails, or if the field is not {@code static}
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         */
        public MethodHandle findStaticGetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName field = resolveOrFail(REF_getStatic, refc, name, type);
            return getDirectField(REF_getStatic, refc, field);
        }

        /**
         * Produces a method handle giving write access to a static field.
         * The type of the method handle will have a void return type.
         * The method handle will take a single
         * argument, of the field's value type, the value to be stored.
         * Access checking is performed immediately on behalf of the lookup class.
         * <p>
         * If the returned method handle is invoked, the field's class will
         * be initialized, if it has not already been initialized.
         * @param refc the class or interface from which the method is accessed
         * @param name the field's name
         * @param type the field's type
         * @return a method handle which can store values into the field
         * @throws NoSuchFieldException if the field does not exist
         * @throws IllegalAccessException if access checking fails, or if the field is not {@code static}
         *                                or is {@code final}
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         */
        public MethodHandle findStaticSetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName field = resolveOrFail(REF_putStatic, refc, name, type);
            return getDirectField(REF_putStatic, refc, field);
        }

        /**
         * Produces a VarHandle giving access to a static field {@code name} of
         * type {@code type} declared in a class of type {@code decl}.
         * The VarHandle's variable type is {@code type} and it has no
         * coordinate types.
         * <p>
         * Access checking is performed immediately on behalf of the lookup
         * class.
         * <p>
         * If the returned VarHandle is operated on, the declaring class will be
         * initialized, if it has not already been initialized.
         * <p>
         * Certain access modes of the returned VarHandle are unsupported under
         * the following conditions:
         * <ul>
         * <li>if the field is declared {@code final}, then the write, atomic
         *     update, numeric atomic update, and bitwise atomic update access
         *     modes are unsupported.
         * <li>if the field type is anything other than {@code byte},
         *     {@code short}, {@code char}, {@code int}, {@code long},
         *     {@code float}, or {@code double}, then numeric atomic update
         *     access modes are unsupported.
         * <li>if the field type is anything other than {@code boolean},
         *     {@code byte}, {@code short}, {@code char}, {@code int} or
         *     {@code long} then bitwise atomic update access modes are
         *     unsupported.
         * </ul>
         * <p>
         * If the field is declared {@code volatile} then the returned VarHandle
         * will override access to the field (effectively ignore the
         * {@code volatile} declaration) in accordance to its specified
         * access modes.
         * <p>
         * If the field type is {@code float} or {@code double} then numeric
         * and atomic update access modes compare values using their bitwise
         * representation (see {@link Float#floatToRawIntBits} and
         * {@link Double#doubleToRawLongBits}, respectively).
         * @apiNote
         * Bitwise comparison of {@code float} values or {@code double} values,
         * as performed by the numeric and atomic update access modes, differ
         * from the primitive {@code ==} operator and the {@link Float#equals}
         * and {@link Double#equals} methods, specifically with respect to
         * comparing NaN values or comparing {@code -0.0} with {@code +0.0}.
         * Care should be taken when performing a compare and set or a compare
         * and exchange operation with such values since the operation may
         * unexpectedly fail.
         * There are many possible NaN values that are considered to be
         * {@code NaN} in Java, although no IEEE 754 floating-point operation
         * provided by Java can distinguish between them.  Operation failure can
         * occur if the expected or witness value is a NaN value and it is
         * transformed (perhaps in a platform specific manner) into another NaN
         * value, and thus has a different bitwise representation (see
         * {@link Float#intBitsToFloat} or {@link Double#longBitsToDouble} for more
         * details).
         * The values {@code -0.0} and {@code +0.0} have different bitwise
         * representations but are considered equal when using the primitive
         * {@code ==} operator.  Operation failure can occur if, for example, a
         * numeric algorithm computes an expected value to be say {@code -0.0}
         * and previously computed the witness value to be say {@code +0.0}.
         * @param decl the class that declares the static field
         * @param name the field's name
         * @param type the field's type, of type {@code T}
         * @return a VarHandle giving access to a static field
         * @throws NoSuchFieldException if the field does not exist
         * @throws IllegalAccessException if access checking fails, or if the field is not {@code static}
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         * @since 9
         */
        public VarHandle findStaticVarHandle(Class<?> decl, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName getField = resolveOrFail(REF_getStatic, decl, name, type);
            MemberName putField = resolveOrFail(REF_putStatic, decl, name, type);
            return getFieldVarHandle(REF_getStatic, REF_putStatic, decl, getField, putField);
        }

        /**
         * Produces an early-bound method handle for a non-static method.
         * The receiver must have a supertype {@code defc} in which a method
         * of the given name and type is accessible to the lookup class.
         * The method and all its argument types must be accessible to the lookup object.
         * The type of the method handle will be that of the method,
         * without any insertion of an additional receiver parameter.
         * The given receiver will be bound into the method handle,
         * so that every call to the method handle will invoke the
         * requested method on the given receiver.
         * <p>
         * The returned method handle will have
         * {@linkplain MethodHandle#asVarargsCollector variable arity} if and only if
         * the method's variable arity modifier bit ({@code 0x0080}) is set
         * <em>and</em> the trailing array argument is not the only argument.
         * (If the trailing array argument is the only argument,
         * the given receiver value will be bound to it.)
         * <p>
         * This is almost equivalent to the following code, with some differences noted below:
         * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodHandle mh0 = lookup().findVirtual(defc, name, type);
MethodHandle mh1 = mh0.bindTo(receiver);
mh1 = mh1.withVarargs(mh0.isVarargsCollector());
return mh1;
         * }</pre></blockquote>
         * where {@code defc} is either {@code receiver.getClass()} or a super
         * type of that class, in which the requested method is accessible
         * to the lookup class.
         * (Unlike {@code bind}, {@code bindTo} does not preserve variable arity.
         * Also, {@code bindTo} may throw a {@code ClassCastException} in instances where {@code bind} would
         * throw an {@code IllegalAccessException}, as in the case where the member is {@code protected} and
         * the receiver is restricted by {@code findVirtual} to the lookup class.)
         * @param receiver the object from which the method is accessed
         * @param name the name of the method
         * @param type the type of the method, with the receiver argument omitted
         * @return the desired method handle
         * @throws NoSuchMethodException if the method does not exist
         * @throws IllegalAccessException if access checking fails
         *                                or if the method's variable arity modifier bit
         *                                is set and {@code asVarargsCollector} fails
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws NullPointerException if any argument is null
         * @see MethodHandle#bindTo
         * @see #findVirtual
         */
        public MethodHandle bind(Object receiver, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            Class<? extends Object> refc = receiver.getClass(); // may get NPE
            MemberName method = resolveOrFail(REF_invokeSpecial, refc, name, type);
            MethodHandle mh = getDirectMethodNoRestrictInvokeSpecial(refc, method, findBoundCallerLookup(method));
            if (!mh.type().leadingReferenceParameter().isAssignableFrom(receiver.getClass())) {
                throw new IllegalAccessException("The restricted defining class " +
                                                 mh.type().leadingReferenceParameter().getName() +
                                                 " is not assignable from receiver class " +
                                                 receiver.getClass().getName());
            }
            return mh.bindArgumentL(0, receiver).setVarargs(method);
        }

        /**
         * Makes a <a href="MethodHandleInfo.html#directmh">direct method handle</a>
         * to <i>m</i>, if the lookup class has permission.
         * If <i>m</i> is non-static, the receiver argument is treated as an initial argument.
         * If <i>m</i> is virtual, overriding is respected on every call.
         * Unlike the Core Reflection API, exceptions are <em>not</em> wrapped.
         * The type of the method handle will be that of the method,
         * with the receiver type prepended (but only if it is non-static).
         * If the method's {@code accessible} flag is not set,
         * access checking is performed immediately on behalf of the lookup class.
         * If <i>m</i> is not public, do not share the resulting handle with untrusted parties.
         * <p>
         * The returned method handle will have
         * {@linkplain MethodHandle#asVarargsCollector variable arity} if and only if
         * the method's variable arity modifier bit ({@code 0x0080}) is set.
         * <p>
         * If <i>m</i> is static, and
         * if the returned method handle is invoked, the method's class will
         * be initialized, if it has not already been initialized.
         * @param m the reflected method
         * @return a method handle which can invoke the reflected method
         * @throws IllegalAccessException if access checking fails
         *                                or if the method's variable arity modifier bit
         *                                is set and {@code asVarargsCollector} fails
         * @throws NullPointerException if the argument is null
         */
        public MethodHandle unreflect(Method m) throws IllegalAccessException {
            if (m.getDeclaringClass() == MethodHandle.class) {
                MethodHandle mh = unreflectForMH(m);
                if (mh != null)  return mh;
            }
            if (m.getDeclaringClass() == VarHandle.class) {
                MethodHandle mh = unreflectForVH(m);
                if (mh != null)  return mh;
            }
            MemberName method = new MemberName(m);
            byte refKind = method.getReferenceKind();
            if (refKind == REF_invokeSpecial)
                refKind = REF_invokeVirtual;
            assert(method.isMethod());
            @SuppressWarnings("deprecation")
            Lookup lookup = m.isAccessible() ? IMPL_LOOKUP : this;
            return lookup.getDirectMethodNoSecurityManager(refKind, method.getDeclaringClass(), method, findBoundCallerLookup(method));
        }
        private MethodHandle unreflectForMH(Method m) {
            // these names require special lookups because they throw UnsupportedOperationException
            if (MemberName.isMethodHandleInvokeName(m.getName()))
                return MethodHandleImpl.fakeMethodHandleInvoke(new MemberName(m));
            return null;
        }
        private MethodHandle unreflectForVH(Method m) {
            // these names require special lookups because they throw UnsupportedOperationException
            if (MemberName.isVarHandleMethodInvokeName(m.getName()))
                return MethodHandleImpl.fakeVarHandleInvoke(new MemberName(m));
            return null;
        }

        /**
         * Produces a method handle for a reflected method.
         * It will bypass checks for overriding methods on the receiver,
         * <a href="MethodHandles.Lookup.html#equiv">as if called</a> from an {@code invokespecial}
         * instruction from within the explicitly specified {@code specialCaller}.
         * The type of the method handle will be that of the method,
         * with a suitably restricted receiver type prepended.
         * (The receiver type will be {@code specialCaller} or a subtype.)
         * If the method's {@code accessible} flag is not set,
         * access checking is performed immediately on behalf of the lookup class,
         * as if {@code invokespecial} instruction were being linked.
         * <p>
         * Before method resolution,
         * if the explicitly specified caller class is not identical with the
         * lookup class, or if this lookup object does not have
         * <a href="MethodHandles.Lookup.html#privacc">private access</a>
         * privileges, the access fails.
         * <p>
         * The returned method handle will have
         * {@linkplain MethodHandle#asVarargsCollector variable arity} if and only if
         * the method's variable arity modifier bit ({@code 0x0080}) is set.
         * @param m the reflected method
         * @param specialCaller the class nominally calling the method
         * @return a method handle which can invoke the reflected method
         * @throws IllegalAccessException if access checking fails,
         *                                or if the method is {@code static},
         *                                or if the method's variable arity modifier bit
         *                                is set and {@code asVarargsCollector} fails
         * @throws NullPointerException if any argument is null
         */
        public MethodHandle unreflectSpecial(Method m, Class<?> specialCaller) throws IllegalAccessException {
            checkSpecialCaller(specialCaller, m.getDeclaringClass());
            Lookup specialLookup = this.in(specialCaller);
            MemberName method = new MemberName(m, true);
            assert(method.isMethod());
            // ignore m.isAccessible:  this is a new kind of access
            return specialLookup.getDirectMethodNoSecurityManager(REF_invokeSpecial, method.getDeclaringClass(), method, findBoundCallerLookup(method));
        }

        /**
         * Produces a method handle for a reflected constructor.
         * The type of the method handle will be that of the constructor,
         * with the return type changed to the declaring class.
         * The method handle will perform a {@code newInstance} operation,
         * creating a new instance of the constructor's class on the
         * arguments passed to the method handle.
         * <p>
         * If the constructor's {@code accessible} flag is not set,
         * access checking is performed immediately on behalf of the lookup class.
         * <p>
         * The returned method handle will have
         * {@linkplain MethodHandle#asVarargsCollector variable arity} if and only if
         * the constructor's variable arity modifier bit ({@code 0x0080}) is set.
         * <p>
         * If the returned method handle is invoked, the constructor's class will
         * be initialized, if it has not already been initialized.
         * @param c the reflected constructor
         * @return a method handle which can invoke the reflected constructor
         * @throws IllegalAccessException if access checking fails
         *                                or if the method's variable arity modifier bit
         *                                is set and {@code asVarargsCollector} fails
         * @throws NullPointerException if the argument is null
         */
        public MethodHandle unreflectConstructor(Constructor<?> c) throws IllegalAccessException {
            MemberName ctor = new MemberName(c);
            assert(ctor.isConstructor());
            @SuppressWarnings("deprecation")
            Lookup lookup = c.isAccessible() ? IMPL_LOOKUP : this;
            return lookup.getDirectConstructorNoSecurityManager(ctor.getDeclaringClass(), ctor);
        }

        /**
         * Produces a method handle giving read access to a reflected field.
         * The type of the method handle will have a return type of the field's
         * value type.
         * If the field is {@code static}, the method handle will take no arguments.
         * Otherwise, its single argument will be the instance containing
         * the field.
         * If the {@code Field} object's {@code accessible} flag is not set,
         * access checking is performed immediately on behalf of the lookup class.
         * <p>
         * If the field is static, and
         * if the returned method handle is invoked, the field's class will
         * be initialized, if it has not already been initialized.
         * @param f the reflected field
         * @return a method handle which can load values from the reflected field
         * @throws IllegalAccessException if access checking fails
         * @throws NullPointerException if the argument is null
         */
        public MethodHandle unreflectGetter(Field f) throws IllegalAccessException {
            return unreflectField(f, false);
        }

        /**
         * Produces a method handle giving write access to a reflected field.
         * The type of the method handle will have a void return type.
         * If the field is {@code static}, the method handle will take a single
         * argument, of the field's value type, the value to be stored.
         * Otherwise, the two arguments will be the instance containing
         * the field, and the value to be stored.
         * If the {@code Field} object's {@code accessible} flag is not set,
         * access checking is performed immediately on behalf of the lookup class.
         * <p>
         * If the field is {@code final}, write access will not be
         * allowed and access checking will fail, except under certain
         * narrow circumstances documented for {@link Field#set Field.set}.
         * A method handle is returned only if a corresponding call to
         * the {@code Field} object's {@code set} method could return
         * normally.  In particular, fields which are both {@code static}
         * and {@code final} may never be set.
         * <p>
         * If the field is {@code static}, and
         * if the returned method handle is invoked, the field's class will
         * be initialized, if it has not already been initialized.
         * @param f the reflected field
         * @return a method handle which can store values into the reflected field
         * @throws IllegalAccessException if access checking fails,
         *         or if the field is {@code final} and write access
         *         is not enabled on the {@code Field} object
         * @throws NullPointerException if the argument is null
         */
        public MethodHandle unreflectSetter(Field f) throws IllegalAccessException {
            return unreflectField(f, true);
        }

        private MethodHandle unreflectField(Field f, boolean isSetter) throws IllegalAccessException {
            MemberName field = new MemberName(f, isSetter);
            if (isSetter && field.isFinal()) {
                if (field.isTrustedFinalField()) {
                    String msg = field.isStatic() ? "static final field has no write access"
                                                  : "final field has no write access";
                    throw field.makeAccessException(msg, this);
                }
            }
            assert(isSetter
                    ? MethodHandleNatives.refKindIsSetter(field.getReferenceKind())
                    : MethodHandleNatives.refKindIsGetter(field.getReferenceKind()));
            @SuppressWarnings("deprecation")
            Lookup lookup = f.isAccessible() ? IMPL_LOOKUP : this;
            return lookup.getDirectFieldNoSecurityManager(field.getReferenceKind(), f.getDeclaringClass(), field);
        }

        /**
         * Produces a VarHandle giving access to a reflected field {@code f}
         * of type {@code T} declared in a class of type {@code R}.
         * The VarHandle's variable type is {@code T}.
         * If the field is non-static the VarHandle has one coordinate type,
         * {@code R}.  Otherwise, the field is static, and the VarHandle has no
         * coordinate types.
         * <p>
         * Access checking is performed immediately on behalf of the lookup
         * class, regardless of the value of the field's {@code accessible}
         * flag.
         * <p>
         * If the field is static, and if the returned VarHandle is operated
         * on, the field's declaring class will be initialized, if it has not
         * already been initialized.
         * <p>
         * Certain access modes of the returned VarHandle are unsupported under
         * the following conditions:
         * <ul>
         * <li>if the field is declared {@code final}, then the write, atomic
         *     update, numeric atomic update, and bitwise atomic update access
         *     modes are unsupported.
         * <li>if the field type is anything other than {@code byte},
         *     {@code short}, {@code char}, {@code int}, {@code long},
         *     {@code float}, or {@code double} then numeric atomic update
         *     access modes are unsupported.
         * <li>if the field type is anything other than {@code boolean},
         *     {@code byte}, {@code short}, {@code char}, {@code int} or
         *     {@code long} then bitwise atomic update access modes are
         *     unsupported.
         * </ul>
         * <p>
         * If the field is declared {@code volatile} then the returned VarHandle
         * will override access to the field (effectively ignore the
         * {@code volatile} declaration) in accordance to its specified
         * access modes.
         * <p>
         * If the field type is {@code float} or {@code double} then numeric
         * and atomic update access modes compare values using their bitwise
         * representation (see {@link Float#floatToRawIntBits} and
         * {@link Double#doubleToRawLongBits}, respectively).
         * @apiNote
         * Bitwise comparison of {@code float} values or {@code double} values,
         * as performed by the numeric and atomic update access modes, differ
         * from the primitive {@code ==} operator and the {@link Float#equals}
         * and {@link Double#equals} methods, specifically with respect to
         * comparing NaN values or comparing {@code -0.0} with {@code +0.0}.
         * Care should be taken when performing a compare and set or a compare
         * and exchange operation with such values since the operation may
         * unexpectedly fail.
         * There are many possible NaN values that are considered to be
         * {@code NaN} in Java, although no IEEE 754 floating-point operation
         * provided by Java can distinguish between them.  Operation failure can
         * occur if the expected or witness value is a NaN value and it is
         * transformed (perhaps in a platform specific manner) into another NaN
         * value, and thus has a different bitwise representation (see
         * {@link Float#intBitsToFloat} or {@link Double#longBitsToDouble} for more
         * details).
         * The values {@code -0.0} and {@code +0.0} have different bitwise
         * representations but are considered equal when using the primitive
         * {@code ==} operator.  Operation failure can occur if, for example, a
         * numeric algorithm computes an expected value to be say {@code -0.0}
         * and previously computed the witness value to be say {@code +0.0}.
         * @param f the reflected field, with a field of type {@code T}, and
         * a declaring class of type {@code R}
         * @return a VarHandle giving access to non-static fields or a static
         * field
         * @throws IllegalAccessException if access checking fails
         * @throws NullPointerException if the argument is null
         * @since 9
         */
        public VarHandle unreflectVarHandle(Field f) throws IllegalAccessException {
            MemberName getField = new MemberName(f, false);
            MemberName putField = new MemberName(f, true);
            return getFieldVarHandleNoSecurityManager(getField.getReferenceKind(), putField.getReferenceKind(),
                                                      f.getDeclaringClass(), getField, putField);
        }

        /**
         * Cracks a <a href="MethodHandleInfo.html#directmh">direct method handle</a>
         * created by this lookup object or a similar one.
         * Security and access checks are performed to ensure that this lookup object
         * is capable of reproducing the target method handle.
         * This means that the cracking may fail if target is a direct method handle
         * but was created by an unrelated lookup object.
         * This can happen if the method handle is <a href="MethodHandles.Lookup.html#callsens">caller sensitive</a>
         * and was created by a lookup object for a different class.
         * @param target a direct method handle to crack into symbolic reference components
         * @return a symbolic reference which can be used to reconstruct this method handle from this lookup object
         * @throws    SecurityException if a security manager is present and it
         *                              <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
         * @throws IllegalArgumentException if the target is not a direct method handle or if access checking fails
         * @throws    NullPointerException if the target is {@code null}
         * @see MethodHandleInfo
         * @since 1.8
         */
        public MethodHandleInfo revealDirect(MethodHandle target) {
            if (!target.isCrackable()) {
                throw newIllegalArgumentException("not a direct method handle");
            }
            MemberName member = target.internalMemberName();
            Class<?> defc = member.getDeclaringClass();
            byte refKind = member.getReferenceKind();
            assert(MethodHandleNatives.refKindIsValid(refKind));
            if (refKind == REF_invokeSpecial && !target.isInvokeSpecial())
                // Devirtualized method invocation is usually formally virtual.
                // To avoid creating extra MemberName objects for this common case,
                // we encode this extra degree of freedom using MH.isInvokeSpecial.
                refKind = REF_invokeVirtual;
            if (refKind == REF_invokeVirtual && defc.isInterface())
                // Symbolic reference is through interface but resolves to Object method (toString, etc.)
                refKind = REF_invokeInterface;
            // Check SM permissions and member access before cracking.
            try {
                checkAccess(refKind, defc, member);
                checkSecurityManager(defc, member);
            } catch (IllegalAccessException ex) {
                throw new IllegalArgumentException(ex);
            }
            if (allowedModes != TRUSTED && member.isCallerSensitive()) {
                Class<?> callerClass = target.internalCallerClass();
                if ((lookupModes() & ORIGINAL) == 0 || callerClass != lookupClass())
                    throw new IllegalArgumentException("method handle is caller sensitive: "+callerClass);
            }
            // Produce the handle to the results.
            return new InfoFromMemberName(this, member, refKind);
        }

        /// Helper methods, all package-private.

        MemberName resolveOrFail(byte refKind, Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            checkSymbolicClass(refc);  // do this before attempting to resolve
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
            return IMPL_NAMES.resolveOrFail(refKind, new MemberName(refc, name, type, refKind), lookupClassOrNull(), allowedModes,
                                            NoSuchFieldException.class);
        }

        MemberName resolveOrFail(byte refKind, Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            checkSymbolicClass(refc);  // do this before attempting to resolve
            Objects.requireNonNull(type);
            checkMethodName(refKind, name);  // implicit null-check of name
            return IMPL_NAMES.resolveOrFail(refKind, new MemberName(refc, name, type, refKind), lookupClassOrNull(), allowedModes,
                                            NoSuchMethodException.class);
        }

        MemberName resolveOrFail(byte refKind, MemberName member) throws ReflectiveOperationException {
            checkSymbolicClass(member.getDeclaringClass());  // do this before attempting to resolve
            Objects.requireNonNull(member.getName());
            Objects.requireNonNull(member.getType());
            return IMPL_NAMES.resolveOrFail(refKind, member, lookupClassOrNull(), allowedModes,
                                            ReflectiveOperationException.class);
        }

        MemberName resolveOrNull(byte refKind, MemberName member) {
            // do this before attempting to resolve
            if (!isClassAccessible(member.getDeclaringClass())) {
                return null;
            }
            Objects.requireNonNull(member.getName());
            Objects.requireNonNull(member.getType());
            return IMPL_NAMES.resolveOrNull(refKind, member, lookupClassOrNull(), allowedModes);
        }

        MemberName resolveOrNull(byte refKind, Class<?> refc, String name, MethodType type) {
            // do this before attempting to resolve
            if (!isClassAccessible(refc)) {
                return null;
            }
            Objects.requireNonNull(type);
            // implicit null-check of name
            if (name.startsWith("<") && refKind != REF_newInvokeSpecial) {
                return null;
            }
            return IMPL_NAMES.resolveOrNull(refKind, new MemberName(refc, name, type, refKind), lookupClassOrNull(), allowedModes);
        }

        void checkSymbolicClass(Class<?> refc) throws IllegalAccessException {
            if (!isClassAccessible(refc)) {
                throw new MemberName(refc).makeAccessException("symbolic reference class is not accessible", this);
            }
        }

        boolean isClassAccessible(Class<?> refc) {
            Objects.requireNonNull(refc);
            Class<?> caller = lookupClassOrNull();
            Class<?> type = refc;
            while (type.isArray()) {
                type = type.getComponentType();
            }
            return caller == null || VerifyAccess.isClassAccessible(type, caller, prevLookupClass, allowedModes);
        }

        /** Check name for an illegal leading "&lt;" character. */
        void checkMethodName(byte refKind, String name) throws NoSuchMethodException {
            if (name.startsWith("<") && refKind != REF_newInvokeSpecial)
                throw new NoSuchMethodException("illegal method name: "+name);
        }

        /**
         * Find my trustable caller class if m is a caller sensitive method.
         * If this lookup object has original full privilege access, then the caller class is the lookupClass.
         * Otherwise, if m is caller-sensitive, throw IllegalAccessException.
         */
        Lookup findBoundCallerLookup(MemberName m) throws IllegalAccessException {
            if (MethodHandleNatives.isCallerSensitive(m) && (lookupModes() & ORIGINAL) == 0) {
                // Only lookups with full privilege access are allowed to resolve caller-sensitive methods
                throw new IllegalAccessException("Attempt to lookup caller-sensitive method using restricted lookup object");
            }
            return this;
        }

        /**
         * Returns {@code true} if this lookup has {@code PRIVATE} and {@code MODULE} access.
         * @return {@code true} if this lookup has {@code PRIVATE} and {@code MODULE} access.
         *
         * @deprecated This method was originally designed to test {@code PRIVATE} access
         * that implies full privilege access but {@code MODULE} access has since become
         * independent of {@code PRIVATE} access.  It is recommended to call
         * {@link #hasFullPrivilegeAccess()} instead.
         * @since 9
         */
        @Deprecated(since="14")
        public boolean hasPrivateAccess() {
            return hasFullPrivilegeAccess();
        }

        /**
         * Returns {@code true} if this lookup has <em>full privilege access</em>,
         * i.e. {@code PRIVATE} and {@code MODULE} access.
         * A {@code Lookup} object must have full privilege access in order to
         * access all members that are allowed to the
         * {@linkplain #lookupClass() lookup class}.
         *
         * @return {@code true} if this lookup has full privilege access.
         * @since 14
         * @see <a href="MethodHandles.Lookup.html#privacc">private and module access</a>
         */
        public boolean hasFullPrivilegeAccess() {
            return (allowedModes & (PRIVATE|MODULE)) == (PRIVATE|MODULE);
        }

        /**
         * Perform steps 1 and 2b <a href="MethodHandles.Lookup.html#secmgr">access checks</a>
         * for ensureInitialzed, findClass or accessClass.
         */
        void checkSecurityManager(Class<?> refc) {
            if (allowedModes == TRUSTED)  return;

            @SuppressWarnings("removal")
            SecurityManager smgr = System.getSecurityManager();
            if (smgr == null)  return;

            // Step 1:
            boolean fullPrivilegeLookup = hasFullPrivilegeAccess();
            if (!fullPrivilegeLookup ||
                !VerifyAccess.classLoaderIsAncestor(lookupClass, refc)) {
                ReflectUtil.checkPackageAccess(refc);
            }

            // Step 2b:
            if (!fullPrivilegeLookup) {
                smgr.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
            }
        }

        /**
         * Perform steps 1, 2a and 3 <a href="MethodHandles.Lookup.html#secmgr">access checks</a>.
         * Determines a trustable caller class to compare with refc, the symbolic reference class.
         * If this lookup object has full privilege access except original access,
         * then the caller class is the lookupClass.
         *
         * Lookup object created by {@link MethodHandles#privateLookupIn(Class, Lookup)}
         * from the same module skips the security permission check.
         */
        void checkSecurityManager(Class<?> refc, MemberName m) {
            Objects.requireNonNull(refc);
            Objects.requireNonNull(m);

            if (allowedModes == TRUSTED)  return;

            @SuppressWarnings("removal")
            SecurityManager smgr = System.getSecurityManager();
            if (smgr == null)  return;

            // Step 1:
            boolean fullPrivilegeLookup = hasFullPrivilegeAccess();
            if (!fullPrivilegeLookup ||
                !VerifyAccess.classLoaderIsAncestor(lookupClass, refc)) {
                ReflectUtil.checkPackageAccess(refc);
            }

            // Step 2a:
            if (m.isPublic()) return;
            if (!fullPrivilegeLookup) {
                smgr.checkPermission(SecurityConstants.CHECK_MEMBER_ACCESS_PERMISSION);
            }

            // Step 3:
            Class<?> defc = m.getDeclaringClass();
            if (!fullPrivilegeLookup && defc != refc) {
                ReflectUtil.checkPackageAccess(defc);
            }
        }

        void checkMethod(byte refKind, Class<?> refc, MemberName m) throws IllegalAccessException {
            boolean wantStatic = (refKind == REF_invokeStatic);
            String message;
            if (m.isConstructor())
                message = "expected a method, not a constructor";
            else if (!m.isMethod())
                message = "expected a method";
            else if (wantStatic != m.isStatic())
                message = wantStatic ? "expected a static method" : "expected a non-static method";
            else
                { checkAccess(refKind, refc, m); return; }
            throw m.makeAccessException(message, this);
        }

        void checkField(byte refKind, Class<?> refc, MemberName m) throws IllegalAccessException {
            boolean wantStatic = !MethodHandleNatives.refKindHasReceiver(refKind);
            String message;
            if (wantStatic != m.isStatic())
                message = wantStatic ? "expected a static field" : "expected a non-static field";
            else
                { checkAccess(refKind, refc, m); return; }
            throw m.makeAccessException(message, this);
        }

        /** Check public/protected/private bits on the symbolic reference class and its member. */
        void checkAccess(byte refKind, Class<?> refc, MemberName m) throws IllegalAccessException {
            assert(m.referenceKindIsConsistentWith(refKind) &&
                   MethodHandleNatives.refKindIsValid(refKind) &&
                   (MethodHandleNatives.refKindIsField(refKind) == m.isField()));
            int allowedModes = this.allowedModes;
            if (allowedModes == TRUSTED)  return;
            int mods = m.getModifiers();
            if (Modifier.isProtected(mods) &&
                    refKind == REF_invokeVirtual &&
                    m.getDeclaringClass() == Object.class &&
                    m.getName().equals("clone") &&
                    refc.isArray()) {
                // The JVM does this hack also.
                // (See ClassVerifier::verify_invoke_instructions
                // and LinkResolver::check_method_accessability.)
                // Because the JVM does not allow separate methods on array types,
                // there is no separate method for int[].clone.
                // All arrays simply inherit Object.clone.
                // But for access checking logic, we make Object.clone
                // (normally protected) appear to be public.
                // Later on, when the DirectMethodHandle is created,
                // its leading argument will be restricted to the
                // requested array type.
                // N.B. The return type is not adjusted, because
                // that is *not* the bytecode behavior.
                mods ^= Modifier.PROTECTED | Modifier.PUBLIC;
            }
            if (Modifier.isProtected(mods) && refKind == REF_newInvokeSpecial) {
                // cannot "new" a protected ctor in a different package
                mods ^= Modifier.PROTECTED;
            }
            if (Modifier.isFinal(mods) &&
                    MethodHandleNatives.refKindIsSetter(refKind))
                throw m.makeAccessException("unexpected set of a final field", this);
            int requestedModes = fixmods(mods);  // adjust 0 => PACKAGE
            if ((requestedModes & allowedModes) != 0) {
                if (VerifyAccess.isMemberAccessible(refc, m.getDeclaringClass(),
                                                    mods, lookupClass(), previousLookupClass(), allowedModes))
                    return;
            } else {
                // Protected members can also be checked as if they were package-private.
                if ((requestedModes & PROTECTED) != 0 && (allowedModes & PACKAGE) != 0
                        && VerifyAccess.isSamePackage(m.getDeclaringClass(), lookupClass()))
                    return;
            }
            throw m.makeAccessException(accessFailedMessage(refc, m), this);
        }

        String accessFailedMessage(Class<?> refc, MemberName m) {
            Class<?> defc = m.getDeclaringClass();
            int mods = m.getModifiers();
            // check the class first:
            boolean classOK = (Modifier.isPublic(defc.getModifiers()) &&
                               (defc == refc ||
                                Modifier.isPublic(refc.getModifiers())));
            if (!classOK && (allowedModes & PACKAGE) != 0) {
                // ignore previous lookup class to check if default package access
                classOK = (VerifyAccess.isClassAccessible(defc, lookupClass(), null, FULL_POWER_MODES) &&
                           (defc == refc ||
                            VerifyAccess.isClassAccessible(refc, lookupClass(), null, FULL_POWER_MODES)));
            }
            if (!classOK)
                return "class is not public";
            if (Modifier.isPublic(mods))
                return "access to public member failed";  // (how?, module not readable?)
            if (Modifier.isPrivate(mods))
                return "member is private";
            if (Modifier.isProtected(mods))
                return "member is protected";
            return "member is private to package";
        }

        private void checkSpecialCaller(Class<?> specialCaller, Class<?> refc) throws IllegalAccessException {
            int allowedModes = this.allowedModes;
            if (allowedModes == TRUSTED)  return;
            if ((lookupModes() & PRIVATE) == 0
                || (specialCaller != lookupClass()
                       // ensure non-abstract methods in superinterfaces can be special-invoked
                    && !(refc != null && refc.isInterface() && refc.isAssignableFrom(specialCaller))))
                throw new MemberName(specialCaller).
                    makeAccessException("no private access for invokespecial", this);
        }

        private boolean restrictProtectedReceiver(MemberName method) {
            // The accessing class only has the right to use a protected member
            // on itself or a subclass.  Enforce that restriction, from JVMS 5.4.4, etc.
            if (!method.isProtected() || method.isStatic()
                || allowedModes == TRUSTED
                || method.getDeclaringClass() == lookupClass()
                || VerifyAccess.isSamePackage(method.getDeclaringClass(), lookupClass()))
                return false;
            return true;
        }
        private MethodHandle restrictReceiver(MemberName method, DirectMethodHandle mh, Class<?> caller) throws IllegalAccessException {
            assert(!method.isStatic());
            // receiver type of mh is too wide; narrow to caller
            if (!method.getDeclaringClass().isAssignableFrom(caller)) {
                throw method.makeAccessException("caller class must be a subclass below the method", caller);
            }
            MethodType rawType = mh.type();
            if (caller.isAssignableFrom(rawType.parameterType(0))) return mh; // no need to restrict; already narrow
            MethodType narrowType = rawType.changeParameterType(0, caller);
            assert(!mh.isVarargsCollector());  // viewAsType will lose varargs-ness
            assert(mh.viewAsTypeChecks(narrowType, true));
            return mh.copyWith(narrowType, mh.form);
        }

        /** Check access and get the requested method. */
        private MethodHandle getDirectMethod(byte refKind, Class<?> refc, MemberName method, Lookup callerLookup) throws IllegalAccessException {
            final boolean doRestrict    = true;
            final boolean checkSecurity = true;
            return getDirectMethodCommon(refKind, refc, method, checkSecurity, doRestrict, callerLookup);
        }
        /** Check access and get the requested method, for invokespecial with no restriction on the application of narrowing rules. */
        private MethodHandle getDirectMethodNoRestrictInvokeSpecial(Class<?> refc, MemberName method, Lookup callerLookup) throws IllegalAccessException {
            final boolean doRestrict    = false;
            final boolean checkSecurity = true;
            return getDirectMethodCommon(REF_invokeSpecial, refc, method, checkSecurity, doRestrict, callerLookup);
        }
        /** Check access and get the requested method, eliding security manager checks. */
        private MethodHandle getDirectMethodNoSecurityManager(byte refKind, Class<?> refc, MemberName method, Lookup callerLookup) throws IllegalAccessException {
            final boolean doRestrict    = true;
            final boolean checkSecurity = false;  // not needed for reflection or for linking CONSTANT_MH constants
            return getDirectMethodCommon(refKind, refc, method, checkSecurity, doRestrict, callerLookup);
        }
        /** Common code for all methods; do not call directly except from immediately above. */
        private MethodHandle getDirectMethodCommon(byte refKind, Class<?> refc, MemberName method,
                                                   boolean checkSecurity,
                                                   boolean doRestrict,
                                                   Lookup boundCaller) throws IllegalAccessException {
            checkMethod(refKind, refc, method);
            // Optionally check with the security manager; this isn't needed for unreflect* calls.
            if (checkSecurity)
                checkSecurityManager(refc, method);
            assert(!method.isMethodHandleInvoke());

            if (refKind == REF_invokeSpecial &&
                refc != lookupClass() &&
                !refc.isInterface() &&
                refc != lookupClass().getSuperclass() &&
                refc.isAssignableFrom(lookupClass())) {
                assert(!method.getName().equals("<init>"));  // not this code path

                // Per JVMS 6.5, desc. of invokespecial instruction:
                // If the method is in a superclass of the LC,
                // and if our original search was above LC.super,
                // repeat the search (symbolic lookup) from LC.super
                // and continue with the direct superclass of that class,
                // and so forth, until a match is found or no further superclasses exist.
                // FIXME: MemberName.resolve should handle this instead.
                Class<?> refcAsSuper = lookupClass();
                MemberName m2;
                do {
                    refcAsSuper = refcAsSuper.getSuperclass();
                    m2 = new MemberName(refcAsSuper,
                                        method.getName(),
                                        method.getMethodType(),
                                        REF_invokeSpecial);
                    m2 = IMPL_NAMES.resolveOrNull(refKind, m2, lookupClassOrNull(), allowedModes);
                } while (m2 == null &&         // no method is found yet
                         refc != refcAsSuper); // search up to refc
                if (m2 == null)  throw new InternalError(method.toString());
                method = m2;
                refc = refcAsSuper;
                // redo basic checks
                checkMethod(refKind, refc, method);
            }
            DirectMethodHandle dmh = DirectMethodHandle.make(refKind, refc, method, lookupClass());
            MethodHandle mh = dmh;
            // Optionally narrow the receiver argument to lookupClass using restrictReceiver.
            if ((doRestrict && refKind == REF_invokeSpecial) ||
                    (MethodHandleNatives.refKindHasReceiver(refKind) && restrictProtectedReceiver(method))) {
                mh = restrictReceiver(method, dmh, lookupClass());
            }
            mh = maybeBindCaller(method, mh, boundCaller);
            mh = mh.setVarargs(method);
            return mh;
        }
        private MethodHandle maybeBindCaller(MemberName method, MethodHandle mh, Lookup boundCaller)
                                             throws IllegalAccessException {
            if (boundCaller.allowedModes == TRUSTED || !MethodHandleNatives.isCallerSensitive(method))
                return mh;

            // boundCaller must have full privilege access.
            // It should have been checked by findBoundCallerLookup. Safe to check this again.
            if ((boundCaller.lookupModes() & ORIGINAL) == 0)
                throw new IllegalAccessException("Attempt to lookup caller-sensitive method using restricted lookup object");

            assert boundCaller.hasFullPrivilegeAccess();

            MethodHandle cbmh = MethodHandleImpl.bindCaller(mh, boundCaller.lookupClass);
            // Note: caller will apply varargs after this step happens.
            return cbmh;
        }

        /** Check access and get the requested field. */
        private MethodHandle getDirectField(byte refKind, Class<?> refc, MemberName field) throws IllegalAccessException {
            final boolean checkSecurity = true;
            return getDirectFieldCommon(refKind, refc, field, checkSecurity);
        }
        /** Check access and get the requested field, eliding security manager checks. */
        private MethodHandle getDirectFieldNoSecurityManager(byte refKind, Class<?> refc, MemberName field) throws IllegalAccessException {
            final boolean checkSecurity = false;  // not needed for reflection or for linking CONSTANT_MH constants
            return getDirectFieldCommon(refKind, refc, field, checkSecurity);
        }
        /** Common code for all fields; do not call directly except from immediately above. */
        private MethodHandle getDirectFieldCommon(byte refKind, Class<?> refc, MemberName field,
                                                  boolean checkSecurity) throws IllegalAccessException {
            checkField(refKind, refc, field);
            // Optionally check with the security manager; this isn't needed for unreflect* calls.
            if (checkSecurity)
                checkSecurityManager(refc, field);
            DirectMethodHandle dmh = DirectMethodHandle.make(refc, field);
            boolean doRestrict = (MethodHandleNatives.refKindHasReceiver(refKind) &&
                                    restrictProtectedReceiver(field));
            if (doRestrict)
                return restrictReceiver(field, dmh, lookupClass());
            return dmh;
        }
        private VarHandle getFieldVarHandle(byte getRefKind, byte putRefKind,
                                            Class<?> refc, MemberName getField, MemberName putField)
                throws IllegalAccessException {
            final boolean checkSecurity = true;
            return getFieldVarHandleCommon(getRefKind, putRefKind, refc, getField, putField, checkSecurity);
        }
        private VarHandle getFieldVarHandleNoSecurityManager(byte getRefKind, byte putRefKind,
                                                             Class<?> refc, MemberName getField, MemberName putField)
                throws IllegalAccessException {
            final boolean checkSecurity = false;
            return getFieldVarHandleCommon(getRefKind, putRefKind, refc, getField, putField, checkSecurity);
        }
        private VarHandle getFieldVarHandleCommon(byte getRefKind, byte putRefKind,
                                                  Class<?> refc, MemberName getField, MemberName putField,
                                                  boolean checkSecurity) throws IllegalAccessException {
            assert getField.isStatic() == putField.isStatic();
            assert getField.isGetter() && putField.isSetter();
            assert MethodHandleNatives.refKindIsStatic(getRefKind) == MethodHandleNatives.refKindIsStatic(putRefKind);
            assert MethodHandleNatives.refKindIsGetter(getRefKind) && MethodHandleNatives.refKindIsSetter(putRefKind);

            checkField(getRefKind, refc, getField);
            if (checkSecurity)
                checkSecurityManager(refc, getField);

            if (!putField.isFinal()) {
                // A VarHandle does not support updates to final fields, any
                // such VarHandle to a final field will be read-only and
                // therefore the following write-based accessibility checks are
                // only required for non-final fields
                checkField(putRefKind, refc, putField);
                if (checkSecurity)
                    checkSecurityManager(refc, putField);
            }

            boolean doRestrict = (MethodHandleNatives.refKindHasReceiver(getRefKind) &&
                                  restrictProtectedReceiver(getField));
            if (doRestrict) {
                assert !getField.isStatic();
                // receiver type of VarHandle is too wide; narrow to caller
                if (!getField.getDeclaringClass().isAssignableFrom(lookupClass())) {
                    throw getField.makeAccessException("caller class must be a subclass below the method", lookupClass());
                }
                refc = lookupClass();
            }
            return VarHandles.makeFieldHandle(getField, refc, getField.getFieldType(),
                                              this.allowedModes == TRUSTED && !getField.isTrustedFinalField());
        }
        /** Check access and get the requested constructor. */
        private MethodHandle getDirectConstructor(Class<?> refc, MemberName ctor) throws IllegalAccessException {
            final boolean checkSecurity = true;
            return getDirectConstructorCommon(refc, ctor, checkSecurity);
        }
        /** Check access and get the requested constructor, eliding security manager checks. */
        private MethodHandle getDirectConstructorNoSecurityManager(Class<?> refc, MemberName ctor) throws IllegalAccessException {
            final boolean checkSecurity = false;  // not needed for reflection or for linking CONSTANT_MH constants
            return getDirectConstructorCommon(refc, ctor, checkSecurity);
        }
        /** Common code for all constructors; do not call directly except from immediately above. */
        private MethodHandle getDirectConstructorCommon(Class<?> refc, MemberName ctor,
                                                  boolean checkSecurity) throws IllegalAccessException {
            assert(ctor.isConstructor());
            checkAccess(REF_newInvokeSpecial, refc, ctor);
            // Optionally check with the security manager; this isn't needed for unreflect* calls.
            if (checkSecurity)
                checkSecurityManager(refc, ctor);
            assert(!MethodHandleNatives.isCallerSensitive(ctor));  // maybeBindCaller not relevant here
            return DirectMethodHandle.make(ctor).setVarargs(ctor);
        }

        /** Hook called from the JVM (via MethodHandleNatives) to link MH constants:
         */
        /*non-public*/
        MethodHandle linkMethodHandleConstant(byte refKind, Class<?> defc, String name, Object type)
                throws ReflectiveOperationException {
            if (!(type instanceof Class || type instanceof MethodType))
                throw new InternalError("unresolved MemberName");
            MemberName member = new MemberName(refKind, defc, name, type);
            MethodHandle mh = LOOKASIDE_TABLE.get(member);
            if (mh != null) {
                checkSymbolicClass(defc);
                return mh;
            }
            if (defc == MethodHandle.class && refKind == REF_invokeVirtual) {
                // Treat MethodHandle.invoke and invokeExact specially.
                mh = findVirtualForMH(member.getName(), member.getMethodType());
                if (mh != null) {
                    return mh;
                }
            } else if (defc == VarHandle.class && refKind == REF_invokeVirtual) {
                // Treat signature-polymorphic methods on VarHandle specially.
                mh = findVirtualForVH(member.getName(), member.getMethodType());
                if (mh != null) {
                    return mh;
                }
            }
            MemberName resolved = resolveOrFail(refKind, member);
            mh = getDirectMethodForConstant(refKind, defc, resolved);
            if (mh instanceof DirectMethodHandle
                    && canBeCached(refKind, defc, resolved)) {
                MemberName key = mh.internalMemberName();
                if (key != null) {
                    key = key.asNormalOriginal();
                }
                if (member.equals(key)) {  // better safe than sorry
                    LOOKASIDE_TABLE.put(key, (DirectMethodHandle) mh);
                }
            }
            return mh;
        }
        private boolean canBeCached(byte refKind, Class<?> defc, MemberName member) {
            if (refKind == REF_invokeSpecial) {
                return false;
            }
            if (!Modifier.isPublic(defc.getModifiers()) ||
                    !Modifier.isPublic(member.getDeclaringClass().getModifiers()) ||
                    !member.isPublic() ||
                    member.isCallerSensitive()) {
                return false;
            }
            ClassLoader loader = defc.getClassLoader();
            if (loader != null) {
                ClassLoader sysl = ClassLoader.getSystemClassLoader();
                boolean found = false;
                while (sysl != null) {
                    if (loader == sysl) { found = true; break; }
                    sysl = sysl.getParent();
                }
                if (!found) {
                    return false;
                }
            }
            try {
                MemberName resolved2 = publicLookup().resolveOrNull(refKind,
                    new MemberName(refKind, defc, member.getName(), member.getType()));
                if (resolved2 == null) {
                    return false;
                }
                checkSecurityManager(defc, resolved2);
            } catch (SecurityException ex) {
                return false;
            }
            return true;
        }
        private MethodHandle getDirectMethodForConstant(byte refKind, Class<?> defc, MemberName member)
                throws ReflectiveOperationException {
            if (MethodHandleNatives.refKindIsField(refKind)) {
                return getDirectFieldNoSecurityManager(refKind, defc, member);
            } else if (MethodHandleNatives.refKindIsMethod(refKind)) {
                return getDirectMethodNoSecurityManager(refKind, defc, member, findBoundCallerLookup(member));
            } else if (refKind == REF_newInvokeSpecial) {
                return getDirectConstructorNoSecurityManager(defc, member);
            }
            // oops
            throw newIllegalArgumentException("bad MethodHandle constant #"+member);
        }

        static ConcurrentHashMap<MemberName, DirectMethodHandle> LOOKASIDE_TABLE = new ConcurrentHashMap<>();
    }

    /**
     * Produces a method handle constructing arrays of a desired type,
     * as if by the {@code anewarray} bytecode.
     * The return type of the method handle will be the array type.
     * The type of its sole argument will be {@code int}, which specifies the size of the array.
     *
     * <p> If the returned method handle is invoked with a negative
     * array size, a {@code NegativeArraySizeException} will be thrown.
     *
     * @param arrayClass an array type
     * @return a method handle which can create arrays of the given type
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalArgumentException if {@code arrayClass} is not an array type
     * @see java.lang.reflect.Array#newInstance(Class, int)
     * @jvms 6.5 {@code anewarray} Instruction
     * @since 9
     */
    public static MethodHandle arrayConstructor(Class<?> arrayClass) throws IllegalArgumentException {
        if (!arrayClass.isArray()) {
            throw newIllegalArgumentException("not an array class: " + arrayClass.getName());
        }
        MethodHandle ani = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_Array_newInstance).
                bindTo(arrayClass.getComponentType());
        return ani.asType(ani.type().changeReturnType(arrayClass));
    }

    /**
     * Produces a method handle returning the length of an array,
     * as if by the {@code arraylength} bytecode.
     * The type of the method handle will have {@code int} as return type,
     * and its sole argument will be the array type.
     *
     * <p> If the returned method handle is invoked with a {@code null}
     * array reference, a {@code NullPointerException} will be thrown.
     *
     * @param arrayClass an array type
     * @return a method handle which can retrieve the length of an array of the given array type
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalArgumentException if arrayClass is not an array type
     * @jvms 6.5 {@code arraylength} Instruction
     * @since 9
     */
    public static MethodHandle arrayLength(Class<?> arrayClass) throws IllegalArgumentException {
        return MethodHandleImpl.makeArrayElementAccessor(arrayClass, MethodHandleImpl.ArrayAccess.LENGTH);
    }

    /**
     * Produces a method handle giving read access to elements of an array,
     * as if by the {@code aaload} bytecode.
     * The type of the method handle will have a return type of the array's
     * element type.  Its first argument will be the array type,
     * and the second will be {@code int}.
     *
     * <p> When the returned method handle is invoked,
     * the array reference and array index are checked.
     * A {@code NullPointerException} will be thrown if the array reference
     * is {@code null} and an {@code ArrayIndexOutOfBoundsException} will be
     * thrown if the index is negative or if it is greater than or equal to
     * the length of the array.
     *
     * @param arrayClass an array type
     * @return a method handle which can load values from the given array type
     * @throws NullPointerException if the argument is null
     * @throws  IllegalArgumentException if arrayClass is not an array type
     * @jvms 6.5 {@code aaload} Instruction
     */
    public static MethodHandle arrayElementGetter(Class<?> arrayClass) throws IllegalArgumentException {
        return MethodHandleImpl.makeArrayElementAccessor(arrayClass, MethodHandleImpl.ArrayAccess.GET);
    }

    /**
     * Produces a method handle giving write access to elements of an array,
     * as if by the {@code astore} bytecode.
     * The type of the method handle will have a void return type.
     * Its last argument will be the array's element type.
     * The first and second arguments will be the array type and int.
     *
     * <p> When the returned method handle is invoked,
     * the array reference and array index are checked.
     * A {@code NullPointerException} will be thrown if the array reference
     * is {@code null} and an {@code ArrayIndexOutOfBoundsException} will be
     * thrown if the index is negative or if it is greater than or equal to
     * the length of the array.
     *
     * @param arrayClass the class of an array
     * @return a method handle which can store values into the array type
     * @throws NullPointerException if the argument is null
     * @throws IllegalArgumentException if arrayClass is not an array type
     * @jvms 6.5 {@code aastore} Instruction
     */
    public static MethodHandle arrayElementSetter(Class<?> arrayClass) throws IllegalArgumentException {
        return MethodHandleImpl.makeArrayElementAccessor(arrayClass, MethodHandleImpl.ArrayAccess.SET);
    }

    /**
     * Produces a VarHandle giving access to elements of an array of type
     * {@code arrayClass}.  The VarHandle's variable type is the component type
     * of {@code arrayClass} and the list of coordinate types is
     * {@code (arrayClass, int)}, where the {@code int} coordinate type
     * corresponds to an argument that is an index into an array.
     * <p>
     * Certain access modes of the returned VarHandle are unsupported under
     * the following conditions:
     * <ul>
     * <li>if the component type is anything other than {@code byte},
     *     {@code short}, {@code char}, {@code int}, {@code long},
     *     {@code float}, or {@code double} then numeric atomic update access
     *     modes are unsupported.
     * <li>if the component type is anything other than {@code boolean},
     *     {@code byte}, {@code short}, {@code char}, {@code int} or
     *     {@code long} then bitwise atomic update access modes are
     *     unsupported.
     * </ul>
     * <p>
     * If the component type is {@code float} or {@code double} then numeric
     * and atomic update access modes compare values using their bitwise
     * representation (see {@link Float#floatToRawIntBits} and
     * {@link Double#doubleToRawLongBits}, respectively).
     *
     * <p> When the returned {@code VarHandle} is invoked,
     * the array reference and array index are checked.
     * A {@code NullPointerException} will be thrown if the array reference
     * is {@code null} and an {@code ArrayIndexOutOfBoundsException} will be
     * thrown if the index is negative or if it is greater than or equal to
     * the length of the array.
     *
     * @apiNote
     * Bitwise comparison of {@code float} values or {@code double} values,
     * as performed by the numeric and atomic update access modes, differ
     * from the primitive {@code ==} operator and the {@link Float#equals}
     * and {@link Double#equals} methods, specifically with respect to
     * comparing NaN values or comparing {@code -0.0} with {@code +0.0}.
     * Care should be taken when performing a compare and set or a compare
     * and exchange operation with such values since the operation may
     * unexpectedly fail.
     * There are many possible NaN values that are considered to be
     * {@code NaN} in Java, although no IEEE 754 floating-point operation
     * provided by Java can distinguish between them.  Operation failure can
     * occur if the expected or witness value is a NaN value and it is
     * transformed (perhaps in a platform specific manner) into another NaN
     * value, and thus has a different bitwise representation (see
     * {@link Float#intBitsToFloat} or {@link Double#longBitsToDouble} for more
     * details).
     * The values {@code -0.0} and {@code +0.0} have different bitwise
     * representations but are considered equal when using the primitive
     * {@code ==} operator.  Operation failure can occur if, for example, a
     * numeric algorithm computes an expected value to be say {@code -0.0}
     * and previously computed the witness value to be say {@code +0.0}.
     * @param arrayClass the class of an array, of type {@code T[]}
     * @return a VarHandle giving access to elements of an array
     * @throws NullPointerException if the arrayClass is null
     * @throws IllegalArgumentException if arrayClass is not an array type
     * @since 9
     */
    public static VarHandle arrayElementVarHandle(Class<?> arrayClass) throws IllegalArgumentException {
        return VarHandles.makeArrayElementHandle(arrayClass);
    }

    /**
     * Produces a VarHandle giving access to elements of a {@code byte[]} array
     * viewed as if it were a different primitive array type, such as
     * {@code int[]} or {@code long[]}.
     * The VarHandle's variable type is the component type of
     * {@code viewArrayClass} and the list of coordinate types is
     * {@code (byte[], int)}, where the {@code int} coordinate type
     * corresponds to an argument that is an index into a {@code byte[]} array.
     * The returned VarHandle accesses bytes at an index in a {@code byte[]}
     * array, composing bytes to or from a value of the component type of
     * {@code viewArrayClass} according to the given endianness.
     * <p>
     * The supported component types (variables types) are {@code short},
     * {@code char}, {@code int}, {@code long}, {@code float} and
     * {@code double}.
     * <p>
     * Access of bytes at a given index will result in an
     * {@code ArrayIndexOutOfBoundsException} if the index is less than {@code 0}
     * or greater than the {@code byte[]} array length minus the size (in bytes)
     * of {@code T}.
     * <p>
     * Access of bytes at an index may be aligned or misaligned for {@code T},
     * with respect to the underlying memory address, {@code A} say, associated
     * with the array and index.
     * If access is misaligned then access for anything other than the
     * {@code get} and {@code set} access modes will result in an
     * {@code IllegalStateException}.  In such cases atomic access is only
     * guaranteed with respect to the largest power of two that divides the GCD
     * of {@code A} and the size (in bytes) of {@code T}.
     * If access is aligned then following access modes are supported and are
     * guaranteed to support atomic access:
     * <ul>
     * <li>read write access modes for all {@code T}, with the exception of
     *     access modes {@code get} and {@code set} for {@code long} and
     *     {@code double} on 32-bit platforms.
     * <li>atomic update access modes for {@code int}, {@code long},
     *     {@code float} or {@code double}.
     *     (Future major platform releases of the JDK may support additional
     *     types for certain currently unsupported access modes.)
     * <li>numeric atomic update access modes for {@code int} and {@code long}.
     *     (Future major platform releases of the JDK may support additional
     *     numeric types for certain currently unsupported access modes.)
     * <li>bitwise atomic update access modes for {@code int} and {@code long}.
     *     (Future major platform releases of the JDK may support additional
     *     numeric types for certain currently unsupported access modes.)
     * </ul>
     * <p>
     * Misaligned access, and therefore atomicity guarantees, may be determined
     * for {@code byte[]} arrays without operating on a specific array.  Given
     * an {@code index}, {@code T} and it's corresponding boxed type,
     * {@code T_BOX}, misalignment may be determined as follows:
     * <pre>{@code
     * int sizeOfT = T_BOX.BYTES;  // size in bytes of T
     * int misalignedAtZeroIndex = ByteBuffer.wrap(new byte[0]).
     *     alignmentOffset(0, sizeOfT);
     * int misalignedAtIndex = (misalignedAtZeroIndex + index) % sizeOfT;
     * boolean isMisaligned = misalignedAtIndex != 0;
     * }</pre>
     * <p>
     * If the variable type is {@code float} or {@code double} then atomic
     * update access modes compare values using their bitwise representation
     * (see {@link Float#floatToRawIntBits} and
     * {@link Double#doubleToRawLongBits}, respectively).
     * @param viewArrayClass the view array class, with a component type of
     * type {@code T}
     * @param byteOrder the endianness of the view array elements, as
     * stored in the underlying {@code byte} array
     * @return a VarHandle giving access to elements of a {@code byte[]} array
     * viewed as if elements corresponding to the components type of the view
     * array class
     * @throws NullPointerException if viewArrayClass or byteOrder is null
     * @throws IllegalArgumentException if viewArrayClass is not an array type
     * @throws UnsupportedOperationException if the component type of
     * viewArrayClass is not supported as a variable type
     * @since 9
     */
    public static VarHandle byteArrayViewVarHandle(Class<?> viewArrayClass,
                                     ByteOrder byteOrder) throws IllegalArgumentException {
        Objects.requireNonNull(byteOrder);
        return VarHandles.byteArrayViewHandle(viewArrayClass,
                                              byteOrder == ByteOrder.BIG_ENDIAN);
    }

    /**
     * Produces a VarHandle giving access to elements of a {@code ByteBuffer}
     * viewed as if it were an array of elements of a different primitive
     * component type to that of {@code byte}, such as {@code int[]} or
     * {@code long[]}.
     * The VarHandle's variable type is the component type of
     * {@code viewArrayClass} and the list of coordinate types is
     * {@code (ByteBuffer, int)}, where the {@code int} coordinate type
     * corresponds to an argument that is an index into a {@code byte[]} array.
     * The returned VarHandle accesses bytes at an index in a
     * {@code ByteBuffer}, composing bytes to or from a value of the component
     * type of {@code viewArrayClass} according to the given endianness.
     * <p>
     * The supported component types (variables types) are {@code short},
     * {@code char}, {@code int}, {@code long}, {@code float} and
     * {@code double}.
     * <p>
     * Access will result in a {@code ReadOnlyBufferException} for anything
     * other than the read access modes if the {@code ByteBuffer} is read-only.
     * <p>
     * Access of bytes at a given index will result in an
     * {@code IndexOutOfBoundsException} if the index is less than {@code 0}
     * or greater than the {@code ByteBuffer} limit minus the size (in bytes) of
     * {@code T}.
     * <p>
     * Access of bytes at an index may be aligned or misaligned for {@code T},
     * with respect to the underlying memory address, {@code A} say, associated
     * with the {@code ByteBuffer} and index.
     * If access is misaligned then access for anything other than the
     * {@code get} and {@code set} access modes will result in an
     * {@code IllegalStateException}.  In such cases atomic access is only
     * guaranteed with respect to the largest power of two that divides the GCD
     * of {@code A} and the size (in bytes) of {@code T}.
     * If access is aligned then following access modes are supported and are
     * guaranteed to support atomic access:
     * <ul>
     * <li>read write access modes for all {@code T}, with the exception of
     *     access modes {@code get} and {@code set} for {@code long} and
     *     {@code double} on 32-bit platforms.
     * <li>atomic update access modes for {@code int}, {@code long},
     *     {@code float} or {@code double}.
     *     (Future major platform releases of the JDK may support additional
     *     types for certain currently unsupported access modes.)
     * <li>numeric atomic update access modes for {@code int} and {@code long}.
     *     (Future major platform releases of the JDK may support additional
     *     numeric types for certain currently unsupported access modes.)
     * <li>bitwise atomic update access modes for {@code int} and {@code long}.
     *     (Future major platform releases of the JDK may support additional
     *     numeric types for certain currently unsupported access modes.)
     * </ul>
     * <p>
     * Misaligned access, and therefore atomicity guarantees, may be determined
     * for a {@code ByteBuffer}, {@code bb} (direct or otherwise), an
     * {@code index}, {@code T} and it's corresponding boxed type,
     * {@code T_BOX}, as follows:
     * <pre>{@code
     * int sizeOfT = T_BOX.BYTES;  // size in bytes of T
     * ByteBuffer bb = ...
     * int misalignedAtIndex = bb.alignmentOffset(index, sizeOfT);
     * boolean isMisaligned = misalignedAtIndex != 0;
     * }</pre>
     * <p>
     * If the variable type is {@code float} or {@code double} then atomic
     * update access modes compare values using their bitwise representation
     * (see {@link Float#floatToRawIntBits} and
     * {@link Double#doubleToRawLongBits}, respectively).
     * @param viewArrayClass the view array class, with a component type of
     * type {@code T}
     * @param byteOrder the endianness of the view array elements, as
     * stored in the underlying {@code ByteBuffer} (Note this overrides the
     * endianness of a {@code ByteBuffer})
     * @return a VarHandle giving access to elements of a {@code ByteBuffer}
     * viewed as if elements corresponding to the components type of the view
     * array class
     * @throws NullPointerException if viewArrayClass or byteOrder is null
     * @throws IllegalArgumentException if viewArrayClass is not an array type
     * @throws UnsupportedOperationException if the component type of
     * viewArrayClass is not supported as a variable type
     * @since 9
     */
    public static VarHandle byteBufferViewVarHandle(Class<?> viewArrayClass,
                                      ByteOrder byteOrder) throws IllegalArgumentException {
        Objects.requireNonNull(byteOrder);
        return VarHandles.makeByteBufferViewHandle(viewArrayClass,
                                                   byteOrder == ByteOrder.BIG_ENDIAN);
    }


    /// method handle invocation (reflective style)

    /**
     * Produces a method handle which will invoke any method handle of the
     * given {@code type}, with a given number of trailing arguments replaced by
     * a single trailing {@code Object[]} array.
     * The resulting invoker will be a method handle with the following
     * arguments:
     * <ul>
     * <li>a single {@code MethodHandle} target
     * <li>zero or more leading values (counted by {@code leadingArgCount})
     * <li>an {@code Object[]} array containing trailing arguments
     * </ul>
     * <p>
     * The invoker will invoke its target like a call to {@link MethodHandle#invoke invoke} with
     * the indicated {@code type}.
     * That is, if the target is exactly of the given {@code type}, it will behave
     * like {@code invokeExact}; otherwise it behave as if {@link MethodHandle#asType asType}
     * is used to convert the target to the required {@code type}.
     * <p>
     * The type of the returned invoker will not be the given {@code type}, but rather
     * will have all parameters except the first {@code leadingArgCount}
     * replaced by a single array of type {@code Object[]}, which will be
     * the final parameter.
     * <p>
     * Before invoking its target, the invoker will spread the final array, apply
     * reference casts as necessary, and unbox and widen primitive arguments.
     * If, when the invoker is called, the supplied array argument does
     * not have the correct number of elements, the invoker will throw
     * an {@link IllegalArgumentException} instead of invoking the target.
     * <p>
     * This method is equivalent to the following code (though it may be more efficient):
     * <blockquote><pre>{@code
MethodHandle invoker = MethodHandles.invoker(type);
int spreadArgCount = type.parameterCount() - leadingArgCount;
invoker = invoker.asSpreader(Object[].class, spreadArgCount);
return invoker;
     * }</pre></blockquote>
     * This method throws no reflective or security exceptions.
     * @param type the desired target type
     * @param leadingArgCount number of fixed arguments, to be passed unchanged to the target
     * @return a method handle suitable for invoking any method handle of the given type
     * @throws NullPointerException if {@code type} is null
     * @throws IllegalArgumentException if {@code leadingArgCount} is not in
     *                  the range from 0 to {@code type.parameterCount()} inclusive,
     *                  or if the resulting method handle's type would have
     *          <a href="MethodHandle.html#maxarity">too many parameters</a>
     */
    public static MethodHandle spreadInvoker(MethodType type, int leadingArgCount) {
        if (leadingArgCount < 0 || leadingArgCount > type.parameterCount())
            throw newIllegalArgumentException("bad argument count", leadingArgCount);
        type = type.asSpreaderType(Object[].class, leadingArgCount, type.parameterCount() - leadingArgCount);
        return type.invokers().spreadInvoker(leadingArgCount);
    }

    /**
     * Produces a special <em>invoker method handle</em> which can be used to
     * invoke any method handle of the given type, as if by {@link MethodHandle#invokeExact invokeExact}.
     * The resulting invoker will have a type which is
     * exactly equal to the desired type, except that it will accept
     * an additional leading argument of type {@code MethodHandle}.
     * <p>
     * This method is equivalent to the following code (though it may be more efficient):
     * {@code publicLookup().findVirtual(MethodHandle.class, "invokeExact", type)}
     *
     * <p style="font-size:smaller;">
     * <em>Discussion:</em>
     * Invoker method handles can be useful when working with variable method handles
     * of unknown types.
     * For example, to emulate an {@code invokeExact} call to a variable method
     * handle {@code M}, extract its type {@code T},
     * look up the invoker method {@code X} for {@code T},
     * and call the invoker method, as {@code X.invoke(T, A...)}.
     * (It would not work to call {@code X.invokeExact}, since the type {@code T}
     * is unknown.)
     * If spreading, collecting, or other argument transformations are required,
     * they can be applied once to the invoker {@code X} and reused on many {@code M}
     * method handle values, as long as they are compatible with the type of {@code X}.
     * <p style="font-size:smaller;">
     * <em>(Note:  The invoker method is not available via the Core Reflection API.
     * An attempt to call {@linkplain java.lang.reflect.Method#invoke java.lang.reflect.Method.invoke}
     * on the declared {@code invokeExact} or {@code invoke} method will raise an
     * {@link java.lang.UnsupportedOperationException UnsupportedOperationException}.)</em>
     * <p>
     * This method throws no reflective or security exceptions.
     * @param type the desired target type
     * @return a method handle suitable for invoking any method handle of the given type
     * @throws IllegalArgumentException if the resulting method handle's type would have
     *          <a href="MethodHandle.html#maxarity">too many parameters</a>
     */
    public static MethodHandle exactInvoker(MethodType type) {
        return type.invokers().exactInvoker();
    }

    /**
     * Produces a special <em>invoker method handle</em> which can be used to
     * invoke any method handle compatible with the given type, as if by {@link MethodHandle#invoke invoke}.
     * The resulting invoker will have a type which is
     * exactly equal to the desired type, except that it will accept
     * an additional leading argument of type {@code MethodHandle}.
     * <p>
     * Before invoking its target, if the target differs from the expected type,
     * the invoker will apply reference casts as
     * necessary and box, unbox, or widen primitive values, as if by {@link MethodHandle#asType asType}.
     * Similarly, the return value will be converted as necessary.
     * If the target is a {@linkplain MethodHandle#asVarargsCollector variable arity method handle},
     * the required arity conversion will be made, again as if by {@link MethodHandle#asType asType}.
     * <p>
     * This method is equivalent to the following code (though it may be more efficient):
     * {@code publicLookup().findVirtual(MethodHandle.class, "invoke", type)}
     * <p style="font-size:smaller;">
     * <em>Discussion:</em>
     * A {@linkplain MethodType#genericMethodType general method type} is one which
     * mentions only {@code Object} arguments and return values.
     * An invoker for such a type is capable of calling any method handle
     * of the same arity as the general type.
     * <p style="font-size:smaller;">
     * <em>(Note:  The invoker method is not available via the Core Reflection API.
     * An attempt to call {@linkplain java.lang.reflect.Method#invoke java.lang.reflect.Method.invoke}
     * on the declared {@code invokeExact} or {@code invoke} method will raise an
     * {@link java.lang.UnsupportedOperationException UnsupportedOperationException}.)</em>
     * <p>
     * This method throws no reflective or security exceptions.
     * @param type the desired target type
     * @return a method handle suitable for invoking any method handle convertible to the given type
     * @throws IllegalArgumentException if the resulting method handle's type would have
     *          <a href="MethodHandle.html#maxarity">too many parameters</a>
     */
    public static MethodHandle invoker(MethodType type) {
        return type.invokers().genericInvoker();
    }

    /**
     * Produces a special <em>invoker method handle</em> which can be used to
     * invoke a signature-polymorphic access mode method on any VarHandle whose
     * associated access mode type is compatible with the given type.
     * The resulting invoker will have a type which is exactly equal to the
     * desired given type, except that it will accept an additional leading
     * argument of type {@code VarHandle}.
     *
     * @param accessMode the VarHandle access mode
     * @param type the desired target type
     * @return a method handle suitable for invoking an access mode method of
     *         any VarHandle whose access mode type is of the given type.
     * @since 9
     */
    public static MethodHandle varHandleExactInvoker(VarHandle.AccessMode accessMode, MethodType type) {
        return type.invokers().varHandleMethodExactInvoker(accessMode);
    }

    /**
     * Produces a special <em>invoker method handle</em> which can be used to
     * invoke a signature-polymorphic access mode method on any VarHandle whose
     * associated access mode type is compatible with the given type.
     * The resulting invoker will have a type which is exactly equal to the
     * desired given type, except that it will accept an additional leading
     * argument of type {@code VarHandle}.
     * <p>
     * Before invoking its target, if the access mode type differs from the
     * desired given type, the invoker will apply reference casts as necessary
     * and box, unbox, or widen primitive values, as if by
     * {@link MethodHandle#asType asType}.  Similarly, the return value will be
     * converted as necessary.
     * <p>
     * This method is equivalent to the following code (though it may be more
     * efficient): {@code publicLookup().findVirtual(VarHandle.class, accessMode.name(), type)}
     *
     * @param accessMode the VarHandle access mode
     * @param type the desired target type
     * @return a method handle suitable for invoking an access mode method of
     *         any VarHandle whose access mode type is convertible to the given
     *         type.
     * @since 9
     */
    public static MethodHandle varHandleInvoker(VarHandle.AccessMode accessMode, MethodType type) {
        return type.invokers().varHandleMethodInvoker(accessMode);
    }

    /*non-public*/
    static MethodHandle basicInvoker(MethodType type) {
        return type.invokers().basicInvoker();
    }

     /// method handle modification (creation from other method handles)

    /**
     * Produces a method handle which adapts the type of the
     * given method handle to a new type by pairwise argument and return type conversion.
     * The original type and new type must have the same number of arguments.
     * The resulting method handle is guaranteed to report a type
     * which is equal to the desired new type.
     * <p>
     * If the original type and new type are equal, returns target.
     * <p>
     * The same conversions are allowed as for {@link MethodHandle#asType MethodHandle.asType},
     * and some additional conversions are also applied if those conversions fail.
     * Given types <em>T0</em>, <em>T1</em>, one of the following conversions is applied
     * if possible, before or instead of any conversions done by {@code asType}:
     * <ul>
     * <li>If <em>T0</em> and <em>T1</em> are references, and <em>T1</em> is an interface type,
     *     then the value of type <em>T0</em> is passed as a <em>T1</em> without a cast.
     *     (This treatment of interfaces follows the usage of the bytecode verifier.)
     * <li>If <em>T0</em> is boolean and <em>T1</em> is another primitive,
     *     the boolean is converted to a byte value, 1 for true, 0 for false.
     *     (This treatment follows the usage of the bytecode verifier.)
     * <li>If <em>T1</em> is boolean and <em>T0</em> is another primitive,
     *     <em>T0</em> is converted to byte via Java casting conversion (JLS 5.5),
     *     and the low order bit of the result is tested, as if by {@code (x & 1) != 0}.
     * <li>If <em>T0</em> and <em>T1</em> are primitives other than boolean,
     *     then a Java casting conversion (JLS 5.5) is applied.
     *     (Specifically, <em>T0</em> will convert to <em>T1</em> by
     *     widening and/or narrowing.)
     * <li>If <em>T0</em> is a reference and <em>T1</em> a primitive, an unboxing
     *     conversion will be applied at runtime, possibly followed
     *     by a Java casting conversion (JLS 5.5) on the primitive value,
     *     possibly followed by a conversion from byte to boolean by testing
     *     the low-order bit.
     * <li>If <em>T0</em> is a reference and <em>T1</em> a primitive,
     *     and if the reference is null at runtime, a zero value is introduced.
     * </ul>
     * @param target the method handle to invoke after arguments are retyped
     * @param newType the expected type of the new method handle
     * @return a method handle which delegates to the target after performing
     *           any necessary argument conversions, and arranges for any
     *           necessary return value conversions
     * @throws NullPointerException if either argument is null
     * @throws WrongMethodTypeException if the conversion cannot be made
     * @see MethodHandle#asType
     */
    public static MethodHandle explicitCastArguments(MethodHandle target, MethodType newType) {
        explicitCastArgumentsChecks(target, newType);
        // use the asTypeCache when possible:
        MethodType oldType = target.type();
        if (oldType == newType)  return target;
        if (oldType.explicitCastEquivalentToAsType(newType)) {
            return target.asFixedArity().asType(newType);
        }
        return MethodHandleImpl.makePairwiseConvert(target, newType, false);
    }

    private static void explicitCastArgumentsChecks(MethodHandle target, MethodType newType) {
        if (target.type().parameterCount() != newType.parameterCount()) {
            throw new WrongMethodTypeException("cannot explicitly cast " + target + " to " + newType);
        }
    }

    /**
     * Produces a method handle which adapts the calling sequence of the
     * given method handle to a new type, by reordering the arguments.
     * The resulting method handle is guaranteed to report a type
     * which is equal to the desired new type.
     * <p>
     * The given array controls the reordering.
     * Call {@code #I} the number of incoming parameters (the value
     * {@code newType.parameterCount()}, and call {@code #O} the number
     * of outgoing parameters (the value {@code target.type().parameterCount()}).
     * Then the length of the reordering array must be {@code #O},
     * and each element must be a non-negative number less than {@code #I}.
     * For every {@code N} less than {@code #O}, the {@code N}-th
     * outgoing argument will be taken from the {@code I}-th incoming
     * argument, where {@code I} is {@code reorder[N]}.
     * <p>
     * No argument or return value conversions are applied.
     * The type of each incoming argument, as determined by {@code newType},
     * must be identical to the type of the corresponding outgoing parameter
     * or parameters in the target method handle.
     * The return type of {@code newType} must be identical to the return
     * type of the original target.
     * <p>
     * The reordering array need not specify an actual permutation.
     * An incoming argument will be duplicated if its index appears
     * more than once in the array, and an incoming argument will be dropped
     * if its index does not appear in the array.
     * As in the case of {@link #dropArguments(MethodHandle,int,List) dropArguments},
     * incoming arguments which are not mentioned in the reordering array
     * may be of any type, as determined only by {@code newType}.
     * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodType intfn1 = methodType(int.class, int.class);
MethodType intfn2 = methodType(int.class, int.class, int.class);
MethodHandle sub = ... (int x, int y) -> (x-y) ...;
assert(sub.type().equals(intfn2));
MethodHandle sub1 = permuteArguments(sub, intfn2, 0, 1);
MethodHandle rsub = permuteArguments(sub, intfn2, 1, 0);
assert((int)rsub.invokeExact(1, 100) == 99);
MethodHandle add = ... (int x, int y) -> (x+y) ...;
assert(add.type().equals(intfn2));
MethodHandle twice = permuteArguments(add, intfn1, 0, 0);
assert(twice.type().equals(intfn1));
assert((int)twice.invokeExact(21) == 42);
     * }</pre></blockquote>
     * <p>
     * <em>Note:</em> The resulting adapter is never a {@linkplain MethodHandle#asVarargsCollector
     * variable-arity method handle}, even if the original target method handle was.
     * @param target the method handle to invoke after arguments are reordered
     * @param newType the expected type of the new method handle
     * @param reorder an index array which controls the reordering
     * @return a method handle which delegates to the target after it
     *           drops unused arguments and moves and/or duplicates the other arguments
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the index array length is not equal to
     *                  the arity of the target, or if any index array element
     *                  not a valid index for a parameter of {@code newType},
     *                  or if two corresponding parameter types in
     *                  {@code target.type()} and {@code newType} are not identical,
     */
    public static MethodHandle permuteArguments(MethodHandle target, MethodType newType, int... reorder) {
        reorder = reorder.clone();  // get a private copy
        MethodType oldType = target.type();
        permuteArgumentChecks(reorder, newType, oldType);
        // first detect dropped arguments and handle them separately
        int[] originalReorder = reorder;
        BoundMethodHandle result = target.rebind();
        LambdaForm form = result.form;
        int newArity = newType.parameterCount();
        // Normalize the reordering into a real permutation,
        // by removing duplicates and adding dropped elements.
        // This somewhat improves lambda form caching, as well
        // as simplifying the transform by breaking it up into steps.
        for (int ddIdx; (ddIdx = findFirstDupOrDrop(reorder, newArity)) != 0; ) {
            if (ddIdx > 0) {
                // We found a duplicated entry at reorder[ddIdx].
                // Example:  (x,y,z)->asList(x,y,z)
                // permuted by [1*,0,1] => (a0,a1)=>asList(a1,a0,a1)
                // permuted by [0,1,0*] => (a0,a1)=>asList(a0,a1,a0)
                // The starred element corresponds to the argument
                // deleted by the dupArgumentForm transform.
                int srcPos = ddIdx, dstPos = srcPos, dupVal = reorder[srcPos];
                boolean killFirst = false;
                for (int val; (val = reorder[--dstPos]) != dupVal; ) {
                    // Set killFirst if the dup is larger than an intervening position.
                    // This will remove at least one inversion from the permutation.
                    if (dupVal > val) killFirst = true;
                }
                if (!killFirst) {
                    srcPos = dstPos;
                    dstPos = ddIdx;
                }
                form = form.editor().dupArgumentForm(1 + srcPos, 1 + dstPos);
                assert (reorder[srcPos] == reorder[dstPos]);
                oldType = oldType.dropParameterTypes(dstPos, dstPos + 1);
                // contract the reordering by removing the element at dstPos
                int tailPos = dstPos + 1;
                System.arraycopy(reorder, tailPos, reorder, dstPos, reorder.length - tailPos);
                reorder = Arrays.copyOf(reorder, reorder.length - 1);
            } else {
                int dropVal = ~ddIdx, insPos = 0;
                while (insPos < reorder.length && reorder[insPos] < dropVal) {
                    // Find first element of reorder larger than dropVal.
                    // This is where we will insert the dropVal.
                    insPos += 1;
                }
                Class<?> ptype = newType.parameterType(dropVal);
                form = form.editor().addArgumentForm(1 + insPos, BasicType.basicType(ptype));
                oldType = oldType.insertParameterTypes(insPos, ptype);
                // expand the reordering by inserting an element at insPos
                int tailPos = insPos + 1;
                reorder = Arrays.copyOf(reorder, reorder.length + 1);
                System.arraycopy(reorder, insPos, reorder, tailPos, reorder.length - tailPos);
                reorder[insPos] = dropVal;
            }
            assert (permuteArgumentChecks(reorder, newType, oldType));
        }
        assert (reorder.length == newArity);  // a perfect permutation
        // Note:  This may cache too many distinct LFs. Consider backing off to varargs code.
        form = form.editor().permuteArgumentsForm(1, reorder);
        if (newType == result.type() && form == result.internalForm())
            return result;
        return result.copyWith(newType, form);
    }

    /**
     * Return an indication of any duplicate or omission in reorder.
     * If the reorder contains a duplicate entry, return the index of the second occurrence.
     * Otherwise, return ~(n), for the first n in [0..newArity-1] that is not present in reorder.
     * Otherwise, return zero.
     * If an element not in [0..newArity-1] is encountered, return reorder.length.
     */
    private static int findFirstDupOrDrop(int[] reorder, int newArity) {
        final int BIT_LIMIT = 63;  // max number of bits in bit mask
        if (newArity < BIT_LIMIT) {
            long mask = 0;
            for (int i = 0; i < reorder.length; i++) {
                int arg = reorder[i];
                if (arg >= newArity) {
                    return reorder.length;
                }
                long bit = 1L << arg;
                if ((mask & bit) != 0) {
                    return i;  // >0 indicates a dup
                }
                mask |= bit;
            }
            if (mask == (1L << newArity) - 1) {
                assert(Long.numberOfTrailingZeros(Long.lowestOneBit(~mask)) == newArity);
                return 0;
            }
            // find first zero
            long zeroBit = Long.lowestOneBit(~mask);
            int zeroPos = Long.numberOfTrailingZeros(zeroBit);
            assert(zeroPos <= newArity);
            if (zeroPos == newArity) {
                return 0;
            }
            return ~zeroPos;
        } else {
            // same algorithm, different bit set
            BitSet mask = new BitSet(newArity);
            for (int i = 0; i < reorder.length; i++) {
                int arg = reorder[i];
                if (arg >= newArity) {
                    return reorder.length;
                }
                if (mask.get(arg)) {
                    return i;  // >0 indicates a dup
                }
                mask.set(arg);
            }
            int zeroPos = mask.nextClearBit(0);
            assert(zeroPos <= newArity);
            if (zeroPos == newArity) {
                return 0;
            }
            return ~zeroPos;
        }
    }

    static boolean permuteArgumentChecks(int[] reorder, MethodType newType, MethodType oldType) {
        if (newType.returnType() != oldType.returnType())
            throw newIllegalArgumentException("return types do not match",
                    oldType, newType);
        if (reorder.length != oldType.parameterCount())
            throw newIllegalArgumentException("old type parameter count and reorder array length do not match",
                    oldType, Arrays.toString(reorder));

        int limit = newType.parameterCount();
        for (int j = 0; j < reorder.length; j++) {
            int i = reorder[j];
            if (i < 0 || i >= limit) {
                throw newIllegalArgumentException("index is out of bounds for new type",
                        i, newType);
            }
            Class<?> src = newType.parameterType(i);
            Class<?> dst = oldType.parameterType(j);
            if (src != dst)
                throw newIllegalArgumentException("parameter types do not match after reorder",
                        oldType, newType);
        }
        return true;
    }

    /**
     * Produces a method handle of the requested return type which returns the given
     * constant value every time it is invoked.
     * <p>
     * Before the method handle is returned, the passed-in value is converted to the requested type.
     * If the requested type is primitive, widening primitive conversions are attempted,
     * else reference conversions are attempted.
     * <p>The returned method handle is equivalent to {@code identity(type).bindTo(value)}.
     * @param type the return type of the desired method handle
     * @param value the value to return
     * @return a method handle of the given return type and no arguments, which always returns the given value
     * @throws NullPointerException if the {@code type} argument is null
     * @throws ClassCastException if the value cannot be converted to the required return type
     * @throws IllegalArgumentException if the given type is {@code void.class}
     */
    public static MethodHandle constant(Class<?> type, Object value) {
        if (type.isPrimitive()) {
            if (type == void.class)
                throw newIllegalArgumentException("void type");
            Wrapper w = Wrapper.forPrimitiveType(type);
            value = w.convert(value, type);
            if (w.zero().equals(value))
                return zero(w, type);
            return insertArguments(identity(type), 0, value);
        } else {
            if (value == null)
                return zero(Wrapper.OBJECT, type);
            return identity(type).bindTo(value);
        }
    }

    /**
     * Produces a method handle which returns its sole argument when invoked.
     * @param type the type of the sole parameter and return value of the desired method handle
     * @return a unary method handle which accepts and returns the given type
     * @throws NullPointerException if the argument is null
     * @throws IllegalArgumentException if the given type is {@code void.class}
     */
    public static MethodHandle identity(Class<?> type) {
        Wrapper btw = (type.isPrimitive() ? Wrapper.forPrimitiveType(type) : Wrapper.OBJECT);
        int pos = btw.ordinal();
        MethodHandle ident = IDENTITY_MHS[pos];
        if (ident == null) {
            ident = setCachedMethodHandle(IDENTITY_MHS, pos, makeIdentity(btw.primitiveType()));
        }
        if (ident.type().returnType() == type)
            return ident;
        // something like identity(Foo.class); do not bother to intern these
        assert (btw == Wrapper.OBJECT);
        return makeIdentity(type);
    }

    /**
     * Produces a constant method handle of the requested return type which
     * returns the default value for that type every time it is invoked.
     * The resulting constant method handle will have no side effects.
     * <p>The returned method handle is equivalent to {@code empty(methodType(type))}.
     * It is also equivalent to {@code explicitCastArguments(constant(Object.class, null), methodType(type))},
     * since {@code explicitCastArguments} converts {@code null} to default values.
     * @param type the expected return type of the desired method handle
     * @return a constant method handle that takes no arguments
     *         and returns the default value of the given type (or void, if the type is void)
     * @throws NullPointerException if the argument is null
     * @see MethodHandles#constant
     * @see MethodHandles#empty
     * @see MethodHandles#explicitCastArguments
     * @since 9
     */
    public static MethodHandle zero(Class<?> type) {
        Objects.requireNonNull(type);
        return type.isPrimitive() ?  zero(Wrapper.forPrimitiveType(type), type) : zero(Wrapper.OBJECT, type);
    }

    private static MethodHandle identityOrVoid(Class<?> type) {
        return type == void.class ? zero(type) : identity(type);
    }

    /**
     * Produces a method handle of the requested type which ignores any arguments, does nothing,
     * and returns a suitable default depending on the return type.
     * That is, it returns a zero primitive value, a {@code null}, or {@code void}.
     * <p>The returned method handle is equivalent to
     * {@code dropArguments(zero(type.returnType()), 0, type.parameterList())}.
     *
     * @apiNote Given a predicate and target, a useful "if-then" construct can be produced as
     * {@code guardWithTest(pred, target, empty(target.type())}.
     * @param type the type of the desired method handle
     * @return a constant method handle of the given type, which returns a default value of the given return type
     * @throws NullPointerException if the argument is null
     * @see MethodHandles#zero
     * @see MethodHandles#constant
     * @since 9
     */
    public static  MethodHandle empty(MethodType type) {
        Objects.requireNonNull(type);
        return dropArguments(zero(type.returnType()), 0, type.parameterList());
    }

    private static final MethodHandle[] IDENTITY_MHS = new MethodHandle[Wrapper.COUNT];
    private static MethodHandle makeIdentity(Class<?> ptype) {
        MethodType mtype = methodType(ptype, ptype);
        LambdaForm lform = LambdaForm.identityForm(BasicType.basicType(ptype));
        return MethodHandleImpl.makeIntrinsic(mtype, lform, Intrinsic.IDENTITY);
    }

    private static MethodHandle zero(Wrapper btw, Class<?> rtype) {
        int pos = btw.ordinal();
        MethodHandle zero = ZERO_MHS[pos];
        if (zero == null) {
            zero = setCachedMethodHandle(ZERO_MHS, pos, makeZero(btw.primitiveType()));
        }
        if (zero.type().returnType() == rtype)
            return zero;
        assert(btw == Wrapper.OBJECT);
        return makeZero(rtype);
    }
    private static final MethodHandle[] ZERO_MHS = new MethodHandle[Wrapper.COUNT];
    private static MethodHandle makeZero(Class<?> rtype) {
        MethodType mtype = methodType(rtype);
        LambdaForm lform = LambdaForm.zeroForm(BasicType.basicType(rtype));
        return MethodHandleImpl.makeIntrinsic(mtype, lform, Intrinsic.ZERO);
    }

    private static synchronized MethodHandle setCachedMethodHandle(MethodHandle[] cache, int pos, MethodHandle value) {
        // Simulate a CAS, to avoid racy duplication of results.
        MethodHandle prev = cache[pos];
        if (prev != null) return prev;
        return cache[pos] = value;
    }

    /**
     * Provides a target method handle with one or more <em>bound arguments</em>
     * in advance of the method handle's invocation.
     * The formal parameters to the target corresponding to the bound
     * arguments are called <em>bound parameters</em>.
     * Returns a new method handle which saves away the bound arguments.
     * When it is invoked, it receives arguments for any non-bound parameters,
     * binds the saved arguments to their corresponding parameters,
     * and calls the original target.
     * <p>
     * The type of the new method handle will drop the types for the bound
     * parameters from the original target type, since the new method handle
     * will no longer require those arguments to be supplied by its callers.
     * <p>
     * Each given argument object must match the corresponding bound parameter type.
     * If a bound parameter type is a primitive, the argument object
     * must be a wrapper, and will be unboxed to produce the primitive value.
     * <p>
     * The {@code pos} argument selects which parameters are to be bound.
     * It may range between zero and <i>N-L</i> (inclusively),
     * where <i>N</i> is the arity of the target method handle
     * and <i>L</i> is the length of the values array.
     * <p>
     * <em>Note:</em> The resulting adapter is never a {@linkplain MethodHandle#asVarargsCollector
     * variable-arity method handle}, even if the original target method handle was.
     * @param target the method handle to invoke after the argument is inserted
     * @param pos where to insert the argument (zero for the first)
     * @param values the series of arguments to insert
     * @return a method handle which inserts an additional argument,
     *         before calling the original method handle
     * @throws NullPointerException if the target or the {@code values} array is null
     * @throws IllegalArgumentException if (@code pos) is less than {@code 0} or greater than
     *         {@code N - L} where {@code N} is the arity of the target method handle and {@code L}
     *         is the length of the values array.
     * @throws ClassCastException if an argument does not match the corresponding bound parameter
     *         type.
     * @see MethodHandle#bindTo
     */
    public static MethodHandle insertArguments(MethodHandle target, int pos, Object... values) {
        int insCount = values.length;
        Class<?>[] ptypes = insertArgumentsChecks(target, insCount, pos);
        if (insCount == 0)  return target;
        BoundMethodHandle result = target.rebind();
        for (int i = 0; i < insCount; i++) {
            Object value = values[i];
            Class<?> ptype = ptypes[pos+i];
            if (ptype.isPrimitive()) {
                result = insertArgumentPrimitive(result, pos, ptype, value);
            } else {
                value = ptype.cast(value);  // throw CCE if needed
                result = result.bindArgumentL(pos, value);
            }
        }
        return result;
    }

    private static BoundMethodHandle insertArgumentPrimitive(BoundMethodHandle result, int pos,
                                                             Class<?> ptype, Object value) {
        Wrapper w = Wrapper.forPrimitiveType(ptype);
        // perform unboxing and/or primitive conversion
        value = w.convert(value, ptype);
        return switch (w) {
            case INT    -> result.bindArgumentI(pos, (int) value);
            case LONG   -> result.bindArgumentJ(pos, (long) value);
            case FLOAT  -> result.bindArgumentF(pos, (float) value);
            case DOUBLE -> result.bindArgumentD(pos, (double) value);
            default -> result.bindArgumentI(pos, ValueConversions.widenSubword(value));
        };
    }

    private static Class<?>[] insertArgumentsChecks(MethodHandle target, int insCount, int pos) throws RuntimeException {
        MethodType oldType = target.type();
        int outargs = oldType.parameterCount();
        int inargs  = outargs - insCount;
        if (inargs < 0)
            throw newIllegalArgumentException("too many values to insert");
        if (pos < 0 || pos > inargs)
            throw newIllegalArgumentException("no argument type to append");
        return oldType.ptypes();
    }

    /**
     * Produces a method handle which will discard some dummy arguments
     * before calling some other specified <i>target</i> method handle.
     * The type of the new method handle will be the same as the target's type,
     * except it will also include the dummy argument types,
     * at some given position.
     * <p>
     * The {@code pos} argument may range between zero and <i>N</i>,
     * where <i>N</i> is the arity of the target.
     * If {@code pos} is zero, the dummy arguments will precede
     * the target's real arguments; if {@code pos} is <i>N</i>
     * they will come after.
     * <p>
     * <b>Example:</b>
     * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodHandle cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
assertEquals("xy", (String) cat.invokeExact("x", "y"));
MethodType bigType = cat.type().insertParameterTypes(0, int.class, String.class);
MethodHandle d0 = dropArguments(cat, 0, bigType.parameterList().subList(0,2));
assertEquals(bigType, d0.type());
assertEquals("yz", (String) d0.invokeExact(123, "x", "y", "z"));
     * }</pre></blockquote>
     * <p>
     * This method is also equivalent to the following code:
     * <blockquote><pre>
     * {@link #dropArguments(MethodHandle,int,Class...) dropArguments}{@code (target, pos, valueTypes.toArray(new Class[0]))}
     * </pre></blockquote>
     * @param target the method handle to invoke after the arguments are dropped
     * @param pos position of first argument to drop (zero for the leftmost)
     * @param valueTypes the type(s) of the argument(s) to drop
     * @return a method handle which drops arguments of the given types,
     *         before calling the original method handle
     * @throws NullPointerException if the target is null,
     *                              or if the {@code valueTypes} list or any of its elements is null
     * @throws IllegalArgumentException if any element of {@code valueTypes} is {@code void.class},
     *                  or if {@code pos} is negative or greater than the arity of the target,
     *                  or if the new method handle's type would have too many parameters
     */
    public static MethodHandle dropArguments(MethodHandle target, int pos, List<Class<?>> valueTypes) {
        return dropArguments0(target, pos, copyTypes(valueTypes.toArray()));
    }

    private static List<Class<?>> copyTypes(Object[] array) {
        return Arrays.asList(Arrays.copyOf(array, array.length, Class[].class));
    }

    private static MethodHandle dropArguments0(MethodHandle target, int pos, List<Class<?>> valueTypes) {
        MethodType oldType = target.type();  // get NPE
        int dropped = dropArgumentChecks(oldType, pos, valueTypes);
        MethodType newType = oldType.insertParameterTypes(pos, valueTypes);
        if (dropped == 0)  return target;
        BoundMethodHandle result = target.rebind();
        LambdaForm lform = result.form;
        int insertFormArg = 1 + pos;
        for (Class<?> ptype : valueTypes) {
            lform = lform.editor().addArgumentForm(insertFormArg++, BasicType.basicType(ptype));
        }
        result = result.copyWith(newType, lform);
        return result;
    }

    private static int dropArgumentChecks(MethodType oldType, int pos, List<Class<?>> valueTypes) {
        int dropped = valueTypes.size();
        MethodType.checkSlotCount(dropped);
        int outargs = oldType.parameterCount();
        int inargs  = outargs + dropped;
        if (pos < 0 || pos > outargs)
            throw newIllegalArgumentException("no argument type to remove"
                    + Arrays.asList(oldType, pos, valueTypes, inargs, outargs)
                    );
        return dropped;
    }

    /**
     * Produces a method handle which will discard some dummy arguments
     * before calling some other specified <i>target</i> method handle.
     * The type of the new method handle will be the same as the target's type,
     * except it will also include the dummy argument types,
     * at some given position.
     * <p>
     * The {@code pos} argument may range between zero and <i>N</i>,
     * where <i>N</i> is the arity of the target.
     * If {@code pos} is zero, the dummy arguments will precede
     * the target's real arguments; if {@code pos} is <i>N</i>
     * they will come after.
     * @apiNote
     * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodHandle cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
assertEquals("xy", (String) cat.invokeExact("x", "y"));
MethodHandle d0 = dropArguments(cat, 0, String.class);
assertEquals("yz", (String) d0.invokeExact("x", "y", "z"));
MethodHandle d1 = dropArguments(cat, 1, String.class);
assertEquals("xz", (String) d1.invokeExact("x", "y", "z"));
MethodHandle d2 = dropArguments(cat, 2, String.class);
assertEquals("xy", (String) d2.invokeExact("x", "y", "z"));
MethodHandle d12 = dropArguments(cat, 1, int.class, boolean.class);
assertEquals("xz", (String) d12.invokeExact("x", 12, true, "z"));
     * }</pre></blockquote>
     * <p>
     * This method is also equivalent to the following code:
     * <blockquote><pre>
     * {@link #dropArguments(MethodHandle,int,List) dropArguments}{@code (target, pos, Arrays.asList(valueTypes))}
     * </pre></blockquote>
     * @param target the method handle to invoke after the arguments are dropped
     * @param pos position of first argument to drop (zero for the leftmost)
     * @param valueTypes the type(s) of the argument(s) to drop
     * @return a method handle which drops arguments of the given types,
     *         before calling the original method handle
     * @throws NullPointerException if the target is null,
     *                              or if the {@code valueTypes} array or any of its elements is null
     * @throws IllegalArgumentException if any element of {@code valueTypes} is {@code void.class},
     *                  or if {@code pos} is negative or greater than the arity of the target,
     *                  or if the new method handle's type would have
     *                  <a href="MethodHandle.html#maxarity">too many parameters</a>
     */
    public static MethodHandle dropArguments(MethodHandle target, int pos, Class<?>... valueTypes) {
        return dropArguments0(target, pos, copyTypes(valueTypes));
    }

    // private version which allows caller some freedom with error handling
    private static MethodHandle dropArgumentsToMatch(MethodHandle target, int skip, List<Class<?>> newTypes, int pos,
                                      boolean nullOnFailure) {
        newTypes = copyTypes(newTypes.toArray());
        List<Class<?>> oldTypes = target.type().parameterList();
        int match = oldTypes.size();
        if (skip != 0) {
            if (skip < 0 || skip > match) {
                throw newIllegalArgumentException("illegal skip", skip, target);
            }
            oldTypes = oldTypes.subList(skip, match);
            match -= skip;
        }
        List<Class<?>> addTypes = newTypes;
        int add = addTypes.size();
        if (pos != 0) {
            if (pos < 0 || pos > add) {
                throw newIllegalArgumentException("illegal pos", pos, newTypes);
            }
            addTypes = addTypes.subList(pos, add);
            add -= pos;
            assert(addTypes.size() == add);
        }
        // Do not add types which already match the existing arguments.
        if (match > add || !oldTypes.equals(addTypes.subList(0, match))) {
            if (nullOnFailure) {
                return null;
            }
            throw newIllegalArgumentException("argument lists do not match", oldTypes, newTypes);
        }
        addTypes = addTypes.subList(match, add);
        add -= match;
        assert(addTypes.size() == add);
        // newTypes:     (   P*[pos], M*[match], A*[add] )
        // target: ( S*[skip],        M*[match]  )
        MethodHandle adapter = target;
        if (add > 0) {
            adapter = dropArguments0(adapter, skip+ match, addTypes);
        }
        // adapter: (S*[skip],        M*[match], A*[add] )
        if (pos > 0) {
            adapter = dropArguments0(adapter, skip, newTypes.subList(0, pos));
        }
        // adapter: (S*[skip], P*[pos], M*[match], A*[add] )
        return adapter;
    }

    /**
     * Adapts a target method handle to match the given parameter type list. If necessary, adds dummy arguments. Some
     * leading parameters can be skipped before matching begins. The remaining types in the {@code target}'s parameter
     * type list must be a sub-list of the {@code newTypes} type list at the starting position {@code pos}. The
     * resulting handle will have the target handle's parameter type list, with any non-matching parameter types (before
     * or after the matching sub-list) inserted in corresponding positions of the target's original parameters, as if by
     * {@link #dropArguments(MethodHandle, int, Class[])}.
     * <p>
     * The resulting handle will have the same return type as the target handle.
     * <p>
     * In more formal terms, assume these two type lists:<ul>
     * <li>The target handle has the parameter type list {@code S..., M...}, with as many types in {@code S} as
     * indicated by {@code skip}. The {@code M} types are those that are supposed to match part of the given type list,
     * {@code newTypes}.
     * <li>The {@code newTypes} list contains types {@code P..., M..., A...}, with as many types in {@code P} as
     * indicated by {@code pos}. The {@code M} types are precisely those that the {@code M} types in the target handle's
     * parameter type list are supposed to match. The types in {@code A} are additional types found after the matching
     * sub-list.
     * </ul>
     * Given these assumptions, the result of an invocation of {@code dropArgumentsToMatch} will have the parameter type
     * list {@code S..., P..., M..., A...}, with the {@code P} and {@code A} types inserted as if by
     * {@link #dropArguments(MethodHandle, int, Class[])}.
     *
     * @apiNote
     * Two method handles whose argument lists are "effectively identical" (i.e., identical in a common prefix) may be
     * mutually converted to a common type by two calls to {@code dropArgumentsToMatch}, as follows:
     * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
...
MethodHandle h0 = constant(boolean.class, true);
MethodHandle h1 = lookup().findVirtual(String.class, "concat", methodType(String.class, String.class));
MethodType bigType = h1.type().insertParameterTypes(1, String.class, int.class);
MethodHandle h2 = dropArguments(h1, 0, bigType.parameterList());
if (h1.type().parameterCount() < h2.type().parameterCount())
    h1 = dropArgumentsToMatch(h1, 0, h2.type().parameterList(), 0);  // lengthen h1
else
    h2 = dropArgumentsToMatch(h2, 0, h1.type().parameterList(), 0);    // lengthen h2
MethodHandle h3 = guardWithTest(h0, h1, h2);
assertEquals("xy", h3.invoke("x", "y", 1, "a", "b", "c"));
     * }</pre></blockquote>
     * @param target the method handle to adapt
     * @param skip number of targets parameters to disregard (they will be unchanged)
     * @param newTypes the list of types to match {@code target}'s parameter type list to
     * @param pos place in {@code newTypes} where the non-skipped target parameters must occur
     * @return a possibly adapted method handle
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if any element of {@code newTypes} is {@code void.class},
     *         or if {@code skip} is negative or greater than the arity of the target,
     *         or if {@code pos} is negative or greater than the newTypes list size,
     *         or if {@code newTypes} does not contain the {@code target}'s non-skipped parameter types at position
     *         {@code pos}.
     * @since 9
     */
    public static MethodHandle dropArgumentsToMatch(MethodHandle target, int skip, List<Class<?>> newTypes, int pos) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(newTypes);
        return dropArgumentsToMatch(target, skip, newTypes, pos, false);
    }

    /**
     * Drop the return value of the target handle (if any).
     * The returned method handle will have a {@code void} return type.
     *
     * @param target the method handle to adapt
     * @return a possibly adapted method handle
     * @throws NullPointerException if {@code target} is null
     * @since 16
     */
    public static MethodHandle dropReturn(MethodHandle target) {
        Objects.requireNonNull(target);
        MethodType oldType = target.type();
        Class<?> oldReturnType = oldType.returnType();
        if (oldReturnType == void.class)
            return target;
        MethodType newType = oldType.changeReturnType(void.class);
        BoundMethodHandle result = target.rebind();
        LambdaForm lform = result.editor().filterReturnForm(V_TYPE, true);
        result = result.copyWith(newType, lform);
        return result;
    }

    /**
     * Adapts a target method handle by pre-processing
     * one or more of its arguments, each with its own unary filter function,
     * and then calling the target with each pre-processed argument
     * replaced by the result of its corresponding filter function.
     * <p>
     * The pre-processing is performed by one or more method handles,
     * specified in the elements of the {@code filters} array.
     * The first element of the filter array corresponds to the {@code pos}
     * argument of the target, and so on in sequence.
     * The filter functions are invoked in left to right order.
     * <p>
     * Null arguments in the array are treated as identity functions,
     * and the corresponding arguments left unchanged.
     * (If there are no non-null elements in the array, the original target is returned.)
     * Each filter is applied to the corresponding argument of the adapter.
     * <p>
     * If a filter {@code F} applies to the {@code N}th argument of
     * the target, then {@code F} must be a method handle which
     * takes exactly one argument.  The type of {@code F}'s sole argument
     * replaces the corresponding argument type of the target
     * in the resulting adapted method handle.
     * The return type of {@code F} must be identical to the corresponding
     * parameter type of the target.
     * <p>
     * It is an error if there are elements of {@code filters}
     * (null or not)
     * which do not correspond to argument positions in the target.
     * <p><b>Example:</b>
     * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodHandle cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
MethodHandle upcase = lookup().findVirtual(String.class,
  "toUpperCase", methodType(String.class));
assertEquals("xy", (String) cat.invokeExact("x", "y"));
MethodHandle f0 = filterArguments(cat, 0, upcase);
assertEquals("Xy", (String) f0.invokeExact("x", "y")); // Xy
MethodHandle f1 = filterArguments(cat, 1, upcase);
assertEquals("xY", (String) f1.invokeExact("x", "y")); // xY
MethodHandle f2 = filterArguments(cat, 0, upcase, upcase);
assertEquals("XY", (String) f2.invokeExact("x", "y")); // XY
     * }</pre></blockquote>
     * <p>Here is pseudocode for the resulting adapter. In the code, {@code T}
     * denotes the return type of both the {@code target} and resulting adapter.
     * {@code P}/{@code p} and {@code B}/{@code b} represent the types and values
     * of the parameters and arguments that precede and follow the filter position
     * {@code pos}, respectively. {@code A[i]}/{@code a[i]} stand for the types and
     * values of the filtered parameters and arguments; they also represent the
     * return types of the {@code filter[i]} handles. The latter accept arguments
     * {@code v[i]} of type {@code V[i]}, which also appear in the signature of
     * the resulting adapter.
     * <blockquote><pre>{@code
     * T target(P... p, A[i]... a[i], B... b);
     * A[i] filter[i](V[i]);
     * T adapter(P... p, V[i]... v[i], B... b) {
     *   return target(p..., filter[i](v[i])..., b...);
     * }
     * }</pre></blockquote>
     * <p>
     * <em>Note:</em> The resulting adapter is never a {@linkplain MethodHandle#asVarargsCollector
     * variable-arity method handle}, even if the original target method handle was.
     *
     * @param target the method handle to invoke after arguments are filtered
     * @param pos the position of the first argument to filter
     * @param filters method handles to call initially on filtered arguments
     * @return method handle which incorporates the specified argument filtering logic
     * @throws NullPointerException if the target is null
     *                              or if the {@code filters} array is null
     * @throws IllegalArgumentException if a non-null element of {@code filters}
     *          does not match a corresponding argument type of target as described above,
     *          or if the {@code pos+filters.length} is greater than {@code target.type().parameterCount()},
     *          or if the resulting method handle's type would have
     *          <a href="MethodHandle.html#maxarity">too many parameters</a>
     */
    public static MethodHandle filterArguments(MethodHandle target, int pos, MethodHandle... filters) {
        // In method types arguments start at index 0, while the LF
        // editor have the MH receiver at position 0 - adjust appropriately.
        final int MH_RECEIVER_OFFSET = 1;
        filterArgumentsCheckArity(target, pos, filters);
        MethodHandle adapter = target;

        // keep track of currently matched filters, as to optimize repeated filters
        int index = 0;
        int[] positions = new int[filters.length];
        MethodHandle filter = null;

        // process filters in reverse order so that the invocation of
        // the resulting adapter will invoke the filters in left-to-right order
        for (int i = filters.length - 1; i >= 0; --i) {
            MethodHandle newFilter = filters[i];
            if (newFilter == null) continue;  // ignore null elements of filters

            // flush changes on update
            if (filter != newFilter) {
                if (filter != null) {
                    if (index > 1) {
                        adapter = filterRepeatedArgument(adapter, filter, Arrays.copyOf(positions, index));
                    } else {
                        adapter = filterArgument(adapter, positions[0] - 1, filter);
                    }
                }
                filter = newFilter;
                index = 0;
            }

            filterArgumentChecks(target, pos + i, newFilter);
            positions[index++] = pos + i + MH_RECEIVER_OFFSET;
        }
        if (index > 1) {
            adapter = filterRepeatedArgument(adapter, filter, Arrays.copyOf(positions, index));
        } else if (index == 1) {
            adapter = filterArgument(adapter, positions[0] - 1, filter);
        }
        return adapter;
    }

    private static MethodHandle filterRepeatedArgument(MethodHandle adapter, MethodHandle filter, int[] positions) {
        MethodType targetType = adapter.type();
        MethodType filterType = filter.type();
        BoundMethodHandle result = adapter.rebind();
        Class<?> newParamType = filterType.parameterType(0);

        Class<?>[] ptypes = targetType.ptypes().clone();
        for (int pos : positions) {
            ptypes[pos - 1] = newParamType;
        }
        MethodType newType = MethodType.makeImpl(targetType.rtype(), ptypes, true);

        LambdaForm lform = result.editor().filterRepeatedArgumentForm(BasicType.basicType(newParamType), positions);
        return result.copyWithExtendL(newType, lform, filter);
    }

    /*non-public*/
    static MethodHandle filterArgument(MethodHandle target, int pos, MethodHandle filter) {
        filterArgumentChecks(target, pos, filter);
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        BoundMethodHandle result = target.rebind();
        Class<?> newParamType = filterType.parameterType(0);
        LambdaForm lform = result.editor().filterArgumentForm(1 + pos, BasicType.basicType(newParamType));
        MethodType newType = targetType.changeParameterType(pos, newParamType);
        result = result.copyWithExtendL(newType, lform, filter);
        return result;
    }

    private static void filterArgumentsCheckArity(MethodHandle target, int pos, MethodHandle[] filters) {
        MethodType targetType = target.type();
        int maxPos = targetType.parameterCount();
        if (pos + filters.length > maxPos)
            throw newIllegalArgumentException("too many filters");
    }

    private static void filterArgumentChecks(MethodHandle target, int pos, MethodHandle filter) throws RuntimeException {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        if (filterType.parameterCount() != 1
            || filterType.returnType() != targetType.parameterType(pos))
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
    }

    /**
     * Adapts a target method handle by pre-processing
     * a sub-sequence of its arguments with a filter (another method handle).
     * The pre-processed arguments are replaced by the result (if any) of the
     * filter function.
     * The target is then called on the modified (usually shortened) argument list.
     * <p>
     * If the filter returns a value, the target must accept that value as
     * its argument in position {@code pos}, preceded and/or followed by
     * any arguments not passed to the filter.
     * If the filter returns void, the target must accept all arguments
     * not passed to the filter.
     * No arguments are reordered, and a result returned from the filter
     * replaces (in order) the whole subsequence of arguments originally
     * passed to the adapter.
     * <p>
     * The argument types (if any) of the filter
     * replace zero or one argument types of the target, at position {@code pos},
     * in the resulting adapted method handle.
     * The return type of the filter (if any) must be identical to the
     * argument type of the target at position {@code pos}, and that target argument
     * is supplied by the return value of the filter.
     * <p>
     * In all cases, {@code pos} must be greater than or equal to zero, and
     * {@code pos} must also be less than or equal to the target's arity.
     * <p><b>Example:</b>
     * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodHandle deepToString = publicLookup()
  .findStatic(Arrays.class, "deepToString", methodType(String.class, Object[].class));

MethodHandle ts1 = deepToString.asCollector(String[].class, 1);
assertEquals("[strange]", (String) ts1.invokeExact("strange"));

MethodHandle ts2 = deepToString.asCollector(String[].class, 2);
assertEquals("[up, down]", (String) ts2.invokeExact("up", "down"));

MethodHandle ts3 = deepToString.asCollector(String[].class, 3);
MethodHandle ts3_ts2 = collectArguments(ts3, 1, ts2);
assertEquals("[top, [up, down], strange]",
             (String) ts3_ts2.invokeExact("top", "up", "down", "strange"));

MethodHandle ts3_ts2_ts1 = collectArguments(ts3_ts2, 3, ts1);
assertEquals("[top, [up, down], [strange]]",
             (String) ts3_ts2_ts1.invokeExact("top", "up", "down", "strange"));

MethodHandle ts3_ts2_ts3 = collectArguments(ts3_ts2, 1, ts3);
assertEquals("[top, [[up, down, strange], charm], bottom]",
             (String) ts3_ts2_ts3.invokeExact("top", "up", "down", "strange", "charm", "bottom"));
     * }</pre></blockquote>
     * <p>Here is pseudocode for the resulting adapter. In the code, {@code T}
     * represents the return type of the {@code target} and resulting adapter.
     * {@code V}/{@code v} stand for the return type and value of the
     * {@code filter}, which are also found in the signature and arguments of
     * the {@code target}, respectively, unless {@code V} is {@code void}.
     * {@code A}/{@code a} and {@code C}/{@code c} represent the parameter types
     * and values preceding and following the collection position, {@code pos},
     * in the {@code target}'s signature. They also turn up in the resulting
     * adapter's signature and arguments, where they surround
     * {@code B}/{@code b}, which represent the parameter types and arguments
     * to the {@code filter} (if any).
     * <blockquote><pre>{@code
     * T target(A...,V,C...);
     * V filter(B...);
     * T adapter(A... a,B... b,C... c) {
     *   V v = filter(b...);
     *   return target(a...,v,c...);
     * }
     * // and if the filter has no arguments:
     * T target2(A...,V,C...);
     * V filter2();
     * T adapter2(A... a,C... c) {
     *   V v = filter2();
     *   return target2(a...,v,c...);
     * }
     * // and if the filter has a void return:
     * T target3(A...,C...);
     * void filter3(B...);
     * T adapter3(A... a,B... b,C... c) {
     *   filter3(b...);
     *   return target3(a...,c...);
     * }
     * }</pre></blockquote>
     * <p>
     * A collection adapter {@code collectArguments(mh, 0, coll)} is equivalent to
     * one which first "folds" the affected arguments, and then drops them, in separate
     * steps as follows:
     * <blockquote><pre>{@code
     * mh = MethodHandles.dropArguments(mh, 1, coll.type().parameterList()); //step 2
     * mh = MethodHandles.foldArguments(mh, coll); //step 1
     * }</pre></blockquote>
     * If the target method handle consumes no arguments besides than the result
     * (if any) of the filter {@code coll}, then {@code collectArguments(mh, 0, coll)}
     * is equivalent to {@code filterReturnValue(coll, mh)}.
     * If the filter method handle {@code coll} consumes one argument and produces
     * a non-void result, then {@code collectArguments(mh, N, coll)}
     * is equivalent to {@code filterArguments(mh, N, coll)}.
     * Other equivalences are possible but would require argument permutation.
     * <p>
     * <em>Note:</em> The resulting adapter is never a {@linkplain MethodHandle#asVarargsCollector
     * variable-arity method handle}, even if the original target method handle was.
     *
     * @param target the method handle to invoke after filtering the subsequence of arguments
     * @param pos the position of the first adapter argument to pass to the filter,
     *            and/or the target argument which receives the result of the filter
     * @param filter method handle to call on the subsequence of arguments
     * @return method handle which incorporates the specified argument subsequence filtering logic
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the return type of {@code filter}
     *          is non-void and is not the same as the {@code pos} argument of the target,
     *          or if {@code pos} is not between 0 and the target's arity, inclusive,
     *          or if the resulting method handle's type would have
     *          <a href="MethodHandle.html#maxarity">too many parameters</a>
     * @see MethodHandles#foldArguments
     * @see MethodHandles#filterArguments
     * @see MethodHandles#filterReturnValue
     */
    public static MethodHandle collectArguments(MethodHandle target, int pos, MethodHandle filter) {
        MethodType newType = collectArgumentsChecks(target, pos, filter);
        MethodType collectorType = filter.type();
        BoundMethodHandle result = target.rebind();
        LambdaForm lform = result.editor().collectArgumentsForm(1 + pos, collectorType.basicType());
        return result.copyWithExtendL(newType, lform, filter);
    }

    private static MethodType collectArgumentsChecks(MethodHandle target, int pos, MethodHandle filter) throws RuntimeException {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        Class<?> rtype = filterType.returnType();
        List<Class<?>> filterArgs = filterType.parameterList();
        if (pos < 0 || (rtype == void.class && pos > targetType.parameterCount()) ||
                       (rtype != void.class && pos >= targetType.parameterCount())) {
            throw newIllegalArgumentException("position is out of range for target", target, pos);
        }
        if (rtype == void.class) {
            return targetType.insertParameterTypes(pos, filterArgs);
        }
        if (rtype != targetType.parameterType(pos)) {
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
        }
        return targetType.dropParameterTypes(pos, pos+1).insertParameterTypes(pos, filterArgs);
    }

    /**
     * Adapts a target method handle by post-processing
     * its return value (if any) with a filter (another method handle).
     * The result of the filter is returned from the adapter.
     * <p>
     * If the target returns a value, the filter must accept that value as
     * its only argument.
     * If the target returns void, the filter must accept no arguments.
     * <p>
     * The return type of the filter
     * replaces the return type of the target
     * in the resulting adapted method handle.
     * The argument type of the filter (if any) must be identical to the
     * return type of the target.
     * <p><b>Example:</b>
     * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodHandle cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
MethodHandle length = lookup().findVirtual(String.class,
  "length", methodType(int.class));
System.out.println((String) cat.invokeExact("x", "y")); // xy
MethodHandle f0 = filterReturnValue(cat, length);
System.out.println((int) f0.invokeExact("x", "y")); // 2
     * }</pre></blockquote>
     * <p>Here is pseudocode for the resulting adapter. In the code,
     * {@code T}/{@code t} represent the result type and value of the
     * {@code target}; {@code V}, the result type of the {@code filter}; and
     * {@code A}/{@code a}, the types and values of the parameters and arguments
     * of the {@code target} as well as the resulting adapter.
     * <blockquote><pre>{@code
     * T target(A...);
     * V filter(T);
     * V adapter(A... a) {
     *   T t = target(a...);
     *   return filter(t);
     * }
     * // and if the target has a void return:
     * void target2(A...);
     * V filter2();
     * V adapter2(A... a) {
     *   target2(a...);
     *   return filter2();
     * }
     * // and if the filter has a void return:
     * T target3(A...);
     * void filter3(V);
     * void adapter3(A... a) {
     *   T t = target3(a...);
     *   filter3(t);
     * }
     * }</pre></blockquote>
     * <p>
     * <em>Note:</em> The resulting adapter is never a {@linkplain MethodHandle#asVarargsCollector
     * variable-arity method handle}, even if the original target method handle was.
     * @param target the method handle to invoke before filtering the return value
     * @param filter method handle to call on the return value
     * @return method handle which incorporates the specified return value filtering logic
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the argument list of {@code filter}
     *          does not match the return type of target as described above
     */
    public static MethodHandle filterReturnValue(MethodHandle target, MethodHandle filter) {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        filterReturnValueChecks(targetType, filterType);
        BoundMethodHandle result = target.rebind();
        BasicType rtype = BasicType.basicType(filterType.returnType());
        LambdaForm lform = result.editor().filterReturnForm(rtype, false);
        MethodType newType = targetType.changeReturnType(filterType.returnType());
        result = result.copyWithExtendL(newType, lform, filter);
        return result;
    }

    private static void filterReturnValueChecks(MethodType targetType, MethodType filterType) throws RuntimeException {
        Class<?> rtype = targetType.returnType();
        int filterValues = filterType.parameterCount();
        if (filterValues == 0
                ? (rtype != void.class)
                : (rtype != filterType.parameterType(0) || filterValues != 1))
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
    }

    /**
     * Filter the return value of a target method handle with a filter function. The filter function is
     * applied to the return value of the original handle; if the filter specifies more than one parameters,
     * then any remaining parameter is appended to the adapter handle. In other words, the adaptation works
     * as follows:
     * <blockquote><pre>{@code
     * T target(A...)
     * V filter(B... , T)
     * V adapter(A... a, B... b) {
     *     T t = target(a...);
     *     return filter(b..., t);
     * }</pre></blockquote>
     * <p>
     * If the filter handle is a unary function, then this method behaves like {@link #filterReturnValue(MethodHandle, MethodHandle)}.
     *
     * @param target the target method handle
     * @param filter the filter method handle
     * @return the adapter method handle
     */
    /* package */ static MethodHandle collectReturnValue(MethodHandle target, MethodHandle filter) {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        BoundMethodHandle result = target.rebind();
        LambdaForm lform = result.editor().collectReturnValueForm(filterType.basicType());
        MethodType newType = targetType.changeReturnType(filterType.returnType());
        if (filterType.parameterList().size() > 1) {
            for (int i = 0 ; i < filterType.parameterList().size() - 1 ; i++) {
                newType = newType.appendParameterTypes(filterType.parameterType(i));
            }
        }
        result = result.copyWithExtendL(newType, lform, filter);
        return result;
    }

    /**
     * Adapts a target method handle by pre-processing
     * some of its arguments, and then calling the target with
     * the result of the pre-processing, inserted into the original
     * sequence of arguments.
     * <p>
     * The pre-processing is performed by {@code combiner}, a second method handle.
     * Of the arguments passed to the adapter, the first {@code N} arguments
     * are copied to the combiner, which is then called.
     * (Here, {@code N} is defined as the parameter count of the combiner.)
     * After this, control passes to the target, with any result
     * from the combiner inserted before the original {@code N} incoming
     * arguments.
     * <p>
     * If the combiner returns a value, the first parameter type of the target
     * must be identical with the return type of the combiner, and the next
     * {@code N} parameter types of the target must exactly match the parameters
     * of the combiner.
     * <p>
     * If the combiner has a void return, no result will be inserted,
     * and the first {@code N} parameter types of the target
     * must exactly match the parameters of the combiner.
     * <p>
     * The resulting adapter is the same type as the target, except that the
     * first parameter type is dropped,
     * if it corresponds to the result of the combiner.
     * <p>
     * (Note that {@link #dropArguments(MethodHandle,int,List) dropArguments} can be used to remove any arguments
     * that either the combiner or the target does not wish to receive.
     * If some of the incoming arguments are destined only for the combiner,
     * consider using {@link MethodHandle#asCollector asCollector} instead, since those
     * arguments will not need to be live on the stack on entry to the
     * target.)
     * <p><b>Example:</b>
     * <blockquote><pre>{@code
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
...
MethodHandle trace = publicLookup().findVirtual(java.io.PrintStream.class,
  "println", methodType(void.class, String.class))
    .bindTo(System.out);
MethodHandle cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
assertEquals("boojum", (String) cat.invokeExact("boo", "jum"));
MethodHandle catTrace = foldArguments(cat, trace);
// also prints "boo":
assertEquals("boojum", (String) catTrace.invokeExact("boo", "jum"));
     * }</pre></blockquote>
     * <p>Here is pseudocode for the resulting adapter. In the code, {@code T}
     * represents the result type of the {@code target} and resulting adapter.
     * {@code V}/{@code v} represent the type and value of the parameter and argument
     * of {@code target} that precedes the folding position; {@code V} also is
     * the result type of the {@code combiner}. {@code A}/{@code a} denote the
     * types and values of the {@code N} parameters and arguments at the folding
     * position. {@code B}/{@code b} represent the types and values of the
     * {@code target} parameters and arguments that follow the folded parameters
     * and arguments.
     * <blockquote><pre>{@code
     * // there are N arguments in A...
     * T target(V, A[N]..., B...);
     * V combiner(A...);
     * T adapter(A... a, B... b) {
     *   V v = combiner(a...);
     *   return target(v, a..., b...);
     * }
     * // and if the combiner has a void return:
     * T target2(A[N]..., B...);
     * void combiner2(A...);
     * T adapter2(A... a, B... b) {
     *   combiner2(a...);
     *   return target2(a..., b...);
     * }
     * }</pre></blockquote>
     * <p>
     * <em>Note:</em> The resulting adapter is never a {@linkplain MethodHandle#asVarargsCollector
     * variable-arity method handle}, even if the original target method handle was.
     * @param target the method handle to invoke after arguments are combined
     * @param combiner method handle to call initially on the incoming arguments
     * @return method handle which incorporates the specified argument folding logic
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if {@code combiner}'s return type
     *          is non-void and not the same as the first argument type of
     *          the target, or if the initial {@code N} argument types
     *          of the target
     *          (skipping one matching the {@code combiner}'s return type)
     *          are not identical with the argument types of {@code combiner}
     */
    public static MethodHandle foldArguments(MethodHandle target, MethodHandle combiner) {
        return foldArguments(target, 0, combiner);
    }

    /**
     * Adapts a target method handle by pre-processing some of its arguments, starting at a given position, and then
     * calling the target with the result of the pre-processing, inserted into the original sequence of arguments just
     * before the folded arguments.
     * <p>
     * This method is closely related to {@link #foldArguments(MethodHandle, MethodHandle)}, but allows to control the
     * position in the parameter list at which folding takes place. The argument controlling this, {@code pos}, is a
     * zero-based index. The aforementioned method {@link #foldArguments(MethodHandle, MethodHandle)} assumes position
     * 0.
     *
     * @apiNote Example:
     * <blockquote><pre>{@code
    import static java.lang.invoke.MethodHandles.*;
    import static java.lang.invoke.MethodType.*;
    ...
    MethodHandle trace = publicLookup().findVirtual(java.io.PrintStream.class,
    "println", methodType(void.class, String.class))
    .bindTo(System.out);
    MethodHandle cat = lookup().findVirtual(String.class,
    "concat", methodType(String.class, String.class));
    assertEquals("boojum", (String) cat.invokeExact("boo", "jum"));
    MethodHandle catTrace = foldArguments(cat, 1, trace);
    // also prints "jum":
    assertEquals("boojum", (String) catTrace.invokeExact("boo", "jum"));
     * }</pre></blockquote>
     * <p>Here is pseudocode for the resulting adapter. In the code, {@code T}
     * represents the result type of the {@code target} and resulting adapter.
     * {@code V}/{@code v} represent the type and value of the parameter and argument
     * of {@code target} that precedes the folding position; {@code V} also is
     * the result type of the {@code combiner}. {@code A}/{@code a} denote the
     * types and values of the {@code N} parameters and arguments at the folding
     * position. {@code Z}/{@code z} and {@code B}/{@code b} represent the types
     * and values of the {@code target} parameters and arguments that precede and
     * follow the folded parameters and arguments starting at {@code pos},
     * respectively.
     * <blockquote><pre>{@code
     * // there are N arguments in A...
     * T target(Z..., V, A[N]..., B...);
     * V combiner(A...);
     * T adapter(Z... z, A... a, B... b) {
     *   V v = combiner(a...);
     *   return target(z..., v, a..., b...);
     * }
     * // and if the combiner has a void return:
     * T target2(Z..., A[N]..., B...);
     * void combiner2(A...);
     * T adapter2(Z... z, A... a, B... b) {
     *   combiner2(a...);
     *   return target2(z..., a..., b...);
     * }
     * }</pre></blockquote>
     * <p>
     * <em>Note:</em> The resulting adapter is never a {@linkplain MethodHandle#asVarargsCollector
     * variable-arity method handle}, even if the original target method handle was.
     *
     * @param target the method handle to invoke after arguments are combined
     * @param pos the position at which to start folding and at which to insert the folding result; if this is {@code
     *            0}, the effect is the same as for {@link #foldArguments(MethodHandle, MethodHandle)}.
     * @param combiner method handle to call initially on the incoming arguments
     * @return method handle which incorporates the specified argument folding logic
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if either of the following two conditions holds:
     *          (1) {@code combiner}'s return type is non-{@code void} and not the same as the argument type at position
     *              {@code pos} of the target signature;
     *          (2) the {@code N} argument types at position {@code pos} of the target signature (skipping one matching
     *              the {@code combiner}'s return type) are not identical with the argument types of {@code combiner}.
     *
     * @see #foldArguments(MethodHandle, MethodHandle)
     * @since 9
     */
    public static MethodHandle foldArguments(MethodHandle target, int pos, MethodHandle combiner) {
        MethodType targetType = target.type();
        MethodType combinerType = combiner.type();
        Class<?> rtype = foldArgumentChecks(pos, targetType, combinerType);
        BoundMethodHandle result = target.rebind();
        boolean dropResult = rtype == void.class;
        LambdaForm lform = result.editor().foldArgumentsForm(1 + pos, dropResult, combinerType.basicType());
        MethodType newType = targetType;
        if (!dropResult) {
            newType = newType.dropParameterTypes(pos, pos + 1);
        }
        result = result.copyWithExtendL(newType, lform, combiner);
        return result;
    }

    private static Class<?> foldArgumentChecks(int foldPos, MethodType targetType, MethodType combinerType) {
        int foldArgs   = combinerType.parameterCount();
        Class<?> rtype = combinerType.returnType();
        int foldVals = rtype == void.class ? 0 : 1;
        int afterInsertPos = foldPos + foldVals;
        boolean ok = (targetType.parameterCount() >= afterInsertPos + foldArgs);
        if (ok) {
            for (int i = 0; i < foldArgs; i++) {
                if (combinerType.parameterType(i) != targetType.parameterType(i + afterInsertPos)) {
                    ok = false;
                    break;
                }
            }
        }
        if (ok && foldVals != 0 && combinerType.returnType() != targetType.parameterType(foldPos))
            ok = false;
        if (!ok)
            throw misMatchedTypes("target and combiner types", targetType, combinerType);
        return rtype;
    }

    /**
     * Adapts a target method handle by pre-processing some of its arguments, then calling the target with the result
     * of the pre-processing replacing the argument at the given position.
     *
     * @param target the method handle to invoke after arguments are combined
     * @param position the position at which to start folding and at which to insert the folding result; if this is {@code
     *            0}, the effect is the same as for {@link #foldArguments(MethodHandle, MethodHandle)}.
     * @param combiner method handle to call initially on the incoming arguments
     * @param argPositions indexes of the target to pick arguments sent to the combiner from
     * @return method handle which incorporates the specified argument folding logic
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if either of the following two conditions holds:
     *          (1) {@code combiner}'s return type is not the same as the argument type at position
     *              {@code pos} of the target signature;
     *          (2) the {@code N} argument types at positions {@code argPositions[1...N]} of the target signature are
     *              not identical with the argument types of {@code combiner}.
     */
    /*non-public*/
    static MethodHandle filterArgumentsWithCombiner(MethodHandle target, int position, MethodHandle combiner, int ... argPositions) {
        return argumentsWithCombiner(true, target, position, combiner, argPositions);
    }

    /**
     * Adapts a target method handle by pre-processing some of its arguments, calling the target with the result of
     * the pre-processing inserted into the original sequence of arguments at the given position.
     *
     * @param target the method handle to invoke after arguments are combined
     * @param position the position at which to start folding and at which to insert the folding result; if this is {@code
     *            0}, the effect is the same as for {@link #foldArguments(MethodHandle, MethodHandle)}.
     * @param combiner method handle to call initially on the incoming arguments
     * @param argPositions indexes of the target to pick arguments sent to the combiner from
     * @return method handle which incorporates the specified argument folding logic
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if either of the following two conditions holds:
     *          (1) {@code combiner}'s return type is non-{@code void} and not the same as the argument type at position
     *              {@code pos} of the target signature;
     *          (2) the {@code N} argument types at positions {@code argPositions[1...N]} of the target signature
     *              (skipping {@code position} where the {@code combiner}'s return will be folded in) are not identical
     *              with the argument types of {@code combiner}.
     */
    /*non-public*/
    static MethodHandle foldArgumentsWithCombiner(MethodHandle target, int position, MethodHandle combiner, int ... argPositions) {
        return argumentsWithCombiner(false, target, position, combiner, argPositions);
    }

    private static MethodHandle argumentsWithCombiner(boolean filter, MethodHandle target, int position, MethodHandle combiner, int ... argPositions) {
        MethodType targetType = target.type();
        MethodType combinerType = combiner.type();
        Class<?> rtype = argumentsWithCombinerChecks(position, filter, targetType, combinerType, argPositions);
        BoundMethodHandle result = target.rebind();

        MethodType newType = targetType;
        LambdaForm lform;
        if (filter) {
            lform = result.editor().filterArgumentsForm(1 + position, combinerType.basicType(), argPositions);
        } else {
            boolean dropResult = rtype == void.class;
            lform = result.editor().foldArgumentsForm(1 + position, dropResult, combinerType.basicType(), argPositions);
            if (!dropResult) {
                newType = newType.dropParameterTypes(position, position + 1);
            }
        }
        result = result.copyWithExtendL(newType, lform, combiner);
        return result;
    }

    private static Class<?> argumentsWithCombinerChecks(int position, boolean filter, MethodType targetType, MethodType combinerType, int ... argPos) {
        int combinerArgs = combinerType.parameterCount();
        if (argPos.length != combinerArgs) {
            throw newIllegalArgumentException("combiner and argument map must be equal size", combinerType, argPos.length);
        }
        Class<?> rtype = combinerType.returnType();

        for (int i = 0; i < combinerArgs; i++) {
            int arg = argPos[i];
            if (arg < 0 || arg > targetType.parameterCount()) {
                throw newIllegalArgumentException("arg outside of target parameterRange", targetType, arg);
            }
            if (combinerType.parameterType(i) != targetType.parameterType(arg)) {
                throw newIllegalArgumentException("target argument type at position " + arg
                        + " must match combiner argument type at index " + i + ": " + targetType
                        + " -> " + combinerType + ", map: " + Arrays.toString(argPos));
            }
        }
        if (filter && combinerType.returnType() != targetType.parameterType(position)) {
            throw misMatchedTypes("target and combiner types", targetType, combinerType);
        }
        return rtype;
    }

    /**
     * Makes a method handle which adapts a target method handle,
     * by guarding it with a test, a boolean-valued method handle.
     * If the guard fails, a fallback handle is called instead.
     * All three method handles must have the same corresponding
     * argument and return types, except that the return type
     * of the test must be boolean, and the test is allowed
     * to have fewer arguments than the other two method handles.
     * <p>
     * Here is pseudocode for the resulting adapter. In the code, {@code T}
     * represents the uniform result type of the three involved handles;
     * {@code A}/{@code a}, the types and values of the {@code target}
     * parameters and arguments that are consumed by the {@code test}; and
     * {@code B}/{@code b}, those types and values of the {@code target}
     * parameters and arguments that are not consumed by the {@code test}.
     * <blockquote><pre>{@code
     * boolean test(A...);
     * T target(A...,B...);
     * T fallback(A...,B...);
     * T adapter(A... a,B... b) {
     *   if (test(a...))
     *     return target(a..., b...);
     *   else
     *     return fallback(a..., b...);
     * }
     * }</pre></blockquote>
     * Note that the test arguments ({@code a...} in the pseudocode) cannot
     * be modified by execution of the test, and so are passed unchanged
     * from the caller to the target or fallback as appropriate.
     * @param test method handle used for test, must return boolean
     * @param target method handle to call if test passes
     * @param fallback method handle to call if test fails
     * @return method handle which incorporates the specified if/then/else logic
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code test} does not return boolean,
     *          or if all three method types do not match (with the return
     *          type of {@code test} changed to match that of the target).
     */
    public static MethodHandle guardWithTest(MethodHandle test,
                               MethodHandle target,
                               MethodHandle fallback) {
        MethodType gtype = test.type();
        MethodType ttype = target.type();
        MethodType ftype = fallback.type();
        if (!ttype.equals(ftype))
            throw misMatchedTypes("target and fallback types", ttype, ftype);
        if (gtype.returnType() != boolean.class)
            throw newIllegalArgumentException("guard type is not a predicate "+gtype);
        List<Class<?>> targs = ttype.parameterList();
        test = dropArgumentsToMatch(test, 0, targs, 0, true);
        if (test == null) {
            throw misMatchedTypes("target and test types", ttype, gtype);
        }
        return MethodHandleImpl.makeGuardWithTest(test, target, fallback);
    }

    static <T> RuntimeException misMatchedTypes(String what, T t1, T t2) {
        return newIllegalArgumentException(what + " must match: " + t1 + " != " + t2);
    }

    /**
     * Makes a method handle which adapts a target method handle,
     * by running it inside an exception handler.
     * If the target returns normally, the adapter returns that value.
     * If an exception matching the specified type is thrown, the fallback
     * handle is called instead on the exception, plus the original arguments.
     * <p>
     * The target and handler must have the same corresponding
     * argument and return types, except that handler may omit trailing arguments
     * (similarly to the predicate in {@link #guardWithTest guardWithTest}).
     * Also, the handler must have an extra leading parameter of {@code exType} or a supertype.
     * <p>
     * Here is pseudocode for the resulting adapter. In the code, {@code T}
     * represents the return type of the {@code target} and {@code handler},
     * and correspondingly that of the resulting adapter; {@code A}/{@code a},
     * the types and values of arguments to the resulting handle consumed by
     * {@code handler}; and {@code B}/{@code b}, those of arguments to the
     * resulting handle discarded by {@code handler}.
     * <blockquote><pre>{@code
     * T target(A..., B...);
     * T handler(ExType, A...);
     * T adapter(A... a, B... b) {
     *   try {
     *     return target(a..., b...);
     *   } catch (ExType ex) {
     *     return handler(ex, a...);
     *   }
     * }
     * }</pre></blockquote>
     * Note that the saved arguments ({@code a...} in the pseudocode) cannot
     * be modified by execution of the target, and so are passed unchanged
     * from the caller to the handler, if the handler is invoked.
     * <p>
     * The target and handler must return the same type, even if the handler
     * always throws.  (This might happen, for instance, because the handler
     * is simulating a {@code finally} clause).
     * To create such a throwing handler, compose the handler creation logic
     * with {@link #throwException throwException},
     * in order to create a method handle of the correct return type.
     * @param target method handle to call
     * @param exType the type of exception which the handler will catch
     * @param handler method handle to call if a matching exception is thrown
     * @return method handle which incorporates the specified try/catch logic
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code handler} does not accept
     *          the given exception type, or if the method handle types do
     *          not match in their return types and their
     *          corresponding parameters
     * @see MethodHandles#tryFinally(MethodHandle, MethodHandle)
     */
    public static MethodHandle catchException(MethodHandle target,
                                Class<? extends Throwable> exType,
                                MethodHandle handler) {
        MethodType ttype = target.type();
        MethodType htype = handler.type();
        if (!Throwable.class.isAssignableFrom(exType))
            throw new ClassCastException(exType.getName());
        if (htype.parameterCount() < 1 ||
            !htype.parameterType(0).isAssignableFrom(exType))
            throw newIllegalArgumentException("handler does not accept exception type "+exType);
        if (htype.returnType() != ttype.returnType())
            throw misMatchedTypes("target and handler return types", ttype, htype);
        handler = dropArgumentsToMatch(handler, 1, ttype.parameterList(), 0, true);
        if (handler == null) {
            throw misMatchedTypes("target and handler types", ttype, htype);
        }
        return MethodHandleImpl.makeGuardWithCatch(target, exType, handler);
    }

    /**
     * Produces a method handle which will throw exceptions of the given {@code exType}.
     * The method handle will accept a single argument of {@code exType},
     * and immediately throw it as an exception.
     * The method type will nominally specify a return of {@code returnType}.
     * The return type may be anything convenient:  It doesn't matter to the
     * method handle's behavior, since it will never return normally.
     * @param returnType the return type of the desired method handle
     * @param exType the parameter type of the desired method handle
     * @return method handle which can throw the given exceptions
     * @throws NullPointerException if either argument is null
     */
    public static MethodHandle throwException(Class<?> returnType, Class<? extends Throwable> exType) {
        if (!Throwable.class.isAssignableFrom(exType))
            throw new ClassCastException(exType.getName());
        return MethodHandleImpl.throwException(methodType(returnType, exType));
    }

    /**
     * Constructs a method handle representing a loop with several loop variables that are updated and checked upon each
     * iteration. Upon termination of the loop due to one of the predicates, a corresponding finalizer is run and
     * delivers the loop's result, which is the return value of the resulting handle.
     * <p>
     * Intuitively, every loop is formed by one or more "clauses", each specifying a local <em>iteration variable</em> and/or a loop
     * exit. Each iteration of the loop executes each clause in order. A clause can optionally update its iteration
     * variable; it can also optionally perform a test and conditional loop exit. In order to express this logic in
     * terms of method handles, each clause will specify up to four independent actions:<ul>
     * <li><em>init:</em> Before the loop executes, the initialization of an iteration variable {@code v} of type {@code V}.
     * <li><em>step:</em> When a clause executes, an update step for the iteration variable {@code v}.
     * <li><em>pred:</em> When a clause executes, a predicate execution to test for loop exit.
     * <li><em>fini:</em> If a clause causes a loop exit, a finalizer execution to compute the loop's return value.
     * </ul>
     * The full sequence of all iteration variable types, in clause order, will be notated as {@code (V...)}.
     * The values themselves will be {@code (v...)}.  When we speak of "parameter lists", we will usually
     * be referring to types, but in some contexts (describing execution) the lists will be of actual values.
     * <p>
     * Some of these clause parts may be omitted according to certain rules, and useful default behavior is provided in
     * this case. See below for a detailed description.
     * <p>
     * <em>Parameters optional everywhere:</em>
     * Each clause function is allowed but not required to accept a parameter for each iteration variable {@code v}.
     * As an exception, the init functions cannot take any {@code v} parameters,
     * because those values are not yet computed when the init functions are executed.
     * Any clause function may neglect to take any trailing subsequence of parameters it is entitled to take.
     * In fact, any clause function may take no arguments at all.
     * <p>
     * <em>Loop parameters:</em>
     * A clause function may take all the iteration variable values it is entitled to, in which case
     * it may also take more trailing parameters. Such extra values are called <em>loop parameters</em>,
     * with their types and values notated as {@code (A...)} and {@code (a...)}.
     * These become the parameters of the resulting loop handle, to be supplied whenever the loop is executed.
     * (Since init functions do not accept iteration variables {@code v}, any parameter to an
     * init function is automatically a loop parameter {@code a}.)
     * As with iteration variables, clause functions are allowed but not required to accept loop parameters.
     * These loop parameters act as loop-invariant values visible across the whole loop.
     * <p>
     * <em>Parameters visible everywhere:</em>
     * Each non-init clause function is permitted to observe the entire loop state, because it can be passed the full
     * list {@code (v... a...)} of current iteration variable values and incoming loop parameters.
     * The init functions can observe initial pre-loop state, in the form {@code (a...)}.
     * Most clause functions will not need all of this information, but they will be formally connected to it
     * as if by {@link #dropArguments}.
     * <a id="astar"></a>
     * More specifically, we shall use the notation {@code (V*)} to express an arbitrary prefix of a full
     * sequence {@code (V...)} (and likewise for {@code (v*)}, {@code (A*)}, {@code (a*)}).
     * In that notation, the general form of an init function parameter list
     * is {@code (A*)}, and the general form of a non-init function parameter list is {@code (V*)} or {@code (V... A*)}.
     * <p>
     * <em>Checking clause structure:</em>
     * Given a set of clauses, there is a number of checks and adjustments performed to connect all the parts of the
     * loop. They are spelled out in detail in the steps below. In these steps, every occurrence of the word "must"
     * corresponds to a place where {@link IllegalArgumentException} will be thrown if the required constraint is not
     * met by the inputs to the loop combinator.
     * <p>
     * <em>Effectively identical sequences:</em>
     * <a id="effid"></a>
     * A parameter list {@code A} is defined to be <em>effectively identical</em> to another parameter list {@code B}
     * if {@code A} and {@code B} are identical, or if {@code A} is shorter and is identical with a proper prefix of {@code B}.
     * When speaking of an unordered set of parameter lists, we say they the set is "effectively identical"
     * as a whole if the set contains a longest list, and all members of the set are effectively identical to
     * that longest list.
     * For example, any set of type sequences of the form {@code (V*)} is effectively identical,
     * and the same is true if more sequences of the form {@code (V... A*)} are added.
     * <p>
     * <em>Step 0: Determine clause structure.</em><ol type="a">
     * <li>The clause array (of type {@code MethodHandle[][]}) must be non-{@code null} and contain at least one element.
     * <li>The clause array may not contain {@code null}s or sub-arrays longer than four elements.
     * <li>Clauses shorter than four elements are treated as if they were padded by {@code null} elements to length
     * four. Padding takes place by appending elements to the array.
     * <li>Clauses with all {@code null}s are disregarded.
     * <li>Each clause is treated as a four-tuple of functions, called "init", "step", "pred", and "fini".
     * </ol>
     * <p>
     * <em>Step 1A: Determine iteration variable types {@code (V...)}.</em><ol type="a">
     * <li>The iteration variable type for each clause is determined using the clause's init and step return types.
     * <li>If both functions are omitted, there is no iteration variable for the corresponding clause ({@code void} is
     * used as the type to indicate that). If one of them is omitted, the other's return type defines the clause's
     * iteration variable type. If both are given, the common return type (they must be identical) defines the clause's
     * iteration variable type.
     * <li>Form the list of return types (in clause order), omitting all occurrences of {@code void}.
     * <li>This list of types is called the "iteration variable types" ({@code (V...)}).
     * </ol>
     * <p>
     * <em>Step 1B: Determine loop parameters {@code (A...)}.</em><ul>
     * <li>Examine and collect init function parameter lists (which are of the form {@code (A*)}).
     * <li>Examine and collect the suffixes of the step, pred, and fini parameter lists, after removing the iteration variable types.
     * (They must have the form {@code (V... A*)}; collect the {@code (A*)} parts only.)
     * <li>Do not collect suffixes from step, pred, and fini parameter lists that do not begin with all the iteration variable types.
     * (These types will be checked in step 2, along with all the clause function types.)
     * <li>Omitted clause functions are ignored.  (Equivalently, they are deemed to have empty parameter lists.)
     * <li>All of the collected parameter lists must be effectively identical.
     * <li>The longest parameter list (which is necessarily unique) is called the "external parameter list" ({@code (A...)}).
     * <li>If there is no such parameter list, the external parameter list is taken to be the empty sequence.
     * <li>The combined list consisting of iteration variable types followed by the external parameter types is called
     * the "internal parameter list".
     * </ul>
     * <p>
     * <em>Step 1C: Determine loop return type.</em><ol type="a">
     * <li>Examine fini function return types, disregarding omitted fini functions.
     * <li>If there are no fini functions, the loop return type is {@code void}.
     * <li>Otherwise, the common return type {@code R} of the fini functions (their return types must be identical) defines the loop return
     * type.
     * </ol>
     * <p>
     * <em>Step 1D: Check other types.</em><ol type="a">
     * <li>There must be at least one non-omitted pred function.
     * <li>Every non-omitted pred function must have a {@code boolean} return type.
     * </ol>
     * <p>
     * <em>Step 2: Determine parameter lists.</em><ol type="a">
     * <li>The parameter list for the resulting loop handle will be the external parameter list {@code (A...)}.
     * <li>The parameter list for init functions will be adjusted to the external parameter list.
     * (Note that their parameter lists are already effectively identical to this list.)
     * <li>The parameter list for every non-omitted, non-init (step, pred, and fini) function must be
     * effectively identical to the internal parameter list {@code (V... A...)}.
     * </ol>
     * <p>
     * <em>Step 3: Fill in omitted functions.</em><ol type="a">
     * <li>If an init function is omitted, use a {@linkplain #empty default value} for the clause's iteration variable
     * type.
     * <li>If a step function is omitted, use an {@linkplain #identity identity function} of the clause's iteration
     * variable type; insert dropped argument parameters before the identity function parameter for the non-{@code void}
     * iteration variables of preceding clauses. (This will turn the loop variable into a local loop invariant.)
     * <li>If a pred function is omitted, use a constant {@code true} function. (This will keep the loop going, as far
     * as this clause is concerned.  Note that in such cases the corresponding fini function is unreachable.)
     * <li>If a fini function is omitted, use a {@linkplain #empty default value} for the
     * loop return type.
     * </ol>
     * <p>
     * <em>Step 4: Fill in missing parameter types.</em><ol type="a">
     * <li>At this point, every init function parameter list is effectively identical to the external parameter list {@code (A...)},
     * but some lists may be shorter. For every init function with a short parameter list, pad out the end of the list.
     * <li>At this point, every non-init function parameter list is effectively identical to the internal parameter
     * list {@code (V... A...)}, but some lists may be shorter. For every non-init function with a short parameter list,
     * pad out the end of the list.
     * <li>Argument lists are padded out by {@linkplain #dropArgumentsToMatch(MethodHandle, int, List, int) dropping unused trailing arguments}.
     * </ol>
     * <p>
     * <em>Final observations.</em><ol type="a">
     * <li>After these steps, all clauses have been adjusted by supplying omitted functions and arguments.
     * <li>All init functions have a common parameter type list {@code (A...)}, which the final loop handle will also have.
     * <li>All fini functions have a common return type {@code R}, which the final loop handle will also have.
     * <li>All non-init functions have a common parameter type list {@code (V... A...)}, of
     * (non-{@code void}) iteration variables {@code V} followed by loop parameters.
     * <li>Each pair of init and step functions agrees in their return type {@code V}.
     * <li>Each non-init function will be able to observe the current values {@code (v...)} of all iteration variables.
     * <li>Every function will be able to observe the incoming values {@code (a...)} of all loop parameters.
     * </ol>
     * <p>
     * <em>Example.</em> As a consequence of step 1A above, the {@code loop} combinator has the following property:
     * <ul>
     * <li>Given {@code N} clauses {@code Cn = {null, Sn, Pn}} with {@code n = 1..N}.
     * <li>Suppose predicate handles {@code Pn} are either {@code null} or have no parameters.
     * (Only one {@code Pn} has to be non-{@code null}.)
     * <li>Suppose step handles {@code Sn} have signatures {@code (B1..BX)Rn}, for some constant {@code X>=N}.
     * <li>Suppose {@code Q} is the count of non-void types {@code Rn}, and {@code (V1...VQ)} is the sequence of those types.
     * <li>It must be that {@code Vn == Bn} for {@code n = 1..min(X,Q)}.
     * <li>The parameter types {@code Vn} will be interpreted as loop-local state elements {@code (V...)}.
     * <li>Any remaining types {@code BQ+1..BX} (if {@code Q<X}) will determine
     * the resulting loop handle's parameter types {@code (A...)}.
     * </ul>
     * In this example, the loop handle parameters {@code (A...)} were derived from the step functions,
     * which is natural if most of the loop computation happens in the steps.  For some loops,
     * the burden of computation might be heaviest in the pred functions, and so the pred functions
     * might need to accept the loop parameter values.  For loops with complex exit logic, the fini
     * functions might need to accept loop parameters, and likewise for loops with complex entry logic,
     * where the init functions will need the extra parameters.  For such reasons, the rules for
     * determining these parameters are as symmetric as possible, across all clause parts.
     * In general, the loop parameters function as common invariant values across the whole
     * loop, while the iteration variables function as common variant values, or (if there is
     * no step function) as internal loop invariant temporaries.
     * <p>
     * <em>Loop execution.</em><ol type="a">
     * <li>When the loop is called, the loop input values are saved in locals, to be passed to
     * every clause function. These locals are loop invariant.
     * <li>Each init function is executed in clause order (passing the external arguments {@code (a...)})
     * and the non-{@code void} values are saved (as the iteration variables {@code (v...)}) into locals.
     * These locals will be loop varying (unless their steps behave as identity functions, as noted above).
     * <li>All function executions (except init functions) will be passed the internal parameter list, consisting of
     * the non-{@code void} iteration values {@code (v...)} (in clause order) and then the loop inputs {@code (a...)}
     * (in argument order).
     * <li>The step and pred functions are then executed, in clause order (step before pred), until a pred function
     * returns {@code false}.
     * <li>The non-{@code void} result from a step function call is used to update the corresponding value in the
     * sequence {@code (v...)} of loop variables.
     * The updated value is immediately visible to all subsequent function calls.
     * <li>If a pred function returns {@code false}, the corresponding fini function is called, and the resulting value
     * (of type {@code R}) is returned from the loop as a whole.
     * <li>If all the pred functions always return true, no fini function is ever invoked, and the loop cannot exit
     * except by throwing an exception.
     * </ol>
     * <p>
     * <em>Usage tips.</em>
     * <ul>
     * <li>Although each step function will receive the current values of <em>all</em> the loop variables,
     * sometimes a step function only needs to observe the current value of its own variable.
     * In that case, the step function may need to explicitly {@linkplain #dropArguments drop all preceding loop variables}.
     * This will require mentioning their types, in an expression like {@code dropArguments(step, 0, V0.class, ...)}.
     * <li>Loop variables are not required to vary; they can be loop invariant.  A clause can create
     * a loop invariant by a suitable init function with no step, pred, or fini function.  This may be
     * useful to "wire" an incoming loop argument into the step or pred function of an adjacent loop variable.
     * <li>If some of the clause functions are virtual methods on an instance, the instance
     * itself can be conveniently placed in an initial invariant loop "variable", using an initial clause
     * like {@code new MethodHandle[]{identity(ObjType.class)}}.  In that case, the instance reference
     * will be the first iteration variable value, and it will be easy to use virtual
     * methods as clause parts, since all of them will take a leading instance reference matching that value.
     * </ul>
     * <p>
     * Here is pseudocode for the resulting loop handle. As above, {@code V} and {@code v} represent the types
     * and values of loop variables; {@code A} and {@code a} represent arguments passed to the whole loop;
     * and {@code R} is the common result type of all finalizers as well as of the resulting loop.
     * <blockquote><pre>{@code
     * V... init...(A...);
     * boolean pred...(V..., A...);
     * V... step...(V..., A...);
     * R fini...(V..., A...);
     * R loop(A... a) {
     *   V... v... = init...(a...);
     *   for (;;) {
     *     for ((v, p, s, f) in (v..., pred..., step..., fini...)) {
     *       v = s(v..., a...);
     *       if (!p(v..., a...)) {
     *         return f(v..., a...);
     *       }
     *     }
     *   }
     * }
     * }</pre></blockquote>
     * Note that the parameter type lists {@code (V...)} and {@code (A...)} have been expanded
     * to their full length, even though individual clause functions may neglect to take them all.
     * As noted above, missing parameters are filled in as if by {@link #dropArgumentsToMatch(MethodHandle, int, List, int)}.
     *
     * @apiNote Example:
     * <blockquote><pre>{@code
     * // iterative implementation of the factorial function as a loop handle
     * static int one(int k) { return 1; }
     * static int inc(int i, int acc, int k) { return i + 1; }
     * static int mult(int i, int acc, int k) { return i * acc; }
     * static boolean pred(int i, int acc, int k) { return i < k; }
     * static int fin(int i, int acc, int k) { return acc; }
     * // assume MH_one, MH_inc, MH_mult, MH_pred, and MH_fin are handles to the above methods
     * // null initializer for counter, should initialize to 0
     * MethodHandle[] counterClause = new MethodHandle[]{null, MH_inc};
     * MethodHandle[] accumulatorClause = new MethodHandle[]{MH_one, MH_mult, MH_pred, MH_fin};
     * MethodHandle loop = MethodHandles.loop(counterClause, accumulatorClause);
     * assertEquals(120, loop.invoke(5));
     * }</pre></blockquote>
     * The same example, dropping arguments and using combinators:
     * <blockquote><pre>{@code
     * // simplified implementation of the factorial function as a loop handle
     * static int inc(int i) { return i + 1; } // drop acc, k
     * static int mult(int i, int acc) { return i * acc; } //drop k
     * static boolean cmp(int i, int k) { return i < k; }
     * // assume MH_inc, MH_mult, and MH_cmp are handles to the above methods
     * // null initializer for counter, should initialize to 0
     * MethodHandle MH_one = MethodHandles.constant(int.class, 1);
     * MethodHandle MH_pred = MethodHandles.dropArguments(MH_cmp, 1, int.class); // drop acc
     * MethodHandle MH_fin = MethodHandles.dropArguments(MethodHandles.identity(int.class), 0, int.class); // drop i
     * MethodHandle[] counterClause = new MethodHandle[]{null, MH_inc};
     * MethodHandle[] accumulatorClause = new MethodHandle[]{MH_one, MH_mult, MH_pred, MH_fin};
     * MethodHandle loop = MethodHandles.loop(counterClause, accumulatorClause);
     * assertEquals(720, loop.invoke(6));
     * }</pre></blockquote>
     * A similar example, using a helper object to hold a loop parameter:
     * <blockquote><pre>{@code
     * // instance-based implementation of the factorial function as a loop handle
     * static class FacLoop {
     *   final int k;
     *   FacLoop(int k) { this.k = k; }
     *   int inc(int i) { return i + 1; }
     *   int mult(int i, int acc) { return i * acc; }
     *   boolean pred(int i) { return i < k; }
     *   int fin(int i, int acc) { return acc; }
     * }
     * // assume MH_FacLoop is a handle to the constructor
     * // assume MH_inc, MH_mult, MH_pred, and MH_fin are handles to the above methods
     * // null initializer for counter, should initialize to 0
     * MethodHandle MH_one = MethodHandles.constant(int.class, 1);
     * MethodHandle[] instanceClause = new MethodHandle[]{MH_FacLoop};
     * MethodHandle[] counterClause = new MethodHandle[]{null, MH_inc};
     * MethodHandle[] accumulatorClause = new MethodHandle[]{MH_one, MH_mult, MH_pred, MH_fin};
     * MethodHandle loop = MethodHandles.loop(instanceClause, counterClause, accumulatorClause);
     * assertEquals(5040, loop.invoke(7));
     * }</pre></blockquote>
     *
     * @param clauses an array of arrays (4-tuples) of {@link MethodHandle}s adhering to the rules described above.
     *
     * @return a method handle embodying the looping behavior as defined by the arguments.
     *
     * @throws IllegalArgumentException in case any of the constraints described above is violated.
     *
     * @see MethodHandles#whileLoop(MethodHandle, MethodHandle, MethodHandle)
     * @see MethodHandles#doWhileLoop(MethodHandle, MethodHandle, MethodHandle)
     * @see MethodHandles#countedLoop(MethodHandle, MethodHandle, MethodHandle)
     * @see MethodHandles#iteratedLoop(MethodHandle, MethodHandle, MethodHandle)
     * @since 9
     */
    public static MethodHandle loop(MethodHandle[]... clauses) {
        // Step 0: determine clause structure.
        loopChecks0(clauses);

        List<MethodHandle> init = new ArrayList<>();
        List<MethodHandle> step = new ArrayList<>();
        List<MethodHandle> pred = new ArrayList<>();
        List<MethodHandle> fini = new ArrayList<>();

        Stream.of(clauses).filter(c -> Stream.of(c).anyMatch(Objects::nonNull)).forEach(clause -> {
            init.add(clause[0]); // all clauses have at least length 1
            step.add(clause.length <= 1 ? null : clause[1]);
            pred.add(clause.length <= 2 ? null : clause[2]);
            fini.add(clause.length <= 3 ? null : clause[3]);
        });

        assert Stream.of(init, step, pred, fini).map(List::size).distinct().count() == 1;
        final int nclauses = init.size();

        // Step 1A: determine iteration variables (V...).
        final List<Class<?>> iterationVariableTypes = new ArrayList<>();
        for (int i = 0; i < nclauses; ++i) {
            MethodHandle in = init.get(i);
            MethodHandle st = step.get(i);
            if (in == null && st == null) {
                iterationVariableTypes.add(void.class);
            } else if (in != null && st != null) {
                loopChecks1a(i, in, st);
                iterationVariableTypes.add(in.type().returnType());
            } else {
                iterationVariableTypes.add(in == null ? st.type().returnType() : in.type().returnType());
            }
        }
        final List<Class<?>> commonPrefix = iterationVariableTypes.stream().filter(t -> t != void.class).toList();

        // Step 1B: determine loop parameters (A...).
        final List<Class<?>> commonSuffix = buildCommonSuffix(init, step, pred, fini, commonPrefix.size());
        loopChecks1b(init, commonSuffix);

        // Step 1C: determine loop return type.
        // Step 1D: check other types.
        // local variable required here; see JDK-8223553
        Stream<Class<?>> cstream = fini.stream().filter(Objects::nonNull).map(MethodHandle::type)
                .map(MethodType::returnType);
        final Class<?> loopReturnType = cstream.findFirst().orElse(void.class);
        loopChecks1cd(pred, fini, loopReturnType);

        // Step 2: determine parameter lists.
        final List<Class<?>> commonParameterSequence = new ArrayList<>(commonPrefix);
        commonParameterSequence.addAll(commonSuffix);
        loopChecks2(step, pred, fini, commonParameterSequence);

        // Step 3: fill in omitted functions.
        for (int i = 0; i < nclauses; ++i) {
            Class<?> t = iterationVariableTypes.get(i);
            if (init.get(i) == null) {
                init.set(i, empty(methodType(t, commonSuffix)));
            }
            if (step.get(i) == null) {
                step.set(i, dropArgumentsToMatch(identityOrVoid(t), 0, commonParameterSequence, i));
            }
            if (pred.get(i) == null) {
                pred.set(i, dropArguments0(constant(boolean.class, true), 0, commonParameterSequence));
            }
            if (fini.get(i) == null) {
                fini.set(i, empty(methodType(t, commonParameterSequence)));
            }
        }

        // Step 4: fill in missing parameter types.
        // Also convert all handles to fixed-arity handles.
        List<MethodHandle> finit = fixArities(fillParameterTypes(init, commonSuffix));
        List<MethodHandle> fstep = fixArities(fillParameterTypes(step, commonParameterSequence));
        List<MethodHandle> fpred = fixArities(fillParameterTypes(pred, commonParameterSequence));
        List<MethodHandle> ffini = fixArities(fillParameterTypes(fini, commonParameterSequence));

        assert finit.stream().map(MethodHandle::type).map(MethodType::parameterList).
                allMatch(pl -> pl.equals(commonSuffix));
        assert Stream.of(fstep, fpred, ffini).flatMap(List::stream).map(MethodHandle::type).map(MethodType::parameterList).
                allMatch(pl -> pl.equals(commonParameterSequence));

        return MethodHandleImpl.makeLoop(loopReturnType, commonSuffix, finit, fstep, fpred, ffini);
    }

    private static void loopChecks0(MethodHandle[][] clauses) {
        if (clauses == null || clauses.length == 0) {
            throw newIllegalArgumentException("null or no clauses passed");
        }
        if (Stream.of(clauses).anyMatch(Objects::isNull)) {
            throw newIllegalArgumentException("null clauses are not allowed");
        }
        if (Stream.of(clauses).anyMatch(c -> c.length > 4)) {
            throw newIllegalArgumentException("All loop clauses must be represented as MethodHandle arrays with at most 4 elements.");
        }
    }

    private static void loopChecks1a(int i, MethodHandle in, MethodHandle st) {
        if (in.type().returnType() != st.type().returnType()) {
            throw misMatchedTypes("clause " + i + ": init and step return types", in.type().returnType(),
                    st.type().returnType());
        }
    }

    private static List<Class<?>> longestParameterList(Stream<MethodHandle> mhs, int skipSize) {
        final List<Class<?>> empty = List.of();
        final List<Class<?>> longest = mhs.filter(Objects::nonNull).
                // take only those that can contribute to a common suffix because they are longer than the prefix
                        map(MethodHandle::type).
                        filter(t -> t.parameterCount() > skipSize).
                        map(MethodType::parameterList).
                        reduce((p, q) -> p.size() >= q.size() ? p : q).orElse(empty);
        return longest.size() == 0 ? empty : longest.subList(skipSize, longest.size());
    }

    private static List<Class<?>> longestParameterList(List<List<Class<?>>> lists) {
        final List<Class<?>> empty = List.of();
        return lists.stream().reduce((p, q) -> p.size() >= q.size() ? p : q).orElse(empty);
    }

    private static List<Class<?>> buildCommonSuffix(List<MethodHandle> init, List<MethodHandle> step, List<MethodHandle> pred, List<MethodHandle> fini, int cpSize) {
        final List<Class<?>> longest1 = longestParameterList(Stream.of(step, pred, fini).flatMap(List::stream), cpSize);
        final List<Class<?>> longest2 = longestParameterList(init.stream(), 0);
        return longestParameterList(Arrays.asList(longest1, longest2));
    }

    private static void loopChecks1b(List<MethodHandle> init, List<Class<?>> commonSuffix) {
        if (init.stream().filter(Objects::nonNull).map(MethodHandle::type).
                anyMatch(t -> !t.effectivelyIdenticalParameters(0, commonSuffix))) {
            throw newIllegalArgumentException("found non-effectively identical init parameter type lists: " + init +
                    " (common suffix: " + commonSuffix + ")");
        }
    }

    private static void loopChecks1cd(List<MethodHandle> pred, List<MethodHandle> fini, Class<?> loopReturnType) {
        if (fini.stream().filter(Objects::nonNull).map(MethodHandle::type).map(MethodType::returnType).
                anyMatch(t -> t != loopReturnType)) {
            throw newIllegalArgumentException("found non-identical finalizer return types: " + fini + " (return type: " +
                    loopReturnType + ")");
        }

        if (!pred.stream().filter(Objects::nonNull).findFirst().isPresent()) {
            throw newIllegalArgumentException("no predicate found", pred);
        }
        if (pred.stream().filter(Objects::nonNull).map(MethodHandle::type).map(MethodType::returnType).
                anyMatch(t -> t != boolean.class)) {
            throw newIllegalArgumentException("predicates must have boolean return type", pred);
        }
    }

    private static void loopChecks2(List<MethodHandle> step, List<MethodHandle> pred, List<MethodHandle> fini, List<Class<?>> commonParameterSequence) {
        if (Stream.of(step, pred, fini).flatMap(List::stream).filter(Objects::nonNull).map(MethodHandle::type).
                anyMatch(t -> !t.effectivelyIdenticalParameters(0, commonParameterSequence))) {
            throw newIllegalArgumentException("found non-effectively identical parameter type lists:\nstep: " + step +
                    "\npred: " + pred + "\nfini: " + fini + " (common parameter sequence: " + commonParameterSequence + ")");
        }
    }

    private static List<MethodHandle> fillParameterTypes(List<MethodHandle> hs, final List<Class<?>> targetParams) {
        return hs.stream().map(h -> {
            int pc = h.type().parameterCount();
            int tpsize = targetParams.size();
            return pc < tpsize ? dropArguments0(h, pc, targetParams.subList(pc, tpsize)) : h;
        }).toList();
    }

    private static List<MethodHandle> fixArities(List<MethodHandle> hs) {
        return hs.stream().map(MethodHandle::asFixedArity).toList();
    }

    /**
     * Constructs a {@code while} loop from an initializer, a body, and a predicate.
     * This is a convenience wrapper for the {@linkplain #loop(MethodHandle[][]) generic loop combinator}.
     * <p>
     * The {@code pred} handle describes the loop condition; and {@code body}, its body. The loop resulting from this
     * method will, in each iteration, first evaluate the predicate and then execute its body (if the predicate
     * evaluates to {@code true}).
     * The loop will terminate once the predicate evaluates to {@code false} (the body will not be executed in this case).
     * <p>
     * The {@code init} handle describes the initial value of an additional optional loop-local variable.
     * In each iteration, this loop-local variable, if present, will be passed to the {@code body}
     * and updated with the value returned from its invocation. The result of loop execution will be
     * the final value of the additional loop-local variable (if present).
     * <p>
     * The following rules hold for these argument handles:<ul>
     * <li>The {@code body} handle must not be {@code null}; its type must be of the form
     * {@code (V A...)V}, where {@code V} is non-{@code void}, or else {@code (A...)void}.
     * (In the {@code void} case, we assign the type {@code void} to the name {@code V},
     * and we will write {@code (V A...)V} with the understanding that a {@code void} type {@code V}
     * is quietly dropped from the parameter list, leaving {@code (A...)V}.)
     * <li>The parameter list {@code (V A...)} of the body is called the <em>internal parameter list</em>.
     * It will constrain the parameter lists of the other loop parts.
     * <li>If the iteration variable type {@code V} is dropped from the internal parameter list, the resulting shorter
     * list {@code (A...)} is called the <em>external parameter list</em>.
     * <li>The body return type {@code V}, if non-{@code void}, determines the type of an
     * additional state variable of the loop.
     * The body must both accept and return a value of this type {@code V}.
     * <li>If {@code init} is non-{@code null}, it must have return type {@code V}.
     * Its parameter list (of some <a href="MethodHandles.html#astar">form {@code (A*)}</a>) must be
     * <a href="MethodHandles.html#effid">effectively identical</a>
     * to the external parameter list {@code (A...)}.
     * <li>If {@code init} is {@code null}, the loop variable will be initialized to its
     * {@linkplain #empty default value}.
     * <li>The {@code pred} handle must not be {@code null}.  It must have {@code boolean} as its return type.
     * Its parameter list (either empty or of the form {@code (V A*)}) must be
     * effectively identical to the internal parameter list.
     * </ul>
     * <p>
     * The resulting loop handle's result type and parameter signature are determined as follows:<ul>
     * <li>The loop handle's result type is the result type {@code V} of the body.
     * <li>The loop handle's parameter types are the types {@code (A...)},
     * from the external parameter list.
     * </ul>
     * <p>
     * Here is pseudocode for the resulting loop handle. In the code, {@code V}/{@code v} represent the type / value of
     * the sole loop variable as well as the result type of the loop; and {@code A}/{@code a}, that of the argument
     * passed to the loop.
     * <blockquote><pre>{@code
     * V init(A...);
     * boolean pred(V, A...);
     * V body(V, A...);
     * V whileLoop(A... a...) {
     *   V v = init(a...);
     *   while (pred(v, a...)) {
     *     v = body(v, a...);
     *   }
     *   return v;
     * }
     * }</pre></blockquote>
     *
     * @apiNote Example:
     * <blockquote><pre>{@code
     * // implement the zip function for lists as a loop handle
     * static List<String> initZip(Iterator<String> a, Iterator<String> b) { return new ArrayList<>(); }
     * static boolean zipPred(List<String> zip, Iterator<String> a, Iterator<String> b) { return a.hasNext() && b.hasNext(); }
     * static List<String> zipStep(List<String> zip, Iterator<String> a, Iterator<String> b) {
     *   zip.add(a.next());
     *   zip.add(b.next());
     *   return zip;
     * }
     * // assume MH_initZip, MH_zipPred, and MH_zipStep are handles to the above methods
     * MethodHandle loop = MethodHandles.whileLoop(MH_initZip, MH_zipPred, MH_zipStep);
     * List<String> a = Arrays.asList("a", "b", "c", "d");
     * List<String> b = Arrays.asList("e", "f", "g", "h");
     * List<String> zipped = Arrays.asList("a", "e", "b", "f", "c", "g", "d", "h");
     * assertEquals(zipped, (List<String>) loop.invoke(a.iterator(), b.iterator()));
     * }</pre></blockquote>
     *
     *
     * @apiNote The implementation of this method can be expressed as follows:
     * <blockquote><pre>{@code
     * MethodHandle whileLoop(MethodHandle init, MethodHandle pred, MethodHandle body) {
     *     MethodHandle fini = (body.type().returnType() == void.class
     *                         ? null : identity(body.type().returnType()));
     *     MethodHandle[]
     *         checkExit = { null, null, pred, fini },
     *         varBody   = { init, body };
     *     return loop(checkExit, varBody);
     * }
     * }</pre></blockquote>
     *
     * @param init optional initializer, providing the initial value of the loop variable.
     *             May be {@code null}, implying a default initial value.  See above for other constraints.
     * @param pred condition for the loop, which may not be {@code null}. Its result type must be {@code boolean}. See
     *             above for other constraints.
     * @param body body of the loop, which may not be {@code null}. It controls the loop parameters and result type.
     *             See above for other constraints.
     *
     * @return a method handle implementing the {@code while} loop as described by the arguments.
     * @throws IllegalArgumentException if the rules for the arguments are violated.
     * @throws NullPointerException if {@code pred} or {@code body} are {@code null}.
     *
     * @see #loop(MethodHandle[][])
     * @see #doWhileLoop(MethodHandle, MethodHandle, MethodHandle)
     * @since 9
     */
    public static MethodHandle whileLoop(MethodHandle init, MethodHandle pred, MethodHandle body) {
        whileLoopChecks(init, pred, body);
        MethodHandle fini = identityOrVoid(body.type().returnType());
        MethodHandle[] checkExit = { null, null, pred, fini };
        MethodHandle[] varBody = { init, body };
        return loop(checkExit, varBody);
    }

    /**
     * Constructs a {@code do-while} loop from an initializer, a body, and a predicate.
     * This is a convenience wrapper for the {@linkplain #loop(MethodHandle[][]) generic loop combinator}.
     * <p>
     * The {@code pred} handle describes the loop condition; and {@code body}, its body. The loop resulting from this
     * method will, in each iteration, first execute its body and then evaluate the predicate.
     * The loop will terminate once the predicate evaluates to {@code false} after an execution of the body.
     * <p>
     * The {@code init} handle describes the initial value of an additional optional loop-local variable.
     * In each iteration, this loop-local variable, if present, will be passed to the {@code body}
     * and updated with the value returned from its invocation. The result of loop execution will be
     * the final value of the additional loop-local variable (if present).
     * <p>
     * The following rules hold for these argument handles:<ul>
     * <li>The {@code body} handle must not be {@code null}; its type must be of the form
     * {@code (V A...)V}, where {@code V} is non-{@code void}, or else {@code (A...)void}.
     * (In the {@code void} case, we assign the type {@code void} to the name {@code V},
     * and we will write {@code (V A...)V} with the understanding that a {@code void} type {@code V}
     * is quietly dropped from the parameter list, leaving {@code (A...)V}.)
     * <li>The parameter list {@code (V A...)} of the body is called the <em>internal parameter list</em>.
     * It will constrain the parameter lists of the other loop parts.
     * <li>If the iteration variable type {@code V} is dropped from the internal parameter list, the resulting shorter
     * list {@code (A...)} is called the <em>external parameter list</em>.
     * <li>The body return type {@code V}, if non-{@code void}, determines the type of an
     * additional state variable of the loop.
     * The body must both accept and return a value of this type {@code V}.
     * <li>If {@code init} is non-{@code null}, it must have return type {@code V}.
     * Its parameter list (of some <a href="MethodHandles.html#astar">form {@code (A*)}</a>) must be
     * <a href="MethodHandles.html#effid">effectively identical</a>
     * to the external parameter list {@code (A...)}.
     * <li>If {@code init} is {@code null}, the loop variable will be initialized to its
     * {@linkplain #empty default value}.
     * <li>The {@code pred} handle must not be {@code null}.  It must have {@code boolean} as its return type.
     * Its parameter list (either empty or of the form {@code (V A*)}) must be
     * effectively identical to the internal parameter list.
     * </ul>
     * <p>
     * The resulting loop handle's result type and parameter signature are determined as follows:<ul>
     * <li>The loop handle's result type is the result type {@code V} of the body.
     * <li>The loop handle's parameter types are the types {@code (A...)},
     * from the external parameter list.
     * </ul>
     * <p>
     * Here is pseudocode for the resulting loop handle. In the code, {@code V}/{@code v} represent the type / value of
     * the sole loop variable as well as the result type of the loop; and {@code A}/{@code a}, that of the argument
     * passed to the loop.
     * <blockquote><pre>{@code
     * V init(A...);
     * boolean pred(V, A...);
     * V body(V, A...);
     * V doWhileLoop(A... a...) {
     *   V v = init(a...);
     *   do {
     *     v = body(v, a...);
     *   } while (pred(v, a...));
     *   return v;
     * }
     * }</pre></blockquote>
     *
     * @apiNote Example:
     * <blockquote><pre>{@code
     * // int i = 0; while (i < limit) { ++i; } return i; => limit
     * static int zero(int limit) { return 0; }
     * static int step(int i, int limit) { return i + 1; }
     * static boolean pred(int i, int limit) { return i < limit; }
     * // assume MH_zero, MH_step, and MH_pred are handles to the above methods
     * MethodHandle loop = MethodHandles.doWhileLoop(MH_zero, MH_step, MH_pred);
     * assertEquals(23, loop.invoke(23));
     * }</pre></blockquote>
     *
     *
     * @apiNote The implementation of this method can be expressed as follows:
     * <blockquote><pre>{@code
     * MethodHandle doWhileLoop(MethodHandle init, MethodHandle body, MethodHandle pred) {
     *     MethodHandle fini = (body.type().returnType() == void.class
     *                         ? null : identity(body.type().returnType()));
     *     MethodHandle[] clause = { init, body, pred, fini };
     *     return loop(clause);
     * }
     * }</pre></blockquote>
     *
     * @param init optional initializer, providing the initial value of the loop variable.
     *             May be {@code null}, implying a default initial value.  See above for other constraints.
     * @param body body of the loop, which may not be {@code null}. It controls the loop parameters and result type.
     *             See above for other constraints.
     * @param pred condition for the loop, which may not be {@code null}. Its result type must be {@code boolean}. See
     *             above for other constraints.
     *
     * @return a method handle implementing the {@code while} loop as described by the arguments.
     * @throws IllegalArgumentException if the rules for the arguments are violated.
     * @throws NullPointerException if {@code pred} or {@code body} are {@code null}.
     *
     * @see #loop(MethodHandle[][])
     * @see #whileLoop(MethodHandle, MethodHandle, MethodHandle)
     * @since 9
     */
    public static MethodHandle doWhileLoop(MethodHandle init, MethodHandle body, MethodHandle pred) {
        whileLoopChecks(init, pred, body);
        MethodHandle fini = identityOrVoid(body.type().returnType());
        MethodHandle[] clause = {init, body, pred, fini };
        return loop(clause);
    }

    private static void whileLoopChecks(MethodHandle init, MethodHandle pred, MethodHandle body) {
        Objects.requireNonNull(pred);
        Objects.requireNonNull(body);
        MethodType bodyType = body.type();
        Class<?> returnType = bodyType.returnType();
        List<Class<?>> innerList = bodyType.parameterList();
        List<Class<?>> outerList = innerList;
        if (returnType == void.class) {
            // OK
        } else if (innerList.size() == 0 || innerList.get(0) != returnType) {
            // leading V argument missing => error
            MethodType expected = bodyType.insertParameterTypes(0, returnType);
            throw misMatchedTypes("body function", bodyType, expected);
        } else {
            outerList = innerList.subList(1, innerList.size());
        }
        MethodType predType = pred.type();
        if (predType.returnType() != boolean.class ||
                !predType.effectivelyIdenticalParameters(0, innerList)) {
            throw misMatchedTypes("loop predicate", predType, methodType(boolean.class, innerList));
        }
        if (init != null) {
            MethodType initType = init.type();
            if (initType.returnType() != returnType ||
                    !initType.effectivelyIdenticalParameters(0, outerList)) {
                throw misMatchedTypes("loop initializer", initType, methodType(returnType, outerList));
            }
        }
    }

    /**
     * Constructs a loop that runs a given number of iterations.
     * This is a convenience wrapper for the {@linkplain #loop(MethodHandle[][]) generic loop combinator}.
     * <p>
     * The number of iterations is determined by the {@code iterations} handle evaluation result.
     * The loop counter {@code i} is an extra loop iteration variable of type {@code int}.
     * It will be initialized to 0 and incremented by 1 in each iteration.
     * <p>
     * If the {@code body} handle returns a non-{@code void} type {@code V}, a leading loop iteration variable
     * of that type is also present.  This variable is initialized using the optional {@code init} handle,
     * or to the {@linkplain #empty default value} of type {@code V} if that handle is {@code null}.
     * <p>
     * In each iteration, the iteration variables are passed to an invocation of the {@code body} handle.
     * A non-{@code void} value returned from the body (of type {@code V}) updates the leading
     * iteration variable.
     * The result of the loop handle execution will be the final {@code V} value of that variable
     * (or {@code void} if there is no {@code V} variable).
     * <p>
     * The following rules hold for the argument handles:<ul>
     * <li>The {@code iterations} handle must not be {@code null}, and must return
     * the type {@code int}, referred to here as {@code I} in parameter type lists.
     * <li>The {@code body} handle must not be {@code null}; its type must be of the form
     * {@code (V I A...)V}, where {@code V} is non-{@code void}, or else {@code (I A...)void}.
     * (In the {@code void} case, we assign the type {@code void} to the name {@code V},
     * and we will write {@code (V I A...)V} with the understanding that a {@code void} type {@code V}
     * is quietly dropped from the parameter list, leaving {@code (I A...)V}.)
     * <li>The parameter list {@code (V I A...)} of the body contributes to a list
     * of types called the <em>internal parameter list</em>.
     * It will constrain the parameter lists of the other loop parts.
     * <li>As a special case, if the body contributes only {@code V} and {@code I} types,
     * with no additional {@code A} types, then the internal parameter list is extended by
     * the argument types {@code A...} of the {@code iterations} handle.
     * <li>If the iteration variable types {@code (V I)} are dropped from the internal parameter list, the resulting shorter
     * list {@code (A...)} is called the <em>external parameter list</em>.
     * <li>The body return type {@code V}, if non-{@code void}, determines the type of an
     * additional state variable of the loop.
     * The body must both accept a leading parameter and return a value of this type {@code V}.
     * <li>If {@code init} is non-{@code null}, it must have return type {@code V}.
     * Its parameter list (of some <a href="MethodHandles.html#astar">form {@code (A*)}</a>) must be
     * <a href="MethodHandles.html#effid">effectively identical</a>
     * to the external parameter list {@code (A...)}.
     * <li>If {@code init} is {@code null}, the loop variable will be initialized to its
     * {@linkplain #empty default value}.
     * <li>The parameter list of {@code iterations} (of some form {@code (A*)}) must be
     * effectively identical to the external parameter list {@code (A...)}.
     * </ul>
     * <p>
     * The resulting loop handle's result type and parameter signature are determined as follows:<ul>
     * <li>The loop handle's result type is the result type {@code V} of the body.
     * <li>The loop handle's parameter types are the types {@code (A...)},
     * from the external parameter list.
     * </ul>
     * <p>
     * Here is pseudocode for the resulting loop handle. In the code, {@code V}/{@code v} represent the type / value of
     * the second loop variable as well as the result type of the loop; and {@code A...}/{@code a...} represent
     * arguments passed to the loop.
     * <blockquote><pre>{@code
     * int iterations(A...);
     * V init(A...);
     * V body(V, int, A...);
     * V countedLoop(A... a...) {
     *   int end = iterations(a...);
     *   V v = init(a...);
     *   for (int i = 0; i < end; ++i) {
     *     v = body(v, i, a...);
     *   }
     *   return v;
     * }
     * }</pre></blockquote>
     *
     * @apiNote Example with a fully conformant body method:
     * <blockquote><pre>{@code
     * // String s = "Lambdaman!"; for (int i = 0; i < 13; ++i) { s = "na " + s; } return s;
     * // => a variation on a well known theme
     * static String step(String v, int counter, String init) { return "na " + v; }
     * // assume MH_step is a handle to the method above
     * MethodHandle fit13 = MethodHandles.constant(int.class, 13);
     * MethodHandle start = MethodHandles.identity(String.class);
     * MethodHandle loop = MethodHandles.countedLoop(fit13, start, MH_step);
     * assertEquals("na na na na na na na na na na na na na Lambdaman!", loop.invoke("Lambdaman!"));
     * }</pre></blockquote>
     *
     * @apiNote Example with the simplest possible body method type,
     * and passing the number of iterations to the loop invocation:
     * <blockquote><pre>{@code
     * // String s = "Lambdaman!"; for (int i = 0; i < 13; ++i) { s = "na " + s; } return s;
     * // => a variation on a well known theme
     * static String step(String v, int counter ) { return "na " + v; }
     * // assume MH_step is a handle to the method above
     * MethodHandle count = MethodHandles.dropArguments(MethodHandles.identity(int.class), 1, String.class);
     * MethodHandle start = MethodHandles.dropArguments(MethodHandles.identity(String.class), 0, int.class);
     * MethodHandle loop = MethodHandles.countedLoop(count, start, MH_step);  // (v, i) -> "na " + v
     * assertEquals("na na na na na na na na na na na na na Lambdaman!", loop.invoke(13, "Lambdaman!"));
     * }</pre></blockquote>
     *
     * @apiNote Example that treats the number of iterations, string to append to, and string to append
     * as loop parameters:
     * <blockquote><pre>{@code
     * // String s = "Lambdaman!", t = "na"; for (int i = 0; i < 13; ++i) { s = t + " " + s; } return s;
     * // => a variation on a well known theme
     * static String step(String v, int counter, int iterations_, String pre, String start_) { return pre + " " + v; }
     * // assume MH_step is a handle to the method above
     * MethodHandle count = MethodHandles.identity(int.class);
     * MethodHandle start = MethodHandles.dropArguments(MethodHandles.identity(String.class), 0, int.class, String.class);
     * MethodHandle loop = MethodHandles.countedLoop(count, start, MH_step);  // (v, i, _, pre, _) -> pre + " " + v
     * assertEquals("na na na na na na na na na na na na na Lambdaman!", loop.invoke(13, "na", "Lambdaman!"));
     * }</pre></blockquote>
     *
     * @apiNote Example that illustrates the usage of {@link #dropArgumentsToMatch(MethodHandle, int, List, int)}
     * to enforce a loop type:
     * <blockquote><pre>{@code
     * // String s = "Lambdaman!", t = "na"; for (int i = 0; i < 13; ++i) { s = t + " " + s; } return s;
     * // => a variation on a well known theme
     * static String step(String v, int counter, String pre) { return pre + " " + v; }
     * // assume MH_step is a handle to the method above
     * MethodType loopType = methodType(String.class, String.class, int.class, String.class);
     * MethodHandle count = MethodHandles.dropArgumentsToMatch(MethodHandles.identity(int.class),    0, loopType.parameterList(), 1);
     * MethodHandle start = MethodHandles.dropArgumentsToMatch(MethodHandles.identity(String.class), 0, loopType.parameterList(), 2);
     * MethodHandle body  = MethodHandles.dropArgumentsToMatch(MH_step,                              2, loopType.parameterList(), 0);
     * MethodHandle loop = MethodHandles.countedLoop(count, start, body);  // (v, i, pre, _, _) -> pre + " " + v
     * assertEquals("na na na na na na na na na na na na na Lambdaman!", loop.invoke("na", 13, "Lambdaman!"));
     * }</pre></blockquote>
     *
     * @apiNote The implementation of this method can be expressed as follows:
     * <blockquote><pre>{@code
     * MethodHandle countedLoop(MethodHandle iterations, MethodHandle init, MethodHandle body) {
     *     return countedLoop(empty(iterations.type()), iterations, init, body);
     * }
     * }</pre></blockquote>
     *
     * @param iterations a non-{@code null} handle to return the number of iterations this loop should run. The handle's
     *                   result type must be {@code int}. See above for other constraints.
     * @param init optional initializer, providing the initial value of the loop variable.
     *             May be {@code null}, implying a default initial value.  See above for other constraints.
     * @param body body of the loop, which may not be {@code null}.
     *             It controls the loop parameters and result type in the standard case (see above for details).
     *             It must accept its own return type (if non-void) plus an {@code int} parameter (for the counter),
     *             and may accept any number of additional types.
     *             See above for other constraints.
     *
     * @return a method handle representing the loop.
     * @throws NullPointerException if either of the {@code iterations} or {@code body} handles is {@code null}.
     * @throws IllegalArgumentException if any argument violates the rules formulated above.
     *
     * @see #countedLoop(MethodHandle, MethodHandle, MethodHandle, MethodHandle)
     * @since 9
     */
    public static MethodHandle countedLoop(MethodHandle iterations, MethodHandle init, MethodHandle body) {
        return countedLoop(empty(iterations.type()), iterations, init, body);
    }

    /**
     * Constructs a loop that counts over a range of numbers.
     * This is a convenience wrapper for the {@linkplain #loop(MethodHandle[][]) generic loop combinator}.
     * <p>
     * The loop counter {@code i} is a loop iteration variable of type {@code int}.
     * The {@code start} and {@code end} handles determine the start (inclusive) and end (exclusive)
     * values of the loop counter.
     * The loop counter will be initialized to the {@code int} value returned from the evaluation of the
     * {@code start} handle and run to the value returned from {@code end} (exclusively) with a step width of 1.
     * <p>
     * If the {@code body} handle returns a non-{@code void} type {@code V}, a leading loop iteration variable
     * of that type is also present.  This variable is initialized using the optional {@code init} handle,
     * or to the {@linkplain #empty default value} of type {@code V} if that handle is {@code null}.
     * <p>
     * In each iteration, the iteration variables are passed to an invocation of the {@code body} handle.
     * A non-{@code void} value returned from the body (of type {@code V}) updates the leading
     * iteration variable.
     * The result of the loop handle execution will be the final {@code V} value of that variable
     * (or {@code void} if there is no {@code V} variable).
     * <p>
     * The following rules hold for the argument handles:<ul>
     * <li>The {@code start} and {@code end} handles must not be {@code null}, and must both return
     * the common type {@code int}, referred to here as {@code I} in parameter type lists.
     * <li>The {@code body} handle must not be {@code null}; its type must be of the form
     * {@code (V I A...)V}, where {@code V} is non-{@code void}, or else {@code (I A...)void}.
     * (In the {@code void} case, we assign the type {@code void} to the name {@code V},
     * and we will write {@code (V I A...)V} with the understanding that a {@code void} type {@code V}
     * is quietly dropped from the parameter list, leaving {@code (I A...)V}.)
     * <li>The parameter list {@code (V I A...)} of the body contributes to a list
     * of types called the <em>internal parameter list</em>.
     * It will constrain the parameter lists of the other loop parts.
     * <li>As a special case, if the body contributes only {@code V} and {@code I} types,
     * with no additional {@code A} types, then the internal parameter list is extended by
     * the argument types {@code A...} of the {@code end} handle.
     * <li>If the iteration variable types {@code (V I)} are dropped from the internal parameter list, the resulting shorter
     * list {@code (A...)} is called the <em>external parameter list</em>.
     * <li>The body return type {@code V}, if non-{@code void}, determines the type of an
     * additional state variable of the loop.
     * The body must both accept a leading parameter and return a value of this type {@code V}.
     * <li>If {@code init} is non-{@code null}, it must have return type {@code V}.
     * Its parameter list (of some <a href="MethodHandles.html#astar">form {@code (A*)}</a>) must be
     * <a href="MethodHandles.html#effid">effectively identical</a>
     * to the external parameter list {@code (A...)}.
     * <li>If {@code init} is {@code null}, the loop variable will be initialized to its
     * {@linkplain #empty default value}.
     * <li>The parameter list of {@code start} (of some form {@code (A*)}) must be
     * effectively identical to the external parameter list {@code (A...)}.
     * <li>Likewise, the parameter list of {@code end} must be effectively identical
     * to the external parameter list.
     * </ul>
     * <p>
     * The resulting loop handle's result type and parameter signature are determined as follows:<ul>
     * <li>The loop handle's result type is the result type {@code V} of the body.
     * <li>The loop handle's parameter types are the types {@code (A...)},
     * from the external parameter list.
     * </ul>
     * <p>
     * Here is pseudocode for the resulting loop handle. In the code, {@code V}/{@code v} represent the type / value of
     * the second loop variable as well as the result type of the loop; and {@code A...}/{@code a...} represent
     * arguments passed to the loop.
     * <blockquote><pre>{@code
     * int start(A...);
     * int end(A...);
     * V init(A...);
     * V body(V, int, A...);
     * V countedLoop(A... a...) {
     *   int e = end(a...);
     *   int s = start(a...);
     *   V v = init(a...);
     *   for (int i = s; i < e; ++i) {
     *     v = body(v, i, a...);
     *   }
     *   return v;
     * }
     * }</pre></blockquote>
     *
     * @apiNote The implementation of this method can be expressed as follows:
     * <blockquote><pre>{@code
     * MethodHandle countedLoop(MethodHandle start, MethodHandle end, MethodHandle init, MethodHandle body) {
     *     MethodHandle returnVar = dropArguments(identity(init.type().returnType()), 0, int.class, int.class);
     *     // assume MH_increment and MH_predicate are handles to implementation-internal methods with
     *     // the following semantics:
     *     // MH_increment: (int limit, int counter) -> counter + 1
     *     // MH_predicate: (int limit, int counter) -> counter < limit
     *     Class<?> counterType = start.type().returnType();  // int
     *     Class<?> returnType = body.type().returnType();
     *     MethodHandle incr = MH_increment, pred = MH_predicate, retv = null;
     *     if (returnType != void.class) {  // ignore the V variable
     *         incr = dropArguments(incr, 1, returnType);  // (limit, v, i) => (limit, i)
     *         pred = dropArguments(pred, 1, returnType);  // ditto
     *         retv = dropArguments(identity(returnType), 0, counterType); // ignore limit
     *     }
     *     body = dropArguments(body, 0, counterType);  // ignore the limit variable
     *     MethodHandle[]
     *         loopLimit  = { end, null, pred, retv }, // limit = end(); i < limit || return v
     *         bodyClause = { init, body },            // v = init(); v = body(v, i)
     *         indexVar   = { start, incr };           // i = start(); i = i + 1
     *     return loop(loopLimit, bodyClause, indexVar);
     * }
     * }</pre></blockquote>
     *
     * @param start a non-{@code null} handle to return the start value of the loop counter, which must be {@code int}.
     *              See above for other constraints.
     * @param end a non-{@code null} handle to return the end value of the loop counter (the loop will run to
     *            {@code end-1}). The result type must be {@code int}. See above for other constraints.
     * @param init optional initializer, providing the initial value of the loop variable.
     *             May be {@code null}, implying a default initial value.  See above for other constraints.
     * @param body body of the loop, which may not be {@code null}.
     *             It controls the loop parameters and result type in the standard case (see above for details).
     *             It must accept its own return type (if non-void) plus an {@code int} parameter (for the counter),
     *             and may accept any number of additional types.
     *             See above for other constraints.
     *
     * @return a method handle representing the loop.
     * @throws NullPointerException if any of the {@code start}, {@code end}, or {@code body} handles is {@code null}.
     * @throws IllegalArgumentException if any argument violates the rules formulated above.
     *
     * @see #countedLoop(MethodHandle, MethodHandle, MethodHandle)
     * @since 9
     */
    public static MethodHandle countedLoop(MethodHandle start, MethodHandle end, MethodHandle init, MethodHandle body) {
        countedLoopChecks(start, end, init, body);
        Class<?> counterType = start.type().returnType();  // int, but who's counting?
        Class<?> limitType   = end.type().returnType();    // yes, int again
        Class<?> returnType  = body.type().returnType();
        MethodHandle incr = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_countedLoopStep);
        MethodHandle pred = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_countedLoopPred);
        MethodHandle retv = null;
        if (returnType != void.class) {
            incr = dropArguments(incr, 1, returnType);  // (limit, v, i) => (limit, i)
            pred = dropArguments(pred, 1, returnType);  // ditto
            retv = dropArguments(identity(returnType), 0, counterType);
        }
        body = dropArguments(body, 0, counterType);  // ignore the limit variable
        MethodHandle[]
            loopLimit  = { end, null, pred, retv }, // limit = end(); i < limit || return v
            bodyClause = { init, body },            // v = init(); v = body(v, i)
            indexVar   = { start, incr };           // i = start(); i = i + 1
        return loop(loopLimit, bodyClause, indexVar);
    }

    private static void countedLoopChecks(MethodHandle start, MethodHandle end, MethodHandle init, MethodHandle body) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(body);
        Class<?> counterType = start.type().returnType();
        if (counterType != int.class) {
            MethodType expected = start.type().changeReturnType(int.class);
            throw misMatchedTypes("start function", start.type(), expected);
        } else if (end.type().returnType() != counterType) {
            MethodType expected = end.type().changeReturnType(counterType);
            throw misMatchedTypes("end function", end.type(), expected);
        }
        MethodType bodyType = body.type();
        Class<?> returnType = bodyType.returnType();
        List<Class<?>> innerList = bodyType.parameterList();
        // strip leading V value if present
        int vsize = (returnType == void.class ? 0 : 1);
        if (vsize != 0 && (innerList.size() == 0 || innerList.get(0) != returnType)) {
            // argument list has no "V" => error
            MethodType expected = bodyType.insertParameterTypes(0, returnType);
            throw misMatchedTypes("body function", bodyType, expected);
        } else if (innerList.size() <= vsize || innerList.get(vsize) != counterType) {
            // missing I type => error
            MethodType expected = bodyType.insertParameterTypes(vsize, counterType);
            throw misMatchedTypes("body function", bodyType, expected);
        }
        List<Class<?>> outerList = innerList.subList(vsize + 1, innerList.size());
        if (outerList.isEmpty()) {
            // special case; take lists from end handle
            outerList = end.type().parameterList();
            innerList = bodyType.insertParameterTypes(vsize + 1, outerList).parameterList();
        }
        MethodType expected = methodType(counterType, outerList);
        if (!start.type().effectivelyIdenticalParameters(0, outerList)) {
            throw misMatchedTypes("start parameter types", start.type(), expected);
        }
        if (end.type() != start.type() &&
            !end.type().effectivelyIdenticalParameters(0, outerList)) {
            throw misMatchedTypes("end parameter types", end.type(), expected);
        }
        if (init != null) {
            MethodType initType = init.type();
            if (initType.returnType() != returnType ||
                !initType.effectivelyIdenticalParameters(0, outerList)) {
                throw misMatchedTypes("loop initializer", initType, methodType(returnType, outerList));
            }
        }
    }

    /**
     * Constructs a loop that ranges over the values produced by an {@code Iterator<T>}.
     * This is a convenience wrapper for the {@linkplain #loop(MethodHandle[][]) generic loop combinator}.
     * <p>
     * The iterator itself will be determined by the evaluation of the {@code iterator} handle.
     * Each value it produces will be stored in a loop iteration variable of type {@code T}.
     * <p>
     * If the {@code body} handle returns a non-{@code void} type {@code V}, a leading loop iteration variable
     * of that type is also present.  This variable is initialized using the optional {@code init} handle,
     * or to the {@linkplain #empty default value} of type {@code V} if that handle is {@code null}.
     * <p>
     * In each iteration, the iteration variables are passed to an invocation of the {@code body} handle.
     * A non-{@code void} value returned from the body (of type {@code V}) updates the leading
     * iteration variable.
     * The result of the loop handle execution will be the final {@code V} value of that variable
     * (or {@code void} if there is no {@code V} variable).
     * <p>
     * The following rules hold for the argument handles:<ul>
     * <li>The {@code body} handle must not be {@code null}; its type must be of the form
     * {@code (V T A...)V}, where {@code V} is non-{@code void}, or else {@code (T A...)void}.
     * (In the {@code void} case, we assign the type {@code void} to the name {@code V},
     * and we will write {@code (V T A...)V} with the understanding that a {@code void} type {@code V}
     * is quietly dropped from the parameter list, leaving {@code (T A...)V}.)
     * <li>The parameter list {@code (V T A...)} of the body contributes to a list
     * of types called the <em>internal parameter list</em>.
     * It will constrain the parameter lists of the other loop parts.
     * <li>As a special case, if the body contributes only {@code V} and {@code T} types,
     * with no additional {@code A} types, then the internal parameter list is extended by
     * the argument types {@code A...} of the {@code iterator} handle; if it is {@code null} the
     * single type {@code Iterable} is added and constitutes the {@code A...} list.
     * <li>If the iteration variable types {@code (V T)} are dropped from the internal parameter list, the resulting shorter
     * list {@code (A...)} is called the <em>external parameter list</em>.
     * <li>The body return type {@code V}, if non-{@code void}, determines the type of an
     * additional state variable of the loop.
     * The body must both accept a leading parameter and return a value of this type {@code V}.
     * <li>If {@code init} is non-{@code null}, it must have return type {@code V}.
     * Its parameter list (of some <a href="MethodHandles.html#astar">form {@code (A*)}</a>) must be
     * <a href="MethodHandles.html#effid">effectively identical</a>
     * to the external parameter list {@code (A...)}.
     * <li>If {@code init} is {@code null}, the loop variable will be initialized to its
     * {@linkplain #empty default value}.
     * <li>If the {@code iterator} handle is non-{@code null}, it must have the return
     * type {@code java.util.Iterator} or a subtype thereof.
     * The iterator it produces when the loop is executed will be assumed
     * to yield values which can be converted to type {@code T}.
     * <li>The parameter list of an {@code iterator} that is non-{@code null} (of some form {@code (A*)}) must be
     * effectively identical to the external parameter list {@code (A...)}.
     * <li>If {@code iterator} is {@code null} it defaults to a method handle which behaves
     * like {@link java.lang.Iterable#iterator()}.  In that case, the internal parameter list
     * {@code (V T A...)} must have at least one {@code A} type, and the default iterator
     * handle parameter is adjusted to accept the leading {@code A} type, as if by
     * the {@link MethodHandle#asType asType} conversion method.
     * The leading {@code A} type must be {@code Iterable} or a subtype thereof.
     * This conversion step, done at loop construction time, must not throw a {@code WrongMethodTypeException}.
     * </ul>
     * <p>
     * The type {@code T} may be either a primitive or reference.
     * Since type {@code Iterator<T>} is erased in the method handle representation to the raw type {@code Iterator},
     * the {@code iteratedLoop} combinator adjusts the leading argument type for {@code body} to {@code Object}
     * as if by the {@link MethodHandle#asType asType} conversion method.
     * Therefore, if an iterator of the wrong type appears as the loop is executed, runtime exceptions may occur
     * as the result of dynamic conversions performed by {@link MethodHandle#asType(MethodType)}.
     * <p>
     * The resulting loop handle's result type and parameter signature are determined as follows:<ul>
     * <li>The loop handle's result type is the result type {@code V} of the body.
     * <li>The loop handle's parameter types are the types {@code (A...)},
     * from the external parameter list.
     * </ul>
     * <p>
     * Here is pseudocode for the resulting loop handle. In the code, {@code V}/{@code v} represent the type / value of
     * the loop variable as well as the result type of the loop; {@code T}/{@code t}, that of the elements of the
     * structure the loop iterates over, and {@code A...}/{@code a...} represent arguments passed to the loop.
     * <blockquote><pre>{@code
     * Iterator<T> iterator(A...);  // defaults to Iterable::iterator
     * V init(A...);
     * V body(V,T,A...);
     * V iteratedLoop(A... a...) {
     *   Iterator<T> it = iterator(a...);
     *   V v = init(a...);
     *   while (it.hasNext()) {
     *     T t = it.next();
     *     v = body(v, t, a...);
     *   }
     *   return v;
     * }
     * }</pre></blockquote>
     *
     * @apiNote Example:
     * <blockquote><pre>{@code
     * // get an iterator from a list
     * static List<String> reverseStep(List<String> r, String e) {
     *   r.add(0, e);
     *   return r;
     * }
     * static List<String> newArrayList() { return new ArrayList<>(); }
     * // assume MH_reverseStep and MH_newArrayList are handles to the above methods
     * MethodHandle loop = MethodHandles.iteratedLoop(null, MH_newArrayList, MH_reverseStep);
     * List<String> list = Arrays.asList("a", "b", "c", "d", "e");
     * List<String> reversedList = Arrays.asList("e", "d", "c", "b", "a");
     * assertEquals(reversedList, (List<String>) loop.invoke(list));
     * }</pre></blockquote>
     *
     * @apiNote The implementation of this method can be expressed approximately as follows:
     * <blockquote><pre>{@code
     * MethodHandle iteratedLoop(MethodHandle iterator, MethodHandle init, MethodHandle body) {
     *     // assume MH_next, MH_hasNext, MH_startIter are handles to methods of Iterator/Iterable
     *     Class<?> returnType = body.type().returnType();
     *     Class<?> ttype = body.type().parameterType(returnType == void.class ? 0 : 1);
     *     MethodHandle nextVal = MH_next.asType(MH_next.type().changeReturnType(ttype));
     *     MethodHandle retv = null, step = body, startIter = iterator;
     *     if (returnType != void.class) {
     *         // the simple thing first:  in (I V A...), drop the I to get V
     *         retv = dropArguments(identity(returnType), 0, Iterator.class);
     *         // body type signature (V T A...), internal loop types (I V A...)
     *         step = swapArguments(body, 0, 1);  // swap V <-> T
     *     }
     *     if (startIter == null)  startIter = MH_getIter;
     *     MethodHandle[]
     *         iterVar    = { startIter, null, MH_hasNext, retv }, // it = iterator; while (it.hasNext())
     *         bodyClause = { init, filterArguments(step, 0, nextVal) };  // v = body(v, t, a)
     *     return loop(iterVar, bodyClause);
     * }
     * }</pre></blockquote>
     *
     * @param iterator an optional handle to return the iterator to start the loop.
     *                 If non-{@code null}, the handle must return {@link java.util.Iterator} or a subtype.
     *                 See above for other constraints.
     * @param init optional initializer, providing the initial value of the loop variable.
     *             May be {@code null}, implying a default initial value.  See above for other constraints.
     * @param body body of the loop, which may not be {@code null}.
     *             It controls the loop parameters and result type in the standard case (see above for details).
     *             It must accept its own return type (if non-void) plus a {@code T} parameter (for the iterated values),
     *             and may accept any number of additional types.
     *             See above for other constraints.
     *
     * @return a method handle embodying the iteration loop functionality.
     * @throws NullPointerException if the {@code body} handle is {@code null}.
     * @throws IllegalArgumentException if any argument violates the above requirements.
     *
     * @since 9
     */
    public static MethodHandle iteratedLoop(MethodHandle iterator, MethodHandle init, MethodHandle body) {
        Class<?> iterableType = iteratedLoopChecks(iterator, init, body);
        Class<?> returnType = body.type().returnType();
        MethodHandle hasNext = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_iteratePred);
        MethodHandle nextRaw = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_iterateNext);
        MethodHandle startIter;
        MethodHandle nextVal;
        {
            MethodType iteratorType;
            if (iterator == null) {
                // derive argument type from body, if available, else use Iterable
                startIter = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_initIterator);
                iteratorType = startIter.type().changeParameterType(0, iterableType);
            } else {
                // force return type to the internal iterator class
                iteratorType = iterator.type().changeReturnType(Iterator.class);
                startIter = iterator;
            }
            Class<?> ttype = body.type().parameterType(returnType == void.class ? 0 : 1);
            MethodType nextValType = nextRaw.type().changeReturnType(ttype);

            // perform the asType transforms under an exception transformer, as per spec.:
            try {
                startIter = startIter.asType(iteratorType);
                nextVal = nextRaw.asType(nextValType);
            } catch (WrongMethodTypeException ex) {
                throw new IllegalArgumentException(ex);
            }
        }

        MethodHandle retv = null, step = body;
        if (returnType != void.class) {
            // the simple thing first:  in (I V A...), drop the I to get V
            retv = dropArguments(identity(returnType), 0, Iterator.class);
            // body type signature (V T A...), internal loop types (I V A...)
            step = swapArguments(body, 0, 1);  // swap V <-> T
        }

        MethodHandle[]
            iterVar    = { startIter, null, hasNext, retv },
            bodyClause = { init, filterArgument(step, 0, nextVal) };
        return loop(iterVar, bodyClause);
    }

    private static Class<?> iteratedLoopChecks(MethodHandle iterator, MethodHandle init, MethodHandle body) {
        Objects.requireNonNull(body);
        MethodType bodyType = body.type();
        Class<?> returnType = bodyType.returnType();
        List<Class<?>> internalParamList = bodyType.parameterList();
        // strip leading V value if present
        int vsize = (returnType == void.class ? 0 : 1);
        if (vsize != 0 && (internalParamList.size() == 0 || internalParamList.get(0) != returnType)) {
            // argument list has no "V" => error
            MethodType expected = bodyType.insertParameterTypes(0, returnType);
            throw misMatchedTypes("body function", bodyType, expected);
        } else if (internalParamList.size() <= vsize) {
            // missing T type => error
            MethodType expected = bodyType.insertParameterTypes(vsize, Object.class);
            throw misMatchedTypes("body function", bodyType, expected);
        }
        List<Class<?>> externalParamList = internalParamList.subList(vsize + 1, internalParamList.size());
        Class<?> iterableType = null;
        if (iterator != null) {
            // special case; if the body handle only declares V and T then
            // the external parameter list is obtained from iterator handle
            if (externalParamList.isEmpty()) {
                externalParamList = iterator.type().parameterList();
            }
            MethodType itype = iterator.type();
            if (!Iterator.class.isAssignableFrom(itype.returnType())) {
                throw newIllegalArgumentException("iteratedLoop first argument must have Iterator return type");
            }
            if (!itype.effectivelyIdenticalParameters(0, externalParamList)) {
                MethodType expected = methodType(itype.returnType(), externalParamList);
                throw misMatchedTypes("iterator parameters", itype, expected);
            }
        } else {
            if (externalParamList.isEmpty()) {
                // special case; if the iterator handle is null and the body handle
                // only declares V and T then the external parameter list consists
                // of Iterable
                externalParamList = Arrays.asList(Iterable.class);
                iterableType = Iterable.class;
            } else {
                // special case; if the iterator handle is null and the external
                // parameter list is not empty then the first parameter must be
                // assignable to Iterable
                iterableType = externalParamList.get(0);
                if (!Iterable.class.isAssignableFrom(iterableType)) {
                    throw newIllegalArgumentException(
                            "inferred first loop argument must inherit from Iterable: " + iterableType);
                }
            }
        }
        if (init != null) {
            MethodType initType = init.type();
            if (initType.returnType() != returnType ||
                    !initType.effectivelyIdenticalParameters(0, externalParamList)) {
                throw misMatchedTypes("loop initializer", initType, methodType(returnType, externalParamList));
            }
        }
        return iterableType;  // help the caller a bit
    }

    /*non-public*/
    static MethodHandle swapArguments(MethodHandle mh, int i, int j) {
        // there should be a better way to uncross my wires
        int arity = mh.type().parameterCount();
        int[] order = new int[arity];
        for (int k = 0; k < arity; k++)  order[k] = k;
        order[i] = j; order[j] = i;
        Class<?>[] types = mh.type().parameterArray();
        Class<?> ti = types[i]; types[i] = types[j]; types[j] = ti;
        MethodType swapType = methodType(mh.type().returnType(), types);
        return permuteArguments(mh, swapType, order);
    }

    /**
     * Makes a method handle that adapts a {@code target} method handle by wrapping it in a {@code try-finally} block.
     * Another method handle, {@code cleanup}, represents the functionality of the {@code finally} block. Any exception
     * thrown during the execution of the {@code target} handle will be passed to the {@code cleanup} handle. The
     * exception will be rethrown, unless {@code cleanup} handle throws an exception first.  The
     * value returned from the {@code cleanup} handle's execution will be the result of the execution of the
     * {@code try-finally} handle.
     * <p>
     * The {@code cleanup} handle will be passed one or two additional leading arguments.
     * The first is the exception thrown during the
     * execution of the {@code target} handle, or {@code null} if no exception was thrown.
     * The second is the result of the execution of the {@code target} handle, or, if it throws an exception,
     * a {@code null}, zero, or {@code false} value of the required type is supplied as a placeholder.
     * The second argument is not present if the {@code target} handle has a {@code void} return type.
     * (Note that, except for argument type conversions, combinators represent {@code void} values in parameter lists
     * by omitting the corresponding paradoxical arguments, not by inserting {@code null} or zero values.)
     * <p>
     * The {@code target} and {@code cleanup} handles must have the same corresponding argument and return types, except
     * that the {@code cleanup} handle may omit trailing arguments. Also, the {@code cleanup} handle must have one or
     * two extra leading parameters:<ul>
     * <li>a {@code Throwable}, which will carry the exception thrown by the {@code target} handle (if any); and
     * <li>a parameter of the same type as the return type of both {@code target} and {@code cleanup}, which will carry
     * the result from the execution of the {@code target} handle.
     * This parameter is not present if the {@code target} returns {@code void}.
     * </ul>
     * <p>
     * The pseudocode for the resulting adapter looks as follows. In the code, {@code V} represents the result type of
     * the {@code try/finally} construct; {@code A}/{@code a}, the types and values of arguments to the resulting
     * handle consumed by the cleanup; and {@code B}/{@code b}, those of arguments to the resulting handle discarded by
     * the cleanup.
     * <blockquote><pre>{@code
     * V target(A..., B...);
     * V cleanup(Throwable, V, A...);
     * V adapter(A... a, B... b) {
     *   V result = (zero value for V);
     *   Throwable throwable = null;
     *   try {
     *     result = target(a..., b...);
     *   } catch (Throwable t) {
     *     throwable = t;
     *     throw t;
     *   } finally {
     *     result = cleanup(throwable, result, a...);
     *   }
     *   return result;
     * }
     * }</pre></blockquote>
     * <p>
     * Note that the saved arguments ({@code a...} in the pseudocode) cannot
     * be modified by execution of the target, and so are passed unchanged
     * from the caller to the cleanup, if it is invoked.
     * <p>
     * The target and cleanup must return the same type, even if the cleanup
     * always throws.
     * To create such a throwing cleanup, compose the cleanup logic
     * with {@link #throwException throwException},
     * in order to create a method handle of the correct return type.
     * <p>
     * Note that {@code tryFinally} never converts exceptions into normal returns.
     * In rare cases where exceptions must be converted in that way, first wrap
     * the target with {@link #catchException(MethodHandle, Class, MethodHandle)}
     * to capture an outgoing exception, and then wrap with {@code tryFinally}.
     * <p>
     * It is recommended that the first parameter type of {@code cleanup} be
     * declared {@code Throwable} rather than a narrower subtype.  This ensures
     * {@code cleanup} will always be invoked with whatever exception that
     * {@code target} throws.  Declaring a narrower type may result in a
     * {@code ClassCastException} being thrown by the {@code try-finally}
     * handle if the type of the exception thrown by {@code target} is not
     * assignable to the first parameter type of {@code cleanup}.  Note that
     * various exception types of {@code VirtualMachineError},
     * {@code LinkageError}, and {@code RuntimeException} can in principle be
     * thrown by almost any kind of Java code, and a finally clause that
     * catches (say) only {@code IOException} would mask any of the others
     * behind a {@code ClassCastException}.
     *
     * @param target the handle whose execution is to be wrapped in a {@code try} block.
     * @param cleanup the handle that is invoked in the finally block.
     *
     * @return a method handle embodying the {@code try-finally} block composed of the two arguments.
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code cleanup} does not accept
     *          the required leading arguments, or if the method handle types do
     *          not match in their return types and their
     *          corresponding trailing parameters
     *
     * @see MethodHandles#catchException(MethodHandle, Class, MethodHandle)
     * @since 9
     */
    public static MethodHandle tryFinally(MethodHandle target, MethodHandle cleanup) {
        List<Class<?>> targetParamTypes = target.type().parameterList();
        Class<?> rtype = target.type().returnType();

        tryFinallyChecks(target, cleanup);

        // Match parameter lists: if the cleanup has a shorter parameter list than the target, add ignored arguments.
        // The cleanup parameter list (minus the leading Throwable and result parameters) must be a sublist of the
        // target parameter list.
        cleanup = dropArgumentsToMatch(cleanup, (rtype == void.class ? 1 : 2), targetParamTypes, 0);

        // Ensure that the intrinsic type checks the instance thrown by the
        // target against the first parameter of cleanup
        cleanup = cleanup.asType(cleanup.type().changeParameterType(0, Throwable.class));

        // Use asFixedArity() to avoid unnecessary boxing of last argument for VarargsCollector case.
        return MethodHandleImpl.makeTryFinally(target.asFixedArity(), cleanup.asFixedArity(), rtype, targetParamTypes);
    }

    private static void tryFinallyChecks(MethodHandle target, MethodHandle cleanup) {
        Class<?> rtype = target.type().returnType();
        if (rtype != cleanup.type().returnType()) {
            throw misMatchedTypes("target and return types", cleanup.type().returnType(), rtype);
        }
        MethodType cleanupType = cleanup.type();
        if (!Throwable.class.isAssignableFrom(cleanupType.parameterType(0))) {
            throw misMatchedTypes("cleanup first argument and Throwable", cleanup.type(), Throwable.class);
        }
        if (rtype != void.class && cleanupType.parameterType(1) != rtype) {
            throw misMatchedTypes("cleanup second argument and target return type", cleanup.type(), rtype);
        }
        // The cleanup parameter list (minus the leading Throwable and result parameters) must be a sublist of the
        // target parameter list.
        int cleanupArgIndex = rtype == void.class ? 1 : 2;
        if (!cleanupType.effectivelyIdenticalParameters(cleanupArgIndex, target.type().parameterList())) {
            throw misMatchedTypes("cleanup parameters after (Throwable,result) and target parameter list prefix",
                    cleanup.type(), target.type());
        }
    }

    /**
     * Creates a table switch method handle, which can be used to switch over a set of target
     * method handles, based on a given target index, called selector.
     * <p>
     * For a selector value of {@code n}, where {@code n} falls in the range {@code [0, N)},
     * and where {@code N} is the number of target method handles, the table switch method
     * handle will invoke the n-th target method handle from the list of target method handles.
     * <p>
     * For a selector value that does not fall in the range {@code [0, N)}, the table switch
     * method handle will invoke the given fallback method handle.
     * <p>
     * All method handles passed to this method must have the same type, with the additional
     * requirement that the leading parameter be of type {@code int}. The leading parameter
     * represents the selector.
     * <p>
     * Any trailing parameters present in the type will appear on the returned table switch
     * method handle as well. Any arguments assigned to these parameters will be forwarded,
     * together with the selector value, to the selected method handle when invoking it.
     *
     * @apiNote Example:
     * The cases each drop the {@code selector} value they are given, and take an additional
     * {@code String} argument, which is concatenated (using {@link String#concat(String)})
     * to a specific constant label string for each case:
     * <blockquote><pre>{@code
     * MethodHandles.Lookup lookup = MethodHandles.lookup();
     * MethodHandle caseMh = lookup.findVirtual(String.class, "concat",
     *         MethodType.methodType(String.class, String.class));
     * caseMh = MethodHandles.dropArguments(caseMh, 0, int.class);
     *
     * MethodHandle caseDefault = MethodHandles.insertArguments(caseMh, 1, "default: ");
     * MethodHandle case0 = MethodHandles.insertArguments(caseMh, 1, "case 0: ");
     * MethodHandle case1 = MethodHandles.insertArguments(caseMh, 1, "case 1: ");
     *
     * MethodHandle mhSwitch = MethodHandles.tableSwitch(
     *     caseDefault,
     *     case0,
     *     case1
     * );
     *
     * assertEquals("default: data", (String) mhSwitch.invokeExact(-1, "data"));
     * assertEquals("case 0: data", (String) mhSwitch.invokeExact(0, "data"));
     * assertEquals("case 1: data", (String) mhSwitch.invokeExact(1, "data"));
     * assertEquals("default: data", (String) mhSwitch.invokeExact(2, "data"));
     * }</pre></blockquote>
     *
     * @param fallback the fallback method handle that is called when the selector is not
     *                 within the range {@code [0, N)}.
     * @param targets array of target method handles.
     * @return the table switch method handle.
     * @throws NullPointerException if {@code fallback}, the {@code targets} array, or any
     *                              any of the elements of the {@code targets} array are
     *                              {@code null}.
     * @throws IllegalArgumentException if the {@code targets} array is empty, if the leading
     *                                  parameter of the fallback handle or any of the target
     *                                  handles is not {@code int}, or if the types of
     *                                  the fallback handle and all of target handles are
     *                                  not the same.
     */
    public static MethodHandle tableSwitch(MethodHandle fallback, MethodHandle... targets) {
        Objects.requireNonNull(fallback);
        Objects.requireNonNull(targets);
        targets = targets.clone();
        MethodType type = tableSwitchChecks(fallback, targets);
        return MethodHandleImpl.makeTableSwitch(type, fallback, targets);
    }

    private static MethodType tableSwitchChecks(MethodHandle defaultCase, MethodHandle[] caseActions) {
        if (caseActions.length == 0)
            throw new IllegalArgumentException("Not enough cases: " + Arrays.toString(caseActions));

        MethodType expectedType = defaultCase.type();

        if (!(expectedType.parameterCount() >= 1) || expectedType.parameterType(0) != int.class)
            throw new IllegalArgumentException(
                "Case actions must have int as leading parameter: " + Arrays.toString(caseActions));

        for (MethodHandle mh : caseActions) {
            Objects.requireNonNull(mh);
            if (mh.type() != expectedType)
                throw new IllegalArgumentException(
                    "Case actions must have the same type: " + Arrays.toString(caseActions));
        }

        return expectedType;
    }

}
