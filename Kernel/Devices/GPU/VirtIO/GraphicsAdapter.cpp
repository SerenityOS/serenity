/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinaryBufferWriter.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Devices/GPU/VirtIO/Console.h>
#include <Kernel/Devices/GPU/VirtIO/DisplayConnector.h>
#include <Kernel/Devices/GPU/VirtIO/GPU3DDevice.h>
#include <Kernel/Devices/GPU/VirtIO/GraphicsAdapter.h>

namespace Kernel {

#define DEVICE_EVENTS_READ 0x0
#define DEVICE_EVENTS_CLEAR 0x4
#define DEVICE_NUM_SCANOUTS 0x8

static constexpr size_t COMMAND_TIMEOUT_US = 200'000;

ErrorOr<bool> VirtIOGraphicsAdapter::probe(PCI::DeviceIdentifier const& device_identifier)
{
    return device_identifier.hardware_id().vendor_id == PCI::VendorID::VirtIO;
}

ErrorOr<NonnullLockRefPtr<GPUDevice>> VirtIOGraphicsAdapter::create(PCI::DeviceIdentifier const& device_identifier)
{
    // Setup memory transfer region
    auto scratch_space_region = TRY(MM.allocate_contiguous_kernel_region(
        32 * PAGE_SIZE,
        "VirtGPU Scratch Space"sv,
        Memory::Region::Access::ReadWrite));

    auto active_context_ids = TRY(Bitmap::create(VREND_MAX_CTX, false));
    auto pci_transport_link = TRY(VirtIO::PCIeTransportLink::create(device_identifier));
    auto adapter = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) VirtIOGraphicsAdapter(move(pci_transport_link), move(active_context_ids), move(scratch_space_region))));
    TRY(adapter->initialize_virtio_resources());
    TRY(adapter->initialize_adapter());
    return adapter;
}

ErrorOr<void> VirtIOGraphicsAdapter::initialize_adapter()
{
    VERIFY(m_num_scanouts <= VIRTIO_GPU_MAX_SCANOUTS);
    TRY(initialize_3d_device());
    for (size_t index = 0; index < m_num_scanouts; index++) {
        auto display_connector = TRY(VirtIODisplayConnector::create(*this, index));
        m_scanouts[index].display_connector = display_connector;
        TRY(query_and_set_edid(index, *display_connector));
        display_connector->set_safe_mode_setting_after_initialization({});
        display_connector->initialize_console({});
    }
    return {};
}

ErrorOr<void> VirtIOGraphicsAdapter::mode_set_resolution(Badge<VirtIODisplayConnector>, VirtIODisplayConnector& connector, size_t width, size_t height)
{
    SpinlockLocker locker(m_operation_lock);
    VERIFY(connector.scanout_id() < VIRTIO_GPU_MAX_SCANOUTS);
    auto rounded_buffer_size = TRY(calculate_framebuffer_size(width, height));
    TRY(attach_physical_range_to_framebuffer(connector, true, 0, rounded_buffer_size));
    return {};
}

