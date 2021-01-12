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

#include <LibGUI/Command.h>
#include <LibGUI/UndoStack.h>

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

    auto pop_container_and_undo = [this]() {
        for (;;) {
            if (m_stack_index >= m_stack.size())
                break;

            auto& container = m_stack[m_stack_index++];
            if (container.m_undo_vector.size() == 0)
                continue;
            for (auto& command : container.m_undo_vector)
                command.undo();
            break;
        }
    };

    // If this is the first undo, finish off our current combo
    if (m_stack_index == 0)
        finalize_current_combo();
    pop_container_and_undo();
}

void UndoStack::redo()
{
    if (!can_redo())
        return;

    m_stack_index -= 1;
    auto& vector = m_stack[m_stack_index].m_undo_vector;
    for (int i = vector.size() - 1; i >= 0; i--)
        vector[i].redo();
}

void UndoStack::push(NonnullOwnPtr<Command>&& command)
{
    if (m_stack.is_empty())
        finalize_current_combo();

    if (m_stack_index > 0) {
        for (size_t i = 0; i < m_stack_index; i++)
            m_stack.remove(0);
        m_stack_index = 0;
        finalize_current_combo();
    }

    auto& current_vector = m_stack.first().m_undo_vector;
    current_vector.prepend(move(command));
}

void UndoStack::finalize_current_combo()
{
    if (m_stack_index > 0)
        return;
    if (m_stack.size() != 0 && m_stack.first().m_undo_vector.size() == 0)
        return;

    auto undo_commands_container = make<UndoCommandsContainer>();
    m_stack.prepend(move(undo_commands_container));
}

}
