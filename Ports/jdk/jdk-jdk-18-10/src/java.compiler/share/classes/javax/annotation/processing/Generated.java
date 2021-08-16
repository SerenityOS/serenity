/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
package javax.annotation.processing;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * The Generated annotation is used to mark source code that has been generated.
 * It can also be used to differentiate user written code from generated code in
 * a single file.
 *
 * <h2>Examples:</h2>
 * <pre>
 *   &#064;Generated("com.example.Generator")
 * </pre>
 * <pre>
 *   &#064;Generated(value="com.example.Generator", date= "2017-07-04T12:08:56.235-0700")
 * </pre>
 * <pre>
 *   &#064;Generated(value="com.example.Generator", date= "2017-07-04T12:08:56.235-0700",
 *      comments= "comment 1")
 * </pre>
 *
 * @since 9
 */
@Documented
@Retention(SOURCE)
@Target({PACKAGE, TYPE, METHOD, CONSTRUCTOR, FIELD,
    LOCAL_VARIABLE, PARAMETER})
public @interface Generated {

    /**
     * The value element MUST have the name of the code generator. The
     * name is the fully qualified name of the code generator.
     *
     * @return The name of the code generator
     */
    String[] value();

    /**
     * Date when the source was generated. The date element must follow the ISO
     * 8601 standard. For example the date element would have the following
     * value 2017-07-04T12:08:56.235-0700 which represents 2017-07-04 12:08:56
     * local time in the U.S. Pacific Time time zone.
     *
     * @return The date the source was generated
     */
    String date() default "";

    /**
     * A place holder for any comments that the code generator may want to
     * include in the generated code.
     *
     * @return Comments that the code generated included
     */
    String comments() default "";
}
