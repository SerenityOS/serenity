/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Locator.h"
#include "HackStudio.h"
#include "Project.h"
#include "ProjectDeclarations.h"
#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

namespace HackStudio {

class LocatorSuggestionModel final : public GUI::Model {
public:
    struct Suggestion {
        static Suggestion create_filename(const String& filename);
        static Suggestion create_symbol_declaration(const GUI::AutocompleteProvider::Declaration&);

        bool is_filename() const { return as_filename.has_value(); }
        bool is_symbol_declaration() const { return as_symbol_declaration.has_value(); }

        Optional<String> as_filename;
        Optional<GUI::AutocompleteProvider::Declaration> as_symbol_declaration;
    };

    explicit LocatorSuggestionModel(Vector<Suggestion>&& suggestions)
        : m_suggestions(move(suggestions))
    {
    }

    enum Column {
        Icon,
        Name,
        Filename,
        __Column_Count,
    };
    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_suggestions.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Column_Count; }
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
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
                if (suggestion.as_symbol_declaration.value().scope.is_null())
                    return suggestion.as_symbol_declaration.value().name;
                return String::formatted("{}::{}", suggestion.as_symbol_declaration.value().scope, suggestion.as_symbol_declaration.value().name);
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

    const Vector<Suggestion>& suggestions() const { return m_suggestions; }

private:
    Vector<Suggestion> m_suggestions;
};

LocatorSuggestionModel::Suggestion LocatorSuggestionModel::Suggestion::create_filename(const String& filename)
{
    LocatorSuggestionModel::Suggestion s;
    s.as_filename = filename;
    return s;
}
LocatorSuggestionModel::Suggestion LocatorSuggestionModel::Suggestion::create_symbol_declaration(const GUI::AutocompleteProvider::Declaration& decl)
{
    LocatorSuggestionModel::Suggestion s;
    s.as_symbol_declaration = decl;
    return s;
}

Locator::Locator(Core::Object* parent)
{
    set_layout<GUI::VerticalBoxLayout>();
    set_fixed_height(20);
    m_textbox = add<GUI::TextBox>();
    m_textbox->on_change = [this] {
        update_suggestions();
    };

    m_textbox->on_escape_pressed = [this] {
        m_popup_window->hide();
        m_textbox->set_focus(false);
    };

    m_textbox->on_up_pressed = [this] {
        GUI::ModelIndex new_index = m_suggestion_view->selection().first();
        if (new_index.is_valid())
            new_index = m_suggestion_view->model()->index(new_index.row() - 1);
        else
            new_index = m_suggestion_view->model()->index(0);

        if (m_suggestion_view->model()->is_within_range(new_index)) {
            m_suggestion_view->selection().set(new_index);
            m_suggestion_view->scroll_into_view(new_index, Orientation::Vertical);
        }
    };
    m_textbox->on_down_pressed = [this] {
        GUI::ModelIndex new_index = m_suggestion_view->selection().first();
        if (new_index.is_valid())
            new_index = m_suggestion_view->model()->index(new_index.row() + 1);
        else
            new_index = m_suggestion_view->model()->index(0);

        if (m_suggestion_view->model()->is_within_range(new_index)) {
            m_suggestion_view->selection().set(new_index);
            m_suggestion_view->scroll_into_view(new_index, Orientation::Vertical);
        }
    };

    m_textbox->on_return_pressed = [this] {
        auto selected_index = m_suggestion_view->selection().first();
        if (!selected_index.is_valid())
            return;
        open_suggestion(selected_index);
    };

    m_textbox->on_focusout = [&]() {
        close();
    };

    m_popup_window = GUI::Window::construct(parent);
    // FIXME: This is obviously not a tooltip window, but it's the closest thing to what we want atm.
    m_popup_window->set_window_type(GUI::WindowType::Tooltip);
    m_popup_window->set_rect(0, 0, 500, 200);

    m_suggestion_view = m_popup_window->set_main_widget<GUI::TableView>();
    m_suggestion_view->set_column_headers_visible(false);

    m_suggestion_view->on_activation = [this](auto& index) {
        open_suggestion(index);
    };
}

Locator::~Locator()
{
}

void Locator::open_suggestion(const GUI::ModelIndex& index)
{
    auto& model = reinterpret_cast<LocatorSuggestionModel&>(*m_suggestion_view->model());
    auto suggestion = model.suggestions()[index.row()];
    if (suggestion.is_filename()) {
        auto filename = suggestion.as_filename.value();
        open_file(filename);
    }
    if (suggestion.is_symbol_declaration()) {
        auto position = suggestion.as_symbol_declaration.value().position;
        open_file(position.file, position.line, position.column);
    }
    close();
}

void Locator::open()
{
    m_textbox->set_focus(true);
    if (!m_textbox->text().is_empty()) {
        m_textbox->select_all();
        m_popup_window->show();
    }
}

void Locator::close()
{
    m_popup_window->hide();
}

void Locator::update_suggestions()
{
    auto typed_text = m_textbox->text();
    Vector<LocatorSuggestionModel::Suggestion> suggestions;
    project().for_each_text_file([&](auto& file) {
        if (file.name().contains(typed_text, CaseSensitivity::CaseInsensitive))
            suggestions.append(LocatorSuggestionModel::Suggestion::create_filename(file.name()));
    });

    ProjectDeclarations::the().for_each_declared_symbol([&suggestions, &typed_text](auto& decl) {
        if (decl.name.contains(typed_text, CaseSensitivity::CaseInsensitive) || decl.scope.contains(typed_text, CaseSensitivity::CaseInsensitive))
            suggestions.append((LocatorSuggestionModel::Suggestion::create_symbol_declaration(decl)));
    });

    dbgln("I have {} suggestion(s):", suggestions.size());
    // Limit the debug logging otherwise this can be very slow for large projects
    if (suggestions.size() < 100) {
        for (auto& s : suggestions) {
            if (s.is_filename())
                dbgln("    {}", s.as_filename.value());
            if (s.is_symbol_declaration())
                dbgln("    {} ({})", s.as_symbol_declaration.value().name, s.as_symbol_declaration.value().position.file);
        }
    }

    bool has_suggestions = !suggestions.is_empty();

    m_suggestion_view->set_model(adopt_ref(*new LocatorSuggestionModel(move(suggestions))));

    if (!has_suggestions)
        m_suggestion_view->selection().clear();
    else
        m_suggestion_view->selection().set(m_suggestion_view->model()->index(0));

    m_popup_window->move_to(screen_relative_rect().top_left().translated(0, -m_popup_window->height()));
    dbgln("Popup rect: {}", m_popup_window->rect());
    m_popup_window->show();
}
}
