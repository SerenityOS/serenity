/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ExtraMathConstants.h>
#include <AK/OwnPtr.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/CanvasRenderingContext2D.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/ImageData.h>
#include <LibWeb/HTML/TextMetrics.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::HTML {

CanvasRenderingContext2D::CanvasRenderingContext2D(HTMLCanvasElement& element)
    : m_element(element)
{
}

CanvasRenderingContext2D::~CanvasRenderingContext2D()
{
}

void CanvasRenderingContext2D::set_fill_style(String style)
{
    m_drawing_state.fill_style = Gfx::Color::from_string(style).value_or(Color::Black);
}

String CanvasRenderingContext2D::fill_style() const
{
    return m_drawing_state.fill_style.to_string();
}

void CanvasRenderingContext2D::fill_rect(float x, float y, float width, float height)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto rect = m_drawing_state.transform.map(Gfx::FloatRect(x, y, width, height));
    painter->fill_rect(enclosing_int_rect(rect), m_drawing_state.fill_style);
    did_draw(rect);
}

void CanvasRenderingContext2D::clear_rect(float x, float y, float width, float height)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto rect = m_drawing_state.transform.map(Gfx::FloatRect(x, y, width, height));
    painter->clear_rect(enclosing_int_rect(rect), Color());
    did_draw(rect);
}

void CanvasRenderingContext2D::set_stroke_style(String style)
{
    m_drawing_state.stroke_style = Gfx::Color::from_string(style).value_or(Color::Black);
}

String CanvasRenderingContext2D::stroke_style() const
{
    return m_drawing_state.stroke_style.to_string();
}

void CanvasRenderingContext2D::stroke_rect(float x, float y, float width, float height)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto rect = m_drawing_state.transform.map(Gfx::FloatRect(x, y, width, height));

    auto top_left = m_drawing_state.transform.map(Gfx::FloatPoint(x, y)).to_type<int>();
    auto top_right = m_drawing_state.transform.map(Gfx::FloatPoint(x + width - 1, y)).to_type<int>();
    auto bottom_left = m_drawing_state.transform.map(Gfx::FloatPoint(x, y + height - 1)).to_type<int>();
    auto bottom_right = m_drawing_state.transform.map(Gfx::FloatPoint(x + width - 1, y + height - 1)).to_type<int>();

    painter->draw_line(top_left, top_right, m_drawing_state.stroke_style, m_drawing_state.line_width);
    painter->draw_line(top_right, bottom_right, m_drawing_state.stroke_style, m_drawing_state.line_width);
    painter->draw_line(bottom_right, bottom_left, m_drawing_state.stroke_style, m_drawing_state.line_width);
    painter->draw_line(bottom_left, top_left, m_drawing_state.stroke_style, m_drawing_state.line_width);

    did_draw(rect);
}

void CanvasRenderingContext2D::draw_image(const HTMLImageElement& image_element, float x, float y)
{
    if (!image_element.bitmap())
        return;

    auto painter = this->painter();
    if (!painter)
        return;

    auto src_rect = image_element.bitmap()->rect();
    Gfx::FloatRect dst_rect = { x, y, (float)image_element.bitmap()->width(), (float)image_element.bitmap()->height() };
    auto rect = m_drawing_state.transform.map(dst_rect);

    painter->draw_scaled_bitmap(rounded_int_rect(rect), *image_element.bitmap(), src_rect, 1.0f, Gfx::Painter::ScalingMode::BilinearBlend);
}

void CanvasRenderingContext2D::scale(float sx, float sy)
{
    dbgln("CanvasRenderingContext2D::scale({}, {})", sx, sy);
    m_drawing_state.transform.scale(sx, sy);
}

void CanvasRenderingContext2D::translate(float tx, float ty)
{
    dbgln("CanvasRenderingContext2D::translate({}, {})", tx, ty);
    m_drawing_state.transform.translate(tx, ty);
}

void CanvasRenderingContext2D::rotate(float radians)
{
    dbgln("CanvasRenderingContext2D::rotate({})", radians);
    m_drawing_state.transform.rotate_radians(radians);
}

void CanvasRenderingContext2D::did_draw(const Gfx::FloatRect&)
{
    // FIXME: Make use of the rect to reduce the invalidated area when possible.
    if (!m_element)
        return;
    if (!m_element->layout_node())
        return;
    m_element->layout_node()->set_needs_display();
}

OwnPtr<Gfx::Painter> CanvasRenderingContext2D::painter()
{
    if (!m_element)
        return {};

    if (!m_element->bitmap()) {
        if (!m_element->create_bitmap())
            return {};
    }

    return make<Gfx::Painter>(*m_element->bitmap());
}

