$DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition
# $DIR = $PSScriptRoot

Set-Location $DIR
Set-Location ..

if ($args.Count -gt 0)
{
    switch ($args[0])
    {
        { "dev,development" -contains $_ } {
            $profile = "development"
        }

        { "prod,production" -contains $_ } {
            $profile = "production"
        }

        default {
            $profile = "test"
        }
    }
}
else
{
    $profile = "test"
}

$profiles = @(
"make-dist-exec",
"$profile"
) -join ","

mvn -e clean compile package `
    -P "$profiles" `
    -DskipTests=true
