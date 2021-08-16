/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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


/*
 * (C) Copyright IBM Corp. 2003, All Rights Reserved.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 */

package j2dbench.tests.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.NumericShaper;
import java.awt.font.TextAttribute;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;

import j2dbench.Destinations;
import j2dbench.Group;
import j2dbench.Node;
import j2dbench.Option;
import j2dbench.Option.ObjectList;
import j2dbench.Result;
import j2dbench.Test;
import j2dbench.TestEnvironment;

public abstract class TextTests extends Test {
    public static boolean hasGraphics2D;

    static {
        try {
            hasGraphics2D = (Graphics2D.class != null);
        } catch (NoClassDefFoundError e) {
        }
    }

    // core data
    static final int[] tlengths = {
        1, 2, 4, 8, 16, 32, 64, 128, 256, 512
    };

    static final String[] tscripts = {
        // german, vietnamese, surrogate, dingbats
        "english", "arabic", "greek", "hebrew", "hindi", "japanese", "korean", "thai",
        "english-arabic", "english-greek", "english-hindi", "english-arabic-hindi"
    };

    static final float[] fsizes = {
        1f, 6f, 8f, 10f, 12f, 12.5f, 13f, 13.5f, 16f, 20f, 36f, 72f, 128f
    };

    static final float[] fintsizes = {
        1f, 6f, 8f, 10f, 12f, 13f, 16f, 20f, 36f, 72f, 128f
    };

    // utilties
    static Float[] floatObjectList(float[] input) {
        Float[] result = new Float[input.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = new Float(input[i]);
        }
        return result;
    }

    static String[] floatStringList(float[] input) {
        return floatStringList("", input, "");
    }

    static String[] floatStringList(float[] input, String sfx) {
        return floatStringList("", input, sfx);
    }

    static String[] floatStringList(String pfx, float[] input, String sfx) {
        String[] result = new String[input.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = pfx + input[i] + sfx;
        }
        return result;
    }

    static String[] intStringList(int[] input) {
        return intStringList("", input, "");
    }

    static String[] intStringList(int[] input, String sfx) {
        return intStringList("", input, sfx);
    }

    static String[] intStringList(String pfx, int[] input, String sfx) {
        String[] result = new String[input.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = pfx + input[i] + sfx;
        }
        return result;
    }

