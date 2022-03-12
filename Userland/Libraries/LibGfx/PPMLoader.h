/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/PortableImageMapLoader.h>

namespace Gfx {

struct PPM {
    static constexpr auto ascii_magic_number = '3';
    static constexpr auto binary_magic_number = '6';
    static constexpr StringView image_type = "PPM";
    u16 max_val { 0 };
};

using PPMLoadingContext = PortableImageMapLoadingContext<PPM>;

class PPMImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    PPMImageDecoderPlugin(const u8*, size_t);
    virtual ~PPMImageDecoderPlugin() override;

    virtual IntSize size() override;

    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;

    virtual bool sniff() override;

    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;

private:
    OwnPtr<PPMLoadingContext> m_context;
};

}
