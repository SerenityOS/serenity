/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "precompiled.hpp"

#ifdef _WINDOWS

#include "logging/log.hpp"
#include "runtime/os.hpp"
#include "runtime/flags/flagSetting.hpp"
#include "runtime/globals_extension.hpp"
#include "concurrentTestRunner.inline.hpp"
#include "unittest.hpp"

namespace {
  class MemoryReleaser {
    char* const _ptr;
    const size_t _size;
   public:
    MemoryReleaser(char* ptr, size_t size) : _ptr(ptr), _size(size) { }
    ~MemoryReleaser() {
      if (_ptr != NULL) {
        os::release_memory_special(_ptr, _size);
      }
    }
  };
}

// test tries to allocate memory in a single contiguous memory block at a particular address.
// The test first tries to find a good approximate address to allocate at by using the same
// method to allocate some memory at any address. The test then tries to allocate memory in
// the vicinity (not directly after it to avoid possible by-chance use of that location)
// This is of course only some dodgy assumption, there is no guarantee that the vicinity of
// the previously allocated memory is available for allocation. The only actual failure
// that is reported is when the test tries to allocate at a particular location but gets a
// different valid one. A NULL return value at this point is not considered an error but may
// be legitimate.
void TestReserveMemorySpecial_test() {
  if (!UseLargePages) {
    return;
  }

  // set globals to make sure we hit the correct code path
  AutoSaveRestore<bool> FLAG_GUARD(UseLargePagesIndividualAllocation);
  AutoSaveRestore<bool> FLAG_GUARD(UseNUMAInterleaving);
  FLAG_SET_CMDLINE(UseLargePagesIndividualAllocation, false);
  FLAG_SET_CMDLINE(UseNUMAInterleaving, false);

  const size_t large_allocation_size = os::large_page_size() * 4;
  char* result = os::reserve_memory_special(large_allocation_size, os::large_page_size(), os::large_page_size(), NULL, false);
  if (result == NULL) {
      // failed to allocate memory, skipping the test
      return;
  }
  MemoryReleaser m1(result, large_allocation_size);

  // Reserve another page within the recently allocated memory area. This should fail
  const size_t expected_allocation_size = os::large_page_size();
  char* expected_location = result + os::large_page_size();
  char* actual_location = os::reserve_memory_special(expected_allocation_size, os::large_page_size(), os::large_page_size(), expected_location, false);
  EXPECT_TRUE(actual_location == NULL) << "Should not be allowed to reserve within present reservation";

  // Instead try reserving after the first reservation.
  expected_location = result + large_allocation_size;
  actual_location = os::reserve_memory_special(expected_allocation_size, os::large_page_size(), os::large_page_size(), expected_location, false);
  EXPECT_TRUE(actual_location != NULL) << "Unexpected reservation failure, can’t verify correct location";
  EXPECT_TRUE(actual_location == expected_location) << "Reservation must be at requested location";
  MemoryReleaser m2(actual_location, os::large_page_size());

  // Now try to do a reservation with a larger alignment.
  const size_t alignment = os::large_page_size() * 2;
  const size_t new_large_size = alignment * 4;
  char* aligned_request = os::reserve_memory_special(new_large_size, alignment, os::large_page_size(), NULL, false);
  EXPECT_TRUE(aligned_request != NULL) << "Unexpected reservation failure, can’t verify correct alignment";
  EXPECT_TRUE(is_aligned(aligned_request, alignment)) << "Returned address must be aligned";
  MemoryReleaser m3(aligned_request, new_large_size);
}

// The types of path modifications we randomly apply to a path. They should not change the file designated by the path.
enum ModsFilter {
  Allow_None = 0, // No modifications
  Allow_Sep_Mods = 1, // Replace '\\' by any sequence of '/' or '\\' or at least length 1.
  Allow_Dot_Path = 2, // Add /. segments at random positions
  Allow_Dot_Dot_Path = 4, // Add /../<correct-dir> segments at random positions.
  Allow_All = Allow_Sep_Mods | Allow_Dot_Path | Allow_Dot_Dot_Path
};

