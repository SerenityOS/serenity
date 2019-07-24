#include <Kernel/KParams.h>

static KParams* s_the;

KParams& KParams::the()
{
    return *s_the;
}

KParams::KParams(const String& cmdline)
    : m_cmdline(cmdline)
{
    s_the = this;

    for (auto str : m_cmdline.split(' ')) {
        if (str == "") {
            continue;
        }

        auto pair = str.split_limit('=', 2);

        if (pair.size() == 1) {
            m_params.set(pair[0], "");
        } else {
            m_params.set(pair[0], pair[1]);
        }
    }
}

String KParams::get(const String& key) const
{
    return m_params.get(key).value_or({});
}

bool KParams::has(const String& key) const
{
    return m_params.contains(key);
}
