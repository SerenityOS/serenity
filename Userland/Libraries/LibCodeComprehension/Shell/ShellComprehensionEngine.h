/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCodeComprehension/CodeComprehensionEngine.h>
#include <LibShell/Shell.h>

namespace CodeComprehension::Shell {

class ShellComprehensionEngine : public CodeComprehensionEngine {
public:
    ShellComprehensionEngine(FileDB const& filedb);
    virtual Vector<CodeComprehension::AutocompleteResultEntry> get_suggestions(ByteString const& file, const GUI::TextPosition& position) override;
    virtual void on_edit(ByteString const& file) override;
    virtual void file_opened([[maybe_unused]] ByteString const& file) override;
    virtual Optional<CodeComprehension::ProjectLocation> find_declaration_of(ByteString const& filename, const GUI::TextPosition& identifier_position) override;

private:
    struct DocumentData {
        DocumentData(ByteString&& text, ByteString filename);
        ByteString filename;
        ByteString text;
        NonnullRefPtr<::Shell::AST::Node> node;

        Vector<ByteString> const& sourced_paths() const;

    private:
        NonnullRefPtr<::Shell::AST::Node> parse() const;

        mutable Optional<Vector<ByteString>> all_sourced_paths {};
    };

    DocumentData const& get_document_data(ByteString const& file) const;
    DocumentData const& get_or_create_document_data(ByteString const& file);
    void set_document_data(ByteString const& file, OwnPtr<DocumentData>&& data);

    OwnPtr<DocumentData> create_document_data_for(ByteString const& file);
    void update_declared_symbols(DocumentData const&);

    static size_t resolve(ShellComprehensionEngine::DocumentData const& document, const GUI::TextPosition& position);

    ::Shell::Shell& shell()
    {
        if (s_shell)
            return *s_shell;
        s_shell = ::Shell::Shell::construct();
        return *s_shell;
    }

    HashMap<ByteString, OwnPtr<DocumentData>> m_documents;
    static RefPtr<::Shell::Shell> s_shell;
};
}
