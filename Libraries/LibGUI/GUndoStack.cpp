/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGUI/GUndoStack.h>

namespace GUI {

UndoStack::UndoStack()
{
}

UndoStack::~UndoStack()
{
}

void UndoStack::undo()
{
    if (!can_undo())
        return;

    auto& undo_container = m_stack[m_stack_index];
    auto& undo_vector = undo_container.m_undo_vector;

    //If we try to undo a empty vector, delete it and skip over.
    if (undo_vector.is_empty()) {
        m_stack.remove(m_stack_index);
        undo();
        return;
    }

    for (int i = 0; i < undo_vector.size(); i++) {
        auto& undo_command = undo_vector[i];
        undo_command.undo();
    }

    m_stack_index++;
}

void UndoStack::redo()
{
    if (!can_redo())
        return;

    auto& undo_container = m_stack[m_stack_index - 1];
    auto& redo_vector = undo_container.m_undo_vector;

    for (int i = redo_vector.size() - 1; i >= 0; i--) {
        auto& undo_command = redo_vector[i];
        undo_command.redo();
    }

    m_stack_index--;
}

void UndoStack::push(NonnullOwnPtr<Command>&& command)
{
    if (m_stack.is_empty()) {
        auto undo_commands_container = make<UndoCommandsContainer>();
        m_stack.prepend(move(undo_commands_container));
    }

    // Clear the elements of the stack before the m_undo_stack_index (Excluding our new element)
    for (int i = 1; i < m_stack_index; i++)
        m_stack.remove(1);

    if (m_stack_index > 0 && !m_stack.is_empty())
        m_stack[0].m_undo_vector.clear();

    m_stack_index = 0;

    m_stack[0].m_undo_vector.prepend(move(command));
}

void UndoStack::finalize_current_combo()
{
    if (m_stack.is_empty())
        return;

    auto& undo_vector = m_stack[0].m_undo_vector;

    if (undo_vector.size() == m_last_updated_undo_vector_size && !undo_vector.is_empty()) {
        auto undo_commands_container = make<UndoCommandsContainer>();
        m_stack.prepend(move(undo_commands_container));
        // Note: Remove dbg() if we're 100% sure there are no bugs left.
        dbg() << "Undo stack increased to " << m_stack.size();

        // Shift the index to the left since we're adding an empty container.
        if (m_stack_index > 0)
            m_stack_index++;
    }

    m_last_updated_undo_vector_size = undo_vector.size();
}

}
