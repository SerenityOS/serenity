/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/aarch64/RPi/SDHostController.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel::RPi {

SDHostController::SDHostController(Memory::TypedMapping<SD::HostControlRegisterMap volatile> registers)
    : m_registers(move(registers))
{
}

static constinit Array const compatibles_array = {
    "brcm,bcm2835-sdhci"sv,
};

DEVICETREE_DRIVER(BCM2835SDHCIController, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/mmc/brcm,iproc-sdhci.yaml
ErrorOr<void> BCM2835SDHCIController::probe(DeviceTree::Device const& device, StringView) const
{
    auto physical_address = TRY(device.get_resource(0)).paddr;

    DeviceTree::DeviceRecipe<NonnullRefPtr<StorageController>> recipe {
        name(),
        device.node_name(),
        [physical_address] -> ErrorOr<NonnullRefPtr<StorageController>> {
            auto registers = TRY(Memory::map_typed_writable<SD::HostControlRegisterMap volatile>(physical_address));
            auto sdhc = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SDHostController(move(registers))));
            TRY(sdhc->initialize());
            return sdhc;
        }
    };

    StorageManagement::add_recipe(move(recipe));

    return {};
}

}
