/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef MsiDb_h
#define MsiDb_h

#include <windows.h>
#include <msiquery.h>

#include "MsiUtils.h"


class Guid;

namespace msi {

void closeDatabaseView(MSIHANDLE h);

class CA;
class DatabaseView;
class DatabaseRecord;


/**
 * Opens product's database to query properties.
 * The database is opened in R/O mode, i.e. it is safe to call this method
 * even if there is active install/uninstall session. Unlike MsiOpenProduct(),
 * it never fails with 1618 ('Another installation is
 * already in progress') error code.
 *
 * Database can be opened from product code GUID, path to msi package or from
 * custom action.
 *
 * If opened from CA the database is opened in R/W mode, however only adding
 * new temporary records is supported. It is forbidden to change data in
 * existing records.
 */
class Database {
public:
    /**
     * Opens msi database from the given product code GUID.
     * Throws exception if fails.
     */
    explicit Database(const Guid& productCode);

    /**
     * Opens msi database from the given path to .msi file.
     * Throws exception if fails.
     */
    explicit Database(const tstring& msiPath);

    /**
     * Opens msi database from the given custom action.
     * Throws exception if fails.
     */
    explicit Database(const CA& ca);

    ~Database() {
      if (dbHandle != 0) {
        closeMSIHANDLE(dbHandle);
      }
    }

    /**
     * Returns value of property with the given name.
     * Throws NoMoreItemsError if property with the given name doesn't exist
     * or Error if some error occurred.
     */
    tstring getProperty(const tstring& name) const;

    /**
     * Returns value of property with the given name.
     * Returns empty string if property with the given name doesn't exist or
     * if some error occurred.
     */
    tstring getProperty(const std::nothrow_t&, const tstring& name) const;

    friend class DatabaseView;

private:
    Database(const Database&);
    Database& operator=(const Database&);
private:
    const tstring msiPath;
    MSIHANDLE dbHandle;
};

typedef std::unique_ptr<Database> DatabasePtr;


class DatabaseRecord {
public:
    DatabaseRecord(): handle(MSIHANDLE(0)) {
    }

    DatabaseRecord(const DatabaseRecord& other): handle(MSIHANDLE(0)) {
        handle = other.handle;
        other.handle = 0;
    }

    DatabaseRecord& operator=(const DatabaseRecord& other);

    friend class DatabaseView;

    explicit DatabaseRecord(unsigned fieldCount);

    explicit DatabaseRecord(DatabaseView& view) : handle(MSIHANDLE(0)) {
        fetch(view);
    }

    ~DatabaseRecord() {
      if (handle != 0) {
        closeMSIHANDLE(handle);
      }
    }

    DatabaseRecord& fetch(DatabaseView& view);

    DatabaseRecord& tryFetch(DatabaseView& view);

    DatabaseRecord& setString(unsigned idx, const tstring& v);

    DatabaseRecord& setInteger(unsigned idx, int v);

    DatabaseRecord& setStreamFromFile(unsigned idx, const tstring& v);

    unsigned getFieldCount() const;

    tstring getString(unsigned idx) const;

    int getInteger(unsigned idx) const;

    void saveStreamToFile(unsigned idx, const tstring& path) const;

    bool empty() const {
        return 0 == handle;
    }

    MSIHANDLE getHandle() const {
        return handle;
    }

private:
    mutable MSIHANDLE handle;
};


class DatabaseView {
public:
    DatabaseView(const Database& db, const tstring& sqlQuery,
                        const DatabaseRecord& queryParam=DatabaseRecord());

    ~DatabaseView() {
      if (handle != 0) {
        closeMSIHANDLE(handle);
      }
    }

    DatabaseRecord fetch();

    DatabaseRecord tryFetch();

    DatabaseView& modify(const DatabaseRecord& record, MSIMODIFY mode);

private:
    tstring sqlQuery;
    const Database& db;
    MSIHANDLE handle;
};

} // namespace msi

#endif // #ifndef MsiDb_h
