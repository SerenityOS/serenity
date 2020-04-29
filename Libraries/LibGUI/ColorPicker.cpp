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
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Palette.h>

namespace GUI {

class ColorButton : public AbstractButton {
    C_OBJECT(ColorButton)

public:
    virtual ~ColorButton() override;

    void set_selected(bool selected);
    Color color() const { return m_color; }

    Function<void(const Color)> on_click;

protected:
    virtual void click() override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void paint_event(PaintEvent&) override;

private:
    explicit ColorButton(ColorPicker& picker, Color color = {});

    ColorPicker& m_picker;
    Color m_color;
    bool m_selected { false };
};

class CustomColorWidget final : public GUI::Widget {
    C_OBJECT(CustomColorWidget);

public:
    Function<void(Color)> on_pick;
    void clear_last_position();

private:
    CustomColorWidget();

    RefPtr<Gfx::Bitmap> m_custom_colors;
    bool m_being_pressed { false };
    Gfx::Point m_last_position;

    void pick_color_at_position(GUI::MouseEvent& event);

    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
};

ColorPicker::ColorPicker(Color color, Window* parent_window, String title)
    : Dialog(parent_window)
    , m_color(color)
{
    set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/color-chooser.png"));
    set_title(title);
    set_resizable(false);
    resize(530, 325);

    build_ui();
}

ColorPicker::~ColorPicker()
{
}

void ColorPicker::build_ui()
{
    auto& root_container = set_main_widget<Widget>();
    root_container.set_layout<VerticalBoxLayout>();
    root_container.layout()->set_margins({ 4, 4, 4, 4 });
    root_container.set_fill_with_background_color(true);

    auto& tab_widget = root_container.add<GUI::TabWidget>();

    auto& tab_palette = tab_widget.add_tab<Widget>("Palette");
    tab_palette.set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
    tab_palette.set_layout<VerticalBoxLayout>();
    tab_palette.layout()->set_margins({ 4, 4, 4, 4 });
    tab_palette.layout()->set_spacing(4);

    build_ui_palette(tab_palette);

    auto& tab_custom_color = tab_widget.add_tab<Widget>("Custom Color");
    tab_custom_color.set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
    tab_custom_color.set_layout<VerticalBoxLayout>();
    tab_custom_color.layout()->set_margins({ 4, 4, 4, 4 });
    tab_custom_color.layout()->set_spacing(4);

    build_ui_custom(tab_custom_color);

    auto& button_container = root_container.add<Widget>();
    button_container.set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_container.set_preferred_size(0, 22);
    button_container.set_layout<HorizontalBoxLayout>();
    button_container.layout()->set_spacing(4);
    button_container.layout()->add_spacer();

    auto& cancel_button = button_container.add<Button>();
    cancel_button.set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    cancel_button.set_preferred_size(80, 0);
    cancel_button.set_text("Cancel");
    cancel_button.on_click = [this] {
        done(ExecCancel);
    };

    auto& ok_button = button_container.add<Button>();
    ok_button.set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    ok_button.set_preferred_size(80, 0);
    ok_button.set_text("Select");
    ok_button.on_click = [this] {
        done(ExecOK);
    };
}

void ColorPicker::build_ui_palette(Widget& root_container)
{
    unsigned colors[4][9] = {
        { 0xef2929, 0xf0b143, 0xfce94f, 0x9fe13a, 0x7c9ece, 0xa680a8, 0xe1ba70, 0x888a85, 0xeeeeec },
        { 0xba1e09, 0xf57900, 0xe9d51a, 0x8bd121, 0x4164a3, 0x6f517b, 0xb77f19, 0x555753, 0xd4d7cf },
        { 0x961605, 0xbf600c, 0xe9d51a, 0x619910, 0x2b4986, 0x573666, 0x875b09, 0x2f3436, 0xbbbdb6 },
        { 0x000000, 0x2f3436, 0x555753, 0x808080, 0xbabdb6, 0xd3d7cf, 0xeeeeec, 0xf3f3f3, 0xffffff }
    };

    for (int r = 0; r < 4; r++) {
        auto& colors_row = root_container.add<Widget>();
        colors_row.set_layout<HorizontalBoxLayout>();
        colors_row.set_size_policy(SizePolicy::Fill, SizePolicy::Fill);

        for (int i = 0; i < 8; i++) {
            create_color_button(colors_row, colors[r][i]);
        }
    }
}

void ColorPicker::build_ui_custom(Widget& root_container)
{
    enum RGBComponent {
        Red,
        Green,
        Blue
    };

    auto& horizontal_container = root_container.add<Widget>();
    horizontal_container.set_fill_with_background_color(true);
    horizontal_container.set_layout<HorizontalBoxLayout>();

    // Left Side
    m_custom_color = horizontal_container.add<GUI::CustomColorWidget>();
    m_custom_color->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
    m_custom_color->on_pick = [this](Color color) {
        if (m_color == color)
            return;

        m_color = color;
        update_color_widgets();
    };

    // Right Side
    auto& vertical_container = horizontal_container.add<Widget>();
    vertical_container.set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    vertical_container.set_layout<VerticalBoxLayout>();
    vertical_container.layout()->set_margins({ 4, 0, 0, 0 });
    vertical_container.set_preferred_size(150, 0);

    // Preview
    m_preview_widget = vertical_container.add<Frame>();
    m_preview_widget->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_preview_widget->set_preferred_size(0, 150);
    m_preview_widget->set_fill_with_background_color(true);

    auto pal = m_preview_widget->palette();
    pal.set_color(ColorRole::Background, m_color);
    m_preview_widget->set_palette(pal);

    vertical_container.layout()->add_spacer();

    // HTML
    auto& html_container = vertical_container.add<GUI::Widget>();
    html_container.set_layout<GUI::HorizontalBoxLayout>();
    html_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    html_container.set_preferred_size(0, 22);

    auto& html_label = html_container.add<GUI::Label>();
    html_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    html_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    html_label.set_preferred_size({ 70, 0 });
    html_label.set_text("HTML:");

    m_html_text = html_container.add<GUI::TextBox>();
    m_html_text->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    m_html_text->set_text(m_color_has_alpha_channel ? m_color.to_string() : m_color.to_string_without_alpha());
    m_html_text->on_change = [this]() {
        auto color_name = this->m_html_text->text();
        auto optional_color = Color::from_string(color_name);
        if (optional_color.has_value()) {
            auto color = optional_color.value();
            if (m_color == color)
                return;

            m_color = optional_color.value();
            this->m_custom_color->clear_last_position();
            update_color_widgets();
        }
    };

    // RGB Lines
    auto make_spinbox = [&](RGBComponent component, int initial_value) {
        auto& rgb_container = vertical_container.add<GUI::Widget>();
        rgb_container.set_layout<GUI::HorizontalBoxLayout>();
        rgb_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        rgb_container.set_preferred_size(0, 22);

        auto& rgb_label = rgb_container.add<GUI::Label>();
        rgb_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        rgb_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
        rgb_label.set_preferred_size({ 70, 0 });

        auto& spinbox = rgb_container.add<SpinBox>();
        spinbox.set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        spinbox.set_preferred_size(0, 20);
        spinbox.set_min(0);
        spinbox.set_max(255);
        spinbox.set_value(initial_value);
        spinbox.on_change = [this, component](auto value) {
            auto color = m_color;

            if (component == Red)
                color.set_red(value);
            if (component == Green)
                color.set_green(value);
            if (component == Blue)
                color.set_blue(value);

            if (m_color == color)
                return;

            m_color = color;

            this->m_custom_color->clear_last_position();
            update_color_widgets();
        };

        if (component == Red) {
            rgb_label.set_text("Red:");
            m_red_spinbox = spinbox;
        } else if (component == Green) {
            rgb_label.set_text("Green:");
            m_green_spinbox = spinbox;
        } else if (component == Blue) {
            rgb_label.set_text("Blue:");
            m_blue_spinbox = spinbox;
        }
    };

    make_spinbox(Red, m_color.red());
    make_spinbox(Green, m_color.green());
    make_spinbox(Blue, m_color.blue());
}

void ColorPicker::update_color_widgets()
{
    auto pal = m_preview_widget->palette();
    pal.set_color(ColorRole::Background, m_color);
    m_preview_widget->set_palette(pal);
    m_preview_widget->update();

    m_html_text->set_text(m_color_has_alpha_channel ? m_color.to_string() : m_color.to_string_without_alpha());

    m_red_spinbox->set_value(m_color.red());
    m_green_spinbox->set_value(m_color.green());
    m_blue_spinbox->set_value(m_color.blue());
}

void ColorPicker::create_color_button(Widget& container, unsigned rgb)
{
    Color color = Color::from_rgb(rgb);

    auto& widget = container.add<ColorButton>(*this, color);
    widget.set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
    widget.on_click = [this](Color color) {
        for (auto& value : m_color_widgets) {
            value->set_selected(false);
            value->update();
        }

        this->m_color = color;
    };

    if (color == m_color) {
        widget.set_selected(true);
    }

    m_color_widgets.append(&widget);
}

ColorButton::ColorButton(ColorPicker& picker, Color color)
    : m_picker(picker)
{
    m_color = color;
}

ColorButton::~ColorButton()
{
}

void ColorButton::set_selected(bool selected)
{
    m_selected = selected;
}

void ColorButton::doubleclick_event(GUI::MouseEvent&)
{
    click();
    m_selected = true;
    m_picker.done(Dialog::ExecOK);
}

void ColorButton::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::StylePainter::paint_button(painter, rect(), palette(), Gfx::ButtonStyle::Normal, is_being_pressed(), is_hovered(), is_checked(), is_enabled());

