/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Span.h>
#include <AK/kmalloc.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Bus/USB/xHCI/Utils.h>
#include <Kernel/Bus/USB/xHCI/xHCIController.h>
#include <Kernel/Library/StdLib.h>

namespace Kernel::USB::xHCI {

xHCIController::~xHCIController()
{
    for (auto* device_context : m_device_context_base_address_array) {
        if (!device_context)
            return;
        kfree_sized(device_context,
            device_context_size(device_context->context_entries,
                m_cap_regs->capability_parameters1.context_size));
    }
    // FIXME: Maybe find a better way to call delete[] with a size
    //        (or change the allocation strategies)
    // FIXME: Should we call the aligned delete[] here?
    // Note: The `size` parameter on these is in bytes, not elements
    operator delete[](m_device_context_base_address_array.data(),
        m_device_context_base_address_array.size() * sizeof(SlotContext*));

    operator delete[](m_command_ring.data(), PAGE_SIZE);
}

ErrorOr<NonnullLockRefPtr<xHCIController>> xHCIController::try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    // FIXME:
    if (!pci_device_identifier.is_msix_capable())
        return ENOTSUP;

    PCI::enable_bus_mastering(pci_device_identifier);
    PCI::enable_memory_space(pci_device_identifier);

    // FIXME: We need to set the Frame Length Adjustment (FLADJ) PCI register,
    //        if the BIOS did not do so
    // FIXME: We rely on the BIOS' BAR addresses

    auto pci_bar_address = TRY(PCI::get_bar_address(pci_device_identifier, BaseRegister));
    auto pci_bar_space_size = PCI::get_BAR_space_size(pci_device_identifier, BaseRegister);
    auto register_region = TRY(MM.allocate_kernel_region(pci_bar_address, pci_bar_space_size, {}, Memory::Region::Access::ReadWrite));

    auto controller = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) xHCIController(pci_device_identifier, move(register_region))));

    TRY(controller->initialize());

    return controller;
}

xHCIController::xHCIController(PCI::DeviceIdentifier const& pci_device_identifier, NonnullOwnPtr<Memory::Region> register_region)
    : PCI::Device(pci_device_identifier)
    , m_register_region(move(register_region))
{
    // FIXME: Bounds checks
    m_cap_regs = bit_cast<CapabilityRegisters const volatile*>(m_register_region->vaddr().get());
    m_op_regs = bit_cast<OperationalRegisters volatile*>(m_register_region->vaddr().get() + m_cap_regs->caplength);
    m_doorbell_regs = bit_cast<DoorbellRegister volatile*>(m_register_region->vaddr().get() + m_cap_regs->doorbell_offset);
    m_runtime_regs = bit_cast<RuntimeRegisters volatile*>(m_register_region->vaddr().get() + m_cap_regs->runtime_register_space_offset);
}

