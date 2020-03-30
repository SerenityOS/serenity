#pragma once

#include "Definitions.h"

namespace SMBIOS {
namespace Parsing {

size_t calculate_full_table_size(const TableHeader& table)
{
    const char* strtab = (char*)&const_cast<TableHeader&>(table) + table.length;
    size_t index = 1;
    while (strtab[index - 1] != '\0' || strtab[index] != '\0') {
        index++;
    }
    return table.length + index + 1;
}

Optional<String> try_to_acquire_smbios_string(const SMBIOS::TableHeader& table, u8 string_number)
{
    auto* string = (const char*)((u8*)&const_cast<SMBIOS::TableHeader&>(table) + table.length);

    // If the structure has no strings, we return with nothing immediately
    if (string_number == 0)
        return {};

    string_number -= 1;

    if (string[0] == '\0' && string[1] == '\0')
        return {};

    size_t count = 0;
    while (count < string_number) {
        if (string[0] == '\0' && string[1] == '\0') {
            return {};
        }
        while (*string++ != '\0') {
        }
        count++;
    }

    auto start_of_string = string;
    while (*string++ != '\0') {
    }
    return String(start_of_string, string - start_of_string);
}

String create_uuid(u64 part1, u64 part2)
{
    return String::format("%x%x-%x-%x-%x-%x%x%x%x", part1 & 0xffff, (part1 >> 16 & 0xffff), (part1 >> 32 & 0xffff), (part1 >> 48 & 0xffff), (part2 & 0xffff), (part2 >> 16 & 0xffff), (part2 >> 32 & 0xffff), (part2 >> 48 & 0xffff));
}

}
}