// The mode in which to run.
enum Mode {
  TEST, // Runs the test. This is the normal modus.
  EXAMPLES, // Runs example which document the behaviour of the Windows system calls.
  BENCH // Runs a small benchmark which tries to show the costs of using the *W variants/_wfullpath.
};

// Parameters of the test.
static ModsFilter mods_filter = Allow_All;
static int mods_per_path = 50; // The number of variants of a path we try.
static Mode mode = TEST;


// Utility methods
static void get_current_dir_w(wchar_t* path, size_t size) {
  DWORD count = GetCurrentDirectoryW((DWORD) size, path);
  EXPECT_GT((int) count, 0) << "Failed to get current directory: " << GetLastError();
  EXPECT_LT((size_t) count, size) << "Buffer too small for current directory: " << size;
}

#define WITH_ABS_PATH(path) \
  wchar_t abs_path[JVM_MAXPATHLEN]; \
  wchar_t cwd[JVM_MAXPATHLEN]; \
  get_current_dir_w(cwd, JVM_MAXPATHLEN); \
  wsprintfW(abs_path, L"\\\\?\\%ls\\%ls", cwd, (path))

static bool file_exists_w(const wchar_t* path) {
  WIN32_FILE_ATTRIBUTE_DATA file_data;
  return ::GetFileAttributesExW(path, GetFileExInfoStandard, &file_data);
}

static void create_rel_directory_w(const wchar_t* path) {
  WITH_ABS_PATH(path);
  EXPECT_FALSE(file_exists_w(abs_path)) <<  "Can't create directory: \"" << path << "\" already exists";
  BOOL result = CreateDirectoryW(abs_path, NULL);
  EXPECT_TRUE(result) << "Failed to create directory \"" << path << "\" " << GetLastError();
}

static void delete_empty_rel_directory_w(const wchar_t* path) {
  WITH_ABS_PATH(path);
  EXPECT_TRUE(file_exists_w(abs_path)) << "Can't delete directory: \"" << path << "\" does not exists";
  const int retry_count = 20;

  // If the directory cannot be deleted directly, a file in it might be kept
  // open by a virus scanner. Try a few times, since this should be temporary.
  for (int i = 0; i <= retry_count; ++i) {
    BOOL result = RemoveDirectoryW(abs_path);

    if (!result && (i < retry_count)) {
      Sleep(1);
    } else {
      EXPECT_TRUE(result) << "Failed to delete directory \"" << path << "\": " << GetLastError();
      return;
    }
  }
}

static void create_rel_file_w(const wchar_t* path) {
  WITH_ABS_PATH(path);
  EXPECT_FALSE(file_exists_w(abs_path)) << "Can't create file: \"" << path << "\" already exists";
  HANDLE h = CreateFileW(abs_path, 0, 0, NULL, CREATE_NEW, FILE_ATTRIBUTE_NORMAL, NULL);
  EXPECT_NE(h, INVALID_HANDLE_VALUE) << "Failed to create file \"" << path << "\": " << GetLastError();
  CloseHandle(h);
}

static void delete_rel_file_w(const wchar_t* path) {
  WITH_ABS_PATH(path);
  EXPECT_TRUE(file_exists_w(abs_path)) << "Can't delete file: \"" << path << "\" does not exists";
  BOOL result = DeleteFileW(abs_path);
  EXPECT_TRUE(result) << "Failed to delete file \"" << path << "\": " << GetLastError();
}

static bool convert_to_cstring(char* c_str, size_t size, wchar_t* w_str) {
  size_t converted;
  errno_t err = wcstombs_s(&converted, c_str, size, w_str, size - 1);
  EXPECT_EQ(err, ERROR_SUCCESS) << "Could not convert \"" << w_str << "\" to c-string";

  return err == ERROR_SUCCESS;
}

static wchar_t* my_wcscpy_s(wchar_t* dest, size_t size, wchar_t* start, const wchar_t* to_copy) {
  size_t already_used = dest - start;
  size_t len = wcslen(to_copy);

  if (already_used + len < size) {
    wcscpy_s(dest, size - already_used, to_copy);
  }

  return dest + wcslen(to_copy);
}

