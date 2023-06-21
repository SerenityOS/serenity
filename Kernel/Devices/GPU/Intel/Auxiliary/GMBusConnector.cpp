/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Devices/GPU/Intel/Auxiliary/GMBusConnector.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

enum class GMBusStatus {
    TransactionCompletion,
    HardwareReady
};

enum GMBusCycle {
    Wait = 1,
    Stop = 4,
};

ErrorOr<NonnullOwnPtr<GMBusConnector>> GMBusConnector::create_with_physical_address(PhysicalAddress gmbus_start_address)
{
    auto registers_mapping = TRY(map_typed<GMBusRegisters volatile>(gmbus_start_address, sizeof(GMBusRegisters), Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) GMBusConnector(move(registers_mapping)));
}

GMBusConnector::GMBusConnector(Memory::TypedMapping<GMBusRegisters volatile> registers_mapping)
    : m_gmbus_registers(move(registers_mapping))
{
    set_default_rate();
    set_pin_pair(PinPair::DedicatedAnalog);
}

bool GMBusConnector::wait_for(GMBusStatus desired_status, size_t milliseconds_timeout)
{
    VERIFY(m_access_lock.is_locked());
    size_t milliseconds_passed = 0;
    while (1) {
        if (milliseconds_timeout < milliseconds_passed)
            return false;
        full_memory_barrier();
        u32 status = m_gmbus_registers->status;
        full_memory_barrier();
        VERIFY(!(status & (1 << 10))); // error happened
        switch (desired_status) {
        case GMBusStatus::HardwareReady:
            if (status & (1 << 11))
                return true;
            break;
        case GMBusStatus::TransactionCompletion:
            if (status & (1 << 14))
                return true;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        microseconds_delay(1000);
        milliseconds_passed++;
    }
}

ErrorOr<void> GMBusConnector::write(unsigned address, u32 data)
{
    VERIFY(address < 256);
    SpinlockLocker locker(m_access_lock);
    full_memory_barrier();
    m_gmbus_registers->data = data;
    full_memory_barrier();
    m_gmbus_registers->command = ((address << 1) | (1 << 16) | (GMBusCycle::Wait << 25) | (1 << 30));
    full_memory_barrier();
    if (!wait_for(GMBusStatus::TransactionCompletion, 250))
        return Error::from_errno(EBUSY);
    return {};
}

void GMBusConnector::set_default_rate()
{
    // FIXME: Verify GMBUS Rate Select is set only when GMBUS is idle
    SpinlockLocker locker(m_access_lock);
    // Set the rate to 100KHz
    m_gmbus_registers->clock = m_gmbus_registers->clock & ~(0b111 << 8);
}

void GMBusConnector::set_pin_pair(PinPair pin_pair)
{
    // FIXME: Verify GMBUS is idle
    SpinlockLocker locker(m_access_lock);
    m_gmbus_registers->clock = (m_gmbus_registers->clock & (~0b111)) | (pin_pair & 0b111);
}

ErrorOr<void> GMBusConnector::read(unsigned address, u8* buf, size_t length)
{
    VERIFY(address < 256);
    SpinlockLocker locker(m_access_lock);
    size_t nread = 0;
    auto read_set = [&] {
        full_memory_barrier();
        u32 data = m_gmbus_registers->data;
        full_memory_barrier();
        for (size_t index = 0; index < 4; index++) {
            if (nread == length)
                break;
            buf[nread] = (data >> (8 * index)) & 0xFF;
            nread++;
        }
    };

    full_memory_barrier();
    m_gmbus_registers->command = (1 | (address << 1) | (length << 16) | (GMBusCycle::Wait << 25) | (1 << 30));
    full_memory_barrier();
    while (nread < length) {
        if (!wait_for(GMBusStatus::HardwareReady, 250))
            return Error::from_errno(EBUSY);
        read_set();
    }
    if (!wait_for(GMBusStatus::TransactionCompletion, 250))
        return Error::from_errno(EBUSY);
    return {};
}

}
