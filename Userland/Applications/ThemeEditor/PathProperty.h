/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace ThemeEditor {

class PathProperty : public GUI::Frame {
    C_OBJECT_ABSTRACT(PathProperty)

public:
    static ErrorOr<NonnullRefPtr<PathProperty>> try_create();
    virtual ~PathProperty() override = default;

private:
    PathProperty() = default;
};
}
