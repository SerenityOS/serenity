/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996 - 1997, All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998, All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by Taligent, Inc., a wholly-owned subsidiary
 * of IBM. These materials are provided under terms of a License
 * Agreement between Taligent and Sun. This technology is protected
 * by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.awt.font;

import java.io.InvalidObjectException;
import java.io.Serial;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashMap;
import java.util.Map;

import jdk.internal.access.SharedSecrets;

/**
 * The {@code TextAttribute} class defines attribute keys and
 * attribute values used for text rendering.
 * <p>
 * {@code TextAttribute} instances are used as attribute keys to
 * identify attributes in
 * {@link java.awt.Font Font},
 * {@link java.awt.font.TextLayout TextLayout},
 * {@link java.text.AttributedCharacterIterator AttributedCharacterIterator},
 * and other classes handling text attributes. Other constants defined
 * in this class can be used as attribute values.
 * <p>
 * For each text attribute, the documentation provides:
 * <UL>
 *   <LI>the type of its value,
 *   <LI>the relevant predefined constants, if any
 *   <LI>the default effect if the attribute is absent
 *   <LI>the valid values if there are limitations
 *   <LI>a description of the effect.
 * </UL>
 *
 * <H2>Values</H2>
 * <UL>
 *   <LI>The values of attributes must always be immutable.
 *   <LI>Where value limitations are given, any value outside of that
 *   set is reserved for future use; the value will be treated as
 *   the default.
 *   <LI>The value {@code null} is treated the same as the
 *   default value and results in the default behavior.
 *   <li>If the value is not of the proper type, the attribute
 *   will be ignored.
 *   <li>The identity of the value does not matter, only the actual
 *   value.  For example, {@code TextAttribute.WEIGHT_BOLD} and
 *   {@code Float.valueOf(2.0f)}
 *   indicate the same {@code WEIGHT}.
 *   <li>Attribute values of type {@code Number} (used for
 *   {@code WEIGHT}, {@code WIDTH}, {@code POSTURE},
 *   {@code SIZE}, {@code JUSTIFICATION}, and
 *   {@code TRACKING}) can vary along their natural range and are
 *   not restricted to the predefined constants.
 *   {@code Number.floatValue()} is used to get the actual value
 *   from the {@code Number}.
 *   <li>The values for {@code WEIGHT}, {@code WIDTH}, and
 *   {@code POSTURE} are interpolated by the system, which
 *   can select the 'nearest available' font or use other techniques to
 *   approximate the user's request.
 *
 * </UL>
 *
 * <h3>Summary of attributes</h3>
 *
 * <table style="width:95%;margin: 0px auto" class="striped">
 * <caption>Key, value type, principal constants, and default value behavior of
 * all TextAttributes</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Key
 *     <th scope="col">Value Type
 *     <th scope="col">Principal Constants
 *     <th scope="col">Default Value
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">{@link #FAMILY}
 *     <td>String
 *     <td>See Font {@link java.awt.Font#DIALOG DIALOG},
 *     {@link java.awt.Font#DIALOG_INPUT DIALOG_INPUT},
 *     <br>
 *     {@link java.awt.Font#SERIF SERIF},
 *     {@link java.awt.Font#SANS_SERIF SANS_SERIF}, and
 *     {@link java.awt.Font#MONOSPACED MONOSPACED}.
 *     <td>"Default" (use platform default)
 *   <tr>
 *     <th scope="row">{@link #WEIGHT}
 *     <td>Number
 *     <td>WEIGHT_REGULAR, WEIGHT_BOLD
 *     <td>WEIGHT_REGULAR
 *   <tr>
 *     <th scope="row">{@link #WIDTH}
 *     <td>Number
 *     <td>WIDTH_CONDENSED, WIDTH_REGULAR,<br>WIDTH_EXTENDED
 *     <td>WIDTH_REGULAR
 *   <tr>
 *     <th scope="row">{@link #POSTURE}
 *     <td>Number
 *     <td>POSTURE_REGULAR, POSTURE_OBLIQUE
 *     <td>POSTURE_REGULAR
 *   <tr>
 *     <th scope="row">{@link #SIZE}
 *     <td>Number
 *     <td>none
 *     <td>12.0
 *   <tr>
 *     <th scope="row">{@link #TRANSFORM}
 *     <td>{@link TransformAttribute}
 *     <td>See TransformAttribute {@link TransformAttribute#IDENTITY IDENTITY}
 *     <td>TransformAttribute.IDENTITY
 *   <tr>
 *     <th scope="row">{@link #SUPERSCRIPT}
 *     <td>Integer
 *     <td>SUPERSCRIPT_SUPER, SUPERSCRIPT_SUB
 *     <td>0 (use the standard glyphs and metrics)
 *   <tr>
 *     <th scope="row">{@link #FONT}
 *     <td>{@link java.awt.Font}
 *     <td>none
 *     <td>null (do not override font resolution)
 *   <tr>
 *     <th scope="row">{@link #CHAR_REPLACEMENT}
 *     <td>{@link GraphicAttribute}
 *     <td>none
 *     <td>null (draw text using font glyphs)
 *   <tr>
 *     <th scope="row">{@link #FOREGROUND}
 *     <td>{@link java.awt.Paint}
 *     <td>none
 *     <td>null (use current graphics paint)
 *   <tr>
 *     <th scope="row">{@link #BACKGROUND}
 *     <td>{@link java.awt.Paint}
 *     <td>none
 *     <td>null (do not render background)
 *   <tr>
 *     <th scope="row">{@link #UNDERLINE}
 *     <td>Integer
 *     <td>UNDERLINE_ON
 *     <td>-1 (do not render underline)
 *   <tr>
 *     <th scope="row">{@link #STRIKETHROUGH}
 *     <td>Boolean
 *     <td>STRIKETHROUGH_ON
 *     <td>false (do not render strikethrough)
 *   <tr>
 *     <th scope="row">{@link #RUN_DIRECTION}
 *     <td>Boolean
 *     <td>RUN_DIRECTION_LTR<br>RUN_DIRECTION_RTL
 *     <td>null (use {@link java.text.Bidi} standard default)
 *   <tr>
 *     <th scope="row">{@link #BIDI_EMBEDDING}
 *     <td>Integer
 *     <td>none
 *     <td>0 (use base line direction)
 *   <tr>
 *     <th scope="row">{@link #JUSTIFICATION}
 *     <td>Number
 *     <td>JUSTIFICATION_FULL
 *     <td>JUSTIFICATION_FULL
 *   <tr>
 *     <th scope="row">{@link #INPUT_METHOD_HIGHLIGHT}
 *     <td>{@link java.awt.im.InputMethodHighlight},
 *     <br>
 *     {@link java.text.Annotation}
 *     <td>(see class)
 *     <td>null (do not apply input highlighting)
 *   <tr>
 *     <th scope="row">{@link #INPUT_METHOD_UNDERLINE}
 *     <td>Integer
 *     <td>UNDERLINE_LOW_ONE_PIXEL,<br>UNDERLINE_LOW_TWO_PIXEL
 *     <td>-1 (do not render underline)
 *   <tr>
 *     <th scope="row">{@link #SWAP_COLORS}
 *     <td>Boolean
 *     <td>SWAP_COLORS_ON
 *     <td>false (do not swap colors)
 *   <tr>
 *     <th scope="row">{@link #NUMERIC_SHAPING}
 *     <td>{@link java.awt.font.NumericShaper}
 *     <td>none
 *     <td>null (do not shape digits)
 *   <tr>
 *     <th scope="row">{@link #KERNING}
 *     <td>Integer
 *     <td>KERNING_ON
 *     <td>0 (do not request kerning)
 *   <tr>
 *     <th scope="row">{@link #LIGATURES}
 *     <td>Integer
 *     <td>LIGATURES_ON
 *     <td>0 (do not form optional ligatures)
 *   <tr>
 *     <th scope="row">{@link #TRACKING}
 *     <td>Number
 *     <td>TRACKING_LOOSE, TRACKING_TIGHT
 *     <td>0 (do not add tracking)
 *   </tr>
 * </tbody>
 * </table>
 *
 * @see java.awt.Font
 * @see java.awt.font.TextLayout
 * @see java.text.AttributedCharacterIterator
 */
