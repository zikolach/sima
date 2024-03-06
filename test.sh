#!/usr/bin/env bash

curl -O "http://localhost:8080/crop?width=100&height=100&url=https://raw.githubusercontent.com/h2non/imaginary/master/testdata/test.webp"
mv crop test.webp
open test.webp