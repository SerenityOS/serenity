/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.gtk;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import javax.swing.plaf.synth.ColorType;
import javax.swing.plaf.synth.SynthConstants;
import javax.swing.plaf.synth.SynthContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.sun.java.swing.plaf.gtk.GTKConstants.ArrowType;
import com.sun.java.swing.plaf.gtk.GTKConstants.ShadowType;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sun.swing.SwingUtilities2;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 */
class Metacity implements SynthConstants {
    // Tutorial:
    // http://developer.gnome.org/doc/tutorials/metacity/metacity-themes.html

    // Themes:
    // http://art.gnome.org/theme_list.php?category=metacity

    static Metacity INSTANCE;

    private static final String[] themeNames = {
        getUserTheme(),
        "blueprint",
        "Bluecurve",
        "Crux",
        "SwingFallbackTheme"
    };

    static {
        for (String themeName : themeNames) {
            if (themeName != null) {
            try {
                INSTANCE = new Metacity(themeName);
            } catch (FileNotFoundException ex) {
            } catch (IOException ex) {
                logError(themeName, ex);
            } catch (ParserConfigurationException ex) {
                logError(themeName, ex);
            } catch (SAXException ex) {
                logError(themeName, ex);
            }
            }
            if (INSTANCE != null) {
            break;
            }
        }
        if (INSTANCE == null) {
            throw new Error("Could not find any installed metacity theme, and fallback failed");
        }
    }

    private static boolean errorLogged = false;
    private static DocumentBuilder documentBuilder;
    private static Document xmlDoc;
    private static String userHome;

    private Node frame_style_set;
    private Map<String, Object> frameGeometry;
    private Map<String, Map<String, Object>> frameGeometries;

    private LayoutManager titlePaneLayout = new TitlePaneLayout();

    private ColorizeImageFilter imageFilter = new ColorizeImageFilter();
    private URL themeDir = null;
    private SynthContext context;
    private String themeName;

    private ArithmeticExpressionEvaluator aee = new ArithmeticExpressionEvaluator();
    private Map<String, Integer> variables;

    // Reusable clip shape object
    private RoundRectClipShape roundedClipShape;

