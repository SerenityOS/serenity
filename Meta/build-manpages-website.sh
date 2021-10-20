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
    mkdir -p "output/${dir_name}"
done

# Convert markdown to html

# If you're here because your local results are different from the website:
# Check that your pandoc version matches the pandoc-version specified in manpages.yaml.

for md_file in "${MAN_DIR}"*/*.md; do
    relative_path="$(realpath --relative-to="${MAN_DIR}" "${md_file}")"
    section="${relative_path%%/*}"
    section_number="${section#man}"
    filename="${relative_path#*/}"
    name="${filename%.md}"
    pandoc -f gfm -t html5 -s \
        -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
        --lua-filter=Meta/convert-markdown-links.lua \
        --metadata title="${name}(${section_number}) - SerenityOS man pages" \
        -o "output/${section}/${name}.html" \
        "${md_file}"
done

# Generate man page listings through pandoc
for d in output/*/; do
    section=$(basename "$d")
    section_number="${section#man}"
    pandoc -f gfm -t html5 -s \
        -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
        --metadata title="Section ${section_number} - SerenityOS man pages" \
        -o "output/${section}/index.html" \
        <(
            for f in "$d"/*; do
                filename=$(basename "$f")
                name="${filename%.html}"
                if [[ "$filename" == "index.html" ]]; then
                    continue
                fi
                echo "- [${name}](${filename})"
            done
        )
done

# Generate man page listings through pandoc
for d in output/*/; do
    section=$(basename "$d")
    section_number="${section#man}"
    pandoc -f gfm -t html5 -s \
        -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
        --metadata title="Section ${section_number} - SerenityOS man pages" \
        -o "output/${section}/index.html" \
        <(
            for f in "$d"/*; do
                filename=$(basename "$f")
                name="${filename%.html}"
                if [[ "$filename" == "index.html" ]]; then
                    continue
                fi
                echo "- [${name}](${filename})"
            done
        )
done

# Generate main landing page listings through pandoc
pandoc -f gfm -t html5 -s \
    -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
    --metadata title="SerenityOS man pages" \
    -o output/index.html \
    Meta/Websites/man.serenityos.org/index.md

# Copy pre-made files
cp Meta/Websites/man.serenityos.org/banner.png output/
