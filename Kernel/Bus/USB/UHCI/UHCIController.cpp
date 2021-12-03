/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/USB/UHCI/UHCIController.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Debug.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/StdLib.h>
#include <Kernel/Time/TimeManagement.h>

static constexpr u8 RETRY_COUNTER_RELOAD = 3;

namespace Kernel::USB {

static constexpr u16 UHCI_USBCMD_RUN = 0x0001;
static constexpr u16 UHCI_USBCMD_HOST_CONTROLLER_RESET = 0x0002;
static constexpr u16 UHCI_USBCMD_GLOBAL_RESET = 0x0004;
static constexpr u16 UHCI_USBCMD_ENTER_GLOBAL_SUSPEND_MODE = 0x0008;
static constexpr u16 UHCI_USBCMD_FORCE_GLOBAL_RESUME = 0x0010;
static constexpr u16 UHCI_USBCMD_SOFTWARE_DEBUG = 0x0020;
static constexpr u16 UHCI_USBCMD_CONFIGURE_FLAG = 0x0040;
static constexpr u16 UHCI_USBCMD_MAX_PACKET = 0x0080;

static constexpr u16 UHCI_USBSTS_HOST_CONTROLLER_HALTED = 0x0020;
static constexpr u16 UHCI_USBSTS_HOST_CONTROLLER_PROCESS_ERROR = 0x0010;
static constexpr u16 UHCI_USBSTS_PCI_BUS_ERROR = 0x0008;
static constexpr u16 UHCI_USBSTS_RESUME_RECEIVED = 0x0004;
static constexpr u16 UHCI_USBSTS_USB_ERROR_INTERRUPT = 0x0002;
static constexpr u16 UHCI_USBSTS_USB_INTERRUPT = 0x0001;

static constexpr u8 UHCI_USBINTR_TIMEOUT_CRC_ENABLE = 0x01;
static constexpr u8 UHCI_USBINTR_RESUME_INTR_ENABLE = 0x02;
static constexpr u8 UHCI_USBINTR_IOC_ENABLE = 0x04;
static constexpr u8 UHCI_USBINTR_SHORT_PACKET_INTR_ENABLE = 0x08;

static constexpr u16 UHCI_FRAMELIST_FRAME_COUNT = 1024; // Each entry is 4 bytes in our allocated page
static constexpr u16 UHCI_FRAMELIST_FRAME_INVALID = 0x0001;

// Port stuff
static constexpr u8 UHCI_ROOT_PORT_COUNT = 2;
static constexpr u16 UHCI_PORTSC_CURRRENT_CONNECT_STATUS = 0x0001;
static constexpr u16 UHCI_PORTSC_CONNECT_STATUS_CHANGED = 0x0002;
static constexpr u16 UHCI_PORTSC_PORT_ENABLED = 0x0004;
static constexpr u16 UHCI_PORTSC_PORT_ENABLE_CHANGED = 0x0008;
static constexpr u16 UHCI_PORTSC_LINE_STATUS = 0x0030;
static constexpr u16 UHCI_PORTSC_RESUME_DETECT = 0x40;
static constexpr u16 UHCI_PORTSC_LOW_SPEED_DEVICE = 0x0100;
static constexpr u16 UHCI_PORTSC_PORT_RESET = 0x0200;
static constexpr u16 UHCI_PORTSC_SUSPEND = 0x1000;
static constexpr u16 UCHI_PORTSC_NON_WRITE_CLEAR_BIT_MASK = 0x1FF5; // This is used to mask out the Write Clear bits making sure we don't accidentally clear them.

// *BSD and a few other drivers seem to use this number
static constexpr u8 UHCI_NUMBER_OF_ISOCHRONOUS_TDS = 128;
static constexpr u16 UHCI_NUMBER_OF_FRAMES = 1024;

ErrorOr<NonnullRefPtr<UHCIController>> UHCIController::try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    // NOTE: This assumes that address is pointing to a valid UHCI controller.
    auto controller = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) UHCIController(pci_device_identifier)));
    TRY(controller->initialize());
    return controller;
}

