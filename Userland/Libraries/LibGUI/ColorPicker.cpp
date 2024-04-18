/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ColorPicker.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/OpacitySlider.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Palette.h>

namespace GUI {

class ColorButton : public AbstractButton {
    C_OBJECT(ColorButton);

public:
    virtual ~ColorButton() override = default;

    void set_selected(bool selected);
    Color color() const { return m_color; }

    Function<void(Color const)> on_click;

protected:
    virtual void click(unsigned modifiers = 0) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void paint_event(PaintEvent&) override;

private:
    explicit ColorButton(ColorPicker& picker, Color color = {});

    ColorPicker& m_picker;
    Color m_color;
    bool m_selected { false };
};

class ColorField final : public GUI::Frame {
    C_OBJECT(ColorField);

public:
    Function<void(Color)> on_pick;
    void set_color(Color);
    void set_hue(double);
    void set_hue_from_pick(double);

private:
    ColorField(Color color);

    Color m_color;
    // save hue separately so full white color doesn't reset it to 0
    double m_hue;

    RefPtr<Gfx::Bitmap> m_color_bitmap;
    bool m_being_pressed { false };
    Gfx::IntPoint m_last_position;

    void create_color_bitmap();
    void pick_color_at_position(GUI::MouseEvent& event);
    void recalculate_position();

    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
};

class ColorSlider final : public GUI::Frame {
    C_OBJECT(ColorSlider);

public:
    Function<void(double)> on_pick;
    void set_value(double);

private:
    ColorSlider(double value);

    double m_value;

    RefPtr<Gfx::Bitmap> m_color_bitmap;
    bool m_being_pressed { false };
    int m_last_position;

    void pick_value_at_position(GUI::MouseEvent& event);
    void recalculate_position();

    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
};

class ColorPreview final : public GUI::Widget {
    C_OBJECT(ColorPreview);

public:
    void set_color(Color);

private:
    ColorPreview(Color);

    Color m_color;
    virtual void paint_event(GUI::PaintEvent&) override;
};

class CustomColorWidget final : public GUI::Widget {
    C_OBJECT(CustomColorWidget);

public:
    Function<void(Color)> on_pick;
    void set_color(Color);

private:
    CustomColorWidget(Color);

    RefPtr<ColorField> m_color_field;
    RefPtr<ColorSlider> m_color_slider;
};

class ColorSelectOverlay final : public Widget {
    C_OBJECT(ColorSelectOverlay)
public:
    Optional<Color> exec()
    {
        m_event_loop = make<Core::EventLoop>();

        // FIXME: Allow creation of fully transparent windows without a backing store.
        auto window = Window::construct();
        window->set_main_widget(this);
        window->set_has_alpha_channel(true);
        window->set_fullscreen(true);
        window->set_frameless(true);
        window->show();

        if (!m_event_loop->exec())
            return {};
        return m_col;
    }

    virtual ~ColorSelectOverlay() override = default;
    Function<void(Color)> on_color_changed;

private:
    ColorSelectOverlay()
    {
        set_override_cursor(Gfx::StandardCursor::Eyedropper);
    }

    virtual void mousedown_event(GUI::MouseEvent&) override { m_event_loop->quit(1); }
    virtual void mousemove_event(GUI::MouseEvent&) override
    {
        auto new_col = ConnectionToWindowServer::the().get_color_under_cursor();
        if (!new_col.has_value())
            return;
        if (new_col == m_col)
            return;
        m_col = new_col.value();
        if (on_color_changed)
            on_color_changed(m_col);
    }

    virtual void keydown_event(GUI::KeyEvent& event) override
    {
        if (event.key() == KeyCode::Key_Escape) {
            event.accept();
            m_event_loop->quit(0);
            return;
        }
    }

