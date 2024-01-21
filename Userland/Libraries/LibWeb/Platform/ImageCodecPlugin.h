/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Size.h>

namespace Web::Platform {

struct Frame {
    RefPtr<Gfx::Bitmap> bitmap;
    size_t duration { 0 };
};

struct DecodedImage {
    Gfx::IntSize size {};
    bool is_animated { false };
    u32 loop_count { 0 };
    Vector<Frame> frames;
};

class ImageCodecPlugin {
public:
    static ImageCodecPlugin& the();
    static void install(ImageCodecPlugin&);

    virtual ~ImageCodecPlugin();

    virtual Optional<DecodedImage> decode_image(ReadonlyBytes) = 0;
};

}
