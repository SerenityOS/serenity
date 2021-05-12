/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DevicesModel.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <sys/stat.h>

NonnullRefPtr<DevicesModel> DevicesModel::create()
{
    return adopt_ref(*new DevicesModel);
}

DevicesModel::DevicesModel()
{
}

DevicesModel::~DevicesModel()
{
}

int DevicesModel::row_count(const GUI::ModelIndex&) const
{
    return m_devices.size();
}

int DevicesModel::column_count(const GUI::ModelIndex&) const
{
    return Column::__Count;
}

String DevicesModel::column_name(int column) const
{
    switch (column) {
    case Column::Device:
        return "Device";
    case Column::Major:
        return "Major";
    case Column::Minor:
        return "Minor";
    case Column::ClassName:
        return "Class";
    case Column::Type:
        return "Type";
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant DevicesModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    VERIFY(is_valid(index));

    if (role == GUI::ModelRole::TextAlignment) {
        switch (index.column()) {
        case Column::Device:
            return Gfx::TextAlignment::CenterLeft;
        case Column::Major:
            return Gfx::TextAlignment::CenterRight;
        case Column::Minor:
            return Gfx::TextAlignment::CenterRight;
        case Column::ClassName:
            return Gfx::TextAlignment::CenterLeft;
        case Column::Type:
            return Gfx::TextAlignment::CenterLeft;
        }
        return {};
    }

    if (role == GUI::ModelRole::Sort) {
        const DeviceInfo& device = m_devices[index.row()];
        switch (index.column()) {
        case Column::Device:
            return device.path;
        case Column::Major:
            return device.major;
        case Column::Minor:
            return device.minor;
        case Column::ClassName:
            return device.class_name;
        case Column::Type:
            return device.type;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    if (role == GUI::ModelRole::Display) {
        const DeviceInfo& device = m_devices[index.row()];
        switch (index.column()) {
        case Column::Device:
            return device.path;
        case Column::Major:
            return device.major;
        case Column::Minor:
            return device.minor;
        case Column::ClassName:
            return device.class_name;
        case Column::Type:
            switch (device.type) {
            case DeviceInfo::Type::Block:
                return "Block";
            case DeviceInfo::Type::Character:
                return "Character";
            default:
                VERIFY_NOT_REACHED();
            }
        default:
            VERIFY_NOT_REACHED();
        }
    }

    return {};
}

void DevicesModel::update()
{
    auto proc_devices = Core::File::construct("/proc/devices");
    if (!proc_devices->open(Core::OpenMode::ReadOnly))
        VERIFY_NOT_REACHED();

    auto json = JsonValue::from_string(proc_devices->read_all());
    VERIFY(json.has_value());

    m_devices.clear();
    json.value().as_array().for_each([this](auto& value) {
        JsonObject device = value.as_object();
        DeviceInfo device_info;

        device_info.major = device.get("major").to_uint();
        device_info.minor = device.get("minor").to_uint();
        device_info.class_name = device.get("class_name").to_string();

        String type_str = device.get("type").to_string();
        if (type_str == "block")
            device_info.type = DeviceInfo::Type::Block;
        else if (type_str == "character")
            device_info.type = DeviceInfo::Type::Character;
        else
            VERIFY_NOT_REACHED();

        m_devices.append(move(device_info));
    });

    auto fill_in_paths_from_dir = [this](const String& dir) {
        Core::DirIterator dir_iter { dir, Core::DirIterator::Flags::SkipDots };
        while (dir_iter.has_next()) {
            auto path = dir_iter.next_full_path();
            struct stat statbuf;
            if (lstat(path.characters(), &statbuf) != 0) {
                VERIFY_NOT_REACHED();
            }
            if (!S_ISBLK(statbuf.st_mode) && !S_ISCHR(statbuf.st_mode))
                continue;
            unsigned _major = major(statbuf.st_rdev);
            unsigned _minor = minor(statbuf.st_rdev);

            auto it = m_devices.find_if([_major, _minor](const auto& device_info) {
                return device_info.major == _major && device_info.minor == _minor;
            });
            if (it != m_devices.end()) {
                (*it).path = move(path);
            }
        }
    };

    fill_in_paths_from_dir("/dev");
    fill_in_paths_from_dir("/dev/pts");

    did_update();
}
