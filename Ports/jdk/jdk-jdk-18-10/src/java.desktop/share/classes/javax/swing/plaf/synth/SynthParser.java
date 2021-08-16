/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.synth;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.PatternSyntaxException;

import javax.swing.ImageIcon;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.beans.decoder.DocumentHandler;
import sun.reflect.misc.ReflectUtil;

class SynthParser extends DefaultHandler {
    //
    // Known element names
    //
    private static final String ELEMENT_SYNTH = "synth";
    private static final String ELEMENT_STYLE = "style";
    private static final String ELEMENT_STATE = "state";
    private static final String ELEMENT_FONT = "font";
    private static final String ELEMENT_COLOR = "color";
    private static final String ELEMENT_IMAGE_PAINTER = "imagePainter";
    private static final String ELEMENT_PAINTER = "painter";
    private static final String ELEMENT_PROPERTY = "property";
    private static final String ELEMENT_SYNTH_GRAPHICS = "graphicsUtils";
    private static final String ELEMENT_IMAGE_ICON = "imageIcon";
    private static final String ELEMENT_BIND = "bind";
    private static final String ELEMENT_BIND_KEY = "bindKey";
    private static final String ELEMENT_INSETS = "insets";
    private static final String ELEMENT_OPAQUE = "opaque";
    private static final String ELEMENT_DEFAULTS_PROPERTY =
                                        "defaultsProperty";
    private static final String ELEMENT_INPUT_MAP = "inputMap";

    //
    // Known attribute names
    //
    private static final String ATTRIBUTE_ACTION = "action";
    private static final String ATTRIBUTE_ID = "id";
    private static final String ATTRIBUTE_IDREF = "idref";
    private static final String ATTRIBUTE_CLONE = "clone";
    private static final String ATTRIBUTE_VALUE = "value";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_STYLE = "style";
    private static final String ATTRIBUTE_SIZE = "size";
    private static final String ATTRIBUTE_TYPE = "type";
    private static final String ATTRIBUTE_TOP = "top";
    private static final String ATTRIBUTE_LEFT = "left";
    private static final String ATTRIBUTE_BOTTOM = "bottom";
    private static final String ATTRIBUTE_RIGHT = "right";
    private static final String ATTRIBUTE_KEY = "key";
    private static final String ATTRIBUTE_SOURCE_INSETS = "sourceInsets";
    private static final String ATTRIBUTE_DEST_INSETS = "destinationInsets";
    private static final String ATTRIBUTE_PATH = "path";
    private static final String ATTRIBUTE_STRETCH = "stretch";
    private static final String ATTRIBUTE_PAINT_CENTER = "paintCenter";
    private static final String ATTRIBUTE_METHOD = "method";
    private static final String ATTRIBUTE_DIRECTION = "direction";
    private static final String ATTRIBUTE_CENTER = "center";

    /**
     * Lazily created, used for anything we don't understand.
     */
    private DocumentHandler _handler;

    /**
     * Indicates the depth of how many elements we've encountered but don't
     * understand. This is used when forwarding to beans persistance to know
     * when we hsould stop forwarding.
     */
    private int _depth;

    /**
     * Factory that new styles are added to.
     */
    private DefaultSynthStyleFactory _factory;

    /**
     * Array of state infos for the current style. These are pushed to the
     * style when </style> is received.
     */
    private List<ParsedSynthStyle.StateInfo> _stateInfos;

    /**
     * Current style.
     */
    private ParsedSynthStyle _style;

    /**
     * Current state info.
     */
    private ParsedSynthStyle.StateInfo _stateInfo;

    /**
     * Bindings for the current InputMap
     */
    private List<String> _inputMapBindings;

    /**
     * ID for the input map. This is cached as
     * the InputMap is created AFTER the inputMapProperty has ended.
     */
    private String _inputMapID;

    /**
     * Object references outside the scope of persistance.
     */
    private Map<String,Object> _mapping;

    /**
     * Based URL used to resolve paths.
     */
    private URL _urlResourceBase;

    /**
     * Based class used to resolve paths.
     */
    private Class<?> _classResourceBase;

    /**
     * List of ColorTypes. This is populated in startColorType.
     */
    private List<ColorType> _colorTypes;

    /**
     * defaultsPropertys are placed here.
     */
    private Map<String, Object> _defaultsMap;

    /**
     * List of SynthStyle.Painters that will be applied to the current style.
     */
    private List<ParsedSynthStyle.PainterInfo> _stylePainters;

    /**
     * List of SynthStyle.Painters that will be applied to the current state.
     */
    private List<ParsedSynthStyle.PainterInfo> _statePainters;

    SynthParser() {
        _mapping = new HashMap<String,Object>();
        _stateInfos = new ArrayList<ParsedSynthStyle.StateInfo>();
        _colorTypes = new ArrayList<ColorType>();
        _inputMapBindings = new ArrayList<String>();
        _stylePainters = new ArrayList<ParsedSynthStyle.PainterInfo>();
        _statePainters = new ArrayList<ParsedSynthStyle.PainterInfo>();
    }

