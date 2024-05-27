/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ChessWidget.h"
#include "NewGameWidget.h"
#include <LibGUI/Dialog.h>

namespace Chess {

class NewGameDialog final : public GUI::Dialog {
    C_OBJECT_ABSTRACT(NewGameDialog)
public:
    static ErrorOr<NonnullRefPtr<NewGameDialog>> try_create(GUI::Window* parent_window);

private:
    NewGameDialog(NonnullRefPtr<Chess::NewGameWidget> new_game_widget_widget, GUI::Window* parent_window);
    virtual void event(Core::Event&) override;
};

}
