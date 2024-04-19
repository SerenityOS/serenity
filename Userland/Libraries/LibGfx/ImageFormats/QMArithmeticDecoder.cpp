/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <LibGfx/ImageFormats/QMArithmeticDecoder.h>

namespace Gfx {

// Table E.1 – Qe values and probability estimation process
// See also E.1.2 Coding conventions and approximations
// and E.2.5 Probability estimation.
struct QeEntry {
    u16 qe;          // Sub-interval for the less probable symbol.
    u16 nmps;        // Next index if the more probable symbol is decoded
    u16 nlps;        // Next index if the less probable symbol is decoded
    u16 switch_flag; // See second-to-last paragraph in E.1.2.
};
constexpr auto qe_table = to_array<QeEntry>({
    { 0x5601, 1, 1, 1 },
    { 0x3401, 2, 6, 0 },
    { 0x1801, 3, 9, 0 },
    { 0x0AC1, 4, 12, 0 },
    { 0x0521, 5, 29, 0 },
    { 0x0221, 38, 33, 0 },
    { 0x5601, 7, 6, 1 },
    { 0x5401, 8, 14, 0 },
    { 0x4801, 9, 14, 0 },
    { 0x3801, 10, 14, 0 },
    { 0x3001, 11, 17, 0 },
    { 0x2401, 12, 18, 0 },
    { 0x1C01, 13, 20, 0 },
    { 0x1601, 29, 21, 0 },
    { 0x5601, 15, 14, 1 },
    { 0x5401, 16, 14, 0 },
    { 0x5101, 17, 15, 0 },
    { 0x4801, 18, 16, 0 },
    { 0x3801, 19, 17, 0 },
    { 0x3401, 20, 18, 0 },
    { 0x3001, 21, 19, 0 },
    { 0x2801, 22, 19, 0 },
    { 0x2401, 23, 20, 0 },
    { 0x2201, 24, 21, 0 },
    { 0x1C01, 25, 22, 0 },
    { 0x1801, 26, 23, 0 },
    { 0x1601, 27, 24, 0 },
    { 0x1401, 28, 25, 0 },
    { 0x1201, 29, 26, 0 },
    { 0x1101, 30, 27, 0 },
    { 0x0AC1, 31, 28, 0 },
    { 0x09C1, 32, 29, 0 },
    { 0x08A1, 33, 30, 0 },
    { 0x0521, 34, 31, 0 },
    { 0x0441, 35, 32, 0 },
    { 0x02A1, 36, 33, 0 },
    { 0x0221, 37, 34, 0 },
    { 0x0141, 38, 35, 0 },
    { 0x0111, 39, 36, 0 },
    { 0x0085, 40, 37, 0 },
    { 0x0049, 41, 38, 0 },
    { 0x0025, 42, 39, 0 },
    { 0x0015, 43, 40, 0 },
    { 0x0009, 44, 41, 0 },
    { 0x0005, 45, 42, 0 },
    { 0x0001, 45, 43, 0 },
    { 0x5601, 46, 46, 0 },
});

ErrorOr<QMArithmeticDecoder> QMArithmeticDecoder::initialize(ReadonlyBytes data)
{
    QMArithmeticDecoder decoder { data };
    decoder.INITDEC();
    return decoder;
}

bool QMArithmeticDecoder::get_next_bit(Context& context)
{
    CX = &context;
    // Useful for comparing to Table H.1 – Encoder and decoder trace data.
    // dbg("I={} MPS={} A={:#x} C={:#x} CT={} B={:#x}", I(CX), MPS(CX), A, C, CT, B());
    u8 D = DECODE();
    // dbgln(" -> D={}", D);
    return D;
}

u16 QMArithmeticDecoder::Qe(u16 index) { return qe_table[index].qe; }
u8 QMArithmeticDecoder::NMPS(u16 index) { return qe_table[index].nmps; }
u8 QMArithmeticDecoder::NLPS(u16 index) { return qe_table[index].nlps; }
u8 QMArithmeticDecoder::SWITCH(u16 index) { return qe_table[index].switch_flag; }

u8 QMArithmeticDecoder::B(size_t offset) const
{
    // E.2.10 Minimization of the compressed data
    // "the convention is used in the decoder that when a marker code is encountered,
    //  1-bits (without bit stuffing) are supplied to the decoder until the coding interval is complete."
    if (BP + offset >= m_data.size())
        return 0xFF;
    return m_data[BP + offset];
}

void QMArithmeticDecoder::INITDEC()
{
    // E.3.5 Initialization of the decoder (INITDEC)
    // Figure G.1 – Initialization of the software conventions decoder

    // "BP, the pointer to the compressed data, is initialized to BPST (pointing to the first compressed byte)."
    auto const BPST = 0;
    BP = BPST;
    C = (B() ^ 0xFF) << 16;

    BYTEIN();

    C = C << 7;
    CT = CT - 7;
    A = 0x8000;
}

u8 QMArithmeticDecoder::DECODE()
{
    // E.3.2 Decoding a decision (DECODE)
    // Figure G.2 – Decoding an MPS or an LPS in the software-conventions decoder
    u8 D;
    A = A - Qe(I(CX));
    if (C < ((u32)A << 16)) { // `(C_high < A)` in spec
        if ((A & 0x8000) == 0) {
            D = MPS_EXCHANGE();
            RENORMD();
        } else {
            D = MPS(CX);
        }
    } else {
        C = C - ((u32)A << 16); // `C_high = C_high - A` in spec
        D = LPS_EXCHANGE();
        RENORMD();
    }
    return D;
}

u8 QMArithmeticDecoder::MPS_EXCHANGE()
{
    // Figure E.16 – Decoder MPS path conditional exchange procedure
    u8 D;
    if (A < Qe(I(CX))) {
        D = 1 - MPS(CX);
        if (SWITCH(I(CX)) == 1) {
            MPS(CX) = 1 - MPS(CX);
        }
        I(CX) = NLPS(I(CX));
    } else {
        D = MPS(CX);
        I(CX) = NMPS(I(CX));
    }
    return D;
}

u8 QMArithmeticDecoder::LPS_EXCHANGE()
{
    // Figure E.17 – Decoder LPS path conditional exchange procedure
    u8 D;
    if (A < Qe(I(CX))) {
        A = Qe(I(CX));
        D = MPS(CX);
        I(CX) = NMPS(I(CX));
    } else {
        A = Qe(I(CX));
        D = 1 - MPS(CX);
        if (SWITCH(I(CX)) == 1) {
            MPS(CX) = 1 - MPS(CX);
        }
        I(CX) = NLPS(I(CX));
    }
    return D;
}

void QMArithmeticDecoder::RENORMD()
{
    // E.3.3 Renormalization in the decoder (RENORMD)
    // Figure E.18 – Decoder renormalization procedure
    do {
        if (CT == 0)
            BYTEIN();
        A = A << 1;
        C = C << 1;
        CT = CT - 1;
    } while ((A & 0x8000) == 0);
}

void QMArithmeticDecoder::BYTEIN()
{
    // E.3.4 Compressed data input (BYTEIN)
    // Figure G.3 – Inserting a new byte into the C register in the software-conventions decoder
    if (B() == 0xFF) {
        if (B(1) > 0x8F) {
            CT = 8;
        } else {
            BP = BP + 1;
            C = C + 0xFE00 - (B() << 9);
            CT = 7;
        }
    } else {
        BP = BP + 1;
        C = C + 0xFF00 - (B() << 8);
        CT = 8;
    }
}

}
