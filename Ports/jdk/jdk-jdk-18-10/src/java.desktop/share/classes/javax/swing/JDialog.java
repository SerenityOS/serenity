/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.awt.*;
import java.awt.event.*;
import java.beans.JavaBean;
import java.beans.BeanProperty;
import javax.accessibility.*;

/**
 * The main class for creating a dialog window. You can use this class
 * to create a custom dialog, or invoke the many class methods
 * in {@link JOptionPane} to create a variety of standard dialogs.
 * For information about creating dialogs, see
 * <em>The Java Tutorial</em> section
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/dialog.html">How
 * to Make Dialogs</a>.
 *
 * <p>
 *
 * The {@code JDialog} component contains a {@code JRootPane}
 * as its only child.
 * The {@code contentPane} should be the parent of any children of the
 * {@code JDialog}.
 * As a convenience, the {@code add}, {@code remove}, and {@code setLayout}
 * methods of this class are overridden, so that they delegate calls
 * to the corresponding methods of the {@code ContentPane}.
 * For example, you can add a child component to a dialog as follows:
 * <pre>
 *       dialog.add(child);
 * </pre>
 * And the child will be added to the contentPane.
 * The {@code contentPane} is always non-{@code null}.
 * Attempting to set it to {@code null} generates an exception.
 * The default {@code contentPane} has a {@code BorderLayout}
 * manager set on it.
 * Refer to {@link javax.swing.RootPaneContainer}
 * for details on adding, removing and setting the {@code LayoutManager}
 * of a {@code JDialog}.
 * <p>
 * Please see the {@code JRootPane} documentation for a complete
 * description of the {@code contentPane}, {@code glassPane},
 * and {@code layeredPane} components.
 * <p>
 * In a multi-screen environment, you can create a {@code JDialog}
 * on a different screen device than its owner.  See {@link java.awt.Frame} for
 * more information.
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the {@code java.beans} package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see JOptionPane
 * @see JRootPane
 * @see javax.swing.RootPaneContainer
 *
 * @author David Kloba
 * @author James Gosling
 * @author Scott Violet
 * @since 1.2
 */
