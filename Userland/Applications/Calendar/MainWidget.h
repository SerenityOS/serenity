/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Calendar {

class MainWidget : public GUI::Widget {
    C_OBJECT(MainWidget);

public:
    MainWidget() = default;
    virtual ~MainWidget() = default;

    static ErrorOr<NonnullRefPtr<MainWidget>> try_create();
};

}
