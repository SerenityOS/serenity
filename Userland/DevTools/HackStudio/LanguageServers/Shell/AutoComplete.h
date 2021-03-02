/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <DevTools/HackStudio/LanguageServers/AutoCompleteEngine.h>
#include <Shell/Shell.h>

namespace LanguageServers::Shell {

class AutoComplete : public AutoCompleteEngine {
public:
    AutoComplete(ClientConnection&, const FileDB& filedb);
    virtual Vector<GUI::AutocompleteProvider::Entry> get_suggestions(const String& file, const GUI::TextPosition& position) override;
    virtual void on_edit(const String& file) override;
    virtual void file_opened([[maybe_unused]] const String& file) override;
    virtual Optional<GUI::AutocompleteProvider::ProjectLocation> find_declaration_of(const String& file_name, const GUI::TextPosition& identifier_position) override;

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
    String document_path_from_include_path(const StringView& include_path) const;
    void update_declared_symbols(const DocumentData&);

    static size_t resolve(const AutoComplete::DocumentData& document, const GUI::TextPosition& position);

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
