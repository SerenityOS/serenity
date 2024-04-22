/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/VirtIO/Transport/Entity.h>

namespace Kernel::VirtIO {

auto TransportEntity::mapping_for_resource_index(u8 resource_index) -> IOWindow&
{
    VERIFY(m_use_mmio.was_set());
    VERIFY(m_register_bases[resource_index]);
    return *m_register_bases[resource_index];
}

u8 TransportEntity::config_read8(Configuration const& config, u32 offset)
{
    return mapping_for_resource_index(config.resource_index).read8(config.offset + offset);
}

u16 TransportEntity::config_read16(Configuration const& config, u32 offset)
{
    return mapping_for_resource_index(config.resource_index).read16(config.offset + offset);
}

u32 TransportEntity::config_read32(Configuration const& config, u32 offset)
{
    return mapping_for_resource_index(config.resource_index).read32(config.offset + offset);
}

void TransportEntity::config_write8(Configuration const& config, u32 offset, u8 value)
{
    mapping_for_resource_index(config.resource_index).write8(config.offset + offset, value);
}

void TransportEntity::config_write16(Configuration const& config, u32 offset, u16 value)
{
    mapping_for_resource_index(config.resource_index).write16(config.offset + offset, value);
}

void TransportEntity::config_write32(Configuration const& config, u32 offset, u32 value)
{
    mapping_for_resource_index(config.resource_index).write32(config.offset + offset, value);
}

void TransportEntity::config_write64(Configuration const& config, u32 offset, u64 value)
{
    mapping_for_resource_index(config.resource_index).write32(config.offset + offset, (u32)(value & 0xFFFFFFFF));
    mapping_for_resource_index(config.resource_index).write32(config.offset + offset + 4, (u32)(value >> 32));
}

IOWindow& TransportEntity::base_io_window()
{
    VERIFY(m_register_bases[0]);
    return *m_register_bases[0];
}

u8 TransportEntity::isr_status()
{
    if (!m_isr_cfg)
        return base_io_window().read8(REG_ISR_STATUS);
    return config_read8(*m_isr_cfg, 0);
}

void TransportEntity::set_status_bits(Badge<VirtIO::Device>, u8 status_bits)
{
    return set_status_bits(status_bits);
}

void TransportEntity::set_status_bits(u8 status_bits)
{
    if (!m_common_cfg)
        base_io_window().write8(REG_DEVICE_STATUS, status_bits);
    else
        config_write8(*m_common_cfg, COMMON_CFG_DEVICE_STATUS, status_bits);
}

ErrorOr<NonnullOwnPtr<Queue>> TransportEntity::setup_queue(Badge<VirtIO::Device>, u16 queue_index)
{
    if (!m_common_cfg)
        return Error::from_errno(ENXIO);

    config_write16(*m_common_cfg, COMMON_CFG_QUEUE_SELECT, queue_index);
    u16 queue_size = config_read16(*m_common_cfg, COMMON_CFG_QUEUE_SIZE);
    if (queue_size == 0) {
        dbgln_if(VIRTIO_DEBUG, "Queue[{}] is unavailable!", queue_index);
        return Error::from_errno(ENXIO);
    }

    u16 queue_notify_offset = config_read16(*m_common_cfg, COMMON_CFG_QUEUE_NOTIFY_OFF);

    auto queue = TRY(Queue::try_create(queue_size, queue_notify_offset));

    config_write64(*m_common_cfg, COMMON_CFG_QUEUE_DESC, queue->descriptor_area().get());
    config_write64(*m_common_cfg, COMMON_CFG_QUEUE_DRIVER, queue->driver_area().get());
    config_write64(*m_common_cfg, COMMON_CFG_QUEUE_DEVICE, queue->device_area().get());
    return queue;
}

void TransportEntity::accept_device_features(Badge<VirtIO::Device>, u64 accepted_features)
{
    if (!m_common_cfg) {
        base_io_window().write32(REG_GUEST_FEATURES, accepted_features);
    } else {
        config_write32(*m_common_cfg, COMMON_CFG_DRIVER_FEATURE_SELECT, 0);
        config_write32(*m_common_cfg, COMMON_CFG_DRIVER_FEATURE, accepted_features);
        config_write32(*m_common_cfg, COMMON_CFG_DRIVER_FEATURE_SELECT, 1);
        config_write32(*m_common_cfg, COMMON_CFG_DRIVER_FEATURE, accepted_features >> 32);
    }
}

void TransportEntity::reset_device(Badge<VirtIO::Device>)
{
    if (!m_common_cfg) {
        set_status_bits(0);
        while (read_status_bits() != 0) {
            // TODO: delay a bit?
        }
        return;
    }
    config_write8(*m_common_cfg, COMMON_CFG_DEVICE_STATUS, 0);
    while (config_read8(*m_common_cfg, COMMON_CFG_DEVICE_STATUS) != 0) {
        // TODO: delay a bit?
    }
}

void TransportEntity::notify_queue(Badge<VirtIO::Device>, NotifyQueueDescriptor descriptor)
{
    dbgln_if(VIRTIO_DEBUG, "notifying about queue change at idx: {}", descriptor.queue_index);
    if (!m_notify_cfg)
        base_io_window().write16(REG_QUEUE_NOTIFY, descriptor.queue_index);
    else
        config_write16(*m_notify_cfg, descriptor.possible_notify_offset * m_notify_multiplier, descriptor.queue_index);
}

ErrorOr<void> TransportEntity::activate_queue(Badge<VirtIO::Device>, u16 queue_index)
{
    if (!m_common_cfg)
        return Error::from_errno(ENXIO);

    config_write16(*m_common_cfg, COMMON_CFG_QUEUE_SELECT, queue_index);
    config_write16(*m_common_cfg, COMMON_CFG_QUEUE_ENABLE, true);

    dbgln_if(VIRTIO_DEBUG, "Queue[{}] activated", queue_index);
    return {};
}

u64 TransportEntity::get_device_features()
{
    if (!m_common_cfg)
        return base_io_window().read32(REG_DEVICE_FEATURES);
    config_write32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE_SELECT, 0);
    auto lower_bits = config_read32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE);
    config_write32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE_SELECT, 1);
    u64 upper_bits = (u64)config_read32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE) << 32;
    return upper_bits | lower_bits;
}

u8 TransportEntity::read_status_bits()
{
    if (!m_common_cfg)
        return base_io_window().read8(REG_DEVICE_STATUS);
    return config_read8(*m_common_cfg, COMMON_CFG_DEVICE_STATUS);
}

}
