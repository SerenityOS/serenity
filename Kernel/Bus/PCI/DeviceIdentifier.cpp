/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AnyOf.h>
#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Bus/PCI/Definitions.h>

namespace Kernel::PCI {

ErrorOr<NonnullRefPtr<DeviceIdentifier>> DeviceIdentifier::from_enumerable_identifier(EnumerableDeviceIdentifier const& other_identifier)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) DeviceIdentifier(other_identifier));
}

}
