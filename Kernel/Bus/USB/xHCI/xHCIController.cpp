/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBHub.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Bus/USB/xHCI/xHCIController.h>
#include <Kernel/Bus/USB/xHCI/xHCIInterrupter.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel::USB {

UNMAP_AFTER_INIT xHCIController::xHCIController(Memory::TypedMapping<u8> registers_mapping)
    : m_registers_mapping(move(registers_mapping))
    , m_capability_registers(*reinterpret_cast<CapabilityRegisters volatile*>(m_registers_mapping.ptr()))
    , m_operational_registers(*reinterpret_cast<OperationalRegisters volatile*>(m_registers_mapping.ptr() + m_capability_registers.capability_register_length))
    , m_runtime_registers(*reinterpret_cast<RuntimeRegisters volatile*>(m_registers_mapping.ptr() + m_capability_registers.runtime_register_space_offset))
    , m_doorbell_registers(*reinterpret_cast<DoorbellRegisters volatile*>(m_registers_mapping.ptr() + m_capability_registers.doorbell_offset))
{
}

xHCIController::~xHCIController()
{
    if (m_process) {
        m_process->die();
        // Block until all threads exited to prevent UAF
        ErrorOr<siginfo_t> result = siginfo_t {};
        (void)Thread::current()->block<Thread::WaitBlocker>({}, WEXITED, m_process.release_nonnull(), result);
    }
}

// 4.22.1 Pre-OS to OS Handoff Synchronization
void xHCIController::take_exclusive_control_from_bios()
{
    if (m_capability_registers.capability_parameters_1.xHCI_extended_capabilities_pointer == 0)
        return;
    auto* extended_capabilities_pointer = m_registers_mapping.ptr() + (m_capability_registers.capability_parameters_1.xHCI_extended_capabilities_pointer << 2);

    ExtendedCapability volatile* extended_capability = nullptr;
    while (true) {
        extended_capability = reinterpret_cast<ExtendedCapability volatile*>(extended_capabilities_pointer);
        if (extended_capability->capability_id == ExtendedCapability::CapabilityID::USB_Legacy_Support)
            break;
        u32 const next_extended_capability_offset = extended_capability->next_xHCI_extended_capability_pointer << 2;
        if (next_extended_capability_offset == 0)
            return;
        extended_capabilities_pointer += next_extended_capability_offset;
    };

    auto* usb_legacy_support_extended_capability = reinterpret_cast<USBLegacySupportExtendedCapability volatile*>(extended_capability);
    if (!usb_legacy_support_extended_capability->usb_legacy_support_capability.host_controller_bios_owned_semaphore)
        return;

    dmesgln_xhci("Controller is owned by BIOS - taking ownership");
    usb_legacy_support_extended_capability->usb_legacy_support_capability.host_controller_os_owned_semaphore = 1;
    for (auto attempts = 0; attempts < 20; ++attempts) {
        if (!usb_legacy_support_extended_capability->usb_legacy_support_capability.host_controller_bios_owned_semaphore)
            break;
        microseconds_delay(50000); // The time that OS shall wait for BIOS to respond to the request for ownership should not exceed ‘1’ second.
    }
    if (usb_legacy_support_extended_capability->usb_legacy_support_capability.host_controller_bios_owned_semaphore)
        dmesgln_xhci("Bios refuses to transfer ownership - ignoring");
    else if (usb_legacy_support_extended_capability->usb_legacy_support_capability.host_controller_os_owned_semaphore)
        dmesgln_xhci("Took ownership of controller successfully");
    // Force disable BIOS control in case the BIOS is broken/non-responsive (disable their SMIs)
    usb_legacy_support_extended_capability->usb_legacy_support_capability.host_controller_bios_owned_semaphore = 0;
    usb_legacy_support_extended_capability->usb_legacy_support_control_status.usb_smi_enable = 0;
    usb_legacy_support_extended_capability->usb_legacy_support_control_status.smi_on_host_system_error_enable = 0;
    usb_legacy_support_extended_capability->usb_legacy_support_control_status.smi_on_os_ownership_enable = 0;
    usb_legacy_support_extended_capability->usb_legacy_support_control_status.smi_on_pci_command_enable = 0;
    usb_legacy_support_extended_capability->usb_legacy_support_control_status.smi_on_bar_enable = 0;
    // write '1' to clear bits
    usb_legacy_support_extended_capability->usb_legacy_support_control_status.smi_on_os_ownership_change = 1;
    usb_legacy_support_extended_capability->usb_legacy_support_control_status.smi_on_pci_command = 1;
    usb_legacy_support_extended_capability->usb_legacy_support_control_status.smi_on_bar = 1;
}

ErrorOr<void> xHCIController::find_port_max_speeds()
{
    // At least one of these capability structures is required for all xHCI implementations.
    if (m_capability_registers.capability_parameters_1.xHCI_extended_capabilities_pointer == 0)
        return EINVAL;
    auto* extended_capabilities_pointer = m_registers_mapping.ptr() + (m_capability_registers.capability_parameters_1.xHCI_extended_capabilities_pointer << 2);

    ExtendedCapability volatile* extended_capability = nullptr;
    Vector<SupportedProtocolExtendedCapability volatile*> supported_protocol_capabilities;
    while (true) {
        extended_capability = reinterpret_cast<ExtendedCapability volatile*>(extended_capabilities_pointer);
        if (extended_capability->capability_id == ExtendedCapability::CapabilityID::Supported_Protocols)
            TRY(supported_protocol_capabilities.try_append(reinterpret_cast<SupportedProtocolExtendedCapability volatile*>(extended_capability)));
        u32 const next_extended_capability_offset = extended_capability->next_xHCI_extended_capability_pointer << 2;
        if (next_extended_capability_offset == 0)
            break;
        extended_capabilities_pointer += next_extended_capability_offset;
    };

    if (supported_protocol_capabilities.is_empty())
        return EINVAL;

    for (auto* supported_protocol_capability : supported_protocol_capabilities) {
        if (supported_protocol_capability->name_string != SupportedProtocolExtendedCapability::usb_name_string)
            continue;
        auto major_revision = supported_protocol_capability->major_revision;
        if (major_revision < 0x02 || major_revision > 0x03)
            continue;
        auto minor_revision = supported_protocol_capability->minor_revision;
        if (major_revision == 0x02 && minor_revision != 0x00)
            continue;
        if (major_revision == 0x03 && minor_revision > 0x20)
            continue;
        auto offset = supported_protocol_capability->compatible_port_offset;
        if (offset < 1 || offset > m_port_max_speeds.size())
            continue;
        auto count = supported_protocol_capability->compatible_port_count;
        if (count == 0 || count > (m_port_max_speeds.size() - offset + 1))
            continue;
        if (supported_protocol_capability->protocol_speed_id_count > 0) {
            dmesgln_xhci("Controller has explicit protocol speed ID definitions - this is not supported yet");
            continue;
        }
        auto max_speed = major_revision == 0x03 ? USB::Device::DeviceSpeed::SuperSpeed : USB::Device::DeviceSpeed::HighSpeed;
        for (auto i = offset - 1; i < count; ++i)
            m_port_max_speeds[i] = max_speed;
    }
    return {};
}

