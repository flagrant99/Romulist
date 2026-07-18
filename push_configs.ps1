param (
    [string]$DevicePath = "",
    [string]$Serial = "",
    [ValidateSet("Internal", "External", "Auto")]
    [string]$Storage = "Auto"
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

$connectedDevices = @(Get-Devices)

if ($connectedDevices.Count -eq 0) {
    Write-Error "No devices connected. Please connect your device and enable USB debugging."
    exit 1
}

# Determine target serial
$targetSerial = ""
if ($Serial -ne "") {
    if ($connectedDevices -contains $Serial) {
        $targetSerial = $Serial
    } else {
        Write-Error "Device with serial '$Serial' not found."
        Write-Host "Connected devices: $($connectedDevices -join ', ')"
        exit 1
    }
} else {
    $targetSerial = $connectedDevices[0]
    if ($connectedDevices.Count -gt 1) {
        Write-Warning "Multiple devices detected. Using the first one: $targetSerial"
    }
}

# Get device model name
$deviceModel = & $adb -s $targetSerial shell getprop ro.product.model
if ($null -ne $deviceModel) {
    $deviceModel = $deviceModel.Trim()
} else {
    $deviceModel = "Unknown"
}

Write-Host "Targeting device: $deviceModel ($targetSerial)"

# Auto-detect or use specified Storage
if ($DevicePath -eq "") {
    Write-Host "Resolving storage path ($Storage)..."
    $storageItems = & $adb -s $targetSerial shell ls /storage
    # Look for SD Card pattern (e.g., 3636-3939 or AC50-F7EF)
    $sdCard = $storageItems | ForEach-Object { $_.Trim() } | Where-Object { $_ -match "^[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$" } | Select-Object -First 1

    if ($Storage -eq "External") {
        if ($sdCard) {
            $DevicePath = "/storage/$sdCard/ROMs"
            Write-Host "Using SD Card: $DevicePath"
        } else {
            Write-Error "External storage requested but no SD Card was detected in /storage."
            exit 1
        }
    } elseif ($Storage -eq "Internal") {
        $DevicePath = "/storage/emulated/0/ROMs"
        Write-Host "Using Internal Storage: $DevicePath"
    } else {
        # Auto mode
        if ($sdCard) {
            $DevicePath = "/storage/$sdCard/ROMs"
            Write-Host "Detected SD Card: $DevicePath"
        } else {
            $DevicePath = "/storage/emulated/0/ROMs"
            Write-Host "No SD Card detected, defaulting to internal storage: $DevicePath"
        }
    }
} else {
    Write-Host "Using specified DevicePath: $DevicePath"
}

# Base source directory
#$SourceBase = Join-Path $PSScriptRoot "configs/ES-DE/Internal/ROMs"
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

    # Check if target directory exists on device
    $dirExists = & $adb -s $targetSerial shell "if [ -d '$DestDir' ]; then echo 'YES'; fi"

    if ($dirExists -match "YES") {
        Write-Host "Pushing: $RelativePath"
        Write-Host "Target:  $DestPath"
        & $adb -s $targetSerial push "$($file.FullName)" "$DestPath"
    } else {
        Write-Host "Skipping: $RelativePath (Target directory '$DestDir' not found)"
    }
}

Write-Host "Triggering Media Scan to update MTP/Windows Explorer..."
& $adb -s $targetSerial shell cmd media scan-file "$DevicePath"

Write-Host "Done!"
