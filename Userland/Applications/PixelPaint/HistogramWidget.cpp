/*
 * Copyright (c) 2022, Torsten Engelmann
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HistogramWidget.h"
#include "Image.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>

REGISTER_WIDGET(PixelPaint, HistogramWidget);

namespace PixelPaint {

ErrorOr<void> HistogramWidget::rebuild_histogram_data()
{
    if (!should_process_data())
        return {};

    auto full_bitmap = TRY(m_image->compose_bitmap(Gfx::BitmapFormat::BGRA8888));

    m_data.red.clear_with_capacity();
    m_data.green.clear_with_capacity();
    m_data.blue.clear_with_capacity();
    m_data.brightness.clear_with_capacity();

    for (int i = 0; i < 256; i++) {
        m_data.red.append(0);
        m_data.green.append(0);
        m_data.blue.append(0);
        m_data.brightness.append(0);
    }

    Color pixel_color;
    for (int x = 0; x < full_bitmap->width(); x++) {
        for (int y = 0; y < full_bitmap->height(); y++) {
            pixel_color = full_bitmap->get_pixel(x, y);
            if (!pixel_color.alpha())
                continue;

            m_data.red[pixel_color.red()]++;
            m_data.green[pixel_color.green()]++;
            m_data.blue[pixel_color.blue()]++;
            m_data.brightness[pixel_color.luminosity()]++;
        }
    }
    m_data.max_brightness_frequency = 0;
    m_data.max_color_frequency = 0;
    for (int i = 0; i < 256; i++) {
        if (m_data.red[i] > m_data.max_color_frequency)
            m_data.max_color_frequency = m_data.red[i];
        if (m_data.green[i] > m_data.max_color_frequency)
            m_data.max_color_frequency = m_data.green[i];
        if (m_data.blue[i] > m_data.max_color_frequency)
            m_data.max_color_frequency = m_data.blue[i];
        if (m_data.brightness[i] > m_data.max_brightness_frequency)
            m_data.max_brightness_frequency = m_data.brightness[i];
    }
    return {};
}

void HistogramWidget::paint_event(GUI::PaintEvent& event)
{
    if (!should_process_data() || m_data.max_color_frequency == 0)
        return;

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    int bottom_line = height() - 1;
    float step_width = static_cast<float>(width()) / 256;
    auto scale_height_value = [this, bottom_line](int color_count, int max_value_count) {
        return bottom_line - (color_count * this->height() / max_value_count);
    };

    Gfx::Path brightness_path;
    Gfx::Path red_channel_path;
    Gfx::Path green_channel_path;
    Gfx::Path blue_channel_path;
    red_channel_path.move_to({ 0, scale_height_value(m_data.red[0], m_data.max_color_frequency) });
    green_channel_path.move_to({ 0, scale_height_value(m_data.green[0], m_data.max_color_frequency) });
    blue_channel_path.move_to({ 0, scale_height_value(m_data.blue[0], m_data.max_color_frequency) });
    brightness_path.move_to({ 0, bottom_line });
    brightness_path.line_to({ 0, bottom_line });

    float current_x_as_float = 0;
    int current_x_as_int = 0;
    int last_drawn_x = -1;

    for (int data_column = 0; data_column < 256; data_column++) {
        current_x_as_int = static_cast<int>(current_x_as_float);
        // we would like to skip values that map to the same x position as it does not look so good in the final result
        if (current_x_as_int == last_drawn_x) {
            current_x_as_float += step_width;
            continue;
        }

        // Scale the frequency values to fit the widgets height as this is probably changed when the widget gets detached.
        red_channel_path.line_to({ current_x_as_int, scale_height_value(m_data.red[data_column], m_data.max_color_frequency) });
        green_channel_path.line_to({ current_x_as_int, scale_height_value(m_data.green[data_column], m_data.max_color_frequency) });
        blue_channel_path.line_to({ current_x_as_int, scale_height_value(m_data.blue[data_column], m_data.max_color_frequency) });
        brightness_path.line_to({ current_x_as_int, scale_height_value(m_data.brightness[data_column], m_data.max_brightness_frequency) });

        current_x_as_float += step_width;
        last_drawn_x = current_x_as_int;
    }

    brightness_path.line_to({ last_drawn_x, bottom_line });
    brightness_path.close();

    painter.fill_path(brightness_path, Color::MidGray, Gfx::WindingRule::EvenOdd);
    painter.stroke_path(red_channel_path, Color(Color::NamedColor::Red).with_alpha(90), 2);
    painter.stroke_path(green_channel_path, Color(Color::NamedColor::Green).with_alpha(90), 2);
    painter.stroke_path(blue_channel_path, Color(Color::NamedColor::Blue).with_alpha(90), 2);

    if (m_color_at_mouseposition != Color::Transparent) {
        int x = m_color_at_mouseposition.luminosity() * step_width;
        painter.draw_line({ x, 0 }, { x, bottom_line }, Color::from_hsl(45, 1, .7), 1);
    }
}

void HistogramWidget::image_changed()
{
    (void)rebuild_histogram_data();
    update();
}

}
