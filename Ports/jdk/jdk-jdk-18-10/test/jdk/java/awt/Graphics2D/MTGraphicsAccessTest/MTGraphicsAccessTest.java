/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
  @test
  @bug 5089429 6982632 8145808
  @summary Checks that we don't crash if rendering operations and state
  changes are performed on a graphics context from different threads.

  @author Dmitri.Trembovetski@sun.com area=Graphics
  @run main MTGraphicsAccessTest
 */

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MTGraphicsAccessTest {

    // in seconds
    static final long STANDALONE_RUN_TIME = 20;
    static final long JTREG_RUN_TIME = 7;

    static boolean standaloneMode;
    static boolean allowExceptions = true;
    static long testRunTime;

    volatile boolean done;
    AtomicInteger stillRunning = new AtomicInteger(0);
    volatile int numexceptions;

    Graphics2D sharedGraphics;
    BufferedImage sharedBI =
            new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);

    static final Paint colors[] = {
        Color.red,
        new Color(0x7f, 0xff, 0x00, 0x7f),
        new GradientPaint(0,  0, Color.red,
                          50, 50, new Color(0x7f, 0xff, 0x00, 0x7f)),
    };
    static final Font fonts[] = {
        new Font("Dialog", Font.PLAIN, 12),
        new Font("Dialog", Font.BOLD, 16),
        new Font("Dialog", Font.ITALIC, 18),
    };
    static final AlphaComposite comps[] = {
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f),
        AlphaComposite.Src,
        AlphaComposite.Xor,
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f),
        null,
    };
    static final Stroke strokes[] = {
        new BasicStroke(),
        new BasicStroke(0.0f),
        new BasicStroke(2.0f),
        new BasicStroke(2.0f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_BEVEL),
        new BasicStroke(5.0f, BasicStroke.CAP_SQUARE,
                        BasicStroke.JOIN_ROUND),
        new BasicStroke(0.0f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND, 0,
                        new float[]{0,6,0,6}, 0),
    };
    static final AffineTransform transforms[] = {
        new AffineTransform(),
        AffineTransform.getRotateInstance(10.0),
        AffineTransform.getShearInstance(10.0, 4.0),
        AffineTransform.getScaleInstance(1.1, 1.2),
        AffineTransform.getScaleInstance(3.0, 2.0),
    };

    public MTGraphicsAccessTest() {
        BufferedImage bi =
            new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        sharedGraphics = (Graphics2D)bi.getGraphics();

        done = false;
        numexceptions = 0;

        for (int i = 0; i < (standaloneMode ? stateChangers.length : 3); i++) {
            (new TesterThread(stateChangers[i])).start();
        }
        for (int i = 0; i < (standaloneMode ? renderTests.length : 5); i++) {
            (new TesterThread(renderTests[i])).start();
        }

        mysleep(testRunTime);
        done = true;
        while (stillRunning.get() > 0) { mysleep(500); }

        if (numexceptions == 0) {
            System.err.println("Test passed");
        } else if (!allowExceptions) {
            throw new RuntimeException("Test failed with "+
                                       numexceptions+" exceptions");
        } else {
            System.err.println("Test finished with "+
                               numexceptions+" exceptions");
        }
    }

    private void mysleep(long time) {
        try {
            // add +/-5ms variance to increase randomness
            Thread.sleep(time + (long)(5 - Math.random()*10));
        } catch (InterruptedException e) {};
    }

    public static void usage(String message) {
        if (message != null) {
            System.err.println(message);
        }
        System.err.println("Usage: MTGraphicsAccessTest [-full] "+
            "[-time N/forever] [-help]");
        System.err.println(" -full: run full suite of tests "+
            "(default: limited number of tests is run)");
        System.err.println(" -time N: test duration in seconds/forever"+
            " (default: "+JTREG_RUN_TIME+"s for the short suite, "+
            STANDALONE_RUN_TIME+"s for the full suite)");
        System.err.println(" -help: print this help page");
        System.exit(1);
    }

    public static void main(String[] args) {
        boolean testRunSet = false;
        for (int i = 0; i < args.length; i++) {
            if ("-full".equals(args[i])) {
                standaloneMode = true;
                System.err.println("Running complete list of tests");
            } else if ("-noexc".equals(args[i])) {
                allowExceptions = false;
            } else if ("-time".equals(args[i])) {
                try {
                    String time = args[++i];
                    if ("forever".equals(time)) {
                        testRunTime = (Long.MAX_VALUE - 20)/1000;
                    } else {
                        testRunTime = 1000*Integer.parseInt(time);
                    }
                    testRunSet = true;
                } catch (NumberFormatException e) {
                    usage("Can't parse number of seconds: " + args[i]);
                } catch (ArrayIndexOutOfBoundsException e1) {
                    usage("Missing the 'seconds' argument for -time parameter");
                }
            } else if ("-help".equals(args[i])) {
                usage(null);
            } else {
                usage("Unknown argument:" + args[i]);
            }
        }

        if (!testRunSet) {
            testRunTime = 1000 *
                (standaloneMode ? STANDALONE_RUN_TIME : JTREG_RUN_TIME);
        }

        System.err.println("Approximate test run time: "+
             testRunTime/1000+" seconds");

        new MTGraphicsAccessTest();
    }

    class TesterThread extends Thread {
        Runnable testRunnable;

        public TesterThread(Runnable testRunnable) {
            stillRunning.incrementAndGet();
            this.testRunnable = testRunnable;
        }

        public void run() {
            try {
                while (!done) {
                    try {
                        testRunnable.run();
                        Thread.yield();
                    } catch (Throwable t) {
                        numexceptions++;
                        t.printStackTrace();
                    }
                }
            } finally {
                stillRunning.decrementAndGet();
            }
        }
    }

    final Runnable stateChangers[] = {
        new Runnable() {
            public void run() {
                sharedGraphics.setClip(10, 10, 30, 30);
                mysleep(10);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.setClip(10, 10, 30, 30);
                mysleep(10);
            }
        },
        new Runnable() {
            int c = 0;
            public void run() {
                sharedGraphics.setPaint(colors[c++ % colors.length]);
                mysleep(10);
            }
        },
        new Runnable() {
            boolean AA = false;
            public void run() {
                if (AA) {
                    sharedGraphics.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                } else {
                    sharedGraphics.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_OFF);
                }
                AA = !AA;
                mysleep(10);
            }
        },
        new Runnable() {
            int t = 0;
            public void run() {
                sharedGraphics.setTransform(
                    transforms[t++ % transforms.length]);
                mysleep(10);
            }
        },
        new Runnable() {
            int c = 0;
            public void run() {
                AlphaComposite comp = comps[c++ % comps.length];
                if (comp == null) {
                    sharedGraphics.setXORMode(Color.green);
                } else {
                    sharedGraphics.setComposite(comp);
                }
                mysleep(10);
            }
        },
        new Runnable() {
            int s = 0;
            public void run() {
                sharedGraphics.setStroke(strokes[s++ % strokes.length]);
                mysleep(10);
            }
        },
        new Runnable() {
            int f = 0;
            public void run() {
                sharedGraphics.setFont(fonts[f++ % fonts.length]);
                mysleep(10);
            }
        },
    };

    final Runnable renderTests[] = {
        new Runnable() {
            public void run() {
                sharedGraphics.drawLine(10, 10, 30, 30);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.drawLine(10, 10, 30, 30);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.drawRect(10, 10, 30, 30);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.fillRect(10, 10, 30, 30);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.drawString("Stuff", 10, 10);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.draw3DRect(10, 10, 30, 30, true);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.drawImage(sharedBI, 10, 10, null);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.fill3DRect(10, 10, 30, 30, false);
            }
        },
        // REMIND: copyArea doesn't work when transform is set..
        //          new Runnable() {
        //              public void run() {
        //                  sharedGraphics.copyArea(10, 10, 30, 30, 20, 20);
        //              }
        //          },
        new Runnable() {
            public void run() {
                sharedGraphics.drawRoundRect(10, 10, 30, 30, 20, 20);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.fillRoundRect(10, 10, 30, 30, 20, 20);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.drawArc(10, 10, 30, 30, 0, 90);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.fillArc(10, 10, 30, 30, 0, 90);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.drawOval(10, 10, 30, 30);
            }
        },
        new Runnable() {
            public void run() {
                sharedGraphics.fillOval(10, 10, 30, 30);
            }
        }
    };
}
