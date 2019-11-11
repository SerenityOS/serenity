#pragma once

#include <AK/String.h>
#include <AK/HashMap.h>
#include <AK/RefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibDraw/Color.h>

class CConfigFile : public RefCounted<CConfigFile> {
public:
    static NonnullRefPtr<CConfigFile> get_for_app(const String& app_name);
    static NonnullRefPtr<CConfigFile> get_for_system(const String& app_name);
    static NonnullRefPtr<CConfigFile> open(const String& path);
    ~CConfigFile();

    bool has_group(const String&) const;
    bool has_key(const String& group, const String& key) const;

    Vector<String> groups() const;
    Vector<String> keys(const String& group) const;

    String read_entry(const String& group, const String& key, const String& default_vaule = String()) const;
    int read_num_entry(const String& group, const String& key, int default_value = 0) const;
    bool read_bool_entry(const String& group, const String& key, bool default_value = false) const;
    Color read_color_entry(const String& group, const String& key, Color default_value) const;

    void write_entry(const String& group, const String& key, const String& value);
    void write_num_entry(const String& group, const String& key, int value);
    void write_bool_entry(const String& group, const String& key, bool value);
    void write_color_entry(const String& group, const String& key, Color value);

    void dump() const;

    bool is_dirty() const { return m_dirty; }

    bool sync();

    void remove_group(const String& group);
    void remove_entry(const String& group, const String& key);

    String file_name() const { return m_file_name; }

private:
    explicit CConfigFile(const String& file_name);

    void reparse();

    String m_file_name;
    HashMap<String, HashMap<String, String>> m_groups;
    bool m_dirty { false };
};
