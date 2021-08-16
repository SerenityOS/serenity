/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include "precompiled.hpp"
#include "classfile/vmSymbols.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.inline.hpp"
#include "os_windows.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/os.hpp"
#include "runtime/perfMemory.hpp"
#include "services/memTracker.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/formatBuffer.hpp"

#include <windows.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <lmcons.h>

typedef BOOL (WINAPI *SetSecurityDescriptorControlFnPtr)(
   IN PSECURITY_DESCRIPTOR pSecurityDescriptor,
   IN SECURITY_DESCRIPTOR_CONTROL ControlBitsOfInterest,
   IN SECURITY_DESCRIPTOR_CONTROL ControlBitsToSet);

// Standard Memory Implementation Details

// create the PerfData memory region in standard memory.
//
static char* create_standard_memory(size_t size) {

  // allocate an aligned chuck of memory
  char* mapAddress = os::reserve_memory(size);

  if (mapAddress == NULL) {
    return NULL;
  }

  // commit memory
  if (!os::commit_memory(mapAddress, size, !ExecMem)) {
    if (PrintMiscellaneous && Verbose) {
      warning("Could not commit PerfData memory\n");
    }
    os::release_memory(mapAddress, size);
    return NULL;
  }

  return mapAddress;
}

// delete the PerfData memory region
//
static void delete_standard_memory(char* addr, size_t size) {

  // there are no persistent external resources to cleanup for standard
  // memory. since DestroyJavaVM does not support unloading of the JVM,
  // cleanup of the memory resource is not performed. The memory will be
  // reclaimed by the OS upon termination of the process.
  //
  return;

}

// save the specified memory region to the given file
//
static void save_memory_to_file(char* addr, size_t size) {

  const char* destfile = PerfMemory::get_perfdata_file_path();
  assert(destfile[0] != '\0', "invalid Perfdata file path");

  int fd = ::_open(destfile, _O_BINARY|_O_CREAT|_O_WRONLY|_O_TRUNC,
                   _S_IREAD|_S_IWRITE);

  if (fd == OS_ERR) {
    if (PrintMiscellaneous && Verbose) {
      warning("Could not create Perfdata save file: %s: %s\n",
              destfile, os::strerror(errno));
    }
  } else {
    for (size_t remaining = size; remaining > 0;) {

      int nbytes = ::_write(fd, addr, (unsigned int)remaining);
      if (nbytes == OS_ERR) {
        if (PrintMiscellaneous && Verbose) {
          warning("Could not write Perfdata save file: %s: %s\n",
                  destfile, os::strerror(errno));
        }
        break;
      }

      remaining -= (size_t)nbytes;
      addr += nbytes;
    }

    int result = ::_close(fd);
    if (PrintMiscellaneous && Verbose) {
      if (result == OS_ERR) {
        warning("Could not close %s: %s\n", destfile, os::strerror(errno));
      }
    }
  }

  FREE_C_HEAP_ARRAY(char, destfile);
}

// Shared Memory Implementation Details

// Note: the win32 shared memory implementation uses two objects to represent
// the shared memory: a windows kernel based file mapping object and a backing
// store file. On windows, the name space for shared memory is a kernel
// based name space that is disjoint from other win32 name spaces. Since Java
// is unaware of this name space, a parallel file system based name space is
// maintained, which provides a common file system based shared memory name
// space across the supported platforms and one that Java apps can deal with
// through simple file apis.
//
// For performance and resource cleanup reasons, it is recommended that the
// user specific directory and the backing store file be stored in either a
// RAM based file system or a local disk based file system. Network based
// file systems are not recommended for performance reasons. In addition,
// use of SMB network based file systems may result in unsuccesful cleanup
// of the disk based resource on exit of the VM. The Windows TMP and TEMP
// environement variables, as used by the GetTempPath() Win32 API (see
// os::get_temp_directory() in os_win32.cpp), control the location of the
// user specific directory and the shared memory backing store file.

static HANDLE sharedmem_fileMapHandle = NULL;
static HANDLE sharedmem_fileHandle = INVALID_HANDLE_VALUE;
static char*  sharedmem_fileName = NULL;

// return the user specific temporary directory name.
//
// the caller is expected to free the allocated memory.
//
static char* get_user_tmp_dir(const char* user) {

  const char* tmpdir = os::get_temp_directory();
  const char* perfdir = PERFDATA_NAME;
  size_t nbytes = strlen(tmpdir) + strlen(perfdir) + strlen(user) + 3;
  char* dirname = NEW_C_HEAP_ARRAY(char, nbytes, mtInternal);

  // construct the path name to user specific tmp directory
  _snprintf(dirname, nbytes, "%s\\%s_%s", tmpdir, perfdir, user);

  return dirname;
}

// convert the given file name into a process id. if the file
// does not meet the file naming constraints, return 0.
//
static int filename_to_pid(const char* filename) {

  // a filename that doesn't begin with a digit is not a
  // candidate for conversion.
  //
  if (!isdigit(*filename)) {
    return 0;
  }

  // check if file name can be converted to an integer without
  // any leftover characters.
  //
  char* remainder = NULL;
  errno = 0;
  int pid = (int)strtol(filename, &remainder, 10);

  if (errno != 0) {
    return 0;
  }

  // check for left over characters. If any, then the filename is
  // not a candidate for conversion.
  //
  if (remainder != NULL && *remainder != '\0') {
    return 0;
  }

  // successful conversion, return the pid
  return pid;
}

// check if the given path is considered a secure directory for
// the backing store files. Returns true if the directory exists
// and is considered a secure location. Returns false if the path
// is a symbolic link or if an error occurred.
//
static bool is_directory_secure(const char* path) {

  DWORD fa;

  fa = GetFileAttributes(path);
  if (fa == 0xFFFFFFFF) {
    DWORD lasterror = GetLastError();
    if (lasterror == ERROR_FILE_NOT_FOUND) {
      return false;
    }
    else {
      // unexpected error, declare the path insecure
      if (PrintMiscellaneous && Verbose) {
        warning("could not get attributes for file %s: ",
                " lasterror = %d\n", path, lasterror);
      }
      return false;
    }
  }

  if (fa & FILE_ATTRIBUTE_REPARSE_POINT) {
    // we don't accept any redirection for the user specific directory
    // so declare the path insecure. This may be too conservative,
    // as some types of reparse points might be acceptable, but it
    // is probably more secure to avoid these conditions.
    //
    if (PrintMiscellaneous && Verbose) {
      warning("%s is a reparse point\n", path);
    }
    return false;
  }

  if (fa & FILE_ATTRIBUTE_DIRECTORY) {
    // this is the expected case. Since windows supports symbolic
    // links to directories only, not to files, there is no need
    // to check for open write permissions on the directory. If the
    // directory has open write permissions, any files deposited that
    // are not expected will be removed by the cleanup code.
    //
    return true;
  }
  else {
    // this is either a regular file or some other type of file,
    // any of which are unexpected and therefore insecure.
    //
    if (PrintMiscellaneous && Verbose) {
      warning("%s is not a directory, file attributes = "
              INTPTR_FORMAT "\n", path, fa);
    }
    return false;
  }
}

