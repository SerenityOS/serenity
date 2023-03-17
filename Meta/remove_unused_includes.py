#!/usr/bin/python3

import xml.etree.ElementTree as ET
from html import unescape
import subprocess
import sys

# Removes unused includes from source files.
# WARNING: Don't run if you have uncommitted changes in git!
# Requires some manual steps (see below) and so is not
# suitable for automated CI/CD.
#
# The script takes the output of CLions analysis of unused includes,
# and removes lines of unused includes in batches. For every batch it
# tries to run the build. If the build passes, a commit is made to git.
# If the build fails, it resets the batch, and continues. This means it
# might miss some lines that should have been removed.
#
# First argument is batch size. 100 works fine to start, and you use 1 later
# to ensure you catch everything.
# The second argument is a filename of an OCUnusedIncludeDirective.xml file
# produced by CLion using the following command from the menu:
# Code->Analyze Code->Run Inspection By Name...->Unused Include directive


solved_total = 0
failed_total = 0
solved_since_commit = 0


def build_and_commit():
    global solved_since_commit, solved_total, failed_total
    new_solved_total = solved_total + solved_since_commit
    print("### Solved %s new problems, %s in total, %s failed. Trying to build"
          % (solved_since_commit, new_solved_total, failed_total))
    if subprocess.run(["Meta/serenity.sh", "build"]).returncode == 0:
        print("### Build succeeded, committing")
        message = "WIP: Solving %s problems" % solved_since_commit
        subprocess.run(["git", "commit", "--no-verify", "-a", "-m", message]).check_returncode()
        solved_total = new_solved_total
    else:
        print("### Build failed, resetting")
        subprocess.run(["git", "reset", "HEAD", "--hard"]).check_returncode()
        failed_total += solved_since_commit
    solved_since_commit = 0


batch_size = int(sys.argv[2])
for problem in ET.parse(sys.argv[1]).getroot().iter("problem"):
    problem_class = problem.find("problem_class").attrib.get("attribute_key")
    if problem_class != "NOT_USED_ELEMENT_ATTRIBUTES":
        continue
    filename_full = problem.find("file").text
    filename = filename_full[len("file://$PROJECT_DIR$/"):]
    description = problem.find("description").text
    include = unescape(description[len("Unused \""):-1])
    subprocess.run(["sed", "-i", "/" + include.replace("/", r"\/") + "/d", filename]).check_returncode()
    if subprocess.run(["git", "diff", "--exit-code"]).returncode == 0:
        continue
    solved_since_commit += 1
    if solved_since_commit == batch_size:
        build_and_commit()
if solved_since_commit > 0:
    build_and_commit()
