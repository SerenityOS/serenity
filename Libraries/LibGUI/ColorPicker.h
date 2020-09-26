/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/AbstractButton.h>
#include <LibGUI/Dialog.h>

namespace GUI {

class ColorButton;
class ColorPreview;
class CustomColorWidget;

class ColorPicker final : public Dialog {
    C_OBJECT(ColorPicker)

public:
    virtual ~ColorPicker() override;

    bool color_has_alpha_channel() const { return m_color_has_alpha_channel; }
    void set_color_has_alpha_channel(bool);
    Color color() const { return m_color; }

private:
    explicit ColorPicker(Color, Window* parent_window = nullptr, String title = "Edit Color");

    void build_ui();
    void build_ui_custom(Widget& root_container);
    void build_ui_palette(Widget& root_container);
    void update_color_widgets();
    void create_color_button(Widget& container, unsigned rgb);

    Color m_color;
    bool m_color_has_alpha_channel { true };

    Vector<ColorButton*> m_color_widgets;
    RefPtr<CustomColorWidget> m_custom_color;
    RefPtr<ColorPreview> m_preview_widget;
    RefPtr<TextBox> m_html_text;
    RefPtr<SpinBox> m_red_spinbox;
    RefPtr<SpinBox> m_green_spinbox;
    RefPtr<SpinBox> m_blue_spinbox;
    RefPtr<SpinBox> m_alpha_spinbox;
};

}
