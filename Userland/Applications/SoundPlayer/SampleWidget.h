/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "VisualizationWidget.h"
#include <LibGUI/Frame.h>

namespace Audio {
class Buffer;
}

class SampleWidget final : public VisualizationWidget {
    C_OBJECT(SampleWidget)
public:
    virtual ~SampleWidget() override = default;

    void set_buffer(RefPtr<Audio::Buffer>) override;

private:
    SampleWidget() = default;
    virtual void paint_event(GUI::PaintEvent&) override;

    RefPtr<Audio::Buffer> m_buffer;
};
