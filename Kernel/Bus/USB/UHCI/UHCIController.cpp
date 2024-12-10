/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Find.h>
#include <AK/Platform.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/USB/UHCI/UHCIController.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Debug.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>
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
static constexpr u16 UHCI_PORTSC_CURRENT_CONNECT_STATUS = 0x0001;
static constexpr u16 UHCI_PORTSC_CONNECT_STATUS_CHANGED = 0x0002;
static constexpr u16 UHCI_PORTSC_PORT_ENABLED = 0x0004;
static constexpr u16 UHCI_PORTSC_PORT_ENABLE_CHANGED = 0x0008;
static constexpr u16 UHCI_PORTSC_LINE_STATUS = 0x0030;
static constexpr u16 UHCI_PORTSC_RESUME_DETECT = 0x40;
static constexpr u16 UHCI_PORTSC_LOW_SPEED_DEVICE = 0x0100;
static constexpr u16 UHCI_PORTSC_PORT_RESET = 0x0200;
static constexpr u16 UHCI_PORTSC_SUSPEND = 0x1000;
static constexpr u16 UHCI_PORTSC_NON_WRITE_CLEAR_BIT_MASK = 0x1FF5; // This is used to mask out the Write Clear bits making sure we don't accidentally clear them.

// *BSD and a few other drivers seem to use this number
static constexpr u8 UHCI_NUMBER_OF_ISOCHRONOUS_TDS = 128;
static constexpr u16 UHCI_NUMBER_OF_FRAMES = 1024;

ErrorOr<NonnullLockRefPtr<UHCIController>> UHCIController::try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    // NOTE: This assumes that address is pointing to a valid UHCI controller.
    auto registers_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR4));
    auto controller = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) UHCIController(pci_device_identifier, move(registers_io_window))));
    TRY(controller->initialize());
    return controller;
}

ErrorOr<void> UHCIController::initialize()
{
    dmesgln_pci(*this, "Controller found {} @ {}", PCI::get_hardware_id(device_identifier()), device_identifier().address());
    dmesgln_pci(*this, "I/O base {}", m_registers_io_window);
    dmesgln_pci(*this, "Interrupt line: {}", interrupt_number());

    TRY(spawn_async_poll_process());
    TRY(spawn_port_process());

    TRY(reset());
    return start();
}

UNMAP_AFTER_INIT UHCIController::UHCIController(PCI::DeviceIdentifier const& pci_device_identifier, NonnullOwnPtr<IOWindow> registers_io_window)
    : PCI::Device(const_cast<PCI::DeviceIdentifier&>(pci_device_identifier))
    , IRQHandler(pci_device_identifier.interrupt_line().value())
    , m_registers_io_window(move(registers_io_window))
{
}

UNMAP_AFTER_INIT UHCIController::~UHCIController() = default;

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
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    m_framelist = TRY(MM.allocate_dma_buffer_page("UHCI Framelist"sv, Memory::Region::Access::Write, Memory::MemoryType::IO));
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

    // Doesn't do anything other than give interrupt transfer queues something to set as prev QH so that we don't have to handle that as an extra edge case
    m_schedule_begin_anchor = allocate_queue_head();

    // Create the Interrupt, Full Speed, Low Speed Control and Bulk Queue Heads
    m_interrupt_qh_anchor = allocate_queue_head();
    m_ls_control_qh_anchor = allocate_queue_head();
    m_fs_control_qh_anchor = allocate_queue_head();
    m_bulk_qh_anchor = allocate_queue_head();

    // Now the Transfer Descriptor pool
    m_transfer_descriptor_pool = TRY(UHCIDescriptorPool<TransferDescriptor>::try_create("Transfer Descriptor Pool"sv));

    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    m_isochronous_transfer_pool = TRY(MM.allocate_dma_buffer_page("UHCI Isochronous Descriptor Pool"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));

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
    m_schedule_begin_anchor->link_next_queue_head(m_interrupt_qh_anchor);
    m_schedule_begin_anchor->terminate_element_link_ptr();

    m_interrupt_qh_anchor->link_next_queue_head(m_ls_control_qh_anchor);
    m_interrupt_qh_anchor->terminate_element_link_ptr();

    m_ls_control_qh_anchor->link_next_queue_head(m_fs_control_qh_anchor);
    m_ls_control_qh_anchor->terminate_element_link_ptr();

    m_fs_control_qh_anchor->link_next_queue_head(m_bulk_qh_anchor);
    m_fs_control_qh_anchor->terminate_element_link_ptr();

    auto piix4_td_hack = allocate_transfer_descriptor();
    piix4_td_hack->terminate();
    piix4_td_hack->set_max_len(0x7ff); // Null data packet
    piix4_td_hack->set_device_address(0x7f);
    piix4_td_hack->set_packet_id(PacketID::IN);
    m_bulk_qh_anchor->link_next_queue_head(m_fs_control_qh_anchor);
    m_bulk_qh_anchor->attach_transfer_descriptor_chain(piix4_td_hack);

    u32* framelist = reinterpret_cast<u32*>(m_framelist->vaddr().as_ptr());
    for (int frame_num = 0; frame_num < UHCI_NUMBER_OF_FRAMES; frame_num++) {
        auto& frame_iso_td = m_iso_td_list.at(frame_num % UHCI_NUMBER_OF_ISOCHRONOUS_TDS);
        frame_iso_td->link_queue_head(m_schedule_begin_anchor->paddr());
        framelist[frame_num] = frame_iso_td->paddr();
    }

    m_interrupt_qh_anchor->print();
    m_ls_control_qh_anchor->print();
    m_fs_control_qh_anchor->print();
    m_bulk_qh_anchor->print();
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

