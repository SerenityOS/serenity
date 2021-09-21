/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ExtraMathConstants.h>
#include <AK/OwnPtr.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/HTML/CanvasRenderingContext2D.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/ImageData.h>

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
    m_fill_style = Gfx::Color::from_string(style).value_or(Color::Black);
}

String CanvasRenderingContext2D::fill_style() const
{
    return m_fill_style.to_string();
}

void CanvasRenderingContext2D::fill_rect(float x, float y, float width, float height)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto rect = m_transform.map(Gfx::FloatRect(x, y, width, height));
    painter->fill_rect(enclosing_int_rect(rect), m_fill_style);
    did_draw(rect);
}

void CanvasRenderingContext2D::clear_rect(float x, float y, float width, float height)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto rect = m_transform.map(Gfx::FloatRect(x, y, width, height));
    painter->clear_rect(enclosing_int_rect(rect), Color());
    did_draw(rect);
}

void CanvasRenderingContext2D::set_stroke_style(String style)
{
    m_stroke_style = Gfx::Color::from_string(style).value_or(Color::Black);
}

String CanvasRenderingContext2D::stroke_style() const
{
    return m_fill_style.to_string();
}

void CanvasRenderingContext2D::stroke_rect(float x, float y, float width, float height)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto rect = m_transform.map(Gfx::FloatRect(x, y, width, height));

    auto top_left = m_transform.map(Gfx::FloatPoint(x, y)).to_type<int>();
    auto top_right = m_transform.map(Gfx::FloatPoint(x + width - 1, y)).to_type<int>();
    auto bottom_left = m_transform.map(Gfx::FloatPoint(x, y + height - 1)).to_type<int>();
    auto bottom_right = m_transform.map(Gfx::FloatPoint(x + width - 1, y + height - 1)).to_type<int>();

    painter->draw_line(top_left, top_right, m_stroke_style, m_line_width);
    painter->draw_line(top_right, bottom_right, m_stroke_style, m_line_width);
    painter->draw_line(bottom_right, bottom_left, m_stroke_style, m_line_width);
    painter->draw_line(bottom_left, top_left, m_stroke_style, m_line_width);

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
    auto rect = m_transform.map(dst_rect);

    painter->draw_scaled_bitmap(rounded_int_rect(rect), *image_element.bitmap(), src_rect, 1.0f, Gfx::Painter::ScalingMode::BilinearBlend);
}

void CanvasRenderingContext2D::scale(float sx, float sy)
{
    dbgln("CanvasRenderingContext2D::scale({}, {})", sx, sy);
    m_transform.scale(sx, sy);
}

void CanvasRenderingContext2D::translate(float tx, float ty)
{
    dbgln("CanvasRenderingContext2D::translate({}, {})", tx, ty);
    m_transform.translate(tx, ty);
}

void CanvasRenderingContext2D::rotate(float radians)
{
    dbgln("CanvasRenderingContext2D::rotate({})", radians);
    m_transform.rotate_radians(radians);
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
    auto transformed_rect = m_transform.map(text_rect);
    painter->draw_text(transformed_rect, text, Gfx::TextAlignment::TopLeft, m_fill_style);
    did_draw(transformed_rect.to_type<float>());
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

    painter->stroke_path(m_path, m_stroke_style, m_line_width);
    did_draw(m_path.bounding_box());
}

void CanvasRenderingContext2D::fill(Gfx::Painter::WindingRule winding)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto path = m_path;
    path.close_all_subpaths();
    painter->fill_path(path, m_fill_style, winding);
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

}
