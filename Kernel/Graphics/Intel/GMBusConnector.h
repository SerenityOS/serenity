/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

struct [[gnu::packed]] GMBusRegisters {
    u32 clock;
    u32 command;
    u32 status;
    u32 data;
};

enum class GMBusStatus;

class GMBusConnector {
public:
    enum PinPair : u8 {
        None = 0,
        DedicatedControl = 1,
        DedicatedAnalog = 0b10,
        IntegratedDigital = 0b11,
        sDVO = 0b101,
        Dconnector = 0b111,
    };

public:
    static ErrorOr<NonnullOwnPtr<GMBusConnector>> create_with_physical_address(PhysicalAddress gmbus_start_address);

    void write(unsigned address, u32 data);
    ErrorOr<void> read(unsigned address, u8* buf, size_t length);
    void set_default_rate();

private:
    void set_pin_pair(PinPair pin_pair);

    bool wait_for(GMBusStatus desired_status, Optional<size_t> milliseconds_timeout);

    GMBusConnector(NonnullOwnPtr<Memory::Region> registers_region, size_t registers_region_offset);
    Spinlock m_access_lock;
    Memory::TypedMapping<volatile GMBusRegisters> m_gmbus_registers;
};
}
