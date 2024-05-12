/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Sections.h>

namespace Kernel::VirtIO {

UNMAP_AFTER_INIT ErrorOr<void> Device::initialize_virtio_resources()
{
    TRY(m_transport_entity->locate_configurations_and_resources({}, *this));
    // NOTE: We enable interrupts at least after the m_register_bases[0] ptr is
    // assigned with an IOWindow, to ensure that in case of getting an interrupt
    // we can access registers from that IO window range.
    m_transport_entity->enable_interrupts({});

    // NOTE: Status bits should be set to 0 to keep them in sync, because
    // we reset the device shortly afterwards.
    m_status = 0;
    m_transport_entity->reset_device({});
    set_status_bit(DEVICE_STATUS_ACKNOWLEDGE);
    set_status_bit(DEVICE_STATUS_DRIVER);
    return {};
}

UNMAP_AFTER_INIT VirtIO::Device::Device(NonnullOwnPtr<TransportEntity> transport_entity)
    : m_class_name(transport_entity->determine_device_class_name())
    , m_transport_entity(move(transport_entity))
{
}

void Device::set_status_bit(u8 status_bit)
{
    m_status |= status_bit;
    m_transport_entity->set_status_bits({}, m_status);
}

ErrorOr<void> Device::accept_device_features(u64 device_features, u64 accepted_features)
{
    VERIFY(!m_did_accept_features.was_set());
    m_did_accept_features.set();

    if (is_feature_set(device_features, VIRTIO_F_VERSION_1)) {
        accepted_features |= VIRTIO_F_VERSION_1; // let the device know were not a legacy driver
    }

    if (is_feature_set(device_features, VIRTIO_F_RING_PACKED)) {
        dbgln_if(VIRTIO_DEBUG, "{}: packed queues not yet supported", m_class_name);
        accepted_features &= ~(VIRTIO_F_RING_PACKED);
    }

    // TODO: implement indirect descriptors to allow queue_size buffers instead of buffers totalling (PAGE_SIZE * queue_size) bytes
    if (is_feature_set(device_features, VIRTIO_F_INDIRECT_DESC)) {
        // accepted_features |= VIRTIO_F_INDIRECT_DESC;
    }

    if (is_feature_set(device_features, VIRTIO_F_IN_ORDER)) {
        accepted_features |= VIRTIO_F_IN_ORDER;
    }

    dbgln_if(VIRTIO_DEBUG, "{}: Device features: {}", m_class_name, device_features);
    dbgln_if(VIRTIO_DEBUG, "{}: Accepted features: {}", m_class_name, accepted_features);

    m_transport_entity->accept_device_features({}, accepted_features);
    set_status_bit(DEVICE_STATUS_FEATURES_OK);
    m_status = m_transport_entity->read_status_bits();
    if (!(m_status & DEVICE_STATUS_FEATURES_OK)) {
        set_status_bit(DEVICE_STATUS_FAILED);
        dbgln("{}: Features not accepted by host!", m_class_name);
        return Error::from_errno(EIO);
    }

    m_accepted_features = accepted_features;
    dbgln_if(VIRTIO_DEBUG, "{}: Features accepted by host", m_class_name);
    return {};
}

ErrorOr<void> Device::setup_queue(u16 queue_index)
{
    auto queue = TRY(m_transport_entity->setup_queue({}, queue_index));
    dbgln_if(VIRTIO_DEBUG, "{}: Queue[{}] configured with size: {}", m_class_name, queue_index, queue->size());

    TRY(m_queues.try_append(move(queue)));
    return {};
}

ErrorOr<void> Device::setup_queues(u16 requested_queue_count)
{
    VERIFY(!m_did_setup_queues.was_set());
    m_did_setup_queues.set();

    auto* common_cfg = TRY(m_transport_entity->get_config(ConfigurationType::Common));
    if (common_cfg) {
        auto maximum_queue_count = m_transport_entity->config_read16(*common_cfg, COMMON_CFG_NUM_QUEUES);
        if (requested_queue_count == 0) {
            m_queue_count = maximum_queue_count;
        } else if (requested_queue_count > maximum_queue_count) {
            dbgln("{}: {} queues requested but only {} available!", m_class_name, m_queue_count, maximum_queue_count);
            return Error::from_errno(ENXIO);
        } else {
            m_queue_count = requested_queue_count;
        }
    } else {
        m_queue_count = requested_queue_count;
        dbgln("{}: device's available queue count could not be determined!", m_class_name);
    }

    dbgln_if(VIRTIO_DEBUG, "{}: Setting up {} queues", m_class_name, m_queue_count);
    for (u16 i = 0; i < m_queue_count; i++)
        TRY(setup_queue(i));

    // NOTE: Queues can only be activated *after* all others queues were also configured
    for (u16 i = 0; i < m_queue_count; i++)
        TRY(m_transport_entity->activate_queue({}, i));
    return {};
}

void Device::finish_init()
{
    VERIFY(m_did_accept_features.was_set());       // ensure features were negotiated
    VERIFY(m_did_setup_queues.was_set());          // ensure queues were set-up
    VERIFY(!(m_status & DEVICE_STATUS_DRIVER_OK)); // ensure we didn't already finish the initialization

    set_status_bit(DEVICE_STATUS_DRIVER_OK);
    dbgln_if(VIRTIO_DEBUG, "{}: Finished initialization", m_class_name);
}

bool Device::handle_irq(Badge<TransportInterruptHandler>)
{
    u8 isr_type = m_transport_entity->isr_status();
    if ((isr_type & (QUEUE_INTERRUPT | DEVICE_CONFIG_INTERRUPT)) == 0) {
        dbgln_if(VIRTIO_DEBUG, "{}: Handling interrupt with unknown type: {}", class_name(), isr_type);
        return false;
    }
    if (isr_type & DEVICE_CONFIG_INTERRUPT) {
        dbgln_if(VIRTIO_DEBUG, "{}: VirtIO Device config interrupt!", class_name());
        if (handle_device_config_change().is_error()) {
            set_status_bit(DEVICE_STATUS_FAILED);
            dbgln("{}: Failed to handle device config change!", class_name());
        }
    }
    if (isr_type & QUEUE_INTERRUPT) {
        dbgln_if(VIRTIO_DEBUG, "{}: VirtIO Queue interrupt!", class_name());
        for (size_t i = 0; i < m_queues.size(); i++) {
            if (get_queue(i).new_data_available()) {
                handle_queue_update(i);
                return true;
            }
        }
        dbgln_if(VIRTIO_DEBUG, "{}: Got queue interrupt but all queues are up to date!", class_name());
    }
    return true;
}

void Device::supply_chain_and_notify(u16 queue_index, QueueChain& chain)
{
    auto& queue = get_queue(queue_index);
    VERIFY(&chain.queue() == &queue);
    VERIFY(queue.lock().is_locked());
    chain.submit_to_queue();
    auto descriptor = TransportEntity::NotifyQueueDescriptor { queue_index, get_queue(queue_index).notify_offset() };
    if (queue.should_notify())
        m_transport_entity->notify_queue({}, descriptor);
}

}
