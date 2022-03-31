/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/Graphics/Intel/Definitions.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class DisplayPortAuxiliaryConnector {
public:
    enum class PortIndex {
        PortA,
        PortB,
        PortC,
        PortD,
    };

private:
    struct [[gnu::packed]] UnifiedDisplayPortAuxChannelRegisters {
        IntelGraphics::DisplayPortAuxChannelRegisters set1;
        u8 padding1[236]; // Note: Other registers might reside here, don't change or touch (read/write) these bytes
        IntelGraphics::DisplayPortAuxChannelRegisters set2;
        u8 padding2[236]; // Note: Other registers might reside here, don't change or touch (read/write) these bytes
        IntelGraphics::DisplayPortAuxChannelRegisters set3;
        u8 padding3[236]; // Note: Other registers might reside here, don't change or touch (read/write) these bytes
        IntelGraphics::DisplayPortAuxChannelRegisters set4;
        u8 padding4[236]; // Note: Other registers might reside here, don't change or touch (read/write) these bytes
    };

public:
    static ErrorOr<NonnullOwnPtr<DisplayPortAuxiliaryConnector>> create_with_physical_address(PhysicalAddress display_port_auxiliary_start_address);

    ErrorOr<void> native_write(PortIndex, unsigned address, u8 data);
    ErrorOr<u32> native_read(PortIndex, unsigned address);
    // i2c methods
    ErrorOr<void> i2c_read(PortIndex, unsigned address, u8* buf, size_t length);

private:
    explicit DisplayPortAuxiliaryConnector(Memory::TypedMapping<UnifiedDisplayPortAuxChannelRegisters volatile>);
    IntelGraphics::DisplayPortAuxChannelRegisters volatile& aux_channel_by_port_index(PortIndex) const;

    u32 compose_aux_message(IntelGraphics::DisplayPortAuxiliaryOperation, unsigned address) const;
    u32 compose_native_aux_message(IntelGraphics::DisplayPortAuxiliaryOperation, unsigned address, size_t message_length) const;
    u32 compose_aux_control_register_value(size_t message_length) const;

    Spinlock<LockRank::None> m_access_lock;
    Memory::TypedMapping<UnifiedDisplayPortAuxChannelRegisters volatile> m_all_display_port_aux_registers;
};
}
