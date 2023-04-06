/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <AK/Vector.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HTML/Focus.h>
#include <LibWeb/UIEvents/FocusEvent.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/interaction.html#focus-update-steps
static void run_focus_update_steps(Vector<JS::Handle<DOM::Node>> old_chain, Vector<JS::Handle<DOM::Node>> new_chain, DOM::Node* new_focus_target)
{
    // 1. If the last entry in old chain and the last entry in new chain are the same,
    //    pop the last entry from old chain and the last entry from new chain and redo this step.
    while (!old_chain.is_empty()
        && !new_chain.is_empty()
        && &old_chain.last() == &new_chain.last()) {
        (void)old_chain.take_last();
        (void)new_chain.take_last();
    }

    // 2. For each entry entry in old chain, in order, run these substeps:
    for (auto& entry : old_chain) {
        // FIXME: 1. If entry is an input element, and the change event applies to the element,
        //           and the element does not have a defined activation behavior,
        //           and the user has changed the element's value or its list of selected files
        //           while the control was focused without committing that change
        //           (such that it is different to what it was when the control was first focused),
        //           then fire an event named change at the element,
        //           with the bubbles attribute initialized to true.

        JS::GCPtr<DOM::EventTarget> blur_event_target;
        if (is<DOM::Element>(*entry)) {
            // 2. If entry is an element, let blur event target be entry.
            blur_event_target = entry.ptr();
        } else if (is<DOM::Document>(*entry)) {
            // If entry is a Document object, let blur event target be that Document object's relevant global object.
            blur_event_target = &static_cast<DOM::Document&>(*entry).window();
        }

        // 3. If entry is the last entry in old chain, and entry is an Element,
        //    and the last entry in new chain is also an Element,
        //    then let related blur target be the last entry in new chain.
        //    Otherwise, let related blur target be null.
        JS::GCPtr<DOM::EventTarget> related_blur_target;
        if (!old_chain.is_empty()
            && &entry == &old_chain.last()
            && is<DOM::Element>(*entry)
            && !new_chain.is_empty()
            && is<DOM::Element>(*new_chain.last())) {
            related_blur_target = new_chain.last().ptr();
        }

        // 4. If blur event target is not null, fire a focus event named blur at blur event target,
        //    with related blur target as the related target.
        if (blur_event_target) {
            // FIXME: Implement the "fire a focus event" spec operation.
            auto blur_event = UIEvents::FocusEvent::create(blur_event_target->realm(), HTML::EventNames::blur).release_value_but_fixme_should_propagate_errors();
            blur_event->set_related_target(related_blur_target);
            blur_event_target->dispatch_event(blur_event);
        }
    }

    // FIXME: 3. Apply any relevant platform-specific conventions for focusing new focus target.
    //           (For example, some platforms select the contents of a text control when that control is focused.)
    (void)new_focus_target;

    // 4. For each entry entry in new chain, in reverse order, run these substeps:
    for (auto& entry : new_chain.in_reverse()) {
        // 1. If entry is a focusable area: designate entry as the focused area of the document.
        // FIXME: This isn't entirely right.
        if (is<DOM::Element>(*entry))
            entry->document().set_focused_element(&static_cast<DOM::Element&>(*entry));

        JS::GCPtr<DOM::EventTarget> focus_event_target;
        if (is<DOM::Element>(*entry)) {
            // 2. If entry is an element, let focus event target be entry.
            focus_event_target = entry.ptr();
        } else if (is<DOM::Document>(*entry)) {
            // If entry is a Document object, let focus event target be that Document object's relevant global object.
            focus_event_target = &static_cast<DOM::Document&>(*entry).window();
        }

        // 3. If entry is the last entry in new chain, and entry is an Element,
        //    and the last entry in old chain is also an Element,
        //    then let related focus target be the last entry in old chain.
        //    Otherwise, let related focus target be null.
        JS::GCPtr<DOM::EventTarget> related_focus_target;
        if (!new_chain.is_empty()
            && &entry == &new_chain.last()
            && is<DOM::Element>(*entry)
            && !old_chain.is_empty()
            && is<DOM::Element>(*old_chain.last())) {
            related_focus_target = old_chain.last().ptr();
        }

        // 4. If focus event target is not null, fire a focus event named focus at focus event target,
        //    with related focus target as the related target.
        if (focus_event_target) {
            // FIXME: Implement the "fire a focus event" spec operation.
            auto focus_event = UIEvents::FocusEvent::create(focus_event_target->realm(), HTML::EventNames::focus).release_value_but_fixme_should_propagate_errors();
            focus_event->set_related_target(related_focus_target);
            focus_event_target->dispatch_event(focus_event);
        }
    }
}

