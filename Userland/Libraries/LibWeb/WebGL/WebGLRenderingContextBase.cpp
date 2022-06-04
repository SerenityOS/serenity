/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GLContext.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/WebGL/WebGLRenderingContextBase.h>

namespace Web::WebGL {

WebGLRenderingContextBase::WebGLRenderingContextBase(HTML::HTMLCanvasElement& canvas_element, NonnullOwnPtr<GL::GLContext> context, WebGLContextAttributes context_creation_parameters, WebGLContextAttributes actual_context_parameters)
    : m_canvas_element(canvas_element)
    , m_context(move(context))
    , m_context_creation_parameters(move(context_creation_parameters))
    , m_actual_context_parameters(move(actual_context_parameters))
{
}

WebGLRenderingContextBase::~WebGLRenderingContextBase() = default;

}
