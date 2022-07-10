/*
 * Copyright (c) 2022, Karol Kosek <krkk@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Image.h"
#include <LibGUI/Dialog.h>

namespace PixelPaint {

class ExportPNGDialog final : public GUI::Dialog {
    C_OBJECT(ExportPNGDialog);

private:
    ExportPNGDialog(Core::File&, Image const&, GUI::Window* parent_window);

    Core::File& m_file;
    Image const& m_image;
    RefPtr<GUI::CheckBox> m_transparency_checkbox;
    RefPtr<GUI::ValueSlider> m_compression_slider;
};

}