ErrorOr<void> xHCIController::initialize()
{
    dmesgln_pci(*this, "Controller found {} @ {}", PCI::get_hardware_id(device_identifier()), device_identifier().address());
    dmesgln_pci(*this, "Version {}.{}", (u8)m_cap_regs->hci_version_major, (u8)m_cap_regs->hci_version_minor);
    dmesgln_pci(*this, "Ports: {}", (u8)m_cap_regs->structural_parameters1.number_of_ports);
    dmesgln_pci(*this, "Device Slots: {}", (u8)m_cap_regs->structural_parameters1.max_device_slots);
    dmesgln_pci(*this, "Interrupters: {}", (u8)m_cap_regs->structural_parameters1.max_interrupters);

    if (m_cap_regs->capability_parameters1.addressing_capability_64 == 0) {
        // FIXME:
        PCI::dmesgln_pci(*this, "Controller does not support 64 bit addressing, rejecting for now");
        return ENOTSUP;
    }

    // 4.2 Host Controller Initialization
    // * Initialize the system I/O memory maps, if supported.
    // Note: Done in try_to_initialize

    // * After Chip Hardware Reset 6 wait until the Controller Not Ready (CNR) flag
    //   in the USBSTS is ‘0’ before writing any xHC Operational or Runtime
    //   registers.
    TRY(reset());

    // * Program the Max Device Slots Enabled (MaxSlotsEn) field in th e CONFIG
    //   register (5.4.7) to enable the device slots that system software is going to
    //   use.
    // FIXME: Decide on limits here
    m_op_regs->configure.max_device_slots_enabled = m_cap_regs->structural_parameters1.max_device_slots;

    // * Program the Device Context Base Address Array Pointer (DCBAAP)
    //   register (5.4.6) with a 64-bit address pointing to where the Device
    //   Context Base Address Array is located.
    //   -> 6.1 Device Context Base Address Array
    //      * The Device Context Base Address Array shall be aligned to a 64 byte boundary.
    //      * The Device Context Base Address Array shall be physically contiguous within a page.
    //      * The Device Context Base Address Array shall contain  MaxSlotsEn + 1 entries.
    //        The maximum size of the Device Context Base Address Array is 256 64 -bit
    //        entries, or 2K Bytes.
    size_t array_size = m_cap_regs->structural_parameters1.max_device_slots + 1;
    // FIXME: To ensure we don't span a page boundary, we align up to half a page, aka the maximum size of this array
    //        We could probably choose a smaller alignment when we dont use all 2048 byte
    auto** device_context_base_address_array = new (std::align_val_t(2048), nothrow) SlotContext*[array_size];
    if (device_context_base_address_array == nullptr)
        return ENOMEM;
    m_device_context_base_address_array = { device_context_base_address_array, array_size };
    set_address(m_op_regs->device_context_array_base_pointer, device_context_base_address_array);
    //      * Software shall set Device Context Base Address Array entries for unallocated
    //        Device Slots to ‘0’.
    m_device_context_base_address_array.fill(nullptr);

    // * Define the Command Ring Dequeue Pointer by programming the
    //   Command Ring Control Register (5.4.5) with a 64 -bit address pointing to
    //   the starting address of the first TRB of the Command Ring.
    // FIXME: Figure out an appropriate size
    m_command_ring = TRY(allocate_trb_ring(PAGE_SIZE, true));
    set_address(m_op_regs->command_ring_control.addr, m_command_ring.data());
    // FIXME: I think we might need to set the dequeue/enqueue pointers here?

    // * Initialize interrupts 7 by:
    TRY(initialize_interrupts());

    // * Write the USBCMD (5.4.1) to turn the host controller ON via setting the
    //   Run/Stop (R/S) bit to ‘1’. This operation allows the xHC to begin
    //   accepting doorbell references.
    m_op_regs->usb_command.run_stop = 1;

    return {};
}

ErrorOr<void> xHCIController::initialize_interrupts()
{
    enable_extended_message_signalled_interrupts();

    auto n_interrupters = m_cap_regs->structural_parameters1.max_interrupters;
    TRY(reserve_irqs(n_interrupters, true));

    TRY(m_interrupters.try_ensure_capacity(n_interrupters));
    for (auto i = 0u; i < n_interrupters; ++i) {
        m_interrupters.unchecked_append(
            TRY(Interrupter::try_create(*this, i, TRY(allocate_irq(i)))));
    }

    m_op_regs->usb_command.interrupt_enable = 1;

    return {};
}

ErrorOr<void> xHCIController::stop()
{
    m_op_regs->usb_command.run_stop = 0;

    // FIXME: Timeout
    while (m_op_regs->usb_status.hc_halted == 0)
        Processor::wait_check();

    return {};
}

ErrorOr<void> xHCIController::reset()
{
    TRY(stop());

    m_op_regs->usb_command.host_controller_reset = 1;

    // FIXME: Timeouts
    while (m_op_regs->usb_command.host_controller_reset != 0)
        Processor::wait_check();

    while (m_op_regs->usb_status.controller_not_ready != 0)
        Processor::wait_check();

    return {};
}
}
