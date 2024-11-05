/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/Vector.h>
#include <Kernel/Devices/CharacterDevice.h>

namespace Kernel {

extern Spinlock<LockRank::None> g_console_lock;

class ConsoleDevice final : public CharacterDevice {
    friend class Device;

public:
    static NonnullRefPtr<ConsoleDevice> must_create();

    virtual ~ConsoleDevice() override;

    // ^CharacterDevice
    virtual bool can_read(Kernel::OpenFileDescription const&, u64) const override;
    virtual bool can_write(Kernel::OpenFileDescription const&, u64) const override { return true; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, Kernel::UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, Kernel::UserOrKernelBuffer const&, size_t) override;
    virtual StringView class_name() const override { return "Console"sv; }
    // ^Device
    virtual bool is_openable_by_jailed_processes() const override { return true; }

    void put_char(char);

    CircularQueue<char, 16384> const& logbuffer() const { return m_logbuffer; }

private:
    ConsoleDevice();
    CircularQueue<char, 16384> m_logbuffer;
};

}
