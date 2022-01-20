/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCrypto/BigInt/UnsignedBigInteger.h>

namespace Crypto {

class UnsignedBigIntegerAlgorithms {
public:
    static void add_without_allocation(UnsignedBigInteger const& left, UnsignedBigInteger const& right, UnsignedBigInteger& output);
    static void add_into_accumulator_without_allocation(UnsignedBigInteger& accumulator, UnsignedBigInteger const& value);
    static void subtract_without_allocation(UnsignedBigInteger const& left, UnsignedBigInteger const& right, UnsignedBigInteger& output);
    static void bitwise_or_without_allocation(UnsignedBigInteger const& left, UnsignedBigInteger const& right, UnsignedBigInteger& output);
    static void bitwise_and_without_allocation(UnsignedBigInteger const& left, UnsignedBigInteger const& right, UnsignedBigInteger& output);
    static void bitwise_xor_without_allocation(UnsignedBigInteger const& left, UnsignedBigInteger const& right, UnsignedBigInteger& output);
    static void bitwise_not_fill_to_one_based_index_without_allocation(UnsignedBigInteger const& left, size_t, UnsignedBigInteger& output);
    static void shift_left_without_allocation(UnsignedBigInteger const& number, size_t bits_to_shift_by, UnsignedBigInteger& temp_result, UnsignedBigInteger& temp_plus, UnsignedBigInteger& output);
    static void multiply_without_allocation(UnsignedBigInteger const& left, UnsignedBigInteger const& right, UnsignedBigInteger& temp_shift_result, UnsignedBigInteger& temp_shift_plus, UnsignedBigInteger& temp_shift, UnsignedBigInteger& output);
    static void divide_without_allocation(UnsignedBigInteger const& numerator, UnsignedBigInteger const& denominator, UnsignedBigInteger& temp_shift_result, UnsignedBigInteger& temp_shift_plus, UnsignedBigInteger& temp_shift, UnsignedBigInteger& temp_minus, UnsignedBigInteger& quotient, UnsignedBigInteger& remainder);
    static void divide_u16_without_allocation(UnsignedBigInteger const& numerator, UnsignedBigInteger::Word denominator, UnsignedBigInteger& quotient, UnsignedBigInteger& remainder);

    static void destructive_GCD_without_allocation(UnsignedBigInteger& temp_a, UnsignedBigInteger& temp_b, UnsignedBigInteger& temp_1, UnsignedBigInteger& temp_2, UnsignedBigInteger& temp_3, UnsignedBigInteger& temp_4, UnsignedBigInteger& temp_quotient, UnsignedBigInteger& temp_remainder, UnsignedBigInteger& output);
    static void modular_inverse_without_allocation(UnsignedBigInteger const& a_, UnsignedBigInteger const& b, UnsignedBigInteger& temp_1, UnsignedBigInteger& temp_2, UnsignedBigInteger& temp_3, UnsignedBigInteger& temp_4, UnsignedBigInteger& temp_minus, UnsignedBigInteger& temp_quotient, UnsignedBigInteger& temp_d, UnsignedBigInteger& temp_u, UnsignedBigInteger& temp_v, UnsignedBigInteger& temp_x, UnsignedBigInteger& result);
    static void destructive_modular_power_without_allocation(UnsignedBigInteger& ep, UnsignedBigInteger& base, UnsignedBigInteger const& m, UnsignedBigInteger& temp_1, UnsignedBigInteger& temp_2, UnsignedBigInteger& temp_3, UnsignedBigInteger& temp_4, UnsignedBigInteger& temp_multiply, UnsignedBigInteger& temp_quotient, UnsignedBigInteger& temp_remainder, UnsignedBigInteger& result);
    static void montgomery_modular_power_with_minimal_allocations(UnsignedBigInteger const& base, UnsignedBigInteger const& exponent, UnsignedBigInteger const& modulo, UnsignedBigInteger& temp_z0, UnsignedBigInteger& temp_rr, UnsignedBigInteger& temp_one, UnsignedBigInteger& temp_z, UnsignedBigInteger& temp_zz, UnsignedBigInteger& temp_x, UnsignedBigInteger& temp_extra, UnsignedBigInteger& result);

private:
    static UnsignedBigInteger::Word montgomery_fragment(UnsignedBigInteger& z, size_t offset_in_z, UnsignedBigInteger const& x, UnsignedBigInteger::Word y_digit, size_t num_words);
    static void almost_montgomery_multiplication_without_allocation(UnsignedBigInteger const& x, UnsignedBigInteger const& y, UnsignedBigInteger const& modulo, UnsignedBigInteger& z, UnsignedBigInteger::Word k, size_t num_words, UnsignedBigInteger& result);
    static void shift_left_by_n_words(UnsignedBigInteger const& number, size_t number_of_words, UnsignedBigInteger& output);
    static void shift_right_by_n_words(UnsignedBigInteger const& number, size_t number_of_words, UnsignedBigInteger& output);
    ALWAYS_INLINE static UnsignedBigInteger::Word shift_left_get_one_word(UnsignedBigInteger const& number, size_t num_bits, size_t result_word_index);
};

}
