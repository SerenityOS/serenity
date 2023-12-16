/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Guide.h"
#include "ImageEditor.h"
#include <LibGUI/Dialog.h>

namespace PixelPaint {

class EditGuideDialog final : public GUI::Dialog {
    C_OBJECT(EditGuideDialog);

public:
    ByteString const offset() const { return m_offset; }
    Guide::Orientation orientation() const { return m_orientation; }

    Optional<float> offset_as_pixel(ImageEditor const&);

private:
    EditGuideDialog(GUI::Window* parent_window, ByteString const& offset = {}, Guide::Orientation orientation = Guide::Orientation::Unset);

    ByteString m_offset;
    Guide::Orientation m_orientation;
    RefPtr<GUI::TextBox> m_offset_text_box;
    bool m_is_horizontal_checked { false };
    bool m_is_vertical_checked { false };
};

}