// return the user name for the owner of this process
//
// the caller is expected to free the allocated memory.
//
static char* get_user_name() {

  /* get the user name. This code is adapted from code found in
   * the jdk in src/windows/native/java/lang/java_props_md.c
   * java_props_md.c  1.29 02/02/06. According to the original
   * source, the call to GetUserName is avoided because of a resulting
   * increase in footprint of 100K.
   */
  char* user = getenv("USERNAME");
  char buf[UNLEN+1];
  DWORD buflen = sizeof(buf);
  if (user == NULL || strlen(user) == 0) {
    if (GetUserName(buf, &buflen)) {
      user = buf;
    }
    else {
      return NULL;
    }
  }

  char* user_name = NEW_C_HEAP_ARRAY(char, strlen(user)+1, mtInternal);
  strcpy(user_name, user);

  return user_name;
}

// return the name of the user that owns the process identified by vmid.
//
// This method uses a slow directory search algorithm to find the backing
// store file for the specified vmid and returns the user name, as determined
// by the user name suffix of the hsperfdata_<username> directory name.
//
// the caller is expected to free the allocated memory.
//
static char* get_user_name_slow(int vmid) {

  // directory search
  char* latest_user = NULL;
  time_t latest_ctime = 0;

  const char* tmpdirname = os::get_temp_directory();

  DIR* tmpdirp = os::opendir(tmpdirname);

  if (tmpdirp == NULL) {
    return NULL;
  }

  // for each entry in the directory that matches the pattern hsperfdata_*,
  // open the directory and check if the file for the given vmid exists.
  // The file with the expected name and the latest creation date is used
  // to determine the user name for the process id.
  //
  struct dirent* dentry;
  errno = 0;
  while ((dentry = os::readdir(tmpdirp)) != NULL) {

    // check if the directory entry is a hsperfdata file
    if (strncmp(dentry->d_name, PERFDATA_NAME, strlen(PERFDATA_NAME)) != 0) {
      continue;
    }

    char* usrdir_name = NEW_C_HEAP_ARRAY(char,
        strlen(tmpdirname) + strlen(dentry->d_name) + 2, mtInternal);
    strcpy(usrdir_name, tmpdirname);
    strcat(usrdir_name, "\\");
    strcat(usrdir_name, dentry->d_name);

    DIR* subdirp = os::opendir(usrdir_name);

    if (subdirp == NULL) {
      FREE_C_HEAP_ARRAY(char, usrdir_name);
      continue;
    }

    // Since we don't create the backing store files in directories
    // pointed to by symbolic links, we also don't follow them when
    // looking for the files. We check for a symbolic link after the
    // call to opendir in order to eliminate a small window where the
    // symlink can be exploited.
    //
    if (!is_directory_secure(usrdir_name)) {
      FREE_C_HEAP_ARRAY(char, usrdir_name);
      os::closedir(subdirp);
      continue;
    }

    struct dirent* udentry;
    errno = 0;
    while ((udentry = os::readdir(subdirp)) != NULL) {

      if (filename_to_pid(udentry->d_name) == vmid) {
        struct stat statbuf;

        char* filename = NEW_C_HEAP_ARRAY(char,
           strlen(usrdir_name) + strlen(udentry->d_name) + 2, mtInternal);

        strcpy(filename, usrdir_name);
        strcat(filename, "\\");
        strcat(filename, udentry->d_name);

        if (::stat(filename, &statbuf) == OS_ERR) {
           FREE_C_HEAP_ARRAY(char, filename);
           continue;
        }

        // skip over files that are not regular files.
        if ((statbuf.st_mode & S_IFMT) != S_IFREG) {
          FREE_C_HEAP_ARRAY(char, filename);
          continue;
        }

        // If we found a matching file with a newer creation time, then
        // save the user name. The newer creation time indicates that
        // we found a newer incarnation of the process associated with
        // vmid. Due to the way that Windows recycles pids and the fact
        // that we can't delete the file from the file system namespace
        // until last close, it is possible for there to be more than
        // one hsperfdata file with a name matching vmid (diff users).
        //
        // We no longer ignore hsperfdata files where (st_size == 0).
        // In this function, all we're trying to do is determine the
        // name of the user that owns the process associated with vmid
        // so the size doesn't matter. Very rarely, we have observed
        // hsperfdata files where (st_size == 0) and the st_size field
        // later becomes the expected value.
        //
        if (statbuf.st_ctime > latest_ctime) {
          char* user = strchr(dentry->d_name, '_') + 1;

          FREE_C_HEAP_ARRAY(char, latest_user);
          latest_user = NEW_C_HEAP_ARRAY(char, strlen(user)+1, mtInternal);

          strcpy(latest_user, user);
          latest_ctime = statbuf.st_ctime;
        }

        FREE_C_HEAP_ARRAY(char, filename);
      }
    }
    os::closedir(subdirp);
    FREE_C_HEAP_ARRAY(char, usrdir_name);
  }
  os::closedir(tmpdirp);

  return(latest_user);
}

// return the name of the user that owns the process identified by vmid.
//
// note: this method should only be used via the Perf native methods.
// There are various costs to this method and limiting its use to the
// Perf native methods limits the impact to monitoring applications only.
//
static char* get_user_name(int vmid) {

  // A fast implementation is not provided at this time. It's possible
  // to provide a fast process id to user name mapping function using
  // the win32 apis, but the default ACL for the process object only
  // allows processes with the same owner SID to acquire the process
  // handle (via OpenProcess(PROCESS_QUERY_INFORMATION)). It's possible
  // to have the JVM change the ACL for the process object to allow arbitrary
  // users to access the process handle and the process security token.
  // The security ramifications need to be studied before providing this
  // mechanism.
  //
  return get_user_name_slow(vmid);
}

// return the name of the shared memory file mapping object for the
// named shared memory region for the given user name and vmid.
//
// The file mapping object's name is not the file name. It is a name
// in a separate name space.
//
// the caller is expected to free the allocated memory.
//
static char *get_sharedmem_objectname(const char* user, int vmid) {

  // construct file mapping object's name, add 3 for two '_' and a
  // null terminator.
  int nbytes = (int)strlen(PERFDATA_NAME) + (int)strlen(user) + 3;

  // the id is converted to an unsigned value here because win32 allows
  // negative process ids. However, OpenFileMapping API complains
  // about a name containing a '-' characters.
  //
  nbytes += UINT_CHARS;
  char* name = NEW_C_HEAP_ARRAY(char, nbytes, mtInternal);
  _snprintf(name, nbytes, "%s_%s_%u", PERFDATA_NAME, user, vmid);

  return name;
}

// return the file name of the backing store file for the named
// shared memory region for the given user name and vmid.
//
// the caller is expected to free the allocated memory.
//
static char* get_sharedmem_filename(const char* dirname, int vmid) {

  // add 2 for the file separator and a null terminator.
  size_t nbytes = strlen(dirname) + UINT_CHARS + 2;

  char* name = NEW_C_HEAP_ARRAY(char, nbytes, mtInternal);
  _snprintf(name, nbytes, "%s\\%d", dirname, vmid);

  return name;
}

// remove file
//
// this method removes the file with the given file name.
//
// Note: if the indicated file is on an SMB network file system, this
// method may be unsuccessful in removing the file.
//
static void remove_file(const char* dirname, const char* filename) {

  size_t nbytes = strlen(dirname) + strlen(filename) + 2;
  char* path = NEW_C_HEAP_ARRAY(char, nbytes, mtInternal);

  strcpy(path, dirname);
  strcat(path, "\\");
  strcat(path, filename);

  if (::unlink(path) == OS_ERR) {
    if (PrintMiscellaneous && Verbose) {
      if (errno != ENOENT) {
        warning("Could not unlink shared memory backing"
                " store file %s : %s\n", path, os::strerror(errno));
      }
    }
  }

  FREE_C_HEAP_ARRAY(char, path);
}