ErrorOr<void> xHCIController::initialize()
{
    dmesgln_xhci("Registers base: {}", m_registers_mapping.paddr);

    auto interface_version = m_capability_registers.host_controller_interface_version_number;
    if (interface_version < 0x0090 || interface_version > 0x0120) { // The Intel specification defines versions 0.9.0 up to 1.2.0
        dmesgln_xhci("Unsupported interface version: {}.{}.{}", interface_version >> 8, (interface_version >> 4) & 0xF, interface_version & 0xF);
        return ENOTSUP;
    }
    dmesgln_xhci("Interface version: {}.{}.{}", interface_version >> 8, (interface_version >> 4) & 0xF, interface_version & 0xF);
    dbgln_if(XHCI_DEBUG, "xHCI: Using {}-bit addressing", m_capability_registers.capability_parameters_1._64bit_addressing_capability ? 64 : 32);

    take_exclusive_control_from_bios();

    TRY(find_port_max_speeds());

    TRY(reset());

    if ((m_operational_registers.page_size & 1) == 0) {
        dmesgln_xhci("Interface does not support 4K pages");
        return ENOTSUP;
    }

    auto [process, event_handler_thread] = TRY(Process::create_kernel_process("xHCI Controller"sv, [this]() { event_handling_thread(); }));
    event_handler_thread->set_name("xHCI Event Handling"sv);
    (void)TRY(process->create_kernel_thread("xHCI Hot Plug"sv, [this]() { hot_plug_thread(); }));
    m_process = move(process);

    m_large_contexts = m_capability_registers.capability_parameters_1.context_size;
    m_ports = m_capability_registers.structural_parameters.number_of_ports;
    m_device_slots = min((u8)m_capability_registers.structural_parameters.number_of_device_slots, max_devices);
    // 4.2 Host Controller Initialization
    // 1. Program the Max Device Slots Enabled (MaxSlotsEn) field in the CONFIG register (5.4.7) to enable the device slots that system software is going to use.
    m_operational_registers.configure.max_device_slots_enabled = m_device_slots;

    // 2. Program the Device Context Base Address Array Pointer (DCBAAP) register (5.4.6) with a 64-bit address pointing to where the Device Context Base Address Array is located.
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    m_device_context_base_address_array_region = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up((m_device_slots + 1) * sizeof(u64))), "xHCI Device Context Base Address Array"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
    dbgln_if(XHCI_DEBUG, "xHCI: Device Context Base Address Array - {} / {}", m_device_context_base_address_array_region->vaddr(), m_device_context_base_address_array_region->physical_page(0)->paddr());
    m_device_context_base_address_array = reinterpret_cast<u64*>(m_device_context_base_address_array_region->vaddr().as_ptr());
    auto requested_scratchpad_buffers = (m_capability_registers.structural_parameters.max_scratchpad_buffers_high << 5) | m_capability_registers.structural_parameters.max_scratchpad_buffers_low;
    if (requested_scratchpad_buffers > 0) {
        dbgln_if(XHCI_DEBUG, "xHCI: Allocating {} scratchpad buffers", requested_scratchpad_buffers);
        m_scratchpad_buffers_array_region = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up(requested_scratchpad_buffers * sizeof(u64))), "xHCI Scratchpad Buffers Array"sv, Memory::Region::Access::ReadWrite));
        auto* scratchpad_buffers_array = reinterpret_cast<u64*>(m_scratchpad_buffers_array_region->vaddr().as_ptr());
        for (auto i = 0; i < requested_scratchpad_buffers; ++i) {
            auto page = TRY(MM.allocate_physical_page());
            scratchpad_buffers_array[i] = page->paddr().get();
            TRY(m_scratchpad_buffers.try_append(move(page)));
        }
        m_device_context_base_address_array[0] = m_scratchpad_buffers_array_region->physical_page(0)->paddr().get();
    } else {
        m_device_context_base_address_array[0] = 0;
    }
    auto device_context_base_address_array_pointer = m_device_context_base_address_array_region->physical_page(0)->paddr().get();
    m_operational_registers.device_context_base_address_array_pointer.low = device_context_base_address_array_pointer;
    m_operational_registers.device_context_base_address_array_pointer.high = device_context_base_address_array_pointer >> 32;

    // 3. Define the Command Ring Dequeue Pointer by programming the Command Ring Control Register (5.4.5) with a 64-bit address pointing to the starting address of the first TRB of the Command Ring.
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    m_command_and_event_rings_region = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up(sizeof(CommandAndEventRings))), "xHCI Command and Event Rings"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
    dbgln_if(XHCI_DEBUG, "xHCI: Command and Event Rings - {} / {}", m_command_and_event_rings_region->vaddr(), m_command_and_event_rings_region->physical_page(0)->paddr());
    auto command_and_event_rings_region_virtual_address = m_command_and_event_rings_region->vaddr().get();
    m_command_ring = reinterpret_cast<TransferRequestBlock*>(command_and_event_rings_region_virtual_address + __builtin_offsetof(CommandAndEventRings, command_ring));
    m_command_ring[command_ring_size - 1].generic.transfer_request_block_type = TransferRequestBlock::TRBType::Link;
    auto command_and_event_rings_region_physical_address = m_command_and_event_rings_region->physical_page(0)->paddr().get();
    auto command_ring_pointer = command_and_event_rings_region_physical_address + __builtin_offsetof(CommandAndEventRings, command_ring);
    m_command_ring[command_ring_size - 1].link.ring_segment_pointer_low = command_ring_pointer;
    m_command_ring[command_ring_size - 1].link.ring_segment_pointer_high = command_ring_pointer >> 32;
    m_command_ring[command_ring_size - 1].link.toggle_cycle = 1;

    // Must be written as 2 whole 32-bit writes, since reads always return 0 (so RMW won't work)
    CommandRingControlRegister command_ring_control_register;
    command_ring_control_register.command_ring_pointer_low = command_ring_pointer >> 6;
    command_ring_control_register.command_ring_pointer_high = command_ring_pointer >> 32;
    command_ring_control_register.ring_cycle_state = 1;
    m_operational_registers.command_ring_control.raw0 = command_ring_control_register.raw0;
    m_operational_registers.command_ring_control.raw1 = command_ring_control_register.raw1;

    // Clear interrupt conditions left-over from BIOS
    m_operational_registers.usb_status.raw = m_operational_registers.usb_status.raw;

    // TODO: Support more than one interrupter using MSI/MSI-X
    // 4. Initialize each active interrupter by:
    //   1. Defining the Event Ring: (refer to section 4.9.4 for a discussion of Event Ring Management.)
    //     1. Allocate and initialize the Event Ring Segment(s).
    m_event_ring_segment = reinterpret_cast<TransferRequestBlock*>(command_and_event_rings_region_virtual_address + __builtin_offsetof(CommandAndEventRings, event_ring_segment));
    m_event_ring_segment_pointer = command_and_event_rings_region_physical_address + __builtin_offsetof(CommandAndEventRings, event_ring_segment);

    //     2. Allocate the Event Ring Segment Table (ERST) (section 6.5).
    //     Initialize ERST table entries to point to and to define the size (in TRBs) of the respective Event Ring Segment.
    auto* event_ring_segment_table = reinterpret_cast<EventRingSegmentTableEntry*>(command_and_event_rings_region_virtual_address + __builtin_offsetof(CommandAndEventRings, event_ring_segment_table_entry));
    event_ring_segment_table->ring_segment_base_address_low = m_event_ring_segment_pointer;
    event_ring_segment_table->ring_segment_base_address_high = m_event_ring_segment_pointer >> 32;
    event_ring_segment_table->ring_segment_size = event_ring_segment_size;

    //     3. Program the Interrupter Event Ring Segment Table Size (ERSTSZ) register (5.5.2.3.1) with the number of segments described by the Event Ring Segment Table.
    m_runtime_registers.interrupter_registers[0].even_ring_segment_table_size = 1;

    //     4. Program the Interrupter Event Ring Dequeue Pointer (ERDP) register (5.5.2.3.3) with the starting address of the first segment described by the Event Ring Segment Table.
    m_runtime_registers.interrupter_registers[0].event_ring_dequeue_pointer.event_ring_dequeue_pointer_low = m_event_ring_segment_pointer >> 4;
    m_runtime_registers.interrupter_registers[0].event_ring_dequeue_pointer.event_ring_dequeue_pointer_high = m_event_ring_segment_pointer >> 32;

    //     5. Program the Interrupter Event Ring Segment Table Base Address (ERSTBA) register (5.5.2.3.2) with a 64-bit address pointer to where the Event Ring Segment Table is located.
    auto event_ring_segment_table_entry_address = command_and_event_rings_region_physical_address + __builtin_offsetof(CommandAndEventRings, event_ring_segment_table_entry);
    m_runtime_registers.interrupter_registers[0].event_ring_segment_table_base_address.low = event_ring_segment_table_entry_address;
    m_runtime_registers.interrupter_registers[0].event_ring_segment_table_base_address.high = event_ring_segment_table_entry_address >> 32;

    //   2. Defining the interrupts:
    //     1. Initializing the Interval field of the Interrupt Moderation register (5.5.2.2) with the target interrupt moderation rate.
    m_runtime_registers.interrupter_registers[0].interrupter_moderation.interrupt_moderation_interval = 0x3F8; // max 4000 interrupts/sec

    //     2. Enable system bus interrupt generation by writing a ‘1’ to the Interrupter Enable (INTE) flag of the USBCMD register (5.4.1).
    m_operational_registers.usb_command.interrupter_enable = 1;
    m_operational_registers.usb_command.host_system_error_enable = 1;

    //     3. Enable the Interrupter by writing a ‘1’ to the Interrupt Enable (IE) field of the Interrupter Management register (5.5.2.1).
    m_runtime_registers.interrupter_registers[0].interrupter_management.interrupt_enabled = 1;

    m_using_message_signalled_interrupts = using_message_signalled_interrupts();
    m_interrupter = TRY(create_interrupter(0));

    return start();
}

ErrorOr<void> xHCIController::reset()
{
    dbgln_if(XHCI_DEBUG, "Resetting xHCI Controller");
    TRY(stop());

    m_operational_registers.usb_command.host_controller_reset = 1;
    for (auto attempts = 0; attempts < 1000; ++attempts) {
        microseconds_delay(1000);
        if (!m_operational_registers.usb_command.host_controller_reset)
            break;
    }
    if (m_operational_registers.usb_command.host_controller_reset) {
        dmesgln_xhci("Failed resetting controller - stuck in reset state");
        return EBUSY;
    }

    // After Chip Hardware Reset wait until the Controller Not Ready (CNR) flag in the USBSTS is ‘0’ before writing any xHC Operational or Runtime/ registers.
    for (auto attempts = 0; attempts < 1000; ++attempts) {
        microseconds_delay(1000);
        if (!m_operational_registers.usb_status.controller_not_ready)
            return {};
    }
    dmesgln_xhci("Failed resetting controller - stuck in not-ready state");
    return EBUSY;
}

ErrorOr<void> xHCIController::start()
{
    m_operational_registers.usb_command.run_stop = 1;
    for (auto attempts = 0; attempts < 1000; ++attempts) {
        microseconds_delay(1000);
        if (!m_operational_registers.usb_status.host_controller_halted)
            break;
    }
    if (m_operational_registers.usb_status.host_controller_halted) {
        dmesgln_xhci("Failed starting controller");
        return EBUSY;
    }
    dmesgln_xhci("Finished starting controller");

    m_root_hub = TRY(xHCIRootHub::try_create(*this));
    TRY(m_root_hub->setup({}));
    dmesgln_xhci("Initialized root hub");
    return {};
}

ErrorOr<void> xHCIController::stop()
{
    m_operational_registers.usb_command.run_stop = 0;
    for (auto attempts = 0; attempts < 1000; ++attempts) {
        microseconds_delay(1000);
        if (m_operational_registers.usb_status.host_controller_halted)
            return {};
    }
    dmesgln_xhci("Failed stopping controller");
    return EBUSY;
}

void xHCIController::ring_doorbell(u8 doorbell, u8 doorbell_target)
{
    DoorbellRegister const doorbell_value {
        .doorbell_target = doorbell_target,
        .reserved0 = 0,
        .doorbell_stream_id = 0,
    };
    m_doorbell_registers.doorbells[doorbell] = doorbell_value.raw;
    // Read-after-write to serialize PCI transactions
    (void)m_doorbell_registers.doorbells[doorbell];
}

void xHCIController::enqueue_command(TransferRequestBlock& transfer_request_block)
{
    transfer_request_block.generic.cycle_bit = m_command_ring_producer_cycle_state;
    m_command_ring[m_command_ring_enqueue_index] = transfer_request_block;

    m_command_ring_enqueue_index++;

    if (m_command_ring_enqueue_index == (command_ring_size - 1)) {
        // Reached Link TRB, flip cycle bit and return to start
        m_command_ring[m_command_ring_enqueue_index].link.cycle_bit = m_command_ring_producer_cycle_state;
        m_command_ring_enqueue_index = 0;
        m_command_ring_producer_cycle_state ^= 1;
    }

    atomic_thread_fence(MemoryOrder::memory_order_seq_cst);

    ring_command_doorbell();
}

void xHCIController::execute_command(TransferRequestBlock& transfer_request_block)
{
    SpinlockLocker const locker(m_command_lock);
    enqueue_command(transfer_request_block);
    m_command_completion_queue.wait_forever();
    transfer_request_block = m_command_result_transfer_request_block;
}

ErrorOr<u8> xHCIController::enable_slot()
{
    // 4.6.3 Enable Slot
    // Insert an Enable Slot Command TRB on the Command Ring and initialize the following fields:
    // * TRB Type = Enable Slot command (refer to Table 6-91).
    // * Slot Type = value specified by the Protocol Slot Type field of the associated xHCI Supported Protocol Capability structure (refer to Table 7-9).
    // 7.2.2.1.4 Protocol Slot Type Field
    // The Protocol Slot Type field of a USB3 or USB2 xHCI Supported Protocol Capability shall be set to '0'
    TransferRequestBlock transfer_request_block {};
    transfer_request_block.enable_slot_command.transfer_request_block_type = TransferRequestBlock::TRBType::Enable_Slot_Command;
    transfer_request_block.enable_slot_command.slot_type = 0;
    execute_command(transfer_request_block);
    // If a slot is available, the ID of a selected slot will be returned in the Slot ID field of a successful Command Completion Event on the Event Ring.
    if (transfer_request_block.command_completion_event.completion_code == TransferRequestBlock::CompletionCode::Success) {
        VERIFY(transfer_request_block.command_completion_event.slot_id != 0);
        return (u8)transfer_request_block.command_completion_event.slot_id;
    }
    // If a Device Slot is not available, the Slot ID field shall be cleared to '0' and a No Slots Available Error shall be returned in the Command Completion Event.
    dmesgln_xhci("Enable Slot command failed with completion code: {}", enum_to_string(transfer_request_block.command_completion_event.completion_code));
    return EINVAL;
}