public final class TextAttribute extends Attribute {

    // table of all instances in this class, used by readResolve
    private static final Map<String, TextAttribute>
            instanceMap = new HashMap<String, TextAttribute>(29);

    // For access from java.text.Bidi
    static {
        if (SharedSecrets.getJavaAWTFontAccess() == null) {
            SharedSecrets.setJavaAWTFontAccess(new JavaAWTFontAccessImpl());
        }
    }

    /**
     * Constructs a {@code TextAttribute} with the specified name.
     * @param name the attribute name to assign to this
     * {@code TextAttribute}
     */
    protected TextAttribute(String name) {
        super(name);
        if (this.getClass() == TextAttribute.class) {
            instanceMap.put(name, this);
        }
    }

    /**
     * Resolves instances being deserialized to the predefined constants.
     */
    @Serial
    protected Object readResolve() throws InvalidObjectException {
        if (this.getClass() != TextAttribute.class) {
            throw new InvalidObjectException(
                "subclass didn't correctly implement readResolve");
        }

        TextAttribute instance = instanceMap.get(getName());
        if (instance != null) {
            return instance;
        } else {
            throw new InvalidObjectException("unknown attribute name");
        }
    }

    /**
     * Use serialVersionUID from JDK 1.2 for interoperability.
     * 1.2 will throw an InvalidObjectException if ever asked to deserialize
     * INPUT_METHOD_UNDERLINE. This shouldn't happen in real life.
     */
    @Serial
    private static final long serialVersionUID = 7744112784117861702L;