    /**
     * Parses a set of styles from <code>inputStream</code>, adding the
     * resulting styles to the passed in DefaultSynthStyleFactory.
     * Resources are resolved either from a URL or from a Class. When calling
     * this method, one of the URL or the Class must be null but not both at
     * the same time.
     *
     * @param inputStream XML document containing the styles to read
     * @param factory DefaultSynthStyleFactory that new styles are added to
     * @param urlResourceBase the URL used to resolve any resources, such as Images
     * @param classResourceBase the Class used to resolve any resources, such as Images
     * @param defaultsMap Map that UIDefaults properties are placed in
     */
    public void parse(InputStream inputStream,
                      DefaultSynthStyleFactory factory,
                      URL urlResourceBase, Class<?> classResourceBase,
                      Map<String, Object> defaultsMap)
                      throws ParseException, IllegalArgumentException {
        if (inputStream == null || factory == null ||
            (urlResourceBase == null && classResourceBase == null)) {
            throw new IllegalArgumentException(
                "You must supply an InputStream, StyleFactory and Class or URL");
        }

        assert(!(urlResourceBase != null && classResourceBase != null));

        _factory = factory;
        _classResourceBase = classResourceBase;
        _urlResourceBase = urlResourceBase;
        _defaultsMap = defaultsMap;
        try {
            try {
                SAXParser saxParser = SAXParserFactory.newInstance().
                                                   newSAXParser();
                saxParser.parse(new BufferedInputStream(inputStream), this);
            } catch (ParserConfigurationException e) {
                throw new ParseException("Error parsing: " + e, 0);
            }
            catch (SAXException se) {
                throw new ParseException("Error parsing: " + se + " " +
                                         se.getException(), 0);
            }
            catch (IOException ioe) {
                throw new ParseException("Error parsing: " + ioe, 0);
            }
        } finally {
            reset();
        }
    }

    /**
     * Returns the path to a resource.
     */
    private URL getResource(String path) {
        if (_classResourceBase != null) {
            return _classResourceBase.getResource(path);
        } else {
            try {
                return new URL(_urlResourceBase, path);
            } catch (MalformedURLException mue) {
                return null;
            }
        }
    }

    /**
     * Clears our internal state.
     */
    private void reset() {
        _handler = null;
        _depth = 0;
        _mapping.clear();
        _stateInfos.clear();
        _colorTypes.clear();
        _statePainters.clear();
        _stylePainters.clear();
    }

    /**
     * Returns true if we are forwarding to persistance.
     */
    private boolean isForwarding() {
        return (_depth > 0);
    }

    /**
     * Handles beans persistance.
     */
    private DocumentHandler getHandler() {
        if (_handler == null) {
            _handler = new DocumentHandler();
            if (_urlResourceBase != null) {
                // getHandler() is never called before parse() so it is safe
                // to create a URLClassLoader with _resourceBase.
                //
                // getResource(".") is called to ensure we have the directory
                // containing the resources in the case the resource base is a
                // .class file.
                URL[] urls = new URL[] { getResource(".") };
                ClassLoader parent = Thread.currentThread().getContextClassLoader();
                ClassLoader urlLoader = new URLClassLoader(urls, parent);
                _handler.setClassLoader(urlLoader);
            } else {
                _handler.setClassLoader(_classResourceBase.getClassLoader());
            }

            for (String key : _mapping.keySet()) {
                _handler.setVariable(key, _mapping.get(key));
            }
        }
        return _handler;
    }

    /**
     * If <code>value</code> is an instance of <code>type</code> it is
     * returned, otherwise a SAXException is thrown.
     */
    private Object checkCast(Object value, Class<?> type) throws SAXException {
        if (!type.isInstance(value)) {
            throw new SAXException("Expected type " + type + " got " +
                                   value.getClass());
        }
        return value;
    }

    /**
     * Returns an object created with id=key. If the object is not of
     * type type, this will throw an exception.
     */
    private Object lookup(String key, Class<?> type) throws SAXException {
        Object value;
        if (_handler != null) {
            if (_handler.hasVariable(key)) {
                return checkCast(_handler.getVariable(key), type);
            }
        }
        value = _mapping.get(key);
        if (value == null) {
            throw new SAXException("ID " + key + " has not been defined");
        }
        return checkCast(value, type);
    }

    /**
     * Registers an object by name. This will throw an exception if an
     * object has already been registered under the given name.
     */
    private void register(String key, Object value) throws SAXException {
        if (key != null) {
            if (_mapping.get(key) != null ||
                     (_handler != null && _handler.hasVariable(key))) {
                throw new SAXException("ID " + key + " is already defined");
            }
            if (_handler != null) {
                _handler.setVariable(key, value);
            }
            else {
                _mapping.put(key, value);
            }
        }
    }

