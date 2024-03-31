#!/usr/bin/env bash

rm images/test.webp
curl "http://localhost:8080/crop?width=300&height=300&url=https://raw.githubusercontent.com/h2non/imaginary/master/testdata/test.webp" --output images/test.webp
open images/test.webp