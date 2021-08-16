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
package com.sun.hotspot.igv.view.actions;

import com.sun.hotspot.igv.data.ChangedListener;
import com.sun.hotspot.igv.util.ContextAction;
import com.sun.hotspot.igv.view.DiagramViewModel;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.openide.util.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public final class NextDiagramAction extends ContextAction<DiagramViewModel> implements ChangedListener<DiagramViewModel> {

    private DiagramViewModel model;

    public NextDiagramAction() {
        this(Utilities.actionsGlobalContext());
    }

    public NextDiagramAction(Lookup lookup) {
        putValue(Action.SHORT_DESCRIPTION, "Show next graph of current group");
        putValue(Action.SMALL_ICON, new ImageIcon(ImageUtilities.loadImage("com/sun/hotspot/igv/view/images/next_diagram.png")));
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(NextDiagramAction.class, "CTL_NextDiagramAction");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public Class<DiagramViewModel> contextClass() {
        return DiagramViewModel.class;
    }

    @Override
    public void performAction(DiagramViewModel model) {
        int fp = model.getFirstPosition();
        int sp = model.getSecondPosition();
        if (sp != model.getPositions().size() - 1) {
            int nfp = fp + 1;
            int nsp = sp + 1;
            model.setPositions(nfp, nsp);
        }
    }

    @Override
    public void update(DiagramViewModel model) {
        super.update(model);

        if (this.model != model) {
            if (this.model != null) {
                this.model.getDiagramChangedEvent().removeListener(this);
            }

            this.model = model;
            if (this.model != null) {
                this.model.getDiagramChangedEvent().addListener(this);
            }
        }
    }

    @Override
    public boolean isEnabled(DiagramViewModel model) {
        return model.getSecondPosition() != model.getPositions().size() - 1;
    }

    @Override
    public Action createContextAwareInstance(Lookup arg0) {
        return new NextDiagramAction(arg0);
    }

    @Override
    public void changed(DiagramViewModel source) {
        update(source);
    }
}
