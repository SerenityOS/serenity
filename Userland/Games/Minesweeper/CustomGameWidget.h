/*
 * Copyright (c) 2023, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Minesweeper {

class CustomGameWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(CustomGameWidget)
public:
    static ErrorOr<NonnullRefPtr<CustomGameWidget>> try_create();
    virtual ~CustomGameWidget() override = default;

private:
    CustomGameWidget() = default;
};

}