// returns true if the process represented by pid is alive, otherwise
// returns false. the validity of the result is only accurate if the
// target process is owned by the same principal that owns this process.
// this method should not be used if to test the status of an otherwise
// arbitrary process unless it is know that this process has the appropriate
// privileges to guarantee a result valid.
//
static bool is_alive(int pid) {

  HANDLE ph = OpenProcess(PROCESS_QUERY_INFORMATION, FALSE, pid);
  if (ph == NULL) {
    // the process does not exist.
    if (PrintMiscellaneous && Verbose) {
      DWORD lastError = GetLastError();
      if (lastError != ERROR_INVALID_PARAMETER) {
        warning("OpenProcess failed: %d\n", GetLastError());
      }
    }
    return false;
  }

  DWORD exit_status;
  if (!GetExitCodeProcess(ph, &exit_status)) {
    if (PrintMiscellaneous && Verbose) {
      warning("GetExitCodeProcess failed: %d\n", GetLastError());
    }
    CloseHandle(ph);
    return false;
  }

  CloseHandle(ph);
  return (exit_status == STILL_ACTIVE) ? true : false;
}

// check if the file system is considered secure for the backing store files
//
static bool is_filesystem_secure(const char* path) {

  char root_path[MAX_PATH];
  char fs_type[MAX_PATH];

  if (PerfBypassFileSystemCheck) {
    if (PrintMiscellaneous && Verbose) {
      warning("bypassing file system criteria checks for %s\n", path);
    }
    return true;
  }

  char* first_colon = strchr((char *)path, ':');
  if (first_colon == NULL) {
    if (PrintMiscellaneous && Verbose) {
      warning("expected device specifier in path: %s\n", path);
    }
    return false;
  }

  size_t len = (size_t)(first_colon - path);
  assert(len + 2 <= MAX_PATH, "unexpected device specifier length");
  strncpy(root_path, path, len + 1);
  root_path[len + 1] = '\\';
  root_path[len + 2] = '\0';

  // check that we have something like "C:\" or "AA:\"
  assert(strlen(root_path) >= 3, "device specifier too short");
  assert(strchr(root_path, ':') != NULL, "bad device specifier format");
  assert(strchr(root_path, '\\') != NULL, "bad device specifier format");

  DWORD maxpath;
  DWORD flags;

  if (!GetVolumeInformation(root_path, NULL, 0, NULL, &maxpath,
                            &flags, fs_type, MAX_PATH)) {
    // we can't get information about the volume, so assume unsafe.
    if (PrintMiscellaneous && Verbose) {
      warning("could not get device information for %s: "
              " path = %s: lasterror = %d\n",
              root_path, path, GetLastError());
    }
    return false;
  }

  if ((flags & FS_PERSISTENT_ACLS) == 0) {
    // file system doesn't support ACLs, declare file system unsafe
    if (PrintMiscellaneous && Verbose) {
      warning("file system type %s on device %s does not support"
              " ACLs\n", fs_type, root_path);
    }
    return false;
  }

  if ((flags & FS_VOL_IS_COMPRESSED) != 0) {
    // file system is compressed, declare file system unsafe
    if (PrintMiscellaneous && Verbose) {
      warning("file system type %s on device %s is compressed\n",
              fs_type, root_path);
    }
    return false;
  }

  return true;
}

// cleanup stale shared memory resources
//
// This method attempts to remove all stale shared memory files in
// the named user temporary directory. It scans the named directory
// for files matching the pattern ^$[0-9]*$. For each file found, the
// process id is extracted from the file name and a test is run to
// determine if the process is alive. If the process is not alive,
// any stale file resources are removed.
//
static void cleanup_sharedmem_resources(const char* dirname) {

  // open the user temp directory
  DIR* dirp = os::opendir(dirname);

  if (dirp == NULL) {
    // directory doesn't exist, so there is nothing to cleanup
    return;
  }

  if (!is_directory_secure(dirname)) {
    // the directory is not secure, don't attempt any cleanup
    os::closedir(dirp);
    return;
  }

  // for each entry in the directory that matches the expected file
  // name pattern, determine if the file resources are stale and if
  // so, remove the file resources. Note, instrumented HotSpot processes
  // for this user may start and/or terminate during this search and
  // remove or create new files in this directory. The behavior of this
  // loop under these conditions is dependent upon the implementation of
  // opendir/readdir.
  //
  struct dirent* entry;
  errno = 0;
  while ((entry = os::readdir(dirp)) != NULL) {

    int pid = filename_to_pid(entry->d_name);

    if (pid == 0) {

      if (strcmp(entry->d_name, ".") != 0 && strcmp(entry->d_name, "..") != 0) {

        // attempt to remove all unexpected files, except "." and ".."
        remove_file(dirname, entry->d_name);
      }

      errno = 0;
      continue;
    }

    // we now have a file name that converts to a valid integer
    // that could represent a process id . if this process id
    // matches the current process id or the process is not running,
    // then remove the stale file resources.
    //
    // process liveness is detected by checking the exit status
    // of the process. if the process id is valid and the exit status
    // indicates that it is still running, the file file resources
    // are not removed. If the process id is invalid, or if we don't
    // have permissions to check the process status, or if the process
    // id is valid and the process has terminated, the the file resources
    // are assumed to be stale and are removed.
    //
    if (pid == os::current_process_id() || !is_alive(pid)) {

      // we can only remove the file resources. Any mapped views
      // of the file can only be unmapped by the processes that
      // opened those views and the file mapping object will not
      // get removed until all views are unmapped.
      //
      remove_file(dirname, entry->d_name);
    }
    errno = 0;
  }
  os::closedir(dirp);
}

// create a file mapping object with the requested name, and size
// from the file represented by the given Handle object
//
static HANDLE create_file_mapping(const char* name, HANDLE fh, LPSECURITY_ATTRIBUTES fsa, size_t size) {

  DWORD lowSize = (DWORD)size;
  DWORD highSize = 0;
  HANDLE fmh = NULL;

  // Create a file mapping object with the given name. This function
  // will grow the file to the specified size.
  //
  fmh = CreateFileMapping(
               fh,                 /* HANDLE file handle for backing store */
               fsa,                /* LPSECURITY_ATTRIBUTES Not inheritable */
               PAGE_READWRITE,     /* DWORD protections */
               highSize,           /* DWORD High word of max size */
               lowSize,            /* DWORD Low word of max size */
               name);              /* LPCTSTR name for object */

  if (fmh == NULL) {
    if (PrintMiscellaneous && Verbose) {
      warning("CreateFileMapping failed, lasterror = %d\n", GetLastError());
    }
    return NULL;
  }

  if (GetLastError() == ERROR_ALREADY_EXISTS) {

    // a stale file mapping object was encountered. This object may be
    // owned by this or some other user and cannot be removed until
    // the other processes either exit or close their mapping objects
    // and/or mapped views of this mapping object.
    //
    if (PrintMiscellaneous && Verbose) {
      warning("file mapping already exists, lasterror = %d\n", GetLastError());
    }

    CloseHandle(fmh);
    return NULL;
  }

  return fmh;
}


// method to free the given security descriptor and the contained
// access control list.
//
static void free_security_desc(PSECURITY_DESCRIPTOR pSD) {

  BOOL success, exists, isdefault;
  PACL pACL;

  if (pSD != NULL) {

    // get the access control list from the security descriptor
    success = GetSecurityDescriptorDacl(pSD, &exists, &pACL, &isdefault);

    // if an ACL existed and it was not a default acl, then it must
    // be an ACL we enlisted. free the resources.
    //
    if (success && exists && pACL != NULL && !isdefault) {
      FREE_C_HEAP_ARRAY(char, pACL);
    }

    // free the security descriptor
    FREE_C_HEAP_ARRAY(char, pSD);
  }
}

