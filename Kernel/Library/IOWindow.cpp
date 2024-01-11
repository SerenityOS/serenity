/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Library/IOWindow.h>

namespace Kernel {

#if ARCH(X86_64)
ErrorOr<NonnullOwnPtr<IOWindow>> IOWindow::create_for_io_space(IOAddress address, u64 space_length)
{
    VERIFY(!Checked<u64>::addition_would_overflow(address.get(), space_length));
    auto io_address_range = TRY(adopt_nonnull_own_or_enomem(new (nothrow) IOAddressData(address.get(), space_length)));
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) IOWindow(move(io_address_range))));
}

IOWindow::IOWindow(NonnullOwnPtr<IOAddressData> io_range)
    : m_space_type(SpaceType::IO)
    , m_io_range(move(io_range))
{
}
#endif

ErrorOr<NonnullOwnPtr<IOWindow>> IOWindow::create_from_io_window_with_offset(u64 offset, u64 space_length)
{
#if ARCH(X86_64)
    if (m_space_type == SpaceType::IO) {
        VERIFY(m_io_range);
        if (Checked<u64>::addition_would_overflow(m_io_range->address(), space_length))
            return Error::from_errno(EOVERFLOW);
        auto io_address_range = TRY(adopt_nonnull_own_or_enomem(new (nothrow) IOAddressData(as_io_address().offset(offset).get(), space_length)));
        return TRY(adopt_nonnull_own_or_enomem(new (nothrow) IOWindow(move(io_address_range))));
    }
#endif
    VERIFY(space_type() == SpaceType::Memory);
    VERIFY(m_memory_mapped_range);

    if (Checked<u64>::addition_would_overflow(m_memory_mapped_range->paddr.get(), offset))
        return Error::from_errno(EOVERFLOW);
    if (Checked<u64>::addition_would_overflow(m_memory_mapped_range->paddr.get() + offset, space_length))
        return Error::from_errno(EOVERFLOW);

    auto memory_mapped_range = TRY(Memory::adopt_new_nonnull_own_typed_mapping<u8 volatile>(m_memory_mapped_range->paddr.offset(offset), space_length, Memory::Region::Access::ReadWrite));
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) IOWindow(move(memory_mapped_range))));
}

ErrorOr<NonnullOwnPtr<IOWindow>> IOWindow::create_from_io_window_with_offset(u64 offset)
{

#if ARCH(X86_64)
    if (m_space_type == SpaceType::IO) {
        VERIFY(m_io_range);
        VERIFY(m_io_range->space_length() >= offset);
        return create_from_io_window_with_offset(offset, m_io_range->space_length() - offset);
    }
#endif
    VERIFY(space_type() == SpaceType::Memory);
    VERIFY(m_memory_mapped_range);
    VERIFY(m_memory_mapped_range->length >= offset);
    return create_from_io_window_with_offset(offset, m_memory_mapped_range->length - offset);
}

ErrorOr<NonnullOwnPtr<IOWindow>> IOWindow::create_for_pci_device_bar(PCI::DeviceIdentifier const& pci_device_identifier, PCI::HeaderType0BaseRegister pci_bar, u64 space_length)
{
    u64 pci_bar_value = PCI::get_BAR(pci_device_identifier, pci_bar);
    auto pci_bar_space_type = PCI::get_BAR_space_type(pci_bar_value);

    if (pci_bar_space_type == PCI::BARSpaceType::IOSpace) {
#if ARCH(X86_64)
        auto pci_bar_space_size = PCI::get_BAR_space_size(pci_device_identifier, pci_bar);
        if (pci_bar_space_size < space_length)
            return Error::from_errno(EIO);

        // X86 IO instructions use DX -a 16 bit register- as the "address"
        // So we need to check against u16's
        if (pci_bar_value > AK::NumericLimits<u16>::max())
            return Error::from_errno(EOVERFLOW);
        if (Checked<u16>::addition_would_overflow(static_cast<u16>(pci_bar_value), static_cast<u16>(space_length)))
            return Error::from_errno(EOVERFLOW);
        auto io_address_range = TRY(adopt_nonnull_own_or_enomem(new (nothrow) IOAddressData((pci_bar_value & 0xfffffffc), space_length)));
        return TRY(adopt_nonnull_own_or_enomem(new (nothrow) IOWindow(move(io_address_range))));
#else
        // Note: For non-x86 platforms, IO PCI BARs are simply not useable.
        return Error::from_errno(ENOTSUP);
#endif
    }

    auto memory_mapped_range = TRY(PCI::adopt_new_nonnull_own_bar_mapping<u8 volatile>(pci_device_identifier, pci_bar, space_length));
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) IOWindow(move(memory_mapped_range))));
}

