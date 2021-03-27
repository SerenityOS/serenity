/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/Model.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

static RefPtr<Gfx::Bitmap> s_cpp_identifier_icon;
static RefPtr<Gfx::Bitmap> s_unspecified_identifier_icon;

namespace GUI {

class AutocompleteSuggestionModel final : public GUI::Model {
public:
    explicit AutocompleteSuggestionModel(Vector<AutocompleteProvider::Entry>&& suggestions)
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
                if (suggestion.language == GUI::AutocompleteProvider::Language::Cpp) {
                    if (!s_cpp_identifier_icon) {
                        s_cpp_identifier_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/completion/cpp-identifier.png");
                    }
                    return *s_cpp_identifier_icon;
                }
                if (suggestion.language == GUI::AutocompleteProvider::Language::Unspecified) {
                    if (!s_unspecified_identifier_icon) {
                        s_unspecified_identifier_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/completion/unspecified-identifier.png");
                    }
                    return *s_unspecified_identifier_icon;
                }
                return {};
            }
        }

        if ((int)role == InternalRole::Kind)
            return (u32)suggestion.kind;

        if ((int)role == InternalRole::PartialInputLength)
            return (i64)suggestion.partial_input_length;

        return {};
    }
    virtual void update() override {};

    void set_suggestions(Vector<AutocompleteProvider::Entry>&& suggestions) { m_suggestions = move(suggestions); }

private:
    Vector<AutocompleteProvider::Entry> m_suggestions;
};

AutocompleteBox::~AutocompleteBox() { }

AutocompleteBox::AutocompleteBox(TextEditor& editor)
    : m_editor(editor)
{
    m_popup_window = GUI::Window::construct(m_editor->window());
    m_popup_window->set_window_type(GUI::WindowType::Tooltip);
    m_popup_window->set_rect(0, 0, 200, 100);

    m_suggestion_view = m_popup_window->set_main_widget<GUI::TableView>();
    m_suggestion_view->set_column_headers_visible(false);
    m_suggestion_view->set_column_width(1, 100);
}

void AutocompleteBox::update_suggestions(Vector<AutocompleteProvider::Entry>&& suggestions)
{
    bool has_suggestions = !suggestions.is_empty();
    if (m_suggestion_view->model()) {
        auto& model = *static_cast<AutocompleteSuggestionModel*>(m_suggestion_view->model());
        model.set_suggestions(move(suggestions));
    } else {
        m_suggestion_view->set_model(adopt(*new AutocompleteSuggestionModel(move(suggestions))));
        m_suggestion_view->update();
        if (has_suggestions)
            m_suggestion_view->set_cursor(m_suggestion_view->model()->index(0), GUI::AbstractView::SelectionUpdate::Set);
    }

    m_suggestion_view->model()->update();
    m_suggestion_view->update();
    if (!has_suggestions)
        close();
}

bool AutocompleteBox::is_visible() const
{
    return m_popup_window->is_visible();
}

void AutocompleteBox::show(Gfx::IntPoint suggestion_box_location)
{
    if (!m_suggestion_view->model() || m_suggestion_view->model()->row_count() == 0)
        return;

    m_popup_window->move_to(suggestion_box_location);
    if (!is_visible())
        m_suggestion_view->move_cursor(GUI::AbstractView::CursorMovement::Home, GUI::AbstractTableView::SelectionUpdate::Set);
    m_popup_window->show();
}

void AutocompleteBox::close()
{
    m_popup_window->hide();
}

void AutocompleteBox::next_suggestion()
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

void AutocompleteBox::previous_suggestion()
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

void AutocompleteBox::apply_suggestion()
{
    if (m_editor.is_null())
        return;

    if (!m_editor->is_editable())
        return;

    auto selected_index = m_suggestion_view->selection().first();
    if (!selected_index.is_valid())
        return;

    auto suggestion_index = m_suggestion_view->model()->index(selected_index.row(), AutocompleteSuggestionModel::Column::Name);
    auto suggestion = suggestion_index.data().to_string();
    size_t partial_length = suggestion_index.data((GUI::ModelRole)AutocompleteSuggestionModel::InternalRole::PartialInputLength).to_i64();

    VERIFY(suggestion.length() >= partial_length);
    auto completion = suggestion.substring_view(partial_length, suggestion.length() - partial_length);
    m_editor->insert_at_cursor_or_replace_selection(completion);
}

}
