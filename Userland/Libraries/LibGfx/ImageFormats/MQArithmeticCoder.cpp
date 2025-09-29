/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <LibGfx/ImageFormats/MQArithmeticCoder.h>

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

static u8& I(MQArithmeticCoderContext* cx) { return cx->I; }
static u8& MPS(MQArithmeticCoderContext* cx) { return cx->is_mps; }
static u16 Qe(u16 index) { return qe_table[index].qe; }
static u8 NMPS(u16 index) { return qe_table[index].nmps; }
static u8 NLPS(u16 index) { return qe_table[index].nlps; }
static u8 SWITCH(u16 index) { return qe_table[index].switch_flag; }

ErrorOr<MQArithmeticEncoder> MQArithmeticEncoder::initialize(u8 byte_before_first_encoded_byte)
{
    MQArithmeticEncoder encoder;
    encoder.INITENC(byte_before_first_encoded_byte);
    return encoder;
}

void MQArithmeticEncoder::encode_bit(u8 bit, MQArithmeticCoderContext& context)
{
    CX = &context;

    // Useful for comparing to Table H.1 – Encoder and decoder trace data.
    // dbg("D={} I={} MPS={} Qe={:#04X} A={:#X} C={:#08X} CT={} B={:#X}", bit, I(CX), MPS(CX), Qe(I(CX)), A, C, CT, B);
    ENCODE(bit);
    // dbgln();
}

void MQArithmeticEncoder::emit()
{
    // dbg(" OUT={:#x}", B);
    m_output_bytes.append(B);
}

ErrorOr<ByteBuffer> MQArithmeticEncoder::finalize(Trailing7FFFHandling trailing_7FFF_handling)
{
    FLUSH(trailing_7FFF_handling);

    // The spec starts BP at BPST - 1. We have no BP and append to m_output_bytes every time the spec tells us to increment BP,
    // so we must skip the first byte in m_output_bytes.
    return ByteBuffer::copy(m_output_bytes.span().slice(1));
}

void MQArithmeticEncoder::INITENC(u8 byte_before_first_encoded_byte)
{
    // E.2.8 Initialization of the encoder (INITENC)
    // Figure E.10 – Initialization of the encoder

    A = 0x8000;
    C = 0;

    // The spec has `BP = BPST - 1;` here, which means we set the B (output) pointer to before the first encoded byte.
    B = byte_before_first_encoded_byte;

    CT = 12;

    if (B == 0xFF)
        CT = 13;
}

void MQArithmeticEncoder::ENCODE(u8 D)
{
    // E.2.2 Encoding a decision (ENCODE)
    // Figure E.3 – ENCODE procedure
    if (D == 0)
        CODE0();
    else
        CODE1();
}

void MQArithmeticEncoder::CODE1()
{
    // E.2.3 Encoding a 1 or 0 (CODE1 and CODE0)
    // Figure E.4 – CODE1 procedure
    if (MPS(CX) == 1)
        CODEMPS();
    else
        CODELPS();
}

void MQArithmeticEncoder::CODE0()
{
    // E.2.3 Encoding a 1 or 0 (CODE1 and CODE0)
    // Figure E.5 – CODE0 procedure
    if (MPS(CX) == 0)
        CODEMPS();
    else
        CODELPS();
}

void MQArithmeticEncoder::CODELPS()
{
    // E.2.4 Encoding an MPS or LPS (CODEMPS and CODELPS)
    // Figure E.6 – CODELPS procedure with conditional MPS/LPS exchange
    A = A - Qe(I(CX));

    if (A < Qe(I(CX)))
        C = C + Qe(I(CX));
    else
        A = Qe(I(CX));

    if (SWITCH(I(CX)) == 1)
        MPS(CX) = 1 - MPS(CX);

    I(CX) = NLPS(I(CX));

    RENORME();
}

void MQArithmeticEncoder::CODEMPS()
{
    // E.2.4 Encoding an MPS or LPS (CODEMPS and CODELPS)
    // Figure E.7 – CODEMPS procedure with conditional MPS/LPS exchange
    A = A - Qe(I(CX));

    if ((A & 0x8000) == 0) {
        if (A < Qe(I(CX)))
            A = Qe(I(CX));
        else
            C = C + Qe(I(CX));
        I(CX) = NMPS(I(CX));
        RENORME();
    } else {
        C = C + Qe(I(CX));
    }
}

void MQArithmeticEncoder::RENORME()
{
    // E.2.6 Renormalization in the encoder (RENORME)
    // Figure E.8 – Encoder renormalization procedure
    // Note: The diagram in the spec is wrong! The A / C / CT updates have to be part of the loop, but aren't in the spec.
    // This is correct in Figure C.8 – Encoder renormalization procedure in Annex C of the JPEG2000 spec.
    do {
        A = A << 1;
        C = C << 1;
        CT = CT - 1;

        if (CT == 0)
            BYTEOUT();
    } while ((A & 0x8000) == 0);
}

