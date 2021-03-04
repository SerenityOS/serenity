/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Jesse Buhagiar <jooster669@gmail.com>
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

#include <AK/Platform.h>

// FIXME: This should not be i386-specific.
#if ARCH(I386)

#    include <Kernel/Debug.h>
#    include <Kernel/Devices/USB/UHCIController.h>
#    include <Kernel/Process.h>
#    include <Kernel/StdLib.h>
#    include <Kernel/Time/TimeManagement.h>
#    include <Kernel/VM/AnonymousVMObject.h>
#    include <Kernel/VM/MemoryManager.h>

#    define UHCI_ENABLED 1
static constexpr u8 MAXIMUM_NUMBER_OF_TDS = 128; // Upper pool limit. This consumes the second page we have allocated
static constexpr u8 MAXIMUM_NUMBER_OF_QHS = 64;

namespace Kernel::USB {

static UHCIController* s_the;

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

// *BSD and a few other drivers seem to use this number
static constexpr u8 UHCI_NUMBER_OF_ISOCHRONOUS_TDS = 128;
static constexpr u16 UHCI_NUMBER_OF_FRAMES = 1024;

UHCIController& UHCIController::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT void UHCIController::detect()
{
#    if !UHCI_ENABLED
    return;
#    endif
    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        if (address.is_null())
            return;

        if (PCI::get_class(address) == 0xc && PCI::get_subclass(address) == 0x03 && PCI::get_programming_interface(address) == 0) {
            if (!s_the)
                s_the = new UHCIController(address, id);
        }
    });
}

UNMAP_AFTER_INIT UHCIController::UHCIController(PCI::Address address, PCI::ID id)
    : PCI::Device(address)
    , m_io_base(PCI::get_BAR4(pci_address()) & ~1)
{
    dmesgln("UHCI: Controller found {} @ {}", id, address);
    dmesgln("UHCI: I/O base {}", m_io_base);
    dmesgln("UHCI: Interrupt line: {}", PCI::get_interrupt_line(pci_address()));

    reset();
    start();

    spawn_port_proc();
}

UNMAP_AFTER_INIT UHCIController::~UHCIController()
{
}

void UHCIController::reset()
{
    stop();

    write_usbcmd(UHCI_USBCMD_HOST_CONTROLLER_RESET);

    // FIXME: Timeout
    for (;;) {
        if (read_usbcmd() & UHCI_USBCMD_HOST_CONTROLLER_RESET)
            continue;
        break;
    }

    // Let's allocate the physical page for the Frame List (which is 4KiB aligned)
    auto framelist_vmobj = ContiguousVMObject::create_with_size(PAGE_SIZE);
    m_framelist = MemoryManager::the().allocate_kernel_region_with_vmobject(*framelist_vmobj, PAGE_SIZE, "UHCI Framelist", Region::Access::Write);
    klog() << "UHCI: Allocated framelist at physical address " << m_framelist->physical_page(0)->paddr();
    klog() << "UHCI: Framelist is at virtual address " << m_framelist->vaddr();
    write_sofmod(64); // 1mS frame time

    create_structures();
    setup_schedule();

    write_flbaseadd(m_framelist->physical_page(0)->paddr().get()); // Frame list (physical) address
    write_frnum(0);                                                // Set the initial frame number

    // Enable all interrupt types
    write_frnum(UHCI_USBINTR_TIMEOUT_CRC_ENABLE | UHCI_USBINTR_RESUME_INTR_ENABLE | UHCI_USBINTR_IOC_ENABLE | UHCI_USBINTR_SHORT_PACKET_INTR_ENABLE);
    klog() << "UHCI: Reset completed!";
}

