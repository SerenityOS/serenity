/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// Spec: ITU-T_T_88__08_2018.pdf in the zip file here:
// https://www.itu.int/rec/T-REC-T.88-201808-I
// JBIG2Loader.cpp has many spec notes.

#include <AK/BitStream.h>
#include <AK/Enumerate.h>
#include <AK/HashMap.h>
#include <AK/IntegralMath.h>
#include <AK/MemoryStream.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Stream.h>
#include <AK/Utf16View.h>
#include <LibCompress/Huffman.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibGfx/ImageFormats/CCITTEncoder.h>
#include <LibGfx/ImageFormats/JBIG2Loader.h>
#include <LibGfx/ImageFormats/JBIG2Writer.h>
#include <LibGfx/ImageFormats/MQArithmeticCoder.h>
#include <LibTextCodec/Encoder.h>

namespace Gfx {

namespace JBIG2 {

ArithmeticIntegerEncoder::ArithmeticIntegerEncoder()
{
    contexts.resize(1 << 9);
}

ErrorOr<void> ArithmeticIntegerEncoder::encode(MQArithmeticEncoder& encoder, Optional<i32> maybe_value)
{
    // A.2 Procedure for decoding values (except IAID), but in reverse.
    // "1) Set:
    //    PREV = 1"
    u16 PREV = 1;

    // "2) Follow the flowchart in Figure A.1. Decode each bit with CX equal to "IAx + PREV" where "IAx" represents the identifier
    //     of the current arithmetic integer decoding procedure, "+" represents concatenation, and the rightmost 9 bits of PREV are used."
    auto encode_bit = [&](u8 D) {
        encoder.encode_bit(D, contexts[PREV & 0x1FF]);
        // "3) After each bit is decoded:
        //     If PREV < 256 set:
        //         PREV = (PREV << 1) OR D
        //     Otherwise set:
        //         PREV = (((PREV << 1) OR D) AND 511) OR 256
        //     where D represents the value of the just-decoded bit.
        if (PREV < 256)
            PREV = (PREV << 1) | (u16)D;
        else
            PREV = (((PREV << 1) | (u16)D) & 511) | 256;
        return D;
    };

    auto encode_bits = [&](int value, int number_of_bits) {
        for (int i = 0; i < number_of_bits; ++i)
            encode_bit((value >> (number_of_bits - i - 1)) & 1);
    };

    // Figure A.1 – Flowchart for the integer arithmetic decoding procedures (except IAID)
    i32 value;
    bool is_negative;
    if (!maybe_value.has_value()) {
        is_negative = true;
        value = 0;
    } else {
        is_negative = maybe_value.value() < 0;
        value = abs(maybe_value.value());
    }
    encode_bit(is_negative ? 1 : 0);

    for (u32 bits : { 2, 4, 6, 8, 12 }) {
        if (value < (1 << bits)) {
            encode_bit(0);
            encode_bits(value, bits);
            return {};
        }
        value -= (1 << bits);
        encode_bit(1);
    }

    encode_bits(value, 32);

    // "4) The sequence of bits decoded, interpreted according to Table A.1, gives the value that is the result of this invocation
    //     of the integer arithmetic decoding procedure."
    return {};
}

ErrorOr<void> ArithmeticIntegerEncoder::encode_non_oob(MQArithmeticEncoder& encoder, i32 value)
{
    return encode(encoder, value);
}

ArithmeticIntegerIDEncoder::ArithmeticIntegerIDEncoder(u32 code_length)
    : m_code_length(code_length)
{
    contexts.resize(1 << (code_length + 1));
}

ErrorOr<void> ArithmeticIntegerIDEncoder::encode(MQArithmeticEncoder& encoder, u32 value)
{
    // A.3 The IAID decoding procedure, but in reverse.
    value = value + (1u << m_code_length);
    for (u8 i = 0; i < m_code_length; ++i)
        encoder.encode_bit((value >> (m_code_length - i - 1)) & 1, contexts[value >> (m_code_length - i)]);
    return {};
}

}

namespace {

// Similar to 6.2.2 Input parameters, but with an input image.
struct GenericRegionEncodingInputParameters {
    bool is_modified_modified_read { false }; // "MMR" in spec.
    BilevelImage const& image;                // Of dimensions "GBW" x "GBH" in spec terms.
    u8 gb_template { 0 };
    bool is_typical_prediction_used { false };          // "TPGDON" in spec.
    bool is_extended_reference_template_used { false }; // "EXTTEMPLATE" in spec.
    Optional<BilevelImage const&> skip_pattern {};      // "USESKIP", "SKIP" in spec.

    Array<JBIG2::AdaptiveTemplatePixel, 12> adaptive_template_pixels {}; // "GBATX" / "GBATY" in spec.
    // FIXME: GBCOLS, GBCOMBOP, COLEXTFLAG

    enum RequireEOFBAfterMMR {
        No,
        Yes,
    } require_eof_after_mmr { RequireEOFBAfterMMR::No };

    // If is_modified_modified_read is true, generic_region_encoding_procedure() writes data to this stream.
    Stream* stream { nullptr };

    // If is_modified_modified_read is false, generic_region_encoding_procedure() writes data to this encoder.
    MQArithmeticEncoder* arithmetic_encoder { nullptr };
};

}

// 6.2 Generic region decoding procedure, but in reverse.
static ErrorOr<void> generic_region_encoding_procedure(GenericRegionEncodingInputParameters const& inputs, Optional<JBIG2::GenericContexts>& maybe_contexts)
{
    // FIXME: Try to come up with a way to share more code with generic_region_decoding_procedure().
    auto width = inputs.image.width();
    auto height = inputs.image.height();

    if (inputs.is_modified_modified_read) {
        // FIXME: It's a bit wasteful to re-convert the BilevelImage to a Bitmap here.
        auto append_eofb = CCITT::Group4EncodingOptions::AppendEOFB::No;
        if (inputs.require_eof_after_mmr == GenericRegionEncodingInputParameters::RequireEOFBAfterMMR::Yes)
            append_eofb = CCITT::Group4EncodingOptions::AppendEOFB::Yes;
        TRY(Gfx::CCITT::Group4Encoder::encode(*inputs.stream, TRY(inputs.image.to_gfx_bitmap()), { append_eofb }));
        return {};
    }

    auto& contexts = maybe_contexts.value();

    // 6.2.5 Decoding using a template and arithmetic coding
    if (inputs.is_extended_reference_template_used)
        return Error::from_string_literal("JBIG2Writer: Cannot encode EXTTEMPLATE yet");

    int number_of_adaptive_template_pixels = inputs.gb_template == 0 ? 4 : 1;
    for (int i = 0; i < number_of_adaptive_template_pixels; ++i)
        TRY(check_valid_adaptive_template_pixel(inputs.adaptive_template_pixels[i]));

    if (inputs.skip_pattern.has_value() && (inputs.skip_pattern->width() != width || inputs.skip_pattern->height() != height))
        return Error::from_string_literal("JBIG2Writer: Invalid USESKIP dimensions");

    if (inputs.skip_pattern.has_value())
        return Error::from_string_literal("JBIG2Writer: Cannot encode USESKIP yet");

    static constexpr auto get_pixel = [](BilevelImage const& buffer, int x, int y) -> bool {
        // 6.2.5.2 Coding order and edge conventions
        // "• All pixels lying outside the bounds of the actual bitmap have the value 0."
        // We don't have to check y >= buffer->height() because check_valid_adaptive_template_pixel() rejects y > 0.
        if (x < 0 || x >= (int)buffer.width() || y < 0)
            return false;
        return buffer.get_bit(x, y);
    };

    static constexpr auto get_pixels = [](NonnullRefPtr<BilevelImage> const& buffer, int x, int y, u8 width) -> u8 {
        if (x + width < 0 || x >= (int)buffer->width() || y < 0)
            return 0;
        auto corrected_x = max(x, 0);
        auto right_end = x + width;
        auto corrected_right_end = min(right_end, buffer->width());
        auto in_bounds = corrected_right_end - corrected_x;
        auto res = buffer->get_bits(corrected_x, y, in_bounds);
        res <<= (right_end - corrected_right_end);
        return res;
    };

    // Figure 3(a) – Template when GBTEMPLATE = 0 and EXTTEMPLATE = 0,
    constexpr auto compute_context_0 = [](BilevelImage const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        for (int i = 0; i < 4; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[i].x, y + adaptive_pixels[i].y);
        result = (result << 3) | get_pixels(buffer, x - 1, y - 2, 3);
        result = (result << 5) | get_pixels(buffer, x - 2, y - 1, 5);
        result = (result << 4) | get_pixels(buffer, x - 4, y, 4);
        return result;
    };

    // Figure 4 – Template when GBTEMPLATE = 1
    auto compute_context_1 = [](BilevelImage const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        result = (result << 4) | get_pixels(buffer, x - 1, y - 2, 4);
        result = (result << 5) | get_pixels(buffer, x - 2, y - 1, 5);
        result = (result << 3) | get_pixels(buffer, x - 3, y, 3);
        return result;
    };

    // Figure 5 – Template when GBTEMPLATE = 2
    auto compute_context_2 = [](BilevelImage const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        result = (result << 3) | get_pixels(buffer, x - 1, y - 2, 3);
        result = (result << 4) | get_pixels(buffer, x - 2, y - 1, 4);
        result = (result << 2) | get_pixels(buffer, x - 2, y, 2);
        return result;
    };

    // Figure 6 – Template when GBTEMPLATE = 3
    auto compute_context_3 = [](BilevelImage const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        result = (result << 5) | get_pixels(buffer, x - 3, y - 1, 5);
        result = (result << 4) | get_pixels(buffer, x - 4, y, 4);
        return result;
    };

    u16 (*compute_context)(BilevelImage const&, ReadonlySpan<JBIG2::AdaptiveTemplatePixel>, int, int);
    if (inputs.gb_template == 0)
        compute_context = compute_context_0;
    else if (inputs.gb_template == 1)
        compute_context = compute_context_1;
    else if (inputs.gb_template == 2)
        compute_context = compute_context_2;
    else {
        VERIFY(inputs.gb_template == 3);
        compute_context = compute_context_3;
    }

    // "The values of the pixels in this neighbourhood define a context. Each context has its own adaptive probability estimate
    //  used by the arithmetic coder (see Annex E)."
    // "* Decode the current pixel by invoking the arithmetic entropy decoding procedure, with CX set to the value formed by
    //    concatenating the label "GB" and the 10-16 pixel values gathered in CONTEXT."
    // NOTE: What this is supposed to mean is that we have a bunch of independent contexts, and we pick the
    // context for the current pixel based on pixel values in the neighborhood. The "GB" part just means this context is
    // independent from other contexts in the spec. They are passed in to this function.

    // Figure 8 – Reused context for coding the SLTP value when GBTEMPLATE is 0
    constexpr u16 sltp_context_for_template_0 = 0b10011'0110010'0101;

    // Figure 9 – Reused context for coding the SLTP value when GBTEMPLATE is 1
    constexpr u16 sltp_context_for_template_1 = 0b0011'110010'101;

    // Figure 10 – Reused context for coding the SLTP value when GBTEMPLATE is 2
    constexpr u16 sltp_context_for_template_2 = 0b001'11001'01;

    // Figure 11 – Reused context for coding the SLTP value when GBTEMPLATE is 3
    constexpr u16 sltp_context_for_template_3 = 0b011001'0101;

    u16 sltp_context = [](u8 gb_template) {
        if (gb_template == 0)
            return sltp_context_for_template_0;
        if (gb_template == 1)
            return sltp_context_for_template_1;
        if (gb_template == 2)
            return sltp_context_for_template_2;
        VERIFY(gb_template == 3);
        return sltp_context_for_template_3;
    }(inputs.gb_template);

    // 6.2.5.7 Decoding the bitmap
    MQArithmeticEncoder& encoder = *inputs.arithmetic_encoder;

    // "1) Set:
    //         LTP = 0"
    bool ltp = false; // "Line (uses) Typical Prediction" maybe?

    // " 2) Create a bitmap GBREG of width GBW and height GBH pixels."
    // auto result = TRY(BilevelImage::create(inputs.region_width, inputs.region_height));

    // "3) Decode each row as follows:"
    for (size_t y = 0; y < height; ++y) {
        // "a) If all GBH rows have been decoded then the decoding is complete; proceed to step 4)."
        // "b) If TPGDON is 1, then decode a bit using the arithmetic entropy coder..."
        if (inputs.is_typical_prediction_used) {
            // "i) If the current row of GBREG is identical to the row immediately above, then SLTP = 1; otherwise SLTP = 0."
            // FIXME: If skip_pattern is set, we should probably ignore skipped pixels here.
            bool is_line_identical_to_previous_line = true;
            for (size_t x = 0; x < width; ++x) {
                if (inputs.image.get_bit(x, y) != get_pixel(inputs.image, (int)x, (int)y - 1)) {
                    is_line_identical_to_previous_line = false;
                    break;
                }
            }

            // "SLTP" in spec. "Swap LTP" or "Switch LTP" maybe?
            bool sltp = ltp ^ is_line_identical_to_previous_line;
            encoder.encode_bit(sltp, contexts.contexts[sltp_context]);
            ltp = is_line_identical_to_previous_line;
            if (ltp)
                continue;
        }

        // "d) If LTP = 0 then, from left to right, decode each pixel of the current row of GBREG. The procedure for each
        //     pixel is as follows:"
        for (size_t x = 0; x < width; ++x) {
            // "i) If USESKIP is 1 and the pixel in the bitmap SKIP at the location corresponding to the current pixel is 1,
            //     then set the current pixel to 0."
            if (inputs.skip_pattern.has_value() && inputs.skip_pattern->get_bit(x, y))
                continue;

            // "ii) Otherwise:"
            u16 context = compute_context(inputs.image, inputs.adaptive_template_pixels, x, y);
            encoder.encode_bit(inputs.image.get_bit(x, y), contexts.contexts[context]);
        }
    }

    // "4) After all the rows have been decoded, the current contents of the bitmap GBREG are the results that shall be
    //     obtained by every decoder, whether it performs this exact sequence of steps or not."
    // In the encoding case, this means the compressed data is complete.
    return {};
}

namespace {

// Similar to 6.3.2 Input parameters, but with an input image.
struct GenericRefinementRegionEncodingInputParameters {
    BilevelImage const& image;                                          // Of dimensions "GRW" x "GRH" in spec terms.
    u8 gr_template { 0 };                                               // "GRTEMPLATE" in spec.
    BilevelSubImage reference_bitmap;                                   // "GRREFERENCE" in spec.
    i32 reference_x_offset { 0 };                                       // "GRREFERENCEDX" in spec.
    i32 reference_y_offset { 0 };                                       // "GRREFERENCEDY" in spec.
    bool is_typical_prediction_used { false };                          // "TPGRON" in spec.
    Array<JBIG2::AdaptiveTemplatePixel, 2> adaptive_template_pixels {}; // "GRATX" / "GRATY" in spec.
};

struct RefinementContexts {
    explicit RefinementContexts(u8 refinement_template)
    {
        contexts.resize(1 << (refinement_template == 0 ? 13 : 10));
    }

    Vector<MQArithmeticCoderContext> contexts; // "GR" (+ binary suffix) in spec.
};

}

// 6.3 Generic Refinement Region Decoding Procedure, but in reverse.
static ErrorOr<void> generic_refinement_region_encoding_procedure(GenericRefinementRegionEncodingInputParameters& inputs, MQArithmeticEncoder& encoder, RefinementContexts& contexts)
{
    // FIXME: Try to come up with a way to share more code with generic_refinement_region_decoding_procedure().
    auto width = inputs.image.width();
    auto height = inputs.image.height();

    VERIFY(inputs.gr_template == 0 || inputs.gr_template == 1);

    if (inputs.gr_template == 0) {
        TRY(check_valid_adaptive_template_pixel(inputs.adaptive_template_pixels[0]));
        // inputs.adaptive_template_pixels[1] is allowed to contain any value.
    }
    // GRTEMPLATE 1 never uses adaptive pixels.

    // 6.3.5.3 Fixed templates and adaptive templates
    static constexpr auto get_pixel = [](auto const& buffer, int x, int y) -> bool {
        if (x < 0 || x >= (int)buffer.width() || y < 0 || y >= (int)buffer.height())
            return false;
        return buffer.get_bit(x, y);
    };

    // Figure 12 – 13-pixel refinement template showing the AT pixels at their nominal locations
    constexpr auto compute_context_0 = [](ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, BilevelSubImage const& reference, int reference_x, int reference_y, BilevelImage const& buffer, int x, int y) -> u16 {
        u16 result = 0;

        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                if (dy == -1 && dx == -1)
                    result = (result << 1) | (u16)get_pixel(reference, reference_x + adaptive_pixels[1].x, reference_y + adaptive_pixels[1].y);
                else
                    result = (result << 1) | (u16)get_pixel(reference, reference_x + dx, reference_y + dy);
            }
        }

        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        for (int i = 0; i < 2; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x + i, y - 1);
        result = (result << 1) | (u16)get_pixel(buffer, x - 1, y);

