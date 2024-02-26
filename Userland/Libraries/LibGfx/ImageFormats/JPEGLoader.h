/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022-2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/HashMap.h>
#include <AK/MemoryStream.h>
#include <AK/Optional.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/ImageFormats/JPEGHuffmanTable.h>

namespace Gfx {

namespace JPEG {
struct LoadingContext;

using QuantizationTables = Array<Optional<Array<u16, 64>>, 4>;

struct Tables {
    QuantizationTables quantization_tables {};
    HashMap<u8, HuffmanTable> dc_tables {};
    HashMap<u8, HuffmanTable> ac_tables {};
};

}

// For the specification, see: https://www.w3.org/Graphics/JPEG/itu-t81.pdf

struct JPEGDecoderOptions {
    enum class CMYK {
        // For standalone jpeg files.
        Normal,

        // For jpeg data embedded in PDF files.
        PDF,
    };
    CMYK cmyk { CMYK::Normal };

    enum class TIFFSpecialHandling {
        None,
        TablesOnly,
    };

    TIFFSpecialHandling tiff_special_handling { TIFFSpecialHandling::None };
};

class JPEGImageDecoderPlugin : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create_with_options(ReadonlyBytes, JPEGDecoderOptions = {});

    // This is intended to be used by the TIFF decoder
    static ErrorOr<JPEG::Tables> read_tables(ReadonlyBytes);

    virtual ~JPEGImageDecoderPlugin() override;
    virtual IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;

    virtual Optional<Metadata const&> metadata() override;

    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() override;

    virtual NaturalFrameFormat natural_frame_format() const override;
    virtual ErrorOr<NonnullRefPtr<CMYKBitmap>> cmyk_frame() override;

private:
    JPEGImageDecoderPlugin(NonnullOwnPtr<JPEG::LoadingContext>);

    NonnullOwnPtr<JPEG::LoadingContext> m_context;
};

}
