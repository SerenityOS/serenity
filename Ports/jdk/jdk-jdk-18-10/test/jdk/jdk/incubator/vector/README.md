Introduction:

The goal of this project is to cover unit and performance testing of the Vector API.

Pre-Setup:
- Make sure java/javac point to the VectorAPI build you want to test (setup the PATH environment variable appropriately).

Setup:

1. Run bash gen-tests.sh from this directory. This will generate unit tests and microbenchmarks source files from the templates to each vector shape and type.

2. Run jtreg. Example:
jtreg -ea -esa -avm -va -nr *VectorTests.java