        return result;
    };

    // Figure 13 – 10-pixel refinement template
    constexpr auto compute_context_1 = [](ReadonlySpan<JBIG2::AdaptiveTemplatePixel>, BilevelSubImage const& reference, int reference_x, int reference_y, BilevelImage const& buffer, int x, int y) -> u16 {
        u16 result = 0;

        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                if ((dy == -1 && (dx == -1 || dx == 1)) || (dy == 1 && dx == -1))
                    continue;
                result = (result << 1) | (u16)get_pixel(reference, reference_x + dx, reference_y + dy);
            }
        }

        for (int i = 0; i < 3; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 1 + i, y - 1);
        result = (result << 1) | (u16)get_pixel(buffer, x - 1, y);

        return result;
    };

    auto compute_context = inputs.gr_template == 0 ? compute_context_0 : compute_context_1;

    // Figure 14 – Reused context for coding the SLTP value when GRTEMPLATE is 0
    constexpr u16 sltp_context_for_template_0 = 0b000'010'000'000'0;

    // Figure 15 – Reused context for coding the SLTP value when GRTEMPLATE is 1
    constexpr u16 sltp_context_for_template_1 = 0b0'010'00'000'0;

    u16 const sltp_context = inputs.gr_template == 0 ? sltp_context_for_template_0 : sltp_context_for_template_1;

    // 6.3.5.6 Decoding the refinement bitmap

    // "1) Set LTP = 0."
    bool ltp = false; // "Line (uses) Typical Prediction" maybe?

    // "2) Create a bitmap GRREG of width GRW and height GRH pixels."
    // auto result = TRY(BilevelImage::create(inputs.region_width, inputs.region_height));

    // "3) Decode each row as follows:"
    for (size_t y = 0; y < height; ++y) {
        auto predict = [&](size_t x, size_t y) -> Optional<bool> {
            // "• a 3 × 3 pixel array in the reference bitmap (Figure 16), centred at the location
            //    corresponding to the current pixel, contains pixels all of the same value."
            bool prediction = get_pixel(inputs.reference_bitmap, x - inputs.reference_x_offset - 1, y - inputs.reference_y_offset - 1);
            for (int dy = -1; dy <= 1; ++dy)
                for (int dx = -1; dx <= 1; ++dx)
                    if (get_pixel(inputs.reference_bitmap, x - inputs.reference_x_offset + dx, y - inputs.reference_y_offset + dy) != prediction)
                        return {};
            return prediction;
        };

        // "a) If all GRH rows have been decoded, then the decoding is complete; proceed to step 4)."
        // "b) If TPGRON is 1, then decode a bit using the arithmetic entropy coder..."
        if (inputs.is_typical_prediction_used) {
            // "SLTP" in spec. "Swap LTP" or "Switch LTP" maybe?
            bool line_can_be_predicted = true;
            for (size_t x = 0; x < width; ++x) {
                // "TPGRPIX", "TPGRVAL" in spec.
                auto prediction = predict(x, y);
                if (prediction.has_value() && inputs.image.get_bit(x, y) != prediction.value()) {
                    line_can_be_predicted = false;
                    break;
                }
            }

            bool sltp = ltp ^ line_can_be_predicted;
            encoder.encode_bit(sltp, contexts.contexts[sltp_context]);
            ltp = ltp ^ sltp;
        }

        if (!ltp) {
            // "c) If LTP = 0 then, from left to right, explicitly decode all pixels of the current row of GRREG. The
            //     procedure for each pixel is as follows:"
            for (size_t x = 0; x < width; ++x) {
                u16 context = compute_context(inputs.adaptive_template_pixels, inputs.reference_bitmap, x - inputs.reference_x_offset, y - inputs.reference_y_offset, inputs.image, x, y);
                encoder.encode_bit(inputs.image.get_bit(x, y), contexts.contexts[context]);
            }
        } else {
            // "d) If LTP = 1 then, from left to right, implicitly decode certain pixels of the current row of GRREG,
            //     and explicitly decode the rest. The procedure for each pixel is as follows:"
            for (size_t x = 0; x < width; ++x) {
                // "TPGRPIX", "TPGRVAL" in spec.
                auto prediction = predict(x, y);

                // TPGRON must be 1 if LTP is set. (The spec has an explicit "TPGRON is 1 AND" check here, but it is pointless.)
                VERIFY(inputs.is_typical_prediction_used);
                if (!prediction.has_value()) {
                    u16 context = compute_context(inputs.adaptive_template_pixels, inputs.reference_bitmap, x - inputs.reference_x_offset, y - inputs.reference_y_offset, inputs.image, x, y);
                    encoder.encode_bit(inputs.image.get_bit(x, y), contexts.contexts[context]);
                }
            }
        }
    }

    return {};
}

static ErrorOr<NonnullRefPtr<BilevelImage>> symbol_image(JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol const& symbol)
{
    if (symbol.image.has<NonnullRefPtr<BilevelImage>>())
        return symbol.image.get<NonnullRefPtr<BilevelImage>>();
    if (symbol.image.has<JBIG2::SymbolDictionarySegmentData::HeightClass::RefinedSymbol>())
        return symbol.image.get<JBIG2::SymbolDictionarySegmentData::HeightClass::RefinedSymbol>().refines_to;

    auto const& text_strips = symbol.image.get<JBIG2::SymbolDictionarySegmentData::HeightClass::RefinesUsingStrips>();
    (void)text_strips;
    return Error::from_string_literal("JBIG2Writer: Cannot write refinements of refinements by text strips yet");
}

namespace {

// 6.4.2 Input parameters
// Table 9 – Parameters for the text region decoding procedure
struct TextRegionEncodingInputParameters {
    bool uses_huffman_encoding { false };  // "SBHUFF" in spec.
    bool uses_refinement_coding { false }; // "SBREFINE" in spec.

    u32 size_of_symbol_instance_strips { 0 }; // "SBSTRIPS" in spec.
    // "SBNUMSYMS" is `symbols.size()` below.

    i32 initial_strip_t { 0 };
    Vector<JBIG2::TextRegionStrip> const& symbol_instance_strips;

    // Only set if uses_huffman_encoding is true.
    JBIG2::HuffmanTable const* symbol_id_table { nullptr }; // "SBSYMCODES" in spec.

    u32 id_symbol_code_length { 0 };                                            // "SBSYMCODELEN" in spec.
    Vector<JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol> symbols {}; // "SBNUMSYMS" / "SBSYMS" in spec.

    bool is_transposed { false }; // "TRANSPOSED" in spec.

    JBIG2::ReferenceCorner reference_corner { JBIG2::ReferenceCorner::TopLeft }; // "REFCORNER" in spec.

    i8 delta_s_offset { 0 }; // "SBDSOFFSET" in spec.

    // Only set if uses_huffman_encoding is true.
    JBIG2::HuffmanTable const* first_s_table { nullptr };                 // "SBHUFFFS" in spec.
    JBIG2::HuffmanTable const* subsequent_s_table { nullptr };            // "SBHUFFDS" in spec.
    JBIG2::HuffmanTable const* delta_t_table { nullptr };                 // "SBHUFFDT" in spec.
    JBIG2::HuffmanTable const* refinement_delta_width_table { nullptr };  // "SBHUFFRDW" in spec.
    JBIG2::HuffmanTable const* refinement_delta_height_table { nullptr }; // "SBHUFFRDH" in spec.
    JBIG2::HuffmanTable const* refinement_x_offset_table { nullptr };     // "SBHUFFRDX" in spec.
    JBIG2::HuffmanTable const* refinement_y_offset_table { nullptr };     // "SBHUFFRDY" in spec.
    JBIG2::HuffmanTable const* refinement_size_table { nullptr };         // "SBHUFFRSIZE" in spec.

    u8 refinement_template { 0 };                                                  // "SBRTEMPLATE" in spec.
    Array<JBIG2::AdaptiveTemplatePixel, 2> refinement_adaptive_template_pixels {}; // "SBRATX" / "SBRATY" in spec.
    // FIXME: COLEXTFLAG, SBCOLS

    // If uses_huffman_encoding is true, text_region_encoding_procedure() writes data to this stream.
    BigEndianOutputBitStream* bit_stream { nullptr };

    // If uses_huffman_encoding is false, text_region_encoding_procedure() writes data to this encoder.
    MQArithmeticEncoder* arithmetic_encoder { nullptr };
};

struct TextContexts {
    explicit TextContexts(u32 id_symbol_code_length)
        : id_encoder(id_symbol_code_length)
    {
    }

    JBIG2::ArithmeticIntegerEncoder delta_t_integer_encoder;         // "IADT" in spec.
    JBIG2::ArithmeticIntegerEncoder first_s_integer_encoder;         // "IAFS" in spec.
    JBIG2::ArithmeticIntegerEncoder subsequent_s_integer_encoder;    // "IADS" in spec.
    JBIG2::ArithmeticIntegerEncoder instance_t_integer_encoder;      // "IAIT" in spec.
    JBIG2::ArithmeticIntegerIDEncoder id_encoder;                    // "IAID" in spec.
    JBIG2::ArithmeticIntegerEncoder refinement_delta_width_encoder;  // "IARDW" in spec.
    JBIG2::ArithmeticIntegerEncoder refinement_delta_height_encoder; // "IARDH" in spec.
    JBIG2::ArithmeticIntegerEncoder refinement_x_offset_encoder;     // "IARDX" in spec.
    JBIG2::ArithmeticIntegerEncoder refinement_y_offset_encoder;     // "IARDY" in spec.
    JBIG2::ArithmeticIntegerEncoder has_refinement_image_encoder;    // "IARI" in spec.
};

}

