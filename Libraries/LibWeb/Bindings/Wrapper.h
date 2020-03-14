#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Weakable.h>
#include <LibJS/Object.h>
#include <LibWeb/Forward.h>

namespace Web {
namespace Bindings {

class Wrapper
    : public JS::Object
    , public Weakable<Wrapper> {
protected:
    explicit Wrapper() {}
};

}
}
