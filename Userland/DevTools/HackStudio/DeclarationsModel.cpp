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

DeclarationsModel::Suggestion DeclarationsModel::Suggestion::create_filename(ByteString const& filename)
{
    DeclarationsModel::Suggestion s;
    s.as_filename = filename;
    return s;
}
DeclarationsModel::Suggestion DeclarationsModel::Suggestion::create_symbol_declaration(CodeComprehension::Declaration const& decl)
{
    DeclarationsModel::Suggestion s;
    s.as_symbol_declaration = decl;
    return s;
}

GUI::Variant DeclarationsModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto& suggestion = m_suggestions.at(index.row());
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

}
