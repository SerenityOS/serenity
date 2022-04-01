/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../AutoCompleteResponse.h"
#include "FileDB.h"
#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/TextPosition.h>

namespace LanguageServers {

class ConnectionFromClient;

class CodeComprehensionEngine {
public:
    CodeComprehensionEngine(FileDB const& filedb, bool store_all_declarations = false);
    virtual ~CodeComprehensionEngine() = default;

    virtual Vector<GUI::AutocompleteProvider::Entry> get_suggestions(String const& file, const GUI::TextPosition& autocomplete_position) = 0;

    // TODO: In the future we can pass the range that was edited and only re-parse what we have to.
    virtual void on_edit([[maybe_unused]] String const& file) {};
    virtual void file_opened([[maybe_unused]] String const& file) {};

    virtual Optional<GUI::AutocompleteProvider::ProjectLocation> find_declaration_of(String const&, const GUI::TextPosition&) { return {}; }

    struct FunctionParamsHint {
        Vector<String> params;
        size_t current_index { 0 };
    };
    virtual Optional<FunctionParamsHint> get_function_params_hint(String const&, const GUI::TextPosition&) { return {}; }

    virtual Vector<GUI::AutocompleteProvider::TokenInfo> get_tokens_info(String const&) { return {}; }

public:
    Function<void(String const&, Vector<GUI::AutocompleteProvider::Declaration>&&)> set_declarations_of_document_callback;
    Function<void(String const&, Vector<Cpp::Parser::TodoEntry>&&)> set_todo_entries_of_document_callback;

protected:
    FileDB const& filedb() const { return m_filedb; }
    void set_declarations_of_document(String const&, Vector<GUI::AutocompleteProvider::Declaration>&&);
    void set_todo_entries_of_document(String const&, Vector<Cpp::Parser::TodoEntry>&&);
    HashMap<String, Vector<GUI::AutocompleteProvider::Declaration>> const& all_declarations() const { return m_all_declarations; }

private:
    HashMap<String, Vector<GUI::AutocompleteProvider::Declaration>> m_all_declarations;
    FileDB const& m_filedb;
    bool m_store_all_declarations { false };
};
}
