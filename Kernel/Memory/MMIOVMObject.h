/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/VMObject.h>

namespace Kernel::Memory {

class MMIOVMObject final : public VMObject {
public:
    static ErrorOr<NonnullLockRefPtr<MMIOVMObject>> try_create_for_physical_range(PhysicalAddress paddr, size_t size);

    virtual ErrorOr<NonnullLockRefPtr<VMObject>> try_clone() override { return ENOTSUP; }

private:
    MMIOVMObject(PhysicalAddress, FixedArray<RefPtr<PhysicalPage>>&&);

    virtual StringView class_name() const override { return "MMIOVMObject"sv; }
};

}
