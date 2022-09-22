/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <LibVideo/DecoderError.h>

#include "Parser.h"

namespace Video::VP9 {

class Decoder {
    friend class Parser;

public:
    Decoder();
    DecoderErrorOr<void> decode_frame(ByteBuffer const&);
    void dump_frame_info();

    // FIXME: These functions should be replaced by a struct that contains
    //        all the information needed to display a frame.
    Vector<u16> const& get_output_buffer_for_plane(u8 plane) const;
    Gfx::Size<size_t> get_y_plane_size();
    bool get_uv_subsampling_y();
    bool get_uv_subsampling_x();

private:
    typedef i32 Intermediate;

    DecoderErrorOr<void> allocate_buffers();
    Vector<Intermediate>& get_temp_buffer(u8 plane);
    Vector<u16>& get_output_buffer(u8 plane);

    /* (8.4) Probability Adaptation Process */
    u8 merge_prob(u8 pre_prob, u8 count_0, u8 count_1, u8 count_sat, u8 max_update_factor);
    u8 merge_probs(int const* tree, int index, u8* probs, u8* counts, u8 count_sat, u8 max_update_factor);
    DecoderErrorOr<void> adapt_coef_probs();
    DecoderErrorOr<void> adapt_non_coef_probs();
    void adapt_probs(int const* tree, u8* probs, u8* counts);
    u8 adapt_prob(u8 prob, u8 counts[2]);

    /* (8.5) Prediction Processes */
    // (8.5.1) Intra prediction process
    DecoderErrorOr<void> predict_intra(u8 plane, u32 x, u32 y, bool have_left, bool have_above, bool not_on_right, TXSize tx_size, u32 block_index);

    // (8.5.1) Inter prediction process
    DecoderErrorOr<void> predict_inter(u8 plane, u32 x, u32 y, u32 width, u32 height, u32 block_index);
    // (8.5.2.1) Motion vector selection process
    MotionVector select_motion_vector(u8 plane, u8 ref_list, u32 block_index);
    // (8.5.2.2) Motion vector clamping process
    MotionVector clamp_motion_vector(u8 plane, MotionVector vector);
    // (8.5.2.3) Motion vector scaling process
    DecoderErrorOr<MotionVector> scale_motion_vector(u8 plane, u8 ref_list, u32 x, u32 y, MotionVector vector);
    // From (8.5.1) Inter prediction process, steps 2-5
    DecoderErrorOr<void> predict_inter_block(u8 plane, u8 ref_list, u32 x, u32 y, u32 width, u32 height, u32 block_index, Vector<u16>& buffer);

    /* (8.6) Reconstruction and Dequantization */

    // FIXME: These should be inline or constexpr
    u16 dc_q(u8 b);
    u16 ac_q(u8 b);
    // Returns the quantizer index for the current block
    u8 get_qindex();
    // Returns the quantizer value for the dc coefficient for a particular plane
    u16 get_dc_quant(u8 plane);
    // Returns the quantizer value for the ac coefficient for a particular plane
    u16 get_ac_quant(u8 plane);

    // (8.6.2) Reconstruct process
    DecoderErrorOr<void> reconstruct(u8 plane, u32 transform_block_x, u32 transform_block_y, TXSize transform_block_size);

    // (8.7) Inverse transform process
    DecoderErrorOr<void> inverse_transform_2d(Vector<Intermediate>& dequantized, u8 log2_of_block_size);

    // (8.7.1) 1D Transforms
    // (8.7.1.1) Butterfly functions

    inline i32 cos64(u8 angle);
    inline i32 sin64(u8 angle);
    // The function B( a, b, angle, 0 ) performs a butterfly rotation.
    inline void butterfly_rotation_in_place(Vector<Intermediate>& data, size_t index_a, size_t index_b, u8 angle, bool flip);
    // The function H( a, b, 0 ) performs a Hadamard rotation.
    inline void hadamard_rotation_in_place(Vector<Intermediate>& data, size_t index_a, size_t index_b, bool flip);
    // The function SB( a, b, angle, 0 ) performs a butterfly rotation.
    // Spec defines the source as array T, and the destination array as S.
    template<typename S, typename D>
    inline void butterfly_rotation(Vector<S>& source, Vector<D>& destination, size_t index_a, size_t index_b, u8 angle, bool flip);
    // The function SH( a, b ) performs a Hadamard rotation and rounding.
    // Spec defines the source array as S, and the destination array as T.
    template<typename S, typename D>
    inline void hadamard_rotation(Vector<S>& source, Vector<D>& destination, size_t index_a, size_t index_b);

