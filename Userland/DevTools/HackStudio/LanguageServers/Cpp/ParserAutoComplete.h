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
#include <DevTools/HackStudio/LanguageServers/AutoCompleteEngine.h>
#include <DevTools/HackStudio/LanguageServers/FileDB.h>
#include <LibCpp/AST.h>
#include <LibCpp/Parser.h>
#include <LibCpp/Preprocessor.h>
#include <LibGUI/TextPosition.h>

namespace LanguageServers::Cpp {

using namespace ::Cpp;

class ParserAutoComplete : public AutoCompleteEngine {
public:
    ParserAutoComplete(ClientConnection&, const FileDB& filedb);

    virtual Vector<GUI::AutocompleteProvider::Entry> get_suggestions(const String& file, const GUI::TextPosition& autocomplete_position) override;
    virtual void on_edit(const String& file) override;
    virtual void file_opened([[maybe_unused]] const String& file) override;
    virtual Optional<GUI::AutocompleteProvider::ProjectLocation> find_declaration_of(const String& filename, const GUI::TextPosition& identifier_position) override;

private:
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
    };

    Vector<GUI::AutocompleteProvider::Entry> autocomplete_property(const DocumentData&, const MemberExpression&, const String partial_text) const;
    Vector<GUI::AutocompleteProvider::Entry> autocomplete_name(const DocumentData&, const ASTNode&, const String& partial_text) const;
    String type_of(const DocumentData&, const Expression&) const;
    String type_of_property(const DocumentData&, const Identifier&) const;
    String type_of_variable(const Identifier&) const;
    bool is_property(const ASTNode&) const;
    bool is_empty_property(const DocumentData&, const ASTNode&, const Position& autocomplete_position) const;
    RefPtr<Declaration> find_declaration_of(const DocumentData&, const ASTNode&) const;
    NonnullRefPtrVector<Declaration> get_available_declarations(const DocumentData&, const ASTNode&) const;

    struct PropertyInfo {
        StringView name;
        RefPtr<Type> type;
    };
    Vector<PropertyInfo> properties_of_type(const DocumentData& document, const String& type) const;
    NonnullRefPtrVector<Declaration> get_global_declarations_including_headers(const DocumentData& document) const;
    NonnullRefPtrVector<Declaration> get_global_declarations(const ASTNode& node) const;

    const DocumentData* get_document_data(const String& file) const;
    const DocumentData* get_or_create_document_data(const String& file);
    void set_document_data(const String& file, OwnPtr<DocumentData>&& data);

    OwnPtr<DocumentData> create_document_data_for(const String& file);
    String document_path_from_include_path(const StringView& include_path) const;
    void update_declared_symbols(const DocumentData&);
    GUI::AutocompleteProvider::DeclarationType type_of_declaration(const Declaration&);
    String scope_of_declaration(const Declaration&);
    Optional<GUI::AutocompleteProvider::ProjectLocation> find_preprocessor_definition(const DocumentData&, const GUI::TextPosition&);

    OwnPtr<DocumentData> create_document_data(String&& text, const String& filename);

    HashMap<String, OwnPtr<DocumentData>> m_documents;
};

}
