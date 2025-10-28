/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// Spec: ITU-T_T_88__08_2018.pdf in the zip file here:
// https://www.itu.int/rec/T-REC-T.88-201808-I
// JBIG2Loader.cpp has many spec notes.

#include <AK/BitStream.h>
#include <AK/HashMap.h>
#include <AK/MemoryStream.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Stream.h>
#include <AK/Utf16View.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibGfx/ImageFormats/CCITTEncoder.h>
#include <LibGfx/ImageFormats/JBIG2Writer.h>
#include <LibGfx/ImageFormats/MQArithmeticCoder.h>
#include <LibTextCodec/Encoder.h>

namespace Gfx {

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
    // Implementor's note: What this is supposed to mean is that we have a bunch of independent contexts, and we pick the
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
    BilevelImage const* reference_bitmap { nullptr };                   // "GRREFERENCE" in spec.
    i32 reference_x_offset { 0 };                                       // "GRREFERENCEDX" in spec.
    i32 reference_y_offset { 0 };                                       // "GRREFERENCEDY" in spec.
    bool is_typical_prediction_used { false };                          // "TPGRON" in spec.
    Array<JBIG2::AdaptiveTemplatePixel, 2> adaptive_template_pixels {}; // "GRATX" / "GRATY" in spec.
    MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { MQArithmeticEncoder::Trailing7FFFHandling::Keep };
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
static ErrorOr<ByteBuffer> generic_refinement_region_encoding_procedure(GenericRefinementRegionEncodingInputParameters& inputs, MQArithmeticEncoder& encoder, RefinementContexts& contexts)
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
    constexpr auto compute_context_0 = [](ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, BilevelImage const& reference, int reference_x, int reference_y, BilevelImage const& buffer, int x, int y) -> u16 {
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
    constexpr auto compute_context_1 = [](ReadonlySpan<JBIG2::AdaptiveTemplatePixel>, BilevelImage const& reference, int reference_x, int reference_y, BilevelImage const& buffer, int x, int y) -> u16 {
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
            bool prediction = get_pixel(*inputs.reference_bitmap, x - inputs.reference_x_offset - 1, y - inputs.reference_y_offset - 1);
            for (int dy = -1; dy <= 1; ++dy)
                for (int dx = -1; dx <= 1; ++dx)
                    if (get_pixel(*inputs.reference_bitmap, x - inputs.reference_x_offset + dx, y - inputs.reference_y_offset + dy) != prediction)
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
                u16 context = compute_context(inputs.adaptive_template_pixels, *inputs.reference_bitmap, x - inputs.reference_x_offset, y - inputs.reference_y_offset, inputs.image, x, y);
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
                    u16 context = compute_context(inputs.adaptive_template_pixels, *inputs.reference_bitmap, x - inputs.reference_x_offset, y - inputs.reference_y_offset, inputs.image, x, y);
                    encoder.encode_bit(inputs.image.get_bit(x, y), contexts.contexts[context]);
                }
            }
        }
    }

    return encoder.finalize(inputs.trailing_7fff_handling);
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
        if (header.referred_to_segment_numbers.size() >= (1 << 28))
            return Error::from_string_literal("JBIG2Writer: Too many referred-to segments");
        u32 count_of_referred_to_segments = header.referred_to_segment_numbers.size();
        TRY(stream.write_value<BigEndian<u32>>(count_of_referred_to_segments | 7 << 28));

        LittleEndianOutputBitStream bit_stream { MaybeOwned { stream } };
        TRY(bit_stream.write_bits(header.retention_flag, 1));
        for (size_t i = 0; i < header.referred_to_segment_numbers.size(); ++i)
            TRY(bit_stream.write_bits(header.referred_to_segment_retention_flags[i], 1));
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

static ErrorOr<void> encode_symbol_dictionary(JBIG2::SymbolDictionarySegmentData const& symbol_dictionary, Vector<u8>& scratch_buffer)
{
    // 7.4.2 Symbol dictionary segment syntax
    bool uses_huffman_encoding = (symbol_dictionary.flags & 1) != 0;
    bool uses_refinement_or_aggregate_coding = (symbol_dictionary.flags & 2) != 0;
    u8 symbol_template = (symbol_dictionary.flags >> 10) & 3;
    u8 symbol_refinement_template = (symbol_dictionary.flags >> 12) & 1;

    u8 number_of_adaptive_template_pixels = 0;
    if (!uses_huffman_encoding)
        number_of_adaptive_template_pixels = symbol_template == 0 ? 4 : 1;
    u8 number_of_refinement_adaptive_template_pixels = (uses_refinement_or_aggregate_coding && symbol_refinement_template == 0) ? 2 : 0;

    ByteBuffer data;                    // FIXME: Fill.
    u32 number_of_exported_symbols = 0; // FIXME: Actual value.
    u32 number_of_new_symbols = 0;      // FIXME: Actual value.

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

    return Error::from_string_literal("JBIG2Writer: Symbol dictionary encoding is not yet fully implemented");
}

