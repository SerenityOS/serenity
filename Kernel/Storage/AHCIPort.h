/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
#include <Kernel/Storage/AHCI.h>
#include <Kernel/Storage/AHCIPortHandler.h>
#include <Kernel/Storage/StorageDevice.h>
#include <Kernel/VM/PhysicalPage.h>
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
    void handle_interrupt();

private:
    UNMAP_AFTER_INIT AHCIPort(const AHCIPortHandler&, volatile AHCI::PortRegisters&, u32 port_index);

    ALWAYS_INLINE void clear_sata_error_register() const;

    void eject();

    const char* try_disambiguate_sata_status();

    bool initiate_sata_reset();
    void rebase();
    void recover_from_fatal_error();
    bool shutdown();
    ALWAYS_INLINE void spin_up() const;
    ALWAYS_INLINE void power_on() const;

    void start_request(AsyncBlockDeviceRequest&);
    void complete_current_request(AsyncDeviceRequest::RequestResult);
    bool access_device(AsyncBlockDeviceRequest::RequestType, u64 lba, u8 block_count);

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

    ALWAYS_INLINE bool is_interface_disabled() const { return (m_port_registers.ssts & 0xf) == 4; };

    // Data members

    EntropySource m_entropy_source;
    AsyncBlockDeviceRequest* m_current_request { nullptr };
    u32 m_current_request_block_index { 0 };
    bool m_current_request_uses_dma { false };
    bool m_current_request_flushing_cache { false };
    SpinLock<u8> m_lock;

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
    bool m_disabled_by_firmware { false };
};
}
