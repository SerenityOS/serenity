/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <LibGUI/Frame.h>
#include <LibGfx/SystemTheme.h>

class GraphWidget final : public GUI::Frame {
    C_OBJECT(GraphWidget)
public:
    virtual ~GraphWidget() override;

    void set_max(size_t max) { m_max = max; }
    size_t max() const { return m_max; }

    void add_value(Vector<size_t, 1>&&);

    struct ValueFormat {
        Gfx::ColorRole graph_color_role { Gfx::ColorRole::Base };
        Color text_shadow_color { Color::Transparent };
        Function<String(size_t)> text_formatter;
    };
    void set_value_format(size_t index, ValueFormat&& format)
    {
        if (m_value_format.size() <= index)
            m_value_format.resize(index + 1);
        m_value_format[index] = move(format);
    }
    void set_stack_values(bool stack_values) { m_stack_values = stack_values; }

private:
    explicit GraphWidget();

    virtual void paint_event(GUI::PaintEvent&) override;

    size_t m_max { 100 };
    Vector<ValueFormat, 1> m_value_format;
    CircularQueue<Vector<size_t, 1>, 4000> m_values;
    bool m_stack_values { false };

    Vector<Gfx::IntPoint, 1> m_calculated_points;
};
