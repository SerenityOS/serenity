/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.vm.annotation;

import java.lang.annotation.*;

/**
 * The {@code @IntrinsicCandidate} annotation is specific to the
 * HotSpot Virtual Machine. It indicates that an annotated method
 * may be (but is not guaranteed to be) intrinsified by the HotSpot VM. A method
 * is intrinsified if the HotSpot VM replaces the annotated method with hand-written
 * assembly and/or hand-written compiler IR -- a compiler intrinsic -- to improve
 * performance. The {@code @IntrinsicCandidate} annotation is internal to the
 * Java libraries and is therefore not supposed to have any relevance for application
 * code.
 *
 * Maintainers of the Java libraries must consider the following when
 * modifying methods annotated with {@code @IntrinsicCandidate}.
 *
 * <ul>
 * <li>When modifying a method annotated with {@code @IntrinsicCandidate},
 * the corresponding intrinsic code in the HotSpot VM implementation must be
 * updated to match the semantics of the annotated method.</li>
 * <li>For some annotated methods, the corresponding intrinsic may omit some low-level
 * checks that would be performed as a matter of course if the intrinsic is implemented
 * using Java bytecodes. This is because individual Java bytecodes implicitly check
 * for exceptions like {@code NullPointerException} and {@code ArrayStoreException}.
 * If such a method is replaced by an intrinsic coded in assembly language, any
 * checks performed as a matter of normal bytecode operation must be performed
 * before entry into the assembly code. These checks must be performed, as
 * appropriate, on all arguments to the intrinsic, and on other values (if any) obtained
 * by the intrinsic through those arguments. The checks may be deduced by inspecting
 * the non-intrinsic Java code for the method, and determining exactly which exceptions
 * may be thrown by the code, including undeclared implicit {@code RuntimeException}s.
 * Therefore, depending on the data accesses performed by the intrinsic,
 * the checks may include:
 *
 *  <ul>
 *  <li>null checks on references</li>
 *  <li>range checks on primitive values used as array indexes</li>
 *  <li>other validity checks on primitive values (e.g., for divide-by-zero conditions)</li>
 *  <li>store checks on reference values stored into arrays</li>
 *  <li>array length checks on arrays indexed from within the intrinsic</li>
 *  <li>reference casts (when formal parameters are {@code Object} or some other weak type)</li>
 *  </ul>
 *
 * </li>
 *
 * <li>Note that the receiver value ({@code this}) is passed as a extra argument
 * to all non-static methods. If a non-static method is an intrinsic, the receiver
 * value does not need a null check, but (as stated above) any values loaded by the
 * intrinsic from object fields must also be checked. As a matter of clarity, it is
 * better to make intrinisics be static methods, to make the dependency on {@code this}
 * clear. Also, it is better to explicitly load all required values from object
 * fields before entering the intrinsic code, and pass those values as explicit arguments.
 * First, this may be necessary for null checks (or other checks). Second, if the
 * intrinsic reloads the values from fields and operates on those without checks,
 * race conditions may be able to introduce unchecked invalid values into the intrinsic.
 * If the intrinsic needs to store a value back to an object field, that value should be
 * returned explicitly from the intrinsic; if there are multiple return values, coders
 * should consider buffering them in an array. Removing field access from intrinsics
 * not only clarifies the interface with between the JVM and JDK; it also helps decouple
 * the HotSpot and JDK implementations, since if JDK code before and after the intrinsic
 * manages all field accesses, then intrinsics can be coded to be agnostic of object
 * layouts.</li>
 *
 * Maintainers of the HotSpot VM must consider the following when modifying
 * intrinsics.
 *
 * <ul>
 * <li>When adding a new intrinsic, make sure that the corresponding method
 * in the Java libraries is annotated with {@code @IntrinsicCandidate}
 * and that all possible call sequences that result in calling the intrinsic contain
 * the checks omitted by the intrinsic (if any).</li>
 * <li>When modifying an existing intrinsic, the Java libraries must be updated
 * to match the semantics of the intrinsic and to execute all checks omitted
 * by the intrinsic (if any).</li>
 * </ul>
 *
 * Persons not directly involved with maintaining the Java libraries or the
 * HotSpot VM can safely ignore the fact that a method is annotated with
 * {@code @IntrinsicCandidate}.
 *
 * The HotSpot VM defines (internally) a list of intrinsics. Not all intrinsic
 * are available on all platforms supported by the HotSpot VM. Furthermore,
 * the availability of an intrinsic on a given platform depends on the
 * configuration of the HotSpot VM (e.g., the set of VM flags enabled).
 * Therefore, annotating a method with {@code @IntrinsicCandidate} does
 * not guarantee that the marked method is intrinsified by the HotSpot VM.
 *
 * If the {@code CheckIntrinsics} VM flag is enabled, the HotSpot VM checks
 * (when loading a class) that (1) all methods of that class that are also on
 * the VM's list of intrinsics are annotated with {@code @IntrinsicCandidate}
 * and that (2) for all methods of that class annotated with
 * {@code @IntrinsicCandidate} there is an intrinsic in the list.
 *
 * @since 16
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface IntrinsicCandidate {
}
