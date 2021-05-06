/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <LibCore/Object.h>
#include <LibSQL/StorageForward.h>

namespace SQL {

class Database : public Core::Object {
    C_OBJECT(Database);
public:
    explicit Database(String);

    void commit();
    void add_table(TableDef &table);
    Optional<Key> get_table_key(String table_name);
    RefPtr<TableDef> get_table(String);

    Vector<Tuple> select_all(TableDef const&);
    Vector<Tuple> match(TableDef const&, Key const&);
    bool insert(Tuple &);
    bool update(Tuple &);

private:
    RefPtr<Heap> m_heap;
    RefPtr<BTree> m_tables;
    RefPtr<BTree> m_table_columns;
};

}
