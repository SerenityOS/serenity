/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>

class DevicesModel final : public GUI::Model {
public:
    enum Column {
        Device = 0,
        Major,
        Minor,
        ClassName,
        Type,
        __Count
    };

    virtual ~DevicesModel() override;
    static NonnullRefPtr<DevicesModel> create();

    virtual int row_count(const GUI::ModelIndex&) const override;
    virtual int column_count(const GUI::ModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual void update() override;

private:
    DevicesModel();

    struct DeviceInfo {
        String path;
        unsigned major;
        unsigned minor;
        String class_name;
        enum Type {
            Block,
            Character
        };
        Type type;
    };

    Vector<DeviceInfo> m_devices;
};
