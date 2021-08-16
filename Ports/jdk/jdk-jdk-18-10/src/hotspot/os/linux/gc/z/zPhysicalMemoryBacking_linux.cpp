/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gcLogPrecious.hpp"
#include "gc/z/zArray.inline.hpp"
#include "gc/z/zErrno.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zLargePages.inline.hpp"
#include "gc/z/zMountPoint_linux.hpp"
#include "gc/z/zNUMA.inline.hpp"
#include "gc/z/zPhysicalMemoryBacking_linux.hpp"
#include "gc/z/zSyscall_linux.hpp"
#include "logging/log.hpp"
#include "runtime/init.hpp"
#include "runtime/os.hpp"
#include "runtime/safefetch.inline.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/growableArray.hpp"

#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/statfs.h>
#include <sys/types.h>
#include <unistd.h>

//
// Support for building on older Linux systems
//

// memfd_create(2) flags
#ifndef MFD_CLOEXEC
#define MFD_CLOEXEC                      0x0001U
#endif
#ifndef MFD_HUGETLB
#define MFD_HUGETLB                      0x0004U
#endif

// open(2) flags
#ifndef O_CLOEXEC
#define O_CLOEXEC                        02000000
#endif
#ifndef O_TMPFILE
#define O_TMPFILE                        (020000000 | O_DIRECTORY)
#endif

// fallocate(2) flags
#ifndef FALLOC_FL_KEEP_SIZE
#define FALLOC_FL_KEEP_SIZE              0x01
#endif
#ifndef FALLOC_FL_PUNCH_HOLE
#define FALLOC_FL_PUNCH_HOLE             0x02
#endif

// Filesystem types, see statfs(2)
#ifndef TMPFS_MAGIC
#define TMPFS_MAGIC                      0x01021994
#endif
#ifndef HUGETLBFS_MAGIC
#define HUGETLBFS_MAGIC                  0x958458f6
#endif

// Filesystem names
#define ZFILESYSTEM_TMPFS                "tmpfs"
#define ZFILESYSTEM_HUGETLBFS            "hugetlbfs"

// Proc file entry for max map mount
#define ZFILENAME_PROC_MAX_MAP_COUNT     "/proc/sys/vm/max_map_count"

// Sysfs file for transparent huge page on tmpfs
#define ZFILENAME_SHMEM_ENABLED          "/sys/kernel/mm/transparent_hugepage/shmem_enabled"

// Java heap filename
#define ZFILENAME_HEAP                   "java_heap"

// Preferred tmpfs mount points, ordered by priority
static const char* z_preferred_tmpfs_mountpoints[] = {
  "/dev/shm",
  "/run/shm",
  NULL
};

// Preferred hugetlbfs mount points, ordered by priority
static const char* z_preferred_hugetlbfs_mountpoints[] = {
  "/dev/hugepages",
  "/hugepages",
  NULL
};

static int z_fallocate_hugetlbfs_attempts = 3;
static bool z_fallocate_supported = true;

