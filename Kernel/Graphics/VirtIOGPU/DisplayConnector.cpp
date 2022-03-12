/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/VirGL.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/Graphics/VirtIOGPU/DisplayConnector.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>
#include <Kernel/Graphics/VirtIOGPU/Protocol.h>
#include <Kernel/Random.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

VirtIODisplayConnector::PerContextState::PerContextState(Graphics::VirtIOGPU::ContextID context_id, NonnullOwnPtr<Memory::Region> transfer_buffer_region)
    : m_context_id(context_id)
    , m_transfer_buffer_region(move(transfer_buffer_region))
{
}

void VirtIODisplayConnector::detach(OpenFileDescription& description)
{
    m_context_state_lookup.remove(&description);
    DisplayConnector::detach(description);
}

ErrorOr<RefPtr<VirtIODisplayConnector::PerContextState>> VirtIODisplayConnector::get_context_for_description(OpenFileDescription& description)
{
    auto res = m_context_state_lookup.get(&description);
    if (!res.has_value())
        return EBADF;
    return res.value();
}

NonnullRefPtr<VirtIODisplayConnector> VirtIODisplayConnector::must_create(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id)
{
    // Setup memory transfer region
    auto virtio_virgl3d_uploader_buffer_region = MUST(MM.allocate_kernel_region(
        NUM_TRANSFER_REGION_PAGES * PAGE_SIZE,
        "VIRGL3D upload buffer",
        Memory::Region::Access::ReadWrite,
        AllocationStrategy::AllocateNow));

    auto scratch_space_region = MUST(MM.allocate_contiguous_kernel_region(
        32 * PAGE_SIZE,
        "VirtGPU Scratch Space",
        Memory::Region::Access::ReadWrite));

    auto device_or_error = DeviceManagement::try_create_device<VirtIODisplayConnector>(graphics_adapter, scanout_id, move(scratch_space_region), move(virtio_virgl3d_uploader_buffer_region));
    VERIFY(!device_or_error.is_error());
    auto connector = device_or_error.release_value();
    connector->initialize_3d_context();
    MUST(connector->set_safe_resolution());
    connector->initialize_console();
    return connector;
}

void VirtIODisplayConnector::initialize_3d_context()
{
    if (!m_graphics_adapter->virgl_feature_accepted())
        return;
    m_kernel_context_id = create_context();
}

VirtIODisplayConnector::VirtIODisplayConnector(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id, NonnullOwnPtr<Memory::Region> scratch_space_region, NonnullOwnPtr<Memory::Region> virtio_virgl3d_uploader_buffer_region)
    : DisplayConnector()
    , m_graphics_adapter(graphics_adapter)
    , m_scanout_id(scanout_id)
    , m_scratch_space(move(scratch_space_region))
    , m_virgl3d_uploader_buffer_region(move(virtio_virgl3d_uploader_buffer_region))
{
}

void VirtIODisplayConnector::initialize_console()
{
    m_console = Kernel::Graphics::VirtIOGPU::Console::initialize(*this);
}

DisplayConnector::Hardware3DAccelerationCommandSet VirtIODisplayConnector::hardware_3d_acceleration_commands_set() const
{
    return DisplayConnector::Hardware3DAccelerationCommandSet::VirGL;
}

