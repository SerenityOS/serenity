/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "VisualizationWidget.h"
#include <AK/Complex.h>
#include <LibAudio/Buffer.h>
#include <LibGUI/Frame.h>

class BarsVisualizationWidget final : public VisualizationWidget {
    C_OBJECT(BarsVisualizationWidget)

public:
    ~BarsVisualizationWidget() override = default;

private:
    BarsVisualizationWidget();

    void render(GUI::PaintEvent&, Vector<double> const&) override;
    void context_menu_event(GUI::ContextMenuEvent& event) override;

    Vector<Complex<double>> m_fft_samples;
    Vector<int> m_gfx_falling_bars;
    bool m_is_using_last;
    bool m_adjust_frequencies;
    RefPtr<GUI::Menu> m_context_menu;
};
