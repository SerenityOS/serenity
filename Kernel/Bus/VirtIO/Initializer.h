/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Result.h>
#include <Kernel/Bus/VirtIO/Result.h>

namespace Kernel::VirtIO {

class Initializer {
public:
    static void detect();

    template<typename DeviceType, typename... Args>
    static inline Result<NonnullRefPtr<DeviceType>, VirtIO::InitializationResult> try_create_virtio_device(Args&&... args)
    {
        auto device = adopt_ref_if_nonnull(new DeviceType(forward<Args>(args)...));
        if (!device)
            return InitializationResult(InitializationState::OutOfMemory);
        TRY(device->initialize());
        return device.release_nonnull();
    }
};

}
