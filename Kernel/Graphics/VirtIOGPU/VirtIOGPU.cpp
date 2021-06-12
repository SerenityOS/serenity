/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VirtIOGPU/VirtIOGPU.h>
#include <LibC/sys/ioctl_numbers.h>

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
        // 1. Get display information using VIRTIO_GPU_CMD_GET_DISPLAY_INFO
        query_display_information();
        // 2. Create BUFFER using VIRTIO_GPU_CMD_RESOURCE_CREATE_2D
        m_framebuffer_id = create_2d_resource(m_display_info.rect);
        // 3. Attach backing storage using  VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING
        // FIXME: We really should be trying to allocate a small amount of pages initially, with ensure_backing_storage increasing the backing memory of the region as needed
        size_t buffer_length = calculate_framebuffer_size(MAX_VIRTIOGPU_RESOLUTION_WIDTH, MAX_VIRTIOGPU_RESOLUTION_HEIGHT);
        m_framebuffer = MM.allocate_kernel_region(page_round_up(buffer_length), "VirtGPU FrameBuffer", Region::Access::Read | Region::Access::Write, AllocationStrategy::AllocateNow);
        ensure_backing_storage(*m_framebuffer, buffer_length, m_framebuffer_id);
        // 4. Use VIRTIO_GPU_CMD_SET_SCANOUT to link the framebuffer to a display scanout.
        set_scanout_resource(m_chosen_scanout.value(), m_framebuffer_id, m_display_info.rect);
        // 5. Render our test pattern
        draw_ntsc_test_pattern();
        // 6. Use VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D to update the host resource from guest memory.
        transfer_framebuffer_data_to_host(m_display_info.rect);
        // 7. Use VIRTIO_GPU_CMD_RESOURCE_FLUSH to flush the updated resource to the display.
        flush_displayed_image(m_display_info.rect);
    } else {
        VERIFY_NOT_REACHED();
    }
}

VirtIOGPU::~VirtIOGPU()
{
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
    auto& request = *reinterpret_cast<VirtIOGPUCtrlHeader*>(m_scratch_space->vaddr().as_ptr());
    auto& response = *reinterpret_cast<VirtIOGPURespDisplayInfo*>((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr()));

    populate_virtio_gpu_request_header(request, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_GET_DISPLAY_INFO, VIRTIO_GPU_FLAG_FENCE);

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    for (size_t i = 0; i < VIRTIO_GPU_MAX_SCANOUTS; ++i) {
        auto& scanout = response.scanout_modes[i];
        if (!scanout.enabled)
            continue;
        dbgln_if(VIRTIO_DEBUG, "Scanout {}: x: {}, y: {}, width: {}, height: {}", i, scanout.rect.x, scanout.rect.y, scanout.rect.width, scanout.rect.height);
        m_display_info = scanout;
        m_chosen_scanout = i;
    }
    VERIFY(m_chosen_scanout.has_value());
}

