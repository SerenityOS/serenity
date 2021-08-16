/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SingleSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.plaf.metal.OceanTheme;

/**
 * A demo that shows all of the Swing components.
 *
 * @author Jeff Dinkins
 */
public class SwingSet2 extends JPanel {

    String[] demos = {
      "ButtonDemo",
      "ColorChooserDemo",
      "ComboBoxDemo",
      "FileChooserDemo",
      "HtmlDemo",
      "ListDemo",
      "OptionPaneDemo",
      "ProgressBarDemo",
      "ScrollPaneDemo",
      "SliderDemo",
      "SplitPaneDemo",
      "TabbedPaneDemo",
      "TableDemo",
      "ToolTipDemo",
      "TreeDemo"
    };

     // The current Look & Feel
    private static LookAndFeelData currentLookAndFeel;
    private static LookAndFeelData[] lookAndFeelData;
    // List of demos
    private ArrayList<DemoModule> demosList = new ArrayList<DemoModule>();

    public static final String FRAME_TITLE = getString("Frame.title");
    public static final String THEMES_MENU_TEXT = getString("ThemesMenu.themes_label");
    public static final String OPTIONS_MENU_TEXT = getString("OptionsMenu.options_label");
    public static final String FONT_MENU_TEXT = getString("FontMenu.fonts_label");
    public static final String FONT_PLAIN_MENU_TEXT = getString("FontMenu.plain_label");
    public static final String FONT_BOLD_MENU_TEXT = getString("FontMenu.bold_label");
    public static final String TOOLTIP_MENU_TEXT = getString("OptionsMenu.tooltip_label");
    public static final String OCEAN_MENU_TEXT = getString("ThemesMenu.ocean_label");
    public static final String AQUA_MENU_TEXT = getString("ThemesMenu.aqua_label");
    public static final String STEEL_MENU_TEXT = getString("ThemesMenu.steel_label");
    public static final String CONTRAST_MENU_TEXT = getString("ThemesMenu.contrast_label");
    public static final String CHARCOAL_MENU_TEXT = getString("ThemesMenu.charcoal_label");
    public static final String EMERALD_MENU_TEXT = getString("ThemesMenu.emerald_label");
    public static final String RUBY_MENU_TEXT= getString("ThemesMenu.ruby_label");
    public static final String NAME_PROP_SUFFIX = ".name";

    // The preferred size of the demo
    private static final int PREFERRED_WIDTH = 720;
    private static final int PREFERRED_HEIGHT = 640;

    // A place to hold on to the visible demo
    private DemoModule currentDemo = null;
    private JPanel demoPanel = null;

    // About Box
    private JDialog aboutBox = null;

    // Status Bar
    private JTextField statusField = null;

    // Tool Bar
    private ToggleButtonToolBar toolbar = null;
    private ButtonGroup toolbarGroup = new ButtonGroup();

    // Menus
    private JMenuBar menuBar = null;
    private JMenu lafMenu = null;
    private JMenu themesMenu = null;
    private JMenu audioMenu = null;
    private JMenu optionsMenu = null;
    private ButtonGroup lafMenuGroup = new ButtonGroup();
    private ButtonGroup themesMenuGroup = new ButtonGroup();
    private ButtonGroup audioMenuGroup = new ButtonGroup();

    // Popup menu
    private JPopupMenu popupMenu = null;
    private ButtonGroup popupMenuGroup = new ButtonGroup();

    // Used only if swingset is an application
    private JFrame frame = null;

    // The tab pane that holds the demo
    private JTabbedPane tabbedPane = null;

    private JEditorPane demoSrcPane = null;


    // contentPane cache, saved from the application frame
    Container contentPane = null;


    // number of swingsets - for multiscreen
    // keep track of the number of SwingSets created - we only want to exit
    // the program when the last one has been closed.
    private static int numSSs = 0;
    private static Vector<SwingSet2> swingSets = new Vector<SwingSet2>();

    private boolean dragEnabled = false;

    /**
     * SwingSet2 Constructor
     */
    public SwingSet2(GraphicsConfiguration gc) {

        String lafClassName = UIManager.getLookAndFeel().getClass().getName();
        lookAndFeelData = getInstalledLookAndFeelData();
        currentLookAndFeel = Arrays.stream(lookAndFeelData)
                .filter(laf -> lafClassName.equals(laf.className))
                .findFirst().get();

        frame = createFrame(gc);

        // set the layout
        setLayout(new BorderLayout());

        // set the preferred size of the demo
        setPreferredSize(new Dimension(PREFERRED_WIDTH,PREFERRED_HEIGHT));

        initializeDemo();
        preloadFirstDemo();

        showSwingSet2();
    }

    /**
     * SwingSet2 Main.
     */
    public static void main(final String[] args) {
        // must run in EDT when constructing the GUI components
        SwingUtilities.invokeLater(() -> {
            // Create SwingSet on the default monitor
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            SwingSet2 swingset = new SwingSet2(GraphicsEnvironment.
                                         getLocalGraphicsEnvironment().
                                         getDefaultScreenDevice().
                                         getDefaultConfiguration());
        });
    }

