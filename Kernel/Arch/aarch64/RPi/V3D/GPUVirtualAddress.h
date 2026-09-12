/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/VirtualAddress.h>
#include <Kernel/Memory/VirtualRange.h>

namespace Kernel::RPi::V3D {

class GPUVirtualAddress : public VirtualAddressBase<u32> {
    using VirtualAddressBase::VirtualAddressBase;
};

class GPUVirtualRange : public Memory::VirtualRangeBase<GPUVirtualAddress> {
    using VirtualRangeBase::VirtualRangeBase;
};

}

template<>
struct AK::Formatter<Kernel::RPi::V3D::GPUVirtualAddress> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::RPi::V3D::GPUVirtualAddress value)
    {
        return AK::Formatter<FormatString>::format(builder, "GV{:#08x}"sv, value.get());
    }
};
