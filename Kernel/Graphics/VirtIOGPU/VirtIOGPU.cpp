/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VirtIOGPU/VirtIOFrameBufferDevice.h>
#include <Kernel/Graphics/VirtIOGPU/VirtIOGPU.h>
#include <Kernel/Graphics/VirtIOGPU/VirtIOGPUConsole.h>

#define DEVICE_EVENTS_READ 0x0
#define DEVICE_EVENTS_CLEAR 0x4
#define DEVICE_NUM_SCANOUTS 0x8

namespace Kernel::Graphics {

VirtIOGPU::VirtIOGPU(PCI::Address address)
    : VirtIODevice(address, "VirtIOGPU")
    , m_scratch_space(MM.allocate_contiguous_kernel_region(32 * PAGE_SIZE, "VirtGPU Scratch Space", Region::Access::Read | Region::Access::Write))
{
    VERIFY(!!m_scratch_space);
    if (auto cfg = get_config(ConfigurationType::Device)) {
        m_device_configuration = cfg;
        bool success = negotiate_features([&](u64 supported_features) {
            u64 negotiated = 0;
            if (is_feature_set(supported_features, VIRTIO_GPU_F_VIRGL))
                dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: VIRGL is not yet supported!");
            if (is_feature_set(supported_features, VIRTIO_GPU_F_EDID))
                dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: EDID is not yet supported!");
            return negotiated;
        });
        if (success) {
            read_config_atomic([&]() {
                m_num_scanouts = config_read32(*cfg, DEVICE_NUM_SCANOUTS);
            });
            dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: num_scanouts: {}", m_num_scanouts);
            success = setup_queues(2); // CONTROLQ + CURSORQ
        }
        VERIFY(success);
        finish_init();
        Locker locker(m_operation_lock);
        // Get display information using VIRTIO_GPU_CMD_GET_DISPLAY_INFO
        query_display_information();
    } else {
        VERIFY_NOT_REACHED();
    }
}

VirtIOGPU::~VirtIOGPU()
{
}

void VirtIOGPU::create_framebuffer_devices()
{
    for (size_t i = 0; i < min(m_num_scanouts, VIRTIO_GPU_MAX_SCANOUTS); i++) {
        auto& scanout = m_scanouts[i];
        scanout.framebuffer = adopt_ref(*new VirtIOFrameBufferDevice(*this, i));
        scanout.console = Kernel::Graphics::VirtIOGPUConsole::initialize(scanout.framebuffer);
    }
}

bool VirtIOGPU::handle_device_config_change()
{
    return false;
}

void VirtIOGPU::handle_queue_update(u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Handle queue update");
    VERIFY(queue_index == CONTROLQ);

    auto& queue = get_queue(CONTROLQ);
    ScopedSpinLock queue_lock(queue.lock());
    queue.discard_used_buffers();
    m_outstanding_request.wake_all();
}

u32 VirtIOGPU::get_pending_events()
{
    return config_read32(*m_device_configuration, DEVICE_EVENTS_READ);
}

void VirtIOGPU::clear_pending_events(u32 event_bitmask)
{
    config_write32(*m_device_configuration, DEVICE_EVENTS_CLEAR, event_bitmask);
}

void VirtIOGPU::query_display_information()
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *(m_scratch_space->vaddr().as_ptr<VirtIOGPUCtrlHeader>());
    auto& response = *(m_scratch_space->vaddr().offset(sizeof(request)).as_ptr<VirtIOGPURespDisplayInfo>());

    populate_virtio_gpu_request_header(request, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_GET_DISPLAY_INFO, VIRTIO_GPU_FLAG_FENCE);

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    for (size_t i = 0; i < VIRTIO_GPU_MAX_SCANOUTS; ++i) {
        auto& scanout = m_scanouts[i].display_info;
        scanout = response.scanout_modes[i];
        dbgln_if(VIRTIO_DEBUG, "Scanout {}: enabled: {} x: {}, y: {}, width: {}, height: {}", i, !!scanout.enabled, scanout.rect.x, scanout.rect.y, scanout.rect.width, scanout.rect.height);
        if (scanout.enabled && !m_default_scanout.has_value())
            m_default_scanout = i;
    }
    VERIFY(m_default_scanout.has_value());
}

