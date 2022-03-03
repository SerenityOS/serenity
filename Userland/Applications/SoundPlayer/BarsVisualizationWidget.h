/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "VisualizationWidget.h"
#include <AK/Array.h>
#include <AK/Complex.h>
#include <AK/FixedArray.h>
#include <LibAudio/Buffer.h>
#include <LibGUI/Frame.h>

class BarsVisualizationWidget final : public VisualizationWidget {
    C_OBJECT(BarsVisualizationWidget)

public:
    ~BarsVisualizationWidget() override = default;

private:
    BarsVisualizationWidget();

    void render(GUI::PaintEvent&, FixedArray<double> const&) override;
    void context_menu_event(GUI::ContextMenuEvent& event) override;

    static constexpr size_t fft_size = 512;
    static constexpr size_t bar_count = 64;
    static constexpr size_t values_per_bar = (fft_size / 2) / bar_count;

    Array<Complex<double>, fft_size> m_fft_samples {};
    Array<double, fft_size> m_fft_window {};
    Array<double, fft_size / 2> m_previous_samples {};
    Array<int, bar_count> m_gfx_falling_bars {};
    bool m_is_using_last;
    bool m_adjust_frequencies;
    RefPtr<GUI::Menu> m_context_menu;
};
