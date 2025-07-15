/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Types.h>
#include <LibCrypto/Checksum/ChecksumFunction.h>

namespace Crypto::Checksum {

class IPv4Header : public ChecksumFunction<u16> {
public:
    IPv4Header() = default;
    IPv4Header(ReadonlyBytes data)
    {
        update(data);
    }

    virtual void update(ReadonlyBytes data) override;
    virtual u16 digest() override;

private:
    // NOTE: This is intentionally 32-bit rather than 16-bit.
    // When we're in the process of summing up, we need to avoid
    // overflowing or otherwise losing the carries in the process.
    u32 m_state { 0 };
};

}
