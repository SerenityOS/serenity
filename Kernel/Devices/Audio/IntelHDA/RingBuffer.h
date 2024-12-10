/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Audio/IntelHDA/Timing.h>
#include <Kernel/Library/IOWindow.h>

namespace Kernel::Audio::IntelHDA {

enum class RingBufferType {
    Input,
    Output,
};

// 4.4.1, 4.4.2: CORB and RIRB
template<typename T, RingBufferType U>
class ControllerRingBuffer {
public:
    ControllerRingBuffer(size_t capacity, NonnullOwnPtr<Memory::Region> buffer, NonnullOwnPtr<IOWindow> register_window)
        : m_capacity(capacity)
        , m_buffer(move(buffer))
        , m_register_window(move(register_window))
    {
        // 3.3.22, 3.3.29: Read DMA engine running bit
        u8 control = m_register_window->read8(RingBufferRegisterOffset::Control);
        m_running = (control & RingBufferControlFlag::DMAEnable) > 0;
    }

    static ErrorOr<NonnullOwnPtr<ControllerRingBuffer>> create(StringView name, NonnullOwnPtr<IOWindow> register_window)
    {
        // 3.3.24, 3.3.31: Read the size capability
        auto buffer_size = register_window->read8(RingBufferRegisterOffset::Size);
        u8 size_capability = buffer_size >> 4;
        size_t capacity = ((size_capability & SizeCapabilityFlag::Supports2) > 0) ? 2 : 0;
        if ((size_capability & SizeCapabilityFlag::Supports16) > 0)
            capacity = 16;
        if ((size_capability & SizeCapabilityFlag::Supports256) > 0)
            capacity = 256;
        if (capacity == 0)
            return Error::from_string_view_or_print_error_and_return_errno("RingBuffer reports invalid capacity"sv, ENOTSUP);

        // Create a DMA buffer page to holds the ring buffer
        VERIFY(PAGE_SIZE >= capacity * sizeof(T));
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        auto buffer_region = TRY(MM.allocate_dma_buffer_page(name, U == RingBufferType::Input ? Memory::Region::Access::Read : Memory::Region::Access::Write, Memory::MemoryType::IO));

        // 4.4.1.1, 4.4.2: The CORB buffer in memory must be allocated to start on a 128-byte boundary
        // and in memory configured to match the access type being used.
        VERIFY((buffer_region->physical_page(0)->paddr().get() & 0x7f) == 0);

        return adopt_nonnull_own_or_enomem(new (nothrow) ControllerRingBuffer(capacity, move(buffer_region), move(register_window)));
    }

    size_t capacity() const { return m_capacity; }

    ErrorOr<Optional<T>> read_value()
    requires(U == RingBufferType::Input)
    {
        // 4.4.2: Response Inbound Ring Buffer - RIRB
        auto write_pointer = controller_pointer();
        dbgln_if(INTEL_HDA_DEBUG, "ControllerRingBuffer({}) {}: current_pointer {} write_pointer {}",
            U == RingBufferType::Input ? "input"sv : "output"sv, __FUNCTION__, m_current_pointer, write_pointer);
        if (m_current_pointer == write_pointer)
            return Optional<T> {};

        m_current_pointer = (m_current_pointer + 1) % m_capacity;
        return Optional<T> { *(reinterpret_cast<T*>(m_buffer->vaddr().get()) + m_current_pointer) };
    }

    // 4.4.1.3, 4.4.2.2: Initializing the CORB/RIRB
    ErrorOr<void> register_with_controller()
    {
        // 4.4.1.3, 4.4.2.2: Stop DMA engine
        TRY(set_dma_engine_running(false));

        // 3.3.18, 3.3.19, 3.3.25, 3.3.26, 4.4.1.3: Set base address
        PhysicalPtr buffer_address = m_buffer->physical_page(0)->paddr().get();
        m_register_window->write32(RingBufferRegisterOffset::LowerBaseAddress, buffer_address & 0xffffff80u);
        if constexpr (sizeof(PhysicalPtr) == 8)
            m_register_window->write32(RingBufferRegisterOffset::UpperBaseAddress, buffer_address >> 32);

        // 3.3.24, 3.3.31, 4.4.1.3: Set buffer capacity if more than one capacity is supported
        auto buffer_size = m_register_window->read8(RingBufferRegisterOffset::Size) & static_cast<u8>(~0b11);
        u8 size_capability = buffer_size >> 4;
        if (popcount(size_capability) > 1) {
            switch (m_capacity) {
            case 2:
                break;
            case 16:
                buffer_size |= 0b01;
                break;
            case 256:
                buffer_size |= 0b10;
                break;
            default:
                VERIFY_NOT_REACHED();
            }
            m_register_window->write8(RingBufferRegisterOffset::Size, buffer_size);
        }

        // 4.4.1.3: Reset read and write pointers to 0
        TRY(reset_controller_pointer());
        if constexpr (U == RingBufferType::Output)
            set_write_pointer(0);

        // FIXME: Qemu's Intel HDA device compares the RINTCNT register with the number of responses sent, even
        //        if interrupts are disabled. This is a workaround and allows us to receive 255 responses. We
        //        should try to fix this upstream or toggle this fix with device quirks logic.
        if constexpr (U == RingBufferType::Input)
            m_register_window->write16(RingBufferRegisterOffset::ResponseInterruptCount, 0xff);

        TRY(set_dma_engine_running(true));

        return {};
    }

