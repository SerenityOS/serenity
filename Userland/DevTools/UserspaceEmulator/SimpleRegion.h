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
    SimpleRegion(u32 base, u32 size);
    virtual ~SimpleRegion() override;

    virtual ValueWithShadow<u8> read8(u32 offset) override;
    virtual ValueWithShadow<u16> read16(u32 offset) override;
    virtual ValueWithShadow<u32> read32(u32 offset) override;
    virtual ValueWithShadow<u64> read64(u32 offset) override;
    virtual ValueWithShadow<u128> read128(u32 offset) override;
    virtual ValueWithShadow<u256> read256(u32 offset) override;

    virtual void write8(u32 offset, ValueWithShadow<u8>) override;
    virtual void write16(u32 offset, ValueWithShadow<u16>) override;
    virtual void write32(u32 offset, ValueWithShadow<u32>) override;
    virtual void write64(u32 offset, ValueWithShadow<u64>) override;
    virtual void write128(u32 offset, ValueWithShadow<u128>) override;
    virtual void write256(u32 offset, ValueWithShadow<u256>) override;

    virtual u8* data() override { return m_data; }
    virtual u8* shadow_data() override { return m_shadow_data; }

    virtual u8* cacheable_ptr(u32 offset) override;

private:
    u8* m_data { nullptr };
    u8* m_shadow_data { nullptr };
};

}
