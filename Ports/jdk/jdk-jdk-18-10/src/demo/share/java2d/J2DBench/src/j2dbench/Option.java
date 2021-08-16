/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package j2dbench;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.border.LineBorder;
import java.io.PrintWriter;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public abstract class Option extends Node implements Modifier {
    public Option(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
    }

    public abstract boolean isDefault();

    public void modifyTest(TestEnvironment env, Object val) {
        env.setModifier(this, val);
    }

    public void restoreTest(TestEnvironment env, Object val) {
        env.removeModifier(this);
    }

    public abstract String getValString();

    public String getValString(Object v) {
        return v.toString();
    }

    public String getOptionString() {
        return getTreeName()+"="+getValString();
    }

    public String getOptionString(Object value) {
        return getTreeName()+"="+getValString(value);
    }

    public String getAbbreviatedModifierDescription(Object value) {
        return getNodeName()+"="+getValString(value);
    }

    public String getModifierValueName(Object val) {
        return getValString(val);
    }

    public String setOption(String key, String value) {
        if (key.length() != 0) {
            return "Option name too specific";
        }
        return setValueFromString(value);
    }

    public abstract String setValueFromString(String value);

    public void write(PrintWriter pw) {
        //if (!isDefault()) {
            pw.println(getOptionString());
        //}
    }

    public String toString() {
        return "Option("+getOptionString()+")";
    }

    public static class Toggle extends Option {
        public static final int Off = 0;
        public static final int On = 1;
        public static final int Both = 2;

        private static final String[] valnames = {"Off", "On", "Both"};
        private static final Boolean[][] valuelist = {
            BooleanIterator.FalseList,
            BooleanIterator.TrueList,
            BooleanIterator.FalseTrueList,
        };

        int defaultvalue;
        int value;
        JPanel jp;
        JComboBox jcb;

        public Toggle(Group parent, String nodeName, String description,
                      int defaultvalue)
        {
            super(parent, nodeName, description);
            if (defaultvalue != Off &&
                defaultvalue != On &&
                defaultvalue != Both)
            {
                throw new IllegalArgumentException("bad default");
            }
            this.defaultvalue = this.value = defaultvalue;
        }

        public void restoreDefault() {
            if (value != defaultvalue) {
                value = defaultvalue;
                updateGUI();
            }
        }

        public void updateGUI() {
            if (jcb != null) {
                jcb.setSelectedIndex(value);
            }
        }

        public boolean isDefault() {
            return (value == defaultvalue);
        }

        public Modifier.Iterator getIterator(TestEnvironment env) {
            return new BooleanIterator(valuelist[value]);
        }

        public JComponent getJComponent() {
            if (jp == null) {
                jp = new JPanel();
                jp.setLayout(new BorderLayout());
                JLabel jl = new JLabel(getDescription());
                jp.add(jl, BorderLayout.WEST);
                jcb = new JComboBox(valnames);
                updateGUI();
                jcb.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            JComboBox jcb = (JComboBox) e.getItemSelectable();
                            value = jcb.getSelectedIndex();
                            if (J2DBench.verbose.isEnabled()) {
                                System.out.println(getOptionString());
                            }
                        }
                    }
                });
                jp.add(jcb, BorderLayout.EAST);
            }
            return jp;
        }

        public String getAbbreviatedModifierDescription(Object value) {
            String ret = getNodeName();
            if (value.equals(Boolean.FALSE)) {
                ret = "!"+ret;
            }
            return ret;
        }

        public String getValString() {
            return valnames[value];
        }

        public String setValueFromString(String value) {
            for (int i = 0; i < valnames.length; i++) {
                if (valnames[i].equalsIgnoreCase(value)) {
                    if (this.value != i) {
                        this.value = i;
                        updateGUI();
                    }
                    return null;
                }
            }
            return "Bad value";
        }
    }

    public static class Enable extends Option {
        boolean defaultvalue;
        boolean value;
        JCheckBox jcb;

        public Enable(Group parent, String nodeName, String description,
                      boolean defaultvalue)
        {
            super(parent, nodeName, description);
            this.defaultvalue = this.value = defaultvalue;
        }

        public boolean isEnabled() {
            return value;
        }

        public void modifyTest(TestEnvironment env) {
            // Used from within a Group.EnableSet group.
        }

        public void restoreTest(TestEnvironment env) {
            // Used from within a Group.EnableSet group.
        }

        public void restoreDefault() {
            if (value != defaultvalue) {
                value = defaultvalue;
                updateGUI();
            }
        }

        public void updateGUI() {
            if (jcb != null) {
                jcb.setSelected(value);
            }
        }

        public boolean isDefault() {
            return (value == defaultvalue);
        }

        public Modifier.Iterator getIterator(TestEnvironment env) {
            return new BooleanIterator(value);
        }

        public JComponent getJComponent() {
            if (jcb == null) {
                jcb = new JCheckBox(getDescription());
                updateGUI();
                jcb.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        value = (e.getStateChange() == ItemEvent.SELECTED);
                        if (J2DBench.verbose.isEnabled()) {
                            System.out.println(getOptionString());
                        }
                    }
                });
            }
            return jcb;
        }

        public String getAbbreviatedModifierDescription(Object value) {
            String ret = getNodeName();
            if (value.equals(Boolean.FALSE)) {
                ret = "!"+ret;
            }
            return ret;
        }

        public String getValString() {
            return (value ? "enabled" : "disabled");
        }

        public String setValueFromString(String value) {
            boolean newval;
            if (value.equalsIgnoreCase("enabled")) {
                newval = true;
            } else if (value.equalsIgnoreCase("disabled")) {
                newval = false;
            } else {
                return "Bad Value";
            }
            if (this.value != newval) {
                this.value = newval;
                updateGUI();
            }
            return null;
        }
    }

    public static class Int extends Option {
        int minvalue;
        int maxvalue;
        int defaultvalue;
        int value;
        JPanel jp;
        JTextField jtf;

        public Int(Group parent, String nodeName, String description,
                   int minvalue, int maxvalue, int defaultvalue)
        {
            super(parent, nodeName, description);
            this.minvalue = minvalue;
            this.maxvalue = maxvalue;
            if (defaultvalue < minvalue || defaultvalue > maxvalue) {
                throw new RuntimeException("bad value string: "+value);
            }
            this.defaultvalue = this.value = defaultvalue;
        }

        public int getIntValue() {
            return value;
        }

        public void restoreDefault() {
            if (value != defaultvalue) {
                value = defaultvalue;
                updateGUI();
            }
        }

        public void updateGUI() {
            if (jtf != null) {
                jtf.setText(getValString());
            }
        }

        public boolean isDefault() {
            return (value == defaultvalue);
        }

        public Modifier.Iterator getIterator(TestEnvironment env) {
            return new SwitchIterator(new Object[] { new Integer(value) }, 1);
        }

        public JComponent getJComponent() {
            if (jp == null) {
                jp = new JPanel();
                jp.setLayout(new BorderLayout());
                jp.add(new JLabel(getDescription()), BorderLayout.WEST);
                jtf = new JTextField(10);
                updateGUI();
                jtf.setDocument(new PlainDocument() {
                    public void insertString(int offs, String str,
                                             AttributeSet a)
                        throws BadLocationException
                    {
                        if (str == null) {
                            return;
                        }
                        for (int i = 0; i < str.length(); i++) {
                            char c = str.charAt(i);
                            if (c < '0' || c > '9') {
                                Toolkit.getDefaultToolkit().beep();
                                return;
                            }
                        }
                        String oldstr = jtf.getText();
                        super.insertString(offs, str, a);
                        str = jtf.getText();
                        if (setValueFromString(str) == null) {
                            if (J2DBench.verbose.isEnabled()) {
                                System.out.println(getOptionString());
                            }
                        } else {
                            super.remove(0, super.getLength());
                            super.insertString(0, oldstr, null);
                            Toolkit.getDefaultToolkit().beep();
                        }
                    }
                });
                jtf.setText(getValString());
                jp.add(jtf, BorderLayout.EAST);
            }
            return jp;
        }

        public String getValString() {
            return Integer.toString(value);
        }

        public String setValueFromString(String value) {
            int val;
            try {
                val = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return "Value not an integer ("+value+")";
            }
            if (val < minvalue || val > maxvalue) {
                return "Value out of range";
            }
            if (this.value != val) {
                this.value = val;
                updateGUI();
            }
            return null;
        }
    }

    public static class ObjectList extends Option {
        int size;
        String[] optionnames;
        Object[] optionvalues;
        String[] abbrevnames;
        String[] descnames;
        int defaultenabled;
        int enabled;
        JPanel jp;
        JList jlist;
        int numrows;

        public ObjectList(Group parent, String nodeName, String description,
                          String[] optionnames,
                          Object[] optionvalues,
                          String[] abbrevnames,
                          String[] descnames,
                          int defaultenabled)
        {
            this(parent, nodeName, description,
                 Math.min(Math.min(optionnames.length,
                                   optionvalues.length),
                          Math.min(abbrevnames.length,
                                   descnames.length)),
                 optionnames, optionvalues,
                 abbrevnames, descnames, defaultenabled);
        }

        public ObjectList(Group parent, String nodeName, String description,
                          int size,
                          String[] optionnames,
                          Object[] optionvalues,
                          String[] abbrevnames,
                          String[] descnames,
                          int defaultenabled)
        {
            super(parent, nodeName, description);
            this.size = size;
            this.optionnames = trim(optionnames, size);
            this.optionvalues = trim(optionvalues, size);
            this.abbrevnames = trim(abbrevnames, size);
            this.descnames = trim(descnames, size);
            this.enabled = this.defaultenabled = defaultenabled;
        }

        private static String[] trim(String[] list, int size) {
            if (list.length == size) {
                return list;
            }
            String[] newlist = new String[size];
            System.arraycopy(list, 0, newlist, 0, size);
            return newlist;
        }

        private static Object[] trim(Object[] list, int size) {
            if (list.length == size) {
                return list;
            }
            Object[] newlist = new Object[size];
            System.arraycopy(list, 0, newlist, 0, size);
            return newlist;
        }

        public void restoreDefault() {
            if (enabled != defaultenabled) {
                enabled = defaultenabled;
                updateGUI();
            }
        }

        public void updateGUI() {
            if (jlist != null) {
                int enabled = this.enabled;
                jlist.clearSelection();
                for (int curindex = 0; curindex < size; curindex++) {
                    if ((enabled & (1 << curindex)) != 0) {
                        jlist.addSelectionInterval(curindex, curindex);
                    }
                }
            }
        }

        public boolean isDefault() {
            return (enabled == defaultenabled);
        }

        public Modifier.Iterator getIterator(TestEnvironment env) {
            return new SwitchIterator(optionvalues, enabled);
        }

        public void setNumRows(int numrows) {
            this.numrows = numrows;
        }

        public JComponent getJComponent() {
            if (jp == null) {
                jp = new JPanel();
                jp.setLayout(new BorderLayout());
                jp.add(new JLabel(getDescription()), BorderLayout.WEST);
                jlist = new JList(descnames);
                if (numrows > 0) {
                    try {
                        jlist.setLayoutOrientation(JList.VERTICAL_WRAP);
                    } catch (NoSuchMethodError e) {
                    }
                    jlist.setVisibleRowCount(numrows);
                }
                jlist.setBorder(new LineBorder(Color.black, 2));
                updateGUI();
                jlist.addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        int flags = 0;
                        for (int curindex = 0; curindex < size; curindex++) {
                            JList list = (JList) e.getSource();
                            if (list.isSelectedIndex(curindex)) {
                                flags |= (1 << curindex);
                            }
                        }
                        enabled = flags;
                        if (J2DBench.verbose.isEnabled()) {
                            System.out.println(getOptionString());
                        }
                    }
                });
                jp.add(jlist, BorderLayout.EAST);
            }
            return jp;
        }

        public String getValString() {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < size; i++) {
                if ((enabled & (1 << i)) != 0) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(optionnames[i]);
                }
            }
            return sb.toString();
        }

        int findValueIndex(Object value) {
            for (int i = 0; i < size; i++) {
                if (optionvalues[i] == value) {
                    return i;
                }
            }
            return -1;
        }

        public String getValString(Object value) {
            return optionnames[findValueIndex(value)];
        }

        public String getAbbreviatedModifierDescription(Object value) {
            return abbrevnames[findValueIndex(value)];
        }

        public String setValueFromString(String value) {
            int enabled = 0;
            StringTokenizer st = new StringTokenizer(value, ",");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                try {
                    for (int i = 0; i < size; i++) {
                        if (optionnames[i].equals(s)) {
                            enabled |= (1 << i);
                            s = null;
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                }
                if (s != null) {
                    return "Bad value in list ("+s+")";
                }
            }
            this.enabled = enabled;
            updateGUI();
            return null;
        }
    }

    public static class IntList extends ObjectList {
        public IntList(Group parent, String nodeName, String description,
                       int[] values, String[] abbrevnames, String[] descnames,
                       int defaultenabled)
        {
            super(parent, nodeName, description,
                  makeNames(values), makeValues(values),
                  abbrevnames, descnames, defaultenabled);
        }

        private static String[] makeNames(int[] intvalues) {
            String[] names = new String[intvalues.length];
            for (int i = 0; i < intvalues.length; i++) {
                names[i] = Integer.toString(intvalues[i]);
            }
            return names;
        }

        private static Object[] makeValues(int[] intvalues) {
            Object[] values = new Object[intvalues.length];
            for (int i = 0; i < intvalues.length; i++) {
                values[i] = new Integer(intvalues[i]);
            }
            return values;
        }
    }

    public static class ObjectChoice extends Option {
         int size;
         String[] optionnames;
         Object[] optionvalues;
         String[] abbrevnames;
         String[] descnames;
         int defaultselected;
         int selected;
         JPanel jp;
         JComboBox jcombo;

         public ObjectChoice(Group parent, String nodeName, String description,
                             String[] optionnames,
                             Object[] optionvalues,
                             String[] abbrevnames,
                             String[] descnames,
                             int defaultselected)
         {
             this(parent, nodeName, description,
                  Math.min(Math.min(optionnames.length,
                                    optionvalues.length),
                           Math.min(abbrevnames.length,
                                    descnames.length)),
                  optionnames, optionvalues,
                  abbrevnames, descnames, defaultselected);
         }

         public ObjectChoice(Group parent, String nodeName, String description,
                             int size,
                             String[] optionnames,
                             Object[] optionvalues,
                             String[] abbrevnames,
                             String[] descnames,
                             int defaultselected)
         {
             super(parent, nodeName, description);
             this.size = size;
             this.optionnames = trim(optionnames, size);
             this.optionvalues = trim(optionvalues, size);
             this.abbrevnames = trim(abbrevnames, size);
             this.descnames = trim(descnames, size);
             this.selected = this.defaultselected = defaultselected;
         }

         private static String[] trim(String[] list, int size) {
             if (list.length == size) {
                 return list;
             }
             String[] newlist = new String[size];
             System.arraycopy(list, 0, newlist, 0, size);
             return newlist;
         }

         private static Object[] trim(Object[] list, int size) {
             if (list.length == size) {
                 return list;
             }
             Object[] newlist = new Object[size];
             System.arraycopy(list, 0, newlist, 0, size);
             return newlist;
         }

         public void restoreDefault() {
             if (selected != defaultselected) {
                 selected = defaultselected;
                 updateGUI();
             }
         }

         public void updateGUI() {
             if (jcombo != null) {
                 jcombo.setSelectedIndex(this.selected);
             }
         }

         public boolean isDefault() {
             return (selected == defaultselected);
         }

         public Modifier.Iterator getIterator(TestEnvironment env) {
             return new SwitchIterator(optionvalues, 1 << selected);
         }

         public JComponent getJComponent() {
             if (jp == null) {
                 jp = new JPanel();
                 jp.setLayout(new BorderLayout());
                 jp.add(new JLabel(getDescription()), BorderLayout.WEST);
                 jcombo = new JComboBox(descnames);
                 updateGUI();
                 jcombo.addItemListener(new ItemListener() {
                     public void itemStateChanged(ItemEvent e) {
                         if (e.getStateChange() == ItemEvent.SELECTED) {
                             selected = jcombo.getSelectedIndex();
                             if (J2DBench.verbose.isEnabled()) {
                                 System.out.println(getOptionString());
                             }
                         }
                     }
                 });
                 jp.add(jcombo, BorderLayout.EAST);
             }
             return jp;
         }

         public Object getValue() {
             return optionvalues[selected];
         }

         public int getIntValue() {
             return ((Integer) optionvalues[selected]).intValue();
         }

         public boolean getBooleanValue() {
             return ((Boolean) optionvalues[selected]).booleanValue();
         }

         public String getValString() {
             return optionnames[selected];
         }

         int findValueIndex(Object value) {
             for (int i = 0; i < size; i++) {
                 if (optionvalues[i] == value) {
                     return i;
                 }
             }
             return -1;
         }

         public String getValString(Object value) {
             return optionnames[findValueIndex(value)];
         }

         public String getAbbreviatedModifierDescription(Object value) {
             return abbrevnames[findValueIndex(value)];
         }

         public String setValue(int v) {
             return setValue(new Integer(v));
         }

         public String setValue(boolean v) {
             return setValue(new Boolean(v));
         }

         public String setValue(Object value) {
             for (int i = 0; i < size; i++) {
                 if (optionvalues[i].equals(value)) {
                     this.selected = i;
                     updateGUI();
                     return null;
                 }
             }
             return "Bad value";
         }

         public String setValueFromString(String value) {
             for (int i = 0; i < size; i++) {
                 if (optionnames[i].equals(value)) {
                     this.selected = i;
                     updateGUI();
                     return null;
                 }
             }
             return "Bad value";
         }
    }

    public static class BooleanIterator implements Modifier.Iterator {
        private Boolean[] list;
        private int index;

        public static final Boolean[] FalseList = { Boolean.FALSE };
        public static final Boolean[] TrueList = { Boolean.TRUE };
        public static final Boolean[]
            FalseTrueList = { Boolean.FALSE, Boolean.TRUE };
        public static final Boolean[]
            TrueFalseList = { Boolean.TRUE, Boolean.FALSE };

        public BooleanIterator(boolean v) {
            this(v ? TrueList : FalseList);
        }

        public BooleanIterator(Boolean[] list) {
            this.list = list;
        }

        public boolean hasNext() {
            return (index < list.length);
        }

        public Object next() {
            if (index >= list.length) {
                throw new NoSuchElementException();
            }
            return list[index++];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class SwitchIterator implements Modifier.Iterator {
        private Object[] list;
        private int enabled;
        private int index;

        public SwitchIterator(Object[] list, int enabled) {
            this.list = list;
            this.enabled = enabled;
        }

        public boolean hasNext() {
            return ((1 << index) <= enabled);
        }

        public Object next() {
            while ((enabled & (1 << index)) == 0) {
                index++;
                if (index >= list.length) {
                    throw new NoSuchElementException();
                }
            }
            return list[index++];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
