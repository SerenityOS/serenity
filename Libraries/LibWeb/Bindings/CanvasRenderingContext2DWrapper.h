/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibWeb/Bindings/Wrapper.h>

namespace Web {
namespace Bindings {

class CanvasRenderingContext2DWrapper final : public Wrapper {
public:
    explicit CanvasRenderingContext2DWrapper(CanvasRenderingContext2D&);
    virtual ~CanvasRenderingContext2DWrapper() override;

    CanvasRenderingContext2D& impl() { return m_impl; }
    const CanvasRenderingContext2D& impl() const { return m_impl; }

private:
    virtual const char* class_name() const override { return "CanvasRenderingContext2DWrapper"; }

    static JS::Value fill_rect(JS::Interpreter&);
    static JS::Value stroke_rect(JS::Interpreter&);
    static JS::Value draw_image(JS::Interpreter&);
    static JS::Value scale(JS::Interpreter&);
    static JS::Value translate(JS::Interpreter&);
    static JS::Value fill_style_getter(JS::Interpreter&);
    static void fill_style_setter(JS::Interpreter&, JS::Value);
    static JS::Value stroke_style_getter(JS::Interpreter&);
    static void stroke_style_setter(JS::Interpreter&, JS::Value);
    static JS::Value line_width_getter(JS::Interpreter&);
    static void line_width_setter(JS::Interpreter&, JS::Value);
    static JS::Value begin_path(JS::Interpreter&);
    static JS::Value close_path(JS::Interpreter&);
    static JS::Value stroke(JS::Interpreter&);
    static JS::Value move_to(JS::Interpreter&);
    static JS::Value line_to(JS::Interpreter&);

    NonnullRefPtr<CanvasRenderingContext2D> m_impl;
};

CanvasRenderingContext2DWrapper* wrap(JS::Heap&, CanvasRenderingContext2D&);

}
}
