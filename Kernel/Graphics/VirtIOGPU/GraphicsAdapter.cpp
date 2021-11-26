/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinaryBufferWriter.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/Graphics/VirtIOGPU/FramebufferDevice.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>

namespace Kernel::Graphics::VirtIOGPU {

#define DEVICE_EVENTS_READ 0x0
#define DEVICE_EVENTS_CLEAR 0x4
#define DEVICE_NUM_SCANOUTS 0x8

NonnullRefPtr<GraphicsAdapter> GraphicsAdapter::initialize(PCI::DeviceIdentifier const& device_identifier)
{
    VERIFY(device_identifier.hardware_id().vendor_id == PCI::VendorID::VirtIO);
    auto adapter = adopt_ref(*new GraphicsAdapter(device_identifier));
    adapter->initialize();
    return adapter;
}

GraphicsAdapter::GraphicsAdapter(PCI::DeviceIdentifier const& device_identifier)
    : VirtIO::Device(device_identifier)
{
    auto region_or_error = MM.allocate_contiguous_kernel_region(32 * PAGE_SIZE, "VirtGPU Scratch Space", Memory::Region::Access::ReadWrite);
    if (region_or_error.is_error())
        TODO();
    m_scratch_space = region_or_error.release_value();
}

void GraphicsAdapter::initialize_framebuffer_devices()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Initializing framebuffer devices");
    VERIFY(!m_created_framebuffer_devices);
    create_framebuffer_devices();
    m_created_framebuffer_devices = true;

    // FIXME: This is a very wrong way to do this...
    GraphicsManagement::the().m_console = default_console();
}

void GraphicsAdapter::enable_consoles()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Enabling consoles");
    for_each_framebuffer([&](auto& framebuffer, auto& console) {
        framebuffer.deactivate_writes();
        console.enable();
        return IterationDecision::Continue;
    });
}

void GraphicsAdapter::disable_consoles()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Disabling consoles");
    for_each_framebuffer([&](auto& framebuffer, auto& console) {
        console.disable();
        framebuffer.activate_writes();
        return IterationDecision::Continue;
    });
}

void GraphicsAdapter::initialize()
{
    Device::initialize();
    VERIFY(!!m_scratch_space);
    if (auto* config = get_config(VirtIO::ConfigurationType::Device)) {
        m_device_configuration = config;
        bool success = negotiate_features([&](u64 supported_features) {
            u64 negotiated = 0;
            if (is_feature_set(supported_features, VIRTIO_GPU_F_VIRGL))
                dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: VIRGL is not yet supported!");
            if (is_feature_set(supported_features, VIRTIO_GPU_F_EDID))
                dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: EDID is not yet supported!");
            return negotiated;
        });
        if (success) {
            read_config_atomic([&]() {
                m_num_scanouts = config_read32(*config, DEVICE_NUM_SCANOUTS);
            });
            dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: num_scanouts: {}", m_num_scanouts);
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

void GraphicsAdapter::create_framebuffer_devices()
{
    for (size_t i = 0; i < min(m_num_scanouts, VIRTIO_GPU_MAX_SCANOUTS); i++) {
        auto& scanout = m_scanouts[i];
        scanout.framebuffer = adopt_ref(*new VirtIOGPU::FramebufferDevice(*this, i));
        scanout.framebuffer->after_inserting();
        scanout.console = Kernel::Graphics::VirtIOGPU::Console::initialize(scanout.framebuffer);
    }
}

bool GraphicsAdapter::handle_device_config_change()
{
    auto events = get_pending_events();
    if (events & VIRTIO_GPU_EVENT_DISPLAY) {
        // The host window was resized, in SerenityOS we completely ignore this event
        dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Ignoring virtio gpu display resize event");
        clear_pending_events(VIRTIO_GPU_EVENT_DISPLAY);
    }
    if (events & ~VIRTIO_GPU_EVENT_DISPLAY) {
        dbgln("VirtIO::GraphicsAdapter: Got unknown device config change event: {:#x}", events);
        return false;
    }
    return true;
}

void GraphicsAdapter::handle_queue_update(u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Handle queue update");
    VERIFY(queue_index == CONTROLQ);

    auto& queue = get_queue(CONTROLQ);
    SpinlockLocker queue_lock(queue.lock());
    queue.discard_used_buffers();
    m_outstanding_request.wake_all();
}

u32 GraphicsAdapter::get_pending_events()
{
    return config_read32(*m_device_configuration, DEVICE_EVENTS_READ);
}

void GraphicsAdapter::clear_pending_events(u32 event_bitmask)
{
    config_write32(*m_device_configuration, DEVICE_EVENTS_CLEAR, event_bitmask);
}

void GraphicsAdapter::query_display_information()
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
        dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Scanout {}: enabled: {} x: {}, y: {}, width: {}, height: {}", i, !!scanout.enabled, scanout.rect.x, scanout.rect.y, scanout.rect.width, scanout.rect.height);
        if (scanout.enabled && !m_default_scanout.has_value())
            m_default_scanout = i;
    }
    VERIFY(m_default_scanout.has_value());
}

ResourceID GraphicsAdapter::create_2d_resource(Protocol::Rect rect)
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
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Allocated 2d resource with id {}", resource_id.value());
    return resource_id;
}

void GraphicsAdapter::ensure_backing_storage(ResourceID resource_id, Memory::Region const& region, size_t buffer_offset, size_t buffer_length)
{
    VERIFY(m_operation_lock.is_locked());

    VERIFY(buffer_offset % PAGE_SIZE == 0);
    VERIFY(buffer_length % PAGE_SIZE == 0);
    auto first_page_index = buffer_offset / PAGE_SIZE;
    size_t num_mem_regions = buffer_length / PAGE_SIZE;

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
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Allocated backing storage");
}

void GraphicsAdapter::detach_backing_storage(ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Protocol::ResourceDetachBacking>();
    auto& response = writer.append_structure<Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_DETACH_BACKING, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Detached backing storage");
}

void GraphicsAdapter::set_scanout_resource(ScanoutID scanout, ResourceID resource_id, Protocol::Rect rect)
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
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Set backing scanout");
}

void GraphicsAdapter::transfer_framebuffer_data_to_host(ScanoutID scanout, ResourceID resource_id, Protocol::Rect const& dirty_rect)
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

void GraphicsAdapter::flush_displayed_image(ResourceID resource_id, Protocol::Rect const& dirty_rect)
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

void GraphicsAdapter::synchronous_virtio_gpu_command(PhysicalAddress buffer_start, size_t request_size, size_t response_size)
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

void GraphicsAdapter::populate_virtio_gpu_request_header(Protocol::ControlHeader& header, Protocol::CommandType ctrl_type, u32 flags)
{
    header.type = static_cast<u32>(ctrl_type);
    header.flags = flags;
    header.fence_id = 0;
    header.context_id = 0;
    header.padding = 0;
}

void GraphicsAdapter::flush_dirty_rectangle(ScanoutID scanout_id, ResourceID resource_id, Protocol::Rect const& dirty_rect)
{
    MutexLocker locker(m_operation_lock);
    transfer_framebuffer_data_to_host(scanout_id, resource_id, dirty_rect);
    flush_displayed_image(resource_id, dirty_rect);
}

ResourceID GraphicsAdapter::allocate_resource_id()
{
    VERIFY(m_operation_lock.is_locked());
    m_resource_id_counter = m_resource_id_counter.value() + 1;
    return m_resource_id_counter;
}

void GraphicsAdapter::delete_resource(ResourceID resource_id)
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
