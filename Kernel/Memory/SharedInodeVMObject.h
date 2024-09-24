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
    static ErrorOr<NonnullLockRefPtr<SharedInodeVMObject>> try_create_with_inode(Inode&);
    static ErrorOr<NonnullLockRefPtr<SharedInodeVMObject>> try_create_with_inode_and_range(Inode&, u64 offset, size_t range_size);
    virtual ErrorOr<NonnullLockRefPtr<VMObject>> try_clone() override;

    ErrorOr<void> sync(off_t offset_in_pages, size_t pages);
    ErrorOr<void> sync_before_destroying();

private:
    virtual bool is_shared_inode() const override { return true; }

    explicit SharedInodeVMObject(Inode&, FixedArray<RefPtr<PhysicalRAMPage>>&&, Bitmap dirty_pages);
    explicit SharedInodeVMObject(SharedInodeVMObject const&, FixedArray<RefPtr<PhysicalRAMPage>>&&, Bitmap dirty_pages);

    virtual StringView class_name() const override { return "SharedInodeVMObject"sv; }

    SharedInodeVMObject& operator=(SharedInodeVMObject const&) = delete;

    ErrorOr<void> sync_impl(off_t offset_in_pages, size_t pages, bool should_remap);
};

}
