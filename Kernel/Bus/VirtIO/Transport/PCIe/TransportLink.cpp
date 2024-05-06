/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/InterruptHandler.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>

namespace Kernel::VirtIO {

ErrorOr<NonnullOwnPtr<TransportEntity>> PCIeTransportLink::create(PCI::DeviceIdentifier const& pci_identifier)
{
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) PCIeTransportLink(pci_identifier)));
}

StringView PCIeTransportLink::determine_device_class_name() const
{
    if (device_identifier().revision_id().value() == 0) {
        // Note: If the device is a legacy (or transitional) device, therefore,
        // probe the subsystem ID in the PCI header and figure out the
        auto subsystem_device_id = device_identifier().subsystem_id().value();
        switch (subsystem_device_id) {
        case 1:
            return "VirtIONetAdapter"sv;
        case 2:
            return "VirtIOBlockDevice"sv;
        case 3:
            return "VirtIOConsole"sv;
        case 4:
            return "VirtIORNG"sv;
        case 18:
            return "VirtIOInput"sv;
        default:
            dbgln("VirtIO: Unknown subsystem_device_id {}", subsystem_device_id);
            VERIFY_NOT_REACHED();
        }
    }

    auto id = device_identifier().hardware_id();
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
    case PCI::DeviceID::VirtIOInput:
        return "VirtIOInput"sv;
    default:
        dbgln("VirtIO: Unknown device_id {:#x}", id.device_id);
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<void> PCIeTransportLink::create_interrupt_handler(VirtIO::Device& parent_device)
{
    TRY(reserve_irqs(1, false));
    auto irq = MUST(allocate_irq(0));
    m_irq_handler = TRY(PCIeTransportInterruptHandler::create(*this, parent_device, irq));
    return {};
}

PCIeTransportLink::PCIeTransportLink(PCI::DeviceIdentifier const& pci_identifier)
    : PCI::Device(pci_identifier)
{
    dbgln("{}: Found @ {}", determine_device_class_name(), device_identifier().address());
}

ErrorOr<void> PCIeTransportLink::locate_configurations_and_resources(Badge<VirtIO::Device>, VirtIO::Device& parent_device)
{
    TRY(create_interrupt_handler(parent_device));
    PCI::enable_bus_mastering(device_identifier());

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
                dbgln("{}: Unknown capability configuration type: {}", device_name(), raw_config_type);
                return Error::from_errno(ENXIO);
            }
            config.cfg_type = static_cast<ConfigurationType>(raw_config_type);
            auto cap_length = capability.read8(0x2);
            if (cap_length < 0x10) {
                dbgln("{}: Unexpected capability size: {}", device_name(), cap_length);
                break;
            }
            config.resource_index = capability.read8(0x4);
            if (config.resource_index > 0x5) {
                dbgln("{}: Unexpected capability BAR value: {}", device_name(), config.resource_index);
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
                dbgln("{}: Found configuration {}, with invalid length of 0", device_name(), (u32)config.cfg_type);
                continue;
            }
            dbgln_if(VIRTIO_DEBUG, "{}: Found configuration {}, resource: {}, offset: {}, length: {}", device_name(), (u32)config.cfg_type, config.resource_index, config.offset, config.length);
            if (config.cfg_type == ConfigurationType::Common)
                m_use_mmio.set();
            else if (config.cfg_type == ConfigurationType::Notify)
                m_notify_multiplier = capability.read32(0x10);

            m_configs.append(config);
        }
    }

    if (m_use_mmio.was_set()) {
        for (auto& cfg : m_configs) {
            auto mapping_io_window = TRY(IOWindow::create_for_pci_device_bar(device_identifier(), static_cast<PCI::HeaderType0BaseRegister>(cfg.resource_index)));
            m_register_bases[cfg.resource_index] = move(mapping_io_window);
        }
        m_common_cfg = TRY(get_config(ConfigurationType::Common, 0));
        m_notify_cfg = TRY(get_config(ConfigurationType::Notify, 0));
        m_isr_cfg = TRY(get_config(ConfigurationType::ISR, 0));
    } else {
        auto mapping_io_window = TRY(IOWindow::create_for_pci_device_bar(device_identifier(), PCI::HeaderType0BaseRegister::BAR0));
        m_register_bases[0] = move(mapping_io_window);
    }
    return {};
}

void PCIeTransportLink::disable_interrupts(Badge<VirtIO::Device>)
{
    disable_pin_based_interrupts();
    m_irq_handler->disable_irq();
}

void PCIeTransportLink::enable_interrupts(Badge<VirtIO::Device>)
{
    m_irq_handler->enable_irq();
    enable_pin_based_interrupts();
}

}
