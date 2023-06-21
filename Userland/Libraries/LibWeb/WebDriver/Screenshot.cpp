/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/HTML/AnimationFrameCallbackDriver.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/HTML/TagNames.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/WebDriver/Error.h>
#include <LibWeb/WebDriver/Screenshot.h>

namespace Web::WebDriver {

// https://w3c.github.io/webdriver/#dfn-encoding-a-canvas-as-base64
static Response encode_canvas_element(HTML::HTMLCanvasElement const& canvas)
{
    // FIXME: 1. If the canvas element’s bitmap’s origin-clean flag is set to false, return error with error code unable to capture screen.

    // 2. If the canvas element’s bitmap has no pixels (i.e. either its horizontal dimension or vertical dimension is zero) then return error with error code unable to capture screen.
    if (canvas.bitmap()->width() == 0 || canvas.bitmap()->height() == 0)
        return Error::from_code(ErrorCode::UnableToCaptureScreen, "Captured screenshot is empty"sv);

    // 3. Let file be a serialization of the canvas element’s bitmap as a file, using "image/png" as an argument.
    // 4. Let data url be a data: URL representing file. [RFC2397]
    auto data_url = canvas.to_data_url("image/png"sv, {});

    // 5. Let index be the index of "," in data url.
    auto index = data_url.find(',');
    VERIFY(index.has_value());

    // 6. Let encoded string be a substring of data url using (index + 1) as the start argument.
    auto encoded_string = data_url.substring(*index + 1);

    // 7. Return success with data encoded string.
    return JsonValue { move(encoded_string) };
}

// Common animation callback steps between:
// https://w3c.github.io/webdriver/#take-screenshot
// https://w3c.github.io/webdriver/#take-element-screenshot
Response capture_element_screenshot(Painter const& painter, Page& page, DOM::Element& element, Gfx::IntRect& rect)
{
    Optional<Response> encoded_string_or_error;

    element.document().window().animation_frame_callback_driver().add([&](auto) {
        auto viewport_rect = page.top_level_browsing_context().viewport_rect();
        rect.intersect(page.enclosing_device_rect(viewport_rect).to_type<int>());

        auto canvas_element = DOM::create_element(element.document(), HTML::TagNames::canvas, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
        auto& canvas = verify_cast<HTML::HTMLCanvasElement>(*canvas_element);

        if (!canvas.create_bitmap(rect.width(), rect.height())) {
            encoded_string_or_error = Error::from_code(ErrorCode::UnableToCaptureScreen, "Unable to create a screenshot bitmap"sv);
            return;
        }

        painter(rect, *canvas.bitmap());
        encoded_string_or_error = encode_canvas_element(canvas);
    });

    Platform::EventLoopPlugin::the().spin_until([&]() { return encoded_string_or_error.has_value(); });
    return encoded_string_or_error.release_value();
}

}
