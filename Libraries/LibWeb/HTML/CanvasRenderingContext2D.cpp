/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/OwnPtr.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/HTML/CanvasRenderingContext2D.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/ImageData.h>

namespace Web::HTML {

CanvasRenderingContext2D::CanvasRenderingContext2D(HTMLCanvasElement& element)
    : m_element(element.make_weak_ptr())
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

    painter->draw_scaled_bitmap(enclosing_int_rect(rect), *image_element.bitmap(), src_rect);
}

void CanvasRenderingContext2D::scale(float sx, float sy)
{
    dbg() << "CanvasRenderingContext2D::scale(): " << sx << ", " << sy;
    m_transform.scale(sx, sy);
}

void CanvasRenderingContext2D::translate(float tx, float ty)
{
    dbg() << "CanvasRenderingContext2D::translate(): " << tx << ", " << ty;
    m_transform.translate(tx, ty);
}

void CanvasRenderingContext2D::rotate(float radians)
{
    dbg() << "CanvasRenderingContext2D::rotate(): " << radians;
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
        return nullptr;

    if (!m_element->bitmap()) {
        if (!m_element->create_bitmap())
            return nullptr;
    }

    return make<Gfx::Painter>(*m_element->bitmap());
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

void CanvasRenderingContext2D::stroke()
{
    auto painter = this->painter();
    if (!painter)
        return;

    painter->stroke_path(m_path, m_stroke_style, m_line_width);
}

void CanvasRenderingContext2D::fill(Gfx::Painter::WindingRule winding)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto path = m_path;
    path.close_all_subpaths();
    painter->fill_path(path, m_fill_style, winding);
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
        dbg() << "Hmm! Attempted to create ImageData for wrapper-less CRC2D.";
        return nullptr;
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