// 6.4 Text Region Decoding Procedure, but in reverse.
static ErrorOr<void> text_region_encoding_procedure(TextRegionEncodingInputParameters const& inputs, Optional<TextContexts>& text_contexts, Optional<RefinementContexts>& refinement_contexts)
{
    BigEndianOutputBitStream* bit_stream = nullptr;
    MQArithmeticEncoder* encoder = nullptr;
    if (inputs.uses_huffman_encoding)
        bit_stream = inputs.bit_stream;
    else
        encoder = inputs.arithmetic_encoder;

    // "In order to improve compression, symbol instances are grouped into strips according to their TI values. This is done
    //  according to the value of SBSTRIPS. Symbol instances having TI values between 0 and SBSTRIPS – 1 are grouped
    //  into one strip, symbol instances having TI values between SBSTRIPS and 2 × SBSTRIPS – 1 into the next, and so on.
    //  Within each strip, the symbol instances are coded in the order of increasing S coordinate."

    // 6.4.6 Strip delta T
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFDT and multiply the resulting value by SBSTRIPS.
    //  If SBHUFF is 0, decode a value using the IADT integer arithmetic decoding procedure (see Annex A) and multiply the resulting value by SBSTRIPS."
    auto write_delta_t = [&](i32 value) -> ErrorOr<void> {
        if (value % inputs.size_of_symbol_instance_strips != 0)
            return Error::from_string_literal("JBIG2Writer: delta t not divisible by size_of_symbol_instance_strips");
        i32 quantized = value / static_cast<i32>(inputs.size_of_symbol_instance_strips);
        if (inputs.uses_huffman_encoding)
            return inputs.delta_t_table->write_symbol_non_oob(*bit_stream, quantized);
        return text_contexts->delta_t_integer_encoder.encode_non_oob(*encoder, quantized);
    };

    // 6.4.7 First symbol instance S coordinate
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFFS.
    //  If SBHUFF is 0, decode a value using the IAFS integer arithmetic decoding procedure (see Annex A)."
    auto write_first_s = [&](i32 value) -> ErrorOr<void> {
        if (inputs.uses_huffman_encoding)
            return inputs.first_s_table->write_symbol_non_oob(*bit_stream, value);
        return text_contexts->first_s_integer_encoder.encode_non_oob(*encoder, value);
    };

    // 6.4.8 Subsequent symbol instance S coordinate
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFDS.
    //  If SBHUFF is 0, decode a value using the IADS integer arithmetic decoding procedure (see Annex A).
    //  In either case it is possible that the result of this decoding is the out-of-band value OOB."
    auto write_subsequent_s = [&](Optional<i32> value) -> ErrorOr<void> {
        if (inputs.uses_huffman_encoding)
            return inputs.subsequent_s_table->write_symbol(*bit_stream, value);
        return text_contexts->subsequent_s_integer_encoder.encode(*encoder, value);
    };

    // 6.4.9 Symbol instance T coordinate
    // "If SBSTRIPS == 1, then the value decoded is always zero. Otherwise:
    //  • If SBHUFF is 1, decode a value by reading ceil(log2(SBSTRIPS)) bits directly from the bitstream.
    //  • If SBHUFF is 0, decode a value using the IAIT integer arithmetic decoding procedure (see Annex A)."
    auto write_instance_t = [&](i32 value) -> ErrorOr<void> {
        // FIXME: The spec wants this check for all valid strip sizes (1, 2, 4, 8).
        if (inputs.size_of_symbol_instance_strips == 1 && value >= static_cast<i32>(inputs.size_of_symbol_instance_strips))
            return Error::from_string_literal("JBIG2Writer: Symbol instance T coordinate out of range");
        if (inputs.size_of_symbol_instance_strips == 1)
            return {};
        if (inputs.uses_huffman_encoding)
            return TRY(bit_stream->write_bits((u64)value, ceil(log2(inputs.size_of_symbol_instance_strips))));
        return text_contexts->instance_t_integer_encoder.encode_non_oob(*encoder, value);
    };

    // 6.4.10 Symbol instance symbol ID
    // "If SBHUFF is 1, decode a value by reading one bit at a time until the resulting bit string is equal to one of the entries in
    //  SBSYMCODES. The resulting value, which is IDI, is the index of the entry in SBSYMCODES that is read.
    //  If SBHUFF is 0, decode a value using the IAID integer arithmetic decoding procedure (see Annex A). Set IDI to the
    //  resulting value."
    auto write_symbol_id = [&](u32 value) -> ErrorOr<void> {
        if (inputs.uses_huffman_encoding)
            return inputs.symbol_id_table->write_symbol_non_oob(*bit_stream, value);
        return text_contexts->id_encoder.encode(*encoder, value);
    };

    // 6.4.11.1 Symbol instance refinement delta width
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFRDW.
    //  If SBHUFF is 0, decode a value using the IARDW integer arithmetic decoding procedure (see Annex A)."
    auto write_refinement_delta_width = [&](i32 value) -> ErrorOr<void> {
        if (inputs.uses_huffman_encoding)
            return inputs.refinement_delta_width_table->write_symbol_non_oob(*bit_stream, value);
        return text_contexts->refinement_delta_width_encoder.encode(*encoder, value);
    };

    // 6.4.11.2 Symbol instance refinement delta height
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFRDH.
    //  If SBHUFF is 0, decode a value using the IARDH integer arithmetic decoding procedure (see Annex A)."
    auto write_refinement_delta_height = [&](i32 value) -> ErrorOr<void> {
        if (inputs.uses_huffman_encoding)
            return inputs.refinement_delta_height_table->write_symbol_non_oob(*bit_stream, value);
        return text_contexts->refinement_delta_height_encoder.encode(*encoder, value);
    };

    // 6.4.11.3 Symbol instance refinement X offset
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFRDX.
    //  If SBHUFF is 0, decode a value using the IARDX integer arithmetic decoding procedure (see Annex A)."
    auto write_refinement_x_offset = [&](i32 value) -> ErrorOr<void> {
        if (inputs.uses_huffman_encoding)
            return inputs.refinement_x_offset_table->write_symbol_non_oob(*bit_stream, value);
        return text_contexts->refinement_x_offset_encoder.encode(*encoder, value);
    };

    // 6.4.11.4 Symbol instance refinement Y offset
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFRDY.
    //  If SBHUFF is 0, decode a value using the IARDY integer arithmetic decoding procedure (see Annex A)."
    auto write_refinement_y_offset = [&](i32 value) -> ErrorOr<void> {
        if (inputs.uses_huffman_encoding)
            return inputs.refinement_y_offset_table->write_symbol_non_oob(*bit_stream, value);
        return text_contexts->refinement_y_offset_encoder.encode(*encoder, value);
    };

    // 6.4.11 Symbol instance bitmap
    auto write_bitmap = [&](JBIG2::TextRegionStrip::SymbolInstance const& symbol_instance) -> ErrorOr<IntSize> {
        bool has_refinement_image = symbol_instance.refinement_data.has_value(); // "R_I" in spec.
        if (has_refinement_image && !inputs.uses_refinement_coding)
            return Error::from_string_literal("JBIG2Writer: Text region symbol instance has refinement data, but refinement coding is disabled");

        if (inputs.uses_refinement_coding) {
            has_refinement_image = symbol_instance.refinement_data.has_value();
            // "• If SBHUFF is 1, then read one bit and set RI to the value of that bit.
            //  • If SBHUFF is 0, then decode one bit using the IARI integer arithmetic decoding procedure and set RI to the value of that bit."
            if (inputs.uses_huffman_encoding)
                TRY(bit_stream->write_bits(has_refinement_image ? 1u : 0u, 1u));
            else
                TRY(text_contexts->has_refinement_image_encoder.encode_non_oob(*encoder, has_refinement_image ? 1 : 0));
        }

        if (symbol_instance.symbol_id >= inputs.symbols.size())
            return Error::from_string_literal("JBIG2Writer: Text region symbol ID out of range");
        auto const& symbol = inputs.symbols[symbol_instance.symbol_id];

        // "If RI is 0 then set the symbol instance bitmap IBI to SBSYMS[IDI]."
        if (!has_refinement_image)
            return symbol.size;

        TRY(write_refinement_delta_width(symbol_instance.refinement_data->delta_width));
        TRY(write_refinement_delta_height(symbol_instance.refinement_data->delta_height));
        TRY(write_refinement_x_offset(symbol_instance.refinement_data->x_offset));
        TRY(write_refinement_y_offset(symbol_instance.refinement_data->y_offset));

        MQArithmeticEncoder* refinement_encoder = encoder;
        Optional<MQArithmeticEncoder> huffman_refinement_encoder;
        if (inputs.uses_huffman_encoding) {
            huffman_refinement_encoder = TRY(MQArithmeticEncoder::initialize(0));
            refinement_encoder = &huffman_refinement_encoder.value();
        }

        // Table 12 – Parameters used to decode a symbol instance's bitmap using refinement
        if (static_cast<i32>(symbol.size.width()) + symbol_instance.refinement_data->delta_width < 0)
            return Error::from_string_literal("JBIG2Writer: Refinement width out of bounds");
        if (static_cast<i32>(symbol.size.height()) + symbol_instance.refinement_data->delta_height < 0)
            return Error::from_string_literal("JBIG2Writer: Refinement height out of bounds");

        auto reference_bitmap = TRY(symbol_image(symbol));
        GenericRefinementRegionEncodingInputParameters refinement_inputs {
            .image = symbol_instance.refinement_data->refines_to,
            .reference_bitmap = reference_bitmap->as_subbitmap(),
        };

        // FIXME: Instead, just compute the delta here instead of having it be passed in?
        if (static_cast<i32>(reference_bitmap->width()) + symbol_instance.refinement_data->delta_width != static_cast<i32>(refinement_inputs.image.width()))
            return Error::from_string_literal("JBIG2Writer: Refinement reference width mismatch");
        if (static_cast<i32>(reference_bitmap->height()) + symbol_instance.refinement_data->delta_height != static_cast<i32>(refinement_inputs.image.height()))
            return Error::from_string_literal("JBIG2Writer: Refinement reference height mismatch");

        refinement_inputs.gr_template = inputs.refinement_template;
        refinement_inputs.reference_x_offset = floor_div(symbol_instance.refinement_data->delta_width, 2) + symbol_instance.refinement_data->x_offset;
        refinement_inputs.reference_y_offset = floor_div(symbol_instance.refinement_data->delta_height, 2) + symbol_instance.refinement_data->y_offset;
        refinement_inputs.is_typical_prediction_used = false;
        refinement_inputs.adaptive_template_pixels = inputs.refinement_adaptive_template_pixels;
        TRY(generic_refinement_region_encoding_procedure(refinement_inputs, *refinement_encoder, refinement_contexts.value()));

        if (inputs.uses_huffman_encoding) {
            auto data = TRY(huffman_refinement_encoder->finalize(symbol_instance.refinement_data->trailing_7fff_handling));
            TRY(inputs.refinement_size_table->write_symbol_non_oob(*bit_stream, data.size()));
            TRY(bit_stream->align_to_byte_boundary());
            TRY(bit_stream->write_until_depleted(data));
        }

        return IntSize { refinement_inputs.image.width(), refinement_inputs.image.height() };
    };

    // 6.4.5 Decoding the text region

    // "1) Fill a bitmap SBREG, of the size given by SBW and SBH, with the SBDEFPIXEL value."

    // "2) Decode the initial STRIPT value as described in 6.4.6. Negate the decoded value and assign this negated value to the variable STRIPT.
    //     Assign the value 0 to FIRSTS. Assign the value 0 to NINSTANCES."
    // NINSTANCES is not needed in the encoder.
    i32 strip_t = inputs.initial_strip_t;
    TRY(write_delta_t(-strip_t));
    i32 first_s = 0;

    // "3) If COLEXTFLAG is 1, decode the colour section as described in 6.4.12."
    // FIXME: Implement support for colors one day.

    // "4) Decode each strip as follows:
    //      a) If NINSTANCES is equal to SBNUMINSTANCES then there are no more strips to decode,
    //         and the process of decoding the text region is complete; proceed to step 4)."
    // NOTE: The spec means "proceed to step 5)" at the end of 4a).
    for (auto const& strip : inputs.symbol_instance_strips) {
        for (size_t i = 1; i < strip.symbol_instances.size(); ++i) {
            if (strip.symbol_instances[i].s < strip.symbol_instances[i - 1].s)
                return Error::from_string_literal("JBIG2Writer: Symbol instances in strip not sorted by S coordinate");
        }

        // "b) Decode the strip's delta T value as described in 6.4.6. Let DT be the decoded value. Set:
        //         STRIPT = STRIPT + DT"
        i32 delta_t = strip.strip_t - strip_t;
        TRY(write_delta_t(delta_t));
        strip_t += delta_t;

        i32 cur_s;
        bool is_first_symbol = true;
        for (auto const& symbol_instance : strip.symbol_instances) {
            // "c) Decode each symbol instance in the strip as follows:
            //      i) If the current symbol instance is the first symbol instance in the strip, then decode the first
            //         symbol instance's S coordinate as described in 6.4.7. Let DFS be the decoded value. Set:
            //              FIRSTS = FIRSTS + DFS
            //              CURS = FIRSTS
            //      ii) Otherwise, if the current symbol instance is not the first symbol instance in the strip, decode
            //          the symbol instance's S coordinate as described in 6.4.8. If the result of this decoding is OOB
            //          then the last symbol instance of the strip has been decoded; proceed to step 3 d). Otherwise, let
            //          IDS be the decoded value. Set:
            //              CURS = CURS + IDS + SBDSOFFSET"
            // NOTE: The spec means "proceed to step 4 d)" in 4c ii).
            if (is_first_symbol) {
                i32 delta_first_s = symbol_instance.s - first_s;
                TRY(write_first_s(delta_first_s));
                first_s += delta_first_s;
                cur_s = first_s;
                is_first_symbol = false;
            } else {
                i32 instance_delta_s = symbol_instance.s - cur_s - inputs.delta_s_offset;
                TRY(write_subsequent_s(instance_delta_s));
                cur_s += instance_delta_s + inputs.delta_s_offset;
            }

            //     "iii) Decode the symbol instance's T coordinate as described in 6.4.9. Let CURT be the decoded value. Set:
            //              TI = STRIPT + CURT"
            i32 cur_t = symbol_instance.t - strip_t;
            TRY(write_instance_t(cur_t));
            i32 t_instance = strip_t + cur_t;

            //     "iv) Decode the symbol instance's symbol ID as described in 6.4.10. Let IDI be the decoded value."
            u32 id = symbol_instance.symbol_id;
            TRY(write_symbol_id(id));

            //     "v) Determine the symbol instance's bitmap IBI as described in 6.4.11. The width and height of this
            //         bitmap shall be denoted as WI and HI respectively."
            auto symbol_size = TRY(write_bitmap(symbol_instance));

            //     "vi) Update CURS as follows:
            //      • If TRANSPOSED is 0, and REFCORNER is TOPRIGHT or BOTTOMRIGHT, set:
            //              CURS = CURS + WI – 1
            //      • If TRANSPOSED is 1, and REFCORNER is BOTTOMLEFT or BOTTOMRIGHT, set:
            //              CURS = CURS + HI – 1
            //      • Otherwise, do not change CURS in this step."
            using enum JBIG2::ReferenceCorner;
            if (!inputs.is_transposed && (inputs.reference_corner == TopRight || inputs.reference_corner == BottomRight))
                cur_s += symbol_size.width() - 1;
            if (inputs.is_transposed && (inputs.reference_corner == BottomLeft || inputs.reference_corner == BottomRight))
                cur_s += symbol_size.height() - 1;

            //     "vii) Set:
            //              SI = CURS"
            auto s_instance = cur_s;

            //     "viii) Determine the location of the symbol instance bitmap with respect to SBREG as follows:
            //          • If TRANSPOSED is 0, then:
            //              – If REFCORNER is TOPLEFT then the top left pixel of the symbol instance bitmap
            //                IBI shall be placed at SBREG[SI, TI].
            //              – If REFCORNER is TOPRIGHT then the top right pixel of the symbol instance
            //                bitmap IBI shall be placed at SBREG[SI, TI].
            //              – If REFCORNER is BOTTOMLEFT then the bottom left pixel of the symbol
            //                instance bitmap IBI shall be placed at SBREG[SI, TI].
            //              – If REFCORNER is BOTTOMRIGHT then the bottom right pixel of the symbol
            //                instance bitmap IBI shall be placed at SBREG[SI, TI].
            //          • If TRANSPOSED is 1, then:
            //              – If REFCORNER is TOPLEFT then the top left pixel of the symbol instance bitmap
            //                IBI shall be placed at SBREG[TI, SI].
            //              – If REFCORNER is TOPRIGHT then the top right pixel of the symbol instance
            //                bitmap IBI shall be placed at SBREG[TI, SI].
            //              – If REFCORNER is BOTTOMLEFT then the bottom left pixel of the symbol
            //                instance bitmap IBI shall be placed at SBREG[TI, SI].
            //              – If REFCORNER is BOTTOMRIGHT then the bottom right pixel of the symbol
            //                instance bitmap IBI shall be placed at SBREG[TI, SI].
            //          If any part of IBI, when placed at this location, lies outside the bounds of SBREG, then ignore
            //          this part of IBI in step 3 c) ix)."
            // NOTE: The spec means "ignore this part of IBI in step 3 c) x)" in 3c viii)'s last sentence.
            if (inputs.is_transposed)
                swap(s_instance, t_instance);
            if (inputs.reference_corner == TopRight || inputs.reference_corner == BottomRight)
                s_instance -= symbol_size.width() - 1;
            if (inputs.reference_corner == BottomLeft || inputs.reference_corner == BottomRight)
                t_instance -= symbol_size.height() - 1;

            //     "ix) If COLEXTFLAG is 1, set the colour specified by SBCOLS[SBFGCOLID[NINSTANCES]]
            //          to the foreground colour of the symbol instance bitmap IBI."
            // FIXME: Implement support for colors one day.

            //     "x) Draw IBI into SBREG. Combine each pixel of IBI with the current value of the corresponding
            //         pixel in SBREG, using the combination operator specified by SBCOMBOP. Write the results
            //         of each combination into that pixel in SBREG."

            //     "xi) Update CURS as follows:
            //          • If TRANSPOSED is 0, and REFCORNER is TOPLEFT or BOTTOMLEFT, set:
            //              CURS = CURS + WI – 1
            //          • If TRANSPOSED is 1, and REFCORNER is TOPLEFT or TOPRIGHT, set:
            //              CURS = CURS + HI – 1
            //          • Otherwise, do not change CURS in this step."
            if (!inputs.is_transposed && (inputs.reference_corner == TopLeft || inputs.reference_corner == BottomLeft))
                cur_s += symbol_size.width() - 1;
            if (inputs.is_transposed && (inputs.reference_corner == TopLeft || inputs.reference_corner == TopRight))
                cur_s += symbol_size.height() - 1;

            //      "xii) Set:
            //              NINSTANCES = NINSTANCES + 1"
            // Not needed in the encoder.
        }
        //  "d) When the strip has been completely decoded, decode the next strip."
        TRY(write_subsequent_s(OptionalNone {}));
    }

    //  "5) After all the strips have been decoded, the current contents of SBREG are the results that shall be
    //      obtained by every decoder, whether it performs this exact sequence of steps or not."

    return {};
}

namespace {

// 6.5.2 Input parameters
// Table 13 – Parameters for the symbol dictionary decoding procedure
struct SymbolDictionaryEncodingInputParameters {
    bool uses_huffman_encoding { false };               // "SDHUFF" in spec.
    bool uses_refinement_or_aggregate_coding { false }; // "SDREFAGG" in spec.

    Vector<JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol> input_symbols; // "SDNUMINSYMS", "SDINSYMS" in spec.
    Vector<bool> export_flags_for_referred_to_symbols;

    Vector<JBIG2::SymbolDictionarySegmentData::HeightClass> height_classes;

    u32 number_of_new_symbols { 0 }; // "SDNUMNEWSYMS" in spec.

    // Only set if uses_huffman_encoding is true.
    JBIG2::HuffmanTable const* delta_height_table { nullptr };               // "SDHUFFDH" in spec.
    JBIG2::HuffmanTable const* delta_width_table { nullptr };                // "SDHUFFDW" in spec.
    JBIG2::HuffmanTable const* bitmap_size_table { nullptr };                // "SDHUFFBMSIZE" in spec.
    JBIG2::HuffmanTable const* number_of_symbol_instances_table { nullptr }; // "SDHUFFAGGINST" in spec.

    u8 symbol_template { 0 };                                           // "SDTEMPLATE" in spec.
    Array<JBIG2::AdaptiveTemplatePixel, 4> adaptive_template_pixels {}; // "SDATX" / "SDATY" in spec.

    u8 refinement_template { 0 };                                                  // "SDRTEMPLATE" in spec;
    Array<JBIG2::AdaptiveTemplatePixel, 2> refinement_adaptive_template_pixels {}; // "SDRATX" / "SDRATY" in spec.

    MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { MQArithmeticEncoder::Trailing7FFFHandling::Keep };
};

struct SymbolContexts {
    JBIG2::ArithmeticIntegerEncoder delta_height_integer_encoder;       // "IADH" in spec.
    JBIG2::ArithmeticIntegerEncoder delta_width_integer_encoder;        // "IADW" in spec.
    JBIG2::ArithmeticIntegerEncoder number_of_symbol_instances_encoder; // "IAAI" in spec.
    JBIG2::ArithmeticIntegerEncoder export_integer_encoder;             // "IAEX" in spec.
};

}

