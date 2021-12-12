/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <DevTools/HackStudio/AutoCompleteResponse.h>
#include <DevTools/HackStudio/LanguageServers/CodeComprehensionEngine.h>
#include <DevTools/HackStudio/LanguageServers/FileDB.h>
#include <LibGUI/TextPosition.h>
#include <LibJS/AST.h>
#include <LibJS/Parser.h>

namespace LanguageServers::JS {

using namespace ::JS;

class JSComprehensionEngine : public CodeComprehensionEngine {
public:
    JSComprehensionEngine(FileDB const& filedb);

    virtual Vector<GUI::AutocompleteProvider::Entry> get_suggestions(String const& file, const GUI::TextPosition& autocomplete_position) override;
    virtual void on_edit(String const& file) override;
    virtual void file_opened([[maybe_unused]] String const& file) override;

private:
    struct DocumentData {
        String const& filename() const { return m_filename; }
        String const& text() const { return m_text; }
        Parser const& parser() const
        {
            VERIFY(m_parser);
            return *m_parser;
        }
        Parser& parser()
        {
            VERIFY(m_parser);
            return *m_parser;
        }

        String m_filename;
        String m_text;
        OwnPtr<JS::Lexer> m_lexer;
        OwnPtr<Parser> m_parser;
        RefPtr<JS::Program> m_program_node;
    };

    const DocumentData* get_document_data(String const& file) const;
    const DocumentData* get_or_create_document_data(String const& file);
    void set_document_data(String const& file, OwnPtr<DocumentData>&& data);

    OwnPtr<DocumentData> create_document_data_for(String const& file);
    OwnPtr<DocumentData> create_document_data(String&& text, String const& filename);
    void update_diagnostics(DocumentData const&);

    HashMap<String, OwnPtr<DocumentData>> m_documents;

    // A document's path will be in this set if we're currently processing it.
    // A document is added to this set when we start processing it (e.g because it was #included) and removed when we're done.
    // We use this to prevent circular #includes from looping indefinitely.
    HashTable<String> m_unfinished_documents;
};

}
