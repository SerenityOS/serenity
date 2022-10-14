/*
 * Copyright (c) 2022, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/Authentication/Poly1305.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    if (size < 32)
        return 0;

    auto initial = ReadonlyBytes { data, 32 };
    auto message = ReadonlyBytes { data + 32, size - 32 };

    Crypto::Authentication::Poly1305 mac(initial);
    mac.update(message);
    (void)mac.digest();
    return 0;
}
