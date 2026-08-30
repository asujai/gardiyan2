param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$outputDir = Join-Path $ProjectRoot "store_assets\en-US-v2"
$iconPath = Join-Path $ProjectRoot "store_assets\icon\play-icon-512.png"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

function New-RoundedPath {
    param([float]$X, [float]$Y, [float]$Width, [float]$Height, [float]$Radius)

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $diameter = $Radius * 2
    $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
    $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
    $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-StoreScreenshot {
    param(
        [string]$Source,
        [string]$Destination,
        [string]$Eyebrow,
        [string]$Headline,
        [string]$Subtitle
    )

    $canvas = New-Object System.Drawing.Bitmap(1080, 1920, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($canvas)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    $background = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Rectangle(0, 0, 1080, 1920)),
        [System.Drawing.ColorTranslator]::FromHtml("#07142E"),
        [System.Drawing.ColorTranslator]::FromHtml("#0C2A50"),
        90.0
    )
    $graphics.FillRectangle($background, 0, 0, 1080, 1920)
    $background.Dispose()

    $glowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(24, 30, 230, 195))
    $graphics.FillEllipse($glowBrush, -220, -180, 720, 720)
    $graphics.FillEllipse($glowBrush, 720, 1250, 620, 620)
    $glowBrush.Dispose()

    $icon = [System.Drawing.Image]::FromFile($iconPath)
    $graphics.DrawImage($icon, 58, 48, 82, 82)
    $icon.Dispose()

    $brandFont = New-Object System.Drawing.Font("Segoe UI", 22, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $eyebrowFont = New-Object System.Drawing.Font("Segoe UI", 25, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $headlineFont = New-Object System.Drawing.Font("Segoe UI", 76, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $subtitleFont = New-Object System.Drawing.Font("Segoe UI", 31, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $muted = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#B7C7DC"))
    $accent = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#22E6C5"))

    $graphics.DrawString("LIMITRA", $brandFont, $white, 158, 73)
    $graphics.DrawString($Eyebrow, $eyebrowFont, $accent, 58, 160)
    $graphics.DrawString($Headline, $headlineFont, $white, 54, 205)
    $graphics.DrawString($Subtitle, $subtitleFont, $muted, 58, 410)

    $phoneX = 144
    $phoneY = 510
    $phoneWidth = 792
    $phoneHeight = 1368
    $shadowPath = New-RoundedPath ($phoneX + 14) ($phoneY + 18) $phoneWidth $phoneHeight 62
    $shadow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(100, 0, 0, 0))
    $graphics.FillPath($shadow, $shadowPath)
    $shadow.Dispose()
    $shadowPath.Dispose()

    $phonePath = New-RoundedPath $phoneX $phoneY $phoneWidth $phoneHeight 62
    $border = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(130, 255, 255, 255), 8)
    $graphics.DrawPath($border, $phonePath)
    $border.Dispose()

    $sourceImage = [System.Drawing.Image]::FromFile($Source)
    $previousClip = $graphics.Clip
    $graphics.SetClip($phonePath)
    $graphics.DrawImage($sourceImage, $phoneX, $phoneY, $phoneWidth, $phoneHeight)
    $graphics.Clip = $previousClip
    $sourceImage.Dispose()
    $phonePath.Dispose()

    $brandFont.Dispose()
    $eyebrowFont.Dispose()
    $headlineFont.Dispose()
    $subtitleFont.Dispose()
    $white.Dispose()
    $muted.Dispose()
    $accent.Dispose()
    $graphics.Dispose()
    $canvas.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    $canvas.Dispose()
}

$cards = @(
    @{
        File = "01-block-distractions.png"
        Source = "test_render\limitra-protected3.png"
        Eyebrow = "STRICT APP BLOCKING"
        Headline = "BLOCK DISTRACTIONS.`nKEEP YOUR TIME."
        Subtitle = "Daily limits that stop the endless scroll."
    },
    @{
        File = "02-set-limits.png"
        Source = "test_render\limitra-add.png"
        Eyebrow = "FLEXIBLE DAILY LIMITS"
        Headline = "SET LIMITS`nYOUR WAY."
        Subtitle = "Choose apps, time limits and protection days."
    },
    @{
        File = "03-build-a-streak.png"
        Source = "test_render\limitra-progress.png"
        Eyebrow = "PROGRESS THAT MOTIVATES"
        Headline = "BUILD A STREAK.`nLEVEL UP."
        Subtitle = "Turn better screen habits into visible progress."
    },
    @{
        File = "04-private-history.png"
        Source = "test_render\limitra-timeline.png"
        Eyebrow = "PRIVATE ON-DEVICE HISTORY"
        Headline = "EVERY ACTION.`nCLEARLY TRACKED."
        Subtitle = "Review your protection history without an account."
    },
    @{
        File = "05-offline-private.png"
        Source = "test_render\limitra-permissions-1080.png"
        Eyebrow = "ONE PAYMENT - NO SUBSCRIPTIONS"
        Headline = "YOUR DATA`nSTAYS WITH YOU."
        Subtitle = "No ads, no trackers and no Internet permission."
    }
)

foreach ($card in $cards) {
    New-StoreScreenshot `
        -Source (Join-Path $ProjectRoot $card.Source) `
        -Destination (Join-Path $outputDir $card.File) `
        -Eyebrow $card.Eyebrow `
        -Headline $card.Headline `
        -Subtitle $card.Subtitle
}

$feature = New-Object System.Drawing.Bitmap(1024, 500, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$featureGraphics = [System.Drawing.Graphics]::FromImage($feature)
$featureGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$featureGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$featureGraphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$featureBackground = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Rectangle(0, 0, 1024, 500)),
    [System.Drawing.ColorTranslator]::FromHtml("#06142F"),
    [System.Drawing.ColorTranslator]::FromHtml("#0B3354"),
    0.0
)
$featureGraphics.FillRectangle($featureBackground, 0, 0, 1024, 500)
$featureBackground.Dispose()

$featureIconPath = Join-Path $ProjectRoot "store_assets\icon\limitra-mark-transparent-v2.png"
$icon = [System.Drawing.Image]::FromFile($featureIconPath)
$featureGraphics.DrawImage($icon, 54, 70, 360, 360)
$icon.Dispose()

$featureBrandFont = New-Object System.Drawing.Font("Segoe UI", 66, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$featureHeadlineFont = New-Object System.Drawing.Font("Segoe UI", 34, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$featureMetaFont = New-Object System.Drawing.Font("Segoe UI", 18, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
$accent = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#22E6C5"))
$muted = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#B7C7DC"))
$featureGraphics.DrawString("LIMITRA", $featureBrandFont, $white, 430, 115)
$featureGraphics.DrawString("Block distractions.`nKeep your time.", $featureHeadlineFont, $white, 435, 205)
$featureGraphics.DrawString("PAY ONCE  |  NO SUBSCRIPTIONS  |  100% OFFLINE", $featureMetaFont, $accent, 437, 330)
$featureGraphics.DrawString("Private app blocking, processed on your device.", $featureMetaFont, $muted, 437, 370)

$featureBrandFont.Dispose()
$featureHeadlineFont.Dispose()
$featureMetaFont.Dispose()
$white.Dispose()
$accent.Dispose()
$muted.Dispose()
$featureGraphics.Dispose()
$feature.Save((Join-Path $outputDir "feature-graphic-1024x500.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$feature.Dispose()

Get-ChildItem $outputDir -File | Sort-Object Name | Select-Object Name, Length
