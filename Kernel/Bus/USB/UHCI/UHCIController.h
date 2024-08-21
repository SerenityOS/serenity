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
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/USB/UHCI/UHCIDescriptorPool.h>
#include <Kernel/Bus/USB/UHCI/UHCIDescriptorTypes.h>
#include <Kernel/Bus/USB/UHCI/UHCIRootHub.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel::USB {

class UHCIController final
    : public USBController
    , public PCI::Device
    , public IRQHandler {

    static constexpr u8 MAXIMUM_NUMBER_OF_TDS = 128; // Upper pool limit. This consumes the second page we have allocated
    static constexpr u8 MAXIMUM_NUMBER_OF_QHS = 64;

public:
    static constexpr u8 NUMBER_OF_ROOT_PORTS = 2;
    static ErrorOr<NonnullLockRefPtr<UHCIController>> try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier);
    virtual ~UHCIController() override;

    virtual StringView purpose() const override { return "UHCI"sv; }
    virtual StringView device_name() const override { return purpose(); }

    virtual ErrorOr<void> initialize() override;
    virtual ErrorOr<void> reset() override;
    virtual ErrorOr<void> stop() override;
    virtual ErrorOr<void> start() override;
    ErrorOr<void> spawn_async_poll_process();
    ErrorOr<void> spawn_port_process();

    ErrorOr<QueueHead*> create_transfer_queue(Transfer& transfer);
    ErrorOr<void> submit_async_transfer(NonnullOwnPtr<AsyncTransferHandle> async_handle, QueueHead* anchor, QueueHead* transfer_queue);

    virtual void cancel_async_transfer(NonnullLockRefPtr<Transfer> transfer) override;
    virtual ErrorOr<size_t> submit_control_transfer(Transfer& transfer) override;
    virtual ErrorOr<size_t> submit_bulk_transfer(Transfer& transfer) override;
    virtual ErrorOr<void> submit_async_interrupt_transfer(NonnullLockRefPtr<Transfer> transfer, u16 ms_interval) override;

    virtual ErrorOr<void> initialize_device(USB::Device&) override;

    void get_port_status(Badge<UHCIRootHub>, u8, HubStatus&);
    ErrorOr<void> set_port_feature(Badge<UHCIRootHub>, u8, HubFeatureSelector);
    ErrorOr<void> clear_port_feature(Badge<UHCIRootHub>, u8, HubFeatureSelector);

private:
    UHCIController(PCI::DeviceIdentifier const& pci_device_identifier, NonnullOwnPtr<IOWindow> registers_io_window);

    u16 read_usbcmd() { return m_registers_io_window->read16(0); }
    u16 read_usbsts() { return m_registers_io_window->read16(0x2); }
    u16 read_usbintr() { return m_registers_io_window->read16(0x4); }
    u16 read_frnum() { return m_registers_io_window->read16(0x6); }
    u32 read_flbaseadd() { return m_registers_io_window->read32(0x8); }
    u8 read_sofmod() { return m_registers_io_window->read8(0xc); }
    u16 read_portsc1() { return m_registers_io_window->read16(0x10); }
    u16 read_portsc2() { return m_registers_io_window->read16(0x12); }

    void write_usbcmd(u16 value) { m_registers_io_window->write16(0, value); }
    void write_usbsts(u16 value) { m_registers_io_window->write16(0x2, value); }
    void write_usbintr(u16 value) { m_registers_io_window->write16(0x4, value); }
    void write_frnum(u16 value) { m_registers_io_window->write16(0x6, value); }
    void write_flbaseadd(u32 value) { m_registers_io_window->write32(0x8, value); }
    void write_sofmod(u8 value) { m_registers_io_window->write8(0xc, value); }
    void write_portsc1(u16 value) { m_registers_io_window->write16(0x10, value); }
    void write_portsc2(u16 value) { m_registers_io_window->write16(0x12, value); }

    virtual bool handle_irq() override;

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

    u8 allocate_address();

    NonnullOwnPtr<IOWindow> m_registers_io_window;

    Spinlock<LockRank::None> m_async_lock {};
    Spinlock<LockRank::None> m_schedule_lock {};

    OwnPtr<UHCIRootHub> m_root_hub;
    OwnPtr<UHCIDescriptorPool<QueueHead>> m_queue_head_pool;
    OwnPtr<UHCIDescriptorPool<TransferDescriptor>> m_transfer_descriptor_pool;
    Vector<TransferDescriptor*> m_iso_td_list;
    Array<OwnPtr<AsyncTransferHandle>, MAXIMUM_NUMBER_OF_QHS> m_active_async_transfers;

    QueueHead* m_schedule_begin_anchor;
    QueueHead* m_interrupt_qh_anchor;
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

    u8 m_next_device_index { 1 };
};
}
