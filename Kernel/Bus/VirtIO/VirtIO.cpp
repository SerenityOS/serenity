/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/VirtIO/VirtIO.h>
#include <Kernel/Bus/VirtIO/VirtIOConsole.h>
#include <Kernel/Bus/VirtIO/VirtIORNG.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Sections.h>

namespace Kernel::VirtIO {

UNMAP_AFTER_INIT void detect()
{
    if (kernel_command_line().disable_virtio())
        return;
    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        if (address.is_null() || id.is_null())
            return;
        // TODO: We should also be checking that the device_id is in between 0x1000 - 0x107F inclusive
        if (id.vendor_id != PCI::VendorID::VirtIO)
            return;
        switch (id.device_id) {
        case PCI::DeviceID::VirtIOConsole: {
            [[maybe_unused]] auto& unused = adopt_ref(*new Console(address)).leak_ref();
            break;
        }
        case PCI::DeviceID::VirtIOEntropy: {
            [[maybe_unused]] auto& unused = adopt_ref(*new RNG(address)).leak_ref();
            break;
        }
        case PCI::DeviceID::VirtIOGPU: {
            // This should have been initialized by the graphics subsystem
            break;
        }
        default:
            dbgln_if(VIRTIO_DEBUG, "VirtIO: Unknown VirtIO device with ID: {}", id.device_id);
            break;
        }
    });
}

StringView determine_device_class(const PCI::Address& address)
{
    auto subsystem_device_id = PCI::get_subsystem_id(address);
    switch (subsystem_device_id) {
    case 1:
        return "VirtIONetAdapter";
    case 2:
        return "VirtIOBlockDevice";
    case 3:
        return "VirtIOConsole";
    case 4:
        return "VirtIORNG";
    }
    dbgln("VirtIO: Unknown subsystem_device_id {}", subsystem_device_id);
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT VirtIO::Device::Device(PCI::Address address)
    : PCI::Device(address)
    , IRQHandler(PCI::get_interrupt_line(address))
    , m_io_base(IOAddress(PCI::get_BAR0(pci_address()) & ~1))
{
    dbgln("{}: Found @ {}", VirtIO::determine_device_class(address), pci_address());

    enable_bus_mastering(pci_address());
    PCI::enable_interrupt_line(pci_address());
    enable_irq();

    auto capabilities = PCI::get_physical_id(address).capabilities();
    for (auto& capability : capabilities) {
        if (capability.id() == PCI_CAPABILITY_VENDOR_SPECIFIC) {
            // We have a virtio_pci_cap
            auto cfg = make<Configuration>();
            auto raw_config_type = capability.read8(0x3);
            if (raw_config_type < static_cast<u8>(ConfigurationType::Common) || raw_config_type > static_cast<u8>(ConfigurationType::PCI)) {
                dbgln("{}: Unknown capability configuration type: {}", VirtIO::determine_device_class(address), raw_config_type);
                return;
            }
            cfg->cfg_type = static_cast<ConfigurationType>(raw_config_type);
            auto cap_length = capability.read8(0x2);
            if (cap_length < 0x10) {
                dbgln("{}: Unexpected capability size: {}", VirtIO::determine_device_class(address), cap_length);
                break;
            }
            cfg->bar = capability.read8(0x4);
            if (cfg->bar > 0x5) {
                dbgln("{}: Unexpected capability bar value: {}", VirtIO::determine_device_class(address), cfg->bar);
                break;
            }
            cfg->offset = capability.read32(0x8);
            cfg->length = capability.read32(0xc);
            dbgln_if(VIRTIO_DEBUG, "{}: Found configuration {}, bar: {}, offset: {}, length: {}", VirtIO::determine_device_class(address), (u32)cfg->cfg_type, cfg->bar, cfg->offset, cfg->length);
            if (cfg->cfg_type == ConfigurationType::Common)
                m_use_mmio = true;
            else if (cfg->cfg_type == ConfigurationType::Notify)
                m_notify_multiplier = capability.read32(0x10);

            m_configs.append(move(cfg));
        }
    }

    if (m_use_mmio) {
        m_common_cfg = get_config(ConfigurationType::Common, 0);
        m_notify_cfg = get_config(ConfigurationType::Notify, 0);
        m_isr_cfg = get_config(ConfigurationType::ISR, 0);
    }

    reset_device();
    set_status_bit(DEVICE_STATUS_ACKNOWLEDGE);

    set_status_bit(DEVICE_STATUS_DRIVER);
}

Device::~Device()
{
}

auto Device::mapping_for_bar(u8 bar) -> MappedMMIO&
{
    VERIFY(m_use_mmio);
    auto& mapping = m_mmio[bar];
    if (!mapping.base) {
        mapping.size = PCI::get_BAR_space_size(pci_address(), bar);
        mapping.base = MM.allocate_kernel_region(PhysicalAddress(page_base_of(PCI::get_BAR(pci_address(), bar))), Memory::page_round_up(mapping.size), "VirtIO MMIO", Memory::Region::Access::ReadWrite, Memory::Region::Cacheable::No);
        if (!mapping.base)
            dbgln("{}: Failed to map bar {}", VirtIO::determine_device_class(pci_address()), bar);
    }
    return mapping;
}

void Device::notify_queue(u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "{}: notifying about queue change at idx: {}", VirtIO::determine_device_class(pci_address()), queue_index);
    if (!m_notify_cfg)
        out<u16>(REG_QUEUE_NOTIFY, queue_index);
    else
        config_write16(*m_notify_cfg, get_queue(queue_index).notify_offset() * m_notify_multiplier, queue_index);
}