    //
    // For use with Font.
    //

    /**
     * Attribute key for the font name.  Values are instances of
     * <b>{@code String}</b>.  The default value is
     * {@code "Default"}, which causes the platform default font
     * family to be used.
     *
     * <p> The {@code Font} class defines constants for the logical
     * font names
     * {@link java.awt.Font#DIALOG DIALOG},
     * {@link java.awt.Font#DIALOG_INPUT DIALOG_INPUT},
     * {@link java.awt.Font#SANS_SERIF SANS_SERIF},
     * {@link java.awt.Font#SERIF SERIF}, and
     * {@link java.awt.Font#MONOSPACED MONOSPACED}.
     *
     * <p>This defines the value passed as {@code name} to the
     * {@code Font} constructor.  Both logical and physical
     * font names are allowed. If a font with the requested name
     * is not found, the default font is used.
     *
     * <p><em>Note:</em> This attribute is unfortunately misnamed, as
     * it specifies the face name and not just the family.  Thus
     * values such as "Lucida Sans Bold" will select that face if it
     * exists.  Note, though, that if the requested face does not
     * exist, the default will be used with <em>regular</em> weight.
     * The "Bold" in the name is part of the face name, not a separate
     * request that the font's weight be bold.</p>
     */
    public static final TextAttribute FAMILY =
        new TextAttribute("family");

    /**
     * Attribute key for the weight of a font.  Values are instances
     * of <b>{@code Number}</b>.  The default value is
     * {@code WEIGHT_REGULAR}.
     *
     * <p>Several constant values are provided, see {@link
     * #WEIGHT_EXTRA_LIGHT}, {@link #WEIGHT_LIGHT}, {@link
     * #WEIGHT_DEMILIGHT}, {@link #WEIGHT_REGULAR}, {@link
     * #WEIGHT_SEMIBOLD}, {@link #WEIGHT_MEDIUM}, {@link
     * #WEIGHT_DEMIBOLD}, {@link #WEIGHT_BOLD}, {@link #WEIGHT_HEAVY},
     * {@link #WEIGHT_EXTRABOLD}, and {@link #WEIGHT_ULTRABOLD}.  The
     * value {@code WEIGHT_BOLD} corresponds to the
     * style value {@code Font.BOLD} as passed to the
     * {@code Font} constructor.
     *
     * <p>The value is roughly the ratio of the stem width to that of
     * the regular weight.
     *
     * <p>The system can interpolate the provided value.
     */
    public static final TextAttribute WEIGHT =
        new TextAttribute("weight");

    /**
     * The lightest predefined weight.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_EXTRA_LIGHT =
        Float.valueOf(0.5f);

    /**
     * The standard light weight.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_LIGHT =
        Float.valueOf(0.75f);

    /**
     * An intermediate weight between {@code WEIGHT_LIGHT} and
     * {@code WEIGHT_STANDARD}.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_DEMILIGHT =
        Float.valueOf(0.875f);

    /**
     * The standard weight. This is the default value for {@code WEIGHT}.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_REGULAR =
        Float.valueOf(1.0f);

    /**
     * A moderately heavier weight than {@code WEIGHT_REGULAR}.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_SEMIBOLD =
        Float.valueOf(1.25f);

    /**
     * An intermediate weight between {@code WEIGHT_REGULAR} and
     * {@code WEIGHT_BOLD}.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_MEDIUM =
        Float.valueOf(1.5f);

    /**
     * A moderately lighter weight than {@code WEIGHT_BOLD}.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_DEMIBOLD =
        Float.valueOf(1.75f);

    /**
     * The standard bold weight.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_BOLD =
        Float.valueOf(2.0f);

    /**
     * A moderately heavier weight than {@code WEIGHT_BOLD}.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_HEAVY =
        Float.valueOf(2.25f);

    /**
     * An extra heavy weight.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_EXTRABOLD =
        Float.valueOf(2.5f);

    /**
     * The heaviest predefined weight.
     * @see #WEIGHT
     */
    public static final Float WEIGHT_ULTRABOLD =
        Float.valueOf(2.75f);

