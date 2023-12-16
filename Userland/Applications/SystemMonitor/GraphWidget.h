/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <LibGUI/Frame.h>
#include <LibGfx/SystemTheme.h>

namespace SystemMonitor {

class GraphWidget final : public GUI::Frame {
    C_OBJECT(GraphWidget)
public:
    virtual ~GraphWidget() override = default;

    void set_max(u64 max) { m_max = max; }
    u64 max() const { return m_max; }

    void add_value(Vector<u64, 1>&&);

    struct ValueFormat {
        Gfx::ColorRole graph_color_role { Gfx::ColorRole::Base };
        Color text_shadow_color { Color::Transparent };
        Function<ByteString(u64)> text_formatter;
    };
    void set_value_format(size_t index, ValueFormat&& format)
    {
        if (m_value_format.size() <= index)
            m_value_format.resize(index + 1);
        m_value_format[index] = move(format);
    }
    void set_stack_values(bool stack_values);
    bool stack_values() const { return m_stack_values; }

private:
    explicit GraphWidget();

    virtual void paint_event(GUI::PaintEvent&) override;

    u64 m_max { 100 };
    Vector<ValueFormat, 1> m_value_format;
    CircularQueue<Vector<u64, 1>, 4000> m_values;
    bool m_stack_values { false };

    Vector<Gfx::IntPoint, 1> m_calculated_points;
};

}