    painter.fill_rect({ 1, 1, rect().width() - 2, rect().height() - 2 }, m_color);

    if (m_selected) {
        painter.fill_rect({ 3, 3, rect().width() - 6, rect().height() - 6 }, Color::Black);
        painter.fill_rect({ 5, 5, rect().width() - 10, rect().height() - 10 }, Color::White);
        painter.fill_rect({ 7, 6, rect().width() - 14, rect().height() - 14 }, m_color);
    }
}

void ColorButton::click()
{
    if (on_click)
        on_click(m_color);

    m_selected = true;
}

CustomColorWidget::CustomColorWidget()
{
    m_custom_colors = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { 360, 512 });
    auto painter = Gfx::Painter(*m_custom_colors);

    for (int h = 0; h < 360; h++) {
        Gfx::HSV hsv;
        hsv.hue = h / 2;

        hsv.saturation = 255;
        for (int v = 0; v < 256; v++) {
            hsv.value = v;

            Color color = Color::from_hsv(hsv);
            painter.set_pixel({ h, v }, color);
        }

        hsv.value = 255;
        for (int s = 0; s < 256; s++) {
            hsv.saturation = 255 - s;

            Color color = Color::from_hsv(hsv);
            painter.set_pixel({ h, 256 + s }, color);
        }
    }
}

