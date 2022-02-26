/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLButtonElement.h>
#include <LibWeb/HTML/HTMLFieldSetElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLObjectElement.h>
#include <LibWeb/HTML/HTMLOutputElement.h>
#include <LibWeb/HTML/HTMLSelectElement.h>
#include <LibWeb/HTML/HTMLTextAreaElement.h>
#include <LibWeb/HTML/SubmitEvent.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/URL/URL.h>

namespace Web::HTML {

HTMLFormElement::HTMLFormElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLFormElement::~HTMLFormElement()
{
}

void HTMLFormElement::submit_form(RefPtr<HTMLElement> submitter, bool from_submit_binding)
{
    if (cannot_navigate())
        return;

    if (action().is_null()) {
        dbgln("Unsupported form action ''");
        return;
    }

    auto effective_method = method().to_lowercase();

    if (effective_method == "dialog") {
        dbgln("Failed to submit form: Unsupported form method '{}'", method());
        return;
    }

    if (effective_method != "get" && effective_method != "post") {
        effective_method = "get";
    }

    if (!from_submit_binding) {
        if (m_firing_submission_events)
            return;

        m_firing_submission_events = true;

        // FIXME: If the submitter element's no-validate state is false...

        RefPtr<HTMLElement> submitter_button;

        if (submitter != this)
            submitter_button = submitter;

        SubmitEventInit event_init {};
        event_init.submitter = submitter_button;
        auto submit_event = SubmitEvent::create(EventNames::submit, event_init);
        submit_event->set_bubbles(true);
        submit_event->set_cancelable(true);
        bool continue_ = dispatch_event(submit_event);

        m_firing_submission_events = false;

        if (!continue_)
            return;

        // This is checked again because arbitrary JS may have run when handling submit,
        // which may have changed the result.
        if (cannot_navigate())
            return;
    }

    AK::URL url(document().parse_url(action()));

    if (!url.is_valid()) {
        dbgln("Failed to submit form: Invalid URL: {}", action());
        return;
    }

    if (url.protocol() == "file") {
        if (document().url().protocol() != "file") {
            dbgln("Failed to submit form: Security violation: {} may not submit to {}", document().url(), url);
            return;
        }
        if (effective_method != "get") {
            dbgln("Failed to submit form: Unsupported form method '{}' for URL: {}", method(), url);
            return;
        }
    } else if (url.protocol() != "http" && url.protocol() != "https") {
        dbgln("Failed to submit form: Unsupported protocol for URL: {}", url);
        return;
    }

    Vector<URL::QueryParam> parameters;

    for_each_in_inclusive_subtree_of_type<HTMLInputElement>([&](auto& input) {
        if (!input.name().is_null() && (input.type() != "submit" || &input == submitter))
            parameters.append({ input.name(), input.value() });
        return IterationDecision::Continue;
    });

    if (effective_method == "get") {
        url.set_query(url_encode(parameters, AK::URL::PercentEncodeSet::ApplicationXWWWFormUrlencoded));
    }

    LoadRequest request;
    request.set_url(url);

    if (effective_method == "post") {
        auto body = url_encode(parameters, AK::URL::PercentEncodeSet::ApplicationXWWWFormUrlencoded).to_byte_buffer();
        request.set_method("POST");
        request.set_header("Content-Type", "application/x-www-form-urlencoded");
        request.set_body(body);
    }

    if (auto* page = document().page())
        page->load(request);
}

void HTMLFormElement::submit()
{
    submit_form(this, true);
}

void HTMLFormElement::add_associated_element(Badge<FormAssociatedElement>, HTMLElement& element)
{
    m_associated_elements.append(element);
}

void HTMLFormElement::remove_associated_element(Badge<FormAssociatedElement>, HTMLElement& element)
{
    m_associated_elements.remove_first_matching([&](auto& entry) { return entry.ptr() == &element; });
}

// https://html.spec.whatwg.org/#dom-fs-action
String HTMLFormElement::action() const
{
    auto value = attribute(HTML::AttributeNames::action);

    // Return the current URL if the action attribute is null or an empty string
    if (value.is_null() || value.is_empty()) {
        return document().url().to_string();
    }

    return value;
}

static bool is_form_control(DOM::Element const& element)
{
    if (is<HTMLButtonElement>(element)
        || is<HTMLFieldSetElement>(element)
        || is<HTMLObjectElement>(element)
        || is<HTMLOutputElement>(element)
        || is<HTMLSelectElement>(element)
        || is<HTMLTextAreaElement>(element)) {
        return true;
    }

    if (is<HTMLInputElement>(element)
        && !element.get_attribute(HTML::AttributeNames::type).equals_ignoring_case("image")) {
        return true;
    }

    return false;
}

// https://html.spec.whatwg.org/multipage/forms.html#dom-form-elements
NonnullRefPtr<DOM::HTMLCollection> HTMLFormElement::elements() const
{
    // FIXME: This should return the same HTMLFormControlsCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return DOM::HTMLCollection::create(const_cast<HTMLFormElement&>(*this), [](Element const& element) {
        return is_form_control(element);
    });
}

// https://html.spec.whatwg.org/multipage/forms.html#dom-form-length
unsigned HTMLFormElement::length() const
{
    // The length IDL attribute must return the number of nodes represented by the elements collection.
    return elements()->length();
}

}
