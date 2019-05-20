#!/bin/bash

if [ $# -ne 4 ]
  then
    echo "pass 4 screenshots"
    exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

echo "Cropping.."
"$DIR/crop_circle.sh" "$@"

SCREENSHOT="$1"
OUTPUT_FILE="out-arm.png"
FRAMEFILE="$DIR/arm.jpg"
FRAMEFILE_SCREEN_GEOMETRY="200x200"
FRAMEFILE_TOP_LEFT_OFFSET="+315+150"
convert "${FRAMEFILE}" \( "${SCREENSHOT}" -scale ${FRAMEFILE_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE_TOP_LEFT_OFFSET} -compose SrcOver -composite "${OUTPUT_FILE}"

SCREENSHOT1="$2"
SCREENSHOT2="$3"
SCREENSHOT3="$4"

OUTPUT_FILE="out-3.png"
FRAMEFILE="$DIR/3-faces.jpg"
FRAMEFILE1_SCREEN_GEOMETRY="270x270"
FRAMEFILE2_SCREEN_GEOMETRY="335x335"
FRAMEFILE3_SCREEN_GEOMETRY="280x280"
FRAMEFILE1_TOP_LEFT_OFFSET="+50+260"
FRAMEFILE2_TOP_LEFT_OFFSET="+423+245"
FRAMEFILE3_TOP_LEFT_OFFSET="+870+255"
convert "${FRAMEFILE}" \
    \( "${SCREENSHOT1}" -scale ${FRAMEFILE1_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE1_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    \( "${SCREENSHOT2}" -scale ${FRAMEFILE2_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE2_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    \( "${SCREENSHOT3}" -scale ${FRAMEFILE3_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE3_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    "${OUTPUT_FILE}"

echo "done"