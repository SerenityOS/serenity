/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibGfx/Bitmap.h>

namespace Web::ImageDecoding {

struct Frame {
    RefPtr<Gfx::Bitmap> bitmap;
    size_t duration { 0 };
};

struct DecodedImage {
    bool is_animated { false };
    u32 loop_count { 0 };
    Vector<Frame> frames;
};

class Decoder : public RefCounted<Decoder> {
public:
    virtual ~Decoder();

    static void initialize(RefPtr<Decoder>&&);
    static Decoder& the();

    virtual Optional<DecodedImage> decode_image(ReadonlyBytes) = 0;

protected:
    explicit Decoder();
};

}
