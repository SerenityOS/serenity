/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <Kernel/Memory/InodeVMObject.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class PrivateInodeVMObject final : public InodeVMObject {
    AK_MAKE_NONMOVABLE(PrivateInodeVMObject);

public:
    virtual ~PrivateInodeVMObject() override;

    static RefPtr<PrivateInodeVMObject> try_create_with_inode(Inode&);
    virtual RefPtr<VMObject> try_clone() override;

private:
    virtual bool is_private_inode() const override { return true; }

    explicit PrivateInodeVMObject(Inode&, size_t);
    explicit PrivateInodeVMObject(PrivateInodeVMObject const&);

    virtual StringView class_name() const override { return "PrivateInodeVMObject"sv; }

    PrivateInodeVMObject& operator=(PrivateInodeVMObject const&) = delete;
};

}
