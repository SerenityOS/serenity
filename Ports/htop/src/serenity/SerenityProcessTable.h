/*
htop - serenity/SerenityProcessTable.h
(C) 2026 SerenityOS contributors
Released under the GNU GPLv2+, see the COPYING file
in the source distribution for its full text.
*/

#ifndef HEADER_SerenityProcessTable
#define HEADER_SerenityProcessTable

#include "Hashtable.h"
#include "ProcessTable.h"
#include "UsersTable.h"

typedef struct SerenityProcessTable_ {
    ProcessTable super;
} SerenityProcessTable;

#endif /* HEADER_SerenityProcessTable */
