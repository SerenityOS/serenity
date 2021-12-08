/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Bus/USB/PacketTypes.h>
#include <Kernel/Bus/USB/USBPipe.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/Memory/Region.h>

// TODO: Callback stuff in this class please!
namespace Kernel::USB {

class Transfer : public RefCounted<Transfer> {
public:
    static ErrorOr<NonnullRefPtr<Transfer>> try_create(Pipe&, u16 length);

    Transfer() = delete;
    ~Transfer();

    void set_setup_packet(const USBRequestData& request);
    void set_complete() { m_complete = true; }
    void set_error_occurred() { m_error_occurred = true; }

    // `const` here makes sure we don't blow up by writing to a physical address
    const USBRequestData& request() const { return m_request; }
    const Pipe& pipe() const { return m_pipe; }
    Pipe& pipe() { return m_pipe; }
    VirtualAddress buffer() const { return m_data_buffer->vaddr(); }
    PhysicalAddress buffer_physical() const { return m_data_buffer->physical_page(0)->paddr(); }
    u16 transfer_data_size() const { return m_transfer_data_size; }
    bool complete() const { return m_complete; }
    bool error_occurred() const { return m_error_occurred; }

private:
    Transfer(Pipe& pipe, u16 len, NonnullOwnPtr<Memory::Region>);
    Pipe& m_pipe;                                // Pipe that initiated this transfer
    USBRequestData m_request;                    // USB request
    NonnullOwnPtr<Memory::Region> m_data_buffer; // DMA Data buffer for transaction
    u16 m_transfer_data_size { 0 };              // Size of the transfer's data stage
    bool m_complete { false };                   // Has this transfer been completed?
    bool m_error_occurred { false };             // Did an error occur during this transfer?
};
}
