/*
 * Copyright (c) 2021, Federico Guerinoni <guerinoni.federico@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ToDoEntriesWidget.h"
#include "HackStudio.h"
#include "ToDoEntries.h"
#include <LibGUI/BoxLayout.h>
#include <LibGfx/FontDatabase.h>

namespace HackStudio {

class ToDoEntriesModel final : public GUI::Model {
public:
    enum Column {
        Filename,
        Text,
        Line,
        Column,
        __Count
    };

    explicit ToDoEntriesModel(Vector<Cpp::Parser::TodoEntry> const&& matches)
        : m_matches(move(matches))
    {
    }

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return m_matches.size(); }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }

    virtual String column_name(int column) const override
    {
        switch (column) {
        case Column::Filename:
            return "Filename";
        case Column::Text:
            return "Text";
        case Column::Line:
            return "Line";
        case Column::Column:
            return "Col";
        default:
            VERIFY_NOT_REACHED();
        }
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::TextAlignment)
            return Gfx::TextAlignment::CenterLeft;
        if (role == GUI::ModelRole::Font) {
            if (index.column() == Column::Text)
                return Gfx::FontDatabase::default_fixed_width_font();
            return {};
        }
        if (role == GUI::ModelRole::Display) {
            auto& match = m_matches.at(index.row());
            switch (index.column()) {
            case Column::Filename:
                return match.filename;
            case Column::Text:
                return match.content;
            case Column::Line:
                return String::formatted("{}", match.line + 1);
            case Column::Column:
                return String::formatted("{}", match.column);
            }
        }
        return {};
    }

    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& = GUI::ModelIndex()) const override
    {
        if (row < 0 || row >= (int)m_matches.size())
            return {};
        if (column < 0 || column >= Column::__Count)
            return {};
        return create_index(row, column, &m_matches.at(row));
    }

private:
    Vector<Cpp::Parser::TodoEntry> m_matches;
};

void ToDoEntriesWidget::refresh()
{
    const auto& entries = ToDoEntries::the().get_entries();
    auto results_model = adopt_ref(*new ToDoEntriesModel(move(entries)));
    m_result_view->set_model(results_model);
}

void ToDoEntriesWidget::clear()
{
    ToDoEntries::the().clear_entries();
    refresh();
}

ToDoEntriesWidget::ToDoEntriesWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    m_result_view = add<GUI::TableView>();

    m_result_view->on_activation = [](auto& index) {
        auto& match = *(Cpp::Parser::TodoEntry const*)index.internal_data();
        open_file(match.filename, match.line, match.column);
    };
}

}