u8 UHCIController::allocate_address()
{
    // FIXME: This can be smarter.
    return m_next_device_index++;
}

ErrorOr<void> UHCIController::initialize_device(USB::Device& device)
{
    USBDeviceDescriptor dev_descriptor {};

    // Send 8-bytes to get at least the `max_packet_size` from the device
    constexpr u8 short_device_descriptor_length = 8;
    auto transfer_length = TRY(device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_DEVICE << 8), 0, short_device_descriptor_length, &dev_descriptor));

    // FIXME: This be "not equal to" instead of "less than", but control transfers report a higher transfer length than expected.
    if (transfer_length < short_device_descriptor_length) {
        dbgln("USB Device: Not enough bytes for short device descriptor. Expected {}, got {}.", short_device_descriptor_length, transfer_length);
        return EIO;
    }

    if constexpr (UHCI_DEBUG) {
        dbgln("USB Short Device Descriptor:");
        dbgln("Descriptor length: {}", dev_descriptor.descriptor_header.length);
        dbgln("Descriptor type: {}", dev_descriptor.descriptor_header.descriptor_type);

        dbgln("Device Class: {:02x}", dev_descriptor.device_class);
        dbgln("Device Sub-Class: {:02x}", dev_descriptor.device_sub_class);
        dbgln("Device Protocol: {:02x}", dev_descriptor.device_protocol);
        dbgln("Max Packet Size: {:02x} bytes", dev_descriptor.max_packet_size);
    }

    // Ensure that this is actually a valid device descriptor...
    VERIFY(dev_descriptor.descriptor_header.descriptor_type == DESCRIPTOR_TYPE_DEVICE);
    device.set_max_packet_size<UHCIController>({}, dev_descriptor.max_packet_size);

    transfer_length = TRY(device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_DEVICE << 8), 0, sizeof(USBDeviceDescriptor), &dev_descriptor));

    // FIXME: This be "not equal to" instead of "less than", but control transfers report a higher transfer length than expected.
    if (transfer_length < sizeof(USBDeviceDescriptor)) {
        dbgln("USB Device: Unexpected device descriptor length. Expected {}, got {}.", sizeof(USBDeviceDescriptor), transfer_length);
        return EIO;
    }

    // Ensure that this is actually a valid device descriptor...
    VERIFY(dev_descriptor.descriptor_header.descriptor_type == DESCRIPTOR_TYPE_DEVICE);

    if constexpr (UHCI_DEBUG) {
        dbgln("USB Device Descriptor for {:04x}:{:04x}", dev_descriptor.vendor_id, dev_descriptor.product_id);
        dbgln("Device Class: {:02x}", dev_descriptor.device_class);
        dbgln("Device Sub-Class: {:02x}", dev_descriptor.device_sub_class);
        dbgln("Device Protocol: {:02x}", dev_descriptor.device_protocol);
        dbgln("Max Packet Size: {:02x} bytes", dev_descriptor.max_packet_size);
        dbgln("Number of configurations: {:02x}", dev_descriptor.num_configurations);
    }

    auto new_address = allocate_address();

    // Attempt to set devices address on the bus
    transfer_length = TRY(device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE, USB_REQUEST_SET_ADDRESS, new_address, 0, 0, nullptr));

    // This has to be set after we send out the "Set Address" request because it might be sent to the root hub.
    // The root hub uses the address to intercept requests to itself.
    device.set_address<UHCIController>({}, new_address);

    dbgln_if(USB_DEBUG, "USB Device: Set address to {}", new_address);

    device.set_descriptor<UHCIController>({}, dev_descriptor);

    // Fetch the configuration descriptors from the device
    auto& configurations = device.configurations<UHCIController>({});
    configurations.ensure_capacity(dev_descriptor.num_configurations);
    for (u8 configuration = 0u; configuration < dev_descriptor.num_configurations; configuration++) {
        USBConfigurationDescriptor configuration_descriptor;
        transfer_length = TRY(device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_CONFIGURATION << 8u) | configuration, 0, sizeof(USBConfigurationDescriptor), &configuration_descriptor));

        if constexpr (UHCI_DEBUG) {
            dbgln("USB Configuration Descriptor {}", configuration);
            dbgln("Total Length: {}", configuration_descriptor.total_length);
            dbgln("Number of interfaces: {}", configuration_descriptor.number_of_interfaces);
            dbgln("Configuration Value: {}", configuration_descriptor.configuration_value);
            dbgln("Attributes Bitmap: {:08b}", configuration_descriptor.attributes_bitmap);
            dbgln("Maximum Power: {}mA", configuration_descriptor.max_power_in_ma * 2u); // This value is in 2mA steps
        }

        TRY(configurations.try_empend(device, configuration_descriptor, configuration));
        TRY(configurations.last().enumerate_interfaces());
    }

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

    td->set_token((max_len << TD_TOKEN_MAXLEN_SHIFT) | ((pipe.data_toggle() ? 1 : 0) << TD_TOKEN_DATA_TOGGLE_SHIFT) | (pipe.endpoint_number() << TD_TOKEN_ENDPOINT_SHIFT) | (pipe.device().address() << TD_TOKEN_DEVICE_ADDR_SHIFT) | (static_cast<u8>(direction)));
    pipe.set_toggle(!pipe.data_toggle());

    if (pipe.type() == Pipe::Type::Isochronous) {
        td->set_isochronous();
    } else {
        if (direction == PacketID::IN) {
            td->set_short_packet_detect();
        }
    }

    // Set low-speed bit if the device connected to port is a low=speed device (probably unlikely...)
    if (pipe.device().speed() == USB::Device::DeviceSpeed::LowSpeed) {
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

void UHCIController::enqueue_qh(QueueHead* transfer_queue, QueueHead* anchor)
{
    SpinlockLocker locker(m_schedule_lock);

    auto prev_qh = anchor->prev_qh();
    prev_qh->link_next_queue_head(transfer_queue);
    transfer_queue->link_next_queue_head(anchor);
}

void UHCIController::dequeue_qh(QueueHead* transfer_queue)
{
    SpinlockLocker locker(m_schedule_lock);
    transfer_queue->prev_qh()->link_next_queue_head(transfer_queue->next_qh());
}

ErrorOr<QueueHead*> UHCIController::create_transfer_queue(Transfer& transfer)
{
    Pipe& pipe = transfer.pipe();

    // Create a new descriptor chain
    TransferDescriptor* last_data_descriptor;
    TransferDescriptor* data_descriptor_chain;
    auto buffer_address = Ptr32<u8>(transfer.buffer_physical().as_ptr());
    TRY(create_chain(pipe, transfer.pipe().direction() == Pipe::Direction::In ? PacketID::IN : PacketID::OUT, buffer_address, pipe.max_packet_size(), transfer.transfer_data_size(), &data_descriptor_chain, &last_data_descriptor));

    last_data_descriptor->terminate();

    if constexpr (UHCI_VERBOSE_DEBUG) {
        if (data_descriptor_chain) {
            dbgln("Data TD");
            data_descriptor_chain->print();
        }
    }

    QueueHead* transfer_queue = allocate_queue_head();
    if (!transfer_queue) {
        free_descriptor_chain(data_descriptor_chain);
        return ENOMEM;
    }

    transfer_queue->attach_transfer_descriptor_chain(data_descriptor_chain);
    transfer_queue->set_transfer(&transfer);

    return transfer_queue;
}

ErrorOr<void> UHCIController::submit_async_transfer(NonnullOwnPtr<AsyncTransferHandle> async_handle, QueueHead* anchor, QueueHead* transfer_queue)
{
    {
        SpinlockLocker locker { m_async_lock };
        auto iter = find_if(m_active_async_transfers.begin(), m_active_async_transfers.end(), [](auto& handle) { return handle == nullptr; });
        if (iter == m_active_async_transfers.end())
            return ENOMEM;
        *iter = move(async_handle);
    }

    enqueue_qh(transfer_queue, anchor);

    return {};
}

void UHCIController::cancel_async_transfer(NonnullLockRefPtr<Transfer> transfer)
{
    SpinlockLocker locker { m_async_lock };

    auto iter = find_if(m_active_async_transfers.begin(), m_active_async_transfers.end(), [transfer](auto& handle) { return handle != nullptr && handle->transfer.ptr() == transfer.ptr(); });
    if (iter == m_active_async_transfers.end()) {
        dbgln("Error: couldn't cancel supplied async transfer");
        return; // We can't really do anything here, so just give up
    }

    auto& transfer_queue = (*iter)->qh;
    dequeue_qh(transfer_queue);
    free_descriptor_chain(transfer_queue->get_first_td());
    transfer_queue->free();
    m_queue_head_pool->release_to_pool(transfer_queue);
    *iter = nullptr;
}

ErrorOr<size_t> UHCIController::submit_control_transfer(Transfer& transfer)
{
    Pipe& pipe = transfer.pipe(); // Short circuit the pipe related to this transfer
    bool direction_in = (transfer.request().request_type & USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST) == USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST;

    dbgln_if(UHCI_DEBUG, "UHCI: Received control transfer for address {}. Root Hub is at address {}.", pipe.device().address(), m_root_hub->device_address());

    // Short-circuit the root hub.
    if (pipe.device().address() == m_root_hub->device_address())
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
        return ENOMEM;
    }

    transfer_queue->attach_transfer_descriptor_chain(setup_td);
    transfer_queue->set_transfer(&transfer);

    enqueue_qh(transfer_queue, m_fs_control_qh_anchor);

    size_t transfer_size = 0;
    while (!transfer.complete()) {
        dbgln_if(USB_DEBUG, "Control transfer size: {}", transfer_size);
        transfer_size = poll_transfer_queue(*transfer_queue);
    }

    dequeue_qh(transfer_queue);
    free_descriptor_chain(transfer_queue->get_first_td());
    transfer_queue->free();
    m_queue_head_pool->release_to_pool(transfer_queue);

    return transfer_size;
}