// https://html.spec.whatwg.org/multipage/interaction.html#focus-chain
static Vector<JS::Handle<DOM::Node>> focus_chain(DOM::Node* subject)
{
    // FIXME: Move this somewhere more spec-friendly.
    if (!subject)
        return {};

    // 1. Let output be an empty list.
    Vector<JS::Handle<DOM::Node>> output;

    // 2. Let currentObject be subject.
    auto* current_object = subject;

    // 3. While true:
    while (true) {
        // 1. Append currentObject to output.
        output.append(JS::make_handle(*current_object));

        // FIXME: 2. If currentObject is an area element's shape, then append that area element to output.

        // FIXME:    Otherwise, if currentObject's DOM anchor is an element that is not currentObject itself, then append currentObject's DOM anchor to output.

        // FIXME: Everything below needs work. The conditions are not entirely right.
        if (!is<DOM::Document>(*current_object)) {
            // 3. If currentObject is a focusable area, then set currentObject to currentObject's DOM anchor's node document.
            current_object = &current_object->document();
        } else if (is<DOM::Document>(*current_object)
            && static_cast<DOM::Document&>(*current_object).browsing_context()
            && !static_cast<DOM::Document&>(*current_object).browsing_context()->is_top_level()) {
            // Otherwise, if currentObject is a Document whose browsing context is a child browsing context,
            // then set currentObject to currentObject's browsing context's container.
            current_object = static_cast<DOM::Document&>(*current_object).browsing_context()->container();
        } else {
            break;
        }
    }

    // 4. Return output.
    return output;
}

// https://html.spec.whatwg.org/multipage/interaction.html#focusing-steps
// FIXME: This should accept more types.
void run_focusing_steps(DOM::Node* new_focus_target, DOM::Node* fallback_target, [[maybe_unused]] Optional<DeprecatedString> focus_trigger)
{
    // FIXME: 1. If new focus target is not a focusable area, then set new focus target
    //           to the result of getting the focusable area for new focus target,
    //           given focus trigger if it was passed.

    // 2. If new focus target is null, then:
    if (!new_focus_target) {
        // 1. If no fallback target was specified, then return.
        if (!fallback_target)
            return;

        // 2. Otherwise, set new focus target to the fallback target.
        new_focus_target = fallback_target;
    }

    // 3. If new focus target is a browsing context container with non-null nested browsing context,
    //    then set new focus target to the nested browsing context's active document.
    if (is<HTML::BrowsingContextContainer>(*new_focus_target)) {
        auto& browsing_context_container = static_cast<HTML::BrowsingContextContainer&>(*new_focus_target);
        if (auto* nested_browsing_context = browsing_context_container.nested_browsing_context())
            new_focus_target = nested_browsing_context->active_document();
    }

    // FIXME: 4. If new focus target is a focusable area and its DOM anchor is inert, then return.

    // 5. If new focus target is the currently focused area of a top-level browsing context, then return.
    if (!new_focus_target->document().browsing_context())
        return;
    auto& top_level_browsing_context = new_focus_target->document().browsing_context()->top_level_browsing_context();
    if (new_focus_target == top_level_browsing_context.currently_focused_area().ptr())
        return;

    // 6. Let old chain be the current focus chain of the top-level browsing context in which
    //    new focus target finds itself.
    auto old_chain = focus_chain(top_level_browsing_context.currently_focused_area());

    // 7. Let new chain be the focus chain of new focus target.
    auto new_chain = focus_chain(new_focus_target);

    // 8. Run the focus update steps with old chain, new chain, and new focus target respectively.
    run_focus_update_steps(old_chain, new_chain, new_focus_target);
}

