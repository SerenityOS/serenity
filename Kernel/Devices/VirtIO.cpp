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

#include <AK/StringView.h>
#include <Kernel/Devices/VirtIO.h>
#include <Kernel/Devices/VirtIOConsole.h>
#include <Kernel/StdLib.h>

#define VIRTIO_DEBUG

namespace Kernel {

#define REG_DEVICE_FEATURES 0x0
#define REG_GUEST_FEATURES 0x4
#define REG_QUEUE_ADDRESS 0x8
#define REG_QUEUE_SIZE 0xc
#define REG_QUEUE_SELECT 0xe
#define REG_QUEUE_NOTIFY 0x10
#define REG_DEVICE_STATUS 0x12
#define REG_ISR_STATUS 0x13

#define DEVICE_STATUS_ACKNOWLEDGE (1 << 0)
#define DEVICE_STATUS_DRIVER (1 << 1)
#define DEVICE_STATUS_DRIVER_OK (1 << 2)
#define DEVICE_STATUS_FEATURES_OK (1 << 3)
#define DEVICE_STATUS_DEVICE_NEEDS_RESET (1 << 6)
#define DEVICE_STATUS_FAILED (1 << 7)

#define VIRTIO_F_VERSION_1 (1 << 5)
#define VIRTIO_F_RING_PACKED ((1 << 1) | VIRTIO_F_VERSION_1)

#define VIRTIO_PCI_CAP_COMMON_CFG 1
#define VIRTIO_PCI_CAP_NOTIFY_CFG 2
#define VIRTIO_PCI_CAP_ISR_CFG 3
#define VIRTIO_PCI_CAP_DEVICE_CFG 4
#define VIRTIO_PCI_CAP_PCI_CFG 5

// virtio_pci_common_cfg
#define COMMON_CFG_DEVICE_FEATURE_SELECT 0x0
#define COMMON_CFG_DEVICE_FEATURE 0x4
#define COMMON_CFG_DRIVER_FEATURE_SELECT 0x8
#define COMMON_CFG_DRIVER_FEATURE 0xc
#define COMMON_CFG_MSIX_CONFIG 0x10
#define COMMON_CFG_NUM_QUEUES 0x12
#define COMMON_CFG_DEVICE_STATUS 0x14
#define COMMON_CFG_CONFIG_GENERATION 0x15
#define COMMON_CFG_QUEUE_SELECT 0x16
#define COMMON_CFG_QUEUE_SIZE 0x18
#define COMMON_CFG_QUEUE_MSIX_VECTOR 0x1a
#define COMMON_CFG_QUEUE_ENABLE 0x1c
#define COMMON_CFG_QUEUE_NOTIFY_OFF 0x1e
#define COMMON_CFG_QUEUE_DESC 0x20
#define COMMON_CFG_QUEUE_DRIVER 0x28
#define COMMON_CFG_QUEUE_DEVICE 0x30

void VirtIO::detect()
{
    VirtIOConsole::detect();
}

VirtIOQueue::VirtIOQueue(u16 queue_size)
    : m_queue_size(queue_size)
{
    size_t size_of_descriptors = sizeof(VirtIOQueueDescriptor) * queue_size;
    size_t size_of_driver = (2 + queue_size) * sizeof(16);                        // size of VirtIOQueueDriver
    size_t size_of_device = 2 * sizeof(16) + queue_size * sizeof(VirtIOQueueDeviceItem); // size of VirtIOQueueDevice
    m_region = MM.allocate_contiguous_kernel_region(PAGE_ROUND_UP(size_of_descriptors + size_of_driver + size_of_device), "VirtIO queue", Region::Access::Read | Region::Access::Write);
    if (m_region) {
        // TODO: ensure alignment!!!
        u8* ptr = m_region->vaddr().as_ptr();
        memset(ptr, 0, m_region->size());
        m_descriptors = reinterpret_cast<VirtIOQueueDescriptor*>(ptr);
        m_driver = reinterpret_cast<VirtIOQueueDriver*>(ptr + size_of_descriptors);
        m_device = reinterpret_cast<VirtIOQueueDevice*>(ptr + size_of_descriptors + size_of_driver);
    }
}

VirtIOQueue::~VirtIOQueue()
{
}

void VirtIOQueue::enable_interrupts()
{
    m_device->flags = 0;
}

void VirtIOQueue::disable_interrupts()
{
    m_device->flags = 1;
}

VirtIODevice::VirtIODevice(PCI::Address address, u8 irq, const char* class_name)
    : PCI::Device(address, irq)
    , m_class_name(class_name)
    , m_io_base(IOAddress(PCI::get_BAR0(pci_address()) & ~1))
{
    klog() << m_class_name << ": Found @ " << pci_address();

    enable_bus_mastering(pci_address());

    set_status_bit(DEVICE_STATUS_ACKNOWLEDGE);

    PCI::enumerate_capabilities(address, [&](PCI::Capability& capability) {
        if (capability.is_vendor_specific()) {
            // We have a virtio_pci_cap
            Configuration cfg;
            cfg.cfg_type = capability.read8(0x3);
            switch (cfg.cfg_type) {
            case VIRTIO_PCI_CAP_COMMON_CFG:
            case VIRTIO_PCI_CAP_NOTIFY_CFG:
            case VIRTIO_PCI_CAP_ISR_CFG:
            case VIRTIO_PCI_CAP_DEVICE_CFG:
            case VIRTIO_PCI_CAP_PCI_CFG: {
                auto cap_length = capability.read8(0x2);
                if (cap_length < 0x10) {
                    klog() << m_class_name << ": Unexpected capability size: " << cap_length;
                    break;
                }
                cfg.bar = capability.read8(0x4);
                if (cfg.bar > 0x5) {
                    klog() << m_class_name << ": Unexpected capability bar value: " << cfg.bar;
                    break;
                }
                cfg.offset = capability.read32(0x8);
                cfg.length = capability.read32(0xc);
#ifdef VIRTIO_DEBUG
                klog() << m_class_name << "Found configuration " << cfg.cfg_type << ", bar: " << cfg.bar << " offset: " << cfg.offset << " length: " << cfg.length;
#endif
                m_config.append(cfg);

                if (cfg.cfg_type == VIRTIO_PCI_CAP_COMMON_CFG)
                    m_use_mmio = true;
                break;
            }
            default:
                klog() << m_class_name << ": Unknown capability configuration type: " << cfg.cfg_type;
                break;
            }
        }
    });

    m_common_cfg = get_config(VIRTIO_PCI_CAP_COMMON_CFG, 0);

    reset_device();
    set_status_bit(DEVICE_STATUS_DRIVER);
}

VirtIODevice::~VirtIODevice()
{
}

auto VirtIODevice::mapping_for_bar(u8 bar) -> MappedMMIO&
{
    ASSERT(m_use_mmio);
    auto& mapping = m_mmio[bar];
    if (!mapping.base) {
        mapping.size = PCI::get_BAR_space_size(pci_address(), bar);
        mapping.base = MM.allocate_kernel_region(PhysicalAddress(page_base_of(PCI::get_BAR(pci_address(), bar))), PAGE_ROUND_UP(mapping.size), "VirtIO MMIO", Region::Access::Read | Region::Access::Write, false, false);
        if (!mapping.base)
            klog() << m_class_name << ": Failed to map bar " << bar;
    }
    return mapping;
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

u32 VirtIODevice::get_device_features()
{
    if (!m_use_mmio)
        return in<u32>(REG_DEVICE_FEATURES);
    return config_read8(m_common_cfg, COMMON_CFG_DEVICE_FEATURE);
}

bool VirtIODevice::accept_device_features(u32 device_features, u32 accepted_features)
{
    ASSERT(!m_did_accept_features);
    m_did_accept_features = true;

    if (!m_use_mmio) {
        out<u32>(REG_GUEST_FEATURES, accepted_features);
    } else {
        config_write32(m_common_cfg, COMMON_CFG_DRIVER_FEATURE_SELECT, 0);
        config_write32(m_common_cfg, COMMON_CFG_DRIVER_FEATURE, accepted_features);
    }
    set_status_bit(DEVICE_STATUS_FEATURES_OK);
    m_status = read_status_bits();
    if (!(m_status & DEVICE_STATUS_FEATURES_OK)) {
        set_status_bit(DEVICE_STATUS_FAILED);
        klog() << m_class_name << ": Features not accepted by host!";
        return false;
    }

    if (is_feature_set(device_features, VIRTIO_F_RING_PACKED))
        dbg() << m_class_name << ": packed queues not yet supported";

    m_accepted_features = accepted_features;
#ifdef VIRTIO_DEBUG
    klog() << m_class_name << ": Features accepted by host";
#endif
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
    if (!m_use_mmio) {
        clear_status_bit(~0);
        while (read_status_bits() != 0) {
            // TODO: delay a bit?
        }
    } else if (m_common_cfg) {
#ifdef VIRTIO_DEBUG
        klog() << m_class_name << ": Reset device";
#endif
        config_write8(m_common_cfg, COMMON_CFG_DEVICE_STATUS, 0);
        while (config_read8(m_common_cfg, COMMON_CFG_DEVICE_STATUS) != 0) {
            // TODO: delay a bit?
        }
    } else {
        klog() << m_class_name << ": Cannot reset, no common config";
    }
}

bool VirtIODevice::setup_queue(u16 queue_index)
{
    if (!m_use_mmio || !m_common_cfg)
        return false;

    config_write16(m_common_cfg, COMMON_CFG_QUEUE_SELECT, queue_index);
    u16 queue_size = config_read16(m_common_cfg, COMMON_CFG_QUEUE_SIZE);
    if (queue_size == 0) {
        klog() << m_class_name << ": Queue[" << queue_index << "] has no size!";
        return true;
    }

    auto queue = make<VirtIOQueue>(queue_size);
    if (queue->is_null())
        return false;

    config_write64(m_common_cfg, COMMON_CFG_QUEUE_DESC, queue->descriptor_area().get());
    config_write64(m_common_cfg, COMMON_CFG_QUEUE_DRIVER, queue->driver_area().get());
    config_write64(m_common_cfg, COMMON_CFG_QUEUE_DEVICE, queue->device_area().get());

#ifdef VIRTIO_DEBUG
    klog() << m_class_name << ": Queue[" << queue_index << "] size: " << queue_size;
#endif

    m_queues.append(move(queue));
    return true;
}

bool VirtIODevice::setup_queues()
{
    if (m_common_cfg) {
        m_queue_count = config_read16(m_common_cfg, 0x12);
    } else {
        m_queue_count = 0;
    }

#ifdef VIRTIO_DEBUG
        klog() << m_class_name << ": Setting up " << m_queue_count << " queues";
#endif

    // TODO: can we find out how many queues we need to set up?
    for (u16 i = 0; i < m_queue_count; i++) {
        if (!setup_queue(i))
            return false;
    }
    return true;
}

bool VirtIODevice::finish_init()
{
    ASSERT(m_did_accept_features);
    ASSERT(!(m_status & DEVICE_STATUS_DRIVER_OK));
    if (!setup_queues()) {
        klog() << m_class_name << ": Failed to setup queues";
        return false;
    }
    set_status_bit(DEVICE_STATUS_DRIVER_OK);
#ifdef VIRTIO_DEBUG
    klog() << m_class_name << ": Finished initialization";
#endif
    return true;
}

}
