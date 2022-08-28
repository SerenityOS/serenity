/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvasimagesource
// NOTE: This is the Variant created by the IDL wrapper generator, and needs to be updated accordingly.
using CanvasImageSource = Variant<JS::Handle<HTMLImageElement>, JS::Handle<HTMLCanvasElement>>;

// https://html.spec.whatwg.org/multipage/canvas.html#canvasdrawimage
class CanvasDrawImage {
public:
    virtual ~CanvasDrawImage() = default;

    DOM::ExceptionOr<void> draw_image(CanvasImageSource const&, float destination_x, float destination_y);
    DOM::ExceptionOr<void> draw_image(CanvasImageSource const&, float destination_x, float destination_y, float destination_width, float destination_height);
    DOM::ExceptionOr<void> draw_image(CanvasImageSource const&, float source_x, float source_y, float source_width, float source_height, float destination_x, float destination_y, float destination_width, float destination_height);

    virtual DOM::ExceptionOr<void> draw_image_internal(CanvasImageSource const&, float source_x, float source_y, float source_width, float source_height, float destination_x, float destination_y, float destination_width, float destination_height) = 0;

protected:
    CanvasDrawImage() = default;
};

}
