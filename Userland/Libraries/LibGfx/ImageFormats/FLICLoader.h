/*
 * Copyright (c) 2024, Nicolas Ramz <nicolas.ramz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/MemoryStream.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Gfx {

struct FLICLoadingContext;

// Specified at: https://www.fileformat.info/format/fli/spec/e212d1bd7e2e432cb383c84b1ed7f6ee/text.htm

class FLICImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~FLICImageDecoderPlugin() override;

    virtual IntSize size() override;

    // simply decode first frame for now: that would be a good start! ;)
    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual size_t first_animated_frame_index() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;

private:
    FLICImageDecoderPlugin(FixedMemoryStream);

    OwnPtr<FLICLoadingContext> m_context;
};

}
