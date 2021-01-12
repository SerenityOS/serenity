/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

namespace Web {

class SVGContext {
public:
    SVGContext()
    {
        m_states.append(State());
    }

    const Gfx::Color& fill_color() const { return state().fill_color; }
    const Gfx::Color& stroke_color() const { return state().stroke_color; }
    float stroke_width() const { return state().stroke_width; }

    void set_fill_color(Gfx::Color color) { state().fill_color = color; }
    void set_stroke_color(Gfx::Color color) { state().stroke_color = color; }
    void set_stroke_width(float width) { state().stroke_width = width; }

    void save() { m_states.append(m_states.last()); }
    void restore() { m_states.take_last(); }

private:
    struct State {
        Gfx::Color fill_color { Gfx::Color::Black };
        Gfx::Color stroke_color { Gfx::Color::Transparent };
        float stroke_width { 1.0 };
    };

    const State& state() const { return m_states.last(); }
    State& state() { return m_states.last(); }

    Vector<State> m_states;
};

}