    /**
     * Convenience method to return the next int, or throw if there are no
     * more valid ints.
     */
    private int nextInt(StringTokenizer tok, String errorMsg) throws
                   SAXException {
        if (!tok.hasMoreTokens()) {
            throw new SAXException(errorMsg);
        }
        try {
            return Integer.parseInt(tok.nextToken());
        } catch (NumberFormatException nfe) {
            throw new SAXException(errorMsg);
        }
    }

    /**
     * Convenience method to return an Insets object.
     */
    private Insets parseInsets(String insets, String errorMsg) throws
                        SAXException {
        StringTokenizer tokenizer = new StringTokenizer(insets);
        return new Insets(nextInt(tokenizer, errorMsg),
                          nextInt(tokenizer, errorMsg),
                          nextInt(tokenizer, errorMsg),
                          nextInt(tokenizer, errorMsg));
    }



    //
    // The following methods are invoked from startElement/stopElement
    //

    private void startStyle(Attributes attributes) throws SAXException {
        String id = null;

        _style = null;
        for(int i = attributes.getLength() - 1; i >= 0; i--) {
            String key = attributes.getQName(i);
            if (key.equals(ATTRIBUTE_CLONE)) {
                _style = (ParsedSynthStyle)((ParsedSynthStyle)lookup(
                         attributes.getValue(i), ParsedSynthStyle.class)).
                         clone();
            }
            else if (key.equals(ATTRIBUTE_ID)) {
                id = attributes.getValue(i);
            }
        }
        if (_style == null) {
            _style = new ParsedSynthStyle();
        }
        register(id, _style);
    }

    private void endStyle() {
        int size = _stylePainters.size();
        if (size > 0) {
            _style.setPainters(_stylePainters.toArray(new ParsedSynthStyle.PainterInfo[size]));
            _stylePainters.clear();
        }
        size = _stateInfos.size();
        if (size > 0) {
            _style.setStateInfo(_stateInfos.toArray(new ParsedSynthStyle.StateInfo[size]));
            _stateInfos.clear();
        }
        _style = null;
    }

    private void startState(Attributes attributes) throws SAXException {
        ParsedSynthStyle.StateInfo stateInfo = null;
        int state = 0;
        String id = null;

        _stateInfo = null;
        for(int i = attributes.getLength() - 1; i >= 0; i--) {
            String key = attributes.getQName(i);
            if (key.equals(ATTRIBUTE_ID)) {
                id = attributes.getValue(i);
            }
            else if (key.equals(ATTRIBUTE_IDREF)) {
                _stateInfo = (ParsedSynthStyle.StateInfo)lookup(
                   attributes.getValue(i), ParsedSynthStyle.StateInfo.class);
            }
            else if (key.equals(ATTRIBUTE_CLONE)) {
                _stateInfo = (ParsedSynthStyle.StateInfo)((ParsedSynthStyle.
                             StateInfo)lookup(attributes.getValue(i),
                             ParsedSynthStyle.StateInfo.class)).clone();
            }
            else if (key.equals(ATTRIBUTE_VALUE)) {
                StringTokenizer tokenizer = new StringTokenizer(
                                   attributes.getValue(i));
                while (tokenizer.hasMoreTokens()) {
                    String stateString = tokenizer.nextToken().toUpperCase().
                                                   intern();
                    if (stateString == "ENABLED") {
                        state |= SynthConstants.ENABLED;
                    }
                    else if (stateString == "MOUSE_OVER") {
                        state |= SynthConstants.MOUSE_OVER;
                    }
                    else if (stateString == "PRESSED") {
                        state |= SynthConstants.PRESSED;
                    }
                    else if (stateString == "DISABLED") {
                        state |= SynthConstants.DISABLED;
                    }
                    else if (stateString == "FOCUSED") {
                        state |= SynthConstants.FOCUSED;
                    }
                    else if (stateString == "SELECTED") {
                        state |= SynthConstants.SELECTED;
                    }
                    else if (stateString == "DEFAULT") {
                        state |= SynthConstants.DEFAULT;
                    }
                    else if (stateString != "AND") {
                        throw new SAXException("Unknown state: " + state);
                    }
                }
            }
        }
        if (_stateInfo == null) {
            _stateInfo = new ParsedSynthStyle.StateInfo();
        }
        _stateInfo.setComponentState(state);
        register(id, _stateInfo);
        _stateInfos.add(_stateInfo);
    }

    private void endState() {
        int size = _statePainters.size();
        if (size > 0) {
            _stateInfo.setPainters(_statePainters.toArray(new ParsedSynthStyle.PainterInfo[size]));
            _statePainters.clear();
        }
        _stateInfo = null;
    }

