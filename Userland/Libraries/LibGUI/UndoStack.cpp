/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

        if (m_clean_index.has_value()) {
            if (m_clean_index.value() < m_stack_index)
                m_clean_index.clear();
            else
                m_clean_index = m_clean_index.value() - m_stack_index;
        }

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

    if (m_clean_index.has_value())
        m_clean_index = m_clean_index.value() + 1;
}

void UndoStack::set_current_unmodified()
{
    // Skip empty container
    if (can_undo() && m_stack[m_stack_index].m_undo_vector.is_empty())
        m_clean_index = m_stack_index + 1;
    else
        m_clean_index = m_stack_index;
}

bool UndoStack::is_current_modified() const
{
    return !m_clean_index.has_value() || m_stack_index != m_clean_index.value();
}

void UndoStack::clear()
{
    m_stack.clear();
    m_stack_index = 0;
    m_clean_index.clear();
}

}
