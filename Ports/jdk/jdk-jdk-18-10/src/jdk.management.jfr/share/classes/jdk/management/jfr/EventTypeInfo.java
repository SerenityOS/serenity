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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import javax.management.openmbean.CompositeData;

import jdk.jfr.Category;
import jdk.jfr.EventType;
import jdk.jfr.SettingDescriptor;

/**
 * Management representation of an {@code EventType}.
 *
 * @see EventType
 *
 * @since 9
 */
public final class EventTypeInfo {
    private final List<SettingDescriptorInfo> settings;
    private final long id;
    private final String name;
    private final String description;
    private final String label;
    private final List<String> categoryNames;

    // package private
    EventTypeInfo(EventType eventType) {
        this.settings = creatingSettingDescriptorInfos(eventType);
        this.id = eventType.getId();
        this.name = eventType.getName();
        this.label = eventType.getLabel();
        this.description = eventType.getDescription();
        this.categoryNames = eventType.getCategoryNames();
    }

    private EventTypeInfo(CompositeData cd) {
        this.settings = createSettings(cd.get("settings"));
        this.id = (long) cd.get("id");
        this.name = (String) cd.get("name");
        this.label = (String) cd.get("label");
        this.description = (String) cd.get("description");
        this.categoryNames = createCategoryNames((Object[]) cd.get("category"));
    }

    private static List<String> createCategoryNames(Object[] array) {
        List<String> list = new ArrayList<>(array.length);
        for (int i = 0; i < array.length; i++) {
            list.add((String) array[i]);
        }
        return Collections.unmodifiableList(list);
    }

    private static List<SettingDescriptorInfo> creatingSettingDescriptorInfos(EventType eventType) {
        List<SettingDescriptor> settings = eventType.getSettingDescriptors();
        List<SettingDescriptorInfo> settingDescriptorInfos = new ArrayList<>(settings.size());
        for (SettingDescriptor s : settings) {
            settingDescriptorInfos.add(new SettingDescriptorInfo(s));
        }
        return Collections.unmodifiableList(settingDescriptorInfos);
    }

    private static List<SettingDescriptorInfo> createSettings(Object settings) {
        if (settings != null && settings.getClass().isArray()) {
            Object[] settingsArray = (Object[]) settings;
            List<SettingDescriptorInfo> list = new ArrayList<>(settingsArray.length);
            for (Object cd : settingsArray) {
                if (cd instanceof CompositeData) {
                    list.add(SettingDescriptorInfo.from((CompositeData) cd));
                }
            }
            return Collections.unmodifiableList(list);
        }
        return Collections.emptyList();
    }

    /**
     * Returns the label, a human-readable name, associated with the event type
     * for this {@code EventTypeInfo} (for example, {@code "Garbage Collection"}).
     *
     * @return the label, or {@code null} if a label is not set
     *
     * @see EventType#getLabel()
     */
    public String getLabel() {
        return label;
    }

    /**
     *
     * Returns the list of human-readable names that makes up the category for this
     * {@code EventTypeInfo} (for example, {@code "Java Virtual Machine"} or {@code "Garbage Collector"}).
     *
     * @return an immutable list of category names, or a list with the name
     *         {@code "Uncategorized"} if no category has been set
     *
     * @see EventType#getCategoryNames()
     * @see Category
     */
    public List<String> getCategoryNames() {
        return categoryNames;
    }

    /**
     * Returns the unique ID for the event type associated with this
     * {@code EventTypeInfo}, not guaranteed to be the same for different Java
     * Virtual Machines (JVMs) instances.
     *
     * @return the ID
     *
     * @see EventType#getId()
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the name for the event type associated with this
     * {@code EventTypeInfo} (for example, {@code "jdk.GarbageCollection"}).
     *
     * @return the name, not {@code null}
     *
     * @see EventType#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a short sentence or two describing the event type associated with
     * this {@code EventTypeInfo}, for example
     * {@code "Garbage collection performed by the JVM""}.
     *
     * @return the description, or {@code null} if no description exists
     *
     * @see EventType#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns settings for the event type associated with this
     * {@code EventTypeInfo}.
     *
     * @return the settings, not {@code null}
     *
     * @see EventType#getSettingDescriptors()
     */
    public List<SettingDescriptorInfo> getSettingDescriptors() {
        return settings;
    }

    /**
     * Returns a description of this {@code EventTypeInfo}.
     *
     * @return description, not {@code null}
     */
    @Override
    public String toString() {
        Stringifier s = new Stringifier();
        s.add("id", id);
        s.add("name", name);
        s.add("label", label);
        s.add("description", description);
        StringJoiner sj = new StringJoiner(", ", "{", "}");
        for (String categoryName : categoryNames) {
            sj.add(categoryName);
        }
        s.add("category", sj.toString());
        return s.toString();
    }

    /**
     * Returns an {@code EventType} represented by the specified
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
     * <th scope="row">id</th>
     * <td>{@code Long}</td>
     * </tr>
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
     * <th scope="row">category</th>
     * <td><code>ArrayType(1, SimpleType.STRING)</code></td>
     * </tr>
     * <tr>
     * <th scope="row">settings</th>
     * <td>{@code javax.management.openmbean.CompositeData[]} whose element type
     * is the mapped type for {@link SettingDescriptorInfo} as specified in the
     * {@link SettingDescriptorInfo#from} method.</td>
     *
     * </tr>
     * </tbody>
     * </table>
     * </blockquote>
     *
     * @param cd {@code CompositeData} representing the {@code EventTypeInfo} to
     *        return
     *
     * @throws IllegalArgumentException if {@code cd} does not represent a valid
     *         {@code EventTypeInfo}
     *
     * @return an {@code EventTypeInfo}, or {@code null} if {@code cd} is
     *         {@code null}
     */
    public static EventTypeInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        return new EventTypeInfo(cd);
    }
}
