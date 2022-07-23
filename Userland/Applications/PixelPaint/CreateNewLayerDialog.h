/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Layers/Layer.h"
#include <LibGUI/Dialog.h>

namespace PixelPaint {

class CreateNewLayerDialog final : public GUI::Dialog {
    C_OBJECT(CreateNewLayerDialog);

public:
    Gfx::IntSize const& layer_size() const { return m_layer_size; }
    String const& layer_name() const { return m_layer_name; }
    Layer::LayerType layer_type() const { return m_layer_type; }
    Gfx::Color layer_color() const { return m_layer_color; }

private:
    CreateNewLayerDialog(Gfx::IntSize const& suggested_size, GUI::Window* parent_window);

    Gfx::IntSize m_layer_size;
    String m_layer_name;

    Layer::LayerType m_layer_type { Layer::LayerType::BitmapLayer };
    Gfx::Color m_layer_color { Gfx::Color::White };

    RefPtr<GUI::TextBox> m_name_textbox;
};

}
