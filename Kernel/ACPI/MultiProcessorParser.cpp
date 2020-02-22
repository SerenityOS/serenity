/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <Kernel/ACPI/MultiProcessorParser.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibBareMetal/StdLib.h>

namespace Kernel {

static MultiProcessorParser* s_parser;

bool MultiProcessorParser::is_initialized()
{
    return s_parser != nullptr;
}

void MultiProcessorParser::initialize()
{
    if (!MultiProcessorParser::is_initialized())
        s_parser = new MultiProcessorParser;
}

MultiProcessorParser::MultiProcessorParser()
    : m_floating_pointer(search_floating_pointer())
    , m_operable((m_floating_pointer != (uintptr_t) nullptr))
{
    if (m_floating_pointer != (uintptr_t) nullptr) {
        kprintf("MultiProcessor: Floating Pointer Structure @ P 0x%x\n", m_floating_pointer);
        parse_floating_pointer_data();
        parse_configuration_table();
    } else {
        kprintf("MultiProcessor: Can't Locate Floating Pointer Structure, disabled.\n");
    }
}

void MultiProcessorParser::parse_floating_pointer_data()
{
    auto floating_pointer_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)m_floating_pointer)), PAGE_SIZE * 2, "MultiProcessor Parser Parsing Floating Pointer Structure", Region::Access::Read, false, true);
    auto* floating_pointer = (MultiProcessor::FloatingPointer*)floating_pointer_region->vaddr().offset(offset_in_page((u32)m_floating_pointer)).as_ptr();
    m_configuration_table = floating_pointer->physical_address_ptr;
    m_specification_revision = floating_pointer->specification_revision;
}

size_t MultiProcessorParser::get_configuration_table_length()
{
    auto config_table_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)m_configuration_table)), PAGE_SIZE * 2, "MultiProcessor Parser Getting Configuration Table length", Region::Access::Read, false, true);
    auto* config_table = (MultiProcessor::ConfigurationTableHeader*)config_table_region->vaddr().offset(offset_in_page((u32)m_configuration_table)).as_ptr();
    return config_table->length;
}

void MultiProcessorParser::parse_configuration_table()
{
    m_configuration_table_length = get_configuration_table_length();
    auto config_table_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)m_configuration_table)), PAGE_ROUND_UP(m_configuration_table_length), "MultiProcessor Parser Parsing Configuration Table", Region::Access::Read, false, true);
    auto* config_table = (MultiProcessor::ConfigurationTableHeader*)config_table_region->vaddr().offset(offset_in_page((u32)m_configuration_table)).as_ptr();

    size_t entry_count = config_table->entry_count;
    auto* entry = config_table->entries;
    auto* p_entry = reinterpret_cast<MultiProcessor::ConfigurationTableHeader*>(m_configuration_table)->entries;
    while (entry_count > 0) {
        dbg() << "MultiProcessor: Entry Type " << entry->entry_type << " detected.";
        switch (entry->entry_type) {
        case ((u8)MultiProcessor::ConfigurationTableEntryType::Processor):
            entry = (MultiProcessor::EntryHeader*)(u32)entry + (u8)MultiProcessor::ConfigurationTableEntryLength::Processor;
            p_entry = (MultiProcessor::EntryHeader*)(u32)p_entry + (u8)MultiProcessor::ConfigurationTableEntryLength::Processor;
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::Bus):
            m_bus_entries.append((uintptr_t)p_entry);
            entry = (MultiProcessor::EntryHeader*)(u32)entry + (u8)MultiProcessor::ConfigurationTableEntryLength::Bus;
            p_entry = (MultiProcessor::EntryHeader*)(u32)p_entry + (u8)MultiProcessor::ConfigurationTableEntryLength::Bus;
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::IOAPIC):
            entry = (MultiProcessor::EntryHeader*)(u32)entry + (u8)MultiProcessor::ConfigurationTableEntryLength::IOAPIC;
            p_entry = (MultiProcessor::EntryHeader*)(u32)p_entry + (u8)MultiProcessor::ConfigurationTableEntryLength::IOAPIC;
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::IO_Interrupt_Assignment):
            m_io_interrupt_redirection_entries.append((uintptr_t)p_entry);
            entry = (MultiProcessor::EntryHeader*)(u32)entry + (u8)MultiProcessor::ConfigurationTableEntryLength::IO_Interrupt_Assignment;
            p_entry = (MultiProcessor::EntryHeader*)(u32)p_entry + (u8)MultiProcessor::ConfigurationTableEntryLength::IO_Interrupt_Assignment;
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::Local_Interrupt_Assignment):
            entry = (MultiProcessor::EntryHeader*)(u32)entry + (u8)MultiProcessor::ConfigurationTableEntryLength::Local_Interrupt_Assignment;
            p_entry = (MultiProcessor::EntryHeader*)(u32)p_entry + (u8)MultiProcessor::ConfigurationTableEntryLength::Local_Interrupt_Assignment;
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::SystemAddressSpaceMapping):
            entry = (MultiProcessor::EntryHeader*)(u32)entry + (u8)MultiProcessor::ConfigurationTableEntryLength::SystemAddressSpaceMapping;
            p_entry = (MultiProcessor::EntryHeader*)(u32)p_entry + (u8)MultiProcessor::ConfigurationTableEntryLength::SystemAddressSpaceMapping;
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::BusHierarchyDescriptor):
            entry = (MultiProcessor::EntryHeader*)(u32)entry + (u8)MultiProcessor::ConfigurationTableEntryLength::BusHierarchyDescriptor;
            p_entry = (MultiProcessor::EntryHeader*)(u32)p_entry + (u8)MultiProcessor::ConfigurationTableEntryLength::BusHierarchyDescriptor;
            break;
        case ((u8)MultiProcessor::ConfigurationTableEntryType::CompatibilityBusAddressSpaceModifier):
            entry = (MultiProcessor::EntryHeader*)(u32)entry + (u8)MultiProcessor::ConfigurationTableEntryLength::CompatibilityBusAddressSpaceModifier;
            p_entry = (MultiProcessor::EntryHeader*)(u32)p_entry + (u8)MultiProcessor::ConfigurationTableEntryLength::CompatibilityBusAddressSpaceModifier;
            break;
            ASSERT_NOT_REACHED();
        }
        entry_count--;
    }
}