void run_unfocusing_steps(DOM::Node* old_focus_target)
{
    // NOTE: The unfocusing steps do not always result in the focus changing, even when applied to the currently focused
    // area of a top-level browsing context. For example, if the currently focused area of a top-level browsing context
    // is a viewport, then it will usually keep its focus regardless until another focusable area is explicitly focused
    // with the focusing steps.

    auto is_shadow_host = [](DOM::Node* node) {
        return is<DOM::Element>(node) && static_cast<DOM::Element*>(node)->is_shadow_host();
    };

    // 1. If old focus target is a shadow host whose shadow root's delegates focus is true, and old focus target's
    //    shadow root is a shadow-including inclusive ancestor of the currently focused area of a top-level browsing
    //    context's DOM anchor, then set old focus target to that currently focused area of a top-level browsing
    //    context.
    if (is_shadow_host(old_focus_target)) {
        auto* shadow_root = static_cast<DOM::Element*>(old_focus_target)->shadow_root_internal();
        if (shadow_root->delegates_focus()) {
            auto& top_level_browsing_context = old_focus_target->document().browsing_context()->top_level_browsing_context();
            if (auto currently_focused_area = top_level_browsing_context.currently_focused_area()) {
                if (shadow_root->is_shadow_including_ancestor_of(*currently_focused_area)) {
                    old_focus_target = currently_focused_area;
                }
            }
        }
    }

    // FIXME: 2. If old focus target is inert, then return.

    // FIXME: 3. If old focus target is an area element and one of its shapes is the currently focused area of a
    //    top-level browsing context, or, if old focus target is an element with one or more scrollable regions, and one
    //    of them is the currently focused area of a top-level browsing context, then let old focus target be that
    //    currently focused area of a top-level browsing context.

    // NOTE: HTMLAreaElement is currently missing the shapes property

    auto& top_level_browsing_context = old_focus_target->document().browsing_context()->top_level_browsing_context();

    // 4. Let old chain be the current focus chain of the top-level browsing context in which old focus target finds itself.
    auto old_chain = focus_chain(top_level_browsing_context.currently_focused_area());

    // 5. If old focus target is not one of the entries in old chain, then return.
    for (auto& node : old_chain) {
        if (old_focus_target != node) {
            return;
        }
    }

    // 6. If old focus target is not a focusable area, then return.
    if (!old_focus_target->is_focusable())
        return;

    // 7. Let topDocument be old chain's last entry.
    auto* top_document = verify_cast<DOM::Document>(old_chain.last().ptr());

    // 8. If topDocument's browsing context has system focus, then run the focusing steps for topDocument's viewport.
    if (top_document->browsing_context()->system_visibility_state() == HTML::VisibilityState::Visible) {
        // FIXME: run the focusing steps for topDocument's viewport (??)
    } else {
        // FIXME: Otherwise, apply any relevant platform-specific conventions for removing system focus from
        // topDocument's browsing context, and run the focus update steps with old chain, an empty list, and null
        // respectively.

        // What? It already doesn't have system focus, what possible platform-specific conventions are there?

        run_focus_update_steps(old_chain, {}, nullptr);
    }

    // FIXME: When the currently focused area of a top-level browsing context is somehow unfocused without another
    // element being explicitly focused in its stead, the user agent must immediately run the unfocusing steps for that
    // object.

    // What? How are we supposed to detect when something is "somehow unfocused without another element being explicitly focused"?
}

}
