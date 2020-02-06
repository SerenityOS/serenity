/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "DevicesModel.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/CDirIterator.h>
#include <LibCore/CFile.h>
#include <sys/stat.h>

NonnullRefPtr<DevicesModel> DevicesModel::create()
{
    return adopt(*new DevicesModel);
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
        ASSERT_NOT_REACHED();
    }
}

GUI::Model::ColumnMetadata DevicesModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Device:
        return { 70, Gfx::TextAlignment::CenterLeft };
    case Column::Major:
        return { 32, Gfx::TextAlignment::CenterRight };
    case Column::Minor:
        return { 32, Gfx::TextAlignment::CenterRight };
    case Column::ClassName:
        return { 120, Gfx::TextAlignment::CenterLeft };
    case Column::Type:
        return { 120, Gfx::TextAlignment::CenterLeft };
    default:
        ASSERT_NOT_REACHED();
    }
}

GUI::Variant DevicesModel::data(const GUI::ModelIndex& index, Role) const
{
    ASSERT(is_valid(index));

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
            ASSERT_NOT_REACHED();
        }
    default:
        ASSERT_NOT_REACHED();
    }
}

void DevicesModel::update()
{
    auto proc_devices = Core::File::construct("/proc/devices");
    if (!proc_devices->open(Core::IODevice::OpenMode::ReadOnly))
        ASSERT_NOT_REACHED();

    auto json = JsonValue::from_string(proc_devices->read_all()).as_array();

    m_devices.clear();
    json.for_each([this](auto& value) {
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
            ASSERT_NOT_REACHED();

        m_devices.append(move(device_info));
    });

    auto fill_in_paths_from_dir = [this](const String& dir) {
        Core::DirIterator dir_iter { dir, Core::DirIterator::Flags::SkipDots };
        while (dir_iter.has_next()) {
            auto name = dir_iter.next_path();
            auto path = String::format("%s/%s", dir.characters(), name.characters());
            struct stat statbuf;
            if (lstat(path.characters(), &statbuf) != 0) {
                ASSERT_NOT_REACHED();
            }
            if (!S_ISBLK(statbuf.st_mode) && !S_ISCHR(statbuf.st_mode))
                continue;
            unsigned _major = major(statbuf.st_rdev);
            unsigned _minor = minor(statbuf.st_rdev);

            auto it = m_devices.find([_major, _minor](auto& device_info) {
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
