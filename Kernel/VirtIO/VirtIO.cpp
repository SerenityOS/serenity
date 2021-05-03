/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/VirtIO/VirtIO.h>

namespace Kernel {

void VirtIO::detect()
{
    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        if (address.is_null() || id.is_null())
            return;
        if (id.vendor_id != VIRTIO_PCI_VENDOR_ID)
            return;
    });
}

VirtIODevice::VirtIODevice(PCI::Address address, const char* class_name)
    : PCI::Device(address, PCI::get_interrupt_line(address))
    , m_class_name(class_name)
    , m_io_base(IOAddress(PCI::get_BAR0(pci_address()) & ~1))
{
    dbgln("{}: Found @ {}", m_class_name, pci_address());

    enable_bus_mastering(pci_address());

    reset_device();
    set_status_bit(DEVICE_STATUS_ACKNOWLEDGE);

    auto capabilities = PCI::get_physical_id(address).capabilities();
    for (auto& capability : capabilities) {
        if (capability.id() == PCI_CAPABILITY_VENDOR_SPECIFIC) {
            // We have a virtio_pci_cap
            Configuration cfg = {};
            cfg.cfg_type = capability.read8(0x3);
            switch (cfg.cfg_type) {
            case VIRTIO_PCI_CAP_COMMON_CFG:
            case VIRTIO_PCI_CAP_NOTIFY_CFG:
            case VIRTIO_PCI_CAP_ISR_CFG:
            case VIRTIO_PCI_CAP_DEVICE_CFG:
            case VIRTIO_PCI_CAP_PCI_CFG: {
                auto cap_length = capability.read8(0x2);
                if (cap_length < 0x10) {
                    dbgln("{}: Unexpected capability size: {}", m_class_name, cap_length);
                    break;
                }
                cfg.bar = capability.read8(0x4);
                if (cfg.bar > 0x5) {
                    dbgln("{}: Unexpected capability bar value: {}", m_class_name, cfg.bar);
                    break;
                }
                cfg.offset = capability.read32(0x8);
                cfg.length = capability.read32(0xc);
                dbgln_if(VIRTIO_DEBUG, "{}: Found configuration {}, bar: {}, offset: {}, length: {}", m_class_name, cfg.cfg_type, cfg.bar, cfg.offset, cfg.length);
                m_configs.append(cfg);

                if (cfg.cfg_type == VIRTIO_PCI_CAP_COMMON_CFG)
                    m_use_mmio = true;
                else if (cfg.cfg_type == VIRTIO_PCI_CAP_NOTIFY_CFG)
                    m_notify_multiplier = capability.read32(0x10);
                break;
            }
            default:
                dbgln("{}: Unknown capability configuration type: {}", m_class_name, cfg.cfg_type);
                break;
            }
        }
    }

    m_common_cfg = get_config(VIRTIO_PCI_CAP_COMMON_CFG, 0);
    m_notify_cfg = get_config(VIRTIO_PCI_CAP_NOTIFY_CFG, 0);
    m_isr_cfg = get_config(VIRTIO_PCI_CAP_ISR_CFG, 0);

    set_status_bit(DEVICE_STATUS_DRIVER);
}

VirtIODevice::~VirtIODevice()
{
}

auto VirtIODevice::mapping_for_bar(u8 bar) -> MappedMMIO&
{
    VERIFY(m_use_mmio);
    auto& mapping = m_mmio[bar];
    if (!mapping.base) {
        mapping.size = PCI::get_BAR_space_size(pci_address(), bar);
        mapping.base = MM.allocate_kernel_region(PhysicalAddress(page_base_of(PCI::get_BAR(pci_address(), bar))), page_round_up(mapping.size), "VirtIO MMIO", Region::Access::Read | Region::Access::Write, Region::Cacheable::No);
        if (!mapping.base)
            dbgln("{}: Failed to map bar {}", m_class_name, bar);
    }
    return mapping;
}

void VirtIODevice::notify_queue(u16 queue_index)
{
    dbgln("VirtIODevice: notifying about queue change at idx: {}", queue_index);
    if (!m_use_mmio)
        out<u16>(REG_QUEUE_NOTIFY, queue_index);
    else
        config_write16(m_notify_cfg, get_queue(queue_index)->notify_offset() * m_notify_multiplier, queue_index);
}

u8 VirtIODevice::config_read8(const Configuration* config, u32 offset)
{
    return mapping_for_bar(config->bar).read<u8>(config->offset + offset);
}

