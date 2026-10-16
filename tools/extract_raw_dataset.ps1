# Extract raw UTF-8 binary content directly from git branch fix/commit-expanded-arabic-guess-dictionary

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "git"
$psi.Arguments = "show fix/commit-expanded-arabic-guess-dictionary:app/src/main/assets/arabic-guess-words.txt"
$psi.RedirectStandardOutput = $true
$psi.UseShellExecute = $false
$psi.StandardOutputEncoding = [System.Text.Encoding]::UTF8

$p = [System.Diagnostics.Process]::Start($psi)
$content = $p.StandardOutput.ReadToEnd()
$p.WaitForExit()

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText("tools/raw_300k_arabic_words.txt", $content, $utf8NoBom)

Write-Host "Extracted tools/raw_300k_arabic_words.txt successfully."