ErrorOr<NonnullOwnPtr<IOWindow>> IOWindow::create_for_pci_device_bar(PCI::DeviceIdentifier const& pci_device_identifier, PCI::HeaderType0BaseRegister pci_bar)
{
    u64 pci_bar_space_size = PCI::get_BAR_space_size(pci_device_identifier, pci_bar);
    return create_for_pci_device_bar(pci_device_identifier, pci_bar, pci_bar_space_size);
}

IOWindow::IOWindow(NonnullOwnPtr<Memory::TypedMapping<u8 volatile>> memory_mapped_range)
    : m_space_type(SpaceType::Memory)
    , m_memory_mapped_range(move(memory_mapped_range))
{
}

IOWindow::~IOWindow() = default;

bool IOWindow::is_access_aligned(u64 offset, size_t byte_size_access) const
{
    return (offset % byte_size_access) == 0;
}

bool IOWindow::is_access_in_range(u64 offset, size_t byte_size_access) const
{
    if (Checked<u64>::addition_would_overflow(offset, byte_size_access))
        return false;
#if ARCH(X86_64)
    if (m_space_type == SpaceType::IO) {
        VERIFY(m_io_range);
        VERIFY(!Checked<u64>::addition_would_overflow(m_io_range->address(), m_io_range->space_length()));
        // To understand how we treat IO address space with the corresponding calculation, the Intel Software Developer manual
        // helps us to understand the layout of the IO address space -
        //
        // Intel® 64 and IA-32 Architectures Software Developer’s Manual, Volume 1: Basic Architecture, 16.3 I/O ADDRESS SPACE, page 16-1 wrote:
        // Any two consecutive 8-bit ports can be treated as a 16-bit port, and any four consecutive ports can be a 32-bit port.
        // In this manner, the processor can transfer 8, 16, or 32 bits to or from a device in the I/O address space.
        // Like words in memory, 16-bit ports should be aligned to even addresses (0, 2, 4, ...) so that all 16 bits can be transferred in a single bus cycle.
        // Likewise, 32-bit ports should be aligned to addresses that are multiples of four (0, 4, 8, ...).
        // The processor supports data transfers to unaligned ports, but there is a performance penalty because one or more
        // extra bus cycle must be used.
        return (m_io_range->address() + m_io_range->space_length()) >= (offset + byte_size_access);
    }
#endif
    VERIFY(space_type() == SpaceType::Memory);
    VERIFY(m_memory_mapped_range);
    VERIFY(!Checked<u64>::addition_would_overflow(m_memory_mapped_range->offset, m_memory_mapped_range->length));
    return (m_memory_mapped_range->offset + m_memory_mapped_range->length) >= (offset + byte_size_access);
}

