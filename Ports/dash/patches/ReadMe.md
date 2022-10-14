# Patches for dash on SerenityOS

## `0001-Replace-a-use-of-wait3-with-waitpid-in-the-job-contr.patch`

Replace a use of wait3() with waitpid() in the job control

wait3() does not exist on serenity.

## `0002-Skip-building-helpers-by-default.patch`

Skip building helpers by default


