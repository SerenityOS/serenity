/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/InodeVMObject.h>
#include <Kernel/UnixTypes.h>

namespace Kernel::Memory {

class PrivateInodeVMObject final : public InodeVMObject {
    AK_MAKE_NONMOVABLE(PrivateInodeVMObject);

public:
    virtual ~PrivateInodeVMObject() override;

    static ErrorOr<NonnullRefPtr<PrivateInodeVMObject>> try_create_with_inode(Inode&);
    virtual ErrorOr<NonnullRefPtr<VMObject>> try_clone() override;

private:
    virtual bool is_private_inode() const override { return true; }

    explicit PrivateInodeVMObject(Inode&, FixedArray<RefPtr<PhysicalPage>>&&, Bitmap dirty_pages);
    explicit PrivateInodeVMObject(PrivateInodeVMObject const&, FixedArray<RefPtr<PhysicalPage>>&&, Bitmap dirty_pages);

    virtual StringView class_name() const override { return "PrivateInodeVMObject"sv; }

    PrivateInodeVMObject& operator=(PrivateInodeVMObject const&) = delete;
};

}
