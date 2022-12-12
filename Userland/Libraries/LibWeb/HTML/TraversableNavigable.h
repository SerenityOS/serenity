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
    virtual ~TraversableNavigable() override;

    bool is_top_level_traversable() const;

    int current_session_history_step() const { return m_current_session_history_step; };
    Vector<JS::NonnullGCPtr<SessionHistoryEntry>> const& session_history_entries() const { return m_session_history_entries; };
    bool running_nested_apply_history_step() const { return m_running_nested_apply_history_step; };
    VisibilityState system_visibility_state() const { return m_system_visibility_state; };

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
