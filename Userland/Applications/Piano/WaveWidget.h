/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RollWidget.h"
#include <AK/NonnullRefPtr.h>
#include <LibDSP/TrackManager.h>
#include <LibDSP/Transport.h>
#include <LibGUI/Frame.h>

class WaveWidget final : public GUI::Frame {
    C_OBJECT(WaveWidget)
public:
    virtual ~WaveWidget() override;

private:
    WaveWidget(NonnullRefPtr<LibDSP::TrackManager>, NonnullRefPtr<LibDSP::Transport>, NonnullRefPtr<RollWidget>);
    size_t synth_wave_index();

    virtual void paint_event(GUI::PaintEvent&) override;

    int sample_to_y(int sample) const;

    NonnullRefPtr<RollWidget> m_roll_widget;
    NonnullRefPtr<LibDSP::TrackManager> m_track_manager;
    NonnullRefPtr<LibDSP::Transport> m_transport;
};
