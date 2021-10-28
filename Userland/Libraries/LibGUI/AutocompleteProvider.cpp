/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/BoxLayout.h>
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
        Completion,
    };

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_suggestions.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Column_Count; }
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
    {
        auto& suggestion = m_suggestions.at(index.row());
        if (role == GUI::ModelRole::Display) {
            if (index.column() == Column::Name) {
                if (!suggestion.display_text.is_empty())
                    return suggestion.display_text;
                else
                    return suggestion.completion;
            }
            if (index.column() == Column::Icon) {
                if (suggestion.language == GUI::AutocompleteProvider::Language::Cpp) {
                    if (!s_cpp_identifier_icon) {
                        s_cpp_identifier_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/completion/cpp-identifier.png");
                    }
                    return *s_cpp_identifier_icon;
                }
                if (suggestion.language == GUI::AutocompleteProvider::Language::Unspecified) {
                    if (!s_unspecified_identifier_icon) {
                        s_unspecified_identifier_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/completion/unspecified-identifier.png");
                    }
                    return *s_unspecified_identifier_icon;
                }
                return {};
            }
        }

        if ((int)role == InternalRole::PartialInputLength)
            return (i64)suggestion.partial_input_length;

        if ((int)role == InternalRole::Completion)
            return suggestion.completion;

        return {};
    }

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
    m_popup_window->set_rect(0, 0, 175, 25);

    auto& main_widget = m_popup_window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    m_suggestion_view = main_widget.add<GUI::TableView>();
    m_suggestion_view->set_column_headers_visible(false);
    m_suggestion_view->set_visible(false);

    m_no_suggestions_view = main_widget.add<GUI::Label>("No suggestions");
}

void AutocompleteBox::update_suggestions(Vector<AutocompleteProvider::Entry>&& suggestions)
{
    // FIXME: There's a potential race here if, after the user selected an autocomplete suggestion,
    // the LanguageServer sends an update and this function is executed before AutocompleteBox::apply_suggestion()
    // is executed.

    bool has_suggestions = !suggestions.is_empty();
    if (m_suggestion_view->model()) {
        auto& model = *static_cast<AutocompleteSuggestionModel*>(m_suggestion_view->model());
        model.set_suggestions(move(suggestions));
    } else {
        m_suggestion_view->set_model(adopt_ref(*new AutocompleteSuggestionModel(move(suggestions))));
        if (has_suggestions)
            m_suggestion_view->set_cursor(m_suggestion_view->model()->index(0), GUI::AbstractView::SelectionUpdate::Set);
    }

    m_suggestion_view->model()->invalidate();

    m_suggestion_view->set_visible(has_suggestions);
    m_no_suggestions_view->set_visible(!has_suggestions);
    m_popup_window->resize(has_suggestions ? Gfx::IntSize(300, 100) : Gfx::IntSize(175, 25));

    m_suggestion_view->update();
}

bool AutocompleteBox::is_visible() const
{
    return m_popup_window->is_visible();
}

void AutocompleteBox::show(Gfx::IntPoint suggestion_box_location)
{
    if (!m_suggestion_view->model())
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

    if (m_suggestion_view->model()->is_within_range(new_index)) {
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

    if (m_suggestion_view->model()->is_within_range(new_index)) {
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
    if (!selected_index.is_valid() || !m_suggestion_view->model()->is_within_range(selected_index))
        return;

    auto suggestion_index = m_suggestion_view->model()->index(selected_index.row());
    auto completion = suggestion_index.data((GUI::ModelRole)AutocompleteSuggestionModel::InternalRole::Completion).to_string();
    size_t partial_length = suggestion_index.data((GUI::ModelRole)AutocompleteSuggestionModel::InternalRole::PartialInputLength).to_i64();

    VERIFY(completion.length() >= partial_length);
    if (!m_editor->has_selection()) {
        auto cursor = m_editor->cursor();
        VERIFY(m_editor->cursor().column() >= partial_length);

        TextPosition start(cursor.line(), cursor.column() - partial_length);
        auto end = cursor;
        m_editor->delete_text_range(TextRange(start, end));
    }

    m_editor->insert_at_cursor_or_replace_selection(completion);
}

bool AutocompleteProvider::Declaration::operator==(const AutocompleteProvider::Declaration& other) const
{
    return name == other.name && position == other.position && type == other.type && scope == other.scope;
}

bool AutocompleteProvider::ProjectLocation::operator==(const ProjectLocation& other) const
{
    return file == other.file && line == other.line && column == other.column;
}

}
