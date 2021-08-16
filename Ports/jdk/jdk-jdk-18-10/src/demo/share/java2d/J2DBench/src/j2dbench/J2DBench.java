/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.awt.Frame;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.text.SimpleDateFormat;
import java.util.Date;

import j2dbench.tests.GraphicsTests;
import j2dbench.tests.ImageTests;
import j2dbench.tests.MiscTests;
import j2dbench.tests.RenderTests;
import j2dbench.tests.PixelTests;
import j2dbench.tests.iio.IIOTests;
import j2dbench.tests.cmm.CMMTests;
import j2dbench.tests.text.TextConstructionTests;
import j2dbench.tests.text.TextMeasureTests;
import j2dbench.tests.text.TextRenderTests;
import j2dbench.tests.text.TextTests;

public class J2DBench {
    static Group progoptroot;

    static Option.Enable verbose;
    static Option.Enable printresults;

    static boolean looping = false;

    static JFrame guiFrame;

    static final SimpleDateFormat sdf =
        new SimpleDateFormat("MM.dd.yyyy 'at' HH:mm aaa z");

    public static void init() {
        progoptroot = new Group("prog", "Program Options");
        progoptroot.setHidden();

        verbose =
            new Option.Enable(progoptroot,
                              "verbose", "Verbose print statements",
                              false);
        printresults =
            new Option.Enable(progoptroot,
                              "printresults", "Print results after each run",
                              true);
    }

    public static void usage(int exitcode) {
        System.out.println("usage: java -jar J2DBench.jar "+
                           "[<optionname>=<value>]");
        System.out.println("    "+
                           "[-list] "+
                           "[-gui | -interactive] "+
                           "[-batch] "+
                           "[-noshow] "+
                           "[-nosave] "+
                           "[-report:[NMKAUOsmuna/]] "+
                           "[-usage | -help] "+
                           "\n    "+
                           "\n    "+
                           "[-loadopts | -loadoptions] <optfile> "+
                           "[-saveopts | -saveoptions] <optfile> "+
                           "\n    "+
                           "[-saveres | -saveresults] <resfile> "+
                           "[-appres | -appendresults] <resfile> "+
                           "\n    "+
                           "[-title] <title> "+
                           "[-desc | -description] <description> "+
                           "\n    "+
                           "[-loop] <duration> [-loopdef | -loopdefault] "+
                           "");
        System.out.println("        -list      "+
                           "List the option settings on stdout");
        System.out.println("        -gui       "+
                           "Run the program in interactive mode (launch GUI)");
        System.out.println("        -batch     "+
                           "Run the program in batch mode (do not launch GUI)");
        System.out.println("        -noshow    "+
                           "Do not show output on the screen (batch mode)");
        System.out.println("        -nosave    "+
                           "Do not show save results to a file (batch mode)");
        System.out.println("        -report    "+
                           "Rate format to report 'X per Y' (default u/s)");
        System.out.println("                   "+
                           "  N = report in single units or ops");
        System.out.println("                   "+
                           "  M = report in millions of units or ops");
        System.out.println("                   "+
                           "  K = report in thousands of units or ops");
        System.out.println("                   "+
                           "  A = (auto) M or K as needed");
        System.out.println("                   "+
                           "  U = units as defined by the operation");
        System.out.println("                   "+
                           "  O = operations");
        System.out.println("                   "+
                           "  s = report by whole seconds");
        System.out.println("                   "+
                           "  m = report by milliseconds");
        System.out.println("                   "+
                           "  u = report by microseconds");
        System.out.println("                   "+
                           "  n = report by nanoseconds");
        System.out.println("                   "+
                           "  a = (auto) milli/micro/nanoseconds as needed");
        System.out.println("                   "+
                           "  / = invert (N/sec or secs/N)");
        System.out.println("        -usage     "+
                           "Print out this usage message");
        System.out.println("        -saveres   "+
                           "Save the results to the indicated file");
        System.out.println("        -appres    "+
                           "Append the results to the indicated file");
        System.out.println("        -title     "+
                           "Use the title for the saved results");
        System.out.println("        -desc      "+
                           "Use the description for the saved results");
        System.out.println("        -loop      "+
                           "Loop for the specified duration"+
                           "\n                   "+
                           "Duration specified as :"+
                           "\n                     "+
                           "<days>d / <hours>h / <minutes>m / dd:hh:mm");
        System.out.println("        -loopdef   "+
                           "Loop for a default duration of 72 hours");

        System.exit(exitcode);
    }

