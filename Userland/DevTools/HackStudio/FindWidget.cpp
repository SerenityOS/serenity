/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FindWidget.h"
#include "Editor.h"
#include <AK/QuickSort.h>
#include <DevTools/HackStudio/FindWidgetGML.h>
#include <LibGUI/BoxLayout.h>
#include <LibGfx/Palette.h>

namespace HackStudio {

FindWidget::FindWidget(NonnullRefPtr<Editor> editor)
    : m_editor(move(editor))
{
    load_from_gml(find_widget_gml);
    set_fixed_height(widget_height);
    m_input_field = find_descendant_of_type_named<GUI::TextBox>("input_field");
    m_index_label = find_descendant_of_type_named<GUI::Label>("index_label");
    m_next = find_descendant_of_type_named<GUI::Button>("next");
    m_previous = find_descendant_of_type_named<GUI::Button>("previous");

    VERIFY(m_input_field);
    VERIFY(m_next);
    VERIFY(m_previous);

    m_next->on_click = [this](auto) {
        find_next(GUI::TextEditor::SearchDirection::Forward);
    };
    m_previous->on_click = [this](auto) {
        find_next(GUI::TextEditor::SearchDirection::Backward);
    };

    m_input_field->on_change = [this]() {
        m_editor->reset_search_results();
        find_next(GUI::TextEditor::SearchDirection::Forward);
    };

    m_input_field->on_return_pressed = [this]() {
        find_next(GUI::TextEditor::SearchDirection::Forward);
    };

    m_input_field->on_escape_pressed = [this]() {
        hide();
    };
}

void FindWidget::show()
{
    set_visible(true);
    set_focus(true);
    m_input_field->set_focus(true);
    // Adjust scroll value to smooth the appearance of the FindWidget.
    m_editor->vertical_scrollbar().set_value(m_editor->vertical_scrollbar().value() + widget_height, GUI::AllowCallback::Yes, GUI::Scrollbar::DoClamp::No);
    m_visible = !m_visible;
}

void FindWidget::hide()
{
    set_visible(false);
    set_focus(false);
    m_visible = !m_visible;
    m_editor->vertical_scrollbar().set_value(m_editor->vertical_scrollbar().value() - widget_height, GUI::AllowCallback::Yes, GUI::Scrollbar::DoClamp::No);
    m_editor->set_focus(true);
    m_editor->reset_search_results();
}

void FindWidget::find_next(GUI::TextEditor::SearchDirection direction)
{
    auto needle = m_input_field->text();
    if (needle.is_empty()) {
        m_editor->reset_search_results();
        m_index_label->set_text(String::empty());
        return;
    }
    auto result = m_editor->find_text(needle, direction, GUI::TextDocument::SearchShouldWrap::Yes, false, false);

    if (result.is_valid())
        m_index_label->set_text(String::formatted("{}/{}", m_editor->search_result_index().value_or(0) + 1, m_editor->search_results().size()));
    else
        m_index_label->set_text(String::empty());
}

}