ErrorOr<void> UHCIController::initialize()
{
    dmesgln("UHCI: Controller found {} @ {}", PCI::get_hardware_id(pci_address()), pci_address());
    dmesgln("UHCI: I/O base {}", m_io_base);
    dmesgln("UHCI: Interrupt line: {}", interrupt_number());

    spawn_port_proc();

    TRY(reset());
    return start();
}

UNMAP_AFTER_INIT UHCIController::UHCIController(PCI::DeviceIdentifier const& pci_device_identifier)
    : PCI::Device(pci_device_identifier.address())
    , IRQHandler(pci_device_identifier.interrupt_line().value())
    , m_io_base(PCI::get_BAR4(pci_address()) & ~1)
{
}

UNMAP_AFTER_INIT UHCIController::~UHCIController()
{
}

ErrorOr<void> UHCIController::reset()
{
    TRY(stop());

    write_usbcmd(UHCI_USBCMD_HOST_CONTROLLER_RESET);

    // FIXME: Timeout
    for (;;) {
        if (read_usbcmd() & UHCI_USBCMD_HOST_CONTROLLER_RESET)
            continue;
        break;
    }

    // Let's allocate the physical page for the Frame List (which is 4KiB aligned)
    auto vmobject = TRY(Memory::AnonymousVMObject::try_create_physically_contiguous_with_size(PAGE_SIZE));

    m_framelist = TRY(MM.allocate_kernel_region_with_vmobject(move(vmobject), PAGE_SIZE, "UHCI Framelist", Memory::Region::Access::Write));
    dbgln("UHCI: Allocated framelist at physical address {}", m_framelist->physical_page(0)->paddr());
    dbgln("UHCI: Framelist is at virtual address {}", m_framelist->vaddr());
    write_sofmod(64); // 1mS frame time

    TRY(create_structures());

    setup_schedule();

    write_flbaseadd(m_framelist->physical_page(0)->paddr().get()); // Frame list (physical) address
    write_frnum(0);                                                // Set the initial frame number

    // FIXME: Work out why interrupts lock up the entire system....
    // Disable UHCI Controller from raising an IRQ
    write_usbintr(0);
    dbgln("UHCI: Reset completed");

    return {};
}

UNMAP_AFTER_INIT ErrorOr<void> UHCIController::create_structures()
{
    m_queue_head_pool = TRY(UHCIDescriptorPool<QueueHead>::try_create("Queue Head Pool"sv));

    // Create the Full Speed, Low Speed Control and Bulk Queue Heads
    m_interrupt_transfer_queue = allocate_queue_head();
    m_lowspeed_control_qh = allocate_queue_head();
    m_fullspeed_control_qh = allocate_queue_head();
    m_bulk_qh = allocate_queue_head();
    m_dummy_qh = allocate_queue_head();

    // Now the Transfer Descriptor pool
    auto td_pool_vmobject = TRY(Memory::AnonymousVMObject::try_create_physically_contiguous_with_size(PAGE_SIZE));

    m_transfer_descriptor_pool = TRY(UHCIDescriptorPool<TransferDescriptor>::try_create("Transfer Descriptor Pool"sv));

    m_isochronous_transfer_pool = TRY(MM.allocate_kernel_region_with_vmobject(move(td_pool_vmobject), PAGE_SIZE, "UHCI Isochronous Descriptor Pool", Memory::Region::Access::ReadWrite));

    // Set up the Isochronous Transfer Descriptor list
    m_iso_td_list.resize(UHCI_NUMBER_OF_ISOCHRONOUS_TDS);
    for (size_t i = 0; i < m_iso_td_list.size(); i++) {
        auto placement_addr = reinterpret_cast<void*>(m_isochronous_transfer_pool->vaddr().get() + (i * sizeof(Kernel::USB::TransferDescriptor)));
        auto paddr = static_cast<u32>(m_isochronous_transfer_pool->physical_page(0)->paddr().get() + (i * sizeof(Kernel::USB::TransferDescriptor)));

        // Place a new Transfer Descriptor with a 1:1 in our region
        // The pointer returned by `new()` lines up exactly with the value
        // that we store in `paddr`, meaning our member functions directly
        // access the raw descriptor (that we later send to the controller)
        m_iso_td_list.at(i) = new (placement_addr) Kernel::USB::TransferDescriptor(paddr);
        auto transfer_descriptor = m_iso_td_list.at(i);
        transfer_descriptor->set_in_use(true); // Isochronous transfers are ALWAYS marked as in use (in case we somehow get allocated one...)
        transfer_descriptor->set_isochronous();
        transfer_descriptor->link_queue_head(m_interrupt_transfer_queue->paddr());

        if constexpr (UHCI_VERBOSE_DEBUG)
            transfer_descriptor->print();
    }

    if constexpr (UHCI_DEBUG) {
        dbgln("UHCI: Pool information:");
        m_queue_head_pool->print_pool_information();
        m_transfer_descriptor_pool->print_pool_information();
    }

    return {};
}