u16 VirtIODevice::config_read16(const Configuration* config, u32 offset)
{
    return mapping_for_bar(config->bar).read<u16>(config->offset + offset);
}

u32 VirtIODevice::config_read32(const Configuration* config, u32 offset)
{
    return mapping_for_bar(config->bar).read<u32>(config->offset + offset);
}

void VirtIODevice::config_write8(const Configuration* config, u32 offset, u8 value)
{
    mapping_for_bar(config->bar).write(config->offset + offset, value);
}

void VirtIODevice::config_write16(const Configuration* config, u32 offset, u16 value)
{
    mapping_for_bar(config->bar).write(config->offset + offset, value);
}

void VirtIODevice::config_write32(const Configuration* config, u32 offset, u32 value)
{
    mapping_for_bar(config->bar).write(config->offset + offset, value);
}

void VirtIODevice::config_write64(const Configuration* config, u32 offset, u64 value)
{
    mapping_for_bar(config->bar).write(config->offset + offset, value);
}

u8 VirtIODevice::read_status_bits()
{
    if (!m_use_mmio)
        return in<u8>(REG_DEVICE_STATUS);
    return config_read8(m_common_cfg, COMMON_CFG_DEVICE_STATUS);
}

void VirtIODevice::clear_status_bit(u8 status_bit)
{
    m_status &= status_bit;
    if (!m_use_mmio)
        out<u8>(REG_DEVICE_STATUS, m_status);
    else
        config_write8(m_common_cfg, COMMON_CFG_DEVICE_STATUS, m_status);
}

void VirtIODevice::set_status_bit(u8 status_bit)
{
    m_status |= status_bit;
    if (!m_use_mmio)
        out<u8>(REG_DEVICE_STATUS, m_status);
    else
        config_write8(m_common_cfg, COMMON_CFG_DEVICE_STATUS, m_status);
}

u64 VirtIODevice::get_device_features()
{
    if (!m_use_mmio)
        return in<u32>(REG_DEVICE_FEATURES);
    config_write32(m_common_cfg, COMMON_CFG_DEVICE_FEATURE_SELECT, 0);
    auto lower_bits = config_read32(m_common_cfg, COMMON_CFG_DEVICE_FEATURE);
    config_write32(m_common_cfg, COMMON_CFG_DEVICE_FEATURE_SELECT, 1);
    u64 upper_bits = (u64)config_read32(m_common_cfg, COMMON_CFG_DEVICE_FEATURE) << 32;
    return upper_bits | lower_bits;
}

bool VirtIODevice::accept_device_features(u64 device_features, u64 accepted_features)
{
    VERIFY(!m_did_accept_features);
    m_did_accept_features = true;

    if (is_feature_set(device_features, VIRTIO_F_VERSION_1)) {
        accepted_features |= VIRTIO_F_VERSION_1;
    } else {
        dbgln("{}: legacy device detected", m_class_name);
    }

    if (is_feature_set(device_features, VIRTIO_F_RING_PACKED)) {
        dbgln("{}: packed queues not yet supported", m_class_name);
        accepted_features &= ~(VIRTIO_F_RING_PACKED);
    }

    dbgln("VirtIOConsole: Device features: {}", device_features);
    dbgln("VirtIOConsole: Accepted features: {}", accepted_features);

    if (!m_use_mmio) {
        out<u32>(REG_GUEST_FEATURES, accepted_features);
    } else {
        config_write32(m_common_cfg, COMMON_CFG_DRIVER_FEATURE_SELECT, 0);
        config_write32(m_common_cfg, COMMON_CFG_DRIVER_FEATURE, accepted_features);
        config_write32(m_common_cfg, COMMON_CFG_DRIVER_FEATURE_SELECT, 1);
        config_write32(m_common_cfg, COMMON_CFG_DRIVER_FEATURE, accepted_features >> 32);
    }
    set_status_bit(DEVICE_STATUS_FEATURES_OK);
    m_status = read_status_bits();
    if (!(m_status & DEVICE_STATUS_FEATURES_OK)) {
        set_status_bit(DEVICE_STATUS_FAILED);
        dbgln("{}: Features not accepted by host!", m_class_name);
        return false;
    }

    m_accepted_features = accepted_features;
    dbgln_if(VIRTIO_DEBUG, "{}: Features accepted by host", m_class_name);
    return true;
}

auto VirtIODevice::get_common_config(u32 index) const -> const Configuration*
{
    if (index == 0)
        return m_common_cfg;
    return get_config(VIRTIO_PCI_CAP_COMMON_CFG, index);
}

