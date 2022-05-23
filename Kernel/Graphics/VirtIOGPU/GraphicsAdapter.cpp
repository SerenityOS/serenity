/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinaryBufferWriter.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/Graphics/VirtIOGPU/DisplayConnector.h>
#include <Kernel/Graphics/VirtIOGPU/GPU3DDevice.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>

namespace Kernel {

#define DEVICE_EVENTS_READ 0x0
#define DEVICE_EVENTS_CLEAR 0x4
#define DEVICE_NUM_SCANOUTS 0x8

NonnullRefPtr<VirtIOGraphicsAdapter> VirtIOGraphicsAdapter::initialize(PCI::DeviceIdentifier const& device_identifier)
{
    VERIFY(device_identifier.hardware_id().vendor_id == PCI::VendorID::VirtIO);
    // Setup memory transfer region
    auto scratch_space_region = MUST(MM.allocate_contiguous_kernel_region(
        32 * PAGE_SIZE,
        "VirtGPU Scratch Space",
        Memory::Region::Access::ReadWrite));

    auto adapter = adopt_ref(*new (nothrow) VirtIOGraphicsAdapter(device_identifier, move(scratch_space_region)));
    adapter->initialize();
    MUST(adapter->initialize_adapter());
    return adapter;
}

ErrorOr<void> VirtIOGraphicsAdapter::initialize_adapter()
{
    VERIFY(m_num_scanouts <= VIRTIO_GPU_MAX_SCANOUTS);
    for (size_t index = 0; index < m_num_scanouts; index++) {
        auto display_connector = VirtIODisplayConnector::must_create(*this, index);
        m_scanouts[index].display_connector = display_connector;
        MUST(query_and_set_edid(index, *display_connector));
        display_connector->set_safe_mode_setting_after_initialization({});
    }
    return {};
}

VirtIOGraphicsAdapter::VirtIOGraphicsAdapter(PCI::DeviceIdentifier const& device_identifier, NonnullOwnPtr<Memory::Region> scratch_space_region)
    : VirtIO::Device(device_identifier)
    , m_scratch_space(move(scratch_space_region))
{
}

void VirtIOGraphicsAdapter::initialize()
{
    VirtIO::Device::initialize();
    if (auto* config = get_config(VirtIO::ConfigurationType::Device)) {
        m_device_configuration = config;
        bool success = negotiate_features([&](u64 supported_features) {
            u64 negotiated = 0;
            if (is_feature_set(supported_features, VIRTIO_GPU_F_VIRGL)) {
                dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: VirGL is available, enabling");
                negotiated |= VIRTIO_GPU_F_VIRGL;
                m_has_virgl_support = true;
            }
            if (is_feature_set(supported_features, VIRTIO_GPU_F_EDID))
                negotiated |= VIRTIO_GPU_F_EDID;
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
    } else {
        VERIFY_NOT_REACHED();
    }
}

Graphics::VirtIOGPU::ResourceID VirtIOGraphicsAdapter::allocate_resource_id(Badge<VirtIODisplayConnector>)
{
    return m_resource_id_counter++;
}

Graphics::VirtIOGPU::ContextID VirtIOGraphicsAdapter::allocate_context_id(Badge<VirtIODisplayConnector>)
{
    // FIXME: This should really be tracked using a bitmap, instead of an atomic counter
    return m_context_id_counter++;
}

bool VirtIOGraphicsAdapter::handle_device_config_change()
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

void VirtIOGraphicsAdapter::handle_queue_update(u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Handle queue update");
    VERIFY(queue_index == CONTROLQ);

    auto& queue = get_queue(CONTROLQ);
    SpinlockLocker queue_lock(queue.lock());
    queue.discard_used_buffers();
    m_outstanding_request.wake_all();
}

u32 VirtIOGraphicsAdapter::get_pending_events()
{
    return config_read32(*m_device_configuration, DEVICE_EVENTS_READ);
}

void VirtIOGraphicsAdapter::clear_pending_events(u32 event_bitmask)
{
    config_write32(*m_device_configuration, DEVICE_EVENTS_CLEAR, event_bitmask);
}

ErrorOr<void> VirtIOGraphicsAdapter::query_and_set_edid(u32 scanout_id, VirtIODisplayConnector& display_connector)
{
    SpinlockLocker locker(m_operation_lock);
    if (!is_feature_accepted(VIRTIO_GPU_F_EDID))
        return Error::from_errno(ENOTSUP);

    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::GetEDID>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::GetEDIDResponse>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_GET_EDID, 0);

    request.scanout_id = scanout_id;
    request.padding = 0;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    if (response.header.type != to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_EDID))
        return Error::from_string_literal("VirtIO::GraphicsAdapter: Failed to get EDID");

    if (response.size == 0)
        return Error::from_string_literal("VirtIO::GraphicsAdapter: Failed to get EDID, empty buffer");

    Array<u8, 128> raw_edid;
    memcpy(raw_edid.data(), response.edid, min(sizeof(raw_edid), response.size));
    display_connector.set_edid_bytes({}, raw_edid);
    return {};
}

Graphics::VirtIOGPU::ResourceID VirtIOGraphicsAdapter::create_2d_resource(Graphics::VirtIOGPU::Protocol::Rect rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceCreate2D>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_CREATE_2D, 0);