    public static void main(String[] argv) {
        init();
        TestEnvironment.init();
        Result.init();

        Destinations.init();
        GraphicsTests.init();
        RenderTests.init();
        PixelTests.init();
        ImageTests.init();
        MiscTests.init();
        TextTests.init();
        TextRenderTests.init();
        TextMeasureTests.init();
        TextConstructionTests.init();
        IIOTests.init();
        CMMTests.init();

        boolean gui = true;
        boolean showresults = true;
        boolean saveresults = true;
        String resfilename = null;
        String title = null;
        String desc = null;
        boolean appendres = false;
        long requiredLoopTime = 259200000; // 72 hrs * 60 * 60 * 1000
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.equalsIgnoreCase("-list")) {
                PrintWriter pw = new PrintWriter(System.out);
                Node.Iterator iter = Group.root.getRecursiveChildIterator();
                while (iter.hasNext()) {
                    Node n = iter.next();
                    n.write(pw);
                }
                pw.flush();
            } else if (arg.equalsIgnoreCase("-gui") ||
                       arg.equalsIgnoreCase("-interactive"))
            {
                gui = true;
            } else if (arg.equalsIgnoreCase("-batch")) {
                gui = false;
            } else if (arg.equalsIgnoreCase("-noshow")) {
                showresults = false;
            } else if (arg.equalsIgnoreCase("-nosave")) {
                saveresults = false;
            } else if (arg.equalsIgnoreCase("-usage") ||
                       arg.equalsIgnoreCase("-help"))
            {
                usage(0);
            } else if (arg.equalsIgnoreCase("-loadoptions") ||
                       arg.equalsIgnoreCase("-loadopts"))
            {
                if (++i < argv.length) {
                    String file = argv[i];
                    String reason = loadOptions(file);
                    if (reason != null) {
                        System.err.println(reason);
                        System.exit(1);
                    }
                } else {
                    usage(1);
                }
            } else if (arg.equalsIgnoreCase("-saveoptions") ||
                       arg.equalsIgnoreCase("-saveopts"))
            {
                if (++i < argv.length) {
                    String file = argv[i];
                    String reason = saveOptions(file);
                    if (reason != null) {
                        System.err.println(reason);
                        System.exit(1);
                    }
                } else {
                    usage(1);
                }
            } else if (arg.equalsIgnoreCase("-saveresults") ||
                       arg.equalsIgnoreCase("-saveres") ||
                       arg.equalsIgnoreCase("-appendresults") ||
                       arg.equalsIgnoreCase("-appres"))
            {
                if (++i < argv.length) {
                    resfilename = argv[i];
                    appendres = arg.substring(0, 4).equalsIgnoreCase("-app");
                } else {
                    usage(1);
                }
            } else if (arg.equalsIgnoreCase("-title")) {
                if (++i < argv.length) {
                    title = argv[i];
                } else {
                    usage(1);
                }
            } else if (arg.equalsIgnoreCase("-desc") ||
                       arg.equalsIgnoreCase("-description"))
            {
                if (++i < argv.length) {
                    desc = argv[i];
                } else {
                    usage(1);
                }
            } else if (arg.equalsIgnoreCase("-loopdef") ||
                       arg.equalsIgnoreCase("-loopdefault"))
            {
                requiredLoopTime = 259200000; // 72 hrs * 60 * 60 * 1000
                J2DBench.looping = true;
            } else if (arg.equalsIgnoreCase("-loop")) {

                if (++i >= argv.length) {
                    usage(1);
                }

                J2DBench.looping = true;

                /*
                 * d or D    ->  Days
                 * h or H    ->  Hours
                 * m or M    ->  Minutes
                 * dd:hh:mm  ->  Days:Hours:Minutes
                 */

                if (argv[i].indexOf(":") >= 0) {

                    String[] values = argv[i].split(":");
                    int[] intVals = new int[3];

                    for(int j=0; j<values.length; j++) {
                        try {
                            intVals[j] = Integer.parseInt(values[j]);
                        } catch(Exception e) {}
                    }

                    System.out.println("\nLoop for " + intVals[0] +
                                       " days " + intVals[1] +
                                       " hours and " + intVals[2] + " minutes.\n");

                    requiredLoopTime = ((intVals[0] * 24 * 60 * 60) +
                                        (intVals[1] * 60 * 60) +
                                        (intVals[2] * 60)) * 1000;

                } else {

                    String type = argv[i].substring(argv[i].length() - 1);

                    int multiplyWith = 1;

                    if (type.equalsIgnoreCase("d")) {
                        multiplyWith = 24 * 60 * 60;
                    } else if (type.equalsIgnoreCase("h")) {
                        multiplyWith = 60 * 60;
                    } else if (type.equalsIgnoreCase("m")) {
                        multiplyWith = 60;
                    } else {
                        System.err.println("Invalid \"-loop\" option specified.");
                        usage(1);
                    }

                    int val = 1;
                    try {
                        val = Integer.parseInt(argv[i].substring(0, argv[i].length() - 1));
                    } catch(Exception e) {
                        System.err.println("Invalid \"-loop\" option specified.");
                        usage(1);
                    }

                    requiredLoopTime = val * multiplyWith * 1000;
                }

           } else if (arg.length() > 8 &&
                        arg.substring(0, 8).equalsIgnoreCase("-report:"))
           {
                String error = Result.parseRateOpt(arg.substring(8));
                if (error != null) {
                     System.err.println("Invalid rate: "+error);
                     usage(1);
                }
            } else {
                String reason = Group.root.setOption(arg);
                if (reason != null) {
                    System.err.println("Option "+arg+" ignored: "+reason);
                }
            }
        }
        if (verbose.isEnabled()) {
            Group.root.traverse(new Node.Visitor() {
                public void visit(Node node) {
                    System.out.println(node);
                }
            });
        }

        if (gui) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    startGUI();
                }
            });
        } else {

            long start = System.currentTimeMillis();

            int nLoopCount = 1;

            if (saveresults) {
                if (title == null) {
                    title = inputUserStr("title");
                }
                if (desc == null) {
                    desc = inputUserStr("description");
                }
            }

            PrintWriter writer = null;

            if (J2DBench.looping) {

                System.out.println("\nAbout to run tests for : " +
                                   (requiredLoopTime/1000) + " seconds.\n");

                if(resfilename != null) {

                    try {
                        String loopReportFileName =
                            resfilename.substring(0, resfilename.lastIndexOf(".xml"));
                        writer = new PrintWriter(
                            new FileWriter(loopReportFileName + "_Loop.html"));
                        writer.println("<html><head><title>" + title + "</title></head>");
                        writer.println("<body bgcolor=\"#ffffff\"><hr size=\"1\">");
                        writer.println("<center><h2>" + title + "</h2>");
                        writer.println("</center><hr size=\"1\"><br>");
                        writer.flush();
                    } catch(IOException ioe) {
                        ioe.printStackTrace();
                        System.err.println("\nERROR : Could not create Loop-Report. Exit");
                        System.exit(1);
                    }
                }
            }

            do {

                Date loopStart = new Date();
                if (J2DBench.looping) {
                    writer.println("<b>Loop # " + nLoopCount + "</b><br>");
                    writer.println("<b>Start : </b>" + sdf.format(loopStart) + "<br>");
                    writer.flush();
                }

                runTests(showresults);
                if (saveresults) {
                    if (resfilename != null) {
                        lastResults.setTitle(title);
                        lastResults.setDescription(desc);
                        String reason = saveResults(resfilename, appendres);
                        if (reason != null) {
                            System.err.println(reason);
                        }
                    } else {
                        saveResults(title, desc);
                    }
                }

                if (J2DBench.looping) {

                    Date loopEnd = new Date();

                    System.out.println("\n================================================================");
                    System.out.println("-- Completed Loop " + nLoopCount + " at " + sdf.format(loopEnd) + " --");
                    System.out.println("================================================================\n");

                    writer.println("<b>End : </b>" + sdf.format(loopEnd) + "<br>");
                    writer.println("<b>Duration </b>: " + (loopEnd.getTime() - loopStart.getTime())/1000 + " Seconds<br>");
                    writer.println("<b>Total : " + (loopEnd.getTime() - start)/1000 + " Seconds</b><br>");
                    writer.println("</center><hr size=\"1\">");
                    writer.flush();

                    if ((loopEnd.getTime() - start) > requiredLoopTime) {
                        break;
                    }

                    //Append results for looping - mode
                    appendres = true;

                    nLoopCount++;
                }

            } while(J2DBench.looping);

            if (J2DBench.looping) {
                writer.println("</html>");
                writer.flush();
                writer.close();
            }
        }
    }

    public static String loadOptions(String filename) {
        FileReader fr;
        try {
            fr = new FileReader(filename);
        } catch (FileNotFoundException e) {
            return "file "+filename+" not found";
        }
        return loadOptions(fr, filename);
    }

    public static String loadOptions(File file) {
        FileReader fr;
        try {
            fr = new FileReader(file);
        } catch (FileNotFoundException e) {
            return "file "+file.getPath()+" not found";
        }
        return loadOptions(fr, file.getPath());
    }

    public static String loadOptions(FileReader fr, String filename) {
        LineNumberReader lnr = new LineNumberReader(fr);
        Group.restoreAllDefaults();
        String line;
        try {
            while ((line = lnr.readLine()) != null) {
                String reason = Group.root.setOption(line);
                if (reason != null) {
                    System.err.println("Option "+line+
                                       " at line "+lnr.getLineNumber()+
                                       " ignored: "+reason);
                }
            }
        } catch (IOException e) {
            Group.restoreAllDefaults();
            return ("IO Error reading "+filename+
                    " at line "+lnr.getLineNumber());
        }
        return null;
    }

    public static String saveOptions(String filename) {
        return saveOptions(new File(filename));
    }

    public static String saveOptions(File file) {
        if (file.exists()) {
            if (!file.isFile()) {
                return "Cannot save options to a directory!";
            }
            int ret = JOptionPane.showOptionDialog
                (guiFrame,
                 new String[] {
                     "The file '"+file.getName()+"' already exists!",
                     "",
                     "Do you wish to overwrite this file?",
                 },
                 "File exists!",
                 JOptionPane.DEFAULT_OPTION,
                 JOptionPane.WARNING_MESSAGE,
                 null, new String[] {
                     "Overwrite",
                     "Cancel",
                 }, "Cancel");
            if (ret == 1) {
                return null;
            }
        }
        FileWriter fw;
        try {
            fw = new FileWriter(file);
        } catch (IOException e) {
            return "Error opening option file "+file.getPath();
        }
        return saveOptions(fw, file.getPath());
    }

    public static String saveOptions(FileWriter fw, String filename) {
        PrintWriter pw = new PrintWriter(fw);
        Group.writeAll(pw);
        return null;
    }

    public static JFileChooser theFC;
    public static JFileChooser getFileChooser() {
        if (theFC == null) {
            theFC = new JFileChooser(System.getProperty("user.dir"));
        }
        theFC.rescanCurrentDirectory();
        return theFC;
    }

    public static ResultSet lastResults;
    public static boolean saveOrDiscardLastResults() {
        if (lastResults != null) {
            int ret = JOptionPane.showConfirmDialog
                (guiFrame,
                 "The results of the last test will be "+
                 "discarded if you continue!  Do you want "+
                 "to save them?",
                 "Discard last results?",
                 JOptionPane.YES_NO_CANCEL_OPTION);
            if (ret == JOptionPane.CANCEL_OPTION) {
                return false;
            } else if (ret == JOptionPane.YES_OPTION) {
                if (saveResults()) {
                    lastResults = null;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public static String inputUserStr(String type) {
        return JOptionPane.showInputDialog("Enter a "+
                                           type+
                                           " for this result set:");
    }

    public static boolean saveResults() {
        return saveResults(inputUserStr("title"), inputUserStr("description"));
    }

    public static boolean saveResults(String title, String desc) {
        lastResults.setTitle(title);
        lastResults.setDescription(desc);
        JFileChooser fc = getFileChooser();
        int ret = fc.showSaveDialog(guiFrame);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            boolean append = false;
            if (file.exists()) {
                if (!file.isFile()) {
                    System.err.println("Cannot save results to a directory!");
                    return false;
                }
                ret = JOptionPane.showOptionDialog
                    (guiFrame,
                     new String[] {
                         "The file '"+file.getName()+"' already exists!",
                         "",
                         "Do you wish to overwrite or append to this file?",
                     },
                     "File exists!",
                     JOptionPane.DEFAULT_OPTION,
                     JOptionPane.WARNING_MESSAGE,
                     null, new String[] {
                         "Overwrite",
                         "Append",
                         "Cancel",
                     }, "Cancel");
                if (ret == 0) {
                    append = false;
                } else if (ret == 1) {
                    append = true;
                } else {
                    return false;
                }
            }
            String reason = saveResults(file, append);
            if (reason == null) {
                return true;
            } else {
                System.err.println(reason);
            }
        }
        return false;
    }

    public static String saveResults(String filename, boolean append) {
        FileWriter fw;
        try {
            fw = new FileWriter(filename, append);
        } catch (IOException e) {
            return "Error opening results file "+filename;
        }
        return saveResults(fw, filename, append);
    }

    public static String saveResults(File file, boolean append) {
        FileWriter fw;
        try {
            fw = new FileWriter(file, append);
        } catch (IOException e) {
            return "Error opening results file "+file.getName();
        }
        return saveResults(fw, file.getName(), append);
    }

    public static String saveResults(FileWriter fw, String filename,
                                     boolean append)
    {
        PrintWriter pw = new PrintWriter(fw);
        if (!append) {
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<!--For Entertainment Purposes Only-->");
        }
        pw.println();
        lastResults.write(pw);
        pw.flush();
        pw.close();
        return null;
    }

    public static void startGUI() {
        final JFrame f = new JFrame("J2DBench") {
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                pref.width = Math.max(pref.width, 800);
                pref.height = Math.max(pref.height, 600);
                return pref;
            }
        };
        guiFrame = f;
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.getContentPane().setLayout(new BorderLayout());
        f.getContentPane().add(Group.root.getJComponent(), BorderLayout.CENTER);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        JButton b = new JButton("Run Tests...");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!saveOrDiscardLastResults()) {
                    return;
                }
                if (verbose.isEnabled()) {
                    System.out.println(e);
                    System.out.println("running tests...");
                }
                new Thread(new Runnable() {
                    public void run() {
                        runTests(true);
                    }
                }).start();
                if (verbose.isEnabled()) {
                    System.out.println("done");
                }
            }
        });
        p.add(b);

        b = new JButton("Load Options");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = getFileChooser();
                int ret = fc.showOpenDialog(f);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String reason = loadOptions(fc.getSelectedFile());
                    if (reason != null) {
                        System.err.println(reason);
                    }
                }
            }
        });
        p.add(b);

        b = new JButton("Save Options");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = getFileChooser();
                int ret = fc.showSaveDialog(f);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String reason = saveOptions(fc.getSelectedFile());
                    if (reason != null) {
                        System.err.println(reason);
                    }
                }
            }
        });
        p.add(b);

        b = new JButton("Save Results");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (saveResults()) {
                    lastResults = null;
                }
            }
        });
        p.add(b);

        b = new JButton("Quit");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!saveOrDiscardLastResults()) {
                    return;
                }
                System.exit(0);
            }
        });
        p.add(b);

        f.getContentPane().add(p, BorderLayout.SOUTH);
        f.pack();
        f.setLocationRelativeTo(null);
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds().intersection(f.getBounds());
        f.setBounds(usable);
        f.setVisible(true);
    }

    public static void runTests(boolean showresults) {
        final TestEnvironment env = new TestEnvironment();
        Frame f = null;
        if (showresults) {
            f = new Frame("J2DBench test run");
            f.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    env.stop();
                }
            });
            f.add(env.getCanvas());
            f.pack();
            f.show();
        }
        for (int i = 0; i < 5; i++) {
            env.idle();
        }
        env.runAllTests();
        if (showresults) {
            f.hide();
            f.dispose();
        }
        lastResults = env.results;
        if (J2DBench.printresults.isEnabled()) {
            System.out.println();
        }
        System.out.println("All test results:");
        env.summarize();
        System.out.println();
    }
}
