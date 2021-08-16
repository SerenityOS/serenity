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


import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Global Controls panel for changing graphic attributes of
 * the demo surface.
 */
@SuppressWarnings("serial")
public class GlobalControls extends JPanel implements ItemListener,
        ChangeListener {
    private final DemoInstVarsAccessor demoInstVars;
    public static final String[] screenNames = {
        "Auto Screen", "On Screen", "Off Screen",
        "INT_xRGB", "INT_ARGB", "INT_ARGB_PRE", "INT_BGR",
        "3BYTE_BGR", "4BYTE_ABGR", "4BYTE_ABGR_PRE", "USHORT_565_RGB",
        "USHORT_x555_RGB", "BYTE_GRAY", "USHORT_GRAY",
        "BYTE_BINARY", "BYTE_INDEXED", "BYTE_BINARY 2 bit", "BYTE_BINARY 4 bit",
        "INT_RGBx", "USHORT_555x_RGB" };
    public final JComboBox<String> screenCombo;
    public TextureChooser texturechooser;
    public JCheckBox aliasCB, renderCB, toolBarCB;
    public JCheckBox compositeCB, textureCB;
    public JSlider slider;
    public Object obj;
    private Font font = new Font(Font.SERIF, Font.PLAIN, 12);

    @SuppressWarnings("LeakingThisInConstructor")
    public GlobalControls(DemoInstVarsAccessor demoInstVars) {
        this.demoInstVars = demoInstVars;

        setLayout(new GridBagLayout());
        setBorder(new TitledBorder(new EtchedBorder(), "Global Controls"));

        aliasCB = createCheckBox("Anti-Aliasing", true, 0);
        renderCB = createCheckBox("Rendering Quality", false, 1);
        textureCB = createCheckBox("Texture", false, 2);
        compositeCB = createCheckBox("AlphaComposite", false, 3);

        screenCombo = new JComboBox<>();
        screenCombo.setPreferredSize(new Dimension(120, 18));
        screenCombo.setLightWeightPopupEnabled(true);
        screenCombo.setFont(font);
        for (int i = 0; i < screenNames.length; i++) {
            screenCombo.addItem(screenNames[i]);
        }
        screenCombo.addItemListener(this);
        J2Ddemo.addToGridBag(this, screenCombo, 0, 4, 1, 1, 0.0, 0.0);

        toolBarCB = createCheckBox("Tools", false, 5);

        slider = new JSlider(SwingConstants.HORIZONTAL, 0, 200, 30);
        slider.addChangeListener(this);
        TitledBorder tb = new TitledBorder(new EtchedBorder());
        tb.setTitleFont(font);
        tb.setTitle("Anim delay = 30 ms");
        slider.setBorder(tb);
        slider.setMinimumSize(new Dimension(80, 46));
        J2Ddemo.addToGridBag(this, slider, 0, 6, 1, 1, 1.0, 1.0);

        texturechooser = new TextureChooser(0, demoInstVars);
        J2Ddemo.addToGridBag(this, texturechooser, 0, 7, 1, 1, 1.0, 1.0);
    }

    private JCheckBox createCheckBox(String s, boolean b, int y) {
        JCheckBox cb = new JCheckBox(s, b);
        cb.setFont(font);
        cb.setHorizontalAlignment(SwingConstants.LEFT);
        cb.addItemListener(this);
        J2Ddemo.addToGridBag(this, cb, 0, y, 1, 1, 1.0, 1.0);
        return cb;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        int value = slider.getValue();
        TitledBorder tb = (TitledBorder) slider.getBorder();
        tb.setTitle("Anim delay = " + String.valueOf(value) + " ms");
        int index = demoInstVars.getTabbedPane().getSelectedIndex() - 1;
        DemoGroup dg = demoInstVars.getGroup()[index];
        JPanel p = dg.getPanel();
        for (int i = 0; i < p.getComponentCount(); i++) {
            DemoPanel dp = (DemoPanel) p.getComponent(i);
            if (dp.tools != null && dp.tools.slider != null) {
                dp.tools.slider.setValue(value);
            }
        }
        slider.repaint();
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (demoInstVars.getTabbedPane().getSelectedIndex() != 0) {
            obj = e.getSource();
            int index = demoInstVars.getTabbedPane().getSelectedIndex() - 1;
            demoInstVars.getGroup()[index].setup(true);
            obj = null;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(135, 260);
    }
}
