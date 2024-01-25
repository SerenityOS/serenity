/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DeclarationsModel.h"
#include "ProjectDeclarations.h"

namespace HackStudio {

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
        if (index.column() == Column::Name) {
            if (!suggestion.as_symbol_declaration.value().scope.is_empty())
                return suggestion.as_symbol_declaration.value().name;
            return ByteString::formatted("{}::{}", suggestion.as_symbol_declaration.value().scope, suggestion.as_symbol_declaration.value().name);
        }
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
        if (declaration.as_filename->contains(needle, CaseSensitivity::CaseInsensitive))
            return { TriState::True };
        return { TriState::False };
    }
    if (declaration.is_symbol_declaration()) {
        if (declaration.as_symbol_declaration->name.contains(needle, CaseSensitivity::CaseInsensitive)
            || declaration.as_symbol_declaration->scope.contains(needle, CaseSensitivity::CaseInsensitive))
            return { TriState::True };
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
