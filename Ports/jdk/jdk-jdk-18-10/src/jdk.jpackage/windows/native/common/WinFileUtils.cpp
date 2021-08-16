/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

#include <memory>
#include <algorithm>
#include <shlwapi.h>
#include <stdlib.h>
#include <direct.h>

#include "FileUtils.h"
#include "WinFileUtils.h"
#include "WinErrorHandling.h"
#include "Log.h"


// Needed by FileUtils::isDirectoryNotEmpty
#pragma comment(lib, "shlwapi")


namespace FileUtils {

namespace {

tstring reservedFilenameChars() {
    tstring buf;
    for (char charCode = 0; charCode < 32; ++charCode) {
        buf.append(1, charCode);
    }
    buf += _T("<>:\"|?*/\\");
    return buf;
}

} // namespace

bool isFileExists(const tstring &filePath) {
    return GetFileAttributes(filePath.c_str()) != INVALID_FILE_ATTRIBUTES;
}

namespace {
bool isDirectoryAttrs(const DWORD attrs) {
    return attrs != INVALID_FILE_ATTRIBUTES
            && (attrs & FILE_ATTRIBUTE_DIRECTORY) != 0;
}
} // namespace

bool isDirectory(const tstring &filePath) {
    return isDirectoryAttrs(GetFileAttributes(filePath.c_str()));
}

bool isDirectoryNotEmpty(const tstring &dirPath) {
    if (!isDirectory(dirPath)) {
        return false;
    }
    return FALSE == PathIsDirectoryEmpty(dirPath.c_str());
}


tstring toAbsolutePath(const tstring& path) {
    if (path.empty()) {
        TCHAR* buf = _tgetcwd(0, 1);
        if (buf) {
            const tstring result(buf);
            free(buf);
            if (result.empty()) {
                JP_THROW(tstrings::any() << "_tgetcwd() returned empty string");
            }
            return result;
        }

        JP_THROW(tstrings::any() << "_tgetcwd() failed");
    }

    TCHAR* buf = _tfullpath(0, path.c_str(), size_t(1));
    if (buf) {
        const tstring result(buf);
        free(buf);
        return result;
    }

    JP_THROW(tstrings::any() << "_tfullpath(" << path << ") failed");
}


namespace {

bool createNewFile(const tstring& path) {
    HANDLE h = CreateFile(path.c_str(), GENERIC_WRITE, 0, NULL, CREATE_NEW,
            FILE_ATTRIBUTE_NORMAL, NULL);
    // if the file exists => h == INVALID_HANDLE_VALUE & GetLastError
    // returns ERROR_FILE_EXISTS
    if (h != INVALID_HANDLE_VALUE) {
        CloseHandle(h);
        LOG_TRACE(tstrings::any() << "Created [" << path << "] file");
        return true;
    }
    return false;
}

} // namespace

tstring createTempFile(const tstring &prefix, const tstring &suffix,
        const tstring &path) {
    const tstring invalidChars = reservedFilenameChars();

    if (prefix.find_first_of(invalidChars) != tstring::npos) {
        JP_THROW(tstrings::any() << "Illegal characters in prefix=" << prefix);
    }

    if (suffix.find_first_of(invalidChars) != tstring::npos) {
        JP_THROW(tstrings::any() << "Illegal characters in suffix=" << suffix);
    }

    int rnd = (int)GetTickCount();

    // do no more than 100 attempts
    for (int i=0; i<100; i++) {
        const tstring filePath = mkpath() << path << (prefix
                + (tstrings::any() << (rnd + i)).tstr() + suffix);
        if (createNewFile(filePath)) {
            return filePath;
        }
    }

    // 100 attempts failed
    JP_THROW(tstrings::any() << "createTempFile("  << prefix << ", "
                                                    << suffix << ", "
                                                    << path << ") failed");
}

tstring createTempDirectory(const tstring &prefix, const tstring &suffix,
        const tstring &basedir) {
    const tstring filePath = createTempFile(prefix, suffix, basedir);
    // delete the file and create directory with the same name
    deleteFile(filePath);
    createDirectory(filePath);
    return filePath;
}

tstring createUniqueFile(const tstring &prototype) {
    if (createNewFile(prototype)) {
        return prototype;
    }

    return createTempFile(replaceSuffix(basename(prototype)),
            suffix(prototype), dirname(prototype));
}

namespace {

void createDir(const tstring path, LPSECURITY_ATTRIBUTES saAttr,
        tstring_array* createdDirs=0) {
    if (CreateDirectory(path.c_str(), saAttr)) {
        LOG_TRACE(tstrings::any() << "Created [" << path << "] directory");
        if (createdDirs) {
            createdDirs->push_back(removeTrailingSlash(path));
        }
    } else {
        const DWORD createDirectoryErr = GetLastError();
        // if saAttr is specified, fail even if the directory exists
        if (saAttr != NULL || !isDirectory(path)) {
            JP_THROW(SysError(tstrings::any() << "CreateDirectory("
                << path << ") failed", CreateDirectory, createDirectoryErr));
        }
    }
}

}

void createDirectory(const tstring &path, tstring_array* createdDirs) {
    const tstring dirPath = removeTrailingSlash(path) + _T("\\");

    tstring::size_type pos = dirPath.find_first_of(_T("\\/"));
    while (pos != tstring::npos) {
        const tstring subdirPath = dirPath.substr(0, pos + 1);
        createDir(subdirPath, NULL, createdDirs);
        pos = dirPath.find_first_of(_T("\\/"), pos + 1);
    }
}


void copyFile(const tstring& fromPath, const tstring& toPath,
        bool failIfExists) {
    createDirectory(dirname(toPath));
    if (!CopyFile(fromPath.c_str(), toPath.c_str(),
            (failIfExists ? TRUE : FALSE))) {
        JP_THROW(SysError(tstrings::any()
                << "CopyFile(" << fromPath << ", " << toPath << ", "
                << failIfExists << ") failed", CopyFile));
    }
    LOG_TRACE(tstrings::any() << "Copied [" << fromPath << "] file to ["
            << toPath << "]");
}


namespace {

void moveFileImpl(const tstring& fromPath, const tstring& toPath,
        DWORD flags) {
    const bool isDir = isDirectory(fromPath);
    if (!MoveFileEx(fromPath.c_str(), toPath.empty() ? NULL : toPath.c_str(),
            flags)) {
        JP_THROW(SysError(tstrings::any() << "MoveFileEx(" << fromPath
                << ", " << toPath << ", " << flags << ") failed", MoveFileEx));
    }

    const bool onReboot = 0 != (flags & MOVEFILE_DELAY_UNTIL_REBOOT);

    const LPCTSTR label = isDir ? _T("folder") : _T("file");

    tstrings::any msg;
    if (!toPath.empty()) {
        if (onReboot) {
            msg << "Move";
        } else {
            msg << "Moved";
        }
        msg << " '" << fromPath << "' " << label << " to '" << toPath << "'";
    } else {
        if (onReboot) {
            msg << "Delete";
        } else {
            msg << "Deleted";
        }
        msg << " '" << fromPath << "' " << label;
    }
    if (onReboot) {
        msg << " on reboot";
    }
    LOG_TRACE(msg);
}

} // namespace


void moveFile(const tstring& fromPath, const tstring& toPath,
        bool failIfExists) {
    createDirectory(dirname(toPath));

    DWORD flags = MOVEFILE_COPY_ALLOWED;
    if (!failIfExists) {
        flags |= MOVEFILE_REPLACE_EXISTING;
    }

    moveFileImpl(fromPath, toPath, flags);
}

void deleteFile(const tstring &path)
{
    if (!deleteFile(path, std::nothrow)) {
        JP_THROW(SysError(tstrings::any()
                << "DeleteFile(" << path << ") failed", DeleteFile));
    }
}

namespace {

bool notFound(const DWORD status=GetLastError()) {
    return status == ERROR_FILE_NOT_FOUND || status == ERROR_PATH_NOT_FOUND;
}

bool deleteFileImpl(const std::nothrow_t &, const tstring &path) {
    const bool deleted = (DeleteFile(path.c_str()) != 0);
    if (deleted) {
        LOG_TRACE(tstrings::any() << "Deleted [" << path << "] file");
        return true;
    }
    return notFound();
}

} // namespace

bool deleteFile(const tstring &path, const std::nothrow_t &) throw()
{
    bool deleted = deleteFileImpl(std::nothrow, path);
    const DWORD status = GetLastError();
    if (!deleted && status == ERROR_ACCESS_DENIED) {
        DWORD attrs = GetFileAttributes(path.c_str());
        SetLastError(status);
        if (attrs == INVALID_FILE_ATTRIBUTES) {
            return false;
        }
        if (attrs & FILE_ATTRIBUTE_READONLY) {
            // DeleteFile() failed because file is R/O.
            // Remove R/O attribute and retry DeleteFile().
            attrs &= ~FILE_ATTRIBUTE_READONLY;
            if (SetFileAttributes(path.c_str(), attrs)) {
                LOG_TRACE(tstrings::any() << "Discarded R/O attribute from ["
                                                        << path << "] file");
                deleted = deleteFileImpl(std::nothrow, path);
            } else {
                LOG_WARNING(SysError(tstrings::any()
                            << "Failed to discard R/O attribute from ["
                            << path << "] file. File will not be deleted",
                            SetFileAttributes).what());
                SetLastError(status);
            }
        }
    }

    return deleted || notFound();
}

void deleteDirectory(const tstring &path)
{
    if (!deleteDirectory(path, std::nothrow)) {
        JP_THROW(SysError(tstrings::any()
                << "RemoveDirectory(" << path << ") failed", RemoveDirectory));
    }
}

bool deleteDirectory(const tstring &path, const std::nothrow_t &) throw()
{
    const bool deleted = (RemoveDirectory(path.c_str()) != 0);
    if (deleted) {
        LOG_TRACE(tstrings::any() << "Deleted [" << path << "] directory");
    }
    return deleted || notFound();
}

namespace {

class DeleteFilesCallback: public DirectoryCallback {
public:
    explicit DeleteFilesCallback(bool ff): failfast(ff), failed(false) {
    }