ErrorOr<void> xHCIController::address_device(u8 slot, u64 input_context_address)
{
    // 4.6.5 Address Device
    // Insert an Address Device Command on the Command Ring and initialize the following fields:
    // * TRB Type = Address Device command (refer to Table 6-91).
    // * Slot ID = ID of the target Device Slot.
    // * Input Context Pointer = The base address of the Input Context data structure.
    TransferRequestBlock transfer_request_block {};
    transfer_request_block.address_device_command.transfer_request_block_type = TransferRequestBlock::TRBType::Address_Device_Command;
    transfer_request_block.address_device_command.slot_id = slot;
    transfer_request_block.address_device_command.input_context_pointer_low = input_context_address;
    transfer_request_block.address_device_command.input_context_pointer_high = input_context_address >> 32;
    transfer_request_block.address_device_command.block_set_address_request = 0;
    execute_command(transfer_request_block);
    if (transfer_request_block.command_completion_event.completion_code != TransferRequestBlock::CompletionCode::Success) {
        dmesgln_xhci("Address Device command failed with completion code: {}", enum_to_string(transfer_request_block.command_completion_event.completion_code));
        return EINVAL;
    }
    return {};
}

ErrorOr<void> xHCIController::evaluate_context(u8 slot, u64 input_context_address)
{
    // 4.6.7 Evaluate Context
    // Insert an Evaluate Context Command on the Command Ring and initialize the following fields:
    // * TRB Type = Evaluate Context Command (refer to Table 6-91).
    // * Slot ID = ID of the target Device Slot.
    // * Input Context Pointer = The base address of the Input Context data structure.
    TransferRequestBlock transfer_request_block {};
    transfer_request_block.evaluate_context_command.transfer_request_block_type = TransferRequestBlock::TRBType::Evaluate_Context_Command;
    transfer_request_block.evaluate_context_command.slot_id = slot;
    transfer_request_block.evaluate_context_command.input_context_pointer_low = input_context_address;
    transfer_request_block.evaluate_context_command.input_context_pointer_high = input_context_address >> 32;
    execute_command(transfer_request_block);
    if (transfer_request_block.command_completion_event.completion_code != TransferRequestBlock::CompletionCode::Success) {
        dmesgln_xhci("Evaluate Context command failed with completion code: {}", enum_to_string(transfer_request_block.command_completion_event.completion_code));
        return EINVAL;
    }
    return {};
}

ErrorOr<void> xHCIController::configure_endpoint(u8 slot, u64 input_context_address)
{
    // 4.6.6 Configure Endpoint
    // Insert a Configure Endpoint Command on the Command Ring and initialize the following fields:
    // * TRB Type = Configure Endpoint Command (refer to Table 6-91).
    // * Slot ID = ID of the target Device Slot.
    // * Input Context Pointer = The base address of the Input Context data structure.
    TransferRequestBlock transfer_request_block {};
    transfer_request_block.configure_endpoint_command.transfer_request_block_type = TransferRequestBlock::TRBType::Configure_Endpoint_Command;
    transfer_request_block.configure_endpoint_command.slot_id = slot;
    transfer_request_block.configure_endpoint_command.input_context_pointer_low = input_context_address;
    transfer_request_block.configure_endpoint_command.input_context_pointer_high = input_context_address >> 32;
    transfer_request_block.configure_endpoint_command.deconfigure = 0;
    execute_command(transfer_request_block);
    if (transfer_request_block.command_completion_event.completion_code != TransferRequestBlock::CompletionCode::Success) {
        dmesgln_xhci("Configure Endpoint command failed with completion code: {}", enum_to_string(transfer_request_block.command_completion_event.completion_code));
        return EINVAL;
    }
    return {};
}

ErrorOr<void> xHCIController::reset_endpoint(u8 slot, u8 endpoint, TransferStatePreserve transfer_state_preserve)
{
    // 4.6.8 Reset Endpoint
    // Insert a Reset Endpoint Command TRB on the Command Ring and initialize the following fields:
    // * TRB Type = Reset Endpoint Command (r refer to Table 6-91).
    // * Transfer State Preserve (TSP) = Desired Transfer State result.
    // * Endpoint ID = ID of the target endpoint.
    // * Slot ID = ID of the target Device Slot.
    TransferRequestBlock transfer_request_block {};
    transfer_request_block.reset_endpoint_command.transfer_request_block_type = TransferRequestBlock::TRBType::Reset_Endpoint_Command;
    transfer_request_block.reset_endpoint_command.transfer_state_preserve = transfer_state_preserve == TransferStatePreserve::Yes ? 1 : 0;
    transfer_request_block.reset_endpoint_command.endpoint_id = endpoint;
    transfer_request_block.reset_endpoint_command.slot_id = slot;
    execute_command(transfer_request_block);
    if (transfer_request_block.command_completion_event.completion_code != TransferRequestBlock::CompletionCode::Success) {
        dmesgln_xhci("Reset Endpoint command failed with completion code: {}", enum_to_string(transfer_request_block.command_completion_event.completion_code));
        return EINVAL;
    }
    return {};
}

ErrorOr<void> xHCIController::set_tr_dequeue_pointer(u8 slot, u8 endpoint, u8 stream_context_type, u16 stream, u64 new_tr_dequeue_pointer, u8 dequeue_cycle_state)
{
    // 4.6.10 Set TR Dequeue Pointer
    // Insert a Set TR Dequeue Pointer Command on the Command Ring and initialize the following fields:
    // * TRB Type = Set TR Dequeue Pointer Command (refer to Table 6-91).
    // * Endpoint ID = ID of the target endpoint.
    // * Stream ID = ID of the target Stream Context or ‘0’ if MaxPStreams = ‘0’.
    // * Slot ID = ID of the target Device Slot.
    // * New TR Dequeue Pointer = The new TR Dequeue Pointer field value for the target endpoint.
    // * Dequeue Cycle State (DCS) = The state of the xHCI CCS flag for the TRB pointed to by the TR Dequeue Pointer field.
    TransferRequestBlock transfer_request_block {};
    transfer_request_block.set_tr_dequeue_pointer_command.transfer_request_block_type = TransferRequestBlock::TRBType::Set_TR_Dequeue_Pointer_Command;
    transfer_request_block.set_tr_dequeue_pointer_command.endpoint_id = endpoint;
    transfer_request_block.set_tr_dequeue_pointer_command.stream_id = stream;
    transfer_request_block.set_tr_dequeue_pointer_command.slot_id = slot;
    transfer_request_block.set_tr_dequeue_pointer_command.new_tr_dequeue_pointer_low = new_tr_dequeue_pointer >> 4;
    transfer_request_block.set_tr_dequeue_pointer_command.new_tr_dequeue_pointer_high = new_tr_dequeue_pointer >> 32;
    transfer_request_block.set_tr_dequeue_pointer_command.dequeue_cycle_state = dequeue_cycle_state;
    transfer_request_block.set_tr_dequeue_pointer_command.stream_context_type = stream_context_type;
    execute_command(transfer_request_block);
    if (transfer_request_block.command_completion_event.completion_code != TransferRequestBlock::CompletionCode::Success) {
        dmesgln_xhci("Set TR Dequeue Pointer command failed with completion code: {}", enum_to_string(transfer_request_block.command_completion_event.completion_code));
        return EINVAL;
    }
    return {};
}

