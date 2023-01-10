/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/WebGL/WebGLRenderingContextBase.h>

namespace Web::WebGL {

class WebGLRenderingContext final : public WebGLRenderingContextBase {
    WEB_PLATFORM_OBJECT(WebGLRenderingContext, WebGLRenderingContextBase);

public:
    static JS::ThrowCompletionOr<JS::GCPtr<WebGLRenderingContext>> create(JS::Realm&, HTML::HTMLCanvasElement& canvas_element, JS::Value options);

    virtual ~WebGLRenderingContext() override;

private:
    virtual void initialize(JS::Realm&) override;

    WebGLRenderingContext(JS::Realm&, HTML::HTMLCanvasElement&, NonnullOwnPtr<GL::GLContext> context, WebGLContextAttributes context_creation_parameters, WebGLContextAttributes actual_context_parameters);
};

}