    /**
     * Attribute key for the width of a font.  Values are instances of
     * <b>{@code Number}</b>.  The default value is
     * {@code WIDTH_REGULAR}.
     *
     * <p>Several constant values are provided, see {@link
     * #WIDTH_CONDENSED}, {@link #WIDTH_SEMI_CONDENSED}, {@link
     * #WIDTH_REGULAR}, {@link #WIDTH_SEMI_EXTENDED}, {@link
     * #WIDTH_EXTENDED}.
     *
     * <p>The value is roughly the ratio of the advance width to that
     * of the regular width.
     *
     * <p>The system can interpolate the provided value.
     */
    public static final TextAttribute WIDTH =
        new TextAttribute("width");

    /**
     * The most condensed predefined width.
     * @see #WIDTH
     */
    public static final Float WIDTH_CONDENSED =
        Float.valueOf(0.75f);

    /**
     * A moderately condensed width.
     * @see #WIDTH
     */
    public static final Float WIDTH_SEMI_CONDENSED =
        Float.valueOf(0.875f);

    /**
     * The standard width. This is the default value for
     * {@code WIDTH}.
     * @see #WIDTH
     */
    public static final Float WIDTH_REGULAR =
        Float.valueOf(1.0f);

    /**
     * A moderately extended width.
     * @see #WIDTH
     */
    public static final Float WIDTH_SEMI_EXTENDED =
        Float.valueOf(1.25f);

    /**
     * The most extended predefined width.
     * @see #WIDTH
     */
    public static final Float WIDTH_EXTENDED =
        Float.valueOf(1.5f);

    /**
     * Attribute key for the posture of a font.  Values are instances
     * of <b>{@code Number}</b>. The default value is
     * {@code POSTURE_REGULAR}.
     *
     * <p>Two constant values are provided, {@link #POSTURE_REGULAR}
     * and {@link #POSTURE_OBLIQUE}. The value
     * {@code POSTURE_OBLIQUE} corresponds to the style value
     * {@code Font.ITALIC} as passed to the {@code Font}
     * constructor.
     *
     * <p>The value is roughly the slope of the stems of the font,
     * expressed as the run over the rise.  Positive values lean right.
     *
     * <p>The system can interpolate the provided value.
     *
     * <p>This will affect the font's italic angle as returned by
     * {@code Font.getItalicAngle}.
     *
     * @see java.awt.Font#getItalicAngle()
     */
    public static final TextAttribute POSTURE =
        new TextAttribute("posture");

    /**
     * The standard posture, upright.  This is the default value for
     * {@code POSTURE}.
     * @see #POSTURE
     */
    public static final Float POSTURE_REGULAR =
        Float.valueOf(0.0f);

    /**
     * The standard italic posture.
     * @see #POSTURE
     */
    public static final Float POSTURE_OBLIQUE =
        Float.valueOf(0.20f);

    /**
     * Attribute key for the font size.  Values are instances of
     * <b>{@code Number}</b>.  The default value is 12pt.
     *
     * <p>This corresponds to the {@code size} parameter to the
     * {@code Font} constructor.
     *
     * <p>Very large or small sizes will impact rendering performance,
     * and the rendering system might not render text at these sizes.
     * Negative sizes are illegal and result in the default size.
     *
     * <p>Note that the appearance and metrics of a 12pt font with a
     * 2x transform might be different than that of a 24 point font
     * with no transform.
     */
    public static final TextAttribute SIZE =
        new TextAttribute("size");

    /**
     * Attribute key for the transform of a font.  Values are
     * instances of <b>{@code TransformAttribute}</b>.  The
     * default value is {@code TransformAttribute.IDENTITY}.
     *
     * <p>The {@code TransformAttribute} class defines the
     * constant {@link TransformAttribute#IDENTITY IDENTITY}.
     *
     * <p>This corresponds to the transform passed to
     * {@code Font.deriveFont(AffineTransform)}.  Since that
     * transform is mutable and {@code TextAttribute} values must
     * not be, the {@code TransformAttribute} wrapper class is
     * used.
     *
     * <p>The primary intent is to support scaling and skewing, though
     * other effects are possible.</p>
     *
     * <p>Some transforms will cause the baseline to be rotated and/or
     * shifted.  The text and the baseline are transformed together so
     * that the text follows the new baseline.  For example, with text
     * on a horizontal baseline, the new baseline follows the
     * direction of the unit x vector passed through the
     * transform. Text metrics are measured against this new baseline.
     * So, for example, with other things being equal, text rendered
     * with a rotated TRANSFORM and an unrotated TRANSFORM will measure as
     * having the same ascent, descent, and advance.</p>
     *
     * <p>In styled text, the baselines for each such run are aligned
     * one after the other to potentially create a non-linear baseline
     * for the entire run of text. For more information, see {@link
     * TextLayout#getLayoutPath}.</p>
     *
     * @see TransformAttribute
     * @see java.awt.geom.AffineTransform
     */
     public static final TextAttribute TRANSFORM =
        new TextAttribute("transform");