static ErrorOr<void> encode_text_region(JBIG2::TextRegionSegmentData const& text_region, JBIG2::SegmentHeaderData const&, HashMap<u32, JBIG2::SegmentData const*>&, Vector<u8>& scratch_buffer)
{
    // 7.4.3 Text region segment syntax
    bool uses_huffman_encoding = (text_region.flags & 1) != 0;
    bool uses_refinement_coding = (text_region.flags & 2) != 0;
    u8 refinement_template = (text_region.flags >> 15);
    u8 number_of_refinement_adaptive_template_pixels = (uses_refinement_coding && refinement_template == 0) ? 2 : 0;

    // FIXME: Get referred-to symbol dictionaries and tables off header.referred_to_segments.

    u32 number_of_symbol_instances = 0;          // FIXME: Actual value.
    ByteBuffer symbol_id_huffman_decoding_table; // FIXME: Fill.
    ByteBuffer data;                             // FIXME: Fill.

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

    return Error::from_string_literal("JBIG2Writer: Text region encoding is not yet fully implemented");
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

static ErrorOr<void> encode_halftone_region(JBIG2::HalftoneRegionSegmentData const& halftone_region, JBIG2::SegmentHeaderData const& header, HashMap<u32, JBIG2::SegmentData const*>& segment_by_id, Vector<u8>& scratch_buffer)
{
    // 7.4.5 Halftone region segment syntax
    if (header.referred_to_segments.size() != 1)
        return Error::from_string_literal("JBIG2Writer: Halftone region must refer to exactly one segment");

    auto maybe_segment = segment_by_id.get(header.referred_to_segments[0].segment_number);
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

    GrayscaleInputParameters inputs { .grayscale_image = halftone_region.grayscale_image, .skip_pattern = skip_pattern };
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

    TRY(scratch_buffer.try_resize(sizeof(JBIG2::RegionSegmentInformationField) + 1 + 2 * 4 * 2 * 2 + data.size()));
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

static ErrorOr<void> encode_generic_refinement_region(JBIG2::GenericRefinementRegionSegmentData const& generic_refinement_region, JBIG2::SegmentHeaderData const& header, HashMap<u32, JBIG2::SegmentData const*>& segment_by_id, Vector<u8>& scratch_buffer)
{
    // 7.4.7 Generic refinement region syntax
    if (header.referred_to_segments.size() > 1)
        return Error::from_string_literal("JBIG2Writer: Generic refinement region must refer to at most one segment");
    if (header.referred_to_segments.size() == 0)
        return Error::from_string_literal("JBIG2Writer: Generic refinement region refining page not yet implemented");

    auto maybe_segment = segment_by_id.get(header.referred_to_segments[0].segment_number);
    if (!maybe_segment.has_value())
        return Error::from_string_literal("JBIG2Writer: Could not find referred-to segment for generic refinement region");
    auto const& referred_to_segment = *maybe_segment.value();

    auto const* reference_bitmap = TRY(referred_to_segment.data.visit(
        [](JBIG2::IntermediateGenericRegionSegmentData const& generic_region_wrapper) -> ErrorOr<BilevelImage const*> {
            return generic_region_wrapper.generic_region.image;
        },
        [](JBIG2::IntermediateGenericRefinementRegionSegmentData const& generic_refinement_region_wrapper) -> ErrorOr<BilevelImage const*> {
            return generic_refinement_region_wrapper.generic_refinement_region.image;
        },
        [](auto const&) -> ErrorOr<BilevelImage const*> {
            return Error::from_string_literal("JBIG2Writer: Generic refinement region can only refer to intermediate region segments");
        }));

    GenericRefinementRegionEncodingInputParameters inputs { .image = *generic_refinement_region.image };
    inputs.gr_template = generic_refinement_region.flags & 1;
    inputs.reference_bitmap = reference_bitmap;
    inputs.is_typical_prediction_used = (generic_refinement_region.flags >> 1) & 1;
    inputs.adaptive_template_pixels = generic_refinement_region.adaptive_template_pixels;
    inputs.trailing_7fff_handling = generic_refinement_region.trailing_7fff_handling;
    RefinementContexts contexts { inputs.gr_template };
    MQArithmeticEncoder encoder = TRY(MQArithmeticEncoder::initialize(0));
    auto data = TRY(generic_refinement_region_encoding_procedure(inputs, encoder, contexts));

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

static ErrorOr<void> encode_tables(JBIG2::TablesData const& tables, Vector<u8>& scratch_buffer)
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
    do {
        if (i >= tables.entries.size())
            return Error::from_string_literal("JBIG2Writer: Not enough table entries");

        // "a) Read HTPS bits."
        TRY(write_prefix_length(tables.entries[i].prefix_length));

        // "b) Read HTRS bits."
        if (tables.entries[i].range_length >= (1 << range_bit_count))
            return Error::from_string_literal("JBIG2Writer: Table range length too large for bit count");
        TRY(bit_stream.write_bits<u8>(tables.entries[i].range_length, range_bit_count));

        // "c) Set:
        //         RANGELOW[NTEMP] = CURRANGELOW
        //         CURRANGELOW = CURRANGELOW + 2 ** RANGELEN[NTEMP]
        //         NTEMP = NTEMP + 1"
        value += 1 << tables.entries[i].range_length;
        i++;

        // "d) If CURRANGELOW ≥ HTHIGH then proceed to step 6)."
    } while (value < tables.highest_value);

    if (i != tables.entries.size())
        return Error::from_string_literal("JBIG2Writer: Too many table entries");

    // "6) Read HTPS bits. Let LOWPREFLEN be the value read."
    // "7) [...] This is the lower range table line for this table."
    TRY(write_prefix_length(tables.lower_range_prefix_length));

    // "8) Read HTPS bits. Let HIGHPREFLEN be the value read."
    // "9) [...] This is the upper range table line for this table."
    TRY(write_prefix_length(tables.upper_range_prefix_length));

    // "10) If HTOOB is 1, then:"
    if (has_out_of_band) {
        // "a) Read HTPS bits. Let OOBPREFLEN be the value read."
        TRY(write_prefix_length(tables.out_of_band_prefix_length));
    }

    TRY(bit_stream.align_to_byte_boundary());

    TRY(scratch_buffer.try_extend(TRY(output_stream.read_until_eof())));
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

static ErrorOr<void> encode_segment(Stream& stream, JBIG2::SegmentData const& segment_data, HashMap<u32, JBIG2::SegmentData const*>& segment_by_id)
{
    Vector<u8> scratch_buffer;

    auto encoded_data = TRY(segment_data.data.visit(
        [&scratch_buffer](JBIG2::SymbolDictionarySegmentData const& symbol_dictionary) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_symbol_dictionary(symbol_dictionary, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &segment_by_id](JBIG2::ImmediateTextRegionSegmentData const& text_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_text_region(text_region_wrapper.text_region, segment_data.header, segment_by_id, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &segment_by_id](JBIG2::ImmediateLosslessTextRegionSegmentData const& text_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_text_region(text_region_wrapper.text_region, segment_data.header, segment_by_id, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer](JBIG2::PatternDictionarySegmentData const& pattern_dictionary) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_pattern_dictionary(pattern_dictionary, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &segment_by_id](JBIG2::ImmediateHalftoneRegionSegmentData const& halftone_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_halftone_region(halftone_region_wrapper.halftone_region, segment_data.header, segment_by_id, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &segment_by_id](JBIG2::ImmediateLosslessHalftoneRegionSegmentData const& halftone_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_halftone_region(halftone_region_wrapper.halftone_region, segment_data.header, segment_by_id, scratch_buffer));
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
        [&scratch_buffer, &segment_data, &segment_by_id](JBIG2::ImmediateGenericRefinementRegionSegmentData const& generic_refinement_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_refinement_region(generic_refinement_region_wrapper.generic_refinement_region, segment_data.header, segment_by_id, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &segment_by_id](JBIG2::ImmediateLosslessGenericRefinementRegionSegmentData const& generic_refinement_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_refinement_region(generic_refinement_region_wrapper.generic_refinement_region, segment_data.header, segment_by_id, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer, &segment_data, &segment_by_id](JBIG2::IntermediateGenericRefinementRegionSegmentData const& generic_refinement_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_refinement_region(generic_refinement_region_wrapper.generic_refinement_region, segment_data.header, segment_by_id, scratch_buffer));
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
        [&scratch_buffer](JBIG2::TablesData const& tables) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_tables(tables, scratch_buffer));
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
    TRY(encode_segment_header(stream, header, page_association_size));
    TRY(stream.write_until_depleted(encoded_data));

    return {};
}

ErrorOr<void> JBIG2Writer::encode_with_explicit_data(Stream& stream, JBIG2::FileData const& file_data)
{
    if (file_data.header.organization != JBIG2::Organization::Sequential)
        return Error::from_string_literal("JBIG2Writer: Can only encode sequential files yet");

    TRY(encode_jbig2_header(stream, file_data.header));

    HashMap<u32, JBIG2::SegmentData const*> segment_by_id;
    for (auto const& segment : file_data.segments) {
        if (segment_by_id.set(segment.header.segment_number, &segment) != HashSetResult::InsertedNewEntry)
            return Error::from_string_literal("JBIG2Writer: Duplicate segment number");
    }

    for (auto const& segment : file_data.segments) {
        TRY(encode_segment(stream, segment, segment_by_id));
    }

    return {};
}

}