ErrorOr<ByteBuffer> VirtIODisplayConnector::get_edid() const
{
    SpinlockLocker locker(m_operation_lock);
    if (!m_edid.has_value())
        return Error::from_errno(ENOTSUP);
    return ByteBuffer::copy(m_edid.value().bytes());
}
ErrorOr<void> VirtIODisplayConnector::set_resolution(Resolution const& resolution)
{
    SpinlockLocker locker(m_operation_lock);
    if (resolution.width > MAX_VIRTIOGPU_RESOLUTION_WIDTH || resolution.height > MAX_VIRTIOGPU_RESOLUTION_HEIGHT)
        return Error::from_errno(ENOTSUP);

    auto& info = m_display_info;

    info.rect = {
        .x = 0,
        .y = 0,
        .width = (u32)resolution.width,
        .height = (u32)resolution.height,
    };
    TRY(create_framebuffer());
    return {};
}
ErrorOr<void> VirtIODisplayConnector::set_safe_resolution()
{
    DisplayConnector::Resolution safe_resolution { 1024, 768, 32, 1024 * sizeof(u32), {} };
    return set_resolution(safe_resolution);
}
ErrorOr<DisplayConnector::Resolution> VirtIODisplayConnector::get_resolution()
{
    SpinlockLocker locker(m_operation_lock);
    auto& info = m_display_info;
    Resolution current_resolution;
    current_resolution.width = info.rect.width;
    current_resolution.pitch = info.rect.width * sizeof(u32);
    current_resolution.bpp = 32;
    current_resolution.height = info.rect.height;
    return current_resolution;
}
ErrorOr<void> VirtIODisplayConnector::set_y_offset(size_t)
{
    return Error::from_errno(ENOTIMPL);
}
ErrorOr<void> VirtIODisplayConnector::unblank()
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<size_t> VirtIODisplayConnector::write_to_first_surface(u64 offset, UserOrKernelBuffer const& buffer, size_t length)
{
    VERIFY(m_control_lock.is_locked());
    if (offset + length > m_buffer_size)
        return Error::from_errno(EOVERFLOW);
    TRY(buffer.read(m_main_buffer.framebuffer_data + offset, 0, length));
    return length;
}

ErrorOr<void> VirtIODisplayConnector::flush_rectangle(size_t buffer_index, FBRect const& rect)
{
    VERIFY(m_flushing_lock.is_locked());
    Graphics::VirtIOGPU::Protocol::Rect dirty_rect {
        .x = rect.x,
        .y = rect.y,
        .width = rect.width,
        .height = rect.height
    };
    auto& buffer = buffer_from_index(buffer_index);
    transfer_framebuffer_data_to_host(buffer.resource_id, dirty_rect);
    if (&buffer == m_current_buffer) {
        // Flushing directly to screen
        flush_displayed_image(buffer.resource_id, dirty_rect);
        buffer.dirty_rect = {};
    } else {
        if (buffer.dirty_rect.width == 0 || buffer.dirty_rect.height == 0) {
            buffer.dirty_rect = dirty_rect;
        } else {
            auto current_dirty_right = buffer.dirty_rect.x + buffer.dirty_rect.width;
            auto current_dirty_bottom = buffer.dirty_rect.y + buffer.dirty_rect.height;
            buffer.dirty_rect.x = min(buffer.dirty_rect.x, dirty_rect.x);
            buffer.dirty_rect.y = min(buffer.dirty_rect.y, dirty_rect.y);
            buffer.dirty_rect.width = max(current_dirty_right, dirty_rect.x + dirty_rect.width) - buffer.dirty_rect.x;
            buffer.dirty_rect.height = max(current_dirty_bottom, dirty_rect.y + dirty_rect.height) - buffer.dirty_rect.y;
        }
    }
    return {};
}

ErrorOr<void> VirtIODisplayConnector::flush_first_surface()
{
    VERIFY(m_flushing_lock.is_locked());
    SpinlockLocker locker(m_operation_lock);
    Graphics::VirtIOGPU::Protocol::Rect dirty_rect {
        .x = 0,
        .y = 0,
        .width = m_display_info.rect.width,
        .height = m_display_info.rect.height
    };
    auto& buffer = buffer_from_index(0);
    transfer_framebuffer_data_to_host(buffer.resource_id, dirty_rect);
    if (&buffer == m_current_buffer) {
        // Flushing directly to screen
        flush_displayed_image(buffer.resource_id, dirty_rect);
        buffer.dirty_rect = {};
    } else {
        if (buffer.dirty_rect.width == 0 || buffer.dirty_rect.height == 0) {
            buffer.dirty_rect = dirty_rect;
        } else {
            auto current_dirty_right = buffer.dirty_rect.x + buffer.dirty_rect.width;
            auto current_dirty_bottom = buffer.dirty_rect.y + buffer.dirty_rect.height;
            buffer.dirty_rect.x = min(buffer.dirty_rect.x, dirty_rect.x);
            buffer.dirty_rect.y = min(buffer.dirty_rect.y, dirty_rect.y);
            buffer.dirty_rect.width = max(current_dirty_right, dirty_rect.x + dirty_rect.width) - buffer.dirty_rect.x;
            buffer.dirty_rect.height = max(current_dirty_bottom, dirty_rect.y + dirty_rect.height) - buffer.dirty_rect.y;
        }
    }
    return {};
}

