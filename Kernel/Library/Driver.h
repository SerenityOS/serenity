/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Types.h>

namespace Kernel {

#define KERNEL_MAKE_DRIVER_LISTABLE(c) \
public:                                \
    IntrusiveListNode<c, NonnullRefPtr<c>> m_driver_list_node;

}
