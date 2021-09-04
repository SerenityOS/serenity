/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/PK/RSA.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    ByteBuffer rsa_data = ByteBuffer::copy(data, size);
    Crypto::PK::RSA::parse_rsa_key(rsa_data);
    return 0;
}
