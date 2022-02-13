# Patches for citron on SerenityOS

## `0001-Remove-m-tune-arch-native.patch`

Remove -m(tune arch)=native


## `0002-Don-t-use-execinfo-on-serenity.patch`

Don't use execinfo on serenity


## `0003-Get-rid-of-wordexp-on-serenity.patch`

Get rid of wordexp on serenity


## `0004-Use-fcntl.h-on-serenity.patch`

Use <fcntl.h> on serenity


## `0005-Pull-arc4random-from-stdlib.h.patch`

Pull arc4random from stdlib.h


## `0006-Don-t-use-prctl.patch`

Don't use prctl


## `0007-Make-fiber-a-noop.patch`

Make fiber a noop

Serenity doesn't have ucontext.

## `0008-Make-coroutines-a-noop.patch`

Make coroutines a noop

Serenity doesn't have ucontext.

## `0009-Use-setjmp-for-callcc.patch`

Use setjmp for callcc


## `0010-Don-t-mess-with-libsocket.patch`

Don't mess with libsocket


## `0011-Disable-inject.patch`

Disable inject

tcc requires ucontext.

## `0012-Use-unsigned-short-int-instead-of-u-short-int.patch`

Use unsigned (short int) instead of u(short int)


## `0013-Disable-openmp.patch`

Disable openmp


## `0014-Yoink-out-libs.patch`

Yoink out libs


## `0015-Make-boehm-gc-optional.patch`

Make boehm gc optional


## `0016-don-t-build-tcc-for-no-reason.patch`

don't build tcc for no reason


## `0017-Respect-DESTDIR.patch`

Respect DESTDIR


## `0018-Disable-GC-on-serenity.patch`

Disable GC on serenity


