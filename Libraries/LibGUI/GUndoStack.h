#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibGUI/GCommand.h>

class GCommand;

class GUndoStack {
public:
    GUndoStack();
    ~GUndoStack();

    void push(NonnullOwnPtr<GCommand>&&);

    bool can_undo() const { return m_stack_index < m_stack.size() && !m_stack.is_empty(); }
    bool can_redo() const { return m_stack_index > 0 && m_stack[m_stack_index - 1].m_undo_vector.size() > 0 && !m_stack.is_empty(); }

    void undo();
    void redo();

    void finalize_current_combo();

private:
    struct UndoCommandsContainer {
        NonnullOwnPtrVector<GCommand> m_undo_vector;
    };

    NonnullOwnPtrVector<UndoCommandsContainer> m_stack;
    int m_stack_index { 0 };
    int m_last_updated_undo_vector_size { 0 };
};
