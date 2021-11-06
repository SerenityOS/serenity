/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/InodeVMObject.h>
#include <Kernel/UnixTypes.h>

namespace Kernel::Memory {

class SharedInodeVMObject final : public InodeVMObject {
    AK_MAKE_NONMOVABLE(SharedInodeVMObject);

public:
    static KResultOr<NonnullRefPtr<SharedInodeVMObject>> try_create_with_inode(Inode&);
    virtual KResultOr<NonnullRefPtr<VMObject>> try_clone() override;

private:
    virtual bool is_shared_inode() const override { return true; }

    explicit SharedInodeVMObject(Inode&, size_t);
    explicit SharedInodeVMObject(SharedInodeVMObject const&);

    virtual StringView class_name() const override { return "SharedInodeVMObject"sv; }

    SharedInodeVMObject& operator=(SharedInodeVMObject const&) = delete;
};

}
