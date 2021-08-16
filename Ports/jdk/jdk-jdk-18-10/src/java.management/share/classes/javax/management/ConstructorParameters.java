/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * <p>
 * An annotation on a constructor that shows how the parameters of
 * that constructor correspond to the constructed object's getter
 * methods.  For example:
 * </p>
 * <blockquote>
 *     <pre>
 *         public class MemoryUsage {
 *             // standard JavaBean conventions with getters
 *             <b>@ConstructorParameters({"init", "used", "committed", "max"})</b>
 *             public MemoryUsage(long init, long used,
 *                                long committed, long max) {...}
 *             public long getInit() {...}
 *             public long getUsed() {...}
 *             public long getCommitted() {...}
 *             public long getMax() {...}
 *         }
 *     </pre>
 * </blockquote>
 * <p>
 * The annotation shows that the first parameter of the constructor
 * can be retrieved with the {@code getInit()} method, the second one with
 * the {@code getUsed()} method, and so on. Since parameter names are not in
 * general available at runtime, without the annotation there would be
 * no way of knowing which parameter corresponds to which property.
 * </p>
 * <p>
 * If a constructor is annotated by the both {@code @java.beans.ConstructorProperties}
 * and {@code @javax.management.ConstructorParameters} annotations
 * the JMX introspection will give an absolute precedence to the latter one.
 * </p>
 *
 * @since 9
 */
@Documented @Target(CONSTRUCTOR) @Retention(RUNTIME)
public @interface ConstructorParameters {
    /**
     * <p>The getter names.</p>
     *
     * @return the getter names corresponding to the parameters in the
     * annotated constructor.
    */
    String[] value();
}
