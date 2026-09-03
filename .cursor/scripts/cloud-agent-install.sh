#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
mvn -B -q dependency:go-offline test-compile
