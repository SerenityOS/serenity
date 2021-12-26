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

int DevicesModel::row_count(const GModelIndex&) const
{
    return m_devices.size();
}

int DevicesModel::column_count(const GModelIndex&) const
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

GModel::ColumnMetadata DevicesModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Device:
        return { 70, TextAlignment::CenterLeft };
    case Column::Major:
        return { 32, TextAlignment::CenterRight };
    case Column::Minor:
        return { 32, TextAlignment::CenterRight };
    case Column::ClassName:
        return { 120, TextAlignment::CenterLeft };
    case Column::Type:
        return { 120, TextAlignment::CenterLeft };
    default:
        ASSERT_NOT_REACHED();
    }
}

GVariant DevicesModel::data(const GModelIndex& index, Role) const
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
    auto proc_devices = CFile::construct("/proc/devices");
    if (!proc_devices->open(CIODevice::OpenMode::ReadOnly))
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
        CDirIterator dir_iter { dir, CDirIterator::Flags::SkipDots };
        while (dir_iter.has_next()) {
            auto name = dir_iter.next_path();
            auto path = String::format("%s/%s", dir.characters(), name.characters());
            struct stat statbuf;
            if (lstat(path.characters(), &statbuf) != 0) {
                ASSERT_NOT_REACHED();
            }
            if (!S_ISBLK(statbuf.st_mode) && !S_ISCHR(statbuf.st_mode))
                continue;
            unsigned major = ::major(statbuf.st_rdev);
            unsigned minor = ::minor(statbuf.st_rdev);

            auto it = m_devices.find([major, minor](auto& device_info) {
                return device_info.major == major && device_info.minor == minor;
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