    auto resource_id = allocate_resource_id();
    request.resource_id = resource_id.value();
    request.width = rect.width;
    request.height = rect.height;
    request.format = to_underlying(Graphics::VirtIOGPU::Protocol::TextureFormat::VIRTIO_GPU_FORMAT_B8G8R8X8_UNORM);

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Allocated 2d resource with id {}", resource_id.value());
    return resource_id;
}

Graphics::VirtIOGPU::ResourceID VirtIOGraphicsAdapter::create_3d_resource(Graphics::VirtIOGPU::Protocol::Resource3DSpecification const& resource_3d_specification)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceCreate3D>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_CREATE_3D, 0);

    auto resource_id = allocate_resource_id();
    request.resource_id = resource_id.value();
    // TODO: Abstract this out a bit more
    u32* start_of_copied_fields = &request.target;

    // Validate that the sub copy from the resource_3d_specification to the offset of the request fits.
    static_assert((sizeof(request) - offsetof(Graphics::VirtIOGPU::Protocol::ResourceCreate3D, target) == sizeof(resource_3d_specification)));
    memcpy(start_of_copied_fields, &resource_3d_specification, sizeof(resource_3d_specification));

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == static_cast<u32>(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Allocated 3d resource with id {}", resource_id.value());
    return resource_id;
}

void VirtIOGraphicsAdapter::ensure_backing_storage(Graphics::VirtIOGPU::ResourceID resource_id, Memory::Region const& region, size_t buffer_offset, size_t buffer_length)
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
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Allocated backing storage");
}

void VirtIOGraphicsAdapter::detach_backing_storage(Graphics::VirtIOGPU::ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceDetachBacking>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_DETACH_BACKING, 0);
    request.resource_id = resource_id.value();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Detached backing storage");
}

void VirtIOGraphicsAdapter::set_scanout_resource(Graphics::VirtIOGPU::ScanoutID scanout, Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect rect)
{
    VERIFY(m_operation_lock.is_locked());
    {
        // We need to scope the request/response here so that we can query display information later on
        auto writer = create_scratchspace_writer();
        auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::SetScanOut>();
        auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

        populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_SET_SCANOUT, 0);
        request.resource_id = resource_id.value();
        request.scanout_id = scanout.value();
        request.rect = rect;

        synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

        VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
        dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Set backing scanout");
    }
}

void VirtIOGraphicsAdapter::transfer_framebuffer_data_to_host(Graphics::VirtIOGPU::ScanoutID scanout, Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::TransferToHost2D>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D, 0);
    request.offset = (dirty_rect.x + (dirty_rect.y * m_scanouts[scanout.value()].display_connector->display_information({}).rect.width)) * sizeof(u32);
    request.resource_id = resource_id.value();
    request.rect = dirty_rect;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

void VirtIOGraphicsAdapter::flush_displayed_image(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceFlush>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_FLUSH, 0);
    request.resource_id = resource_id.value();
    request.rect = dirty_rect;

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request), sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

void VirtIOGraphicsAdapter::synchronous_virtio_gpu_command(PhysicalAddress buffer_start, size_t request_size, size_t response_size)
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

void VirtIOGraphicsAdapter::populate_virtio_gpu_request_header(Graphics::VirtIOGPU::Protocol::ControlHeader& header, Graphics::VirtIOGPU::Protocol::CommandType ctrl_type, u32 flags)
{
    header.type = to_underlying(ctrl_type);
    header.flags = flags;
    header.fence_id = 0;
    header.context_id = 0;
    header.padding = 0;
}

void VirtIOGraphicsAdapter::flush_dirty_rectangle(Graphics::VirtIOGPU::ScanoutID scanout_id, Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect)
{
    VERIFY(m_operation_lock.is_locked());
    transfer_framebuffer_data_to_host(scanout_id, resource_id, dirty_rect);
    flush_displayed_image(resource_id, dirty_rect);
}

Graphics::VirtIOGPU::ResourceID VirtIOGraphicsAdapter::allocate_resource_id()
{
    return m_resource_id_counter++;
}

Graphics::VirtIOGPU::ContextID VirtIOGraphicsAdapter::allocate_context_id()
{
    // FIXME: This should really be tracked using a bitmap, instead of an atomic counter
    return m_context_id_counter++;
}

void VirtIOGraphicsAdapter::delete_resource(Graphics::VirtIOGPU::ResourceID resource_id)
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

void VirtIOGraphicsAdapter::initialize_3d_device()
{
    if (m_has_virgl_support) {
        SpinlockLocker locker(m_operation_lock);
        m_3d_device = VirtIOGPU3DDevice::must_create(*this);
    }
}

Graphics::VirtIOGPU::ContextID VirtIOGraphicsAdapter::create_context()
{
    VERIFY(m_operation_lock.is_locked());
    auto ctx_id = allocate_context_id();
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

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
    return ctx_id;
}

void VirtIOGraphicsAdapter::submit_command_buffer(Graphics::VirtIOGPU::ContextID context_id, Function<size_t(Bytes)> buffer_writer)
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
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Sending command buffer of length {}", request.size);
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    synchronous_virtio_gpu_command(start_of_scratch_space(), sizeof(request) + request.size, sizeof(response));

    VERIFY(response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA));
}

void VirtIOGraphicsAdapter::attach_resource_to_context(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::ContextID context_id)
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
