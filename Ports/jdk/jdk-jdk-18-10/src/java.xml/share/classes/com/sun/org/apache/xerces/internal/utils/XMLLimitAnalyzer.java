/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.utils;

import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager.Limit;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import jdk.xml.internal.JdkConstants;

/**
 * A helper for analyzing entity expansion limits
 *
 * @author Joe Wang Oracle Corp.
 *
 */
public final class XMLLimitAnalyzer {

    /**
     * Map old property names with the new ones
     */
    public static enum NameMap {
        ENTITY_EXPANSION_LIMIT(JdkConstants.SP_ENTITY_EXPANSION_LIMIT, JdkConstants.ENTITY_EXPANSION_LIMIT),
        MAX_OCCUR_NODE_LIMIT(JdkConstants.SP_MAX_OCCUR_LIMIT, JdkConstants.MAX_OCCUR_LIMIT),
        ELEMENT_ATTRIBUTE_LIMIT(JdkConstants.SP_ELEMENT_ATTRIBUTE_LIMIT, JdkConstants.ELEMENT_ATTRIBUTE_LIMIT);

        final String newName;
        final String oldName;

        NameMap(String newName, String oldName) {
            this.newName = newName;
            this.oldName = oldName;
        }

        String getOldName(String newName) {
            if (newName.equals(this.newName)) {
                return oldName;
            }
            return null;
        }
    }

    /**
     * Max value accumulated for each property
     */
    private final int[] values;
    /**
     * Names of the entities corresponding to their max values
     */
    private final String[] names;
    /**
     * Total value of accumulated entities
     */
    private final int[] totalValue;

    /**
     * Maintain values of the top 10 elements in the process of parsing
     */
    private final Map<String, Integer>[] caches;

    private String entityStart, entityEnd;
    /**
     * Default constructor. Establishes default values for known security
     * vulnerabilities.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public XMLLimitAnalyzer() {
        values = new int[Limit.values().length];
        totalValue = new int[Limit.values().length];
        names = new String[Limit.values().length];
        caches = new Map[Limit.values().length];
    }

    /**
     * Add the value to the current max count for the specified property
     * To find the max value of all entities, set no limit
     *
     * @param limit the type of the property
     * @param entityName the name of the entity
     * @param value the value of the entity
     */
    public void addValue(Limit limit, String entityName, int value) {
        addValue(limit.ordinal(), entityName, value);
    }

    /**
     * Add the value to the current count by the index of the property
     * @param index the index of the property
     * @param entityName the name of the entity
     * @param value the value of the entity
     */
    public void addValue(int index, String entityName, int value) {
        if (index == Limit.ENTITY_EXPANSION_LIMIT.ordinal() ||
                index == Limit.MAX_OCCUR_NODE_LIMIT.ordinal() ||
                index == Limit.ELEMENT_ATTRIBUTE_LIMIT.ordinal() ||
                index == Limit.TOTAL_ENTITY_SIZE_LIMIT.ordinal() ||
                index == Limit.ENTITY_REPLACEMENT_LIMIT.ordinal()
                ) {
            totalValue[index] += value;
            return;
        }
        if (index == Limit.MAX_ELEMENT_DEPTH_LIMIT.ordinal() ||
                index == Limit.MAX_NAME_LIMIT.ordinal()) {
            values[index] = value;
            totalValue[index] = value;
            return;
        }

        Map<String, Integer> cache;
        if (caches[index] == null) {
            cache = new HashMap<>(10);
            caches[index] = cache;
        } else {
            cache = caches[index];
        }

        int accumulatedValue = value;
        if (cache.containsKey(entityName)) {
            accumulatedValue += cache.get(entityName);
            cache.put(entityName, accumulatedValue);
        } else {
            cache.put(entityName, value);
        }

        if (accumulatedValue > values[index]) {
            values[index] = accumulatedValue;
            names[index] = entityName;
        }


        if (index == Limit.GENERAL_ENTITY_SIZE_LIMIT.ordinal() ||
                index == Limit.PARAMETER_ENTITY_SIZE_LIMIT.ordinal()) {
            totalValue[Limit.TOTAL_ENTITY_SIZE_LIMIT.ordinal()] += value;
        }
    }

    /**
     * Return the value of the current max count for the specified property
     *
     * @param limit the property
     * @return the value of the property
     */
    public int getValue(Limit limit) {
        return getValue(limit.ordinal());
    }

    public int getValue(int index) {
        if (index == Limit.ENTITY_REPLACEMENT_LIMIT.ordinal()) {
            return totalValue[index];
        }
        return values[index];
    }
    /**
     * Return the total value accumulated so far
     *
     * @param limit the property
     * @return the accumulated value of the property
     */
    public int getTotalValue(Limit limit) {
        return totalValue[limit.ordinal()];
    }

    public int getTotalValue(int index) {
        return totalValue[index];
    }
    /**
     * Return the current max value (count or length) by the index of a property
     * @param index the index of a property
     * @return count of a property
     */
    public int getValueByIndex(int index) {
        return values[index];
    }

    public void startEntity(String name) {
        entityStart = name;
    }

    public boolean isTracking(String name) {
        if (entityStart == null) {
            return false;
        }
        return entityStart.equals(name);
    }
    /**
     * Stop tracking the entity
     * @param limit the limit property
     * @param name the name of an entity
     */
    public void endEntity(Limit limit, String name) {
        entityStart = "";
        Map<String, Integer> cache = caches[limit.ordinal()];
        if (cache != null) {
            cache.remove(name);
        }
    }

    /**
     * Resets the current value of the specified limit.
     * @param limit The limit to be reset.
     */
    public void reset(Limit limit) {
        if (limit.ordinal() == Limit.TOTAL_ENTITY_SIZE_LIMIT.ordinal()) {
            totalValue[limit.ordinal()] = 0;
        } else if (limit.ordinal() == Limit.GENERAL_ENTITY_SIZE_LIMIT.ordinal()) {
            names[limit.ordinal()] = null;
            values[limit.ordinal()] = 0;
            caches[limit.ordinal()] = null;
            totalValue[limit.ordinal()] = 0;
        }
    }

    public void debugPrint(XMLSecurityManager securityManager) {
        Formatter formatter = new Formatter();
        System.out.println(formatter.format("%30s %15s %15s %15s %30s",
                "Property","Limit","Total size","Size","Entity Name"));

        for (Limit limit : Limit.values()) {
            formatter = new Formatter();
            System.out.println(formatter.format("%30s %15d %15d %15d %30s",
                    limit.name(),
                    securityManager.getLimit(limit),
                    totalValue[limit.ordinal()],
                    values[limit.ordinal()],
                    names[limit.ordinal()]));
        }
    }
}