    OwnPtr<Core::EventLoop> m_event_loop;
    Color m_col;
};

ColorPicker::ColorPicker(Color color, Window* parent_window, ByteString title)
    : Dialog(parent_window)
    , m_original_color(color)
    , m_color(color)
{
    set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/color-chooser.png"sv).release_value_but_fixme_should_propagate_errors());
    set_title(title);
    set_resizable(false);
    resize(480, 326);

    build_ui();
}

void ColorPicker::set_color_has_alpha_channel(bool has_alpha)
{
    if (m_color_has_alpha_channel == has_alpha)
        return;

    m_color_has_alpha_channel = has_alpha;
    update_color_widgets();
}

void ColorPicker::build_ui()
{
    auto root_container = set_main_widget<Widget>();
    root_container->set_layout<VerticalBoxLayout>(4);
    root_container->set_fill_with_background_color(true);

    auto& tab_widget = root_container->add<GUI::TabWidget>();

    auto& tab_palette = tab_widget.add_tab<Widget>("Palette"_string);
    tab_palette.set_layout<VerticalBoxLayout>(4, 4);

    build_ui_palette(tab_palette);

    auto& tab_custom_color = tab_widget.add_tab<Widget>("Custom Color"_string);
    tab_custom_color.set_layout<VerticalBoxLayout>(4, 4);

    build_ui_custom(tab_custom_color);

    auto& button_container = root_container->add<Widget>();
    button_container.set_preferred_height(GUI::SpecialDimension::Fit);
    button_container.set_layout<HorizontalBoxLayout>(4);
    button_container.add_spacer();

    auto& ok_button = button_container.add<DialogButton>();
    ok_button.set_text("OK"_string);
    ok_button.on_click = [this](auto) {
        if (on_color_changed)
            on_color_changed(m_color);
        done(ExecResult::OK);
    };
    ok_button.set_default(true);

    auto& cancel_button = button_container.add<DialogButton>();
    cancel_button.set_text("Cancel"_string);
    cancel_button.on_click = [this](auto) {
        if (on_color_changed)
            on_color_changed(m_original_color);
        done(ExecResult::Cancel);
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
        Blue,
        Alpha
    };

    auto& horizontal_container = root_container.add<Widget>();
    horizontal_container.set_fill_with_background_color(true);
    horizontal_container.set_layout<HorizontalBoxLayout>();

    // Left Side
    m_custom_color = horizontal_container.add<CustomColorWidget>(m_color);
    m_custom_color->set_preferred_size(299, 260);
    m_custom_color->on_pick = [this](Color color) {
        if (m_color == color) {
            // NOTE: This call to update() is needed so that when changing the vertical color slider with the initial Color::White
            //       selected value (which doesn't change with that slider as in all the slider's values the new color at that position
            //       will still be Color::White) the spinbox colors are updated.
            update();
            return;
        }

        m_alpha->set_base_color(color);
        m_color = color;
        update_color_widgets();
    };

    m_alpha = horizontal_container.add<GUI::VerticalOpacitySlider>();
    m_alpha->set_visible(m_color_has_alpha_channel);
    m_alpha->set_min(0);
    m_alpha->set_max(255);
    m_alpha->set_value(m_color.alpha());
    m_alpha->on_change = [this](auto value) {
        auto color = m_color;
        color.set_alpha(value);

        if (m_color == color)
            return;

        m_color = color;
        m_custom_color->set_color(color);
        update_color_widgets();
    };

    // Right Side
    auto& vertical_container = horizontal_container.add<Widget>();
    vertical_container.set_layout<VerticalBoxLayout>(GUI::Margins { 0, 0, 0, 8 });
    vertical_container.set_min_width(120);

    auto& preview_container = vertical_container.add<Frame>();
    preview_container.set_layout<VerticalBoxLayout>(2, 0);
    preview_container.set_fixed_height(100);

    // Current color
    preview_container.add<ColorPreview>(m_color);

    // Preview selected color
    m_preview_widget = preview_container.add<ColorPreview>(m_color);

    vertical_container.add_spacer();

    // HTML
    auto& html_container = vertical_container.add<GUI::Widget>();
    html_container.set_layout<GUI::HorizontalBoxLayout>();
    html_container.set_preferred_height(GUI::SpecialDimension::Fit);

    auto& html_label = html_container.add<GUI::Label>();
    html_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    html_label.set_preferred_width(48);
    html_label.set_text("HTML:"_string);

    m_html_text = html_container.add<GUI::TextBox>();
    m_html_text->set_text(m_color_has_alpha_channel ? m_color.to_byte_string() : m_color.to_byte_string_without_alpha());
    m_html_text->on_change = [this]() {
        auto color_name = m_html_text->text();
        auto optional_color = Color::from_string(color_name);
        if (optional_color.has_value() && (!color_name.starts_with('#') || color_name.length() == ((m_color_has_alpha_channel) ? 9 : 7))) {
            // The color length must be 9/7 (unless it is a name like red), because:
            //    - If we allowed 5/4 character rgb color, the field would reset to 9/7 characters after you deleted 4/3 characters.
            auto color = optional_color.value();
            if (m_color == color)
                return;

            m_color = optional_color.value();
            m_custom_color->set_color(color);
            update_color_widgets();
        }
    };

    // RGB Lines
    auto make_spinbox = [&](RGBComponent component, int initial_value) {
        auto& rgb_container = vertical_container.add<GUI::Widget>();
        rgb_container.set_layout<GUI::HorizontalBoxLayout>();
        rgb_container.set_preferred_height(GUI::SpecialDimension::Fit);

        auto& rgb_label = rgb_container.add<GUI::Label>();
        rgb_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        rgb_label.set_preferred_width(48);

        auto& spinbox = rgb_container.add<SpinBox>();
        spinbox.set_min(0);
        spinbox.set_max(255);
        spinbox.set_value(initial_value);
        spinbox.set_enabled(m_color_has_alpha_channel);
        spinbox.on_change = [this, component](auto value) {
            auto color = m_color;

            if (component == Red)
                color.set_red(value);
            if (component == Green)
                color.set_green(value);
            if (component == Blue)
                color.set_blue(value);
            if (component == Alpha)
                color.set_alpha(value);

            if (m_color == color)
                return;

            m_color = color;
            m_custom_color->set_color(color);
            update_color_widgets();
        };

        if (component == Red) {
            rgb_label.set_text("Red:"_string);
            m_red_spinbox = spinbox;
        } else if (component == Green) {
            rgb_label.set_text("Green:"_string);
            m_green_spinbox = spinbox;
        } else if (component == Blue) {
            rgb_label.set_text("Blue:"_string);
            m_blue_spinbox = spinbox;
        } else if (component == Alpha) {
            rgb_label.set_text("Alpha:"_string);
            m_alpha_spinbox = spinbox;
        }
    };

    make_spinbox(Red, m_color.red());
    make_spinbox(Green, m_color.green());
    make_spinbox(Blue, m_color.blue());
    make_spinbox(Alpha, m_color.alpha());

    m_selector_button = vertical_container.add<GUI::Button>("Select on Screen"_string);
    m_selector_button->on_click = [this](auto) {
        auto selector = ColorSelectOverlay::construct();
        auto original_color = m_color;
        // This allows us to use the color preview widget as a live-preview for
        // the color currently under the cursor, which is helpful.
        selector->on_color_changed = [this](auto color) {
            m_color = color;
            update_color_widgets();
        };

        // Set the final color
        auto maybe_color = selector->exec();
        m_color = maybe_color.value_or(original_color);
        m_custom_color->set_color(m_color);
        update_color_widgets();
    };
}

void ColorPicker::update_color_widgets()
{
    m_preview_widget->set_color(m_color);

    m_html_text->set_text(m_color_has_alpha_channel ? m_color.to_byte_string() : m_color.to_byte_string_without_alpha());

    m_red_spinbox->set_value(m_color.red());
    m_green_spinbox->set_value(m_color.green());
    m_blue_spinbox->set_value(m_color.blue());
    m_alpha_spinbox->set_value(m_color.alpha());
    m_alpha_spinbox->set_enabled(m_color_has_alpha_channel);
    m_alpha->set_value(m_color.alpha());
    m_alpha->set_visible(m_color_has_alpha_channel);
    if (on_color_changed)
        on_color_changed(m_color);
}

void ColorPicker::create_color_button(Widget& container, unsigned rgb)
{
    Color color = Color::from_rgb(rgb);

    auto& widget = container.add<ColorButton>(*this, color);
    widget.on_click = [this](Color color) {
        for (auto& value : m_color_widgets) {
            value.set_selected(false);
            value.update();
        }

        m_color = color;
        m_custom_color->set_color(color);
        update_color_widgets();
    };

    if (color == m_color) {
        widget.set_selected(true);
    }

    m_color_widgets.append(widget);
}

ColorButton::ColorButton(ColorPicker& picker, Color color)
    : m_picker(picker)
{
    m_color = color;
}

void ColorButton::set_selected(bool selected)
{
    m_selected = selected;
}

void ColorButton::doubleclick_event(GUI::MouseEvent&)
{
    click();
    m_selected = true;
    m_picker.done(Dialog::ExecResult::OK);
}

void ColorButton::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::StylePainter::paint_button(painter, rect(), palette(), Gfx::ButtonStyle::Normal, is_being_pressed(), is_hovered(), is_checked(), is_enabled(), is_focused());