void CanvasRenderingContext2D::fill_text(const String& text, float x, float y, Optional<double> max_width)
{
    if (max_width.has_value() && max_width.value() <= 0)
        return;

    auto painter = this->painter();
    if (!painter)
        return;

    // FIXME: painter only supports integer rects for text right now, so this effectively chops off any fractional position
    auto text_rect = Gfx::IntRect(x, y, max_width.has_value() ? max_width.value() : painter->font().width(text), painter->font().glyph_height());
    auto transformed_rect = m_drawing_state.transform.map(text_rect);
    painter->draw_text(transformed_rect, text, Gfx::TextAlignment::TopLeft, m_drawing_state.fill_style);
    did_draw(transformed_rect.to_type<float>());
}

void CanvasRenderingContext2D::stroke_text(String const& text, float x, float y, Optional<double> max_width)
{
    // FIXME: Stroke the text instead of filling it.
    fill_text(text, x, y, max_width);
}

void CanvasRenderingContext2D::begin_path()
{
    m_path = Gfx::Path();
}

void CanvasRenderingContext2D::close_path()
{
    m_path.close();
}

void CanvasRenderingContext2D::move_to(float x, float y)
{
    m_path.move_to({ x, y });
}

void CanvasRenderingContext2D::line_to(float x, float y)
{
    m_path.line_to({ x, y });
}

void CanvasRenderingContext2D::quadratic_curve_to(float cx, float cy, float x, float y)
{
    m_path.quadratic_bezier_curve_to({ cx, cy }, { x, y });
}

void CanvasRenderingContext2D::bezier_curve_to(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y)
{
    m_path.cubic_bezier_curve_to(Gfx::FloatPoint(cp1x, cp1y), Gfx::FloatPoint(cp2x, cp2y), Gfx::FloatPoint(x, y));
}

DOM::ExceptionOr<void> CanvasRenderingContext2D::arc(float x, float y, float radius, float start_angle, float end_angle, bool counter_clockwise)
{
    if (radius < 0)
        return DOM::IndexSizeError::create(String::formatted("The radius provided ({}) is negative.", radius));
    return ellipse(x, y, radius, radius, 0, start_angle, end_angle, counter_clockwise);
}

DOM::ExceptionOr<void> CanvasRenderingContext2D::ellipse(float x, float y, float radius_x, float radius_y, float rotation, float start_angle, float end_angle, bool counter_clockwise)
{
    if (radius_x < 0)
        return DOM::IndexSizeError::create(String::formatted("The major-axis radius provided ({}) is negative.", radius_x));

    if (radius_y < 0)
        return DOM::IndexSizeError::create(String::formatted("The minor-axis radius provided ({}) is negative.", radius_y));

    if (constexpr float tau = M_TAU; (!counter_clockwise && (end_angle - start_angle) >= tau)
        || (counter_clockwise && (start_angle - end_angle) >= tau)) {
        start_angle = 0;
        end_angle = tau;
    } else {
        start_angle = fmodf(start_angle, tau);
        end_angle = fmodf(end_angle, tau);
    }

    // Then, figure out where the ends of the arc are.
    // To do so, we can pretend that the center of this ellipse is at (0, 0),
    // and the whole coordinate system is rotated `rotation` radians around the x axis, centered on `center`.
    // The sign of the resulting relative positions is just whether our angle is on one of the left quadrants.
    auto sin_rotation = sinf(rotation);
    auto cos_rotation = cosf(rotation);

    auto resolve_point_with_angle = [&](float angle) {
        auto tan_relative = tanf(angle);
        auto tan2 = tan_relative * tan_relative;

        auto ab = radius_x * radius_y;
        auto a2 = radius_x * radius_x;
        auto b2 = radius_y * radius_y;
        auto sqrt = sqrtf(b2 + a2 * tan2);

        auto relative_x_position = ab / sqrt;
        auto relative_y_position = ab * tan_relative / sqrt;

        // Make sure to set the correct sign
        float sn = sinf(angle) >= 0 ? 1 : -1;
        relative_x_position *= sn;
        relative_y_position *= sn;

        // Now rotate it (back) around the center point by 'rotation' radians, then move it back to our actual origin.
        auto relative_rotated_x_position = relative_x_position * cos_rotation - relative_y_position * sin_rotation;
        auto relative_rotated_y_position = relative_x_position * sin_rotation + relative_y_position * cos_rotation;
        return Gfx::FloatPoint { relative_rotated_x_position + x, relative_rotated_y_position + y };
    };

    auto start_point = resolve_point_with_angle(start_angle);
    auto end_point = resolve_point_with_angle(end_angle);

    m_path.move_to(start_point);

    double delta_theta = end_angle - start_angle;

    // FIXME: This is still goofy for some values.
    m_path.elliptical_arc_to(end_point, { radius_x, radius_y }, rotation, delta_theta > M_PI, !counter_clockwise);

    m_path.close();
    return {};
}

