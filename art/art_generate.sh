#!/bin/bash

# if [ $# -ne 4 ]
#   then
#     echo "pass 4 screenshots"
#     exit 1
# fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# echo "Cropping.."
# "$DIR/crop_circle.sh" "$@"

SCREENSHOT1="$DIR/screenshots/orange.png"
OUTPUT_FILE="$DIR/out/out-arm.png"
FRAMEFILE="$DIR/stock_input/arm.jpg"
FRAMEFILE1_SCREEN_GEOMETRY="202x202"
FRAMEFILE1_TOP_LEFT_OFFSET="+314+147"
convert "${FRAMEFILE}" \
    \( "${SCREENSHOT1}" -scale ${FRAMEFILE1_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE1_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    "${OUTPUT_FILE}"
echo "Created $OUTPUT_FILE"

SCREENSHOT1="$DIR/screenshots/teal.png"
SCREENSHOT2="$DIR/screenshots/green.png"
SCREENSHOT3="$DIR/screenshots/blue.png"
OUTPUT_FILE="$DIR/out/out-3.png"
FRAMEFILE="$DIR/stock_input/3-faces.jpg"
FRAMEFILE1_SCREEN_GEOMETRY="194x194"
FRAMEFILE2_SCREEN_GEOMETRY="246x246"
FRAMEFILE3_SCREEN_GEOMETRY="225x225"
FRAMEFILE1_TOP_LEFT_OFFSET="+111+131"
FRAMEFILE2_TOP_LEFT_OFFSET="+383+120"
FRAMEFILE3_TOP_LEFT_OFFSET="+700+112"
convert "${FRAMEFILE}" \
    \( "${SCREENSHOT1}" -scale ${FRAMEFILE1_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE1_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    \( "${SCREENSHOT2}" -scale ${FRAMEFILE2_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE2_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    \( "${SCREENSHOT3}" -scale ${FRAMEFILE3_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE3_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    "${OUTPUT_FILE}"
echo "Created $OUTPUT_FILE"

OUTPUT_FILE="$DIR/out/out-double.png"
FRAMEFILE="$DIR/stock_input/double.jpg"
FRAMEFILE1_SCREEN_GEOMETRY="302x302"
FRAMEFILE2_SCREEN_GEOMETRY="257x257"
FRAMEFILE1_TOP_LEFT_OFFSET="+64+348"
FRAMEFILE2_TOP_LEFT_OFFSET="+418+152"
convert "${FRAMEFILE}" \
    \( "${SCREENSHOT1}" -scale ${FRAMEFILE1_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE1_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    \( "${SCREENSHOT2}" -scale ${FRAMEFILE2_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE2_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    "${OUTPUT_FILE}"
echo "Created $OUTPUT_FILE"

SCREENSHOT1="$DIR/screenshots/ambient.png"
OUTPUT_FILE="$DIR/out/out-single.png"
FRAMEFILE="$DIR/stock_input/single.jpg"
FRAMEFILE1_SCREEN_GEOMETRY="248x248"
FRAMEFILE1_TOP_LEFT_OFFSET="+184+184"
convert "${FRAMEFILE}" \
    \( "${SCREENSHOT1}" -scale ${FRAMEFILE1_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE1_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    "${OUTPUT_FILE}"
echo "Created $OUTPUT_FILE"

SCREENSHOT1="$DIR/screenshots/blue.png"
OUTPUT_FILE="$DIR/out/out-fossil.png"
FRAMEFILE="$DIR/stock_input/fossil-bands.jpg"
FRAMEFILE1_SCREEN_GEOMETRY="236x236"
FRAMEFILE1_TOP_LEFT_OFFSET="+269+273"
convert "${FRAMEFILE}" \
    \( "${SCREENSHOT1}" -scale ${FRAMEFILE1_SCREEN_GEOMETRY} \) -geometry ${FRAMEFILE1_TOP_LEFT_OFFSET} -compose SrcOver -composite \
    "${OUTPUT_FILE}"
echo "Created $OUTPUT_FILE"

echo "done"