// The currently finite list of seperator sequences we might use instead of '\\'.
static const wchar_t* sep_replacements[] = {
  L"\\", L"\\/", L"/", L"//", L"\\\\/\\", L"//\\/"
};

// Takes a path and modifies it in a way that it should still designate the same file.
static bool unnormalize_path(wchar_t* result, size_t size, bool is_dir, const wchar_t* path) {
  wchar_t* dest = result;
  const wchar_t* src = path;
  const wchar_t* path_start;

  if (wcsncmp(src, L"\\\\?\\UNC\\", 8) == 0) {
    path_start = src + 8;
  } else if (wcsncmp(src, L"\\\\?\\", 4) == 0) {
    if (src[5] == L':') {
      path_start = src + 6;
    } else {
      path_start = wcschr(src + 4, L'\\');
    }
  } else if (wcsncmp(src, L"\\\\", 2) == 0) {
    path_start = wcschr(src + 2, L'?');

    if (path_start == NULL) {
      path_start = wcschr(src + 2, L'\\');
    } else {
      path_start = wcschr(path_start, L'\\');
    }
  } else {
    path_start = wcschr(src + 1, L'\\');
  }

  bool allow_sep_change = (mods_filter & Allow_Sep_Mods) && (os::random() & 1) == 0;
  bool allow_dot_change = (mods_filter & Allow_Dot_Path) && (os::random() & 1) == 0;
  bool allow_dotdot_change = (mods_filter & Allow_Dot_Dot_Path) && (os::random() & 1) == 0;

  while ((*src != L'\0') && (result + size > dest)) {
    wchar_t c = *src;
    *dest = c;
    ++src;
    ++dest;

    if (c == L'\\') {
      if (allow_sep_change && (os::random() & 3) == 3) {
        int i = os::random() % (sizeof(sep_replacements) / sizeof(sep_replacements[0]));

        if (i >= 0) {
          const wchar_t* replacement = sep_replacements[i];
          dest = my_wcscpy_s(dest - 1, size,  result, replacement);
        }
      } else if (path_start != NULL) {
        if (allow_dotdot_change && (src > path_start + 1) && ((os::random() & 7) == 7)) {
          wchar_t const* last_sep = src - 2;

          while (last_sep[0] != L'\\') {
            --last_sep;
          }

          if (last_sep > path_start) {
            dest = my_wcscpy_s(dest, size, result, L"../");
            src = last_sep + 1;
          }
        } else if (allow_dot_change && (src > path_start + 1) && ((os::random() & 7) == 7)) {
          dest = my_wcscpy_s(dest, size, result, L"./");
        }
      }
    }
  }

  while (is_dir && ((os::random() & 15) == 1)) {
    dest = my_wcscpy_s(dest, size, result, L"/");
  }

  if (result + size > dest) {
    *dest = L'\0';
  }

  // Use this modification only if not too close to the max size.
  return result + size - 10 > dest;
}

static void check_dir_impl(wchar_t* path, bool should_be_empty) {
  char buf[JVM_MAXPATHLEN];

  if (convert_to_cstring(buf, JVM_MAXPATHLEN, path)) {
    struct stat st;
    EXPECT_EQ(os::stat(buf, &st), 0) << "os::stat failed for \"" << path << "\"";
    EXPECT_EQ(st.st_mode & S_IFMT, S_IFDIR) << "\"" << path << "\" is not a directory according to os::stat";
    errno = ERROR_SUCCESS;
    bool is_empty = os::dir_is_empty(buf);
    errno_t err = errno;
    EXPECT_EQ(is_empty, should_be_empty) << "os::dir_is_empty assumed \"" << path << "\" is "
                                         << (should_be_empty ?  "not ": "") << "empty";
    EXPECT_EQ(err, ERROR_SUCCESS) << "os::dir_is_empty failed for \"" << path << "\"with errno " << err;
  }
}

