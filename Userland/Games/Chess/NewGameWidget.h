/*
 * Copyright (c) 2024, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Chess {

class NewGameWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(NewGameWidget)
public:
    static ErrorOr<NonnullRefPtr<NewGameWidget>> try_create();
    virtual ~NewGameWidget() override = default;

private:
    NewGameWidget() = default;
};

}
