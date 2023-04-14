/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SoftMMU.h"

namespace UserspaceEmulator {

class SimpleRegion final : public Region {
public:
    SimpleRegion(FlatPtr base, FlatPtr size);
    virtual ~SimpleRegion() override;

    virtual ValueWithShadow<u8> read8(FlatPtr offset) override;
    virtual ValueWithShadow<u16> read16(FlatPtr offset) override;
    virtual ValueWithShadow<u32> read32(FlatPtr offset) override;
    virtual ValueWithShadow<u64> read64(FlatPtr offset) override;
    virtual ValueWithShadow<u128> read128(FlatPtr offset) override;
    virtual ValueWithShadow<u256> read256(FlatPtr offset) override;

    virtual void write8(FlatPtr offset, ValueWithShadow<u8>) override;
    virtual void write16(FlatPtr offset, ValueWithShadow<u16>) override;
    virtual void write32(FlatPtr offset, ValueWithShadow<u32>) override;
    virtual void write64(FlatPtr offset, ValueWithShadow<u64>) override;
    virtual void write128(FlatPtr offset, ValueWithShadow<u128>) override;
    virtual void write256(FlatPtr offset, ValueWithShadow<u256>) override;

    virtual u8* data() override { return m_data; }
    virtual u8* shadow_data() override { return m_shadow_data; }

    virtual u8* cacheable_ptr(FlatPtr offset) override;

private:
    u8* m_data { nullptr };
    u8* m_shadow_data { nullptr };
};

}
