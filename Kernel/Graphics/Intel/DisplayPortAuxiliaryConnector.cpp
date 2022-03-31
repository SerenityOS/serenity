/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Graphics/Intel/DisplayPortAuxiliaryConnector.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

enum class GMBusStatus {
    TransactionCompletion,
    HardwareReady
};

enum DisplayPortAuxiliary {
    Wait = 1,
    Stop = 4,
};

ErrorOr<NonnullOwnPtr<DisplayPortAuxiliaryConnector>> DisplayPortAuxiliaryConnector::create_with_physical_address(PhysicalAddress display_port_auxiliary_start_address)
{
    auto registers_mapping = TRY(Memory::map_typed<UnifiedDisplayPortAuxChannelRegisters volatile>(display_port_auxiliary_start_address, sizeof(UnifiedDisplayPortAuxChannelRegisters), Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) DisplayPortAuxiliaryConnector(move(registers_mapping)));
}

DisplayPortAuxiliaryConnector::DisplayPortAuxiliaryConnector(Memory::TypedMapping<UnifiedDisplayPortAuxChannelRegisters volatile> all_display_port_aux_registers)
    : m_all_display_port_aux_registers(move(all_display_port_aux_registers))
{
}

IntelGraphics::DisplayPortAuxChannelRegisters volatile& DisplayPortAuxiliaryConnector::aux_channel_by_port_index(PortIndex port_index) const
{
    VERIFY(m_access_lock.is_locked());
    switch (port_index) {
    case PortIndex::PortA:
        return *((IntelGraphics::DisplayPortAuxChannelRegisters volatile*)m_all_display_port_aux_registers.base_address().offset(__builtin_offsetof(UnifiedDisplayPortAuxChannelRegisters, set1)).as_ptr());
    case PortIndex::PortB:
        return *((IntelGraphics::DisplayPortAuxChannelRegisters volatile*)m_all_display_port_aux_registers.base_address().offset(__builtin_offsetof(UnifiedDisplayPortAuxChannelRegisters, set2)).as_ptr());
    case PortIndex::PortC:
        return *((IntelGraphics::DisplayPortAuxChannelRegisters volatile*)m_all_display_port_aux_registers.base_address().offset(__builtin_offsetof(UnifiedDisplayPortAuxChannelRegisters, set3)).as_ptr());
    case PortIndex::PortD:
        return *((IntelGraphics::DisplayPortAuxChannelRegisters volatile*)m_all_display_port_aux_registers.base_address().offset(__builtin_offsetof(UnifiedDisplayPortAuxChannelRegisters, set4)).as_ptr());
    default:
        VERIFY_NOT_REACHED();
    }
}

u32 DisplayPortAuxiliaryConnector::compose_aux_message(IntelGraphics::DisplayPortAuxiliaryOperation operation, unsigned address) const
{
    // Note: We should send an auxiliary message to the chosen port like this:
    // We first set the first Auxiliary Data Register in a set. The message there
    // should be composed with a message address and "opcode".
    VERIFY(m_access_lock.is_locked());
    u32 message = (address << 8) | (to_underlying(operation) << 28);
    return message;
}

u32 DisplayPortAuxiliaryConnector::compose_aux_control_register_value(size_t message_length) const
{
    VERIFY(m_access_lock.is_locked());
    return (1 << 31) | message_length << 20 | 0x23f;
}

u32 DisplayPortAuxiliaryConnector::compose_native_aux_message(IntelGraphics::DisplayPortAuxiliaryOperation operation, unsigned address, size_t message_length) const
{
    VERIFY(m_access_lock.is_locked());
    // Note: Compose AUX message, and OR it with length to indicate that the length bytes.
    u32 message = (address << 8) | (to_underlying(operation) << 28) | message_length;
    return message;
}

ErrorOr<void> DisplayPortAuxiliaryConnector::native_write(PortIndex port_index, unsigned address, u8 data)
{
    SpinlockLocker locker(m_access_lock);
    auto& registers_set = aux_channel_by_port_index(port_index);

    registers_set.data1 = compose_native_aux_message(IntelGraphics::DisplayPortAuxiliaryOperation::NativeWrite, address, 1);
    u32 actual_raw_data = data << 24;
    registers_set.data2 = actual_raw_data;
    microseconds_delay(100);
    registers_set.control = compose_aux_control_register_value(1);
    microseconds_delay(100);
    // FIXME: Wait for ACK
    return {};
}
ErrorOr<u32> DisplayPortAuxiliaryConnector::native_read(PortIndex port_index, unsigned address)
{
    SpinlockLocker locker(m_access_lock);
    // FIXME: Currently we only allow reading in chunks of 4 bytes.
    auto& registers_set = aux_channel_by_port_index(port_index);
    registers_set.data1 = compose_native_aux_message(IntelGraphics::DisplayPortAuxiliaryOperation::NativeRead, address, 4);
    microseconds_delay(100);
    registers_set.control = compose_aux_control_register_value(4);
    microseconds_delay(100);
    // FIXME: Wait for ACK
    microseconds_delay(100);
    u32 value1 = registers_set.data1;
    u32 value2 = registers_set.data2;
    u32 actual_value = 0;
    actual_value |= (value1 >> 16) & 0xff;
    actual_value |= ((value1 >> 8) & 0xff) << 8;
    actual_value |= (value1 & 0xff) << 16;
    actual_value |= ((value2) >> 24 & 0xff) << 24;
    return actual_value;
}

ErrorOr<void> DisplayPortAuxiliaryConnector::i2c_read(PortIndex port_index, unsigned address, u8* buf, size_t length)
{
    SpinlockLocker locker(m_access_lock);
    auto& registers_set = aux_channel_by_port_index(port_index);
    registers_set.data1 = compose_aux_message(IntelGraphics::DisplayPortAuxiliaryOperation::MOT, 0x0);

    microseconds_delay(100);
    registers_set.control = compose_aux_control_register_value(3);
    // FIXME: Wait for ACK
    microseconds_delay(100);

    microseconds_delay(100);
    registers_set.data1 = compose_aux_message(IntelGraphics::DisplayPortAuxiliaryOperation::MOT, address);
    microseconds_delay(100);
    registers_set.control = compose_aux_control_register_value(3);
    // FIXME: Wait for ACK
    microseconds_delay(100);

    for (size_t byte_index = 0; byte_index < length; byte_index++) {
        registers_set.data1 = compose_aux_message(IntelGraphics::DisplayPortAuxiliaryOperation::I2CRead, 0);
        microseconds_delay(100);
        registers_set.control = compose_aux_control_register_value(4);
        // FIXME: Wait for ACK
        microseconds_delay(100);
        u32 value = registers_set.data1;
        buf[byte_index] = (value >> 16) & 0xff;
    }
    return {};
}

}