uintptr_t MultiProcessorParser::search_floating_pointer()
{
    uintptr_t mp_floating_pointer = (uintptr_t) nullptr;
    auto region = MM.allocate_kernel_region(PhysicalAddress(0), PAGE_SIZE, "MultiProcessor Parser Floating Pointer Structure Finding", Region::Access::Read);
    u16 ebda_seg = (u16) * ((uint16_t*)((region->vaddr().get() & PAGE_MASK) + 0x40e));
    kprintf("MultiProcessor: Probing EBDA, Segment 0x%x\n", ebda_seg);

    mp_floating_pointer = search_floating_pointer_in_ebda(ebda_seg);
    if (mp_floating_pointer != (uintptr_t) nullptr)
        return mp_floating_pointer;
    return search_floating_pointer_in_bios_area();
}

uintptr_t MultiProcessorParser::search_floating_pointer_in_ebda(u16 ebda_segment)
{
    auto floating_pointer_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)(ebda_segment << 4))), PAGE_ROUND_UP(1024), "MultiProcessor Parser floating_pointer Finding #1", Region::Access::Read, false, true);
    char* p_floating_pointer_str = (char*)(PhysicalAddress(ebda_segment << 4).as_ptr());
    for (char* floating_pointer_str = (char*)floating_pointer_region->vaddr().offset(offset_in_page((u32)(ebda_segment << 4))).as_ptr(); floating_pointer_str < (char*)(floating_pointer_region->vaddr().offset(offset_in_page((u32)(ebda_segment << 4))).get() + 1024); floating_pointer_str += 16) {
#ifdef MUTLIPROCESSOR_DEBUG
        dbg() << "MultiProcessor: Looking for floating pointer structure in EBDA @ V0x " << String::format("%x", floating_pointer_str) << ", P0x" << String::format("%x", p_floating_pointer_str);
#endif
        if (!strncmp("_MP_", floating_pointer_str, strlen("_MP_")))
            return (uintptr_t)p_floating_pointer_str;
        p_floating_pointer_str += 16;
    }
    return (uintptr_t) nullptr;
}
uintptr_t MultiProcessorParser::search_floating_pointer_in_bios_area()
{
    auto floating_pointer_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)0xE0000)), PAGE_ROUND_UP(0xFFFFF - 0xE0000), "MultiProcessor Parser floating_pointer Finding #2", Region::Access::Read, false, true);
    char* p_floating_pointer_str = (char*)(PhysicalAddress(0xE0000).as_ptr());
    for (char* floating_pointer_str = (char*)floating_pointer_region->vaddr().offset(offset_in_page((u32)(0xE0000))).as_ptr(); floating_pointer_str < (char*)(floating_pointer_region->vaddr().offset(offset_in_page((u32)(0xE0000))).get() + (0xFFFFF - 0xE0000)); floating_pointer_str += 16) {
#ifdef MUTLIPROCESSOR_DEBUG
        dbg() << "MultiProcessor: Looking for floating pointer structure in BIOS area @ V0x " << String::format("%x", floating_pointer_str) << ", P0x" << String::format("%x", p_floating_pointer_str);
#endif
        if (!strncmp("_MP_", floating_pointer_str, strlen("_MP_")))
            return (uintptr_t)p_floating_pointer_str;
        p_floating_pointer_str += 16;
    }
    return (uintptr_t) nullptr;
}

