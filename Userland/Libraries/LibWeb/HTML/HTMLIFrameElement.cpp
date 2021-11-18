/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/Layout/FrameBox.h>
#include <LibWeb/Origin.h>

namespace Web::HTML {

HTMLIFrameElement::HTMLIFrameElement(DOM::Document& document, QualifiedName qualified_name)
    : BrowsingContextContainer(document, move(qualified_name))
{
}

HTMLIFrameElement::~HTMLIFrameElement()
{
}

RefPtr<Layout::Node> HTMLIFrameElement::create_layout_node()
{
    auto style = document().style_computer().compute_style(*this);
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
    BrowsingContextContainer::inserted();
    if (is_connected())
        load_src(attribute(HTML::AttributeNames::src));
}

void HTMLIFrameElement::load_src(const String& value)
{
    if (!m_nested_browsing_context)
        return;

    if (value.is_null())
        return;

    auto url = document().parse_url(value);
    if (!url.is_valid()) {
        dbgln("iframe failed to load URL: Invalid URL: {}", value);
        return;
    }
    if (url.protocol() == "file" && document().origin().protocol() != "file") {
        dbgln("iframe failed to load URL: Security violation: {} may not load {}", document().url(), url);
        return;
    }

    dbgln("Loading iframe document from {}", value);
    m_nested_browsing_context->loader().load(url, FrameLoader::Type::IFrame);
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#iframe-load-event-steps
void run_iframe_load_event_steps(HTML::HTMLIFrameElement& element)
{
    // 1. Assert: element's nested browsing context is not null.
    VERIFY(element.nested_browsing_context());

    // 2. Let childDocument be the active document of element's nested browsing context.
    [[maybe_unused]] auto* child_document = element.nested_browsing_context()->active_document();

    // FIXME: 3. If childDocument has its mute iframe load flag set, then return.

    // FIXME: 4. Set childDocument's iframe load in progress flag.

    // 5. Fire an event named load at element.
    element.dispatch_event(DOM::Event::create(HTML::EventNames::load));

    // FIXME: 6. Unset childDocument's iframe load in progress flag.
}

}