    protected Metacity(String themeName) throws IOException, ParserConfigurationException, SAXException {
        this.themeName = themeName;
        themeDir = getThemeDir(themeName);
        if (themeDir != null) {
            URL themeURL = new URL(themeDir, "metacity-theme-1.xml");
            xmlDoc = getXMLDoc(themeURL);
            if (xmlDoc == null) {
                throw new IOException(themeURL.toString());
            }
        } else {
            throw new FileNotFoundException(themeName);
        }

        // Initialize constants
        variables = new HashMap<String, Integer>();
        NodeList nodes = xmlDoc.getElementsByTagName("constant");
        int n = nodes.getLength();
        for (int i = 0; i < n; i++) {
            Node node = nodes.item(i);
            String name = getStringAttr(node, "name");
            if (name != null) {
                String value = getStringAttr(node, "value");
                if (value != null) {
                    try {
                        variables.put(name, Integer.parseInt(value));
                    } catch (NumberFormatException ex) {
                        logError(themeName, ex);
                        // Ignore bad value
                    }
                }
            }
        }

        // Cache frame geometries
        frameGeometries = new HashMap<String, Map<String, Object>>();
        nodes = xmlDoc.getElementsByTagName("frame_geometry");
        n = nodes.getLength();
        for (int i = 0; i < n; i++) {
            Node node = nodes.item(i);
            String name = getStringAttr(node, "name");
            if (name != null) {
                HashMap<String, Object> gm = new HashMap<String, Object>();
                frameGeometries.put(name, gm);

                String parentGM = getStringAttr(node, "parent");
                if (parentGM != null) {
                    gm.putAll(frameGeometries.get(parentGM));
                }

                gm.put("has_title",
                       Boolean.valueOf(getBooleanAttr(node, "has_title",            true)));
                gm.put("rounded_top_left",
                       Boolean.valueOf(getBooleanAttr(node, "rounded_top_left",     false)));
                gm.put("rounded_top_right",
                       Boolean.valueOf(getBooleanAttr(node, "rounded_top_right",    false)));
                gm.put("rounded_bottom_left",
                       Boolean.valueOf(getBooleanAttr(node, "rounded_bottom_left",  false)));
                gm.put("rounded_bottom_right",
                       Boolean.valueOf(getBooleanAttr(node, "rounded_bottom_right", false)));

                NodeList childNodes = node.getChildNodes();
                int nc = childNodes.getLength();
                for (int j = 0; j < nc; j++) {
                    Node child = childNodes.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        name = child.getNodeName();
                        Object value = null;
                        if ("distance".equals(name)) {
                            value = Integer.valueOf(getIntAttr(child, "value", 0));
                        } else if ("border".equals(name)) {
                            value = new Insets(getIntAttr(child, "top", 0),
                                               getIntAttr(child, "left", 0),
                                               getIntAttr(child, "bottom", 0),
                                               getIntAttr(child, "right", 0));
                        } else if ("aspect_ratio".equals(name)) {
                            value = Float.valueOf(getFloatAttr(child, "value", 1.0F));
                        } else {
                            logError(themeName, "Unknown Metacity frame geometry value type: "+name);
                        }
                        String childName = getStringAttr(child, "name");
                        if (childName != null && value != null) {
                            gm.put(childName, value);
                        }
                    }
                }
            }
        }
        frameGeometry = frameGeometries.get("normal");
    }


    public static LayoutManager getTitlePaneLayout() {
        return INSTANCE.titlePaneLayout;
    }

    private Shape getRoundedClipShape(int x, int y, int w, int h,
                                      int arcw, int arch, int corners) {
        if (roundedClipShape == null) {
            roundedClipShape = new RoundRectClipShape();
        }
        roundedClipShape.setRoundedRect(x, y, w, h, arcw, arch, corners);

        return roundedClipShape;
    }

    void paintButtonBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
        updateFrameGeometry(context);

        this.context = context;
        JButton button = (JButton)context.getComponent();
        String buttonName = button.getName();
        int buttonState = context.getComponentState();

        JComponent titlePane = (JComponent)button.getParent();
        Container titlePaneParent = titlePane.getParent();

        JInternalFrame jif = findInternalFrame(titlePaneParent);
        if (jif == null) {
            return;
        }

        boolean active = jif.isSelected();
        button.setOpaque(false);

        String state = "normal";
        if ((buttonState & PRESSED) != 0) {
            state = "pressed";
        } else if ((buttonState & MOUSE_OVER) != 0) {
            state = "prelight";
        }

        String function = null;
        String location = null;
        boolean left_corner  = false;
        boolean right_corner = false;


        if (buttonName == "InternalFrameTitlePane.menuButton") {
            function = "menu";
            location = "left_left";
            left_corner = true;
        } else if (buttonName == "InternalFrameTitlePane.iconifyButton") {
            function = "minimize";
            int nButtons = ((jif.isIconifiable() ? 1 : 0) +
                            (jif.isMaximizable() ? 1 : 0) +
                            (jif.isClosable() ? 1 : 0));
            right_corner = (nButtons == 1);
            switch (nButtons) {
              case 1: location = "right_right"; break;
              case 2: location = "right_middle"; break;
              case 3: location = "right_left"; break;
            }
        } else if (buttonName == "InternalFrameTitlePane.maximizeButton") {
            function = "maximize";
            right_corner = !jif.isClosable();
            location = jif.isClosable() ? "right_middle" : "right_right";
        } else if (buttonName == "InternalFrameTitlePane.closeButton") {
            function = "close";
            right_corner = true;
            location = "right_right";
        }

        Node frame = getNode(frame_style_set, "frame", new String[] {
            "focus", (active ? "yes" : "no"),
            "state", (jif.isMaximum() ? "maximized" : "normal")
        });

        if (function != null && frame != null) {
            Node frame_style = getNode("frame_style", new String[] {
                "name", getStringAttr(frame, "style")
            });
            if (frame_style != null) {
                Shape oldClip = g.getClip();
                if ((right_corner && getBoolean("rounded_top_right", false)) ||
                    (left_corner  && getBoolean("rounded_top_left", false))) {

                    Point buttonLoc = button.getLocation();
                    if (right_corner) {
                        g.setClip(getRoundedClipShape(0, 0, w, h,
                                                      12, 12, RoundRectClipShape.TOP_RIGHT));
                    } else {
                        g.setClip(getRoundedClipShape(0, 0, w, h,
                                                      11, 11, RoundRectClipShape.TOP_LEFT));
                    }

                    Rectangle clipBounds = oldClip.getBounds();
                    g.clipRect(clipBounds.x, clipBounds.y,
                               clipBounds.width, clipBounds.height);
                }
                drawButton(frame_style, location+"_background", state, g, w, h, jif);
                drawButton(frame_style, function, state, g, w, h, jif);
                g.setClip(oldClip);
            }
        }
    }

    protected void drawButton(Node frame_style, String function, String state,
                            Graphics g, int w, int h, JInternalFrame jif) {
        Node buttonNode = getNode(frame_style, "button",
                                  new String[] { "function", function, "state", state });
        if (buttonNode == null && !state.equals("normal")) {
            buttonNode = getNode(frame_style, "button",
                                 new String[] { "function", function, "state", "normal" });
        }
        if (buttonNode != null) {
            Node draw_ops;
            String draw_ops_name = getStringAttr(buttonNode, "draw_ops");
            if (draw_ops_name != null) {
                draw_ops = getNode("draw_ops", new String[] { "name", draw_ops_name });
            } else {
                draw_ops = getNode(buttonNode, "draw_ops", null);
            }
            variables.put("width",  w);
            variables.put("height", h);
            draw(draw_ops, g, jif);
        }
    }

    JInternalFrame findInternalFrame(Component comp) {
        if (comp.getParent() instanceof BasicInternalFrameTitlePane) {
            comp = comp.getParent();
        }
        if (comp instanceof JInternalFrame) {
            return  (JInternalFrame)comp;
        } else if (comp instanceof JInternalFrame.JDesktopIcon) {
            return ((JInternalFrame.JDesktopIcon)comp).getInternalFrame();
        }
        assert false : "cannot find the internal frame";
        return null;
    }

    void paintFrameBorder(SynthContext context, Graphics g, int x0, int y0, int width, int height) {
        updateFrameGeometry(context);

        this.context = context;
        JComponent comp = context.getComponent();
        JComponent titlePane = findChild(comp, "InternalFrame.northPane");

        if (titlePane == null) {
            return;
        }

        JInternalFrame jif = findInternalFrame(comp);
        if (jif == null) {
            return;
        }

        boolean active = jif.isSelected();
        Font oldFont = g.getFont();
        g.setFont(titlePane.getFont());
        g.translate(x0, y0);

        Rectangle titleRect = calculateTitleArea(jif);
        JComponent menuButton = findChild(titlePane, "InternalFrameTitlePane.menuButton");

        Icon frameIcon = jif.getFrameIcon();
        variables.put("mini_icon_width",
                      (frameIcon != null) ? frameIcon.getIconWidth()  : 0);
        variables.put("mini_icon_height",
                      (frameIcon != null) ? frameIcon.getIconHeight() : 0);
        variables.put("title_width",  calculateTitleTextWidth(g, jif));
        FontMetrics fm = SwingUtilities2.getFontMetrics(jif, g);
        variables.put("title_height", fm.getAscent() + fm.getDescent());

        // These don't seem to apply here, but the Galaxy theme uses them. Not sure why.
        variables.put("icon_width",  32);
        variables.put("icon_height", 32);

        if (frame_style_set != null) {
            Node frame = getNode(frame_style_set, "frame", new String[] {
                "focus", (active ? "yes" : "no"),
                "state", (jif.isMaximum() ? "maximized" : "normal")
            });

            if (frame != null) {
                Node frame_style = getNode("frame_style", new String[] {
                    "name", getStringAttr(frame, "style")
                });
                if (frame_style != null) {
                    Shape oldClip = g.getClip();
                    boolean roundTopLeft     = getBoolean("rounded_top_left",     false);
                    boolean roundTopRight    = getBoolean("rounded_top_right",    false);
                    boolean roundBottomLeft  = getBoolean("rounded_bottom_left",  false);
                    boolean roundBottomRight = getBoolean("rounded_bottom_right", false);

                    if (roundTopLeft || roundTopRight || roundBottomLeft || roundBottomRight) {
                        jif.setOpaque(false);

                        g.setClip(getRoundedClipShape(0, 0, width, height, 12, 12,
                                        (roundTopLeft     ? RoundRectClipShape.TOP_LEFT     : 0) |
                                        (roundTopRight    ? RoundRectClipShape.TOP_RIGHT    : 0) |
                                        (roundBottomLeft  ? RoundRectClipShape.BOTTOM_LEFT  : 0) |
                                        (roundBottomRight ? RoundRectClipShape.BOTTOM_RIGHT : 0)));
                    }

                    Rectangle clipBounds = oldClip.getBounds();
                    g.clipRect(clipBounds.x, clipBounds.y,
                               clipBounds.width, clipBounds.height);

                    int titleHeight = titlePane.getHeight();

                    boolean minimized = jif.isIcon();
                    Insets insets = getBorderInsets(context, null);

                    int leftTitlebarEdge   = getInt("left_titlebar_edge");
                    int rightTitlebarEdge  = getInt("right_titlebar_edge");
                    int topTitlebarEdge    = getInt("top_titlebar_edge");
                    int bottomTitlebarEdge = getInt("bottom_titlebar_edge");

                    if (!minimized) {
                        drawPiece(frame_style, g, "entire_background",
                                  0, 0, width, height, jif);
                    }
                    drawPiece(frame_style, g, "titlebar",
                              0, 0, width, titleHeight, jif);
                    drawPiece(frame_style, g, "titlebar_middle",
                              leftTitlebarEdge, topTitlebarEdge,
                              width - leftTitlebarEdge - rightTitlebarEdge,
                              titleHeight - topTitlebarEdge - bottomTitlebarEdge,
                              jif);
                    drawPiece(frame_style, g, "left_titlebar_edge",
                              0, 0, leftTitlebarEdge, titleHeight, jif);
                    drawPiece(frame_style, g, "right_titlebar_edge",
                              width - rightTitlebarEdge, 0,
                              rightTitlebarEdge, titleHeight, jif);
                    drawPiece(frame_style, g, "top_titlebar_edge",
                              0, 0, width, topTitlebarEdge, jif);
                    drawPiece(frame_style, g, "bottom_titlebar_edge",
                              0, titleHeight - bottomTitlebarEdge,
                              width, bottomTitlebarEdge, jif);
                    drawPiece(frame_style, g, "title",
                              titleRect.x, titleRect.y, titleRect.width, titleRect.height, jif);
                    if (!minimized) {
                        drawPiece(frame_style, g, "left_edge",
                                  0, titleHeight, insets.left, height-titleHeight, jif);
                        drawPiece(frame_style, g, "right_edge",
                                  width-insets.right, titleHeight, insets.right, height-titleHeight, jif);
                        drawPiece(frame_style, g, "bottom_edge",
                                  0, height - insets.bottom, width, insets.bottom, jif);
                        drawPiece(frame_style, g, "overlay",
                                  0, 0, width, height, jif);
                    }
                    g.setClip(oldClip);
                }
            }
        }
        g.translate(-x0, -y0);
        g.setFont(oldFont);
    }



    private static class Privileged implements PrivilegedAction<Object> {
        private static int GET_THEME_DIR  = 0;
        private static int GET_USER_THEME = 1;
        private static int GET_IMAGE      = 2;
        private int type;
        private Object arg;

        @SuppressWarnings("removal")
        public Object doPrivileged(int type, Object arg) {
            this.type = type;
            this.arg = arg;
            return AccessController.doPrivileged(this);
        }

        public Object run() {
            if (type == GET_THEME_DIR) {
                String sep = File.separator;
                String[] dirs = new String[] {
                    userHome + sep + ".themes",
                    System.getProperty("swing.metacitythemedir"),
                    "/usr/X11R6/share/themes",
                    "/usr/X11R6/share/gnome/themes",
                    "/usr/local/share/themes",
                    "/usr/local/share/gnome/themes",
                    "/usr/share/themes",
                    "/usr/gnome/share/themes",  // Debian/Redhat/Solaris
                    "/opt/gnome2/share/themes"  // SuSE
                };

                URL themeDir = null;
                for (int i = 0; i < dirs.length; i++) {
                    // System property may not be set so skip null directories.
                    if (dirs[i] == null) {
                        continue;
                    }
                    File dir =
                        new File(dirs[i] + sep + arg + sep + "metacity-1");
                    if (new File(dir, "metacity-theme-1.xml").canRead()) {
                        try {
                            themeDir = dir.toURI().toURL();
                        } catch (MalformedURLException ex) {
                            themeDir = null;
                        }
                        break;
                    }
                }
                if (themeDir == null) {
                    String filename = "resources/metacity/" + arg +
                        "/metacity-1/metacity-theme-1.xml";
                    URL url = getClass().getResource(filename);
                    if (url != null) {
                        String str = url.toString();
                        try {
                            themeDir = new URL(str.substring(0, str.lastIndexOf('/'))+"/");
                        } catch (MalformedURLException ex) {
                            themeDir = null;
                        }
                    }
                }
                return themeDir;
            } else if (type == GET_USER_THEME) {
                try {
                    // Set userHome here because we need the privilege
                    userHome = System.getProperty("user.home");

                    String theme = System.getProperty("swing.metacitythemename");
                    if (theme != null) {
                        return theme;
                    }
                    // Note: this is a small file (< 1024 bytes) so it's not worth
                    // starting an XML parser or even to use a buffered reader.
                    URL url = new URL(new File(userHome).toURI().toURL(),
                                      ".gconf/apps/metacity/general/%25gconf.xml");
                    // Pending: verify character encoding spec for gconf
                    Reader reader = new InputStreamReader(url.openStream(),
                                                          ISO_8859_1);
                    char[] buf = new char[1024];
                    StringBuilder sb = new StringBuilder();
                    int n;
                    while ((n = reader.read(buf)) >= 0) {
                        sb.append(buf, 0, n);
                    }
                    reader.close();
                    String str = sb.toString();
                    if (str != null) {
                        String strLowerCase = str.toLowerCase();
                        int i = strLowerCase.indexOf("<entry name=\"theme\"");
                        if (i >= 0) {
                            i = strLowerCase.indexOf("<stringvalue>", i);
                            if (i > 0) {
                                i += "<stringvalue>".length();
                                int i2 = str.indexOf('<', i);
                                return str.substring(i, i2);
                            }
                        }
                    }
                } catch (MalformedURLException ex) {
                    // OK to just ignore. We'll use a fallback theme.
                } catch (IOException ex) {
                    // OK to just ignore. We'll use a fallback theme.
                }
                return null;
            } else if (type == GET_IMAGE) {
                return new ImageIcon((URL)arg).getImage();
            } else {
                return null;
            }
        }
    }

    private static URL getThemeDir(String themeName) {
        return (URL)new Privileged().doPrivileged(Privileged.GET_THEME_DIR, themeName);
    }

    private static String getUserTheme() {
        return (String)new Privileged().doPrivileged(Privileged.GET_USER_THEME, null);
    }

    protected void tileImage(Graphics g, Image image, int x0, int y0, int w, int h, float[] alphas) {
        Graphics2D g2 = (Graphics2D)g;
        Composite oldComp = g2.getComposite();

        int sw = image.getWidth(null);
        int sh = image.getHeight(null);
        int y = y0;
        while (y < y0 + h) {
            sh = Math.min(sh, y0 + h - y);
            int x = x0;
            while (x < x0 + w) {
                float f = (alphas.length - 1.0F) * x / (x0 + w);
                int i = (int)f;
                f -= (int)f;
                float alpha = (1-f) * alphas[i];
                if (i+1 < alphas.length) {
                    alpha += f * alphas[i+1];
                }
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                int swm = Math.min(sw, x0 + w - x);
                g.drawImage(image, x, y, x+swm, y+sh, 0, 0, swm, sh, null);
                x += swm;
            }
            y += sh;
        }
        g2.setComposite(oldComp);
    }

    private HashMap<String, Image> images = new HashMap<String, Image>();

    protected Image getImage(String key, Color c) {
        Image image = images.get(key+"-"+c.getRGB());
        if (image == null) {
            image = imageFilter.colorize(getImage(key), c);
            if (image != null) {
                images.put(key+"-"+c.getRGB(), image);
            }
        }
        return image;
    }

    protected Image getImage(String key) {
        Image image = images.get(key);
        if (image == null) {
            if (themeDir != null) {
                try {
                    URL url = new URL(themeDir, key);
                    image = (Image)new Privileged().doPrivileged(Privileged.GET_IMAGE, url);
                } catch (MalformedURLException ex) {
                    //log("Bad image url: "+ themeDir + "/" + key);
                }
            }
            if (image != null) {
                images.put(key, image);
            }
        }
        return image;
    }

    private class ColorizeImageFilter extends RGBImageFilter {
        double cr, cg, cb;

        public ColorizeImageFilter() {
            canFilterIndexColorModel = true;
        }

        public void setColor(Color color) {
            cr = color.getRed()   / 255.0;
            cg = color.getGreen() / 255.0;
            cb = color.getBlue()  / 255.0;
        }

        public Image colorize(Image fromImage, Color c) {
            setColor(c);
            ImageProducer producer = new FilteredImageSource(fromImage.getSource(), this);
            return new ImageIcon(context.getComponent().createImage(producer)).getImage();
        }

        public int filterRGB(int x, int y, int rgb) {
            // Assume all rgb values are shades of gray
            double grayLevel = 2 * (rgb & 0xff) / 255.0;
            double r, g, b;

            if (grayLevel <= 1.0) {
                r = cr * grayLevel;
                g = cg * grayLevel;
                b = cb * grayLevel;
            } else {
                grayLevel -= 1.0;
                r = cr + (1.0 - cr) * grayLevel;
                g = cg + (1.0 - cg) * grayLevel;
                b = cb + (1.0 - cb) * grayLevel;
            }

            return ((rgb & 0xff000000) +
                    (((int)(r * 255)) << 16) +
                    (((int)(g * 255)) << 8) +
                    (int)(b * 255));
        }
    }

    protected static JComponent findChild(JComponent parent, String name) {
        int n = parent.getComponentCount();
        for (int i = 0; i < n; i++) {
            JComponent c = (JComponent)parent.getComponent(i);
            if (name.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }


    protected class TitlePaneLayout implements LayoutManager {
        public void addLayoutComponent(String name, Component c) {}
        public void removeLayoutComponent(Component c) {}
        public Dimension preferredLayoutSize(Container c)  {
            return minimumLayoutSize(c);
        }

        public Dimension minimumLayoutSize(Container c) {
            JComponent titlePane = (JComponent)c;
            Container titlePaneParent = titlePane.getParent();
            JInternalFrame frame;
            if (titlePaneParent instanceof JInternalFrame) {
                frame = (JInternalFrame)titlePaneParent;
            } else if (titlePaneParent instanceof JInternalFrame.JDesktopIcon) {
                frame = ((JInternalFrame.JDesktopIcon)titlePaneParent).getInternalFrame();
            } else {
                return null;
            }

            Dimension buttonDim = calculateButtonSize(titlePane);
            Insets title_border  = (Insets)getFrameGeometry().get("title_border");
            Insets button_border = (Insets)getFrameGeometry().get("button_border");

            // Calculate width.
            int width = getInt("left_titlebar_edge") + buttonDim.width + getInt("right_titlebar_edge");
            if (title_border != null) {
                width += title_border.left + title_border.right;
            }
            if (frame.isClosable()) {
                width += buttonDim.width;
            }
            if (frame.isMaximizable()) {
                width += buttonDim.width;
            }
            if (frame.isIconifiable()) {
                width += buttonDim.width;
            }
            FontMetrics fm = frame.getFontMetrics(titlePane.getFont());
            String frameTitle = frame.getTitle();
            int title_w = frameTitle != null ? SwingUtilities2.stringWidth(
                               frame, fm, frameTitle) : 0;
            int title_length = frameTitle != null ? frameTitle.length() : 0;

            // Leave room for three characters in the title.
            if (title_length > 3) {
                int subtitle_w = SwingUtilities2.stringWidth(
                    frame, fm, frameTitle.substring(0, 3) + "...");
                width += (title_w < subtitle_w) ? title_w : subtitle_w;
            } else {
                width += title_w;
            }

            // Calculate height.
            int titleHeight = fm.getHeight() + getInt("title_vertical_pad");
            if (title_border != null) {
                titleHeight += title_border.top + title_border.bottom;
            }
            int buttonHeight = buttonDim.height;
            if (button_border != null) {
                buttonHeight += button_border.top + button_border.bottom;
            }
            int height = Math.max(buttonHeight, titleHeight);

            return new Dimension(width, height);
        }

        public void layoutContainer(Container c) {
            JComponent titlePane = (JComponent)c;
            Container titlePaneParent = titlePane.getParent();
            JInternalFrame frame;
            if (titlePaneParent instanceof JInternalFrame) {
                frame = (JInternalFrame)titlePaneParent;
            } else if (titlePaneParent instanceof JInternalFrame.JDesktopIcon) {
                frame = ((JInternalFrame.JDesktopIcon)titlePaneParent).getInternalFrame();
            } else {
                return;
            }
            Map<String, Object> gm = getFrameGeometry();

            int w = titlePane.getWidth();
            int h = titlePane.getHeight();

            JComponent menuButton     = findChild(titlePane, "InternalFrameTitlePane.menuButton");
            JComponent minimizeButton = findChild(titlePane, "InternalFrameTitlePane.iconifyButton");
            JComponent maximizeButton = findChild(titlePane, "InternalFrameTitlePane.maximizeButton");
            JComponent closeButton    = findChild(titlePane, "InternalFrameTitlePane.closeButton");

            Insets button_border = (Insets)gm.get("button_border");
            Dimension buttonDim = calculateButtonSize(titlePane);

            int y = (button_border != null) ? button_border.top : 0;
            if (titlePaneParent.getComponentOrientation().isLeftToRight()) {
                int x = getInt("left_titlebar_edge");

                menuButton.setBounds(x, y, buttonDim.width, buttonDim.height);

                x = w - buttonDim.width - getInt("right_titlebar_edge");
                if (button_border != null) {
                    x -= button_border.right;
                }

                if (frame.isClosable()) {
                    closeButton.setBounds(x, y, buttonDim.width, buttonDim.height);
                    x -= buttonDim.width;
                }

                if (frame.isMaximizable()) {
                    maximizeButton.setBounds(x, y, buttonDim.width, buttonDim.height);
                    x -= buttonDim.width;
                }

                if (frame.isIconifiable()) {
                    minimizeButton.setBounds(x, y, buttonDim.width, buttonDim.height);
                }
            } else {
                int x = w - buttonDim.width - getInt("right_titlebar_edge");

                menuButton.setBounds(x, y, buttonDim.width, buttonDim.height);

                x = getInt("left_titlebar_edge");
                if (button_border != null) {
                    x += button_border.left;
                }

                if (frame.isClosable()) {
                    closeButton.setBounds(x, y, buttonDim.width, buttonDim.height);
                    x += buttonDim.width;
                }

                if (frame.isMaximizable()) {
                    maximizeButton.setBounds(x, y, buttonDim.width, buttonDim.height);
                    x += buttonDim.width;
                }

                if (frame.isIconifiable()) {
                    minimizeButton.setBounds(x, y, buttonDim.width, buttonDim.height);
                }
            }
        }
    } // end TitlePaneLayout

    protected Map<String, Object> getFrameGeometry() {
        return frameGeometry;
    }

    protected void setFrameGeometry(JComponent titlePane, Map<String, Object> gm) {
        this.frameGeometry = gm;
        if (getInt("top_height") == 0 && titlePane != null) {
            gm.put("top_height", Integer.valueOf(titlePane.getHeight()));
        }
    }

    protected int getInt(String key) {
        Integer i = (Integer)frameGeometry.get(key);
        if (i == null) {
            i = variables.get(key);
        }
        return (i != null) ? i.intValue() : 0;
    }

    protected boolean getBoolean(String key, boolean fallback) {
        Boolean b = (Boolean)frameGeometry.get(key);
        return (b != null) ? b.booleanValue() : fallback;
    }


    protected void drawArc(Node node, Graphics g) {
        NamedNodeMap attrs = node.getAttributes();
        Color color = parseColor(getStringAttr(attrs, "color"));
        int x = aee.evaluate(getStringAttr(attrs, "x"));
        int y = aee.evaluate(getStringAttr(attrs, "y"));
        int w = aee.evaluate(getStringAttr(attrs, "width"));
        int h = aee.evaluate(getStringAttr(attrs, "height"));
        int start_angle = aee.evaluate(getStringAttr(attrs, "start_angle"));
        int extent_angle = aee.evaluate(getStringAttr(attrs, "extent_angle"));
        boolean filled = getBooleanAttr(node, "filled", false);
        if (getInt("width") == -1) {
            x -= w;
        }
        if (getInt("height") == -1) {
            y -= h;
        }
        g.setColor(color);
        if (filled) {
            g.fillArc(x, y, w, h, start_angle, extent_angle);
        } else {
            g.drawArc(x, y, w, h, start_angle, extent_angle);
        }
    }

    protected void drawLine(Node node, Graphics g) {
        NamedNodeMap attrs = node.getAttributes();
        Color color = parseColor(getStringAttr(attrs, "color"));
        int x1 = aee.evaluate(getStringAttr(attrs, "x1"));
        int y1 = aee.evaluate(getStringAttr(attrs, "y1"));
        int x2 = aee.evaluate(getStringAttr(attrs, "x2"));
        int y2 = aee.evaluate(getStringAttr(attrs, "y2"));
        int lineWidth = aee.evaluate(getStringAttr(attrs, "width"), 1);
        g.setColor(color);
        if (lineWidth != 1) {
            Graphics2D g2d = (Graphics2D)g;
            Stroke stroke = g2d.getStroke();
            g2d.setStroke(new BasicStroke((float)lineWidth));
            g2d.drawLine(x1, y1, x2, y2);
            g2d.setStroke(stroke);
        } else {
            g.drawLine(x1, y1, x2, y2);
        }
    }

    protected void drawRectangle(Node node, Graphics g) {
        NamedNodeMap attrs = node.getAttributes();
        Color color = parseColor(getStringAttr(attrs, "color"));
        boolean filled = getBooleanAttr(node, "filled", false);
        int x = aee.evaluate(getStringAttr(attrs, "x"));
        int y = aee.evaluate(getStringAttr(attrs, "y"));
        int w = aee.evaluate(getStringAttr(attrs, "width"));
        int h = aee.evaluate(getStringAttr(attrs, "height"));
        g.setColor(color);
        if (getInt("width") == -1) {
            x -= w;
        }
        if (getInt("height") == -1) {
            y -= h;
        }
        if (filled) {
            g.fillRect(x, y, w, h);
        } else {
            g.drawRect(x, y, w, h);
        }
    }

    protected void drawTile(Node node, Graphics g, JInternalFrame jif) {
        NamedNodeMap attrs = node.getAttributes();
        int x0 = aee.evaluate(getStringAttr(attrs, "x"));
        int y0 = aee.evaluate(getStringAttr(attrs, "y"));
        int w = aee.evaluate(getStringAttr(attrs, "width"));
        int h = aee.evaluate(getStringAttr(attrs, "height"));
        int tw = aee.evaluate(getStringAttr(attrs, "tile_width"));
        int th = aee.evaluate(getStringAttr(attrs, "tile_height"));
        int width  = getInt("width");
        int height = getInt("height");
        if (width == -1) {
            x0 -= w;
        }
        if (height == -1) {
            y0 -= h;
        }
        Shape oldClip = g.getClip();
        if (g instanceof Graphics2D) {
            ((Graphics2D)g).clip(new Rectangle(x0, y0, w, h));
        }
        variables.put("width",  tw);
        variables.put("height", th);

        Node draw_ops = getNode("draw_ops", new String[] { "name", getStringAttr(node, "name") });

        int y = y0;
        while (y < y0 + h) {
            int x = x0;
            while (x < x0 + w) {
                g.translate(x, y);
                draw(draw_ops, g, jif);
                g.translate(-x, -y);
                x += tw;
            }
            y += th;
        }

        variables.put("width",  width);
        variables.put("height", height);
        g.setClip(oldClip);
    }

    protected void drawTint(Node node, Graphics g) {
        NamedNodeMap attrs = node.getAttributes();
        Color color = parseColor(getStringAttr(attrs, "color"));
        float alpha = Float.parseFloat(getStringAttr(attrs, "alpha"));
        int x = aee.evaluate(getStringAttr(attrs, "x"));
        int y = aee.evaluate(getStringAttr(attrs, "y"));
        int w = aee.evaluate(getStringAttr(attrs, "width"));
        int h = aee.evaluate(getStringAttr(attrs, "height"));
        if (getInt("width") == -1) {
            x -= w;
        }
        if (getInt("height") == -1) {
            y -= h;
        }
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D)g;
            Composite oldComp = g2.getComposite();
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            g2.setComposite(ac);
            g2.setColor(color);
            g2.fillRect(x, y, w, h);
            g2.setComposite(oldComp);
        }
    }

    protected void drawTitle(Node node, Graphics g, JInternalFrame jif) {
        NamedNodeMap attrs = node.getAttributes();
        String colorStr = getStringAttr(attrs, "color");
        int i = colorStr.indexOf("gtk:fg[");
        if (i > 0) {
            colorStr = colorStr.substring(0, i) + "gtk:text[" + colorStr.substring(i+7);
        }
        Color color = parseColor(colorStr);
        int x = aee.evaluate(getStringAttr(attrs, "x"));
        int y = aee.evaluate(getStringAttr(attrs, "y"));

        String title = jif.getTitle();
        if (title != null) {
            FontMetrics fm = SwingUtilities2.getFontMetrics(jif, g);
            title = SwingUtilities2.clipStringIfNecessary(jif, fm, title,
                         calculateTitleArea(jif).width);
            g.setColor(color);
            SwingUtilities2.drawString(jif, g, title, x, y + fm.getAscent());
        }
    }

    protected Dimension calculateButtonSize(JComponent titlePane) {
        int buttonHeight = getInt("button_height");
        if (buttonHeight == 0) {
            buttonHeight = titlePane.getHeight();
            if (buttonHeight == 0) {
                buttonHeight = 13;
            } else {
                Insets button_border = (Insets)frameGeometry.get("button_border");
                if (button_border != null) {
                    buttonHeight -= (button_border.top + button_border.bottom);
                }
            }
        }
        int buttonWidth = getInt("button_width");
        if (buttonWidth == 0) {
            buttonWidth = buttonHeight;
            Float aspect_ratio = (Float)frameGeometry.get("aspect_ratio");
            if (aspect_ratio != null) {
                buttonWidth = (int)(buttonHeight / aspect_ratio.floatValue());
            }
        }
        return new Dimension(buttonWidth, buttonHeight);
    }

    protected Rectangle calculateTitleArea(JInternalFrame jif) {
        JComponent titlePane = findChild(jif, "InternalFrame.northPane");
        Dimension buttonDim = calculateButtonSize(titlePane);
        Insets title_border = (Insets)frameGeometry.get("title_border");
        Insets button_border = (Insets)getFrameGeometry().get("button_border");

        Rectangle r = new Rectangle();
        r.x = getInt("left_titlebar_edge");
        r.y = 0;
        r.height = titlePane.getHeight();
        if (title_border != null) {
            r.x += title_border.left;
            r.y += title_border.top;
            r.height -= (title_border.top + title_border.bottom);
        }

        if (titlePane.getParent().getComponentOrientation().isLeftToRight()) {
            r.x += buttonDim.width;
            if (button_border != null) {
                r.x += button_border.left;
            }
            r.width = titlePane.getWidth() - r.x - getInt("right_titlebar_edge");
            if (jif.isClosable()) {
                r.width -= buttonDim.width;
            }
            if (jif.isMaximizable()) {
                r.width -= buttonDim.width;
            }
            if (jif.isIconifiable()) {
                r.width -= buttonDim.width;
            }
        } else {
            if (jif.isClosable()) {
                r.x += buttonDim.width;
            }
            if (jif.isMaximizable()) {
                r.x += buttonDim.width;
            }
            if (jif.isIconifiable()) {
                r.x += buttonDim.width;
            }
            r.width = titlePane.getWidth() - r.x - getInt("right_titlebar_edge")
                    - buttonDim.width;
            if (button_border != null) {
                r.x -= button_border.right;
            }
        }
        if (title_border != null) {
            r.width -= title_border.right;
        }
        return r;
    }


    protected int calculateTitleTextWidth(Graphics g, JInternalFrame jif) {
        String title = jif.getTitle();
        if (title != null) {
            Rectangle r = calculateTitleArea(jif);
            return Math.min(SwingUtilities2.stringWidth(jif,
                     SwingUtilities2.getFontMetrics(jif, g), title), r.width);
        }
        return 0;
    }

    protected void setClip(Node node, Graphics g) {
        NamedNodeMap attrs = node.getAttributes();
        int x = aee.evaluate(getStringAttr(attrs, "x"));
        int y = aee.evaluate(getStringAttr(attrs, "y"));
        int w = aee.evaluate(getStringAttr(attrs, "width"));
        int h = aee.evaluate(getStringAttr(attrs, "height"));
        if (getInt("width") == -1) {
            x -= w;
        }
        if (getInt("height") == -1) {
            y -= h;
        }
        if (g instanceof Graphics2D) {
            ((Graphics2D)g).clip(new Rectangle(x, y, w, h));
        }
    }

    protected void drawGTKArrow(Node node, Graphics g) {
        NamedNodeMap attrs = node.getAttributes();
        String arrow    = getStringAttr(attrs, "arrow");
        String shadow   = getStringAttr(attrs, "shadow");
        String stateStr = getStringAttr(attrs, "state").toUpperCase();
        int x = aee.evaluate(getStringAttr(attrs, "x"));
        int y = aee.evaluate(getStringAttr(attrs, "y"));
        int w = aee.evaluate(getStringAttr(attrs, "width"));
        int h = aee.evaluate(getStringAttr(attrs, "height"));

        int state = -1;
        if ("NORMAL".equals(stateStr)) {
            state = ENABLED;
        } else if ("SELECTED".equals(stateStr)) {
            state = SELECTED;
        } else if ("INSENSITIVE".equals(stateStr)) {
            state = DISABLED;
        } else if ("PRELIGHT".equals(stateStr)) {
            state = MOUSE_OVER;
        }

        ShadowType shadowType = null;
        if ("in".equals(shadow)) {
            shadowType = ShadowType.IN;
        } else if ("out".equals(shadow)) {
            shadowType = ShadowType.OUT;
        } else if ("etched_in".equals(shadow)) {
            shadowType = ShadowType.ETCHED_IN;
        } else if ("etched_out".equals(shadow)) {
            shadowType = ShadowType.ETCHED_OUT;
        } else if ("none".equals(shadow)) {
            shadowType = ShadowType.NONE;
        }

        ArrowType direction = null;
        if ("up".equals(arrow)) {
            direction = ArrowType.UP;
        } else if ("down".equals(arrow)) {
            direction = ArrowType.DOWN;
        } else if ("left".equals(arrow)) {
            direction = ArrowType.LEFT;
        } else if ("right".equals(arrow)) {
            direction = ArrowType.RIGHT;
        }

        GTKPainter.INSTANCE.paintMetacityElement(context, g, state,
                "metacity-arrow", x, y, w, h, shadowType, direction);
    }

    protected void drawGTKBox(Node node, Graphics g) {
        NamedNodeMap attrs = node.getAttributes();
        String shadow   = getStringAttr(attrs, "shadow");
        String stateStr = getStringAttr(attrs, "state").toUpperCase();
        int x = aee.evaluate(getStringAttr(attrs, "x"));
        int y = aee.evaluate(getStringAttr(attrs, "y"));
        int w = aee.evaluate(getStringAttr(attrs, "width"));
        int h = aee.evaluate(getStringAttr(attrs, "height"));

        int state = -1;
        if ("NORMAL".equals(stateStr)) {
            state = ENABLED;
        } else if ("SELECTED".equals(stateStr)) {
            state = SELECTED;
        } else if ("INSENSITIVE".equals(stateStr)) {
            state = DISABLED;
        } else if ("PRELIGHT".equals(stateStr)) {
            state = MOUSE_OVER;
        }

        ShadowType shadowType = null;
        if ("in".equals(shadow)) {
            shadowType = ShadowType.IN;
        } else if ("out".equals(shadow)) {
            shadowType = ShadowType.OUT;
        } else if ("etched_in".equals(shadow)) {
            shadowType = ShadowType.ETCHED_IN;
        } else if ("etched_out".equals(shadow)) {
            shadowType = ShadowType.ETCHED_OUT;
        } else if ("none".equals(shadow)) {
            shadowType = ShadowType.NONE;
        }
        GTKPainter.INSTANCE.paintMetacityElement(context, g, state,
                "metacity-box", x, y, w, h, shadowType, null);
    }

    protected void drawGTKVLine(Node node, Graphics g) {
        NamedNodeMap attrs = node.getAttributes();
        String stateStr = getStringAttr(attrs, "state").toUpperCase();

        int x  = aee.evaluate(getStringAttr(attrs, "x"));
        int y1 = aee.evaluate(getStringAttr(attrs, "y1"));
        int y2 = aee.evaluate(getStringAttr(attrs, "y2"));

        int state = -1;
        if ("NORMAL".equals(stateStr)) {
            state = ENABLED;
        } else if ("SELECTED".equals(stateStr)) {
            state = SELECTED;
        } else if ("INSENSITIVE".equals(stateStr)) {
            state = DISABLED;
        } else if ("PRELIGHT".equals(stateStr)) {
            state = MOUSE_OVER;
        }

        GTKPainter.INSTANCE.paintMetacityElement(context, g, state,
                "metacity-vline", x, y1, 1, y2 - y1, null, null);
    }

    protected void drawGradient(Node node, Graphics g) {
        NamedNodeMap attrs = node.getAttributes();
        String type = getStringAttr(attrs, "type");
        float alpha = getFloatAttr(node, "alpha", -1F);
        int x = aee.evaluate(getStringAttr(attrs, "x"));
        int y = aee.evaluate(getStringAttr(attrs, "y"));
        int w = aee.evaluate(getStringAttr(attrs, "width"));
        int h = aee.evaluate(getStringAttr(attrs, "height"));
        if (getInt("width") == -1) {
            x -= w;
        }
        if (getInt("height") == -1) {
            y -= h;
        }

        // Get colors from child nodes
        Node[] colorNodes = getNodesByName(node, "color");
        Color[] colors = new Color[colorNodes.length];
        for (int i = 0; i < colorNodes.length; i++) {
            colors[i] = parseColor(getStringAttr(colorNodes[i], "value"));
        }

        boolean horizontal = ("diagonal".equals(type) || "horizontal".equals(type));
        boolean vertical   = ("diagonal".equals(type) || "vertical".equals(type));

        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D)g;
            Composite oldComp = g2.getComposite();
            if (alpha >= 0F) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            }
            int n = colors.length - 1;
            for (int i = 0; i < n; i++) {
                g2.setPaint(new GradientPaint(x + (horizontal ? (i*w/n) : 0),
                                              y + (vertical   ? (i*h/n) : 0),
                                              colors[i],
                                              x + (horizontal ? ((i+1)*w/n) : 0),
                                              y + (vertical   ? ((i+1)*h/n) : 0),
                                              colors[i+1]));
                g2.fillRect(x + (horizontal ? (i*w/n) : 0),
                            y + (vertical   ? (i*h/n) : 0),
                            (horizontal ? (w/n) : w),
                            (vertical   ? (h/n) : h));
            }
            g2.setComposite(oldComp);
        }
    }

    protected void drawImage(Node node, Graphics g) {
        NamedNodeMap attrs = node.getAttributes();
        String filename = getStringAttr(attrs, "filename");
        String colorizeStr = getStringAttr(attrs, "colorize");
        Color colorize = (colorizeStr != null) ? parseColor(colorizeStr) : null;
        String alpha = getStringAttr(attrs, "alpha");
        Image object = (colorize != null) ? getImage(filename, colorize) : getImage(filename);
        variables.put("object_width",  object.getWidth(null));
        variables.put("object_height", object.getHeight(null));
        String fill_type = getStringAttr(attrs, "fill_type");
        int x = aee.evaluate(getStringAttr(attrs, "x"));
        int y = aee.evaluate(getStringAttr(attrs, "y"));
        int w = aee.evaluate(getStringAttr(attrs, "width"));
        int h = aee.evaluate(getStringAttr(attrs, "height"));
        if (getInt("width") == -1) {
            x -= w;
        }
        if (getInt("height") == -1) {
            y -= h;
        }

        if (alpha != null) {
            if ("tile".equals(fill_type)) {
                StringTokenizer tokenizer = new StringTokenizer(alpha, ":");
                float[] alphas = new float[tokenizer.countTokens()];
                for (int i = 0; i < alphas.length; i++) {
                    alphas[i] = Float.parseFloat(tokenizer.nextToken());
                }
                tileImage(g, object, x, y, w, h, alphas);
            } else {
                float a = Float.parseFloat(alpha);
                if (g instanceof Graphics2D) {
                    Graphics2D g2 = (Graphics2D)g;
                    Composite oldComp = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
                    g2.drawImage(object, x, y, w, h, null);
                    g2.setComposite(oldComp);
                }
            }
        } else {
            g.drawImage(object, x, y, w, h, null);
        }
    }

    protected void drawIcon(Node node, Graphics g, JInternalFrame jif) {
        Icon icon = jif.getFrameIcon();
        if (icon == null) {
            return;
        }

        NamedNodeMap attrs = node.getAttributes();
        String alpha = getStringAttr(attrs, "alpha");
        int x = aee.evaluate(getStringAttr(attrs, "x"));
        int y = aee.evaluate(getStringAttr(attrs, "y"));
        int w = aee.evaluate(getStringAttr(attrs, "width"));
        int h = aee.evaluate(getStringAttr(attrs, "height"));
        if (getInt("width") == -1) {
            x -= w;
        }
        if (getInt("height") == -1) {
            y -= h;
        }

        if (alpha != null) {
            float a = Float.parseFloat(alpha);
            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D)g;
                Composite oldComp = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
                icon.paintIcon(jif, g, x, y);
                g2.setComposite(oldComp);
            }
        } else {
            icon.paintIcon(jif, g, x, y);
        }
    }

    protected void drawInclude(Node node, Graphics g, JInternalFrame jif) {
        int oldWidth  = getInt("width");
        int oldHeight = getInt("height");

        NamedNodeMap attrs = node.getAttributes();
        int x = aee.evaluate(getStringAttr(attrs, "x"),       0);
        int y = aee.evaluate(getStringAttr(attrs, "y"),       0);
        int w = aee.evaluate(getStringAttr(attrs, "width"),  -1);
        int h = aee.evaluate(getStringAttr(attrs, "height"), -1);

        if (w != -1) {
            variables.put("width",  w);
        }
        if (h != -1) {
            variables.put("height", h);
        }

        Node draw_ops = getNode("draw_ops", new String[] {
            "name", getStringAttr(node, "name")
        });
        g.translate(x, y);
        draw(draw_ops, g, jif);
        g.translate(-x, -y);

        if (w != -1) {
            variables.put("width",  oldWidth);
        }
        if (h != -1) {
            variables.put("height", oldHeight);
        }
    }

    protected void draw(Node draw_ops, Graphics g, JInternalFrame jif) {
        if (draw_ops != null) {
            NodeList nodes = draw_ops.getChildNodes();
            if (nodes != null) {
                Shape oldClip = g.getClip();
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node child = nodes.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        try {
                            String name = child.getNodeName();
                            if ("include".equals(name)) {
                                drawInclude(child, g, jif);
                            } else if ("arc".equals(name)) {
                                drawArc(child, g);
                            } else if ("clip".equals(name)) {
                                setClip(child, g);
                            } else if ("gradient".equals(name)) {
                                drawGradient(child, g);
                            } else if ("gtk_arrow".equals(name)) {
                                drawGTKArrow(child, g);
                            } else if ("gtk_box".equals(name)) {
                                drawGTKBox(child, g);
                            } else if ("gtk_vline".equals(name)) {
                                drawGTKVLine(child, g);
                            } else if ("image".equals(name)) {
                                drawImage(child, g);
                            } else if ("icon".equals(name)) {
                                drawIcon(child, g, jif);
                            } else if ("line".equals(name)) {
                                drawLine(child, g);
                            } else if ("rectangle".equals(name)) {
                                drawRectangle(child, g);
                            } else if ("tint".equals(name)) {
                                drawTint(child, g);
                            } else if ("tile".equals(name)) {
                                drawTile(child, g, jif);
                            } else if ("title".equals(name)) {
                                drawTitle(child, g, jif);
                            } else {
                                System.err.println("Unknown Metacity drawing op: "+child);
                            }
                        } catch (NumberFormatException ex) {
                            logError(themeName, ex);
                        }
                    }
                }
                g.setClip(oldClip);
            }
        }
    }

    protected void drawPiece(Node frame_style, Graphics g, String position, int x, int y,
                             int width, int height, JInternalFrame jif) {
        Node piece = getNode(frame_style, "piece", new String[] { "position", position });
        if (piece != null) {
            Node draw_ops;
            String draw_ops_name = getStringAttr(piece, "draw_ops");
            if (draw_ops_name != null) {
                draw_ops = getNode("draw_ops", new String[] { "name", draw_ops_name });
            } else {
                draw_ops = getNode(piece, "draw_ops", null);
            }
            variables.put("width",  width);
            variables.put("height", height);
            g.translate(x, y);
            draw(draw_ops, g, jif);
            g.translate(-x, -y);
        }
    }


    Insets getBorderInsets(SynthContext context, Insets insets) {
        updateFrameGeometry(context);

        if (insets == null) {
            insets = new Insets(0, 0, 0, 0);
        }
        insets.top    = ((Insets)frameGeometry.get("title_border")).top;
        insets.bottom = getInt("bottom_height");
        insets.left   = getInt("left_width");
        insets.right  = getInt("right_width");
        return insets;
    }


    private void updateFrameGeometry(SynthContext context) {
        this.context = context;
        JComponent comp = context.getComponent();
        JComponent titlePane = findChild(comp, "InternalFrame.northPane");

        JInternalFrame jif;
        if (comp instanceof JButton) {
            JComponent bTitlePane = (JComponent)comp.getParent();
            Container titlePaneParent = bTitlePane.getParent();
            jif = findInternalFrame(titlePaneParent);
        } else {
            jif = findInternalFrame(comp);
        }
        if (jif == null) {
            return;
        }

        if (frame_style_set == null) {
            Node window = getNode("window", new String[]{"type", "normal"});

            if (window != null) {
                frame_style_set = getNode("frame_style_set",
                        new String[] {"name", getStringAttr(window, "style_set")});
            }

            if (frame_style_set == null) {
                frame_style_set = getNode("frame_style_set", new String[] {"name", "normal"});
            }
        }

        if (frame_style_set != null) {
            Node frame = getNode(frame_style_set, "frame", new String[] {
                "focus", (jif.isSelected() ? "yes" : "no"),
                "state", (jif.isMaximum() ? "maximized" : "normal")
            });

            if (frame != null) {
                Node frame_style = getNode("frame_style", new String[] {
                    "name", getStringAttr(frame, "style")
                });
                if (frame_style != null) {
                    Map<String, Object> gm = frameGeometries.get(getStringAttr(frame_style, "geometry"));

                    setFrameGeometry(titlePane, gm);
                }
            }
        }
    }


    protected static void logError(String themeName, Exception ex) {
        logError(themeName, ex.toString());
    }

    protected static void logError(String themeName, String msg) {
        if (!errorLogged) {
            System.err.println("Exception in Metacity for theme \""+themeName+"\": "+msg);
            errorLogged = true;
        }
    }


    // XML Parsing


    protected static Document getXMLDoc(final URL xmlFile)
                                throws IOException,
                                       ParserConfigurationException,
                                       SAXException {
        if (documentBuilder == null) {
            documentBuilder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        @SuppressWarnings("removal")
        InputStream inputStream =
            AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                public InputStream run() {
                    try {
                        return new BufferedInputStream(xmlFile.openStream());
                    } catch (IOException ex) {
                        return null;
                    }
                }
            });

        Document doc = null;
        if (inputStream != null) {
            doc = documentBuilder.parse(inputStream);
        }
        return doc;
    }


    protected Node[] getNodesByName(Node parent, String name) {
        NodeList nodes = parent.getChildNodes(); // ElementNode
        int n = nodes.getLength();
        ArrayList<Node> list = new ArrayList<Node>();
        for (int i=0; i < n; i++) {
            Node node = nodes.item(i);
            if (name.equals(node.getNodeName())) {
                list.add(node);
            }
        }
        return list.toArray(new Node[list.size()]);
    }



    protected Node getNode(String tagName, String[] attrs) {
        NodeList nodes = xmlDoc.getElementsByTagName(tagName);
        return (nodes != null) ? getNode(nodes, tagName, attrs) : null;
    }

    protected Node getNode(Node parent, String name, String[] attrs) {
        Node node = null;
        NodeList nodes = parent.getChildNodes();
        if (nodes != null) {
            node = getNode(nodes, name, attrs);
        }
        if (node == null) {
            String inheritFrom = getStringAttr(parent, "parent");
            if (inheritFrom != null) {
                Node inheritFromNode = getNode(parent.getParentNode(),
                                               parent.getNodeName(),
                                               new String[] { "name", inheritFrom });
                if (inheritFromNode != null) {
                    node = getNode(inheritFromNode, name, attrs);
                }
            }
        }
        return node;
    }

    protected Node getNode(NodeList nodes, String name, String[] attrs) {
        int n = nodes.getLength();
        for (int i=0; i < n; i++) {
            Node node = nodes.item(i);
            if (name.equals(node.getNodeName())) {
                if (attrs != null) {
                    NamedNodeMap nodeAttrs = node.getAttributes();
                    if (nodeAttrs != null) {
                        boolean matches = true;
                        int nAttrs = attrs.length / 2;
                        for (int a = 0; a < nAttrs; a++) {
                            String aName  = attrs[a * 2];
                            String aValue = attrs[a * 2 + 1];
                            Node attr = nodeAttrs.getNamedItem(aName);
                            if (attr == null ||
                                aValue != null && !aValue.equals(attr.getNodeValue())) {
                                matches = false;
                                break;
                            }
                        }
                        if (matches) {
                            return node;
                        }
                    }
                } else {
                    return node;
                }
            }
        }
        return null;
    }

    protected String getStringAttr(Node node, String name) {
        String value = null;
        NamedNodeMap attrs = node.getAttributes();
        if (attrs != null) {
            value = getStringAttr(attrs, name);
            if (value == null) {
                String inheritFrom = getStringAttr(attrs, "parent");
                if (inheritFrom != null) {
                    Node inheritFromNode = getNode(node.getParentNode(),
                                                   node.getNodeName(),
                                                   new String[] { "name", inheritFrom });
                    if (inheritFromNode != null) {
                        value = getStringAttr(inheritFromNode, name);
                    }
                }
            }
        }
        return value;
    }

    protected String getStringAttr(NamedNodeMap attrs, String name) {
        Node item = attrs.getNamedItem(name);
        return (item != null) ? item.getNodeValue() : null;
    }

    protected boolean getBooleanAttr(Node node, String name, boolean fallback) {
        String str = getStringAttr(node, name);
        if (str != null) {
            return Boolean.valueOf(str).booleanValue();
        }
        return fallback;
    }

    protected int getIntAttr(Node node, String name, int fallback) {
        String str = getStringAttr(node, name);
        int value = fallback;
        if (str != null) {
            try {
                value = Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                logError(themeName, ex);
            }
        }
        return value;
    }

    protected float getFloatAttr(Node node, String name, float fallback) {
        String str = getStringAttr(node, name);
        float value = fallback;
        if (str != null) {
            try {
                value = Float.parseFloat(str);
            } catch (NumberFormatException ex) {
                logError(themeName, ex);
            }
        }
        return value;
    }



    protected Color parseColor(String str) {
        StringTokenizer tokenizer = new StringTokenizer(str, "/");
        int n = tokenizer.countTokens();
        if (n > 1) {
            String function = tokenizer.nextToken();
            if ("shade".equals(function)) {
                assert (n == 3);
                Color c = parseColor2(tokenizer.nextToken());
                float alpha = Float.parseFloat(tokenizer.nextToken());
                return GTKColorType.adjustColor(c, 1.0F, alpha, alpha);
            } else if ("blend".equals(function)) {
                assert (n == 4);
                Color  bg = parseColor2(tokenizer.nextToken());
                Color  fg = parseColor2(tokenizer.nextToken());
                float alpha = Float.parseFloat(tokenizer.nextToken());
                if (alpha > 1.0f) {
                    alpha = 1.0f / alpha;
                }

                return new Color((int)(bg.getRed() + ((fg.getRed() - bg.getRed()) * alpha)),
                                 (int)(bg.getRed() + ((fg.getRed() - bg.getRed()) * alpha)),
                                 (int)(bg.getRed() + ((fg.getRed() - bg.getRed()) * alpha)));
            } else {
                System.err.println("Unknown Metacity color function="+str);
                return null;
            }
        } else {
            return parseColor2(str);
        }
    }

    protected Color parseColor2(String str) {
        Color c = null;
        if (str.startsWith("gtk:")) {
            int i1 = str.indexOf('[');
            if (i1 > 3) {
                String typeStr = str.substring(4, i1).toLowerCase();
                int i2 = str.indexOf(']');
                if (i2 > i1+1) {
                    String stateStr = str.substring(i1+1, i2).toUpperCase();
                    int state = -1;
                    if ("ACTIVE".equals(stateStr)) {
                        state = PRESSED;
                    } else if ("INSENSITIVE".equals(stateStr)) {
                        state = DISABLED;
                    } else if ("NORMAL".equals(stateStr)) {
                        state = ENABLED;
                    } else if ("PRELIGHT".equals(stateStr)) {
                        state = MOUSE_OVER;
                    } else if ("SELECTED".equals(stateStr)) {
                        state = SELECTED;
                    }
                    ColorType type = null;
                    if ("fg".equals(typeStr)) {
                        type = GTKColorType.FOREGROUND;
                    } else if ("bg".equals(typeStr)) {
                        type = GTKColorType.BACKGROUND;
                    } else if ("base".equals(typeStr)) {
                        type = GTKColorType.TEXT_BACKGROUND;
                    } else if ("text".equals(typeStr)) {
                        type = GTKColorType.TEXT_FOREGROUND;
                    } else if ("dark".equals(typeStr)) {
                        type = GTKColorType.DARK;
                    } else if ("light".equals(typeStr)) {
                        type = GTKColorType.LIGHT;
                    }
                    if (state >= 0 && type != null) {
                        c = ((GTKStyle)context.getStyle()).getGTKColor(context, state, type);
                    }
                }
            }
        }
        if (c == null) {
            c = parseColorString(str);
        }
        return c;
    }

    private static Color parseColorString(String str) {
        if (str.charAt(0) == '#') {
            str = str.substring(1);

            int i = str.length();

            if (i < 3 || i > 12 || (i % 3) != 0) {
                return null;
            }

            i /= 3;

            int r;
            int g;
            int b;

            try {
                r = Integer.parseInt(str.substring(0, i), 16);
                g = Integer.parseInt(str.substring(i, i * 2), 16);
                b = Integer.parseInt(str.substring(i * 2, i * 3), 16);
            } catch (NumberFormatException nfe) {
                return null;
            }

            if (i == 4) {
                return new ColorUIResource(r / 65535.0f, g / 65535.0f, b / 65535.0f);
            } else if (i == 1) {
                return new ColorUIResource(r / 15.0f, g / 15.0f, b / 15.0f);
            } else if (i == 2) {
                return new ColorUIResource(r, g, b);
            } else {
                return new ColorUIResource(r / 4095.0f, g / 4095.0f, b / 4095.0f);
            }
        } else {
            return XColors.lookupColor(str);
        }
    }

    class ArithmeticExpressionEvaluator {
        private PeekableStringTokenizer tokenizer;

        int evaluate(String expr) {
            tokenizer = new PeekableStringTokenizer(expr, " \t+-*/%()", true);
            return Math.round(expression());
        }

        int evaluate(String expr, int fallback) {
            return (expr != null) ? evaluate(expr) : fallback;
        }

        public float expression() {
            float value = getTermValue();
            boolean done = false;
            while (!done && tokenizer.hasMoreTokens()) {
                String next = tokenizer.peek();
                if ("+".equals(next) ||
                    "-".equals(next) ||
                    "`max`".equals(next) ||
                    "`min`".equals(next)) {
                    tokenizer.nextToken();
                    float value2 = getTermValue();
                    if ("+".equals(next)) {
                        value += value2;
                    } else if ("-".equals(next)) {
                        value -= value2;
                    } else if ("`max`".equals(next)) {
                        value = Math.max(value, value2);
                    } else if ("`min`".equals(next)) {
                        value = Math.min(value, value2);
                    }
                } else {
                    done = true;
                }
            }
            return value;
        }

        public float getTermValue() {
            float value = getFactorValue();
            boolean done = false;
            while (!done && tokenizer.hasMoreTokens()) {
                String next = tokenizer.peek();
                if ("*".equals(next) || "/".equals(next) || "%".equals(next)) {
                    tokenizer.nextToken();
                    float value2 = getFactorValue();
                    if ("*".equals(next)) {
                        value *= value2;
                    } else if ("/".equals(next)) {
                        value /= value2;
                    } else {
                        value %= value2;
                    }
                } else {
                    done = true;
                }
            }
            return value;
        }

        public float getFactorValue() {
            float value;
            if ("(".equals(tokenizer.peek())) {
                tokenizer.nextToken();
                value = expression();
                tokenizer.nextToken(); // skip right paren
            } else {
                String token = tokenizer.nextToken();
                if (Character.isDigit(token.charAt(0))) {
                    value = Float.parseFloat(token);
                } else {
                    Integer i = variables.get(token);
                    if (i == null) {
                        i = (Integer)getFrameGeometry().get(token);
                    }
                    if (i == null) {
                        logError(themeName, "Variable \"" + token + "\" not defined");
                        return 0;
                    }
                    value = (i != null) ? i.intValue() : 0F;
                }
            }
            return value;
        }


    }

    static class PeekableStringTokenizer extends StringTokenizer {
        String token = null;

        public PeekableStringTokenizer(String str, String delim,
                                       boolean returnDelims) {
            super(str, delim, returnDelims);
            peek();
        }

        public String peek() {
            if (token == null) {
                token = nextToken();
            }
            return token;
        }

        public boolean hasMoreTokens() {
            return (token != null || super.hasMoreTokens());
        }

        public String nextToken() {
            if (token != null) {
                String t = token;
                token = null;
                if (hasMoreTokens()) {
                    peek();
                }
                return t;
            } else {
                String token = super.nextToken();
                while ((token.equals(" ") || token.equals("\t"))
                       && hasMoreTokens()) {
                    token = super.nextToken();
                }
                return token;
            }
        }
    }


    static class RoundRectClipShape extends RectangularShape {
        static final int TOP_LEFT = 1;
        static final int TOP_RIGHT = 2;
        static final int BOTTOM_LEFT = 4;
        static final int BOTTOM_RIGHT = 8;

        int x;
        int y;
        int width;
        int height;
        int arcwidth;
        int archeight;
        int corners;

        public RoundRectClipShape() {
        }

        public RoundRectClipShape(int x, int y, int w, int h,
                                  int arcw, int arch, int corners) {
            setRoundedRect(x, y, w, h, arcw, arch, corners);
        }

        public void setRoundedRect(int x, int y, int w, int h,
                                   int arcw, int arch, int corners) {
            this.corners = corners;
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.arcwidth = arcw;
            this.archeight = arch;
        }

        public double getX() {
            return (double)x;
        }

        public double getY() {
            return (double)y;
        }

        public double getWidth() {
            return (double)width;
        }

        public double getHeight() {
            return (double)height;
        }

        public double getArcWidth() {
            return (double)arcwidth;
        }

        public double getArcHeight() {
            return (double)archeight;
        }

        public boolean isEmpty() {
            return false;  // Not called
        }

        public Rectangle2D getBounds2D() {
            return null;  // Not called
        }

        public int getCornerFlags() {
            return corners;
        }

        public void setFrame(double x, double y, double w, double h) {
            // Not called
        }

        public boolean contains(double x, double y) {
            return false;  // Not called
        }

        private int classify(double coord, double left, double right, double arcsize) {
            return 0;  // Not called
        }

        public boolean intersects(double x, double y, double w, double h) {
            return false;  // Not called
        }

        public boolean contains(double x, double y, double w, double h) {
            return false;  // Not called
        }

        public PathIterator getPathIterator(AffineTransform at) {
            return new RoundishRectIterator(this, at);
        }


        static class RoundishRectIterator implements PathIterator {
            double x, y, w, h, aw, ah;
            AffineTransform affine;
            int index;

            double[][] ctrlpts;
            int[] types;

            private static final double angle = Math.PI / 4.0;
            private static final double a = 1.0 - Math.cos(angle);
            private static final double b = Math.tan(angle);
            private static final double c = Math.sqrt(1.0 + b * b) - 1 + a;
            private static final double cv = 4.0 / 3.0 * a * b / c;
            private static final double acv = (1.0 - cv) / 2.0;

            // For each array:
            //     4 values for each point {v0, v1, v2, v3}:
            //         point = (x + v0 * w + v1 * arcWidth,
            //                  y + v2 * h + v3 * arcHeight);
            private static final double[][] CtrlPtTemplate = {
                {  0.0,  0.0,  1.0,  0.0 },     /* BOTTOM LEFT corner */
                {  0.0,  0.0,  1.0, -0.5 },     /* BOTTOM LEFT arc start */
                {  0.0,  0.0,  1.0, -acv,       /* BOTTOM LEFT arc curve */
                   0.0,  acv,  1.0,  0.0,
                   0.0,  0.5,  1.0,  0.0 },
                {  1.0,  0.0,  1.0,  0.0 },     /* BOTTOM RIGHT corner */
                {  1.0, -0.5,  1.0,  0.0 },     /* BOTTOM RIGHT arc start */
                {  1.0, -acv,  1.0,  0.0,       /* BOTTOM RIGHT arc curve */
                   1.0,  0.0,  1.0, -acv,
                   1.0,  0.0,  1.0, -0.5 },
                {  1.0,  0.0,  0.0,  0.0 },     /* TOP RIGHT corner */
                {  1.0,  0.0,  0.0,  0.5 },     /* TOP RIGHT arc start */
                {  1.0,  0.0,  0.0,  acv,       /* TOP RIGHT arc curve */
                   1.0, -acv,  0.0,  0.0,
                   1.0, -0.5,  0.0,  0.0 },
                {  0.0,  0.0,  0.0,  0.0 },     /* TOP LEFT corner */
                {  0.0,  0.5,  0.0,  0.0 },     /* TOP LEFT arc start */
                {  0.0,  acv,  0.0,  0.0,       /* TOP LEFT arc curve */
                   0.0,  0.0,  0.0,  acv,
                   0.0,  0.0,  0.0,  0.5 },
                {},                             /* Closing path element */
            };
            private static final int[] CornerFlags = {
                RoundRectClipShape.BOTTOM_LEFT,
                RoundRectClipShape.BOTTOM_RIGHT,
                RoundRectClipShape.TOP_RIGHT,
                RoundRectClipShape.TOP_LEFT,
            };

            RoundishRectIterator(RoundRectClipShape rr, AffineTransform at) {
                this.x = rr.getX();
                this.y = rr.getY();
                this.w = rr.getWidth();
                this.h = rr.getHeight();
                this.aw = Math.min(w, Math.abs(rr.getArcWidth()));
                this.ah = Math.min(h, Math.abs(rr.getArcHeight()));
                this.affine = at;
                if (w < 0 || h < 0) {
                    // Don't draw anything...
                    ctrlpts = new double[0][];
                    types = new int[0];
                } else {
                    int corners = rr.getCornerFlags();
                    int numedges = 5;  // 4xCORNER_POINT, CLOSE
                    for (int i = 1; i < 0x10; i <<= 1) {
                        // Add one for each corner that has a curve
                        if ((corners & i) != 0) numedges++;
                    }
                    ctrlpts = new double[numedges][];
                    types = new int[numedges];
                    int j = 0;
                    for (int i = 0; i < 4; i++) {
                        types[j] = SEG_LINETO;
                        if ((corners & CornerFlags[i]) == 0) {
                            ctrlpts[j++] = CtrlPtTemplate[i*3+0];
                        } else {
                            ctrlpts[j++] = CtrlPtTemplate[i*3+1];
                            types[j] = SEG_CUBICTO;
                            ctrlpts[j++] = CtrlPtTemplate[i*3+2];
                        }
                    }
                    types[j] = SEG_CLOSE;
                    ctrlpts[j++] = CtrlPtTemplate[12];
                    types[0] = SEG_MOVETO;
                }
            }

            public int getWindingRule() {
                return WIND_NON_ZERO;
            }

            public boolean isDone() {
                return index >= ctrlpts.length;
            }

            public void next() {
                index++;
            }

            public int currentSegment(float[] coords) {
                if (isDone()) {
                    throw new NoSuchElementException("roundrect iterator out of bounds");
                }
                double[] ctrls = ctrlpts[index];
                int nc = 0;
                for (int i = 0; i < ctrls.length; i += 4) {
                    coords[nc++] = (float) (x + ctrls[i + 0] * w + ctrls[i + 1] * aw);
                    coords[nc++] = (float) (y + ctrls[i + 2] * h + ctrls[i + 3] * ah);
                }
                if (affine != null) {
                    affine.transform(coords, 0, coords, 0, nc / 2);
                }
                return types[index];
            }

            public int currentSegment(double[] coords) {
                if (isDone()) {
                    throw new NoSuchElementException("roundrect iterator out of bounds");
                }
                double[] ctrls = ctrlpts[index];
                int nc = 0;
                for (int i = 0; i < ctrls.length; i += 4) {
                    coords[nc++] = x + ctrls[i + 0] * w + ctrls[i + 1] * aw;
                    coords[nc++] = y + ctrls[i + 2] * h + ctrls[i + 3] * ah;
                }
                if (affine != null) {
                    affine.transform(coords, 0, coords, 0, nc / 2);
                }
                return types[index];
            }
        }
    }
}
