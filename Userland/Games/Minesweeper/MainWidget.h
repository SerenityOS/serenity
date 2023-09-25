/*
 * Copyright (c) 2023, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Minesweeper {

class MainWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(MainWidget)
public:
    static ErrorOr<NonnullRefPtr<Minesweeper::MainWidget>> try_create();
    virtual ~MainWidget() override = default;

private:
    MainWidget() = default;
};

}