UNMAP_AFTER_INIT void UHCIController::setup_schedule()
{
    //
    // https://github.com/alkber/minix3-usbsubsystem/blob/master/usb/uhci-hcd.c
    //
    // This lad probably has the best explanation as to how this is actually done. I'll try and
    // explain it here to so that there's no need for anyone to go hunting for this shit again, because
    // the USB spec and Intel explain next to nothing.
    // According to the USB spec (and the UHCI datasheet), 90% of the bandwidth should be used for
    // Isochronous and """Interrupt""" related transfers, with the rest being used for control and bulk
    // transfers.
    // That is, most of the time, the schedule is going to be executing either an Isochronous transfer
    // in our framelist, or an Interrupt transfer. The allocation in `create_structures` reflects this.
    //
    // Each frame has it's own Isochronous transfer Transfer Descriptor(s) that point to each other
    // horizontally in the list. The end of these transfers then point to the Interrupt Queue Headers,
    // in which we can attach Transfer Descriptors (related to Interrupt Transfers). These are attached
    // to the Queue Head _vertically_. We need to ensure that these are executed every 8ms, so they are inserted
    // at different points in the schedule (TODO: How do we do this?!?!). After the Interrupt Transfer Queue Heads,
    // we attach the Control Queue Heads. We need two in total, one for Low Speed devices, and one for Full Speed
    // USB devices. Finally, we attach the Bulk Transfer Queue Head.
    // Not specified in the datasheet, however, is another Queue Head with an "inactive" Transfer Descriptor. This
    // is to circumvent a bug in the silicon of the PIIX4's UHCI controller.
    // https://github.com/openbsd/src/blob/master/sys/dev/usb/uhci.c#L390
    //

    m_interrupt_transfer_queue->link_next_queue_head(m_lowspeed_control_qh);
    m_interrupt_transfer_queue->terminate_element_link_ptr();

    m_lowspeed_control_qh->link_next_queue_head(m_fullspeed_control_qh);
    m_lowspeed_control_qh->terminate_element_link_ptr();

    m_fullspeed_control_qh->link_next_queue_head(m_bulk_qh);
    m_fullspeed_control_qh->terminate_element_link_ptr();

    m_bulk_qh->link_next_queue_head(m_dummy_qh);
    m_bulk_qh->terminate_element_link_ptr();

    auto piix4_td_hack = allocate_transfer_descriptor();
    piix4_td_hack->terminate();
    piix4_td_hack->set_max_len(0x7ff); // Null data packet
    piix4_td_hack->set_device_address(0x7f);
    piix4_td_hack->set_packet_id(PacketID::IN);
    m_dummy_qh->terminate_with_stray_descriptor(piix4_td_hack);
    m_dummy_qh->terminate_element_link_ptr();

    u32* framelist = reinterpret_cast<u32*>(m_framelist->vaddr().as_ptr());
    for (int frame = 0; frame < UHCI_NUMBER_OF_FRAMES; frame++) {
        // Each frame pointer points to iso_td % NUM_ISO_TDS
        framelist[frame] = m_iso_td_list.at(frame % UHCI_NUMBER_OF_ISOCHRONOUS_TDS)->paddr();
    }

    m_interrupt_transfer_queue->print();
    m_lowspeed_control_qh->print();
    m_fullspeed_control_qh->print();
    m_bulk_qh->print();
    m_dummy_qh->print();
}

QueueHead* UHCIController::allocate_queue_head()
{
    return m_queue_head_pool->try_take_free_descriptor();
}

