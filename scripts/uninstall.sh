#!/bin/bash

# Set the installation directory
installdir="/usr/local/houston"

# Check if the Houston directory exists
if [ -d "$installdir" ]; then
  # Remove the Houston directory and all its contents
  sudo rm -rf "$installdir"

  # Remove symbolic links
  sudo rm -f "/usr/local/bin/hu"
  sudo rm -f "/usr/local/bin/houston"

  echo "Houston has been successfully removed from your system."
else
  echo "Could not find Houston installation to remove."
fi
