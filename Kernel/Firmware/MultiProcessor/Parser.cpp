/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Debug.h>
#include <Kernel/Firmware/BIOS.h>
#include <Kernel/Firmware/MultiProcessor/Parser.h>
#include <Kernel/Interrupts/IOAPIC.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>
#include <Kernel/StdLib.h>

namespace Kernel {

UNMAP_AFTER_INIT OwnPtr<MultiProcessorParser> MultiProcessorParser::autodetect()
{
    auto floating_pointer = find_floating_pointer();
    if (!floating_pointer.has_value())
        return {};
    auto parser = adopt_own_if_nonnull(new (nothrow) MultiProcessorParser(floating_pointer.value()));
    VERIFY(parser != nullptr);
    return parser;
}

UNMAP_AFTER_INIT MultiProcessorParser::MultiProcessorParser(PhysicalAddress floating_pointer)
    : m_floating_pointer(floating_pointer)
{
    dbgln("MultiProcessor: Floating Pointer Structure @ {}", m_floating_pointer);
    parse_floating_pointer_data();
    parse_configuration_table();
}

UNMAP_AFTER_INIT void MultiProcessorParser::parse_floating_pointer_data()
{
    auto floating_pointer = Memory::map_typed<MultiProcessor::FloatingPointer>(m_floating_pointer).release_value_but_fixme_should_propagate_errors();
    m_configuration_table = PhysicalAddress(floating_pointer->physical_address_ptr);
    dbgln("Features {}, IMCR? {}", floating_pointer->feature_info[0], (floating_pointer->feature_info[0] & (1 << 7)));
}

UNMAP_AFTER_INIT void MultiProcessorParser::parse_configuration_table()
{
    auto configuration_table_length = Memory::map_typed<MultiProcessor::ConfigurationTableHeader>(m_configuration_table).release_value_but_fixme_should_propagate_errors()->length;
    auto config_table = Memory::map_typed<MultiProcessor::ConfigurationTableHeader>(m_configuration_table, configuration_table_length).release_value_but_fixme_should_propagate_errors();

    size_t entry_count = config_table->entry_count;
    auto* entry = config_table->entries;
    while (entry_count > 0) {
        dbgln_if(MULTIPROCESSOR_DEBUG, "MultiProcessor: Entry Type {} detected.", entry->entry_type);
        switch (entry->entry_type) {
        case ((u8)MultiProcessor::ConfigurationTableEntryType::Processor):
            entry = (MultiProcessor::EntryHeader*)(FlatPtr)entry + sizeof(MultiProcessor::ProcessorEntry);
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::Bus):
            MUST(m_bus_entries.try_append(*(const MultiProcessor::BusEntry*)entry));
            entry = (MultiProcessor::EntryHeader*)(FlatPtr)entry + sizeof(MultiProcessor::BusEntry);
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::IOAPIC):
            entry = (MultiProcessor::EntryHeader*)(FlatPtr)entry + sizeof(MultiProcessor::IOAPICEntry);
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::IO_Interrupt_Assignment):
            MUST(m_io_interrupt_assignment_entries.try_append(*(const MultiProcessor::IOInterruptAssignmentEntry*)entry));
            entry = (MultiProcessor::EntryHeader*)(FlatPtr)entry + sizeof(MultiProcessor::IOInterruptAssignmentEntry);
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::Local_Interrupt_Assignment):
            entry = (MultiProcessor::EntryHeader*)(FlatPtr)entry + sizeof(MultiProcessor::LocalInterruptAssignmentEntry);
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::SystemAddressSpaceMapping):
            entry = (MultiProcessor::EntryHeader*)(FlatPtr)entry + sizeof(MultiProcessor::SystemAddressSpaceMappingEntry);
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::BusHierarchyDescriptor):
            entry = (MultiProcessor::EntryHeader*)(FlatPtr)entry + sizeof(MultiProcessor::BusHierarchyDescriptorEntry);
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::CompatibilityBusAddressSpaceModifier):
            entry = (MultiProcessor::EntryHeader*)(FlatPtr)entry + sizeof(MultiProcessor::CompatibilityBusAddressSpaceModifierEntry);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        --entry_count;
    }
}

UNMAP_AFTER_INIT Optional<PhysicalAddress> MultiProcessorParser::find_floating_pointer()
{
    constexpr auto signature = "_MP_"sv;
    auto ebda_or_error = map_ebda();
    if (!ebda_or_error.is_error()) {
        auto mp_floating_pointer = ebda_or_error.value().find_chunk_starting_with(signature, 16);
        if (mp_floating_pointer.has_value())
            return mp_floating_pointer;
    }
    auto bios_or_error = map_bios();
    if (bios_or_error.is_error())
        return {};
    return bios_or_error.value().find_chunk_starting_with(signature, 16);
}

UNMAP_AFTER_INIT Vector<u8> MultiProcessorParser::get_pci_bus_ids() const
{
    Vector<u8> pci_bus_ids;
    for (auto& entry : m_bus_entries) {
        if (!strncmp("PCI   ", entry.bus_type, strlen("PCI   ")))
            pci_bus_ids.append(entry.bus_id);
    }
    return pci_bus_ids;
}

UNMAP_AFTER_INIT Vector<PCIInterruptOverrideMetadata> MultiProcessorParser::get_pci_interrupt_redirections()
{
    dbgln("MultiProcessor: Get PCI IOAPIC redirections");
    Vector<PCIInterruptOverrideMetadata> overrides;
    auto pci_bus_ids = get_pci_bus_ids();
    for (auto& entry : m_io_interrupt_assignment_entries) {
        for (auto id : pci_bus_ids) {
            if (id == entry.source_bus_id) {

                dbgln("Interrupts: Bus {}, polarity {}, trigger mode {}, INT {}, IOAPIC {}, IOAPIC INTIN {}",
                    entry.source_bus_id,
                    entry.polarity,
                    entry.trigger_mode,
                    entry.source_bus_irq,
                    entry.destination_ioapic_id,
                    entry.destination_ioapic_intin_pin);
                MUST(overrides.try_empend(
                    entry.source_bus_id,
                    entry.polarity,
                    entry.trigger_mode,
                    entry.source_bus_irq,
                    entry.destination_ioapic_id,
                    entry.destination_ioapic_intin_pin));
            }
        }
    }

    for (auto& override_metadata : overrides) {
        dbgln("Interrupts: Bus {}, polarity {}, PCI device {}, trigger mode {}, INT {}, IOAPIC {}, IOAPIC INTIN {}",
            override_metadata.bus(),
            override_metadata.polarity(),
            override_metadata.pci_device_number(),
            override_metadata.trigger_mode(),
            override_metadata.pci_interrupt_pin(),
            override_metadata.ioapic_id(),
            override_metadata.ioapic_interrupt_pin());
    }
    return overrides;
}

UNMAP_AFTER_INIT PCIInterruptOverrideMetadata::PCIInterruptOverrideMetadata(u8 bus_id, u8 polarity, u8 trigger_mode, u8 source_irq, u32 ioapic_id, u16 ioapic_int_pin)
    : m_bus_id(bus_id)
    , m_polarity(polarity)
    , m_trigger_mode(trigger_mode)
    , m_pci_interrupt_pin(source_irq & 0b11)
    , m_pci_device_number((source_irq >> 2) & 0b11111)
    , m_ioapic_id(ioapic_id)
    , m_ioapic_interrupt_pin(ioapic_int_pin)
{
}

}
