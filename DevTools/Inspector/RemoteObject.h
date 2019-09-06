#pragma once

#include <AK/String.h>
#include <AK/JsonObject.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Vector.h>

class RemoteObjectPropertyModel;

class RemoteObject {
public:
    RemoteObject();

    RemoteObjectPropertyModel& property_model();

    RemoteObject* parent { nullptr };
    NonnullOwnPtrVector<RemoteObject> children;

    String address;
    String parent_address;
    String class_name;
    String name;

    JsonObject json;

    NonnullRefPtr<RemoteObjectPropertyModel> m_property_model;
};