    /**
     * Attribute key for superscripting and subscripting.  Values are
     * instances of <b>{@code Integer}</b>.  The default value is
     * 0, which means that no superscript or subscript is used.
     *
     * <p>Two constant values are provided, see {@link
     * #SUPERSCRIPT_SUPER} and {@link #SUPERSCRIPT_SUB}.  These have
     * the values 1 and -1 respectively.  Values of
     * greater magnitude define greater levels of superscript or
     * subscripting, for example, 2 corresponds to super-superscript,
     * 3 to super-super-superscript, and similarly for negative values
     * and subscript, up to a level of 7 (or -7).  Values beyond this
     * range are reserved; behavior is platform-dependent.
     *
     * <p>{@code SUPERSCRIPT} can
     * impact the ascent and descent of a font.  The ascent
     * and descent can never become negative, however.
     */
    public static final TextAttribute SUPERSCRIPT =
        new TextAttribute("superscript");

    /**
     * Standard superscript.
     * @see #SUPERSCRIPT
     */
    public static final Integer SUPERSCRIPT_SUPER =
        Integer.valueOf(1);

    /**
     * Standard subscript.
     * @see #SUPERSCRIPT
     */
    public static final Integer SUPERSCRIPT_SUB =
        Integer.valueOf(-1);

    /**
     * Attribute key used to provide the font to use to render text.
     * Values are instances of {@link java.awt.Font}.  The default
     * value is null, indicating that normal resolution of a
     * {@code Font} from attributes should be performed.
     *
     * <p>{@code TextLayout} and
     * {@code AttributedCharacterIterator} work in terms of
     * {@code Maps} of {@code TextAttributes}.  Normally,
     * all the attributes are examined and used to select and
     * configure a {@code Font} instance.  If a {@code FONT}
     * attribute is present, though, its associated {@code Font}
     * will be used.  This provides a way for users to override the
     * resolution of font attributes into a {@code Font}, or
     * force use of a particular {@code Font} instance.  This
     * also allows users to specify subclasses of {@code Font} in
     * cases where a {@code Font} can be subclassed.
     *
     * <p>{@code FONT} is used for special situations where
     * clients already have a {@code Font} instance but still
     * need to use {@code Map}-based APIs.  Typically, there will
     * be no other attributes in the {@code Map} except the
     * {@code FONT} attribute.  With {@code Map}-based APIs
     * the common case is to specify all attributes individually, so
     * {@code FONT} is not needed or desirable.
     *
     * <p>However, if both {@code FONT} and other attributes are
     * present in the {@code Map}, the rendering system will
     * merge the attributes defined in the {@code Font} with the
     * additional attributes.  This merging process classifies
     * {@code TextAttributes} into two groups.  One group, the
     * 'primary' group, is considered fundamental to the selection and
     * metric behavior of a font.  These attributes are
     * {@code FAMILY}, {@code WEIGHT}, {@code WIDTH},
     * {@code POSTURE}, {@code SIZE},
     * {@code TRANSFORM}, {@code SUPERSCRIPT}, and
     * {@code TRACKING}. The other group, the 'secondary' group,
     * consists of all other defined attributes, with the exception of
     * {@code FONT} itself.
     *
     * <p>To generate the new {@code Map}, first the
     * {@code Font} is obtained from the {@code FONT}
     * attribute, and <em>all</em> of its attributes extracted into a
     * new {@code Map}.  Then only the <em>secondary</em>
     * attributes from the original {@code Map} are added to
     * those in the new {@code Map}.  Thus the values of primary
     * attributes come solely from the {@code Font}, and the
     * values of secondary attributes originate with the
     * {@code Font} but can be overridden by other values in the
     * {@code Map}.
     *
     * <p><em>Note:</em>{@code Font's Map}-based
     * constructor and {@code deriveFont} methods do not process
     * the {@code FONT} attribute, as these are used to create
     * new {@code Font} objects.  Instead, {@link
     * java.awt.Font#getFont(Map) Font.getFont(Map)} should be used to
     * handle the {@code FONT} attribute.
     *
     * @see java.awt.Font
     */
    public static final TextAttribute FONT =
        new TextAttribute("font");