UNMAP_AFTER_INIT void UHCIController::create_structures()
{
    // Let's allocate memory for botht the QH and TD pools
    // First the QH pool and all of the Interrupt QH's
    auto qh_pool_vmobject = ContiguousVMObject::create_with_size(2 * PAGE_SIZE);
    m_qh_pool = MemoryManager::the().allocate_kernel_region_with_vmobject(*qh_pool_vmobject, 2 * PAGE_SIZE, "UHCI Queue Head Pool", Region::Access::Write);
    memset(m_qh_pool->vaddr().as_ptr(), 0, 2 * PAGE_SIZE); // Zero out both pages

    // Let's populate our free qh list (so we have some we can allocate later on)
    m_free_qh_pool.resize(MAXIMUM_NUMBER_OF_TDS);
    for (size_t i = 0; i < m_free_qh_pool.size(); i++) {
        auto placement_addr = reinterpret_cast<void*>(m_qh_pool->vaddr().get() + (i * sizeof(QueueHead)));
        auto paddr = static_cast<u32>(m_qh_pool->physical_page(0)->paddr().get() + (i * sizeof(QueueHead)));
        m_free_qh_pool.at(i) = new (placement_addr) QueueHead(paddr);
    }

    // Create the Full Speed, Low Speed Control and Bulk Queue Heads
    m_interrupt_transfer_queue = allocate_queue_head();
    m_lowspeed_control_qh = allocate_queue_head();
    m_fullspeed_control_qh = allocate_queue_head();
    m_bulk_qh = allocate_queue_head();
    m_dummy_qh = allocate_queue_head();

    // Now the Transfer Descriptor pool
    auto td_pool_vmobject = ContiguousVMObject::create_with_size(2 * PAGE_SIZE);
    m_td_pool = MemoryManager::the().allocate_kernel_region_with_vmobject(*td_pool_vmobject, 2 * PAGE_SIZE, "UHCI Transfer Descriptor Pool", Region::Access::Write);
    memset(m_td_pool->vaddr().as_ptr(), 0, 2 * PAGE_SIZE);

    // Set up the Isochronous Transfer Descriptor list
    m_iso_td_list.resize(UHCI_NUMBER_OF_ISOCHRONOUS_TDS);
    for (size_t i = 0; i < m_iso_td_list.size(); i++) {
        auto placement_addr = reinterpret_cast<void*>(m_td_pool->vaddr().get() + (i * sizeof(Kernel::USB::TransferDescriptor)));
        auto paddr = static_cast<u32>(m_td_pool->physical_page(0)->paddr().get() + (i * sizeof(Kernel::USB::TransferDescriptor)));

        // Place a new Transfer Descriptor with a 1:1 in our region
        // The pointer returned by `new()` lines up exactly with the value
        // that we store in `paddr`, meaning our member functions directly
        // access the raw descriptor (that we later send to the controller)
        m_iso_td_list.at(i) = new (placement_addr) Kernel::USB::TransferDescriptor(paddr);
        auto transfer_descriptor = m_iso_td_list.at(i);
        transfer_descriptor->set_in_use(true); // Isochronous transfers are ALWAYS marked as in use (in case we somehow get allocated one...)
        transfer_descriptor->set_isochronous();
        transfer_descriptor->link_queue_head(m_interrupt_transfer_queue->paddr());

#    if UHCI_VERBOSE_DEBUG
        transfer_descriptor->print();
#    endif
    }

    m_free_td_pool.resize(MAXIMUM_NUMBER_OF_TDS);
    for (size_t i = 0; i < m_free_td_pool.size(); i++) {
        auto placement_addr = reinterpret_cast<void*>(m_td_pool->vaddr().offset(PAGE_SIZE).get() + (i * sizeof(Kernel::USB::TransferDescriptor)));
        auto paddr = static_cast<u32>(m_td_pool->physical_page(1)->paddr().get() + (i * sizeof(Kernel::USB::TransferDescriptor)));

        // Place a new Transfer Descriptor with a 1:1 in our region
        // The pointer returned by `new()` lines up exactly with the value
        // that we store in `paddr`, meaning our member functions directly
        // access the raw descriptor (that we later send to the controller)
        m_free_td_pool.at(i) = new (placement_addr) Kernel::USB::TransferDescriptor(paddr);

#    if UHCI_VERBOSE_DEBUG
        auto transfer_descriptor = m_free_td_pool.at(i);
        transfer_descriptor->print();
#    endif
    }

#    if UHCI_DEBUG
    klog() << "UHCI: Pool information:";
    klog() << "\tqh_pool: " << PhysicalAddress(m_qh_pool->physical_page(0)->paddr()) << ", length: " << m_qh_pool->range().size();
    klog() << "\ttd_pool: " << PhysicalAddress(m_td_pool->physical_page(0)->paddr()) << ", length: " << m_td_pool->range().size();
#    endif
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
        // klog() << PhysicalAddress(framelist[frame]);
    }

    m_interrupt_transfer_queue->print();
    m_lowspeed_control_qh->print();
    m_fullspeed_control_qh->print();
    m_bulk_qh->print();
    m_dummy_qh->print();
}