ErrorOr<size_t> UHCIController::submit_bulk_transfer(Transfer& transfer)
{
    auto transfer_queue = TRY(create_transfer_queue(transfer));
    enqueue_qh(transfer_queue, m_bulk_qh_anchor);

    dbgln_if(UHCI_DEBUG, "UHCI: Received bulk transfer for address {}. Root Hub is at address {}.", transfer.pipe().device().address(), m_root_hub->device_address());

    size_t transfer_size = 0;
    while (!transfer.complete()) {
        transfer_size = poll_transfer_queue(*transfer_queue);
        dbgln_if(USB_DEBUG, "Bulk transfer size: {}", transfer_size);
    }

    dequeue_qh(transfer_queue);
    free_descriptor_chain(transfer_queue->get_first_td());
    transfer_queue->free();
    m_queue_head_pool->release_to_pool(transfer_queue);

    return transfer_size;
}

ErrorOr<void> UHCIController::submit_async_interrupt_transfer(NonnullLockRefPtr<Transfer> transfer, u16 ms_interval)
{
    dbgln_if(UHCI_DEBUG, "UHCI: Received interrupt transfer for address {}. Root Hub is at address {}.", transfer->pipe().device().address(), m_root_hub->device_address());

    if (ms_interval == 0) {
        return EINVAL;
    }

    auto transfer_queue = TRY(create_transfer_queue(*transfer));
    auto async_transfer_handle = TRY(adopt_nonnull_own_or_enomem(new (nothrow) AsyncTransferHandle { transfer, transfer_queue, ms_interval }));
    TRY(submit_async_transfer(move(async_transfer_handle), m_interrupt_qh_anchor, transfer_queue));

    return {};
}

