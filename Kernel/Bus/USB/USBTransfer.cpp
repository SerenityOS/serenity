/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBTransfer.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::USB {

ErrorOr<NonnullLockRefPtr<Transfer>> Transfer::create(Pipe& pipe, u16 length, Memory::Region& dma_buffer, USBAsyncCallback callback)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) Transfer(pipe, length, dma_buffer, move(callback)));
}

Transfer::Transfer(Pipe& pipe, u16 len, Memory::Region& dma_buffer, USBAsyncCallback callback)
    : m_pipe(pipe)
    , m_dma_buffer(dma_buffer)
    , m_transfer_data_size(len)
    , m_callback(move(callback))
{
}

Transfer::~Transfer() = default;

void Transfer::set_setup_packet(USBRequestData const& request)
{
    // Kind of a nasty hack... Because the kernel isn't in the business
    // of handing out physical pointers that we can directly write to,
    // we set the address of the setup packet to be the first 8 bytes of
    // the data buffer, which we then set to the physical address.
    auto* request_data = reinterpret_cast<USBRequestData*>(buffer().as_ptr());

    request_data->request_type = request.request_type;
    request_data->request = request.request;
    request_data->value = request.value;
    request_data->index = request.index;
    request_data->length = request.length;

    m_request = request;
}

ErrorOr<void> Transfer::write_buffer(u16 len, void const* data)
{
    VERIFY(len <= m_dma_buffer.size());
    m_transfer_data_size = len;
    memcpy(buffer().as_ptr(), data, len);

    return {};
}

ErrorOr<void> Transfer::write_buffer(u16 len, UserOrKernelBuffer const data)
{
    VERIFY(len <= m_dma_buffer.size());
    m_transfer_data_size = len;
    return data.read(buffer().as_ptr(), len);
}

void Transfer::invoke_async_callback()
{
    if (transfer_data_size() == 0)
        return;
    if (m_callback)
        m_callback(this);
}

}
