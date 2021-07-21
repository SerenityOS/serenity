/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {
class ContiguousVMObject final : public VMObject {
public:
    virtual ~ContiguousVMObject() override;

    static RefPtr<ContiguousVMObject> try_create_with_size(size_t);

private:
    explicit ContiguousVMObject(size_t, NonnullRefPtrVector<PhysicalPage>&);
    explicit ContiguousVMObject(ContiguousVMObject const&);

    virtual StringView class_name() const override { return "ContiguousVMObject"sv; }
    virtual RefPtr<VMObject> try_clone() override;

    ContiguousVMObject& operator=(ContiguousVMObject const&) = delete;
    ContiguousVMObject& operator=(ContiguousVMObject&&) = delete;
    ContiguousVMObject(ContiguousVMObject&&) = delete;

    virtual bool is_contiguous() const override { return true; }
};

}
