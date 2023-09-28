/*
 * Copyright (c) 2023, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace HexEditor {

class GoToOffsetWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(GoToOffsetWidget)
public:
    static ErrorOr<NonnullRefPtr<GoToOffsetWidget>> try_create();
    virtual ~GoToOffsetWidget() override = default;

private:
    GoToOffsetWidget() = default;
};

}
