#!/usr/bin/env bash

if [ "$1" != "--rec" ]; then
  ./gradlew installDist
fi

if [ "$(id -u)" -ne 0 ]; then # not root
  # check if --rec was passed
  if [ "$1" = "--rec" ]; then
    # fail
    echo "Houston requires root privileges to install."
    exit 1
  else
    # run this script again with --rec
    sudo "$0" --rec
    exit 0
  fi
fi

# guaranteed to be root from here on

installdir="/usr/local/houston"

mkdir -p "$installdir"

# Copy files to installation directory
cp -R build/install/Houston/* "$installdir"

# Create (or update) symbolic links
ln -sf "$installdir/bin/hu" "/usr/local/bin/hu"
ln -sf "$installdir/bin/hu" "/usr/local/bin/houston"

echo "Houston has been installed."

# check if /usr/local/bin is in the PATH using bash's builtin [[ ]] operator
if [[ ":$PATH:" != *":/usr/local/bin:"* ]]; then
  echo "WARNING: In order to use the hu command, you must add /usr/local/bin to your PATH."
fi

uninstall_script_location="/usr/local/bin/houston-uninstall"
cp scripts/uninstall.sh $uninstall_script_location
echo "sudo rm /usr/local/bin/houston-uninstall" >>$uninstall_script_location
