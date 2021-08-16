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

import com.sun.hotspot.igv.data.ChangedEvent;
import com.sun.hotspot.igv.data.ChangedEventProvider;
import com.sun.hotspot.igv.data.ChangedListener;
import com.sun.hotspot.igv.graph.Diagram;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 */
public class FilterChain implements ChangedEventProvider<FilterChain> {

    private List<Filter> filters;
    private transient ChangedEvent<FilterChain> changedEvent;

    private ChangedListener<Filter> changedListener = new ChangedListener<Filter>() {
        @Override
        public void changed(Filter source) {
            changedEvent.fire();
        }
    };

    public FilterChain() {
        filters = new ArrayList<>();
        changedEvent = new ChangedEvent<>(this);
    }

    public FilterChain(FilterChain f) {
        this.filters = new ArrayList<>(f.filters);
        changedEvent = new ChangedEvent<>(this);
    }

    @Override
    public ChangedEvent<FilterChain> getChangedEvent() {
        return changedEvent;
    }

    public Filter getFilterAt(int index) {
        assert index >= 0 && index < filters.size();
        return filters.get(index);
    }

    public void apply(Diagram d) {
        for (Filter f : filters) {
            f.apply(d);
        }
    }

    public void apply(Diagram d, FilterChain sequence) {
        List<Filter> applied = new ArrayList<>();
        for (Filter f : sequence.getFilters()) {
            if (filters.contains(f)) {
                f.apply(d);
                applied.add(f);
            }
        }


        for (Filter f : filters) {
            if (!applied.contains(f)) {
                f.apply(d);
            }
        }
    }


    public void addFilter(Filter filter) {
        assert filter != null;
        filters.add(filter);
        filter.getChangedEvent().addListener(changedListener);
        changedEvent.fire();
    }

    public boolean containsFilter(Filter filter) {
        return filters.contains(filter);
    }

    public void removeFilter(Filter filter) {
        assert filters.contains(filter);
        filters.remove(filter);
        filter.getChangedEvent().removeListener(changedListener);
        changedEvent.fire();
    }

    public void moveFilterUp(Filter filter) {
        assert filters.contains(filter);
        int index = filters.indexOf(filter);
        if (index != 0) {
            filters.remove(index);
            filters.add(index - 1, filter);
        }
        changedEvent.fire();
    }

    public void moveFilterDown(Filter filter) {
        assert filters.contains(filter);
        int index = filters.indexOf(filter);
        if (index != filters.size() - 1) {
            filters.remove(index);
            filters.add(index + 1, filter);
        }
        changedEvent.fire();
    }

    public List<Filter> getFilters() {
        return Collections.unmodifiableList(filters);
    }
}
