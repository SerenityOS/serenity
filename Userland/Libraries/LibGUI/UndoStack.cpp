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

bool UndoStack::can_undo() const
{
    return m_stack_index > 0;
}

bool UndoStack::can_redo() const
{
    if (m_stack.is_empty())
        return false;
    return m_stack_index != m_stack.size() - 1;
}

void UndoStack::undo()
{
    if (!can_undo())
        return;

    finalize_current_combo();
    auto& combo = m_stack[--m_stack_index];
    for (auto i = static_cast<ssize_t>(combo.commands.size()) - 1; i >= 0; i--)
        combo.commands[i].undo();
}

void UndoStack::redo()
{
    if (!can_redo())
        return;

    auto& commands = m_stack[m_stack_index++].commands;
    for (auto& command : commands)
        command.redo();
}

void UndoStack::pop()
{
    VERIFY(!m_stack.is_empty());
    m_stack.take_last();
    if (m_clean_index.has_value() && m_clean_index.value() > m_stack.size())
        m_clean_index = {};
}

void UndoStack::push(NonnullOwnPtr<Command>&& command)
{
    if (m_stack.is_empty())
        m_stack.append(make<Combo>());

    // If the stack cursor is behind the top of the stack, nuke everything from here to the top.
    if (m_stack_index != m_stack.size() - 1) {
        while (m_stack.size() != m_stack_index) {
            pop();
        }
        finalize_current_combo();
    }

    m_stack.last().commands.append(move(command));
}

void UndoStack::finalize_current_combo()
{
    if (m_stack.is_empty()) {
        m_stack.append(make<Combo>());
        return;
    }

    if (!m_stack.last().commands.is_empty()) {
        m_stack.append(make<Combo>());
        m_stack_index = m_stack.size() - 1;
    }
}

void UndoStack::set_current_unmodified()
{
    m_clean_index = m_stack_index;
}

bool UndoStack::is_current_modified() const
{
    return !(m_clean_index.has_value() && m_clean_index.value() == m_stack_index);
}

void UndoStack::clear()
{
    m_stack.clear();
    m_stack_index = 0;
    m_clean_index.clear();
}

}