    static final String[] txNames;
    static final String[] txDescNames;
    static final AffineTransform[] txList;
    static final Map[] maps;
    static {
        AffineTransform identity = new AffineTransform();
        AffineTransform sm_scale = AffineTransform.getScaleInstance(.5, .5);
        AffineTransform lg_scale = AffineTransform.getScaleInstance(2, 2);
        AffineTransform wide = AffineTransform.getScaleInstance(2, .8);
        AffineTransform tall = AffineTransform.getScaleInstance(.8, 2);
        AffineTransform x_trans = AffineTransform.getTranslateInstance(50, 0);
        AffineTransform y_trans = AffineTransform.getTranslateInstance(0, -30);
        AffineTransform xy_trans = AffineTransform.getTranslateInstance(50, -30);
        AffineTransform sm_rot = AffineTransform.getRotateInstance(Math.PI / 3);
        AffineTransform lg_rot = AffineTransform.getRotateInstance(Math.PI * 4 / 3);
        AffineTransform pi2_rot = AffineTransform.getRotateInstance(Math.PI / 2);
        AffineTransform x_shear = AffineTransform.getShearInstance(.4, 0);
        AffineTransform y_shear = AffineTransform.getShearInstance(0, -.4);
        AffineTransform xy_shear = AffineTransform.getShearInstance(.3, .3);
        AffineTransform x_flip = AffineTransform.getScaleInstance(-1, 1);
        AffineTransform y_flip = AffineTransform.getScaleInstance(1, -1);
        AffineTransform xy_flip = AffineTransform.getScaleInstance(-1, -1);
        AffineTransform w_rot = AffineTransform.getRotateInstance(Math.PI / 3);
        w_rot.scale(2, .8);
        AffineTransform w_y_shear = AffineTransform.getShearInstance(0, -.4);
        w_y_shear.scale(2, .8);
        AffineTransform w_r_trans = AffineTransform.getTranslateInstance(3, -7);
        w_r_trans.rotate(Math.PI / 3);
        w_r_trans.scale(2, .8);
        AffineTransform w_t_rot = AffineTransform.getRotateInstance(Math.PI / 3);
        w_t_rot.translate(3, -7);
        w_t_rot.scale(2, .8);
        AffineTransform w_y_s_r_trans = AffineTransform.getTranslateInstance(3, -7);
        w_y_s_r_trans.rotate(Math.PI / 3);
        w_y_s_r_trans.shear(0, -.4);
        w_y_s_r_trans.scale(2, .8);

        txNames = new String[] {
            "ident",
            "smsc", "lgsc", "wide", "tall",
            "xtrn", "ytrn", "xytrn",
            "srot", "lrot", "hrot",
            "xshr", "yshr", "xyshr",
            "flx", "fly", "flxy",
            "wr", "wys", "wrt",
            "wtr", "wysrt"
        };

        txDescNames = new String[] {
            "Identity",
            "Sm Scale", "Lg Scale", "Wide", "Tall",
            "X Trans", "Y Trans", "XY Trans",
            "Sm Rot", "Lg Rot", "PI/2 Rot",
            "X Shear", "Y Shear", "XY Shear",
            "FlipX", "FlipY", "FlipXY",
            "WRot", "WYShear", "WRTrans",
            "WTRot", "WYSRTrans"
        };

        txList = new AffineTransform[] {
            identity,
            sm_scale, lg_scale, wide, tall,
            x_trans, y_trans, xy_trans,
            sm_rot, lg_rot, pi2_rot,
            x_shear, y_shear, xy_shear,
            x_flip, y_flip, xy_flip,
            w_rot, w_y_shear, w_r_trans,
            w_t_rot, w_y_s_r_trans,
        };

        // maps
        HashMap fontMap = new HashMap();
        fontMap.put(TextAttribute.FONT, new Font("Dialog", Font.ITALIC, 18));

        HashMap emptyMap = new HashMap();

        HashMap simpleMap = new HashMap();
        simpleMap.put(TextAttribute.FAMILY, "Lucida Sans");
        simpleMap.put(TextAttribute.SIZE, new Float(14));
        simpleMap.put(TextAttribute.FOREGROUND, Color.blue);

        HashMap complexMap = new HashMap();
        complexMap.put(TextAttribute.FAMILY, "Serif");
        complexMap.put(TextAttribute.TRANSFORM, tall);
        complexMap.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        complexMap.put(TextAttribute.RUN_DIRECTION,
                       TextAttribute.RUN_DIRECTION_RTL);
        try {
            complexMap.put(TextAttribute.NUMERIC_SHAPING,
                           NumericShaper.getContextualShaper(NumericShaper.ALL_RANGES));
        } catch (NoSuchFieldError e) {
        }

        maps = new Map[] {
            fontMap,
            emptyMap,
            simpleMap,
            complexMap,
        };
    }

    static String getString(Object key, int len) {
        String keyString = key.toString();
        String[] strings = new String[4]; // leave room for index == 3 to return null
        int span = Math.min(32, len);
        int n = keyString.indexOf('-');
        if (n == -1) {
            strings[0] = getSimpleString(keyString);
        } else {
            strings[0] = getSimpleString(keyString.substring(0, n));
            int m = keyString.indexOf('-', n+1);
            if (m == -1) {
                strings[1] = getSimpleString(keyString.substring(n+1));
                // 2 to 1 ratio, short spans between 1 and 16 chars long
                span = Math.max(1, Math.min(16, len / 3));
            } else {
                strings[1] = getSimpleString(keyString.substring(n+1, m));
                strings[2] = getSimpleString(keyString.substring(m+1));
                span = Math.max(1, Math.min(16, len / 4));
            }
        }
        String s = "";
        int pos = 0;
        int strx = 0;
        while (s.length() < len) {
            String src;
            if (strings[strx] == null) {
                src = strings[0]; // use strings[0] twice for each other string
                strx = 0;
            } else {
                src = strings[strx++];
            }
            if (pos + span > src.length()) {
                pos = 0; // we know all strings are longer than span
            }
            s += src.substring(pos, pos+span);
            pos += span;
        }
        return s.substring(0, len);
    }


