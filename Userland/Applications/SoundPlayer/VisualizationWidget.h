/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibAudio/Buffer.h>
#include <LibGUI/Frame.h>

class VisualizationWidget : public GUI::Frame {
    C_OBJECT(VisualizationWidget)

public:
    virtual void set_buffer(RefPtr<Audio::Buffer> buffer) = 0;
    virtual void set_samplerate(int) { }
    virtual void start_new_file(StringView) { }

protected:
    VisualizationWidget() = default;
    virtual ~VisualizationWidget() = default;
};