void CanvasRenderingContext2D::rect(float x, float y, float width, float height)
{
    m_path.move_to({ x, y });
    if (width == 0 || height == 0)
        return;
    m_path.line_to({ x + width, y });
    m_path.line_to({ x + width, y + height });
    m_path.line_to({ x, y + height });
    m_path.close();
}

void CanvasRenderingContext2D::stroke()
{
    auto painter = this->painter();
    if (!painter)
        return;

    painter->stroke_path(m_path, m_drawing_state.stroke_style, m_drawing_state.line_width);
    did_draw(m_path.bounding_box());
}

void CanvasRenderingContext2D::fill(Gfx::Painter::WindingRule winding)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto path = m_path;
    path.close_all_subpaths();
    painter->fill_path(path, m_drawing_state.fill_style, winding);
    did_draw(m_path.bounding_box());
}

void CanvasRenderingContext2D::fill(const String& fill_rule)
{
    if (fill_rule == "evenodd")
        return fill(Gfx::Painter::WindingRule::EvenOdd);
    return fill(Gfx::Painter::WindingRule::Nonzero);
}

RefPtr<ImageData> CanvasRenderingContext2D::create_image_data(int width, int height) const
{
    if (!wrapper()) {
        dbgln("Hmm! Attempted to create ImageData for wrapper-less CRC2D.");
        return {};
    }
    return ImageData::create_with_size(wrapper()->global_object(), width, height);
}

