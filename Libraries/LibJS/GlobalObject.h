#pragma once

#include <LibJS/Object.h>

namespace JS {

class GlobalObject final : public Object {
public:
    explicit GlobalObject(Heap&);
    virtual ~GlobalObject() override;

private:
    virtual const char* class_name() const override { return "GlobalObject"; }
};

}