    ErrorOr<void> write_value(T value)
    requires(U == RingBufferType::Output)
    {
        // 4.4.1.4: Transmitting Commands via the CORB
        auto read_pointer = controller_pointer();
        auto write_pointer = (m_current_pointer + 1) % m_capacity;
        dbgln_if(INTEL_HDA_DEBUG, "ControllerRingBuffer({}) {}: read_pointer {} write_pointer {}",
            U == RingBufferType::Input ? "input"sv : "output"sv, __FUNCTION__, read_pointer, write_pointer);

        if (write_pointer == read_pointer)
            return ENOSPC;

        auto* target_slot = reinterpret_cast<T*>(m_buffer->vaddr().get()) + write_pointer;
        *target_slot = value;
        set_write_pointer(write_pointer);
        return {};
    }

private:
    // 3.3: High Definition Audio Controller Register Set - CORB/RIRB
    enum RingBufferRegisterOffset : u8 {
        LowerBaseAddress = 0x0,
        UpperBaseAddress = 0x4,
        WritePointer = 0x8,
        ReadPointer = 0xa,
        ResponseInterruptCount = 0xa,
        Control = 0xc,
        Status = 0xd,
        Size = 0xe,
    };

    // 3.3.21, 3.3.27: Read/Write Pointer
    enum PointerFlag : u16 {
        Reset = 1u << 15,
    };

    // 3.3.22, 3.3.29: Ring Buffer Control
    enum RingBufferControlFlag : u8 {
        DMAEnable = 1u << 1,
    };

    // 3.3.24, 3.3.31: Size
    enum SizeCapabilityFlag : u8 {
        Supports2 = 1u << 0,
        Supports16 = 1u << 1,
        Supports256 = 1u << 2,
    };

    u8 controller_pointer()
    {
        // 3.3.21, 3.3.27: Get the Read/Write pointer
        auto offset = U == RingBufferType::Input
            ? RingBufferRegisterOffset::WritePointer
            : RingBufferRegisterOffset::ReadPointer;
        return m_register_window->read16(offset) & 0xffu;
    }

    ErrorOr<void> reset_controller_pointer()
    {
        // 3.3.21, 3.3.27: Set the Read/Write pointer reset bit
        auto offset = U == RingBufferType::Input
            ? RingBufferRegisterOffset::WritePointer
            : RingBufferRegisterOffset::ReadPointer;
        m_register_window->write16(offset, PointerFlag::Reset);

        if constexpr (U == RingBufferType::Output) {
            // 3.3.21: "The hardware will physically update this bit to 1 when the CORB pointer reset is
            //          complete. Software must read a 1 to verify that the reset completed correctly."
            TRY(wait_until(frame_delay_in_microseconds(1), controller_timeout_in_microseconds, [&]() {
                u16 read_pointer = m_register_window->read16(offset);
                return (read_pointer & PointerFlag::Reset) > 0;
            }));

            // 3.3.21: "Software must clear this bit back to 0, by writing a 0, and then read back the 0
            //          to verify that the clear completed correctly."
            m_register_window->write16(offset, 0);
            TRY(wait_until(frame_delay_in_microseconds(1), controller_timeout_in_microseconds, [&]() {
                u16 read_pointer = m_register_window->read16(offset);
                return (read_pointer & PointerFlag::Reset) == 0;
            }));
        }

        dbgln_if(INTEL_HDA_DEBUG, "ControllerRingBuffer({}) {}",
            U == RingBufferType::Input ? "input"sv : "output"sv, __FUNCTION__);

        return {};
    }

    ErrorOr<void> set_dma_engine_running(bool running)
    {
        if (m_running == running)
            return {};

        // 3.3.22, 3.3.29: Set DMA engine running bit
        u8 control = m_register_window->read8(RingBufferRegisterOffset::Control);
        if (running)
            control |= RingBufferControlFlag::DMAEnable;
        else
            control &= ~RingBufferControlFlag::DMAEnable;
        dbgln_if(INTEL_HDA_DEBUG, "ControllerRingBuffer({}) {}: {:#08b}",
            U == RingBufferType::Input ? "input"sv : "output"sv, __FUNCTION__, control);
        m_register_window->write8(RingBufferRegisterOffset::Control, control);

        // Must read the value back
        TRY(wait_until(frame_delay_in_microseconds(1), controller_timeout_in_microseconds, [&]() {
            control = m_register_window->read8(RingBufferRegisterOffset::Control);
            return (control & RingBufferControlFlag::DMAEnable) == (running ? RingBufferControlFlag::DMAEnable : 0);
        }));
        m_running = running;
        return {};
    }

    void set_write_pointer(u8 pointer)
    requires(U == RingBufferType::Output)
    {
        // 3.3.20: CORBWP – CORB Write Pointer
        m_register_window->write16(RingBufferRegisterOffset::WritePointer, pointer);
        m_current_pointer = pointer;
    }

    size_t m_capacity;
    NonnullOwnPtr<Memory::Region> m_buffer;
    NonnullOwnPtr<IOWindow> m_register_window;
    bool m_running { false };
    u8 m_current_pointer { 0 };
};

using CommandOutboundRingBuffer = ControllerRingBuffer<u32, RingBufferType::Output>;
using ResponseInboundRingBuffer = ControllerRingBuffer<u64, RingBufferType::Input>;

}
