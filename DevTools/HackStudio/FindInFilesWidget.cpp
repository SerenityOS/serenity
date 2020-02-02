/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "FindInFilesWidget.h"
#include "Project.h"
#include <AK/StringBuilder.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextBox.h>

extern GUI::TextEditor& current_editor();
extern void open_file(const String&);
extern OwnPtr<Project> g_project;

struct Match {
    String filename;
    GUI::TextRange range;
    String text;
};

class SearchResultsModel final : public GUI::Model {
public:
    enum Column {
        Filename,
        Location,
        MatchedText,
        __Count
    };

    explicit SearchResultsModel(const Vector<Match>&& matches)
        : m_matches(move(matches))
    {
    }

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_matches.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }

    virtual String column_name(int column) const override
    {
        switch (column) {
        case Column::Filename:
            return "Filename";
        case Column::Location:
            return "#";
        case Column::MatchedText:
            return "Text";
        default:
            ASSERT_NOT_REACHED();
        }
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, Role role = Role::Display) const override
    {
        if (role == Role::Display) {
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

    virtual ColumnMetadata column_metadata(int column) const override
    {
        if (column == Column::MatchedText) {
            return { 0, TextAlignment::CenterLeft, &Font::default_fixed_width_font() };
        }
        return {};
    }

    virtual void update() override {}
    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& = GUI::ModelIndex()) const override { return create_index(row, column, &m_matches.at(row)); }

private:
    Vector<Match> m_matches;
};

static RefPtr<SearchResultsModel> find_in_files(const StringView& text)
{
    Vector<Match> matches;
    g_project->for_each_text_file([&](auto& file) {
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
            matches.append({ file.name(), range, builder.to_string() });
        }
    });

    return adopt(*new SearchResultsModel(move(matches)));
}

FindInFilesWidget::FindInFilesWidget(GUI::Widget* parent)
    : GUI::Widget(parent)
{
    set_layout(make<GUI::VBoxLayout>());
    m_textbox = GUI::TextBox::construct(this);
    m_textbox->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_textbox->set_preferred_size(0, 20);
    m_button = GUI::Button::construct("Find in files", this);
    m_button->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_button->set_preferred_size(0, 20);

    m_result_view = GUI::TableView::construct(this);
    m_result_view->set_size_columns_to_fit_content(true);

    m_result_view->on_activation = [](auto& index) {
        auto& match = *(const Match*)index.internal_data();
        open_file(match.filename);
        current_editor().set_selection(match.range);
        current_editor().set_focus(true);
    };

    m_button->on_click = [this](auto&) {
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
