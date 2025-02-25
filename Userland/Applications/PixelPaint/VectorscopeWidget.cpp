/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "VectorscopeWidget.h"
#include "Layer.h"
#include <AK/Math.h>
#include <AK/Types.h>
#include <LibGUI/Event.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>
#include <LibGfx/TextAlignment.h>
#include <LibGfx/TextElision.h>

REGISTER_WIDGET(PixelPaint, VectorscopeWidget);

namespace PixelPaint {

void VectorscopeWidget::image_changed()
{
    (void)rebuild_vectorscope_data();
    rebuild_vectorscope_image();
    update();
}

ErrorOr<void> VectorscopeWidget::rebuild_vectorscope_data()
{
    if (!should_process_data())
        return {};

    m_vectorscope_data.fill({});
    VERIFY(AK::abs(m_vectorscope_data[0][0]) < 0.01f);
    auto full_bitmap = TRY(m_image->compose_bitmap(Gfx::BitmapFormat::BGRA8888));

    for (size_t x = 0; x < static_cast<size_t>(full_bitmap->width()); ++x) {
        for (size_t y = 0; y < static_cast<size_t>(full_bitmap->height()); ++y) {
            auto yuv = full_bitmap->get_pixel(x, y).to_yuv();
            auto u_index = u_v_to_index(yuv.u);
            auto v_index = u_v_to_index(yuv.v);
            m_vectorscope_data[u_index][v_index]++;
        }
    }

    auto maximum = full_bitmap->width() * full_bitmap->height() * pixel_percentage_for_max_brightness * pixel_percentage_for_max_brightness;

    for (size_t i = 0; i < m_vectorscope_data.size(); ++i) {
        for (size_t j = 0; j < m_vectorscope_data[i].size(); ++j) {
            m_vectorscope_data[i][j] = AK::sqrt(m_vectorscope_data[i][j]) / maximum;
        }
    }

    return {};
}

void VectorscopeWidget::rebuild_vectorscope_image()
{
    m_vectorscope_image = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, size()));
    m_vectorscope_image->fill(Color::Transparent);

    Gfx::Painter base_painter(*m_vectorscope_image);
    Gfx::AntiAliasingPainter painter(base_painter);

    auto const scope_size = min(height(), width());
    auto const min_scope_size = parent_widget()->min_height().as_int();
    auto const color_vector_scale = scope_size / static_cast<float>(min_scope_size);
    auto const size_1x1 = Gfx::FloatSize { 2.5f, 2.5f } * static_cast<float>(color_vector_scale);

    base_painter.translate(width() / 2, height() / 2);
    painter.translate(static_cast<float>(width()) / 2.0f, static_cast<float>(height()) / 2.0f);
    for (size_t u_index = 0; u_index < u_v_steps; ++u_index) {
        for (size_t v_index = 0; v_index < u_v_steps; ++v_index) {
            auto const color_vector = ColorVector::from_indices(u_index, v_index);
            auto const brightness = m_vectorscope_data[u_index][v_index];
            if (brightness < 0.0001f)
                continue;
            auto const pseudo_rect = Gfx::FloatRect::centered_on(color_vector.to_vector(scope_size) * 2.0f, size_1x1);
            auto color = Color::from_yuv(0.6f, color_vector.u, color_vector.v);
            color = color.saturated_to(1.0f - min(brightness, 1.0f));
            color.set_alpha(static_cast<u8>(min(AK::sqrt(brightness), alpha_range) * NumericLimits<u8>::max() / alpha_range));
            painter.fill_rect(pseudo_rect, color);
        }
    }
}

void VectorscopeWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter base_painter(*this);
    Gfx::AntiAliasingPainter painter(base_painter);
    base_painter.add_clip_rect(event.rect());
    // From this point on we're working with 0,0 as the scope center to make things easier.
    base_painter.translate(width() / 2, height() / 2);
    painter.translate(static_cast<float>(width()) / 2.0f, static_cast<float>(height()) / 2.0f);

    auto const graticule_color = Color::White;
    auto const scope_size = min(height(), width());
    auto const graticule_size = scope_size / 6;
    auto const graticule_thickness = graticule_size / 12;
    auto const entire_scope_rect = Gfx::FloatRect::centered_on({ 0, 0 }, { scope_size, scope_size });

    painter.fill_ellipse(entire_scope_rect.to_rounded<int>().shrunken(graticule_thickness * 2, graticule_thickness * 2), Color::Black);

    // Main scope data
    if (m_image) {
        if (m_vectorscope_image->size() != this->size())
            rebuild_vectorscope_image();

        base_painter.blit({ -width() / 2, -height() / 2 }, *m_vectorscope_image, m_vectorscope_image->rect());
    }

    // Graticule(s)
    painter.draw_ellipse(entire_scope_rect.to_rounded<int>(), graticule_color, graticule_thickness);

    // FIXME: Translation calls to the painters don't appear to work correctly, and I figured out a combination of calls through trial and error that do what I want, but I don't know how they do that.
    //        Translation does work correctly with things like rectangle and text drawing, so it's very strange.
    painter.translate(-static_cast<float>(width()) / 2.0f, -static_cast<float>(height()) / 2.0f);
    // We intentionally draw the skin tone line much further than the actual color we're using for it.
    painter.draw_line({ 0, 0 }, skin_tone_color.to_vector(scope_size) * 2.0, graticule_color);
    painter.translate(-static_cast<float>(width()) / 2.0f, -static_cast<float>(height()) / 2.0f);

    for (auto const& primary_color : primary_colors) {
        auto center = primary_color.to_vector(scope_size);
        auto center_rounded = center.to_rounded<int>();
        // Box color
        Gfx::Color corner_color = Gfx::Color::from_yuv(0.5f, primary_color.u, primary_color.v).saturated_to(0.5);

        // Bracket vertex calculations
        int left_outer_vertex = center_rounded.x() - graticule_size / 2;
        int right_outer_vertex = center_rounded.x() + graticule_size / 2;
        int top_outer_vertex = center_rounded.y() - graticule_size / 2;
        int bottom_outer_vertex = center_rounded.y() + graticule_size / 2;
        int left_inner_vertex = center_rounded.x() - graticule_size / 3;
        int right_inner_vertex = center_rounded.x() + graticule_size / 3;
        int top_inner_vertex = center_rounded.y() - graticule_size / 3;
        int bottom_inner_vertex = center_rounded.y() + graticule_size / 3;

        // Top Left Corner
        base_painter.draw_line(Gfx::IntPoint(left_outer_vertex, top_outer_vertex), Gfx::IntPoint(left_inner_vertex, top_outer_vertex), corner_color, graticule_thickness);
        base_painter.draw_line(Gfx::IntPoint(left_outer_vertex, top_outer_vertex), Gfx::IntPoint(left_outer_vertex, top_inner_vertex), corner_color, graticule_thickness);
        // Top Right Corner
        base_painter.draw_line(Gfx::IntPoint(right_outer_vertex, top_outer_vertex), Gfx::IntPoint(right_inner_vertex, top_outer_vertex), corner_color, graticule_thickness);
        base_painter.draw_line(Gfx::IntPoint(right_outer_vertex, top_outer_vertex), Gfx::IntPoint(right_outer_vertex, top_inner_vertex), corner_color, graticule_thickness);
        // Bottom Left Corner
        base_painter.draw_line(Gfx::IntPoint(left_outer_vertex, bottom_outer_vertex), Gfx::IntPoint(left_inner_vertex, center_rounded.y() + graticule_size / 2), corner_color, graticule_thickness);
        base_painter.draw_line(Gfx::IntPoint(left_outer_vertex, center_rounded.y() + graticule_size / 2), Gfx::IntPoint(left_outer_vertex, bottom_inner_vertex), corner_color, graticule_thickness);
        // Bottom Right Corner
        base_painter.draw_line(Gfx::IntPoint(right_outer_vertex, center_rounded.y() + graticule_size / 2), Gfx::IntPoint(right_inner_vertex, center_rounded.y() + graticule_size / 2), corner_color, graticule_thickness);
        base_painter.draw_line(Gfx::IntPoint(right_outer_vertex, center_rounded.y() + graticule_size / 2), Gfx::IntPoint(right_outer_vertex, bottom_inner_vertex), corner_color, graticule_thickness);

        // Add text label to vectorscope
        auto text_rect = Gfx::FloatRect::centered_on(center, { graticule_size, graticule_size }).to_rounded<int>().translated(-(graticule_thickness + 1), -(graticule_thickness + 1));
        base_painter.draw_text(text_rect, StringView { &primary_color.symbol, 1 }, Gfx::TextAlignment::BottomRight, graticule_color);
    }

    if (m_color_at_mouseposition != Color::Transparent) {
        auto color_vector = ColorVector { m_color_at_mouseposition };
        painter.draw_ellipse(Gfx::FloatRect::centered_on(color_vector.to_vector(scope_size) * 2.0, { graticule_size, graticule_size }).to_rounded<int>(), graticule_color, graticule_thickness);
    }
}

}
