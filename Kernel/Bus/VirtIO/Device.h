/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SetOnce.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/VirtIO/Definitions.h>
#include <Kernel/Bus/VirtIO/Queue.h>
#include <Kernel/Bus/VirtIO/Transport/Entity.h>
#include <Kernel/Bus/VirtIO/Transport/InterruptHandler.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::VirtIO {

class Device {
public:
    virtual ~Device() = default;

    virtual ErrorOr<void> initialize_virtio_resources();

    bool handle_irq(Badge<TransportInterruptHandler>);

protected:
    virtual StringView class_name() const { return "VirtIO::Device"sv; }

    explicit Device(NonnullOwnPtr<TransportEntity>);

    void mask_status_bits(u8 status_mask);
    void set_status_bit(u8);
    ErrorOr<void> setup_queues(u16 requested_queue_count = 0);
    void finish_init();

    Queue& get_queue(u16 queue_index)
    {
        VERIFY(queue_index < m_queue_count);
        return *m_queues[queue_index];
    }

    Queue const& get_queue(u16 queue_index) const
    {
        VERIFY(queue_index < m_queue_count);
        return *m_queues[queue_index];
    }

    template<typename F>
    ErrorOr<void> negotiate_features(F f)
    {
        u64 device_features = m_transport_entity->get_device_features();
        u64 accept_features = f(device_features);
        VERIFY(!(~device_features & accept_features));
        return accept_device_features(device_features, accept_features);
    }

    static bool is_feature_set(u64 feature_set, u64 test_feature)
    {
        // features can have more than one bit
        return (feature_set & test_feature) == test_feature;
    }
    bool is_feature_accepted(u64 feature) const
    {
        VERIFY(m_did_accept_features.was_set());
        return is_feature_set(m_accepted_features, feature);
    }

    void supply_chain_and_notify(u16 queue_index, QueueChain& chain);

    virtual ErrorOr<void> handle_device_config_change() = 0;
    virtual void handle_queue_update(u16 queue_index) = 0;

    TransportEntity& transport_entity() { return *m_transport_entity; }

private:
    ErrorOr<void> accept_device_features(u64 device_features, u64 accepted_features);

    ErrorOr<void> setup_queue(u16 queue_index);
    ErrorOr<void> activate_queue(u16 queue_index);
    void notify_queue(u16 queue_index);

    Vector<NonnullOwnPtr<Queue>> m_queues;

    StringView const m_class_name;

    u16 m_queue_count { 0 };
    u8 m_status { 0 };
    u64 m_accepted_features { 0 };
    SetOnce m_did_accept_features;
    SetOnce m_did_setup_queues;

    NonnullOwnPtr<TransportEntity> const m_transport_entity;
};
}
