#!/bin/bash

# Complete 16KB-aligned GDAL build script for Linux/WSL environment
# This script builds GDAL with proper 16KB page size alignment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== 16KB-aligned GDAL Build Script ===${NC}"
echo -e "${YELLOW}This will rebuild GDAL libraries with 16KB page size alignment${NC}"

# Check if we're in the right directory
if [ ! -f "build_cpp.sh" ]; then
    echo -e "${RED}Error: build_cpp.sh not found. Please run this from the gdal directory.${NC}"
    exit 1
fi

# Set environment variables for 16KB alignment
export CFLAGS="$CFLAGS -Wl,-z,max-page-size=16384"
export CXXFLAGS="$CXXFLAGS -Wl,-z,max-page-size=16384"
export LDFLAGS="$LDFLAGS -Wl,-z,max-page-size=16384"

echo -e "${GREEN}Environment configured with 16KB alignment flags:${NC}"
echo -e "  CFLAGS: $CFLAGS"
echo -e "  CXXFLAGS: $CXXFLAGS"
echo -e "  LDFLAGS: $LDFLAGS"

# Build for all architectures
echo -e "${GREEN}Building for all architectures...${NC}"

# Update build script to include 16KB flags
sed -i 's/build_for_target "x86_64-linux-android" "x86_64" 21/# Build with 16KB alignment\nbuild_for_target "x86_64-linux-android" "x86_64" 21\nbuild_for_target "aarch64-linux-android" "arm64-v8a" 21/' build_cpp.sh

echo -e "${GREEN}Starting GDAL build process...${NC}"

# Run the build
if ./build_cpp.sh "$1" "$2" "$3" "$4"; then
    echo -e "${GREEN}✅ GDAL build completed successfully!${NC}"
    
    # Verify 16KB alignment
    echo -e "${GREEN}Verifying 16KB alignment...${NC}"
    for abi in arm64-v8a x86_64; do
        if [ -d "src/main/jniLibs/$abi" ]; then
            echo -e "${YELLOW}Checking $abi libraries:${NC}"
            for sofile in src/main/jniLibs/$abi/*.so; do
                if [ -f "$sofile" ]; then
                    echo -e "  $(basename "$sofile")"
                    readelf -l "$sofile" | grep LOAD | head -2
                fi
            done
        fi
    done
    
    echo -e "${GREEN}=== Build Complete ===${NC}"
    echo -e "${YELLOW}Next steps:${NC}"
    echo -e "1. Copy the generated AAR to your app's libs folder"
    echo -e "2. Rename it to gdal-debug-16kb.aar"
    echo -e "3. Rebuild your app"
    
else
    echo -e "${RED}❌ GDAL build failed!${NC}"
    exit 1
fi
