/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CodeComprehensionEngine.h"

namespace LanguageServers {

CodeComprehensionEngine::CodeComprehensionEngine(const FileDB& filedb, bool should_store_all_declarations)
    : m_filedb(filedb)
    , m_store_all_declarations(should_store_all_declarations)
{
}

CodeComprehensionEngine::~CodeComprehensionEngine()
{
}
void CodeComprehensionEngine::set_declarations_of_document(const String& filename, Vector<GUI::AutocompleteProvider::Declaration>&& declarations)
{
    if (!set_declarations_of_document_callback)
        return;

    // Optimization - Only notify callback if declarations have changed
    if (auto previous_declarations = m_all_declarations.get(filename); previous_declarations.has_value()) {
        if (previous_declarations.value() == declarations)
            return;
    }
    if (m_store_all_declarations)
        m_all_declarations.set(filename, declarations);
    set_declarations_of_document_callback(filename, move(declarations));
}
}
