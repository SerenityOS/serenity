/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/URL.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::HTML {

HTMLLinkElement::HTMLLinkElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
    , m_css_loader(*this)
{
    m_css_loader.on_load = [&] {
        document.update_style();
    };
}

HTMLLinkElement::~HTMLLinkElement()
{
}

void HTMLLinkElement::inserted()
{
    HTMLElement::inserted();

    if (m_relationship & Relationship::Stylesheet && !(m_relationship & Relationship::Alternate)) {
        m_css_loader.load_from_url(document().parse_url(href()));
        if (auto sheet = m_css_loader.style_sheet())
            document().style_sheets().add_sheet(sheet.release_nonnull());
    }
}

void HTMLLinkElement::parse_attribute(const FlyString& name, const String& value)
{
    if (name == HTML::AttributeNames::rel) {
        m_relationship = 0;
        auto parts = value.split_view(' ');
        for (auto& part : parts) {
            if (part == "stylesheet")
                m_relationship |= Relationship::Stylesheet;
            else if (part == "alternate")
                m_relationship |= Relationship::Alternate;
        }
    }
}

}
