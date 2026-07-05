#!/bin/bash
set -e
docker build -t codeduel-runner-java ./executor/runners/java
docker build -t codeduel-runner-python ./executor/runners/python
docker build -t codeduel-runner-cpp ./executor/runners/cpp
echo "All runner images built successfully."
