/*
 *
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JTabbedPane;

/**
 * The implementation of 'DemoInstVarsAccessor' interface with empty methods.
 * It is used, when some parts of the demo are executed as standalone applications
 * not creating 'J2Ddemo' instances, for example in 'TextureChooser.main',
 * 'DemoGroup.main', 'Surface.createDemoFrame'.
 */
public class DemoInstVarsAccessorImplBase implements DemoInstVarsAccessor {
    private JCheckBoxMenuItem printCB = new JCheckBoxMenuItem("Default Printer");

    @Override
    public GlobalControls getControls() {
        return null;
    }

    @Override
    public MemoryMonitor getMemoryMonitor() {
        return null;
    }

    @Override
    public PerformanceMonitor getPerformanceMonitor() {
        return null;
    }

    @Override
    public JTabbedPane getTabbedPane() {
        return null;
    }

    @Override
    public DemoGroup[] getGroup() {
        return null;
    }

    @Override
    public void setGroupColumns(int columns) {
    }

    @Override
    public JCheckBoxMenuItem getVerboseCB() {
        return null;
    }

    @Override
    public JCheckBoxMenuItem getCcthreadCB() {
        return null;
    }

    @Override
    public JCheckBoxMenuItem getPrintCB() {
        return printCB;
    }

    @Override
    public Color getBackgroundColor() {
        return null;
    }

    @Override
    public JCheckBoxMenuItem getMemoryCB() {
        return null;
    }

    @Override
    public JCheckBoxMenuItem getPerfCB() {
        return null;
    }

    @Override
    public Intro getIntro() {
        return null;
    }
}