    painter.fill_rect(rect().shrunken(2, 2), m_color);

    if (m_selected) {
        painter.fill_rect(rect().shrunken(6, 6), Color::Black);
        painter.fill_rect(rect().shrunken(10, 10), Color::White);
        painter.fill_rect(rect().shrunken(14, 14), m_color);
    }
}

void ColorButton::click(unsigned)
{
    if (on_click)
        on_click(m_color);

    m_selected = true;
}

CustomColorWidget::CustomColorWidget(Color color)
{
    set_layout<HorizontalBoxLayout>();

    m_color_field = add<ColorField>(color);
    auto size = 256 + (m_color_field->frame_thickness() * 2);
    m_color_field->set_fixed_size(size, size);
    m_color_field->on_pick = [this](Color color) {
        if (on_pick)
            on_pick(color);
    };

    m_color_slider = add<ColorSlider>(color.to_hsv().hue);
    auto slider_width = 24 + (m_color_slider->frame_thickness() * 2);
    m_color_slider->set_fixed_size(slider_width, size);
    m_color_slider->on_pick = [this](double value) {
        m_color_field->set_hue_from_pick(value);
    };
}

void CustomColorWidget::set_color(Color color)
{
    m_color_field->set_color(color);
    m_color_field->set_hue(color.to_hsv().hue);

    m_color_slider->set_value(color.to_hsv().hue);
}

