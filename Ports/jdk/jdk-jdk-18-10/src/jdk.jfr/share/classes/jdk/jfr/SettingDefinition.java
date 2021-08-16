/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that specifies that a method in an event class should be used to
 * filter out events.
 * <p>
 * For the method to be valid it must return a {@code SettingControl} and only have one
 * parameter, which should be a non-abstract subclass of {@link SettingControl}
 * <p>
 * The return value of the method specifies whether the event is to be
 * written to the Flight Recorder system or not.
 * <p>
 * The following example shows how to annotate a method in an event class.
 *
 * <pre>{@literal
 * class HelloWorld extends Event {
 *
 *   @Label("Message")
 *   String message;
 *
 *   @SettingDefinition
 *   @Label("Message Filter")
 *   public boolean filter(RegExpControl regExp) {
 *     return regExp.matches(message);
 *   }
 * }
 * }</pre>
 *
 * For an example of how the setting controls are defined, see
 * {@link SettingControl}.
 *
 * @see SettingControl
 *
 * @since 9
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface SettingDefinition {
}
