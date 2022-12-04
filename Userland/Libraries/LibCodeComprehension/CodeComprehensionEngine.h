/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "FileDB.h"
#include "Types.h"
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibGUI/TextPosition.h>

namespace CodeComprehension {

class CodeComprehensionEngine {
    AK_MAKE_NONCOPYABLE(CodeComprehensionEngine);
    AK_MAKE_NONMOVABLE(CodeComprehensionEngine);

public:
    CodeComprehensionEngine(FileDB const& filedb, bool store_all_declarations = false);
    virtual ~CodeComprehensionEngine() = default;

    virtual Vector<AutocompleteResultEntry> get_suggestions(DeprecatedString const& file, GUI::TextPosition const& autocomplete_position) = 0;

    // TODO: In the future we can pass the range that was edited and only re-parse what we have to.
    virtual void on_edit([[maybe_unused]] DeprecatedString const& file) {};
    virtual void file_opened([[maybe_unused]] DeprecatedString const& file) {};

    virtual Optional<ProjectLocation> find_declaration_of(DeprecatedString const&, GUI::TextPosition const&) { return {}; }

    struct FunctionParamsHint {
        Vector<DeprecatedString> params;
        size_t current_index { 0 };
    };
    virtual Optional<FunctionParamsHint> get_function_params_hint(DeprecatedString const&, GUI::TextPosition const&) { return {}; }

    virtual Vector<TokenInfo> get_tokens_info(DeprecatedString const&) { return {}; }

    Function<void(DeprecatedString const&, Vector<Declaration>&&)> set_declarations_of_document_callback;
    Function<void(DeprecatedString const&, Vector<TodoEntry>&&)> set_todo_entries_of_document_callback;

protected:
    FileDB const& filedb() const { return m_filedb; }
    void set_declarations_of_document(DeprecatedString const&, Vector<Declaration>&&);
    void set_todo_entries_of_document(DeprecatedString const&, Vector<TodoEntry>&&);
    HashMap<DeprecatedString, Vector<Declaration>> const& all_declarations() const { return m_all_declarations; }

private:
    HashMap<DeprecatedString, Vector<Declaration>> m_all_declarations;
    FileDB const& m_filedb;
    bool m_store_all_declarations { false };
};
}
