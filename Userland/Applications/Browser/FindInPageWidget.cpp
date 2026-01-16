/*
 * Copyright (c) 2026, Fırat Kızılboğa <firatkizilboga11@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FindInPageWidget.h"
#include <Applications/Browser/FindInPageWidgetGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Bitmap.h>

REGISTER_WIDGET(Browser, FindInPageWidget)

namespace Browser {

FindInPageWidget::FindInPageWidget()
{
    load_from_gml(find_in_page_widget_gml).release_value_but_fixme_should_propagate_errors();

    m_close_button = find_descendant_of_type_named<GUI::Button>("close_button");
    m_previous_button = find_descendant_of_type_named<GUI::Button>("previous_button");
    m_next_button = find_descendant_of_type_named<GUI::Button>("next_button");
    m_search_textbox = find_descendant_of_type_named<GUI::TextBox>("search_textbox");
    m_match_case_checkbox = find_descendant_of_type_named<GUI::CheckBox>("match_case_checkbox");
    m_result_label = find_descendant_of_type_named<GUI::Label>("result_label");

    m_close_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/window-close.png"sv).release_value_but_fixme_should_propagate_errors());
    m_previous_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-up.png"sv).release_value_but_fixme_should_propagate_errors());
    m_next_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-down.png"sv).release_value_but_fixme_should_propagate_errors());
}

void FindInPageWidget::initialize(WebView::OutOfProcessWebView& web_view)
{
    m_web_content_view = web_view;

    m_search_textbox->on_change = [this] {
        find_text_changed();
    };

    m_search_textbox->on_return_pressed = [this] {
        m_web_content_view->find_in_page_next_match();
    };

    m_search_textbox->on_shift_return_pressed = [this] {
        m_web_content_view->find_in_page_previous_match();
    };

    m_next_button->on_click = [this](auto) {
        m_web_content_view->find_in_page_next_match();
    };

    m_previous_button->on_click = [this](auto) {
        m_web_content_view->find_in_page_previous_match();
    };

    m_close_button->on_click = [this](auto) {
        set_visible(false);
    };

    m_match_case_checkbox->on_checked = [this](bool) {
        if (!m_search_textbox->text().is_empty())
            find_text_changed();
    };

    m_web_content_view->on_find_in_page = [this](size_t current_match_index, Optional<size_t> const& total_match_count) {
        update_result_label(current_match_index, total_match_count);
    };
}

void FindInPageWidget::find_text_changed()
{
    auto query = MUST(String::from_byte_string(m_search_textbox->text()));
    auto case_sensitive = m_match_case_checkbox->is_checked() ? CaseSensitivity::CaseSensitive : CaseSensitivity::CaseInsensitive;
    m_web_content_view->find_in_page(query, case_sensitive);
}

void FindInPageWidget::update_result_label(size_t current_match_index, Optional<size_t> const& total_match_count)
{
    if (total_match_count.has_value()) {
        if (*total_match_count > 0) {
            m_result_label->set_text(MUST(String::formatted("{} of {} matches", current_match_index + 1, *total_match_count)));
        } else {
            m_result_label->set_text("Phrase not found"_string);
        }
        m_result_label->set_visible(true);
    } else {
        m_result_label->set_visible(false);
    }
}

void FindInPageWidget::set_search_text(String const& text)
{
    m_search_textbox->set_text(text);
    m_search_textbox->select_all();
}

void FindInPageWidget::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == KeyCode::Key_Escape) {
        set_visible(false);
        event.accept();
        return;
    }

    Widget::keydown_event(event);
}

}