    virtual bool onFile(const tstring& path) {
        if (failfast) {
            deleteFile(path);
        } else {
            updateStatus(deleteFile(path, std::nothrow));
        }
        return true;
    }

    bool good() const {
        return !failed;
    }

protected:
    void updateStatus(bool success) {
        if (!success) {
            failed = true;
        }
    }

    const bool failfast;
private:
    bool failed;
};

class DeleteAllCallback: public DeleteFilesCallback {
public:
    explicit DeleteAllCallback(bool failfast): DeleteFilesCallback(failfast) {
    }

    virtual bool onDirectory(const tstring& path) {
        if (failfast) {
            deleteDirectoryRecursive(path);
        } else {
            updateStatus(deleteDirectoryRecursive(path, std::nothrow));
        }
        return true;
    }
};


class BatchDeleter {
    const tstring dirPath;
    bool recursive;
public:
    explicit BatchDeleter(const tstring& path): dirPath(path) {
        deleteSubdirs(false);
    }

    BatchDeleter& deleteSubdirs(bool v) {
        recursive = v;
        return *this;
    }

    void execute() const {
        if (!isFileExists(dirPath)) {
            return;
        }
        iterateDirectory(true /* fail fast */);
        if (recursive) {
            deleteDirectory(dirPath);
        }
    }

