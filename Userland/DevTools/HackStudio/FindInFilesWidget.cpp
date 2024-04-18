/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FindInFilesWidget.h"
#include "HackStudio.h"
#include "Project.h"
#include <AK/StringBuilder.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font/FontDatabase.h>

namespace HackStudio {

struct Match {
    ByteString filename;
    GUI::TextRange range;
    ByteString text;
};

class SearchResultsModel final : public GUI::Model {
public:
    enum Column {
        Filename,
        Location,
        MatchedText,
        __Count
    };

    explicit SearchResultsModel(Vector<Match> const&& matches)
        : m_matches(move(matches))
    {
    }

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_matches.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }

    virtual ErrorOr<String> column_name(int column) const override
    {
        switch (column) {
        case Column::Filename:
            return "Filename"_string;
        case Column::Location:
            return "#"_string;
        case Column::MatchedText:
            return "Text"_string;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::TextAlignment)
            return Gfx::TextAlignment::CenterLeft;
        if (role == GUI::ModelRole::Font) {
            if (index.column() == Column::MatchedText)
                return Gfx::FontDatabase::default_fixed_width_font();
            return {};
        }
        if (role == GUI::ModelRole::Display) {
            auto& match = m_matches.at(index.row());
            switch (index.column()) {
            case Column::Filename:
                return match.filename;
            case Column::Location:
                return (int)match.range.start().line();
            case Column::MatchedText:
                return match.text;
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
    Vector<Match> m_matches;
};

static RefPtr<SearchResultsModel> find_in_files(StringView text)
{
    Vector<Match> matches;
    project().for_each_text_file([&](auto& file) {
        auto matches_in_file = file.document().find_all(text);
        for (auto& range : matches_in_file) {
            auto whole_line_range = file.document().range_for_entire_line(range.start().line());
            auto whole_line_containing_match = file.document().text_in_range(whole_line_range);
            auto left_part = whole_line_containing_match.substring(0, range.start().column());
            auto right_part = whole_line_containing_match.substring(range.end().column(), whole_line_containing_match.length() - range.end().column());
            StringBuilder builder;
            builder.append(left_part);
            builder.append(0x01);
            builder.append(file.document().text_in_range(range));
            builder.append(0x02);
            builder.append(right_part);
            matches.append({ file.name(), range, builder.to_byte_string() });
        }
    });

    return adopt_ref(*new SearchResultsModel(move(matches)));
}

FindInFilesWidget::FindInFilesWidget()
{
    set_layout<GUI::VerticalBoxLayout>();

    auto& top_container = add<Widget>();
    top_container.set_layout<GUI::HorizontalBoxLayout>();
    top_container.set_fixed_height(22);

    m_textbox = top_container.add<GUI::TextBox>();

    m_button = top_container.add<GUI::Button>("Find"_string);
    m_button->set_fixed_width(50);

    m_result_view = add<GUI::TableView>();

    m_result_view->on_activation = [](auto& index) {
        auto& match = *(Match const*)index.internal_data();
        open_file(match.filename);
        current_editor().set_selection(match.range);
        current_editor().set_focus(true);
    };

    m_button->on_click = [this](auto) {
        auto results_model = find_in_files(m_textbox->text());
        m_result_view->set_model(results_model);
    };
    m_textbox->on_return_pressed = [this] {
        m_button->click();
    };
}

void FindInFilesWidget::focus_textbox_and_select_all()
{
    m_textbox->select_all();
    m_textbox->set_focus(true);
}
void FindInFilesWidget::reset()
{
    m_result_view->set_model(nullptr);
}

}
