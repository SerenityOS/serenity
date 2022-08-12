/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/ImageData.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvasdrawpath
class CanvasImageData {
public:
    virtual ~CanvasImageData() = default;

    virtual RefPtr<ImageData> create_image_data(int width, int height) const = 0;
    virtual DOM::ExceptionOr<RefPtr<ImageData>> get_image_data(int x, int y, int width, int height) const = 0;
    virtual void put_image_data(ImageData const&, float x, float y) = 0;

protected:
    CanvasImageData() = default;
};

}
