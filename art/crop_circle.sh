#!/bin/bash

if [ $# -eq 0 ]
  then
    echo "No files supplied"
    exit 1
fi

for var in "$@"
do
    echo "converting $var"
    #cp "$var" "$var.bak"
    magick "$var" \( +clone -threshold 101% -fill white -draw 'circle %[fx:int(w/2)],%[fx:int(h/2)] %[fx:int(w/2)],%[fx:int(h)]' \) -channel-fx '| gray=>alpha' "$var"
done

echo "done"