TransferDescriptor* UHCIController::allocate_transfer_descriptor()
{
    return m_transfer_descriptor_pool->try_take_free_descriptor();
}

ErrorOr<void> UHCIController::stop()
{
    write_usbcmd(read_usbcmd() & ~UHCI_USBCMD_RUN);
    // FIXME: Timeout
    for (;;) {
        if (read_usbsts() & UHCI_USBSTS_HOST_CONTROLLER_HALTED)
            break;
    }
    return {};
}

ErrorOr<void> UHCIController::start()
{
    write_usbcmd(read_usbcmd() | UHCI_USBCMD_RUN);
    // FIXME: Timeout
    for (;;) {
        if (!(read_usbsts() & UHCI_USBSTS_HOST_CONTROLLER_HALTED))
            break;
    }
    dbgln("UHCI: Started");

    m_root_hub = TRY(UHCIRootHub::try_create(*this));
    TRY(m_root_hub->setup({}));
    return {};
}

TransferDescriptor* UHCIController::create_transfer_descriptor(Pipe& pipe, PacketID direction, size_t data_len)
{
    TransferDescriptor* td = allocate_transfer_descriptor();
    if (td == nullptr) {
        return nullptr;
    }

    u16 max_len = (data_len > 0) ? (data_len - 1) : 0x7ff;
    VERIFY(max_len <= 0x4FF || max_len == 0x7FF); // According to the datasheet, anything in the range of 0x500 to 0x7FE are illegal

    td->set_token((max_len << TD_TOKEN_MAXLEN_SHIFT) | ((pipe.data_toggle() ? 1 : 0) << TD_TOKEN_DATA_TOGGLE_SHIFT) | (pipe.endpoint_address() << TD_TOKEN_ENDPOINT_SHIFT) | (pipe.device_address() << TD_TOKEN_DEVICE_ADDR_SHIFT) | (static_cast<u8>(direction)));
    pipe.set_toggle(!pipe.data_toggle());

    if (pipe.type() == Pipe::Type::Isochronous) {
        td->set_isochronous();
    } else {
        if (direction == PacketID::IN) {
            td->set_short_packet_detect();
        }
    }

    // Set low-speed bit if the device connected to port is a low=speed device (probably unlikely...)
    if (pipe.device_speed() == Pipe::DeviceSpeed::LowSpeed) {
        td->set_lowspeed();
    }

    td->set_active();
    td->set_error_retry_counter(RETRY_COUNTER_RELOAD);

    return td;
}

ErrorOr<void> UHCIController::create_chain(Pipe& pipe, PacketID direction, Ptr32<u8>& buffer_address, size_t max_size, size_t transfer_size, TransferDescriptor** td_chain, TransferDescriptor** last_td)
{
    // We need to create `n` transfer descriptors based on the max
    // size of each transfer (which we've learned from the device already by reading
    // its device descriptor, or 8 bytes). Each TD then has its buffer pointer
    // set to the initial buffer address + (max_size * index), where index is
    // the ID of the TD in the chain.
    size_t byte_count = 0;
    TransferDescriptor* current_td = nullptr;
    TransferDescriptor* prev_td = nullptr;
    TransferDescriptor* first_td = nullptr;

    // Keep creating transfer descriptors while we still have some data
    while (byte_count < transfer_size) {
        size_t packet_size = transfer_size - byte_count;
        if (packet_size > max_size) {
            packet_size = max_size;
        }

        current_td = create_transfer_descriptor(pipe, direction, packet_size);
        if (current_td == nullptr) {
            free_descriptor_chain(first_td);
            return ENOMEM;
        }

        if (Checked<FlatPtr>::addition_would_overflow(reinterpret_cast<FlatPtr>(&*buffer_address), byte_count))
            return EOVERFLOW;

        auto buffer_pointer = Ptr32<u8>(buffer_address + byte_count);
        current_td->set_buffer_address(buffer_pointer);
        byte_count += packet_size;

        if (prev_td != nullptr)
            prev_td->insert_next_transfer_descriptor(current_td);
        else
            first_td = current_td;

        prev_td = current_td;
    }

    *last_td = current_td;
    *td_chain = first_td;
    return {};
}

