/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.igv.filter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Thomas Wuerthinger
 */
public class FilterSetting {

    private Set<Filter> filters;
    private String name;

    public FilterSetting() {
        this(null);
    }

    public FilterSetting(String name) {
        this.name = name;
        filters = new HashSet<>();
    }

    public Set<Filter> getFilters() {
        return Collections.unmodifiableSet(filters);
    }

    public void addFilter(Filter f) {
        assert !filters.contains(f);
        filters.add(f);
    }

    public void removeFilter(Filter f) {
        assert filters.contains(f);
        filters.remove(f);
    }

    public boolean containsFilter(Filter f) {
        return filters.contains(f);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFilterCount() {
        return filters.size();
    }

    @Override
    public String toString() {
        return getName();
    }
}
