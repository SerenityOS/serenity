/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MemoryStream.h>
#include <AK/OwnPtr.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Gfx {

struct JBIG2LoadingContext;

namespace JBIG2 {

// E.3 Arithmetic decoding procedure, but with the changes described in
// Annex G Arithmetic decoding procedure (software conventions).
// Exposed for testing.
class ArithmeticDecoder {
public:
    struct Context {
        u8 I { 0 };      // Index I stored for context CX (E.2.4)
        u8 is_mps { 0 }; // "More probable symbol" (E.1.1). 0 or 1.
    };

    static ErrorOr<ArithmeticDecoder> initialize(ReadonlyBytes data);

    bool get_next_bit(Context& context);

private:
    ArithmeticDecoder(ReadonlyBytes data)
        : m_data(data)
    {
    }

    ReadonlyBytes m_data;

    // The code below uses names from the spec, so that the algorithms look exactly like the flowcharts in the spec.

    // Abbreviations:
    // "CX": "Context" (E.1)
    // "D": "Decision" (as in "encoder input" / "decoder output") (E.1)
    // "I(CX)": "Index I stored for context CX" (E.2.4)
    // "MPS": "More probable symbol" (E.1.1)
    // "LPS": "Less probable symbol" (E.1.1)

    void INITDEC();
    u8 DECODE(); // Returns a single decoded bit.
    u8 MPS_EXCHANGE();
    u8 LPS_EXCHANGE();
    void RENORMD();
    void BYTEIN();

    u8 B(size_t offset = 0) const; // Byte pointed to by BP.
    size_t BP { 0 };               // Pointer into compressed data.

    // E.3.1 Decoder code register conventions
    u32 C { 0 }; // Consists of u16 C_high, C_low.
    u16 A { 0 }; // Current value of the fraction. Fixed precision; 0x8000 is equivalent to 0.75.

    u8 CT { 0 }; // Count of the number of bits in C.

    Context* CX { nullptr };
    static u8& I(Context* cx) { return cx->I; }
    static u8& MPS(Context* cx) { return cx->is_mps; }
    static u16 Qe(u16);
    static u8 NMPS(u16);
    static u8 NLPS(u16);
    static u8 SWITCH(u16);
};

}

class JBIG2ImageDecoderPlugin : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~JBIG2ImageDecoderPlugin() override = default;

    virtual IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;

    static ErrorOr<ByteBuffer> decode_embedded(Vector<ReadonlyBytes>);

private:
    JBIG2ImageDecoderPlugin();

    OwnPtr<JBIG2LoadingContext> m_context;
};

}
