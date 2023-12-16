/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <LibGUI/Command.h>
#include <LibGUI/UndoStack.h>

namespace GUI {

bool UndoStack::can_undo() const
{
    return m_stack_index > 0;
}

bool UndoStack::can_redo() const
{
    if (m_stack.is_empty())
        return false;
    return m_stack_index != m_stack.size();
}

void UndoStack::undo()
{
    if (!can_undo())
        return;

    auto& command = m_stack[--m_stack_index];
    command->undo();

    if (on_state_change)
        on_state_change();
}

void UndoStack::redo()
{
    if (!can_redo())
        return;

    auto& command = m_stack[m_stack_index++];
    command->redo();

    if (on_state_change)
        on_state_change();
}

ErrorOr<void> UndoStack::try_push(NonnullOwnPtr<Command> command)
{
    // If the stack cursor is behind the top of the stack, nuke everything from here to the top.
    while (m_stack.size() != m_stack_index)
        (void)m_stack.take_last();

    if (m_clean_index.has_value() && m_clean_index.value() > m_stack.size())
        m_clean_index = {};

    if (!m_stack.is_empty() && is_current_modified()) {
        if (m_stack.last()->merge_with(*command))
            return {};
    }

    TRY(m_stack.try_append(move(command)));
    m_stack_index = m_stack.size();

    if (on_state_change)
        on_state_change();

    return {};
}

void UndoStack::push(NonnullOwnPtr<Command> command)
{
    MUST(try_push(move(command)));
}

void UndoStack::set_current_unmodified()
{
    if (m_clean_index.has_value() && m_clean_index.value() == m_stack_index)
        return;

    m_clean_index = m_stack_index;
    m_last_unmodified_timestamp = MonotonicTime::now();

    if (on_state_change)
        on_state_change();
}

bool UndoStack::is_current_modified() const
{
    if (m_stack.is_empty())
        return false;
    if (!m_clean_index.has_value())
        return true;
    if (m_stack_index != m_clean_index.value())
        return true;
    return false;
}

void UndoStack::clear()
{
    if (m_stack.is_empty() && m_stack_index == 0 && !m_clean_index.has_value())
        return;

    m_stack.clear();
    m_stack_index = 0;
    m_clean_index.clear();

    if (on_state_change)
        on_state_change();
}

Optional<ByteString> UndoStack::undo_action_text() const
{
    if (!can_undo())
        return {};
    return m_stack[m_stack_index - 1]->action_text();
}

Optional<ByteString> UndoStack::redo_action_text() const
{
    if (!can_redo())
        return {};
    return m_stack[m_stack_index]->action_text();
}

}
