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
    EventListenerWrapper(JS::GlobalObject&, DOM::EventListener&);
    virtual ~EventListenerWrapper() override;

    DOM::EventListener& impl() { return *m_impl; }
    const DOM::EventListener& impl() const { return *m_impl; }

private:
    NonnullRefPtr<DOM::EventListener> m_impl;
};

}
}