// 6.5 Symbol Dictionary Decoding Procedure, but in reverse.
static ErrorOr<ByteBuffer> symbol_dictionary_encoding_procedure(SymbolDictionaryEncodingInputParameters const& inputs, Vector<JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol>& exported_symbols)
{
    Optional<AllocatingMemoryStream> stream;
    Optional<BigEndianOutputBitStream> bit_stream;
    Optional<MQArithmeticEncoder> encoder;
    Optional<JBIG2::GenericContexts> generic_contexts;
    Optional<SymbolContexts> symbol_contexts;
    if (inputs.uses_huffman_encoding) {
        stream = AllocatingMemoryStream {};
        bit_stream = BigEndianOutputBitStream { MaybeOwned { stream.value() } };
    } else {
        encoder = TRY(MQArithmeticEncoder::initialize(0));
        generic_contexts = JBIG2::GenericContexts { inputs.symbol_template };
        symbol_contexts = SymbolContexts {};
    }

    // 6.5.6 Height class delta height
    // "If SDHUFF is 1, decode a value using the Huffman table specified by SDHUFFDH.
    //  If SDHUFF is 0, decode a value using the IADH integer arithmetic decoding procedure (see Annex A)."
    auto write_delta_height = [&](i32 value) -> ErrorOr<void> {
        if (inputs.uses_huffman_encoding)
            return inputs.delta_height_table->write_symbol_non_oob(*bit_stream, value);
        return symbol_contexts->delta_height_integer_encoder.encode_non_oob(*encoder, value);
    };

    // 6.5.7 Delta width
    // "If SDHUFF is 1, decode a value using the Huffman table specified by SDHUFFDW.
    //  If SDHUFF is 0, decode a value using the IADW integer arithmetic decoding procedure (see Annex A).
    //  In either case it is possible that the result of this decoding is the out-of-band value OOB."
    auto write_delta_width = [&](Optional<i32> value) -> ErrorOr<void> {
        if (inputs.uses_huffman_encoding)
            return inputs.delta_width_table->write_symbol(*bit_stream, value);
        return symbol_contexts->delta_width_integer_encoder.encode(*encoder, value);
    };

    // 6.5.8 Symbol bitmap
    // "This field is only present if SDHUFF = 0 or SDREFAGG = 1. This field takes one of two forms; SDREFAGG
    //  determines which form is used."

    // 6.5.8.2.1 Number of symbol instances in aggregation
    // If SDHUFF is 1, decode a value using the Huffman table specified by SDHUFFAGGINST.
    // If SDHUFF is 0, decode a value using the IAAI integer arithmetic decoding procedure (see Annex A).
    Optional<JBIG2::ArithmeticIntegerEncoder> number_of_symbol_instances_decoder; // "IAAI" in spec.
    auto write_number_of_symbol_instances = [&](i32 value) -> ErrorOr<void> {
        if (inputs.uses_huffman_encoding)
            return inputs.number_of_symbol_instances_table->write_symbol_non_oob(*bit_stream, value);
        return symbol_contexts->number_of_symbol_instances_encoder.encode_non_oob(*encoder, value);
    };

    // 6.5.8.1 Direct-coded symbol bitmap
    Optional<TextContexts> text_contexts;
    Optional<RefinementContexts> refinement_contexts;

    // This belongs in 6.5.5 1) below, but also needs to be captured by write_symbol_bitmap here.
    Vector<JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol> new_symbols;

    // Likewise, this is from 6.5.8.2.3 below.
    Vector<JBIG2::Code> symbol_id_codes;
    Optional<JBIG2::HuffmanTable> symbol_id_table_storage;

    auto write_symbol_bitmap = [&](JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol const& symbol) -> ErrorOr<void> {
        // 6.5.8 Symbol bitmap

        // 6.5.8.1 Direct-coded symbol bitmap
        // "If SDREFAGG is 0, then decode the symbol's bitmap using a generic region decoding procedure as described in 6.2.
        //  Set the parameters to this decoding procedure as shown in Table 16."
        if (!inputs.uses_refinement_or_aggregate_coding) {
            VERIFY(!inputs.uses_huffman_encoding);

            if (!symbol.image.has<NonnullRefPtr<BilevelImage>>())
                return Error::from_string_literal("JBIG2Writer: Symbol region not using refinement or aggregation coding must only use simple images");

            // Table 16 – Parameters used to decode a symbol's bitmap using generic bitmap decoding
            GenericRegionEncodingInputParameters generic_inputs { .image = symbol.image.get<NonnullRefPtr<BilevelImage>>() };
            generic_inputs.is_modified_modified_read = false;
            generic_inputs.gb_template = inputs.symbol_template;
            generic_inputs.is_extended_reference_template_used = false; // Missing from spec in table 16.
            for (int i = 0; i < 4; ++i)
                generic_inputs.adaptive_template_pixels[i] = inputs.adaptive_template_pixels[i];
            generic_inputs.arithmetic_encoder = &encoder.value();
            return generic_region_encoding_procedure(generic_inputs, generic_contexts);
        }

        if (symbol.image.has<NonnullRefPtr<BilevelImage>>())
            return Error::from_string_literal("JBIG2Writer: Symbol region using refinement or aggregation coding must not use simple images");

        // 6.5.8.2 Refinement/aggregate-coded symbol bitmap
        // "1) Decode the number of symbol instances contained in the aggregation, as specified in 6.5.8.2.1. Let REFAGGNINST be the value decoded."
        i32 number_of_symbol_instances = 1;
        if (symbol.image.has<JBIG2::SymbolDictionarySegmentData::HeightClass::RefinesUsingStrips>()) {
            number_of_symbol_instances = 0;
            auto const& strips = symbol.image.get<JBIG2::SymbolDictionarySegmentData::HeightClass::RefinesUsingStrips>().strips;
            for (auto const& strip : strips)
                number_of_symbol_instances += strip.symbol_instances.size();

            if (number_of_symbol_instances <= 1)
                return Error::from_string_literal("JBIG2Writer: Text region strip symbol must have more than one symbol instance");
        }
        TRY(write_number_of_symbol_instances(number_of_symbol_instances));

        // 6.5.8.2.3 Setting SBSYMCODES and SBSYMCODELEN
        u32 number_of_symbols = inputs.input_symbols.size() + inputs.number_of_new_symbols; // "SBNUMSYMS" in spec.
        u32 code_length = ceil(log2(number_of_symbols));                                    // "SBSYMCODELEN" in spec.
        JBIG2::HuffmanTable const* symbol_id_table { nullptr };
        if (inputs.uses_huffman_encoding) {
            if (!symbol_id_table_storage.has_value()) {
                symbol_id_codes = TRY(JBIG2::uniform_huffman_codes(number_of_symbols, max(code_length, 1u)));
                symbol_id_table_storage = JBIG2::HuffmanTable { symbol_id_codes };
            }
            symbol_id_table = &symbol_id_table_storage.value();
        }

        if (!text_contexts.has_value())
            text_contexts = TextContexts { code_length };
        if (!refinement_contexts.has_value())
            refinement_contexts = RefinementContexts(inputs.refinement_template);

        if (number_of_symbol_instances > 1) {
            auto const& refines_using_strips = symbol.image.get<JBIG2::SymbolDictionarySegmentData::HeightClass::RefinesUsingStrips>();

            // "2) If REFAGGNINST is greater than one, then decode the bitmap itself using a text region decoding procedure
            //     as described in 6.4. Set the parameters to this decoding procedure as shown in Table 17."

            // Table 17 – Parameters used to decode a symbol's bitmap using refinement/aggregate decoding
            TextRegionEncodingInputParameters text_inputs { .symbol_instance_strips = refines_using_strips.strips };
            text_inputs.uses_huffman_encoding = inputs.uses_huffman_encoding;
            text_inputs.uses_refinement_coding = true;
            text_inputs.size_of_symbol_instance_strips = 1;
            text_inputs.initial_strip_t = refines_using_strips.initial_strip_t;
            text_inputs.symbol_id_table = symbol_id_table;
            text_inputs.id_symbol_code_length = code_length;

            // 6.5.8.2.4 Setting SBSYMS
            // "Set SBSYMS to an array of SDNUMINSYMS + NSYMSDECODED symbols, formed by concatenating the array
            //  SDINSYMS and the first NSYMSDECODED entries of the array SDNEWSYMS."
            text_inputs.symbols.extend(inputs.input_symbols);
            text_inputs.symbols.extend(new_symbols);

            text_inputs.is_transposed = false;
            text_inputs.reference_corner = JBIG2::ReferenceCorner::TopLeft;
            text_inputs.delta_s_offset = 0;
            text_inputs.first_s_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_6));
            text_inputs.subsequent_s_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_8));
            text_inputs.delta_t_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_11));
            text_inputs.refinement_delta_width_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
            text_inputs.refinement_delta_height_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
            text_inputs.refinement_x_offset_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
            text_inputs.refinement_y_offset_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
            text_inputs.refinement_size_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_1));
            text_inputs.refinement_template = inputs.refinement_template;
            text_inputs.refinement_adaptive_template_pixels = inputs.refinement_adaptive_template_pixels;

            if (inputs.uses_huffman_encoding)
                text_inputs.bit_stream = &bit_stream.value();
            else
                text_inputs.arithmetic_encoder = &encoder.value();
            return text_region_encoding_procedure(text_inputs, text_contexts, refinement_contexts);
        }

        // "3) If REFAGGNINST is equal to one, then decode the bitmap as described in 6.5.8.2.2."

        // 6.5.8.2.2 Decoding a bitmap when REFAGGNINST = 1

        auto const& refinement_image = symbol.image.get<JBIG2::SymbolDictionarySegmentData::HeightClass::RefinedSymbol>();
        if (inputs.uses_huffman_encoding)
            TRY(symbol_id_table->write_symbol_non_oob(*bit_stream, refinement_image.symbol_id));
        else
            TRY(text_contexts->id_encoder.encode(*encoder, refinement_image.symbol_id));

        if (inputs.uses_huffman_encoding)
            TRY(TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15))->write_symbol_non_oob(*bit_stream, refinement_image.delta_x_offset));
        else
            TRY(text_contexts->refinement_x_offset_encoder.encode_non_oob(*encoder, refinement_image.delta_x_offset));

        if (inputs.uses_huffman_encoding)
            TRY(TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15))->write_symbol_non_oob(*bit_stream, refinement_image.delta_y_offset));
        else
            TRY(text_contexts->refinement_y_offset_encoder.encode_non_oob(*encoder, refinement_image.delta_y_offset));

        if (refinement_image.symbol_id >= inputs.input_symbols.size() && refinement_image.symbol_id - inputs.input_symbols.size() >= new_symbols.size())
            return Error::from_string_literal("JBIG2Writer: Refinement/aggregate symbol ID out of range");

        auto const& IBO = TRY(symbol_image(refinement_image.symbol_id < inputs.input_symbols.size() ? inputs.input_symbols[refinement_image.symbol_id] : new_symbols[refinement_image.symbol_id - inputs.input_symbols.size()]));

        MQArithmeticEncoder* refinement_encoder = nullptr;
        Optional<MQArithmeticEncoder> huffman_refinement_encoder;
        if (inputs.uses_huffman_encoding) {
            huffman_refinement_encoder = TRY(MQArithmeticEncoder::initialize(0));
            refinement_encoder = &huffman_refinement_encoder.value();
        } else {
            refinement_encoder = &encoder.value();
        }

        // Table 18 – Parameters used to decode a symbol's bitmap when REFAGGNINST = 1
        GenericRefinementRegionEncodingInputParameters refinement_inputs {
            .image = *refinement_image.refines_to,
            .reference_bitmap = IBO->as_subbitmap(),
        };
        refinement_inputs.gr_template = inputs.refinement_template;
        refinement_inputs.reference_x_offset = refinement_image.delta_x_offset;
        refinement_inputs.reference_y_offset = refinement_image.delta_y_offset;
        refinement_inputs.is_typical_prediction_used = false;
        refinement_inputs.adaptive_template_pixels = inputs.refinement_adaptive_template_pixels;
        TRY(generic_refinement_region_encoding_procedure(refinement_inputs, *refinement_encoder, refinement_contexts.value()));

        if (inputs.uses_huffman_encoding) {
            auto data = TRY(huffman_refinement_encoder->finalize(refinement_image.trailing_7fff_handling));
            TRY(TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_1))->write_symbol_non_oob(*bit_stream, data.size()));
            TRY(bit_stream->align_to_byte_boundary());
            TRY(stream->write_until_depleted(data));
        }

        return {};
    };

    auto write_height_class_collective_bitmap = [&](BilevelImage const& image, bool compress) -> ErrorOr<void> {
        // 6.5.9 Height class collective bitmap
        if (!compress) {
            // "1) Read the size in bytes using the SDHUFFBMSIZE Huffman table. Let BMSIZE be the value decoded."
            TRY(inputs.bitmap_size_table->write_symbol_non_oob(*bit_stream, 0));

            // "2) Skip over any bits remaining in the last byte read."
            TRY(bit_stream->align_to_byte_boundary());

            // "3) If BMSIZE is zero, then the bitmap is stored uncompressed, and the actual size in bytes is:
            //
            //         HCHEIGHT * ceil_div(TOTWIDTH, 8)
            //
            //     Decode the bitmap by reading this many bytes and treating it as HCHEIGHT rows of TOTWIDTH pixels, each
            //     row padded out to a byte boundary with 0-7 0 bits."
            u32 padding_bits = align_up_to(image.width(), 8) - image.width();
            for (u32 y = 0; y < image.height(); ++y) {
                for (u32 x = 0; x < image.width(); ++x)
                    TRY(bit_stream->write_bits(image.get_bit(x, y), 1u));
                TRY(bit_stream->write_bits(0u, padding_bits));
            }

            // "5) Skip over any bits remaining in the last byte read."
            // Already byte-aligned here in the uncompressed case.

            return {};
        }

        // "4) Otherwise, decode the bitmap using a generic bitmap decoding procedure as described in 6.2. Set the
        //     parameters to this decoding procedure as shown in Table 19."
        // Table 19 – Parameters used to decode a height class collective bitmap
        AllocatingMemoryStream bitmap_stream;
        GenericRegionEncodingInputParameters generic_inputs { .image = image };
        generic_inputs.is_modified_modified_read = true;
        generic_inputs.stream = &bitmap_stream;
        TRY(generic_region_encoding_procedure(generic_inputs, generic_contexts));
        auto data = TRY(bitmap_stream.read_until_eof());

        // "1) Read the size in bytes using the SDHUFFBMSIZE Huffman table. Let BMSIZE be the value decoded."
        TRY(inputs.bitmap_size_table->write_symbol_non_oob(*bit_stream, data.size()));

        // "2) Skip over any bits remaining in the last byte read."
        TRY(bit_stream->align_to_byte_boundary());

        TRY(bit_stream->write_until_depleted(data));

        // "5) Skip over any bits remaining in the last byte read."
        TRY(bit_stream->align_to_byte_boundary());

        return {};
    };

    // 6.5.5 Decoding the symbol dictionary
    // "1) Create an array SDNEWSYMS of bitmaps, having SDNUMNEWSYMS entries."
    // Done above read_symbol_bitmap's definition.

    // "2) If SDHUFF is 1 and SDREFAGG is 0, create an array SDNEWSYMWIDTHS of integers, having SDNUMNEWSYMS entries."
    Vector<u32> new_symbol_widths;

    // "3) Set:
    //      HCHEIGHT = 0
    //      NSYMSDECODED = 0"
    u32 height_class_height = 0;
    u32 number_of_symbols_encoded = 0;

    // "4) Decode each height class as follows:
    //      a) If NSYMSDECODED == SDNUMNEWSYMS then all the symbols in the dictionary have been decoded; proceed to step 5)."
    for (auto const& height_class : inputs.height_classes) {
        // "b) Decode the height class delta height as described in 6.5.6. Let HCDH be the decoded value. Set:
        //      HCHEIGHT = HCEIGHT + HCDH
        //      SYMWIDTH = 0
        //      TOTWIDTH = 0
        //      HCFIRSTSYM = NSYMSDECODED"
        i32 delta_height = height_class.symbols[0].size.height() - static_cast<i32>(height_class_height);
        TRY(write_delta_height(delta_height));
        height_class_height += delta_height;
        u32 symbol_width = 0;
        u32 total_width = 0;

        // u32 height_class_first_symbol = number_of_symbols_encoded;
        // "c) Decode each symbol within the height class as follows:"
        for (auto const& symbol : height_class.symbols) {
            if (symbol.size.height() != static_cast<i32>(height_class_height))
                return Error::from_string_literal("JBIG2Writer: Symbol height does not match height class height");

            // "i) Decode the delta width for the symbol as described in 6.5.7."
            i32 delta_width = symbol.size.width() - static_cast<i32>(symbol_width);
            TRY(write_delta_width(delta_width));

            VERIFY(number_of_symbols_encoded < inputs.number_of_new_symbols);
            // "   Otherwise let DW be the decoded value and set:"
            //         SYMWIDTH = SYMWIDTH + DW
            //         TOTWIDTH = TOTWIDTH + SYMWIDTH"
            symbol_width += delta_width;
            total_width += symbol_width;

            // "ii) If SDHUFF is 0 or SDREFAGG is 1, then decode the symbol's bitmap as described in 6.5.8.
            //      Let BS be the decoded bitmap (this bitmap has width SYMWIDTH and height HCHEIGHT). Set:
            //          SDNEWSYMS[NSYMSDECODED] = BS"
            if (!inputs.uses_huffman_encoding || inputs.uses_refinement_or_aggregate_coding) {
                TRY(write_symbol_bitmap(symbol));
                new_symbols.append(symbol);
            }

            // "iii) If SDHUFF is 1 and SDREFAGG is 0, then set:
            //      SDNEWSYMWIDTHS[NSYMSDECODED] = SYMWIDTH"
            if (inputs.uses_huffman_encoding && !inputs.uses_refinement_or_aggregate_coding)
                new_symbol_widths.append(symbol_width);

            // "iv) Set:
            //      NSYMSDECODED = NSYMSDECODED + 1"
            number_of_symbols_encoded++;
        }
        TRY(write_delta_width(OptionalNone {}));

        // "d) If SDHUFF is 1 and SDREFAGG is 0, then decode the height class collective bitmap as described
        //     in 6.5.9. Let BHC be the decoded bitmap. This bitmap has width TOTWIDTH and height
        //     HCHEIGHT. Break up the bitmap BHC as follows to obtain the symbols
        //     SDNEWSYMS[HCFIRSTSYM] through SDNEWSYMS[NSYMSDECODED – 1].
        //
        //     BHC contains the NSYMSDECODED – HCFIRSTSYM symbols concatenated left-to-right, with no
        //     intervening gaps. For each I between HCFIRSTSYM and NSYMSDECODED – 1:
        //
        //     • the width of SDNEWSYMS[I] is the value of SDNEWSYMWIDTHS[I];
        //     • the height of SDNEWSYMS[I] is HCHEIGHT; and
        //     • the bitmap SDNEWSYMS[I] can be obtained by extracting the columns of BHC from:
        //
        //           sum(J=HCFIRSTSYM to I-1, SDNEWSYMWIDTHS[J]) to sum(J=HCFIRSTSYM to I-1, SDNEWSYMWIDTHS[J])^(-1)"
        // Note: I think the spec means "...to sum(J=HCFIRSTSYM to I, SDNEWSYMWIDTHS[J]) - 1" in the last sentence.
        if (inputs.uses_huffman_encoding && !inputs.uses_refinement_or_aggregate_coding) {
            auto collective_bitmap = TRY(BilevelImage::create(total_width, height_class_height));
            int current_column = 0;
            for (auto const& symbol : height_class.symbols) {
                // If we get here, we're guaranteed that the symbol does not use refinement.
                auto bitmap = symbol.image.get<NonnullRefPtr<BilevelImage>>();
                bitmap->composite_onto(collective_bitmap, { current_column, 0 }, BilevelImage::CompositionType::Replace);
                VERIFY(static_cast<int>(bitmap->width()) == symbol.size.width());
                new_symbols.append(symbol);
                current_column += symbol.size.width();
            }
            TRY(write_height_class_collective_bitmap(collective_bitmap, height_class.is_collective_bitmap_compressed));
        } else {
            if (!height_class.is_collective_bitmap_compressed)
                return Error::from_string_literal("JBIG2Writer: Height class collective bitmaps compression turned off, but not writing a height class collective bitmap when not using huffman coding, or using huffman coding with refinement");
        }
    }

    // "5) Determine which symbol bitmaps are exported from this symbol dictionary, as described in 6.5.10. These
    //     bitmaps can be drawn from the symbols that are used as input to the symbol dictionary decoding
    //     procedure as well as the new symbols produced by the decoding procedure."
    Optional<JBIG2::HuffmanTable*> export_table;
    if (inputs.uses_huffman_encoding)
        export_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_1));

    // 6.5.10 Exported symbols
    Vector<bool> export_flags_for_referred_to_symbols;
    if (inputs.export_flags_for_referred_to_symbols.is_empty()) {
        for (size_t i = 0; i < inputs.input_symbols.size(); ++i)
            export_flags_for_referred_to_symbols.append(true);
    } else {
        export_flags_for_referred_to_symbols = inputs.export_flags_for_referred_to_symbols;
    }
    if (export_flags_for_referred_to_symbols.size() != inputs.input_symbols.size())
        return Error::from_string_literal("JBIG2Writer: Mismatched size of export flags for referred-to symbols");

    Vector<bool> export_flags;
    export_flags.extend(export_flags_for_referred_to_symbols);
    for (auto const& height_class : inputs.height_classes) {
        for (auto const& symbol : height_class.symbols)
            export_flags.append(symbol.is_exported);
    }

    // "1) Set:
    //      EXINDEX = 0
    //      CUREXFLAG = 0"
    u32 exported_index = 0;
    bool current_export_flag = false;

    do {
        // "2) Decode a value using Table B.1 if SDHUFF is 1, or the IAEX integer arithmetic decoding procedure if
        //  SDHUFF is 0. Let EXRUNLENGTH be the decoded value."
        i32 export_run_length = 0;
        for (u32 i = exported_index; i < export_flags.size() && export_flags[i] == current_export_flag; ++i)
            export_run_length++;

        if (inputs.uses_huffman_encoding)
            TRY(export_table.value()->write_symbol_non_oob(*bit_stream, export_run_length));
        else
            TRY(symbol_contexts->export_integer_encoder.encode_non_oob(*encoder, export_run_length));

        // "3) Set EXFLAGS[EXINDEX] through EXFLAGS[EXINDEX + EXRUNLENGTH – 1] to CUREXFLAG.
        //  If EXRUNLENGTH = 0, then this step does not change any values."

        // "4) Set:
        //      EXINDEX = EXINDEX + EXRUNLENGTH
        //      CUREXFLAG = NOT(CUREXFLAG)"
        exported_index += export_run_length;
        current_export_flag = !current_export_flag;

        //  5) Repeat steps 2) through 4) until EXINDEX == SDNUMINSYMS + SDNUMNEWSYMS.
    } while (exported_index < inputs.input_symbols.size() + inputs.number_of_new_symbols);

    // "6) The array EXFLAGS now contains 1 for each symbol that is exported from the dictionary, and 0 for each
    //  symbol that is not exported."

    // "7) Set:
    //      I = 0
    //      J = 0
    //  8) For each value of I from 0 to SDNUMINSYMS + SDNUMNEWSYMS – 1,"
    for (size_t i = 0; i < inputs.input_symbols.size() + inputs.number_of_new_symbols; ++i) {
        // "if EXFLAGS[I] == 1 then perform the following steps:"
        if (!export_flags[i])
            continue;
        //  "a) If I < SDNUMINSYMS then set:
        //       SDEXSYMS[J] = SDINSYMS[I]
        //       J = J + 1"
        if (i < inputs.input_symbols.size())
            exported_symbols.append(inputs.input_symbols[i]);

        //  "b) If I >= SDNUMINSYMS then set:
        //       SDEXSYMS[J] = SDNEWSYMS[I – SDNUMINSYMS]
        //       J = J + 1"
        if (i >= inputs.input_symbols.size())
            exported_symbols.append(move(new_symbols[i - inputs.input_symbols.size()]));
    }

    if (inputs.uses_huffman_encoding) {
        TRY(bit_stream->align_to_byte_boundary());
        return stream->read_until_eof();
    }
    return TRY(encoder->finalize(inputs.trailing_7fff_handling));
}

