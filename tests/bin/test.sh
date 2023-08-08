#!/bin/bash
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
HANDBRAKE_WATCHER_JAR="$SCRIPT_DIR/../../dist/handbrake-watcher.jar"
SAMPLE_VIDEO_LIBRARY_PATH="$SCRIPT_DIR/../sample_video_library"
cd "$SAMPLE_VIDEO_LIBRARY_PATH"
export PATH="$SCRIPT_DIR":$PATH
java -jar "$HANDBRAKE_WATCHER_JAR"