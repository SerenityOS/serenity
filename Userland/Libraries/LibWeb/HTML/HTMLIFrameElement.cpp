/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/Navigable.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Layout/FrameBox.h>

namespace Web::HTML {

HTMLIFrameElement::HTMLIFrameElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : NavigableContainer(document, move(qualified_name))
{
}

HTMLIFrameElement::~HTMLIFrameElement() = default;

void HTMLIFrameElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLIFrameElementPrototype>(realm, "HTMLIFrameElement"));
}

JS::GCPtr<Layout::Node> HTMLIFrameElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::FrameBox>(document(), *this, move(style));
}

void HTMLIFrameElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    HTMLElement::attribute_changed(name, value);
    if (m_content_navigable)
        process_the_iframe_attributes();
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#the-iframe-element:the-iframe-element-6
void HTMLIFrameElement::inserted()
{
    HTMLElement::inserted();

    // When an iframe element element is inserted into a document whose browsing context is non-null, the user agent must run these steps:
    if (document().browsing_context()) {
        // 1. Create a new child navigable for element.
        MUST(create_new_child_navigable());

        // FIXME: 2. If element has a sandbox attribute, then parse the sandboxing directive given the attribute's value and element's iframe sandboxing flag set.

        // 3. Process the iframe attributes for element, with initialInsertion set to true.
        process_the_iframe_attributes(true);
    }
}

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#will-lazy-load-element-steps
bool HTMLIFrameElement::will_lazy_load_element() const
{
    // 1. If scripting is disabled for element, then return false.
    if (document().is_scripting_disabled())
        return false;

    // FIXME: 2. If element's lazy loading attribute is in the Lazy state, then return true.

    // 3. Return false.
    return false;
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#process-the-iframe-attributes
void HTMLIFrameElement::process_the_iframe_attributes(bool initial_insertion)
{
    if (!content_navigable())
        return;

    // 1. If element's srcdoc attribute is specified, then:
    if (has_attribute(HTML::AttributeNames::srcdoc)) {
        // 1. Set element's current navigation was lazy loaded boolean to false.
        m_current_navigation_was_lazy_loaded = false;

        // 2. If the will lazy load element steps given element return true, then:
        if (will_lazy_load_element()) {
            // FIXME: 1. Set element's lazy load resumption steps to the rest of this algorithm starting with the step labeled navigate to the srcdoc resource.
            // FIXME: 2. Set element's current navigation was lazy loaded boolean to true.
            // FIXME: 3. Start intersection-observing a lazy loading element for element.
            // FIXME: 4. Return.
        }

        // 3. Navigate to the srcdoc resource: navigate an iframe or frame given element, about:srcdoc, the empty string, and the value of element's srcdoc attribute.
        navigate_an_iframe_or_frame(AK::URL("about:srcdoc"sv), ReferrerPolicy::ReferrerPolicy::EmptyString, String::from_deprecated_string(get_attribute(HTML::AttributeNames::srcdoc)).release_value_but_fixme_should_propagate_errors());

        // FIXME: The resulting Document must be considered an iframe srcdoc document.

        return;
    }

    // 1. Let url be the result of running the shared attribute processing steps for iframe and frame elements given element and initialInsertion.
    auto url = shared_attribute_processing_steps_for_iframe_and_frame(initial_insertion);

    // 2. If url is null, then return.
    if (!url.has_value()) {
        return;
    }

    // 3. If url matches about:blank and initialInsertion is true, then:
    if (url_matches_about_blank(*url) && initial_insertion) {
        // 1. Run the iframe load event steps given element.
        run_iframe_load_event_steps(*this);

        // 2. Return.
        return;
    }

    // FIXME: 4. Let referrerPolicy be the current state of element's referrerpolicy content attribute.
    auto referrer_policy = ReferrerPolicy::ReferrerPolicy::EmptyString;

    // 5. Set element's current navigation was lazy loaded boolean to false.
    m_current_navigation_was_lazy_loaded = false;

    // 6. If the will lazy load element steps given element return true, then:
    if (will_lazy_load_element()) {
        // FIXME: 1. Set element's lazy load resumption steps to the rest of this algorithm starting with the step labeled navigate.
        // FIXME: 2. Set element's current navigation was lazy loaded boolean to true.
        // FIXME: 3. Start intersection-observing a lazy loading element for element.
        // 4. Return.
        return;
    }

    // 7. Navigate: navigate an iframe or frame given element, url, and referrerPolicy.
    navigate_an_iframe_or_frame(*url, referrer_policy);
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#the-iframe-element:the-iframe-element-7
void HTMLIFrameElement::removed_from(DOM::Node* node)
{
    HTMLElement::removed_from(node);

    // When an iframe element is removed from a document, the user agent must destroy the nested navigable of the element.
    destroy_the_child_navigable();
}

// https://html.spec.whatwg.org/multipage/rendering.html#attributes-for-embedded-content-and-images
void HTMLIFrameElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name == HTML::AttributeNames::width) {
            if (auto parsed_value = parse_dimension_value(value))
                style.set_property(CSS::PropertyID::Width, parsed_value.release_nonnull());
        } else if (name == HTML::AttributeNames::height) {
            if (auto parsed_value = parse_dimension_value(value))
                style.set_property(CSS::PropertyID::Height, parsed_value.release_nonnull());
        }
    });
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#iframe-load-event-steps
void run_iframe_load_event_steps(HTML::HTMLIFrameElement& element)
{
    // FIXME: 1. Assert: element's content navigable is not null.
    if (!element.content_navigable()) {
        // FIXME: For some reason, we sometimes end up here in the middle of SunSpider.
        dbgln("FIXME: run_iframe_load_event_steps called with null nested browsing context");
        return;
    }

    // 2. Let childDocument be element's content navigable's active document.
    [[maybe_unused]] auto child_document = element.content_navigable()->active_document();

    // FIXME: 3. If childDocument has its mute iframe load flag set, then return.

    // FIXME: 4. Set childDocument's iframe load in progress flag.

    // 5. Fire an event named load at element.
    element.dispatch_event(DOM::Event::create(element.realm(), HTML::EventNames::load));

    // FIXME: 6. Unset childDocument's iframe load in progress flag.
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-tabindex
i32 HTMLIFrameElement::default_tab_index_value() const
{
    // See the base function for the spec comments.
    return 0;
}

}