void UHCIController::free_descriptor_chain(TransferDescriptor* first_descriptor)
{
    TransferDescriptor* descriptor = first_descriptor;

    while (descriptor) {
        TransferDescriptor* next = descriptor->next_td();

        descriptor->free();
        m_transfer_descriptor_pool->release_to_pool(descriptor);
        descriptor = next;
    }
}

ErrorOr<size_t> UHCIController::submit_control_transfer(Transfer& transfer)
{
    Pipe& pipe = transfer.pipe(); // Short circuit the pipe related to this transfer
    bool direction_in = (transfer.request().request_type & USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST) == USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST;

    dbgln_if(UHCI_DEBUG, "UHCI: Received control transfer for address {}. Root Hub is at address {}.", pipe.device_address(), m_root_hub->device_address());

    // Short-circuit the root hub.
    if (pipe.device_address() == m_root_hub->device_address())
        return m_root_hub->handle_control_transfer(transfer);

    TransferDescriptor* setup_td = create_transfer_descriptor(pipe, PacketID::SETUP, sizeof(USBRequestData));
    if (!setup_td)
        return ENOMEM;

    setup_td->set_buffer_address(transfer.buffer_physical().as_ptr());

    // Create a new descriptor chain
    TransferDescriptor* last_data_descriptor;
    TransferDescriptor* data_descriptor_chain;
    auto buffer_address = Ptr32<u8>(transfer.buffer_physical().as_ptr() + sizeof(USBRequestData));
    TRY(create_chain(pipe, direction_in ? PacketID::IN : PacketID::OUT, buffer_address, pipe.max_packet_size(), transfer.transfer_data_size(), &data_descriptor_chain, &last_data_descriptor));

    // Status TD always has toggle set to 1
    pipe.set_toggle(true);

    TransferDescriptor* status_td = create_transfer_descriptor(pipe, direction_in ? PacketID::OUT : PacketID::IN, 0);
    if (!status_td) {
        free_descriptor_chain(data_descriptor_chain);
        return ENOMEM;
    }
    status_td->terminate();

    // Link transfers together
    if (data_descriptor_chain) {
        setup_td->insert_next_transfer_descriptor(data_descriptor_chain);
        last_data_descriptor->insert_next_transfer_descriptor(status_td);
    } else {
        setup_td->insert_next_transfer_descriptor(status_td);
    }

    // Cool, everything should be chained together now! Let's print it out
    if constexpr (UHCI_VERBOSE_DEBUG) {
        dbgln("Setup TD");
        setup_td->print();
        if (data_descriptor_chain) {
            dbgln("Data TD");
            data_descriptor_chain->print();
        }
        dbgln("Status TD");
        status_td->print();
    }

    QueueHead* transfer_queue = allocate_queue_head();
    if (!transfer_queue) {
        free_descriptor_chain(data_descriptor_chain);
        return 0;
    }

    transfer_queue->attach_transfer_descriptor_chain(setup_td);
    transfer_queue->set_transfer(&transfer);

    m_fullspeed_control_qh->attach_transfer_queue(*transfer_queue);

    size_t transfer_size = 0;
    while (!transfer.complete())
        transfer_size = poll_transfer_queue(*transfer_queue);

    free_descriptor_chain(transfer_queue->get_first_td());
    transfer_queue->free();
    m_queue_head_pool->release_to_pool(transfer_queue);

    return transfer_size;
}

size_t UHCIController::poll_transfer_queue(QueueHead& transfer_queue)
{
    Transfer* transfer = transfer_queue.transfer();
    TransferDescriptor* descriptor = transfer_queue.get_first_td();
    bool transfer_still_in_progress = false;
    size_t transfer_size = 0;

    while (descriptor) {
        u32 status = descriptor->status();

        if (status & TransferDescriptor::StatusBits::Active) {
            transfer_still_in_progress = true;
            break;
        }

        if (status & TransferDescriptor::StatusBits::ErrorMask) {
            transfer->set_complete();
            transfer->set_error_occurred();
            dbgln_if(UHCI_DEBUG, "UHCIController: Transfer failed! Reason: {:08x}", status);
            return 0;
        }

        transfer_size += descriptor->actual_packet_length();
        descriptor = descriptor->next_td();
    }

    if (!transfer_still_in_progress)
        transfer->set_complete();

    return transfer_size;
}