void VirtIOGraphicsAdapter::set_dirty_displayed_rect(Badge<VirtIODisplayConnector>, VirtIODisplayConnector& connector, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect, bool main_buffer)
{
    VERIFY(m_operation_lock.is_locked());
    VERIFY(connector.scanout_id() < VIRTIO_GPU_MAX_SCANOUTS);
    Scanout::PhysicalBuffer& buffer = main_buffer ? m_scanouts[connector.scanout_id().value()].main_buffer : m_scanouts[connector.scanout_id().value()].back_buffer;
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

ErrorOr<void> VirtIOGraphicsAdapter::flush_displayed_image(Badge<VirtIODisplayConnector>, VirtIODisplayConnector& connector, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect, bool main_buffer)
{
    VERIFY(m_operation_lock.is_locked());
    VERIFY(connector.scanout_id() < VIRTIO_GPU_MAX_SCANOUTS);
    Scanout::PhysicalBuffer& buffer = main_buffer ? m_scanouts[connector.scanout_id().value()].main_buffer : m_scanouts[connector.scanout_id().value()].back_buffer;
    TRY(flush_displayed_image(buffer.resource_id, dirty_rect));
    buffer.dirty_rect = {};
    return {};
}

ErrorOr<void> VirtIOGraphicsAdapter::transfer_framebuffer_data_to_host(Badge<VirtIODisplayConnector>, VirtIODisplayConnector& connector, Graphics::VirtIOGPU::Protocol::Rect const& rect, bool main_buffer)
{
    VERIFY(m_operation_lock.is_locked());
    VERIFY(connector.scanout_id() < VIRTIO_GPU_MAX_SCANOUTS);
    Scanout::PhysicalBuffer& buffer = main_buffer ? m_scanouts[connector.scanout_id().value()].main_buffer : m_scanouts[connector.scanout_id().value()].back_buffer;
    TRY(transfer_framebuffer_data_to_host(connector.scanout_id(), buffer.resource_id, rect));
    return {};
}

ErrorOr<void> VirtIOGraphicsAdapter::attach_physical_range_to_framebuffer(VirtIODisplayConnector& connector, bool main_buffer, size_t framebuffer_offset, size_t framebuffer_size)
{
    VERIFY(m_operation_lock.is_locked());
    Scanout::PhysicalBuffer& buffer = main_buffer ? m_scanouts[connector.scanout_id().value()].main_buffer : m_scanouts[connector.scanout_id().value()].back_buffer;
    buffer.framebuffer_offset = framebuffer_offset;

    // 1. Create BUFFER using VIRTIO_GPU_CMD_RESOURCE_CREATE_2D
    if (buffer.resource_id.value() != 0) {
        // FIXME: Do we need to remove the resource regardless of this condition?
        // Do we need to remove it if any of the code below fails for some reason?
        TRY(delete_resource(buffer.resource_id));
    }

    auto display_info = connector.display_information({});
    buffer.resource_id = TRY(create_2d_resource(display_info.rect));

    // 2. Attach backing storage using  VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING
    TRY(ensure_backing_storage(buffer.resource_id, connector.framebuffer_region(), buffer.framebuffer_offset, framebuffer_size));
    // 3. Use VIRTIO_GPU_CMD_SET_SCANOUT to link the framebuffer to a display scanout.
    TRY(set_scanout_resource(connector.scanout_id(), buffer.resource_id, display_info.rect));

    // Make sure we constrain the existing dirty rect (if any)
    if (buffer.dirty_rect.width != 0 || buffer.dirty_rect.height != 0) {
        auto dirty_right = buffer.dirty_rect.x + buffer.dirty_rect.width;
        auto dirty_bottom = buffer.dirty_rect.y + buffer.dirty_rect.height;
        buffer.dirty_rect.width = min(dirty_right, display_info.rect.x + display_info.rect.width) - buffer.dirty_rect.x;
        buffer.dirty_rect.height = min(dirty_bottom, display_info.rect.y + display_info.rect.height) - buffer.dirty_rect.y;
    }
    return {};
}

VirtIOGraphicsAdapter::VirtIOGraphicsAdapter(NonnullOwnPtr<VirtIO::TransportEntity> transport_entity, Bitmap&& active_context_ids, NonnullOwnPtr<Memory::Region> scratch_space_region)
    : VirtIO::Device(move(transport_entity))
    , m_scratch_space(move(scratch_space_region))
{
    m_active_context_ids.with([&](Bitmap& my_active_context_ids) {
        my_active_context_ids = move(active_context_ids);
        // Note: Context ID 0 is invalid, so mark it as in use.
        my_active_context_ids.set(0, true);
    });
}

ErrorOr<void> VirtIOGraphicsAdapter::initialize_virtio_resources()
{
    TRY(VirtIO::Device::initialize_virtio_resources());
    auto* config = TRY(transport_entity().get_config(VirtIO::ConfigurationType::Device));
    m_device_configuration = config;
    TRY(negotiate_features([&](u64 supported_features) {
        u64 negotiated = 0;
        if (is_feature_set(supported_features, VIRTIO_GPU_F_VIRGL)) {
            dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: VirGL is available, enabling");
            negotiated |= VIRTIO_GPU_F_VIRGL;
            m_has_virgl_support = true;
        }
        if (is_feature_set(supported_features, VIRTIO_GPU_F_EDID))
            negotiated |= VIRTIO_GPU_F_EDID;
        return negotiated;
    }));
    transport_entity().read_config_atomic([&]() {
        m_num_scanouts = transport_entity().config_read32(*config, DEVICE_NUM_SCANOUTS);
    });
    dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: num_scanouts: {}", m_num_scanouts);
    TRY(setup_queues(2)); // CONTROLQ + CURSORQ
    finish_init();
    return {};
}

ErrorOr<void> VirtIOGraphicsAdapter::handle_device_config_change()
{
    auto events = get_pending_events();
    if (events & VIRTIO_GPU_EVENT_DISPLAY) {
        // The host window was resized, in SerenityOS we completely ignore this event
        dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Ignoring virtio gpu display resize event");
        clear_pending_events(VIRTIO_GPU_EVENT_DISPLAY);
    }
    if (events & ~VIRTIO_GPU_EVENT_DISPLAY) {
        dbgln("VirtIO::GraphicsAdapter: Got unknown device config change event: {:#x}", events);
        return Error::from_errno(EIO);
    }
    return {};
}

void VirtIOGraphicsAdapter::handle_queue_update(u16)
{
}

u32 VirtIOGraphicsAdapter::get_pending_events()
{
    return transport_entity().config_read32(*m_device_configuration, DEVICE_EVENTS_READ);
}

void VirtIOGraphicsAdapter::clear_pending_events(u32 event_bitmask)
{
    transport_entity().config_write32(*m_device_configuration, DEVICE_EVENTS_CLEAR, event_bitmask);
}

static void populate_virtio_gpu_request_header(Graphics::VirtIOGPU::Protocol::ControlHeader& header, Graphics::VirtIOGPU::Protocol::CommandType ctrl_type, u32 flags)
{
    header.type = to_underlying(ctrl_type);
    header.flags = flags;
    header.fence_id = 0;
    header.context_id = 0;
    header.padding = 0;
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

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request), sizeof(response)));

    if (response.header.type != to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_EDID)) {
        dmesgln("VirtIO::GraphicsAdapter: Failed to get EDID");
        return Error::from_errno(ENOTSUP);
    }

    if (response.size == 0) {
        dmesgln("VirtIO::GraphicsAdapter: Failed to get EDID, empty buffer");
        return Error::from_errno(EIO);
    }

    Array<u8, 128> raw_edid;
    memcpy(raw_edid.data(), response.edid, min(sizeof(raw_edid), response.size));
    display_connector.set_edid_bytes({}, raw_edid);
    return {};
}

