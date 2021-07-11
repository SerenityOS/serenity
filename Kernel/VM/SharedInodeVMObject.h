/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/InodeVMObject.h>

namespace Kernel {

class SharedInodeVMObject final : public InodeVMObject {
    AK_MAKE_NONMOVABLE(SharedInodeVMObject);

public:
    static RefPtr<SharedInodeVMObject> try_create_with_inode(Inode&);
    virtual RefPtr<VMObject> clone() override;

private:
    virtual bool is_shared_inode() const override { return true; }

    explicit SharedInodeVMObject(Inode&, size_t);
    explicit SharedInodeVMObject(const SharedInodeVMObject&);

    virtual StringView class_name() const override { return "SharedInodeVMObject"sv; }

    SharedInodeVMObject& operator=(const SharedInodeVMObject&) = delete;
};

}