void VirtIODisplayConnector::enable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_console);
    m_console->enable();
}

void VirtIODisplayConnector::disable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_console);
    m_console->disable();
}

ErrorOr<void> VirtIODisplayConnector::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{
    // TODO: We really should have ioctls for destroying resources as well
    switch (request) {
    case VIRGL_IOCTL_CREATE_CONTEXT: {
        if (m_context_state_lookup.contains(&description))
            return EEXIST;
        SpinlockLocker locker(m_operation_lock);
        // TODO: Delete the context if it fails to be set in m_context_state_lookup
        auto context_id = create_context();
        RefPtr<PerContextState> per_context_state = TRY(PerContextState::try_create(context_id));
        TRY(m_context_state_lookup.try_set(&description, per_context_state));
        return {};
    }
    case VIRGL_IOCTL_TRANSFER_DATA: {
        auto& transfer_buffer_region = TRY(get_context_for_description(description))->transfer_buffer_region();
        auto user_transfer_descriptor = static_ptr_cast<VirGLTransferDescriptor const*>(arg);
        auto transfer_descriptor = TRY(copy_typed_from_user(user_transfer_descriptor));
        if (transfer_descriptor.direction == VIRGL_DATA_DIR_GUEST_TO_HOST) {
            if (transfer_descriptor.offset_in_region + transfer_descriptor.num_bytes > NUM_TRANSFER_REGION_PAGES * PAGE_SIZE) {
                return EOVERFLOW;
            }
            auto target = transfer_buffer_region.vaddr().offset(transfer_descriptor.offset_in_region).as_ptr();
            return copy_from_user(target, transfer_descriptor.data, transfer_descriptor.num_bytes);
        } else if (transfer_descriptor.direction == VIRGL_DATA_DIR_HOST_TO_GUEST) {
            if (transfer_descriptor.offset_in_region + transfer_descriptor.num_bytes > NUM_TRANSFER_REGION_PAGES * PAGE_SIZE) {
                return EOVERFLOW;
            }
            auto source = transfer_buffer_region.vaddr().offset(transfer_descriptor.offset_in_region).as_ptr();
            return copy_to_user(transfer_descriptor.data, source, transfer_descriptor.num_bytes);
        } else {
            return EINVAL;
        }
    }
    case VIRGL_IOCTL_SUBMIT_CMD: {
        auto context_id = TRY(get_context_for_description(description))->context_id();
        SpinlockLocker locker(m_operation_lock);
        auto user_command_buffer = static_ptr_cast<VirGLCommandBuffer const*>(arg);
        auto command_buffer = TRY(copy_typed_from_user(user_command_buffer));
        submit_command_buffer(context_id, [&](Bytes buffer) {
            auto num_bytes = command_buffer.num_elems * sizeof(u32);
            VERIFY(num_bytes <= buffer.size());
            MUST(copy_from_user(buffer.data(), command_buffer.data, num_bytes));
            return num_bytes;
        });
        return {};
    }
    case VIRGL_IOCTL_CREATE_RESOURCE: {
        auto per_context_state = TRY(get_context_for_description(description));
        auto user_spec = static_ptr_cast<VirGL3DResourceSpec const*>(arg);
        VirGL3DResourceSpec spec = TRY(copy_typed_from_user(user_spec));

        Graphics::VirtIOGPU::Protocol::Resource3DSpecification const resource_spec = {
            .target = static_cast<Graphics::VirtIOGPU::Protocol::Gallium::PipeTextureTarget>(spec.target),
            .format = spec.format,
            .bind = spec.bind,
            .width = spec.width,
            .height = spec.height,
            .depth = spec.depth,
            .array_size = spec.array_size,
            .last_level = spec.last_level,
            .nr_samples = spec.nr_samples,
            .flags = spec.flags,
            .padding = 0,
        };
        SpinlockLocker locker(m_operation_lock);
        auto resource_id = create_3d_resource(resource_spec).value();
        attach_resource_to_context(resource_id, per_context_state->context_id());
        ensure_backing_storage(resource_id, per_context_state->transfer_buffer_region(), 0, NUM_TRANSFER_REGION_PAGES * PAGE_SIZE);
        spec.created_resource_id = resource_id;
        // FIXME: We should delete the resource we just created if we fail to copy the resource id out
        return copy_to_user(static_ptr_cast<VirGL3DResourceSpec*>(arg), &spec);
    }
    }
    return DisplayConnector::ioctl(description, request, arg);
}

