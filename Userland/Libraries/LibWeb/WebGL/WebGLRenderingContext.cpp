/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Wrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/WebGL/WebGLContextEvent.h>
#include <LibWeb/WebGL/WebGLRenderingContext.h>

namespace Web::WebGL {

// https://www.khronos.org/registry/webgl/specs/latest/1.0/#fire-a-webgl-context-event
static void fire_webgl_context_event(HTML::HTMLCanvasElement& canvas_element, FlyString const& type)
{
    // To fire a WebGL context event named e means that an event using the WebGLContextEvent interface, with its type attribute [DOM4] initialized to e, its cancelable attribute initialized to true, and its isTrusted attribute [DOM4] initialized to true, is to be dispatched at the given object.
    // FIXME: Consider setting a status message.
    auto event = WebGLContextEvent::create(canvas_element.document().window(), type, WebGLContextEventInit {});
    event->set_is_trusted(true);
    event->set_cancelable(true);
    canvas_element.dispatch_event(*event);
}

// https://www.khronos.org/registry/webgl/specs/latest/1.0/#fire-a-webgl-context-creation-error
static void fire_webgl_context_creation_error(HTML::HTMLCanvasElement& canvas_element)
{
    // 1. Fire a WebGL context event named "webglcontextcreationerror" at canvas, optionally with its statusMessage attribute set to a platform dependent string about the nature of the failure.
    fire_webgl_context_event(canvas_element, "webglcontextcreationerror"sv);
}

JS::ThrowCompletionOr<RefPtr<WebGLRenderingContext>> WebGLRenderingContext::create(HTML::HTMLCanvasElement& canvas_element, JS::Value options)
{
    // We should be coming here from getContext being called on a wrapped <canvas> element.
    auto context_attributes = TRY(convert_value_to_context_attributes_dictionary(canvas_element.vm(), options));

    bool created_bitmap = canvas_element.create_bitmap(/* minimum_width= */ 1, /* minimum_height= */ 1);
    if (!created_bitmap) {
        fire_webgl_context_creation_error(canvas_element);
        return RefPtr<WebGLRenderingContext> { nullptr };
    }

#ifndef __serenity__
    // FIXME: Make WebGL work on other platforms.
    (void)context_attributes;
    dbgln("FIXME: WebGL not supported on the current platform");
    fire_webgl_context_creation_error(canvas_element);
    return RefPtr<WebGLRenderingContext> { nullptr };
#else
    // FIXME: LibGL currently doesn't propagate context creation errors.
    auto context = GL::create_context(*canvas_element.bitmap());
    return adopt_ref(*new WebGLRenderingContext(canvas_element, move(context), context_attributes, context_attributes));
#endif
}

}
