/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
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

HTMLFormElement::~HTMLFormElement() = default;

JS::ThrowCompletionOr<void> HTMLFormElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLFormElementPrototype>(realm, "HTMLFormElement"));

    return {};
}

void HTMLFormElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_elements);
    for (auto& element : m_associated_elements)
        visitor.visit(element.ptr());
}

ErrorOr<void> HTMLFormElement::submit_form(JS::GCPtr<HTMLElement> submitter, bool from_submit_binding)
{
    if (cannot_navigate())
        return {};

    if (action().is_null()) {
        dbgln("Unsupported form action ''");
        return {};
    }

    auto effective_method = method().to_lowercase();

    if (effective_method == "dialog") {
        dbgln("Failed to submit form: Unsupported form method '{}'", method());
        return {};
    }

    if (effective_method != "get" && effective_method != "post") {
        effective_method = "get";
    }

    if (!from_submit_binding) {
        if (m_firing_submission_events)
            return {};

        m_firing_submission_events = true;

        // FIXME: If the submitter element's no-validate state is false...

        JS::GCPtr<HTMLElement> submitter_button;

        if (submitter != this)
            submitter_button = submitter;

        SubmitEventInit event_init {};
        event_init.submitter = submitter_button;
        auto submit_event = SubmitEvent::create(realm(), String::from_deprecated_string(EventNames::submit).release_value_but_fixme_should_propagate_errors(), event_init).release_value_but_fixme_should_propagate_errors();
        submit_event->set_bubbles(true);
        submit_event->set_cancelable(true);
        bool continue_ = dispatch_event(*submit_event);

        m_firing_submission_events = false;

        if (!continue_)
            return {};

        // This is checked again because arbitrary JS may have run when handling submit,
        // which may have changed the result.
        if (cannot_navigate())
            return {};
    }

    AK::URL url(document().parse_url(action()));

    if (!url.is_valid()) {
        dbgln("Failed to submit form: Invalid URL: {}", action());
        return {};
    }

    if (url.scheme() == "file") {
        if (document().url().scheme() != "file") {
            dbgln("Failed to submit form: Security violation: {} may not submit to {}", document().url(), url);
            return {};
        }
        if (effective_method != "get") {
            dbgln("Failed to submit form: Unsupported form method '{}' for URL: {}", method(), url);
            return {};
        }
    } else if (url.scheme() != "http" && url.scheme() != "https") {
        dbgln("Failed to submit form: Unsupported protocol for URL: {}", url);
        return {};
    }

    Vector<URL::QueryParam> parameters;

    for_each_in_inclusive_subtree_of_type<HTMLInputElement>([&](auto& input) {
        if (!input.name().is_null() && (input.type() != "submit" || &input == submitter)) {
            auto name = String::from_deprecated_string(input.name()).release_value_but_fixme_should_propagate_errors();
            auto value = String::from_deprecated_string(input.value()).release_value_but_fixme_should_propagate_errors();
            parameters.append({ move(name), move(value) });
        }
        return IterationDecision::Continue;
    });

    if (effective_method == "get") {
        auto url_encoded_parameters = TRY(url_encode(parameters, AK::URL::PercentEncodeSet::ApplicationXWWWFormUrlencoded)).to_deprecated_string();
        url.set_query(move(url_encoded_parameters));
    }

    LoadRequest request = LoadRequest::create_for_url_on_page(url, document().page());

    if (effective_method == "post") {
        auto url_encoded_parameters_as_bytes = TRY(url_encode(parameters, AK::URL::PercentEncodeSet::ApplicationXWWWFormUrlencoded)).bytes();
        auto body = TRY(ByteBuffer::copy(url_encoded_parameters_as_bytes));
        request.set_method("POST");
        request.set_header("Content-Type", "application/x-www-form-urlencoded");
        request.set_body(move(body));
    }

    if (auto* page = document().page())
        page->load(request);

    return {};
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#resetting-a-form
void HTMLFormElement::reset_form()
{
    // 1. Let reset be the result of firing an event named reset at form, with the bubbles and cancelable attributes initialized to true.
    auto reset_event = DOM::Event::create(realm(), HTML::EventNames::reset).release_value_but_fixme_should_propagate_errors();
    reset_event->set_bubbles(true);
    reset_event->set_cancelable(true);

    bool reset = dispatch_event(reset_event);

    // 2. If reset is true, then invoke the reset algorithm of each resettable element whose form owner is form.
    if (reset) {
        for (auto element : m_associated_elements) {
            VERIFY(is<FormAssociatedElement>(*element));
            auto& form_associated_element = dynamic_cast<FormAssociatedElement&>(*element);
            if (form_associated_element.is_resettable())
                form_associated_element.reset_algorithm();
        }
    }
}

WebIDL::ExceptionOr<void> HTMLFormElement::submit()
{
    auto& vm = realm().vm();

    TRY_OR_THROW_OOM(vm, submit_form(this, true));
    return {};
}

// https://html.spec.whatwg.org/multipage/forms.html#dom-form-reset
void HTMLFormElement::reset()
{
    // 1. If the form element is marked as locked for reset, then return.
    if (m_locked_for_reset)
        return;

    // 2. Mark the form element as locked for reset.
    m_locked_for_reset = true;

    // 3. Reset the form element.
    reset_form();

    // 4. Unmark the form element as locked for reset.
    m_locked_for_reset = false;
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
DeprecatedString HTMLFormElement::action() const
{
    auto value = attribute(HTML::AttributeNames::action);

    // Return the current URL if the action attribute is null or an empty string
    if (value.is_null() || value.is_empty()) {
        return document().url().to_deprecated_string();
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
        && !element.get_attribute(HTML::AttributeNames::type).equals_ignoring_case("image"sv)) {
        return true;
    }

    return false;
}

// https://html.spec.whatwg.org/multipage/forms.html#dom-form-elements
JS::NonnullGCPtr<DOM::HTMLCollection> HTMLFormElement::elements() const
{
    if (!m_elements) {
        m_elements = DOM::HTMLCollection::create(const_cast<HTMLFormElement&>(*this), [](Element const& element) {
            return is_form_control(element);
        }).release_value_but_fixme_should_propagate_errors();
    }
    return *m_elements;
}

// https://html.spec.whatwg.org/multipage/forms.html#dom-form-length
unsigned HTMLFormElement::length() const
{
    // The length IDL attribute must return the number of nodes represented by the elements collection.
    return elements()->length();
}

// https://html.spec.whatwg.org/multipage/forms.html#category-submit
ErrorOr<Vector<JS::NonnullGCPtr<DOM::Element>>> HTMLFormElement::get_submittable_elements()
{
    Vector<JS::NonnullGCPtr<DOM::Element>> submittable_elements = {};
    for (size_t i = 0; i < elements()->length(); i++) {
        auto* element = elements()->item(i);
        TRY(populate_vector_with_submittable_elements_in_tree_order(*element, submittable_elements));
    }
    return submittable_elements;
}

ErrorOr<void> HTMLFormElement::populate_vector_with_submittable_elements_in_tree_order(JS::NonnullGCPtr<DOM::Element> element, Vector<JS::NonnullGCPtr<DOM::Element>>& elements)
{
    if (auto* form_associated_element = dynamic_cast<HTML::FormAssociatedElement*>(element.ptr())) {
        if (form_associated_element->is_submittable())
            TRY(elements.try_append(element));
    }

    for (size_t i = 0; i < element->children()->length(); i++) {
        auto* child = element->children()->item(i);
        TRY(populate_vector_with_submittable_elements_in_tree_order(*child, elements));
    }

    return {};
}

}