VirtIOGPUResourceID VirtIOGPU::create_2d_resource(VirtIOGPURect rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *reinterpret_cast<VirtIOGPUResourceCreate2D*>(m_scratch_space->vaddr().as_ptr());
    auto& response = *reinterpret_cast<VirtIOGPUCtrlHeader*>((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr()));

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

void VirtIOGPU::ensure_backing_storage(Region& region, size_t buffer_length, VirtIOGPUResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    // Allocate backing region
    auto& vm_object = region.vmobject();
    size_t desired_num_pages = page_round_up(buffer_length);
    auto& pages = vm_object.physical_pages();
    for (size_t i = pages.size(); i < desired_num_pages / PAGE_SIZE; ++i) {
        auto page = MM.allocate_user_physical_page();
        // FIXME: Instead of verifying, fail the framebuffer resize operation
        VERIFY(!page.is_null());
        pages.append(move(page));
    }
    region.remap();
    size_t num_mem_regions = vm_object.page_count();

    // Send request
    auto& request = *reinterpret_cast<VirtIOGPUResourceAttachBacking*>(m_scratch_space->vaddr().as_ptr());
    const size_t header_block_size = sizeof(request) + num_mem_regions * sizeof(VirtIOGPUMemEntry);
    auto& response = *reinterpret_cast<VirtIOGPUCtrlHeader*>((m_scratch_space->vaddr().offset(header_block_size).as_ptr()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();
    request.num_entries = num_mem_regions;
    for (size_t i = 0; i < num_mem_regions; ++i) {
        request.entries[i].address = m_framebuffer->physical_page(i)->paddr().get();
        request.entries[i].length = PAGE_SIZE;
    }

    synchronous_virtio_gpu_command(start_of_scratch_space(), header_block_size, sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Allocated backing storage");
}

void VirtIOGPU::detach_backing_storage(VirtIOGPUResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *reinterpret_cast<VirtIOGPUResourceDetachBacking*>(m_scratch_space->vaddr().as_ptr());
    auto& response = *reinterpret_cast<VirtIOGPUCtrlHeader*>((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_RESOURCE_DETACH_BACKING, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Detached backing storage");
}

void VirtIOGPU::set_scanout_resource(VirtIOGPUScanoutID scanout, VirtIOGPUResourceID resource_id, VirtIOGPURect rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *reinterpret_cast<VirtIOGPUSetScanOut*>(m_scratch_space->vaddr().as_ptr());
    auto& response = *reinterpret_cast<VirtIOGPUCtrlHeader*>((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_SET_SCANOUT, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();
    request.scanout_id = scanout.value();
    request.rect = rect;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Set backing scanout");
}

void VirtIOGPU::draw_ntsc_test_pattern()
{
    static constexpr u8 colors[12][4] = {
        { 0xff, 0xff, 0xff, 0xff }, // White
        { 0x00, 0xff, 0xff, 0xff }, // Primary + Composite colors
        { 0xff, 0xff, 0x00, 0xff },
        { 0x00, 0xff, 0x00, 0xff },
        { 0xff, 0x00, 0xff, 0xff },
        { 0x00, 0x00, 0xff, 0xff },
        { 0xff, 0x00, 0x00, 0xff },
        { 0xba, 0x01, 0x5f, 0xff }, // Dark blue
        { 0x8d, 0x3d, 0x00, 0xff }, // Purple
        { 0x22, 0x22, 0x22, 0xff }, // Shades of gray
        { 0x10, 0x10, 0x10, 0xff },
        { 0x00, 0x00, 0x00, 0xff },
    };
    size_t width = m_display_info.rect.width;
    size_t height = m_display_info.rect.height;
    u8* data = m_framebuffer->vaddr().as_ptr();
    // Draw NTSC test card
    for (size_t y = 0; y < height; ++y) {
        for (size_t x = 0; x < width; ++x) {
            size_t color = 0;
            if (3 * y < 2 * height) {
                // Top 2/3 of image is 7 vertical stripes of color spectrum
                color = (7 * x) / width;
            } else if (4 * y < 3 * height) {
                // 2/3 mark to 3/4 mark  is backwards color spectrum alternating with black
                auto segment = (7 * x) / width;
                color = segment % 2 ? 10 : 6 - segment;
            } else {
                if (28 * x < 5 * width) {
                    color = 8;
                } else if (28 * x < 10 * width) {
                    color = 0;
                } else if (28 * x < 15 * width) {
                    color = 7;
                } else if (28 * x < 20 * width) {
                    color = 10;
                } else if (7 * x < 6 * width) {
                    // Grayscale gradient
                    color = 26 - ((21 * x) / width);
                } else {
                    // Solid black
                    color = 10;
                }
            }
            u8* pixel = &data[4 * (y * width + x)];
            for (int i = 0; i < 4; ++i) {
                pixel[i] = colors[color][i];
            }
        }
    }
    dbgln_if(VIRTIO_DEBUG, "Finish drawing the pattern");
}

void VirtIOGPU::transfer_framebuffer_data_to_host(VirtIOGPURect dirty_rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *reinterpret_cast<VirtIOGPUTransferToHost2D*>(m_scratch_space->vaddr().as_ptr());
    auto& response = *reinterpret_cast<VirtIOGPUCtrlHeader*>((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D, VIRTIO_GPU_FLAG_FENCE);
    request.offset = (dirty_rect.x + (dirty_rect.y * m_display_info.rect.width)) * sizeof(u32);
    request.resource_id = m_framebuffer_id.value();
    request.rect = dirty_rect;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
}

void VirtIOGPU::flush_displayed_image(VirtIOGPURect dirty_rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto& request = *reinterpret_cast<VirtIOGPUResourceFlush*>(m_scratch_space->vaddr().as_ptr());
    auto& response = *reinterpret_cast<VirtIOGPUCtrlHeader*>((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_RESOURCE_FLUSH, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = m_framebuffer_id.value();
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

void VirtIOGPU::flush_dirty_window(VirtIOGPURect dirty_rect)
{
    Locker locker(m_operation_lock);
    transfer_framebuffer_data_to_host(dirty_rect);
    flush_displayed_image(dirty_rect);
}

bool VirtIOGPU::try_to_set_resolution(size_t width, size_t height)
{
    if (width > MAX_VIRTIOGPU_RESOLUTION_WIDTH && height > MAX_VIRTIOGPU_RESOLUTION_HEIGHT)
        return false;
    Locker locker(m_operation_lock);
    VirtIOGPURect rect = {
        .x = 0,
        .y = 0,
        .width = (u32)width,
        .height = (u32)height,
    };
    auto old_framebuffer_id = m_framebuffer_id;
    auto new_framebuffer_id = create_2d_resource(rect);
    ensure_backing_storage(*m_framebuffer, calculate_framebuffer_size(width, height), new_framebuffer_id);
    set_scanout_resource(m_chosen_scanout.value(), new_framebuffer_id, rect);
    detach_backing_storage(old_framebuffer_id);
    delete_resource(old_framebuffer_id);
    m_framebuffer_id = new_framebuffer_id;
    m_display_info.rect = rect;
    return true;
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
    auto& request = *reinterpret_cast<VirtioGPUResourceUnref*>(m_scratch_space->vaddr().as_ptr());
    auto& response = *reinterpret_cast<VirtIOGPUCtrlHeader*>((m_scratch_space->vaddr().offset(sizeof(request)).as_ptr()));

    populate_virtio_gpu_request_header(request.header, VirtIOGPUCtrlType::VIRTIO_GPU_CMD_RESOURCE_UNREF, VIRTIO_GPU_FLAG_FENCE);
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(VirtIOGPUCtrlType::VIRTIO_GPU_RESP_OK_NODATA));
}

size_t VirtIOGPU::framebuffer_size_in_bytes() const
{
    return m_display_info.rect.width * m_display_info.rect.height * sizeof(u32);
}

void VirtIOGPU::clear_to_black()
{
    size_t width = m_display_info.rect.width;
    size_t height = m_display_info.rect.height;
    u8* data = m_framebuffer->vaddr().as_ptr();
    for (size_t i = 0; i < width * height; ++i) {
        data[4 * i + 0] = 0x00;
        data[4 * i + 1] = 0x00;
        data[4 * i + 2] = 0x00;
        data[4 * i + 3] = 0xff;
    }
}

}
