/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Icon.h>
#include <LibGUI/Widget.h>
#include <Services/LoginServer/LoginWindow.h>
#include <Services/LoginServer/LoginWindowGML.h>

LoginWindow::LoginWindow(GUI::Window* parent)
    : GUI::Window(parent)
{
    set_title("Log in to SerenityOS");
    resize(413, 170);
    center_on_screen();
    set_resizable(false);
    set_minimizable(false);
    set_closeable(false);
    set_icon(GUI::Icon::default_icon("ladyball").bitmap_for_size(16));

    auto& widget = set_main_widget<GUI::Widget>();
    widget.load_from_gml(login_window_gml);
    m_banner = *widget.find_descendant_of_type_named<GUI::ImageWidget>("banner");
    m_banner->load_from_file("/res/graphics/brand-banner.png");
    m_banner->set_auto_resize(true);

    m_username = *widget.find_descendant_of_type_named<GUI::TextBox>("username");
    m_username->set_focus(true);
    m_password = *widget.find_descendant_of_type_named<GUI::PasswordBox>("password");

    m_log_in_button = *widget.find_descendant_of_type_named<GUI::Button>("log_in");
    m_log_in_button->on_click = [&](auto) {
        if (on_submit)
            on_submit();
    };
    m_log_in_button->set_default(true);

    m_fail_message = *widget.find_descendant_of_type_named<GUI::Label>("fail_message");
    m_username->on_change = [&] {
        m_fail_message->set_text("");
    };
    m_password->on_change = [&] {
        if (!m_password->text().is_empty())
            m_fail_message->set_text("");
    };
}
