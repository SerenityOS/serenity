/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include "AutoCompleteBox.h"
#include "Editor.h"
#include <LibGUI/Model.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

namespace HackStudio {

static RefPtr<Gfx::Bitmap> s_cplusplus_icon;

class AutoCompleteSuggestionModel final : public GUI::Model {
public:
    explicit AutoCompleteSuggestionModel(Vector<AutoCompleteResponse>&& suggestions)
        : m_suggestions(move(suggestions))
    {
    }

    enum Column {
        Icon,
        Name,
        __Column_Count,
    };

    enum InternalRole {
        __ModelRoleCustom = (int)GUI::ModelRole::Custom,
        PartialInputLength,
        Kind,
    };

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_suggestions.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Column_Count; }
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
    {
        auto& suggestion = m_suggestions.at(index.row());
        if (role == GUI::ModelRole::Display) {
            if (index.column() == Column::Name) {
                return suggestion.completion;
            }
            if (index.column() == Column::Icon) {
                // TODO: Have separate icons for fields, functions, methods etc
                // FIXME: Probably should have different icons for the different kinds, rather than for "c++".
                if (suggestion.kind == CompletionKind::Identifier)
                    return *s_cplusplus_icon;
                return *s_cplusplus_icon;
            }
        }

        if ((int)role == InternalRole::Kind)
            return (u32)suggestion.kind;

        if ((int)role == InternalRole::PartialInputLength)
            return (i64)suggestion.partial_input_length;

        return {};
    }
    virtual void update() override {};

private:
    Vector<AutoCompleteResponse> m_suggestions;
};

AutoCompleteBox::~AutoCompleteBox() { }

AutoCompleteBox::AutoCompleteBox(WeakPtr<Editor> editor)
    : m_editor(move(editor))
{
    if (!s_cplusplus_icon) {
        s_cplusplus_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-cplusplus.png");
    }

    m_popup_window = GUI::Window::construct();
    m_popup_window->set_window_type(GUI::WindowType::Tooltip);
    m_popup_window->set_rect(0, 0, 200, 100);

    m_suggestion_view = m_popup_window->set_main_widget<GUI::TableView>();
    m_suggestion_view->set_column_headers_visible(false);
}

void AutoCompleteBox::update_suggestions(Vector<AutoCompleteResponse>&& suggestions)
{
    if (suggestions.is_empty())
        return;

    bool has_suggestions = !suggestions.is_empty();
    m_suggestion_view->set_model(adopt(*new AutoCompleteSuggestionModel(move(suggestions))));

    if (!has_suggestions)
        m_suggestion_view->selection().clear();
    else
        m_suggestion_view->selection().set(m_suggestion_view->model()->index(0));
}

void AutoCompleteBox::show(Gfx::IntPoint suggstion_box_location)
{
    m_popup_window->move_to(suggstion_box_location);
    m_popup_window->show();
}

void AutoCompleteBox::close()
{
    m_popup_window->hide();
}

void AutoCompleteBox::next_suggestion()
{
    GUI::ModelIndex new_index = m_suggestion_view->selection().first();
    if (new_index.is_valid())
        new_index = m_suggestion_view->model()->index(new_index.row() + 1);
    else
        new_index = m_suggestion_view->model()->index(0);

    if (m_suggestion_view->model()->is_valid(new_index)) {
        m_suggestion_view->selection().set(new_index);
        m_suggestion_view->scroll_into_view(new_index, Orientation::Vertical);
    }
}

void AutoCompleteBox::previous_suggestion()
{
    GUI::ModelIndex new_index = m_suggestion_view->selection().first();
    if (new_index.is_valid())
        new_index = m_suggestion_view->model()->index(new_index.row() - 1);
    else
        new_index = m_suggestion_view->model()->index(0);

    if (m_suggestion_view->model()->is_valid(new_index)) {
        m_suggestion_view->selection().set(new_index);
        m_suggestion_view->scroll_into_view(new_index, Orientation::Vertical);
    }
}

void AutoCompleteBox::apply_suggestion()
{
    if (m_editor.is_null())
        return;

    auto selected_index = m_suggestion_view->selection().first();
    if (!selected_index.is_valid())
        return;

    auto suggestion_index = m_suggestion_view->model()->index(selected_index.row(), AutoCompleteSuggestionModel::Column::Name);
    auto suggestion = suggestion_index.data().to_string();
    size_t partial_length = suggestion_index.data((GUI::ModelRole)AutoCompleteSuggestionModel::InternalRole::PartialInputLength).to_i64();

    ASSERT(suggestion.length() >= partial_length);
    auto completion = suggestion.substring_view(partial_length, suggestion.length() - partial_length);
    m_editor->insert_at_cursor_or_replace_selection(completion);
}

};
