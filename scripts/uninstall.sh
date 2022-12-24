#!/bin/bash

# Set the installation directory
installdir="/usr/local/houston"

# Check if the Houston directory exists
if [ -d "$installdir" ]; then
  # Remove the Houston directory and all its contents
  sudo rm -rf "$installdir"

  # Remove symbolic link
  sudo rm "/usr/local/bin/hu"

  echo "Houston has been uninstalled."
else
  echo "Houston was not installed."
fi
