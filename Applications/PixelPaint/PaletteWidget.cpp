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

#include "PaletteWidget.h"
#include "ImageEditor.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ColorPicker.h>
#include <LibGfx/Palette.h>

namespace PixelPaint {

class ColorWidget : public GUI::Frame {
    C_OBJECT(ColorWidget);

public:
    explicit ColorWidget(Color color, PaletteWidget& palette_widget)
        : m_palette_widget(palette_widget)
        , m_color(color)
    {
    }

    virtual ~ColorWidget() override
    {
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.modifiers() & KeyModifier::Mod_Ctrl && event.button() == GUI::MouseButton::Left) {
            auto dialog = GUI::ColorPicker::construct(m_color, window());
            if (dialog->exec() == GUI::Dialog::ExecOK) {
                m_color = dialog->color();
                auto pal = palette();
                pal.set_color(ColorRole::Background, m_color);
                set_palette(pal);
                update();
            }
            return;
        }

        if (event.button() == GUI::MouseButton::Left)
            m_palette_widget.set_primary_color(m_color);
        else if (event.button() == GUI::MouseButton::Right)
            m_palette_widget.set_secondary_color(m_color);
    }

private:
    PaletteWidget& m_palette_widget;
    Color m_color;
};

PaletteWidget::PaletteWidget(ImageEditor& editor)
    : m_editor(editor)
{
    set_frame_shape(Gfx::FrameShape::Panel);
    set_frame_shadow(Gfx::FrameShadow::Raised);
    set_frame_thickness(0);
    set_fill_with_background_color(true);

    set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    set_preferred_size(0, 34);

    m_secondary_color_widget = add<GUI::Frame>();
    m_secondary_color_widget->set_relative_rect({ 2, 2, 60, 31 });
    m_secondary_color_widget->set_fill_with_background_color(true);
    set_secondary_color(m_editor.secondary_color());

    m_primary_color_widget = add<GUI::Frame>();
    Gfx::IntRect rect { 0, 0, 38, 15 };
    rect.center_within(m_secondary_color_widget->relative_rect());
    m_primary_color_widget->set_relative_rect(rect);
    m_primary_color_widget->set_fill_with_background_color(true);
    set_primary_color(m_editor.primary_color());

    m_editor.on_primary_color_change = [this](Color color) {
        set_primary_color(color);
    };

    m_editor.on_secondary_color_change = [this](Color color) {
        set_secondary_color(color);
    };

    auto& color_container = add<GUI::Widget>();
    color_container.set_relative_rect(m_secondary_color_widget->relative_rect().right() + 2, 2, 500, 32);
    color_container.set_layout<GUI::VerticalBoxLayout>();
    color_container.layout()->set_spacing(1);

    auto& top_color_container = color_container.add<GUI::Widget>();
    top_color_container.set_layout<GUI::HorizontalBoxLayout>();
    top_color_container.layout()->set_spacing(1);

    auto& bottom_color_container = color_container.add<GUI::Widget>();
    bottom_color_container.set_layout<GUI::HorizontalBoxLayout>();
    bottom_color_container.layout()->set_spacing(1);

    auto add_color_widget = [&](GUI::Widget& container, Color color) {
        auto& color_widget = container.add<ColorWidget>(color, *this);
        color_widget.set_fill_with_background_color(true);
        auto pal = color_widget.palette();
        pal.set_color(ColorRole::Background, color);
        color_widget.set_palette(pal);
    };

    add_color_widget(top_color_container, Color::from_rgb(0x000000));
    add_color_widget(top_color_container, Color::from_rgb(0x808080));
    add_color_widget(top_color_container, Color::from_rgb(0x800000));
    add_color_widget(top_color_container, Color::from_rgb(0x808000));
    add_color_widget(top_color_container, Color::from_rgb(0x008000));
    add_color_widget(top_color_container, Color::from_rgb(0x008080));
    add_color_widget(top_color_container, Color::from_rgb(0x000080));
    add_color_widget(top_color_container, Color::from_rgb(0x800080));
    add_color_widget(top_color_container, Color::from_rgb(0x808040));
    add_color_widget(top_color_container, Color::from_rgb(0x004040));
    add_color_widget(top_color_container, Color::from_rgb(0x0080ff));
    add_color_widget(top_color_container, Color::from_rgb(0x004080));
    add_color_widget(top_color_container, Color::from_rgb(0x8000ff));
    add_color_widget(top_color_container, Color::from_rgb(0x804000));

    add_color_widget(bottom_color_container, Color::from_rgb(0xffffff));
    add_color_widget(bottom_color_container, Color::from_rgb(0xc0c0c0));
    add_color_widget(bottom_color_container, Color::from_rgb(0xff0000));
    add_color_widget(bottom_color_container, Color::from_rgb(0xffff00));
    add_color_widget(bottom_color_container, Color::from_rgb(0x00ff00));
    add_color_widget(bottom_color_container, Color::from_rgb(0x00ffff));
    add_color_widget(bottom_color_container, Color::from_rgb(0x0000ff));
    add_color_widget(bottom_color_container, Color::from_rgb(0xff00ff));
    add_color_widget(bottom_color_container, Color::from_rgb(0xffff80));
    add_color_widget(bottom_color_container, Color::from_rgb(0x00ff80));
    add_color_widget(bottom_color_container, Color::from_rgb(0x80ffff));
    add_color_widget(bottom_color_container, Color::from_rgb(0x8080ff));
    add_color_widget(bottom_color_container, Color::from_rgb(0xff0080));
    add_color_widget(bottom_color_container, Color::from_rgb(0xff8040));
}

PaletteWidget::~PaletteWidget()
{
}

void PaletteWidget::set_primary_color(Color color)
{
    m_editor.set_primary_color(color);
    auto pal = m_primary_color_widget->palette();
    pal.set_color(ColorRole::Background, color);
    m_primary_color_widget->set_palette(pal);
    m_primary_color_widget->update();
}

void PaletteWidget::set_secondary_color(Color color)
{
    m_editor.set_secondary_color(color);
    auto pal = m_secondary_color_widget->palette();
    pal.set_color(ColorRole::Background, color);
    m_secondary_color_widget->set_palette(pal);
    m_secondary_color_widget->update();
}

}