static void check_file_impl(wchar_t* path) {
  char buf[JVM_MAXPATHLEN];

  if (convert_to_cstring(buf, JVM_MAXPATHLEN, path)) {
    struct stat st;
    EXPECT_EQ(os::stat(buf, &st), 0) << "os::stat failed for \"" << path << "\"";
    EXPECT_EQ(st.st_mode & S_IFMT, S_IFREG) << "\"" << path << "\" is not a regular file according to os::stat";
    int fd = os::open(buf, O_RDONLY, 0);
    EXPECT_NE(fd, -1) << "os::open failed for \"" << path << "\" with errno " << errno;
    if (fd >= 0) {
      ::close(fd);
    }
  }
}

static void check_file_not_present_impl(wchar_t* path) {
  char buf[JVM_MAXPATHLEN];

  if (convert_to_cstring(buf, JVM_MAXPATHLEN, path)) {
    struct stat st;
    int stat_ret;
    EXPECT_EQ(stat_ret = os::stat(buf, &st), -1) << "os::stat did not fail for \"" << path << "\"";
    if (stat_ret != -1) {
      // Only check open if stat not already failed.
      int fd = os::open(buf, O_RDONLY, 0);
      EXPECT_EQ(fd, -1) << "os::open did not fail for \"" << path << "\"";
      if (fd >= 0) {
        ::close(fd);
      }
    }
  }
}

static void check_dir(wchar_t* path, bool should_be_empty) {
  check_dir_impl(path, should_be_empty);

  for (int i = 0; mods_filter != Allow_None && i < mods_per_path; ++i) {
    wchar_t tmp[JVM_MAXPATHLEN];
    if (unnormalize_path(tmp, JVM_MAXPATHLEN, true, path)) {
      check_dir_impl(tmp, should_be_empty);
    }
  }
}

static void check_file(wchar_t* path) {
  check_file_impl(path);

  // Check os::same_files at least somewhat.
  char buf[JVM_MAXPATHLEN];

  if (convert_to_cstring(buf, JVM_MAXPATHLEN, path)) {
    wchar_t mod[JVM_MAXPATHLEN];

    if (unnormalize_path(mod, JVM_MAXPATHLEN, false, path)) {
      char mod_c[JVM_MAXPATHLEN];
      if (convert_to_cstring(mod_c, JVM_MAXPATHLEN, mod)) {
        EXPECT_EQ(os::same_files(buf, mod_c), true) << "os::same files failed for \\" << path << "\" and \"" << mod_c << "\"";
      }
    }
  }

  for (int i = 0; mods_filter != Allow_None && i < mods_per_path; ++i) {
    wchar_t tmp[JVM_MAXPATHLEN];
    if (unnormalize_path(tmp, JVM_MAXPATHLEN, false, path)) {
      check_file_impl(tmp);
    }
  }
}

static void check_file_not_present(wchar_t* path) {
  check_file_not_present_impl(path);

  for (int i = 0; mods_filter != Allow_None && i < mods_per_path; ++i) {
    wchar_t tmp[JVM_MAXPATHLEN];
    if (unnormalize_path(tmp, JVM_MAXPATHLEN, false, path)) {
      check_file_not_present_impl(tmp);
    }
  }
}

static void record_path(char const* name, char const* len_name, wchar_t* path) {
  char buf[JVM_MAXPATHLEN];

  if (convert_to_cstring(buf, JVM_MAXPATHLEN, path)) {
    ::testing::Test::RecordProperty(name, buf);
    os::snprintf(buf, JVM_MAXPATHLEN, "%d", (int) wcslen(path));
    ::testing::Test::RecordProperty(len_name, buf);
  }
}

