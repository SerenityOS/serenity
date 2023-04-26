/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/VirtIO/Console.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Bus/VirtIO/RNG.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Sections.h>

namespace Kernel::VirtIO {

UNMAP_AFTER_INIT void detect()
{
    if (kernel_command_line().disable_virtio())
        return;
    MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
        if (device_identifier.hardware_id().is_null())
            return;
        // TODO: We should also be checking that the device_id is in between 0x1000 - 0x107F inclusive
        if (device_identifier.hardware_id().vendor_id != PCI::VendorID::VirtIO)
            return;
        switch (device_identifier.hardware_id().device_id) {
        case PCI::DeviceID::VirtIOConsole: {
            auto& console = Console::must_create(device_identifier).leak_ref();
            MUST(console.initialize_virtio_resources());
            break;
        }
        case PCI::DeviceID::VirtIOEntropy: {
            auto& rng = RNG::must_create(device_identifier).leak_ref();
            MUST(rng.initialize_virtio_resources());
            break;
        }
        case PCI::DeviceID::VirtIOGPU: {
            // This should have been initialized by the graphics subsystem
            break;
        }
        default:
            dbgln_if(VIRTIO_DEBUG, "VirtIO: Unknown VirtIO device with ID: {}", device_identifier.hardware_id().device_id);
            break;
        }
    }));
}

static StringView determine_device_class(PCI::DeviceIdentifier const& device_identifier)
{
    if (device_identifier.revision_id().value() == 0) {
        // Note: If the device is a legacy (or transitional) device, therefore,
        // probe the subsystem ID in the PCI header and figure out the
        auto subsystem_device_id = device_identifier.subsystem_id().value();
        switch (subsystem_device_id) {
        case 1:
            return "VirtIONetAdapter"sv;
        case 2:
            return "VirtIOBlockDevice"sv;
        case 3:
            return "VirtIOConsole"sv;
        case 4:
            return "VirtIORNG"sv;
        default:
            dbgln("VirtIO: Unknown subsystem_device_id {}", subsystem_device_id);
            VERIFY_NOT_REACHED();
        }
    }

    auto id = device_identifier.hardware_id();
    VERIFY(id.vendor_id == PCI::VendorID::VirtIO);
    switch (id.device_id) {
    case PCI::DeviceID::VirtIONetAdapter:
        return "VirtIONetAdapter"sv;
    case PCI::DeviceID::VirtIOBlockDevice:
        return "VirtIOBlockDevice"sv;
    case PCI::DeviceID::VirtIOConsole:
        return "VirtIOConsole"sv;
    case PCI::DeviceID::VirtIOEntropy:
        return "VirtIORNG"sv;
    case PCI::DeviceID::VirtIOGPU:
        return "VirtIOGPU"sv;
    default:
        dbgln("VirtIO: Unknown device_id {}", id.vendor_id);
        VERIFY_NOT_REACHED();
    }
}

