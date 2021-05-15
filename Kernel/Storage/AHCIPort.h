/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>
#include <Kernel/SpinLock.h>
#include <Kernel/Storage/AHCI.h>
#include <Kernel/Storage/AHCIPortHandler.h>
#include <Kernel/Storage/StorageDevice.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/ScatterGatherList.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class AHCIPortHandler;
class SATADiskDevice;
class AHCIPort : public RefCounted<AHCIPort> {
    friend class AHCIPortHandler;
    friend class SATADiskDevice;

public:
    UNMAP_AFTER_INIT static NonnullRefPtr<AHCIPort> create(const AHCIPortHandler&, volatile AHCI::PortRegisters&, u32 port_index);

    u32 port_index() const { return m_port_index; }
    u32 representative_port_index() const { return port_index() + 1; }
    bool is_operable() const;
    bool is_hot_pluggable() const;
    bool is_atapi_attached() const { return m_port_registers.sig == (u32)AHCI::DeviceSignature::ATAPI; };

    RefPtr<StorageDevice> connected_device() const { return m_connected_device; }

    bool reset();
    UNMAP_AFTER_INIT bool initialize_without_reset();
    void handle_interrupt();

private:
    bool is_phy_enabled() const { return (m_port_registers.ssts & 0xf) == 3; }
    bool initialize(ScopedSpinLock<SpinLock<u8>>&);

    UNMAP_AFTER_INIT AHCIPort(const AHCIPortHandler&, volatile AHCI::PortRegisters&, u32 port_index);

    ALWAYS_INLINE void clear_sata_error_register() const;

    void eject();

    const char* try_disambiguate_sata_status();
    void try_disambiguate_sata_error();

    bool initiate_sata_reset(ScopedSpinLock<SpinLock<u8>>&);
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

    bool identify_device(ScopedSpinLock<SpinLock<u8>>&);

    ALWAYS_INLINE void start_command_list_processing() const;
    ALWAYS_INLINE void mark_command_header_ready_to_process(u8 command_header_index) const;
    ALWAYS_INLINE void stop_command_list_processing() const;

    ALWAYS_INLINE void start_fis_receiving() const;
    ALWAYS_INLINE void stop_fis_receiving() const;

    ALWAYS_INLINE void set_active_state() const;
    ALWAYS_INLINE void set_sleep_state() const;

    void set_interface_state(AHCI::DeviceDetectionInitialization);

    Optional<u8> try_to_find_unused_command_header();

    ALWAYS_INLINE bool is_interface_disabled() const { return (m_port_registers.ssts & 0xf) == 4; };

    // Data members

    EntropySource m_entropy_source;
    RefPtr<AsyncBlockDeviceRequest> m_current_request;
    SpinLock<u8> m_hard_lock;
    Lock m_lock { "AHCIPort" };

    mutable bool m_wait_for_completion { false };
    bool m_wait_connect_for_completion { false };

    NonnullRefPtrVector<PhysicalPage> m_dma_buffers;
    NonnullRefPtrVector<PhysicalPage> m_command_table_pages;
    RefPtr<PhysicalPage> m_command_list_page;
    OwnPtr<Region> m_command_list_region;
    RefPtr<PhysicalPage> m_fis_receive_page;
    RefPtr<StorageDevice> m_connected_device;

    u32 m_port_index;
    volatile AHCI::PortRegisters& m_port_registers;
    NonnullRefPtr<AHCIPortHandler> m_parent_handler;
    AHCI::PortInterruptStatusBitField m_interrupt_status;
    AHCI::PortInterruptEnableBitField m_interrupt_enable;

    RefPtr<ScatterGatherList> m_current_scatter_list;
    bool m_disabled_by_firmware { false };
};
}