ZPhysicalMemoryBacking::ZPhysicalMemoryBacking(size_t max_capacity) :
    _fd(-1),
    _filesystem(0),
    _block_size(0),
    _available(0),
    _initialized(false) {

  // Create backing file
  _fd = create_fd(ZFILENAME_HEAP);
  if (_fd == -1) {
    return;
  }

  // Truncate backing file
  while (ftruncate(_fd, max_capacity) == -1) {
    if (errno != EINTR) {
      ZErrno err;
      log_error_p(gc)("Failed to truncate backing file (%s)", err.to_string());
      return;
    }
  }

  // Get filesystem statistics
  struct statfs buf;
  if (fstatfs(_fd, &buf) == -1) {
    ZErrno err;
    log_error_p(gc)("Failed to determine filesystem type for backing file (%s)", err.to_string());
    return;
  }

  _filesystem = buf.f_type;
  _block_size = buf.f_bsize;
  _available = buf.f_bavail * _block_size;

  log_info_p(gc, init)("Heap Backing Filesystem: %s (0x" UINT64_FORMAT_X ")",
                       is_tmpfs() ? ZFILESYSTEM_TMPFS : is_hugetlbfs() ? ZFILESYSTEM_HUGETLBFS : "other", _filesystem);

  // Make sure the filesystem type matches requested large page type
  if (ZLargePages::is_transparent() && !is_tmpfs()) {
    log_error_p(gc)("-XX:+UseTransparentHugePages can only be enabled when using a %s filesystem",
                    ZFILESYSTEM_TMPFS);
    return;
  }

  if (ZLargePages::is_transparent() && !tmpfs_supports_transparent_huge_pages()) {
    log_error_p(gc)("-XX:+UseTransparentHugePages on a %s filesystem not supported by kernel",
                    ZFILESYSTEM_TMPFS);
    return;
  }

  if (ZLargePages::is_explicit() && !is_hugetlbfs()) {
    log_error_p(gc)("-XX:+UseLargePages (without -XX:+UseTransparentHugePages) can only be enabled "
                    "when using a %s filesystem", ZFILESYSTEM_HUGETLBFS);
    return;
  }

  if (!ZLargePages::is_explicit() && is_hugetlbfs()) {
    log_error_p(gc)("-XX:+UseLargePages must be enabled when using a %s filesystem",
                    ZFILESYSTEM_HUGETLBFS);
    return;
  }

  if (ZLargePages::is_explicit() && os::large_page_size() != ZGranuleSize) {
    log_error_p(gc)("Incompatible large page size configured " SIZE_FORMAT " (expected " SIZE_FORMAT ")",
                    os::large_page_size(), ZGranuleSize);
    return;
  }

  // Make sure the filesystem block size is compatible
  if (ZGranuleSize % _block_size != 0) {
    log_error_p(gc)("Filesystem backing the heap has incompatible block size (" SIZE_FORMAT ")",
                    _block_size);
    return;
  }

  if (is_hugetlbfs() && _block_size != ZGranuleSize) {
    log_error_p(gc)("%s filesystem has unexpected block size " SIZE_FORMAT " (expected " SIZE_FORMAT ")",
                    ZFILESYSTEM_HUGETLBFS, _block_size, ZGranuleSize);
    return;
  }

  // Successfully initialized
  _initialized = true;
}

int ZPhysicalMemoryBacking::create_mem_fd(const char* name) const {
  // Create file name
  char filename[PATH_MAX];
  snprintf(filename, sizeof(filename), "%s%s", name, ZLargePages::is_explicit() ? ".hugetlb" : "");

  // Create file
  const int extra_flags = ZLargePages::is_explicit() ? MFD_HUGETLB : 0;
  const int fd = ZSyscall::memfd_create(filename, MFD_CLOEXEC | extra_flags);
  if (fd == -1) {
    ZErrno err;
    log_debug_p(gc, init)("Failed to create memfd file (%s)",
                          ((ZLargePages::is_explicit() && err == EINVAL) ? "Hugepages not supported" : err.to_string()));
    return -1;
  }

  log_info_p(gc, init)("Heap Backing File: /memfd:%s", filename);

  return fd;
}

int ZPhysicalMemoryBacking::create_file_fd(const char* name) const {
  const char* const filesystem = ZLargePages::is_explicit()
                                 ? ZFILESYSTEM_HUGETLBFS
                                 : ZFILESYSTEM_TMPFS;
  const char** const preferred_mountpoints = ZLargePages::is_explicit()
                                             ? z_preferred_hugetlbfs_mountpoints
                                             : z_preferred_tmpfs_mountpoints;

  // Find mountpoint
  ZMountPoint mountpoint(filesystem, preferred_mountpoints);
  if (mountpoint.get() == NULL) {
    log_error_p(gc)("Use -XX:AllocateHeapAt to specify the path to a %s filesystem", filesystem);
    return -1;
  }

  // Try to create an anonymous file using the O_TMPFILE flag. Note that this
  // flag requires kernel >= 3.11. If this fails we fall back to open/unlink.
  const int fd_anon = os::open(mountpoint.get(), O_TMPFILE|O_EXCL|O_RDWR|O_CLOEXEC, S_IRUSR|S_IWUSR);
  if (fd_anon == -1) {
    ZErrno err;
    log_debug_p(gc, init)("Failed to create anonymous file in %s (%s)", mountpoint.get(),
                          (err == EINVAL ? "Not supported" : err.to_string()));
  } else {
    // Get inode number for anonymous file
    struct stat stat_buf;
    if (fstat(fd_anon, &stat_buf) == -1) {
      ZErrno err;
      log_error_pd(gc)("Failed to determine inode number for anonymous file (%s)", err.to_string());
      return -1;
    }

    log_info_p(gc, init)("Heap Backing File: %s/#" UINT64_FORMAT, mountpoint.get(), (uint64_t)stat_buf.st_ino);

    return fd_anon;
  }

  log_debug_p(gc, init)("Falling back to open/unlink");

  // Create file name
  char filename[PATH_MAX];
  snprintf(filename, sizeof(filename), "%s/%s.%d", mountpoint.get(), name, os::current_process_id());

  // Create file
  const int fd = os::open(filename, O_CREAT|O_EXCL|O_RDWR|O_CLOEXEC, S_IRUSR|S_IWUSR);
  if (fd == -1) {
    ZErrno err;
    log_error_p(gc)("Failed to create file %s (%s)", filename, err.to_string());
    return -1;
  }

  // Unlink file
  if (unlink(filename) == -1) {
    ZErrno err;
    log_error_p(gc)("Failed to unlink file %s (%s)", filename, err.to_string());
    return -1;
  }

  log_info_p(gc, init)("Heap Backing File: %s", filename);

  return fd;
}

