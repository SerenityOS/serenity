/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/Layout/FrameBox.h>
#include <LibWeb/Origin.h>
#include <LibWeb/Page/Frame.h>

namespace Web::HTML {

HTMLIFrameElement::HTMLIFrameElement(DOM::Document& document, QualifiedName qualified_name)
    : FrameHostElement(document, move(qualified_name))
{
}

HTMLIFrameElement::~HTMLIFrameElement()
{
}

RefPtr<Layout::Node> HTMLIFrameElement::create_layout_node()
{
    auto style = document().style_resolver().resolve_style(*this);
    return adopt_ref(*new Layout::FrameBox(document(), *this, move(style)));
}

void HTMLIFrameElement::parse_attribute(const FlyString& name, const String& value)
{
    HTMLElement::parse_attribute(name, value);
    if (name == HTML::AttributeNames::src)
        load_src(value);
}

void HTMLIFrameElement::inserted()
{
    FrameHostElement::inserted();
    if (is_connected())
        load_src(attribute(HTML::AttributeNames::src));
}

void HTMLIFrameElement::load_src(const String& value)
{
    if (!m_content_frame)
        return;

    if (value.is_null())
        return;

    auto url = document().complete_url(value);
    if (!url.is_valid()) {
        dbgln("iframe failed to load URL: Invalid URL: {}", value);
        return;
    }
    if (url.protocol() == "file" && document().origin().protocol() != "file") {
        dbgln("iframe failed to load URL: Security violation: {} may not load {}", document().url(), url);
        return;
    }

    dbgln("Loading iframe document from {}", value);
    m_content_frame->loader().load(url, FrameLoader::Type::IFrame);
}

}
