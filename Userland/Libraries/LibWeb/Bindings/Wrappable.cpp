/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Bindings/Wrapper.h>

namespace Web {
namespace Bindings {

void Wrappable::set_wrapper(Wrapper& wrapper)
{
    VERIFY(!m_wrapper);
    m_wrapper = wrapper.make_weak_ptr<Wrapper>();
}

}
}
