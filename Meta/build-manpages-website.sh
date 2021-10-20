#!/bin/bash
# shellcheck disable=SC1004 # literal backslash+linefeed is intended

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.."

export LC_ALL=C  # Make the directory order reproducible
export MAN_DIR=Base/usr/share/man/

if [[ -e output ]]; then
    echo "Directory 'output/' already exists. Delete it first."
    exit 1
fi

# Prepare output directories
for d in "${MAN_DIR}"*/; do
    dir_name=$(basename "$d")
    section="${dir_name/man}"
    mkdir -p "output/${section}"
done

# Convert markdown to html

# If you're here because your local results are different from the website:
# Check that your pandoc version matches the pandoc-version specified in manpages.yaml.

find "${MAN_DIR}" -iname '*.md' -type f -exec sh -c '\
    relative_path="$(realpath --relative-to="${MAN_DIR}" $0)" \
    && stripped_path="${relative_path#man}" \
    && section="${stripped_path%%/*}" \
    && filename="${stripped_path#*/}" \
    && name="${filename%.md}" \
    && pandoc -f gfm -t html5 -s --lua-filter=Meta/convert-markdown-links.lua --metadata title="${name}(${section}) - SerenityOS man pages" -o "output/${section}/${name}.html" "${0}" \
' {} \;

# Generate man page listings
for d in output/*/; do
    section=$(basename "$d")
    echo "<!DOCTYPE html><html><head><title>Section ${section} - SerenityOS man pages</title></head><body>" > "${d}/index.html"
    for f in "$d"/*; do
        filename=$(basename "$f")
        name="${filename%.html}"
        if [[ "$filename" == "index.html" ]]; then
            continue
        fi
        echo "<a href=\"${filename}\"><p>${name}(${section})</p></a>" >> "${d}/index.html"
    done
    echo "</body></html>" >> "$d/index.html"
done

# Copy pre-made files
cp -R Meta/Websites/man.serenityos.org/* output/
