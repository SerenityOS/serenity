/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/Devices/UHCIController.h>

#define UHCI_ENABLED 0

namespace Kernel {

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

void UHCIController::detect()
{
#if !UHCI_ENABLED
    return;
#endif
    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        if (address.is_null())
            return;

        if (PCI::get_class(address) == 0xc && PCI::get_subclass(address) == 0x03 && PCI::get_programming_interface(address) == 0) {
            new UHCIController(address, id);
        }
    });
}

UHCIController::UHCIController(PCI::Address address, PCI::ID id)
    : PCI::Device(address)
    , m_io_base(PCI::get_BAR4(pci_address()) & ~1)
{
    klog() << "UHCI: Controller found " << id << " @ " << address;
    klog() << "UHCI: I/O base " << m_io_base;
    klog() << "UHCI: Interrupt line: " << PCI::get_interrupt_line(pci_address());

    reset();
    start();
}

UHCIController::~UHCIController()
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

    klog() << "UHCI: Reset completed!";
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

void UHCIController::handle_irq(const RegisterState&)
{
}

}
