/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>

namespace Gfx {

// This is the arithmetic decoder described in Annex E of the JBIG2 spec.
// See JBIG2Loader.cpp for the JBIG2 spec link.
//
// It's also used in JPEG2000 and also described in Annex C of the JPEG2000 spec.
// See JPEG2000Loader.cpp for the JPEG2000 spec link.

// E.3 Arithmetic decoding procedure, but with the changes described in
// Annex G Arithmetic decoding procedure (software conventions).
// Exposed for testing.
class QMArithmeticDecoder {
public:
    struct Context {
        u8 I { 0 };      // Index I stored for context CX (E.2.4)
        u8 is_mps { 0 }; // "More probable symbol" (E.1.1). 0 or 1.
    };

    static ErrorOr<QMArithmeticDecoder> initialize(ReadonlyBytes data);

    bool get_next_bit(Context& context);

private:
    QMArithmeticDecoder(ReadonlyBytes data)
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