ErrorOr<Graphics::VirtIOGPU::ResourceID> VirtIOGraphicsAdapter::create_2d_resource(Graphics::VirtIOGPU::Protocol::Rect rect)
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

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request), sizeof(response)));

    if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA)) {
        dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Allocated 2d resource with id {}", resource_id.value());
        return resource_id;
    }
    return EIO;
}

ErrorOr<Graphics::VirtIOGPU::ResourceID> VirtIOGraphicsAdapter::create_3d_resource(Graphics::VirtIOGPU::Protocol::Resource3DSpecification const& resource_3d_specification)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceCreate3D>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_CREATE_3D, 0);

    // FIXME: What would be an appropriate resource free-ing mechanism to use in case anything
    // after this fails?
    auto resource_id = allocate_resource_id();
    request.resource_id = resource_id.value();
    // TODO: Abstract this out a bit more
    u32* start_of_copied_fields = &request.target;

    // Validate that the sub copy from the resource_3d_specification to the offset of the request fits.
    static_assert((sizeof(request) - offsetof(Graphics::VirtIOGPU::Protocol::ResourceCreate3D, target) == sizeof(resource_3d_specification)));
    memcpy(start_of_copied_fields, &resource_3d_specification, sizeof(resource_3d_specification));

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request), sizeof(response)));

    if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA)) {
        dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Allocated 3d resource with id {}", resource_id.value());
        return resource_id;
    }
    return EIO;
}