    // *******************************************************
    // *************** Demo Loading Methods ******************
    // *******************************************************


    public void initializeDemo() {
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);

        menuBar = createMenus();
        frame.setJMenuBar(menuBar);

        // creates popup menu accessible via keyboard
        popupMenu = createPopupMenu();

        ToolBarPanel toolbarPanel = new ToolBarPanel();
        toolbarPanel.setLayout(new BorderLayout());
        toolbar = new ToggleButtonToolBar();
        toolbarPanel.add(toolbar, BorderLayout.CENTER);
        top.add(toolbarPanel, BorderLayout.SOUTH);
        toolbarPanel.addContainerListener(toolbarPanel);

        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.getModel().addChangeListener(new TabListener());

        statusField = new JTextField("");
        statusField.setEditable(false);
        add(statusField, BorderLayout.SOUTH);

        demoPanel = new JPanel();
        demoPanel.setLayout(new BorderLayout());
        demoPanel.setBorder(new EtchedBorder());
        tabbedPane.addTab("Hi There!", demoPanel);

        // Add html src code viewer
        demoSrcPane = new JEditorPane("text/html", getString("SourceCode.loading"));
        demoSrcPane.setEditable(false);

        JScrollPane scroller = new JScrollPane();
        scroller.getViewport().add(demoSrcPane);

