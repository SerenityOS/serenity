#!/usr/bin/env bash

set -eo pipefail

SCRIPT_DIR="$(realpath "$(dirname "${BASH_SOURCE[0]}")")"

if [ -z "$SERENITY_SOURCE_DIR" ]
then
    SERENITY_SOURCE_DIR="$(realpath "${SCRIPT_DIR}/../../../")"
    export SERENITY_SOURCE_DIR
fi

if [[ "$1" == "--update-expectations-metadata" ]]; then
    update_expectations_metadata=true
else
    update_expectations_metadata=false
fi

# NOTE: WPT runner assumes Ladybird, WebContent and WebDriver are available in $PATH.
export PATH="${SERENITY_SOURCE_DIR}/Build/lagom/bin:${SERENITY_SOURCE_DIR}/Meta/Lagom/Build/bin:${PATH}"

pushd "${SCRIPT_DIR}"

if [ ! -d "${SCRIPT_DIR}/wpt" ]; then
    # Clone patched web-platform-tests repository
    git clone --depth 10000 https://github.com/web-platform-tests/wpt.git

    # Switch to the commit that was used to generate tests expectations. Requires periodic updates.
    (cd wpt; git checkout 4434e91bd0801dfefff044b5b9a9744e30d255d3)

    # Apply WPT patch with Ladybird runner
    (cd wpt; git apply ../ladybird_runner.patch)

    # Update hosts file if needed
    if [ "$(comm -13 <(sort -u /etc/hosts) <(python3 ./wpt/wpt make-hosts-file | sort -u) | wc -l)" -gt 0 ]; then
        echo "Enter superuser password to append wpt hosts to /etc/hosts"
        python3 "./wpt/wpt" make-hosts-file | sudo tee -a /etc/hosts
    fi
fi

# Extract metadata.txt into directory with expectation files expected by WPT runner
python3 ./concat-extract-metadata.py --extract metadata.txt metadata

# Generate name for file with wpt run log
wpt_run_log_filename="$(mktemp).txt"

# Run tests.
python3 ./wpt/wpt run ladybird --no-fail-on-unexpected --no-fail-on-unexpected-pass --skip-timeout --include-manifest include.ini --metadata ./metadata --manifest ./MANIFEST.json --log-raw "${wpt_run_log_filename}"

# Update expectations metadata files if requested
if [[ $update_expectations_metadata == true ]]; then
    python3 ./wpt/wpt update-expectations --product ladybird --metadata ./metadata --manifest ./MANIFEST.json "${wpt_run_log_filename}"
    python3 ./concat-extract-metadata.py --concat ./metadata > metadata.txt
fi

popd
