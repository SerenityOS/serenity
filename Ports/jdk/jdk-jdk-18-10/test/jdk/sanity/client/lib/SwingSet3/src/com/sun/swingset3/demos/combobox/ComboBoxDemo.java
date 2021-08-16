/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.swingset3.demos.combobox;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.accessibility.AccessibleRelation;
import javax.swing.*;
import javax.swing.border.BevelBorder;

import com.sun.swingset3.demos.ResourceManager;
import com.sun.swingset3.DemoProperties;

/**
 * JComboBox Demo
 *
 * @author Jeff Dinkins
 * @version 1.13 11/17/05
 */
@DemoProperties(
        value = "JComboBox Demo",
        category = "Controls",
        description = "Demonstrates JComboBox, a control which allows the user to make a selection from a popup list",
        sourceFiles = {
            "com/sun/swingset3/demos/combobox/ComboBoxDemo.java",
            "com/sun/swingset3/demos/ResourceManager.java",
            "com/sun/swingset3/demos/combobox/resources/ComboBoxDemo.properties",
            "com/sun/swingset3/demos/combobox/resources/images/brenteyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/brenthair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/brentmouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/ComboBoxDemo.gif",
            "com/sun/swingset3/demos/combobox/resources/images/georgeseyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/georgeshair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/georgesmouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/hanseyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/hanshair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/hansmouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/howardeyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/howardhair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/howardmouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/jameseyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/jameshair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/jamesmouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/jeffeyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/jeffhair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/jeffmouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/joneyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/jonhair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/jonmouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/laraeyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/larahair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/laramouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/larryeyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/larryhair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/larrymouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/lisaeyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/lisahair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/lisamouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/michaeleyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/michaelhair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/michaelmouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/philipeyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/philiphair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/philipmouth.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/scotteyes.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/scotthair.jpg",
            "com/sun/swingset3/demos/combobox/resources/images/scottmouth.jpg"
        }
)
public class ComboBoxDemo extends JPanel implements ActionListener {

    private static final Dimension VGAP15 = new Dimension(1, 15);
    private static final Dimension HGAP20 = new Dimension(20, 1);
    private static final Dimension VGAP20 = new Dimension(1, 20);
    private static final Dimension HGAP30 = new Dimension(30, 1);
    private static final Dimension VGAP30 = new Dimension(1, 30);

    private final ResourceManager resourceManager = new ResourceManager(this.getClass());
    public static final String DEMO_TITLE = ComboBoxDemo.class.getAnnotation(DemoProperties.class).value();

    private Face face;
    private JLabel faceLabel;

    private JComboBox<String> hairCB;
    private JComboBox<String> eyesCB;
    private JComboBox<String> mouthCB;

    private JComboBox<String> presetCB;

    private final Map<String, Object> parts = new HashMap<>();

    /**
     * main method allows us to run as a standalone demo.
     *
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(DEMO_TITLE);

        frame.getContentPane().add(new ComboBoxDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * ComboBoxDemo Constructor
     */
    public ComboBoxDemo() {
        createComboBoxDemo();
    }

