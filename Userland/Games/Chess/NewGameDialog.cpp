/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NewGameDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>

namespace Chess {

ErrorOr<NonnullRefPtr<NewGameDialog>> NewGameDialog::try_create(GUI::Window* parent_window)
{
    auto new_game_widget = TRY(Chess::NewGameWidget::try_create());
    auto new_game_dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) NewGameDialog(move(new_game_widget), move(parent_window))));
    return new_game_dialog;
}

NewGameDialog::NewGameDialog(NonnullRefPtr<Chess::NewGameWidget> new_game_widget, GUI::Window* parent_window)
    : GUI::Dialog(parent_window)
{
    set_title("New Game");
    set_main_widget(new_game_widget);

    auto start_button = new_game_widget->find_descendant_of_type_named<GUI::Button>("start_button");
    start_button->on_click = [this](auto) {
        done(ExecResult::OK);
    };
}

void NewGameDialog::event(Core::Event& event)
{
    GUI::Dialog::event(event);
}

}
