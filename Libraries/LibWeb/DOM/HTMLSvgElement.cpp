/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <LibGfx/Path.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/HTMLPathElement.h>
#include <LibWeb/DOM/HTMLSvgElement.h>
#include <LibWeb/Layout/LayoutSvg.h>
#include <ctype.h>

namespace Web {

static constexpr auto max_svg_area = 16384 * 16384;

HTMLSvgElement::HTMLSvgElement(Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
{
}

void HTMLSvgElement::parse_attribute(const FlyString& name, const String& value)
{
    HTMLElement::parse_attribute(name, value);
    if (name == "stroke") {
        m_stroke_color = Gfx::Color::from_string(value);
    } else if (name == "stroke-width") {
        auto result = value.to_int();
        if (result.has_value())
            m_stroke_width = result.value();
    } else if (name == "fill") {
        m_fill_color = Gfx::Color::from_string(value);
    }
}

RefPtr<LayoutNode> HTMLSvgElement::create_layout_node(const StyleProperties* parent_style)
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    if (style->display() == CSS::Display::None)
        return nullptr;
    return adopt(*new LayoutSvg(document(), *this, move(style)));
}

static Gfx::IntSize bitmap_size_for_canvas(const HTMLSvgElement& canvas)
{
    auto width = canvas.width();
    auto height = canvas.height();

    Checked<size_t> area = width;
    area *= height;

    if (area.has_overflow()) {
        dbg() << "Refusing to create " << width << "x" << height << " svg (overflow)";
        return {};
    }
    if (area.value() > max_svg_area) {
        dbg() << "Refusing to create " << width << "x" << height << " svg (exceeds maximum size)";
        return {};
    }
    return Gfx::IntSize(width, height);
}

bool HTMLSvgElement::create_bitmap()
{
    auto size = bitmap_size_for_canvas(*this);
    if (size.is_empty()) {
        m_bitmap = nullptr;
        return false;
    }

    if (!m_bitmap || m_bitmap->size() != size)
        m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, size);

    Gfx::Painter painter(*m_bitmap);
    paint(painter);

    return m_bitmap;
}

SvgPaintingContext HTMLSvgElement::make_context() const
{
    SvgPaintingContext context;

    if (m_stroke_width.has_value())
        context.stroke_width = m_stroke_width.value();
    if (m_stroke_color.has_value())
        context.stroke_color = m_stroke_color.value();
    if (m_fill_color.has_value())
        context.fill_color = m_fill_color.value();

    return context;
}

unsigned HTMLSvgElement::width() const
{
    return attribute(HTML::AttributeNames::width).to_uint().value_or(300);
}

unsigned HTMLSvgElement::height() const
{
    return attribute(HTML::AttributeNames::height).to_uint().value_or(150);
}

static bool is_svg_graphic_element(const HTMLElement& element)
{
    return is<HTMLPathElement>(element);
}

static SvgGraphicElement& as_svg_graphic_element(HTMLElement& element)
{
    if (is<HTMLPathElement>(element))
        return to<HTMLPathElement>(element);
    ASSERT_NOT_REACHED();
}

void HTMLSvgElement::paint(Gfx::Painter& painter)
{
    for_each_child([&](Node& child) {
        if (is<HTMLElement>(child)) {
            auto& element = to<HTMLElement>(child);
            if (is_svg_graphic_element(element)) {
                as_svg_graphic_element(element).paint(make_context(), painter);
            }
        }
    });
}

}
