/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <DevTools/HackStudio/AutoCompleteResponse.h>
#include <DevTools/HackStudio/LanguageServers/CodeComprehensionEngine.h>
#include <DevTools/HackStudio/LanguageServers/FileDB.h>
#include <LibCpp/AST.h>
#include <LibCpp/Parser.h>
#include <LibCpp/Preprocessor.h>
#include <LibGUI/TextPosition.h>

namespace LanguageServers::Cpp {

using namespace ::Cpp;

class CppComprehensionEngine : public CodeComprehensionEngine {
public:
    CppComprehensionEngine(FileDB const& filedb);

    virtual Vector<GUI::AutocompleteProvider::Entry> get_suggestions(String const& file, const GUI::TextPosition& autocomplete_position) override;
    virtual void on_edit(String const& file) override;
    virtual void file_opened([[maybe_unused]] String const& file) override;
    virtual Optional<GUI::AutocompleteProvider::ProjectLocation> find_declaration_of(String const& filename, const GUI::TextPosition& identifier_position) override;
    virtual Optional<FunctionParamsHint> get_function_params_hint(String const&, const GUI::TextPosition&) override;
    virtual Vector<GUI::AutocompleteProvider::TokenInfo> get_tokens_info(String const& filename) override;

private:
    struct SymbolName {
        StringView name;
        Vector<StringView> scope;

        static SymbolName create(StringView, Vector<StringView>&&);
        static SymbolName create(StringView);
        String scope_as_string() const;
        String to_string() const;

        bool operator==(SymbolName const&) const = default;
    };

    struct Symbol {
        SymbolName name;
        NonnullRefPtr<Declaration> declaration;

        // Local symbols are symbols that should not appear in a global symbol search.
        // For example, a variable that is declared inside a function will have is_local = true.
        bool is_local { false };

        enum class IsLocal {
            No,
            Yes
        };
        static Symbol create(StringView name, Vector<StringView> const& scope, NonnullRefPtr<Declaration>, IsLocal is_local);
    };

    friend Traits<SymbolName>;

    struct DocumentData {
        String const& filename() const { return m_filename; }
        String const& text() const { return m_text; }
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

        String m_filename;
        String m_text;
        OwnPtr<Preprocessor> m_preprocessor;
        OwnPtr<Parser> m_parser;

        HashMap<SymbolName, Symbol> m_symbols;
        HashTable<String> m_available_headers;
    };

    Vector<GUI::AutocompleteProvider::Entry> autocomplete_property(DocumentData const&, MemberExpression const&, const String partial_text) const;
    Vector<GUI::AutocompleteProvider::Entry> autocomplete_name(DocumentData const&, ASTNode const&, String const& partial_text) const;
    String type_of(DocumentData const&, Expression const&) const;
    String type_of_property(DocumentData const&, Identifier const&) const;
    String type_of_variable(Identifier const&) const;
    bool is_property(ASTNode const&) const;
    RefPtr<Declaration> find_declaration_of(DocumentData const&, ASTNode const&) const;
    RefPtr<Declaration> find_declaration_of(DocumentData const&, SymbolName const&) const;
    RefPtr<Declaration> find_declaration_of(DocumentData const&, const GUI::TextPosition& identifier_position);

    enum class RecurseIntoScopes {
        No,
        Yes
    };

    Vector<Symbol> properties_of_type(DocumentData const& document, String const& type) const;
    Vector<Symbol> get_child_symbols(ASTNode const&) const;
    Vector<Symbol> get_child_symbols(ASTNode const&, Vector<StringView> const& scope, Symbol::IsLocal) const;

    DocumentData const* get_document_data(String const& file) const;
    DocumentData const* get_or_create_document_data(String const& file);
    void set_document_data(String const& file, OwnPtr<DocumentData>&& data);

    OwnPtr<DocumentData> create_document_data_for(String const& file);
    String document_path_from_include_path(StringView include_path) const;
    void update_declared_symbols(DocumentData&);
    void update_todo_entries(DocumentData&);
    GUI::AutocompleteProvider::DeclarationType type_of_declaration(Declaration const&);
    Vector<StringView> scope_of_node(ASTNode const&) const;
    Vector<StringView> scope_of_reference_to_symbol(ASTNode const&) const;

    Optional<GUI::AutocompleteProvider::ProjectLocation> find_preprocessor_definition(DocumentData const&, const GUI::TextPosition&);
    Optional<Cpp::Preprocessor::Substitution> find_preprocessor_substitution(DocumentData const&, Cpp::Position const&);

    OwnPtr<DocumentData> create_document_data(String&& text, String const& filename);
    Optional<Vector<GUI::AutocompleteProvider::Entry>> try_autocomplete_property(DocumentData const&, ASTNode const&, Optional<Token> containing_token) const;
    Optional<Vector<GUI::AutocompleteProvider::Entry>> try_autocomplete_name(DocumentData const&, ASTNode const&, Optional<Token> containing_token) const;
    Optional<Vector<GUI::AutocompleteProvider::Entry>> try_autocomplete_include(DocumentData const&, Token include_path_token, Cpp::Position const& cursor_position) const;
    static bool is_symbol_available(Symbol const&, Vector<StringView> const& current_scope, Vector<StringView> const& reference_scope);
    Optional<FunctionParamsHint> get_function_params_hint(DocumentData const&, FunctionCall&, size_t argument_index);

    template<typename Func>
    void for_each_available_symbol(DocumentData const&, Func) const;

    template<typename Func>
    void for_each_included_document_recursive(DocumentData const&, Func) const;

    GUI::AutocompleteProvider::TokenInfo::SemanticType get_token_semantic_type(DocumentData const&, Token const&);
    GUI::AutocompleteProvider::TokenInfo::SemanticType get_semantic_type_for_identifier(DocumentData const&, Position);

    HashMap<String, OwnPtr<DocumentData>> m_documents;

    // A document's path will be in this set if we're currently processing it.
    // A document is added to this set when we start processing it (e.g because it was #included) and removed when we're done.
    // We use this to prevent circular #includes from looping indefinitely.
    HashTable<String> m_unfinished_documents;
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
struct Traits<LanguageServers::Cpp::CppComprehensionEngine::SymbolName> : public GenericTraits<LanguageServers::Cpp::CppComprehensionEngine::SymbolName> {
    static unsigned hash(LanguageServers::Cpp::CppComprehensionEngine::SymbolName const& key)
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
