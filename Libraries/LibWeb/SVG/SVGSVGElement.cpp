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

#include <LibGfx/Painter.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/Layout/LayoutSVG.h>
#include <LibWeb/SVG/SVGPathElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>
#include <ctype.h>

namespace Web::SVG {

static constexpr auto max_svg_area = 16384 * 16384;

SVGSVGElement::SVGSVGElement(DOM::Document& document, const FlyString& tag_name)
    : SVGGraphicsElement(document, tag_name)
{
}

RefPtr<LayoutNode> SVGSVGElement::create_layout_node(const CSS::StyleProperties* parent_style)
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    if (style->display() == CSS::Display::None)
        return nullptr;
    return adopt(*new LayoutSVG(document(), *this, move(style)));
}

static Gfx::IntSize bitmap_size_for_canvas(const SVGSVGElement& canvas)
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

bool SVGSVGElement::create_bitmap_as_top_level_svg_element()
{
    auto size = bitmap_size_for_canvas(*this);
    if (size.is_empty()) {
        m_bitmap = nullptr;
        return false;
    }

    if (!m_bitmap || m_bitmap->size() != size)
        m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, size);

    Gfx::Painter painter(*m_bitmap);
    paint(painter, make_painting_context_from(default_painting_context));

    return m_bitmap;
}

unsigned SVGSVGElement::width() const
{
    return attribute(HTML::AttributeNames::width).to_uint().value_or(300);
}

unsigned SVGSVGElement::height() const
{
    return attribute(HTML::AttributeNames::height).to_uint().value_or(150);
}

void SVGSVGElement::paint(Gfx::Painter& painter, const SVGPaintingContext& context)
{
    for_each_child([&](Node& child) {
        if (is<SVGGraphicsElement>(child)) {
            downcast<SVGGraphicsElement>(child).paint(painter, make_painting_context_from(context));
        }
    });
}

}