    static HashMap strcache = new HashMap(tscripts.length);
    private static String getSimpleString(Object key) {
        String s = (String)strcache.get(key);
        if (s == null) {
            String fname = "textdata/" + key + ".ut8.txt";
            try {
                InputStream is = TextTests.class.getResourceAsStream(fname);
                if (is == null) {
                    throw new IOException("Can't load resource " + fname);
                }
                BufferedReader r =
                    new BufferedReader(new InputStreamReader(is, "utf8"));
                StringBuffer buf = new StringBuffer(r.readLine());
                while (null != (s = r.readLine())) {
                    buf.append("  ");
                    buf.append(s);
                }
                s = buf.toString();
                if (s.charAt(0) == '\ufeff') {
                    s = s.substring(1);
                }
            }
            catch (IOException e) {
                s = "This is a dummy ascii string because " +
                    fname + " was not found.";
            }
            strcache.put(key, s);
        }
        return s;
    }

    static Group textroot;
    static Group txoptroot;
    static Group txoptdataroot;
    static Group txoptfontroot;
    static Group txoptgraphicsroot;
    static Group advoptsroot;

    static Option tlengthList;
    static Option tscriptList;
    static Option fnameList;
    static Option fstyleList;
    static Option fsizeList;
    static Option ftxList;
    static Option taaList;
    static Option tfmTog;
    static Option gaaTog;
    static Option gtxList;
    static Option gvstyList;
    static Option tlrunList;
    static Option tlmapList;

    // core is textlength, text script, font name/style/size/tx, frc

    // drawing
    //   drawString, drawChars, drawBytes, drawGlyphVector, TextLayout.draw, drawAttributedString
    // length of text
    //   1, 2, 4, 8, 16, 32, 64, 128, 256 chars
    // script of text
    //   simple: latin-1, japanese, arabic, hebrew, indic, thai, surrogate, dingbats
    //   mixed:  latin-1 + x  (1, 2, 3, 4 pairs)
    // font of text
    //   name (composite, not), style, size (6, 12, 18, 24, 30, 36, 42, 48, 54, 60), transform (full set)
    // text rendering hints
    //   aa, fm, gaa
    // graphics transform (full set)
    // (gv) gtx, gpos
    // (tl, as) num style runs
    //
    // querying/measuring
    //   ascent/descent/leading
    //   advance
    //   (gv) lb, vb, pb, glb, gvb, glb, gp, gjust, gmet, gtx
    //   (tl) bounds, charpos, cursor
    //
    // construction/layout
    //   (bidi) no controls, controls, styles
    //   (gv) createGV, layoutGV
    //   (tl) TL constructors
    //   (tm) line break