ErrorOr<void> xHCIController::initialize_device(USB::Device& device)
{
    // 4. After the port successfully reaches the Enabled state, system software shall obtain a Device Slot for the newly attached device using an Enable Slot Command,
    // as described in section 4.3.2.
    auto slot = TRY(enable_slot());
    VERIFY(slot > 0 && slot <= m_device_slots);
    device.set_controller_identifier<xHCIController>({}, slot);

    auto& slot_state = m_slots_state[slot - 1];
    SpinlockLocker const locker(slot_state.lock);
    VERIFY(!slot_state.input_context_region); // Prevent trying to initialize an already initialized device

    // 5. After successfully obtaining a Device Slot, system software shall initialize the data structures associated with the slot as described in section 4.3.3.
    //   1. Allocate an Input Context data structure (6.2.5) and initialize all fields to ‘0’.
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    slot_state.input_context_region = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up(input_context_size())), "xHCI Input Context"sv, Memory::Region::ReadWrite, Memory::MemoryType::IO));

    //   2. Initialize the Input Control Context (6.2.5.1) of the Input Context by setting the A0 and A1 flags to ‘1’.
    // These flags indicate that the Slot Context and the Endpoint 0 Context of the Input Context are affected by the command.
    auto* control_context = input_control_context(slot);
    control_context->drop_contexts = 0;
    control_context->add_contexts = (1 << 0) | (1 << 1);

    //   3. Initialize the Input Slot Context data structure (6.2.2).
    auto* slot_context = input_slot_context(slot);
    u8 parent_hub_port = device.port();
    u32 device_route = 0;
    for (auto const* hub = device.hub(); hub != &m_root_hub->hub(); hub = hub->hub()) {
        device_route <<= 4;
        device_route |= min(parent_hub_port, 15);
        parent_hub_port = hub->port();
    }
    //   * Root Hub Port Number = Topology defined.
    slot_context->root_hub_port_number = parent_hub_port;
    //   * Route String = Topology defined. Refer to section 8.9 in the USB3 spec. Note that the Route String does not include the Root Hub Port Number.
    slot_context->route_string = device_route;
    //   * Context Entries = 1.
    slot_context->context_entries = 1;
    //   * Interrupter Target = System defined.
    slot_context->interrupter_target = 0; // TODO: Support more than one interrupter using MSI/MSI-X
    //   * Speed = Defined by downstream facing port attached to the device
    USB::Device::DeviceSpeed speed = device.speed();
    if (device_route == 0) { // If this is a root hub port, use the PORTSC Port Speed instead of relying on the fake hub reported speed
        auto port_speed = m_operational_registers.port_registers[device.port() - 1].port_status_and_control.port_speed;
        slot_context->speed = port_speed;
        switch (port_speed) {
        case 1:
            speed = USB::Device::DeviceSpeed::FullSpeed;
            break;
        case 2:
            speed = USB::Device::DeviceSpeed::LowSpeed;
            break;
        case 3:
            speed = USB::Device::DeviceSpeed::HighSpeed;
            break;
        case 4:
            speed = USB::Device::DeviceSpeed::SuperSpeed;
            break;
        default:
            dmesgln_xhci("Unknown port speed reported ({}), assuming SuperSpeed/USB3", port_speed);
            speed = USB::Device::DeviceSpeed::SuperSpeed;
            break;
        }
    } else {
        switch (speed) {
        case USB::Device::DeviceSpeed::LowSpeed:
            slot_context->speed = 2;
            break;
        case USB::Device::DeviceSpeed::FullSpeed:
            slot_context->speed = 1;
            break;
        case USB::Device::DeviceSpeed::HighSpeed:
            slot_context->speed = 3;
            break;
        case USB::Device::DeviceSpeed::SuperSpeed:
            slot_context->speed = 4;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }
    //   * If the device is a Low-/Full-speed function or hub accessed through a High-speed hub, then the following values are derived from the “parent” High-speed hub whose downstream facing port isolates the High-speed signaling environment from the Low-/Full-speed signaling environment:
    if (device_route != 0 && (speed == USB::Device::DeviceSpeed::LowSpeed || speed == USB::Device::DeviceSpeed::FullSpeed) && device.hub()->speed() == USB::Device::DeviceSpeed::HighSpeed) {
        // * MTT = '1' if the Multi-TT Interface of the hub has been enabled with a Set Interface request, otherwise '0'. Software shall issue a Set Interface request to select the Multi-TT interface of the hub prior to issuing any transactions to devices attached to the hub.
        slot_context->multi_transaction_translator = 0;
        // * Parent Port Number = The number of the downstream facing port in the parent High-speed hub that the device is accessed through.
        slot_context->parent_port_number = device.port();
        // * Parent Hub Slot ID = The Slot ID of the parent High-speed hub.
        slot_context->parent_hub_slot_id = device.hub()->controller_identifier();
    }

    //   4.  Allocate and initialize the Transfer Ring for the Default Control Endpoint.
    //   Refer to section 4.9 for TRB Ring initialization requirements and to section 6.4 for the formats of TRBs
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    slot_state.endpoint_rings[0].region = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up(endpoint_ring_size * sizeof(TransferRequestBlock))), "xHCI Endpoint Rings"sv, Memory::Region::ReadWrite, Memory::MemoryType::IO));
    auto* endpoint_ring_memory = slot_state.endpoint_rings[0].ring_vaddr();
    endpoint_ring_memory[endpoint_ring_size - 1].generic.transfer_request_block_type = TransferRequestBlock::TRBType::Link;
    auto endpoint_ring_address = slot_state.endpoint_rings[0].ring_paddr();
    endpoint_ring_memory[endpoint_ring_size - 1].link.ring_segment_pointer_low = endpoint_ring_address;
    endpoint_ring_memory[endpoint_ring_size - 1].link.ring_segment_pointer_high = endpoint_ring_address >> 32;
    endpoint_ring_memory[endpoint_ring_size - 1].link.toggle_cycle = 1;

    //   5. Initialize the Input default control Endpoint 0 Context (6.2.3).
    auto* endpoint_context = input_endpoint_context(slot, 0, Pipe::Direction::Bidirectional);
    //   * EP Type = Control.
    endpoint_context->endpoint_type = EndpointContext::EndpointType::Control_Bidirectional;
    //   * Max Packet Size = The default maximum packet size for the Default Control Endpoint, as function of the PORTSC Port Speed field.
    u16 default_max_packet_size;
    switch (speed) {
    case USB::Device::DeviceSpeed::LowSpeed:
    case USB::Device::DeviceSpeed::FullSpeed:
        default_max_packet_size = 8;
        break;
    case USB::Device::DeviceSpeed::HighSpeed:
        default_max_packet_size = 64;
        break;
    case USB::Device::DeviceSpeed::SuperSpeed:
        default_max_packet_size = 512;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    endpoint_context->max_packet_size = default_max_packet_size;
    //   * Max Burst Size = 0.
    endpoint_context->max_burst_size = 0;
    //   * TR Dequeue Pointer = Start address of first segment of the Default Control Endpoint Transfer Ring.
    endpoint_context->transfer_ring_dequeue_pointer_low = endpoint_ring_address >> 4;
    endpoint_context->transfer_ring_dequeue_pointer_high = endpoint_ring_address >> 32;
    //   * Dequeue Cycle State (DCS) = 1. Reflects Cycle bit state for valid TRBs written by software.
    endpoint_context->dequeue_cycle_state = 1;
    //   * Interval = 0.
    endpoint_context->interval = 0;
    //   * Max Primary Streams (MaxPStreams) = 0.
    endpoint_context->max_primary_streams = 0;
    //   * Mult = 0.
    endpoint_context->mult = 0;
    //   * Error Count (CErr) = 3.
    endpoint_context->error_count = 3;
    //   "Reasonable initial values of Average TRB Length for Control endpoints would be 8B"
    endpoint_context->average_transfer_request_block = 8;

    //   6. Allocate the Output Device Context data structure (6.2.1) and initialize it to ‘0’.
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    slot_state.device_context_region = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up(device_context_size())), "xHCI Device Context"sv, Memory::Region::ReadWrite, Memory::MemoryType::IO));

    //   7. Load the appropriate (Device Slot ID) entry in the Device Context Base Address Array (5.4.6) with a pointer to the Output Device Context data structure (6.2.1).
    m_device_context_base_address_array[slot] = slot_state.device_context_region->physical_page(0)->paddr().get();

    // 6. Once the slot related data structures are initialized, system software shall use an Address Device Command to assign an address to the device and enable its Default Control Endpoint,
    // as described in section 4.3.4.
    auto input_context_address = slot_state.input_context_region->physical_page(0)->paddr().get();
    TRY(address_device(slot, input_context_address));
    auto new_address = device_slot_context(slot)->usb_device_address + 1; // We add 1 to the address since we use 1 as the fake address for the root hub
    device.set_address<xHCIController>({}, new_address);
    dbgln_if(USB_DEBUG, "USB Device: Set address to {}", new_address);

    // 7. For LS, HS, and SS devices; 8, 64, and 512 bytes, respectively, are the only packet sizes allowed for the Default Control Endpoint, so this step may be skipped.
    // For FS devices, system software should initially read the first 8 bytes of the USB Device Descriptor to retrieve the value of the bMaxPacketSize0 field and determine the actual Max Packet Size for the Default Control Endpoint,
    // by issuing a USB GET_DESCRIPTOR request to the device, update the Default Control Endpoint Context with the actual Max Packet Size and inform the xHC of the context change.
    constexpr u8 short_device_descriptor_length = 8;
    USBDeviceDescriptor dev_descriptor {};
    auto transfer_length = TRY(device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_DEVICE << 8), 0, short_device_descriptor_length, &dev_descriptor));
    if (transfer_length < short_device_descriptor_length) {
        dmesgln_xhci("USB Device did not return enough bytes for short device descriptor - Expected {} but got {}", short_device_descriptor_length, transfer_length);
        return EIO;
    }
    VERIFY(dev_descriptor.descriptor_header.descriptor_type == DESCRIPTOR_TYPE_DEVICE);
    device.set_max_packet_size<xHCIController>({}, dev_descriptor.max_packet_size);
    if (speed == USB::Device::DeviceSpeed::FullSpeed && dev_descriptor.max_packet_size != 8) {
        control_context->drop_contexts = 0;
        control_context->add_contexts = (1 << 1);
        endpoint_context->max_packet_size = dev_descriptor.max_packet_size;
        TRY(evaluate_context(slot, input_context_address));
    }

    // 8. Now that the Default Control Endpoint is fully operational, system software may read the complete USB Device Descriptor and possibly the Configuration Descriptors so that it can hand the device off to the appropriate Class Driver(s).
    transfer_length = TRY(device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_DEVICE << 8), 0, sizeof(USBDeviceDescriptor), &dev_descriptor));
    if (transfer_length < sizeof(USBDeviceDescriptor)) {
        dmesgln_xhci("USB Device did not return enough bytes for device descriptor - Expected {} but got {}", sizeof(USBDeviceDescriptor), transfer_length);
        return EIO;
    }
    VERIFY(dev_descriptor.descriptor_header.descriptor_type == DESCRIPTOR_TYPE_DEVICE);
    device.set_descriptor<xHCIController>({}, dev_descriptor);

    // If the device is a hub:
    if (dev_descriptor.device_class == USB_CLASS_HUB) {
        USBHubDescriptor hub_descriptor {};
        transfer_length = TRY(device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST | USB_REQUEST_TYPE_CLASS, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_HUB << 8), 0, sizeof(USBHubDescriptor), &hub_descriptor));
        if (transfer_length < sizeof(USBHubDescriptor)) {
            dmesgln_xhci("USB Device did not return enough bytes for hub descriptor - Expected {} but got {}", sizeof(USBHubDescriptor), transfer_length);
            return EIO;
        }
        control_context->drop_contexts = 0;
        control_context->add_contexts = (1 << 0);
        // * Hub = ‘1’.
        slot_context->hub = 1;
        // * Number of Ports = bNbrPorts from the USB Hub Descriptor.
        slot_context->number_of_ports = hub_descriptor.number_of_downstream_ports;
        // * If the device Speed = High-Speed (‘3’):
        if (speed == USB::Device::DeviceSpeed::HighSpeed) {
            // * TT Think Time (TTT) = Value of the TT Think Time sub-field (USB2 spec, Table 11-13) in the Hub Descriptor:wHubCharacteristics field.
            slot_context->transaction_translator_think_time = hub_descriptor.hub_characteristics.usb2.transaction_translator_think_time;
            // * Multi-TT (MTT) = '1' if the Multi-TT Interface of the hub has been enabled with a Set Interface request, otherwise '0'.
            slot_context->multi_transaction_translator = 0;
        }
        TRY(evaluate_context(slot, input_context_address));
    }

    // Fetch the configuration descriptors from the device
    auto& configurations = device.configurations<xHCIController>({});
    configurations.ensure_capacity(dev_descriptor.num_configurations);
    for (u8 configuration = 0u; configuration < dev_descriptor.num_configurations; configuration++) {
        USBConfigurationDescriptor configuration_descriptor;
        transfer_length = TRY(device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_CONFIGURATION << 8u) | configuration, 0, sizeof(USBConfigurationDescriptor), &configuration_descriptor));
        if (transfer_length < sizeof(USBConfigurationDescriptor)) {
            dbgln_if(XHCI_DEBUG, "xHCI: Did not receive enough bytes for configuration descriptor - Expected {} but got {}", sizeof(USBConfigurationDescriptor), transfer_length);
            continue;
        }

        TRY(configurations.try_empend(device, configuration_descriptor, configuration));
        TRY(configurations.last().enumerate_interfaces());
    }

    return {};
}

