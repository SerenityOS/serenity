/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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



import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.undo.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.UIManager.LookAndFeelInfo;


/**
 * Sample application using the simple text editor component that
 * supports only one font.
 *
 * @author  Timothy Prinzing
 */
@SuppressWarnings("serial")
public class Notepad extends JPanel {

    protected static Properties properties;
    private static ResourceBundle resources;
    private static final String EXIT_AFTER_PAINT = "-exit";
    private static boolean exitAfterFirstPaint;

    private static final String[] MENUBAR_KEYS = {"file", "edit", "debug"};
    private static final String[] TOOLBAR_KEYS = {"new", "open", "save", "-", "cut", "copy", "paste"};
    private static final String[] FILE_KEYS = {"new", "open", "save", "-", "exit"};
    private static final String[] EDIT_KEYS = {"cut", "copy", "paste", "-", "undo", "redo"};
    private static final String[] DEBUG_KEYS = {"dump", "showElementTree"};

    static {
        try {
            properties = new Properties();
            properties.load(Notepad.class.getResourceAsStream(
                    "resources/NotepadSystem.properties"));
            resources = ResourceBundle.getBundle("resources.Notepad",
                    Locale.getDefault());
        } catch (MissingResourceException | IOException  e) {
            System.err.println("resources/Notepad.properties "
                    + "or resources/NotepadSystem.properties not found");
            System.exit(1);
        }
    }

