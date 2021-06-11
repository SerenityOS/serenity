/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>

namespace PixelPaint {

class CreateNewLayerDialog final : public GUI::Dialog {
    C_OBJECT(CreateNewLayerDialog);

public:
    Gfx::IntSize const& layer_size() const { return m_layer_size; }
    String const& layer_name() const { return m_layer_name; }

private:
    CreateNewLayerDialog(Gfx::IntSize const& suggested_size, GUI::Window* parent_window);

    Gfx::IntSize m_layer_size;
    String m_layer_name;

    RefPtr<GUI::TextBox> m_name_textbox;
};

}
