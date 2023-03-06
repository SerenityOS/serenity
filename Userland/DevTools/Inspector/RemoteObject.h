/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/JsonObject.h>

namespace Inspector {

class RemoteObjectPropertyModel;

class RemoteObject {
public:
    RemoteObject();

    RemoteObjectPropertyModel& property_model();

    RemoteObject* parent { nullptr };
    Vector<NonnullOwnPtr<RemoteObject>> children;

    FlatPtr address { 0 };
    FlatPtr parent_address { 0 };
    DeprecatedString class_name;
    DeprecatedString name;

    JsonObject json;

    NonnullRefPtr<RemoteObjectPropertyModel> m_property_model;
};

}
