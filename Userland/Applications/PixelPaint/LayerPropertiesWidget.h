/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ColorInput.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

class Layer;

class LayerPropertiesWidget final : public GUI::Widget {
    C_OBJECT(LayerPropertiesWidget);

public:
    virtual ~LayerPropertiesWidget() override = default;

    void set_layer(Layer*);

private:
    LayerPropertiesWidget();

    RefPtr<GUI::CheckBox> m_visibility_checkbox;
    RefPtr<GUI::OpacitySlider> m_opacity_slider;
    RefPtr<GUI::TextBox> m_name_textbox;
    RefPtr<GUI::Widget> m_color_container;
    RefPtr<GUI::ColorInput> m_color_select;

    RefPtr<Layer> m_layer;
};

}
