/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteReader.h>
#include <AK/Platform.h>
#include <AK/Types.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/IO.h>
#endif
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class IOWindow {
public:
    enum class SpaceType {
#if ARCH(X86_64)
        IO,
#endif
        Memory,
    };

    SpaceType space_type() const { return m_space_type; }

#if ARCH(X86_64)
    static ErrorOr<NonnullOwnPtr<IOWindow>> create_for_io_space(IOAddress, u64 space_length);
#endif
    static ErrorOr<NonnullOwnPtr<IOWindow>> create_for_pci_device_bar(PCI::DeviceIdentifier const&, PCI::HeaderType0BaseRegister, u64 space_length);
    static ErrorOr<NonnullOwnPtr<IOWindow>> create_for_pci_device_bar(PCI::DeviceIdentifier const&, PCI::HeaderType0BaseRegister);

    ErrorOr<NonnullOwnPtr<IOWindow>> create_from_io_window_with_offset(u64 offset, u64 space_length);
    ErrorOr<NonnullOwnPtr<IOWindow>> create_from_io_window_with_offset(u64 offset);

    u8 read8(u64 offset);
    u16 read16(u64 offset);
    u32 read32(u64 offset);

    void write8(u64 offset, u8);
    void write16(u64 offset, u16);
    void write32(u64 offset, u32);

    // Note: These methods are useful in exceptional cases where we need to do unaligned
    // access. This mostly happens on emulators and hypervisors (such as VMWare) because they don't enforce aligned access
    // to IO and sometimes even require such access, so we have to use these functions.
    void write32_unaligned(u64 offset, u32);
    u32 read32_unaligned(u64 offset);

    bool operator==(IOWindow const& other) const = delete;
    bool operator!=(IOWindow const& other) const = delete;
    bool operator>(IOWindow const& other) const = delete;
    bool operator>=(IOWindow const& other) const = delete;
    bool operator<(IOWindow const& other) const = delete;
    bool operator<=(IOWindow const& other) const = delete;

    ~IOWindow();

    PhysicalAddress as_physical_memory_address() const;
#if ARCH(X86_64)
    IOAddress as_io_address() const;
#endif

private:
    explicit IOWindow(NonnullOwnPtr<Memory::TypedMapping<u8 volatile>>);

    u8 volatile* as_memory_address_pointer();

#if ARCH(X86_64)
    struct IOAddressData {
    public:
        IOAddressData(u64 address, u64 space_length)
            : m_address(address)
            , m_space_length(space_length)
        {
        }
        u64 address() const { return m_address; }
        u64 space_length() const { return m_space_length; }

    private:
        u64 m_address { 0 };
        u64 m_space_length { 0 };
    };

    explicit IOWindow(NonnullOwnPtr<IOAddressData>);
#endif

    bool is_access_in_range(u64 offset, size_t byte_size_access) const;
    bool is_access_aligned(u64 offset, size_t byte_size_access) const;

    template<typename T>
    ALWAYS_INLINE void in(u64 start_offset, T& data)
    {
#if ARCH(X86_64)
        if (m_space_type == SpaceType::IO) {
            data = as_io_address().offset(start_offset).in<T>();
            return;
        }
#endif
        VERIFY(m_space_type == SpaceType::Memory);
        VERIFY(m_memory_mapped_range);
        // Note: For memory-mapped IO we simply never allow unaligned access as it
        // can cause problems with strict bare metal hardware. For example, some XHCI USB controllers
        // might completely lock up because of an unaligned memory access to their registers.
        VERIFY((start_offset % sizeof(T)) == 0);
        data = *(T volatile*)(as_memory_address_pointer() + start_offset);
    }

    template<typename T>
    ALWAYS_INLINE void out(u64 start_offset, T value)
    {
#if ARCH(X86_64)
        if (m_space_type == SpaceType::IO) {
            VERIFY(m_io_range);
            as_io_address().offset(start_offset).out<T>(value);
            return;
        }
#endif
        VERIFY(m_space_type == SpaceType::Memory);
        VERIFY(m_memory_mapped_range);
        // Note: For memory-mapped IO we simply never allow unaligned access as it
        // can cause problems with strict bare metal hardware. For example, some XHCI USB controllers
        // might completely lock up because of an unaligned memory access to their registers.
        VERIFY((start_offset % sizeof(T)) == 0);
        *(T volatile*)(as_memory_address_pointer() + start_offset) = value;
    }

    SpaceType m_space_type { SpaceType::Memory };

    OwnPtr<Memory::TypedMapping<u8 volatile>> m_memory_mapped_range;

#if ARCH(X86_64)
    OwnPtr<IOAddressData> m_io_range;
#endif
};

}

template<>
struct AK::Formatter<Kernel::IOWindow> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::IOWindow const& value)
    {
#if ARCH(X86_64)
        if (value.space_type() == Kernel::IOWindow::SpaceType::IO)
            return Formatter<FormatString>::format(builder, "{}"sv, value.as_io_address());
#endif
        VERIFY(value.space_type() == Kernel::IOWindow::SpaceType::Memory);
        return Formatter<FormatString>::format(builder, "Memory {}"sv, value.as_physical_memory_address());
    }
};
