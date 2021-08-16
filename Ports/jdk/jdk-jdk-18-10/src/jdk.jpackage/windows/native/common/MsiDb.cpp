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

#include "MsiDb.h"
#include "FileUtils.h"
#include "WinFileUtils.h"
#include "Log.h"


#pragma comment(lib, "msi.lib")


namespace msi {

void closeDatabaseView(MSIHANDLE hView) {
    if (hView) {
        const auto status = MsiViewClose(hView);
        if (status != ERROR_SUCCESS) {
            LOG_WARNING(tstrings::any() << "MsiViewClose("
                                << hView << ") failed with error=" << status);
            return;
        }
        closeMSIHANDLE(hView);
    }
}


namespace {
MSIHANDLE openDatabase(const tstring& msiPath) {
    MSIHANDLE h = 0;
    const UINT status = MsiOpenDatabase(msiPath.c_str(),
                                                    MSIDBOPEN_READONLY, &h);
    if (status != ERROR_SUCCESS) {
        JP_THROW(Error(tstrings::any()
                                << "MsiOpenDatabase(" << msiPath
                                << ", MSIDBOPEN_READONLY) failed", status));
    }
    return h;
}

} // namespace

Database::Database(const Guid& productCode):
        msiPath(getProductInfo(productCode, INSTALLPROPERTY_LOCALPACKAGE)),
                                            dbHandle(openDatabase(msiPath)) {
}


Database::Database(const tstring& msiPath): msiPath(msiPath),
                                            dbHandle(openDatabase(msiPath)) {
}


tstring Database::getProperty(const tstring& name) const {
    // Query value of a property with the given name from 'Property' MSI table.
    const tstring sqlQuery = (tstrings::any()
                    << "SELECT Value FROM Property WHERE Property = '"
                    << name << "'").tstr();

    DatabaseView view(*this, sqlQuery);
    const DatabaseRecord record(view);

    // Data is stored in a record object. SQL query is constructed in a way
    // this record object contains a single field.
    // Verify record contains exactly one field.
    if (record.getFieldCount() != 1) {
        JP_THROW(Error(
                    tstrings::any() << "record.getFieldCount(" << msiPath
                                    << ", " << sqlQuery
                                    << ") returned unexpected value",
                    ERROR_SUCCESS));
    }

    // Field identifier. They start with 1, not from 0.
    const unsigned field = 1;
    return record.getString(field);
}


tstring Database::getProperty(const std::nothrow_t&, const tstring& name) const {
    try {
        return getProperty(name);
    } catch (const NoMoreItemsError&) {
    }
    JP_CATCH_EXCEPTIONS;
    return tstring();
}


DatabaseRecord::DatabaseRecord(unsigned fieldCount) {
    MSIHANDLE h = MsiCreateRecord(fieldCount);
    if (!h) {
        JP_THROW(msi::Error(tstrings::any() << "MsiCreateRecord("
                        << fieldCount << ") failed", ERROR_FUNCTION_FAILED));
    }
    handle = h;
}


DatabaseRecord& DatabaseRecord::operator=(const DatabaseRecord& other) {
    DatabaseRecord tmp(other);
    std::swap(handle, tmp.handle);
    return *this;
}


DatabaseRecord& DatabaseRecord::fetch(DatabaseView& view) {
    *this = view.fetch();
    return *this;
}


DatabaseRecord& DatabaseRecord::tryFetch(DatabaseView& view) {
    *this = view.tryFetch();
    return *this;
}


DatabaseRecord& DatabaseRecord::setString(unsigned idx, const tstring& v) {
    const UINT status = MsiRecordSetString(handle, idx, v.c_str());
    if (status != ERROR_SUCCESS) {
        JP_THROW(Error(tstrings::any() << "MsiRecordSetString(" << idx
                                        << ", " << v << ") failed", status));
    }
    return *this;
}


DatabaseRecord& DatabaseRecord::setInteger(unsigned idx, int v) {
    const UINT status = MsiRecordSetInteger(handle, idx, v);
    if (status != ERROR_SUCCESS) {
        JP_THROW(Error(tstrings::any() << "MsiRecordSetInteger(" << idx
                                        << ", " << v << ") failed", status));
    }
    return *this;
}


DatabaseRecord& DatabaseRecord::setStreamFromFile(unsigned idx,
                                                        const tstring& v) {
    const UINT status = MsiRecordSetStream(handle, idx, v.c_str());
    if (status != ERROR_SUCCESS) {
        JP_THROW(Error(tstrings::any() << "MsiRecordSetStream(" << idx
                                        << ", " << v << ") failed", status));
    }
    return *this;
}


unsigned DatabaseRecord::getFieldCount() const {
    const unsigned reply = MsiRecordGetFieldCount(handle);
    if (int(reply) <= 0) {
        JP_THROW(Error(std::string("MsiRecordGetFieldCount() failed"),
                                                    ERROR_FUNCTION_FAILED));
    }
    return reply;
}


int DatabaseRecord::getInteger(unsigned idx) const {
    int const reply = MsiRecordGetInteger(handle, idx);
    if (reply == MSI_NULL_INTEGER) {
        JP_THROW(Error(tstrings::any() << "MsiRecordGetInteger(" << idx
                                        << ") failed", ERROR_FUNCTION_FAILED));
    }
    return reply;
}


void DatabaseRecord::saveStreamToFile(unsigned idx,
                                                const tstring& path) const {
    enum { ReadStreamBufferBytes = 1024 * 1024 };

    FileUtils::FileWriter writer(path);

    std::vector<char> buffer(ReadStreamBufferBytes);
    DWORD bytes;
    do {
        bytes = ReadStreamBufferBytes;
        const UINT status = MsiRecordReadStream(handle, UINT(idx),
                                                    buffer.data(), &bytes);
        if (status != ERROR_SUCCESS) {
            JP_THROW(Error(std::string("MsiRecordReadStream() failed"),
                                                                    status));
        }
        writer.write(buffer.data(), bytes);
    } while(bytes == ReadStreamBufferBytes);
    writer.finalize();
}


DatabaseView::DatabaseView(const Database& db, const tstring& sqlQuery,
            const DatabaseRecord& queryParam): db(db), sqlQuery(sqlQuery) {
    MSIHANDLE h = 0;

    // Create SQL query.
    for (const UINT status = MsiDatabaseOpenView(db.dbHandle,
                            sqlQuery.c_str(), &h); status != ERROR_SUCCESS; ) {
        JP_THROW(Error(tstrings::any() << "MsiDatabaseOpenView("
                                        << sqlQuery << ") failed", status));
    }

    // Run SQL query.
    for (const UINT status = MsiViewExecute(h, queryParam.handle);
                                                status != ERROR_SUCCESS; ) {
        closeMSIHANDLE(h);
        JP_THROW(Error(tstrings::any() << "MsiViewExecute("
                                        << sqlQuery << ") failed", status));
    }

    // MsiViewClose should be called only after
    // successful MsiViewExecute() call.
    handle = h;
}


DatabaseRecord DatabaseView::fetch() {
    DatabaseRecord reply = tryFetch();
    if (reply.empty()) {
        JP_THROW(NoMoreItemsError(tstrings::any() << "No more items in ["
                                                << sqlQuery << "] query"));
    }
    return reply;
}


DatabaseRecord DatabaseView::tryFetch() {
    MSIHANDLE h = 0;

    // Fetch data from executed SQL query.
    // Data is stored in a record object.
    for (const UINT status = MsiViewFetch(handle, &h);
                                                status != ERROR_SUCCESS; ) {
        if (status == ERROR_NO_MORE_ITEMS) {
            return DatabaseRecord();
        }

        JP_THROW(Error(tstrings::any() << "MsiViewFetch(" << sqlQuery
                                                    << ") failed", status));
    }

    DatabaseRecord reply;
    reply.handle = h;
    return reply;
}


DatabaseView& DatabaseView::modify(const DatabaseRecord& record,
                                                        MSIMODIFY mode) {
    const UINT status = MsiViewModify(handle, mode, record.handle);
    if (status != ERROR_SUCCESS) {
        JP_THROW(Error(tstrings::any() << "MsiViewModify(mode=" << mode
                                                    << ") failed", status));
    }
    return *this;
}

} // namespace msi
