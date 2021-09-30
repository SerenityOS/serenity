/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "VisualizationWidget.h"
#include <LibAudio/Buffer.h>
#include <LibGUI/Frame.h>

class NoVisualizationWidget final : public VisualizationWidget {
    C_OBJECT(NoVisualizationWidget)

public:
    ~NoVisualizationWidget() override;
    void set_buffer(RefPtr<Audio::Buffer>) override;

private:
    void paint_event(GUI::PaintEvent&) override;
    NoVisualizationWidget();

    RefPtr<Gfx::Bitmap> m_serenity_bg;
};
