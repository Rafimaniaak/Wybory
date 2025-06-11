# Konfiguracja
$outputFile = "struktura_i_zawartosc.txt"
$excludeFiles = @($outputFile, $MyInvocation.MyCommand.Name)

# Funkcja sprawdzaj�ca czy plik jest tekstowy (na podstawie rozszerzenia)
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

# G��wna funkcja eksportuj�ca
function Export-Structure {
    param(
        [string]$startPath = ".",
        [string]$output = "output.txt"
    )
    
    # Utw�rz plik wyj�ciowy (lub wyczy�� je�li istnieje)
    Set-Content -Path $output -Value "Eksport struktury katalog�w i plik�w`nWygenerowano: $(Get-Date)`n" -Force
    
    # Uzyskaj pe�n� �cie�k� startow�
    $absoluteStart = (Get-Item $startPath).FullName
    
    # Przeszukaj wszystkie pliki
    Get-ChildItem -Path $startPath -Recurse -File | ForEach-Object {
        $absolutePath = $_.FullName

        # Pomijanie plik�w wykluczonych po nazwie
        if ($excludeFiles -contains $_.Name) { return }
        
        # Pomijanie pliku wyj�ciowego po pe�nej �cie�ce
        if ($absolutePath -eq (Join-Path $pwd $output)) { return }

        # Pomijaj pliki binarne
        if (-not (Test-IsTextFile -Path $absolutePath)) { return }
        
        $relativePath = $absolutePath.Substring($absoluteStart.Length).TrimStart('\')
        $separator = "-" * ($relativePath.Length + 12)
        
        Add-Content -Path $output -Value "`n`n�CIE�KA: $relativePath"
        Add-Content -Path $output -Value $separator
        
        # Pr�ba odczytu zawarto�ci
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
            Add-Content -Path $output -Value "[BRAK UPRAWNIE� - NIE MO�NA ODCZYTA�]"
        }
        catch {
            Add-Content -Path $output -Value "[B��D ODCZYTU: $($_.Exception.Message)]"
        }
    }
}

# Uruchomienie eksportu
Export-Structure -output $outputFile

# Komunikat ko�cowy
$outputPath = Join-Path (Get-Location) $outputFile
Write-Host "`nOperacja zako�czona pomy�lnie!"
Write-Host "Plik wyj�ciowy: $outputPath"
Write-Host "`nNaci�nij dowolny klawisz, aby kontynuowa�..."
[Console]::ReadKey($true) | Out-Null
