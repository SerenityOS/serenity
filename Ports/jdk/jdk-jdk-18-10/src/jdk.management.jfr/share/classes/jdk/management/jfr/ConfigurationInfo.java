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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import jdk.jfr.Configuration;


/**
 * Management representation of a {@code Configuration}.
 *
 * @see Configuration
 *
 * @since 9
 */
public final class ConfigurationInfo {
    private final Map<String, String> settings;
    private final String name;
    private final String label;
    private final String description;
    private final String provider;
    private final String contents;

    ConfigurationInfo(Configuration config) {
        this.settings = config.getSettings();
        this.name = config.getName();
        this.label = config.getLabel();
        this.description = config.getDescription();
        this.provider = config.getProvider();
        this.contents = config.getContents();
    }

    private ConfigurationInfo(CompositeData cd) {
        this.settings = createMap(cd.get("settings"));
        this.name = (String) cd.get("name");
        this.label = (String) cd.get("label");
        this.description = (String) cd.get("description");
        this.provider = (String) cd.get("provider");
        this.contents = (String) cd.get("contents");
    }

    private static Map<String, String> createMap(Object o) {
        if (o instanceof TabularData) {
            TabularData td = (TabularData) o;
            Collection<?> values = td.values();
            Map<String, String> map = new HashMap<>(values.size());
            for (Object value : td.values()) {
                if (value instanceof CompositeData) {
                    CompositeData cdRow = (CompositeData) value;
                    Object k = cdRow.get("key");
                    Object v = cdRow.get("value");
                    if (k instanceof String && v instanceof String) {
                        map.put((String) k, (String) v);
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the provider of the configuration associated with this
     * {@code ConfigurationInfo} (for example, {@code "OpenJDK"}).
     *
     * @return the provider, or {@code null} if doesn't exist
     *
     * @see Configuration#getProvider()
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Returns the textual representation of the configuration associated with
     * this {@code ConfigurationInfo}, typically the contents of the
     * configuration file that was used to create the configuration.
     *
     * @return contents, or {@code null} if doesn't exist
     *
     * @see Configuration#getContents()
     */
    public String getContents() {
        return contents;
    }

    /**
     * Returns the settings for the configuration associated with this
     * {@code ConfigurationInfo}.
     *
     * @return a {@code Map} with settings, not {@code null}
     *
     * @see Configuration#getSettings()
     */
    public Map<String, String> getSettings() {
        return settings;
    }

    /**
     * Returns the human-readable name (for example, {@code "Continuous"} or {@code "Profiling"}) for
     * the configuration associated with this {@code ConfigurationInfo}
     *
     * @return the label, or {@code null} if doesn't exist
     *
     * @see Configuration#getLabel()
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the name of the configuration associated with this
     * {@code ConfigurationInfo} (for example, {@code "default"}).
     *
     * @return the name, or {@code null} if doesn't exist
     *
     * @see Configuration#getLabel()
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a short sentence that describes the configuration associated with
     * this {@code ConfigurationInfo} (for example, {@code "Low
     * overhead configuration safe for continuous use in production
     * environments"}.
     *
     * @return the description, or {@code null} if doesn't exist
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns a {@code ConfigurationInfo} object represented by the specified
     * {@code CompositeData}.
     * <p>
     * The following table shows the required attributes that the specified {@code CompositeData} must contain.
     * <blockquote>
     * <table class="striped">
     * <caption>Required names and types for CompositeData</caption>
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
     * <th scope="row">provider</th>
     * <td>{@code String}</td>
     * </tr>
     * <tr>
     * <th scope="row">contents</th>
     * <td>{@code String}</td>
     * </tr>
     *
     * <tr>
     * <th scope="row">settings</th>
     * <td>{@code javax.management.openmbean.TabularData} with a
     * {@code TabularType} with the keys {@code "key"} and {@code "value"}, both
     * of the {@code String} type</td>
     * </tr>
     * </tbody>
     * </table>
     * </blockquote>
     *
     * @param cd {@code CompositeData} representing a {@code ConfigurationInfo}
     *
     * @throws IllegalArgumentException if {@code cd} does not represent a
     *         {@code ConfigurationInfo} with the required attributes
     *
     * @return a {@code ConfigurationInfo} object represented by {@code cd} if
     *         {@code cd} is not {@code null}, {@code null} otherwise
     */
    public static ConfigurationInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        return new ConfigurationInfo(cd);
    }

    /**
     * Returns a description of the configuration that is associated with this
     * {@code ConfigurationInfo}.
     *
     * @return the description of the configuration, not {@code null}
     */
    @Override
    public String toString() {
        Stringifier s = new Stringifier();
        s.add("name", name);
        s.add("label", label);
        s.add("description", description);
        s.add("provider", provider);
        return s.toString();
    }
}
