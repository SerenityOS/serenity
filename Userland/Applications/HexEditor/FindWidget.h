/*
 * Copyright (c) 2023, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace HexEditor {

class FindWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(FindWidget)
public:
    static ErrorOr<NonnullRefPtr<FindWidget>> try_create();
    virtual ~FindWidget() override = default;

private:
    FindWidget() = default;
};

}
