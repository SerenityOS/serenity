/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.compiler.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.gc.epsilon.*;
import sun.jvm.hotspot.gc.parallel.*;
import sun.jvm.hotspot.gc.shared.*;
import sun.jvm.hotspot.gc.shenandoah.*;
import sun.jvm.hotspot.gc.g1.*;
import sun.jvm.hotspot.gc.z.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.ui.*;
import sun.jvm.hotspot.ui.tree.*;
import sun.jvm.hotspot.ui.classbrowser.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** The top-level HotSpot Debugger. FIXME: make this an embeddable
    component! (Among other things, figure out what to do with the
    menu bar...) */

public class HSDB implements ObjectHistogramPanel.Listener, SAListener {
  public static void main(String[] args) {
    new HSDB(args).run();
  }

  //--------------------------------------------------------------------------------
  // Internals only below this point
  //
  private HotSpotAgent agent;
  private JVMDebugger jvmDebugger;
  private JDesktopPane desktop;
  private boolean      attached;
  private boolean      argError;
  private JFrame frame;
  private java.util.List<JMenuItem> attachMenuItems;
  private java.util.List<JMenuItem> detachMenuItems;
  private JMenu toolsMenu;
  private JMenuItem showDbgConsoleMenuItem;
  private JMenuItem computeRevPtrsMenuItem;
  private JInternalFrame attachWaitDialog;
  private JInternalFrame threadsFrame;
  private JInternalFrame consoleFrame;
  private WorkerThread workerThread;
  // These had to be made data members because they are referenced in inner classes.
  private int pid;
  private String execPath;
  private String coreFilename;
  private String debugServerName;

  private void doUsage() {
    System.out.println("Usage:  java HSDB [[pid] | [path-to-java-executable [path-to-corefile]] | help ]");
    System.out.println("           pid:                     attach to the process whose id is 'pid'");
    System.out.println("           path-to-java-executable: Debug a core file produced by this program");
    System.out.println("           path-to-corefile:        Debug this corefile.  The default is 'core'");
    System.out.println("        If no arguments are specified, you can select what to do from the GUI.\n");
    HotSpotAgent.showUsage();
    argError = true;
  }

  public HSDB(JVMDebugger d) {
    pid = -1;
    execPath = null;
    coreFilename = null;
    debugServerName = null;
    jvmDebugger = d;
  }

  private HSDB(String[] args) {
    pid = -1;
    execPath = null;
    coreFilename = null;
    debugServerName = null;

    switch (args.length) {
    case (0):
      break;

    case (1):
      if (args[0].equals("help") || args[0].equals("-help")) {
        doUsage();
      }
      try {
        // Attempt to attach as a PID
        pid = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        // Attempt to connect to remote debug server
        debugServerName = args[0];
      }
      break;

    case (2):
      execPath = args[0];
      coreFilename = args[1];
      break;

    default:
      System.out.println("HSDB Error: Too many options specified");
      doUsage();
    }
  }

  private class CloseUI extends WindowAdapter {

      @Override
      public void windowClosing(WindowEvent e) {
          workerThread.shutdown();
          frame.dispose();
      }

  }

  public void run() {
    // Don't start the UI if there were bad arguments.
    if (argError) {
        return;
    }

    // Create frame first, to catch any GUI creation issues
    // before we initialize agent

    frame = new JFrame("HSDB - HotSpot Debugger");
    frame.setSize(800, 600);
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new CloseUI());

    agent = new HotSpotAgent();
    workerThread = new WorkerThread();
    attachMenuItems = new java.util.ArrayList<>();
    detachMenuItems = new java.util.ArrayList<>();


    JMenuBar menuBar = new JMenuBar();

    //
    // File menu
    //

