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

    virtual void paint_event(GUI::PaintEvent&) override;

    ErrorOr<void> rebuild_histogram_data();

    struct HistogramData {
        Vector<int> red;
        Vector<int> green;
        Vector<int> blue;
        Vector<int> brightness;
    };
    HistogramData m_data;
};

}
