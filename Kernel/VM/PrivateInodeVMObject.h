/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/InodeVMObject.h>

namespace Kernel {

class PrivateInodeVMObject final : public InodeVMObject {
    AK_MAKE_NONMOVABLE(PrivateInodeVMObject);

public:
    virtual ~PrivateInodeVMObject() override;

    static RefPtr<PrivateInodeVMObject> create_with_inode(Inode&);
    virtual RefPtr<VMObject> clone() override;

private:
    virtual bool is_private_inode() const override { return true; }

    explicit PrivateInodeVMObject(Inode&, size_t);
    explicit PrivateInodeVMObject(const PrivateInodeVMObject&);

    virtual const char* class_name() const override { return "PrivateInodeVMObject"; }

    PrivateInodeVMObject& operator=(const PrivateInodeVMObject&) = delete;
};

}
