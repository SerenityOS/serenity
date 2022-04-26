/*
 * Copyright (c) 2022, Torsten Engelmann
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Image.h"
#include <LibGUI/AbstractScrollableWidget.h>

namespace PixelPaint {

class HistogramWidget final
    : public GUI::Frame
    , ImageClient {
    C_OBJECT(HistogramWidget);

public:
    virtual ~HistogramWidget() override;

    void set_image(Image*);
    void image_changed();
    void set_color_at_mouseposition(Color);

private:
    HistogramWidget();

    virtual void paint_event(GUI::PaintEvent&) override;

    ErrorOr<void> rebuild_histogram_data();
    int m_widget_height = 0;
    Color m_color_at_mouseposition = Color::Transparent;
    RefPtr<Image> m_image;

    struct HistogramData {
        Vector<int> red;
        Vector<int> green;
        Vector<int> blue;
        Vector<int> brightness;
    };
    HistogramData m_data;
};

}
