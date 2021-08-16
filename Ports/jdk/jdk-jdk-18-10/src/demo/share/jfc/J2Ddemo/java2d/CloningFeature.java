/*
 *
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;


/**
 * Illustration of how to use the clone feature of the demo.
 */
@SuppressWarnings("serial")
public final class CloningFeature extends JPanel implements Runnable {

    private final DemoInstVarsAccessor demoInstVars;
    private Thread thread;
    private JTextArea ta;

    public CloningFeature(DemoInstVarsAccessor demoInstVars) {
        this.demoInstVars = demoInstVars;

        setLayout(new BorderLayout());
        EmptyBorder eb = new EmptyBorder(5, 5, 5, 5);
        SoftBevelBorder sbb = new SoftBevelBorder(BevelBorder.RAISED);
        setBorder(new CompoundBorder(eb, sbb));

        ta = new JTextArea("Cloning Demonstrated\n\nClicking once on a demo\n");
        ta.setMinimumSize(new Dimension(300, 500));
        JScrollPane scroller = new JScrollPane();
        scroller.getViewport().add(ta);
        ta.setFont(new Font("Dialog", Font.PLAIN, 14));
        ta.setForeground(Color.black);
        ta.setBackground(Color.lightGray);
        ta.setEditable(false);

        add("Center", scroller);

        start();
    }

    public void start() {
        thread = new Thread(this);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setName("CloningFeature");
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = null;
    }

    @Override
    @SuppressWarnings("SleepWhileHoldingLock")
    public void run() {


        int index = demoInstVars.getTabbedPane().getSelectedIndex();
        if (index == 0) {
            demoInstVars.getTabbedPane().setSelectedIndex(1);
            try {
                Thread.sleep(3333);
            } catch (Exception e) {
                return;
            }
        }

        if (!demoInstVars.getControls().toolBarCB.isSelected()) {
            demoInstVars.getControls().toolBarCB.setSelected(true);
            try {
                Thread.sleep(2222);
            } catch (Exception e) {
                return;
            }
        }

        index = demoInstVars.getTabbedPane().getSelectedIndex() - 1;
        DemoGroup dg = demoInstVars.getGroup()[index];
        DemoPanel dp = (DemoPanel) dg.getPanel().getComponent(0);
        if (dp.surface == null) {
            ta.append("Sorry your zeroth component is not a Surface.");
            return;
        }

        dg.mouseClicked(dp.surface);

        try {
            Thread.sleep(3333);
        } catch (Exception e) {
            return;
        }

        ta.append("Clicking the ToolBar double document button\n");
        try {
            Thread.sleep(3333);
        } catch (Exception e) {
            return;
        }

        dp = (DemoPanel) dg.clonePanels[0].getComponent(0);

        if (dp.tools != null) {
            for (int i = 0; i < 3 && thread != null; i++) {
                ta.append("   Cloning\n");
                dp.tools.cloneB.doClick();
                try {
                    Thread.sleep(3333);
                } catch (Exception e) {
                    return;
                }
            }
        }

        ta.append("Changing attributes \n");

        try {
            Thread.sleep(3333);
        } catch (Exception e) {
            return;
        }

        Component[] cmps = dg.clonePanels[0].getComponents();
        for (int i = 0; i < cmps.length && thread != null; i++) {
            if ((dp = (DemoPanel) cmps[i]).tools == null) {
                continue;
            }
            switch (i) {
                case 0:
                    ta.append("   Changing AntiAliasing\n");
                    dp.tools.aliasB.doClick();
                    break;
                case 1:
                    ta.append("   Changing Composite & Texture\n");
                    dp.tools.compositeB.doClick();
                    dp.tools.textureB.doClick();
                    break;
                case 2:
                    ta.append("   Changing Screen\n");
                    dp.tools.screenCombo.setSelectedIndex(4);
                    break;
                case 3:
                    ta.append("   Removing a clone\n");
                    dp.tools.cloneB.doClick();
            }
            try {
                Thread.sleep(3333);
            } catch (Exception e) {
                return;
            }
        }

        ta.append("\nAll Done!");
    }
}
