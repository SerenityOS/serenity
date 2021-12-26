/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/VM/RingBuffer.h>
#include <Kernel/VirtIO/VirtIO.h>

namespace Kernel {

class VirtIOConsole;

#define VIRTIO_CONSOLE_F_SIZE (1 << 0)
#define VIRTIO_CONSOLE_F_MULTIPORT (1 << 1)
#define VIRTIO_CONSOLE_F_EMERG_WRITE (1 << 2)

class VirtIOConsolePort
    : public CharacterDevice {
public:
    explicit VirtIOConsolePort(unsigned port, VirtIOConsole&);
    void handle_queue_update(Badge<VirtIOConsole>, u16 queue_index);

    void set_open(Badge<VirtIOConsole>, bool state) { m_open = state; }
    bool is_open() const { return m_open; }

private:
    constexpr static size_t RINGBUFFER_SIZE = 2 * PAGE_SIZE;

    virtual StringView class_name() const override { return "VirtIOConsolePort"; }

    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<NonnullRefPtr<FileDescription>> open(int options) override;

    mode_t required_mode() const override { return 0666; }

    String device_name() const override;

    void init_receive_buffer();

    static unsigned next_device_id;
    u16 m_receive_queue {};
    u16 m_transmit_queue {};

    OwnPtr<RingBuffer> m_receive_buffer;
    OwnPtr<RingBuffer> m_transmit_buffer;

    VirtIOConsole& m_console;
    unsigned m_port;

    bool m_open { false };
    Atomic<bool> m_receive_buffer_exhausted;
};

}
