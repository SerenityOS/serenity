/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ShellComprehensionEngine.h"
#include <AK/Assertions.h>
#include <AK/HashTable.h>
#include <LibRegex/Regex.h>

namespace CodeComprehension::Shell {

RefPtr<::Shell::Shell> ShellComprehensionEngine::s_shell {};

ShellComprehensionEngine::ShellComprehensionEngine(FileDB const& filedb)
    : CodeComprehensionEngine(filedb, true)
{
}

ShellComprehensionEngine::DocumentData const& ShellComprehensionEngine::get_or_create_document_data(ByteString const& file)
{
    auto absolute_path = filedb().to_absolute_path(file);
    if (!m_documents.contains(absolute_path)) {
        set_document_data(absolute_path, create_document_data_for(absolute_path));
    }
    return get_document_data(absolute_path);
}

ShellComprehensionEngine::DocumentData const& ShellComprehensionEngine::get_document_data(ByteString const& file) const
{
    auto absolute_path = filedb().to_absolute_path(file);
    auto document_data = m_documents.get(absolute_path);
    VERIFY(document_data.has_value());
    return *document_data.value();
}

OwnPtr<ShellComprehensionEngine::DocumentData> ShellComprehensionEngine::create_document_data_for(ByteString const& file)
{
    auto document = filedb().get_or_read_from_filesystem(file);
    if (!document.has_value())
        return {};

    auto content = document.value();
    auto document_data = make<DocumentData>(move(content), file);
    for (auto& path : document_data->sourced_paths())
        get_or_create_document_data(path);

    update_declared_symbols(*document_data);
    return document_data;
}

void ShellComprehensionEngine::set_document_data(ByteString const& file, OwnPtr<DocumentData>&& data)
{
    m_documents.set(filedb().to_absolute_path(file), move(data));
}

ShellComprehensionEngine::DocumentData::DocumentData(ByteString&& _text, ByteString _filename)
    : filename(move(_filename))
    , text(move(_text))
    , node(parse())
{
}

Vector<ByteString> const& ShellComprehensionEngine::DocumentData::sourced_paths() const
{
    if (all_sourced_paths.has_value())
        return all_sourced_paths.value();

    struct : public ::Shell::AST::NodeVisitor {
        void visit(::Shell::AST::CastToCommand const* node) override
        {
            auto& inner = node->inner();
            if (inner->is_list()) {
                if (auto* list = dynamic_cast<::Shell::AST::ListConcatenate const*>(inner.ptr())) {
                    auto& entries = list->list();
                    if (entries.size() == 2 && entries.first()->is_bareword() && static_ptr_cast<::Shell::AST::BarewordLiteral>(entries.first())->text() == "source") {
                        auto& filename = entries[1];
                        if (filename->would_execute())
                            return;
                        auto name_list_node = const_cast<::Shell::AST::Node*>(filename.ptr())->run(nullptr).release_value_but_fixme_should_propagate_errors();
                        auto name_list = name_list_node->resolve_as_list(nullptr).release_value_but_fixme_should_propagate_errors();
                        StringBuilder builder;
                        builder.join(' ', name_list);
                        sourced_files.set(builder.to_byte_string());
                    }
                }
            }
            ::Shell::AST::NodeVisitor::visit(node);
        }

        HashTable<ByteString> sourced_files;
    } visitor;

    node->visit(visitor);

    Vector<ByteString> sourced_paths;
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

    return ::Shell::AST::make_ref_counted<::Shell::AST::SyntaxError>(::Shell::AST::Position {}, "Unable to parse file"_string);
}

size_t ShellComprehensionEngine::resolve(ShellComprehensionEngine::DocumentData const& document, const GUI::TextPosition& position)
{
    size_t offset = 0;

    if (position.line() > 0) {
        auto first = true;
        size_t line = 0;
        for (auto& line_view : document.text.split_limit('\n', position.line() + 1, SplitBehavior::KeepEmpty)) {
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

Vector<CodeComprehension::AutocompleteResultEntry> ShellComprehensionEngine::get_suggestions(ByteString const& file, const GUI::TextPosition& position)
{
    dbgln_if(SH_LANGUAGE_SERVER_DEBUG, "ShellComprehensionEngine position {}:{}", position.line(), position.column());

    auto const& document = get_or_create_document_data(file);
    size_t offset_in_file = resolve(document, position);

    ::Shell::AST::HitTestResult hit_test = document.node->hit_test_position(offset_in_file);
    if (!hit_test.matching_node) {
        dbgln_if(SH_LANGUAGE_SERVER_DEBUG, "no node at position {}:{}", position.line(), position.column());
        return {};
    }

    auto completions = const_cast<::Shell::AST::Node*>(document.node.ptr())->complete_for_editor(shell(), offset_in_file, hit_test).release_value_but_fixme_should_propagate_errors();
    Vector<CodeComprehension::AutocompleteResultEntry> entries;
    for (auto& completion : completions)
        entries.append({ completion.text_string(), completion.input_offset });

    return entries;
}

void ShellComprehensionEngine::on_edit(ByteString const& file)
{
    set_document_data(file, create_document_data_for(file));
}

void ShellComprehensionEngine::file_opened([[maybe_unused]] ByteString const& file)
{
    set_document_data(file, create_document_data_for(file));
}

Optional<CodeComprehension::ProjectLocation> ShellComprehensionEngine::find_declaration_of(ByteString const& filename, const GUI::TextPosition& identifier_position)
{
    dbgln_if(SH_LANGUAGE_SERVER_DEBUG, "find_declaration_of({}, {}:{})", filename, identifier_position.line(), identifier_position.column());
    auto const& document = get_or_create_document_data(filename);
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

    auto name = static_ptr_cast<::Shell::AST::BarewordLiteral const>(result.matching_node)->text();
    auto& declarations = all_declarations();
    for (auto& entry : declarations) {
        for (auto& declaration : entry.value) {
            if (declaration.name.view() == name)
                return declaration.position;
        }
    }

    return {};
}

void ShellComprehensionEngine::update_declared_symbols(DocumentData const& document)
{
    struct Visitor : public ::Shell::AST::NodeVisitor {
        explicit Visitor(ByteString const& filename)
            : filename(filename)
        {
        }

        void visit(::Shell::AST::VariableDeclarations const* node) override
        {
            for (auto& entry : node->variables()) {
                auto literal = entry.name->leftmost_trivial_literal();
                if (!literal)
                    continue;

                ByteString name;
                if (literal->is_bareword())
                    name = static_ptr_cast<::Shell::AST::BarewordLiteral const>(literal)->text().to_byte_string();

                if (!name.is_empty()) {
                    dbgln("Found variable {}", name);
                    declarations.append({ move(name), { filename, entry.name->position().start_line.line_number, entry.name->position().start_line.line_column }, CodeComprehension::DeclarationType::Variable, {} });
                }
            }
            ::Shell::AST::NodeVisitor::visit(node);
        }

        void visit(::Shell::AST::FunctionDeclaration const* node) override
        {
            dbgln("Found function {}", node->name().name);
            declarations.append({ node->name().name.to_byte_string(), { filename, node->position().start_line.line_number, node->position().start_line.line_column }, CodeComprehension::DeclarationType::Function, {} });
        }

        ByteString const& filename;
        Vector<CodeComprehension::Declaration> declarations;
    } visitor { document.filename };

    document.node->visit(visitor);

    set_declarations_of_document(document.filename, move(visitor.declarations));
}
}