ErrorOr<void> VirtIOGraphicsAdapter::ensure_backing_storage(Graphics::VirtIOGPU::ResourceID resource_id, Memory::Region const& region, size_t buffer_offset, size_t buffer_length)
{
    VERIFY(m_operation_lock.is_locked());

    VERIFY(buffer_offset % PAGE_SIZE == 0);
    VERIFY(buffer_length % PAGE_SIZE == 0);
    auto first_page_index = buffer_offset / PAGE_SIZE;
    size_t num_mem_regions = buffer_length / PAGE_SIZE;

    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceAttachBacking>();
    size_t const header_block_size = sizeof(request) + num_mem_regions * sizeof(Graphics::VirtIOGPU::Protocol::MemoryEntry);

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING, 0);
    request.resource_id = resource_id.value();
    request.num_entries = num_mem_regions;
    for (size_t i = 0; i < num_mem_regions; ++i) {
        auto& memory_entry = writer.append_structure<Graphics::VirtIOGPU::Protocol::MemoryEntry>();
        memory_entry.address = region.physical_page(first_page_index + i)->paddr().get();
        memory_entry.length = PAGE_SIZE;
    }

    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), header_block_size, sizeof(response)));

    if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA)) {
        dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Allocated backing storage");
        return {};
    }
    return EIO;
}

ErrorOr<void> VirtIOGraphicsAdapter::detach_backing_storage(Graphics::VirtIOGPU::ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceDetachBacking>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_DETACH_BACKING, 0);
    request.resource_id = resource_id.value();

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request), sizeof(response)));

    if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA)) {
        dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Detached backing storage");
        return {};
    }
    return EIO;
}

ErrorOr<void> VirtIOGraphicsAdapter::set_scanout_resource(Graphics::VirtIOGPU::ScanoutID scanout, Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect rect)
{
    VERIFY(m_operation_lock.is_locked());
    // We need to scope the request/response here so that we can query display information later on
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::SetScanOut>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_SET_SCANOUT, 0);
    request.resource_id = resource_id.value();
    request.scanout_id = scanout.value();
    request.rect = rect;

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request), sizeof(response)));

    if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA)) {
        dbgln_if(VIRTIO_DEBUG, "VirtIO::GraphicsAdapter: Set backing scanout");
        return {};
    }
    return EIO;
}

ErrorOr<void> VirtIOGraphicsAdapter::transfer_framebuffer_data_to_host(Graphics::VirtIOGPU::ScanoutID scanout, Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::TransferToHost2D>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D, 0);
    request.offset = (dirty_rect.x + (dirty_rect.y * m_scanouts[scanout.value()].display_connector->display_information({}).rect.width)) * sizeof(u32);
    request.resource_id = resource_id.value();
    request.rect = dirty_rect;

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request), sizeof(response)));

    if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA))
        return {};
    return EIO;
}

ErrorOr<void> VirtIOGraphicsAdapter::flush_displayed_image(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceFlush>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_FLUSH, 0);
    request.resource_id = resource_id.value();
    request.rect = dirty_rect;

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request), sizeof(response)));

    if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA))
        return {};
    return EIO;
}

ErrorOr<void> VirtIOGraphicsAdapter::synchronous_virtio_gpu_command(size_t microseconds_timeout, PhysicalAddress buffer_start, size_t request_size, size_t response_size)
{
    VERIFY(m_operation_lock.is_locked());
    auto& queue = get_queue(CONTROLQ);
    queue.disable_interrupts();
    SpinlockLocker lock(queue.lock());
    VirtIO::QueueChain chain { queue };
    chain.add_buffer_to_chain(buffer_start, request_size, VirtIO::BufferType::DeviceReadable);
    chain.add_buffer_to_chain(buffer_start.offset(request_size), response_size, VirtIO::BufferType::DeviceWritable);
    supply_chain_and_notify(CONTROLQ, chain);
    full_memory_barrier();
    size_t current_time = 0;
    ScopeGuard clear_used_buffers([&] {
        queue.discard_used_buffers();
    });
    while (current_time < microseconds_timeout) {
        if (queue.new_data_available())
            return {};
        microseconds_delay(1);
        current_time++;
    }
    return Error::from_errno(EBUSY);
}