void UHCIController::spawn_port_proc()
{
    RefPtr<Thread> usb_hotplug_thread;

    auto process_name = KString::try_create("UHCI hotplug");
    if (process_name.is_error())
        TODO();

    (void)Process::create_kernel_process(usb_hotplug_thread, process_name.release_value(), [&] {
        for (;;) {
            if (m_root_hub)
                m_root_hub->check_for_port_updates();

            (void)Thread::current()->sleep(Time::from_seconds(1));
        }
    });
}

bool UHCIController::handle_irq(const RegisterState&)
{
    u32 status = read_usbsts();

    // Shared IRQ. Not ours!
    if (!status)
        return false;

    if constexpr (UHCI_DEBUG) {
        dbgln("UHCI: Interrupt happened!");
        dbgln("Value of USBSTS: {:#04x}", read_usbsts());
    }

    // Write back USBSTS to clear bits
    write_usbsts(status);
    return true;
}

void UHCIController::get_port_status(Badge<UHCIRootHub>, u8 port, HubStatus& hub_port_status)
{
    // The check is done by UHCIRootHub.
    VERIFY(port < NUMBER_OF_ROOT_PORTS);

    u16 status = port == 0 ? read_portsc1() : read_portsc2();

    if (status & UHCI_PORTSC_CURRRENT_CONNECT_STATUS)
        hub_port_status.status |= PORT_STATUS_CURRENT_CONNECT_STATUS;

    if (status & UHCI_PORTSC_CONNECT_STATUS_CHANGED)
        hub_port_status.change |= PORT_STATUS_CONNECT_STATUS_CHANGED;

    if (status & UHCI_PORTSC_PORT_ENABLED)
        hub_port_status.status |= PORT_STATUS_PORT_ENABLED;

    if (status & UHCI_PORTSC_PORT_ENABLE_CHANGED)
        hub_port_status.change |= PORT_STATUS_PORT_ENABLED_CHANGED;

    if (status & UHCI_PORTSC_LOW_SPEED_DEVICE)
        hub_port_status.status |= PORT_STATUS_LOW_SPEED_DEVICE_ATTACHED;

    if (status & UHCI_PORTSC_PORT_RESET)
        hub_port_status.status |= PORT_STATUS_RESET;

    if (m_port_reset_change_statuses & (1 << port))
        hub_port_status.change |= PORT_STATUS_RESET_CHANGED;

    if (status & UHCI_PORTSC_SUSPEND)
        hub_port_status.status |= PORT_STATUS_SUSPEND;

    if (m_port_suspend_change_statuses & (1 << port))
        hub_port_status.change |= PORT_STATUS_SUSPEND_CHANGED;

    // UHCI ports are always powered.
    hub_port_status.status |= PORT_STATUS_PORT_POWER;

    dbgln_if(UHCI_DEBUG, "UHCI: get_port_status status=0x{:04x} change=0x{:04x}", hub_port_status.status, hub_port_status.change);
}

void UHCIController::reset_port(u8 port)
{
    // We still have to reset the port manually because UHCI does not automatically enable the port after reset.
    // Additionally, the USB 2.0 specification says the SetPortFeature(PORT_ENABLE) request is not specified and that the _ideal_ behavior is to return a Request Error.
    // Source: USB 2.0 Specification Section 11.24.2.7.1.2
    // This means the hub code cannot rely on using it.

    // The check is done by UHCIRootHub and set_port_feature.
    VERIFY(port < NUMBER_OF_ROOT_PORTS);

    u16 port_data = port == 0 ? read_portsc1() : read_portsc2();
    port_data &= UCHI_PORTSC_NON_WRITE_CLEAR_BIT_MASK;
    port_data |= UHCI_PORTSC_PORT_RESET;
    if (port == 0)
        write_portsc1(port_data);
    else
        write_portsc2(port_data);

    // Wait at least 50 ms for the port to reset.
    // This is T DRSTR in the USB 2.0 Specification Page 186 Table 7-13.
    constexpr u16 reset_delay = 50 * 1000;
    IO::delay(reset_delay);

    port_data &= ~UHCI_PORTSC_PORT_RESET;
    if (port == 0)
        write_portsc1(port_data);
    else
        write_portsc2(port_data);

    // Wait 10 ms for the port to recover.
    // This is T RSTRCY in the USB 2.0 Specification Page 188 Table 7-14.
    constexpr u16 reset_recovery_delay = 10 * 1000;
    IO::delay(reset_recovery_delay);

    port_data = port == 0 ? read_portsc1() : read_portsc2();
    port_data |= UHCI_PORTSC_PORT_ENABLED;
    if (port == 0)
        write_portsc1(port_data);
    else
        write_portsc2(port_data);

    dbgln_if(UHCI_DEBUG, "UHCI: Port should be enabled now: {:#04x}", port == 0 ? read_portsc1() : read_portsc2());
    m_port_reset_change_statuses |= (1 << port);
}