    private void startFont(Attributes attributes) throws SAXException {
        Font font = null;
        int style = Font.PLAIN;
        int size = 0;
        String id = null;
        String name = null;

        for(int i = attributes.getLength() - 1; i >= 0; i--) {
            String key = attributes.getQName(i);
            if (key.equals(ATTRIBUTE_ID)) {
                id = attributes.getValue(i);
            }
            else if (key.equals(ATTRIBUTE_IDREF)) {
                font = (Font)lookup(attributes.getValue(i), Font.class);
            }
            else if (key.equals(ATTRIBUTE_NAME)) {
                name = attributes.getValue(i);
            }
            else if (key.equals(ATTRIBUTE_SIZE)) {
                try {
                    size = Integer.parseInt(attributes.getValue(i));
                } catch (NumberFormatException nfe) {
                    throw new SAXException("Invalid font size: " +
                                           attributes.getValue(i));
                }
            }
            else if (key.equals(ATTRIBUTE_STYLE)) {
                StringTokenizer tok = new StringTokenizer(
                                                attributes.getValue(i));
                while (tok.hasMoreTokens()) {
                    String token = tok.nextToken().intern();
                    if (token == "BOLD") {
                        style = ((style | Font.PLAIN) ^ Font.PLAIN) |
                                Font.BOLD;
                    }
                    else if (token == "ITALIC") {
                        style |= Font.ITALIC;
                    }
                }
            }
        }
        if (font == null) {
            if (name == null) {
                throw new SAXException("You must define a name for the font");
            }
            if (size == 0) {
                throw new SAXException("You must define a size for the font");
            }
            font = new FontUIResource(name, style, size);
        }
        else if (name != null || size != 0 || style != Font.PLAIN) {
            throw new SAXException("Name, size and style are not for use " +
                                   "with idref");
        }
        register(id, font);
        if (_stateInfo != null) {
            _stateInfo.setFont(font);
        }
        else if (_style != null) {
            _style.setFont(font);
        }
    }

    private void startColor(Attributes attributes) throws SAXException {
        Color color = null;
        String id = null;

        _colorTypes.clear();
        for(int i = attributes.getLength() - 1; i >= 0; i--) {
            String key = attributes.getQName(i);
            if (key.equals(ATTRIBUTE_ID)) {
                id = attributes.getValue(i);
            }
            else if (key.equals(ATTRIBUTE_IDREF)) {
                color = (Color)lookup(attributes.getValue(i), Color.class);
            }
            else if (key.equals(ATTRIBUTE_NAME)) {
            }
            else if (key.equals(ATTRIBUTE_VALUE)) {
                String value = attributes.getValue(i);

                if (value.startsWith("#")) {
                    try {
                        int argb;
                        boolean hasAlpha;

                        int length = value.length();
                        if (length < 8) {
                            // Just RGB, or some portion of it.
                            argb = Integer.decode(value);
                            hasAlpha = false;
                        } else if (length == 8) {
                            // Single character alpha: #ARRGGBB.
                            argb = Integer.decode(value);
                            hasAlpha = true;
                        } else if (length == 9) {
                            // Color has alpha and is of the form
                            // #AARRGGBB.
                            // The following split decoding is mandatory due to
                            // Integer.decode() behavior which won't decode
                            // hexadecimal values higher than #7FFFFFFF.
                            // Thus, when an alpha channel is detected, it is
                            // decoded separately from the RGB channels.
                            int rgb = Integer.decode('#' +
                                                     value.substring(3, 9));
                            int a = Integer.decode(value.substring(0, 3));
                            argb = (a << 24) | rgb;
                            hasAlpha = true;
                        } else {
                            throw new SAXException("Invalid Color value: "
                                + value);
                        }

                        color = new ColorUIResource(new Color(argb, hasAlpha));
                    } catch (NumberFormatException nfe) {
                        throw new SAXException("Invalid Color value: " +value);
                    }
                }
                else {
                    try {
                        color = new ColorUIResource((Color)Color.class.
                              getField(value.toUpperCase()).get(Color.class));
                    } catch (NoSuchFieldException nsfe) {
                        throw new SAXException("Invalid color name: " + value);
                    } catch (IllegalAccessException iae) {
                        throw new SAXException("Invalid color name: " + value);
                    }
                }
            }
            else if (key.equals(ATTRIBUTE_TYPE)) {
                StringTokenizer tokenizer = new StringTokenizer(
                                   attributes.getValue(i));
                while (tokenizer.hasMoreTokens()) {
                    String typeName = tokenizer.nextToken();
                    int classIndex = typeName.lastIndexOf('.');
                    Class<?> typeClass;

                    if (classIndex == -1) {
                        typeClass = ColorType.class;
                        classIndex = 0;
                    }
                    else {
                        try {
                            typeClass = ReflectUtil.forName(typeName.substring(
                                                      0, classIndex));
                        } catch (ClassNotFoundException cnfe) {
                            throw new SAXException("Unknown class: " +
                                      typeName.substring(0, classIndex));
                        }
                        classIndex++;
                    }
                    try {
                        _colorTypes.add((ColorType)checkCast(typeClass.
                              getField(typeName.substring(classIndex)).
                              get(typeClass), ColorType.class));
                    } catch (NoSuchFieldException nsfe) {
                        throw new SAXException("Unable to find color type: " +
                                               typeName);
                    } catch (IllegalAccessException iae) {
                        throw new SAXException("Unable to find color type: " +
                                               typeName);
                    }
                }
            }
        }
        if (color == null) {
            throw new SAXException("color: you must specificy a value");
        }
        register(id, color);
        if (_stateInfo != null && _colorTypes.size() > 0) {
            Color[] colors = _stateInfo.getColors();
            int max = 0;
            for (int counter = _colorTypes.size() - 1; counter >= 0;
                     counter--) {
                max = Math.max(max, _colorTypes.get(counter).getID());
            }
            if (colors == null || colors.length <= max) {
                Color[] newColors = new Color[max + 1];
                if (colors != null) {
                    System.arraycopy(colors, 0, newColors, 0, colors.length);
                }
                colors = newColors;
            }
            for (int counter = _colorTypes.size() - 1; counter >= 0;
                     counter--) {
                colors[_colorTypes.get(counter).getID()] = color;
            }
            _stateInfo.setColors(colors);
        }
    }

