#pragma once

#include <AK/Vector.h>
#include <AK/HashMap.h>
#include <AK/AKString.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

class CConfigFile : public Retainable<CConfigFile> {
public:
    static Retained<CConfigFile> get_for_app(const String& app_name);
    ~CConfigFile();

    bool has_group(const String&) const;
    bool has_key(const String& group, const String& key) const;

    Vector<String> groups() const;
    Vector<String> keys(const String& group) const;

    String read_entry(const String& group, const String& key, const String& default_vaule = String()) const;
    int read_num_entry(const String& group, const String& key, int default_value = 0) const;
    bool read_bool_entry(const String& group, const String& key, bool default_value = false) const;

    void write_entry(const String& group, const String& key, const String &value);
    void write_num_entry(const String& group, const String& key, int value);
    void write_bool_entry(const String& group, const String& key, bool value);

	void dump() const;

    bool is_dirty() const { return m_dirty; }

	bool sync();

    void remove_group(const String& group);
    void remove_entry(const String& group, const String& key);

private:
    explicit CConfigFile(const String& file_name);

    void reparse();

    String m_file_name;
    HashMap<String, HashMap<String, String>> m_groups;
    bool m_dirty { false };
};
