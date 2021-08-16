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

#ifndef WINFILEUTILS_H
#define WINFILEUTILS_H


#include <fstream>
#include "SysInfo.h"


namespace FileUtils {

    // Creates a file with unique name in the specified base directory,
    // throws an exception if operation fails
    // path is constructed as <prefix><random number><suffix>.
    // The function fails and throws exception if 'path' doesn't exist.
    tstring createTempFile(const tstring &prefix = _T(""),
            const tstring &suffix = _T(".tmp"),
            const tstring &path=SysInfo::getTempDir());

    // Creates a directory with unique name in the specified base directory,
    // throws an exception if operation fails
    // path is constructed as <prefix><random number><suffix>
    // The function fails and throws exception if 'path' doesn't exist.
    tstring createTempDirectory(const tstring &prefix = _T(""),
            const tstring &suffix = _T(".tmp"),
            const tstring &basedir=SysInfo::getTempDir());

    // If the file referenced with "prototype" parameter DOES NOT exist,
    // the return value is the given path. No new files created.
    // Otherwise the function creates another file in the same directory as
    // the given file with the same suffix and with the basename from the
    // basename of the given file with some random chars appended to ensure
    // created file is unique.
    tstring createUniqueFile(const tstring &prototype);

    // Creates directory and subdirectories if don't exist.
    // Currently supports only "standard" path like "c:\bla-bla"
    // If 'createdDirs' parameter is not NULL, the given array is appended with
    // all subdirectories created by this function call.
    void createDirectory(const tstring &path, tstring_array* createdDirs=0);

    // copies file from fromPath to toPath.
    // Creates output directory if doesn't exist.
    void copyFile(const tstring& fromPath, const tstring& toPath,
            bool failIfExists);

    // moves file from fromPath to toPath.
    // Creates output directory if doesn't exist.
    void moveFile(const tstring& fromPath, const tstring& toPath,
            bool failIfExists);

    // Throws exception if fails to delete specified 'path'.
    // Exits normally if 'path' doesn't exist or it has been deleted.
    // Attempts to strip R/O attribute if delete fails and retry delete.
    void deleteFile(const tstring &path);
    // Returns 'false' if fails to delete specified 'path'.
    // Returns 'true' if 'path' doesn't exist or it has been deleted.
    // Attempts to strip R/O attribute if delete fails and retry delete.
    bool deleteFile(const tstring &path, const std::nothrow_t &) throw();

    // Like deleteFile(), but applies to directories.
    void deleteDirectory(const tstring &path);
    bool deleteDirectory(const tstring &path, const std::nothrow_t &) throw();

    // Deletes all files (not subdirectories) from the specified directory.
    // Exits normally if all files in 'dirPath' have been deleted or if
    // 'dirPath' doesn't exist.
    // Throws exception if 'dirPath' references existing file system object
    // which is not a directory or when the first failure of file delete
    // occurs.
    void deleteFilesInDirectory(const tstring &dirPath);
    // Deletes all files (not subdirectories) from the specified directory.
    // Returns 'true' normally if all files in 'dirPath' have been deleted or
    // if 'dirPath' doesn't exist.
    // Returns 'false' if 'dirPath' references existing file system object
    // which is not a directory or if failed to delete one ore more files in
    // 'dirPath' directory.
    // Doesn't abort iteration over files if the given directory after the
    // first failure to delete a file.
    bool deleteFilesInDirectory(const tstring &dirPath,
            const std::nothrow_t &) throw();
    // Like deleteFilesInDirectory, but deletes subdirectories as well
    void deleteDirectoryRecursive(const tstring &dirPath);
    bool deleteDirectoryRecursive(const tstring &dirPath,
            const std::nothrow_t &) throw();

    class DirectoryCallback {
    public:
        virtual ~DirectoryCallback() {};

        virtual bool onFile(const tstring& path) {
            return true;
        }
        virtual bool onDirectory(const tstring& path) {
            return true;
        }
    };

    // Calls the given callback for every file and subdirectory of
    // the given directory.
    void iterateDirectory(const tstring &dirPath, DirectoryCallback& callback);

    class DirectoryIterator: DirectoryCallback {
    public:
        DirectoryIterator(const tstring& root=tstring()): root(root) {
            recurse().withFiles().withFolders();
        }

        DirectoryIterator& recurse(bool v=true) {
            theRecurse = v;
            return *this;
        }

        DirectoryIterator& withFiles(bool v=true) {
            theWithFiles = v;
            return *this;
        }

        DirectoryIterator& withFolders(bool v=true) {
            theWithFolders = v;
            return *this;
        }

