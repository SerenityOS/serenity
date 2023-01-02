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

# Use case-insensitive sorting, which will lead to more intuitive index pages.
SORT="sort -f"

# Prepare output directories
for d in "${MAN_DIR}"*/; do
    dir_name=$(basename "$d")
    section="${dir_name/man}"
    mkdir -p "output/${dir_name}"
done

# Convert markdown to html

# If you're here because your local results are different from the website:
# Check that your pandoc version matches the pandoc-version specified in manpages.yaml.

for md_file in $(find "${MAN_DIR}" -iname '*.md' | ${SORT}); do
    relative_path="$(realpath --relative-to="${MAN_DIR}" "${md_file}")"
    section="${relative_path%%/*}"
    section_number="${section#man}"
    filename="${relative_path#*/}"
    name="${filename%.md}"
    output_file="output/${section}/${name}.html"

    echo "Generating $md_file -> $output_file"
    mkdir -p "$(dirname "${output_file}")"
    pandoc -f gfm -t html5 -s \
        -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
        --lua-filter=Meta/convert-markdown-links.lua \
        --metadata title="${name}(${section_number}) - SerenityOS man pages" \
        -o "${output_file}" \
        "${md_file}"
done

# Generate man page listings through pandoc
for section_directory in output/*/; do
    section=$(basename "${section_directory}")
    section_number="${section#man}"
    case "${section_number}" in
        1) section_title="User Programs";;
        2) section_title="System Calls";;
        3) section_title="Library Functions";;
        4) section_title="Special Files";;
        5) section_title="File Formats";;
        6) section_title="Games";;
        7) section_title="Miscellanea";;
        8) section_title="Sysadmin Tools";;
        *) section_title="SerenityOS man pages"; echo "WARNING: Unrecognized section ${section_number}?!";;
    esac
    output="output/${section}/index.html"

    echo "Generating section ${section_number} index -> ${output}"
    pandoc -f gfm -t html5 -s \
        -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
        --metadata title="Section ${section_number} - ${section_title}" \
        -o "${output}" \
        <(
            for f in $(find "${section_directory}" -iname '*.html' | ${SORT}); do
                filename=$(realpath --relative-to="${section_directory}" "$f")
                name="${filename%.html}"
                if [[ "$filename" == "index.html" ]]; then
                    continue
                fi
                echo "- [${name}](${filename})"
            done
        )
done

# Generate main landing page listings through pandoc
echo 'Generating main pages'
pandoc -f gfm -t html5 -s \
    -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
    --metadata title="SerenityOS man pages" \
    -o output/index.html \
    Meta/Websites/man.serenityos.org/index.md
pandoc -f gfm -t html5 -s \
    -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
    --metadata title="Can't run applications" \
    -o output/cant-run-application.html \
    Meta/Websites/man.serenityos.org/cant-run-application.md

# Copy pre-made files
echo 'Copying images'
cp Meta/Websites/man.serenityos.org/banner.png output/
cp Base/usr/share/man/man7/LibDSP_classes.svg output/
cp Base/usr/share/man/man1/*.png output/

# Copy icons
mkdir output/icons

while read -r p; do
  rsync -a --relative Base/res/icons/./"$p" output/icons/
done < icons.txt

rm icons.txt
