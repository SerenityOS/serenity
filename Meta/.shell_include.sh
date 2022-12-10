#!/usr/bin/env bash
# shellcheck disable=SC2034
# SC2034: "Variable appears unused. Verify it or export it."
#         Those are intentional here, as the file is meant to be included elsewhere.

# NOTE: If using another privilege escalation binary make sure it is configured or has the appropiate flag
#       to keep the current environment variables in the launched process (in sudo's case this is achieved
#       through the -E flag described in sudo(8).
SUDO="sudo -E"

if [ "$(uname -s)" = "SerenityOS" ]; then
    SUDO="pls -E"
fi

die() {
    echo "die: $*"
    exit 1
}

find_executable() {
  paths=("/usr/sbin" "/sbin")

  if [ "$(uname -s)" = "Darwin" ]; then
    paths+=("/usr/local/opt/e2fsprogs/bin" "/usr/local/opt/e2fsprogs/sbin")
    paths+=("/opt/homebrew/opt/e2fsprogs/bin" "/opt/homebrew/opt/e2fsprogs/sbin")
  fi

  executable="${1}"

  # Prefer tools from PATH over fallback paths
  if command -v "${executable}"; then
    return 0
  fi

  for path in "${paths[@]}"; do
    if command -v "${path}/${executable}"; then
      return 0
    fi
  done

  # We return the executable's name back to provide meaningful messages on future failure
  echo "${executable}"
}

FUSE2FS_PATH="$(find_executable fuse2fs)"
RESIZE2FS_PATH="$(find_executable resize2fs)"
E2FSCK_PATH="$(find_executable e2fsck)"
