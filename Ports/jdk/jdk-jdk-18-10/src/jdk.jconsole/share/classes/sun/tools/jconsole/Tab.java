/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole;

import java.awt.*;
import javax.swing.*;

@SuppressWarnings("serial") // JDK implementation class
public abstract class Tab extends JPanel {
    private String name;
    private Worker worker;

    protected VMPanel vmPanel;

    private SwingWorker<?, ?> prevSW;

    public Tab(VMPanel vmPanel, String name) {
        this.vmPanel = vmPanel;
        this.name = name;
    }

    public SwingWorker<?, ?> newSwingWorker() {
        return null;
    }

    public void update() {
        final ProxyClient proxyClient = vmPanel.getProxyClient();
        if (!proxyClient.hasPlatformMXBeans()) {
            throw new UnsupportedOperationException(
                "Platform MXBeans not registered in MBeanServer");
        }

        SwingWorker<?,?> sw = newSwingWorker();
        // schedule SwingWorker to run only if the previous
        // SwingWorker has finished its task and it hasn't started.
        if (prevSW == null || prevSW.isDone()) {
            if (sw == null || sw.getState() == SwingWorker.StateValue.PENDING) {
                prevSW = sw;
                if (sw != null) {
                    sw.execute();
                }
            }
        }
    }

    public synchronized void dispose() {
        if(worker != null)
            worker.stopWorker();

        // Subclasses will override to clean up
    }

    protected VMPanel getVMPanel() {
        return vmPanel;
    }

    OverviewPanel[] getOverviewPanels() {
        return null;
    }

    public synchronized void workerAdd(Runnable job) {
        if (worker == null) {
            worker = new Worker(name+"-"+vmPanel.getConnectionName());
            worker.start();
        }
        worker.add(job);
    }

    public Dimension getPreferredSize() {
        return new Dimension(700, 500);
    }
}