QueueHead* UHCIController::allocate_queue_head() const
{
    for (QueueHead* queue_head : m_free_qh_pool) {
        if (!queue_head->in_use()) {
            queue_head->set_in_use(true);
#    if UHCI_DEBUG
            klog() << "UHCI: Allocated a new Queue Head! Located @ " << VirtualAddress(queue_head) << "(" << PhysicalAddress(queue_head->paddr()) << ")";
#    endif
            return queue_head;
        }
    }

    VERIFY_NOT_REACHED(); // Let's just assert for now, this should never happen
    return nullptr;       // Huh!? We're outta queue heads!
}

TransferDescriptor* UHCIController::allocate_transfer_descriptor() const
{
    for (TransferDescriptor* transfer_descriptor : m_free_td_pool) {
        if (!transfer_descriptor->in_use()) {
            transfer_descriptor->set_in_use(true);
#    if UHCI_DEBUG
            klog() << "UHCI: Allocated a new Transfer Descriptor! Located @ " << VirtualAddress(transfer_descriptor) << "(" << PhysicalAddress(transfer_descriptor->paddr()) << ")";
#    endif
            return transfer_descriptor;
        }
    }

    VERIFY_NOT_REACHED(); // Let's just assert for now, this should never happen
    return nullptr;       // Huh?! We're outta TDs!!
}

void UHCIController::stop()
{
    write_usbcmd(read_usbcmd() & ~UHCI_USBCMD_RUN);
    // FIXME: Timeout
    for (;;) {
        if (read_usbsts() & UHCI_USBSTS_HOST_CONTROLLER_HALTED)
            break;
    }
}

void UHCIController::start()
{
    write_usbcmd(read_usbcmd() | UHCI_USBCMD_RUN);
    // FIXME: Timeout
    for (;;) {
        if (!(read_usbsts() & UHCI_USBSTS_HOST_CONTROLLER_HALTED))
            break;
    }
    klog() << "UHCI: Started!";
}

struct setup_packet {
    u8 bmRequestType;
    u8 bRequest;
    u16 wValue;
    u16 wIndex;
    u16 wLength;
};

