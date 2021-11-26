/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/DOM/EventListener.h>

namespace Web::DOM {

JS::FunctionObject& EventListener::function()
{
    VERIFY(m_function.cell());
    return *m_function.cell();
}

}