size_t UHCIController::poll_transfer_queue(QueueHead& transfer_queue)
{
    Transfer* transfer = transfer_queue.transfer();
    TransferDescriptor* descriptor = transfer_queue.get_first_td();
    bool transfer_still_in_progress = false;
    size_t transfer_size = 0;

    while (descriptor) {
        u32 status = descriptor->status();

        if (status & TransferDescriptor::StatusBits::NAKReceived) {
            transfer_still_in_progress = false;
            break;
        }

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

ErrorOr<void> UHCIController::spawn_port_process()
{
    TRY(Process::create_kernel_process("UHCI Hot Plug Task"sv, [&] {
        while (!Process::current().is_dying()) {
            if (m_root_hub)
                m_root_hub->check_for_port_updates();

            (void)Thread::current()->sleep(Duration::from_seconds(1));
        }
        Process::current().sys$exit(0);
        VERIFY_NOT_REACHED();
    }));
    return {};
}

ErrorOr<void> UHCIController::spawn_async_poll_process()
{
    TRY(Process::create_kernel_process("UHCI Async Poll Task"sv, [&] {
        u16 poll_interval_ms = 1024;
        while (!Process::current().is_dying()) {
            {
                SpinlockLocker locker { m_async_lock };
                for (OwnPtr<AsyncTransferHandle>& handle : m_active_async_transfers) {
                    if (handle != nullptr) {
                        poll_interval_ms = min(poll_interval_ms, handle->ms_poll_interval);
                        QueueHead* qh = handle->qh;
                        for (auto td = qh->get_first_td(); td != nullptr && !td->active(); td = td->next_td()) {
                            if (td->next_td() == nullptr) { // Finished QH
                                handle->transfer->invoke_async_callback();
                                qh->reinitialize(); // Set the QH to be active again
                            }
                        }
                    }
                }
            }
            (void)Thread::current()->sleep(Duration::from_milliseconds(poll_interval_ms));
        }
        Process::current().sys$exit(0);
        VERIFY_NOT_REACHED();
    }));
    return {};
}

bool UHCIController::handle_irq()
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

    if (status & UHCI_PORTSC_CURRENT_CONNECT_STATUS)
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

    dbgln_if(UHCI_DEBUG, "UHCI: get_port_status status={:#04x} change={:#04x}", hub_port_status.status, hub_port_status.change);
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
    port_data &= UHCI_PORTSC_NON_WRITE_CLEAR_BIT_MASK;
    port_data |= UHCI_PORTSC_PORT_RESET;
    if (port == 0)
        write_portsc1(port_data);
    else
        write_portsc2(port_data);

    // Wait at least 50 ms for the port to reset.
    // This is T DRSTR in the USB 2.0 Specification Page 186 Table 7-13.
    constexpr u16 reset_delay = 50 * 1000;
    microseconds_delay(reset_delay);

    port_data &= ~UHCI_PORTSC_PORT_RESET;
    if (port == 0)
        write_portsc1(port_data);
    else
        write_portsc2(port_data);

    // Wait 10 ms for the port to recover.
    // This is T RSTRCY in the USB 2.0 Specification Page 188 Table 7-14.
    constexpr u16 reset_recovery_delay = 10 * 1000;
    microseconds_delay(reset_recovery_delay);

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
        port_data &= UHCI_PORTSC_NON_WRITE_CLEAR_BIT_MASK;
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
    port_data &= UHCI_PORTSC_NON_WRITE_CLEAR_BIT_MASK;

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

    dbgln_if(UHCI_DEBUG, "UHCI: clear_port_feature: writing {:#04x} to portsc{}.", port_data, port + 1);

    if (port == 0)
        write_portsc1(port_data);
    else
        write_portsc2(port_data);

    return {};
}

}
