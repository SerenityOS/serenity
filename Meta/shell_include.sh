# shellcheck shell=bash
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

exit_if_running_as_root() {
    if [ "$(id -u)" -eq 0 ]; then
       die "$*"
    fi
}

find_executable() {
  paths=("/usr/sbin" "/sbin")

  if [ "$(uname -s)" = "Darwin" ]; then
    if [ -n "${HOMEBREW_PREFIX}" ]; then
      paths+=("${HOMEBREW_PREFIX}/opt/e2fsprogs/bin" "${HOMEBREW_PREFIX}/opt/e2fsprogs/sbin")
    elif command -v brew > /dev/null 2>&1; then
      if prefix=$(brew --prefix e2fsprogs 2>/dev/null); then
        paths+=("${prefix}/bin" "${prefix}/sbin")
      fi
    fi
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
MKE2FS_PATH="$(find_executable mke2fs)"
