/*
 * Copyright (c) 2024, circl <circl.lastname@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Screenshot {

class MainWidget : public GUI::Widget {
    C_OBJECT(MainWidget)
public:
    MainWidget() = default;
    static ErrorOr<NonnullRefPtr<MainWidget>> try_create();

    virtual ~MainWidget() override = default;
};

}
