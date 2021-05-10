/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2020-2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "UnsignedBigIntegerAlgorithms.h"

namespace Crypto {

void UnsignedBigIntegerAlgorithms::destructive_modular_power_without_allocation(
    UnsignedBigInteger& ep,
    UnsignedBigInteger& base,
    UnsignedBigInteger const& m,
    UnsignedBigInteger& temp_1,
    UnsignedBigInteger& temp_2,
    UnsignedBigInteger& temp_3,
    UnsignedBigInteger& temp_4,
    UnsignedBigInteger& temp_multiply,
    UnsignedBigInteger& temp_quotient,
    UnsignedBigInteger& temp_remainder,
    UnsignedBigInteger& exp)
{
    exp.set_to(1);
    while (!(ep < 1)) {
        if (ep.words()[0] % 2 == 1) {
            // exp = (exp * base) % m;
            multiply_without_allocation(exp, base, temp_1, temp_2, temp_3, temp_4, temp_multiply);
            divide_without_allocation(temp_multiply, m, temp_1, temp_2, temp_3, temp_4, temp_quotient, temp_remainder);
            exp.set_to(temp_remainder);
        }

        // ep = ep / 2;
        divide_u16_without_allocation(ep, 2, temp_quotient, temp_remainder);
        ep.set_to(temp_quotient);

        // base = (base * base) % m;
        multiply_without_allocation(base, base, temp_1, temp_2, temp_3, temp_4, temp_multiply);
        divide_without_allocation(temp_multiply, m, temp_1, temp_2, temp_3, temp_4, temp_quotient, temp_remainder);
        base.set_to(temp_remainder);

        // Note that not clamping here would cause future calculations (multiply, specifically) to allocate even more unused space
        // which would then persist through the temp bigints, and significantly slow down later loops.
        // To avoid that, we can clamp to a specific max size, or just clamp to the min needed amount of space.
        ep.clamp_to_trimmed_length();
        exp.clamp_to_trimmed_length();
        base.clamp_to_trimmed_length();
    }
}

}
