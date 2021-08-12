#!/usr/bin/env bash

set -euo pipefail

script_path="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"
cd "${script_path}/.."

script_name="$(basename "$0")"

function list_templates() {
  echo "Available templates:"
  for file in ./Base/res/devel/templates/*.ini; do
    printf '  %s - ' "$(basename "${file%%.ini}")"
    awk -F "=" '/Description/ { print $2 }' "$file"
  done
}

function usage() {
  local return_code=${1:-0}

  cat <<EOF
Usage: $script_name TEMPLATE DESTINATION

Instantiates a HackStudio template into a project at the given destination.
The last component of the destination path is used as the project name, and must
be a valid identifier (but hyphens are allowed and will be converted into
underscores).

Parameters:
  TEMPLATE - The HackStudio template to use.
  DESTINATION - The destination directory.

$(list_templates)
EOF
  exit "$return_code"
}

while [[ $# -ge 1 ]]; do
  case "$1" in
    -h|--help) usage;;
    -*) echo "$script_name: unknown parameter $1"; usage 1;;
    *) break;;
  esac
done

[[ $# -ne 2 ]] && echo "$script_name: takes exactly 2 parameters" && usage 1

TEMPLATE="$1"
DESTINATION="$2"

TEMPLATE_SOURCE_DIRECTORY="./Base/res/devel/templates/$TEMPLATE"
TEMPLATE_INI="./Base/res/devel/templates/$TEMPLATE.ini"
TEMPLATE_POSTCREATE="./Base/res/devel/templates/$TEMPLATE.postcreate"

if [[ ! -f "$TEMPLATE_INI" ]]; then
  echo "$script_name: unknown template \"$TEMPLATE\"."
  list_templates
  exit 1
fi

PROJECT_NAME="$(basename "$DESTINATION")"

if ! echo "$PROJECT_NAME" | grep -q '^[a-zA-Z][a-zA-Z0-9_\-]*$'; then
  echo "$script_name: The destination directory name contains invalid characters."
  echo "The destination directory name must be a valid identifier, with the exception of hyphens."
  exit 1
fi

if [[ -d "$DESTINATION" ]]; then
  echo "$script_name: Path already exists: $DESTINATION"
  echo "Refusing to overwrite it."
  exit 1
fi

sh="Build/lagom/shell"

if [[ ! -x $sh ]]; then
  echo "Building the Serenity shell, please wait..."
  Meta/serenity.sh build lagom shell_lagom
fi

echo "Instantiating template \"$TEMPLATE\" at \"$DESTINATION\"..."
mkdir -p "$DESTINATION"

if [[ -d "$TEMPLATE_SOURCE_DIRECTORY" ]]; then
  printf '  Copying template contents... '
  cp -R "$TEMPLATE_SOURCE_DIRECTORY"/* "$DESTINATION"
  echo "OK"
fi

if [[ -f "$TEMPLATE_POSTCREATE" ]]; then
  printf '  Running postcreate script... '

  namespace_safe_name="${PROJECT_NAME//-/_}"
  $sh "$TEMPLATE_POSTCREATE" "$PROJECT_NAME" "$(realpath "$DESTINATION")" "$namespace_safe_name"

  echo "OK"
fi

echo "Project created successfully at $(realpath "$DESTINATION")."
