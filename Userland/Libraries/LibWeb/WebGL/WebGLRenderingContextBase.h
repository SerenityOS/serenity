/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibGL/GLContext.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebGL/WebGLContextAttributes.h>

namespace Web::WebGL {

class WebGLRenderingContextBase
    : public RefCounted<WebGLRenderingContextBase>
    , public Weakable<WebGLRenderingContextBase> {
public:
    virtual ~WebGLRenderingContextBase();

    void present();

    void clear(GLbitfield mask);
    void clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);

protected:
    WebGLRenderingContextBase(HTML::HTMLCanvasElement& canvas_element, NonnullOwnPtr<GL::GLContext> context, WebGLContextAttributes context_creation_parameters, WebGLContextAttributes actual_context_parameters);

private:
    WeakPtr<HTML::HTMLCanvasElement> m_canvas_element;

    NonnullOwnPtr<GL::GLContext> m_context;

    // https://www.khronos.org/registry/webgl/specs/latest/1.0/#context-creation-parameters
    // Each WebGLRenderingContext has context creation parameters, set upon creation, in a WebGLContextAttributes object.
    WebGLContextAttributes m_context_creation_parameters {};

    // https://www.khronos.org/registry/webgl/specs/latest/1.0/#actual-context-parameters
    // Each WebGLRenderingContext has actual context parameters, set each time the drawing buffer is created, in a WebGLContextAttributes object.
    WebGLContextAttributes m_actual_context_parameters {};

    // https://www.khronos.org/registry/webgl/specs/latest/1.0/#webgl-context-lost-flag
    // Each WebGLRenderingContext has a webgl context lost flag, which is initially unset.
    bool m_context_lost { false };

    // WebGL presents its drawing buffer to the HTML page compositor immediately before a compositing operation, but only if at least one of the following has occurred since the previous compositing operation:
    // - Context creation
    // - Canvas resize
    // - clear, drawArrays, or drawElements has been called while the drawing buffer is the currently bound framebuffer
    bool m_should_present { true };

    void needs_to_present();
};

}
