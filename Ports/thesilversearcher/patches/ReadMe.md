# Patches for thesilversearcher on SerenityOS

## `0001-pledge-thread.patch`

src/main.c: Add thread option to pledges.
On OpenBSD, stdio typically grants thread,
but on Serenity it is its own option.
