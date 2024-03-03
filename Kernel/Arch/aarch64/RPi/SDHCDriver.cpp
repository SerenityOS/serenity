/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/PlatformDriver.h>
#include <Kernel/Arch/aarch64/RPi/SDHostController.h>
#include <Kernel/Bus/PCI/Access.h>

namespace Kernel::RPi {

class SDHCDriver final : public PlatformDriver {
public:
    static void init();

    SDHCDriver()
        : PlatformDriver("RPi::SDHC"sv)
    {
    }
};

void SDHCDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new SDHCDriver()));
    all_instances().with([&driver](auto& list) {
        list.append(*driver);
    });

    uto& rpi_sdhc = SDHostController::the();
    if (auto maybe_error = rpi_sdhc.initialize(); maybe_error.is_error()) {
        dmesgln("Unable to initialize RaspberryPi's SD Host Controller: {}", maybe_error.error());
    }
}

PLATFORM_DEVICE_DRIVER(SDHCDriver);

}
