/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/ShadowRoot.h>

namespace Web::DOM {

void Event::append_to_path(EventTarget& invocation_target, RefPtr<EventTarget> shadow_adjusted_target, RefPtr<EventTarget> related_target, TouchTargetList& touch_targets, bool slot_in_closed_tree)
{
    bool invocation_target_in_shadow_tree = false;
    bool root_of_closed_tree = false;

    if (is<Node>(invocation_target)) {
        auto& invocation_target_node = verify_cast<Node>(invocation_target);
        if (is<ShadowRoot>(invocation_target_node.root()))
            invocation_target_in_shadow_tree = true;
        if (is<ShadowRoot>(invocation_target_node)) {
            auto& invocation_target_shadow_root = verify_cast<ShadowRoot>(invocation_target_node);
            root_of_closed_tree = invocation_target_shadow_root.closed();
        }
    }

    m_path.append({ invocation_target, invocation_target_in_shadow_tree, shadow_adjusted_target, related_target, touch_targets, root_of_closed_tree, slot_in_closed_tree, m_path.size() });
}

void Event::set_cancelled_flag()
{
    if (m_cancelable && !m_in_passive_listener)
        m_cancelled = true;
}

// https://dom.spec.whatwg.org/#concept-event-initialize
void Event::initialize(String const& type, bool bubbles, bool cancelable)
{
    m_initialized = true;
    m_stop_propagation = false;
    m_stop_immediate_propagation = false;
    m_cancelled = false;
    m_is_trusted = false;
    m_target = nullptr;
    m_type = type;
    m_bubbles = bubbles;
    m_cancelable = cancelable;
}

// https://dom.spec.whatwg.org/#dom-event-initevent
void Event::init_event(String const& type, bool bubbles, bool cancelable)
{
    if (m_dispatch)
        return;

    initialize(type, bubbles, cancelable);
}

}
