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

    static RefPtr<ContiguousVMObject> create_with_size(size_t, size_t physical_alignment = PAGE_SIZE);

private:
    explicit ContiguousVMObject(size_t, NonnullRefPtrVector<PhysicalPage>&);
    explicit ContiguousVMObject(const ContiguousVMObject&);

    virtual const char* class_name() const override { return "ContiguousVMObject"; }
    virtual RefPtr<VMObject> clone() override;

    ContiguousVMObject& operator=(const ContiguousVMObject&) = delete;
    ContiguousVMObject& operator=(ContiguousVMObject&&) = delete;
    ContiguousVMObject(ContiguousVMObject&&) = delete;

    virtual bool is_contiguous() const override { return true; }
};

}