int ZPhysicalMemoryBacking::create_fd(const char* name) const {
  if (AllocateHeapAt == NULL) {
    // If the path is not explicitly specified, then we first try to create a memfd file
    // instead of looking for a tmpfd/hugetlbfs mount point. Note that memfd_create() might
    // not be supported at all (requires kernel >= 3.17), or it might not support large
    // pages (requires kernel >= 4.14). If memfd_create() fails, then we try to create a
    // file on an accessible tmpfs or hugetlbfs mount point.
    const int fd = create_mem_fd(name);
    if (fd != -1) {
      return fd;
    }

    log_debug_p(gc)("Falling back to searching for an accessible mount point");
  }

  return create_file_fd(name);
}

bool ZPhysicalMemoryBacking::is_initialized() const {
  return _initialized;
}

void ZPhysicalMemoryBacking::warn_available_space(size_t max_capacity) const {
  // Note that the available space on a tmpfs or a hugetlbfs filesystem
  // will be zero if no size limit was specified when it was mounted.
  if (_available == 0) {
    // No size limit set, skip check
    log_info_p(gc, init)("Available space on backing filesystem: N/A");
    return;
  }

  log_info_p(gc, init)("Available space on backing filesystem: " SIZE_FORMAT "M", _available / M);

  // Warn if the filesystem doesn't currently have enough space available to hold
  // the max heap size. The max heap size will be capped if we later hit this limit
  // when trying to expand the heap.
  if (_available < max_capacity) {
    log_warning_p(gc)("***** WARNING! INCORRECT SYSTEM CONFIGURATION DETECTED! *****");
    log_warning_p(gc)("Not enough space available on the backing filesystem to hold the current max Java heap");
    log_warning_p(gc)("size (" SIZE_FORMAT "M). Please adjust the size of the backing filesystem accordingly "
                      "(available", max_capacity / M);
    log_warning_p(gc)("space is currently " SIZE_FORMAT "M). Continuing execution with the current filesystem "
                      "size could", _available / M);
    log_warning_p(gc)("lead to a premature OutOfMemoryError being thrown, due to failure to commit memory.");
  }
}

void ZPhysicalMemoryBacking::warn_max_map_count(size_t max_capacity) const {
  const char* const filename = ZFILENAME_PROC_MAX_MAP_COUNT;
  FILE* const file = fopen(filename, "r");
  if (file == NULL) {
    // Failed to open file, skip check
    log_debug_p(gc, init)("Failed to open %s", filename);
    return;
  }

  size_t actual_max_map_count = 0;
  const int result = fscanf(file, SIZE_FORMAT, &actual_max_map_count);
  fclose(file);
  if (result != 1) {
    // Failed to read file, skip check
    log_debug_p(gc, init)("Failed to read %s", filename);
    return;
  }

  // The required max map count is impossible to calculate exactly since subsystems
  // other than ZGC are also creating memory mappings, and we have no control over that.
  // However, ZGC tends to create the most mappings and dominate the total count.
  // In the worst cases, ZGC will map each granule three times, i.e. once per heap view.
  // We speculate that we need another 20% to allow for non-ZGC subsystems to map memory.
  const size_t required_max_map_count = (max_capacity / ZGranuleSize) * 3 * 1.2;
  if (actual_max_map_count < required_max_map_count) {
    log_warning_p(gc)("***** WARNING! INCORRECT SYSTEM CONFIGURATION DETECTED! *****");
    log_warning_p(gc)("The system limit on number of memory mappings per process might be too low for the given");
    log_warning_p(gc)("max Java heap size (" SIZE_FORMAT "M). Please adjust %s to allow for at",
                      max_capacity / M, filename);
    log_warning_p(gc)("least " SIZE_FORMAT " mappings (current limit is " SIZE_FORMAT "). Continuing execution "
                      "with the current", required_max_map_count, actual_max_map_count);
    log_warning_p(gc)("limit could lead to a premature OutOfMemoryError being thrown, due to failure to map memory.");
  }
}