VirtIOGPUResourceID VirtIOGPU::create_2d_resource(VirtIOGPURect rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *(m_scratch_space->vaddr().as_ptr<VirtIOGPUResourceCreate2D>());
    auto& response = *((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr<VirtIOGPUCtrlHeader>()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_RESOURCE_CREATE_2D, VIRTIO_GPU_FLAG_FENCE);

    auto resource_id = allocate_resource_id();
    request.resource_id = resource_id.value();
    request.width = rect.width;
    request.height = rect.height;
    request.format = static_cast<u32>(VirtIOGPUFormats::VIRTIO_GPU_FORMAT_B8G8R8X8_UNORM);

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Allocated 2d resource with id {}", resource_id.value());
    return resource_id;
}

void VirtIOGPU::ensure_backing_storage(Region const& region, size_t buffer_offset, size_t buffer_length, VirtIOGPUResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());

    VERIFY(buffer_offset % PAGE_SIZE == 0);
    VERIFY(buffer_length % PAGE_SIZE == 0);
    auto first_page_index = buffer_offset / PAGE_SIZE;
    size_t num_mem_regions = buffer_length / PAGE_SIZE;

    // Send request
    auto& request = *(m_scratch_space->vaddr().as_ptr<VirtIOGPUResourceAttachBacking>());
    const size_t header_block_size = sizeof(request) + num_mem_regions * sizeof(VirtIOGPUMemEntry);
    auto& response = *((m_scratch_space->vaddr().offset(header_block_size).as_ptr<VirtIOGPUCtrlHeader>()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();
    request.num_entries = num_mem_regions;
    for (size_t i = 0; i < num_mem_regions; ++i) {
        request.entries[i].address = region.physical_page(first_page_index + i)->paddr().get();
        request.entries[i].length = PAGE_SIZE;
    }

    synchronous_virtio_gpu_command(start_of_scratch_space(), header_block_size, sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Allocated backing storage");
}

void VirtIOGPU::detach_backing_storage(VirtIOGPUResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *(m_scratch_space->vaddr().as_ptr<VirtIOGPUResourceDetachBacking>());
    auto& response = *((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr<VirtIOGPUCtrlHeader>()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_RESOURCE_DETACH_BACKING, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Detached backing storage");
}

void VirtIOGPU::set_scanout_resource(VirtIOGPUScanoutID scanout, VirtIOGPUResourceID resource_id, VirtIOGPURect rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *(m_scratch_space->vaddr().as_ptr<VirtIOGPUSetScanOut>());
    auto& response = *((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr<VirtIOGPUCtrlHeader>()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_SET_SCANOUT, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();
    request.scanout_id = scanout.value();
    request.rect = rect;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Set backing scanout");
}

void VirtIOGPU::transfer_framebuffer_data_to_host(VirtIOGPUScanoutID scanout, VirtIOGPURect const& dirty_rect, VirtIOGPUResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *(m_scratch_space->vaddr().as_ptr<VirtIOGPUTransferToHost2D>());
    auto& response = *((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr<VirtIOGPUCtrlHeader>()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D, VIRTIO_GPU_FLAG_FENCE);
    request.offset = (dirty_rect.x + (dirty_rect.y * m_scanouts[scanout.value()].display_info.rect.width)) * sizeof(u32);
    request.resource_id = resource_id.value();
    request.rect = dirty_rect;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
}

void VirtIOGPU::flush_displayed_image(VirtIOGPURect const& dirty_rect, VirtIOGPUResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *(m_scratch_space->vaddr().as_ptr<VirtIOGPUResourceFlush>());
    auto& response = *((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr<VirtIOGPUCtrlHeader>()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_RESOURCE_FLUSH, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();
    request.rect = dirty_rect;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
}

void VirtIOGPU::synchronous_virtio_gpu_command(PhysicalAddress buffer_start, size_t request_size, size_t response_size)
{
    VERIFY(m_operation_lock.is_locked());
    VERIFY(m_outstanding_request.is_empty());
    auto& queue = get_queue(CONTROLQ);
    {
        ScopedSpinLock lock(queue.lock());
        VirtIOQueueChain chain { queue };
        chain.add_buffer_to_chain(buffer_start, request_size, BufferType::DeviceReadable);
        chain.add_buffer_to_chain(buffer_start.offset(request_size), response_size, BufferType::DeviceWritable);
        supply_chain_and_notify(CONTROLQ, chain);
        full_memory_barrier();
    }
    m_outstanding_request.wait_forever();
}

void VirtIOGPU::populate_virtio_gpu_request_header(VirtIOGPUCtrlHeader& header, VirtIOGPUCtrlType ctrl_type, u32 flags)
{
    header.type = static_cast<u32>(ctrl_type);
    header.flags = flags;
    header.fence_id = 0;
    header.context_id = 0;
    header.padding = 0;
}

void VirtIOGPU::flush_dirty_window(VirtIOGPUScanoutID scanout, VirtIOGPURect const& dirty_rect, VirtIOGPUResourceID resource_id)
{
    Locker locker(m_operation_lock);
    transfer_framebuffer_data_to_host(scanout, dirty_rect, resource_id);
    flush_displayed_image(dirty_rect, resource_id);
}

VirtIOGPUResourceID VirtIOGPU::allocate_resource_id()
{
    VERIFY(m_operation_lock.is_locked());
    m_resource_id_counter = m_resource_id_counter.value() + 1;
    return m_resource_id_counter;
}

void VirtIOGPU::delete_resource(VirtIOGPUResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *(m_scratch_space->vaddr().as_ptr<VirtioGPUResourceUnref>());
    auto& response = *((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr<VirtIOGPUCtrlHeader>()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_RESOURCE_UNREF, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
}

}
