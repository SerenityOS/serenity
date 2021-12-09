/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WasmComprehensionEngine.h"
#include <AK/Assertions.h>
#include <AK/HashTable.h>
#include <Userland/DevTools/HackStudio/LanguageServers/ClientConnection.h>

namespace LanguageServers::Wasm {

WasmComprehensionEngine::WasmComprehensionEngine(FileDB const& filedb)
    : CodeComprehensionEngine(filedb, true)
{
}

const WasmComprehensionEngine::DocumentData& WasmComprehensionEngine::get_or_create_document_data(String const& file)
{
    auto absolute_path = filedb().to_absolute_path(file);
    if (!m_documents.contains(absolute_path)) {
        set_document_data(absolute_path, create_document_data_for(absolute_path));
    }
    return get_document_data(absolute_path);
}

const WasmComprehensionEngine::DocumentData& WasmComprehensionEngine::get_document_data(String const& file) const
{
    auto absolute_path = filedb().to_absolute_path(file);
    auto document_data = m_documents.get(absolute_path);
    VERIFY(document_data.has_value());
    return *document_data.value();
}

OwnPtr<WasmComprehensionEngine::DocumentData> WasmComprehensionEngine::create_document_data_for(String const& file)
{
    auto document = filedb().get(file);
    if (!document)
        return {};
    auto content = document->text();
    auto document_data = make<DocumentData>(document->text(), file);
    update_declared_symbols(*document_data);
    emit_diagnostics(file, *document_data);
    return document_data;
}

void WasmComprehensionEngine::set_document_data(String const& file, OwnPtr<DocumentData>&& data)
{
    emit_diagnostics(file, *data);
    m_documents.set(filedb().to_absolute_path(file), move(data));
}

WasmComprehensionEngine::DocumentData::DocumentData(String&& _text, String _filename)
    : filename(move(_filename))
    , text(move(_text))
    , parse_result(parse())
{
}

ErrorOr<::Wasm::Module, ::Wasm::TextFormatParseError> WasmComprehensionEngine::DocumentData::parse() const
{
    GenericLexer lexer { text };
    DuplexMemoryStream binary_stream;
    TRY(::Wasm::parse_and_generate_module_from_text_format(lexer, binary_stream));

    auto parse_result = ::Wasm::Module::parse(binary_stream);
    if (parse_result.is_error()) {
        return ::Wasm::TextFormatParseError {
            0, 0,
            String::formatted("Likely internal error: {}", ::Wasm::parse_error_to_string(parse_result.error()))
        };
    }

    return parse_result.release_value();
}

Vector<GUI::AutocompleteProvider::Entry> WasmComprehensionEngine::get_suggestions(String const&, const GUI::TextPosition& position)
{
    dbgln_if(SH_LANGUAGE_SERVER_DEBUG, "WasmComprehensionEngine position {}:{}", position.line(), position.column());

    // FIXME: Generate suggestions somehow
    Vector<GUI::AutocompleteProvider::Entry> entries;
    return entries;
}

void WasmComprehensionEngine::emit_diagnostics(String const& filename, DocumentData const& document_data)
{
    if (!document_data.parse_result.is_error()) {
        diagnostics_in_document_callback(filename, {});
        return;
    }

    Vector<HackStudio::Diagnostic> diagnostics;

    auto const& error = static_cast<::Wasm::TextFormatParseError&>(document_data.parse_result.error());
    diagnostics.append({
        { filename, error.line + 1, error.column + 1 },
        { filename, error.line + 1, error.column + 2 },
        error.error,
        HackStudio::Diagnostic::Level::Error,
    });

    diagnostics_in_document_callback(filename, move(diagnostics));
}

void WasmComprehensionEngine::on_edit(String const& file)
{
    set_document_data(file, create_document_data_for(file));
}

void WasmComprehensionEngine::file_opened([[maybe_unused]] String const& file)
{
    set_document_data(file, create_document_data_for(file));
}

Optional<GUI::AutocompleteProvider::ProjectLocation> WasmComprehensionEngine::find_declaration_of(String const&, GUI::TextPosition const&)
{
    // FIXME: Implement this.

    return {};
}

void WasmComprehensionEngine::update_declared_symbols(DocumentData const& document)
{
    Vector<GUI::AutocompleteProvider::Declaration> declarations;
    set_declarations_of_document(document.filename, move(declarations));
}
}
