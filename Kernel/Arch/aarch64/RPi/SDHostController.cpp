/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/aarch64/RPi/SDHostController.h>
#include <Kernel/Arch/aarch64/RPi/Timer.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel::RPi {

SDHostController::SDHostController(Model model, DeviceTree::Device const& device, Memory::TypedMapping<SD::HostControlRegisterMap volatile> registers)
    : m_model(model)
    , m_device(device)
    , m_registers(move(registers))
{
}

ErrorOr<u32> SDHostController::retrieve_sd_clock_frequency()
{
    if (m_model == Model::BCM2712_SDHCI) {
        // On the Pi 5, the "Get clock rate" mailbox message doesn't appear to work if the firmware didn't initialize the SD host controller.
        // In its devicetree, the EMMC2 clock is represented by a "fixed-clock" node, so get the frequency from there.
        // FIXME: Add a proper abstraction for devicetree clock hierarchies.

        auto maybe_clocks = m_device.node().get_property("clocks"sv);
        if (!maybe_clocks.has_value() || maybe_clocks->size() != sizeof(u32))
            return ENOTSUP;

        auto clock_phandle = maybe_clocks->as<u32>();

        auto const* clock = DeviceTree::get().phandle(clock_phandle);
        if (clock == nullptr)
            return EINVAL;

        if (!clock->is_compatible_with("fixed-clock"sv))
            return ENOTSUP;

        auto maybe_clock_frequency = clock->get_property("clock-frequency"sv);
        if (!maybe_clock_frequency.has_value() || maybe_clock_frequency->size() != sizeof(u32))
            return EINVAL;

        return maybe_clock_frequency->as<u32>();
    }

    return Timer::get_clock_rate(Timer::ClockID::EMMC);
}

static constinit Array const compatibles_array = {
    "brcm,bcm2712-sdhci"sv,
    "brcm,bcm2835-sdhci"sv,
};

DEVICETREE_DRIVER(BCM2835SDHCIController, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/mmc/brcm,iproc-sdhci.yaml
ErrorOr<void> BCM2835SDHCIController::probe(DeviceTree::Device const& device, StringView compatible) const
{
    SDHostController::Model model;
    if (compatible == "brcm,bcm2712-sdhci"sv)
        model = SDHostController::Model::BCM2712_SDHCI;
    else if (compatible == "brcm,bcm2835-sdhci"sv)
        model = SDHostController::Model::BCM2835_SDHCI;
    else
        VERIFY_NOT_REACHED();

    auto physical_address = TRY(device.get_resource(0)).paddr;

    auto registers = TRY(Memory::map_typed_writable<SD::HostControlRegisterMap volatile>(physical_address));
    auto sdhc = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SDHostController(model, device, move(registers))));
    TRY(sdhc->initialize());

    TRY(StorageManagement::the().add_controller(*sdhc));

    return {};
}

}