ErrorOr<void> UHCIController::set_port_feature(Badge<UHCIRootHub>, u8 port, HubFeatureSelector feature_selector)
{
    // The check is done by UHCIRootHub.
    VERIFY(port < NUMBER_OF_ROOT_PORTS);

    dbgln_if(UHCI_DEBUG, "UHCI: set_port_feature: port={} feature_selector={}", port, (u8)feature_selector);

    switch (feature_selector) {
    case HubFeatureSelector::PORT_POWER:
        // Ignore the request. UHCI ports are always powered.
        break;
    case HubFeatureSelector::PORT_RESET:
        reset_port(port);
        break;
    case HubFeatureSelector::PORT_SUSPEND: {
        u16 port_data = port == 0 ? read_portsc1() : read_portsc2();
        port_data &= UCHI_PORTSC_NON_WRITE_CLEAR_BIT_MASK;
        port_data |= UHCI_PORTSC_SUSPEND;

        if (port == 0)
            write_portsc1(port_data);
        else
            write_portsc2(port_data);

        m_port_suspend_change_statuses |= (1 << port);
        break;
    }
    default:
        dbgln("UHCI: Unknown feature selector in set_port_feature: {}", (u8)feature_selector);
        return EINVAL;
    }

    return {};
}

ErrorOr<void> UHCIController::clear_port_feature(Badge<UHCIRootHub>, u8 port, HubFeatureSelector feature_selector)
{
    // The check is done by UHCIRootHub.
    VERIFY(port < NUMBER_OF_ROOT_PORTS);

    dbgln_if(UHCI_DEBUG, "UHCI: clear_port_feature: port={} feature_selector={}", port, (u8)feature_selector);

    u16 port_data = port == 0 ? read_portsc1() : read_portsc2();
    port_data &= UCHI_PORTSC_NON_WRITE_CLEAR_BIT_MASK;

    switch (feature_selector) {
    case HubFeatureSelector::PORT_ENABLE:
        port_data &= ~UHCI_PORTSC_PORT_ENABLED;
        break;
    case HubFeatureSelector::PORT_SUSPEND:
        port_data &= ~UHCI_PORTSC_SUSPEND;
        break;
    case HubFeatureSelector::PORT_POWER:
        // Ignore the request. UHCI ports are always powered.
        break;
    case HubFeatureSelector::C_PORT_CONNECTION:
        // This field is Write Clear.
        port_data |= UHCI_PORTSC_CONNECT_STATUS_CHANGED;
        break;
    case HubFeatureSelector::C_PORT_RESET:
        m_port_reset_change_statuses &= ~(1 << port);
        break;
    case HubFeatureSelector::C_PORT_ENABLE:
        // This field is Write Clear.
        port_data |= UHCI_PORTSC_PORT_ENABLE_CHANGED;
        break;
    case HubFeatureSelector::C_PORT_SUSPEND:
        m_port_suspend_change_statuses &= ~(1 << port);
        break;
    default:
        dbgln("UHCI: Unknown feature selector in clear_port_feature: {}", (u8)feature_selector);
        return EINVAL;
    }

    dbgln_if(UHCI_DEBUG, "UHCI: clear_port_feature: writing 0x{:04x} to portsc{}.", port_data, port + 1);

    if (port == 0)
        write_portsc1(port_data);
    else
        write_portsc2(port_data);

    return {};
}

}
