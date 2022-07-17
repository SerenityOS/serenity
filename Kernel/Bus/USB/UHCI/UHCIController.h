/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Platform.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/USB/UHCI/UHCIDescriptorPool.h>
#include <Kernel/Bus/USB/UHCI/UHCIDescriptorTypes.h>
#include <Kernel/Bus/USB/UHCI/UHCIRootHub.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel::USB {

class UHCIController final
    : public USBController
    , public PCI::Device
    , public IRQHandler {

    static constexpr u8 MAXIMUM_NUMBER_OF_TDS = 128; // Upper pool limit. This consumes the second page we have allocated
    static constexpr u8 MAXIMUM_NUMBER_OF_QHS = 64;
    static constexpr u8 NUMBER_OF_INTERRUPT_QHS = 11;

public:
    static constexpr u8 NUMBER_OF_ROOT_PORTS = 2;
    static ErrorOr<NonnullLockRefPtr<UHCIController>> try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier);
    virtual ~UHCIController() override;

    virtual StringView purpose() const override { return "UHCI"sv; }

    virtual ErrorOr<void> initialize() override;
    virtual ErrorOr<void> reset() override;
    virtual ErrorOr<void> stop() override;
    virtual ErrorOr<void> start() override;
    ErrorOr<void> spawn_port_process();

    virtual ErrorOr<size_t> submit_control_transfer(Transfer& transfer) override;
    virtual ErrorOr<size_t> submit_bulk_transfer(Transfer& transfer) override;

    void get_port_status(Badge<UHCIRootHub>, u8, HubStatus&);
    ErrorOr<void> set_port_feature(Badge<UHCIRootHub>, u8, HubFeatureSelector);
    ErrorOr<void> clear_port_feature(Badge<UHCIRootHub>, u8, HubFeatureSelector);

private:
    explicit UHCIController(PCI::DeviceIdentifier const& pci_device_identifier);

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

    virtual bool handle_irq(RegisterState const&) override;

    ErrorOr<void> create_structures();
    void setup_schedule();

    void enqueue_qh(QueueHead* transfer_queue, QueueHead* anchor);
    void dequeue_qh(QueueHead* transfer_queue);

    size_t poll_transfer_queue(QueueHead& transfer_queue);

    TransferDescriptor* create_transfer_descriptor(Pipe& pipe, PacketID direction, size_t data_len);
    ErrorOr<void> create_chain(Pipe& pipe, PacketID direction, Ptr32<u8>& buffer_address, size_t max_size, size_t transfer_size, TransferDescriptor** td_chain, TransferDescriptor** last_td);
    void free_descriptor_chain(TransferDescriptor* first_descriptor);

    QueueHead* allocate_queue_head();
    TransferDescriptor* allocate_transfer_descriptor();

    void reset_port(u8);

    IOAddress m_io_base;

    Spinlock m_schedule_lock;

    OwnPtr<UHCIRootHub> m_root_hub;
    OwnPtr<UHCIDescriptorPool<QueueHead>> m_queue_head_pool;
    OwnPtr<UHCIDescriptorPool<TransferDescriptor>> m_transfer_descriptor_pool;
    Vector<TransferDescriptor*> m_iso_td_list;

    QueueHead* m_schedule_begin_anchor;
    Array<QueueHead*, NUMBER_OF_INTERRUPT_QHS> m_interrupt_qh_anchor_arr;
    QueueHead* m_ls_control_qh_anchor;
    QueueHead* m_fs_control_qh_anchor;
    // Always final queue in the schedule, may loop back to previous QH for bandwidth
    // reclamation instead of actually terminating
    QueueHead* m_bulk_qh_anchor;

    OwnPtr<Memory::Region> m_framelist;
    OwnPtr<Memory::Region> m_isochronous_transfer_pool;

    // Bitfield containing whether a given port should signal a change in reset or not.
    u8 m_port_reset_change_statuses { 0 };

    // Bitfield containing whether a given port should signal a change in suspend or not.
    u8 m_port_suspend_change_statuses { 0 };
};
}
