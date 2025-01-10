/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace ThemeEditor {

class WindowThemeProperty : public GUI::Frame {
    C_OBJECT_ABSTRACT(WindowThemeProperty)

public:
    static ErrorOr<NonnullRefPtr<WindowThemeProperty>> try_create();
    virtual ~WindowThemeProperty() override = default;

private:
    WindowThemeProperty() = default;
};
}
