#!/bin/bash

if [ $# -eq 0 ]
  then
    echo "No name supplied"
    exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

adb exec-out screencap -p > "$DIR/$1.png"

"$DIR/crop_circle.sh" "$DIR/$1.png"

echo "saved $DIR/$1.png"