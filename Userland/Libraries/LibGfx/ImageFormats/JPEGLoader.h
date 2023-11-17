/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022-2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MemoryStream.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Gfx {

struct JPEGLoadingContext;

// For the specification, see: https://www.w3.org/Graphics/JPEG/itu-t81.pdf

struct JPEGDecoderOptions {
    enum class CMYK {
        // For standalone jpeg files.
        Normal,

        // For jpeg data embedded in PDF files.
        PDF,
    };
    CMYK cmyk { CMYK::Normal };
};

class JPEGImageDecoderPlugin : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create_with_options(ReadonlyBytes, JPEGDecoderOptions = {});

    virtual ~JPEGImageDecoderPlugin() override;
    virtual IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;
    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() override;

private:
    JPEGImageDecoderPlugin(NonnullOwnPtr<JPEGLoadingContext>);

    NonnullOwnPtr<JPEGLoadingContext> m_context;
};

}
