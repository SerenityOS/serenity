/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/HTMLObjectElement.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::HTML {

HTMLObjectElement::HTMLObjectElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : FormAssociatedElement(document, move(qualified_name))
    , m_image_loader(*this)
{
    m_image_loader.on_load = [this] {
        m_should_show_fallback_content = false;
        set_needs_style_update(true);
    };

    m_image_loader.on_fail = [this] {
        m_should_show_fallback_content = true;
        set_needs_style_update(true);
    };
}

HTMLObjectElement::~HTMLObjectElement()
{
}

void HTMLObjectElement::parse_attribute(const FlyString& name, const String& value)
{
    HTMLElement::parse_attribute(name, value);

    if (name == HTML::AttributeNames::data)
        m_image_loader.load(document().parse_url(value));
}

RefPtr<Layout::Node> HTMLObjectElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    if (m_should_show_fallback_content)
        return HTMLElement::create_layout_node(move(style));
    if (m_image_loader.has_image())
        return adopt_ref(*new Layout::ImageBox(document(), *this, move(style), m_image_loader));
    return nullptr;
}

}
