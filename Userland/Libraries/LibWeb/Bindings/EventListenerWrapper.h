/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/Wrapper.h>

namespace Web {
namespace Bindings {

class EventListenerWrapper final : public Wrapper {
    JS_OBJECT(EventListenerWrapper, Wrapper);

public:
    EventListenerWrapper(JS::GlobalObject&, DOM::IDLEventListener&);
    virtual ~EventListenerWrapper() override;

    DOM::IDLEventListener& impl() { return *m_impl; }
    DOM::IDLEventListener const& impl() const { return *m_impl; }

private:
    NonnullRefPtr<DOM::IDLEventListener> m_impl;
};

}
}
