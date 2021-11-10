/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <DevTools/HackStudio/LanguageServers/CodeComprehensionEngine.h>
#include <Shell/Shell.h>

namespace LanguageServers::Shell {

class ShellComprehensionEngine : public CodeComprehensionEngine {
public:
    ShellComprehensionEngine(const FileDB& filedb);
    virtual Vector<GUI::AutocompleteProvider::Entry> get_suggestions(const String& file, const GUI::TextPosition& position) override;
    virtual void on_edit(const String& file) override;
    virtual void file_opened([[maybe_unused]] const String& file) override;
    virtual Optional<GUI::AutocompleteProvider::ProjectLocation> find_declaration_of(const String& filename, const GUI::TextPosition& identifier_position) override;

private:
    struct DocumentData {
        DocumentData(String&& text, String filename);
        String filename;
        String text;
        NonnullRefPtr<::Shell::AST::Node> node;

        const Vector<String>& sourced_paths() const;

    private:
        NonnullRefPtr<::Shell::AST::Node> parse() const;

        mutable Optional<Vector<String>> all_sourced_paths {};
    };

    const DocumentData& get_document_data(const String& file) const;
    const DocumentData& get_or_create_document_data(const String& file);
    void set_document_data(const String& file, OwnPtr<DocumentData>&& data);

    OwnPtr<DocumentData> create_document_data_for(const String& file);
    String document_path_from_include_path(StringView include_path) const;
    void update_declared_symbols(const DocumentData&);

    static size_t resolve(const ShellComprehensionEngine::DocumentData& document, const GUI::TextPosition& position);

    ::Shell::Shell& shell()
    {
        if (s_shell)
            return *s_shell;
        s_shell = ::Shell::Shell::construct();
        return *s_shell;
    }

    HashMap<String, OwnPtr<DocumentData>> m_documents;
    static RefPtr<::Shell::Shell> s_shell;
};
}
