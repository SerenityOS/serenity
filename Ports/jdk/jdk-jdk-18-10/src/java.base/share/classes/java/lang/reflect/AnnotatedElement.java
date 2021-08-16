/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.lang.annotation.Repeatable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import sun.reflect.annotation.AnnotationSupport;
import sun.reflect.annotation.AnnotationType;

/**
 * Represents an annotated construct of the program currently running
 * in this VM.
 *
 * A construct is either an element or a type. Annotations on an
 * element are on a <em>declaration</em>, whereas annotations on a
 * type are on a specific <em>use</em> of a type name.
 *
 * As defined by <cite>The Java Language Specification</cite>
 * section {@jls 9.7.4}, an annotation on an element is a
 * <em>declaration annotation</em> and an annotation on a type is a
 * <em>type annotation</em>.
 *
 * Note that any annotations returned by methods on the {@link
 * AnnotatedType AnnotatedType} interface and its subinterfaces are
 * type annotations as the entity being potentially annotated is a
 * type. Annotations returned by methods outside of the {@code
 * AnnotatedType} hierarchy are declaration annotations.
 *
 * <p>This interface allows annotations to be read reflectively.  All
 * annotations returned by methods in this interface are immutable and
 * serializable. The arrays returned by methods of this interface may
 * be modified by callers without affecting the arrays returned to
 * other callers.
 *
 * <p>The {@link #getAnnotationsByType(Class)} and {@link
 * #getDeclaredAnnotationsByType(Class)} methods support multiple
 * annotations of the same type on an element. If the argument to
 * either method is a repeatable annotation type (JLS {@jls 9.6}),
 * then the method will "look through" a container annotation (JLS
 * {@jls 9.7}), if present, and return any annotations inside the
 * container. Container annotations may be generated at compile-time
 * to wrap multiple annotations of the argument type.
 *
 * <p>The terms <em>directly present</em>, <em>indirectly present</em>,
 * <em>present</em>, and <em>associated</em> are used throughout this
 * interface to describe precisely which annotations are returned by
 * methods:
 *
 * <ul>
 *
 * <li> An annotation <i>A</i> is <em>directly present</em> on an
 * element <i>E</i> if <i>E</i> has a {@code
 * RuntimeVisibleAnnotations} or {@code
 * RuntimeVisibleParameterAnnotations} or {@code
 * RuntimeVisibleTypeAnnotations} attribute, and the attribute
 * contains <i>A</i>.
 *
 * <li>An annotation <i>A</i> is <em>indirectly present</em> on an
 * element <i>E</i> if <i>E</i> has a {@code RuntimeVisibleAnnotations} or
 * {@code RuntimeVisibleParameterAnnotations} or {@code RuntimeVisibleTypeAnnotations}
 * attribute, and <i>A</i> 's type is repeatable, and the attribute contains
 * exactly one annotation whose value element contains <i>A</i> and whose
 * type is the containing annotation type of <i>A</i> 's type.
 *
 * <li>An annotation <i>A</i> is <em>present</em> on an element <i>E</i> if either:
 *
 * <ul>
 *
 * <li><i>A</i> is directly present on <i>E</i>; or
 *
 * <li>No annotation of <i>A</i> 's type is directly present on
 * <i>E</i>, and <i>E</i> is a class, and <i>A</i> 's type is
 * inheritable, and <i>A</i> is present on the superclass of <i>E</i>.
 *
 * </ul>
 *
 * <li>An annotation <i>A</i> is <em>associated</em> with an element <i>E</i>
 * if either:
 *
 * <ul>
 *
 * <li><i>A</i> is directly or indirectly present on <i>E</i>; or
 *
 * <li>No annotation of <i>A</i> 's type is directly or indirectly
 * present on <i>E</i>, and <i>E</i> is a class, and <i>A</i>'s type
 * is inheritable, and <i>A</i> is associated with the superclass of
 * <i>E</i>.
 *
 * </ul>
 *
 * </ul>
 *
 * <p>The table below summarizes which kind of annotation presence
 * different methods in this interface examine.
 *
 * <table class="plain">
 * <caption>Overview of kind of presence detected by different AnnotatedElement methods</caption>
 * <thead>
 * <tr><th colspan=2 scope="col">Method</th>
 *     <th colspan=4 scope="col">Kind of Presence</th>
 * <tr><th scope="col">Return Type</th>
 *     <th scope="col">Signature</th>
 *     <th scope="col">Directly Present</th>
 *     <th scope="col">Indirectly Present</th>
 *     <th scope="col">Present</th>
 *     <th scope="col">Associated</th>
 * </thead>
 * <tbody>
 * <tr><td style="text-align:right">{@code T}</td>
 * <th scope="row" style="font-weight:normal; text-align:left">{@link #getAnnotation(Class) getAnnotation(Class&lt;T&gt;)}
 * <td></td><td></td><td style="text-align:center">X</td><td></td>
 * </tr>
 * <tr><td style="text-align:right">{@code Annotation[]}</td>
 * <th scope="row" style="font-weight:normal; text-align:left">{@link #getAnnotations getAnnotations()}
 * <td></td><td></td><td style="text-align:center">X</td><td></td>
 * </tr>
 * <tr><td style="text-align:right">{@code T[]}</td>
 * <th scope="row" style="font-weight:normal; text-align:left">{@link #getAnnotationsByType(Class) getAnnotationsByType(Class&lt;T&gt;)}
 * <td></td><td></td><td></td><td style="text-align:center">X</td>
 * </tr>
 * <tr><td style="text-align:right">{@code T}</td>
 * <th scope="row" style="font-weight:normal; text-align:left">{@link #getDeclaredAnnotation(Class) getDeclaredAnnotation(Class&lt;T&gt;)}
 * <td style="text-align:center">X</td><td></td><td></td><td></td>
 * </tr>
 * <tr><td style="text-align:right">{@code Annotation[]}</td>
 * <th scope="row" style="font-weight:normal; text-align:left">{@link #getDeclaredAnnotations getDeclaredAnnotations()}
 * <td style="text-align:center">X</td><td></td><td></td><td></td>
 * </tr>
 * <tr><td style="text-align:right">{@code T[]}</td>
 * <th scope="row" style="font-weight:normal; text-align:left">{@link #getDeclaredAnnotationsByType(Class) getDeclaredAnnotationsByType(Class&lt;T&gt;)}
 * <td style="text-align:center">X</td><td style="text-align:center">X</td><td></td><td></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <p>For an invocation of {@code get[Declared]AnnotationsByType(Class <T>)},
 * the order of annotations which are directly or indirectly
 * present on an element <i>E</i> is computed as if indirectly present
 * annotations on <i>E</i> are directly present on <i>E</i> in place
 * of their container annotation, in the order in which they appear in
 * the value element of the container annotation.
 *
 * <p>There are several compatibility concerns to keep in mind if an
 * annotation type <i>T</i> is originally <em>not</em> repeatable and
 * later modified to be repeatable.
 *
 * The containing annotation type for <i>T</i> is <i>TC</i>.
 *
 * <ul>
 *
 * <li>Modifying <i>T</i> to be repeatable is source and binary
 * compatible with existing uses of <i>T</i> and with existing uses
 * of <i>TC</i>.
 *
 * That is, for source compatibility, source code with annotations of
 * type <i>T</i> or of type <i>TC</i> will still compile. For binary
 * compatibility, class files with annotations of type <i>T</i> or of
 * type <i>TC</i> (or with other kinds of uses of type <i>T</i> or of
 * type <i>TC</i>) will link against the modified version of <i>T</i>
 * if they linked against the earlier version.
 *
 * (An annotation type <i>TC</i> may informally serve as an acting
 * containing annotation type before <i>T</i> is modified to be
 * formally repeatable. Alternatively, when <i>T</i> is made
 * repeatable, <i>TC</i> can be introduced as a new type.)
 *
 * <li>If an annotation type <i>TC</i> is present on an element, and
 * <i>T</i> is modified to be repeatable with <i>TC</i> as its
 * containing annotation type then:
 *
 * <ul>
 *
 * <li>The change to <i>T</i> is behaviorally compatible with respect
 * to the {@code get[Declared]Annotation(Class<T>)} (called with an
 * argument of <i>T</i> or <i>TC</i>) and {@code
 * get[Declared]Annotations()} methods because the results of the
 * methods will not change due to <i>TC</i> becoming the containing
 * annotation type for <i>T</i>.
 *
 * <li>The change to <i>T</i> changes the results of the {@code
 * get[Declared]AnnotationsByType(Class<T>)} methods called with an
 * argument of <i>T</i>, because those methods will now recognize an
 * annotation of type <i>TC</i> as a container annotation for <i>T</i>
 * and will "look through" it to expose annotations of type <i>T</i>.
 *
 * </ul>
 *
 * <li>If an annotation of type <i>T</i> is present on an
 * element and <i>T</i> is made repeatable and more annotations of
 * type <i>T</i> are added to the element:
 *
 * <ul>
 *
 * <li> The addition of the annotations of type <i>T</i> is both
 * source compatible and binary compatible.
 *
 * <li>The addition of the annotations of type <i>T</i> changes the results
 * of the {@code get[Declared]Annotation(Class<T>)} methods and {@code
 * get[Declared]Annotations()} methods, because those methods will now
 * only see a container annotation on the element and not see an
 * annotation of type <i>T</i>.
 *
 * <li>The addition of the annotations of type <i>T</i> changes the
 * results of the {@code get[Declared]AnnotationsByType(Class<T>)}
 * methods, because their results will expose the additional
 * annotations of type <i>T</i> whereas previously they exposed only a
 * single annotation of type <i>T</i>.
 *
 * </ul>
 *
 * </ul>
 *
 * <p>If an annotation returned by a method in this interface contains
 * (directly or indirectly) a {@link Class}-valued member referring to
 * a class that is not accessible in this VM, attempting to read the class
 * by calling the relevant Class-returning method on the returned annotation
 * will result in a {@link TypeNotPresentException}.
 *
 * <p>Similarly, attempting to read an enum-valued member will result in
 * a {@link EnumConstantNotPresentException} if the enum constant in the
 * annotation is no longer present in the enum class.
 *
 * <p>If an annotation type <i>T</i> is (meta-)annotated with an
 * {@code @Repeatable} annotation whose value element indicates a type
 * <i>TC</i>, but <i>TC</i> does not declare a {@code value()} method
 * with a return type of <i>T</i>{@code []}, then an exception of type
 * {@link java.lang.annotation.AnnotationFormatError} is thrown.
 *
 * <p>Finally, attempting to read a member whose definition has evolved
 * incompatibly will result in a {@link
 * java.lang.annotation.AnnotationTypeMismatchException} or an
 * {@link java.lang.annotation.IncompleteAnnotationException}.
 *
 * @see java.lang.EnumConstantNotPresentException
 * @see java.lang.TypeNotPresentException
 * @see AnnotationFormatError
 * @see java.lang.annotation.AnnotationTypeMismatchException
 * @see java.lang.annotation.IncompleteAnnotationException
 * @since 1.5
 * @author Josh Bloch
 */
