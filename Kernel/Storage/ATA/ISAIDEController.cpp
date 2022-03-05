/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATA/ATADiskDevice.h>
#include <Kernel/Storage/ATA/BMIDEChannel.h>
#include <Kernel/Storage/ATA/ISAIDEController.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<ISAIDEController> ISAIDEController::initialize()
{
    return adopt_ref(*new ISAIDEController());
}

UNMAP_AFTER_INIT ISAIDEController::ISAIDEController()
{
    initialize_channels();
}

UNMAP_AFTER_INIT void ISAIDEController::initialize_channels()
{
    auto primary_base_io = IOAddress(0x1F0);
    auto primary_control_io = IOAddress(0x3F6);
    auto secondary_base_io = IOAddress(0x170);
    auto secondary_control_io = IOAddress(0x376);

    m_channels.append(IDEChannel::create(*this, { primary_base_io, primary_control_io }, IDEChannel::ChannelType::Primary));
    m_channels[0].enable_irq();

    m_channels.append(IDEChannel::create(*this, { secondary_base_io, secondary_control_io }, IDEChannel::ChannelType::Secondary));
    m_channels[1].enable_irq();
    dbgln("ISA IDE controller detected and initialized");
}

}