void MQArithmeticEncoder::BYTEOUT()
{
    // E.2.7 Compressed data output (BYTEOUT)
    // Figure E.9 – BYTEOUT procedure for encoder

    auto emit_with_bit_stuffing = [&]() {
        // Box in lower right of Figure E.9.
        emit(); // "BP = BP + 1" in spec.
        B = C >> 20;

        // Note: The spec incorrectly has 0x7F'FFFF instead of 0xF'FFFF in Figure E.9.
        // This is fixed in Figure C.9 – BYTEOUT procedure for encoder in Annex C of the JPEG2000 spec.
        // Without the fix, the encoder would emit 0x38 instead of 0x37 for the 23rd byte emitted in Table H.1.
        C = C & 0xF'FFFF;
        CT = 7;
    };

    auto emit_without_bit_stuffing = [&]() {
        // Box in lower left of Figure E.9.
        emit(); // "BP = BP + 1" in spec.
        B = C >> 19;
        C = C & 0x7'FFFF;
        CT = 8;
    };

    if (B == 0xFF) {
        emit_with_bit_stuffing();
        return;
    }

    if (C < 0x800'0000) {
        emit_without_bit_stuffing();
        return;
    }

    B = B + 1;
    if (B == 0xFF) {
        C = C & 0x7FF'FFFF;
        emit_with_bit_stuffing();
        return;
    }

    emit_without_bit_stuffing();
}

void MQArithmeticEncoder::FLUSH(Trailing7FFFHandling trailing_7FFF_handling)
{
    // E.2.9 Termination of encoding (FLUSH)
    // Figure E.11 – FLUSH procedure
    SETBITS();
    C = C << CT;
    BYTEOUT();
    C = C << CT;
    BYTEOUT();
    if (B != 0xFF) {
        emit(); // BP = BP + 1 in spec.
        B = 0xFF;
    }

    // "Optionally remove trailing 0x7FFF pairs following the leading 0xFF"
    // This is a quote from Figure E.11 – FLUSH procedure on page 129.
    // It's apparently not marked as text in the PDF and PDF "Search" doesn't find it.
    // Due to how we do emission, we do this after the next emit(), which writes the final 0xFF.

    emit(); // BP = BP + 1 in spec.

    if (trailing_7FFF_handling == Trailing7FFFHandling::Remove) {
        while (m_output_bytes.size() >= 3
            && m_output_bytes[m_output_bytes.size() - 1] == 0xFF
            && m_output_bytes[m_output_bytes.size() - 2] == 0x7F
            && m_output_bytes[m_output_bytes.size() - 3] == 0xFF) {
            m_output_bytes.take_last();
            m_output_bytes.take_last();
        }
    }

    B = 0xAC;
    emit(); // BP = BP + 1 in spec.
}

void MQArithmeticEncoder::SETBITS()
{
    // E.2.9 Termination of encoding (FLUSH)
    // Figure E.12 – Setting the final bits in the C register
    auto TEMPC = C + A;
    C = C | 0xFFFF;
    if (C >= TEMPC)
        C = C - 0x8000;
}

ErrorOr<MQArithmeticDecoder> MQArithmeticDecoder::initialize(ReadonlyBytes data)
{
    MQArithmeticDecoder decoder { data };
    decoder.INITDEC();
    return decoder;
}

bool MQArithmeticDecoder::get_next_bit(MQArithmeticCoderContext& context)
{
    CX = &context;
    // Useful for comparing to Table H.1 – Encoder and decoder trace data.
    // dbg("I={} MPS={} A={:#X} C={:#08X} CT={} B={:#X}", I(CX), MPS(CX), A, C, CT, B());
    u8 D = DECODE();
    // dbgln(" -> D={}", D);
    return D;
}

u8 MQArithmeticDecoder::B(size_t offset) const
{
    // E.2.10 Minimization of the compressed data
    // "the convention is used in the decoder that when a marker code is encountered,
    //  1-bits (without bit stuffing) are supplied to the decoder until the coding interval is complete."
    if (BP + offset >= m_data.size())
        return 0xFF;
    return m_data[BP + offset];
}

void MQArithmeticDecoder::INITDEC()
{
    // E.3.5 Initialization of the decoder (INITDEC)
    // Figure G.1 – Initialization of the software conventions decoder
    // (Annex G replacement for Figure E.20 – Initialization of the decoder)

    // "BP, the pointer to the compressed data, is initialized to BPST (pointing to the first compressed byte)."
    auto const BPST = 0;
    BP = BPST;
    C = (B() ^ 0xFF) << 16;

    BYTEIN();

    C = C << 7;
    CT = CT - 7;
    A = 0x8000;
}

u8 MQArithmeticDecoder::DECODE()
{
    // E.3.2 Decoding a decision (DECODE)
    // Figure G.2 – Decoding an MPS or an LPS in the software-conventions decoder
    // (Annex G replacement for Figure E.15 – Decoding an MPS or an LPS)
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

u8 MQArithmeticDecoder::MPS_EXCHANGE()
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

u8 MQArithmeticDecoder::LPS_EXCHANGE()
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

void MQArithmeticDecoder::RENORMD()
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

void MQArithmeticDecoder::BYTEIN()
{
    // E.3.4 Compressed data input (BYTEIN)
    // Figure G.3 – Inserting a new byte into the C register in the software-conventions decoder
    // (Annex G replacement for Figure E.19 – BYTEIN procedure for decoder)
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
