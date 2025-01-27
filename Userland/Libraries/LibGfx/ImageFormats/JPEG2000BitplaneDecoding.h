/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/ImageFormats/JPEG2000Loader.h>
#include <LibGfx/ImageFormats/JPEG2000Span2D.h>
#include <LibGfx/ImageFormats/QMArithmeticDecoder.h>

namespace Gfx::JPEG2000 {

inline ErrorOr<void> decode_code_block(Span2D<i16> result, SubBand sub_band, int number_of_coding_passes, ReadonlyBytes data, int M_b, int p)
{
    // This is an implementation of the bitplane decoding algorithm described in Annex D of the JPEG2000 spec.
    // It's modeled closely after Figure D.3 – Flow chart for all coding passes on a code-block bit-plane,
    // and is currently not written for performance.
    // It assumes that data from all layers of a code-block have been concatenated into a single buffer.

    int const w = result.size.width();
    int const h = result.size.height();
    int const num_strips = ceil_div(h, 4);

    // Decoder state.

    // State per coefficient:
    // - significance (1 bit)
    // - sign (1 bit)
    // - magnitude (technically up to 38 bits, but we only store 16)
    // Store this as:
    // - u8 with 2 bits for significance and sign for four vertically-adjacent coefficients
    // - One u16 per coefficient for magnitude

    // Stores 1 bit significance and 1 bit sign for the 4 pixels in a vertical strip.
    Vector<u8> significance_and_sign;
    TRY(significance_and_sign.try_resize(w * num_strips));

    Vector<u16> magnitudes;
    TRY(magnitudes.try_resize(w * h));

    // Stores bit index where the coefficient became significant.
    Vector<u8> became_significant_at_bitplane;
    TRY(became_significant_at_bitplane.try_resize(w * h));

    // Stores the pass the coefficient was coded in, even if it was coded as "not yet significant".
    // Will always be a significance propagation pass.
    Vector<u8> was_coded_in_pass;
    TRY(was_coded_in_pass.try_resize(w * h));

    // B.10.5 Zero bit-plane information
    // "the number of missing most significant bit-planes, P, may vary from code-block to code-block; these missing bit-planes are all taken to be zero."
    int current_bitplane { p };

    int pass { 0 };

    QMArithmeticDecoder::Context uniform_context;
    QMArithmeticDecoder::Context run_length_context;
    Array<QMArithmeticDecoder::Context, 17> all_other_contexts {};

    QMArithmeticDecoder arithmetic_decoder = TRY(QMArithmeticDecoder::initialize(data));

    auto reset_contexts = [&]() {
        // Table D.7 – Initial states for all contexts
        uniform_context = { 46, 0 };
        run_length_context = { 3, 0 };
        for (auto& context : all_other_contexts)
            context = { 0, 0 };
        all_other_contexts[0] = { 4, 0 }; // "All zero neighbours"
    };
    reset_contexts();

    // State setters and getters.

    auto is_significant = [&](int x, int y) {
        if (x < 0 || x >= w || y < 0 || y >= h)
            return false;
        auto strip_index = y / 4;
        auto strip_y = y % 4;
        auto strip_offset = strip_index * w;
        auto strip_value = significance_and_sign[strip_offset + x];
        return (strip_value & (1 << strip_y)) != 0;
    };
    auto sign_is_negative = [&](int x, int y) {
        auto strip_index = y / 4;
        auto strip_y = y % 4;
        auto strip_offset = strip_index * w;
        auto strip_value = significance_and_sign[strip_offset + x];
        return (strip_value & (1 << (strip_y + 4))) != 0;
    };

    auto set_significant = [&](int x, int y, bool value) {
        auto strip_index = y / 4;
        auto strip_y = y % 4;
        auto strip_offset = strip_index * w;
        auto strip_value = significance_and_sign[strip_offset + x];
        if (value)
            strip_value |= 1 << strip_y;
        else
            strip_value &= ~(1 << strip_y);
        significance_and_sign[strip_offset + x] = strip_value;
    };
    auto set_sign = [&](int x, int y, bool is_negative) {
        auto strip_index = y / 4;
        auto strip_y = y % 4;
        auto strip_offset = strip_index * w;
        auto strip_value = significance_and_sign[strip_offset + x];
        if (is_negative)
            strip_value |= 1 << (strip_y + 4);
        else
            strip_value &= ~(1 << (strip_y + 4));
        significance_and_sign[strip_offset + x] = strip_value;
    };

    // Helper functions, mostly for computing arithmetic decoder contexts in various situations.

    auto compute_context_ll_lh = [&](int x, int y) -> unsigned {
        // Table D.1 – Contexts for the significance propagation and cleanup coding passes
        u8 sum_h = is_significant(x - 1, y) + is_significant(x + 1, y);
        u8 sum_v = is_significant(x, y - 1) + is_significant(x, y + 1);
        u8 sum_d = is_significant(x - 1, y - 1) + is_significant(x - 1, y + 1) + is_significant(x + 1, y - 1) + is_significant(x + 1, y + 1);

        if (sum_h == 2)
            return 8;

        if (sum_h == 1) {
            if (sum_v >= 1)
                return 7;
            if (sum_d >= 1)
                return 6;
            return 5;
        }

        if (sum_v == 2)
            return 4;
        if (sum_v == 1)
            return 3;
        if (sum_d >= 2)
            return 2;
        if (sum_d == 1)
            return 1;

        return 0;
    };

    // Like compute_context_ll_lh but with sum_h and sum_v swapped
    auto compute_context_hl = [&](int x, int y) -> unsigned {
        // Table D.1 – Contexts for the significance propagation and cleanup coding passes
        u8 sum_h = is_significant(x - 1, y) + is_significant(x + 1, y);
        u8 sum_v = is_significant(x, y - 1) + is_significant(x, y + 1);
        u8 sum_d = is_significant(x - 1, y - 1) + is_significant(x - 1, y + 1) + is_significant(x + 1, y - 1) + is_significant(x + 1, y + 1);

        if (sum_v == 2)
            return 8;

        if (sum_v == 1) {
            if (sum_h >= 1)
                return 7;
            if (sum_d >= 1)
                return 6;
            return 5;
        }

        if (sum_h == 2)
            return 4;
        if (sum_h == 1)
            return 3;
        if (sum_d >= 2)
            return 2;
        if (sum_d == 1)
            return 1;

        return 0;
    };

    auto compute_context_hh = [&](int x, int y) -> unsigned {
        // Table D.1 – Contexts for the significance propagation and cleanup coding passes
        u8 sum_h = is_significant(x - 1, y) + is_significant(x + 1, y);
        u8 sum_v = is_significant(x, y - 1) + is_significant(x, y + 1);
        u8 sum_h_v = sum_h + sum_v;
        u8 sum_d = is_significant(x - 1, y - 1) + is_significant(x - 1, y + 1) + is_significant(x + 1, y - 1) + is_significant(x + 1, y + 1);

        if (sum_d >= 3)
            return 8;

        if (sum_d == 2) {
            if (sum_h_v >= 1)
                return 7;
            return 6;
        }

        if (sum_d == 1) {
            if (sum_h_v >= 2)
                return 5;
            if (sum_h_v == 1)
                return 4;
            return 3;
        }

        if (sum_h_v >= 2)
            return 2;
        if (sum_h_v == 1)
            return 1;

        return 0;
    };

    auto compute_context = [&](int x, int y) -> unsigned {
        switch (sub_band) {
        case SubBand::HorizontalLowpassVerticalLowpass:
        case SubBand::HorizontalLowpassVerticalHighpass:
            return compute_context_ll_lh(x, y);
        case SubBand::HorizontalHighpassVerticalLowpass:
            return compute_context_hl(x, y);
        case SubBand::HorizontalHighpassVerticalHighpass:
            return compute_context_hh(x, y);
        }
        VERIFY_NOT_REACHED();
    };

    auto v_or_h_contribution = [&](IntPoint p, IntPoint d0, IntPoint d1) -> i8 {
        auto p0 = p + d0;
        auto p1 = p + d1;
        // Table D.2 – Contributions of the vertical (and the horizontal) neighbours to the sign context
        if (is_significant(p1.x(), p1.y())) {
            if (!sign_is_negative(p1.x(), p1.y())) {
                if (is_significant(p0.x(), p0.y()))
                    return !sign_is_negative(p0.x(), p0.y()) ? 1 : 0;
                return 1;
            }
            if (is_significant(p0.x(), p0.y()))
                return !sign_is_negative(p0.x(), p0.y()) ? 0 : -1;
            return -1;
        }
        if (is_significant(p0.x(), p0.y()))
            return !sign_is_negative(p0.x(), p0.y()) ? 1 : -1;
        return 0;
    };

    auto read_sign_bit = [&](int x, int y) {
        // C2, Decode sign bit of current coefficient
        // Sign bit
        // D.3.2 Sign bit decoding
        // Table D.2 – Contributions of the vertical (and the horizontal) neighbours to the sign context
        i8 v_contribution = v_or_h_contribution({ x, y }, { 0, -1 }, { 0, 1 });
        i8 h_contribution = v_or_h_contribution({ x, y }, { -1, 0 }, { 1, 0 });
        // Table D.3 – Sign contexts from the vertical and horizontal contributions
        u8 context_label = 9;
        if (h_contribution == 0)
            context_label += abs(v_contribution);
        else
            context_label += 3 + h_contribution * v_contribution;
        u8 xor_bit = 0;
        if (h_contribution == -1 || (h_contribution == 0 && v_contribution == -1))
            xor_bit = 1;
        bool sign_bit = arithmetic_decoder.get_next_bit(all_other_contexts[context_label]) ^ xor_bit;
        return sign_bit;
    };

    // Actual decoding algorithm, mostly based on section D.8 Flow diagram of the code-block coding,
    // in particular:
    // Figure D.3 – Flow chart for all coding passes on a code-block bit-plane
    // Table D.10 – Decisions in the context model flow chart
    // Table D.11 – Decoding in the context model flow chart

    int num_bits = M_b - 1; // Spec indexes i starting 1, we (morally) start current_bitplane at 0.

    auto significance_propagation_pass = [&](int current_bitplane, int pass) {
        // D.3.1 Significance propagation decoding pass,
        // and "Start of significance propagation" part of Figure D.3 – Flow chart for all coding passes on a code-block bit-plane.
        for (int y = 0; y < h; y += 4) {
            int y_end = min(y + 4, h);
            int num_rows = y_end - y;
            for (int x = 0; x < w; ++x) {
                for (u8 coefficient_index = 0; coefficient_index < num_rows; ++coefficient_index) {
                    // D1, Is the current coefficient significant?
                    if (!is_significant(x, y + coefficient_index)) {
                        // D2, Is the context bin zero? (see Table D.1)
                        u8 context = compute_context(x, y + coefficient_index);
                        if (context != 0) {
                            // C1, Decode significance bit of current coefficient (See D.3.1)
                            bool is_newly_significant = arithmetic_decoder.get_next_bit(all_other_contexts[context]);
                            set_significant(x, y + coefficient_index, is_newly_significant);
                            if (is_newly_significant) {
                                became_significant_at_bitplane[(y + coefficient_index) * w + x] = current_bitplane;
                                magnitudes[(y + coefficient_index) * w + x] |= 1 << (num_bits - current_bitplane);
                            }
                            was_coded_in_pass[(y + coefficient_index) * w + x] = pass;

                            // D3, Did the current coefficient just become significant?
                            if (is_newly_significant) {
                                bool sign_bit = read_sign_bit(x, y + coefficient_index);
                                set_sign(x, y + coefficient_index, sign_bit);
                            }
                        }
                    }
                    // D4, Are there more coefficients in the significance propagation?
                    // C0, Go to the next coefficient or column
                }
            }
        }
    };

    auto magnitude_refinement_pass = [&](int current_bitplane) {
        // D.3.3 Magnitude refinement pass,
        // and "Start of magnitude refinement pass" part of Figure D.3 – Flow chart for all coding passes on a code-block bit-plane.
        for (int y = 0; y < h; y += 4) {
            int y_end = min(y + 4, h);
            int num_rows = y_end - y;
            // PERF: Maybe store a "is any pixel significant in this scanline" flag to skip entire scanlines?
            for (int x = 0; x < w; ++x) {
                for (u8 coefficient_index = 0; coefficient_index < num_rows; ++coefficient_index) {
                    // D5, Is the coefficient insignificant?
                    if (!is_significant(x, y + coefficient_index))
                        continue;

                    // D6, Was the coefficient coded in the last significance propagation?
                    if (became_significant_at_bitplane[(y + coefficient_index) * w + x] != current_bitplane) {
                        // C3, Decode magnitude refinement pass bit of current coefficient
                        // Table D.4 – Contexts for the magnitude refinement coding passes
                        u8 context;
                        if (became_significant_at_bitplane[(y + coefficient_index) * w + x] == current_bitplane - 1) {
                            u8 sum_h = is_significant(x - 1, y + coefficient_index) + is_significant(x + 1, y + coefficient_index);
                            u8 sum_v = is_significant(x, y + coefficient_index - 1) + is_significant(x, y + coefficient_index + 1);
                            u8 sum_d = is_significant(x - 1, y + coefficient_index - 1) + is_significant(x - 1, y + coefficient_index + 1) + is_significant(x + 1, y + coefficient_index - 1) + is_significant(x + 1, y + coefficient_index + 1);
                            context = (sum_h + sum_v + sum_d) >= 1 ? 15 : 14;
                        } else {
                            context = 16;
                        }
                        bool magnitude_bit = arithmetic_decoder.get_next_bit(all_other_contexts[context]);
                        magnitudes[(y + coefficient_index) * w + x] |= magnitude_bit << (num_bits - current_bitplane);
                    }

                    // D7, Are there more coefficients in the magnitude refinement pass?
                    // C0, Go to the next coefficient or column
                }
            }
        }
    };

    auto cleanup_pass = [&](int current_bitplane, int pass) {
        // D.3.4 Cleanup pass,
        // and "Start of cleanup pass" part of Figure D.3 – Flow chart for all coding passes on a code-block bit-plane.
        // PERF: Have a "everything is significant" bit and skip this pass when it's set?
        for (int y = 0; y < h; y += 4) {
            int y_end = min(y + 4, h);
            int num_rows = y_end - y;
            for (int x = 0; x < w; ++x) {
                Array<u8, 4> contexts {};
                int num_undecoded = 0;
                for (int i = 0; i < 4; ++i) {
                    contexts[i] = compute_context(x, y + i);
                    if (!is_significant(x, y + i))
                        ++num_undecoded; // FIXME: This is probably redundant since this would imply a context being non-0.
                }

                // D8, Are four contiguous undecoded coefficients in a column each with a 0 context?, See D.3.4
                bool are_four_contiguous_undecoded_coefficients_in_a_column_each_with_a_0_context = num_rows == 4 && num_undecoded == 4 && (contexts[0] + contexts[1] + contexts[2] + contexts[3] == 0);
                if (are_four_contiguous_undecoded_coefficients_in_a_column_each_with_a_0_context) {
                    // C4, Run-length context label
                    auto not_four_zeros = arithmetic_decoder.get_next_bit(run_length_context);

                    // D11, Are the four contiguous bits all zero?
                    bool are_the_four_contiguous_bits_all_zero = !not_four_zeros;
                    if (!are_the_four_contiguous_bits_all_zero) {
                        // C5
                        u8 first_coefficient_index = arithmetic_decoder.get_next_bit(uniform_context);
                        first_coefficient_index = (first_coefficient_index << 1) | arithmetic_decoder.get_next_bit(uniform_context);
                        u8 coefficient_index = first_coefficient_index;

                        bool is_first_coefficient = true;
                        bool is_current_coefficient_significant = true;
                        set_significant(x, y + coefficient_index, true);
                        became_significant_at_bitplane[(y + coefficient_index) * w + x] = current_bitplane;
                        magnitudes[(y + coefficient_index) * w + x] |= 1 << (num_bits - current_bitplane);

                        do {
                            if (!is_first_coefficient) {
                                // C0, Go to the next coefficient or column
                                ++coefficient_index;

                                // C1, Decode significance bit of current coefficient (See D.3.1)
                                u8 context = compute_context(x, y + coefficient_index); // PERF: could use `contexts` cache (needs invalidation then).
                                bool is_newly_significant = arithmetic_decoder.get_next_bit(all_other_contexts[context]);
                                is_current_coefficient_significant = is_newly_significant;
                                set_significant(x, y + coefficient_index, is_newly_significant);
                                if (is_newly_significant) {
                                    became_significant_at_bitplane[(y + coefficient_index) * w + x] = current_bitplane;
                                    magnitudes[(y + coefficient_index) * w + x] |= 1 << (num_bits - current_bitplane);
                                }
                            }
                            is_first_coefficient = false;

                            // D3, Did the current coefficient just become significant?
                            if (is_current_coefficient_significant) {
                                bool sign_bit = read_sign_bit(x, y + coefficient_index);
                                set_sign(x, y + coefficient_index, sign_bit);
                            }

                            // D10, Are there more coefficients remaining of the four column coefficients?
                        } while (coefficient_index + 1 < num_rows);
                    }
                } else {
                    u8 coefficient_index = 0;
                    bool is_first_coefficient = true;
                    do {
                        if (!is_first_coefficient) {
                            // C0, Go to the next coefficient or column
                            ++coefficient_index;
                        }
                        is_first_coefficient = false;

                        // D9, Is the coefficient significant or has the bit already been coded during the Significance Propagation coding pass?
                        // Note: The significance propagation pass is pretty similar to this loop here.
                        bool is_significant_or_coded = is_significant(x, y + coefficient_index);
                        bool has_already_been_coded = pass > 0 && was_coded_in_pass[(y + coefficient_index) * w + x] == pass - 2;
                        if (!is_significant_or_coded && !has_already_been_coded) {
                            // C1, Decode significance bit of current coefficient
                            u8 context = compute_context(x, y + coefficient_index); // PERF: could use `contexts` cache (needs invalidation then).
                            bool is_newly_significant = arithmetic_decoder.get_next_bit(all_other_contexts[context]);
                            set_significant(x, y + coefficient_index, is_newly_significant);
                            if (is_newly_significant) {
                                became_significant_at_bitplane[(y + coefficient_index) * w + x] = current_bitplane;
                                magnitudes[(y + coefficient_index) * w + x] |= 1 << (num_bits - current_bitplane);
                            }

                            // D3, Did the current coefficient just become significant?
                            if (is_newly_significant) {
                                bool sign_bit = read_sign_bit(x, y + coefficient_index);
                                set_sign(x, y + coefficient_index, sign_bit);
                            }
                        }
                        // D10, Are there more coefficients remaining of the four column coefficients?
                    } while (coefficient_index + 1 < num_rows);
                }
                // D12, Are there more coefficients in the cleanup pass?
                // C0, Go to the next coefficient or column
                // (Both done by loop.)
            }
        }
    };

    for (; pass < number_of_coding_passes && current_bitplane < M_b; ++pass) {
        // D0, Is this the first bit-plane for the code-block?
        switch ((pass + 2) % 3) {
        case 0:
            significance_propagation_pass(current_bitplane, pass);
            break;
        case 1:
            magnitude_refinement_pass(current_bitplane);
            break;
        case 2:
            cleanup_pass(current_bitplane, pass);
            ++current_bitplane;
            break;
        }

        // FIXME: Optionally reset contexts between bitplanes, depending on uses_termination_on_each_coding_pass().
    }

    // Convert internal state to output.
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            auto magnitude = magnitudes[y * w + x];
            auto value = magnitude * (sign_is_negative(x, y) ? -1 : 1);
            result.data[y * result.pitch + x] = value;
        }
    }

    return {};
}

}