ErrorOr<void> VirtIOGraphicsAdapter::flush_dirty_rectangle(Graphics::VirtIOGPU::ScanoutID scanout_id, Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect)
{
    VERIFY(m_operation_lock.is_locked());
    TRY(transfer_framebuffer_data_to_host(scanout_id, resource_id, dirty_rect));
    TRY(flush_displayed_image(resource_id, dirty_rect));
    return {};
}

Graphics::VirtIOGPU::ResourceID VirtIOGraphicsAdapter::allocate_resource_id()
{
    return m_resource_id_counter++;
}

ErrorOr<void> VirtIOGraphicsAdapter::delete_resource(Graphics::VirtIOGPU::ResourceID resource_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ResourceUnref>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_RESOURCE_UNREF, 0);
    request.resource_id = resource_id.value();

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request), sizeof(response)));

    if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA))
        return {};
    return EIO;
}

ErrorOr<void> VirtIOGraphicsAdapter::initialize_3d_device()
{
    if (m_has_virgl_support) {
        SpinlockLocker locker(m_operation_lock);
        m_3d_device = TRY(VirtIOGPU3DDevice::create(*this));
    }
    return {};
}

ErrorOr<Graphics::VirtIOGPU::ContextID> VirtIOGraphicsAdapter::create_context()
{
    VERIFY(m_operation_lock.is_locked());
    return m_active_context_ids.with([&](Bitmap& active_context_ids) -> ErrorOr<Graphics::VirtIOGPU::ContextID> {
        auto maybe_available_id = active_context_ids.find_first_unset();
        if (!maybe_available_id.has_value()) {
            dmesgln("VirtIO::GraphicsAdapter: No available context IDs.");
            return Error::from_errno(ENXIO);
        }
        auto new_context_id = static_cast<u32>(maybe_available_id.value());

        auto writer = create_scratchspace_writer();
        auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ContextCreate>();
        auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();

        constexpr char const* region_name = "Serenity VirGL3D Context";
        populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_CTX_CREATE, 0);
        request.header.context_id = new_context_id;
        request.name_length = strlen(region_name);
        memset(request.debug_name.data(), 0, 64);
        VERIFY(request.name_length <= 64);
        memcpy(request.debug_name.data(), region_name, request.name_length);

        TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request), sizeof(response)));

        if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA)) {
            active_context_ids.set(maybe_available_id.value(), true);
            return static_cast<Graphics::VirtIOGPU::ContextID>(new_context_id);
        }
        return Error::from_errno(EIO);
    });
}

ErrorOr<void> VirtIOGraphicsAdapter::submit_command_buffer(Graphics::VirtIOGPU::ContextID context_id, Function<size_t(Bytes)> buffer_writer)
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

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request) + request.size, sizeof(response)));

    if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA))
        return {};
    return EIO;
}

ErrorOr<void> VirtIOGraphicsAdapter::attach_resource_to_context(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::ContextID context_id)
{
    VERIFY(m_operation_lock.is_locked());
    auto writer = create_scratchspace_writer();
    auto& request = writer.append_structure<Graphics::VirtIOGPU::Protocol::ContextAttachResource>();
    auto& response = writer.append_structure<Graphics::VirtIOGPU::Protocol::ControlHeader>();
    populate_virtio_gpu_request_header(request.header, Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_CMD_CTX_ATTACH_RESOURCE, 0);
    request.header.context_id = context_id.value();
    request.resource_id = resource_id.value();

    TRY(synchronous_virtio_gpu_command(COMMAND_TIMEOUT_US, start_of_scratch_space(), sizeof(request), sizeof(response)));

    if (response.type == to_underlying(Graphics::VirtIOGPU::Protocol::CommandType::VIRTIO_GPU_RESP_OK_NODATA))
        return {};
    return EIO;
}

}
