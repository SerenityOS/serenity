/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
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

u32 round_previous_power_of_2(u32 x);

void BarsVisualizationWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    painter.add_clip_rect(event.rect());
    painter.fill_rect(frame_inner_rect(), Color::Black);

    if (m_sample_buffer.is_empty())
        return;

    LibDSP::fft(m_sample_buffer, false);
    double max = AK::sqrt(m_sample_count * 2.);

    double freq_bin = m_samplerate / (double)m_sample_count;

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

    int bins_per_group = ceil_div((m_sample_count - 1) / 2, group_count);
    for (int i = 1; i < m_sample_count / 2; i++) {
        groups[i / bins_per_group] += AK::fabs(m_sample_buffer.data()[i].real());
    }
    for (int i = 0; i < group_count; i++)
        groups[i] /= max * freq_bin / (m_adjust_frequencies ? (clamp(AK::pow(AK::E<double>, (double)i / group_count * 3.) - 1.75, 1., 15.)) : 1.);

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

BarsVisualizationWidget::~BarsVisualizationWidget()
{
}

BarsVisualizationWidget::BarsVisualizationWidget()
    : m_last_id(-1)
    , m_is_using_last(false)
    , m_adjust_frequencies(false)
{
    m_context_menu = GUI::Menu::construct();
    m_context_menu->add_action(GUI::Action::create_checkable("Adjust frequency energy (for aesthetics)", [&](GUI::Action& action) {
        m_adjust_frequencies = action.is_checked();
    }));
}

// black magic from Hacker's delight
u32 round_previous_power_of_2(u32 x)
{
    x = x | (x >> 1);
    x = x | (x >> 2);
    x = x | (x >> 4);
    x = x | (x >> 8);
    x = x | (x >> 16);
    return x - (x >> 1);
}

void BarsVisualizationWidget::set_buffer(RefPtr<Audio::Buffer> buffer, int samples_to_use)
{
    if (m_is_using_last)
        return;
    m_is_using_last = true;
    // FIXME: We should dynamically adapt to the sample count and e.g. perform the fft over multiple buffers.
    // For now, the visualizer doesn't work with extremely low global sample rates.
    if (buffer->sample_count() < 256)
        return;
    m_sample_count = round_previous_power_of_2(samples_to_use);
    m_sample_buffer.resize(m_sample_count);
    for (int i = 0; i < m_sample_count; i++) {
        m_sample_buffer.data()[i] = (AK::fabs(buffer->samples()[i].left) + AK::fabs(buffer->samples()[i].right)) / 2.;
    }

    update();
}

void BarsVisualizationWidget::set_buffer(RefPtr<Audio::Buffer> buffer)
{
    if (buffer.is_null())
        return;
    if (m_last_id == buffer->id())
        return;
    set_buffer(buffer, buffer->sample_count());
}

void BarsVisualizationWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    m_context_menu->popup(event.screen_position());
}

void BarsVisualizationWidget::set_samplerate(int samplerate)
{
    m_samplerate = samplerate;
}
