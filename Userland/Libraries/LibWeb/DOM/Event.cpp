/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibWeb/Bindings/EventPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>

namespace Web::DOM {

JS_DEFINE_ALLOCATOR(Event);

JS::NonnullGCPtr<Event> Event::create(JS::Realm& realm, FlyString const& event_name, EventInit const& event_init)
{
    return realm.heap().allocate<Event>(realm, realm, event_name, event_init);
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<Event>> Event::construct_impl(JS::Realm& realm, FlyString const& event_name, EventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

// https://dom.spec.whatwg.org/#inner-event-creation-steps
Event::Event(JS::Realm& realm, FlyString const& type)
    : PlatformObject(realm)
    , m_type(type)
    , m_initialized(true)
    , m_time_stamp(HighResolutionTime::current_high_resolution_time(HTML::relevant_global_object(*this)))
{
}

// https://dom.spec.whatwg.org/#inner-event-creation-steps
Event::Event(JS::Realm& realm, FlyString const& type, EventInit const& event_init)
    : PlatformObject(realm)
    , m_type(type)
    , m_bubbles(event_init.bubbles)
    , m_cancelable(event_init.cancelable)
    , m_composed(event_init.composed)
    , m_initialized(true)
    , m_time_stamp(HighResolutionTime::current_high_resolution_time(HTML::relevant_global_object(*this)))
{
}

void Event::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(Event);
}

void Event::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_target);
    visitor.visit(m_related_target);
    visitor.visit(m_current_target);
    for (auto& it : m_path) {
        visitor.visit(it.invocation_target);
        visitor.visit(it.shadow_adjusted_target);
        visitor.visit(it.related_target);
        visitor.visit(it.touch_target_list);
    }
    visitor.visit(m_touch_target_list);
}

// https://dom.spec.whatwg.org/#concept-event-path-append
void Event::append_to_path(EventTarget& invocation_target, JS::GCPtr<EventTarget> shadow_adjusted_target, JS::GCPtr<EventTarget> related_target, TouchTargetList& touch_targets, bool slot_in_closed_tree)
{
    // 1. Let invocationTargetInShadowTree be false.
    bool invocation_target_in_shadow_tree = false;

    // 3. Let root-of-closed-tree be false.
    bool root_of_closed_tree = false;

    // 2. If invocationTarget is a node and its root is a shadow root, then set invocationTargetInShadowTree to true.
    if (is<Node>(invocation_target)) {
        auto& invocation_target_node = verify_cast<Node>(invocation_target);
        if (is<ShadowRoot>(invocation_target_node.root()))
            invocation_target_in_shadow_tree = true;
        if (is<ShadowRoot>(invocation_target_node)) {
            auto& invocation_target_shadow_root = verify_cast<ShadowRoot>(invocation_target_node);
            // 4. If invocationTarget is a shadow root whose mode is "closed", then set root-of-closed-tree to true.
            root_of_closed_tree = invocation_target_shadow_root.mode() == Bindings::ShadowRootMode::Closed;
        }
    }

    // 5. Append a new struct to event’s path whose invocation target is invocationTarget, invocation-target-in-shadow-tree is invocationTargetInShadowTree,
    // shadow-adjusted target is shadowAdjustedTarget, relatedTarget is relatedTarget, touch target list is touchTargets, root-of-closed-tree is root-of-closed-tree,
    // and slot-in-closed-tree is slot-in-closed-tree.
    m_path.append({ invocation_target, invocation_target_in_shadow_tree, shadow_adjusted_target, related_target, touch_targets, root_of_closed_tree, slot_in_closed_tree, m_path.size() });
}

void Event::set_cancelled_flag()
{
    if (m_cancelable && !m_in_passive_listener)
        m_cancelled = true;
}

// https://dom.spec.whatwg.org/#concept-event-initialize
void Event::initialize_event(String const& type, bool bubbles, bool cancelable)
{
    // 1. Set event’s initialized flag.
    m_initialized = true;

    // 2. Unset event’s stop propagation flag, stop immediate propagation flag, and canceled flag.
    m_stop_propagation = false;
    m_stop_immediate_propagation = false;
    m_cancelled = false;

    // 3. Set event’s isTrusted attribute to false.
    m_is_trusted = false;

    // 4. Set event’s target to null.
    m_target = nullptr;

    // 5. Set event’s type attribute to type.
    m_type = type;

    // 6. Set event’s bubbles attribute to bubbles.
    m_bubbles = bubbles;

    // 8. Set event’s cancelable attribute to cancelable.
    m_cancelable = cancelable;
}

// https://dom.spec.whatwg.org/#dom-event-initevent
void Event::init_event(String const& type, bool bubbles, bool cancelable)
{
    // 1. If this’s dispatch flag is set, then return.
    if (m_dispatch)
        return;

    // 2. Initialize this with type, bubbles, and cancelable.
    initialize_event(type, bubbles, cancelable);
}

