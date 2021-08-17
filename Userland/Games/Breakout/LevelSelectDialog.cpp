/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LevelSelectDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>

namespace Breakout {

LevelSelectDialog::LevelSelectDialog(Window* parent_window)
    : Dialog(parent_window)
{
    set_rect(0, 0, 300, 250);
    set_title("Level Select");
    build();
}

LevelSelectDialog::~LevelSelectDialog()
{
}

int LevelSelectDialog::show(int& board_number, Window* parent_window)
{
    auto box = LevelSelectDialog::construct(parent_window);
    box->set_resizable(false);
    if (parent_window)
        box->set_icon(parent_window->icon());
    auto result = box->exec();
    board_number = box->level();
    return result;
}

void LevelSelectDialog::build()
{
    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);

    auto& layout = main_widget.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins(4);

    main_widget.add<GUI::Label>("Choose a level").set_text_alignment(Gfx::TextAlignment::Center);

    auto& level_list = main_widget.add<GUI::Widget>();
    auto& scroll_layout = level_list.set_layout<GUI::VerticalBoxLayout>();
    scroll_layout.set_spacing(4);

    level_list.add<GUI::Button>("Rainbow").on_click = [this](auto) {
        m_level = -1;
        done(Dialog::ExecOK);
    };

    level_list.add<GUI::Button>(":^)").on_click = [this](auto) {
        m_level = 0;
        done(Dialog::ExecOK);
    };
}
}
