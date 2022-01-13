/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/Forward.h>

namespace Kernel {

class Mount {
public:
    Mount(FileSystem&, Custody* host_custody, int flags);
    Mount(Inode& source, Custody& host_custody, int flags);

    Inode const* host() const;
    Inode* host();

    Inode const& guest() const { return *m_guest; }
    Inode& guest() { return *m_guest; }

    FileSystem const& guest_fs() const { return *m_guest_fs; }
    FileSystem& guest_fs() { return *m_guest_fs; }

    ErrorOr<NonnullOwnPtr<KString>> absolute_path() const;

    int flags() const { return m_flags; }
    void set_flags(int flags) { m_flags = flags; }

private:
    NonnullRefPtr<Inode> m_guest;
    NonnullRefPtr<FileSystem> m_guest_fs;
    RefPtr<Custody> m_host_custody;
    int m_flags;
};

}
