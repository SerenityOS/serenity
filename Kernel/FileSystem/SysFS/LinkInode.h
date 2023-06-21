/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Inode.h>

namespace Kernel {

class SysFSLinkInode : public SysFSInode {
    friend class SysFS;

public:
    static ErrorOr<NonnullRefPtr<SysFSLinkInode>> try_create(SysFS const&, SysFSComponent const&);
    virtual ~SysFSLinkInode() override;

protected:
    SysFSLinkInode(SysFS const&, SysFSComponent const&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
};

}
