/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <DevTools/HackStudio/LanguageServers/CodeComprehensionEngine.h>
#include <LibWasm/Parser/TextFormat.h>
#include <LibWasm/Types.h>

namespace LanguageServers::Wasm {

class WasmComprehensionEngine : public CodeComprehensionEngine {
public:
    WasmComprehensionEngine(FileDB const& filedb);
    virtual Vector<GUI::AutocompleteProvider::Entry> get_suggestions(String const& file, const GUI::TextPosition& position) override;
    virtual void on_edit(String const& file) override;
    virtual void file_opened([[maybe_unused]] String const& file) override;
    virtual Optional<GUI::AutocompleteProvider::ProjectLocation> find_declaration_of(String const& filename, const GUI::TextPosition& identifier_position) override;

private:
    struct DocumentData {
        DocumentData(String&& text, String filename);
        String filename;
        String text;
        mutable ErrorOr<::Wasm::Module, ::Wasm::TextFormatParseError> parse_result;

    private:
        ErrorOr<::Wasm::Module, ::Wasm::TextFormatParseError> parse() const;
    };

    void emit_diagnostics(String const&, DocumentData const&);
    DocumentData const& get_document_data(String const& file) const;
    DocumentData const& get_or_create_document_data(String const& file);
    void set_document_data(String const& file, OwnPtr<DocumentData>&& data);

    OwnPtr<DocumentData> create_document_data_for(String const& file);
    void update_declared_symbols(DocumentData const&);

    HashMap<String, OwnPtr<DocumentData>> m_documents;
};
}
