# Konfiguracja
$outputFile = "struktura_i_zawartosc.txt"
$excludeFiles = @($outputFile, $MyInvocation.MyCommand.Name)

# Funkcja sprawdzaj¹ca czy plik jest tekstowy (na podstawie rozszerzenia)
function Test-IsTextFile {
    param([string]$Path)
    
    $textExtensions = @(
        '.txt', '.csv', '.ini', '.log','.json', '.js', '.css', '.html', '.htm',
         '.bat', '.java', '.py', '.c', '.cpp', '.h', '.cs', '.md',
        '.yml', '.yaml', '.properties','.php', '.asp', '.aspx', '.jsp', '.sh', '.fxml'
        #,'.sql','.ps1', '.xml'
    )
    
    $extension = [System.IO.Path]::GetExtension($Path).ToLower()
    return $textExtensions -contains $extension
}

# G³ówna funkcja eksportuj¹ca
function Export-Structure {
    param(
        [string]$startPath = ".",
        [string]$output = "output.txt"
    )
    
    # Utwórz plik wyjœciowy (lub wyczyœæ jeœli istnieje)
    Set-Content -Path $output -Value "Eksport struktury katalogów i plików`nWygenerowano: $(Get-Date)`n" -Force
    
    # Uzyskaj pe³n¹ œcie¿kê startow¹
    $absoluteStart = (Get-Item $startPath).FullName
    
    # Przeszukaj wszystkie pliki
    Get-ChildItem -Path $startPath -Recurse -File | ForEach-Object {
        $absolutePath = $_.FullName

        # Pomijanie plików wykluczonych po nazwie
        if ($excludeFiles -contains $_.Name) { return }
        
        # Pomijanie pliku wyjœciowego po pe³nej œcie¿ce
        if ($absolutePath -eq (Join-Path $pwd $output)) { return }

        # Pomijaj pliki binarne
        if (-not (Test-IsTextFile -Path $absolutePath)) { return }
        
        $relativePath = $absolutePath.Substring($absoluteStart.Length).TrimStart('\')
        $separator = "-" * ($relativePath.Length + 12)
        
        Add-Content -Path $output -Value "`n`nŒCIE¯KA: $relativePath"
        Add-Content -Path $output -Value $separator
        
        # Próba odczytu zawartoœci
        try {
            $content = Get-Content -Path $absolutePath -Raw -ErrorAction Stop
            
            if ([string]::IsNullOrWhiteSpace($content)) {
                Add-Content -Path $output -Value "[PLIK PUSTY]"
            }
            else {
                Add-Content -Path $output -Value $content
            }
        }
        catch [System.UnauthorizedAccessException] {
            Add-Content -Path $output -Value "[BRAK UPRAWNIEÑ - NIE MO¯NA ODCZYTAÆ]"
        }
        catch {
            Add-Content -Path $output -Value "[B£¥D ODCZYTU: $($_.Exception.Message)]"
        }
    }
}

# Uruchomienie eksportu
Export-Structure -output $outputFile

# Komunikat koñcowy
$outputPath = Join-Path (Get-Location) $outputFile
Write-Host "`nOperacja zakoñczona pomyœlnie!"
Write-Host "Plik wyjœciowy: $outputPath"
Write-Host "`nNaciœnij dowolny klawisz, aby kontynuowaæ..."
[Console]::ReadKey($true) | Out-Null
