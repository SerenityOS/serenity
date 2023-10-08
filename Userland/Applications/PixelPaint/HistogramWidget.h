/*
 * Copyright (c) 2022, Torsten Engelmann
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Image.h"
#include "ScopeWidget.h"

namespace PixelPaint {

class HistogramWidget final
    : public ScopeWidget {
    C_OBJECT(HistogramWidget);

public:
    virtual ~HistogramWidget() = default;
    virtual void image_changed() override;

private:
    HistogramWidget() = default;

    virtual AK::StringView widget_config_name() const override { return "ShowHistogram"sv; }
    virtual void paint_event(GUI::PaintEvent&) override;

    ErrorOr<void> rebuild_histogram_data();

    struct HistogramData {
        Vector<int> red;
        Vector<int> green;
        Vector<int> blue;
        Vector<int> brightness;
        int max_brightness_frequency;
        int max_color_frequency;
    };
    HistogramData m_data;
};

}
