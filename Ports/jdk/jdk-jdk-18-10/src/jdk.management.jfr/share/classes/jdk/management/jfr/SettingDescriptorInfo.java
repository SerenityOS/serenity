/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.management.jfr;

import java.util.concurrent.Callable;

import javax.management.openmbean.CompositeData;

import jdk.jfr.EventType;
import jdk.jfr.SettingDescriptor;
import jdk.management.jfr.internal.FlightRecorderMXBeanProvider;

/**
 * Management class that describes a setting, for example name, description and
 * default value.
 *
 * @see EventType#getSettingDescriptors()
 *
 * @since 9
 */
public final class SettingDescriptorInfo {

    // Purpose of this static initializer is to allow
    // FlightRecorderMXBeanProvider
    // to be in an internal package and not visible, but at the same time allow
    // it to instantiate FlightRecorderMXBeanImpl.
    //
    // The reason the mechanism is in this class is because it is light weight
    // and can easily be triggered from FlightRecorderMXBeanProvider.
    static {
        FlightRecorderMXBeanProvider.setFlightRecorderMXBeanFactory(new Callable<FlightRecorderMXBean>() {
            @Override
            public FlightRecorderMXBean call() throws Exception {
                return new FlightRecorderMXBeanImpl();
            }
        });
    }

    private final String name;
    private final String label;
    private final String description;
    private final String typeName;
    private final String contentType;
    private final String defaultValue;

    // package private
    SettingDescriptorInfo(SettingDescriptor settingDescriptor) {
        this.name = settingDescriptor.getName();
        this.label = settingDescriptor.getLabel();
        this.description = settingDescriptor.getDescription();
        this.typeName = settingDescriptor.getTypeName();
        this.contentType = settingDescriptor.getContentType();
        this.defaultValue = settingDescriptor.getDefaultValue();
    }

    private SettingDescriptorInfo(CompositeData cd) {
        this.name = (String) cd.get("name");
        this.label = (String) cd.get("label");
        this.description = (String) cd.get("description");
        this.typeName = (String) cd.get("typeName");
        this.defaultValue = (String) cd.get("defaultValue");
        this.contentType = (String) cd.get("contentType");
    }

    /**
    * Returns the human-readable name of the setting associated with this
     * {@code SettingDescriptorInfo} (for example, {@code "Threshold"}).
     *
     * @return the label for this setting, not {@code null}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the name of the setting associated with this
     * {@code SettingDescriptorInfo} (for example, {@code "threshold"}).
     *
     * @return the name of this setting, not {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of the setting associated this
     * {@code SettingDescriptorInfo} (for example,
     * {@code "The duration an event must exceed to be be recorded"}).
     *
     * @return the description of this setting, not null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the type name of the setting associated this
     * {@code SettingDescriptorInfo} (for example,
     * {@code "jdk.settings.Threshold"}).
     * <p>
     * The type can be used to identify what type of setting this is.
     *
     * @return the name of this settings type, not {@code null}
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Returns the content type of the setting associated this
     * {@code SettingDescriptorInfo} (for example, {@code "jdk.jfr.Timespan"}).
     * <p>
     * The content type can be used to determine how the setting should be
     * rendered in a graphical user interface.
     *
     * @return the name of this settings type, not {@code null}
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the default value of the setting associated this
     * {@code SettingDescriptorInfo} (for example, {@code "20 ms"}).
     *
     * @return default value for this setting, not {@code null}
     *
     * @see SettingDescriptor#getDefaultValue()
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns an {@code SettingDescriptorInfo} represented by the specified
     * {@code CompositeData}
     * <p>
     * The supplied {@code CompositeData} must have the following item names and
     * item types to be valid. <blockquote>
     * <table class="striped">
     * <caption>The name and type the specified CompositeData must contain</caption>
     * <thead>
     * <tr>
     * <th scope="col" style="text-align:left">Name</th>
     * <th scope="col" style="text-align:left">Type</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <th scope="row">name</th>
     * <td>{@code String}</td>
     * </tr>
     * <tr>
     * <th scope="row">label</th>
     * <td>{@code String}</td>
     * </tr>
     * <tr>
     * <th scope="row">description</th>
     * <td>{@code String}</td>
     * </tr>
     * <tr>
     * <th scope="row">typeName</th>
     * <td>{@code String}</td>
     * </tr>
     * <tr>
     * <th scope="row">contentType</th>
     * <td>{@code String}</td>
     * </tr>
     * <tr>
     * <th scope="row">defaultValue</th>
     * <td>{@code String}</td>
     * </tr>
     * </tbody>
     * </table>
     * </blockquote>
     *
     * @param cd {@code CompositeData} representing the {@code SettingDescriptorInfo} to
     *        return
     *
     * @throws IllegalArgumentException if {@code cd} does not represent a valid
     *         {@code EventTypeInfo}
     *
     * @return a {@code SettingDescriptorInfo}, or {@code null} if {@code cd} is
     *         {@code null}
     */
    public static SettingDescriptorInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        return new SettingDescriptorInfo(cd);
    }

    /**
     * Returns a {@code String} description of this {@code SettingDescriptorInfo}.
     *
     * @return a string describing this setting, not {@code null}
     */
    @Override
    public String toString() {
        Stringifier s = new Stringifier();
        s.add("name", name);
        s.add("label", label);
        s.add("description", description);
        s.add("typeName", typeName);
        s.add("contentType", contentType);
        s.add("defaultValue", defaultValue);
        return s.toString();
    }
}
