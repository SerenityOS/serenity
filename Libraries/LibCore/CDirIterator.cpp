#include "CDirIterator.h"
#include <cerrno>

CDirIterator::CDirIterator(const StringView& path, Flags flags)
    : m_flags(flags)
{
    m_dir = opendir(String(path).characters());
    if (m_dir == nullptr) {
        m_error = errno;
    }
}

CDirIterator::~CDirIterator()
{
    if (m_dir != nullptr) {
        closedir(m_dir);
        m_dir = nullptr;
    }
}

bool CDirIterator::advance_next()
{
    if (m_dir == nullptr)
        return false;

    bool keep_advancing = true;
    while (keep_advancing) {
        errno = 0;
        auto* de = readdir(m_dir);
        if (de) {
            m_next = de->d_name;
        } else {
            m_error = errno;
            m_next = String();
        }

        if (m_next.is_null()) {
            keep_advancing = false;
        } else if (m_flags & Flags::SkipDots) {
            if (m_next.length() < 1 || m_next[0] != '.') {
                keep_advancing = false;
            }
        } else {
            keep_advancing = false;
        }
    }

    return m_next.length() > 0;
}

bool CDirIterator::has_next()
{
    if (!m_next.is_null())
        return true;

    return advance_next();
}

String CDirIterator::next_path()
{
    if (m_next.is_null())
        advance_next();

    auto tmp = m_next;
    m_next = String();
    return tmp;
}