    private void startProperty(Attributes attributes,
                               Object property) throws SAXException {
        Object value = null;
        String key = null;
        // Type of the value: 0=idref, 1=boolean, 2=dimension, 3=insets,
        // 4=integer,5=string
        int iType = 0;
        String aValue = null;

        for(int i = attributes.getLength() - 1; i >= 0; i--) {
            String aName = attributes.getQName(i);
            if (aName.equals(ATTRIBUTE_TYPE)) {
                String type = attributes.getValue(i).toUpperCase();
                if (type.equals("IDREF")) {
                    iType = 0;
                }
                else if (type.equals("BOOLEAN")) {
                    iType = 1;
                }
                else if (type.equals("DIMENSION")) {
                    iType = 2;
                }
                else if (type.equals("INSETS")) {
                    iType = 3;
                }
                else if (type.equals("INTEGER")) {
                    iType = 4;
                }
                else if (type.equals("STRING")) {
                    iType = 5;
                }
                else {
                    throw new SAXException(property + " unknown type, use" +
                        "idref, boolean, dimension, insets or integer");
                }
            }
            else if (aName.equals(ATTRIBUTE_VALUE)) {
                aValue = attributes.getValue(i);
            }
            else if (aName.equals(ATTRIBUTE_KEY)) {
                key = attributes.getValue(i);
            }
        }
        if (aValue != null) {
            switch (iType) {
            case 0: // idref
                value = lookup(aValue, Object.class);
                break;
            case 1: // boolean
                value = Boolean.parseBoolean(aValue);
                break;
            case 2: // dimension
                StringTokenizer tok = new StringTokenizer(aValue);
                value = new DimensionUIResource(
                    nextInt(tok, "Invalid dimension"),
                    nextInt(tok, "Invalid dimension"));
                break;
            case 3: // insets
                value = parseInsets(aValue, property + " invalid insets");
                break;
            case 4: // integer
                try {
                    value = Integer.valueOf(aValue);
                } catch (NumberFormatException nfe) {
                    throw new SAXException(property + " invalid value");
                }
                break;
            case 5: //string
                value = aValue;
                break;
            }
        }
        if (value == null || key == null) {
            throw new SAXException(property + ": you must supply a " +
                                   "key and value");
        }
        if (property == ELEMENT_DEFAULTS_PROPERTY) {
            _defaultsMap.put(key, value);
        }
        else if (_stateInfo != null) {
            if (_stateInfo.getData() == null) {
                _stateInfo.setData(new HashMap<>());
            }
            _stateInfo.getData().put(key, value);
        }
        else if (_style != null) {
            if (_style.getData() == null) {
                _style.setData(new HashMap<>());
            }
            _style.getData().put(key, value);
        }
    }

    private void startGraphics(Attributes attributes) throws SAXException {
        SynthGraphicsUtils graphics = null;

        for(int i = attributes.getLength() - 1; i >= 0; i--) {
            String key = attributes.getQName(i);
            if (key.equals(ATTRIBUTE_IDREF)) {
                graphics = (SynthGraphicsUtils)lookup(attributes.getValue(i),
                                                 SynthGraphicsUtils.class);
            }
        }
        if (graphics == null) {
            throw new SAXException("graphicsUtils: you must supply an idref");
        }
        if (_style != null) {
            _style.setGraphicsUtils(graphics);
        }
    }

