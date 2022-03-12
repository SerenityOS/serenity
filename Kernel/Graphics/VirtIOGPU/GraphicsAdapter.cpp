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
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>

namespace Kernel {

#define DEVICE_EVENTS_READ 0x0
#define DEVICE_EVENTS_CLEAR 0x4
#define DEVICE_NUM_SCANOUTS 0x8

NonnullRefPtr<VirtIOGraphicsAdapter> VirtIOGraphicsAdapter::initialize(PCI::DeviceIdentifier const& device_identifier)
{
    VERIFY(device_identifier.hardware_id().vendor_id == PCI::VendorID::VirtIO);
    auto adapter = adopt_ref(*new (nothrow) VirtIOGraphicsAdapter(device_identifier));
    adapter->initialize();
    MUST(adapter->initialize_adapter());
    return adapter;
}

ErrorOr<void> VirtIOGraphicsAdapter::initialize_adapter()
{
    VERIFY(m_num_scanouts <= VIRTIO_GPU_MAX_SCANOUTS);
    for (size_t index = 0; index < m_num_scanouts; index++) {
        m_scanouts[index].display_connector = VirtIODisplayConnector::must_create(*this, index);
    }
    return {};
}

VirtIOGraphicsAdapter::VirtIOGraphicsAdapter(PCI::DeviceIdentifier const& device_identifier)
    : VirtIO::Device(device_identifier)
{
}

bool VirtIOGraphicsAdapter::edid_feature_accepted() const
{
    return is_feature_accepted(VIRTIO_GPU_F_EDID);
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
    SpinlockLocker locker(m_resource_allocation_lock);
    m_resource_id_counter = m_resource_id_counter.value() + 1;
    return m_resource_id_counter;
}

Graphics::VirtIOGPU::ContextID VirtIOGraphicsAdapter::allocate_context_id(Badge<VirtIODisplayConnector>)
{
    SpinlockLocker locker(m_context_allocation_lock);
    // FIXME: This should really be tracked using a bitmap, instead of an atomic counter
    m_context_id_counter = m_context_id_counter.value() + 1;
    return m_context_id_counter;
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

void VirtIOGraphicsAdapter::wait_for_outstanding_request()
{
    m_outstanding_request.wait_forever();
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

}