u8 IOWindow::read8(u64 offset)
{
    VERIFY(is_access_in_range(offset, sizeof(u8)));
    u8 data { 0 };
    in<u8>(offset, data);
    return data;
}
u16 IOWindow::read16(u64 offset)
{
    // Note: Although it might be OK to allow unaligned access on regular memory,
    // for memory mapped IO access, it should always be considered a bug.
    // The same goes for port mapped IO access, because in x86 unaligned access to ports
    // is possible but there's a performance penalty.
    VERIFY(is_access_in_range(offset, sizeof(u16)));
    VERIFY(is_access_aligned(offset, sizeof(u16)));
    u16 data { 0 };
    in<u16>(offset, data);
    return data;
}
u32 IOWindow::read32(u64 offset)
{
    // Note: Although it might be OK to allow unaligned access on regular memory,
    // for memory mapped IO access, it should always be considered a bug.
    // The same goes for port mapped IO access, because in x86 unaligned access to ports
    // is possible but there's a performance penalty.
    VERIFY(is_access_in_range(offset, sizeof(u32)));
    VERIFY(is_access_aligned(offset, sizeof(u32)));
    u32 data { 0 };
    in<u32>(offset, data);
    return data;
}

void IOWindow::write8(u64 offset, u8 data)
{
    VERIFY(is_access_in_range(offset, sizeof(u8)));
    out<u8>(offset, data);
}
void IOWindow::write16(u64 offset, u16 data)
{
    // Note: Although it might be OK to allow unaligned access on regular memory,
    // for memory mapped IO access, it should always be considered a bug.
    // The same goes for port mapped IO access, because in x86 unaligned access to ports
    // is possible but there's a performance penalty.
    VERIFY(is_access_in_range(offset, sizeof(u16)));
    VERIFY(is_access_aligned(offset, sizeof(u16)));
    out<u16>(offset, data);
}
void IOWindow::write32(u64 offset, u32 data)
{
    // Note: Although it might be OK to allow unaligned access on regular memory,
    // for memory mapped IO access, it should always be considered a bug.
    // The same goes for port mapped IO access, because in x86 unaligned access to ports
    // is possible but there's a performance penalty.
    VERIFY(is_access_in_range(offset, sizeof(u32)));
    VERIFY(is_access_aligned(offset, sizeof(u32)));
    out<u32>(offset, data);
}

void IOWindow::write32_unaligned(u64 offset, u32 data)
{
    // Note: We only verify that we access IO in the expected range.
    // Note: for port mapped IO access, because in x86 unaligned access to ports
    // is possible but there's a performance penalty, we can still allow that to happen.
    // However, it should be noted that most cases should not use unaligned access
    // to hardware IO, so this is a valid case in emulators or hypervisors only.
    // Note: Using this for memory mapped IO will fail for unaligned access, because
    // there's no valid use case for it (yet).
    VERIFY(space_type() != SpaceType::Memory);
    VERIFY(is_access_in_range(offset, sizeof(u32)));
    out<u32>(offset, data);
}

u32 IOWindow::read32_unaligned(u64 offset)
{
    // Note: We only verify that we access IO in the expected range.
    // Note: for port mapped IO access, because in x86 unaligned access to ports
    // is possible but there's a performance penalty, we can still allow that to happen.
    // However, it should be noted that most cases should not use unaligned access
    // to hardware IO, so this is a valid case in emulators or hypervisors only.
    // Note: Using this for memory mapped IO will fail for unaligned access, because
    // there's no valid use case for it (yet).
    VERIFY(space_type() != SpaceType::Memory);
    VERIFY(is_access_in_range(offset, sizeof(u32)));
    u32 data { 0 };
    in<u32>(offset, data);
    return data;
}

PhysicalAddress IOWindow::as_physical_memory_address() const
{
    VERIFY(space_type() == SpaceType::Memory);
    VERIFY(m_memory_mapped_range);
    return m_memory_mapped_range->paddr;
}

u8 volatile* IOWindow::as_memory_address_pointer()
{
    VERIFY(space_type() == SpaceType::Memory);
    VERIFY(m_memory_mapped_range);
    return m_memory_mapped_range->ptr();
}

#if ARCH(X86_64)
IOAddress IOWindow::as_io_address() const
{
    VERIFY(space_type() == SpaceType::IO);
    VERIFY(m_io_range);
    return IOAddress(m_io_range->address());
}
#endif

}
