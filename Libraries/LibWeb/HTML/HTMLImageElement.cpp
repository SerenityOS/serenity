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
#include <LibWeb/CSS/Parser/CSSParser.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/LayoutImage.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::HTML {

HTMLImageElement::HTMLImageElement(DOM::Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
{
    m_image_loader.on_load = [this] {
        this->document().update_layout();
        dispatch_event(DOM::Event::create("load"));
    };

    m_image_loader.on_fail = [this] {
        dbg() << "HTMLImageElement: Resource did fail: " << this->src();
        this->document().update_layout();
        dispatch_event(DOM::Event::create("error"));
    };

    m_image_loader.on_animate = [this] {
        if (layout_node())
            layout_node()->set_needs_display();
    };
}

HTMLImageElement::~HTMLImageElement()
{
}

void HTMLImageElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name == HTML::AttributeNames::width) {
            if (auto parsed_value = parse_html_length(document(), value)) {
                style.set_property(CSS::PropertyID::Width, parsed_value.release_nonnull());
            }
        } else if (name == HTML::AttributeNames::height) {
            if (auto parsed_value = parse_html_length(document(), value)) {
                style.set_property(CSS::PropertyID::Height, parsed_value.release_nonnull());
            }
        }
    });
}

void HTMLImageElement::parse_attribute(const FlyString& name, const String& value)
{
    HTMLElement::parse_attribute(name, value);

    if (name == HTML::AttributeNames::src)
        m_image_loader.load(document().complete_url(value));
}

RefPtr<LayoutNode> HTMLImageElement::create_layout_node(const CSS::StyleProperties* parent_style)
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    if (style->display() == CSS::Display::None)
        return nullptr;
    return adopt(*new LayoutImage(document(), *this, move(style), m_image_loader));
}

const Gfx::Bitmap* HTMLImageElement::bitmap() const
{
    return m_image_loader.bitmap();
}

}