void CanvasRenderingContext2D::put_image_data(const ImageData& image_data, float x, float y)
{
    auto painter = this->painter();
    if (!painter)
        return;

    painter->blit(Gfx::IntPoint(x, y), image_data.bitmap(), image_data.bitmap().rect());

    did_draw(Gfx::FloatRect(x, y, image_data.width(), image_data.height()));
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-save
void CanvasRenderingContext2D::save()
{
    // The save() method steps are to push a copy of the current drawing state onto the drawing state stack.
    m_drawing_state_stack.append(m_drawing_state);
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-restore
void CanvasRenderingContext2D::restore()
{
    // The restore() method steps are to pop the top entry in the drawing state stack, and reset the drawing state it describes. If there is no saved state, then the method must do nothing.
    if (m_drawing_state_stack.is_empty())
        return;
    m_drawing_state = m_drawing_state_stack.take_last();
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-reset
void CanvasRenderingContext2D::reset()
{
    // The reset() method steps are to reset the rendering context to its default state.
    reset_to_default_state();
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-iscontextlost
bool CanvasRenderingContext2D::is_context_lost()
{
    // The isContextLost() method steps are to return this's context lost.
    return m_context_lost;
}

// https://html.spec.whatwg.org/multipage/canvas.html#reset-the-rendering-context-to-its-default-state
void CanvasRenderingContext2D::reset_to_default_state()
{
    auto painter = this->painter();

    // 1. Clear canvas's bitmap to transparent black.
    if (painter)
        painter->clear_rect(painter->target()->rect(), Color::Transparent);

    // 2. Empty the list of subpaths in context's current default path.
    m_path.clear();

    // 3. Clear the context's drawing state stack.
    m_drawing_state_stack.clear();

    // 4. Reset everything that drawing state consists of to their initial values.
    m_drawing_state = {};

    if (painter)
        did_draw(painter->target()->rect().to_type<float>());
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-measuretext
RefPtr<TextMetrics> CanvasRenderingContext2D::measure_text(String const& text)
{
    // The measureText(text) method steps are to run the text preparation
    // algorithm, passing it text and the object implementing the CanvasText
    // interface, and then using the returned inline box must return a new
    // TextMetrics object with members behaving as described in the following
    // list:
    auto prepared_text = prepare_text(text);
    auto metrics = TextMetrics::create();
    // FIXME: Use the font that was used to create the glyphs in prepared_text.
    auto& font = Gfx::FontDatabase::default_font();

    // width attribute: The width of that inline box, in CSS pixels. (The text's advance width.)
    metrics->set_width(prepared_text.bounding_box.width());
    // actualBoundingBoxLeft attribute: The distance parallel to the baseline from the alignment point given by the textAlign attribute to the left side of the bounding rectangle of the given text, in CSS pixels; positive numbers indicating a distance going left from the given alignment point.
    metrics->set_actual_bounding_box_left(-prepared_text.bounding_box.left());
    // actualBoundingBoxRight attribute: The distance parallel to the baseline from the alignment point given by the textAlign attribute to the right side of the bounding rectangle of the given text, in CSS pixels; positive numbers indicating a distance going right from the given alignment point.
    metrics->set_actual_bounding_box_right(prepared_text.bounding_box.right());
    // fontBoundingBoxAscent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the ascent metric of the first available font, in CSS pixels; positive numbers indicating a distance going up from the given baseline.
    metrics->set_font_bounding_box_ascent(font.baseline());
    // fontBoundingBoxDescent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the descent metric of the first available font, in CSS pixels; positive numbers indicating a distance going down from the given baseline.
    metrics->set_font_bounding_box_descent(prepared_text.bounding_box.height() - font.baseline());
    // actualBoundingBoxAscent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the top of the bounding rectangle of the given text, in CSS pixels; positive numbers indicating a distance going up from the given baseline.
    metrics->set_actual_bounding_box_ascent(font.baseline());
    // actualBoundingBoxDescent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the bottom of the bounding rectangle of the given text, in CSS pixels; positive numbers indicating a distance going down from the given baseline.
    metrics->set_actual_bounding_box_descent(prepared_text.bounding_box.height() - font.baseline());
    // emHeightAscent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the highest top of the em squares in the inline box, in CSS pixels; positive numbers indicating that the given baseline is below the top of that em square (so this value will usually be positive). Zero if the given baseline is the top of that em square; half the font size if the given baseline is the middle of that em square.
    metrics->set_em_height_ascent(font.baseline());
    // emHeightDescent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the lowest bottom of the em squares in the inline box, in CSS pixels; positive numbers indicating that the given baseline is above the bottom of that em square. (Zero if the given baseline is the bottom of that em square.)
    metrics->set_em_height_descent(prepared_text.bounding_box.height() - font.baseline());
    // hangingBaseline attribute: The distance from the horizontal line indicated by the textBaseline attribute to the hanging baseline of the inline box, in CSS pixels; positive numbers indicating that the given baseline is below the hanging baseline. (Zero if the given baseline is the hanging baseline.)
    metrics->set_hanging_baseline(font.baseline());
    // alphabeticBaseline attribute: The distance from the horizontal line indicated by the textBaseline attribute to the alphabetic baseline of the inline box, in CSS pixels; positive numbers indicating that the given baseline is below the alphabetic baseline. (Zero if the given baseline is the alphabetic baseline.)
    metrics->set_font_bounding_box_ascent(0);
    // ideographicBaseline attribute: The distance from the horizontal line indicated by the textBaseline attribute to the ideographic-under baseline of the inline box, in CSS pixels; positive numbers indicating that the given baseline is below the ideographic-under baseline. (Zero if the given baseline is the ideographic-under baseline.)
    metrics->set_font_bounding_box_ascent(0);

    return metrics;
}

// https://html.spec.whatwg.org/multipage/canvas.html#text-preparation-algorithm
CanvasRenderingContext2D::PreparedText CanvasRenderingContext2D::prepare_text(String const& text, float max_width)
{
    // 1. If maxWidth was provided but is less than or equal to zero or equal to NaN, then return an empty array.
    if (max_width <= 0 || max_width != max_width) {
        return {};
    }

    // 2. Replace all ASCII whitespace in text with U+0020 SPACE characters.
    // NOTE: This also replaces vertical tabs with space even though WHATWG
    //       doesn't consider it as whitespace.
    StringBuilder builder { text.length() };
    for (auto c : text) {
        builder.append(is_ascii_space(c) ? ' ' : c);
    }
    String replaced_text = builder.build();

    // 3. Let font be the current font of target, as given by that object's font attribute.
    // FIXME: Once we have CanvasTextDrawingStyles, implement font selection.

    // 4. Apply the appropriate step from the following list to determine the value of direction:
    //   4.1. If the target object's direction attribute has the value "ltr": Let direction be 'ltr'.
    //   4.2. If the target object's direction attribute has the value "rtl": Let direction be 'rtl'.
    //   4.3. If the target object's font style source object is an element: Let direction be the directionality of the target object's font style source object.
    //   4.4. If the target object's font style source object is a Document with a non-null document element: Let direction be the directionality of the target object's font style source object's document element.
    //   4.5. Otherwise: Let direction be 'ltr'.
    // FIXME: Once we have CanvasTextDrawingStyles, implement directionality.

    // 5. Form a hypothetical infinitely-wide CSS line box containing a single inline box containing the text text, with its CSS properties set as follows:
    //   'direction'         -> direction
    //   'font'              -> font
    //   'font-kerning'      -> target's fontKerning
    //   'font-stretch'      -> target's fontStretch
    //   'font-variant-caps' -> target's fontVariantCaps
    //   'letter-spacing'    -> target's letterSpacing
    //   SVG text-rendering  -> target's textRendering
    //   'white-space'       -> 'pre'
    //   'word-spacing'      -> target's wordSpacing
    // ...and with all other properties set to their initial values.
    // FIXME: Actually use a LineBox here instead of, you know, using the default font and measuring its size (which is not the spec at all).
    // FIXME: Once we have CanvasTextDrawingStyles, add the CSS attributes.
    auto& font = Gfx::FontDatabase::default_font();
    size_t width = 0;
    size_t height = font.glyph_height();
    for (auto c : Utf8View { replaced_text }) {
        width += font.glyph_or_emoji_width(c);
    }

    // 6. If maxWidth was provided and the hypothetical width of the inline box in the hypothetical line box is greater than maxWidth CSS pixels, then change font to have a more condensed font (if one is available or if a reasonably readable one can be synthesized by applying a horizontal scale factor to the font) or a smaller font, and return to the previous step.
    // FIXME: Record the font size used for this piece of text, and actually retry with a smaller size if needed.

    // 7. The anchor point is a point on the inline box, and the physical alignment is one of the values left, right, and center. These variables are determined by the textAlign and textBaseline values as follows:
    // Horizontal position:
    //   7.1. If textAlign is left, if textAlign is start and direction is 'ltr' or if textAlign is end and direction is 'rtl': Let the anchor point's horizontal position be the left edge of the inline box, and let physical alignment be left.
    //   7.2. If textAlign is right, if textAlign is end and direction is 'ltr' or if textAlign is start and direction is 'rtl': Let the anchor point's horizontal position be the right edge of the inline box, and let physical alignment be right.
    //   7.3. If textAlign is center: Let the anchor point's horizontal position be half way between the left and right edges of the inline box, and let physical alignment be center.
    // Vertical position:
    //   7.4. If textBaseline is top: Let the anchor point's vertical position be the top of the em box of the first available font of the inline box.
    //   7.5. If textBaseline is hanging: Let the anchor point's vertical position be the hanging baseline of the first available font of the inline box.
    //   7.6. If textBaseline is middle: Let the anchor point's vertical position be half way between the bottom and the top of the em box of the first available font of the inline box.
    //   7.7. If textBaseline is alphabetic: Let the anchor point's vertical position be the alphabetic baseline of the first available font of the inline box.
    //   7.8. If textBaseline is ideographic: Let the anchor point's vertical position be the ideographic-under baseline of the first available font of the inline box.
    //   7.9. If textBaseline is bottom: Let the anchor point's vertical position be the bottom of the em box of the first available font of the inline box.
    // FIXME: Once we have CanvasTextDrawingStyles, handle the alignment and baseline.
    Gfx::IntPoint anchor { 0, 0 };
    auto physical_alignment = Gfx::TextAlignment::CenterLeft;

    // 8. Let result be an array constructed by iterating over each glyph in the inline box from left to right (if any), adding to the array, for each glyph, the shape of the glyph as it is in the inline box, positioned on a coordinate space using CSS pixels with its origin is at the anchor point.
    PreparedText prepared_text { {}, physical_alignment, { 0, 0, static_cast<int>(width), static_cast<int>(height) } };
    prepared_text.glyphs.ensure_capacity(replaced_text.length());

    size_t offset = 0;
    for (auto c : Utf8View { replaced_text }) {
        prepared_text.glyphs.append({ c, { static_cast<int>(offset), 0 } });
        offset += font.glyph_or_emoji_width(c);
    }

    // 9. Return result, physical alignment, and the inline box.
    return prepared_text;
}

NonnullRefPtr<CanvasGradient> CanvasRenderingContext2D::create_radial_gradient(double x0, double y0, double r0, double x1, double y1, double r1)
{
    return CanvasGradient::create_radial(x0, y0, r0, x1, y1, r1);
}

NonnullRefPtr<CanvasGradient> CanvasRenderingContext2D::create_linear_gradient(double x0, double y0, double x1, double y1)
{
    return CanvasGradient::create_linear(x0, y0, x1, y1);
}

NonnullRefPtr<CanvasGradient> CanvasRenderingContext2D::create_conic_gradient(double start_angle, double x, double y)
{
    return CanvasGradient::create_conic(start_angle, x, y);
}

}