    /**
     * Attribute key for a user-defined glyph to display in lieu
     * of the font's standard glyph for a character.  Values are
     * instances of GraphicAttribute.  The default value is null,
     * indicating that the standard glyphs provided by the font
     * should be used.
     *
     * <p>This attribute is used to reserve space for a graphic or
     * other component embedded in a line of text.  It is required for
     * correct positioning of 'inline' components within a line when
     * bidirectional reordering (see {@link java.text.Bidi}) is
     * performed.  Each character (Unicode code point) will be
     * rendered using the provided GraphicAttribute. Typically, the
     * characters to which this attribute is applied should be
     * <code>&#92;uFFFC</code>.
     *
     * <p>The GraphicAttribute determines the logical and visual
     * bounds of the text; the actual Font values are ignored.
     *
     * @see GraphicAttribute
     */
    public static final TextAttribute CHAR_REPLACEMENT =
        new TextAttribute("char_replacement");

    //
    // Adornments added to text.
    //

    /**
     * Attribute key for the paint used to render the text.  Values are
     * instances of <b>{@code Paint}</b>.  The default value is
     * null, indicating that the {@code Paint} set on the
     * {@code Graphics2D} at the time of rendering is used.
     *
     * <p>Glyphs will be rendered using this
     * {@code Paint} regardless of the {@code Paint} value
     * set on the {@code Graphics} (but see {@link #SWAP_COLORS}).
     *
     * @see java.awt.Paint
     * @see #SWAP_COLORS
     */
    public static final TextAttribute FOREGROUND =
        new TextAttribute("foreground");

    /**
     * Attribute key for the paint used to render the background of
     * the text.  Values are instances of <b>{@code Paint}</b>.
     * The default value is null, indicating that the background
     * should not be rendered.
     *
     * <p>The logical bounds of the text will be filled using this
     * {@code Paint}, and then the text will be rendered on top
     * of it (but see {@link #SWAP_COLORS}).
     *
     * <p>The visual bounds of the text is extended to include the
     * logical bounds, if necessary.  The outline is not affected.
     *
     * @see java.awt.Paint
     * @see #SWAP_COLORS
     */
    public static final TextAttribute BACKGROUND =
        new TextAttribute("background");

    /**
     * Attribute key for underline.  Values are instances of
     * <b>{@code Integer}</b>.  The default value is -1, which
     * means no underline.
     *
     * <p>The constant value {@link #UNDERLINE_ON} is provided.
     *
     * <p>The underline affects both the visual bounds and the outline
     * of the text.
     */
    public static final TextAttribute UNDERLINE =
        new TextAttribute("underline");

    /**
     * Standard underline.
     *
     * @see #UNDERLINE
     */
    public static final Integer UNDERLINE_ON =
        Integer.valueOf(0);

    /**
     * Attribute key for strikethrough.  Values are instances of
     * <b>{@code Boolean}</b>.  The default value is
     * {@code false}, which means no strikethrough.
     *
     * <p>The constant value {@link #STRIKETHROUGH_ON} is provided.
     *
     * <p>The strikethrough affects both the visual bounds and the
     * outline of the text.
     */
    public static final TextAttribute STRIKETHROUGH =
        new TextAttribute("strikethrough");

    /**
     * A single strikethrough.
     *
     * @see #STRIKETHROUGH
     */
    public static final Boolean STRIKETHROUGH_ON =
        Boolean.TRUE;

    //
    // Attributes use to control layout of text on a line.
    //

    /**
     * Attribute key for the run direction of the line.  Values are
     * instances of <b>{@code Boolean}</b>.  The default value is
     * null, which indicates that the standard Bidi algorithm for
     * determining run direction should be used with the value {@link
     * java.text.Bidi#DIRECTION_DEFAULT_LEFT_TO_RIGHT}.
     *
     * <p>The constants {@link #RUN_DIRECTION_RTL} and {@link
     * #RUN_DIRECTION_LTR} are provided.
     *
     * <p>This determines the value passed to the {@link
     * java.text.Bidi} constructor to select the primary direction of
     * the text in the paragraph.
     *
     * <p><em>Note:</em> This attribute should have the same value for
     * all the text in a paragraph, otherwise the behavior is
     * undetermined.
     *
     * @see java.text.Bidi
     */
    public static final TextAttribute RUN_DIRECTION =
        new TextAttribute("run_direction");

    /**
     * Left-to-right run direction.
     * @see #RUN_DIRECTION
     */
    public static final Boolean RUN_DIRECTION_LTR =
        Boolean.FALSE;

    /**
     * Right-to-left run direction.
     * @see #RUN_DIRECTION
     */
    public static final Boolean RUN_DIRECTION_RTL =
        Boolean.TRUE;

