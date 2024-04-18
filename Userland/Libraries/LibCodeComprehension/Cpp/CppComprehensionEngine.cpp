/*
 * Copyright (c) 2021-2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CppComprehensionEngine.h"
#include <AK/Assertions.h>
#include <AK/HashTable.h>
#include <AK/OwnPtr.h>
#include <AK/ScopeGuard.h>
#include <LibCore/DirIterator.h>
#include <LibCpp/AST.h>
#include <LibCpp/Lexer.h>
#include <LibCpp/Parser.h>
#include <LibCpp/Preprocessor.h>
#include <LibFileSystem/FileSystem.h>
#include <LibRegex/Regex.h>
#include <Userland/DevTools/HackStudio/LanguageServers/ConnectionFromClient.h>

namespace CodeComprehension::Cpp {

CppComprehensionEngine::CppComprehensionEngine(FileDB const& filedb)
    : CodeComprehensionEngine(filedb, true)
{
}

CppComprehensionEngine::DocumentData const* CppComprehensionEngine::get_or_create_document_data(ByteString const& file)
{
    auto absolute_path = filedb().to_absolute_path(file);
    if (!m_documents.contains(absolute_path)) {
        set_document_data(absolute_path, create_document_data_for(absolute_path));
    }
    return get_document_data(absolute_path);
}

CppComprehensionEngine::DocumentData const* CppComprehensionEngine::get_document_data(ByteString const& file) const
{
    auto absolute_path = filedb().to_absolute_path(file);
    auto document_data = m_documents.get(absolute_path);
    if (!document_data.has_value())
        return nullptr;
    return document_data.value();
}

OwnPtr<CppComprehensionEngine::DocumentData> CppComprehensionEngine::create_document_data_for(ByteString const& file)
{
    if (m_unfinished_documents.contains(file)) {
        return {};
    }
    m_unfinished_documents.set(file);
    ScopeGuard mark_finished([&file, this]() { m_unfinished_documents.remove(file); });
    auto document = filedb().get_or_read_from_filesystem(file);
    if (!document.has_value())
        return {};
    return create_document_data(move(document.value()), file);
}

void CppComprehensionEngine::set_document_data(ByteString const& file, OwnPtr<DocumentData>&& data)
{
    m_documents.set(filedb().to_absolute_path(file), move(data));
}

Vector<CodeComprehension::AutocompleteResultEntry> CppComprehensionEngine::get_suggestions(ByteString const& file, const GUI::TextPosition& autocomplete_position)
{
    Cpp::Position position { autocomplete_position.line(), autocomplete_position.column() > 0 ? autocomplete_position.column() - 1 : 0 };

    dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "CppComprehensionEngine position {}:{}", position.line, position.column);

    auto const* document_ptr = get_or_create_document_data(file);
    if (!document_ptr)
        return {};

    auto const& document = *document_ptr;
    auto containing_token = document.parser().token_at(position);

    if (containing_token.has_value() && containing_token->type() == Token::Type::IncludePath) {
        auto results = try_autocomplete_include(document, containing_token.value(), position);
        if (results.has_value())
            return results.value();
    }

    auto node = document.parser().node_at(position);
    if (!node) {
        dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "no node at position {}:{}", position.line, position.column);
        return {};
    }

    if (node->parent() && node->parent()->parent())
        dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "node: {}, parent: {}, grandparent: {}", node->class_name(), node->parent()->class_name(), node->parent()->parent()->class_name());

    if (!node->parent())
        return {};

    auto results = try_autocomplete_property(document, *node, containing_token);
    if (results.has_value())
        return results.value();

    results = try_autocomplete_name(document, *node, containing_token);
    if (results.has_value())
        return results.value();
    return {};
}

Optional<Vector<CodeComprehension::AutocompleteResultEntry>> CppComprehensionEngine::try_autocomplete_name(DocumentData const& document, ASTNode const& node, Optional<Token> containing_token) const
{
    auto partial_text = ByteString::empty();
    if (containing_token.has_value() && containing_token.value().type() != Token::Type::ColonColon) {
        partial_text = containing_token.value().text();
    }
    return autocomplete_name(document, node, partial_text);
}

Optional<Vector<CodeComprehension::AutocompleteResultEntry>> CppComprehensionEngine::try_autocomplete_property(DocumentData const& document, ASTNode const& node, Optional<Token> containing_token) const
{
    if (!containing_token.has_value())
        return {};

    if (!node.parent()->is_member_expression())
        return {};

    auto const& parent = static_cast<MemberExpression const&>(*node.parent());

    auto partial_text = ByteString::empty();
    if (containing_token.value().type() != Token::Type::Dot) {
        if (&node != parent.property())
            return {};
        partial_text = containing_token.value().text();
    }

    return autocomplete_property(document, parent, partial_text);
}

Vector<CodeComprehension::AutocompleteResultEntry> CppComprehensionEngine::autocomplete_name(DocumentData const& document, ASTNode const& node, ByteString const& partial_text) const
{
    auto reference_scope = scope_of_reference_to_symbol(node);
    auto current_scope = scope_of_node(node);

    auto symbol_matches = [&](Symbol const& symbol) {
        if (!is_symbol_available(symbol, current_scope, reference_scope)) {
            return false;
        }

        if (!symbol.name.name.starts_with(partial_text))
            return false;

        if (symbol.is_local) {
            // If this symbol was declared below us in a function, it's not available to us.
            bool is_unavailable = symbol.is_local && symbol.declaration->start().line > node.start().line;
            if (is_unavailable)
                return false;
        }

        return true;
    };

    Vector<Symbol> matches;

    for_each_available_symbol(document, [&](Symbol const& symbol) {
        if (symbol_matches(symbol)) {
            matches.append(symbol);
        }
        return IterationDecision::Continue;
    });

    Vector<CodeComprehension::AutocompleteResultEntry> suggestions;
    for (auto& symbol : matches) {
        suggestions.append({ symbol.name.name, partial_text.length() });
    }

    if (reference_scope.is_empty()) {
        for (auto& preprocessor_name : document.preprocessor().definitions().keys()) {
            if (preprocessor_name.starts_with(partial_text)) {
                suggestions.append({ preprocessor_name, partial_text.length() });
            }
        }
    }

    return suggestions;
}

Vector<StringView> CppComprehensionEngine::scope_of_reference_to_symbol(ASTNode const& node) const
{
    Name const* name = nullptr;
    if (node.is_name()) {
        // FIXME It looks like this code path is never taken
        name = reinterpret_cast<Name const*>(&node);
    } else if (node.is_identifier()) {
        auto* parent = node.parent();
        if (!(parent && parent->is_name()))
            return {};
        name = reinterpret_cast<Name const*>(parent);
    } else {
        return {};
    }

    VERIFY(name->is_name());

    Vector<StringView> scope_parts;
    for (auto& scope_part : name->scope()) {
        // If the target node is part of a scope reference, we want to end the scope chain before it.
        if (scope_part == &node)
            break;
        scope_parts.append(scope_part->name());
    }
    return scope_parts;
}

Vector<CodeComprehension::AutocompleteResultEntry> CppComprehensionEngine::autocomplete_property(DocumentData const& document, MemberExpression const& parent, ByteString const partial_text) const
{
    VERIFY(parent.object());
    auto type = type_of(document, *parent.object());
    if (type.is_empty()) {
        dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "Could not infer type of object");
        return {};
    }

    Vector<CodeComprehension::AutocompleteResultEntry> suggestions;
    for (auto& prop : properties_of_type(document, type)) {
        if (prop.name.name.starts_with(partial_text)) {
            suggestions.append({ prop.name.name, partial_text.length() });
        }
    }
    return suggestions;
}

bool CppComprehensionEngine::is_property(ASTNode const& node) const
{
    if (!node.parent()->is_member_expression())
        return false;

    auto& parent = verify_cast<MemberExpression>(*node.parent());
    return parent.property() == &node;
}

ByteString CppComprehensionEngine::type_of_property(DocumentData const& document, Identifier const& identifier) const
{
    auto& parent = verify_cast<MemberExpression>(*identifier.parent());
    VERIFY(parent.object());
    auto properties = properties_of_type(document, type_of(document, *parent.object()));
    for (auto& prop : properties) {
        if (prop.name.name != identifier.name())
            continue;
        Type const* type { nullptr };
        if (prop.declaration->is_variable_declaration()) {
            type = verify_cast<VariableDeclaration>(*prop.declaration).type();
        }
        if (!type)
            continue;
        if (!type->is_named_type())
            continue;

        VERIFY(verify_cast<NamedType>(*type).name());
        if (verify_cast<NamedType>(*type).name())
            return verify_cast<NamedType>(*type).name()->full_name();
        return ByteString::empty();
    }
    return {};
}

ByteString CppComprehensionEngine::type_of_variable(Identifier const& identifier) const
{
    ASTNode const* current = &identifier;
    while (current) {
        for (auto& decl : current->declarations()) {
            if (decl->is_variable_or_parameter_declaration()) {
                auto& var_or_param = verify_cast<VariableOrParameterDeclaration>(*decl);
                if (var_or_param.full_name() == identifier.name() && var_or_param.type()->is_named_type()) {
                    VERIFY(verify_cast<NamedType>(*var_or_param.type()).name());
                    if (verify_cast<NamedType>(*var_or_param.type()).name())
                        return verify_cast<NamedType>(*var_or_param.type()).name()->full_name();
                    return ByteString::empty();
                }
            }
        }
        current = current->parent();
    }
    return {};
}

ByteString CppComprehensionEngine::type_of(DocumentData const& document, Expression const& expression) const
{
    if (expression.is_member_expression()) {
        auto& member_expression = verify_cast<MemberExpression>(expression);
        VERIFY(member_expression.property());
        if (member_expression.property()->is_identifier())
            return type_of_property(document, static_cast<Identifier const&>(*member_expression.property()));
        return {};
    }

    Identifier const* identifier { nullptr };
    if (expression.is_name()) {
        identifier = static_cast<Name const&>(expression).name();
    } else if (expression.is_identifier()) {
        identifier = &static_cast<Identifier const&>(expression);
    } else {
        dbgln("expected identifier or name, got: {}", expression.class_name());
        VERIFY_NOT_REACHED(); // TODO
    }
    VERIFY(identifier);
    if (is_property(*identifier))
        return type_of_property(document, *identifier);

    return type_of_variable(*identifier);
}

Vector<CppComprehensionEngine::Symbol> CppComprehensionEngine::properties_of_type(DocumentData const& document, ByteString const& type) const
{
    auto type_symbol = SymbolName::create(type);
    auto decl = find_declaration_of(document, type_symbol);
    if (!decl) {
        dbgln("Couldn't find declaration of type: {}", type);
        return {};
    }

    if (!decl->is_struct_or_class()) {
        dbgln("Expected declaration of type: {} to be struct or class", type);
        return {};
    }

    auto& struct_or_class = verify_cast<StructOrClassDeclaration>(*decl);
    VERIFY(struct_or_class.full_name() == type_symbol.name);

    Vector<Symbol> properties;
    for (auto& member : struct_or_class.members()) {
        Vector<StringView> scope(type_symbol.scope);
        scope.append(type_symbol.name);
        // FIXME: We don't have to create the Symbol here, it should already exist in the 'm_symbol' table of some DocumentData we already parsed.
        properties.append(Symbol::create(member->full_name(), scope, member, Symbol::IsLocal::No));
    }
    return properties;
}

CppComprehensionEngine::Symbol CppComprehensionEngine::Symbol::create(StringView name, Vector<StringView> const& scope, NonnullRefPtr<Cpp::Declaration const> declaration, IsLocal is_local)
{
    return { { name, scope }, move(declaration), is_local == IsLocal::Yes };
}

Vector<CppComprehensionEngine::Symbol> CppComprehensionEngine::get_child_symbols(ASTNode const& node) const
{
    return get_child_symbols(node, {}, Symbol::IsLocal::No);
}

Vector<CppComprehensionEngine::Symbol> CppComprehensionEngine::get_child_symbols(ASTNode const& node, Vector<StringView> const& scope, Symbol::IsLocal is_local) const
{
    Vector<Symbol> symbols;

    for (auto& decl : node.declarations()) {
        symbols.append(Symbol::create(decl->full_name(), scope, decl, is_local));

        bool should_recurse = decl->is_namespace() || decl->is_struct_or_class() || decl->is_function();
        bool are_child_symbols_local = decl->is_function();

        if (!should_recurse)
            continue;

        auto new_scope = scope;
        new_scope.append(decl->full_name());
        symbols.extend(get_child_symbols(decl, new_scope, are_child_symbols_local ? Symbol::IsLocal::Yes : is_local));
    }

    return symbols;
}

ByteString CppComprehensionEngine::document_path_from_include_path(StringView include_path) const
{
    static Regex<PosixExtended> library_include("<(.+)>");
    static Regex<PosixExtended> user_defined_include("\"(.+)\"");

    auto document_path_for_library_include = [&](StringView include_path) -> ByteString {
        RegexResult result;
        if (!library_include.search(include_path, result))
            return {};

        auto path = result.capture_group_matches.at(0).at(0).view.string_view();
        return ByteString::formatted("/usr/include/{}", path);
    };

    auto document_path_for_user_defined_include = [&](StringView include_path) -> ByteString {
        RegexResult result;
        if (!user_defined_include.search(include_path, result))
            return {};

        return result.capture_group_matches.at(0).at(0).view.string_view();
    };

    auto result = document_path_for_library_include(include_path);
    if (result.is_empty())
        result = document_path_for_user_defined_include(include_path);

    return result;
}

void CppComprehensionEngine::on_edit(ByteString const& file)
{
    set_document_data(file, create_document_data_for(file));
}

void CppComprehensionEngine::file_opened([[maybe_unused]] ByteString const& file)
{
    get_or_create_document_data(file);
}

Optional<CodeComprehension::ProjectLocation> CppComprehensionEngine::find_declaration_of(ByteString const& filename, const GUI::TextPosition& identifier_position)
{
    auto const* document_ptr = get_or_create_document_data(filename);
    if (!document_ptr)
        return {};

    auto const& document = *document_ptr;
    auto decl = find_declaration_of(document, identifier_position);
    if (decl) {
        return CodeComprehension::ProjectLocation { decl->filename(), decl->start().line, decl->start().column };
    }

    return find_preprocessor_definition(document, identifier_position);
}

RefPtr<Cpp::Declaration const> CppComprehensionEngine::find_declaration_of(DocumentData const& document, const GUI::TextPosition& identifier_position)
{
    auto node = document.parser().node_at(Cpp::Position { identifier_position.line(), identifier_position.column() });
    if (!node) {
        dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "no node at position {}:{}", identifier_position.line(), identifier_position.column());
        return {};
    }
    return find_declaration_of(document, *node);
}

Optional<CodeComprehension::ProjectLocation> CppComprehensionEngine::find_preprocessor_definition(DocumentData const& document, const GUI::TextPosition& text_position)
{
    Position cpp_position { text_position.line(), text_position.column() };
    auto substitution = find_preprocessor_substitution(document, cpp_position);
    if (!substitution.has_value())
        return {};
    return CodeComprehension::ProjectLocation { substitution->defined_value.filename, substitution->defined_value.line, substitution->defined_value.column };
}

Optional<Cpp::Preprocessor::Substitution> CppComprehensionEngine::find_preprocessor_substitution(DocumentData const& document, Cpp::Position const& cpp_position)
{
    // Search for a replaced preprocessor token that intersects with text_position
    for (auto& substitution : document.preprocessor().substitutions()) {
        if (substitution.original_tokens.first().start() > cpp_position)
            continue;
        if (substitution.original_tokens.first().end() < cpp_position)
            continue;
        return substitution;
    }
    return {};
}

struct TargetDeclaration {
    enum Type {
        Variable,
        Type,
        Function,
        Property,
        Scope
    } type;
    ByteString name;
};

static Optional<TargetDeclaration> get_target_declaration(ASTNode const& node, ByteString name);
static Optional<TargetDeclaration> get_target_declaration(ASTNode const& node)
{
    if (node.is_identifier()) {
        return get_target_declaration(node, static_cast<Identifier const&>(node).name());
    }

    if (node.is_declaration()) {
        return get_target_declaration(node, verify_cast<Cpp::Declaration>(node).full_name());
    }

    if (node.is_type() && node.parent() && node.parent()->is_declaration()) {
        return get_target_declaration(*node.parent(), verify_cast<Cpp::Declaration>(node.parent())->full_name());
    }

    dbgln("get_target_declaration: Invalid argument node of type: {}", node.class_name());
    return {};
}

static Optional<TargetDeclaration> get_target_declaration(ASTNode const& node, ByteString name)
{
    if (node.parent() && node.parent()->is_name()) {
        auto& name_node = *verify_cast<Name>(node.parent());
        if (&node != name_node.name()) {
            // Node is part of scope reference chain
            return TargetDeclaration { TargetDeclaration::Type::Scope, name };
        }
        if (name_node.parent() && name_node.parent()->is_declaration()) {
            auto declaration = verify_cast<Cpp::Declaration>(name_node.parent());
            if (declaration->is_struct_or_class() || declaration->is_enum()) {
                return TargetDeclaration { TargetDeclaration::Type::Type, name };
            }
            if (declaration->is_function()) {
                return TargetDeclaration { TargetDeclaration::Type::Function, name };
            }
        }
    }

    if ((node.parent() && node.parent()->is_function_call()) || (node.parent()->is_name() && node.parent()->parent() && node.parent()->parent()->is_function_call())) {
        return TargetDeclaration { TargetDeclaration::Type::Function, name };
    }

    if ((node.parent() && node.parent()->is_type()) || (node.parent()->is_name() && node.parent()->parent() && node.parent()->parent()->is_type()))
        return TargetDeclaration { TargetDeclaration::Type::Type, name };

    if ((node.parent() && node.parent()->is_member_expression()))
        return TargetDeclaration { TargetDeclaration::Type::Property, name };

    return TargetDeclaration { TargetDeclaration::Type::Variable, name };
}
RefPtr<Cpp::Declaration const> CppComprehensionEngine::find_declaration_of(DocumentData const& document_data, ASTNode const& node) const
{
    dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "find_declaration_of: {} ({})", document_data.parser().text_of_node(node), node.class_name());

    auto target_decl = get_target_declaration(node);
    if (!target_decl.has_value())
        return {};

    auto reference_scope = scope_of_reference_to_symbol(node);
    auto current_scope = scope_of_node(node);

    auto symbol_matches = [&](Symbol const& symbol) {
        bool match_function = target_decl.value().type == TargetDeclaration::Function && symbol.declaration->is_function();
        bool match_variable = target_decl.value().type == TargetDeclaration::Variable && symbol.declaration->is_variable_declaration();
        bool match_type = target_decl.value().type == TargetDeclaration::Type && (symbol.declaration->is_struct_or_class() || symbol.declaration->is_enum());
        bool match_property = target_decl.value().type == TargetDeclaration::Property && symbol.declaration->parent()->is_declaration() && verify_cast<Cpp::Declaration>(symbol.declaration->parent())->is_struct_or_class();
        bool match_parameter = target_decl.value().type == TargetDeclaration::Variable && symbol.declaration->is_parameter();
        bool match_scope = target_decl.value().type == TargetDeclaration::Scope && (symbol.declaration->is_namespace() || symbol.declaration->is_struct_or_class());

        if (match_property) {
            // FIXME: This is not really correct, we also need to check that the type of the struct/class matches (not just the property name)
            if (symbol.name.name == target_decl.value().name) {
                return true;
            }
        }

        if (!is_symbol_available(symbol, current_scope, reference_scope)) {
            return false;
        }

        if (match_function || match_type || match_scope) {
            if (symbol.name.name == target_decl->name)
                return true;
        }

        if (match_variable || match_parameter) {
            // If this symbol was declared below us in a function, it's not available to us.
            bool is_unavailable = symbol.is_local && symbol.declaration->start().line > node.start().line;

            if (!is_unavailable && (symbol.name.name == target_decl->name)) {
                return true;
            }
        }

        return false;
    };

    Optional<Symbol> match;

    for_each_available_symbol(document_data, [&](Symbol const& symbol) {
        if (symbol_matches(symbol)) {
            match = symbol;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    if (!match.has_value())
        return {};

    return match->declaration;
}

void CppComprehensionEngine::update_declared_symbols(DocumentData& document)
{
    for (auto& symbol : get_child_symbols(*document.parser().root_node())) {
        document.m_symbols.set(symbol.name, move(symbol));
    }

    Vector<CodeComprehension::Declaration> declarations;
    for (auto& symbol_entry : document.m_symbols) {
        auto& symbol = symbol_entry.value;
        declarations.append({ symbol.name.name, { document.filename(), symbol.declaration->start().line, symbol.declaration->start().column }, type_of_declaration(symbol.declaration), symbol.name.scope_as_string() });
    }

    for (auto& definition : document.preprocessor().definitions()) {
        declarations.append({ definition.key, { document.filename(), definition.value.line, definition.value.column }, CodeComprehension::DeclarationType::PreprocessorDefinition, {} });
    }
    set_declarations_of_document(document.filename(), move(declarations));
}

void CppComprehensionEngine::update_todo_entries(DocumentData& document)
{
    set_todo_entries_of_document(document.filename(), document.parser().get_todo_entries());
}

CodeComprehension::DeclarationType CppComprehensionEngine::type_of_declaration(Cpp::Declaration const& decl)
{
    if (decl.is_struct())
        return CodeComprehension::DeclarationType::Struct;
    if (decl.is_class())
        return CodeComprehension::DeclarationType::Class;
    if (decl.is_function())
        return CodeComprehension::DeclarationType::Function;
    if (decl.is_variable_declaration())
        return CodeComprehension::DeclarationType::Variable;
    if (decl.is_namespace())
        return CodeComprehension::DeclarationType::Namespace;
    if (decl.is_member())
        return CodeComprehension::DeclarationType::Member;
    return CodeComprehension::DeclarationType::Variable;
}

OwnPtr<CppComprehensionEngine::DocumentData> CppComprehensionEngine::create_document_data(ByteString text, ByteString const& filename)
{
    auto document_data = make<DocumentData>();
    document_data->m_filename = filename;
    document_data->m_text = move(text);
    document_data->m_preprocessor = make<Preprocessor>(document_data->m_filename, document_data->text());
    document_data->preprocessor().set_ignore_unsupported_keywords(true);
    document_data->preprocessor().set_ignore_invalid_statements(true);
    document_data->preprocessor().set_keep_include_statements(true);

    document_data->preprocessor().definitions_in_header_callback = [this](StringView include_path) -> Preprocessor::Definitions {
        auto included_document = get_or_create_document_data(document_path_from_include_path(include_path));
        if (!included_document)
            return {};

        return included_document->preprocessor().definitions();
    };

    auto tokens = document_data->preprocessor().process_and_lex();

    for (auto include_path : document_data->preprocessor().included_paths()) {
        auto include_fullpath = document_path_from_include_path(include_path);
        auto included_document = get_or_create_document_data(include_fullpath);
        if (!included_document)
            continue;

        document_data->m_available_headers.set(include_fullpath);

        for (auto& header : included_document->m_available_headers)
            document_data->m_available_headers.set(header);
    }

    document_data->m_parser = make<Parser>(move(tokens), filename);

    auto root = document_data->parser().parse();

    if constexpr (CPP_LANGUAGE_SERVER_DEBUG)
        root->dump();

    update_declared_symbols(*document_data);
    update_todo_entries(*document_data);

    return document_data;
}

Vector<StringView> CppComprehensionEngine::scope_of_node(ASTNode const& node) const
{

    auto parent = node.parent();
    if (!parent)
        return {};

    auto parent_scope = scope_of_node(*parent);

    if (!parent->is_declaration())
        return parent_scope;

    auto& parent_decl = static_cast<Cpp::Declaration const&>(*parent);

    StringView containing_scope;
    if (parent_decl.is_namespace())
        containing_scope = static_cast<NamespaceDeclaration const&>(parent_decl).full_name();
    if (parent_decl.is_struct_or_class())
        containing_scope = static_cast<StructOrClassDeclaration const&>(parent_decl).full_name();
    if (parent_decl.is_function())
        containing_scope = static_cast<FunctionDeclaration const&>(parent_decl).full_name();

    parent_scope.append(containing_scope);
    return parent_scope;
}

Optional<Vector<CodeComprehension::AutocompleteResultEntry>> CppComprehensionEngine::try_autocomplete_include(DocumentData const&, Token include_path_token, Cpp::Position const& cursor_position) const
{
    VERIFY(include_path_token.type() == Token::Type::IncludePath);
    auto partial_include = include_path_token.text().trim_whitespace();

    enum IncludeType {
        Project,
        System,
    } include_type { Project };

    ByteString include_root;
    bool already_has_suffix = false;
    if (partial_include.starts_with('<')) {
        include_root = "/usr/include/";
        include_type = System;
        if (partial_include.ends_with('>')) {
            already_has_suffix = true;
            partial_include = partial_include.substring_view(0, partial_include.length() - 1).trim_whitespace();
        }
    } else if (partial_include.starts_with('"')) {
        include_root = filedb().project_root().value_or("");
        if (partial_include.length() > 1 && partial_include.ends_with('\"')) {
            already_has_suffix = true;
            partial_include = partial_include.substring_view(0, partial_include.length() - 1).trim_whitespace();
        }
    } else
        return {};

    // The cursor is past the end of the <> or "", and so should not trigger autocomplete.
    if (already_has_suffix && include_path_token.end() <= cursor_position)
        return {};

    auto last_slash = partial_include.find_last('/');
    auto include_dir = ByteString::empty();
    auto partial_basename = partial_include.substring_view((last_slash.has_value() ? last_slash.value() : 0) + 1);
    if (last_slash.has_value()) {
        include_dir = partial_include.substring_view(1, last_slash.value());
    }

    auto full_dir = LexicalPath::join(include_root, include_dir).string();
    dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "searching path: {}, partial_basename: {}", full_dir, partial_basename);

    Core::DirIterator it(full_dir, Core::DirIterator::Flags::SkipDots);
    Vector<CodeComprehension::AutocompleteResultEntry> options;

    auto prefix = include_type == System ? "<" : "\"";
    auto suffix = include_type == System ? ">" : "\"";
    while (it.has_next()) {
        auto path = it.next_path();

        if (!path.starts_with(partial_basename))
            continue;

        if (FileSystem::is_directory(LexicalPath::join(full_dir, path).string())) {
            // FIXME: Don't dismiss the autocomplete when filling these suggestions.
            auto completion = ByteString::formatted("{}{}{}/", prefix, include_dir, path);
            options.empend(completion, include_dir.length() + partial_basename.length() + 1, CodeComprehension::Language::Cpp, path, CodeComprehension::AutocompleteResultEntry::HideAutocompleteAfterApplying::No);
        } else if (path.ends_with(".h"sv)) {
            // FIXME: Place the cursor after the trailing > or ", even if it was
            //        already typed.
            auto completion = ByteString::formatted("{}{}{}{}", prefix, include_dir, path, already_has_suffix ? "" : suffix);
            options.empend(completion, include_dir.length() + partial_basename.length() + 1, CodeComprehension::Language::Cpp, path);
        }
    }

    return options;
}

RefPtr<Cpp::Declaration const> CppComprehensionEngine::find_declaration_of(CppComprehensionEngine::DocumentData const& document, CppComprehensionEngine::SymbolName const& target_symbol_name) const
{
    RefPtr<Cpp::Declaration const> target_declaration;
    for_each_available_symbol(document, [&](Symbol const& symbol) {
        if (symbol.name == target_symbol_name) {
            target_declaration = symbol.declaration;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return target_declaration;
}

ByteString CppComprehensionEngine::SymbolName::scope_as_string() const
{
    if (scope.is_empty())
        return ByteString::empty();

    StringBuilder builder;
    for (size_t i = 0; i < scope.size() - 1; ++i) {
        builder.appendff("{}::", scope[i]);
    }
    builder.append(scope.last());
    return builder.to_byte_string();
}

CppComprehensionEngine::SymbolName CppComprehensionEngine::SymbolName::create(StringView name, Vector<StringView>&& scope)
{
    return { name, move(scope) };
}

CppComprehensionEngine::SymbolName CppComprehensionEngine::SymbolName::create(StringView qualified_name)
{
    auto parts = qualified_name.split_view("::"sv);
    VERIFY(!parts.is_empty());
    auto name = parts.take_last();
    return SymbolName::create(name, move(parts));
}

ByteString CppComprehensionEngine::SymbolName::to_byte_string() const
{
    if (scope.is_empty())
        return name;
    return ByteString::formatted("{}::{}", scope_as_string(), name);
}

bool CppComprehensionEngine::is_symbol_available(Symbol const& symbol, Vector<StringView> const& current_scope, Vector<StringView> const& reference_scope)
{

    if (!reference_scope.is_empty()) {
        return reference_scope == symbol.name.scope;
    }

    // FIXME: Take "using namespace ..." into consideration

    // Check if current_scope starts with symbol's scope
    if (symbol.name.scope.size() > current_scope.size())
        return false;

    for (size_t i = 0; i < symbol.name.scope.size(); ++i) {
        if (current_scope[i] != symbol.name.scope[i])
            return false;
    }

    return true;
}

Optional<CodeComprehensionEngine::FunctionParamsHint> CppComprehensionEngine::get_function_params_hint(ByteString const& filename, const GUI::TextPosition& identifier_position)
{
    auto const* document_ptr = get_or_create_document_data(filename);
    if (!document_ptr)
        return {};

    auto const& document = *document_ptr;
    Cpp::Position cpp_position { identifier_position.line(), identifier_position.column() };
    auto node = document.parser().node_at(cpp_position);
    if (!node) {
        dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "no node at position {}:{}", identifier_position.line(), identifier_position.column());
        return {};
    }

    dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "node type: {}", node->class_name());

    FunctionCall const* call_node { nullptr };

    if (node->is_function_call()) {
        call_node = verify_cast<FunctionCall>(node.ptr());

        auto token = document.parser().token_at(cpp_position);

        // If we're in a function call with 0 arguments
        if (token.has_value() && (token->type() == Token::Type::LeftParen || token->type() == Token::Type::RightParen)) {
            return get_function_params_hint(document, *call_node, call_node->arguments().is_empty() ? 0 : call_node->arguments().size() - 1);
        }
    }

    // Walk upwards in the AST to find a FunctionCall node
    while (!call_node && node) {
        auto parent_is_call = node->parent() && node->parent()->is_function_call();
        if (parent_is_call) {
            call_node = verify_cast<FunctionCall>(node->parent());
            break;
        }
        node = node->parent();
    }

    if (!call_node) {
        dbgln("did not find function call");
        return {};
    }

    Optional<size_t> invoked_arg_index;
    for (size_t arg_index = 0; arg_index < call_node->arguments().size(); ++arg_index) {
        if (call_node->arguments()[arg_index] == node.ptr()) {
            invoked_arg_index = arg_index;
            break;
        }
    }
    if (!invoked_arg_index.has_value()) {
        dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "could not find argument index, defaulting to the last argument");
        invoked_arg_index = call_node->arguments().is_empty() ? 0 : call_node->arguments().size() - 1;
    }

    dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "arg index: {}", invoked_arg_index.value());
    return get_function_params_hint(document, *call_node, invoked_arg_index.value());
}

Optional<CppComprehensionEngine::FunctionParamsHint> CppComprehensionEngine::get_function_params_hint(
    DocumentData const& document,
    FunctionCall const& call_node,
    size_t argument_index)
{
    Identifier const* callee = nullptr;
    VERIFY(call_node.callee());
    if (call_node.callee()->is_identifier()) {
        callee = verify_cast<Identifier>(call_node.callee());
    } else if (call_node.callee()->is_name()) {
        callee = verify_cast<Name>(*call_node.callee()).name();
    } else if (call_node.callee()->is_member_expression()) {
        auto& member_exp = verify_cast<MemberExpression>(*call_node.callee());
        VERIFY(member_exp.property());
        if (member_exp.property()->is_identifier()) {
            callee = verify_cast<Identifier>(member_exp.property());
        }
    }

    if (!callee) {
        dbgln("unexpected node type for function call: {}", call_node.callee()->class_name());
        return {};
    }
    VERIFY(callee);

    auto decl = find_declaration_of(document, *callee);
    if (!decl) {
        dbgln("func decl not found");
        return {};
    }
    if (!decl->is_function()) {
        dbgln("declaration is not a function");
        return {};
    }

    auto& func_decl = verify_cast<FunctionDeclaration>(*decl);
    auto document_of_declaration = get_document_data(func_decl.filename());

    FunctionParamsHint hint {};
    hint.current_index = argument_index;
    for (auto& arg : func_decl.parameters()) {
        Vector<StringView> tokens_text;
        for (auto token : document_of_declaration->parser().tokens_in_range(arg->start(), arg->end())) {
            tokens_text.append(token.text());
        }
        hint.params.append(ByteString::join(' ', tokens_text));
    }

    return hint;
}

Vector<CodeComprehension::TokenInfo> CppComprehensionEngine::get_tokens_info(ByteString const& filename)
{
    dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "CppComprehensionEngine::get_tokens_info: {}", filename);

    auto const* document_ptr = get_or_create_document_data(filename);
    if (!document_ptr)
        return {};

    auto const& document = *document_ptr;

    Vector<CodeComprehension::TokenInfo> tokens_info;
    for (auto const& token : document.preprocessor().unprocessed_tokens()) {

        tokens_info.append({ get_token_semantic_type(document, token),
            token.start().line, token.start().column, token.end().line, token.end().column });
        dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "{}: {}", token.text(), CodeComprehension::TokenInfo::type_to_string(tokens_info.last().type));
    }
    return tokens_info;
}

CodeComprehension::TokenInfo::SemanticType CppComprehensionEngine::get_token_semantic_type(DocumentData const& document, Token const& token)
{
    using GUI::AutocompleteProvider;
    switch (token.type()) {
    case Cpp::Token::Type::Identifier:
        return get_semantic_type_for_identifier(document, token.start());
    case Cpp::Token::Type::Keyword:
        return CodeComprehension::TokenInfo::SemanticType::Keyword;
    case Cpp::Token::Type::KnownType:
        return CodeComprehension::TokenInfo::SemanticType::Type;
    case Cpp::Token::Type::DoubleQuotedString:
    case Cpp::Token::Type::SingleQuotedString:
    case Cpp::Token::Type::RawString:
        return CodeComprehension::TokenInfo::SemanticType::String;
    case Cpp::Token::Type::Integer:
    case Cpp::Token::Type::Float:
        return CodeComprehension::TokenInfo::SemanticType::Number;
    case Cpp::Token::Type::IncludePath:
        return CodeComprehension::TokenInfo::SemanticType::IncludePath;
    case Cpp::Token::Type::EscapeSequence:
        return CodeComprehension::TokenInfo::SemanticType::Keyword;
    case Cpp::Token::Type::PreprocessorStatement:
    case Cpp::Token::Type::IncludeStatement:
        return CodeComprehension::TokenInfo::SemanticType::PreprocessorStatement;
    case Cpp::Token::Type::Comment:
        return CodeComprehension::TokenInfo::SemanticType::Comment;
    default:
        return CodeComprehension::TokenInfo::SemanticType::Unknown;
    }
}

CodeComprehension::TokenInfo::SemanticType CppComprehensionEngine::get_semantic_type_for_identifier(DocumentData const& document, Position position)
{
    if (find_preprocessor_substitution(document, position).has_value())
        return CodeComprehension::TokenInfo::SemanticType::PreprocessorMacro;

    auto decl = find_declaration_of(document, GUI::TextPosition { position.line, position.column });
    if (!decl)
        return CodeComprehension::TokenInfo::SemanticType::Identifier;

    if (decl->is_function())
        return CodeComprehension::TokenInfo::SemanticType::Function;
    if (decl->is_parameter())
        return CodeComprehension::TokenInfo::SemanticType::Parameter;
    if (decl->is_variable_declaration()) {
        if (decl->is_member())
            return CodeComprehension::TokenInfo::SemanticType::Member;
        return CodeComprehension::TokenInfo::SemanticType::Variable;
    }
    if (decl->is_struct_or_class() || decl->is_enum())
        return CodeComprehension::TokenInfo::SemanticType::CustomType;
    if (decl->is_namespace())
        return CodeComprehension::TokenInfo::SemanticType::Namespace;

    return CodeComprehension::TokenInfo::SemanticType::Identifier;
}

}