void ZPhysicalMemoryBacking::warn_commit_limits(size_t max_capacity) const {
  // Warn if available space is too low
  warn_available_space(max_capacity);

  // Warn if max map count is too low
  warn_max_map_count(max_capacity);
}

bool ZPhysicalMemoryBacking::is_tmpfs() const {
  return _filesystem == TMPFS_MAGIC;
}

bool ZPhysicalMemoryBacking::is_hugetlbfs() const {
  return _filesystem == HUGETLBFS_MAGIC;
}

bool ZPhysicalMemoryBacking::tmpfs_supports_transparent_huge_pages() const {
  // If the shmem_enabled file exists and is readable then we
  // know the kernel supports transparent huge pages for tmpfs.
  return access(ZFILENAME_SHMEM_ENABLED, R_OK) == 0;
}

ZErrno ZPhysicalMemoryBacking::fallocate_compat_mmap_hugetlbfs(size_t offset, size_t length, bool touch) const {
  // On hugetlbfs, mapping a file segment will fail immediately, without
  // the need to touch the mapped pages first, if there aren't enough huge
  // pages available to back the mapping.
  void* const addr = mmap(0, length, PROT_READ|PROT_WRITE, MAP_SHARED, _fd, offset);
  if (addr == MAP_FAILED) {
    // Failed
    return errno;
  }

  // Once mapped, the huge pages are only reserved. We need to touch them
  // to associate them with the file segment. Note that we can not punch
  // hole in file segments which only have reserved pages.
  if (touch) {
    char* const start = (char*)addr;
    char* const end = start + length;
    os::pretouch_memory(start, end, _block_size);
  }

  // Unmap again. From now on, the huge pages that were mapped are allocated
  // to this file. There's no risk of getting a SIGBUS when mapping and
  // touching these pages again.
  if (munmap(addr, length) == -1) {
    // Failed
    return errno;
  }

  // Success
  return 0;
}

static bool safe_touch_mapping(void* addr, size_t length, size_t page_size) {
  char* const start = (char*)addr;
  char* const end = start + length;

  // Touching a mapping that can't be backed by memory will generate a
  // SIGBUS. By using SafeFetch32 any SIGBUS will be safely caught and
  // handled. On tmpfs, doing a fetch (rather than a store) is enough
  // to cause backing pages to be allocated (there's no zero-page to
  // worry about).
  for (char *p = start; p < end; p += page_size) {
    if (SafeFetch32((int*)p, -1) == -1) {
      // Failed
      return false;
    }
  }

  // Success
  return true;
}

ZErrno ZPhysicalMemoryBacking::fallocate_compat_mmap_tmpfs(size_t offset, size_t length) const {
  // On tmpfs, we need to touch the mapped pages to figure out
  // if there are enough pages available to back the mapping.
  void* const addr = mmap(0, length, PROT_READ|PROT_WRITE, MAP_SHARED, _fd, offset);
  if (addr == MAP_FAILED) {
    // Failed
    return errno;
  }

  // Advise mapping to use transparent huge pages
  os::realign_memory((char*)addr, length, os::large_page_size());

  // Touch the mapping (safely) to make sure it's backed by memory
  const bool backed = safe_touch_mapping(addr, length, _block_size);

  // Unmap again. If successfully touched, the backing memory will
  // be allocated to this file. There's no risk of getting a SIGBUS
  // when mapping and touching these pages again.
  if (munmap(addr, length) == -1) {
    // Failed
    return errno;
  }

  // Success
  return backed ? 0 : ENOMEM;
}

ZErrno ZPhysicalMemoryBacking::fallocate_compat_pwrite(size_t offset, size_t length) const {
  uint8_t data = 0;

  // Allocate backing memory by writing to each block
  for (size_t pos = offset; pos < offset + length; pos += _block_size) {
    if (pwrite(_fd, &data, sizeof(data), pos) == -1) {
      // Failed
      return errno;
    }
  }

  // Success
  return 0;
}