    public static void init() {
        textroot = new Group("text", "Text Benchmarks");
        textroot.setTabbed();

        txoptroot = new Group(textroot, "opts", "Text Options");
        txoptroot.setTabbed();

        txoptdataroot = new Group(txoptroot, "data", "Text Data");

        tlengthList = new Option.IntList(txoptdataroot, "tlength",
                                        "Text Length",
                                        tlengths,
                                        intStringList(tlengths),
                                        intStringList(tlengths, " chars"),
                                        0x10);
        ((Option.ObjectList) tlengthList).setNumRows(5);

        tscriptList = new Option.ObjectList(txoptdataroot, "tscript",
                                            "Text Script",
                                            tscripts,
                                            tscripts,
                                            tscripts,
                                            tscripts,
                                            0x1);
        ((Option.ObjectList) tscriptList).setNumRows(4);

        txoptfontroot = new Group(txoptroot, "font", "Font");

        fnameList = new FontOption(txoptfontroot, "fname", "Family Name");

        fstyleList = new Option.IntList(txoptfontroot, "fstyle",
                                        "Style",
                                        new int[] {
                                            Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD + Font.ITALIC,
                                        },
                                        new String[] {
                                            "plain", "bold", "italic", "bolditalic",
                                        },
                                        new String[] {
                                            "Plain", "Bold", "Italic", "Bold Italic",
                                        },
                                        0x1);

        float[] fsl = hasGraphics2D ? fsizes : fintsizes;
        fsizeList = new Option.ObjectList(txoptfontroot, "fsize",
                                          "Size",
                                          floatStringList(fsl),
                                          floatObjectList(fsl),
                                          floatStringList(fsl),
                                          floatStringList(fsl, "pt"),
                                          0x40);
        ((Option.ObjectList) fsizeList).setNumRows(5);

        if (hasGraphics2D) {
            ftxList = new Option.ObjectList(txoptfontroot, "ftx",
                                            "Transform",
                                            txDescNames,
                                            txList,
                                            txNames,
                                            txDescNames,
                                            0x1);
            ((Option.ObjectList) ftxList).setNumRows(6);

            txoptgraphicsroot = new Group(txoptroot, "graphics", "Graphics");

            String[] taaNames;
            Object[] taaHints;
            try {
                taaNames = new String[] {
                    "Off", "On",
                    "LCD_HRGB", "LCD_HBGR", "LCD_VRGB", "LCD_VBGR"
                };
                taaHints = new Object[] {
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR,
                };
            } catch (NoSuchFieldError e) {
                taaNames = new String[] {
                    "Off", "On"
                };
                taaHints = new Object[] {
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
                };
            }
            taaList = new Option.ObjectList(txoptgraphicsroot, "textaa",
                                            "Text AntiAlias",
                                            taaNames, taaHints,
                                            taaNames, taaNames,
                                            0x1);
            ((Option.ObjectList) taaList).setNumRows(6);
            // add special TextAAOpt for backwards compatibility with
            // older options files
            new TextAAOpt();

            tfmTog = new Option.Toggle(txoptgraphicsroot, "tfm",
                                       "Fractional Metrics", Option.Toggle.Off);
            gaaTog = new Option.Toggle(txoptgraphicsroot, "gaa",
                                       "Graphics AntiAlias", Option.Toggle.Off);

            gtxList = new Option.ObjectList(txoptgraphicsroot, "gtx",
                                            "Transform",
                                            txDescNames,
                                            txList,
                                            txNames,
                                            txDescNames,
                                            0x1);
            ((Option.ObjectList) gtxList).setNumRows(6);

            advoptsroot = new Group(txoptroot, "advopts", "Advanced Options");
            gvstyList = new Option.IntList(advoptsroot, "gvstyle", "Style",
                                           new int[] { 0, 1, 2, 3 },
                                           new String[] { "std", "wave", "twist", "circle" },
                                           new String[] { "Standard",
                                                          "Positions adjusted",
                                                          "Glyph angles adjusted",
                                                          "Layout to circle"
                                           },
                                           0x1);

            int[] runs = { 1, 2, 4, 8 };
            tlrunList = new Option.IntList(advoptsroot, "tlruns", "Attribute Runs",
                                           runs,
                                           intStringList(runs),
                                           intStringList(runs, " runs"),
                                           0x1);

            String[] tlmapnames = new String[] { "FONT", "Empty", "Simple", "Complex" };
            tlmapList = new Option.ObjectList(advoptsroot, "maptype", "Map",
                                              tlmapnames,
                                              maps,
                                              new String[] { "font", "empty", "simple", "complex" },
                                              tlmapnames,
                                              0x1);
        }
    }

    /**
     * This "virtual Node" implementation is here to maintain backward
     * compatibility with older J2DBench releases, specifically those
     * options files that were created before we added LCD-optimized text
     * hints in JDK 6.  This class will translate the text antialias settings
     * from the old "taa" On/Off/Both choice into the new expanded version.
     */
    private static class TextAAOpt extends Node {
        public TextAAOpt() {
            super(txoptgraphicsroot, "taa", "Text AntiAlias");
        }

        public JComponent getJComponent() {
            return null;
        }

        public void restoreDefault() {
            // no-op
        }

        public void write(PrintWriter pw) {
            // no-op (the old "taa" choice will be saved as part of the
            // new "textaa" option)
        }

        public String setOption(String key, String value) {
            String opts;
            if (value.equals("On")) {
                opts = "On";
            } else if (value.equals("Off")) {
                opts = "Off";
            } else if (value.equals("Both")) {
                opts = "On,Off";
            } else {
                return "Bad value";
            }
            return ((Option.ObjectList)taaList).setValueFromString(opts);
        }
    }

    public static class Context {
        void init(TestEnvironment env, Result result) {}
        void cleanup(TestEnvironment env) {}
    }

    public static class TextContext extends Context {
        Graphics graphics;
        String text;
        char[] chars;
        Font font;