    private void startInsets(Attributes attributes) throws SAXException {
        int top = 0;
        int bottom = 0;
        int left = 0;
        int right = 0;
        Insets insets = null;
        String id = null;

        for(int i = attributes.getLength() - 1; i >= 0; i--) {
            String key = attributes.getQName(i);

            try {
                if (key.equals(ATTRIBUTE_IDREF)) {
                    insets = (Insets)lookup(attributes.getValue(i),
                                                   Insets.class);
                }
                else if (key.equals(ATTRIBUTE_ID)) {
                    id = attributes.getValue(i);
                }
                else if (key.equals(ATTRIBUTE_TOP)) {
                    top = Integer.parseInt(attributes.getValue(i));
                }
                else if (key.equals(ATTRIBUTE_LEFT)) {
                    left = Integer.parseInt(attributes.getValue(i));
                }
                else if (key.equals(ATTRIBUTE_BOTTOM)) {
                    bottom = Integer.parseInt(attributes.getValue(i));
                }
                else if (key.equals(ATTRIBUTE_RIGHT)) {
                    right = Integer.parseInt(attributes.getValue(i));
                }
            } catch (NumberFormatException nfe) {
                throw new SAXException("insets: bad integer value for " +
                                       attributes.getValue(i));
            }
        }
        if (insets == null) {
            insets = new InsetsUIResource(top, left, bottom, right);
        }
        register(id, insets);
        if (_style != null) {
            _style.setInsets(insets);
        }
    }

    private void startBind(Attributes attributes) throws SAXException {
        ParsedSynthStyle style = null;
        String path = null;
        int type = -1;

        for(int i = attributes.getLength() - 1; i >= 0; i--) {
            String key = attributes.getQName(i);

            if (key.equals(ATTRIBUTE_STYLE)) {
                style = (ParsedSynthStyle)lookup(attributes.getValue(i),
                                                  ParsedSynthStyle.class);
            }
            else if (key.equals(ATTRIBUTE_TYPE)) {
                String typeS = attributes.getValue(i).toUpperCase();

                if (typeS.equals("NAME")) {
                    type = DefaultSynthStyleFactory.NAME;
                }
                else if (typeS.equals("REGION")) {
                    type = DefaultSynthStyleFactory.REGION;
                }
                else {
                    throw new SAXException("bind: unknown type " + typeS);
                }
            }
            else if (key.equals(ATTRIBUTE_KEY)) {
                path = attributes.getValue(i);
            }
        }
        if (style == null || path == null || type == -1) {
            throw new SAXException("bind: you must specify a style, type " +
                                   "and key");
        }
        try {
            _factory.addStyle(style, path, type);
        } catch (PatternSyntaxException pse) {
            throw new SAXException("bind: " + path + " is not a valid " +
                                   "regular expression");
        }
    }

    private void startPainter(Attributes attributes, String type) throws SAXException {
        Insets sourceInsets = null;
        Insets destInsets = null;
        String path = null;
        boolean paintCenter = true;
        boolean stretch = true;
        SynthPainter painter = null;
        String method = null;
        String id = null;
        int direction = -1;
        boolean center = false;

        boolean stretchSpecified = false;
        boolean paintCenterSpecified = false;

        for(int i = attributes.getLength() - 1; i >= 0; i--) {
            String key = attributes.getQName(i);
            String value = attributes.getValue(i);

            if (key.equals(ATTRIBUTE_ID)) {
                id = value;
            }
            else if (key.equals(ATTRIBUTE_METHOD)) {
                method = value.toLowerCase(Locale.ENGLISH);
            }
            else if (key.equals(ATTRIBUTE_IDREF)) {
                painter = (SynthPainter)lookup(value, SynthPainter.class);
            }
            else if (key.equals(ATTRIBUTE_PATH)) {
                path = value;
            }
            else if (key.equals(ATTRIBUTE_SOURCE_INSETS)) {
                sourceInsets = parseInsets(value, type +
                   ": sourceInsets must be top left bottom right");
            }
            else if (key.equals(ATTRIBUTE_DEST_INSETS)) {
                destInsets = parseInsets(value, type +
                  ": destinationInsets must be top left bottom right");
            }
            else if (key.equals(ATTRIBUTE_PAINT_CENTER)) {
                paintCenter = Boolean.parseBoolean(value);
                paintCenterSpecified = true;
            }
            else if (key.equals(ATTRIBUTE_STRETCH)) {
                stretch = Boolean.parseBoolean(value);
                stretchSpecified = true;
            }
            else if (key.equals(ATTRIBUTE_DIRECTION)) {
                value = value.toUpperCase().intern();
                if (value == "EAST") {
                    direction = SwingConstants.EAST;
                }
                else if (value == "NORTH") {
                    direction = SwingConstants.NORTH;
                }
                else if (value == "SOUTH") {
                    direction = SwingConstants.SOUTH;
                }
                else if (value == "WEST") {
                    direction = SwingConstants.WEST;
                }
                else if (value == "TOP") {
                    direction = SwingConstants.TOP;
                }
                else if (value == "LEFT") {
                    direction = SwingConstants.LEFT;
                }
                else if (value == "BOTTOM") {
                    direction = SwingConstants.BOTTOM;
                }
                else if (value == "RIGHT") {
                    direction = SwingConstants.RIGHT;
                }
                else if (value == "HORIZONTAL") {
                    direction = SwingConstants.HORIZONTAL;
                }
                else if (value == "VERTICAL") {
                    direction = SwingConstants.VERTICAL;
                }
                else if (value == "HORIZONTAL_SPLIT") {
                    direction = JSplitPane.HORIZONTAL_SPLIT;
                }
                else if (value == "VERTICAL_SPLIT") {
                    direction = JSplitPane.VERTICAL_SPLIT;
                }
                else {
                    throw new SAXException(type + ": unknown direction");
                }
            }
            else if (key.equals(ATTRIBUTE_CENTER)) {
                center = Boolean.parseBoolean(value);
            }
        }
        if (painter == null) {
            if (type == ELEMENT_PAINTER) {
                throw new SAXException(type +
                             ": you must specify an idref");
            }
            if (sourceInsets == null && !center) {
                throw new SAXException(
                             "property: you must specify sourceInsets");
            }
            if (path == null) {
                throw new SAXException("property: you must specify a path");
            }
            if (center && (sourceInsets != null || destInsets != null ||
                           paintCenterSpecified || stretchSpecified)) {
                throw new SAXException("The attributes: sourceInsets, " +
                                       "destinationInsets, paintCenter and stretch " +
                                       " are not legal when center is true");
            }
            painter = new ImagePainter(!stretch, paintCenter,
                     sourceInsets, destInsets, getResource(path), center);
        }
        register(id, painter);
        if (_stateInfo != null) {
            addPainterOrMerge(_statePainters, method, painter, direction);
        }
        else if (_style != null) {
            addPainterOrMerge(_stylePainters, method, painter, direction);
        }
    }