    bool execute(const std::nothrow_t&) const {
        if (!isFileExists(dirPath)) {
            return true;
        }

        if (!isDirectory(dirPath)) {
            return false;
        }

        JP_TRY;
        if (!iterateDirectory(false /* ignore errors */)) {
            return false;
        }
        if (recursive) {
            return deleteDirectory(dirPath, std::nothrow);
        }
        return true;
        JP_CATCH_ALL;

        return false;
    }

private:
    bool iterateDirectory(bool failfast) const {
        std::unique_ptr<DeleteFilesCallback> callback;
        if (recursive) {
            callback = std::unique_ptr<DeleteFilesCallback>(
                                            new DeleteAllCallback(failfast));
        } else {
            callback = std::unique_ptr<DeleteFilesCallback>(
                                            new DeleteFilesCallback(failfast));
        }

        FileUtils::iterateDirectory(dirPath, *callback);
        return callback->good();
    }
};

} // namespace

void deleteFilesInDirectory(const tstring &dirPath) {
    BatchDeleter(dirPath).execute();
}

bool deleteFilesInDirectory(const tstring &dirPath,
                                            const std::nothrow_t &) throw() {
    return BatchDeleter(dirPath).execute(std::nothrow);
}

void deleteDirectoryRecursive(const tstring &dirPath) {
    BatchDeleter(dirPath).deleteSubdirs(true).execute();
}

bool deleteDirectoryRecursive(const tstring &dirPath,
                                            const std::nothrow_t &) throw() {
    return BatchDeleter(dirPath).deleteSubdirs(true).execute(std::nothrow);
}

namespace {

struct FindFileDeleter {
    typedef HANDLE pointer;

