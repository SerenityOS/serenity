/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Checked.h>
#include <LibGfx/Bitmap.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/CanvasRenderingContext2D.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/HTMLCanvasElement.h>
#include <LibWeb/Layout/LayoutCanvas.h>

namespace Web {

static constexpr auto max_canvas_area = 16384 * 16384;

HTMLCanvasElement::HTMLCanvasElement(Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLCanvasElement::~HTMLCanvasElement()
{
}

int HTMLCanvasElement::requested_width() const
{
    bool ok = false;
    unsigned width = attribute("width").to_int(ok);
    if (ok)
        return width;

    return 300;
}

int HTMLCanvasElement::requested_height() const
{
    bool ok = false;
    unsigned height = attribute("height").to_int(ok);
    if (ok)
        return height;

    return 150;
}

RefPtr<LayoutNode> HTMLCanvasElement::create_layout_node(const StyleProperties* parent_style) const
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    auto display = style->string_or_fallback(CSS::PropertyID::Display, "inline");
    if (display == "none")
        return nullptr;
    return adopt(*new LayoutCanvas(*this, move(style)));
}

CanvasRenderingContext2D* HTMLCanvasElement::get_context(String type)
{
    ASSERT(type == "2d");
    if (!m_context)
        m_context = CanvasRenderingContext2D::create(*this);
    return m_context;
}

static Gfx::Size bitmap_size_for_canvas(const HTMLCanvasElement& canvas)
{
    int width = canvas.requested_width();
    int height = canvas.requested_height();
    if (width < 0 || height < 0) {
        dbg() << "Refusing to create canvas with negative size";
        return {};
    }

    Checked<size_t> area = width;
    area *= height;

    if (area.has_overflow()) {
        dbg() << "Refusing to create " << width << "x" << height << " canvas (overflow)";
        return {};
    }
    if (area.value() > max_canvas_area) {
        dbg() << "Refusing to create " << width << "x" << height << " canvas (exceeds maximum size)";
        return {};
    }
    return { width, height };
}

bool HTMLCanvasElement::create_bitmap()
{
    auto size = bitmap_size_for_canvas(*this);
    if (size.is_empty()) {
        m_bitmap = nullptr;
        return false;
    }
    if (!m_bitmap || m_bitmap->size() != size)
        m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, size);
    return m_bitmap;
}

}