    @Override
    public void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (exitAfterFirstPaint) {
            System.exit(0);
        }
    }

    @SuppressWarnings("OverridableMethodCallInConstructor")
    Notepad() {
        super(true);

        // Trying to set Nimbus look and feel
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new BorderLayout());

        // create the embedded JTextComponent
        editor = createEditor();
        // Add this as a listener for undoable edits.
        editor.getDocument().addUndoableEditListener(undoHandler);

        // install the command table
        commands = new HashMap<Object, Action>();
        Action[] actions = getActions();
        for (Action a : actions) {
            commands.put(a.getValue(Action.NAME), a);
        }

        JScrollPane scroller = new JScrollPane();
        JViewport port = scroller.getViewport();
        port.add(editor);

        String vpFlag = getProperty("ViewportBackingStore");
        if (vpFlag != null) {
            Boolean bs = Boolean.valueOf(vpFlag);
            port.setScrollMode(bs
                    ? JViewport.BACKINGSTORE_SCROLL_MODE
                    : JViewport.BLIT_SCROLL_MODE);
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add("North", createToolbar());
        panel.add("Center", scroller);
        add("Center", panel);
        add("South", createStatusbar());
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals(EXIT_AFTER_PAINT)) {
            exitAfterFirstPaint = true;
        }
        SwingUtilities.invokeAndWait(new Runnable() {

            public void run() {
                JFrame frame = new JFrame();
                frame.setTitle(resources.getString("Title"));
                frame.setBackground(Color.lightGray);
                frame.getContentPane().setLayout(new BorderLayout());
                Notepad notepad = new Notepad();
                frame.getContentPane().add("Center", notepad);
                frame.setJMenuBar(notepad.createMenubar());
                frame.addWindowListener(new AppCloser());
                frame.pack();
                frame.setSize(500, 600);
                frame.setVisible(true);
            }
        });
    }

    /**
     * Fetch the list of actions supported by this
     * editor.  It is implemented to return the list
     * of actions supported by the embedded JTextComponent
     * augmented with the actions defined locally.
     */
    public Action[] getActions() {
        return TextAction.augmentList(editor.getActions(), defaultActions);
    }

    /**
     * Create an editor to represent the given document.
     */
    protected JTextComponent createEditor() {
        JTextComponent c = new JTextArea();
        c.setDragEnabled(true);
        c.setFont(new Font("monospaced", Font.PLAIN, 12));
        return c;
    }

    /**
     * Fetch the editor contained in this panel
     */
    protected JTextComponent getEditor() {
        return editor;
    }


    /**
     * To shutdown when run as an application.  This is a
     * fairly lame implementation.   A more self-respecting
     * implementation would at least check to see if a save
     * was needed.
     */
    protected static final class AppCloser extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
    }

    /**
     * Find the hosting frame, for the file-chooser dialog.
     */
    protected Frame getFrame() {
        for (Container p = getParent(); p != null; p = p.getParent()) {
            if (p instanceof Frame) {
                return (Frame) p;
            }
        }
        return null;
    }

    /**
     * This is the hook through which all menu items are
     * created.
     */
    protected JMenuItem createMenuItem(String cmd) {
        JMenuItem mi = new JMenuItem(getResourceString(cmd + labelSuffix));
        URL url = getResource(cmd + imageSuffix);
        if (url != null) {
            mi.setHorizontalTextPosition(JButton.RIGHT);
            mi.setIcon(new ImageIcon(url));
        }
        String astr = getProperty(cmd + actionSuffix);
        if (astr == null) {
            astr = cmd;
        }
        mi.setActionCommand(astr);
        Action a = getAction(astr);
        if (a != null) {
            mi.addActionListener(a);
            a.addPropertyChangeListener(createActionChangeListener(mi));
            mi.setEnabled(a.isEnabled());
        } else {
            mi.setEnabled(false);
        }
        return mi;
    }

    protected Action getAction(String cmd) {
        return commands.get(cmd);
    }

    protected String getProperty(String key) {
        return properties.getProperty(key);
    }

    protected String getResourceString(String nm) {
        String str;
        try {
            str = resources.getString(nm);
        } catch (MissingResourceException mre) {
            str = null;
        }
        return str;
    }

    protected URL getResource(String key) {
        String name = getResourceString(key);
        if (name != null) {
            return this.getClass().getResource(name);
        }
        return null;
    }

    /**
     * Create a status bar
     */
    protected Component createStatusbar() {
        // need to do something reasonable here
        status = new StatusBar();
        return status;
    }

    /**
     * Resets the undo manager.
     */
    protected void resetUndoManager() {
        undo.discardAllEdits();
        undoAction.update();
        redoAction.update();
    }

    /**
     * Create the toolbar.  By default this reads the
     * resource file for the definition of the toolbar.
     */
    private Component createToolbar() {
        toolbar = new JToolBar();
        for (String toolKey: getToolBarKeys()) {
            if (toolKey.equals("-")) {
                toolbar.add(Box.createHorizontalStrut(5));
            } else {
                toolbar.add(createTool(toolKey));
            }
        }
        toolbar.add(Box.createHorizontalGlue());
        return toolbar;
    }

    /**
     * Hook through which every toolbar item is created.
     */
    protected Component createTool(String key) {
        return createToolbarButton(key);
    }

    /**
     * Create a button to go inside of the toolbar.  By default this
     * will load an image resource.  The image filename is relative to
     * the classpath (including the '.' directory if its a part of the
     * classpath), and may either be in a JAR file or a separate file.
     *
     * @param key The key in the resource file to serve as the basis
     *  of lookups.
     */
    protected JButton createToolbarButton(String key) {
        URL url = getResource(key + imageSuffix);
        JButton b = new JButton(new ImageIcon(url)) {

            @Override
            public float getAlignmentY() {
                return 0.5f;
            }
        };
        b.setRequestFocusEnabled(false);
        b.setMargin(new Insets(1, 1, 1, 1));

        String astr = getProperty(key + actionSuffix);
        if (astr == null) {
            astr = key;
        }
        Action a = getAction(astr);
        if (a != null) {
            b.setActionCommand(astr);
            b.addActionListener(a);
        } else {
            b.setEnabled(false);
        }

        String tip = getResourceString(key + tipSuffix);
        if (tip != null) {
            b.setToolTipText(tip);
        }

        return b;
    }

    /**
     * Create the menubar for the app.  By default this pulls the
     * definition of the menu from the associated resource file.
     */
    protected JMenuBar createMenubar() {
        JMenuBar mb = new JMenuBar();
        for(String menuKey: getMenuBarKeys()){
            JMenu m = createMenu(menuKey);
            if (m != null) {
                mb.add(m);
            }
        }
        return mb;
    }

    /**
     * Create a menu for the app.  By default this pulls the
     * definition of the menu from the associated resource file.
     */
    protected JMenu createMenu(String key) {
        JMenu menu = new JMenu(getResourceString(key + labelSuffix));
        for (String itemKey: getItemKeys(key)) {
            if (itemKey.equals("-")) {
                menu.addSeparator();
            } else {
                JMenuItem mi = createMenuItem(itemKey);
                menu.add(mi);
            }
        }
        return menu;
    }

    /**
     *  Get keys for menus
     */
    protected String[] getItemKeys(String key) {
        switch (key) {
            case "file":
                return FILE_KEYS;
            case "edit":
                return EDIT_KEYS;
            case "debug":
                return DEBUG_KEYS;
            default:
                return null;
        }
    }

    protected String[] getMenuBarKeys() {
        return MENUBAR_KEYS;
    }

    protected String[] getToolBarKeys() {
        return TOOLBAR_KEYS;
    }

    // Yarked from JMenu, ideally this would be public.
    protected PropertyChangeListener createActionChangeListener(JMenuItem b) {
        return new ActionChangedListener(b);
    }

    // Yarked from JMenu, ideally this would be public.

    private class ActionChangedListener implements PropertyChangeListener {

        JMenuItem menuItem;

        ActionChangedListener(JMenuItem mi) {
            super();
            this.menuItem = mi;
        }

        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (e.getPropertyName().equals(Action.NAME)) {
                String text = (String) e.getNewValue();
                menuItem.setText(text);
            } else if (propertyName.equals("enabled")) {
                Boolean enabledState = (Boolean) e.getNewValue();
                menuItem.setEnabled(enabledState.booleanValue());
            }
        }
    }
    private JTextComponent editor;
    private Map<Object, Action> commands;
    private JToolBar toolbar;
    private JComponent status;
    private JFrame elementTreeFrame;
    protected ElementTreePanel elementTreePanel;

    /**
     * Listener for the edits on the current document.
     */
    protected UndoableEditListener undoHandler = new UndoHandler();
    /** UndoManager that we add edits to. */
    protected UndoManager undo = new UndoManager();
    /**
     * Suffix applied to the key used in resource file
     * lookups for an image.
     */
    public static final String imageSuffix = "Image";
    /**
     * Suffix applied to the key used in resource file
     * lookups for a label.
     */
    public static final String labelSuffix = "Label";
    /**
     * Suffix applied to the key used in resource file
     * lookups for an action.
     */
    public static final String actionSuffix = "Action";
    /**
     * Suffix applied to the key used in resource file
     * lookups for tooltip text.
     */
    public static final String tipSuffix = "Tooltip";
    public static final String openAction = "open";
    public static final String newAction = "new";
    public static final String saveAction = "save";
    public static final String exitAction = "exit";
    public static final String showElementTreeAction = "showElementTree";


    class UndoHandler implements UndoableEditListener {

        /**
         * Messaged when the Document has created an edit, the edit is
         * added to <code>undo</code>, an instance of UndoManager.
         */
        public void undoableEditHappened(UndoableEditEvent e) {
            undo.addEdit(e.getEdit());
            undoAction.update();
            redoAction.update();
        }
    }


    /**
     * FIXME - I'm not very useful yet
     */
    class StatusBar extends JComponent {

        public StatusBar() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
        }
    }
    // --- action implementations -----------------------------------
    private UndoAction undoAction = new UndoAction();
    private RedoAction redoAction = new RedoAction();
    /**
     * Actions defined by the Notepad class
     */
    private Action[] defaultActions = {
        new NewAction(),
        new OpenAction(),
        new SaveAction(),
        new ExitAction(),
        new ShowElementTreeAction(),
        undoAction,
        redoAction
    };


    class UndoAction extends AbstractAction {

        public UndoAction() {
            super("Undo");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                undo.undo();
            } catch (CannotUndoException ex) {
                Logger.getLogger(UndoAction.class.getName()).log(Level.SEVERE,
                        "Unable to undo", ex);
            }
            update();
            redoAction.update();
        }

        protected void update() {
            if (undo.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }


    class RedoAction extends AbstractAction {

        public RedoAction() {
            super("Redo");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                undo.redo();
            } catch (CannotRedoException ex) {
                Logger.getLogger(RedoAction.class.getName()).log(Level.SEVERE,
                        "Unable to redo", ex);
            }
            update();
            undoAction.update();
        }

        protected void update() {
            if (undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }


    class OpenAction extends NewAction {

        OpenAction() {
            super(openAction);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Frame frame = getFrame();
            JFileChooser chooser = new JFileChooser();
            int ret = chooser.showOpenDialog(frame);

            if (ret != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File f = chooser.getSelectedFile();
            if (f.isFile() && f.canRead()) {
                Document oldDoc = getEditor().getDocument();
                if (oldDoc != null) {
                    oldDoc.removeUndoableEditListener(undoHandler);
                }
                if (elementTreePanel != null) {
                    elementTreePanel.setEditor(null);
                }
                getEditor().setDocument(new PlainDocument());
                frame.setTitle(f.getName());
                Thread loader = new FileLoader(f, editor.getDocument());
                loader.start();
            } else {
                JOptionPane.showMessageDialog(getFrame(),
                        "Could not open file: " + f,
                        "Error opening file",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    class SaveAction extends AbstractAction {

        SaveAction() {
            super(saveAction);
        }

        public void actionPerformed(ActionEvent e) {
            Frame frame = getFrame();
            JFileChooser chooser = new JFileChooser();
            int ret = chooser.showSaveDialog(frame);

            if (ret != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File f = chooser.getSelectedFile();
            frame.setTitle(f.getName());
            Thread saver = new FileSaver(f, editor.getDocument());
            saver.start();
        }
    }


    class NewAction extends AbstractAction {

        NewAction() {
            super(newAction);
        }

        NewAction(String nm) {
            super(nm);
        }

        public void actionPerformed(ActionEvent e) {
            Document oldDoc = getEditor().getDocument();
            if (oldDoc != null) {
                oldDoc.removeUndoableEditListener(undoHandler);
            }
            getEditor().setDocument(new PlainDocument());
            getEditor().getDocument().addUndoableEditListener(undoHandler);
            resetUndoManager();
            getFrame().setTitle(resources.getString("Title"));
            revalidate();
        }
    }


    /**
     * Really lame implementation of an exit command
     */
    class ExitAction extends AbstractAction {

        ExitAction() {
            super(exitAction);
        }

        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }


    /**
     * Action that brings up a JFrame with a JTree showing the structure
     * of the document.
     */
    class ShowElementTreeAction extends AbstractAction {

        ShowElementTreeAction() {
            super(showElementTreeAction);
        }

        public void actionPerformed(ActionEvent e) {
            if (elementTreeFrame == null) {
                // Create a frame containing an instance of
                // ElementTreePanel.
                try {
                    String title = resources.getString("ElementTreeFrameTitle");
                    elementTreeFrame = new JFrame(title);
                } catch (MissingResourceException mre) {
                    elementTreeFrame = new JFrame();
                }

                elementTreeFrame.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent weeee) {
                        elementTreeFrame.setVisible(false);
                    }
                });
                Container fContentPane = elementTreeFrame.getContentPane();

                fContentPane.setLayout(new BorderLayout());
                elementTreePanel = new ElementTreePanel(getEditor());
                fContentPane.add(elementTreePanel);
                elementTreeFrame.pack();
            }
            elementTreeFrame.setVisible(true);
        }
    }


    /**
     * Thread to load a file into the text storage model
     */
    class FileLoader extends Thread {

        FileLoader(File f, Document doc) {
            setPriority(4);
            this.f = f;
            this.doc = doc;
        }

        @Override
        public void run() {
            try {
                // initialize the statusbar
                status.removeAll();
                JProgressBar progress = new JProgressBar();
                progress.setMinimum(0);
                progress.setMaximum((int) f.length());
                status.add(progress);
                status.revalidate();

                // try to start reading
                Reader in = new FileReader(f);
                char[] buff = new char[4096];
                int nch;
                while ((nch = in.read(buff, 0, buff.length)) != -1) {
                    doc.insertString(doc.getLength(), new String(buff, 0, nch),
                            null);
                    progress.setValue(progress.getValue() + nch);
                }
            } catch (IOException e) {
                final String msg = e.getMessage();
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        JOptionPane.showMessageDialog(getFrame(),
                                "Could not open file: " + msg,
                                "Error opening file",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (BadLocationException e) {
                System.err.println(e.getMessage());
            }
            doc.addUndoableEditListener(undoHandler);
            // we are done... get rid of progressbar
            status.removeAll();
            status.revalidate();

            resetUndoManager();

            if (elementTreePanel != null) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        elementTreePanel.setEditor(getEditor());
                    }
                });
            }
        }
        Document doc;
        File f;
    }


    /**
     * Thread to save a document to file
     */
    class FileSaver extends Thread {

        Document doc;
        File f;

        FileSaver(File f, Document doc) {
            setPriority(4);
            this.f = f;
            this.doc = doc;
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            try {
                // initialize the statusbar
                status.removeAll();
                JProgressBar progress = new JProgressBar();
                progress.setMinimum(0);
                progress.setMaximum(doc.getLength());
                status.add(progress);
                status.revalidate();

                // start writing
                Writer out = new FileWriter(f);
                Segment text = new Segment();
                text.setPartialReturn(true);
                int charsLeft = doc.getLength();
                int offset = 0;
                while (charsLeft > 0) {
                    doc.getText(offset, Math.min(4096, charsLeft), text);
                    out.write(text.array, text.offset, text.count);
                    charsLeft -= text.count;
                    offset += text.count;
                    progress.setValue(offset);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Logger.getLogger(FileSaver.class.getName()).log(
                                Level.SEVERE,
                                null, e);
                    }
                }
                out.flush();
                out.close();
            } catch (IOException e) {
                final String msg = e.getMessage();
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        JOptionPane.showMessageDialog(getFrame(),
                                "Could not save file: " + msg,
                                "Error saving file",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (BadLocationException e) {
                System.err.println(e.getMessage());
            }
            // we are done... get rid of progressbar
            status.removeAll();
            status.revalidate();
        }
    }
}
