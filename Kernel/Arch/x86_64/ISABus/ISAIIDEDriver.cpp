/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/PlatformDriver.h>
#include <Kernel/Arch/x86_64/ISABus/IDEController.h>
#include <Kernel/Bus/PCI/Access.h>

namespace Kernel {

class ISAIDEDriver final : public PlatformDriver {
public:
    static void init();

    ISAIDEDriver()
        : PlatformDriver("ISAIDE"sv)
    {
    }
};

void ISAIDEDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new ISAIDEDriver()));
    all_instances().with([&driver](auto& list) {
        list.append(*driver);
    });

    // NOTE: If PCI is disabled, we assume that at least we have an ISA IDE controller
    // to probe and use
    if (PCI::Access::is_disabled()) {
        auto controller_or_error = ISAIDEController::initialize();
        if (controller_or_error.is_error()) {
            dmesgln("ISA-IDE: Failed to initialize ISA IDE controller due to {}", controller_or_error.release_error());
        } else {
            auto controller = controller_or_error.release_value();
            (void)controller.leak_ref();
        }
    }
        
}

PLATFORM_DEVICE_DRIVER(ISAIDEDriver);

}