void CustomColorWidget::clear_last_position()
{
    m_last_position = { -1, -1 };
    update();
}

void CustomColorWidget::pick_color_at_position(GUI::MouseEvent& event)
{
    if (!m_being_pressed)
        return;

    auto position = event.position();
    if (!rect().contains(position))
        return;

    auto color = m_custom_colors->get_pixel(position);
    m_last_position = position;

    if (on_pick)
        on_pick(color);

    update();
}

void CustomColorWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Left) {
        m_being_pressed = true;
        pick_color_at_position(event);
    }
}

void CustomColorWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Left) {
        m_being_pressed = false;
        pick_color_at_position(event);
    }
}

void CustomColorWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (event.buttons() & GUI::MouseButton::Left)
        pick_color_at_position(event);
}

void CustomColorWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    Gfx::Rect rect = event.rect();

    painter.add_clip_rect(rect);

    painter.draw_scaled_bitmap(rect, *m_custom_colors, m_custom_colors->rect());

    painter.draw_line({ m_last_position.x(), 0 }, { m_last_position.x(), rect.height() }, Color::Black);
    painter.draw_line({ 0, m_last_position.y() }, { rect.width(), m_last_position.y() }, Color::Black);
}

void CustomColorWidget::resize_event(ResizeEvent&)
{
    clear_last_position();
}

}