public interface AnnotatedElement {
    /**
     * Returns true if an annotation for the specified type
     * is <em>present</em> on this element, else false.  This method
     * is designed primarily for convenient access to marker annotations.
     *
     * <p>The truth value returned by this method is equivalent to:
     * {@code getAnnotation(annotationClass) != null}
     *
     * @implSpec The default implementation returns {@code
     * getAnnotation(annotationClass) != null}.
     *
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return true if an annotation for the specified annotation
     *     type is present on this element, else false
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    default boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>present</em>, else null.
     *
     * @param <T> the type of the annotation to query for and return if present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this element's annotation for the specified annotation type if
     *     present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * Returns annotations that are <em>present</em> on this element.
     *
     * If there are no annotations <em>present</em> on this element, the return
     * value is an array of length 0.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations present on this element
     * @since 1.5
     */
    Annotation[] getAnnotations();

    /**
     * Returns annotations that are <em>associated</em> with this element.
     *
     * If there are no annotations <em>associated</em> with this element, the return
     * value is an array of length 0.
     *
     * The difference between this method and {@link #getAnnotation(Class)}
     * is that this method detects if its argument is a <em>repeatable
     * annotation type</em> (JLS {@jls 9.6}), and if so, attempts to find one or
     * more annotations of that type by "looking through" a container
     * annotation.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @implSpec The default implementation first calls {@link
     * #getDeclaredAnnotationsByType(Class)} passing {@code
     * annotationClass} as the argument. If the returned array has
     * length greater than zero, the array is returned. If the returned
     * array is zero-length and this {@code AnnotatedElement} is a
     * class and the argument type is an inheritable annotation type,
     * and the superclass of this {@code AnnotatedElement} is non-null,
     * then the returned result is the result of calling {@link
     * #getAnnotationsByType(Class)} on the superclass with {@code
     * annotationClass} as the argument. Otherwise, a zero-length
     * array is returned.
     *
     * @param <T> the type of the annotation to query for and return if present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return all this element's annotations for the specified annotation type if
     *     associated with this element, else an array of length zero
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    default <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
         /*
          * Definition of associated: directly or indirectly present OR
          * neither directly nor indirectly present AND the element is
          * a Class, the annotation type is inheritable, and the
          * annotation type is associated with the superclass of the
          * element.
          */
         T[] result = getDeclaredAnnotationsByType(annotationClass);

