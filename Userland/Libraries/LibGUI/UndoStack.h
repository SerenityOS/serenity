/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Time.h>
#include <LibGUI/Forward.h>

namespace GUI {

class UndoStack {
public:
    UndoStack() = default;
    ~UndoStack() = default;

    void push(NonnullOwnPtr<Command>);

    bool can_undo() const;
    bool can_redo() const;

    void undo();
    void redo();

    void set_current_unmodified();
    bool is_current_modified() const;

    Optional<Time> last_unmodified_timestamp() const { return m_last_unmodified_timestamp; }

    void clear();

    Optional<String> undo_action_text() const;
    Optional<String> redo_action_text() const;

    Function<void()> on_state_change;

private:
    NonnullOwnPtrVector<Command> m_stack;
    size_t m_stack_index { 0 };
    Optional<size_t> m_clean_index;
    Optional<Time> m_last_unmodified_timestamp;
};

}
