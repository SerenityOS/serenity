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

#include <LibGfx/Bitmap.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/CanvasRenderingContext2D.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/HTMLCanvasElement.h>
#include <LibWeb/Layout/LayoutCanvas.h>

namespace Web {

HTMLCanvasElement::HTMLCanvasElement(Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLCanvasElement::~HTMLCanvasElement()
{
}

int HTMLCanvasElement::preferred_width() const
{
    bool ok = false;
    int width = attribute("width").to_int(ok);
    if (ok)
        return width;

    return 300;
}

int HTMLCanvasElement::preferred_height() const
{
    bool ok = false;
    int height = attribute("height").to_int(ok);
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
    ASSERT(type.to_lowercase() == "2d");
    if (!m_context)
        m_context = CanvasRenderingContext2D::create(*this);
    return m_context;
}

Gfx::Bitmap& HTMLCanvasElement::ensure_bitmap()
{
    if (!m_bitmap || m_bitmap->size() != Gfx::Size(preferred_width(), preferred_height())) {
        m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, { preferred_width(), preferred_height() });
    }
    return *m_bitmap;
}

}
