/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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
    virtual bool unref() const override;
    virtual ~SlavePTY() override;

    void on_master_write(UserOrKernelBuffer const&, size_t);
    unsigned index() const { return m_index; }

    time_t time_of_last_write() const { return m_time_of_last_write; }

    virtual FileBlockerSet& blocker_set() override;

private:
    // ^TTY
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_name() const override;
    virtual ErrorOr<size_t> on_tty_write(UserOrKernelBuffer const&, size_t) override;
    virtual void echo(u8) override;

    // ^CharacterDevice
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual StringView class_name() const override { return "SlavePTY"sv; }
    virtual ErrorOr<void> close() override;

    friend class MasterPTY;
    SlavePTY(MasterPTY&, unsigned index);

    LockRefPtr<MasterPTY> m_master;
    time_t m_time_of_last_write { 0 };
    unsigned m_index { 0 };

    mutable IntrusiveListNode<SlavePTY> m_list_node;

public:
    using List = IntrusiveList<&SlavePTY::m_list_node>;
    static SpinlockProtected<SlavePTY::List>& all_instances();
};

}
