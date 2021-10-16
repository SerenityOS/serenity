/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
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
void Event::initialize(const String& type, bool bubbles, bool cancelable)
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
void Event::init_event(const String& type, bool bubbles, bool cancelable)
{
    if (m_dispatch)
        return;

    initialize(type, bubbles, cancelable);
}

// https://dom.spec.whatwg.org/#dom-event-timestamp
double Event::time_stamp() const
{
    return m_time_stamp;
}

// https://dom.spec.whatwg.org/#dom-event-composedpath
NonnullRefPtrVector<EventTarget> Event::composed_path() const
{
    // 1. Let composedPath be an empty list.
    NonnullRefPtrVector<EventTarget> composed_path;

    // 2. Let path be this’s path. (NOTE: Not necessary)

    // 3. If path is empty, then return composedPath.
    if (m_path.is_empty())
        return composed_path;

    // 4. Let currentTarget be this’s currentTarget attribute value. (NOTE: Not necessary)

    // 5. Append currentTarget to composedPath.
    // NOTE: If path is not empty, then the event is being dispatched and will have a currentTarget.
    VERIFY(m_current_target);
    composed_path.append(*m_current_target);

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
            composed_path.prepend(*path_entry.invocation_target);
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
            composed_path.append(*path_entry.invocation_target);
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
