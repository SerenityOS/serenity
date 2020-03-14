#pragma once

#include <AK/WeakPtr.h>
#include <LibJS/Heap.h>
#include <LibWeb/Forward.h>

namespace Web {
namespace Bindings {

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
inline Wrapper* wrap(JS::Heap& heap, NativeObject& native_object)
{
    if (!native_object.wrapper())
        native_object.set_wrapper(*heap.allocate<typename NativeObject::WrapperType>(native_object));
    return native_object.wrapper();
}

}
}
