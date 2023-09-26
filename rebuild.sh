#!/bin/bash

#  If you delete an image, Docker removes it from its storage, but if layers from that image are 
#  shared by other images, those layers are not deleted. When building an image, Docker will reuse 
#  layers from cache if possible, which is why sometimes builds are faster if they've been built 
#  previously.

IMAGE_NAME="slot-checker:latest"

echo "==== Starting Cleanup and Build Process for Slot Checker ===="

# Stop and remove the services defined in the docker-compose.yml of the current directory
echo "Stopping existing slot-checker services..."
docker-compose down

# Build the image
echo "Building the slot-checker image..."
docker-compose build

# Spin up the services
echo "Starting the slot-checker services..."
docker-compose up -d

# Cleanup
echo "Cleaning up old resources..."

# Remove any stopped containers of slot-checker
CONTAINERS_TO_REMOVE=$(docker ps -a | grep "$IMAGE_NAME" | awk '{print $1}')
if [ -z "$CONTAINERS_TO_REMOVE" ]; then
    echo "No old slot-checker containers to remove."
else
    echo "Removing old slot-checker containers..."
    echo $CONTAINERS_TO_REMOVE | xargs docker rm
fi

# Remove dangling images related to slot-checker
IMAGES_TO_REMOVE=$(docker images -f "dangling=true" | grep "$IMAGE_NAME" | awk '{print $3}')
if [ -z "$IMAGES_TO_REMOVE" ]; then
    echo "No old slot-checker images to remove."
else
    echo "Removing old slot-checker images..."
    echo $IMAGES_TO_REMOVE | xargs docker rmi
fi

echo "==== Cleanup and Build Process Completed ===="
