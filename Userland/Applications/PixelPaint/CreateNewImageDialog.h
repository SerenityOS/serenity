/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>
#include <LibGfx/Color.h>

namespace PixelPaint {

class CreateNewImageDialog final : public GUI::Dialog {
    C_OBJECT(CreateNewImageDialog)

public:
    Gfx::IntSize image_size() const { return m_image_size; }
    ByteString const& image_name() const { return m_image_name; }
    Gfx::Color background_color() const { return m_background_color; }

private:
    CreateNewImageDialog(GUI::Window* parent_window);

    ByteString m_image_name;
    Gfx::IntSize m_image_size;
    Gfx::Color m_background_color {};

    RefPtr<GUI::TextBox> m_name_textbox;
};

}