        tstring_array findItems() {
            tstring_array reply;
            findItems(reply);
            return reply;
        }

        DirectoryIterator& findItems(tstring_array& v);

    private:
        virtual bool onFile(const tstring& path);
        virtual bool onDirectory(const tstring& path);

    private:
        bool theRecurse;
        bool theWithFiles;
        bool theWithFolders;
        tstring root;
        tstring_array items;
    };

    // Returns array of all the files/sub-folders from the given directory,
    // empty array if basedir is not a directory. The returned
    // array is ordered from top down (i.e. dirs are listed first followed
    // by subfolders and files).
    // Order of subfolders and files is undefined
    // but usually they are sorted by names.
    inline tstring_array listAllContents(const tstring& basedir) {
        return DirectoryIterator(basedir).findItems();
    }

    struct Directory {
        Directory() {
        }

        Directory(const tstring &parent,
                const tstring &subdir) : parent(parent), subdir(subdir)  {
        }

        operator tstring () const {
            return getPath();
        }

        tstring getPath() const {
            return combinePath(parent, subdir);
        }

        bool empty() const {
            return (parent.empty() && subdir.empty());
        }

        tstring parent;
        tstring subdir;
    };

    // Deletes list of files and directories in batch mode.
    // Registered files and directories are deleted when destructor is called.
    // Order or delete operations is following:
    //  - delete items registered with appendFile() calls;
    //  - delete items registered with appendAllFilesInDirectory() calls;
    //  - delete items registered with appendRecursiveDirectory() calls;
    //  - delete items registered with appendEmptyDirectory() calls.
    class Deleter {
    public:
        Deleter() {
        }

        ~Deleter() {
            execute();
        }

        typedef std::pair<tstring, int> Path;
        typedef std::vector<Path> Paths;

        /**
         * Appends all records from the given deleter Deleter into this Deleter
         * instance. On success array with records in the passed in Deleter
         * instance is emptied.
         */
        Deleter& appendFrom(Deleter& other) {
            Paths tmp(paths);
            tmp.insert(tmp.end(), other.paths.begin(), other.paths.end());
            Paths empty;
            other.paths.swap(empty);
            paths.swap(tmp);
            return *this;
        }

        // Schedule file for deletion.
        Deleter& appendFile(const tstring& path);

        // Schedule files for deletion.
        template <class It>
        Deleter& appendFiles(It b, It e) {
            for (It it = b; it != e; ++it) {
                appendFile(*it);
            }
            return *this;
        }

        // Schedule files for deletion in the given directory.
        template <class It>
        Deleter& appendFiles(const tstring& dirname, It b, It e) {
            for (It it = b; it != e; ++it) {
                appendFile(FileUtils::mkpath() << dirname << *it);
            }
            return *this;
        }

        // Schedule empty directory for deletion with empty roots
        // (up to Directory.parent).
        Deleter& appendEmptyDirectory(const Directory& dir);

        // Schedule empty directory for deletion without roots.
        // This is a particular case of
        // appendEmptyDirectory(const Directory& dir)
        // with Directory(dirname(path), basename(path)).
        Deleter& appendEmptyDirectory(const tstring& path);

        // Schedule all file from the given directory for deletion.
        Deleter& appendAllFilesInDirectory(const tstring& path);

        // Schedule directory for recursive deletion.
        Deleter& appendRecursiveDirectory(const tstring& path);

        void cancel() {
            paths.clear();
        }

        // Deletes scheduled files and directories. After this function
        // is called internal list of scheduled items is emptied.
        void execute();

    private:
        Paths paths;
    };


    /**
     * Helper to write chunks of data into binary file.
     * Creates temporary file in the same folder with destination file.
     * All subsequent requests to save data chunks are redirected to temporary
     * file. finalize() method closes temporary file stream and renames
     * temporary file.
     * If finalize() method is not called, temporary file is deleted in
     * ~FileWriter(), destination file is not touched.
     */
    class FileWriter {
    public:
        explicit FileWriter(const tstring& path);

        FileWriter& write(const void* buf, size_t bytes);

        template <class Ctnr>
        FileWriter& write(const Ctnr& buf) {
            return write(buf.data(),
                            buf.size() * sizeof(typename Ctnr::value_type));
        }

        void finalize();

    private:
        // Not accessible by design!
        FileWriter& write(const std::wstring& str);

    private:
        tstring tmpFile;
        Deleter cleaner;
        std::ofstream tmp;
        tstring dstPath;
    };
} // FileUtils

#endif // WINFILEUTILS_H