void xHCIController::cancel_async_transfer(NonnullLockRefPtr<Transfer>)
{
    TODO();
}

ErrorOr<void> xHCIController::enqueue_transfer(u8 slot, u8 endpoint, Pipe::Direction direction, Span<TransferRequestBlock> transfer_request_blocks, PendingTransfer& pending_transfer)
{
    VERIFY(transfer_request_blocks.size() > 0);
    VERIFY(transfer_request_blocks.size() < endpoint_ring_size);
    SpinlockLocker const locker(m_slots_state[slot - 1].lock);

    auto& endpoint_ring = m_slots_state[slot - 1].endpoint_rings[endpoint_index(endpoint, direction) - 1];
    VERIFY(endpoint_ring.region);
    if (transfer_request_blocks.size() > endpoint_ring.free_transfer_request_blocks)
        return ENOBUFS;
    endpoint_ring.free_transfer_request_blocks -= transfer_request_blocks.size();

    auto* ring_memory = endpoint_ring.ring_vaddr();
    auto first_trb_index = endpoint_ring.enqueue_index;
    auto last_trb_index = 0u;
    for (auto i = 0u; i < transfer_request_blocks.size(); i++) {
        transfer_request_blocks[i].generic.cycle_bit = endpoint_ring.producer_cycle_state ^ (i == 0);
        ring_memory[endpoint_ring.enqueue_index] = transfer_request_blocks[i];

        last_trb_index = endpoint_ring.enqueue_index;
        endpoint_ring.enqueue_index++;

        if (endpoint_ring.enqueue_index == (endpoint_ring_size - 1)) {
            // Reached Link TRB, flip cycle bit and return to start
            ring_memory[endpoint_ring.enqueue_index].link.chain_bit = transfer_request_blocks[i].generic.chain_bit; // Make sure we don't interrupt a multi-TRB chain
            ring_memory[endpoint_ring.enqueue_index].link.cycle_bit = endpoint_ring.producer_cycle_state;
            endpoint_ring.enqueue_index = 0;
            endpoint_ring.producer_cycle_state ^= 1;
        }
    }

    pending_transfer.start_index = first_trb_index;
    pending_transfer.end_index = last_trb_index;
    endpoint_ring.pending_transfers.append(pending_transfer);

    atomic_thread_fence(MemoryOrder::memory_order_seq_cst);

    ring_memory[first_trb_index].generic.cycle_bit ^= 1;

    atomic_thread_fence(MemoryOrder::memory_order_seq_cst);

    ring_endpoint_doorbell(slot, endpoint, direction);

    return {};
}

ErrorOr<size_t> xHCIController::submit_control_transfer(Transfer& transfer)
{
    dbgln_if(XHCI_DEBUG, "xHCI: Received control transfer for address {}", transfer.pipe().device().address());

    // Short-circuit the root hub.
    if (transfer.pipe().device().address() == m_root_hub->device_address())
        return m_root_hub->handle_control_transfer(transfer);

    bool const direction_in = (transfer.request().request_type & USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST) == USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST;
    auto const& device = transfer.pipe().device();
    auto const slot = device.controller_identifier();

    TransferRequestBlock transfer_request_blocks[3] {};
    auto trb_index = 0u;

    auto* setup_trb = &transfer_request_blocks[trb_index++];
    memcpy(setup_trb, &transfer.request(), sizeof(USBRequestData));
    setup_trb->setup_stage.transfer_request_block_transfer_length = 8; // Always 8.
    setup_trb->setup_stage.interrupter_target = 0;
    setup_trb->setup_stage.interrupt_on_completion = 0;
    setup_trb->setup_stage.immediate_data = 1; // This bit shall be set to ‘1’ in a Setup Stage TRB
    setup_trb->setup_stage.transfer_request_block_type = TransferRequestBlock::TRBType::Setup_Stage;
    if (transfer.transfer_data_size() > 0)
        setup_trb->setup_stage.transfer_type = direction_in ? TransferRequestBlock::TransferType::IN_Data_Stage : TransferRequestBlock::TransferType::OUT_Data_Stage;
    else
        setup_trb->setup_stage.transfer_type = TransferRequestBlock::TransferType::No_Data_Stage;

    if (transfer.transfer_data_size() > 0) {
        auto* data_trb = &transfer_request_blocks[trb_index++];
        auto data_buffer_paddr = transfer.buffer_physical().get() + sizeof(USBRequestData); // FIXME: This is an ugly hack in the USB subsystem that works around a UHCI-specific issue, get rid of this
        data_trb->data_stage.data_buffer_low = data_buffer_paddr;
        data_trb->data_stage.data_buffer_high = data_buffer_paddr >> 32;
        data_trb->data_stage.transfer_request_block_transfer_length = transfer.transfer_data_size();
        data_trb->data_stage.transfer_descriptor_size = 0;
        data_trb->data_stage.interrupter_target = 0;
        data_trb->data_stage.chain_bit = 0;
        data_trb->data_stage.interrupt_on_completion = 0;
        data_trb->data_stage.immediate_data = 0;
        data_trb->data_stage.transfer_request_block_type = TransferRequestBlock::TRBType::Data_Stage;
        data_trb->data_stage.direction = direction_in;
    }

    auto* status_trb = &transfer_request_blocks[trb_index++];
    status_trb->status_stage.interrupter_target = 0;
    status_trb->status_stage.chain_bit = 0;
    status_trb->status_stage.interrupt_on_completion = 1;
    status_trb->status_stage.transfer_request_block_type = TransferRequestBlock::TRBType::Status_Stage;
    status_trb->status_stage.direction = !direction_in || transfer.transfer_data_size() == 0;

    SyncPendingTransfer pending_transfer;
    TRY(enqueue_transfer(slot, 0, Pipe::Direction::Bidirectional, { transfer_request_blocks, trb_index }, pending_transfer));
    pending_transfer.wait_queue.wait_forever();
    VERIFY(!pending_transfer.endpoint_list_node.is_in_list());

    if (pending_transfer.completion_code == TransferRequestBlock::CompletionCode::Stall_Error) {
        // 4.8.3 Endpoint Context State
        // Note: A STALL detected on any stage (Setup, Data, or Status) of a Default Control
        //       Endpoint request shall transition the Endpoint Context to the Halted state. A
        //       Default Control Endpoint STALL condition is cleared by a Reset Endpoint
        //       Command which transitions the endpoint from the Halted to the Stopped state.
        //       The Default Control Endpoint shall return to the Running state when the
        //       Doorbell is rung for the next Setup Stage TD sent to the endpoint.
        //
        //       Section 8.5.3.4 of the USB2 spec and section 8.12.2.3 of the USB3 spec state of
        //       Control pipes, “Unlike the case of a functional stall, protocol stall does not
        //       indicate an error with the device.” The xHC treats a functional stall and protocol
        //       stall identically, by Halting the endpoint and requiring software to clear the
        //       condition by issuing a Reset Endpoint Command.

        // Callers of this function expect normal USB behavior, so we have to handle the xHCI quirk of
        // requiring software to clear the halt condition for control pipes here.
        if (reset_pipe(transfer.pipe().device(), transfer.pipe()).is_error())
            return EIO;

        return ESHUTDOWN;
    }

    if (pending_transfer.completion_code != TransferRequestBlock::CompletionCode::Success)
        return EINVAL;

    return transfer.transfer_data_size() - pending_transfer.remainder;
}

ErrorOr<Vector<xHCIController::TransferRequestBlock>> xHCIController::prepare_normal_transfer(Transfer& transfer)
{
    auto const& device = transfer.pipe().device();
    auto const slot = device.controller_identifier();

    u32 max_burst_payload = 0;
    {
        auto endpoint_id = endpoint_index(transfer.pipe().endpoint_number(), transfer.pipe().direction());
        SpinlockLocker locker(m_slots_state[slot - 1].lock);
        max_burst_payload = m_slots_state[slot - 1].endpoint_rings[endpoint_id - 1].max_burst_payload;
    }
    VERIFY(max_burst_payload > 0);

    u32 total_transfer_size = transfer.transfer_data_size();
    auto transfer_request_blocks_count = ceil_div(total_transfer_size, max_burst_payload);
    Vector<TransferRequestBlock> transfer_request_blocks;
    TRY(transfer_request_blocks.try_resize(transfer_request_blocks_count));

    auto offset = 0u;
    for (auto i = 0u; i < transfer_request_blocks_count; ++i) {
        auto& transfer_request_block = transfer_request_blocks[i];
        auto buffer_pointer = transfer.buffer_physical().get() + offset;
        transfer_request_block.normal.data_buffer_pointer_low = buffer_pointer;
        transfer_request_block.normal.data_buffer_pointer_high = buffer_pointer >> 32;

        auto remaining = total_transfer_size - offset;
        auto trb_transfer_length = remaining < max_burst_payload ? remaining : max_burst_payload;
        transfer_request_block.normal.transfer_request_block_transfer_length = trb_transfer_length;
        offset += trb_transfer_length;

        transfer_request_block.normal.transfer_descriptor_size = min(transfer_request_blocks_count - i - 1, 31);
        transfer_request_block.normal.interrupter_target = 0;

        if (i != (transfer_request_blocks_count - 1))
            transfer_request_block.normal.chain_bit = 1;
        else
            transfer_request_block.normal.interrupt_on_completion = 1;

        transfer_request_block.normal.transfer_request_block_type = TransferRequestBlock::TRBType::Normal;
    }

    return transfer_request_blocks;
}

ErrorOr<size_t> xHCIController::submit_bulk_transfer(Transfer& transfer)
{
    dbgln_if(XHCI_DEBUG, "xHCI: Received bulk transfer for address {}", transfer.pipe().device().address());

    TRY(initialize_endpoint_if_needed(transfer.pipe()));

    auto transfer_request_blocks = TRY(prepare_normal_transfer(transfer));

    SyncPendingTransfer pending_transfer;
    TRY(enqueue_transfer(transfer.pipe().device().controller_identifier(), transfer.pipe().endpoint_number(), transfer.pipe().direction(), transfer_request_blocks, pending_transfer));
    pending_transfer.wait_queue.wait_forever();
    VERIFY(!pending_transfer.endpoint_list_node.is_in_list());

    if (pending_transfer.completion_code == TransferRequestBlock::CompletionCode::Stall_Error)
        return ESHUTDOWN;

    if (pending_transfer.completion_code != TransferRequestBlock::CompletionCode::Success
        && pending_transfer.completion_code != TransferRequestBlock::CompletionCode::Short_Packet)
        return EIO;

    return transfer.transfer_data_size() - pending_transfer.remainder;
}