Vector<unsigned> MultiProcessorParser::get_pci_bus_ids()
{
    Vector<unsigned> pci_bus_ids;
    for (auto entry : m_bus_entries) {
        auto entry_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)entry)), PAGE_ROUND_UP(m_configuration_table_length), "MultiProcessor Parser Parsing Bus Entry", Region::Access::Read, false, true);
        auto* v_entry_ptr = (MultiProcessor::BusEntry*)entry_region->vaddr().offset(offset_in_page((u32)entry)).as_ptr();
        if (!strncmp("PCI   ", v_entry_ptr->bus_type, strlen("PCI   ")))
            pci_bus_ids.append(v_entry_ptr->bus_id);
    }
    return pci_bus_ids;
}

MultiProcessorParser& MultiProcessorParser::the()
{
    ASSERT(!MultiProcessorParser::is_initialized());
    return *s_parser;
}

Vector<RefPtr<PCIInterruptOverrideMetadata>> MultiProcessorParser::get_pci_interrupt_redirections()
{
    dbg() << "MultiProcessor: Get PCI IOAPIC redirections";
    Vector<RefPtr<PCIInterruptOverrideMetadata>> overrides;
    Vector<unsigned> pci_bus_ids = get_pci_bus_ids();
    for (auto entry : m_io_interrupt_redirection_entries) {
        auto entry_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)entry)), PAGE_ROUND_UP(m_configuration_table_length), "MultiProcessor Parser Parsing Bus Entry", Region::Access::Read, false, true);
        auto* v_entry_ptr = (MultiProcessor::IOInterruptAssignmentEntry*)entry_region->vaddr().offset(offset_in_page((u32)entry)).as_ptr();
        dbg() << "MultiProcessor: Parsing Entry P 0x" << String::format("%x", entry) << ", V " << v_entry_ptr;
        for (auto id : pci_bus_ids) {
            if (id == v_entry_ptr->source_bus_id) {

                kprintf("Interrupts: Bus %d, Polarity 0x%x, Trigger Mode 0x%x, INT %x, IOAPIC %d, IOAPIC INTIN %d\n", v_entry_ptr->source_bus_id,
                    v_entry_ptr->polarity,
                    v_entry_ptr->trigger_mode,
                    v_entry_ptr->source_bus_irq,
                    v_entry_ptr->destination_ioapic_id,
                    v_entry_ptr->destination_ioapic_intin_pin);
                overrides.append(adopt(*new PCIInterruptOverrideMetadata(
                    v_entry_ptr->source_bus_id,
                    v_entry_ptr->polarity,
                    v_entry_ptr->trigger_mode,
                    v_entry_ptr->source_bus_irq,
                    v_entry_ptr->destination_ioapic_id,
                    v_entry_ptr->destination_ioapic_intin_pin)));
            }
        }
    }

    for (auto override_metadata : overrides) {
        kprintf("Interrupts: Bus %d, Polarity 0x%x, PCI Device %d, Trigger Mode 0x%x, INT %x, IOAPIC %d, IOAPIC INTIN %d\n",
            override_metadata->bus(),
            override_metadata->polarity(),
            override_metadata->pci_device_number(),
            override_metadata->trigger_mode(),
            override_metadata->pci_interrupt_pin(),
            override_metadata->ioapic_id(),
            override_metadata->ioapic_interrupt_pin());
    }
    return overrides;
}

PCIInterruptOverrideMetadata::PCIInterruptOverrideMetadata(u8 bus_id, u8 polarity, u8 trigger_mode, u8 source_irq, u32 ioapic_id, u16 ioapic_int_pin)
    : m_bus_id(bus_id)
    , m_polarity(polarity)
    , m_trigger_mode(trigger_mode)
    , m_pci_interrupt_pin(source_irq & 0b11)
    , m_pci_device_number((source_irq & 0b11111) >> 2)
    , m_ioapic_id(ioapic_id)
    , m_ioapic_interrupt_pin(ioapic_int_pin)
{
}
u8 PCIInterruptOverrideMetadata::bus() const
{
    return m_bus_id;
}
u8 PCIInterruptOverrideMetadata::polarity() const
{
    return m_polarity;
}
u8 PCIInterruptOverrideMetadata::trigger_mode() const
{
    return m_trigger_mode;
}
u8 PCIInterruptOverrideMetadata::pci_interrupt_pin() const
{
    return m_pci_interrupt_pin;
}
u8 PCIInterruptOverrideMetadata::pci_device_number() const
{
    return m_pci_device_number;
}
u32 PCIInterruptOverrideMetadata::ioapic_id() const
{
    return m_ioapic_id;
}
u16 PCIInterruptOverrideMetadata::ioapic_interrupt_pin() const
{
    return m_ioapic_interrupt_pin;
}
}
