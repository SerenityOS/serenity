/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibGfx/Forward.h>
#include <LibJS/Heap/Cell.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/images.html#img-req-data
class DecodedImageData : public RefCounted<DecodedImageData> {
public:
    virtual ~DecodedImageData();

    virtual RefPtr<Gfx::Bitmap const> bitmap(size_t frame_index) const = 0;
    virtual int frame_duration(size_t frame_index) const = 0;

    virtual size_t frame_count() const = 0;
    virtual size_t loop_count() const = 0;
    virtual bool is_animated() const = 0;

    virtual Optional<int> natural_width() const = 0;
    virtual Optional<int> natural_height() const = 0;

protected:
    DecodedImageData();
};

}
