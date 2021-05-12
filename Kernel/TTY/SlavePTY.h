/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

class MasterPTY;

class SlavePTY final : public TTY {
public:
    virtual ~SlavePTY() override;

    void on_master_write(const UserOrKernelBuffer&, ssize_t);
    unsigned index() const { return m_index; }

    time_t time_of_last_write() const { return m_time_of_last_write; }

    virtual FileBlockCondition& block_condition() override;

private:
    // ^TTY
    virtual String const& tty_name() const override;
    virtual ssize_t on_tty_write(const UserOrKernelBuffer&, ssize_t) override;
    virtual void echo(u8) override;

    // ^CharacterDevice
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual const char* class_name() const override { return "SlavePTY"; }
    virtual KResult close() override;

    // ^Device
    virtual String device_name() const override;

    friend class MasterPTY;
    SlavePTY(MasterPTY&, unsigned index);

    RefPtr<MasterPTY> m_master;
    time_t m_time_of_last_write { 0 };
    unsigned m_index { 0 };
    String m_tty_name;
};

}