u8 Device::config_read8(const Configuration& config, u32 offset)
{
    return mapping_for_bar(config.bar).read<u8>(config.offset + offset);
}

u16 Device::config_read16(const Configuration& config, u32 offset)
{
    return mapping_for_bar(config.bar).read<u16>(config.offset + offset);
}

u32 Device::config_read32(const Configuration& config, u32 offset)
{
    return mapping_for_bar(config.bar).read<u32>(config.offset + offset);
}

void Device::config_write8(const Configuration& config, u32 offset, u8 value)
{
    mapping_for_bar(config.bar).write(config.offset + offset, value);
}

void Device::config_write16(const Configuration& config, u32 offset, u16 value)
{
    mapping_for_bar(config.bar).write(config.offset + offset, value);
}

void Device::config_write32(const Configuration& config, u32 offset, u32 value)
{
    mapping_for_bar(config.bar).write(config.offset + offset, value);
}

void Device::config_write64(const Configuration& config, u32 offset, u64 value)
{
    mapping_for_bar(config.bar).write(config.offset + offset, value);
}

u8 Device::read_status_bits()
{
    if (!m_common_cfg)
        return in<u8>(REG_DEVICE_STATUS);
    return config_read8(*m_common_cfg, COMMON_CFG_DEVICE_STATUS);
}

void Device::mask_status_bits(u8 status_mask)
{
    m_status &= status_mask;
    if (!m_common_cfg)
        out<u8>(REG_DEVICE_STATUS, m_status);
    else
        config_write8(*m_common_cfg, COMMON_CFG_DEVICE_STATUS, m_status);
}

void Device::set_status_bit(u8 status_bit)
{
    m_status |= status_bit;
    if (!m_common_cfg)
        out<u8>(REG_DEVICE_STATUS, m_status);
    else
        config_write8(*m_common_cfg, COMMON_CFG_DEVICE_STATUS, m_status);
}

u64 Device::get_device_features()
{
    if (!m_common_cfg)
        return in<u32>(REG_DEVICE_FEATURES);
    config_write32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE_SELECT, 0);
    auto lower_bits = config_read32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE);
    config_write32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE_SELECT, 1);
    u64 upper_bits = (u64)config_read32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE) << 32;
    return upper_bits | lower_bits;
}

