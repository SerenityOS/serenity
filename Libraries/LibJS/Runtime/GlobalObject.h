#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class GlobalObject final : public Object {
public:
    explicit GlobalObject();
    virtual ~GlobalObject() override;

private:
    virtual const char* class_name() const override { return "GlobalObject"; }
};

}
