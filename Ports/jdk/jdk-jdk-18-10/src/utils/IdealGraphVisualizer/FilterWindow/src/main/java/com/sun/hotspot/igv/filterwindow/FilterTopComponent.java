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

import com.sun.hotspot.igv.data.ChangedEvent;
import com.sun.hotspot.igv.data.ChangedListener;
import com.sun.hotspot.igv.filter.CustomFilter;
import com.sun.hotspot.igv.filter.Filter;
import com.sun.hotspot.igv.filter.FilterChain;
import com.sun.hotspot.igv.filter.FilterSetting;
import com.sun.hotspot.igv.filterwindow.actions.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.border.Border;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.awt.Toolbar;
import org.openide.awt.ToolbarPool;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Thomas Wuerthinger
 */
public final class FilterTopComponent extends TopComponent implements LookupListener, ExplorerManager.Provider {

    private static FilterTopComponent instance;
    public static final String FOLDER_ID = "Filters";
    public static final String AFTER_ID = "after";
    public static final String ENABLED_ID = "enabled";
    public static final String PREFERRED_ID = "FilterTopComponent";
    private CheckListView view;
    private ExplorerManager manager;
    private FilterChain filterChain;
    private FilterChain sequence;
    private Lookup.Result<FilterChain> result;
    private JComboBox comboBox;
    private List<FilterSetting> filterSettings;
    private FilterSetting customFilterSetting = new FilterSetting("-- Custom --");
    private ChangedEvent<FilterTopComponent> filterSettingsChangedEvent;
    private ActionListener comboBoxActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            comboBoxSelectionChanged();
        }
    };

    public ChangedEvent<FilterTopComponent> getFilterSettingsChangedEvent() {
        return filterSettingsChangedEvent;
    }

    public FilterChain getSequence() {
        return sequence;
    }

    public void updateSelection() {
        Node[] nodes = this.getExplorerManager().getSelectedNodes();
        int[] arr = new int[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            int index = sequence.getFilters().indexOf(((FilterNode) nodes[i]).getFilter());
            arr[i] = index;
        }
        view.showSelection(arr);
    }

    private void comboBoxSelectionChanged() {

        Object o = comboBox.getSelectedItem();
        if (o == null) {
            return;
        }
        assert o instanceof FilterSetting;
        FilterSetting s = (FilterSetting) o;

        if (s != customFilterSetting) {
            FilterChain chain = getFilterChain();
            chain.getChangedEvent().beginAtomic();
            List<Filter> toRemove = new ArrayList<>();
            for (Filter f : chain.getFilters()) {
                if (!s.containsFilter(f)) {
                    toRemove.add(f);
                }
            }
            for (Filter f : toRemove) {
                chain.removeFilter(f);
            }

            for (Filter f : s.getFilters()) {
                if (!chain.containsFilter(f)) {
                    chain.addFilter(f);
                }
            }

            chain.getChangedEvent().endAtomic();
            filterSettingsChangedEvent.fire();
        } else {
            this.updateComboBoxSelection();
        }

        SystemAction.get(RemoveFilterSettingsAction.class).setEnabled(comboBox.getSelectedItem() != this.customFilterSetting);
        SystemAction.get(SaveFilterSettingsAction.class).setEnabled(comboBox.getSelectedItem() == this.customFilterSetting);
    }

    private void updateComboBox() {
        comboBox.removeAllItems();
        comboBox.addItem(customFilterSetting);
        for (FilterSetting s : filterSettings) {
            comboBox.addItem(s);
        }

        this.updateComboBoxSelection();
    }

    public void addFilterSetting() {
        NotifyDescriptor.InputLine l = new NotifyDescriptor.InputLine("Name of the new profile:", "Filter Profile");
        if (DialogDisplayer.getDefault().notify(l) == NotifyDescriptor.OK_OPTION) {
            String name = l.getInputText();

            FilterSetting toRemove = null;
            for (FilterSetting s : filterSettings) {
                if (s.getName().equals(name)) {
                    NotifyDescriptor.Confirmation conf = new NotifyDescriptor.Confirmation("Filter profile \"" + name + "\" already exists, do you want to replace it?", "Filter");
                    if (DialogDisplayer.getDefault().notify(conf) == NotifyDescriptor.YES_OPTION) {
                        toRemove = s;
                        break;
                    } else {
                        return;
                    }
                }
            }

            if (toRemove != null) {
                filterSettings.remove(toRemove);
            }
            FilterSetting setting = createFilterSetting(name);
            filterSettings.add(setting);

            // Sort alphabetically
            Collections.sort(filterSettings, new Comparator<FilterSetting>() {

                @Override
                public int compare(FilterSetting o1, FilterSetting o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            updateComboBox();
        }
    }

    public boolean canRemoveFilterSetting() {
        return comboBox.getSelectedItem() != customFilterSetting;
    }

    public void removeFilterSetting() {
        if (canRemoveFilterSetting()) {
            Object o = comboBox.getSelectedItem();
            assert o instanceof FilterSetting;
            FilterSetting f = (FilterSetting) o;
            assert f != customFilterSetting;
            assert filterSettings.contains(f);
            NotifyDescriptor.Confirmation l = new NotifyDescriptor.Confirmation("Do you really want to remove filter profile \"" + f + "\"?", "Filter Profile");
            if (DialogDisplayer.getDefault().notify(l) == NotifyDescriptor.YES_OPTION) {
                filterSettings.remove(f);
                updateComboBox();
            }
        }
    }

    private FilterSetting createFilterSetting(String name) {
        FilterSetting s = new FilterSetting(name);
        FilterChain chain = this.getFilterChain();
        for (Filter f : chain.getFilters()) {
            s.addFilter(f);
        }
        return s;
    }

    private void updateComboBoxSelection() {
        List<Filter> filters = this.getFilterChain().getFilters();
        boolean found = false;
        for (FilterSetting s : filterSettings) {
            if (s.getFilterCount() == filters.size()) {
                boolean ok = true;
                for (Filter f : filters) {
                    if (!s.containsFilter(f)) {
                        ok = false;
                    }
                }

                if (ok) {
                    if (comboBox.getSelectedItem() != s) {
                        comboBox.setSelectedItem(s);
                    }
                    found = true;
                    break;
                }
            }
        }

        if (!found && comboBox.getSelectedItem() != customFilterSetting) {
            comboBox.setSelectedItem(customFilterSetting);
        }
    }

    private class FilterChildren extends Children.Keys<Filter> implements ChangedListener<CheckNode> {

        private HashMap<Filter, Node> nodeHash = new HashMap<>();

        @Override
        protected Node[] createNodes(Filter filter) {
            if (nodeHash.containsKey(filter)) {
                return new Node[]{nodeHash.get(filter)};
            }

            FilterNode node = new FilterNode(filter);
            node.getSelectionChangedEvent().addListener(this);
            nodeHash.put(filter, node);
            return new Node[]{node};
        }

        public FilterChildren() {
            sequence.getChangedEvent().addListener(new ChangedListener<FilterChain>() {

                @Override
                public void changed(FilterChain source) {
                    addNotify();
                }
            });

            setBefore(false);
        }

        @Override
        protected void addNotify() {
            setKeys(sequence.getFilters());
            updateSelection();
        }

        @Override
        public void changed(CheckNode source) {
            FilterNode node = (FilterNode) source;
            Filter f = node.getFilter();
            FilterChain chain = getFilterChain();
            if (node.isSelected()) {
                if (!chain.containsFilter(f)) {
                    chain.addFilter(f);
                }
            } else {
                if (chain.containsFilter(f)) {
                    chain.removeFilter(f);
                }
            }
            view.revalidate();
            view.repaint();
            updateComboBoxSelection();
        }
    }

    public FilterChain getFilterChain() {
        return filterChain;
    }

    private FilterTopComponent() {
        filterSettingsChangedEvent = new ChangedEvent<>(this);
        initComponents();
        setName(NbBundle.getMessage(FilterTopComponent.class, "CTL_FilterTopComponent"));
        setToolTipText(NbBundle.getMessage(FilterTopComponent.class, "HINT_FilterTopComponent"));
        //        setIcon(Utilities.loadImage(ICON_PATH, true));

        sequence = new FilterChain();
        filterChain = new FilterChain();
        initFilters();
        manager = new ExplorerManager();
        manager.setRootContext(new AbstractNode(new FilterChildren()));
        associateLookup(ExplorerUtils.createLookup(manager, getActionMap()));
        view = new CheckListView();

        ToolbarPool.getDefault().setPreferredIconSize(16);
        Toolbar toolBar = new Toolbar();
        Border b = (Border) UIManager.get("Nb.Editor.Toolbar.border"); //NOI18N
        toolBar.setBorder(b);
        comboBox = new JComboBox();
        toolBar.add(comboBox);
        this.add(toolBar, BorderLayout.NORTH);
        toolBar.add(SaveFilterSettingsAction.get(SaveFilterSettingsAction.class));
        toolBar.add(RemoveFilterSettingsAction.get(RemoveFilterSettingsAction.class));
        toolBar.addSeparator();
        toolBar.add(NewFilterAction.get(NewFilterAction.class));
        toolBar.add(RemoveFilterAction.get(RemoveFilterAction.class).createContextAwareInstance(this.getLookup()));
        toolBar.add(MoveFilterUpAction.get(MoveFilterUpAction.class).createContextAwareInstance(this.getLookup()));
        toolBar.add(MoveFilterDownAction.get(MoveFilterDownAction.class).createContextAwareInstance(this.getLookup()));
        this.add(view, BorderLayout.CENTER);

        filterSettings = new ArrayList<>();
        updateComboBox();

        comboBox.addActionListener(comboBoxActionListener);
        setChain(filterChain);
    }

    public void newFilter() {
        CustomFilter cf = new CustomFilter("My custom filter", "");
        if (cf.openInEditor()) {
            sequence.addFilter(cf);
            FileObject fo = getFileObject(cf);
            FilterChangedListener listener = new FilterChangedListener(fo, cf);
            listener.changed(cf);
            cf.getChangedEvent().addListener(listener);
        }
    }

    public void removeFilter(Filter f) {
        com.sun.hotspot.igv.filter.CustomFilter cf = (com.sun.hotspot.igv.filter.CustomFilter) f;

        sequence.removeFilter(cf);
        try {
            getFileObject(cf).delete();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    private static class FilterChangedListener implements ChangedListener<Filter> {

        private FileObject fileObject;
        private CustomFilter filter;

        public FilterChangedListener(FileObject fo, CustomFilter cf) {
            fileObject = fo;
            filter = cf;
        }

        @Override
        public void changed(Filter source) {
            try {
                if (!fileObject.getName().equals(filter.getName())) {
                    FileLock lock = fileObject.lock();
                    fileObject.move(lock, fileObject.getParent(), filter.getName(), "");
                    lock.releaseLock();
                    FileObject newFileObject = fileObject.getParent().getFileObject(filter.getName());
                    fileObject = newFileObject;
                }

                FileLock lock = fileObject.lock();
                OutputStream os = fileObject.getOutputStream(lock);
                try (Writer w = new OutputStreamWriter(os)) {
                    String s = filter.getCode();
                    w.write(s);
                }
                lock.releaseLock();

            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    public void initFilters() {
        FileObject folder = FileUtil.getConfigRoot().getFileObject(FOLDER_ID);
        FileObject[] children = folder.getChildren();

        List<CustomFilter> customFilters = new ArrayList<>();
        HashMap<CustomFilter, String> afterMap = new HashMap<>();
        Set<CustomFilter> enabledSet = new HashSet<>();
        HashMap<String, CustomFilter> map = new HashMap<>();

        for (final FileObject fo : children) {
            InputStream is = null;

            String code = "";
            FileLock lock = null;
            try {
                lock = fo.lock();
                is = fo.getInputStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                String s;
                StringBuilder sb = new StringBuilder();
                while ((s = r.readLine()) != null) {
                    sb.append(s);
                    sb.append("\n");
                }
                code = sb.toString();
            } catch (FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                lock.releaseLock();
            }

            String displayName = fo.getName();


            final CustomFilter cf = new CustomFilter(displayName, code);
            map.put(displayName, cf);

            String after = (String) fo.getAttribute(AFTER_ID);
            afterMap.put(cf, after);

            Boolean enabled = (Boolean) fo.getAttribute(ENABLED_ID);
            if (enabled != null && (boolean) enabled) {
                enabledSet.add(cf);
            }

            cf.getChangedEvent().addListener(new FilterChangedListener(fo, cf));

            customFilters.add(cf);
        }

        for (int j = 0; j < customFilters.size(); j++) {
            for (int i = 0; i < customFilters.size(); i++) {
                List<CustomFilter> copiedList = new ArrayList<>(customFilters);
                for (CustomFilter cf : copiedList) {

                    String after = afterMap.get(cf);

                    if (map.containsKey(after)) {
                        CustomFilter afterCf = map.get(after);
                        int index = customFilters.indexOf(afterCf);
                        int currentIndex = customFilters.indexOf(cf);

                        if (currentIndex < index) {
                            customFilters.remove(currentIndex);
                            customFilters.add(index, cf);
                        }
                    }
                }
            }
        }

        for (CustomFilter cf : customFilters) {
            sequence.addFilter(cf);
            if (enabledSet.contains(cf)) {
                filterChain.addFilter(cf);
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());

    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link findInstance}.
     */
    public static synchronized FilterTopComponent getDefault() {
        if (instance == null) {
            instance = new FilterTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the FilterTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized FilterTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            ErrorManager.getDefault().log(ErrorManager.WARNING, "Cannot find Filter component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof FilterTopComponent) {
            return (FilterTopComponent) win;
        }
        ErrorManager.getDefault().log(ErrorManager.WARNING, "There seem to be multiple components with the '" + PREFERRED_ID + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return manager;
    }

    @Override
    public void componentOpened() {
        Lookup.Template<FilterChain> tpl = new Lookup.Template<>(FilterChain.class);
        result = Utilities.actionsGlobalContext().lookup(tpl);
        result.addLookupListener(this);
    }

    @Override
    public void componentClosed() {
        result.removeLookupListener(this);
        result = null;
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setChain(Utilities.actionsGlobalContext().lookup(FilterChain.class));
    }

    public void setChain(FilterChain chain) {
        updateComboBoxSelection();
    }

    private FileObject getFileObject(CustomFilter cf) {
        FileObject fo = FileUtil.getConfigRoot().getFileObject(FOLDER_ID + "/" + cf.getName());
        if (fo == null) {
            try {
                fo = FileUtil.getConfigRoot().getFileObject(FOLDER_ID).createData(cf.getName());
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return fo;
    }

    @Override
    public boolean requestFocus(boolean temporary) {
        view.requestFocus();
        return super.requestFocus(temporary);
    }

    @Override
    protected boolean requestFocusInWindow(boolean temporary) {
        view.requestFocus();
        return super.requestFocusInWindow(temporary);
    }

    @Override
    public void requestActive() {
        super.requestActive();
        view.requestFocus();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeInt(filterSettings.size());
        for (FilterSetting f : filterSettings) {
            out.writeUTF(f.getName());

            out.writeInt(f.getFilterCount());
            for (Filter filter : f.getFilters()) {
                CustomFilter cf = (CustomFilter) filter;
                out.writeUTF(cf.getName());
            }
        }

        CustomFilter prev = null;
        for (Filter f : this.sequence.getFilters()) {
            CustomFilter cf = (CustomFilter) f;
            FileObject fo = getFileObject(cf);
            if (getFilterChain().containsFilter(cf)) {
                fo.setAttribute(ENABLED_ID, true);
            } else {
                fo.setAttribute(ENABLED_ID, false);
            }

            if (prev == null) {
                fo.setAttribute(AFTER_ID, null);
            } else {
                fo.setAttribute(AFTER_ID, prev.getName());
            }

            prev = cf;
        }
    }

    public CustomFilter findFilter(String name) {
        for (Filter f : sequence.getFilters()) {

            CustomFilter cf = (CustomFilter) f;
            if (cf.getName().equals(name)) {
                return cf;
            }
        }

        return null;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        int filterSettingsCount = in.readInt();
        for (int i = 0; i < filterSettingsCount; i++) {
            String name = in.readUTF();
            FilterSetting s = new FilterSetting(name);
            int filterCount = in.readInt();
            for (int j = 0; j < filterCount; j++) {
                String filterName = in.readUTF();
                CustomFilter filter = findFilter(filterName);
                if (filter != null) {
                    s.addFilter(filter);
                }
            }

            filterSettings.add(s);
        }
        updateComboBox();
    }

    final static class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 1L;

        public Object readResolve() {
            return FilterTopComponent.getDefault();
        }
    }
}