// method to free up a security attributes structure and any
// contained security descriptors and ACL
//
static void free_security_attr(LPSECURITY_ATTRIBUTES lpSA) {

  if (lpSA != NULL) {
    // free the contained security descriptor and the ACL
    free_security_desc(lpSA->lpSecurityDescriptor);
    lpSA->lpSecurityDescriptor = NULL;

    // free the security attributes structure
    FREE_C_HEAP_OBJ(lpSA);
  }
}

// get the user SID for the process indicated by the process handle
//
static PSID get_user_sid(HANDLE hProcess) {

  HANDLE hAccessToken;
  PTOKEN_USER token_buf = NULL;
  DWORD rsize = 0;

  if (hProcess == NULL) {
    return NULL;
  }

  // get the process token
  if (!OpenProcessToken(hProcess, TOKEN_READ, &hAccessToken)) {
    if (PrintMiscellaneous && Verbose) {
      warning("OpenProcessToken failure: lasterror = %d \n", GetLastError());
    }
    return NULL;
  }

  // determine the size of the token structured needed to retrieve
  // the user token information from the access token.
  //
  if (!GetTokenInformation(hAccessToken, TokenUser, NULL, rsize, &rsize)) {
    DWORD lasterror = GetLastError();
    if (lasterror != ERROR_INSUFFICIENT_BUFFER) {
      if (PrintMiscellaneous && Verbose) {
        warning("GetTokenInformation failure: lasterror = %d,"
                " rsize = %d\n", lasterror, rsize);
      }
      CloseHandle(hAccessToken);
      return NULL;
    }
  }

  token_buf = (PTOKEN_USER) NEW_C_HEAP_ARRAY(char, rsize, mtInternal);

  // get the user token information
  if (!GetTokenInformation(hAccessToken, TokenUser, token_buf, rsize, &rsize)) {
    if (PrintMiscellaneous && Verbose) {
      warning("GetTokenInformation failure: lasterror = %d,"
              " rsize = %d\n", GetLastError(), rsize);
    }
    FREE_C_HEAP_ARRAY(char, token_buf);
    CloseHandle(hAccessToken);
    return NULL;
  }

  DWORD nbytes = GetLengthSid(token_buf->User.Sid);
  PSID pSID = NEW_C_HEAP_ARRAY(char, nbytes, mtInternal);

  if (!CopySid(nbytes, pSID, token_buf->User.Sid)) {
    if (PrintMiscellaneous && Verbose) {
      warning("GetTokenInformation failure: lasterror = %d,"
              " rsize = %d\n", GetLastError(), rsize);
    }
    FREE_C_HEAP_ARRAY(char, token_buf);
    FREE_C_HEAP_ARRAY(char, pSID);
    CloseHandle(hAccessToken);
    return NULL;
  }

  // close the access token.
  CloseHandle(hAccessToken);
  FREE_C_HEAP_ARRAY(char, token_buf);

  return pSID;
}

// structure used to consolidate access control entry information
//
typedef struct ace_data {
  PSID pSid;      // SID of the ACE
  DWORD mask;     // mask for the ACE
} ace_data_t;


// method to add an allow access control entry with the access rights
// indicated in mask for the principal indicated in SID to the given
// security descriptor. Much of the DACL handling was adapted from
// the example provided here:
//      http://support.microsoft.com/kb/102102/EN-US/
//

static bool add_allow_aces(PSECURITY_DESCRIPTOR pSD,
                           ace_data_t aces[], int ace_count) {
  PACL newACL = NULL;
  PACL oldACL = NULL;

  if (pSD == NULL) {
    return false;
  }

  BOOL exists, isdefault;

  // retrieve any existing access control list.
  if (!GetSecurityDescriptorDacl(pSD, &exists, &oldACL, &isdefault)) {
    if (PrintMiscellaneous && Verbose) {
      warning("GetSecurityDescriptor failure: lasterror = %d \n",
              GetLastError());
    }
    return false;
  }

  // get the size of the DACL
  ACL_SIZE_INFORMATION aclinfo;

  // GetSecurityDescriptorDacl may return true value for exists (lpbDaclPresent)
  // while oldACL is NULL for some case.
  if (oldACL == NULL) {
    exists = FALSE;
  }

  if (exists) {
    if (!GetAclInformation(oldACL, &aclinfo,
                           sizeof(ACL_SIZE_INFORMATION),
                           AclSizeInformation)) {
      if (PrintMiscellaneous && Verbose) {
        warning("GetAclInformation failure: lasterror = %d \n", GetLastError());
        return false;
      }
    }
  } else {
    aclinfo.AceCount = 0; // assume NULL DACL
    aclinfo.AclBytesFree = 0;
    aclinfo.AclBytesInUse = sizeof(ACL);
  }

  // compute the size needed for the new ACL
  // initial size of ACL is sum of the following:
  //   * size of ACL structure.
  //   * size of each ACE structure that ACL is to contain minus the sid
  //     sidStart member (DWORD) of the ACE.
  //   * length of the SID that each ACE is to contain.
  DWORD newACLsize = aclinfo.AclBytesInUse +
                        (sizeof(ACCESS_ALLOWED_ACE) - sizeof(DWORD)) * ace_count;
  for (int i = 0; i < ace_count; i++) {
     assert(aces[i].pSid != 0, "pSid should not be 0");
     newACLsize += GetLengthSid(aces[i].pSid);
  }

  // create the new ACL
  newACL = (PACL) NEW_C_HEAP_ARRAY(char, newACLsize, mtInternal);

  if (!InitializeAcl(newACL, newACLsize, ACL_REVISION)) {
    if (PrintMiscellaneous && Verbose) {
      warning("InitializeAcl failure: lasterror = %d \n", GetLastError());
    }
    FREE_C_HEAP_ARRAY(char, newACL);
    return false;
  }

  unsigned int ace_index = 0;
  // copy any existing ACEs from the old ACL (if any) to the new ACL.
  if (aclinfo.AceCount != 0) {
    while (ace_index < aclinfo.AceCount) {
      LPVOID ace;
      if (!GetAce(oldACL, ace_index, &ace)) {
        if (PrintMiscellaneous && Verbose) {
          warning("InitializeAcl failure: lasterror = %d \n", GetLastError());
        }
        FREE_C_HEAP_ARRAY(char, newACL);
        return false;
      }
      if (((ACCESS_ALLOWED_ACE *)ace)->Header.AceFlags && INHERITED_ACE) {
        // this is an inherited, allowed ACE; break from loop so we can
        // add the new access allowed, non-inherited ACE in the correct
        // position, immediately following all non-inherited ACEs.
        break;
      }

      // determine if the SID of this ACE matches any of the SIDs
      // for which we plan to set ACEs.
      int matches = 0;
      for (int i = 0; i < ace_count; i++) {
        if (EqualSid(aces[i].pSid, &(((ACCESS_ALLOWED_ACE *)ace)->SidStart))) {
          matches++;
          break;
        }
      }

      // if there are no SID matches, then add this existing ACE to the new ACL
      if (matches == 0) {
        if (!AddAce(newACL, ACL_REVISION, MAXDWORD, ace,
                    ((PACE_HEADER)ace)->AceSize)) {
          if (PrintMiscellaneous && Verbose) {
            warning("AddAce failure: lasterror = %d \n", GetLastError());
          }
          FREE_C_HEAP_ARRAY(char, newACL);
          return false;
        }
      }
      ace_index++;
    }
  }

  // add the passed-in access control entries to the new ACL
  for (int i = 0; i < ace_count; i++) {
    if (!AddAccessAllowedAce(newACL, ACL_REVISION,
                             aces[i].mask, aces[i].pSid)) {
      if (PrintMiscellaneous && Verbose) {
        warning("AddAccessAllowedAce failure: lasterror = %d \n",
                GetLastError());
      }
      FREE_C_HEAP_ARRAY(char, newACL);
      return false;
    }
  }

  // now copy the rest of the inherited ACEs from the old ACL
  if (aclinfo.AceCount != 0) {
    // picking up at ace_index, where we left off in the
    // previous ace_index loop
    while (ace_index < aclinfo.AceCount) {
      LPVOID ace;
      if (!GetAce(oldACL, ace_index, &ace)) {
        if (PrintMiscellaneous && Verbose) {
          warning("InitializeAcl failure: lasterror = %d \n", GetLastError());
        }
        FREE_C_HEAP_ARRAY(char, newACL);
        return false;
      }
      if (!AddAce(newACL, ACL_REVISION, MAXDWORD, ace,
                  ((PACE_HEADER)ace)->AceSize)) {
        if (PrintMiscellaneous && Verbose) {
          warning("AddAce failure: lasterror = %d \n", GetLastError());
        }
        FREE_C_HEAP_ARRAY(char, newACL);
        return false;
      }
      ace_index++;
    }
  }

  // add the new ACL to the security descriptor.
  if (!SetSecurityDescriptorDacl(pSD, TRUE, newACL, FALSE)) {
    if (PrintMiscellaneous && Verbose) {
      warning("SetSecurityDescriptorDacl failure:"
              " lasterror = %d \n", GetLastError());
    }
    FREE_C_HEAP_ARRAY(char, newACL);
    return false;
  }

  // if running on windows 2000 or later, set the automatic inheritance
  // control flags.
  SetSecurityDescriptorControlFnPtr _SetSecurityDescriptorControl;
  _SetSecurityDescriptorControl = (SetSecurityDescriptorControlFnPtr)
       GetProcAddress(GetModuleHandle(TEXT("advapi32.dll")),
                      "SetSecurityDescriptorControl");

  if (_SetSecurityDescriptorControl != NULL) {
    // We do not want to further propagate inherited DACLs, so making them
    // protected prevents that.
    if (!_SetSecurityDescriptorControl(pSD, SE_DACL_PROTECTED,
                                            SE_DACL_PROTECTED)) {
      if (PrintMiscellaneous && Verbose) {
        warning("SetSecurityDescriptorControl failure:"
                " lasterror = %d \n", GetLastError());
      }
      FREE_C_HEAP_ARRAY(char, newACL);
      return false;
    }
  }
   // Note, the security descriptor maintains a reference to the newACL, not
   // a copy of it. Therefore, the newACL is not freed here. It is freed when
   // the security descriptor containing its reference is freed.
   //
   return true;
}