ColorField::ColorField(Color color)
    : m_color(color)
    , m_hue(color.to_hsv().hue)
{
    create_color_bitmap();
}

void ColorField::create_color_bitmap()
{
    m_color_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 256, 256 }).release_value_but_fixme_should_propagate_errors();
    auto painter = Gfx::Painter(*m_color_bitmap);

    Gfx::HSV hsv;
    hsv.hue = m_hue;
    for (int x = 0; x < 256; x++) {
        hsv.saturation = x / 255.0;
        for (int y = 0; y < 256; y++) {
            hsv.value = (255 - y) / 255.0;
            Color color = Color::from_hsv(hsv);
            painter.set_pixel({ x, y }, color);
        }
    }
}

void ColorField::set_color(Color color)
{
    if (m_color == color)
        return;

    m_color = color;
    // don't save m_hue here by default, we don't want to set it to 0 in case color is full white
    // m_hue = color.to_hsv().hue;

    recalculate_position();
}

void ColorField::recalculate_position()
{
    Gfx::HSV hsv = m_color.to_hsv();
    auto x = hsv.saturation * width();
    auto y = (1 - hsv.value) * height();
    m_last_position = Gfx::IntPoint(x, y);
    update();
}

void ColorField::set_hue(double hue)
{
    if (m_hue == hue)
        return;

    auto hsv = m_color.to_hsv();
    hsv.hue = hue;

    m_hue = hue;
    create_color_bitmap();

    auto color = Color::from_hsv(hsv);
    color.set_alpha(m_color.alpha());
    set_color(color);
}

void ColorField::set_hue_from_pick(double hue)
{
    set_hue(hue);
    if (on_pick)
        on_pick(m_color);
}

void ColorField::pick_color_at_position(GUI::MouseEvent& event)
{
    if (!m_being_pressed)
        return;

    auto inner_rect = frame_inner_rect();
    auto position = event.position().constrained(inner_rect).translated(-frame_thickness(), -frame_thickness());
    auto color = Color::from_hsv(m_hue, (double)position.x() / inner_rect.width(), (double)(inner_rect.height() - position.y()) / inner_rect.height());
    color.set_alpha(m_color.alpha());
    m_last_position = position;
    m_color = color;

    if (on_pick)
        on_pick(color);

    update();
}