static void bench_path(wchar_t* path) {
  char buf[JVM_MAXPATHLEN];
  int reps = 100000;

  if (convert_to_cstring(buf, JVM_MAXPATHLEN, path)) {
    jlong wtime[2];

    for (int t = 0; t < 2; ++t) {
      wtime[t] = os::javaTimeNanos();

      for (int i = 0; i < reps; ++i) {
        bool succ = false;
        size_t buf_len = strlen(buf);
        wchar_t* w_path = (wchar_t*) os::malloc(sizeof(wchar_t) * (buf_len + 1), mtInternal);

        if (w_path != NULL) {
          size_t converted_chars;
          if (::mbstowcs_s(&converted_chars, w_path, buf_len + 1, buf, buf_len) == ERROR_SUCCESS) {
            if (t == 1) {
              wchar_t* tmp = (wchar_t*) os::malloc(sizeof(wchar_t) * JVM_MAXPATHLEN, mtInternal);

              if (tmp) {
                if (_wfullpath(tmp, w_path, JVM_MAXPATHLEN)) {
                  succ = true;
                }

                // Note that we really don't use the full path name, but just add the cost of running _wfullpath.
                os::free(tmp);
              }
              if (!succ) {
                printf("Failed fullpathing \"%s\"\n", buf);
                return;
              }
              succ = false;
            }
            HANDLE h = ::CreateFileW(w_path, 0, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS, NULL);

            if (h != INVALID_HANDLE_VALUE) {
              ::CloseHandle(h);
              succ = true;
            }
          }
        }

        os::free(w_path);
        if (!succ) {
          printf("Failed getting W*attr. \"%s\"\n", buf);
          return;
        }
      }

      wtime[t] = os::javaTimeNanos() - wtime[t];
    }

    jlong ctime = os::javaTimeNanos();

    for (int i = 0; i < reps; ++i) {
      HANDLE h = ::CreateFileA(buf, 0, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS, NULL);

      if (h == INVALID_HANDLE_VALUE) {
        return;
      }

      ::CloseHandle(h);
    }

    ctime = os::javaTimeNanos() - ctime;

    printf("\"%s\" %f us for *A, %f us for *W, %f us for *W with fullpath\n", buf,
      0.001 * ctime / reps, 0.001 * wtime[0] / reps, 0.001 * wtime[1] / reps);
  }
}

static void print_attr_result_for_path(wchar_t* path) {
  WIN32_FILE_ATTRIBUTE_DATA file_data;
  struct stat st;
  char buf[JVM_MAXPATHLEN];
  wchar_t abs[JVM_MAXPATHLEN];

  _wfullpath(abs, path, JVM_MAXPATHLEN);
  printf("Checking \"%ls\" (%d chars):\n", path, (int) wcslen(path));
  printf("_wfullpath             %ls (%d chars)\n", abs, (int) wcslen(abs));
  BOOL bret = ::GetFileAttributesExW(path, GetFileExInfoStandard, &file_data);
  printf("GetFileAttributesExW() %s\n", bret ? "success" : "failed");

  if (convert_to_cstring(buf, JVM_MAXPATHLEN, path)) {
    bret = ::GetFileAttributesExA(buf, GetFileExInfoStandard, &file_data);
    printf("GetFileAttributesExA() %s\n", bret ? "success" : "failed");

    bool succ = os::stat(buf, &st) != -1;
    printf("os::stat()             %s\n", succ ? "success" : "failed");
  }
}

static void print_attr_result(wchar_t* format, ...) {
  va_list argptr;
  wchar_t buf[JVM_MAXPATHLEN];

  va_start(argptr, format);
  wvsprintfW(buf, format, argptr);
  print_attr_result_for_path(buf);
  va_end(argptr);
}

#define RECORD_PATH(name) record_path(#name, #name "Len", name)
#define NAME_PART_50 L"01234567890123456789012345678901234567890123456789"
#define NAME_PART_250 NAME_PART_50 NAME_PART_50 NAME_PART_50 NAME_PART_50 NAME_PART_50

