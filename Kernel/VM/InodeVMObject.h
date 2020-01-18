/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <Kernel/UnixTypes.h>
#include <Kernel/VM/VMObject.h>

class InodeVMObject final : public VMObject {
public:
    virtual ~InodeVMObject() override;

    static NonnullRefPtr<InodeVMObject> create_with_inode(Inode&);
    virtual NonnullRefPtr<VMObject> clone() override;

    Inode& inode() { return *m_inode; }
    const Inode& inode() const { return *m_inode; }

    void inode_contents_changed(Badge<Inode>, off_t, ssize_t, const u8*);
    void inode_size_changed(Badge<Inode>, size_t old_size, size_t new_size);

    size_t amount_dirty() const;
    size_t amount_clean() const;

    int release_all_clean_pages();

private:
    explicit InodeVMObject(Inode&, size_t);
    explicit InodeVMObject(const InodeVMObject&);

    InodeVMObject& operator=(const InodeVMObject&) = delete;
    InodeVMObject& operator=(InodeVMObject&&) = delete;
    InodeVMObject(InodeVMObject&&) = delete;

    virtual bool is_inode() const override { return true; }

    int release_all_clean_pages_impl();

    NonnullRefPtr<Inode> m_inode;
    Bitmap m_dirty_pages;
};
