[CmdletBinding()]
param(
    [string]$SourcePath = "",
    [string]$OutputPath = "app/src/main/assets/arabic-guess-words.txt",
    [int]$MaxWords = 150000
)
$ErrorActionPreference = 'Stop'

if ($MaxWords -lt 1) { throw "MaxWords must be greater than zero." }

$repoRoot = (Get-Location).Path
if (-not $SourcePath) {
    $SourcePath = Join-Path $env:TEMP 'kalima-arabic-words.txt'
}
if (-not [System.IO.Path]::IsPathRooted($SourcePath)) {
    $SourcePath = Join-Path $repoRoot $SourcePath
}
if (-not [System.IO.Path]::IsPathRooted($OutputPath)) {
    $OutputPath = Join-Path $repoRoot $OutputPath
}

$sourceUrl = 'https://raw.githubusercontent.com/MustafaLinux/arabic-words-list/main/arabic-words.txt'
if (-not (Test-Path -LiteralPath $SourcePath -PathType Leaf)) {
    Write-Host "Downloading Arabic word list (~47 MB)..." -ForegroundColor Cyan
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $sourceUrl -OutFile $SourcePath -UseBasicParsing
}

if (-not (Test-Path -LiteralPath $SourcePath -PathType Leaf)) {
    throw "Source file not found: $SourcePath"
}

function Normalize-ArabicWord([string]$Word) {
    $s = $Word.Normalize([Text.NormalizationForm]::FormC)
    $s = $s -replace '[\u064B-\u065F\u0670\u06D6-\u06ED\u0640]', ''
    $s = $s.Replace('أ','ا').Replace('إ','ا').Replace('آ','ا').Replace('ٱ','ا').Replace('ة','ه')
    return $s
}

# Read line-by-line. Keep only Arabic-only words of 3–8 normalized letters.
$seen = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$candidates = [System.Collections.Generic.List[string]]::new()
$reader = [System.IO.StreamReader]::new($SourcePath, [System.Text.Encoding]::UTF8, $true)
try {
    $lineNo = 0
    while (($line = $reader.ReadLine()) -ne $null) {
        $lineNo++
        $word = $line.Trim()
        if ($word.Length -lt 3 -or $word.Length -gt 8) { continue }
        if ($word -notmatch '^[\u0621-\u064A\u0671\u0672\u0673\u0675\u0676\u0677\u0678\u0679\u067A\u067B\u067C\u067D\u067E\u067F\u0680\u0681\u0682\u0683\u0684\u0685\u0686\u0687\u0688\u0689\u068A\u068B\u068C\u068D\u068E\u068F\u0690\u0691\u0692\u0693\u0694\u0695\u0696\u0697\u0698\u0699\u06A9\u06AF\u06BA\u06BB\u06BC\u06BD\u06BE\u06C0\u06C1\u06C2\u06C3\u06C4\u06C5\u06C6\u06C7\u06C8\u06C9\u06CA\u06CB\u06CC\u06CD\u06CE\u06CF\u06D0\u06D1\u06D2\u06D3]+$') { continue }
        $normalized = Normalize-ArabicWord $word
        if ($normalized.Length -lt 3 -or $normalized.Length -gt 8) { continue }
        if ($seen.Add($normalized)) { $candidates.Add($normalized) }
        if (($lineNo % 250000) -eq 0) { Write-Progress -Activity 'Filtering Arabic words' -Status "$lineNo source lines scanned; $($candidates.Count) unique words" }
    }
}
finally { $reader.Dispose() }
Write-Progress -Activity 'Filtering Arabic words' -Completed

# Keep every 3- and 4-letter candidate. Only longer words are sampled when needed.
# This prevents the alphabetical stride from randomly dropping core short guesses.
$candidates.Sort([StringComparer]::Ordinal)
$shortWords = [System.Collections.Generic.List[string]]::new()
$longWords = [System.Collections.Generic.List[string]]::new()
foreach ($word in $candidates) {
    if ($word.Length -le 4) {
        $shortWords.Add($word)
    } else {
        $longWords.Add($word)
    }
}

$selected = [System.Collections.Generic.SortedSet[string]]::new([StringComparer]::Ordinal)
foreach ($word in $shortWords) { [void]$selected.Add($word) }

$remaining = [Math]::Max(0, $MaxWords - $selected.Count)
if ($remaining -gt 0) {
    if ($longWords.Count -le $remaining) {
        foreach ($word in $longWords) { [void]$selected.Add($word) }
    } else {
        $step = [double]$longWords.Count / [double]$remaining
        for ($i = 0; $i -lt $remaining; $i++) {
            [void]$selected.Add($longWords[[int][Math]::Floor($i * $step)])
        }
    }
}

# Guaranteed regression words; these are guesses only, never secret answers.
foreach ($word in @('جحش','سلب','رقى','رقي')) {
    [void]$selected.Add((Normalize-ArabicWord $word))
}

$outDir = Split-Path -Parent $OutputPath
if (-not (Test-Path -LiteralPath $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllLines($OutputPath, [string[]]@($selected), $utf8NoBom)

$check = [System.IO.File]::ReadAllLines($OutputPath, [System.Text.Encoding]::UTF8)
$required = @('جحش','سلب','رقى','رقي')
$missing = @($required | Where-Object { $_ -notin $check })
if ($missing.Count -gt 0) { throw "Regression words missing: $($missing -join ', ')" }

Write-Host "SUCCESS: created offline guess dictionary" -ForegroundColor Green
Write-Host "Source words accepted before selection: $($candidates.Count)"
Write-Host "Short words (3–4 letters) retained: $($shortWords.Count)"
Write-Host "Output words: $($check.Count)"
Write-Host "Verified: جحش, سلب, رقى, رقي"
Write-Host "Output: $OutputPath"
Write-Host "Note: This broad word list may contain errors; it is not a linguistic validator."
