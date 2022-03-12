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

struct PGM {
    static constexpr auto ascii_magic_number = '2';
    static constexpr auto binary_magic_number = '5';
    static constexpr StringView image_type = "PGM";
    u16 max_val { 0 };
};

using PGMLoadingContext = PortableImageMapLoadingContext<PGM>;

class PGMImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    PGMImageDecoderPlugin(const u8*, size_t);
    virtual ~PGMImageDecoderPlugin() override;

    virtual IntSize size() override;

    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;

    virtual bool sniff() override;

    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;

private:
    OwnPtr<PGMLoadingContext> m_context;
};

}