bool Device::accept_device_features(u64 device_features, u64 accepted_features)
{
    VERIFY(!m_did_accept_features);
    m_did_accept_features = true;

    if (is_feature_set(device_features, VIRTIO_F_VERSION_1)) {
        accepted_features |= VIRTIO_F_VERSION_1; // let the device know were not a legacy driver
    }

    if (is_feature_set(device_features, VIRTIO_F_RING_PACKED)) {
        dbgln_if(VIRTIO_DEBUG, "{}: packed queues not yet supported", VirtIO::determine_device_class(pci_address()));
        accepted_features &= ~(VIRTIO_F_RING_PACKED);
    }

    // TODO: implement indirect descriptors to allow queue_size buffers instead of buffers totalling (PAGE_SIZE * queue_size) bytes
    if (is_feature_set(device_features, VIRTIO_F_INDIRECT_DESC)) {
        // accepted_features |= VIRTIO_F_INDIRECT_DESC;
    }

    if (is_feature_set(device_features, VIRTIO_F_IN_ORDER)) {
        accepted_features |= VIRTIO_F_IN_ORDER;
    }

    dbgln_if(VIRTIO_DEBUG, "{}: Device features: {}", VirtIO::determine_device_class(pci_address()), device_features);
    dbgln_if(VIRTIO_DEBUG, "{}: Accepted features: {}", VirtIO::determine_device_class(pci_address()), accepted_features);

    if (!m_common_cfg) {
        out<u32>(REG_GUEST_FEATURES, accepted_features);
    } else {
        config_write32(*m_common_cfg, COMMON_CFG_DRIVER_FEATURE_SELECT, 0);
        config_write32(*m_common_cfg, COMMON_CFG_DRIVER_FEATURE, accepted_features);
        config_write32(*m_common_cfg, COMMON_CFG_DRIVER_FEATURE_SELECT, 1);
        config_write32(*m_common_cfg, COMMON_CFG_DRIVER_FEATURE, accepted_features >> 32);
    }
    set_status_bit(DEVICE_STATUS_FEATURES_OK);
    m_status = read_status_bits();
    if (!(m_status & DEVICE_STATUS_FEATURES_OK)) {
        set_status_bit(DEVICE_STATUS_FAILED);
        dbgln("{}: Features not accepted by host!", VirtIO::determine_device_class(pci_address()));
        return false;
    }

    m_accepted_features = accepted_features;
    dbgln_if(VIRTIO_DEBUG, "{}: Features accepted by host", VirtIO::determine_device_class(pci_address()));
    return true;
}

