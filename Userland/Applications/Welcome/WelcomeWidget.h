/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibWeb/OutOfProcessWebView.h>

class WelcomeWidget final : public GUI::Widget {
    C_OBJECT(WelcomeWidget);

public:
    virtual ~WelcomeWidget() override = default;

private:
    WelcomeWidget();

    virtual void paint_event(GUI::PaintEvent&) override;

    void set_random_tip();
    void open_and_parse_tips_file();
    void open_and_parse_readme_file();

    RefPtr<GUI::Button> m_close_button;
    RefPtr<GUI::Button> m_next_button;
    RefPtr<GUI::Button> m_help_button;
    RefPtr<GUI::Button> m_new_button;
    RefPtr<GUI::Label> m_tip_label;
    RefPtr<GUI::CheckBox> m_startup_checkbox;
    RefPtr<Web::OutOfProcessWebView> m_web_view;

    size_t m_initial_tip_index { 0 };
    Vector<String> m_tips;
};
