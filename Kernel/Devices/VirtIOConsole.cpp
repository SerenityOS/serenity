/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <Kernel/Devices/VirtIOConsole.h>

namespace Kernel {

#define VIRTIO_CONSOLE_F_SIZE (1 << 0)
#define VIRTIO_CONSOLE_F_MULTIPORT (1 << 1)
#define VIRTIO_CONSOLE_F_EMERG_WRITE (1 << 2)

struct VirtIOConsoleConfig
{
    u16 cols;
    u16 rows;
    u32 max_nr_ports;
    u32 emerg_wr;
};

void VirtIOConsole::detect()
{
    static const PCI::ID virtio_serial_id = { 0x1af4, 0x1003 };
    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        if (address.is_null())
            return;
        if (id != virtio_serial_id)
            return;
        u8 irq = PCI::get_interrupt_line(address);
        (void)adopt(*new VirtIOConsole(address, irq)).leak_ref();
    });
}

VirtIOConsole::VirtIOConsole(PCI::Address address, u8 irq)
    : CharacterDevice(1003, 0)
    , VirtIODevice(address, irq, "VirtIOConsole")
{
    if (auto* cfg = get_device_config()) {
        bool success = negotiate_features([&](u32 supported_features) {
            u32 negotiated = 0;
            if (is_feature_set(supported_features, VIRTIO_CONSOLE_F_SIZE))
                klog() << "VirtIOConsole: Console size is not yet supported!";
            if (is_feature_set(supported_features, VIRTIO_CONSOLE_F_MULTIPORT))
                negotiated |= VIRTIO_CONSOLE_F_MULTIPORT;
            return negotiated;
        });
        if (success) {
            u32 max_nr_ports = 0;
            u16 cols = 0, rows = 0;
            bool have_cols_rows = is_feature_accepted(VIRTIO_CONSOLE_F_SIZE);
            if (is_feature_accepted(VIRTIO_CONSOLE_F_MULTIPORT)) {
                read_config_atomic([&]() {
                    if (have_cols_rows) {
                        cols = config_read16(cfg, 0x0);
                        rows = config_read16(cfg, 0x2);
                    }
                    max_nr_ports = config_read32(cfg, 0x4);
                });
            }
            klog() << "VirtIOConsole: cols: " << cols << " rows: " << rows << " max nr ports: " << max_nr_ports;
            success = finish_init();
        }
        if (success) {
            m_receive_queue = get_queue(0);
            m_send_queue = get_queue(1);
        }
    }
}

VirtIOConsole::~VirtIOConsole()
{
}

void VirtIOConsole::handle_irq(const RegisterState&)
{
    klog() << "VirtIOConsole: handle_irq";
}

bool VirtIOConsole::can_read(const FileDescription&, size_t) const
{
    //return (get_line_status() & DataReady) != 0;
    return false;
}

KResultOr<size_t> VirtIOConsole::read(FileDescription&, size_t, u8* buffer, size_t size)
{
    if (!size)
        return 0;

    //if (!(get_line_status() & DataReady))
    //    return 0;

    //buffer[0] = IO::in8(m_base_addr);
    (void)buffer;

    return 1;
}

bool VirtIOConsole::can_write(const FileDescription&, size_t) const
{
    //return (get_line_status() & EmptyTransmitterHoldingRegister) != 0;
    return false;
}

KResultOr<size_t> VirtIOConsole::write(FileDescription&, size_t, const u8* buffer, size_t size)
{
    if (!size)
        return 0;

    //if (!(get_line_status() & EmptyTransmitterHoldingRegister))
    //    return 0;

    //IO::out8(m_base_addr, buffer[0]);
    (void)buffer;

    return 1;
}

}