    private void addPainterOrMerge(List<ParsedSynthStyle.PainterInfo> painters, String method,
                                   SynthPainter painter, int direction) {
        ParsedSynthStyle.PainterInfo painterInfo;
        painterInfo = new ParsedSynthStyle.PainterInfo(method,
                                                       painter,
                                                       direction);

        for (Object infoObject: painters) {
            ParsedSynthStyle.PainterInfo info;
            info = (ParsedSynthStyle.PainterInfo) infoObject;

            if (painterInfo.equalsPainter(info)) {
                info.addPainter(painter);
                return;
            }
        }

        painters.add(painterInfo);
    }

    private void startImageIcon(Attributes attributes) throws SAXException {
        String path = null;
        String id = null;

        for(int i = attributes.getLength() - 1; i >= 0; i--) {
            String key = attributes.getQName(i);

            if (key.equals(ATTRIBUTE_ID)) {
                id = attributes.getValue(i);
            }
            else if (key.equals(ATTRIBUTE_PATH)) {
                path = attributes.getValue(i);
            }
        }
        if (path == null) {
            throw new SAXException("imageIcon: you must specify a path");
        }
        register(id, new LazyImageIcon(getResource(path)));
       }

    private void startOpaque(Attributes attributes) {
        if (_style != null) {
            _style.setOpaque(true);
            for(int i = attributes.getLength() - 1; i >= 0; i--) {
                String key = attributes.getQName(i);

                if (key.equals(ATTRIBUTE_VALUE)) {
                    _style.setOpaque("true".equals(attributes.getValue(i).
                                                   toLowerCase()));
                }
            }
        }
    }

    private void startInputMap(Attributes attributes) throws SAXException {
        _inputMapBindings.clear();
        _inputMapID = null;
        if (_style != null) {
            for(int i = attributes.getLength() - 1; i >= 0; i--) {
                String key = attributes.getQName(i);

                if (key.equals(ATTRIBUTE_ID)) {
                    _inputMapID = attributes.getValue(i);
                }
            }
        }
    }

    private void endInputMap() throws SAXException {
        if (_inputMapID != null) {
            register(_inputMapID, new UIDefaults.LazyInputMap(
                     _inputMapBindings.toArray(new Object[_inputMapBindings.
                     size()])));
        }
        _inputMapBindings.clear();
        _inputMapID = null;
    }

    private void startBindKey(Attributes attributes) throws SAXException {
        if (_inputMapID == null) {
            // Not in an inputmap, bail.
            return;
        }
        if (_style != null) {
            String key = null;
            String value = null;
            for(int i = attributes.getLength() - 1; i >= 0; i--) {
                String aKey = attributes.getQName(i);

                if (aKey.equals(ATTRIBUTE_KEY)) {
                    key = attributes.getValue(i);
                }
                else if (aKey.equals(ATTRIBUTE_ACTION)) {
                    value = attributes.getValue(i);
                }
            }
            if (key == null || value == null) {
                throw new SAXException(
                    "bindKey: you must supply a key and action");
            }
            _inputMapBindings.add(key);
            _inputMapBindings.add(value);
        }
    }

    //
    // SAX methods, these forward to the DocumentHandler if we don't know
    // the element name.
    //