        public void init(TestEnvironment env, Result result) {
            // graphics
            graphics = env.getGraphics();

            // text
            String sname = (String)env.getModifier(tscriptList);
            int slen = env.getIntValue(tlengthList);
            text = getString(sname, slen);

            // chars
            chars = text.toCharArray();

            // font
            String fname = (String)env.getModifier(fnameList);
            if ("Physical".equals(fname)) {
                fname = physicalFontNameFor(sname, slen, text);
            }
            int fstyle = env.getIntValue(fstyleList);
            float fsize = ((Float)env.getModifier(fsizeList)).floatValue();
            AffineTransform ftx = (AffineTransform)env.getModifier(ftxList);
            font = new Font(fname, fstyle, (int)fsize);
            if (hasGraphics2D) {
                if (fsize != Math.floor(fsize)) {
                    font = font.deriveFont(fsize);
                }
                if (!ftx.isIdentity()) {
                    font = font.deriveFont(ftx);
                }
            }

            // graphics
            if (hasGraphics2D) {
                Graphics2D g2d = (Graphics2D)graphics;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                     env.getModifier(taaList));
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                     env.isEnabled(tfmTog)
                                     ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                                     : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     env.isEnabled(gaaTog)
                                     ? RenderingHints.VALUE_ANTIALIAS_ON
                                     : RenderingHints.VALUE_ANTIALIAS_OFF);
                g2d.transform((AffineTransform)env.getModifier(gtxList));
            }

            // set result
            result.setUnits(text.length());
            result.setUnitName("char");
        }

        public void cleanup(TestEnvironment env) {
            graphics.dispose();
            graphics = null;
        }
    }

    public static class G2DContext extends TextContext {
        Graphics2D g2d;
        FontRenderContext frc;

        public void init(TestEnvironment env, Result results){
            super.init(env, results);
            g2d = (Graphics2D)graphics;
            frc = g2d.getFontRenderContext();
        }
    }

    public TextTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
        addDependency(Destinations.destroot);
        addDependencies(txoptroot, true);
    }

    public Context createContext() {
        return new TextContext();
    }

    public Object initTest(TestEnvironment env, Result result) {
        Context ctx = createContext();
        ctx.init(env, result);
        return ctx;
    }

    public void cleanupTest(TestEnvironment env, Object ctx) {
        ((Context)ctx).cleanup(env);
    }

    static Map physicalMap = new HashMap();
    public static String physicalFontNameFor(String textname, int textlen, String text) {
        Map lenMap = (Map)physicalMap.get(textname);
        if (lenMap == null) {
            lenMap = new HashMap();
            physicalMap.put(textname, lenMap);
        }
        Integer key = new Integer(textlen);
        Font textfont = (Font)lenMap.get(key);
        if (textfont == null) {
            Font[] fontsToTry = null;
            if (lenMap.isEmpty()) {
                fontsToTry = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
            } else {
                Set fontset = new HashSet();
                java.util.Iterator iter = lenMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry e = (Map.Entry)iter.next();
                    fontset.add(e.getValue());
                }
                fontsToTry = (Font[])fontset.toArray(new Font[fontset.size()]);
            }

            Font bestFont = null;
            int bestCount = 0;
            for (int i = 0; i < fontsToTry.length; ++i) {
                Font font = fontsToTry[i];
                int count = 0;
                for (int j = 0, limit = text.length(); j < limit; ++j) {
                    if (font.canDisplay(text.charAt(j))) {
                        ++count;
                    }
                }
                if (count > bestCount) {
                    bestFont = font;
                    bestCount = count;
                }
            }

            textfont = bestFont;
            lenMap.put(key, textfont);
        }
        return textfont.getName();
    }

    static class FontOption extends ObjectList {
        static String[] optionnames = {
            "default", "serif", "lucida", "physical"
        };
        static String[] descnames = {
            "Default", "Serif", "Lucida Sans", "Physical"
        };

        public FontOption(Group parent, String nodeName, String description) {
            super(parent, nodeName, description,
                  optionnames, descnames, optionnames, descnames, 0xa);
        }

        public String getValString(Object value) {
            return value.toString();
        }

        public String getAbbreviatedModifierDescription(Object value) {
            return value.toString();
        }
    }
}

