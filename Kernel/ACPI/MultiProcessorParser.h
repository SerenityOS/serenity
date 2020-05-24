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

#pragma once

#include <AK/Types.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel {
namespace MultiProcessor {

struct [[gnu::packed]] FloatingPointer
{
    char sig[4];
    u32 physical_address_ptr;
    u8 length;
    u8 specification_revision;
    u8 checksum;
    u8 feature_info[5];
};

struct [[gnu::packed]] EntryHeader
{
    u8 entry_type;
};

struct [[gnu::packed]] ConfigurationTableHeader
{
    char sig[4];
    u16 length;
    u8 specification_revision;
    u8 checksum;
    char oem_id[8];
    char product_id[12];
    u32 oem_table_ptr;
    u16 oem_table_size;
    u16 entry_count;
    u32 local_apic_address;
    u16 ext_table_length;
    u8 ext_table_checksum;
    u8 reserved;
    EntryHeader entries[];
};

enum class ConfigurationTableEntryType {
    Processor = 0,
    Bus = 1,
    IOAPIC = 2,
    IO_Interrupt_Assignment = 3,
    Local_Interrupt_Assignment = 4,
    SystemAddressSpaceMapping = 128,
    BusHierarchyDescriptor = 129,
    CompatibilityBusAddressSpaceModifier = 130
};

struct [[gnu::packed]] ExtEntryHeader
{
    u8 entry_type;
    u8 entry_length;
};

struct [[gnu::packed]] ProcessorEntry
{
    EntryHeader h;
    u8 local_apic_id;
    u8 local_apic_version;
    u8 cpu_flags;
    u32 cpu_signature;
    u32 feature_flags;
    u8 reserved[8];
};

struct [[gnu::packed]] BusEntry
{
    EntryHeader h;
    u8 bus_id;
    char bus_type[6];
};

struct [[gnu::packed]] IOAPICEntry
{
    EntryHeader h;
    u8 ioapic_id;
    u8 ioapic_version;
    u8 ioapic_flags;
    u32 ioapic_address;
};

enum class InterruptType {
    INT = 0,
    NMI = 1,
    SMI = 2,
    ExtINT = 3,
};

struct [[gnu::packed]] IOInterruptAssignmentEntry
{
    EntryHeader h;
    u8 interrupt_type;
    u8 polarity;
    u8 trigger_mode;
    u8 source_bus_id;
    u8 source_bus_irq;
    u8 destination_ioapic_id;
    u8 destination_ioapic_intin_pin;
};

struct [[gnu::packed]] LocalInterruptAssignmentEntry
{
    EntryHeader h;
    u8 interrupt_type;
    u8 polarity;
    u8 trigger_mode;
    u8 source_bus_id;
    u8 source_bus_irq;
    u8 destination_lapic_id;
    u8 destination_lapic_lintin_pin;
};

enum class SystemAddressType {
    IO = 0,
    Memory = 1,
    Prefetch = 2,
};

struct [[gnu::packed]] SystemAddressSpaceMappingEntry
{
    ExtEntryHeader h;
    u8 bus_id;
    u8 address_type;
    u64 address_base;
    u64 length;
};

struct [[gnu::packed]] BusHierarchyDescriptorEntry
{
    ExtEntryHeader h;
    u8 bus_id;
    u8 bus_info;
    u8 parent_bus;
    u8 reserved[3];
};

struct [[gnu::packed]] CompatibilityBusAddressSpaceModifierEntry
{
    ExtEntryHeader h;
    u8 bus_id;
    u8 address_modifier;
    u32 predefined_range_list;
};

}

class PCIInterruptOverrideMetadata;

class MultiProcessorParser final {
public:
    static OwnPtr<MultiProcessorParser> autodetect();

    Vector<PCIInterruptOverrideMetadata> get_pci_interrupt_redirections();

private:
    explicit MultiProcessorParser(PhysicalAddress floating_pointer);

    void parse_configuration_table();
    void parse_floating_pointer_data();

    Vector<u8> get_pci_bus_ids() const;

    static Optional<PhysicalAddress> find_floating_pointer();

    PhysicalAddress m_floating_pointer;
    PhysicalAddress m_configuration_table;
    Vector<MultiProcessor::IOInterruptAssignmentEntry> m_io_interrupt_assignment_entries;
    Vector<MultiProcessor::BusEntry> m_bus_entries;
};
}
