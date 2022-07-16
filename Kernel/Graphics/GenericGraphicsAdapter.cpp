/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/GraphicsManagement.h>

namespace Kernel {

GenericGraphicsAdapter::GenericGraphicsAdapter()
    : m_adapter_id(GraphicsManagement::generate_adapter_id())
{
}

}
