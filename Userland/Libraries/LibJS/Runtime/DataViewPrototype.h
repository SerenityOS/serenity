/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/DataView.h>

namespace JS {

class DataViewPrototype final : public Object {
    JS_OBJECT(DataViewPrototype, Object);

public:
    DataViewPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~DataViewPrototype() override;

private:
    JS_DECLARE_NATIVE_GETTER(buffer_getter);
    JS_DECLARE_NATIVE_GETTER(byte_length_getter);
    JS_DECLARE_NATIVE_GETTER(byte_offset_getter);
};

}