auto VirtIODevice::get_device_config(u32 index) const -> const Configuration*
{
    return get_config(VIRTIO_PCI_CAP_DEVICE_CFG, index);
}

void VirtIODevice::reset_device()
{
    dbgln_if(VIRTIO_DEBUG, "{}: Reset device", m_class_name);
    if (!m_use_mmio) {
        clear_status_bit(0);
        while (read_status_bits() != 0) {
            // TODO: delay a bit?
        }
        return;
    } else if (m_common_cfg) {
        config_write8(m_common_cfg, COMMON_CFG_DEVICE_STATUS, 0);
        while (config_read8(m_common_cfg, COMMON_CFG_DEVICE_STATUS) != 0) {
            // TODO: delay a bit?
        }
        return;
    }
    dbgln_if(VIRTIO_DEBUG, "{}: No handle to device, cant reset", m_class_name);
}

bool VirtIODevice::setup_queue(u16 queue_index)
{
    if (!m_use_mmio || !m_common_cfg)
        return false;

    config_write16(m_common_cfg, COMMON_CFG_QUEUE_SELECT, queue_index);
    u16 queue_size = config_read16(m_common_cfg, COMMON_CFG_QUEUE_SIZE);
    if (queue_size == 0) {
        dbgln_if(VIRTIO_DEBUG, "{}: Queue[{}] is unavailable!", m_class_name, queue_index);
        return true;
    }

    u16 queue_notify_offset = config_read16(m_common_cfg, COMMON_CFG_QUEUE_NOTIFY_OFF);

    auto queue = make<VirtIOQueue>(queue_size, queue_notify_offset);
    if (queue->is_null())
        return false;

    config_write64(m_common_cfg, COMMON_CFG_QUEUE_DESC, queue->descriptor_area().get());
    config_write64(m_common_cfg, COMMON_CFG_QUEUE_DRIVER, queue->driver_area().get());
    config_write64(m_common_cfg, COMMON_CFG_QUEUE_DEVICE, queue->device_area().get());

    dbgln_if(VIRTIO_DEBUG, "{}: Queue[{}] size: {}", m_class_name, queue_index, queue_size);

    m_queues.append(move(queue));
    return true;
}

void VirtIODevice::set_requested_queue_count(u16 count)
{
    m_queue_count = count;
}

bool VirtIODevice::setup_queues()
{
    if (m_common_cfg) {
        auto maximum_queue_count = config_read16(m_common_cfg, COMMON_CFG_NUM_QUEUES);
        if (m_queue_count == 0) {
            m_queue_count = maximum_queue_count;
        } else if (m_queue_count > maximum_queue_count) {
            dbgln("{}: {} queues requested but only {} available!", m_class_name, m_queue_count, maximum_queue_count);
            return false;
        }
    }

    dbgln_if(VIRTIO_DEBUG, "{}: Setting up {} queues", m_class_name, m_queue_count);
    for (u16 i = 0; i < m_queue_count; i++) {
        if (!setup_queue(i))
            return false;
    }
    return true;
}

bool VirtIODevice::finish_init()
{
    VERIFY(m_did_accept_features);
    VERIFY(!(m_status & DEVICE_STATUS_DRIVER_OK));
    if (!setup_queues()) {
        dbgln("{}: Failed to setup queues", m_class_name);
        return false;
    }
    set_status_bit(DEVICE_STATUS_DRIVER_OK);
    dbgln_if(VIRTIO_DEBUG, "{}: Finished initialization", m_class_name);
    return true;
}

void VirtIODevice::supply_buffer_and_notify(u16 queue_index, const u8* buffer, u32 len, BufferType buffer_type)
{
    VERIFY(queue_index < m_queue_count);
    if (get_queue(queue_index)->supply_buffer(buffer, len, buffer_type))
        notify_queue(queue_index);
}

u8 VirtIODevice::isr_status()
{
    if (!m_use_mmio)
        return in<u8>(REG_ISR_STATUS);
    return config_read8(m_isr_cfg, 0);
}

void VirtIODevice::handle_irq(const RegisterState&)
{
    u8 isr_type = isr_status();
    dbgln_if(VIRTIO_DEBUG, "VirtIODevice: Handling interrupt with status: {}", isr_type);
    if (isr_type & DEVICE_CONFIG_INTERRUPT)
        handle_device_config_change();
    if (isr_type & QUEUE_INTERRUPT) {
        for (auto& queue : m_queues) {
            if (queue.handle_interrupt())
                return;
        }
    }
}

}