    /**
     * Attribute key for the embedding level of the text.  Values are
     * instances of <b>{@code Integer}</b>.  The default value is
     * {@code null}, indicating that the Bidirectional
     * algorithm should run without explicit embeddings.
     *
     * <p>Positive values 1 through 61 are <em>embedding</em> levels,
     * negative values -1 through -61 are <em>override</em> levels.
     * The value 0 means that the base line direction is used.  These
     * levels are passed in the embedding levels array to the {@link
     * java.text.Bidi} constructor.
     *
     * <p><em>Note:</em> When this attribute is present anywhere in
     * a paragraph, then any Unicode bidi control characters (RLO,
     * LRO, RLE, LRE, and PDF) in the paragraph are
     * disregarded, and runs of text where this attribute is not
     * present are treated as though it were present and had the value
     * 0.
     *
     * @see java.text.Bidi
     */
    public static final TextAttribute BIDI_EMBEDDING =
        new TextAttribute("bidi_embedding");

    /**
     * Attribute key for the justification of a paragraph.  Values are
     * instances of <b>{@code Number}</b>.  The default value is
     * 1, indicating that justification should use the full width
     * provided.  Values are pinned to the range [0..1].
     *
     * <p>The constants {@link #JUSTIFICATION_FULL} and {@link
     * #JUSTIFICATION_NONE} are provided.
     *
     * <p>Specifies the fraction of the extra space to use when
     * justification is requested on a {@code TextLayout}. For
     * example, if the line is 50 points wide and it is requested to
     * justify to 70 points, a value of 0.75 will pad to use
     * three-quarters of the remaining space, or 15 points, so that
     * the resulting line will be 65 points in length.
     *
     * <p><em>Note:</em> This should have the same value for all the
     * text in a paragraph, otherwise the behavior is undetermined.
     *
     * @see TextLayout#getJustifiedLayout
     */
    public static final TextAttribute JUSTIFICATION =
        new TextAttribute("justification");

    /**
     * Justify the line to the full requested width.  This is the
     * default value for {@code JUSTIFICATION}.
     * @see #JUSTIFICATION
     */
    public static final Float JUSTIFICATION_FULL =
        Float.valueOf(1.0f);

    /**
     * Do not allow the line to be justified.
     * @see #JUSTIFICATION
     */
    public static final Float JUSTIFICATION_NONE =
        Float.valueOf(0.0f);

    //
    // For use by input method.
    //

    /**
     * Attribute key for input method highlight styles.
     *
     * <p>Values are instances of {@link
     * java.awt.im.InputMethodHighlight} or {@link
     * java.text.Annotation}.  The default value is {@code null},
     * which means that input method styles should not be applied
     * before rendering.
     *
     * <p>If adjacent runs of text with the same
     * {@code InputMethodHighlight} need to be rendered
     * separately, the {@code InputMethodHighlights} should be
     * wrapped in {@code Annotation} instances.
     *
     * <p>Input method highlights are used while text is being
     * composed by an input method. Text editing components should
     * retain them even if they generally only deal with unstyled
     * text, and make them available to the drawing routines.
     *
     * @see java.awt.Font
     * @see java.awt.im.InputMethodHighlight
     * @see java.text.Annotation
     */
    public static final TextAttribute INPUT_METHOD_HIGHLIGHT =
        new TextAttribute("input method highlight");

    /**
     * Attribute key for input method underlines.  Values
     * are instances of <b>{@code Integer}</b>.  The default
     * value is {@code -1}, which means no underline.
     *
     * <p>Several constant values are provided, see {@link
     * #UNDERLINE_LOW_ONE_PIXEL}, {@link #UNDERLINE_LOW_TWO_PIXEL},
     * {@link #UNDERLINE_LOW_DOTTED}, {@link #UNDERLINE_LOW_GRAY}, and
     * {@link #UNDERLINE_LOW_DASHED}.
     *
     * <p>This may be used in conjunction with {@link #UNDERLINE} if
     * desired.  The primary purpose is for use by input methods.
     * Other use of these underlines for simple ornamentation might
     * confuse users.
     *
     * <p>The input method underline affects both the visual bounds and
     * the outline of the text.
     *
     * @since 1.3
     */
    public static final TextAttribute INPUT_METHOD_UNDERLINE =
        new TextAttribute("input method underline");

    /**
     * Single pixel solid low underline.
     * @see #INPUT_METHOD_UNDERLINE
     * @since 1.3
     */
    public static final Integer UNDERLINE_LOW_ONE_PIXEL =
        Integer.valueOf(1);

    /**
     * Double pixel solid low underline.
     * @see #INPUT_METHOD_UNDERLINE
     * @since 1.3
     */
    public static final Integer UNDERLINE_LOW_TWO_PIXEL =
        Integer.valueOf(2);

    /**
     * Single pixel dotted low underline.
     * @see #INPUT_METHOD_UNDERLINE
     * @since 1.3
     */
    public static final Integer UNDERLINE_LOW_DOTTED =
        Integer.valueOf(3);

