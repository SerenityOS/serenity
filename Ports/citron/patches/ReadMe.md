# Patches for citron on SerenityOS

## `0001-Get-rid-of-wordexp-on-serenity.patch`

Get rid of wordexp on serenity


## `0002-Make-fiber-a-noop.patch`

Make fiber a noop

Serenity doesn't have ucontext.

## `0003-Make-coroutines-a-noop.patch`

Make coroutines a noop

Serenity doesn't have ucontext.

## `0004-Don-t-mess-with-libsocket.patch`

Don't mess with libsocket


## `0005-Disable-inject.patch`

Disable inject

tcc requires ucontext.

## `0006-Disable-openmp.patch`

Disable openmp


## `0007-Disable-GC-on-serenity.patch`

Disable GC on serenity


## `0008-Don-t-use-libbsd-on-serenity.patch`

Don't use libbsd on serenity

arc4random exists on serenity, there's no need to pull libbsd in for
functionality that already exists.

## `0009-Disable-boehm-GC-on-serenity.patch`

Disable boehm GC on serenity

Serenity doesn't have a bdwgc port, so disable it here.