ErrorOr<void> xHCIController::submit_async_interrupt_transfer(NonnullLockRefPtr<Transfer> transfer, u16)
{
    dbgln_if(XHCI_DEBUG, "xHCI: Received async interrupt transfer for address {}", transfer->pipe().device().address());

    TRY(initialize_endpoint_if_needed(transfer->pipe()));

    auto transfer_request_blocks = TRY(prepare_normal_transfer(transfer));

    NonnullOwnPtr<PeriodicPendingTransfer> pending_transfer = TRY(adopt_nonnull_own_or_enomem(new (nothrow) PeriodicPendingTransfer({}, move(transfer_request_blocks), move(transfer))));
    TRY(enqueue_transfer(pending_transfer->original_transfer->pipe().device().controller_identifier(), pending_transfer->original_transfer->pipe().endpoint_number(), pending_transfer->original_transfer->pipe().direction(), pending_transfer->transfer_request_blocks, *pending_transfer));
    TRY(m_active_periodic_transfers.try_append(move(pending_transfer)));

    return {};
}

ErrorOr<void> xHCIController::reset_pipe(USB::Device& device, USB::Pipe& pipe)
{
    u8 slot = device.controller_identifier();
    u8 endpoint_id = endpoint_index(pipe.endpoint_number(), pipe.direction());

    // 3rd "Note:" in 4.6.8 Reset Endpoint

    // Reset Endpoint Command (TSP = ‘0’).
    TRY(reset_endpoint(slot, endpoint_id, TransferStatePreserve::No));

    // If the device was behind a TT and it is a Control or Bulk endpoint:
    //   * TODO: Issue a ClearFeature(CLEAR_TT_BUFFER) request to the hub.

    // If not a Control endpoint:
    //   * Issue a ClearFeature(ENDPOINT_HALT) request to device.
    if (pipe.type() != Pipe::Type::Control) {
        TRY(device.control_transfer(USB_REQUEST_TYPE_STANDARD | USB_REQUEST_RECIPIENT_ENDPOINT | USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE,
            USB_REQUEST_CLEAR_FEATURE, USB_FEATURE_ENDPOINT_HALT, pipe.endpoint_address(), 0, nullptr));
    }

    auto& slot_state = m_slots_state[slot - 1];
    auto& endpoint_ring = slot_state.endpoint_rings[endpoint_id - 1];

    // Issue a Set TR Dequeue Pointer Command, clear the endpoint state and reference the TRB to start.

    // TODO: Set the Stream ID and Stream Context Type if streams are enabled for the endpoint once we support streams.
    TRY(set_tr_dequeue_pointer(slot, endpoint_id, 0, 0, endpoint_ring.ring_paddr(), 1));

    endpoint_ring.enqueue_index = 0;
    endpoint_ring.pending_transfers.clear();
    endpoint_ring.producer_cycle_state = 1;
    endpoint_ring.free_transfer_request_blocks = endpoint_ring_size - 1; // -1 to exclude the Link TRB
    for (size_t i = 0; i < endpoint_ring_size - 1; i++)
        endpoint_ring.ring_vaddr()[i] = {};

    // Ring Doorbell to restart the pipe.
    ring_endpoint_doorbell(slot, pipe.endpoint_number(), pipe.direction());

    return {};
}