    JMenu menu = new JMenu("File");
    menu.setMnemonic(KeyEvent.VK_F);
    JMenuItem item;
    item = createMenuItem("Attach to HotSpot process...",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showAttachDialog();
                              }
                            });
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
    item.setMnemonic(KeyEvent.VK_A);
    menu.add(item);
    attachMenuItems.add(item);

    item = createMenuItem("Open HotSpot core file...",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showOpenCoreFileDialog();
                              }
                            });
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
    item.setMnemonic(KeyEvent.VK_O);
    menu.add(item);
    attachMenuItems.add(item);

    item = createMenuItem("Connect to debug server...",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showConnectDialog();
                              }
                            });
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
    item.setMnemonic(KeyEvent.VK_S);
    menu.add(item);
    attachMenuItems.add(item);

    item = createMenuItem("Detach",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                detach();
                              }
                            });
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
    item.setMnemonic(KeyEvent.VK_S);
    menu.add(item);
    detachMenuItems.add(item);

    // Disable detach menu items at first
    setMenuItemsEnabled(detachMenuItems, false);

    menu.addSeparator();

    item = createMenuItem("Exit",
                            new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                  workerThread.shutdown();
                                  frame.dispose();
                                }
                              });
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
    item.setMnemonic(KeyEvent.VK_X);
    menu.add(item);
    menuBar.add(menu);

    //
    // Tools menu
    //

    toolsMenu = new JMenu("Tools");
    toolsMenu.setMnemonic(KeyEvent.VK_T);

    item = createMenuItem("Class Browser",
                          new ActionListener() {
                             public void actionPerformed(ActionEvent e) {
                                showClassBrowser();
                             }
                          });
    item.setMnemonic(KeyEvent.VK_B);

    toolsMenu.add(item);

    item = createMenuItem("Code Viewer",
                          new ActionListener() {
                             public void actionPerformed(ActionEvent e) {
                                showCodeViewer();
                             }
                          });
    item.setMnemonic(KeyEvent.VK_C);

    toolsMenu.add(item);


    item = createMenuItem("Compute Reverse Ptrs",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                fireComputeReversePtrs();
                              }
                            });
    computeRevPtrsMenuItem = item;
    item.setMnemonic(KeyEvent.VK_M);
    toolsMenu.add(item);

    item = createMenuItem("Deadlock Detection",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showDeadlockDetectionPanel();
                              }
                            });
    item.setMnemonic(KeyEvent.VK_D);
    toolsMenu.add(item);


    item = createMenuItem("Find Pointer",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showFindPanel();
                              }
                            });
    item.setMnemonic(KeyEvent.VK_P);
    toolsMenu.add(item);

    item = createMenuItem("Find Value In Heap",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showFindInHeapPanel();
                              }
                            });
    item.setMnemonic(KeyEvent.VK_V);
    toolsMenu.add(item);

    item = createMenuItem("Find Value In Code Cache",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showFindInCodeCachePanel();
                              }
                            });
    item.setMnemonic(KeyEvent.VK_A);
    toolsMenu.add(item);

    item = createMenuItem("Heap Parameters",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showHeapParametersPanel();
                              }
                            });
    item.setMnemonic(KeyEvent.VK_H);
    toolsMenu.add(item);

    item = createMenuItem("Inspector",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showInspector(null);
                              }
                            });
    item.setMnemonic(KeyEvent.VK_R);
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
    toolsMenu.add(item);

    item = createMenuItem("Memory Viewer",
                          new ActionListener() {
                             public void actionPerformed(ActionEvent e) {
                                showMemoryViewer();
                             }
                          });
    item.setMnemonic(KeyEvent.VK_M);
    toolsMenu.add(item);

    item = createMenuItem("Monitor Cache Dump",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showMonitorCacheDumpPanel();
                              }
                            });
    item.setMnemonic(KeyEvent.VK_D);
    toolsMenu.add(item);

    item = createMenuItem("Object Histogram",
                          new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                showObjectHistogram();
                              }
                            });
    item.setMnemonic(KeyEvent.VK_O);
    toolsMenu.add(item);

    item = createMenuItem("Show System Properties",
                          new ActionListener() {
                             public void actionPerformed(ActionEvent e) {
                                showSystemProperties();
                             }
                          });
    item.setMnemonic(KeyEvent.VK_S);
    toolsMenu.add(item);

    item = createMenuItem("Show VM Version",
                          new ActionListener() {
                             public void actionPerformed(ActionEvent e) {
                                showVMVersion();
                             }
                          });
    item.setMnemonic(KeyEvent.VK_M);
    toolsMenu.add(item);

    item = createMenuItem("Show -XX flags",
                          new ActionListener() {
                             public void actionPerformed(ActionEvent e) {
                                showCommandLineFlags();
                             }
                          });
    item.setMnemonic(KeyEvent.VK_X);
    toolsMenu.add(item);

    toolsMenu.setEnabled(false);
    menuBar.add(toolsMenu);

    //
    // Windows menu
    //

    JMenu windowsMenu = new JMenu("Windows");
    windowsMenu.setMnemonic(KeyEvent.VK_W);
    item = createMenuItem("Console",
                          new ActionListener() {
                             public void actionPerformed(ActionEvent e) {
                                 showConsole();
                             }
                          });
    item.setMnemonic(KeyEvent.VK_C);
    windowsMenu.add(item);
    showDbgConsoleMenuItem = createMenuItem("Debugger Console",
                                         new ActionListener() {
                                             public void actionPerformed(ActionEvent e) {
                                               showDebuggerConsole();
                                             }
                                           });
    showDbgConsoleMenuItem.setMnemonic(KeyEvent.VK_D);
    windowsMenu.add(showDbgConsoleMenuItem);
    showDbgConsoleMenuItem.setEnabled(false);

    menuBar.add(windowsMenu);


    frame.setJMenuBar(menuBar);

    desktop = new JDesktopPane();
    frame.getContentPane().add(desktop);
    GraphicsUtilities.reshapeToAspectRatio(frame, 4.0f/3.0f, 0.75f, Toolkit.getDefaultToolkit().getScreenSize());
    GraphicsUtilities.centerInContainer(frame, Toolkit.getDefaultToolkit().getScreenSize());
    frame.setVisible(true);

    Runtime.getRuntime().addShutdownHook(new java.lang.Thread() {
        public void run() {
          detachDebugger();
        }
      });

    // If jvmDebugger is already set, we have been given a JVMDebugger.
    // Otherwise, if pid != -1 we are supposed to attach to it.
    // If execPath != null, it is the path of a jdk/bin/java
    // and coreFilename is the pathname of a core file we are
    // supposed to attach to.
    // Finally, if debugServerName != null, we are supposed to
    // connect to remote debug server.

    if (jvmDebugger != null) {
      attach(jvmDebugger);
    } else if (pid != -1) {
      attach(pid);
    } else if (execPath != null) {
      attach(execPath, coreFilename);
    } else if (debugServerName != null) {
      connect(debugServerName);
    }
  }

  // FIXME: merge showAttachDialog, showOpenCoreFileDialog, showConnectDialog
  private void showAttachDialog() {
    // FIXME: create filtered text field which only accepts numbers
    setMenuItemsEnabled(attachMenuItems, false);
    final JInternalFrame attachDialog = new JInternalFrame("Attach to HotSpot process");
    attachDialog.getContentPane().setLayout(new BorderLayout());

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    attachDialog.setBackground(panel.getBackground());

    panel.add(new JLabel("Enter process ID:"));
    final JTextField pidTextField = new JTextField(10);
    ActionListener attacher = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          attachDialog.setVisible(false);
          desktop.remove(attachDialog);
          workerThread.invokeLater(new Runnable() {
              public void run() {
                attach(Integer.parseInt(pidTextField.getText()));
              }
            });
        }
      };

    pidTextField.addActionListener(attacher);
    panel.add(pidTextField);
    attachDialog.getContentPane().add(panel, BorderLayout.NORTH);

    Box vbox = Box.createVerticalBox();
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    JTextArea ta = new JTextArea(
                                 "Enter the process ID of a currently-running HotSpot process. On " +
                                 "most Unix operating systems, this can be determined by " +
                                 "typing \"ps -u <your username> | grep java\"; the process ID is the " +
                                 "first number which appears on the resulting line. On Windows, the " +
                                 "process ID is present in the Task Manager, which can be brought up " +
                                 "while logged on to the desktop by pressing Ctrl-Alt-Delete.");
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setEditable(false);
    ta.setBackground(panel.getBackground());
    panel.add(ta);
    vbox.add(panel);

    Box hbox = Box.createHorizontalBox();
    hbox.add(Box.createGlue());
    JButton button = new JButton("OK");
    button.addActionListener(attacher);
    hbox.add(button);
    hbox.add(Box.createHorizontalStrut(20));
    button = new JButton("Cancel");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          attachDialog.setVisible(false);
          desktop.remove(attachDialog);
          setMenuItemsEnabled(attachMenuItems, true);
        }
      });
    hbox.add(button);
    hbox.add(Box.createGlue());
    panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panel.add(hbox);
    vbox.add(panel);

    attachDialog.getContentPane().add(vbox, BorderLayout.SOUTH);

    desktop.add(attachDialog);
    attachDialog.setSize(400, 300);
    GraphicsUtilities.centerInContainer(attachDialog);
    attachDialog.show();
    pidTextField.requestFocus();
  }

  // FIXME: merge showAttachDialog, showOpenCoreFileDialog, showConnectDialog
  private void showOpenCoreFileDialog() {
    setMenuItemsEnabled(attachMenuItems, false);
    final JInternalFrame dialog = new JInternalFrame("Open Core File");
    dialog.getContentPane().setLayout(new BorderLayout());

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    dialog.setBackground(panel.getBackground());

    Box hbox = Box.createHorizontalBox();
    Box vbox = Box.createVerticalBox();
    vbox.add(new JLabel("Path to core file:"));
    vbox.add(new JLabel("Path to Java executable:"));
    hbox.add(vbox);

    vbox = Box.createVerticalBox();
    final JTextField corePathField = new JTextField(40);
    final JTextField execPathField = new JTextField(40);
    vbox.add(corePathField);
    vbox.add(execPathField);
    hbox.add(vbox);

    final JButton browseCorePath = new JButton("Browse ..");
    final JButton browseExecPath = new JButton("Browse ..");
    browseCorePath.addActionListener(new ActionListener() {
                                        public void actionPerformed(ActionEvent e) {
                                           JFileChooser fileChooser = new JFileChooser(new File("."));
                                           int retVal = fileChooser.showOpenDialog(dialog);
                                           if (retVal == JFileChooser.APPROVE_OPTION) {
                                              corePathField.setText(fileChooser.getSelectedFile().getPath());
                                           }
                                        }
                                     });
    browseExecPath.addActionListener(new ActionListener() {
                                        public void actionPerformed(ActionEvent e) {
                                           JFileChooser fileChooser = new JFileChooser(new File("."));
                                           int retVal = fileChooser.showOpenDialog(dialog);
                                           if (retVal == JFileChooser.APPROVE_OPTION) {
                                              execPathField.setText(fileChooser.getSelectedFile().getPath());
                                           }
                                        }
                                     });
    vbox = Box.createVerticalBox();
    vbox.add(browseCorePath);
    vbox.add(browseExecPath);
    hbox.add(vbox);

    panel.add(hbox);
    dialog.getContentPane().add(panel, BorderLayout.NORTH);

    ActionListener attacher = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          dialog.setVisible(false);
          desktop.remove(dialog);
          workerThread.invokeLater(new Runnable() {
              public void run() {
                attach(execPathField.getText(), corePathField.getText());
              }
            });
        }
      };
    corePathField.addActionListener(attacher);
    execPathField.addActionListener(attacher);

    vbox = Box.createVerticalBox();
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    JTextArea ta = new JTextArea(
                                 "Enter the full path names to the core file from a HotSpot process " +
                                 "and the Java executable from which it came. The latter is typically " +
                                 "located in the JDK/JRE directory under the directory " +
                                 "jre/bin/<arch>/native_threads.");
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setEditable(false);
    ta.setBackground(panel.getBackground());
    panel.add(ta);
    vbox.add(panel);

    hbox = Box.createHorizontalBox();
    hbox.add(Box.createGlue());
    JButton button = new JButton("OK");
    button.addActionListener(attacher);
    hbox.add(button);
    hbox.add(Box.createHorizontalStrut(20));
    button = new JButton("Cancel");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          dialog.setVisible(false);
          desktop.remove(dialog);
          setMenuItemsEnabled(attachMenuItems, true);
        }
      });
    hbox.add(button);
    hbox.add(Box.createGlue());
    panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panel.add(hbox);
    vbox.add(panel);

    dialog.getContentPane().add(vbox, BorderLayout.SOUTH);

    desktop.add(dialog);
    dialog.setSize(500, 300);
    GraphicsUtilities.centerInContainer(dialog);
    dialog.show();
    corePathField.requestFocus();
  }

  // FIXME: merge showAttachDialog, showOpenCoreFileDialog, showConnectDialog
  private void showConnectDialog() {
    // FIXME: create filtered text field which only accepts numbers
    setMenuItemsEnabled(attachMenuItems, false);
    final JInternalFrame dialog = new JInternalFrame("Connect to HotSpot Debug Server");
    dialog.getContentPane().setLayout(new BorderLayout());

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    dialog.setBackground(panel.getBackground());

    panel.add(new JLabel("Enter machine name:"));
    final JTextField pidTextField = new JTextField(40);
    ActionListener attacher = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          dialog.setVisible(false);
          desktop.remove(dialog);
          workerThread.invokeLater(new Runnable() {
              public void run() {
                connect(pidTextField.getText());
              }
            });
        }
      };

    pidTextField.addActionListener(attacher);
    panel.add(pidTextField);
    dialog.getContentPane().add(panel, BorderLayout.NORTH);

    Box vbox = Box.createVerticalBox();
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    JTextArea ta = new JTextArea(
                                 "Enter the name of a machine on which the HotSpot \"Debug Server\" is " +
                                 "running and is attached to a process or core file.");
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setEditable(false);
    ta.setBackground(panel.getBackground());
    panel.add(ta);
    vbox.add(panel);

    Box hbox = Box.createHorizontalBox();
    hbox.add(Box.createGlue());
    JButton button = new JButton("OK");
    button.addActionListener(attacher);
    hbox.add(button);
    hbox.add(Box.createHorizontalStrut(20));
    button = new JButton("Cancel");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          dialog.setVisible(false);
          desktop.remove(dialog);
          setMenuItemsEnabled(attachMenuItems, true);
        }
      });
    hbox.add(button);
    hbox.add(Box.createGlue());
    panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panel.add(hbox);
    vbox.add(panel);

    dialog.getContentPane().add(vbox, BorderLayout.SOUTH);

    desktop.add(dialog);
    dialog.setSize(400, 300);
    GraphicsUtilities.centerInContainer(dialog);
    dialog.show();
    pidTextField.requestFocus();
  }

  public void showThreadOopInspector(JavaThread thread) {
    showInspector(new OopTreeNodeAdapter(thread.getThreadObj(), null));
  }

  public void showInspector(SimpleTreeNode adapter) {
    showPanel("Inspector", new Inspector(adapter), 1.0f, 0.65f);
  }

  public void showLiveness(Oop oop, LivenessPathList liveness) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintStream tty = new PrintStream(bos);
    int numPaths = liveness.size();
    for (int i = 0; i < numPaths; i++) {
      tty.println("Path " + (i + 1) + " of " + numPaths + ":");
      liveness.get(i).printOn(tty);
    }
    JTextArea ta = new JTextArea(bos.toString());
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setEditable(false);

    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    JScrollPane scroller = new JScrollPane();
    scroller.getViewport().add(ta);

    panel.add(scroller, BorderLayout.CENTER);

    bos = new ByteArrayOutputStream();
    tty = new PrintStream(bos);
    tty.print("Liveness result for ");
    Oop.printOopValueOn(oop, tty);

    JInternalFrame frame = new JInternalFrame(bos.toString());
    frame.setResizable(true);
    frame.setClosable(true);
    frame.setIconifiable(true);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(panel, BorderLayout.CENTER);
    frame.pack();
    desktop.add(frame);
    GraphicsUtilities.reshapeToAspectRatio(frame, 0.5f / 0.2f, 0.5f, frame.getParent().getSize());
    frame.show();
  }

  private void fireComputeReversePtrs() {
    // Possible this might have been computed elsewhere
    if (VM.getVM().getRevPtrs() != null) {
      computeRevPtrsMenuItem.setEnabled(false);
      return;
    }

    workerThread.invokeLater(new Runnable() {
        public void run() {
          HeapProgress progress = new HeapProgress("Reverse Pointers Analysis");
          try {
            ReversePtrsAnalysis analysis = new ReversePtrsAnalysis();
            analysis.setHeapProgressThunk(progress);
            analysis.run();
            computeRevPtrsMenuItem.setEnabled(false);
          } catch (OutOfMemoryError e) {
            final String errMsg = formatMessage(e.toString(), 80);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  JOptionPane.showInternalMessageDialog(desktop,
                                                        "Error computing reverse pointers:" + errMsg,
                                                        "Error",
                                                        JOptionPane.WARNING_MESSAGE);
                }
              });
          } finally {
            // make sure the progress bar goes away
            progress.heapIterationComplete();
          }
        }
      });
  }

  // Simple struct containing signal information
  class SignalInfo {
    public int sigNum;
    public String sigName;
  }

  // Need to have mutable vframe as well as visible memory panel
  abstract class StackWalker implements Runnable {
    protected JavaVFrame vf;
    protected AnnotatedMemoryPanel annoPanel;

    StackWalker(JavaVFrame vf, AnnotatedMemoryPanel annoPanel) {
      this.vf = vf;
      this.annoPanel = annoPanel;
    }
  }

  public void showThreadStackMemory(final JavaThread thread) {
    // dumpStack(thread);
    JavaVFrame vframe = getLastJavaVFrame(thread);
    if (vframe == null) {
      JOptionPane.showInternalMessageDialog(desktop,
                                            "Thread \"" + thread.getThreadName() +
                                            "\" has no Java frames on its stack",
                                            "Show Stack Memory",
                                            JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    JInternalFrame stackFrame = new JInternalFrame("Stack Memory for " + thread.getThreadName());
    stackFrame.getContentPane().setLayout(new BorderLayout());
    stackFrame.setResizable(true);
    stackFrame.setClosable(true);
    stackFrame.setIconifiable(true);
    final long addressSize = agent.getTypeDataBase().getAddressSize();
    boolean is64Bit = (addressSize == 8);
    // This is somewhat of a  hack to guess a thread's stack limits since the
    // JavaThread doesn't support this functionality. However it is nice in that
    // it locks us into the active region of the thread's stack and not its
    // theoretical limits.
    //
    sun.jvm.hotspot.runtime.Frame tmpFrame = thread.getCurrentFrameGuess();
    Address sp = tmpFrame.getSP();
    Address starting = sp;
    Address maxSP = starting;
    Address minSP = starting;
    RegisterMap tmpMap = thread.newRegisterMap(false);
    while ((tmpFrame != null) && (!tmpFrame.isFirstFrame())) {
        tmpFrame = tmpFrame.sender(tmpMap);
        if (tmpFrame != null) {
          sp = tmpFrame.getSP();
          if (sp != null) {
            maxSP = AddressOps.max(maxSP, sp);
            minSP = AddressOps.min(minSP, sp);
          }
        }

    }
    // It is useful to be able to see say +/- 8K on the current stack range
    AnnotatedMemoryPanel annoMemPanel = new AnnotatedMemoryPanel(agent.getDebugger(), is64Bit, starting,
                                                                 minSP.addOffsetTo(-8192),
                                                                 maxSP.addOffsetTo( 8192));

    stackFrame.getContentPane().add(annoMemPanel, BorderLayout.CENTER);
    desktop.add(stackFrame);
    GraphicsUtilities.reshapeToAspectRatio(stackFrame, 4.0f / 3.0f, 0.85f, stackFrame.getParent().getSize());
    stackFrame.show();

    // Stackmap computation for interpreted frames is expensive; do
    // all stackwalking work in another thread for better GUI
    // responsiveness
    workerThread.invokeLater(new StackWalker(vframe, annoMemPanel) {
        public void run() {
          Address startAddr = null;

          // As this is a debugger, we want to provide potential crash
          // information to the user, i.e., by marking signal handler frames
          // on the stack. Since this system is currently targeted at
          // annotating the Java frames (interpreted or compiled) on the
          // stack and not, for example, "external" frames (note the current
          // absence of a PC-to-symbol lookup mechanism at the Debugger
          // level), we want to mark any Java frames which were interrupted
          // by a signal. We do this by making two passes over the stack,
          // one which finds signal handler frames and puts the parent
          // frames in a table and one which finds Java frames and if they
          // are in the table indicates that they were interrupted by a signal.

          Map<sun.jvm.hotspot.runtime.Frame, SignalInfo> interruptedFrameMap = new HashMap<>();
          {
            sun.jvm.hotspot.runtime.Frame tmpFrame = thread.getCurrentFrameGuess();
            RegisterMap tmpMap = thread.newRegisterMap(false);
            while ((tmpFrame != null) && (!tmpFrame.isFirstFrame())) {
              if (tmpFrame.isSignalHandlerFrameDbg()) {
                // Add some information to the map that we can extract later
                sun.jvm.hotspot.runtime.Frame interruptedFrame = tmpFrame.sender(tmpMap);
                SignalInfo info = new SignalInfo();
                info.sigNum  = tmpFrame.getSignalNumberDbg();
                info.sigName = tmpFrame.getSignalNameDbg();
                interruptedFrameMap.put(interruptedFrame, info);
              }
              tmpFrame = tmpFrame.sender(tmpMap);
            }
          }

          while (vf != null) {
            String anno = null;
            JavaVFrame curVFrame = vf;
            sun.jvm.hotspot.runtime.Frame curFrame = curVFrame.getFrame();
            Method interpreterFrameMethod = null;

            if (curVFrame.isInterpretedFrame()) {
              anno = "Interpreted frame";
            } else {
              anno = "Compiled frame";
              if (curVFrame.isDeoptimized()) {
                anno += " (deoptimized)";
              }
            }
            if (curVFrame.mayBeImpreciseDbg()) {
              anno += "; information may be imprecise";
            }

            if (curVFrame.isInterpretedFrame()) {
              // Find the codelet
              InterpreterCodelet codelet = VM.getVM().getInterpreter().getCodeletContaining(curFrame.getPC());
              String description = null;
              if (codelet != null) {
                description = codelet.getDescription();
              }
              if (description == null) {
                anno += "\n(Unknown interpreter codelet)";
              } else {
                anno += "\nExecuting in codelet \"" + description + "\" at PC = " + curFrame.getPC();
              }
            } else if (curVFrame.isCompiledFrame()) {
              anno += "\nExecuting at PC = " + curFrame.getPC();
            }

            if (startAddr == null) {
              startAddr = curFrame.getSP();
            }

            // FIXME: some compiled frames with empty oop map sets have been
            // found (for example, Vector's inner Enumeration class, method
            // "hasMoreElements"). Not sure yet why these cases are showing
            // up -- should be possible (though unlikely) for safepoint code
            // to patch the return instruction of these methods and then
            // later attempt to get an oop map for that instruction. For
            // now, we warn if we find such a method.
            boolean shouldSkipOopMaps = false;
            if (curVFrame.isCompiledFrame()) {
              CodeBlob cb = VM.getVM().getCodeCache().findBlob(curFrame.getPC());
              ImmutableOopMapSet maps = cb.getOopMaps();
              if ((maps == null) || (maps.getCount() == 0)) {
                shouldSkipOopMaps = true;
              }
            }

            // Add signal information to annotation if necessary
            SignalInfo sigInfo = (SignalInfo) interruptedFrameMap.get(curFrame);
            if (sigInfo != null) {
              // This frame took a signal and we need to report it.
              anno = (anno + "\n*** INTERRUPTED BY SIGNAL " + Integer.toString(sigInfo.sigNum) +
                      " (" + sigInfo.sigName + ")");
            }

            JavaVFrame nextVFrame = curVFrame;
            sun.jvm.hotspot.runtime.Frame nextFrame = curFrame;
            do {
              curVFrame = nextVFrame;
              curFrame = nextFrame;

              try {
                Method method = curVFrame.getMethod();
                if (interpreterFrameMethod == null && curVFrame.isInterpretedFrame()) {
                  interpreterFrameMethod = method;
                }
                int bci = curVFrame.getBCI();
                String lineNumberAnno = "";
                if (method.hasLineNumberTable()) {
                  if ((bci == DebugInformationRecorder.SYNCHRONIZATION_ENTRY_BCI) ||
                      (bci >= 0 && bci < method.getCodeSize())) {
                    lineNumberAnno = ", line " + method.getLineNumberFromBCI(bci);
                  } else {
                    lineNumberAnno = " (INVALID BCI)";
                  }
                }
                anno += "\n" + method.getMethodHolder().getName().asString() + "." +
                               method.getName().asString() + method.getSignature().asString() +
                               "\n@bci " + bci + lineNumberAnno;
              } catch (Exception e) {
                anno += "\n(ERROR while iterating vframes for frame " + curFrame + ")";
              }

              nextVFrame = curVFrame.javaSender();
              if (nextVFrame != null) {
                nextFrame = nextVFrame.getFrame();
              }
            } while (nextVFrame != null && nextFrame.equals(curFrame));

            if (shouldSkipOopMaps) {
              anno = anno + "\nNOTE: null or empty ImmutableOopMapSet found for this CodeBlob";
            }

            if (curFrame.getFP() != null) {
              annoPanel.addAnnotation(new Annotation(curFrame.getSP(),
                                                     curFrame.getFP(),
                                                     anno));
            } else {
              // For C2, which has null frame pointers on x86/amd64/aarch64
              CodeBlob cb = VM.getVM().getCodeCache().findBlob(curFrame.getPC());
              Address sp = curFrame.getSP();
              if (Assert.ASSERTS_ENABLED) {
                Assert.that(cb.getFrameSize() > 0, "CodeBlob must have non-zero frame size");
              }
              annoPanel.addAnnotation(new Annotation(sp,
                                                     sp.addOffsetTo(cb.getFrameSize()),
                                                     anno));
            }

            // Add interpreter frame annotations
            if (curFrame.isInterpretedFrame()) {
              annoPanel.addAnnotation(new Annotation(curFrame.addressOfInterpreterFrameExpressionStack(),
                                                     curFrame.addressOfInterpreterFrameTOS(),
                                                     "Interpreter expression stack"));
              Address monBegin = curFrame.interpreterFrameMonitorBegin().address();
              Address monEnd = curFrame.interpreterFrameMonitorEnd().address();
              if (!monBegin.equals(monEnd)) {
                  annoPanel.addAnnotation(new Annotation(monBegin, monEnd,
                                                         "BasicObjectLocks"));
              }
              if (interpreterFrameMethod != null) {
                // The offset is just to get the right stack slots highlighted in the output
                int offset = 1;
                annoPanel.addAnnotation(new Annotation(curFrame.addressOfInterpreterFrameLocal(offset),
                                                       curFrame.addressOfInterpreterFrameLocal((int) interpreterFrameMethod.getMaxLocals() + offset),
                                                       "Interpreter locals area for frame with SP = " + curFrame.getSP()));
              }
              String methodAnno = "Interpreter frame Method*";
              if (interpreterFrameMethod == null) {
                methodAnno += " (BAD OOP)";
              }
              Address a = curFrame.addressOfInterpreterFrameMethod();
              annoPanel.addAnnotation(new Annotation(a, a.addOffsetTo(addressSize), methodAnno));
              a = curFrame.addressOfInterpreterFrameCPCache();
              annoPanel.addAnnotation(new Annotation(a, a.addOffsetTo(addressSize), "Interpreter constant pool cache"));
            }

            RegisterMap rm = (RegisterMap) vf.getRegisterMap().clone();
            if (!shouldSkipOopMaps) {
              try {
                curFrame.oopsDo(new AddressVisitor() {
                    public void visitAddress(Address addr) {
                      if (Assert.ASSERTS_ENABLED) {
                        Assert.that(addr.andWithMask(VM.getVM().getAddressSize() - 1) == null,
                                    "Address " + addr + "should have been aligned");
                      }
                      OopHandle handle = addr.getOopHandleAt(0);
                      addAnnotation(addr, handle);
                    }

                    public void visitCompOopAddress(Address addr) {
                      if (Assert.ASSERTS_ENABLED) {
                        Assert.that(addr.andWithMask(VM.getVM().getAddressSize() - 1) == null,
                                    "Address " + addr + "should have been aligned");
                      }
                      OopHandle handle = addr.getCompOopHandleAt(0);
                      addAnnotation(addr, handle);
                    }

                    public void addAnnotation(Address addr, OopHandle handle) {
                      // Check contents
                      String anno = "null oop";
                      if (handle != null) {
                        // Find location
                        CollectedHeap collHeap = VM.getVM().getUniverse().heap();
                        boolean bad = true;
                        anno = "BAD OOP";
                        if (collHeap instanceof GenCollectedHeap) {
                          GenCollectedHeap heap = (GenCollectedHeap) collHeap;
                          for (int i = 0; i < heap.nGens(); i++) {
                            if (heap.getGen(i).isIn(handle)) {
                              if (i == 0) {
                                anno = "NewGen ";
                              } else if (i == 1) {
                                anno = "OldGen ";
                              } else {
                                anno = "Gen " + i + " ";
                              }
                              bad = false;
                              break;
                            }
                          }

                        } else if (collHeap instanceof G1CollectedHeap) {
                          G1CollectedHeap heap = (G1CollectedHeap)collHeap;
                          HeapRegion region = heap.hrm().getByAddress(handle);

                          if (region.isFree()) {
                            anno = "Free ";
                            bad = false;
                          } else if (region.isYoung()) {
                            anno = "Young ";
                            bad = false;
                          } else if (region.isHumongous()) {
                            anno = "Humongous ";
                            bad = false;
                          } else if (region.isPinned()) {
                            anno = "Pinned ";
                            bad = false;
                          } else if (region.isOld()) {
                            anno = "Old ";
                            bad = false;
                          }
                        } else if (collHeap instanceof ParallelScavengeHeap) {
                          ParallelScavengeHeap heap = (ParallelScavengeHeap) collHeap;
                          if (heap.youngGen().isIn(handle)) {
                            anno = "PSYoungGen ";
                            bad = false;
                          } else if (heap.oldGen().isIn(handle)) {
                            anno = "PSOldGen ";
                            bad = false;
                          }
                        } else if (collHeap instanceof EpsilonHeap) {
                          anno = "Epsilon ";
                          bad = false;
                        } else if (collHeap instanceof ShenandoahHeap) {
                          ShenandoahHeap heap = (ShenandoahHeap) collHeap;
                          anno = "ShenandoahHeap ";
                          bad = false;
                        } else if (collHeap instanceof ZCollectedHeap) {
                          ZCollectedHeap heap = (ZCollectedHeap) collHeap;
                          anno = "ZHeap ";
                          bad = false;
                        } else {
                          // Optimistically assume the oop isn't bad
                          anno = "[Unknown generation] ";
                          bad = false;
                        }

                        if (!bad) {
                          try {
                            Oop oop = VM.getVM().getObjectHeap().newOop(handle);
                            if (oop instanceof Instance) {
                                // Java-level objects always have workable names
                              anno = anno + oop.getKlass().getName().asString();
                            } else {
                              ByteArrayOutputStream bos = new ByteArrayOutputStream();
                              Oop.printOopValueOn(oop, new PrintStream(bos));
                              anno = anno + bos.toString();
                            }
                          }
                          catch (AddressException e) {
                            anno += "CORRUPT OOP";
                          }
                          catch (NullPointerException e) {
                            anno += "CORRUPT OOP (null pointer)";
                          }
                        }
                      }

                      annoPanel.addAnnotation(new Annotation(addr, addr.addOffsetTo(addressSize), anno));
                    }
                  }, rm);
              } catch (Exception e) {
                System.err.println("Error while performing oopsDo for frame " + curFrame);
                e.printStackTrace();
              }
            }

            vf = nextVFrame;
          }

          // This used to paint as we walked the frames. This caused the display to be refreshed
          // enough to be annoying on remote displays. It also would cause the annotations to
          // be displayed in varying order which caused some annotations to overwrite others
          // depending on the races between painting and adding annotations. This latter problem
          // still exists to some degree but moving this code here definitely seems to reduce it
          annoPanel.makeVisible(startAddr);
          annoPanel.repaint();
        }
      });
  }

  // Attach to existing JVMDebugger, which should be already attached to a core/process.
  private void attach(JVMDebugger d) {
    attached = true;
    showThreadsDialog();
  }

  /** NOTE we are in a different thread here than either the main
      thread or the Swing/AWT event handler thread, so we must be very
      careful when creating or removing widgets */
  private void attach(int pid) {
    this.pid = pid;
    // Try to attach to this process
    Runnable remover = new Runnable() {
          public void run() {
            attachWaitDialog.setVisible(false);
            desktop.remove(attachWaitDialog);
            attachWaitDialog = null;
          }
      };

    try {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            JOptionPane pane = new JOptionPane("Attaching to process " + pid + ", please wait...", JOptionPane.INFORMATION_MESSAGE);
            pane.setOptions(new Object[] {});
            attachWaitDialog = pane.createInternalFrame(desktop, "Attaching to Process");
            attachWaitDialog.show();
          }
        });

      // FIXME: display exec'd debugger's output messages during this
      // lengthy call
      agent.attach(pid);
      if (agent.getDebugger().hasConsole()) {
        showDbgConsoleMenuItem.setEnabled(true);
      }
      attached = true;
      SwingUtilities.invokeLater(remover);
    }
    catch (DebuggerException e) {
      SwingUtilities.invokeLater(remover);
      final String errMsg = formatMessage(e.getMessage(), 80);
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            setMenuItemsEnabled(attachMenuItems, true);
            JOptionPane.showInternalMessageDialog(desktop,
                                                  "Unable to connect to process ID " + pid + ":\n\n" + errMsg,
                                                  "Unable to Connect",
                                                  JOptionPane.WARNING_MESSAGE);
          }
        });
      agent.detach();
      return;
    }

    // OK, the VM should be available. Create the Threads dialog.
    showThreadsDialog();
  }

  /** NOTE we are in a different thread here than either the main
      thread or the Swing/AWT event handler thread, so we must be very
      careful when creating or removing widgets */
  private void attach(final String executablePath, final String corePath) {
    // Try to open this core file
    Runnable remover = new Runnable() {
          public void run() {
            attachWaitDialog.setVisible(false);
            desktop.remove(attachWaitDialog);
            attachWaitDialog = null;
          }
      };

    try {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            JOptionPane pane = new JOptionPane("Opening core file, please wait...", JOptionPane.INFORMATION_MESSAGE);
            pane.setOptions(new Object[] {});
            attachWaitDialog = pane.createInternalFrame(desktop, "Opening Core File");
            attachWaitDialog.show();
          }
        });

      // FIXME: display exec'd debugger's output messages during this
      // lengthy call
      agent.attach(executablePath, corePath);
      if (agent.getDebugger().hasConsole()) {
        showDbgConsoleMenuItem.setEnabled(true);
      }
      attached = true;
      SwingUtilities.invokeLater(remover);
    }
    catch (DebuggerException e) {
      SwingUtilities.invokeLater(remover);
      final String errMsg = formatMessage(e.getMessage(), 80);
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            setMenuItemsEnabled(attachMenuItems, true);
            JOptionPane.showInternalMessageDialog(desktop,
                                                  "Unable to open core file\n" + corePath + ":\n\n" + errMsg,
                                                  "Unable to Open Core File",
                                                  JOptionPane.WARNING_MESSAGE);
          }
        });
      agent.detach();
      return;
    }

    // OK, the VM should be available. Create the Threads dialog.
    showThreadsDialog();
  }

  /** NOTE we are in a different thread here than either the main
      thread or the Swing/AWT event handler thread, so we must be very
      careful when creating or removing widgets */
  private void connect(final String debugServerName) {
    // Try to open this core file
    Runnable remover = new Runnable() {
          public void run() {
            attachWaitDialog.setVisible(false);
            desktop.remove(attachWaitDialog);
            attachWaitDialog = null;
          }
      };

    try {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            JOptionPane pane = new JOptionPane("Connecting to debug server, please wait...", JOptionPane.INFORMATION_MESSAGE);
            pane.setOptions(new Object[] {});
            attachWaitDialog = pane.createInternalFrame(desktop, "Connecting to Debug Server");
            attachWaitDialog.show();
          }
        });

      agent.attach(debugServerName);
      if (agent.getDebugger().hasConsole()) {
        showDbgConsoleMenuItem.setEnabled(true);
      }
      attached = true;
      SwingUtilities.invokeLater(remover);
    }
    catch (DebuggerException e) {
      SwingUtilities.invokeLater(remover);
      final String errMsg = formatMessage(e.getMessage(), 80);
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            setMenuItemsEnabled(attachMenuItems, true);
            JOptionPane.showInternalMessageDialog(desktop,
                                                  "Unable to connect to machine \"" + debugServerName + "\":\n\n" + errMsg,
                                                  "Unable to Connect",
                                                  JOptionPane.WARNING_MESSAGE);
          }
        });
      agent.detach();
      return;
    }

    // OK, the VM should be available. Create the Threads dialog.
    showThreadsDialog();
  }

  private void detachDebugger() {
    if (!attached) {
      return;
    }
    agent.detach();
    attached = false;
  }

  private void detach() {
    detachDebugger();
    attachWaitDialog = null;
    threadsFrame = null;
    consoleFrame = null;
    setMenuItemsEnabled(attachMenuItems, true);
    setMenuItemsEnabled(detachMenuItems, false);
    toolsMenu.setEnabled(false);
    showDbgConsoleMenuItem.setEnabled(false);
    // FIXME: is this sufficient, or will I have to do anything else
    // to the components to kill them off? What about WorkerThreads?
    desktop.removeAll();
    desktop.invalidate();
    desktop.validate();
    desktop.repaint();
  }

  /** NOTE that this is called from another thread than the main or
      Swing thread and we have to be careful about synchronization */
  private void showThreadsDialog() {
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          threadsFrame = new JInternalFrame("Java Threads");
          threadsFrame.setResizable(true);
          threadsFrame.setIconifiable(true);
          JavaThreadsPanel threadsPanel = new JavaThreadsPanel();
          threadsPanel.addPanelListener(HSDB.this);
          threadsFrame.getContentPane().add(threadsPanel);
          threadsFrame.setSize(500, 300);
          threadsFrame.pack();
          desktop.add(threadsFrame);
          GraphicsUtilities.moveToInContainer(threadsFrame, 0.75f, 0.25f, 0, 20);
          threadsFrame.show();
          setMenuItemsEnabled(attachMenuItems, false);
          setMenuItemsEnabled(detachMenuItems, true);
          toolsMenu.setEnabled(true);
          VM.registerVMInitializedObserver(new Observer() {
              public void update(Observable o, Object data) {
                computeRevPtrsMenuItem.setEnabled(true);
              }
            });
        }
      });
  }

  private void showObjectHistogram() {
    sun.jvm.hotspot.oops.ObjectHistogram histo = new sun.jvm.hotspot.oops.ObjectHistogram();
    ObjectHistogramCleanupThunk cleanup =
      new ObjectHistogramCleanupThunk(histo);
    doHeapIteration("Object Histogram",
                    "Generating histogram...",
                    histo,
                    cleanup);
  }

  class ObjectHistogramCleanupThunk implements CleanupThunk {
    sun.jvm.hotspot.oops.ObjectHistogram histo;

    ObjectHistogramCleanupThunk(sun.jvm.hotspot.oops.ObjectHistogram histo) {
      this.histo = histo;
    }

    public void heapIterationComplete() {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            JInternalFrame histoFrame = new JInternalFrame("Object Histogram");
            histoFrame.setResizable(true);
            histoFrame.setClosable(true);
            histoFrame.setIconifiable(true);
            histoFrame.getContentPane().setLayout(new BorderLayout());
            ObjectHistogramPanel panel = new ObjectHistogramPanel(histo);
            panel.addPanelListener(HSDB.this);
            histoFrame.getContentPane().add(panel);
            desktop.add(histoFrame);
            GraphicsUtilities.reshapeToAspectRatio(histoFrame, 4.0f / 3.0f, 0.6f,
                                       histoFrame.getParent().getSize());
            GraphicsUtilities.centerInContainer(histoFrame);
            histoFrame.show();
          }
        });
    }
  }

  public void showObjectsOfType(Klass type) {
    FindObjectByType finder = new FindObjectByType(type);
    FindObjectByTypeCleanupThunk cleanup =
      new FindObjectByTypeCleanupThunk(finder);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    type.printValueOn(new PrintStream(bos));
    String typeName = bos.toString();
    doHeapIteration("Show Objects Of Type",
                    "Finding instances of \"" + typeName + "\"",
                    finder,
                    cleanup);
  }

  class FindObjectByTypeCleanupThunk implements CleanupThunk {
    FindObjectByType finder;

    FindObjectByTypeCleanupThunk(FindObjectByType finder) {
      this.finder = finder;
    }

    public void heapIterationComplete() {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            JInternalFrame finderFrame = new JInternalFrame("Show Objects of Type");
            finderFrame.getContentPane().setLayout(new BorderLayout());
            finderFrame.setResizable(true);
            finderFrame.setClosable(true);
            finderFrame.setIconifiable(true);
            ObjectListPanel panel = new ObjectListPanel(finder.getResults(),
                                                        new HeapProgress("Reverse Pointers Analysis"));
            panel.addPanelListener(HSDB.this);
            finderFrame.getContentPane().add(panel);
            desktop.add(finderFrame);
            GraphicsUtilities.reshapeToAspectRatio(finderFrame, 4.0f / 3.0f, 0.6f,
                                       finderFrame.getParent().getSize());
            GraphicsUtilities.centerInContainer(finderFrame);
            finderFrame.show();
          }
        });
    }
  }

  private void showDebuggerConsole() {
    if (consoleFrame == null) {
      consoleFrame = new JInternalFrame("Debugger Console");
      consoleFrame.setResizable(true);
      consoleFrame.setClosable(true);
      consoleFrame.setIconifiable(true);
      consoleFrame.getContentPane().setLayout(new BorderLayout());
      consoleFrame.getContentPane().add(new DebuggerConsolePanel(agent.getDebugger()), BorderLayout.CENTER);
      GraphicsUtilities.reshapeToAspectRatio(consoleFrame, 5.0f, 0.9f, desktop.getSize());
    }
    if (consoleFrame.getParent() == null) {
      desktop.add(consoleFrame);
    }
    consoleFrame.setVisible(true);
    consoleFrame.show();
    consoleFrame.getContentPane().getComponent(0).requestFocus();
  }

  private void showConsole() {
      CommandProcessor.DebuggerInterface di = new CommandProcessor.DebuggerInterface() {
              public HotSpotAgent getAgent() {
                  return agent;
              }
              public boolean isAttached() {
                  return attached;
              }
              public void attach(int pid) {
                  HSDB.this.attach(pid);
              }
              public void attach(String java, String core) {
              }
              public void attach(String debugServerName) {
                  HSDB.this.connect(debugServerName);
              }
              public void detach() {
                  detachDebugger();
              }
              public void reattach() {
                  if (attached) {
                      detachDebugger();
                  }
                  if (pid != -1) {
                      attach(pid);
                  } else if (debugServerName != null) {
                      connect(debugServerName);
                  } else {
                      attach(execPath, coreFilename);
                  }
              }
          };

      showPanel("Command Line", new CommandProcessorPanel(new CommandProcessor(di, null, null, null)));
  }

  private void showFindPanel() {
    showPanel("Find Pointer", new FindPanel());
  }

  private void showFindInHeapPanel() {
    showPanel("Find Address In Heap", new FindInHeapPanel());
  }

  private void showFindInCodeCachePanel() {
    showPanel("Find Address In Code Cache", new FindInCodeCachePanel());
  }

  private void showHeapParametersPanel() {
    showPanel("Heap Parameters", new HeapParametersPanel());
  }

  public void showThreadInfo(final JavaThread thread) {
    showPanel("Info for " + thread.getThreadName(), new ThreadInfoPanel(thread));
  }

  public void showJavaStackTrace(final JavaThread thread) {
    JavaStackTracePanel jstp = new JavaStackTracePanel();
    showPanel("Java stack trace for " + thread.getThreadName(), jstp);
    jstp.setJavaThread(thread);
  }

  private void showDeadlockDetectionPanel() {
    showPanel("Deadlock Detection", new DeadlockDetectionPanel());
  }

  private void showMonitorCacheDumpPanel() {
    showPanel("Monitor Cache Dump", new MonitorCacheDumpPanel());
  }

  public void showClassBrowser() {
    final JInternalFrame progressFrame = new JInternalFrame("Class Browser");
    progressFrame.setResizable(true);
    progressFrame.setClosable(true);
    progressFrame.setIconifiable(true);
    progressFrame.getContentPane().setLayout(new BorderLayout());
    final ProgressBarPanel bar = new ProgressBarPanel("Generating class list ..");
    bar.setIndeterminate(true);
    progressFrame.getContentPane().add(bar, BorderLayout.CENTER);
    desktop.add(progressFrame);
    progressFrame.pack();
    GraphicsUtilities.centerInContainer(progressFrame);
    progressFrame.show();

    workerThread.invokeLater(new Runnable() {
                                public void run() {
                                   HTMLGenerator htmlGen = new HTMLGenerator();
                                   InstanceKlass[] klasses = SystemDictionaryHelper.getAllInstanceKlasses();
                                   final String htmlText = htmlGen.genHTMLForKlassNames(klasses);
                                   SwingUtilities.invokeLater(new Runnable() {
                                      public void run() {
                                         JInternalFrame cbFrame = new JInternalFrame("Class Browser");
                                         cbFrame.getContentPane().setLayout(new BorderLayout());
                                         cbFrame.setResizable(true);
                                         cbFrame.setClosable(true);
                                         cbFrame.setIconifiable(true);
                                         ClassBrowserPanel cbPanel = new ClassBrowserPanel();
                                         cbFrame.getContentPane().add(cbPanel, BorderLayout.CENTER);
                                         desktop.remove(progressFrame);
                                         desktop.repaint();
                                         desktop.add(cbFrame);
                                         GraphicsUtilities.reshapeToAspectRatio(cbFrame, 1.25f, 0.85f,
                                                                      cbFrame.getParent().getSize());
                                         cbFrame.show();
                                         cbPanel.setClassesText(htmlText);
                                      }
                                   });
                                }
                             });
  }

  public void showCodeViewer() {
    showPanel("Code Viewer", new CodeViewerPanel(), 1.25f, 0.85f);
  }

  public void showCodeViewer(final Address address) {
    final CodeViewerPanel panel = new CodeViewerPanel();
    showPanel("Code Viewer", panel, 1.25f, 0.85f);
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          panel.viewAddress(address);
        }
      });

  }

  public void showMemoryViewer() {
    showPanel("Memory Viewer", new MemoryViewer(agent.getDebugger(), agent.getTypeDataBase().getAddressSize() == 8));
  }

  public void showCommandLineFlags() {
    showPanel("Command Line Flags", new VMFlagsPanel());
  }

  public void showVMVersion() {
    showPanel("VM Version Info", new VMVersionInfoPanel());
  }

  public void showSystemProperties() {
    showPanel("System Properties", new SysPropsPanel());
  }

  private void showPanel(String name, JPanel panel) {
    showPanel(name, panel, 5.0f / 3.0f, 0.4f);
  }

  private void showPanel(String name, JPanel panel, float aspectRatio, float fillRatio) {
    JInternalFrame frame = new JInternalFrame(name);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.setResizable(true);
    frame.setClosable(true);
    frame.setIconifiable(true);
    frame.setMaximizable(true);
    frame.getContentPane().add(panel, BorderLayout.CENTER);
    desktop.add(frame);
    GraphicsUtilities.reshapeToAspectRatio(frame, aspectRatio, fillRatio, frame.getParent().getSize());
    GraphicsUtilities.randomLocation(frame);
    frame.show();
    if (panel instanceof SAPanel) {
      ((SAPanel)panel).addPanelListener(this);
    }
  }

  //--------------------------------------------------------------------------------
  // Framework for heap iteration with progress bar
  //

  interface CleanupThunk {
    public void heapIterationComplete();
  }

  class HeapProgress implements HeapProgressThunk {
    private JInternalFrame frame;
    private ProgressBarPanel bar;
    private String windowTitle;
    private String progressBarTitle;
    private CleanupThunk cleanup;

    HeapProgress(String windowTitle) {
      this(windowTitle, "Percentage of heap visited", null);
    }

    HeapProgress(String windowTitle, String progressBarTitle) {
      this(windowTitle, progressBarTitle, null);
    }

    HeapProgress(String windowTitle, String progressBarTitle, CleanupThunk cleanup) {
      this.windowTitle = windowTitle;
      this.progressBarTitle = progressBarTitle;
      this.cleanup = cleanup;
    }

    public void heapIterationFractionUpdate(final double fractionOfHeapVisited) {
      if (frame == null) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              frame = new JInternalFrame(windowTitle);
              frame.setResizable(true);
              frame.setIconifiable(true);
              frame.getContentPane().setLayout(new BorderLayout());
              bar = new ProgressBarPanel(progressBarTitle);
              frame.getContentPane().add(bar, BorderLayout.CENTER);
              desktop.add(frame);
              frame.pack();
              GraphicsUtilities.constrainToSize(frame, frame.getParent().getSize());
              GraphicsUtilities.centerInContainer(frame);
              frame.show();
            }
          });
      }

      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            bar.setValue(fractionOfHeapVisited);
          }
        });
    }

    public void heapIterationComplete() {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            desktop.remove(frame);
            desktop.repaint();
            if (VM.getVM().getRevPtrs() != null) {
              // Ended up computing reverse pointers as a side-effect
              computeRevPtrsMenuItem.setEnabled(false);
            }
          }
        });

      if (cleanup != null) {
        cleanup.heapIterationComplete();
      }
    }
  }

  class VisitHeap implements Runnable {
    HeapVisitor visitor;

    VisitHeap(HeapVisitor visitor) {
      this.visitor = visitor;
    }

    public void run() {
      VM.getVM().getObjectHeap().iterate(visitor);
    }
  }

  private void doHeapIteration(String frameTitle,
                               String progressBarText,
                               HeapVisitor visitor,
                               CleanupThunk cleanup) {
    sun.jvm.hotspot.oops.ObjectHistogram histo = new sun.jvm.hotspot.oops.ObjectHistogram();
    HeapProgress progress = new HeapProgress(frameTitle,
                                             progressBarText,
                                             cleanup);
    HeapVisitor progVisitor = new ProgressiveHeapVisitor(visitor, progress);
    workerThread.invokeLater(new VisitHeap(progVisitor));
  }

  //--------------------------------------------------------------------------------
  // Stack trace helper
  //

  private static JavaVFrame getLastJavaVFrame(JavaThread cur) {
    RegisterMap regMap = cur.newRegisterMap(true);
    sun.jvm.hotspot.runtime.Frame f = cur.getCurrentFrameGuess();
    if (f == null) return null;
    boolean imprecise = true;
    if (f.isInterpretedFrame() && !f.isInterpretedFrameValid()) {
      System.err.println("Correcting for invalid interpreter frame");
      f = f.sender(regMap);
      imprecise = false;
    }
    VFrame vf = VFrame.newVFrame(f, regMap, cur, true, imprecise);
    if (vf == null) {
      System.err.println(" (Unable to create vframe for topmost frame guess)");
      return null;
    }
    if (vf.isJavaFrame()) {
      return (JavaVFrame) vf;
    }
    return (JavaVFrame) vf.javaSender();
  }

  // Internal routine for debugging
  private static void dumpStack(JavaThread cur) {
    RegisterMap regMap = cur.newRegisterMap(true);
    sun.jvm.hotspot.runtime.Frame f = cur.getCurrentFrameGuess();
    PrintStream tty = System.err;
    while (f != null) {
      tty.print("Found ");
           if (f.isInterpretedFrame()) { tty.print("interpreted"); }
      else if (f.isCompiledFrame())    { tty.print("compiled"); }
      else if (f.isEntryFrame())       { tty.print("entry"); }
      else if (f.isNativeFrame())      { tty.print("native"); }
      else if (f.isRuntimeFrame())     { tty.print("runtime"); }
      else { tty.print("external"); }
      tty.print(" frame with PC = " + f.getPC() + ", SP = " + f.getSP() + ", FP = " + f.getFP());
      if (f.isSignalHandlerFrameDbg()) {
        tty.print(" (SIGNAL HANDLER)");
      }
      tty.println();

      if (!f.isFirstFrame()) {
        f = f.sender(regMap);
      } else {
        f = null;
      }
    }
  }

  //--------------------------------------------------------------------------------
  // Component utilities
  //

  private static JMenuItem createMenuItem(String name, ActionListener l) {
    JMenuItem item = new JMenuItem(name);
    item.addActionListener(l);
    return item;
  }

  /** Punctuates the given string with \n's where necessary to not
      exceed the given number of characters per line. Strips
      extraneous whitespace. */
  private String formatMessage(String message, int charsPerLine) {
    StringBuilder buf = new StringBuilder(message.length());
    StringTokenizer tokenizer = new StringTokenizer(message);
    int curLineLength = 0;
    while (tokenizer.hasMoreTokens()) {
      String tok = tokenizer.nextToken();
      if (curLineLength + tok.length() > charsPerLine) {
        buf.append('\n');
        curLineLength = 0;
      } else {
        if (curLineLength != 0) {
          buf.append(' ');
          ++curLineLength;
        }
      }
      buf.append(tok);
      curLineLength += tok.length();
    }
    return buf.toString();
  }

  private void setMenuItemsEnabled(java.util.List<JMenuItem> items, boolean enabled) {
    for (Iterator<JMenuItem> iter = items.iterator(); iter.hasNext(); ) {
      iter.next().setEnabled(enabled);
    }
  }
}