void Device::reset_device()
{
    dbgln_if(VIRTIO_DEBUG, "{}: Reset device", VirtIO::determine_device_class(pci_address()));
    if (!m_common_cfg) {
        mask_status_bits(0);
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

bool Device::setup_queue(u16 queue_index)
{
    if (!m_common_cfg)
        return false;

    config_write16(*m_common_cfg, COMMON_CFG_QUEUE_SELECT, queue_index);
    u16 queue_size = config_read16(*m_common_cfg, COMMON_CFG_QUEUE_SIZE);
    if (queue_size == 0) {
        dbgln_if(VIRTIO_DEBUG, "{}: Queue[{}] is unavailable!", VirtIO::determine_device_class(pci_address()), queue_index);
        return true;
    }

    u16 queue_notify_offset = config_read16(*m_common_cfg, COMMON_CFG_QUEUE_NOTIFY_OFF);

    auto queue = make<Queue>(queue_size, queue_notify_offset);
    if (queue->is_null())
        return false;

    config_write64(*m_common_cfg, COMMON_CFG_QUEUE_DESC, queue->descriptor_area().get());
    config_write64(*m_common_cfg, COMMON_CFG_QUEUE_DRIVER, queue->driver_area().get());
    config_write64(*m_common_cfg, COMMON_CFG_QUEUE_DEVICE, queue->device_area().get());

    dbgln_if(VIRTIO_DEBUG, "{}: Queue[{}] configured with size: {}", VirtIO::determine_device_class(pci_address()), queue_index, queue_size);

    m_queues.append(move(queue));
    return true;
}

bool Device::activate_queue(u16 queue_index)
{
    if (!m_common_cfg)
        return false;

    config_write16(*m_common_cfg, COMMON_CFG_QUEUE_SELECT, queue_index);
    config_write16(*m_common_cfg, COMMON_CFG_QUEUE_ENABLE, true);

    dbgln_if(VIRTIO_DEBUG, "{}: Queue[{}] activated", VirtIO::determine_device_class(pci_address()), queue_index);
    return true;
}

bool Device::setup_queues(u16 requested_queue_count)
{
    VERIFY(!m_did_setup_queues);
    m_did_setup_queues = true;

    if (m_common_cfg) {
        auto maximum_queue_count = config_read16(*m_common_cfg, COMMON_CFG_NUM_QUEUES);
        if (requested_queue_count == 0) {
            m_queue_count = maximum_queue_count;
        } else if (requested_queue_count > maximum_queue_count) {
            dbgln("{}: {} queues requested but only {} available!", VirtIO::determine_device_class(pci_address()), m_queue_count, maximum_queue_count);
            return false;
        } else {
            m_queue_count = requested_queue_count;
        }
    } else {
        m_queue_count = requested_queue_count;
        dbgln("{}: device's available queue count could not be determined!", VirtIO::determine_device_class(pci_address()));
    }

    dbgln_if(VIRTIO_DEBUG, "{}: Setting up {} queues", VirtIO::determine_device_class(pci_address()), m_queue_count);
    for (u16 i = 0; i < m_queue_count; i++) {
        if (!setup_queue(i))
            return false;
    }
    for (u16 i = 0; i < m_queue_count; i++) { // Queues can only be activated *after* all others queues were also configured
        if (!activate_queue(i))
            return false;
    }
    return true;
}

void Device::finish_init()
{
    VERIFY(m_did_accept_features);                 // ensure features were negotiated
    VERIFY(m_did_setup_queues);                    // ensure queues were set-up
    VERIFY(!(m_status & DEVICE_STATUS_DRIVER_OK)); // ensure we didn't already finish the initialization

    set_status_bit(DEVICE_STATUS_DRIVER_OK);
    dbgln_if(VIRTIO_DEBUG, "{}: Finished initialization", VirtIO::determine_device_class(pci_address()));
}

u8 Device::isr_status()
{
    if (!m_isr_cfg)
        return in<u8>(REG_ISR_STATUS);
    return config_read8(*m_isr_cfg, 0);
}

bool Device::handle_irq(const RegisterState&)
{
    u8 isr_type = isr_status();
    if ((isr_type & (QUEUE_INTERRUPT | DEVICE_CONFIG_INTERRUPT)) == 0) {
        dbgln_if(VIRTIO_DEBUG, "{}: Handling interrupt with unknown type: {}", VirtIO::determine_device_class(pci_address()), isr_type);
        return false;
    }
    if (isr_type & DEVICE_CONFIG_INTERRUPT) {
        dbgln_if(VIRTIO_DEBUG, "{}: VirtIO Device config interrupt!", VirtIO::determine_device_class(pci_address()));
        if (!handle_device_config_change()) {
            set_status_bit(DEVICE_STATUS_FAILED);
            dbgln("{}: Failed to handle device config change!", VirtIO::determine_device_class(pci_address()));
        }
    }
    if (isr_type & QUEUE_INTERRUPT) {
        dbgln_if(VIRTIO_DEBUG, "{}: VirtIO Queue interrupt!", VirtIO::determine_device_class(pci_address()));
        for (size_t i = 0; i < m_queues.size(); i++) {
            if (get_queue(i).new_data_available()) {
                handle_queue_update(i);
                return true;
            }
        }
        dbgln_if(VIRTIO_DEBUG, "{}: Got queue interrupt but all queues are up to date!", VirtIO::determine_device_class(pci_address()));
    }
    return true;
}

void Device::supply_chain_and_notify(u16 queue_index, QueueChain& chain)
{
    auto& queue = get_queue(queue_index);
    VERIFY(&chain.queue() == &queue);
    VERIFY(queue.lock().is_locked());
    chain.submit_to_queue();
    if (queue.should_notify())
        notify_queue(queue_index);
}

}
