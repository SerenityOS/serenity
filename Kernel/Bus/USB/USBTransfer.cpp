/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBTransfer.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::USB {

ErrorOr<NonnullLockRefPtr<Transfer>> Transfer::try_create(Pipe& pipe, u16 length, USBDMAPool<USBDMAHandle>& dma_pool)
{
    auto dma_buffer = dma_pool.try_take_free_buffer();
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) Transfer(pipe, length, dma_pool, dma_buffer));
}

Transfer::Transfer(Pipe& pipe, u16 len, USBDMAPool<USBDMAHandle>& dma_pool, USBDMAHandle* dma_buffer)
    : m_pipe(pipe)
    , m_transfer_data_size(len)
    , m_dma_pool(dma_pool)
    , m_dma_buffer(dma_buffer)
{
}

Transfer::~Transfer()
{
    m_dma_pool.release_to_pool(m_dma_buffer);
}

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

ErrorOr<void> Transfer::write_buffer(u16 len, void* data)
{
    VERIFY(len <= m_dma_buffer->size);
    m_transfer_data_size = len;
    memcpy(buffer().as_ptr(), data, len);

    return {};
}

}
