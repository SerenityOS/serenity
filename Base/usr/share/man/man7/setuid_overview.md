## Name

Overview of real, effective, and saved user and group IDs

## Description

Each process runs has a user ID and a group ID. By default, a new process inherits the user ID and group ID of its parent process.

Each file has permissions that control if it can be read, written, and executed by processes belonging to its owner, by processes belonging to other users in the owner's group, and by processes belonging to others.

In addition to the access permissions bits, executables may have the "Set User ID"  and the "Set Group ID" bit set. When executables with these bits set are executed, they run with the user or group ID of the executable, instead of with the user and group ID of their parent process. These binaries are also called "SUID binaries" or "setuid binaries" (or "SGID binaries" or "setgid binaries" for the "Set Group ID" bit).

The motivation behind SUID binaries is that they allow users to do tasks that would normally require elevated permissions, without having to give users these permissions fully.

For example, `/bin/ping` has the Set User ID bit set and is owned by user root in group root. So if any process executes `/bin/ping`, it will run as root, which means it will be able to send network packets, even if the current process doesn't normally have network access.

For another example, many other Unix systems contain a utility `passwd` that changes the password of the current user. To store the password, it has to write to `/etc/shadow` -- but the current user is not supposed to have write access to `/etc/shadow` so that they cannot change passwords of other users. The solution is to make `passwd` a SUID binary. (SerenityOS currently doesn't have support for user passwords.)

SUID binaries mean that the effective user ID of a process and the real user ID of a process can be different. When a SUID binary is executed, it assumes the owner of the binary as effective user ID, and the user ID of the parent process that executed it as real user ID. The effective user ID is used for permission checks.

Since SUID binaries are able to bypass access checks, only carefully selected binaries should be made SUID. If, for example, `cp` was SUID root, everyone could overwrite every file using this `cp` binary.

In some instances, it is useful for a SUID binary to either temporarily or permanently drop its permissions and set the effective user ID to the real user ID.

To make this possible, each process has *three* user (and group) IDs: The (real) user ID, the *effective* user ID, and the *saved* user ID. When a process executes a normal binary, all three IDs are set to the parent process's user ID. However, when a process executes a SUID binary, the process runs with the paren process's ID as its real ID, but it takes its effective ID and saved ID from the binary. (Analogously for the group ID for SGID binaries.)

The function [`setresuid`(2)](../man2/getresuid.md) can change the real, effective, and saved user ID of a process -- but for non-root processes it is only valid to set each new ID to the current value of real, effective, or saved user ID. Since SUID binaries start with the binary's owner as effective and saved user ID and with the current user's ID as real user ID, this allows switching the effective user ID between the SUID owner's ID and the current user's ID.

Hence, to temporarily drop SUID privileges, set the effective ID to a less privileged user ID, and store the current effective user ID in the saved user ID so that it can be restored in a later call:

```c++
if (setresuid(-1, new_uid, geteuid()) < 0)
  return OH_NO;
```

(Since the saved ID starts out as the file owner's ID for SUID binaries, this should in practice be the same as `seteuid(getuid())`, but it's easier to reason about.)

The process then has fewer permissions, but since the former effective ID is still stored in the saved ID, the process can restore its former permissions with:

```c++
uid_t ruid, euid, suid;
if (getresuid(&ruid, &euid, &suid) < 0)
  return OH_NO;
if (setresuid(-1, suid, -1) < 0)
  return OH_NO;
```

(Since SUID binaries are often owned by root who has user ID 0, this is often identical to `seteuid(0)` -- in particular, if a SUID root binary accepts user input in an unsafe way with temporarily dropped privileges, then if a user manages to take control of the binary with malicious input they can restore privileges with `seteuid(0)`.)

A process can permanently drop its SUID privileges by copying the real user ID into both effective and saved user ID. Then, it's impossible to set the effective ID to anything else (assuming the current user isn't the superuser). To permanently drop privileges:

```c++
if (setresuid(new_uid, new_uid, new_uid) < 0)
  return OH_NO;
```

(On SerenityOS, this is usually the same as calling `setuid(new_uid)`, but easier to reason about.)

Changing group IDs is analogous.  Since changing the user ID changes the permissions of a process, group privileges should be dropped before user privileges are dropped, and if they're dropped temporarily, user privileges should be restored before group privileges are restored.

For historical reasons, there are many functions for setting and getting these IDs. `setresuid()`, `setresgid()`, `getresuid()`, and `getresgid()` are the most flexible of these functions and they have the easiest to understand semantics.

## See also

* "Setuid Demystified", Proceedings of the 11th USENIX Security Symposium, August 2002, Pages 171–190
* [`getresuid`(2) / `getresgid`(2)](../man2/getresuid.md)
* [`geteuid`(2) / `getegid`(2)](../man2/geteuid.md)
* [`getuid`(2) / `getgid`(2)](../man2/getuid.md)
* [`seteuid`(2) / `setegid`(2)](../man2/seteuid.md)
* [`setuid`(2) / `setgid`(2)](../man2/setuid.md)
* [`setresuid`(2) / `setresgid`(2)](../man2/setresuid.md)
