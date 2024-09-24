/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <Kernel/Memory/VMObject.h>
#include <Kernel/UnixTypes.h>

namespace Kernel::Memory {

class InodeVMObject : public VMObject {
public:
    virtual ~InodeVMObject() override;

    Inode& inode() { return *m_inode; }
    Inode const& inode() const { return *m_inode; }

    size_t amount_dirty() const;
    size_t amount_clean() const;

    bool is_page_dirty(size_t page_index) const;
    void set_page_dirty(size_t page_index, bool is_dirty);

    int release_all_clean_pages();
    int try_release_clean_pages(int page_amount);

    u32 writable_mappings() const;

protected:
    explicit InodeVMObject(Inode&, FixedArray<RefPtr<PhysicalRAMPage>>&&, Bitmap dirty_pages);
    explicit InodeVMObject(InodeVMObject const&, FixedArray<RefPtr<PhysicalRAMPage>>&&, Bitmap dirty_pages);

    InodeVMObject& operator=(InodeVMObject const&) = delete;
    InodeVMObject& operator=(InodeVMObject&&) = delete;
    InodeVMObject(InodeVMObject&&) = delete;

    virtual bool is_inode() const final { return true; }

    NonnullRefPtr<Inode> const m_inode;
    Bitmap m_dirty_pages;
};

}
