/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.options;
import java.lang.annotation.*;
/**
 * This annotation is the most useful one of the whole API.
 * It is used to mark non-static fields of the object and to declare the
 * corresponding options. The name of the option defaults to the name of the field.
 * The help message for the option should also be also declared
 * here (or it could be declared at the {@link ObjectFactory} level at any case it is mandatory).
 * For non-simple option types a factory attribute should be provided which
 * points to a class which implements {@link ObjectFactory} interface. That factory is
 * then used to instantiate an object given the option value and to populate the
 * annotated field.
 *
 * If a default value is provided it is used if the corresponding option is not declared
 * at the command line. If it is not provided and there is no option then an error is thrown.
 * For non-simple field types default values can be provided at ObjectFactory level.
 * See also the documentation at the package level.).
 */
// kept at runtime, applied to fields only.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option
{
    // these strings help to find out if the corresponding attribute was not defined.
    final public static String defName = "[no name]";
    final public static String defDefaultValue = "[no default]";
    final public static String defDescription = "[no description]";

    /**
     * The name of the option, defaults to the name of the annotated field.
     */
    String name() default defName;
    /**
     * The default value for the option, option is mandatory if it is not specified here
     * and at the ObjectFactory level.
     */
    String default_value() default defDefaultValue;
    /**
     * A short description of the option used to generate a help message.
     */
    String description() default defDescription;

    /**
     * The factory class to use for instantiating the corresponding option.
     */
    Class<? extends OptionObjectFactory> factory() default OptionObjectFactory.class;
}
