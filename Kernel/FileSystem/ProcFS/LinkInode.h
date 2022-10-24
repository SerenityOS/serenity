/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/ProcFS/GlobalInode.h>

namespace Kernel {

class ProcFSLinkInode : public ProcFSGlobalInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSLinkInode>> try_create(ProcFS const&, ProcFSExposedComponent const&);

protected:
    ProcFSLinkInode(ProcFS const&, ProcFSExposedComponent const&);
    virtual InodeMetadata metadata() const override;
};

}
