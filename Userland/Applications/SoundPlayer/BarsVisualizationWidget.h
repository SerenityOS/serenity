/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "VisualizationBase.h"
#include <AK/Complex.h>
#include <LibAudio/Buffer.h>
#include <LibGUI/Frame.h>

class BarsVisualizationWidget final : public GUI::Frame
    , public Visualization {
    C_OBJECT(BarsVisualizationWidget)

public:
    ~BarsVisualizationWidget() override;
    void set_buffer(RefPtr<Audio::Buffer> buffer) override;
    void set_samplerate(int samplerate) override;

private:
    BarsVisualizationWidget();
    void set_buffer(RefPtr<Audio::Buffer> buffer, int samples_to_use);

    void paint_event(GUI::PaintEvent&) override;
    void mousedown_event(GUI::MouseEvent& event) override;

    Vector<Complex<double>> m_sample_buffer;
    Vector<int> m_gfx_falling_bars;
    int m_last_id;
    int m_sample_count;
    int m_samplerate;
    bool m_is_using_last;
    bool m_adjust_frequencies;
    RefPtr<GUI::Menu> m_context_menu;
};
