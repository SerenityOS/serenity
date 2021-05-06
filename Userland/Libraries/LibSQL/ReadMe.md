### SQL Storage Overview

#### Heap

A `Heap` is a logical container for database (SQL) data. Conceptually a 
`Heap` can be a database file, or a memory block, or another storage medium.

A `Heap` contains datastructures, like B-Trees, hash tables, or tuple stores
(basically a list of data tuples). 

A `Heap` can be thought of the backing storage of a single database. It's
assumed that a single SQL database is backed by a single `Heap`.

Right now only B-Trees and tuple stores are implemented.

#### Value

A `Value` is an atomic piece of SQL data. A `Value` has a basic type 
(Text/String, Integer, Float, etc). Richer types are implemented in higher
level layers, but the resulting data is stored in these `Value` objects.

#### Key

A `Key` is an element of a searchable data collection in a `Heap`. `Key`
objects stored in such a collection have a definition controlling the 
number of parts or columns the key has, the types of the parts, and the 
sort order of these parts. Besides having an optional definition, a `Key` 
consists of one `Value` object per part.In addition, keys have a `u32 pointer`
member which points to a `Heap` location.

`Key` objects without a definition can be used to locate/find objects in 
a searchable data collection.

Right now the `Key` definition is passed as an `IndexDefinition` meta 
data object, meaning that names are associated with both the definition
and the parts of the key. These names are not used, meaning that key 
definitions should probably be constructed in a different way.

#### Tuple

A tuple is an element of a sequentially accessible data collection like
a flat database table. Like a key it has a definition for all its parts,
but unlike a key this definition is not optional.

`Tuples` should logically belong to a `TupleStore` object, but right now
they stand by themselves; they contain a row's worth of data and a pointer
to the next `Tuple`.

#### BTree

The `BTree` class models a B-Tree index (duh). It contains a collection of 
`Key` objects organized in `TreeNode` objects. Keys can be inserted, 
located, deleted, and the set can be traversed in sort order. All keys in
a tree have the same underlying structure. A `BTree`'s `TreeNode`s and 
the keys it includes are lazily loaded from the `Heap` when needed.

The classes implementing the B-Tree functionality are `BTree`, `TreeNode`, 
`BTreeIterator`, and `DownPointer` (a smart pointer-like helper class).

#### Database

A `Database` object logically connects a `Heap` with the SQL data we want
to store in it. It has `BTree` pointers for B-Trees holding the definitions
of tables, columns, indexes, and other SQL objects.

#### Meta

The `meta.h` and `meta.cpp` contain objects describing tables, indexes, and
columns. It remains to be seen if this will survive in it's current form.

