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

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ColorPicker.h>
#include <LibGUI/Frame.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Palette.h>

namespace GUI {

ColorPicker::ColorPicker(Color color, Core::Object* parent)
    : Dialog(parent)
    , m_color(color)
{
    set_title("Edit Color");
    build();
}

ColorPicker::~ColorPicker()
{
}

void ColorPicker::build()
{
    auto horizontal_container = Widget::construct();
    horizontal_container->set_fill_with_background_color(true);
    horizontal_container->set_layout(make<HorizontalBoxLayout>());
    horizontal_container->layout()->set_margins({ 4, 4, 4, 4 });
    set_main_widget(horizontal_container);

    auto left_vertical_container = horizontal_container->add<Widget>();
    left_vertical_container->set_layout(make<VerticalBoxLayout>());

    auto right_vertical_container = horizontal_container->add<Widget>();
    right_vertical_container->set_layout(make<VerticalBoxLayout>());

    enum RGBComponent {
        Red,
        Green,
        Blue
    };

    m_preview_widget = right_vertical_container->add<Frame>();
    auto pal = m_preview_widget->palette();
    pal.set_color(ColorRole::Background, m_color);
    m_preview_widget->set_fill_with_background_color(true);
    m_preview_widget->set_palette(pal);
    right_vertical_container->layout()->add_spacer();
    auto cancel_button = right_vertical_container->add<Button>("Cancel");
    cancel_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    cancel_button->set_preferred_size(0, 20);
    cancel_button->on_click = [&] {
        done(Dialog::ExecCancel);
    };
    auto ok_button = right_vertical_container->add<Button>("Okay");
    ok_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    ok_button->set_preferred_size(0, 20);
    ok_button->on_click = [&] {
        done(Dialog::ExecOK);
    };

    auto make_spinbox = [&](RGBComponent component, int initial_value) {
        auto spinbox = left_vertical_container->add<SpinBox>();
        spinbox->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        spinbox->set_preferred_size(0, 20);
        spinbox->set_min(0);
        spinbox->set_max(255);
        spinbox->set_value(initial_value);

        spinbox->on_change = [this, component](auto value) {
            if (component == Red)
                m_color.set_red(value);
            if (component == Green)
                m_color.set_green(value);
            if (component == Blue)
                m_color.set_blue(value);

            auto pal = m_preview_widget->palette();
            pal.set_color(ColorRole::Background, m_color);
            m_preview_widget->set_palette(pal);
            m_preview_widget->update();
        };
        return spinbox;
    };

    make_spinbox(Red, m_color.red());
    make_spinbox(Green, m_color.green());
    make_spinbox(Blue, m_color.blue());
}

}
