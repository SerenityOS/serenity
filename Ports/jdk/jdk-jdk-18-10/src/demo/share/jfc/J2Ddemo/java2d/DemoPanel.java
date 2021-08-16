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


import static java2d.CustomControlsContext.State.START;
import static java2d.CustomControlsContext.State.STOP;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;


/**
 * The panel for the Surface, Custom Controls & Tools.
 * Other component types welcome.
 */
@SuppressWarnings("serial")
public class DemoPanel extends JPanel {
    private final DemoInstVarsAccessor demoInstVars;
    public Surface surface;
    public CustomControlsContext ccc;
    public Tools tools;
    public String className;

    public DemoPanel(Object obj, DemoInstVarsAccessor demoInstVars) {
        this.demoInstVars = demoInstVars;

        setLayout(new BorderLayout());
        try {
            if (obj instanceof String) {
                className = (String) obj;
                obj = Class.forName(className).getDeclaredConstructor().newInstance();
            }
            if (obj instanceof Component) {
                add((Component) obj);
            }
            if (obj instanceof Surface) {
                add("South", tools = new Tools(surface = (Surface) obj, demoInstVars));
            }
            if (obj instanceof CustomControlsContext) {
                ccc = (CustomControlsContext) obj;
                Component[] cmps = ccc.getControls();
                String[] cons = ccc.getConstraints();
                for (int i = 0; i < cmps.length; i++) {
                    add(cmps[i], cons[i]);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(DemoPanel.class.getName()).log(Level.SEVERE, null,
                    e);
        }
    }

    public void start() {
        if (surface != null) {
            surface.startClock();
        }
        if (tools != null && surface != null) {
            if (tools.startStopB != null && tools.startStopB.isSelected()) {
                surface.animating.start();
            }
        }
        if (ccc != null
                && demoInstVars.getCcthreadCB() != null
                && demoInstVars.getCcthreadCB().isSelected()) {
            ccc.handleThread(START);
        }
    }

    public void stop() {
        if (surface != null) {
            if (surface.animating != null) {
                surface.animating.stop();
            }
            surface.bimg = null;
        }
        if (ccc != null) {
            ccc.handleThread(STOP);
        }
    }

    public void setDemoBorder(JPanel p) {
        int top = (p.getComponentCount() + 1 >= 3) ? 0 : 5;
        int left = ((p.getComponentCount() + 1) % 2) == 0 ? 0 : 5;
        EmptyBorder eb = new EmptyBorder(top, left, 5, 5);
        SoftBevelBorder sbb = new SoftBevelBorder(BevelBorder.RAISED);
        setBorder(new CompoundBorder(eb, sbb));
    }
}