void VirtIODisplayConnector::clear_to_black(Buffer& buffer)
{
    size_t width = m_display_info.rect.width;
    size_t height = m_display_info.rect.height;
    u8* data = buffer.framebuffer_data;
    for (size_t i = 0; i < width * height; ++i) {
        data[4 * i + 0] = 0x00;
        data[4 * i + 1] = 0x00;
        data[4 * i + 2] = 0x00;
        data[4 * i + 3] = 0xff;
    }
}

void VirtIODisplayConnector::draw_ntsc_test_pattern(Buffer& buffer)
{
    constexpr u8 colors[12][4] = {
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
    u8* data = buffer.framebuffer_data;
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

u8* VirtIODisplayConnector::framebuffer_data()
{
    return m_current_buffer->framebuffer_data;
}

ErrorOr<void> VirtIODisplayConnector::create_framebuffer()
{
    SpinlockLocker locker(m_operation_lock);
    // First delete any existing framebuffers to free the memory first
    m_framebuffer = nullptr;
    m_framebuffer_sink_vmobject = nullptr;

    // Allocate frame buffer for both front and back
    m_buffer_size = calculate_framebuffer_size(m_display_info.rect.width, m_display_info.rect.height);
    auto region_name = TRY(KString::formatted("VirtGPU FrameBuffer #{}", m_scanout_id.value()));
    m_framebuffer = TRY(MM.allocate_kernel_region(m_buffer_size * 2, region_name->view(), Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow));
    auto write_sink_page = TRY(MM.allocate_user_physical_page(Memory::MemoryManager::ShouldZeroFill::No));
    auto num_needed_pages = m_framebuffer->vmobject().page_count();

    NonnullRefPtrVector<Memory::PhysicalPage> pages;
    for (auto i = 0u; i < num_needed_pages; ++i) {
        TRY(pages.try_append(write_sink_page));
    }
    m_framebuffer_sink_vmobject = TRY(Memory::AnonymousVMObject::try_create_with_physical_pages(pages.span()));

    m_current_buffer = &buffer_from_index(m_last_set_buffer_index.load());
    create_buffer(m_main_buffer, 0, m_buffer_size);
    create_buffer(m_back_buffer, m_buffer_size, m_buffer_size);

    return {};
}

void VirtIODisplayConnector::create_buffer(Buffer& buffer, size_t framebuffer_offset, size_t framebuffer_size)
{
    buffer.framebuffer_offset = framebuffer_offset;
    buffer.framebuffer_data = m_framebuffer->vaddr().as_ptr() + framebuffer_offset;

    // 1. Create BUFFER using VIRTIO_GPU_CMD_RESOURCE_CREATE_2D
    if (buffer.resource_id.value() != 0)
        delete_resource(buffer.resource_id);
    buffer.resource_id = create_2d_resource(m_display_info.rect);

    // 2. Attach backing storage using  VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING
    ensure_backing_storage(buffer.resource_id, *m_framebuffer, buffer.framebuffer_offset, framebuffer_size);
    // 3. Use VIRTIO_GPU_CMD_SET_SCANOUT to link the framebuffer to a display scanout.
    if (&buffer == m_current_buffer)
        set_scanout_resource(buffer.resource_id, m_display_info.rect);
    // 4. Render our test pattern
    draw_ntsc_test_pattern(buffer);
    // 5. Use VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D to update the host resource from guest memory.
    transfer_framebuffer_data_to_host(buffer.resource_id, m_display_info.rect);
    // 6. Use VIRTIO_GPU_CMD_RESOURCE_FLUSH to flush the updated resource to the display.
    if (&buffer == m_current_buffer)
        flush_displayed_image(buffer.resource_id, m_display_info.rect);

    // Make sure we constrain the existing dirty rect (if any)
    if (buffer.dirty_rect.width != 0 || buffer.dirty_rect.height != 0) {
        auto dirty_right = buffer.dirty_rect.x + buffer.dirty_rect.width;
        auto dirty_bottom = buffer.dirty_rect.y + buffer.dirty_rect.height;
        buffer.dirty_rect.width = min(dirty_right, m_display_info.rect.x + m_display_info.rect.width) - buffer.dirty_rect.x;
        buffer.dirty_rect.height = min(dirty_bottom, m_display_info.rect.y + m_display_info.rect.height) - buffer.dirty_rect.y;
    }

    m_display_info.enabled = 1;
}

void VirtIODisplayConnector::query_display_information()
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();
    populate_virtio_gpu_request_header(request, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_GET_DISPLAY_INFO, 0);
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::DisplayInfoResponse>();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    auto& scanout = m_display_info;
    scanout = response.scanout_modes[m_scanout_id.value()];
    dbgln_if(VIRTIO_DEBUG, "VirtIODisplayConnector (Scanout {}): enabled: {} x: {}, y: {}, width: {}, height: {}", m_scanout_id.value(), !!scanout.enabled, scanout.rect.x, scanout.rect.y, scanout.rect.width, scanout.rect.height);
    m_edid = {};
}

void VirtIODisplayConnector::query_display_edid()
{
    VERIFY(m_operation_lock.is_locked());

    if (!m_graphics_adapter->edid_feature_accepted())
        return;

    // scanout.display_info.enabled doesn't seem to reflect the actual state,
    // even if we were to call query_display_information prior to calling
    // this function. So, just ignore, we seem to get EDID information regardless.

    auto query_edid_result = query_edid_from_virtio_adapter();
    if (query_edid_result.is_error()) {
        dbgln("VirtIODisplayConnector: (Scanout {}): Failed to parse EDID: {}", m_scanout_id, query_edid_result.error());
        m_edid = {};
    } else {
        m_edid = query_edid_result.release_value();
        if (m_edid.has_value()) {
            auto& parsed_edid = m_edid.value();
            dbgln("VirtIODisplayConnector (Scanout {}): EDID {}: Manufacturer: {} Product: {} Serial #{}", m_scanout_id,
                parsed_edid.version(), parsed_edid.legacy_manufacturer_id(), parsed_edid.product_code(), parsed_edid.serial_number());
            if (auto screen_size = parsed_edid.screen_size(); screen_size.has_value()) {
                auto& size = screen_size.value();
                dbgln("VirtIODisplayConnector (Scanout {}):           Screen size: {}cm x {}cm", m_scanout_id,
                    size.horizontal_cm(), size.vertical_cm());
            } else if (auto aspect_ratio = parsed_edid.aspect_ratio(); aspect_ratio.has_value()) {
                auto& ratio = aspect_ratio.value();
                dbgln("VirtIODisplayConnector (Scanout {}):           Aspect ratio: {} : 1", m_scanout_id, ratio.ratio());
            } else {
                dbgln("VirtIODisplayConnector (Scanout {}):           Unknown screen size or aspect ratio", m_scanout_id);
            }
        } else {
            dbgln("VirtIODisplayConnector (Scanout {}): No EDID", m_scanout_id);
        }
    }
}

ErrorOr<Optional<EDID::Parser>> VirtIODisplayConnector::query_edid_from_virtio_adapter()
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::GetEDID>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::GetEDIDResponse>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_GET_EDID, 0);

    request.scanout_id = m_scanout_id.value();
    request.padding = 0;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    if (response.header.type != to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_EDID))
        return Error::from_string_literal("VirtIODisplayConnector: Failed to get EDID");

    if (response.size == 0)
        return Error::from_string_literal("VirtIODisplayConnector: Failed to get EDID, empty buffer");

    auto edid_buffer = TRY(ByteBuffer::copy(response.edid, response.size));
    auto edid = TRY(EDID::Parser::from_bytes(move(edid_buffer)));
    return edid;
}

