/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCodeComprehension/Types.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Model.h>

namespace HackStudio {

struct Declaration {
    static Declaration create_filename(ByteString const& filename);
    static Declaration create_symbol_declaration(CodeComprehension::Declaration const&);

    bool is_filename() const { return as_filename.has_value(); }
    bool is_symbol_declaration() const { return as_symbol_declaration.has_value(); }

    Optional<ByteString> as_filename;
    Optional<CodeComprehension::Declaration> as_symbol_declaration;
};

class DeclarationsModel final : public GUI::Model {
public:
    explicit DeclarationsModel(Vector<Declaration>&& declarations)
        : m_declarations(move(declarations))
    {
    }

    enum Column {
        Icon,
        Name,
        Filename,
        __Column_Count,
    };

    virtual int row_count(GUI::ModelIndex const& index = GUI::ModelIndex()) const override
    {
        if (!index.is_valid())
            return m_declarations.size();
        return 0;
    }

    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Column_Count; }
    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override;
    virtual MatchResult data_matches(GUI::ModelIndex const&, GUI::Variant const&) const override;

    Vector<Declaration> const& declarations() const { return m_declarations; }
    void set_declarations(Vector<Declaration>&&);

private:
    Vector<Declaration> m_declarations;
};

}
