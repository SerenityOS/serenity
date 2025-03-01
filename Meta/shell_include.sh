# shellcheck shell=bash
# shellcheck disable=SC2034
# SC2034: "Variable appears unused. Verify it or export it."
#         Those are intentional here, as the file is meant to be included elsewhere.

# NOTE: If using another privilege escalation binary make sure it is configured or has the appropriate flag
#       to keep the current environment variables in the launched process (in sudo's case this is achieved
#       through the -E flag described in sudo(8).
die() {
    echo "die: $*"
    exit 1
}

if [ "$(uname -s)" = "SerenityOS" ]; then
    SUDO="pls -E"
elif command -v sudo >/dev/null; then
    SUDO="sudo -E"
elif command -v doas >/dev/null; then
    if [ "$SUDO_UID" = '' ]; then
        SUDO_UID=$(id -u)
        SUDO_GID=$(id -g)
        export SUDO_UID SUDO_GID
    fi
    # To make doas work, you have to make sure you use the "keepenv" flag in doas.conf
    SUDO="doas"
else
    die "You need sudo, doas or pls to build Serenity..."
fi

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

get_number_of_processing_units() {
  number_of_processing_units="nproc"
  SYSTEM_NAME="$(uname -s)"

  if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
      number_of_processing_units="sysctl -n hw.ncpuonline"
  elif [ "$SYSTEM_NAME" = "FreeBSD" ]; then
      number_of_processing_units="sysctl -n hw.ncpu"
  elif [ "$SYSTEM_NAME" = "Darwin" ]; then
      number_of_processing_units="sysctl -n hw.ncpu"
  fi

  ($number_of_processing_units)
}

# Discover how to get apparent size from `du`. GNU/BusyBox du has --apparent-size / -b, BSD/Darwin du has `-A`.
if du --help 2>&1 | grep -qE "GNU coreutils|BusyBox"; then
    DU_APPARENT_SIZE_FLAG="-b"
else
    DU_APPARENT_SIZE_FLAG="-A"
fi

disk_usage() {
    # shellcheck disable=SC2003,SC2307
    expr "$(du ${DU_APPARENT_SIZE_FLAG} -sm "$1" | cut -f1)"
}

inode_usage() {
    find "$1" | wc -l
}

check_sha256() {
    if [ $# -ne 2 ]; then
        error "Usage: check_sha256 FILE EXPECTED_HASH"
        return 1
    fi

    FILE="${1}"
    EXPECTED_HASH="${2}"

    SYSTEM_NAME="$(uname -s)"
    if [ "$SYSTEM_NAME" = "Darwin" ]; then
        SEEN_HASH="$(shasum -a 256 "${FILE}" | cut -d " " -f 1)"
    else
        SEEN_HASH="$(sha256sum "${FILE}" | cut -d " " -f 1)"
    fi
    test "${EXPECTED_HASH}" = "${SEEN_HASH}"
}