Graphics::VirtIOGPU::ResourceID VirtIODisplayConnector::create_2d_resource(Graphics::VirtIOGPU::Protocol::Rect rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceCreate2D>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_CREATE_2D, 0);

    auto resource_id = m_graphics_adapter->allocate_resource_id({});
    request.resource_id = resource_id.value();
    request.width = rect.width;
    request.height = rect.height;
    request.format = to_underlying(Graphics::VirtIOGPU::Protocol::TextureFormat::VIRTIO_GPU_FORMAT_B8G8R8X8_UNORM);

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIODisplayConnector: Allocated 2d resource with id {}", resource_id.value());
    return resource_id;
}

Graphics::VirtIOGPU::ResourceID VirtIODisplayConnector::create_3d_resource(Graphics::VirtIOGPU::Protocol::Resource3DSpecification const& resource_3d_specification)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceCreate3D>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_CREATE_3D, 0);

    auto resource_id = m_graphics_adapter->allocate_resource_id({});
    request.resource_id = resource_id.value();
    // TODO: Abstract this out a bit more
    u32* start_of_copied_fields = &request.target;
    memcpy(start_of_copied_fields, &resource_3d_specification, sizeof(Graphics::VirtIOGPU::Protocol::Resource3DSpecification));

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIODisplayConnector: Allocated 3d resource with id {}", resource_id.value());
    return resource_id;
}