// https://dom.spec.whatwg.org/#dom-event-timestamp
double Event::time_stamp() const
{
    return m_time_stamp;
}

// https://dom.spec.whatwg.org/#dom-event-composedpath
Vector<JS::Handle<EventTarget>> Event::composed_path() const
{
    // 1. Let composedPath be an empty list.
    Vector<JS::Handle<EventTarget>> composed_path;

    // 2. Let path be this’s path. (NOTE: Not necessary)

    // 3. If path is empty, then return composedPath.
    if (m_path.is_empty())
        return composed_path;

    // 4. Let currentTarget be this’s currentTarget attribute value. (NOTE: Not necessary)

    // 5. Append currentTarget to composedPath.
    // NOTE: If path is not empty, then the event is being dispatched and will have a currentTarget.
    VERIFY(m_current_target);
    composed_path.append(const_cast<EventTarget*>(m_current_target.ptr()));

    // 6. Let currentTargetIndex be 0.
    size_t current_target_index = 0;

    // 7. Let currentTargetHiddenSubtreeLevel be 0.
    size_t current_target_hidden_subtree_level = 0;

    // 8. Let index be path’s size − 1.
    // 9. While index is greater than or equal to 0:
    for (ssize_t index = m_path.size() - 1; index >= 0; --index) {
        auto& path_entry = m_path.at(index);

        // 1. If path[index]'s root-of-closed-tree is true, then increase currentTargetHiddenSubtreeLevel by 1.
        if (path_entry.root_of_closed_tree)
            ++current_target_hidden_subtree_level;

        // 2. If path[index]'s invocation target is currentTarget, then set currentTargetIndex to index and break.
        if (path_entry.invocation_target == m_current_target) {
            current_target_index = index;
            break;
        }

        // 3. If path[index]'s slot-in-closed-tree is true, then decrease currentTargetHiddenSubtreeLevel by 1.
        if (path_entry.slot_in_closed_tree)
            --current_target_hidden_subtree_level;

        // 4. Decrease index by 1.
    }

    // 10. Let currentHiddenLevel and maxHiddenLevel be currentTargetHiddenSubtreeLevel.
    size_t current_hidden_level = current_target_hidden_subtree_level;
    size_t max_hidden_level = current_target_hidden_subtree_level;

    // 11. Set index to currentTargetIndex − 1.
    // 12. While index is greater than or equal to 0:
    for (ssize_t index = current_target_index - 1; index >= 0; --index) {
        auto& path_entry = m_path.at(index);

        // 1. If path[index]'s root-of-closed-tree is true, then increase currentHiddenLevel by 1.
        if (path_entry.root_of_closed_tree)
            ++current_hidden_level;

        // 2. If currentHiddenLevel is less than or equal to maxHiddenLevel, then prepend path[index]'s invocation target to composedPath.
        if (current_hidden_level <= max_hidden_level) {
            VERIFY(path_entry.invocation_target);
            composed_path.prepend(const_cast<EventTarget*>(path_entry.invocation_target.ptr()));
        }

        // 3. If path[index]'s slot-in-closed-tree is true, then:
        if (path_entry.slot_in_closed_tree) {
            // 1. Decrease currentHiddenLevel by 1.
            --current_hidden_level;

            // 2. If currentHiddenLevel is less than maxHiddenLevel, then set maxHiddenLevel to currentHiddenLevel.
            if (current_hidden_level < max_hidden_level)
                max_hidden_level = current_hidden_level;
        }

        // 4. Decrease index by 1.
    }

    // 13. Set currentHiddenLevel and maxHiddenLevel to currentTargetHiddenSubtreeLevel.
    current_hidden_level = current_target_hidden_subtree_level;
    max_hidden_level = current_target_hidden_subtree_level;

    // 14. Set index to currentTargetIndex + 1.
    // 15. While index is less than path’s size:
    for (size_t index = current_target_index + 1; index < m_path.size(); ++index) {
        auto& path_entry = m_path.at(index);

        // 1. If path[index]'s slot-in-closed-tree is true, then increase currentHiddenLevel by 1.
        if (path_entry.slot_in_closed_tree)
            ++current_hidden_level;

        // 2. If currentHiddenLevel is less than or equal to maxHiddenLevel, then append path[index]'s invocation target to composedPath.
        if (current_hidden_level <= max_hidden_level) {
            VERIFY(path_entry.invocation_target);
            composed_path.append(const_cast<EventTarget*>(path_entry.invocation_target.ptr()));
        }

        // 3. If path[index]'s root-of-closed-tree is true, then:
        if (path_entry.root_of_closed_tree) {
            // 1. Decrease currentHiddenLevel by 1.
            --current_hidden_level;

            // 2. If currentHiddenLevel is less than maxHiddenLevel, then set maxHiddenLevel to currentHiddenLevel.
            if (current_hidden_level < max_hidden_level)
                max_hidden_level = current_hidden_level;
        }

        // 4. Increase index by 1.
    }

    // 16. Return composedPath.
    return composed_path;
}

}