UNMAP_AFTER_INIT ErrorOr<void> Device::initialize_virtio_resources()
{
    enable_bus_mastering(device_identifier());

    auto capabilities = device_identifier().capabilities();
    for (auto& capability : capabilities) {
        if (capability.id().value() == PCI::Capabilities::ID::VendorSpecific) {
            // We have a virtio_pci_cap
            Configuration config {};
            auto raw_config_type = capability.read8(0x3);
            // NOTE: The VirtIO specification allows iteration of configurations
            // through a special PCI capbility structure with the VIRTIO_PCI_CAP_PCI_CFG tag:
            //
            // "Each structure can be mapped by a Base Address register (BAR) belonging to the function, or accessed via
            // the special VIRTIO_PCI_CAP_PCI_CFG field in the PCI configuration space"
            //
            // "The VIRTIO_PCI_CAP_PCI_CFG capability creates an alternative (and likely suboptimal) access method
            // to the common configuration, notification, ISR and device-specific configuration regions."
            //
            // Also, it is *very* likely to see this PCI capability as the first vendor-specific capbility of a certain PCI function,
            // but this is not guaranteed by the VirtIO specification.
            // Therefore, ignore this type of configuration as this is not needed by our implementation currently.
            if (raw_config_type == static_cast<u8>(ConfigurationType::PCICapabilitiesAccess))
                continue;
            if (raw_config_type < static_cast<u8>(ConfigurationType::Common) || raw_config_type > static_cast<u8>(ConfigurationType::PCICapabilitiesAccess)) {
                dbgln("{}: Unknown capability configuration type: {}", m_class_name, raw_config_type);
                return Error::from_errno(ENXIO);
            }
            config.cfg_type = static_cast<ConfigurationType>(raw_config_type);
            auto cap_length = capability.read8(0x2);
            if (cap_length < 0x10) {
                dbgln("{}: Unexpected capability size: {}", m_class_name, cap_length);
                break;
            }
            config.bar = capability.read8(0x4);
            if (config.bar > 0x5) {
                dbgln("{}: Unexpected capability bar value: {}", m_class_name, config.bar);
                break;
            }
            config.offset = capability.read32(0x8);
            config.length = capability.read32(0xc);
            // NOTE: Configuration length of zero is an invalid configuration, or at the very least a configuration
            // type we don't know how to handle correctly...
            // The VIRTIO_PCI_CAP_PCI_CFG configuration structure has length of 0
            // but because we ignore that type and all other types should have a length
            // greater than 0, we should ignore any other configuration in case this condition is not met.
            if (config.length == 0) {
                dbgln("{}: Found configuration {}, with invalid length of 0", m_class_name, (u32)config.cfg_type);
                continue;
            }
            dbgln_if(VIRTIO_DEBUG, "{}: Found configuration {}, bar: {}, offset: {}, length: {}", m_class_name, (u32)config.cfg_type, config.bar, config.offset, config.length);
            if (config.cfg_type == ConfigurationType::Common)
                m_use_mmio = true;
            else if (config.cfg_type == ConfigurationType::Notify)
                m_notify_multiplier = capability.read32(0x10);

            m_configs.append(config);
        }
    }

    if (m_use_mmio) {
        for (auto& cfg : m_configs) {
            auto mapping_io_window = TRY(IOWindow::create_for_pci_device_bar(device_identifier(), static_cast<PCI::HeaderType0BaseRegister>(cfg.bar)));
            m_register_bases[cfg.bar] = move(mapping_io_window);
        }
        m_common_cfg = get_config(ConfigurationType::Common, 0);
        m_notify_cfg = get_config(ConfigurationType::Notify, 0);
        m_isr_cfg = get_config(ConfigurationType::ISR, 0);
    } else {
        auto mapping_io_window = TRY(IOWindow::create_for_pci_device_bar(device_identifier(), PCI::HeaderType0BaseRegister::BAR0));
        m_register_bases[0] = move(mapping_io_window);
    }

    // Note: We enable interrupts at least after the m_register_bases[0] ptr is
    // assigned with an IOWindow, to ensure that in case of getting an interrupt
    // we can access registers from that IO window range.
    PCI::enable_interrupt_line(device_identifier());
    enable_irq();

    reset_device();
    set_status_bit(DEVICE_STATUS_ACKNOWLEDGE);

    set_status_bit(DEVICE_STATUS_DRIVER);
    return {};
}

UNMAP_AFTER_INIT VirtIO::Device::Device(PCI::DeviceIdentifier const& device_identifier)
    : PCI::Device(const_cast<PCI::DeviceIdentifier&>(device_identifier))
    , IRQHandler(device_identifier.interrupt_line().value())
    , m_class_name(VirtIO::determine_device_class(device_identifier))
{
    dbgln("{}: Found @ {}", m_class_name, device_identifier.address());
}

void Device::notify_queue(u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "{}: notifying about queue change at idx: {}", m_class_name, queue_index);
    if (!m_notify_cfg)
        base_io_window().write16(REG_QUEUE_NOTIFY, queue_index);
    else
        config_write16(*m_notify_cfg, get_queue(queue_index).notify_offset() * m_notify_multiplier, queue_index);
}

auto Device::mapping_for_bar(u8 bar) -> IOWindow&
{
    VERIFY(m_use_mmio);
    VERIFY(m_register_bases[bar]);
    return *m_register_bases[bar];
}

u8 Device::config_read8(Configuration const& config, u32 offset)
{
    return mapping_for_bar(config.bar).read8(config.offset + offset);
}

u16 Device::config_read16(Configuration const& config, u32 offset)
{
    return mapping_for_bar(config.bar).read16(config.offset + offset);
}

u32 Device::config_read32(Configuration const& config, u32 offset)
{
    return mapping_for_bar(config.bar).read32(config.offset + offset);
}

void Device::config_write8(Configuration const& config, u32 offset, u8 value)
{
    mapping_for_bar(config.bar).write8(config.offset + offset, value);
}

void Device::config_write16(Configuration const& config, u32 offset, u16 value)
{
    mapping_for_bar(config.bar).write16(config.offset + offset, value);
}

void Device::config_write32(Configuration const& config, u32 offset, u32 value)
{
    mapping_for_bar(config.bar).write32(config.offset + offset, value);
}

void Device::config_write64(Configuration const& config, u32 offset, u64 value)
{
    mapping_for_bar(config.bar).write32(config.offset + offset, (u32)(value & 0xFFFFFFFF));
    mapping_for_bar(config.bar).write32(config.offset + offset + 4, (u32)(value >> 32));
}

u8 Device::read_status_bits()
{
    if (!m_common_cfg)
        return base_io_window().read8(REG_DEVICE_STATUS);
    return config_read8(*m_common_cfg, COMMON_CFG_DEVICE_STATUS);
}

