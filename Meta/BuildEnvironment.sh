#!/bin/sh

docker build BuildEnvironment -t serenity-buildenv
docker run --rm -it -v ${PWD}:/root/env serenity-buildenv