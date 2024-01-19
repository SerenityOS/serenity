/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace Welcome {
class WelcomeWidget final : public GUI::Widget {
    C_OBJECT(WelcomeWidget);

public:
    static ErrorOr<NonnullRefPtr<WelcomeWidget>> create();
    virtual ~WelcomeWidget() override = default;

private:
    WelcomeWidget() = default;
    ErrorOr<void> create_widgets();
    static ErrorOr<NonnullRefPtr<WelcomeWidget>> try_create();

    virtual void paint_event(GUI::PaintEvent&) override;

    void set_random_tip();
    ErrorOr<void> open_and_parse_tips_file();

    RefPtr<Gfx::BitmapFont> m_banner_font;
    RefPtr<GUI::Widget> m_banner_widget;

    RefPtr<GUI::Button> m_close_button;
    RefPtr<GUI::Button> m_next_button;
    RefPtr<GUI::Button> m_help_button;
    RefPtr<GUI::Button> m_new_button;
    RefPtr<GUI::Frame> m_tip_frame;
    RefPtr<GUI::Label> m_tip_label;
    RefPtr<GUI::CheckBox> m_startup_checkbox;
    RefPtr<WebView::OutOfProcessWebView> m_web_view;

    size_t m_tip_index { 0 };
    Vector<String> m_tips;
};
}
