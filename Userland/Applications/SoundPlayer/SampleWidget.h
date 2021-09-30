/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
    virtual ~SampleWidget() override;

    void set_buffer(RefPtr<Audio::Buffer>) override;

private:
    SampleWidget();
    virtual void paint_event(GUI::PaintEvent&) override;

    RefPtr<Audio::Buffer> m_buffer;
};
