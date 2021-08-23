/*
 * Copyright (c) 2021, Robin Allen <r@foon.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"

#include "DictionaryModel.h"

#include <Applications/Dictionary/DictionaryWindowGML.h>
#include <LibGUI/ListView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/TextEditor.h>

namespace Dictionary {

MainWidget::MainWidget(StringView initial_query)
{
    load_from_gml(dictionary_window_gml);

    m_editor = *find_descendant_of_type_named<GUI::TextEditor>("editor");
    m_list_view = *find_descendant_of_type_named<GUI::ListView>("index");
    m_search = *find_descendant_of_type_named<GUI::TextBox>("search");

    m_editor->set_wrapping_mode(GUI::TextEditor::WrappingMode::WrapAtWords);
    m_editor->set_mode(GUI::TextEditor::ReadOnly);

    m_search->set_text(initial_query);

    m_model = DictionaryModel::create();
    m_model->set_query(initial_query);
    m_list_view->set_model(m_model);

    m_document = GUI::TextDocument::create();
    m_editor->set_document(*m_document);

    m_list_view->on_selection_change = [this] {
        if (m_model->row_count() != 0) {
            auto index = m_list_view->selection().first();
            if (index.is_valid()) {
                int row = index.row();
                m_document->set_text(m_model->definition_of(row));
            }
        }
    };

    m_search->on_change = [this] {
        String value = m_search->text();
        m_model->set_query(value);

        m_list_view->selection().set(m_model->index(0, 0));
    };

    m_search->on_up_pressed = [this] {
        move_selection_by(-1);
    };

    m_search->on_down_pressed = [this] {
        move_selection_by(1);
    };

    if (m_model->row_count() == 0) {
        m_document->set_text(
            "To use the Dictionary, you'll need to generate its "
            "data file.\n\n"
            "You can do this using Meta/build-dictionaries.py\n");
    }
}

void MainWidget::focus_search_box()
{
    m_search->set_focus(true);
}

void MainWidget::move_selection_by(int delta)
{
    auto& selection = m_list_view->selection();

    int n = m_model->row_count();

    if (n == 0) {
        return;
    }

    if (selection.is_empty()) {
        selection.set(m_model->index(delta == 1 ? 0 : n - 1, 0));
    } else {
        auto index = selection.first();

        int new_row = index.row() + delta;

        if (0 <= new_row && new_row < n) {
            auto new_index = m_model->index(new_row, index.column());
            selection.set(new_index);
            m_list_view->scroll_into_view(new_index, false, true);
        }
    }
}

}