// method to create a security attributes structure, which contains a
// security descriptor and an access control list comprised of 0 or more
// access control entries. The method take an array of ace_data structures
// that indicate the ACE to be added to the security descriptor.
//
// the caller must free the resources associated with the security
// attributes structure created by this method by calling the
// free_security_attr() method.
//
static LPSECURITY_ATTRIBUTES make_security_attr(ace_data_t aces[], int count) {

  // allocate space for a security descriptor
  PSECURITY_DESCRIPTOR pSD = (PSECURITY_DESCRIPTOR)
     NEW_C_HEAP_ARRAY(char, SECURITY_DESCRIPTOR_MIN_LENGTH, mtInternal);

  // initialize the security descriptor
  if (!InitializeSecurityDescriptor(pSD, SECURITY_DESCRIPTOR_REVISION)) {
    if (PrintMiscellaneous && Verbose) {
      warning("InitializeSecurityDescriptor failure: "
              "lasterror = %d \n", GetLastError());
    }
    free_security_desc(pSD);
    return NULL;
  }

  // add the access control entries
  if (!add_allow_aces(pSD, aces, count)) {
    free_security_desc(pSD);
    return NULL;
  }

  // allocate and initialize the security attributes structure and
  // return it to the caller.
  //
  LPSECURITY_ATTRIBUTES lpSA =
      NEW_C_HEAP_OBJ(SECURITY_ATTRIBUTES, mtInternal);
  lpSA->nLength = sizeof(SECURITY_ATTRIBUTES);
  lpSA->lpSecurityDescriptor = pSD;
  lpSA->bInheritHandle = FALSE;

  return(lpSA);
}

// method to create a security attributes structure with a restrictive
// access control list that creates a set access rights for the user/owner
// of the securable object and a separate set access rights for everyone else.
// also provides for full access rights for the administrator group.
//
// the caller must free the resources associated with the security
// attributes structure created by this method by calling the
// free_security_attr() method.
//

static LPSECURITY_ATTRIBUTES make_user_everybody_admin_security_attr(
                                DWORD umask, DWORD emask, DWORD amask) {

  ace_data_t aces[3];

  // initialize the user ace data
  aces[0].pSid = get_user_sid(GetCurrentProcess());
  aces[0].mask = umask;

  if (aces[0].pSid == 0)
    return NULL;

  // get the well known SID for BUILTIN\Administrators
  PSID administratorsSid = NULL;
  SID_IDENTIFIER_AUTHORITY SIDAuthAdministrators = SECURITY_NT_AUTHORITY;

  if (!AllocateAndInitializeSid( &SIDAuthAdministrators, 2,
           SECURITY_BUILTIN_DOMAIN_RID,
           DOMAIN_ALIAS_RID_ADMINS,
           0, 0, 0, 0, 0, 0, &administratorsSid)) {

    if (PrintMiscellaneous && Verbose) {
      warning("AllocateAndInitializeSid failure: "
              "lasterror = %d \n", GetLastError());
    }
    return NULL;
  }

  // initialize the ace data for administrator group
  aces[1].pSid = administratorsSid;
  aces[1].mask = amask;

  // get the well known SID for the universal Everybody
  PSID everybodySid = NULL;
  SID_IDENTIFIER_AUTHORITY SIDAuthEverybody = SECURITY_WORLD_SID_AUTHORITY;

  if (!AllocateAndInitializeSid( &SIDAuthEverybody, 1, SECURITY_WORLD_RID,
           0, 0, 0, 0, 0, 0, 0, &everybodySid)) {

    if (PrintMiscellaneous && Verbose) {
      warning("AllocateAndInitializeSid failure: "
              "lasterror = %d \n", GetLastError());
    }
    return NULL;
  }

  // initialize the ace data for everybody else.
  aces[2].pSid = everybodySid;
  aces[2].mask = emask;

  // create a security attributes structure with access control
  // entries as initialized above.
  LPSECURITY_ATTRIBUTES lpSA = make_security_attr(aces, 3);
  FREE_C_HEAP_ARRAY(char, aces[0].pSid);
  FreeSid(everybodySid);
  FreeSid(administratorsSid);
  return(lpSA);
}