    void operator()(HANDLE h) {
        if (h && h != INVALID_HANDLE_VALUE) {
            FindClose(h);
        }
    }
};

typedef std::unique_ptr<HANDLE, FindFileDeleter> UniqueFindFileHandle;

}; // namesace
void iterateDirectory(const tstring &dirPath, DirectoryCallback& callback)
{
    const tstring searchString = combinePath(dirPath, _T("*"));
    WIN32_FIND_DATA findData;
    UniqueFindFileHandle h(FindFirstFile(searchString.c_str(), &findData));
    if (h.get() == INVALID_HANDLE_VALUE) {
        // GetLastError() == ERROR_FILE_NOT_FOUND is OK
        // - no files in the directory
        // ERROR_PATH_NOT_FOUND is returned
        // if the parent directory does not exist
        if (GetLastError() != ERROR_FILE_NOT_FOUND) {
            JP_THROW(SysError(tstrings::any() << "FindFirstFile("
                    << dirPath << ") failed", FindFirstFile));
        }
        return;
    }

    do {
        const tstring fname(findData.cFileName);
        const tstring filePath = combinePath(dirPath, fname);
        if (!isDirectoryAttrs(findData.dwFileAttributes)) {
            if (!callback.onFile(filePath)) {
                return;
            }
        } else if (fname != _T(".") && fname != _T("..")) {
            if (!callback.onDirectory(filePath)) {
                return;
            }
        }
    } while (FindNextFile(h.get(), &findData));

    // expect GetLastError() == ERROR_NO_MORE_FILES
    if (GetLastError() != ERROR_NO_MORE_FILES) {
        JP_THROW(SysError(tstrings::any() << "FindNextFile("
                << dirPath << ") failed", FindNextFile));
    }
}


DirectoryIterator& DirectoryIterator::findItems(tstring_array& v) {
    if (!isDirectory(root)) {
        return *this;
    }

    iterateDirectory(root, *this);
    v.insert(v.end(), items.begin(), items.end());
    items = tstring_array();
    return *this;
}

bool DirectoryIterator::onFile(const tstring& path) {
    if (theWithFiles) {
        items.push_back(path);
    }
    return true;
}

bool DirectoryIterator::onDirectory(const tstring& path) {
    if (theWithFolders) {
        items.push_back(path);
    }
    if (theRecurse) {
        DirectoryIterator(path).recurse(theRecurse)
                .withFiles(theWithFiles)
                .withFolders(theWithFolders)
                .findItems(items);
    }
    return true;
}


namespace {

struct DeleterFunctor {
    // Order of items in the following enum is important!
    // It controls order in which items of particular type will be deleted.
    // See Deleter::execute().
    enum {
        File,
        FilesInDirectory,
        RecursiveDirectory,
        EmptyDirectory
    };

    void operator () (const Deleter::Path& path) const {
        switch (path.second) {
#define DELETE_SOME(o, f)\
        case o:\
            f(path.first, std::nothrow);\
            break

        DELETE_SOME(File, deleteFile);
        DELETE_SOME(EmptyDirectory, deleteDirectory);
        DELETE_SOME(FilesInDirectory, deleteFilesInDirectory);
        DELETE_SOME(RecursiveDirectory, deleteDirectoryRecursive);

#undef DELETE_SOME
        default:
            break;
        }
    }
};

} // namespace

void Deleter::execute() {
    Paths tmp;
    tmp.swap(paths);

    // Reorder items to delete.
    std::stable_sort(tmp.begin(), tmp.end(), [] (const Paths::value_type& a,
                                                const Paths::value_type& b) {
        return a.second < b.second;
    });

    std::for_each(tmp.begin(), tmp.end(), DeleterFunctor());
}

Deleter& Deleter::appendFile(const tstring& path) {
    paths.push_back(std::make_pair(path, DeleterFunctor::File));
    return *this;
}

Deleter& Deleter::appendEmptyDirectory(const Directory& dir) {
     tstring path =  normalizePath(removeTrailingSlash(dir));
     const tstring parent = normalizePath(removeTrailingSlash(dir.parent));
     while(parent != path) {
         appendEmptyDirectory(path);
         path = dirname(path);
     }

    return *this;
}

Deleter& Deleter::appendEmptyDirectory(const tstring& path) {
    paths.push_back(std::make_pair(path, DeleterFunctor::EmptyDirectory));
    return *this;
}

Deleter& Deleter::appendAllFilesInDirectory(const tstring& path) {
    paths.push_back(std::make_pair(path, DeleterFunctor::FilesInDirectory));
    return *this;
}

Deleter& Deleter::appendRecursiveDirectory(const tstring& path) {
    paths.push_back(std::make_pair(path, DeleterFunctor::RecursiveDirectory));
    return *this;
}


FileWriter::FileWriter(const tstring& path): dstPath(path) {
    tmpFile = FileUtils::createTempFile(_T("jds"), _T(".tmp"),
            FileUtils::dirname(path));

    cleaner.appendFile(tmpFile);

    // we want to get exception on error
    tmp.exceptions(std::ifstream::failbit | std::ifstream::badbit);
    tmp.open(tmpFile, std::ios::binary | std::ios::trunc);
}

FileWriter& FileWriter::write(const void* buf, size_t bytes) {
    tmp.write(static_cast<const char*>(buf), bytes);
    return *this;
}

void FileWriter::finalize() {
    tmp.close();

    FileUtils::moveFile(tmpFile, dstPath, false);

    // cancel file deletion
    cleaner.cancel();
}

tstring stripExeSuffix(const tstring& path) {
    // for windows - there is a ".exe" suffix to remove
    // allow for ".*" (last dot beyond the last slash)
    const tstring::size_type pos = path.rfind(_T("."));
    const tstring::size_type spos = path.rfind(_T("\\/"));
    if (pos == tstring::npos || (spos > pos && spos != tstring::npos)) {
        return path;
    }
    return path.substr(0, pos);
}

} //  namespace FileUtils
