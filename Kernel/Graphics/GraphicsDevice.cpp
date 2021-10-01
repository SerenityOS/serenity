/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/Graphics/GraphicsManagement.h>

namespace Kernel {

void GraphicsDevice::set_console(Graphics::Console const& console) const
{
    GraphicsManagement::the().set_console({}, console);
}

}