// method to create the security attributes structure for restricting
// access to the user temporary directory.
//
// the caller must free the resources associated with the security
// attributes structure created by this method by calling the
// free_security_attr() method.
//
static LPSECURITY_ATTRIBUTES make_tmpdir_security_attr() {

  // create full access rights for the user/owner of the directory
  // and read-only access rights for everybody else. This is
  // effectively equivalent to UNIX 755 permissions on a directory.
  //
  DWORD umask = STANDARD_RIGHTS_REQUIRED | FILE_ALL_ACCESS;
  DWORD emask = GENERIC_READ | FILE_LIST_DIRECTORY | FILE_TRAVERSE;
  DWORD amask = STANDARD_RIGHTS_ALL | FILE_ALL_ACCESS;

  return make_user_everybody_admin_security_attr(umask, emask, amask);
}

// method to create the security attributes structure for restricting
// access to the shared memory backing store file.
//
// the caller must free the resources associated with the security
// attributes structure created by this method by calling the
// free_security_attr() method.
//
static LPSECURITY_ATTRIBUTES make_file_security_attr() {

  // create extensive access rights for the user/owner of the file
  // and attribute read-only access rights for everybody else. This
  // is effectively equivalent to UNIX 600 permissions on a file.
  //
  DWORD umask = STANDARD_RIGHTS_ALL | FILE_ALL_ACCESS;
  DWORD emask = STANDARD_RIGHTS_READ | FILE_READ_ATTRIBUTES |
                 FILE_READ_EA | FILE_LIST_DIRECTORY | FILE_TRAVERSE;
  DWORD amask = STANDARD_RIGHTS_ALL | FILE_ALL_ACCESS;

  return make_user_everybody_admin_security_attr(umask, emask, amask);
}

// method to create the security attributes structure for restricting
// access to the name shared memory file mapping object.
//
// the caller must free the resources associated with the security
// attributes structure created by this method by calling the
// free_security_attr() method.
//
static LPSECURITY_ATTRIBUTES make_smo_security_attr() {

  // create extensive access rights for the user/owner of the shared
  // memory object and attribute read-only access rights for everybody
  // else. This is effectively equivalent to UNIX 600 permissions on
  // on the shared memory object.
  //
  DWORD umask = STANDARD_RIGHTS_REQUIRED | FILE_MAP_ALL_ACCESS;
  DWORD emask = STANDARD_RIGHTS_READ; // attributes only
  DWORD amask = STANDARD_RIGHTS_ALL | FILE_MAP_ALL_ACCESS;

  return make_user_everybody_admin_security_attr(umask, emask, amask);
}

// make the user specific temporary directory
//
static bool make_user_tmp_dir(const char* dirname) {


  LPSECURITY_ATTRIBUTES pDirSA = make_tmpdir_security_attr();
  if (pDirSA == NULL) {
    return false;
  }


  // create the directory with the given security attributes
  if (!CreateDirectory(dirname, pDirSA)) {
    DWORD lasterror = GetLastError();
    if (lasterror == ERROR_ALREADY_EXISTS) {
      // The directory already exists and was probably created by another
      // JVM instance. However, this could also be the result of a
      // deliberate symlink. Verify that the existing directory is safe.
      //
      if (!is_directory_secure(dirname)) {
        // directory is not secure
        if (PrintMiscellaneous && Verbose) {
          warning("%s directory is insecure\n", dirname);
        }
        return false;
      }
      // The administrator should be able to delete this directory.
      // But the directory created by previous version of JVM may not
      // have permission for administrators to delete this directory.
      // So add full permission to the administrator. Also setting new
      // DACLs might fix the corrupted the DACLs.
      SECURITY_INFORMATION secInfo = DACL_SECURITY_INFORMATION;
      if (!SetFileSecurity(dirname, secInfo, pDirSA->lpSecurityDescriptor)) {
        if (PrintMiscellaneous && Verbose) {
          lasterror = GetLastError();
          warning("SetFileSecurity failed for %s directory.  lasterror %d \n",
                                                        dirname, lasterror);
        }
      }
    }
    else {
      if (PrintMiscellaneous && Verbose) {
        warning("CreateDirectory failed: %d\n", GetLastError());
      }
      return false;
    }
  }

  // free the security attributes structure
  free_security_attr(pDirSA);

  return true;
}

// create the shared memory resources
//
// This function creates the shared memory resources. This includes
// the backing store file and the file mapping shared memory object.
//
static HANDLE create_sharedmem_resources(const char* dirname, const char* filename, const char* objectname, size_t size) {

  HANDLE fh = INVALID_HANDLE_VALUE;
  HANDLE fmh = NULL;


  // create the security attributes for the backing store file
  LPSECURITY_ATTRIBUTES lpFileSA = make_file_security_attr();
  if (lpFileSA == NULL) {
    return NULL;
  }

  // create the security attributes for the shared memory object
  LPSECURITY_ATTRIBUTES lpSmoSA = make_smo_security_attr();
  if (lpSmoSA == NULL) {
    free_security_attr(lpFileSA);
    return NULL;
  }

  // create the user temporary directory
  if (!make_user_tmp_dir(dirname)) {
    // could not make/find the directory or the found directory
    // was not secure
    return NULL;
  }

  // Create the file - the FILE_FLAG_DELETE_ON_CLOSE flag allows the
  // file to be deleted by the last process that closes its handle to
  // the file. This is important as the apis do not allow a terminating
  // JVM being monitored by another process to remove the file name.
  //
  fh = CreateFile(
             filename,                          /* LPCTSTR file name */

             GENERIC_READ|GENERIC_WRITE,        /* DWORD desired access */
             FILE_SHARE_DELETE|FILE_SHARE_READ, /* DWORD share mode, future READONLY
                                                 * open operations allowed
                                                 */
             lpFileSA,                          /* LPSECURITY security attributes */
             CREATE_ALWAYS,                     /* DWORD creation disposition
                                                 * create file, if it already
                                                 * exists, overwrite it.
                                                 */
             FILE_FLAG_DELETE_ON_CLOSE,         /* DWORD flags and attributes */

             NULL);                             /* HANDLE template file access */

  free_security_attr(lpFileSA);

  if (fh == INVALID_HANDLE_VALUE) {
    DWORD lasterror = GetLastError();
    if (PrintMiscellaneous && Verbose) {
      warning("could not create file %s: %d\n", filename, lasterror);
    }
    return NULL;
  }

  // try to create the file mapping
  fmh = create_file_mapping(objectname, fh, lpSmoSA, size);

  free_security_attr(lpSmoSA);

  if (fmh == NULL) {
    // closing the file handle here will decrement the reference count
    // on the file. When all processes accessing the file close their
    // handle to it, the reference count will decrement to 0 and the
    // OS will delete the file. These semantics are requested by the
    // FILE_FLAG_DELETE_ON_CLOSE flag in CreateFile call above.
    CloseHandle(fh);
    fh = NULL;
    return NULL;
  } else {
    // We created the file mapping, but rarely the size of the
    // backing store file is reported as zero (0) which can cause
    // failures when trying to use the hsperfdata file.
    struct stat statbuf;
    int ret_code = ::stat(filename, &statbuf);
    if (ret_code == OS_ERR) {
      if (PrintMiscellaneous && Verbose) {
        warning("Could not get status information from file %s: %s\n",
            filename, os::strerror(errno));
      }
      CloseHandle(fmh);
      CloseHandle(fh);
      fh = NULL;
      fmh = NULL;
      return NULL;
    }

    // We could always call FlushFileBuffers() but the Microsoft
    // docs indicate that it is considered expensive so we only
    // call it when we observe the size as zero (0).
    if (statbuf.st_size == 0 && FlushFileBuffers(fh) != TRUE) {
      DWORD lasterror = GetLastError();
      if (PrintMiscellaneous && Verbose) {
        warning("could not flush file %s: %d\n", filename, lasterror);
      }
      CloseHandle(fmh);
      CloseHandle(fh);
      fh = NULL;
      fmh = NULL;
      return NULL;
    }
  }

  // the file has been successfully created and the file mapping
  // object has been created.
  sharedmem_fileHandle = fh;
  sharedmem_fileName = os::strdup(filename);

  return fmh;
}