        tabbedPane.addTab(
            getString("TabbedPane.src_label"),
            null,
            scroller,
            getString("TabbedPane.src_tooltip")
        );
    }

    DemoModule currentTabDemo = null;
    class TabListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            SingleSelectionModel model = (SingleSelectionModel) e.getSource();
            boolean srcSelected = model.getSelectedIndex() == 1;
            if(currentTabDemo != currentDemo && demoSrcPane != null && srcSelected) {
                demoSrcPane.setText(getString("SourceCode.loading"));
                repaint();
            }
            if(currentTabDemo != currentDemo && srcSelected) {
                currentTabDemo = currentDemo;
                setSourceCode(currentDemo);
            }
        }
    }

    /**
     * Create menus
     */
    public JMenuBar createMenus() {
        JMenuItem mi;
        // ***** create the menubar ****
        JMenuBar menuBar = new JMenuBar();
        menuBar.getAccessibleContext().setAccessibleName(
            getString("MenuBar.accessible_description"));

        // ***** create File menu
        JMenu fileMenu = (JMenu) menuBar.add(new JMenu(getString("FileMenu.file_label")));
        fileMenu.setMnemonic(getMnemonic("FileMenu.file_mnemonic"));
        fileMenu.getAccessibleContext().setAccessibleDescription(getString("FileMenu.accessible_description"));

        createMenuItem(fileMenu, "FileMenu.about_label", "FileMenu.about_mnemonic",
                       "FileMenu.about_accessible_description", new AboutAction(this));

        fileMenu.addSeparator();

        createMenuItem(fileMenu, "FileMenu.open_label", "FileMenu.open_mnemonic",
                       "FileMenu.open_accessible_description", null);

        createMenuItem(fileMenu, "FileMenu.save_label", "FileMenu.save_mnemonic",
                       "FileMenu.save_accessible_description", null);

        createMenuItem(fileMenu, "FileMenu.save_as_label", "FileMenu.save_as_mnemonic",
                       "FileMenu.save_as_accessible_description", null);

        fileMenu.addSeparator();

        createMenuItem(fileMenu, "FileMenu.exit_label", "FileMenu.exit_mnemonic",
                       "FileMenu.exit_accessible_description", new ExitAction(this)
        );

        // ***** create laf switcher menu
        lafMenu = (JMenu) menuBar.add(new JMenu(getString("LafMenu.laf_label")));
        lafMenu.setMnemonic(getMnemonic("LafMenu.laf_mnemonic"));
        lafMenu.getAccessibleContext().setAccessibleDescription(
            getString("LafMenu.laf_accessible_description"));

        for (LookAndFeelData lafData : lookAndFeelData) {
            mi = createLafMenuItem(lafMenu, lafData);
            mi.setSelected(lafData.equals(currentLookAndFeel));
        }

        // ***** create themes menu
        themesMenu = (JMenu) menuBar.add(new JMenu(THEMES_MENU_TEXT));
        themesMenu.setMnemonic(getMnemonic("ThemesMenu.themes_mnemonic"));
        themesMenu.getAccessibleContext().setAccessibleDescription(
            getString("ThemesMenu.themes_accessible_description"));

        // ***** create the audio submenu under the theme menu
        audioMenu = (JMenu) themesMenu.add(new JMenu(getString("AudioMenu.audio_label")));
        audioMenu.setMnemonic(getMnemonic("AudioMenu.audio_mnemonic"));
        audioMenu.getAccessibleContext().setAccessibleDescription(
            getString("AudioMenu.audio_accessible_description"));

        createAudioMenuItem(audioMenu, "AudioMenu.on_label",
                            "AudioMenu.on_mnemonic",
                            "AudioMenu.on_accessible_description",
                            new OnAudioAction(this));

        mi = createAudioMenuItem(audioMenu, "AudioMenu.default_label",
                                 "AudioMenu.default_mnemonic",
                                 "AudioMenu.default_accessible_description",
                                 new DefaultAudioAction(this));
        mi.setSelected(true); // This is the default feedback setting

        createAudioMenuItem(audioMenu, "AudioMenu.off_label",
                            "AudioMenu.off_mnemonic",
                            "AudioMenu.off_accessible_description",
                            new OffAudioAction(this));


        // ***** create the font submenu under the theme menu
        JMenu fontMenu = (JMenu) themesMenu.add(new JMenu(FONT_MENU_TEXT));
        fontMenu.setMnemonic(getMnemonic("FontMenu.fonts_mnemonic"));
        fontMenu.getAccessibleContext().setAccessibleDescription(
            getString("FontMenu.fonts_accessible_description"));
        ButtonGroup fontButtonGroup = new ButtonGroup();
        mi = createButtonGroupMenuItem(fontMenu, FONT_PLAIN_MENU_TEXT,
                "FontMenu.plain_mnemonic",
                "FontMenu.plain_accessible_description",
                new ChangeFontAction(this, true), fontButtonGroup);
        mi.setSelected(true);
        mi = createButtonGroupMenuItem(fontMenu, FONT_BOLD_MENU_TEXT,
                "FontMenu.bold_mnemonic",
                "FontMenu.bold_accessible_description",
                new ChangeFontAction(this, false), fontButtonGroup);



        // *** now back to adding color/font themes to the theme menu
        mi = createThemesMenuItem(themesMenu, OCEAN_MENU_TEXT,
                                              "ThemesMenu.ocean_mnemonic",
                                              "ThemesMenu.ocean_accessible_description",
                                              new OceanTheme());
        mi.setSelected(true); // This is the default theme

        createThemesMenuItem(themesMenu, STEEL_MENU_TEXT,
                             "ThemesMenu.steel_mnemonic",
                             "ThemesMenu.steel_accessible_description",
                             new DefaultMetalTheme());

        createThemesMenuItem(themesMenu, AQUA_MENU_TEXT, "ThemesMenu.aqua_mnemonic",
                       "ThemesMenu.aqua_accessible_description", new AquaTheme());

        createThemesMenuItem(themesMenu, CHARCOAL_MENU_TEXT, "ThemesMenu.charcoal_mnemonic",
                       "ThemesMenu.charcoal_accessible_description", new CharcoalTheme());

        createThemesMenuItem(themesMenu, CONTRAST_MENU_TEXT, "ThemesMenu.contrast_mnemonic",
                       "ThemesMenu.contrast_accessible_description", new ContrastTheme());

        createThemesMenuItem(themesMenu, EMERALD_MENU_TEXT, "ThemesMenu.emerald_mnemonic",
                       "ThemesMenu.emerald_accessible_description", new EmeraldTheme());

        createThemesMenuItem(themesMenu, RUBY_MENU_TEXT, "ThemesMenu.ruby_mnemonic",
                       "ThemesMenu.ruby_accessible_description", new RubyTheme());

        // Enable theme menu based on L&F
        themesMenu.setEnabled("Metal".equals(currentLookAndFeel.name));

        // ***** create the options menu
        optionsMenu = (JMenu)menuBar.add(
            new JMenu(OPTIONS_MENU_TEXT));
        optionsMenu.setMnemonic(getMnemonic("OptionsMenu.options_mnemonic"));
        optionsMenu.getAccessibleContext().setAccessibleDescription(
            getString("OptionsMenu.options_accessible_description"));

        // ***** create tool tip submenu item.
        mi = createCheckBoxMenuItem(optionsMenu, TOOLTIP_MENU_TEXT,
                "OptionsMenu.tooltip_mnemonic",
                "OptionsMenu.tooltip_accessible_description",
                new ToolTipAction());
        mi.setSelected(true);

        // ***** create drag support submenu item.
        createCheckBoxMenuItem(optionsMenu, getString("OptionsMenu.dragEnabled_label"),
                "OptionsMenu.dragEnabled_mnemonic",
                "OptionsMenu.dragEnabled_accessible_description",
                new DragSupportAction());

        // ***** create the multiscreen menu, if we have multiple screens
        GraphicsDevice[] screens = GraphicsEnvironment.
                getLocalGraphicsEnvironment().
                getScreenDevices();
        if (screens.length > 1) {
            JMenu multiScreenMenu = (JMenu) menuBar.add(new JMenu(
                 getString("MultiMenu.multi_label")));
            multiScreenMenu.setMnemonic(getMnemonic("MultiMenu.multi_mnemonic"));
            multiScreenMenu.getAccessibleContext().setAccessibleDescription(
                    getString("MultiMenu.multi_accessible_description"));

            createMultiscreenMenuItem(multiScreenMenu, MultiScreenAction.ALL_SCREENS);
            for (int i = 0; i < screens.length; i++) {
                createMultiscreenMenuItem(multiScreenMenu, i);
            }
        }
        return menuBar;
    }

    /**
     * Create a checkbox menu menu item
     */
    private JMenuItem createCheckBoxMenuItem(JMenu menu, String label,
                                             String mnemonic,
                                             String accessibleDescription,
                                             Action action) {
        JCheckBoxMenuItem mi = (JCheckBoxMenuItem)menu.add(
                new JCheckBoxMenuItem(label));
        mi.setMnemonic(getMnemonic(mnemonic));
        mi.getAccessibleContext().setAccessibleDescription(getString(
                accessibleDescription));
        mi.addActionListener(action);
        return mi;
    }

    /**
     * Create a radio button menu menu item for items that are part of a
     * button group.
     */
    private JMenuItem createButtonGroupMenuItem(JMenu menu, String label,
                                                String mnemonic,
                                                String accessibleDescription,
                                                Action action,
                                                ButtonGroup buttonGroup) {
        JRadioButtonMenuItem mi = (JRadioButtonMenuItem)menu.add(
                new JRadioButtonMenuItem(label));
        buttonGroup.add(mi);
        mi.setMnemonic(getMnemonic(mnemonic));
        mi.getAccessibleContext().setAccessibleDescription(getString(
                accessibleDescription));
        mi.addActionListener(action);
        return mi;
    }

    /**
     * Create the theme's audio submenu
     */
    public JMenuItem createAudioMenuItem(JMenu menu, String label,
                                         String mnemonic,
                                         String accessibleDescription,
                                         Action action) {
        JRadioButtonMenuItem mi = (JRadioButtonMenuItem) menu.add(new JRadioButtonMenuItem(getString(label)));
        audioMenuGroup.add(mi);
        mi.setMnemonic(getMnemonic(mnemonic));
        mi.getAccessibleContext().setAccessibleDescription(getString(accessibleDescription));
        mi.addActionListener(action);
        return mi;
    }

    /**
     * Creates a generic menu item
     */
    public JMenuItem createMenuItem(JMenu menu, String label, String mnemonic,
                               String accessibleDescription, Action action) {
        JMenuItem mi = (JMenuItem) menu.add(new JMenuItem(getString(label)));
        mi.setMnemonic(getMnemonic(mnemonic));
        mi.getAccessibleContext().setAccessibleDescription(getString(accessibleDescription));
        mi.addActionListener(action);
        if(action == null) {
            mi.setEnabled(false);
        }
        return mi;
    }

    /**
     * Creates a JRadioButtonMenuItem for the Themes menu
     */
    public JMenuItem createThemesMenuItem(JMenu menu, String label, String mnemonic,
                               String accessibleDescription, MetalTheme theme) {
        JRadioButtonMenuItem mi = (JRadioButtonMenuItem) menu.add(new JRadioButtonMenuItem(label));
        themesMenuGroup.add(mi);
        mi.setMnemonic(getMnemonic(mnemonic));
        mi.getAccessibleContext().setAccessibleDescription(getString(accessibleDescription));
        mi.addActionListener(new ChangeThemeAction(this, theme));
        return mi;
    }

    /**
     * Creates a JRadioButtonMenuItem for the Look and Feel menu
     */
    public JMenuItem createLafMenuItem(JMenu menu, LookAndFeelData lafData) {
        JMenuItem mi = menu.add(new JRadioButtonMenuItem(lafData.label));
        lafMenuGroup.add(mi);
        mi.setMnemonic(lafData.mnemonic);
        mi.getAccessibleContext().setAccessibleDescription(lafData.accDescription);
        mi.addActionListener(new ChangeLookAndFeelAction(this, lafData));
        return mi;
    }

    /**
     * Creates a multi-screen menu item
     */
    public JMenuItem createMultiscreenMenuItem(JMenu menu, int screen) {
        JMenuItem mi = null;
        if (screen == MultiScreenAction.ALL_SCREENS) {
            mi = (JMenuItem) menu.add(new JMenuItem(getString("MultiMenu.all_label")));
            mi.setMnemonic(getMnemonic("MultiMenu.all_mnemonic"));
            mi.getAccessibleContext().setAccessibleDescription(getString(
                                                                 "MultiMenu.all_accessible_description"));
        }
        else {
            mi = (JMenuItem) menu.add(new JMenuItem(getString("MultiMenu.single_label") + " " +
                                                                                                 screen));
            mi.setMnemonic(KeyEvent.VK_0 + screen);
            mi.getAccessibleContext().setAccessibleDescription(getString(
                                               "MultiMenu.single_accessible_description") + " " + screen);

        }
        mi.addActionListener(new MultiScreenAction(this, screen));
        return mi;
    }

    public JPopupMenu createPopupMenu() {
        JPopupMenu popup = new JPopupMenu("JPopupMenu demo");

        for (LookAndFeelData lafData : lookAndFeelData) {
            createPopupMenuItem(popup, lafData);
        }

        // register key binding to activate popup menu
        InputMap map = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        map.put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_MASK),
                "postMenuAction");
        map.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "postMenuAction");
        getActionMap().put("postMenuAction", new ActivatePopupMenuAction(this, popup));

        return popup;
    }

    /**
     * Creates a JMenuItem for the Look and Feel popup menu
     */
    public JMenuItem createPopupMenuItem(JPopupMenu menu, LookAndFeelData lafData) {
        JMenuItem mi = menu.add(new JMenuItem(lafData.label));
        popupMenuGroup.add(mi);
        mi.setMnemonic(lafData.mnemonic);
        mi.getAccessibleContext().setAccessibleDescription(lafData.accDescription);
        mi.addActionListener(new ChangeLookAndFeelAction(this, lafData));
        return mi;
    }

    /**
     * Load the first demo. This is done separately from the remaining demos
     * so that we can get SwingSet2 up and available to the user quickly.
     */
    public void preloadFirstDemo() {
        DemoModule demo = addDemo(new OptionPaneDemo(this));
        setDemo(demo);
    }

    /**
     * Add a demo to the toolbar
     */
    public DemoModule addDemo(DemoModule demo) {
        demosList.add(demo);
        if (dragEnabled) {
            demo.updateDragEnabled(true);
        }
        // do the following on the gui thread
        SwingUtilities.invokeLater(new SwingSetRunnable(this, demo) {
            public void run() {
                SwitchToDemoAction action = new SwitchToDemoAction(swingset, (DemoModule) obj);
                JToggleButton tb = swingset.getToolBar().addToggleButton(action);
                swingset.getToolBarGroup().add(tb);
                if(swingset.getToolBarGroup().getSelection() == null) {
                    tb.setSelected(true);
                }
                tb.setText(null);
                tb.setToolTipText(((DemoModule)obj).getToolTip());

                if(demos[demos.length-1].equals(obj.getClass().getName())) {
                    setStatus(getString("Status.popupMenuAccessible"));
                }
            }
        });
        return demo;
    }

    /**
     * Sets the current demo
     */
    public void setDemo(DemoModule demo) {
        currentDemo = demo;

        // Ensure panel's UI is current before making visible
        JComponent currentDemoPanel = demo.getDemoPanel();
        SwingUtilities.updateComponentTreeUI(currentDemoPanel);

        demoPanel.removeAll();
        demoPanel.add(currentDemoPanel, BorderLayout.CENTER);

        tabbedPane.setSelectedIndex(0);
        tabbedPane.setTitleAt(0, demo.getName());
        tabbedPane.setToolTipTextAt(0, demo.getToolTip());
    }

    /**
     * Bring up the SwingSet2 demo by showing the frame
     */
    public void showSwingSet2() {
        if(getFrame() != null) {
            // put swingset in a frame and show it
            JFrame f = getFrame();
            f.setTitle(FRAME_TITLE);
            f.getContentPane().add(this, BorderLayout.CENTER);
            f.pack();

            Rectangle screenRect = f.getGraphicsConfiguration().getBounds();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
                    f.getGraphicsConfiguration());

            // Make sure we don't place the demo off the screen.
            int centerWidth = screenRect.width < f.getSize().width ?
                    screenRect.x :
                    screenRect.x + screenRect.width/2 - f.getSize().width/2;
            int centerHeight = screenRect.height < f.getSize().height ?
                    screenRect.y :
                    screenRect.y + screenRect.height/2 - f.getSize().height/2;

            centerHeight = centerHeight < screenInsets.top ?
                    screenInsets.top : centerHeight;

            f.setLocation(centerWidth, centerHeight);
            f.show();
            numSSs++;
            swingSets.add(this);
        }
    }

    // *******************************************************
    // ****************** Utility Methods ********************
    // *******************************************************

    /**
     * Returns the frame instance
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Returns the toolbar
     */
    public ToggleButtonToolBar getToolBar() {
        return toolbar;
    }

    /**
     * Returns the toolbar button group
     */
    public ButtonGroup getToolBarGroup() {
        return toolbarGroup;
    }

    /**
     * Create a frame for SwingSet2 to reside in if brought up
     * as an application.
     */
    public static JFrame createFrame(GraphicsConfiguration gc) {
        JFrame frame = new JFrame(gc);
        if (numSSs == 0) {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        } else {
            WindowListener l = new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    numSSs--;
                    swingSets.remove(this);
                }
            };
            frame.addWindowListener(l);
        }
        return frame;
    }

    /**
     * Set the status
     */
    public void setStatus(String s) {
        // do the following on the gui thread
        SwingUtilities.invokeLater(new SwingSetRunnable(this, s) {
            public void run() {
                swingset.statusField.setText((String) obj);
            }
        });
    }

    /**
     * This method returns a string from the demo's resource bundle.
     */
    public static String getString(String key) {
        String value = null;
        try {
            value = TextAndMnemonicUtils.getTextAndMnemonicString(key);
        } catch (MissingResourceException e) {
            System.out.println("java.util.MissingResourceException: Couldn't find value for: " + key);
        }
        if(value == null) {
            value = "Could not find resource: " + key + "  ";
        }
        return value;
    }

    void setDragEnabled(boolean dragEnabled) {
        if (dragEnabled == this.dragEnabled) {
            return;
        }

        this.dragEnabled = dragEnabled;

        for (DemoModule dm : demosList) {
            dm.updateDragEnabled(dragEnabled);
        }

        demoSrcPane.setDragEnabled(dragEnabled);
    }

    /**
     * Returns a mnemonic from the resource bundle. Typically used as
     * keyboard shortcuts in menu items.
     */
    public char getMnemonic(String key) {
        return (getString(key)).charAt(0);
    }

    /**
     * Creates an icon from an image contained in the "images" directory.
     */
    public ImageIcon createImageIcon(String filename, String description) {
        String path = "/resources/images/" + filename;
        return new ImageIcon(getClass().getResource(path));
    }

    /**
     * Stores the current L&F, and calls updateLookAndFeel, below
     */
    public void setLookAndFeel(LookAndFeelData laf) {
        if(!currentLookAndFeel.equals(laf)) {
            currentLookAndFeel = laf;
            /* The recommended way of synchronizing state between multiple
             * controls that represent the same command is to use Actions.
             * The code below is a workaround and will be replaced in future
             * version of SwingSet2 demo.
             */
            String lafName = laf.label;
            themesMenu.setEnabled(laf.name.equals("Metal"));
            updateLookAndFeel();
            for(int i=0;i<lafMenu.getItemCount();i++) {
                JMenuItem item = lafMenu.getItem(i);
                item.setSelected(item.getText().equals(lafName));
            }
        }
    }

    private void updateThisSwingSet() {
        JFrame frame = getFrame();
        if (frame == null) {
            SwingUtilities.updateComponentTreeUI(this);
        } else {
            SwingUtilities.updateComponentTreeUI(frame);
        }

        SwingUtilities.updateComponentTreeUI(popupMenu);
        if (aboutBox != null) {
            SwingUtilities.updateComponentTreeUI(aboutBox);
        }
    }

    /**
     * Sets the current L&F on each demo module
     */
    public void updateLookAndFeel() {
        try {
            UIManager.setLookAndFeel(currentLookAndFeel.className);
            for (SwingSet2 ss : swingSets) {
                ss.updateThisSwingSet();
            }
        } catch (Exception ex) {
            System.out.println("Failed loading L&F: " + currentLookAndFeel);
            System.out.println(ex);
        }
    }

    /**
     * Loads and puts the source code text into JEditorPane in the "Source Code" tab
     */
    public void setSourceCode(DemoModule demo) {
        // do the following on the gui thread
        SwingUtilities.invokeLater(new SwingSetRunnable(this, demo) {
            public void run() {
                swingset.demoSrcPane.setText(((DemoModule)obj).getSourceCode());
                swingset.demoSrcPane.setCaretPosition(0);

            }
        });
    }

    // *******************************************************
    // **************   ToggleButtonToolbar  *****************
    // *******************************************************
    static Insets zeroInsets = new Insets(1,1,1,1);
    protected class ToggleButtonToolBar extends JToolBar {
        public ToggleButtonToolBar() {
            super();
        }

        JToggleButton addToggleButton(Action a) {
            JToggleButton tb = new JToggleButton(
                (String)a.getValue(Action.NAME),
                (Icon)a.getValue(Action.SMALL_ICON)
            );
            tb.setName((String)a.getValue(Action.NAME));
            tb.setMargin(zeroInsets);
            tb.setText(null);
            tb.setEnabled(a.isEnabled());
            tb.setToolTipText((String)a.getValue(Action.SHORT_DESCRIPTION));
            tb.setAction(a);
            add(tb);
            return tb;
        }
    }

    // *******************************************************
    // *********  ToolBar Panel / Docking Listener ***********
    // *******************************************************
    class ToolBarPanel extends JPanel implements ContainerListener {

        public boolean contains(int x, int y) {
            Component c = getParent();
            if (c != null) {
                Rectangle r = c.getBounds();
                return (x >= 0) && (x < r.width) && (y >= 0) && (y < r.height);
            }
            else {
                return super.contains(x,y);
            }
        }

        public void componentAdded(ContainerEvent e) {
            Container c = e.getContainer().getParent();
            if (c != null) {
                c.getParent().validate();
                c.getParent().repaint();
            }
        }

        public void componentRemoved(ContainerEvent e) {
            Container c = e.getContainer().getParent();
            if (c != null) {
                c.getParent().validate();
                c.getParent().repaint();
            }
        }
    }

    // *******************************************************
    // ******************   Runnables  ***********************
    // *******************************************************

    /**
     * Generic SwingSet2 runnable. This is intended to run on the
     * AWT gui event thread so as not to muck things up by doing
     * gui work off the gui thread. Accepts a SwingSet2 and an Object
     * as arguments, which gives subtypes of this class the two
     * "must haves" needed in most runnables for this demo.
     */
    class SwingSetRunnable implements Runnable {
        protected SwingSet2 swingset;
        protected Object obj;

        public SwingSetRunnable(SwingSet2 swingset, Object obj) {
            this.swingset = swingset;
            this.obj = obj;
        }

        public void run() {
        }
    }


    // *******************************************************
    // ********************   Actions  ***********************
    // *******************************************************

    public class SwitchToDemoAction extends AbstractAction {
        SwingSet2 swingset;
        DemoModule demo;

        public SwitchToDemoAction(SwingSet2 swingset, DemoModule demo) {
            super(demo.getName(), demo.getIcon());
            this.swingset = swingset;
            this.demo = demo;
        }

        public void actionPerformed(ActionEvent e) {
            swingset.setDemo(demo);
        }
    }

    class OkAction extends AbstractAction {
        JDialog aboutBox;

        protected OkAction(JDialog aboutBox) {
            super("OkAction");
            this.aboutBox = aboutBox;
        }

        public void actionPerformed(ActionEvent e) {
            aboutBox.setVisible(false);
        }
    }

    class ChangeLookAndFeelAction extends AbstractAction {
        SwingSet2 swingset;
        LookAndFeelData lafData;
        protected ChangeLookAndFeelAction(SwingSet2 swingset, LookAndFeelData lafData) {
            super("ChangeTheme");
            this.swingset = swingset;
            this.lafData = lafData;
        }

        public void actionPerformed(ActionEvent e) {
            swingset.setLookAndFeel(lafData);
        }
    }

    class ActivatePopupMenuAction extends AbstractAction {
        SwingSet2 swingset;
        JPopupMenu popup;
        protected ActivatePopupMenuAction(SwingSet2 swingset, JPopupMenu popup) {
            super("ActivatePopupMenu");
            this.swingset = swingset;
            this.popup = popup;
        }

        public void actionPerformed(ActionEvent e) {
            Dimension invokerSize = getSize();
            Dimension popupSize = popup.getPreferredSize();
            popup.show(swingset, (invokerSize.width - popupSize.width) / 2,
                       (invokerSize.height - popupSize.height) / 2);
        }
    }

    // Turns on all possible auditory feedback
    class OnAudioAction extends AbstractAction {
        SwingSet2 swingset;
        protected OnAudioAction(SwingSet2 swingset) {
            super("Audio On");
            this.swingset = swingset;
        }
        public void actionPerformed(ActionEvent e) {
            UIManager.put("AuditoryCues.playList",
                          UIManager.get("AuditoryCues.allAuditoryCues"));
            swingset.updateLookAndFeel();
        }
    }

    // Turns on the default amount of auditory feedback
    class DefaultAudioAction extends AbstractAction {
        SwingSet2 swingset;
        protected DefaultAudioAction(SwingSet2 swingset) {
            super("Audio Default");
            this.swingset = swingset;
        }
        public void actionPerformed(ActionEvent e) {
            UIManager.put("AuditoryCues.playList",
                          UIManager.get("AuditoryCues.defaultCueList"));
            swingset.updateLookAndFeel();
        }
    }

    // Turns off all possible auditory feedback
    class OffAudioAction extends AbstractAction {
        SwingSet2 swingset;
        protected OffAudioAction(SwingSet2 swingset) {
            super("Audio Off");
            this.swingset = swingset;
        }
        public void actionPerformed(ActionEvent e) {
            UIManager.put("AuditoryCues.playList",
                          UIManager.get("AuditoryCues.noAuditoryCues"));
            swingset.updateLookAndFeel();
        }
    }

    // Turns on or off the tool tips for the demo.
    class ToolTipAction extends AbstractAction {
        protected ToolTipAction() {
            super("ToolTip Control");
        }

        public void actionPerformed(ActionEvent e) {
            boolean status = ((JCheckBoxMenuItem)e.getSource()).isSelected();
            ToolTipManager.sharedInstance().setEnabled(status);
        }
    }

    class DragSupportAction extends AbstractAction {
        protected DragSupportAction() {
            super("DragSupport Control");
        }

        public void actionPerformed(ActionEvent e) {
            boolean dragEnabled = ((JCheckBoxMenuItem)e.getSource()).isSelected();
            for (SwingSet2 ss : swingSets) {
                ss.setDragEnabled(dragEnabled);
            }
        }
    }

    class ChangeThemeAction extends AbstractAction {
        SwingSet2 swingset;
        MetalTheme theme;
        protected ChangeThemeAction(SwingSet2 swingset, MetalTheme theme) {
            super("ChangeTheme");
            this.swingset = swingset;
            this.theme = theme;
        }

        public void actionPerformed(ActionEvent e) {
            MetalLookAndFeel.setCurrentTheme(theme);
            swingset.updateLookAndFeel();
        }
    }

    class ExitAction extends AbstractAction {
        SwingSet2 swingset;
        protected ExitAction(SwingSet2 swingset) {
            super("ExitAction");
            this.swingset = swingset;
        }

        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }

    class AboutAction extends AbstractAction {
        SwingSet2 swingset;
        protected AboutAction(SwingSet2 swingset) {
            super("AboutAction");
            this.swingset = swingset;
        }

        public void actionPerformed(ActionEvent e) {
            if(aboutBox == null) {
                // JPanel panel = new JPanel(new BorderLayout());
                JPanel panel = new AboutPanel(swingset);
                panel.setLayout(new BorderLayout());

                aboutBox = new JDialog(swingset.getFrame(), getString("AboutBox.title"), false);
                aboutBox.setResizable(false);
                aboutBox.getContentPane().add(panel, BorderLayout.CENTER);

                // JButton button = new JButton(getString("AboutBox.ok_button_text"));
                JPanel buttonpanel = new JPanel();
                buttonpanel.setBorder(new javax.swing.border.EmptyBorder(0, 0, 3, 0));
                buttonpanel.setOpaque(false);
                JButton button = (JButton) buttonpanel.add(
                    new JButton(getString("AboutBox.ok_button_text"))
                );
                panel.add(buttonpanel, BorderLayout.SOUTH);

                button.addActionListener(new OkAction(aboutBox));
            }
            aboutBox.pack();
            aboutBox.setLocationRelativeTo(getFrame());
            aboutBox.show();
        }
    }

    class MultiScreenAction extends AbstractAction {
        static final int ALL_SCREENS = -1;
        int screen;
        protected MultiScreenAction(SwingSet2 swingset, int screen) {
            super("MultiScreenAction");
            this.screen = screen;
        }

        public void actionPerformed(ActionEvent e) {
            GraphicsDevice[] gds = GraphicsEnvironment.
                                   getLocalGraphicsEnvironment().
                                   getScreenDevices();
            if (screen == ALL_SCREENS) {
                for (int i = 0; i < gds.length; i++) {
                    SwingSet2 swingset = new SwingSet2(
                                  gds[i].getDefaultConfiguration());
                    swingset.setDragEnabled(dragEnabled);
                }
            }
            else {
                SwingSet2 swingset = new SwingSet2(
                             gds[screen].getDefaultConfiguration());
                swingset.setDragEnabled(dragEnabled);
            }
        }
    }

    // *******************************************************
    // **********************  Misc  *************************
    // *******************************************************

    class AboutPanel extends JPanel {
        ImageIcon aboutimage = null;
        SwingSet2 swingset = null;

        public AboutPanel(SwingSet2 swingset) {
            this.swingset = swingset;
            aboutimage = swingset.createImageIcon("About.jpg", "AboutBox.accessible_description");
            setOpaque(false);
        }

        public void paint(Graphics g) {
            aboutimage.paintIcon(this, g, 0, 0);
            super.paint(g);
        }

        public Dimension getPreferredSize() {
            return new Dimension(aboutimage.getIconWidth(),
                                 aboutimage.getIconHeight());
        }
    }

    private class ChangeFontAction extends AbstractAction {
        private SwingSet2 swingset;
        private boolean plain;

        ChangeFontAction(SwingSet2 swingset, boolean plain) {
            super("FontMenu");
            this.swingset = swingset;
            this.plain = plain;
        }

        public void actionPerformed(ActionEvent e) {
            if (plain) {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
            }
            else {
                UIManager.put("swing.boldMetal", Boolean.TRUE);
            }
            // Change the look and feel to force the settings to take effect.
            updateLookAndFeel();
        }
    }

    private static LookAndFeelData[] getInstalledLookAndFeelData() {
        return Arrays.stream(UIManager.getInstalledLookAndFeels())
                .map(laf -> getLookAndFeelData(laf))
                .toArray(LookAndFeelData[]::new);
    }

    private static LookAndFeelData getLookAndFeelData(
            UIManager.LookAndFeelInfo info) {
        switch (info.getName()) {
            case "Metal":
                return new LookAndFeelData(info, "java");
            case "Nimbus":
                return new LookAndFeelData(info, "nimbus");
            case "Windows":
                return new LookAndFeelData(info, "windows");
            case "GTK+":
                return new LookAndFeelData(info, "gtk");
            case "CDE/Motif":
                return new LookAndFeelData(info, "motif");
            case "Mac OS X":
                return new LookAndFeelData(info, "mac");
            default:
                return new LookAndFeelData(info);
        }
    }

    private static class LookAndFeelData {
        String name;
        String className;
        String label;
        char mnemonic;
        String accDescription;

        public LookAndFeelData(UIManager.LookAndFeelInfo info) {
            this(info.getName(), info.getClassName(), info.getName(),
                 info.getName(), info.getName());
        }

        public LookAndFeelData(UIManager.LookAndFeelInfo info, String property) {
            this(info.getName(), info.getClassName(),
                    getString(String.format("LafMenu.%s_label", property)),
                    getString(String.format("LafMenu.%s_mnemonic", property)),
                    getString(String.format("LafMenu.%s_accessible_description",
                                    property)));
        }

        public LookAndFeelData(String name, String className, String label,
                               String mnemonic, String accDescription) {
            this.name = name;
            this.className = className;
            this.label = label;
            this.mnemonic = mnemonic.charAt(0);
            this.accDescription = accDescription;
        }

        @Override
        public String toString() {
            return className;
        }
    }
}
