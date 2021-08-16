/*
 *
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package java2d;


import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Panel that holds the Demo groups, Controls and Monitors for each tab.
 * It's a special "always visible" panel for the Controls, MemoryMonitor &
 * PerformanceMonitor.
 */
@SuppressWarnings("serial")
public class GlobalPanel extends JPanel implements ChangeListener {
    private final DemoInstVarsAccessor demoInstVars;
    private JPanel p;
    private int index;

    public GlobalPanel(DemoInstVarsAccessor demoInstVars) {
        this.demoInstVars = demoInstVars;

        setLayout(new BorderLayout());
        p = new JPanel(new GridBagLayout());
        EmptyBorder eb = new EmptyBorder(5, 0, 5, 5);
        BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
        p.setBorder(new CompoundBorder(eb, bb));
        J2Ddemo.addToGridBag(p, demoInstVars.getControls(), 0, 0, 1, 1, 0, 0);
        J2Ddemo.addToGridBag(p, demoInstVars.getMemoryMonitor(), 0, 1, 1, 1, 0, 0);
        J2Ddemo.addToGridBag(p, demoInstVars.getPerformanceMonitor(), 0, 2, 1, 1, 0, 0);
        add(demoInstVars.getIntro());
    }

    @Override
    public void stateChanged(ChangeEvent e) {

        demoInstVars.getGroup()[index].shutDown(demoInstVars.getGroup()[index].getPanel());
        if (demoInstVars.getTabbedPane().getSelectedIndex() == 0) {
            demoInstVars.getMemoryMonitor().surf.stop();
            demoInstVars.getPerformanceMonitor().surf.stop();
            removeAll();
            add(demoInstVars.getIntro());
            demoInstVars.getIntro().start();
        } else {
            if (getComponentCount() == 1) {
                demoInstVars.getIntro().stop();
                remove(demoInstVars.getIntro());
                add(p, BorderLayout.EAST);
                if (demoInstVars.getMemoryCB().getState()) {
                    demoInstVars.getMemoryMonitor().surf.start();
                }
                if (demoInstVars.getPerfCB().getState()) {
                    demoInstVars.getPerformanceMonitor().surf.start();
                }
            } else {
                remove(demoInstVars.getGroup()[index]);
            }
            index = demoInstVars.getTabbedPane().getSelectedIndex() - 1;
            add(demoInstVars.getGroup()[index]);
            demoInstVars.getGroup()[index].setup(false);
        }
        revalidate();
    }
}