ZErrno ZPhysicalMemoryBacking::fallocate_fill_hole_compat(size_t offset, size_t length) const {
  // fallocate(2) is only supported by tmpfs since Linux 3.5, and by hugetlbfs
  // since Linux 4.3. When fallocate(2) is not supported we emulate it using
  // mmap/munmap (for hugetlbfs and tmpfs with transparent huge pages) or pwrite
  // (for tmpfs without transparent huge pages and other filesystem types).
  if (ZLargePages::is_explicit()) {
    return fallocate_compat_mmap_hugetlbfs(offset, length, false /* touch */);
  } else if (ZLargePages::is_transparent()) {
    return fallocate_compat_mmap_tmpfs(offset, length);
  } else {
    return fallocate_compat_pwrite(offset, length);
  }
}

ZErrno ZPhysicalMemoryBacking::fallocate_fill_hole_syscall(size_t offset, size_t length) const {
  const int mode = 0; // Allocate
  const int res = ZSyscall::fallocate(_fd, mode, offset, length);
  if (res == -1) {
    // Failed
    return errno;
  }

  // Success
  return 0;
}

ZErrno ZPhysicalMemoryBacking::fallocate_fill_hole(size_t offset, size_t length) const {
  // Using compat mode is more efficient when allocating space on hugetlbfs.
  // Note that allocating huge pages this way will only reserve them, and not
  // associate them with segments of the file. We must guarantee that we at
  // some point touch these segments, otherwise we can not punch hole in them.
  // Also note that we need to use compat mode when using transparent huge pages,
  // since we need to use madvise(2) on the mapping before the page is allocated.
  if (z_fallocate_supported && !ZLargePages::is_enabled()) {
     const ZErrno err = fallocate_fill_hole_syscall(offset, length);
     if (!err) {
       // Success
       return 0;
     }

     if (err != ENOSYS && err != EOPNOTSUPP) {
       // Failed
       return err;
     }

     // Not supported
     log_debug_p(gc)("Falling back to fallocate() compatibility mode");
     z_fallocate_supported = false;
  }

  return fallocate_fill_hole_compat(offset, length);
}

ZErrno ZPhysicalMemoryBacking::fallocate_punch_hole(size_t offset, size_t length) const {
  if (ZLargePages::is_explicit()) {
    // We can only punch hole in pages that have been touched. Non-touched
    // pages are only reserved, and not associated with any specific file
    // segment. We don't know which pages have been previously touched, so
    // we always touch them here to guarantee that we can punch hole.
    const ZErrno err = fallocate_compat_mmap_hugetlbfs(offset, length, true /* touch */);
    if (err) {
      // Failed
      return err;
    }
  }

  const int mode = FALLOC_FL_PUNCH_HOLE|FALLOC_FL_KEEP_SIZE;
  if (ZSyscall::fallocate(_fd, mode, offset, length) == -1) {
    // Failed
    return errno;
  }

  // Success
  return 0;
}

ZErrno ZPhysicalMemoryBacking::split_and_fallocate(bool punch_hole, size_t offset, size_t length) const {
  // Try first half
  const size_t offset0 = offset;
  const size_t length0 = align_up(length / 2, _block_size);
  const ZErrno err0 = fallocate(punch_hole, offset0, length0);
  if (err0) {
    return err0;
  }

  // Try second half
  const size_t offset1 = offset0 + length0;
  const size_t length1 = length - length0;
  const ZErrno err1 = fallocate(punch_hole, offset1, length1);
  if (err1) {
    return err1;
  }

  // Success
  return 0;
}

ZErrno ZPhysicalMemoryBacking::fallocate(bool punch_hole, size_t offset, size_t length) const {
  assert(is_aligned(offset, _block_size), "Invalid offset");
  assert(is_aligned(length, _block_size), "Invalid length");

  const ZErrno err = punch_hole ? fallocate_punch_hole(offset, length) : fallocate_fill_hole(offset, length);
  if (err == EINTR && length > _block_size) {
    // Calling fallocate(2) with a large length can take a long time to
    // complete. When running profilers, such as VTune, this syscall will
    // be constantly interrupted by signals. Expanding the file in smaller
    // steps avoids this problem.
    return split_and_fallocate(punch_hole, offset, length);
  }

  return err;
}

