/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MemoryStream.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Gfx {

// This is a link to the main TIFF specification from 1992
// https://www.itu.int/itudoc/itu-t/com16/tiff-fx/docs/tiff6.pdf

// First TIFF Technical notes from 1995
// https://www.awaresystems.be/imaging/tiff/specification/TIFFPM6.pdf

namespace TIFF {
class TIFFLoadingContext;
}

class TIFFImageDecoderPlugin : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~TIFFImageDecoderPlugin() override = default;

    virtual IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;

    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() override;

private:
    TIFFImageDecoderPlugin(NonnullOwnPtr<FixedMemoryStream>);

    OwnPtr<TIFF::TIFFLoadingContext> m_context;
};

}