    template<typename T>
    inline i32 round_2(T value, u8 bits);

    // Checks whether the value is representable by a signed integer with (8 + bit_depth) bits.
    inline bool check_intermediate_bounds(Intermediate value);

    // (8.7.1.10) This process does an in-place Walsh-Hadamard transform of the array T (of length 4).
    inline DecoderErrorOr<void> inverse_walsh_hadamard_transform(Vector<Intermediate>& data, u8 log2_of_block_size, u8 shift);

    // (8.7.1.2) Inverse DCT array permutation process
    inline DecoderErrorOr<void> inverse_discrete_cosine_transform_array_permutation(Vector<Intermediate>& data, u8 log2_of_block_size);
    // (8.7.1.3) Inverse DCT process
    inline DecoderErrorOr<void> inverse_discrete_cosine_transform(Vector<Intermediate>& data, u8 log2_of_block_size);

    // (8.7.1.4) This process performs the in-place permutation of the array T of length 2 n which is required as the first step of
    // the inverse ADST.
    inline void inverse_asymmetric_discrete_sine_transform_input_array_permutation(Vector<Intermediate>& data, Vector<Intermediate>& temp, u8 log2_of_block_size);
    // (8.7.1.5) This process performs the in-place permutation of the array T of length 2 n which is required before the final
    // step of the inverse ADST.
    inline void inverse_asymmetric_discrete_sine_transform_output_array_permutation(Vector<Intermediate>& data, Vector<Intermediate>& temp, u8 log2_of_block_size);

    // (8.7.1.6) This process does an in-place transform of the array T to perform an inverse ADST.
    inline void inverse_asymmetric_discrete_sine_transform_4(Vector<Intermediate>& data);
    // (8.7.1.7) This process does an in-place transform of the array T using a higher precision array S for intermediate
    // results.
    inline DecoderErrorOr<void> inverse_asymmetric_discrete_sine_transform_8(Vector<Intermediate>& data);
    // (8.7.1.8) This process does an in-place transform of the array T using a higher precision array S for intermediate
    // results.
    inline DecoderErrorOr<void> inverse_asymmetric_discrete_sine_transform_16(Vector<Intermediate>& data);
    // (8.7.1.9) This process performs an in-place inverse ADST process on the array T of size 2 n for 2 ≤ n ≤ 4.
    inline DecoderErrorOr<void> inverse_asymmetric_discrete_sine_transform(Vector<Intermediate>& data, u8 log2_of_block_size);

    /* (8.10) Reference Frame Update Process */
    DecoderErrorOr<void> update_reference_frames();

    NonnullOwnPtr<Parser> m_parser;

    struct {
        // FIXME: We may be able to consolidate some of these to reduce memory consumption.

        // FIXME: Create a new struct to store these buffers, specifying size and providing
        //        helper functions to get values at coordinates. All *_at(row, column)
        //        functions in Decoder.cpp and functions returning row * width + column
        //        should be replaced if possible.

        Vector<Intermediate> dequantized;
        Vector<Intermediate> row_or_column;

        // predict_intra
        Vector<Intermediate> above_row;
        Vector<Intermediate> left_column;
        Vector<Intermediate> predicted_samples;

        // transforms (dct, adst)
        Vector<Intermediate> transform_temp;
        Vector<i64> adst_temp;

        // predict_inter
        Vector<u16> inter_horizontal;
        Vector<u16> inter_predicted;
        Vector<u16> inter_predicted_compound;

        Vector<Intermediate> intermediate[3];
        Vector<u16> output[3];
    } m_buffers;
};

}
