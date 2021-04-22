/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>

namespace PixelPaint {

class CreateNewImageDialog final : public GUI::Dialog {
    C_OBJECT(CreateNewImageDialog)

public:
    const Gfx::IntSize& image_size() const { return m_image_size; }
    const String& image_name() const { return m_image_name; }

private:
    CreateNewImageDialog(GUI::Window* parent_window);

    String m_image_name;
    Gfx::IntSize m_image_size;

    RefPtr<GUI::TextBox> m_name_textbox;
};

}
