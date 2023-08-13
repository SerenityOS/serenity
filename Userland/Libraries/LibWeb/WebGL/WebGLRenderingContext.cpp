/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/WebGL/EventNames.h>
#include <LibWeb/WebGL/WebGLContextEvent.h>
#include <LibWeb/WebGL/WebGLRenderingContext.h>

namespace Web::WebGL {

// https://www.khronos.org/registry/webgl/specs/latest/1.0/#fire-a-webgl-context-event
static void fire_webgl_context_event(HTML::HTMLCanvasElement& canvas_element, FlyString const& type)
{
    // To fire a WebGL context event named e means that an event using the WebGLContextEvent interface, with its type attribute [DOM4] initialized to e, its cancelable attribute initialized to true, and its isTrusted attribute [DOM4] initialized to true, is to be dispatched at the given object.
    // FIXME: Consider setting a status message.
    auto event = WebGLContextEvent::create(canvas_element.realm(), type, WebGLContextEventInit {});
    event->set_is_trusted(true);
    event->set_cancelable(true);
    canvas_element.dispatch_event(*event);
}

// https://www.khronos.org/registry/webgl/specs/latest/1.0/#fire-a-webgl-context-creation-error
static void fire_webgl_context_creation_error(HTML::HTMLCanvasElement& canvas_element)
{
    // 1. Fire a WebGL context event named "webglcontextcreationerror" at canvas, optionally with its statusMessage attribute set to a platform dependent string about the nature of the failure.
    fire_webgl_context_event(canvas_element, EventNames::webglcontextcreationerror);
}

JS::ThrowCompletionOr<JS::GCPtr<WebGLRenderingContext>> WebGLRenderingContext::create(JS::Realm& realm, HTML::HTMLCanvasElement& canvas_element, JS::Value options)
{
    // We should be coming here from getContext being called on a wrapped <canvas> element.
    auto context_attributes = TRY(convert_value_to_context_attributes_dictionary(canvas_element.vm(), options));

    bool created_bitmap = canvas_element.create_bitmap(/* minimum_width= */ 1, /* minimum_height= */ 1);
    if (!created_bitmap) {
        fire_webgl_context_creation_error(canvas_element);
        return JS::GCPtr<WebGLRenderingContext> { nullptr };
    }

    auto context_or_error = GL::create_context(*canvas_element.bitmap());
    if (context_or_error.is_error()) {
        fire_webgl_context_creation_error(canvas_element);
        return JS::GCPtr<WebGLRenderingContext> { nullptr };
    }
    return realm.heap().allocate<WebGLRenderingContext>(realm, realm, canvas_element, context_or_error.release_value(), context_attributes, context_attributes);
}

WebGLRenderingContext::WebGLRenderingContext(JS::Realm& realm, HTML::HTMLCanvasElement& canvas_element, NonnullOwnPtr<GL::GLContext> context, WebGLContextAttributes context_creation_parameters, WebGLContextAttributes actual_context_parameters)
    : WebGLRenderingContextBase(realm, canvas_element, move(context), move(context_creation_parameters), move(actual_context_parameters))
{
}

WebGLRenderingContext::~WebGLRenderingContext() = default;

void WebGLRenderingContext::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::WebGLRenderingContextPrototype>(realm, "WebGLRenderingContext"));
}

}
