#!/bin/bash

repoUrl="https://github.com/npgrosser/Houston.git"
repoDir="$TMPDIR/houston.git.ez-install"
repoBranch="master"

originalDir=$(pwd)

trap 'cd "$originalDir" || exit 1' EXIT

if [ -d "$repoDir" ]; then # update repo

  cd "$repoDir" || exit 1
  if ! git pull origin "$repoBranch"; then # if it fails, delete it and try to clone it again
    echo "Failed to reuse cached repository. Cloning again instead."
    cd ..
    rm -rf "$repoDir"
    git clone "$repoUrl" "$repoDir"
    cd "$repoDir" || exit 1
  fi

else # Clone repo
  git clone "$repoUrl" "$repoDir" -b "$repoBranch"
  cd "$repoDir" || exit 1
fi

sh scripts/install.sh

# shellcheck disable=SC2181
if [ $? -ne 0 ]; then
  echo "Installation failed."
fi
