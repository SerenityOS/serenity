/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Locator.h"
#include "DeclarationsModel.h"
#include "HackStudio.h"
#include "Project.h"
#include "ProjectDeclarations.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

namespace HackStudio {

Locator::Locator(Core::EventReceiver* parent)
{
    set_layout<GUI::VerticalBoxLayout>();
    set_fixed_height(22);
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
    m_popup_window->set_window_type(GUI::WindowType::Autocomplete);
    m_popup_window->set_rect(0, 0, 500, 200);

    m_suggestion_view = m_popup_window->set_main_widget<GUI::TableView>();
    m_suggestion_view->set_column_headers_visible(false);

    m_suggestion_view->on_activation = [this](auto& index) {
        open_suggestion(index);
    };
}

void Locator::open_suggestion(const GUI::ModelIndex& index)
{
    auto& model = reinterpret_cast<DeclarationsModel&>(*m_suggestion_view->model());
    auto suggestion = model.declarations()[index.row()];
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
    m_textbox->set_focus(false);
}

void Locator::update_suggestions()
{
    auto typed_text = m_textbox->text();
    Vector<Declaration> suggestions;
    project().for_each_text_file([&](auto& file) {
        if (file.name().contains(typed_text, CaseSensitivity::CaseInsensitive))
            suggestions.append(Declaration::create_filename(file.name()));
    });

    ProjectDeclarations::the().for_each_declared_symbol([&suggestions, &typed_text](auto& decl) {
        if (decl.name.contains(typed_text, CaseSensitivity::CaseInsensitive) || decl.scope.contains(typed_text, CaseSensitivity::CaseInsensitive))
            suggestions.append((Declaration::create_symbol_declaration(decl)));
    });

    bool has_suggestions = !suggestions.is_empty();

    m_suggestion_view->set_model(adopt_ref(*new DeclarationsModel(move(suggestions))));

    if (!has_suggestions)
        m_suggestion_view->selection().clear();
    else
        m_suggestion_view->selection().set(m_suggestion_view->model()->index(0));

    m_popup_window->move_to(screen_relative_rect().top_left().translated(0, -m_popup_window->height()));
    m_popup_window->show();
}
}