void UHCIController::do_debug_transfer()
{
    klog() << "UHCI: Attempting a dummy transfer...";

    // Okay, let's set up the buffer so we can write some data
    auto vmobj = ContiguousVMObject::create_with_size(PAGE_SIZE);
    m_td_buffer_region = MemoryManager::the().allocate_kernel_region_with_vmobject(*vmobj, PAGE_SIZE, "UHCI Debug Data Region", Region::Access::Write);

    // We need to set up THREE Transfer descriptors here
    // 1. The SETUP packet TD
    // 2. The DATA packet
    // 3. The ACK TD that will be filled by the device
    // We can use the buffer pool provided above to do this, using nasty pointer offsets!
    auto setup_td = allocate_transfer_descriptor();
    auto data_td = allocate_transfer_descriptor();
    auto response_td = allocate_transfer_descriptor();

    dbgln("BUFFER PHYSICAL ADDRESS = {}", m_td_buffer_region->physical_page(0)->paddr());

    setup_packet* packet = reinterpret_cast<setup_packet*>(m_td_buffer_region->vaddr().as_ptr());
    packet->bmRequestType = 0x81;
    packet->bRequest = 0x06;
    packet->wValue = 0x2200;
    packet->wIndex = 0x0;
    packet->wLength = 8;

    // Let's begin....
    setup_td->set_status(0x18800000);
    setup_td->set_token(0x00E0002D);
    setup_td->set_buffer_address(m_td_buffer_region->physical_page(0)->paddr().get());

    data_td->set_status(0x18800000);
    data_td->set_token(0x00E80069);
    data_td->set_buffer_address(m_td_buffer_region->physical_page(0)->paddr().get() + 16);

    response_td->set_status(0x19800000);
    response_td->set_token(0xFFE800E1);

    setup_td->insert_next_transfer_descriptor(data_td);
    data_td->insert_next_transfer_descriptor(response_td);
    response_td->terminate();

    setup_td->print();
    data_td->print();
    response_td->print();

    // Now let's (attempt) to attach to one of the queue heads
    m_lowspeed_control_qh->attach_transfer_descriptor_chain(setup_td);
}

void UHCIController::spawn_port_proc()
{
    RefPtr<Thread> usb_hotplug_thread;

    Process::create_kernel_process(usb_hotplug_thread, "UHCIHotplug", [&] {
        for (;;) {
            for (int port = 0; port < UHCI_ROOT_PORT_COUNT; port++) {
                u16 port_data = 0;

                if (port == 1) {
                    // Let's see what's happening on port 1
                    // Current status
                    port_data = read_portsc1();
                    if (port_data & UHCI_PORTSC_CONNECT_STATUS_CHANGED) {
                        if (port_data & UHCI_PORTSC_CURRRENT_CONNECT_STATUS) {
                            dmesgln("UHCI: Device attach detected on Root Port 1!");

                            // Reset the port
                            port_data = read_portsc1();
                            write_portsc1(port_data | UHCI_PORTSC_PORT_RESET);
                            for (size_t i = 0; i < 50000; ++i)
                                IO::in8(0x80);

                            write_portsc1(port_data & ~UHCI_PORTSC_PORT_RESET);
                            for (size_t i = 0; i < 100000; ++i)
                                IO::in8(0x80);

                            write_portsc1(port_data & (~UHCI_PORTSC_PORT_ENABLE_CHANGED | ~UHCI_PORTSC_CONNECT_STATUS_CHANGED));
                        } else {
                            dmesgln("UHCI: Device detach detected on Root Port 1!");
                        }

                        port_data = read_portsc1();
                        write_portsc1(port_data | UHCI_PORTSC_PORT_ENABLED);
                        dbgln("port should be enabled now: {:#04x}\n", read_portsc1());
                        do_debug_transfer();
                    }
                } else {
                    port_data = UHCIController::the().read_portsc2();
                    if (port_data & UHCI_PORTSC_CONNECT_STATUS_CHANGED) {
                        if (port_data & UHCI_PORTSC_CURRRENT_CONNECT_STATUS) {
                            dmesgln("UHCI: Device attach detected on Root Port 2!");
                        } else {
                            dmesgln("UHCI: Device detach detected on Root Port 2!");
                        }

                        UHCIController::the().write_portsc2(
                            UHCI_PORTSC_CONNECT_STATUS_CHANGED);
                    }
                }
            }
            (void)Thread::current()->sleep(Time::from_seconds(1));
        }
    });
}

void UHCIController::handle_irq(const RegisterState&)
{
    // Shared IRQ. Not ours!
    if (!read_usbsts())
        return;

    if constexpr (UHCI_DEBUG) {
        dbgln("UHCI: Interrupt happened!");
        dbgln("Value of USBSTS: {:#04x}", read_usbsts());
    }
}

}

#endif
