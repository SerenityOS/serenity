/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.util;

import java.awt.EventQueue;
import org.openide.util.*;
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Thomas Wuerthinger
 */
public abstract class ContextAction<T> extends CallableSystemAction implements LookupListener, ContextAwareAction {

    private Lookup context = null;
    private Lookup.Result<T> result = null;

    public ContextAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ContextAction(Lookup context) {
        init(context);
    }

    private void init(Lookup context) {
        this.context = context;
        result = context.lookupResult(contextClass());
        result.addLookupListener(this);
        resultChanged(null);
    }

    @Override
    public void resultChanged(LookupEvent e) {
        if (result.allItems().size() != 0) {
            update(result.allInstances().iterator().next());
        } else {
            update(null);
        }
    }

    @Override
    public void performAction() {
        final T t = result.allInstances().iterator().next();

        // Ensure it's AWT event thread
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                performAction(t);
            }
        });
    }

    public void update(T t) {
        if (t == null) {
            setEnabled(false);
        } else {
            setEnabled(isEnabled(t));
        }
    }

    public boolean isEnabled(T context) {
        return true;
    }

    public abstract Class<T> contextClass();

    public abstract void performAction(T context);
}
