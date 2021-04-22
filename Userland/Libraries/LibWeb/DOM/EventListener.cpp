/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Function.h>
#include <LibWeb/DOM/EventListener.h>

namespace Web::DOM {

JS::Function& EventListener::function()
{
    VERIFY(m_function.cell());
    return *m_function.cell();
}

}