    private void createComboBoxDemo() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));

        add(Box.createRigidArea(VGAP20));
        add(innerPanel);
        add(Box.createRigidArea(VGAP20));

        innerPanel.add(Box.createRigidArea(HGAP20));

        // Create a panel to hold buttons
        JPanel comboBoxPanel = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(getPreferredSize().width, super.getMaximumSize().height);
            }
        };
        comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel, BoxLayout.Y_AXIS));

        comboBoxPanel.add(Box.createRigidArea(VGAP15));

        JLabel l = (JLabel) comboBoxPanel.add(new JLabel(resourceManager.getString("ComboBoxDemo.presets")));
        l.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        comboBoxPanel.add(presetCB = createPresetComboBox());
        presetCB.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
        l.setLabelFor(presetCB);
        comboBoxPanel.add(Box.createRigidArea(VGAP30));

        l = (JLabel) comboBoxPanel.add(new JLabel(resourceManager.getString("ComboBoxDemo.hair_description")));
        l.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        comboBoxPanel.add(hairCB = createHairComboBox());
        hairCB.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
        l.setLabelFor(hairCB);
        comboBoxPanel.add(Box.createRigidArea(VGAP15));

        l = (JLabel) comboBoxPanel.add(new JLabel(resourceManager.getString("ComboBoxDemo.eyes_description")));
        l.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        comboBoxPanel.add(eyesCB = createEyesComboBox());
        eyesCB.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
        l.setLabelFor(eyesCB);
        comboBoxPanel.add(Box.createRigidArea(VGAP15));

        l = (JLabel) comboBoxPanel.add(new JLabel(resourceManager.getString("ComboBoxDemo.mouth_description")));
        l.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        comboBoxPanel.add(mouthCB = createMouthComboBox());

        mouthCB.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
        l.setLabelFor(mouthCB);
        comboBoxPanel.add(Box.createRigidArea(VGAP15));

        // Fill up the remaining space
        comboBoxPanel.add(new JPanel(new BorderLayout()));

        // Create and place the Face.
        face = new Face();
        JPanel facePanel = new JPanel();
        facePanel.setLayout(new BorderLayout());
        facePanel.setBorder(new BevelBorder(BevelBorder.LOWERED));

        faceLabel = new JLabel(face);
        facePanel.add(faceLabel, BorderLayout.CENTER);
        // Indicate that the face panel is controlled by the hair, eyes and
        // mouth combo boxes.
        Object[] controlledByObjects = new Object[3];
        controlledByObjects[0] = hairCB;
        controlledByObjects[1] = eyesCB;
        controlledByObjects[2] = mouthCB;
        AccessibleRelation controlledByRelation
                = new AccessibleRelation(AccessibleRelation.CONTROLLED_BY_PROPERTY,
                        controlledByObjects);
        facePanel.getAccessibleContext().getAccessibleRelationSet().add(controlledByRelation);

        // Indicate that the hair, eyes and mouth combo boxes are controllers
        // for the face panel.
        AccessibleRelation controllerForRelation
                = new AccessibleRelation(AccessibleRelation.CONTROLLER_FOR_PROPERTY,
                        facePanel);
        hairCB.getAccessibleContext().getAccessibleRelationSet().add(controllerForRelation);
        eyesCB.getAccessibleContext().getAccessibleRelationSet().add(controllerForRelation);
        mouthCB.getAccessibleContext().getAccessibleRelationSet().add(controllerForRelation);

        // add buttons and image panels to inner panel
        innerPanel.add(comboBoxPanel);
        innerPanel.add(Box.createRigidArea(HGAP30));
        innerPanel.add(facePanel);
        innerPanel.add(Box.createRigidArea(HGAP20));

        // load up the face parts
        addFace("brent", resourceManager.getString("ComboBoxDemo.brent"));
        addFace("georges", resourceManager.getString("ComboBoxDemo.georges"));
        addFace("hans", resourceManager.getString("ComboBoxDemo.hans"));
        addFace("howard", resourceManager.getString("ComboBoxDemo.howard"));
        addFace("james", resourceManager.getString("ComboBoxDemo.james"));
        addFace("jeff", resourceManager.getString("ComboBoxDemo.jeff"));
        addFace("jon", resourceManager.getString("ComboBoxDemo.jon"));
        addFace("lara", resourceManager.getString("ComboBoxDemo.lara"));
        addFace("larry", resourceManager.getString("ComboBoxDemo.larry"));
        addFace("lisa", resourceManager.getString("ComboBoxDemo.lisa"));
        addFace("michael", resourceManager.getString("ComboBoxDemo.michael"));
        addFace("philip", resourceManager.getString("ComboBoxDemo.philip"));
        addFace("scott", resourceManager.getString("ComboBoxDemo.scott"));

        // set the default face
        presetCB.setSelectedIndex(0);
    }

    private void addFace(String name, String i18n_name) {
        ImageIcon i;
        String i18n_hair = resourceManager.getString("ComboBoxDemo.hair");
        String i18n_eyes = resourceManager.getString("ComboBoxDemo.eyes");
        String i18n_mouth = resourceManager.getString("ComboBoxDemo.mouth");

        parts.put(i18n_name, name); // i18n name lookup
        parts.put(name, i18n_name); // reverse name lookup

        i = resourceManager.createImageIcon(name + "hair.jpg", i18n_name + i18n_hair);
        parts.put(name + "hair", i);

        i = resourceManager.createImageIcon(name + "eyes.jpg", i18n_name + i18n_eyes);
        parts.put(name + "eyes", i);

        i = resourceManager.createImageIcon(name + "mouth.jpg", i18n_name + i18n_mouth);
        parts.put(name + "mouth", i);
    }

    private JComboBox<String> createHairComboBox() {
        JComboBox<String> cb = new JComboBox<>();
        fillComboBox(cb);
        cb.addActionListener(this);
        return cb;
    }

    private JComboBox<String> createEyesComboBox() {
        JComboBox<String> cb = new JComboBox<>();
        fillComboBox(cb);
        cb.addActionListener(this);
        return cb;
    }

    private JComboBox<String> createMouthComboBox() {
        JComboBox<String> cb = new JComboBox<>();
        fillComboBox(cb);
        cb.addActionListener(this);
        return cb;
    }

    private JComboBox<String> createPresetComboBox() {
        JComboBox<String> cb = new JComboBox<>();
        cb.addItem(resourceManager.getString("ComboBoxDemo.preset1"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.preset2"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.preset3"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.preset4"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.preset5"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.preset6"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.preset7"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.preset8"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.preset9"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.preset10"));
        cb.addActionListener(this);
        return cb;
    }

    private void fillComboBox(JComboBox<String> cb) {
        cb.addItem(resourceManager.getString("ComboBoxDemo.brent"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.georges"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.hans"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.howard"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.james"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.jeff"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.jon"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.lara"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.larry"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.lisa"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.michael"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.philip"));
        cb.addItem(resourceManager.getString("ComboBoxDemo.scott"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == hairCB) {
            String name = (String) parts.get(hairCB.getSelectedItem());
            face.setHair((ImageIcon) parts.get(name + "hair"));
            faceLabel.repaint();
        } else if (e.getSource() == eyesCB) {
            String name = (String) parts.get(eyesCB.getSelectedItem());
            face.setEyes((ImageIcon) parts.get(name + "eyes"));
            faceLabel.repaint();
        } else if (e.getSource() == mouthCB) {
            String name = (String) parts.get(mouthCB.getSelectedItem());
            face.setMouth((ImageIcon) parts.get(name + "mouth"));
            faceLabel.repaint();
        } else if (e.getSource() == presetCB) {
            String hair = null;
            String eyes = null;
            String mouth = null;
            switch (presetCB.getSelectedIndex()) {
                case 0:
                    hair = (String) parts.get("philip");
                    eyes = (String) parts.get("howard");
                    mouth = (String) parts.get("jeff");
                    break;
                case 1:
                    hair = (String) parts.get("jeff");
                    eyes = (String) parts.get("larry");
                    mouth = (String) parts.get("philip");
                    break;
                case 2:
                    hair = (String) parts.get("howard");
                    eyes = (String) parts.get("scott");
                    mouth = (String) parts.get("hans");
                    break;
                case 3:
                    hair = (String) parts.get("philip");
                    eyes = (String) parts.get("jeff");
                    mouth = (String) parts.get("hans");
                    break;
                case 4:
                    hair = (String) parts.get("brent");
                    eyes = (String) parts.get("jon");
                    mouth = (String) parts.get("scott");
                    break;
                case 5:
                    hair = (String) parts.get("lara");
                    eyes = (String) parts.get("larry");
                    mouth = (String) parts.get("lisa");
                    break;
                case 6:
                    hair = (String) parts.get("james");
                    eyes = (String) parts.get("philip");
                    mouth = (String) parts.get("michael");
                    break;
                case 7:
                    hair = (String) parts.get("philip");
                    eyes = (String) parts.get("lisa");
                    mouth = (String) parts.get("brent");
                    break;
                case 8:
                    hair = (String) parts.get("james");
                    eyes = (String) parts.get("philip");
                    mouth = (String) parts.get("jon");
                    break;
                case 9:
                    hair = (String) parts.get("lara");
                    eyes = (String) parts.get("jon");
                    mouth = (String) parts.get("scott");
                    break;
            }
            if (hair != null) {
                hairCB.setSelectedItem(hair);
                eyesCB.setSelectedItem(eyes);
                mouthCB.setSelectedItem(mouth);
                faceLabel.repaint();
            }
        }
    }

    private static class Face implements Icon {

        private ImageIcon hair;
        private ImageIcon eyes;
        private ImageIcon mouth;

        void setHair(ImageIcon i) {
            hair = i;
        }

        void setEyes(ImageIcon i) {
            eyes = i;
        }

        void setMouth(ImageIcon i) {
            mouth = i;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            int height = y;
            x = c.getWidth() / 2 - getIconWidth() / 2;

            if (hair != null) {
                hair.paintIcon(c, g, x, height);
                height += hair.getIconHeight();
            }

            if (eyes != null) {
                eyes.paintIcon(c, g, x, height);
                height += eyes.getIconHeight();
            }

            if (mouth != null) {
                mouth.paintIcon(c, g, x, height);
            }
        }

        @Override
        public int getIconWidth() {
            return 344;
        }

        @Override
        public int getIconHeight() {
            return 455;
        }
    }
}
