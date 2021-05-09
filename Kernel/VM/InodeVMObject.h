/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {

class InodeVMObject : public VMObject {
public:
    virtual ~InodeVMObject() override;

    Inode& inode() { return *m_inode; }
    const Inode& inode() const { return *m_inode; }

    size_t amount_dirty() const;
    size_t amount_clean() const;

    int release_all_clean_pages();

    u32 writable_mappings() const;
    u32 executable_mappings() const;

protected:
    explicit InodeVMObject(Inode&, size_t);
    explicit InodeVMObject(const InodeVMObject&);

    InodeVMObject& operator=(const InodeVMObject&) = delete;
    InodeVMObject& operator=(InodeVMObject&&) = delete;
    InodeVMObject(InodeVMObject&&) = delete;

    virtual bool is_inode() const final { return true; }

    int release_all_clean_pages_impl();

    NonnullRefPtr<Inode> m_inode;
    Bitmap m_dirty_pages;
};

}
