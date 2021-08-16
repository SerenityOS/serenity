/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.coordinator;

import com.sun.hotspot.igv.coordinator.actions.RemoveCookie;
import com.sun.hotspot.igv.data.*;
import com.sun.hotspot.igv.util.PropertiesSheet;
import java.awt.Image;
import java.util.List;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Thomas Wuerthinger
 */
public class FolderNode extends AbstractNode {

    private InstanceContent content;
    private FolderChildren children;

    private static class FolderChildren extends Children.Keys<FolderElement> implements ChangedListener {

        private final Folder folder;

        public FolderChildren(Folder folder) {
            this.folder = folder;
            folder.getChangedEvent().addListener(this);
        }

        @Override
        protected Node[] createNodes(FolderElement e) {
             if (e instanceof InputGraph) {
                return new Node[]{new GraphNode((InputGraph) e)};
            } else if (e instanceof Folder) {
                 return new Node[]{new FolderNode((Folder) e)};
             } else {
                return null;
            }
        }

        @Override
        public void addNotify() {
            this.setKeys(folder.getElements());
        }

        @Override
        public void changed(Object source) {
            addNotify();
         }
    }

    @Override
    protected Sheet createSheet() {
        Sheet s = super.createSheet();
        if (children.folder instanceof Properties.Entity) {
            Properties.Entity p = (Properties.Entity) children.folder;
            PropertiesSheet.initializeSheet(p.getProperties(), s);
        }
        return s;
    }

    @Override
    public Image getIcon(int i) {
        return ImageUtilities.loadImage("com/sun/hotspot/igv/coordinator/images/folder.png");
    }

    protected FolderNode(Folder folder) {
        this(folder, new FolderChildren(folder), new InstanceContent());
    }

    private FolderNode(final Folder folder, FolderChildren children, InstanceContent content) {
        super(children, new AbstractLookup(content));
        this.content = content;
        this.children = children;
        if (folder instanceof FolderElement) {
            final FolderElement folderElement = (FolderElement) folder;
            this.setDisplayName(folderElement.getName());
            content.add(new RemoveCookie() {
                @Override
                public void remove() {
                    folderElement.getParent().removeElement(folderElement);
                }
            });
        }
    }

    public void init(String name, List<Group> groups) {
        this.setDisplayName(name);
        children.addNotify();

        for (Group g : groups) {
            content.add(g);
        }
    }

    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
}
