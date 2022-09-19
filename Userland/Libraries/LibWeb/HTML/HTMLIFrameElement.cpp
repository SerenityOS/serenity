/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/Layout/FrameBox.h>

namespace Web::HTML {

HTMLIFrameElement::HTMLIFrameElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : BrowsingContextContainer(document, move(qualified_name))
{
    set_prototype(&document.window().cached_web_prototype("HTMLIFrameElement"));
}

HTMLIFrameElement::~HTMLIFrameElement() = default;

RefPtr<Layout::Node> HTMLIFrameElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return adopt_ref(*new Layout::FrameBox(document(), *this, move(style)));
}

void HTMLIFrameElement::parse_attribute(FlyString const& name, String const& value)
{
    HTMLElement::parse_attribute(name, value);
    if (name == HTML::AttributeNames::src)
        load_src(value);
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#the-iframe-element:the-iframe-element-6
void HTMLIFrameElement::inserted()
{
    HTMLElement::inserted();

    // When an iframe element element is inserted into a document whose browsing context is non-null, the user agent must run these steps:
    if (document().browsing_context()) {
        // 1. Create a new nested browsing context for element.
        create_new_nested_browsing_context();

        // FIXME: 2. If element has a sandbox attribute, then parse the sandboxing directive given the attribute's value and element's iframe sandboxing flag set.

        // 3. Process the iframe attributes for element, with initialInsertion set to true.
        load_src(attribute(HTML::AttributeNames::src));
    }
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#the-iframe-element:the-iframe-element-7
void HTMLIFrameElement::removed_from(DOM::Node* node)
{
    HTMLElement::removed_from(node);
    discard_nested_browsing_context();
}

void HTMLIFrameElement::load_src(String const& value)
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
    element.dispatch_event(*DOM::Event::create(element.document().window(), HTML::EventNames::load));

    // FIXME: 6. Unset childDocument's iframe load in progress flag.
}

}