ErrorOr<void> xHCIController::initialize_endpoint_if_needed(Pipe const& pipe)
{
    VERIFY(pipe.endpoint_number() != 0); // Endpoint 0 is manually initialized during device initialization
    auto const slot = pipe.device().controller_identifier();
    auto& slot_state = m_slots_state[slot - 1];
    SpinlockLocker locker(slot_state.lock);
    VERIFY(slot_state.input_context_region);
    auto endpoint_id = endpoint_index(pipe.endpoint_number(), pipe.direction());
    auto& endpoint_ring = slot_state.endpoint_rings[endpoint_id - 1];
    if (endpoint_ring.region)
        return {}; // Already initialized

    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    endpoint_ring.region = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up(endpoint_ring_size * sizeof(TransferRequestBlock))), "xHCI Endpoint Rings"sv, Memory::Region::ReadWrite, Memory::MemoryType::IO));
    auto* endpoint_ring_memory = endpoint_ring.ring_vaddr();
    endpoint_ring_memory[endpoint_ring_size - 1].generic.transfer_request_block_type = TransferRequestBlock::TRBType::Link;
    auto endpoint_ring_address = endpoint_ring.ring_paddr();
    endpoint_ring_memory[endpoint_ring_size - 1].link.ring_segment_pointer_low = endpoint_ring_address;
    endpoint_ring_memory[endpoint_ring_size - 1].link.ring_segment_pointer_high = endpoint_ring_address >> 32;
    endpoint_ring_memory[endpoint_ring_size - 1].link.toggle_cycle = 1;

    endpoint_ring.type = pipe.type();

    auto input_context_address = slot_state.input_context_region->physical_page(0)->paddr().get();
    auto* control_context = input_control_context(slot);
    if (device_slot_context(slot)->context_entries < endpoint_id) {
        control_context->drop_contexts = 0;
        control_context->add_contexts = (1 << 0);
        input_slot_context(slot)->context_entries = endpoint_id;
        TRY(evaluate_context(slot, input_context_address));
    }

    control_context->drop_contexts = 0;
    control_context->add_contexts = (1 << 0) | (1 << endpoint_id);

    auto* endpoint_context = input_endpoint_context(slot, pipe.endpoint_number(), pipe.direction());
    switch (pipe.type()) {
    case Pipe::Type::Isochronous:
        if (pipe.direction() == Pipe::Direction::In)
            endpoint_context->endpoint_type = EndpointContext::EndpointType::Isoch_In;
        else
            endpoint_context->endpoint_type = EndpointContext::EndpointType::Isoch_Out;
        break;
    case Pipe::Type::Bulk:
        if (pipe.direction() == Pipe::Direction::In)
            endpoint_context->endpoint_type = EndpointContext::EndpointType::Bulk_In;
        else
            endpoint_context->endpoint_type = EndpointContext::EndpointType::Bulk_Out;
        break;
    case Pipe::Type::Interrupt:
        if (pipe.direction() == Pipe::Direction::In)
            endpoint_context->endpoint_type = EndpointContext::EndpointType::Interrupt_In;
        else
            endpoint_context->endpoint_type = EndpointContext::EndpointType::Interrupt_Out;
        break;
    case Pipe::Type::Control: // The control pipe is configured during device initialization
    default:
        VERIFY_NOT_REACHED();
    }

    // FIXME: We should be all three of these somehow from the SuperSpeedEndpointCompanionDescriptor/SuperSpeedPlusEndpointCompanionDescriptor for SuperSpeed/SuperSpeedPlus devices
    if (pipe.type() == Pipe::Type::Isochronous || pipe.type() == Pipe::Type::Interrupt) {
        endpoint_context->max_packet_size = pipe.max_packet_size() & 0x7FF;
        endpoint_context->max_burst_size = (pipe.max_packet_size() & 0x1800) >> 11;
    } else {
        endpoint_context->max_packet_size = pipe.max_packet_size();
        endpoint_context->max_burst_size = 0;
    }
    // The Max Burst Payload (MBP) is the number of bytes moved by a maximum sized burst, i.e. (Max Burst Size + 1) * Max Packet Size bytes.
    endpoint_ring.max_burst_payload = endpoint_context->max_packet_size * (endpoint_context->max_burst_size + 1);
    if (pipe.type() == Pipe::Type::Isochronous || pipe.type() == Pipe::Type::Interrupt) {
        endpoint_context->max_endpoint_service_time_interval_payload_low = endpoint_ring.max_burst_payload;
        endpoint_context->max_endpoint_service_time_interval_payload_high = endpoint_ring.max_burst_payload >> 16;
    }

    endpoint_context->transfer_ring_dequeue_pointer_low = endpoint_ring_address >> 4;
    endpoint_context->transfer_ring_dequeue_pointer_high = endpoint_ring_address >> 32;
    endpoint_context->dequeue_cycle_state = 1;

    if (pipe.type() == Pipe::Type::Bulk) {
        endpoint_context->interval = 0;
    } else {
        u16 base_interval = 0;
        if (pipe.type() == Pipe::Type::Isochronous) {
            TODO(); // TODO: Fetch Isoch interval once we support Isoch pipes
        } else if (pipe.type() == Pipe::Type::Interrupt) {
            if (pipe.direction() == Pipe::Direction::In)
                base_interval = static_cast<InterruptInPipe const&>(pipe).poll_interval();
            else
                base_interval = static_cast<InterruptOutPipe const&>(pipe).poll_interval();
        }
        // Table 6-12: Endpoint Type vs. Interval Calculation
        switch (pipe.device().speed()) {
        case USB::Device::DeviceSpeed::FullSpeed:
            if (pipe.type() == Pipe::Type::Isochronous) {
                endpoint_context->interval = min(max(base_interval, 1), 16) + 2;
                break;
            }
            [[fallthrough]];
        case USB::Device::DeviceSpeed::LowSpeed:
            endpoint_context->interval = count_required_bits(min(max(base_interval, 1), 255)) + 2;
            break;
        case USB::Device::DeviceSpeed::HighSpeed:
        case USB::Device::DeviceSpeed::SuperSpeed:
            endpoint_context->interval = min(max(base_interval, 1), 16) - 1;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    endpoint_context->max_primary_streams = 0;
    if (pipe.type() == Pipe::Type::Isochronous) {
        endpoint_context->mult = 0; // FIXME: We should be getting this somehow from the SuperSpeedEndpointCompanionDescriptor for SuperSpeed devices
        endpoint_context->error_count = 0;
    } else {
        endpoint_context->mult = 0;
        endpoint_context->error_count = 3;
    }

    // "Reasonable initial values of Average TRB Length for Control endpoints would be 8B, Interrupt endpoints 1KB, and Bulk and Isoch endpoints 3KB."
    switch (pipe.type()) {
    case Pipe::Type::Isochronous:
    case Pipe::Type::Bulk:
        endpoint_context->average_transfer_request_block = 3 * KiB;
        break;
    case Pipe::Type::Interrupt:
        endpoint_context->average_transfer_request_block = 1 * KiB;
        break;
    case Pipe::Type::Control:
    default:
        VERIFY_NOT_REACHED();
    }

    return configure_endpoint(slot, input_context_address);
}

ErrorOr<HubStatus> xHCIController::get_port_status(Badge<xHCIRootHub>, u8 port)
{
    dbgln_if(XHCI_DEBUG, "xHCI: get port status for port {}", port);
    if (port >= m_ports)
        return EINVAL;

    PortStatusAndControl const port_status { .raw = m_operational_registers.port_registers[port].port_status_and_control.raw };
    HubStatus hub_status {};
    if (port_status.current_connect_status)
        hub_status.status |= PORT_STATUS_CURRENT_CONNECT_STATUS;
    if (port_status.connect_status_change)
        hub_status.change |= PORT_STATUS_CONNECT_STATUS_CHANGED;
    if (port_status.port_enabled_disabled)
        hub_status.status |= PORT_STATUS_PORT_ENABLED;
    if (port_status.port_enabled_disabled_change)
        hub_status.change |= PORT_STATUS_PORT_ENABLED_CHANGED;
    if (port_status.port_reset)
        hub_status.status |= PORT_STATUS_RESET;
    if (port_status.port_reset_change)
        hub_status.change |= PORT_STATUS_RESET_CHANGED;
    if (port_status.over_current_active)
        hub_status.status |= PORT_STATUS_OVER_CURRENT;
    if (port_status.over_current_change)
        hub_status.change |= HUB_STATUS_OVER_CURRENT_CHANGED;
    if (port_status.port_power) {
        if (m_port_max_speeds[port] == USB::Device::DeviceSpeed::SuperSpeed)
            hub_status.status |= SUPERSPEED_PORT_STATUS_POWER;
        else
            hub_status.status |= PORT_STATUS_PORT_POWER;
    }
    if (m_port_max_speeds[port] != USB::Device::DeviceSpeed::SuperSpeed) {
        if (port_status.port_speed == 2)
            hub_status.status |= PORT_STATUS_LOW_SPEED_DEVICE_ATTACHED;
        else if (port_status.port_speed == 3)
            hub_status.status |= PORT_STATUS_HIGH_SPEED_DEVICE_ATTACHED;
    }
    return hub_status;
}

ErrorOr<void> xHCIController::set_port_feature(Badge<xHCIRootHub>, u8 port, HubFeatureSelector feature)
{
    dbgln_if(XHCI_DEBUG, "xHCI: set port feature {} for port {}", (u8)feature, port);
    if (port >= m_ports)
        return EINVAL;

    // The PORTSC must be read/written manually since it has RW1C/S fields which will change state given a normal read-modify-write sequence
    PortStatusAndControl port_status { .raw = m_operational_registers.port_registers[port].port_status_and_control.raw };
    // clear RW1C/S fields
    port_status.port_enabled_disabled = 0;
    port_status.port_reset = 0;
    port_status.connect_status_change = 0;
    port_status.port_enabled_disabled_change = 0;
    port_status.warm_port_reset_change = 0;
    port_status.over_current_change = 0;
    port_status.port_reset_change = 0;
    port_status.port_link_state_change = 0;
    port_status.port_config_error_change = 0;
    switch (feature) {
    case HubFeatureSelector::PORT_POWER:
        port_status.port_power = 1;
        break;
    case HubFeatureSelector::PORT_RESET:
        port_status.port_reset = 1;
        break;
    case HubFeatureSelector::PORT_SUSPEND:
        if (!m_operational_registers.port_registers[port].port_status_and_control.port_enabled_disabled     // Port disabled
            || m_operational_registers.port_registers[port].port_status_and_control.port_reset              // Port resetting
            || m_operational_registers.port_registers[port].port_status_and_control.port_link_state >= 3) { // Port is not in suspendable state
            dmesgln_xhci("Attempt to suspend port {} in non-suspendable state", port);
            return EINVAL;
        }
        port_status.port_link_state_write_strobe = 1;
        port_status.port_link_state = 3;
        break;
    default:
        dmesgln_xhci("Attempt to set unknown feature {} for port {}", (u8)feature, port);
        return EINVAL;
    }
    m_operational_registers.port_registers[port].port_status_and_control.raw = port_status.raw;

    return {};
}

ErrorOr<void> xHCIController::clear_port_feature(Badge<xHCIRootHub>, u8 port, HubFeatureSelector feature)
{
    dbgln_if(XHCI_DEBUG, "xHCI: clear port feature {} for port {}", (u8)feature, port);
    if (port >= m_ports)
        return EINVAL;

    // The PORTSC must be read/written manually since it has RW1C/S fields which will change state given a normal read-modify-write sequence
    PortStatusAndControl port_status { .raw = m_operational_registers.port_registers[port].port_status_and_control.raw };
    // clear RW1C/S fields
    port_status.port_enabled_disabled = 0;
    port_status.port_reset = 0;
    port_status.connect_status_change = 0;
    port_status.port_enabled_disabled_change = 0;
    port_status.warm_port_reset_change = 0;
    port_status.over_current_change = 0;
    port_status.port_reset_change = 0;
    port_status.port_link_state_change = 0;
    port_status.port_config_error_change = 0;
    switch (feature) {
    case HubFeatureSelector::PORT_ENABLE:
        port_status.port_enabled_disabled = 1;
        break;
    case HubFeatureSelector::PORT_SUSPEND:
        if (!m_operational_registers.port_registers[port].port_status_and_control.port_enabled_disabled     // Port disabled
            || m_operational_registers.port_registers[port].port_status_and_control.port_reset              // Port resetting
            || m_operational_registers.port_registers[port].port_status_and_control.port_link_state != 3) { // Port is not in suspended state
            dmesgln_xhci("Attempt to un-suspend port {} in non-suspended state", port);
            return EINVAL;
        }
        port_status.port_link_state_write_strobe = 1;
        port_status.port_link_state = 0;
        break;
    case HubFeatureSelector::PORT_POWER:
        port_status.port_power = 0;
        break;
    case HubFeatureSelector::C_PORT_CONNECTION:
        port_status.connect_status_change = 1;
        break;
    case HubFeatureSelector::C_PORT_RESET:
        port_status.port_reset_change = 1;
        break;
    case HubFeatureSelector::C_PORT_ENABLE:
        port_status.port_enabled_disabled_change = 1;
        break;
    case HubFeatureSelector::C_PORT_LINK_STATE:
        port_status.port_link_state_change = 1;
        break;
    case HubFeatureSelector::C_PORT_OVER_CURRENT:
        port_status.over_current_change = 1;
        break;
    default:
        dmesgln_xhci("Attempt to clear unknown feature {} for port {}", (u8)feature, port);
        return EINVAL;
    }
    m_operational_registers.port_registers[port].port_status_and_control.raw = port_status.raw;

    return {};
}

void xHCIController::handle_interrupt(u16 interrupter_id)
{
    VERIFY(interrupter_id == 0);
    // The USBSTS must be read/written manually since it has RW1C/S fields which will change state given a normal read-modify-write sequence
    USBStatus usb_status { .raw = m_operational_registers.usb_status.raw };
    m_operational_registers.usb_status.raw = usb_status.raw; // Clear pending status bits

    if (!m_using_message_signalled_interrupts) // MSI/MSI-X automatically clears the interrupt pending flag, otherwise, clear it manually
        m_runtime_registers.interrupter_registers[0].interrupter_management.interrupt_pending = 1;

    if (usb_status.host_controller_halted) {
        dmesgln_xhci("Host controller halted unexpectedly");
        return;
    }
    if (usb_status.host_system_error) {
        dmesgln_xhci("Host system error");
        return;
    }
    if (usb_status.host_controller_error) {
        dmesgln_xhci("Host controller error");
        return;
    }
    if (usb_status.event_interrupt) {
        m_event_queue.wake_all();
        return;
    }
}

StringView xHCIController::enum_to_string(TransferRequestBlock::CompletionCode completion_code)
{
    switch (completion_code) {
    case TransferRequestBlock::CompletionCode::Invalid:
        return "Invalid"sv;
    case TransferRequestBlock::CompletionCode::Success:
        return "Success"sv;
    case TransferRequestBlock::CompletionCode::Data_Buffer_Error:
        return "Data Buffer Error"sv;
    case TransferRequestBlock::CompletionCode::Babble_Detected_Error:
        return "Babble Detected Error"sv;
    case TransferRequestBlock::CompletionCode::USB_Transaction_Error:
        return "USB Transaction Error"sv;
    case TransferRequestBlock::CompletionCode::TRB_Error:
        return "TRB Error"sv;
    case TransferRequestBlock::CompletionCode::Stall_Error:
        return "Stall Error"sv;
    case TransferRequestBlock::CompletionCode::Resource_Error:
        return "Resource Error"sv;
    case TransferRequestBlock::CompletionCode::Bandwidth_Error:
        return "Bandwidth Error"sv;
    case TransferRequestBlock::CompletionCode::No_Slots_Available_Error:
        return "No Slots Available Error"sv;
    case TransferRequestBlock::CompletionCode::Invalid_Stream_Type_Error:
        return "Invalid Stream Type Error"sv;
    case TransferRequestBlock::CompletionCode::Slot_Not_Enabled_Error:
        return "Slot Not Enabled Error"sv;
    case TransferRequestBlock::CompletionCode::Endpoint_Not_Enabled_Error:
        return "Endpoint Not Enabled Error"sv;
    case TransferRequestBlock::CompletionCode::Short_Packet:
        return "Short Packet"sv;
    case TransferRequestBlock::CompletionCode::Ring_Underrun:
        return "Ring Underrun"sv;
    case TransferRequestBlock::CompletionCode::Ring_Overrun:
        return "Ring Overrun"sv;
    case TransferRequestBlock::CompletionCode::VF_Event_Ring_Full_Error:
        return "VF Event Ring Full Error"sv;
    case TransferRequestBlock::CompletionCode::Parameter_Error:
        return "Parameter Error"sv;
    case TransferRequestBlock::CompletionCode::Bandwidth_Overrun_Error:
        return "Bandwidth Overrun Error"sv;
    case TransferRequestBlock::CompletionCode::Context_State_Error:
        return "Context State Error"sv;
    case TransferRequestBlock::CompletionCode::No_Ping_Response_Error:
        return "No Ping Response Error"sv;
    case TransferRequestBlock::CompletionCode::Event_Ring_Full_Error:
        return "Event Ring Full Error"sv;
    case TransferRequestBlock::CompletionCode::Incompatible_Device_Error:
        return "Incompatible Device Error"sv;
    case TransferRequestBlock::CompletionCode::Missed_Service_Error:
        return "Missed Service Error"sv;
    case TransferRequestBlock::CompletionCode::Command_Ring_Stopped:
        return "Command Ring Stopped"sv;
    case TransferRequestBlock::CompletionCode::Command_Aborted:
        return "Command Aborted"sv;
    case TransferRequestBlock::CompletionCode::Stopped:
        return "Stopped"sv;
    case TransferRequestBlock::CompletionCode::Stopped_Length_Invalid:
        return "Stopped Length Invalid"sv;
    case TransferRequestBlock::CompletionCode::Stopped_Short_Packet:
        return "Stopped Short Packet"sv;
    case TransferRequestBlock::CompletionCode::Max_Exit_Latency_Too_Large_Error:
        return "Max Exit Latency Too Large Error"sv;
    case TransferRequestBlock::CompletionCode::Isoch_Buffer_Overrun:
        return "Isoch Buffer Overrun"sv;
    case TransferRequestBlock::CompletionCode::Event_Lost_Error:
        return "Event Lost Error"sv;
    case TransferRequestBlock::CompletionCode::Undefined_Error:
        return "Undefined Error"sv;
    case TransferRequestBlock::CompletionCode::Invalid_Stream_ID_Error:
        return "Invalid Stream ID Error"sv;
    case TransferRequestBlock::CompletionCode::Secondary_Bandwidth_Error:
        return "Secondary Bandwidth Error"sv;
    case TransferRequestBlock::CompletionCode::Split_Transaction_Error:
        return "Split Transaction Error"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

StringView xHCIController::enum_to_string(TransferRequestBlock::TRBType trb_type)
{
    switch (trb_type) {
    case TransferRequestBlock::TRBType::Normal:
        return "Normal"sv;
    case TransferRequestBlock::TRBType::Setup_Stage:
        return "Setup Stage"sv;
    case TransferRequestBlock::TRBType::Data_Stage:
        return "Data Stage"sv;
    case TransferRequestBlock::TRBType::Status_Stage:
        return "Status Stage"sv;
    case TransferRequestBlock::TRBType::Isoch:
        return "Isoch"sv;
    case TransferRequestBlock::TRBType::Link:
        return "Link"sv;
    case TransferRequestBlock::TRBType::Event_Data:
        return "Event Data"sv;
    case TransferRequestBlock::TRBType::No_Op:
        return "No Op"sv;
    case TransferRequestBlock::TRBType::Enable_Slot_Command:
        return "Enable Slot Command"sv;
    case TransferRequestBlock::TRBType::Disable_Slot_Command:
        return "Disable Slot Command"sv;
    case TransferRequestBlock::TRBType::Address_Device_Command:
        return "Address Device Command"sv;
    case TransferRequestBlock::TRBType::Configure_Endpoint_Command:
        return "Configure Endpoint Command"sv;
    case TransferRequestBlock::TRBType::Evaluate_Context_Command:
        return "Evaluate Context Command"sv;
    case TransferRequestBlock::TRBType::Reset_Endpoint_Command:
        return "Reset Endpoint Command"sv;
    case TransferRequestBlock::TRBType::Stop_Endpoint_Command:
        return "Stop Endpoint Command"sv;
    case TransferRequestBlock::TRBType::Set_TR_Dequeue_Pointer_Command:
        return "Set TR Dequeue Pointer Command"sv;
    case TransferRequestBlock::TRBType::Reset_Device_Command:
        return "Reset Device Command"sv;
    case TransferRequestBlock::TRBType::Force_Event_Command:
        return "Force Event Command"sv;
    case TransferRequestBlock::TRBType::Negotiate_Bandwidth_Command:
        return "Negotiate Bandwidth Command"sv;
    case TransferRequestBlock::TRBType::Set_Latency_Tolerance_Value_Command:
        return "Set Latency Tolerance Value Command"sv;
    case TransferRequestBlock::TRBType::Get_Port_Bandwidth_Command:
        return "Get Port Bandwidth Command"sv;
    case TransferRequestBlock::TRBType::Force_Header_Command:
        return "Force Header Command"sv;
    case TransferRequestBlock::TRBType::No_Op_Command:
        return "No Op Command"sv;
    case TransferRequestBlock::TRBType::Get_Extended_Property_Command:
        return "Get Extended Property Command"sv;
    case TransferRequestBlock::TRBType::Set_Extended_Property_Command:
        return "Set Extended Property Command"sv;
    case TransferRequestBlock::TRBType::Transfer_Event:
        return "Transfer Event"sv;
    case TransferRequestBlock::TRBType::Command_Completion_Event:
        return "Command Completion Event"sv;
    case TransferRequestBlock::TRBType::Port_Status_Change_Event:
        return "Port Status Change Event"sv;
    case TransferRequestBlock::TRBType::Bandwidth_Request_Event:
        return "Bandwidth Request Event"sv;
    case TransferRequestBlock::TRBType::Doorbell_Event:
        return "Doorbell Event"sv;
    case TransferRequestBlock::TRBType::Host_Controller_Event:
        return "Host Controller Event"sv;
    case TransferRequestBlock::TRBType::Device_Notification_Event:
        return "Device Notification Event"sv;
    case TransferRequestBlock::TRBType::Microframe_Index_Wrap_Event:
        return "Microframe Index Wrap Event"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void xHCIController::handle_transfer_event(TransferRequestBlock const& transfer_request_block)
{
    auto slot = transfer_request_block.transfer_event.slot_id;
    VERIFY(slot > 0 && slot <= m_device_slots);
    auto& slot_state = m_slots_state[slot - 1];
    SpinlockLocker const locker(slot_state.lock);

    auto endpoint = transfer_request_block.transfer_event.endpoint_id;
    VERIFY(endpoint > 0 && endpoint <= max_endpoints);
    auto& endpoint_ring = slot_state.endpoint_rings[endpoint - 1];
    VERIFY(endpoint_ring.region);

    if (transfer_request_block.transfer_event.completion_code != TransferRequestBlock::CompletionCode::Success
        && transfer_request_block.transfer_event.completion_code != TransferRequestBlock::CompletionCode::Short_Packet)
        dmesgln_xhci("Transfer error on slot {} endpoint {}: {}", slot, endpoint, enum_to_string(transfer_request_block.transfer_event.completion_code));

    VERIFY(transfer_request_block.transfer_event.event_data == 0); // The Pointer points to the interrupting TRB
    auto transfer_request_block_pointer = ((u64)transfer_request_block.transfer_event.transfer_request_block_pointer_high << 32) | transfer_request_block.transfer_event.transfer_request_block_pointer_low;
    VERIFY(transfer_request_block_pointer % sizeof(TransferRequestBlock) == 0);
    if (transfer_request_block_pointer < endpoint_ring.ring_paddr() || (transfer_request_block_pointer - endpoint_ring.ring_paddr()) > (endpoint_ring_size * sizeof(TransferRequestBlock))) {
        dmesgln_xhci("Transfer event on slot {} endpoint {} points to unknown TRB", slot, endpoint);
        return;
    }
    auto transfer_request_block_index = (transfer_request_block_pointer - endpoint_ring.ring_paddr()) / sizeof(TransferRequestBlock);
    for (auto& pending_transfer : endpoint_ring.pending_transfers) {
        auto freed_transfer_request_blocks = 0;
        if (pending_transfer.start_index <= pending_transfer.end_index) {
            if (pending_transfer.start_index > transfer_request_block_index || transfer_request_block_index > pending_transfer.end_index)
                continue;
            freed_transfer_request_blocks = pending_transfer.end_index - pending_transfer.start_index + 1;
        } else {
            if (pending_transfer.start_index > transfer_request_block_index && transfer_request_block_index > pending_transfer.end_index)
                continue;
            freed_transfer_request_blocks = (endpoint_ring_size - pending_transfer.start_index) + pending_transfer.end_index;
        }
        endpoint_ring.free_transfer_request_blocks += freed_transfer_request_blocks;
        pending_transfer.endpoint_list_node.remove();
        if (endpoint_ring.type == Pipe::Type::Control || endpoint_ring.type == Pipe::Type::Bulk) {
            auto& sync_pending_transfer = static_cast<SyncPendingTransfer&>(pending_transfer);
            sync_pending_transfer.completion_code = transfer_request_block.transfer_event.completion_code;
            sync_pending_transfer.remainder = transfer_request_block.transfer_event.transfer_request_block_transfer_length;
            atomic_thread_fence(MemoryOrder::memory_order_seq_cst);
            sync_pending_transfer.wait_queue.wake_all();
        } else {
            auto& periodic_pending_transfer = static_cast<PeriodicPendingTransfer&>(pending_transfer);
            periodic_pending_transfer.original_transfer->invoke_async_callback();
            // Reschedule the periodic transfer (NOTE: We MUST() here since a re-enqueue should never fail)
            MUST(enqueue_transfer(slot, periodic_pending_transfer.original_transfer->pipe().endpoint_number(), periodic_pending_transfer.original_transfer->pipe().direction(), periodic_pending_transfer.transfer_request_blocks, periodic_pending_transfer));
        }
        return;
    }
    dmesgln_xhci("Transfer event on slot {} endpoint {} points to unowned TRB", slot, endpoint);
}

void xHCIController::event_handling_thread()
{
    while (!Process::current().is_dying()) {
        m_event_queue.wait_forever("xHCI"sv);
        // Handle up to ring-size events each time
        for (auto i = 0u; i < event_ring_segment_size; ++i) {
            // If the Cycle bit of the Event TRB pointed to by the Event Ring Dequeue Pointer equals CCS, then the Event TRB is a valid event,
            // software processes it and advances the Event Ring Dequeue Pointer.
            if (m_event_ring_segment[m_event_ring_dequeue_index].generic.cycle_bit != m_event_ring_consumer_cycle_state)
                break;

            auto event_type = m_event_ring_segment[m_event_ring_dequeue_index].generic.transfer_request_block_type;
            switch (event_type) {
            case TransferRequestBlock::TRBType::Transfer_Event:
                handle_transfer_event(m_event_ring_segment[m_event_ring_dequeue_index]);
                break;
            case TransferRequestBlock::TRBType::Command_Completion_Event:
                // We only process a single command at a time (and the caller holds the m_command_lock throughout), so we only ever have a single
                // active command result.
                m_command_result_transfer_request_block = m_event_ring_segment[m_event_ring_dequeue_index];
                atomic_thread_fence(MemoryOrder::memory_order_seq_cst);
                m_command_completion_queue.wake_all();
                break;
            case TransferRequestBlock::TRBType::Port_Status_Change_Event:
                dbgln_if(XHCI_DEBUG, "Port status change detected by controller");
                break;
            default:
                dmesgln_xhci("Received unknown event type {} from controller", enum_to_string(event_type));
                break;
            }

            m_event_ring_dequeue_index++;

            if (m_event_ring_dequeue_index == event_ring_segment_size) {
                m_event_ring_dequeue_index = 0;
                m_event_ring_consumer_cycle_state ^= 1;
            }
        }
        auto new_event_ring_dequeue_pointer = m_event_ring_segment_pointer + (sizeof(TransferRequestBlock) * m_event_ring_dequeue_index);
        m_runtime_registers.interrupter_registers[0].event_ring_dequeue_pointer.event_ring_dequeue_pointer_low = new_event_ring_dequeue_pointer >> 4;
        m_runtime_registers.interrupter_registers[0].event_ring_dequeue_pointer.event_ring_dequeue_pointer_high = new_event_ring_dequeue_pointer >> 32;
    }
    Thread::current()->exit();
    VERIFY_NOT_REACHED();
}

void xHCIController::hot_plug_thread()
{
    while (!Process::current().is_dying()) {
        if (m_root_hub)
            m_root_hub->check_for_port_updates();

        (void)Thread::current()->sleep(Duration::from_seconds(1));
    }
    Thread::current()->exit();
    VERIFY_NOT_REACHED();
}

}
