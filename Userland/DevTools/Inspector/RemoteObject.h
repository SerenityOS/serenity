/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/JsonObject.h>
#include <YAK/NonnullOwnPtrVector.h>
#include <YAK/String.h>
#include <YAK/Vector.h>

namespace Inspector {

class RemoteObjectPropertyModel;

class RemoteObject {
public:
    RemoteObject();

    RemoteObjectPropertyModel& property_model();

    RemoteObject* parent { nullptr };
    NonnullOwnPtrVector<RemoteObject> children;

    FlatPtr address { 0 };
    FlatPtr parent_address { 0 };
    String class_name;
    String name;

    JsonObject json;

    NonnullRefPtr<RemoteObjectPropertyModel> m_property_model;
};

}
