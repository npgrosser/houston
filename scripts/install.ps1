& ".\gradlew.bat" installDist

$installDir = "$env:APPDATA\Houston"

New-Item -ItemType Directory -Path $installDir -ErrorAction Ignore | Out-Null

Copy-Item -Recurse -Force -Destination $installDir ".\build\install\Houston\*"

# create copy of bin/hu to bin/houston
Copy-Item -Path "$installDir\bin\hu" -Destination "$installDir\bin\houston"

Write-Output "Houston has been installed"


# check if $installDir\bin is in PATH, if not print warning (not modifiing path here, just warning)
$envPath = $env:PATH
$envPath = $envPath.ToLower()
$installDir = $installDir.ToLower()
if ($envPath -notlike "*$installDir\bin*")
{
    Write-Output ""
    Write-Output "Make sure to add $installDir\bin to your PATH environment variable."
}

