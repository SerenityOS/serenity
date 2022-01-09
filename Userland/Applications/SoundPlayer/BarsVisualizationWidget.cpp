/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BarsVisualizationWidget.h"
#include <AK/Math.h>
#include <LibDSP/FFT.h>
#include <LibDSP/Window.h>
#include <LibGUI/Event.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>

void BarsVisualizationWidget::render(GUI::PaintEvent& event, Vector<double> const& samples)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    painter.add_clip_rect(event.rect());
    painter.fill_rect(frame_inner_rect(), Color::Black);

    assert(m_fft_samples.size() == FFTSize);
    for (size_t i = 0; i < FFTSize; i++)
        m_fft_samples[i] = samples[i] * m_fft_window[i];

    LibDSP::fft(m_fft_samples, false);

    Array<double, BinGroupCount> groups {};

    for (size_t i = 0; i < FFTSize / 2; i += BinsPerGroup) {
        double const magnitude = m_fft_samples[i].magnitude();
        groups[i / BinsPerGroup] = magnitude;
        for (size_t j = 0; j < BinsPerGroup; j++) {
            double const magnitude = m_fft_samples[i + j].magnitude();
            groups[i / BinsPerGroup] += magnitude;
        }
        groups[i / BinsPerGroup] /= BinsPerGroup;
    }

    double const max_peak_value = AK::sqrt(static_cast<double>(FFTSize));
    for (size_t i = 0; i < BinGroupCount; i++) {
        groups[i] = AK::log(groups[i] + 1) / AK::log(max_peak_value);
        if (m_adjust_frequencies)
            groups[i] *= 1 + 3.0 * i / BinGroupCount;
    }

    int const horizontal_margin = 30;
    int const top_vertical_margin = 15;
    int const pixels_inbetween_groups = frame_inner_rect().width() > 350 ? 5 : 2;
    int const pixel_per_group_width = (frame_inner_rect().width() - horizontal_margin * 2 - pixels_inbetween_groups * (BinGroupCount - 1)) / BinGroupCount;
    int const max_height = frame_inner_rect().height() - top_vertical_margin;
    int current_xpos = horizontal_margin;
    for (size_t g = 0; g < BinGroupCount; g++) {
        m_gfx_falling_bars[g] = AK::min(clamp(max_height - (int)(groups[g] * max_height * 0.8), 0, max_height), m_gfx_falling_bars[g]);
        painter.fill_rect(Gfx::Rect(current_xpos, max_height - (int)(groups[g] * max_height * 0.8), pixel_per_group_width, (int)(groups[g] * max_height * 0.8)), Gfx::Color::from_rgb(0x95d437));
        painter.fill_rect(Gfx::Rect(current_xpos, m_gfx_falling_bars[g], pixel_per_group_width, 2), Gfx::Color::White);
        current_xpos += pixel_per_group_width + pixels_inbetween_groups;
        m_gfx_falling_bars[g] += 3;
    }
}

BarsVisualizationWidget::BarsVisualizationWidget()
    : m_gfx_falling_bars {}
    , m_is_using_last(false)
    , m_adjust_frequencies(false)
{
    m_context_menu = GUI::Menu::construct();
    m_context_menu->add_action(GUI::Action::create_checkable("Adjust frequency energy (for aesthetics)", [&](GUI::Action& action) {
        m_adjust_frequencies = action.is_checked();
    }));

    m_fft_window = LibDSP::Window<double>::hann<FFTSize>();

    set_render_sample_count(FFTSize);
    m_fft_samples.resize(FFTSize);
    assert(m_fft_samples.size() == FFTSize);
}

void BarsVisualizationWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    m_context_menu->popup(event.screen_position());
}
