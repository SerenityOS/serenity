/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace ThemeEditor {

class ColorProperty : public GUI::Frame {
    C_OBJECT_ABSTRACT(ColorProperty)

public:
    static ErrorOr<NonnullRefPtr<ColorProperty>> try_create();
    virtual ~ColorProperty() override = default;

private:
    ColorProperty() = default;
};
}
