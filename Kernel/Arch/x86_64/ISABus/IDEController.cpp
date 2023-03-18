/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86_64/ISABus/IDEController.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Devices/Storage/ATA/ATADiskDevice.h>
#include <Kernel/Devices/Storage/ATA/GenericIDE/Channel.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<ISAIDEController>> ISAIDEController::initialize()
{
    auto controller = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ISAIDEController()));
    TRY(controller->initialize_channels());
    return controller;
}

UNMAP_AFTER_INIT ISAIDEController::ISAIDEController()
{
}

UNMAP_AFTER_INIT ErrorOr<void> ISAIDEController::initialize_channels()
{
    auto primary_base_io_window = TRY(IOWindow::create_for_io_space(IOAddress(0x1F0), 8));
    auto primary_control_io_window = TRY(IOWindow::create_for_io_space(IOAddress(0x3F6), 4));
    auto secondary_base_io_window = TRY(IOWindow::create_for_io_space(IOAddress(0x170), 8));
    auto secondary_control_io_window = TRY(IOWindow::create_for_io_space(IOAddress(0x376), 4));

    auto initialize_and_enumerate = [](IDEChannel& channel) -> ErrorOr<void> {
        TRY(channel.allocate_resources_for_isa_ide_controller({}));
        TRY(channel.detect_connected_devices());
        return {};
    };

    auto primary_channel_io_window_group = IDEChannel::IOWindowGroup { move(primary_base_io_window), move(primary_control_io_window) };
    auto secondary_channel_io_window_group = IDEChannel::IOWindowGroup { move(secondary_base_io_window), move(secondary_control_io_window) };

    m_channels[0] = TRY(IDEChannel::create(*this, move(primary_channel_io_window_group), IDEChannel::ChannelType::Primary));
    TRY(initialize_and_enumerate(*m_channels[0]));
    m_channels[0]->enable_irq();

    m_channels[1] = TRY(IDEChannel::create(*this, move(secondary_channel_io_window_group), IDEChannel::ChannelType::Secondary));
    TRY(initialize_and_enumerate(*m_channels[1]));
    m_channels[1]->enable_irq();
    dbgln("ISA IDE controller detected and initialized");
    return {};
}

}
