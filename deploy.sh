#!/bin/bash
set -e
cp -vf ./build/dist/lib/tcs/auto_pilot/1.0.2/auto_pilot.jar ../tcs/lib/tcs/auto_pilot/1.0.2/
cd ../tcs/lib
export gitemail=$(curl -s https://api.github.com/users/$GITUSER/events/public | grep -m 1 -i email | cut -d : -f2 | cut -d , -f1 | sed -e 's/ //' )
git config user.name "$GITUSER"
git config user.email $gitemail
git config push.default matching
git remote set-url origin  https://$GITUSERDETATILS@github.com/topcoder-platform/tc-java-component-libs.git
git add --all
git commit -m "Autopilot library update" .
git push -u origin
echo "library updated without any failure"