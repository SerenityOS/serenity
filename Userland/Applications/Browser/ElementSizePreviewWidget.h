/*
 * Copyright (c) 2022, Michiel Vrins
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Rect.h>
#include <LibWeb/Layout/BoxModelMetrics.h>

namespace Browser {

class ElementSizePreviewWidget final : public GUI::AbstractScrollableWidget {
    C_OBJECT(ElementSizePreviewWidget)

public:
    void set_box_model(Web::Layout::BoxModelMetrics box_model) { m_node_box_sizing = box_model; };
    void set_node_content_height(float height) { m_node_content_height = height; };
    void set_node_content_width(float width) { m_node_content_width = width; };

private:
    virtual void paint_event(GUI::PaintEvent&) override;
    Web::Layout::BoxModelMetrics m_node_box_sizing;
    float m_node_content_height = 0;
    float m_node_content_width = 0;
};

}