bool ZPhysicalMemoryBacking::commit_inner(size_t offset, size_t length) const {
  log_trace(gc, heap)("Committing memory: " SIZE_FORMAT "M-" SIZE_FORMAT "M (" SIZE_FORMAT "M)",
                      offset / M, (offset + length) / M, length / M);

retry:
  const ZErrno err = fallocate(false /* punch_hole */, offset, length);
  if (err) {
    if (err == ENOSPC && !is_init_completed() && ZLargePages::is_explicit() && z_fallocate_hugetlbfs_attempts-- > 0) {
      // If we fail to allocate during initialization, due to lack of space on
      // the hugetlbfs filesystem, then we wait and retry a few times before
      // giving up. Otherwise there is a risk that running JVMs back-to-back
      // will fail, since there is a delay between process termination and the
      // huge pages owned by that process being returned to the huge page pool
      // and made available for new allocations.
      log_debug_p(gc, init)("Failed to commit memory (%s), retrying", err.to_string());

      // Wait and retry in one second, in the hope that huge pages will be
      // available by then.
      sleep(1);
      goto retry;
    }

    // Failed
    log_error_p(gc)("Failed to commit memory (%s)", err.to_string());
    return false;
  }

  // Success
  return true;
}

static int offset_to_node(size_t offset) {
  const GrowableArray<int>* mapping = os::Linux::numa_nindex_to_node();
  const size_t nindex = (offset >> ZGranuleSizeShift) % mapping->length();
  return mapping->at((int)nindex);
}

size_t ZPhysicalMemoryBacking::commit_numa_interleaved(size_t offset, size_t length) const {
  size_t committed = 0;

  // Commit one granule at a time, so that each granule
  // can be allocated from a different preferred node.
  while (committed < length) {
    const size_t granule_offset = offset + committed;

    // Setup NUMA policy to allocate memory from a preferred node
    os::Linux::numa_set_preferred(offset_to_node(granule_offset));

    if (!commit_inner(granule_offset, ZGranuleSize)) {
      // Failed
      break;
    }

    committed += ZGranuleSize;
  }

  // Restore NUMA policy
  os::Linux::numa_set_preferred(-1);

  return committed;
}

size_t ZPhysicalMemoryBacking::commit_default(size_t offset, size_t length) const {
  // Try to commit the whole region
  if (commit_inner(offset, length)) {
    // Success
    return length;
  }

  // Failed, try to commit as much as possible
  size_t start = offset;
  size_t end = offset + length;

  for (;;) {
    length = align_down((end - start) / 2, ZGranuleSize);
    if (length < ZGranuleSize) {
      // Done, don't commit more
      return start - offset;
    }

    if (commit_inner(start, length)) {
      // Success, try commit more
      start += length;
    } else {
      // Failed, try commit less
      end -= length;
    }
  }
}

size_t ZPhysicalMemoryBacking::commit(size_t offset, size_t length) const {
  if (ZNUMA::is_enabled() && !ZLargePages::is_explicit()) {
    // To get granule-level NUMA interleaving when using non-large pages,
    // we must explicitly interleave the memory at commit/fallocate time.
    return commit_numa_interleaved(offset, length);
  }

  return commit_default(offset, length);
}

size_t ZPhysicalMemoryBacking::uncommit(size_t offset, size_t length) const {
  log_trace(gc, heap)("Uncommitting memory: " SIZE_FORMAT "M-" SIZE_FORMAT "M (" SIZE_FORMAT "M)",
                      offset / M, (offset + length) / M, length / M);

  const ZErrno err = fallocate(true /* punch_hole */, offset, length);
  if (err) {
    log_error(gc)("Failed to uncommit memory (%s)", err.to_string());
    return 0;
  }

  return length;
}

void ZPhysicalMemoryBacking::map(uintptr_t addr, size_t size, uintptr_t offset) const {
  const void* const res = mmap((void*)addr, size, PROT_READ|PROT_WRITE, MAP_FIXED|MAP_SHARED, _fd, offset);
  if (res == MAP_FAILED) {
    ZErrno err;
    fatal("Failed to map memory (%s)", err.to_string());
  }
}

void ZPhysicalMemoryBacking::unmap(uintptr_t addr, size_t size) const {
  // Note that we must keep the address space reservation intact and just detach
  // the backing memory. For this reason we map a new anonymous, non-accessible
  // and non-reserved page over the mapping instead of actually unmapping.
  const void* const res = mmap((void*)addr, size, PROT_NONE, MAP_FIXED | MAP_ANONYMOUS | MAP_PRIVATE | MAP_NORESERVE, -1, 0);
  if (res == MAP_FAILED) {
    ZErrno err;
    fatal("Failed to map memory (%s)", err.to_string());
  }
}
