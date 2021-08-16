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
 * This annotation is coupled with {@link BasicObjectFactory} class,
 * and allows one to create implementations of ObjectFactory via annotations.
 * <pre> a simple example:
&#064;Factory(description="dummy factory", default_value="array_list", placeholder_text="a type",
classlist={
&#064;FClass(description="a linked list", key="linked_list", type=LinkedList.class),
&#064;FClass(description="an array  list", key="array_list", type=ArrayList.class)
})
public class BasicObjectFactoryUsageExample extends BasicObjectFactory<Collection>
{
}
 * </pre>
 * @see BasicObjectFactory
 * @see FClass
 * @see vm.share.options.test.BasicObjectFactoryUsageExample
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Factory
{
    final public static String defDefaultValue = "[no factory default]";
    final public static String defDescription = "[no factory description]";
    /**
     * Used for generating placeholder text in <..> part of help message,
     * is mandatory.
     */
    String placeholder_text(); // mandatory ;
    /**
     * Default value, used if the option is not specified AND if no default
     * value has been specified in the corresponding @Option annotation.
     */
    String default_value() default defDefaultValue;

    /**
     * A help message string for the factory.
     */
    String description() default defDescription;

    /**
     * The list of classes and keys to instantiate.
     * @see FClass
     */
    FClass[] classlist(); // mandatory
}
