/*
 * Copyright (c) 2021-2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Function.h>
#include <AK/Vector.h>
#include <DevTools/HackStudio/AutoCompleteResponse.h>
#include <DevTools/HackStudio/LanguageServers/FileDB.h>
#include <LibCpp/AST.h>
#include <LibCpp/Parser.h>
#include <LibCpp/Preprocessor.h>
#include <LibGUI/TextPosition.h>
#include <Libraries/LibCodeComprehension/CodeComprehensionEngine.h>

namespace CodeComprehension::Cpp {

using namespace ::Cpp;

class CppComprehensionEngine : public CodeComprehensionEngine {
public:
    CppComprehensionEngine(FileDB const& filedb);

    virtual Vector<CodeComprehension::AutocompleteResultEntry> get_suggestions(ByteString const& file, GUI::TextPosition const& autocomplete_position) override;
    virtual void on_edit(ByteString const& file) override;
    virtual void file_opened([[maybe_unused]] ByteString const& file) override;
    virtual Optional<CodeComprehension::ProjectLocation> find_declaration_of(ByteString const& filename, GUI::TextPosition const& identifier_position) override;
    virtual Optional<FunctionParamsHint> get_function_params_hint(ByteString const&, GUI::TextPosition const&) override;
    virtual Vector<CodeComprehension::TokenInfo> get_tokens_info(ByteString const& filename) override;

private:
    struct SymbolName {
        StringView name;
        Vector<StringView> scope;

        static SymbolName create(StringView, Vector<StringView>&&);
        static SymbolName create(StringView);
        ByteString scope_as_string() const;
        ByteString to_byte_string() const;

        bool operator==(SymbolName const&) const = default;
    };

    struct Symbol {
        SymbolName name;
        NonnullRefPtr<Cpp::Declaration const> declaration;

        // Local symbols are symbols that should not appear in a global symbol search.
        // For example, a variable that is declared inside a function will have is_local = true.
        bool is_local { false };

        enum class IsLocal {
            No,
            Yes
        };
        static Symbol create(StringView name, Vector<StringView> const& scope, NonnullRefPtr<Cpp::Declaration const>, IsLocal is_local);
    };

    friend Traits<SymbolName>;

    struct DocumentData {
        ByteString const& filename() const { return m_filename; }
        ByteString const& text() const { return m_text; }
        Preprocessor const& preprocessor() const
        {
            VERIFY(m_preprocessor);
            return *m_preprocessor;
        }
        Preprocessor& preprocessor()
        {
            VERIFY(m_preprocessor);
            return *m_preprocessor;
        }
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

        ByteString m_filename;
        ByteString m_text;
        OwnPtr<Preprocessor> m_preprocessor;
        OwnPtr<Parser> m_parser;

        HashMap<SymbolName, Symbol> m_symbols;
        HashTable<ByteString> m_available_headers;
    };

    Vector<CodeComprehension::AutocompleteResultEntry> autocomplete_property(DocumentData const&, MemberExpression const&, ByteString const partial_text) const;
    Vector<AutocompleteResultEntry> autocomplete_name(DocumentData const&, ASTNode const&, ByteString const& partial_text) const;
    ByteString type_of(DocumentData const&, Expression const&) const;
    ByteString type_of_property(DocumentData const&, Identifier const&) const;
    ByteString type_of_variable(Identifier const&) const;
    bool is_property(ASTNode const&) const;
    RefPtr<Cpp::Declaration const> find_declaration_of(DocumentData const&, ASTNode const&) const;
    RefPtr<Cpp::Declaration const> find_declaration_of(DocumentData const&, SymbolName const&) const;
    RefPtr<Cpp::Declaration const> find_declaration_of(DocumentData const&, const GUI::TextPosition& identifier_position);

    enum class RecurseIntoScopes {
        No,
        Yes
    };

    Vector<Symbol> properties_of_type(DocumentData const& document, ByteString const& type) const;
    Vector<Symbol> get_child_symbols(ASTNode const&) const;
    Vector<Symbol> get_child_symbols(ASTNode const&, Vector<StringView> const& scope, Symbol::IsLocal) const;

    DocumentData const* get_document_data(ByteString const& file) const;
    DocumentData const* get_or_create_document_data(ByteString const& file);
    void set_document_data(ByteString const& file, OwnPtr<DocumentData>&& data);

    OwnPtr<DocumentData> create_document_data_for(ByteString const& file);
    ByteString document_path_from_include_path(StringView include_path) const;
    void update_declared_symbols(DocumentData&);
    void update_todo_entries(DocumentData&);
    CodeComprehension::DeclarationType type_of_declaration(Cpp::Declaration const&);
    Vector<StringView> scope_of_node(ASTNode const&) const;
    Vector<StringView> scope_of_reference_to_symbol(ASTNode const&) const;

    Optional<CodeComprehension::ProjectLocation> find_preprocessor_definition(DocumentData const&, const GUI::TextPosition&);
    Optional<Cpp::Preprocessor::Substitution> find_preprocessor_substitution(DocumentData const&, Cpp::Position const&);

    OwnPtr<DocumentData> create_document_data(ByteString text, ByteString const& filename);
    Optional<Vector<CodeComprehension::AutocompleteResultEntry>> try_autocomplete_property(DocumentData const&, ASTNode const&, Optional<Token> containing_token) const;
    Optional<Vector<CodeComprehension::AutocompleteResultEntry>> try_autocomplete_name(DocumentData const&, ASTNode const&, Optional<Token> containing_token) const;
    Optional<Vector<CodeComprehension::AutocompleteResultEntry>> try_autocomplete_include(DocumentData const&, Token include_path_token, Cpp::Position const& cursor_position) const;
    static bool is_symbol_available(Symbol const&, Vector<StringView> const& current_scope, Vector<StringView> const& reference_scope);
    Optional<FunctionParamsHint> get_function_params_hint(DocumentData const&, FunctionCall const&, size_t argument_index);

    template<typename Func>
    void for_each_available_symbol(DocumentData const&, Func) const;

    template<typename Func>
    void for_each_included_document_recursive(DocumentData const&, Func) const;

    CodeComprehension::TokenInfo::SemanticType get_token_semantic_type(DocumentData const&, Token const&);
    CodeComprehension::TokenInfo::SemanticType get_semantic_type_for_identifier(DocumentData const&, Position);

    HashMap<ByteString, OwnPtr<DocumentData>> m_documents;

    // A document's path will be in this set if we're currently processing it.
    // A document is added to this set when we start processing it (e.g because it was #included) and removed when we're done.
    // We use this to prevent circular #includes from looping indefinitely.
    HashTable<ByteString> m_unfinished_documents;
};

template<typename Func>
void CppComprehensionEngine::for_each_available_symbol(DocumentData const& document, Func func) const
{
    for (auto& item : document.m_symbols) {
        auto decision = func(item.value);
        if (decision == IterationDecision::Break)
            return;
    }

    for_each_included_document_recursive(document, [&](DocumentData const& document) {
        for (auto& item : document.m_symbols) {
            auto decision = func(item.value);
            if (decision == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
}

template<typename Func>
void CppComprehensionEngine::for_each_included_document_recursive(DocumentData const& document, Func func) const
{
    for (auto& included_path : document.m_available_headers) {
        auto* included_document = get_document_data(included_path);
        if (!included_document)
            continue;
        auto decision = func(*included_document);
        if (decision == IterationDecision::Break)
            continue;
    }
}
}

namespace AK {

template<>
struct Traits<CodeComprehension::Cpp::CppComprehensionEngine::SymbolName> : public DefaultTraits<CodeComprehension::Cpp::CppComprehensionEngine::SymbolName> {
    static unsigned hash(CodeComprehension::Cpp::CppComprehensionEngine::SymbolName const& key)
    {
        unsigned hash = 0;
        hash = pair_int_hash(hash, string_hash(key.name.characters_without_null_termination(), key.name.length()));
        for (auto& scope_part : key.scope) {
            hash = pair_int_hash(hash, string_hash(scope_part.characters_without_null_termination(), scope_part.length()));
        }
        return hash;
    }
};

}
