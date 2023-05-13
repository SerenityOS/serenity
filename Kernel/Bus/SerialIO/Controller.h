/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/SerialIO/PS2/Definitions.h>

namespace Kernel {

class HIDManagement;
class SerialIOController : public AtomicRefCounted<SerialIOController> {
    friend class HIDManagement;

public:
    virtual ~SerialIOController() = default;

    virtual StringView controller_type_name() const = 0;

protected:
    SerialIOController() = default;

private:
    IntrusiveListNode<SerialIOController, NonnullRefPtr<SerialIOController>> m_list_node;
};

}
