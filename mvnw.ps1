$ErrorActionPreference = "Stop"
if (-not $env:JAVA_HOME) {
    if (Test-Path "C:\Program Files\Java\jdk-22") {
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
    }
}
$MavenHome = Join-Path $PSScriptRoot "maven\apache-maven-3.9.9"
$MvnCmd = Join-Path $MavenHome "bin\mvn.cmd"
& $MvnCmd $args
