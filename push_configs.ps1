param (
    [string]$DevicePath = "/sdcard/ROMs/ROMs",
    [string]$Serial = "HA1Q3MLF"
)

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Check if adb exists
if (-not (Test-Path $adb)) {
    Write-Error "adb.exe not found at $adb. Please ensure Android SDK is installed."
    exit 1
}

# Function to get connected devices
function Get-Devices {
    $deviceList = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" }
    return $deviceList.ForEach({ $_.Split("`t")[0] })
}

$connectedDevices = Get-Devices

if ($connectedDevices.Count -eq 0) {
    Write-Error "No devices connected. Please connect your tablet."
    exit 1
}

$targetSerial = ""
if ($Serial -ne "") {
    if ($connectedDevices -contains $Serial) {
        $targetSerial = $Serial
    } else {
        Write-Error "Device with serial '$Serial' not found."
        Write-Host "Connected devices: $($connectedDevices -join ', ')"
        exit 1
    }
} elseif ($connectedDevices.Count -gt 1) {
    Write-Warning "Multiple devices detected: $($connectedDevices -join ', ')"
    Write-Host "Please run the script again with -Serial <device_id>"
    Write-Host "Example: .\push_configs.ps1 -Serial $($connectedDevices[0])"
    exit 1
} else {
    $targetSerial = $connectedDevices[0]
}

Write-Host "Targeting device: $targetSerial"

$adbCmd = { & $adb -s $targetSerial $args }

# Base source directory
$SourceBase = Join-Path $PSScriptRoot "app/src/main/assets/ROMs"

if (-not (Test-Path $SourceBase)) {
    Write-Error "Source path $SourceBase not found."
    exit 1
}

$files = Get-ChildItem -Path $SourceBase -Filter "romulist.xml" -Recurse

foreach ($file in $files) {
    $RelativePath = $file.FullName.Substring($SourceBase.Length + 1)
    $DestPath = "$DevicePath/$RelativePath".Replace('\', '/')
    $DestDir = ($DestPath.Substring(0, $DestPath.LastIndexOf('/')))

    Write-Host "Pushing: $RelativePath"
    Write-Host "Target:  $DestPath"

    # Create directory and push file
    & $adb -s $targetSerial shell mkdir -p "'$DestDir'"
    & $adb -s $targetSerial push "$($file.FullName)" "$DestPath"
}

Write-Host "Done!"
