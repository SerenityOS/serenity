/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#include <AK/NonnullOwnPtr.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/USB/UHCIDescriptorTypes.h>
#include <Kernel/Bus/USB/UHCIRootHub.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/IO.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel::USB {

class UHCIController final
    : public USBController
    , public PCI::Device {

public:
    static constexpr u8 NUMBER_OF_ROOT_PORTS = 2;
    static KResultOr<NonnullRefPtr<UHCIController>> try_to_initialize(PCI::Address address);
    virtual ~UHCIController() override;

    virtual StringView purpose() const override { return "UHCI"; }

    virtual KResult initialize() override;
    virtual KResult reset() override;
    virtual KResult stop() override;
    virtual KResult start() override;
    void spawn_port_proc();

    void do_debug_transfer();

    virtual KResultOr<size_t> submit_control_transfer(Transfer& transfer) override;

    virtual RefPtr<USB::Device> const get_device_at_port(USB::Device::PortNumber) override;
    virtual RefPtr<USB::Device> const get_device_from_address(u8 device_address) override;

    void get_port_status(Badge<UHCIRootHub>, u8, HubStatus&);
    KResult set_port_feature(Badge<UHCIRootHub>, u8, HubFeatureSelector);
    KResult clear_port_feature(Badge<UHCIRootHub>, u8, HubFeatureSelector);

private:
    explicit UHCIController(PCI::Address);

    u16 read_usbcmd() { return m_io_base.offset(0).in<u16>(); }
    u16 read_usbsts() { return m_io_base.offset(0x2).in<u16>(); }
    u16 read_usbintr() { return m_io_base.offset(0x4).in<u16>(); }
    u16 read_frnum() { return m_io_base.offset(0x6).in<u16>(); }
    u32 read_flbaseadd() { return m_io_base.offset(0x8).in<u32>(); }
    u8 read_sofmod() { return m_io_base.offset(0xc).in<u8>(); }
    u16 read_portsc1() { return m_io_base.offset(0x10).in<u16>(); }
    u16 read_portsc2() { return m_io_base.offset(0x12).in<u16>(); }

    void write_usbcmd(u16 value) { m_io_base.offset(0).out(value); }
    void write_usbsts(u16 value) { m_io_base.offset(0x2).out(value); }
    void write_usbintr(u16 value) { m_io_base.offset(0x4).out(value); }
    void write_frnum(u16 value) { m_io_base.offset(0x6).out(value); }
    void write_flbaseadd(u32 value) { m_io_base.offset(0x8).out(value); }
    void write_sofmod(u8 value) { m_io_base.offset(0xc).out(value); }
    void write_portsc1(u16 value) { m_io_base.offset(0x10).out(value); }
    void write_portsc2(u16 value) { m_io_base.offset(0x12).out(value); }

    virtual bool handle_irq(const RegisterState&) override;

    void create_structures();
    void setup_schedule();
    size_t poll_transfer_queue(QueueHead& transfer_queue);

    TransferDescriptor* create_transfer_descriptor(Pipe& pipe, PacketID direction, size_t data_len);
    KResult create_chain(Pipe& pipe, PacketID direction, Ptr32<u8>& buffer_address, size_t max_size, size_t transfer_size, TransferDescriptor** td_chain, TransferDescriptor** last_td);
    void free_descriptor_chain(TransferDescriptor* first_descriptor);

    QueueHead* allocate_queue_head() const;
    TransferDescriptor* allocate_transfer_descriptor() const;

    void reset_port(u8);

private:
    IOAddress m_io_base;

    OwnPtr<UHCIRootHub> m_root_hub;

    Vector<QueueHead*> m_free_qh_pool;
    Vector<TransferDescriptor*> m_free_td_pool;
    Vector<TransferDescriptor*> m_iso_td_list;

    QueueHead* m_interrupt_transfer_queue;
    QueueHead* m_lowspeed_control_qh;
    QueueHead* m_fullspeed_control_qh;
    QueueHead* m_bulk_qh;
    QueueHead* m_dummy_qh; // Needed for PIIX4 hack

    OwnPtr<Memory::Region> m_framelist;
    OwnPtr<Memory::Region> m_qh_pool;
    OwnPtr<Memory::Region> m_td_pool;

    // Bitfield containing whether a given port should signal a change in reset or not.
    u8 m_port_reset_change_statuses { 0 };

    // Bitfield containing whether a given port should signal a change in suspend or not.
    u8 m_port_suspend_change_statuses { 0 };

    Array<RefPtr<USB::Device>, NUMBER_OF_ROOT_PORTS> m_devices; // Devices connected to the root ports (of which there are two)
};

}
