#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>

class KParams {
    AK_MAKE_ETERNAL
public:
    static KParams& the();

    KParams(const String& cmdline);

    const String& cmdline() const { return m_cmdline; }
    String get(const String& key) const;
    bool has(const String& key) const;
private:
    String m_cmdline;
    HashMap<String, String> m_params;
};
