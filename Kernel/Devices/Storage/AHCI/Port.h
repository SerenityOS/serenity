/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Storage/AHCI/ATADevice.h>
#include <Kernel/Devices/Storage/AHCI/Definitions.h>
#include <Kernel/Devices/Storage/AHCI/InterruptHandler.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/LockWeakPtr.h>
#include <Kernel/Library/LockWeakable.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/PhysicalRAMPage.h>
#include <Kernel/Memory/ScatterGatherList.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class AHCIInterruptHandler;
class AHCIPort
    : public AtomicRefCounted<AHCIPort>
    , public LockWeakable<AHCIPort> {
    friend class AHCIController;

public:
    static ErrorOr<NonnullLockRefPtr<AHCIPort>> create(AHCIController const&, AHCI::HBADefinedCapabilities, volatile AHCI::PortRegisters&, u32 port_index);

    u32 port_index() const { return m_port_index; }
    u32 representative_port_index() const { return port_index() + 1; }
    bool is_operable() const;
    bool is_atapi_attached() const { return m_port_registers.sig == (u32)ATA::DeviceSignature::ATAPI; }

    RefPtr<StorageDevice> connected_device() const { return m_connected_device; }

    bool reset();
    bool initialize_without_reset();
    void handle_interrupt();

private:
    ErrorOr<void> allocate_resources_and_initialize_ports();

    bool is_phy_enabled() const { return (m_port_registers.ssts & 0xf) == 3; }
    bool initialize();

    AHCIPort(AHCIController const&, NonnullRefPtr<Memory::PhysicalRAMPage> identify_buffer_page, AHCI::HBADefinedCapabilities, volatile AHCI::PortRegisters&, u32 port_index);

    ALWAYS_INLINE void clear_sata_error_register() const;

    char const* try_disambiguate_sata_status();
    void try_disambiguate_sata_error();

    bool initiate_sata_reset();
    void rebase();
    void recover_from_fatal_error();
    bool shutdown();
    ALWAYS_INLINE void spin_up() const;
    ALWAYS_INLINE void power_on() const;

    void start_request(AsyncBlockDeviceRequest&);
    void complete_current_request(AsyncDeviceRequest::RequestResult);
    bool access_device(AsyncBlockDeviceRequest::RequestType, u64 lba, u8 block_count);
    size_t calculate_descriptors_count(size_t block_count) const;
    [[nodiscard]] Optional<AsyncDeviceRequest::RequestResult> prepare_and_set_scatter_list(AsyncBlockDeviceRequest& request);

    ALWAYS_INLINE bool is_interrupts_enabled() const;

    bool spin_until_ready() const;

    bool identify_device();

    ALWAYS_INLINE void start_command_list_processing() const;
    ALWAYS_INLINE void mark_command_header_ready_to_process(u8 command_header_index) const;
    ALWAYS_INLINE void stop_command_list_processing() const;

    ALWAYS_INLINE void start_fis_receiving() const;
    ALWAYS_INLINE void stop_fis_receiving() const;

    ALWAYS_INLINE void set_active_state() const;
    ALWAYS_INLINE void set_sleep_state() const;

    void set_interface_state(AHCI::DeviceDetectionInitialization);

    Optional<u8> try_to_find_unused_command_header();

    ALWAYS_INLINE bool is_interface_disabled() const { return (m_port_registers.ssts & 0xf) == 4; }

    ALWAYS_INLINE void wait_until_condition_met_or_timeout(size_t delay_in_microseconds, size_t retries, Function<bool(void)> condition_being_met) const;

    // Data members

    EntropySource m_entropy_source;
    LockRefPtr<AsyncBlockDeviceRequest> m_current_request;
    Spinlock<LockRank::None> m_hard_lock {};
    Mutex m_lock { "AHCIPort"sv };

    mutable bool m_wait_for_completion { false };

    Vector<NonnullRefPtr<Memory::PhysicalRAMPage>> m_dma_buffers;
    Vector<NonnullRefPtr<Memory::PhysicalRAMPage>> m_command_table_pages;
    RefPtr<Memory::PhysicalRAMPage> m_command_list_page;
    OwnPtr<Memory::Region> m_command_list_region;
    RefPtr<Memory::PhysicalRAMPage> m_fis_receive_page;
    // FIXME: This should have some locking once we actually support hotplug
    // properly.
    RefPtr<ATADevice> m_connected_device;

    u32 m_port_index;

    // Note: Ideally the AHCIController should be the only object to hold this data
    // but because using the m_parent_controller means we need to take a strong ref,
    // it's probably better to just "cache" this here instead.
    AHCI::HBADefinedCapabilities const m_hba_capabilities;

    NonnullRefPtr<Memory::PhysicalRAMPage> const m_identify_buffer_page;

    volatile AHCI::PortRegisters& m_port_registers;
    NonnullRefPtr<AHCIController> const m_parent_controller;
    AHCI::PortInterruptStatusBitField m_interrupt_status;
    AHCI::PortInterruptEnableBitField m_interrupt_enable;

    LockRefPtr<Memory::ScatterGatherList> m_current_scatter_list;
    bool m_disabled_by_firmware { false };
};
}
