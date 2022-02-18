/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/Bindings/EventTargetWrapper.h>
#include <LibWeb/Bindings/EventTargetWrapperFactory.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/DOM/IDLEventListener.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web::DOM {

// FIXME: This shouldn't be here, as retargeting is not only used by the event dispatcher.
//        When moving this function, it needs to be generalized. https://dom.spec.whatwg.org/#retarget
static EventTarget* retarget(EventTarget* left, EventTarget* right)
{
    for (;;) {
        if (!is<Node>(left))
            return left;

        auto* left_node = verify_cast<Node>(left);
        auto& left_root = left_node->root();
        if (!is<ShadowRoot>(left_root))
            return left;

        if (is<Node>(right) && left_root.is_shadow_including_inclusive_ancestor_of(verify_cast<Node>(*right)))
            return left;

        auto& left_shadow_root = verify_cast<ShadowRoot>(left_root);
        left = left_shadow_root.host();
    }
}

// https://dom.spec.whatwg.org/#concept-event-listener-inner-invoke
bool EventDispatcher::inner_invoke(Event& event, Vector<NonnullRefPtr<DOM::DOMEventListener>>& listeners, Event::Phase phase, bool invocation_target_in_shadow_tree)
{
    // 1. Let found be false.
    bool found = false;

    // 2. For each listener in listeners, whose removed is false:
    for (auto& listener : listeners) {
        if (listener->removed)
            continue;

        // 1. If event’s type attribute value is not listener’s type, then continue.
        if (event.type() != listener->type)
            continue;

        // 2. Set found to true.
        found = true;

        // 3. If phase is "capturing" and listener’s capture is false, then continue.
        if (phase == Event::Phase::CapturingPhase && !listener->capture)
            continue;

        // 4. If phase is "bubbling" and listener’s capture is true, then continue.
        if (phase == Event::Phase::BubblingPhase && listener->capture)
            continue;

        // 5. If listener’s once is true, then remove listener from event’s currentTarget attribute value’s event listener list.
        if (listener->once)
            event.current_target()->remove_from_event_listener_list(listener);

        // 6. Let global be listener callback’s associated Realm’s global object.
        auto& callback = listener->callback->callback();
        auto& global = callback.callback.cell()->global_object();

        // 7. Let currentEvent be undefined.
        RefPtr<Event> current_event;

        // 8. If global is a Window object, then:
        if (is<Bindings::WindowObject>(global)) {
            auto& bindings_window_global = verify_cast<Bindings::WindowObject>(global);
            auto& window_impl = bindings_window_global.impl();

            // 1. Set currentEvent to global’s current event.
            current_event = window_impl.current_event();

            // 2. If invocationTargetInShadowTree is false, then set global’s current event to event.
            if (!invocation_target_in_shadow_tree)
                window_impl.set_current_event(&event);
        }

        // 9. If listener’s passive is true, then set event’s in passive listener flag.
        if (listener->passive)
            event.set_in_passive_listener(true);

        // 10. Call a user object’s operation with listener’s callback, "handleEvent", « event », and event’s currentTarget attribute value. If this throws an exception, then:
        // FIXME: These should be wrapped for us in call_user_object_operation, but it currently doesn't do that.
        auto* this_value = Bindings::wrap(global, *event.current_target());
        auto* wrapped_event = Bindings::wrap(global, event);
        auto result = Bindings::IDL::call_user_object_operation(callback, "handleEvent", this_value, wrapped_event);

        // If this throws an exception, then:
        if (result.is_error()) {
            // 1. Report the exception.
            HTML::report_exception(result);

            // FIXME: 2. Set legacyOutputDidListenersThrowFlag if given. (Only used by IndexedDB currently)
        }

        // 11. Unset event’s in passive listener flag.
        event.set_in_passive_listener(false);

        // 12. If global is a Window object, then set global’s current event to currentEvent.
        if (is<Bindings::WindowObject>(global)) {
            auto& bindings_window_global = verify_cast<Bindings::WindowObject>(global);
            auto& window_impl = bindings_window_global.impl();
            window_impl.set_current_event(current_event);
        }

        // 13. If event’s stop immediate propagation flag is set, then return found.
        if (event.should_stop_immediate_propagation())
            return found;
    }

    // 3. Return found.
    return found;
}

// https://dom.spec.whatwg.org/#concept-event-listener-invoke
void EventDispatcher::invoke(Event::PathEntry& struct_, Event& event, Event::Phase phase)
{
    auto last_valid_shadow_adjusted_target = event.path().last_matching([&struct_](auto& entry) {
        return entry.index <= struct_.index && !entry.shadow_adjusted_target.is_null();
    });

    VERIFY(last_valid_shadow_adjusted_target.has_value());

    event.set_target(last_valid_shadow_adjusted_target.value().shadow_adjusted_target);
    event.set_related_target(struct_.related_target);
    event.set_touch_target_list(struct_.touch_target_list);

    if (event.should_stop_propagation())
        return;

    event.set_current_target(struct_.invocation_target);

    // NOTE: This is an intentional copy. Any event listeners added after this point will not be invoked.
    auto listeners = event.current_target()->event_listener_list();
    bool invocation_target_in_shadow_tree = struct_.invocation_target_in_shadow_tree;

    bool found = inner_invoke(event, listeners, phase, invocation_target_in_shadow_tree);

    if (!found && event.is_trusted()) {
        auto original_event_type = event.type();

        if (event.type() == "animationend")
            event.set_type("webkitAnimationEnd");
        else if (event.type() == "animationiteration")
            event.set_type("webkitAnimationIteration");
        else if (event.type() == "animationstart")
            event.set_type("webkitAnimationStart");
        else if (event.type() == "transitionend")
            event.set_type("webkitTransitionEnd");
        else
            return;

        inner_invoke(event, listeners, phase, invocation_target_in_shadow_tree);
        event.set_type(original_event_type);
    }
}

