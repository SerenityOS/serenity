/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/WebGL/WebGLRenderingContextBase.h>

namespace Web::WebGL {

class WebGLRenderingContext
    : public WebGLRenderingContextBase
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::WebGLRenderingContextWrapper;

    static JS::ThrowCompletionOr<RefPtr<WebGLRenderingContext>> create(HTML::HTMLCanvasElement& canvas_element, JS::Value options);

    virtual ~WebGLRenderingContext() override = default;

private:
    WebGLRenderingContext(HTML::HTMLCanvasElement& canvas_element, NonnullOwnPtr<GL::GLContext> context, WebGLContextAttributes context_creation_parameters, WebGLContextAttributes actual_context_parameters)
        : WebGLRenderingContextBase(canvas_element, move(context), move(context_creation_parameters), move(actual_context_parameters))
    {
    }
};

}
