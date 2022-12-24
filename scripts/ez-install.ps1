# Clone the Houston repository

$repoUrl = "https://github.com/npgrosser/Houston.git"
$repoDir = [System.IO.Path]::GetTempPath() + "\houston.git.ez-install"
$repoBranch = "master"

function GitClone
{
    git clone $repoUrl $repoDir -b $repoBranch
}

Push-Location

try
{
    if (Test-Path $repoDir)
    {
        Set-Location $repoDir
        git pull origin $repoBranch

        if ($LastExitCode -ne 0)
        {
            Write-Warning "Houston repository pull failed.  Removing and re-cloning."

            Set-Location ..
            Remove-Item $repoDir -Recurse -Force -ErrorAction Stop
            GitClone
            Set-Location $repoDir
        }
    }
    else
    {
        GitClone
        Set-Location $repoDir
    }

    if ($LastExitCode -ne 0)
    {
        Write-Error "Failed to clone repository"
        exit 1
    }

    $scriptPath = Join-Path -Path (Get-Location) -ChildPath "scripts/install.ps1"

    $output = Invoke-Expression $scriptPath | Out-String

    # Check the exit code of the build-ez-install.ps1 script
    if ($LASTEXITCODE -ne 0)
    {
        # Print the output of the script if it failed
        Write-Output $output

        # Print a message if the script failed
        Write-Output "Installation failed."
    }
    else
    {
        # Print the output of the script if it succeeded
        Write-Output $output

        # Print a message if the script succeeded
        Write-Output "Installation succeeded."
    }
}
catch
{
    throw
}
finally
{
    Pop-Location
}