// https://dom.spec.whatwg.org/#concept-event-dispatch
bool EventDispatcher::dispatch(NonnullRefPtr<EventTarget> target, NonnullRefPtr<Event> event, bool legacy_target_override)
{
    event->set_dispatched(true);
    RefPtr<EventTarget> target_override;

    if (!legacy_target_override) {
        target_override = target;
    } else {
        // NOTE: This can be done because legacy_target_override is only set for events targeted at Window.
        target_override = verify_cast<Window>(*target).associated_document();
    }

    RefPtr<EventTarget> activation_target;
    RefPtr<EventTarget> related_target = retarget(event->related_target(), target);

    bool clear_targets = false;

    if (related_target != target || event->related_target() == target) {
        Event::TouchTargetList touch_targets;

        for (auto& touch_target : event->touch_target_list()) {
            touch_targets.append(retarget(touch_target, target));
        }

        event->append_to_path(*target, target_override, related_target, touch_targets, false);

        bool is_activation_event = is<UIEvents::MouseEvent>(*event) && event->type() == HTML::EventNames::click;

        if (is_activation_event && target->activation_behavior)
            activation_target = target;

        // FIXME: Let slottable be target, if target is a slottable and is assigned, and null otherwise.

        bool slot_in_closed_tree = false;
        auto* parent = target->get_parent(event);

        while (parent) {
            // FIXME: If slottable is non-null:

            // FIXME: If parent is a slottable and is assigned, then set slottable to parent.

            related_target = retarget(event->related_target(), parent);
            touch_targets.clear();

            for (auto& touch_target : event->touch_target_list()) {
                touch_targets.append(retarget(touch_target, parent));
            }

            if (is<Window>(parent)
                || (is<Node>(parent) && verify_cast<Node>(*target).root().is_shadow_including_inclusive_ancestor_of(verify_cast<Node>(*parent)))) {
                if (is_activation_event && event->bubbles() && !activation_target && parent->activation_behavior)
                    activation_target = parent;

                event->append_to_path(*parent, nullptr, related_target, touch_targets, slot_in_closed_tree);
            } else if (related_target == parent) {
                parent = nullptr;
            } else {
                target = *parent;

                if (is_activation_event && !activation_target && target->activation_behavior)
                    activation_target = target;

                event->append_to_path(*parent, target, related_target, touch_targets, slot_in_closed_tree);
            }

            if (parent) {
                parent = parent->get_parent(event);
            }

            slot_in_closed_tree = false;
        }

        auto clear_targets_struct = event->path().last_matching([](auto& entry) {
            return !entry.shadow_adjusted_target.is_null();
        });

        VERIFY(clear_targets_struct.has_value());

        if (is<Node>(clear_targets_struct.value().shadow_adjusted_target.ptr())) {
            auto& shadow_adjusted_target_node = verify_cast<Node>(*clear_targets_struct.value().shadow_adjusted_target);
            if (is<ShadowRoot>(shadow_adjusted_target_node.root()))
                clear_targets = true;
        }

        if (!clear_targets && is<Node>(clear_targets_struct.value().related_target.ptr())) {
            auto& related_target_node = verify_cast<Node>(*clear_targets_struct.value().related_target);
            if (is<ShadowRoot>(related_target_node.root()))
                clear_targets = true;
        }

        if (!clear_targets) {
            for (auto touch_target : clear_targets_struct.value().touch_target_list) {
                if (is<Node>(*touch_target.ptr())) {
                    auto& touch_target_node = verify_cast<Node>(*touch_target.ptr());
                    if (is<ShadowRoot>(touch_target_node.root())) {
                        clear_targets = true;
                        break;
                    }
                }
            }
        }

        if (activation_target)
            activation_target->legacy_pre_activation_behavior();

        for (ssize_t i = event->path().size() - 1; i >= 0; --i) {
            auto& entry = event->path().at(i);

            if (entry.shadow_adjusted_target)
                event->set_phase(Event::Phase::AtTarget);
            else
                event->set_phase(Event::Phase::CapturingPhase);

            invoke(entry, event, Event::Phase::CapturingPhase);
        }

        for (auto& entry : event->path()) {
            if (entry.shadow_adjusted_target) {
                event->set_phase(Event::Phase::AtTarget);
            } else {
                if (!event->bubbles())
                    continue;

                event->set_phase(Event::Phase::BubblingPhase);
            }

            invoke(entry, event, Event::Phase::BubblingPhase);
        }
    }

    event->set_phase(Event::Phase::None);
    event->set_current_target(nullptr);
    event->clear_path();
    event->set_dispatched(false);
    event->set_stop_propagation(false);
    event->set_stop_immediate_propagation(false);

    if (clear_targets) {
        event->set_target(nullptr);
        event->set_related_target(nullptr);
        event->clear_touch_target_list();
    }

    if (activation_target) {
        if (!event->cancelled()) {
            // NOTE: Since activation_target is set, it will have activation behavior.
            activation_target->activation_behavior(event);
        } else {
            activation_target->legacy_cancelled_activation_behavior();
        }
    }

    return !event->cancelled();
}

}
