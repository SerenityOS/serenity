/*
htop - serenity/ProcessField.h
(C) 2026 SerenityOS contributors
Released under the GNU GPLv2+, see the COPYING file
in the source distribution for its full text.
*/

#ifndef HEADER_SerenityProcessField
#define HEADER_SerenityProcessField

#define PLATFORM_PROCESS_FIELDS  \
    PLEDGE = 100,                \
    VEIL = 101,                  \
    SERENITY_INODE_FAULTS = 102, \
    SERENITY_ZERO_FAULTS = 103,  \
    SERENITY_COW_FAULTS = 104,   \
                                 \
    DUMMY_BUMP_FIELD = CWD, // End of list

#endif /* HEADER_SerenityProcessField */
