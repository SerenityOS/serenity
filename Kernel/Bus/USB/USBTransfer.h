/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/OwnPtr.h>
#include <Kernel/Bus/USB/PacketTypes.h>
#include <Kernel/Bus/USB/USBPipe.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/PhysicalRAMPage.h>
#include <Kernel/Memory/Region.h>

// TODO: Callback stuff in this class please!
namespace Kernel::USB {

class Transfer final : public AtomicRefCounted<Transfer> {
public:
    static ErrorOr<NonnullLockRefPtr<Transfer>> create(Pipe&, u16 length, Memory::Region& dma_buffer, USBAsyncCallback callback = nullptr);

    Transfer() = delete;
    ~Transfer();

    void set_setup_packet(USBRequestData const& request);
    void set_complete() { m_complete = true; }
    void set_error_occurred() { m_error_occurred = true; }

    ErrorOr<void> write_buffer(u16 len, void const* data);
    ErrorOr<void> write_buffer(u16 len, UserOrKernelBuffer const data);

    // `const` here makes sure we don't blow up by writing to a physical address
    USBRequestData const& request() const { return m_request; }
    Pipe const& pipe() const { return m_pipe; }
    Pipe& pipe() { return m_pipe; }
    VirtualAddress buffer() const { return m_dma_buffer.vaddr(); }
    PhysicalAddress buffer_physical() const { return m_dma_buffer.physical_page(0)->paddr(); }
    u16 transfer_data_size() const { return m_transfer_data_size; }
    bool complete() const { return m_complete; }
    bool error_occurred() const { return m_error_occurred; }

    void invoke_async_callback();

private:
    Transfer(Pipe& pipe, u16 len, Memory::Region& dma_buffer, USBAsyncCallback callback);
    Pipe& m_pipe;                    // Pipe that initiated this transfer
    Memory::Region& m_dma_buffer;    // DMA buffer
    USBRequestData m_request;        // USB request
    u16 m_transfer_data_size { 0 };  // Size of the transfer's data stage
    bool m_complete { false };       // Has this transfer been completed?
    bool m_error_occurred { false }; // Did an error occur during this transfer?
    USBAsyncCallback m_callback { nullptr };
};

}