void ColorField::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        m_being_pressed = true;
        pick_color_at_position(event);
    }
}

void ColorField::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        m_being_pressed = false;
        pick_color_at_position(event);
    }
}

void ColorField::mousemove_event(GUI::MouseEvent& event)
{
    if (event.buttons() & GUI::MouseButton::Primary)
        pick_color_at_position(event);
}

void ColorField::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    painter.draw_scaled_bitmap(frame_inner_rect(), *m_color_bitmap, m_color_bitmap->rect());

    painter.translate(frame_thickness(), frame_thickness());
    painter.draw_line({ m_last_position.x() - 1, 0 }, { m_last_position.x() - 1, height() }, Color::White);
    painter.draw_line({ m_last_position.x() + 1, 0 }, { m_last_position.x() + 1, height() }, Color::White);
    painter.draw_line({ 0, m_last_position.y() - 1 }, { width(), m_last_position.y() - 1 }, Color::White);
    painter.draw_line({ 0, m_last_position.y() + 1 }, { width(), m_last_position.y() + 1 }, Color::White);
    painter.draw_line({ m_last_position.x(), 0 }, { m_last_position.x(), height() }, Color::Black);
    painter.draw_line({ 0, m_last_position.y() }, { width(), m_last_position.y() }, Color::Black);
}

void ColorField::resize_event(ResizeEvent&)
{
    recalculate_position();
}

ColorSlider::ColorSlider(double value)
    : m_value(value)
{
    m_color_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 32, 360 }).release_value_but_fixme_should_propagate_errors();
    auto painter = Gfx::Painter(*m_color_bitmap);

    for (int h = 0; h < 360; h++) {
        Gfx::HSV hsv;
        hsv.hue = h;
        hsv.saturation = 1.0;
        hsv.value = 1.0;
        Color color = Color::from_hsv(hsv);
        painter.draw_line({ 0, h }, { 32, h }, color);
    }
}

void ColorSlider::set_value(double value)
{
    if (m_value == value)
        return;

    m_value = value;
    recalculate_position();
}

void ColorSlider::recalculate_position()
{
    m_last_position = (m_value / 360.0) * height();
    update();
}

void ColorSlider::pick_value_at_position(GUI::MouseEvent& event)
{
    if (!m_being_pressed)
        return;

    auto inner_rect = frame_inner_rect();
    auto position = event.position().constrained(inner_rect).translated(-frame_thickness(), -frame_thickness());
    auto hue = (double)position.y() / inner_rect.height() * 360;
    if (hue >= 360)
        hue -= 360;
    m_last_position = position.y();
    m_value = hue;

    if (on_pick)
        on_pick(m_value);

    update();
}

void ColorSlider::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        m_being_pressed = true;
        pick_value_at_position(event);
    }
}

void ColorSlider::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        m_being_pressed = false;
        pick_value_at_position(event);
    }
}

void ColorSlider::mousemove_event(GUI::MouseEvent& event)
{
    if (event.buttons() & GUI::MouseButton::Primary)
        pick_value_at_position(event);
}

void ColorSlider::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    painter.draw_scaled_bitmap(frame_inner_rect(), *m_color_bitmap, m_color_bitmap->rect());

    painter.translate(frame_thickness(), frame_thickness());
    painter.draw_line({ 0, m_last_position - 1 }, { width(), m_last_position - 1 }, Color::White);
    painter.draw_line({ 0, m_last_position + 1 }, { width(), m_last_position + 1 }, Color::White);
    painter.draw_line({ 0, m_last_position }, { width(), m_last_position }, Color::Black);
}

void ColorSlider::resize_event(ResizeEvent&)
{
    recalculate_position();
}

ColorPreview::ColorPreview(Color color)
    : m_color(color)
{
}

void ColorPreview::set_color(Color color)
{
    if (m_color == color)
        return;

    m_color = color;
    update();
}

void ColorPreview::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (m_color.alpha() < 255) {
        Gfx::StylePainter::paint_transparency_grid(painter, rect(), palette());
        painter.fill_rect(rect(), m_color);
        painter.fill_rect({ 0, 0, rect().width() / 4, rect().height() }, m_color.with_alpha(255));
    } else {
        painter.fill_rect(rect(), m_color);
    }
}

}
