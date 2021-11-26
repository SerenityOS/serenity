/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SettingsDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>

SettingsDialog::SettingsDialog(GUI::Window* parent, String player_name)
    : GUI::Dialog(parent)
    , m_player_name(move(player_name))
{
    set_rect({ 0, 0, 250, 75 });
    set_title("Settings");
    set_icon(parent->icon());
    set_resizable(false);

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);

    auto& layout = main_widget.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins(4);

    auto& name_box = main_widget.add<GUI::Widget>();
    auto& input_layout = name_box.set_layout<GUI::HorizontalBoxLayout>();
    input_layout.set_spacing(4);

    auto& name_label = name_box.add<GUI::Label>("Name:");
    name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& textbox = name_box.add<GUI::TextBox>();
    textbox.set_text(m_player_name);
    textbox.on_change = [&] {
        m_player_name = textbox.text();
    };

    auto& button_box = main_widget.add<GUI::Widget>();
    auto& button_layout = button_box.set_layout<GUI::HorizontalBoxLayout>();
    button_layout.set_spacing(10);

    button_box.add<GUI::Button>("Cancel").on_click = [this](auto) {
        done(Dialog::ExecCancel);
    };

    button_box.add<GUI::Button>("OK").on_click = [this](auto) {
        done(Dialog::ExecOK);
    };
}
