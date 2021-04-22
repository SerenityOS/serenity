/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibGUI/Forward.h>

namespace GUI {

class UndoStack {
public:
    UndoStack();
    ~UndoStack();

    void push(NonnullOwnPtr<Command>&&);

    bool can_undo() const { return m_stack_index < m_stack.size() && !m_stack.is_empty(); }
    bool can_redo() const { return m_stack_index > 0 && !m_stack.is_empty(); }

    void undo();
    void redo();

    void finalize_current_combo();

private:
    struct UndoCommandsContainer {
        NonnullOwnPtrVector<Command> m_undo_vector;
    };

    NonnullOwnPtrVector<UndoCommandsContainer> m_stack;
    size_t m_stack_index { 0 };
};

}
