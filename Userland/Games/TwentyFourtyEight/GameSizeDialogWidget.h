/*
 * Copyright (c) 2024, Aryan Baburajan <aryanbaburajan2007@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace TwentyFourtyEight {

class GameSizeDialogWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(GameSizeDialogWidget)

public:
    static ErrorOr<NonnullRefPtr<GameSizeDialogWidget>> try_create();
    virtual ~GameSizeDialogWidget() override = default;

private:
    GameSizeDialogWidget() = default;
};

}
