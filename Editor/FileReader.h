#pragma once

#include <string>
#include <stdio.h>

class FileReader {
public:
    explicit FileReader(const std::string& path);
    ~FileReader();

    bool can_read() const;
    std::string read_line();

private:
    std::string m_path;
    FILE* m_file { nullptr };
};
