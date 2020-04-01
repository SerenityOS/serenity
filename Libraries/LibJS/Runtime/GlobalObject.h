#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class GlobalObject : public Object {
public:
    explicit GlobalObject();
    virtual ~GlobalObject() override;

private:
    virtual const char* class_name() const override { return "GlobalObject"; }

    static Value gc(Interpreter&);
    static Value is_nan(Interpreter&);
};

}
