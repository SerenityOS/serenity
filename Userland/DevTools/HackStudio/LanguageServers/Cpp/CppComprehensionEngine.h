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
    CppComprehensionEngine(const FileDB& filedb);

    virtual Vector<GUI::AutocompleteProvider::Entry> get_suggestions(const String& file, const GUI::TextPosition& autocomplete_position) override;
    virtual void on_edit(const String& file) override;
    virtual void file_opened([[maybe_unused]] const String& file) override;
    virtual Optional<GUI::AutocompleteProvider::ProjectLocation> find_declaration_of(const String& filename, const GUI::TextPosition& identifier_position) override;
    virtual Optional<FunctionParamsHint> get_function_params_hint(const String&, const GUI::TextPosition&) override;

private:
    struct SymbolName {
        StringView name;
        Vector<StringView> scope;

        static SymbolName create(StringView, Vector<StringView>&&);
        static SymbolName create(StringView);
        String scope_as_string() const;
        String to_string() const;

        bool operator==(const SymbolName&) const = default;
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
        static Symbol create(StringView name, const Vector<StringView>& scope, NonnullRefPtr<Declaration>, IsLocal is_local);
    };

    friend Traits<SymbolName>;

    struct DocumentData {
        const String& filename() const { return m_filename; }
        const String& text() const { return m_text; }
        const Preprocessor& preprocessor() const
        {
            VERIFY(m_preprocessor);
            return *m_preprocessor;
        }
        Preprocessor& preprocessor()
        {
            VERIFY(m_preprocessor);
            return *m_preprocessor;
        }
        const Parser& parser() const
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

    Vector<GUI::AutocompleteProvider::Entry> autocomplete_property(const DocumentData&, const MemberExpression&, const String partial_text) const;
    Vector<GUI::AutocompleteProvider::Entry> autocomplete_name(const DocumentData&, const ASTNode&, const String& partial_text) const;
    String type_of(const DocumentData&, const Expression&) const;
    String type_of_property(const DocumentData&, const Identifier&) const;
    String type_of_variable(const Identifier&) const;
    bool is_property(const ASTNode&) const;
    RefPtr<Declaration> find_declaration_of(const DocumentData&, const ASTNode&) const;
    RefPtr<Declaration> find_declaration_of(const DocumentData&, const SymbolName&) const;

    enum class RecurseIntoScopes {
        No,
        Yes
    };

    Vector<Symbol> properties_of_type(const DocumentData& document, const String& type) const;
    Vector<Symbol> get_child_symbols(const ASTNode&) const;
    Vector<Symbol> get_child_symbols(const ASTNode&, const Vector<StringView>& scope, Symbol::IsLocal) const;

    const DocumentData* get_document_data(const String& file) const;
    const DocumentData* get_or_create_document_data(const String& file);
    void set_document_data(const String& file, OwnPtr<DocumentData>&& data);

    OwnPtr<DocumentData> create_document_data_for(const String& file);
    String document_path_from_include_path(StringView include_path) const;
    void update_declared_symbols(DocumentData&);
    void update_todo_entries(DocumentData&);
    GUI::AutocompleteProvider::DeclarationType type_of_declaration(const Declaration&);
    Vector<StringView> scope_of_node(const ASTNode&) const;
    Vector<StringView> scope_of_reference_to_symbol(const ASTNode&) const;

    Optional<GUI::AutocompleteProvider::ProjectLocation> find_preprocessor_definition(const DocumentData&, const GUI::TextPosition&);

    OwnPtr<DocumentData> create_document_data(String&& text, const String& filename);
    Optional<Vector<GUI::AutocompleteProvider::Entry>> try_autocomplete_property(const DocumentData&, const ASTNode&, Optional<Token> containing_token) const;
    Optional<Vector<GUI::AutocompleteProvider::Entry>> try_autocomplete_name(const DocumentData&, const ASTNode&, Optional<Token> containing_token) const;
    Optional<Vector<GUI::AutocompleteProvider::Entry>> try_autocomplete_include(const DocumentData&, Token include_path_token, Cpp::Position const& cursor_position) const;
    static bool is_symbol_available(const Symbol&, const Vector<StringView>& current_scope, const Vector<StringView>& reference_scope);
    Optional<FunctionParamsHint> get_function_params_hint(DocumentData const&, FunctionCall&, size_t argument_index);

    template<typename Func>
    void for_each_available_symbol(const DocumentData&, Func) const;

    template<typename Func>
    void for_each_included_document_recursive(const DocumentData&, Func) const;

    HashMap<String, OwnPtr<DocumentData>> m_documents;

    // A document's path will be in this set if we're currently processing it.
    // A document is added to this set when we start processing it (e.g because it was #included) and removed when we're done.
    // We use this to prevent circular #includes from looping indefinitely.
    HashTable<String> m_unfinished_documents;
};

template<typename Func>
void CppComprehensionEngine::for_each_available_symbol(const DocumentData& document, Func func) const
{
    for (auto& item : document.m_symbols) {
        auto decision = func(item.value);
        if (decision == IterationDecision::Break)
            return;
    }

    for_each_included_document_recursive(document, [&](const DocumentData& document) {
        for (auto& item : document.m_symbols) {
            auto decision = func(item.value);
            if (decision == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
}

template<typename Func>
void CppComprehensionEngine::for_each_included_document_recursive(const DocumentData& document, Func func) const
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
    static unsigned hash(const LanguageServers::Cpp::CppComprehensionEngine::SymbolName& key)
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