    public InputSource resolveEntity(String publicId, String systemId)
                              throws IOException, SAXException {
        if (isForwarding()) {
            return getHandler().resolveEntity(publicId, systemId);
        }
        return null;
    }

    public void notationDecl(String name, String publicId, String systemId) throws SAXException {
        if (isForwarding()) {
            getHandler().notationDecl(name, publicId, systemId);
        }
    }

    public void unparsedEntityDecl(String name, String publicId,
                                   String systemId, String notationName) throws SAXException {
        if (isForwarding()) {
            getHandler().unparsedEntityDecl(name, publicId, systemId,
                                            notationName);
        }
    }

    public void setDocumentLocator(Locator locator) {
        if (isForwarding()) {
            getHandler().setDocumentLocator(locator);
        }
    }

    public void startDocument() throws SAXException {
        if (isForwarding()) {
            getHandler().startDocument();
        }
    }

    public void endDocument() throws SAXException {
        if (isForwarding()) {
            getHandler().endDocument();
        }
    }

    public void startElement(String uri, String local, String name, Attributes attributes)
                     throws SAXException {
        name = name.intern();
        if (name == ELEMENT_STYLE) {
            startStyle(attributes);
        }
        else if (name == ELEMENT_STATE) {
            startState(attributes);
        }
        else if (name == ELEMENT_FONT) {
            startFont(attributes);
        }
        else if (name == ELEMENT_COLOR) {
            startColor(attributes);
        }
        else if (name == ELEMENT_PAINTER) {
            startPainter(attributes, name);
        }
        else if (name == ELEMENT_IMAGE_PAINTER) {
            startPainter(attributes, name);
        }
        else if (name == ELEMENT_PROPERTY) {
            startProperty(attributes, ELEMENT_PROPERTY);
        }
        else if (name == ELEMENT_DEFAULTS_PROPERTY) {
            startProperty(attributes, ELEMENT_DEFAULTS_PROPERTY);
        }
        else if (name == ELEMENT_SYNTH_GRAPHICS) {
            startGraphics(attributes);
        }
        else if (name == ELEMENT_INSETS) {
            startInsets(attributes);
        }
        else if (name == ELEMENT_BIND) {
            startBind(attributes);
        }
        else if (name == ELEMENT_BIND_KEY) {
            startBindKey(attributes);
        }
        else if (name == ELEMENT_IMAGE_ICON) {
            startImageIcon(attributes);
        }
        else if (name == ELEMENT_OPAQUE) {
            startOpaque(attributes);
        }
        else if (name == ELEMENT_INPUT_MAP) {
            startInputMap(attributes);
        }
        else if (name != ELEMENT_SYNTH) {
            if (_depth++ == 0) {
                getHandler().startDocument();
            }
            getHandler().startElement(uri, local, name, attributes);
        }
    }

    public void endElement(String uri, String local, String name) throws SAXException {
        if (isForwarding()) {
            getHandler().endElement(uri, local, name);
            _depth--;
            if (!isForwarding()) {
                getHandler().startDocument();
            }
        }
        else {
            name = name.intern();
            if (name == ELEMENT_STYLE) {
                endStyle();
            }
            else if (name == ELEMENT_STATE) {
                endState();
            }
            else if (name == ELEMENT_INPUT_MAP) {
                endInputMap();
            }
        }
    }

    public void characters(char[] ch, int start, int length)
                           throws SAXException {
        if (isForwarding()) {
            getHandler().characters(ch, start, length);
        }
    }

    public void ignorableWhitespace (char[] ch, int start, int length)
        throws SAXException {
        if (isForwarding()) {
            getHandler().ignorableWhitespace(ch, start, length);
        }
    }

    public void processingInstruction(String target, String data)
                                     throws SAXException {
        if (isForwarding()) {
            getHandler().processingInstruction(target, data);
        }
    }

    public void warning(SAXParseException e) throws SAXException {
        if (isForwarding()) {
            getHandler().warning(e);
        }
    }

    public void error(SAXParseException e) throws SAXException {
        if (isForwarding()) {
            getHandler().error(e);
        }
    }


    public void fatalError(SAXParseException e) throws SAXException {
        if (isForwarding()) {
            getHandler().fatalError(e);
        }
        throw e;
    }


    /**
     * ImageIcon that lazily loads the image until needed.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private static class LazyImageIcon extends ImageIcon implements UIResource {
        private URL location;

        public LazyImageIcon(URL location) {
            super();
            this.location = location;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (getImage() != null) {
                super.paintIcon(c, g, x, y);
            }
        }

        public int getIconWidth() {
            if (getImage() != null) {
                return super.getIconWidth();
            }
            return 0;
        }

        public int getIconHeight() {
            if (getImage() != null) {
                return super.getIconHeight();
            }
            return 0;
        }

        public Image getImage() {
            if (location != null) {
                setImage(Toolkit.getDefaultToolkit().getImage(location));
                location = null;
            }
            return super.getImage();
        }
    }
}
