/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/Drivers/USBDriver.h>
#include <Kernel/Bus/USB/USBInterface.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Devices/Storage/USB/BOT/BulkSCSIInterface.h>
#include <Kernel/Devices/Storage/USB/UAS/UASInterface.h>

namespace Kernel::USB {

class MassStorageDriver final : public Driver {
public:
    MassStorageDriver()
        : Driver("USB MassStorage"sv)
    {
    }

    static void init();

    virtual ~MassStorageDriver() override = default;

    virtual ErrorOr<void> probe(USB::Device&) override;
    virtual void detach(USB::Device&) override;

private:
    BulkSCSIInterface::List m_bot_interfaces;
    UASInterface::List m_uas_interfaces;

    ErrorOr<void> initialise_bulk_only_device(USB::Device&, USBInterface const&);
    ErrorOr<void> initialise_uas_device(USB::Device&, USBInterface const&);
};

}
