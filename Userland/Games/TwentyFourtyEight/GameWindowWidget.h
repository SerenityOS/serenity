/*
 * Copyright (c) 2024, Aryan Baburajan <aryanbaburajan2007@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace TwentyFourtyEight {

class GameWindowWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(GameWindowWidget)

public:
    static ErrorOr<NonnullRefPtr<GameWindowWidget>> try_create();
    virtual ~GameWindowWidget() override = default;

private:
    GameWindowWidget() = default;
};

}
