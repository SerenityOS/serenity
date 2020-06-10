/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include "CreateNewLayerDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>

namespace PixelPaint {

CreateNewLayerDialog::CreateNewLayerDialog(const Gfx::IntSize& suggested_size, GUI::Window* parent_window)
    : Dialog(parent_window)
{
    set_title("Create new layer");
    resize(200, 200);

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);

    auto& layout = main_widget.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 4, 4, 4, 4 });

    auto& name_label = main_widget.add<GUI::Label>("Name:");
    name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    m_name_textbox = main_widget.add<GUI::TextBox>();
    m_name_textbox->on_change = [this] {
        m_layer_name = m_name_textbox->text();
    };

    auto& width_label = main_widget.add<GUI::Label>("Width:");
    width_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& width_spinbox = main_widget.add<GUI::SpinBox>();

    auto& height_label = main_widget.add<GUI::Label>("Height:");
    height_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& height_spinbox = main_widget.add<GUI::SpinBox>();

    auto& button_container = main_widget.add<GUI::Widget>();
    button_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& ok_button = button_container.add<GUI::Button>("OK");
    ok_button.on_click = [this](auto) {
        done(ExecOK);
    };

    auto& cancel_button = button_container.add<GUI::Button>("Cancel");
    cancel_button.on_click = [this](auto) {
        done(ExecCancel);
    };

    width_spinbox.on_change = [this](int value) {
        m_layer_size.set_width(value);
    };

    height_spinbox.on_change = [this](int value) {
        m_layer_size.set_height(value);
    };

    width_spinbox.set_range(0, 16384);
    height_spinbox.set_range(0, 16384);

    width_spinbox.set_value(suggested_size.width());
    height_spinbox.set_value(suggested_size.height());
}

}
