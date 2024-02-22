/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::PCI {

inline ErrorOr<PhysicalAddress> get_bar_address(DeviceIdentifier const& device, HeaderType0BaseRegister bar)
{
    u64 pci_bar_value = get_BAR(device, bar);
    auto pci_bar_space_type = get_BAR_space_type(pci_bar_value);

    if (pci_bar_space_type == BARSpaceType::IOSpace)
        return EIO;

    if (pci_bar_space_type == BARSpaceType::Memory64BitSpace) {
        // FIXME: In theory, BAR5 cannot be assigned to 64 bit as it is the last one...
        // however, there might be 64 bit BAR5 for real bare metal hardware, so remove this
        // if it makes a problem.
        if (bar == HeaderType0BaseRegister::BAR5) {
            return Error::from_errno(EINVAL);
        }
        u64 next_pci_bar_value = get_BAR(device, static_cast<PCI::HeaderType0BaseRegister>(to_underlying(bar) + 1));
        pci_bar_value |= next_pci_bar_value << 32;

        return PhysicalAddress { pci_bar_value & bar_address_mask };
    }

    return PhysicalAddress { pci_bar_value & bar_address_mask };
}

template<typename T>
inline ErrorOr<Memory::TypedMapping<T>> map_bar(DeviceIdentifier const& device, HeaderType0BaseRegister bar, size_t size, Memory::Region::Access access = IsConst<T> ? Memory::Region::Access::Read : Memory::Region::Access::ReadWrite)
{
    u64 pci_bar_value = get_BAR(device, bar);
    auto pci_bar_space_type = get_BAR_space_type(pci_bar_value);

    if (pci_bar_space_type == PCI::BARSpaceType::IOSpace)
        return EIO;

    auto bar_address = TRY(get_bar_address(device, bar));

    auto pci_bar_space_size = PCI::get_BAR_space_size(device, bar);
    if (pci_bar_space_size < size)
        return Error::from_errno(EIO);

    if (pci_bar_space_type == PCI::BARSpaceType::Memory32BitSpace && Checked<u32>::addition_would_overflow(bar_address.get(), size))
        return Error::from_errno(EOVERFLOW);
    if (pci_bar_space_type == PCI::BARSpaceType::Memory16BitSpace && Checked<u16>::addition_would_overflow(bar_address.get(), size))
        return Error::from_errno(EOVERFLOW);
    if (pci_bar_space_type == PCI::BARSpaceType::Memory64BitSpace && Checked<u64>::addition_would_overflow(bar_address.get(), size))
        return Error::from_errno(EOVERFLOW);

    return Memory::map_typed<T>(bar_address, size, access);
}

template<typename T>
inline ErrorOr<Memory::TypedMapping<T>> map_bar(DeviceIdentifier const& device, HeaderType0BaseRegister bar, Memory::Region::Access access = IsConst<T> ? Memory::Region::Access::Read : Memory::Region::Access::ReadWrite)
{
    return map_bar<T>(device, bar, PCI::get_BAR_space_size(device, bar), access);
}

template<typename T>
inline ErrorOr<NonnullOwnPtr<Memory::TypedMapping<T>>> adopt_new_nonnull_own_bar_mapping(DeviceIdentifier const& device, HeaderType0BaseRegister bar, size_t size, Memory::Region::Access access = IsConst<T> ? Memory::Region::Access::Read : Memory::Region::Access::ReadWrite)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) Memory::TypedMapping<T>(TRY(map_bar<T>(device, bar, size, access))));
}

template<typename T>
inline ErrorOr<NonnullOwnPtr<Memory::TypedMapping<T>>> adopt_new_nonnull_own_bar_mapping(DeviceIdentifier const& device, HeaderType0BaseRegister bar, Memory::Region::Access access = IsConst<T> ? Memory::Region::Access::Read : Memory::Region::Access::ReadWrite)
{
    return adopt_new_nonnull_own_bar_mapping<T>(device, bar, PCI::get_BAR_space_size(device, bar), access);
}

}
