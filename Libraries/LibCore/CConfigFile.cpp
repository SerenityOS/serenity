#include <AK/StringBuilder.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CFile.h>
#include <LibCore/CUserInfo.h>
#include <pwd.h>
#include <stdio.h>
#include <unistd.h>

NonnullRefPtr<CConfigFile> CConfigFile::get_for_app(const String& app_name)
{
    String home_path = get_current_user_home_path();
    if (home_path == "/")
        home_path = String::format("/tmp");
    auto path = String::format("%s/%s.ini", home_path.characters(), app_name.characters());
    return adopt(*new CConfigFile(path));
}

NonnullRefPtr<CConfigFile> CConfigFile::get_for_system(const String& app_name)
{
    auto path = String::format("/etc/%s.ini", app_name.characters());
    return adopt(*new CConfigFile(path));
}

NonnullRefPtr<CConfigFile> CConfigFile::open(const String& path)
{
    return adopt(*new CConfigFile(path));
}

CConfigFile::CConfigFile(const String& file_name)
    : m_file_name(file_name)
{
    reparse();
}

CConfigFile::~CConfigFile()
{
    sync();
}

void CConfigFile::reparse()
{
    m_groups.clear();

    auto file = CFile::construct(m_file_name);
    if (!file->open(CIODevice::OpenMode::ReadOnly))
        return;

    HashMap<String, String>* current_group = nullptr;

    while (file->can_read_line()) {
        auto line = file->read_line(BUFSIZ);
        auto* cp = (const char*)line.data();

        while (*cp && (*cp == ' ' || *cp == '\t' || *cp == '\n'))
            ++cp;

        switch (*cp) {
        case '\0': // EOL...
        case '#':  // Comment, skip entire line.
        case ';':  // -||-
            continue;
        case '[': { // Start of new group.
            StringBuilder builder;
            ++cp; // Skip the '['
            while (*cp && (*cp != ']'))
                builder.append(*(cp++));
            current_group = &m_groups.ensure(builder.to_string());
            break;
        }
        default: { // Start of key{
            StringBuilder key_builder;
            StringBuilder value_builder;
            while (*cp && (*cp != '='))
                key_builder.append(*(cp++));
            ++cp; // Skip the '='
            while (*cp && (*cp != '\n'))
                value_builder.append(*(cp++));
            if (!current_group) {
                // We're not in a group yet, create one with the name ""...
                current_group = &m_groups.ensure("");
            }
            current_group->set(key_builder.to_string(), value_builder.to_string());
        }
        }
    }
}

String CConfigFile::read_entry(const String& group, const String& key, const String& default_value) const
{
    if (!has_key(group, key)) {
        const_cast<CConfigFile&>(*this).write_entry(group, key, default_value);
        return default_value;
    }
    auto it = m_groups.find(group);
    auto jt = it->value.find(key);
    return jt->value;
}

int CConfigFile::read_num_entry(const String& group, const String& key, int default_value) const
{
    if (!has_key(group, key)) {
        const_cast<CConfigFile&>(*this).write_num_entry(group, key, default_value);
        return default_value;
    }

    bool ok;
    int value = read_entry(group, key).to_uint(ok);
    if (!ok)
        return default_value;
    return value;
}

Color CConfigFile::read_color_entry(const String& group, const String& key, Color default_value) const
{
    if (!has_key(group, key)) {
        const_cast<CConfigFile&>(*this).write_color_entry(group, key, default_value);
        return default_value;
    }

    auto shades = read_entry(group, key).split(',');
    if (shades.size() < 3)
        return default_value;
    bool ok1 = true,
         ok2 = true,
         ok3 = true,
         ok4 = true;
    Color value;
    if (shades.size() == 3) {
        value = Color(shades[0].to_uint(ok1),
            shades[1].to_uint(ok2),
            shades[2].to_uint(ok3));
    } else {
        value = Color(shades[0].to_uint(ok1),
            shades[1].to_uint(ok2),
            shades[2].to_uint(ok3),
            shades[3].to_uint(ok4));
    }
    if (!(ok1 && ok2 && ok3 && ok4))
        return default_value;
    return value;
}

bool CConfigFile::read_bool_entry(const String& group, const String& key, bool default_value) const
{
    return read_entry(group, key, default_value ? "1" : "0") == "1";
}

void CConfigFile::write_entry(const String& group, const String& key, const String& value)
{
    m_groups.ensure(group).ensure(key) = value;
    m_dirty = true;
}

void CConfigFile::write_num_entry(const String& group, const String& key, int value)
{
    write_entry(group, key, String::number(value));
}
void CConfigFile::write_bool_entry(const String& group, const String& key, bool value)
{
    write_entry(group, key, value ? "1" : "0");
}
void CConfigFile::write_color_entry(const String& group, const String& key, Color value)
{
    write_entry(group, key, String::format("%d,%d,%d,%d", value.red(), value.green(), value.blue(), value.alpha()));
}

bool CConfigFile::sync()
{
    if (!m_dirty)
        return true;

    FILE* fp = fopen(m_file_name.characters(), "wb");
    if (!fp)
        return false;

    for (auto& it : m_groups) {
        fprintf(fp, "[%s]\n", it.key.characters());
        for (auto& jt : it.value)
            fprintf(fp, "%s=%s\n", jt.key.characters(), jt.value.characters());
        fprintf(fp, "\n");
    }

    fclose(fp);

    m_dirty = false;
    return true;
}

void CConfigFile::dump() const
{
    for (auto& it : m_groups) {
        printf("[%s]\n", it.key.characters());
        for (auto& jt : it.value)
            printf("%s=%s\n", jt.key.characters(), jt.value.characters());
        printf("\n");
    }
}

Vector<String> CConfigFile::groups() const
{
    return m_groups.keys();
}

Vector<String> CConfigFile::keys(const String& group) const
{
    auto it = m_groups.find(group);
    if (it == m_groups.end())
        return {};
    return it->value.keys();
}

bool CConfigFile::has_key(const String& group, const String& key) const
{
    auto it = m_groups.find(group);
    if (it == m_groups.end())
        return {};
    return it->value.contains(key);
}

bool CConfigFile::has_group(const String& group) const
{
    return m_groups.contains(group);
}

void CConfigFile::remove_group(const String& group)
{
    m_groups.remove(group);
    m_dirty = true;
}

void CConfigFile::remove_entry(const String& group, const String& key)
{
    auto it = m_groups.find(group);
    if (it == m_groups.end())
        return;
    it->value.remove(key);
    m_dirty = true;
}
