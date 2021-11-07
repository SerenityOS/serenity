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

class ConsoleDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
    friend class DeviceManagement;

public:
    static NonnullRefPtr<ConsoleDevice> must_create();

    virtual ~ConsoleDevice() override;

    // ^CharacterDevice
    virtual bool can_read(const Kernel::OpenFileDescription&, size_t) const override;
    virtual bool can_write(const Kernel::OpenFileDescription&, size_t) const override { return true; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, Kernel::UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const Kernel::UserOrKernelBuffer&, size_t) override;
    virtual StringView class_name() const override { return "Console"sv; }

    void put_char(char);

    const CircularQueue<char, 16384>& logbuffer() const { return m_logbuffer; }

private:
    ConsoleDevice();
    CircularQueue<char, 16384> m_logbuffer;
};

}
