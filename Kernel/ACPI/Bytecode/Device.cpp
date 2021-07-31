/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntrusiveList.h>
#include <AK/NeverDestroyed.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/ACPI/Bytecode/Device.h>
#include <Kernel/ACPI/Bytecode/TermObjectEnumerator.h>
#include <Kernel/Sections.h>

namespace Kernel::ACPI {

UNMAP_AFTER_INIT NonnullRefPtr<Device> Device::must_create(const TermObjectEnumerator& parent_enumerator, Span<u8 const> encoded_name_string)
{
    auto new_device = adopt_ref_if_nonnull(new (nothrow) Device(encoded_name_string)).release_nonnull();
    new_device->enumerate(parent_enumerator);
    return new_device;
}

UNMAP_AFTER_INIT Device::Device(Span<u8 const> encoded_name_string)
    : Scope(encoded_name_string)
{
}

}
