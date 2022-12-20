/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/IntrusiveList.h>

namespace Kernel {

class HIDManagement;
class HIDController : public AtomicRefCounted<HIDController> {
    friend class HIDManagement;

public:
    virtual ~HIDController() = default;

protected:
    HIDController() = default;

private:
    IntrusiveListNode<HIDController, NonnullLockRefPtr<HIDController>> m_list_node;
};

}
