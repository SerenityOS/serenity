/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Types.h>
#include <LibCrypto/Checksum/ChecksumFunction.h>

namespace Crypto::Checksum {

struct Table {
    u32 data[256];

    constexpr Table()
        : data()
    {
        for (auto i = 0; i < 256; i++) {
            u32 value = i;

            for (auto j = 0; j < 8; j++) {
                if (value & 1) {
                    value = 0xEDB88320 ^ (value >> 1);
                } else {
                    value = value >> 1;
                }
            }

            data[i] = value;
        }
    }

    constexpr u32 operator[](int index) const
    {
        return data[index];
    }
};

constexpr static auto table = Table();

class CRC32 : public ChecksumFunction<u32> {
public:
    CRC32() { }
    CRC32(ReadonlyBytes data)
    {
        update(data);
    }

    CRC32(u32 initial_state, ReadonlyBytes data)
        : m_state(initial_state)
    {
        update(data);
    }

    virtual void update(ReadonlyBytes data) override;
    virtual u32 digest() override;

private:
    u32 m_state { ~0u };
};

}
