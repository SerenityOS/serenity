/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace ThemeEditor {

class AlignmentProperty : public GUI::Frame {
    C_OBJECT_ABSTRACT(AlignmentProperty)

public:
    static ErrorOr<NonnullRefPtr<AlignmentProperty>> try_create();
    virtual ~AlignmentProperty() override = default;

private:
    AlignmentProperty() = default;
};
}
