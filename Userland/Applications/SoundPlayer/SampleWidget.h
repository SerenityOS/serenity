/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "VisualizationWidget.h"
#include <LibGUI/Frame.h>

class SampleWidget final : public VisualizationWidget {
    C_OBJECT(SampleWidget)
public:
    virtual ~SampleWidget() override = default;

private:
    SampleWidget();
    virtual void render(GUI::PaintEvent&, FixedArray<float> const& samples) override;
};
