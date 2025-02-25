/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace ThemeEditor {

class FlagProperty : public GUI::Frame {
    C_OBJECT_ABSTRACT(FlagProperty)

public:
    static ErrorOr<NonnullRefPtr<FlagProperty>> try_create();
    virtual ~FlagProperty() override = default;

private:
    FlagProperty() = default;
};
}