@JavaBean(defaultProperty = "JMenuBar", description = "A toplevel window for creating dialog boxes.")
@SwingContainer(delegate = "getContentPane")
@SuppressWarnings("serial") // Same-version serialization only
public class JDialog extends Dialog implements WindowConstants,
                                               Accessible,
                                               RootPaneContainer,
                               TransferHandler.HasGetTransferHandler
{
    /**
     * Key into the AppContext, used to check if should provide decorations
     * by default.
     */
    private static final Object defaultLookAndFeelDecoratedKey =
            new StringBuffer("JDialog.defaultLookAndFeelDecorated");

    private int defaultCloseOperation = HIDE_ON_CLOSE;

    /**
     * @see #getRootPane
     * @see #setRootPane
     */
    protected JRootPane rootPane;

    /**
     * If true then calls to {@code add} and {@code setLayout}
     * will be forwarded to the {@code contentPane}. This is initially
     * false, but is set to true when the {@code JDialog} is constructed.
     *
     * @see #isRootPaneCheckingEnabled
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    protected boolean rootPaneCheckingEnabled = false;

    /**
     * The {@code TransferHandler} for this dialog.
     */
    private TransferHandler transferHandler;

    /**
     * Creates a modeless dialog without a title and without a specified
     * {@code Frame} owner.  A shared, hidden frame will be
     * set as the owner of the dialog.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     * <p>
     * NOTE: This constructor does not allow you to create an unowned
     * {@code JDialog}. To create an unowned {@code JDialog}
     * you must use either the {@code JDialog(Window)} or
     * {@code JDialog(Dialog)} constructor with an argument of
     * {@code null}.
     *
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    public JDialog() {
        this((Frame)null, false);
    }

    /**
     * Creates a modeless dialog with the specified {@code Frame}
     * as its owner and an empty title. If {@code owner}
     * is {@code null}, a shared, hidden frame will be set as the
     * owner of the dialog.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     * <p>
     * NOTE: This constructor does not allow you to create an unowned
     * {@code JDialog}. To create an unowned {@code JDialog}
     * you must use either the {@code JDialog(Window)} or
     * {@code JDialog(Dialog)} constructor with an argument of
     * {@code null}.
     *
     * @param owner the {@code Frame} from which the dialog is displayed
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    public JDialog(Frame owner) {
        this(owner, false);
    }

    /**
     * Creates a dialog with an empty title and the specified modality and
     * {@code Frame} as its owner. If {@code owner} is {@code null},
     * a shared, hidden frame will be set as the owner of the dialog.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     * <p>
     * NOTE: This constructor does not allow you to create an unowned
     * {@code JDialog}. To create an unowned {@code JDialog}
     * you must use either the {@code JDialog(Window)} or
     * {@code JDialog(Dialog)} constructor with an argument of
     * {@code null}.
     *
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param modal specifies whether dialog blocks user input to other top-level
     *     windows when shown. If {@code true}, the modality type property is set to
     *     {@code DEFAULT_MODALITY_TYPE}, otherwise the dialog is modeless.
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    public JDialog(Frame owner, boolean modal) {
        this(owner, "", modal);
    }

    /**
     * Creates a modeless dialog with the specified title and
     * with the specified owner frame.  If {@code owner}
     * is {@code null}, a shared, hidden frame will be set as the
     * owner of the dialog.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     * <p>
     * NOTE: This constructor does not allow you to create an unowned
     * {@code JDialog}. To create an unowned {@code JDialog}
     * you must use either the {@code JDialog(Window)} or
     * {@code JDialog(Dialog)} constructor with an argument of
     * {@code null}.
     *
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param title  the {@code String} to display in the dialog's
     *                  title bar
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    public JDialog(Frame owner, String title) {
        this(owner, title, false);
    }

    /**
     * Creates a dialog with the specified title, owner {@code Frame}
     * and modality. If {@code owner} is {@code null},
     * a shared, hidden frame will be set as the owner of this dialog.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     * <p>
     * NOTE: Any popup components ({@code JComboBox},
     * {@code JPopupMenu}, {@code JMenuBar})
     * created within a modal dialog will be forced to be lightweight.
     * <p>
     * NOTE: This constructor does not allow you to create an unowned
     * {@code JDialog}. To create an unowned {@code JDialog}
     * you must use either the {@code JDialog(Window)} or
     * {@code JDialog(Dialog)} constructor with an argument of
     * {@code null}.
     *
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param title  the {@code String} to display in the dialog's
     *     title bar
     * @param modal specifies whether dialog blocks user input to other top-level
     *     windows when shown. If {@code true}, the modality type property is set to
     *     {@code DEFAULT_MODALITY_TYPE} otherwise the dialog is modeless
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     *
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog.ModalityType#MODELESS
     * @see java.awt.Dialog#DEFAULT_MODALITY_TYPE
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    public JDialog(Frame owner, String title, boolean modal) {
        super(owner == null? SwingUtilities.getSharedOwnerFrame() : owner,
              title, modal);
        if (owner == null) {
            WindowListener ownerShutdownListener =
                    SwingUtilities.getSharedOwnerFrameShutdownListener();
            addWindowListener(ownerShutdownListener);
        }
        dialogInit();
    }

    /**
     * Creates a dialog with the specified title,
     * owner {@code Frame}, modality and {@code GraphicsConfiguration}.
     * If {@code owner} is {@code null},
     * a shared, hidden frame will be set as the owner of this dialog.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     * <p>
     * NOTE: Any popup components ({@code JComboBox},
     * {@code JPopupMenu}, {@code JMenuBar})
     * created within a modal dialog will be forced to be lightweight.
     * <p>
     * NOTE: This constructor does not allow you to create an unowned
     * {@code JDialog}. To create an unowned {@code JDialog}
     * you must use either the {@code JDialog(Window)} or
     * {@code JDialog(Dialog)} constructor with an argument of
     * {@code null}.
     *
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param title  the {@code String} to display in the dialog's
     *     title bar
     * @param modal specifies whether dialog blocks user input to other top-level
     *     windows when shown. If {@code true}, the modality type property is set to
     *     {@code DEFAULT_MODALITY_TYPE}, otherwise the dialog is modeless.
     * @param gc the {@code GraphicsConfiguration} of the target screen device;
     *     if {@code null}, the default system {@code GraphicsConfiguration}
     *     is assumed
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog.ModalityType#MODELESS
     * @see java.awt.Dialog#DEFAULT_MODALITY_TYPE
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     * @since 1.4
     */
    public JDialog(Frame owner, String title, boolean modal,
                   GraphicsConfiguration gc) {
        super(owner == null? SwingUtilities.getSharedOwnerFrame() : owner,
              title, modal, gc);
        if (owner == null) {
            WindowListener ownerShutdownListener =
                    SwingUtilities.getSharedOwnerFrameShutdownListener();
            addWindowListener(ownerShutdownListener);
        }
        dialogInit();
    }

    /**
     * Creates a modeless dialog with the specified {@code Dialog}
     * as its owner and an empty title.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the owner {@code Dialog} from which the dialog is displayed
     *     or {@code null} if this dialog has no owner
     * @throws HeadlessException {@code if GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    public JDialog(Dialog owner) {
        this(owner, false);
    }

    /**
     * Creates a dialog with an empty title and the specified modality and
     * {@code Dialog} as its owner.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the owner {@code Dialog} from which the dialog is displayed
     *     or {@code null} if this dialog has no owner
     * @param modal specifies whether dialog blocks user input to other top-level
     *     windows when shown. If {@code true}, the modality type property is set to
     *     {@code DEFAULT_MODALITY_TYPE}, otherwise the dialog is modeless.
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog.ModalityType#MODELESS
     * @see java.awt.Dialog#DEFAULT_MODALITY_TYPE
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    public JDialog(Dialog owner, boolean modal) {
        this(owner, "", modal);
    }

    /**
     * Creates a modeless dialog with the specified title and
     * with the specified owner dialog.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the owner {@code Dialog} from which the dialog is displayed
     *     or {@code null} if this dialog has no owner
     * @param title  the {@code String} to display in the dialog's
     *                  title bar
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    public JDialog(Dialog owner, String title) {
        this(owner, title, false);
    }

    /**
     * Creates a dialog with the specified title, modality
     * and the specified owner {@code Dialog}.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the owner {@code Dialog} from which the dialog is displayed
     *     or {@code null} if this dialog has no owner
     * @param title  the {@code String} to display in the dialog's
     *     title bar
     * @param modal specifies whether dialog blocks user input to other top-level
     *     windows when shown. If {@code true}, the modality type property is set to
     *     {@code DEFAULT_MODALITY_TYPE}, otherwise the dialog is modeless
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog.ModalityType#MODELESS
     * @see java.awt.Dialog#DEFAULT_MODALITY_TYPE
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    public JDialog(Dialog owner, String title, boolean modal) {
        super(owner, title, modal);
        dialogInit();
    }

    /**
     * Creates a dialog with the specified title, owner {@code Dialog},
     * modality and {@code GraphicsConfiguration}.
     *
     * <p>
     * NOTE: Any popup components ({@code JComboBox},
     * {@code JPopupMenu}, {@code JMenuBar})
     * created within a modal dialog will be forced to be lightweight.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the owner {@code Dialog} from which the dialog is displayed
     *     or {@code null} if this dialog has no owner
     * @param title  the {@code String} to display in the dialog's
     *     title bar
     * @param modal specifies whether dialog blocks user input to other top-level
     *     windows when shown. If {@code true}, the modality type property is set to
     *     {@code DEFAULT_MODALITY_TYPE}, otherwise the dialog is modeless
     * @param gc the {@code GraphicsConfiguration} of the target screen device;
     *     if {@code null}, the default system {@code GraphicsConfiguration}
     *     is assumed
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *     returns {@code true}.
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog.ModalityType#MODELESS
     * @see java.awt.Dialog#DEFAULT_MODALITY_TYPE
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     * @since 1.4
     */
    public JDialog(Dialog owner, String title, boolean modal,
                   GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        dialogInit();
    }

    /**
     * Creates a modeless dialog with the specified {@code Window}
     * as its owner and an empty title.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the {@code Window} from which the dialog is displayed or
     *     {@code null} if this dialog has no owner
     *
     * @throws IllegalArgumentException
     *     if the {@code owner} is not an instance of {@link java.awt.Dialog Dialog}
     *     or {@link java.awt.Frame Frame}
     * @throws IllegalArgumentException
     *     if the {@code owner}'s {@code GraphicsConfiguration} is not from a screen device
     * @throws HeadlessException
     *     when {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     *
     * @since 1.6
     */
    public JDialog(Window owner) {
        this(owner, Dialog.ModalityType.MODELESS);
    }

    /**
     * Creates a dialog with an empty title and the specified modality and
     * {@code Window} as its owner.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the {@code Window} from which the dialog is displayed or
     *     {@code null} if this dialog has no owner
     * @param modalityType specifies whether dialog blocks input to other
     *     windows when shown. {@code null} value and unsupported modality
     *     types are equivalent to {@code MODELESS}
     *
     * @throws IllegalArgumentException
     *     if the {@code owner} is not an instance of {@link java.awt.Dialog Dialog}
     *     or {@link java.awt.Frame Frame}
     * @throws IllegalArgumentException
     *     if the {@code owner}'s {@code GraphicsConfiguration} is not from a screen device
     * @throws HeadlessException
     *     when {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @throws SecurityException
     *     if the calling thread does not have permission to create modal dialogs
     *     with the given {@code modalityType}
     *
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     *
     * @since 1.6
     */
    public JDialog(Window owner, ModalityType modalityType) {
        this(owner, "", modalityType);
    }

    /**
     * Creates a modeless dialog with the specified title and owner
     * {@code Window}.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the {@code Window} from which the dialog is displayed or
     *     {@code null} if this dialog has no owner
     * @param title the {@code String} to display in the dialog's
     *     title bar or {@code null} if the dialog has no title
     *
     * @throws IllegalArgumentException
     *     if the {@code owner} is not an instance of {@link java.awt.Dialog Dialog}
     *     or {@link java.awt.Frame Frame}
     * @throws IllegalArgumentException
     *     if the {@code owner}'s {@code GraphicsConfiguration} is not from a screen device
     * @throws HeadlessException
     *     when {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     *
     * @since 1.6
     */
    public JDialog(Window owner, String title) {
        this(owner, title, Dialog.ModalityType.MODELESS);
    }

    /**
     * Creates a dialog with the specified title, owner {@code Window} and
     * modality.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the {@code Window} from which the dialog is displayed or
     *     {@code null} if this dialog has no owner
     * @param title the {@code String} to display in the dialog's
     *     title bar or {@code null} if the dialog has no title
     * @param modalityType specifies whether dialog blocks input to other
     *     windows when shown. {@code null} value and unsupported modality
     *     types are equivalent to {@code MODELESS}
     *
     * @throws IllegalArgumentException
     *     if the {@code owner} is not an instance of {@link java.awt.Dialog Dialog}
     *     or {@link java.awt.Frame Frame}
     * @throws IllegalArgumentException
     *     if the {@code owner}'s {@code GraphicsConfiguration} is not from a screen device
     * @throws HeadlessException
     *     when {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @throws SecurityException
     *     if the calling thread does not have permission to create modal dialogs
     *     with the given {@code modalityType}
     *
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     *
     * @since 1.6
     */
    public JDialog(Window owner, String title, Dialog.ModalityType modalityType) {
        super(owner, title, modalityType);
        dialogInit();
    }

    /**
     * Creates a dialog with the specified title, owner {@code Window},
     * modality and {@code GraphicsConfiguration}.
     * <p>
     * NOTE: Any popup components ({@code JComboBox},
     * {@code JPopupMenu}, {@code JMenuBar})
     * created within a modal dialog will be forced to be lightweight.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the {@code Window} from which the dialog is displayed or
     *     {@code null} if this dialog has no owner
     * @param title the {@code String} to display in the dialog's
     *     title bar or {@code null} if the dialog has no title
     * @param modalityType specifies whether dialog blocks input to other
     *     windows when shown. {@code null} value and unsupported modality
     *     types are equivalent to {@code MODELESS}
     * @param gc the {@code GraphicsConfiguration} of the target screen device;
     *     if {@code null}, the default system {@code GraphicsConfiguration}
     *     is assumed
     * @throws IllegalArgumentException
     *     if the {@code owner} is not an instance of {@link java.awt.Dialog Dialog}
     *     or {@link java.awt.Frame Frame}
     * @throws IllegalArgumentException
     *     if the {@code owner}'s {@code GraphicsConfiguration} is not from a screen device
     * @throws HeadlessException
     *     when {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @throws SecurityException
     *     if the calling thread does not have permission to create modal dialogs
     *     with the given {@code modalityType}
     *
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     *
     * @since 1.6
     */
    public JDialog(Window owner, String title, Dialog.ModalityType modalityType,
                   GraphicsConfiguration gc) {
        super(owner, title, modalityType, gc);
        dialogInit();
    }

    /**
     * Called by the constructors to init the {@code JDialog} properly.
     */
    protected void dialogInit() {
        enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);
        setLocale( JComponent.getDefaultLocale() );
        setRootPane(createRootPane());
        setBackground(UIManager.getColor("control"));
        setRootPaneCheckingEnabled(true);
        if (JDialog.isDefaultLookAndFeelDecorated()) {
            boolean supportsWindowDecorations =
            UIManager.getLookAndFeel().getSupportsWindowDecorations();
            if (supportsWindowDecorations) {
                setUndecorated(true);
                getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
            }
        }
        sun.awt.SunToolkit.checkAndSetPolicy(this);
    }

    /**
     * Called by the constructor methods to create the default
     * {@code rootPane}.
     *
     * @return  a new {@code JRootPane}
     */
    protected JRootPane createRootPane() {
        JRootPane rp = new JRootPane();
        // NOTE: this uses setOpaque vs LookAndFeel.installProperty as there
        // is NO reason for the RootPane not to be opaque. For painting to
        // work the contentPane must be opaque, therefor the RootPane can
        // also be opaque.
        rp.setOpaque(true);
        return rp;
    }

    /**
     * Handles window events depending on the state of the
     * {@code defaultCloseOperation} property.
     *
     * @see #setDefaultCloseOperation
     */
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);

        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            switch(defaultCloseOperation) {
              case HIDE_ON_CLOSE:
                 setVisible(false);
                 break;
              case DISPOSE_ON_CLOSE:
                 dispose();
                 break;
              case DO_NOTHING_ON_CLOSE:
                 default:
                 break;
            }
        }
    }


    /**
     * Sets the operation that will happen by default when
     * the user initiates a "close" on this dialog.
     * You must specify one of the following choices:
     * <br><br>
     * <ul>
     * <li>{@code DO_NOTHING_ON_CLOSE}
     * (defined in {@code WindowConstants}):
     * Don't do anything; require the
     * program to handle the operation in the {@code windowClosing}
     * method of a registered {@code WindowListener} object.
     *
     * <li>{@code HIDE_ON_CLOSE}
     * (defined in {@code WindowConstants}):
     * Automatically hide the dialog after
     * invoking any registered {@code WindowListener}
     * objects.
     *
     * <li>{@code DISPOSE_ON_CLOSE}
     * (defined in {@code WindowConstants}):
     * Automatically hide and dispose the
     * dialog after invoking any registered {@code WindowListener}
     * objects.
     * </ul>
     * <p>
     * The value is set to {@code HIDE_ON_CLOSE} by default. Changes
     * to the value of this property cause the firing of a property
     * change event, with property name "defaultCloseOperation".
     * <p>
     * <b>Note</b>: When the last displayable window within the
     * Java virtual machine (VM) is disposed of, the VM may
     * terminate.  See <a href="../../java/awt/doc-files/AWTThreadIssues.html">
     * AWT Threading Issues</a> for more information.
     *
     * @param operation the operation which should be performed when the
     *        user closes the dialog
     * @throws IllegalArgumentException if defaultCloseOperation value
     *         isn't one of the above valid values
     * @see #addWindowListener
     * @see #getDefaultCloseOperation
     * @see WindowConstants
     */
    @BeanProperty(preferred = true, enumerationValues = {
            "WindowConstants.DO_NOTHING_ON_CLOSE",
            "WindowConstants.HIDE_ON_CLOSE",
            "WindowConstants.DISPOSE_ON_CLOSE"}, description
            = "The dialog's default close operation.")
    public void setDefaultCloseOperation(int operation) {
        if (operation != DO_NOTHING_ON_CLOSE &&
            operation != HIDE_ON_CLOSE &&
            operation != DISPOSE_ON_CLOSE) {
            throw new IllegalArgumentException("defaultCloseOperation must be one of: DO_NOTHING_ON_CLOSE, HIDE_ON_CLOSE, or DISPOSE_ON_CLOSE");
        }

        int oldValue = this.defaultCloseOperation;
        this.defaultCloseOperation = operation;
        firePropertyChange("defaultCloseOperation", oldValue, operation);
    }

   /**
    * Returns the operation which occurs when the user
    * initiates a "close" on this dialog.
    *
    * @return an integer indicating the window-close operation
    * @see #setDefaultCloseOperation
    */
    public int getDefaultCloseOperation() {
        return defaultCloseOperation;
    }

    /**
     * Sets the {@code transferHandler} property, which is a mechanism to
     * support transfer of data into this component. Use {@code null}
     * if the component does not support data transfer operations.
     * <p>
     * If the system property {@code suppressSwingDropSupport} is {@code false}
     * (the default) and the current drop target on this component is either
     * {@code null} or not a user-set drop target, this method will change the
     * drop target as follows: If {@code newHandler} is {@code null} it will
     * clear the drop target. If not {@code null} it will install a new
     * {@code DropTarget}.
     * <p>
     * Note: When used with {@code JDialog}, {@code TransferHandler} only
     * provides data import capability, as the data export related methods
     * are currently typed to {@code JComponent}.
     * <p>
     * Please see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/dnd/index.html">
     * How to Use Drag and Drop and Data Transfer</a>, a section in
     * <em>The Java Tutorial</em>, for more information.
     *
     * @param newHandler the new {@code TransferHandler}
     *
     * @see TransferHandler
     * @see #getTransferHandler
     * @see java.awt.Component#setDropTarget
     * @since 1.6
     */
    @BeanProperty(hidden = true, description
            = "Mechanism for transfer of data into the component")
    public void setTransferHandler(TransferHandler newHandler) {
        TransferHandler oldHandler = transferHandler;
        transferHandler = newHandler;
        SwingUtilities.installSwingDropTargetAsNecessary(this, transferHandler);
        firePropertyChange("transferHandler", oldHandler, newHandler);
    }

    /**
     * Gets the {@code transferHandler} property.
     *
     * @return the value of the {@code transferHandler} property
     *
     * @see TransferHandler
     * @see #setTransferHandler
     * @since 1.6
     */
    public TransferHandler getTransferHandler() {
        return transferHandler;
    }

    /**
     * Calls {@code paint(g)}.  This method was overridden to
     * prevent an unnecessary call to clear the background.
     *
     * @param g  the {@code Graphics} context in which to paint
     */
    public void update(Graphics g) {
        paint(g);
    }

   /**
    * Sets the menubar for this dialog.
    *
    * @param menu the menubar being placed in the dialog
    *
    * @see #getJMenuBar
    */
    @BeanProperty(bound = false, hidden = true, description
            = "The menubar for accessing pulldown menus from this dialog.")
    public void setJMenuBar(final JMenuBar menu) {
        getRootPane().setJMenuBar(menu);
    }

   /**
    * Returns the menubar set on this dialog.
    *
    * @return the menubar set on this dialog
    * @see #setJMenuBar
    */
    public JMenuBar getJMenuBar() {
        return getRootPane().getJMenuBar();
    }

    /**
     * Returns whether calls to {@code add} and
     * {@code setLayout} are forwarded to the {@code contentPane}.
     *
     * @return true if {@code add} and {@code setLayout}
     *         are forwarded; false otherwise
     *
     * @see #addImpl
     * @see #setLayout
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }


    /**
     * Sets whether calls to {@code add} and
     * {@code setLayout} are forwarded to the {@code contentPane}.
     *
     * @param enabled  true if {@code add} and {@code setLayout}
     *        are forwarded, false if they should operate directly on the
     *        {@code JDialog}.
     *
     * @see #addImpl
     * @see #setLayout
     * @see #isRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    @BeanProperty(hidden = true, description
            = "Whether the add and setLayout methods are forwarded")
    protected void setRootPaneCheckingEnabled(boolean enabled) {
        rootPaneCheckingEnabled = enabled;
    }

    /**
     * Adds the specified child {@code Component}.
     * This method is overridden to conditionally forward calls to the
     * {@code contentPane}.
     * By default, children are added to the {@code contentPane} instead
     * of the frame, refer to {@link javax.swing.RootPaneContainer} for
     * details.
     *
     * @param comp the component to be enhanced
     * @param constraints the constraints to be respected
     * @param index the index
     * @throws IllegalArgumentException if {@code index} is invalid
     * @throws IllegalArgumentException if adding the container's parent
     *                  to itself
     * @throws IllegalArgumentException if adding a window to a container
     *
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    protected void addImpl(Component comp, Object constraints, int index)
    {
        if(isRootPaneCheckingEnabled()) {
            getContentPane().add(comp, constraints, index);
        }
        else {
            super.addImpl(comp, constraints, index);
        }
    }

    /**
     * Removes the specified component from the container. If
     * {@code comp} is not the {@code rootPane}, this will forward
     * the call to the {@code contentPane}. This will do nothing if
     * {@code comp} is not a child of the {@code JDialog} or
     * {@code contentPane}.
     *
     * @param comp the component to be removed
     * @throws NullPointerException if {@code comp} is null
     * @see #add
     * @see javax.swing.RootPaneContainer
     */
    public void remove(Component comp) {
        if (comp == rootPane) {
            super.remove(comp);
        } else {
            getContentPane().remove(comp);
        }
    }


    /**
     * Sets the {@code LayoutManager}.
     * Overridden to conditionally forward the call to the
     * {@code contentPane}.
     * Refer to {@link javax.swing.RootPaneContainer} for
     * more information.
     *
     * @param manager the {@code LayoutManager}
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    public void setLayout(LayoutManager manager) {
        if(isRootPaneCheckingEnabled()) {
            getContentPane().setLayout(manager);
        }
        else {
            super.setLayout(manager);
        }
    }


    /**
     * Returns the {@code rootPane} object for this dialog.
     *
     * @see #setRootPane
     * @see RootPaneContainer#getRootPane
     */
    @BeanProperty(bound = false, hidden = true, description
            = "the RootPane object for this dialog.")
    public JRootPane getRootPane() {
        return rootPane;
    }


    /**
     * Sets the {@code rootPane} property.
     * This method is called by the constructor.
     *
     * @param root the {@code rootPane} object for this dialog
     *
     * @see #getRootPane
     */
    protected void setRootPane(JRootPane root) {
        if(rootPane != null) {
            remove(rootPane);
        }
        rootPane = root;
        if(rootPane != null) {
            boolean checkingEnabled = isRootPaneCheckingEnabled();
            try {
                setRootPaneCheckingEnabled(false);
                add(rootPane, BorderLayout.CENTER);
            }
            finally {
                setRootPaneCheckingEnabled(checkingEnabled);
            }
        }
    }


    /**
     * Returns the {@code contentPane} object for this dialog.
     *
     * @return the {@code contentPane} property
     *
     * @see #setContentPane
     * @see RootPaneContainer#getContentPane
     */
    public Container getContentPane() {
        return getRootPane().getContentPane();
    }


   /**
     * Sets the {@code contentPane} property.
     * This method is called by the constructor.
     * <p>
     * Swing's painting architecture requires an opaque {@code JComponent}
     * in the containment hierarchy. This is typically provided by the
     * content pane. If you replace the content pane it is recommended you
     * replace it with an opaque {@code JComponent}.
     * @see JRootPane
     *
     * @param contentPane the {@code contentPane} object for this dialog
     *
     * @throws java.awt.IllegalComponentStateException (a runtime
     *            exception) if the content pane parameter is {@code null}
     * @see #getContentPane
     * @see RootPaneContainer#setContentPane
     */
    @BeanProperty(bound = false, hidden = true, description
            = "The client area of the dialog where child components are normally inserted.")
    public void setContentPane(Container contentPane) {
        getRootPane().setContentPane(contentPane);
    }

    /**
     * Returns the {@code layeredPane} object for this dialog.
     *
     * @return the {@code layeredPane} property
     *
     * @see #setLayeredPane
     * @see RootPaneContainer#getLayeredPane
     */
    public JLayeredPane getLayeredPane() {
        return getRootPane().getLayeredPane();
    }

    /**
     * Sets the {@code layeredPane} property.
     * This method is called by the constructor.
     *
     * @param layeredPane the new {@code layeredPane} property
     *
     * @throws java.awt.IllegalComponentStateException (a runtime
     *            exception) if the layered pane parameter is null
     * @see #getLayeredPane
     * @see RootPaneContainer#setLayeredPane
     */
    @BeanProperty(bound = false, hidden = true, description
            = "The pane which holds the various dialog layers.")
    public void setLayeredPane(JLayeredPane layeredPane) {
        getRootPane().setLayeredPane(layeredPane);
    }

    /**
     * Returns the {@code glassPane} object for this dialog.
     *
     * @return the {@code glassPane} property
     *
     * @see #setGlassPane
     * @see RootPaneContainer#getGlassPane
     */
    public Component getGlassPane() {
        return getRootPane().getGlassPane();
    }

    /**
     * Sets the {@code glassPane} property.
     * This method is called by the constructor.
     *
     * @param glassPane the {@code glassPane} object for this dialog
     * @see #getGlassPane
     * @see RootPaneContainer#setGlassPane
     */
    @BeanProperty(bound = false, hidden = true, description
            = "A transparent pane used for menu rendering.")
    public void setGlassPane(Component glassPane) {
        getRootPane().setGlassPane(glassPane);
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.6
     */
    @BeanProperty(bound = false)
    public Graphics getGraphics() {
        JComponent.getGraphicsInvoked(this);
        return super.getGraphics();
    }

    /**
     * Repaints the specified rectangle of this component within
     * {@code time} milliseconds.  Refer to {@code RepaintManager}
     * for details on how the repaint is handled.
     *
     * @param     time   maximum time in milliseconds before update
     * @param     x    the <i>x</i> coordinate
     * @param     y    the <i>y</i> coordinate
     * @param     width    the width
     * @param     height   the height
     * @see       RepaintManager
     * @since     1.6
     */
    public void repaint(long time, int x, int y, int width, int height) {
        if (RepaintManager.HANDLE_TOP_LEVEL_PAINT) {
            RepaintManager.currentManager(this).addDirtyRegion(
                              this, x, y, width, height);
        }
        else {
            super.repaint(time, x, y, width, height);
        }
    }

    /**
     * Provides a hint as to whether or not newly created {@code JDialog}s
     * should have their Window decorations (such as borders, widgets to
     * close the window, title...) provided by the current look
     * and feel. If {@code defaultLookAndFeelDecorated} is true,
     * the current {@code LookAndFeel} supports providing window
     * decorations, and the current window manager supports undecorated
     * windows, then newly created {@code JDialog}s will have their
     * Window decorations provided by the current {@code LookAndFeel}.
     * Otherwise, newly created {@code JDialog}s will have their
     * Window decorations provided by the current window manager.
     * <p>
     * You can get the same effect on a single JDialog by doing the following:
     * <pre>
     *    JDialog dialog = new JDialog();
     *    dialog.setUndecorated(true);
     *    dialog.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
     * </pre>
     *
     * @param defaultLookAndFeelDecorated A hint as to whether or not current
     *        look and feel should provide window decorations
     * @see javax.swing.LookAndFeel#getSupportsWindowDecorations
     * @since 1.4
     */
    public static void setDefaultLookAndFeelDecorated(boolean defaultLookAndFeelDecorated) {
        if (defaultLookAndFeelDecorated) {
            SwingUtilities.appContextPut(defaultLookAndFeelDecoratedKey, Boolean.TRUE);
        } else {
            SwingUtilities.appContextPut(defaultLookAndFeelDecoratedKey, Boolean.FALSE);
        }
    }

    /**
     * Returns true if newly created {@code JDialog}s should have their
     * Window decorations provided by the current look and feel. This is only
     * a hint, as certain look and feels may not support this feature.
     *
     * @return true if look and feel should provide Window decorations.
     * @since 1.4
     */
    public static boolean isDefaultLookAndFeelDecorated() {
        Boolean defaultLookAndFeelDecorated =
            (Boolean) SwingUtilities.appContextGet(defaultLookAndFeelDecoratedKey);
        if (defaultLookAndFeelDecorated == null) {
            defaultLookAndFeelDecorated = Boolean.FALSE;
        }
        return defaultLookAndFeelDecorated.booleanValue();
    }

    /**
     * Returns a string representation of this {@code JDialog}.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be {@code null}.
     *
     * @return  a string representation of this {@code JDialog}.
     */
    protected String paramString() {
        String defaultCloseOperationString;
        if (defaultCloseOperation == HIDE_ON_CLOSE) {
            defaultCloseOperationString = "HIDE_ON_CLOSE";
        } else if (defaultCloseOperation == DISPOSE_ON_CLOSE) {
            defaultCloseOperationString = "DISPOSE_ON_CLOSE";
        } else if (defaultCloseOperation == DO_NOTHING_ON_CLOSE) {
            defaultCloseOperationString = "DO_NOTHING_ON_CLOSE";
        } else defaultCloseOperationString = "";
        String rootPaneString = (rootPane != null ?
                                 rootPane.toString() : "");
        String rootPaneCheckingEnabledString = (rootPaneCheckingEnabled ?
                                                "true" : "false");

        return super.paramString() +
        ",defaultCloseOperation=" + defaultCloseOperationString +
        ",rootPane=" + rootPaneString +
        ",rootPaneCheckingEnabled=" + rootPaneCheckingEnabledString;
    }


/////////////////
// Accessibility support
////////////////

    /**
     * {@code AccessibleContext} associated with this {@code JDialog}
     */
    protected AccessibleContext accessibleContext = null;

    /**
     * Gets the AccessibleContext associated with this JDialog.
     * For JDialogs, the AccessibleContext takes the form of an
     * AccessibleJDialog.
     * A new AccessibleJDialog instance is created if necessary.
     *
     * @return an AccessibleJDialog that serves as the
     *         AccessibleContext of this JDialog
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJDialog();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * {@code JDialog} class.  It provides an implementation of the
     * Java Accessibility API appropriate to dialog user-interface
     * elements.
     */
    protected class AccessibleJDialog extends AccessibleAWTDialog {

        /**
         * Constructs an {@code AccessibleJDialog}.
         */
        protected AccessibleJDialog() {}

        // AccessibleContext methods
        //
        /**
         * Get the accessible name of this object.
         *
         * @return the localized name of the object -- can be null if this
         * object does not have a name
         */
        public String getAccessibleName() {
            if (accessibleName != null) {
                return accessibleName;
            } else {
                if (getTitle() == null) {
                    return super.getAccessibleName();
                } else {
                    return getTitle();
                }
            }
        }

        /**
         * Get the state of this object.
         *
         * @return an instance of AccessibleStateSet containing the current
         * state set of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();

            if (isResizable()) {
                states.add(AccessibleState.RESIZABLE);
            }
            if (getFocusOwner() != null) {
                states.add(AccessibleState.ACTIVE);
            }
            if (isModal()) {
                states.add(AccessibleState.MODAL);
            }
            return states;
        }

    } // inner class AccessibleJDialog
}
