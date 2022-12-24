if (Test-Path "$env:APPDATA\Houston")
{
    Remove-Item -Recurse -Force "$env:APPDATA\Houston"
    Write-Output "Houston has been uninstalled."
}
else
{
    Write-Output "No Houston installation found."
}