void VirtIODisplayConnector::ensure_backing_storage(Graphics::VirtIOGPU::ResourceID resource_id, Memory::Region const& region, size_t buffer_offset, size_t buffer_length)
{
    VERIFY(m_operation_lock.is_locked());

    VERIFY(buffer_offset % PAGE_SIZE == 0);
    VERIFY(buffer_length % PAGE_SIZE == 0);
    auto first_page_index = buffer_offset / PAGE_SIZE;
    size_t num_mem_regions = buffer_length / PAGE_SIZE;

    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceAttachBacking>();
    const size_t header_block_size = sizeof(request) + num_mem_regions * sizeof(Graphics::VirtIOGPU::Protocol::MemoryEntry);

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING, 0);
    request.resource_id = resource_id.value();
    request.num_entries = num_mem_regions;
    for (size_t i = 0; i < num_mem_regions; ++i) {
        auto& memory_entry = writer.append_structure<Graphics::VirtIOGPU::Protocol::MemoryEntry>();
        memory_entry.address = region.physical_page(first_page_index + i)->paddr().get();
        memory_entry.length = PAGE_SIZE;
    }

    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    synchronous_virtio_gpu_command(start_of_scratch_space(), header_block_size, sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIODisplayConnector: Allocated backing storage");
}

void VirtIODisplayConnector::detach_backing_storage(Graphics::VirtIOGPU::ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceDetachBacking>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_DETACH_BACKING, 0);
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIODisplayConnector: Detached backing storage");
}

void VirtIODisplayConnector::set_scanout_resource(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect rect)
{
    VERIFY(m_operation_lock.is_locked());
    {
        // We need to scope the request/response here so that we can query display information later on
        auto writer = create_scratchspace_writer();
        auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::SetScanOut>();
        auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

        populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_SET_SCANOUT, 0);
        request.resource_id = resource_id.value();
        request.scanout_id = m_scanout_id.value();
        request.rect = rect;

        synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

        VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
        dbgln_if(VIRTIO_DEBUG, "VirtIODisplayConnector: Set backing scanout");
    }

    // Now that the Scanout should be enabled, update the EDID
    query_display_edid();
}

void VirtIODisplayConnector::transfer_framebuffer_data_to_host(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect)
{
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::TransferToHost2D>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D, 0);
    request.offset = (dirty_rect.x + (dirty_rect.y * m_display_info.rect.width)) * sizeof(u32);
    request.resource_id = resource_id.value();
    request.rect = dirty_rect;

    // FIXME: Find a better way to put multiple commands on the graphics adapter.
    auto was_able_to_queue = synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));
    if (was_able_to_queue)
        VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

void VirtIODisplayConnector::flush_displayed_image(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect)
{
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceFlush>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_FLUSH, 0);
    request.resource_id = resource_id.value();
    request.rect = dirty_rect;

    // FIXME: Find a better way to put multiple commands on the graphics adapter.
    auto was_able_to_queue = synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));
    if (was_able_to_queue)
        VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

