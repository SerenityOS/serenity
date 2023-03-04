### LibJSGCVerifier

This is a simple Clang tool to validate certain behavior relating to LibJS's GC. It currently validates
two things:

- For all types wrapped by `GCPtr` or `NonnullGCPtr`, that the wrapped type inherits from `Cell`
- For all types not wrapped by `GCPtr` or `NonnullGCPtr`, that the wrapped type does not inherit from `Cell`
  (otherwise it should be wrapped).

Usage:
```
cmake -GNinja -B build
cmake --build build
src/main.py -b <path to serenity>/Build
```
