/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractButton.h>
#include <LibGUI/Dialog.h>

namespace GUI {

class ColorButton;
class ColorPreview;
class CustomColorWidget;
class ColorSelectOverlay;

class ColorPicker final : public Dialog {
    C_OBJECT(ColorPicker)

public:
    virtual ~ColorPicker() override = default;

    bool color_has_alpha_channel() const { return m_color_has_alpha_channel; }
    void set_color_has_alpha_channel(bool);
    Color color() const { return m_color; }
    Function<void(Color)> on_color_changed;

private:
    explicit ColorPicker(Color, Window* parent_window = nullptr, ByteString title = "Color Picker");

    void build_ui();
    void build_ui_custom(Widget& root_container);
    void build_ui_palette(Widget& root_container);
    void update_color_widgets();
    void create_color_button(Widget& container, unsigned rgb);

    Color m_original_color;
    Color m_color;
    bool m_color_has_alpha_channel { true };

    Vector<ColorButton&> m_color_widgets;
    RefPtr<CustomColorWidget> m_custom_color;
    RefPtr<GUI::VerticalOpacitySlider> m_alpha;
    RefPtr<ColorPreview> m_preview_widget;
    RefPtr<Button> m_selector_button;
    RefPtr<TextBox> m_html_text;
    RefPtr<SpinBox> m_red_spinbox;
    RefPtr<SpinBox> m_green_spinbox;
    RefPtr<SpinBox> m_blue_spinbox;
    RefPtr<SpinBox> m_alpha_spinbox;
};

}
