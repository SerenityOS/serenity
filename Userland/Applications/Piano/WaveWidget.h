/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibAudio/Sample.h>
#include <LibGUI/Frame.h>

class TrackManager;

class WaveWidget final : public GUI::Frame {
    C_OBJECT(WaveWidget)
public:
    virtual ~WaveWidget() override = default;

private:
    // Scales the sample-y value down by a bit, so that it doesn't look like it is clipping.
    static constexpr float rescale_factor = 1.2f;

    explicit WaveWidget(TrackManager&);

    virtual void paint_event(GUI::PaintEvent&) override;

    int sample_to_y(float sample, float sample_max) const;

    TrackManager& m_track_manager;
};
