/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DeclarationsModel.h"
#include "ProjectDeclarations.h"
#include <AK/FuzzyMatch.h>

namespace HackStudio {

static String qualified_symbol_name(CodeComprehension::Declaration symbol)
{
    if (!symbol.scope.is_empty())
        return MUST(String::from_byte_string(symbol.name));
    return MUST(String::formatted("{}::{}", symbol.scope, symbol.name));
}

Declaration Declaration::create_filename(ByteString const& filename)
{
    Declaration s;
    s.as_filename = filename;
    return s;
}
Declaration Declaration::create_symbol_declaration(CodeComprehension::Declaration const& decl)
{
    Declaration s;
    s.as_symbol_declaration = decl;
    return s;
}

GUI::Variant DeclarationsModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto& suggestion = m_declarations.at(index.row());
    if (role != GUI::ModelRole::Display)
        return {};

    if (suggestion.is_filename()) {
        if (index.column() == Column::Name)
            return suggestion.as_filename.value();
        if (index.column() == Column::Filename)
            return "";
        if (index.column() == Column::Icon)
            return GUI::FileIconProvider::icon_for_path(suggestion.as_filename.value());
    }
    if (suggestion.is_symbol_declaration()) {
        if (index.column() == Column::Name)
            return qualified_symbol_name(suggestion.as_symbol_declaration.value());
        if (index.column() == Column::Filename)
            return suggestion.as_symbol_declaration.value().position.file;
        if (index.column() == Column::Icon) {
            auto icon = ProjectDeclarations::get_icon_for(suggestion.as_symbol_declaration.value().type);
            if (icon.has_value())
                return icon.value();
            return {};
        }
    }

    return {};
}

GUI::Model::MatchResult DeclarationsModel::data_matches(GUI::ModelIndex const& index, GUI::Variant const& term) const
{
    if (index.row() < 0 || (size_t)index.row() >= m_declarations.size())
        return { TriState::False };

    auto needle = term.as_string();
    if (needle.is_empty())
        return { TriState::True };

    auto& declaration = m_declarations[index.row()];
    if (declaration.is_filename()) {
        if (auto match_result = fuzzy_match(needle, *declaration.as_filename); match_result.matched)
            return { TriState::True, match_result.score };
        return { TriState::False };
    }
    if (declaration.is_symbol_declaration()) {
        auto& symbol = *declaration.as_symbol_declaration;
        auto haystack = qualified_symbol_name(symbol);
        if (auto match_result = fuzzy_match(needle, haystack); match_result.matched)
            return { TriState::True, match_result.score };
        return { TriState::False };
    }

    return { TriState::False };
}

void DeclarationsModel::set_declarations(Vector<HackStudio::Declaration>&& declarations)
{
    m_declarations = move(declarations);
    did_update();
}

}
