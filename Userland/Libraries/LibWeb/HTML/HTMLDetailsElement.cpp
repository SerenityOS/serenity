/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/HTMLDetailsElement.h>
#include <LibWeb/HTML/HTMLSummaryElement.h>

namespace Web::HTML {

HTMLDetailsElement::HTMLDetailsElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDetailsElement::~HTMLDetailsElement() = default;

WebIDL::ExceptionOr<void> HTMLDetailsElement::set_attribute(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    auto result = HTMLElement::set_attribute(name, value);
    if (result.is_exception())
        return result.exception();

    if (name == HTML::AttributeNames::open)
        run_details_notification_task_steps();

    return result;
}

void HTMLDetailsElement::remove_attribute(DeprecatedFlyString const& name)
{
    HTMLElement::remove_attribute(name);

    if (name == HTML::AttributeNames::open)
        run_details_notification_task_steps();
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#the-details-element:details-notification-task-steps
void HTMLDetailsElement::run_details_notification_task_steps()
{
    // Whenever the open attribute is added to or removed from a details element,
    // the user agent must queue an element task on the DOM manipulation task source given then details element that runs the following steps,
    // which are known as the details notification task steps, for this details element:
    queue_an_element_task(HTML::Task::Source::DOMManipulation, [this] {
        // 1. FIXME: If another task has been queued to run the details notification task steps for this details element, then return.

        // 2. Fire an event named toggle at the details element.
        dispatch_event(Web::DOM::Event::create(realm(), HTML::EventNames::toggle));
    });
}

void HTMLDetailsElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLDetailsElementPrototype>(realm, "HTMLDetailsElement"));
}

}
