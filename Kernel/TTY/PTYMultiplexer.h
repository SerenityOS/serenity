/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Badge.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

class MasterPTY;

class PTYMultiplexer final : public CharacterDevice {
    YAK_MAKE_ETERNAL
public:
    PTYMultiplexer();
    virtual ~PTYMultiplexer() override;

    static void initialize()
    {
        the();
    }
    static PTYMultiplexer& the();

    // ^CharacterDevice
    virtual KResultOr<NonnullRefPtr<FileDescription>> open(int options) override;
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return 0; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return 0; }
    virtual bool can_read(const FileDescription&, size_t) const override { return true; }
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }

    void notify_master_destroyed(Badge<MasterPTY>, unsigned index);

    // ^Device
    virtual mode_t required_mode() const override { return 0666; }
    virtual String device_name() const override { return "ptmx"; }

private:
    // ^CharacterDevice
    virtual StringView class_name() const override { return "PTYMultiplexer"; }

    static constexpr size_t max_pty_pairs = 64;
    MutexProtected<Vector<unsigned, max_pty_pairs>> m_freelist;
};

}
