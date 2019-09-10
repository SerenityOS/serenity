#pragma once
#include <AK/String.h>

namespace FileUtils {
bool copy_file_or_directory(const String& src_path, const String& dst_path);

String get_duplicate_name(const String& path, int duplicate_count);
bool copy_file(const String& src_path, const String& dst_path, const struct stat& src_stat, int src_fd);
bool copy_directory(const String& src_path, const String& dst_path);
}