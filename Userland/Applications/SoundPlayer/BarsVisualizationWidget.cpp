/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BarsVisualizationWidget.h"
#include <AK/Math.h>
#include <LibDSP/FFT.h>
#include <LibGUI/Event.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>

void BarsVisualizationWidget::render(GUI::PaintEvent& event, FixedArray<double> const& samples)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    painter.add_clip_rect(event.rect());
    painter.fill_rect(frame_inner_rect(), Color::Black);

    for (size_t i = 0; i < samples.size(); i++)
        m_fft_samples[i] = samples[i];

    LibDSP::fft(m_fft_samples.span(), false);
    double max = AK::sqrt(samples.size() * 2.);

    double freq_bin = m_samplerate / (double)samples.size();

    constexpr int group_count = 60;
    Vector<double, group_count> groups;
    groups.resize(group_count);
    if (m_gfx_falling_bars.size() != group_count) {
        m_gfx_falling_bars.resize(group_count);
        for (int& i : m_gfx_falling_bars)
            i = 0;
    }
    for (double& d : groups)
        d = 0.;

    int bins_per_group = ceil_div((samples.size() - 1) / 2, static_cast<size_t>(group_count));
    for (size_t i = 1; i < samples.size() / 2; i++) {
        groups[i / bins_per_group] += AK::abs(m_fft_samples[i].real());
    }
    for (int i = 0; i < group_count; i++)
        groups[i] /= max * freq_bin / (m_adjust_frequencies ? (clamp(AK::exp((double)i / group_count * 3.) - 1.75, 1., 15.)) : 1.);

    const int horizontal_margin = 30;
    const int top_vertical_margin = 15;
    const int pixels_inbetween_groups = frame_inner_rect().width() > 350 ? 5 : 2;
    int pixel_per_group_width = (frame_inner_rect().width() - horizontal_margin * 2 - pixels_inbetween_groups * (group_count - 1)) / group_count;
    int max_height = frame_inner_rect().height() - top_vertical_margin;
    int current_xpos = horizontal_margin;
    for (int g = 0; g < group_count; g++) {
        m_gfx_falling_bars[g] = AK::min(clamp(max_height - (int)(groups[g] * max_height * 0.8), 0, max_height), m_gfx_falling_bars[g]);
        painter.fill_rect(Gfx::Rect(current_xpos, max_height - (int)(groups[g] * max_height * 0.8), pixel_per_group_width, (int)(groups[g] * max_height * 0.8)), Gfx::Color::from_rgb(0x95d437));
        painter.fill_rect(Gfx::Rect(current_xpos, m_gfx_falling_bars[g], pixel_per_group_width, 2), Gfx::Color::White);
        current_xpos += pixel_per_group_width + pixels_inbetween_groups;
        m_gfx_falling_bars[g] += 3;
    }

    m_is_using_last = false;
}

BarsVisualizationWidget::BarsVisualizationWidget()
    : m_fft_samples(MUST(FixedArray<Complex<double>>::try_create(128)))
    , m_is_using_last(false)
    , m_adjust_frequencies(true)
{
    m_context_menu = GUI::Menu::construct();
    auto frequency_energy_action = GUI::Action::create_checkable("Adjust frequency energy (for aesthetics)", [&](GUI::Action& action) {
        m_adjust_frequencies = action.is_checked();
    });
    frequency_energy_action->set_checked(true);
    m_context_menu->add_action(frequency_energy_action);

    MUST(set_render_sample_count(128));
}

void BarsVisualizationWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    m_context_menu->popup(event.screen_position());
}
