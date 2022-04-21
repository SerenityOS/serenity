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

HistogramWidget::HistogramWidget()
{
    set_height(65);
}

HistogramWidget::~HistogramWidget()
{
    if (m_image)
        m_image->remove_client(*this);
}

void HistogramWidget::set_image(Image* image)
{
    if (m_image == image)
        return;
    if (m_image)
        m_image->remove_client(*this);
    m_image = image;
    if (m_image)
        m_image->add_client(*this);

    (void)rebuild_histogram_data();
}

ErrorOr<void> HistogramWidget::rebuild_histogram_data()
{
    if (!m_image)
        return {};

    auto full_bitmap = TRY(m_image->try_compose_bitmap(Gfx::BitmapFormat::BGRA8888));

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
    int max_brightness_frequency = 0;
    int max_color_frequency = 0;
    for (int i = 0; i < 256; i++) {
        if (m_data.red[i] > max_color_frequency)
            max_color_frequency = m_data.red[i];
        if (m_data.green[i] > max_color_frequency)
            max_color_frequency = m_data.green[i];
        if (m_data.blue[i] > max_color_frequency)
            max_color_frequency = m_data.blue[i];
        if (m_data.brightness[i] > max_brightness_frequency)
            max_brightness_frequency = m_data.brightness[i];
    }

    // Scale the frequency values to fit the widgets height.
    m_widget_height = height();

    for (int i = 0; i < 256; i++) {
        m_data.red[i] = (static_cast<float>(m_data.red[i]) / max_color_frequency) * m_widget_height;
        m_data.green[i] = (static_cast<float>(m_data.green[i]) / max_color_frequency) * m_widget_height;
        m_data.blue[i] = (static_cast<float>(m_data.blue[i]) / max_color_frequency) * m_widget_height;
        m_data.brightness[i] = (static_cast<float>(m_data.brightness[i]) / max_brightness_frequency) * m_widget_height;
    }

    update();
    return {};
}

void HistogramWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (!m_image)
        return;

    int bottom_line = m_widget_height - 1;
    float step_width = static_cast<float>(width()) / 256;

    Gfx::Path brightness_path;
    Gfx::Path red_channel_path;
    Gfx::Path green_channel_path;
    Gfx::Path blue_channel_path;
    red_channel_path.move_to({ 0, bottom_line - m_data.red[0] });
    green_channel_path.move_to({ 0, bottom_line - m_data.green[0] });
    blue_channel_path.move_to({ 0, bottom_line - m_data.blue[0] });
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

        red_channel_path.line_to({ current_x_as_int, bottom_line - m_data.red[data_column] });
        green_channel_path.line_to({ current_x_as_int, bottom_line - m_data.green[data_column] });
        blue_channel_path.line_to({ current_x_as_int, bottom_line - m_data.blue[data_column] });
        brightness_path.line_to({ current_x_as_int, bottom_line - m_data.brightness[data_column] });

        current_x_as_float += step_width;
        last_drawn_x = current_x_as_int;
    }

    brightness_path.line_to({ last_drawn_x, bottom_line });
    brightness_path.close();

    painter.fill_path(brightness_path, Color::MidGray, Gfx::Painter::WindingRule::EvenOdd);
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
}

void HistogramWidget::set_color_at_mouseposition(Color color)
{
    if (m_color_at_mouseposition == color)
        return;

    m_color_at_mouseposition = color;
    update();
}

}
