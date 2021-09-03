/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/BinaryBufferWriter.h>
#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/Graphics/VirtIOGPU/FrameBufferDevice.h>
#include <Kernel/Graphics/VirtIOGPU/GPU.h>

#define DEVICE_EVENTS_READ 0x0
#define DEVICE_EVENTS_CLEAR 0x4
#define DEVICE_NUM_SCANOUTS 0x8

namespace Kernel::Graphics::VirtIOGPU {

GPU::GPU(PCI::Address address)
    : VirtIO::Device(address)
    , m_scratch_space(MM.allocate_contiguous_kernel_region(32 * PAGE_SIZE, "VirtGPU Scratch Space", Memory::Region::Access::ReadWrite))
{
    VERIFY(!!m_scratch_space);
    if (auto cfg = get_config(VirtIO::ConfigurationType::Device)) {
        m_device_configuration = cfg;
        bool success = negotiate_features([&](u64 supported_features) {
            u64 negotiated = 0;
            if (is_feature_set(supported_features, VIRTIO_GPU_F_VIRGL))
                dbgln_if(VIRTIO_DEBUG, "GPU: VIRGL is not yet supported!");
            if (is_feature_set(supported_features, VIRTIO_GPU_F_EDID))
                dbgln_if(VIRTIO_DEBUG, "GPU: EDID is not yet supported!");
            return negotiated;
        });
        if (success) {
            read_config_atomic([&]() {
                m_num_scanouts = config_read32(*cfg, DEVICE_NUM_SCANOUTS);
            });
            dbgln_if(VIRTIO_DEBUG, "GPU: num_scanouts: {}", m_num_scanouts);
            success = setup_queues(2); // CONTROLQ + CURSORQ
        }
        VERIFY(success);
        finish_init();
        MutexLocker locker(m_operation_lock);
        // Get display information using VIRTIO_GPU_CMD_GET_DISPLAY_INFO
        query_display_information();
    } else {
        VERIFY_NOT_REACHED();
    }
}

GPU::~GPU()
{
}

void GPU::create_framebuffer_devices()
{
    for (size_t i = 0; i < min(m_num_scanouts, VIRTIO_GPU_MAX_SCANOUTS); i++) {
        auto& scanout = m_scanouts[i];
        scanout.framebuffer = adopt_ref(*new VirtIOGPU::FrameBufferDevice(*this, i));
        scanout.console = Kernel::Graphics::VirtIOGPU::Console::initialize(scanout.framebuffer);
    }
}

bool GPU::handle_device_config_change()
{
    auto events = get_pending_events();
    if (events & VIRTIO_GPU_EVENT_DISPLAY) {
        // The host window was resized, in SerenityOS we completely ignore this event
        dbgln_if(VIRTIO_DEBUG, "VirtIO::GPU: Ignoring virtio gpu display resize event");
        clear_pending_events(VIRTIO_GPU_EVENT_DISPLAY);
    }
    if (events & ~VIRTIO_GPU_EVENT_DISPLAY) {
        dbgln("GPU: Got unknown device config change event: {:#x}", events);
        return false;
    }
    return true;
}

void GPU::handle_queue_update(u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "GPU: Handle queue update");
    VERIFY(queue_index == CONTROLQ);

    auto& queue = get_queue(CONTROLQ);
    SpinlockLocker queue_lock(queue.lock());
    queue.discard_used_buffers();
    m_outstanding_request.wake_all();
}

u32 GPU::get_pending_events()
{
    return config_read32(*m_device_configuration, DEVICE_EVENTS_READ);
}

void GPU::clear_pending_events(u32 event_bitmask)
{
    config_write32(*m_device_configuration, DEVICE_EVENTS_CLEAR, event_bitmask);
}

void GPU::query_display_information()
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Protocol::ControlHeader>();
    populate_virtio_gpu_request_header(request, Protocol::CommandType::VIRTIO_GPU_CMD_GET_DISPLAY_INFO, VIRTIO_GPU_FLAG_FENCE);
    auto& response = writer.append_structure<Protocol::DisplayInfoResponse>();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    for (size_t i = 0; i < VIRTIO_GPU_MAX_SCANOUTS; ++i) {
        auto& scanout = m_scanouts[i].display_info;
        scanout = response.scanout_modes[i];
        dbgln_if(VIRTIO_DEBUG, "GPU: Scanout {}: enabled: {} x: {}, y: {}, width: {}, height: {}", i, !!scanout.enabled, scanout.rect.x, scanout.rect.y, scanout.rect.width, scanout.rect.height);
        if (scanout.enabled && !m_default_scanout.has_value())
            m_default_scanout = i;
    }
    VERIFY(m_default_scanout.has_value());
}