    /**
     * Double pixel gray low underline.
     * @see #INPUT_METHOD_UNDERLINE
     * @since 1.3
     */
    public static final Integer UNDERLINE_LOW_GRAY =
        Integer.valueOf(4);

    /**
     * Single pixel dashed low underline.
     * @see #INPUT_METHOD_UNDERLINE
     * @since 1.3
     */
    public static final Integer UNDERLINE_LOW_DASHED =
        Integer.valueOf(5);

    /**
     * Attribute key for swapping foreground and background
     * {@code Paints}.  Values are instances of
     * <b>{@code Boolean}</b>.  The default value is
     * {@code false}, which means do not swap colors.
     *
     * <p>The constant value {@link #SWAP_COLORS_ON} is defined.
     *
     * <p>If the {@link #FOREGROUND} attribute is set, its
     * {@code Paint} will be used as the background, otherwise
     * the {@code Paint} currently on the {@code Graphics}
     * will be used.  If the {@link #BACKGROUND} attribute is set, its
     * {@code Paint} will be used as the foreground, otherwise
     * the system will find a contrasting color to the
     * (resolved) background so that the text will be visible.
     *
     * @see #FOREGROUND
     * @see #BACKGROUND
     */
    public static final TextAttribute SWAP_COLORS =
        new TextAttribute("swap_colors");

    /**
     * Swap foreground and background.
     * @see #SWAP_COLORS
     * @since 1.3
     */
    public static final Boolean SWAP_COLORS_ON =
        Boolean.TRUE;

    /**
     * Attribute key for converting ASCII decimal digits to other
     * decimal ranges.  Values are instances of {@link NumericShaper}.
     * The default is {@code null}, which means do not perform
     * numeric shaping.
     *
     * <p>When a numeric shaper is defined, the text is first
     * processed by the shaper before any other analysis of the text
     * is performed.
     *
     * <p><em>Note:</em> This should have the same value for all the
     * text in the paragraph, otherwise the behavior is undetermined.
     *
     * @see NumericShaper
     * @since 1.4
     */
    public static final TextAttribute NUMERIC_SHAPING =
        new TextAttribute("numeric_shaping");

    /**
     * Attribute key to request kerning. Values are instances of
     * <b>{@code Integer}</b>.  The default value is
     * {@code 0}, which does not request kerning.
     *
     * <p>The constant value {@link #KERNING_ON} is provided.
     *
     * <p>The default advances of single characters are not
     * appropriate for some character sequences, for example "To" or
     * "AWAY".  Without kerning the adjacent characters appear to be
     * separated by too much space.  Kerning causes selected sequences
     * of characters to be spaced differently for a more pleasing
     * visual appearance.
     *
     * @since 1.6
     */
    public static final TextAttribute KERNING =
        new TextAttribute("kerning");

    /**
     * Request standard kerning.
     * @see #KERNING
     * @since 1.6
     */
    public static final Integer KERNING_ON =
        Integer.valueOf(1);


    /**
     * Attribute key for enabling optional ligatures. Values are
     * instances of <b>{@code Integer}</b>.  The default value is
     * {@code 0}, which means do not use optional ligatures.
     *
     * <p>The constant value {@link #LIGATURES_ON} is defined.
     *
     * <p>Ligatures required by the writing system are always enabled.
     *
     * @since 1.6
     */
    public static final TextAttribute LIGATURES =
        new TextAttribute("ligatures");

    /**
     * Request standard optional ligatures.
     * @see #LIGATURES
     * @since 1.6
     */
    public static final Integer LIGATURES_ON =
        Integer.valueOf(1);

    /**
     * Attribute key to control tracking.  Values are instances of
     * <b>{@code Number}</b>.  The default value is
     * {@code 0}, which means no additional tracking.
     *
     * <p>The constant values {@link #TRACKING_TIGHT} and {@link
     * #TRACKING_LOOSE} are provided.
     *
     * <p>The tracking value is multiplied by the font point size and
     * passed through the font transform to determine an additional
     * amount to add to the advance of each glyph cluster.  Positive
     * tracking values will inhibit formation of optional ligatures.
     * Tracking values are typically between {@code -0.1} and
     * {@code 0.3}; values outside this range are generally not
     * desirable.
     *
     * @since 1.6
     */
    public static final TextAttribute TRACKING =
        new TextAttribute("tracking");

    /**
     * Perform tight tracking.
     * @see #TRACKING
     * @since 1.6
     */
    public static final Float TRACKING_TIGHT =
        Float.valueOf(-.04f);

    /**
     * Perform loose tracking.
     * @see #TRACKING
     * @since 1.6
     */
    public static final Float TRACKING_LOOSE =
        Float.valueOf(.04f);
}
