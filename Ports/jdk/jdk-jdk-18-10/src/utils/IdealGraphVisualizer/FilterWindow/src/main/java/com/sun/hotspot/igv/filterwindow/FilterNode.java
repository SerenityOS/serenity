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
package com.sun.hotspot.igv.filterwindow;

import com.sun.hotspot.igv.data.ChangedListener;
import com.sun.hotspot.igv.filter.Filter;
import com.sun.hotspot.igv.filter.FilterChain;
import com.sun.hotspot.igv.filterwindow.actions.MoveFilterDownAction;
import com.sun.hotspot.igv.filterwindow.actions.MoveFilterUpAction;
import com.sun.hotspot.igv.filterwindow.actions.RemoveFilterAction;
import com.sun.hotspot.igv.util.PropertiesSheet;
import javax.swing.Action;
import org.openide.actions.OpenAction;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Thomas Wuerthinger
 */
public class FilterNode extends CheckNode implements LookupListener, ChangedListener<FilterTopComponent> {

    private Filter filter;
    private Lookup.Result<FilterChain> result;

    public FilterNode(Filter filter) {
        this(filter, new InstanceContent());
    }

    private FilterNode(Filter filter, InstanceContent content) {
        super(Children.LEAF, new AbstractLookup(content));
        content.add(filter);

        content.add(filter.getEditor());
        this.filter = filter;
        filter.getChangedEvent().addListener(new ChangedListener<Filter>() {

            @Override
            public void changed(Filter source) {
                update();
            }
        });

        update();

        Lookup.Template<FilterChain> tpl = new Lookup.Template<>(FilterChain.class);
        result = Utilities.actionsGlobalContext().lookup(tpl);
        result.addLookupListener(this);

        FilterTopComponent.findInstance().getFilterSettingsChangedEvent().addListener(this);
        resultChanged(null);

        setShortDescription("Double-click to open filter");
    }

    private void update() {
        this.setDisplayName(filter.getName());
    }

    public Filter getFilter() {
        return filter;
    }

    @Override
    protected Sheet createSheet() {
        Sheet s = super.createSheet();
        PropertiesSheet.initializeSheet(getFilter().getProperties(), s);
        return s;
    }

    @Override
    public Action[] getActions(boolean b) {
        return new Action[]{(Action) OpenAction.findObject(OpenAction.class, true), (Action) MoveFilterUpAction.findObject(MoveFilterUpAction.class, true), (Action) MoveFilterDownAction.findObject(MoveFilterDownAction.class, true), (Action) RemoveFilterAction.findObject(RemoveFilterAction.class, true)};
    }

    @Override
    public Action getPreferredAction() {
        return OpenAction.get(OpenAction.class).createContextAwareInstance(Utilities.actionsGlobalContext());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        changed(FilterTopComponent.findInstance());
    }

    @Override
    public void changed(FilterTopComponent source) {
        setSelected(source.getFilterChain().containsFilter(filter));
    }
}
