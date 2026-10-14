$json = Get-Content -Raw -Path "dictionary/remote-dictionary-v1.json" -Encoding utf8 | ConvertFrom-Json
$remoteWords = @()
if ($json.answers) { $remoteWords += $json.answers }
if ($json.guesses) { $remoteWords += $json.guesses }
$remoteWords = $remoteWords | Where-Object { $_ -and $_.Trim().Length -in 3..8 } | Select-Object -Unique

$formattedRemoteWords = $remoteWords | ForEach-Object { "`"$_`"" }
$chunkSize = 200
$lines = @()
for ($i=0; $i -lt $formattedRemoteWords.Count; $i+=$chunkSize) {
    $slice = $formattedRemoteWords[$i..([Math]::Min($i+$chunkSize-1, $formattedRemoteWords.Count-1))]
    $lines += "        " + ($slice -join ", ")
}
$remoteGuessesKotlin = "`n    private val remoteGuesses = listOf(`n" + ($lines -join ",`n") + "`n    )`"

$dictPath = "app/src/main/java/com/adamos/kalima/data/Dictionary.kt"
$dictContent = Get-Content -Raw -Path $dictPath -Encoding utf8

$target = "private val guessableWords: Set<String> ="
$replacement = "$remoteGuessesKotlin`n`n    $target"

if ($dictContent -notmatch "private val remoteGuesses") {
    $dictContent = $dictContent -replace [System.Text.RegularExpressions.Regex]::Escape($target), $replacement
    $dictContent = $dictContent -replace "extraGuesses.map \{ normalize\(it\.trim\(\)\) \}", "extraGuesses.map { normalize(it.trim()) } + remoteGuesses.map { normalize(it.trim()) }"
    Set-Content -Path $dictPath -Value $dictContent -Encoding utf8
    Write-Host "Integrated remoteGuesses successfully!"
} else {
    Write-Host "Already integrated."
}
