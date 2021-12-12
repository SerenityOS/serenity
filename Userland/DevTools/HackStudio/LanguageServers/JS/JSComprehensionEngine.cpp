/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JSComprehensionEngine.h"
#include <AK/OwnPtr.h>
#include <AK/ScopeGuard.h>
#include <LibCore/DirIterator.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <Userland/DevTools/HackStudio/LanguageServers/ClientConnection.h>

namespace LanguageServers::JS {

JSComprehensionEngine::JSComprehensionEngine(FileDB const& filedb)
    : CodeComprehensionEngine(filedb, true)
{
}

const JSComprehensionEngine::DocumentData* JSComprehensionEngine::get_or_create_document_data(String const& file)
{
    auto absolute_path = filedb().to_absolute_path(file);
    if (!m_documents.contains(absolute_path)) {
        set_document_data(absolute_path, create_document_data_for(absolute_path));
    }
    return get_document_data(absolute_path);
}

const JSComprehensionEngine::DocumentData* JSComprehensionEngine::get_document_data(String const& file) const
{
    auto absolute_path = filedb().to_absolute_path(file);
    auto document_data = m_documents.get(absolute_path);
    if (!document_data.has_value())
        return nullptr;
    return document_data.value();
}

OwnPtr<JSComprehensionEngine::DocumentData> JSComprehensionEngine::create_document_data_for(String const& file)
{
    if (m_unfinished_documents.contains(file)) {
        return {};
    }
    m_unfinished_documents.set(file);
    ScopeGuard mark_finished([&file, this]() { m_unfinished_documents.remove(file); });
    auto document = filedb().get_or_create_from_filesystem(file);
    if (!document)
        return {};
    return create_document_data(document->text(), file);
}

void JSComprehensionEngine::set_document_data(String const& file, OwnPtr<DocumentData>&& data)
{
    m_documents.set(filedb().to_absolute_path(file), move(data));
}

Vector<GUI::AutocompleteProvider::Entry> JSComprehensionEngine::get_suggestions(String const&, const GUI::TextPosition&)
{
    // FIXME: Implement this :P
    return {};
}

void JSComprehensionEngine::on_edit(String const& file)
{
    set_document_data(file, create_document_data_for(file));
}

void JSComprehensionEngine::file_opened([[maybe_unused]] String const& file)
{
    get_or_create_document_data(file);
}

void JSComprehensionEngine::update_diagnostics(DocumentData const& document_data)
{
    if (!document_data.m_parser->has_errors()) {
        diagnostics_in_document_callback(document_data.filename(), {});
        return;
    }

    Vector<HackStudio::Diagnostic> diagnostics;
    diagnostics.ensure_capacity(document_data.m_parser->errors().size());

    for (auto& error : document_data.m_parser->errors()) {
        auto position = error.position.value_or({ 0, 0, 0 });
        auto start = GUI::AutocompleteProvider::ProjectLocation { document_data.m_filename, position.line, position.column };
        diagnostics.append(HackStudio::Diagnostic {
            start,
            start,
            error.message,
            HackStudio::Diagnostic::Level::Error,
        });
    }

    diagnostics_in_document_callback(document_data.filename(), move(diagnostics));
}

OwnPtr<JSComprehensionEngine::DocumentData> JSComprehensionEngine::create_document_data(String&& text, String const& filename)
{
    auto document_data = make<DocumentData>();
    document_data->m_filename = filename;
    document_data->m_text = move(text);
    document_data->m_lexer = make<JS::Lexer>(document_data->m_text);
    document_data->m_parser = make<JS::Parser>(*document_data->m_lexer);
    document_data->m_program_node = document_data->parser().parse_program();

    update_diagnostics(*document_data);

    return document_data;
}

}
