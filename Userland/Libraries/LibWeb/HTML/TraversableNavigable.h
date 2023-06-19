/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/HTML/Navigable.h>
#include <LibWeb/HTML/VisibilityState.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/document-sequences.html#traversable-navigable
class TraversableNavigable final : public Navigable {
    JS_CELL(TraversableNavigable, Navigable);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<TraversableNavigable>> create_a_new_top_level_traversable(Page&, JS::GCPtr<BrowsingContext> opener, String target_name);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<TraversableNavigable>> create_a_fresh_top_level_traversable(Page&, AK::URL const& initial_navigation_url, Variant<Empty, String, POSTResource> = Empty {});

    virtual ~TraversableNavigable() override;

    bool is_top_level_traversable() const;

    int current_session_history_step() const { return m_current_session_history_step; };
    Vector<JS::NonnullGCPtr<SessionHistoryEntry>>& session_history_entries() { return m_session_history_entries; };
    Vector<JS::NonnullGCPtr<SessionHistoryEntry>> const& session_history_entries() const { return m_session_history_entries; };
    bool running_nested_apply_history_step() const { return m_running_nested_apply_history_step; };
    VisibilityState system_visibility_state() const { return m_system_visibility_state; };

    struct HistoryObjectLengthAndIndex {
        size_t script_history_length;
        size_t script_history_index;
    };
    HistoryObjectLengthAndIndex get_the_history_object_length_and_index(int) const;

    Vector<int> get_all_used_history_steps() const;
    void clear_the_forward_session_history();

private:
    TraversableNavigable();

    virtual void visit_edges(Cell::Visitor&) override;

    // https://html.spec.whatwg.org/multipage/document-sequences.html#tn-current-session-history-step
    int m_current_session_history_step { 0 };

    // https://html.spec.whatwg.org/multipage/document-sequences.html#tn-session-history-entries
    Vector<JS::NonnullGCPtr<SessionHistoryEntry>> m_session_history_entries;

    // FIXME: https://html.spec.whatwg.org/multipage/document-sequences.html#tn-session-history-traversal-queue

    // https://html.spec.whatwg.org/multipage/document-sequences.html#tn-running-nested-apply-history-step
    bool m_running_nested_apply_history_step { false };

    // https://html.spec.whatwg.org/multipage/document-sequences.html#system-visibility-state
    VisibilityState m_system_visibility_state { VisibilityState::Visible };
};

}
