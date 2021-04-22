/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RemoteObject.h"
#include "RemoteObjectPropertyModel.h"

namespace Inspector {

RemoteObject::RemoteObject()
    : m_property_model(RemoteObjectPropertyModel::create(*this))
{
}

RemoteObjectPropertyModel& RemoteObject::property_model()
{
    m_property_model->update();
    return *m_property_model;
}

}
