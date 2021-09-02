/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ShellComprehensionEngine.h"
#include <AK/Assertions.h>
#include <AK/HashTable.h>
#include <LibRegex/Regex.h>
#include <Userland/DevTools/HackStudio/LanguageServers/ClientConnection.h>

namespace LanguageServers::Shell {

RefPtr<::Shell::Shell> ShellComprehensionEngine::s_shell {};

ShellComprehensionEngine::ShellComprehensionEngine(const FileDB& filedb)
    : CodeComprehensionEngine(filedb, true)
{
}

const ShellComprehensionEngine::DocumentData& ShellComprehensionEngine::get_or_create_document_data(const String& file)
{
    auto absolute_path = filedb().to_absolute_path(file);
    if (!m_documents.contains(absolute_path)) {
        set_document_data(absolute_path, create_document_data_for(absolute_path));
    }
    return get_document_data(absolute_path);
}

const ShellComprehensionEngine::DocumentData& ShellComprehensionEngine::get_document_data(const String& file) const
{
    auto absolute_path = filedb().to_absolute_path(file);
    auto document_data = m_documents.get(absolute_path);
    VERIFY(document_data.has_value());
    return *document_data.value();
}

OwnPtr<ShellComprehensionEngine::DocumentData> ShellComprehensionEngine::create_document_data_for(const String& file)
{
    auto document = filedb().get(file);
    if (!document)
        return {};
    auto content = document->text();
    auto document_data = make<DocumentData>(document->text(), file);
    for (auto& path : document_data->sourced_paths())
        get_or_create_document_data(path);

    update_declared_symbols(*document_data);
    return document_data;
}

void ShellComprehensionEngine::set_document_data(const String& file, OwnPtr<DocumentData>&& data)
{
    m_documents.set(filedb().to_absolute_path(file), move(data));
}

ShellComprehensionEngine::DocumentData::DocumentData(String&& _text, String _filename)
    : filename(move(_filename))
    , text(move(_text))
    , node(parse())
{
}

const Vector<String>& ShellComprehensionEngine::DocumentData::sourced_paths() const
{
    if (all_sourced_paths.has_value())
        return all_sourced_paths.value();

    struct : public ::Shell::AST::NodeVisitor {
        void visit(const ::Shell::AST::CastToCommand* node) override
        {
            auto& inner = node->inner();
            if (inner->is_list()) {
                if (auto* list = dynamic_cast<const ::Shell::AST::ListConcatenate*>(inner.ptr())) {
                    auto& entries = list->list();
                    if (entries.size() == 2 && entries.first()->is_bareword() && static_ptr_cast<::Shell::AST::BarewordLiteral>(entries.first())->text() == "source") {
                        auto& filename = entries[1];
                        if (filename->would_execute())
                            return;
                        auto name_list = const_cast<::Shell::AST::Node*>(filename.ptr())->run(nullptr)->resolve_as_list(nullptr);
                        StringBuilder builder;
                        builder.join(" ", name_list);
                        sourced_files.set(builder.build());
                    }
                }
            }
            ::Shell::AST::NodeVisitor::visit(node);
        }

        HashTable<String> sourced_files;
    } visitor;

    node->visit(visitor);

    Vector<String> sourced_paths;
    for (auto& entry : visitor.sourced_files)
        sourced_paths.append(move(entry));

    all_sourced_paths = move(sourced_paths);
    return all_sourced_paths.value();
}

NonnullRefPtr<::Shell::AST::Node> ShellComprehensionEngine::DocumentData::parse() const
{
    ::Shell::Parser parser { text };
    if (auto node = parser.parse())
        return node.release_nonnull();

    return ::Shell::AST::make_ref_counted<::Shell::AST::SyntaxError>(::Shell::AST::Position {}, "Unable to parse file");
}

size_t ShellComprehensionEngine::resolve(const ShellComprehensionEngine::DocumentData& document, const GUI::TextPosition& position)
{
    size_t offset = 0;

    if (position.line() > 0) {
        auto first = true;
        size_t line = 0;
        for (auto& line_view : document.text.split_limit('\n', position.line() + 1, true)) {
            if (line == position.line())
                break;
            if (first)
                first = false;
            else
                ++offset; // For the newline.
            offset += line_view.length();
            ++line;
        }
    }

    offset += position.column() + 1;
    return offset;
}

Vector<GUI::AutocompleteProvider::Entry> ShellComprehensionEngine::get_suggestions(const String& file, const GUI::TextPosition& position)
{
    dbgln_if(SH_LANGUAGE_SERVER_DEBUG, "ShellComprehensionEngine position {}:{}", position.line(), position.column());

    const auto& document = get_or_create_document_data(file);
    size_t offset_in_file = resolve(document, position);

    ::Shell::AST::HitTestResult hit_test = document.node->hit_test_position(offset_in_file);
    if (!hit_test.matching_node) {
        dbgln_if(SH_LANGUAGE_SERVER_DEBUG, "no node at position {}:{}", position.line(), position.column());
        return {};
    }

    auto completions = const_cast<::Shell::AST::Node*>(document.node.ptr())->complete_for_editor(shell(), offset_in_file, hit_test);
    Vector<GUI::AutocompleteProvider::Entry> entries;
    for (auto& completion : completions)
        entries.append({ completion.text_string, completion.input_offset });

    return entries;
}

void ShellComprehensionEngine::on_edit(const String& file)
{
    set_document_data(file, create_document_data_for(file));
}

void ShellComprehensionEngine::file_opened([[maybe_unused]] const String& file)
{
    set_document_data(file, create_document_data_for(file));
}

Optional<GUI::AutocompleteProvider::ProjectLocation> ShellComprehensionEngine::find_declaration_of(const String& filename, const GUI::TextPosition& identifier_position)
{
    dbgln_if(SH_LANGUAGE_SERVER_DEBUG, "find_declaration_of({}, {}:{})", filename, identifier_position.line(), identifier_position.column());
    const auto& document = get_or_create_document_data(filename);
    auto position = resolve(document, identifier_position);
    auto result = document.node->hit_test_position(position);
    if (!result.matching_node) {
        dbgln_if(SH_LANGUAGE_SERVER_DEBUG, "no node at position {}:{}", identifier_position.line(), identifier_position.column());
        return {};
    }

    if (!result.matching_node->is_bareword()) {
        dbgln_if(SH_LANGUAGE_SERVER_DEBUG, "no bareword at position {}:{}", identifier_position.line(), identifier_position.column());
        return {};
    }

    auto name = static_ptr_cast<::Shell::AST::BarewordLiteral>(result.matching_node)->text();
    auto& declarations = all_declarations();
    for (auto& entry : declarations) {
        for (auto& declaration : entry.value) {
            if (declaration.name == name)
                return declaration.position;
        }
    }

    return {};
}

void ShellComprehensionEngine::update_declared_symbols(const DocumentData& document)
{
    struct Visitor : public ::Shell::AST::NodeVisitor {
        explicit Visitor(const String& filename)
            : filename(filename)
        {
        }

        void visit(const ::Shell::AST::VariableDeclarations* node) override
        {
            for (auto& entry : node->variables()) {
                auto literal = entry.name->leftmost_trivial_literal();
                if (!literal)
                    continue;

                String name;
                if (literal->is_bareword())
                    name = static_ptr_cast<::Shell::AST::BarewordLiteral>(literal)->text();

                if (!name.is_empty()) {
                    dbgln("Found variable {}", name);
                    declarations.append({ move(name), { filename, entry.name->position().start_line.line_number, entry.name->position().start_line.line_column }, GUI::AutocompleteProvider::DeclarationType::Variable, {} });
                }
            }
            ::Shell::AST::NodeVisitor::visit(node);
        }

        void visit(const ::Shell::AST::FunctionDeclaration* node) override
        {
            dbgln("Found function {}", node->name().name);
            declarations.append({ node->name().name, { filename, node->position().start_line.line_number, node->position().start_line.line_column }, GUI::AutocompleteProvider::DeclarationType::Function, {} });
        }

        const String& filename;
        Vector<GUI::AutocompleteProvider::Declaration> declarations;
    } visitor { document.filename };

    document.node->visit(visitor);

    set_declarations_of_document(document.filename, move(visitor.declarations));
}
}