namespace {

// C.2 Input parameters
// Table C.1 – Parameters for the gray-scale image decoding procedure
struct GrayscaleInputParameters {
    bool uses_mmr { false }; // "GSMMR" in spec.

    Vector<u64> const& grayscale_image;
    Optional<BilevelImage const&> skip_pattern; // "GSUSESKIP" / "GSKIP" in spec.

    u8 bpp { 0 };         // "GSBPP" in spec.
    u32 width { 0 };      // "GSW" in spec.
    u32 height { 0 };     // "GSH" in spec.
    u8 template_id { 0 }; // "GSTEMPLATE" in spec.

    MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { MQArithmeticEncoder::Trailing7FFFHandling::Keep };
};

}

// C.5 Decoding the gray-scale image, but in reverse.
static ErrorOr<ByteBuffer> grayscale_image_encoding_procedure(GrayscaleInputParameters const& inputs, Optional<JBIG2::GenericContexts>& contexts)
{
    VERIFY(inputs.bpp < 64);

    if (inputs.grayscale_image.size() != inputs.width * inputs.height)
        return Error::from_string_literal("JBIG2Writer: Halftone graymap size does not match dimensions");

    auto bitplane = TRY(BilevelImage::create(inputs.width, inputs.height));

    // Table C.4 – Parameters used to decode a bitplane of the gray-scale image
    GenericRegionEncodingInputParameters generic_inputs { .image = *bitplane, .skip_pattern = inputs.skip_pattern };
    generic_inputs.is_modified_modified_read = inputs.uses_mmr;
    generic_inputs.gb_template = inputs.template_id;
    generic_inputs.is_typical_prediction_used = false;
    generic_inputs.is_extended_reference_template_used = false; // Missing from spec.
    generic_inputs.adaptive_template_pixels[0].x = inputs.template_id <= 1 ? 3 : 2;
    generic_inputs.adaptive_template_pixels[0].y = -1;
    generic_inputs.adaptive_template_pixels[1].x = -3;
    generic_inputs.adaptive_template_pixels[1].y = -1;
    generic_inputs.adaptive_template_pixels[2].x = 2;
    generic_inputs.adaptive_template_pixels[2].y = -2;
    generic_inputs.adaptive_template_pixels[3].x = -2;
    generic_inputs.adaptive_template_pixels[3].y = -2;

    // An MMR graymap is the only case where the size of the a generic region is not known in advance,
    // and where the data is immediately followed by more MMR data. We need to have the MMR encoder
    // write EOFB markers at the end, so that the following bitplanes can be decoded.
    // See 6.2.6 Decoding using MMR coding.
    generic_inputs.require_eof_after_mmr = GenericRegionEncodingInputParameters::RequireEOFBAfterMMR::Yes;

    AllocatingMemoryStream mmr_output_stream;
    Optional<MQArithmeticEncoder> arithmetic_encoder;
    if (generic_inputs.is_modified_modified_read) {
        generic_inputs.stream = &mmr_output_stream;
    } else {
        arithmetic_encoder = TRY(MQArithmeticEncoder::initialize(0));
        generic_inputs.arithmetic_encoder = &arithmetic_encoder.value();
    }

    // C.5 Decoding the gray-scale image
    // "The gray-scale image is obtained by decoding GSBPP bitplanes. These bitplanes are denoted (from least significant to
    //  most significant) GSPLANES[0], GSPLANES[1], . . . , GSPLANES[GSBPP – 1]. The bitplanes are Gray-coded, so
    //  that each bitplane's true value is equal to its coded value XORed with the next-more-significant bitplane."

    for (u32 y = 0; y < inputs.height; ++y)
        for (u32 x = 0; x < inputs.width; ++x)
            if (inputs.grayscale_image[y * inputs.width + x] >= (1ull << inputs.bpp))
                return Error::from_string_literal("JBIG2Writer: Halftone region graymap entry too large for number of patterns");

    // "1) Decode GSPLANES[GSBPP – 1] using the generic region decoding procedure. The parameters to the
    //     generic region decoding procedure are as shown in Table C.4."
    for (u32 y = 0; y < inputs.height; ++y) {
        for (u32 x = 0; x < inputs.width; ++x) {
            bool bit_is_set = (inputs.grayscale_image[y * inputs.width + x] & (1 << (inputs.bpp - 1))) != 0;
            bitplane->set_bit(x, y, bit_is_set);
        }
    }

    TRY(generic_region_encoding_procedure(generic_inputs, contexts));

    // "2) Set J = GSBPP – 2."
    int j = inputs.bpp - 2;

    // "3) While J >= 0, perform the following steps:"
    while (j >= 0) {
        // "a) Decode GSPLANES[J] using the generic region decoding procedure. The parameters to the generic
        //     region decoding procedure are as shown in Table C.4."
        // "b) For each pixel (x, y) in GSPLANES[J], set:
        //     GSPLANES[J][x, y] = GSPLANES[J + 1][x, y] XOR GSPLANES[J][x, y]"
        for (u32 y = 0; y < inputs.height; ++y) {
            for (u32 x = 0; x < inputs.width; ++x) {
                bool bit_is_set = (inputs.grayscale_image[y * inputs.width + x] & (1 << j)) != 0;
                bit_is_set ^= (inputs.grayscale_image[y * inputs.width + x] & (1 << (j + 1))) != 0;
                bitplane->set_bit(x, y, bit_is_set);
            }
        }

        TRY(generic_region_encoding_procedure(generic_inputs, contexts));

        // "c) Set J = J – 1."
        j = j - 1;
    }

    // "4) For each (x, y), set:
    //     GSVALS [x, y] = sum_{J = 0}^{GSBPP - 1} GSPLANES[J][x,y] × 2**J)"

    if (generic_inputs.is_modified_modified_read)
        return TRY(mmr_output_stream.read_until_eof());
    return TRY(arithmetic_encoder->finalize(inputs.trailing_7fff_handling));
}

static ErrorOr<void> encode_jbig2_header(Stream& stream, JBIG2::FileHeaderData const& header)
{
    TRY(stream.write_until_depleted(JBIG2::id_string));

    // D.4.2 File header flags
    u8 header_flags = 0;

    JBIG2::Organization organization = header.organization;
    if (organization == JBIG2::Organization::Sequential)
        header_flags |= 1;

    // FIXME: Add an option for this.
    bool uses_templates_with_12_AT_pixels = false;

    // FIXME: Maybe add support for colors one day.
    bool contains_colored_region_segments = false;

    if (!header.number_of_pages.has_value())
        header_flags |= 2;

    if (uses_templates_with_12_AT_pixels)
        header_flags |= 4;

    if (contains_colored_region_segments)
        header_flags |= 8;

    TRY(stream.write_value<u8>(header_flags));

    // D.4.3 Number of pages
    if (header.number_of_pages.has_value())
        TRY(stream.write_value<BigEndian<u32>>(header.number_of_pages.value()));

    return {};
}

namespace {

struct SerializedSegmentData {
    ByteBuffer data;
    size_t header_size { 0 };
};

struct JBIG2EncodingContext {
    JBIG2EncodingContext(Vector<JBIG2::SegmentData> const& segments)
        : segments(segments)
    {
    }

    Vector<JBIG2::SegmentData> const& segments;

    HashMap<u32, JBIG2::SegmentData const*> segment_by_id;

    HashMap<u32, SerializedSegmentData> segment_data_by_id;

    HashMap<u32, Vector<JBIG2::Code>> codes_by_segment_id;
    HashMap<u32, JBIG2::HuffmanTable> tables_by_segment_id;

    HashMap<u32, Vector<JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol>> symbols_by_segment_id;
};

}

enum class PageAssociationSize {
    Auto,
    Force32Bit,
};

