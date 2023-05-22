/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BarsVisualizationWidget.h"
#include <AK/IntegralMath.h>
#include <AK/Math.h>
#include <AK/TypedTransfer.h>
#include <LibDSP/FFT.h>
#include <LibDSP/Window.h>
#include <LibGUI/Event.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>

void BarsVisualizationWidget::render(GUI::PaintEvent& event, FixedArray<float> const& samples)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    painter.add_clip_rect(event.rect());
    painter.fill_rect(frame_inner_rect(), Color::Black);

    // First half of data is from previous iteration, second half is from now.
    // This gives us fully overlapping windows, which result in more accurate and visually appealing STFT.
    for (size_t i = 0; i < fft_size / 2; i++)
        m_fft_samples[i] = m_previous_samples[i] * m_fft_window[i];
    for (size_t i = 0; i < fft_size / 2; i++)
        m_fft_samples[i + fft_size / 2] = samples[i] * m_fft_window[i + fft_size / 2];

    AK::TypedTransfer<float>::copy(m_previous_samples.data(), samples.data(), samples.size());

    DSP::fft(m_fft_samples.span(), false);

    Array<float, bar_count> groups {};

    if (m_logarithmic_spectrum) {
        auto const log_bar_size = static_cast<float>(bar_count) / AK::log2(fft_size);

        for (size_t i = 0; i < bar_count; ++i) {
            auto const bar_start = i == 0 ? 0 : static_cast<size_t>(floor(AK::pow(2.f, static_cast<float>(i) / log_bar_size)));
            auto const bar_end = clamp(static_cast<size_t>(floor(AK::pow(2.f, static_cast<float>(i + 1) / log_bar_size))), bar_start + 1, cutoff);
            auto const values_in_bar = bar_end - bar_start;

            for (size_t sample_index = bar_start; sample_index < bar_start + values_in_bar; sample_index++) {
                float const magnitude = m_fft_samples[sample_index].magnitude();
                groups[i] += magnitude;
            }
            groups[i] /= static_cast<float>(values_in_bar);
        }
    } else {
        static constexpr size_t values_per_bar = (fft_size / 2) / bar_count;
        for (size_t i = 0; i < fft_size / 2; i += values_per_bar) {
            float const magnitude = m_fft_samples[i].magnitude();
            groups[i / values_per_bar] = magnitude;
            for (size_t j = 0; j < values_per_bar; j++) {
                float const magnitude = m_fft_samples[i + j].magnitude();
                groups[i / values_per_bar] += magnitude;
            }
            groups[i / values_per_bar] /= values_per_bar;
        }
    }

    float const max_peak_value = AK::sqrt(static_cast<float>(fft_size * 2));
    for (size_t i = 0; i < bar_count; i++) {
        groups[i] = AK::log(groups[i] + 1) / AK::log(max_peak_value);
        if (m_adjust_frequencies)
            groups[i] *= 1 + 2.0f * (static_cast<float>(i) - bar_count / 3.0f) / static_cast<float>(bar_count);
    }

    int const horizontal_margin = 30;
    int const top_vertical_margin = 15;
    int const pixels_inbetween_groups = frame_inner_rect().width() > 350 ? 5 : 2;
    int const pixel_per_group_width = (frame_inner_rect().width() - horizontal_margin * 2 - pixels_inbetween_groups * (bar_count - 1)) / bar_count;
    int const max_height = AK::max(0, frame_inner_rect().height() - top_vertical_margin);
    int current_xpos = horizontal_margin;
    for (size_t g = 0; g < bar_count; g++) {
        m_gfx_falling_bars[g] = AK::min(clamp(max_height - static_cast<int>(groups[g] * static_cast<float>(max_height) * 0.8f), 0, max_height), m_gfx_falling_bars[g]);
        painter.fill_rect(Gfx::Rect(current_xpos, max_height - static_cast<int>(groups[g] * static_cast<float>(max_height) * 0.8f), pixel_per_group_width, static_cast<int>(groups[g] * max_height * 0.8f)), Gfx::Color::from_rgb(0x95d437));
        painter.fill_rect(Gfx::Rect(current_xpos, m_gfx_falling_bars[g], pixel_per_group_width, 2), Gfx::Color::White);
        current_xpos += pixel_per_group_width + pixels_inbetween_groups;
        m_gfx_falling_bars[g] += 3;
    }
}

BarsVisualizationWidget::BarsVisualizationWidget()
    : m_is_using_last(false)
    , m_adjust_frequencies(true)
    , m_logarithmic_spectrum(true)
{
    m_context_menu = GUI::Menu::construct();
    auto frequency_energy_action = GUI::Action::create_checkable("Adjust Frequency Energy", [&](GUI::Action& action) {
        m_adjust_frequencies = action.is_checked();
    });
    frequency_energy_action->set_checked(true);
    m_context_menu->add_action(frequency_energy_action);
    auto logarithmic_spectrum_action = GUI::Action::create_checkable("Scale Spectrum Logarithmically", [&](GUI::Action& action) {
        m_logarithmic_spectrum = action.is_checked();
    });
    logarithmic_spectrum_action->set_checked(true);
    m_context_menu->add_action(logarithmic_spectrum_action);

    m_fft_window = DSP::Window<float>::hann<fft_size>();

    // As we use full-overlapping windows, the passed-in data is only half the size of one FFT operation.
    MUST(set_render_sample_count(fft_size / 2));
}

void BarsVisualizationWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    m_context_menu->popup(event.screen_position());
}
