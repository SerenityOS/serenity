#!/usr/bin/env bash

set -eo pipefail

if [ -z "$SERENITY_SOURCE_DIR" ]
then
    echo "SERENITY_SOURCE_DIR is not set. Exiting."
fi

# NOTE: WPT runner assumes Ladybird, WebContent and WebDriver are available in $PATH.
export PATH="${SERENITY_SOURCE_DIR}/Build/lagom/bin:${SERENITY_SOURCE_DIR}/Meta/Lagom/Build/bin:${PATH}"

# Install dependencies.
sudo apt-get install -y git python3 python3-pip python3-venv libssl-dev

# Ensure a `python` binary exists
sudo apt-get install -y python-is-python3

# Clone patched web-platform-tests repository
git clone --depth 10000 https://github.com/web-platform-tests/wpt.git

# Switch to the commit that was used to generate tests expectations. Requires periodic updates.
(cd wpt; git checkout 4c27189ed2db4ddad8e727d4ea9ae8329c3e1672)

# Apply WPT patch with Ladybird runner
(cd wpt; git apply ../ladybird_runner.patch)

# Update hosts file
./wpt/wpt make-hosts-file | sudo tee -a /etc/hosts

# Extract metadata.txt into directory with expectation files expected by WPT runner
./concat-extract-metadata.py --extract metadata.txt metadata

# Run tests.
./wpt/wpt run ladybird --no-fail-on-unexpected --no-fail-on-unexpected-pass --skip-timeout --include-manifest include.ini --metadata ./metadata --manifest ./MANIFEST.json --log-raw "${wpt_run_log_filename}"
