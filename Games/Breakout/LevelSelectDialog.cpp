/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    layout.set_margins({ 4, 4, 4, 4 });

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
