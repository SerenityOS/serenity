# Patches for thesilversearcher on SerenityOS

## `0001-Add-the-thread-pledge-to-the-pledge-list.patch`

Add the thread pledge to the pledge list

On OpenBSD, stdio typically grants thread, but on Serenity it is its own
option.