// open the shared memory object for the given vmid.
//
static HANDLE open_sharedmem_object(const char* objectname, DWORD ofm_access, TRAPS) {

  HANDLE fmh;

  // open the file mapping with the requested mode
  fmh = OpenFileMapping(
               ofm_access,       /* DWORD access mode */
               FALSE,            /* BOOL inherit flag - Do not allow inherit */
               objectname);      /* name for object */

  if (fmh == NULL) {
    DWORD lasterror = GetLastError();
    if (PrintMiscellaneous && Verbose) {
      warning("OpenFileMapping failed for shared memory object %s:"
              " lasterror = %d\n", objectname, lasterror);
    }
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               err_msg("Could not open PerfMemory, error %d", lasterror),
               INVALID_HANDLE_VALUE);
  }

  return fmh;;
}

// create a named shared memory region
//
// On Win32, a named shared memory object has a name space that
// is independent of the file system name space. Shared memory object,
// or more precisely, file mapping objects, provide no mechanism to
// inquire the size of the memory region. There is also no api to
// enumerate the memory regions for various processes.
//
// This implementation utilizes the shared memory name space in parallel
// with the file system name space. This allows us to determine the
// size of the shared memory region from the size of the file and it
// allows us to provide a common, file system based name space for
// shared memory across platforms.
//
static char* mapping_create_shared(size_t size) {

  void *mapAddress;
  int vmid = os::current_process_id();

  // get the name of the user associated with this process
  char* user = get_user_name();

  if (user == NULL) {
    return NULL;
  }

  // construct the name of the user specific temporary directory
  char* dirname = get_user_tmp_dir(user);

  // check that the file system is secure - i.e. it supports ACLs.
  if (!is_filesystem_secure(dirname)) {
    FREE_C_HEAP_ARRAY(char, dirname);
    FREE_C_HEAP_ARRAY(char, user);
    return NULL;
  }

  // create the names of the backing store files and for the
  // share memory object.
  //
  char* filename = get_sharedmem_filename(dirname, vmid);
  char* objectname = get_sharedmem_objectname(user, vmid);

  // cleanup any stale shared memory resources
  cleanup_sharedmem_resources(dirname);

  assert(((size != 0) && (size % os::vm_page_size() == 0)),
         "unexpected PerfMemry region size");

  FREE_C_HEAP_ARRAY(char, user);

  // create the shared memory resources
  sharedmem_fileMapHandle =
               create_sharedmem_resources(dirname, filename, objectname, size);

  FREE_C_HEAP_ARRAY(char, filename);
  FREE_C_HEAP_ARRAY(char, objectname);
  FREE_C_HEAP_ARRAY(char, dirname);

  if (sharedmem_fileMapHandle == NULL) {
    return NULL;
  }

  // map the file into the address space
  mapAddress = MapViewOfFile(
                   sharedmem_fileMapHandle, /* HANDLE = file mapping object */
                   FILE_MAP_ALL_ACCESS,     /* DWORD access flags */
                   0,                       /* DWORD High word of offset */
                   0,                       /* DWORD Low word of offset */
                   (DWORD)size);            /* DWORD Number of bytes to map */

  if (mapAddress == NULL) {
    if (PrintMiscellaneous && Verbose) {
      warning("MapViewOfFile failed, lasterror = %d\n", GetLastError());
    }
    CloseHandle(sharedmem_fileMapHandle);
    sharedmem_fileMapHandle = NULL;
    return NULL;
  }

  // clear the shared memory region
  (void)memset(mapAddress, '\0', size);

  // it does not go through os api, the operation has to record from here
  MemTracker::record_virtual_memory_reserve_and_commit((address)mapAddress,
    size, CURRENT_PC, mtInternal);

  return (char*) mapAddress;
}

// this method deletes the file mapping object.
//
static void delete_file_mapping(char* addr, size_t size) {

  // cleanup the persistent shared memory resources. since DestroyJavaVM does
  // not support unloading of the JVM, unmapping of the memory resource is not
  // performed. The memory will be reclaimed by the OS upon termination of all
  // processes mapping the resource. The file mapping handle and the file
  // handle are closed here to expedite the remove of the file by the OS. The
  // file is not removed directly because it was created with
  // FILE_FLAG_DELETE_ON_CLOSE semantics and any attempt to remove it would
  // be unsuccessful.

  // close the fileMapHandle. the file mapping will still be retained
  // by the OS as long as any other JVM processes has an open file mapping
  // handle or a mapped view of the file.
  //
  if (sharedmem_fileMapHandle != NULL) {
    CloseHandle(sharedmem_fileMapHandle);
    sharedmem_fileMapHandle = NULL;
  }

  // close the file handle. This will decrement the reference count on the
  // backing store file. When the reference count decrements to 0, the OS
  // will delete the file. These semantics apply because the file was
  // created with the FILE_FLAG_DELETE_ON_CLOSE flag.
  //
  if (sharedmem_fileHandle != INVALID_HANDLE_VALUE) {
    CloseHandle(sharedmem_fileHandle);
    sharedmem_fileHandle = INVALID_HANDLE_VALUE;
  }
}

// this method determines the size of the shared memory file
//
static size_t sharedmem_filesize(const char* filename, TRAPS) {

  struct stat statbuf;

  // get the file size
  //
  // on win95/98/me, _stat returns a file size of 0 bytes, but on
  // winnt/2k the appropriate file size is returned. support for
  // the sharable aspects of performance counters was abandonded
  // on the non-nt win32 platforms due to this and other api
  // inconsistencies
  //
  if (::stat(filename, &statbuf) == OS_ERR) {
    if (PrintMiscellaneous && Verbose) {
      warning("stat %s failed: %s\n", filename, os::strerror(errno));
    }
    THROW_MSG_0(vmSymbols::java_io_IOException(),
                "Could not determine PerfMemory size");
  }

  if ((statbuf.st_size == 0) || (statbuf.st_size % os::vm_page_size() != 0)) {
    if (PrintMiscellaneous && Verbose) {
      warning("unexpected file size: size = " SIZE_FORMAT "\n",
              statbuf.st_size);
    }
    THROW_MSG_0(vmSymbols::java_io_IOException(),
                "Invalid PerfMemory size");
  }

  return statbuf.st_size;
}

