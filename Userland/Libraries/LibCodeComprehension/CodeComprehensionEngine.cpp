/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CodeComprehensionEngine.h"

namespace CodeComprehension {

CodeComprehensionEngine::CodeComprehensionEngine(FileDB const& filedb, bool should_store_all_declarations)
    : m_filedb(filedb)
    , m_store_all_declarations(should_store_all_declarations)
{
}

void CodeComprehensionEngine::set_declarations_of_document(ByteString const& filename, Vector<Declaration>&& declarations)
{
    // Callback may not be configured if we're running tests
    if (!set_declarations_of_document_callback)
        return;

    // Optimization - Only notify callback if declarations have changed
    if (auto previous_declarations = m_all_declarations.find(filename); previous_declarations != m_all_declarations.end()) {
        if (previous_declarations->value == declarations)
            return;
    }
    if (m_store_all_declarations)
        m_all_declarations.set(filename, declarations);
    set_declarations_of_document_callback(filename, move(declarations));
}

void CodeComprehensionEngine::set_todo_entries_of_document(ByteString const& filename, Vector<TodoEntry>&& todo_entries)
{
    // Callback may not be configured if we're running tests
    if (!set_todo_entries_of_document_callback)
        return;
    set_todo_entries_of_document_callback(filename, move(todo_entries));
}

}
