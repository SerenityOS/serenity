#include "FileReader.h"

FileReader::FileReader(const std::string& path)
    : m_path(path)
{
    m_file = fopen(path.c_str(), "r");
}

FileReader::~FileReader()
{
    if (m_file)
        fclose(m_file);
    m_file = nullptr;
}

bool FileReader::can_read() const
{
    return m_file && !feof(m_file);
}

std::string FileReader::read_line()
{
    if (!m_file) {
        fprintf(stderr, "Error: FileReader::readLine() called on invalid FileReader for '%s'\n", m_path.c_str());
        return std::string();
    }

    std::string line;

    while (can_read()) {
        int ch = fgetc(m_file);
        if (ch == EOF)
            break;
        if (ch == '\n')
            break;
        line += ch;
    }

    return line;
}