// Test which tries to find out if the os::stat, os::open, os::same_files and os::dir_is_empty methods
// can handle long path names correctly.
TEST_VM(os_windows, handle_long_paths) {
  static wchar_t cwd[JVM_MAXPATHLEN];
  static wchar_t nearly_long_rel_path[JVM_MAXPATHLEN];
  static wchar_t long_rel_path[JVM_MAXPATHLEN];
  static wchar_t empty_dir_rel_path[JVM_MAXPATHLEN];
  static wchar_t not_empty_dir_rel_path[JVM_MAXPATHLEN];
  static wchar_t file_rel_path[JVM_MAXPATHLEN];
  static wchar_t nearly_long_file_rel_path[JVM_MAXPATHLEN];
  static wchar_t nearly_long_path[JVM_MAXPATHLEN];
  static wchar_t empty_dir_path[JVM_MAXPATHLEN];
  static wchar_t not_empty_dir_path[JVM_MAXPATHLEN];
  static wchar_t nearly_long_file_path[JVM_MAXPATHLEN];
  static wchar_t file_path[JVM_MAXPATHLEN];
  static wchar_t nearly_long_unc_path[JVM_MAXPATHLEN];
  static wchar_t empty_dir_unc_path[JVM_MAXPATHLEN];
  static wchar_t not_empty_dir_unc_path[JVM_MAXPATHLEN];
  static wchar_t nearly_long_file_unc_path[JVM_MAXPATHLEN];
  static wchar_t file_unc_path[JVM_MAXPATHLEN];
  static wchar_t root_dir_path[JVM_MAXPATHLEN];
  static wchar_t root_rel_dir_path[JVM_MAXPATHLEN];

  wchar_t* dir_prefix = L"os_windows_long_paths_dir_";
  wchar_t* empty_dir_name = L"empty_directory_with_long_path";
  wchar_t* not_empty_dir_name = L"not_empty_directory_with_long_path";
  wchar_t* file_name = L"file";
  wchar_t dir_letter;
  WIN32_FILE_ATTRIBUTE_DATA file_data;
  bool can_test_unc = false;

  get_current_dir_w(cwd, sizeof(cwd) / sizeof(wchar_t));
  dir_letter = (cwd[1] == L':' ? cwd[0] : L'\0');
  int cwd_len = (int) wcslen(cwd);
  int dir_prefix_len = (int) wcslen(dir_prefix);
  int rel_path_len = MAX2(dir_prefix_len, 235 - cwd_len);

  memcpy(nearly_long_rel_path, dir_prefix, sizeof(wchar_t) * dir_prefix_len);

  for (int i = dir_prefix_len; i < rel_path_len; ++i) {
    nearly_long_rel_path[i] = L'L';
  }

  nearly_long_rel_path[rel_path_len] = L'\0';

  wsprintfW(long_rel_path, L"%ls\\%ls", nearly_long_rel_path, NAME_PART_250);
  wsprintfW(empty_dir_rel_path, L"%ls\\%ls", nearly_long_rel_path, empty_dir_name);
  wsprintfW(not_empty_dir_rel_path, L"%ls\\%ls", nearly_long_rel_path, not_empty_dir_name);
  wsprintfW(nearly_long_file_rel_path, L"%ls\\%ls", nearly_long_rel_path, file_name);
  wsprintfW(file_rel_path, L"%ls\\%ls\\%ls", nearly_long_rel_path, not_empty_dir_name, file_name);
  wsprintfW(nearly_long_path, L"\\\\?\\%ls\\%ls", cwd, nearly_long_rel_path);
  wsprintfW(empty_dir_path, L"%ls\\%ls", nearly_long_path, empty_dir_name);
  wsprintfW(not_empty_dir_path, L"%ls\\%ls", nearly_long_path, not_empty_dir_name);
  wsprintfW(nearly_long_file_path, L"%ls\\%ls", nearly_long_path, file_name);
  wsprintfW(file_path, L"%ls\\%ls\\%ls", nearly_long_path, not_empty_dir_name, file_name);
  wsprintfW(nearly_long_unc_path, L"\\\\localhost\\%lc$\\%s", dir_letter, nearly_long_path + 7);
  wsprintfW(empty_dir_unc_path, L"%s\\%s", nearly_long_unc_path, empty_dir_name);
  wsprintfW(not_empty_dir_unc_path, L"%s\\%s", nearly_long_unc_path, not_empty_dir_name);
  wsprintfW(nearly_long_file_unc_path, L"%ls\\%ls", nearly_long_unc_path, file_name);
  wsprintfW(file_unc_path, L"%s\\%s\\%s", nearly_long_unc_path, not_empty_dir_name, file_name);
  wsprintfW(root_dir_path, L"%lc:\\", dir_letter);
  wsprintfW(root_rel_dir_path, L"%lc:", dir_letter);

  RECORD_PATH(long_rel_path);
  RECORD_PATH(nearly_long_rel_path);
  RECORD_PATH(nearly_long_path);
  RECORD_PATH(nearly_long_unc_path);
  RECORD_PATH(empty_dir_rel_path);
  RECORD_PATH(empty_dir_path);
  RECORD_PATH(empty_dir_unc_path);
  RECORD_PATH(not_empty_dir_rel_path);
  RECORD_PATH(not_empty_dir_path);
  RECORD_PATH(not_empty_dir_unc_path);
  RECORD_PATH(nearly_long_file_rel_path);
  RECORD_PATH(nearly_long_file_path);
  RECORD_PATH(nearly_long_file_unc_path);
  RECORD_PATH(file_rel_path);
  RECORD_PATH(file_path);
  RECORD_PATH(file_unc_path);

  create_rel_directory_w(nearly_long_rel_path);
  create_rel_directory_w(long_rel_path);
  create_rel_directory_w(empty_dir_rel_path);
  create_rel_directory_w(not_empty_dir_rel_path);
  create_rel_file_w(nearly_long_file_rel_path);
  create_rel_file_w(file_rel_path);

  // For UNC path test we assume that the current DRIVE has a share
  // called "<DRIVELETTER>$" (so for D: we expect \\localhost\D$ to be
  // the same). Since this is only an assumption, we have to skip
  // the UNC tests if the share is missing.
  if (dir_letter && !::GetFileAttributesExW(nearly_long_unc_path, GetFileExInfoStandard, &file_data)) {
    printf("Disabled UNC path test, since %lc: is not mapped as share %lc$.\n", dir_letter, dir_letter);
  } else {
    can_test_unc = true;
  }

  if (mode == BENCH) {
    bench_path(nearly_long_path + 4);
    bench_path(nearly_long_rel_path);
    bench_path(nearly_long_file_path + 4);
    bench_path(nearly_long_file_rel_path);
  } else if (mode == EXAMPLES) {
    printf("Working directory: %ls", cwd);

    if (dir_letter) {
      static wchar_t top_buf[JVM_MAXPATHLEN];
      wchar_t* top_path = wcschr(cwd + 3, L'\\');

      if (top_path) {
        size_t top_len = (top_path - cwd) - 3;

        memcpy(top_buf, cwd + 3, top_len * 2);
        top_buf[top_len] = L'\0';
        top_path = top_buf;
      }

      print_attr_result(L"%lc:\\", dir_letter);
      print_attr_result(L"%lc:\\.\\", dir_letter);

      if (top_path) {
        print_attr_result(L"%lc:\\%ls\\..\\%ls\\", dir_letter, top_path, top_path);
      }

      print_attr_result(L"%lc:", dir_letter);
      print_attr_result(L"%lc:.", dir_letter);
      print_attr_result(L"%lc:\\COM1", dir_letter);
      print_attr_result(L"%lc:\\PRN", dir_letter);
      print_attr_result(L"%lc:\\PRN\\COM1", dir_letter);
      print_attr_result(L"\\\\?\\UNC\\localhost\\%lc$\\", dir_letter);
      print_attr_result(L"\\\\?\\UNC\\\\localhost\\%lc$\\", dir_letter);
      print_attr_result(nearly_long_unc_path);
      print_attr_result(L"%ls\\.\\", nearly_long_unc_path);
      print_attr_result(L"%ls\\..\\%ls", nearly_long_unc_path, nearly_long_rel_path);
      print_attr_result(L"\\\\?\\UNC\\%ls", nearly_long_unc_path + 2);
      print_attr_result(file_unc_path);
      print_attr_result(L"%ls\\%ls\\..\\%ls\\%ls", nearly_long_unc_path, not_empty_dir_name, not_empty_dir_name, file_name);
      print_attr_result(L"%ls\\%ls\\.\\%ls", nearly_long_unc_path, not_empty_dir_name, file_name);
      print_attr_result(L"\\\\?\\UNC\\%ls", file_unc_path + 2);
      print_attr_result(L"\\\\?\\UNC\\%ls\\%ls\\.\\%ls", nearly_long_unc_path + 2, not_empty_dir_name, file_name);
      print_attr_result(L"\\\\?\\UNC\\%ls\\%ls\\..\\%ls\\%ls", nearly_long_unc_path + 2, not_empty_dir_name, not_empty_dir_name, file_name);
    }

    print_attr_result(nearly_long_rel_path);
    print_attr_result(L"%ls\\.\\", nearly_long_rel_path);
    print_attr_result(L"%ls\\..\\%ls", nearly_long_rel_path, nearly_long_rel_path);
    print_attr_result(L"%\\\\?\\%ls", nearly_long_rel_path);
    print_attr_result(L"\\\\?\\%ls\\.\\", nearly_long_rel_path);
    print_attr_result(L"\\\\?\\%ls\\..\\%ls", nearly_long_rel_path, nearly_long_rel_path);

    print_attr_result(nearly_long_path + 4);
    print_attr_result(L"%ls\\.\\", nearly_long_path + 4);
    print_attr_result(L"%ls\\..\\%ls", nearly_long_path + 4, nearly_long_rel_path);
    print_attr_result(nearly_long_path);
    print_attr_result(L"%ls\\.\\", nearly_long_path);
    print_attr_result(L"%ls\\..\\%ls", nearly_long_path, nearly_long_rel_path);
  } else {
    check_file_not_present(L"");

    // Check relative paths
    check_dir(nearly_long_rel_path, false);
    check_dir(long_rel_path, true);
    check_dir(empty_dir_rel_path, true);
    check_dir(not_empty_dir_rel_path, false);
    check_file(nearly_long_file_rel_path);
    check_file(file_rel_path);

    // Check absolute paths
    if (dir_letter) {
      check_dir(root_dir_path, false);
      check_dir(root_rel_dir_path, false);
    }

    check_dir(cwd, false);
    check_dir(nearly_long_path + 4, false);
    check_dir(empty_dir_path + 4, true);
    check_dir(not_empty_dir_path + 4, false);
    check_file(nearly_long_file_path + 4);
    check_file(file_path + 4);

    // Check UNC paths
    if (can_test_unc) {
      check_dir(nearly_long_unc_path, false);
      check_dir(empty_dir_unc_path, true);
      check_dir(not_empty_dir_unc_path, false);
      check_file(nearly_long_file_unc_path);
      check_file(file_unc_path);
    }

    // Check handling of <DRIVE>:/../<OTHER_DRIVE>:/path/...
    // The other drive letter should not overwrite the original one.
    if (dir_letter) {
      static wchar_t tmp[JVM_MAXPATHLEN];
      wchar_t* other_letter = dir_letter == L'D' ? L"C" : L"D";
      wsprintfW(tmp, L"%2ls\\..\\%ls:%ls", nearly_long_file_path, other_letter, nearly_long_file_path + 2);
      check_file_not_present(tmp);
      wsprintfW(tmp, L"%2ls\\..\\%ls:%ls", file_path, other_letter, file_path + 2);
      check_file_not_present(tmp);
    }
  }

  delete_rel_file_w(file_rel_path);
  delete_rel_file_w(nearly_long_file_rel_path);
  delete_empty_rel_directory_w(not_empty_dir_rel_path);
  delete_empty_rel_directory_w(empty_dir_rel_path);
  delete_empty_rel_directory_w(long_rel_path);
  delete_empty_rel_directory_w(nearly_long_rel_path);
}

TEST_VM(os_windows, reserve_memory_special) {
  TestReserveMemorySpecial_test();
}

class ReserveMemorySpecialRunnable : public TestRunnable {
public:
  void runUnitTest() const {
    TestReserveMemorySpecial_test();
  }
};

TEST_VM(os_windows, reserve_memory_special_concurrent) {
  ReserveMemorySpecialRunnable runnable;
  ConcurrentTestRunner testRunner(&runnable, 30, 15000);
  testRunner.run();
}

#endif