void Device::mask_status_bits(u8 status_mask)
{
    m_status &= status_mask;
    if (!m_common_cfg)
        base_io_window().write8(REG_DEVICE_STATUS, m_status);
    else
        config_write8(*m_common_cfg, COMMON_CFG_DEVICE_STATUS, m_status);
}

void Device::set_status_bit(u8 status_bit)
{
    m_status |= status_bit;
    if (!m_common_cfg)
        base_io_window().write8(REG_DEVICE_STATUS, m_status);
    else
        config_write8(*m_common_cfg, COMMON_CFG_DEVICE_STATUS, m_status);
}

u64 Device::get_device_features()
{
    if (!m_common_cfg)
        return base_io_window().read32(REG_DEVICE_FEATURES);
    config_write32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE_SELECT, 0);
    auto lower_bits = config_read32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE);
    config_write32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE_SELECT, 1);
    u64 upper_bits = (u64)config_read32(*m_common_cfg, COMMON_CFG_DEVICE_FEATURE) << 32;
    return upper_bits | lower_bits;
}

IOWindow& Device::base_io_window()
{
    VERIFY(m_register_bases[0]);
    return *m_register_bases[0];
}

bool Device::accept_device_features(u64 device_features, u64 accepted_features)
{
    VERIFY(!m_did_accept_features);
    m_did_accept_features = true;

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

    if (!m_common_cfg) {
        base_io_window().write32(REG_GUEST_FEATURES, accepted_features);
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
        dbgln("{}: Features not accepted by host!", m_class_name);
        return false;
    }

    m_accepted_features = accepted_features;
    dbgln_if(VIRTIO_DEBUG, "{}: Features accepted by host", m_class_name);
    return true;
}

void Device::reset_device()
{
    dbgln_if(VIRTIO_DEBUG, "{}: Reset device", m_class_name);
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
        dbgln_if(VIRTIO_DEBUG, "{}: Queue[{}] is unavailable!", m_class_name, queue_index);
        return true;
    }

    u16 queue_notify_offset = config_read16(*m_common_cfg, COMMON_CFG_QUEUE_NOTIFY_OFF);

    auto queue_or_error = Queue::try_create(queue_size, queue_notify_offset);
    if (queue_or_error.is_error())
        return false;
    auto queue = queue_or_error.release_value();

    config_write64(*m_common_cfg, COMMON_CFG_QUEUE_DESC, queue->descriptor_area().get());
    config_write64(*m_common_cfg, COMMON_CFG_QUEUE_DRIVER, queue->driver_area().get());
    config_write64(*m_common_cfg, COMMON_CFG_QUEUE_DEVICE, queue->device_area().get());

    dbgln_if(VIRTIO_DEBUG, "{}: Queue[{}] configured with size: {}", m_class_name, queue_index, queue_size);

    m_queues.append(move(queue));
    return true;
}

bool Device::activate_queue(u16 queue_index)
{
    if (!m_common_cfg)
        return false;

    config_write16(*m_common_cfg, COMMON_CFG_QUEUE_SELECT, queue_index);
    config_write16(*m_common_cfg, COMMON_CFG_QUEUE_ENABLE, true);

    dbgln_if(VIRTIO_DEBUG, "{}: Queue[{}] activated", m_class_name, queue_index);
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
            dbgln("{}: {} queues requested but only {} available!", m_class_name, m_queue_count, maximum_queue_count);
            return false;
        } else {
            m_queue_count = requested_queue_count;
        }
    } else {
        m_queue_count = requested_queue_count;
        dbgln("{}: device's available queue count could not be determined!", m_class_name);
    }

    dbgln_if(VIRTIO_DEBUG, "{}: Setting up {} queues", m_class_name, m_queue_count);
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
    dbgln_if(VIRTIO_DEBUG, "{}: Finished initialization", m_class_name);
}

u8 Device::isr_status()
{
    if (!m_isr_cfg)
        return base_io_window().read8(REG_ISR_STATUS);
    return config_read8(*m_isr_cfg, 0);
}

bool Device::handle_irq(RegisterState const&)
{
    u8 isr_type = isr_status();
    if ((isr_type & (QUEUE_INTERRUPT | DEVICE_CONFIG_INTERRUPT)) == 0) {
        dbgln_if(VIRTIO_DEBUG, "{}: Handling interrupt with unknown type: {}", class_name(), isr_type);
        return false;
    }
    if (isr_type & DEVICE_CONFIG_INTERRUPT) {
        dbgln_if(VIRTIO_DEBUG, "{}: VirtIO Device config interrupt!", class_name());
        if (!handle_device_config_change()) {
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
    if (queue.should_notify())
        notify_queue(queue_index);
}

}
