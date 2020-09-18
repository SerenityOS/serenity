/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {

class ContiguousVMObject final : public VMObject {
public:
    virtual ~ContiguousVMObject() override;

    static NonnullRefPtr<ContiguousVMObject> create_with_size(size_t);

private:
    explicit ContiguousVMObject(size_t);
    explicit ContiguousVMObject(const ContiguousVMObject&);

    virtual const char* class_name() const override { return "ContiguousVMObject"; }
    virtual NonnullRefPtr<VMObject> clone() override;

    ContiguousVMObject& operator=(const ContiguousVMObject&) = delete;
    ContiguousVMObject& operator=(ContiguousVMObject&&) = delete;
    ContiguousVMObject(ContiguousVMObject&&) = delete;

    virtual bool is_contiguous() const override { return true; }
};

}

AK_BEGIN_TYPE_TRAITS(Kernel::ContiguousVMObject)
static bool is_type(const Kernel::VMObject& vmobject) { return vmobject.is_contiguous(); }
AK_END_TYPE_TRAITS()