ResourceID GPU::create_2d_resource(Protocol::Rect rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Protocol::ResourceCreate2D>();
    auto& response = writer.append_structure<Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_CREATE_2D, VIRTIO_GPU_FLAG_FENCE);

    auto resource_id = allocate_resource_id();
    request.resource_id = resource_id.value();
    request.width = rect.width;
    request.height = rect.height;
    request.format = static_cast<u32>(Protocol::TextureFormat::VIRTIO_GPU_FORMAT_B8G8R8X8_UNORM);

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "GPU: Allocated 2d resource with id {}", resource_id.value());
    return resource_id;
}

void GPU::ensure_backing_storage(Memory::Region const& region, size_t buffer_offset, size_t buffer_length, ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());

    VERIFY(buffer_offset % PAGE_SIZE == 0);
    VERIFY(buffer_length % PAGE_SIZE == 0);
    auto first_page_index = buffer_offset / PAGE_SIZE;
    size_t num_mem_regions = buffer_length / PAGE_SIZE;

    // Send request
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Protocol::ResourceAttachBacking>();
    const size_t header_block_size = sizeof(request) + num_mem_regions * sizeof(Protocol::MemoryEntry);

    populate_virtio_gpu_request_header(request.header, Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();
    request.num_entries = num_mem_regions;
    for (size_t i = 0; i < num_mem_regions; ++i) {
        auto& memory_entry = writer.append_structure<Protocol::MemoryEntry>();
        memory_entry.address = region.physical_page(first_page_index + i)->paddr().get();
        memory_entry.length = PAGE_SIZE;
    }

    auto& response = writer.append_structure<Protocol::ControlHeader>();

    synchronous_virtio_gpu_command(start_of_scratch_space(), header_block_size, sizeof(response));

    VERIFY(response.type == static_cast<u32>(Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "GPU: Allocated backing storage");
}

void GPU::detach_backing_storage(ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Protocol::ResourceDetachBacking>();
    auto& response = writer.append_structure<Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_DETACH_BACKING, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "GPU: Detached backing storage");
}

void GPU::set_scanout_resource(ScanoutID scanout, ResourceID resource_id, Protocol::Rect rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Protocol::SetScanOut>();
    auto& response = writer.append_structure<Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Protocol::CommandType::VIRTIO_GPU_CMD_SET_SCANOUT, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();
    request.scanout_id = scanout.value();
    request.rect = rect;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "GPU: Set backing scanout");
}

void GPU::transfer_framebuffer_data_to_host(ScanoutID scanout, Protocol::Rect const& dirty_rect, ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Protocol::TransferToHost2D>();
    auto& response = writer.append_structure<Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Protocol::CommandType::VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D, VIRTIO_GPU_FLAG_FENCE);
    request.offset = (dirty_rect.x + (dirty_rect.y * m_scanouts[scanout.value()].display_info.rect.width)) * sizeof(u32);
    request.resource_id = resource_id.value();
    request.rect = dirty_rect;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

void GPU::flush_displayed_image(Protocol::Rect const& dirty_rect, ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Protocol::ResourceFlush>();
    auto& response = writer.append_structure<Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_FLUSH, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();
    request.rect = dirty_rect;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

void GPU::synchronous_virtio_gpu_command(PhysicalAddress buffer_start, size_t request_size, size_t response_size)
{
    VERIFY(m_operation_lock.is_locked());
    VERIFY(m_outstanding_request.is_empty());
    auto& queue = get_queue(CONTROLQ);
    {
        SpinlockLocker lock(queue.lock());
        VirtIO::QueueChain chain { queue };
        chain.add_buffer_to_chain(buffer_start, request_size, VirtIO::BufferType::DeviceReadable);
        chain.add_buffer_to_chain(buffer_start.offset(request_size), response_size, VirtIO::BufferType::DeviceWritable);
        supply_chain_and_notify(CONTROLQ, chain);
        full_memory_barrier();
    }
    m_outstanding_request.wait_forever();
}

void GPU::populate_virtio_gpu_request_header(Protocol::ControlHeader& header, Protocol::CommandType ctrl_type, u32 flags)
{
    header.type = static_cast<u32>(ctrl_type);
    header.flags = flags;
    header.fence_id = 0;
    header.context_id = 0;
    header.padding = 0;
}

void GPU::flush_dirty_rectangle(ScanoutID scanout_id, Protocol::Rect const& dirty_rect, ResourceID resource_id)
{
    MutexLocker locker(m_operation_lock);
    transfer_framebuffer_data_to_host(scanout_id, dirty_rect, resource_id);
    flush_displayed_image(dirty_rect, resource_id);
}

ResourceID GPU::allocate_resource_id()
{
    VERIFY(m_operation_lock.is_locked());
    m_resource_id_counter = m_resource_id_counter.value() + 1;
    return m_resource_id_counter;
}

void GPU::delete_resource(ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Protocol::ResourceUnref>();
    auto& response = writer.append_structure<Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_UNREF, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

}