// this method opens a file mapping object and maps the object
// into the address space of the process
//
static void open_file_mapping(const char* user, int vmid,
                              PerfMemory::PerfMemoryMode mode,
                              char** addrp, size_t* sizep, TRAPS) {

  ResourceMark rm;

  void *mapAddress = 0;
  size_t size = 0;
  HANDLE fmh;
  DWORD ofm_access;
  DWORD mv_access;
  const char* luser = NULL;

  if (mode == PerfMemory::PERF_MODE_RO) {
    ofm_access = FILE_MAP_READ;
    mv_access = FILE_MAP_READ;
  }
  else if (mode == PerfMemory::PERF_MODE_RW) {
#ifdef LATER
    ofm_access = FILE_MAP_READ | FILE_MAP_WRITE;
    mv_access = FILE_MAP_READ | FILE_MAP_WRITE;
#else
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Unsupported access mode");
#endif
  }
  else {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Illegal access mode");
  }

  // if a user name wasn't specified, then find the user name for
  // the owner of the target vm.
  if (user == NULL || strlen(user) == 0) {
    luser = get_user_name(vmid);
  }
  else {
    luser = user;
  }

  if (luser == NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Could not map vmid to user name");
  }

  // get the names for the resources for the target vm
  char* dirname = get_user_tmp_dir(luser);

  // since we don't follow symbolic links when creating the backing
  // store file, we also don't following them when attaching
  //
  if (!is_directory_secure(dirname)) {
    FREE_C_HEAP_ARRAY(char, dirname);
    if (luser != user) FREE_C_HEAP_ARRAY(char, luser);
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Process not found");
  }

  char* filename = get_sharedmem_filename(dirname, vmid);
  char* objectname = get_sharedmem_objectname(luser, vmid);

  // copy heap memory to resource memory. the objectname and
  // filename are passed to methods that may throw exceptions.
  // using resource arrays for these names prevents the leaks
  // that would otherwise occur.
  //
  char* rfilename = NEW_RESOURCE_ARRAY(char, strlen(filename) + 1);
  char* robjectname = NEW_RESOURCE_ARRAY(char, strlen(objectname) + 1);
  strcpy(rfilename, filename);
  strcpy(robjectname, objectname);

  // free the c heap resources that are no longer needed
  if (luser != user) FREE_C_HEAP_ARRAY(char, luser);
  FREE_C_HEAP_ARRAY(char, dirname);
  FREE_C_HEAP_ARRAY(char, filename);
  FREE_C_HEAP_ARRAY(char, objectname);

  if (*sizep == 0) {
    size = sharedmem_filesize(rfilename, CHECK);
  } else {
    size = *sizep;
  }

  assert(size > 0, "unexpected size <= 0");

  // Open the file mapping object with the given name
  fmh = open_sharedmem_object(robjectname, ofm_access, CHECK);

  assert(fmh != INVALID_HANDLE_VALUE, "unexpected handle value");

  // map the entire file into the address space
  mapAddress = MapViewOfFile(
                 fmh,             /* HANDLE Handle of file mapping object */
                 mv_access,       /* DWORD access flags */
                 0,               /* DWORD High word of offset */
                 0,               /* DWORD Low word of offset */
                 size);           /* DWORD Number of bytes to map */

  if (mapAddress == NULL) {
    if (PrintMiscellaneous && Verbose) {
      warning("MapViewOfFile failed, lasterror = %d\n", GetLastError());
    }
    CloseHandle(fmh);
    THROW_MSG(vmSymbols::java_lang_OutOfMemoryError(),
              "Could not map PerfMemory");
  }

  // it does not go through os api, the operation has to record from here
  MemTracker::record_virtual_memory_reserve_and_commit((address)mapAddress, size,
    CURRENT_PC, mtInternal);


  *addrp = (char*)mapAddress;
  *sizep = size;

  // File mapping object can be closed at this time without
  // invalidating the mapped view of the file
  CloseHandle(fmh);

  log_debug(perf, memops)("mapped " SIZE_FORMAT " bytes for vmid %d at "
                          INTPTR_FORMAT, size, vmid, mapAddress);
}

// this method unmaps the the mapped view of the the
// file mapping object.
//
static void remove_file_mapping(char* addr) {

  // the file mapping object was closed in open_file_mapping()
  // after the file map view was created. We only need to
  // unmap the file view here.
  UnmapViewOfFile(addr);
}

// create the PerfData memory region in shared memory.
static char* create_shared_memory(size_t size) {

  return mapping_create_shared(size);
}

// release a named, shared memory region
//
void delete_shared_memory(char* addr, size_t size) {

  delete_file_mapping(addr, size);
}




// create the PerfData memory region
//
// This method creates the memory region used to store performance
// data for the JVM. The memory may be created in standard or
// shared memory.
//
void PerfMemory::create_memory_region(size_t size) {

  if (PerfDisableSharedMem) {
    // do not share the memory for the performance data.
    PerfDisableSharedMem = true;
    _start = create_standard_memory(size);
  }
  else {
    _start = create_shared_memory(size);
    if (_start == NULL) {

      // creation of the shared memory region failed, attempt
      // to create a contiguous, non-shared memory region instead.
      //
      if (PrintMiscellaneous && Verbose) {
        warning("Reverting to non-shared PerfMemory region.\n");
      }
      PerfDisableSharedMem = true;
      _start = create_standard_memory(size);
    }
  }

  if (_start != NULL) _capacity = size;

}

// delete the PerfData memory region
//
// This method deletes the memory region used to store performance
// data for the JVM. The memory region indicated by the <address, size>
// tuple will be inaccessible after a call to this method.
//
void PerfMemory::delete_memory_region() {

  assert((start() != NULL && capacity() > 0), "verify proper state");

  // If user specifies PerfDataSaveFile, it will save the performance data
  // to the specified file name no matter whether PerfDataSaveToFile is specified
  // or not. In other word, -XX:PerfDataSaveFile=.. overrides flag
  // -XX:+PerfDataSaveToFile.
  if (PerfDataSaveToFile || PerfDataSaveFile != NULL) {
    save_memory_to_file(start(), capacity());
  }

  if (PerfDisableSharedMem) {
    delete_standard_memory(start(), capacity());
  }
  else {
    delete_shared_memory(start(), capacity());
  }
}

// attach to the PerfData memory region for another JVM
//
// This method returns an <address, size> tuple that points to
// a memory buffer that is kept reasonably synchronized with
// the PerfData memory region for the indicated JVM. This
// buffer may be kept in synchronization via shared memory
// or some other mechanism that keeps the buffer updated.
//
// If the JVM chooses not to support the attachability feature,
// this method should throw an UnsupportedOperation exception.
//
// This implementation utilizes named shared memory to map
// the indicated process's PerfData memory region into this JVMs
// address space.
//
void PerfMemory::attach(const char* user, int vmid, PerfMemoryMode mode,
                        char** addrp, size_t* sizep, TRAPS) {

  if (vmid == 0 || vmid == os::current_process_id()) {
     *addrp = start();
     *sizep = capacity();
     return;
  }

  open_file_mapping(user, vmid, mode, addrp, sizep, CHECK);
}

// detach from the PerfData memory region of another JVM
//
// This method detaches the PerfData memory region of another
// JVM, specified as an <address, size> tuple of a buffer
// in this process's address space. This method may perform
// arbitrary actions to accomplish the detachment. The memory
// region specified by <address, size> will be inaccessible after
// a call to this method.
//
// If the JVM chooses not to support the attachability feature,
// this method should throw an UnsupportedOperation exception.
//
// This implementation utilizes named shared memory to detach
// the indicated process's PerfData memory region from this
// process's address space.
//
void PerfMemory::detach(char* addr, size_t bytes) {

  assert(addr != 0, "address sanity check");
  assert(bytes > 0, "capacity sanity check");

  if (PerfMemory::contains(addr) || PerfMemory::contains(addr + bytes - 1)) {
    // prevent accidental detachment of this process's PerfMemory region
    return;
  }

  if (MemTracker::tracking_level() > NMT_minimal) {
    // it does not go through os api, the operation has to record from here
    Tracker tkr(Tracker::release);
    remove_file_mapping(addr);
    tkr.record((address)addr, bytes);
  } else {
    remove_file_mapping(addr);
  }
}
