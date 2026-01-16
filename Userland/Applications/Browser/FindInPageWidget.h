/*
 * Copyright (c) 2026, Fırat Kızılboğa <firatkizilboga11@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Event.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace Browser {

class FindInPageWidget final
    : public GUI::Widget {
    C_OBJECT(FindInPageWidget);

public:
    virtual ~FindInPageWidget() override = default;

    void initialize(WebView::OutOfProcessWebView& web_view);
    void set_search_text(String const& text);

private:
    FindInPageWidget();
    virtual void keydown_event(GUI::KeyEvent&) override;

    void find_text_changed();
    void update_result_label(size_t current_match_index, Optional<size_t> const& total_match_count);

    RefPtr<WebView::OutOfProcessWebView> m_web_content_view;

    RefPtr<GUI::Button> m_close_button;
    RefPtr<GUI::Button> m_previous_button;
    RefPtr<GUI::Button> m_next_button;
    RefPtr<GUI::TextBox> m_search_textbox;
    RefPtr<GUI::CheckBox> m_match_case_checkbox;
    RefPtr<GUI::Label> m_result_label;
};

}