bool VirtIODisplayConnector::synchronous_virtio_gpu_command(PhysicalAddress buffer_start, size_t request_size, size_t response_size)
{
    // FIXME: Find a better way to put multiple commands on the graphics adapter.
    if (!m_graphics_adapter->outstanding_request_empty())
        return false;
    auto& queue = m_graphics_adapter->get_queue(CONTROLQ);
    {
        SpinlockLocker lock(queue.lock());
        VirtIO::QueueChain chain { queue };
        chain.add_buffer_to_chain(buffer_start, request_size, VirtIO::BufferType::DeviceReadable);
        chain.add_buffer_to_chain(buffer_start.offset(request_size), response_size, VirtIO::BufferType::DeviceWritable);
        m_graphics_adapter->supply_chain_and_notify(CONTROLQ, chain);
        full_memory_barrier();
    }
    m_graphics_adapter->wait_for_outstanding_request();
    return true;
}

void VirtIODisplayConnector::populate_virtio_gpu_request_header(Graphics::VirtIOGPU::Protocol::ControlHeader& header, Graphics::VirtIOGPU::Protocol::CommandType ctrl_type, u32 flags)
{
    header.type = to_underlying(ctrl_type);
    header.flags = flags;
    header.fence_id = 0;
    header.context_id = 0;
    header.padding = 0;
}

void VirtIODisplayConnector::flush_dirty_rectangle(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect)
{
    SpinlockLocker locker(m_operation_lock);
    transfer_framebuffer_data_to_host(resource_id, dirty_rect);
    flush_displayed_image(resource_id, dirty_rect);
}

void VirtIODisplayConnector::delete_resource(Graphics::VirtIOGPU::ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceUnref>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_UNREF, 0);
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

Graphics::VirtIOGPU::ContextID VirtIODisplayConnector::create_context()
{
    SpinlockLocker locker(m_operation_lock);
    auto ctx_id = m_graphics_adapter->allocate_context_id({});
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ContextCreate>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    constexpr char const* region_name = "Serenity VirGL3D Context";
    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_CTX_CREATE, 0);
    request.header.context_id = ctx_id.value();
    request.name_length = strlen(region_name);
    memset(request.debug_name.data(), 0, 64);
    VERIFY(request.name_length <= 64);
    memcpy(request.debug_name.data(), region_name, request.name_length);

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));
    dbgln("{}", response.type);
    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    return ctx_id;
}

void VirtIODisplayConnector::submit_command_buffer(Graphics::VirtIOGPU::ContextID context_id, Function<size_t(Bytes)> buffer_writer)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::CommandSubmit>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_SUBMIT_3D, 0);
    request.header.context_id = context_id.value();

    auto max_command_buffer_length = m_scratch_space->size() - sizeof(request) - sizeof(Graphics::VirtIOGPU::Protocol::ControlHeader);
    // Truncate to nearest multiple of alignment, to ensure padding loop doesn't exhaust allocated space
    max_command_buffer_length -= max_command_buffer_length % alignof(Graphics::VirtIOGPU::Protocol::ControlHeader);
    Bytes command_buffer_buffer(m_scratch_space->vaddr().offset(sizeof(request)).as_ptr(), max_command_buffer_length);
    request.size = buffer_writer(command_buffer_buffer);
    writer.skip_bytes(request.size);
    // The alignment of a ControlHeader may be a few words larger than the length of a command buffer, so
    // we pad with no-ops until we reach the correct alignment
    while (writer.current_offset() % alignof(Graphics::VirtIOGPU::Protocol::ControlHeader) != 0) {
        VERIFY((writer.current_offset() % alignof(Graphics::VirtIOGPU::Protocol::ControlHeader)) % sizeof(u32) == 0);
        writer.append_structure<u32>() = to_underlying(Graphics::VirtIOGPU::VirGLCommand::NOP);
        request.size += 4;
    }
    dbgln_if(VIRTIO_DEBUG, "VirtIODisplayConnector: Sending command buffer of length {}", request.size);
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request) + request.size, sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

void VirtIODisplayConnector::attach_resource_to_context(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::ContextID context_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ContextAttachResource>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();
    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_CTX_ATTACH_RESOURCE, 0);
    request.header.context_id = context_id.value();
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

}
