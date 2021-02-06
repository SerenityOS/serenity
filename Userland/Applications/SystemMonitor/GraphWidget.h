/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/CircularQueue.h>
#include <LibGUI/Frame.h>

class GraphWidget final : public GUI::Frame {
    C_OBJECT(GraphWidget)
public:
    virtual ~GraphWidget() override;

    void set_max(int max) { m_max = max; }
    int max() const { return m_max; }

    void add_value(Vector<int, 1>&&);

    struct ValueFormat {
        Color line_color { Color::Transparent };
        Color background_color { Color::Transparent };
        Color text_shadow_color { Color::Transparent };
        Function<String(int)> text_formatter;
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

    int m_max { 100 };
    Vector<ValueFormat, 1> m_value_format;
    CircularQueue<Vector<int, 1>, 4000> m_values;
    bool m_stack_values { false };

    Vector<Gfx::IntPoint, 1> m_calculated_points;
};
