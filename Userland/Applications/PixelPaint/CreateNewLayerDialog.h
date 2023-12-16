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
    Gfx::IntSize layer_size() const { return m_layer_size; }
    ByteString const& layer_name() const { return m_layer_name; }

private:
    static constexpr StringView default_layer_name = "Layer"sv;

    CreateNewLayerDialog(Gfx::IntSize suggested_size, GUI::Window* parent_window);

    Gfx::IntSize m_layer_size;
    ByteString m_layer_name { default_layer_name };

    RefPtr<GUI::TextBox> m_name_textbox;
};

}
