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
#include <LibGfx/ImageDecoder.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLImageElement.h>
#include <LibHTML/Layout/LayoutImage.h>
#include <LibHTML/ResourceLoader.h>

namespace Web {

HTMLImageElement::HTMLImageElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLImageElement::~HTMLImageElement()
{
}

void HTMLImageElement::parse_attribute(const String& name, const String& value)
{
    if (name.equals_ignoring_case("src"))
        load_image(value);
}

void HTMLImageElement::load_image(const String& src)
{
    URL src_url = document().complete_url(src);
    ResourceLoader::the().load(src_url, [this, weak_element = make_weak_ptr()](auto data) {
        if (!weak_element) {
            dbg() << "HTMLImageElement: Load completed after element destroyed.";
            return;
        }
        if (data.is_null()) {
            dbg() << "HTMLImageElement: Failed to load " << this->src();
            return;
        }

        m_encoded_data = data;
        m_image_decoder = Gfx::ImageDecoder::create(m_encoded_data.data(), m_encoded_data.size());
        document().update_layout();
    });
}

int HTMLImageElement::preferred_width() const
{
    bool ok = false;
    int width = attribute("width").to_int(ok);
    if (ok)
        return width;

    if (m_image_decoder)
        return m_image_decoder->width();

    return 0;
}

int HTMLImageElement::preferred_height() const
{
    bool ok = false;
    int height = attribute("height").to_int(ok);
    if (ok)
        return height;

    if (m_image_decoder)
        return m_image_decoder->height();

    return 0;
}

RefPtr<LayoutNode> HTMLImageElement::create_layout_node(const StyleProperties* parent_style) const
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    auto display = style->string_or_fallback(CSS::PropertyID::Display, "inline");
    if (display == "none")
        return nullptr;
    return adopt(*new LayoutImage(*this, move(style)));
}

const Gfx::Bitmap* HTMLImageElement::bitmap() const
{
    if (!m_image_decoder)
        return nullptr;
    return m_image_decoder->bitmap();
}

void HTMLImageElement::set_volatile(Badge<LayoutDocument>, bool v)
{
    if (!m_image_decoder)
        return;
    if (v) {
        m_image_decoder->set_volatile();
        return;
    }
    bool has_image = m_image_decoder->set_nonvolatile();
    if (has_image)
        return;
    m_image_decoder = Gfx::ImageDecoder::create(m_encoded_data.data(), m_encoded_data.size());
}

}
