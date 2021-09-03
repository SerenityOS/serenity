/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/WeakPtr.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

class Wrappable {
public:
    virtual ~Wrappable();

    void set_wrapper(Wrapper&);
    Wrapper* wrapper() { return m_wrapper; }
    const Wrapper* wrapper() const { return m_wrapper; }

private:
    WeakPtr<Wrapper> m_wrapper;
};

template<class NativeObject>
inline Wrapper* wrap_impl(JS::GlobalObject& global_object, NativeObject& native_object)
{
    if (!native_object.wrapper()) {
        native_object.set_wrapper(*global_object.heap().allocate<typename NativeObject::WrapperType>(global_object, global_object, native_object));
    }
    return native_object.wrapper();
}

}
