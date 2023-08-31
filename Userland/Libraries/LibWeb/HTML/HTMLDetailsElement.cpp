/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/EventLoop/TaskQueue.h>
#include <LibWeb/HTML/HTMLDetailsElement.h>
#include <LibWeb/HTML/HTMLSummaryElement.h>
#include <LibWeb/HTML/ToggleEvent.h>

namespace Web::HTML {

HTMLDetailsElement::HTMLDetailsElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDetailsElement::~HTMLDetailsElement() = default;

void HTMLDetailsElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLDetailsElementPrototype>(realm, "HTMLDetailsElement"));
}

void HTMLDetailsElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    Base::attribute_changed(name, value);

    // https://html.spec.whatwg.org/multipage/interactive-elements.html#details-notification-task-steps
    if (name == HTML::AttributeNames::open) {
        // 1. If the open attribute is added, queue a details toggle event task given the details element, "closed", and "open".
        if (!value.is_null()) {
            queue_a_details_toggle_event_task("closed"_string, "open"_string);
        }
        // 2. Otherwise, queue a details toggle event task given the details element, "open", and "closed".
        else {
            queue_a_details_toggle_event_task("open"_string, "closed"_string);
        }
    }
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#queue-a-details-toggle-event-task
void HTMLDetailsElement::queue_a_details_toggle_event_task(String old_state, String new_state)
{
    // 1. If element's details toggle task tracker is not null, then:
    if (m_details_toggle_task_tracker.has_value()) {
        // 1. Set oldState to element's details toggle task tracker's old state.
        old_state = move(m_details_toggle_task_tracker->old_state);

        // 2. Remove element's details toggle task tracker's task from its task queue.
        HTML::main_thread_event_loop().task_queue().remove_tasks_matching([&](auto const& task) {
            return task.id() == m_details_toggle_task_tracker->task_id;
        });

        // 3. Set element's details toggle task tracker to null.
        m_details_toggle_task_tracker->task_id = {};
    }

    // 2. Queue an element task given the DOM manipulation task source and element to run the following steps:
    auto task_id = queue_an_element_task(HTML::Task::Source::DOMManipulation, [this, old_state, new_state = move(new_state)]() mutable {
        // 1. Fire an event named toggle at element, using ToggleEvent, with the oldState attribute initialized to
        //    oldState and the newState attribute initialized to newState.
        ToggleEventInit event_init {};
        event_init.old_state = move(old_state);
        event_init.new_state = move(new_state);

        dispatch_event(ToggleEvent::create(realm(), HTML::EventNames::toggle, move(event_init)));

        // 2. Set element's details toggle task tracker to null.
        m_details_toggle_task_tracker = {};
    });

    // 3. Set element's details toggle task tracker to a struct with task set to the just-queued task and old state set to oldState.
    m_details_toggle_task_tracker = ToggleTaskTracker {
        .task_id = task_id,
        .old_state = move(old_state),
    };
}

}