         if (result.length == 0 && // Neither directly nor indirectly present
             this instanceof Class && // the element is a class
             AnnotationType.getInstance(annotationClass).isInherited()) { // Inheritable
             Class<?> superClass = ((Class<?>) this).getSuperclass();
             if (superClass != null) {
                 // Determine if the annotation is associated with the
                 // superclass
                 result = superClass.getAnnotationsByType(annotationClass);
             }
         }

         return result;
     }

    /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>directly present</em>, else null.
     *
     * This method ignores inherited annotations. (Returns null if no
     * annotations are directly present on this element.)
     *
     * @implSpec The default implementation first performs a null check
     * and then loops over the results of {@link
     * #getDeclaredAnnotations} returning the first annotation whose
     * annotation type matches the argument type.
     *
     * @param <T> the type of the annotation to query for and return if directly present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this element's annotation for the specified annotation type if
     *     directly present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    default <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
         Objects.requireNonNull(annotationClass);
         // Loop over all directly-present annotations looking for a matching one
         for (Annotation annotation : getDeclaredAnnotations()) {
             if (annotationClass.equals(annotation.annotationType())) {
                 // More robust to do a dynamic cast at runtime instead
                 // of compile-time only.
                 return annotationClass.cast(annotation);
             }
         }
         return null;
     }

    /**
     * Returns this element's annotation(s) for the specified type if
     * such annotations are either <em>directly present</em> or
     * <em>indirectly present</em>. This method ignores inherited
     * annotations.
     *
     * If there are no specified annotations directly or indirectly
     * present on this element, the return value is an array of length
     * 0.
     *
     * The difference between this method and {@link
     * #getDeclaredAnnotation(Class)} is that this method detects if its
     * argument is a <em>repeatable annotation type</em> (JLS {@jls 9.6}), and if so,
     * attempts to find one or more annotations of that type by "looking
     * through" a container annotation if one is present.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @implSpec The default implementation may call {@link
     * #getDeclaredAnnotation(Class)} one or more times to find a
     * directly present annotation and, if the annotation type is
     * repeatable, to find a container annotation. If annotations of
     * the annotation type {@code annotationClass} are found to be both
     * directly and indirectly present, then {@link
     * #getDeclaredAnnotations()} will get called to determine the
     * order of the elements in the returned array.
     *
     * <p>Alternatively, the default implementation may call {@link
     * #getDeclaredAnnotations()} a single time and the returned array
     * examined for both directly and indirectly present
     * annotations. The results of calling {@link
     * #getDeclaredAnnotations()} are assumed to be consistent with the
     * results of calling {@link #getDeclaredAnnotation(Class)}.
     *
     * @param <T> the type of the annotation to query for and return
     * if directly or indirectly present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return all this element's annotations for the specified annotation type if
     *     directly or indirectly present on this element, else an array of length zero
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    default <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        return AnnotationSupport.
            getDirectlyAndIndirectlyPresent(Arrays.stream(getDeclaredAnnotations()).
                                            collect(Collectors.toMap(Annotation::annotationType,
                                                                     Function.identity(),
                                                                     ((first,second) -> first),
                                                                     LinkedHashMap::new)),
                                            annotationClass);
    }

    /**
     * Returns annotations that are <em>directly present</em> on this element.
     * This method ignores inherited annotations.
     *
     * If there are no annotations <em>directly present</em> on this element,
     * the return value is an array of length 0.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations directly present on this element
     * @since 1.5
     */
    Annotation[] getDeclaredAnnotations();
}
