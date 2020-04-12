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
#include <LibWeb/DOM/CanvasRenderingContext2D.h>
#include <LibWeb/DOM/HTMLCanvasElement.h>

namespace Web {

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
    painter->draw_rect(enclosing_int_rect(rect), m_stroke_style);
    did_draw(rect);
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

    return make<Gfx::Painter>(m_element->ensure_bitmap());
}

}
