#!/bin/bash

PORT=8080

# Check if the port is already in use
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
    echo "Port $PORT is in use. Closing the existing process..."
    fuser -k $PORT/tcp
fi

# Run gradle clean run
echo "Running gradle clean run..."
gradle run