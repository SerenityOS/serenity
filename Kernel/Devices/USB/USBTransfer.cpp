/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/USB/USBTransfer.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel::USB {

RefPtr<Transfer> Transfer::try_create(Pipe& pipe, u16 len)
{
    auto vmobject = ContiguousVMObject::create_with_size(PAGE_SIZE);
    if (!vmobject)
        return nullptr;

    return adopt_ref_if_nonnull(new Transfer(pipe, len, *vmobject));
}

Transfer::Transfer(Pipe& pipe, u16 len, ContiguousVMObject& vmobject)
    : m_pipe(pipe)
    , m_transfer_data_size(len)
{
    // Initialize data buffer for transfer
    // This will definitely need to be refactored in the future, I doubt this will scale well...
    m_data_buffer = MemoryManager::the().allocate_kernel_region_with_vmobject(vmobject, PAGE_SIZE, "USB Transfer Buffer", Region::Access::Read | Region::Access::Write);
}

Transfer::~Transfer()
{
}

void Transfer::set_setup_packet(const USBRequestData& request)
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

}
