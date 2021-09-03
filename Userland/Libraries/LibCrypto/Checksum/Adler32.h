/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Span.h>
#include <YAK/Types.h>
#include <LibCrypto/Checksum/ChecksumFunction.h>

namespace Crypto::Checksum {

class Adler32 : public ChecksumFunction<u32> {
public:
    Adler32() { }
    Adler32(ReadonlyBytes data)
    {
        update(data);
    }

    Adler32(u32 initial_a, u32 initial_b, ReadonlyBytes data)
        : m_state_a(initial_a)
        , m_state_b(initial_b)
    {
        update(data);
    }

    void update(ReadonlyBytes data);
    u32 digest();

private:
    u32 m_state_a { 1 };
    u32 m_state_b { 0 };
};

}
