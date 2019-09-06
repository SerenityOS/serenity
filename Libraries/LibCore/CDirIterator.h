#pragma once

#include <AK/String.h>
#include <dirent.h>

class CDirIterator {
public:
    enum Flags {
        NoFlags = 0x0,
        SkipDots = 0x1,
    };

    CDirIterator(const StringView& path, Flags = Flags::NoFlags);
    ~CDirIterator();

    bool has_error() const { return m_error != 0; }
    int error() const { return m_error; }
    const char* error_string() const { return strerror(m_error); }
    bool has_next();
    String next_path();

private:
    DIR* m_dir = nullptr;
    int m_error = 0;
    String m_next;
    int m_flags;

    bool advance_next();
};