static ErrorOr<void> encode_segment_header(Stream& stream, JBIG2::SegmentHeader const& header, PageAssociationSize page_association_size)
{
    // 7.2.2 Segment number
    TRY(stream.write_value<BigEndian<u32>>(header.segment_number));

    // 7.2.3 Segment header flags

    bool segment_page_association_size_is_32_bits = header.page_association >= 256 || page_association_size == PageAssociationSize::Force32Bit;

    bool segment_retained_only_by_itself_and_extension_segments = false; // FIXME: Compute?

    u8 flags = static_cast<u8>(header.type);
    if (segment_page_association_size_is_32_bits)
        flags |= 0x40;
    if (segment_retained_only_by_itself_and_extension_segments)
        flags |= 0x80;

    TRY(stream.write_value<u8>(flags));

    // 7.2.4 Referred-to segment count and retention flags
    VERIFY(header.referred_to_segment_numbers.size() == header.referred_to_segment_retention_flags.size());
    if (header.referred_to_segment_numbers.size() <= 4) {
        u8 count_and_retention_flags = 0;
        count_and_retention_flags |= header.referred_to_segment_numbers.size() << 5;
        if (header.retention_flag)
            count_and_retention_flags |= 1;
        for (size_t i = 0; i < header.referred_to_segment_numbers.size(); ++i) {
            if (header.referred_to_segment_retention_flags[i])
                count_and_retention_flags |= 1 << (i + 1);
        }
        TRY(stream.write_value<u8>(count_and_retention_flags));
    } else {
        if (header.referred_to_segment_numbers.size() >= (1 << 29))
            return Error::from_string_literal("JBIG2Writer: Too many referred-to segments");
        u32 count_of_referred_to_segments = header.referred_to_segment_numbers.size();
        TRY(stream.write_value<BigEndian<u32>>(count_of_referred_to_segments | 7 << 29));

        LittleEndianOutputBitStream bit_stream { MaybeOwned { stream } };
        TRY(bit_stream.write_bits(header.retention_flag, 1));
        for (size_t i = 0; i < header.referred_to_segment_numbers.size(); ++i)
            TRY(bit_stream.write_bits(header.referred_to_segment_retention_flags[i], 1));
        TRY(bit_stream.align_to_byte_boundary());
        TRY(bit_stream.flush_buffer_to_stream());
    }

    // 7.2.5 Referred-to segment numbers
    for (u32 referred_to_segment_number : header.referred_to_segment_numbers) {
        VERIFY(referred_to_segment_number < header.segment_number);
        if (header.segment_number <= 256)
            TRY(stream.write_value<u8>(referred_to_segment_number));
        else if (header.segment_number <= 65536)
            TRY(stream.write_value<BigEndian<u16>>(referred_to_segment_number));
        else
            TRY(stream.write_value<BigEndian<u32>>(referred_to_segment_number));
    }

    // 7.2.6 Segment page association
    if (segment_page_association_size_is_32_bits)
        TRY(stream.write_value<BigEndian<u32>>(header.page_association));
    else
        TRY(stream.write_value<u8>(header.page_association));

    // 7.2.7 Segment data length
    VERIFY(header.data_length.has_value() || header.type == JBIG2::SegmentType::ImmediateGenericRegion);
    if (header.data_length.has_value())
        TRY(stream.write_value<BigEndian<u32>>(*header.data_length));
    else
        TRY(stream.write_value<BigEndian<u32>>(0xffff'ffff));

    return {};
}

ErrorOr<void> JBIG2Writer::encode(Stream& stream, Bitmap const& bitmap, Options const&)
{
    auto bilevel_image = TRY(BilevelImage::create_from_bitmap(bitmap, DitheringAlgorithm::FloydSteinberg));

    JBIG2::FileData jbig2;
    jbig2.header.number_of_pages = 1;
    jbig2.header.organization = JBIG2::Organization::Sequential;

    u32 next_segment_number { 0 };
    auto next_segment_header = [&next_segment_number]() {
        JBIG2::SegmentHeaderData header;
        header.segment_number = next_segment_number++;
        header.page_association = 1;
        return header;
    };

    Gfx::JBIG2::PageInformationSegment page_info {};
    page_info.bitmap_width = bilevel_image->width();
    page_info.bitmap_height = bilevel_image->height();
    page_info.flags = 1; // "eventually lossless" bit set, default pixel value white, default combination operator OR.
    jbig2.segments.append(JBIG2::SegmentData {
        .header = next_segment_header(),
        .data = page_info,
    });

    JBIG2::GenericRegionSegmentData generic_region { .image = move(bilevel_image) };
    generic_region.region_segment_information.width = generic_region.image->width();
    generic_region.region_segment_information.height = generic_region.image->height();
    generic_region.region_segment_information.flags = 0;
    generic_region.adaptive_template_pixels[0] = { 3, -1 };
    generic_region.adaptive_template_pixels[1] = { -3, -1 };
    generic_region.adaptive_template_pixels[2] = { 2, -2 };
    generic_region.adaptive_template_pixels[3] = { -2, -2 };
    generic_region.flags = 1u << 3; // TPGDON, gb_template 0.
    jbig2.segments.append(JBIG2::SegmentData {
        .header = next_segment_header(),
        .data = JBIG2::ImmediateGenericRegionSegmentData { .generic_region = move(generic_region) },
    });

    jbig2.segments.append(JBIG2::SegmentData {
        .header = next_segment_header(),
        .data = JBIG2::EndOfPageSegmentData {},
    });

    return encode_with_explicit_data(stream, jbig2);
}

static ErrorOr<void> encode_region_segment_information_field(Stream& stream, JBIG2::RegionSegmentInformationField const& region_information)
{
    // 7.4.1 Region segment information field
    TRY(stream.write_value<BigEndian<u32>>(region_information.width));
    TRY(stream.write_value<BigEndian<u32>>(region_information.height));
    TRY(stream.write_value<BigEndian<u32>>(region_information.x_location));
    TRY(stream.write_value<BigEndian<u32>>(region_information.y_location));
    TRY(stream.write_value<u8>(region_information.flags));

    return {};
}

static ErrorOr<void> encode_symbol_dictionary(JBIG2::SymbolDictionarySegmentData const& symbol_dictionary, JBIG2::SegmentHeaderData const& header, JBIG2EncodingContext& context, Vector<u8>& scratch_buffer)
{
    // Get referred-to symbol and table segments off header.referred_to_segments.
    Vector<JBIG2::HuffmanTable const*> custom_tables;
    Vector<JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol> input_symbols;
    for (auto const& referred_to_segment_number : header.referred_to_segments) {
        auto maybe_segment = context.segment_by_id.get(referred_to_segment_number.segment_number);
        if (!maybe_segment.has_value())
            return Error::from_string_literal("JBIG2Writer: Could not find referred-to segment for symbol dictionary");
        auto const& referred_to_segment = *maybe_segment.value();
        if (referred_to_segment.data.has<JBIG2::TablesData>()) {
            auto maybe_table = context.tables_by_segment_id.get(referred_to_segment_number.segment_number);
            if (!maybe_table.has_value())
                return Error::from_string_literal("JBIG2Writer: Could not find referred-to table for text region");
            custom_tables.append(&maybe_table.value());
            continue;
        }
        if (referred_to_segment.data.has<JBIG2::SymbolDictionarySegmentData>()) {
            auto maybe_symbols = context.symbols_by_segment_id.get(referred_to_segment_number.segment_number);
            if (!maybe_symbols.has_value())
                return Error::from_string_literal("JBIG2Writer: Could not find referred-to symbols for text region");
            input_symbols.extend(maybe_symbols.value());
            continue;
        }
    }

    // 7.4.2 Symbol dictionary segment syntax
    bool uses_huffman_encoding = (symbol_dictionary.flags & 1) != 0;
    bool uses_refinement_or_aggregate_coding = (symbol_dictionary.flags & 2) != 0;
    u8 symbol_template = (symbol_dictionary.flags >> 10) & 3;
    u8 symbol_refinement_template = (symbol_dictionary.flags >> 12) & 1;

    u8 number_of_adaptive_template_pixels = 0;
    if (!uses_huffman_encoding)
        number_of_adaptive_template_pixels = symbol_template == 0 ? 4 : 1;
    u8 number_of_refinement_adaptive_template_pixels = (uses_refinement_or_aggregate_coding && symbol_refinement_template == 0) ? 2 : 0;

    u32 number_of_new_symbols = 0;
    for (auto const& height_class : symbol_dictionary.height_classes)
        number_of_new_symbols += height_class.symbols.size();

    auto huffman_tables = TRY(symbol_dictionary_huffman_tables_from_flags(symbol_dictionary.flags, custom_tables));

    SymbolDictionaryEncodingInputParameters inputs;
    inputs.uses_huffman_encoding = uses_huffman_encoding;
    inputs.uses_refinement_or_aggregate_coding = uses_refinement_or_aggregate_coding;
    inputs.input_symbols = move(input_symbols);
    inputs.export_flags_for_referred_to_symbols = symbol_dictionary.export_flags_for_referred_to_symbols;
    inputs.height_classes = symbol_dictionary.height_classes;
    inputs.number_of_new_symbols = number_of_new_symbols;
    inputs.delta_height_table = huffman_tables.delta_height_table;
    inputs.delta_width_table = huffman_tables.delta_width_table;
    inputs.bitmap_size_table = huffman_tables.bitmap_size_table;
    inputs.number_of_symbol_instances_table = huffman_tables.number_of_symbol_instances_table;
    inputs.symbol_template = symbol_template;
    inputs.refinement_template = symbol_refinement_template;
    inputs.adaptive_template_pixels = symbol_dictionary.adaptive_template_pixels;
    inputs.refinement_adaptive_template_pixels = symbol_dictionary.refinement_adaptive_template_pixels;
    inputs.trailing_7fff_handling = symbol_dictionary.trailing_7fff_handling;

    Vector<JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol> exported_symbols;
    ByteBuffer data = TRY(symbol_dictionary_encoding_procedure(inputs, exported_symbols));

    u32 number_of_exported_symbols = exported_symbols.size();

    TRY(scratch_buffer.try_resize(2 + number_of_adaptive_template_pixels * 2 + number_of_refinement_adaptive_template_pixels * 2 + 2 * 4 + data.size()));
    FixedMemoryStream stream { scratch_buffer, FixedMemoryStream::Mode::ReadWrite };
    TRY(stream.write_value<BigEndian<u16>>(symbol_dictionary.flags));
    for (int i = 0; i < number_of_adaptive_template_pixels; ++i) {
        TRY(stream.write_value<i8>(symbol_dictionary.adaptive_template_pixels[i].x));
        TRY(stream.write_value<i8>(symbol_dictionary.adaptive_template_pixels[i].y));
    }
    for (int i = 0; i < number_of_refinement_adaptive_template_pixels; ++i) {
        TRY(stream.write_value<i8>(symbol_dictionary.refinement_adaptive_template_pixels[i].x));
        TRY(stream.write_value<i8>(symbol_dictionary.refinement_adaptive_template_pixels[i].y));
    }
    TRY(stream.write_value<BigEndian<u32>>(number_of_exported_symbols));
    TRY(stream.write_value<BigEndian<u32>>(number_of_new_symbols));
    TRY(stream.write_until_depleted(data));

    if (context.symbols_by_segment_id.set(header.segment_number, move(exported_symbols)) != HashSetResult::InsertedNewEntry)
        return Error::from_string_literal("JBIG2Writer: Duplicate symbol segment ID");

    return {};
}

struct RunCode {
    u8 symbol { 0 };
    u8 count { 0 }; // used for special symbols 32-34.
};

// This is very similar to DeflateCompressor::encode_huffman_lengths().
// But:
// * lengths.size() can be much larger than 288
// * there are 35 different codes
// * code 32 has different semantics than deflate's code 16, requires last_non_zero_symbol
static size_t code_lengths_to_run_codes(ReadonlyBytes lengths, Span<RunCode> encoded_lengths)
{
    // 7.4.3.1.7 Symbol ID Huffman table decoding
    // Table 32 – Meaning of the run codes
    VERIFY(encoded_lengths.size() >= lengths.size());
    size_t encoded_count = 0;
    size_t i = 0;
    u8 last_non_zero_symbol = 8; // "If code 16 is used before a non-zero value has been emitted, a value of 8 is repeated."
    while (i < lengths.size()) {
        if (lengths[i] == 0) {
            auto zero_count = 0;
            for (size_t j = i; j < min(lengths.size(), i + 138) && lengths[j] == 0; j++)
                zero_count++;

            if (zero_count < 3) { // below minimum repeated zero count
                encoded_lengths[encoded_count++].symbol = 0;
                i++;
                continue;
            }

            if (zero_count <= 10) {
                // "RUNCODE33: Repeat a symbol ID code length of 0 for 3-10 times."
                encoded_lengths[encoded_count].symbol = 33;
                encoded_lengths[encoded_count++].count = zero_count;
            } else {
                // "RUNCODE34: Repeat a symbol ID code length of 0 for 11-138 times."
                encoded_lengths[encoded_count].symbol = 34;
                encoded_lengths[encoded_count++].count = zero_count;
            }
            i += zero_count;
            continue;
        }

        VERIFY(lengths[i] != 0);
        last_non_zero_symbol = lengths[i];
        encoded_lengths[encoded_count++].symbol = lengths[i++];

        // "RUNCODE32: Copy the previous symbol ID code length 3-6 times."
        // This is different from deflate (but except for the code, identically to WebP!)
        auto copy_count = 0;
        for (size_t j = i; j < min(lengths.size(), i + 6) && lengths[j] == last_non_zero_symbol; j++)
            copy_count++;

        if (copy_count >= 3) {
            encoded_lengths[encoded_count].symbol = 32;
            encoded_lengths[encoded_count++].count = copy_count;
            i += copy_count;
            continue;
        }
    }
    return encoded_count;
}

static ErrorOr<void> store_huffman_code_lengths(Stream& stream, ReadonlyBytes code_lengths)
{
    // Similar to Deflate or WebP lossless, the code lengths are represented using a custom bytecode that is itself Huffman-compressed for serialization.
    // See 7.4.3.1.7 Symbol ID Huffman table decoding.

    // Drop trailing zero lengths.
    // This is similar to the loops in Deflate::encode_block_lengths().
    size_t code_count = code_lengths.size();
    while (code_count > 0 && code_lengths[code_count - 1] == 0)
        code_count--;

    Vector<RunCode, 256> run_codes;
    TRY(run_codes.try_resize(code_count));
    auto run_codes_count = code_lengths_to_run_codes(code_lengths.trim(code_count), run_codes.span());

    // The code to compute code length code lengths is very similar to some of the code in DeflateCompressor::flush().
    // count code length frequencies
    Array<u16, 35> run_codes_histogram { 0 };
    for (size_t i = 0; i < run_codes_count; i++) {
        VERIFY(run_codes_histogram[run_codes[i].symbol] < UINT16_MAX);
        run_codes_histogram[run_codes[i].symbol]++;
    }

    // generate optimal huffman code lengths code lengths
    Array<u8, 35> run_codes_lengths {};
    Compress::generate_huffman_lengths(run_codes_lengths, run_codes_histogram, 15);

    auto lengths_codes = TRY(JBIG2::assign_huffman_codes(run_codes_lengths));
    Vector<JBIG2::Code> symbol_id_lengths_codes;
    for (auto const& [i, length] : enumerate(run_codes_lengths)) {
        if (length == 0)
            continue;
        JBIG2::Code code { .prefix_length = length, .range_length = 0, .first_value = i, .code = lengths_codes[i] };
        symbol_id_lengths_codes.append(code);
    }
    auto symbol_id_lengths_table = JBIG2::HuffmanTable { symbol_id_lengths_codes };

    // Save huffman-compressed code lengths to stream.
    BigEndianOutputBitStream symbol_id_bit_stream { MaybeOwned { stream } };

    for (auto run_codes_length : run_codes_lengths)
        TRY(symbol_id_bit_stream.write_bits(run_codes_length, 4));

    for (auto const& run_code : run_codes.span().trim(run_codes_count)) {
        TRY(symbol_id_lengths_table.write_symbol_non_oob(symbol_id_bit_stream, run_code.symbol));
        if (run_code.symbol == 32)
            TRY(symbol_id_bit_stream.write_bits(run_code.count - 3u, 2));
        else if (run_code.symbol == 33)
            TRY(symbol_id_bit_stream.write_bits(run_code.count - 3u, 3));
        else if (run_code.symbol == 34)
            TRY(symbol_id_bit_stream.write_bits(run_code.count - 11u, 7));
    }

    TRY(symbol_id_bit_stream.align_to_byte_boundary());
    return {};
}

static ErrorOr<void> encode_text_region(JBIG2::TextRegionSegmentData const& text_region, JBIG2::SegmentHeaderData const& header, JBIG2EncodingContext const& context, Vector<u8>& scratch_buffer)
{
    // Get referred-to symbol dictionaries and tables off header.referred_to_segments.
    Vector<JBIG2::HuffmanTable const*> custom_tables;
    Vector<JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol> symbols;
    for (auto const& referred_to_segment_number : header.referred_to_segments) {
        auto maybe_segment = context.segment_by_id.get(referred_to_segment_number.segment_number);
        if (!maybe_segment.has_value())
            return Error::from_string_literal("JBIG2Writer: Could not find referred-to segment for text region");
        auto const& referred_to_segment = *maybe_segment.value();
        if (referred_to_segment.data.has<JBIG2::TablesData>()) {
            auto maybe_table = context.tables_by_segment_id.get(referred_to_segment_number.segment_number);
            if (!maybe_table.has_value())
                return Error::from_string_literal("JBIG2Writer: Could not find referred-to table for text region");
            custom_tables.append(&maybe_table.value());
            continue;
        }
        if (referred_to_segment.data.has<JBIG2::SymbolDictionarySegmentData>()) {
            auto maybe_symbols = context.symbols_by_segment_id.get(referred_to_segment_number.segment_number);
            if (!maybe_symbols.has_value())
                return Error::from_string_literal("JBIG2Writer: Could not find referred-to symbols for text region");
            symbols.extend(maybe_symbols.value());
            continue;
        }
    }

    // 7.4.3 Text region segment syntax
    bool uses_huffman_encoding = (text_region.flags & 1) != 0;
    bool uses_refinement_coding = (text_region.flags & 2) != 0;
    u8 log_strip_size = (text_region.flags >> 2) & 0b11;
    u8 strip_size = 1u << log_strip_size;
    u8 reference_corner = (text_region.flags >> 4) & 0b11;
    bool is_transposed = (text_region.flags >> 6) & 1;
    u8 combination_operator = (text_region.flags >> 7) & 3; // "SBCOMBOP" in spec.
    if (combination_operator > 4)
        return Error::from_string_literal("JBIG2Writer: Invalid text region combination operator");
    u8 delta_s_offset_value = (text_region.flags >> 10) & 0x1F;
    i8 delta_s_offset = AK::sign_extend(delta_s_offset_value, 5);
    u8 refinement_template = (text_region.flags >> 15) != 0;

    u32 id_symbol_code_length = ceil(log2(symbols.size()));

    ByteBuffer symbol_id_huffman_decoding_table;
    Vector<JBIG2::Code> symbol_id_codes;
    Optional<JBIG2::HuffmanTable> symbol_id_table_storage;
    JBIG2::HuffmanTable const* symbol_id_table = nullptr;

    u32 number_of_symbol_instances = 0;
    u32 highest_symbol_id = 0;
    for (auto const& strip : text_region.strips) {
        number_of_symbol_instances += strip.symbol_instances.size();
        for (auto const& instance : strip.symbol_instances)
            highest_symbol_id = max(highest_symbol_id, instance.symbol_id);
    }

    if (uses_huffman_encoding) {
        // FIXME: Maybe support this one day; the file format supports 32 bits per symbol.
        if (highest_symbol_id >= (1u << 15))
            return Error::from_string_literal("JBIG2Writer: Cannot currently encode more than 32767 symbols with Huffman coding");

        // Compute optimal huffman table for symbol IDs.
        Vector<u16> histogram;
        histogram.resize(highest_symbol_id + 1);
        for (auto const& strip : text_region.strips) {
            for (auto const& instance : strip.symbol_instances) {
                if (histogram[instance.symbol_id] < UINT16_MAX)
                    histogram[instance.symbol_id]++;
            }
        }
        Vector<u8> code_lengths;
        code_lengths.resize(highest_symbol_id + 1);
        Compress::generate_huffman_lengths(code_lengths, histogram, 15);

        auto codes = TRY(JBIG2::assign_huffman_codes(code_lengths));
        for (auto const& [i, length] : enumerate(code_lengths)) {
            if (length == 0)
                continue;
            JBIG2::Code code { .prefix_length = length, .range_length = 0, .first_value = i, .code = codes[i] };
            symbol_id_codes.append(code);
        }
        symbol_id_table_storage = JBIG2::HuffmanTable { symbol_id_codes };
        symbol_id_table = &symbol_id_table_storage.value();

        AllocatingMemoryStream symbol_id_table_stream;
        TRY(store_huffman_code_lengths(symbol_id_table_stream, code_lengths));
        symbol_id_huffman_decoding_table = TRY(symbol_id_table_stream.read_until_eof());
    }

    JBIG2::TextRegionHuffmanTables huffman_tables;
    if (uses_huffman_encoding)
        huffman_tables = TRY(text_region_huffman_tables_from_flags(text_region.huffman_flags, custom_tables));

    TextRegionEncodingInputParameters inputs { .symbol_instance_strips = text_region.strips };
    inputs.uses_huffman_encoding = uses_huffman_encoding;
    inputs.uses_refinement_coding = uses_refinement_coding;
    inputs.size_of_symbol_instance_strips = strip_size;
    inputs.initial_strip_t = text_region.initial_strip_t;
    inputs.symbol_id_table = symbol_id_table;
    inputs.id_symbol_code_length = id_symbol_code_length;
    inputs.symbols = move(symbols);
    inputs.is_transposed = is_transposed;
    inputs.reference_corner = static_cast<JBIG2::ReferenceCorner>(reference_corner);
    inputs.delta_s_offset = delta_s_offset;
    inputs.first_s_table = huffman_tables.first_s_table;
    inputs.subsequent_s_table = huffman_tables.subsequent_s_table;
    inputs.delta_t_table = huffman_tables.delta_t_table;
    inputs.refinement_delta_width_table = huffman_tables.refinement_delta_width_table;
    inputs.refinement_delta_height_table = huffman_tables.refinement_delta_height_table;
    inputs.refinement_x_offset_table = huffman_tables.refinement_x_offset_table;
    inputs.refinement_y_offset_table = huffman_tables.refinement_y_offset_table;
    inputs.refinement_size_table = huffman_tables.refinement_size_table;
    inputs.refinement_template = refinement_template;
    inputs.refinement_adaptive_template_pixels = text_region.refinement_adaptive_template_pixels;

    Optional<TextContexts> text_contexts;
    if (!uses_huffman_encoding)
        text_contexts = TextContexts { id_symbol_code_length };
    Optional<RefinementContexts> refinement_contexts;
    if (uses_refinement_coding)
        refinement_contexts = RefinementContexts { refinement_template };

    AllocatingMemoryStream output_stream;
    Optional<MQArithmeticEncoder> encoder;
    Optional<BigEndianOutputBitStream> bit_stream;
    if (uses_huffman_encoding) {
        bit_stream = BigEndianOutputBitStream { MaybeOwned { output_stream } };
        inputs.bit_stream = &bit_stream.value();
    } else {
        encoder = TRY(MQArithmeticEncoder::initialize(0));
        inputs.arithmetic_encoder = &encoder.value();
    }

    TRY(text_region_encoding_procedure(inputs, text_contexts, refinement_contexts));
    ByteBuffer data;
    if (uses_huffman_encoding) {
        TRY(bit_stream->align_to_byte_boundary());
        data = TRY(output_stream.read_until_eof());
    } else {
        data = TRY(encoder->finalize(text_region.trailing_7fff_handling));
    }

    u8 number_of_refinement_adaptive_template_pixels = (uses_refinement_coding && refinement_template == 0) ? 2 : 0;
    TRY(scratch_buffer.try_resize(sizeof(JBIG2::RegionSegmentInformationField) + 2 + (uses_huffman_encoding ? 2 : 0) + number_of_refinement_adaptive_template_pixels * 2 + 4 + symbol_id_huffman_decoding_table.size() + data.size()));
    FixedMemoryStream stream { scratch_buffer, FixedMemoryStream::Mode::ReadWrite };

    TRY(encode_region_segment_information_field(stream, text_region.region_segment_information));
    TRY(stream.write_value<BigEndian<u16>>(text_region.flags));
    if (uses_huffman_encoding)
        TRY(stream.write_value<BigEndian<u16>>(text_region.huffman_flags));
    for (int i = 0; i < number_of_refinement_adaptive_template_pixels; ++i) {
        TRY(stream.write_value<i8>(text_region.refinement_adaptive_template_pixels[i].x));
        TRY(stream.write_value<i8>(text_region.refinement_adaptive_template_pixels[i].y));
    }
    TRY(stream.write_value<BigEndian<u32>>(number_of_symbol_instances));
    TRY(stream.write_until_depleted(symbol_id_huffman_decoding_table));
    TRY(stream.write_until_depleted(data));

    return {};
}

static ErrorOr<void> encode_pattern_dictionary(JBIG2::PatternDictionarySegmentData const& pattern_dictionary, Vector<u8>& scratch_buffer)
{
    // 7.4.4 Pattern dictionary segment syntax
    if (pattern_dictionary.image->width() != (pattern_dictionary.gray_max + 1) * pattern_dictionary.pattern_width)
        return Error::from_string_literal("JBIG2Writer: Pattern dictionary image has wrong width");
    if (pattern_dictionary.image->height() != pattern_dictionary.pattern_height)
        return Error::from_string_literal("JBIG2Writer: Pattern dictionary image has wrong height");

    // Table 27 – Parameters used to decode a pattern dictionary's collective bitmap
    GenericRegionEncodingInputParameters inputs { .image = *pattern_dictionary.image };
    inputs.is_modified_modified_read = pattern_dictionary.flags & 1;
    inputs.gb_template = (pattern_dictionary.flags >> 1) & 3;
    inputs.is_typical_prediction_used = false;
    inputs.is_extended_reference_template_used = false;
    inputs.adaptive_template_pixels[0].x = -pattern_dictionary.pattern_width;
    inputs.adaptive_template_pixels[0].y = 0;
    inputs.adaptive_template_pixels[1].x = -3;
    inputs.adaptive_template_pixels[1].y = -1;
    inputs.adaptive_template_pixels[2].x = 2;
    inputs.adaptive_template_pixels[2].y = -2;
    inputs.adaptive_template_pixels[3].x = -2;
    inputs.adaptive_template_pixels[3].y = -2;
    inputs.require_eof_after_mmr = GenericRegionEncodingInputParameters::RequireEOFBAfterMMR::No;

    AllocatingMemoryStream mmr_output_stream;
    Optional<JBIG2::GenericContexts> contexts;
    Optional<MQArithmeticEncoder> arithmetic_encoder;
    if (inputs.is_modified_modified_read) {
        inputs.stream = &mmr_output_stream;
    } else {
        contexts = JBIG2::GenericContexts { inputs.gb_template };
        arithmetic_encoder = TRY(MQArithmeticEncoder::initialize(0));
        inputs.arithmetic_encoder = &arithmetic_encoder.value();
    }
    TRY(generic_region_encoding_procedure(inputs, contexts));
    ByteBuffer data;
    if (inputs.is_modified_modified_read)
        data = TRY(mmr_output_stream.read_until_eof());
    else
        data = TRY(arithmetic_encoder->finalize(pattern_dictionary.trailing_7fff_handling));

    TRY(scratch_buffer.try_resize(3 * 1 + 4 + data.size()));
    FixedMemoryStream stream { scratch_buffer, FixedMemoryStream::Mode::ReadWrite };

    TRY(stream.write_value<u8>(pattern_dictionary.flags));
    TRY(stream.write_value<u8>(pattern_dictionary.pattern_width));
    TRY(stream.write_value<u8>(pattern_dictionary.pattern_height));
    TRY(stream.write_value<BigEndian<u32>>(pattern_dictionary.gray_max));
    TRY(stream.write_until_depleted(data));
    return {};
}

static ErrorOr<void> encode_halftone_region(JBIG2::HalftoneRegionSegmentData const& halftone_region, JBIG2::SegmentHeaderData const& header, JBIG2EncodingContext const& context, Vector<u8>& scratch_buffer)
{
    // 7.4.5 Halftone region segment syntax
    if (header.referred_to_segments.size() != 1)
        return Error::from_string_literal("JBIG2Writer: Halftone region must refer to exactly one segment");

    auto maybe_segment = context.segment_by_id.get(header.referred_to_segments[0].segment_number);
    if (!maybe_segment.has_value())
        return Error::from_string_literal("JBIG2Writer: Could not find referred-to segment for halftone region");
    auto const& referred_to_segment = *maybe_segment.value();
    if (!referred_to_segment.data.has<JBIG2::PatternDictionarySegmentData>())
        return Error::from_string_literal("JBIG2Writer: Halftone region must refer to a pattern dictionary segment");
    auto const& pattern_dictionary = referred_to_segment.data.get<JBIG2::PatternDictionarySegmentData>();

    // FIXME: Add a halftone_region_encoding_procedure()? For now, it's just inlined here.
    u32 bits_per_pattern = ceil(log2(pattern_dictionary.gray_max + 1));

    // FIXME: Implement support for enable_skip.
    Optional<BilevelImage const&> skip_pattern;
    bool const enable_skip = ((halftone_region.flags >> 3) & 1) != 0;
    if (enable_skip)
        return Error::from_string_literal("JBIG2Writer: Halftone region skip pattern not yet implemented");

    Vector<u64> grayscale_image = TRY(halftone_region.grayscale_image.visit(
        [](Vector<u64> const& grayscale_image) -> ErrorOr<Vector<u64>> {
            return grayscale_image;
        },
        [&halftone_region, &pattern_dictionary](NonnullRefPtr<Gfx::Bitmap> const& reference) -> ErrorOr<Vector<u64>> {
            // FIXME: This does not handle rotation or non-trivial grid vectors yet.
            if (halftone_region.grid_offset_x_times_256 != 0 || halftone_region.grid_offset_y_times_256 != 0)
                return Error::from_string_literal("JBIG2Writer: Halftone region match_image with non-zero grid offsets not yet implemented");
            if (pattern_dictionary.pattern_width != pattern_dictionary.pattern_height
                || halftone_region.grid_vector_x_times_256 / 256 != pattern_dictionary.pattern_width
                || halftone_region.grid_vector_y_times_256 != 0)
                return Error::from_string_literal("JBIG2Writer: Halftone region match_image with non-trivial grid vectors not yet implemented");

            Vector<u64> converted_image;
            TRY(converted_image.try_resize(halftone_region.grayscale_width * halftone_region.grayscale_height));
            for (u32 y = 0; y < halftone_region.grayscale_height; ++y) {
                for (u32 x = 0; x < halftone_region.grayscale_width; ++x) {
                    // Find best tile in pattern dictionary that matches reference best.
                    // FIXME: This is a naive, inefficient implementation.
                    u32 best_pattern_index = 0;
                    u32 best_pattern_difference = UINT32_MAX;
                    for (u32 pattern_index = 0; pattern_index <= pattern_dictionary.gray_max; ++pattern_index) {
                        u32 pattern_x = pattern_index * pattern_dictionary.pattern_width;
                        u32 pattern_difference = 0;
                        for (u32 py = 0; py < pattern_dictionary.pattern_height; ++py) {
                            for (u32 px = 0; px < pattern_dictionary.pattern_width; ++px) {
                                int reference_x = x * pattern_dictionary.pattern_width + px;
                                int reference_y = y * pattern_dictionary.pattern_height + py;
                                if (reference_x >= reference->width() || reference_y >= reference->height())
                                    continue;
                                auto pattern_pixel = pattern_dictionary.image->get_bit(pattern_x + px, py);
                                auto reference_pixel = reference->get_pixel(reference_x, reference_y);
                                pattern_difference += abs(reference_pixel.luminosity() - (pattern_pixel ? 0 : 255));
                            }
                        }
                        if (pattern_difference < best_pattern_difference) {
                            best_pattern_difference = pattern_difference;
                            best_pattern_index = pattern_index;
                        }
                    }
                    converted_image[y * halftone_region.grayscale_width + x] = best_pattern_index;
                }
            }

            return converted_image;
        }));

    GrayscaleInputParameters inputs { .grayscale_image = grayscale_image, .skip_pattern = skip_pattern };
    inputs.uses_mmr = halftone_region.flags & 1;
    inputs.skip_pattern = skip_pattern;
    inputs.bpp = bits_per_pattern;
    inputs.width = halftone_region.grayscale_width;
    inputs.height = halftone_region.grayscale_height;
    inputs.template_id = (halftone_region.flags >> 1) & 3;
    inputs.trailing_7fff_handling = halftone_region.trailing_7fff_handling;
    Optional<JBIG2::GenericContexts> contexts;
    if (!inputs.uses_mmr)
        contexts = JBIG2::GenericContexts { inputs.template_id };
    auto data = TRY(grayscale_image_encoding_procedure(inputs, contexts));

    TRY(scratch_buffer.try_resize(sizeof(JBIG2::RegionSegmentInformationField) + 1 + 4 * 4 + 2 * 2 + data.size()));
    FixedMemoryStream stream { scratch_buffer, FixedMemoryStream::Mode::ReadWrite };

    TRY(encode_region_segment_information_field(stream, halftone_region.region_segment_information));
    TRY(stream.write_value<u8>(halftone_region.flags));
    TRY(stream.write_value<BigEndian<u32>>(halftone_region.grayscale_width));
    TRY(stream.write_value<BigEndian<u32>>(halftone_region.grayscale_height));
    TRY(stream.write_value<BigEndian<i32>>(halftone_region.grid_offset_x_times_256));
    TRY(stream.write_value<BigEndian<i32>>(halftone_region.grid_offset_y_times_256));
    TRY(stream.write_value<BigEndian<u16>>(halftone_region.grid_vector_x_times_256));
    TRY(stream.write_value<BigEndian<u16>>(halftone_region.grid_vector_y_times_256));
    TRY(stream.write_until_depleted(data));
    return {};
}

static ErrorOr<void> encode_generic_region(JBIG2::GenericRegionSegmentData const& generic_region, Vector<u8>& scratch_buffer)
{
    // 7.4.6 Generic region segment syntax
    GenericRegionEncodingInputParameters inputs { .image = *generic_region.image };
    inputs.is_modified_modified_read = generic_region.flags & 1;
    inputs.gb_template = (generic_region.flags >> 1) & 3;
    inputs.is_typical_prediction_used = (generic_region.flags >> 3) & 1;
    inputs.is_extended_reference_template_used = (generic_region.flags >> 4) & 1;
    inputs.adaptive_template_pixels = generic_region.adaptive_template_pixels;
    inputs.require_eof_after_mmr = GenericRegionEncodingInputParameters::RequireEOFBAfterMMR::No;

    AllocatingMemoryStream mmr_output_stream;
    Optional<JBIG2::GenericContexts> contexts;
    Optional<MQArithmeticEncoder> arithmetic_encoder;
    if (inputs.is_modified_modified_read) {
        inputs.stream = &mmr_output_stream;
    } else {
        contexts = JBIG2::GenericContexts { inputs.gb_template };
        arithmetic_encoder = TRY(MQArithmeticEncoder::initialize(0));
        inputs.arithmetic_encoder = &arithmetic_encoder.value();
    }
    TRY(generic_region_encoding_procedure(inputs, contexts));
    ByteBuffer data;
    if (inputs.is_modified_modified_read)
        data = TRY(mmr_output_stream.read_until_eof());
    else
        data = TRY(arithmetic_encoder->finalize(generic_region.trailing_7fff_handling));

    int number_of_adaptive_template_pixels = 0;
    if (!inputs.is_modified_modified_read)
        number_of_adaptive_template_pixels = inputs.gb_template == 0 ? 4 : 1;

    if (inputs.gb_template == 0 && inputs.is_extended_reference_template_used) {
        // This was added in T.88 Amendment 2 (https://www.itu.int/rec/T-REC-T.88-200306-S!Amd2/en) mid-2003.
        // I haven't seen it being used in the wild, and the spec says "32-byte field as shown below" and then shows 24 bytes,
        // so it's not clear how much data to write.
        return Error::from_string_literal("JBIG2Writer: GBTEMPLATE=0 EXTTEMPLATE=1 not yet implemented");
    }

    TRY(scratch_buffer.try_resize(sizeof(JBIG2::RegionSegmentInformationField) + 1 + 2 * number_of_adaptive_template_pixels + data.size() + (generic_region.real_height_for_generic_region_of_initially_unknown_size.has_value() ? 4 : 0)));
    FixedMemoryStream stream { scratch_buffer, FixedMemoryStream::Mode::ReadWrite };

    TRY(encode_region_segment_information_field(stream, generic_region.region_segment_information));
    TRY(stream.write_value<u8>(generic_region.flags));
    for (int i = 0; i < number_of_adaptive_template_pixels; ++i) {
        TRY(stream.write_value<i8>(generic_region.adaptive_template_pixels[i].x));
        TRY(stream.write_value<i8>(generic_region.adaptive_template_pixels[i].y));
    }
    TRY(stream.write_until_depleted(data));
    if (generic_region.real_height_for_generic_region_of_initially_unknown_size.has_value())
        TRY(stream.write_value<BigEndian<u32>>(generic_region.real_height_for_generic_region_of_initially_unknown_size.value()));
    return {};
}

static ErrorOr<void> encode_generic_refinement_region(JBIG2::GenericRefinementRegionSegmentData const& generic_refinement_region, JBIG2::SegmentHeaderData const& header, JBIG2EncodingContext const& context, Vector<u8>& scratch_buffer)
{
    // 7.4.7 Generic refinement region syntax
    if (header.referred_to_segments.size() > 1)
        return Error::from_string_literal("JBIG2Writer: Generic refinement region must refer to at most one segment");

    // 7.4.7.4 Reference bitmap selection
    auto const reference_bitmap = TRY([&]() -> ErrorOr<BilevelSubImage> {
        // "If this segment refers to another region segment, then set the reference bitmap GRREFERENCE to be the current
        //  contents of the auxiliary buffer associated with the region segment that this segment refers to."
        if (header.referred_to_segments.size() == 1) {
            auto maybe_segment = context.segment_by_id.get(header.referred_to_segments[0].segment_number);
            if (!maybe_segment.has_value())
                return Error::from_string_literal("JBIG2Writer: Could not find referred-to segment for generic refinement region");
            auto const& referred_to_segment = *maybe_segment.value();

            return TRY(referred_to_segment.data.visit(
                           [](JBIG2::IntermediateGenericRegionSegmentData const& generic_region_wrapper) -> ErrorOr<BilevelImage const*> {
                               return generic_region_wrapper.generic_region.image;
                           },
                           [](JBIG2::IntermediateGenericRefinementRegionSegmentData const& generic_refinement_region_wrapper) -> ErrorOr<BilevelImage const*> {
                               return generic_refinement_region_wrapper.generic_refinement_region.image;
                           },
                           [](auto const&) -> ErrorOr<BilevelImage const*> {
                               return Error::from_string_literal("JBIG2Writer: Generic refinement region can only refer to intermediate region segments");
                           }))
                ->as_subbitmap();
        }

        // "If this segment does not refer to another region segment, set GRREFERENCE to be a bitmap containing the current
        //  contents of the page buffer (see clause 8), restricted to the area of the page buffer specified by this segment's region
        //  segment information field."
        VERIFY(header.referred_to_segments.size() == 0);
        Vector<ReadonlyBytes> preceding_segments_on_same_page;
        for (auto const& segment : context.segments) {
            if (segment.header.page_association == 0 || segment.header.page_association == header.page_association) {
                if (&segment.header == &header)
                    break;
                auto const& data = context.segment_data_by_id.get(segment.header.segment_number);
                preceding_segments_on_same_page.append(data->data);
            }
        }
        auto bitmap = TRY(JBIG2ImageDecoderPlugin::decode_embedded(preceding_segments_on_same_page));
        return bitmap->subbitmap(generic_refinement_region.region_segment_information.rect());
    }());

    GenericRefinementRegionEncodingInputParameters inputs {
        .image = *generic_refinement_region.image,
        .reference_bitmap = reference_bitmap,
    };
    inputs.gr_template = generic_refinement_region.flags & 1;
    inputs.is_typical_prediction_used = (generic_refinement_region.flags >> 1) & 1;
    inputs.adaptive_template_pixels = generic_refinement_region.adaptive_template_pixels;
    RefinementContexts contexts { inputs.gr_template };
    MQArithmeticEncoder encoder = TRY(MQArithmeticEncoder::initialize(0));
    TRY(generic_refinement_region_encoding_procedure(inputs, encoder, contexts));
    auto data = TRY(encoder.finalize(generic_refinement_region.trailing_7fff_handling));

    int number_of_adaptive_template_pixels = inputs.gr_template == 0 ? 2 : 0;

    TRY(scratch_buffer.try_resize(sizeof(JBIG2::RegionSegmentInformationField) + 1 + 2 * number_of_adaptive_template_pixels + data.size()));
    FixedMemoryStream stream { scratch_buffer, FixedMemoryStream::Mode::ReadWrite };

    TRY(encode_region_segment_information_field(stream, generic_refinement_region.region_segment_information));
    TRY(stream.write_value<u8>(generic_refinement_region.flags));
    for (int i = 0; i < number_of_adaptive_template_pixels; ++i) {
        TRY(stream.write_value<i8>(generic_refinement_region.adaptive_template_pixels[i].x));
        TRY(stream.write_value<i8>(generic_refinement_region.adaptive_template_pixels[i].y));
    }
    TRY(stream.write_until_depleted(data));
    return {};
}

static ErrorOr<void> encode_page_information_data(Stream& stream, JBIG2::PageInformationSegment const& page_information)
{
    // 7.4.8 Page information segment syntax
    TRY(stream.write_value<BigEndian<u32>>(page_information.bitmap_width));
    TRY(stream.write_value<BigEndian<u32>>(page_information.bitmap_height));
    TRY(stream.write_value<BigEndian<u32>>(page_information.page_x_resolution));
    TRY(stream.write_value<BigEndian<u32>>(page_information.page_y_resolution));
    TRY(stream.write_value<u8>(page_information.flags));
    TRY(stream.write_value<BigEndian<u16>>(page_information.striping_information));
    return {};
}

static ErrorOr<void> encode_tables(JBIG2::TablesData const& tables, JBIG2::SegmentHeaderData const& header, JBIG2EncodingContext& context, Vector<u8>& scratch_buffer)
{
    // 7.4.13 Code table segment syntax
    // B.2 Code table structure, but in reverse
    bool has_out_of_band = tables.flags & 1;             // "HTOOB" in spec.
    u8 prefix_bit_count = ((tables.flags >> 1) & 7) + 1; // "HTPS" (hash table prefix size) in spec.
    u8 range_bit_count = ((tables.flags >> 4) & 7) + 1;  // "HTRS" (hash table range size) in spec.

    AllocatingMemoryStream output_stream;

    // "1) Decode the code table flags field as described in B.2.1. This sets the values HTOOB, HTPS and HTRS."
    TRY(output_stream.write_value<u8>(tables.flags));

    // "2) Decode the code table lowest value field as described in B.2.2. Let HTLOW be the value decoded."
    TRY(output_stream.write_value<BigEndian<i32>>(tables.lowest_value));

    // "3) Decode the code table highest value field as described in B.2.3. Let HTHIGH be the value decoded."
    TRY(output_stream.write_value<BigEndian<i32>>(tables.highest_value));

    auto bit_stream = BigEndianOutputBitStream { MaybeOwned { output_stream } };

    auto write_prefix_length = [&](u8 length) -> ErrorOr<void> {
        if (length >= (1 << prefix_bit_count))
            return Error::from_string_literal("JBIG2Writer: Table prefix length too large for bit count");
        TRY(bit_stream.write_bits<u8>(length, prefix_bit_count));
        return {};
    };

    // "4) Set:
    //         CURRANGELOW = HTLOW
    //         NTEMP = 0"
    i32 value = tables.lowest_value;
    size_t i = 0;

    // "5) Decode each table line as follows:"
    Vector<u8> prefix_lengths;
    Vector<u8> range_lengths;
    Vector<Optional<i32>> range_lows;
    do {
        if (i >= tables.entries.size())
            return Error::from_string_literal("JBIG2Writer: Not enough table entries");

        // "a) Read HTPS bits."
        TRY(write_prefix_length(tables.entries[i].prefix_length));
        TRY(prefix_lengths.try_append(tables.entries[i].prefix_length));

        // "b) Read HTRS bits."
        if (tables.entries[i].range_length >= (1 << range_bit_count))
            return Error::from_string_literal("JBIG2Writer: Table range length too large for bit count");
        TRY(bit_stream.write_bits<u8>(tables.entries[i].range_length, range_bit_count));
        TRY(range_lengths.try_append(tables.entries[i].range_length));

        // "c) Set:
        //         RANGELOW[NTEMP] = CURRANGELOW
        //         CURRANGELOW = CURRANGELOW + 2 ** RANGELEN[NTEMP]
        //         NTEMP = NTEMP + 1"
        TRY(range_lows.try_append(value));
        value += 1 << tables.entries[i].range_length;
        i++;

        // "d) If CURRANGELOW ≥ HTHIGH then proceed to step 6)."
    } while (value < tables.highest_value);

    if (i != tables.entries.size())
        return Error::from_string_literal("JBIG2Writer: Too many table entries");

    // "6) Read HTPS bits. Let LOWPREFLEN be the value read."
    // "7) [...] This is the lower range table line for this table."
    TRY(write_prefix_length(tables.lower_range_prefix_length));
    TRY(prefix_lengths.try_append(tables.lower_range_prefix_length));
    TRY(range_lengths.try_append(32));
    TRY(range_lows.try_append(tables.lowest_value - 1));

    // "8) Read HTPS bits. Let HIGHPREFLEN be the value read."
    // "9) [...] This is the upper range table line for this table."
    TRY(write_prefix_length(tables.upper_range_prefix_length));
    TRY(prefix_lengths.try_append(tables.upper_range_prefix_length));
    TRY(range_lengths.try_append(32));
    TRY(range_lows.try_append(tables.highest_value));

    // "10) If HTOOB is 1, then:"
    if (has_out_of_band) {
        // "a) Read HTPS bits. Let OOBPREFLEN be the value read."
        TRY(write_prefix_length(tables.out_of_band_prefix_length));
        TRY(prefix_lengths.try_append(tables.out_of_band_prefix_length));
        TRY(range_lengths.try_append(0));
        TRY(range_lows.try_append(OptionalNone {}));
    }

    TRY(bit_stream.align_to_byte_boundary());

    TRY(scratch_buffer.try_extend(TRY(output_stream.read_until_eof())));

    // "11) Create the prefix codes using the algorithm described in B.3."
    auto codes = TRY(JBIG2::assign_huffman_codes(prefix_lengths));

    Vector<JBIG2::Code> table_codes;
    for (auto const& [i, length] : enumerate(prefix_lengths)) {
        if (length == 0)
            continue;

        JBIG2::Code code { .prefix_length = length, .range_length = range_lengths[i], .first_value = range_lows[i], .code = codes[i] };
        if (i == prefix_lengths.size() - (has_out_of_band ? 3 : 2))
            code.prefix_length |= JBIG2::Code::LowerRangeBit;
        table_codes.append(code);
    }

    if (context.codes_by_segment_id.set(header.segment_number, move(table_codes)) != HashSetResult::InsertedNewEntry)
        return Error::from_string_literal("JBIG2Writer: Duplicate table segment ID");
    JBIG2::HuffmanTable table { context.codes_by_segment_id.get(header.segment_number).value().span(), has_out_of_band };
    VERIFY(context.tables_by_segment_id.set(header.segment_number, table) == HashSetResult::InsertedNewEntry);

    return {};
}

static ErrorOr<void> encode_extension(JBIG2::ExtensionData const& extension, Vector<u8>& scratch_buffer)
{
    // 7.4.14 Extension segment syntax
    AllocatingMemoryStream output_stream;
    TRY(output_stream.write_value<BigEndian<u32>>(to_underlying(extension.type)));

    switch (extension.type) {
    case JBIG2::ExtensionType::SingleByteCodedComment:
        // 7.4.15.1 Single-byte coded comment
        // Pairs of zero-terminated ISO/IEC 8859-1 (latin1) pairs, terminated by another \0.
        for (auto const& entry : extension.entries) {
            auto write_iso_8859_1_string = [&](StringView string) -> ErrorOr<void> {
                auto encoder = TextCodec::encoder_for_exact_name("ISO-8859-1"sv);
                TRY(encoder->process(
                    Utf8View(string),
                    [&](u8 byte) -> ErrorOr<void> {
                        return output_stream.write_value<u8>(byte);
                    },
                    [&](u32) -> ErrorOr<void> {
                        return Error::from_string_literal("JBIG2Writer: Cannot encode character in ISO-8859-1");
                    }));
                TRY(output_stream.write_value<u8>(0));
                return {};
            };
            TRY(write_iso_8859_1_string(entry.key));
            TRY(write_iso_8859_1_string(entry.value));
        }
        TRY(output_stream.write_value<u8>(0));
        break;

    case JBIG2::ExtensionType::MultiByteCodedComment:
        // 7.4.15.2 Multi-byte coded comment
        // Pairs of (two-byte-)zero-terminated UCS-2 pairs, terminated by another \0\0.
        for (auto const& entry : extension.entries) {
            auto write_ucs2_string = [&](StringView string) -> ErrorOr<void> {
                auto ucs2_string = TRY(utf8_to_utf16(string));
                for (size_t i = 0; i < ucs2_string.size(); ++i) {
                    if (is_unicode_surrogate(ucs2_string[i]))
                        return Error::from_string_literal("JBIG2Writer: Cannot encode surrogate in UCS-2 string");
                    TRY(output_stream.write_value<BigEndian<u16>>(ucs2_string[i]));
                }
                TRY(output_stream.write_value<BigEndian<u16>>(0));
                return {};
            };
            TRY(write_ucs2_string(entry.key));
            TRY(write_ucs2_string(entry.value));
        }
        TRY(output_stream.write_value<BigEndian<u16>>(0));
        break;
    }

    TRY(scratch_buffer.try_extend(TRY(output_stream.read_until_eof())));
    return {};
}

static ErrorOr<SerializedSegmentData> encode_segment(JBIG2::SegmentData const& segment_data, JBIG2EncodingContext& context)
{
    Vector<u8> scratch_buffer;

    auto encoded_data = TRY(segment_data.data.visit(
        [&scratch_buffer, &segment_data, &context](JBIG2::SymbolDictionarySegmentData const& symbol_dictionary) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_symbol_dictionary(symbol_dictionary, segment_data.header, context, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &context](JBIG2::ImmediateTextRegionSegmentData const& text_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_text_region(text_region_wrapper.text_region, segment_data.header, context, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &context](JBIG2::ImmediateLosslessTextRegionSegmentData const& text_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_text_region(text_region_wrapper.text_region, segment_data.header, context, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer](JBIG2::PatternDictionarySegmentData const& pattern_dictionary) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_pattern_dictionary(pattern_dictionary, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &context](JBIG2::ImmediateHalftoneRegionSegmentData const& halftone_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_halftone_region(halftone_region_wrapper.halftone_region, segment_data.header, context, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &context](JBIG2::ImmediateLosslessHalftoneRegionSegmentData const& halftone_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_halftone_region(halftone_region_wrapper.halftone_region, segment_data.header, context, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer](JBIG2::ImmediateGenericRegionSegmentData const& generic_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_region(generic_region_wrapper.generic_region, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer](JBIG2::ImmediateLosslessGenericRegionSegmentData const& generic_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_region(generic_region_wrapper.generic_region, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer](JBIG2::IntermediateGenericRegionSegmentData const& generic_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_region(generic_region_wrapper.generic_region, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &context](JBIG2::ImmediateGenericRefinementRegionSegmentData const& generic_refinement_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_refinement_region(generic_refinement_region_wrapper.generic_refinement_region, segment_data.header, context, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &context](JBIG2::ImmediateLosslessGenericRefinementRegionSegmentData const& generic_refinement_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_refinement_region(generic_refinement_region_wrapper.generic_refinement_region, segment_data.header, context, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &context](JBIG2::IntermediateGenericRefinementRegionSegmentData const& generic_refinement_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_refinement_region(generic_refinement_region_wrapper.generic_refinement_region, segment_data.header, context, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer](JBIG2::PageInformationSegment const& page_information) -> ErrorOr<ReadonlyBytes> {
            TRY(scratch_buffer.try_resize(sizeof(JBIG2::PageInformationSegment)));
            FixedMemoryStream stream { scratch_buffer, FixedMemoryStream::Mode::ReadWrite };
            TRY(encode_page_information_data(stream, page_information));
            return scratch_buffer;
        },
        [](JBIG2::EndOfPageSegmentData const&) -> ErrorOr<ReadonlyBytes> {
            return ReadonlyBytes {};
        },
        [&scratch_buffer](JBIG2::EndOfStripeSegment const& end_of_stripe) -> ErrorOr<ReadonlyBytes> {
            TRY(scratch_buffer.try_resize(sizeof(JBIG2::EndOfStripeSegment)));
            FixedMemoryStream stream { scratch_buffer, FixedMemoryStream::Mode::ReadWrite };
            TRY(stream.write_value<BigEndian<u32>>(end_of_stripe.y_coordinate));
            return scratch_buffer;
        },
        [](JBIG2::EndOfFileSegmentData const&) -> ErrorOr<ReadonlyBytes> {
            return ReadonlyBytes {};
        },
        [&scratch_buffer, &segment_data, &context](JBIG2::TablesData const& tables) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_tables(tables, segment_data.header, context, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer](JBIG2::ExtensionData const& extension) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_extension(extension, scratch_buffer));
            return scratch_buffer;
        }));

    JBIG2::SegmentHeader header;
    header.segment_number = segment_data.header.segment_number;
    header.type = segment_data.data.visit(
        [](JBIG2::SymbolDictionarySegmentData const&) { return JBIG2::SegmentType::SymbolDictionary; },
        [](JBIG2::ImmediateTextRegionSegmentData const&) { return JBIG2::SegmentType::ImmediateTextRegion; },
        [](JBIG2::ImmediateLosslessTextRegionSegmentData const&) { return JBIG2::SegmentType::ImmediateLosslessTextRegion; },
        [](JBIG2::PatternDictionarySegmentData const&) { return JBIG2::SegmentType::PatternDictionary; },
        [](JBIG2::ImmediateHalftoneRegionSegmentData const&) { return JBIG2::SegmentType::ImmediateHalftoneRegion; },
        [](JBIG2::ImmediateLosslessHalftoneRegionSegmentData const&) { return JBIG2::SegmentType::ImmediateLosslessHalftoneRegion; },
        [](JBIG2::ImmediateGenericRegionSegmentData const&) { return JBIG2::SegmentType::ImmediateGenericRegion; },
        [](JBIG2::ImmediateLosslessGenericRegionSegmentData const&) { return JBIG2::SegmentType::ImmediateLosslessGenericRegion; },
        [](JBIG2::IntermediateGenericRegionSegmentData const&) { return JBIG2::SegmentType::IntermediateGenericRegion; },
        [](JBIG2::ImmediateGenericRefinementRegionSegmentData const&) { return JBIG2::SegmentType::ImmediateGenericRefinementRegion; },
        [](JBIG2::ImmediateLosslessGenericRefinementRegionSegmentData const&) { return JBIG2::SegmentType::ImmediateLosslessGenericRefinementRegion; },
        [](JBIG2::IntermediateGenericRefinementRegionSegmentData const&) { return JBIG2::SegmentType::IntermediateGenericRefinementRegion; },
        [](JBIG2::PageInformationSegment const&) { return JBIG2::SegmentType::PageInformation; },
        [](JBIG2::EndOfPageSegmentData const&) { return JBIG2::SegmentType::EndOfPage; },
        [](JBIG2::EndOfStripeSegment const&) { return JBIG2::SegmentType::EndOfStripe; },
        [](JBIG2::EndOfFileSegmentData const&) { return JBIG2::SegmentType::EndOfFile; },
        [](JBIG2::TablesData const&) { return JBIG2::SegmentType::Tables; },
        [](JBIG2::ExtensionData const&) { return JBIG2::SegmentType::Extension; });
    header.retention_flag = segment_data.header.retention_flag;
    for (auto const& reference : segment_data.header.referred_to_segments) {
        header.referred_to_segment_numbers.append(reference.segment_number);
        header.referred_to_segment_retention_flags.append(reference.retention_flag);
    }
    header.page_association = segment_data.header.page_association;
    header.data_length = segment_data.header.is_immediate_generic_region_of_initially_unknown_size ? 0xffff'ffff : encoded_data.size();

    auto page_association_size = segment_data.header.force_32_bit_page_association ? PageAssociationSize::Force32Bit : PageAssociationSize::Auto;
    AllocatingMemoryStream header_stream;
    TRY(encode_segment_header(header_stream, header, page_association_size));
    auto header_data = TRY(header_stream.read_until_eof());

    SerializedSegmentData data;
    data.header_size = header_data.size();
    data.data = TRY(ByteBuffer::create_uninitialized(header_data.size() + encoded_data.size()));
    header_data.span().copy_to(data.data.span());
    encoded_data.copy_to(data.data.span().slice(header_data.size()));

    return data;
}

ErrorOr<void> JBIG2Writer::encode_with_explicit_data(Stream& stream, JBIG2::FileData const& file_data)
{
    if (file_data.header.organization == JBIG2::Organization::Embedded)
        return Error::from_string_literal("JBIG2Writer: Can only encode sequential or random-access files");

    TRY(encode_jbig2_header(stream, file_data.header));

    JBIG2EncodingContext context { file_data.segments };
    for (auto const& segment : file_data.segments) {
        if (context.segment_by_id.set(segment.header.segment_number, &segment) != HashSetResult::InsertedNewEntry)
            return Error::from_string_literal("JBIG2Writer: Duplicate segment number");
    }

    for (auto const& segment : file_data.segments) {
        auto data = TRY(encode_segment(segment, context));
        VERIFY(context.segment_data_by_id.set(segment.header.segment_number, data) == HashSetResult::InsertedNewEntry);
    }

    if (file_data.header.organization == JBIG2::Organization::Sequential) {
        for (auto const& segment : file_data.segments) {
            auto const& data = context.segment_data_by_id.get(segment.header.segment_number);
            TRY(stream.write_until_depleted(data->data));
        }
        return {};
    }

    VERIFY(file_data.header.organization == JBIG2::Organization::RandomAccess);
    for (auto const& segment : file_data.segments) {
        auto const& data = context.segment_data_by_id.get(segment.header.segment_number);
        TRY(stream.write_until_depleted(data->data.span().slice(0, data->header_size)));
    }
    for (auto const& segment : file_data.segments) {
        auto const& data = context.segment_data_by_id.get(segment.header.segment_number);
        TRY(stream.write_until_depleted(data->data.span().slice(data->header_size)));
    }

    return {};
}

}
