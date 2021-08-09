/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Guide.h"
#include <LibGUI/Dialog.h>

namespace PixelPaint {

class CreateNewGuideDialog final : public GUI::Dialog {
    C_OBJECT(CreateNewGuideDialog);

public:
    String const offset() const { return m_offset; }
    Guide::Orientation orientation() const { return m_orientation; }

private:
    CreateNewGuideDialog(GUI::Window* parent_window);

    String m_offset;
    Guide::Orientation m_orientation;

    bool m_is_horizontal_checked { false };
    bool m_is_vertical_checked { false };
};

}
